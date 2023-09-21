package dji.sampleV5.modulecommon.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class TimeSyncVM() : DJIViewModel() {
    var ip : String = ""
    var port: Int = 8020
    val serverTime: MutableLiveData<String> = MutableLiveData()
    var sc: SocketClient = SocketClient(ip, port)

    fun sync(){
        sc.start()
    }

    // サーバに接続するクラス
    inner class SocketClient(var ip: String, var port:Int): Thread(){
        var inetIP: InetAddress = InetAddress.getByName(ip)
        var stop_flag = false
        var socket = DatagramSocket(port)

        val recievedData: MutableLiveData<String> = this@TimeSyncVM.serverTime

        override fun run(){
            this.read()
        }

        fun read(){
            while(!stop_flag){
                send("send")
                sleep(SLEEP_TIME)
                recieve()
            }
        }

        fun recieve(){
//            val socket = DatagramSocket(port)
            val buffer: ByteArray = ByteArray(24)
            val packet: DatagramPacket = DatagramPacket(buffer, buffer.size)

            try {
                socket.receive(packet)
                val data = String(buffer)
//                Log.d(TAG, data)
                recievedData.postValue(data)
            } catch (e: Exception) {
                Log.e(TAG, "Could not receive data")
            }
        }

        fun send(msg: String){
//            val socket = DatagramSocket(port)
            val byte = msg.toByteArray()
            try {
                val packet = DatagramPacket(byte, byte.size, inetIP, port)
                socket.send(packet)
            } catch(e: Exception) {
                Log.e(TAG, "Could not send data")
            }
        }

        fun close(){
            socket.close()
        }

    }

    fun getNowDate(): String {
        val df: DateFormat = SimpleDateFormat("yyyy/MM/dd,HH:mm:ss.SSS")
        val date: Date = Date(System.currentTimeMillis())
        return df.format(date)
    }

    fun setAddress(ip: String, port: Int){
        Log.d(TAG, "settled new addr ($ip,$port)")
        this.ip = ip ; sc.ip = this.ip
        this.port = port; sc.port = this.port
        try { sc.inetIP = InetAddress.getByName(ip) }
        catch(e: Exception) {e.printStackTrace()}
    }

    fun stop(){
        sc.stop_flag = true
        sc.close()
    }


    companion object {
        const val TAG = "TimeSyncVM"
        const val SLEEP_TIME = 1000L
    }
}