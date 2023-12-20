package dji.sampleV5.moduleaircraft.models

import android.util.Log
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sampleV5.modulecommon.models.ValueUpdateObserver
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.coroutines.*
import java.io.File
import java.lang.Thread.sleep
import kotlin.coroutines.EmptyCoroutineContext
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
    private var tolerance = .02f  // a.bcd  a:meter, b:10 centi meter, c:centi meter, d:milli meter
    private val pjob = CoroutineScope(EmptyCoroutineContext)
    private val defaultStickMax = 165 // 125 mm/s
    private var prevError = .0f
    private val Kp = 700 // speed level = 0.05
    private val Kd = 1200
    private var prevErrorDifference = 0f
    private lateinit var scenarioFile: File

    fun executeScript() {
        pjob.launch {
            scenarioFile.forEachLine{ line ->
                // ファイルに書かれてる文字を上から読み順に解析していく
                val parts = line.split(" ")
                if (parts.size >= 2) {
                    val command = parts[0]
                    val args = parts.subList(1, parts.size).joinToString("")

                    // movコマンドの引数チェックと実行処理
                    if(command.uppercase() == "MOV") {
                        val pos = args.split(",").map {it.toFloat()}.toFloatArray()
                        if(pos.size == 3) {
                            Log.d(TAG, "MoveTo: ${pos[0]},${pos[1]},${pos[2]}")
                            moveTo(pos)
                        } else { Log.e(TAG, "Invalid PosArgument length: ${pos.size}") }
                    }
                    // waitコマンドの引数チェックと実行処理
                    else if(command.uppercase() == "WAIT") {
                        val sleepTime = args.toLong()
                        Log.d(TAG, "Wait: $sleepTime mmSec")
                        runBlocking { delay(sleepTime) }
                    } else {Log.e(TAG, "Non Expected Command:$command")}

                } else {
                    Log.e(TAG, "Invalid Script Line")
                }
            }

            Log.d("SelfDriveVM", "Moving Finish!!")
            ToastUtils.showToast("Moving Finish!!")
        }
    }

    private fun moveTo(targetPos: FloatArray){
        try {
            var arriveCnt = 0
            // check Data stream whether TS16 is alive
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
                            ToastUtils.showToast("Arrived!")
                            arriveCnt = 0
                            break
                        }
                    } else {
                        arriveCnt = 0
                    }
                    // ドローンの操作を行う
                    // 目的地Pos-自分のPos ＝ 方向ベクトル を計算する
                    val xDiff = targetPos[0] - currDronePoint[0]
                    val yDiff = targetPos[1] - currDronePoint[1]
                    val zDiff = targetPos[2] - currDronePoint[2]
                    val exDiff = xDiff/pointsDiff
                    val eyDiff = yDiff/pointsDiff
                    val ezDiff = zDiff/pointsDiff

                    // 方向ベクトルの各成分の単位ベクトルを元に移動
                    var horizon = 0
                    var vertical = 0
                    var height = 0
                    // 目的点付近で振動しないよう，一定の閾値を超えたらスピードを更に緩める
                    if (.212f < pointsDiff){
                        horizon = (exDiff * defaultStickMax).toInt()
                        vertical = (eyDiff * defaultStickMax).toInt()
                        height = (ezDiff * defaultStickMax * 10/3).toInt()
//                        height = (ezDiff * defaultStickMax * 3).toInt()
                    }else{
                        val expY = 25*1.1.pow(pointsDiff.toDouble()*100)-25
                        horizon = (exDiff * expY).toInt()
                        vertical = (eyDiff * expY).toInt()
                        height = (ezDiff * expY * 10/3).toInt()
//                        height = (exDiff * expY * 3).toInt()
                    }

                    // 実際のドローン操作
//                    Log.d(TAG,"vertical(R):$vertical,horizon(R):$horizon,height:$height")
                    virtualStickVM.setRightPosition(horizon, vertical)
                    virtualStickVM.setLeftPosition(0, height)

                    sleep(100L)
                    observerFlag = valueObserver.getUpdatingStatus()
                } else {
//                    Log.d(TAG, "stop moving")
                    resetStickPos()
                    continue
                }
            }
            // reset stick values
            resetStickPos()

        }catch(e: Exception){
            Log.d(TAG, "$e")
            resetStickPos()
        }
    }

    private fun resetStickPos() {
        virtualStickVM.setRightPosition(0,0)
        virtualStickVM.setLeftPosition(0,0)
    }

    private fun convertCoordinateTS2UAV(tsPos:FloatArray):FloatArray {
        val convertedPos = floatArrayOf(.0f, .0f, .0f)
        // index  0:x, 1:y, 2:z
        val x = -tsPos[0]; val y = tsPos[1]; val z = tsPos[2] // arbitrary Point for convert
        val x0 = -calibOriginPos[0]; val y0 = calibOriginPos[1]  // TS Axes info
        val x1 = -calibXAxisPos[0]; val y1 = calibXAxisPos[1]

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
    fun setTargetTolerance(dist:Float){
        tolerance = dist
    }

    fun stopMoving() {
        movable = false
        isMoving = false
    }

    fun continueMoving() {
        movable = true
        isMoving = true
    }

    fun resetMoving() {
        try {
            pjob.cancel()
            resetStickPos()
            Log.d(TAG, "Moving was resettle")
        } catch(e: Exception) {
            Log.e(TAG, "$e")
            ToastUtils.showToast("$e")
        }
    }

    fun setScenarioScript(path:String){
        val filename = path.split("/").last()
        try {
            scenarioFile = File(path)
            ToastUtils.showToast("[$filename] was Settled")
        } catch(e: Exception) {
            Log.e(TAG, e.toString())
            ToastUtils.showToast("Failed to load")
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
