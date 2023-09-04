package dji.sampleV5.modulecommon.models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import java.io.OutputStream
import java.net.Socket
import java.util.UUID

class LeicaControllerVM() {
    var isConnected = false
    private val macAddr: String = "D4:36:39:77:DC:92" // MacAddress of Total Station Leica TS16

    var btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var btDevice: BluetoothDevice = btAdapter!!.getRemoteDevice(this.macAddr)
    // check below
    private var connectThread: BluetoothConnectThread = SerialPortProfileConnectThread(btDevice)
    var mReceiveTask: BluetoothReceiveTask? = null

    init {
//        this.connect()
    }

    fun connect(): Int {
        if (this.connectThread == null){
            println("no device")
            return -1
        }

        this.connectThread!!.start()
        try {
            Thread.sleep(2000)
            return 0
        }catch (e: InterruptedException) {
            e.printStackTrace()
            return -1
        }
    }

    fun read(){
        mReceiveTask = BluetoothReceiveTask(this.connectThread!!.getSocket())
        mReceiveTask!!.start()
    }
}