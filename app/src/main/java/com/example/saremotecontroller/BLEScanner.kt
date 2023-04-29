package com.example.saremotecontroller

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

private const val TARGET_DEVICE_NAME = "UFOSA"
private const val SERVICE_UUID = "40EE1111-63EC-4B7F-8CE7-712EFD55B90E"
var connected=false

private val gattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                // デバイスに接続された場合の処理をここに記述
                connected=true
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                connected=false
            }
            else -> {
                // その他の状態変化に対する処理をここに記述
            }
        }
    }
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // GATTサービスが見つかった場合の処理をここに記述
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // キャラクタリスティックを読み取った場合の処理をここに記述
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // キャラクタリスティックに書き込んだ場合の処理をここに記述
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        // キャラクタリスティックが変更された場合の処理をここに記述
    }
}

class BLEScanner(val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: BLEScanCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun getStatus():Boolean{
        return connected
    }

    fun startScan() {
        Log.d(TAG,"Scan Started")
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        scanCallback = BLEScanCallback()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        }
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    private inner class BLEScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val device = result.device
                if (device.name==TARGET_DEVICE_NAME){
                    Log.d(TAG,device.name.toString())
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        device.connectGatt(context,false,gattCallback)!=null
                    }
                }
                if (ActivityCompat.checkSelfPermission(
                        context,
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
}