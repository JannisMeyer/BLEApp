package com.example.bleapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/*class BonndStateReceiver : BroadcastReceiver() {
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
                        MainActivity.bonded = true
                        if (context != null) {
                            MainActivity.setupGattConnection(device.name, context)
                        }
                    }
                }
                BluetoothDevice.BOND_NONE -> {
                    Toast.makeText(context, "Failed to bond!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}*/
