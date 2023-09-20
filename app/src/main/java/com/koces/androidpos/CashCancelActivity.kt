package com.koces.androidpos

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.*
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.db.sqliteDbSdk
import com.koces.androidpos.sdk.db.sqliteDbSdk.TradeMethod
import com.koces.androidpos.sdk.van.Constants
import java.text.SimpleDateFormat
import java.util.*

class CashCancelActivity : BaseActivity() {
    lateinit var mTvwDate:TextView//원거래일자
    lateinit var mEdtAuNo:EditText//원승인번호
    lateinit var mEdtMoney:EditText//취소금액
    lateinit var mEdtTargetNum:EditText//사업자번호,전화번호입력필드
    lateinit var mBtnCashRecipt:Button//결제버튼

    /**취소사유버튼*/
    lateinit var mCl_1:Button//거래취소
    lateinit var mCl_2:Button//오류발급
    lateinit var mCl_3:Button//기타

    lateinit var mBtnPrivate: Button//현금영수증 개인
    lateinit var mBtnBusiness: Button//현금영수증 사업자
    lateinit var mBtnSelf: Button//현금영수증 자진발급

    lateinit var mSwtMsr:Switch//현금MSR카드 사용/미사용
    lateinit var mLinearInputNum:LinearLayout

    var mDate:String = ""
    var mCancelReason:Int = 1
    var mCashTarget:String = TradeMethod.CashPrivate

    /** sample 예방 코드
     * 달력 여러번 화면에 뜨는 문제 방지
     * */
    var mBloadedCalendar:Boolean = false

    var mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()

    /** 연속클릭 방지 시간 */
    private var mLastClickTime = 0L

    /** 복수가맹점일 때 TID 선택DIalog */
    lateinit var mTidDialog: TermIDSelectDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_cancel)
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
        mTvwDate = findViewById(R.id.cashcancel_tvw_date)
        var sdf = SimpleDateFormat("yyMMdd")
        var dateNow:String = sdf.format(Date())

        mDate = dateNow

        sdf = SimpleDateFormat("yy.MM.dd")
        dateNow = sdf.format(Date())
        mTvwDate.text = dateNow

        mTvwDate.setOnClickListener {
            if(mBloadedCalendar){  //현재 달력을 메모리에 로딩 중이거나 달력이 떠 있는 상태에서 터치시
                return@setOnClickListener
            }
            mBloadedCalendar = true
            DatePickerDialog(this,DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                setTextViewDate(mTvwDate,year,month,dayOfMonth);
                mBloadedCalendar = false
            },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show()
        }

        mEdtAuNo = findViewById(R.id.cashcancel_edt_auno)
        mEdtMoney = findViewById(R.id.cashcancel_edt_money)
        mBtnCashRecipt = findViewById(R.id.cashcancel_btn_cashpay)
        mEdtMoney.addTextChangedListener( NumberTextWatcher(mEdtMoney));


        //취소 사유 처리
        mCl_1 = findViewById(R.id.cashcancel_btn_cancel)
        mCl_1.setOnClickListener(BtnCancelReasondListener())
        mCl_2 = findViewById(R.id.cashcancel_btn_error)
        mCl_2.setOnClickListener(BtnCancelReasondListener())
        mCl_3 = findViewById(R.id.cashcancel_btn_error_etc)
        mCl_3.setOnClickListener(BtnCancelReasondListener())

        //발급 대상 버튼 설정
        mBtnPrivate = findViewById(R.id.cashcancel_btn_private)
        mBtnPrivate.setOnClickListener(BtnTargetListener())
        mBtnBusiness = findViewById(R.id.cashcancel_btn_business)
        mBtnBusiness.setOnClickListener(BtnTargetListener())
        mBtnSelf = findViewById(R.id.cashcancel_btn_self)
        mBtnSelf.setOnClickListener(BtnTargetListener())

        mLinearInputNum = findViewById(R.id.cashcancel_linear_customnum)
        mSwtMsr = findViewById(R.id.cashcancel_swt_msr)

        mSwtMsr.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> mLinearInputNum.visibility = View.INVISIBLE
                false -> mLinearInputNum.visibility = View.VISIBLE
            }
        }

        mEdtTargetNum = findViewById(R.id.cashcancel_edt_authnum)

        mKocesSdk.BleConnectionListener(bleSdkInterface.ConnectionListener { result: Boolean ->
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

        mBtnCashRecipt.setOnClickListener(BtnPayListener())
        //어플 최소화
        val btnMinimum = findViewById<Button>(R.id.cashcancel_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@CashCancelActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.cashcancel_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@CashCancelActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.cashcancel_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@CashCancelActivity, menu2Activity::class.java)
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    fun setTextViewDate(tvw: TextView, year:Int, month:Int, day:Int){
        var y:String = year.toString().substring(2,4)
        var m:String = if((month+1)<10) "0${month+1}" else "${month+1}"
        var d:String = if(day<10) "0${day}" else "$day"
        mDate = "$y$m$d"
        tvw.text = "$y.$m.$d"
    }

    inner class BtnCancelReasondListener: View.OnClickListener{
        override fun onClick(v: View?) {
            mCl_1.setBackgroundResource(R.drawable.segmentbtnleft_out)
            mCl_2.setBackgroundResource(R.drawable.segmentbtncenter_out)
            mCl_3.setBackgroundResource(R.drawable.segmentbtnright_out)
            mCl_1.setTextColor(Color.parseColor("#0f74fa"))
            mCl_2.setTextColor(Color.parseColor("#0f74fa"))
            mCl_3.setTextColor(Color.parseColor("#0f74fa"))
            when(v?.id){
                R.id.cashcancel_btn_cancel -> {
                    mCancelReason = 1
                    mCl_1.setBackgroundResource(R.drawable.segmentbtnleft_normal)
                    mCl_1.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.cashcancel_btn_error -> {
                    mCancelReason = 2
                    mCl_2.setBackgroundResource(R.drawable.segmentbtncenter_normal)
                    mCl_2.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.cashcancel_btn_error_etc -> {
                    mCancelReason = 3
                    mCl_3.setBackgroundResource(R.drawable.segmentbtnright_normal)
                    mCl_3.setTextColor(Color.parseColor("#ffffff"))
                }
                else->{}
            }
        }

    }

    inner class BtnTargetListener: View.OnClickListener{
        override fun onClick(v: View?) {
            mBtnPrivate.setBackgroundResource(R.drawable.segmentbtnleft_out)
            mBtnBusiness.setBackgroundResource(R.drawable.segmentbtncenter_out)
            mBtnSelf.setBackgroundResource(R.drawable.segmentbtnright_out)
            mBtnPrivate.setTextColor(Color.parseColor("#0f74fa"))
            mBtnBusiness.setTextColor(Color.parseColor("#0f74fa"))
            mBtnSelf.setTextColor(Color.parseColor("#0f74fa"))
            when(v?.id){
                R.id.cashcancel_btn_private -> {
                    mCashTarget = TradeMethod.CashPrivate
                    mBtnPrivate.setBackgroundResource(R.drawable.segmentbtnleft_normal)
                    mBtnPrivate.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.cashcancel_btn_business -> {
                    mCashTarget = TradeMethod.CashBusiness
                    mBtnBusiness.setBackgroundResource(R.drawable.segmentbtncenter_normal)
                    mBtnBusiness.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.cashcancel_btn_self -> {
                    mCashTarget = TradeMethod.CashSelf
                    mBtnSelf.setBackgroundResource(R.drawable.segmentbtnright_normal)
                    mBtnSelf.setTextColor(Color.parseColor("#ffffff"))
                }
                else->{}
            }
        }
    }

    inner class BtnPayListener:View.OnClickListener{
        override fun onClick(v: View?) {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 3000) {

                return
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            when(Setting.g_PayDeviceType){
                Setting.PayDeviceType.NONE -> {
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    Toast.makeText(this@CashCancelActivity,KocesPosSdk.getInstance().context.resources.getString(R.string.error_no_device),Toast.LENGTH_SHORT).show()

                    intent = Intent(this@CashCancelActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                    startActivity(intent)
                    return
                }
                Setting.PayDeviceType.BLE -> {

                }
                Setting.PayDeviceType.CAT -> {
                    CashCancel(getTid(),getStoreName(),getStoreAddr(),getStoreNumber(),getStorePhone(),getStoreOwner())
                    return
                }
                Setting.PayDeviceType.LINES ->{

                }
                else->{ //else 경우는 없음
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    Toast.makeText(this@CashCancelActivity,KocesPosSdk.getInstance().context.resources.getString(R.string.error_no_device),Toast.LENGTH_SHORT).show()

                    intent = Intent(this@CashCancelActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                    startActivity(intent)
                    return
                }
            }

            if (Setting.getPreference(mKocesSdk.activity,Constants.MULTI_STORE) != "")
            {

                mTidDialog = TermIDSelectDialog(this@CashCancelActivity, false, object:TermIDSelectDialog.DialogBoxListener {
                    override fun onClickCancel(_msg: String) {
                        ShowDialog(_msg)
                    }

                    override fun onClickConfirm(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String) {
                        CashCancel(_tid.replace(" ",""),
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
                CashCancel(getTid(),getStoreName(),getStoreAddr(),getStoreNumber(),getStorePhone(),getStoreOwner())
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

    fun CashCancel(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String)
    {
        if (!CheckValue()) {
            return
        }


        //로딩화면으로 데이터 전송
        val intent: Intent = Intent(this@CashCancelActivity, CashLoadingActivity::class.java)
        intent.putExtra("TrdType","B20")               //B10 B20
        intent.putExtra("TermID",_tid)
        intent.putExtra("CashTarget",when(mCashTarget){     //발급 대상
            sqliteDbSdk.TradeMethod.CashPrivate -> 1
            sqliteDbSdk.TradeMethod.CashBusiness -> 2
            sqliteDbSdk.TradeMethod.CashSelf -> 3
            else -> 1
        })
        intent.putExtra("Money",mEdtMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt())  //금액
        intent.putExtra("VAT", 0)
        intent.putExtra("SVC", 0)
        intent.putExtra("TXF", 0)
        intent.putExtra("OriAuNo",mEdtAuNo.text.toString())
        intent.putExtra("CancelReason",mCancelReason.toString()) //취소 사유가 있어야 한다.
        intent.putExtra("TradeNo", "")                  //코세스거래고유번호
        intent.putExtra("TrdCode", "0")                  //취소시 사용

        var oriDate:String = mDate
        oriDate = oriDate.substring(0,6)    //substring(1,6) 으로 처리 하면 1번째 자리가 표시 되지 않고 5자가 넘어옮
        intent.putExtra("OriAuDate",oriDate)


        var _keyYn:String = "K"
        var _CashInputMethod:String = sqliteDbSdk.TradeMethod.CashDirect
        if (mSwtMsr.isChecked)
        {
            _keyYn = "S"
            _CashInputMethod = sqliteDbSdk.TradeMethod.CashMs
            intent.putExtra("Auth",mEdtTargetNum.text.toString())       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        }
        else
        {
            intent.putExtra("Auth",mEdtTargetNum.text.toString())       //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        }

        intent.putExtra("KeyYn",  _keyYn)                  //'K' : Keyin , 'S' : Swipe, 'I' : IC, 'R' : RF, 'M' : Mobile RF, 'E' : PayOn, 'F' : Mobile PayOn
        intent.putExtra("CashInputMethod", _CashInputMethod)

        intent.putExtra("StoreName", _storeName)
        intent.putExtra("StoreNumber", _storeNumber)
        intent.putExtra("StoreAddr", _storeAddr)
        intent.putExtra("StorePhone", _storePhone)
        intent.putExtra("StoreOwner", _storeOwner)

        startActivity(intent)

    }

    fun CheckValue():Boolean{
        if(mDate==""){
            Toast.makeText(this@CashCancelActivity,KocesPosSdk.getInstance().context.resources.getString(R.string.error_Check_approval_number_date),Toast.LENGTH_SHORT).show()
            return false
        }
        if(mEdtAuNo.text.toString()==""){
            Toast.makeText(this@CashCancelActivity,KocesPosSdk.getInstance().context.resources.getString(R.string.error_Check_approval_number_date),Toast.LENGTH_SHORT).show()
            return false
        }
        if(mEdtMoney.text.toString()==""){
            Toast.makeText(this@CashCancelActivity,"금액을 입력해 주세요",Toast.LENGTH_SHORT).show()
            return false
        }

        //Toast.makeText(this@CashCancelActivity,"작업 필요",Toast.LENGTH_SHORT).show()
        if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE)
        {
            if(!mSwtMsr.isChecked && mEdtTargetNum.text.isEmpty() && mCashTarget!=TradeMethod.CashSelf){
                ShowDialog("고객번호가 입력되지 않았습니다")
                return false
            }//msr이 아닌 상태에서 인증 번호가 없는 경우

        }
        else if(Setting.g_PayDeviceType == Setting.PayDeviceType.LINES)
        {
            //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
            if(Setting.getSignPadType(mKocesSdk.getActivity()) == 3 || Setting.getSignPadType(mKocesSdk.getActivity()) == 0)
            {
                if(!mSwtMsr.isChecked && mEdtTargetNum.text.isEmpty() && mCashTarget!=TradeMethod.CashSelf){
                    ShowDialog("고객번호가 입력되지 않았습니다")
                    return false
                }
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
        if(getTid() == ""){
            ShowDialog("가맹점 등록 정보가 없습니다")
            return false
        }

        return true


    }

    fun getTid():String{
        return Setting.getPreference(mKocesSdk.activity,Constants.TID);
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
}