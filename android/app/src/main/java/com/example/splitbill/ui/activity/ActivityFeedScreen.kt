package com.example.splitbill.ui.activity

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.ActivityResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.LoadingState
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.localization.localized
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(
  viewModel: ActivityFeedViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = "Lịch sử hoạt động".localized(),
        canNavigateBack = true,
        onNavigateBack = onNavigateBack
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize()
    ) {
      when (val state = uiState) {
        is ActivityFeedUiState.Loading -> {
          LoadingState(modifier = Modifier.fillMaxSize())
        }
        is ActivityFeedUiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(state.message, color = MaterialTheme.colorScheme.error)
              Spacer(Modifier.height(Dimens.SpacingM))
              Button(onClick = { viewModel.loadActivities() }) {
                Text("Thử lại".localized())
              }
            }
          }
        }
        is ActivityFeedUiState.Success -> {
          if (state.activities.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "Chưa có hoạt động nào trong nhóm này".localized(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(Dimens.SpacingM),
              verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
            ) {
              itemsIndexed(state.activities) { index, activity ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                  kotlinx.coroutines.delay(index * 30L)
                  visible = true
                }
                AnimatedVisibility(
                  visible = visible,
                  enter = Motion.slideUp + fadeIn()
                ) {
                  ActivityTimelineItem(
                    activity = activity,
                    isLast = index == state.activities.lastIndex
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActivityTimelineItem(
  activity: ActivityResponse,
  isLast: Boolean
) {
  val config = getActivityConfig(activity.activityType)
  
  val formattedTime = remember(activity.createdAt) {
    try {
      val parsed = LocalDateTime.parse(activity.createdAt.substring(0, 19))
      parsed.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
    } catch (e: Exception) {
      activity.createdAt
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
  ) {
    // Left Timeline line & Circle Icon
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxHeight()
        .width(48.dp)
    ) {
      // Circle container for icon
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(config.bgColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = config.icon,
          contentDescription = null,
          tint = config.tintColor,
          modifier = Modifier.size(18.dp)
        )
      }
      
      // Vertical line going down
      if (!isLast) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .weight(1f)
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.outlineVariant,
                  MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                )
              )
            )
        )
      }
    }

    Spacer(Modifier.width(Dimens.SpacingS))

    // Right Content Card
    SplitBillCard(
      modifier = Modifier
        .weight(1f)
        .padding(bottom = Dimens.SpacingS)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(Dimens.SpacingM)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = activity.username,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        
        Spacer(Modifier.height(Dimens.SpacingXS))
        
        Text(
          text = activity.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private data class ActivityConfig(
  val icon: ImageVector,
  val bgColor: Color,
  val tintColor: Color
)

@Composable
private fun getActivityConfig(activityType: String): ActivityConfig {
  return when (activityType) {
    "BILL_CREATED" -> ActivityConfig(
      icon = Icons.Default.ReceiptLong,
      bgColor = Color(0xFFE8F5E9),
      tintColor = Color(0xFF2E7D32)
    )
    "BILL_UPDATED" -> ActivityConfig(
      icon = Icons.Default.Edit,
      bgColor = Color(0xFFE3F2FD),
      tintColor = Color(0xFF1565C0)
    )
    "BILL_DELETED" -> ActivityConfig(
      icon = Icons.Default.DeleteForever,
      bgColor = Color(0xFFFFEBEE),
      tintColor = Color(0xFFC62828)
    )
    "DEBT_SETTLED" -> ActivityConfig(
      icon = Icons.Default.Payments,
      bgColor = Color(0xFFFFF8E1),
      tintColor = Color(0xFFF57F17)
    )
    "MEMBER_JOINED" -> ActivityConfig(
      icon = Icons.Default.PersonAdd,
      bgColor = Color(0xFFF3E5F5),
      tintColor = Color(0xFF6A1B9A)
    )
    else -> ActivityConfig(
      icon = Icons.Default.Notifications,
      bgColor = MaterialTheme.colorScheme.surfaceVariant,
      tintColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
