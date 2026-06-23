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
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val tokenManager = remember { TokenManager(context) }
  val token by tokenManager.getToken().collectAsState(initial = null)
  val currentUserId = remember(token) { TokenManager.getUserIdFromToken(token) }

  var description by rememberSaveable { mutableStateOf("") }
  var totalAmountText by rememberSaveable { mutableStateOf("") }
  var selectedPayerId by rememberSaveable(currentUserId, members) { 
    mutableStateOf(currentUserId ?: members.firstOrNull()?.userId ?: "") 
  }
  // Map: userId -> amount text they owe
  val splitAmounts = remember { mutableStateMapOf<String, String>() }
  var splitMode by rememberSaveable { mutableStateOf(SplitMode.EQUAL) }
  var payerDropdownExpanded by remember { mutableStateOf(false) }
  var showSuccessOverlay by remember { mutableStateOf(false) }

  // Auto-fill equal splits when amount changes
  LaunchedEffect(totalAmountText, splitMode) {
    if (splitMode == SplitMode.EQUAL && members.isNotEmpty()) {
      val total = totalAmountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
      val perPerson = Math.floor(total / members.size)
      val remainder = total - (perPerson * members.size)
      
      members.forEachIndexed { index, member ->
        val finalAmount = if (index == 0) perPerson + remainder else perPerson
        splitAmounts[member.userId] = if (finalAmount > 0) String.format(Locale.US, "%,d", finalAmount.toLong()) else ""
      }
    }
  }

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
          billDescription = description,
          amount = totalAmountText + "đ"
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
          title = "Thêm hóa đơn",
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
                Text(
                  "Tổng số tiền",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(Dimens.SpacingXS))
                OutlinedTextField(
                  value = totalAmountText,
                  onValueChange = { input -> 
                    val clean = input.filter { it.isDigit() }
                    if (clean.isNotEmpty()) {
                      try {
                        totalAmountText = String.format(Locale.US, "%,d", clean.toLong())
                      } catch (e: Exception) { }
                    } else {
                      totalAmountText = ""
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
                  suffix = { Text("đ", style = MaterialTheme.typography.headlineMedium) }
                )
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
                  shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                  label = { Text("Chia đều", fontWeight = FontWeight.Bold) }
                )
                SegmentedButton(
                  selected = splitMode == SplitMode.CUSTOM,
                  onClick = { splitMode = SplitMode.CUSTOM },
                  shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                  label = { Text("Tùy chỉnh", fontWeight = FontWeight.Bold) }
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
                OutlinedTextField(
                  value = splitAmounts[member.userId] ?: "",
                  onValueChange = { input -> 
                    val clean = input.filter { it.isDigit() }
                    if (clean.isNotEmpty()) {
                      try {
                        splitAmounts[member.userId] = String.format(Locale.US, "%,d", clean.toLong())
                      } catch (e: Exception) { }
                    } else {
                      splitAmounts[member.userId] = ""
                    }
                  },
                  enabled = splitMode == SplitMode.CUSTOM,
                  modifier = Modifier.width(120.dp),
                  textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  suffix = { Text("đ") }
                )
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
                  val total = totalAmountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                  val splits = members.mapNotNull { member ->
                    val amount = splitAmounts[member.userId]?.filter { it.isDigit() }?.toDoubleOrNull() ?: 0.0
                    if (amount > 0) BillSplitItem(member.userId, amount) else null
                  }
                  viewModel.createBill(groupId, description, total, selectedPayerId, splits)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState !is AddBillUiState.Loading && totalAmountText.isNotBlank() && description.isNotBlank()
              ) {
                if (uiState is AddBillUiState.Loading) {
                  CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                } else {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                  Spacer(Modifier.width(Dimens.SpacingS))
                  Text("Lưu hóa đơn", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
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

enum class SplitMode { EQUAL, CUSTOM }

