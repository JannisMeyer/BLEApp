package com.example.bleapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bleapp.adapter.BleAdapter
import com.example.bleapp.databinding.ActivityMainBinding

//TODO: Look at handler and callback (not working)
//TODO: Look at permission requesting (not working)

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    private lateinit var adapter : BleAdapter

    private val bluetoothAdapter : BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler()

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH,
                                      Manifest.permission.BLUETOOTH_SCAN,
                                      Manifest.permission.BLUETOOTH_CONNECT,
                                      Manifest.permission.BLUETOOTH_ADMIN,
                                      Manifest.permission.BLUETOOTH_ADVERTISE,
                                      Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                      Manifest.permission.ACCESS_FINE_LOCATION)
    private val blePermissionsRequestCode = 888

    private var devices : MutableList<BluetoothDevice> = arrayListOf()

    // Device scan callback
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            Toast.makeText(applicationContext, "onScanResult()", Toast.LENGTH_LONG).show()
            if (result.device != null) {
                devices.add(result.device)
                adapter.notifyDataSetChanged()
                Toast.makeText(applicationContext, "Devices found!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "No devices found!", Toast.LENGTH_LONG).show()
            }

        }
    }

    // Stops scanning after 10 seconds.
    private val scanPeriod: Long = 10000
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //connect this activity with corresponding display
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView handling
        val recyclerView = binding.devicesRecyclerView as RecyclerView
        adapter = BleAdapter(devices)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        //set OnClickListener for BLE-Button
        binding.scanButton.setOnClickListener {
            checkPermissions(permissions, blePermissionsRequestCode)
        }
    }

    @SuppressLint("MissingPermission") //permissions are checked before call of this method
    @RequiresApi(Build.VERSION_CODES.S)
    private fun scanBleDevice() {

        Toast.makeText(applicationContext, "scanBleDevice()", Toast.LENGTH_LONG).show()
        if (!scanning) { //Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                Toast.makeText(applicationContext, "handler", Toast.LENGTH_LONG).show()
                bluetoothLeScanner.stopScan(leScanCallback)
                binding.scanButton.isEnabled = true
            }, scanPeriod)
            Toast.makeText(applicationContext, "after handler", Toast.LENGTH_LONG).show()
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
            //Toast.makeText(applicationContext, "Started scanning!", Toast.LENGTH_LONG).show()
            binding.scanButton.isEnabled = false
        } else {
            Toast.makeText(applicationContext, "else", Toast.LENGTH_LONG).show()
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            binding.scanButton.isEnabled = true
            Toast.makeText(applicationContext, "Scanning done!", Toast.LENGTH_LONG).show()
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

        if (requestCode == blePermissionsRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Scanning for ble devices...", Toast.LENGTH_LONG).show()
            scanBleDevice()
        } else {
            Toast.makeText(this, "Permissions denied, no ble-scanning possible!", Toast.LENGTH_LONG).show()
        }
    }
}