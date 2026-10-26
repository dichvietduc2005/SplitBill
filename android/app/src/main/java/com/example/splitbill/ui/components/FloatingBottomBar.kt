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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
  Home,
  Groups,
  Debts,
  Profile,
  Settings
}

@Composable
fun FloatingBottomBar(
  selectedTab: HomeTab,
  onTabSelected: (HomeTab) -> Unit,
  onAddClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current

  // Sử dụng Box bao quanh ngoài cùng với chiều cao đủ để chứa nút tròn nhô lên trên mà không bị cắt
  Box(
    modifier = modifier
      .fillMaxWidth()
      .wrapContentHeight(),
    contentAlignment = Alignment.BottomCenter
  ) {
    // 1. Thanh nền Navigation Bar bên dưới
    Surface(
      color = Color(0xFFF9F9F6).copy(alpha = 0.96f),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .height(64.dp)
          .padding(horizontal = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          // Tab 1: Trang chủ
          BottomNavButton(
            title = "Trang chủ",
            icon = Icons.Rounded.Home,
            isSelected = selectedTab == HomeTab.Home,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTabSelected(HomeTab.Home)
            }
          )

          // Tab 2: Nhóm
          BottomNavButton(
            title = "Nhóm",
            icon = Icons.Rounded.Group,
            isSelected = selectedTab == HomeTab.Groups,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTabSelected(HomeTab.Groups)
            }
          )

          // Chỗ trống chính giữa để nhường chỗ cho nút tròn nổi
          Spacer(modifier = Modifier.width(56.dp))

          // Tab 4: Công nợ
          BottomNavButton(
            title = "Công nợ",
            icon = Icons.Rounded.AccountBalanceWallet,
            isSelected = selectedTab == HomeTab.Debts,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTabSelected(HomeTab.Debts)
            }
          )

          // Tab 5: Cá nhân
          BottomNavButton(
            title = "Cá nhân",
            icon = Icons.Rounded.Person,
            isSelected = selectedTab == HomeTab.Profile,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTabSelected(HomeTab.Profile)
            }
          )
        }
      }
    }

    // 2. Nút (+) tròn nổi độc lập (đè lên phía trên thanh điều hướng, không bị cắt)
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = 32.dp) // Nhô cao 32dp đè lên thanh 64dp
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .shadow(
            elevation = 12.dp,
            shape = CircleShape,
            spotColor = Color(0xFF011C27).copy(alpha = 0.45f)
          )
          .clip(CircleShape)
          .background(Color(0xFF011C27))
          .clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onAddClick()
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = "Thêm khoản chi mới",
          tint = Color(0xFFFFFFFF),
          modifier = Modifier.size(28.dp)
        )
      }
    }
  }
}

@Composable
private fun BottomNavButton(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val color = if (isSelected) Color(0xFF011C27) else Color(0xFF546068).copy(alpha = 0.75f)

  Column(
    modifier = Modifier
      .width(56.dp)
      .fillMaxHeight()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = color,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = title,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
      color = color,
      maxLines = 1
    )
  }
}
