package com.example.saremotecontroller

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RoomList : AppCompatActivity(), ButtonAdapter.OnButtonClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonAdapter: ButtonAdapter
    private lateinit var testText: TextView

    private var bleService: BLEService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BLEService.MyServiceBinder
            bleService = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_list)
        val bleIntent = Intent(this, BLEService::class.java)
        bindService(bleIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        /*
        val b1=ButtonData("Room1","User1",1)
        val b2=ButtonData("Room2","User2",1)
        val b3=ButtonData("Room2","User2",1)
        val b4=ButtonData("Room2","User2",1)
        */
        testText = findViewById(R.id.textView)
        val value = intent.getStringArrayExtra("roomList")
        var myDataList=value?.toMutableList()
        //val buttonList = mutableListOf<ButtonData>(b1,b2,b3,b4)
        val buttonList= mutableListOf<ButtonData>()
        testText.text=myDataList.toString()
        if (myDataList != null) {
            for(i in 0 until myDataList.size step 3){
                buttonList.add(ButtonData(myDataList[i],myDataList[i+1],myDataList[i+2].toInt()))
            }
        }

        // ボタン用のデータクラスの動的な配列を設定

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.adapter = ButtonAdapter(buttonList, this)
        recyclerView.layoutManager = LinearLayoutManager(this)


    }
    override fun onButtonClick(name: String) {
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
            .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
            // OKボタン押したときの処理
            val userText = userName.text.toString()
            Toast.makeText(this, "$userText と入力しました", Toast.LENGTH_SHORT).show()
        })
        dialog.setNegativeButton("キャンセル", null)
        dialog.show()
    }
}