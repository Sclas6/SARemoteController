package com.example.saremotecontroller

import android.R.attr
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity


class OffLineActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var testSeekBar: SeekBar

    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    fun sendCrl(speed:Int,mode:Int,device:Int){
        var motor = (mode shl 7 and 0x80)
        motor = (motor or speed)
            val sendByte = byteArrayOf(0x02, 0x01, motor.toByte())
            if (bleService?.getStatus() != false) {
                bleService?.writeValue(sendByte)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_off_line)

        val bleIntent = Intent(this, BLEService::class.java)
        bindService(bleIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        testButton = findViewById(R.id.test_button)
        testSeekBar = findViewById(R.id.testSeekBar)

        testButton.setOnClickListener {
            bleService?.writeValue(byteArrayOf(0x02,0x01,0x0f))
        }

        testSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                testButton.text=i.toString()
                //bleService?.writeValue(byteArrayOf(0x02,0x01,0x0f))
                sendCrl(i,1,0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
}