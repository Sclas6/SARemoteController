package com.saremotecontroller

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saremotecontroller.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class RoomList : AppCompatActivity(), ButtonAdapter.OnButtonClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonAdapter: ButtonAdapter
    private lateinit var buttonUpdate: Button
    private lateinit var buttonCreate: Button

    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_list)
        val bleIntent = Intent(this, BLEService::class.java)
        bindService(bleIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        buttonUpdate = findViewById(R.id.buttonUpdate)
        buttonCreate = findViewById(R.id.buttonCreate)
        buttonUpdate.text = "更新"
        val value = intent.getStringArrayExtra("roomList")
        var myDataList=value?.toMutableList()
        val buttonList= mutableListOf<ButtonData>()
        if (myDataList != null) {
            for(i in 0 until myDataList.size step 3){
                buttonList.add(ButtonData(myDataList[i],myDataList[i+1],myDataList[i+2].toInt()))
            }
        }

        recyclerView = findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        val adapter = ButtonAdapter(buttonList,this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        buttonAdapter = ButtonAdapter(buttonList,this)

        buttonUpdate.setOnClickListener{
            val th = Thread {
                myDataList = updateList().toMutableList()
            }
            th.start()
            th.join()
            buttonList.clear()
            if (myDataList!!.size%3==0) {
                for(i in 0 until myDataList!!.size step 3){
                    buttonList.add(ButtonData(myDataList!![i],myDataList!![i+1],myDataList!![i+2].toInt()))
                }
                //testText.text=buttonList.toString()
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "ルームリストを更新しました", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "サーバへの接続に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
        buttonCreate.setOnClickListener{
            if(bleService!=null&& bleService!!.getStatus()) {
                val dialogView =
                    LayoutInflater.from(this).inflate(R.layout.dialog_create_layout, null)
                val roomName = dialogView.findViewById<EditText>(R.id.roomName)
                roomName.hint = "Room Name"
                val userName = dialogView.findViewById<EditText>(R.id.userName)
                userName.hint = "Your Name"
                val passWard = dialogView.findViewById<EditText>(R.id.passWard)
                passWard.hint = "Room Password"
                val dialog = AlertDialog.Builder(this)
                    .setTitle("ルーム情報")
                    .setView(dialogView)
                    .setPositiveButton("OK") { _, _ ->
                        Thread {
                            if (roomName.text.toString() != "" && userName.text.toString() != "") {
                                val message = chkCreate(roomName.text.toString())
                                Handler(Looper.getMainLooper()).post {
                                    if (message[0] != "Failed to connect to server") {
                                        if (message[0] == "ok") {
                                            val intent = Intent(this, OnlineDefActivity::class.java)
                                            val roomInfo: Array<String> = arrayOf(
                                                roomName.text.toString().replace(" ","_"),
                                                userName.text.toString().replace(" ","_"),
                                                passWard.text.toString().replace(" ","_")
                                            )
                                            intent.putExtra("command", roomInfo)
                                            startActivity(intent)
                                        } else {
                                            AlertDialog.Builder(this)
                                                .setTitle("エラー")
                                                .setMessage("同じ名前の部屋が既に存在します")
                                                .setPositiveButton("OK") { _, _ -> }
                                                .show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "サーバへの接続に失敗しました",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        this,
                                        "ルーム名とユーザ名は入力必須です",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }.start()
                    }
                dialog.setNegativeButton("キャンセル", null)
                dialog.show()
            }else{
                Toast.makeText(this, "Bluetoothデバイスを接続してください", Toast.LENGTH_SHORT).show()
            }
        }

    }
    override fun onButtonClick(name: String) {
        val tokens = name.split(" ")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_layout, null)
        val userName = dialogView.findViewById<EditText>(R.id.userName)
        userName.hint="Your Name"
        val passWard = dialogView.findViewById<EditText>(R.id.passWard)
        passWard.hint="Room Password"
        val dialog = AlertDialog.Builder(this)
            .setTitle("ユーザ名入力")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                Thread {
                    val message = chkJoin(
                        tokens[0],
                        userName.text.toString().replace(" ","_"),
                        passWard.text.toString().replace(" ","_")
                    )
                    Handler(Looper.getMainLooper()).post {
                        if (message[0] != "Failed to connect to server") {
                            if (message[0] == "ok") {
                                val intent = Intent(this, OnlineAtkActivity::class.java)
                                intent.putExtra("command", message)
                                startActivity(intent)
                            } else {
                                AlertDialog.Builder(this)
                                    .setTitle("エラー")
                                    .setMessage("不正なトークン\nパスワードが異なるか, 部屋が存在しません")
                                    .setPositiveButton("OK") { _, _ -> }
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, "サーバへの接続に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        dialog.setNegativeButton("キャンセル", null)
        dialog.show()
    }

    private fun chkJoin(name:String, user:String, pwd:String): Array<String> {
        try {
            val socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket.getOutputStream()
            val printWriter = PrintWriter(outputStream, true)
            if(pwd==""){
                Log.d(TAG, msgMng.shapeMsg(String.format("chk_join %s %s",name,user)))
                printWriter.println(msgMng.shapeMsg(String.format("chk_join %s %s",name,user)))
            }else{
                Log.d(TAG, msgMng.shapeMsg(String.format("chk_join %s %s %s",name,user,pwd)))
                printWriter.println(msgMng.shapeMsg(String.format("chk_join %s %s %s",name,user,pwd)))
            }
            val inputStream = InputStreamReader(socket.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            val message = bufferedReader.readLine()
            socket.close()
            Log.d("APP",message)
            return msgMng.checkMsg(message)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return arrayOf("Failed to connect to server")
    }
    private fun chkCreate(name:String): Array<String> {
        try {
            val socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket.getOutputStream()
            val printWriter = PrintWriter(outputStream, true)
            Log.d(TAG, msgMng.shapeMsg(String.format("chk_create %s",name)))
            printWriter.println(msgMng.shapeMsg(String.format("chk_create %s",name)))
            val inputStream = InputStreamReader(socket.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            val message = bufferedReader.readLine()
            socket.close()
            Log.d("APP",message)
            return msgMng.checkMsg(message)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return arrayOf("Failed to connect to server")
    }
    private fun updateList(): Array<String> {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(address_ip, 19071),3000)
            val outputStream: OutputStream = socket.getOutputStream()
            val printWriter = PrintWriter(outputStream, true)
            printWriter.println(msgMng.shapeMsg("show"))
            val inputStream = InputStreamReader(socket.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            val message = bufferedReader.readLine()
            socket.close()
            Log.d("APP",message)
            return msgMng.checkMsg(message)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return arrayOf("Failed to connect to server")
    }
}