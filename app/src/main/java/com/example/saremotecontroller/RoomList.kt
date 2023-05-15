package com.example.saremotecontroller

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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var testText: TextView
    private lateinit var buttonUpdate: Button

    private var bleService: BLEService? = null
    private val msgMng = MsgManager()

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
        testText = findViewById(R.id.textView)
        buttonUpdate.text = "更新"
        val value = intent.getStringArrayExtra("roomList")
        var myDataList=value?.toMutableList()
        //val buttonList = mutableListOf<ButtonData>(b1,b2,b3,b4)
        val buttonList= mutableListOf<ButtonData>()
        testText.text=myDataList.toString()
        if (myDataList != null) {
            for(i in 0 until myDataList.size step 3){
                buttonList.add(ButtonData(myDataList[i],myDataList[i+1],myDataList[i+2].toInt()))
            }
            testText.text=buttonList.toString()
        }

        // ボタン用のデータクラスの動的な配列を設定

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
                testText.text=buttonList.toString()
                adapter.notifyDataSetChanged()
            }else{
                Toast.makeText(this, "サーバへの接続に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }

    }
    override fun onButtonClick(name: String) {
        val tokens = name.split(" ")
        Toast.makeText(this, bleService?.getStatus().toString(), Toast.LENGTH_SHORT).show()
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null)
        val userName = dialogView.findViewById<EditText>(R.id.userName)
        userName.hint="Your Name"
        val passWard = dialogView.findViewById<EditText>(R.id.passWard)
        passWard.hint="Room Password"
        val dialog = AlertDialog.Builder(this)
            .setTitle("ユーザ名入力")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                // OKボタン押したときの処理
                Thread {
                    val message = connectToServer(
                        tokens[0],
                        userName.text.toString(),
                        passWard.text.toString()
                    )
                    Handler(Looper.getMainLooper()).post {
                        if (message[0] != "Failed to connect to server") {
                            Toast.makeText(this, message.toList().toString(), Toast.LENGTH_SHORT)
                                .show()
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

    private fun connectToServer(name:String,user:String,pwd:String): Array<String> {
        try {
            val socket = Socket(address_ip, 19071)
            val outputStream: OutputStream = socket.getOutputStream()
            val printWriter = PrintWriter(outputStream, true)
            if(pwd==""){
                Log.d(TAG,msgMng.shapeMsg(String.format("chk_join %s %s",name,user)))
                printWriter.println(msgMng.shapeMsg(String.format("chk_join %s %s",name,user)))
            }else{
                Log.d(TAG,msgMng.shapeMsg(String.format("chk_join %s %s %s",name,user,pwd)))
                printWriter.println(msgMng.shapeMsg(String.format("chk_join %s %s %s",name,user,pwd)))
            }
            val inputStream = InputStreamReader(socket.getInputStream())
            val bufferedReader = BufferedReader(inputStream)
            val message = bufferedReader.readLine()
            socket.close()
            Log.d("APP",message)
            //return checkMsg(message)
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