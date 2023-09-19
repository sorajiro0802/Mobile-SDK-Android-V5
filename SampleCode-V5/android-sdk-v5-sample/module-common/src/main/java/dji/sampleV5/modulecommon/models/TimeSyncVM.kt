package dji.sampleV5.modulecommon.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class TimeSyncVM() : DJIViewModel() {
    var ip_addr : String = "192.168.0.98"
    var port: Int = 8020
    val serverTime: MutableLiveData<String> = MutableLiveData()
    var sc: SocketClient = SocketClient(ip_addr, port)

    fun sync(){
        sc.start()
    }

    // サーバに接続するクラス
    inner class SocketClient(var ip:String, var port:Int): Thread(){
        private lateinit var socket: Socket
        private lateinit var reader: BufferedReader
//        private lateinit var reader: InputStream
        private val receivedData: MutableLiveData<String> = this@TimeSyncVM.serverTime

        override fun run(){
            this.connect()
            this.read()
        }

        fun connect(){
            try {
                socket = Socket(ip, port)
                Log.d(TAG, "connected socket")
            } catch (e: Exception) {
                Log.e(TAG, "$e")
//                socket = null
            }
        }

        fun read(){
            try {
                !socket.isConnected
            } catch (e:Exception) {
                Log.e(TAG, "No Connection")
                return
            }
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            Log.d(TAG, "start reading")
            try {
                reader.use {
                    while (true) {
                        val message = it.readLine()
                        if (message != null) {
                            receivedData.postValue(message)
                            Log.d(TAG, message)
                        } else {
                            break
                        }
                        sleep(SLEEP_TIME)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "$e")
            }
        }
//        private fun read(): Boolean{
//            var temp = ""
//            val w = ByteArray(1024)
//            var size: Int
//
//            if (!socket.isConnected) {
//                return false
//            }
//            try {
//                val reader = socket.getInputStream()
//                size = reader.read(w)
//                if (size <= 0) {
//                    print(size)
//                    return false
//                } else {
//                    temp = String(w, 0, size, charset("UTF-8"))
//                    print(temp)
//                }
//                return true
//            } catch (e: Exception) {
//                e.printStackTrace()
//                return false
//            }
//        }

        fun close() {
            if (::reader.isInitialized) {
                reader.close()
                Log.d(TAG, "close reader")
            }
            if (::socket.isInitialized) {
                socket.close()
                Log.d(TAG, "close socket")
            }
        }
    }

    fun getNowDate(): String {
        val df: DateFormat = SimpleDateFormat("yyyy/MM/dd,HH:mm:ss.SSS")
        val date: Date = Date(System.currentTimeMillis())
        return df.format(date)
    }

    fun setAddress(ip: String, port: Int){
        Log.d(TAG, "settled new addr ($ip,$port)")
        this.ip_addr = ip
        this.port = port
        sc.ip = this.ip_addr
        sc.port = this.port
    }

    fun stop(){
        this.sc.close()
    }


    companion object {
        const val TAG = "TimeSyncVM"
        const val SLEEP_TIME = 50L
    }
}