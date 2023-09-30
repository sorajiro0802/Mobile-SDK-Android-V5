package dji.sampleV5.modulecommon.models

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.UUID

// コンストラクタ：bluetoothDevice, uuid. Threadを継承
open class BluetoothConnectThread(device: BluetoothDevice, uuid: UUID): Thread() {
    val TAG: String = "BluetoothConnectThread"
    protected var btSocket: BluetoothSocket? = null

    fun getSocket(): BluetoothSocket? {
        return btSocket
    }

    init {
        var tmp: BluetoothSocket? = null
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid)
        } catch (e: IOException) {
            Log.e(TAG, "$e")
            e.printStackTrace()
        } catch (e: SecurityException) {
            Log.e(TAG, "$e")
            e.printStackTrace()
        }
        btSocket = tmp
    }

    override fun run() {
        println("run in BluetoothConnectThread start!!")
        if(btSocket == null) {
            return
        }
        try {
            btSocket!!.connect()
        }catch (e: IOException) {
            e.printStackTrace()
            try{
                btSocket!!.close()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            return
        } catch (e: SecurityException){
            Log.e(TAG, "$e")
            e.printStackTrace()
        }
        Log.i(TAG, "Bluetooth connecting.")
    }

}