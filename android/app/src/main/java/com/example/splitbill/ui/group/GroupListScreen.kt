package com.example.splitbill.ui.group

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.theme.Dimens
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
  val listState = rememberLazyListState()
  var showCreateDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      SplitBillTopBar(
        title = "Nhóm của tôi".localized(),
        actions = {
          IconButton(onClick = onNavigateToSettings) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Cài đặt".localized(),
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      )
    },
    floatingActionButton = {
      ScrollAwareFab(
        listState = listState,
        onClick = { showCreateDialog = true },
        icon = Icons.Default.Add,
        text = "Tạo Nhóm".localized()
      )
    },
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
            modifier = Modifier.align(Alignment.Center)
          )
        }
        is GroupListUiState.Success -> {
          if (state.groups.isEmpty()) {
            EmptyState(
              title = "Chưa có nhóm nào".localized(),
              message = "Hãy tạo nhóm mới để bắt đầu chia tiền nhé!".localized(),
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
              }
            }
          }
        }
      }
    }
  }

  // Create Group Dialog
  if (showCreateDialog) {
    var groupName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Tạo nhóm mới".localized()) },
      text = {
        Column {
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
            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (groupName.isNotBlank()) {
              viewModel.createGroup(groupName.trim())
              showCreateDialog = false
            }
          }
        ) { Text("Tạo nhóm".localized()) }
      },
      dismissButton = {
        TextButton(onClick = { showCreateDialog = false }) { Text("Hủy".localized()) }
      }
    )
  }
}

@Composable
fun GroupCard(group: GroupResponse, onClick: () -> Unit) {
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
      ) {
        Icon(
          imageVector = Icons.Default.Groups,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
      }
      Column {
        Text(
          text = group.name,
          style = MaterialTheme.typography.titleMedium,
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
