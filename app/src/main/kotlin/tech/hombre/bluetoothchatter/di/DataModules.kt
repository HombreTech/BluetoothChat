package tech.hombre.bluetoothchatter.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.hombre.bluetoothchatter.data.database.Database
import tech.hombre.bluetoothchatter.data.model.*
import tech.hombre.bluetoothchatter.data.recorder.RecorderController
import tech.hombre.bluetoothchatter.data.recorder.RecorderControllerImpl
import tech.hombre.bluetoothchatter.ui.view.NotificationView
import tech.hombre.bluetoothchatter.ui.view.NotificationViewImpl
import tech.hombre.bluetoothchatter.ui.viewmodel.converter.ChatMessageConverter
import tech.hombre.bluetoothchatter.ui.viewmodel.converter.ContactConverter
import tech.hombre.bluetoothchatter.ui.viewmodel.converter.ConversationConverter
import tech.hombre.bluetoothchatter.ui.widget.ShortcutManager
import tech.hombre.bluetoothchatter.ui.widget.ShortcutManagerImpl

val bluetoothConnectionModule = module {
    single { BluetoothConnectorImpl(androidContext()) as BluetoothConnector }
    factory { BluetoothScannerImpl(androidContext()) as BluetoothScanner }
}

val databaseModule = module {
    single { Database.getInstance(androidContext()) }
    single { MessagesStorageImpl(get()) as MessagesStorage }
    single { ConversationsStorageImpl(get()) as ConversationsStorage }
}

val localStorageModule = module {
    single { FileManagerImpl(androidContext()) as FileManager }
    single { UserPreferencesImpl(androidContext()) as UserPreferences }
    single { ProfileManagerImpl(androidContext()) as ProfileManager }
}

val recorderModule = module {
    factory { RecorderControllerImpl(androidContext()) as RecorderController }
}

const val localeScope = "locale_scope"

val viewModule = module {
    single { NotificationViewImpl(androidContext()) as NotificationView }
    single { ShortcutManagerImpl(androidContext()) as ShortcutManager }
    scope(named(localeScope)) { scoped { ContactConverter() } }
    scope(named(localeScope)) { scoped { ConversationConverter(androidContext()) } }
    scope(named(localeScope)) { scoped { ChatMessageConverter(androidContext()) } }

}
