package com.example.splitbill.ui.group

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.GroupDetailSkeleton
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
  viewModel: GroupDetailViewModel,
  refreshSignal: Int,
  onNavigateBack: () -> Unit,
  onAddBill: (groupId: String, members: List<MemberResponse>) -> Unit,
  onViewDebts: (groupId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var showAddMemberDialog by remember { mutableStateOf(false) }
  var showMembersSheet by remember { mutableStateOf(false) }

  // Auto-refresh khi signal thay đổi (từ AddBillScreen quay lại)
  LaunchedEffect(refreshSignal) {
    if (refreshSignal > 0) {
      viewModel.refreshBills()
    }
  }

  // Show snackbar on action message
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(state.actionMessage) {
    state.actionMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearActionMessage()
    }
  }

  // FAB entrance animation
  var fabVisible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(400)
    fabVisible = true
  }

  Scaffold(
    topBar = {
      SplitBillTopBar(
        title = state.group?.name ?: "Chi tiết nhóm",
        canNavigateBack = true,
        onNavigateBack = onNavigateBack,
        actions = {
          IconButton(onClick = { onViewDebts(state.group?.id ?: "") }) {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = "Xem tổng kết nợ",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      )
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = fabVisible && !state.isLoading,
        enter = scaleIn(animationSpec = Motion.springBouncy()) + fadeIn(),
        exit = scaleOut() + fadeOut()
      ) {
        ExtendedFloatingActionButton(
          onClick = { onAddBill(state.group?.id ?: "", state.members) },
          icon = { Icon(Icons.Default.Add, contentDescription = null) },
          text = { Text("Thêm hóa đơn") },
          containerColor = MaterialTheme.colorScheme.primary
        )
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    if (state.isLoading) {
      GroupDetailSkeleton(
        modifier = Modifier.padding(paddingValues).fillMaxSize()
      )
    } else if (state.error != null) {
      EmptyState(
        title = "Lỗi tải dữ liệu",
        message = state.error!!,
        modifier = Modifier.padding(paddingValues).fillMaxSize()
      )
    } else {
      LazyColumn(
        modifier = Modifier.padding(paddingValues).fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpacingM),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
      ) {

        // --- Summary Card (animated entrance) ---
        item {
          AnimatedVisibility(
            visible = true,
            enter = Motion.slideUp
          ) {
            GroupSummaryCard(
              memberCount = state.members.size,
              billCount = state.bills.size,
              totalSpent = state.bills.sumOf { it.totalAmount },
              onViewDebts = { onViewDebts(state.group?.id ?: "") },
              onMembersClick = { showMembersSheet = true }
            )
          }
        }

        // Members section removed from main list to save space

        // --- Bills Section ---
        item {
          Spacer(Modifier.height(Dimens.SpacingXS))
          Text(
            "Hóa đơn (${state.bills.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        }
        if (state.bills.isEmpty()) {
          item {
            EmptyState(
              title = "Chưa có hóa đơn nào",
              message = "Hãy bấm nút '+' để thêm hóa đơn đầu tiên!",
              modifier = Modifier.fillMaxWidth()
            )
          }
        } else {
          itemsIndexed(state.bills, key = { _, bill -> bill.id }) { index, bill ->
            // Staggered entrance animation per bill card
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
              kotlinx.coroutines.delay(index * Motion.StaggerDelay)
              visible = true
            }
            AnimatedVisibility(
              visible = visible,
              enter = Motion.staggeredSlideIn(index)
            ) {
              BillCard(bill = bill, onDelete = { viewModel.deleteBill(bill.id) })
            }
          }
        }

        item { Spacer(Modifier.height(80.dp)) } // Space for FAB
      }
    }
  }

  // --- Members Bottom Sheet ---
  if (showMembersSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMembersSheet = false },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = Dimens.SpacingM)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Thành viên (${state.members.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          TextButton(onClick = { 
            showMembersSheet = false
            showAddMemberDialog = true 
          }) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Mời")
          }
        }
        Spacer(Modifier.height(Dimens.SpacingM))
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
        ) {
          itemsIndexed(state.members) { index, member ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
              kotlinx.coroutines.delay(index * Motion.StaggerDelay)
              visible = true
            }
            AnimatedVisibility(
              visible = visible,
              enter = Motion.staggeredSlideIn(index)
            ) {
              MemberChip(member = member)
            }
          }
          item { Spacer(Modifier.height(32.dp)) }
        }
      }
    }
  }

  // --- Add Member Dialog ---
  if (showAddMemberDialog) {
    var memberInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddMemberDialog = false },
      title = { Text("Mời thành viên") },
      text = {
        Column {
          Text(
            "Nhập username hoặc email của người bạn muốn mời vào nhóm:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(Modifier.height(Dimens.SpacingM))
          OutlinedTextField(
            value = memberInput,
            onValueChange = { memberInput = it },
            label = { Text("Username hoặc Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (memberInput.isNotBlank()) {
              viewModel.addMember(memberInput.trim())
              showAddMemberDialog = false
            }
          }
        ) { Text("Mời") }
      },
      dismissButton = {
        TextButton(onClick = { showAddMemberDialog = false }) { Text("Hủy") }
      }
    )
  }
}

@Composable
private fun GroupSummaryCard(memberCount: Int, billCount: Int, totalSpent: Double, onViewDebts: () -> Unit, onMembersClick: () -> Unit) {
  // Animated counters
  val animatedMembers by animateIntAsState(
    targetValue = memberCount,
    animationSpec = Motion.tweenMedium(),
    label = "members_count"
  )
  val animatedBills by animateIntAsState(
    targetValue = billCount,
    animationSpec = Motion.tweenMedium(),
    label = "bills_count"
  )

  SplitBillCard(modifier = Modifier.fillMaxWidth()) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        SummaryItem(label = "Thành viên", value = "$animatedMembers người", onClick = onMembersClick)
        VerticalDivider(modifier = Modifier.height(48.dp))
        SummaryItem(label = "Hóa đơn", value = "$animatedBills cái")
        VerticalDivider(modifier = Modifier.height(48.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Tổng chi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          AmountText(amount = totalSpent, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
      }

      Spacer(Modifier.height(Dimens.SpacingM))

      Button(
        onClick = onViewDebts,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = MaterialTheme.shapes.medium
      ) {
        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Dimens.SpacingS))
        Text("Tổng kết nợ & Thanh toán", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}

@Composable
private fun SummaryItem(label: String, value: String, onClick: (() -> Unit)? = null) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
  ) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun MemberChip(member: MemberResponse) {
  SplitBillCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Text(
            text = member.username.first().uppercaseChar().toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
      Spacer(Modifier.width(Dimens.SpacingM))
      Column {
        Text(member.username, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun BillCard(bill: BillResponse, onDelete: () -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  SplitBillCard(
    onClick = { expanded = !expanded },
    modifier = Modifier.fillMaxWidth().animateContentSize(
      animationSpec = Motion.springGentle()
    )
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            bill.description,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(Modifier.height(2.dp))
          Text(
            "Trả bởi: ${bill.paidByUsername}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          AmountText(amount = bill.totalAmount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
          }
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = Motion.springGentle()) + fadeIn(animationSpec = Motion.tweenMedium()),
        exit = shrinkVertically(animationSpec = Motion.springGentle()) + fadeOut(animationSpec = Motion.tweenFast())
      ) {
        Column {
          Spacer(Modifier.height(Dimens.SpacingS))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Spacer(Modifier.height(Dimens.SpacingS))
          Text("Chi tiết chia tiền:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(Modifier.height(Dimens.SpacingXS))
          bill.splits.forEach { split ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
              Text(split.username, style = MaterialTheme.typography.bodySmall)
              AmountText(amount = split.amountOwed, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            }
          }
        }
      }
    }
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Xóa hóa đơn?") },
      text = { Text("Bạn có chắc muốn xóa hóa đơn '${bill.description}' không? Hành động này không thể hoàn tác.") },
      confirmButton = {
        Button(
          onClick = { onDelete(); showDeleteDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("Xóa") }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
      }
    )
  }
}
