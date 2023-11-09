package com.example.bleapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bleapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    //(TODO: Add disconnect button)
    //TODO: Replace deprecated intent launch in onClickListener
    //TODO: Look at received data/discovered characteristics when micro:bit sends
    //TODO: Implement receive/answer functionality

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
                                binding.bondStatusText.text = device.name
                                bonded = true
                                binding.scanButton.isEnabled = false
                            }
                            if (context != null && !gattConnected) {
                                setupGattConnection(device.address, context)
                            }
                        }
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Log.i(TAG, "No bond!")
                        binding.bondStatusText.text = ""
                        binding.scanButton.isEnabled = true
                    }
                }
            }
        }
    }

    //companion object to recall bond state
    companion object {
        var bonded = false
        var gattConnected = false
        var servicesDiscovered = false
        var streamActive = false
        var coroutineExists = false
    }

    private lateinit var binding:ActivityMainBinding

    val mainHandler = Handler(Looper.getMainLooper())

    //permissions needed for companion device binding
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
    )

    private val locationPermissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                              Manifest.permission.ACCESS_FINE_LOCATION,
                                              Manifest.permission.ACCESS_COARSE_LOCATION)


    //set up instances and variables for ble device bonding and gatt connection
    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private lateinit var gatt : BluetoothGatt

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "onCreate()")

        //connect this activity with corresponding display
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Check required permissions
        checkPermissions(locationPermissions, Defines.LOCATION_PERMISSIONS_REQUEST_CODE) //location permissions have to be requested separately
        checkPermissions(bluetoothPermissions, Defines.BLUETOOTH_PERMISSIONS_REQUEST_CODE)

        //set up instances for ble device finding and pairing
        val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
            .setNamePattern(Pattern.compile("BBC micro:bit *"))
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(true)
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
                        bonded = true
                        binding.scanButton.isEnabled = false
                        binding.bondStatusText.text = device.name
                        Log.i(TAG, "micro:bit already bonded!")
                        if (!gattConnected) {
                            Log.i(TAG, "Creating gatt connection to micro:bit...")
                            setupGattConnection(device.address, this)
                        }
                        else {
                            Log.i(TAG, "micro:bit already connected!")
                            binding.connectGattButton.isEnabled = false
                        }
                        break
                    }
                }
                if (!bonded) {
                    Log.w(TAG, "Not bonded to micro:bit!")
                    binding.bondStatusText.text = ""
                    binding.scanButton.isEnabled = true
                }
            }
            else {
                Log.i(TAG, "No device bonded!")
                binding.bondStatusText.text = ""
                binding.scanButton.isEnabled = true
            }
        } else {
            Log.e(TAG, "Bluetooth not available/activated!")
            binding.bondStatusText.text = ""
            binding.scanButton.isEnabled = true
        }


        //set OnClickListener for Scan Button
        binding.scanButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
                if (!bonded) {
                    deviceManager.associate(pairingRequest,
                        object :
                            CompanionDeviceManager.Callback() { //Called when a device is found, launch the IntentSender so the user can select the device they want to pair with
                            @Deprecated("Deprecated in Java")
                            override fun onDeviceFound(chooserLauncher: IntentSender) {
                                startIntentSenderForResult(
                                    chooserLauncher,
                                    Defines.SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                                )
                            }

                            override fun onFailure(error: CharSequence?) {
                                Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
                            }
                        }, null
                    )
                }
                else {
                    Log.i(TAG, "micro:bit already bonded!")
                }
            }
            else {
                Toast.makeText(this, "Bluetooth or location not enabled!", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Bluetooth or location not enabled!")
            }
        }

        //set OnClickListener for Connect Gatt Button
        binding.connectGattButton.setOnClickListener {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
                val pairedDevices = bluetoothAdapter.bondedDevices

                if (pairedDevices != null) {
                    for (device in pairedDevices) {
                        if (device.name.contains("BBC micro:bit") && !gattConnected) {
                            Log.i(TAG, "Creating gatt connection to micro:bit...")
                            setupGattConnection(device.address, this)
                            break
                        }
                        else {
                            Log.w(TAG, "micro:bit not bonded or gatt connection already established!")
                        }
                    }
                }
                else {
                    Log.i(TAG, "No device bonded!")
                }
            } else {
                Toast.makeText(this, "Bluetooth or location not enabled!", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Bluetooth or location not available/activated!")
            }
        }
        binding.connectGattButton.isEnabled = false

        //set OnClickListener for Get Name Button
        binding.receiveDataButton.setOnClickListener {

            showReceiveOptionsDialog()
        }
        binding.receiveDataButton.isEnabled = false

        //set OnClickListener for Send Data Button
        binding.sendDataButton.setOnClickListener {

            showSendOptionsDialog()
        }
        binding.sendDataButton.isEnabled = false
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission") //Permissions already granted before
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Toast.makeText(this, "onActivityResult()", Toast.LENGTH_LONG).show()
        when (requestCode) {
            Defines.SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device
                    val scanResult: ScanResult? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    if (scanResult?.device?.createBond() == true) {
                        Log.i(TAG, "Initiating device bonding...")
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
        if (requestCode == Defines.BLUETOOTH_PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty()) {
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
        else if (requestCode == Defines.LOCATION_PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty()) {
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

    @SuppressLint("MissingPermission")
    fun readBleData(paraService : Int) {

        lateinit var service : BluetoothGattService
        lateinit var characteristic : BluetoothGattCharacteristic

        when(paraService) {

            Defines.DEVICE_NAME_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.GENERIC_ACCESS_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.DEVICE_NAME))
            }
            Defines.MODEL_NUMBER_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.DEVICE_INFORMATION_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.MODEL_NUMBER))
            }
            Defines.FIRMWARE_REVISION_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.DEVICE_INFORMATION_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.FIRMWARE_REVISION))
            }
        }

        gatt.readCharacteristic(characteristic)

        //handling of read data in onCharacteristicsRead further below
    }

    @SuppressLint("MissingPermission")
    fun writeBleData(paraService: Int, data: String) {

        lateinit var service : BluetoothGattService
        lateinit var characteristic : BluetoothGattCharacteristic

        when(paraService) {

            Defines.WRITE_LED_TEXT_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.LED_SERVICE_2))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.LED_TEXT))
                characteristic.value = data.toByteArray()
            }
            Defines.WRITE_LED_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.LED_SERVICE_2))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.LED_MATRIX_STATE))
                val byteData = arrayListOf<Byte>(10, 0, 17, 14, 0)
                characteristic.value = byteData.toByteArray()
            }
        }

        if (data != "0") {
            gatt.writeCharacteristic(characteristic)
            streamActive = false
        }
        else {
            if (!coroutineExists) {
                Log.v(TAG, "Starting coroutine...")
                CoroutineScope(Dispatchers.Default).launch {
                    Log.i(TAG, "Coroutine started!")

                    streamActive = true
                    coroutineExists = true

                    /*withContext(Dispatchers.Main) {
                        builder.dismiss()
                    }*/

                    while (streamActive) {
                        gatt.writeCharacteristic(characteristic)
                        delay(1000)
                    }

                    coroutineExists = false

                    //Coroutine ends with this function/block, when streamActive isn't true anymore
                    Log.i(TAG, "Coroutine terminated!")
                }
            }
            else {
                Log.w(TAG, "A coroutine already exists!")
            }
        }

        //handling of written data in onCharacteristicsWrite further below
    }

    private fun showReceiveOptionsDialog() {
        val builder = AlertDialog.Builder(this)
        val options = arrayOf("Device Name", "Model Number", "Firmware Version")

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    readBleData(Defines.DEVICE_NAME_REQUEST)
                }
                1 -> {
                    readBleData(Defines.MODEL_NUMBER_REQUEST)
                }
                2 -> {
                    readBleData(Defines.FIRMWARE_REVISION_REQUEST)
                }
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showSendOptionsDialog() {
        val builder = AlertDialog.Builder(this)
        val options = arrayOf("Text", "Smiley", "Stream") //

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    writeBleData(Defines.WRITE_LED_TEXT_REQUEST, "Text")
                }
                1 -> {
                    writeBleData(Defines.WRITE_LED_REQUEST, "") //no data needed
                }
                2 -> {
                    writeBleData(Defines.WRITE_LED_TEXT_REQUEST, "0")
                }
            }
        }.show()
    }


    @SuppressLint("MissingPermission") //permissions given before
    fun setupGattConnection(deviceAddress:String, context:Context) {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        val gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gattConnected = true
                        mainHandler.post {
                            binding.connectGattButton.isEnabled = false //only main/ui thread can access ui/animation elements
                        }
                        binding.gattStatusText.text = device.name
                        Log.i(TAG, "BluetoothDevice CONNECTED: $device")
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gattConnected = false
                        streamActive = false //to stop potential data streaming/coroutine
                        binding.gattStatusText.text = ""
                        mainHandler.post {
                            binding.connectGattButton.isEnabled = true
                        }
                        Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                    }
                    else {
                        gattConnected = false
                        streamActive = false
                        binding.gattStatusText.text = ""
                        binding.connectGattButton.isEnabled = true
                        Log.e(TAG, "Unknown connect state!")
                    }
                }
                else {
                    Log.e(TAG, "gatt operation failed!")
                    binding.connectGattButton.isEnabled = true
                }
            }

            override fun onServicesDiscovered(paraGatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(paraGatt, status)

                Log.i(TAG, "gattConnected value: $gattConnected")

                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (discovery of services)
                    servicesDiscovered = true
                    mainHandler.post {
                        binding.receiveDataButton.isEnabled = true
                        binding.sendDataButton.isEnabled = true
                    }
                    for (item in paraGatt!!.services) {
                        Log.i(TAG, "got service with uuid: " + item.uuid)
                        for (item2 in item.characteristics) {
                            Log.i(TAG, "    got characteristic with uuid: " + item2.uuid)
                        }
                    }
                }
                else {
                    servicesDiscovered = false
                    mainHandler.post {
                        binding.receiveDataButton.isEnabled = false
                        binding.sendDataButton.isEnabled = false
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (writing characteristic)
                    Log.i(TAG, "Wrote data to micro:bit")
                }
                else {
                    Log.e(TAG, "Failed to write data!")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (reading characteristic)
                    val value = characteristic.getStringValue(0)
                    Log.i(TAG, "Read data from micro:bit: $value")
                    binding.receivedData.text = value

                    //Handler to main thread needed as toasts can only be shown in main ui thread
                    mainHandler.post {
                        Toast.makeText(applicationContext, "New data received!", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    Log.e(TAG, "Failed to read data!")
                }
            }
        }

        //connect to gatt
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()

        //Bond State Receiver has to be unregistered after use to prevent memory leaks
        unregisterReceiver(bondStateReceiver)

        //Disconnect gatt/device
        gatt.disconnect()

        //No unbonding due to the lack of an official unbonding method, bonded devices are permanent and independent from the app runtime anyways
        //(bonding and unbonding can also be done in bluetooth settings)
    }

    @SuppressLint("MissingPermission") //Permissions granted before
    override fun onResume() {
        super.onResume()

        /*
        Log.i(TAG, "onResume()")

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //Check if micro:bit is already bonded (bonded != connected!!!)
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    Log.i(TAG, device.name + ' ' + device.address)
                    if (device.name.contains("BBC micro:bit")) {
                        bonded = true //to deactivate the scanning button
                        binding.bondStatusText.text = device.name
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
                    Log.w(TAG, "micro:bit not bonded!")
                    binding.bondStatusText.text = ""
                }
            }
            else {
                Log.i(TAG, "No device bonded!")
                binding.bondStatusText.text = ""
            }
        } else {
            Log.i(TAG, "Bluetooth or location not available/activated!")
            binding.bondStatusText.text = ""
        }
         */
    }
}