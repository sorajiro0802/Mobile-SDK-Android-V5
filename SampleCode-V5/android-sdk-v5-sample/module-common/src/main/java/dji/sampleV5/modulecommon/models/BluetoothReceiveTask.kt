package dji.sampleV5.modulecommon.models

import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader


class BluetoothReceiveTask(private val socket: BluetoothSocket?): Thread() {
    companion object{
        const val TAG = "BluetoothReceiveTask"
        const val SLEEP_TIME = 500L
    }
    private lateinit var mInputStream: InputStream
    private lateinit var mSocket: BluetoothSocket
    private lateinit var reader: BufferedReader
    val receiveData: MutableLiveData<String> = MutableLiveData()

    @Volatile
    private var mIsCancel = false

    init {
        mIsCancel = false
        if (socket == null) {
            Log.e(TAG, "parameter socket is null.")
        }

        try {
            mInputStream = socket!!.inputStream

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run(){
        // previous Setting
        /*
        var buffer = ByteArray(1024)
        var readSize: Int

        Log.i(TAG, "start read task.")

        while (mInputStream != null) {
            if (mIsCancel) {
                println("mIsCancel")
                break
            }
            readSize = mInputStream!!.read(buffer)
            try {
//                readSize = mInputStream!!.read(buffer)
                if (readSize == 48) {
                    //処理
                    println(readSize)
                } else {
                    Log.e(TAG, "NG $readSize byte")
                }
                Thread.sleep(2000)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                Log.e(TAG, "InterruptedException!!")
            } finally {
                Log.e(TAG, "any error occurred")
            }

        }*/
        // reading socket (https://qiita.com/saccho/items/2741b0f5633cee3ef8ae)
        Log.d(TAG, "Start Receive task")
        reader = BufferedReader(InputStreamReader(mInputStream))
        try {
            reader.use{
                while(true) {
                    val message = it.readLine()
                    if(message != null) {
                        receiveData.postValue(message)
                        Log.d(TAG, message)

                    } else {
                        break
                    }
                    Thread.sleep(SLEEP_TIME)
                }
            }
        } catch (e: Exception) {
           Log.e(TAG, "$e")
        }
    }

    fun cancel() {
        mIsCancel = true
    }

    fun finish() {
        socket.let {
            try {
                it?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}