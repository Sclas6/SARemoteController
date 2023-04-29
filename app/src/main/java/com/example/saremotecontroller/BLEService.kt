package com.example.saremotecontroller

import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log


class BLEService : Service() {
    private val bleScanner=BLEScanner(this)

    private val binder = MyServiceBinder()
    inner class MyServiceBinder : Binder() {
        fun getService(): BLEService = this@BLEService
    }

    fun connectBLE() {
        Log.d(TAG,"try connect")
        bleScanner.startScan()
    }

    fun getStatus():Boolean{
        return bleScanner.getStatus()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"Service Created")
    }


    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}