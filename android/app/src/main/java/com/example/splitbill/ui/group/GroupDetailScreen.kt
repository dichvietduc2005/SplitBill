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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.ui.stats.GroupStatsViewModel
import com.example.splitbill.ui.stats.GroupStatsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
  viewModel: GroupDetailViewModel,
  refreshSignal: Int,
  onNavigateBack: () -> Unit,
  onAddBill: (groupId: String, members: List<MemberResponse>) -> Unit,
  onViewDebts: (groupId: String) -> Unit,
  onViewStats: (groupId: String) -> Unit,
  onViewActivities: (groupId: String) -> Unit,
  modifier: Modifier = Modifier,
  onEditBill: (groupId: String, bill: BillResponse, members: List<MemberResponse>) -> Unit
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var showAddMemberDialog by remember { mutableStateOf(false) }
  var showInviteSheet by remember { mutableStateOf(false) }
  var showMembersSheet by remember { mutableStateOf(false) }
  var showGroupInfoDialog by remember { mutableStateOf(false) }

  // Auto-refresh khi signal thay đổi (quay lại từ AddBill)
  LaunchedEffect(refreshSignal) {
    if (refreshSignal > 0) {
      viewModel.refreshBills()
    }
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
          IconButton(onClick = { onViewActivities(state.group?.id ?: "") }) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = "Lịch sử hoạt động",
              tint = MaterialTheme.colorScheme.primary
            )
          }
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
              onClick = { showInviteSheet = true }
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
              totalSpent = state.bills.sumOf { it.totalAmount * (if (it.exchangeRate > 0) it.exchangeRate else 1.0) },
              memberCount = state.members.size,
              billCount = state.bills.size
            )
          }
        }

        // --- Action Grid ---
        item {
          ActionGrid(
            onSuggestSplit = { onViewDebts(state.group?.id ?: "") },
            onStats = { onViewStats(state.group?.id ?: "") }
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
              BillCard(
                bill = bill, 
                onDelete = { viewModel.deleteBill(bill.id) },
                onEdit = { onEditBill(state.group?.id ?: "", bill, state.members) },
                onTogglePaid = { viewModel.toggleBillPaidStatus(bill.id, bill.isPaid) }
              )
            }
          }
        }

        item { Spacer(Modifier.height(80.dp)) } // Space for FAB
      }
      }
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

  // --- Invite Member BottomSheet ---
  if (showInviteSheet) {
    val invite by viewModel.activeInvite.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(
      onDismissRequest = {
        showInviteSheet = false
        viewModel.clearActiveInvite()
      },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(Dimens.SpacingL),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          "Mời thành viên vào nhóm",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Dimens.SpacingM))

        // Cách 1: Mời trực tiếp bằng email/username
        var memberInput by remember { mutableStateOf("") }
        OutlinedTextField(
          value = memberInput,
          onValueChange = { memberInput = it },
          label = { Text("Username hoặc Email") },
          placeholder = { Text("nhan.nguyen@example.com") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          trailingIcon = {
            IconButton(
              onClick = {
                if (memberInput.isNotBlank()) {
                  viewModel.addMember(memberInput.trim())
                  memberInput = ""
                  showInviteSheet = false
                }
              }
            ) {
              Icon(Icons.Default.Send, contentDescription = "Gửi", tint = MaterialTheme.colorScheme.primary)
            }
          }
        )

        Spacer(Modifier.height(Dimens.SpacingL))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Dimens.SpacingL))

        // Cách 2: Chia sẻ link mời
        Text(
          "Hoặc chia sẻ mã mời (Hạn 7 ngày)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Dimens.SpacingM))

        if (invite == null) {
          Button(
            onClick = { viewModel.loadOrCreateActiveInvite() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
          ) {
            Icon(Icons.Default.Link, contentDescription = null)
            Spacer(Modifier.width(Dimens.SpacingS))
            Text("Tạo liên kết mời 7 ngày", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
          }
        } else {
          // Hiển thị QR Code
          Box(
            modifier = Modifier
              .size(160.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.White)
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${invite!!.inviteUrl}",
              contentDescription = "QR Code mã mời",
              modifier = Modifier.fillMaxSize()
            )
          }

          Spacer(Modifier.height(Dimens.SpacingM))

          // Hiển thị Link và Mã code
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(Dimens.SpacingM)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "Mã mời: ${invite!!.inviteCode}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                invite!!.inviteUrl,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
              )
            }
            IconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(invite!!.inviteUrl))
              }
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
              onClick = {
                val sendIntent = android.content.Intent().apply {
                  action = android.content.Intent.ACTION_SEND
                  putExtra(android.content.Intent.EXTRA_TEXT, "Tham gia nhóm SplitBill '${state.group?.name}' cùng tôi nhé! Mã mời (hạn 7 ngày): ${invite!!.inviteCode}\nLiên kết: ${invite!!.inviteUrl}")
                  type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
              }
            ) {
              Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
        Spacer(Modifier.height(Dimens.SpacingXL))
      }
    }
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

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Dimens.BentoGap)
  ) {
    // Block 1: Total Spent (Full Width, Taller/Spacious Hero Card)
    SplitBillCard(
      modifier = Modifier.fillMaxWidth(),
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = Dimens.SpacingS), // Taller vertical padding to make card roomier
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Tổng chi nhóm",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
          Spacer(Modifier.height(Dimens.SpacingS))
          AmountText(
            amount = animatedTotal.toDouble(),
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.ExtraBold, 
              color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            isDebt = null
          )
        }
        Box(
          modifier = Modifier
            .size(56.dp) // Larger icon box
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Payments, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp) // Larger icon
          )
        }
      }
    }

    // Block 2 & 3: Members & Bills (Divided in half below)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.BentoGap)
    ) {
      // Left Half: Members
      SplitBillCard(
        modifier = Modifier.weight(1f),
        containerColor = com.example.splitbill.theme.BadgeMemberBg
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Thành viên", 
              style = MaterialTheme.typography.labelSmall, 
              color = com.example.splitbill.theme.BadgeMemberIcon.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "$memberCount",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = com.example.splitbill.theme.BadgeMemberIcon
            )
          }
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Group, 
              contentDescription = null, 
              tint = com.example.splitbill.theme.BadgeMemberIcon, 
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // Right Half: Bills
      SplitBillCard(
        modifier = Modifier.weight(1f),
        containerColor = com.example.splitbill.theme.BadgeBillBg
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Hóa đơn", 
              style = MaterialTheme.typography.labelSmall, 
              color = com.example.splitbill.theme.BadgeBillIcon.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "$billCount",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = com.example.splitbill.theme.BadgeBillIcon
            )
          }
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Receipt, 
              contentDescription = null, 
              tint = com.example.splitbill.theme.BadgeBillIcon, 
              modifier = Modifier.size(18.dp)
            )
          }
        }
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
    horizontalArrangement = Arrangement.spacedBy(Dimens.BentoGap)
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
    containerColor = badgeBg // Use the badge background color directly for the card background
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = badgeIconTint
        )
      }
      Spacer(Modifier.width(Dimens.SpacingS))
      Box(
        modifier = Modifier
          .size(36.dp)
          .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          icon, 
          contentDescription = label, 
          tint = badgeIconTint, 
          modifier = Modifier.size(18.dp)
        )
      }
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
      if (!member.avatarUrl.isNullOrBlank()) {
        val avatarFullUrl = remember(member.avatarUrl) {
          if (member.avatarUrl.startsWith("http")) member.avatarUrl else "${com.example.splitbill.data.api.ApiService.BASE_URL}${if (member.avatarUrl.startsWith("/")) "" else "/"}${member.avatarUrl}"
        }
        AsyncImage(
          model = avatarFullUrl,
          contentDescription = member.username,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
          contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
      } else {
        com.example.splitbill.ui.components.GradientAvatar(name = member.username)
      }
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCard(
  bill: BillResponse, 
  onDelete: () -> Unit, 
  onEdit: () -> Unit,
  onTogglePaid: () -> Unit
) {
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              bill.description,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            if (bill.isPaid) {
              Spacer(Modifier.width(Dimens.SpacingXS))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFFE8F5E9))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "Đã trả",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF2E7D32)
                  )
                )
              }
            }
          }
          Spacer(Modifier.height(2.dp))
          val formattedDate = formatCreatedDateTime(bill.createdAt)
          Text(
            if (formattedDate.isNotBlank()) "Trả bởi: ${bill.paidByUsername} • $formattedDate" else "Trả bởi: ${bill.paidByUsername}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          AmountText(amount = bill.totalAmount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), currency = bill.currency)
          if (bill.currency != "VND" && bill.exchangeRate > 1.0) {
            val vndEquivalent = bill.totalAmount * bill.exchangeRate
            Text(
              "≈ ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN")).format(vndEquivalent)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Chi tiết chia tiền:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(
              onClick = onEdit,
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.Edit,
                contentDescription = "Sửa",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(Modifier.height(Dimens.SpacingXS))
          bill.splits.forEach { split ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(split.username, style = MaterialTheme.typography.bodySmall)
              Column(horizontalAlignment = Alignment.End) {
                AmountText(amount = split.amountOwed, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), currency = bill.currency)
                if (bill.currency != "VND" && bill.exchangeRate > 1.0) {
                  val splitVnd = split.amountOwed * bill.exchangeRate
                  Text(
                    "≈ ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN")).format(splitVnd)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
          if (bill.receiptUrl != null) {
            Spacer(Modifier.height(Dimens.SpacingS))
            Text("Ảnh hóa đơn:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.SpacingXS))
            val fullUrl = com.example.splitbill.data.api.ApiService.BASE_URL + bill.receiptUrl
            AsyncImage(
              model = fullUrl,
              contentDescription = "Ảnh hóa đơn",
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
          }

          Spacer(Modifier.height(Dimens.SpacingS))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(
              onClick = onTogglePaid,
              colors = if (bill.isPaid) {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
              } else {
                ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = if (bill.isPaid) Icons.Default.RemoveCircleOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (bill.isPaid) "Đánh dấu chưa trả" else "Đánh dấu đã thanh toán",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
              )
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

private fun formatCreatedDateTime(isoString: String?): String {
  if (isoString.isNullOrBlank()) return ""
  return try {
    val cleaned = isoString.replace("Z", "").replace("T", " ")
    val parts = cleaned.split(" ")
    if (parts.size >= 2) {
      val dateParts = parts[0].split("-")
      val timeParts = parts[1].split(":")
      if (dateParts.size == 3 && timeParts.size >= 2) {
        val hour = timeParts[0]
        val minute = timeParts[1]
        val day = dateParts[2]
        val month = dateParts[1]
        val year = dateParts[0]
        "$hour:$minute - $day/$month/$year"
      } else {
        cleaned
      }
    } else {
      isoString
    }
  } catch (e: Exception) {
    isoString
  }
}



