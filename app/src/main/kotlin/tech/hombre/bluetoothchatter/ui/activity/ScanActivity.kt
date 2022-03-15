package tech.hombre.bluetoothchatter.ui.activity

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.ui.adapter.DevicesAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ScanPresenter
import tech.hombre.bluetoothchatter.ui.view.ScanView
import tech.hombre.bluetoothchatter.ui.widget.ExpiringProgressBar
import tech.hombre.bluetoothchatter.utils.bind
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class ScanActivity : SkeletonActivity(), ScanView {

    private val presenter: ScanPresenter by inject { parametersOf(this) }

    private val container: View by bind(R.id.fl_container)
    private val turnOnHolder: View by bind(R.id.ll_turn_on)
    private val listHolder: View by bind(R.id.cl_list)
    private val progress: View by bind(R.id.fl_progress)

    private val discoveryLabel: TextView by bind(R.id.tv_discovery_label)
    private val progressBar: ExpiringProgressBar by bind(R.id.epb_progress)
    private val makeDiscoverableButton: Button by bind(R.id.btn_make_discoverable)
    private val scanForDevicesButton: Button by bind(R.id.btn_scan)

    private val pairedDevicesList: RecyclerView by bind(R.id.rv_paired_devices)

    private val devicesAdapter: DevicesAdapter = DevicesAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan, ActivityType.CHILD_ACTIVITY)
        lifecycle.addObserver(presenter)

        pairedDevicesList.layoutManager = LinearLayoutManager(this)
        pairedDevicesList.adapter = devicesAdapter

        devicesAdapter.listener = {
            presenter.onDevicePicked(it.address)
            progress.visibility = View.VISIBLE
        }

        presenter.checkBluetoothAvailability()

        findViewById<Button>(R.id.btn_turn_on).setOnClickListener {
            presenter.turnOnBluetooth()
        }

        makeDiscoverableButton.setOnClickListener {
            presenter.makeDiscoverable()
        }

        scanForDevicesButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                presenter.scanForDevices()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    explainAskingLocationPermission()
                } else {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
                }
            }
        }

        findViewById<ImageView>(R.id.iv_share).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                presenter.shareApk()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    explainAskingStoragePermission()
                } else {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                }
            }
        }
    }

    override fun shareApk(uri: Uri) {

        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            `package` = "com.android.bluetooth"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        try {
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.scan__share_intent)))
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.scan__unable_to_share_apk), Toast.LENGTH_LONG).show()
        }
    }

    override fun openChat(device: BluetoothDevice) {
        val intent: Intent = Intent().putExtra(EXTRA_BLUETOOTH_DEVICE, device)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun showPairedDevices(pairedDevices: List<BluetoothDevice>) {

        turnOnHolder.visibility = View.GONE
        listHolder.visibility = View.VISIBLE

        if (pairedDevices.isNotEmpty()) {
            devicesAdapter.pairedList = ArrayList(pairedDevices)
            devicesAdapter.notifyDataSetChanged()
        }
    }

    override fun showBluetoothScanner() {
        container.visibility = View.VISIBLE
        presenter.checkBluetoothEnabling()
    }

    override fun showBluetoothEnablingRequest() {
        turnOnHolder.visibility = View.VISIBLE
        listHolder.visibility = View.GONE
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
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__unable_to_activate)
                .setPositiveButton(R.string.general__ok, null)
                .show()
    }

    override fun showBluetoothIsNotAvailableMessage() = doIfStarted {
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__no_access_to_bluetooth)
                .setPositiveButton(R.string.general__ok) { _, _ -> finish() }
                .show()
    }

    override fun showBluetoothEnablingFailed() = doIfStarted {
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__bluetooth_disabled)
                .setPositiveButton(R.string.general__ok, null)
                .show()
    }

    override fun requestMakingDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
        try {
            startActivityForResult(discoverableIntent, REQUEST_MAKE_DISCOVERABLE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.scan__no_discoverable_activity), Toast.LENGTH_LONG).show()
        }
    }

    override fun showDiscoverableProcess() {
        makeDiscoverableButton.text = getString(R.string.scan__discoverable)
        makeDiscoverableButton.isEnabled = false
    }

    override fun showDiscoverableFinished() {
        makeDiscoverableButton.text = getString(R.string.scan__make_discoverable)
        makeDiscoverableButton.isEnabled = true
    }

    override fun showScanningStarted(seconds: Int) {
        progressBar.runExpiring(seconds)
        progressBar.visibility = View.VISIBLE
        discoveryLabel.visibility = View.VISIBLE
        scanForDevicesButton.text = getString(R.string.scan__stop_scanning)
    }

    override fun showScanningStopped() {
        progressBar.cancel()
        progressBar.visibility = View.GONE
        discoveryLabel.visibility = View.GONE
        scanForDevicesButton.text = getString(R.string.scan__scan_for_devices)
    }

    override fun showBluetoothDiscoverableFailure() = doIfStarted {
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__unable_to_make_discoverable)
                .setPositiveButton(R.string.general__ok, null)
                .show()
    }

    override fun showServiceUnavailable() {
        progress.visibility = View.GONE
        doIfStarted {
            AlertDialog.Builder(this)
                    .setMessage(R.string.scan__unable_to_connect_service)
                    .setPositiveButton(R.string.general__ok, null)
                    .show()
        }
    }

    override fun showUnableToConnect() {
        progress.visibility = View.GONE
        doIfStarted {
            AlertDialog.Builder(this)
                    .setMessage(R.string.scan__unable_to_connect)
                    .setPositiveButton(R.string.general__ok, null)
                    .show()
        }
    }

    override fun addFoundDevice(device: BluetoothDevice) {
        devicesAdapter.addNewFoundDevice(device)
        devicesAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                presenter.onPairedDevicesReady()
            } else {
                presenter.onBluetoothEnablingFailed()
            }
        } else if (requestCode == REQUEST_MAKE_DISCOVERABLE) {
            if (resultCode > 0) {
                presenter.onMadeDiscoverable()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.scanForDevices()
            } else {
                explainAskingLocationPermission()
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.shareApk()
            } else {
                explainAskingStoragePermission()
            }
        }
    }

    private fun explainAskingLocationPermission() {
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__permission_explanation_location)
                .setPositiveButton(R.string.general__ok) { _, _ ->
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
                }
                .show()
    }

    private fun explainAskingStoragePermission() {
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__permission_explanation_storage)
                .setPositiveButton(R.string.general__ok) { _, _ ->
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                }
                .show()
    }

    override fun showExtractionApkFailureMessage() = doIfStarted {
        AlertDialog.Builder(this)
                .setMessage(R.string.scan__unable_to_fetch_apk)
                .setPositiveButton(R.string.general__ok, null)
                .show()
    }

    companion object {

        const val EXTRA_BLUETOOTH_DEVICE = "extra.bluetooth_device"

        private const val REQUEST_ENABLE_BLUETOOTH = 101
        private const val REQUEST_MAKE_DISCOVERABLE = 102
        private const val REQUEST_LOCATION_PERMISSION = 103
        private const val REQUEST_STORAGE_PERMISSION = 104

        fun start(context: Context) =
                context.startActivity(Intent(context, ScanActivity::class.java))

        fun startForResult(context: Activity, requestCode: Int) =
                context.startActivityForResult(
                        Intent(context, ScanActivity::class.java), requestCode)
    }
}
