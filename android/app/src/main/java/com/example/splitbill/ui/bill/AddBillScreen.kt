package com.example.splitbill.ui.bill

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.splitbill.data.ExchangeRateRepository
import com.example.splitbill.data.SettingsManager
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.BillSplitItem
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.model.BillCategory
import com.example.splitbill.data.supabase.SupabaseConfig
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.components.CategoryPickerBottomSheet
import com.example.splitbill.ui.components.ConfettiOverlay
import com.example.splitbill.ui.components.GradientAvatar
import com.example.splitbill.ui.components.SplitBillTopBar
import com.example.splitbill.ui.localization.localized
import com.example.splitbill.utils.HapticManager
import com.example.splitbill.utils.NotificationHelper
import com.example.splitbill.utils.SoundManager
import java.util.Locale
import kotlin.math.abs

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

  // Splits tracking
  val splitAmounts = remember { mutableStateMapOf<String, String>() }
  val splitPercentages = remember { mutableStateMapOf<String, String>() }
  val splitShares = remember { mutableStateMapOf<String, String>() }

  var splitMode by rememberSaveable { mutableStateOf(SplitMode.EQUAL) }
  var showPayerSheet by remember { mutableStateOf(false) }
  var showSuccessOverlay by remember { mutableStateOf(false) }

  var selectedCategory by rememberSaveable(existingBill) { mutableStateOf(existingBill?.category ?: "FOOD") }
  var showCategoryPicker by remember { mutableStateOf(false) }

  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      imageBytes = compressReceiptImage(context, uri)
    }
  }

  // Currency & Exchange Rate State
  var selectedCurrency by rememberSaveable(existingBill) { mutableStateOf(existingBill?.currency ?: "VND") }
  var exchangeRateText by rememberSaveable(existingBill) {
    mutableStateOf(existingBill?.exchangeRate?.toString() ?: "1.0")
  }
  var showCurrencySheet by remember { mutableStateOf(false) }
  var showExchangeRateDialog by remember { mutableStateOf(false) }

  val exchangeRateRepository = remember { ExchangeRateRepository() }
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

  // Active participants map: userId -> is participating
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
        abs(it.amountOwed - perPerson) <= 2.0 || abs(it.amountOwed - (perPerson + (total - perPerson * activeCount))) <= 2.0
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

  // Auto-fill equal splits
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
  val percentageWarning = splitMode == SplitMode.PERCENTAGE && abs(sumPercentages - 100.0) > 0.1

  val parsedTotal = evaluateMathExpression(totalAmountText) ?: totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
  val customSum = members.sumOf { splitAmounts[it.userId]?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
  val customMismatch = splitMode == SplitMode.CUSTOM && parsedTotal > 0 && abs(customSum - parsedTotal) > 1.0

  val view = LocalView.current
  val settingsManager = remember { SettingsManager(context) }
  val pushEnabled by settingsManager.pushEnabled.collectAsState(initial = true)
  val hapticEnabled by settingsManager.hapticEnabled.collectAsState(initial = true)
  val soundEnabled by settingsManager.soundEnabled.collectAsState(initial = true)
  val pushGroupName = "Chia hóa đơn".localized()

  // Navigate back on success after a short animation delay
  LaunchedEffect(uiState) {
    if (uiState is AddBillUiState.Success) {
      showSuccessOverlay = true

      if (hapticEnabled) {
        HapticManager.triggerSuccess(view)
      }
      if (soundEnabled) {
        SoundManager.playSuccessSound()
      }

      if (pushEnabled) {
        val createdBillId = (uiState as? AddBillUiState.Success)?.billId
        NotificationHelper.showBillNotification(
          context = context,
          groupId = groupId,
          billId = createdBillId,
          groupName = pushGroupName,
          billDescription = if (isEditMode) "Cập nhật: $description" else description,
          amount = "$totalAmountText $selectedCurrency"
        )
      }

      kotlinx.coroutines.delay(400)
      onNavigateBack()
      viewModel.resetState()
    }
  }

  // Đảm bảo icon thanh trạng thái (giờ, pin, wifi) màu tối sắc nét trên nền sáng #F9F9F6
  val activity = context as? android.app.Activity
  DisposableEffect(Unit) {
    if (activity != null) {
      val insetsController = androidx.core.view.WindowCompat.getInsetsController(activity.window, view)
      val original = insetsController.isAppearanceLightStatusBars
      insetsController.isAppearanceLightStatusBars = true
      onDispose {
        insetsController.isAppearanceLightStatusBars = original
      }
    } else {
      onDispose { }
    }
  }

  // Palette constants matching the HTML design
  val screenBg = Color(0xFFF9F9F6)
  val cardBg = Color.White
  val borderColor = Color(0xFFE8ECEB)
  val primaryColor = Color(0xFF011C27)
  val secondaryTextColor = Color(0xFF546068)
  val surfaceContainerLow = Color(0xFFF4F4F1)
  val surfaceContainer = Color(0xFFEEEEEE)

  Box(modifier = modifier.fillMaxSize().background(screenBg)) {
    Scaffold(
      containerColor = screenBg,
      topBar = {
        Surface(
          color = screenBg,
          shadowElevation = 0.5.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Nút Back tròn đồng bộ với GroupDetailScreen
              Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E6E9)),
                shadowElevation = 1.dp,
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Quay lại",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              // Tiêu đề màu Deep Navy Slate đồng bộ thương hiệu
              Text(
                text = if (isEditMode) "Sửa hóa đơn" else "Thêm hóa đơn",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  letterSpacing = (-0.2).sp
                ),
                color = primaryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
              )

              // Giữ cân đối tiêu đề ở giữa
              Spacer(modifier = Modifier.size(38.dp))
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.7f), thickness = 0.8.dp)
          }
        }
      },
      bottomBar = {
        // Bottom bar: Only Save Button (no redundant "Tất cả thông tin hợp lệ" box)
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = screenBg.copy(alpha = 0.96f),
          shadowElevation = 8.dp
        ) {
          Box(
            modifier = Modifier
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            val isFormValid = uiState !is AddBillUiState.Loading &&
              totalAmountText.isNotBlank() &&
              parsedTotal > 0 &&
              description.isNotBlank() &&
              !percentageWarning &&
              !customMismatch

            Button(
              onClick = {
                val total = evaluateMathExpression(totalAmountText) ?: totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
                val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                val splits = members.mapNotNull { member ->
                  val amount = splitAmounts[member.userId]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                  if (amount > 0) BillSplitItem(member.userId, amount) else null
                }
                val usernameMap = members.associate { it.userId to it.username }
                if (isEditMode) {
                  viewModel.updateBill(
                    billId = existingBill!!.id,
                    description = description,
                    totalAmount = total,
                    paidByUserId = selectedPayerId,
                    currency = selectedCurrency,
                    exchangeRate = rate,
                    splits = splits,
                    receiptBytes = imageBytes,
                    category = selectedCategory,
                    knownUsernames = usernameMap
                  )
                } else {
                  viewModel.createBill(
                    groupId = groupId,
                    description = description,
                    totalAmount = total,
                    paidByUserId = selectedPayerId,
                    currency = selectedCurrency,
                    exchangeRate = rate,
                    splits = splits,
                    receiptBytes = imageBytes,
                    category = selectedCategory,
                    knownUsernames = usernameMap
                  )
                }
              },
              enabled = isFormValid,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
              shape = RoundedCornerShape(26.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.White,
                disabledContainerColor = primaryColor.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
              ),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
            ) {
              if (uiState is AddBillUiState.Loading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  strokeWidth = 2.5.dp,
                  color = Color.White
                )
              } else {
                Icon(
                  Icons.Rounded.ReceiptLong,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = if (isEditMode) "Cập nhật hóa đơn" else "Lưu hóa đơn",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
              }
            }
          }
        }
      },
      modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
      LazyColumn(
        modifier = Modifier
          .padding(paddingValues)
          .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp)
            ) {
              Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
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

        // ==========================================
        // 1. AMOUNT & MATH CARD
        // ==========================================
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp)
            ) {
              // Top label
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  Icons.Rounded.Payments,
                  contentDescription = null,
                  tint = secondaryTextColor,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "SỐ TIỀN",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  ),
                  color = secondaryTextColor
                )
              }

              Spacer(Modifier.height(8.dp))

              // Math Calculation preview badge (if typed math expression)
              val liveEvaluated = remember(totalAmountText) { evaluateMathExpression(totalAmountText) }
              val hasMath = totalAmountText.any { it == '+' || it == '-' || it == '*' || it == '/' || it.equals('k', ignoreCase = true) }

              if (hasMath && liveEvaluated != null) {
                Surface(
                  shape = CircleShape,
                  color = surfaceContainer,
                  modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                      totalAmountText = if (selectedCurrency == "VND") {
                        String.format(Locale.US, "%,d", liveEvaluated.toLong())
                      } else {
                        String.format(Locale.US, "%.2f", liveEvaluated)
                      }
                    }
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                  ) {
                    Icon(
                      Icons.Rounded.Calculate,
                      contentDescription = null,
                      tint = primaryColor,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = totalAmountText,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                      color = secondaryTextColor
                    )
                    Text(
                      text = "= " + if (selectedCurrency == "VND") String.format(Locale.US, "%,d", liveEvaluated.toLong()) else String.format(Locale.US, "%.2f", liveEvaluated),
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = primaryColor
                    )
                  }
                }
                Spacer(Modifier.height(8.dp))
              }

              // Large Amount Display & Input
              val amountFontSize = when {
                totalAmountText.length > 14 -> 28.sp
                totalAmountText.length > 10 -> 34.sp
                else -> 42.sp
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
              ) {
                BasicTextField(
                  value = totalAmountText,
                  onValueChange = { input ->
                    val validChars = input.filter {
                      it.isDigit() || it == '+' || it == '-' || it == '*' || it == '/' || it == '.' || it == ',' || it == 'k' || it == 'K' || it == ' '
                    }
                    totalAmountText = validChars
                  },
                  textStyle = TextStyle(
                    fontSize = amountFontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = primaryColor
                  ),
                  cursorBrush = SolidColor(primaryColor),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                  visualTransformation = remember { ThousandSeparatorVisualTransformation('.') },
                  singleLine = true,
                  modifier = Modifier.widthIn(min = 60.dp, max = 280.dp),
                  decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                      if (totalAmountText.isEmpty()) {
                        Text(
                          text = "0",
                          fontSize = amountFontSize,
                          fontWeight = FontWeight.SemiBold,
                          color = primaryColor.copy(alpha = 0.25f),
                          textAlign = TextAlign.Center
                        )
                      }
                      innerTextField()
                    }
                  }
                )

                Spacer(Modifier.width(6.dp))

                val currencySymbol = when (selectedCurrency) {
                  "USD" -> "$"
                  "EUR" -> "€"
                  "SGD" -> "S$"
                  else -> "₫"
                }
                Text(
                  text = currencySymbol,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Medium,
                  color = primaryColor
                )
              }

              Spacer(Modifier.height(10.dp))

              // SLEEK COMPACT CURRENCY SELECTOR (Opens Currency Modal Bottom Sheet)
              Surface(
                onClick = { showCurrencySheet = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD3D8DC)),
                shadowElevation = 0.5.dp,
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                  val symbol = when (selectedCurrency) {
                    "USD" -> "$"
                    "EUR" -> "€"
                    "SGD" -> "S$"
                    else -> "₫"
                  }
                  Text(
                    text = "$selectedCurrency ($symbol)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = primaryColor
                  )
                  Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Chọn loại tiền tệ",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              // Exchange Rate indicator if foreign currency
              if (selectedCurrency != "VND") {
                Spacer(Modifier.height(8.dp))
                val totalNum = totalAmountText.replace(",", "").toDoubleOrNull() ?: 0.0
                val rateNum = exchangeRateText.toDoubleOrNull() ?: 1.0
                val vndEquivalent = totalNum * rateNum

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showExchangeRateDialog = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Icon(
                    Icons.Rounded.CurrencyExchange,
                    contentDescription = null,
                    tint = secondaryTextColor,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "≈ ${String.format(Locale.US, "%,.0f", vndEquivalent)} ₫ (1 $selectedCurrency = ${String.format(Locale.US, "%,.0f", rateNum)} ₫)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = secondaryTextColor
                  )
                  Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Sửa tỷ giá",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(12.dp)
                  )
                }
              }
            }
          }
        }

        // ==========================================
        // 2. UNIFIED BILL INFORMATION FORM
        // (Expense Name, Category Carousel, Payer Selector. Date is removed per Requirement 3)
        // ==========================================
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {

              // Expense Name Field
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Tên hóa đơn (Ăn tối, Taxi...)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = secondaryTextColor
                  )
                  Spacer(Modifier.height(2.dp))
                  BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = TextStyle(
                      fontSize = 15.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = primaryColor
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(primaryColor),
                    decorationBox = { innerTextField ->
                      if (description.isEmpty()) {
                        Text(
                          "Nhập tên hóa đơn...",
                          style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Normal
                          )
                        )
                      }
                      innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                  )
                }

                if (description.isNotEmpty()) {
                  IconButton(
                    onClick = { description = "" },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      Icons.Rounded.Cancel,
                      contentDescription = "Xóa",
                      tint = MaterialTheme.colorScheme.outlineVariant,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }

              // Category Picker Carousel
              val quickCategories = remember(selectedCategory) {
                val defaults = listOf(
                  BillCategory.FOOD,
                  BillCategory.TRANSPORT,
                  BillCategory.SHOPPING,
                  BillCategory.ENTERTAINMENT,
                  BillCategory.BILLS
                )
                val current = BillCategory.fromKey(selectedCategory)
                if (current !in defaults) {
                  listOf(current) + defaults.take(4)
                } else {
                  defaults
                }
              }

              LazyRow(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                items(quickCategories, key = { it.key }) { cat ->
                  val isSelected = cat.key == selectedCategory
                  Surface(
                    onClick = { selectedCategory = cat.key },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) primaryColor else Color(0xFFF1F3F5),
                    border = androidx.compose.foundation.BorderStroke(
                      width = 1.dp,
                      color = if (isSelected) primaryColor else Color(0xFFD3D8DC)
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                      Icon(
                        imageVector = cat.icon,
                        contentDescription = cat.displayName,
                        tint = if (isSelected) Color.White else cat.iconColor,
                        modifier = Modifier.size(16.dp)
                      )
                      Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 13.sp
                        ),
                        color = if (isSelected) Color.White else Color(0xFF111827)
                      )
                    }
                  }
                }

                item {
                  Surface(
                    onClick = { showCategoryPicker = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F3F5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD3D8DC)),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                      Icon(
                        Icons.Rounded.MoreHoriz,
                        contentDescription = "Khác",
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(16.dp)
                      )
                      Text(
                        text = "Khác...",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 13.sp
                        ),
                        color = Color(0xFF111827)
                      )
                    }
                  }
                }
              }

              Spacer(Modifier.height(8.dp))
              HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = borderColor, thickness = 1.dp)

              // Payer Selector Row
              val selectedPayer = members.find { it.userId == selectedPayerId } ?: members.firstOrNull()
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showPayerSheet = true }
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Avatar with Check badge
                Box(modifier = Modifier.size(42.dp)) {
                  if (selectedPayer != null && !selectedPayer.avatarUrl.isNullOrBlank()) {
                    val avatarFullUrl = remember(selectedPayer.avatarUrl) {
                      if (selectedPayer.avatarUrl.startsWith("http")) selectedPayer.avatarUrl
                      else "${SupabaseConfig.SUPABASE_URL}${if (selectedPayer.avatarUrl.startsWith("/")) "" else "/"}${selectedPayer.avatarUrl}"
                    }
                    AsyncImage(
                      model = avatarFullUrl,
                      contentDescription = selectedPayer.username,
                      modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                      contentScale = ContentScale.Crop
                    )
                  } else {
                    GradientAvatar(name = selectedPayer?.username ?: "P", size = 42.dp)
                  }

                  Box(
                    modifier = Modifier
                      .size(16.dp)
                      .align(Alignment.BottomEnd)
                      .background(primaryColor, CircleShape)
                      .border(1.5.dp, cardBg, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      Icons.Rounded.Check,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(10.dp)
                    )
                  }
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Ai là người trả tiền?",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = secondaryTextColor
                  )
                  Spacer(Modifier.height(2.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    val isYou = selectedPayer?.userId == currentUserId
                    Text(
                      text = if (isYou) "Bạn (${selectedPayer?.username ?: ""})" else (selectedPayer?.username ?: "Chọn người trả"),
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = primaryColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = primaryColor.copy(alpha = 0.08f)
                    ) {
                      Text(
                        text = "Người trả",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                      )
                    }
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Text(
                    text = "Đổi",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = secondaryTextColor
                  )
                  Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              // REQUIREMENT 3: Date selector is completely removed per user request.
            }
          }
        }

        // ==========================================
        // 3. SPLIT METHOD & CALCULATION SECTION
        // (Segmented Modes, Horizontal Avatar Chips, Calculation List)
        // ==========================================
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              // Section Header
              Text(
                text = "PHƯƠNG THỨC CHIA",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = secondaryTextColor
              )
              Text(
                text = "Cách chia tiền",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = primaryColor
              )

              Spacer(Modifier.height(12.dp))

              // Segmented control (4 modes: Đều, Tùy chỉnh, %, Phần)
              Surface(
                shape = RoundedCornerShape(24.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  val modes = listOf(
                    SplitMode.EQUAL to "Đều",
                    SplitMode.CUSTOM to "Tùy chỉnh",
                    SplitMode.PERCENTAGE to "%",
                    SplitMode.SHARES to "Phần"
                  )
                  modes.forEach { (mode, label) ->
                    val isSelected = splitMode == mode
                    Surface(
                      onClick = { splitMode = mode },
                      shape = RoundedCornerShape(20.dp),
                      color = if (isSelected) primaryColor else Color.Transparent,
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                      ) {
                        Text(
                          text = label,
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp
                          ),
                          color = if (isSelected) Color.White else Color(0xFF111827)
                        )
                      }
                    }
                  }
                }
              }

              // Percentage warning if invalid
              if (splitMode == SplitMode.PERCENTAGE && percentageWarning) {
                Spacer(Modifier.height(8.dp))
                Text(
                  text = "Tổng phần trăm hiện tại là ${String.format(Locale.US, "%.1f", sumPercentages)}% (phải bằng 100%)",
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
              }

              // Custom amount warning if invalid
              if (splitMode == SplitMode.CUSTOM && customMismatch) {
                Spacer(Modifier.height(8.dp))
                Text(
                  text = "Tổng chia (${String.format(Locale.US, "%,d", customSum.toLong())} $selectedCurrency) chưa khớp với tổng hóa đơn (${String.format(Locale.US, "%,d", parsedTotal.toLong())} $selectedCurrency)",
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
              }

              Spacer(Modifier.height(16.dp))

              // Horizontal Participant Selector
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val activeCount = activeMemberIds.values.count { it }
                Text(
                  text = "Người tham gia ($activeCount/${members.size})",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = primaryColor
                )

                val allActive = members.all { activeMemberIds[it.userId] != false }
                TextButton(
                  onClick = {
                    members.forEach { m -> activeMemberIds[m.userId] = !allActive }
                  },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = if (allActive) "Bỏ chọn tất cả" else "Chọn tất cả",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor
                  )
                }
              }

              Spacer(Modifier.height(6.dp))

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(members, key = { it.userId }) { member ->
                  val isSelected = activeMemberIds[member.userId] != false
                  val isYou = member.userId == currentUserId

                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { activeMemberIds[member.userId] = !isSelected }
                      .padding(2.dp)
                  ) {
                    Box(
                      modifier = Modifier.size(50.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      val avatarModifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .then(
                          if (isSelected) Modifier.border(2.dp, primaryColor, CircleShape)
                          else Modifier
                        )
                        .graphicsLayer(alpha = if (isSelected) 1f else 0.35f)

                      if (!member.avatarUrl.isNullOrBlank()) {
                        val avatarFullUrl = remember(member.avatarUrl) {
                          if (member.avatarUrl.startsWith("http")) member.avatarUrl
                          else "${SupabaseConfig.SUPABASE_URL}${if (member.avatarUrl.startsWith("/")) "" else "/"}${member.avatarUrl}"
                        }
                        AsyncImage(
                          model = avatarFullUrl,
                          contentDescription = member.username,
                          modifier = avatarModifier,
                          contentScale = ContentScale.Crop
                        )
                      } else {
                        Box(modifier = avatarModifier) {
                          GradientAvatar(name = member.username, size = 46.dp)
                        }
                      }

                      if (isSelected) {
                        Box(
                          modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(primaryColor, CircleShape)
                            .border(1.5.dp, cardBg, CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                          )
                        }
                      }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                      text = if (isYou) "Bạn" else member.username,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                      ),
                      color = if (isSelected) primaryColor else secondaryTextColor.copy(alpha = 0.6f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.widthIn(max = 56.dp),
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }

              Spacer(Modifier.height(14.dp))

              // Per-member Split Calculation List
              val activeCount = activeMemberIds.values.count { it }
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { member ->
                  val isParticipating = activeMemberIds[member.userId] != false
                  val isYou = member.userId == currentUserId

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      // Left: Name & fraction badge
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Text(
                          text = if (isYou) "Bạn (${member.username})" else member.username,
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isParticipating) FontWeight.SemiBold else FontWeight.Normal
                          ),
                          color = if (isParticipating) primaryColor else secondaryTextColor.copy(alpha = 0.5f),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )

                        if (isParticipating) {
                          val fractionLabel = when (splitMode) {
                            SplitMode.EQUAL -> if (activeCount > 0) "1/$activeCount phần" else ""
                            SplitMode.PERCENTAGE -> "${splitPercentages[member.userId] ?: "0"}%"
                            SplitMode.SHARES -> "${splitShares[member.userId] ?: "1"} phần"
                            SplitMode.CUSTOM -> "Tùy chỉnh"
                          }
                          if (fractionLabel.isNotEmpty()) {
                            Surface(
                              shape = RoundedCornerShape(6.dp),
                              color = Color(0xFFE2E6E9),
                              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFC8CED3))
                            ) {
                              Text(
                                text = fractionLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF111827),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                              )
                            }
                          }
                        } else {
                          Text(
                            text = "Không tham gia",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = Color(0xFF73787B)
                          )
                        }
                      }

                      // Right: Amount or Edit control
                      if (!isParticipating) {
                        Text(
                          text = "0 $selectedCurrency",
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                          color = Color(0xFF73787B)
                        )
                      } else {
                        when (splitMode) {
                          SplitMode.EQUAL -> {
                            val amt = splitAmounts[member.userId] ?: "0"
                            Text(
                              text = "$amt $selectedCurrency",
                              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                              color = primaryColor
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
                                    } catch (_: Exception) { }
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
                              modifier = Modifier.width(130.dp),
                              textStyle = TextStyle(
                                color = primaryColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                              ),
                              colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryColor,
                                unfocusedTextColor = primaryColor,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFBDC4CA)
                              ),
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                              singleLine = true,
                              shape = RoundedCornerShape(10.dp),
                              suffix = { Text(selectedCurrency, color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                          }
                          SplitMode.PERCENTAGE -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                              OutlinedTextField(
                                value = splitPercentages[member.userId] ?: "",
                                onValueChange = { input ->
                                  val clean = input.filter { it.isDigit() || it == '.' }
                                  if (clean.count { it == '.' } <= 1) {
                                    splitPercentages[member.userId] = clean
                                  }
                                },
                                modifier = Modifier.width(86.dp),
                                textStyle = TextStyle(
                                  color = primaryColor,
                                  fontSize = 15.sp,
                                  fontWeight = FontWeight.Bold,
                                  textAlign = TextAlign.End
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                  focusedTextColor = primaryColor,
                                  unfocusedTextColor = primaryColor,
                                  focusedContainerColor = Color.White,
                                  unfocusedContainerColor = Color.White,
                                  focusedBorderColor = primaryColor,
                                  unfocusedBorderColor = Color(0xFFBDC4CA)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                suffix = { Text("%", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                              )
                              val amt = splitAmounts[member.userId] ?: ""
                              if (amt.isNotEmpty()) {
                                Text(
                                  text = "$amt $selectedCurrency",
                                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                  color = primaryColor
                                )
                              }
                            }
                          }
                          SplitMode.SHARES -> {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                              // Stepper Capsule gọn gàng, tinh tế
                              Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDC4CA)),
                                modifier = Modifier.height(28.dp)
                              ) {
                                Row(
                                  verticalAlignment = Alignment.CenterVertically,
                                  modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                  Box(
                                    modifier = Modifier
                                      .size(22.dp)
                                      .clip(CircleShape)
                                      .clickable {
                                        val current = splitShares[member.userId]?.toIntOrNull() ?: 1
                                        if (current > 1) {
                                          splitShares[member.userId] = (current - 1).toString()
                                        }
                                      },
                                    contentAlignment = Alignment.Center
                                  ) {
                                    Icon(
                                      Icons.Rounded.Remove,
                                      contentDescription = "Giảm",
                                      tint = primaryColor,
                                      modifier = Modifier.size(14.dp)
                                    )
                                  }

                                  Text(
                                    text = splitShares[member.userId] ?: "1",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                      fontWeight = FontWeight.Bold,
                                      fontSize = 13.sp
                                    ),
                                    color = primaryColor,
                                    modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center
                                  )

                                  Box(
                                    modifier = Modifier
                                      .size(22.dp)
                                      .clip(CircleShape)
                                      .clickable {
                                        val current = splitShares[member.userId]?.toIntOrNull() ?: 1
                                        splitShares[member.userId] = (current + 1).toString()
                                      },
                                    contentAlignment = Alignment.Center
                                  ) {
                                    Icon(
                                      Icons.Rounded.Add,
                                      contentDescription = "Tăng",
                                      tint = primaryColor,
                                      modifier = Modifier.size(14.dp)
                                    )
                                  }
                                }
                              }

                              val amt = splitAmounts[member.userId] ?: ""
                              if (amt.isNotEmpty()) {
                                Text(
                                  text = "$amt $selectedCurrency",
                                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                  color = primaryColor
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

              // REQUIREMENT 2: "Khớp số tiền (Đã phân bổ 100%)" container is REMOVED per user request!
            }
          }
        }

        // ==========================================
        // 4. RECEIPT IMAGE & SCAN SECTION (Requirement 4)
        // ==========================================
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              // Header with Title + "Quét hóa đơn" button
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = "Ảnh hóa đơn",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor
                  )
                  val hasImage = imageBytes != null || selectedImageUri != null || existingBill?.receiptUrl != null
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = surfaceContainer
                  ) {
                    Text(
                      text = if (hasImage) "1 ảnh" else "0 ảnh",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                      color = secondaryTextColor,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                // "Quét hóa đơn" Action Button
                TextButton(
                  onClick = {
                    Toast.makeText(
                      context,
                      "Tính năng Quét hóa đơn (OCR) đang được phát triển. Tạm thời bạn có thể chọn ảnh từ thư viện!",
                      Toast.LENGTH_SHORT
                    ).show()
                    galleryLauncher.launch("image/*")
                  },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      Icons.Rounded.DocumentScanner,
                      contentDescription = null,
                      tint = primaryColor,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      text = "Quét hóa đơn",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = primaryColor
                    )
                  }
                }
              }

              Spacer(Modifier.height(12.dp))

              // Horizontal thumbnails row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // If an image is selected / existing
                if (imageBytes != null || selectedImageUri != null || existingBill?.receiptUrl != null) {
                  Box(
                    modifier = Modifier
                      .size(width = 80.dp, height = 96.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = surfaceContainer,
                      shadowElevation = 2.dp,
                      modifier = Modifier.fillMaxSize()
                    ) {
                      Box(modifier = Modifier.fillMaxSize()) {
                        val imageModel = selectedImageUri ?: imageBytes ?: (
                          if (existingBill?.receiptUrl?.startsWith("http") == true) existingBill.receiptUrl
                          else "${SupabaseConfig.SUPABASE_URL}${existingBill?.receiptUrl ?: ""}"
                        )
                        AsyncImage(
                          model = imageModel,
                          contentDescription = "Hóa đơn",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )

                        // "Bill #1" Badge
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = Color.Black.copy(alpha = 0.6f),
                          modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          ) {
                            Icon(
                              Icons.Rounded.Receipt,
                              contentDescription = null,
                              tint = Color.White,
                              modifier = Modifier.size(10.dp)
                            )
                            Text(
                              text = "Bill #1",
                              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                              color = Color.White
                            )
                          }
                        }
                      }
                    }

                    // Delete X Button
                    IconButton(
                      onClick = {
                        imageBytes = null
                        selectedImageUri = null
                      },
                      modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .background(cardBg, CircleShape)
                        .border(1.dp, borderColor, CircleShape)
                    ) {
                      Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Xóa ảnh",
                        tint = primaryColor,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }

                // "+ Thêm ảnh" Button
                Surface(
                  onClick = { galleryLauncher.launch("image/*") },
                  shape = RoundedCornerShape(12.dp),
                  color = surfaceContainerLow,
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                  modifier = Modifier
                    .size(width = 80.dp, height = 96.dp)
                    .clip(RoundedCornerShape(12.dp))
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                  ) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, CircleShape)
                        .shadow(1.dp, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        Icons.Rounded.AddAPhoto,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                      text = "Thêm ảnh",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                      color = secondaryTextColor
                    )
                  }
                }
              }
            }
          }
        }

        item {
          Spacer(Modifier.height(8.dp))
        }
      }
    }

    // --- Payer Selection Modal Bottom Sheet ---
    if (showPayerSheet) {
      ModalBottomSheet(
        onDismissRequest = { showPayerSheet = false },
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
        ) {
          Text(
            text = "Ai là người trả tiền?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = primaryColor
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "Chọn thành viên đã thanh toán hóa đơn này",
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor
          )
          Spacer(Modifier.height(16.dp))

          members.forEach { member ->
            val isSelected = member.userId == selectedPayerId
            val isYou = member.userId == currentUserId
            Surface(
              onClick = {
                selectedPayerId = member.userId
                showPayerSheet = false
              },
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) primaryColor.copy(alpha = 0.08f) else Color.Transparent,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  if (!member.avatarUrl.isNullOrBlank()) {
                    val avatarFullUrl = remember(member.avatarUrl) {
                      if (member.avatarUrl.startsWith("http")) member.avatarUrl
                      else "${SupabaseConfig.SUPABASE_URL}${if (member.avatarUrl.startsWith("/")) "" else "/"}${member.avatarUrl}"
                    }
                    AsyncImage(
                      model = avatarFullUrl,
                      contentDescription = member.username,
                      modifier = Modifier.size(40.dp).clip(CircleShape),
                      contentScale = ContentScale.Crop
                    )
                  } else {
                    GradientAvatar(name = member.username, size = 40.dp)
                  }

                  Column {
                    Text(
                      text = if (isYou) "Bạn (${member.username})" else member.username,
                      style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                      ),
                      color = primaryColor
                    )
                    if (isYou) {
                      Text(
                        text = "Tài khoản hiện tại",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = secondaryTextColor
                      )
                    }
                  }
                }

                if (isSelected) {
                  Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    // --- Exchange Rate Dialog ---
    if (showExchangeRateDialog) {
      var tempRate by remember { mutableStateOf(exchangeRateText) }
      AlertDialog(
        onDismissRequest = { showExchangeRateDialog = false },
        title = { Text("Tỷ giá quy đổi", fontWeight = FontWeight.Bold) },
        text = {
          Column {
            Text("Nhập tỷ giá (1 $selectedCurrency = ... VND):", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = tempRate,
              onValueChange = { tempRate = it },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          TextButton(onClick = {
            exchangeRateText = tempRate
            showExchangeRateDialog = false
          }) {
            Text("Xác nhận", fontWeight = FontWeight.Bold, color = primaryColor)
          }
        },
        dismissButton = {
          TextButton(onClick = { showExchangeRateDialog = false }) {
            Text("Hủy")
          }
        }
      )
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
      CategoryPickerBottomSheet(
        selectedCategory = BillCategory.fromKey(selectedCategory),
        onCategorySelected = { selectedCategory = it.key },
        onDismiss = { showCategoryPicker = false }
      )
    }

    // Currency Picker Bottom Sheet
    if (showCurrencySheet) {
      ModalBottomSheet(
        onDismissRequest = { showCurrencySheet = false },
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Chọn loại tiền tệ",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = primaryColor
              )
              Spacer(Modifier.height(2.dp))
              Text(
                text = "Hệ thống hỗ trợ tự động quy đổi theo tỷ giá",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
              )
            }
            IconButton(
              onClick = { showCurrencySheet = false },
              modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFF1F3F5), CircleShape)
            ) {
              Icon(
                Icons.Rounded.Close,
                contentDescription = "Đóng",
                tint = primaryColor,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Spacer(Modifier.height(18.dp))

          val currencyList = listOf(
            Triple("VND", "Đồng Việt Nam (₫)", "Tiền tệ mặc định"),
            Triple("USD", "Đô la Mỹ ($)", "Tỷ giá: 1 USD ≈ 25.400 ₫"),
            Triple("EUR", "Đồng Euro (€)", "Tỷ giá: 1 EUR ≈ 27.500 ₫"),
            Triple("SGD", "Đô la Singapore (S$)", "Tỷ giá: 1 SGD ≈ 19.200 ₫")
          )

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            currencyList.forEach { (code, title, desc) ->
              val isSelected = selectedCurrency == code
              Surface(
                onClick = {
                  selectedCurrency = code
                  showCurrencySheet = false
                },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) primaryColor.copy(alpha = 0.06f) else Color(0xFFF8F9FA),
                border = androidx.compose.foundation.BorderStroke(
                  width = if (isSelected) 1.8.dp else 1.dp,
                  color = if (isSelected) primaryColor else Color(0xFFE2E6E9)
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                  ) {
                    // Currency Circle Badge
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .background(
                          if (isSelected) primaryColor else Color(0xFFE8ECEF),
                          CircleShape
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      val symbol = when (code) {
                        "USD" -> "$"
                        "EUR" -> "€"
                        "SGD" -> "S$"
                        else -> "₫"
                      }
                      Text(
                        text = symbol,
                        color = if (isSelected) Color.White else primaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }

                    Column {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Text(
                          text = code,
                          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                          color = primaryColor
                        )
                        Text(
                          text = "• " + title.substringAfter(" ").trim(),
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                          color = Color(0xFF111827)
                        )
                      }
                      Spacer(Modifier.height(2.dp))
                      Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = secondaryTextColor
                      )
                    }
                  }

                  if (isSelected) {
                    Icon(
                      Icons.Rounded.CheckCircle,
                      contentDescription = null,
                      tint = primaryColor,
                      modifier = Modifier.size(24.dp)
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

private fun compressReceiptImage(context: Context, uri: Uri): ByteArray? {
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
    val formatted = StringBuilder()
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
