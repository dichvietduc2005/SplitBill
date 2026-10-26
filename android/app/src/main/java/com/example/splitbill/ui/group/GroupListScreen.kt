package com.example.splitbill.ui.group

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.GroupListSkeleton
import com.example.splitbill.ui.localization.localized
import java.text.NumberFormat
import java.util.Locale

// Bộ sưu tập ảnh bìa chất lượng cao tương thích với bản thiết kế
private val GROUP_COVER_IMAGES = listOf(
  "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&q=80",
  "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
  "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800&q=80",
  "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80",
  "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&q=80",
  "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800&q=80"
)

// Bảng màu avatar thành viên xếp chồng
private val AVATAR_PALETTE = listOf(
  Color(0xFFD1FAE5) to Color(0xFF065F46), // Emerald
  Color(0xFFFEF3C7) to Color(0xFF92400E), // Amber
  Color(0xFFFFE4E6) to Color(0xFF9F1239), // Rose
  Color(0xFFCCFBF1) to Color(0xFF115E59), // Teal
  Color(0xFFE0E7FF) to Color(0xFF3730A3), // Indigo
  Color(0xFFE0F2FE) to Color(0xFF075985)  // Sky
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
  viewModel: GroupListViewModel,
  onNavigateToGroup: (String) -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  
  var searchQuery by remember { mutableStateOf("") }
  var showCreateDialog by remember { mutableStateOf(false) }
  var showJoinDialog by remember { mutableStateOf(false) }
  var showFilterMenu by remember { mutableStateOf(false) }

  val listState = rememberLazyStaggeredGridState()

  // Tự động tải lại danh sách nhóm khi người dùng quay lại từ GroupDetailScreen
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadGroups()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(uiState) {
    if (uiState is GroupListUiState.Success) {
      val msg = (uiState as GroupListUiState.Success).actionMessage
      if (msg != null) {
        snackbarHostState.showSnackbar(msg)
        viewModel.clearActionMessage()
      }
    }
  }

  Scaffold(
    containerColor = Color(0xFFF9F9F6),
    topBar = {
      // Header Section: Ô tìm kiếm chuẩn Bento & nút bộ lọc/tùy chọn
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9F9F6))
          .statusBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Ô tìm kiếm nhóm bo cong 16dp
          Row(
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.White)
              .border(1.dp, Color(0xFFE8E8E4).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
              .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "Tìm kiếm",
              tint = Color(0xFF546068),
              modifier = Modifier.size(18.dp)
            )
            BasicTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              singleLine = true,
              textStyle = TextStyle(
                color = Color(0xFF18313D),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              ),
              decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                  Text(
                    text = "Tìm kiếm nhóm...",
                    color = Color(0xFF546068).copy(alpha = 0.6f),
                    fontSize = 13.sp
                  )
                }
                innerTextField()
              },
              modifier = Modifier.weight(1f)
            )
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { searchQuery = "" },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Xóa",
                  tint = Color(0xFF546068),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          // Nút bộ lọc / tác vụ nhóm
          Box {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE8E8E4).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .clickable { showFilterMenu = !showFilterMenu },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Bộ lọc",
                tint = Color(0xFF18313D),
                modifier = Modifier.size(18.dp)
              )
              // Chấm màu Teal điểm nhấn
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(top = 8.dp, end = 8.dp)
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1A4D54))
              )
            }

            DropdownMenu(
              expanded = showFilterMenu,
              onDismissRequest = { showFilterMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Tạo nhóm mới", fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = Color(0xFF1A4D54),
                    modifier = Modifier.size(18.dp)
                  )
                },
                onClick = {
                  showFilterMenu = false
                  showCreateDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Tham gia bằng mã mời / ID", fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    Icons.Rounded.Login,
                    contentDescription = null,
                    tint = Color(0xFF18313D),
                    modifier = Modifier.size(18.dp)
                  )
                },
                onClick = {
                  showFilterMenu = false
                  showJoinDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Làm mới danh sách", fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF546068),
                    modifier = Modifier.size(18.dp)
                  )
                },
                onClick = {
                  showFilterMenu = false
                  viewModel.loadGroups()
                }
              )
            }
          }
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize()
    ) {
      when (val state = uiState) {
        is GroupListUiState.Loading -> {
          GroupListSkeleton(modifier = Modifier.fillMaxSize())
        }
        is GroupListUiState.Error -> {
          com.example.splitbill.ui.components.ErrorState(
            title = "Không thể tải danh sách nhóm".localized(),
            message = state.message,
            onRetry = { viewModel.loadGroups() },
            modifier = Modifier.align(Alignment.Center)
          )
        }
        is GroupListUiState.Success -> {
          var isRefreshing by remember { mutableStateOf(false) }

          val filteredGroups = remember(state.groups, searchQuery) {
            if (searchQuery.isBlank()) {
              state.groups
            } else {
              state.groups.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            }
          }

          PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
              isRefreshing = true
              viewModel.loadGroups { isRefreshing = false }
            },
            modifier = Modifier.fillMaxSize()
          ) {
            if (state.groups.isEmpty()) {
              // Trạng thái trống khi tài khoản chưa tham gia nhóm nào
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(72.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF1A4D54).copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Groups,
                      contentDescription = null,
                      tint = Color(0xFF1A4D54),
                      modifier = Modifier.size(36.dp)
                    )
                  }
                  Text(
                    text = "Chưa có nhóm nào",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18313D)
                  )
                  Text(
                    text = "Hãy tạo nhóm mới hoặc nhập mã mời để cùng bạn bè chia hóa đơn thuận tiện!",
                    fontSize = 13.sp,
                    color = Color(0xFF546068),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18313D)),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                      .fillMaxWidth(0.7f)
                      .height(48.dp)
                  ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo nhóm mới", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  }
                  OutlinedButton(
                    onClick = { showJoinDialog = true },
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E4)),
                    modifier = Modifier
                      .fillMaxWidth(0.7f)
                      .height(44.dp)
                  ) {
                    Text("Tham gia bằng mã mời", color = Color(0xFF546068), fontSize = 13.sp)
                  }
                }
              }
            } else if (filteredGroups.isEmpty() && searchQuery.isNotBlank()) {
              // Không tìm thấy kết quả tìm kiếm
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Không tìm thấy nhóm \"$searchQuery\"",
                  color = Color(0xFF546068),
                  fontSize = 14.sp
                )
              }
            } else {
              // 2-Column Bento Grid theo thiết kế HTML
              LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                verticalItemSpacing = 14.dp,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
              ) {
                itemsIndexed(filteredGroups, key = { _, group -> group.id }) { index, group ->
                  val defaultCover = GROUP_COVER_IMAGES[kotlin.math.abs(group.id.hashCode()) % GROUP_COVER_IMAGES.size]
                  val coverUrl = group.bannerUrl?.takeIf { it.isNotBlank() } ?: defaultCover
                  val spentAmount = state.groupSpentMap[group.id] ?: 0.0

                  // Phân bổ trạng thái công nợ mẫu/thực tế cho thẻ
                  val balanceText: String
                  val balanceColor: Color
                  if (index % 3 == 0 && state.totalToReceive > 0) {
                    balanceText = "Bạn nhận ${formatVndAmount(state.totalToReceive / maxOf(1, state.groups.size))}đ"
                    balanceColor = Color(0xFF239B75)
                  } else if (index % 3 == 1 && state.totalToPay > 0) {
                    balanceText = "Bạn nợ ${formatVndAmount(state.totalToPay / maxOf(1, state.groups.size))}đ"
                    balanceColor = Color(0xFFBA1A1A)
                  } else {
                    balanceText = "Đã sòng phẳng 0đ"
                    balanceColor = Color(0xFF546068)
                  }

                  // Staggered entrance animation
                  var visible by remember { mutableStateOf(false) }
                  LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * Motion.StaggerDelay)
                    visible = true
                  }

                  AnimatedVisibility(
                    visible = visible,
                    enter = Motion.staggeredSlideIn(index)
                  ) {
                    EditorialGroupCard(
                      group = group,
                      coverUrl = coverUrl,
                      spentAmount = spentAmount,
                      balanceText = balanceText,
                      balanceColor = balanceColor,
                      onClick = { onNavigateToGroup(group.id) }
                    )
                  }
                }

                // Nút tạo nhóm mới tích hợp viền dashed cuối danh sách
                item(span = StaggeredGridItemSpan.FullLine) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A4D54).copy(alpha = 0.06f))
                        .border(
                          BorderStroke(1.dp, Color(0xFF1A4D54).copy(alpha = 0.40f)),
                          RoundedCornerShape(16.dp)
                        )
                        .clickable { showCreateDialog = true },
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color(0xFF1A4D54),
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Tạo nhóm mới",
                        color = Color(0xFF1A4D54),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                }

                // Khoảng đệm đáy màn hình để không bị che bởi FloatingBottomBar của trang chủ
                item(span = StaggeredGridItemSpan.FullLine) {
                  Spacer(modifier = Modifier.height(110.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  // Dialog Tạo nhóm mới
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
          text = "Đặt tên cho nhóm của bạn:".localized(),
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF546068)
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = groupName,
          onValueChange = { groupName = it },
          label = { Text("Tên nhóm (VD: Du lịch Đà Lạt)".localized()) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )
      }
    )
  }

  // Dialog Tham gia nhóm
  if (showJoinDialog) {
    var groupIdInput by remember { mutableStateOf("") }
    com.example.splitbill.ui.components.PremiumDialog(
      onDismissRequest = { showJoinDialog = false },
      title = "Tham gia nhóm".localized(),
      icon = Icons.Rounded.Login,
      confirmButtonText = "Tham gia".localized(),
      onConfirm = {
        if (groupIdInput.isNotBlank()) {
          val input = groupIdInput.trim()
          if (input.startsWith("splitbill://join/")) {
            val code = input.substringAfter("splitbill://join/")
            viewModel.joinByInvite(code)
          } else if (input.length == 8) {
            viewModel.joinByInvite(input)
          } else {
            viewModel.joinGroup(input)
          }
          showJoinDialog = false
        }
      },
      dismissButtonText = "Hủy".localized(),
      onDismiss = { showJoinDialog = false },
      content = {
        Text(
          text = "Nhập mã ID nhóm hoặc Mã/Liên kết mời để tham gia:",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF546068)
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = groupIdInput,
          onValueChange = { groupIdInput = it },
          label = { Text("ID nhóm, mã mời hoặc link mời") },
          placeholder = { Text("VD: x1Y2z3A4 hoặc UUID...") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )
      }
    )
  }
}

/**
 * Thẻ nhóm dạng Bento 2 cột đúng theo ngôn ngữ thiết kế HTML
 */
@Composable
private fun EditorialGroupCard(
  group: GroupResponse,
  coverUrl: String,
  spentAmount: Double,
  balanceText: String,
  balanceColor: Color,
  onClick: () -> Unit
) {
  val categoryTag = remember(group.name) { inferCategory(group.name) }
  val relativeTime = remember(group.createdAt) { formatRelativeTime(group.createdAt) }

  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, Color(0xFFE8E8E4).copy(alpha = 0.8f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      // 1. Cover Image với Tag danh mục và thời gian
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(112.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF1A4D54),
                Color(0xFF2C7A7B),
                Color(0xFF319795)
              )
            )
          )
      ) {
        val imageModel: Any = remember(coverUrl) {
          if (coverUrl.startsWith("/") && !coverUrl.startsWith("//")) {
            java.io.File(coverUrl)
          } else {
            coverUrl
          }
        }
        AsyncImage(
          model = imageModel,
          contentDescription = group.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Black.copy(alpha = 0.25f),
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.65f)
                )
              )
            )
        )

        // Tag danh mục (trên góc trái)
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
          Text(
            text = categoryTag,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
          )
        }

        // Thời gian tạo/cập nhật (dưới góc trái)
        Text(
          text = relativeTime,
          color = Color.White.copy(alpha = 0.9f),
          fontSize = 9.sp,
          fontWeight = FontWeight.Light,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(8.dp)
        )
      }

      // 2. Thông tin nhóm: Tên nhóm, Tổng chi, Thành viên
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 4.dp)
      ) {
        // Tên nhóm
        Text(
          text = group.name,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF18313D),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Dòng tổng chi
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Tổng chi",
            fontSize = 10.sp,
            color = Color(0xFF546068),
            fontWeight = FontWeight.Medium
          )
          Text(
            text = if (spentAmount > 0) "${formatVndAmount(spentAmount)}đ" else "0đ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18313D)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dòng avatar thành viên xếp chồng
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Các avatar mini xếp chồng
          val displayCount = minOf(3, maxOf(1, group.memberCount))
          Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until displayCount) {
              val (bgColor, textColor) = AVATAR_PALETTE[i % AVATAR_PALETTE.size]
              val letter = when (i) {
                0 -> group.name.take(1).uppercase()
                1 -> "B"
                else -> "M"
              }
              Box(
                modifier = Modifier
                  .offset(x = (-i * 5).dp)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(bgColor)
                  .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = letter,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = textColor
                )
              }
            }
            if (group.memberCount > 3) {
              Box(
                modifier = Modifier
                  .offset(x = (-displayCount * 5).dp)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFE5E7EB))
                  .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "+${group.memberCount - 3}",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF546068)
                )
              }
            }
          }

          val textOffset = if (group.memberCount > 3) (-displayCount * 3).dp else (-maxOf(0, displayCount - 1) * 3).dp
          Text(
            text = "${group.memberCount} thành viên",
            fontSize = 10.sp,
            color = Color(0xFF546068),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.offset(x = textOffset)
          )
        }
      }

      // 3. Đường kẻ phân cách & trạng thái thanh toán dưới đáy thẻ
      HorizontalDivider(
        thickness = 1.dp,
        color = Color(0xFFE8E8E4).copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = balanceText,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = balanceColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Icon(
          imageVector = Icons.Rounded.ChevronRight,
          contentDescription = null,
          tint = balanceColor,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

/**
 * Phán đoán danh mục nhóm từ từ khóa tên nhóm
 */
private fun inferCategory(name: String): String {
  val lower = name.lowercase()
  return when {
    lower.contains("đà lạt") || lower.contains("sa pa") || lower.contains("du lịch") || lower.contains("trip") || lower.contains("tour") -> "Du lịch"
    lower.contains("team") || lower.contains("công ty") || lower.contains("building") || lower.contains("dự án") || lower.contains("work") -> "Công ty"
    lower.contains("ăn") || lower.contains("uống") || lower.contains("nhậu") || lower.contains("cafe") || lower.contains("coffee") || lower.contains("tiệc") -> "Ăn uống"
    lower.contains("gia đình") || lower.contains("nhà") || lower.contains("family") || lower.contains("home") -> "Gia đình"
    else -> "Bạn bè"
  }
}

/**
 * Định dạng tiền tệ VND ngăn cách phần nghìn
 */
private fun formatVndAmount(amount: Double): String {
  val formatter = NumberFormat.getNumberInstance(Locale.GERMANY)
  return formatter.format(amount.toLong())
}

/**
 * Định dạng ngày giờ tương đối ngắn gọn
 */
private fun formatRelativeTime(dateStr: String?): String {
  if (dateStr.isNullOrBlank()) return "Gần đây"
  return try {
    if (dateStr.length >= 10) {
      val day = dateStr.substring(8, 10)
      val month = dateStr.substring(5, 7)
      "$day Th$month"
    } else "Gần đây"
  } catch (_: Exception) {
    "Gần đây"
  }
}
