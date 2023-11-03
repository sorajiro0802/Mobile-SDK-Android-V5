package dji.sampleV5.moduleaircraft.models

import android.util.Log
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sampleV5.modulecommon.models.ValueUpdateObserver
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.manager.aircraft.virtualstick.Stick
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import kotlin.math.*

class SelfDriveVM (val virtualStickVM: VirtualStickVM): DJIViewModel(){
    private val TAG = "SelfDriveVM"
    private var isMoving = false
    private var movable = true
    private var calibOriginPos: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var calibXAxisPos: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var calibZOffsetPos: Float = .0f
    private var currDronePoint:FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    val valueObserver: ValueUpdateObserver = ValueUpdateObserver()
    var observerFlag = true
    private val tolerance = .05f  // a.bcd  a:meter, b:10 centi meter, c:centi meter, d:milli meter


    private var scenarioPoints: Array<FloatArray> = arrayOf(
        floatArrayOf(0.0f, 0.0f, 1.0f),
        floatArrayOf(1.0f, 0.0f, 1.0f),
        floatArrayOf(1.0f, 2.0f, 1.0f)
    )
    private lateinit var scenarioFile: File

    fun executeScript(){}

    fun moveTo(targetPos: FloatArray){
        try {
            var arriveCnt = 0
            // check Data stream from TS16 is alive
            observerFlag = valueObserver.getUpdatingStatus()
            while(observerFlag){
                if(movable) {
                    isMoving = true
                    // 到着したかどうかの判定
                    val pointsDiff = calcL2Norm(targetPos, currDronePoint)
                    if (tolerance > pointsDiff) {
                        arriveCnt++
                        if (arriveCnt > 10) {
                            Log.d(TAG, "Arrived!")
                            arriveCnt = 0
                            break
                        }
                    }
                    // ドローンの操作を行う
                    // 目的地と自分の場所の差分＝方向ベクトルを計算する
                    val x_diff = targetPos[0] - currDronePoint[0]
                    val y_diff = targetPos[1] - currDronePoint[1]
                    val z_diff = targetPos[2] - currDronePoint[2]
                    // 実際のドローン操作
                    Log.d(TAG,"vertical(R):${x_diff / pointsDiff},horizon(R):${y_diff / pointsDiff},height:${(z_diff / pointsDiff)}")
                    virtualStickVM.setRightPosition(
                        ((y_diff / pointsDiff) * Stick.MAX_STICK_POSITION_ABS / 4).toInt(),
                        ((x_diff / pointsDiff) * Stick.MAX_STICK_POSITION_ABS / 4).toInt()
                    )
                    virtualStickVM.setLeftPosition(
                        0,
                        ((z_diff / pointsDiff) * Stick.MAX_STICK_POSITION_ABS / 4).toInt()
                    )
                    sleep(100L)
                    observerFlag = valueObserver.getUpdatingStatus()
                } else {
                    Log.d(TAG, "stop moving")
                    continue
                }
            }

            // reset stick values
            virtualStickVM.setRightPosition(0,0)
            virtualStickVM.setLeftPosition(0,0)

        }catch(e: Exception){
            Log.d(TAG, "$e")
            // reset stick values
            virtualStickVM.setRightPosition(0,0)
            virtualStickVM.setLeftPosition(0,0)
        }
    }

    private fun convertCoordinateTS2UAV(tsPos:FloatArray):FloatArray {
        val convertedPos = floatArrayOf(.0f, .0f, .0f)
        // index  0:x, 1:y, 2:z
        val x = tsPos[0]; val y = tsPos[1]; val z = tsPos[2] // arbitrary Point for convert

        val x0 = calibOriginPos[0]; val y0 = calibOriginPos[1]  // TS Axes info
        val x1 = calibXAxisPos[0]; val y1 = calibXAxisPos[1]

        val px = x1-x0
        val absP = calcL2Norm(calibOriginPos, calibXAxisPos)

        val theta = if (y1 > y0) {
            acos(px/absP)
        }else {
            -acos(px/absP)
        }
        convertedPos[0] = round(((x-x0)*cos(-theta) - (y-y0)*sin(-theta))*1000)/1000
        convertedPos[1] = round(((x-x0)*sin(-theta) + (y-y0)*cos(-theta))*1000)/1000
        convertedPos[2] = z - calibZOffsetPos
        return convertedPos
    }

    fun setTSPos(TSPos: FloatArray){
        this.currDronePoint = convertCoordinateTS2UAV(TSPos)
    }
    fun setTSvaluePrefix(prefix: String){
        valueObserver.setCurValue(prefix)
    }
    fun setOriginPos(pos: FloatArray) {
        calibOriginPos = pos
    }
    fun setXAxisPos(pos: FloatArray) {
        calibXAxisPos = pos
    }
    fun setZPosOffset(zPos:Float) {
        calibZOffsetPos = zPos
    }

    fun stopMoving() {
        movable = false
        isMoving = false
    }

    fun continueMoving() {
        movable = true
        isMoving = true
    }

    fun setScenarioScript(path:String){//TODO: ファイルアクセスがようわからんくて未完成,スタブ(scenarioPoints)でとりあえずは対応
        scenarioFile = File(path)
        try {
            val inputStream: FileInputStream = FileInputStream(scenarioFile)
            val reader: InputStreamReader = InputStreamReader(inputStream)
            Log.d("ReadScenario", reader.readText())

            inputStream.close()
            reader.close()
        } catch(e: Exception) {
            Log.e("ReadScenario", e.toString())
        }
    }
    fun getScenarioScript(): File {
        return scenarioFile
    }

    fun getCurDronePos(): FloatArray {
        return currDronePoint
    }

    private fun calcL2Norm(p1: FloatArray, p2: FloatArray): Float {
        var sum = 0f
        for (i in p1.indices) {
            sum += (p1[i]-p2[i])*(p1[i]-p2[i])
        }
        return sqrt(sum)
    }


}
