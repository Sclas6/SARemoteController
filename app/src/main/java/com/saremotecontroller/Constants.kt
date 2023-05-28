package com.saremotecontroller

const val address_ip = "157.7.214.224"

const val REQUEST_ENABLE_BT = 1
const val MY_REQUEST_CODE: Int = 2

const val BLOCKING = 0
const val NONBLOCKING = 1

internal val DEVICES = arrayOf("UFOSA", "UFO-TW")
internal var DEVICE = ""

const val UFO_SA = 0
const val UFO_TW = 1

const val SERVICE_UUID = "40EE1111-63EC-4B7F-8CE7-712EFD55B90E"
const val CHARACTERISTIC_UUID = "40EE2222-63EC-4B7F-8CE7-712EFD55B90E"
const val MODE_RIGHT = 0
const val MODE_LEFT = 1

internal val msgMng = MsgManager()
internal val scMng = SocketManager()