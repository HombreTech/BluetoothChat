package tech.hombre.bluetoothchatter.ui.activity

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentScanBinding
import tech.hombre.bluetoothchatter.ui.adapter.DevicesAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ScanPresenter
import tech.hombre.bluetoothchatter.ui.view.ScanView
import tech.hombre.bluetoothchatter.utils.setResultOk

class ScanFragment : BaseFragment<FragmentScanBinding>(R.layout.fragment_scan), ScanView {

    private val presenter: ScanPresenter by inject { parametersOf(this) }

    private val devicesAdapter: DevicesAdapter by lazy { DevicesAdapter(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(presenter)

        (requireActivity() as MainActivity).setSupportActionBar(binding.appBar.tbToolbar)
        binding.appBar.tbToolbar.title = getString(R.string.scan__scan)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.rvPairedDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPairedDevices.adapter = devicesAdapter

        devicesAdapter.listener = {
            presenter.onDevicePicked(it.address)
            binding.flProgress.visibility = View.VISIBLE
        }

        presenter.checkBluetoothAvailability()

        binding.btnTurnOn.setOnClickListener {
            presenter.turnOnBluetooth()
        }

        binding.btnMakeDiscoverable.setOnClickListener {
            presenter.makeDiscoverable()
        }

        binding.btnScan.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                presenter.scanForDevices()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    explainAskingLocationPermission()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
            }
        }

        binding.ivShare.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                presenter.shareApk()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    explainAskingStoragePermission()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        findNavController().navigateUp()
    }

    override fun shareApk(uri: Uri) {

        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            //`package` = "com.android.bluetooth"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        try {
            startActivity(
                Intent.createChooser(
                    sharingIntent,
                    getString(R.string.scan__share_intent)
                )
            )
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.scan__unable_to_share_apk),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun openChat(device: BluetoothDevice) {
        setResultOk(bundleOf(EXTRA_BLUETOOTH_DEVICE to device))
        findNavController().navigateUp()
    }

    override fun showPairedDevices(pairedDevices: List<BluetoothDevice>) {

        binding.llTurnOn.visibility = View.GONE
        binding.clList.visibility = View.VISIBLE

        if (pairedDevices.isNotEmpty()) {
            devicesAdapter.pairedList = ArrayList(pairedDevices)
            devicesAdapter.notifyDataSetChanged()
        }
    }

    override fun showBluetoothScanner() {
        binding.flContainer.visibility = View.VISIBLE
        presenter.checkBluetoothEnabling()
    }

    override fun showBluetoothEnablingRequest() {
        binding.llTurnOn.visibility = View.VISIBLE
        binding.clList.visibility = View.GONE
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
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.scan__unable_to_activate)
            .setPositiveButton(R.string.general__ok, null)
            .show()
    }

    override fun showBluetoothIsNotAvailableMessage() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.scan__no_access_to_bluetooth)
            .setPositiveButton(R.string.general__ok) { _, _ -> findNavController().navigateUp() }
            .show()
    }

    override fun showBluetoothEnablingFailed() = doIfStarted {
        AlertDialog.Builder(requireContext())
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
            Toast.makeText(
                requireContext(),
                getString(R.string.scan__no_discoverable_activity),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun showDiscoverableProcess() {
        binding.btnMakeDiscoverable.text = getString(R.string.scan__discoverable)
        binding.btnMakeDiscoverable.isEnabled = false
    }

    override fun showDiscoverableFinished() {
        binding.btnMakeDiscoverable.text = getString(R.string.scan__make_discoverable)
        binding.btnMakeDiscoverable.isEnabled = true
    }

    override fun showScanningStarted(seconds: Int) {
        binding.epbProgress.runExpiring(seconds)
        binding.epbProgress.visibility = View.VISIBLE
        binding.tvDiscoveryLabel.visibility = View.VISIBLE
        binding.btnScan.text = getString(R.string.scan__stop_scanning)
    }

    override fun showScanningStopped() {
        binding.epbProgress.cancel()
        binding.epbProgress.visibility = View.GONE
        binding.tvDiscoveryLabel.visibility = View.GONE
        binding.btnScan.text = getString(R.string.scan__scan_for_devices)
    }

    override fun showBluetoothDiscoverableFailure() = doIfStarted {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.scan__unable_to_make_discoverable)
            .setPositiveButton(R.string.general__ok, null)
            .show()
    }

    override fun showServiceUnavailable() {
        binding.flProgress.visibility = View.GONE
        doIfStarted {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.scan__unable_to_connect_service)
                .setPositiveButton(R.string.general__ok, null)
                .show()
        }
    }

    override fun showUnableToConnect() {
        binding.flProgress.visibility = View.GONE
        doIfStarted {
            AlertDialog.Builder(requireContext())
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

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
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.scan__permission_explanation_location)
            .setPositiveButton(R.string.general__ok) { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
            .show()
    }

    private fun explainAskingStoragePermission() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.scan__permission_explanation_storage)
            .setPositiveButton(R.string.general__ok) { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION
                )
            }
            .show()
    }

    override fun showExtractionApkFailureMessage() = doIfStarted {
        AlertDialog.Builder(requireContext())
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

    }
}
