package com.example.bleapp.adapter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import com.example.bleapp.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView

class BleAdapter(private val deviceData: List<BluetoothDevice>) : RecyclerView.Adapter<BleAdapter.ViewHolder>() {

    private lateinit var context : Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        //inflate view and return view holder
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.device_item, parent, false)

        context = parent.context

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {

        //return number of items the recycler view has to display
        return deviceData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        //display concrete item of recycler view as title of corresponding recipe
        val item: BluetoothDevice = deviceData[position]
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Permissions denied, no access on BLE device!", Toast.LENGTH_LONG).show()
            return
        }
        holder.deviceNameView.text = item.name
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        //connect recycler view item with recipe title as button
        var deviceNameView: TextView = itemView.findViewById(R.id.device_name)

        init {

            //normal onClickListener referred to onClick-method
            deviceNameView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            Toast.makeText(context, "Device clicked!", Toast.LENGTH_LONG).show()
        }
    }
}