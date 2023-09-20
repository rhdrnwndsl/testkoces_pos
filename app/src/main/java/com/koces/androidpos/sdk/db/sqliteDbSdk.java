package com.koces.androidpos.sdk.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteAbortException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.TaxSdk;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Handler;

import kotlin.text.UStringsKt;


/**
 * 무결성검사, 대리점 정보 저장을 위한 sqlite sdk 클래스
 */
public class sqliteDbSdk {
    /**
     * Log를 위한 TAG 설정
     */
    private final static String TAG = "SqliteSDK";
    SQLiteDatabase mSqliteDB;
    final static String mDbName = "Integrity";
    final static int MaxCount = 10;
    final static int MaxAppToAppCount = 100;
    Context mCtx;
    /**
     *  DB에는 무결성 검사결과와 가맹점 정보만 저장 한다.
     */
    public sqliteDbSdk(Context _ctx)
    {
        mCtx = _ctx;
        OpenDB();

    }
    //DB에는 무결성 검사결과와 가맹점 정보만 저장 한다.
    //무결성 검사 결과 저장

    /**
     * 무결성 검사 insert 쿼리 함수
     * @param date 날짜
     * @param result 결과
     * @param etc 성공/실패
     */
    public void InserIntegrityData(String date,String result,String etc)
    {
        try
        {
            CheckCount(Command.DB_IntegrityTableName);
            String insert = "INSERT INTO " + Command.DB_IntegrityTableName +
                    " (date,result,etc) Values ('" + date + "','" + result + "','" + etc + "');";
            mSqliteDB.execSQL(insert);
        }
        catch (SQLiteException ex)
        {
            Log.d(TAG,ex.toString());
        }
    }
    //가맹점 데이터

    /**
     * 가맹점 등록 insert 쿼리 함수
     * @param CreditConnA1200
     * @param CreditConnB1200
     * @param EtcConnA1200
     * @param EtcConnB1200
     * @param CreditConnA2400
     * @param CreditConnB2400
     * @param EtcConnA2400
     * @param EtcConnB2400
     * @param AsNum
     * @param ShpNm
     * @param BsnNo
     * @param PreNm
     * @param ShpAdr
     * @param ShpTel
     * @param WorkingKeyIndex
     * @param WorkingKey
     * @param TMK
     * @param PointCount
     * @param PointInfo
     * @param MchData
     */
    public void InsertStoreData(String CreditConnA1200,String CreditConnB1200,String EtcConnA1200,String EtcConnB1200,String CreditConnA2400,
                                String CreditConnB2400,String EtcConnA2400,String EtcConnB2400,String AsNum,String ShpNm,String tid,String BsnNo,
                                String PreNm,String ShpAdr,String ShpTel,String WorkingKeyIndex,String WorkingKey,String TMK,String PointCount,String PointInfo,String MchData)
    {
        //기존 데이터 삭제
//        String delete = "DELETE FROM " + Command.DB_StoreTableName + " where BsnNo = '" + BsnNo  + "';";
//        mSqliteDB.execSQL(delete);

        String insert = "INSERT INTO " + Command.DB_StoreTableName +
                " (CreditConnA1200,CreditConnB1200,EtcConnA1200,EtcConnB1200,CreditConnA2400,CreditConnB2400,EtcConnA2400,EtcConnB2400" +
                ",AsNum,ShpNm,Tid,BsnNo,PreNm,ShpAdr,ShpTel,WorkingKeyIndex,WorkingKey,TMK,PointCount,PointInfo,MchData)" +
                " Values ('" + CreditConnA1200 + "','" + CreditConnB1200 + "','" + EtcConnA1200 + "','" + EtcConnB1200 + "'," +
                          "'" + CreditConnA2400 + "','" + CreditConnB2400+ "','" + EtcConnA2400+ "','" + EtcConnB2400+ "'," +
                          "'" + AsNum+ "','" + ShpNm +"','" + tid + "','" + BsnNo+ "','" + PreNm+ "'," +
                          "'" + ShpAdr+ "','" + ShpTel+ "','" + WorkingKeyIndex+ "','" + WorkingKey+ "'," +
                          "'" + TMK+ "','" + PointCount+ "','" + PointInfo+ "','" + MchData+ "');";
        mSqliteDB.execSQL(insert);
    }

    /** 22.01.26 kim.jy TID 기준으로 세금 설정 할 수 있도록 DB를 설정한다.
     * @param Tid
     * @param UseVAT 부가세사용
     * @param AutoVAT 부가세 자동, 통합
     * @param IncludeVAT 부가세 포함, 미포함
     * @param UseSVC 봉사료 사용
     * @param AutoSVC 봉사료 자동, 수동
     * @param IncludeSVC 봉사료 포함, 미포함
     * @param minInstallMentAmount 할부 최소 금액
     * @param NoSignAmount 서명 최소 금액
     */
    public void UpdateTaxSettingData(String Tid,boolean UseVAT,int AutoVAT,int IncludeVAT,int VATRate,
                                     boolean UseSVC,int AutoSVC,int IncludeSVC,int SVCRate,int minInstallMentAmount,int NoSignAmount ){
        try {
            String QueryString = "Select count(*) from " + Command.DB_TaxTableName + " where TID='" + Tid + "';";
            Cursor c = mSqliteDB.rawQuery(QueryString, null);
            c.moveToFirst();
            int Count = c.getInt(0);

            //기존 정보를 삭제 하고 다시 추가 한다. 22.02.07 kim.jy
            if (Count != 0) {
                QueryString = "delete from " + Command.DB_TaxTableName + " where TID='" + Tid + "';";
                mSqliteDB.execSQL(QueryString);
            }
            //기존의 데이터가 없으면 인서트 한다.
            QueryString = "Insert into " + Command.DB_TaxTableName + " values ('" + Tid + "','" +
                    "null','" +             //subtid를 null로 채운다.
                    String.valueOf(UseVAT) + "','" +
                    String.valueOf(AutoVAT) + "','" +
                    String.valueOf(IncludeVAT) + "','" +
                    String.valueOf(VATRate) + "','" +
                    String.valueOf(UseSVC) + "','" +
                    String.valueOf(AutoSVC) + "','" +
                    String.valueOf(IncludeSVC) + "','" +
                    String.valueOf(SVCRate) + "','" +
                    String.valueOf(minInstallMentAmount) + "','" +
                    String.valueOf(NoSignAmount) + "','null');";    //etc 값은 null을 넣는다.

            mSqliteDB.execSQL(QueryString);
        }
        catch (SQLiteException ex){
            Log.d(TAG,ex.toString());
        }

    }
    /**가명잼 설정된 세금 설정 정보를 가져 온다. */
    public HashMap<String,String> SelectSettingTaxData(String tid){
        HashMap<String,String> TaxSettingInfo = new HashMap<>();
        String selectQuery = "Select * from " + Command.DB_TaxTableName + " where TID = '" + tid + "';";
        Cursor c = mSqliteDB.rawQuery(selectQuery,null);

        while (c.moveToNext()){
            int subTid = c.getColumnIndex("SUBTID");    //예비 필드
            int vatUse = c.getColumnIndex("VATUSE");       //부가세 적용
            int vatMode = c.getColumnIndex("VATAUTO");     //부가세 자동 수동
            int vatInclude = c.getColumnIndex("VATINCLUDE");   //부가세 포함.
            int vatRate = c.getColumnIndex("VATRATE");    //부가세율
            int svcUse = c.getColumnIndex("SVCUSE");      //봉사료 적용
            int svcMode = c.getColumnIndex("SVCAUTO");      //봉사료 자동
            int svcInclude = c.getColumnIndex("SVCINCLUDE");      //봉사료 포함
            int svcRate = c.getColumnIndex("SVCRATE");      //봉사료율
            int minInstall = c.getColumnIndex("MININSTALLMENT");      //할부 최소 금액
            int noSign = c.getColumnIndex("NOSIGN");      // 서명 시작 금액
            int etc = c.getColumnIndex("ETC");      //예비 필드

            TaxSettingInfo.put("subTid", c.getString(subTid));
            TaxSettingInfo.put(TaxSdk.defVatUse, c.getString(vatUse));
            TaxSettingInfo.put(TaxSdk.defVatMode, c.getString(vatMode));
            TaxSettingInfo.put(TaxSdk.defVatInclude, c.getString(vatInclude));
            TaxSettingInfo.put(TaxSdk.defVatRate, c.getString(vatRate));
            TaxSettingInfo.put(TaxSdk.defSvcUse, c.getString(svcUse));
            TaxSettingInfo.put(TaxSdk.defSvcMdoe, c.getString(svcMode));
            TaxSettingInfo.put(TaxSdk.defSvcInclude, c.getString(svcInclude));
            TaxSettingInfo.put(TaxSdk.defSvcRate, c.getString(svcRate));
            TaxSettingInfo.put(TaxSdk.defMinInstallMent, c.getString(minInstall));
            TaxSettingInfo.put(TaxSdk.defMinNoSignAmount, c.getString(noSign));
            TaxSettingInfo.put("etc", c.getString(etc));

        }
        return TaxSettingInfo;
    }
    public HashMap<String,String> SelectStoreData(String tid, String bsn){
        HashMap<String,String> storeInfo = new HashMap<>();

        String selectQuery = "Select * from " + Command.DB_StoreTableName + " where BsnNo like '" + bsn + "'";
        Cursor c = mSqliteDB.rawQuery(selectQuery,null);
        while (c.moveToNext()) {
            int bsnNo = c.getColumnIndex("BsnNo");    //사업자번호
            int tidNo = c.getColumnIndex("Tid");       //tid
            int AdrNo = c.getColumnIndex("ShpAdr");     //사업장주소
            int OwnNo = c.getColumnIndex("PreNm");   //대표 이름
            int phoNo = c.getColumnIndex("ShpTel");    //전화번호
            int ShpNo = c.getColumnIndex("ShpNm");      //상호명
            storeInfo.put("BsnNo", c.getString(bsnNo));
            storeInfo.put("Tid", c.getString(tidNo));
            storeInfo.put("ShpAdr", c.getString(AdrNo));
            storeInfo.put("PreNm", c.getString(OwnNo));
            storeInfo.put("ShpTel", c.getString(phoNo));
            storeInfo.put("ShpNm", c.getString(ShpNo));
        }

        return storeInfo;
    }
    /**
     * db select 쿼리 함수
     * @param TableName 쿼리할 테이블 이름
     * @return String Array
     */
    public String[] SelectData(String TableName)
    {
        ArrayList<String> tmp = new ArrayList<>();
        String selectQuery = "Select * from " + TableName ;
        Cursor c = mSqliteDB.rawQuery(selectQuery,null);
        if(TableName.equals(Command.DB_IntegrityTableName)) {
            while (c.moveToNext()) {
                int co1 = c.getColumnIndex("date");
                int co2 = c.getColumnIndex("result");
                int co3 = c.getColumnIndex("etc");
                String date = c.getString(co1);
                String type = c.getString(co2);
                String etc = c.getString(co3);
                String result = type.equals("1") ? "정상" : "실패";
                String result2 = etc.equals("1") ? "구동시" : "수동";
                String data = date + "|" + result + "|" + result2;
                tmp.add(data);
            }
        }
        else if(TableName.equals(Command.DB_StoreTableName))
        {

        }
        return tmp.toArray(new String[0]);
    }

    /**
     * DB 오픈 함수
     */
    private void OpenDB()
    {
        try {
            mSqliteDB = mCtx.openOrCreateDatabase(mDbName, Context.MODE_PRIVATE, null);
            String Create = "CREATE TABLE IF NOT EXISTS " + Command.DB_IntegrityTableName + " (ID INTEGER primary key autoincrement, date VARCHAR(14), result VARCHAR(1), etc VARCHAR(1) ); ";
            mSqliteDB.execSQL(Create);
            Create = "CREATE TABLE IF NOT EXISTS " + Command.DB_StoreTableName + " (CreditConnA1200 VARCHAR(15),CreditConnB1200 VARCHAR(15),EtcConnA1200 VARCHAR(15)," +
                    "EtcConnB1200 VARCHAR(15),CreditConnA2400 VARCHAR(15),CreditConnB2400 VARCHAR(15),EtcConnA2400 VARCHAR(15),EtcConnB2400 VARCHAR(15)," +
                    "AsNum VARCHAR(15),ShpNm VARCHAR(40),Tid VARCHAR(10),BsnNo VARCHAR(10),PreNm VARCHAR(20),ShpAdr VARCHAR(50),ShpTel VARCHAR(15),WorkingKeyIndex VARCHAR(2)," +
                    "WorkingKey VARCHAR(16),TMK VARCHAR(16),PointCount VARCHAR(2),PointInfo VARCHAR(340),MchData VARCHAR(64)); ";
            mSqliteDB.execSQL(Create);
        }catch (SQLiteException ex){
                Log.d(TAG,ex.toString());
        }
            //CheckOldTable();
        try {

            String delete = "DROP TABLE TaxRecord";
            mSqliteDB.execSQL(delete);
        }catch(SQLiteException ex){
            Log.d(TAG,ex.toString());
        }
        try{
            //세금 설정 관련 저장 subtid는 나중에 예비 필드,ETC 도 예비 필드 22.01.26 kim.jy
            String Create = "CREATE TABLE IF NOT EXISTS " + Command.DB_TaxTableName + " (TID VARCHAR(15),SUBTID VARCHAR(15),VATUSE VARCHAR(1),VATAUTO VARCHAR(1),VATINCLUDE VARCHAR(1),VATRATE VARCHAR(3)," +
                    "SVCUSE VARCHAR(1),SVCAUTO VARCHAR(1),SVCINCLUDE VARCHAR(1),SVCRATE VARCHAR(3)," +
                    "MININSTALLMENT VARCHAR(10),NOSIGN VARCHAR(10),ETC TEXT" +
                    " );";
            mSqliteDB.execSQL(Create);
        }catch(SQLiteException ex){
            Log.d(TAG,ex.toString());
        }
        try{
            //거래내역 생성 코드
            String Create = "CREATE TABLE IF NOT EXISTS " + Command.DB_TradeTableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "Tid TEXT, StoreName TEXT, StoreAddr TEXT, StoreNumber TEXT, StorePhone TEXT, StoreOwner TEXT, Trade TEXT,Cancel TEXT,Money TEXT,GiftAmt TEXT," +
                    "Tax TEXT,Svc TEXT,Txf TEXT,Inst TEXT,CashTarget TEXT,CashInputType TEXT,CashNum TEXT," +
                    "CardNum TEXT,CardType TEXT,CardInpNm TEXT,CardIssuer TEXT," +
                    "MchNo TEXT,AuDate TEXT,OriAuData TEXT, AuNum TEXT, OriAuNum TEXT, TradeNo TEXT, Message TEXT," +
                    "KakaoMessage TEXT,PayType TEXT,KakaoAuMoney TEXT,KakaoSaleMoney TEXT,KakaoMemberCd TEXT," +
                    "KakaoMemberNo TEXT,Otc TEXT,Pem TEXT,Trid TEXT,CardBin TEXT,SearchNo TEXT," +
                    "PrintBarcd TEXT,PrintUse TEXT,PrintNm TEXT,MchFee TEXT,MchRefund TEXT);";
            mSqliteDB.execSQL(Create);
        }catch(SQLiteException ex){
            Log.d(TAG,ex.toString());
        }
        try{
            //100일 이전 거래 내역 삭제
            String previousDt = getPreviouseDate(-100);
            String Delete100 = "DELETE FROM Trade where CAST(subStr(AuDate,1,6) as INTEGER) < " + previousDt + ";" ;
            mSqliteDB.execSQL(Delete100);
        }
        catch (SQLiteException ex)
        {
            Log.d(TAG,ex.toString());
        }

        try{
            //앱투앱거래내역 생성 코드
            String Create = "CREATE TABLE IF NOT EXISTS " + Command.DB_AppToAppTableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "TrdType TEXT, TermID TEXT, TrdDate TEXT, AnsCode TEXT, Message TEXT, AuNo TEXT, TradeNo TEXT,CardNo TEXT,Keydate TEXT,MchData TEXT," +
                    "CardKind TEXT,OrdCd TEXT,OrdNm TEXT,InpCd TEXT,InpNm TEXT,DDCYn TEXT,EDCYn TEXT," +
                    "GiftAmt TEXT,MchNo TEXT,BillNo TEXT,DisAmt TEXT," +
                    "AuthType TEXT,AnswerTrdNo TEXT,ChargeAmt TEXT, RefundAmt TEXT, QrKind TEXT," +
                    "OriAuDate TEXT, OriAuNo TEXT, TrdAmt TEXT, TaxAmt TEXT, SvcAmt TEXT, TaxFreeAmt TEXT, Month TEXT);";
            mSqliteDB.execSQL(Create);
        }catch(SQLiteException ex){
            Log.d(TAG,ex.toString());
        }
        try{
            //100일 이전 거래 내역 삭제
            String previousDt = getPreviouseDate(-100);
            String Delete100 = "DELETE FROM TrdDate where CAST(subStr(AuDate,1,6) as INTEGER) < " + previousDt + ";" ;
            mSqliteDB.execSQL(Delete100);
        }
        catch (SQLiteException ex)
        {
            Log.d(TAG,ex.toString());
        }
    }

    private String getPreviouseDate(int ValueDay) {
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

        calendar.add(Calendar.DATE, ValueDay);
        String chkDate = SDF.format(calendar.getTime());
        return chkDate;
    }
    private void CloseDB()
    {
        if(mSqliteDB!=null)
        {
            mSqliteDB.close();
        }
    }
    private void CheckOldTable(){
        //taxtable 에 vatrate가 없으면 테이블 삭제 하고 재생성 한다.
        String CheckQuery = "Select * from " + Command.DB_TaxTableName + ";";
        Cursor c = mSqliteDB.rawQuery(CheckQuery,null);

        int columnsCount = c.getColumnCount();

        Log.d(TAG,Command.DB_TaxTableName + "ColumnsCount : " + String.valueOf(columnsCount));

    }
    //현재 기록하고 있는 무결성 검사 리스트가 10개를 넘으면 제일 처음 기록한 데이터를 삭제 한다.

    /**
     * 무결성 검사가 10개를 넘으면 최초 저장 필드를 삭제 하는 함수
     * @param _tableName 테이블 이름
     */
    private void CheckCount(String _tableName)
    {
        int ColumnsCount = 0;
        String selectQuery = "Select * from " + _tableName ;
        Cursor c = mSqliteDB.rawQuery(selectQuery,null);

        while (c.moveToNext())
        {
            ColumnsCount++;
        }

        if(ColumnsCount> (MaxCount-1))
        {
            String delete = "DELETE FROM " + Command.DB_IntegrityTableName + " where ID = (SELECT MIN(ID) FROM " + Command.DB_IntegrityTableName + ");";
            mSqliteDB.execSQL(delete);
        }
    }

    /// 거래내역 입력
    /// - Parameters:
    ///   - _Tid : TermID
    ///   - _storeName : 가맹점이름
    ///   - _storeAddr : 가맹점주소
    ///   - _storeNumber : 사업자번호
    ///   - _storePhone : 가맹점전화번호
    ///   - _storeOwner : 가맹점주
    ///   - _Trade: <#_Trade description#>
    ///   - _cancel: <#_cancel description#>
    ///   - _money: <#_money description#>
    ///   - _giftAmt: 선불카드 잔액
    ///   - _tax: 세금
    ///   - _Scv: 봉사료
    ///   - _Txf: 비과세
    ///   - _InstallMent: 할부
    ///   - _CashTarget: <#_CashTarget description#>
    ///   - _CashInputType: <#_CashInputType description#>
    ///   - _CashNum: <#_CashNum description#>
    ///   - _CardNum: <#_CardNum description#>
    ///   - _CardType: <#_CardType description#>
    ///   - _CardInpNm: <#_CardInpNm description#>
    ///   - _CardIssuer: <#_CardIssuer description#>
    ///   - _MchNo: <#_MchNo description#>
    ///   - _AuDate: 승인 날짜
    ///   - _OriAuDate: 취소시에 원거래일자 기록
    ///   - _AuNum: 인증 번호
    public void InsertTrade(
                            String _Tid,
                            String _storeName,  //가맹점이름
                            String _storeAddr,  //가맹점주소
                            String _storeNumber,    //사업자번호
                            String _storePhone,     //가맹점전화번호
                            String _storeOwner,     //가맹점주
                            String _Trade,
                            String _cancel, //취소여부
                            int _money, //금액
                            String _giftamt,// 선불카드잔액
                            int _tax,//세금
                            int _Scv,  //봉사료
                            int _Txf, //비과세
                            int _InstallMent,
                            String _CashTarget,
                            String _CashInputType, //현금영수증발급형태
                            String _CashNum, // 현금발급번호
                            String _CardNum, //  카드번호
                            String _CardType, // 카드종류
                            String _CardInpNm, // 카드매입사
                            String _CardIssuer, // 카드발급사
                            String _MchNo,// 가맹점번호
                            String _AuDate, // 승인날짜
                            String _OriAuDate, // 원거래일자
                            String _AuNum,// 승인번호
                            String _OriAuNum,// 원승인번호
                            String _TradeNo,
                            String _Message,// Message
                            String _KakaoMessage,// KakaoMessage
                            String _PayType, // PayType
                            String _KakaoAuMoney, // KakaoAuMoney
                            String _KakaoSaleMoney,// KakaoSaleMoney
                            String _KakaoMemberCd, // KakaoMemberCd
                            String _KakaoMemberNo, // KakaoMemberNo
                            String _Otc,// Otc
                            String _Pem, //Pem
                            String _Trid,// Trid
                            String _CardBin, // CardBin
                            String _SearchNo, // SearchNo
                            String _PrintBarcd,// PrintBarcd
                            String _PrintUse, // PrintUse
                            String _PrintNm,    //PrintNm
                            String _MchFee,// MchFee,
                            String _MchRefund// MchRefund
                            ){
        //여기서 부터는 할지 말지 결정되지 않은 상황에서 모양세를 위해서 하는 작업임
        //예를 들어 취소를 한 경우에 이 거래가 며칠전의 거래의 경우 업데이트 하게 되면 취소 했는지 알 수 있는 방법이 리스트를 올려서 며칠전 거래 내약을 조회 해야 한다.
        //이를 막기 위해서 캔슬이 들어오는 경우 기존 거래를 삭제 한다.
        if(_cancel.equals(TradeMethod.Cancel)) {
            DeleteTradeList( _OriAuDate.replace(" ",""), _OriAuNum.replace(" ",""));
        }

        String temp1 = "Insert INTO " + Command.DB_TradeTableName +
                " (Tid,StoreName,StoreAddr,StoreNumber,StorePhone,StoreOwner,Trade,Cancel,Money,GiftAmt,Tax,Svc,Txf,Inst,CashTarget," +
                "CashInputType,CashNum,CardNum,CardType,CardInpNm,CardIssuer,MchNo,AuDate,OriAuData,AuNum," +
                "OriAuNum,TradeNo,Message,KakaoMessage,PayType,KakaoAuMoney,KakaoSaleMoney,KakaoMemberCd,KakaoMemberNo,Otc," +
                "Pem,Trid,CardBin,SearchNo,PrintBarcd,PrintUse,PrintNm,MchFee,MchRefund) Values ";
        String temp2 = "('" + _Tid + "','" + _storeName + "','" + _storeAddr + "','" + _storeNumber + "','" + _storePhone + "','" + _storeOwner + "','" +
                _Trade + "','" + _cancel + "','" + String.valueOf(_money) + "','" + String.valueOf(_giftamt) + "','" +
                String.valueOf(_tax) + "','" + String.valueOf(_Scv) + "','" + String.valueOf(_Txf) + "','" + String.valueOf(_InstallMent) + "','" +
                _CashTarget + "','" + _CashInputType + "','" + _CashNum + "','" + _CardNum + "','" + _CardType + "','" +
                _CardInpNm + "','" + _CardIssuer + "','" + _MchNo + "','" + _AuDate.replace(" ","") + "','" + _OriAuDate.replace(" ","") + "','" + _AuNum.replace(" ","") + "','" +
                _OriAuNum.replace(" ","") + "','" + _TradeNo + "','" +  _Message + "','" + _KakaoMessage + "','" + _PayType + "','" + _KakaoAuMoney + "','" + _KakaoSaleMoney + "','" +
                _KakaoMemberCd + "','" + _KakaoMemberNo + "','" + _Otc + "','" + _Pem + "','" + _Trid + "','" + _CardBin + "','" +
                _SearchNo + "','" + _PrintBarcd + "','" + _PrintUse + "','" + _PrintNm + "','" + _MchFee + "','" + _MchRefund + "');";

        String Insert = temp1 + temp2;
        mSqliteDB.execSQL(Insert);
        if (_Trade.equals(TradeMethod.Credit)){
            Log.d(TAG, "신용거래 추가 DB : " + Insert);
        } else if (_Trade.equals(TradeMethod.Cash)) {
            Log.d(TAG, "현금거래 추가 DB : " + Insert);
        } else if (_Trade.equals(TradeMethod.Kakao)){
            Log.d(TAG, "카카오거래 추가 DB : " + Insert);
        }
    }
    /* _OriAuDate:원거래일자,_OriAuNum:원승인번호 */
    private void DeleteTradeList(String _OriAuDate,String _OriAuNum)
    {
        try {
            String temp1 = "DELETE from " + Command.DB_TradeTableName + " Where AuNum = '" + _OriAuNum + "' AND AuDate like '" + _OriAuDate + "%';";
            mSqliteDB.execSQL(temp1);
        }catch (SQLiteException ex){
            Log.d(TAG,ex.toString());
        }
    }

    public int getTradeListCount() {
        int count = 0;
        String queryString = "SELECT count(*) FROM " + Command.DB_TradeTableName + ";";
        try {
            Cursor cursor = mSqliteDB.rawQuery(queryString, null);
            if (null != cursor)
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = cursor.getInt(0);
                }
            cursor.close();
        }catch (SQLiteException ex){
            return 0;
        }
        return count;
    }
    /// 거래 내역 리스트 가져 오는 함수
    /// - Returns: DbTradeResult 배열
     public ArrayList<DBTradeResult> getTradeAllList(){

         ArrayList<DBTradeResult> result = new ArrayList<>();
         try {
             String SelectQuery = "SELECT * FROM " + Command.DB_TradeTableName + " ORDER BY id DESC;";
             Cursor c = mSqliteDB.rawQuery(SelectQuery, null);

         while(c.moveToNext()){
            @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
            @SuppressLint("Range") String _tid = c.getString(c.getColumnIndex( "Tid"));
             @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
             @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
             @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
             @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
             @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
            @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
            @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
            @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
            @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
            @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
            @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
            @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
            @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
            @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
            @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
            @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
            @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
            @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
            @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
            @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
            @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
            @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
            @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
            @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
            @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
            @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
            @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
            @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
            @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
            @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
            @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
            @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
            @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
            @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
            @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
            @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
            @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
            @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
            @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
            @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
            @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
            @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
            @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

            DBTradeResult a = new DBTradeResult(Integer.parseInt(_id),_tid,_storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                    _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                    _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                    _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                    _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);

            result.add(a);
            }
         }catch (SQLiteException ex){
             return null;
         }
        return result;
    }

    /** 지정한 Tid 에 대해서만 가져온다 */
    public ArrayList<DBTradeResult> getTradeTIDAllList(String tid){

        ArrayList<DBTradeResult> result = new ArrayList<>();
        try {
            String SelectQuery = "";
            if(tid.equals(""))
            {
                SelectQuery = "SELECT * FROM " + Command.DB_TradeTableName + " ORDER BY id DESC;";
            }
            else
            {
                SelectQuery = "SELECT * FROM " + Command.DB_TradeTableName + " where Tid='" + tid + "';";
            }

            Cursor c = mSqliteDB.rawQuery(SelectQuery, null);

            while(c.moveToNext()){
                @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
                @SuppressLint("Range") String _tid = c.getString(c.getColumnIndex( "Tid"));
                @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
                @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
                @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
                @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
                @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
                @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
                @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
                @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
                @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
                @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
                @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
                @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
                @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
                @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
                @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
                @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
                @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
                @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
                @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
                @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
                @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
                @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
                @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
                @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
                @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
                @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
                @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
                @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
                @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
                @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
                @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
                @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
                @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
                @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
                @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
                @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
                @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
                @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
                @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
                @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
                @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
                @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
                @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

                DBTradeResult a = new DBTradeResult(Integer.parseInt(_id),_tid,_storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                        _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                        _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                        _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                        _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);

                result.add(a);
            }
        }catch (SQLiteException ex){
            return null;
        }
        return result;
    }

    /** 지정한TID 거래내역 중 신용,현금,간편,현금IC 로 분류별로 가져 온다. */
    public ArrayList<DBTradeResult> getTradeTIDTradeTypeList(String _tid, String _trade){

        ArrayList<DBTradeResult> result = new ArrayList<>();
        try {
            String queryStatementString = "";
            if (_trade.equals(TradeMethod.EasyPay)) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  " +
                            "AND (Trade like '" + TradeMethod.Kakao + "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat + "'" +
                            "OR Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr +
                            "' OR Trade like '" + TradeMethod.CAT_Ali + "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                            "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR " + "Trade like '" + TradeMethod.CAT_App + "') ORDER BY id DESC";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where  (Trade like '" + TradeMethod.Kakao +
                            "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat + "' OR  Trade like '" + TradeMethod.Ali +
                            "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr + "' OR Trade like '" + TradeMethod.CAT_Ali +
                            "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                            "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "') ORDER BY id DESC;";
                }

            } else if(_trade.equals(TradeMethod.NULL)) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  ORDER BY id DESC;";
                } else {
                    queryStatementString =  "SELECT * FROM " + Command.DB_TradeTableName + " ORDER BY id DESC;";
                }
            } else if (_trade.equals(TradeMethod.Credit)) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                            "Trade like '" + TradeMethod.Credit + "' OR  Trade like '" + TradeMethod.CAT_Credit + "'  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.Credit +
                            "' OR  Trade like '" + TradeMethod.CAT_Credit + "'  ORDER BY id DESC;";
                }
            } else if (_trade.equals(TradeMethod.Cash)) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                            "Trade like '" + TradeMethod.Cash + "' OR  Trade like '" + TradeMethod.CAT_Cash + "' ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.Cash +
                            "' OR  Trade like '" + TradeMethod.CAT_Cash + "' ORDER BY id DESC;";
                }
            } else if (_trade.equals(TradeMethod.CAT_CashIC)) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " Trade like '" + TradeMethod.CAT_CashIC + "'  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.CAT_CashIC + "'  ORDER BY id DESC;";
                }
            } else {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                            "Trade like '" + _trade + "'  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + _trade + "'  ORDER BY id DESC;";
                }
            }

            Cursor c = mSqliteDB.rawQuery(queryStatementString, null);

            while(c.moveToNext()){
                @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
                @SuppressLint("Range") String tid = c.getString(c.getColumnIndex( "Tid"));
                @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
                @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
                @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
                @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
                @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
                @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
                @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
                @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
                @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
                @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
                @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
                @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
                @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
                @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
                @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
                @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
                @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
                @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
                @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
                @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
                @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
                @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
                @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
                @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
                @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
                @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
                @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
                @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
                @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
                @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
                @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
                @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
                @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
                @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
                @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
                @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
                @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
                @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
                @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
                @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
                @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
                @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
                @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

                DBTradeResult a = new DBTradeResult(Integer.parseInt(_id),tid,_storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                        _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                        _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                        _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                        _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);

                result.add(a);
            }
        }catch (SQLiteException ex){
            return null;
        }
        return result;
    }

    /** 지정한TID 거래내역 중 신용,현금,간편,현금IC 를 기간한정으로 분류별로 가져 온다. */
    public ArrayList<DBTradeResult> getTradeTIDTradeTypeDateList
    (String _tid, String _trade, String _fromdate, String _todate){

        String fromDate = "";
        if(!_fromdate.isEmpty()){
            fromDate = _fromdate + "000000";
        }
        String toDate = "";
        if(!_todate.isEmpty()) {
            toDate = _todate +  "235959";
        }

        ArrayList<DBTradeResult> result = new ArrayList<>();
        try {
            String queryStatementString = "";
            if (_trade.equals(TradeMethod.EasyPay)) {
                if(_todate.isEmpty()) {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  " +
                                "AND (Trade like '" + TradeMethod.Kakao + "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat + "'" +
                                "OR Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr +
                                "' OR Trade like '" + TradeMethod.CAT_Ali + "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                                "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR " + "Trade like '" + TradeMethod.CAT_App + "') ORDER BY id DESC";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Trade like '" + TradeMethod.Kakao +
                                "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat +
                                "' OR  Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard +
                                "' OR  Trade like '" + TradeMethod.EmvQr + "' OR Trade like '" + TradeMethod.CAT_Ali +
                                "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                                "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "') ORDER BY id DESC;";
                    }

                } else {
                    if(!_tid.equals("")){
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  " +
                                "AND  (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND " +
                                "(Trade like '" + TradeMethod.Kakao + "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat +
                                "' OR  Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr +
                                "' OR Trade like '" + TradeMethod.CAT_Ali + "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                                "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "')  ORDER BY id DESC";
                    } else {

                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName +
                                " where  (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND " +
                                "(Trade like '" + TradeMethod.Kakao + "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat +
                                "' OR  Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr +
                                "' OR Trade like '" + TradeMethod.CAT_Ali + "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                                "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "')  ORDER BY id DESC;";
                    }

                }

            } else if(_trade.equals(TradeMethod.NULL)) {
                if(_todate.isEmpty()) {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + "   ORDER BY id DESC;";
                    }

                } else {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + "  where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                                "(cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                                " (cast(AuDate as long) < " + toDate + ")  ORDER BY id DESC;";
                    }

                }
            } else if (_trade.equals(TradeMethod.Credit)) {
                if(_todate.isEmpty()) {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " Trade like '" + TradeMethod.Credit + "' OR  Trade like '" + TradeMethod.CAT_Credit + "'  ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.Credit +
                                "' OR  Trade like '" + TradeMethod.CAT_Credit + "'  ORDER BY id DESC;";
                    }

                } else {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                                " (Trade like '" + TradeMethod.Credit + "' OR  Trade like '" + TradeMethod.CAT_Credit + "')  ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                                " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.Credit +
                                "' OR  Trade like '" + TradeMethod.CAT_Credit + "')  ORDER BY id DESC;";
                    }

                }

            } else if (_trade.equals(TradeMethod.Cash)) {
                if(_todate.isEmpty()) {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " Trade like '" + TradeMethod.Cash + "' OR  Trade like '" + TradeMethod.CAT_Cash + "' ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.Cash +
                                "' OR  Trade like '" + TradeMethod.CAT_Cash + "' ORDER BY id DESC;";
                    }

                } else {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                                " (Trade like '" + TradeMethod.Cash + "' OR  Trade like '" + TradeMethod.CAT_Cash + "') ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                                " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.Cash +
                                "' OR  Trade like '" + TradeMethod.CAT_Cash + "') ORDER BY id DESC;";
                    }

                }

            } else if (_trade.equals(TradeMethod.CAT_CashIC)) {
                if(_todate.isEmpty()) {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " Trade like '" + TradeMethod.CAT_CashIC + "'  ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.CAT_CashIC + "'  ORDER BY id DESC;";
                    }

                } else {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                                " (Trade like '" + TradeMethod.CAT_CashIC + "'  ORDER BY id DESC;";

                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                                " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.CAT_CashIC + "')  ORDER BY id DESC;";
                    }

                }
            } else {
                if(_todate.isEmpty()) {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " Trade like '" + _trade + "'  ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + _trade + "'  ORDER BY id DESC;";
                    }

                } else {
                    if (!_tid.equals("")) {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                                " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                                " Trade like '" + _trade + "')  ORDER BY id DESC;";
                    } else {
                        queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                                " (cast(AuDate as long) < " + toDate + ") AND" +
                                "( Trade like '" + _trade + "')  ORDER BY id DESC;";
                    }

                }
            }

            Cursor c = mSqliteDB.rawQuery(queryStatementString, null);

            while(c.moveToNext()){
                @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
                @SuppressLint("Range") String tid = c.getString(c.getColumnIndex( "Tid"));
                @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
                @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
                @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
                @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
                @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
                @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
                @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
                @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
                @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
                @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
                @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
                @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
                @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
                @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
                @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
                @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
                @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
                @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
                @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
                @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
                @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
                @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
                @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
                @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
                @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
                @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
                @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
                @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
                @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
                @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
                @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
                @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
                @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
                @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
                @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
                @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
                @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
                @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
                @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
                @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
                @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
                @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
                @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

                DBTradeResult a = new DBTradeResult(Integer.parseInt(_id),tid,_storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                        _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                        _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                        _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                        _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);

                result.add(a);
            }
        }catch (SQLiteException ex){
            return null;
        }
        return result;
    }

    /** 지정한 결제 한개만 가져온다. */
    public DBTradeResult getTradeList(int index){

        DBTradeResult result = new DBTradeResult();
        try {
            String SelectQuery = "SELECT * FROM " + Command.DB_TradeTableName + " where id='" + String.valueOf(index) + "';";
            Cursor c = mSqliteDB.rawQuery(SelectQuery, null);
            while (c.moveToNext()) {
                @SuppressLint("Range") String _id = c.getString(c.getColumnIndex("id"));
                @SuppressLint("Range") String _tid = c.getString(c.getColumnIndex("Tid"));
                @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
                @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
                @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
                @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
                @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
                @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
                @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
                @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
                @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
                @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
                @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
                @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
                @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
                @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
                @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
                @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
                @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
                @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
                @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
                @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
                @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
                @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
                @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
                @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
                @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
                @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
                @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
                @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
                @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
                @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
                @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
                @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
                @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
                @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
                @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
                @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
                @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
                @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
                @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
                @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
                @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
                @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
                @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

                return result = new DBTradeResult(Integer.parseInt(_id), _tid, _storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                        _Trade, _Cancel, _Money, _GiftAmt, _Tax, _Svc, _Txf, _Inst,
                        _cashTarget, _cashMethod, _cashNum, _CardNum, _CardType, _CardInpNm, _CardIssuer, _MchNo, _AuDate, _OriAuDate,
                        _AuNum, _OriAuNum, _TradeNo, _Message, _KakaoMessage, _PayType, _KakaoAuMoney, _KakaoSaleMoney, _KakaoMemberCd,
                        _KakaoMemberNo, _Otc, _Pem, _Trid, _CardBin, _SearchNo, _PrintBarcd, _PrintUse, _PrintNm, _MchFee, _MchRefund);
            }
        }catch(SQLiteException ex){
            Log.d(TAG,"찾는 거래 내역이 존재 하지 않습니다.");
        }
        return result;
    }
    /// 거래 내역 중 기간별 특정내역만 가져 오는 함수 - 거래내역 조회에서 사용
    /// - Returns: DbTradeResult 배열
    public ArrayList<DBTradeResult> getTradeListParsingData(String _tid, String _trade, String _fromdate, String _todate){
        String fromDate = "";
        if(!_fromdate.isEmpty()){
            fromDate = _fromdate + "000000";
        }
        String toDate = "";
        if(!_todate.isEmpty()) {
            toDate = _todate +  "235959";
        }

        ArrayList<DBTradeResult> result = new ArrayList<>();
        String queryStatementString = "";
        
        if (_trade.equals(TradeMethod.EasyPay)) {
            if(_todate.isEmpty()) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  " + 
                            "AND (Trade like '" + TradeMethod.Kakao + "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat + "'" +
                        "OR Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr +
                            "' OR Trade like '" + TradeMethod.CAT_Ali + "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                            "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR " + "Trade like '" + TradeMethod.CAT_App + "') ORDER BY id DESC";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Trade like '" + TradeMethod.Kakao +
                            "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat + "' OR  Trade like '" + TradeMethod.Ali +
                            "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr + "' OR Trade like '" + TradeMethod.CAT_Ali +
                            "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                            "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "') ORDER BY id DESC;";
                }

            } else {
                if(!_tid.equals("")){
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                            " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND " +
                            "(Trade like '" + TradeMethod.Kakao + "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat +
                            "' OR  Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard + "' OR  Trade like '" + TradeMethod.EmvQr +
                            "' OR Trade like '" + TradeMethod.CAT_Ali + "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                            "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "')  ORDER BY id DESC";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                            " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.Kakao +
                            "' OR  Trade like '" + TradeMethod.Zero + "' OR  Trade like '" + TradeMethod.Wechat +
                            "' OR  Trade like '" + TradeMethod.Ali + "' OR  Trade like '" + TradeMethod.AppCard +
                            "' OR  Trade like '" + TradeMethod.EmvQr + "' OR Trade like '" + TradeMethod.CAT_Ali +
                            "' OR Trade like '" + TradeMethod.CAT_Kakao + "' OR Trade like '" + TradeMethod.CAT_We +
                            "' OR Trade like '" + TradeMethod.CAT_Zero + "' OR  Trade like '" + TradeMethod.CAT_App + "')  ORDER BY id DESC;";
                }

            }

        } else if(_trade.equals(TradeMethod.NULL)) {
            if(_todate.isEmpty()) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  ORDER BY id DESC;";
                } else {
                    queryStatementString =  "SELECT * FROM " + Command.DB_TradeTableName + " ORDER BY id DESC;";
                }

            } else {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + "  where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + "  where (cast(AuDate as long) > " + fromDate + ") AND" +
                            " (cast(AuDate as long) < " + toDate + ") ORDER BY id DESC;";
                }

            }
        } else if (_trade.equals(TradeMethod.Credit)) {
            if(_todate.isEmpty()) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                            "Trade like '" + TradeMethod.Credit + "' OR  Trade like '" + TradeMethod.CAT_Credit + "'  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.Credit +
                            "' OR  Trade like '" + TradeMethod.CAT_Credit + "'  ORDER BY id DESC;";
                }

            } else {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                            " (Trade like '" + TradeMethod.Credit + "' OR  Trade like '" + TradeMethod.CAT_Credit + "')  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                            " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.Credit +
                            "' OR  Trade like '" + TradeMethod.CAT_Credit + "')  ORDER BY id DESC;";
                }

            }

        } else if (_trade.equals(TradeMethod.Cash)) {
            if(_todate.isEmpty()) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " Trade like '" + TradeMethod.Cash + "' OR  Trade like '" + TradeMethod.CAT_Cash + "' ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + TradeMethod.Cash +
                            "' OR  Trade like '" + TradeMethod.CAT_Cash + "' ORDER BY id DESC;";
                }

            } else {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                            " (Trade like '" + TradeMethod.Cash + "' OR  Trade like '" + TradeMethod.CAT_Cash + "') ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                            " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.Cash + "' OR  Trade like '" + TradeMethod.CAT_Cash + "') ORDER BY id DESC;";
                }

            }

        } else if (_trade.equals(TradeMethod.CAT_CashIC)) {
            if(_todate.isEmpty()) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " Trade like '" + TradeMethod.CAT_CashIC + "')  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Trade like '" + TradeMethod.CAT_CashIC + "')  ORDER BY id DESC;";
                }

            } else {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                            " (Trade like '" + TradeMethod.CAT_CashIC + "')  ORDER BY id DESC;";

                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                            " (cast(AuDate as long) < " + toDate + ") AND (Trade like '" + TradeMethod.CAT_CashIC + "')  ORDER BY id DESC;";
                }

            }
        } else {
            if(_todate.isEmpty()) {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND " +
                            "Trade like '" + _trade + "'  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where Trade like '" + _trade + "'  ORDER BY id DESC;";
                }

            } else {
                if (!_tid.equals("")) {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                            " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") AND" +
                            " Trade like '" + _trade + "')  ORDER BY id DESC;";
                } else {
                    queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND" +
                            " (cast(AuDate as long) < " + toDate + ") AND Trade like '" + _trade + "')  ORDER BY id DESC;";
                }

            }
        }

        Cursor c = mSqliteDB.rawQuery(queryStatementString,null);

        while(c.moveToNext()){
            @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
            @SuppressLint("Range") String _Tid = c.getString(c.getColumnIndex( "Tid"));
            @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
            @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
            @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
            @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
            @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
            @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
            @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
            @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
            @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
            @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
            @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
            @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
            @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
            @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
            @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
            @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
            @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
            @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
            @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
            @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
            @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
            @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
            @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
            @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
            @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
            @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
            @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
            @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
            @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
            @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
            @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
            @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
            @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
            @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
            @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
            @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
            @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
            @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
            @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
            @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
            @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
            @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
            @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

            DBTradeResult a = new DBTradeResult(Integer.parseInt(_id),_Tid, _storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                    _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                    _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                    _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                    _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);

            result.add(a);
        }
        return result;
    }

    /// 마지막 거래 내역 데이터 가져 오는 함수
    /// - Returns: DbTradeResult
    public DBTradeResult getTradeLastData(String _tid) {
        DBTradeResult result = new DBTradeResult();

        String queryStatementString = "";
        if(!_tid.equals("")){
            queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')   ORDER BY id DESC LIMIT 1";
        } else if(getTID().equals("")) {
            String queryStatementString2 = "SELECT count(*) FROM " + Command.DB_TradeTableName + ";";
            int id = 0;
            Cursor c = mSqliteDB.rawQuery(queryStatementString2, null);
            c.moveToFirst();
            id = c.getInt(0);
            queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (id = '" + String.valueOf(id) + "')   ORDER BY id DESC LIMIT 1";
        } else {
            queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = " + getTID() + " OR Tid = '" + getTID() + "1" + "' OR Tid = '" + getTID() + "2" + "' OR Tid = '" + getTID() + "3" + "' OR Tid = '" + getTID() + "4" + "' OR Tid = '" + getTID() + "5" + "' )   ORDER BY id DESC LIMIT 1";
        }

        Cursor c = mSqliteDB.rawQuery(queryStatementString,null);

        while(c.moveToNext()){
            @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
            @SuppressLint("Range") String _Tid = c.getString(c.getColumnIndex( "Tid"));
            @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
            @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
            @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
            @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
            @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
            @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
            @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
            @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
            @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
            @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
            @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
            @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
            @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
            @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
            @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
            @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
            @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
            @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
            @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
            @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
            @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
            @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
            @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
            @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
            @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
            @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
            @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
            @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
            @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
            @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
            @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
            @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
            @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
            @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
            @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
            @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
            @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
            @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
            @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
            @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
            @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
            @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
            @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

            result = new DBTradeResult(Integer.parseInt(_id),_Tid, _storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                    _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                    _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                    _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                    _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);
        }
        return result;
    }

    ///거래 내역 기간별 조회
    public ArrayList<DBTradeResult> getTradeListPeriod(String _tid,String _fdate,String _tdate){
        ArrayList<DBTradeResult> result = new ArrayList<>();
        String fromDate = _fdate + "000000";
        String toDate = _tdate +  "235959";

        String queryStatementString = "";
        if(!_tid.equals("")){
            queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')  AND" +
                    " (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") ORDER BY id DESC;";
//            queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (Tid = '" + _tid + "' OR Tid = '')   ORDER BY id DESC LIMIT 1"
        } else {
            queryStatementString = "SELECT * FROM " + Command.DB_TradeTableName + " where (cast(AuDate as long) > " + fromDate + ") AND (cast(AuDate as long) < " + toDate + ") ORDER BY id DESC;";
        }

        Cursor c = mSqliteDB.rawQuery(queryStatementString,null);

        while(c.moveToNext()){
            @SuppressLint("Range") String _id = c.getString(c.getColumnIndex( "id"));
            @SuppressLint("Range") String _Tid = c.getString(c.getColumnIndex( "Tid"));
            @SuppressLint("Range") String _storeName = c.getString(c.getColumnIndex( "StoreName"));
            @SuppressLint("Range") String _storeAddr = c.getString(c.getColumnIndex( "StoreAddr"));
            @SuppressLint("Range") String _storeNumber = c.getString(c.getColumnIndex( "StoreNumber"));
            @SuppressLint("Range") String _storePhone = c.getString(c.getColumnIndex( "StorePhone"));
            @SuppressLint("Range") String _storeOwner = c.getString(c.getColumnIndex( "StoreOwner"));
            @SuppressLint("Range") String _Trade = c.getString(c.getColumnIndex("Trade"));
            @SuppressLint("Range") String _Cancel = c.getString(c.getColumnIndex("Cancel"));
            @SuppressLint("Range") String _Money = c.getString(c.getColumnIndex("Money"));
            @SuppressLint("Range") String _GiftAmt = c.getString(c.getColumnIndex("GiftAmt"));
            @SuppressLint("Range") String _Tax = c.getString(c.getColumnIndex("Tax"));
            @SuppressLint("Range") String _Svc = c.getString(c.getColumnIndex("Svc"));
            @SuppressLint("Range") String _Txf = c.getString(c.getColumnIndex("Txf"));
            @SuppressLint("Range") String _Inst = c.getString(c.getColumnIndex("Inst"));
            @SuppressLint("Range") String _cashTarget = c.getString(c.getColumnIndex("CashTarget"));
            @SuppressLint("Range") String _cashMethod = c.getString(c.getColumnIndex("CashInputType"));
            @SuppressLint("Range") String _cashNum = c.getString(c.getColumnIndex("CashNum"));
            @SuppressLint("Range") String _CardNum = c.getString(c.getColumnIndex("CardNum"));
            @SuppressLint("Range") String _CardType = c.getString(c.getColumnIndex("CardType"));
            @SuppressLint("Range") String _CardInpNm = c.getString(c.getColumnIndex("CardInpNm"));
            @SuppressLint("Range") String _CardIssuer = c.getString(c.getColumnIndex("CardIssuer"));
            @SuppressLint("Range") String _MchNo = c.getString(c.getColumnIndex("MchNo"));
            @SuppressLint("Range") String _AuDate = c.getString(c.getColumnIndex("AuDate"));
            @SuppressLint("Range") String _OriAuDate = c.getString(c.getColumnIndex("OriAuData"));
            @SuppressLint("Range") String _AuNum = c.getString(c.getColumnIndex("AuNum"));
            @SuppressLint("Range") String _OriAuNum = c.getString(c.getColumnIndex("OriAuNum"));
            @SuppressLint("Range") String _TradeNo = c.getString(c.getColumnIndex("TradeNo"));
            @SuppressLint("Range") String _Message = c.getString(c.getColumnIndex("Message"));
            @SuppressLint("Range") String _KakaoMessage = c.getString(c.getColumnIndex("KakaoMessage"));
            @SuppressLint("Range") String _PayType = c.getString(c.getColumnIndex("PayType"));
            @SuppressLint("Range") String _KakaoAuMoney = c.getString(c.getColumnIndex("KakaoAuMoney"));
            @SuppressLint("Range") String _KakaoSaleMoney = c.getString(c.getColumnIndex("KakaoSaleMoney"));
            @SuppressLint("Range") String _KakaoMemberCd = c.getString(c.getColumnIndex("KakaoMemberCd"));
            @SuppressLint("Range") String _KakaoMemberNo = c.getString(c.getColumnIndex("KakaoMemberNo"));
            @SuppressLint("Range") String _Otc = c.getString(c.getColumnIndex("Otc"));
            @SuppressLint("Range") String _Pem = c.getString(c.getColumnIndex("Pem"));
            @SuppressLint("Range") String _Trid = c.getString(c.getColumnIndex("Trid"));
            @SuppressLint("Range") String _CardBin = c.getString(c.getColumnIndex("CardBin"));
            @SuppressLint("Range") String _SearchNo = c.getString(c.getColumnIndex("SearchNo"));
            @SuppressLint("Range") String _PrintBarcd = c.getString(c.getColumnIndex("PrintBarcd"));
            @SuppressLint("Range") String _PrintUse = c.getString(c.getColumnIndex("PrintUse"));
            @SuppressLint("Range") String _PrintNm = c.getString(c.getColumnIndex("PrintNm"));
            @SuppressLint("Range") String _MchFee = c.getString(c.getColumnIndex("MchFee"));
            @SuppressLint("Range") String _MchRefund = c.getString(c.getColumnIndex("MchRefund"));

            DBTradeResult a = new DBTradeResult(Integer.parseInt(_id),_Tid, _storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner,
                    _Trade,_Cancel,_Money,_GiftAmt,_Tax,_Svc,_Txf,_Inst,
                    _cashTarget,_cashMethod,_cashNum,_CardNum,_CardType,_CardInpNm,_CardIssuer,_MchNo,_AuDate,_OriAuDate,
                    _AuNum,_OriAuNum,_TradeNo,_Message,_KakaoMessage,_PayType,_KakaoAuMoney,_KakaoSaleMoney,_KakaoMemberCd,
                    _KakaoMemberNo,_Otc,_Pem,_Trid,_CardBin,_SearchNo,_PrintBarcd,_PrintUse,_PrintNm,_MchFee,_MchRefund);

            result.add(a);
        }
        return result;
    }
    public void UpdateTradeList(String _Tid,String _Trade,String _cancel,int _money,String _giftamt,int _tax,int _Scv,int _Txf,int _InstallMent,
                         String _CashTarget,String _CashInputType,String _CashNum,String _CardNum,String _CardType,String _CardInpNm,
                         String _CardIssuer,String _MchNo,String _AuDate,String _OriAuDate,String _AuNum,String _OriAuNum,String _KocesTradeNo,
                         String _Message,String _KakaoMessage,String _PayType,String _KakaoAuMoney,String _KakaoSaleMoney,String _KakaoMemberCd,
                                String _KakaoMemberNo,String _Otc,String _Pem,String _Trid,String _CardBin,String _SearchNo,String _PrintBarcd,
                                String _PrintUse,String _PrintNm,String _MchFee,String _MchRefund){

        String temp1 = "update " + Command.DB_TradeTableName + " set Tid = '" + _Tid + "',Trade = '" + _Trade + "'  ,Cancel = '" + _cancel + "',Money = ,GiftAmt = '" + _giftamt +
                "',Tax = '" + _tax + "',Svc = '" + _Scv + "',Txf = '" + _Txf + "',Inst = '" + _InstallMent + "',CashTarget = '" + _CashTarget + "',CashInputType = '" + _CashInputType +
                "',CashNum = '" + _CashNum + "',CardNum = '" + _CardNum + "',CardType = '" + _CardType + "',CardInpNm = '" + _CardInpNm + "',CardIssuer = '" + _CardIssuer +
                "',MchNo = '" + _MchNo + "',AuDate = '" + _AuDate + "',OriAuData = '" + _OriAuDate + "',AuNum = '" + _AuNum + "',OriAuNum = '" + _OriAuNum +
                "',TradeNo = '" + _KocesTradeNo + "',Message = '" + _Message + "',KakaoMessage = '" + _KakaoMessage + "',PayType = '" + _PayType +
                "',KakaoAuMoney = '" + _KakaoAuMoney + "',KakaoSaleMoney = '" + _KakaoSaleMoney + "',KakaoMemberCd = '" + _KakaoMemberCd +
                "',KakaoMemberNo = '" + _KakaoMemberNo + "',Otc = '" + _Otc + "',Pem = '" + _Pem + "',Trid = '" + _Trid + "',CardBin = '" + _CardBin +
                "',SearchNo = '" + _SearchNo + "',PrintBarcd = '" + _PrintBarcd + "',PrintUse = '" + _PrintUse + "',PrintNm = '" + _PrintNm +
                "',MchFee = '" + _MchFee + "',MchRefund = '" + _MchRefund + "' ";
        String temp2 = "where Money = '" + _money + "' AND AuNum = '" + _OriAuNum + "' AuDate like '" + _OriAuDate + "%')';";

        String Update = temp1 + temp2;

        mSqliteDB.execSQL(Update);
        if(_Trade.equals(TradeMethod.Credit)) {
            Log.d(TAG,"신용거래 변경 DB : " + Update);
        } else if(_Trade.equals(TradeMethod.Cash)) {
            Log.d(TAG,"현금거래 변경 DB : " + Update);
        }

    }

    /**
     * 앱투앱으로 요청한 결제 결과 데이터를 저장한다.
     */
    public void InsertAppToAppData(HashMap<String,String> resultData)
    {
        try
        {

            CheckCountAppToApp(Command.DB_AppToAppTableName);

            String temp1 = "Insert INTO " + Command.DB_AppToAppTableName +
                    " (TrdType,TermID,TrdDate,AnsCode,Message,AuNo,TradeNo,CardNo,Keydate,MchData,CardKind,OrdCd,OrdNm,InpCd,InpNm," +
                    "DDCYn,EDCYn,GiftAmt,MchNo,BillNo,DisAmt,AuthType,AnswerTrdNo,ChargeAmt,RefundAmt,QrKind,OriAuDate,OriAuNo,TrdAmt,TaxAmt,SvcAmt,TaxFreeAmt,Month) Values" ;

            String TrdType = resultData.get("TrdType") == null ? "":resultData.get("TrdType").replace(" ","");
            String TermID = resultData.get("TermID") == null ? "":resultData.get("TermID").replace(" ","");
            String TrdDate = resultData.get("TrdDate") == null ? Utils.getDate("yyMMdd"):resultData.get("TrdDate").replace(" ","");
//            if (TrdDate.length() >= 8 && TrdDate.substring(0,2) == "20") {
//                TrdDate = TrdDate.substring(2,8);
//            }
//
//            else if (TrdDate.length() >= 6) {
//                TrdDate = TrdDate.substring(0,6);
//            }
            String AnsCode = resultData.get("AnsCode") == null ? "":resultData.get("AnsCode").replace(" ","");
            String Message = resultData.get("Message") == null ? "":resultData.get("Message").replace(" ","");
            String AuNo = resultData.get("AuNo") == null ? "":resultData.get("AuNo").replace(" ","");
            String TradeNo = resultData.get("TradeNo") == null ? "":resultData.get("TradeNo").replace(" ","");
            String CardNo = resultData.get("CardNo") == null ? "":resultData.get("CardNo").replace(" ","");
            String Keydate = resultData.get("Keydate") == null ? "":resultData.get("Keydate").replace(" ","");
            String MchData = resultData.get("MchData") == null ? "":resultData.get("MchData").replace(" ","");
            String CardKind = resultData.get("CardKind") == null ? "":resultData.get("CardKind").replace(" ","");
            String OrdCd = resultData.get("OrdCd") == null ? "":resultData.get("OrdCd").replace(" ","");
            String OrdNm = resultData.get("OrdNm") == null ? "":resultData.get("OrdNm").replace(" ","");
            String InpCd = resultData.get("InpCd") == null ? "":resultData.get("InpCd").replace(" ","");
            String InpNm = resultData.get("InpNm") == null ? "":resultData.get("InpNm").replace(" ","");
            String DDCYn = resultData.get("DDCYn") == null ? "":resultData.get("DDCYn").replace(" ","");
            String EDCYn = resultData.get("EDCYn") == null ? "":resultData.get("EDCYn").replace(" ","");

            String GiftAmt = resultData.get("GiftAmt") == null ? "":resultData.get("GiftAmt").replace(" ","");
            String MchNo = resultData.get("MchNo") == null ? "":resultData.get("MchNo").replace(" ","");
            String BillNo = resultData.get("BillNo") == null ? "":resultData.get("BillNo").replace(" ","");
            String DisAmt = resultData.get("DisAmt") == null ? "":resultData.get("DisAmt").replace(" ","");
            String AuthType = resultData.get("AuthType") == null ? "":resultData.get("AuthType").replace(" ","");
            String AnswerTrdNo = resultData.get("AnswerTrdNo") == null ? "":resultData.get("AnswerTrdNo").replace(" ","");
            String ChargeAmt = resultData.get("ChargeAmt") == null ? "":resultData.get("ChargeAmt").replace(" ","");
            String RefundAmt = resultData.get("RefundAmt") == null ? "":resultData.get("RefundAmt").replace(" ","");
            String QrKind = resultData.get("QrKind") == null ? "":resultData.get("QrKind").replace(" ","");

            String OriAuDate = resultData.get("OriAuDate") == null ? "":resultData.get("OriAuDate").replace(" ","");
            String OriAuNo = resultData.get("OriAuNo") == null ? "":resultData.get("OriAuNo").replace(" ","");

            String TrdAmt = resultData.get("TrdAmt") == null ? "":resultData.get("TrdAmt").replace(" ","");
            String TaxAmt = resultData.get("TaxAmt") == null ? "":resultData.get("TaxAmt").replace(" ","");
            String SvcAmt = resultData.get("SvcAmt") == null ? "":resultData.get("SvcAmt").replace(" ","");
            String TaxFreeAmt = resultData.get("TaxFreeAmt") == null ? "":resultData.get("TaxFreeAmt").replace(" ","");
            String Month = resultData.get("Month") == null ? "":resultData.get("Month").replace(" ","");

            String temp2 = "('" + TrdType + "','" +
                    TermID + "','" +
                    TrdDate + "','" +
                    AnsCode + "','" +
                    Message + "','" +
                    AuNo + "','" +
                    TradeNo + "','" +
                    CardNo + "','" +
                    Keydate + "','" +
                    MchData + "','" +
                    CardKind + "','" +
                    OrdCd + "','" +
                    OrdNm + "','" +
                    InpCd + "','" +
                    InpNm + "','" +
                    DDCYn + "','" +
                    EDCYn + "','" +
                    GiftAmt + "','" +
                    MchNo + "','" +
                    BillNo + "','" +
                    DisAmt + "','" +
                    AuthType + "','" +
                    AnswerTrdNo + "','" +
                    ChargeAmt + "','" +
                    RefundAmt + "','" +
                    QrKind + "','" +
                    OriAuDate + "','" +
                    OriAuNo + "','" +
                    TrdAmt + "','" +
                    TaxAmt + "','" +
                    SvcAmt + "','" +
                    TaxFreeAmt + "','" +
                    Month +  "');";

            DeleteAppToAppList(TermID,BillNo,TrdDate);

            String Insert = temp1 + temp2;
            mSqliteDB.execSQL(Insert);
        }
        catch (SQLiteException ex)
        {
            Log.d(TAG,ex.toString());
        }
    }

    /**
     * 무결성 검사가 100개를 넘으면 최초 저장 필드를 삭제 하는 함수
     * @param _tableName 테이블 이름
     */
    private void CheckCountAppToApp(String _tableName)
    {
        int ColumnsCount = 0;
        String selectQuery = "Select * from " + _tableName ;
        Cursor c = mSqliteDB.rawQuery(selectQuery,null);

        while (c.moveToNext())
        {
            ColumnsCount++;
        }

        if(ColumnsCount> (MaxAppToAppCount-1))
        {
            String delete = "DELETE FROM " + Command.DB_AppToAppTableName + " where ID = (SELECT MIN(ID) FROM " + Command.DB_AppToAppTableName + ");";
            mSqliteDB.execSQL(delete);
        }
    }

    public boolean CheckAppToAppList(String _tid,String _billNo,String _audate)
    {
        boolean _result = false;
        String queryStatementString = "";

        queryStatementString = "SELECT * FROM " + Command.DB_AppToAppTableName + " Where TermID = '" + _tid + "' AND BillNo = '" + _billNo + "' AND TrdDate like '%" + _audate + "%';";

        Cursor c = mSqliteDB.rawQuery(queryStatementString,null);

        while(c.moveToNext()){
            _result = true;
            break;
        }

        return _result;
    }

    private void DeleteAppToAppList(String _tid,String _billNo,String _audate)
    {
        String audate = _audate.substring(0,6);
        try {
            String temp1 = "DELETE from " + Command.DB_AppToAppTableName + " Where TermID = '" + _tid + "' AND BillNo = '" + _billNo + "' AND TrdDate like '%" + audate + "%';";
            mSqliteDB.execSQL(temp1);
        }catch (SQLiteException ex){
            Log.d(TAG,ex.toString());
        }
    }

    /**
     * 앱투앱 거래내역을 재전송한다
     */
    public HashMap<String,String>getAppToAppTrade(String _tid, String _audate, String _billno)
    {
        String queryStatementString = "";
//        queryStatementString = "SELECT * FROM " + Command.DB_AppToAppTableName + " where (TermID = '" + _tid.replace(" ","") + "')  AND (TrdDate like '" + _audate.replace(" ","") + "') AND (BillNo = '" + _billno.replace(" ","") +  "') ORDER BY id DESC;";
        queryStatementString = "SELECT * FROM " + Command.DB_AppToAppTableName + " where ((BillNo = '" + _billno.replace(" ","") + "') AND (TermID = '" + _tid.replace(" ","") + "') AND (TrdDate like '%" + _audate.replace(" ","") + "%'))  ORDER BY id DESC;";
//        queryStatementString = "SELECT * FROM " + Command.DB_AppToAppTableName + " where (TermID = " + _tid.replace(" ","") + " AND TrdDate like '" + _audate.replace(" ","") + "' AND BillNo = '" + _billno.replace(" ","") + "' )  ORDER BY id DESC;";
        Cursor c = mSqliteDB.rawQuery(queryStatementString,null);
        HashMap<String,String> hashMap = new HashMap<String, String>();

        while(c.moveToNext()){
            @SuppressLint("Range") String id = c.getString(c.getColumnIndex( "id"));
            @SuppressLint("Range") String TrdType = c.getString(c.getColumnIndex( "TrdType"));
            @SuppressLint("Range") String TermID = c.getString(c.getColumnIndex( "TermID"));
            @SuppressLint("Range") String TrdDate =  c.getString(c.getColumnIndex( "TrdDate"));
            @SuppressLint("Range") String AnsCode =  c.getString(c.getColumnIndex( "AnsCode"));
            @SuppressLint("Range") String Message =  c.getString(c.getColumnIndex( "Message"));
            @SuppressLint("Range") String AuNo =  c.getString(c.getColumnIndex( "AuNo"));
            @SuppressLint("Range") String TradeNo =  c.getString(c.getColumnIndex( "TradeNo"));
            @SuppressLint("Range") String CardNo =  c.getString(c.getColumnIndex("CardNo"));
            @SuppressLint("Range") String Keydate =  c.getString(c.getColumnIndex("Keydate"));
            @SuppressLint("Range") String MchData =  c.getString(c.getColumnIndex("MchData"));
            @SuppressLint("Range") String CardKind =  c.getString(c.getColumnIndex("CardKind"));
            @SuppressLint("Range") String OrdCd =  c.getString(c.getColumnIndex("OrdCd"));
            @SuppressLint("Range") String OrdNm =  c.getString(c.getColumnIndex("OrdNm"));
            @SuppressLint("Range") String InpCd =  c.getString(c.getColumnIndex("InpCd"));
            @SuppressLint("Range") String InpNm =  c.getString(c.getColumnIndex("InpNm"));
            @SuppressLint("Range") String DDCYn =  c.getString(c.getColumnIndex("DDCYn"));
            @SuppressLint("Range") String EDCYn =  c.getString(c.getColumnIndex("EDCYn"));
            @SuppressLint("Range") String GiftAmt =  c.getString(c.getColumnIndex("GiftAmt"));
            @SuppressLint("Range") String MchNo =  c.getString(c.getColumnIndex("MchNo"));
            @SuppressLint("Range") String BillNo =  c.getString(c.getColumnIndex("BillNo"));
            @SuppressLint("Range") String DisAmt =  c.getString(c.getColumnIndex("DisAmt"));
            @SuppressLint("Range") String AuthType = c.getString(c.getColumnIndex("AuthType"));
            @SuppressLint("Range") String AnswerTrdNo = c.getString(c.getColumnIndex("AnswerTrdNo"));
            @SuppressLint("Range") String ChargeAmt = c.getString(c.getColumnIndex("ChargeAmt"));
            @SuppressLint("Range") String RefundAmt = c.getString(c.getColumnIndex("RefundAmt"));
            @SuppressLint("Range") String QrKind =  c.getString(c.getColumnIndex("QrKind"));
            @SuppressLint("Range") String OriAuDate =  c.getString(c.getColumnIndex("OriAuDate"));
            @SuppressLint("Range") String OriAuNo =  c.getString(c.getColumnIndex("OriAuNo"));
            @SuppressLint("Range") String TrdAmt =  c.getString(c.getColumnIndex("TrdAmt"));
            @SuppressLint("Range") String TaxAmt =  c.getString(c.getColumnIndex("TaxAmt"));
            @SuppressLint("Range") String SvcAmt =  c.getString(c.getColumnIndex("SvcAmt"));
            @SuppressLint("Range") String TaxFreeAmt =  c.getString(c.getColumnIndex("TaxFreeAmt"));
            @SuppressLint("Range") String Month =  c.getString(c.getColumnIndex("Month"));
            hashMap.put("TrdType",TrdType);hashMap.put("TermID",TermID);hashMap.put("TrdDate",TrdDate);
            hashMap.put("AnsCode",AnsCode);hashMap.put("Message",Message);hashMap.put("AuNo",AuNo);
            hashMap.put("TradeNo",TradeNo);hashMap.put("CardNo",CardNo);hashMap.put("Keydate",Keydate);
            hashMap.put("MchData",MchData);hashMap.put("CardKind",CardKind);hashMap.put("OrdCd",OrdCd);
            hashMap.put("OrdNm",OrdNm);hashMap.put("InpCd",InpCd);hashMap.put("InpNm",InpNm);
            hashMap.put("DDCYn",DDCYn);hashMap.put("EDCYn",EDCYn);hashMap.put("GiftAmt",GiftAmt);
            hashMap.put("MchNo",MchNo);hashMap.put("BillNo",BillNo);hashMap.put("DisAmt",DisAmt);
            hashMap.put("AuthType",AuthType);hashMap.put("AnswerTrdNo",AnswerTrdNo);hashMap.put("ChargeAmt",ChargeAmt);
            hashMap.put("RefundAmt",RefundAmt);hashMap.put("QrKind",QrKind);
            hashMap.put("OriAuDate",OriAuDate);hashMap.put("OriAuNo",OriAuNo);
            hashMap.put("TrdAmt",TrdAmt);hashMap.put("TaxAmt",TaxAmt);
            hashMap.put("SvcAmt",SvcAmt);hashMap.put("TaxFreeAmt",TaxFreeAmt);
            hashMap.put("Month",Month);
        }
        return hashMap;
    }

    public class DBAppToAppResult {
        int id = 0;
        //본앱에서도 가맹점등록다운로드를 다른 tid 로 하면서 결제를 요청하고 취소한다. 만일 취소한다고 하면 저장된 tid 로 취소를 요청한다
        String TrdType = "";
        String TermID = "";
        String TrdDate = "";
        String AnsCode = "";
        String Message = "";
        String AuNo = "";
        String TradeNo = "";
        String CardNo = "";
        String Keydate = "";
        String MchData = "";
        String CardKind = "";
        String OrdCd = "";
        String OrdNm = "";
        String InpCd = "";
        String InpNm = "";
        String DDCYn = "";
        String EDCYn = "";
        String GiftAmt = "";
        String MchNo = "";
        String BillNo = "";
        String DisAmt = "";
        String AuthType ="";
        String AnswerTrdNo = "";
        String ChargeAmt = "";
        String RefundAmt = "";
        String QrKind = "";
        String OriAuNo = "";
        String OriAuDate = "";
        String TrdAmt = "";
        String TaxAmt = "";
        String SvcAmt = "";
        String TaxFreeAmt = "";
        String Month = "";

        public DBAppToAppResult(){}
        public DBAppToAppResult(int _id,String mTrdType,String mTermID,String mTrdDate,String mAnsCode,String mMessage,String mAuNo,String mTradeNo,
                                String mCardNo,String mKeydate,String mMchData,String mCardKind,String mOrdCd,String mOrdNm,String mInpCd,String mInpNm,
                                String mDDCYn,String mEDCYn,String mGiftAmt,String mMchNo,String mBillNo,String mDisAmt,String mAuthType,
                                String mAnswerTrdNo,String mChargeAmt,String mRefundAmt,String mQrKind,String mOriAuDate,String mOriAuNo,
                                String mTrdAmt,String mTaxAmt,String mSvcAmt,String mTaxFreeAmt,String mMonth)
        {
            id = _id;
            TrdType = mTrdType;
            TermID = mTermID;
            TrdDate = mTrdDate;
            AnsCode = mAnsCode;
            Message = mMessage;
            AuNo = mAuNo;
            TradeNo = mTradeNo;
            CardNo = mCardNo;
            Keydate = mKeydate;
            MchData = mMchData;
            CardKind = mCardKind;
            OrdCd = mOrdCd;
            OrdNm = mOrdNm;
            InpCd = mInpCd;
            InpNm = mInpNm;
            DDCYn = mDDCYn;
            EDCYn = mEDCYn;
            GiftAmt = mGiftAmt;
            MchNo = mMchNo;
            BillNo = mBillNo;
            DisAmt = mDisAmt;
            AuthType =mAuthType;
            AnswerTrdNo = mAnswerTrdNo;
            ChargeAmt = mChargeAmt;
            RefundAmt = mRefundAmt;
            QrKind = mQrKind;
            OriAuDate = mOriAuDate;
            OriAuNo = mOriAuNo;
            TrdAmt = mTrdAmt;
            TaxAmt = mTaxAmt;
            SvcAmt = mSvcAmt;
            TaxFreeAmt = mTaxFreeAmt;
            Month = mMonth;
        }
    }

    public class DBTradeResult {
        public DBTradeResult(){}
        public DBTradeResult(int _id,String _Tid,String _StoreName, String _StoreAddr, String _StoreNumber, String _StorePhone, String _StoreOwner,
                             String _Trade, String _Cancel, String _Money, String _GiftAmt,
                             String _Tax, String _Svc, String _Txf, String _Inst, String _cashTarget, String _cashMethod,
                             String _cashNum,String _CardNum,String _CardType,String _CardInpNm,String _CardIssuer,String _MchNo,
                             String _AuDate,String _OriAuDate,String _AuNum,String _OriAuNum,String _TradeNo,String _Message,String _KakaoMessage,String _PayType,
                             String _KakaoAuMoney,String _KakaoSaleMoney,String _KakaoMemberCd,String _KakaoMemberNo,String _Otc,
                             String _Pem,String _Trid,String _CardBin,String _SearchNo,String _PrintBarcd,String _PrintUse,String _PrintNm,
                             String _MchFee,String _MchRefund) {
            id = _id;
            Tid = _Tid; //거래 할 때의 TID
            StoreName = _StoreName;
            StoreAddr = _StoreAddr;
            StoreNumber = _StoreNumber;
            StorePhone = _StorePhone;
            StoreOwner = _StoreOwner;
            Trade = _Trade;
            Cancel = _Cancel;
            Money = _Money;   //거래금액
            GiftAmt = _GiftAmt;    //기프트카드 잔액
            Tax = _Tax; //세금
            Svc = _Svc; //봉사료
            Txf = _Txf;  //비과세
            Inst = _Inst;    //할부
            CashTarget = _cashTarget;
            CashInputMethod = _cashMethod;
            CashNum = _cashNum;     //현금영수증 발행 번호
            CardNum = _CardNum;
            CardType = _CardType;    //카드종류
            CardInpNm = _CardInpNm;    //매입사명
            CardIssuer = _CardIssuer;  //발급사명
            MchNo =_MchNo;   //가맹점번호
            AuDate = _AuDate;  //승인날짜
            OriAuDate = _OriAuDate;   //원승인날짜(취소전표에 출력내용에 표시)
            AuNum = _AuNum;   //승인번호
            OriAuNum = _OriAuNum;   //원승인번호(취소 시 거래내역 업데이트 에서 필요)
            TradeNo = _TradeNo;
            Message = _Message; //거래 응답메시지

            /** 여기서부터 간편결제용 추가 내용 */
            //카카오페이추가내용
            KakaoMessage = _KakaoMessage;    //알림메세지
            PayType = _PayType; //결제수단
            KakaoAuMoney = _KakaoAuMoney;   //승인금액(카카오머니 승인 응답 시(승인금액 = 거래금액 - 카카오할인금액))
            KakaoSaleMoney = _KakaoSaleMoney;  //카카오페이할인금액
            KakaoMemberCd = _KakaoMemberCd;   //카카오 멤버십바코드
            KakaoMemberNo = _KakaoMemberNo;   //카카오 멤버십 번호
            Otc = _Otc; //카드번호정보(OTC) - 결제수단 카드 시
            Pem = _Pem;//PEM - 결제수단 카드 시
            Trid = _Trid;    //trid - 결제수단 카드 시
            CardBin = _CardBin;    //카드Bin - 결제수단 카드 시
            SearchNo = _SearchNo;    //조회고유번호
            PrintBarcd = _PrintBarcd;    //출력용 바코드 번호(전표 출력시 사용될 바코드 번호)
            PrintUse = _PrintUse;    //전표출력여부
            PrintNm = _PrintNm;    //전표구분명
            //제로페이추가내용
            MchFee = _MchFee;  //제로페이 가맹점수수료
            MchRefund = _MchRefund;   //제로페이 가맹점 환불금액

        }

        int id = 0;
        //본앱에서도 가맹점등록다운로드를 다른 tid 로 하면서 결제를 요청하고 취소한다. 만일 취소한다고 하면 저장된 tid 로 취소를 요청한다
        String Tid = ""; //거래 할 때의 TID
        String StoreName = ""; //가맹점이름
        String StoreAddr = ""; //가맹점주소
        String StoreNumber = ""; //사업자번호
        String StorePhone = ""; //가맹점전화번호
        String StoreOwner = ""; //가맹점주
        String Trade = "";
        String Cancel = "";
        String Money = "";   //거래금액
        String GiftAmt = "";    //기프트카드 잔액
        String Tax = ""; //세금
        String Svc = ""; //봉사료
        String Txf = "";  //비과세
        String Inst = "";    //할부
        String CashTarget = "";
        String CashInputMethod = "";
        String CashNum = "";     //현금영수증 발행 번호
        String CardNum = "";
        String CardType = "";    //카드종류
        String CardInpNm = "";    //매입사명
        String CardIssuer = "";  //발급사명
        String MchNo ="";   //가맹점번호
        String AuDate = "";  //승인날짜
        String OriAuDate = "";   //원승인날짜(취소전표에 출력내용에 표시)
        String AuNum = "";   //승인번호
        String OriAuNum = "";   //원승인번호(취소 시 거래내역 업데이트 에서 필요)
        String TradeNo = "";
        String Message = ""; //거래 응답메시지

        /** 여기서부터 간편결제용 추가 내용 */
        //카카오페이추가내용
        String KakaoMessage = "";    //알림메세지
        String PayType = ""; //결제수단
        String KakaoAuMoney = "";   //승인금액(카카오머니 승인 응답 시(승인금액 = 거래금액 - 카카오할인금액))
        String KakaoSaleMoney = "";  //카카오페이할인금액
        String KakaoMemberCd = "";   //카카오 멤버십바코드
        String KakaoMemberNo = "";   //카카오 멤버십 번호
        String Otc = ""; //카드번호정보(OTC) - 결제수단 카드 시
        String Pem = "";//PEM - 결제수단 카드 시
        String Trid = "";    //trid - 결제수단 카드 시
        String CardBin = "";    //카드Bin - 결제수단 카드 시
        String SearchNo = "";    //조회고유번호
        String PrintBarcd = "";    //출력용 바코드 번호(전표 출력시 사용될 바코드 번호)
        String PrintUse = "";    //전표출력여부
        String PrintNm = "";    //전표구분명
        //제로페이추가내용
        String MchFee = "";  //제로페이 가맹점수수료
        String MchRefund = "";   //제로페이 가맹점 환불금액


        public int getid()  {return id; }
        //본앱에서도 가맹점등록다운로드를 다른 tid 로 하면서 결제를 요청하고 취소한다. 만일 취소한다고 하면 저장된 tid 로 취소를 요청한다
        public String getTid()  { return Tid; } //거래 할 때의 TID
        public String getStoreName()  { return StoreName; } //거래 할 때의 TID
        public String getStoreAddr()  { return StoreAddr; } //거래 할 때의 TID
        public String getStoreNumber()  { return StoreNumber; } //거래 할 때의 TID
        public String getStorePhone()  { return StorePhone; } //거래 할 때의 TID
        public String getStoreOwner()  { return StoreOwner; } //거래 할 때의 TID
        public String getTrade()  { return Trade; }
        public String getCancel()  { return Cancel; }
        public String getMoney()  { return Money; }
        public String getGiftAmt()  { return GiftAmt; }   //기프트카드 잔액
        public String getTax()  { return Tax; }
        public String getSvc()  { return Svc; }
        public String getTxf()  { return Txf; }
        public String getInst()  { return Inst; }
        public String getCashTarget()  { return CashTarget; }
        public String getCashMethod()  { return CashInputMethod; }
        public String getCashNum()  { return CashNum; }
        public String getCardNum()  { return CardNum; }
        public String getCardType()  { return CardType; }
        public String getCardInpNm()  { return CardInpNm; }
        public String getCardIssuer()  { return CardIssuer; }
        public String getMchNo()  {return MchNo; }
        public String getAuDate()  { return AuDate; }
        public String getOriAuDate()  {return OriAuDate; }
        public String getAuNum()  { return AuNum; }
        public String getOriAuNum()  {return OriAuNum; }
        public String getTradeNo()  { return TradeNo; }
        public String getMessage()  {return Message;}

        /** 여기서부터 간편결제용 추가 내용 */
        //카카오페이추가내용
        public String getKakaoMessage()  { return KakaoMessage; }
        public String getPayType()  { return PayType; }
        public String getKakaoAuMoney()  { return KakaoAuMoney; }
        public String getKakaoSaleMoney()  { return KakaoSaleMoney; }
        public String getKakaoMemberCd()  { return KakaoMemberCd; }
        public String getKakaoMemberNo()  { return KakaoMemberNo; }
        public String getOtc()  { return Otc; }
        public String getPem()  { return Pem; }
        public String getTrid()  { return Trid; }
        public String getCardBin()  {return CardBin; }
        public String getSearchNo()  { return SearchNo; }
        public String getPrintBarcd()  {return PrintBarcd; }
        public String getPrintUse()  { return PrintUse; }
        public String getPrintNm()  { return PrintNm;}
        //제로페이추가내용
        public String getMchFee()  { return MchFee; }
        public String getMchRefund()  { return MchRefund; }
    }

    public static class TradeMethod{
        public final static String Credit = "신용";
        public final static String EasyPay = "간편결제";       //모든간편결제 데이터들을 DB에서 조회하기위해 사용
        public final static String Cash = "현금";
        public final static String Kakao = "카카오";
        public final static String Zero = "제로";
        public final static String Wechat = "위쳇";
        public final static String Ali = "알리";
        public final static String AppCard = "앱카드";
        public final static String EmvQr = "EmvQr";
        public final static String NoCancel = "0";
        public final static String Cancel = "1";
        public final static String CashPrivate = "개인";
        public final static String CashBusiness = "사업";
        public final static String CashSelf = "자체";
        public final static String CashDirect = "직접입력";
        public final static String CashMs = "Msr";
        public final static String NULL = "";
        public final static String CAT_Credit = "신용(CAT)";
        public final static String CAT_App = "간편앱카드(CAT)";
        public final static String CAT_Zero = "간편제로(CAT)";
        public final static String CAT_Kakao = "간편카카오(CAT)";
        public final static String CAT_Ali = "간편알리(CAT)";
        public final static String CAT_We = "간편위쳇(CAT)";
        public final static String CAT_Cash = "현금(CAT)";
        public final static String CAT_CashIC = "현금IC(CAT)";
    }

    private String getTID()
    {
        return Setting.getPreference(KocesPosSdk.getInstance().getActivity(),Constants.TID);
    }
}
