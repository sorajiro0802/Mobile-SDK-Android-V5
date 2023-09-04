package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.data.DJIToastResult
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.value.airlink.WlmDongleInfo
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.lte.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LTEVM : DJIViewModel() {

    val lteAuthenticationInfo = MutableLiveData<LTEAuthenticationInfo>()
    val lteLinkInfo = MutableLiveData<LTELinkInfo>()
    val acWlmDongleInfo = MutableLiveData<MutableList<WlmDongleInfo>>()
    val rcWlmDongleInfo = MutableLiveData<MutableList<WlmDongleInfo>>()

    private val lteAuthenticationInfoListener = { info: LTEAuthenticationInfo ->
        lteAuthenticationInfo.postValue(info)
    }

    private val lteLinkInfoListener = { info: LTELinkInfo ->
        lteLinkInfo.postValue(info)
    }

    private val lteDongleInfoListener = object :
        LTEDongleInfoListener {
        override fun onLTEAircraftDongleInfoUpdate(aircraftDongleInfos: MutableList<WlmDongleInfo>) {
            acWlmDongleInfo.postValue(aircraftDongleInfos)
        }

        override fun onLTERemoteControllerDongleInfoUpdate(remoteControllerDongleInfos: MutableList<WlmDongleInfo>) {
            rcWlmDongleInfo.postValue(remoteControllerDongleInfos)
        }
    }

    fun initListener() {
        LTEManager.getInstance().addLTEAuthenticationInfoListener(lteAuthenticationInfoListener)
        LTEManager.getInstance().addLTELinkInfoListener(lteLinkInfoListener)
        LTEManager.getInstance().addLTEDongleInfoListener(lteDongleInfoListener)
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
        LTEManager.getInstance().removeLTEAuthenticationInfoListener(lteAuthenticationInfoListener)
        LTEManager.getInstance().removeLTELinkInfoListener(lteLinkInfoListener)
        LTEManager.getInstance().removeLTEDongleInfoListener(lteDongleInfoListener)
    }

    fun updateLTEAuthenticationInfo() {
        LTEManager.getInstance().updateLTEAuthenticationInfo(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun getLTEAuthenticationVerificationCode(phoneAreaCode: String, phoneNumber: String) {
        LTEManager.getInstance().getLTEAuthenticationVerificationCode(phoneAreaCode, phoneNumber, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun startLTEAuthentication(phoneAreaCode: String, phoneNumber: String, verificationCode: String) {
        LTEManager.getInstance().startLTEAuthentication(phoneAreaCode, phoneNumber, verificationCode, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun setLTEEnhancedTransmissionType(lteLinkType: LTELinkType) {
        LTEManager.getInstance().setLTEEnhancedTransmissionType(lteLinkType, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun getLTEEnhancedTransmissionType() {
        LTEManager.getInstance().getLTEEnhancedTransmissionType(object :
            CommonCallbacks.CompletionCallbackWithParam<LTELinkType> {
            override fun onSuccess(lteLinkType: LTELinkType) {
                toastResult?.postValue(DJIToastResult.success(lteLinkType.name))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }
}