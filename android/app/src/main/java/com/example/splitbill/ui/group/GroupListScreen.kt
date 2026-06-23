package com.example.splitbill.ui.group

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.LocalSplitBillCustomColors
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.GroupListSkeleton
import com.example.splitbill.ui.components.ScrollAwareFab
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.localization.localized

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
  viewModel: GroupListViewModel,
  onNavigateToGroup: (String) -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
  LaunchedEffect(uiState) {
    if (uiState is GroupListUiState.Success) {
      val msg = (uiState as GroupListUiState.Success).actionMessage
      if (msg != null) {
        snackbarHostState.showSnackbar(msg)
        viewModel.clearActionMessage()
      }
    }
  }

  val listState = rememberLazyListState()
  var showCreateDialog by remember { mutableStateOf(false) }
  var showJoinDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    topBar = {
      SplitBillTopBar(
        title = "Nhóm của tôi".localized(),
        actions = {
          IconButton(onClick = onNavigateToSettings) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Cài đặt".localized(),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      )
    },
    floatingActionButton = {
      // Hide FAB when scrolling down, show when scrolling up
      AnimatedVisibility(
        visible = listState.firstVisibleItemScrollOffset == 0 || !listState.isScrollInProgress,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
      ) {
        com.example.splitbill.ui.components.SpeedDialFab(
          items = listOf(
            com.example.splitbill.ui.components.SpeedDialItem(
              icon = Icons.Rounded.RocketLaunch,
              label = "Tạo nhóm mới",
              onClick = { showCreateDialog = true }
            ),
            com.example.splitbill.ui.components.SpeedDialItem(
              icon = Icons.Rounded.Login,
              label = "Tham gia nhóm",
              onClick = { showJoinDialog = true }
            )
          )
        )
      }
    },
    snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      when (val state = uiState) {
        is GroupListUiState.Loading -> {
          GroupListSkeleton(modifier = Modifier.fillMaxSize())
        }
        is GroupListUiState.Error -> {
          EmptyState(
            title = "Đã xảy ra lỗi".localized(),
            message = state.message,
            emoji = "⚠️",
            modifier = Modifier.align(Alignment.Center)
          )
        }
        is GroupListUiState.Success -> {
          if (state.groups.isEmpty()) {
            EmptyState(
              title = "Chưa có nhóm nào".localized(),
              message = "Hãy tạo nhóm mới để bắt đầu chia tiền nhé!".localized(),
              emoji = "🎉",
              modifier = Modifier.align(Alignment.Center)
            )
          } else {
            PullToRefreshBox(
              isRefreshing = false,
              onRefresh = { viewModel.loadGroups() }
            ) {
              LazyColumn(
                state = listState,
                contentPadding = PaddingValues(Dimens.SpacingM),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM),
                modifier = Modifier.fillMaxSize()
              ) {
                itemsIndexed(state.groups, key = { _, group -> group.id }) { index, group ->
                  // Staggered entrance
                  var visible by remember { mutableStateOf(false) }
                  LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * Motion.StaggerDelay)
                    visible = true
                  }
                  AnimatedVisibility(
                    visible = visible,
                    enter = Motion.staggeredSlideIn(index)
                  ) {
                    GroupCard(group = group, onClick = { onNavigateToGroup(group.id) })
                  }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
              }
            }
          }
        }
      }
    }
  }

  // Create Group PremiumDialog
  if (showCreateDialog) {
    var groupName by remember { mutableStateOf("") }
    com.example.splitbill.ui.components.PremiumDialog(
      onDismissRequest = { showCreateDialog = false },
      title = "Tạo nhóm mới".localized(),
      icon = Icons.Rounded.RocketLaunch,
      confirmButtonText = "Tạo nhóm".localized(),
      onConfirm = {
        if (groupName.isNotBlank()) {
          viewModel.createGroup(groupName.trim())
          showCreateDialog = false
        }
      },
      dismissButtonText = "Hủy".localized(),
      onDismiss = { showCreateDialog = false },
      content = {
        Text(
          "Đặt tên cho nhóm của bạn:".localized(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingM))
        OutlinedTextField(
          value = groupName,
          onValueChange = { groupName = it },
          label = { Text("Tên nhóm (VD: Du lịch Đà Lạt)".localized()) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )
      }
    )
  }

  // Join Group PremiumDialog
  if (showJoinDialog) {
    var groupIdInput by remember { mutableStateOf("") }
    com.example.splitbill.ui.components.PremiumDialog(
      onDismissRequest = { showJoinDialog = false },
      title = "Tham gia nhóm".localized(),
      icon = Icons.Rounded.Login,
      confirmButtonText = "Tham gia".localized(),
      onConfirm = {
        if (groupIdInput.isNotBlank()) {
          viewModel.joinGroup(groupIdInput.trim())
          showJoinDialog = false
        }
      },
      dismissButtonText = "Hủy".localized(),
      onDismiss = { showJoinDialog = false },
      content = {
        Text(
          "Nhập mã ID của nhóm để tham gia:",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingM))
        OutlinedTextField(
          value = groupIdInput,
          onValueChange = { groupIdInput = it },
          label = { Text("Mã ID nhóm") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )
      }
    )
  }
}

@Composable
fun GroupCard(group: GroupResponse, onClick: () -> Unit) {
  val customColors = LocalSplitBillCustomColors.current
  SplitBillCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .padding(end = Dimens.SpacingM)
          .size(48.dp)
          .clip(CircleShape)
          .background(customColors.badgeGroupBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Groups,
          contentDescription = null,
          tint = customColors.badgeGroupIcon,
          modifier = Modifier.size(24.dp)
        )
      }
      Column {
        Text(
          text = group.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingXXS))
        Text(
          text = "${group.memberCount} " + "thành viên".localized(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
