package tech.hombre.bluetoothchatter.data.model

import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import tech.hombre.bluetoothchatter.R

class UserPreferencesImpl(private val context: Context) : UserPreferences {

    private val keyPreferencesName = "userPreferences"
    private val keyNotificationSound = "notifications_sound"
    private val keyAppearanceChatBgColor = "notifications_chat_bg_color"
    private val keyAppearanceNightMode = "appearance_night_mode"
    private val keyDiscoveryClassification = "discovery_classification"

    private val preferences
            by lazy { context.getSharedPreferences(keyPreferencesName, Context.MODE_PRIVATE) }

    override fun isSoundEnabled() =
            preferences.getBoolean(keyNotificationSound, false)

    override fun isClassificationEnabled() =
            preferences.getBoolean(keyDiscoveryClassification, true)

    override fun getChatBackgroundColor(context: Context) =
            preferences.getInt(keyAppearanceChatBgColor, ContextCompat.getColor(context, R.color.background_chat_default))

    override fun getNightMode() =
            preferences.getInt(keyAppearanceNightMode, AppCompatDelegate.MODE_NIGHT_YES)

    override fun saveChatBgColor(color: Int) {
        preferences.edit()
                .putInt(keyAppearanceChatBgColor, color)
                .apply()
    }

    override fun saveNewSoundPreference(enabled: Boolean) {
        preferences.edit()
                .putBoolean(keyNotificationSound, enabled)
                .apply()
    }

    override fun saveNewClassificationPreference(enabled: Boolean) {
        preferences.edit()
                .putBoolean(keyDiscoveryClassification, enabled)
                .apply()
    }

    override fun saveNightMode(mode: Int) {
        preferences.edit()
                .putInt(keyAppearanceNightMode, mode)
                .apply()
    }
}
