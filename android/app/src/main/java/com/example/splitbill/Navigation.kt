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
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object GroupList : NavKey
@Serializable data class GroupDetail(val groupId: String) : NavKey
@Serializable data class AddBill(val groupId: String, val memberIds: List<String>, val memberNames: List<String>, val memberEmails: List<String>) : NavKey
@Serializable data class DebtSummary(val groupId: String) : NavKey
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
            backStack.add(GroupList)
          }
        )
      }

      entry<GroupList> {
        val groupRepository = GroupRepository(tokenManager)
        val viewModel = viewModel { GroupListViewModel(groupRepository) }
        GroupListScreen(
          viewModel = viewModel,
          onNavigateToGroup = { groupId ->
            backStack.add(GroupDetail(groupId))
          },
          onNavigateToSettings = {
            backStack.add(Settings)
          }
        )
      }

      entry<GroupDetail> { key ->
        val groupRepository = GroupRepository(tokenManager)
        val billRepository = BillRepository(tokenManager)
        val viewModel = viewModel(key = key.groupId) {
          GroupDetailViewModel(key.groupId, groupRepository, billRepository)
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
        AddBillScreen(
          viewModel = viewModel,
          groupId = key.groupId,
          members = members,
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
        val viewModel = viewModel(key = key.groupId) {
          DebtSummaryViewModel(key.groupId, billRepository, profileRepository)
        }
        DebtSummaryScreen(
          viewModel = viewModel,
          onNavigateBack = { backStack.removeLastOrNull() },
          onNavigateToProfile = { backStack.add(Profile) }
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
