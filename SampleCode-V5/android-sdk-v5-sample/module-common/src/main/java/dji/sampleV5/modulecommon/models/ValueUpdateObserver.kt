package dji.sampleV5.modulecommon.models

import android.nfc.Tag
import android.util.Log

class ValueUpdateObserver(): Thread() {
    private val TAG = "ValueUpdateObserver"
    private var currentValue = ""
    private var previValue = ""
    private var observePeriodT = 100L
    private var dataComing = false


    override fun run(){
        startObserve()
    }
    fun setCurValue(value: String) {
        this.currentValue = value
    }

    private fun startObserve(){
        var cnt = 0

        while(true) {
            this.previValue = this.currentValue
            sleep(observePeriodT) // n mmSec
            if( this.previValue == this.currentValue) {
                cnt++
                if(cnt > 10) {
                    this.dataComing = false
                    cnt = 11
                }
            } else {
                this.dataComing = true
                cnt=0
            }
//            Log.d(TAG, "$previValue $currentValue")
        }

    }

    fun getUpdatingStatus(): Boolean{
        return this.dataComing
    }
}