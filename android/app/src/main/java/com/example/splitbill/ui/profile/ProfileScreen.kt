package com.example.splitbill.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.splitbill.data.api.ProfileResponse
import com.example.splitbill.data.supabase.SupabaseConfig
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.components.ProfileSkeleton
import com.example.splitbill.ui.components.getBankDisplayName
import com.example.splitbill.ui.components.saveVietQrImageToGallery
import com.example.splitbill.ui.localization.localized
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)
private val InnerShape = RoundedCornerShape(12.dp)
private const val QR_DESCRIPTION = "SplitBill chuyen tien"

// Danh sách ngân hàng Việt Nam phổ biến
val VIETNAMESE_BANKS = listOf(
  "ACB" to "ACB - Á Châu",
  "BIDV" to "BIDV",
  "ICB" to "VietinBank",
  "EIB" to "Eximbank",
  "HDB" to "HDBank",
  "LPB" to "LPBank",
  "MB" to "MB Bank",
  "MSB" to "MSB",
  "NAB" to "Nam A Bank",
  "NCB" to "NCB",
  "OCB" to "OCB",
  "PVB" to "PVcomBank",
  "SCB" to "SCB",
  "SEAB" to "SeABank",
  "SGB" to "Saigonbank",
  "SHB" to "SHB",
  "TCB" to "Techcombank",
  "TPB" to "TPBank",
  "VAB" to "VietABank",
  "VCB" to "Vietcombank",
  "VIB" to "VIB",
  "VPB" to "VPBank",
  "WOO" to "Woori Bank"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  isTab: Boolean = false
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val saveState by viewModel.saveState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var bankCode by remember { mutableStateOf("") }
  var accountNumber by remember { mutableStateOf("") }
  var accountName by remember { mutableStateOf("") }

  var showBankSheet by remember { mutableStateOf(false) }
  var showNotifications by remember { mutableStateOf(false) }
  // null = đóng, false = xem trước, true = kèm số tiền
  var qrSheetWithAmount by remember { mutableStateOf<Boolean?>(null) }
  var accountRevealed by remember { mutableStateOf(false) }
  var numberCopied by remember { mutableStateOf(false) }

  // Load form khi profile được tải
  LaunchedEffect(uiState) {
    if (uiState is ProfileUiState.Success) {
      val profile = (uiState as ProfileUiState.Success).profile
      bankCode = profile.bankCode ?: ""
      accountNumber = profile.accountNumber ?: ""
      accountName = profile.accountName ?: ""
    }
  }

  // Xử lý kết quả lưu
  LaunchedEffect(saveState) {
    when {
      saveState == "success" -> {
        showBankSheet = false
        Toast.makeText(context, "Đã lưu thông tin ngân hàng", Toast.LENGTH_SHORT).show()
        viewModel.clearSaveState()
      }
      saveState == "avatar_success" -> {
        Toast.makeText(context, "Đã cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
        viewModel.clearSaveState()
      }
      saveState?.startsWith("error:") == true -> {
        val errorMsg = saveState?.removePrefix("error:") ?: "Thao tác thất bại"
        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        viewModel.clearSaveState()
      }
    }
  }

  // Tự trả nhãn "Sao chép" về trạng thái ban đầu sau 2 giây
  LaunchedEffect(numberCopied) {
    if (numberCopied) {
      delay(2000)
      numberCopied = false
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val bytes = compressAvatarImage(context, uri)
      if (bytes != null) {
        viewModel.uploadAvatar(bytes)
      } else {
        try {
          context.contentResolver.openInputStream(uri)?.use { inputStream ->
            viewModel.uploadAvatar(inputStream.readBytes())
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  val pickAvatar = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        // Thanh bar và nút (+) do Navigation vẽ đè lên, nên nội dung tự chừa
        // inset thanh điều hướng; phần đệm cho thanh bar nằm trong contentPadding dưới.
        .navigationBarsPadding()
    ) {
      ProfileHeader(
        isTab = isTab,
        onNavigateBack = onNavigateBack,
        onOpenNotifications = { showNotifications = true }
      )

      when (val state = uiState) {
        is ProfileUiState.Loading -> {
          ProfileSkeleton(modifier = Modifier.fillMaxSize())
        }

        is ProfileUiState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { viewModel.loadProfile() }) {
              Text("Thử lại".localized())
            }
          }
        }

        is ProfileUiState.Success -> {
          val profile = state.profile
          val bankConnected = !profile.bankCode.isNullOrBlank() && !profile.accountNumber.isNullOrBlank()
          val avatarUrl = remember(profile.avatarUrl) { profile.avatarUrl.toFullUrl() }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              start = 20.dp,
              end = 20.dp,
              top = 8.dp,
              bottom = Dimens.TabContentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            item {
              IdentityCard(
                profile = profile,
                avatarUrl = avatarUrl,
                onPickAvatar = { pickAvatar() }
              )
            }

            item {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                  title = "Thông tin ngân hàng".localized(),
                  subtitle = "Giúp bạn bè chuyển khoản hoàn trả dễ dàng hơn.".localized(),
                  trailing = { StatusChip(active = bankConnected, onLabel = "Đã lưu", offLabel = "Chưa lưu") }
                )

                if (bankConnected) {
                  BankCard(
                    bankCode = bankCode,
                    accountNumber = accountNumber,
                    accountName = accountName.ifBlank { profile.username },
                    revealed = accountRevealed,
                    copied = numberCopied,
                    onToggleReveal = { accountRevealed = !accountRevealed },
                    onCopy = {
                      copyToClipboard(context, "Số tài khoản", accountNumber)
                      numberCopied = true
                    },
                    onEdit = { showBankSheet = true }
                  )
                } else {
                  EmptyBankCard(onSetup = { showBankSheet = true })
                }
              }
            }

            item {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                  title = "Thanh toán bằng VietQR".localized(),
                  subtitle = "Bạn bè có thể quét mã QR để chuyển khoản hoàn trả cho bạn.".localized()
                )
                VietQrCard(
                  enabled = bankConnected,
                  onPreview = { qrSheetWithAmount = false },
                  onCustomAmount = { qrSheetWithAmount = true }
                )
              }
            }
          }
        }
      }
    }
  }

  if (showBankSheet) {
    BankEditSheet(
      initialBankCode = bankCode,
      initialAccountNumber = accountNumber,
      initialAccountName = accountName,
      onDismiss = { showBankSheet = false },
      onSave = { code, number, name ->
        viewModel.saveBankInfo(code, number, name)
      }
    )
  }

  val successProfile = (uiState as? ProfileUiState.Success)?.profile
  val qrAmountMode = qrSheetWithAmount
  if (successProfile != null && qrAmountMode != null) {
    ProfileQrSheet(
      profile = successProfile,
      startWithAmount = qrAmountMode,
      onDismiss = { qrSheetWithAmount = null }
    )
  }

  if (showNotifications) {
    NotificationsSheet(onDismiss = { showNotifications = false })
  }
}

/* --------------------------------------------------------------- header */

@Composable
private fun ProfileHeader(
  isTab: Boolean,
  onNavigateBack: () -> Unit,
  onOpenNotifications: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (!isTab) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .clickable(onClick = onNavigateBack),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(Modifier.width(4.dp))
    }

    // Không hiển thị tiêu đề "Cá nhân" (bottom bar đã có nhãn), chỉ giữ khoảng trống cho header
    Spacer(Modifier.weight(1f))

    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .clickable(onClick = onOpenNotifications),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.NotificationsNone,
        contentDescription = "Thông báo",
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

/* -------------------------------------------------------------- identity */

@Composable
private fun IdentityCard(
  profile: ProfileResponse,
  avatarUrl: String?,
  onPickAvatar: () -> Unit
) {
  Surface(
    shape = CardShape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(modifier = Modifier.size(72.dp)) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .align(Alignment.TopStart)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center
        ) {
          if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
              model = avatarUrl,
              contentDescription = "Avatar",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Text(
              text = initialsOf(profile.username),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }
        }

        // Nút máy ảnh để đổi ảnh đại diện
        Box(
          modifier = Modifier
            .size(26.dp)
            .align(Alignment.BottomEnd)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onPickAvatar),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.PhotoCamera,
            contentDescription = "Thay đổi ảnh đại diện",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(13.dp)
          )
        }
      }

      Spacer(Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = profile.username,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = profile.email,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "@${handleOf(profile.username)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          val joinedYear = profile.createdAt?.take(4)
          if (!joinedYear.isNullOrBlank()) {
            Box(
              modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Text(
              text = "${"Thành viên từ".localized()} $joinedYear",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

/* ----------------------------------------------------------------- bank */

@Composable
private fun BankCard(
  bankCode: String,
  accountNumber: String,
  accountName: String,
  revealed: Boolean,
  copied: Boolean,
  onToggleReveal: () -> Unit,
  onCopy: () -> Unit,
  onEdit: () -> Unit
) {
  Surface(
    shape = CardShape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(InnerShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
          contentAlignment = Alignment.Center
        ) {
          AsyncImage(
            model = "https://api.vietqr.io/img/$bankCode.png",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp)
          )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = getBankDisplayName(bankCode),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = bankCode.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Icon(
          imageVector = Icons.Rounded.Contactless,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(22.dp)
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(InnerShape)
          .background(MaterialTheme.colorScheme.surfaceContainerLow)
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Số tài khoản".localized().uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = MaterialTheme.colorScheme.secondary
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (revealed) groupDigits(accountNumber) else maskAccount(accountNumber),
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleReveal),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = "Ẩn hoặc hiện số tài khoản",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(17.dp)
              )
            }
          }
        }

        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onCopy)
            .padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (copied) Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(Modifier.width(5.dp))
          Text(
            text = if (copied) "Đã chép!".localized() else "Sao chép".localized(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Chủ tài khoản".localized().uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = MaterialTheme.colorScheme.secondary
          )
          Text(
            text = accountName.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.4.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Row(
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = "Chỉnh sửa".localized(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyBankCard(onSetup: () -> Unit) {
  Surface(
    shape = CardShape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(InnerShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Chưa thiết lập ngân hàng".localized(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Thêm tài khoản nhận để bạn bè chuyển khoản trả nợ.".localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = onSetup,
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp),
        shape = CircleShape
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Thiết lập ngay".localized(), style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

/* ---------------------------------------------------------------- vietqr */

@Composable
private fun VietQrCard(
  enabled: Boolean,
  onPreview: () -> Unit,
  onCustomAmount: () -> Unit
) {
  Surface(
    shape = CardShape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(InnerShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.QrCode2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Tự động gắn mã QR khi đòi nợ".localized(),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            StatusChip(active = enabled, onLabel = "Đang bật", offLabel = "Chưa bật")
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = "Mỗi khi chia tiền trong nhóm, SplitBill sẽ tự động gắn thông tin VietQR ở trên để bạn bè thanh toán chỉ trong 1 chạm.".localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QrActionButton(
          icon = Icons.Rounded.Visibility,
          label = "Xem trước mã QR".localized(),
          enabled = enabled,
          onClick = onPreview,
          modifier = Modifier.weight(1f)
        )
        QrActionButton(
          icon = Icons.Rounded.Tune,
          label = "Cài đặt kèm số tiền".localized(),
          enabled = enabled,
          onClick = onCustomAmount,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun QrActionButton(
  icon: ImageVector,
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .clip(InnerShape)
      .background(
        if (enabled) {
          MaterialTheme.colorScheme.surfaceContainerLow
        } else {
          MaterialTheme.colorScheme.surfaceContainer
        }
      )
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
      } else {
        MaterialTheme.colorScheme.outline
      },
      modifier = Modifier.size(16.dp)
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
      } else {
        MaterialTheme.colorScheme.outline
      },
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/* -------------------------------------------------------------- helpers */

@Composable
private fun SectionHeader(
  title: String,
  subtitle: String,
  trailing: @Composable (() -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 2.dp),
    verticalAlignment = Alignment.Top
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    if (trailing != null) {
      Spacer(Modifier.width(12.dp))
      trailing()
    }
  }
}

@Composable
private fun StatusChip(active: Boolean, onLabel: String, offLabel: String) {
  val tint = if (active) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.outline
  }
  Row(
    modifier = Modifier
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainer)
      .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(tint)
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = if (active) onLabel.localized() else offLabel.localized(),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = tint
    )
  }
}

@Composable
private fun CloseCircleButton(onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(34.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainer)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Rounded.Close,
      contentDescription = "Đóng",
      tint = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(18.dp)
    )
  }
}

/* --------------------------------------------------------------- sheets */

/** Ô nhập dùng chung, đồng bộ kiểu với thanh tìm kiếm ở tab Nhóm. */
@Composable
private fun ProfileField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  leadingIcon: ImageVector? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
  val interactionSource = remember { MutableInteractionSource() }
  val focused by interactionSource.collectIsFocusedAsState()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = 1.dp,
        color = if (focused) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        },
        shape = RoundedCornerShape(16.dp)
      )
      .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    if (leadingIcon != null) {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      interactionSource = interactionSource,
      textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium
      ),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      keyboardOptions = keyboardOptions,
      decorationBox = { innerTextField ->
        Box(contentAlignment = Alignment.CenterStart) {
          if (value.isEmpty()) {
            Text(
              text = placeholder,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          innerTextField()
        }
      },
      modifier = Modifier.weight(1f)
    )
  }
}

/**
 * Khung sheet tự dựng thay cho ModalBottomSheet cho các form có ô nhập.
 * ModalBottomSheet nằm trong một cửa sổ dialog riêng nên không co lại khi bàn phím bật,
 * khiến ô nhập bị che. Ở đây cả tấm sheet được đẩy lên trên bàn phím và thanh điều hướng,
 * phần nội dung còn lại tự cuộn được.
 *
 * Cử chỉ giống ModalBottomSheet: nắm thanh gạch trên đầu rồi kéo lên để xem thêm nội dung,
 * kéo xuống quá ngưỡng (hoặc vuốt mạnh xuống) để đóng; kéo xuống khi nội dung đang ở đầu
 * cũng đóng được. Thả tay chưa đủ ngưỡng thì sheet bật về chỗ cũ.
 */
@Composable
private fun ProfileSheet(
  onDismiss: () -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val scrollState = rememberScrollState()
  var dragPx by remember { mutableFloatStateOf(0f) }
  var sheetHeightPx by remember { mutableIntStateOf(0) }
  var settleJob by remember { mutableStateOf<Job?>(null) }

  val dismissThreshold = with(density) { 120.dp.toPx() }
  val flickThreshold = with(density) { 24.dp.toPx() }
  val maxLift = with(density) { 72.dp.toPx() }

  // Thả tay: đóng nếu đã kéo đủ xa (hoặc vuốt mạnh), ngược lại bật về vị trí neo
  suspend fun settle(velocityY: Float) {
    val dismiss = dragPx > dismissThreshold ||
      (velocityY > 1400f && dragPx > flickThreshold)
    animate(
      initialValue = dragPx,
      targetValue = if (dismiss) (sheetHeightPx.takeIf { it > 0 } ?: 800).toFloat() else 0f,
      animationSpec = if (dismiss) {
        tween(durationMillis = 180)
      } else {
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
      }
    ) { value, _ -> dragPx = value }
    if (dismiss) onDismiss()
  }

  val dragState = rememberDraggableState { delta ->
    when {
      // Kéo lên: trả sheet về vị trí neo trước, rồi cuộn nội dung, hết chỗ thì nhích nhẹ
      delta < 0f -> when {
        dragPx > 0f -> dragPx = (dragPx + delta).coerceAtLeast(0f)
        scrollState.canScrollForward -> scrollState.dispatchRawDelta(-delta)
        else -> dragPx = (dragPx + delta * 0.35f).coerceAtLeast(-maxLift)
      }
      // Kéo xuống: cuộn nội dung về đầu trước, sau đó cả tấm sheet đi theo tay
      else -> when {
        dragPx < 0f -> dragPx = (dragPx + delta).coerceAtMost(0f)
        scrollState.canScrollBackward -> scrollState.dispatchRawDelta(-delta)
        else -> dragPx += delta
      }
    }
  }

  // Kéo xuống ngay trên nội dung (khi đã ở đầu) cũng đóng được như ModalBottomSheet
  val nestedScrollConnection = remember(scrollState) {
    object : NestedScrollConnection {
      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
      ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (available.y <= 0f || scrollState.value > 0 || dragPx < 0f) return Offset.Zero
        dragPx += available.y
        return Offset(0f, available.y)
      }

      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (dragPx > 0f) settle(available.y)
        return Velocity.Zero
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Lớp mờ: chạm ra ngoài để đóng, mờ dần theo độ kéo của sheet
      Box(
        modifier = Modifier
          .fillMaxSize()
          .drawBehind {
            val hideRatio = dragPx.coerceAtLeast(0f) /
              sheetHeightPx.coerceAtLeast(1).toFloat()
            drawRect(Color.Black, alpha = 0.45f * (1f - hideRatio).coerceIn(0f, 1f))
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismiss
          )
      )

      // Vùng neo sheet: chừa chỗ cho bàn phím, thanh điều hướng và thanh trạng thái
      Box(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(
            WindowInsets.ime
              .union(WindowInsets.navigationBars)
              .union(WindowInsets.statusBars)
          )
      ) {
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .offset { IntOffset(0, dragPx.roundToInt()) }
            .onSizeChanged { sheetHeightPx = it.height },
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
          color = MaterialTheme.colorScheme.surface
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // Tay nắm: kéo lên/xuống
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .draggable(
                  state = dragState,
                  orientation = Orientation.Vertical,
                  onDragStarted = { settleJob?.cancel() },
                  onDragStopped = { velocity ->
                    settleJob = scope.launch { settle(velocity) }
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(width = 36.dp, height = 4.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.outlineVariant)
              )
            }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
              content = content
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BankEditSheet(
  initialBankCode: String,
  initialAccountNumber: String,
  initialAccountName: String,
  onDismiss: () -> Unit,
  onSave: (bankCode: String, accountNumber: String, accountName: String) -> Unit
) {
  var bankCode by remember { mutableStateOf(initialBankCode) }
  var accountNumber by remember { mutableStateOf(initialAccountNumber) }
  var accountName by remember { mutableStateOf(initialAccountName) }
  var pickerOpen by remember { mutableStateOf(false) }
  var bankQuery by remember { mutableStateOf("") }

  ProfileSheet(onDismiss = onDismiss) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Cập nhật tài khoản nhận".localized(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Sử dụng để tạo VietQR nhận hoàn tiền tức thì".localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(Modifier.width(12.dp))
        CloseCircleButton(onClick = onDismiss)
      }

      // Ngân hàng thụ hưởng
      Column {
        Text(
          text = "Ngân hàng thụ hưởng".localized(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
              shape = RoundedCornerShape(16.dp)
            )
            .clickable { pickerOpen = !pickerOpen }
            .padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (bankCode.isNotBlank()) {
            AsyncImage(
              model = "https://api.vietqr.io/img/$bankCode.png",
              contentDescription = null,
              contentScale = ContentScale.Fit,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = getBankDisplayName(bankCode),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f)
            )
          } else {
            Text(
              text = "Chọn Ngân Hàng".localized(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
          }
          Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }

        if (pickerOpen) {
          Spacer(Modifier.height(8.dp))
          ProfileField(
            value = bankQuery,
            onValueChange = { bankQuery = it },
            placeholder = "Tìm kiếm ngân hàng...".localized(),
            leadingIcon = Icons.Rounded.Search
          )
          Spacer(Modifier.height(4.dp))
          val filtered = VIETNAMESE_BANKS.filter {
            it.first.contains(bankQuery, ignoreCase = true) ||
              it.second.contains(bankQuery, ignoreCase = true)
          }
          LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(filtered) { (code, name) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    bankCode = code
                    pickerOpen = false
                    bankQuery = ""
                  }
                  .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AsyncImage(
                  model = "https://api.vietqr.io/img/$code.png",
                  contentDescription = null,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                  text = name,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      // Số tài khoản
      Column {
        Text(
          text = "Số tài khoản".localized(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        ProfileField(
          value = accountNumber,
          onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
          placeholder = "Nhập số tài khoản (giữ số 0 đầu)".localized(),
          leadingIcon = Icons.Rounded.CreditCard,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "Hỗ trợ giữ nguyên số 0 ở đầu tài khoản".localized(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary
        )
      }

      // Tên chủ tài khoản
      Column {
        Text(
          text = "Tên chủ tài khoản (In hoa không dấu)".localized(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        ProfileField(
          value = accountName,
          onValueChange = { accountName = it.uppercase() },
          placeholder = "VD: NGUYEN VAN A",
          leadingIcon = Icons.Rounded.Person,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Hủy".localized(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Button(
          onClick = { onSave(bankCode, accountNumber, accountName.trim()) },
          enabled = bankCode.isNotBlank() && accountNumber.isNotBlank() && accountName.isNotBlank(),
          shape = CircleShape,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
        ) {
          Text("Lưu thông tin".localized(), style = MaterialTheme.typography.labelLarge)
        }
      }
  }
}

@Composable
private fun ProfileQrSheet(
  profile: ProfileResponse,
  startWithAmount: Boolean,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var amountText by remember { mutableStateOf("") }

  val bankCode = (profile.bankCode ?: "").let { if (it.uppercase() == "CTG") "ICB" else it }
  val accountNumber = profile.accountNumber ?: ""
  val accountName = profile.accountName ?: profile.username
  val amount = amountText.toLongOrNull()

  val qrUrl = remember(bankCode, accountNumber, accountName, amount) {
    buildPersonalQrUrl(bankCode, accountNumber, accountName, amount)
  }

  ProfileSheet(onDismiss = onDismiss) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Mã VietQR cá nhân".localized(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Sử dụng để bạn bè thanh toán chia tiền".localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(Modifier.width(12.dp))
        CloseCircleButton(onClick = onDismiss)
      }

      // Khung thẻ VietQR
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(CardShape)
          .background(MaterialTheme.colorScheme.surfaceContainerLow)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "VIETQR.NAPAS",
              style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = getBankDisplayName(bankCode).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box(
          modifier = Modifier
            .size(248.dp)
            .clip(InnerShape)
            .background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          AsyncImage(
            model = qrUrl,
            contentDescription = "Mã VietQR cá nhân",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .padding(10.dp)
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = accountName.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.4.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${groupDigits(accountNumber)} • ${bankCode.uppercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }

      if (startWithAmount) {
        ProfileField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() }.take(12) },
          placeholder = "Số tiền cần thu (đ)".localized(),
          leadingIcon = Icons.Rounded.Payments,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
      }

      Button(
        onClick = { saveVietQrImageToGallery(context, qrUrl) },
        shape = CircleShape,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
      ) {
        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Lưu mã QR về máy".localized(), style = MaterialTheme.typography.labelLarge)
      }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSheet(onDismiss: () -> Unit) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Thông báo".localized(),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )
        CloseCircleButton(onClick = onDismiss)
      }
      Spacer(Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.NotificationsNone,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
      }
      Text(
        text = "Chưa có thông báo mới".localized(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Thông báo về khoản chi mới, nhắc nợ và xác nhận thanh toán sẽ xuất hiện ở đây.".localized(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(4.dp))
      TextButton(onClick = onDismiss) {
        Text("Đóng".localized())
      }
    }
  }
}

/* ------------------------------------------------------- formatters/util */

private fun String?.toFullUrl(): String? = when {
  this.isNullOrBlank() -> null
  this.startsWith("http") -> this
  else -> "${SupabaseConfig.SUPABASE_URL}${if (this.startsWith("/")) "" else "/"}$this"
}

private fun initialsOf(name: String): String {
  val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  return when {
    words.isEmpty() -> "?"
    words.size == 1 -> words.first().take(1).uppercase()
    else -> (words[0].take(1) + words[1].take(1)).uppercase()
  }
}

private fun handleOf(name: String): String =
  name.trim().lowercase().replace(Regex("\\s+"), "")

private fun maskAccount(number: String): String =
  if (number.length <= 4) number else "•••• •••• ${number.takeLast(4)}"

private fun groupDigits(number: String): String =
  number.chunked(4).joinToString(" ")

private fun buildPersonalQrUrl(
  bankCode: String,
  accountNumber: String,
  accountName: String,
  amount: Long?
): String {
  val params = buildList {
    if (amount != null && amount > 0) add("amount=$amount")
    add("addInfo=${Uri.encode(QR_DESCRIPTION)}")
    add("accountName=${Uri.encode(accountName)}")
  }
  return "https://img.vietqr.io/image/$bankCode-$accountNumber-compact2.png?${params.joinToString("&")}"
}

private fun copyToClipboard(context: Context, label: String, value: String) {
  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cm.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun compressAvatarImage(context: Context, uri: android.net.Uri): ByteArray? {
  return try {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null

    val maxDim = 360
    val width = originalBitmap.width
    val height = originalBitmap.height
    val scale = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height).coerceAtMost(1.0f)

    val scaledWidth = (width * scale).toInt()
    val scaledHeight = (height * scale).toInt()
    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

    val outputStream = java.io.ByteArrayOutputStream()
    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
    outputStream.toByteArray()
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

/* ----------------------------------------------------- bank picker sheet */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectionBottomSheet(
  onDismiss: () -> Unit,
  onBankSelected: (String) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var searchQuery by remember { mutableStateOf("") }

  val filteredBanks = remember(searchQuery) {
    VIETNAMESE_BANKS.filter {
      it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = "Chọn Ngân Hàng".localized(),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(16.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Tìm kiếm ngân hàng...".localized()) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = true
      )
      Spacer(Modifier.height(16.dp))

      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(filteredBanks) { (code, name) ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onBankSelected(code)
                onDismiss()
              }
              .padding(vertical = 16.dp)
          ) {
            AsyncImage(
              model = "https://api.vietqr.io/img/$code.png",
              contentDescription = code,
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        item {
          Spacer(Modifier.height(32.dp))
        }
      }
    }
  }
}
