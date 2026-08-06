package com.example.splitbill.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.BillRepository
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.api.GroupStatsResponse
import com.example.splitbill.utils.ExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GroupStatsUiState {
  object Loading : GroupStatsUiState
  data class Success(val data: GroupStatsResponse) : GroupStatsUiState
  data class Error(val message: String) : GroupStatsUiState
}

data class CategorySpending(
  val categoryKey: String,
  val totalAmount: Double,
  val percentage: Float
)

class GroupStatsViewModel(
  val groupId: String,
  private val statsRepository: StatsRepository,
  private val billRepository: BillRepository
) : ViewModel() {

  companion object {
    private val stateCache = mutableMapOf<String, GroupStatsUiState>()
  }

  private val _uiState = MutableStateFlow<GroupStatsUiState>(
    stateCache[groupId] ?: GroupStatsUiState.Loading
  )
  val uiState: StateFlow<GroupStatsUiState> = _uiState.asStateFlow()

  private val _categoryBreakdown = MutableStateFlow<List<CategorySpending>>(emptyList())
  val categoryBreakdown: StateFlow<List<CategorySpending>> = _categoryBreakdown.asStateFlow()

  init {
    loadGroupStats()
  }

  fun loadGroupStats() {
    if (_uiState.value !is GroupStatsUiState.Success) {
      _uiState.value = GroupStatsUiState.Loading
    }
    viewModelScope.launch {
      val result = statsRepository.getGroupStats(groupId)
      val newState = if (result.isSuccess) {
        GroupStatsUiState.Success(result.getOrNull()!!)
      } else {
        if (_uiState.value is GroupStatsUiState.Success) {
          _uiState.value
        } else {
          GroupStatsUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải thống kê nhóm")
        }
      }
      _uiState.value = newState
      if (newState is GroupStatsUiState.Success) {
        stateCache[groupId] = newState
      }

      // Calculate Category Breakdown
      val billsResult = billRepository.getBillsForGroup(groupId)
      if (billsResult.isSuccess) {
        val bills = billsResult.getOrNull() ?: emptyList()
        val totalGroupSpent = bills.sumOf { it.totalAmount }
        if (totalGroupSpent > 0) {
          val breakdown = bills.groupBy { it.category }
            .map { (catKey, catBills) ->
              val catTotal = catBills.sumOf { it.totalAmount }
              CategorySpending(
                categoryKey = catKey,
                totalAmount = catTotal,
                percentage = ((catTotal / totalGroupSpent) * 100).toFloat()
              )
            }
            .sortedByDescending { it.totalAmount }
          _categoryBreakdown.value = breakdown
        }
      }
    }
  }

  fun exportPdf(context: Context, groupName: String) {
    viewModelScope.launch {
      val billsResult = billRepository.getBillsForGroup(groupId)
      val bills = billsResult.getOrDefault(emptyList())
      val currentStats = (_uiState.value as? GroupStatsUiState.Success)?.data

      val pdfFile = ExportManager.generatePdf(context, groupName, currentStats, bills)
      ExportManager.shareFile(context, pdfFile, "application/pdf")
    }
  }

  fun exportCsv(context: Context, groupName: String) {
    viewModelScope.launch {
      val billsResult = billRepository.getBillsForGroup(groupId)
      val bills = billsResult.getOrDefault(emptyList())

      val csvFile = ExportManager.generateCsv(context, groupName, bills)
      ExportManager.shareFile(context, csvFile, "text/csv")
    }
  }
}
