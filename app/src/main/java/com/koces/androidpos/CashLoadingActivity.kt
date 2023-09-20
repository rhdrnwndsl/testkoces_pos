package com.koces.androidpos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.Setting.PayDeviceType
import com.koces.androidpos.sdk.Utils.CCTcpPacket
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.db.sqliteDbSdk.TradeMethod
import com.koces.androidpos.sdk.van.Constants
import com.koces.androidpos.sdk.van.TcpInterface
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.concurrent.timer

class CashLoadingActivity : BaseActivity() {
    val TAG:String = CashLoadingActivity::class.java.simpleName
    lateinit var imm: InputMethodManager
    lateinit var mTvwTimer:TextView
    lateinit var mTvwMessage:TextView
    lateinit var mEdtAuthNumber:EditText
    private val mMaxinumTime:Long = 1000
    var mCount:Int = 30
    var mTrdType:String =""
    var mTermID:String = ""
    var mStoreName:String = ""
    var mStoreAddr:String = ""
    var mStoreNumber:String = ""
    var mStorePhone:String = ""
    var mStoreOwner:String = ""
    var mTrdCode:String =""
    var mCashTarget:Int = 0
    var mCashAuthNum:String = ""
    var mMoney:Int = 0
    var mSvc:Int = 0
    var mVat:Int = 0
    var mTxf:Int = 0
    var mKeyYn:String = ""
    var mCancel:Int = -1
    var mCancelReason:String =""
    var moriAuthNo:String=""
    var moriAuthDate:String=""
    var mTradeNo:String=""      //고유거래키 취소 번호
    var mTradeNoOption:Boolean = false  //고유거래키 취소 유무
    var mCashInputMethod:String = ""    //MSR 인지 번호입력인지
    private val mKocesSdk:KocesPosSdk = KocesPosSdk.getInstance()
    lateinit var mCountTimer:Timer

    /** 사인패드 진행시에는 카운트다운을 하지 않는다. 그외의 경우 다시 카운트다운을 한다 */
    var mCountSign:Boolean = false

    lateinit var mLinearInputBox:LinearLayout
    lateinit var mBtnCancel:Button
    lateinit var mBtnOk:Button
    lateinit var mLinearExitBox:LinearLayout
    lateinit var mBtnExit:Button    //위의 cancel버튼은 번호입력란의 버튼. 이것은 번호입력란이 없어졌을 때 나오는 나가기버튼

    //sdk
    lateinit var mPaySdk:PaymentSdk
    lateinit var mBleSdk:BlePaymentSdk
    lateinit var mCatSdk: CatPaymentSdk

    val mProcMsg1:String = "서버로 처리 요청"
    /** EOT취소를 위한 변수  */
    var mEotCancel = 0


    /** 앱투앱에 해쉬맵을 보낼때 정상메세지일 경우 처리하는 키  */
    private val RETURN_OK = 0
    /** 앱투앱에 해쉬맵을 보낼때 정상오류메세지일 경우 처리하는 키  */
    private val RETURN_CANCEL = 1

    /** 앱투앱_취소메세지_원승인번호 혹은 원승인일자가 없을경우  */
    private val ERROR_NOAUDATE = "원승인번호 원승인일자가 없습니다."
    private val ERROR_NOTRADENO = "거래고유번호가 없습니다."

    /** 앱투앱 비엘이 장치에서 현금 거래영수증 번호 없어 , 키인으로 요청 한 경우에 에려 메세지 211230 kim.jy  */
    private val ERROR_NOBLEKEYIN = "고객번호가 없습니다. 고객번호를 포함하여 거래하여 주십시오."

    /** 앱투앱_취소메세지_취소 구분자가 없습니다  */
    private val ERROR_EMPTY_CANCEL_REASON = "취소 구분자가 없습니다."
    /** 앱투앱_취소메세지_고객번호가 없을시 현금영수증요청했는데 서명패드 미사용or터치사명일경우  */
    private val ERROR_NOSIGNPAD = "고객 번호가 없거나 장치가 연결 되어 있지 않습니다."

    //거래를 종료하고 다시 크레딧페이지로 이동할 때 보내줄 데이터
    var mEdtMoney:String=""
    var mEdtTxf:String=""
    var mEdtSvc:String=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_loading)
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

    fun initRes(){
        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this)
        mKocesSdk.BleregisterReceiver(this)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

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
        //값을 받아와서 처리 한다.
        if(intent.hasExtra("TrdType")) {
            mTrdType = intent.getStringExtra("TrdType").toString()                      //B10 B20
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
            mCashTarget = intent.getIntExtra("CashTarget", 0)                   //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
            mCashAuthNum = intent.getStringExtra("Auth").toString()  ?:""                        //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
            mMoney = intent.getIntExtra("Money",0)                              //거래금액 승인:공급가액, 취소:원승인거래총액
            mVat = intent.getIntExtra("VAT",0)                                  //세금
            mSvc = intent.getIntExtra("SVC",0)                                  //봉사료
            mTxf = intent.getIntExtra("TXF",0)                                  //비과세
            moriAuthNo = intent.getStringExtra("OriAuNo").toString() ?: ""                  //원승인번호
            moriAuthDate = intent.getStringExtra("OriAuDate").toString() ?: ""              //원거래일자 YYMMDD
            mCancelReason = intent.getStringExtra("CancelReason").toString() ?:""           //취소사유는 1=거래취소
            if(!Utils.isNullOrEmpty(intent.getStringExtra("TradeNo").toString())){
                mTradeNo = intent.getStringExtra("TradeNo").toString()                      //코세스거래고유번호
            }else{
                mTradeNo = ""
            }
            mKeyYn = intent.getStringExtra("KeyYn").toString() ?: ""                        //'K' : Keyin , 'S' : Swipe, 'I' : IC, 'R' : RF, 'M' : Mobile RF, 'E' : PayOn, 'F' : Mobile PayOn
            mCashInputMethod = intent.getStringExtra("CashInputMethod").toString() ?:""     //현금영수증발급형태 MSR or 번호입력인지 체크
            mTrdCode = intent.getStringExtra("TrdCode").toString() ?:""

            if (mCashTarget == 3){
                mCashInputMethod = TradeMethod.CashDirect
                mCashAuthNum = "0100001234"
                mKeyYn = "K"
            }

            if (mCashInputMethod == TradeMethod.CashMs)
            {
                mKeyYn = "S"
            }

            mEdtMoney = intent.getStringExtra("EdtMoney").toString() ?: ""
            mEdtSvc = intent.getStringExtra("EdtSvc").toString() ?: ""
            mEdtTxf = intent.getStringExtra("EdtTxf").toString() ?: ""

            mLinearInputBox = findViewById(R.id.load_cash_linear_inputnum)  //번호입력 리니어레이아웃
            mTvwTimer = findViewById(R.id.load_cash_timer)
            mTvwMessage = findViewById(R.id.load_cash_message)

            //번호 입력 취소의 경우 메인 화면으로 보낸다.
            mEdtAuthNumber = findViewById(R.id.load_cash_edt_number)
            //아래에 취소버튼이 또 있어서 이부분 주석처리 22-03-08 jiw
//            mBtnCancel = findViewById(R.id.load_cash_btn_cancel)    //입력 취소
//            mBtnCancel.setOnClickListener {
//                val intent = Intent(this@CashLoadingActivity, Main2Activity::class.java)    //main2로 이동
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//                startActivity(intent)
//            }

            mLinearExitBox = findViewById(R.id.load_cash_linear_cancel_exit)    //최하단 나가기레이아웃

            mBtnExit = findViewById(R.id.load_cash_btn_cancel_exit)     //위의 cancel버튼은 번호입력란의 버튼. 이것은 번호입력란이 없어졌을 때 나오는 나가기버튼
            mBtnExit.visibility = View.INVISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                if (!(mTvwMessage.text.toString()).contains("거래 승인")) {
                    mBtnExit.visibility = View.VISIBLE
                }

            },1000)
            mBtnExit.setOnClickListener {
                cancelTimer()
                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                    if(Utils.isNullOrEmpty(mEdtAuthNumber.text.toString()))
                    {
                        mKocesSdk.DeviceReset()
                    }

                } else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                    if(Utils.isNullOrEmpty(mEdtAuthNumber.text.toString()))
                    {
                        mKocesSdk.__BLEPosinit("99", null)
                        mKocesSdk.__BLEReadingCancel(null)
                    }
                } else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                    mKocesSdk.mTcpClient.DisConnectCatServer()
                    if (mCatSdk != null)
                    {
                        mCatSdk.Cat_SendCancelCommandE(false)
                    }

                }

                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
            }

            //취소 버튼을 누르면 메인 페이지로 이동 한다.
            mBtnCancel = findViewById(R.id.load_cash_btn_cancel)
            mBtnCancel.setOnClickListener {
                //여기서 ble/usb 종료커맨드를 날린다 220308.jiw
                cancelTimer()
                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                    if(Utils.isNullOrEmpty(mCashAuthNum))
                    {
                        mKocesSdk.DeviceReset()
                    }

                } else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                    if(Utils.isNullOrEmpty(mCashAuthNum))
                    {
                        mKocesSdk.__BLEPosinit("99", null)
                        mKocesSdk.__BLEReadingCancel(null)
                    }
                } else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                    if (mCatSdk != null)
                    {
                        mCatSdk.Cat_SendCancelCommandE(false)
                    }
                    mKocesSdk.mTcpClient.DisConnectCatServer()
                }

                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
            }

            mBtnOk = findViewById(R.id.load_cash_btn_ok)

            mBtnOk.setOnClickListener {
                //취소시에 EditView가 값이 있는지 없는지 검사 후에
                //mCashAuthNum에 값을 넣는다.

                if(Utils.isNullOrEmpty(mEdtAuthNumber.text.toString())){
                    ShowToastBox("빈 값을 사용 할 수 없습니다")
//                Toast.makeText(this@CashLoadingActivity,"빈 값을 사용 할 수 없습니다",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                imm.hideSoftInputFromWindow(mEdtAuthNumber.getWindowToken(), 0);
                mCashAuthNum = mEdtAuthNumber.text.toString()
                mLinearInputBox.visibility = View.GONE     //입력 박스 숨기기
                mTvwTimer.visibility = View.VISIBLE             //타이머 표시
                mLinearExitBox.visibility = View.VISIBLE    //최하단 나가기버튼
                ShowStartTimer()
            }

            if(mCashInputMethod == TradeMethod.CashDirect && mCashAuthNum == "")    //카드없이 결제를 하는데 번호가 입력되어있지 않다면 번호입력받을 준비를 한다
            {
                if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE)
                {
                    mLinearInputBox.visibility = View.VISIBLE
                    mTvwTimer.visibility = View.GONE
                    mLinearExitBox.visibility = View.GONE
                    return
                }
                else if(Setting.g_PayDeviceType == Setting.PayDeviceType.LINES)
                {
                    //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
                    if(Setting.getSignPadType(mKocesSdk.getActivity()) == 3 || Setting.getSignPadType(mKocesSdk.getActivity()) == 0)
                    {
                        mLinearInputBox.visibility = View.VISIBLE
                        mTvwTimer.visibility = View.GONE
                        mLinearExitBox.visibility = View.GONE
                        return
                    }

                }


            }

            mLinearInputBox.visibility = View.GONE
            mTvwTimer.visibility = View.VISIBLE
            mLinearExitBox.visibility = View.VISIBLE
            when(Setting.g_PayDeviceType){
                PayDeviceType.NONE -> {
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                PayDeviceType.BLE -> {
                    Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this, Constants.BLE_TIME_OUT)))
                    Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this, Constants.BLE_TIME_OUT)))
                    mCount = Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT))
                    if(mCashInputMethod == TradeMethod.CashDirect){
                        mTvwMessage.text = "현금 영수증 처리중입니다."
                    }else {
                        mTvwMessage.text = "MSR 거래 중입니다."
                    }
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
                    if(mCashInputMethod == TradeMethod.CashDirect){
                        mTvwMessage.text = "현금 영수증 처리중입니다."
                    }else {
                        mTvwMessage.text = "MSR 거래 중입니다."
                    }
                }
                else->{ //else 경우는 없음
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
            }
            ShowStartTimer()
        }
        else
        {
            //TrdType 이 없는 오류다. 메인으로 본낸다
            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
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
                    if (Setting.getTopContext().toString().contains("CashLoadingActivity"))
                    {
                        when(Setting.g_PayDeviceType){
                            PayDeviceType.NONE -> {
                                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

                            }
                            PayDeviceType.BLE -> {
                                if(Utils.isNullOrEmpty(mCashAuthNum))
                                {
                                    mKocesSdk.__BLEPosinit("99", null)
                                    mKocesSdk.__BLEReadingCancel(null)
                                }
                                mKocesSdk.mTcpClient.Dispose()
                            }
                            PayDeviceType.CAT -> {
                                mKocesSdk.mTcpClient.DisConnectCatServer()
                            }
                            PayDeviceType.LINES ->{
                                if(Utils.isNullOrEmpty(mCashAuthNum))
                                {
                                    mKocesSdk.DeviceReset()
                                }
                                mKocesSdk.mTcpClient.Dispose()
                            }
                            else->{ //else 경우는 없음
                                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

                            }
                        }
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                    }

                }, 2000)
                cancelTimer()



                ShowToastBox("대기시간을 초과하였습니다")
                return@timer
            }
            if(mCountSign)  //사인패드중일경우
            {
                mTvwTimer.text = ""
            }
            else
            {

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

        Cash()
    }
    private fun Cash(){

        when(Setting.g_PayDeviceType){
            PayDeviceType.NONE -> {
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
            PayDeviceType.BLE -> {
                BleCash()
                return
            }
            PayDeviceType.CAT -> {
                mBtnExit.visibility = View.GONE
                CatCash()
                return
            }
            PayDeviceType.LINES ->{
                LineCash()
                return
            }
            else->{ //else 경우는 없음
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
        }
    }

    private fun LineCash(){
        if (mCashAuthNum.length > 12) {
            mCashAuthNum = mCashAuthNum.substring(0, 13)
        }

        if (mTrdCode == "T" && mTradeNo != "") {
        } else {
            if (mCashAuthNum != "") {
            } else {
                if (mKocesSdk.getUsbDevice().size > 0 && mKocesSdk.getICReaderAddr() != "") {
                    if (mKocesSdk.CheckConnectedUsbSerialState(mKocesSdk.getICReaderAddr())) {
                    } else {
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                        return
                    }
                } else if (mKocesSdk.getUsbDevice().size > 0 && mKocesSdk.getMultiReaderAddr() != "") {
                    if (mKocesSdk.CheckConnectedUsbSerialState(mKocesSdk.getMultiReaderAddr())) {
                    } else {
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                        return
                    }
                } else {
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
            }
        }


        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (mTxf == null ) {
            mTxf = 0
        }
        if (mMoney == null) {
            mMoney = 0
        }
//        mMoney += mTxf

        //거래 고유키 예외 처리

        //거래 고유키 예외 처리
        if (mTrdType == "B20") {
            if (moriAuthDate == "" || moriAuthNo == "") //만일 원승인번호 원승인일자가 없는경우
            {
                ShowToastBox(ERROR_NOAUDATE)
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
            if (mTrdCode == "T" || mTrdCode == "t") {
                if (mTradeNo == "") {
                    ShowToastBox(ERROR_NOTRADENO)
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                var CanRea = ""
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRea = "a" + moriAuthDate + moriAuthNo
                }
                /* 현금거래고유키 취소 */
                mKocesSdk.___cashtrade(mTrdType,
                    mTermID,
                    Utils.getDate("yyMMddHHmmss"),
                    Constants.TEST_SOREWAREVERSION,
                    "",
                    CanRea,
                    mKeyYn,
                    null,
                    null,
                    mMoney.toString(),
                    mVat.toString(),
                    mSvc.toString(),
                    mTxf.toString(),
                    mCashTarget.toString(),
                    mCancelReason,
                    "",
                    "",
                    "",
                    "",
                    mTradeNo,
                    TcpInterface.DataListener { _rev ->
                        val tp = CCTcpPacket(_rev)
                        val code = String(tp.resData[0])
                        val authorizationnumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                        val kocesTradeUniqueNumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                        val CardNo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                        val PtResCode =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                        val PtResMessage =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                        val PtResInfo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                        val Encryptionkey_expiration_date =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                        val StoreData =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                        val Money =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                        val Tax =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                        val ServiceCharge =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                        val Tax_exempt =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                        val bangi =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                        val sendData = HashMap<String, String>()
                        sendData["TrdType"] = tp.responseCode
                        sendData["TermID"] = tp.terminalID
                        sendData["TrdDate"] = tp.date
                        sendData["AnsCode"] = code
                        sendData["AuNo"] = authorizationnumber
                        sendData["TradeNo"] = kocesTradeUniqueNumber
                        sendData["CardNo"] = CardNo
                        sendData["Keydate"] = Encryptionkey_expiration_date
                        sendData["MchData"] = StoreData
                        sendData["CardKind"] =
                            "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                        sendData["OrdCd"] = "" //발급사코드
                        sendData["OrdNm"] = "" //발급사명
                        sendData["InpCd"] = "" //매입사코드
                        sendData["InpNm"] = "" //매입사명
                        sendData["DDCYn"] = "" //DDC 여부
                        sendData["EDCYn"] = "" //EDC 여부
                        sendData["GiftAmt"] = "" //기프트카드 잔액
                        sendData["MchNo"] = "" //가맹점번호
                        try {
                            sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                        } catch (ex: UnsupportedEncodingException) {
                        }
                        /* 로그기록. 현금거래고유키취소 */
                        val mLog =
                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
                                    "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                        cout("SEND : KOCES_CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
                        if(code=="0000")
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,mMoney,"",mVat,mSvc,mTxf,0,when(mCashTarget){
                                1 -> TradeMethod.CashPrivate
                                2 -> TradeMethod.CashBusiness
                                3 -> TradeMethod.CashSelf
                                else -> ""
                            },mCashInputMethod,CardNo,"","","","","",tp.date,moriAuthDate,authorizationnumber,moriAuthNo,
                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","","","",
                                "","","","","","","","","","","","")

                        }

                        SendreturnData(RETURN_OK, sendData, "")

                    })
                return
            }
        }

        if (mCashAuthNum == "" && mKeyYn == "S")  //사용자 정보를 직접 입력 하지 않은 경우 카드 리더기의 현금 영수증 발행
        //사용자 정보를 직접 입력 하지 않은 경우 카드 리더기의 현금 영수증 발행
        {
            var DeviceType = 0
            for (n in KocesPosSdk.mDevicesList) {
                if (n.getmType() == 1 || n.getmType() == 4) {
                    DeviceType = 1
                }
            }
            if (DeviceType == 0) {
                ShowToastBox(ERROR_NOSIGNPAD)
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }
            /* 콜백으로 현금거래결과를 받는다 */
            mPaySdk = PaymentSdk(2, false) { result: String?, Code: String, resultData: HashMap<String, String> ->
                if (Code == "SHOW") {
                    if (result != null) {
                        ShowToastBox(result)
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                    }
                    else
                    {
                        ShowToastBox("서버통신 오류")
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                    }
                } else {
                    if (result != null) {
                        CallBackReciptResult(result, Code, resultData)
                    }
                    else {
                        CallBackReciptResult("", Code, resultData)
                    }
                }
            }

            /* 취소구분자체크 */
            var CanRes = ""
            if (mTrdType == "B20") {
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRes = reSetCancelReason(mCancelReason) + moriAuthDate + moriAuthNo
                }
                else if (mTrdType == "B20" && mCancelReason == "") //취소 커맨드에 취소 구분자가 없는 경우
                {
                    ShowToastBox(ERROR_EMPTY_CANCEL_REASON)
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
            }
            /* 카드(멀티)리더기로 현금체크카드를 사용한다 */
            mPaySdk.CashRecipt(
                this,
                mTermID,
                mMoney.toString(),
                mVat,
                mSvc,
                mTxf,
                mCashTarget,
                "0000",
                CanRes,
                if (CanRes == "") "" else moriAuthDate,
                mKeyYn,
                mCancelReason,
                "",
                "",
                "",
                "",
                mTradeNo,
                (if (Setting.getPreference(this, Constants.SELECTED_DEVICE_CARD_READER) == "")
                    Command.TYPEDEFINE_ICMULTIREADER else Command.TYPEDEFINE_ICCARDREADER.toInt()) as Int,
                mStoreName,
                mStoreAddr,
                mStoreNumber,
                mStorePhone,
                mStoreOwner
            )
        }
        else if (mCashAuthNum == "" && mKeyYn == "K")   //사용자가 정보를 직접 입력 하지 않은 경우 키인입력
        //사용자가 정보를 직접 입력 하지 않은 경우 키인입력
        {
            //만일 터치서명 혹은 사인패드미사용 일 경우
            var DeviceType = 0
            for (n in KocesPosSdk.mDevicesList) {
                if (n.getmType() == 2 || n.getmType() == 3 || n.getmType() == 4) {
                    DeviceType = 1
                }
            }
            if (DeviceType == 0) {
                ShowToastBox(ERROR_NOSIGNPAD)
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }

            mPaySdk = PaymentSdk(2, false) { result: String?, Code: String, resultData: HashMap<String, String> ->
                if (Code == "SHOW") {
                    if (result != null) {
                        ShowToastBox(result)
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                    }
                    else
                    {
                        ShowToastBox("서버통신 오류")
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                            intent.putExtra("AppToApp",2)
                        }
                        startActivity(intent)
                    }
                } else {
                    if (result != null) {
                        CallBackReciptResult(result, Code, resultData)
                    }
                    else {
                        CallBackReciptResult("", Code, resultData)
                    }
                }
            }

            /* 취소구분자체크 */
            var CanRes = ""
            if (mTrdType == "B20") {
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRes = reSetCancelReason(mCancelReason) + moriAuthDate + moriAuthNo
                }
            }
            /* 현금거래 서명패드(멀티패드)에 키인입력거래 */
            mPaySdk.CashRecipt(
                this,
                mTermID,
                mMoney.toString(),
                mVat,
                mSvc,
                mTxf,
                mCashTarget,
                "0000",
                CanRes,
                if (CanRes == "") "" else moriAuthDate,
                mKeyYn,
                mCancelReason,
                "",
                "",
                "",
                "",
                mTradeNo,
                Command.TYPEDEFINE_SIGNPAD.toInt(),
                mStoreName,
                mStoreAddr,
                mStoreNumber,
                mStorePhone,
                mStoreOwner
            )
        }
        else
        {
            val id = ByteArray(40)
            Arrays.fill(id, 0x20.toByte())
            System.arraycopy(mCashAuthNum.toByteArray(), 0, id, 0, mCashAuthNum.length)
            if (mTrdType == TCPCommand.CMD_CASH_RECEIPT_REQ) {
                //MchData ="";
                //고객번호를 입력하여 현금거래
                val finalTrdAmt: String = mMoney.toString()
                val finalTaxFreeAmt: String = mTxf.toString()
                mKocesSdk.___cashtrade(mTrdType,
                    mTermID,
                    Utils.getDate("yyMMddHHmmss"),
                    Constants.TEST_SOREWAREVERSION,
                    "",
                    "",
                    mKeyYn,
                    id,
                    null,
                    mMoney.toString(),
                    mVat.toString(),
                    mSvc.toString(),
                    mTxf.toString(),
                    mCashTarget.toString(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    mTradeNo,
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
                        val _receivecode = String(_b.CutToSize(3))
                        val tp = CCTcpPacket(_rev)
                        val code = String(tp.resData[0])
                        val authorizationnumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                        val kocesTradeUniqueNumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                        val CardNo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                        val PtResCode =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                        val PtResMessage =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                        val PtResInfo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                        val Encryptionkey_expiration_date =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                        val StoreData =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                        val Money =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                        val Tax =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                        val ServiceCharge =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                        val Tax_exempt =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                        val bangi =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                        val sendData = HashMap<String, String>()
                        sendData["TrdType"] = tp.responseCode
                        sendData["TermID"] = tp.terminalID
                        sendData["TrdDate"] = tp.date
                        sendData["AnsCode"] = code
                        sendData["AuNo"] = authorizationnumber
                        sendData["TradeNo"] = kocesTradeUniqueNumber
                        sendData["CardNo"] = CardNo
                        sendData["Keydate"] = Encryptionkey_expiration_date
                        sendData["MchData"] = StoreData
                        sendData["CardKind"] =
                            "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                        sendData["OrdCd"] = "" //발급사코드
                        sendData["OrdNm"] = "" //발급사명
                        sendData["InpCd"] = "" //매입사코드
                        sendData["InpNm"] = "" //매입사명
                        sendData["DDCYn"] = "" //DDC 여부
                        sendData["EDCYn"] = "" //EDC 여부
                        sendData["GiftAmt"] = "" //기프트카드 잔액
                        sendData["MchNo"] = "" //가맹점번호
                        if (Setting.getEOTResult() == 1 && _receivecode == "B15") /* EOT정상으로 현금영수증전문응답정상실행 */
                        {
                            try {
                                sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                            } catch (ex: UnsupportedEncodingException) {
                            }
                            if (mEotCancel == 0) {
                                val mLog =
                                    "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
                                            "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                                cout("SEND : KOCES_CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)

                                if(code=="0000")
                                {
                                    mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.NoCancel,
                                        mMoney,"",mVat,mSvc,mTxf, 0,
                                        when(mCashTarget){
                                            1 -> TradeMethod.CashPrivate
                                            2 -> TradeMethod.CashBusiness
                                            3 -> TradeMethod.CashSelf
                                            else -> ""
                                        },mCashInputMethod,CardNo,"","","","","",
                                        tp.date,"",authorizationnumber,"",
                                        kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                        "","", "","","","","","","",
                                        "","","","","")

                                }

                                SendreturnData(RETURN_OK, sendData, "")
                            }
                            else
                            {
                                SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                            }

                        }
                        else if (Setting.getEOTResult() == -1 && _receivecode == "B15") /* EOT비정상으로 현금영수증취소실행 */
                        {
                            val cash_appro_num =
                                Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
                            var cash_appro_date = tp.date.substring(0, 6)
                            var mCancelInfo = "1" + tp.date.substring(0, 6) + cash_appro_num
                            val _tmpCancel = mCancelInfo.toByteArray()
                            _tmpCancel[0] = 0x49.toByte()
                            mCancelInfo = String(_tmpCancel)
                            mEotCancel = 1
                            mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ,
                                mTermID,
                                Utils.getDate("yyMMddHHmmss"),
                                Constants.TEST_SOREWAREVERSION,
                                "",
                                mCancelInfo,
                                mKeyYn,
                                id,
                                null,
                                finalTrdAmt,
                                mVat.toString(),
                                mSvc.toString(),
                                finalTaxFreeAmt,
                                mCashTarget.toString(),
                                "1",
                                "",
                                "",
                                "",
                                "",
                                mTradeNo,
                                TcpInterface.DataListener { _rev ->
                                    val tp = CCTcpPacket(_rev)
                                    val code = String(tp.resData[0])
                                    val authorizationnumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                                    val kocesTradeUniqueNumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                                    val CardNo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                                    val PtResCode =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                                    val PtResMessage =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                                    val PtResInfo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                                    val Encryptionkey_expiration_date =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                                    val StoreData =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                                    val Money =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                                    val Tax =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                                    val ServiceCharge =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                                    val Tax_exempt =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                                    val bangi =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                                    val sendData = HashMap<String, String>()
                                    sendData["TrdType"] = tp.responseCode
                                    sendData["TermID"] = tp.terminalID
                                    sendData["TrdDate"] = tp.date
                                    sendData["AnsCode"] = code
                                    sendData["AuNo"] = authorizationnumber
                                    sendData["TradeNo"] = kocesTradeUniqueNumber
                                    sendData["CardNo"] = CardNo
                                    sendData["Keydate"] = Encryptionkey_expiration_date
                                    sendData["MchData"] = StoreData
                                    sendData["CardKind"] =
                                        "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                    sendData["OrdCd"] = "" //발급사코드
                                    sendData["OrdNm"] = "" //발급사명
                                    sendData["InpCd"] = "" //매입사코드
                                    sendData["InpNm"] = "" //매입사명
                                    sendData["DDCYn"] = "" //DDC 여부
                                    sendData["EDCYn"] = "" //EDC 여부
                                    sendData["GiftAmt"] = "" //기프트카드 잔액
                                    sendData["MchNo"] = "" //가맹점번호
                                    try {
                                        sendData["Message"] =
                                            Utils.getByteToString_euc_kr(tp.resData[1])
                                    } catch (ex: UnsupportedEncodingException) {
                                    }
                                    if (mEotCancel == 0) {
                                        val mLog =
                                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
                                                    "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                                        cout("SEND : KOCES_CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)

                                        if(code=="0000")
                                        {
                                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,
                                                Integer.valueOf(finalTrdAmt),"",mVat,mSvc,Integer.valueOf(finalTaxFreeAmt), 0,
                                                when(mCashTarget){
                                                    1 -> TradeMethod.CashPrivate
                                                    2 -> TradeMethod.CashBusiness
                                                    3 -> TradeMethod.CashSelf
                                                    else -> ""
                                                },mCashInputMethod,CardNo,"","","","","",
                                                tp.date,cash_appro_date,authorizationnumber,cash_appro_num,
                                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                                "","", "","","","","","","",
                                                "","","","","")

                                        }

                                        SendreturnData(RETURN_OK, sendData, "")
                                    }
                                    else
                                    {
                                        SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                                    }

                                })
                            mCancelInfo = ""
                            Setting.setEOTResult(0)
                        } else if (Setting.getEOTResult() == 0 && _receivecode == "B15") /* 승인일때 들어와서 시도하며 취소일때는 하지 않는다 EOT비정상으로 현금영수증취소실행 */ {
                            val cash_appro_num =
                                Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
                            var cash_appro_date = tp.date.substring(0, 6)
                            val mCancelInfo = "1" + tp.date.substring(0, 6) + cash_appro_num
                            mEotCancel = 1
                            mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ,
                                mTermID,
                                Utils.getDate("yyMMddHHmmss"),
                                Constants.TEST_SOREWAREVERSION,
                                "",
                                mCancelInfo,
                                mKeyYn,
                                id,
                                null,
                                finalTrdAmt,
                                mVat.toString(),
                                mSvc.toString(),
                                finalTaxFreeAmt,
                                mCashTarget.toString(),
                                "1",
                                "",
                                "",
                                "",
                                "",
                                mTradeNo,
                                TcpInterface.DataListener { _rev ->
                                    val tp = CCTcpPacket(_rev)
                                    val code = String(tp.resData[0])
                                    val authorizationnumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                                    val kocesTradeUniqueNumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                                    val CardNo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                                    val PtResCode =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                                    val PtResMessage =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                                    val PtResInfo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                                    val Encryptionkey_expiration_date =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                                    val StoreData =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                                            tp.resData[9]
                                        )
                                    val Money =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                                    val Tax =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                                    val ServiceCharge =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                                    val Tax_exempt =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                                    val bangi =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                                    val sendData = HashMap<String, String>()
                                    sendData["TrdType"] = tp.responseCode
                                    sendData["TermID"] = tp.terminalID
                                    sendData["TrdDate"] = tp.date
                                    sendData["AnsCode"] = code
                                    sendData["AuNo"] = authorizationnumber
                                    sendData["TradeNo"] = kocesTradeUniqueNumber
                                    sendData["CardNo"] = CardNo
                                    sendData["Keydate"] = Encryptionkey_expiration_date
                                    sendData["MchData"] = StoreData
                                    sendData["CardKind"] =
                                        "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                    sendData["OrdCd"] = "" //발급사코드
                                    sendData["OrdNm"] = "" //발급사명
                                    sendData["InpCd"] = "" //매입사코드
                                    sendData["InpNm"] = "" //매입사명
                                    sendData["DDCYn"] = "" //DDC 여부
                                    sendData["EDCYn"] = "" //EDC 여부
                                    sendData["GiftAmt"] = "" //기프트카드 잔액
                                    sendData["MchNo"] = "" //가맹점번호
                                    try {
                                        sendData["Message"] =
                                            Utils.getByteToString_euc_kr(tp.resData[1])
                                    } catch (ex: UnsupportedEncodingException) {
                                    }
                                    if (mEotCancel == 0) {
                                        val mLog =
                                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
                                                    "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                                        cout("SEND : KOCES_CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
                                        if(code=="0000")
                                        {
                                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,
                                                Integer.valueOf(finalTrdAmt),"",mVat,mSvc,Integer.valueOf(finalTaxFreeAmt), 0,
                                                when(mCashTarget){
                                                    1 -> TradeMethod.CashPrivate
                                                    2 -> TradeMethod.CashBusiness
                                                    3 -> TradeMethod.CashSelf
                                                    else -> ""
                                                },mCashInputMethod,CardNo,"","","","","",
                                                tp.date,cash_appro_date,authorizationnumber,cash_appro_num,
                                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                                "","", "","","","","","","",
                                                "","","","","")

                                        }

                                        SendreturnData(RETURN_OK, sendData, "")
                                    }
                                    else
                                    {
                                        SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                                    }

                                })
                        }
                        else if (_receivecode == "B25") /* 현금영수증취소 인경우 들어오지 않는다 */
                        {
                            try {
                                sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                            } catch (ex: UnsupportedEncodingException) {
                            }
                            if (mEotCancel == 0) {
//                                val mLog =
//                                    "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
//                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
//                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
//                                            "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
//                                cout("SEND : CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
//                                SendreturnData(RETURN_OK, sendData, "")
                            }
                            else
                            {
                                SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                            }
                        }
                    })
            }
            else if (mTrdType == TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ) //고객번호 입력하여 현금 영수증 취소의 경우
            {
                var CanRea = ""
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRea = reSetCancelReason(mCancelReason) + moriAuthDate + moriAuthNo
                }
                mKocesSdk.___cashtrade(mTrdType,
                    mTermID,
                    Utils.getDate("yyMMddHHmmss"),
                    Constants.TEST_SOREWAREVERSION,
                    "",
                    CanRea,
                    mKeyYn,
                    id,
                    null,
                    mMoney.toString(),
                    mVat.toString(),
                    mSvc.toString(),
                    mTxf.toString(),
                    mCashTarget.toString(),
                    mCancelReason,
                    "",
                    "",
                    "",
                    "",
                    mTradeNo,
                    TcpInterface.DataListener { _rev ->
                        val tp = CCTcpPacket(_rev)
                        val code = String(tp.resData[0])
                        val authorizationnumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                        val kocesTradeUniqueNumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                        val CardNo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                        val PtResCode =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                        val PtResMessage =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                        val PtResInfo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                        val Encryptionkey_expiration_date =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                        val StoreData =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                        val Money =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                        val Tax =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                        val ServiceCharge =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                        val Tax_exempt =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                        val bangi =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                        val sendData = HashMap<String, String>()
                        sendData["TrdType"] = tp.responseCode
                        sendData["TermID"] = tp.terminalID
                        sendData["TrdDate"] = tp.date
                        sendData["AnsCode"] = code
                        sendData["AuNo"] = authorizationnumber
                        sendData["TradeNo"] = kocesTradeUniqueNumber
                        sendData["CardNo"] = CardNo
                        sendData["Keydate"] = Encryptionkey_expiration_date
                        sendData["MchData"] = StoreData
                        sendData["CardKind"] =
                            "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                        sendData["OrdCd"] = "" //발급사코드
                        sendData["OrdNm"] = "" //발급사명
                        sendData["InpCd"] = "" //매입사코드
                        sendData["InpNm"] = "" //매입사명
                        sendData["DDCYn"] = "" //DDC 여부
                        sendData["EDCYn"] = "" //EDC 여부
                        sendData["GiftAmt"] = "" //기프트카드 잔액
                        sendData["MchNo"] = "" //가맹점번호
                        try {
                            sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                        } catch (ex: UnsupportedEncodingException) {
                        }
                        val mLog =
                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
                                    "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                        cout("SEND : KOCES_CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)

                        if(code=="0000")
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,
                                mMoney,"",mVat,mSvc,mTxf, 0,
                                when(mCashTarget){
                                    1 -> TradeMethod.CashPrivate
                                    2 -> TradeMethod.CashBusiness
                                    3 -> TradeMethod.CashSelf
                                    else -> ""
                                },mCashInputMethod,CardNo,"","","","","",
                                tp.date,moriAuthDate,authorizationnumber,moriAuthNo,
                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                "","", "","","","","","","",
                                "","","","","")

                        }

                        SendreturnData(RETURN_OK, sendData, "")

                    })
            }
        }


    }

    fun BleCash() {
        // 1.코세스 고유키 취소의 경우
        // 2. 일반 취소의 경우
        // 3. msr요청
        // 4. 서버 승인 요청

        if(!Setting.bleisConnected && mCashAuthNum == "")
        {
            if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
            }
            else
            {
                intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                intent.putExtra("AppToApp",2)
            }
            startActivity(intent)
            return
        }

        if (mCashAuthNum.length > 12)
        {
            mCashAuthNum = mCashAuthNum.substring(0, 13)
        }

        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (mTxf == null ) {
            mTxf = 0
        }
        if (mMoney == null) {
            mMoney = 0
        }
//        mMoney += mTxf

        /* 로그기록. 현금거래요청 */

        /* 로그기록. 현금거래요청 */
        val mLog =
            "TrdType : " + mTrdType + "," + "단말기ID : " + mTermID + "," + "원거래일자 : " + moriAuthDate + "," + "원승인번호 : " + moriAuthNo + "," +
                    "키인 : " + mKeyYn + "," + "거래금액 : " + mMoney + "," + "세금 : " + mVat + "," + "봉사료 : " + mSvc + "," + "비과세 : " + mTxf + "," +
                    "거래고유번호 : " + mTradeNo + "," + "개인법인구분 : " + mCashTarget + "," + "취소사유 : " + mCancelReason + "," + "고객번호 : " + mCashAuthNum
        cout("RECV : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)

        //거래 고유키 예외 처리
        if (mTrdType == "B20")
        {
            if (moriAuthDate == "" || moriAuthNo == "") //만일 원승인번호 원승인일자가 없는경우
            {
                ShowToastBox(ERROR_NOAUDATE)
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                }
                startActivity(intent)
                return
            }

            if (mTrdCode == "T" || mTrdCode == "t") {
                if (mTradeNo == "") {
                    ShowToastBox(ERROR_NOTRADENO)
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        intent.putExtra("AppToApp",2)
                    }
                    startActivity(intent)
                    return
                }
                var CanRea = ""
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRea = "a" + moriAuthDate + moriAuthNo
                }
                /* 현금거래고유키 취소 */
                mKocesSdk.___cashtrade(mTrdType,
                    mTermID,
                    Utils.getDate("yyMMddHHmmss"),
                    Constants.TEST_SOREWAREVERSION,
                    "",
                    CanRea,
                    mKeyYn,
                    null,
                    null,
                    mMoney.toString(),
                    mVat.toString(),
                    mSvc.toString(),
                    mTxf.toString(),
                    mCashTarget.toString(),
                    mCancelReason.toString(),
                    "",
                    "",
                    "",
                    "",
                    if (mTradeNo == null) "" else mTradeNo,
                    TcpInterface.DataListener { _rev ->
                        val tp = CCTcpPacket(_rev)
                        val code = String(tp.resData[0])
                        val authorizationnumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                        val kocesTradeUniqueNumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                                tp.resData[3]
                            ) //KOCES거래고유번호
                        val CardNo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                        val PtResCode =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                        val PtResMessage =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                        val PtResInfo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                        val Encryptionkey_expiration_date =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                                tp.resData[8]
                            )
                        val StoreData =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                        val Money =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                        val Tax =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                        val ServiceCharge =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                        val Tax_exempt =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                        val bangi =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                        val sendData = HashMap<String, String>()
                        sendData["TrdType"] = tp.responseCode
                        sendData["TermID"] = tp.terminalID
                        sendData["TrdDate"] = tp.date
                        sendData["AnsCode"] = code
                        sendData["AuNo"] = authorizationnumber
                        sendData["TradeNo"] = kocesTradeUniqueNumber
                        sendData["CardNo"] = CardNo
                        sendData["Keydate"] = Encryptionkey_expiration_date
                        sendData["MchData"] = StoreData
                        sendData["CardKind"] =
                            "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                        sendData["OrdCd"] = "" //발급사코드
                        sendData["OrdNm"] = "" //발급사명
                        sendData["InpCd"] = "" //매입사코드
                        sendData["InpNm"] = "" //매입사명
                        sendData["DDCYn"] = "" //DDC 여부
                        sendData["EDCYn"] = "" //EDC 여부
                        sendData["GiftAmt"] = "" //기프트카드 잔액
                        sendData["MchNo"] = "" //가맹점번호
                        try {
                            sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                        } catch (ex: UnsupportedEncodingException) {
                        }
                        /* 로그기록. 현금거래고유키취소 */
                        val mLog =
                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                    "가맹점데이터 : " + StoreData
                        cout("SEND : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)
                        if(code=="0000")
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,mMoney,"",mVat,mSvc,mTxf,0,when(mCashTarget){
                                1 -> TradeMethod.CashPrivate
                                2 -> TradeMethod.CashBusiness
                                3 -> TradeMethod.CashSelf
                                else -> ""
                            },mCashInputMethod,CardNo,"","","","","",tp.date,moriAuthDate,authorizationnumber,moriAuthNo,
                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","","","",
                                "","","","","","","","","","","","")

                        }

                        SendreturnData(RETURN_OK, sendData, "")

                    })
                return
            }

        }


        if (mCashAuthNum == "" && mKeyYn == "S") //사용자 정보를 직접 입력 하지 않은 경우 카드 리더기의 현금 영수증 발행
        {

            /* 취소구분자체크 */
            var CanRes = ""
            if (mTrdType == "B20") {
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRes = reSetCancelReason(mCancelReason.toString()) + moriAuthDate + moriAuthNo
                }
                else if (mTrdType == "B20" && mCancelReason == "") //취소 커맨드에 취소 구분자가 없는 경우
                {
                    ShowToastBox(ERROR_EMPTY_CANCEL_REASON)
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
            }

            /* 콜백으로 현금거래결과를 받는다 */
            mBleSdk = BlePaymentSdk(2) { result: String?, Code: String, resultData: HashMap<String, String> ->
                if (Code == "SHOW") {
                    if (result != null) {
                        ShowToastBox(result)
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        }
                        startActivity(intent)
                    }
                    else
                    {
                        ShowToastBox("서버통신 오류")
                        if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                        {
                            intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                            intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                            intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                            intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                            intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                            intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                        }
                        else
                        {
                            intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                        }
                        startActivity(intent)
                    }
                }
                else {
                    /* 콜백으로 결과를 받아온다 */
                    if (result != null) {
                        CallBackReciptResult(result, Code, resultData)
                    }
                    else {
                        CallBackReciptResult("", Code, resultData)
                    }
                }
            }

            /* 카드(멀티)리더기로 현금체크카드를 사용한다 */
            mBleSdk.CashRecipt(
                this,
                mTermID,
                mMoney.toString(),
                mVat,
                mSvc,
                mTxf,
                mCashTarget,
                "0000",
                CanRes,
                if (CanRes == "") "" else moriAuthDate,
                mKeyYn,
                mCancelReason,
                "",
                "",
                "",
                "",
                mTradeNo,
                false,
                mStoreName,
                mStoreAddr,
                mStoreNumber,
                mStorePhone,
                mStoreOwner
            )

        }
        else if (mCashAuthNum == "" && mKeyYn == "K") //사용자가 정보를 직접 입력 하지 않은 경우 키인입력
        {
            SendreturnData(RETURN_CANCEL, null, ERROR_NOBLEKEYIN)
            return
            /** 211230 kim.jy ble 거래 입력 번호가 없는 경우 KeyYn이 k로 오는 경우에는 에러 처리 한다.  */

        } else {
            val id = ByteArray(40)
            Arrays.fill(id, 0x20.toByte())
            System.arraycopy(mCashAuthNum.toByteArray(), 0, id, 0, mCashAuthNum.length)
            if (mTrdType == TCPCommand.CMD_CASH_RECEIPT_REQ) {
                //MchData ="";
                //고객번호를 입력하여 현금거래
                val finalTrdAmt: String = mMoney.toString()
                val finalTaxFreeAmt: String = mTxf.toString()
                mKocesSdk.___cashtrade(mTrdType,
                    mTermID,
                    Utils.getDate("yyMMddHHmmss"),
                    Constants.TEST_SOREWAREVERSION,
                    "",
                    "",
                    mKeyYn,
                    id,
                    null,
                    mMoney.toString(),
                    mVat.toString(),
                    mSvc.toString(),
                    mTxf.toString(),
                    mCashTarget.toString(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    mTradeNo,
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
                        val _receivecode = String(_b.CutToSize(3))
                        val tp = CCTcpPacket(_rev)
                        val code = String(tp.resData[0])
                        val authorizationnumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                                tp.resData[2]
                            ) //현금영수증 승인번호
                        val kocesTradeUniqueNumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                                tp.resData[3]
                            ) //KOCES거래고유번호
                        val CardNo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                        val PtResCode =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                        val PtResMessage =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                        val PtResInfo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                        val Encryptionkey_expiration_date =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(
                                tp.resData[8]
                            )
                        val StoreData =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                        val Money =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                        val Tax =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                        val ServiceCharge =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                        val Tax_exempt =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                        val bangi =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                        val sendData = HashMap<String, String>()
                        sendData["TrdType"] = tp.responseCode
                        sendData["TermID"] = tp.terminalID
                        sendData["TrdDate"] = tp.date
                        sendData["AnsCode"] = code
                        sendData["AuNo"] = authorizationnumber
                        sendData["TradeNo"] = kocesTradeUniqueNumber
                        sendData["CardNo"] = CardNo
                        sendData["Keydate"] = Encryptionkey_expiration_date
                        sendData["MchData"] = StoreData
                        sendData["CardKind"] =
                            "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                        sendData["OrdCd"] = "" //발급사코드
                        sendData["OrdNm"] = "" //발급사명
                        sendData["InpCd"] = "" //매입사코드
                        sendData["InpNm"] = "" //매입사명
                        sendData["DDCYn"] = "" //DDC 여부
                        sendData["EDCYn"] = "" //EDC 여부
                        sendData["GiftAmt"] = "" //기프트카드 잔액
                        sendData["MchNo"] = "" //가맹점번호
                        if (Setting.getEOTResult() == 1 && _receivecode == "B15") /* EOT정상으로 현금영수증전문응답정상실행 */ {
                            try {
                                sendData["Message"] =
                                    Utils.getByteToString_euc_kr(tp.resData[1])
                            } catch (ex: UnsupportedEncodingException) {
                            }
                            if (mEotCancel == 0) {
                                val mLog =
                                    "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                            "가맹점데이터 : " + StoreData
                                cout("SEND : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)

                                if(code=="0000")
                                {
                                    mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.NoCancel,
                                        mMoney,"",mVat,mSvc,mTxf, 0,
                                        when(mCashTarget){
                                            1 -> TradeMethod.CashPrivate
                                            2 -> TradeMethod.CashBusiness
                                            3 -> TradeMethod.CashSelf
                                            else -> ""
                                        },mCashInputMethod,CardNo,"","","","","",
                                        tp.date,"",authorizationnumber,"",
                                        kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                        "","", "","","","","","","",
                                        "","","","","")

                                }

                                SendreturnData(RETURN_OK, sendData, "")
                            }
                            else {
                                SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                            }

                        }
                        else if (Setting.getEOTResult() == -1 && _receivecode == "B15") /* EOT비정상으로 현금영수증취소실행 */
                        {
                            val cash_appro_num =
                                Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
                            val cash_appro_date = tp.date.substring(0, 6)
                            var mCancelInfo = "1" + tp.date.substring(0, 6) + cash_appro_num
                            val _tmpCancel = mCancelInfo.toByteArray()
                            _tmpCancel[0] = 0x49.toByte()
                            mCancelInfo = String(_tmpCancel)
                            mEotCancel = 1
                            mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ,
                                mTermID,
                                Utils.getDate("yyMMddHHmmss"),
                                Constants.TEST_SOREWAREVERSION,
                                "",
                                mCancelInfo,
                                mKeyYn,
                                id,
                                null,
                                finalTrdAmt,
                                mVat.toString(),
                                mSvc.toString(),
                                finalTaxFreeAmt,
                                mCashTarget.toString(),
                                "1",
                                "",
                                "",
                                "",
                                "",
                                mTradeNo,
                                TcpInterface.DataListener { _rev ->
                                    val tp = CCTcpPacket(_rev)
                                    val code = String(tp.resData[0])
                                    val authorizationnumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                                    val kocesTradeUniqueNumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                                    val CardNo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                                    val PtResCode =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                                    val PtResMessage =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                                    val PtResInfo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                                    val Encryptionkey_expiration_date =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                                    val StoreData =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                                    val Money =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                                    val Tax =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                                    val ServiceCharge =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                                    val Tax_exempt =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                                    val bangi =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                                    val sendData = HashMap<String, String>()
                                    sendData["TrdType"] = tp.responseCode
                                    sendData["TermID"] = tp.terminalID
                                    sendData["TrdDate"] = tp.date
                                    sendData["AnsCode"] = code
                                    sendData["AuNo"] = authorizationnumber
                                    sendData["TradeNo"] = kocesTradeUniqueNumber
                                    sendData["CardNo"] = CardNo
                                    sendData["Keydate"] = Encryptionkey_expiration_date
                                    sendData["MchData"] = StoreData
                                    sendData["CardKind"] =
                                        "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                    sendData["OrdCd"] = "" //발급사코드
                                    sendData["OrdNm"] = "" //발급사명
                                    sendData["InpCd"] = "" //매입사코드
                                    sendData["InpNm"] = "" //매입사명
                                    sendData["DDCYn"] = "" //DDC 여부
                                    sendData["EDCYn"] = "" //EDC 여부
                                    sendData["GiftAmt"] = "" //기프트카드 잔액
                                    sendData["MchNo"] = "" //가맹점번호
                                    try {
                                        sendData["Message"] =
                                            Utils.getByteToString_euc_kr(tp.resData[1])
                                    } catch (ex: UnsupportedEncodingException) {
                                    }
                                    if (mEotCancel == 0) {
                                        val mLog =
                                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " +
                                                    Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                                        cout("SEND : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)
                                        if (code == "0000")
                                        {
                                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,
                                                Integer.valueOf(finalTrdAmt),"",mVat,mSvc,Integer.valueOf(finalTaxFreeAmt), 0,
                                                when(mCashTarget){
                                                    1 -> TradeMethod.CashPrivate
                                                    2 -> TradeMethod.CashBusiness
                                                    3 -> TradeMethod.CashSelf
                                                    else -> ""
                                                },mCashInputMethod,CardNo,"","","","","",
                                                tp.date,cash_appro_date,authorizationnumber,cash_appro_num,
                                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                                "","", "","","","","","","",
                                                "","","","","")
                                        }

                                        SendreturnData(RETURN_OK, sendData, "")
                                    }
                                    else
                                    {
                                        SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                                    }

                                })
                            mCancelInfo = ""
                            Setting.setEOTResult(0)
                        }
                        else if (Setting.getEOTResult() == 0 && _receivecode == "B15") /* 승인일때 들어와서 시도하며 취소일때는 하지 않는다 EOT비정상으로 현금영수증취소실행 */
                        {
                            val cash_appro_num =
                                Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
                            val cash_appro_date = tp.date.substring(0, 6)
                            val mCancelInfo = "1" + tp.date.substring(0, 6) + cash_appro_num
                            mEotCancel = 1
                            mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ,
                                mTermID,
                                Utils.getDate("yyMMddHHmmss"),
                                Constants.TEST_SOREWAREVERSION,
                                "",
                                mCancelInfo,
                                mKeyYn,
                                id,
                                null,
                                finalTrdAmt,
                                mVat.toString(),
                                mSvc.toString(),
                                finalTaxFreeAmt,
                                mCashTarget.toString(),
                                "1",
                                "",
                                "",
                                "",
                                "",
                                mTradeNo,
                                TcpInterface.DataListener { _rev ->
                                    val tp = CCTcpPacket(_rev)
                                    val code = String(tp.resData[0])
                                    val authorizationnumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                                    val kocesTradeUniqueNumber =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                                    val CardNo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                                    val PtResCode =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                                    val PtResMessage =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                                    val PtResInfo =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                                    val Encryptionkey_expiration_date =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                                    val StoreData =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                                    val Money =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                                    val Tax =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                                    val ServiceCharge =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                                    val Tax_exempt =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                                    val bangi =
                                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                                    val sendData = HashMap<String, String>()
                                    sendData["TrdType"] = tp.responseCode
                                    sendData["TermID"] = tp.terminalID
                                    sendData["TrdDate"] = tp.date
                                    sendData["AnsCode"] = code
                                    sendData["AuNo"] = authorizationnumber
                                    sendData["TradeNo"] = kocesTradeUniqueNumber
                                    sendData["CardNo"] = CardNo
                                    sendData["Keydate"] = Encryptionkey_expiration_date
                                    sendData["MchData"] = StoreData
                                    sendData["CardKind"] =
                                        "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                    sendData["OrdCd"] = "" //발급사코드
                                    sendData["OrdNm"] = "" //발급사명
                                    sendData["InpCd"] = "" //매입사코드
                                    sendData["InpNm"] = "" //매입사명
                                    sendData["DDCYn"] = "" //DDC 여부
                                    sendData["EDCYn"] = "" //EDC 여부
                                    sendData["GiftAmt"] = "" //기프트카드 잔액
                                    sendData["MchNo"] = "" //가맹점번호
                                    try {
                                        sendData["Message"] =
                                            Utils.getByteToString_euc_kr(tp.resData[1])
                                    } catch (ex: UnsupportedEncodingException) {
                                    }
                                    if (mEotCancel == 0) {
                                        val mLog =
                                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " +
                                                    Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                                        cout("SEND : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)
                                        if(code=="0000")
                                        {
                                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,
                                                Integer.valueOf(finalTrdAmt),"",mVat,mSvc,Integer.valueOf(finalTaxFreeAmt), 0,
                                                when(mCashTarget){
                                                    1 -> TradeMethod.CashPrivate
                                                    2 -> TradeMethod.CashBusiness
                                                    3 -> TradeMethod.CashSelf
                                                    else -> ""
                                                },mCashInputMethod,CardNo,"","","","","",
                                                tp.date,cash_appro_date,authorizationnumber,cash_appro_num,
                                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                                "","", "","","","","","","",
                                                "","","","","")
                                        }

                                        SendreturnData(RETURN_OK, sendData, "")
                                    }
                                    else
                                    {
                                        SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                                    }
                                })
                        } else if (_receivecode == "B25") /* 현금영수증취소 인경우 들어오지 않는다*/ {
                            try {
                                sendData["Message"] =
                                    Utils.getByteToString_euc_kr(tp.resData[1])
                            } catch (ex: UnsupportedEncodingException) {
                            }
                            if (mEotCancel == 0) {
//                                    val mLog =
//                                        "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
//                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " +
//                                                Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
//                                    cout("SEND : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)
//                                    mKocesSdk.setSqliteDB_InsertTradeData(mTermID,TradeMethod.Cash,TradeMethod.Cancel,
//                                        Integer.valueOf(finalTrdAmt),"",mVat,mSvc,Integer.valueOf(finalTaxFreeAmt), 0,
//                                        when(mCashTarget){
//                                            1 -> TradeMethod.CashPrivate
//                                            2 -> TradeMethod.CashBusiness
//                                            3 -> TradeMethod.CashSelf
//                                            else -> ""
//                                        },mCashInputMethod,CardNo,"","","","","",
//                                        tp.date,cash_appro_date,authorizationnumber,cash_appro_num,
//                                        kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
//                                        "","", "","","","","","","",
//                                        "","","","","")
//                                    SendreturnData(RETURN_OK, sendData, "")
                            }
                            else
                            {
                                SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
                            }
                        }
                    })
            }
            else if (mTrdType == TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ) //고객번호 입력하여 현금 영수증 취소의 경우
            {
                var CanRea = ""
                if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
                    CanRea = reSetCancelReason(mCancelReason.toString()) + moriAuthDate + moriAuthNo
                }
                mKocesSdk.___cashtrade(mTrdType,
                    mTermID,
                    Utils.getDate("yyMMddHHmmss"),
                    Constants.TEST_SOREWAREVERSION,
                    "",
                    CanRea,
                    mKeyYn,
                    id,
                    null,
                    mMoney.toString(),
                    mVat.toString(),
                    mSvc.toString(),
                    mTxf.toString(),
                    mCashTarget.toString(),
                    mCancelReason.toString(),
                    "",
                    "",
                    "",
                    "",
                    mTradeNo,
                    TcpInterface.DataListener { _rev ->
                        val tp = CCTcpPacket(_rev)
                        val code = String(tp.resData[0])
                        val authorizationnumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                        val kocesTradeUniqueNumber =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                        val CardNo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                        val PtResCode =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                        val PtResMessage =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                        val PtResInfo =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                        val Encryptionkey_expiration_date =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                        val StoreData =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                        val Money =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                        val Tax =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                        val ServiceCharge =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                        val Tax_exempt =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                        val bangi =
                            Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                        val sendData = HashMap<String, String>()
                        sendData["TrdType"] = tp.responseCode
                        sendData["TermID"] = tp.terminalID
                        sendData["TrdDate"] = tp.date
                        sendData["AnsCode"] = code
                        sendData["AuNo"] = authorizationnumber
                        sendData["TradeNo"] = kocesTradeUniqueNumber
                        sendData["CardNo"] = CardNo
                        sendData["Keydate"] = Encryptionkey_expiration_date
                        sendData["MchData"] = StoreData
                        sendData["CardKind"] =
                            "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                        sendData["OrdCd"] = "" //발급사코드
                        sendData["OrdNm"] = "" //발급사명
                        sendData["InpCd"] = "" //매입사코드
                        sendData["InpNm"] = "" //매입사명
                        sendData["DDCYn"] = "" //DDC 여부
                        sendData["EDCYn"] = "" //EDC 여부
                        sendData["GiftAmt"] = "" //기프트카드 잔액
                        sendData["MchNo"] = "" //가맹점번호
                        try {
                            sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                        } catch (ex: UnsupportedEncodingException) {
                        }
                        val mLog =
                            "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," +
                                    "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," + "가맹점데이터 : " + StoreData
                        cout("SEND : KOCES_CASHRECIPT_BLE", Utils.getDate("yyyyMMddHHmmss"), mLog)
                        if(code=="0000")
                        {
                            mKocesSdk.setSqliteDB_InsertTradeData(mTermID,mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,TradeMethod.Cash,TradeMethod.Cancel,
                                mMoney,"",mVat,mSvc,mTxf, 0,
                                when(mCashTarget){
                                    1 -> TradeMethod.CashPrivate
                                    2 -> TradeMethod.CashBusiness
                                    3 -> TradeMethod.CashSelf
                                    else -> ""
                                },mCashInputMethod,CardNo,"","","","","",
                                tp.date,moriAuthDate,authorizationnumber,moriAuthNo,
                                kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","",
                                "","", "","","","","","","",
                                "","","","","")
                        }

                        SendreturnData(RETURN_OK, sendData, "")

                    })
            }
        }


//        if(!Utils.isNullOrEmpty(mTradeNo) && mTradeNoOption)    //고유 거래키가 있고 고유 거래키 요청인 경우에만 처리
//        {   //고유 거래키 취소
//            return
//        }
//        else if(!Utils.isNullOrEmpty(moriAuthDate) && !Utils.isNullOrEmpty(moriAuthNo))
//        { //일반 취소
//
//            if(mCashAuthNum=="")
//            {
//                CashBleCancel()
//                return
//            }
//            else{
//                CashTradeDirectCancel()
//                return
//            }
//        }
//
//        if(mCashAuthNum==""){   //Msr
//
//        }else { //현금 영수증 발급
//            runOnUiThread {
//                //TODO 22-02-17 If you do not use a breakpoint, it will not be applied.
//                mTvwMessage.text = mProcMsg1
//            }
//            var KeyYn = "K"
//            val id = ByteArray(40)
//            Arrays.fill(id, 0x20.toByte())
//            System.arraycopy(mCashAuthNum.toByteArray(), 0, id, 0, mCashAuthNum.length)
//
//                //MchData ="";
//                //고객번호를 입력하여 현금거래
//
//                mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_REQ, getTID(), Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", "", KeyYn, id, null,
//                        TrdAmt, mVat.toString(),mSvc.toString(),mTxf.toString(),mCashTarget.toString(), "", "", "","", "",mTradeNo, TcpInterface.DataListener { _rev ->
//                    var _tmpEot = 0
//                    while (true) //TCP EOT 수신 될때 까지 기다리기 위해서
//                    {
//                        if (Setting.getEOTResult() != 0) {
//                            break
//                        }
//                        try {
//                            Thread.sleep(100)
//                        } catch (e: InterruptedException) {
//                            e.printStackTrace()
//                        }
//                        _tmpEot++
//                        if (_tmpEot >= 30) {
//                            break
//                        }
//                    }
//                    val _b = KByteArray(_rev)
//                    _b.CutToSize(10)
//                    val _receivecode = String(_b.CutToSize(3))
//                    val tp = CCTcpPacket(_rev)
//                    val code = String(tp.resData[0])
//                    val authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
//                    val kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
//                    val CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
//                    val PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
//                    val PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
//                    val PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
//                    val Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
//                    val StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
//                    val Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
//                    val Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
//                    val ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
//                    val Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
//                    val bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
//                    val sendData = HashMap<String, String>()
//                    sendData["TrdType"] = tp.responseCode
//                    sendData["TermID"] = tp.terminalID
//                    sendData["TrdDate"] = tp.date
//                    sendData["AnsCode"] = code
//                    sendData["AuNo"] = authorizationnumber
//                    sendData["TradeNo"] = kocesTradeUniqueNumber
//                    sendData["CardNo"] = CardNo
//                    sendData["Keydate"] = Encryptionkey_expiration_date
//                    sendData["MchData"] = StoreData
//                    sendData["CardKind"] = "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
//                    sendData["OrdCd"] = "" //발급사코드
//                    sendData["OrdNm"] = "" //발급사명
//                    sendData["InpCd"] = "" //매입사코드
//                    sendData["InpNm"] = "" //매입사명
//                    sendData["DDCYn"] = "" //DDC 여부
//                    sendData["EDCYn"] = "" //EDC 여부
//                    sendData["GiftAmt"] = "" //기프트카드 잔액
//                    sendData["MchNo"] = "" //가맹점번호
//                    if (Setting.getEOTResult() == 1 && _receivecode == "B15") /* EOT정상으로 현금영수증전문응답정상실행 */ {
//                        try {
//                            sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
//                        } catch (ex: UnsupportedEncodingException) {
//                        }
//                        if (mEotCancel == 0) {
//                            val mLog = "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
//                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
//                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                    "가맹점데이터 : " + StoreData
//                            cout("SEND : CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
//                            //22-02-16 kim.jy 작업 필요
//                            //여기서 db에 데이터를 저장 한다.
//
//                            mKocesSdk.setSqliteDB_InsertTradeData(getTID(),TradeMethod.Cash,TradeMethod.NoCancel,mMoney,"",mVat,mSvc,mTxf,0,when(mCashTarget){
//                                1 -> TradeMethod.CashPrivate
//                                2 -> TradeMethod.CashBusiness
//                                3 -> TradeMethod.CashSelf
//                                else -> ""
//                            },TradeMethod.CashDirect,CardNo,"","","","","",tp.date,"",authorizationnumber,"",
//                                    kocesTradeUniqueNumber,Utils.getByteToString_euc_kr(tp.resData[1]),"","","","",
//                                    "","","","","","","","","","","","")
//                            //여기서 영수증 화면으로 보낸다.
//                            intent = Intent(this@CashLoadingActivity,ReceiptCashActivity::class.java)
//                            intent.putExtra("data","last")
//                            cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
//                            startActivity(intent)
//                        } else {
//                            //SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
//                        }
//                    } else if (Setting.getEOTResult() == -1 && _receivecode == "B15") /* EOT비정상으로 현금영수증취소실행 */ {
//                        val cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
//                        var mCancelInfo = "1" + tp.date.substring(0, 6) + cash_appro_num
//                        val _tmpCancel = mCancelInfo.toByteArray()
//                        _tmpCancel[0] = 0x49.toByte()
//                        mCancelInfo = String(_tmpCancel)
//                        mEotCancel = 1
//                        mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, getTID(), Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", mCancelInfo, KeyYn, id, null,
//                                TrdAmt, mVat.toString(),mSvc.toString(),mTxf.toString(),mCashTarget.toString(), "1", "", "", "", "", mTradeNo, TcpInterface.DataListener { _rev ->
//                            val tp = CCTcpPacket(_rev)
//                            val code = String(tp.resData[0])
//                            val authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
//                            val kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
//                            val CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
//                            val PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
//                            val PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
//                            val PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
//                            val Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
//                            val StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
//                            val Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
//                            val Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
//                            val ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
//                            val Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
//                            val bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
//                            val sendData = HashMap<String, String>()
//                            sendData["TrdType"] = tp.responseCode
//                            sendData["TermID"] = tp.terminalID
//                            sendData["TrdDate"] = tp.date
//                            sendData["AnsCode"] = code
//                            sendData["AuNo"] = authorizationnumber
//                            sendData["TradeNo"] = kocesTradeUniqueNumber
//                            sendData["CardNo"] = CardNo
//                            sendData["Keydate"] = Encryptionkey_expiration_date
//                            sendData["MchData"] = StoreData
//                            sendData["CardKind"] = "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
//                            sendData["OrdCd"] = "" //발급사코드
//                            sendData["OrdNm"] = "" //발급사명
//                            sendData["InpCd"] = "" //매입사코드
//                            sendData["InpNm"] = "" //매입사명
//                            sendData["DDCYn"] = "" //DDC 여부
//                            sendData["EDCYn"] = "" //EDC 여부
//                            sendData["GiftAmt"] = "" //기프트카드 잔액
//                            sendData["MchNo"] = "" //가맹점번호
//                            try {
//                                sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
//                            } catch (ex: UnsupportedEncodingException) {
//                            }
//                            if (mEotCancel == 0) {
//                                val mLog = "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
//                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
//                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                        "가맹점데이터 : " + StoreData
//                                cout("SEND : CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
//                                //TODO 22-02-16 kim.jy 작업 필요
//                                //SendreturnData(RETURN_OK, sendData, null)
//                            } else {
//                                //SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
//                            }
//                        })
//                        mCancelInfo = ""
//                        Setting.setEOTResult(0)
//                    } else if (Setting.getEOTResult() == 0 && _receivecode == "B15") /* 승인일때 들어와서 시도하며 취소일때는 하지 않는다 EOT비정상으로 현금영수증취소실행 */ {
//                        val cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //신용승인번호
//                        val mCancelInfo = "1" + tp.date.substring(0, 6) + cash_appro_num
//                        mEotCancel = 1
//                        mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, getTID(), Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", mCancelInfo, KeyYn, id, null,
//                                TrdAmt, mVat.toString(),mSvc.toString(),mTxf.toString(),mCashTarget.toString(), "1", "", "", "", "", mTradeNo, TcpInterface.DataListener { _rev ->
//                            val tp = CCTcpPacket(_rev)
//                            val code = String(tp.resData[0])
//                            val authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
//                            val kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
//                            val CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
//                            val PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
//                            val PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
//                            val PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
//                            val Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
//                            val StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
//                            val Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
//                            val Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
//                            val ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
//                            val Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
//                            val bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
//                            val sendData = HashMap<String, String>()
//                            sendData["TrdType"] = tp.responseCode
//                            sendData["TermID"] = tp.terminalID
//                            sendData["TrdDate"] = tp.date
//                            sendData["AnsCode"] = code
//                            sendData["AuNo"] = authorizationnumber
//                            sendData["TradeNo"] = kocesTradeUniqueNumber
//                            sendData["CardNo"] = CardNo
//                            sendData["Keydate"] = Encryptionkey_expiration_date
//                            sendData["MchData"] = StoreData
//                            sendData["CardKind"] = "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
//                            sendData["OrdCd"] = "" //발급사코드
//                            sendData["OrdNm"] = "" //발급사명
//                            sendData["InpCd"] = "" //매입사코드
//                            sendData["InpNm"] = "" //매입사명
//                            sendData["DDCYn"] = "" //DDC 여부
//                            sendData["EDCYn"] = "" //EDC 여부
//                            sendData["GiftAmt"] = "" //기프트카드 잔액
//                            sendData["MchNo"] = "" //가맹점번호
//                            try {
//                                sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
//                            } catch (ex: UnsupportedEncodingException) {
//                            }
//                            if (mEotCancel == 0) {
//                                val mLog = "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
//                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
//                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                        "가맹점데이터 : " + StoreData
//                                cout("SEND : CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
//                                //TODO 22-02-16 kim.jy 작업 필요
//                                //SendreturnData(RETURN_OK, sendData, null)
//                            } else {
//                                //TODO 22-02-16 kim.jy 작업 필요
//                                //SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
//                            }
//                        })
//                    } else if (_receivecode == "B25") /* 현금영수증취소 인경우 */ {
//                        try {
//                            sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
//                        } catch (ex: UnsupportedEncodingException) {
//                        }
//                        if (mEotCancel == 0) {
//                            val mLog = "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
//                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
//                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                    "가맹점데이터 : " + StoreData
//                            cout("SEND : CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
//                            //TODO 22-02-16 kim.jy 작업 필요
//                            //SendreturnData(RETURN_OK, sendData, null)
//                        } else {
//                            //SendreturnData(RETURN_CANCEL, null, "망취소발생, 거래실패" + sendData["Message"])
//                        }
//                    }
//                })
//
//
//
//
//        }

    }
    fun CatCash() {
        // 1.코세스 고유키 취소의 경우
        // 2. 일반 취소의 경우
        // 3. msr요청
        // 4. 서버 승인 요청
        if (mCashAuthNum !== "" && mCashAuthNum.length > 12) {
            mCashAuthNum = mCashAuthNum.substring(0, 13)
        }


        val CanRes = ""
        if (mTrdCode == "t" || mTrdCode == "T") {
            mTrdCode = "a"
        } else {
            mTrdCode = "0"
        }

        if (moriAuthNo != "") {
            moriAuthNo = moriAuthNo.replace(" ", "")
        }

        /* 로그기록. 현금거래요청 */

        /* 로그기록. 현금거래요청 */
        val mLog =
            "TrdType : " + mTrdType + "," + "단말기ID : " + mTermID + "," + "원거래일자 : " + moriAuthDate + "," + "원승인번호 : " + moriAuthNo + "," +
                    "키인 : " + mKeyYn + "," + "거래금액 : " + mMoney + "," + "세금 : " + mVat + "," + "봉사료 : " + mSvc + "," + "비과세 : " + mTxf + "," +
                    "거래구분 : " + mTrdCode + "," + "거래고유번호 : " + mTradeNo + "," + "개인법인구분 : " + mCashTarget + "," +
                    "취소사유 : " + mCancelReason + "," + "고객번호 : " + mCashAuthNum
        cout("RECV : KOCES_CASHRECIPT_CAT", Utils.getDate("yyyyMMddHHmmss"), mLog)

        mCatSdk = CatPaymentSdk(this, Constants.CatPayType.Cash) { result: String?, Code: String, resultData: HashMap<String, String> ->
            if (Code == "SHOW") {
                if (result != null) {
                    ShowToastBox(result)
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                }
                else
                {
                    ShowToastBox("서버통신 오류")
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
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

        //만일 취소인 경우
        if (mTrdType == "B20") {
            if (moriAuthDate == "" || moriAuthNo == "") //만일 원승인번호 원승인일자가 없는경우
            {
                ShowToastBox(ERROR_NOAUDATE)
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

            //취소
            if (mTrdCode == "a") {
                if (mCashAuthNum === "") {
                    if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
                        mTermID = ""
                    }
                    mCatSdk.CashRecipt(
                        mTermID,
                        mMoney.toString(),
                        mVat.toString(),
                        mSvc.toString(),
                        mTxf.toString(),
                        moriAuthDate,
                        moriAuthNo,
                        mTradeNo,
                        "",
                        "",
                        mCashTarget.toString(),
                        true,
                        mCancelReason,
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
                    mCatSdk.CashRecipt(
                        mTermID,
                        mMoney.toString(),
                        mVat.toString(),
                        mSvc.toString(),
                        mTxf.toString(),
                        moriAuthDate,
                        moriAuthNo,
                        mTradeNo,
                        "",
                        mCashAuthNum,
                        mCashTarget.toString(),
                        true,
                        mCancelReason,
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
            } else {
                if (mCashAuthNum === "") {
                    if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
                        mTermID = ""
                    }
                    mCatSdk.CashRecipt(
                        mTermID,
                        mMoney.toString(),
                        mVat.toString(),
                        mSvc.toString(),
                        mTxf.toString(),
                        moriAuthDate,
                        moriAuthNo,
                        "",
                        "",
                        "",
                        mCashTarget.toString(),
                        true,
                        mCancelReason,
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
                    mCatSdk.CashRecipt(
                        mTermID,
                        mMoney.toString(),
                        mVat.toString(),
                        mSvc.toString(),
                        mTxf.toString(),
                        moriAuthDate,
                        moriAuthNo,
                        "",
                        "",
                        mCashAuthNum,
                        mCashTarget.toString(),
                        true,
                        mCancelReason,
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
            }
            return
        }

        if (mCashAuthNum == "") {
            if (Setting.getPreference(this, Constants.MULTI_STORE) == "") {
                mTermID = ""
            }
            mCatSdk.CashRecipt(
                mTermID, mMoney.toString(), mVat.toString(), mSvc.toString(), mTxf.toString(), "", "", "",
                "", "", mCashTarget.toString(), false, "", "", "", false,
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
            mCatSdk.CashRecipt(
                mTermID, mMoney.toString(), mVat.toString(), mSvc.toString(), mTxf.toString(), "", "", "",
                "", mCashAuthNum, mCashTarget.toString(), false, "", "", "", false,
                mStoreName,
                mStoreAddr,
                mStoreNumber,
                mStorePhone,
                mStoreOwner
            )
        }

        return
    }
    /** ble 취소 */
    fun CashBleCancel(){

    }
    /** serial 취소 */
    fun CashLineCancel(){

    }
    /** CAT 취소 */
    fun CashCatCancel(){

    }
    /** 직접 입력,서버 직접 취소 */
    fun CashTradeDirectCancel(){
            //서버 직접 승인
        val TrdAmt:String = (mMoney - mTxf + mVat + mSvc).toString()
        val id = ByteArray(40)
        Arrays.fill(id, 0x20.toByte())
        System.arraycopy(mCashAuthNum.toByteArray(), 0, id, 0, mCashAuthNum.length)
        var KeyYn = "K"
        var CanRea = ""
        if (mCancelReason == "1" || mCancelReason == "2" || mCancelReason == "3") {
            CanRea = reSetCancelReason(mCancelReason.toString()) + moriAuthDate + moriAuthNo
        }
        //[2]01391029[28]B200710000900220304171956000006PKA009[28]0220304130351621   [28]K[28]01027730127                             [28][28]1004[28]0[28]0[28]0[28]1[28]1[28][28][28][28][28]777130351621[3]h
        //[2]01271029[28]B200710000900220303153045000017PKA009[28]020303130261521   [28]K[28]01027730127                             [28][28]1004[28]91[28]0[28]0[28]0[28]1[28][28][28][28][28][3]a
        //[2]01271029[28]B200710000900220304174501000013PKA009[28]0220304130362453   [28]K[28]01027730127                             [28][28]1004[28]0[28]0[28]0[28]1[28]1[28][28][28][28][28][3]h
        //[2]01391029[28]B200710000900220304181617000017PKA009[28]0220304130373583   [28]K[28]01027730127                             [28][28]1004[28]0[28]0[28]0[28]1[28]1[28][28][28][28][28]777130373583[3]m
        mKocesSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, getTID(), Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRea, KeyYn, id, null,
            TrdAmt, "0", "0",  "0", mCashTarget.toString(), mCancelReason.toString(), "", "", "", "", mTradeNo, TcpInterface.DataListener { _rev ->
                val tp = CCTcpPacket(_rev)
                val code = String(tp.resData[0])
                val authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[2]) //현금영수증 승인번호
                val kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[3]) //KOCES거래고유번호
                val CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[4])
                val PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[5])
                val PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[6])
                val PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[7])
                val Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[8])
                val StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[9])
                val Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[10])
                val Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[11])
                val ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[12])
                val Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[13])
                val bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.resData[14])
                val sendData = HashMap<String, String>()
                sendData["TrdType"] = tp.responseCode
                sendData["TermID"] = tp.terminalID
                sendData["TrdDate"] = tp.date
                sendData["AnsCode"] = code
                sendData["AuNo"] = authorizationnumber
                sendData["TradeNo"] = kocesTradeUniqueNumber
                sendData["CardNo"] = CardNo
                sendData["Keydate"] = Encryptionkey_expiration_date
                sendData["MchData"] = StoreData
                sendData["CardKind"] = "" //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                sendData["OrdCd"] = "" //발급사코드
                sendData["OrdNm"] = "" //발급사명
                sendData["InpCd"] = "" //매입사코드
                sendData["InpNm"] = "" //매입사명
                sendData["DDCYn"] = "" //DDC 여부
                sendData["EDCYn"] = "" //EDC 여부
                sendData["GiftAmt"] = "" //기프트카드 잔액
                sendData["MchNo"] = "" //가맹점번호
                try {
                    sendData["Message"] = Utils.getByteToString_euc_kr(tp.resData[1])
                } catch (ex: UnsupportedEncodingException) {
                    cancelTimer()
                }
                cancelTimer()

                val mLog =
                    "전문번호 : " + tp.responseCode + "," + "단말기ID : " + tp.terminalID + "," + "거래일자 : " + tp.date + "," +
                            "응답코드 : " + code + "," + "응답메세지 : " + tp.resData[1] + "," + "원승인번호 : " + authorizationnumber + "," +
                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                            "가맹점데이터 : " + StoreData
                cout("SEND : CASHRECIPT_SERIAL", Utils.getDate("yyyyMMddHHmmss"), mLog)
                Log.d(TAG, mLog)
                if(code=="0000") {
                    mKocesSdk.setSqliteDB_InsertTradeData(
                        getTID(),
                        mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                        TradeMethod.Cash,
                        TradeMethod.Cancel,
                        if (Utils.isNullOrEmpty(Money)) 0 else Money.toInt(),
                        "",
                        if (Utils.isNullOrEmpty(Tax)) 0 else Tax.toInt(),
                        if (Utils.isNullOrEmpty(ServiceCharge)) 0 else ServiceCharge.toInt(),
                        if (Utils.isNullOrEmpty(Tax_exempt)) 0 else Tax_exempt.toInt(),
                        0,
                        when (mCashTarget) {
                            1 -> TradeMethod.CashPrivate
                            2 -> TradeMethod.CashBusiness
                            3 -> TradeMethod.CashSelf
                            else -> ""
                        },
                        TradeMethod.CashDirect,
                        CardNo,
                        "",
                        "",
                        "",
                        "",
                        "",
                        tp.date,
                        moriAuthDate,
                        authorizationnumber,
                        moriAuthNo,
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
                    //여기서 영수증 화면으로 보낸다.
                    intent = Intent(this@CashLoadingActivity, ReceiptCashActivity::class.java)
                    intent.putExtra("data", "last")
                    cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
                    startActivity(intent)
                }else{
                    sendData["Message"]?.let { ShowToastBox(it) };
//                    Toast.makeText(this@CashLoadingActivity,sendData["Message"],Toast.LENGTH_SHORT).show()
                    cancelTimer()
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)

                }
                //SendreturnData(RETURN_OK, sendData, null)

            })
    }
    fun getTID(): String {
        return Setting.getPreference(mKocesSdk.activity, Constants.TID)
    }

    fun ShowToastBox(_str: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(mKocesSdk.activity, _str, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 카드리더기or멀티리더기가 정상적으로 앱에 셋팅되어 있는지 체크한다
     * @param _DeviceType
     * @return
     */
    private fun CheckDeviceState(_DeviceType: Int): Boolean {
        val tmp = false
        if (mKocesSdk.getUsbDevice().size > 0 && mKocesSdk.getICReaderAddr() != "" && mKocesSdk.CheckConnectedUsbSerialState(mKocesSdk.getICReaderAddr())) {
            return true
        }
        return if (mKocesSdk.getUsbDevice().size > 0 && mKocesSdk.getMultiReaderAddr() != "" && mKocesSdk.CheckConnectedUsbSerialState(mKocesSdk.getMultiReaderAddr())) {
            true
        } else tmp
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

     /**
     * App to App 현금영수증 취소 시 필수 '1' : 거래취소, '2' : 오류발급, '3' : 기타
     * @param _str
     * @return
     */
    private fun reSetCancelReason(_str: String): String? {
        if (_str == "1") {
            return "0"
        } else if (_str == "2") {
            return "0"
        } else if (_str == "3") {
            return "0"
        }
        return "0"
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
                    if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                    {
                        intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                        intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                        intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                        intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                        intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                        intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                    }
                    else
                    {
                        intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                    }
                    startActivity(intent)
                    return
                }
            }
            else
            {
                ShowToastBox(_Message)
                cancelTimer()
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }
            //여기서 영수증 화면으로 보낸다.
            if (_hashMap.get("AnsCode") == "0000")
            {
                intent = Intent(this@CashLoadingActivity, ReceiptCashActivity::class.java)
                intent.putExtra("TermID", _hashMap.get("TermID") as String)
                intent.putExtra("data", "last")
                cancelTimer()   //이걸 사용하지 않으면 타이머가 살아 있어서 작동한다.
                startActivity(intent)
            }
            else
            {
                ShowToastBox(_hashMap.get("Message") as String)
                cancelTimer()
                if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
                {
                    intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                    intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                    intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                    intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                    intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                    intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                }
                else
                {
                    intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
                }
                startActivity(intent)
                return
            }

        } else if (_state == 1) {
            ShowToastBox(_Message);
            cancelTimer()
            if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
            }
            else
            {
                intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
            }
            startActivity(intent)
        } else {
            ShowToastBox(_Message);
            cancelTimer()
            if (mTrdType == "B10")  //ㄱ결제페이지에서 넘어왔다면 결제페이지로 이동 취소페이지에서 넘어왔다면 메인페에지로 이동
            {
                intent = Intent(this@CashLoadingActivity,CashActivity2::class.java)
                intent.putExtra("EdtMoney", mEdtMoney)              //입력금액
                intent.putExtra("EdtTxf", mEdtTxf)                //비과세
                intent.putExtra("EdtSvc", mEdtSvc)                //봉사료
                intent.putExtra("CashTarget",mCashTarget)             //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
                intent.putExtra("Auth",intent.getStringExtra("Auth").toString()  ?:"")       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
            }
            else
            {
                intent = Intent(this@CashLoadingActivity,Main2Activity::class.java)
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

    @Synchronized
    private fun CallBackReciptResult(result: String, Code: String, resultData: HashMap<String, String>)
    {
        if (Code == "COMPLETE")    /* 현금영수증정상콜백 */
        {
            val sendData = HashMap<String, String?>()
            sendData["TrdType"] = resultData["TrdType"]
            sendData["TermID"] = resultData["TermID"]
            sendData["TrdDate"] = resultData["date"]
            if (resultData["date"] == null) {
                sendData["TrdDate"] = resultData["TrdDate"]
            }
            sendData["AnsCode"] = resultData["AnsCode"]
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
            sendData["OrdNm"] = resultData["OrdNm"] //발급사명
            sendData["InpCd"] = resultData["InpCd"] //매입사코드
            sendData["InpNm"] = resultData["InpNm"] //매입사명
            sendData["DDCYn"] = resultData["DDCYn"] //DDC 여부
            sendData["EDCYn"] = resultData["EDCYn"] //EDC 여부
            sendData["GiftAmt"] = resultData["GiftAmt"] //기프트카드 잔액
            sendData["MchNo"] = resultData["MchNo"] //가맹점번호
            sendData["Message"] = resultData["Message"]
            val mLog =
                "거래일자 : " + resultData["date"] + "," + "응답코드 : " + resultData["AnsCode"] + "," + "응답메세지 : " + resultData["Message"] + "," +
                        "승인번호 : " + resultData["AuNo"] + "," + "거래고유번호 : " + resultData["TradeNo"] + "," + "출력용카드번호 : " + DesCardNumber + "," +
                        "암호키만료잔여일 : " + resultData["Keydate"] + "," + "가맹점데이터 : " + resultData["MchData"]
            cout("SEND : CASHRECIPT", Utils.getDate("yyyyMMddHHmmss"), mLog)
            SendreturnData(RETURN_OK, sendData, "")
        } else {
            SendreturnData(RETURN_CANCEL, null, result)
        }

    }

}