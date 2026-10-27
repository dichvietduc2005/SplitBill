package com.example.splitbill.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.api.BillSplitItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AddBillUiState {
  object Idle : AddBillUiState
  object Loading : AddBillUiState
  data class Success(val billId: String? = null, val groupId: String? = null) : AddBillUiState
  data class Error(val message: String) : AddBillUiState
}

class AddBillViewModel(
  private val billRepository: BillRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<AddBillUiState>(AddBillUiState.Idle)
  val uiState: StateFlow<AddBillUiState> = _uiState.asStateFlow()

  fun createBill(
    groupId: String,
    description: String,
    totalAmount: Double,
    paidByUserId: String,
    currency: String,
    exchangeRate: Double,
    splits: List<BillSplitItem>,
    receiptBytes: ByteArray? = null,
    category: String = "GENERAL",
    knownUsernames: Map<String, String> = emptyMap()
  ) {
    if (description.isBlank()) {
      _uiState.value = AddBillUiState.Error("Vui lòng nhập mô tả hóa đơn")
      return
    }
    if (totalAmount <= 0) {
      _uiState.value = AddBillUiState.Error("Tổng tiền phải lớn hơn 0")
      return
    }
    if (splits.isEmpty()) {
      _uiState.value = AddBillUiState.Error("Phải chọn ít nhất 1 người chia")
      return
    }
    val splitsSum = splits.sumOf { it.amount }
    if (kotlin.math.abs(splitsSum - totalAmount) > 0.01) {
      _uiState.value = AddBillUiState.Error("Tổng chia (${splitsSum.toLong()}$currency) chưa khớp tổng hóa đơn (${totalAmount.toLong()}$currency)")
      return
    }

    _uiState.value = AddBillUiState.Loading
    viewModelScope.launch {
      val result = billRepository.createBill(
        groupId = groupId,
        description = description,
        totalAmount = totalAmount,
        paidByUserId = paidByUserId,
        currency = currency,
        exchangeRate = exchangeRate,
        splits = splits,
        category = category,
        knownUsernames = knownUsernames
      )
      if (result.isSuccess) {
        val bill = result.getOrNull()
        if (bill != null && receiptBytes != null) {
          // Tải ảnh hóa đơn ngầm không chặn UI
          CoroutineScope(Dispatchers.IO).launch {
            try {
              billRepository.uploadReceipt(bill.id, receiptBytes)
            } catch (e: Exception) {
              android.util.Log.w("AddBillViewModel", "Upload receipt failed: ${e.message}")
            }
          }
        }
        _uiState.value = AddBillUiState.Success(billId = bill?.id, groupId = groupId)
      } else {
        _uiState.value = AddBillUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tạo hóa đơn")
      }
    }
  }

  fun updateBill(
    billId: String,
    description: String,
    totalAmount: Double,
    paidByUserId: String,
    currency: String,
    exchangeRate: Double,
    splits: List<BillSplitItem>,
    receiptBytes: ByteArray? = null,
    category: String = "GENERAL",
    knownUsernames: Map<String, String> = emptyMap()
  ) {
    if (description.isBlank()) {
      _uiState.value = AddBillUiState.Error("Vui lòng nhập mô tả hóa đơn")
      return
    }
    if (totalAmount <= 0) {
      _uiState.value = AddBillUiState.Error("Tổng tiền phải lớn hơn 0")
      return
    }
    if (splits.isEmpty()) {
      _uiState.value = AddBillUiState.Error("Phải chọn ít nhất 1 người chia")
      return
    }
    val splitsSum = splits.sumOf { it.amount }
    if (kotlin.math.abs(splitsSum - totalAmount) > 0.01) {
      _uiState.value = AddBillUiState.Error("Tổng chia (${splitsSum.toLong()}$currency) chưa khớp tổng hóa đơn (${totalAmount.toLong()}$currency)")
      return
    }

    _uiState.value = AddBillUiState.Loading
    viewModelScope.launch {
      val result = billRepository.updateBill(
        billId = billId,
        description = description,
        totalAmount = totalAmount,
        paidByUserId = paidByUserId,
        currency = currency,
        exchangeRate = exchangeRate,
        splits = splits,
        category = category,
        knownUsernames = knownUsernames
      )
      if (result.isSuccess) {
        val bill = result.getOrNull()
        if (bill != null && receiptBytes != null) {
          // Tải ảnh hóa đơn ngầm không chặn UI
          kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
              billRepository.uploadReceipt(bill.id, receiptBytes)
            } catch (e: Exception) {
              android.util.Log.w("AddBillViewModel", "Upload receipt failed: ${e.message}")
            }
          }
        }
        _uiState.value = AddBillUiState.Success(billId = bill?.id ?: billId, groupId = null)
      } else {
        _uiState.value = AddBillUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi cập nhật hóa đơn")
      }
    }
  }

  fun resetState() {
    _uiState.value = AddBillUiState.Idle
  }
}
