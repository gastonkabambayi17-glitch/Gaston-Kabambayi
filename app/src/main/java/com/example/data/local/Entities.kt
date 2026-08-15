package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(";;;") ?: ""
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";;;")
    }
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val birthDate: String, // YYYY-MM-DD
    val age: Int,
    val gender: String, // "Femme", "Homme", "Non-binaire"
    val interestedIn: String, // "Tous", "Femme", "Homme"
    val city: String,
    val country: String = "France",
    val bio: String,
    val avatarRes: String, // Drawable resource name or avatar identifier
    val extraPhotos: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = true,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUserId: Long,
    val toUserId: Long,
    val isSuperLike: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val user1Id: Long,
    val user2Id: Long,
    val matchedAt: Long = System.currentTimeMillis(),
    val lastInteractionAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val mediaAttachmentRes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val type: String, // "MATCH", "MESSAGE", "LIKE", "SYSTEM", "MODERATION"
    val relatedUserId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportedUserId: Long,
    val reporterUserId: Long,
    val reason: String,
    val details: String = "",
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockerUserId: Long,
    val blockedUserId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "platform_settings")
data class PlatformSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
