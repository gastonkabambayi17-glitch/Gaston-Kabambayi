package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class MatchResult(
    val isMatch: Boolean,
    val matchedUser: UserEntity? = null,
    val matchId: Long? = null
)

class GastonLoveRepository(private val dao: GastonLoveDao) {

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val totalCount = dao.getTotalUsersCount().first()
        if (totalCount == 0) {
            val sampleUsers = listOf(
                UserEntity(
                    email = "admin@gastonlove.com",
                    passwordHash = "admin123",
                    fullName = "Gaston Moderation",
                    birthDate = "1990-05-15",
                    age = 34,
                    gender = "Homme",
                    interestedIn = "Tous",
                    city = "Paris",
                    country = "France",
                    bio = "Compte officiel d'administration et de sécurité Gaston Love.",
                    avatarRes = "img_profile_lucas",
                    extraPhotos = listOf("img_profile_lucas", "img_landing_couple"),
                    interests = listOf("Sécurité", "Technologie", "Modération", "Communauté"),
                    isVerified = true,
                    isAdmin = true,
                    isOnline = true
                ),
                UserEntity(
                    email = "sophie.martin@example.com",
                    passwordHash = "secret123",
                    fullName = "Sophie Martin",
                    birthDate = "1998-04-12",
                    age = 26,
                    gender = "Femme",
                    interestedIn = "Homme",
                    city = "Paris",
                    country = "France",
                    bio = "Passionnée de photographie, de cafés parisiens et de voyages spontanés. J'adore les conversations sincères autour d'un bon vin 🍷✨.",
                    avatarRes = "img_profile_sophie",
                    extraPhotos = listOf("img_profile_sophie", "img_landing_couple"),
                    interests = listOf("Photographie", "Voyage", "Gastronomie", "Art", "Musique"),
                    isVerified = true,
                    isOnline = true
                ),
                UserEntity(
                    email = "lucas.dubois@example.com",
                    passwordHash = "secret123",
                    fullName = "Lucas Dubois",
                    birthDate = "1996-09-20",
                    age = 28,
                    gender = "Homme",
                    interestedIn = "Femme",
                    city = "Lyon",
                    country = "France",
                    bio = "Architecte d'intérieur le jour, chef amateur le soir 🍝. Toujours partant pour une randonnée ou un concert de jazz.",
                    avatarRes = "img_profile_lucas",
                    extraPhotos = listOf("img_profile_lucas", "img_landing_couple"),
                    interests = listOf("Architecture", "Cuisine", "Jazz", "Randonnée", "Design"),
                    isVerified = true,
                    isOnline = true
                ),
                UserEntity(
                    email = "camille.leroy@example.com",
                    passwordHash = "secret123",
                    fullName = "Camille Leroy",
                    birthDate = "1999-11-03",
                    age = 25,
                    gender = "Femme",
                    interestedIn = "Homme",
                    city = "Bordeaux",
                    country = "France",
                    bio = "Amoureuse de littérature, de couchers de soleil sur la plage et de rires partagés. À la recherche de quelqu'un d'authentique et attentionné 🌸.",
                    avatarRes = "img_profile_sophie",
                    extraPhotos = listOf("img_profile_sophie"),
                    interests = listOf("Lecture", "Plage", "Nature", "Cinéma", "Yoga"),
                    isVerified = true,
                    isOnline = false
                ),
                UserEntity(
                    email = "alexandre.bernard@example.com",
                    passwordHash = "secret123",
                    fullName = "Alexandre Bernard",
                    birthDate = "1994-07-14",
                    age = 30,
                    gender = "Homme",
                    interestedIn = "Femme",
                    city = "Marseille",
                    country = "France",
                    bio = "Entrepreneur passionné de sports nautiques et de découvertes culturelles. Vivre chaque jour avec passion et bienveillance ☀️⛵.",
                    avatarRes = "img_profile_lucas",
                    extraPhotos = listOf("img_profile_lucas"),
                    interests = listOf("Voile", "Sport", "Entrepreneuriat", "Voyage", "Cuisine"),
                    isVerified = false,
                    isOnline = true
                ),
                UserEntity(
                    email = "lea.moreau@example.com",
                    passwordHash = "secret123",
                    fullName = "Léa Moreau",
                    birthDate = "2000-02-18",
                    age = 24,
                    gender = "Femme",
                    interestedIn = "Tous",
                    city = "Toulouse",
                    country = "France",
                    bio = "Graphiste & créative. J'aime les musées, les playlists indie et cuisiner pour mes amis. Partante pour un café ?",
                    avatarRes = "img_profile_sophie",
                    extraPhotos = listOf("img_profile_sophie"),
                    interests = listOf("Design", "Musées", "Musique", "Café", "Mode"),
                    isVerified = true,
                    isOnline = true
                )
            )

            dao.insertUsers(sampleUsers)

            // Make Sophie like the second user for instant match testing
            val insertedSophie = dao.getUserByEmail("sophie.martin@example.com")
            val insertedLucas = dao.getUserByEmail("lucas.dubois@example.com")
            if (insertedSophie != null && insertedLucas != null) {
                dao.insertLike(
                    LikeEntity(
                        fromUserId = insertedSophie.id,
                        toUserId = insertedLucas.id,
                        isSuperLike = true
                    )
                )
            }
        }
    }

    // --- Authentication & User Management ---
    suspend fun registerUser(
        email: String,
        passwordHash: String,
        fullName: String,
        birthDate: String,
        age: Int,
        gender: String,
        interestedIn: String,
        city: String,
        bio: String,
        avatarRes: String,
        interests: List<String>
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getUserByEmail(email.trim())
        if (existing != null) {
            return@withContext Result.failure(Exception("Un compte existe déjà avec cette adresse e-mail."))
        }
        if (age < 18) {
            return@withContext Result.failure(Exception("Vous devez avoir au moins 18 ans pour vous inscrire."))
        }

        val newUser = UserEntity(
            email = email.trim().lowercase(),
            passwordHash = passwordHash,
            fullName = fullName.trim(),
            birthDate = birthDate,
            age = age,
            gender = gender,
            interestedIn = interestedIn,
            city = city.trim(),
            bio = bio.trim(),
            avatarRes = avatarRes.ifBlank { "img_profile_sophie" },
            extraPhotos = listOf(avatarRes.ifBlank { "img_profile_sophie" }),
            interests = interests,
            isVerified = false,
            isOnline = true
        )
        val id = dao.insertUser(newUser)
        val created = dao.getUserById(id)
        if (created != null) {
            // Welcome notification
            dao.insertNotification(
                NotificationEntity(
                    userId = created.id,
                    title = "Bienvenue sur Gaston Love ❤️",
                    message = "Félicitations pour votre inscription ! Découvrez dès maintenant des profils qui vous correspondent.",
                    type = "SYSTEM"
                )
            )
            Result.success(created)
        } else {
            Result.failure(Exception("Erreur lors de la création du compte."))
        }
    }

    suspend fun loginUser(email: String, passwordHash: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(email.trim().lowercase())
        if (user == null) {
            return@withContext Result.failure(Exception("E-mail ou mot de passe incorrect."))
        }
        if (user.passwordHash != passwordHash) {
            return@withContext Result.failure(Exception("E-mail ou mot de passe incorrect."))
        }
        if (user.isBanned) {
            return@withContext Result.failure(Exception("Ce compte a été suspendu par l'administration pour non-respect de la charte."))
        }

        dao.updateUser(user.copy(isOnline = true, lastActiveTime = System.currentTimeMillis()))
        Result.success(user)
    }

    suspend fun getUserById(userId: Long): UserEntity? = dao.getUserById(userId)

    suspend fun getOrCreateFirebaseUser(
        email: String,
        displayName: String?,
        photoUrl: String? = null
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            if (existing.isBanned) {
                return@withContext Result.failure(Exception("Ce compte a été suspendu par l'administration."))
            }
            dao.updateUser(existing.copy(isOnline = true, lastActiveTime = System.currentTimeMillis()))
            return@withContext Result.success(existing)
        }

        val name = displayName?.ifBlank { null } ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val newUser = UserEntity(
            email = cleanEmail,
            passwordHash = "firebase_auth_user",
            fullName = name,
            birthDate = "1998-01-01",
            age = 26,
            gender = "Femme",
            interestedIn = "Tous",
            city = "Paris",
            country = "France",
            bio = "Nouveau membre connecté via Firebase ❤️",
            avatarRes = "img_profile_sophie",
            extraPhotos = listOf("img_profile_sophie"),
            interests = listOf("Rencontres", "Discussions", "Voyage"),
            isVerified = true,
            isOnline = true
        )
        val id = dao.insertUser(newUser)
        val created = dao.getUserById(id)
        if (created != null) {
            dao.insertNotification(
                NotificationEntity(
                    userId = created.id,
                    title = "Bienvenue sur Gaston Love ❤️",
                    message = "Compte Firebase connecté avec succès ! Complétez votre profil pour maximiser vos matchs.",
                    type = "SYSTEM"
                )
            )
            Result.success(created)
        } else {
            Result.failure(Exception("Erreur lors de la création du profil utilisateur."))
        }
    }

    fun observeUser(userId: Long): Flow<UserEntity?> = dao.observeUserById(userId)

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.updateUser(user)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(email.trim().lowercase())
        if (user == null) {
            return@withContext Result.failure(Exception("Aucun compte associé à cette adresse e-mail."))
        }
        dao.updateUser(user.copy(passwordHash = newPassword))
        Result.success(Unit)
    }

    // --- Discovery & Matching ---
    fun getDiscoverableUsers(currentUserId: Long): Flow<List<UserEntity>> =
        dao.getDiscoverableUsers(currentUserId)

    suspend fun recordLike(fromUserId: Long, toUserId: Long, isSuperLike: Boolean): MatchResult = withContext(Dispatchers.IO) {
        val fromUser = dao.getUserById(fromUserId)
        val toUser = dao.getUserById(toUserId) ?: return@withContext MatchResult(isMatch = false)

        dao.insertLike(
            LikeEntity(
                fromUserId = fromUserId,
                toUserId = toUserId,
                isSuperLike = isSuperLike
            )
        )

        // Check if reciprocal like exists
        val reciprocal = dao.getLike(fromUserId = toUserId, toUserId = fromUserId)
        if (reciprocal != null) {
            // Check if match already recorded
            var match = dao.getMatchBetween(fromUserId, toUserId)
            val matchId = if (match == null) {
                val newMatch = MatchEntity(
                    user1Id = minOf(fromUserId, toUserId),
                    user2Id = maxOf(fromUserId, toUserId),
                    matchedAt = System.currentTimeMillis(),
                    lastInteractionAt = System.currentTimeMillis()
                )
                dao.insertMatch(newMatch)
            } else {
                match.id
            }

            // Notifications for both
            dao.insertNotification(
                NotificationEntity(
                    userId = fromUserId,
                    title = "C'est un Match ❤️ !",
                    message = "Vous avez matché avec ${toUser.fullName} ! Envoyez le premier message dès maintenant.",
                    type = "MATCH",
                    relatedUserId = toUserId
                )
            )
            if (fromUser != null) {
                dao.insertNotification(
                    NotificationEntity(
                        userId = toUserId,
                        title = "C'est un Match ❤️ !",
                        message = "Vous avez matché avec ${fromUser.fullName} ! Envoyez le premier message dès maintenant.",
                        type = "MATCH",
                        relatedUserId = fromUserId
                    )
                )
            }

            return@withContext MatchResult(
                isMatch = true,
                matchedUser = toUser,
                matchId = matchId
            )
        } else {
            // Send like notification to toUser
            if (fromUser != null) {
                val actionText = if (isSuperLike) "vous a envoyé un Super Like ⭐ !" else "a aimé votre profil ❤️ !"
                dao.insertNotification(
                    NotificationEntity(
                        userId = toUserId,
                        title = if (isSuperLike) "Nouveau Super Like ⭐" else "Nouveau Like ❤️",
                        message = "${fromUser.fullName} $actionText",
                        type = "LIKE",
                        relatedUserId = fromUserId
                    )
                )
            }
            return@withContext MatchResult(isMatch = false)
        }
    }

    suspend fun recordPass(fromUserId: Long, toUserId: Long) = withContext(Dispatchers.IO) {
        // Record pass as a non-matching interaction in like table with negative flag if needed, or by simply inserting standard like entry to not show again
        dao.insertLike(
            LikeEntity(
                fromUserId = fromUserId,
                toUserId = toUserId,
                isSuperLike = false
            )
        )
    }

    fun getMatchesForUser(userId: Long): Flow<List<MatchEntity>> = dao.getMatchesForUser(userId)

    fun getLikesReceived(userId: Long): Flow<List<LikeEntity>> = dao.getLikesReceived(userId)

    suspend fun getMatchById(matchId: Long): MatchEntity? = dao.getMatchById(matchId)

    suspend fun deleteMatch(matchId: Long) = withContext(Dispatchers.IO) {
        dao.deleteMessagesForMatch(matchId)
        dao.deleteMatch(matchId)
    }

    // --- Messaging ---
    fun getMessagesForMatch(matchId: Long): Flow<List<MessageEntity>> = dao.getMessagesForMatch(matchId)

    suspend fun getLastMessageForMatch(matchId: Long): MessageEntity? = dao.getLastMessageForMatch(matchId)

    suspend fun sendMessage(
        matchId: Long,
        senderId: Long,
        receiverId: Long,
        content: String,
        mediaAttachmentRes: String? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        if (content.isBlank() && mediaAttachmentRes == null) {
            return@withContext Result.failure(Exception("Le message ne peut pas être vide."))
        }

        // Check if blocked
        if (dao.isBlocked(receiverId, senderId) || dao.isBlocked(senderId, receiverId)) {
            return@withContext Result.failure(Exception("Impossible d'envoyer le message : cet utilisateur a été bloqué."))
        }

        val message = MessageEntity(
            matchId = matchId,
            senderId = senderId,
            receiverId = receiverId,
            content = content.trim(),
            mediaAttachmentRes = mediaAttachmentRes,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        val id = dao.insertMessage(message)
        dao.updateMatchInteraction(matchId, System.currentTimeMillis())

        val sender = dao.getUserById(senderId)
        if (sender != null) {
            dao.insertNotification(
                NotificationEntity(
                    userId = receiverId,
                    title = "Message de ${sender.fullName} 💬",
                    message = if (content.isNotBlank()) content else "Photo partagée 📷",
                    type = "MESSAGE",
                    relatedUserId = senderId
                )
            )
        }

        Result.success(message.copy(id = id))
    }

    suspend fun markMessagesAsRead(matchId: Long, userId: Long) = withContext(Dispatchers.IO) {
        dao.markMessagesAsRead(matchId, userId)
    }

    fun getUnreadMessageCount(userId: Long): Flow<Int> = dao.getUnreadMessageCount(userId)

    // --- Notifications ---
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>> =
        dao.getNotificationsForUser(userId)

    fun getUnreadNotificationsCount(userId: Long): Flow<Int> = dao.getUnreadNotificationsCount(userId)

    suspend fun markNotificationAsRead(notificationId: Long) = withContext(Dispatchers.IO) {
        dao.markNotificationAsRead(notificationId)
    }

    suspend fun markAllNotificationsAsRead(userId: Long) = withContext(Dispatchers.IO) {
        dao.markAllNotificationsAsRead(userId)
    }

    suspend fun clearNotifications(userId: Long) = withContext(Dispatchers.IO) {
        dao.clearAllNotifications(userId)
    }

    // --- Safety, Reports & Blocking ---
    suspend fun reportUser(
        reportedUserId: Long,
        reporterUserId: Long,
        reason: String,
        details: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        dao.insertReport(
            ReportEntity(
                reportedUserId = reportedUserId,
                reporterUserId = reporterUserId,
                reason = reason,
                details = details,
                status = "PENDING"
            )
        )
        // Automatically block reported user for safety
        dao.insertBlockedUser(
            BlockedUserEntity(
                blockerUserId = reporterUserId,
                blockedUserId = reportedUserId
            )
        )
        dao.insertNotification(
            NotificationEntity(
                userId = reporterUserId,
                title = "Signalement pris en compte 🛡️",
                message = "Merci d'aider à préserver la sécurité de la communauté Gaston Love. Notre équipe examine ce profil.",
                type = "MODERATION"
            )
        )
        Result.success(Unit)
    }

    suspend fun blockUser(blockerUserId: Long, blockedUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        dao.insertBlockedUser(
            BlockedUserEntity(
                blockerUserId = blockerUserId,
                blockedUserId = blockedUserId
            )
        )
        Result.success(Unit)
    }

    suspend fun unblockUser(blockerUserId: Long, blockedUserId: Long) = withContext(Dispatchers.IO) {
        dao.unblockUser(blockerUserId, blockedUserId)
    }

    fun getBlockedUsersForUser(userId: Long): Flow<List<BlockedUserEntity>> =
        dao.getBlockedUsersForUser(userId)

    // --- Admin Operations ---
    fun getAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()
    fun getAllReports(): Flow<List<ReportEntity>> = dao.getAllReports()
    fun getTotalUsersCount(): Flow<Int> = dao.getTotalUsersCount()
    fun getTotalMatchesCount(): Flow<Int> = dao.getTotalMatchesCount()
    fun getTotalMessagesCount(): Flow<Int> = dao.getTotalMessagesCount()
    fun getPendingReportsCount(): Flow<Int> = dao.getPendingReportsCount()

    suspend fun setUserBanned(userId: Long, isBanned: Boolean) = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext
        dao.updateUser(user.copy(isBanned = isBanned))
    }

    suspend fun setUserVerified(userId: Long, isVerified: Boolean) = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext
        dao.updateUser(user.copy(isVerified = isVerified))
    }

    suspend fun deleteUser(userId: Long) = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext
        dao.deleteUser(user)
    }

    suspend fun updateReportStatus(reportId: Long, status: String) = withContext(Dispatchers.IO) {
        dao.updateReportStatus(reportId, status)
    }
}
