package com.example.saremotecontroller

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.sqrt

class OnlineAtkActivity : AppCompatActivity() {

    private lateinit var seekbarCtr: SeekBar
    private lateinit var seekbarMax: SeekBar
    private lateinit var indicator: ProgressBar
    private lateinit var sensor: ImageView
    private lateinit var textMaxSpeed: TextView
    private lateinit var textRotationSpeed: TextView
    private lateinit var buttonExit: Button

    //private var atkService: ATKService? = null
    private val msgMng = MsgManager()
    private var value: Array<String>? = null
    private var th: Thread? = null
    private var socket: Socket? = null
    private var printWriter:PrintWriter? = null
/*
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG,"ATKService connected")
            val binder = service as ATKService.MyServiceBinder
            atkService = binder.getService()
            Log.d(TAG, "[ATK]${atkService}")
            while(true){
                if (atkService!!.chkCon()){
                    break
                }
            }
            if(value!![4]=="None"){
                atkService?.sendValue("${value!![1]} ${value!![2]} ${value!![3]}")
            }else{
                atkService?.sendValue("${value!![1]} ${value!![2]} ${value!![3]} ${value!![4]}")
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }
*/
    fun sendCrl(speed:Int,mode:Int,device:Int){
        Log.d(TAG,chkCon().toString())
        val d: Int = if(device==0) {
            0x02
        }else{
            0x01
        }
        var motor = (mode shl 7 and 0x80)
        motor = (motor or speed)
        val sendByte = byteArrayOf(d.toByte(), 0x01, motor.toByte())
        sendValue("ctr ${sendByte.contentToString().replace(" ","")}")
        indicator.progress=speed
        textRotationSpeed.text = String.format("Rotation Speed: %3d%%",speed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        value = intent.getStringArrayExtra("command")
        setContentView(R.layout.activity_online_atk)

        seekbarCtr = findViewById(R.id.rotationPower2)
        seekbarMax = findViewById(R.id.maxSpeed2)
        indicator = findViewById(R.id.progressBar2)
        sensor = findViewById(R.id.imageView2)
        textMaxSpeed = findViewById(R.id.textMaxSpeed2)
        textRotationSpeed = findViewById(R.id.textSpeedCtr2)
        buttonExit = findViewById(R.id.button_exit2)

        seekbarMax.progress = 100
        th = Thread{
            connectToServer()
        }
        th!!.start()

        while(true){
            if (chkCon()){
                break
            }
        }
        if(value!![4]=="None"){
            sendValue("${value!![1]} ${value!![2]} ${value!![3]}")
        }else{
            sendValue("${value!![1]} ${value!![2]} ${value!![3]} ${value!![4]}")
        }

        seekbarCtr.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if(i>=0){
                    sendCrl(i,1,0)
                }else{
                    sendCrl(i*-1,0,0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekbarMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                maxSpeed = i.toDouble()/100
                textMaxSpeed.text = String.format("Max Speed: %3d%%",i)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        buttonExit.setOnClickListener{
            sendValue("exit")
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
                        if(i==0){
                            //Log.d(TAG,"Mode Change")
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
                        }
                        val distance =
                            sqrt((distanceX * distanceX).toDouble() + (distanceY * distanceY).toDouble())
                        var speed = distance /100
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
                    }else{
                        sendCrl(0,0,0)
                    }
                }
                i++

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
    private fun connectToServer(): Array<String> {
        try {
            socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket!!.getOutputStream()
            printWriter = PrintWriter(outputStream, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return arrayOf("Failed to connect to server")
    }
    private fun sendValue(msg:String){
        Thread{
            printWriter?.println(msgMng.shapeMsg(msg))
        }.start()
    }
    private fun chkCon() :Boolean{
        return socket != null
    }
    override fun onDestroy() {
        super.onDestroy()
        sendValue(msgMng.shapeMsg("exit"))
        sendValue(msgMng.shapeMsg("quit"))
        socket!!.close()
        if(th!=null){
            th!!.interrupt()
        }
    }
}