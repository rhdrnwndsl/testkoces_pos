package com.koces.androidpos

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.van.Constants
import java.text.SimpleDateFormat
import java.util.*

class CashICActivity : BaseActivity() {
    private final val TAG:String = CashICActivity::class.java.simpleName

    lateinit var mBtnType_buy:Button                //구매
    lateinit var mBtnType_refund:Button             //환불
    lateinit var mBtnType_balance:Button            //잔액조회
    lateinit var mBtnType_buyInquiry:Button         //구매조회
    lateinit var mBtnType_refundInquiry:Button      //환불조회

    lateinit var mEdtMoney:EditText                 //거래금액
    lateinit var mEdtSvc:EditText                   //봉사료
    lateinit var mEdtTxf:EditText                   //비과세

    /**화면 표시용 필드 */
    lateinit var mTvwMoney:TextView                 //거래금액
    lateinit var mTvwVat:TextView                   //세금
    lateinit var mTvwSvc:TextView                   //봉사료
    lateinit var mTvwTotalMoney:TextView            //결제금액


    lateinit var mTvwOriAuDate:TextView             //원거래 일자
    lateinit var mEdtOriAuNo:EditText               //원거래번호
    lateinit var mEdtAulMoney:EditText              //원거래금액
    lateinit var mSwtStreamline:Switch              //간소화된 거래 여부
    lateinit var mSwtCardless:Switch                  //무카드취소
    lateinit var mBtnCashICPay:Button               //현금IC거래

    lateinit var mLinearSvcAuto:LinearLayout        //봉사료 자동
    lateinit var mLinearSvcManual: LinearLayout     //봉사료 수동
    lateinit var mLinearTxfAuto:LinearLayout        //비과세 자동
    lateinit var mLinearTxfManual:LinearLayout      //비과세 수동

    lateinit var mLinearTradeView:LinearLayout        //결제뷰
    lateinit var mLinearCancelView: LinearLayout     //취소뷰
    lateinit var mLinearDirectTradeView:LinearLayout        //간소화거래여부레이아웃(현재 사용하지 않기때문에 숨김)
    lateinit var mLinearNoCardView:LinearLayout      //무카드취소레이아웃(취소시에만 나와야함)
    /** 현금IC 타입
     * 0: 구매
     * 1: 환불
     * 2: 잔액조회
     * 3: 구매조회
     * 4: 환불조회
     * */
    var mPayType = 0

    /** 원거래 설정 */
    var mDate:String = ""
    var mBloadedCalendar:Boolean = false    //원거래일자가 여러번 눌리는 문제 처리 위해서

    /** 간소화거래 여부 */
    var mStreamlinedTrade = false
    var mCardlessCancel = false

    var mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()

    val mTaxSdk:TaxSdk = TaxSdk.getInstance()

    /** 잔액조회 */
    lateinit var mDefaultDialog: DefaultDialog

    /** 연속클릭 방지 시간 */
    private var mLastClickTime = 0L

    /** 금액계산 */
    private var taxvalue: HashMap<String, Int> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashic)
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
        //거래 타입
        mBtnType_buy = findViewById(R.id.cashic_btn_buy)
        mBtnType_refund= findViewById(R.id.cashic_btn_refund)
        mBtnType_balance= findViewById(R.id.cashic_btn_balance)
        mBtnType_buyInquiry= findViewById(R.id.cashic_btn_buyinquiry)
        mBtnType_refundInquiry= findViewById(R.id.cashic_btn_refundinquiry)
        mBtnType_buy.setOnClickListener(BtnPayTypeListener())
        mBtnType_refund.setOnClickListener(BtnPayTypeListener())
        mBtnType_balance.setOnClickListener(BtnPayTypeListener())
        mBtnType_buyInquiry.setOnClickListener(BtnPayTypeListener())
        mBtnType_refundInquiry.setOnClickListener(BtnPayTypeListener())

        //입력 필드
        mEdtMoney = findViewById(R.id.cashic_edt_money)
        mEdtTxf = findViewById(R.id.cashic_edt_txf)
        mEdtSvc = findViewById(R.id.cashic_edt_svc)
        mEdtMoney.addTextChangedListener( NumberTextWatcher(mEdtMoney));
        mEdtSvc.addTextChangedListener( NumberTextWatcher(mEdtSvc));
        mEdtTxf.addTextChangedListener( NumberTextWatcher(mEdtTxf));

        //표시 필드
        mTvwMoney = findViewById(R.id.cashic_tvw_money)     //거래금액
        mTvwVat = findViewById(R.id.cashic_tvw_vat)
        mTvwSvc = findViewById(R.id.cashic_tvw_svc)
        mTvwTotalMoney = findViewById(R.id.cashic_tvw_totalmoney)   //결제금액
        mTvwTotalMoney.addTextChangedListener( NumberTextWatcher(mTvwTotalMoney));
        mTvwMoney.addTextChangedListener( NumberTextWatcher(mTvwMoney));
        mTvwVat.addTextChangedListener( NumberTextWatcher(mTvwVat));
        mTvwSvc.addTextChangedListener( NumberTextWatcher(mTvwSvc));

        //원거래 일자
        mTvwOriAuDate = findViewById(R.id.cashic_tvw_date)
        var sdf = SimpleDateFormat("yyMMdd")
        var dateNow:String = sdf.format(Date())

        mDate = dateNow

        sdf = SimpleDateFormat("yy.MM.dd")
        dateNow = sdf.format(Date())
        mTvwOriAuDate.text = dateNow

        mTvwOriAuDate.setOnClickListener {
            if(mBloadedCalendar){  //현재 달력을 메모리에 로딩 중이거나 달력이 떠 있는 상태에서 터치시
                return@setOnClickListener
            }
            mBloadedCalendar = true
            DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                setTextViewDate(mTvwOriAuDate,year,month,dayOfMonth);
                mBloadedCalendar = false
            },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show()
        }



        //원승인번호
        mEdtOriAuNo = findViewById(R.id.cashic_edt_auno)
        //취소금액
        mEdtAulMoney = findViewById(R.id.cashic_edt_cancelmoney)
        mEdtAulMoney.addTextChangedListener( NumberTextWatcher(mEdtAulMoney));

        //간소화 거래 여부
        mSwtStreamline = findViewById(R.id.cashic_swt_trade)
        mSwtStreamline.setOnCheckedChangeListener { _, isChecked -> mStreamlinedTrade = isChecked }
        mSwtCardless = findViewById(R.id.cashic_swt_nocard)
        mSwtCardless.setOnCheckedChangeListener{_, isChecked -> mCardlessCancel = isChecked }

        mBtnCashICPay = findViewById(R.id.cashic_btn_cashicpay)
        mBtnCashICPay.setOnClickListener {
            CashIC()
        }

        //세금 설정에 따러서 표시 방법을 다르게 한다.
        //봉사료 설정 사용유무, 수동, 자동
        mLinearSvcAuto = findViewById(R.id.cashic_linear_svc_auto)
        mLinearSvcManual = findViewById(R.id.cashic_linear_svc_manual)

        if(mTaxSdk.useSVC) {
            mLinearSvcAuto.visibility = when(mTaxSdk.svcMode){
                0->View.VISIBLE
                1->View.GONE
                else->View.GONE
            }

            mLinearSvcManual.visibility = when(mTaxSdk.svcMode){
                0->View.GONE
                1->View.VISIBLE
                else->View.GONE
            }
        }else{
            mLinearSvcAuto.visibility = View.GONE
            mLinearSvcManual.visibility = View.GONE
        }
        //비과세 설정, 이부분은 자동, 수동 때문에 나중에 따라 확인이 한번 필요 함
        mLinearTxfManual = findViewById(R.id.cashic_linear_txf)
        mLinearTxfManual.visibility = View.GONE
        if(mTaxSdk.useSVC && mTaxSdk.vatMode==1){
            mLinearTxfManual.visibility = View.VISIBLE
        }

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

        mLinearTradeView = findViewById(R.id.cashic_trade_view)
        mLinearCancelView = findViewById(R.id.cashic_canceltrade_view)
        mLinearDirectTradeView = findViewById(R.id.cashic_simpletrade_view)
        mLinearNoCardView = findViewById(R.id.cashic_nocard_view)

        mLinearTradeView.visibility = View.VISIBLE
        mLinearCancelView.visibility = View.GONE
        mLinearDirectTradeView.visibility = View.GONE
        mLinearNoCardView.visibility = View.GONE

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
        val btnMinimum = findViewById<Button>(R.id.cashic_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@CashICActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.cashic_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@CashICActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.cashic_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@CashICActivity, menu2Activity::class.java)
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

            ChangeValue()

            if(intent.hasExtra("ServeMoney"))
            {
                mDefaultDialog = DefaultDialog(this@CashICActivity, intent.getStringExtra("ServeMoney").toString(), object:DefaultDialog.DialogBoxListener {
                    override fun onClickConfirm(_msg: String) {
                        ShowDialog(_msg)
                    }

                })
                mDefaultDialog.show()
            }
        }
        else
        {
            mEdtSvc.setText("")
            mEdtTxf.setText("");
            mTvwTotalMoney.setText("")
            mTvwMoney.setText("");
            mTvwVat.setText("");
            mTvwSvc.setText("")
        }
    }

    fun setTextViewDate(tvw: TextView, year:Int, month:Int, day:Int){
        var y:String = year.toString().substring(2,4)
        var m:String = if((month+1)<10) "0${month+1}" else "${month+1}"
        var d:String = if(day<10) "0${day}" else "$day"
        mDate = "$y$m$d"
        tvw.text = "$y.$m.$d"
    }

    inner class BtnPayTypeListener: View.OnClickListener{
        override fun onClick(v: View?) {
            testResetPayTypeButton()
            when(v?.id){
                R.id.cashic_btn_buy -> {
                    mPayType = 0
                    mBtnType_buy.setBackgroundResource(R.drawable.segmentsubbtnleft_normal)
                    mBtnType_buy.setTextColor(Color.parseColor("#7b28ef"))
                    mLinearTradeView.visibility = View.VISIBLE
                    mLinearCancelView.visibility = View.GONE
                    mLinearDirectTradeView.visibility = View.VISIBLE
                    mLinearNoCardView.visibility = View.GONE
                }
                R.id.cashic_btn_refund -> {
                    mPayType = 1
                    mBtnType_refund.setBackgroundResource(R.drawable.segmentsubbtncenter_normal)
                    mBtnType_refund.setTextColor(Color.parseColor("#7b28ef"))
                    mLinearTradeView.visibility = View.GONE
                    mLinearCancelView.visibility = View.VISIBLE
                    mLinearDirectTradeView.visibility = View.GONE
                    mLinearNoCardView.visibility = View.VISIBLE
                }
                R.id.cashic_btn_balance -> {
                    mPayType = 2
                    mBtnType_balance.setBackgroundResource(R.drawable.segmentsubbtncenter_normal)
                    mBtnType_balance.setTextColor(Color.parseColor("#7b28ef"))
                    mLinearTradeView.visibility = View.GONE
                    mLinearCancelView.visibility = View.GONE
                    mLinearDirectTradeView.visibility = View.GONE
                    mLinearNoCardView.visibility = View.GONE
                }
                R.id.cashic_btn_buyinquiry ->{
                    mPayType = 3
                    mBtnType_buyInquiry.setBackgroundResource(R.drawable.segmentsubbtncenter_normal)
                    mBtnType_buyInquiry.setTextColor(Color.parseColor("#7b28ef"))
                    mLinearTradeView.visibility = View.GONE
                    mLinearCancelView.visibility = View.VISIBLE
                    mLinearDirectTradeView.visibility = View.GONE
                    mLinearNoCardView.visibility = View.GONE

                }
                R.id.cashic_btn_refundinquiry ->{
                    mPayType = 4
                    mBtnType_refundInquiry.setBackgroundResource(R.drawable.segmentsubbtnright_normal)
                    mBtnType_refundInquiry.setTextColor(Color.parseColor("#7b28ef"))
                    mLinearTradeView.visibility = View.GONE
                    mLinearCancelView.visibility = View.VISIBLE
                    mLinearDirectTradeView.visibility = View.GONE
                    mLinearNoCardView.visibility = View.GONE
                }
                else->{}
            }
        }
    }
    fun testResetPayTypeButton(){
        mBtnType_buy.setBackgroundResource(R.drawable.segmentsubbtnleft_out)
        mBtnType_refund.setBackgroundResource(R.drawable.segmentsubbtncenter_out)
        mBtnType_balance.setBackgroundResource(R.drawable.segmentsubbtncenter_out)
        mBtnType_buyInquiry.setBackgroundResource(R.drawable.segmentsubbtncenter_out)
        mBtnType_refundInquiry.setBackgroundResource(R.drawable.segmentsubbtnright_out)
        mBtnType_buy.setTextColor(Color.parseColor("#505050"))
        mBtnType_refund.setTextColor(Color.parseColor("#505050"))
        mBtnType_balance.setTextColor(Color.parseColor("#505050"))
        mBtnType_buyInquiry.setTextColor(Color.parseColor("#505050"))
        mBtnType_refundInquiry.setTextColor(Color.parseColor("#505050"))
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

    fun CashIC()
    {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 3000) {

            return
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        ChangeValue()
        //0=buy 1=cancel 2=search 3=buysearch 4=cancelsearch
        when (mPayType) {
            0 -> {
                //봉사료가 수동 입력인 경우에 입력값 처리
                ChangeValue()

                CashIC_Buy()
            }
            1 -> {
                CashIC_Cancel()
            }
            2 -> {
                CashIC_Search()
            }
            3 -> {
                CashIC_BuySearch()
            }
            4 -> {
                CashIC_CancelSearch()
            }
            else -> return
        }
    }

    fun CashIC_Buy()
    {
        //금액 계산
        ChangeValue()
        //로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@CashICActivity, CashICLoadingActivity::class.java)
        intent.putExtra("PreviewActivity","CashICActivity")
        intent.putExtra("TrdType","C10")               //C10 C20 C30 C40 C50
        intent.putExtra("TermID",getTid())
        intent.putExtra("TrdAmt", taxvalue.get("Money"))       //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", taxvalue.get("VAT"))           //세금
        intent.putExtra("SvcAmt", taxvalue.get("SVC"))           //봉사료
        intent.putExtra("TaxFreeAmt", taxvalue.get("TXF"))           //비과세
        intent.putExtra("AuDate", "")                //원거래일자 YYMMDD
        intent.putExtra("AuNo", "")                  //원승인번호

        if (mSwtStreamline.isChecked)
        {
            intent.putExtra("DirectTrade","1")  //간소화거래여부
        }
        else {
            intent.putExtra("DirectTrade", "0")
        }

        intent.putExtra("CardInfo", "0")
//        if (mSwtCardless.isChecked)
//        {
//            intent.putExtra("CardInfo","1")  //카드정보수록여부
//        }
//        else {
//
//        }
        intent.putExtra("Cancel",  false)       //취소여부

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        intent.putExtra("StoreName", getStoreName())
        intent.putExtra("StoreNumber", getStoreNumber())
        intent.putExtra("StoreAddr", getStoreAddr())
        intent.putExtra("StorePhone", getStorePhone())
        intent.putExtra("StoreOwner", getStoreOwner())

        startActivity(intent)
    }

    fun CashIC_Cancel()
    {
        //로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@CashICActivity, CashICLoadingActivity::class.java)
        intent.putExtra("PreviewActivity","CashICActivity")
        intent.putExtra("TrdType","C20")               //C10 C20 C30 C40 C50
        intent.putExtra("TermID",getTid())
        intent.putExtra("TrdAmt", mEdtAulMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt())       //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", 0)           //세금
        intent.putExtra("SvcAmt", 0)           //봉사료
        intent.putExtra("TaxFreeAmt", 0)           //비과세
        intent.putExtra("AuDate", mTvwOriAuDate.text.toString().replace(".",""))                //원거래일자 YYMMDD
        intent.putExtra("AuNo", StringUtil.rightPad(mEdtOriAuNo.text.toString()," ",13))                  //원승인번호

        intent.putExtra("DirectTrade", "0")
        if (mSwtCardless.isChecked)
        {
            intent.putExtra("CardInfo","1")  //카드정보수록여부
        }
        else {
            intent.putExtra("CardInfo", "0")
        }
        intent.putExtra("Cancel",  true)       //취소여부

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        intent.putExtra("StoreName", getStoreName())
        intent.putExtra("StoreNumber", getStoreNumber())
        intent.putExtra("StoreAddr", getStoreAddr())
        intent.putExtra("StorePhone", getStorePhone())
        intent.putExtra("StoreOwner", getStoreOwner())

        startActivity(intent)
    }

    fun CashIC_Search()
    {
//로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@CashICActivity, CashICLoadingActivity::class.java)
        intent.putExtra("PreviewActivity","CashICActivity")
        intent.putExtra("TrdType","C30")               //C10 C20 C30 C40 C50
        intent.putExtra("TermID","")
        intent.putExtra("TrdAmt", "0")       //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", "0")           //세금
        intent.putExtra("SvcAmt", "0")           //봉사료
        intent.putExtra("TaxFreeAmt", "0")           //비과세
        intent.putExtra("AuDate", "")                //원거래일자 YYMMDD
        intent.putExtra("AuNo", "")                  //원승인번호

        intent.putExtra("DirectTrade", "0")
        intent.putExtra("CardInfo", "0")

        intent.putExtra("Cancel",  false)       //취소여부

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        startActivity(intent)
    }

    fun CashIC_BuySearch()
    {
//로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@CashICActivity, CashICLoadingActivity::class.java)
        intent.putExtra("PreviewActivity","CashICActivity")
        intent.putExtra("TrdType","C40")               //C10 C20 C30 C40 C50
        intent.putExtra("TermID","")
        intent.putExtra("TrdAmt", mEdtAulMoney.text.toString().replace("[^0-9]".toRegex(), ""))       //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", "0")           //세금
        intent.putExtra("SvcAmt", "0")           //봉사료
        intent.putExtra("TaxFreeAmt", "0")           //비과세
        intent.putExtra("AuDate", mTvwOriAuDate.text.toString().replace(".",""))                //원거래일자 YYMMDD
        intent.putExtra("AuNo", StringUtil.rightPad(mEdtOriAuNo.text.toString()," ",13))                  //원승인번호

        intent.putExtra("DirectTrade", "0")
        intent.putExtra("CardInfo", "0")

        intent.putExtra("Cancel",  false)       //취소여부

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        startActivity(intent)
    }

    fun CashIC_CancelSearch()
    {
//로딩화면으로 값을 넘긴다.
        val intent: Intent = Intent(this@CashICActivity, CashICLoadingActivity::class.java)
        intent.putExtra("PreviewActivity","CashICActivity")
        intent.putExtra("TrdType","C50")               //C10 C20 C30 C40 C50
        intent.putExtra("TermID","")
        intent.putExtra("TrdAmt", mEdtAulMoney.text.toString().replace("[^0-9]".toRegex(), ""))       //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", "0")           //세금
        intent.putExtra("SvcAmt", "0")           //봉사료
        intent.putExtra("TaxFreeAmt", "0")           //비과세
        intent.putExtra("AuDate", mTvwOriAuDate.text.toString().replace(".",""))                //원거래일자 YYMMDD
        intent.putExtra("AuNo", StringUtil.rightPad(mEdtOriAuNo.text.toString()," ",13))                  //원승인번호

        intent.putExtra("DirectTrade", "0")
        intent.putExtra("CardInfo", "0")

        intent.putExtra("Cancel",  false)       //취소여부

        /**
         * 이부분은 만일 거래를 안하거나 오류로 종료되어 다시 크레딧페이지로 돌아올 시 셋팅하기 위한 값을 보내준다
         */
        intent.putExtra("EdtMoney", mEdtMoney.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtTxf", mEdtTxf.text.toString().replace("[^0-9]".toRegex(), ""))
        intent.putExtra("EdtSvc", mEdtSvc.text.toString().replace("[^0-9]".toRegex(), ""))

        startActivity(intent)
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


    fun getTid():String{
        return Setting.getPreference(KocesPosSdk.getInstance().activity,Constants.TID).replace(" ","");
    }

    fun getStoreName():String{
        return Setting.getPreference(KocesPosSdk.getInstance().activity,Constants.STORE_NM).replace(" ","");
    }
    fun getStoreNumber():String{
        return Setting.getPreference(KocesPosSdk.getInstance().activity,Constants.STORE_NO).replace(" ","");
    }
    fun getStoreAddr():String{
        return Setting.getPreference(KocesPosSdk.getInstance().activity,Constants.STORE_ADDR).replace(" ","");
    }
    fun getStorePhone():String{
        return Setting.getPreference(KocesPosSdk.getInstance().activity,Constants.STORE_PHONE).replace(" ","");
    }
    fun getStoreOwner():String{
        return Setting.getPreference(KocesPosSdk.getInstance().activity,Constants.OWNER_NM).replace(" ","");
    }

    fun ShowDialog(_str: String) {
        Toast.makeText(this,_str, Toast.LENGTH_SHORT).show()
    }
}