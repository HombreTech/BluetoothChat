package tech.hombre.bluetoothchatter.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.databinding.FragmentChatBinding
import tech.hombre.bluetoothchatter.ui.adapter.ChatAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ChatPresenter
import tech.hombre.bluetoothchatter.ui.util.ScrollAwareBehavior
import tech.hombre.bluetoothchatter.ui.util.SimpleTextWatcher
import tech.hombre.bluetoothchatter.ui.util.getFileType
import tech.hombre.bluetoothchatter.ui.util.getPath
import tech.hombre.bluetoothchatter.ui.view.ChatView
import tech.hombre.bluetoothchatter.ui.view.NotificationView
import tech.hombre.bluetoothchatter.ui.viewmodel.ChatMessageViewModel
import tech.hombre.bluetoothchatter.ui.widget.ActionView
import tech.hombre.bluetoothchatter.ui.widget.SendFilePopup
import tech.hombre.bluetoothchatter.utils.getNotificationManager
import tech.hombre.bluetoothchatter.utils.onEnd
import tech.hombre.bluetoothchatter.utils.toReadableFileSize
import java.io.File
import java.util.*


class ChatFragment : BaseFragment<FragmentChatBinding>(R.layout.fragment_chat), ChatView {

    private val args: ChatFragmentArgs by navArgs()

    private val deviceAddress by lazy { args.address ?: "" }

    private val presenter: ChatPresenter by inject {
        parametersOf(deviceAddress, this)
    }

    private lateinit var chatLayoutManager: LinearLayoutManager

    private lateinit var scrollBehavior: ScrollAwareBehavior
    private lateinit var chatAdapter: ChatAdapter

    private val showAnimation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.anime_fade_slide_in)
    }
    private val hideAnimation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.anime_fade_slide_out).apply {
            onEnd { binding.cvPresharingImageHolder.visibility = View.GONE }
        }
    }

    private lateinit var sendFilePopup: SendFilePopup

    private val easyImage by lazy {
        EasyImage.Builder(requireContext())
            .setChooserTitle(getString(R.string.chat__choose_image))
            .allowMultiple(false)
            .build()
    }

    private var disconnectedDialog: AlertDialog? = null
    private var lostConnectionDialog: AlertDialog? = null

    private val textWatcher = object : SimpleTextWatcher() {

        private var previousText: String? = null

        override fun afterTextChanged(text: String) {

            if (previousText.isNullOrEmpty() && text.isNotEmpty()) {
                binding.vsSendButtons.showNext()
            } else if (!previousText.isNullOrEmpty() && text.isEmpty()) {
                binding.vsSendButtons.showPrevious()
            }
            previousText = text
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        lifecycle.addObserver(presenter)
        setHasOptionsMenu(true)
        (requireActivity() as MainActivity).setSupportActionBar(binding.appBar.tbToolbar)
        binding.appBar.tbToolbar.title =
            if (deviceAddress.isEmpty()) getString(R.string.app_name) else deviceAddress
        binding.appBar.tbToolbar.let {
            it.subtitle = getString(R.string.chat__not_connected)
            it.setTitleTextAppearance(requireContext(), R.style.ActionBar_TitleTextStyle)
            it.setSubtitleTextAppearance(requireContext(), R.style.ActionBar_SubTitleTextStyle)
        }

        sendFilePopup = SendFilePopup(requireContext()).apply {
            setOnOptionClickListener {
                when (it) {
                    SendFilePopup.Option.IMAGES ->
                        presenter.performImagePicking()
                    SendFilePopup.Option.FILES ->
                        presenter.performFilePicking()
                }
            }
        }

        binding.etMessage.addTextChangedListener(textWatcher)

        binding.ibSend.setOnClickListener {
            presenter.sendMessage(binding.etMessage.text.toString().trim())
        }

        binding.ibSendFilePicker.setOnClickListener {
            sendFilePopup.show(it)
        }

        binding.ibCancel.setOnClickListener {
            presenter.cancelFileTransfer()
        }

        binding.btnRetry.setOnClickListener {
            binding.cvPresharingImageHolder.startAnimation(hideAnimation)
            presenter.proceedPresharing()
        }

        binding.btnCancel.setOnClickListener {
            binding.cvPresharingImageHolder.startAnimation(hideAnimation)
            presenter.cancelPresharing()
        }

        binding.gdbGoDown.setOnClickListener {
            chatLayoutManager.scrollToPosition(0)
            scrollBehavior.hideChild()
            binding.gdbGoDown.setUnreadMessageNumber(0)
        }

        scrollBehavior = ScrollAwareBehavior(requireActivity()).apply {
            onHideListener = { binding.gdbGoDown.setUnreadMessageNumber(0) }
        }

        val params = binding.gdbGoDown.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = scrollBehavior
        binding.gdbGoDown.requestLayout()

        binding.rvChat.doOnPreDraw {
            binding.rvChat.postDelayed({
                startPostponedEnterTransition()
            }, 100)
        }

        chatAdapter = ChatAdapter().apply {
            imageClickListener = { view, message ->
                if (message.type == PayloadType.IMAGE) {
                    val extras = FragmentNavigatorExtras(
                        view to message.uid.toString()
                    )
                    findNavController().navigate(
                        ChatFragmentDirections.actionChatFragmentToImagePreviewFragment(
                            messageId = message.uid,
                            imagePath = message.filePath,
                            own = message.own
                        ), navigatorExtras = extras
                    )
                } else {
                    with(Intent(Intent.ACTION_VIEW)) {
                        val uri = FileProvider.getUriForFile(
                            context!!,
                            requireContext().packageName + ".provider",
                            File(message.filePath)
                        )
                        data = uri
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivity(
                            Intent.createChooser(
                                this,
                                getString(R.string.files__open_chooser)
                            )
                        )
                    }
                }
            }
        }

        binding.rvChat.apply {

            adapter = chatAdapter
            chatAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
            }
            chatLayoutManager = layoutManager as LinearLayoutManager

            addItemDecoration(StickyRecyclerHeadersDecoration(chatAdapter))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(recyclerView: RecyclerView, scrollState: Int) {

                    val picasso = Picasso.get()
                    if (scrollState == RecyclerView.SCROLL_STATE_IDLE || scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        picasso.resumeTag(chatAdapter.picassoTag)
                    } else {
                        picasso.pauseTag(chatAdapter.picassoTag)
                    }
                }
            })
        }

        presenter.onViewCreated()

        if ((!args.message.isNullOrEmpty() || !args.filepath.isNullOrEmpty()) && !args.address.isNullOrEmpty()) {

            val textToShare = args.message
            val fileToShare = args.filepath

            if (textToShare != null) {
                binding.etMessage.setText(textToShare)
            } else if (fileToShare != null) {
                //FIXME
                Handler().postDelayed({
                    val file = File(fileToShare)
                    presenter.sendFile(File(fileToShare), file.absolutePath.getFileType())
                }, 1000)
            }

        }
    }

    override fun onBackPressed() {
        findNavController().navigateUp()
    }

    override fun setBackgroundColor(color: Int) {
        binding.clChatContainer.setBackgroundColor(color)
    }

    override fun dismissMessageNotification() {
        requireActivity().getNotificationManager()
            .cancel(
                NotificationView.NOTIFICATION_TAG_MESSAGE,
                NotificationView.NOTIFICATION_ID_MESSAGE
            )
    }

    override fun showPartnerName(name: String, device: String) {
        binding.appBar.tbToolbar.title = "$name ($device)"
    }

    override fun showStatusConnected() {
        binding.appBar.tbToolbar.subtitle = getString(R.string.chat__connected)
    }

    override fun showStatusNotConnected() {
        binding.appBar.tbToolbar.subtitle = getString(R.string.chat__not_connected)
    }

    override fun showStatusPending() {
        binding.appBar.tbToolbar.subtitle = getString(R.string.chat__pending)
    }

    override fun showNotConnectedToSend() =
        Toast.makeText(
            requireActivity(),
            getString(R.string.chat__not_connected_to_send),
            Toast.LENGTH_LONG
        ).show()

    override fun afterMessageSent() {
        binding.etMessage.text = null
    }

    override fun showNotConnectedToThisDevice(currentDevice: String) {
        binding.avActions.setActionsAndShow(
            getString(R.string.chat__connected_to_another, currentDevice),
            ActionView.Action(getString(R.string.chat__connect)) { presenter.connectToDevice() },
            null
        )
    }

    override fun showNotConnectedToAnyDevice() {
        binding.avActions.setActionsAndShow(
            getString(R.string.chat__not_connected_to_this_device),
            ActionView.Action(getString(R.string.chat__connect)) { presenter.connectToDevice() },
            null
        )
    }

    override fun showWainingForOpponent() {
        binding.avActions.setActionsAndShow(
            getString(R.string.chat__waiting_for_device),
            ActionView.Action(getString(R.string.general__cancel)) { presenter.resetConnection() },
            null
        )
    }

    override fun showConnectionRequest(displayName: String, deviceName: String) {
        binding.avActions.setActionsAndShow(getString(
            R.string.chat__connection_request,
            displayName,
            deviceName
        ),
            ActionView.Action(getString(R.string.general__start_chat)) { presenter.acceptConnection() },
            ActionView.Action(getString(R.string.chat__disconnect)) { presenter.rejectConnection() }
        )
    }

    override fun showServiceDestroyed() = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(getString(R.string.general__service_lost))
            .setPositiveButton(getString(R.string.general__restart)) { _, _ -> presenter.prepareConnection() }
            .setCancelable(false)
            .show()
    }

    override fun hideActions() {
        binding.avActions.visibility = View.GONE
    }

    override fun showMessagesHistory(messages: List<ChatMessageViewModel>) {
        chatAdapter.messages = LinkedList(messages)
        chatAdapter.notifyDataSetChanged()
    }

    override fun showReceivedMessage(message: ChatMessageViewModel) {
        chatAdapter.messages.addFirst(message)
        chatAdapter.notifyItemInserted(0)
        if (!scrollBehavior.isChildShown()) {
            chatLayoutManager.scrollToPosition(0)
        } else {
            binding.gdbGoDown.setUnreadMessageNumber(binding.gdbGoDown.getUnreadMessageNumber() + 1)
        }
    }

    override fun showSentMessage(message: ChatMessageViewModel) {
        chatAdapter.messages.addFirst(message)
        chatAdapter.notifyItemInserted(0)
        chatLayoutManager.scrollToPosition(0)
    }

    override fun showSendingMessageFailure() {
        Toast.makeText(requireActivity(), R.string.chat__sending_failed, Toast.LENGTH_LONG).show()
    }

    override fun showRejectedConnection() = doIfStarted {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.chat__connection_rejected))
            .setPositiveButton(getString(R.string.general__ok), null)
            .show()
    }

    override fun showBluetoothDisabled() {
        binding.avActions.setActionsAndShow(
            getString(R.string.chat__bluetooth_is_disabled),
            ActionView.Action(getString(R.string.chat__enable)) { presenter.enableBluetooth() },
            null
        )
    }

    override fun showLostConnection() = doIfStarted {
        lostConnectionDialog = AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.chat__connection_lost))
            .setPositiveButton(getString(R.string.chat__reconnect)) { _, _ -> presenter.reconnect() }
            .setNegativeButton(getString(R.string.general__cancel), null)
            .show()
    }

    override fun hideLostConnection() {
        lostConnectionDialog?.dismiss()
        lostConnectionDialog = null
    }

    override fun showDisconnected() = doIfStarted {
        disconnectedDialog = AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.chat__partner_disconnected))
            .setPositiveButton(getString(R.string.chat__reconnect)) { _, _ -> presenter.reconnect() }
            .setNegativeButton(getString(R.string.general__cancel), null)
            .show()
    }

    override fun hideDisconnected() {
        disconnectedDialog?.dismiss()
        disconnectedDialog = null
    }

    override fun showFailedConnection() = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(getString(R.string.chat__unable_to_connect))
            .setPositiveButton(getString(R.string.general__try_again)) { _, _ -> presenter.connectToDevice() }
            .setNegativeButton(getString(R.string.general__cancel), null)
            .show()
    }

    override fun showNotValidMessage() {
        Toast.makeText(
            requireActivity(),
            getString(R.string.chat__message_cannot_be_empty),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun showDeviceIsNotAvailable() = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(getString(R.string.chat__device_is_not_available))
            .setPositiveButton(getString(R.string.chat__rescan)) { _, _ ->
                findNavController().navigate(ChatFragmentDirections.actionChatFragmentToScanFragment())
            }
            .show()
    }

    override fun requestBluetoothEnabling() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        } catch (e: ActivityNotFoundException) {
            showUnableToActivateBluetoothMessage()
        }
    }

    private fun showUnableToActivateBluetoothMessage() = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(R.string.scan__unable_to_activate)
            .setPositiveButton(R.string.general__ok, null)
            .show()
    }

    override fun showBluetoothEnablingFailed() = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(getString(R.string.chat__bluetooth_required))
            .setPositiveButton(getString(R.string.general__ok), null)
            .show()
    }

    override fun showFileTooBig(maxSize: Long) = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(getString(R.string.chat__too_big_file, maxSize.toReadableFileSize()))
            .setPositiveButton(getString(R.string.general__ok), null)
            .show()
    }

    override fun showImageNotExist() {
        Toast.makeText(requireActivity(), R.string.chat__file_not_exist, Toast.LENGTH_LONG).show()
    }

    override fun showPresharingImage(path: String) {

        binding.cvPresharingImageHolder.visibility = View.VISIBLE
        binding.cvPresharingImageHolder.startAnimation(showAnimation)

        Picasso.get()
            .load("file://$path")
            .centerCrop()
            .fit()
            .into(binding.ivPresharingImage)
    }

    override fun showPresharingFile() {
        binding.cvPresharingImageHolder.visibility = View.VISIBLE
        binding.cvPresharingImageHolder.startAnimation(showAnimation)
        binding.ivPresharingImage.setImageResource(R.drawable.ic_file_upload)
    }

    override fun openImagePicker() {
        easyImage.openGallery(this)
    }

    override fun openFilePicker() {
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        fileIntent.type = "*/*"
        startActivityForResult(fileIntent, REQUEST_FILE)
    }

    override fun showFileTransferLayout(
        fileAddress: String?,
        fileSize: Long,
        transferType: ChatView.FileTransferType,
        type: PayloadType
    ) {

        binding.llTextSendingHolder.visibility = View.GONE
        binding.llImageSendingHolder.visibility = View.VISIBLE

        if (type == PayloadType.IMAGE) {
            binding.tvSendingImageLabel.text = getString(
                if (transferType == ChatView.FileTransferType.SENDING)
                    R.string.chat__sending_image else R.string.chat__receiving_images
            )

            Picasso.get()
                .load("file://$fileAddress")
                .into(object : Target {

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        binding.ivTransferringImage.setImageResource(R.drawable.ic_photo)
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        binding.ivTransferringImage.setImageResource(R.drawable.ic_photo)
                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        binding.ivTransferringImage.setImageBitmap(bitmap)
                    }
                })
        } else {
            binding.tvSendingImageLabel.text = getString(
                if (transferType == ChatView.FileTransferType.SENDING)
                    R.string.chat__sending_file else R.string.chat__receiving_file
            )
            binding.ivTransferringImage.setImageResource(R.drawable.ic_file_upload)
        }

        binding.tvFileSize.text = fileSize.toReadableFileSize()
        binding.tvFileSendingPercentage.text = "0%"
        //FIXME should work with Long
        binding.pbTransferringProgress.progress = 0
        binding.pbTransferringProgress.max = fileSize.toInt()
    }

    @SuppressLint("SetTextI18n")
    override fun updateFileTransferProgress(transferredBytes: Long, totalBytes: Long) {

        val percents = transferredBytes.toFloat() / totalBytes * 100
        binding.tvFileSendingPercentage.text = "${Math.round(percents)}%"
        //FIXME should work with Long
        binding.pbTransferringProgress.progress = transferredBytes.toInt()

    }

    override fun hideFileTransferLayout() {
        binding.llTextSendingHolder.visibility = View.VISIBLE
        binding.llImageSendingHolder.visibility = View.GONE
    }

    override fun showFileTransferCanceled() {
        Toast.makeText(
            requireActivity(),
            R.string.chat__partner_canceled_file_transfer,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun showFileTransferFailure() {
        Toast.makeText(
            requireActivity(),
            R.string.chat__problem_during_file_transfer,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun showReceiverUnableToReceiveFiles() = doIfStarted {
        AlertDialog.Builder(requireActivity())
            .setMessage(R.string.chat__partner_unable_to_receive_files)
            .setPositiveButton(R.string.general__ok, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_chat, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> {
                if (resultCode == Activity.RESULT_OK) {
                    presenter.onBluetoothEnabled()
                } else {
                    presenter.onBluetoothEnablingFailed()
                }
            }
            REQUEST_FILE -> {
                if (data != null && data.data != null) {
                    val uri = data.data
                    val path = requireContext().getPath(uri)
                    presenter.sendFile(File(path), PayloadType.FILE)
                }
            }
            else -> {

                easyImage.handleActivityResult(
                    requestCode,
                    resultCode,
                    data,
                    requireActivity(),
                    object : DefaultCallback() {

                        override fun onImagePickerError(error: Throwable, source: MediaSource) {
                            Toast.makeText(
                                requireActivity(),
                                R.string.chat__unable_to_pick_image,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onCanceled(source: MediaSource) {

                        }

                        override fun onMediaFilesPicked(
                            imageFiles: Array<MediaFile>,
                            source: MediaSource
                        ) {
                            if (imageFiles.isNotEmpty()) {
                                presenter.sendFile(imageFiles[0].file, PayloadType.IMAGE)
                            }
                        }
                    })
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_files -> {
                findNavController().navigate(
                    ChatFragmentDirections.actionChatFragmentToReceivedFilesFragment(
                        address = deviceAddress
                    )
                )
                true
            }
            R.id.action_disconnect -> {
                presenter.disconnect()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {

        private const val REQUEST_ENABLE_BLUETOOTH = 101
        private const val REQUEST_FILE = 201

    }
}
