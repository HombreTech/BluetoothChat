package tech.hombre.bluetoothchatter

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.FirebaseCrashlytics
import tech.hombre.bluetoothchatter.data.model.BluetoothConnector
import tech.hombre.bluetoothchatter.data.model.ProfileManager
import tech.hombre.bluetoothchatter.data.model.UserPreferences
import tech.hombre.bluetoothchatter.di.*
import tech.hombre.bluetoothchatter.ui.activity.ChatActivity
import tech.hombre.bluetoothchatter.ui.activity.ConversationsActivity
import tech.hombre.bluetoothchatter.ui.util.StartStopActivityLifecycleCallbacks
import tech.hombre.bluetoothchatter.ui.util.ThemeHolder
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

class ChatApplication : Application(), LifecycleObserver, ThemeHolder {

    var isConversationsOpened = false
    var currentChat: String? = null

    @NightMode
    private var nightMode: Int = AppCompatDelegate.MODE_NIGHT_NO

    private val connector: BluetoothConnector by inject()
    private val profileManager: ProfileManager by inject()
    private val preferences: UserPreferences by inject()

    private lateinit var localeSession: Scope

    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.DEBUG) FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        startKoin {
            androidContext(this@ChatApplication)
            modules(
                listOf(
                    applicationModule,
                    bluetoothConnectionModule, databaseModule, localStorageModule, viewModule
                )
            )
        }

        localeSession = getKoin().createScope(localeScope, named(localeScope))

        nightMode = preferences.getNightMode()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        registerActivityLifecycleCallbacks(object : StartStopActivityLifecycleCallbacks() {

            override fun onActivityStarted(activity: Activity) {
                when (activity) {
                    is ConversationsActivity -> isConversationsOpened = true
                    is ChatActivity -> currentChat =
                        activity.intent.getStringExtra(ChatActivity.EXTRA_ADDRESS)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                when (activity) {
                    is ConversationsActivity -> isConversationsOpened = false
                    is ChatActivity -> currentChat = null
                }
            }
        })

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)

        if (BuildConfig.DEBUG) {

            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeSession.close()
        localeSession = getKoin().createScope(localeScope, named(localeScope))
    }


    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                if (profileManager.getUserName().isNotEmpty()) {
                    connector.prepare()
                }
            }
            Lifecycle.Event.ON_STOP -> {
                connector.release()
            }
            else -> {}
        }
    }

    override fun setNightMode(@NightMode nightMode: Int) {
        this.nightMode = nightMode
    }

    override fun getNightMode() = nightMode
}
