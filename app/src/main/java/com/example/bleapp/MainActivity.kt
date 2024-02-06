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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bleapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    // (TODO: Replace deprecated functions)
    // TODO: Add bond flags in UI

    // activate xml binding
    private lateinit var xmlBinding:ActivityMainBinding

    val mainHandler = Handler(Looper.getMainLooper())


    // global companion object to recall states of ble mechanics and devices
    companion object {

        var bluetoothEnabled = false
        var locationEnabled = false

        var microbitSelected = false
        var microbitBonded = false
        var microbitConnected = false
        var microbitAddress : String = ""

        var streamActive = false
        var coroutineExists = false

        var inkbirdSelected = false
        var inkbirdBonded = false
        var inkbirdConnected = false
        var inkbirdAddress : String = ""
    }


    // set up bluetoothStateReceiver
    @SuppressLint("MissingPermission") // permissions given before
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            // switch bluetoothEnabled flag
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                Log.i(TAG, "Bluetooth enabled!")
                bluetoothEnabled = true
            } else if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                Log.w(TAG, "Bluetooth disabled!")
                bluetoothEnabled = false
            }

            // check if micro:bit or Inkbird are still bonded
            if (bluetoothEnabled && locationEnabled) {
                val pairedDevices = bluetoothAdapter.bondedDevices

                if (pairedDevices != null) {
                    for (device in pairedDevices) {
                        Log.i(TAG, device.name + ' ' + device.address)
                        if (device.name.contains("BBC micro:bit")) {
                            microbitBonded = true
                            xmlBinding.scanButton.isEnabled = false
                            microbitAddress = device.address
                            Log.i(TAG, "micro:bit still bonded!")
                            break
                        } else if (device.name.contains("Ink@IAM-T1")) {
                            inkbirdBonded = true
                            xmlBinding.scanButton.isEnabled = false
                            inkbirdAddress = device.address
                            Log.i(TAG, "Inkbird still bonded!")
                            break
                        }
                    }
                    if (!microbitBonded && !inkbirdBonded) {
                        Log.w(TAG, "Not bonded to any of accepted devices anymore!")
                        xmlBinding.scanButton.isEnabled = true
                    }
                }
                else {
                    Log.i(TAG, "No bonded devices!")
                    xmlBinding.scanButton.isEnabled = true
                }
            } else {
                Log.e(TAG, "Bluetooth or location not enabled!")
                xmlBinding.scanButton.isEnabled = true
            }
        }
    }


    // set up locationStateReceiver
    private val locationStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            // switch locationEnabled flag
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent?.action) {
                val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                if (locationManager.isLocationEnabled) {
                    Log.i(TAG, "Location enabled!")
                    locationEnabled = true
                } else {
                    Log.w(TAG, "Location disabled!")
                    locationEnabled = false
                }
            }
        }
    }


    // permissions needed for ble communication
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
    )

    private val locationPermissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)


    // set up bondStateReceiver for device bonding
    private val bondStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {

            val action = intent?.action

            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                // check bond state
                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) {
                    BluetoothDevice.BOND_BONDING -> {
                        Log.i(TAG, "Bonding...")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        if (device != null) {
                            Log.i(TAG, "Bonded to " + device.name + "!")

                            // check if newly bonded device is accepted device
                            if (device.name.contains("BBC micro:bit")) {
                                microbitBonded = true
                                microbitAddress = device.address
                                mainHandler.post {
                                    Toast.makeText(applicationContext, "Bonded to micro:bit!", Toast.LENGTH_LONG).show()
                                }
                            } else if (device.name.contains("Ink@IAM-T1")) {
                                inkbirdBonded = true
                                inkbirdAddress = device.address
                                mainHandler.post {
                                    Toast.makeText(applicationContext, "Bonded to Inkbird!", Toast.LENGTH_LONG).show()
                                }
                            }

                            xmlBinding.scanButton.isEnabled = false
                        }
                    }
                    BluetoothDevice.BOND_NONE -> {
                        if (device != null) {
                            Log.i(TAG, "Failed to bond to ${device.name}!")
                        }
                        xmlBinding.receivedData.text = "" // clear data textbox
                        xmlBinding.scanButton.isEnabled = true
                    }
                }
            }
        }
    }


    // set up instances and variables for ble device bonding and gatt connection
    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private lateinit var gatt : BluetoothGatt

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // connect this activity with corresponding display
        xmlBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(xmlBinding.root)


        // set up device selection menu
        val spinnerAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.spinnerItems,
            android.R.layout.simple_spinner_item
        )

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        xmlBinding.selection.adapter = spinnerAdapter

        xmlBinding.selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: android.view.View?, position: Int, id: Long) {

                // set flags corresponding to selected item
                val selectedDevice = parentView?.getItemAtPosition(position).toString()
                if (selectedDevice == "micro:bit") {
                    microbitSelected = true
                    inkbirdSelected = false
                    xmlBinding.gattButton.isEnabled = true
                } else if (selectedDevice == "Inkbird") {
                    microbitSelected = false
                    inkbirdSelected = true
                    xmlBinding.gattButton.isEnabled = true
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) { // has to be defined although not used
                ;
            }
        }


        // check required permissions
        checkPermissions(locationPermissions, Defines.LOCATION_PERMISSIONS_REQUEST_CODE) //location permissions have to be requested separately
        checkPermissions(bluetoothPermissions, Defines.BLUETOOTH_PERMISSIONS_REQUEST_CODE)


        // register receivers for bluetooth and location state
        val bluetoothStateFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, bluetoothStateFilter)

        val locationStateFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(locationStateReceiver, locationStateFilter)


        // set up instances for ble device finding and pairing
        val deviceFilterMicrobit: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
            .setNamePattern(Pattern.compile("BBC micro:bit*"))
            .build()

        val deviceFilterInkBird: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
            .setNamePattern(Pattern.compile("Ink@IAM-T1*"))
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilterMicrobit)
            .addDeviceFilter(deviceFilterInkBird)
            .build()

        val bondFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bondStateReceiver, bondFilter)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager


        // initial check if micro:bit or Inkbird are already bonded and Bluetooth and Location enabled
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
            val pairedDevices = bluetoothAdapter.bondedDevices
            bluetoothEnabled = true
            locationEnabled = true

            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    Log.i(TAG, device.name + ' ' + device.address)

                    if (device.name.contains("BBC micro:bit")) {
                        microbitBonded = true
                        xmlBinding.scanButton.isEnabled = false
                        microbitAddress = device.address
                        Log.i(TAG, "micro:bit already bonded!")
                        break
                    } else if (device.name.contains("Ink@IAM-T1")) {
                        inkbirdBonded = true
                        xmlBinding.scanButton.isEnabled = false
                        inkbirdAddress = device.address
                        Log.i(TAG, "Inkbird already bonded!")
                        break
                    }
                }
                if (!microbitBonded && !inkbirdBonded) {
                    Log.w(TAG, "Not bonded to any of accepted devices!")
                    xmlBinding.scanButton.isEnabled = true
                }
            }
            else {
                Log.i(TAG, "No bonded devices!")
                xmlBinding.scanButton.isEnabled = true
            }
        } else {
            Log.e(TAG, "Bluetooth or location not enabled (onCreate)!")
            xmlBinding.scanButton.isEnabled = true
        }


        // set onClickListener for scan button
        xmlBinding.scanButton.setOnClickListener {
            if (bluetoothEnabled && locationEnabled) {
                deviceManager.associate(pairingRequest,
                    object :
                        CompanionDeviceManager.Callback() { // called when a device matching the filter is found, launch the intentSender so the user can select the device they want to pair with
                        @Deprecated("Deprecated in Java")
                        override fun onDeviceFound(chooserLauncher: IntentSender) {

                            Log.d(TAG, "onDeviceFound")
                            startIntentSenderForResult(chooserLauncher, Defines.SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
                        }

                        override fun onFailure(error: CharSequence?) {

                            Log.e(TAG, "Scanning for devices failed with error: $error")
                        }
                    }, null
                )
            }
            else {
                Toast.makeText(this, "Bluetooth or location not enabled!", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Bluetooth or location not enabled!")
            }
        }

        xmlBinding.scanButton.isEnabled = false // initial disabling

        //set onClickListener for connect gatt button
        xmlBinding.gattButton.setOnClickListener {

            if (microbitConnected || inkbirdConnected) {
                gatt.disconnect()
            } else if (bluetoothEnabled && locationEnabled) {
                if (microbitSelected) {
                    if (microbitBonded) {
                        Log.i(TAG, "Creating gatt connection to micro:bit...")
                        xmlBinding.gattButton.isEnabled = false
                        xmlBinding.selection.isEnabled = false
                        setupGattConnection(microbitAddress, this)
                    } else {
                        Log.w(TAG, "micro:bit not bonded!")
                        Toast.makeText(this, "micro:bit not bonded, please scan for devices!", Toast.LENGTH_LONG).show()
                        xmlBinding.scanButton.isEnabled = true
                    }
                } else if (inkbirdSelected) {
                    if (inkbirdBonded) {
                        Log.i(TAG, "Creating gatt connection to Inkbird...")
                        xmlBinding.gattButton.isEnabled = false
                        xmlBinding.selection.isEnabled = false
                        setupGattConnection(inkbirdAddress, this)
                    } else {
                        Log.w(TAG, "Inkbird not bonded!")
                        Toast.makeText(this, "Inkbird not bonded, please scan for devices!", Toast.LENGTH_LONG).show()
                        xmlBinding.scanButton.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "Bluetooth or location not enabled!", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Bluetooth or location not enabled!")
            }
        }

        xmlBinding.gattButton.isEnabled = false // initial disabling


        //set onClickListener for receive data button
        xmlBinding.receiveDataButton.setOnClickListener {

            if (microbitConnected) {
                showReceiveOptionsDialogMicrobit()
            } else if (inkbirdConnected) {
                showReceiveOptionsDialogInkbird()
            }
        }

        xmlBinding.receiveDataButton.isEnabled = false // initial disabling


        //set onClickListener for send data button
        xmlBinding.sendDataButton.setOnClickListener {

            showSendOptionsDialogMicrobit()
        }

        xmlBinding.sendDataButton.isEnabled = false // initial disabling
    }


    private fun checkPermissions(permissions: Array<out String>, requestCode: Int) {

        // iterate through required permissions and request them if needed
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

        // check if permissions were granted
        if (requestCode == Defines.BLUETOOTH_PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty()) {
            for (item in grantResults) {
                if (item == PackageManager.PERMISSION_DENIED) {
                    granted = false
                    break;
                }
            }
            if (granted) {
                Log.i(TAG, "Bluetooth permissions granted!")
            } else {
                Log.i(TAG, "Bluetooth permissions denied!")
            }
        } else if (requestCode == Defines.LOCATION_PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty()) {
            for (item in grantResults) {
                if (item == PackageManager.PERMISSION_DENIED) {
                    granted = false
                }
            }
            if (granted) {
                Log.i(TAG, "Location permissions granted!")
            } else {
                Toast.makeText(this, "Location permissions denied! Please activate location permissions in settings!", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Location permissions denied!")
            }
        } else {
            Log.e(TAG, "Failed to give permissions!")
        }
    }


    @SuppressLint("MissingPermission") // permissions already granted before
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {

            // check result of selection of device to bond with
            Defines.SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> { // the user chose to pair the app with micro:bit or Inkbird
                    val scanResult: ScanResult? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    if (scanResult?.device?.createBond() == true) {
                        Log.i(TAG, "Initiating device bonding...")
                    }
                    else {
                        if (scanResult != null) {
                            if (microbitBonded && scanResult.device.name.contains("BBC micro:bit")) {
                                Log.w(TAG, "Failed to initiate device bonding, micro:bit already bonded!")
                                Toast.makeText(applicationContext, "micro:bit already bonded!", Toast.LENGTH_LONG).show()
                            } else if (inkbirdBonded && scanResult.device.name.contains("Ink@IAM-T1")) {
                                Log.w(TAG, "Failed to initiate device bonding, Inkbird already bonded!")
                                Toast.makeText(applicationContext, "Inkbird already bonded!", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e(TAG, "Failed to initiate device bonding, scanResult is empty!")
                                Toast.makeText(applicationContext, "Failed to initiate device bonding!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e(TAG, "Failed to initiate device bonding!")
                            Toast.makeText(applicationContext, "Failed to initiate device bonding!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }


    @SuppressLint("MissingPermission") // permissions given before
    fun setupGattConnection(deviceAddress:String, context:Context) {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        val gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

                if (status == BluetoothGatt.GATT_SUCCESS) {

                    // check if connected device is accepted device
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "connected to: ${device.name}")
                        if (device.name.contains("BBC micro:bit")) {
                            microbitConnected = true
                            mainHandler.post { // only main/ui thread can access ui/animation elements
                                Toast.makeText(applicationContext, "micro:bit connected!", Toast.LENGTH_LONG).show()
                            }
                        } else if (device.name.contains("Ink@IAM-T1")) {
                            inkbirdConnected = true
                            mainHandler.post {
                                Toast.makeText(applicationContext, "Inkbird connected!", Toast.LENGTH_LONG).show()
                            }
                        }

                        // set appropriate flags
                        mainHandler.post {
                            xmlBinding.gattButton.isEnabled = true
                            xmlBinding.gattButton.text = getString(R.string.gatt_button_text_connected)
                            xmlBinding.gattButton.setBackgroundColor(getColor(R.color.red))
                            xmlBinding.gattStatusText.text = device.name
                            xmlBinding.selection.isEnabled = false

                            xmlBinding.scanButton.isEnabled = false
                        }

                        // set connection priority (for testing)
                        //gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                        //gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) // set connection parameters to achieve a high prio, low latency connection
                        //gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_DCK) // phone too old
                        gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER)
                        gatt?.discoverServices()
                        gatt?.readPhy()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        // set appropriate flags
                        microbitConnected = false // no if-block necessary as there can only be one (dis-)connection
                        inkbirdConnected = false
                        streamActive = false // to stop potential data streaming/coroutine from micro:bit
                        mainHandler.post {
                            Toast.makeText(applicationContext, "Device disconnected!", Toast.LENGTH_LONG).show()
                            xmlBinding.gattStatusText.text = ""
                            xmlBinding.gattButton.text = getString(R.string.gatt_button_text_unconnected)
                            xmlBinding.gattButton.setBackgroundColor(getColor(R.color.bluetooth_blue))
                            xmlBinding.receiveDataButton.isEnabled = false
                            xmlBinding.sendDataButton.isEnabled = false
                            xmlBinding.selection.isEnabled = true
                            xmlBinding.receivedData.text = ""
                        }
                        Log.i(TAG, "disconnected: ${device.name}")
                    }
                }
                else {
                    Log.e(TAG, "gatt operation failed with error: $status")
                    mainHandler.post {
                        Toast.makeText(applicationContext, "Connection failed!", Toast.LENGTH_LONG).show()
                        xmlBinding.gattButton.isEnabled = true
                        xmlBinding.selection.isEnabled = true
                    }
                }
            }

            override fun onServicesDiscovered(paraGatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(paraGatt, status)

                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (discovery of services)

                    // set appropriate flags and show found services in log
                    mainHandler.post {
                        xmlBinding.receiveDataButton.isEnabled = true
                        if (device.name.contains("BBC micro:bit")) {
                            xmlBinding.sendDataButton.isEnabled = true
                        }
                    }
                    for (item in paraGatt!!.services) {
                        Log.i(TAG, "got service with uuid: " + item.uuid)
                        for (item2 in item.characteristics) {
                            Log.i(TAG, "    got characteristic with uuid: " + item2.uuid)
                        }
                    }
                }
                else {
                    mainHandler.post {
                        xmlBinding.receiveDataButton.isEnabled = false
                        xmlBinding.sendDataButton.isEnabled = false
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {

                if (status == BluetoothGatt.GATT_SUCCESS) { // GATT-operation successfull (writing characteristic)
                    Log.i(TAG, "Wrote data to connected device")
                }
                else {
                    Log.e(TAG, "Failed to write data!")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {

                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-operation successfull (reading characteristic)
                    val value = characteristic.getStringValue(0)
                    Log.i(TAG, "Read data from connected device: $value")
                    xmlBinding.receivedData.text = value

                    mainHandler.post {
                        Toast.makeText(applicationContext, "New data received!", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    xmlBinding.receivedData.text = "" // clear data textbox
                    Log.e(TAG, "Failed to read data!")
                }
            }
        }

        // connect to gatt / form connection
        gatt = device.connectGatt(context, false, gattCallback)
    }


    private fun showReceiveOptionsDialogMicrobit() {

        val builder = AlertDialog.Builder(this)
        val options = arrayOf("Device Name", "Model Number", "Firmware Version")

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    readBleData(Defines.MICROBIT_DEVICE_NAME_REQUEST)
                }
                1 -> {
                    readBleData(Defines.MICROBIT_MODEL_NUMBER_REQUEST)
                }
                2 -> {
                    readBleData(Defines.MICROBIT_FIRMWARE_REVISION_REQUEST)
                }
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showReceiveOptionsDialogInkbird() {

        val builder = AlertDialog.Builder(this)
        val options = arrayOf("Device Name")

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    readBleData(Defines.INKBIRD_NAME_REQUEST)
                }
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showSendOptionsDialogMicrobit() {

        val builder = AlertDialog.Builder(this)
        val options = arrayOf("Text", "Smiley", "Stream") //

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    writeBleData(Defines.MICROBIT_WRITE_LED_TEXT_REQUEST, "Text")
                }
                1 -> {
                    writeBleData(Defines.MICROBIT_WRITE_LED_REQUEST, "") //no data needed
                }
                2 -> {
                    writeBleData(Defines.MICROBIT_WRITE_LED_TEXT_REQUEST, "0")
                }
            }
        }.show()
    }


    @SuppressLint("MissingPermission")
    fun readBleData(paraService : Int) {

        lateinit var service : BluetoothGattService
        lateinit var characteristic : BluetoothGattCharacteristic

        when(paraService) {

            Defines.MICROBIT_DEVICE_NAME_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.MICROBIT_GENERIC_ACCESS_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.MICROBIT_DEVICE_NAME))
            }
            Defines.MICROBIT_MODEL_NUMBER_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.MICROBIT_DEVICE_INFORMATION_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.MICROBIT_MODEL_NUMBER))
            }
            Defines.MICROBIT_FIRMWARE_REVISION_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.MICROBIT_DEVICE_INFORMATION_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.MICROBIT_FIRMWARE_REVISION))
            }
            Defines.INKBIRD_NAME_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.INKBIRD_GENERIC_ACCESS_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.INKBIRD_DEVICE_NAME))
            }
        }

        gatt.readCharacteristic(characteristic)
    }


    @SuppressLint("MissingPermission")
    fun writeBleData(paraService: Int, data: String) {

        lateinit var service : BluetoothGattService
        lateinit var characteristic : BluetoothGattCharacteristic

        when(paraService) {

            Defines.MICROBIT_WRITE_LED_TEXT_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.MICROBIT_LED_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.MICROBIT_LED_TEXT))
                characteristic.value = data.toByteArray()
            }
            Defines.MICROBIT_WRITE_LED_REQUEST -> {
                service = gatt.getService(UUID.fromString(Defines.MICROBIT_LED_SERVICE))
                characteristic = service.getCharacteristic(UUID.fromString(Defines.MICROBIT_LED_MATRIX_STATE))
                val byteData = arrayListOf<Byte>(10, 0, 17, 14, 0)
                characteristic.value = byteData.toByteArray()
            }
        }

        if (data != "0") {
            gatt.writeCharacteristic(characteristic)
            streamActive = false
        }
        else {

            // start data stream / coroutine
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

                    // coroutine ends with this function/block, when streamActive isn't true anymore
                    Log.i(TAG, "Coroutine terminated!")
                }
            }
            else {
                Log.w(TAG, "A coroutine already exists!")
            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()

        // receivers have to be unregistered after use to prevent memory leaks
        unregisterReceiver(bondStateReceiver)
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(locationStateReceiver)

        // disconnect gatt/device
        gatt.disconnect()

        // no unbonding due to the lack of an official unbonding method, bonded devices are permanent and independent from the app runtime anyways
        // (bonding and unbonding can also be done in bluetooth settings)
    }
}