package com.example.saremotecontroller

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*

const val MODE_RIGHT = 0
const val MODE_LEFT = 1
class OffLineActivity : AppCompatActivity() {

    var prevX=0f
    var prevY=0f
    var prevTime=0L
    var i = 1
    var maxSpeed = 1.0

    private lateinit var seekbarCtr: SeekBar
    private lateinit var seekbarMax: SeekBar
    private lateinit var indicator: ProgressBar
    private lateinit var sensor: ImageView
    private lateinit var textMaxSpeed: TextView
    private lateinit var textRotationSpeed: TextView

    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    fun sendCrl(speed:Int,mode:Int,device:Int){
        var motor = (mode shl 7 and 0x80)
        motor = (motor or speed)
        val sendByte = byteArrayOf(0x02, 0x01, motor.toByte())
        if (bleService?.getStatus() != false) {
            bleService?.writeValue(sendByte)
        }
        //testButton.text=String.format("%d, %d",mode,speed)
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

        seekbarMax.progress = 100

        //testButton.setOnClickListener {
            //bleService?.writeValue(byteArrayOf(0x02,0x01,0x0f))
        //}

        seekbarCtr.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if(i>=0){
                    sendCrl(i,1,0)
                }else{
                    sendCrl(i*-1,0,0)
                }
                //textRotationSpeed.text = String.format("Max Speed: %3d%%",i)
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
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x.toInt()-sensor.width/2
                val y = sensor.height/2-event.y.toInt()

                //testButton.text= String.format("x: %d, y: %d",x,y)
            }
            MotionEvent.ACTION_MOVE ->{
                i++
                val location = IntArray(2)
                sensor.getLocationOnScreen(location)
                val cX = sensor.width/2
                val cY = sensor.height/2
                val circle = cY*cY+cX+cX
                val x = event.x - location[0] - cX
                val y = cY - event.y + location[1]
                val r = y*y+x*x
                if(i%4==0){
                    if(r<circle) {
                        //testButton.text = String.format("%s,%s", circle.toString(), r.toString())
                        val time = System.currentTimeMillis()
                        val distanceX = x - prevX
                        val distanceY = y - prevY
                        var mode = 0
                        mode = if(distanceX>=0){
                            if(y>=0){
                                MODE_RIGHT
                            }else{
                                MODE_LEFT
                            }
                        }else{
                            if(y>=0){
                                MODE_LEFT
                            }else{
                                MODE_RIGHT
                            }
                        }
                        val distance =
                            sqrt((distanceX * distanceX).toDouble() + (distanceY * distanceY).toDouble())
                        val deltaTime = time - prevTime
                        var speed = distance / deltaTime
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
                        sendCrl((speed*100).toInt(),mode,0)
                        prevX = x
                        prevY = y
                        prevTime = time
                        //testButton.text = String.format("%s,%s", ((speed)*100).toInt().toString(), mode.toString())
                        //testButton.text = String.format("%s,%s", r.toInt().toString(), circle.toString())
                        //testButton.text = i.toString()
                    }else{
                        sendCrl(0,0,0)
                    }
                }

            }
            MotionEvent.ACTION_UP ->{
                i = 0
                sendCrl(0,0,0)
            }
            MotionEvent.ACTION_CANCEL ->{
                i = 0
                sendCrl(0,0,0)
            }
        }
        return super.onTouchEvent(event) //※
    }
}