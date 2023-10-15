package dji.sampleV5.moduleaircraft.models

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.moduleaircraft.models.VirtualStickVM
import dji.sampleV5.modulecommon.models.DJIViewModel
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class SelfDriveVM (virtualStickVM: VirtualStickVM): DJIViewModel(){
    private var isFlying = false
    var isArrived = MutableLiveData<Boolean>()
    private var scenarioPoints = mutableListOf<Double>()

    init {

    }

    fun setScenarioScript(path:String){//TODO(): ファイルアクセスがようわからんくて未完成,スタブでとりあえずは対応
        val file = File(path)
        try {
            val inputstream: FileInputStream = FileInputStream(file)
            val reader: InputStreamReader = InputStreamReader(inputstream)
            Log.d("ReadScenario", reader.readText())
        } catch(e: Exception) {
            Log.e("ReadScenario", e.toString())
        }
    }

}