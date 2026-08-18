package com.example.splitbill.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitbill.data.api.MonthlySpent
import com.example.splitbill.theme.Dimens

/**
 * Biểu đồ cột chi tiêu theo tháng đơn giản & tinh tế (Simple Monthly Bar Chart with Top Month Nav).
 */
@Composable
fun MonthlyBarChart(
  monthlyData: List<MonthlySpent>,
  modifier: Modifier = Modifier
) {
  if (monthlyData.isEmpty()) return

  // Default to the last month (most recent month)
  var selectedIndex by remember(monthlyData) {
    mutableIntStateOf(monthlyData.lastIndex)
  }

  val selectedItem = monthlyData.getOrNull(selectedIndex) ?: monthlyData.last()

  val maxAmount = remember(monthlyData) {
    monthlyData.maxOf { it.amount }.coerceAtLeast(1.0)
  }

  // Animation trigger for height bars
  var animTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(monthlyData) {
    animTrigger = true
  }

  val animProgress by animateFloatAsState(
    targetValue = if (animTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "simple_bar_anim"
  )

  Column(
    modifier = modifier.fillMaxWidth()
  ) {
    // ─── 1. Header: Month Navigation (< Tháng 08/2026 >) & Total Amount ───
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = Dimens.SpacingM),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Prev Month Button (<)
      IconButton(
        onClick = { if (selectedIndex > 0) selectedIndex-- },
        enabled = selectedIndex > 0,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
          contentDescription = "Tháng trước",
          tint = if (selectedIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
      }

      // Center Month & Amount
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = "Tháng ${selectedItem.month}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Spacer(Modifier.height(2.dp))
        AmountText(
          amount = selectedItem.amount,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )
      }

      // Next Month Button (>)
      IconButton(
        onClick = { if (selectedIndex < monthlyData.lastIndex) selectedIndex++ },
        enabled = selectedIndex < monthlyData.lastIndex,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = "Tháng sau",
          tint = if (selectedIndex < monthlyData.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
      }
    }

    // ─── 2. Simple & Clean Bar Chart ───
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .padding(horizontal = Dimens.SpacingS),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.Bottom
    ) {
      monthlyData.forEachIndexed { index, item ->
        val isSelected = index == selectedIndex
        val heightRatio = ((item.amount / maxAmount).toFloat() * animProgress).coerceIn(0.05f, 1f)

        val shortAmount = remember(item.amount) {
          when {
            item.amount >= 1_000_000 -> "${(item.amount / 1_000_000).let { if (it % 1 == 0.0) it.toInt() else String.format("%.1f", it) }}M"
            item.amount >= 1_000 -> "${(item.amount / 1000).toInt()}k"
            else -> "${item.amount.toInt()}"
          }
        }

        val shortMonth = remember(item.month) {
          val parts = item.month.split("-", "/")
          if (parts.size >= 2) "T${parts[0]}" else item.month
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) { selectedIndex = index },
          verticalArrangement = Arrangement.Bottom
        ) {
          // Amount text on top
          Text(
            text = shortAmount,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
          )

          // Simple rounded bar column
          Box(
            modifier = Modifier
              .fillMaxHeight(0.75f)
              .width(40.dp),
            contentAlignment = Alignment.BottomCenter
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightRatio)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
          }

          Spacer(Modifier.height(6.dp))

          // Month label below
          Text(
            text = shortMonth,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
