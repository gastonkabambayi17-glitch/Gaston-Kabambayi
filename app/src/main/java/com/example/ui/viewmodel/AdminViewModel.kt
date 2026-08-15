package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ReportEntity
import com.example.data.local.UserEntity
import com.example.data.repository.GastonLoveRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReportWithUsers(
    val report: ReportEntity,
    val reportedUser: UserEntity?,
    val reporterUser: UserEntity?
)

data class AdminUiState(
    val totalUsers: Int = 0,
    val totalMatches: Int = 0,
    val totalMessages: Int = 0,
    val pendingReportsCount: Int = 0,
    val allUsers: List<UserEntity> = emptyList(),
    val filteredUsers: List<UserEntity> = emptyList(),
    val searchQuery: String = "",
    val filterStatus: String = "TOUS", // "TOUS", "VERIFIES", "SUSPENDUS"
    val reports: List<ReportWithUsers> = emptyList(),
    val message: String? = null
)

class AdminViewModel(private val repository: GastonLoveRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        observeUsers()
        observeReports()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun loadStats() {
        viewModelScope.launch {
            repository.getTotalUsersCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalUsers = count)
            }
        }
        viewModelScope.launch {
            repository.getTotalMatchesCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalMatches = count)
            }
        }
        viewModelScope.launch {
            repository.getTotalMessagesCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalMessages = count)
            }
        }
        viewModelScope.launch {
            repository.getPendingReportsCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingReportsCount = count)
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collect { users ->
                _uiState.value = _uiState.value.copy(
                    allUsers = users,
                    filteredUsers = filterUserList(users, _uiState.value.searchQuery, _uiState.value.filterStatus)
                )
            }
        }
    }

    private fun observeReports() {
        viewModelScope.launch {
            repository.getAllReports().collect { reportsList ->
                val list = reportsList.map { report ->
                    val reported = repository.getUserById(report.reportedUserId)
                    val reporter = repository.getUserById(report.reporterUserId)
                    ReportWithUsers(report, reported, reporter)
                }
                _uiState.value = _uiState.value.copy(reports = list)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredUsers = filterUserList(_uiState.value.allUsers, query, _uiState.value.filterStatus)
        )
    }

    fun setFilterStatus(status: String) {
        _uiState.value = _uiState.value.copy(
            filterStatus = status,
            filteredUsers = filterUserList(_uiState.value.allUsers, _uiState.value.searchQuery, status)
        )
    }

    private fun filterUserList(users: List<UserEntity>, query: String, status: String): List<UserEntity> {
        return users.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.fullName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.city.contains(query, ignoreCase = true)
            val matchesStatus = when (status) {
                "VERIFIES" -> user.isVerified
                "SUSPENDUS" -> user.isBanned
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    fun toggleBanUser(user: UserEntity) {
        viewModelScope.launch {
            val newStatus = !user.isBanned
            repository.setUserBanned(user.id, newStatus)
            val msg = if (newStatus) "Compte ${user.fullName} suspendu." else "Compte ${user.fullName} réactivé."
            _uiState.value = _uiState.value.copy(message = msg)
        }
    }

    fun toggleVerifyUser(user: UserEntity) {
        viewModelScope.launch {
            val newStatus = !user.isVerified
            repository.setUserVerified(user.id, newStatus)
            val msg = if (newStatus) "Badge de vérification accordé à ${user.fullName}." else "Badge retiré."
            _uiState.value = _uiState.value.copy(message = msg)
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user.id)
            _uiState.value = _uiState.value.copy(message = "Utilisateur ${user.fullName} supprimé définitivement.")
        }
    }

    fun updateReport(reportId: Long, status: String) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, status)
            _uiState.value = _uiState.value.copy(message = "Statut du signalement mis à jour : $status")
        }
    }

    fun banReportedUser(report: ReportWithUsers) {
        val reported = report.reportedUser ?: return
        viewModelScope.launch {
            repository.setUserBanned(reported.id, true)
            repository.updateReportStatus(report.report.id, "RESOLVED")
            _uiState.value = _uiState.value.copy(message = "Utilisateur ${reported.fullName} suspendu suite au signalement.")
        }
    }
}

class AdminViewModelFactory(private val repository: GastonLoveRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AdminViewModel(repository) as T
    }
}
