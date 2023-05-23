package com.example.saremotecontroller

import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class SocketManager {
    fun connectServer(ip: String,port: Int, to: Int?): Socket?{
        if(Looper.myLooper() != Looper.getMainLooper()){
            return try{
                if(to!=null){
                    val sc = Socket()
                    sc.connect(InetSocketAddress(ip, port), to)
                    sc
                }else{
                    val sc = Socket(ip, port)
                    sc
                }
            }catch (e: Exception){
                null
            }
        }else{
            var result: Socket? = null
            val th = Thread{
                try{
                    result = if(to != null){
                        val sc = Socket()
                        sc.connect(InetSocketAddress(ip, port), to)
                        sc
                    }else{
                        val sc = Socket(ip, port)
                        sc
                    }
                }catch (_: Exception){
                }
            }
            th.start()
            th.join()
            return result
        }
    }
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