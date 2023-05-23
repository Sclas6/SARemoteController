@file:Suppress("DEPRECATION")

package com.example.saremotecontroller

import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock.sleep
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

private var isGpsEnabled: Boolean = false
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG,"Service connected")
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
            bleService?.connectBLE()
            Log.d(TAG,bleService?.getStatus().toString())
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.isIndeterminate = true
            progressDialog.setMessage("Searching...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
            Thread{
                while(true){
                    if(bleService!!.getStatus()){
                        progressDialog.cancel()
                        break
                    }
                    sleep(2000)
                }
            }.start()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }
    private lateinit var connectButton: Button
    private lateinit var offlineButton: Button
    private lateinit var bleSearchButton: Button

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        connectButton = findViewById(R.id.connect_button)
        bleSearchButton = findViewById(R.id.button2)
        offlineButton = findViewById(R.id.button)

        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(applicationContext, "Permission ERROR", Toast.LENGTH_LONG).show()
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if(!isGpsEnabled){
            startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), MY_REQUEST_CODE)
        }
        connectButton.setOnClickListener {
            Thread {
                val message = connectToServer()
                Handler(Looper.getMainLooper()).post {
                    if (message[0] != "Failed to connect to server") {
                        val intent = Intent(this, RoomList::class.java)
                        intent.putExtra("roomList", message)
                        startActivity(intent)
                    }else{
                        Toast.makeText(this@MainActivity, "サーバへの接続に失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
        offlineButton.setOnClickListener {
            if(bleService!=null&& bleService!!.getStatus()){
                val intent = Intent(this, OffLineActivity::class.java)
                startActivity(intent)
            }else {
                Toast.makeText(this, "BlueToothデバイスに接続してください", Toast.LENGTH_SHORT).show()
            }
        }
        bleSearchButton.setOnClickListener {
            DEVICE = DEVICES[UFO_SA]
            val intent = Intent(this, BLEService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    private fun connectToServer(): Array<String> {
        val socket = scMng.connectServer(address_ip, 19071, 5000)
        if(socket != null){
            scMng.sendValue(socket,"show")
            val message = scMng.readValue(socket, BLOCKING)
            socket.close()
            return message
        }
        return arrayOf("Failed to connect to server")
    }
}