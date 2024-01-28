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
    // TODO: activate scan button when selected device is not bonded

    // activate xml binding
    private lateinit var binding:ActivityMainBinding

    val mainHandler = Handler(Looper.getMainLooper())


    // companion object to recall bond/connect states
    companion object {

        var streamActive = false
        var coroutineExists = false

        var microbitSelected = false
        var microbitBonded = false
        var microbitConnected = false
        var microbitAddress : String = ""

        var inkbirdSelected = false
        var inkbirdBonded = false
        var inkbirdConnected = false
        var inkbirdAddress : String = ""
    }


    // permissions needed for companion device binding
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
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) {
                    BluetoothDevice.BOND_BONDING -> {
                        Log.i(TAG, "Bonding...")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        if (device != null) {
                            Log.i(TAG, "Bonded to " + device.name + "!")
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

                            // deactivate scan button
                            binding.scanButton.isEnabled = false
                        }
                    }
                    BluetoothDevice.BOND_NONE -> {
                        if (device != null) {
                            Log.i(TAG, "Failed to bond to ${device.name}!")
                        }
                        binding.receivedData.text = "" // clear data textbox
                        binding.scanButton.isEnabled = true
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // set up device selection menu
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.spinnerItems,
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.selection.adapter = adapter

        binding.selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: android.view.View?, position: Int, id: Long) {

                val selectedDevice = parentView?.getItemAtPosition(position).toString()
                if (selectedDevice == "micro:bit") {
                    microbitSelected = true
                    inkbirdSelected = false
                    binding.gattButton.isEnabled = true
                } else if (selectedDevice == "Inkbird") {
                    microbitSelected = false
                    inkbirdSelected = true
                    binding.gattButton.isEnabled = true
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) { // has to be defined although not used
                ;
            }
        }


        // check required permissions
        checkPermissions(locationPermissions, Defines.LOCATION_PERMISSIONS_REQUEST_CODE) //location permissions have to be requested separately
        checkPermissions(bluetoothPermissions, Defines.BLUETOOTH_PERMISSIONS_REQUEST_CODE)


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

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bondStateReceiver, filter)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager


        // check if micro:bit or Inkbird are already bonded
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    Log.i(TAG, device.name + ' ' + device.address)
                    if (device.name.contains("BBC micro:bit")) {
                        microbitBonded = true
                        binding.scanButton.isEnabled = false
                        microbitAddress = device.address
                        Log.i(TAG, "micro:bit already bonded!")
                        break
                    } else if (device.name.contains("Ink@IAM-T1")) {
                        inkbirdBonded = true
                        binding.scanButton.isEnabled = false
                        inkbirdAddress = device.address
                        Log.i(TAG, "Inkbird already bonded!")
                        break
                    }
                }
                if (!microbitBonded && !inkbirdBonded) {
                    Log.w(TAG, "Not bonded to any of accepted devices!")
                    binding.scanButton.isEnabled = true
                }
            }
            else {
                Log.i(TAG, "No bonded devices!")
                binding.scanButton.isEnabled = true
            }
        } else {
            Log.e(TAG, "Bluetooth or location not enabled!")
            binding.scanButton.isEnabled = true
        }


        // set onClickListener for scan button
        binding.scanButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
                deviceManager.associate(pairingRequest,
                    object :
                        CompanionDeviceManager.Callback() { // called when a device matching the filter is found, launch the intentSender so the user can select the device they want to pair with
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
                Toast.makeText(this, "Bluetooth or location not enabled!", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Bluetooth or location not enabled!")
            }
        }

        //set onClickListener for connect gatt button
        binding.gattButton.setOnClickListener {

            if (microbitConnected || inkbirdConnected) {
                gatt.disconnect()
            } else if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && locationManager.isLocationEnabled) {
                if (microbitSelected) {
                    if (microbitBonded) {
                        Log.i(TAG, "Creating gatt connection to micro:bit...")
                        binding.gattButton.isEnabled = false
                        binding.selection.isEnabled = false
                        setupGattConnection(microbitAddress, this)
                    } else {
                        Log.i(TAG, "micro:bit not bonded!")
                        Toast.makeText(this, "micro:bit not bonded!", Toast.LENGTH_LONG).show()
                    }
                } else if (inkbirdSelected) {
                    if (inkbirdBonded) {
                        Log.i(TAG, "Creating gatt connection to Inkbird...")
                        binding.gattButton.isEnabled = false
                        binding.selection.isEnabled = false
                        setupGattConnection(inkbirdAddress, this)
                    } else {
                        Log.i(TAG, "Inkbird not bonded!")
                        Toast.makeText(this, "Inkbird not bonded!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Bluetooth or location not enabled!", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Bluetooth or location not enabled!")
            }
        }

        binding.gattButton.isEnabled = false


        //set onClickListener for receive data button
        binding.receiveDataButton.setOnClickListener {

            if (microbitConnected) {
                showReceiveOptionsDialogMicrobit()
            } else if (inkbirdConnected) {
                showReceiveOptionsDialogInkbird()
            }
        }

        binding.receiveDataButton.isEnabled = false


        //set onClickListener for send data button
        binding.sendDataButton.setOnClickListener {

            showSendOptionsDialogMicrobit()
        }

        binding.sendDataButton.isEnabled = false
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

        // case of bluetooth permissions
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
        }
        // case of location permissions
        else if (requestCode == Defines.LOCATION_PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty()) {
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
        // case of fail
        } else {
            Log.e(TAG, "Failed to give permissions!")
        }
    }


    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission") // permissions already granted before
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            Defines.SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> { // the user chose to pair the app with micro:bit or Inkbird
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


    @SuppressLint("MissingPermission") // permissions given before
    fun setupGattConnection(deviceAddress:String, context:Context) {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        val gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
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
                        mainHandler.post {
                            binding.gattButton.isEnabled = true
                            binding.gattButton.text = getString(R.string.gatt_button_text_connected)
                            binding.gattButton.setBackgroundColor(getColor(R.color.red))
                            binding.gattStatusText.text = device.name
                            binding.selection.isEnabled = false
                        }
                        Log.i(TAG, "BluetoothDevice CONNECTED: ${device.name}")
                        //gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                        //gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) // set connection parameters to achieve a high prio, low latency connection
                        //gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_DCK) // phone too old
                        gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER)
                        gatt?.discoverServices()
                        gatt?.readPhy()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        microbitConnected = false // no if necessary as there can only be one (dis-)connection
                        inkbirdConnected = false
                        streamActive = false // to stop potential data streaming/coroutine from micro:bit
                        mainHandler.post {
                            binding.gattStatusText.text = ""
                            binding.gattButton.text = getString(R.string.gatt_button_text_unconnected)
                            binding.gattButton.setBackgroundColor(getColor(R.color.bluetooth_blue))
                            binding.receiveDataButton.isEnabled = false
                            binding.sendDataButton.isEnabled = false
                            Toast.makeText(applicationContext, "Device disconnected!", Toast.LENGTH_LONG).show()
                            binding.selection.isEnabled = true
                            binding.receivedData.text = ""
                        }
                        Log.i(TAG, "BluetoothDevice DISCONNECTED: ${device.name}")
                    }
                }
                else {
                    Log.e(TAG, "gatt operation failed with error: $status")
                    mainHandler.post {
                        binding.gattButton.isEnabled = true
                        binding.selection.isEnabled = true
                        Toast.makeText(applicationContext, "Connection failed!", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onServicesDiscovered(paraGatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(paraGatt, status)

                if (status == BluetoothGatt.GATT_SUCCESS) { //GATT-Operation successfull (discovery of services)
                    mainHandler.post {
                        binding.receiveDataButton.isEnabled = true
                        if (device.name.contains("BBC micro:bit")) {
                            binding.sendDataButton.isEnabled = true
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
                        binding.receiveDataButton.isEnabled = false
                        binding.sendDataButton.isEnabled = false
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
                    binding.receivedData.text = value

                    mainHandler.post {
                        Toast.makeText(applicationContext, "New data received!", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    binding.receivedData.text = "" // clear data textbox
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

        // bondStateReceiver has to be unregistered after use to prevent memory leaks
        unregisterReceiver(bondStateReceiver)

        // disconnect gatt/device
        gatt.disconnect()

        // no unbonding due to the lack of an official unbonding method, bonded devices are permanent and independent from the app runtime anyways
        // (bonding and unbonding can also be done in bluetooth settings)
    }
}