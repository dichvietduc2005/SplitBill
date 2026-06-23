package com.example.splitbill.ui.group

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.AmountText
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.GroupDetailSkeleton
import com.example.splitbill.ui.components.PremiumDialog
import com.example.splitbill.ui.components.SpeedDialFab
import com.example.splitbill.ui.components.SpeedDialItem
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.localization.localized
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

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
  var showStatsSheet by remember { mutableStateOf(false) }
  var showMembersSheet by remember { mutableStateOf(false) }
  var showGroupInfoDialog by remember { mutableStateOf(false) }

  // Auto-refresh khi signal thay đổi
  LaunchedEffect(refreshSignal) {
    if (refreshSignal > 0) {
      viewModel.loadAll()
    }
  }

  // Luôn làm mới dữ liệu khi vào màn hình
  LaunchedEffect(Unit) {
    viewModel.loadAll()
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(state.actionMessage) {
    state.actionMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearActionMessage()
    }
  }

  var fabVisible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(400)
    fabVisible = true
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = state.group?.name ?: "Chi tiết nhóm",
        canNavigateBack = true,
        onNavigateBack = onNavigateBack,
        actions = {
          IconButton(onClick = { showGroupInfoDialog = true }) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Thông tin nhóm",
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
        SpeedDialFab(
          items = listOf(
            SpeedDialItem(
              icon = Icons.Rounded.PostAdd,
              label = "Thêm hóa đơn",
              onClick = { onAddBill(state.group?.id ?: "", state.members) }
            ),
            SpeedDialItem(
              icon = Icons.Rounded.GroupAdd,
              label = "Mời thành viên",
              onClick = { showAddMemberDialog = true }
            )
          )
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
      var isRefreshing by remember { mutableStateOf(false) }
      LaunchedEffect(state.isLoading) {
          if (!state.isLoading) isRefreshing = false
      }
      val pullRefreshState = rememberPullToRefreshState()
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { 
            isRefreshing = true
            viewModel.loadAll() 
        },
        state = pullRefreshState,
        modifier = Modifier.padding(paddingValues).fillMaxSize()
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(Dimens.SpacingM),
          verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
        ) {

        // --- Hero Summary Card ---
        item {
          AnimatedVisibility(
            visible = true,
            enter = Motion.slideUp
          ) {
            HeroSummaryCard(
              totalSpent = state.bills.sumOf { it.totalAmount },
              memberCount = state.members.size,
              billCount = state.bills.size
            )
          }
        }

        // --- Action Grid ---
        item {
          ActionGrid(
            onSuggestSplit = { onViewDebts(state.group?.id ?: "") },
            onStats = { showStatsSheet = true }
          )
        }

        // --- Members & Balance Section ---
        item {
          Spacer(Modifier.height(Dimens.SpacingS))
          Text(
            "Thành viên & Công nợ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        }
        
        itemsIndexed(state.members, key = { _, member -> member.userId }) { index, member ->
          val balance = state.memberBalances[member.userId] ?: 0.0
          var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(index * Motion.StaggerDelay)
            visible = true
          }
          AnimatedVisibility(
            visible = visible,
            enter = Motion.staggeredSlideIn(index)
          ) {
            MemberBalanceCard(member = member, balance = balance)
          }
        }

        // --- Bills Section ---
        item {
          Spacer(Modifier.height(Dimens.SpacingM))
          Text(
            "Hóa đơn gần đây (${state.bills.size})",
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
            var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
              kotlinx.coroutines.delay((index + state.members.size) * Motion.StaggerDelay)
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
  }

  // --- Statistics Bottom Sheet ---
  if (showStatsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showStatsSheet = false },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      StatisticsContent(state = state)
    }
  }

  // --- Members Bottom Sheet (Simplified view) ---
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
        Text(
          "Tất cả thành viên (${state.members.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Dimens.SpacingM))
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
        ) {
          items(state.members) { member ->
            val balance = state.memberBalances[member.userId] ?: 0.0
            MemberBalanceCard(member = member, balance = balance)
          }
          item { Spacer(Modifier.height(32.dp)) }
        }
      }
    }
  }

  // --- Add Member PremiumDialog ---
  if (showAddMemberDialog) {
    var memberInput by remember { mutableStateOf("") }
    PremiumDialog(
      onDismissRequest = { showAddMemberDialog = false },
      title = "Mời thành viên",
      icon = Icons.Default.PersonAdd,
      confirmButtonText = "Mời",
      onConfirm = {
        if (memberInput.isNotBlank()) {
          viewModel.addMember(memberInput.trim())
          showAddMemberDialog = false
        }
      },
      dismissButtonText = "Hủy",
      onDismiss = { showAddMemberDialog = false },
      content = {
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
          shape = RoundedCornerShape(12.dp)
        )
      }
    )
  }

  // --- Group Info PremiumDialog ---
  if (showGroupInfoDialog && state.group != null) {
    val clipboardManager = LocalClipboardManager.current
    PremiumDialog(
      onDismissRequest = { showGroupInfoDialog = false },
      title = "Thông tin nhóm",
      icon = Icons.Default.Info,
      confirmButtonText = "Đóng",
      onConfirm = { showGroupInfoDialog = false },
      content = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = state.group!!.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(Modifier.height(Dimens.SpacingL))
          
          Box(
            modifier = Modifier
              .size(160.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.White)
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${state.group!!.id}",
              contentDescription = "QR Code của nhóm",
              modifier = Modifier.fillMaxSize()
            )
          }
          
          Spacer(Modifier.height(Dimens.SpacingL))
          
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable { 
                clipboardManager.setText(AnnotatedString(state.group!!.id))
              }
              .padding(horizontal = Dimens.SpacingM, vertical = Dimens.SpacingS)
          ) {
            Text(
              text = "Mã ID: ${state.group!!.id}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(Dimens.SpacingS))
            Icon(
              Icons.Default.ContentCopy, 
              contentDescription = "Copy",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          
          Spacer(Modifier.height(Dimens.SpacingL))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Spacer(Modifier.height(Dimens.SpacingM))
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Người tạo:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.group!!.createdByName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
          }
          Spacer(Modifier.height(Dimens.SpacingXS))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Ngày tạo:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.group!!.createdAt.take(10), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
          }
        }
      }
    )
  }
}

@Composable
private fun HeroSummaryCard(totalSpent: Double, memberCount: Int, billCount: Int) {
  val animatedTotal by animateFloatAsState(
    targetValue = totalSpent.toFloat(),
    animationSpec = Motion.tweenMedium(),
    label = "total_spent"
  )

  // Animated aurora gradient
  val transition = rememberInfiniteTransition(label = "aurora_transition")
  val translateAnim by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(
      animation = tween(10000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "aurora_translate"
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(24.dp),
        spotColor = com.example.splitbill.theme.GradientAuroraStart,
        ambientColor = com.example.splitbill.theme.GradientAuroraStart
      )
      .clip(RoundedCornerShape(24.dp))
      .background(
        brush = Brush.linearGradient(
          colors = listOf(
            com.example.splitbill.theme.GradientAuroraStart,
            com.example.splitbill.theme.GradientAuroraMid,
            com.example.splitbill.theme.GradientAuroraEnd
          ),
          start = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim),
          end = androidx.compose.ui.geometry.Offset(translateAnim + 800f, translateAnim + 800f)
        )
      )
      .padding(Dimens.SpacingL)
  ) {
    Column {
      Text(
        text = "Tổng chi của nhóm",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.85f)
      )
      Spacer(Modifier.height(Dimens.SpacingXS))
      AmountText(
        amount = animatedTotal.toDouble(),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
        isDebt = null
      )
      Spacer(Modifier.height(Dimens.SpacingS))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.width(4.dp))
        Text(
          text = "$memberCount thành viên • $billCount hóa đơn",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.85f)
        )
      }
    }
  }
}

@Composable
private fun ActionGrid(
  onSuggestSplit: () -> Unit,
  onStats: () -> Unit
) {
  val customColors = com.example.splitbill.theme.LocalSplitBillCustomColors.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
  ) {
    ActionItem(
      icon = Icons.Default.AccountBalanceWallet,
      label = "Gợi ý chia tiền",
      onClick = onSuggestSplit,
      modifier = Modifier.weight(1f),
      badgeBg = customColors.badgeBillBg,
      badgeIconTint = customColors.badgeBillIcon
    )
    ActionItem(
      icon = Icons.Default.BarChart,
      label = "Thống kê",
      onClick = onStats,
      modifier = Modifier.weight(1f),
      badgeBg = customColors.badgeStatsBg,
      badgeIconTint = customColors.badgeStatsIcon
    )
  }
}

@Composable
private fun ActionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector, 
  label: String, 
  onClick: () -> Unit, 
  modifier: Modifier = Modifier,
  badgeBg: Color = MaterialTheme.colorScheme.primaryContainer,
  badgeIconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
  SplitBillCard(
    onClick = onClick,
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(vertical = Dimens.SpacingXS)
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(badgeBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = label, tint = badgeIconTint, modifier = Modifier.size(20.dp))
      }
      Spacer(Modifier.width(Dimens.SpacingS))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun MemberBalanceCard(member: MemberResponse, balance: Double) {
  SplitBillCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      com.example.splitbill.ui.components.GradientAvatar(name = member.username)
      Spacer(Modifier.width(Dimens.SpacingM))
      Column(modifier = Modifier.weight(1f)) {
        Text(member.username, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        if (balance == 0.0) {
          Text("Đã thanh toán xong", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (balance > 0) {
          Text("Nhóm nợ người này", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
        } else {
          Text("Đang nợ nhóm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
      if (balance != 0.0) {
        AmountText(
          amount = Math.abs(balance),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          isDebt = balance < 0
        )
      } else {
        Text("0 đ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun StatisticsContent(state: GroupDetailState) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = Dimens.SpacingM, vertical = Dimens.SpacingS)
  ) {
    Text(
      "Thống kê nhóm",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(Dimens.SpacingM))

    val totalSpent = state.bills.sumOf { it.totalAmount }
    val avgPerBill = if (state.bills.isNotEmpty()) totalSpent / state.bills.size else 0.0
    val avgPerMember = if (state.members.isNotEmpty()) totalSpent / state.members.size else 0.0
    val biggestBill = state.bills.maxByOrNull { it.totalAmount }
    
    val payerTotals = state.bills.groupBy { it.paidByUsername }
      .mapValues { (_, bills) -> bills.sumOf { it.totalAmount } }
    val topPayer = payerTotals.maxByOrNull { it.value }

    StatRow(label = "Trung bình/hóa đơn", amount = avgPerBill)
    StatRow(label = "Trung bình/người", amount = avgPerMember)
    if (biggestBill != null) {
      Spacer(Modifier.height(Dimens.SpacingS))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(Modifier.height(Dimens.SpacingS))
      Text("Hóa đơn lớn nhất:", style = MaterialTheme.typography.labelMedium)
      Text(biggestBill.description, style = MaterialTheme.typography.bodyMedium)
      AmountText(amount = biggestBill.totalAmount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
    if (topPayer != null) {
      Spacer(Modifier.height(Dimens.SpacingS))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(Modifier.height(Dimens.SpacingS))
      Text("Thanh toán nhiều nhất:", style = MaterialTheme.typography.labelMedium)
      Text(topPayer.key, style = MaterialTheme.typography.bodyMedium)
      AmountText(amount = topPayer.value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
    Spacer(Modifier.height(Dimens.SpacingL))
  }
}

@Composable
private fun StatRow(label: String, amount: Double) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpacingXS),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    AmountText(amount = amount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCard(bill: BillResponse, onDelete: () -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  val customColors = com.example.splitbill.theme.LocalSplitBillCustomColors.current

  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
      if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
        showDeleteDialog = true
        false // Wait for dialog confirmation
      } else {
        false
      }
    }
  )

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false,
    backgroundContent = {
      val color by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
          SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
          else -> Color.Transparent
        },
        label = "swipe_color"
      )
      
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(com.example.splitbill.theme.SplitBillShapes.medium)
          .background(color)
          .padding(end = Dimens.SpacingL),
        contentAlignment = Alignment.CenterEnd
      ) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Xóa",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  ) {
    SplitBillCard(
      onClick = { expanded = !expanded },
      modifier = Modifier.fillMaxWidth().animateContentSize(
        animationSpec = Motion.springGentle()
      )
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
          modifier = Modifier
            .padding(end = Dimens.SpacingM)
            .size(40.dp)
            .clip(CircleShape)
            .background(customColors.badgeBillBg),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            tint = customColors.badgeBillIcon,
            modifier = Modifier.size(20.dp)
          )
        }
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
  }

  if (showDeleteDialog) {
    PremiumDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = "Xóa hóa đơn?",
      icon = Icons.Default.DeleteSweep,
      confirmButtonText = "Xóa",
      onConfirm = { onDelete(); showDeleteDialog = false },
      dismissButtonText = "Hủy",
      onDismiss = { showDeleteDialog = false },
      content = {
        Text("Bạn có chắc muốn xóa hóa đơn '${bill.description}' không? Hành động này không thể hoàn tác.")
      }
    )
  }
}
