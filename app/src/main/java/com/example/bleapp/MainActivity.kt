package com.example.bleapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
                        Log.i(TAG, "Bonding...")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        if (device != null) {
                            Log.i(TAG, "Bonded to " + device.name + "!")
                            if (device.name == "BBC micro:bit") {
                                bonded = true
                            }
                            if (context != null && !gattConnected) {
                                setupGattConnection(device.address, context)
                            }
                        }
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Log.i(TAG, "No bond!")
                    }
                }
            }
        }
    }

    //companion object to recall bond state
    companion object {
        var bonded = false
        var gattConnected = false
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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "onCreate()")

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

        //Check if micro:bit/other devices are already bonded (bonded != connected!!!)
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    Log.i(TAG, device.name + ' ' + device.address)
                    if (device.name.contains("BBC micro:bit")) {
                        bonded = true //to deactivate the scanning button
                        Log.i(TAG, "micro:bit already bonded!")
                        if (!gattConnected) {
                            Log.i(TAG, "Creating gatt connection to micro:bit...")
                            setupGattConnection(device.address, this)
                        }
                        else {
                            Log.i(TAG, "micro:bit already connected!")
                        }
                        break
                    }
                }
                if (!bonded) {
                    Log.w(TAG, "Not bonded to micro:bit!")
                }
            }
            else {
                Log.i(TAG, "No device bonded!")
            }
        } else {
            Log.e(TAG, "Bluetooth not available/activated!")
        }


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
                Log.w(TAG, "Bluetooth or Location not enabled!")
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
                        Log.i(TAG, "Initiating device connection...")
                    }
                    else {
                        Log.e(TAG, "Failed to initiate device bonding!")
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
                Log.i(TAG, "Bluetooth permissions granted!")
            } else {
                Log.i(TAG, "Bluetooth permissions denied!")
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
                Log.i(TAG, "Location permissions granted!")
            } else {
                Toast.makeText(this, "Location permissions denied! Please activate location permissions in settings!", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Location permissions denied!")
            }
        //case of fail
        } else {
            Log.e(TAG, "Failed to give permissions!")
        }
    }

    @SuppressLint("MissingPermission") //permissions given before
    fun setupGattConnection(deviceName:String, context:Context) {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceName)
        val gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gattConnected = true
                    Log.i(TAG, "BluetoothDevice CONNECTED: $device")
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gattConnected = false
                    Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)

                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (discovery of services)
                    for (item in gatt!!.services) {
                        Log.v(TAG, "got service with uuid: " + item.uuid)
                        for (item2 in item.characteristics) {
                            Log.v(TAG, "    got characteristic with uuid: " + item2.uuid)
                        }
                    }

                    val service = gatt!!.getService(UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")) //UUID for Device Information Service on nRF52833 chip (micro:bit)

                    /*Log.i(TAG, "successfully discovered services");

                    val characteristic = service.getCharacteristic(UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB")) //UUID for Manufacturer Name

                    Log.i(TAG, "got characteristic with uuid: " + characteristic.uuid.toString())*/

                    //TODO: Use characteristics
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (writing characteristic)

                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (reading characteristic)
                    val value = characteristic.getStringValue(0)
                    Log.i(TAG, "onCharacteristicRead: " + value + " UUID " + characteristic.uuid.toString())
                }
            }
        }

        //connect to gatt
        val gatt = device.connectGatt(context, false, gattCallback)

    }

    override fun onDestroy() {
        super.onDestroy()

        //Bond State Receiver has to be unregistered after use to prevent memory leaks
        unregisterReceiver(bondStateReceiver)
    }

    @SuppressLint("MissingPermission") //Permissions granted before
    override fun onResume() {
        super.onResume()

        Log.i(TAG, "onResume()")

        //Check bond status everytime the app resumes
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //Check if micro:bit/other devices are already bonded (bonded != connected!!!)
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    Log.i(TAG, device.name + ' ' + device.address)
                    if (device.name.contains("BBC micro:bit")) {
                        bonded = true //to deactivate the scanning button
                        Log.i(TAG, "micro:bit already bonded!")
                        if (!gattConnected) {
                            Log.i(TAG, "Creating gatt connection to micro:bit...")
                            //setupGattConnection(device.address, this)
                        }
                        else {
                            Log.i(TAG, "micro:bit already connected!")
                        }
                        break
                    }
                }
                if (!bonded) {
                    Log.w(TAG, "micro:bit not bonded!")
                }
            }
            else {
                Log.i(TAG, "No device bonded!")
            }
        } else {
            Log.i(TAG, "Bluetooth not available/activated!")
        }
    }
}