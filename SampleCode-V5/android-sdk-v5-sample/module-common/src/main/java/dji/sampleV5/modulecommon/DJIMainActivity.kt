package dji.sampleV5.modulecommon

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dji.sampleV5.modulecommon.models.*
import dji.sampleV5.modulecommon.util.Helper
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.PermissionUtil
import dji.v5.utils.common.StringUtils
import dji.v5.utils.common.ToastUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/10
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
abstract class DJIMainActivity : AppCompatActivity() {
    val tag: String = LogUtils.getTag(this)
    private val permissionArray = arrayListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.KILL_BACKGROUND_PROCESSES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    init {
        permissionArray.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

        }
    }
    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
    protected val msdkInfoVm: MSDKInfoVm by viewModels()
    private val msdkManagerVM: MSDKManagerVM by globalViewModels()
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()
    private val posData = mutableListOf<String>()
    private var timesync: TimeSyncVM = TimeSyncVM()

    abstract fun prepareUxActivity()

    abstract fun prepareTestingToolsActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        initMSDKInfoView()
        observeSDKManagerStatus()
        checkPermissionAndRequest()

        val timeSyncBtn: Button = findViewById<Button>(R.id.synctimeButton)
        val timeSyncStopBtn: Button = findViewById<Button>(R.id.syncTime_stopButton); timeSyncStopBtn.isEnabled = false
        val timeSyncAddr: EditText = findViewById<EditText>(R.id.edit_text_ipAddr)
        val timeDiff: TextView = findViewById<TextView>(R.id.text_view_timeDiff)

        timeSyncBtn.setOnClickListener(View.OnClickListener {
            // Button Toggle
            it.isEnabled = false
            timeSyncStopBtn.isEnabled = true
            // Start synchronization
            timesync.setAddress(timeSyncAddr.text.toString(), 8020)
            timesync.sync()
        })
        timeSyncStopBtn.setOnClickListener(View.OnClickListener {
            // Button Toggle
            it.isEnabled = false
            timeSyncBtn.isEnabled = true
            // Stop synchro
            timesync.stop()
        })

        // 時間同期サーバから受信した時間データ
        timesync.serverTime.observe(this, Observer{
            Log.d(tag, "Data:$it")
            val server_date = it
            val client_data = timesync.getNowDate()
            val timediff = timesync.calcTimeDiff(server_date, client_data)
//            timeDiffData.add(timediff)

            timeDiff.text = "time diff: $timediff ms"
        })



        val TSConnectBtn: Button = findViewById<Button>(R.id.bt_connectTS)
        val TSReadBtn: Button = findViewById<Button>(R.id.bt_readTS)
        val tvLeicaValue: TextView = findViewById<TextView>(R.id.tv_leicaValue)
        TSConnectBtn.setOnClickListener{v->(
                    if(msdkInfoVm.leicaController.connect() == 0){
                        Log.d(tag, "successfully connected")
                        this.exceptionToast("Success connecting to TS16")
                        msdkInfoVm.updateTSConnectionStatus()
                    } else {
                        Log.d(tag, "failed to connect")
                        this.exceptionToast("Failed connecting to TS16")
                        msdkInfoVm.updateTSConnectionStatus()
                    }
            )}
        TSReadBtn.setOnClickListener(View.OnClickListener{
            msdkInfoVm.leicaController.read()
        })
        msdkInfoVm.leicaController.prismPos.observe(this, Observer {
            tvLeicaValue.text = StringUtils.getResStr(R.string.tv_leicaValue, it)
            posData.add(it)

            // TS16で取得したデータを保存する
            val saveBatchSize = 10
            val homeDir = Environment.getExternalStorageDirectory().absolutePath
            val saveDir = "$homeDir/TS16Data"
            val filename = "tmp_data_ts16.txt"
            val filepath = File("$saveDir/$filename")
            if(posData.size >= saveBatchSize){
                try{
                    val outstream: FileOutputStream = FileOutputStream(filepath, true)
                    val writer: OutputStreamWriter = OutputStreamWriter(outstream)
                    for (data in posData) {
                        writer.write(data)
                        writer.write("\r")
                    }
                    writer.flush()
                    writer.close()
                    posData.clear()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch( e:IOException){
                    e.printStackTrace()
                }
            }
        })


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    private fun handleAfterPermissionPermitted() {
        prepareTestingToolsActivity()
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        ToastUtils.init(this)
        msdkInfoVm.msdkInfo.observe(this) {
            text_view_version.text = StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            text_view_product_name.text = StringUtils.getResStr(R.string.product_name, it.productType.name)
            text_view_package_product_category.text = StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            text_view_is_debug.text = StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
            text_core_info.text = it.coreInfo.toString()
            text_view_TSConnection.text = StringUtils.getResStr(R.string.ts_connection, it.tsConnection)
//            tv_leicaValue.text = StringUtils.getResStr(R.string.tv_leicaValue, it.tsValue)
        }
        baseMainActivityVm.sdkNews.observe(this) {
            item_news_msdk.setTitle(StringUtils.getResStr(it.title))
            item_news_msdk.setDescription(StringUtils.getResStr(it.description))
            item_news_msdk.setDate(it.date)

            item_news_uxsdk.setTitle(StringUtils.getResStr(it.title))
            item_news_uxsdk.setDescription(StringUtils.getResStr(it.description))
            item_news_uxsdk.setDate(it.date)
        }

        icon_sdk_forum.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.sdk_forum_url))
        }
        icon_release_node.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.release_node_url))
        }
        icon_tech_support.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.tech_support_url))
        }
        view_base_info.setOnClickListener {
            baseMainActivityVm.doPairing {
                ToastUtils.showToast(it)
            }
        }
    }

    private fun observeSDKManagerStatus() {
        msdkManagerVM.lvRegisterState.observe(this) { resultPair ->
            val statusText: String?
            if (resultPair.first) {
                ToastUtils.showToast("Register Success")
                statusText = StringUtils.getResStr(this, R.string.registered)
                msdkInfoVm.initListener()
                handler.postDelayed({
                    prepareUxActivity()
                }, 5000)
            } else {
                ToastUtils.showToast("Register Failure: ${resultPair.second}")
                statusText = StringUtils.getResStr(this, R.string.unregistered)
            }
            text_view_registered.text = StringUtils.getResStr(R.string.registration_status, statusText)
        }

        msdkManagerVM.lvProductConnectionState.observe(this) { resultPair ->
            ToastUtils.showToast("Product: ${resultPair.second} ,ConnectionState:  ${resultPair.first}")
        }

        msdkManagerVM.lvProductChanges.observe(this) { productId ->
            ToastUtils.showToast("Product: $productId Changed")
        }

        msdkManagerVM.lvInitProcess.observe(this) { processPair ->
            ToastUtils.showToast("Init Process event: ${processPair.first.name}")
        }

        msdkManagerVM.lvDBDownloadProgress.observe(this) { resultPair ->
            ToastUtils.showToast("Database Download Progress current: ${resultPair.first}, total: ${resultPair.second}")
        }
    }


    fun <T> enableDefaultLayout(cl: Class<T>) {
        enableShowCaseButton(default_layout_button, cl)
    }

    fun <T> enableWidgetList(cl: Class<T>) {
        enableShowCaseButton(widget_list_button, cl)
    }

    fun <T> enableTestingTools(cl: Class<T>) {
        enableShowCaseButton(testing_tool_button, cl)
    }

    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    private fun checkPermissionAndRequest() {
        if (!checkPermission()) {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        for (i in permissionArray.indices) {
            if (!PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                return false
            }
        }
        return true
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result?.entries?.forEach {
            if (it.value == false) {
                requestPermission()
                return@forEach
            }
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissionArray.toArray(arrayOf()))
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        ToastUtils.destroy()
    }
    private fun exceptionToast(res: String) {
        Toast.makeText(this@DJIMainActivity, res, Toast.LENGTH_SHORT).show()
    }
}