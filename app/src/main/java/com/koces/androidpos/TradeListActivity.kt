package com.koces.androidpos

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import com.koces.androidpos.sdk.*
import com.koces.androidpos.sdk.ble.bleSdkInterface.ConnectionListener
import com.koces.androidpos.sdk.ble.bleSdkInterface.ResDataListener
import com.koces.androidpos.sdk.ble.bleWoosimInterface
import com.koces.androidpos.sdk.db.sqliteDbSdk
import com.koces.androidpos.sdk.db.sqliteDbSdk.DBTradeResult
import com.koces.androidpos.sdk.van.Constants
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class TradeListActivity : BaseActivity() {
    val TAG:String = TradeListActivity::class.java.simpleName
    lateinit var instance:TradeListActivity
    lateinit var mTableLayout:TableLayout
    /** 전체:0, 당일:1, 당월:2, 달력을 선택한 경우:3 */
    var mPeriod:Int = 1
    /**
     * 0:전체,1:신용,2:현금,3:간편,4:현금IC
     */
    var mPayType:Int = 0
    /**
     * 조회 시작일 */
    var mFromDate:String = ""
    /** 조회 완료일 */
    var mToDate:String = ""
    /** 날짜 지정 버튼 */
    lateinit var mBtnAll:Button
    lateinit var mBtnDay:Button
    lateinit var mBtnMonth:Button
    /** 기간 */
    lateinit var mTvwFromDate:TextView
    lateinit var mTvwToDate:TextView
    /** 결제 타입 버튼 */
    lateinit var mBtnType_All:Button
    lateinit var mBtnType_Credit:Button
    lateinit var mBtnType_Cash:Button
    lateinit var mBtnType_Easy:Button
    lateinit var mBtnType_CashIC:Button
    /** 조회 버튼 */
    lateinit var mBtnSearch:Button

    /** 복수가맹점일 때 TID 선택DIalog */
    lateinit var mTidDialog: TermIDSelectDialog

    val mKocesPosSdk:KocesPosSdk = KocesPosSdk.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_list)
        initRes()
        Setting.setIsAppForeGround(1);
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mKocesPosSdk.BleUnregisterReceiver(this)
        mKocesPosSdk.BleregisterReceiver(Setting.getTopContext())
    }

    fun initRes(){
        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this)
        instance = this
        mKocesPosSdk.BleregisterReceiver(this)
        //상단 버튼 설정 , 날짜 버튼 그룹
        SetDateButtonGroup()
        //달력 설정
        SetCalender(0)
        //결제 타입 버튼 설정, 조회 버튼
        SetPayTypeButtonGroup()
        //조회 버튼
        getDbSearch()

        //모든 결제 데이터 표시
        addTradeData(getTid())



        mKocesPosSdk.BleConnectionListener(ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mKocesPosSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
//                ReadyDialogHide()
                Handler(Looper.getMainLooper()).post {
                    if (Setting.getBleName() == Setting.getPreference(
                            mKocesPosSdk.activity,
                            Constants.BLE_DEVICE_NAME
                        )
                    ) {
                        BleDeviceInfo()
                    } else {
                        Setting.setPreference(
                            mKocesPosSdk.activity,
                            Constants.BLE_DEVICE_NAME,
                            Setting.getBleName()
                        )
                        Setting.setPreference(
                            mKocesPosSdk.activity,
                            Constants.BLE_DEVICE_ADDR,
                            Setting.getBleAddr()
                        )
                        setBleInitializeStep()
                    }
                }
//                setBleInitializeStep()
            } else {
                ReadyDialogHide();
            }
        })
        mKocesPosSdk.BleWoosimConnectionListener(bleWoosimInterface.ConnectionListener { result: Boolean ->
            if (result == true) {
//                Toast.makeText(mKocesPosSdk.getActivity(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
//                ReadyDialogHide()
                Handler(Looper.getMainLooper()).post {
                    if (Setting.getBleName() == Setting.getPreference(
                            mKocesPosSdk.activity,
                            Constants.BLE_DEVICE_NAME
                        )
                    ) {
                        BleDeviceInfo()
                    } else {
                        Setting.setPreference(
                            mKocesPosSdk.activity,
                            Constants.BLE_DEVICE_NAME,
                            Setting.getBleName()
                        )
                        Setting.setPreference(
                            mKocesPosSdk.activity,
                            Constants.BLE_DEVICE_ADDR,
                            Setting.getBleAddr()
                        )
                        setBleInitializeStep()
                    }
                }
//                setBleInitializeStep()
            } else {
                ReadyDialogHide();
            }
        })

        //하단 버튼 처리


        //어플 최소화
        val btnMinimum = findViewById<Button>(R.id.trade_btn_home)
        btnMinimum.setOnClickListener {
            val intent = Intent(this@TradeListActivity, Main2Activity::class.java) // for tabslayout
//            intent.flags =
//                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("AppToApp",2)
            startActivity(intent)
        }
        //가맹점등록
        val btnStoreInfo = findViewById<Button>(R.id.trade_btn_store_info)
        btnStoreInfo.setOnClickListener {

            val intent = Intent(this@TradeListActivity, StoreMenuActivity::class.java) // for tabslayout
//            intent.flags =
//                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        //환경설정이동
        val btnEnvironment = findViewById<Button>(R.id.trade_btn_setting);
        btnEnvironment.setOnClickListener {
            val intent = Intent(this@TradeListActivity, menu2Activity::class.java)
//            intent.flags =
//                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            intent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    /** DB로 부터 데이터를 수산하여 TableLayout에 추가 하는 함수 */
    fun addTradeData(_tid:String) {
        mTableLayout = findViewById(R.id.trade_tbl_layout)
        mTableLayout.removeAllViews()
        var tradedata: ArrayList<DBTradeResult> = ArrayList()
        //만약 거래내역이 없어서 null 이 날라오는 경우에는 그냥 리턴 시킨다.
        /**
         * mPeriod = 전체:0, 당일:1, 당월:2, 달력을 선택한 경우:3
         * mPayType = 0:전체,1:신용,2:현금,3:간편,4:현금IC
         */
        if(mPeriod==0) {
            if(mPayType==0)
            {
                tradedata = mKocesPosSdk.sqliteDB_SelectTradeData ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==1)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Credit) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==2)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Cash) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==3)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.EasyPay) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==4)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.CAT_CashIC) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
        }
        else if(mPeriod==1) {
            if(mPayType==0)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,"",mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==1)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Credit,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==2)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Cash,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==3)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.EasyPay,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==4)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.CAT_CashIC,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
        }
        else if(mPeriod==2) {
            if(mPayType==0)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,"",mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==1)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Credit,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==2)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Cash,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==3)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.EasyPay,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==4)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.CAT_CashIC,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
        }
        else if(mPeriod==3) {
            if(mPayType==0)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,"",mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==1)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Credit,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==2)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.Cash,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==3)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.EasyPay,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
            else if(mPayType==4)
            {
                tradedata = mKocesPosSdk.getSqliteDB_SelectTradeData(_tid,sqliteDbSdk.TradeMethod.CAT_CashIC,mFromDate,mToDate) ?:
                        return    Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            }
        }
//        if(mPeriod==1){
//            //tradedata = mKocesPosSdk.getSqliteDB_SelectTradeListParsingData(getTid())
//        }

        if(tradedata.size == 0){
            Toast.makeText(this@TradeListActivity,"해당 데이터가 없습니다",Toast.LENGTH_SHORT).show()
            return
        }

//        var sdf = SimpleDateFormat("yyMMddHHmmss")
//        var dateNow:String = sdf.format(Date())
//
//        var date:Date
//        if (tradedata[tradedata.size-1].auDate.count() == 14)
//        {
//            sdf = SimpleDateFormat("yyyyMMddHHmmss")
//        }
//        date = sdf.parse(tradedata[tradedata.size-1].auDate)
//
//        dateNow = sdf.format(date)
//
//        sdf = SimpleDateFormat("yy.MM.dd")
//        dateNow = sdf.format(date)
//        mTvwFromDate.text = dateNow
//        mFromDate = dateNow

        for (rows: Int in 0 until(tradedata.size)){
            var tr: TableRow = TableRow(this)
            tr.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tr.id = tradedata[rows].getid().toInt()
            for (i: Int in 1..4) {
                val tvw: TextView = TextView(this)
                tvw.text = when(i){
                    1 -> parsingDate(tradedata[rows].auDate)
                    2 -> {
                        var TotalMoney:Int = tradedata[rows].money.toInt() + tradedata[rows].tax.toInt() + tradedata[rows].svc.toInt()

                        var TradeType:String = tradedata[rows].trade
                        if (TradeType.contains("(CAT)")) { //CAT 거래인 경우
                            TotalMoney = tradedata[rows].money.toInt() + tradedata[rows].tax.toInt() + tradedata[rows].svc.toInt() + tradedata[rows].txf.toInt()
                        }
                        Utils.PrintMoney((TotalMoney).toString())
                    }
                    3 -> tradedata[rows].trade
                    4 -> if(tradedata[rows].cancel==sqliteDbSdk.TradeMethod.NoCancel) "승인" else "취소"
                    else -> ""
                }

                //거래 취소의 경우에는 취소 글씨를 붉은색으로 표시 22.03.4 kim.jy
                if(tvw.text.toString()=="취소") tvw.setTextColor(Color.RED)
                tr.id = tradedata[rows].getid().toInt()
                tvw.layoutParams = TableRow.LayoutParams(0, (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics)).toInt(), when (i) {
                    1 -> 3.0f
                    2 -> 2.5f
                    3 -> 1.5f
                    4 -> 1.0f
                    else -> 1.0f
                })
                tvw.gravity = when (i) {
                    1 -> Gravity.RIGHT
                    2 -> Gravity.RIGHT
                    3 -> Gravity.CENTER
                    4 -> Gravity.CENTER
                    else -> Gravity.CENTER
                }
                tr.addView(tvw)
                tr.setOnClickListener(datalistListener())
            }
            var trline: TableRow = TableRow(this)
            trline.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val line:View = View(this)
            line.layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)).toInt())
            line.background = AppCompatResources.getDrawable(this,R.drawable.dash_thin_line)
            trline.addView(line)
            mTableLayout.addView(tr)
            mTableLayout.addView(trline)
        }
    }
    /** db로 부터 수산 날짜에 //::를 추가하기 위한 함수 */
    fun parsingDate(date:String):String{
        val ba: ByteArray = date.toByteArray()
        if(ba.size>12){
            ba.dropLast(ba.size - 12)
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
        return ba2.toString(Charset.defaultCharset())
    }
    /** 날짜 지정 버튼 처리 함수 */
    fun SetDateButtonGroup(){
        mBtnAll = findViewById(R.id.trade_btn_dateall)
        mBtnDay = findViewById(R.id.trade_btn_dateday)
        mBtnMonth = findViewById(R.id.trade_btn_datemonth)

        mBtnAll.setOnClickListener(BtnPeriodListener())
        mBtnDay.setOnClickListener(BtnPeriodListener())
        mBtnMonth.setOnClickListener(BtnPeriodListener())
    }
    /** 날짜 지정 버튼 처리 함수 */
    fun SetCalender(period:Int){
        mTvwFromDate = findViewById(R.id.trade_tvw_startdate)
        mTvwToDate = findViewById(R.id.trade_tvw_enddate)

        var sdf = SimpleDateFormat("yyMMdd")
        var dateNow:String = sdf.format(Date())

        mFromDate = dateNow
        mToDate = dateNow

        sdf = SimpleDateFormat("yy.MM.dd")
        dateNow = sdf.format(Date())
        mTvwFromDate.text = dateNow


        mTvwFromDate.setOnClickListener {
           DatePickerDialog(this,DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
               setTextViewDate(mTvwFromDate,year,month,dayOfMonth);
           },
               Calendar.getInstance().get(Calendar.YEAR),
               Calendar.getInstance().get(Calendar.MONTH),
               Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show()
        }

        mTvwToDate.text = dateNow




        mTvwToDate.setOnClickListener {
            DatePickerDialog(this,DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                setTextViewDate(mTvwToDate,year,month,dayOfMonth);
            },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show()
        }

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
    /** 결제 종류를 지정하는 버튼 처리 함수 */
    fun SetPayTypeButtonGroup(){
        mBtnType_All    = findViewById(R.id.trade_btn_typeall)
        mBtnType_Credit = findViewById(R.id.trade_btn_typecredit)
        mBtnType_Cash   = findViewById(R.id.trade_btn_typecash)
        mBtnType_Easy   = findViewById(R.id.trade_btn_typeeasy)
        mBtnType_CashIC =findViewById(R.id.trade_btn_typeetc)

        mBtnType_All.setOnClickListener(BtnPayTypeListener())
        mBtnType_Credit.setOnClickListener(BtnPayTypeListener())
        mBtnType_Cash.setOnClickListener(BtnPayTypeListener())
        mBtnType_Easy.setOnClickListener(BtnPayTypeListener())
        mBtnType_CashIC.setOnClickListener(BtnPayTypeListener())
    }
    /** 조회 처리 함수  */
    fun getDbSearch(){
        mBtnSearch = findViewById(R.id.trade_btn_search)
        mBtnSearch.setOnClickListener {
            if (Setting.getPreference(mKocesPosSdk.activity,Constants.MULTI_STORE) != "")
            {
                mTidDialog = TermIDSelectDialog(this@TradeListActivity, true, object:TermIDSelectDialog.DialogBoxListener {
                    override fun onClickCancel(_msg: String) {
                        Toast.makeText(this@TradeListActivity,_msg,Toast.LENGTH_SHORT).show()
                    }

                    override fun onClickConfirm(_tid: String,_storeName:String,_storeAddr:String,_storeNumber:String,_storePhone:String,_storeOwner:String) {
                        addTradeData(_tid.replace(" ",""))
                    }
                })
                mTidDialog.show()
            }
            else
            {
                addTradeData(getTid())
            }
        }
    }
    /**
     * 월에 + 1을 하는 이유 이 녀석만 아래처럼 정의 되어 있어서
     *  public final static int JANUARY = 0;
     *  */
    fun setTextViewDate(tvw:TextView,year:Int,month:Int,day:Int){
        mPeriod = 3 //날짜를 선택해서 값을 변경 한 경우 조회시에 달력을 기준으로 조회 한다.
        var y:String = year.toString().substring(2,4)
        var m:String = if((month+1)<10) "0${month+1}" else "${month+1}"
        var d:String = if(day<10) "0${day}" else "$day"


        if(tvw==mTvwFromDate){
            val tmp:String = "$y$m$d"
            if(tmp.toInt()>mToDate.toInt()){
                Toast.makeText(this@TradeListActivity,"조회 시작일이 완료일보다 빠를 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            mFromDate = "$y$m$d"
        }else{
            val tmp:String = "$y$m$d"
            if(tmp.toInt() < mFromDate.toInt()){
                Toast.makeText(this@TradeListActivity,"조회 완료일이 시작일보다 빠를 수 없습니다",Toast.LENGTH_SHORT).show()
                return
            }
            mToDate = "$y$m$d"
        }
        tvw.text = "$y.$m.$d"
    }

    /** 테이블 뷰에서 항목을 클릭한 경우의 이벤트 처리 */
    inner class datalistListener:View.OnClickListener{
        override fun onClick(v: View?) {
            Log.d(TAG, v?.id.toString())
            v?.setBackgroundColor(Color.rgb(220,220,220))

            var _trade:String = ""
            var tradedata: ArrayList<DBTradeResult> = ArrayList()
            tradedata = mKocesPosSdk.sqliteDB_SelectTradeData ?: return
            for (rows: Int in 0 until(tradedata.size)){
                if (v?.id.toString() == tradedata[rows].getid().toString())
                {
                    _trade = tradedata[rows].trade
                }
            }

            if (_trade == sqliteDbSdk.TradeMethod.CAT_Cash ||
                _trade == sqliteDbSdk.TradeMethod.Cash ||
                _trade == sqliteDbSdk.TradeMethod.CAT_CashIC)
            {
                intent = Intent(this@TradeListActivity,ReceiptCashActivity::class.java)
            }
            else if (_trade == sqliteDbSdk.TradeMethod.CAT_App ||
                _trade == sqliteDbSdk.TradeMethod.Zero ||
                _trade == sqliteDbSdk.TradeMethod.Kakao ||
                _trade == sqliteDbSdk.TradeMethod.AppCard ||
                _trade == sqliteDbSdk.TradeMethod.EmvQr ||
                _trade == sqliteDbSdk.TradeMethod.EasyPay||
                _trade == sqliteDbSdk.TradeMethod.CAT_Zero ||
                _trade == sqliteDbSdk.TradeMethod.CAT_Kakao ||
                _trade == sqliteDbSdk.TradeMethod.CAT_Ali ||
                _trade == sqliteDbSdk.TradeMethod.CAT_We)
            {
                intent = Intent(this@TradeListActivity,ReceiptEasyActivity::class.java)
            }
            else
            {
                intent = Intent(this@TradeListActivity,ReceiptCreditActivity::class.java)
            }


            intent.putExtra("data",v?.id.toString())
            startActivity(intent)
        }

    }

    inner class BtnPeriodListener:View.OnClickListener{
        override fun onClick(v: View?) {
            mBtnAll.setBackgroundResource(R.drawable.segmentbtnright_out)
            mBtnDay.setBackgroundResource(R.drawable.segmentbtnleft_out)
            mBtnMonth.setBackgroundResource(R.drawable.segmentbtncenter_out)
            mBtnAll.setTextColor(Color.parseColor("#0f74fa"))
            mBtnDay.setTextColor(Color.parseColor("#0f74fa"))
            mBtnMonth.setTextColor(Color.parseColor("#0f74fa"))
            when(v?.id){
                R.id.trade_btn_dateall -> {
                    mPeriod = 0
                    SetCalender(0)
                    mBtnAll.setBackgroundResource(R.drawable.segmentbtnright_normal)
                    mBtnAll.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.trade_btn_dateday -> {
                    mPeriod = 1
                    SetCalender(1)
                    mBtnDay.setBackgroundResource(R.drawable.segmentbtnleft_normal)
                    mBtnDay.setTextColor(Color.parseColor("#FFFFFF"))
                }
                R.id.trade_btn_datemonth -> {
                    mPeriod = 2
                    SetCalender(2)
                    mBtnMonth.setBackgroundResource(R.drawable.segmentbtncenter_normal)
                    mBtnMonth.setTextColor(Color.parseColor("#ffffff"))
                }
                else->{}
            }
        }

    }

    inner class BtnPayTypeListener:View.OnClickListener{
        override fun onClick(v: View?) {
            testResetPayTypeButton()
            when(v?.id){
                R.id.trade_btn_typeall -> {
                    mPayType = 0
                    mBtnType_All.setBackgroundResource(R.drawable.segmentsubbtnleft_normal)
                    mBtnType_All.setTextColor(Color.parseColor("#7b28ef"))
                }
                R.id.trade_btn_typecredit -> {
                    mPayType = 1
                    mBtnType_Credit.setBackgroundResource(R.drawable.segmentsubbtncenter_normal)
                    mBtnType_Credit.setTextColor(Color.parseColor("#7b28ef"))
                }
                R.id.trade_btn_typecash -> {
                    mPayType = 2
                    mBtnType_Cash.setBackgroundResource(R.drawable.segmentsubbtncenter_normal)
                    mBtnType_Cash.setTextColor(Color.parseColor("#7b28ef"))
                }
                R.id.trade_btn_typeeasy ->{
                    mPayType = 3
                    mBtnType_Easy.setBackgroundResource(R.drawable.segmentsubbtncenter_normal)
                    mBtnType_Easy.setTextColor(Color.parseColor("#7b28ef"))

                }
                R.id.trade_btn_typeetc ->{
                    mPayType = 4
                    mBtnType_CashIC.setBackgroundResource(R.drawable.segmentsubbtnright_normal)
                    mBtnType_CashIC.setTextColor(Color.parseColor("#7b28ef"))

                }
                else->{}
            }
        }

    }

    fun getTid():String{
        return Setting.getPreference(mKocesPosSdk.activity, Constants.TID);
    }

    fun testResetPayTypeButton(){
        mBtnType_All.setBackgroundResource(R.drawable.segmentsubbtnleft_out)
        mBtnType_Credit.setBackgroundResource(R.drawable.segmentsubbtncenter_out)
        mBtnType_Cash.setBackgroundResource(R.drawable.segmentsubbtncenter_out)
        mBtnType_Easy.setBackgroundResource(R.drawable.segmentsubbtncenter_out)
        mBtnType_CashIC.setBackgroundResource(R.drawable.segmentsubbtnright_out)
        mBtnType_All.setTextColor(Color.parseColor("#505050"))
        mBtnType_Credit.setTextColor(Color.parseColor("#505050"))
        mBtnType_Cash.setTextColor(Color.parseColor("#505050"))
        mBtnType_Easy.setTextColor(Color.parseColor("#505050"))
        mBtnType_CashIC.setTextColor(Color.parseColor("#505050"))
    }

    var _bleCount = 0
    private fun setBleInitializeStep() {
        ReadyDialogShow(Setting.getTopContext(), "무결성 검증 중 입니다.", 0)
        _bleCount += 1
        /* 무결성 검증. 초기화진행 */
        val deviceSecuritySDK = DeviceSecuritySDK(this) { result: String, Code: String?, state: String?, resultData: HashMap<String?, String?>? ->
            if (result == "00") {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1) //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
                //                ReadyDialogHide();
            } else {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1)
                //                ReadyDialogHide();
            }
            Handler(Looper.getMainLooper()).postDelayed({
                if (result == "00") {
                    Toast.makeText(
                        mKocesPosSdk.getActivity(),
                        "무결성 검증에 성공하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (result == "9999") {

                    if (_bleCount > 1) {
                        _bleCount = 0
                        Toast.makeText(
                            mKocesPosSdk.activity,
                            "네트워크 오류. 다시 시도해 주세요",
                            Toast.LENGTH_SHORT
                        ).show()

                        mKocesPosSdk.BleDisConnect()

                    } else {
                        mKocesPosSdk.BleDisConnect()
                        Handler(Looper.getMainLooper()).postDelayed({
                            ReadyDialogShow(Setting.getTopContext(),"무결성 검증 중 입니다.",0)
                        },200)
                        Handler(Looper.getMainLooper()).postDelayed({
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                            mKocesPosSdk.BleConnect(
                                mKocesPosSdk.activity,
                                Setting.getPreference(
                                    mKocesPosSdk.activity,
                                    Constants.BLE_DEVICE_ADDR
                                ),
                                Setting.getPreference(
                                    mKocesPosSdk.activity,
                                    Constants.BLE_DEVICE_NAME
                                )
                            )
                        }, 500)
                        return@postDelayed
                    }
                }
                else {
                    Toast.makeText(
                        mKocesPosSdk.getActivity(),
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
                Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show()
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
        mKocesPosSdk.__BLEPosInfo(
            Utils.getDate("yyyyMMddHHmmss"),
            ResDataListener { res: ByteArray ->
                ReadyDialogHide()
                Toast.makeText(mKocesPosSdk.getContext(), "연결에 성공하였습니다", Toast.LENGTH_SHORT).show()
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
                    mKocesPosSdk.getActivity(),
                    Constants.REGIST_DEVICE_NAME,
                    authNum
                )
                Setting.setPreference(
                    mKocesPosSdk.getActivity(),
                    Constants.REGIST_DEVICE_VERSION,
                    version
                )
                Setting.setPreference(
                    mKocesPosSdk.getActivity(),
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
                mKocesPosSdk.BleDisConnect()
                Handler(Looper.getMainLooper()).postDelayed({
                    ReadyDialogShow(
                        mKocesPosSdk.getActivity(),
                        "장치 연결 중 입니다.",
                        0
                    )
                }, 200)
                Handler(Looper.getMainLooper()).postDelayed({
                    //                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                    mKocesPosSdk.BleConnect(
                        mKocesPosSdk.getActivity(),
                        Setting.getPreference(
                            mKocesPosSdk.getActivity(),
                            Constants.BLE_DEVICE_ADDR
                        ),
                        Setting.getPreference(
                            mKocesPosSdk.getActivity(),
                            Constants.BLE_DEVICE_NAME
                        )
                    )
                }, 500)
                return@Runnable
            } else if (_bleDeviceCheck > 1) {
                _bleDeviceCheck = 0
                Toast.makeText(
                    mKocesPosSdk.getActivity(),
                    "블루투스 통신 오류. 다시 시도해 주세요",
                    Toast.LENGTH_SHORT
                ).show()
                mKocesPosSdk.BleDisConnect()
                return@Runnable
            } else if (_bleDeviceCheck == 0) {
                //정상
            }
        }, 2000)
    }
}