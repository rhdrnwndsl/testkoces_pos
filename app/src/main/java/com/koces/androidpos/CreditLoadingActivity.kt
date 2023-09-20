package com.koces.androidpos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.Setting.PayDeviceType
import com.koces.androidpos.sdk.Utils.CCTcpPacket
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.db.sqliteDbSdk
import com.koces.androidpos.sdk.van.Constants
import com.koces.androidpos.sdk.van.TcpInterface
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.concurrent.timer
import kotlin.plus

class CreditLoadingActivity : BaseActivity() {
    val TAG:String = CreditLoadingActivity::class.java.simpleName

    /** 앱투앱에 해쉬맵을 보낼때 정상오류메세지일 경우 처리하는 키  */
    private val RETURN_CANCEL = 1

    /** 사인패드의 결과 값이 들어올 경우 이를 리시브 하기 위한 키  */
    private val REQUEST_SIGNPAD = 10001

    /** 앱투앱에 해쉬맵을 보낼때 정상메세지일 경우 처리하는 키  */
    private val RETURN_OK = 0
    lateinit var imm: InputMethodManager
    private val mMaxinumTime:Long = 1000
    lateinit var mTvwTimer: TextView
    lateinit var mTvwMessage: TextView
    var mCount:Int = 30
    var mTrdType:String =""
    var mTermID:String = ""
    var mStoreName:String = ""
    var mStoreAddr:String = ""
    var mStoreNumber:String = ""
    var mStorePhone:String = ""
    var mStoreOwner:String = ""
    var mTrdCode:String =""
    var mTrdAmt:Int = 0
    var mTaxAmt:Int = 0
    var mSvcAmt:Int = 0
    var mTaxFreeAmt:Int = 0
    var mMonth:Int = 0
    var mAuNo:String=""
    var mAuDate:String=""
    var mTradeNo:String=""      //고유거래키 취소 번호
    var mDscYn:String = ""
    var mFBYn:String = ""
    private val mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()
    lateinit var mCountTimer: Timer

    lateinit var mBtnCancel: Button

    //sdk
    lateinit var mPaySdk: PaymentSdk
    lateinit var mBleSdk: BlePaymentSdk
    lateinit var mCatSdk: CatPaymentSdk

    //거래를 종료하고 다시 크레딧페이지로 이동할 때 보내줄 데이터
    var mEdtMoney:String=""
    var mEdtTxf:String=""
    var mEdtSvc:String=""

    /** 사인패드 진행시에는 카운트다운을 하지 않는다. 그외의 경우 다시 카운트다운을 한다 */
    var mCountSign:Boolean = false

    val mProcMsg1:String = "서버로 처리 요청"
    /** EOT취소를 위한 변수  */
    var mEotCancel = 0

    /** 앱투앱_취소메세지_등록된 TID가 없거나 등록된 TID와 다릅니다  */
    private val ERROR_MCHTID_DIFF = "등록된 TID가 없거나 등록된 TID와 다릅니다."

    /** 앱투앱_취소메세지_장치가 설정 되지 않았습니다  */
    private val ERROR_NODEVICE = "장치가 설정 되지 않았습니다."

    /** 앱투앱_취소메세지_취소 구분자가 없습니다  */
    private val ERROR_EMPTY_CANCEL_REASON = "취소 구분자가 없습니다."

    /** 앱투앱_취소메세지_고객번호가 없을시 현금영수증요청했는데 서명패드 미사용or터치사명일경우  */
    private val ERROR_NOSIGNPAD = "고객 번호가 없거나 장치가 연결 되어 있지 않습니다."

    /** 앱투앱 비엘이 장치에서 현금 거래영수증 번호 없어 , 키인으로 요청 한 경우에 에려 메세지 211230 kim.jy  */
    private val ERROR_NOBLEKEYIN = "고객번호가 없습니다. 고객번호를 포함하여 거래하여 주십시오."

    /** 앱투앱_취소메세지_원승인번호 혹은 원승인일자가 없을경우  */
    private val ERROR_NOAUDATE = "원승인번호 원승인일자가 없습니다."

    /** 앱투앱_취소메세지_가맹점다운로드 시 필요데이터가 없는경우  */
    private val ERROR_NODATA = "필요 데이터가 없습니다."

    /** 앱투앱_취소메세지_네트워크 연결이 불량인 경우  */
    private val ERROR_NO_NETWORK_SERVICE = "네트워크 연결 상태를 확인 해 주십시요"

    /** 앱투앱_취소메세지_사용하지않음  */
    private val ERROR_NO_APP_RUNNING = "앱이 미실행 중입니다."

    /** 앱투앱_취소메세지_사용하지않음  */
    private val ERROR_NO_DEVICE_INIT = "장치교체 후 장치설정을 완료하지 않았습니다."

    /** 앱투앱_취소메세지  */
    private val ERROR_SET_ENVIRONMENT = "환경설정에서 장치설정을 해야합니다."

    /** 앱투앱_취소메세지_전자서명데이터없음(2. bmp data 사용인데 데이터를 보내오지 않았다)  */
    private val ERROR_NO_BMPDATA = "BMP 데이터가 올바르지 않습니다"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit_loading)
        initRes()
        Setting.setIsAppForeGround(1);
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mKocesSdk.BleUnregisterReceiver(this)
        mKocesSdk.BleregisterReceiver(Setting.getTopContext())
    }

    fun initRes()
    {
        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this)
        mKocesSdk.BleregisterReceiver(this)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        mTvwTimer = findViewById(R.id.load_credit_timer)
        mTvwMessage = findViewById(R.id.load_credit_message)

        mKocesSdk.BleConnectionListener(ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mKocesSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
                BleDeviceInfo()
            } else {
                ReadyDialogHide();
            }
        })
        mKocesSdk.BleWoosimConnectionListener(bleWoosimInterface.ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mKocesSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
                BleDeviceInfo()
            } else {
                ReadyDialogHide();
            }
        })

        //취소 버튼을 누르면 메인 페이지로 이동 한다.
        mBtnCancel = findViewById(R.id.load_credit_btn_cancel)
        mBtnCancel.setOnClickListener {
            //여기서 ble/usb 종료커맨드를 날린다
            cancelTimer()
            if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                mKocesSdk.DeviceReset()
            } else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
//                mKocesSdk.__BLEPosinit("99", null)
                mKocesSdk.__BLEReadingCancel(null)
            } else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                if (mCatSdk != null)
                {
                    mCatSdk.Cat_SendCancelCommandE(false)
                }
                mKocesSdk.mTcpClient.DisConnectCatServer()


            }

            if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("Month", mMonth)    //할부
            }
            else
            {
                intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                intent.putExtra("AppToApp",2)
            }
            startActivity(intent)

        }
        mBtnCancel.visibility = View.INVISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            if (!(mTvwMessage.text.toString()).contains("거래 승인")) {
                mBtnCancel.visibility = View.VISIBLE
            }
        },1000)
        //값을 받아와서 처리 한다.
        if(intent.hasExtra("TrdType")) {
            mTrdType = intent.getStringExtra("TrdType").toString()
            mTermID = intent.getStringExtra("TermID").toString() ?:""
            if (mTermID == "" )
            {
                mTermID = getTID()
            }
            mStoreName = intent.getStringExtra("StoreName").toString() ?:""
            mStoreAddr = intent.getStringExtra("StoreAddr").toString() ?:""
            mStoreNumber = intent.getStringExtra("StoreNumber").toString() ?:""
            mStorePhone = intent.getStringExtra("StorePhone").toString() ?:""
            mStoreOwner = intent.getStringExtra("StoreOwner").toString() ?:""
            mTrdAmt = intent.getIntExtra("TrdAmt",0)
            mTaxAmt = intent.getIntExtra("TaxAmt",0)
            mSvcAmt = intent.getIntExtra("SvcAmt",0)
            mTaxFreeAmt = intent.getIntExtra("TaxFreeAmt",0)
            mMonth = Integer.valueOf(intent.getStringExtra("Month").toString() ?:"0")
            mAuNo = intent.getStringExtra("AuNo").toString() ?: ""
            mAuDate = intent.getStringExtra("AuDate").toString() ?: ""
            if(!Utils.isNullOrEmpty(intent.getStringExtra("TradeNo").toString())){
                mTradeNo = intent.getStringExtra("TradeNo").toString()
            }else{
                mTradeNo = ""
            }
            mDscYn = intent.getStringExtra("DscYn").toString() ?: "1"
            Setting.g_sDigSignInfo = mDscYn
            mFBYn = intent.getStringExtra("FBYn").toString() ?: "0"
            mTrdCode = intent.getStringExtra("TrdCode").toString() ?:""

            mEdtMoney = intent.getStringExtra("EdtMoney").toString() ?: ""
            mEdtSvc = intent.getStringExtra("EdtSvc").toString() ?: ""
            mEdtTxf = intent.getStringExtra("EdtTxf").toString() ?: ""

            when(Setting.g_PayDeviceType){
                PayDeviceType.NONE -> {
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                PayDeviceType.BLE -> {
                    Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this, Constants.BLE_TIME_OUT)))
                    Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this, Constants.BLE_TIME_OUT)))
                    mCount = Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT))
                    mTvwMessage.text = "IC카드를 넣어주세요"
                }
                PayDeviceType.CAT -> {
                    Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this, Constants.CAT_TIME_OUT)))
                    Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this, Constants.CAT_TIME_OUT)))
                    mCount = Integer.valueOf(Setting.getPreference(this,Constants.CAT_TIME_OUT))
                    mTvwMessage.text = "거래 승인 중입니다"
                }
                PayDeviceType.LINES ->{
                    Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this, Constants.USB_TIME_OUT)))
                    Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this, Constants.USB_TIME_OUT)))
                    mCount = Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT))
                    mTvwMessage.text = "IC카드를 넣어주세요"
                }
                else->{ //else 경우는 없음
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
            }

            Log.d("CreditLoading",mKocesSdk.activity.toString() )
            Log.d("CreditLoading",Setting.getTopContext().toString() )
            ShowStartTimer()
        }
        else
        {
            //TrdType 이 없는경우. 오류다. 메인으로 보낸다
            intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
    }

    /**
     * ble 리더기 식별번호 표시를 위한 장치 정보 요청 함
     */
    var _bleDeviceCheck: Int = 0
    private fun BleDeviceInfo() {
        _bleDeviceCheck += 1
        mKocesSdk.__BLEPosInfo(
            Utils.getDate("yyyyMMddHHmmss"),
            ResDataListener { res: ByteArray ->
                ReadyDialogHide()
                Toast.makeText(mKocesSdk.getContext(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
                if (res[3] == 0x15.toByte()) {
                    //장비에서 NAK 올라 옮
                    return@ResDataListener
                }
                if (res.size == 6) {      //ACK가 6바이트 올라옴
                    return@ResDataListener
                }
                if (res.size < 50) {
                    return@ResDataListener
                }
                _bleDeviceCheck = 0
                val KByteArray = KByteArray(res)
                KByteArray.CutToSize(4)
                val authNum = String(KByteArray.CutToSize(32)) //장비 인식 번호
                val serialNum = String(KByteArray.CutToSize(10))
                val version = String(KByteArray.CutToSize(5))
                val key = String(KByteArray.CutToSize(2))
                Setting.mBleHScrKeyYn = key
                Setting.setPreference(
                    mKocesSdk.getActivity(),
                    Constants.REGIST_DEVICE_NAME,
                    authNum
                )
                Setting.setPreference(
                    mKocesSdk.getActivity(),
                    Constants.REGIST_DEVICE_VERSION,
                    version
                )
                Setting.setPreference(
                    mKocesSdk.getActivity(),
                    Constants.REGIST_DEVICE_SN,
                    serialNum
                )
                //공백을 제거하여 추가 한다.
                val tmp = authNum.trim { it <= ' ' }
                //            Setting.mAuthNum = authNum.trim(); //BLE는 이것을 쓰지 않는다. 유선이 사용한다
//            mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다

            })


        Handler().postDelayed(Runnable {
            if (_bleDeviceCheck == 1) {
                mKocesSdk.BleDisConnect()
                Handler().postDelayed({
                    ReadyDialogShow(
                        mKocesSdk.getActivity(),
                        "장치 연결 중 입니다.",
                        0
                    )
                }, 200)
                Handler().postDelayed({
                    //                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                    mKocesSdk.BleConnect(
                        mKocesSdk.getActivity(),
                        Setting.getPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_ADDR
                        ),
                        Setting.getPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME
                        )
                    )
                }, 500)
                return@Runnable
            } else if (_bleDeviceCheck > 1) {
                _bleDeviceCheck = 0
                Toast.makeText(
                    mKocesSdk.getActivity(),
                    "블루투스 통신 오류. 다시 시도해 주세요",
                    Toast.LENGTH_SHORT
                ).show()
                mKocesSdk.BleDisConnect()
                return@Runnable
            } else if (_bleDeviceCheck == 0) {
                //정상
            }
        }, 2000)
    }

    private fun ShowStartTimer(){

        mTvwTimer.text = mCount.toString()
        mCountTimer = timer(period = mMaxinumTime, initialDelay = 1000){
            if(mCount==0){

                Handler(Looper.getMainLooper()).postDelayed({
                    //네트워크 문제든 기타의 문제로 타이머가 종료되었는데 sdk 로부터 별다른 메세지를 못받아서 계속 로딩페이지에 있을 경우 종료시킨다.
                    if (Setting.getTopContext().toString().contains("CreditLoadingActivity"))
                    {
                        when(Setting.g_PayDeviceType){
                            PayDeviceType.NONE -> {
                                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

                            }
                            PayDeviceType.BLE -> {
//                                mKocesSdk.__BLEPosinit("99", null)
                                mKocesSdk.__BLEReadingCancel(null)
                                mKocesSdk.mTcpClient.Dispose()
                            }
                            PayDeviceType.CAT -> {
                                mKocesSdk.mTcpClient.DisConnectCatServer()
                            }
                            PayDeviceType.LINES ->{
                                mKocesSdk.DeviceReset()
                                mKocesSdk.mTcpClient.Dispose()
                            }
                            else->{ //else 경우는 없음
                                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

                            }
                        }
                        if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("Month", mMonth)    //할부
                        }
                        else
                        {
                            intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                    }

                }, 2000)
                cancelTimer()



                ShowToastBox("대기시간을 초과하였습니다")

//                intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
//                startActivity(intent)
                return@timer
            }
            mCount -= 1
            runOnUiThread {
                if(mCountSign)  //사인패드중일경우
                {
                    mTvwTimer.text = ""
                }
                else
                {
                    mTvwTimer.text = mCount.toString()
                }
            }

        }

        Credit()
    }

    fun Credit()
    {
        when(Setting.g_PayDeviceType){
            PayDeviceType.NONE -> {
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
            PayDeviceType.BLE -> {
                BleCredit()
                return
            }
            PayDeviceType.CAT -> {
                mBtnCancel.visibility = View.GONE
                CatCredit()
                return
            }
            PayDeviceType.LINES ->{
                LineCredit()
            }
            else->{ //else 경우는 없음
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
        }
    }

    fun BleCredit()
    {
        if(!Setting.bleisConnected)
        {
            if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("Month", mMonth)    //할부
            }
            else
            {
                intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                intent.putExtra("AppToApp",2)
            }
            startActivity(intent)
            return
        }

        mBleSdk = BlePaymentSdk(1) { result: String?, Code: String, resultData: HashMap<String, String> ->
            if (Code == "SHOW") {
                if (result != null) {
                    ShowToastBox(result)
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                }
                else
                {
                    ShowToastBox("서버통신 오류")
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                }
            } else {
                /* 콜백으로 결과를 받아온다 */
                if (result != null) {
                    CallBackReciptResult(result, Code, resultData)
                }
                else {
                    CallBackReciptResult("", Code, resultData)
                }
            }
        }

        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (mTaxFreeAmt == null ) {
            mTaxFreeAmt = 0
        }
        if (mTrdAmt == null) {
            mTrdAmt = 0
        }
//        mTrdAmt += mTaxFreeAmt

        var CanRes = ""
        if (mTrdCode == "t" || mTrdCode == "T") {
            mTrdCode = "a"
        } else {
            mTrdCode = "0"
        }
        CanRes = mTrdCode + mAuDate + mAuNo

        /* 로그기록. 신용거래요청 */

        /* 로그기록. 신용거래요청 */
        val mLog =
            "TrdType : " + mTrdType + "," + "단말기ID : " + mTermID + "," + "원거래일자 : " + mAuDate + "," + "원승인번호 : " + mAuNo + "," +
                    "거래금액 : " + mTrdAmt + "," + "세금 : " + mTaxAmt + "," + "봉사료 : " + mSvcAmt + "," + "비과세 : " + mTaxFreeAmt + "," +
                    "할부개월 : " + mMonth + "," + "거래구분 : " + mTrdCode + "," + "거래고유번호 : " + mTradeNo + "," +
                    "카드입력대기시간 : " + mCount + "," + "전자서명사융유무 : " + mDscYn + "," + "fallback사융유무 : " + mFBYn
        cout("RECV : KOCES_CREDIT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)

        //만일 거래고유키취소사용시 코세스거래고유번호사용
        if (mTrdCode == "a" && mTrdType == "A20") {
            if (mAuDate == "" || mAuNo == "") //만일 원승인번호 원승인일자가 없는경우
            {
                ShowToastBox(ERROR_NOAUDATE)
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
            Setting.g_sDigSignInfo = "4" //거래고유키 취소일 때 설정한다
            /* 거래고유키 취소시 앱투앱엑티비티에서 처리한다 */
            mKocesSdk.___ictrade(TCPCommand.CMD_ICTRADE_CANCEL_REQ,
                mTermID,
                Utils.getDate("yyMMddHHmmss"),
                Constants.TEST_SOREWAREVERSION,
                "",
                CanRes,
                "K",
                "",
                "",
                null,
                mTrdAmt.toString(),
                mTaxAmt.toString(),
                mSvcAmt.toString(),
                mTaxFreeAmt.toString(),
                "",
                mMonth.toString(),
                "",
                "",
                " ",
                "",
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                null,
                Setting.g_sDigSignInfo,
                "",
                "",
                null,
                "",
                "",
                mTradeNo,
                "",
                Utils.getMacAddress(this),
                Utils.getHardwareKey(this, false, mTermID),
                TcpInterface.DataListener { _rev ->
                    var _tmpEot = 0
                    while (true) //TCP EOT 수신 될때 까지 기다리기 위해서
                    {
                        if (Setting.getEOTResult() != 0) {
                            break
                        }
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                        _tmpEot++
                        if (_tmpEot >= 30) {
                            break
                        }
                    }
                    val _b = KByteArray(_rev)
                    _b.CutToSize(10)
                    val _responsecode = String(_b.CutToSize(3))
                    val tp = CCTcpPacket(_rev)
                    val code = String(tp.resData[0])
                    val authorizationnumber =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
                    val kocesTradeUniqueNumber =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                            tp.resData[3]
                        ) //koces거래고유번호
                    var tmpCardNum1 = String(tp.resData[4])
                    val tmpCardNum2 = tmpCardNum1.split("-").toTypedArray()
                    tmpCardNum1 = ""
                    for (n in tmpCardNum2) {
                        tmpCardNum1 += n
                    }

                    /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리  */
                    val _CardNum = tmpCardNum1.toByteArray()
                    var CardNo =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum)
                    val sb = StringBuffer()
                    sb.append(tmpCardNum1)
                    if (_CardNum.size > 12) {
                        sb.replace(8, 12, "****")
                    }
                    if (tmpCardNum1.indexOf("=") > 0) {
                        sb.replace(tmpCardNum1.indexOf("="), tmpCardNum1.indexOf("=") + 1, "*")
                    }

                    CardNo = sb.toString()

//                    val _CardNum = tmpCardNum1.toByteArray()
//                    if (_CardNum != null) {
//                        //System.arraycopy( data.get(4),0,_CardNum,0,6);
//                        for (i in 8 until _CardNum.size) {
//                            //* = 0x2A
//                            _CardNum[i] = 0x2A.toByte()
//                        }
//                        //카드번호가 올라오지만 그중 8개만 사용하고 나머지는 *로 바꾼다
//                    }
//                    val CardNo =
//                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum) //출력용카드번호

                    //카드 정보 삭제____________________________________________________________________________________
                    val rand = Random()
                    for (i in _CardNum.indices) {
                        //* = 0x2A
                        _CardNum[i] = rand.nextInt(255).toByte()
                    }
                    tp.resData[4] = _CardNum
                    Arrays.fill(_CardNum, 0x01.toByte())
                    tp.resData[4] = _CardNum
                    Arrays.fill(_CardNum, 0x00.toByte())
                    tp.resData[4] = _CardNum
                    val card_type =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6]) //카드종류
                    val issuer_code =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7]) //발급사코드
                    var issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                        tp.resData[8],
                        "EUC-KR"
                    ) //발급사명
                    val issuer_names = issuer_name.split(" ").toTypedArray()
                    issuer_name = ""
                    for (n in issuer_names) {
                        issuer_name += n
                    }
                    val purchaser_code =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9]) //매입사코드
                    var purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                        tp.resData[10],
                        "EUC-KR"
                    ) //매입사명
                    val purchaser_names = purchaser_name.split(" ").toTypedArray()
                    purchaser_name = ""
                    for (n in purchaser_names) {
                        purchaser_name += n
                    }
                    val ddc_status =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11]) //DDC 여부
                    val edc_status =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12]) //EDC 여부
                    var giftcard_money =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[15]) //기프트카드 잔액
                    val merchant_number =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[16]) //가맹점번호
                    val encryp_key_expiration_date =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                            tp.resData[25]
                        ) //암호키만료잔여일
                    val merchant_data =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[26]) //가맹점데이터
                    val sendData = HashMap<String, String>()
                    sendData["TrdType"] = tp.responseCode
                    sendData["TermID"] = tp.terminalID
                    sendData["TrdDate"] = tp.date
                    sendData["AnsCode"] = code
                    sendData["AuNo"] = authorizationnumber
                    sendData["TradeNo"] = kocesTradeUniqueNumber
                    if (CardNo.length > 8) {
                        sendData["CardNo"] = CardNo.substring(0, 8)
                    } else {
                        sendData["CardNo"] = CardNo
                    }
                    sendData["Keydate"] = encryp_key_expiration_date
                    sendData["MchData"] = merchant_data
                    sendData["CardKind"] =
                        card_type //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                    sendData["OrdCd"] = issuer_code //발급사코드
                    sendData["InpCd"] = purchaser_code //매입사코드
                    sendData["OrdNm"] = issuer_name //발급사명
                    sendData["InpNm"] = purchaser_name //매입사명
                    sendData["DDCYn"] = ddc_status //DDC 여부
                    sendData["EDCYn"] = edc_status //EDC 여부
                    if (giftcard_money == "")
                    {
                        giftcard_money = "0"
                    }
                    sendData["GiftAmt"] = giftcard_money //기프트카드 잔액
                    sendData["MchNo"] = merchant_number //가맹점번호
                    try {
                        sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                    } catch (ex: UnsupportedEncodingException) {
                    }
                    /* 로그기록 거래고유키취소 결과 */
                    val mLog =
                        "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + encryp_key_expiration_date + "," +
                                "가맹점데이터 : " + merchant_data + "," + "카드종류 : " + card_type + "," + "발급사코드 : " + issuer_code + "," + "발급사명 : " + issuer_name + "," +
                                "메입사코드 : " + purchaser_code + "," + "메입사명 : " + purchaser_name + "," + "DDC여부 : " + ddc_status + "," + "EDC여부 : " + edc_status + "," +
                                "기프트잔액 : " + giftcard_money + "," + "가맹점번호 : " + merchant_number
                    cout("SEND : KOCES_CREDIT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)
                    if (code == "0000")
                    {
                        if (card_type == "3" || card_type == "4")
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(
                                mTermID,
                                mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                                sqliteDbSdk.TradeMethod.Credit,
                                sqliteDbSdk.TradeMethod.Cancel,
                                mTrdAmt,
                                Utils.getByteToString_euc_kr(tp.resData[1]) ,
                                mTaxAmt,
                                mSvcAmt,
                                mTaxFreeAmt,
                                mMonth,
                                "",
                                "",
                                "",
                                CardNo,
                                card_type,
                                purchaser_name,
                                issuer_name,
                                merchant_number,
                                tp.date,
                                mAuDate,
                                authorizationnumber,
                                mAuNo,
                                kocesTradeUniqueNumber,
                                Utils.getByteToString_euc_kr(tp.resData[1]),
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        }
                        else
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(
                                mTermID,
                                mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                                sqliteDbSdk.TradeMethod.Credit,
                                sqliteDbSdk.TradeMethod.Cancel,
                                mTrdAmt,
                                giftcard_money ,
                                mTaxAmt,
                                mSvcAmt,
                                mTaxFreeAmt,
                                mMonth,
                                "",
                                "",
                                "",
                                CardNo,
                                card_type,
                                purchaser_name,
                                issuer_name,
                                merchant_number,
                                tp.date,
                                mAuDate,
                                authorizationnumber,
                                mAuNo,
                                kocesTradeUniqueNumber,
                                Utils.getByteToString_euc_kr(tp.resData[1]),
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        }

                    }
                    SendreturnData(RETURN_OK, sendData, "")

                })
        } else {
            if (mTrdType == "A10") {
                /* 신용요청인경우_A10 */
                mBleSdk.CreditIC(
                    this,
                    mTermID,
                    mTrdAmt.toString(),
                    mTaxAmt,
                    mSvcAmt,
                    mTaxFreeAmt,
                    mMonth.toString(),
                    "",
                    "",
                    "",
                    mTradeNo,
                    "",
                    false,
                    mFBYn,
                    mStoreName,
                    mStoreAddr,
                    mStoreNumber,
                    mStorePhone,
                    mStoreOwner
                )
            } else {
                if (mAuDate == "" || mAuNo == "") //만일 원승인번호 원승인일자가 없는경우
                {
                    ShowToastBox(ERROR_NOAUDATE)
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
                /* 신용취소인경우_A20 */
                mBleSdk.CreditIC(
                    this,
                    mTermID,
                    mTrdAmt.toString(),
                    mTaxAmt,
                    mSvcAmt,
                    mTaxFreeAmt,
                    mMonth.toString(),
                    mAuDate,
                    CanRes,
                    "",
                    mTradeNo,
                    "",
                    false,
                    mFBYn,
                    mStoreName,
                    mStoreAddr,
                    mStoreNumber,
                    mStorePhone,
                    mStoreOwner
                )
            }
        }
    }

    fun CatCredit()
    {

        /* 로그기록. 신용거래요청 */
        val mLog =
            "TrdType : " + mTrdType + "," + "단말기ID : " + mTermID + "," + "원거래일자 : " + mAuDate + "," + "원승인번호 : " + mAuNo + "," +
                    "거래금액 : " + mTrdAmt + "," + "세금 : " + mTaxAmt + "," + "봉사료 : " + mSvcAmt + "," + "비과세 : " + mTaxFreeAmt + "," +
                    "할부개월 : " + mMonth + "," + "거래구분 : " + mTrdCode + "," + "거래고유번호 : " + mTradeNo + "," +
                    "카드입력대기시간 : " + mCount + "," + "전자서명사융유무 : " + mDscYn + "," + "fallback사융유무 : " + mFBYn
        cout("RECV : KOCES_CREDIT_CAT", Utils.getDate("yyyyMMddHHmmss"), mLog)

        mCatSdk = CatPaymentSdk(this, Constants.CatPayType.Credit) {
                result: String?, Code: String, resultData: HashMap<String, String> ->
            if (Code == "SHOW") {
                if (result != null) {
                    ShowToastBox(result)
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                }
                else
                {
                    ShowToastBox("서버통신 오류")
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                }
            } else {
                /* 콜백으로 결과를 받아온다 */
                if (result != null) {
                    CallBackReciptResult(result, Code, resultData)
                }
                else {
                    CallBackReciptResult("", Code, resultData)
                }
            }
        }

        /** 아래 커스텀박스를 다른 것으로 바꾼다  */
        if (mTrdCode == "t" || mTrdCode == "T") {
            mTrdCode = "a"
        } else {
            mTrdCode = "0"
        }
        if (mAuDate != "") {
            mAuDate = mAuDate.substring(0, 6)
        }
        if (mAuNo != "") {
            mAuNo = mAuNo.replace(" ", "")
        }

        //만일 취소인 경우
        if (mTrdType == "A20") {
            if (mAuDate == "" || mAuNo == "") //만일 원승인번호 원승인일자가 없는경우
            {
                ShowToastBox(ERROR_NOAUDATE)
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

            //취소
            if (mTrdCode == "a") {
                if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
                    mTermID = ""
                }
                mCatSdk.PayCredit(
                    mTermID,
                    mTrdAmt.toString(),
                    mTaxAmt.toString(),
                    mSvcAmt.toString(),
                    mTaxFreeAmt.toString(),
                    mAuDate,
                    mAuNo,
                    mTradeNo,
                    mMonth.toString(),
                    true,
                    "",
                    "",
                    false,
                    mStoreName,
                    mStoreAddr,
                    mStoreNumber,
                    mStorePhone,
                    mStoreOwner
                )
            } else {
                if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
                    mTermID = ""
                }
                mCatSdk.PayCredit(
                    mTermID,
                    mTrdAmt.toString(),
                    mTaxAmt.toString(),
                    mSvcAmt.toString(),
                    mTaxFreeAmt.toString(),
                    mAuDate,
                    mAuNo,
                    "",
                    mMonth.toString(),
                    true,
                    "",
                    "",
                    false,
                    mStoreName,
                    mStoreAddr,
                    mStoreNumber,
                    mStorePhone,
                    mStoreOwner
                )
            }
            return
        }

        //신용
        if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
            mTermID = ""
        }
        mCatSdk.PayCredit(
            mTermID, mTrdAmt.toString(), mTaxAmt.toString(), mSvcAmt.toString(), mTaxFreeAmt.toString(), "", "", "",
            mMonth.toString(), false, "", "", false, mStoreName,
            mStoreAddr,
            mStoreNumber,
            mStorePhone,
            mStoreOwner
        )
    }

    private fun LineCredit()
    {
        if (mTrdCode == "T" && mTradeNo != "") {
        } else {
            if (mKocesSdk.getUsbDevice().size > 0 && mKocesSdk.getICReaderAddr() != "") {
                if (mKocesSdk.CheckConnectedUsbSerialState(mKocesSdk.getICReaderAddr())) {
                } else {
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
            } else if (mKocesSdk.getUsbDevice().size > 0 && mKocesSdk.getMultiReaderAddr() != "") {
                if (mKocesSdk.CheckConnectedUsbSerialState(mKocesSdk.getMultiReaderAddr())) {
                } else {
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return

                }
            } else {
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

        }

        /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
        mPaySdk = PaymentSdk(1, false) { result: String?, Code: String, resultData: HashMap<String, String> ->
            if (Code == "SHOW") {
                if (result != null) {
                    ShowToastBox(result)
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                }
                else
                {
                    ShowToastBox("서버통신 오류")
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                }
            } else {
                /* 콜백으로 결과를 받아온다 */
                if (result != null) {
                    CallBackReciptResult(result, Code, resultData)
                }
                else {
                    CallBackReciptResult("", Code, resultData)
                }
            }
        }

        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (mTaxFreeAmt == null) {
            mTaxFreeAmt = 0
        }
        if (mTrdAmt == null) {
            mTrdAmt = 0
        }
//        mTrdAmt += mTaxFreeAmt


        var CanRes = ""
        if (mTrdCode == "t" || mTrdCode == "T") {
            mTrdCode = "a"
        } else {
            mTrdCode = "0"
        }
        CanRes = mTrdCode + mAuDate + mAuNo

        /* 로그기록. 신용거래요청 */

        /* 로그기록. 신용거래요청 */
        val mLog =
            "TrdType : " + mTrdType + "," + "단말기ID : " + mTermID + "," + "원거래일자 : " + mAuDate + "," + "원승인번호 : " + mAuNo + "," +
                    "거래금액 : " + mTrdAmt + "," + "세금 : " + mTaxAmt + "," + "봉사료 : " + mSvcAmt + "," + "비과세 : " + mTaxFreeAmt + "," +
                    "할부개월 : " + mMonth + "," + "거래구분 : " + mTrdCode + "," + "거래고유번호 : " + mTradeNo + "," +
                    "카드입력대기시간 : " + mCount + "," + "전자서명사융유무 : " + mDscYn + "," + "fallback사융유무 : " + mFBYn
        cout("RECV : KOCES_CREDIT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)




        //만일 거래고유키취소사용시 코세스거래고유번호사용
        if (mTrdCode == "a" && mTrdType == "A20") {
            if (mAuDate == "" || mAuNo == "") //만일 원승인번호 원승인일자가 없는경우
            {
                ShowToastBox(ERROR_NOAUDATE)
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

            Setting.g_sDigSignInfo = "4" //거래고유키 취소일 때 설정한다
            /* 거래고유키 취소시 앱투앱엑티비티에서 처리한다 */
            mKocesSdk.___ictrade(TCPCommand.CMD_ICTRADE_CANCEL_REQ,
                mTermID,
                Utils.getDate("yyMMddHHmmss"),
                Constants.TEST_SOREWAREVERSION,
                "",
                CanRes,
                "K",
                "",
                "",
                null,
                 mTrdAmt.toString(),
                mTaxAmt.toString(),
                mSvcAmt.toString(),
                mTaxFreeAmt.toString(),
                "",
                mMonth.toString(),
                "",
                "",
                " ",
                "",
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                null,
                Setting.g_sDigSignInfo,
                "",
                "",
                null,
                "",
                "",
                mTradeNo,
                "",
                Utils.getMacAddress(this),
                Utils.getHardwareKey(this, false, mTermID),
                TcpInterface.DataListener { _rev ->
                    var _tmpEot = 0
                    while (true) //TCP EOT 수신 될때 까지 기다리기 위해서
                    {
                        if (Setting.getEOTResult() != 0) {
                            break
                        }
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                        _tmpEot++
                        if (_tmpEot >= 30) {
                            break
                        }
                    }
                    val _b = KByteArray(_rev)
                    _b.CutToSize(10)
                    val _responsecode = String(_b.CutToSize(3))
                    val tp = CCTcpPacket(_rev)
                    val code = String(tp.resData[0])
                    val authorizationnumber =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
                    val kocesTradeUniqueNumber =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                            tp.resData[3]
                        ) //koces거래고유번호
                    var tmpCardNum1 = String(tp.resData[4])
                    val tmpCardNum2 = tmpCardNum1.split("-").toTypedArray()
                    tmpCardNum1 = ""
                    for (n in tmpCardNum2) {
                        tmpCardNum1 += n
                    }

                    /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리  */
                    val _CardNum = tmpCardNum1.toByteArray()
                    var CardNo =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum)
                    val sb = StringBuffer()
                    sb.append(tmpCardNum1)
                    if (_CardNum.size > 12) {
                        sb.replace(8, 12, "****")
                    }
                    if (tmpCardNum1.indexOf("=") > 0) {
                        sb.replace(tmpCardNum1.indexOf("="), tmpCardNum1.indexOf("=") + 1, "*")
                    }

                    CardNo = sb.toString()

//                    val _CardNum = tmpCardNum1.toByteArray()
//                    if (_CardNum != null) {
//                        //System.arraycopy( data.get(4),0,_CardNum,0,6);
//                        for (i in 8 until _CardNum.size) {
//                            //* = 0x2A
//                            _CardNum[i] = 0x2A.toByte()
//                        }
//                        //카드번호가 올라오지만 그중 8개만 사용하고 나머지는 *로 바꾼다
//                    }
//                    val CardNo =
//                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum) //출력용카드번호

                    //카드 정보 삭제____________________________________________________________________________________
                    val rand = Random()
                    for (i in _CardNum!!.indices) {
                        //* = 0x2A
                        _CardNum!![i] = rand.nextInt(255).toByte()
                    }
                    tp.resData[4] = _CardNum
                    Arrays.fill(_CardNum, 0x01.toByte())
                    tp.resData[4] = _CardNum
                    Arrays.fill(_CardNum, 0x00.toByte())
                    tp.resData[4] = _CardNum
                    val card_type =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6]) //카드종류
                    val issuer_code =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7]) //발급사코드
                    var issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                        tp.resData[8],
                        "EUC-KR"
                    ) //발급사명
                    val issuer_names = issuer_name.split(" ").toTypedArray()
                    issuer_name = ""
                    for (n in issuer_names) {
                        issuer_name += n
                    }
                    val purchaser_code =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9]) //매입사코드
                    var purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                        tp.resData[10],
                        "EUC-KR"
                    ) //매입사명
                    val purchaser_names = purchaser_name.split(" ").toTypedArray()
                    purchaser_name = ""
                    for (n in purchaser_names) {
                        purchaser_name += n
                    }
                    val ddc_status =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11]) //DDC 여부
                    val edc_status =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12]) //EDC 여부
                    val giftcard_money =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[15]) //기프트카드 잔액
                    val merchant_number =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[16]) //가맹점번호
                    val encryp_key_expiration_date =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                            tp.resData[25]
                        ) //암호키만료잔여일
                    val merchant_data =
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[26]) //가맹점데이터
                    val sendData = HashMap<String, String>()
                    sendData["TrdType"] = tp.responseCode
                    sendData["TermID"] = tp.terminalID
                    sendData["TrdDate"] = tp.date
                    sendData["AnsCode"] = code
                    sendData["AuNo"] = authorizationnumber
                    sendData["TradeNo"] = kocesTradeUniqueNumber
                    if (CardNo.length > 8) {
                        sendData["CardNo"] = CardNo.substring(0, 8)
                    } else {
                        sendData["CardNo"] = CardNo
                    }
                    sendData["Keydate"] = encryp_key_expiration_date
                    sendData["MchData"] = merchant_data
                    sendData["CardKind"] =
                        card_type //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                    sendData["OrdCd"] = issuer_code //발급사코드
                    sendData["InpCd"] = purchaser_code //매입사코드
                    sendData["OrdNm"] = issuer_name //발급사명
                    sendData["InpNm"] = purchaser_name //매입사명
                    sendData["DDCYn"] = ddc_status //DDC 여부
                    sendData["EDCYn"] = edc_status //EDC 여부
                    sendData["GiftAmt"] = giftcard_money //기프트카드 잔액
                    sendData["MchNo"] = merchant_number //가맹점번호
                    try {
                        sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                    } catch (ex: UnsupportedEncodingException) {
                    }
                    /* 로그기록 거래고유키취소 결과 */
                    val mLog =
                        "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + encryp_key_expiration_date + "," +
                                "가맹점데이터 : " + merchant_data + "," + "카드종류 : " + card_type + "," + "발급사코드 : " + issuer_code + "," + "발급사명 : " + issuer_name + "," +
                                "메입사코드 : " + purchaser_code + "," + "메입사명 : " + purchaser_name + "," + "DDC여부 : " + ddc_status + "," + "EDC여부 : " + edc_status + "," +
                                "기프트잔액 : " + giftcard_money + "," + "가맹점번호 : " + merchant_number
                    cout("SEND : KOCES_CREDIT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
                    if (code == "0000")
                    {
                        if (card_type == "3" || card_type == "4")
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(
                                mTermID,
                                mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                                sqliteDbSdk.TradeMethod.Credit,
                                sqliteDbSdk.TradeMethod.Cancel,
                                mTrdAmt,
                                Utils.getByteToString_euc_kr(tp.resData[1]) ,
                                mTaxAmt,
                                mSvcAmt,
                                mTaxFreeAmt,
                                mMonth,
                                "",
                                "",
                                "",
                                CardNo,
                                card_type,
                                purchaser_name,
                                issuer_name,
                                merchant_number,
                                tp.date,
                                mAuDate,
                                authorizationnumber,
                                mAuNo,
                                kocesTradeUniqueNumber,
                                Utils.getByteToString_euc_kr(tp.resData[1]),
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        }
                        else
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(
                                mTermID,
                                mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                                sqliteDbSdk.TradeMethod.Credit,
                                sqliteDbSdk.TradeMethod.Cancel,
                                mTrdAmt,
                                giftcard_money ,
                                mTaxAmt,
                                mSvcAmt,
                                mTaxFreeAmt,
                                mMonth,
                                "",
                                "",
                                "",
                                CardNo,
                                card_type,
                                purchaser_name,
                                issuer_name,
                                merchant_number,
                                tp.date,
                                mAuDate,
                                authorizationnumber,
                                mAuNo,
                                kocesTradeUniqueNumber,
                                Utils.getByteToString_euc_kr(tp.resData[1]),
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        }

                    }
                    SendreturnData(RETURN_OK, sendData, "")

                })
        } else {
            if (mTrdType == "A10") {
                /* 신용요청인경우_A10 */
                mPaySdk.CreditIC(
                    this,
                    mTermID,
                    mTrdAmt.toString(),
                    mTaxAmt,
                    mSvcAmt,
                    mTaxFreeAmt,
                    mMonth.toString(),
                    "",
                    "",
                    "",
                    mTradeNo,
                    "",
                    mFBYn,
                    mStoreName,
                    mStoreAddr,
                    mStoreNumber,
                    mStorePhone,
                    mStoreOwner
                )
            } else {
                if (mAuDate == "" || mAuNo == "") //만일 원승인번호 원승인일자가 없는경우
                {
                    ShowToastBox(ERROR_NOAUDATE)
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
                /* 신용취소인경우_A20 */
                mPaySdk.CreditIC(
                    this,
                    mTermID,
                    mTrdAmt.toString(),
                    mTaxAmt,
                    mSvcAmt,
                    mTaxFreeAmt,
                    mMonth.toString(),
                    mAuDate,
                    CanRes,
                    "",
                    mTradeNo,
                    "",
                    mFBYn,
                    mStoreName,
                    mStoreAddr,
                    mStoreNumber,
                    mStorePhone,
                    mStoreOwner
                )
            }
        }
    }

    @Synchronized
    private fun CallBackReciptResult(result: String, Code: String, resultData: HashMap<String, String>)
    {
        if (Code == "COMPLETE_IC") /* 신용거래정상콜백 */ {
            val sendData = HashMap<String, String?>()
            sendData["TrdType"] = resultData["TrdType"]
            sendData["TermID"] = resultData["TermID"]
            sendData["TrdDate"] = resultData["TrdDate"]
            sendData["AnsCode"] = resultData["AnsCode"]
            sendData["Message"] = resultData["Message"]
            sendData["AuNo"] = resultData["AuNo"]
            sendData["TradeNo"] = resultData["TradeNo"]
            var OriginalMakingCardNumber = resultData["CardNo"]
            var DesCardNumber = OriginalMakingCardNumber
            if (OriginalMakingCardNumber!!.length > 8) {
                DesCardNumber = OriginalMakingCardNumber.substring(0, 8)
                val length = OriginalMakingCardNumber.length
                for (i in 0 until length - 8) {
                    DesCardNumber += "*"
                }
                sendData["CardNo"] = DesCardNumber
            } else {
                sendData["CardNo"] = DesCardNumber
            }
            OriginalMakingCardNumber = ""
            resultData["CardNo"] = ""
            sendData["Keydate"] = resultData["Keydate"]
            sendData["MchData"] = resultData["MchData"]
            sendData["CardKind"] =
                resultData["CardKind"] //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
            sendData["OrdCd"] = resultData["OrdCd"] //발급사코드
            val issuer_names = resultData["OrdNm"]!!.split(" ").toTypedArray()
            var issuer_name: String? = ""
            for (n in issuer_names) {
                issuer_name += n
            }
            sendData["OrdNm"] = issuer_name //발급사명
            sendData["InpCd"] = resultData["InpCd"] //매입사코드
            val purchaser_names = resultData["InpNm"]!!.split(" ").toTypedArray()
            var purchaser_name: String? = ""
            for (n in purchaser_names) {
                purchaser_name += n
            }
            sendData["InpNm"] = purchaser_name //매입사명
            sendData["DDCYn"] = resultData["DDCYn"] //DDC 여부
            sendData["EDCYn"] = resultData["EDCYn"] //EDC 여부
            sendData["GiftAmt"] = resultData["GiftAmt"] //기프트카드 잔액
            sendData["MchNo"] = resultData["MchNo"] //가맹점번호
            val mLog =
                "거래일자 : " + resultData["TrdDate"] + "," + "응답코드 : " + resultData["AnsCode"] + "," + "응답메세지 : " + resultData["Message"] + "," +
                        "승인번호 : " + resultData["AuNo"] + "," + "거래고유번호 : " + resultData["TradeNo"] + "," + "출력용카드번호 : " + DesCardNumber + "," +
                        "암호키만료잔여일 : " + resultData["Keydate"] + "," + "가맹점데이터 : " + resultData["MchData"] + "," + "카드종류 : " + resultData["CardKind"] + "," +
                        "발급사코드 : " + resultData["OrdCd"] + "," + "발급사명 : " + resultData["OrdNm"] + "," + "메입사코드 : " + resultData["InpCd"] + "," +
                        "메입사명 : " + resultData["InpNm"] + "," + "DDC여부 : " + resultData["DDCYn"] + "," + "EDC여부 : " + resultData["EDCYn"] + "," +
                        "기프트잔액 : " + resultData["GiftAmt"] + "," + "가맹점번호 : " + resultData["MchNo"]
            cout("SEND:CREDIT", Utils.getDate("yyyyMMddHHmmss"), mLog)
            SendreturnData(RETURN_OK, sendData, "")
        } else {
            SendreturnData(RETURN_CANCEL, null, result)
        }

    }

    fun SendreturnData(_state: Int, _hashMap: HashMap<*, *>?, _Message: String)
    {
        mEotCancel = 0
        Setting.setDscyn("1")
        Setting.setDscData("")
        Setting.setEasyCheck(false)

        if (_state == 0) {
            if (_hashMap != null) {
                if (_hashMap.get("AnsCode") != "0000") {
                    ShowToastBox(_hashMap.get("Message") as String)
                    cancelTimer()
                    if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("Month", mMonth)    //할부
                    }
                    else
                    {
                        intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
            }
            else
            {
                ShowToastBox(_Message)
                cancelTimer()
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }
            //여기서 영수증 화면으로 보낸다.
            if (_hashMap.get("AnsCode") == "0000")
            {
                intent = Intent(this@CreditLoadingActivity, ReceiptCreditActivity::class.java)
                intent.putExtra("TermID", _hashMap.get("TermID") as String)
                intent.putExtra("data", "last")
                cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
                startActivity(intent)
            }
            else
            {
                ShowToastBox(_hashMap.get("Message") as String)
                cancelTimer()
                if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("Month", mMonth)    //할부
                }
                else
                {
                    intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

        } else if (_state == 1) {
            ShowToastBox(_Message);
            cancelTimer()
            if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("Month", mMonth)    //할부
            }
            else
            {
                intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
            }
            startActivity(intent)
        } else {
            ShowToastBox(_Message);
            cancelTimer()
            if (mTrdType == "A10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CreditLoadingActivity,CreditActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("Month", mMonth)    //할부
            }
            else
            {
                intent = Intent(this@CreditLoadingActivity,Main2Activity::class.java)
            }
            startActivity(intent)
        }


        when(Setting.g_PayDeviceType){
            PayDeviceType.NONE -> {
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

                return
            }
            PayDeviceType.BLE -> {

                return
            }
            PayDeviceType.CAT -> {

                return
            }
            PayDeviceType.LINES ->{
                Handler(Looper.getMainLooper()).postDelayed({ mKocesSdk.DeviceReset() /* 장치전체초기화 */ }, 500)
            }
            else->{ //else 경우는 없음

                return
            }
        }

        return
    }

    fun ShowToastBox(_str: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, _str, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 로그파일에 보낼 데이터(메시지)들을 정렬하여 보낸다
     * @param _title
     * @param _time
     * @param _Contents
     */
    private fun cout(_title: String?, _time: String?, _Contents: String?) {
        if (_title != null && _title != "") {
            WriteLogFile("\n")
            WriteLogFile("<$_title>\n")
        }
        if (_time != null && _time != "") {
            WriteLogFile("[$_time]  ")
        }
        if (_Contents != null && _Contents != "") {
            WriteLogFile(_Contents)
            WriteLogFile("\n")
        }
    }

    fun cancelTimer(){
        mEotCancel = 0
        Setting.setDscyn("1")
        Setting.setDscData("")
        Setting.setEasyCheck(false)

        try {
            mCountTimer?.cancel()
        }
        catch ( e : UninitializedPropertyAccessException )
        {
            print(e)
        }
        try {
            mTvwTimer.text = ""
        }
        catch ( e : Exception )
        {
            print(e)
        }
    }

    fun getTID(): String {
        return Setting.getPreference(this, Constants.TID)
    }

    //사인데이터를 받아오는 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SIGNPAD) {
            mCountSign = false
            when (Setting.g_PayDeviceType) {
                PayDeviceType.NONE -> {}
                PayDeviceType.BLE -> {
                    if (requestCode == REQUEST_SIGNPAD) {
                        if (resultCode == RESULT_OK && data!!.getStringExtra("signresult") == "OK") {
                            //만일 서명패드가 없이 터치서명 또는 서명없음일 경우 _img 파일을 저장된 파일로 대체 한다. 저장된 파일이 없다면 이미지없음으로 처리한다.
                            if (Setting.getDscyn() == "0") {
                                val bytes = data.getByteArrayExtra("signdata")

                                mBleSdk.Req_tcp_Credit(
                                    mTermID,
                                    bytes,
                                    "",
                                    "",
                                    ""
                                ) /* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                            } else {
                                val tmpSign = data.getByteArrayExtra("signdata")
                                val CodeNversion = data.getStringExtra("codeNver")
                                mBleSdk.Req_tcp_Credit(
                                    mTermID,
                                    tmpSign,
                                    "",
                                    "",
                                    ""
                                ) /* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                            }
                        } else if (resultCode == RESULT_CANCELED && data!!.getStringExtra("signresult") == "CANCEL") {
                            //사인처리가 취소됨. 모든 정보 클리어 처리
                            ReadyDialogHide()
                            mBleSdk.DeviceReset()
                            SendreturnData(RETURN_CANCEL, null, "서명 입력이 취소되었습니다")
                        }
                    }
                    return
                }
                PayDeviceType.CAT -> {}
                PayDeviceType.LINES -> {}
                else -> {}
            }

            if (resultCode == RESULT_OK && data!!.getStringExtra("signresult") == "OK") {
                //만일 서명패드가 없이 터치서명 또는 서명없음일 경우 _img 파일을 저장된 파일로 대체 한다. 저장된 파일이 없다면 이미지없음으로 처리한다.
                if (Setting.getSignPadType(mKocesSdk.activity) == 0 || Setting.getSignPadType(mKocesSdk.activity) == 3) {
                    val bytes = data.getByteArrayExtra("signdata")

                    mPaySdk.Req_tcp_Credit(
                        mTermID,
                        bytes,
                        "",
                        "",
                        ""
                    ) /* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                } else {
                    val tmpSign = data.getByteArrayExtra("signdata")
                    val CodeNversion = data.getStringExtra("codeNver")
                    mPaySdk.Req_tcp_Credit(
                        mTermID,
                        tmpSign,
                        "",
                        "",
                        ""
                    ) /* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                }
            } else if (resultCode == RESULT_CANCELED && data!!.getStringExtra("signresult") == "CANCEL") {
                //사인처리가 취소됨. 모든 정보 클리어 처리
                ReadyDialogHide()
                mPaySdk.DeviceReset()
                SendreturnData(RETURN_CANCEL, null, "서명 입력이 취소되었습니다")
            }
        }
    }

}