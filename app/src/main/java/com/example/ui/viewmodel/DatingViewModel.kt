package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.GastonLoveRepository
import com.example.data.repository.MatchResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MatchWithUser(
    val match: MatchEntity,
    val otherUser: UserEntity,
    val lastMessage: MessageEntity? = null,
    val unreadCount: Int = 0
)

data class LikeWithUser(
    val like: LikeEntity,
    val fromUser: UserEntity
)

data class DatingUiState(
    val discoverProfiles: List<UserEntity> = emptyList(),
    val filteredProfiles: List<UserEntity> = emptyList(),
    val matches: List<MatchWithUser> = emptyList(),
    val likesReceived: List<LikeWithUser> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadNotifCount: Int = 0,
    val unreadMessageCount: Int = 0,
    val activeMatchDialog: MatchResult? = null,
    val activeChatMatch: MatchWithUser? = null,
    val chatMessages: List<MessageEntity> = emptyList(),
    // Filter State
    val filterMinAge: Int = 18,
    val filterMaxAge: Int = 65,
    val filterGender: String = "Tous",
    val filterCity: String = "",
    val filterInterest: String = "",
    val isFilterSheetOpen: Boolean = false,
    val isReportDialogOpen: Boolean = false,
    val userToReport: UserEntity? = null,
    val toastMessage: String? = null
)

class DatingViewModel(
    private val repository: GastonLoveRepository,
    initialUserId: Long = 1L
) : ViewModel() {

    private var currentUserId: Long = initialUserId

    private val _uiState = MutableStateFlow(DatingUiState())
    val uiState: StateFlow<DatingUiState> = _uiState.asStateFlow()

    init {
        loadDataForUser(currentUserId)
    }

    fun setCurrentUser(user: UserEntity) {
        if (currentUserId != user.id) {
            currentUserId = user.id
            loadDataForUser(user.id)
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    private fun loadDataForUser(userId: Long) {
        observeDiscoverProfiles(userId)
        observeMatches(userId)
        observeLikes(userId)
        observeNotifications(userId)
        observeUnreadCounts(userId)
    }

    private fun observeDiscoverProfiles(userId: Long) {
        viewModelScope.launch {
            repository.getDiscoverableUsers(userId).collect { users ->
                _uiState.value = _uiState.value.copy(
                    discoverProfiles = users,
                    filteredProfiles = applyFilters(users)
                )
            }
        }
    }

    private fun observeMatches(userId: Long) {
        viewModelScope.launch {
            repository.getMatchesForUser(userId).collect { matchesList ->
                val matchesWithUser = matchesList.mapNotNull { match ->
                    val otherUserId = if (match.user1Id == userId) match.user2Id else match.user1Id
                    val otherUser = repository.getUserById(otherUserId)
                    if (otherUser != null && !otherUser.isBanned) {
                        val lastMessage = repository.getLastMessageForMatch(match.id)
                        MatchWithUser(
                            match = match,
                            otherUser = otherUser,
                            lastMessage = lastMessage
                        )
                    } else null
                }
                _uiState.value = _uiState.value.copy(matches = matchesWithUser)
            }
        }
    }

    private fun observeLikes(userId: Long) {
        viewModelScope.launch {
            repository.getLikesReceived(userId).collect { likes ->
                val likesWithUser = likes.mapNotNull { like ->
                    val fromUser = repository.getUserById(like.fromUserId)
                    if (fromUser != null && !fromUser.isBanned) {
                        LikeWithUser(like = like, fromUser = fromUser)
                    } else null
                }
                _uiState.value = _uiState.value.copy(likesReceived = likesWithUser)
            }
        }
    }

    private fun observeNotifications(userId: Long) {
        viewModelScope.launch {
            repository.getNotificationsForUser(userId).collect { notifs ->
                _uiState.value = _uiState.value.copy(notifications = notifs)
            }
        }
    }

    private fun observeUnreadCounts(userId: Long) {
        viewModelScope.launch {
            repository.getUnreadNotificationsCount(userId).collect { count ->
                _uiState.value = _uiState.value.copy(unreadNotifCount = count)
            }
        }
        viewModelScope.launch {
            repository.getUnreadMessageCount(userId).collect { count ->
                _uiState.value = _uiState.value.copy(unreadMessageCount = count)
            }
        }
    }

    // --- Filters ---
    fun setFilters(minAge: Int, maxAge: Int, gender: String, city: String, interest: String) {
        _uiState.value = _uiState.value.copy(
            filterMinAge = minAge,
            filterMaxAge = maxAge,
            filterGender = gender,
            filterCity = city,
            filterInterest = interest,
            filteredProfiles = applyFilters(
                _uiState.value.discoverProfiles,
                minAge, maxAge, gender, city, interest
            )
        )
    }

    fun resetFilters() {
        setFilters(18, 65, "Tous", "", "")
    }

    private fun applyFilters(
        list: List<UserEntity>,
        minAge: Int = _uiState.value.filterMinAge,
        maxAge: Int = _uiState.value.filterMaxAge,
        gender: String = _uiState.value.filterGender,
        city: String = _uiState.value.filterCity,
        interest: String = _uiState.value.filterInterest
    ): List<UserEntity> {
        return list.filter { user ->
            val matchesAge = user.age in minAge..maxAge
            val matchesGender = (gender == "Tous") || (user.gender.equals(gender, ignoreCase = true))
            val matchesCity = city.isBlank() || user.city.contains(city, ignoreCase = true)
            val matchesInterest = interest.isBlank() || user.interests.any { it.contains(interest, ignoreCase = true) }
            matchesAge && matchesGender && matchesCity && matchesInterest
        }
    }

    // --- Matching Actions ---
    fun likeUser(toUserId: Long, isSuperLike: Boolean = false) {
        viewModelScope.launch {
            val result = repository.recordLike(currentUserId, toUserId, isSuperLike)
            if (result.isMatch) {
                _uiState.value = _uiState.value.copy(
                    activeMatchDialog = result,
                    toastMessage = "Vous avez un nouveau Match ❤️ !"
                )
            } else {
                val action = if (isSuperLike) "Super Like envoyé ⭐" else "Coup de cœur envoyé ❤️"
                _uiState.value = _uiState.value.copy(toastMessage = action)
            }
        }
    }

    fun passUser(toUserId: Long) {
        viewModelScope.launch {
            repository.recordPass(currentUserId, toUserId)
        }
    }

    fun dismissMatchDialog() {
        _uiState.value = _uiState.value.copy(activeMatchDialog = null)
    }

    // --- Active Chat ---
    fun openChat(match: MatchWithUser) {
        _uiState.value = _uiState.value.copy(activeChatMatch = match)
        viewModelScope.launch {
            repository.markMessagesAsRead(match.match.id, currentUserId)
            repository.getMessagesForMatch(match.match.id).collect { msgs ->
                _uiState.value = _uiState.value.copy(chatMessages = msgs)
            }
        }
    }

    fun closeChat() {
        _uiState.value = _uiState.value.copy(activeChatMatch = null, chatMessages = emptyList())
    }

    fun sendMessage(content: String, mediaAttachment: String? = null) {
        val activeMatch = _uiState.value.activeChatMatch ?: return
        viewModelScope.launch {
            val result = repository.sendMessage(
                matchId = activeMatch.match.id,
                senderId = currentUserId,
                receiverId = activeMatch.otherUser.id,
                content = content,
                mediaAttachmentRes = mediaAttachment
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(toastMessage = error.message)
            }
        }
    }

    fun deleteConversation(matchId: Long) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
            closeChat()
            _uiState.value = _uiState.value.copy(toastMessage = "Conversation supprimée.")
        }
    }

    // --- Safety & Moderation ---
    fun openReportDialog(user: UserEntity) {
        _uiState.value = _uiState.value.copy(isReportDialogOpen = true, userToReport = user)
    }

    fun dismissReportDialog() {
        _uiState.value = _uiState.value.copy(isReportDialogOpen = false, userToReport = null)
    }

    fun submitReport(reason: String, details: String) {
        val user = _uiState.value.userToReport ?: return
        viewModelScope.launch {
            repository.reportUser(
                reportedUserId = user.id,
                reporterUserId = currentUserId,
                reason = reason,
                details = details
            )
            dismissReportDialog()
            closeChat()
            _uiState.value = _uiState.value.copy(
                toastMessage = "Profil signalé et bloqué. Merci pour votre vigilance 🛡️"
            )
        }
    }

    fun blockUser(targetUserId: Long) {
        viewModelScope.launch {
            repository.blockUser(currentUserId, targetUserId)
            closeChat()
            _uiState.value = _uiState.value.copy(toastMessage = "Ce profil a été bloqué.")
        }
    }

    // --- Profile Editing ---
    fun updateProfile(
        bio: String,
        city: String,
        interests: List<String>,
        extraPhotos: List<String>,
        interestedIn: String
    ) {
        viewModelScope.launch {
            val current = repository.getUserById(currentUserId) ?: return@launch
            val updated = current.copy(
                bio = bio.trim(),
                city = city.trim(),
                interests = interests,
                extraPhotos = extraPhotos,
                interestedIn = interestedIn
            )
            repository.updateUser(updated)
            _uiState.value = _uiState.value.copy(toastMessage = "Profil mis à jour avec succès !")
        }
    }

    // --- Notifications ---
    fun markNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(currentUserId)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications(currentUserId)
            _uiState.value = _uiState.value.copy(toastMessage = "Toutes les notifications ont été effacées.")
        }
    }
}

class DatingViewModelFactory(
    private val repository: GastonLoveRepository,
    private val currentUserId: Long = 1L
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DatingViewModel(repository, currentUserId) as T
    }
}
