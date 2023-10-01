package dji.sampleV5.modulecommon.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

class TimeSyncVM() : DJIViewModel() {
    var ip : String = ""
    var port: Int = 8020
    val serverTime: MutableLiveData<String> = MutableLiveData<String>()
    var sc: SocketClient = SocketClient(ip, port)

    fun sync(){
        sc = SocketClient(ip, port)
        sc.start()
    }

    // サーバに接続するクラス
    inner class SocketClient(var ip: String, var port:Int): Thread(){
        var inetIP: InetAddress = InetAddress.getByName(ip)
        var stop_flag = false
        private lateinit var socket: DatagramSocket

        override fun run(){
            this.read()
        }

        private fun read(){
            socket = DatagramSocket(port)
            while (!stop_flag) {
                send("send")
                sleep(SLEEP_TIME)
                recieve()
            }
        }

        private fun recieve(){
            val buffer: ByteArray = ByteArray(24)
            val packet: DatagramPacket = DatagramPacket(buffer, buffer.size)

            try {
                socket.receive(packet)
                val data = String(buffer)
//                Log.d(TAG, data)
                this@TimeSyncVM.serverTime.postValue(data)
            } catch (e: Exception) {
                Log.e(TAG, "Could not receive data")
            }
        }

        fun send(msg: String){
            val byte = msg.toByteArray()
            try {
                val packet = DatagramPacket(byte, byte.size, inetIP, port)
                socket.send(packet)
            } catch(e: Exception) {
                Log.e(TAG, "Could not send data")
            }
        }

        fun close(){
            try {
                socket.close()
            } catch(e: Exception) {
                Log.e(TAG, "Could not close UDP Socket")
            }
        }

    }

    fun getNowDate(): String {
        val df: DateFormat = SimpleDateFormat("yyyy/MM/dd,HH:mm:ss.SSS")
        val date: Date = Date(System.currentTimeMillis())
        return df.format(date)
    }

    fun calcTimeDiff(date1: String, date2: String):Float{
        // 分以下の誤差を計算する仕様(時間までは合っているだろう)
        val underMin1 = date1.drop(14).split(":"); val underMin2 = date2.drop(14).split(":")
        val sec1 = underMin1[0].toInt() * 60 + underMin1[1].toDouble()
        val sec2 = underMin2[0].toInt() * 60 + underMin2[1].toDouble()
        val diff = (sec2 - sec1)*1000
        return (floor(diff)/1000).toFloat()
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