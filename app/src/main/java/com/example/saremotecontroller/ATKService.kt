package com.example.saremotecontroller

import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
//import java.io.BufferedReader
import java.io.IOException
//import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket

class ATKService : Service() {
    private val msgMng = MsgManager()

    private var printWriter:PrintWriter? = null
    private var socket:Socket? = null


    private val binder = MyServiceBinder()
    inner class MyServiceBinder : Binder() {
        fun getService(): ATKService = this@ATKService
    }
    override fun onCreate() {
        Thread{
            connectToServer()
        }.start()
        Log.d(TAG,"Service Created")
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    private fun connectToServer(): Array<String> {
        try {
            socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket!!.getOutputStream()
            printWriter = PrintWriter(outputStream, true)
            //val inputStream = InputStreamReader(socket!!.getInputStream())
            //val bufferedReader = BufferedReader(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return arrayOf("Failed to connect to server")
    }
    fun sendValue(msg:String){
        Thread{
            //Log.d(TAG,msg)
            printWriter?.println(msgMng.shapeMsg(msg))
        }.start()
    }
    fun chkCon() :Boolean{
        return socket != null
    }

    override fun onDestroy() {
        super.onDestroy()
        sendValue(msgMng.shapeMsg("exit"))
        sendValue(msgMng.shapeMsg("quit"))
        socket!!.close()
    }
}