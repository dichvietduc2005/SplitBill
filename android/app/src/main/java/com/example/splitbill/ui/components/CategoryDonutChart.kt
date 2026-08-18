package com.example.splitbill.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitbill.data.model.BillCategory
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.stats.CategorySpending
import kotlin.math.atan2

/**
 * Biểu đồ hình tròn phân bố chi tiêu theo danh mục (Clean Donut Chart).
 * Tự động đổi màu chữ, màu nền và viền theo đúng màu danh mục được chọn (Đỏ cam cho Ăn uống, Xanh cho Di chuyển...).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CategoryDonutChart(
  categoryBreakdown: List<CategorySpending>,
  totalAmount: Double,
  modifier: Modifier = Modifier
) {
  if (categoryBreakdown.isEmpty()) return

  var selectedIndex by remember { mutableIntStateOf(-1) }
  var animTrigger by remember { mutableStateOf(false) }

  LaunchedEffect(categoryBreakdown) {
    animTrigger = true
  }

  val progressAnim by animateFloatAsState(
    targetValue = if (animTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "clean_donut_anim"
  )

  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxWidth()
  ) {
    // --- Sleek Canvas Donut Ring ---
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(220.dp)
        .padding(8.dp)
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(categoryBreakdown) {
            detectTapGestures { tapOffset ->
              val center = Offset(size.width / 2f, size.height / 2f)
              val dx = tapOffset.x - center.x
              val dy = tapOffset.y - center.y
              val distance = kotlin.math.sqrt(dx * dx + dy * dy)
              val outerRadius = size.width / 2f
              val innerRadius = outerRadius * 0.65f

              if (distance in innerRadius..outerRadius) {
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angle < 0) angle += 360f
                val adjustedAngle = (angle + 90f) % 360f

                var currentAngle = 0f
                categoryBreakdown.forEachIndexed { index, catSpending ->
                  val sweep = (catSpending.percentage / 100f) * 360f
                  if (adjustedAngle in currentAngle..(currentAngle + sweep)) {
                    selectedIndex = if (selectedIndex == index) -1 else index
                    return@detectTapGestures
                  }
                  currentAngle += sweep
                }
              } else {
                selectedIndex = -1
              }
            }
          }
      ) {
        val strokeWidth = 22.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        // 1. Background Track Circle
        drawArc(
          color = trackColor,
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidth)
        )

        // 2. Animated Category Slices
        var startAngle = -90f
        categoryBreakdown.forEachIndexed { index, catSpending ->
          val category = BillCategory.fromKey(catSpending.categoryKey)
          val rawSweep = (catSpending.percentage / 100f) * 360f * progressAnim
          val sweepAngle = (rawSweep - 2.5f).coerceAtLeast(0.1f) // 2.5deg clean gap
          val isSelected = selectedIndex == index
          val currentStrokeWidth = if (isSelected) strokeWidth * 1.3f else strokeWidth

          drawArc(
            color = category.iconColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
          )

          startAngle += rawSweep
        }
      }

      // --- Central Typography ---
      AnimatedContent(
        targetState = selectedIndex,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "donut_center_content"
      ) { targetIndex ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = Dimens.SpacingM)
        ) {
          if (targetIndex in categoryBreakdown.indices) {
            val selectedCat = categoryBreakdown[targetIndex]
            val category = BillCategory.fromKey(selectedCat.categoryKey)

            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(category.iconColor.copy(alpha = 0.15f))
                .border(1.dp, category.iconColor, CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = category.iconColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(Modifier.height(4.dp))
            AmountText(
              amount = selectedCat.totalAmount,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = category.iconColor)
            )
            Text(
              text = "${String.format(java.util.Locale.US, "%.1f", selectedCat.percentage)}%",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = category.iconColor.copy(alpha = 0.85f)
            )
          } else {
            Text(
              text = "TỔNG CHI TIÊU",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(2.dp))
            AmountText(
              amount = totalAmount,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = "${categoryBreakdown.size} danh mục",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    Spacer(Modifier.height(Dimens.SpacingM))

    // --- Refined Dynamic Color Legend List ---
    Column(
      verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXS),
      modifier = Modifier.fillMaxWidth()
    ) {
      categoryBreakdown.forEachIndexed { index, catSpending ->
        val category = BillCategory.fromKey(catSpending.categoryKey)
        val isSelected = selectedIndex == index

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
              if (isSelected) Modifier
                .background(category.iconColor.copy(alpha = 0.15f))
                .border(1.5.dp, category.iconColor, RoundedCornerShape(14.dp))
              else Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            )
            .clickable { selectedIndex = if (selectedIndex == index) -1 else index }
            .padding(horizontal = Dimens.SpacingM, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) category.iconColor.copy(alpha = 0.25f) else category.bgColor),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = category.iconColor,
                modifier = Modifier.size(16.dp)
              )
            }

            Spacer(Modifier.width(Dimens.SpacingS))

            Column {
              Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) category.iconColor else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${String.format(java.util.Locale.US, "%.1f", catSpending.percentage)}%",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) category.iconColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          AmountText(
            amount = catSpending.totalAmount,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = if (isSelected) category.iconColor else MaterialTheme.colorScheme.onSurface
            )
          )
        }
      }
    }
  }
}
