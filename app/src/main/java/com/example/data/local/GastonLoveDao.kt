package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GastonLoveDao {

    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("""
        SELECT * FROM users 
        WHERE id != :currentUserId 
          AND isBanned = 0 
          AND id NOT IN (SELECT blockedUserId FROM blocked_users WHERE blockerUserId = :currentUserId)
          AND id NOT IN (SELECT blockerUserId FROM blocked_users WHERE blockedUserId = :currentUserId)
          AND id NOT IN (SELECT toUserId FROM likes WHERE fromUserId = :currentUserId)
        ORDER BY lastActiveTime DESC
    """)
    fun getDiscoverableUsers(currentUserId: Long): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    fun getTotalUsersCount(): Flow<Int>

    // --- Likes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity): Long

    @Query("SELECT * FROM likes WHERE fromUserId = :fromUserId AND toUserId = :toUserId LIMIT 1")
    suspend fun getLike(fromUserId: Long, toUserId: Long): LikeEntity?

    @Query("SELECT * FROM likes WHERE toUserId = :userId ORDER BY timestamp DESC")
    fun getLikesReceived(userId: Long): Flow<List<LikeEntity>>

    // --- Matches ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Query("""
        SELECT * FROM matches 
        WHERE user1Id = :userId OR user2Id = :userId 
        ORDER BY lastInteractionAt DESC
    """)
    fun getMatchesForUser(userId: Long): Flow<List<MatchEntity>>

    @Query("""
        SELECT * FROM matches 
        WHERE (user1Id = :user1Id AND user2Id = :user2Id) 
           OR (user1Id = :user2Id AND user2Id = :user1Id) 
        LIMIT 1
    """)
    suspend fun getMatchBetween(user1Id: Long, user2Id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Long): MatchEntity?

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: Long)

    @Query("UPDATE matches SET lastInteractionAt = :timestamp WHERE id = :matchId")
    suspend fun updateMatchInteraction(matchId: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM matches")
    fun getTotalMatchesCount(): Flow<Int>

    // --- Messages ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getMessagesForMatch(matchId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE matchId = :matchId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForMatch(matchId: Long): MessageEntity?

    @Query("UPDATE messages SET isRead = 1 WHERE matchId = :matchId AND receiverId = :userId")
    suspend fun markMessagesAsRead(matchId: Long, userId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadMessageCount(userId: Long): Flow<Int>

    @Query("DELETE FROM messages WHERE matchId = :matchId")
    suspend fun deleteMessagesForMatch(matchId: Long)

    @Query("SELECT COUNT(*) FROM messages")
    fun getTotalMessagesCount(): Flow<Int>

    // --- Notifications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadNotificationsCount(userId: Long): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: Long)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearAllNotifications(userId: Long)

    // --- Reports ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: Long, status: String)

    @Query("SELECT COUNT(*) FROM reports WHERE status = 'PENDING'")
    fun getPendingReportsCount(): Flow<Int>

    // --- Blocked Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(block: BlockedUserEntity): Long

    @Query("SELECT * FROM blocked_users WHERE blockerUserId = :userId")
    fun getBlockedUsersForUser(userId: Long): Flow<List<BlockedUserEntity>>

    @Query("DELETE FROM blocked_users WHERE blockerUserId = :blockerId AND blockedUserId = :blockedId")
    suspend fun unblockUser(blockerId: Long, blockedId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_users WHERE blockerUserId = :userA AND blockedUserId = :userB)")
    suspend fun isBlocked(userA: Long, userB: Long): Boolean
}
