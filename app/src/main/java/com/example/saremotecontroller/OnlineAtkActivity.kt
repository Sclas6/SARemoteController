package com.example.saremotecontroller

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView

class OnlineAtkActivity : AppCompatActivity() {
    private lateinit var textMaxSpeed: TextView

    private var atkService: ATKService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG,"ATKService connected")
            val binder = service as ATKService.MyServiceBinder
            atkService = binder.getService()
            // BLEデバイスのスキャンを開始する
            atkService?.sendValue("join Room1 a")
            //Log.d(TAG,atkService?.getStatus().toString())
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    private var msgMng: MsgManager = MsgManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val value = intent.getStringArrayExtra("command")

        setContentView(R.layout.activity_online_atk)
        val test = msgMng.checkMsg("8 pika chu")
        Log.d(TAG, test.contentToString())

        textMaxSpeed = findViewById(R.id.textSpeedCtr2)
        textMaxSpeed.text=value.contentToString()

        Log.d("clickUpdateButton", "Update Button Clicked!!")
        val intent = Intent(this, ATKService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Service bound.")
    }

    override fun onStop() {
        super.onStop()
        stopService(Intent(this,ATKService::class.java))
    }
}