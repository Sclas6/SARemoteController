package com.example.saremotecontroller

import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket

const val BLOCKING = 0
const val NONBLOCKING = 1

class SocketManager {

    private val msgMng = MsgManager()

    fun sendValue(sc: Socket, value: String){
        if(Looper.myLooper() != Looper.getMainLooper()){
            val outputStream: OutputStream = sc.getOutputStream()
            val printWriter = PrintWriter(outputStream, true)
            printWriter.println(msgMng.shapeMsg(value))
        }else{
            Thread{
                val outputStream: OutputStream = sc.getOutputStream()
                val printWriter = PrintWriter(outputStream, true)
                printWriter.println(msgMng.shapeMsg(value))
            }.start()
        }
    }
    fun readValue(sc: Socket, mode: Int): Array<String>{
        if(mode == BLOCKING){
            sc.soTimeout=0
        }else if(mode == NONBLOCKING){
            sc.soTimeout=5
        }
        try{
            return if(Looper.myLooper() != Looper.getMainLooper()) {
                val inputStream = InputStreamReader(sc.getInputStream())
                val bufferedReader = BufferedReader(inputStream)
                val msg = bufferedReader.readLine()
                sc.soTimeout=0
                msgMng.checkMsg(msg)
            }else{
                var result = ""
                val th = Thread{
                    val inputStream = InputStreamReader(sc.getInputStream())
                    val bufferedReader = BufferedReader(inputStream)
                    result = bufferedReader.readLine()
                }
                th.start()
                th.join()
                sc.soTimeout=0
                msgMng.checkMsg(result)
            }
        }catch (e: Exception){
            return arrayOf()
        }
    }
}