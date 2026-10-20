package com.notey.app

import android.content.Context
import java.util.Calendar
import java.util.UUID

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

object UserProfileValidator {
    /**
     * Validates display name:
     * - Must not be empty or whitespace only
     * - Must be 40 characters or fewer
     * - Must not contain ISO control characters
     * - International Unicode characters are fully supported
     * Returns null if valid, or a descriptive error message.
     */
    fun validateDisplayName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return "Display name cannot be empty"
        }
        if (trimmed.length > 40) {
            return "Display name must be 40 characters or fewer"
        }
        if (trimmed.any { it.isISOControl() }) {
            return "Display name contains invalid characters"
        }
        return null
    }

    /**
     * Generates a time-aware personalized greeting using local device time.
     * E.g. "Good morning, Alex"
     * If name is empty, returns a neutral fallback "Welcome to Notey".
     */
    fun getTimeAwareGreeting(displayName: String): String {
        val trimmed = displayName.trim()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefix = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Welcome back"
        }
        return if (trimmed.isNotBlank()) "$prefix, $trimmed" else "$prefix to Notey"
    }

    /**
     * Generates 1-2 uppercase initials from a display name for the avatar badge.
     */
    fun getInitials(displayName: String): String {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return "N"
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() -> "${parts[0].first().uppercaseChar()}"
            else -> "N"
        }
    }
}

class UserProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("notey_user_profile", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "profile_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_AVATAR_URI = "avatar_uri"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_PROFILE_CONFIGURED = "profile_configured"
    }

    fun hasProfile(): Boolean {
        val isConfigured = prefs.getBoolean(KEY_PROFILE_CONFIGURED, false)
        val name = prefs.getString(KEY_DISPLAY_NAME, null)
        return isConfigured && !name.isNullOrBlank()
    }

    fun getProfile(): UserProfile? {
        if (!hasProfile()) return null
        return UserProfile(
            id = prefs.getString(KEY_ID, UUID.randomUUID().toString()) ?: UUID.randomUUID().toString(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            avatarUri = prefs.getString(KEY_AVATAR_URI, null),
            createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis()),
            updatedAt = prefs.getLong(KEY_UPDATED_AT, System.currentTimeMillis())
        )
    }

    fun saveProfile(profile: UserProfile): Boolean {
        val error = UserProfileValidator.validateDisplayName(profile.displayName)
        if (error != null) return false

        return prefs.edit()
            .putString(KEY_ID, profile.id)
            .putString(KEY_DISPLAY_NAME, profile.displayName.trim())
            .putString(KEY_AVATAR_URI, profile.avatarUri)
            .putLong(KEY_CREATED_AT, profile.createdAt)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .putBoolean(KEY_PROFILE_CONFIGURED, true)
            .commit() // Use synchronous commit to prevent race conditions during onboarding navigation
    }

    fun clearProfile() {
        prefs.edit().clear().commit()
    }
}
