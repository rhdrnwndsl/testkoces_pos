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
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.van.Constants
import java.util.*
import kotlin.concurrent.timer

class CashICLoadingActivity: BaseActivity() {
    val TAG:String = CashICLoadingActivity::class.java.simpleName

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
    var mAuNo:String=""
    var mAuDate:String=""
    var mDirectTrade:String=""
    var mCardInfo:String = ""
    var mCancel:Boolean = false

    private val mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()
    lateinit var mCountTimer: Timer

    /** 사인패드 진행시에는 카운트다운을 하지 않는다. 그외의 경우 다시 카운트다운을 한다 */
    var mCountSign:Boolean = false

    lateinit var mBtnCancel: Button

    //sdk
    lateinit var mCatSdk: CatPaymentSdk

    //거래를 종료하고 다시 크레딧페이지로 이동할 때 보내줄 데이터
    var mEdtMoney:String=""
    var mEdtTxf:String=""
    var mEdtSvc:String=""

    var mPreviewActivity:String = ""    //이전에 넘어온 화면이어디인지체크

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
            if (mCatSdk != null)
            {
                mCatSdk.Cat_SendCancelCommandE(false)
            }
            mKocesSdk.mTcpClient.DisConnectCatServer()
            cancelTimer()

            if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CashICLoadingActivity,CashICActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
            }
            else
            {
                intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
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
//            if (mTermID == "" )
//            {
//                mTermID = getTID()
//            }
            mStoreName = intent.getStringExtra("StoreName").toString() ?:""
            mStoreAddr = intent.getStringExtra("StoreAddr").toString() ?:""
            mStoreNumber = intent.getStringExtra("StoreNumber").toString() ?:""
            mStorePhone = intent.getStringExtra("StorePhone").toString() ?:""
            mStoreOwner = intent.getStringExtra("StoreOwner").toString() ?:""
            mPreviewActivity = intent.getStringExtra("PreviewActivity").toString()
            mTrdAmt = intent.getIntExtra("TrdAmt",0)
            mTaxAmt = intent.getIntExtra("TaxAmt",0)
            mSvcAmt = intent.getIntExtra("SvcAmt",0)
            mTaxFreeAmt = intent.getIntExtra("TaxFreeAmt",0)
            mAuNo = intent.getStringExtra("AuNo").toString() ?: ""
            mAuDate = intent.getStringExtra("AuDate").toString() ?: ""
            mDirectTrade = intent.getStringExtra("DirectTrade").toString() ?: ""
            mCardInfo = intent.getStringExtra("CardInfo").toString() ?: ""
            mCancel = intent.getBooleanExtra("Cancel",false)

            mEdtMoney = intent.getStringExtra("EdtMoney").toString() ?: ""
            mEdtSvc = intent.getStringExtra("EdtSvc").toString() ?: ""
            mEdtTxf = intent.getStringExtra("EdtTxf").toString() ?: ""

            when(Setting.g_PayDeviceType){
                Setting.PayDeviceType.NONE -> {
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity,CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    }
                    else
                    {
                        intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                Setting.PayDeviceType.BLE -> {
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity,CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    }
                    else
                    {
                        intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                Setting.PayDeviceType.CAT -> {
                    Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this, Constants.CAT_TIME_OUT)))
                    Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this, Constants.CAT_TIME_OUT)))
                    mCount = Integer.valueOf(Setting.getPreference(this, Constants.CAT_TIME_OUT))
                    mTvwMessage.text = "거래 승인 중입니다"
                }
                Setting.PayDeviceType.LINES ->{
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity,CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    }
                    else
                    {
                        intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                else->{ //else 경우는 없음
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity,CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    }
                    else
                    {
                        intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
            }

            Log.d("CashICLoadingActivity",mKocesSdk.activity.toString() )
            Log.d("CashICLoadingActivity",Setting.getTopContext().toString() )
            ShowStartTimer()
        }
        else
        {
            //TrdType 이 없는경우. 오류다. 메인으로 보낸다
            intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
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
                        mKocesSdk.mTcpClient.DisConnectCatServer()
                        if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashICLoadingActivity,CashICActivity::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        }
                        else
                        {
                            intent = Intent(this@CashICLoadingActivity,Main2Activity::class.java)
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

        CashIC()
    }

    fun CashIC() {
        /* 로그기록. 신용거래요청 */
        val mLog =
            "TrdType : " + mTrdType + "," + "단말기ID : " + mTermID + "," + "원거래일자 : " + mAuDate + "," + "원승인번호 : " + mAuNo + "," +
                    "거래금액 : " + mTrdAmt + "," + "세금 : " + mTaxAmt + "," + "봉사료 : " + mSvcAmt + "," + "비과세 : " + mTaxFreeAmt + "," +
                    "간소화거래여부 : " + mDirectTrade + "," + "카드정보수록여부 : " + mCardInfo + "," +
                    "취소 : " + mCancel
        cout("RECV : KOCES_CASH_IC_CAT", Utils.getDate("yyyyMMddHHmmss"), mLog)

        mCatSdk = CatPaymentSdk(
            this,
            Constants.CatPayType.CashIC
        ) { result: String?, Code: String, resultData: HashMap<String, String> ->
            if (Code == "SHOW") {
                if (result != null) {
                    ShowToastBox(result)
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    } else {
                        intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                } else {
                    ShowToastBox("서버통신 오류")
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    } else {
                        intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                }
            } else {
                /* 콜백으로 결과를 받아온다 */
                if (result != null) {
                    CallBackReciptResult(result, Code, resultData)
                } else {
                    CallBackReciptResult("", Code, resultData)
                }
            }
        }

        /** 아래 커스텀박스를 다른 것으로 바꾼다  */
        if (mAuDate != "") {
            mAuDate = mAuDate.replace(" ", "")
        }
        if (mAuNo != "") {
            mAuNo = mAuNo.replace(" ", "")
        }

        var _class: Constants.CashICBusinessClassification = when (mTrdType) {      //부가세 자동인 경우
            "C10" -> Constants.CashICBusinessClassification.Buy
            "C20" -> Constants.CashICBusinessClassification.Cancel
            "C30" -> Constants.CashICBusinessClassification.Search
            "C40" -> Constants.CashICBusinessClassification.BuySearch
            "C50" -> Constants.CashICBusinessClassification.CancelSearch
            else -> {
                Constants.CashICBusinessClassification.Buy
            }
        }

        //만일 취소인 경우
        if (mTrdType == Constants.CashICBusinessClassification.Cancel.toString() ||
            mTrdType == Constants.CashICBusinessClassification.CancelSearch.toString())
            {
                if (mAuDate == "" || mAuNo == "") //만일 원승인번호 원승인일자가 없는경우
                {
                    ShowToastBox(ERROR_NOAUDATE)
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    } else {
                        intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
        }

        if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
            mTermID = ""
        }
        mCatSdk.CashIC(
            _class,
            mTermID,
            mTrdAmt.toString(),
            mTaxAmt.toString(),
            mSvcAmt.toString(),
            mTaxFreeAmt.toString(),
            mAuDate.replace(" ", ""),
            mAuNo.replace(" ", ""),
            mDirectTrade,
            mCardInfo,
            mCancel,
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

    @Synchronized
    private fun CallBackReciptResult(result: String, Code: String, resultData: HashMap<String, String>)
    {
        if (Code == "COMPLETE") /* 신용거래정상콜백 */ {
            val sendData = HashMap<String, String?>()
            sendData["TrdType"] = resultData["TrdType"]
            sendData["TermID"] = resultData["TermID"]
            sendData["TrdDate"] = resultData["TrdDate"]
            sendData["AnsCode"] = resultData["AnsCode"]
            sendData["Message"] = resultData["Message"]
            sendData["ResponseNo"] = resultData["ResponseNo"]

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
            sendData["MchData"] = resultData["MchData"]
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
            sendData["ServeMoney"] = resultData["ServeMoney"] //잔액 -> 데이터가 이상해서 뒤에서부터 잘라서 총 12자리만 실어보냄
            sendData["MchNo"] = resultData["MchNo"] //가맹점번호
            val mLog =
                "거래일자 : " + resultData["TrdDate"] + "," + "응답코드 : " + resultData["AnsCode"] + "," + "응답메세지 : " + resultData["Message"] + "," +
                        "승인번호 : " + resultData["AuNo"] + "," + "거래고유번호 : " + resultData["TradeNo"] + "," + "출력용카드번호 : " + DesCardNumber + "," +
                        "가맹점데이터 : " + resultData["MchData"] + "," + "발급사코드 : " + resultData["OrdCd"] + "," +
                        "발급사명 : " + resultData["OrdNm"] + "," + "메입사코드 : " + resultData["InpCd"] + "," +
                        "메입사명 : " + resultData["InpNm"] + "," + "잔액 : " + resultData["ServeMoney"] + "," + "가맹점번호 : " + resultData["MchNo"]
            cout("SEND:CASH_IC", Utils.getDate("yyyyMMddHHmmss"), mLog)
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
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    } else {
                        intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
            }
            else
            {
                ShowToastBox(_Message)
                cancelTimer()
                if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                } else {
                    intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }
            //여기서 영수증 화면으로 보낸다.
            if (_hashMap.get("AnsCode") == "0000")
            {
                if (_hashMap.get("ResponseNo") == "C15" ||
                    _hashMap.get("ResponseNo") == "C25")    //구매와 취소면 영수증화면으로 아니면 돌아간다
                {
                    intent = Intent(this@CashICLoadingActivity, ReceiptCashActivity::class.java)
                    intent.putExtra("TermID", _hashMap.get("TermID") as String)
                    intent.putExtra("data", "last")
                    cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
                    startActivity(intent)
                    return
                }
                else if(_hashMap.get("ResponseNo") == "C35")
                {
//                    ShowToastBox("잔액 : " + _hashMap.get("ServeMoney") as String)
                    cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("ServeMoney", _hashMap.get("ServeMoney") as String) //잔액
                    } else {
                        intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
                else
                {
                    ShowToastBox(_hashMap.get("Message") as String)
                    cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
                    if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    } else {
                        intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }

            }
            else
            {
                ShowToastBox(_hashMap.get("Message") as String)
                cancelTimer()
                ShowToastBox(_Message)
                if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                } else {
                    intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

        } else if (_state == 1) {
            ShowToastBox(_Message);
            cancelTimer()
            if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
            } else {
                intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
            }
            startActivity(intent)
        } else {
            ShowToastBox(_Message);
            cancelTimer()
            if (mPreviewActivity == "CashICActivity")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CashICLoadingActivity, CashICActivity::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
            } else {
                intent = Intent(this@CashICLoadingActivity, Main2Activity::class.java)
            }
            startActivity(intent)
        }
        when(Setting.g_PayDeviceType){
            Setting.PayDeviceType.NONE -> {
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

                return
            }
            Setting.PayDeviceType.BLE -> {

                return
            }
            Setting.PayDeviceType.CAT -> {

                return
            }
            Setting.PayDeviceType.LINES ->{
                Handler(Looper.getMainLooper()).postDelayed({ mKocesSdk.DeviceReset() /* 장치전체초기화 */ }, 500)
            }
            else->{ //else 경우는 없음

                return
            }
        }
        return
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

    fun ShowToastBox(_str: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, _str, Toast.LENGTH_SHORT).show()
        }
    }
}