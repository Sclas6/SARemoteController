package com.example.saremotecontroller

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.TextView

/*パスワード未実装*/

class OnlineAtkActivity : AppCompatActivity() {
    private lateinit var textMaxSpeed: TextView

    private var atkService: ATKService? = null
    private var value: Array<String>? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG,"ATKService connected")
            val binder = service as ATKService.MyServiceBinder
            atkService = binder.getService()
            // BLEデバイスのスキャンを開始する
            //sleep(2000)
            //atkService?.sendValue(value)
            Log.d(TAG, "[ATK]${value.contentToString()}")
            sleep(100)
            if(value!![4]=="None"){
                atkService?.sendValue("${value!![1]} ${value!![2]} ${value!![3]}");
            }
            //Log.d(TAG,atkService?.getStatus().toString())
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    private var msgMng: MsgManager = MsgManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        value = intent.getStringArrayExtra("command")

        setContentView(R.layout.activity_online_atk)
        val test = msgMng.checkMsg("8 pika chu")
        Log.d(TAG, test.contentToString())

        textMaxSpeed = findViewById(R.id.textSpeedCtr2)
        textMaxSpeed.text=value.contentToString()

        Log.d("clickUpdateButton", "Update Button Clicked!!")
        val intent = Intent(this, ATKService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Service bound.")
        //sleep(2000)
        Log.d(TAG,"[ATK]"+atkService.toString())
        //atkService?.sendValue("join Room1 a")

    }

    override fun onStop() {
        super.onStop()
        stopService(Intent(this,ATKService::class.java))
    }
}