package com.example.splitbill.ui.debt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
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
import com.example.splitbill.ui.components.PremiumDialog

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

  var showSettleConfirmDialog by remember { mutableStateOf<SimplifiedDebt?>(null) }
  var settlementNote by remember { mutableStateOf("") }
  var showSuccessOverlay by remember { mutableStateOf(false) }

  LaunchedEffect(showSuccessOverlay) {
    if (showSuccessOverlay) {
      kotlinx.coroutines.delay(1500)
      showSuccessOverlay = false
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
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
            var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
              SplitBillCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer
              ) {
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
              var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
              LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(Motion.StaggerDelay)
                visible = true
              }
              AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(1)) {
                SplitBillCard(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
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
              var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
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
              var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
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
                  onPayClick = { viewModel.selectDebtForPayment(debt) },
                  onSettleClick = {
                    showSettleConfirmDialog = debt
                    settlementNote = ""
                  }
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

  // Confirm Settle Dialog
  if (showSettleConfirmDialog != null) {
    val debt = showSettleConfirmDialog!!
    PremiumDialog(
      onDismissRequest = { showSettleConfirmDialog = null },
      title = "Xác nhận trả nợ",
      icon = Icons.Default.CheckCircle,
      confirmButtonText = "Xác nhận",
      onConfirm = {
        viewModel.settleDebt(
          toUserId = debt.toUserId,
          amount = debt.amount,
          note = settlementNote.trim().ifBlank { null }
        ) {
          showSuccessOverlay = true
        }
        showSettleConfirmDialog = null
      },
      dismissButtonText = "Hủy",
      onDismiss = { showSettleConfirmDialog = null },
      content = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Xác nhận bạn (${debt.fromUsername}) đã trả số tiền:",
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(Modifier.height(Dimens.SpacingS))
          AmountText(
            amount = debt.amount,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            isDebt = null
          )
          Spacer(Modifier.height(Dimens.SpacingS))
          Text(
            text = "Cho ${debt.toUsername}.",
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(Modifier.height(Dimens.SpacingM))
          OutlinedTextField(
            value = settlementNote,
            onValueChange = { settlementNote = it },
            label = { Text("Ghi chú (ví dụ: Chuyển khoản, tiền mặt...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
          )
        }
      }
    )
  }

  // Success Confetti Overlay
  AnimatedVisibility(
    visible = showSuccessOverlay,
    enter = fadeIn(animationSpec = tween(300)),
    exit = fadeOut(),
    modifier = Modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
      contentAlignment = Alignment.Center
    ) {
      com.example.splitbill.ui.components.ConfettiOverlay()

      val scale = remember { Animatable(0f) }
      LaunchedEffect(Unit) {
        scale.animateTo(
          targetValue = 1f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
          )
        )
      }
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(140.dp).scale(scale.value),
        shadowElevation = 8.dp
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
          )
        }
      }
    }
  }
  }
}

@Composable
private fun DebtCard(debt: SimplifiedDebt, currentUserId: String, onPayClick: () -> Unit, onSettleClick: () -> Unit) {
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

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
      ) {
        // Nút QR Code
        Button(
          onClick = onPayClick,
          modifier = Modifier.weight(1f).height(44.dp),
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        ) {
          Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(Dimens.SpacingXS))
          Text(
            if (isCreditor) "Mã nhận QR" else "Quét mã QR",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }

        // Nút Đã trả xong
        Button(
          onClick = onSettleClick,
          modifier = Modifier.weight(1f).height(44.dp),
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(Dimens.SpacingXS))
          Text(
            "Đã trả xong",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
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
