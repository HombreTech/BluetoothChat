package tech.hombre.bluetoothchatter.ui.presenter

import android.content.Context
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import tech.hombre.bluetoothchatter.data.model.UserPreferences
import tech.hombre.bluetoothchatter.ui.util.ThemeHolder
import tech.hombre.bluetoothchatter.ui.view.SettingsView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsPresenter(private val view: SettingsView,
                        private val preferences: UserPreferences,
                        private val themeHolder: ThemeHolder,
                        private val uiContext: CoroutineDispatcher = Dispatchers.Main,
                        private val bgContext: CoroutineDispatcher = Dispatchers.IO) : BasePresenter(uiContext) {

    @NightMode
    private var initialNightMode: Int = AppCompatDelegate.MODE_NIGHT_NO
    @NightMode
    private var changedNightMode: Int = AppCompatDelegate.MODE_NIGHT_NO

    fun loadPreferences(context: Context) = launch {

        val color = withContext(bgContext) { preferences.getChatBackgroundColor(context) }
        val nightMode = withContext(bgContext) { preferences.getNightMode() }
        val sound = withContext(bgContext) { preferences.isSoundEnabled() }
        val classification = withContext(bgContext) { preferences.isClassificationEnabled() }
        val playerPauseOnMinimize = withContext(bgContext) { preferences.isPlayerPauseOnMinimizeEnabled() }

        initialNightMode = nightMode
        changedNightMode = nightMode

        view.displayBgColorSettings(color)
        view.displayNightModeSettings(nightMode)
        view.displayNotificationSetting(sound)
        view.displayDiscoverySetting(classification)
        view.displayPausePlayerOnHide(playerPauseOnMinimize)
    }

    fun prepareColorPicker(context: Context) {
        view.displayColorPicker(preferences.getChatBackgroundColor(context))
    }

    fun prepareNightModePicker() {
        view.displayNightModePicker(preferences.getNightMode())
    }

    fun onNewColorPicked(@ColorInt color: Int) = launch(bgContext) {
        preferences.saveChatBgColor(color)
        launch(uiContext) {
            view.displayColorPicker(color)
        }
    }

    fun onNewNightModePreference(@NightMode nightMode: Int) = launch(bgContext) {
        preferences.saveNightMode(nightMode)
        launch(uiContext) {
            view.displayNightModeSettings(nightMode)
            themeHolder.setNightMode(nightMode)
            changedNightMode = nightMode
        }
    }

    fun onNewSoundPreference(enabled: Boolean) = launch(bgContext) {
        preferences.saveNewSoundPreference(enabled)
    }

    fun onNewClassificationPreference(enabled: Boolean) = launch(bgContext) {
        preferences.saveNewClassificationPreference(enabled)
    }

    fun onNewPausePlayerPreference(paused: Boolean) = launch(bgContext) {
        preferences.saveNewPausePlayerPreference(paused)
    }

    fun isNightModeChanged() = initialNightMode != changedNightMode
}
