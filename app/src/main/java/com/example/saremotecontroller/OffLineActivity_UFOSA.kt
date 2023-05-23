package com.example.saremotecontroller

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*
class OffLineActivity : AppCompatActivity() {

    private lateinit var seekbarCtr: SeekBar
    private lateinit var seekbarMax: SeekBar
    private lateinit var indicator: ProgressBar
    private lateinit var sensor: ImageView
    private lateinit var textMaxSpeed: TextView
    private lateinit var textRotationSpeed: TextView
    private lateinit var buttonExit: Button

    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    fun sendCrl(speed:Int,mode:Int){
        var motor = (mode shl 7 and 0x80)
        motor = (motor or speed)
        if(DEVICE == DEVICES[UFO_SA]){
            val sendByte = byteArrayOf(0x02, 0x01, motor.toByte())
            if (bleService?.getStatus() != false) {
                bleService?.writeValue(sendByte)
            }
        }else if (DEVICE == DEVICES[UFO_TW]){
            val sendByte = byteArrayOf(0x05, (motor xor 0x80).toByte(), motor.toByte())
            if (bleService?.getStatus() != false) {
                bleService?.writeValue(sendByte)
            }
        }
        indicator.progress=speed
        textRotationSpeed.text = String.format("Rotation Speed: %3d%%",speed)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline)

        val bleIntent = Intent(this, BLEService::class.java)
        bindService(bleIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        //testButton = findViewById(R.id.test_button)
        seekbarCtr = findViewById(R.id.rotationPower)
        seekbarMax = findViewById(R.id.maxSpeed)
        indicator = findViewById(R.id.progressBar)
        sensor = findViewById(R.id.imageView)
        textMaxSpeed = findViewById(R.id.textMaxSpeed)
        textRotationSpeed = findViewById(R.id.textSpeedCtr)
        buttonExit = findViewById(R.id.button_exit)

        seekbarMax.progress = 100

        seekbarCtr.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if(i>=0){
                    sendCrl(i,1)
                }else{
                    sendCrl(i*-1,0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekbarMax.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                maxSpeed = i.toDouble()/100
                textMaxSpeed.text = String.format("Max Speed: %3d%%",i)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        buttonExit.setOnClickListener{
            finish()
        }
    }
    private var prevX=0f
    private var prevY=0f
    private var mode = 0
    private val location = IntArray(2)
    private var prevTime=0L
    private var i = 0
    var maxSpeed = 1.0
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                sensor.getLocationOnScreen(location)
                val cX = sensor.width/2
                val cY = sensor.height/2
                prevX = event.x - location[0] - cX
                prevY = cY - event.y + location[1]
                //testButton.text= String.format("x: %d, y: %d",x,y)
            }
            MotionEvent.ACTION_MOVE ->{
                sensor.getLocationOnScreen(location)
                val cX = sensor.width/2
                val cY = sensor.height/2
                val circle = cY*cY+cX+cX
                val x = event.x - location[0] - cX
                val y = cY - event.y + location[1]
                val r = y*y+x*x
                if(i%4==0){
                    if(r<circle) {
                        val time = System.currentTimeMillis()
                        val distanceX = x - prevX
                        val distanceY = y - prevY
                        if(i == 4){
                            mode = if(x>=0){
                                if(atan2(distanceY,distanceX)<0){
                                    MODE_RIGHT
                                }else{
                                    MODE_LEFT
                                }
                            }else{
                                if(atan2(distanceY,distanceX)>=0){
                                    MODE_RIGHT
                                }else{
                                    MODE_LEFT
                                }
                            }
                        }
                        val distance =
                            sqrt((distanceX * distanceX).toDouble() + (distanceY * distanceY).toDouble())
                        var speed = distance /75
                        speed *= when {
                            r < 5000 -> 0.9
                            r < 12000 -> 0.8
                            r < 20000 -> 0.6
                            r < 40000 -> 0.5
                            r < 60000 -> 0.4
                            r < 80000 -> 0.3
                            r < 100000 -> 0.25
                            else -> 0.2
                        }
                        speed *= 0.6 * maxSpeed
                        if(speed>=1.0){speed=1.0}
                        sendCrl((speed*100).toInt(),mode)
                        prevX = x
                        prevY = y
                        prevTime = time
                    }else{
                        sendCrl(0,0)
                    }
                }
                i++

            }
            MotionEvent.ACTION_UP ->{
                i = 0
                sendCrl(0,0)
            }
            MotionEvent.ACTION_CANCEL ->{
                i = 0
                sendCrl(0,0)
            }
        }
        return super.onTouchEvent(event) //※
    }
}