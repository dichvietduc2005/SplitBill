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
  // Lấy màu từ theme để thanh bar và nút (+) đổi theo sáng/tối
  // (ở theme sáng các màu này trùng đúng #F9F9F6 và #011C27 như trước)
  //
  // Thanh bar phải đục hoàn toàn: nội dung của các tab được phép cuộn phía sau nó,
  // nếu để alpha < 1 sẽ thấy chữ mờ xuyên qua thanh khi cuộn.
  val barColor = MaterialTheme.colorScheme.surface
  val fabColor = MaterialTheme.colorScheme.primary
  val fabContentColor = MaterialTheme.colorScheme.onPrimary

  // Sử dụng Box bao quanh ngoài cùng với chiều cao đủ để chứa nút tròn nhô lên trên mà không bị cắt
  Box(
    modifier = modifier
      .fillMaxWidth()
      .wrapContentHeight(),
    contentAlignment = Alignment.BottomCenter
  ) {
    // 1. Thanh nền Navigation Bar bên dưới
    Surface(
      color = barColor,
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .height(Dimens.BottomBarHeight)
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

          // Tab 3: Cá nhân
          BottomNavButton(
            title = "Cá nhân",
            icon = Icons.Rounded.Person,
            isSelected = selectedTab == HomeTab.Profile,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTabSelected(HomeTab.Profile)
            }
          )

          // Tab 4: Cài đặt
          BottomNavButton(
            title = "Cài đặt",
            icon = Icons.Rounded.Settings,
            isSelected = selectedTab == HomeTab.Settings,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTabSelected(HomeTab.Settings)
            }
          )
        }
      }
    }

    // 2. Nút (+) tròn nổi độc lập (đè lên phía trên thanh điều hướng, không bị cắt)
    //
    // Dùng padding chứ không phải offset: vùng chạm của nút phải nằm trong khung của
    // thanh bar, nếu vẽ tràn ra ngoài thì Compose không nhận được cú chạm ở nửa trên.
    // Nền phía sau phần nhô lên là nền của chính màn hình tab (Navigator không pad đáy
    // cho nội dung nữa), nên không lộ dải màu lạ ở bất kỳ theme nào.
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = Dimens.BottomBarOverhang)
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
          .background(fabColor)
          .clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onAddClick()
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = "Thêm khoản chi mới",
          tint = fabContentColor,
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
  val color = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
  }

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
