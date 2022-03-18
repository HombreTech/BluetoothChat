package tech.hombre.bluetoothchatter.ui.widget

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.amulyakhare.textdrawable.TextDrawable
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.ui.activity.MainActivity
import tech.hombre.bluetoothchatter.utils.getBitmap
import tech.hombre.bluetoothchatter.utils.getFirstLetter

class ShortcutManagerImpl(private val context: Context) : ShortcutManager {

    private val idSearch = "id.search"

    private var shortcutManager: android.content.pm.ShortcutManager? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager = context
                .getSystemService(android.content.pm.ShortcutManager::class.java)
        }
    }

    override fun addSearchShortcut() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

            shortcutManager?.let { manager ->

                val isSearchAdded = manager.dynamicShortcuts.asSequence()
                    .filter { it.id == idSearch }
                    .any()

                if (isSearchAdded) {
                    return
                }
            }

            val shortcut = ShortcutInfo.Builder(context, idSearch)
                .setShortLabel(context.getString(R.string.scan__scan))
                .setLongLabel(context.getString(R.string.scan__scan))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_search_black_24dp))
                .setIntent(
                    Intent(
                        Intent.ACTION_VIEW,
                        "bluetoothchatter://scan".toUri(),
                        context,
                        MainActivity::class.java
                    ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                .build()

            shortcutManager?.addDynamicShortcuts(listOf(shortcut))
        }
    }

    override fun addConversationShortcut(address: String, name: String, @ColorInt color: Int) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

            removeLatestIfNeeded(address)

            val shortcut = createConversationShortcut(address, name, color)
            shortcutManager?.addDynamicShortcuts(listOf(shortcut))
        }
    }

    override fun removeConversationShortcut(address: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager?.removeDynamicShortcuts(listOf(address))
        }
    }

    override fun requestPinConversationShortcut(address: String, name: String, color: Int) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcut = createConversationShortcut(address, name, color)
            shortcutManager?.requestPinShortcut(shortcut, null)
        }
    }

    override fun isRequestPinShortcutSupported() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            shortcutManager?.isRequestPinShortcutSupported ?: false
        } else {
            false
        }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createConversationShortcut(
        address: String,
        name: String,
        @ColorInt color: Int
    ): ShortcutInfo {

        val drawable = TextDrawable.builder().buildRound(name.getFirstLetter(), color)

        return ShortcutInfo.Builder(context, address)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(Icon.createWithBitmap(drawable.getBitmap()))
            .setIntents(
                arrayOf(
                    Intent(
                        Intent.ACTION_VIEW,
                        "bluetoothchatter://conversations".toUri(),
                        context,
                        MainActivity::class.java
                    ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    Intent(
                        Intent.ACTION_VIEW,
                        "bluetoothchatter://conversations/$address".toUri(),
                        context,
                        MainActivity::class.java
                    )
                )
            )
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun removeLatestIfNeeded(newShortcutId: String) = shortcutManager?.let { manager ->

        manager.removeDynamicShortcuts(listOf(newShortcutId))

        val conversations = manager.dynamicShortcuts.asSequence()
            .filter { it.id != idSearch }
            .sortedByDescending { it.lastChangedTimestamp }
            .toList()

        if (conversations.size == 2) {
            shortcutManager?.removeDynamicShortcuts(listOf(conversations[1].id))
        }
    }
}
