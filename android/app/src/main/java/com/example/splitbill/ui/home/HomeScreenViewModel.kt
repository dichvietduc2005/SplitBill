package com.example.splitbill.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.ProfileRepository
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.api.ProfileResponse
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeGroupItem(
  val group: GroupResponse,
  val members: List<MemberResponse> = emptyList(),
  val bannerUrl: String? = null
)

sealed interface HomeUiState {
  object Loading : HomeUiState
  data class Success(
    val userProfile: ProfileResponse? = null,
    val totalToReceive: Double = 0.0,
    val totalToPay: Double = 0.0,
    val countToReceive: Int = 0,
    val countToPay: Int = 0,
    val recentGroups: List<HomeGroupItem> = emptyList(),
    val recentBills: List<BillResponse> = emptyList(),
    val currentUserId: String? = null
  ) : HomeUiState
  data class Error(val message: String) : HomeUiState
}

class HomeScreenViewModel(
  private val groupRepository: GroupRepository,
  private val billRepository: BillRepository,
  private val profileRepository: ProfileRepository,
  private val statsRepository: StatsRepository,
  private val tokenManager: TokenManager
) : ViewModel() {

  companion object {
    private var cachedState: HomeUiState.Success? = null
    fun clearCache() {
      cachedState = null
    }
  }

  private val _uiState = MutableStateFlow<HomeUiState>(cachedState ?: HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    loadHomeData()
  }

  fun loadHomeData(onComplete: (() -> Unit)? = null) {
    if (_uiState.value !is HomeUiState.Success && cachedState == null) {
      _uiState.value = HomeUiState.Loading
    }
    viewModelScope.launch {
      try {
        val cachedToken = tokenManager.getCachedToken()
        if (cachedToken.isNullOrBlank()) {
          tokenManager.deleteToken()
          return@launch
        }

        // Đảm bảo session hợp lệ với Supabase
        val sessionValid = com.example.splitbill.data.supabase.SupabaseConfig.ensureSession(cachedToken, tokenManager)
        val currentUserId = com.example.splitbill.data.supabase.SupabaseConfig.client.auth.currentUserOrNull()?.id ?: tokenManager.getUserId()
        if (!sessionValid || currentUserId == null || com.example.splitbill.data.supabase.SupabaseConfig.client.auth.currentSessionOrNull() == null) {
          tokenManager.deleteToken()
          return@launch
        }

        // Tải song song profile, nhóm, thống kê và hóa đơn gần đây
        val profileDeferred = async { profileRepository.getMyProfile() }
        val groupsDeferred = async { groupRepository.getGroups() }
        val statsDeferred = async { statsRepository.getUserStats() }
        val recentBillsDeferred = async { billRepository.getRecentBills(limit = 10) }

        val profileResult = profileDeferred.await()
        val groupsResult = groupsDeferred.await()
        val statsResult = statsDeferred.await()
        val recentBillsResult = recentBillsDeferred.await()

        // Kiểm tra lỗi xác thực / hết hạn phiên từ bất kỳ API nào
        val isAuthError = listOf(profileResult, groupsResult, statsResult, recentBillsResult).any {
          it.isFailure && com.example.splitbill.data.supabase.SupabaseConfig.isAuthError(it.exceptionOrNull())
        }
        if (isAuthError) {
          tokenManager.deleteToken()
          return@launch
        }

        val profile = profileResult.getOrNull()
        val groups = groupsResult.getOrDefault(emptyList())
        val stats = statsResult.getOrNull()
        val recentBills = recentBillsResult.getOrDefault(emptyList())

        // Map nhóm trực tiếp (không gọi getMembers tuần tự lặp lại gây nghẽn skeleton)
        val recentGroupsWithMembers = groups.take(6).map { group ->
          HomeGroupItem(
            group = group,
            members = emptyList(),
            bannerUrl = group.bannerUrl
          )
        }

        // Đếm số lượng khoản nợ từ recentBills hoặc ước lượng từ stats
        val countToPay = recentBills.count { bill ->
          bill.paidByUserId != currentUserId && bill.splits.any { it.userId == currentUserId }
        }
        val countToReceive = recentBills.count { bill ->
          bill.paidByUserId == currentUserId && bill.splits.any { it.userId != currentUserId }
        }

        val fallbackUsername = TokenManager.getUsernameFromToken(cachedToken) 
          ?: com.example.splitbill.data.supabase.SupabaseConfig.client.auth.currentUserOrNull()?.email?.substringBefore('@')
          ?: "Bạn"

        val effectiveProfile = profile ?: com.example.splitbill.data.api.ProfileResponse(
          id = currentUserId,
          username = fallbackUsername,
          email = "",
          avatarUrl = null
        )

        val successState = HomeUiState.Success(
          userProfile = effectiveProfile,
          totalToReceive = stats?.totalOthersOweToMe ?: 0.0,
          totalToPay = stats?.totalOwedToOthers ?: 0.0,
          countToReceive = if (countToReceive > 0) countToReceive else if ((stats?.totalOthersOweToMe ?: 0.0) > 0) 1 else 0,
          countToPay = if (countToPay > 0) countToPay else if ((stats?.totalOwedToOthers ?: 0.0) > 0) 1 else 0,
          recentGroups = recentGroupsWithMembers,
          recentBills = recentBills,
          currentUserId = currentUserId
        )
        cachedState = successState
        _uiState.value = successState
      } catch (e: Exception) {
        if (com.example.splitbill.data.supabase.SupabaseConfig.isAuthError(e)) {
          tokenManager.deleteToken()
        } else {
          _uiState.value = HomeUiState.Error(e.message?.ifBlank { "Không thể tải dữ liệu trang chủ" } ?: "Không thể tải dữ liệu trang chủ")
        }
      } finally {
        onComplete?.invoke()
      }
    }
  }
}
