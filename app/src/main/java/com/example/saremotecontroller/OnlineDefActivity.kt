package com.example.saremotecontroller

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.TextView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.Exception
import java.net.Socket

class OnlineDefActivity : AppCompatActivity() {
    private lateinit var testText:TextView

    private var value: Array<String>? = null
    private var th: Thread? = null

    private var socket:Socket? = null
    private var printWriter:PrintWriter? = null

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

        testText = findViewById(R.id.testText)
        testText.text = value.contentToString()

        th = Thread{
            defLoop(value!![0], value!![1], value!![2])
        }
        th!!.start()
        //sleep(1000)
        //sendValue("exit")

    }

    private fun defLoop(name:String, user:String, pwd:String){
        try {
            socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket!!.getOutputStream()
            printWriter = PrintWriter(outputStream, true)
            if(pwd==""){
                Log.d(ContentValues.TAG,msgMng.shapeMsg(String.format("create %s %s",name,user)))
                printWriter!!.println(msgMng.shapeMsg(String.format("create %s %s",name,user)))
            }else{
                Log.d(ContentValues.TAG,msgMng.shapeMsg(String.format("create %s %s %s",name,user,pwd)))
                printWriter!!.println(msgMng.shapeMsg(String.format("create %s %s %s",name,user,pwd)))
            }
            val inputStream = InputStreamReader(socket!!.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            var message: String
            while(true){
                message = bufferedReader.readLine()
                try{
                    if(msgMng.checkMsg(message)[0]=="ctr"){
                        val ctr = msgMng.checkMsg(message)[1]
                        val sendInt = ctr.substring(1,ctr.length-1).split(',')
                        bleService!!.writeValue(byteArrayOf(sendInt[0].toInt().toByte(),sendInt[1].toInt().toByte(),sendInt[2].toInt().toByte()))
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