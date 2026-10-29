package com.example.splitbill.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.BuildConfig
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.localization.localized
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(16.dp)
private val IconBadgeShape = RoundedCornerShape(12.dp)
private const val FONT_MIN = 0.85f
private const val FONT_MAX = 1.25f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateToProfile: () -> Unit,
  onLogoutSuccess: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  isTab: Boolean = false
) {
  val profileState by viewModel.profileUiState.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
  val language by viewModel.language.collectAsStateWithLifecycle()
  val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
  val pushEnabled by viewModel.pushEnabled.collectAsStateWithLifecycle()
  val autoSettleEnabled by viewModel.autoSettleEnabled.collectAsStateWithLifecycle()

  val context = LocalContext.current
  var showLanguageSheet by remember { mutableStateOf(false) }
  var showLogoutSheet by remember { mutableStateOf(false) }
  var infoPage by remember { mutableStateOf<AppInfoPage?>(null) }
  var hasListenerPermission by remember { mutableStateOf(context.hasNotificationListenerAccess()) }
  var awaitingListenerPermission by remember { mutableStateOf(false) }

  val requestListenerPermission = {
    awaitingListenerPermission = true
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
  }

  // Đồng bộ lại trạng thái quyền mỗi khi người dùng quay về từ màn hình cài đặt hệ thống
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val granted = context.hasNotificationListenerAccess()
        hasListenerPermission = granted
        // Người dùng vừa đi cấp quyền → bật luôn đối soát tự động
        if (granted && awaitingListenerPermission) {
          awaitingListenerPermission = false
          viewModel.saveAutoSettleEnabled(true)
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val bankConnected = (profileState as? SettingsProfileUiState.Success)?.hasBankInfo == true

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
      // Bottom bar đã hiển thị tên "Cài đặt" nên màn hình không cần top bar
      if (!isTab) {
        SettingsBackBar(onNavigateBack = onNavigateBack)
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 20.dp,
          end = 20.dp,
          top = 12.dp,
          bottom = Dimens.TabContentBottomPadding
        )
      ) {
        item { AccountSyncBar() }

        item {
          SettingsSection(title = "Giao diện".localized()) {
            AppearanceCard(
              themeMode = themeMode,
              fontScale = fontScale,
              onThemeSelected = { viewModel.saveThemeMode(it) },
              onFontScaleChange = { viewModel.saveFontScale(it) }
            )
          }
        }

        item {
          SettingsSection(title = "Ngôn ngữ".localized()) {
            SettingsPanel {
              SettingRow(
                icon = Icons.Rounded.Translate,
                title = "Ngôn ngữ hiển thị".localized(),
                subtitle = "Áp dụng toàn bộ ứng dụng".localized(),
                modifier = Modifier.clickable { showLanguageSheet = true },
                trailing = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageChip(language = language)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              )
            }
          }
        }

        item {
          SettingsSection(title = "Thông báo".localized()) {
            SettingsPanel {
              SettingRow(
                icon = Icons.Rounded.NotificationsActive,
                title = "Thông báo đẩy".localized(),
                subtitle = "Nhắc nợ lịch sự & xác nhận thanh toán tức thời".localized(),
                trailing = {
                  Switch(
                    checked = pushEnabled,
                    onCheckedChange = { viewModel.savePushEnabled(it) }
                  )
                }
              )
            }
          }
        }

        item {
          SettingsSection(title = "Bảo mật và đối soát".localized()) {
            SettingsPanel {
              SettingRow(
                icon = Icons.Rounded.Fingerprint,
                title = "Bảo mật sinh trắc học".localized(),
                subtitle = "Khóa bằng vân tay hoặc khuôn mặt".localized(),
                trailing = {
                  Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { viewModel.saveBiometricEnabled(it) }
                  )
                }
              )
              SettingsDivider()
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                SettingRow(
                  icon = Icons.Rounded.DocumentScanner,
                  title = "Tự động nhận diện thanh toán".localized(),
                  subtitle = "Khớp tiền hoàn từ biến động số dư ngân hàng".localized(),
                  contentPadding = PaddingValues(0.dp),
                  trailing = {
                    Switch(
                      checked = autoSettleEnabled && hasListenerPermission,
                      onCheckedChange = { isChecked ->
                        if (isChecked && !hasListenerPermission) {
                          requestListenerPermission()
                        } else {
                          viewModel.saveAutoSettleEnabled(isChecked)
                        }
                      }
                    )
                  }
                )
                if (hasListenerPermission) {
                  ReconcileEnabledNotice()
                } else {
                  ListenerPermissionNotice(onRequestPermission = { requestListenerPermission() })
                }
              }
            }
          }
        }

        item {
          SettingsSection(title = "Thanh toán & Chuyển khoản".localized()) {
            SettingsPanel(modifier = Modifier.clickable { onNavigateToProfile() }) {
              SettingRow(
                icon = Icons.Rounded.QrCode2,
                title = "Thiết lập VietQR".localized(),
                subtitle = "Tạo mã QR kèm đúng số tiền nợ cho bạn bè quét trả".localized(),
                titleAccessory = { ConnectionChip(connected = bankConnected) },
                trailing = {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              )
            }
          }
        }

        item {
          SettingsSection(title = "Thông tin ứng dụng".localized()) {
            SettingsPanel {
              AppInfoLinkRow(
                title = "Điều khoản sử dụng".localized(),
                onClick = { infoPage = AppInfoPage.Terms }
              )
              SettingsDivider()
              AppInfoLinkRow(
                title = "Chính sách quyền riêng tư".localized(),
                onClick = { infoPage = AppInfoPage.Privacy }
              )
              SettingsDivider()
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Phiên bản ứng dụng".localized(),
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.outline
                )
              }
            }
          }
        }

        item {
          Box(modifier = Modifier.padding(bottom = 16.dp)) {
            SettingsPanel(modifier = Modifier.clickable { showLogoutSheet = true }) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(IconBadgeShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Đăng xuất".localized(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = "Đăng xuất tài khoản khỏi thiết bị này".localized(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }

      }
    }
  }

  if (showLanguageSheet) {
    LanguageSheet(
      language = language,
      onDismiss = { showLanguageSheet = false },
      onLanguageSelected = {
        viewModel.saveLanguage(it)
        showLanguageSheet = false
      }
    )
  }

  if (showLogoutSheet) {
    LogoutSheet(
      onDismiss = { showLogoutSheet = false },
      onConfirm = {
        showLogoutSheet = false
        viewModel.logout { onLogoutSuccess() }
      }
    )
  }

  infoPage?.let { page ->
    AlertDialog(
      onDismissRequest = { infoPage = null },
      title = { Text(page.title(), fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = page.body(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        TextButton(onClick = { infoPage = null }) {
          Text("Đóng".localized())
        }
      },
      shape = MaterialTheme.shapes.large
    )
  }
}

/* ---------------------------------------------------------------- header */

/** Chỉ dùng khi màn hình được mở như một route riêng (không phải tab). */
@Composable
private fun SettingsBackBar(onNavigateBack: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .clickable(onClick = onNavigateBack),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = "Back",
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
private fun AccountSyncBar() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 20.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary)
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = "Tài khoản & Thiết bị".localized().uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shape = CircleShape
    ) {
      Text(
        text = "Đồng bộ đám mây".localized(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }
  }
}

/* -------------------------------------------------------------- sections */

@Composable
private fun SettingsSection(
  title: String,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 24.dp)
  ) {
    Text(
      text = title.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
    content()
  }
}

@Composable
private fun SettingsPanel(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(
        elevation = 3.dp,
        shape = CardShape,
        spotColor = Color.Black.copy(alpha = 0.06f),
        ambientColor = Color.Black.copy(alpha = 0.04f)
      )
      .clip(CardShape)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    content = content
  )
}

@Composable
private fun SettingsDivider() {
  Box(
    modifier = Modifier
      .padding(horizontal = 16.dp)
      .fillMaxWidth()
      .height(1.dp)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
  )
}

@Composable
private fun SettingRow(
  icon: ImageVector,
  title: String,
  subtitle: String? = null,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  titleAccessory: @Composable (() -> Unit)? = null,
  trailing: @Composable () -> Unit = {}
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(contentPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SettingIconBadge(icon)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (titleAccessory != null) {
          Spacer(Modifier.width(8.dp))
          titleAccessory()
        }
      }
      if (subtitle != null) {
        Spacer(Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    Spacer(Modifier.width(8.dp))
    trailing()
  }
}

@Composable
private fun SettingIconBadge(icon: ImageVector) {
  Box(
    modifier = Modifier
      .size(36.dp)
      .clip(IconBadgeShape)
      .background(MaterialTheme.colorScheme.surfaceContainer),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp)
    )
  }
}

/* ------------------------------------------------------------ appearance */

@Composable
private fun AppearanceCard(
  themeMode: String,
  fontScale: Float,
  onThemeSelected: (String) -> Unit,
  onFontScaleChange: (Float) -> Unit
) {
  var sliderValue by remember(fontScale) { mutableStateOf(fontScale.coerceIn(FONT_MIN, FONT_MAX)) }

  SettingsPanel {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SettingIconBadge(Icons.Rounded.Palette)
          Spacer(Modifier.width(12.dp))
          Text(
            text = "Chế độ hiển thị".localized(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = when (themeMode) {
            "light" -> "Sáng".localized()
            "dark" -> "Tối".localized()
            else -> "Hệ thống".localized()
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      ThemeSegmentedSelector(
        themeMode = themeMode,
        onThemeSelected = onThemeSelected
      )
    }

    SettingsDivider()

    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SettingIconBadge(Icons.Rounded.FormatSize)
          Spacer(Modifier.width(12.dp))
          Text(
            text = "Cỡ chữ".localized(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        FontScaleBadge(scale = sliderValue)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "A−",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
          value = sliderValue,
          onValueChange = { raw ->
            sliderValue = ((raw * 20f).roundToInt() / 20f).coerceIn(FONT_MIN, FONT_MAX)
          },
          onValueChangeFinished = { onFontScaleChange(sliderValue) },
          valueRange = FONT_MIN..FONT_MAX,
          steps = 7,
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
          ),
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
        )
        Text(
          text = "A+",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(IconBadgeShape)
          .background(MaterialTheme.colorScheme.surfaceContainerLow)
          .padding(12.dp),
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          imageVector = Icons.Rounded.Visibility,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .size(18.dp)
            .padding(top = 1.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
          text = "Xem trước: Bữa tối nhóm hôm qua chia đều 145.000đ cho mỗi thành viên.".localized(),
          fontSize = (12f * sliderValue).sp,
          lineHeight = (16f * sliderValue).sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun ThemeSegmentedSelector(
  themeMode: String,
  onThemeSelected: (String) -> Unit
) {
  val options = listOf(
    ThemeOption("light", "Sáng".localized(), Icons.Rounded.LightMode),
    ThemeOption("dark", "Tối".localized(), Icons.Rounded.DarkMode),
    ThemeOption("system", "Hệ thống".localized(), Icons.Rounded.BrightnessAuto)
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(IconBadgeShape)
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    options.forEach { option ->
      val isSelected = themeMode == option.value
      Row(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .then(
            if (isSelected) {
              Modifier.shadow(1.dp, RoundedCornerShape(8.dp))
            } else {
              Modifier
            }
          )
          .background(
            if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent
          )
          .clickable { onThemeSelected(option.value) }
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = option.icon,
          contentDescription = null,
          tint = if (isSelected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = option.label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
          color = if (isSelected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        )
      }
    }
  }
}

private data class ThemeOption(
  val value: String,
  val label: String,
  val icon: ImageVector
)

@Composable
private fun FontScaleBadge(scale: Float) {
  val percent = (scale * 100).roundToInt()
  val label = when {
    percent < 95 -> "Nhỏ".localized()
    percent > 105 -> "Lớn".localized()
    else -> "Chuẩn".localized()
  }
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = CircleShape
  ) {
    Text(
      text = "$label ($percent%)",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
    )
  }
}

/* ------------------------------------------------------------- language */

@Composable
private fun LanguageChip(language: String) {
  val isEnglish = language == "en"
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = CircleShape
  ) {
    Text(
      text = if (isEnglish) "🇬🇧 English" else "🇻🇳 Tiếng Việt",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

/* ------------------------------------------------------- security cards */

@Composable
private fun ListenerPermissionNotice(onRequestPermission: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(IconBadgeShape)
      .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Rounded.VerifiedUser,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(18.dp)
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = "Cần quyền Notification Listener".localized(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    Text(
      text = ("SplitBill cần quyền phát hiện thông báo ngân hàng khi bạn nhận tiền hoàn từ bạn bè. " +
        "Ứng dụng cam kết hoàn toàn không đọc tin nhắn SMS hay thông tin tài khoản riêng tư.").localized(),
      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Bảo mật thiết bị Android".localized(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )
      Row(
        modifier = Modifier
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary)
          .clickable(onClick = onRequestPermission)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Thiết lập quyền".localized(),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.width(4.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
private fun ReconcileEnabledNotice() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(IconBadgeShape)
      .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = "Đã bật đối soát giao dịch tự động".localized(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    Text(
      text = "Ứng dụng đang lắng nghe thông báo hoàn tiền để tự động cập nhật khoản thu chung.".localized(),
      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun ConnectionChip(connected: Boolean) {
  Surface(
    color = if (connected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    shape = CircleShape
  ) {
    Text(
      text = if (connected) "Đã kết nối".localized() else "Chưa kết nối".localized(),
      style = MaterialTheme.typography.labelSmall,
      color = if (connected) {
        MaterialTheme.colorScheme.onSecondaryContainer
      } else {
        MaterialTheme.colorScheme.outline
      },
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

/* ------------------------------------------------------- app info & footer */

@Composable
private fun AppInfoLinkRow(title: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
      color = MaterialTheme.colorScheme.onSurface
    )
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp)
    )
  }
}

/* ---------------------------------------------------------------- sheets */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(
  language: String,
  onDismiss: () -> Unit,
  onLanguageSelected: (String) -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Chọn ngôn ngữ".localized(),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      LanguageOptionRow(
        flag = "🇻🇳",
        label = "Tiếng Việt",
        isSelected = language != "en",
        onClick = { onLanguageSelected("vi") }
      )
      LanguageOptionRow(
        flag = "🇬🇧",
        label = "English (UK)",
        isSelected = language == "en",
        onClick = { onLanguageSelected("en") }
      )
    }
  }
}

@Composable
private fun LanguageOptionRow(
  flag: String,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(IconBadgeShape)
      .background(
        if (isSelected) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
      )
      .clickable(onClick = onClick)
      .padding(14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = flag, fontSize = 20.sp)
      Spacer(Modifier.width(12.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    Icon(
      imageVector = Icons.Rounded.CheckCircle,
      contentDescription = null,
      tint = if (isSelected) {
        MaterialTheme.colorScheme.primary
      } else {
        Color.Transparent
      },
      modifier = Modifier.size(20.dp)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogoutSheet(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Đăng xuất khỏi SplitBill?".localized(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = "Dữ liệu sổ thu chi vẫn sẽ được lưu an toàn trên đám mây.".localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
          onClick = onDismiss,
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
          )
        ) {
          Text(text = "Hủy".localized(), style = MaterialTheme.typography.labelLarge)
        }
        Button(
          onClick = onConfirm,
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        ) {
          Text(text = "Đăng xuất".localized(), style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

/* ----------------------------------------------------------------- utils */

private enum class AppInfoPage {
  Terms,
  Privacy
}

@Composable
private fun AppInfoPage.title(): String = when (this) {
  AppInfoPage.Terms -> "Điều khoản sử dụng".localized()
  AppInfoPage.Privacy -> "Chính sách quyền riêng tư".localized()
}

@Composable
private fun AppInfoPage.body(): String = when (this) {
  AppInfoPage.Terms ->
    ("SplitBill là công cụ hỗ trợ ghi lại và chia sẻ chi phí trong nhóm. Bạn chịu trách nhiệm về " +
      "tính chính xác của các khoản chi đã nhập cũng như các thỏa thuận thanh toán giữa các thành viên. " +
      "Vui lòng không sử dụng ứng dụng cho mục đích trái pháp luật.").localized()

  AppInfoPage.Privacy ->
    ("SplitBill chỉ lưu những thông tin cần thiết để vận hành: tên, email, ảnh đại diện và thông tin " +
      "ngân hàng do bạn tự nhập. Ứng dụng không đọc tin nhắn SMS hay nội dung trò chuyện cá nhân. " +
      "Dữ liệu được lưu trữ an toàn trên đám mây và bạn có thể yêu cầu xóa bất cứ lúc nào.").localized()
}

private fun Context.hasNotificationListenerAccess(): Boolean {
  val component = ComponentName(this, com.example.splitbill.service.BankNotificationListener::class.java)
  val enabledListeners = Settings.Secure.getString(
    contentResolver,
    "enabled_notification_listeners"
  )
  return enabledListeners != null && enabledListeners.contains(component.flattenToString())
}
