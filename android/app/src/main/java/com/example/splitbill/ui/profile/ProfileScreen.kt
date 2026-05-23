package com.example.splitbill.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.theme.Dimens
import com.example.splitbill.ui.components.LoadingState
import com.example.splitbill.ui.components.SplitBillCard
import com.example.splitbill.ui.components.SplitBillTopBar

// Danh sách ngân hàng Việt Nam phổ biến
val VIETNAMESE_BANKS = listOf(
  "ACB" to "ACB - Á Châu",
  "BIDV" to "BIDV",
  "ICB" to "VietinBank",
  "EIB" to "Eximbank",
  "HDB" to "HDBank",
  "LPB" to "LPBank",
  "MB" to "MB Bank",
  "MSB" to "MSB",
  "NAB" to "Nam A Bank",
  "NCB" to "NCB",
  "OCB" to "OCB",
  "PVB" to "PVcomBank",
  "SCB" to "SCB",
  "SEAB" to "SeABank",
  "SGB" to "Saigonbank",
  "SHB" to "SHB",
  "TCB" to "Techcombank",
  "TPB" to "TPBank",
  "VAB" to "VietABank",
  "VCB" to "Vietcombank",
  "VIB" to "VIB",
  "VPB" to "VPBank",
  "WOO" to "Woori Bank"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val saveState by viewModel.saveState.collectAsStateWithLifecycle()

  var bankCode by remember { mutableStateOf("") }
  var accountNumber by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  var showBankSelection by remember { mutableStateOf(false) }
  var accountName by remember { mutableStateOf("") }
  var showSaveSuccess by remember { mutableStateOf(false) }

  // Load form khi profile được tải
  LaunchedEffect(uiState) {
    if (uiState is ProfileUiState.Success) {
      val profile = (uiState as ProfileUiState.Success).profile
      bankCode = profile.bankCode ?: ""
      accountNumber = profile.accountNumber ?: ""
      accountName = profile.accountName ?: ""
    }
  }

  // Xử lý kết quả lưu
  LaunchedEffect(saveState) {
    if (saveState == "success") {
      showSaveSuccess = true
      viewModel.clearSaveState()
    }
  }

  Scaffold(
    topBar = {
      SplitBillTopBar(
        title = "Thông tin thanh toán",
        canNavigateBack = true,
        onNavigateBack = onNavigateBack
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    when (val state = uiState) {
      is ProfileUiState.Loading -> {
        LoadingState(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          message = "Đang tải thông tin..."
        )
      }
      is ProfileUiState.Error -> {
        Box(Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(state.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is ProfileUiState.Success -> {
        LazyColumn(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          contentPadding = PaddingValues(Dimens.SpacingM),
          verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
        ) {
          // Header card - thông tin user
          item {
            SplitBillCard(modifier = Modifier.fillMaxWidth()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Surface(
                  shape = MaterialTheme.shapes.medium,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(56.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      state.profile.username.first().uppercaseChar().toString(),
                      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }
                }
                Spacer(Modifier.width(Dimens.SpacingM))
                Column {
                  Text(
                    state.profile.username,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    state.profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          // VietQR info card
          item {
            SplitBillCard(modifier = Modifier.fillMaxWidth()) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .background(
                        brush = Brush.linearGradient(
                          listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                          )
                        ),
                        shape = MaterialTheme.shapes.small
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      Icons.Default.QrCode2,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Spacer(Modifier.width(Dimens.SpacingS))
                  Text(
                    "Thiết lập VietQR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Spacer(Modifier.height(Dimens.SpacingS))
                Text(
                  "Bạn bè sẽ quét mã QR này khi muốn chuyển khoản trả nợ cho bạn.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Form ngân hàng
          item {
            SplitBillCard(modifier = Modifier.fillMaxWidth()) {
              Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)) {
                Text(
                  "Thông tin ngân hàng",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )

                // Nút chọn ngân hàng xịn xò có Icon
                Card(
                  onClick = { showBankSelection = true },
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (bankCode.isNotEmpty()) {
                      coil3.compose.AsyncImage(
                        model = "https://api.vietqr.io/img/$bankCode.png",
                        contentDescription = null,
                        modifier = Modifier
                          .size(36.dp)
                          .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                      )
                      Spacer(Modifier.width(Dimens.SpacingM))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          VIETNAMESE_BANKS.find { it.first == bankCode }?.second ?: bankCode,
                          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(bankCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    } else {
                      Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                      Spacer(Modifier.width(Dimens.SpacingM))
                      Text(
                        "Chọn Ngân Hàng",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                      )
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                
                if (showBankSelection) {
                  BankSelectionBottomSheet(
                    onDismiss = { showBankSelection = false },
                    onBankSelected = { bankCode = it }
                  )
                }

                // Số tài khoản
                OutlinedTextField(
                  value = accountNumber,
                  onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                  label = { Text("Số tài khoản") },
                  leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                  placeholder = { Text("Nhập số tài khoản") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = MaterialTheme.shapes.medium
                )

                // Tên chủ tài khoản
                OutlinedTextField(
                  value = accountName,
                  onValueChange = { accountName = it.uppercase() },
                  label = { Text("Tên chủ tài khoản") },
                  leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                  placeholder = { Text("NGUYEN VAN A") },
                  keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = MaterialTheme.shapes.medium
                )
              }
            }
          }

          // Nút lưu + thông báo thành công
          item {
            AnimatedVisibility(
              visible = showSaveSuccess,
              enter = fadeIn() + slideInVertically(),
              exit = fadeOut()
            ) {
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(Dimens.SpacingM),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Spacer(Modifier.width(Dimens.SpacingS))
                  Text(
                    "Đã lưu thông tin ngân hàng thành công!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }
            }

            Spacer(Modifier.height(Dimens.SpacingS))

            Button(
              onClick = {
                showSaveSuccess = false
                viewModel.saveBankInfo(bankCode, accountNumber, accountName)
              },
              enabled = bankCode.isNotBlank() && accountNumber.isNotBlank() && accountName.isNotBlank(),
              modifier = Modifier.fillMaxWidth().height(56.dp),
              shape = MaterialTheme.shapes.medium
            ) {
              Icon(Icons.Default.Save, contentDescription = null)
              Spacer(Modifier.width(Dimens.SpacingS))
              Text(
                "Lưu thông tin",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
          }

          item { Spacer(Modifier.height(Dimens.SpacingXL)) }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectionBottomSheet(
  onDismiss: () -> Unit,
  onBankSelected: (String) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var searchQuery by remember { mutableStateOf("") }

  val filteredBanks = remember(searchQuery) {
    VIETNAMESE_BANKS.filter {
      it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.SpacingM)
    ) {
      Text(
        "Chọn Ngân Hàng",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      Spacer(Modifier.height(Dimens.SpacingM))

      // Search bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Tìm kiếm ngân hàng...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = true
      )
      Spacer(Modifier.height(Dimens.SpacingM))

      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(filteredBanks) { (code, name) ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onBankSelected(code)
                onDismiss()
              }
              .padding(vertical = Dimens.SpacingM)
          ) {
            // Icon
            coil3.compose.AsyncImage(
              model = "https://api.vietqr.io/img/$code.png",
              contentDescription = code,
              modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(Dimens.SpacingM))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        item {
          Spacer(Modifier.height(32.dp))
        }
      }
    }
  }
}

