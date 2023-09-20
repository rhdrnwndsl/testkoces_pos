package com.koces.androidpos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.Setting.PayDeviceType
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.van.Constants

class EasyPayActivity : BaseActivity() {
    val TAG: String = EasyPayActivity::class.java.simpleName

    /** 사인패드의 결과 값이 들어올 경우 이를 리시브 하기 위한 키  */
    private val REQUEST_SIGNPAD = 10001

    /** QR스캔카메라연동  */
    lateinit var intentIntegrator: IntentIntegrator

    /** 입력필드 선언 */
    lateinit var mEdtMoney: EditText
    lateinit var mEdtSvc: EditText
    lateinit var mEdtTxf: EditText
    lateinit var mEdtInstallMonth: EditText  //할부 개월

    /** 텍스트박스 선언 */
    lateinit var mTvwMoney: TextView
    lateinit var mTvwVat: TextView
    lateinit var mTvwSvc: TextView
    lateinit var mTvwTotalMoney: TextView

    /** 버튼 선언 */
    lateinit var mBtnEasyPay: Button
    lateinit var mBtnInstall: Button
    lateinit var mBtnNoInstall: Button

    /** 리니어레이아웃 */
    lateinit var mLinearTxf: LinearLayout        //비과세 입력 LinearLayout
    lateinit var mLinearSvcAuto: LinearLayout    //봉사료 표시 LinearLayout
    lateinit var mLinearSvcManual: LinearLayout  //봉사료 입력 LinearLayout
    lateinit var mLinearMoney: LinearLayout      //금액 입력 LinearLayout
    lateinit var mLinearInstallment: LinearLayout //할부 입력 LinearLayout

    var mUseInstall: Boolean = false         //할부 사용


    val mTaxSdk: TaxSdk = TaxSdk.getInstance()
    var mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()
    //sdk
    lateinit var mPaySdk: PaymentSdk
    lateinit var mCatSdk: CatPaymentSdk

    var mQrNo: String = ""
    var mEasyKind: String = ""

    var mTid:String = ""    //복수가맹점일 때 사용
    var mStoreAddr:String = "" //가맹점주소
    var mStoreName:String = "" //가맹점명
    var mStoreOwner:String = "" //대표자명
    var mStorePhone:String = "" //연락처
    var mStoreNumber:String = "" //사업자번호

    /** 복수가맹점일 때 TID 선택DIalog */
    lateinit var mTidDialog: TermIDSelectDialog

    /** 연속클릭 방지 시간 */
    private var mLastClickTime = 0L

    /** 금액계산 */
    private var taxvalue: java.util.HashMap<String, Int> = java.util.HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_pay)
        initRes()
        Setting.setIsAppForeGround(1);
    }

    override fun onDestroy() {
        super.onDestroy()
        mKocesSdk.BleUnregisterReceiver(this)
        mKocesSdk.BleregisterReceiver(Setting.getTopContext())
    }

    fun initRes() {

        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this)
        mKocesSdk.BleregisterReceiver(this)
        mEdtMoney = findViewById(R.id.easy_edt_money)
        mEdtSvc = findViewById(R.id.easy_edt_svc)
        mEdtTxf = findViewById(R.id.easy_edt_txf)
        mEdtMoney.addTextChangedListener( NumberTextWatcher(mEdtMoney));
        mEdtSvc.addTextChangedListener( NumberTextWatcher(mEdtSvc));
        mEdtTxf.addTextChangedListener( NumberTextWatcher(mEdtTxf));

        mTvwTotalMoney = findViewById(R.id.easy_tvw_totalmoney)
        mTvwTotalMoney.text = ""
        mTvwMoney = findViewById(R.id.easy_tvw_money)
        mTvwMoney.text=""
        mTvwVat = findViewById(R.id.easy_tvw_vat)
        mTvwVat.text = ""
        mTvwSvc = findViewById(R.id.easy_tvw_svc)
        mTvwSvc.text  = ""
        mEdtSvc.setText("")
        mEdtTxf.setText("")
        mTvwTotalMoney.addTextChangedListener( NumberTextWatcher(mTvwTotalMoney));
        mTvwMoney.addTextChangedListener( NumberTextWatcher(mTvwMoney));
        mTvwVat.addTextChangedListener( NumberTextWatcher(mTvwVat));
        mTvwSvc.addTextChangedListener( NumberTextWatcher(mTvwSvc));

        mEdtInstallMonth = findViewById(R.id.easy_edt_installment)
        mEdtInstallMonth.setText("")
        //linearlayout 설정
        mLinearSvcAuto = findViewById(R.id.easy_linear_svc_auto)
        mLinearSvcManual = findViewById(R.id.easy_linear_svc_manual)
        mLinearTxf = findViewById(R.id.easy_linear_txf)
        mLinearMoney = findViewById(R.id.easy_linear_money)

        mTaxSdk.readTaxSettingDB(Setting.getPreference(mKocesSdk.activity,Constants.TID))

        //봉사료를 사용하지 않는 경우 봉사료 항목을 숨긴다.
        if (!mTaxSdk.useSVC) {
            mLinearSvcAuto.visibility = View.GONE
            mLinearSvcManual.visibility = View.GONE
        }
        else
        {
            //봉사료 수동 입력의 경우
            if (mTaxSdk.svcMode == 1) {
                mLinearSvcAuto.visibility = View.GONE
                mLinearSvcManual.visibility = View.VISIBLE

            }
            else{
                mLinearSvcAuto.visibility = View.VISIBLE
                mLinearSvcManual.visibility = View.GONE
            }
        }

        mLinearTxf.visibility = when (mTaxSdk.useVAT) {
            true -> when (mTaxSdk.vatMode) {
                0 -> View.GONE  //부가세 자동인 경우 부가세 입력 필드를 표시 하지 않는다.
                1 -> View.VISIBLE //부가세가 통합인 경우에는 부가세 입력 필드를 표시 한다.
                else -> View.INVISIBLE
            }
            false -> View.VISIBLE
        }

        mLinearMoney.visibility = when (mTaxSdk.useVAT) {
            true -> View.VISIBLE
            false -> View.GONE
        }

        //버튼 설정
        mBtnInstall = findViewById(R.id.easy_btn_install)
        mBtnNoInstall = findViewById(R.id.easy_btn_no_install)
        mBtnInstall.setOnClickListener(InstallmentButtonListener())
        mBtnNoInstall.setOnClickListener(InstallmentButtonListener())

        mLinearInstallment = findViewById(R.id.easy_linear_installment)
        mLinearInstallment.visibility = View.GONE
        //결제
        mBtnEasyPay = findViewById(R.id.easy_btn_pay)

        //하단 버튼 처리
        mBtnEasyPay.setOnClickListener(PayButtonListener())

        //입력금액애 따라서 실시간으로 세금을 표시 한다.
        mEdtMoney.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                ChangeValue()
            }

        })
        mEdtSvc.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                ChangeValue()
            }
        })

        mEdtTxf.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                ChangeValue()
            }
        })


        mKocesSdk.BleConnectionListener(ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mKocesSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
//                ReadyDialogHide()
                Handler(Looper.getMainLooper()).post {
                    if (Setting.getBleName() == Setting.getPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME
                        )
                    ) {
                        BleDeviceInfo()
                    } else {
                        Setting.setPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME,
                            Setting.getBleName()
                        )
                        Setting.setPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_ADDR,
                            Setting.getBleAddr()
                        )
                        setBleInitializeStep()
                    }
                }
            } else {
                ReadyDialogHide();
            }
        })

        mKocesSdk.BleWoosimConnectionListener(bleWoosimInterface.ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mKocesSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
//                ReadyDialogHide()
                Handler(Looper.getMainLooper()).post {
                    if (Setting.getBleName() == Setting.getPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME
                        )
                    ) {
                        BleDeviceInfo()
                    } else {
                        Setting.setPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME,
                            Setting.getBleName()
                        )
                        Setting.setPreference(
                            mKocesSdk.getActivity(),
                            Constants.BLE_DEVICE_ADDR,
                            Setting.getBleAddr()
                        )
                        setBleInitializeStep()
                    }
                }
            } else {
                ReadyDialogHide();
            }
        })

        //어플 최소화
        val btnMinimum = findViewById<Button>(R.id.easy_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@EasyPayActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.easy_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent =
                Intent(this@EasyPayActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.easy_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@EasyPayActivity, menu2Activity::class.java)
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //만일 결제페이지(결제로딩페이지) 에서 취소되어 다시 이곳으로 넘어온 경우 금액셋팅을 그대로 돌린다
        if(intent.hasExtra("EdtMoney"))
        {
            mEdtMoney.setText(intent.getStringExtra("EdtMoney").toString());
            mEdtTxf.setText(intent.getStringExtra("EdtTxf").toString());
            mEdtSvc.setText(intent.getStringExtra("EdtSvc").toString())

            mEdtInstallMonth.setText((intent.getIntExtra("Month",0)).toString())

            if  (intent.getIntExtra("Month",0) == 0 )
            {
                mLinearInstallment.visibility = View.GONE
            }
            ChangeValue()

        }
        else
        {
            mEdtSvc.setText("")
            mEdtTxf.setText("");
            mTvwTotalMoney.setText("")
            mTvwMoney.setText("");
            mTvwVat.setText("");
            mTvwSvc.setText("")
            mEdtInstallMonth.setText("")
            mLinearInstallment.visibility = View.GONE
        }
    }

    inner class InstallmentButtonListener : View.OnClickListener {
        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.easy_btn_no_install -> {
                    segmentBtnSetting(0)
                }
                R.id.easy_btn_install -> {
                    segmentBtnSetting(1)
                }
                else -> {
                    segmentBtnSetting(0)
                }
            }
        }
    }

    var _bleCount = 0
    private fun setBleInitializeStep() {
        ReadyDialogShow(Setting.getTopContext(), "무결성 검증 중 입니다.", 0)
        _bleCount += 1
        /* 무결성 검증. 초기화진행 */
        val deviceSecuritySDK = DeviceSecuritySDK(this) { result: String, Code: String?, state: String?, resultData: java.util.HashMap<String?, String?>? ->
            if (result == "00") {
                mKocesSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1) //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
                //                ReadyDialogHide();
            }

            else {
                mKocesSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1)
                //                ReadyDialogHide();
            }

            Handler().postDelayed({
                if (result == "00") {
                    Toast.makeText(
                        mKocesSdk.getActivity(),
                        "무결성 검증에 성공하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (result == "9999") {

                    if (_bleCount > 1) {
                        _bleCount = 0
                        Toast.makeText(
                            mKocesSdk.activity,
                            "네트워크 오류. 다시 시도해 주세요",
                            Toast.LENGTH_SHORT
                        ).show()

                        mKocesSdk.BleDisConnect()

                    } else {
                        mKocesSdk.BleDisConnect()
                        Handler().postDelayed({
                         ReadyDialogShow(Setting.getTopContext(),"무결성 검증 중 입니다.",0)
                        }, 200)
                        Handler().postDelayed({
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                            mKocesSdk.BleConnect(
                                mKocesSdk.activity,
                                Setting.getPreference(
                                    mKocesSdk.activity,
                                    Constants.BLE_DEVICE_ADDR
                                ),
                                Setting.getPreference(
                                    mKocesSdk.activity,
                                    Constants.BLE_DEVICE_NAME
                                )
                            )
                        }, 500)
                        return@postDelayed
                    }
                }
                else {
                    Toast.makeText(
                        mKocesSdk.getActivity(),
                        "무결성 검증에 실패하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, 200)

            if (result == "9999") {
                return@DeviceSecuritySDK
            }
            Handler().postDelayed({
                //장치 정보 요청
                Toast.makeText(mKocesSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                BleDeviceInfo()
            }, 500)
        }
        deviceSecuritySDK.Req_BLEIntegrity() /* 무결성 검증. 키갱신 요청 */
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


    fun segmentBtnSetting(index: Int) {
        mUseInstall = if (index == 0) false else true
        when (mUseInstall) {
            false -> {
                mBtnNoInstall.setBackgroundResource(R.drawable.segmentbtnleft_normal)
                mBtnInstall.setBackgroundResource(R.drawable.segmentbtnright_out)
                mBtnNoInstall.setTextColor(Color.parseColor("#FFFFFF"))
                mBtnInstall.setTextColor(Color.parseColor("#0f74fa"))
                mLinearInstallment.visibility = View.GONE
            }
            true -> {
                mBtnNoInstall.setBackgroundResource(R.drawable.segmentbtnleft_out)
                mBtnInstall.setBackgroundResource(R.drawable.segmentbtnright_normal)
                mBtnNoInstall.setTextColor(Color.parseColor("#0f74fa"))
                mBtnInstall.setTextColor(Color.parseColor("#FFFFFF"))
                mLinearInstallment.visibility = View.VISIBLE
            }

        }

    }

    fun QrScan()
    {
        intentIntegrator = IntentIntegrator(this)
        intentIntegrator.setOrientationLocked(false) //세로
        intentIntegrator.setTimeout(30 * 1000)
        intentIntegrator.setPrompt("")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.initiateScan()
        return
    }

    fun CatQrReader()
    {
//금액 계산
        var taxvalue: HashMap<String, Int> = HashMap()
        if (mEdtMoney.text.toString().isNotEmpty() && mEdtSvc.text.toString()
                .isNotEmpty() && mEdtTxf.text.toString().isNotEmpty()
        ) {
            val tmpMoney: Int = mEdtMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt()
            val tmpSvc: Int = mEdtSvc.text.toString().replace("[^0-9]".toRegex(), "").toInt()
            val tmpTxf: Int = mEdtTxf.text.toString().replace("[^0-9]".toRegex(), "").toInt()
            if (tmpMoney > 9) {    //결제 최소 금액은 10원 이상 2022-02-15 kim.jy
                taxvalue = mTaxSdk.TaxCalc(tmpMoney, tmpSvc, tmpTxf, true)
                val conMoney = taxvalue.get("Money")
                val conVAT = taxvalue.get("VAT")
                val conSVC = taxvalue.get("SVC")

                mTvwMoney.text = conMoney.toString()
                mTvwVat.text = conVAT.toString()
                mTvwSvc.text = conSVC.toString()
            }

        }

        //로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@EasyPayActivity, EasyLoadingActivity::class.java)
        intent.putExtra("TrdType","A10")                     //K21, K22, "G150" -> 캣에서 간편결제
        intent.putExtra("AuDate", "")                           //원거래일자 YYMMDD
        intent.putExtra("AuNo", "")                             //원승인번호
        intent.putExtra("TrdAmt", taxvalue.get("Money"))              //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", taxvalue.get("VAT"))                //세금
        intent.putExtra("SvcAmt", taxvalue.get("SVC"))                //봉사료
        intent.putExtra("TaxFreeAmt", taxvalue.get("TXF"))            //비과세
        if (mUseInstall)
        {
            intent.putExtra("Month",  mEdtInstallMonth.text.toString())    //할부
        }
        else
        {
            intent.putExtra("Month", "0")    //할부
        }
        intent.putExtra("TradeNo", "")                          //Koces거래고유번호(거래고유키 취소 시 사용)
        intent.putExtra("DscYn", "1")                           //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn("1")
        intent.putExtra("FBYn", "0")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        intent.putExtra("QrNo", "")            //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        intent.putExtra("EasyKind", "UN")  //간편결제 거래종류(카카오, 제로, 앱카드 등)
        intent.putExtra("SearchNumber", "")            //조회번호

        /** CAT_APP_CARD 시 추가로 필요한 사항 */
        intent.putExtra("Cancel", false)    //승인인지 취소인지
        intent.putExtra("Extra", "")    //여유필드

        intent.putExtra("TermID", mTid)
        intent.putExtra("StoreName", mStoreName)
        intent.putExtra("StoreNumber", mStoreNumber)
        intent.putExtra("StoreAddr", mStoreAddr)
        intent.putExtra("StorePhone", mStorePhone)
        intent.putExtra("StoreOwner", mStoreOwner)

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        startActivity(intent)
    }

    fun LineQrReader()
    {
        /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
        mPaySdk = PaymentSdk(1, false) { result: String?, Code: String, resultData: java.util.HashMap<String, String> ->
            if (Code == "SHOW") {
                if (result != null) {
                    ShowToastBox(result)
                }
                else
                {
                    ShowToastBox("서버통신 오류")

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

        ReadyDialogShow(this, "바코드/QR코드 읽혀주세요",
            Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)), true)
        mPaySdk.BarcodeReader(this,"간편결제","금액: " + mTvwTotalMoney.text.toString() + "원","바코드/QR 코드를 읽혀주세요")

    }

    @Synchronized
    private fun CallBackReciptResult(result: String, Code: String, resultData: java.util.HashMap<String, String>)
    {
        if (Code == "COMPLETE_EASY") /* 신용거래정상콜백 */ {
            val sendData = java.util.HashMap<String, String?>()
            mQrNo = resultData["Qr"].toString()

            mEasyKind = Scan_Data_Parser(mQrNo)
            EasyPay(mTid)

        } else {
            ShowToastBox(result)
        }
    }

    fun EasyPay(_tid:String) {
        Log.d(TAG, "신용카드 결제 시작 함수")

        //금액 계산
        ChangeValue()
//        var taxvalue: HashMap<String, Int> = HashMap()
//        if (mEdtMoney.text.toString().isNotEmpty() && mEdtSvc.text.toString()
//                .isNotEmpty() && mEdtTxf.text.toString().isNotEmpty()
//        ) {
//            val tmpMoney: Int = mEdtMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt()
//            val tmpSvc: Int = mEdtSvc.text.toString().replace("[^0-9]".toRegex(), "").toInt()
//            val tmpTxf: Int = mEdtTxf.text.toString().replace("[^0-9]".toRegex(), "").toInt()
//            if (tmpMoney > 9) {    //결제 최소 금액은 10원 이상 2022-02-15 kim.jy
//                taxvalue = mTaxSdk.TaxCalc(tmpMoney, tmpSvc, tmpTxf, true)
//                val conMoney = taxvalue.get("Money")
//                val conVAT = taxvalue.get("VAT")
//                val conSVC = taxvalue.get("SVC")
//                val conTXF = taxvalue.get("TXF")
//
//                mTvwMoney.text = conMoney.toString()
//                mTvwVat.text = conVAT.toString()
//                mTvwSvc.text = conSVC.toString()
//            }
//
//        }

        //로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@EasyPayActivity, EasyLoadingActivity::class.java)
        intent.putExtra("TrdType","K21")                     //K21, K22
        intent.putExtra("TermID", _tid)
        intent.putExtra("AuDate", "")                           //원거래일자 YYMMDD
        intent.putExtra("AuNo", "")                             //원승인번호
        intent.putExtra("TrdAmt", taxvalue.get("Money"))              //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", taxvalue.get("VAT"))                //세금
        intent.putExtra("SvcAmt", taxvalue.get("SVC"))                //봉사료
        intent.putExtra("TaxFreeAmt", taxvalue.get("TXF"))            //비과세
        if (mUseInstall)
        {
            intent.putExtra("Month",  mEdtInstallMonth.text.toString())    //할부
        }
        else
        {
            intent.putExtra("Month", "0")    //할부
        }
//        intent.putExtra("MchData", "")                          //가맹점데이터
//        intent.putExtra("TrdCode", "")                          //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        intent.putExtra("TradeNo", "")                          //Koces거래고유번호(거래고유키 취소 시 사용)
//        intent.putExtra("CompCode", "")                         //업체코드(koces에서 부여한 업체코드)
//        intent.putExtra("DgTmout", "")                          //카드입력대기시간 10~99, 거래요청 후 카드입력 대기시간
        intent.putExtra("DscYn", "1")                           //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn("1")
//        intent.putExtra("DscData", "")                          //위의 dscyn 이 2 혹은 3일 경우 아래로 데이터를 체크하여 서명에 실어서 보낸다
        intent.putExtra("FBYn", "0")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        intent.putExtra("QrNo", mQrNo)            //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        intent.putExtra("SearchNumber", "")            //조회번호
        intent.putExtra("EasyKind", mEasyKind)  //간편결제 거래종류(카카오, 제로, 앱카드 등)

        intent.putExtra("StoreName", mStoreName)
        intent.putExtra("StoreNumber", mStoreNumber)
        intent.putExtra("StoreAddr", mStoreAddr)
        intent.putExtra("StorePhone", mStorePhone)
        intent.putExtra("StoreOwner", mStoreOwner)

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))
        startActivity(intent)
    }

    fun CatAppCard()
    {
        //금액 계산
        ChangeValue()
//        var taxvalue: HashMap<String, Int> = HashMap()
//        if (mEdtMoney.text.toString().isNotEmpty() && mEdtSvc.text.toString()
//                .isNotEmpty() && mEdtTxf.text.toString().isNotEmpty()
//        ) {
//            val tmpMoney: Int = mEdtMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt()
//            val tmpSvc: Int = mEdtSvc.text.toString().replace("[^0-9]".toRegex(), "").toInt()
//            val tmpTxf: Int = mEdtTxf.text.toString().replace("[^0-9]".toRegex(), "").toInt()
//            if (tmpMoney > 9) {    //결제 최소 금액은 10원 이상 2022-02-15 kim.jy
//                taxvalue = mTaxSdk.TaxCalc(tmpMoney, tmpSvc, tmpTxf, true)
//                val conMoney = taxvalue.get("Money")
//                val conVAT = taxvalue.get("VAT")
//                val conSVC = taxvalue.get("SVC")
//
//                mTvwMoney.text = conMoney.toString()
//                mTvwVat.text = conVAT.toString()
//                mTvwSvc.text = conSVC.toString()
//            }
//
//        }

        //로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@EasyPayActivity, EasyLoadingActivity::class.java)
        intent.putExtra("TrdType","A10")                     //K21, K22, "G150" -> 캣에서 간편결제
        intent.putExtra("AuDate", "")                           //원거래일자 YYMMDD
        intent.putExtra("AuNo", "")                             //원승인번호
        intent.putExtra("TrdAmt", taxvalue.get("Money"))              //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", taxvalue.get("VAT"))                //세금
        intent.putExtra("SvcAmt", taxvalue.get("SVC"))                //봉사료
        intent.putExtra("TaxFreeAmt", taxvalue.get("TXF"))            //비과세
        if (mUseInstall)
        {
            intent.putExtra("Month",  mEdtInstallMonth.text.toString())    //할부
        }
        else
        {
            intent.putExtra("Month", "0")    //할부
        }
        intent.putExtra("TradeNo", "")                          //Koces거래고유번호(거래고유키 취소 시 사용)
        intent.putExtra("DscYn", "1")                           //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn("1")
        intent.putExtra("FBYn", "0")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        intent.putExtra("QrNo", mQrNo)            //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        intent.putExtra("EasyKind", mEasyKind)  //간편결제 거래종류(카카오, 제로, 앱카드 등)
        intent.putExtra("SearchNumber", "")            //조회번호

        /** CAT_APP_CARD 시 추가로 필요한 사항 */
        intent.putExtra("Cancel", false)    //승인인지 취소인지
        intent.putExtra("Extra", "")    //여유필드

        intent.putExtra("TermID", mTid)
        intent.putExtra("StoreName", mStoreName)
        intent.putExtra("StoreNumber", mStoreNumber)
        intent.putExtra("StoreAddr", mStoreAddr)
        intent.putExtra("StorePhone", mStorePhone)
        intent.putExtra("StoreOwner", mStoreOwner)

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        startActivity(intent)
    }

    inner class PayButtonListener : View.OnClickListener {
        override fun onClick(v: View?) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 3000) {

                return
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            //TODO 2022.02.10 kim.jy 추가 작업 필요

            if (checkValue()) {

            } else {
                return
            }
//            when(checkValue()){
//                false->return
//            }

            mStoreName = Setting.getPreference(this@EasyPayActivity,Constants.STORE_NM);
            mStoreAddr = Setting.getPreference(this@EasyPayActivity,Constants.STORE_ADDR);
            mStorePhone = Setting.getPreference(this@EasyPayActivity,Constants.STORE_PHONE);
            mStoreNumber = Setting.getPreference(this@EasyPayActivity,Constants.STORE_NO);
            mStoreOwner = Setting.getPreference(this@EasyPayActivity,Constants.OWNER_NM);

            if (Setting.getPreference(mKocesSdk.activity,Constants.MULTI_STORE) != "")
            {

                mTidDialog = TermIDSelectDialog(this@EasyPayActivity, false, object:TermIDSelectDialog.DialogBoxListener {
                    override fun onClickCancel(_msg: String) {
                        ShowDialog(_msg)
                    }

                    override fun onClickConfirm(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String) {
                        mTid = _tid.replace(" ","")
                        mStoreName = _storeName.replace(" ","")
                        mStoreAddr = _storeAddr.replace(" ","")
                        mStorePhone = _storePhone.replace(" ","")
                        mStoreNumber = _storeNumber.replace(" ","")
                        mStoreOwner = _storeOwner.replace(" ","")
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            if(Setting.getPreference(this@EasyPayActivity,Constants.LINE_QR_READER) == Constants.LineQrReader.Camera.toString())
                            {
                                QrScan()
                            }
                            else
                            {
                                LineQrReader()
                            }
                        } else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                            QrScan()
                        } else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            if(Setting.getPreference(this@EasyPayActivity,Constants.CAT_QR_READER) == Constants.CatQrReader.Camera.toString())
                            {
                                QrScan()
                            }
                            else
                            {
                                CatQrReader()
                            }
                        }


                    }
                })
                mTidDialog.show()
            }
            else
            {
                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                    if(Setting.getPreference(this@EasyPayActivity,Constants.LINE_QR_READER) == Constants.LineQrReader.Camera.toString())
                    {
                        QrScan()
                    }
                    else
                    {
                        LineQrReader()
                    }
                } else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                    QrScan()
                } else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                    if(Setting.getPreference(this@EasyPayActivity,Constants.CAT_QR_READER) == Constants.CatQrReader.Camera.toString())
                    {
                        QrScan()
                    }
                    else
                    {
                        CatQrReader()
                    }
                }
            }

        }
    }

    fun ChangeValue(){

        if(mEdtMoney.text.toString().isEmpty())
        {
            mEdtMoney.setText("0")
        }
        if(mEdtSvc.text.toString().isEmpty())
        {
            mEdtSvc.setText("0")
        }
        if(mEdtTxf.text.toString().isEmpty())
        {
            mEdtTxf.setText("0")
        }

        if(mEdtMoney.text.toString().isNotEmpty() && mEdtSvc.text.toString().isNotEmpty() && mEdtTxf.text.toString().isNotEmpty()){
            val tmpMoney:Int = mEdtMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt()
            val tmpSvc:Int = mEdtSvc.text.toString().replace("[^0-9]".toRegex(), "").toInt()
            val tmpTxf:Int = mEdtTxf.text.toString().replace("[^0-9]".toRegex(), "").toInt()

            if(mTaxSdk.useVAT) {
                if(tmpMoney + tmpTxf < 10)
                {
                    mTvwMoney.text = "0"
                    mTvwSvc.text = "0"
                    mTvwVat.text = "0"
                    mTvwTotalMoney.text = "0"
                    return
                }
            } else {
                if(tmpTxf < 10)
                {
                    mTvwMoney.text = "0"
                    mTvwSvc.text = "0"
                    mTvwVat.text = "0"
                    mTvwTotalMoney.text = "0"
                    return
                }
            }
            val _deviceCheck = if (Setting.g_PayDeviceType === Setting.PayDeviceType.CAT) false else true
            taxvalue  = mTaxSdk.TaxCalc(tmpMoney, tmpTxf, tmpSvc,_deviceCheck)
            val conMoney = taxvalue.get("Money")
            val conVAT = taxvalue.get("VAT")
            val conSVC = taxvalue.get("SVC")
            var conTXF = taxvalue.get("TXF")

            mTvwMoney.text = conMoney.toString()
            mTvwVat.text = conVAT.toString()
            mTvwSvc.text = conSVC.toString()
            mTvwTotalMoney.text = (conMoney!! + conVAT!! + conSVC!!).toString()
            when(Setting.g_PayDeviceType){
                Setting.PayDeviceType.NONE -> {

                }
                Setting.PayDeviceType.BLE -> {

                }
                Setting.PayDeviceType.CAT -> {
                    mTvwTotalMoney.text = (conMoney!! + conVAT!! + conSVC!! + conTXF!!).toString()
                }
                Setting.PayDeviceType.LINES ->{

                }
                else->{ //else 경우는 없음

                }
            }

            /** 화면상에 표시되는 공급가액 금액을 CAT 처럼 맞추기 위해 일부러 한번 더 한다 */
            val taxvalue2:HashMap<String,Int>  = mTaxSdk.TaxCalc(tmpMoney,tmpTxf,tmpSvc,false)
            val conMoney2 = taxvalue2.get("Money")
            val conVAT2 = taxvalue2.get("VAT")
            val conSVC2 = taxvalue2.get("SVC")
            var conTXF2 = taxvalue2.get("TXF")

            mTvwMoney.text = conMoney2.toString()
        }
    }

    fun checkValue():Boolean {

        //할부 개월 수 체크부
        if (mUseInstall) {
            //할부 개월 수 체크
            val bArray = mEdtInstallMonth.text.toString().toByteArray()

            for (byte in bArray) {
                if(byte < 0x30 || byte > 0x39) {
                    ShowDialog("할부 개월 수는 숫자만 입력 가능 합니다.")
                    return false
                }
            }

            if (bArray.isEmpty()) {
                ShowDialog("할부는 최소 2개월 이상입니다")
                return false
            }

            if(mEdtInstallMonth.text.toString().toInt()<2 || mEdtInstallMonth.text.toString().toInt()>24){
                ShowDialog("할부는 최소 2개월 또는 최대 24개월 이하 입니다")
                return false
            }
        }


        when (Setting.g_PayDeviceType) {
            Setting.PayDeviceType.NONE -> {
                ShowDialog("환경설정에 장치 설정을 해야 합니다.")
                return false
            }
            Setting.PayDeviceType.BLE -> {}
            Setting.PayDeviceType.CAT -> return true
            Setting.PayDeviceType.LINES -> {}
            else -> {}
        }


        if(getTid()==""){
            ShowDialog("가맹점 등록 정보가 없습니다")
            return false
        }

        return true
    }

    fun ShowDialog(_str: String) {
        Toast.makeText(this,_str, Toast.LENGTH_SHORT).show()
    }

    fun getTid():String{
        return Setting.getPreference(mKocesSdk.activity, Constants.TID);
    }
    fun getStoreName():String{
        return Setting.getPreference(mKocesSdk.activity,Constants.STORE_NM);
    }
    fun getStoreNumber():String{
        return Setting.getPreference(mKocesSdk.activity,Constants.STORE_NO);
    }
    fun getStoreAddr():String{
        return Setting.getPreference(mKocesSdk.activity,Constants.STORE_ADDR);
    }
    fun getStorePhone():String{
        return Setting.getPreference(mKocesSdk.activity,Constants.STORE_PHONE);
    }
    fun getStoreOwner():String{
        return Setting.getPreference(mKocesSdk.activity,Constants.OWNER_NM);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SIGNPAD) {
            if (result != null) {
                if (result.contents == null || result.contents == "") {
                    ShowDialog("바코드 스캔이 처리되지 않았습니다")
                    return
                }
                mQrNo = result.contents
                mEasyKind = Scan_Data_Parser(mQrNo)
                when(Setting.g_PayDeviceType){
                    PayDeviceType.NONE -> {
                        //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                        ShowDialog("환경설정에서 장치설정을 해주십시오")
                        return
                    }
                    PayDeviceType.BLE -> {

                    }
                    PayDeviceType.CAT -> {
//                        ShowDialog("CAT 앱카드결제는 준비중입니다")
                        CatAppCard()
                        return
                    }
                    PayDeviceType.LINES ->{

                    }
                    else->{ //else 경우는 없음
                        //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                        ShowDialog("환경설정에서 장치설정을 해주십시오")
                        return
                    }
                }
                EasyPay(mTid)
            } else {
                ShowDialog("바코드 스캔이 처리되지 않았습니다")
                return
            }
            return
        }

        ShowDialog("바코드 스캔이 처리되지 않았습니다")
        return

    }

    /**
     * 스캔한 데이터를 파싱해서 제로/카카오/emv 등을 구분한다
     */
    private fun Scan_Data_Parser(_scan: String): String
    {
        var returnData = ""
        if (_scan.substring(0, 7) == "hQVDUFY") {
            //EMV QR
            returnData = "AP"
        } else if (_scan.substring(0, 6) == "281006") {
            //카카오페이
            returnData = "KP"
        } else if (_scan.substring(0, 6) == "800088") {
            //제로페이 Barcode
            returnData = "ZP"
        } else if (_scan.substring(0, 2) == "3-") {
            //제로페이 QRcode
            returnData = "ZP"
        } else {
            if (_scan.substring(0, 2) == "11" || _scan.substring(0, 2) == "12" ||
                _scan.substring(0, 2) == "13" || _scan.substring(0, 2) == "14" ||
                _scan.substring(0, 2) == "15" || _scan.substring(0, 2) == "10") {
                //위쳇페이
                returnData = "WC"
            } else if (_scan.substring(0, 2) == "25" || _scan.substring(0, 2) == "26" ||
                _scan.substring(0, 2) == "27" || _scan.substring(0, 2) == "28" ||
                _scan.substring(0, 2) == "29" || _scan.substring(0, 2) == "30") {
                //알리페이
                returnData = "AL"
            } else {
                if (_scan.length == 21) {
                    //APP 카드
                    returnData = "AP"
                }
            }
        }
        return returnData
    }

    fun ShowToastBox(_str: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, _str, Toast.LENGTH_SHORT).show()
        }
    }
}
