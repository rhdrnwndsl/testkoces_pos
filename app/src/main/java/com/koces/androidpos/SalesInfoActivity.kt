package com.koces.androidpos

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.db.sqliteDbSdk.DBTradeResult
import com.koces.androidpos.sdk.db.sqliteDbSdk.TradeMethod
import com.koces.androidpos.sdk.van.Constants
import java.text.SimpleDateFormat
import java.util.*

class SalesInfoActivity : BaseActivity() {
    private final val TAG:String = SalesInfoActivity::class.java.simpleName

    /** 전체 선택 */
    lateinit var mBtnAll:Button
    /** 당일 선택 */
    lateinit var mBtnDay:Button
    /** 당월 선택 */
    lateinit var mBtnMonth:Button
    /** 조회 */
    lateinit var mBtnSearch:Button
    /** 전체:0, 당일:1, 당월:2, 달력을 선택한 경우:3 */
    var mPeriod:Int = 1

    /** 시작일 */
    lateinit var mTvwFromDate:TextView
    /** 종료일 */
    lateinit var mTvwToDate:TextView
    /** 조회 시작일 */
    var mFromDate:String = ""
    /** 조회 완료일 */
    var mToDate:String = ""

    /** 결제금액 */
    lateinit var mTvwAmout:TextView
    /** 결제건수 */
    lateinit var mTvwCount:TextView
    /** 평균금액 */
    lateinit var mTvwAverage:TextView

    /** 환불금액 */
    lateinit var mTvwRefundAmout:TextView
    /** 환불건수 */
    lateinit var mTvwRefundCount:TextView
    /** 환불금액 */
    lateinit var mTvwRefundAverage:TextView

    /** 신용 */
    lateinit var mTvwCrdit:TextView
    /** 체크 */
    lateinit var mTvwCheck:TextView
    /** 기프트 */
    lateinit var mTvwGift:TextView
    /** 기타 */
    lateinit var mTvwETC:TextView

    /** 개인 */
    lateinit var mTvwPrivate:TextView
    /** 사업자 */
    lateinit var mTvwBusiness:TextView
    /** 자진 */
    lateinit var mTvwSelf:TextView

    /** 카카오 */
    lateinit var mTvwKaKao:TextView
    /** 제로 */
    lateinit var mTvwZero:TextView
    /** APP */
    lateinit var mTvwApp:TextView
    /** BC QR */
    lateinit var mTvwBcqr:TextView

    /** 현금IC */
    lateinit var mTvwCashIC:TextView

    /** 복수가맹점일 때 TID 선택DIalog */
    lateinit var mTidDialog: TermIDSelectDialog

    lateinit var mSalesClass:Sales
    val mPosSdk:KocesPosSdk = KocesPosSdk.getInstance()
    lateinit var mProgressBar:ProgressBarDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_info)
        mSalesClass = Sales()
        mProgressBar = ProgressBarDialog(this)
        initRes()
        Setting.setIsAppForeGround(1);
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPosSdk.BleUnregisterReceiver(this)
        mPosSdk.BleregisterReceiver(Setting.getTopContext())
    }

    fun initRes(){
        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this)
        mPosSdk.BleregisterReceiver(this)
        //전체, 당일, 당월 선택
        mBtnAll = findViewById(R.id.sales_btn_dateall)
        mBtnDay = findViewById(R.id.sales_btn_dateday)
        mBtnMonth = findViewById(R.id.sales_btn_datemonth)
        mBtnAll.setOnClickListener(BtnPeriodListener())
        mBtnDay.setOnClickListener(BtnPeriodListener())
        mBtnMonth.setOnClickListener(BtnPeriodListener())

        mBtnSearch = findViewById(R.id.sales_btn_search)    //조회 버튼
        mBtnSearch.setOnClickListener(BtnSearchListener())

        //날짜입력 설정
        SetCalender()
        //결제
        mTvwAmout = findViewById(R.id.sales_tvw_pay_money)
        mTvwCount = findViewById(R.id.sales_tvw_pay_count)
        mTvwAverage = findViewById(R.id.sales_tvw_pay_average)
        //환불
        mTvwRefundAmout = findViewById(R.id.sales_tvw_refund_money)
        mTvwRefundCount = findViewById(R.id.sales_tvw_refund_count)
        mTvwRefundAverage = findViewById(R.id.sales_tvw_refund_average)
        //카드
        mTvwCrdit = findViewById(R.id.sales_tvw_credit)
        mTvwCheck = findViewById(R.id.sales_tvw_check)
        mTvwGift = findViewById(R.id.sales_tvw_gift)
        mTvwETC = findViewById(R.id.sales_tvw_etc)
        //현금
        mTvwPrivate = findViewById(R.id.sales_tvw_private)
        mTvwBusiness = findViewById(R.id.sales_tvw_business)
        mTvwSelf = findViewById(R.id.sales_tvw_self)
        //간편결제
        mTvwKaKao = findViewById(R.id.sales_tvw_kakao)
        mTvwZero = findViewById(R.id.sales_tvw_zero)
        mTvwApp = findViewById(R.id.sales_tvw_app)
        mTvwBcqr = findViewById(R.id.sales_tvw_bcqr)
        //현금IC
        mTvwCashIC = findViewById(R.id.sales_tvw_cashic)



        mPosSdk.BleConnectionListener(ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mPosSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
//                ReadyDialogHide()
                Handler(Looper.getMainLooper()).post {
                    if (Setting.getBleName() == Setting.getPreference(
                            mPosSdk.activity,
                            Constants.BLE_DEVICE_NAME
                        )
                    ) {
                        BleDeviceInfo()
                    } else {
                        Setting.setPreference(
                            mPosSdk.activity,
                            Constants.BLE_DEVICE_NAME,
                            Setting.getBleName()
                        )
                        Setting.setPreference(
                            mPosSdk.activity,
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
        mPosSdk.BleWoosimConnectionListener(bleWoosimInterface.ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mPosSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
//                ReadyDialogHide()
                Handler(Looper.getMainLooper()).post {
                    if (Setting.getBleName() == Setting.getPreference(
                            mPosSdk.activity,
                            Constants.BLE_DEVICE_NAME
                        )
                    ) {
                        BleDeviceInfo()
                    } else {
                        Setting.setPreference(
                            mPosSdk.activity,
                            Constants.BLE_DEVICE_NAME,
                            Setting.getBleName()
                        )
                        Setting.setPreference(
                            mPosSdk.activity,
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

        //어플 최소화
        val btnMinimum = findViewById<Button>(R.id.sales_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@SalesInfoActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp", 2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.sales_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@SalesInfoActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.sales_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@SalesInfoActivity, menu2Activity::class.java)
//            intent.flags =
//                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
    /** 날짜 지정 버튼 처리 함수 */
    fun SetCalender() {
        mTvwFromDate = findViewById(R.id.sales_tvw_startdate)
        mTvwToDate = findViewById(R.id.sales_tvw_enddate)

        var sdf = SimpleDateFormat("yyMMdd")
        var dateNow: String = sdf.format(Date())

        mFromDate = dateNow
        mToDate = dateNow

        sdf = SimpleDateFormat("yy.MM.dd")
        dateNow = sdf.format(Date())
        mTvwFromDate.text = dateNow


        mTvwFromDate.setOnClickListener {
            DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    setDateTextView(mTvwFromDate, year, month, dayOfMonth);
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        mTvwToDate.text = dateNow




        mTvwToDate.setOnClickListener {
            DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    setDateTextView(mTvwToDate, year, month, dayOfMonth);
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
    fun SetCalender(period:Int){
        var sdf = SimpleDateFormat("yyMMdd")
        var dateNow: String = sdf.format(Date())

        mFromDate = dateNow
        mToDate = dateNow

        sdf = SimpleDateFormat("yy.MM.dd")
        dateNow = sdf.format(Date())
        mTvwFromDate.text = dateNow
        mTvwToDate.text = dateNow

        when(period){
            2->{
                sdf = SimpleDateFormat("yyMM")
                dateNow = sdf.format(Date())
                mFromDate = dateNow + "01"

                sdf = SimpleDateFormat("yy.MM")
                dateNow = sdf.format(Date())
                mTvwFromDate.text = dateNow + ".01"
            }
            else->{

            }
        }
    }
    /**
     * 월에 + 1을 하는 이유 이 녀석만 아래처럼 정의 되어 있어서
     *  public final static int JANUARY = 0;
     *  */
    fun setDateTextView(tvw:TextView,year:Int,month:Int,day:Int){
        mPeriod = 3 //날짜를 선택해서 값을 변경 한 경우 조회시에 달력을 기준으로 조회 한다.
        var y:String = year.toString().substring(2,4)
        var m:String = if((month+1)<10) "0${month+1}" else "${month+1}"
        var d:String = if(day<10) "0${day}" else "$day"


        if(tvw==mTvwFromDate){
            val tmp:String = "$y$m$d"
            if(tmp.toInt()>mToDate.toInt()){
                Toast.makeText(this@SalesInfoActivity,"조회 시작일이 완료일보다 빠를 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            mFromDate = "$y$m$d"

        }else{
            val tmp:String = "$y$m$d"
            if(tmp.toInt() < mFromDate.toInt()){
                Toast.makeText(this@SalesInfoActivity,"조회 완료일이 시작일보다 빠를 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            mToDate = "$y$m$d"
        }
        tvw.text = "$y.$m.$d"
    }
    inner class BtnPeriodListener: View.OnClickListener{
        override fun onClick(v: View?) {
            mBtnAll.setBackgroundResource(R.drawable.segmentbtnright_out)
            mBtnDay.setBackgroundResource(R.drawable.segmentbtnleft_out)
            mBtnMonth.setBackgroundResource(R.drawable.segmentbtncenter_out)
            mBtnAll.setTextColor(Color.parseColor("#0f74fa"))
            mBtnDay.setTextColor(Color.parseColor("#0f74fa"))
            mBtnMonth.setTextColor(Color.parseColor("#0f74fa"))
            when(v?.id){
                R.id.sales_btn_dateday -> {
                    mPeriod = 1
                    SetCalender(1)
                    mBtnDay.setBackgroundResource(R.drawable.segmentbtnleft_normal)
                    mBtnDay.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.sales_btn_datemonth -> {
                    mPeriod = 2
                    SetCalender(2)
                    mBtnMonth.setBackgroundResource(R.drawable.segmentbtncenter_normal)
                    mBtnMonth.setTextColor(Color.parseColor("#ffffff"))
                }
                R.id.sales_btn_dateall -> {
                    mPeriod = 0
                    SetCalender(0)
                    mBtnAll.setBackgroundResource(R.drawable.segmentbtnright_normal)
                    mBtnAll.setTextColor(Color.parseColor("#FFFFFF"))
                }
                else->{}
            }
        }
    }

    inner class BtnSearchListener: View.OnClickListener {
        override fun onClick(v: View?) {
            //조회 결과 처리 필이ㅛ 22.02.25 kim.jy
            //Toast.makeText(this@SalesInfoActivity,"현재 준비중",Toast.LENGTH_SHORT).show()
            if (Setting.getPreference(mPosSdk.activity,Constants.MULTI_STORE) != "")
            {
                mTidDialog = TermIDSelectDialog(this@SalesInfoActivity, true, object:TermIDSelectDialog.DialogBoxListener {
                    override fun onClickCancel(_msg: String) {
                        Toast.makeText(this@SalesInfoActivity,_msg,Toast.LENGTH_SHORT).show()
                    }

                    override fun onClickConfirm(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String) {
                        getDbData(_tid.replace(" ",""))
                    }
                })
                mTidDialog.show()
            }
            else
            {
                getDbData(getTid())
            }

        }
    }

    fun getDbData(_tid:String){
        mProgressBar.setText("데이터 조회중")
        mProgressBar.show()

        /**
         * 여기서 멀티 tid 인 경우 tid를 체크해서 해당 tid로 검색. 만일 tid 없을 경우 전체검색
         */

        var tid = _tid
//        if(Utils.isNullOrEmpty(tid)){
//            Toast.makeText(this@SalesInfoActivity,"등록된 TID 가 없습니다. 가맹점등록을 진행해 주십시오.",Toast.LENGTH_SHORT).show()
//            mProgressBar.dismiss()
//            return
//        }
        var dbTradeResult:ArrayList<DBTradeResult> = ArrayList()

            when(mPeriod){
            0 -> {
                dbTradeResult = mPosSdk.getSqliteDB_SelectTradeData(_tid)
            }
            1 -> {
                dbTradeResult = mPosSdk.getSqliteDB_SelectTradeListPeriod(tid,mFromDate,mToDate)
            }
            2 -> {
                dbTradeResult = mPosSdk.getSqliteDB_SelectTradeListPeriod(tid,mFromDate,mToDate)
            }
            3 -> {
                dbTradeResult = mPosSdk.getSqliteDB_SelectTradeListPeriod(tid,mFromDate,mToDate)
            }
        }

        getAvarageValue(dbTradeResult)
    }
    fun getAvarageValue(_data:ArrayList<DBTradeResult>){
        //모든 값을 초기화 한다.
        mSalesClass.Clear() //내부 멥버를 값을 0으로 초기화
        setTextViewValue()

        /** 데이터가 없으면 바로 넘어간다 */
        if(_data.count()==0){
            Toast.makeText(this@SalesInfoActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            mProgressBar.dismiss()
            return
        }

        //db에서 넘어온데이터로 처리 한다.
        _data.forEach{ result ->
            (
                when (result.cancel)
                {
                    TradeMethod.NoCancel ->
                    {
                        val money: Int =
                            result.money.toInt() + result.tax.toInt() + result.svc.toInt() - result.txf.toInt()
                        mSalesClass.addpayAmount(money)
                        when (result.trade)
                        {
                            /** 신용의 경우 */
                            TradeMethod.Credit -> mSalesClass.credit(money) /** 신용의 경우 */
                            TradeMethod.CAT_Credit -> mSalesClass.credit(money)
                            /** 현금의 경우 */
                            TradeMethod.Cash -> when(result.cashTarget)
                            {
                                TradeMethod.CashPrivate -> mSalesClass.private(money)   /** 현금- 현금 영수증 */
                                TradeMethod.CashBusiness -> mSalesClass.business(money) /** 현금 - 사업자지출증빙 */
                                TradeMethod.CashSelf -> mSalesClass.self(money)         /** 현금 - 자체발급 */
                            }
                            TradeMethod.CAT_Cash->when(result.cashTarget)
                            {
                                TradeMethod.CashPrivate -> mSalesClass.private(money)   /** 현금- 현금 영수증 */
                                TradeMethod.CashBusiness -> mSalesClass.business(money) /** 현금 - 사업자지출증빙 */
                                TradeMethod.CashSelf -> mSalesClass.self(money)         /** 현금 - 자체발급 */
                            }
                            /** 체크의 경우 */

                            /** 기프트의 경우 */

                            /** 카카오 */
                            TradeMethod.Kakao-> mSalesClass.kakao(money)
                            TradeMethod.CAT_Kakao -> mSalesClass.kakao(money)
                            /** 제로 */
                            TradeMethod.Zero -> mSalesClass.zero(money)
                            TradeMethod.CAT_Zero -> mSalesClass.zero(money)
                            /** App */
                            TradeMethod.AppCard-> mSalesClass.appi(money)
                            TradeMethod.CAT_App -> mSalesClass.appi(money)
                            /** 위쳇 */
                            /** 알리 */
                            /** BCQR */

                            /** 현금 IC */
                            TradeMethod.CAT_CashIC->mSalesClass.cashIC(money)
                        }
                    }
                    TradeMethod.Cancel ->
                    {
                        val money: Int =
                            result.money.toInt() + result.tax.toInt() + result.svc.toInt() - result.txf.toInt()
                        mSalesClass.addrefundAmount(money)
                    }
                }
            )
        }

        setTextViewValue()
        //화면에 표시 되는 데이터 로딩중 화면 닫기
        mProgressBar.dismiss()
        Toast.makeText(this@SalesInfoActivity,"데이터 조회가 완료 되었습니다",Toast.LENGTH_SHORT).show()
    }
    fun setTextViewValue(){
        mTvwAmout.text = Utils.PrintMoney(mSalesClass.getpayAmount().toString())
        mTvwCount.text = mSalesClass.getpayCount().toString()
        mTvwAverage.text = Utils.PrintMoney(mSalesClass.getpayAverage().toString())
        mTvwRefundAmout.text = Utils.PrintMoney(mSalesClass.getrefundAmount().toString())
        mTvwRefundCount.text = mSalesClass.getrefundCount().toString()
        mTvwRefundAverage.text = Utils.PrintMoney(mSalesClass.getrefundAverage().toString())
        mTvwCrdit.text = Utils.PrintMoney(mSalesClass.credit().toString())
        mTvwCheck.text = Utils.PrintMoney(mSalesClass.check().toString())
        mTvwGift.text = Utils.PrintMoney(mSalesClass.gift().toString())
        mTvwETC.text = Utils.PrintMoney(mSalesClass.etc().toString())
        mTvwPrivate.text = Utils.PrintMoney(mSalesClass.private().toString())
        mTvwBusiness.text = Utils.PrintMoney(mSalesClass.business().toString())
        mTvwSelf.text = Utils.PrintMoney(mSalesClass.self().toString())
        mTvwKaKao.text = Utils.PrintMoney(mSalesClass.kakao().toString())
        mTvwZero.text = Utils.PrintMoney(mSalesClass.zero().toString())
        mTvwApp.text = Utils.PrintMoney(mSalesClass.appi().toString())
        mTvwBcqr.text = Utils.PrintMoney(mSalesClass.bcqr().toString())
        mTvwCashIC.text = Utils.PrintMoney(mSalesClass.cashIC().toString())
    }
    inner class Sales {
        private var payAmount = 0;
        private var payCount = 0;
        private var payAverage = 0
        private var refundAmount = 0;
        private var refundCount = 0;
        private var refundAverage = 0
        private var credit = 0;
        private var check = 0;
        private var gift = 0;
        private var etc = 0;
        private var private = 0;
        private var business = 0;
        private var self = 0;
        private var kakao = 0;
        private var zero = 0;
        private var appi = 0;
        private var bcqr = 0;
        private var cashIC = 0
        fun init() {
            Clear()
        }
        /** 결제 데이터 추가 */
        fun addpayAmount(value: Int) {
            payAmount += value
            payCount++
            payAverage = (payAmount / payCount).toInt()
        }
        /** 결제 금액 */
        fun getpayAmount(): Int { return payAmount }
        /** 결제 건수 */
        fun getpayCount(): Int { return payCount }
        /** 걸제 평균 금액 */
        fun getpayAverage(): Int { return payAverage }
        /** 환별 결제 데이터 추가 */
        fun addrefundAmount(value: Int) {
            refundAmount += value
            refundCount++
            refundAverage = (refundAmount / refundCount).toInt()
        }
        /** 환별 총금액 */
        fun getrefundAmount():Int{ return refundAmount }
        /** 환불 건수 */
        fun getrefundCount():Int{return refundCount}
        /** 환불 평균 금액 */
        fun getrefundAverage():Int{return refundAverage}
        /** 신용 금액 */
        fun credit(value: Int) { credit += value }
        fun credit(): Int { return credit }
        /** 체크 금액 */
        fun check(value: Int) { check += value }
        fun check(): Int { return check }
        /** gift amount */
        fun gift(value: Int) { gift += value }
        fun gift(): Int { return gift }
        /** etc amount */
        fun etc(value: Int) {etc += value }
        fun etc(): Int {return etc }
        /** 현금영수증 */
        fun private(value: Int) {private += value }
        fun private(): Int {return private}
        /** 사업자 지출 증빙 */
        fun business(value: Int) {business += value}
        fun business(): Int {return business}
        /** 자진발급 */
        fun self(value: Int) {self += value}
        fun self(): Int {return self}
        /** 카카오 */
        fun kakao(value: Int) {kakao += value}
        fun kakao(): Int {return kakao}
        /** 제로페이 */
        fun zero(value: Int) {zero += value}
        fun zero(): Int {return zero}
        /** App */
        fun appi(value: Int) {appi += value}
        fun appi(): Int {return appi}
        /** BCQR */
        fun bcqr(value: Int) {bcqr += value}
        fun bcqr(): Int {return bcqr}
        /** 현금IC */
        fun cashIC(value: Int) {cashIC += value}
        fun cashIC(): Int {return cashIC}
        /** 데이터클리어 */
        fun Clear() {
            payAmount = 0; payCount = 0; payAverage = 0
            refundAmount = 0; refundCount = 0; refundAverage = 0
            credit = 0; check = 0; gift = 0; etc = 0;
            private = 0; business = 0; self = 0;
            kakao = 0; zero = 0; appi = 0; bcqr = 0;
            cashIC = 0;
        }
    }

    fun getTid():String{
        return Setting.getPreference(mPosSdk.activity, Constants.TID);
    }

    var _bleCount = 0
    private fun setBleInitializeStep() {
        ReadyDialogShow(Setting.getTopContext(), "무결성 검증 중 입니다.", 0)
        _bleCount += 1
        /* 무결성 검증. 초기화진행 */
        val deviceSecuritySDK = DeviceSecuritySDK(this) { result: String, Code: String?, state: String?, resultData: HashMap<String?, String?>? ->
            if (result == "00") {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1) //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
                //                ReadyDialogHide();
            }
            else {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1)
                //                ReadyDialogHide();
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (result == "00") {
                    Toast.makeText(
                        mPosSdk.getActivity(),
                        "무결성 검증에 성공하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (result == "9999") {

                    if (_bleCount > 1) {
                        _bleCount = 0
                        Toast.makeText(
                            mPosSdk.activity,
                            "네트워크 오류. 다시 시도해 주세요",
                            Toast.LENGTH_SHORT
                        ).show()

                        mPosSdk.BleDisConnect()

                    } else {
                        mPosSdk.BleDisConnect()
                        Handler(Looper.getMainLooper()).postDelayed({
                            ReadyDialogShow(Setting.getTopContext(),"무결성 검증 중 입니다.",0)
                        },200)

                        Handler(Looper.getMainLooper()).postDelayed({
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                            mPosSdk.BleConnect(
                                mPosSdk.activity,
                                Setting.getPreference(
                                    mPosSdk.activity,
                                    Constants.BLE_DEVICE_ADDR
                                ),
                                Setting.getPreference(
                                    mPosSdk.activity,
                                    Constants.BLE_DEVICE_NAME
                                )
                            )
                        }, 500)
                        return@postDelayed
                    }
                }
                else {
                    Toast.makeText(
                        mPosSdk.getActivity(),
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
                Toast.makeText(mPosSdk.activity, "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show()
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
        mPosSdk.__BLEPosInfo(
            Utils.getDate("yyyyMMddHHmmss"),
            ResDataListener { res: ByteArray ->
                ReadyDialogHide()
                Toast.makeText(mPosSdk.getContext(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
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
                    mPosSdk.getActivity(),
                    Constants.REGIST_DEVICE_NAME,
                    authNum
                )
                Setting.setPreference(
                    mPosSdk.getActivity(),
                    Constants.REGIST_DEVICE_VERSION,
                    version
                )
                Setting.setPreference(
                    mPosSdk.getActivity(),
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
                mPosSdk.BleDisConnect()
                Handler(Looper.getMainLooper()).postDelayed({
                    ReadyDialogShow(
                        mPosSdk.getActivity(),
                        "장치 연결 중 입니다.",
                        0
                    )
                }, 200)
                Handler(Looper.getMainLooper()).postDelayed({
                    //                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                    mPosSdk.BleConnect(
                        mPosSdk.getActivity(),
                        Setting.getPreference(
                            mPosSdk.getActivity(),
                            Constants.BLE_DEVICE_ADDR
                        ),
                        Setting.getPreference(
                            mPosSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME
                        )
                    )
                }, 500)
                return@Runnable
            } else if (_bleDeviceCheck > 1) {
                _bleDeviceCheck = 0
                Toast.makeText(
                    mPosSdk.getActivity(),
                    "블루투스 통신 오류. 다시 시도해 주세요",
                    Toast.LENGTH_SHORT
                ).show()
                mPosSdk.BleDisConnect()
                return@Runnable
            } else if (_bleDeviceCheck == 0) {
                //정상
            }
        }, 2000)
    }
}