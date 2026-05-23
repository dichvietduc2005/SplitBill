package com.example.splitbill.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.theme.SplitBillShapes
import com.example.splitbill.ui.components.LoadingState
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.localization.localized

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateToProfile: () -> Unit,
  onLogoutSuccess: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val profileState by viewModel.profileUiState.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
  val language by viewModel.language.collectAsStateWithLifecycle()
  val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
  val pushEnabled by viewModel.pushEnabled.collectAsStateWithLifecycle()

  var showLogoutConfirm by remember { mutableStateOf(false) }
  var showLanguagePicker by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      SplitBillTopBar(
        title = "Cài đặt".localized(),
        canNavigateBack = true,
        onNavigateBack = onNavigateBack
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize(),
      contentPadding = PaddingValues(Dimens.SpacingM),
      verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
    ) {
      
      // Section 1: User Profile Header Card
      item {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
          SplitBillCard(modifier = Modifier.fillMaxWidth()) {
            when (val state = profileState) {
              is SettingsProfileUiState.Loading -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpacingS)
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(24.dp))
                  Spacer(Modifier.width(Dimens.SpacingM))
                  Text("Đang tải thông tin...".localized())
                }
              }
              is SettingsProfileUiState.Error -> {
                ProfileCardContent(
                  username = "SplitBill User",
                  email = "user@splitbill.com",
                  avatarChar = "S"
                )
              }
              is SettingsProfileUiState.Success -> {
                ProfileCardContent(
                  username = state.username,
                  email = state.email,
                  avatarChar = state.username.first().uppercase()
                )
              }
            }
          }
        }
      }

      // Section 2: General Settings Label
      item {
        Text(
          text = "Cài đặt chung".localized(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = Dimens.SpacingXS, top = Dimens.SpacingS)
        )
      }

      // General Settings Card
      item {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
          kotlinx.coroutines.delay(Motion.StaggerDelay)
          visible = true
        }
        AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(1)) {
          SplitBillCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingL)) {
              
              // 2.1 Theme Switcher Row (Sáng / Tối / Hệ thống)
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Palette,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(Dimens.SpacingS))
                    Text(
                      text = "Giao diện".localized(),
                      fontWeight = FontWeight.SemiBold,
                      style = MaterialTheme.typography.bodyLarge
                    )
                  }
                  Text(
                    text = when (themeMode) {
                      "light" -> "Sáng".localized()
                      "dark" -> "Tối".localized()
                      else -> "Hệ thống".localized()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                  )
                }

                Spacer(Modifier.height(Dimens.SpacingS))

                // Beautiful Segmented Control for theme selector
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(SplitBillShapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  val modes = listOf("light" to "Sáng", "dark" to "Tối", "system" to "Hệ thống")
                  modes.forEach { (modeKey, modeLabel) ->
                    val isSelected = themeMode == modeKey
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .clip(SplitBillShapes.small)
                        .background(
                          if (isSelected) MaterialTheme.colorScheme.primary
                          else Color.Transparent
                        )
                        .clickable { viewModel.saveThemeMode(modeKey) }
                        .padding(vertical = 10.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = modeLabel.localized(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                      )
                    }
                  }
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // 2.2 Language Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showLanguagePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Spacer(Modifier.width(Dimens.SpacingS))
                  Text(
                    text = "Ngôn ngữ".localized(),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = if (language == "en") "🇺🇸 English" else "🇻🇳 Tiếng Việt",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(Modifier.width(Dimens.SpacingXS))
                  Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // 2.3 Font Size scale slider with Live Preview
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.FormatSize,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(Dimens.SpacingS))
                    Text(
                      text = "Cỡ chữ".localized(),
                      fontWeight = FontWeight.SemiBold,
                      style = MaterialTheme.typography.bodyLarge
                    )
                  }
                  Text(
                    text = when {
                      fontScale < 0.9f -> "Cỡ chữ: Rất nhỏ".localized()
                      fontScale < 1.0f -> "Cỡ chữ: Nhỏ".localized()
                      fontScale < 1.1f -> "Cỡ chữ: Bình thường".localized()
                      fontScale < 1.25f -> "Cỡ chữ: Lớn".localized()
                      else -> "Cỡ chữ: Rất lớn".localized()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                Spacer(Modifier.height(Dimens.SpacingXS))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    "A-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  
                  Slider(
                    value = fontScale,
                    onValueChange = { viewModel.saveFontScale(it) },
                    valueRange = 0.8f..1.3f,
                    steps = 3,
                    modifier = Modifier
                      .weight(1f)
                      .padding(horizontal = Dimens.SpacingM)
                  )
                  
                  Text(
                    "A+",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }
      }

      // Section 3: Payment Account Title & Item
      item {
        Text(
          text = "Tài khoản thanh toán".localized(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = Dimens.SpacingXS, top = Dimens.SpacingS)
        )
      }

      item {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
          kotlinx.coroutines.delay(2 * Motion.StaggerDelay)
          visible = true
        }
        AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(2)) {
          SplitBillCard(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .background(
                      brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                      ),
                      shape = SplitBillShapes.medium
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(Modifier.width(Dimens.SpacingM))
                Column {
                  Text(
                    text = "Thiết lập VietQR".localized(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                  )
                  Text(
                    text = "Bạn bè quét mã QR để gửi tiền nhanh chóng".localized(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Section 4: Custom Settings (Tùy chỉnh khác)
      item {
        Text(
          text = "Tùy chỉnh khác".localized(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = Dimens.SpacingXS, top = Dimens.SpacingS)
        )
      }

      item {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
          kotlinx.coroutines.delay(3 * Motion.StaggerDelay)
          visible = true
        }
        AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(3)) {
          SplitBillCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingL)) {
              // Notification Toggle
              CustomSettingRow(
                icon = Icons.Default.NotificationsActive,
                title = "Thông báo đẩy".localized(),
                subtitle = "Bật thông báo khi có hóa đơn mới".localized(),
                checked = pushEnabled,
                onCheckedChange = { viewModel.savePushEnabled(it) }
              )

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // Biometric
              CustomSettingRow(
                icon = Icons.Default.Fingerprint,
                title = "Bảo mật sinh trắc học".localized(),
                subtitle = "Sử dụng vân tay/khuôn mặt".localized(),
                checked = biometricEnabled,
                onCheckedChange = { viewModel.saveBiometricEnabled(it) }
              )
            }
          }
        }
      }

      // Section 5: Logout Action Button
      item {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
          kotlinx.coroutines.delay(4 * Motion.StaggerDelay)
          visible = true
        }
        Spacer(Modifier.height(Dimens.SpacingS))
        AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(4)) {
          Button(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier
              .fillMaxWidth()
              .height(Dimens.ButtonHeight),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error
            ),
            shape = SplitBillShapes.medium
          ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(Dimens.SpacingS))
            Text(
              text = "Đăng xuất".localized(),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }

      item { Spacer(Modifier.height(Dimens.SpacingXL)) }
    }
  }

  // 6. Language Selection
  if (showLanguagePicker) {
    AlertDialog(
      onDismissRequest = { showLanguagePicker = false },
      title = {
        Text(
          "Chọn ngôn ngữ".localized(),
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingS)) {
          LanguageOptionItem(
            flag = "🇻🇳",
            label = "Tiếng Việt",
            isSelected = language == "vi",
            onClick = {
              viewModel.saveLanguage("vi")
              showLanguagePicker = false
            }
          )
          LanguageOptionItem(
            flag = "🇺🇸",
            label = "English",
            isSelected = language == "en",
            onClick = {
              viewModel.saveLanguage("en")
              showLanguagePicker = false
            }
          )
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showLanguagePicker = false }) {
          Text("Hủy".localized())
        }
      },
      shape = SplitBillShapes.large
    )
  }

  // 7. Premium Spring Logout Confirmation Dialog
  if (showLogoutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      icon = {
        Icon(
          Icons.Default.Warning,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          "Xác nhận đăng xuất".localized(),
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          "Bạn có chắc chắn muốn đăng xuất?".localized(),
          style = MaterialTheme.typography.bodyLarge
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirm = false
            viewModel.logout {
              onLogoutSuccess()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = SplitBillShapes.medium
        ) {
          Text("Đăng xuất".localized(), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) {
          Text("Hủy".localized())
        }
      },
      shape = SplitBillShapes.large
    )
  }
}

@Composable
private fun ProfileCardContent(
  username: String,
  email: String,
  avatarChar: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Surface(
      shape = SplitBillShapes.medium,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier
        .size(64.dp)
        .border(
          width = 1.5.dp,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
          shape = SplitBillShapes.medium
        )
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
          text = avatarChar,
          style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )
      }
    }
    
    Spacer(Modifier.width(Dimens.SpacingM))
    
    Column {
      Text(
        text = username,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = email,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun CustomSettingRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(Modifier.width(Dimens.SpacingM))
      Column {
        Text(
          text = title,
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodyLarge
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange
    )
  }
}

@Composable
private fun LanguageOptionItem(
  flag: String,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.outlineVariant
    )
  ) {
    Row(
      modifier = Modifier
        .padding(Dimens.SpacingM)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(flag, fontSize = 24.sp)
        Spacer(Modifier.width(Dimens.SpacingM))
        Text(
          text = label,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          style = MaterialTheme.typography.bodyLarge
        )
      }
      if (isSelected) {
        Icon(
          Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}
