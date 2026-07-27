package com.example.splitbill.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPageData(
  val title: String,
  val description: String,
  val icon: ImageVector,
  val colorGradient: List<Color>
)

@Composable
fun OnboardingScreen(
  onFinishOnboarding: () -> Unit,
  modifier: Modifier = Modifier
) {
  val pages = listOf(
    OnboardingPageData(
      title = "Chia Hóa Đơn Dễ Dàng",
      description = "Tạo hóa đơn, tự động tính toán số tiền mỗi thành viên cần trả một cách chính xác tuyệt đối.",
      icon = Icons.Default.Group,
      colorGradient = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
    ),
    OnboardingPageData(
      title = "Thanh Toán VietQR Thông Minh",
      description = "Quét mã VietQR tự động điền số tiền và nội dung. Đọc thông báo ngân hàng để tự động gạch nợ!",
      icon = Icons.Default.QrCodeScanner,
      colorGradient = listOf(Color(0xFF059669), Color(0xFF10B981))
    ),
    OnboardingPageData(
      title = "Thống Kê Chi Tiêu Trực Quan",
      description = "Theo dõi dòng tiền nhóm, biểu đồ chi tiêu và xuất báo cáo PDF/CSV bất cứ lúc nào.",
      icon = Icons.Default.AccountBalanceWallet,
      colorGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
    )
  )

  val pagerState = rememberPagerState(pageCount = { pages.size })
  val coroutineScope = rememberCoroutineScope()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top row: Skip button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        if (pagerState.currentPage < pages.size - 1) {
          TextButton(onClick = onFinishOnboarding) {
            Text("Bỏ qua", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
          }
        } else {
          Spacer(modifier = Modifier.height(48.dp))
        }
      }

      Spacer(modifier = Modifier.weight(0.2f))

      // Pager Content
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) { pageIndex ->
        val page = pages[pageIndex]
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          // Illustration Hero Circle
          Box(
            modifier = Modifier
              .size(200.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(page.colorGradient)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = page.icon,
              contentDescription = null,
              modifier = Modifier.size(96.dp),
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.height(40.dp))

          Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      }

      // Page Indicators
      Row(
        modifier = Modifier.padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center
      ) {
        repeat(pages.size) { index ->
          val isSelected = pagerState.currentPage == index
          val width by animateDpAsState(
            targetValue = if (isSelected) 32.dp else 10.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
          )
          val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

          Box(
            modifier = Modifier
              .padding(4.dp)
              .height(10.dp)
              .width(width)
              .clip(CircleShape)
              .background(color)
          )
        }
      }

      // Bottom Button
      Button(
        onClick = {
          if (pagerState.currentPage < pages.size - 1) {
            coroutineScope.launch {
              pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
          } else {
            onFinishOnboarding()
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = if (pagerState.currentPage == pages.size - 1) "Bắt đầu ngay" else "Tiếp tục",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = if (pagerState.currentPage == pages.size - 1) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null
          )
        }
      }
    }
  }
}
