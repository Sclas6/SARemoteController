package com.saremotecontroller
class MsgManager {
    fun checkMsg(msg:String): Array<String>{
        try{
            val list = msg.split(" ").toTypedArray()
            var dataLen = list.size-2
            for(i in 1 until list.size){
                dataLen+=list[i].length
            }
            //Log.d(TAG,dataLen.toString())
            if(list[0].toInt()==dataLen) {
                for (i in 0 until list.size - 1) {
                    list[i] = list[i + 1]
                }
                return list.copyOfRange(0, list.size - 1)
            }
            return arrayOf("error")
        }catch (e:Exception){
            return arrayOf("error")
        }
    }
    fun shapeMsg(msg:String): String{
        val i = msg.length
        return String.format("%d %s",i,msg)
    }
}