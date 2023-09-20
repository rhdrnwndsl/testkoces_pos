package com.koces.androidpos

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.van.Constants
import java.text.SimpleDateFormat
import java.util.*

class CreditCancelActivity : BaseActivity() {
    private final val TAG:String = CreditCancelActivity::class.java.simpleName
    lateinit var mTvwDate:TextView
    lateinit var medtAuNo:EditText
    lateinit var medtMoney:EditText
    lateinit var mBtnCreditCancel:Button

    var mDate:String = ""
    var mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()

    /** 복수가맹점일 때 TID 선택DIalog */
    lateinit var mTidDialog: TermIDSelectDialog

    /** 연속클릭 방지 시간 */
    private var mLastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cedit_cancel)
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
        //날짜 설정
        mTvwDate = findViewById(R.id.creditcancel_tvw_date)
        var sdf = SimpleDateFormat("yyMMdd")
        var dateNow:String = sdf.format(Date())

        mDate = dateNow

        sdf = SimpleDateFormat("yy.MM.dd")
        dateNow = sdf.format(Date())
        mTvwDate.text = dateNow


        mTvwDate.setOnClickListener {
            DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                setTextViewDate(mTvwDate,year,month,dayOfMonth);
            },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show()
        }
        //승인번호, 승인금액 입력
        medtAuNo = findViewById(R.id.creditcancel_edt_auno)
        medtMoney = findViewById(R.id.creditcancel_edt_money)
        medtMoney.addTextChangedListener( NumberTextWatcher(medtMoney));

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
                ReadyDialogHide()
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
                ReadyDialogHide()
            }
        })

        //카드 결제 취소 버튼
        mBtnCreditCancel = findViewById(R.id.creditcancel_btn_cashpay)
        mBtnCreditCancel.setOnClickListener(CreditPayListener())
        //어플 최소화
        val btnMinimum = findViewById<Button>(R.id.creditcancel_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@CreditCancelActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.creditcancel_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@CreditCancelActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.creditcancel_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@CreditCancelActivity, menu2Activity::class.java)
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
    fun setTextViewDate(tvw:TextView,year:Int,month:Int,day:Int){
        var y:String = year.toString().substring(2,4)
        var m:String = if((month+1)<10) "0${month+1}" else "${month+1}"
        var d:String = if(day<10) "0${day}" else "$day"
        mDate = "$y$m$d"
        tvw.text = "$y.$m.$d"
    }

    inner class CreditPayListener: View.OnClickListener{
        override fun onClick(v: View?) {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 3000) {

                return
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            when(Setting.g_PayDeviceType){
                Setting.PayDeviceType.NONE -> {
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    Toast.makeText(this@CreditCancelActivity,KocesPosSdk.getInstance().context.resources.getString(R.string.error_no_device),Toast.LENGTH_SHORT).show()

                    intent = Intent(this@CreditCancelActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                    startActivity(intent)
                    return
                }
                Setting.PayDeviceType.BLE -> {

                }
                Setting.PayDeviceType.CAT -> {
                    Credit(getTid(),getStoreName(),getStoreAddr(),getStoreNumber(),getStorePhone(),getStoreOwner())
                    return
                }
                Setting.PayDeviceType.LINES ->{

                }
                else->{ //else 경우는 없음
                    //이런 경우는 사전에 체크 되기 때문에 없어야 한다.
                    Toast.makeText(this@CreditCancelActivity,KocesPosSdk.getInstance().context.resources.getString(R.string.error_no_device),Toast.LENGTH_SHORT).show()

                    intent = Intent(this@CreditCancelActivity,Main2Activity::class.java)
                    intent.putExtra("AppToApp",2)
                    startActivity(intent)
                    return
                }
            }

            if (Setting.getPreference(mKocesSdk.activity,Constants.MULTI_STORE) != "")
            {

                mTidDialog = TermIDSelectDialog(this@CreditCancelActivity, false, object:TermIDSelectDialog.DialogBoxListener {
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
                        ReadyDialogShow(Setting.getTopContext(),"무결성 검증 중 입니다.",0)
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

    fun Credit(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String)
    {
        if (!CheckValue()) {
            return
        }

        val intent: Intent = Intent(this@CreditCancelActivity, CreditLoadingActivity::class.java)
        intent.putExtra("TrdType","A20")                     //A10, A20
        intent.putExtra("TermID",_tid)
        var oriDate:String = mDate
        oriDate = oriDate.substring(0,6)    //substring(1,6) 으로 처리 하면 1번째 자리가 표시 되지 않고 5자가 넘어옮
        intent.putExtra("AuDate", oriDate)                           //원거래일자 YYMMDD
        intent.putExtra("AuNo", medtAuNo.text.toString())                             //원승인번호
        intent.putExtra("TrdAmt", medtMoney.text.toString().replace("[^0-9]".toRegex(), "").toInt())              //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", 0)                //세금
        intent.putExtra("SvcAmt", 0)                //봉사료
        intent.putExtra("TaxFreeAmt", 0)            //비과세
        intent.putExtra("Month", "0")    //할부
//        intent.putExtra("TrdCode", "")                          //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        intent.putExtra("TradeNo", "")                          //Koces거래고유번호(거래고유키 취소 시 사용)
        intent.putExtra("DscYn", "1")                           //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn("1")
        if (Setting.getPreference(this, Constants.FALLBACK_USE) == "0") {
            intent.putExtra("FBYn", "0")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        } else {
            intent.putExtra("FBYn", "1")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        }
        intent.putExtra("TrdCode","0")               //취소시 사용. 거래구분자

        intent.putExtra("StoreName", _storeName)
        intent.putExtra("StoreNumber", _storeNumber)
        intent.putExtra("StoreAddr", _storeAddr)
        intent.putExtra("StorePhone", _storePhone)
        intent.putExtra("StoreOwner", _storeOwner)

        startActivity(intent)
    }

    fun CheckValue():Boolean{
        if(medtAuNo.length() == 0 || medtAuNo.length()>10){
            Toast.makeText(this@CreditCancelActivity,"정확한 승인 번호가 필요 합니다.",Toast.LENGTH_SHORT).show()
            return false
        }

        if(medtMoney.length() == 0){
            Toast.makeText(this@CreditCancelActivity,"금액을 입력 해야 합니다.",Toast.LENGTH_SHORT).show()
            return false
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
}

