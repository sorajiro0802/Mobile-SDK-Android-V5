package dji.sampleV5.modulecommon.models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.*

class LeicaControllerVM() : DJIViewModel(){
    private val TAG = "LeicaControllerVM"
    private var connection = false
    private val macAddr: String = "D4:36:39:77:DC:92" // MacAddress of Total Station Leica TS16

    var btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var btDevice: BluetoothDevice = btAdapter!!.getRemoteDevice(this.macAddr)
    // check below
    private var connectThread: BluetoothConnectThread = SerialPortProfileConnectThread(btDevice)
    var mReceiveTask: BluetoothReceiveTask? = null
    var leicaValue: MutableLiveData<String> = MutableLiveData<String>()

    init {
//        this.connect()
    }
    fun isConnected(): Boolean {
        return connection
    }

    fun connect(): Int {
        return try {
            connectThread = SerialPortProfileConnectThread(btDevice)
            this.connectThread.start()
            Thread.sleep(2000)
            this.connection = true
            mReceiveTask = BluetoothReceiveTask(connectThread.getSocket())
            0
        } catch (e: InterruptedException) {
            e.printStackTrace()
            this.connection = false
            -1
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "$e")
            this.connection = false
            -1
        }

    }

    fun read() {
        if(this.connection) {
            mReceiveTask!!.receiveData = leicaValue
            mReceiveTask!!.start()
        } else {
            Log.e(TAG, "Leica is not connected")
        }
    }
    fun stop(){

        try {
//            prismPos.postValue("")
        } catch (e: InterruptedException){
            e.printStackTrace()
        }
    }

    fun close() {
        try {
            mReceiveTask!!.cancel()
            mReceiveTask!!.finish()
        } catch (e: InterruptedException){
            mReceiveTask!!.interrupt()
            connectThread.interrupt()
        }
        this.connection = false
    }
}