package com.example.saremotecontroller

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.lang.Exception
import java.net.Socket
import kotlin.math.abs

class OnlineDefActivity : AppCompatActivity() {

    private var value: Array<String>? = null
    private var th: Thread? = null
    private var socket:Socket? = null

    private lateinit var rotateRight:ImageView
    private lateinit var rotateLeft:ImageView
    private lateinit var rotateText:TextView
    private lateinit var roomInfoText: TextView
    private var rotateSpeed = 0

    private var bleService: BLEService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }
        override fun onServiceDisconnected(className: ComponentName) {
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        value = intent.getStringArrayExtra("command")
        setContentView(R.layout.activity_online_def)
        val bleIntent = Intent(this, BLEService::class.java)
        bindService(bleIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        rotateText = findViewById(R.id.rotateView)
        rotateText.text = "0%"
        rotateRight = findViewById(R.id.rotate_Right)
        rotateLeft = findViewById(R.id.rotate_Left)
        roomInfoText = findViewById(R.id.roomInfoText)
        roomInfoText.text = "Room:  ${value!![0]}\nUser:  <入室待ち>"
        rotateLeft.alpha = 0f
        rotateThread.start()
        th = Thread{
            defLoop(value!![0], value!![1], value!![2])
        }
        th!!.start()
    }
    private val rotateThread = Thread{
        //rotateSpeed:0~100
        //SA MAX: rotation:5 sleep:9
        while(true){
            if(abs(rotateSpeed) >=10){
                Handler(Looper.getMainLooper()).post{
                    rotateRight.rotation += rotateSpeed / 10
                    rotateLeft.rotation += rotateSpeed / 10
                }
                sleep(18)
            }else if(abs(rotateSpeed)>=5){
                Handler(Looper.getMainLooper()).post{
                    rotateRight.rotation += rotateSpeed / 5
                    rotateLeft.rotation += rotateSpeed / 5
                }
                sleep(32)
            }else{
                Handler(Looper.getMainLooper()).post{
                    rotateRight.rotation += rotateSpeed / 2
                    rotateLeft.rotation += rotateSpeed / 2
                }
                sleep(90)
            }
        }
    }
    private fun rotateImage(mode: Int, speed: Int){
        rotateSpeed = speed * mode
        if(mode<0){
            rotateRight.alpha = 0f
            rotateLeft.alpha = 1f
        }else{
            rotateRight.alpha = 1f
            rotateLeft.alpha = 0f
        }
        if(speed == 0){
            rotateRight.alpha = 0f
            rotateLeft.alpha = 0f
        }
    }
    @SuppressLint("SetTextI18n")
    private fun defLoop(name:String, user:String, pwd:String){
        try {
            socket = scMng.connectServer(address_ip, 19071, null)
            if(pwd == ""){
                Log.d(ContentValues.TAG,String.format("create %s %s",name,user))
                scMng.sendValue(socket!!,String.format("create %s %s",name,user))
            }else{
                Log.d(ContentValues.TAG,String.format("create %s %s %s", name, user, pwd))
                scMng.sendValue(socket!!,(String.format("create %s %s %s", name, user, pwd)))
            }
            var message: Array<String>
            while(true){
                message = scMng.readValue(socket!!, BLOCKING)
                try{
                    if(message[0] == "matching"){
                        Handler(Looper.getMainLooper()).post{
                            roomInfoText.text = "Room:  ${value!![0]}\nUser:  ${message[1]}"
                            AlertDialog.Builder(this)
                                .setTitle("マッチング")
                                .setMessage("${message[1]}さんが入室しました")
                                .setPositiveButton("OK") { _, _ ->
                                }.show()
                        }
                    }else if(message[0] == "ctr"){
                        val ctr = message[1]
                        val sendInt = ctr.substring(1,ctr.length-1).split(',')
                        bleService!!.writeValue(byteArrayOf(sendInt[0].toInt().toByte(), sendInt[1].toInt().toByte(), sendInt[2].toInt().toByte()))
                        val speed = (sendInt[2].toInt() and 0xFF) and 0x7f
                        Handler(Looper.getMainLooper()).post{
                            rotateText.text = "$speed%"
                            rotateImage(if (sendInt[2].toInt()<0) -1 else 1, speed)
                        }
                    }else if(message[0] == "err"){
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(this, "通信が切断されました", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }catch (_:Exception){
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        scMng.sendValue(socket!!,"exit")
        sleep(1000)
        socket!!.close()
        th?.interrupt()
    }
}