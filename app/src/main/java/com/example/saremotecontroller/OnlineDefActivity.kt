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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.Exception
import java.net.Socket
import kotlin.math.abs

class OnlineDefActivity : AppCompatActivity() {

    private var value: Array<String>? = null
    private var th: Thread? = null
    private var socket:Socket? = null
    private var printWriter:PrintWriter? = null

    private lateinit var rotateRight:ImageView
    private lateinit var rotateLeft:ImageView
    private lateinit var rotateText:TextView
    private var rotateSpeed = 0

    private val msgMng = MsgManager()
    private var bleService: BLEService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }
        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

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
            socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket!!.getOutputStream()
            printWriter = PrintWriter(outputStream, true)
            if(pwd == ""){
                Log.d(ContentValues.TAG,msgMng.shapeMsg(String.format("create %s %s",name,user)))
                printWriter!!.println(msgMng.shapeMsg(String.format("create %s %s",name,user)))
            }else{
                Log.d(ContentValues.TAG,msgMng.shapeMsg(String.format("create %s %s %s", name, user, pwd)))
                printWriter!!.println(msgMng.shapeMsg(String.format("create %s %s %s", name, user, pwd)))
            }
            val inputStream = InputStreamReader(socket!!.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            var message: String
            while(true){
                message = bufferedReader.readLine()
                try{
                    if(msgMng.checkMsg(message)[0] == "ctr"){
                        val ctr = msgMng.checkMsg(message)[1]
                        val sendInt = ctr.substring(1,ctr.length-1).split(',')
                        bleService!!.writeValue(byteArrayOf(sendInt[0].toInt().toByte(), sendInt[1].toInt().toByte(), sendInt[2].toInt().toByte()))
                        val speed = (sendInt[2].toInt() and 0xFF) and 0x7f
                        rotateText.text = "$speed%"
                        rotateImage(if (sendInt[2].toInt()<0) -1 else 1, speed)
                    }
                }catch (_:Exception){
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun sendValue(value:String){
        Thread{
            printWriter!!.println(msgMng.shapeMsg(value))
            Log.d("APP","sendEXIT")
        }.start()
    }
    override fun onDestroy() {
        super.onDestroy()
        sendValue("exit")
        sleep(1000)
        socket!!.close()
        th?.interrupt()
    }
}