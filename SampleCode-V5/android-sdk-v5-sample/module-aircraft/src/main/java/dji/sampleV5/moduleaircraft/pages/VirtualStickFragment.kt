package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.Helper
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.BasicAircraftControlVM
import dji.sampleV5.moduleaircraft.models.SimulatorVM
import dji.sampleV5.moduleaircraft.models.VirtualStickVM
import dji.sampleV5.moduleaircraft.virtualstick.OnScreenJoystick
import dji.sampleV5.moduleaircraft.virtualstick.OnScreenJoystickListener
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sampleV5.modulecommon.models.LeicaControllerVM
import dji.sampleV5.modulecommon.util.SaveList
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.virtualstick.Stick
import dji.v5.utils.common.JsonUtil
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.utils.common.StringUtils
import kotlinx.android.synthetic.main.frag_virtual_stick_page.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/11
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class VirtualStickFragment : DJIFragment() {

    private val basicAircraftControlVM: BasicAircraftControlVM by activityViewModels()
    private val virtualStickVM: VirtualStickVM by activityViewModels()
    private val simulatorVM: SimulatorVM by activityViewModels()
    private val deviation: Double = 0.02
    private val leicaCtlVM: LeicaControllerVM by viewModels()

    // TS data receiver
    private val posData = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_virtual_stick_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        widget_horizontal_situation_indicator.setSimpleModeEnable(false)
        initBtnClickListener()
        initStickListener()

        // Buttons for TS16
        connectTSBtnLintener()
        readTSBtnListener()
        stopTSBtnListener()
        disconnectTSBtnListener()

        // Button for controlling Drone
        selfDriveBtnListener()

        // Prism Position Data Observer
        leicaCtlVM.prismPos.observe(viewLifecycleOwner) {
            tv_leicaValue.text = StringUtils.getResStr(R.string.tv_leicaValue, it)
            posData.add(it)

            // TS16で取得したデータを保存する
            //  リストをバッチ的に保存する
            val saveBatchSize = 100
            //   保存するファイルパス
            val homeDir = Environment.getExternalStorageDirectory().absolutePath
            val saveDir = "$homeDir/TS16Data"
            //  EditTextから保存ファイル名を取得する
            val filename = et_saveFileName.text.toString()
            val filepath = "$saveDir/$filename.txt"
            val saver = SaveList()
            saver.set(filepath)

            if(posData.size >= saveBatchSize){
                var time = saver.save(posData)
                Log.d("FileSaveTime TS Data", "$time ms")
                posData.clear()
            }
        }

        virtualStickVM.listenRCStick()
        virtualStickVM.currentSpeedLevel.observe(viewLifecycleOwner) {
            updateVirtualStickInfo()
        }
        virtualStickVM.useRcStick.observe(viewLifecycleOwner) {
            updateVirtualStickInfo()
        }
        virtualStickVM.currentVirtualStickStateInfo.observe(viewLifecycleOwner) {
            updateVirtualStickInfo()
        }
        virtualStickVM.stickValue.observe(viewLifecycleOwner) {
            updateVirtualStickInfo()
        }
        virtualStickVM.virtualStickAdvancedParam.observe(viewLifecycleOwner) {
            updateVirtualStickInfo()
        }
        simulatorVM.simulatorStateSb.observe(viewLifecycleOwner) {
            simulator_state_info_tv.text = it
        }
    }

    private fun initBtnClickListener() {
        btn_enable_virtual_stick.setOnClickListener {
            virtualStickVM.enableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("enableVirtualStick success.")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("enableVirtualStick error,$error")
                }
            })
        }
        btn_disable_virtual_stick.setOnClickListener {
            virtualStickVM.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("disableVirtualStick success.")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("disableVirtualStick error,${error})")
                }
            })
        }
        btn_set_virtual_stick_speed_level.setOnClickListener {
            val speedLevels = doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
            initPopupNumberPicker(Helper.makeList(speedLevels)) {
                virtualStickVM.setSpeedLevel(speedLevels[indexChosen[0]])
                resetIndex()
            }
        }
        btn_take_off.setOnClickListener {
            basicAircraftControlVM.startTakeOff(object :
                CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(t: EmptyMsg?) {
                    ToastUtils.showToast("start takeOff onSuccess.")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("start takeOff onFailure,$error")
                }
            })
        }
        btn_landing.setOnClickListener {
            basicAircraftControlVM.startLanding(object :
                CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(t: EmptyMsg?) {
                    ToastUtils.showToast("start landing onSuccess.")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("start landing onFailure,$error")
                }
            })
        }
        btn_use_rc_stick.setOnClickListener {
            virtualStickVM.useRcStick.value = virtualStickVM.useRcStick.value != true
            if (virtualStickVM.useRcStick.value == true) {
                ToastUtils.showToast(
                    "After it is turned on," +
                            "the joystick value of the RC will be used as the left/ right stick value"
                )
            }
        }
        btn_set_virtual_stick_advanced_param.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "Set Virtual Stick Advanced Param",
                JsonUtil.toJson(virtualStickVM.virtualStickAdvancedParam.value), "", false
            ) {
                it?.apply {
                    val param = JsonUtil.toBean(this, VirtualStickFlightControlParam::class.java)
                    if (param == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    virtualStickVM.virtualStickAdvancedParam.postValue(param)
                }
            }
        }
        btn_send_virtual_stick_advanced_param.setOnClickListener {
            virtualStickVM.virtualStickAdvancedParam.value?.let {
                virtualStickVM.sendVirtualStickAdvancedParam(it)
            }
        }
        btn_enable_virtual_stick_advanced_mode.setOnClickListener {
            virtualStickVM.enableVirtualStickAdvancedMode()
        }
        btn_disable_virtual_stick_advanced_mode.setOnClickListener {
            virtualStickVM.disableVirtualStickAdvancedMode()
        }
    }

    private fun selfDriveBtnListener() {
        fun goStraight(p: Float, duration: Long){
            println("Go Straight!!!")
            runBlocking {
                // 右のvirtual stickを真っ直ぐに倒す ： 直進する
                virtualStickVM.setRightPosition(
                    0,
                    (p * Stick.MAX_STICK_POSITION_ABS).toInt())
                delay(duration)
            }
            // 直進した後，スティックを中央に戻す
            virtualStickVM.setRightPosition(0, 0)
        }
        fun turnRight(p:Float, duration: Long) {
            println("Turn Right!!!")
            runBlocking {
                // 左のvirtual stickを右に倒す：時計回りに回転
                virtualStickVM.setLeftPosition(
                    (p * Stick.MAX_STICK_POSITION_ABS).toInt(),
                    0 )
                delay(duration)
            }
            // 回転した後，スティックを中央に戻す
            virtualStickVM.setLeftPosition(0, 0)
        }

        // Main Program is below
        btn_selfDrive_test.setOnClickListener{
            // start starting
            println("Start!!")
//            turnRight(1.0F, 1200) // p=1.0, d=1200 : Rotate about 90°
            goStraight(0.05F, 1500)
            runBlocking { delay(1000) }
            turnRight(1.0F, 1200)
            runBlocking { delay(1000) }
            goStraight(0.05F, 1500)
            runBlocking { delay(1000) }
            turnRight(1.0F, 1200)
            runBlocking { delay(1000) }
            goStraight(0.05F, 1500)

            println("Finish!!")
        }
    }

    private fun connectTSBtnLintener() {
        bt_connectTS.setOnClickListener {
                if(leicaCtlVM.connect() == 0) {
                    Log.d(tag, "successfully connected")
                    ToastUtils.showToast("Success connecting to TS16")
                } else {
                    Log.d(tag, "failed to connect")
                    ToastUtils.showToast("Failed connecting to TS16")
                }
            }
    }

    // Start Reading
    private fun readTSBtnListener() {
        bt_readTS.setOnClickListener {
            leicaCtlVM.read()
            ToastUtils.showToast("Start Reading")
        }
    }

    // Stop Reading
    private fun stopTSBtnListener() {
        bt_stopTS.setOnClickListener {
            leicaCtlVM.stop()
            ToastUtils.showToast("Stop Reading")
        }
    }

    // Disconnect TS16
    private fun disconnectTSBtnListener() {
        bt_disconnectTS.setOnClickListener {
            leicaCtlVM.close()
            ToastUtils.showToast("Disconnect")

            }
    }


    private fun initStickListener() {
        left_stick_view.setJoystickListener(object : OnScreenJoystickListener {
            override fun onTouch(joystick: OnScreenJoystick?, pX: Float, pY: Float) {
                println("pX=$pX")
                var leftPx = 0F
                var leftPy = 0F

                if (abs(pX) >= deviation) {
                    leftPx = pX
                }

                if (abs(pY) >= deviation) {
                    leftPy = pY
                }

                virtualStickVM.setLeftPosition(
                    (leftPx * Stick.MAX_STICK_POSITION_ABS).toInt(),
                    (leftPy * Stick.MAX_STICK_POSITION_ABS).toInt()
                )
            }
        })
        right_stick_view.setJoystickListener(object : OnScreenJoystickListener {
            override fun onTouch(joystick: OnScreenJoystick?, pX: Float, pY: Float) {
                var rightPx = 0F
                var rightPy = 0F

                if (abs(pX) >= deviation) {
                    rightPx = pX
                }

                if (abs(pY) >= deviation) {
                    rightPy = pY
                }

                virtualStickVM.setRightPosition(
                    (rightPx * Stick.MAX_STICK_POSITION_ABS).toInt(),
                    (rightPy * Stick.MAX_STICK_POSITION_ABS).toInt()
                )
            }
        })
    }

    private fun updateVirtualStickInfo() {
        val builder = StringBuilder()
        builder.append("Speed level:").append(virtualStickVM.currentSpeedLevel.value)
        builder.append("\n")
        builder.append("Use rc stick as virtual stick:").append(virtualStickVM.useRcStick.value)
        builder.append("\n")
        builder.append("Is virtual stick enable:").append(virtualStickVM.currentVirtualStickStateInfo.value?.state?.isVirtualStickEnable)
        builder.append("\n")
        builder.append("Current control permission owner:").append(virtualStickVM.currentVirtualStickStateInfo.value?.state?.currentFlightControlAuthorityOwner)
        builder.append("\n")
        builder.append("Change reason:").append(virtualStickVM.currentVirtualStickStateInfo.value?.reason)
        builder.append("\n")
        builder.append("Rc stick value:").append(virtualStickVM.stickValue.value?.toString())
        builder.append("\n")
        builder.append("Is virtual stick advanced mode enable:").append(virtualStickVM.currentVirtualStickStateInfo.value?.state?.isVirtualStickAdvancedModeEnabled)
        builder.append("\n")
        builder.append("Virtual stick advanced mode param:").append(virtualStickVM.virtualStickAdvancedParam.value?.toJson())
        builder.append("\n")
        mainHandler.post {
            virtual_stick_info_tv.text = builder.toString()
        }
    }
}