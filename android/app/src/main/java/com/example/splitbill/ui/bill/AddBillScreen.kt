package com.example.splitbill.ui.bill

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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

  var selectedCategory by rememberSaveable(existingBill) { mutableStateOf(existingBill?.category ?: "GENERAL") }
  var showCategoryPicker by remember { mutableStateOf(false) }

  var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
  var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

  val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      imageBytes = compressReceiptImage(context, uri)
    }
  }

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

  // Map: userId -> is participating in the bill
  val activeMemberIds = remember { mutableStateMapOf<String, Boolean>() }

  // Pre-populate splits and active members if in edit mode
  LaunchedEffect(existingBill, members) {
    if (existingBill != null && members.isNotEmpty()) {
      splitAmounts.clear()
      val participantIds = existingBill.splits.filter { it.amountOwed > 0 }.map { it.userId }.toSet()
      members.forEach { m ->
        activeMemberIds[m.userId] = participantIds.contains(m.userId)
      }
      existingBill.splits.forEach { split ->
        splitAmounts[split.userId] = String.format(Locale.US, "%,d", split.amountOwed.toLong())
      }
      val total = existingBill.totalAmount
      val activeCount = if (participantIds.isNotEmpty()) participantIds.size else members.size
      val perPerson = Math.floor(total / activeCount)
      val isEachEqual = existingBill.splits.filter { it.amountOwed > 0 }.all { 
        Math.abs(it.amountOwed - perPerson) <= 2.0 || Math.abs(it.amountOwed - (perPerson + (total - perPerson * activeCount))) <= 2.0 
      }
      splitMode = if (isEachEqual) SplitMode.EQUAL else SplitMode.CUSTOM
    } else {
      members.forEach { m ->
        if (!activeMemberIds.containsKey(m.userId)) {
          activeMemberIds[m.userId] = true
        }
      }
    }
  }

  // Auto-fill equal splits when amount, active members, currency, exchange rate, or payer changes
  LaunchedEffect(totalAmountText, splitMode, selectedCurrency, exchangeRateText, selectedPayerId, activeMemberIds.toMap()) {
    if (splitMode == SplitMode.EQUAL && members.isNotEmpty()) {
      val total = evaluateMathExpression(totalAmountText) ?: totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
      val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
      val activeMembers = members.filter { activeMemberIds[it.userId] != false }
      
      if (activeMembers.isEmpty()) {
        members.forEach { splitAmounts[it.userId] = "" }
        return@LaunchedEffect
      }

      if (selectedCurrency == "VND" || rate <= 1.0) {
        val perPerson = Math.floor(total / activeMembers.size)
        val remainder = total - (perPerson * activeMembers.size)
        
        members.forEach { member ->
          if (activeMemberIds[member.userId] == false) {
            splitAmounts[member.userId] = ""
          } else {
            val isPayer = member.userId == selectedPayerId || (selectedPayerId.isBlank() && member == activeMembers.first())
            val finalAmount = if (isPayer) perPerson + remainder else perPerson
            splitAmounts[member.userId] = if (finalAmount > 0) String.format(Locale.US, "%,d", finalAmount.toLong()) else ""
          }
        }
      } else {
        val totalVnd = total * rate
        val perPersonVnd = Math.floor(totalVnd / activeMembers.size)
        val remainderVnd = totalVnd - (perPersonVnd * activeMembers.size)
        
        members.forEach { member ->
          if (activeMemberIds[member.userId] == false) {
            splitAmounts[member.userId] = ""
          } else {
            val isPayer = member.userId == selectedPayerId || (selectedPayerId.isBlank() && member == activeMembers.first())
            val finalVnd = if (isPayer) perPersonVnd + remainderVnd else perPersonVnd
            val foreignAmount = finalVnd / rate
            splitAmounts[member.userId] = if (foreignAmount > 0) String.format(Locale.US, "%.2f", foreignAmount) else ""
          }
        }
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

  val view = androidx.compose.ui.platform.LocalView.current
  val settingsManager = remember { com.example.splitbill.data.SettingsManager(context) }
  val pushEnabled by settingsManager.pushEnabled.collectAsState(initial = true)
  val hapticEnabled by settingsManager.hapticEnabled.collectAsState(initial = true)
  val soundEnabled by settingsManager.soundEnabled.collectAsState(initial = true)
  val pushGroupName = "Chia hóa đơn".localized()

  // Navigate back on success after a short animation delay
  LaunchedEffect(uiState) {
    if (uiState is AddBillUiState.Success) {
      showSuccessOverlay = true
      
      if (hapticEnabled) {
        com.example.splitbill.utils.HapticManager.triggerSuccess(view)
      }
      if (soundEnabled) {
        com.example.splitbill.utils.SoundManager.playSuccessSound()
      }

      if (pushEnabled) {
        val createdBillId = (uiState as? com.example.splitbill.ui.bill.AddBillUiState.Success)?.billId
        com.example.splitbill.utils.NotificationHelper.showBillNotification(
          context = context,
          groupId = groupId,
          billId = createdBillId,
          groupName = pushGroupName,
          billDescription = if (isEditMode) "Cập nhật: $description" else description,
          amount = totalAmountText + " " + selectedCurrency
        )
      }
      
      kotlinx.coroutines.delay(400) // Fast navigation back
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
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
                // Auto-scaling amount input
                val hasMathInput = totalAmountText.any { it == '+' || it == '*' || it == '/' || it.equals('k', ignoreCase = true) }
                val liveAmtFormatted = remember(totalAmountText) {
                  val raw = totalAmountText.replace(",", "").toLongOrNull()
                  if (raw != null && !hasMathInput) String.format(java.util.Locale.US, "%,d", raw) else null
                }
                val amountFontSize = when {
                  totalAmountText.length > 14 -> 28.sp
                  totalAmountText.length > 10 -> 36.sp
                  else -> 52.sp
                }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { },
                  contentAlignment = Alignment.Center
                ) {
                  if (totalAmountText.isEmpty()) {
                    Text(
                      "0",
                      fontSize = amountFontSize,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                    )
                  }
                  androidx.compose.foundation.text.BasicTextField(
                    value = totalAmountText,
                    onValueChange = { input ->
                      val validChars = input.filter { it.isDigit() || it == '+' || it == '-' || it == '*' || it == '/' || it == '.' || it == ',' || it == 'k' || it == 'K' || it == ' ' }
                      totalAmountText = validChars
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                      fontSize = amountFontSize,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    visualTransformation = remember { ThousandSeparatorVisualTransformation('.') },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        innerTextField()
                      }
                    }
                  )
                }

                // Live calculation preview badge
                val liveEvaluated = remember(totalAmountText) { evaluateMathExpression(totalAmountText) }
                val hasMath = totalAmountText.any { it == '+' || it == '-' || it == '*' || it == '/' || it.equals('k', ignoreCase = true) }
                if (hasMath && liveEvaluated != null) {
                  Spacer(Modifier.height(Dimens.SpacingXS))
                  Row(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.primaryContainer)
                      .clickable {
                        totalAmountText = if (selectedCurrency == "VND") String.format(Locale.US, "%,d", liveEvaluated.toLong()) else String.format(Locale.US, "%.2f", liveEvaluated)
                      }
                      .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      Icons.Rounded.Calculate,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                    Text(
                      text = "= ${String.format(Locale.US, if (selectedCurrency == "VND") "%,d" else "%.2f", if (selectedCurrency == "VND") liveEvaluated.toLong() else liveEvaluated)} $selectedCurrency",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                      text = "(Chạm để áp dụng)",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                  }
                }

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

        // --- Category Picker ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay((1.5 * Motion.StaggerDelay).toLong()); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            SplitBillCard(modifier = Modifier.fillMaxWidth()) {
              com.example.splitbill.ui.components.CategoryQuickPicker(
                selectedCategory = com.example.splitbill.data.model.BillCategory.fromKey(selectedCategory),
                onCategorySelected = { selectedCategory = it.key },
                onShowAllCategories = { showCategoryPicker = true }
              )
            }
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
                leadingIcon = {
                  val selectedPayer = members.find { it.userId == selectedPayerId }
                  if (selectedPayer != null && !selectedPayer.avatarUrl.isNullOrBlank()) {
                    val avatarFullUrl = remember(selectedPayer.avatarUrl) {
                      if (selectedPayer.avatarUrl.startsWith("http")) selectedPayer.avatarUrl else "${com.example.splitbill.data.supabase.SupabaseConfig.SUPABASE_URL}${if (selectedPayer.avatarUrl.startsWith("/")) "" else "/"}${selectedPayer.avatarUrl}"
                    }
                    coil3.compose.AsyncImage(
                      model = avatarFullUrl,
                      contentDescription = selectedPayer.username,
                      modifier = Modifier.size(24.dp).clip(CircleShape),
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                  } else {
                    Icon(Icons.Rounded.Wallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  }
                }
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
                      if (!member.avatarUrl.isNullOrBlank()) {
                        val avatarFullUrl = remember(member.avatarUrl) {
                          if (member.avatarUrl.startsWith("http")) member.avatarUrl else "${com.example.splitbill.data.supabase.SupabaseConfig.SUPABASE_URL}${if (member.avatarUrl.startsWith("/")) "" else "/"}${member.avatarUrl}"
                        }
                        coil3.compose.AsyncImage(
                          model = avatarFullUrl,
                          contentDescription = member.username,
                          modifier = Modifier.size(24.dp).clip(CircleShape),
                          contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                      } else {
                        com.example.splitbill.ui.components.GradientAvatar(name = member.username, size = 24.dp)
                      }
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
                  label = { Text("Tùy chỉnh", fontWeight = FontWeight.Bold) }
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
              if (splitMode == SplitMode.PERCENTAGE) {
                val sumPercentages = members.sumOf { splitPercentages[it.userId]?.toDoubleOrNull() ?: 0.0 }
                if (java.lang.Math.abs(sumPercentages - 100.0) > 0.1) {
                  Text(
                    text = "Tổng phần trăm hiện tại là ${String.format(java.util.Locale.US, "%.1f", sumPercentages)}% (phải bằng 100%)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = Dimens.SpacingS)
                  )
                }
              }
            }
          }
        }

        // --- Quick Avatar Chips Participant Selector ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay((3.5 * Motion.StaggerDelay).toLong()); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpacingM)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "Người tham gia (${activeMemberIds.values.count { it }}/${members.size})",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                  onClick = {
                    val allActive = members.all { activeMemberIds[it.userId] != false }
                    members.forEach { m -> activeMemberIds[m.userId] = !allActive }
                  }
                ) {
                  val allActive = members.all { activeMemberIds[it.userId] != false }
                  Text(if (allActive) "Bỏ chọn tất cả" else "Chọn tất cả", style = MaterialTheme.typography.labelMedium)
                }
              }
              Spacer(Modifier.height(Dimens.SpacingXS))
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingM),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(members, key = { it.userId }) { member ->
                  val isSelected = activeMemberIds[member.userId] != false
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable {
                        activeMemberIds[member.userId] = !isSelected
                      }
                      .padding(horizontal = 4.dp, vertical = 6.dp)
                  ) {
                    Box(
                      modifier = Modifier.size(52.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      if (!member.avatarUrl.isNullOrBlank()) {
                        val avatarFullUrl = remember(member.avatarUrl) {
                          if (member.avatarUrl.startsWith("http")) member.avatarUrl else "${com.example.splitbill.data.supabase.SupabaseConfig.SUPABASE_URL}${if (member.avatarUrl.startsWith("/")) "" else "/"}${member.avatarUrl}"
                        }
                        coil3.compose.AsyncImage(
                          model = avatarFullUrl,
                          contentDescription = member.username,
                          modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .graphicsLayer(alpha = if (isSelected) 1f else 0.35f),
                          contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                      } else {
                        Box(modifier = Modifier.graphicsLayer(alpha = if (isSelected) 1f else 0.35f)) {
                          com.example.splitbill.ui.components.GradientAvatar(name = member.username, size = 48.dp)
                        }
                      }

                      if (isSelected) {
                        Box(
                          modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                          )
                        }
                      }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                      text = member.username,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                      ),
                      color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                      modifier = Modifier.widthIn(max = 68.dp),
                      textAlign = TextAlign.Center
                    )
                  }
                }
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
          val isParticipating = activeMemberIds[member.userId] != false
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay((5 + index) * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(index)) {
            SplitBillCard(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              containerColor = if (isParticipating && splitAmounts[member.userId]?.isNotEmpty() == true) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
              ) {
                if (!member.avatarUrl.isNullOrBlank()) {
                  val avatarFullUrl = remember(member.avatarUrl) {
                    if (member.avatarUrl.startsWith("http")) member.avatarUrl else "${com.example.splitbill.data.supabase.SupabaseConfig.SUPABASE_URL}${if (member.avatarUrl.startsWith("/")) "" else "/"}${member.avatarUrl}"
                  }
                  coil3.compose.AsyncImage(
                    model = avatarFullUrl,
                    contentDescription = member.username,
                    modifier = Modifier.size(40.dp).clip(CircleShape).graphicsLayer(alpha = if (isParticipating) 1f else 0.4f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                  )
                } else {
                  Box(modifier = Modifier.graphicsLayer(alpha = if (isParticipating) 1f else 0.4f)) {
                    com.example.splitbill.ui.components.GradientAvatar(name = member.username)
                  }
                }
                Text(
                  member.username,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isParticipating) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  ),
                  modifier = Modifier.weight(1f)
                )
                if (!isParticipating) {
                  Text(
                    text = "Không tham gia",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                  )
                } else {
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
                            Icon(Icons.Rounded.Remove, contentDescription = "Giảm", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
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
                            Icon(Icons.Rounded.Add, contentDescription = "Tăng", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
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
        }

        // --- Receipt Picker Card ---
        item {
          var visible by rememberSaveable { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(6 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(6)) {
            SplitBillCard(modifier = Modifier.fillMaxWidth()) {
              Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)) {
                Text(
                  text = "Ảnh hóa đơn".localized(),
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium
                )
                
                if (imageBytes != null) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(200.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                  ) {
                    coil3.compose.AsyncImage(
                      model = selectedImageUri ?: imageBytes,
                      contentDescription = "Receipt Preview",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    
                    IconButton(
                      onClick = {
                        imageBytes = null
                        selectedImageUri = null
                      },
                      modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                      Icon(Icons.Rounded.Close, contentDescription = "Xóa ảnh", tint = Color.White)
                    }
                  }
                } else if (existingBill?.receiptUrl != null) {
                  val fullUrl = if (existingBill.receiptUrl.startsWith("http")) existingBill.receiptUrl else com.example.splitbill.data.supabase.SupabaseConfig.SUPABASE_URL + existingBill.receiptUrl
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(200.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                  ) {
                    coil3.compose.AsyncImage(
                      model = fullUrl,
                      contentDescription = "Receipt Image",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                  }
                } else {
                  OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center
                    ) {
                      Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(32.dp))
                      Spacer(Modifier.height(Dimens.SpacingXS))
                      Text("Chọn ảnh hóa đơn từ Thư viện".localized())
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
                  val total = evaluateMathExpression(totalAmountText) ?: totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
                  val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                  val splits = members.mapNotNull { member ->
                    val amount = splitAmounts[member.userId]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    if (amount > 0) BillSplitItem(member.userId, amount) else null
                  }
                  if (isEditMode) {
                    viewModel.updateBill(existingBill!!.id, description, total, selectedPayerId, selectedCurrency, rate, splits, imageBytes, selectedCategory)
                  } else {
                    viewModel.createBill(groupId, description, total, selectedPayerId, selectedCurrency, rate, splits, imageBytes, selectedCategory)
                  }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState !is AddBillUiState.Loading && totalAmountText.isNotBlank() && description.isNotBlank() && !percentageWarning
              ) {
                if (uiState is AddBillUiState.Loading) {
                  CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                } else {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
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
              Icons.Rounded.CheckCircle,
              contentDescription = "Success",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(80.dp)
            )
          }
        }
      }
    }

    // Category Picker Bottom Sheet
    if (showCategoryPicker) {
      com.example.splitbill.ui.components.CategoryPickerBottomSheet(
        selectedCategory = com.example.splitbill.data.model.BillCategory.fromKey(selectedCategory),
        onCategorySelected = { selectedCategory = it.key },
        onDismiss = { showCategoryPicker = false }
      )
    }
  }
}

enum class SplitMode { EQUAL, CUSTOM, PERCENTAGE, SHARES }

fun evaluateMathExpression(input: String): Double? {
  try {
    val clean = input.replace(",", "")
      .replace(Regex("(?i)(\\d+(?:\\.\\d+)?)k"), "$1*1000")
      .replace(" ", "")
    if (clean.isBlank()) return null
    if (!clean.any { it == '+' || it == '-' || it == '*' || it == '/' }) {
      return clean.toDoubleOrNull()
    }
    val tokens = mutableListOf<String>()
    var num = StringBuilder()
    for (ch in clean) {
      if (ch.isDigit() || ch == '.') {
        num.append(ch)
      } else if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
        if (num.isNotEmpty()) {
          tokens.add(num.toString())
          num = StringBuilder()
        }
        tokens.add(ch.toString())
      }
    }
    if (num.isNotEmpty()) tokens.add(num.toString())
    if (tokens.isEmpty()) return null
    val intermediate = mutableListOf<String>()
    var i = 0
    while (i < tokens.size) {
      val t = tokens[i]
      if (t == "*" || t == "/") {
        if (intermediate.isEmpty() || i + 1 >= tokens.size) return null
        val prev = intermediate.removeAt(intermediate.size - 1).toDoubleOrNull() ?: return null
        val next = tokens[i + 1].toDoubleOrNull() ?: return null
        val res = if (t == "*") prev * next else if (next != 0.0) prev / next else return null
        intermediate.add(res.toString())
        i += 2
      } else {
        intermediate.add(t)
        i++
      }
    }
    if (intermediate.isEmpty()) return null
    var total = intermediate[0].toDoubleOrNull() ?: return null
    var j = 1
    while (j < intermediate.size) {
      val op = intermediate[j]
      if (j + 1 >= intermediate.size) break
      val next = intermediate[j + 1].toDoubleOrNull() ?: return null
      if (op == "+") total += next
      else if (op == "-") total -= next
      j += 2
    }
    return if (total >= 0) total else null
  } catch (e: Exception) {
    return null
  }
}

private fun compressReceiptImage(context: android.content.Context, uri: android.net.Uri): ByteArray? {
  return try {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null
    
    val maxDim = 800
    val width = originalBitmap.width
    val height = originalBitmap.height
    val scale = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height).coerceAtMost(1.0f)
    
    val scaledWidth = (width * scale).toInt()
    val scaledHeight = (height * scale).toInt()
    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    
    val outputStream = java.io.ByteArrayOutputStream()
    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
    outputStream.toByteArray()
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

class ThousandSeparatorVisualTransformation(val separator: Char = '.') : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val original = text.text
        if (original.isEmpty() || original.any { !it.isDigit() }) {
            return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        }
        val formatted = java.lang.StringBuilder()
        val length = original.length
        for (i in 0 until length) {
            formatted.append(original[i])
            val remaining = length - i - 1
            if (remaining > 0 && remaining % 3 == 0) {
                formatted.append(separator)
            }
        }
        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= length) return formatted.length
                val commasBefore = (length - 1) / 3 - (length - offset - 1) / 3
                return offset + commasBefore
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formatted.length) return length
                var originalOffset = 0
                var transformedOffset = 0
                while (transformedOffset < offset && originalOffset < length) {
                    transformedOffset++
                    val remaining = length - originalOffset - 1
                    if (remaining > 0 && remaining % 3 == 0) {
                        transformedOffset++
                    }
                    originalOffset++
                }
                return originalOffset
            }
        }
        return androidx.compose.ui.text.input.TransformedText(
            androidx.compose.ui.text.AnnotatedString(formatted.toString()),
            offsetMapping
        )
    }
}
