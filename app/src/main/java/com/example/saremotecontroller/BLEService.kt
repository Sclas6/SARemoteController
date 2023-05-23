package com.example.saremotecontroller

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

private var connected=false
private var bluetoothGatt: BluetoothGatt? = null
private var saDevice: BluetoothDevice? = null

@Suppress("DEPRECATION")
class BLEService : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: BLEScanCallback? = null
    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }
    private val binder = MyServiceBinder()
    inner class MyServiceBinder : Binder() {
        fun getService(): BLEService = this@BLEService
    }
    fun connectBLE() {
        Log.d(TAG,"Scan Started")
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        scanCallback = BLEScanCallback()
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG,"START SCANNER")
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        }
    }

    inner class BLEScanCallback : ScanCallback() {

        private val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        // デバイスに接続された場合の処理をここに記述
                        connected=true
                        saDevice=gatt.device
                        bluetoothGatt=gatt
                        if (ActivityCompat.checkSelfPermission(
                                this@BLEService,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            bluetoothGatt?.discoverServices()
                        }
                        Log.d(TAG,"[Gatt Connected]")


                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connected=false
                        if (ActivityCompat.checkSelfPermission(MainActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d(TAG,"[Gatt Closed]")
                            //bluetoothGatt?.close()
                            //bluetoothGatt=null
                        }

                    }
                    else -> {
                        // その他の状態変化に対する処理をここに記述
                    }
                }
            }
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)

            }
        }
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val device = result.device
                if (device.name== DEVICE){
                    Log.d(TAG,device.name.toString())
                    if (ActivityCompat.checkSelfPermission(
                            this@BLEService,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        device.connectGatt(this@BLEService,false,gattCallback)
                    }
                }
                if (ActivityCompat.checkSelfPermission(
                        this@BLEService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                val uuids =  device.uuids
                uuids?.forEach { uuid ->
                    Log.d(TAG,"Found any device")
                    if (uuid.uuid == UUID.fromString("40EE1111-63EC-4B7F-8CE7-712EFD55B90E")) {
                        // Found device with matching UUID, do something with it
                        Log.d(TAG,"FOUND!!!!!")
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
        }
    }

    fun getStatus():Boolean{
        return connected
    }
    /*
    fun getGatt():String{
        return bluetoothGatt.toString()
    }
*/
    fun writeValue(value:ByteArray){
        if(connected){
            if (ActivityCompat.checkSelfPermission(
                    this@BLEService,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val service = bluetoothGatt?.getService(UUID.fromString(SERVICE_UUID))
                val characteristic = service?.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                characteristic?.value = value
                bluetoothGatt?.writeCharacteristic(characteristic).toString()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"Service Created")
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}