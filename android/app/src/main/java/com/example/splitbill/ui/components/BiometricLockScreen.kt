package com.example.splitbill.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.localization.localized
import kotlinx.coroutines.delay

@Composable
fun BiometricLockScreen(onUnlockSuccess: () -> Unit) {
  var isScanning by remember { mutableStateOf(false) }
  var isSuccess by remember { mutableStateOf(false) }

  // Fingerprint pulse animation
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  LaunchedEffect(isScanning) {
    if (isScanning) {
      delay(1200) // Simulate scanning
      isSuccess = true
      delay(400) // Success delay
      onUnlockSuccess()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.background
          )
        )
      )
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 64.dp, horizontal = Dimens.SpacingXL)
    ) {
      
      // Top section: App Branding and scanning Status
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "SplitBill Secure",
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingS))

        Text(
          text = if (isSuccess) "Xác thực thành công".localized()
          else if (isScanning) "Đang quét vân tay...".localized()
          else "Chạm để quét vân tay".localized(),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Bottom section: Ergonomic Fingerprint Scanner Mimic (Familiar in-display smartphone placement)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 32.dp)
      ) {
        Surface(
          modifier = Modifier
            .size(96.dp)
            .scale(if (isScanning && !isSuccess) pulseScale else 1f)
            .clip(CircleShape)
            .clickable(enabled = !isScanning) { isScanning = true },
          color = if (isSuccess) MaterialTheme.colorScheme.primary
          else if (isScanning) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
          else MaterialTheme.colorScheme.surfaceVariant,
          shape = CircleShape,
          tonalElevation = 8.dp
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = "Scan",
              tint = if (isSuccess) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(54.dp)
            )
          }
        }
        
        Spacer(modifier = Modifier.height(Dimens.SpacingM))
        
        Text(
          text = "Cảm biến vân tay".localized(),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
      }
    }
  }
}
