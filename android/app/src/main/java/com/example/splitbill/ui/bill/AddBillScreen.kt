package com.example.splitbill.ui.bill

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.splitbill.data.api.BillSplitItem
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion
import com.example.splitbill.ui.localization.localized
import com.example.splitbill.ui.components.SplitBillTopBar
import androidx.compose.ui.platform.LocalContext

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

  var description by remember { mutableStateOf("") }
  var totalAmountText by remember { mutableStateOf("") }
  var selectedPayerId by remember { mutableStateOf(members.firstOrNull()?.userId ?: "") }
  // Map: userId -> amount text they owe
  val splitAmounts = remember { mutableStateMapOf<String, String>() }
  var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
  var payerDropdownExpanded by remember { mutableStateOf(false) }
  var showSuccessOverlay by remember { mutableStateOf(false) }

  // Auto-fill equal splits when amount changes
  LaunchedEffect(totalAmountText, splitMode) {
    if (splitMode == SplitMode.EQUAL && members.isNotEmpty()) {
      val total = totalAmountText.toDoubleOrNull() ?: 0.0
      val perPerson = if (members.isNotEmpty()) total / members.size else 0.0
      members.forEach { member ->
        splitAmounts[member.userId] = if (perPerson > 0) "%.0f".format(perPerson) else ""
      }
    }
  }

  val context = LocalContext.current
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

        // --- Description ---
        item {
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            Column {
              Text("Thông tin hóa đơn", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Spacer(Modifier.height(Dimens.SpacingS))
              OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Mô tả (VD: Ăn tối, Xăng xe...)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                singleLine = true
              )
            }
          }
        }

        // --- Total Amount ---
        item {
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            OutlinedTextField(
              value = totalAmountText,
              onValueChange = { totalAmountText = it },
              label = { Text("Tổng tiền (VNĐ)") },
              modifier = Modifier.fillMaxWidth(),
              leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              suffix = { Text("đ") }
            )
          }
        }

        // --- Payer Dropdown ---
        item {
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(2 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            Column {
              Text("Ai đã trả tiền?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Spacer(Modifier.height(Dimens.SpacingS))
              ExposedDropdownMenuBox(
                expanded = payerDropdownExpanded,
                onExpandedChange = { payerDropdownExpanded = !payerDropdownExpanded }
              ) {
                OutlinedTextField(
                  value = members.find { it.userId == selectedPayerId }?.username ?: "Chọn người trả",
                  onValueChange = {},
                  readOnly = true,
                  modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                  label = { Text("Người trả tiền") },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(payerDropdownExpanded) },
                  leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                ExposedDropdownMenu(
                  expanded = payerDropdownExpanded,
                  onDismissRequest = { payerDropdownExpanded = false }
                ) {
                  members.forEach { member ->
                    DropdownMenuItem(
                      text = { Text(member.username) },
                      onClick = {
                        selectedPayerId = member.userId
                        payerDropdownExpanded = false
                      }
                    )
                  }
                }
              }
            }
          }
        }

        // --- Split Mode Toggle ---
        item {
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(3 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.slideUp) {
            Column {
              Text("Cách chia tiền", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Spacer(Modifier.height(Dimens.SpacingS))
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                  selected = splitMode == SplitMode.EQUAL,
                  onClick = { splitMode = SplitMode.EQUAL },
                  shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                  label = { Text("Chia đều") }
                )
                SegmentedButton(
                  selected = splitMode == SplitMode.CUSTOM,
                  onClick = { splitMode = SplitMode.CUSTOM },
                  shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                  label = { Text("Tùy chỉnh") }
                )
              }
            }
          }
        }

        // --- Split Inputs per member ---
        item {
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(4 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = fadeIn()) {
            Text(
              "Chia cho từng người",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
        
        itemsIndexed(members) { index, member ->
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay((5 + index) * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = Motion.staggeredSlideIn(index)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingS)
            ) {
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  Text(
                    member.username.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }
              Text(
                member.username,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = splitAmounts[member.userId] ?: "",
                onValueChange = { splitAmounts[member.userId] = it },
                enabled = splitMode == SplitMode.CUSTOM,
                modifier = Modifier.width(110.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                suffix = { Text("đ") }
              )
            }
          }
        }

        // --- Submit Button ---
        item {
          var visible by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) { kotlinx.coroutines.delay(7 * Motion.StaggerDelay); visible = true }
          AnimatedVisibility(visible = visible, enter = slideInVertically(animationSpec = Motion.tweenSlow(), initialOffsetY = { it / 2 }) + fadeIn()) {
            Column {
              Spacer(Modifier.height(Dimens.SpacingS))
              Button(
                onClick = {
                  val total = totalAmountText.toDoubleOrNull() ?: 0.0
                  val splits = members.mapNotNull { member ->
                    val amount = splitAmounts[member.userId]?.toDoubleOrNull() ?: 0.0
                    if (amount > 0) BillSplitItem(member.userId, amount) else null
                  }
                  viewModel.createBill(groupId, description, total, selectedPayerId, splits)
                },
                modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
                enabled = uiState !is AddBillUiState.Loading
              ) {
                if (uiState is AddBillUiState.Loading) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                  Icon(Icons.Default.Check, contentDescription = null)
                  Spacer(Modifier.width(Dimens.SpacingS))
                  Text("Lưu hóa đơn", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
              }
              Spacer(Modifier.height(Dimens.SpacingM))
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
      ) {
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
          modifier = Modifier.size(120.dp).scale(scale.value)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = "Success",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(64.dp)
            )
          }
        }
      }
    }
  }
}

enum class SplitMode { EQUAL, CUSTOM }

