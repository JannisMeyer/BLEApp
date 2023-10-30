package com.example.bleapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.ScanResult
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bleapp.databinding.ActivityMainBinding
import java.util.*
import java.util.regex.Pattern



class MainActivity : AppCompatActivity() {

    private val bondStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) {
                    BluetoothDevice.BOND_BONDING -> {
                        Toast.makeText(context, "bonding...", Toast.LENGTH_LONG).show()
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        if (device != null) {
                            Toast.makeText(context, "bonded to "+device.name.toString(), Toast.LENGTH_LONG).show()
                            bonded = true
                            if (context != null) {
                                setupGattConnection(device.address, context)
                            }
                        }
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Toast.makeText(context, "No bond!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    //companion object to recall bond state
    companion object {
        var bonded: Boolean = false
    }

    private lateinit var binding:ActivityMainBinding

    //permissions needed for companion device binding
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
    )

    private val locationPermissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                              Manifest.permission.ACCESS_FINE_LOCATION,
                                              Manifest.permission.ACCESS_COARSE_LOCATION)


    ////set up instances and variables for ble device finding and pairing
    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private val selectDeviceRequestCode = 1
    private val bluetoothPermissionsRequestCode = 2
    private val locationPermissionsRequestCode = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //connect this activity with corresponding display
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Check required permissions
        checkPermissions(locationPermissions, locationPermissionsRequestCode) //location permissions have to be requested separately
        checkPermissions(bluetoothPermissions, bluetoothPermissionsRequestCode)

        //set up instances for ble device finding and pairing
        val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
            .setNamePattern(Pattern.compile("BBC micro:bit *"))
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            // Find only devices that match this request filter.
            .addDeviceFilter(deviceFilter)
            // Stop scanning as soon as one device matching the filter is found.
            .setSingleDevice(false)
            .build()

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bondStateReceiver, filter)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager


        //set On-Click Listener for scanning button
        binding.scanButton.setOnClickListener() {
            //Toast.makeText(this, "click", Toast.LENGTH_LONG).show()
            if (bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
                deviceManager.associate(pairingRequest,
                    object : CompanionDeviceManager.Callback() { //Called when a device is found, launch the IntentSender so the user can select the device they want to pair with
                        @Deprecated("Deprecated in Java")
                        override fun onDeviceFound(chooserLauncher: IntentSender) {
                            //Toast.makeText(applicationContext, "onDeviceFound()", Toast.LENGTH_LONG).show()
                            startIntentSenderForResult(chooserLauncher,
                                selectDeviceRequestCode, null, 0, 0, 0)
                        }

                        override fun onFailure(error: CharSequence?) {
                            Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
                        }
                    }, null)
            }
            else {
                Toast.makeText(this, "Bluetooth or Location not enabled!", Toast.LENGTH_LONG).show()
            }

        }
        binding.scanButton.isEnabled = !bonded
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission") //Permissions already granted before
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Toast.makeText(this, "onActivityResult()", Toast.LENGTH_LONG).show()
        when (requestCode) {
            selectDeviceRequestCode -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device
                    val scanResult: ScanResult? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    if (scanResult?.device?.createBond() == true) {
                        Toast.makeText(this, "Starting device bonding...", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this, "Failed to start device bonding!", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPermissions(permissions: Array<out String>, requestCode: Int) {

        //Iterate through required permissions and request them if needed
        for (item in permissions) {
            if (ActivityCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                //shouldShowRequestPermissionRationale("test")
                ActivityCompat.requestPermissions(this, permissions, requestCode)
                break
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var granted = true

        var test = 0

        //case of bluetooth permissions
        if (requestCode == bluetoothPermissionsRequestCode && grantResults.isNotEmpty()) {
            for (item in grantResults) {
                if (item == PackageManager.PERMISSION_DENIED) {
                    granted = false
                    Toast.makeText(this, item.toString() + test.toString(), Toast.LENGTH_LONG)
                        .show()
                    break;
                }
                test++
            }
            if (granted) {
                Toast.makeText(this, "Bluetooth permissions granted!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Bluetooth permissions denied!", Toast.LENGTH_LONG).show()
            }
        }
        //case of location permissions
        else if (requestCode == locationPermissionsRequestCode && grantResults.isNotEmpty()) {
            for (item in grantResults) {
                if (item == PackageManager.PERMISSION_DENIED) {
                    granted = false
                    Toast.makeText(this, "$item $test", Toast.LENGTH_LONG)
                        .show()
                    break;
                }
                test++
            }
            if (granted) {
                Toast.makeText(this, "Location permissions granted!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Location permissions denied!", Toast.LENGTH_LONG).show()
            }
        //case of fail
        } else {
            Toast.makeText(this, "Failed giving permissions!", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission") //permissions given before
    fun setupGattConnection(deviceName:String, context:Context) {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceName)
        val gattCallback = object : BluetoothGattCallback() {
            // Callback-Funktionen für Verbindungsstatus, Service-Entdeckung, Datenübertragung, etc.
        }

        val gatt = device.connectGatt(context, false, gattCallback)

        //TODO: manage device connection after bonding
    }

    override fun onDestroy() {
        super.onDestroy()

        //Bond State Receiver has to be unregistered after use to prevent memory leaks
        unregisterReceiver(bondStateReceiver)
    }

    override fun onResume() {
        super.onResume()
        //Toast.makeText(this, "test", Toast.LENGTH_LONG).show()
    }
}