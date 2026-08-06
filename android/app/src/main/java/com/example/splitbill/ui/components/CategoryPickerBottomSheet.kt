package com.example.splitbill.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitbill.data.model.BillCategory
import com.example.splitbill.theme.Dimens

/**
 * Bottom Sheet lưới 3 cột chọn danh mục hóa đơn — theo phong cách Money Lover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
  selectedCategory: BillCategory,
  onCategorySelected: (BillCategory) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    dragHandle = { BottomSheetDefaults.DragHandle() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.SpacingM)
        .padding(bottom = Dimens.SpacingXL)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Danh mục",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${BillCategory.entries.size} lựa chọn",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(Modifier.height(Dimens.SpacingXS))

      Text(
        text = "Chọn danh mục phù hợp nhất với giao dịch này",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(Modifier.height(Dimens.SpacingM))

      // Category Grid — 3 columns
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(BillCategory.entries.toList()) { category ->
          CategoryGridItem(
            category = category,
            isSelected = category == selectedCategory,
            onClick = {
              onCategorySelected(category)
              onDismiss()
            }
          )
        }
      }
    }
  }
}

@Composable
private fun CategoryGridItem(
  category: BillCategory,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
    label = "category_border"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .then(
        if (isSelected) Modifier
          .border(2.dp, borderColor, RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        else Modifier
      )
      .padding(vertical = Dimens.SpacingS, horizontal = Dimens.SpacingXS)
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(category.bgColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = category.icon,
        contentDescription = category.displayName,
        tint = category.iconColor,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(Modifier.height(6.dp))

    Text(
      text = category.displayName,
      style = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      ),
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * Hàng chọn nhanh 4 danh mục + nút "Tất cả" — dùng trong AddBillScreen.
 */
@Composable
fun CategoryQuickPicker(
  selectedCategory: BillCategory,
  onCategorySelected: (BillCategory) -> Unit,
  onShowAllCategories: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Danh mục",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      TextButton(onClick = onShowAllCategories) {
        Text(
          text = "Xem tất cả",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }

    Spacer(Modifier.height(Dimens.SpacingXS))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
    ) {
      BillCategory.quickPick.forEach { category ->
        val isSelected = category == selectedCategory

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCategorySelected(category) }
            .then(
              if (isSelected) Modifier
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
              else Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            .padding(vertical = Dimens.SpacingS)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(category.bgColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = category.icon,
              contentDescription = category.displayName,
              tint = category.iconColor,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 11.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
