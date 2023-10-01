package dji.sampleV5.modulecommon.models

import android.bluetooth.BluetoothDevice
import java.util.*

class SerialPortProfileConnectThread(device: BluetoothDevice):BluetoothConnectThread(device, SPP_UUID) {
    companion object{
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
    init {
        var TAG = "SerialPortProfileConnectThread"
    }
}