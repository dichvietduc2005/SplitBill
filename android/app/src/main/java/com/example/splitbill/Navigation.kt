package com.example.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.splitbill.data.AuthRepository
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.ProfileRepository
import com.example.splitbill.data.SettlementRepository
import com.example.splitbill.data.InviteRepository
import com.example.splitbill.data.SettingsManager
import com.example.splitbill.data.TokenManager
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.ui.auth.LoginScreen
import com.example.splitbill.ui.auth.LoginViewModel
import com.example.splitbill.ui.bill.AddBillScreen
import com.example.splitbill.ui.bill.AddBillViewModel
import com.example.splitbill.ui.debt.DebtSummaryScreen
import com.example.splitbill.ui.debt.DebtSummaryViewModel
import com.example.splitbill.ui.group.GroupDetailScreen
import com.example.splitbill.ui.group.GroupDetailViewModel
import com.example.splitbill.ui.group.GroupListScreen
import com.example.splitbill.ui.group.GroupListViewModel
import com.example.splitbill.ui.profile.ProfileScreen
import com.example.splitbill.ui.profile.ProfileViewModel
import com.example.splitbill.ui.settings.SettingsScreen
import com.example.splitbill.ui.settings.SettingsViewModel
import com.example.splitbill.ui.stats.GroupStatsScreen
import com.example.splitbill.ui.stats.GroupStatsViewModel
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.ui.components.HomeTab
import com.example.splitbill.ui.components.FloatingBottomBar
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data class HomeTabs(val initialTab: HomeTab = HomeTab.Groups) : NavKey
@Serializable data class GroupDetail(val groupId: String) : NavKey
@Serializable data class AddBill(
  val groupId: String, 
  val memberIds: List<String>, 
  val memberNames: List<String>, 
  val memberEmails: List<String>,
  val existingBillJson: String? = null
) : NavKey
@Serializable data class DebtSummary(val groupId: String) : NavKey
@Serializable data class GroupStats(val groupId: String) : NavKey
@Serializable data object Profile : NavKey
@Serializable data object Settings : NavKey

@Composable
fun MainNavigation(settingsManager: SettingsManager) {
  val backStack = rememberNavBackStack(Login)
  val context = LocalContext.current
  val tokenManager = TokenManager(context)

  // Signal để GroupDetailScreen biết cần refresh khi quay lại từ AddBill
  val refreshSignal = remember { mutableIntStateOf(0) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {

      entry<Login> {
        val authRepository = AuthRepository(tokenManager)
        val viewModel = viewModel { LoginViewModel(authRepository) }
        LoginScreen(
          viewModel = viewModel,
          onLoginSuccess = {
            backStack.removeLastOrNull()
            backStack.add(HomeTabs(HomeTab.Groups))
          }
        )
      }

      entry<HomeTabs> { key ->
        HomeTabsScreen(
          initialTab = key.initialTab,
          settingsManager = settingsManager,
          tokenManager = tokenManager,
          onNavigateToGroup = { groupId ->
            backStack.add(GroupDetail(groupId))
          },
          onNavigateToProfile = {
            backStack.add(Profile)
          },
          onLogoutSuccess = {
            backStack.clear()
            backStack.add(Login)
          }
        )
      }

      entry<GroupDetail> { key ->
        val groupRepository = GroupRepository(tokenManager)
        val billRepository = BillRepository(tokenManager)
        val inviteRepository = InviteRepository(tokenManager)
        val viewModel = viewModel(key = key.groupId) {
          GroupDetailViewModel(key.groupId, groupRepository, billRepository, inviteRepository)
        }
        GroupDetailScreen(
          viewModel = viewModel,
          refreshSignal = refreshSignal.intValue,
          onNavigateBack = { backStack.removeLastOrNull() },
          onAddBill = { groupId, members ->
            backStack.add(
              AddBill(
                groupId = groupId,
                memberIds = members.map { it.userId },
                memberNames = members.map { it.username },
                memberEmails = members.map { it.email }
              )
            )
          },
          onViewDebts = { groupId ->
            backStack.add(DebtSummary(groupId))
          },
          onViewStats = { groupId ->
            backStack.add(GroupStats(groupId))
          },
          onEditBill = { groupId, bill, members ->
            val jsonStr = kotlinx.serialization.json.Json.encodeToString(com.example.splitbill.data.api.BillResponse.serializer(), bill)
            backStack.add(
              AddBill(
                groupId = groupId,
                memberIds = members.map { it.userId },
                memberNames = members.map { it.username },
                memberEmails = members.map { it.email },
                existingBillJson = jsonStr
              )
            )
          }
        )
      }

      entry<AddBill> { key ->
        val billRepository = BillRepository(tokenManager)
        val viewModel = viewModel { AddBillViewModel(billRepository) }
        // Reconstruct MemberResponse list from serializable primitives
        val members = key.memberIds.indices.map { i ->
          MemberResponse(
            userId = key.memberIds[i],
            username = key.memberNames[i],
            email = key.memberEmails[i],
            joinedAt = ""
          )
        }
        val existingBill = key.existingBillJson?.let {
          try {
            kotlinx.serialization.json.Json.decodeFromString(com.example.splitbill.data.api.BillResponse.serializer(), it)
          } catch (e: Exception) {
            null
          }
        }
        AddBillScreen(
          viewModel = viewModel,
          groupId = key.groupId,
          members = members,
          existingBill = existingBill,
          onNavigateBack = {
            // Tăng signal → GroupDetailScreen sẽ tự refresh
            refreshSignal.intValue++
            backStack.removeLastOrNull()
          }
        )
      }

      entry<DebtSummary> { key ->
        val billRepository = BillRepository(tokenManager)
        val profileRepository = ProfileRepository(tokenManager)
        val settlementRepository = SettlementRepository(tokenManager)
        val viewModel = viewModel(key = key.groupId) {
          DebtSummaryViewModel(key.groupId, billRepository, profileRepository, settlementRepository)
        }
        DebtSummaryScreen(
          viewModel = viewModel,
          onNavigateBack = { backStack.removeLastOrNull() },
          onNavigateToProfile = { backStack.add(Profile) }
        )
      }

      entry<GroupStats> { key ->
        val statsRepository = StatsRepository(tokenManager)
        val viewModel = viewModel(key = key.groupId) {
          GroupStatsViewModel(key.groupId, statsRepository)
        }
        GroupStatsScreen(
          viewModel = viewModel,
          onNavigateBack = { backStack.removeLastOrNull() }
        )
      }

      entry<Profile> {
        val profileRepository = ProfileRepository(tokenManager)
        val viewModel = viewModel { ProfileViewModel(profileRepository) }
        ProfileScreen(
          viewModel = viewModel,
          onNavigateBack = { backStack.removeLastOrNull() }
        )
      }

      entry<Settings> {
        val authRepository = AuthRepository(tokenManager)
        val profileRepository = ProfileRepository(tokenManager)
        val viewModel = viewModel {
          SettingsViewModel(settingsManager, authRepository, profileRepository)
        }
        SettingsScreen(
          viewModel = viewModel,
          onNavigateToProfile = { backStack.add(Profile) },
          onLogoutSuccess = {
            // Atomic backstack mutation to prevent composition disposal from cancelling navigation!
            backStack.clear()
            backStack.add(Login)
          },
          onNavigateBack = { backStack.removeLastOrNull() }
        )
      }
    }
  )
}

@Composable
fun HomeTabsScreen(
  initialTab: HomeTab,
  settingsManager: SettingsManager,
  tokenManager: TokenManager,
  onNavigateToGroup: (String) -> Unit,
  onNavigateToProfile: () -> Unit,
  onLogoutSuccess: () -> Unit
) {
  var currentTab by remember { mutableStateOf(initialTab) }

  // Xử lý nút Back hệ thống: Nếu ở Tab Profile/Settings thì quay về Tab Groups trước khi thoát
  androidx.activity.compose.BackHandler(enabled = currentTab != HomeTab.Groups) {
    currentTab = HomeTab.Groups
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    bottomBar = {
      FloatingBottomBar(
        selectedTab = currentTab,
        onTabSelected = { currentTab = it }
      )
    },
    modifier = Modifier.fillMaxSize()
  ) { paddingValues ->
    Crossfade(
      targetState = currentTab,
      animationSpec = androidx.compose.animation.core.tween(200),
      label = "tab_transition",
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = paddingValues.calculateBottomPadding() - 16.dp)
    ) { tab ->
      when (tab) {
        HomeTab.Groups -> {
          val groupRepository = GroupRepository(tokenManager)
          val inviteRepository = InviteRepository(tokenManager)
          val viewModel = viewModel { GroupListViewModel(groupRepository, inviteRepository) }
          GroupListScreen(
            viewModel = viewModel,
            onNavigateToGroup = onNavigateToGroup,
            onNavigateToSettings = {},
            modifier = Modifier.fillMaxSize()
          )
        }
        HomeTab.Profile -> {
          val profileRepository = ProfileRepository(tokenManager)
          val viewModel = viewModel { ProfileViewModel(profileRepository) }
          ProfileScreen(
            viewModel = viewModel,
            onNavigateBack = {},
            isTab = true,
            modifier = Modifier.fillMaxSize()
          )
        }
        HomeTab.Settings -> {
          val authRepository = AuthRepository(tokenManager)
          val profileRepository = ProfileRepository(tokenManager)
          val viewModel = viewModel {
            SettingsViewModel(settingsManager, authRepository, profileRepository)
          }
          SettingsScreen(
            viewModel = viewModel,
            onNavigateToProfile = onNavigateToProfile,
            onLogoutSuccess = onLogoutSuccess,
            onNavigateBack = {},
            isTab = true,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
