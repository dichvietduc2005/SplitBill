package com.example.splitbill.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.InviteRepository
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.api.InviteResponse
import com.example.splitbill.data.supabase.SupabaseConfig
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.example.splitbill.service.NotificationEventBus

data class GroupDetailState(
  val isLoading: Boolean = true,
  val group: GroupResponse? = null,
  val members: List<MemberResponse> = emptyList(),
  val bills: List<BillResponse> = emptyList(),
  val memberBalances: Map<String, Double> = emptyMap(),
  val error: String? = null,
  val actionMessage: String? = null
)

class GroupDetailViewModel(
  private val groupId: String,
  private val groupRepository: GroupRepository,
  private val billRepository: BillRepository,
  private val inviteRepository: InviteRepository
) : ViewModel() {

  companion object {
    /** Bộ nhớ đệm tĩnh: giữ state cũ để hiện tức thì khi quay lại trang */
    private val stateCache = mutableMapOf<String, GroupDetailState>()
  }

  private val _state = MutableStateFlow(
    // Khôi phục từ cache ngay lập tức nếu có dữ liệu nhóm thực tế
    stateCache[groupId]?.takeIf { it.group != null }?.copy(isLoading = false) ?: GroupDetailState()
  )
  val state: StateFlow<GroupDetailState> = _state.asStateFlow()

  private var realtimeChannel: RealtimeChannel? = null

  init {
    loadAll() // Refresh ngầm trong nền
    subscribeRealtime()

    // Quan sát Room bills cache qua Flow (SSOT): khi DB thay đổi, UI tự động cập nhật
    viewModelScope.launch {
      billRepository.observeBillsForGroup(groupId)?.collect { cachedBills ->
        if (cachedBills.isNotEmpty()) {
          val currentMembers = _state.value.members
          val updatedBalances = computeBalances(cachedBills, currentMembers)
          _state.value = _state.value.copy(
            bills = cachedBills,
            memberBalances = updatedBalances,
            isLoading = false
          )
        }
      }
    }

    viewModelScope.launch {
      NotificationEventBus.events.collect { event ->
        if (event.groupId == groupId) {
          loadAll()
        }
      }
    }
  }

  /** Subscribe Supabase Realtime để nhận live updates cho bills & settlements */
  private fun subscribeRealtime() {
    viewModelScope.launch {
      try {
        val client = SupabaseConfig.client
        val channel = client.channel("group-$groupId")
        realtimeChannel = channel

        // Lắng nghe thay đổi bảng bills
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
          table = "bills"
        }.onEach {
          loadAll()
        }.launchIn(viewModelScope)

        // Lắng nghe thay đổi bảng settlements
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
          table = "settlements"
        }.onEach {
          loadAll()
        }.launchIn(viewModelScope)

        channel.subscribe()
      } catch (e: Exception) {
        // Realtime không bắt buộc — nếu lỗi vẫn tiếp tục với pull-to-refresh
        e.printStackTrace()
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    viewModelScope.launch {
      try { realtimeChannel?.unsubscribe() } catch (_: Exception) {}
    }
  }

  fun loadAll(onComplete: (() -> Unit)? = null) {
    // Chỉ hiện skeleton khi chưa có data nào (lần đầu hoàn toàn)
    if (_state.value.group == null) {
      _state.value = _state.value.copy(isLoading = true, error = null)
    }
    viewModelScope.launch {
      try {
        // Chạy 3 API call song song thay vì tuần tự
        val groupDeferred = async { groupRepository.getGroupDetails(groupId) }
        val membersDeferred = async { groupRepository.getMembers(groupId) }
        val billsDeferred = async { billRepository.getBillsForGroup(groupId) }

        val groupResult = groupDeferred.await()
        val membersResult = membersDeferred.await()
        val billsResult = billsDeferred.await()

        val membersList = membersResult.getOrElse { _state.value.members }
        val billsList = billsResult.getOrElse { _state.value.bills }

        val newState = _state.value.copy(
          isLoading = false,
          group = groupResult.getOrNull() ?: _state.value.group,
          members = membersList,
          bills = billsList,
          memberBalances = computeBalances(billsList, membersList),
          error = if (groupResult.isFailure && _state.value.group == null) groupResult.exceptionOrNull()?.message else null
        )
        _state.value = newState
        stateCache[groupId] = newState // Lưu vào cache
      } finally {
        onComplete?.invoke()
      }
    }
  }

  private fun computeBalances(bills: List<BillResponse>, members: List<MemberResponse>): Map<String, Double> {
    val balances = mutableMapOf<String, Double>()
    members.forEach { balances[it.userId] = 0.0 }
    
    bills.forEach { bill ->
      val rate = if (bill.exchangeRate > 0) bill.exchangeRate else 1.0
      val totalVnd = bill.totalAmount * rate
      
      // Người trả tiền ĐƯỢC nhóm nợ → balance TĂNG (VND)
      balances[bill.paidByUserId] = (balances[bill.paidByUserId] ?: 0.0) + totalVnd
      
      // Mỗi người trong splits NỢ nhóm → balance GIẢM (VND)
      bill.splits.forEach { split ->
        val splitVnd = split.amountOwed * rate
        balances[split.userId] = (balances[split.userId] ?: 0.0) - splitVnd
      }
    }
    return balances
  }

  /** Refresh chỉ bills — dùng khi quay lại từ AddBillScreen */
  fun refreshBills() {
    viewModelScope.launch {
      val billsResult = billRepository.getBillsForGroup(groupId)
      val newBills = billsResult.getOrElse { _state.value.bills }
      _state.value = _state.value.copy(
        bills = newBills,
        memberBalances = computeBalances(newBills, _state.value.members)
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
        val newBills = _state.value.bills.filter { it.id != billId }
        _state.value = _state.value.copy(
          bills = newBills,
          memberBalances = computeBalances(newBills, _state.value.members)
        )
      }
    }
  }

  fun toggleBillPaidStatus(billId: String, currentStatus: Boolean) {
    viewModelScope.launch {
      val result = billRepository.markBillAsPaid(billId, !currentStatus)
      if (result.isSuccess) {
        val updatedBills = _state.value.bills.map {
          if (it.id == billId) it.copy(isPaid = !currentStatus) else it
        }
        _state.value = _state.value.copy(
          bills = updatedBills,
          memberBalances = computeBalances(updatedBills, _state.value.members)
        )
      }
    }
  }

  fun clearActionMessage() {
    _state.value = _state.value.copy(actionMessage = null)
  }

  private val _activeInvite = MutableStateFlow<InviteResponse?>(null)
  val activeInvite: StateFlow<InviteResponse?> = _activeInvite.asStateFlow()

  fun loadOrCreateActiveInvite() {
    viewModelScope.launch {
      val activeResult = inviteRepository.getActiveInvites(groupId)
      val existing = activeResult.getOrNull()?.firstOrNull()
      if (existing != null) {
        _activeInvite.value = existing
      } else {
        val createResult = inviteRepository.createInvite(groupId, maxUses = null)
        _activeInvite.value = createResult.getOrNull()
      }
    }
  }

  fun clearActiveInvite() {
    _activeInvite.value = null
  }
}
