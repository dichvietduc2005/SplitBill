package com.example.splitbill.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
  onDismiss: () -> Unit,
  onExportPdf: () -> Unit,
  onExportCsv: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(Dimens.SpacingL)
        .padding(bottom = Dimens.SpacingXL)
    ) {
      Text(
        "Xuất Báo Cáo Chi Tiêu",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        "Chọn định dạng file bạn muốn xuất và chia sẻ",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(Dimens.SpacingL))

      ExportOptionCard(
        title = "Xuất file PDF (.pdf)",
        description = "Báo cáo đẹp mắt có bảng kê chi tiết, sẵn sàng để in hoặc gửi file",
        icon = Icons.Default.PictureAsPdf,
        iconTint = MaterialTheme.colorScheme.error,
        onClick = {
          onDismiss()
          onExportPdf()
        }
      )

      Spacer(modifier = Modifier.height(Dimens.SpacingM))

      ExportOptionCard(
        title = "Xuất file CSV (.csv)",
        description = "File dữ liệu bảng tính, dễ dàng mở bằng Excel hoặc Google Sheets",
        icon = Icons.Default.Description,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = {
          onDismiss()
          onExportCsv()
        }
      )
    }
  }
}

@Composable
private fun ExportOptionCard(
  title: String,
  description: String,
  icon: ImageVector,
  iconTint: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
  ) {
    Row(
      modifier = Modifier.padding(Dimens.SpacingM),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.width(Dimens.SpacingM))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
