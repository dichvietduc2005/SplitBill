package com.example.splitbill.ui.bill

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.BillSplitItem
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.localization.localized
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.components.SplitBillCard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.splitbill.ui.components.ConfettiOverlay
import java.util.Locale
import com.example.splitbill.data.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillScreen(
  viewModel: AddBillViewModel,
  groupId: String,
  members: List<MemberResponse>,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  existingBill: BillResponse? = null
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val tokenManager = remember { TokenManager(context) }
  val token by tokenManager.getToken().collectAsState(initial = null)
  val currentUserId = remember(token) { TokenManager.getUserIdFromToken(token) }

  val isEditMode = existingBill != null

  var description by rememberSaveable(existingBill) { mutableStateOf(existingBill?.description ?: "") }
  var totalAmountText by rememberSaveable(existingBill) { 
    mutableStateOf(existingBill?.let { String.format(Locale.US, "%,d", it.totalAmount.toLong()) } ?: "") 
  }
  var selectedPayerId by rememberSaveable(existingBill, currentUserId, members) { 
    mutableStateOf(existingBill?.paidByUserId ?: currentUserId ?: members.firstOrNull()?.userId ?: "") 
  }
  // Map: userId -> amount text they owe
  val splitAmounts = remember { mutableStateMapOf<String, String>() }
  // Map: userId -> percentage
  val splitPercentages = remember { mutableStateMapOf<String, String>() }
  // Map: userId -> shares
  val splitShares = remember { mutableStateMapOf<String, String>() }

  var splitMode by rememberSaveable { mutableStateOf(SplitMode.EQUAL) }
  var payerDropdownExpanded by remember { mutableStateOf(false) }
  var showSuccessOverlay by remember { mutableStateOf(false) }

  var selectedCurrency by rememberSaveable(existingBill) { mutableStateOf(existingBill?.currency ?: "VND") }
  var exchangeRateText by rememberSaveable(existingBill) { 
    mutableStateOf(existingBill?.exchangeRate?.toString() ?: "1.0") 
  }

  val exchangeRateRepository = remember { com.example.splitbill.data.ExchangeRateRepository() }
  LaunchedEffect(selectedCurrency) {
    if (selectedCurrency == "VND") {
      exchangeRateText = "1.0"
    } else {
      val result = exchangeRateRepository.getExchangeRateToVnd(selectedCurrency)
      if (result.isSuccess) {
        exchangeRateText = result.getOrNull().toString()
      }
    }
  }

  // Pre-populate splits if in edit mode
  LaunchedEffect(existingBill, members) {
    if (existingBill != null && members.isNotEmpty()) {
      splitAmounts.clear()
      existingBill.splits.forEach { split ->
        splitAmounts[split.userId] = String.format(Locale.US, "%,d", split.amountOwed.toLong())
      }
      val total = existingBill.totalAmount
      val perPerson = Math.floor(total / members.size)
      val isEachEqual = existingBill.splits.all { Math.abs(it.amountOwed - perPerson) <= 2.0 || Math.abs(it.amountOwed - (perPerson + (total - perPerson * members.size))) <= 2.0 }
      splitMode = if (isEachEqual) SplitMode.EQUAL else SplitMode.CUSTOM
    }
  }

  // Auto-fill equal splits when amount changes
  LaunchedEffect(totalAmountText, splitMode) {
    if (splitMode == SplitMode.EQUAL && members.isNotEmpty()) {
      val total = totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
      val perPerson = Math.floor(total / members.size)
      val remainder = total - (perPerson * members.size)
      
      members.forEachIndexed { index, member ->
        val finalAmount = if (index == 0) perPerson + remainder else perPerson
        splitAmounts[member.userId] = if (finalAmount > 0) {
          if (selectedCurrency == "VND") String.format(Locale.US, "%,d", finalAmount.toLong()) else String.format(Locale.US, "%.2f", finalAmount)
        } else ""
      }
    }
  }

  // Auto-fill splits based on percentage or shares
  LaunchedEffect(totalAmountText, splitMode, splitPercentages.entries.toList(), splitShares.entries.toList()) {
    val total = totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
    if (splitMode == SplitMode.PERCENTAGE && members.isNotEmpty()) {
      members.forEach { member ->
        val pct = splitPercentages[member.userId]?.toDoubleOrNull() ?: 0.0
        val amount = Math.round(total * pct / 100.0 * 100.0) / 100.0
        splitAmounts[member.userId] = if (amount > 0) {
          if (selectedCurrency == "VND") String.format(Locale.US, "%,d", amount.toLong()) else String.format(Locale.US, "%.2f", amount)
        } else ""
      }
    } else if (splitMode == SplitMode.SHARES && members.isNotEmpty()) {
      val totalShares = members.sumOf { splitShares[it.userId]?.toIntOrNull() ?: 1 }
      if (totalShares > 0) {
        members.forEach { member ->
          val shares = splitShares[member.userId]?.toIntOrNull() ?: 1
          val amount = Math.round(total * shares.toDouble() / totalShares.toDouble() * 100.0) / 100.0
          splitAmounts[member.userId] = if (amount > 0) {
            if (selectedCurrency == "VND") String.format(Locale.US, "%,d", amount.toLong()) else String.format(Locale.US, "%.2f", amount)
          } else ""
        }
      }
    }
  }

  // Initialize percentage or shares defaults
  LaunchedEffect(splitMode, members) {
    if (members.isNotEmpty()) {
      if (splitMode == SplitMode.PERCENTAGE && splitPercentages.isEmpty()) {
        val basePct = 100 / members.size
        val remainder = 100 - (basePct * members.size)
        members.forEachIndexed { index, member ->
          val pct = if (index == 0) basePct + remainder else basePct
          splitPercentages[member.userId] = pct.toString()
        }
      } else if (splitMode == SplitMode.SHARES && splitShares.isEmpty()) {
        members.forEach { member ->
          splitShares[member.userId] = "1"
        }
      }
    }
  }

  val sumPercentages = members.sumOf { splitPercentages[it.userId]?.toDoubleOrNull() ?: 0.0 }
  val percentageWarning = splitMode == SplitMode.PERCENTAGE && kotlin.math.abs(sumPercentages - 100.0) > 0.01

  val settingsManager = remember { com.example.splitbill.data.SettingsManager(context) }
  val pushEnabled by settingsManager.pushEnabled.collectAsState(initial = true)
  val pushGroupName = "Chia hóa đơn".localized()

  // Navigate back on success after a short animation delay
  LaunchedEffect(uiState) {
    if (uiState is AddBillUiState.Success) {
      showSuccessOverlay = true
      
      if (pushEnabled) {
        com.example.splitbill.utils.NotificationHelper.showBillNotification(
          context = context,
          groupName = pushGroupName,
          billDescription = if (isEditMode) "Cập nhật: $description" else description,
          amount = totalAmountText + selectedCurrency
        )
      }
      
      kotlinx.coroutines.delay(1200) // Show success animation briefly
      onNavigateBack()
      viewModel.resetState()
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      topBar = {
        SplitBillTopBar(
          title = if (isEditMode) "Sửa hóa đơn" else "Thêm hóa đơn",
          canNavigateBack = true,
          onNavigateBack = onNavigateBack
        )
      },
      modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
      LazyColumn(
        modifier = Modifier.padding(paddingValues).fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpacingM),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
      ) {

        // --- Error Banner ---
        item {
          AnimatedVisibility(
            visible = uiState is AddBillUiState.Error,
            enter = expandVertically(animationSpec = Motion.springGentle()) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
          ) {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(Modifier.padding(Dimens.SpacingM), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(Dimens.SpacingS))
                Text(
                  (uiState as? AddBillUiState.Error)?.message ?: "",
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }
          }
        }

        // --- Total Amount (Premium Header) ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            Card(
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.SpacingS)
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpacingL, horizontal = Dimens.SpacingM)
              ) {
                // Currency Chips Selector Row
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(bottom = Dimens.SpacingS)
                ) {
                  listOf("VND", "USD", "EUR", "SGD").forEach { curr ->
                    val isSelected = selectedCurrency == curr
                    FilterChip(
                      selected = isSelected,
                      onClick = { selectedCurrency = curr; totalAmountText = "" },
                      label = { Text(curr) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                      )
                    )
                  }
                }

                Text(
                  "Tổng số tiền",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(Dimens.SpacingXS))
                OutlinedTextField(
                  value = totalAmountText,
                  onValueChange = { input -> 
                    if (selectedCurrency == "VND") {
                      val clean = input.filter { it.isDigit() }
                      if (clean.isNotEmpty()) {
                        try {
                          totalAmountText = String.format(Locale.US, "%,d", clean.toLong())
                        } catch (e: Exception) { }
                      } else {
                        totalAmountText = ""
                      }
                    } else {
                      if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                        if (input.count { it == '.' } <= 1) {
                          totalAmountText = input
                        }
                      }
                    }
                  },
                  textStyle = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                  ),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                  ),
                  placeholder = {
                    Text(
                      "0",
                      style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                      modifier = Modifier.fillMaxWidth(),
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                    )
                  },
                  modifier = Modifier.fillMaxWidth(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  suffix = { Text(selectedCurrency, style = MaterialTheme.typography.headlineMedium) }
                )

                if (selectedCurrency != "VND") {
                  Spacer(Modifier.height(Dimens.SpacingS))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.SpacingM)
                  ) {
                    Text("Tỷ giá quy đổi (1 $selectedCurrency = ... VND)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    OutlinedTextField(
                      value = exchangeRateText,
                      onValueChange = { exchangeRateText = it },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      singleLine = true,
                      modifier = Modifier.width(100.dp).height(48.dp),
                      shape = RoundedCornerShape(8.dp),
                      textStyle = MaterialTheme.typography.bodySmall
                    )
                  }
                  
                  val total = totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
                  val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                  val vndEquivalent = total * rate
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = "Quy đổi: ≈ ${String.format(Locale.US, "%,.0f", vndEquivalent)}đ",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }

        // --- Description ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            TextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Tên hóa đơn (Ăn tối, Taxi...)") },
              modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
              leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
              shape = RoundedCornerShape(16.dp),
              colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
              singleLine = true
            )
          }
        }

        // --- Payer Dropdown ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(2 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            ExposedDropdownMenuBox(
              expanded = payerDropdownExpanded,
              onExpandedChange = { payerDropdownExpanded = !payerDropdownExpanded }
            ) {
              TextField(
                value = members.find { it.userId == selectedPayerId }?.username ?: "Chọn người trả",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).shadow(4.dp, RoundedCornerShape(16.dp)),
                label = { Text("Ai là người trả tiền?") },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent,
                  focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(payerDropdownExpanded) },
                leadingIcon = { Icon(Icons.Rounded.Wallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
              )
              ExposedDropdownMenu(
                expanded = payerDropdownExpanded,
                onDismissRequest = { payerDropdownExpanded = false }
              ) {
                members.forEach { member ->
                  DropdownMenuItem(
                    text = { Text(member.username, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                      selectedPayerId = member.userId
                      payerDropdownExpanded = false
                    },
                    leadingIcon = {
                      Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                  )
                }
              }
            }
          }
        }

        // --- Split Mode Toggle ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(3 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            Column {
              Spacer(Modifier.height(Dimens.SpacingS))
              Text("Phương thức chia", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Spacer(Modifier.height(Dimens.SpacingS))
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                  selected = splitMode == SplitMode.EQUAL,
                  onClick = { splitMode = SplitMode.EQUAL },
                  shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                  label = { Text("Đều", fontWeight = FontWeight.Bold) }
                )
                SegmentedButton(
                  selected = splitMode == SplitMode.CUSTOM,
                  onClick = { splitMode = SplitMode.CUSTOM },
                  shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                  label = { Text("Số tiền", fontWeight = FontWeight.Bold) }
                )
                SegmentedButton(
                  selected = splitMode == SplitMode.PERCENTAGE,
                  onClick = { splitMode = SplitMode.PERCENTAGE },
                  shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                  label = { Text("%", fontWeight = FontWeight.Bold) }
                )
                SegmentedButton(
                  selected = splitMode == SplitMode.SHARES,
                  onClick = { splitMode = SplitMode.SHARES },
                  shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                  label = { Text("Phần", fontWeight = FontWeight.Bold) }
                )
              }
              if (percentageWarning) {
                Text(
                  text = "Tổng phần trăm hiện tại là ${String.format(Locale.US, "%.1f", sumPercentages)}% (phải bằng 100%)",
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(top = Dimens.SpacingS)
                )
              }
            }
          }
        }

        // --- Split Inputs per member ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(4 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = fadeIn()) {
            Spacer(Modifier.height(Dimens.SpacingXS))
          }
        }
        
        itemsIndexed(members) { index, member ->
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay((5 + index) * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(index)) {
            SplitBillCard(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              containerColor = if (splitAmounts[member.userId]?.isNotEmpty() == true) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
              ) {
                com.example.splitbill.ui.components.GradientAvatar(name = member.username)
                Text(
                  member.username,
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                  modifier = Modifier.weight(1f)
                )
                when (splitMode) {
                  SplitMode.EQUAL -> {
                    Text(
                      text = (splitAmounts[member.userId] ?: "0") + selectedCurrency,
                      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                  }
                  SplitMode.CUSTOM -> {
                    OutlinedTextField(
                      value = splitAmounts[member.userId] ?: "",
                      onValueChange = { input -> 
                        if (selectedCurrency == "VND") {
                          val clean = input.filter { it.isDigit() }
                          if (clean.isNotEmpty()) {
                            try {
                              splitAmounts[member.userId] = String.format(Locale.US, "%,d", clean.toLong())
                            } catch (e: Exception) { }
                          } else {
                            splitAmounts[member.userId] = ""
                          }
                        } else {
                          if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                            if (input.count { it == '.' } <= 1) {
                              splitAmounts[member.userId] = input
                            }
                          }
                        }
                      },
                      modifier = Modifier.width(120.dp),
                      textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      shape = RoundedCornerShape(12.dp),
                      suffix = { Text(selectedCurrency) }
                    )
                  }
                  SplitMode.PERCENTAGE -> {
                    Column(horizontalAlignment = Alignment.End) {
                      OutlinedTextField(
                        value = splitPercentages[member.userId] ?: "",
                        onValueChange = { input -> 
                          val clean = input.filter { it.isDigit() || it == '.' }
                          if (clean.count { it == '.' } <= 1) {
                            splitPercentages[member.userId] = clean
                          }
                        },
                        modifier = Modifier.width(120.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("%") }
                      )
                      val amt = splitAmounts[member.userId] ?: ""
                      if (amt.isNotEmpty()) {
                        Text(
                          text = "${amt}${selectedCurrency}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                  SplitMode.SHARES -> {
                    Column(horizontalAlignment = Alignment.End) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        IconButton(
                          onClick = {
                            val current = splitShares[member.userId]?.toIntOrNull() ?: 1
                            if (current > 1) {
                              splitShares[member.userId] = (current - 1).toString()
                            }
                          },
                          modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                          Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                        }
                        Text(
                          text = splitShares[member.userId] ?: "1",
                          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                          modifier = Modifier.width(20.dp),
                          textAlign = TextAlign.Center
                        )
                        IconButton(
                          onClick = {
                            val current = splitShares[member.userId]?.toIntOrNull() ?: 1
                            splitShares[member.userId] = (current + 1).toString()
                          },
                          modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                          Icon(Icons.Default.Add, contentDescription = "Tăng", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                        }
                      }
                      val amt = splitAmounts[member.userId] ?: ""
                      if (amt.isNotEmpty()) {
                        Text(
                          text = "${amt}${selectedCurrency}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // --- Submit Button ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(7 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = slideInVertically(animationSpec = Motion.tweenSlow(), initialOffsetY = { it / 2 }) + fadeIn()) {
            Column {
              Spacer(Modifier.height(Dimens.SpacingM))
              com.example.splitbill.ui.components.GradientButton(
                onClick = {
                  val total = totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
                  val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                  val splits = members.mapNotNull { member ->
                    val amount = splitAmounts[member.userId]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    if (amount > 0) BillSplitItem(member.userId, amount) else null
                  }
                  if (isEditMode) {
                    viewModel.updateBill(existingBill!!.id, description, total, selectedPayerId, selectedCurrency, rate, splits)
                  } else {
                    viewModel.createBill(groupId, description, total, selectedPayerId, selectedCurrency, rate, splits)
                  }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState !is AddBillUiState.Loading && totalAmountText.isNotBlank() && description.isNotBlank() && !percentageWarning
              ) {
                if (uiState is AddBillUiState.Loading) {
                  CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                } else {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                  Spacer(Modifier.width(Dimens.SpacingS))
                  Text(if (isEditMode) "Cập nhật hóa đơn" else "Lưu hóa đơn", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
              }
              Spacer(Modifier.height(Dimens.SpacingXL))
            }
          }
        }
      }
    }

    // Success Overlay Animation
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
        // Confetti!
        ConfettiOverlay()

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

enum class SplitMode { EQUAL, CUSTOM, PERCENTAGE, SHARES }

