package com.example.splitbill.ui.group

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.model.BillCategory
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.EmptyState
import com.example.splitbill.ui.components.GradientAvatar
import com.example.splitbill.ui.components.GroupDetailSkeleton
import com.example.splitbill.ui.components.PremiumDialog
import com.example.splitbill.ui.components.SpeedDialFab
import com.example.splitbill.ui.components.SpeedDialItem
import com.example.splitbill.ui.localization.localized
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
  viewModel: GroupDetailViewModel,
  refreshSignal: Int,
  initialBillId: String? = null,
  onNavigateBack: () -> Unit,
  onAddBill: (groupId: String, members: List<MemberResponse>) -> Unit,
  onViewDebts: (groupId: String) -> Unit,
  onViewStats: (groupId: String) -> Unit,
  onViewActivities: (groupId: String) -> Unit,
  modifier: Modifier = Modifier,
  onEditBill: (groupId: String, bill: BillResponse, members: List<MemberResponse>) -> Unit
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val tokenManager = remember { TokenManager(context) }
  val currentUserId = remember { tokenManager.getUserId() }

  var showInviteSheet by remember { mutableStateOf(false) }
  var showMembersSheet by remember { mutableStateOf(false) }
  var showGroupInfoDialog by remember { mutableStateOf(false) }
  var showOptionsMenu by remember { mutableStateOf(false) }
  var showChangeBannerSheet by remember { mutableStateOf(false) }

  // Launcher chọn ảnh từ thiết bị
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val (localPath, bytes) = try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = inputStream?.use { android.graphics.BitmapFactory.decodeStream(it) }
        val compressedBytes = if (originalBitmap != null) {
          val maxDim = 1200
          val width = originalBitmap.width
          val height = originalBitmap.height
          val scale = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height).coerceAtMost(1.0f)
          val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
          val bos = java.io.ByteArrayOutputStream()
          scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos)
          bos.toByteArray()
        } else null

        val bannerDir = java.io.File(context.filesDir, "banners").apply { mkdirs() }
        val targetFile = java.io.File(bannerDir, "banner_${state.group?.id ?: "temp"}_${System.currentTimeMillis()}.jpg")
        if (compressedBytes != null) {
          targetFile.writeBytes(compressedBytes)
        }
        Pair(targetFile.toURI().toString(), compressedBytes)
      } catch (_: Exception) {
        Pair(uri.toString(), null)
      }

      // Cập nhật preview cục bộ tức thì
      viewModel.updateBanner(localPath)
      // Đồng thời tải ảnh lên Supabase Storage để lưu trữ vĩnh viễn trên đám mây
      if (bytes != null) {
        viewModel.uploadBanner(bytes)
      }
      showChangeBannerSheet = false
    }
  }

  val defaultBannerIndex = remember(state.group?.id) {
    val id = state.group?.id ?: ""
    (Math.abs(id.hashCode())) % PRESET_GROUP_BANNERS.size
  }
  val currentBannerUrl = state.group?.bannerUrl?.takeIf { it.isNotBlank() } ?: PRESET_GROUP_BANNERS[defaultBannerIndex].url

  // Tab: 0 = Khoản chi, 1 = Công nợ
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }

  // Auto-refresh khi signal thay đổi (quay lại từ AddBill)
  LaunchedEffect(refreshSignal) {
    if (refreshSignal > 0) {
      viewModel.refreshBills()
    }
  }

  val listState = androidx.compose.foundation.lazy.rememberLazyListState()
  var hasScrolledToInitialBill by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(state.bills, initialBillId) {
    if (!hasScrolledToInitialBill && !initialBillId.isNullOrBlank() && state.bills.isNotEmpty()) {
      val billIndex = state.bills.indexOfFirst { it.id == initialBillId }
      if (billIndex != -1) {
        hasScrolledToInitialBill = true
        // Delay scroll đến hóa đơn được chọn
        kotlinx.coroutines.delay(300)
        listState.animateScrollToItem((billIndex + 1).coerceAtLeast(0))
      }
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(state.actionMessage) {
    state.actionMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearActionMessage()
    }
  }

  var fabVisible by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(400)
    fabVisible = true
  }

  // Tính toán số liệu tài chính tổng quan cho nhóm
  val totalSpent = remember(state.bills) {
    state.bills.sumOf { it.totalAmount * (if (it.exchangeRate > 0) it.exchangeRate else 1.0) }
  }
  val myBalance = remember(state.memberBalances, currentUserId) {
    currentUserId?.let { state.memberBalances[it] } ?: 0.0
  }
  val myDebt = if (myBalance < 0) -myBalance else 0.0
  val myCredit = if (myBalance > 0) myBalance else 0.0

  Scaffold(
    containerColor = Color(0xFFF9F9F6),
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
              label = "Thêm khoản chi",
              onClick = { onAddBill(state.group?.id ?: "", state.members) }
            ),
            SpeedDialItem(
              icon = Icons.Rounded.PersonAdd,
              label = "Mời thành viên",
              onClick = { showInviteSheet = true }
            ),
            SpeedDialItem(
              icon = Icons.Rounded.BarChart,
              label = "Xem thống kê",
              onClick = { onViewStats(state.group?.id ?: "") }
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
        modifier = Modifier
          .padding(paddingValues)
          .fillMaxSize()
      )
    } else if (state.error != null) {
      EmptyState(
        title = "Lỗi tải dữ liệu",
        message = state.error!!,
        modifier = Modifier
          .padding(paddingValues)
          .fillMaxSize()
      )
    } else {
      var isRefreshing by remember { mutableStateOf(false) }
      LaunchedEffect(state.isLoading) {
        if (!state.isLoading) isRefreshing = false
      }

      // Bộ lọc và tìm kiếm hóa đơn cho Tab Khoản chi
      var searchQuery by rememberSaveable { mutableStateOf("") }
      var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
      var selectedSortOption by rememberSaveable { mutableStateOf(BillSortOption.NEWEST) }
      var sortDropdownExpanded by remember { mutableStateOf(false) }

      val filteredBills = remember(state.bills, searchQuery, selectedCategoryFilter, selectedSortOption) {
        state.bills.filter { bill ->
          val matchesSearch = searchQuery.isBlank() ||
            bill.description.contains(searchQuery, ignoreCase = true) ||
            bill.paidByUsername.contains(searchQuery, ignoreCase = true)
          val matchesCategory = selectedCategoryFilter == null || bill.category == selectedCategoryFilter
          matchesSearch && matchesCategory
        }.let { list ->
          when (selectedSortOption) {
            BillSortOption.NEWEST -> list
            BillSortOption.OLDEST -> list.reversed()
            BillSortOption.HIGHEST -> list.sortedByDescending { it.totalAmount * (if (it.exchangeRate > 0) it.exchangeRate else 1.0) }
            BillSortOption.LOWEST -> list.sortedBy { it.totalAmount * (if (it.exchangeRate > 0) it.exchangeRate else 1.0) }
          }
        }
      }

      val pullRefreshState = rememberPullToRefreshState()
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
          isRefreshing = true
          viewModel.loadAll { isRefreshing = false }
        },
        state = pullRefreshState,
        modifier = Modifier
          .padding(bottom = paddingValues.calculateBottomPadding())
          .fillMaxSize()
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize()
        ) {
          // ─── 1. Group Scenic Hero Section (Ảnh tràn viền + Tên & Số lượng đè lên ảnh) ────
          item {
            GroupScenicHeroSection(
              groupName = state.group?.name ?: "Chi tiết nhóm",
              createdByName = state.group?.createdByName?.takeIf { it.isNotBlank() } ?: "bạn",
              members = state.members,
              bannerUrl = currentBannerUrl,
              onNavigateBack = onNavigateBack,
              onViewStats = { onViewStats(state.group?.id ?: "") },
              onChangeBannerClick = { showChangeBannerSheet = true },
              onShareClick = {
                val group = state.group
                if (group != null) {
                  val joinLink = "https://splitbill.app/join/${group.id}"
                  val shareText = "🎉 Tham gia nhóm chia tiền \"${group.name}\" trên SplitBill nhé!\n" +
                    "👉 $joinLink\n(Mã nhóm: ${group.id})"
                  val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                  }
                  context.startActivity(android.content.Intent.createChooser(sendIntent, "Chia sẻ nhóm"))
                }
              },
              onOptionsClick = { showOptionsMenu = true },
              onMembersClick = { showMembersSheet = true },
              optionsMenuContent = {
                DropdownMenu(
                  expanded = showOptionsMenu,
                  onDismissRequest = { showOptionsMenu = false }
                ) {
                  DropdownMenuItem(
                    text = { Text("Đổi ảnh nền nhóm", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.CameraAlt, null, tint = Color(0xFF18313D)) },
                    onClick = {
                      showOptionsMenu = false
                      showChangeBannerSheet = true
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Thống kê chi tiêu", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.BarChart, null, tint = Color(0xFF18313D)) },
                    onClick = {
                      showOptionsMenu = false
                      onViewStats(state.group?.id ?: "")
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Lịch sử hoạt động", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.History, null, tint = Color(0xFF546068)) },
                    onClick = {
                      showOptionsMenu = false
                      onViewActivities(state.group?.id ?: "")
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Mời thành viên", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.PersonAdd, null, tint = Color(0xFF18313D)) },
                    onClick = {
                      showOptionsMenu = false
                      showInviteSheet = true
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Thông tin nhóm", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Info, null, tint = Color(0xFF546068)) },
                    onClick = {
                      showOptionsMenu = false
                      showGroupInfoDialog = true
                    }
                  )
                }
              }
            )
          }

          // ─── 2. Financial Summary Section (Card độ khối chuẩn HomeScreen nổi trên nền) ─────
          item {
            Box(
              modifier = Modifier
                .offset(y = (-14).dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
            ) {
              Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                // Cột 1: Tổng chi
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "TỔNG CHI",
                    fontSize = 11.sp,
                    color = Color(0xFF546068),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  )
                  Text(
                    text = "${formatVndAmount(totalSpent)}đ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF011C27),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                // Vạch kẻ dọc 1
                Box(
                  modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Color(0xFFEEEEEB))
                )

                // Cột 2: Bạn đang nợ
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                ) {
                  Text(
                    text = "BẠN ĐANG NỢ",
                    fontSize = 11.sp,
                    color = Color(0xFF546068),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  )
                  Text(
                    text = "${formatVndAmount(myDebt)}đ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBA1A1A),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                // Vạch kẻ dọc 2
                Box(
                  modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Color(0xFFEEEEEB))
                )

                // Cột 3: Bạn được nhận
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
                ) {
                  Text(
                    text = "BẠN ĐƯỢC NHẬN",
                    fontSize = 11.sp,
                    color = Color(0xFF546068),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  )
                  Text(
                    text = "${formatVndAmount(myCredit)}đ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF239B75),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }

        // ─── 3. Tabs Navigation (Khoản chi / Công nợ) ─────────────────────
        item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .drawBottomBorder(Color(0xFFEEEEEB), strokeWidth = 1.dp)
            ) {
              // Tab Khoản chi
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedTab = 0 }
                  .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Khoản chi (${state.bills.size})",
                  fontSize = 14.sp,
                  fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                  color = if (selectedTab == 0) Color(0xFF011C27) else Color(0xFF546068)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedTab == 0) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(2.5.dp)
                      .clip(RoundedCornerShape(100.dp))
                      .background(Color(0xFF18313D))
                  )
                } else {
                  Spacer(modifier = Modifier.height(2.5.dp))
                }
              }

              // Tab Công nợ
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedTab = 1 }
                  .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Công nợ (${state.members.size})",
                  fontSize = 14.sp,
                  fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                  color = if (selectedTab == 1) Color(0xFF011C27) else Color(0xFF546068)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedTab == 1) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(2.5.dp)
                      .clip(RoundedCornerShape(100.dp))
                      .background(Color(0xFF18313D))
                  )
                } else {
                  Spacer(modifier = Modifier.height(2.5.dp))
                }
              }
            }
          }

          // ─── 4. Nội dung theo Tab đã chọn ────────────────────────────────
          if (selectedTab == 0) {
            // ─── TAB 0: KHOẢN CHI ───────────────────────────────────────────

            // Thanh tìm kiếm & Sắp xếp
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Ô tìm kiếm bo cong 16dp với độ khối đồng bộ HomeScreen
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = Color.White,
                  border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                  shadowElevation = 2.dp,
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = Color(0xFF011C27),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                      ),
                      decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                          Text(
                            text = "Tìm kiếm khoản chi...",
                            color = Color(0xFF8F9799),
                            fontSize = 13.sp
                          )
                        }
                        innerTextField()
                      },
                      modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Xóa",
                        tint = Color(0xFF546068),
                        modifier = Modifier
                          .size(16.dp)
                          .clickable { searchQuery = "" }
                      )
                    }
                  }
                }

                // Nút sắp xếp độ khối đồng bộ HomeScreen
                Box {
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                      .height(44.dp)
                      .clickable { sortDropdownExpanded = true }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Sort,
                        contentDescription = "Sắp xếp",
                        tint = Color(0xFF18313D),
                        modifier = Modifier.size(18.dp)
                      )
                      Text(
                        text = selectedSortOption.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF18313D)
                      )
                    }
                  }

                  DropdownMenu(
                    expanded = sortDropdownExpanded,
                    onDismissRequest = { sortDropdownExpanded = false }
                  ) {
                    BillSortOption.entries.forEach { option ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            option.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (option == selectedSortOption) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == selectedSortOption) Color(0xFF18313D) else Color(0xFF546068)
                          )
                        },
                        onClick = {
                          selectedSortOption = option
                          sortDropdownExpanded = false
                        },
                        leadingIcon = {
                          if (option == selectedSortOption) {
                            Icon(Icons.Rounded.Check, null, tint = Color(0xFF18313D), modifier = Modifier.size(18.dp))
                          }
                        }
                      )
                    }
                  }
                }
              }
            }

            // Thanh lọc theo Danh mục (GIỮ NGUYÊN DANH MỤC CŨ theo yêu cầu)
            item {
              LazyRow(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                // Chip "Tất cả"
                item {
                  EditorialCategoryChip(
                    label = "Tất cả",
                    icon = null,
                    iconColor = Color.Unspecified,
                    bgColor = Color.Unspecified,
                    isSelected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null }
                  )
                }

                // 16 danh mục gốc của hệ thống (Ăn uống, Di chuyển, Mua sắm, Hóa đơn, Giải trí...)
                items(BillCategory.entries.toTypedArray()) { cat ->
                  val isSelected = selectedCategoryFilter == cat.key
                  EditorialCategoryChip(
                    label = cat.displayName,
                    icon = cat.icon,
                    iconColor = cat.iconColor,
                    bgColor = cat.bgColor,
                    isSelected = isSelected,
                    onClick = {
                      selectedCategoryFilter = if (isSelected) null else cat.key
                    }
                  )
                }
              }
            }

            // Danh sách các khoản chi (Card chuẩn độ khối HomeScreen)
            if (filteredBills.isEmpty()) {
              item {
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color.White),
                  shape = RoundedCornerShape(18.dp),
                  border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(32.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = if (state.bills.isEmpty()) "Chưa có khoản chi nào trong nhóm" else "Không tìm thấy khoản chi phù hợp",
                      color = Color(0xFF546068),
                      fontSize = 14.sp
                    )
                  }
                }
              }
            } else {
              item {
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color.White),
                  shape = RoundedCornerShape(18.dp),
                  border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp)
                  ) {
                    filteredBills.forEachIndexed { index, bill ->
                      EditorialBillItem(
                        bill = bill,
                        currentUserId = currentUserId,
                        isHighlighted = bill.id == initialBillId,
                        onEdit = { onEditBill(state.group?.id ?: "", bill, state.members) },
                        onDelete = { viewModel.deleteBill(bill.id) },
                        onTogglePaid = { viewModel.toggleBillPaidStatus(bill.id, bill.isPaid) }
                      )
                      if (index < filteredBills.size - 1) {
                        HorizontalDivider(
                          thickness = 1.dp,
                          color = Color(0xFFEEEEEB),
                          modifier = Modifier.padding(horizontal = 4.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          } else {
            // ─── TAB 1: CÔNG NỢ ─────────────────────────────────────────────
            item {
              // Thẻ gợi ý quyết toán nhanh (Card chuẩn HomeScreen)
              Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 8.dp)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Quyết toán & Trả nợ tối ưu",
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF011C27)
                    )
                    Text(
                      text = "Tự động đơn giản hóa nợ chéo với số lần chuyển tiền ít nhất",
                      fontSize = 12.sp,
                      color = Color(0xFF546068),
                      modifier = Modifier.padding(top = 2.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Button(
                    onClick = { onViewDebts(state.group?.id ?: "") },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF18313D),
                      contentColor = Color(0xFFF9F9F6)
                    ),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Text("Xem gợi ý", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }

            // Danh sách thành viên và trạng thái công nợ (Card chuẩn HomeScreen)
            if (state.members.isNotEmpty()) {
              item {
                Text(
                  text = "TRẠNG THÁI CÔNG NỢ TỪNG THÀNH VIÊN",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.2.sp,
                  color = Color(0xFF546068),
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
              }

              item {
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color.White),
                  shape = RoundedCornerShape(18.dp),
                  border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp)
                  ) {
                    state.members.forEachIndexed { index, member ->
                      val balance = state.memberBalances[member.userId] ?: 0.0
                      EditorialMemberBalanceItem(
                        member = member,
                        balance = balance,
                        isCurrentUser = member.userId == currentUserId
                      )
                      if (index < state.members.size - 1) {
                        HorizontalDivider(
                          thickness = 1.dp,
                          color = Color(0xFFEEEEEB),
                          modifier = Modifier.padding(horizontal = 4.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Khoảng trống dưới cùng tránh bị che bởi FAB
          item { Spacer(modifier = Modifier.height(100.dp)) }
        }
      }
    }
  }

  // ─── Modal Sheets & Dialogs phụ trợ ─────────────────────────────────────────

  // Sheet danh sách toàn bộ thành viên
  if (showMembersSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMembersSheet = false },
      containerColor = Color(0xFFFAF9F5)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Thành viên nhóm (${state.members.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF011C27)
          )
          TextButton(onClick = { showMembersSheet = false; showInviteSheet = true }) {
            Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(16.dp), tint = Color(0xFF18313D))
            Spacer(Modifier.width(4.dp))
            Text("Mời thêm", color = Color(0xFF18313D))
          }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(state.members) { member ->
            val balance = state.memberBalances[member.userId] ?: 0.0
            EditorialMemberBalanceItem(
              member = member,
              balance = balance,
              isCurrentUser = member.userId == currentUserId
            )
          }
          item { Spacer(Modifier.height(32.dp)) }
        }
      }
    }
  }

  // Sheet mời thành viên
  if (showInviteSheet) {
    val invite by viewModel.activeInvite.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
      onDismissRequest = {
        showInviteSheet = false
        viewModel.clearActiveInvite()
      },
      containerColor = Color.White
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Mời bạn bè vào nhóm",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF011C27)
        )
        Spacer(Modifier.height(16.dp))

        // Mời trực tiếp qua Username / Email
        var memberInput by remember { mutableStateOf("") }
        OutlinedTextField(
          value = memberInput,
          onValueChange = { memberInput = it },
          label = { Text("Username hoặc Email") },
          placeholder = { Text("banbe@gmail.com") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
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
              Icon(Icons.Rounded.Send, contentDescription = "Gửi", tint = Color(0xFF18313D))
            }
          }
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFFEEEEEB))
        Spacer(Modifier.height(20.dp))

        // Mã mời và liên kết
        if (invite == null) {
          Button(
            onClick = { viewModel.loadOrCreateActiveInvite() },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF18313D),
              contentColor = Color(0xFFF9F9F6)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(100.dp)
          ) {
            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tạo liên kết & mã mời QR", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          }
        } else {
          // QR Code
          Box(
            modifier = Modifier
              .size(150.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFFF9F9F6))
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${invite!!.inviteUrl}",
              contentDescription = "Mã QR",
              modifier = Modifier.fillMaxSize()
            )
          }

          Spacer(Modifier.height(12.dp))

          // Card hiển thị mã code và link
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF4F4F1),
            border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Mã mời: ${invite!!.inviteCode}",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF011C27)
                )
                Text(
                  text = invite!!.inviteUrl,
                  fontSize = 12.sp,
                  color = Color(0xFF18313D),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              IconButton(onClick = { clipboardManager.setText(AnnotatedString(invite!!.inviteUrl)) }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Sao chép", tint = Color(0xFF18313D))
              }
            }
          }
        }
        Spacer(Modifier.height(28.dp))
      }
    }
  }

  // Dialog thông tin nhóm
  if (showGroupInfoDialog && state.group != null) {
    val group = state.group!!
    ModalBottomSheet(
      onDismissRequest = { showGroupInfoDialog = false },
      containerColor = Color.White
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = group.name,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF011C27)
        )
        Text(
          text = "Mã nhóm: ${group.id}",
          fontSize = 12.sp,
          color = Color(0xFF546068),
          modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(
          onClick = { showGroupInfoDialog = false },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF18313D),
            contentColor = Color(0xFFF9F9F6)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
          shape = RoundedCornerShape(100.dp)
        ) {
          Text("Đóng")
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }

  // Sheet đổi ảnh bìa / ảnh nền nhóm
  if (showChangeBannerSheet) {
    var customUrlInput by remember { mutableStateOf("") }

    ModalBottomSheet(
      onDismissRequest = { showChangeBannerSheet = false },
      containerColor = Color(0xFFFAF9F5),
      dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFD4D4D0)) }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
      ) {
        // Tiêu đề
        Text(
          text = "Đổi ảnh nền nhóm",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF011C27)
        )
        Text(
          text = "Ảnh nền sẽ hiển thị tràn viền ở đầu trang nhóm và thẻ nhóm ở trang chủ",
          fontSize = 12.5.sp,
          color = Color(0xFF546068),
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Nút chọn từ thư viện máy
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFE8EEF1),
          border = BorderStroke(1.dp, Color(0xFF18313D).copy(alpha = 0.15f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF18313D)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tải ảnh từ thư viện máy",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF011C27)
              )
              Text(
                text = "Chọn ảnh chụp hoặc tải về từ thiết bị của bạn",
                fontSize = 12.sp,
                color = Color(0xFF546068)
              )
            }
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = null,
              tint = Color(0xFF546068),
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(Modifier.height(14.dp))

        // Nhập link URL
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = customUrlInput,
            onValueChange = { customUrlInput = it },
            placeholder = { Text("Hoặc dán URL ảnh mạng (https://...)", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White,
              focusedBorderColor = Color(0xFF18313D),
              unfocusedBorderColor = Color(0xFFDCDCD8)
            )
          )

          Button(
            onClick = {
              if (customUrlInput.isNotBlank()) {
                viewModel.updateBanner(customUrlInput.trim())
                showChangeBannerSheet = false
              }
            },
            enabled = customUrlInput.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF18313D),
              contentColor = Color.White
            ),
            modifier = Modifier.height(52.dp)
          ) {
            Text("Lưu", fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(Modifier.height(20.dp))

        Text(
          text = "Bộ sưu tập phong cảnh có sẵn",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF011C27)
        )

        Spacer(Modifier.height(10.dp))

        // Grid 8 ảnh phong cảnh preset
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(PRESET_GROUP_BANNERS) { preset ->
            val isSelected = currentBannerUrl == preset.url
            Card(
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF18313D) else Color(0xFFE2E2DE)
              ),
              elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.updateBanner(preset.url)
                  showChangeBannerSheet = false
                }
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(88.dp)
              ) {
                AsyncImage(
                  model = preset.url,
                  contentDescription = preset.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                      )
                    )
                )

                // Tên preset
                Text(
                  text = preset.name,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                // Dấu tích đã chọn
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(6.dp)
                      .size(22.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF18313D)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Check,
                      contentDescription = "Đang chọn",
                      tint = Color.White,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

// ─── Sub-Components chuẩn Editorial theo HTML mẫu ────────────────────────────

/**
 * Chip chọn danh mục được giữ nguyên logic và icon của hệ thống cũ
 */
@Composable
private fun EditorialCategoryChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector?,
  iconColor: Color,
  bgColor: Color,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val containerColor = if (isSelected) Color(0xFF18313D) else Color(0xFFF4F4F1)
  val contentColor = if (isSelected) Color(0xFFF9F9F6) else Color(0xFF546068)
  val borderColor = if (isSelected) Color(0xFF18313D) else Color(0xFFEEEEEB)

  Surface(
    shape = RoundedCornerShape(100.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .height(34.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) Color(0xFFF9F9F6) else iconColor,
          modifier = Modifier.size(15.dp)
        )
      }
      Text(
        text = label,
        fontSize = 12.5.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        color = contentColor
      )
    }
  }
}

/**
 * Thẻ hiển thị từng hóa đơn đồng bộ chuẩn màu và độ khối HomeScreen:
 * - Icon danh mục tròn nền pastel
 * - Tiêu đề & ngày tháng / người trả
 * - Số tiền nợ/nhận cá nhân hoặc tổng tiền + số người chia
 */
@Composable
private fun EditorialBillItem(
  bill: BillResponse,
  currentUserId: String?,
  isHighlighted: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onTogglePaid: () -> Unit
) {
  var isExpanded by rememberSaveable(bill.id) { mutableStateOf(isHighlighted) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val cat = BillCategory.fromKey(bill.category)

  // Tính số tiền liên quan đến user hiện tại
  val mySplit = bill.splits.firstOrNull { it.userId == currentUserId }
  val isPayer = bill.paidByUserId == currentUserId

  val amountText: String
  val amountColor: Color
  if (isPayer) {
    val lentAmount = bill.totalAmount - (mySplit?.amountOwed ?: 0.0)
    amountText = "+${formatVndAmount(lentAmount)}đ"
    amountColor = Color(0xFF239B75) // Xanh lá: Bạn được nhận lại
  } else if (mySplit != null) {
    amountText = "-${formatVndAmount(mySplit.amountOwed)}đ"
    amountColor = Color(0xFFBA1A1A) // Đỏ: Bạn nợ phần này
  } else {
    amountText = "${formatVndAmount(bill.totalAmount)}đ"
    amountColor = Color(0xFF011C27) // Trung tính
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { isExpanded = !isExpanded }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Cụm trái: Icon danh mục + Tiêu đề + Ngày tháng/người trả
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Icon danh mục tròn nền pastel mềm mại
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(cat.bgColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = cat.icon,
            contentDescription = cat.displayName,
            tint = cat.iconColor,
            modifier = Modifier.size(20.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = bill.description,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF011C27),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (bill.isPaid) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF239B75).copy(alpha = 0.12f))
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "Đã trả",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF239B75)
                )
              }
            }
          }
          Text(
            text = "${formatShortDate(bill.createdAt)} · ${bill.paidByUsername}",
            fontSize = 12.sp,
            color = Color(0xFF546068),
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      // Cụm phải: Số tiền + số người + mũi tên chevron
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = amountText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
          )
          Text(
            text = "${bill.splits.size} người",
            fontSize = 11.sp,
            color = Color(0xFF8F9799),
            modifier = Modifier.padding(top = 1.dp)
          )
        }

        Icon(
          imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ChevronRight,
          contentDescription = null,
          tint = Color(0xFF8F9799),
          modifier = Modifier.size(18.dp)
        )
      }
    }

    // Phần mở rộng chi tiết khi bấm vào hóa đơn
    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = Motion.springGentle()) + fadeIn(),
      exit = shrinkVertically(animationSpec = Motion.springGentle()) + fadeOut()
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF4F4F1),
        border = BorderStroke(1.dp, Color(0xFFEEEEEB)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 14.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Tổng giá trị gốc
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Tổng giá trị hóa đơn:", fontSize = 12.sp, color = Color(0xFF546068))
            Text("${formatVndAmount(bill.totalAmount)} ${bill.currency}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF011C27))
          }

          Spacer(Modifier.height(8.dp))
          HorizontalDivider(color = Color(0xFFEEEEEB), thickness = 1.dp)
          Spacer(Modifier.height(8.dp))

          // Danh sách người chia
          Text("Phần chia tiền:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF011C27))
          Spacer(Modifier.height(4.dp))
          bill.splits.forEach { split ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(split.username, fontSize = 12.5.sp, color = Color(0xFF546068))
              Text("${formatVndAmount(split.amountOwed)}đ", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF011C27))
            }
          }

          Spacer(Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFEEEEEB), thickness = 1.dp)
          Spacer(Modifier.height(8.dp))

          // Các nút thao tác: Sửa / Đổi trạng thái / Xóa
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = onTogglePaid) {
              Text(if (bill.isPaid) "Đánh dấu chưa trả" else "Đánh dấu đã trả", fontSize = 12.sp, color = Color(0xFF18313D))
            }
            TextButton(onClick = onEdit) {
              Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp), tint = Color(0xFF18313D))
              Spacer(Modifier.width(4.dp))
              Text("Sửa", fontSize = 12.sp, color = Color(0xFF18313D))
            }
            TextButton(onClick = { showDeleteDialog = true }) {
              Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(14.dp), tint = Color(0xFFBA1A1A))
              Spacer(Modifier.width(4.dp))
              Text("Xóa", fontSize = 12.sp, color = Color(0xFFBA1A1A))
            }
          }
        }
      }
    }
  }

  if (showDeleteDialog) {
    PremiumDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = "Xóa khoản chi?",
      icon = Icons.Rounded.DeleteSweep,
      confirmButtonText = "Xóa",
      onConfirm = { onDelete(); showDeleteDialog = false },
      dismissButtonText = "Hủy",
      onDismiss = { showDeleteDialog = false },
      content = {
        Text("Bạn có chắc muốn xóa khoản chi '${bill.description}' không? Hành động này không thể hoàn tác.")
      }
    )
  }
}

/**
 * Mục thành viên và công nợ trong Tab Công nợ
 */
@Composable
private fun EditorialMemberBalanceItem(
  member: MemberResponse,
  balance: Double,
  isCurrentUser: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (!member.avatarUrl.isNullOrBlank()) {
        AsyncImage(
          model = member.avatarUrl,
          contentDescription = member.username,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(1.5.dp, Color(0xFFEEEEEB), CircleShape),
          contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
      } else {
        GradientAvatar(name = member.username, size = 38.dp)
      }

      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = member.username,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF011C27)
          )
          if (isCurrentUser) {
            Spacer(Modifier.width(4.dp))
            Text("(Bạn)", fontSize = 11.sp, color = Color(0xFF8F9799))
          }
        }
        Text(
          text = when {
            balance == 0.0 -> "Đã thanh toán xong"
            balance > 0 -> "Nhóm nợ người này"
            else -> "Đang nợ nhóm"
          },
          fontSize = 12.sp,
          color = when {
            balance == 0.0 -> Color(0xFF8F9799)
            balance > 0 -> Color(0xFF239B75)
            else -> Color(0xFFBA1A1A)
          }
        )
      }
    }

    // Hiển thị số tiền
    val balanceText = when {
      balance == 0.0 -> "0đ"
      balance > 0 -> "+${formatVndAmount(balance)}đ"
      else -> "-${formatVndAmount(-balance)}đ"
    }
    val balanceColor = when {
      balance == 0.0 -> Color(0xFF8F9799)
      balance > 0 -> Color(0xFF239B75)
      else -> Color(0xFFBA1A1A)
    }

    Text(
      text = balanceText,
      fontSize = 14.5.sp,
      fontWeight = FontWeight.Bold,
      color = balanceColor
    )
  }
}

// ─── Utility Formatters ──────────────────────────────────────────────────────

private fun formatVndAmount(amount: Double): String {
  val formatter = DecimalFormat("#,###")
  return formatter.format(Math.abs(amount).toLong())
}

private fun formatShortDate(isoString: String?): String {
  if (isoString.isNullOrBlank()) return ""
  return try {
    val cleaned = isoString.replace("Z", "").replace("T", " ")
    val parts = cleaned.split(" ")
    val dateParts = parts[0].split("-")
    if (dateParts.size == 3) {
      val day = dateParts[2]
      val month = dateParts[1]
      val year = dateParts[0]
      "$day/$month/$year"
    } else {
      parts[0]
    }
  } catch (_: Exception) {
    isoString ?: ""
  }
}

/**
 * Modifier vẽ đường viền dưới mỏng
 */
private fun Modifier.drawBottomBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp): Modifier {
  return this.drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    val y = size.height - strokeWidthPx / 2
    drawLine(
      color = color,
      start = androidx.compose.ui.geometry.Offset(0f, y),
      end = androidx.compose.ui.geometry.Offset(size.width, y),
      strokeWidth = strokeWidthPx
    )
  }
}

enum class BillSortOption(val displayName: String) {
  NEWEST("Mới nhất"),
  OLDEST("Cũ nhất"),
  HIGHEST("Giá trị cao nhất"),
  LOWEST("Giá trị thấp nhất")
}

data class PresetGroupBanner(
  val id: String,
  val name: String,
  val url: String
)

val PRESET_GROUP_BANNERS = listOf(
  PresetGroupBanner(
    id = "dalat",
    name = "Rừng thông Đà Lạt",
    url = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "sunset",
    name = "Hoàng hôn biển",
    url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "stars",
    name = "Bầu trời sao cắm trại",
    url = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "valley",
    name = "Thung lũng & suối mát",
    url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "cafe",
    name = "Quán cà phê ấm cúng",
    url = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "lake",
    name = "Hồ nước & núi mờ",
    url = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "sunset_lake",
    name = "Nắng vàng chiều tà",
    url = "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=1200&q=80"
  ),
  PresetGroupBanner(
    id = "sunrise",
    name = "Bình minh rừng cây",
    url = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=1200&q=80"
  )
)

/**
 * Hero Banner Section ở đầu trang:
 * - Ảnh phong cảnh tràn viền có thể đổi được
 * - Các nút thao tác Back / Đổi ảnh / Thống kê / Chia sẻ / Tùy chọn nổi phủ kính
 * - Tên nhóm & số lượng thành viên đè trực tiếp lên ảnh với gradient nền sắc nét
 */
@Composable
private fun GroupScenicHeroSection(
  groupName: String,
  createdByName: String,
  members: List<MemberResponse>,
  bannerUrl: String,
  onNavigateBack: () -> Unit,
  onViewStats: () -> Unit,
  onChangeBannerClick: () -> Unit,
  onShareClick: () -> Unit,
  onOptionsClick: () -> Unit,
  onMembersClick: () -> Unit,
  optionsMenuContent: @Composable () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(290.dp)
  ) {
    // 1. Ảnh nền nhóm tràn viền (Edge-to-edge Hero Image)
    AsyncImage(
      model = bannerUrl,
      contentDescription = "Ảnh nền nhóm",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Lớp phủ chuyển sắc trên (Top gradient scrim giúp các nút thao tác luôn rõ nét)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(110.dp)
        .align(Alignment.TopCenter)
        .background(
          Brush.verticalGradient(
            listOf(
              Color.Black.copy(alpha = 0.58f),
              Color.Black.copy(alpha = 0.20f),
              Color.Transparent
            )
          )
        )
    )

    // 3. Lớp phủ chuyển sắc dưới (Bottom dark scrim giúp Tên nhóm & Thành viên đè lên ảnh sắc nét tuyệt đối)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .align(Alignment.BottomCenter)
        .background(
          Brush.verticalGradient(
            listOf(
              Color.Transparent,
              Color(0xFF011C27).copy(alpha = 0.40f),
              Color(0xFF011C27).copy(alpha = 0.85f),
              Color(0xFF011C27).copy(alpha = 0.98f)
            )
          )
        )
    )

    // 4. Thanh điều hướng nổi phía trên (Top Actions Bar)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Nút Quay lại (Glass circle)
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.38f))
          .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
          .clickable(onClick = onNavigateBack),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.ArrowBackIosNew,
          contentDescription = "Quay lại",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }

      // Nhóm nút bên phải: Đổi ảnh + Thống kê + Chia sẻ + Menu
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Nút ĐỔI ẢNH BÌA (Glass pill button)
        Surface(
          shape = RoundedCornerShape(100.dp),
          color = Color.Black.copy(alpha = 0.38f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
          modifier = Modifier.clickable(onClick = onChangeBannerClick)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.CameraAlt,
              contentDescription = "Đổi ảnh bìa",
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "Đổi ảnh",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }
        }

        // Nút THỐNG KÊ (Glass pill button)
        Surface(
          shape = RoundedCornerShape(100.dp),
          color = Color.Black.copy(alpha = 0.38f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
          modifier = Modifier.clickable(onClick = onViewStats)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.BarChart,
              contentDescription = "Thống kê",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Thống kê",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }
        }

        // Nút Chia sẻ link nhóm
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onShareClick),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = "Chia sẻ nhóm",
            tint = Color.White,
            modifier = Modifier.size(17.dp)
          )
        }

        // Nút Menu 3 chấm
        Box {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.38f))
              .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
              .clickable(onClick = onOptionsClick),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = "Tùy chọn",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          optionsMenuContent()
        }
      }
    }

    // 5. TÊN NHÓM VÀ SỐ LƯỢNG THÀNH VIÊN NẰM ĐÈ LÊN ẢNH
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 26.dp)
    ) {
      Text(
        text = groupName.ifBlank { "Chi tiết nhóm" },
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = (-0.5).sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = "Tạo bởi ${createdByName.ifBlank { "bạn" }} · ${members.size} thành viên",
        fontStyle = FontStyle.Italic,
        fontSize = 13.5.sp,
        color = Color.White.copy(alpha = 0.88f),
        modifier = Modifier.padding(top = 2.dp)
      )

      // Stack các avatar thành viên nằm đè lên ảnh
      Row(
        modifier = Modifier
          .padding(top = 10.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onMembersClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy((-8).dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val displayMembers = members.take(4)
          displayMembers.forEach { member ->
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color.White.copy(alpha = 0.3f)),
              contentAlignment = Alignment.Center
            ) {
              if (!member.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                  model = member.avatarUrl,
                  contentDescription = member.username,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                GradientAvatar(name = member.username, size = 26.dp)
              }
            }
          }

          if (members.size > 4) {
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "+${members.size - 4}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }

        Text(
          text = "${members.size} thành viên",
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White.copy(alpha = 0.95f)
        )
      }
    }
  }
}
