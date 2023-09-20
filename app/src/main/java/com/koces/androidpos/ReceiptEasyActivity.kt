package com.koces.androidpos

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.db.sqliteDbSdk
import com.koces.androidpos.sdk.van.Constants
import com.woosim.printer.WoosimCmd
import java.io.*
import java.nio.charset.Charset

class ReceiptEasyActivity : BaseActivity() {
    val TAG:String = ReceiptEasyActivity::class.java.simpleName
    val mKocesSdk: KocesPosSdk = KocesPosSdk.getInstance()

    lateinit var mTvwStoreNm:TextView
    lateinit var mTvwBusineNumber:TextView
    lateinit var mTvwTID:TextView
    lateinit var mTvwOwner:TextView
    lateinit var mTvwPhone:TextView
    lateinit var mTvwAddr:TextView

    lateinit var mTvwNo: TextView
    lateinit var mTvwAuDate: TextView
    lateinit var mTvwAuNum: TextView
    lateinit var mTvwCardNum: TextView  //카드번호
    lateinit var mTvwCardKind: TextView //카드종류
    lateinit var mTvwIssuer: TextView       //발급사
    lateinit var mTvwAcquirer: TextView     //매입사

    lateinit var mTvwMoney:TextView
    lateinit var mTvwVat:TextView
    lateinit var mTvwSvc:TextView
    lateinit var mTvwTxf:TextView
    lateinit var mTvwTotalMoney:TextView
    lateinit var mInstallment:TextView
    lateinit var mTvwMsg:TextView   //응답메세지
    lateinit var mTvwKakaoAuMoney:TextView
    lateinit var mTvwKakaoSaleMoney:TextView

    /** 거래취소 */
    lateinit var mBtnCancel: Button
    /** 프린트 버튼 */
    lateinit var mBtnPrint: Button
    /** 이미지 저장 */
    lateinit var mBtnImageSave: Button

    var tradeType:String = ""   //거래구분자 캣신용, 일반신용 등을 구분
    var CancelInfo:String = ""  //취소인지/승인인지 구분자
    var oriAudate:String = ""   //원거래일자
    var giftMoney:String = ""   //선불카드잔액
    var mchNo:String = ""       //가맹점번호
    var totalMoney:String = ""  //결제금액(계산하기 편하기 위해 사용)

    var TID:String = "" //취소시 사용하는 DB 에 저장된 TID
    var BSN:String = ""     //사업자번호
    var StoreAddr:String = "" //가맹점주소
    var StoreName:String = "" //가맹점명
    var StoreOwner:String = "" //대표자명
    var StorePhone:String = "" //연락처

    var mPrintCount:Int = 0  //프린트는 1회만 가능하다. 재출력 불가
    var printMsg:String = ""    //영수증에서 프린트 시 출력할 내용
    lateinit var mLinearSvc:LinearLayout
    lateinit var mLinearTxf:LinearLayout
    lateinit var mLinearCancel:LinearLayout
    lateinit var mLinearKakaoAu:LinearLayout
    lateinit var mLinearKakaoSale:LinearLayout
    lateinit var mLinearEasy:LinearLayout

    lateinit var mData: sqliteDbSdk.DBTradeResult

    /** QR스캔카메라연동  */
    lateinit var intentIntegrator: IntentIntegrator

    /** 사인패드의 결과 값이 들어올 경우 이를 리시브 하기 위한 키  */
    private val REQUEST_SIGNPAD = 10001

    /** Qr스캔으로 받은 Qr번호 이번호를 취소시 사용한다 */
    var mQrNo: String = ""
    var mEasyKind: String = ""
    var mLastTid:String = ""    //복수가맹점일경우 TID 를 getTID로 불러오지 못하기에 TID를 따로 받는다
    var mValue: String = ""

    //sdk
    lateinit var mCatSdk: CatPaymentSdk
    lateinit var mPaySdk: PaymentSdk
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_easy)

        mValue = intent.getStringExtra("data").toString()
        mLastTid = intent.getStringExtra("TermID").toString()
        if (mLastTid == "")
        {
            mLastTid = getTid()
        }
        initRes(mValue)
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

    fun initRes(idx:String) {
        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this)
        mKocesSdk.BleregisterReceiver(this)
        mData = when(idx){
            "last" -> mKocesSdk.getSqliteDB_SelectTradeLastData(mLastTid)
            else -> mKocesSdk.getSqliteDB_SelectTradeData(Integer.parseInt(idx))
        }
        TID = if (mData.tid == "") getTid() else mData.tid

        when(mData.trade){
            sqliteDbSdk.TradeMethod.CAT_App -> {
                mEasyKind = "AP"
            }
            sqliteDbSdk.TradeMethod.CAT_Zero -> {
                mEasyKind = "ZP"
            }
            sqliteDbSdk.TradeMethod.CAT_Kakao ->{
                mEasyKind = "KP"
            }
            sqliteDbSdk.TradeMethod.CAT_Ali ->{
                mEasyKind = "AL"
            }
            sqliteDbSdk.TradeMethod.CAT_We ->{
                mEasyKind = "WC"
            }

            sqliteDbSdk.TradeMethod.Kakao -> {
                mEasyKind = "KP"
            }
            sqliteDbSdk.TradeMethod.Zero -> {
                mEasyKind = "ZP"
            }
            sqliteDbSdk.TradeMethod.Wechat ->{
                mEasyKind = "WC"
            }
            sqliteDbSdk.TradeMethod.Ali ->{
                mEasyKind = "AL"
            }
            sqliteDbSdk.TradeMethod.AppCard ->{
                mEasyKind = "AP"
            }
            sqliteDbSdk.TradeMethod.EmvQr ->{
                mEasyKind = "AP"
            }
            else->{ //else 경우는 없음
                mEasyKind = ""
            }
        }


        mTvwStoreNm = findViewById(R.id.receipteasy_txt_storenm)
        mTvwStoreNm.text =  when(TID){
            Setting.getPreference(this,Constants.TID) -> Setting.getPreference(this,Constants.STORE_NM).replace(" ","")
            Setting.getPreference(this,Constants.TID + "0") -> Setting.getPreference(this,Constants.STORE_NM+ "0").replace(" ","")
            Setting.getPreference(this,Constants.TID + "1") -> Setting.getPreference(this,Constants.STORE_NM+ "1").replace(" ","")
            Setting.getPreference(this,Constants.TID + "2") -> Setting.getPreference(this,Constants.STORE_NM+ "2").replace(" ","")
            Setting.getPreference(this,Constants.TID + "3") -> Setting.getPreference(this,Constants.STORE_NM+ "3").replace(" ","")
            Setting.getPreference(this,Constants.TID + "4") -> Setting.getPreference(this,Constants.STORE_NM + "4").replace(" ","")
            Setting.getPreference(this,Constants.TID+ "5") -> Setting.getPreference(this,Constants.STORE_NM+ "5").replace(" ","")
            else -> {
                mData.storeName.toString()
            }
        }

        mTvwBusineNumber = findViewById(R.id.receipteasy_txt_businesenumber)
        mTvwBusineNumber.text = when(TID){
            Setting.getPreference(this,Constants.TID) -> Setting.getPreference(this,Constants.STORE_NO).replace(" ","")
            Setting.getPreference(this,Constants.TID + "0") -> Setting.getPreference(this,Constants.STORE_NO+ "0").replace(" ","")
            Setting.getPreference(this,Constants.TID + "1") -> Setting.getPreference(this,Constants.STORE_NO+ "1").replace(" ","")
            Setting.getPreference(this,Constants.TID + "2") -> Setting.getPreference(this,Constants.STORE_NO+ "2").replace(" ","")
            Setting.getPreference(this,Constants.TID + "3") -> Setting.getPreference(this,Constants.STORE_NO+ "3").replace(" ","")
            Setting.getPreference(this,Constants.TID + "4") -> Setting.getPreference(this,Constants.STORE_NO + "4").replace(" ","")
            Setting.getPreference(this,Constants.TID+ "5") -> Setting.getPreference(this,Constants.STORE_NO+ "5").replace(" ","")
            else -> {
                mData.storeNumber.toString()
            }
        }

        mTvwTID = findViewById(R.id.receipteasy_txt_tid)
        mTvwTID.text =  "***" +  TID.substring(3)
        mTvwOwner = findViewById(R.id.receipteasy_txt_owner)
        mTvwOwner.text = when(TID){
            Setting.getPreference(this,Constants.TID) -> Setting.getPreference(this,Constants.OWNER_NM).replace(" ","")
            Setting.getPreference(this,Constants.TID + "0") -> Setting.getPreference(this,Constants.OWNER_NM+ "0").replace(" ","")
            Setting.getPreference(this,Constants.TID + "1") -> Setting.getPreference(this,Constants.OWNER_NM+ "1").replace(" ","")
            Setting.getPreference(this,Constants.TID + "2") -> Setting.getPreference(this,Constants.OWNER_NM+ "2").replace(" ","")
            Setting.getPreference(this,Constants.TID + "3") -> Setting.getPreference(this,Constants.OWNER_NM+ "3").replace(" ","")
            Setting.getPreference(this,Constants.TID + "4") -> Setting.getPreference(this,Constants.OWNER_NM + "4").replace(" ","")
            Setting.getPreference(this,Constants.TID+ "5") -> Setting.getPreference(this,Constants.OWNER_NM+ "5").replace(" ","")
            else -> {
                mData.storeOwner.toString()
            }
        }

        mTvwPhone = findViewById(R.id.receipteasy_txt_phone)
        mTvwPhone.text = when(TID){
            Setting.getPreference(this,Constants.TID) -> Setting.getPreference(this,Constants.STORE_PHONE).replace(" ","")
            Setting.getPreference(this,Constants.TID + "0") -> Setting.getPreference(this,Constants.STORE_PHONE+ "0").replace(" ","")
            Setting.getPreference(this,Constants.TID + "1") -> Setting.getPreference(this,Constants.STORE_PHONE+ "1").replace(" ","")
            Setting.getPreference(this,Constants.TID + "2") -> Setting.getPreference(this,Constants.STORE_PHONE+ "2").replace(" ","")
            Setting.getPreference(this,Constants.TID + "3") -> Setting.getPreference(this,Constants.STORE_PHONE+ "3").replace(" ","")
            Setting.getPreference(this,Constants.TID + "4") -> Setting.getPreference(this,Constants.STORE_PHONE + "4").replace(" ","")
            Setting.getPreference(this,Constants.TID+ "5") -> Setting.getPreference(this,Constants.STORE_PHONE+ "5").replace(" ","")
            else -> {
                mData.storePhone.toString()
            }
        }

        mTvwAddr = findViewById(R.id.receipteasy_txt_addr)
        mTvwAddr.text = when(TID){
            Setting.getPreference(this,Constants.TID) -> Setting.getPreference(this,Constants.STORE_ADDR).replace(" ","")
            Setting.getPreference(this,Constants.TID + "0") -> Setting.getPreference(this,Constants.STORE_ADDR+ "0").replace(" ","")
            Setting.getPreference(this,Constants.TID + "1") -> Setting.getPreference(this,Constants.STORE_ADDR+ "1").replace(" ","")
            Setting.getPreference(this,Constants.TID + "2") -> Setting.getPreference(this,Constants.STORE_ADDR+ "2").replace(" ","")
            Setting.getPreference(this,Constants.TID + "3") -> Setting.getPreference(this,Constants.STORE_ADDR+ "3").replace(" ","")
            Setting.getPreference(this,Constants.TID + "4") -> Setting.getPreference(this,Constants.STORE_ADDR + "4").replace(" ","")
            Setting.getPreference(this,Constants.TID+ "5") -> Setting.getPreference(this,Constants.STORE_ADDR+ "5").replace(" ","")
            else -> {
                mData.storeAddr.toString()
            }
        }
        if (mTvwStoreNm.text.toString() == "")
        {
            mTvwStoreNm.text =  mData.storeName.toString()
        }
        if (mTvwBusineNumber.text.toString() == "")
        {
            mTvwBusineNumber.text =  mData.storeNumber.toString()
        }
        if (mTvwOwner.text.toString() == "")
        {
            mTvwOwner.text =  mData.storeOwner.toString()
        }
        if (mTvwPhone.text.toString() == "")
        {
            mTvwPhone.text =  mData.storePhone.toString()
        }
        if (mTvwAddr.text.toString() == "")
        {
            mTvwAddr.text =  mData.storeAddr.toString()
        }

        mTvwNo = findViewById(R.id.receipteasy_txt_no)
        mTvwNo.text = mData.getid().toString()
        mTvwAuDate = findViewById(R.id.receipteasy_txt_date)
        mTvwAuDate.text = parsingDate(mData.auDate)
        mTvwAuNum = findViewById(R.id.receipteasy_txt_authno)
        mTvwAuNum.text = mData.auNum
        mTvwAcquirer = findViewById(R.id.receipteasy_txt_Acquirer)
        mTvwAcquirer.text = mData.cardInpNm
        mTvwIssuer = findViewById(R.id.receipteasy_txt_issuer)
        mTvwIssuer.text = mData.cardIssuer
        mTvwCardNum = findViewById(R.id.receipteasy_txt_cardnum)
        mTvwCardNum.text = mData.printBarcd
        mTvwCardKind = findViewById(R.id.receipteasy_txt_cardkind)
        when(mData.cardType){
            "1" -> {
                mTvwCardKind.text = "신용"
            }
            "2" -> {
                mTvwCardKind.text = "체크"
            }
            "3" ->{
                mTvwCardKind.text = "기프트"
            }
            "4" ->{
                mTvwCardKind.text = "기타"
            }
            " " ->{
                mTvwCardKind.text = "신용"
            }
            else->{ //else 경우는 없음
                mTvwCardKind.text = "기타"
            }
        }

        mInstallment = findViewById(R.id.receipteasy_txt_inst)
        var _inst = ""
        _inst = mData.inst + " 개월"
        if(mData.inst == "" || mData.inst == "0")
            _inst = "일시불"
        mInstallment.text = _inst
        mTvwMsg = findViewById(R.id.receipteasy_txt_msg)
        mTvwMsg.text = mData.message

        /** 금액설정 */
        mTvwMoney = findViewById(R.id.receipteasy_txt_orimoney)
        mTvwMoney.text = Utils.PrintMoney(mData.money) + "원"
        mTvwVat = findViewById(R.id.receipteasy_txt_vat)
        mTvwVat.text = Utils.PrintMoney(mData.tax) + "원"
        mTvwSvc = findViewById(R.id.receipteasy_txt_svc)
        mTvwSvc.text = Utils.PrintMoney(mData.svc) + "원"
        mTvwTxf = findViewById(R.id.receipteasy_txt_txf)
        mTvwTxf.text = Utils.PrintMoney(mData.txf) + "원"
        mTvwTotalMoney = findViewById(R.id.receipteasy_txt_totalmoney)
        if (!mData.trade.contains("(CAT)"))
        {
            totalMoney = "${mData.money.toInt() + mData.tax.toInt() + mData.svc.toInt()}"
        }
        else
        {
            totalMoney = "${mData.money.toInt() + mData.tax.toInt() + mData.svc.toInt() + mData.txf.toInt()}"
        }
        mTvwTotalMoney.text = Utils.PrintMoney(totalMoney) + "원"
        mTvwKakaoAuMoney = findViewById(R.id.receipteasy_txt_kakaoaumoney)
        mTvwKakaoAuMoney.text = Utils.PrintMoney(mData.kakaoAuMoney) + "원"
        mTvwKakaoSaleMoney = findViewById(R.id.receipteasy_txt_kakaosale)
        mTvwKakaoSaleMoney.text = Utils.PrintMoney(mData.kakaoSaleMoney) + "원"

        /** 봉사료,비과세 0원의 경우 화면에 표시 하지 않는다.  */
        mLinearSvc = findViewById(R.id.receipteasy_linear_svc)
        mLinearTxf = findViewById(R.id.receipteasy_linear_txf)
        if(Utils.isNullOrEmpty(mData.svc) || mData.svc == "0")
        {
            mLinearSvc.visibility = View.GONE
        }
        if(Utils.isNullOrEmpty(mData.txf) || mData.txf == "0"){
            mLinearTxf.visibility = View.GONE
        }

        /** 카카오페이가 0원이거나 없는 경우 화면에 표시 하지 않는다. */
        mLinearKakaoAu = findViewById(R.id.receipteasy_linear_kakaoaumoney)
        mLinearKakaoSale = findViewById(R.id.receipteasy_linear_kakaosale)
        if(Utils.isNullOrEmpty(mData.kakaoAuMoney) || mData.kakaoAuMoney == "0")
        {
            mLinearKakaoAu.visibility = View.GONE
        }
        if(Utils.isNullOrEmpty(mData.kakaoSaleMoney) || mData.kakaoSaleMoney == "0"){
            mLinearKakaoSale.visibility = View.GONE
        }

        //거래 취소의 경우에는 취소 글씨를 붉은색으로 표시 22.03.4 kim.jy
        if(mData.cancel == sqliteDbSdk.TradeMethod.Cancel){     //거래 취소의 경우에 앞에 - 붙인다.
            mTvwMoney.text = "-" + Utils.PrintMoney(mData.money) + "원"
            mTvwVat.text = "-" + Utils.PrintMoney(mData.tax) + "원"
            mTvwSvc.text = "-" + Utils.PrintMoney(mData.svc) + "원"
            mTvwTxf.text = "-" + Utils.PrintMoney(mData.txf) + "원"
            mTvwTotalMoney.text = "-" + Utils.PrintMoney(totalMoney) + "원"
            //거래의 취소의 경우에 붉은색으로 표시 한다.
            mTvwMoney.setTextColor(Color.RED)
            mTvwVat.setTextColor(Color.RED)
            mTvwSvc.setTextColor(Color.RED)
            mTvwTxf.setTextColor(Color.RED)
            mTvwTotalMoney.setTextColor(Color.RED)
        }

        tradeType = mData.trade
        CancelInfo = mData.cancel

        /** 영수증화면 전체레이아웃 */
        mLinearEasy = findViewById(R.id.receipteasy)

        /** 프린터 영수증 */
        mBtnPrint = findViewById(R.id.receipteasy_btn_print)
        mBtnPrint.setOnClickListener {
            Print_Receipt()
        }

        /** 이미지 저장 */
        mBtnImageSave = findViewById(R.id.receipteasy_btn_imagesave)
        mBtnImageSave.setOnClickListener {
            imgSaveOnClick()
        }

        /** 결제 취소 버튼 */
        mLinearCancel = findViewById(R.id.receipteasy_linear_cancel)
        mBtnCancel = findViewById(R.id.receipteasy_btn_cancel)
        if (mValue == "last")
        {
            mBtnCancel.text = "확인"
        }
        //이미 결제 취소된 상태라면 취소 버튼을 숨기기
        if(!Utils.isNullOrEmpty(mData.oriAuDate) && !Utils.isNullOrEmpty(mData.oriAuNum)){
//            mLinearCancel.visibility = View.GONE
            mBtnCancel.text = "확인"
        }

        var mCancelLastClickTime = 0L
        mBtnCancel.setOnClickListener {
//            CancelEasy()
            if (SystemClock.elapsedRealtime() - mCancelLastClickTime < 3000) {

                return@setOnClickListener
            }
            mCancelLastClickTime = SystemClock.elapsedRealtime();

            if (mValue == "last")
            {
                val intent = Intent(this@ReceiptEasyActivity, Main2Activity::class.java) // for tabslayout
//                intent.flags =
//                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//                intent.flags =
//                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("AppToApp",2)
                startActivity(intent)
                return@setOnClickListener
            }
            //이미 결제 취소된 상태라면 취소 버튼을 숨기기
            if(!Utils.isNullOrEmpty(mData.oriAuDate) && !Utils.isNullOrEmpty(mData.oriAuNum)){
                val intent = Intent(this@ReceiptEasyActivity, Main2Activity::class.java) // for tabslayout
//                intent.flags =
//                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//                intent.flags =
//                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("AppToApp",2)
                startActivity(intent)
                return@setOnClickListener
            }

            if (Setting.g_PayDeviceType == Setting.PayDeviceType.LINES) {
                if(Setting.getPreference(this@ReceiptEasyActivity,Constants.LINE_QR_READER) == Constants.LineQrReader.Camera.toString())
                {
                    if(mEasyKind == "ZP" || mEasyKind == "WC")
                    {
                        CancelEasy()
                    }
                    else
                    {
                        QrScan()
                    }

                }
                else
                {
                    if(mEasyKind == "ZP" || mEasyKind == "WC")
                    {
                        CancelEasy()
                    }
                    else
                    {
                        LineQrReader()
                    }

                }
            } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
                if(mEasyKind == "ZP" || mEasyKind == "WC")
                {
                    CancelEasy()
                }
                else
                {
                    QrScan()
                }

            } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.CAT) {
                if(Setting.getPreference(this@ReceiptEasyActivity,Constants.CAT_QR_READER) == Constants.CatQrReader.Camera.toString())
                {
                    if(mEasyKind == "ZP" || mEasyKind == "WC")
                    {
                        CancelEasy()
                    }
                    else
                    {
                        QrScan()
                    }
                }
                else
                {
                    CancelEasy()

                }
            }

//            if(mEasyKind == "ZP" || mEasyKind == "WC")
//            {
//
//            }
//            else
//            {
//                QrScan()
//            }


        }



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

        //하단 버튼 처리
        //어플 최소화
        val btnMinimum = findViewById<Button>(R.id.receipteasy_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@ReceiptEasyActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.receipteasy_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@ReceiptEasyActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.receipteasy_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@ReceiptEasyActivity, menu2Activity::class.java)
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
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

//            mEasyKind = Scan_Data_Parser(mQrNo)
            CancelEasy()

        } else {
            ShowToastBox(result)
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

    /** 간편 결제 취소 */
    fun CancelEasy(){
        //TODO 20.03.04 kim.jy 여기서 결제 취소 처리 한다.
        val intent: Intent = Intent(this@ReceiptEasyActivity, EasyLoadingActivity::class.java)
        when(Setting.g_PayDeviceType){
            Setting.PayDeviceType.NONE -> {
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

            }
            Setting.PayDeviceType.BLE -> {
                intent.putExtra("TrdType","K22")                     //K21, K22
            }
            Setting.PayDeviceType.CAT -> {
                intent.putExtra("TrdType","A20")
            }
            Setting.PayDeviceType.LINES ->{
                intent.putExtra("TrdType","K22")                     //K21, K22
            }
            else->{ //else 경우는 없음
                //이런 경우는 사전에 체크 되기 때문에 없어야 한다.

            }
        }

        intent.putExtra("TermID",TID)                     //K21, K22
        var oriDate:String = mData.auDate
        oriDate = oriDate.substring(0,6)    //substring(1,6) 으로 처리 하면 1번째 자리가 표시 되지 않고 5자가 넘어옮

        intent.putExtra("AuDate", oriDate)                           //원거래일자 YYMMDD
        intent.putExtra("AuNo", mData.auNum)                             //원승인번호
        intent.putExtra("TrdAmt", mData.money.toInt())              //거래금액 승인:공급가액, 취소:원승인거래총액
        intent.putExtra("TaxAmt", mData.tax.toInt())                //세금
        intent.putExtra("SvcAmt", mData.svc.toInt())                //봉사료
        intent.putExtra("TaxFreeAmt", mData.txf.toInt())            //비과세
        intent.putExtra("Month", mData.inst)    //할부
//        intent.putExtra("TrdCode", "")                          //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        intent.putExtra("TradeNo", mData.tradeNo.replace(" ",""))                          //Koces거래고유번호(거래고유키 취소 시 사용)
        intent.putExtra("DscYn", "1")                           //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn("1")
        intent.putExtra("FBYn", "0")                             //fallback 사용 0:fallback 사용 1: fallback 미사용
        intent.putExtra("QrNo", mQrNo)
        intent.putExtra("EasyKind", mEasyKind)  //간편결제 거래종류(카카오, 제로, 앱카드 등)
        intent.putExtra("SearchNumber", mData.searchNo.replace(" ",""))

        intent.putExtra("StoreName", mTvwStoreNm.text.toString().replace(" ",""))
        intent.putExtra("StoreNumber", mTvwBusineNumber.text.toString().replace(" ",""))
        intent.putExtra("StoreAddr", mTvwAddr.text.toString().replace(" ",""))
        intent.putExtra("StorePhone", mTvwPhone.text.toString().replace(" ",""))
        intent.putExtra("StoreOwner", mTvwOwner.text.toString().replace(" ",""))

        startActivity(intent)
    }


    /** 영수증 프린트 */
    /** 연속클릭 방지 시간 */
    private var mPrintLastClickTime = 0L
    fun Print_Receipt(){
        if (SystemClock.elapsedRealtime() - mPrintLastClickTime < 3000) {

            return
        }
        mPrintLastClickTime = SystemClock.elapsedRealtime();

        printMsg = ""

        //TODO 20.03.04 kim.jy 영수증 프린터 관련 처리 필요
        if (Setting.g_PayDeviceType == Setting.PayDeviceType.LINES) {
            ShowToastBox("USB 리더기는 지원하지 않습니다")
            return
        } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
            mKocesSdk.BleIsConnected()
            if (!Setting.getBleIsConnected())
            {
                ShowToastBox("BLE 리더기가 연결되지 않았습니다")
                return
            }
            if (!Utils.PrintDeviceCheck(this).isEmpty())
            {
                ShowToastBox("출력 가능한 장치가 없습니다")
                return
            }
        } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.CAT) {

        }

        PrintReceiptInit()
    }

    /** 여기서 부터 프린트 관련 파싱 */
    fun PrintReceiptInit()
    {
        //TODO : 여기서 프린트 중이라는 메세지박스를 보여준다
//        Utils.printAlertBox(Title: "프린트 출력중입니다", LoadingBar: true, GetButton: "")

        //신용매출
        if (CancelInfo == "1") {
            printParser(Utils.PrintCenter(Utils.PrintBold("간편결제취소")) + Constants.PENTER)
        } else {
            printParser(Utils.PrintCenter(Utils.PrintBold("간편결제승인")) + Constants.PENTER)
        }

        //전표번호(로컬DB에 저장되어 있는 거래내역리스트의 번호) + 전표출력일시
        printParser(Utils.PrintPad("No." + StringUtil.leftPad(mData.getid().toString(), "0", 6),parsingDate(mData.auDate)) + Constants.PENTER)
        //만약 취소 시에는 여기에서 원거래일자를 삽입해야 한다. 결국 sqlLITE 에 원거래일자 항목을 하나 만들어서 취소시에는 원거래일자에 승인일자를 삽입해야 한다.
        if (CancelInfo == "1") {
            printParser(Utils.PrintPad("원거래일", parsingDate(mData.oriAuDate)) + Constants.PENTER)
        }
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER)

        //가맹점명
        printParser(Utils.PrintPad("가맹점명", mTvwStoreNm.text.toString().replace(" ", "")) + Constants.PENTER)
        //사업자번호
        printParser(Utils.PrintPad("사업자번호", parsingbsn(mTvwBusineNumber.text.toString())) + Constants.PENTER)
        //단말기TID
        printParser(Utils.PrintPad("단말기ID", mTvwTID.text.toString()) + Constants.PENTER)
        //대표자명
        printParser(Utils.PrintPad("대표자명", mTvwOwner.text.toString()) + Constants.PENTER)
        //연락처
        printParser(Utils.PrintPad("연락처", phoneParser(mTvwPhone.text.toString())) + Constants.PENTER)
        //주소
        printParser(Utils.PrintPad("주소  ", mTvwAddr.text.toString()) + Constants.PENTER)
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER)

        //승인일시
        printParser(Utils.PrintPad("승인일시", parsingDate(mData.auDate))  + Constants.PENTER)
        //할부개월
        if (tradeType == sqliteDbSdk.TradeMethod.Credit.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Credit.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_App.toString()||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Zero.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Kakao.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Ali.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_We.toString())
        {
            printParser(Utils.PrintPad("할부개월", mInstallment.text.toString()) + Constants.PENTER)
        }
        //카드번호
        printParser(Utils.PrintPad( "고객번호",  mTvwCardNum.text.toString()) + Constants.PENTER)
        //승인번호
        printParser(Utils.PrintPad("승인번호", mTvwAuNum.text.toString().replace(" ","")) + Constants.PENTER)

        if (tradeType == sqliteDbSdk.TradeMethod.Credit.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Credit.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_App.toString()||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Zero.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Kakao.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_Ali.toString() ||
            tradeType == sqliteDbSdk.TradeMethod.CAT_We.toString())
        {
            //가맹점번호
            printParser(Utils.PrintPad("가맹점번호", mData.mchNo.replace(" ","")) + Constants.PENTER)
            //매입사명
            printParser(Utils.PrintPad("매입사명", mTvwAcquirer.text.toString().replace(" ", "")) + Constants.PENTER)
            //카드종류
            printParser(Utils.PrintPad("발급사명", mTvwIssuer.text.toString().replace(" ", "")) + Constants.PENTER)
        }
        //-------------
        printParser(Utils.PrintLine( "- ") + Constants.PENTER)

        //공급가액
        var correctMoney:Int = 0
        if (Setting.g_PayDeviceType == Setting.PayDeviceType.CAT) {
            correctMoney = Integer.parseInt(mData.money);
        }
        else if (Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
            correctMoney = Integer.parseInt(mData.money) - Integer.parseInt(mData.txf)
        } else {
            correctMoney = Integer.parseInt(mData.money) - Integer.parseInt(mData.txf)
        }
        var _totalMoney:String = totalMoney

        if(mData.cancel == sqliteDbSdk.TradeMethod.Cancel){     //거래 취소의 경우에 앞에 - 붙인다.
            //공급가액
            printParser(Utils.PrintPad("공급가액", "-" + Utils.PrintMoney( correctMoney.toString()) + "원" ) + Constants.PENTER)
            //부가세
            printParser(Utils.PrintPad("부가세", "-" + Utils.PrintMoney(mData.tax) + "원") + Constants.PENTER)
            //봉사료
            printParser(Utils.PrintPad("봉사료", "-" + Utils.PrintMoney(mData.svc) + "원") + Constants.PENTER)
            //비과세
            printParser(Utils.PrintPad("비과세", "-" + Utils.PrintMoney(mData.txf) + "원") + Constants.PENTER)

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , "-" + Utils.PrintBold(Utils.PrintMoney(_totalMoney) + "원")) + Constants.PENTER)
        }
        else
        {
            //공급가액
            printParser(Utils.PrintPad("공급가액", Utils.PrintMoney( correctMoney.toString()) + "원" ) + Constants.PENTER)
            //부가세
            printParser(Utils.PrintPad("부가세", Utils.PrintMoney(mData.tax) + "원") + Constants.PENTER)
            //봉사료
            printParser(Utils.PrintPad("봉사료", Utils.PrintMoney(mData.svc) + "원") + Constants.PENTER)
            //비과세
            printParser(Utils.PrintPad("비과세", Utils.PrintMoney(mData.txf) + "원") + Constants.PENTER)

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , Utils.PrintBold(Utils.PrintMoney(_totalMoney) + "원")) + Constants.PENTER)
        }
        //기프트카드 잔액
        if (mData.cardType.contains("3") || mData.cardType.contains("4")) {
            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(mData.giftAmt.replace(("[^\\d.]").toRegex(), "")) + "원") + Constants.PENTER)
        }

        //카카오승인금액 카카오할인금액
        if (tradeType == sqliteDbSdk.TradeMethod.Kakao.toString() &&
                !mData.kakaoAuMoney.isEmpty()) {
            printParser(Utils.PrintPad("카카오승인금액", Utils.PrintMoney(mData.kakaoAuMoney) + "원") + Constants.PENTER)
            printParser(Utils.PrintPad("카카오할인금액", Utils.PrintMoney(mData.kakaoSaleMoney) + "원") + Constants.PENTER)
        }
        //제로페이 가맹점수수료 가맹점환불금액
        if (tradeType == sqliteDbSdk.TradeMethod.Zero.toString() && !mData.mchFee.isEmpty()) {
            printParser(Utils.PrintPad("가맹점수수료", Utils.PrintMoney(mData.mchFee) + "원") + Constants.PENTER)
            printParser(Utils.PrintPad("가맹점환불금액", Utils.PrintMoney(mData.mchRefund) + "원") + Constants.PENTER)
        }

        //응답메시지
        printParser(mData.message + Constants.PENTER)
        printParser(Utils.PrintLine("- ") + Constants.PENTER)
//        if (Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE) ) {
        if (Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            printParser(Utils.PrintLine("  ") + Constants.PENTER)
            printParser(Utils.PrintLine("  ") + Constants.PENTER)
        }
        PrintReceipt(printMsg)

    }

    //개개 메세지 한줄씩 파싱
    fun printParser(_msg:String) {
        printMsg += _msg
    }

    //영수증을 프린터로 출력한다
    fun PrintReceipt(_msg:String) {

        var _totalMsg:String = ""
        _totalMsg += _msg
        if (!Setting.getPreference(this,Constants.PRINT_LOWLAVEL).isEmpty()) {
            _totalMsg += (Setting.getPreference(this, Constants.PRINT_LOWLAVEL) + Constants.PENTER)
        }

        //프린트 타임아웃 체크
        val mPrintSet = PrintDialog(this,"프린트 중입니다.", 30, object : PrintDialog.DialogBoxListener {
            override fun onResult(_msg: String) {
                ShowToastBox(_msg)
                printMsg = ""
                if (Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
                    mKocesSdk.__BLEDeviceInit(null, "99")
                } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.CAT)
                {
                    mCatSdk.Cat_SendCancelCommandE(false)
                }
            }
        })
        mPrintSet.show()
        if (Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
            mKocesSdk.BleUnregisterReceiver(this)
            mKocesSdk.BleregisterReceiver(Setting.getTopContext())
//            if (Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE) ) {
            if (Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
                try {
                    var text: ByteArray? = null
                    val prtStr = Command.BLEPrintParser(_totalMsg)
                    val string = prtStr
                    //                if (string == null)
//                    return;
//                else {
//                    try {
////                text = string.getBytes("US-ASCII");
//                        text = string.getBytes("EUC-KR");
//                        text = prtStr;
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
//                }
                    text = prtStr

//                byte b1[] = new byte[text.length + 3];
//                System.arraycopy(text, 0, b1, 3, text.length);
//                b1[0] = (byte)0x1B;
//                b1[1] = (byte)0x21;
//                b1[2] = (byte)0x02;
                    val byteStream = ByteArrayOutputStream()
                    byteStream.write(WoosimCmd.setTextStyle(true, true, false, 1, 1))
                    byteStream.write(WoosimCmd.setTextAlign(0))
                    if (text != null) byteStream.write(text)
                    byteStream.write(WoosimCmd.printData())
                    Handler(Looper.getMainLooper()).postDelayed({
                        mKocesSdk.__Printer(Utils.MakePacket(
                            0xc6.toByte(),
                            byteStream.toByteArray()
                        ), bleSdkInterface.ResDataListener { res ->

                            printMsg = ""
                            if (res[3] == Command.CMD_PRINT_RES) {
                                if (res[4] == Command.CMD_PRINT_ERROR30) {
                                    ShowToastBox("프린트를 완료하였습니다")
                                    mPrintSet.DisMiss()
                                } else if (res[4] == Command.CMD_PRINT_ERROR31) {
                                    ShowToastBox("용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다")
                                    mPrintSet.DisMiss()
                                } else if (res[4] == Command.CMD_PRINT_ERROR32) {
                                    ShowToastBox("배터리 부족으로 프린트를 실패하였습니다")
                                    mPrintSet.DisMiss()
                                } else {
                                    ShowToastBox("프린트를 실패하였습니다")
                                    mPrintSet.DisMiss()
                                }

                                return@ResDataListener
                                mKocesSdk.__BLEDeviceInit(null, "99")
                            }

                            if (res[9] == Command.CMD_PRINT_RES) {
                                if (res[10] == Command.CMD_PRINT_ERROR30) {
                                    ShowToastBox("프린트를 완료하였습니다")
                                    mPrintSet.DisMiss()
                                } else if (res[10] == Command.CMD_PRINT_ERROR31) {
                                    ShowToastBox("용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다")
                                    mPrintSet.DisMiss()
                                } else if (res[10] == Command.CMD_PRINT_ERROR32) {
                                    ShowToastBox("배터리 부족으로 프린트를 실패하였습니다")
                                    mPrintSet.DisMiss()
                                } else {
                                    ShowToastBox("프린트를 실패하였습니다")
                                    mPrintSet.DisMiss()
                                }

                                return@ResDataListener
                                mKocesSdk.__BLEDeviceInit(null, "99")
                            }
                        })
                    }, 1000)

                } catch (e: IOException) {
                    e.printStackTrace()
                    ShowToastBox("프린트를 실패하였습니다")
                    mPrintSet.DisMiss()
                    return
                }

                return
            }

            val prtStr = Command.BLEPrintParser(_totalMsg)
            mKocesSdk.__BLEDeviceInit(null, "99")
            Handler(Looper.getMainLooper()).postDelayed({
                mKocesSdk.__Printer(prtStr, bleSdkInterface.ResDataListener { res ->

                    printMsg = ""
                    if (res[3] == Command.CMD_PRINT_RES) {
                        if (res[4] == Command.CMD_PRINT_ERROR30) {
                            ShowToastBox("프린트를 완료하였습니다")
                            mPrintSet.DisMiss()
                        } else if (res[4] == Command.CMD_PRINT_ERROR31) {
                            ShowToastBox("용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다")
                            mPrintSet.DisMiss()
                        } else if (res[4] == Command.CMD_PRINT_ERROR32) {
                            ShowToastBox("배터리 부족으로 프린트를 실패하였습니다")
                            mPrintSet.DisMiss()
                        } else {
                            ShowToastBox("프린트를 실패하였습니다")
                            mPrintSet.DisMiss()
                        }

                        return@ResDataListener
                        mKocesSdk.__BLEDeviceInit(null, "99")
                    }
                })
            }, 1000)
        } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.CAT) {
            mCatSdk = CatPaymentSdk(this, Constants.CatPayType.Print)
            { result: String?, Code: String, resultData: HashMap<String?, String?>? ->
                if (Code == "SHOW") {
                    ShowToastBox(result + "\n" + "프린트를 실패하였습니다")
                } else {
                    if (Code == "COMPLETE_PRINT") {
                        ShowToastBox("프린트를 완료하였습니다")
                    } else {
                        ShowToastBox(result + "\n" + "프린트를 실패하였습니다")
                    }
                }
                mPrintSet.DisMiss()
            }

            mCatSdk.Print(_totalMsg, false)
        }
        return
    }


    /** 여기까지 프린트 관련 파싱 */
    fun parsingbsn(bsn:String):String{
        if (bsn == "")
        {
            return ""
        }
        val _bsn = bsn.replace(" ","")
        val ba: ByteArray = _bsn.toByteArray()
        if(ba.size>9){
            var ba2:ByteArray = ba.sliceArray(0..2)
            ba2 += 0x2D.toByte()
            ba2 += ba.sliceArray(3..4)
            ba2 += 0x2D.toByte()
            ba2 += ba.sliceArray(5..9)
            return ba2.toString(Charset.defaultCharset())
        }
        else
        {
            return _bsn
        }

    }

    //전화번호 중간에 - 를 넣는다
    fun phoneParser(tel:String) : String {
        if (tel == "")
        {
            return ""
        }
        val _telchars = tel.replace(" ","")
        val telchars: ByteArray = _telchars.toByteArray()
        var _tel:String = ""
        var ba2:ByteArray
        if (telchars.size == 9) {
            ba2 = telchars.sliceArray(0..1)
            ba2 += 0x2D.toByte()
            ba2 += telchars.sliceArray(2..4)
            ba2 += 0x2D.toByte()
            ba2 += telchars.sliceArray(5..8)
            return ba2.toString(Charset.defaultCharset())
        } else if (telchars.size == 10) {
            ba2 = telchars.sliceArray(0..1)
            ba2 += 0x2D.toByte()
            ba2 += telchars.sliceArray(2..5)
            ba2 += 0x2D.toByte()
            ba2 += telchars.sliceArray(6..9)
            return ba2.toString(Charset.defaultCharset())
        } else if (telchars.size == 11) {
            ba2 = telchars.sliceArray(0..2)
            ba2 += 0x2D.toByte()
            ba2 += telchars.sliceArray(3..6)
            ba2 += 0x2D.toByte()
            ba2 += telchars.sliceArray(7..10)
            return ba2.toString(Charset.defaultCharset())
        } else {
            _tel = tel.replace( " ",  "")
        }
        return _tel
    }

    fun parsingDate(date:String):String{
        if (date == "")
        {
            return ""
        }
        val ba: ByteArray = date.toByteArray()
        if(ba.size>12){
            ba.dropLast(ba.size - 12)
        }
        if (ba.size<10)
        {
            var ba2:ByteArray = ba.sliceArray(0..1)
            ba2 += 0x2F.toByte()
            ba2 += ba.sliceArray(2..3)
            ba2 += 0x2F.toByte()
            ba2 += ba.sliceArray(4..5)
            return ba2.toString(Charset.defaultCharset())
        }
        var ba2:ByteArray = ba.sliceArray(0..1)
        ba2 += 0x2F.toByte()
        ba2 += ba.sliceArray(2..3)
        ba2 += 0x2F.toByte()
        ba2 += ba.sliceArray(4..5)
        ba2 += 0x20.toByte()
        ba2 += ba.sliceArray(6..7)
        ba2 += 0x3A.toByte()
        ba2 += ba.sliceArray(8..9)
        ba2 += 0x3A.toByte()
        ba2 += ba.sliceArray(10..11)
        return ba2.toString(Charset.defaultCharset())
    }
    fun getTid():String{
        return Setting.getPreference(mKocesSdk.activity, Constants.TID);

    }

    fun ShowToastBox(_str: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(mKocesSdk.activity, _str, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SIGNPAD) {
            if (result != null) {
                if (result.contents == null || result.contents == "") {
                    ShowToastBox("바코드 스캔이 처리되지 않았습니다")
                    return
                }
                mQrNo = result.contents
//                mEasyKind = Scan_Data_Parser(mQrNo)
                CancelEasy()
            } else {
                ShowToastBox("바코드 스캔이 처리되지 않았습니다")
                return
            }
            return
        }

        ShowToastBox("바코드 스캔이 처리되지 않았습니다")
        return

    }

    //이미지 저장 버튼 클릭 메서드
    /** 연속클릭 방지 시간 */
    private var mImgLastClickTime = 0L
    fun imgSaveOnClick() {
        if (SystemClock.elapsedRealtime() - mImgLastClickTime < 3000) {

            return
        }
        mImgLastClickTime = SystemClock.elapsedRealtime();
        val bitmap = drawBitmap()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
            saveImageOnAboveAndroidQ(bitmap)
            Toast.makeText(baseContext, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
            val writePermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if(writePermission == PackageManager.PERMISSION_GRANTED) {
                saveImageOnUnderAndroidQ(bitmap)
                Toast.makeText(baseContext, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val requestExternalStorageCode = 1

                val permissionStorage = arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

                ActivityCompat.requestPermissions(this, permissionStorage, requestExternalStorageCode)
            }
        }

    }

    // 화면에 나타난 View를 Bitmap에 그릴 용도.
    private fun drawBitmap(): Bitmap {
        //기기 해상도를 가져옴.
        mLinearEasy.buildDrawingCache() //캐시 비트 맵 만들기
        val bitmap = mLinearEasy.drawingCache

        return bitmap
    }

    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
    private fun saveImageOnAboveAndroidQ(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".png" // 파일이름 현재시간.png

        /*
        * ContentValues() 객체 생성.
        * ContentValues는 ContentResolver가 처리할 수 있는 값을 저장해둘 목적으로 사용된다.
        * */
        val contentValues = ContentValues()
        contentValues.apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave") // 경로 설정
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName) // 파일이름을 put해준다.
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1) // 현재 is_pending 상태임을 만들어준다.
            // 다른 곳에서 이 데이터를 요구하면 무시하라는 의미로, 해당 저장소를 독점할 수 있다.
        }

        // 이미지를 저장할 uri를 미리 설정해놓는다.
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(uri != null) {
                val image = contentResolver.openFileDescriptor(uri, "w", null)
                // write 모드로 file을 open한다.

                if(image != null) {
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    //비트맵을 FileOutputStream를 통해 compress한다.
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 저장소 독점을 해제한다.
                    contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch(e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImageOnUnderAndroidQ(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)

        if(dir.exists().not()) {
            dir.mkdirs() // 폴더 없을경우 폴더 생성
        }

        try {
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile()
            //0KB 파일 생성.

            val fos = FileOutputStream(fileItem) // 파일 아웃풋 스트림

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            //파일 아웃풋 스트림 객체를 통해서 Bitmap 압축.

            fos.close() // 파일 아웃풋 스트림 객체 close

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
            // 브로드캐스트 수신자에게 파일 미디어 스캔 액션 요청. 그리고 데이터로 추가된 파일에 Uri를 넘겨준다.
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

            Handler(Looper.getMainLooper()).postDelayed({
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
                        Handler(Looper.getMainLooper()).postDelayed({
                            ReadyDialogShow(Setting.getTopContext(),"무결성 검증 중 입니다.",0)
                        }, 200)
                        Handler(Looper.getMainLooper()).postDelayed({
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
            Handler(Looper.getMainLooper()).postDelayed({
                //장치 정보 요청

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

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            if (_bleDeviceCheck == 1) {
                mKocesSdk.BleDisConnect()
                Handler(Looper.getMainLooper()).postDelayed({
                    ReadyDialogShow(
                        mKocesSdk.getActivity(),
                        "장치 연결 중 입니다.",
                        0
                    )
                }, 200)
                Handler(Looper.getMainLooper()).postDelayed({
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
}