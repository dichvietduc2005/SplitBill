package com.example.splitbill.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.api.BillSplitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AddBillUiState {
  object Idle : AddBillUiState
  object Loading : AddBillUiState
  object Success : AddBillUiState
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
    splits: List<BillSplitItem>
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
      _uiState.value = AddBillUiState.Error("Tổng chia (${splitsSum.toLong()}đ) chưa khớp tổng hóa đơn (${totalAmount.toLong()}đ)")
      return
    }

    _uiState.value = AddBillUiState.Loading
    viewModelScope.launch {
      val result = billRepository.createBill(groupId, description, totalAmount, paidByUserId, splits)
      _uiState.value = if (result.isSuccess) {
        AddBillUiState.Success
      } else {
        AddBillUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tạo hóa đơn")
      }
    }
  }

  fun resetState() {
    _uiState.value = AddBillUiState.Idle
  }
}
