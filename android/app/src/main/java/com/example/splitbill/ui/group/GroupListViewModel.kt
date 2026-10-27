package com.example.splitbill.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.InviteRepository
import com.example.splitbill.data.api.GroupResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GroupListUiState {
  object Loading : GroupListUiState
  data class Success(
    val groups: List<GroupResponse>,
    val actionMessage: String? = null,
    val totalToReceive: Double = 0.0,
    val totalToPay: Double = 0.0,
    val groupSpentMap: Map<String, Double> = emptyMap(),
    val groupBalanceMap: Map<String, Double> = emptyMap()
  ) : GroupListUiState
  data class Error(val message: String) : GroupListUiState
}

class GroupListViewModel(
  private val groupRepository: GroupRepository,
  private val inviteRepository: InviteRepository,
  private val statsRepository: com.example.splitbill.data.StatsRepository? = null
) : ViewModel() {

  companion object {
    private var cachedState: GroupListUiState.Success? = null
    fun clearCache() {
      cachedState = null
    }
  }

  private val _uiState = MutableStateFlow<GroupListUiState>(
    cachedState ?: GroupListUiState.Loading
  )
  val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

  private var activeLoadJob: Job? = null

  init {
    loadGroups()

    // Quan sát Room groups cache qua Flow (SSOT): khi DB cập nhật, UI tự cập nhật tức thì
    viewModelScope.launch {
      groupRepository.observeGroups()?.collect { cachedGroups ->
        if (cachedGroups.isNotEmpty()) {
          val current = _uiState.value
          val lastSuccess = (current as? GroupListUiState.Success) ?: cachedState
          val updated = GroupListUiState.Success(
            groups = cachedGroups,
            actionMessage = lastSuccess?.actionMessage,
            totalToReceive = lastSuccess?.totalToReceive ?: 0.0,
            totalToPay = lastSuccess?.totalToPay ?: 0.0,
            groupSpentMap = lastSuccess?.groupSpentMap ?: emptyMap(),
            groupBalanceMap = lastSuccess?.groupBalanceMap ?: emptyMap()
          )
          _uiState.value = updated
          cachedState = updated
        }
      }
    }

    // Lắng nghe cập nhật ảnh bìa từ GroupDetailScreen để đổi tức thì trên GroupListScreen
    viewModelScope.launch {
      groupRepository.bannerUpdateFlow.collect { (groupId, newBannerUrl) ->
        updateGroupBannerInMemory(groupId, newBannerUrl)
      }
    }
  }

  fun updateGroupBannerInMemory(groupId: String, newBannerUrl: String) {
    val current = _uiState.value
    val target = (current as? GroupListUiState.Success) ?: cachedState
    if (target != null) {
      val updatedGroups = target.groups.map { group ->
        if (group.id == groupId) {
          group.copy(bannerUrl = newBannerUrl)
        } else group
      }
      val newState = target.copy(groups = updatedGroups)
      _uiState.value = newState
      cachedState = newState
    }
  }

  fun loadGroups(onComplete: (() -> Unit)? = null) {
    // Ngay khi loadGroups được gọi, đồng bộ ảnh bìa mới nhất từ SharedPreferences ngay lập tức
    val target = (_uiState.value as? GroupListUiState.Success) ?: cachedState
    if (target != null) {
      val syncedGroups = target.groups.map { group ->
        val localBanner = groupRepository.getLocalGroupBanner(group.id)
        if (localBanner != null && localBanner != group.bannerUrl) {
          group.copy(bannerUrl = localBanner)
        } else group
      }
      if (syncedGroups != target.groups) {
        val newState = target.copy(groups = syncedGroups)
        _uiState.value = newState
        cachedState = newState
      }
    }

    // Nếu đang có tác vụ tải đang chạy, tránh gọi mạng trùng lặp đồng thời
    if (activeLoadJob?.isActive == true) {
      onComplete?.invoke()
      return
    }

    // Chỉ hiện skeleton lần đầu nếu chưa hề có dữ liệu nhóm nào
    if (_uiState.value !is GroupListUiState.Success && cachedState == null) {
      _uiState.value = GroupListUiState.Loading
    }

    activeLoadJob = viewModelScope.launch {
      try {
        // Tải song song danh sách nhóm và số dư thống kê
        val groupsDeferred = async { groupRepository.getGroups() }
        val statsDeferred = async { statsRepository?.getUserStats() }

        val groupsResult = groupsDeferred.await()
        val statsResult = statsDeferred.await()

        if (groupsResult.isSuccess) {
          val rawGroups = groupsResult.getOrDefault(emptyList())
          val groups = rawGroups.map { g ->
            val b = groupRepository.getLocalGroupBanner(g.id) ?: g.bannerUrl
            g.copy(bannerUrl = b)
          }
          val stats = statsResult?.getOrNull() ?: statsRepository?.let { com.example.splitbill.data.StatsRepository.getCachedStats() }

          val current = _uiState.value
          val lastSuccess = (current as? GroupListUiState.Success) ?: cachedState

          val spentMap = if (stats != null) {
            stats.spentByGroup.associate { it.groupId to it.amount }
          } else {
            lastSuccess?.groupSpentMap ?: emptyMap()
          }

          val balanceMap = if (stats != null) {
            stats.groupBalances
          } else {
            lastSuccess?.groupBalanceMap ?: emptyMap()
          }

          val totalReceive = stats?.totalOthersOweToMe ?: lastSuccess?.totalToReceive ?: 0.0
          val totalPay = stats?.totalOwedToOthers ?: lastSuccess?.totalToPay ?: 0.0

          val successState = GroupListUiState.Success(
            groups = groups,
            actionMessage = lastSuccess?.actionMessage,
            totalToReceive = totalReceive,
            totalToPay = totalPay,
            groupSpentMap = spentMap,
            groupBalanceMap = balanceMap
          )
          _uiState.value = successState
          cachedState = successState
        } else {
          val errorMsg = groupsResult.exceptionOrNull()?.message ?: "Lỗi tải nhóm"
          if (_uiState.value !is GroupListUiState.Success && cachedState == null) {
            _uiState.value = GroupListUiState.Error(errorMsg)
          }
        }
      } catch (e: Exception) {
        if (_uiState.value !is GroupListUiState.Success && cachedState == null) {
          _uiState.value = GroupListUiState.Error(e.message ?: "Lỗi tải nhóm")
        }
      } finally {
        activeLoadJob = null
        onComplete?.invoke()
      }
    }
  }

  fun createGroup(name: String) {
    viewModelScope.launch {
      val result = groupRepository.createGroup(name)
      if (result.isSuccess) {
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Đã tạo nhóm thành công")
        }
        loadGroups() // Reload
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi tạo nhóm"
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Lỗi: $errorMsg")
        } else {
          _uiState.value = GroupListUiState.Error(errorMsg)
        }
      }
    }
  }

  fun joinGroup(groupId: String) {
    viewModelScope.launch {
      val result = groupRepository.joinGroup(groupId)
      if (result.isSuccess) {
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = result.getOrNull() ?: "Đã tham gia nhóm")
        }
        loadGroups() // Reload
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi tham gia nhóm"
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Lỗi: $errorMsg")
        } else {
          _uiState.value = GroupListUiState.Error(errorMsg)
        }
      }
    }
  }

  fun joinByInvite(inviteCode: String) {
    viewModelScope.launch {
      val result = inviteRepository.joinByInvite(inviteCode)
      if (result.isSuccess) {
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Đã tham gia nhóm thành công")
        }
        loadGroups() // Reload
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi tham gia bằng mã mời"
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Lỗi: $errorMsg")
        } else {
          _uiState.value = GroupListUiState.Error(errorMsg)
        }
      }
    }
  }

  fun clearActionMessage() {
    val currentState = _uiState.value
    if (currentState is GroupListUiState.Success) {
      _uiState.value = currentState.copy(actionMessage = null)
    }
  }
}
