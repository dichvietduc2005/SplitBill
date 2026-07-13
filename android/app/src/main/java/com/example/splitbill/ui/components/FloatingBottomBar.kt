package com.example.splitbill.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.localization.localized

enum class HomeTab {
  Groups,
  Profile,
  Settings
}

@Composable
fun FloatingBottomBar(
  selectedTab: HomeTab,
  onTabSelected: (HomeTab) -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val tabs = listOf(
    TabItem(HomeTab.Groups, "Nhóm".localized(), Icons.Default.Groups),
    TabItem(HomeTab.Profile, "Cá nhân".localized(), Icons.Default.Person),
    TabItem(HomeTab.Settings, "Cài đặt".localized(), Icons.Default.Settings)
  )

  Box(
    modifier = modifier
      .windowInsetsPadding(WindowInsets.navigationBars) // Tương thích với thanh điều hướng hệ thống (phím ảo hoặc vuốt cử chỉ)
      .padding(horizontal = 24.dp)
      .padding(bottom = 16.dp)
      .shadow(
        elevation = 16.dp, // Premium soft shadow
        shape = RoundedCornerShape(24.dp),
        spotColor = Color.Black.copy(alpha = 0.15f),
        ambientColor = Color.Black.copy(alpha = 0.05f)
      )
      .clip(RoundedCornerShape(24.dp))
      .background(MaterialTheme.colorScheme.surface)
      .height(64.dp)
      .fillMaxWidth()
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val tabWidth = maxWidth / tabs.size
      
      // Sliding active pill indicator
      val animatedOffset by animateDpAsState(
        targetValue = when (selectedTab) {
          HomeTab.Groups -> 0.dp
          HomeTab.Profile -> tabWidth
          HomeTab.Settings -> tabWidth * 2
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "pill_offset"
      )

      Box(
        modifier = Modifier
          .offset(x = animatedOffset)
          .width(tabWidth)
          .fillMaxHeight()
          .padding(horizontal = 8.dp, vertical = 8.dp)
          .background(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(18.dp)
          )
      )

      // Tab items
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        tabs.forEach { tab ->
          val isSelected = tab.type == selectedTab
          val contentColor by animateColorAsState(
            targetValue = if (isSelected) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
            animationSpec = tween(180),
            label = "tab_color"
          )

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Disabling default ripple since we have the sliding pill
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTabSelected(tab.type)
              },
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 8.dp)
            ) {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
              )
              if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = tab.label,
                  color = contentColor,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }
}

private data class TabItem(
  val type: HomeTab,
  val label: String,
  val icon: ImageVector
)
