package com.example.splitbill.ui.debt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.SimplifiedDebt
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.DebtSummarySkeleton
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.components.VietQrBottomSheet

@Composable
fun DebtSummaryScreen(
  viewModel: DebtSummaryViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val selectedDebt by viewModel.selectedDebt.collectAsStateWithLifecycle()
  val creditorProfile by viewModel.creditorProfile.collectAsStateWithLifecycle()
  val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      SplitBillTopBar(
        title = "Tổng kết nợ",
        canNavigateBack = true,
        onNavigateBack = onNavigateBack,
        actions = {
          // Nút vào profile để thiết lập tài khoản ngân hàng
          IconButton(onClick = onNavigateToProfile) {
            Icon(
              Icons.Default.AccountBalanceWallet,
              contentDescription = "Tài khoản ngân hàng",
              tint = MaterialTheme.colorScheme.primary
            )
          }
          IconButton(onClick = { viewModel.loadDebts() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    when (val state = uiState) {
      is DebtSummaryUiState.Loading -> {
        DebtSummarySkeleton(
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
      is DebtSummaryUiState.Error -> {
        EmptyState(
          title = "Không thể tải dữ liệu",
          message = state.message,
          modifier = Modifier.padding(paddingValues).fillMaxSize()
        )
      }
      is DebtSummaryUiState.Success -> {
        val data = state.data
        LazyColumn(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          contentPadding = PaddingValues(Dimens.SpacingM),
          verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
        ) {
          // Header summary card — animated entrance
          item {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
              SplitBillCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.AccountBalance,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(Dimens.SpacingS))
                    Text(
                      data.groupName,
                      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                  Spacer(Modifier.height(Dimens.SpacingS))
                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                  Spacer(Modifier.height(Dimens.SpacingS))
                  Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(
                      "Tổng số giao dịch cần thực hiện:",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Animated transaction count
                    val animatedCount by animateIntAsState(
                      targetValue = data.totalTransactions,
                      animationSpec = Motion.tweenMedium(),
                      label = "tx_count"
                    )
                    Text(
                      "$animatedCount giao dịch",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = if (data.totalTransactions > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                  }
                }
              }
            }
          }

          // Gợi ý thiết lập VietQR
          if (data.debts.isNotEmpty()) {
            item {
              var visible by remember { mutableStateOf(false) }
              LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(Motion.StaggerDelay)
                visible = true
              }
              AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(1)) {
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .clickable { onNavigateToProfile() }
                      .padding(Dimens.SpacingM),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      Icons.Default.QrCode2,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Dimens.SpacingS))
                    Text(
                      "Thiết lập VietQR để bạn bè quét mã trả tiền bạn →",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.weight(1f)
                    )
                  }
                }
              }
            }
          }

          // No debts case
          if (data.debts.isEmpty()) {
            item {
              EmptyState(
                title = "🎉 Tất cả đã huề!",
                message = "Không có ai nợ ai cả. Nhóm đã chia tiền rất công bằng!",
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
              )
            }
          } else {
            item {
              var visible by remember { mutableStateOf(false) }
              LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2 * Motion.StaggerDelay)
                visible = true
              }
              AnimatedVisibility(visible = visible, enter = fadeIn(Motion.tweenMedium())) {
                Text(
                  "Danh sách cần thanh toán",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground
                )
              }
            }
            itemsIndexed(data.debts, key = { _, d -> "${d.fromUserId}_${d.toUserId}" }) { index, debt ->
              // Staggered slide-in from right
              var visible by remember { mutableStateOf(false) }
              LaunchedEffect(Unit) {
                kotlinx.coroutines.delay((index + 3) * Motion.StaggerDelay)
                visible = true
              }
              AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                  animationSpec = tween(
                    durationMillis = Motion.DurationSlow,
                    delayMillis = (index * Motion.StaggerDelay).toInt(),
                    easing = Motion.EasingDecelerate
                  ),
                  initialOffsetX = { it / 3 }
                ) + fadeIn(
                  animationSpec = tween(
                    durationMillis = Motion.DurationMedium,
                    delayMillis = (index * Motion.StaggerDelay).toInt()
                  )
                )
              ) {
                DebtCard(
                  debt = debt,
                  currentUserId = currentUserId,
                  onPayClick = { viewModel.selectDebtForPayment(debt) }
                )
              }
            }
          }

          item { Spacer(Modifier.height(Dimens.SpacingXL)) }
        }
      }
    }
  }

  // VietQR Bottom Sheet
  if (selectedDebt != null) {
    val isCreditorMode = selectedDebt!!.toUserId == currentUserId
    VietQrBottomSheet(
      creditorProfile = creditorProfile,
      debtorName = selectedDebt!!.fromUsername,
      amount = selectedDebt!!.amount,
      isCreditorMode = isCreditorMode,
      onDismiss = { viewModel.dismissQrSheet() }
    )
  }
}

@Composable
private fun DebtCard(debt: SimplifiedDebt, currentUserId: String, onPayClick: () -> Unit) {
  val isCreditor = debt.toUserId == currentUserId

  SplitBillCard(modifier = Modifier.fillMaxWidth()) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // From user avatar
        UserAvatar(name = debt.fromUsername, containerColor = MaterialTheme.colorScheme.errorContainer)
        Spacer(Modifier.width(Dimens.SpacingS))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            debt.fromUsername,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            "trả cho",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            debt.toUsername,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.height(4.dp))
          AmountText(
            amount = debt.amount,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.error
            )
          )
        }
      }

      Spacer(Modifier.height(Dimens.SpacingS))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(Modifier.height(Dimens.SpacingS))

      // Nút thanh toán QR
      Button(
        onClick = onPayClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Dimens.SpacingXS))
        Text(
          if (isCreditor) "Mã nhận tiền VietQR" else "Thanh toán VietQR",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

@Composable
private fun UserAvatar(name: String, containerColor: androidx.compose.ui.graphics.Color) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = containerColor,
    modifier = Modifier.size(40.dp)
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(
        name.first().uppercaseChar().toString(),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onErrorContainer
      )
    }
  }
}
