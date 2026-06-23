package com.example.splitbill.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.theme.SplitBillShapes
import com.example.splitbill.ui.components.LoadingState
import com.example.splitbill.ui.localization.localized
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  viewModel: LoginViewModel,
  onLoginSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val settingsManager = remember { com.example.splitbill.data.SettingsManager(context) }
  val biometricEnabled by settingsManager.biometricEnabled.collectAsState(initial = false)
  val hasBiometricToken by viewModel.hasBiometricToken.collectAsStateWithLifecycle()
  
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isLoginMode by remember { mutableStateOf(true) }
  var showPassword by remember { mutableStateOf(false) }
  var showBiometricQuickSheet by remember { mutableStateOf(false) }
  var biometricErrorMsg by remember { mutableStateOf<String?>(null) }

  // Staggered entrance states
  var showLogo by remember { mutableStateOf(false) }
  var showSubtitle by remember { mutableStateOf(false) }
  var showCard by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.resetState()
    viewModel.checkLoginStatus()
    viewModel.checkBiometricAvailability()
    showLogo = true
    kotlinx.coroutines.delay(150)
    showSubtitle = true
    kotlinx.coroutines.delay(150)
    showCard = true
  }

  LaunchedEffect(uiState) {
    if (uiState is LoginUiState.Success) {
      viewModel.resetState()
      onLoginSuccess()
    }
  }

  // Shake Error feedback using Animatable offset
  val shakeOffset = remember { Animatable(0f) }
  LaunchedEffect(uiState) {
    if (uiState is LoginUiState.Error) {
      val shakeSpec = keyframes<Float> {
        durationMillis = 500
        0f at 0
        20f at 100
        -20f at 200
        15f at 300
        -15f at 400
        0f at 500
      }
      shakeOffset.animateTo(0f, animationSpec = shakeSpec)
    }
  }

  // Logo pulse and rotation transition
  val infiniteTransition = rememberInfiniteTransition(label = "premium_anims")
  val logoScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "logo_scale"
  )

  // Fluid background Orbs movement angles
  val angle1 by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(18000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "orb_angle_1"
  )
  val angle2 by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(24000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "orb_angle_2"
  )

  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1. Fluid Canvas Glow Orbs (Premium Design Aesthetic)
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .blur(80.dp) // Blur creates the soft organic lighting look
        .graphicsLayer(alpha = 0.45f)
    ) {
      val cx = size.width / 2f
      val cy = size.height / 2f

      // Orb 1: Primary color
      val orb1X = cx + cos(angle1) * (cx * 0.5f)
      val orb1Y = cy + sin(angle1) * (cy * 0.4f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFF6750A4), Color.Transparent),
          center = Offset(orb1X, orb1Y),
          radius = size.width * 0.45f
        ),
        radius = size.width * 0.45f,
        center = Offset(orb1X, orb1Y)
      )

      // Orb 2: Secondary / Teal color
      val orb2X = cx + cos(angle2 + Math.PI.toFloat()) * (cx * 0.6f)
      val orb2Y = cy + sin(angle2) * (cy * 0.3f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFF03DAC5), Color.Transparent),
          center = Offset(orb2X, orb2Y),
          radius = size.width * 0.4f
        ),
        radius = size.width * 0.4f,
        center = Offset(orb2X, orb2Y)
      )

      // Orb 3: Accent Warm Orange color for richness
      val orb3X = cx + sin(angle1) * (cx * 0.4f)
      val orb3Y = cy + cos(angle2) * (cy * 0.5f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFFFFB74D), Color.Transparent),
          center = Offset(orb3X, orb3Y),
          radius = size.width * 0.35f
        ),
        radius = size.width * 0.35f,
        center = Offset(orb3X, orb3Y)
      )
    }

    // Main Content Layout
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(Dimens.SpacingXL)
    ) {
      
      // Logo (SplitBill)
      AnimatedVisibility(
        visible = showLogo,
        enter = fadeIn(Motion.tweenSlow()) + slideInVertically(
          animationSpec = Motion.tweenEntrance(),
          initialOffsetY = { -it / 2 }
        )
      ) {
        Text(
          text = "SplitBill",
          style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp
          ),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.scale(logoScale)
        )
      }

      // Premium Subtitle
      AnimatedVisibility(
        visible = showSubtitle,
        enter = fadeIn(Motion.tweenMedium()) + slideInVertically(
          animationSpec = Motion.tweenSlow(),
          initialOffsetY = { it / 3 }
        )
      ) {
        Text(
          text = "Chia tiền dễ dàng, tình cảm bền lâu".localized(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          modifier = Modifier.padding(top = Dimens.SpacingXS)
        )
      }
      
      Spacer(modifier = Modifier.height(Dimens.SpacingXXL))

      // 2. Glassmorphism Card Wrapper with Shake Animation
      AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(Motion.tweenSlow()) + slideInVertically(
          animationSpec = Motion.tweenEntrance(),
          initialOffsetY = { it / 4 }
        )
      ) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shakeOffset.value }
            .shadow(
              elevation = 16.dp,
              shape = SplitBillShapes.large,
              spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
            .border(
              width = 1.dp,
              brush = Brush.verticalGradient(
                listOf(
                  Color.White.copy(alpha = 0.18f),
                  Color.White.copy(alpha = 0.04f)
                )
              ),
              shape = SplitBillShapes.large
            ),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
          ),
          shape = SplitBillShapes.large
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(Dimens.SpacingL),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            
            // Mode Header (Đăng nhập / Đăng ký switch)
            TabRow(
              selectedTabIndex = if (isLoginMode) 0 else 1,
              containerColor = Color.Transparent,
              contentColor = MaterialTheme.colorScheme.primary,
              divider = {},
              indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                  modifier = Modifier.tabIndicatorOffset(tabPositions[if (isLoginMode) 0 else 1]),
                  color = MaterialTheme.colorScheme.primary,
                  height = 3.dp
                )
              },
              modifier = Modifier.fillMaxWidth(0.8f)
            ) {
              Tab(
                selected = isLoginMode,
                onClick = { isLoginMode = true },
                text = {
                  Text(
                    "Đăng Nhập".localized(),
                    fontWeight = if (isLoginMode) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium
                  )
                }
              )
              Tab(
                selected = !isLoginMode,
                onClick = { isLoginMode = false },
                text = {
                  Text(
                    "Đăng Ký".localized(),
                    fontWeight = if (!isLoginMode) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium
                  )
                }
              )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXL))

            // Error display
            if (uiState is LoginUiState.Error) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = Dimens.SpacingM)
              ) {
                Row(
                  modifier = Modifier.padding(Dimens.SpacingM),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                  Spacer(modifier = Modifier.width(Dimens.SpacingS))
                  Text(
                    text = (uiState as LoginUiState.Error).message.localized(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                  )
                }
              }
            }

            // Input Fields
            OutlinedTextField(
              value = username,
              onValueChange = { username = it },
              label = { Text("Tên đăng nhập".localized()) },
              leadingIcon = {
                Icon(
                  Icons.Default.Person,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
              },
              trailingIcon = {
                if (username.isNotEmpty()) {
                  IconButton(onClick = { username = "" }) {
                    Icon(Icons.Default.Cancel, contentDescription = "Clear")
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = SplitBillShapes.medium,
              singleLine = true,
              colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
              )
            )
            
            Spacer(modifier = Modifier.height(Dimens.SpacingM))
            
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Mật khẩu".localized()) },
              leadingIcon = {
                Icon(
                  Icons.Default.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
              },
              trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                  Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Show/Hide password"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = SplitBillShapes.medium,
              singleLine = true,
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
              )
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXL))

            // Action morph button or progress bar
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.fillMaxWidth()
            ) {
              if (uiState is LoginUiState.Loading) {
                Surface(
                  modifier = Modifier
                    .size(56.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                  ) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(32.dp),
                      strokeWidth = 3.dp,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              } else {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingM),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  com.example.splitbill.ui.components.GradientButton(
                    onClick = {
                      if (isLoginMode) {
                        viewModel.login(username.trim(), password)
                      } else {
                        viewModel.register(username.trim(), password)
                      }
                    },
                    modifier = Modifier
                      .weight(1f)
                      .height(Dimens.ButtonHeight),
                    shape = SplitBillShapes.medium,
                    enabled = username.isNotBlank() && password.isNotBlank()
                  ) {
                    Text(
                      text = if (isLoginMode) "Đăng Nhập".localized() else "Đăng Ký".localized(),
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color.White
                    )
                  }

                  if (isLoginMode && biometricEnabled && hasBiometricToken) {
                    FilledIconButton(
                      onClick = { 
                        showBiometricQuickSheet = true 
                        biometricErrorMsg = null
                      },
                      modifier = Modifier.size(Dimens.ButtonHeight),
                      shape = SplitBillShapes.medium,
                      colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                      )
                    ) {
                      Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric login",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingS))

            // Subtle Mode Switch helper label
            TextButton(
              onClick = { isLoginMode = !isLoginMode },
              colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
              Text(
                text = if (isLoginMode) "Chưa có tài khoản? Đăng ký ngay".localized() else "Đã có tài khoản? Đăng nhập".localized(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
              )
            }
          }
        }
      }
    }

    // 3. Biometric Quick Login Overlay Handling
    if (showBiometricQuickSheet) {
      val activity = LocalContext.current as? androidx.fragment.app.FragmentActivity
      LaunchedEffect(activity) {
        if (activity != null) {
          BiometricHelper.showBiometricPrompt(
            activity = activity,
            onSuccess = {
              showBiometricQuickSheet = false
              viewModel.loginWithBiometrics()
            },
            onError = { err ->
              showBiometricQuickSheet = false
              biometricErrorMsg = err
            }
          )
        } else {
          showBiometricQuickSheet = false
          biometricErrorMsg = "Không thể mở xác thực sinh trắc học"
        }
      }
    }

    // Display Biometric Error if any
    biometricErrorMsg?.let { msg ->
      LaunchedEffect(msg) {
        // We can show a snackbar or let the viewmodel handle error state.
        // For now, let's just use the viewmodel's error state.
        // But since we can't directly set uiState, we can clear it after a delay.
        kotlinx.coroutines.delay(3000)
        biometricErrorMsg = null
      }
      Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = SplitBillShapes.medium
        ) {
          Text(
            text = msg,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }
    }
  }
}
