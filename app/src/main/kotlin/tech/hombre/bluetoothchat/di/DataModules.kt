package tech.hombre.bluetoothchat.di

import tech.hombre.bluetoothchat.data.database.Database
import tech.hombre.bluetoothchat.data.model.*
import tech.hombre.bluetoothchat.ui.view.NotificationView
import tech.hombre.bluetoothchat.ui.view.NotificationViewImpl
import tech.hombre.bluetoothchat.ui.viewmodel.converter.ChatMessageConverter
import tech.hombre.bluetoothchat.ui.viewmodel.converter.ContactConverter
import tech.hombre.bluetoothchat.ui.viewmodel.converter.ConversationConverter
import tech.hombre.bluetoothchat.ui.widget.ShortcutManager
import tech.hombre.bluetoothchat.ui.widget.ShortcutManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

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

const val localeScope = "locale_scope"

val viewModule = module {
    single { NotificationViewImpl(androidContext()) as NotificationView }
    single { ShortcutManagerImpl(androidContext()) as ShortcutManager }
    scope(named(localeScope)) { scoped(qualifier = named(localeScope)) { ContactConverter() } }
    scope(named(localeScope)) { scoped(qualifier = named(localeScope)) { ConversationConverter(androidContext()) } }
    scope(named(localeScope)) { scoped(qualifier = named(localeScope)) { ChatMessageConverter(androidContext()) } }
}
