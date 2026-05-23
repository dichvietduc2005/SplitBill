package com.example.splitbill.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.ProfileRepository
import com.example.splitbill.data.api.DebtResponse
import com.example.splitbill.data.api.ProfileResponse
import com.example.splitbill.data.api.SimplifiedDebt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DebtSummaryUiState {
  object Loading : DebtSummaryUiState
  data class Success(val data: DebtResponse) : DebtSummaryUiState
  data class Error(val message: String) : DebtSummaryUiState
}

class DebtSummaryViewModel(
  private val groupId: String,
  private val billRepository: BillRepository,
  private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<DebtSummaryUiState>(DebtSummaryUiState.Loading)
  val uiState: StateFlow<DebtSummaryUiState> = _uiState.asStateFlow()

  // State cho VietQR bottom sheet: khoản nợ đang chọn + profile người nhận
  private val _selectedDebt = MutableStateFlow<SimplifiedDebt?>(null)
  val selectedDebt: StateFlow<SimplifiedDebt?> = _selectedDebt.asStateFlow()

  private val _creditorProfile = MutableStateFlow<ProfileResponse?>(null)
  val creditorProfile: StateFlow<ProfileResponse?> = _creditorProfile.asStateFlow()

  private val _currentUserId = MutableStateFlow<String>("")
  val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

  init {
    loadDebts()
    loadCurrentUserId()
  }

  private fun loadCurrentUserId() {
    viewModelScope.launch {
      val result = profileRepository.getMyProfile()
      if (result.isSuccess) {
        _currentUserId.value = result.getOrNull()?.id ?: ""
      }
    }
  }

  fun loadDebts() {
    // Only show loading state if we don't have success data yet to prevent layout flickering
    if (_uiState.value !is DebtSummaryUiState.Success) {
      _uiState.value = DebtSummaryUiState.Loading
    }
    viewModelScope.launch {
      val result = billRepository.getDebtsForGroup(groupId)
      _uiState.value = if (result.isSuccess) {
        DebtSummaryUiState.Success(result.getOrNull()!!)
      } else {
        DebtSummaryUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải dữ liệu nợ")
      }
    }
  }

  /** Khi user bấm vào khoản nợ → load profile người nhận và mở QR sheet */
  fun selectDebtForPayment(debt: SimplifiedDebt) {
    _selectedDebt.value = debt
    _creditorProfile.value = null // Reset về null để hiển thị loading trong sheet
    viewModelScope.launch {
      val result = profileRepository.getUserProfile(debt.toUserId)
      _creditorProfile.value = result.getOrNull() // null nếu lỗi, sẽ hiển thị thông báo lỗi
    }
  }

  fun dismissQrSheet() {
    _selectedDebt.value = null
    _creditorProfile.value = null
  }
}
