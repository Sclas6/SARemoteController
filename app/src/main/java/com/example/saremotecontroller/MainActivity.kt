package com.example.saremotecontroller

import android.Manifest
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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import java.security.AccessController.getContext


private const val REQUEST_ENABLE_BT = 1
private const val MY_REQUEST_CODE: Int = 2
private var isGpsEnabled: Boolean = false

class MainActivity : AppCompatActivity() {

    private lateinit var button: Button
    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG,"Service connected")
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
            // BLEデバイスのスキャンを開始する
            bleService?.connectBLE()
            Log.d(TAG,bleService?.getStatus().toString())
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

        //connectButton.isEnabled=false
        //offlineButton.isEnabled=false

        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

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
        }else{
            Toast.makeText(applicationContext, "GPS OK", Toast.LENGTH_LONG).show()
        }

        connectButton.setOnClickListener {
            if(bleService!=null&& bleService!!.getStatus()){
                Thread {
                    val message = connectToServer()
                    //val message = if (connected) "Connected to server" else "Failed to connect to server"
                    //Toast.makeText(this@MainActivity, message.contentToString(), Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).post {
                        //Toast.makeText(this@MainActivity, message.contentToString(), Toast.LENGTH_SHORT).show()
                        if (message[0] != "Failed to connect to server") {
                            val intent = Intent(this, RoomList::class.java)
                            intent.putExtra("roomList", message)
                            //intent.putExtras("ble",bleService)
                            startActivity(intent)
                        }else{
                            Toast.makeText(this@MainActivity, "サーバへの接続に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }else {
                Toast.makeText(this, "BlueToothデバイスに接続してください", Toast.LENGTH_SHORT).show()
            }
        }
        offlineButton.setOnClickListener {
            if(bleService!=null&& bleService!!.getStatus()){
                //Toast.makeText(this, "接続されています", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, OffLineActivity::class.java)
                startActivity(intent)
                //intent.putExtra("roomList", message)
                //intent.putExtras("ble",bleService)
                //startActivity(intent)
                //Toast.makeText(this, bleService?.getDevice(), Toast.LENGTH_SHORT).show()

                //bleService?.writeValue(byteArrayOf(0x02,0x01,0x0f))
            }else {
                Toast.makeText(this, "BlueToothデバイスに接続してください", Toast.LENGTH_SHORT).show()
            }
        }
        bleSearchButton.setOnClickListener(View.OnClickListener {
            Log.d("clickUpdateButton", "Update Button Clicked!!")
            val intent = Intent(this, BLEService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "Service bound.")
        })
    }



    private fun connectToServer(): Array<String> {
        try {
            val socket = Socket("192.168.11.14", 19071)
            val outputStream: OutputStream = socket.getOutputStream()
            val printWriter = PrintWriter(outputStream, true)
            printWriter.println("4 show")
            val inputStream = InputStreamReader(socket.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            val message = bufferedReader.readLine()
            socket.close()
            Log.d("APP",message)
            return checkMsg(message)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return arrayOf("Failed to connect to server")
    }

    private fun checkMsg(msg: String): Array<String>{
        val list = msg.split(" ").toMutableList()
        var dataLen= list.size.minus(2)
        for(i in list.subList(1,list.size)){
            dataLen+=i.length
        }
        if(list[0].toInt()==dataLen){
            for(i in 0 until list.size-1){
                list[i]=list[i+1]
            }
            list.removeLast()
            return list.toTypedArray()
        }
        return arrayOf()
    }



}