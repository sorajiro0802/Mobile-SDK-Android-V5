package dji.sampleV5.modulecommon.models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import java.io.OutputStream
import java.net.Socket
import java.util.UUID

class LeicaControllerVM() : ViewModel() {
    private val TAG = "LeicaControllerVM"
    private var connection = false
    private val macAddr: String = "D4:36:39:77:DC:92" // MacAddress of Total Station Leica TS16

    var btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var btDevice: BluetoothDevice = btAdapter!!.getRemoteDevice(this.macAddr)
    // check below
    private var connectThread: BluetoothConnectThread = SerialPortProfileConnectThread(btDevice)
    var mReceiveTask: BluetoothReceiveTask? = null

    init {
//        this.connect()
    }
    fun isConnected(): Boolean {
        return connection
    }

    fun connect(): Int {
        if (this.connectThread == null){
            println("no device")
            return -1
        }

        this.connectThread!!.start()
        try {
            Thread.sleep(2000)
            this.connection = true
            return 0
        }catch (e: InterruptedException) {
            e.printStackTrace()
            this.connection = false
            return -1
        }
    }

    fun read(){
        if(this.connection) {
            mReceiveTask = BluetoothReceiveTask(this.connectThread!!.getSocket())
            mReceiveTask!!.start()
        } else {
            Log.e(TAG, "Leica is not connected")
        }

    }
}