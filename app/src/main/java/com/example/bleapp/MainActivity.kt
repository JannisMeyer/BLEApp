package com.example.bleapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bleapp.adapter.BleAdapter
import com.example.bleapp.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.Executor
import java.util.regex.Pattern

//(TODO: Look at handler and callback (not working)
//TODO: Look at permission requesting (not working))
//TODO: test/implement onActivityResult() from deviceManager callback, manage requirements properly

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    private val permissions = arrayOf(Manifest.permission.BLUETOOTH,
                                      Manifest.permission.BLUETOOTH_ADMIN,
                                      /*Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                      Manifest.permission.ACCESS_FINE_LOCATION,
                                      Manifest.permission.ACCESS_COARSE_LOCATION*/)


    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private val selectDeviceRequestCode = 1
    private val blePermissionsRequestCode = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions(permissions, blePermissionsRequestCode)

        //connect this activity with corresponding display
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
            // Match only Bluetooth devices whose name matches the pattern.
            //.setNamePattern(Pattern.compile("BBC micro:bit CMSIS-DAP"))
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            // Find only devices that match this request filter.
            .addDeviceFilter(deviceFilter)
            // Stop scanning as soon as one device matching the filter is found.
            .setSingleDevice(false)
            .build()

        deviceManager.associate(pairingRequest,
            object : CompanionDeviceManager.Callback() {
                // Called when a device is found. Launch the IntentSender so the user
                // can select the device they want to pair with.
                @Deprecated("Deprecated in Java")
                override fun onDeviceFound(chooserLauncher: IntentSender) {
                    startIntentSenderForResult(chooserLauncher,
                        selectDeviceRequestCode, null, 0, 0, 0)
                }

                override fun onFailure(error: CharSequence?) {
                    Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
                }
            }, null)
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission") //Permissions already granted before
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            selectDeviceRequestCode -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.createBond()
                    // Continue to interact with the paired device.
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPermissions(permissions: Array<out String>, requestCode: Int) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            shouldShowRequestPermissionRationale("test")
            ActivityCompat.requestPermissions(this, permissions, requestCode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var granted = true

        var test = 0

        if (requestCode == blePermissionsRequestCode && grantResults.isNotEmpty() /*&& grantResults[0] == PackageManager.PERMISSION_GRANTED*/) {
            for (item in grantResults) {
                if(item == PackageManager.PERMISSION_DENIED) {
                    granted = false
                    Toast.makeText(this, item.toString()+test.toString(), Toast.LENGTH_LONG).show()
                    break;
                }
                test++
            }
            if (granted) {
                Toast.makeText(this, "Scanning for ble devices...", Toast.LENGTH_LONG).show()
                //...
            }
            else {
                Toast.makeText(this, "Permissions denied!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Failed giving permissions!", Toast.LENGTH_LONG).show()
        }
    }
}