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
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.van.Constants

class CreditActivity : BaseActivity() {
    val TAG:String = CreditActivity::class.java.simpleName
    /** 입력필드 선언 */
    lateinit var mEdtMoney: EditText
    lateinit var mEdtSvc: EditText
    lateinit var mEdtTxf: EditText
    lateinit var mEdtInstallMonth:EditText  //할부 개월

    /** 텍스트박스 선언 */
    lateinit var mTvwMoney: TextView
    lateinit var mTvwVat: TextView
    lateinit var mTvwSvc: TextView
    lateinit var mTvwTotalMoney: TextView

    /** 버튼 선언 */
    lateinit var mBtnCreditPay: Button
    lateinit var mBtnInstall: Button
    lateinit var mBtnNoInstall: Button

    /** 리니어레이아웃 */
    lateinit var mLinearTxf:LinearLayout        //비과세 입력 LinearLayout
    lateinit var mLinearSvcAuto:LinearLayout    //봉사료 표시 LinearLayout
    lateinit var mLinearSvcManual:LinearLayout  //봉사료 입력 LinearLayout
    lateinit var mLinearMoney:LinearLayout      //금액 입력 LinearLayout
    lateinit var mLinearInstallment:LinearLayout //할부 입력 LinearLayout

    var mUseInstall:Boolean = false         //할부 사용


    val mTaxSdk:TaxSdk = TaxSdk.getInstance()
    var mKocesSdk:KocesPosSdk = KocesPosSdk.getInstance()

    /** 복수가맹점일 때 TID 선택DIalog */
    lateinit var mTidDialog: TermIDSelectDialog

    /** 연속클릭 방지 시간 */
    private var mLastClickTime = 0L

    /** 금액계산 */
    private var taxvalue: HashMap<String, Int> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit)
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
        mEdtMoney = findViewById(R.id.credit_edt_money)
        mEdtSvc = findViewById(R.id.credit_edt_svc)
        mEdtTxf = findViewById(R.id.credit_edt_txf)
        mEdtMoney.addTextChangedListener( NumberTextWatcher(mEdtMoney));
        mEdtSvc.addTextChangedListener( NumberTextWatcher(mEdtSvc));
        mEdtTxf.addTextChangedListener( NumberTextWatcher(mEdtTxf));

        mTvwTotalMoney = findViewById(R.id.credit_tvw_totalmoney)
        mTvwMoney = findViewById(R.id.credit_tvw_money)
        mTvwVat = findViewById(R.id.credit_tvw_vat)
        mTvwSvc = findViewById(R.id.credit_tvw_svc)
        mTvwTotalMoney.addTextChangedListener( NumberTextWatcher(mTvwTotalMoney));
        mTvwMoney.addTextChangedListener( NumberTextWatcher(mTvwMoney));
        mTvwVat.addTextChangedListener( NumberTextWatcher(mTvwVat));
        mTvwSvc.addTextChangedListener( NumberTextWatcher(mTvwSvc));


        mEdtInstallMonth = findViewById(R.id.credit_edt_installment)

        //linearlayout 설정
        mLinearSvcAuto = findViewById(R.id.credit_linear_svc_auto)
        mLinearSvcManual = findViewById(R.id.credit_linear_svc_manual)
        mLinearTxf = findViewById(R.id.credit_linear_txf)
        mLinearMoney = findViewById(R.id.credit_linear_money)

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

        mLinearTxf.visibility = when(mTaxSdk.useVAT){
            true -> when(mTaxSdk.vatMode){
                0 -> {
                    View.GONE
                }  //부가세 자동인 경우 부가세 입력 필드를 표시 하지 않는다.
                1 -> {
                    View.VISIBLE
                } //부가세가 통합인 경우에는 부가세 입력 필드를 표시 한다.
                else -> {
                    View.INVISIBLE
                }
            }
            false -> {
                View.VISIBLE
            }
        }

        mLinearMoney.visibility = when(mTaxSdk.useVAT){
            true ->{
                View.VISIBLE
            }
            false -> {
                View.GONE
            }
        }

        //버튼 설정
        mBtnInstall = findViewById(R.id.credit_btn_install)
        mBtnNoInstall = findViewById(R.id.credit_btn_no_install)
        mBtnInstall.setOnClickListener(InstallmentButtonListener())
        mBtnNoInstall.setOnClickListener(InstallmentButtonListener())

        mLinearInstallment = findViewById(R.id.credit_linear_installment) //할부 입력 리니어레이아웃

        //결제
        mBtnCreditPay = findViewById(R.id.credit_btn_pay)

        //입력금액애 따라서 실시간으로 세금을 표시 한다.
        mEdtMoney.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if(mEdtMoney.text.toString().isEmpty())
//                {
//                    mEdtMoney.setText("0")
//                }
            }

            override fun afterTextChanged(s: Editable?) {

                ChangeValue()
            }

        })
        mEdtSvc.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if(mEdtSvc.text.toString().isEmpty())
//                {
//                    mEdtSvc.setText("0")
//                }
            }

            override fun afterTextChanged(s: Editable?) {

                ChangeValue()
            }
        })

        mEdtTxf.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if(mEdtTxf.text.toString().isEmpty())
//                {
//                    mEdtTxf.setText("0")
//                }
            }

            override fun afterTextChanged(s: Editable?) {

                ChangeValue()
            }
        })

        //하단 버튼 처리
        mBtnCreditPay.setOnClickListener(PayButtonListener())

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
        val btnMinimum = findViewById<Button>(R.id.credit_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@CreditActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.credit_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@CreditActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.credit_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@CreditActivity,menu2Activity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            mEdtMoney.setText("")
            mEdtSvc.setText("")
            mEdtTxf.setText("");
            mEdtInstallMonth.setText("")

            mTvwTotalMoney.setText("")
            mTvwMoney.setText("");
            mTvwVat.setText("");
            mTvwSvc.setText("")

            mLinearInstallment.visibility = View.GONE
        }
    }

    inner class InstallmentButtonListener:View.OnClickListener{
        override fun onClick(v: View?) {
            when(v?.id){
                R.id.credit_btn_no_install -> {
                    segmentBtnSetting(0)
                }
                R.id.credit_btn_install-> {
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
        val deviceSecuritySDK = DeviceSecuritySDK(this) { result: String, Code: String?, state: String?, resultData: HashMap<String?, String?>? ->
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


    fun segmentBtnSetting(index:Int){
        mUseInstall = if (index == 0) false else true
        when(mUseInstall){
            false ->{
                mBtnNoInstall.setBackgroundResource(R.drawable.segmentbtnleft_normal)
                mBtnInstall.setBackgroundResource(R.drawable.segmentbtnright_out)
                mBtnNoInstall.setTextColor(Color.parseColor("#FFFFFF"))
                mBtnInstall.setTextColor(Color.parseColor("#0f74fa"))
                mLinearInstallment.visibility = View.GONE
            }
            true->{
                mBtnNoInstall.setBackgroundResource(R.drawable.segmentbtnleft_out)
                mBtnInstall.setBackgroundResource(R.drawable.segmentbtnright_normal)
                mBtnNoInstall.setTextColor(Color.parseColor("#0f74fa"))
                mBtnInstall.setTextColor(Color.parseColor("#FFFFFF"))
                mLinearInstallment.visibility = View.VISIBLE
            }

        }

    }


    fun Credit(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String){

        Log.d(TAG,"신용카드 결제 시작 함수")

        //금액 계산
//        var taxvalue: HashMap<String, Int> = HashMap()
        ChangeValue()
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
        val intent: Intent = Intent(this@CreditActivity, CreditLoadingActivity::class.java)
        intent.putExtra("TrdType","A10")                     //A10, A20
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
        if (Setting.getPreference(this, Constants.FALLBACK_USE) == "0") {
            intent.putExtra("FBYn", "0")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        } else {
            intent.putExtra("FBYn", "1")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        }

        intent.putExtra("TrdCode","")               //취소시 사용. 거래구분자

        intent.putExtra("StoreName", _storeName)
        intent.putExtra("StoreNumber", _storeNumber)
        intent.putExtra("StoreAddr", _storeAddr)
        intent.putExtra("StorePhone", _storePhone)
        intent.putExtra("StoreOwner", _storeOwner)

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        startActivity(intent)
    }
    inner class PayButtonListener:View.OnClickListener{
        override fun onClick(v: View?) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 3000) {

                return
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            //TODO 2022.02.10 kim.jy 추가 작업 필요

            if (CheckValue()) {

            } else {
                return
            }
//            when(CheckValue()){
//                false->return
//            }

            when(Setting.g_PayDeviceType){
                Setting.PayDeviceType.NONE -> {
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    intent = Intent(this@CreditActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                    startActivity(intent)
                    return
                }
                Setting.PayDeviceType.BLE -> {

                }
                Setting.PayDeviceType.CAT -> {
//                    Credit(getTid(),getStoreName(),getStoreAddr(),getStoreNumber(),getStorePhone(),getStoreOwner())
//                    return
                }
                Setting.PayDeviceType.LINES ->{

                }
                else->{ //else 경우는 없음
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    intent = Intent(this@CreditActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                    startActivity(intent)
                    return
                }
            }

            if (Setting.getPreference(mKocesSdk.activity,Constants.MULTI_STORE) != "")
            {

                mTidDialog = TermIDSelectDialog(this@CreditActivity, false, object:TermIDSelectDialog.DialogBoxListener {
                    override fun onClickCancel(_msg: String) {
                        ShowDialog(_msg)
                    }

                    override fun onClickConfirm(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String) {
                        Credit(_tid.replace(" ",""),
                            _storeName.replace(" ",""),
                            _storeAddr.replace(" ",""),
                            _storeNumber.replace(" ",""),
                            _storePhone.replace(" ",""),
                            _storeOwner.replace(" ",""))
                    }
                })
                mTidDialog.show()
            }
            else
            {
                Credit(getTid(),getStoreName(),getStoreAddr(),getStoreNumber(),getStorePhone(),getStoreOwner())
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

    fun CheckValue():Boolean {

        if( Setting.g_PayDeviceType == Setting.PayDeviceType.NONE){
            ShowDialog("환경설정에 장치 설정을 해야 합니다.")
            return false
        }

        //할부 개월 수 체크부
        if (mUseInstall) {
            val bArray = mEdtInstallMonth.text.toString().toByteArray()

            for (byte in bArray) {
                if (byte < 0x30 || byte > 0x39) {
                    ShowDialog("할부 개월 수는 숫자만 입력 가능 합니다.")
                    return false
                }
            }

            if (bArray.isEmpty()) {
                ShowDialog("할부는 최소 2개월 이상입니다")
                return false
            }

            if (mEdtInstallMonth.text.toString().toInt() < 2 || mEdtInstallMonth.text.toString()
                    .toInt() > 24
            ) {
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

    fun ShowDialog(_str: String) {
        Toast.makeText(this,_str, Toast.LENGTH_SHORT).show()
    }

    fun HideDialog() {
//        Setting.HidePopup();
        ReadyDialogHide()
    }
}
