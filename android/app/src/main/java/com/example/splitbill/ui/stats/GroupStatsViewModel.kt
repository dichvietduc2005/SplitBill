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
