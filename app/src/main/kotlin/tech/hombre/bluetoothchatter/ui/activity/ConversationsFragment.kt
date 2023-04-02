package tech.hombre.bluetoothchatter.ui.activity

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amulyakhare.textdrawable.TextDrawable
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentConversationsBinding
import tech.hombre.bluetoothchatter.ui.adapter.ConversationsAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ConversationsPresenter
import tech.hombre.bluetoothchatter.ui.view.ConversationsView
import tech.hombre.bluetoothchatter.ui.view.NotificationView
import tech.hombre.bluetoothchatter.ui.viewmodel.ConversationViewModel
import tech.hombre.bluetoothchatter.ui.widget.ActionView
import tech.hombre.bluetoothchatter.ui.widget.SettingsPopup
import tech.hombre.bluetoothchatter.ui.widget.ShortcutManager
import tech.hombre.bluetoothchatter.utils.getFilePath
import tech.hombre.bluetoothchatter.utils.getFirstLetter
import tech.hombre.bluetoothchatter.utils.getNotificationManager
import tech.hombre.bluetoothchatter.utils.navigateWithResult

class ConversationsFragment :
    BaseFragment<FragmentConversationsBinding>(R.layout.fragment_conversations), ConversationsView {

    private val presenter: ConversationsPresenter by inject { parametersOf(this) }
    private val shortcutsManager: ShortcutManager by inject()

    private lateinit var settingsPopup: SettingsPopup
    private lateinit var storagePermissionDialog: AlertDialog

    private val conversationsAdapter = ConversationsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(presenter)

        (requireActivity() as MainActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.contentMain.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.contentMain.rvConversations.adapter = conversationsAdapter

        settingsPopup = SettingsPopup(requireContext()).apply {
            setOnOptionClickListener {
                when (it) {
                    SettingsPopup.Option.PROFILE ->
                        findNavController().navigate(
                            ConversationsFragmentDirections.actionConversationsFragmentToProfileFragment(
                                editMode = true
                            )
                        )
                    SettingsPopup.Option.IMAGES ->
                        findNavController().navigate(
                            ConversationsFragmentDirections.actionConversationsFragmentToReceivedImagesFragment(
                                address = null
                            )
                        )
                    SettingsPopup.Option.SETTINGS ->
                        findNavController().navigate(ConversationsFragmentDirections.actionConversationsFragmentToSettingsFragment())
                    SettingsPopup.Option.ABOUT ->
                        findNavController().navigate(ConversationsFragmentDirections.actionConversationsFragmentToAboutFragment())
                }
            }
        }

        conversationsAdapter.clickListener = {
            findNavController().navigate(
                ConversationsFragmentDirections.actionConversationsFragmentToChatFragment(
                    address = it.address,
                    nickname = it.displayName,
                )
            )
        }
        conversationsAdapter.longClickListener = { conversation, isCurrent ->
            showContextMenu(conversation, isCurrent)
        }

        binding.fabNewConversation.setOnClickListener {
            goToScan()
        }

        binding.contentMain.btnScan.setOnClickListener {
            goToScan()
        }

        binding.llOptions.setOnClickListener {
            settingsPopup.show(it)
        }

        arguments?.let {
            if (it.size() <= 1) return@let
            var textToShare: String? = null
            var fileToShare: String? = null
            val type = it.getString(Intent.EXTRA_MIME_TYPES)
            if (type == "text/plain") {
                textToShare = it.getString(Intent.EXTRA_TEXT)?.trim()
            } else if (type?.startsWith("image/") == true) {
                val imageUri = it.getParcelable(Intent.EXTRA_STREAM) as Uri?
                fileToShare = imageUri?.getFilePath(requireContext())
            }
            if (textToShare != null || fileToShare != null) {
                findNavController().navigate(
                    ConversationsFragmentDirections.actionConversationsFragmentToContactChooserFragment(
                        message = textToShare,
                        filePath = fileToShare
                    )
                )
            }
        }

        storagePermissionDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_storage_permission)
            .setPositiveButton(R.string.general__ok) { _, _ ->
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
            }
            .setNegativeButton(R.string.general__exit) { _, _ -> findNavController().navigateUp() }
            .setCancelable(false)
            .create()

        shortcutsManager.addSearchShortcut()
    }

    private fun goToScan() {
        navigateWithResult(
            ConversationsFragmentDirections.actionConversationsFragmentToScanFragment(),
            action = {
                val device = it.getParcelable<BluetoothDevice>(ScanFragment.EXTRA_BLUETOOTH_DEVICE)

                if (device != null) {
                    findNavController().navigate(
                        ConversationsFragmentDirections.actionConversationsFragmentToChatFragment(
                            address = device.address,
                            nickname = device.name,
                        )
                    )
                }
            })
    }

    private fun showContextMenu(conversation: ConversationViewModel, isCurrent: Boolean) {

        val labels = ArrayList<String>()
        labels.add(getString(R.string.conversations__remove))
        if (isCurrent) {
            labels.add(getString(R.string.general__disconnect))
        }
        if (shortcutsManager.isRequestPinShortcutSupported()) {
            labels.add(getString(R.string.conversations__pin_to_home_screen))
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.conversations__options))
            .setItems(labels.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        confirmRemoval(conversation.address)
                    }
                    1 -> {
                        if (isCurrent) {
                            presenter.disconnect()
                        } else {
                            requestPinShortcut(conversation)
                        }
                    }
                    2 -> {
                        requestPinShortcut(conversation)
                    }
                }
            }
        builder.create().show()
    }

    private fun requestPinShortcut(conversation: ConversationViewModel) {
        shortcutsManager.requestPinConversationShortcut(
            conversation.address, conversation.displayName, conversation.color
        )
    }

    private fun confirmRemoval(address: String) {

        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.conversations__removal_confirmation))
            .setPositiveButton(getString(R.string.general__yes)) { _, _ ->
                presenter.removeConversation(
                    address
                )
            }
            .setNegativeButton(getString(R.string.general__no), null)
            .show()
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED && !storagePermissionDialog.isShowing
        ) {
            storagePermissionDialog.show()
        }
    }

    override fun dismissConversationNotification() {
        requireContext().getNotificationManager()
            .cancel(
                NotificationView.NOTIFICATION_TAG_CONNECTION,
                NotificationView.NOTIFICATION_ID_CONNECTION
            )
    }

    override fun hideActions() {
        binding.contentMain.avActions.visibility = View.GONE
    }

    override fun showNoConversations() {
        binding.fabNewConversation.hide()
        binding.contentMain.rvConversations.visibility = View.GONE
        binding.contentMain.llEmptyHolder.visibility = View.VISIBLE
    }

    override fun showConversations(conversations: List<ConversationViewModel>, connected: String?) {

        binding.fabNewConversation.show()
        binding.contentMain.rvConversations.visibility = View.VISIBLE
        binding.contentMain.llEmptyHolder.visibility = View.GONE

        conversationsAdapter.setData(ArrayList(conversations), connected)
        conversationsAdapter.notifyDataSetChanged()
    }

    override fun showServiceDestroyed() = doIfStarted {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.general__service_lost))
            .setPositiveButton(getString(R.string.general__restart)) { _, _ ->
                presenter.prepareConnection()
                presenter.loadUserProfile()
            }
            .setCancelable(false)
            .show()
    }

    override fun refreshList(connected: String?) {
        conversationsAdapter.setCurrentConversation(connected)
        conversationsAdapter.notifyDataSetChanged()
    }

    override fun notifyAboutConnectedDevice(conversation: ConversationViewModel) {
        binding.contentMain.avActions.setActionsAndShow(getString(
            R.string.conversations__connection_request,
            conversation.displayName,
            conversation.deviceName
        ),
            ActionView.Action(getString(R.string.general__start_chat)) {
                presenter.startChat(
                    conversation
                )
            },
            ActionView.Action(getString(R.string.general__disconnect)) { presenter.rejectConnection() }
        )
    }

    override fun showRejectedNotification(conversation: ConversationViewModel) = doIfStarted {
        AlertDialog.Builder(requireContext())
            .setMessage(
                getString(
                    R.string.conversations__connection_rejected,
                    conversation.displayName, conversation.deviceName
                )
            )
            .setPositiveButton(getString(R.string.general__ok), null)
            .setCancelable(false)
            .show()
    }

    override fun redirectToChat(conversation: ConversationViewModel) {
        findNavController().navigate(
            ConversationsFragmentDirections.actionConversationsFragmentToChatFragment(
                address = conversation.address,
                nickname = conversation.displayName,
            )
        )
    }

    override fun showUserProfile(name: String, color: Int) {
        val drawable = TextDrawable.builder().buildRound(name.getFirstLetter(), color)
        binding.ivAvatar.setImageDrawable(drawable)
        settingsPopup.populateData(name, color)
    }

    override fun removeFromShortcuts(address: String) {
        shortcutsManager.removeConversationShortcut(address)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED && !storagePermissionDialog.isShowing) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !shouldShowRequestPermissionRationale(
                    permissions[0]
                )
            ) {

                AlertDialog.Builder(requireContext())
                    .setMessage(Html.fromHtml(getString(R.string.conversations__storage_permission)))
                    .setPositiveButton(getString(R.string.conversations__permissions_settings)) { _, _ ->

                        val intent = Intent()
                            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setData(Uri.parse("package:${requireContext().packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        startActivity(intent)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                storagePermissionDialog.show()
            }
        }
    }

    companion object {

        private const val REQUEST_STORAGE_PERMISSION = 101
        private const val REQUEST_SCAN = 102

    }
}
