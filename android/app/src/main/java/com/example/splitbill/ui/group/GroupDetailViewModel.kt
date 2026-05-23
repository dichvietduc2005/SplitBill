package com.example.splitbill.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.data.api.MemberResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupDetailState(
  val isLoading: Boolean = true,
  val group: GroupResponse? = null,
  val members: List<MemberResponse> = emptyList(),
  val bills: List<BillResponse> = emptyList(),
  val error: String? = null,
  val actionMessage: String? = null
)

class GroupDetailViewModel(
  private val groupId: String,
  private val groupRepository: GroupRepository,
  private val billRepository: BillRepository
) : ViewModel() {

  private val _state = MutableStateFlow(GroupDetailState())
  val state: StateFlow<GroupDetailState> = _state.asStateFlow()

  init {
    loadAll()
  }

  fun loadAll() {
    // Only show loading skeleton on initial load (when group detail is empty) to prevent layout flickering
    if (_state.value.group == null) {
      _state.value = _state.value.copy(isLoading = true, error = null)
    }
    viewModelScope.launch {
      // Chạy 3 API call song song thay vì tuần tự
      val groupDeferred = async { groupRepository.getGroupDetails(groupId) }
      val membersDeferred = async { groupRepository.getMembers(groupId) }
      val billsDeferred = async { billRepository.getBillsForGroup(groupId) }

      val groupResult = groupDeferred.await()
      val membersResult = membersDeferred.await()
      val billsResult = billsDeferred.await()

      _state.value = _state.value.copy(
        isLoading = false,
        group = groupResult.getOrNull() ?: _state.value.group,
        members = membersResult.getOrElse { _state.value.members },
        bills = billsResult.getOrElse { _state.value.bills },
        error = if (groupResult.isFailure && _state.value.group == null) groupResult.exceptionOrNull()?.message else null
      )
    }
  }

  /** Refresh chỉ bills — dùng khi quay lại từ AddBillScreen */
  fun refreshBills() {
    viewModelScope.launch {
      val billsResult = billRepository.getBillsForGroup(groupId)
      _state.value = _state.value.copy(
        bills = billsResult.getOrElse { _state.value.bills }
      )
    }
  }

  fun addMember(usernameOrEmail: String) {
    viewModelScope.launch {
      val result = groupRepository.addMember(groupId, usernameOrEmail)
      if (result.isSuccess) {
        _state.value = _state.value.copy(actionMessage = result.getOrNull())
        loadAll()
      } else {
        _state.value = _state.value.copy(
          actionMessage = "Lỗi: ${result.exceptionOrNull()?.message}"
        )
      }
    }
  }

  fun deleteBill(billId: String) {
    viewModelScope.launch {
      val result = billRepository.deleteBill(billId)
      if (result.isSuccess) {
        // Xóa bill khỏi danh sách ngay lập tức (optimistic update)
        _state.value = _state.value.copy(
          bills = _state.value.bills.filter { it.id != billId }
        )
      }
    }
  }

  fun clearActionMessage() {
    _state.value = _state.value.copy(actionMessage = null)
  }
}
