package com.koces.androidpos.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportService;

import com.koces.androidpos.AppToAppActivity;
import com.koces.androidpos.BaseActivity;
import com.koces.androidpos.CashLoadingActivity;
import com.koces.androidpos.CreditLoadingActivity;
import com.koces.androidpos.EasyLoadingActivity;
import com.koces.androidpos.R;
import com.koces.androidpos.SignPadActivity;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class EasyPaySdk {
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 결제관련 기능을 수행하기 위해 사용한다 */
    private KocesPosSdk mPosSdk;
    /* 결제관련SDK를 수행하는 위치 Context */
    private Context mCtx;

    private String 전문번호 = "";
    private String _Tid = "";
    private String mStoreName = "";
    private String mStoreAddr = "";
    private String mStoreNumber = "";
    private String mStorePhone = "";
    private String mStoreOwner = "";
    private String 거래일시 = "";
    private String 단말버전 = "";
    private String 단말추가정보 = "";
    private String 취소구분 = "";
    private String 원거래일자 = "";
    private String 원승인번호 = "";
    private String 입력방법 = "";
    private String 바코드번호 = "";
    private byte[] OTC카드번호 = new byte[]{};
    private String 거래금액 = "";
    private String 세금 = "";
    private String 봉사료 = "";
    private String 비과세 = "";
    private String 통화코드 = "";
    private String 할부개월 = "";
    private String 결제수단 = "";
    private String 취소종류 = "";
    private String 취소타입 = "";
    private String 점포코드 = "";
    private String _PEM = "";
    private String _trid = "";
    private String 카드BIN = "";
    private String 조회고유번호 = "";
    private String _WorkingKeyIndex = "";
    private String 전자서명사용여부 = "";
    private String 사인패드시리얼번호 = "";
    private byte[] 전자서명데이터 = new byte[]{};
    private String 가맹점데이터 = "";

    //제로페이 추가데이터
    private String 가맹점추가정보 = "";
    private String KOCES거래고유번호 = "";
    private String mICCancelInfo = "";  //망취소 시 사용(앱카드 emv 일 때 취소시 사용)

    //앱투앱 추가데이터
    private String QrKind = "";

    /** 현재 화면이 앱투앱인지 아닌지를 체크 DB 저장을 위해서 */
    private boolean mDBAppToApp = false;

    /** 망취소발생하여 거래를 취소했다는 것을 알려주는 내용 */
    private boolean m2TradeCancel = false;

    /** 망취소 발생시 카운트함 -> 현금쪽은 망취소 발생하여 해당 내역을 다시 수신하면 거래성공으로 간주해버리기때문에 */
    int mEotCancel = 0;

    /** UI화면에 표시 또는 APP TO APP 데이터 전달용 HashMap 결과 데이터 */
    HashMap<String,String> sendData;

    /** ApptoAppActivity 또는 PaymentActivity 로 데이터를 보내기 위한 리스너 */
    private SerialInterface.PaymentListener mPaymentListener;

    /** (앱투앱으로 실행시 앱투앱에서 받는다) 서명데이터 결과값을 받을 시 결과 키값 */
    private int REQUEST_SIGNPAD = 10001;

    /**
     * PaymentSdk 생성
     * @param _appToapp 앱투앱인지 아닌지 true = 앱투앱
     * @param _PaymentListener 리스너
     */
    public EasyPaySdk(Context _ctx,boolean _appToapp,SerialInterface.PaymentListener _PaymentListener)
    {
        mCtx = _ctx;
        mDBAppToApp = _appToapp;
        mPosSdk = KocesPosSdk.getInstance();
        mPaymentListener = _PaymentListener;
        Clear();
    }

    void Clear() {
        전문번호 = "";
        _Tid = "";
        거래일시 = "";
        단말버전 = "";
        단말추가정보 = "";
        취소구분 = "";
        원거래일자 = "";
        원승인번호 = "";
        입력방법 = "";
        바코드번호 = "";
        OTC카드번호 = new byte[]{};
        거래금액 = "";
        세금 = "";
        봉사료 = "";
        비과세 = "";
        통화코드 = "";
        할부개월 = "";
        결제수단 = "";
        취소종류 = "";
        취소타입 = "";
        점포코드 = "";
        _PEM = "";
        _trid = "";
        카드BIN = "";
        조회고유번호 = "";
        _WorkingKeyIndex = "";
        전자서명사용여부 = "";
        사인패드시리얼번호 = "";
        전자서명데이터 = new byte[]{};
        가맹점데이터 = "";
        m2TradeCancel = false;
        if (sendData != null) {
            sendData.clear();
            sendData = null;
        }
        mStoreName = "";
        mStoreAddr = "";
        mStoreNumber = "";
        mStorePhone = "";
        mStoreOwner = "";
        QrKind = "";
    }

    /**
     일단 카카오페이만 테스트한다. 다른 부분이 들어온다면 공통 처리부분만 만들어두고 나머지를 이후 처리한다
     */
    public void EasyPay(String 전문번호,String _Tid,String 거래일시,String 단말버전,String 단말추가정보,String 취소구분, String 원거래일자,
                        String 원승인번호, String 입력방법, String 바코드번호, byte[] OTC카드번호, String 거래금액,String 세금,String 봉사료,
                        String 비과세,String 통화코드, String 할부개월, String 결제수단, String 취소종류, String 취소타입, String 점포코드,
                        String _PEM, String _trid, String 카드BIN, String 조회고유번호, String _WorkingKeyIndex, String 전자서명사용여부,
                        String 사인패드시리얼번호, byte[] 전자서명데이터, String 가맹점데이터, String 가맹점추가정보, String KOCES거래고유번호,
                        String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner, String _qrKind) {
        //시작전에 항상 클리어 한다.
        Clear();
        this.전문번호 = 전문번호;
        this._Tid = _Tid;
        this.mStoreName = _storeName;
        this.mStoreAddr = _storeAddr;
        this.mStoreNumber = _storeNumber;
        this.mStorePhone = _storePhone;
        this.mStoreOwner = _storeOwner;
        this.거래일시 = 거래일시;
        this.단말버전 = 단말버전;
        this.단말추가정보 = 단말추가정보;
        this.취소구분 = 취소구분;
        this.원거래일자 = 원거래일자;
        this.원승인번호 = 원승인번호;
        this.입력방법 = 입력방법;
        this.바코드번호 = 바코드번호;
        this.OTC카드번호 = OTC카드번호;
        this.거래금액 = 거래금액;
        this.세금 = 세금;
        this.봉사료 = 봉사료;
        this.비과세 = 비과세;
        this.통화코드 = 통화코드;
        this.할부개월 = 할부개월;
        this.결제수단 = 결제수단;
        this.취소종류 = 취소종류;
        this.취소타입 = 취소타입;
        this.점포코드 = 점포코드;
        this._PEM = _PEM;
        this._trid = _trid;
        this.카드BIN = 카드BIN;
        this.조회고유번호 = 조회고유번호;
        this._WorkingKeyIndex = _WorkingKeyIndex;
        this.전자서명사용여부 = 전자서명사용여부;
        this.사인패드시리얼번호 = 사인패드시리얼번호;
        this.전자서명데이터 = 전자서명데이터;
        this.가맹점데이터 = 가맹점데이터;

        //제로페이 추가데이터
        this.KOCES거래고유번호 = KOCES거래고유번호;
        this.가맹점추가정보 = 가맹점추가정보;

        //앱투앱 추가데이터
        this.QrKind = _qrKind;

        if  (거래금액 != null && !거래금액.equals(""))  //금액이 이상한 경우
        {
            String trimmedString = 거래금액.replace(" ","");
            this.거래금액 = trimmedString;

            if (Integer.parseInt(this.거래금액) < 0 || Integer.parseInt(this.거래금액) > 900000000 ) //금액은 최대 9억을 넘을 수 없다.
            {
                Clear();
                mPaymentListener.result(mCtx.getResources().getString(R.string.error_input_wrong_Payment_total_money),"ERROR",new HashMap<>());
                return;
            }
        }
        else
        {
            Clear();
            mPaymentListener.result(mCtx.getResources().getString(R.string.error_input_wrong_Payment_total_money),"ERROR",new HashMap<>());
            return;
        }
        if (할부개월 != null && !할부개월.equals(""))  //할부금액이 이상한 경우
        {
            this.할부개월 = 할부개월;

            if (Integer.parseInt(this.할부개월) == 0) {}
            else
            {
                if (Integer.parseInt(this.할부개월) < 2 || Integer.parseInt(this.할부개월) > 99) //할부의 경우에는 최대 99개월을 넘길 수 없다.
                {
                    Clear();
                    mPaymentListener.result(mCtx.getResources().getString(R.string.error_input_wrong_Payment_installment), "ERROR", new HashMap<>());
                    return;
                }
            }
        }

        this.할부개월 = 할부개월;

        if (취소구분 != null && !취소구분.equals("")) {
            this.원거래일자 = 원거래일자;
            int _convert = Integer.valueOf(거래금액) + Integer.valueOf(세금) + Integer.valueOf(봉사료);
            this.거래금액 = String.valueOf(_convert);
            this.세금 = "0";
            this.봉사료 = "0";
            this.비과세 = 비과세;
            if(QrKind.equals("ZP"))
            {
                전문번호 = "Z20";
            }
        }

        if (전문번호.equals("Z20")) {
            //제로페이 취소요청의 경우 바코드를 읽지 않고 바로 취소요청을 한다
            Res_Scanner( true,  "",  "");
            return;
        }

        if (전문번호.equals("A20") || 전문번호.equals("K21")) {
            if(QrKind.equals("WC"))
            {
                //위쳇페이 취소요청의 경우 바코드를 읽지 않고 바로 취소요청을 한다
                Res_Scanner( true,  "",  "");
                return;
            }

        }

        //만일 바코드번호가 있는경우 스캐너를 열지 않고 해당 값으로 비교한다
        if (this.바코드번호 != null && !바코드번호.equals("")) {
            Res_Scanner( true,  "",  this.바코드번호);
            return;
        }


        Clear();
        mPaymentListener.result("바코드(QR) 번호가 입력되지 않았습니다", "ERROR", new HashMap<>());
        return;
//        Utils.ScannerOpen(Sdk: "KAKAO")

    }

    /**
     스캐너 결과값
     */
    public void Res_Scanner(boolean _result, String _msg, String _scanner)
    {
        if (_result != true)
        {
            Clear();
            mPaymentListener.result(_msg, "ERROR", new HashMap<>());
            return;
        }

        this.바코드번호 = _scanner;

        String scan_result = Scan_Data_Parser(_scanner);

        if (_scanner != null && !_scanner.equals("")) {
            if (전문번호.equals("Z20")) {
                //제로페이 취소일 경우
                scan_result = Constants.EasyPayMethod.Zero_Bar.toString();
            }
        }
        if (_scanner != null && !_scanner.equals("")) {
            if (전문번호.equals("E20")) {
                if(QrKind.equals("WC"))
                {
                    //위쳇페이 취소일 경우
                    scan_result = Constants.EasyPayMethod.Wechat.toString();
                }

            }
        }

        if(QrKind.equals("AP"))
        {
            if(_scanner != null && !_scanner.equals(""))
            {
                scan_result = Scan_Data_Parser(_scanner);
            }
            else
            {
                scan_result = Constants.EasyPayMethod.App_Card.toString();
            }

        }
        else if(QrKind.equals("ZP"))
        {
            scan_result = Constants.EasyPayMethod.Zero_Bar.toString();
        }
        else if(QrKind.equals("KP"))
        {
            scan_result = Constants.EasyPayMethod.Kakao.toString();
        }
        else if(QrKind.equals("AL"))
        {
            scan_result = Constants.EasyPayMethod.Ali.toString();
        }
        else if(QrKind.equals("WC"))
        {
            scan_result = Constants.EasyPayMethod.Wechat.toString();
        }

        String 취소정보 = "";
        if (this.취소구분 != null && !this.취소구분.equals("")) { 취소정보 = this.취소구분 + this.원거래일자.substring(0,6) + this.원승인번호; }

        // 금액이 5만원 이상, 이하인 경우
        int TaxConvert = 0 ;
        if(this.세금 != null && !this.세금.equals("")) {
            TaxConvert = Integer.parseInt(this.세금);
        }
        int SrvCharge = 0;
        if(this.봉사료 != null && !this.봉사료.equals("")) {
            SrvCharge = Integer.parseInt(this.봉사료);
        }
        int TotalMoney = Integer.parseInt(this.거래금액) + TaxConvert + SrvCharge;

        int compareMoney = 0;
        compareMoney = Integer.parseInt(
                (Setting.getPreference(mCtx,Constants.UNSIGNED_SETMONEY)).equals("") ?
                        "50000":Setting.getPreference(mCtx,Constants.UNSIGNED_SETMONEY)
        );


        String convertMoney = "";
        String convertTax = "";
        String convertSvc = "";
        String convertTaf = "";
        if (this.세금 == null || this.세금.equals(" ") || this.세금.equals("") || this.세금.equals("0")) { this.세금 = ""; }
        else
        {
//            convertTax = StringUtil.leftPad(this.세금.replace(" ",""), "0", 12);
        }
        if (this.봉사료 == null || this.봉사료.equals(" ") || this.봉사료.equals("") || this.봉사료.equals("0")) { this.봉사료 = ""; }
        else
        {
//            convertSvc = StringUtil.leftPad(this.봉사료.replace(" ",""), "0", 12);
        }
        if (this.비과세 == null || this.비과세.equals(" ") || this.비과세.equals("") || this.비과세.equals("0")) { this.비과세 = ""; }
        else
        {
//            convertTaf = StringUtil.leftPad(this.비과세.replace(" ",""), "0",  12);
        }

//        convertMoney = StringUtil.leftPad(this.거래금액.replace(" ",""),  "0",  12);

        if (mDBAppToApp)
        {
            if(TotalMoney > compareMoney)
            {
                if(Setting.getDscyn().equals("1"))
                {
                    Setting.g_sDigSignInfo = "6";
                    전자서명사용여부 = "6";
                    String final취소정보 = 취소정보;
                    String finalConvertMoney = convertMoney;
                    String finalConvertTax = convertTax;
                    String finalConvertSvc = convertSvc;
                    String finalConvertTaf = convertTaf;
                    String finalScan_result = scan_result;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            switch (finalScan_result) {
                                case "EMV":
                                    ReadySignPad(mCtx,TotalMoney);
                                    break;
                                case "Kakao":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                            TCPCommand.CMD_KAKAOPAY_SEARCH_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                    if(전문번호.equals(TCPCommand.CMD_KAKAOPAY_CANCEL_REQ))
                                    {
                                        ReadySignPad(mCtx,TotalMoney);
                                        return;
                                    }
                                    mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                            거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                            점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, Setting.getDscData().getBytes(), 가맹점데이터,mTcpDatalistener);
                                    return;
                                case "Zero_Bar":
                                case "Zero_Qr":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                            TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                    mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                            거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                    return;
                                case "Wechat":
                                case "Ali":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                            TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                    Clear();
                                    mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                    return;
                                case "App_Card":
                                    ReadySignPad(mCtx,TotalMoney);
                                    break;
                                default:
                                    ReadySignPad(mCtx,TotalMoney);
                                    break;
                            }
                        }
                    }, 1000);


                }
                else if(Setting.getDscyn().equals("2"))
                {
                    Setting.g_sDigSignInfo = "B";
                    전자서명사용여부 = "B";
                    String final취소정보 = 취소정보;
                    String finalConvertMoney = convertMoney;
                    String finalConvertTax = convertTax;
                    String finalConvertSvc = convertSvc;
                    String finalConvertTaf = convertTaf;
                    String finalScan_result = scan_result;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            switch (finalScan_result) {
                                case "EMV":
                                    입력방법 = "U";
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                            TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                    mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "U", "", "",
                                            new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, Utils.AppTmlcNo(),
                                            "", "", "", "", 바코드번호.getBytes(StandardCharsets.UTF_8),
                                            _WorkingKeyIndex, "", "", "", "", "", "",
                                            "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", Setting.getDscData().getBytes(), "",
                                            가맹점데이터, "", "",
                                            Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                    break;
                                case "Kakao":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                            TCPCommand.CMD_KAKAOPAY_SEARCH_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                    mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                            거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                            점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, Setting.getDscData().getBytes(), 가맹점데이터,mTcpDatalistener);
                                    break;
                                case "Zero_Bar":
                                case "Zero_Qr":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                            TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                    mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                            거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                    break;
                                case "Wechat":
                                case "Ali":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                            TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                    Clear();
                                    mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                    break;
                                case "App_Card":
                                    입력방법 = "K";
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                            TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                    mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "K", 바코드번호 + "=8911",
                                            "",new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월,
                                            Utils.AppTmlcNo(), "", "", "", "",바코드번호.substring(0,6).getBytes(),
                                            _WorkingKeyIndex, "", "", "", "", "", "",
                                            "", "",new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "",Setting.getDscData().getBytes(), "",
                                            가맹점데이터, "", "",
                                            Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }, 1000);
                }
                else    //금액이 오만원 이하의 경우에는 서버에 결제 요청을 진행 한다.
                {
                    //무서명으로 올경우 향후 제거될 예정
                    Setting.g_sDigSignInfo = "6";
                    전자서명사용여부 = "6";
                    String final취소정보 = 취소정보;
                    String finalConvertMoney = convertMoney;
                    String finalConvertTax = convertTax;
                    String finalConvertSvc = convertSvc;
                    String finalConvertTaf = convertTaf;
                    String finalScan_result = scan_result;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            switch (finalScan_result) {
                                case "EMV":
                                    ReadySignPad(mCtx,TotalMoney);
                                    break;
                                case "Kakao":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                            TCPCommand.CMD_KAKAOPAY_SEARCH_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                    if(전문번호.equals(TCPCommand.CMD_KAKAOPAY_CANCEL_REQ))
                                    {
                                        ReadySignPad(mCtx,TotalMoney);
                                        return;
                                    }
                                    mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                            거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                            점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, Setting.getDscData().getBytes(), 가맹점데이터,mTcpDatalistener);
                                    return;
                                case "Zero_Bar":
                                case "Zero_Qr":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                            TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                    mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                            거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                    return;
                                case "Wechat":
                                case "Ali":
                                    전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                            TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                    Clear();
                                    mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                    return;
                                case "App_Card":
                                    ReadySignPad(mCtx,TotalMoney);
                                    break;
                                default:
                                    ReadySignPad(mCtx,TotalMoney);
                                    break;
                            }
                        }
                    }, 1000);

//                    Setting.g_sDigSignInfo = "5";
//                    Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
                }
            }
            else
            {
                //금액이 5만원이하
                Setting.g_sDigSignInfo = "5";
                전자서명사용여부 = "5";
                String final취소정보 = 취소정보;
                String finalConvertMoney = convertMoney;
                String finalConvertTax = convertTax;
                String finalConvertSvc = convertSvc;
                String finalConvertTaf = convertTaf;
                String finalScan_result = scan_result;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (finalScan_result) {
                            case "EMV":
                                입력방법 = "U";
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                        TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "U", "", "",
                                        new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, Utils.AppTmlcNo(),
                                        "", "", "", "", 바코드번호.getBytes(StandardCharsets.UTF_8),
                                        _WorkingKeyIndex, "", "", "", "", "", "",
                                        "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", new byte[]{}, "",
                                        가맹점데이터, "", "",
                                        Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                break;
                            case "Kakao":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                        TCPCommand.CMD_KAKAOPAY_SEARCH_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                        점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, new byte[]{}, 가맹점데이터,mTcpDatalistener);
                                break;
                            case "Zero_Bar":
                            case "Zero_Qr":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                        TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                break;
                            case "Wechat":
                            case "Ali":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                        TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                Clear();
                                mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                break;
                            case "App_Card":
                                입력방법 = "K";
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                        TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "K", 바코드번호 + "=8911",
                                        "",new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월,
                                        Utils.AppTmlcNo(), "", "", "", "",바코드번호.substring(0,6).getBytes(),
                                        _WorkingKeyIndex, "", "", "", "", "", "",
                                        "", "",new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "",new byte[]{}, "",
                                        가맹점데이터, "", "",
                                        Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                break;
                            default:
                                break;
                        }
                    }
                }, 1000);

            }
        }
        else
        {
            //TODO: 앱투앱이 아닌경우 비교하는 가격이 5만원이면 안된다
            if(TotalMoney > compareMoney)
            {
//                Setting.g_sDigSignInfo = "B";
//                전자서명사용여부 = "B";
                Setting.g_sDigSignInfo = "6";
                전자서명사용여부 = "6";
                String final취소정보 = 취소정보;
                String finalConvertMoney = convertMoney;
                String finalConvertTax = convertTax;
                String finalConvertSvc = convertSvc;
                String finalConvertTaf = convertTaf;
                String finalScan_result = scan_result;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (finalScan_result) {
                            case "EMV":
                                ReadySignPad(mCtx,TotalMoney);
                                break;
                            case "Kakao":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                        TCPCommand.CMD_KAKAOPAY_SEARCH_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                if(전문번호.equals(TCPCommand.CMD_KAKAOPAY_CANCEL_REQ))
                                {
                                    ReadySignPad(mCtx,TotalMoney);
                                    return;
                                }
                                mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                        점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, Setting.getDscData().getBytes(), 가맹점데이터,mTcpDatalistener);
                                return;
                            case "Zero_Bar":
                            case "Zero_Qr":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                        TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                return;
                            case "Wechat":
                            case "Ali":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                        TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                Clear();
                                mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                return;
                            case "App_Card":
                                ReadySignPad(mCtx,TotalMoney);
                                break;
                            default:
                                ReadySignPad(mCtx,TotalMoney);
                                break;
                        }
                    }
                }, 1000);

            }
            else    //금액이 오만원 이하의 경우에는 서버에 결제 요청을 진행 한다.
            {
                //금액이 5만원이하
                Setting.g_sDigSignInfo = "5";
                전자서명사용여부 = "5";
                String final취소정보 = 취소정보;
                String finalConvertMoney = convertMoney;
                String finalConvertTax = convertTax;
                String finalConvertSvc = convertSvc;
                String finalConvertTaf = convertTaf;
                String finalScan_result = scan_result;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (finalScan_result) {
                            case "EMV":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                        TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "U", "", "",
                                        new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, Utils.AppTmlcNo(),
                                        "", "", "", "", 바코드번호.getBytes(StandardCharsets.UTF_8),
                                        _WorkingKeyIndex, "", "", "", "", "", "",
                                        "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", new byte[]{}, "",
                                        가맹점데이터, "", "",
                                        Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                break;
                            case "Kakao":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                        TCPCommand.CMD_KAKAOPAY_SEARCH_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                        점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, new byte[]{}, 가맹점데이터,mTcpDatalistener);
                                break;
                            case "Zero_Bar":
                            case "Zero_Qr":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                        TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                break;
                            case "Wechat":
                            case "Ali":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                        TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                Clear();
                                mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                break;
                            case "App_Card":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                        TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "K", 바코드번호 + "=8911",
                                        "",new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월,
                                        Utils.AppTmlcNo(), "", "", "", "",바코드번호.substring(0,6).getBytes(),
                                        _WorkingKeyIndex, "", "", "", "", "", "",
                                        "", "",new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "",new byte[]{}, "",
                                        가맹점데이터, "", "",
                                        Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                break;
                            default:
                                break;
                        }
                    }
                }, 1000);
            }
        }


    }

    /**
     사인패드에서 이미지를 그리면 이곳으로 보낸다
     */
    public void Result_SignPad(boolean _sign, byte[] _signImage)
    {
        전자서명데이터 = _signImage;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!_sign)
                {
                    Clear();
                    mPaymentListener.result("서명오류입니다. 서명이 정상적으로 진행되지 않았습니다", "ERROR", new HashMap<>());
                    return;
                }

                String scan_result = Scan_Data_Parser(바코드번호);

                if (바코드번호 != null && !바코드번호.equals("")) {
                    if (전문번호.equals("Z20")) {
                        //제로페이 취소일 경우
                        scan_result = Constants.EasyPayMethod.Zero_Bar.toString();
                    }
                }

                if(QrKind.equals("AP"))
                {
                    if(바코드번호 != null && !바코드번호.equals(""))
                    {
                        scan_result = Scan_Data_Parser(바코드번호);
                    }
                    else
                    {
                        scan_result = Constants.EasyPayMethod.App_Card.toString();
                    }
                    if(scan_result.equals("EMV"))
                    {
                        scan_result = Constants.EasyPayMethod.EMV.toString();
                    }
                    else
                    {
                        scan_result = Constants.EasyPayMethod.App_Card.toString();
                    }

                }
                else if(QrKind.equals("ZP"))
                {
                    scan_result = Constants.EasyPayMethod.Zero_Bar.toString();
                }
                else if(QrKind.equals("KP"))
                {
                    scan_result = Constants.EasyPayMethod.Kakao.toString();
                }
                else if(QrKind.equals("AL"))
                {
                    scan_result = Constants.EasyPayMethod.Ali.toString();
                }
                else if(QrKind.equals("WC"))
                {
                    scan_result = Constants.EasyPayMethod.Wechat.toString();
                }
                String 취소정보 = "";
                if (취소구분 != null && !취소구분.equals("")) { 취소정보 = 취소구분 + 원거래일자.substring(0,6) + 원승인번호; }
                String convertMoney = "";
                String convertTax = "";
                String convertSvc = "";
                String convertTaf = "";
                if (세금 == null || 세금.equals(" ") || 세금.equals("") || 세금.equals("0")) { 세금 = ""; }
                else
                {
//                    convertTax = StringUtil.leftPad(세금.replace(" ",""), "0", 12);
                }
                if (봉사료 == null || 봉사료.equals(" ") || 봉사료.equals("") || 봉사료.equals("0")) { 봉사료 = ""; }
                else
                {
//                    convertSvc = StringUtil.leftPad(봉사료.replace(" ",""), "0", 12);
                }
                if (비과세 == null || 비과세.equals(" ") || 비과세.equals("") || 비과세.equals("0")) { 비과세 = ""; }
                else
                {
//                    convertTaf = StringUtil.leftPad(비과세.replace(" ",""), "0",  12);
                }

//                convertMoney = StringUtil.leftPad(거래금액.replace(" ",""),  "0",  12);

                String final취소정보 = 취소정보;
                String finalConvertMoney = convertMoney;
                String finalConvertTax = convertTax;
                String finalConvertSvc = convertSvc;
                String finalConvertTaf = convertTaf;
                String finalScan_result = scan_result;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (finalScan_result) {
                            case "EMV":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                        TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "U", "", "",
                                        new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, Utils.AppTmlcNo(),
                                        "", "", "", "", 바코드번호.getBytes(StandardCharsets.UTF_8),
                                        _WorkingKeyIndex, "", "", "", "", "", "",
                                        "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", 전자서명데이터, "",
                                        가맹점데이터, "", "",
                                        Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                break;
                            case "Kakao":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_KAKAOPAY_SEARCH_REQ) ?
                                        TCPCommand.CMD_KAKAOPAY_REQ : TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                                mPosSdk.___KakaoPay(전문번호,_Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, 입력방법, 바코드번호,OTC카드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, 취소종류, 취소타입,
                                        점포코드, _PEM, _trid, 카드BIN,조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터,mTcpDatalistener);
                                break;
                            case "Zero_Bar":
                            case "Zero_Qr":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ZEROPAY_REQ) ?
                                        TCPCommand.CMD_ZEROPAY_REQ : TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                                mPosSdk.___ZeroPay(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보,취소구분, 입력방법, 원거래일자, 원승인번호,바코드번호,
                                        거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 가맹점추가정보,
                                        가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                                break;
                            case "Wechat":
                            case "Ali":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_WECHAT_ALIPAY_REQ) ?
                                        TCPCommand.CMD_WECHAT_ALIPAY_REQ : TCPCommand.CMD_WECHAT_ALIPAY_CANCEL_REQ;
                                Clear();
                                mPaymentListener.result("거래불가. 위쳇 알리페이는 지원하지 않습니다.", "ERROR", new HashMap<>());
                                break;
                            case "App_Card":
                                전문번호 = 전문번호.equals(TCPCommand.CMD_KAKAOPAY_REQ) || 전문번호.equals(TCPCommand.CMD_ICTRADE_REQ) ?
                                        TCPCommand.CMD_ICTRADE_REQ : TCPCommand.CMD_ICTRADE_CANCEL_REQ;
                                mPosSdk.___ictrade(전문번호, _Tid, 거래일시, 단말버전, 단말추가정보, final취소정보, "K", 바코드번호 + "=8911",
                                        "",new byte[]{}, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월,
                                        Utils.AppTmlcNo(), "", "", "", "",바코드번호.substring(0,6).getBytes(),
                                        _WorkingKeyIndex, "", "", "", "", "", "",
                                        "", "",new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "",전자서명데이터, "",
                                        가맹점데이터, "", "",
                                        Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                                break;
                            default:
                                break;
                        }
                    }
                }, 1000);
            }
        },500);

    }

    /**
     스캔한 데이터를 파싱해서 제로/카카오/emv 등을 구분한다
     */
    private String Scan_Data_Parser(String _scan)
    {
        String returnData = "";
        if(_scan == null || _scan.equals("") || _scan.length() < 2)
        {
            return returnData;
        }
        if (_scan.length()>=7 && _scan.substring(0,7).equals("hQVDUFY")) {
            //EMV QR
            returnData = Constants.EasyPayMethod.EMV.toString();
        } else if (_scan.length()>=6 &&_scan.substring(0,6).equals("281006")) {
            //카카오페이
            returnData = Constants.EasyPayMethod.Kakao.toString();
        } else if (_scan.length()>=6 &&_scan.substring(0,6).equals("800088")) {
            //제로페이 Barcode
            returnData = Constants.EasyPayMethod.Zero_Bar.toString();
        } else if (_scan.substring(0,2).equals("3-")) {
            //제로페이 QRcode
            returnData = Constants.EasyPayMethod.Zero_Qr.toString();
        } else {
            if (_scan.substring(0,2).equals("11") || _scan.substring(0,2).equals("12") ||
                    _scan.substring(0,2).equals("13") || _scan.substring(0,2).equals("14") ||
                    _scan.substring(0,2).equals("15") || _scan.substring(0,2).equals("10"))  {
                //위쳇페이
                returnData = Constants.EasyPayMethod.Wechat.toString();
            } else if (_scan.substring(0,2).equals("25") || _scan.substring(0,2).equals("26") ||
                    _scan.substring(0,2).equals("27") || _scan.substring(0,2).equals("28") ||
                    _scan.substring(0,2).equals("29") || _scan.substring(0,2).equals("30")) {
                //알리페이
                returnData = Constants.EasyPayMethod.Ali.toString();
            } else {
                if (_scan.length() == 21) {
                    //APP 카드
                    returnData = Constants.EasyPayMethod.App_Card.toString();
                }
            }
        }

        return returnData;
    }





    /**
     * 서버에서 응답을 받은 데이터
     */
    private TcpInterface.DataListener mTcpDatalistener = new TcpInterface.DataListener()
    {
        @Override
        public void onRecviced(byte[] _rev)
        {
            if(_rev==null || _rev.length < 10)
            {
                ((BaseActivity)mCtx).ReadyDialogHide();
                mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
                return;
            }
            //mPaymentListener.result("여기까지 일단 서버 통신 완료","OK");
            Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);

            //신용 승인 취소 응답"A25" 이 오면 Eot 한번만 취소한고 끝낸다.
            KByteArray _b = new KByteArray(_rev);
            _b.CutToSize(10);
            String _creditCode = new String(_b.CutToSize(3));

            switch (tp.getResponseCode())
            {
                case TCPCommand.CMD_KAKAOPAY_SEARCH_RES:    //카카오페이 전문 승인조회 응답(승인요청 전에 조회부터 처리한다)
                    Res_TCP_KakaoSearch(tp.getResData(),tp.Date,tp.getTerminalID(),_creditCode);
                    break;
                case TCPCommand.CMD_KAKAOPAY_RES:           //카카오페이 전문 응답
                case TCPCommand.CMD_KAKAOPAY_CANCEL_RES:    //카카오페이 전문 취소 응답
                    Res_TCP_Kakao(tp.getResData(),tp.Date,tp.getTerminalID(),_creditCode);
                    break;
                case TCPCommand.CMD_ZEROPAY_SEARCH_RES:    //제로페이 전문 취소조회 응답(취소 거래 이며 응답 코드 "0100" 수신 시 응답 메세지"처리결과 확인 필요" 문구 포스 디스플레이 후 가맹점 사용자 확인 후 취소 조회 업무 진행하여 정상 취소 처 리 여부 확인 필요)
                case TCPCommand.CMD_ZEROPAY_RES:           //제로페이 전문 응답
                case TCPCommand.CMD_ZEROPAY_CANCEL_RES:    //제로페이 전문 취소 응답
                    Res_TCP_Zero(tp.getResData(),tp.Date,tp.getTerminalID(),_creditCode);
                    break;
                case TCPCommand.CMD_IC_OK_RES:             //앱카드, EMVQR 전문 응답
                case TCPCommand.CMD_IC_CANCEL_RES:         //앱카드, EMVQR 전문 취소 응답
                    Res_TCP_Credit(tp.getResData(),tp.Date,tp.getTerminalID(),_creditCode);
                    break;
                default:
                    ((BaseActivity)mCtx).ReadyDialogHide();
                    mPaymentListener.result("서버 수신 데이터 오류." + Utils.ByteArrayToString(_creditCode.getBytes()),"ERROR",new HashMap<>());
                    return;
            }

        }
    };

    /**
     * 서버로 부터 신용거래를 받아서 eot 수신을 체크한다
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _creditCode
     */
    private void Res_TCP_Zero(List<byte[]> _res, String _date, String _TerminalID, String _creditCode)
    {
        if(_res==null)
        {
            ((BaseActivity)mCtx).ReadyDialogHide();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }
        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        int TaxConvert = 0 ;
        if(this.세금 != null && !this.세금.equals("")) {
            TaxConvert = Integer.parseInt(this.세금);
        }
        int SrvCharge = 0;
        if(this.봉사료 != null && !this.봉사료.equals("")) {
            SrvCharge = Integer.parseInt(this.봉사료);
        }
        int TxfCharge = 0;
        if(this.비과세 != null && !this.비과세.equals("")) {
            TxfCharge = Integer.parseInt(this.비과세);
        }
        int TotalMoney = Integer.parseInt(this.거래금액) + TaxConvert + SrvCharge + TxfCharge;

        String convertMoney = "";
        String convertTax = "";
        String convertSvc = "";
        String convertTaf = "";

        convertTax = "0";
        convertSvc = "0";
        convertTaf = "0";

        convertMoney = String.valueOf(TotalMoney);

        if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            int _tmpEot = 0;
            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
            {
                if (Setting.getEOTResult() != 0) {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult()); break;
                }
                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                _tmpEot ++;
                if(_tmpEot >= 30)
                {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult());
                    break;
                }
            }

            if (Setting.getEOTResult() == 1 && _creditCode.equals("Z15"))   //eot정상 정상응답
            {
                Res_TCP_Zero_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
            else if (Setting.getEOTResult() == -1  && _creditCode.equals("Z15")) {  //망취소한다
                String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //신용승인번호
                mICCancelInfo = "I" + _date + ic_appro_number;
                byte[] _tmpCancel = mICCancelInfo.getBytes();
                _tmpCancel[0] = (byte) 0x49;
                mICCancelInfo = new String(_tmpCancel);
                mEotCancel=1;
                전문번호 = TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                조회고유번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13));
                mPosSdk.___ZeroPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보,"1", 입력방법, 원거래일자, 원승인번호,바코드번호,
                        convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);

                mICCancelInfo = "";
                Setting.setEOTResult(0);
            }
            else if(Setting.getEOTResult() == 0 && _creditCode.equals("Z15"))   //승인일때 들어와서 시도하며 취소일때는 하지 않는다. 망취소한다
            {
                mEotCancel=1;
                mICCancelInfo = "I" + _date.substring(0,6) + Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3));
                조회고유번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13));
                전문번호 = TCPCommand.CMD_ZEROPAY_CANCEL_REQ;
                mPosSdk.___ZeroPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보,"1", 입력방법, 원거래일자, 원승인번호,바코드번호,
                        convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);

            }
            else if(_creditCode.equals("Z25"))  //정상취소
            {
                Res_TCP_Zero_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
            else if(_creditCode.equals("Z35"))   //조회업무
            {
                Res_TCP_Zero_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
        }
        else    //기타오류코드로인한 종료
        {
            if (code.equals("0100"))
            {
                전문번호 = TCPCommand.CMD_ZEROPAY_SEARCH_REQ;
                mPosSdk.___ZeroPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보,"", "", 원거래일자, 원승인번호,바코드번호,
                        convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호,mTcpDatalistener);
                return;
            }

            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            Res_TCP_Zero_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
        }
    }

    /**
     * eot수신 체크 후에 서버로부터 받은 신용거래 결과
     * @param data
     * @param _date
     * @param _TerminalID
     * @param _EotCancel
     * @param code
     * @param ic_result_message
     * @param _creditCode
     */
    private void Res_TCP_Zero_Sucess(List<byte[]> data,String _date,String _TerminalID,int _EotCancel,String code,String ic_result_message,String _creditCode)
    {
        //String ic_result_message = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(1)); //IC_응답메세지
        String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //신용승인번호
        String koces_tran_unique_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //koces거래고유번호
        String issuer_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(4)); //기관코드
        String issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(5),"EUC-KR"); //기관명

        String merchant_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(6)); //가맹점번호

        String MchFee = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(7)); //가맹점수수료
        String MchRefund = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(8)); //가맹점환불금액

        String merchant_data = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)); //가맹점데이터

        String trd_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(10)); //거래금액
        String tax_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(11)); //세금
        String svc_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(12)); //봉사료
        String tax_free_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13)); //비과세

        KOCES거래고유번호 = koces_tran_unique_num;
        sendData = new HashMap<>();
        if(mDBAppToApp)
        {
            if (_creditCode.equals(TCPCommand.CMD_ZEROPAY_RES))
            {
                sendData.put("TrdType","E15");
            }
            else if(_creditCode.equals(TCPCommand.CMD_ZEROPAY_CANCEL_RES))
            {
                sendData.put("TrdType","E25");
            }
            else
            {
                sendData.put("TrdType","E35");
            }
        }
        else
        {
            if (_creditCode.equals(TCPCommand.CMD_ZEROPAY_RES))
            {
                sendData.put("TrdType","A15");
            }
            else if(_creditCode.equals(TCPCommand.CMD_ZEROPAY_CANCEL_RES))
            {
                sendData.put("TrdType","A25");
            }
            else
            {
                sendData.put("TrdType","A35");
            }
        }

        sendData.put("TermID",_TerminalID);
        sendData.put("TrdDate",_date);
        sendData.put("AnsCode", code);
        sendData.put("Message", ic_result_message);
        sendData.put("AuNo",ic_appro_number);
        sendData.put("TradeNo", KOCES거래고유번호);
//        if(!print_card_num.equals(""))
//        {
//            sendData.put("CardNo", print_card_num.substring(0,6));
//        }
//        else
//        {
//            sendData.put("CardNo", print_card_num);
//        }
//        sendData.put("Keydate", encryp_key_expiration_date);
        String _cardNum = this.바코드번호;
        if(_cardNum != null && !_cardNum.equals("") && _cardNum.length()>=8)
        {
            sendData.put("CardNo", this.바코드번호.substring(0,8));
        }
        else
        {
            sendData.put("CardNo", "");
        }

        sendData.put("MchData", merchant_data);
        sendData.put("CardKind", "");
//        sendData.put("CardKind", card_type); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
        sendData.put("OrdCd", issuer_code);   //발급사코드
        sendData.put("OrdNm", issuer_name);   //발급사명
        sendData.put("InpNm", "");   //매입사명
        sendData.put("InpCd", "");   //매입사코드
        sendData.put("DDCYn", "");   //DDC 여부
        sendData.put("EDCYn", "");   //EDC 여부
//        sendData.put("GiftAmt", giftcard_money); //기프트카드 잔액
        sendData.put("MchNo", merchant_number);   //가맹점번호

        sendData.put("DisAmt", "");   //카카오페이할인금액
        sendData.put("AuthType", "");   //카카오페이 결제수단
        sendData.put("AnswerTrdNo", "");   //위쳇페이거래고유번호

        sendData.put("ChargeAmt", MchFee);   //가맹점수수료
        sendData.put("RefundAmt", MchRefund);   //가맹점환불금액
        sendData.put("QrKind", "ZP");   //간편결제 거래종류


        KOCES거래고유번호 = "";
        //       mARD = 0x00; mIAD = null; mIS = null;
        String codeMessage = "";
        try {
            codeMessage = Utils.getByteToString_euc_kr(data.get(1));
            sendData.put("Message",codeMessage);
        } catch (UnsupportedEncodingException ex) {

        }
        boolean _giftCheck = false;
        if (codeMessage.contains("잔액"))
        {
            _giftCheck = true;
            sendData.put("GiftAmt", codeMessage); //기프트카드 잔액
//            codeMessage.replace("잔액","");
//            codeMessage.replace(":","");
//            codeMessage.replace(" ","");
//            codeMessage.replace("원","");
        }
        if(_EotCancel==0)
        {
            mEotCancel=0;
            if((ic_appro_number.replace(" ","")).equals(""))
            {
                mPaymentListener.result("거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());
                Clear();
                return;
            }
            if (!mDBAppToApp && !_creditCode.equals(TCPCommand.CMD_ZEROPAY_SEARCH_RES))
            {
                mPosSdk.setSqliteDB_InsertTradeData(
                        _Tid,
                        mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                        sqliteDbSdk.TradeMethod.Zero,
                        _creditCode.equals(TCPCommand.CMD_ZEROPAY_RES) ? sqliteDbSdk.TradeMethod.NoCancel:sqliteDbSdk.TradeMethod.Cancel,
                        Integer.parseInt(거래금액.equals("") ? "0":거래금액),
                        _giftCheck ? codeMessage:"0",
                        Integer.parseInt(세금.equals("") ? "0":세금),
                        Integer.parseInt(봉사료.equals("") ? "0":봉사료),
                        Integer.parseInt(비과세.equals("") ? "0":비과세),
                        Integer.parseInt(할부개월.equals("") ? "0":할부개월),
                        sqliteDbSdk.TradeMethod.NULL, sqliteDbSdk.TradeMethod.NULL,"", "",
                        "",
                        "",
                        issuer_name,
                        merchant_number,
                        _date,
                        _creditCode.equals(TCPCommand.CMD_ZEROPAY_RES) ? "":원거래일자,
                        ic_appro_number,
                        _creditCode.equals(TCPCommand.CMD_ZEROPAY_RES) ? "":원승인번호,
                        KOCES거래고유번호, codeMessage,
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
                        MchFee,
                        MchRefund);
            }

            mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
        }
        else
        {
            mEotCancel=0;

            mPaymentListener.result("망취소 발생, 거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
        }
        //사용자 카드관련 정보 초기화_________________________________________________________________________
        Clear();
        //__________________________________________________________________________________________________
    }

    /**
     * 서버로 부터 신용거래를 받아서 eot 수신을 체크한다
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _creditCode
     */
    private void Res_TCP_KakaoSearch(List<byte[]> _res, String _date, String _TerminalID, String _creditCode)
    {
        if(_res==null)
        {
            ((BaseActivity)mCtx).ReadyDialogHide();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }
        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        if(code.equals("0000") || code.equals("0001") || code.equals("0002"))
        {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            int _tmpEot = 0;
            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
            {
                if (Setting.getEOTResult() != 0) {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult()); break;
                }
                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                _tmpEot ++;
                if(_tmpEot >= 30)
                {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult());
                    break;
                }
            }

            if (Setting.getEOTResult() == 1) //eot정상 정상응답
            {

                전문번호 = TCPCommand.CMD_KAKAOPAY_REQ;
                OTC카드번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)).getBytes();
                결제수단 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(4));
                _PEM = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(10));
                _trid = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(11));
                카드BIN = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(12));
                조회고유번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13));
                // 금액이 5만원 이상, 이하인 경우
                int TaxConvert = 0 ;
                if(this.세금 != null && !this.세금.equals("")) {
                    TaxConvert = Integer.parseInt(this.세금);
                }
                int SrvCharge = 0;
                if(this.봉사료 != null && !this.봉사료.equals("")) {
                    SrvCharge = Integer.parseInt(this.봉사료);
                }
                int TotalMoney = Integer.parseInt(this.거래금액) + TaxConvert + SrvCharge;

                int compareMoney = 0;
                compareMoney = Integer.parseInt(
                        (Setting.getPreference(mCtx,Constants.UNSIGNED_SETMONEY)).equals("") ?
                                "50000":Setting.getPreference(mCtx,Constants.UNSIGNED_SETMONEY)
                );


                String convertMoney = "";
                String convertTax = "";
                String convertSvc = "";
                String convertTaf = "";
                if (mDBAppToApp)
                {
                    if(TotalMoney > compareMoney)
                    {
                        if(Setting.getDscyn().equals("1"))
                        {
                            Setting.g_sDigSignInfo = "6";
                            전자서명사용여부 = "6";
                            if(!결제수단.equals("C"))
                            {
                                mPosSdk.___KakaoPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, "", 입력방법, 바코드번호,
                                        OTC카드번호, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, "0", "B",
                                        점포코드, _PEM, _trid, 카드BIN, 조회고유번호, _WorkingKeyIndex, 전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터, mTcpDatalistener);
                                return;
                            }
                            ReadySignPad(mCtx,TotalMoney);
                        }
                        else if(Setting.getDscyn().equals("2"))
                        {
                            Setting.g_sDigSignInfo = "B";
                            전자서명사용여부 = "B";
                            mPosSdk.___KakaoPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, "", 입력방법, 바코드번호,
                                    OTC카드번호, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, "0", "B",
                                    점포코드, _PEM, _trid, 카드BIN, 조회고유번호, _WorkingKeyIndex, 전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터, mTcpDatalistener);

                        }
                        else    //금액이 오만원 이하의 경우에는 서버에 결제 요청을 진행 한다.
                        {
                            //무서명으로 올경우 향후 제거될 예정
                            Setting.g_sDigSignInfo = "6";
                            전자서명사용여부 = "6";
                            if(!결제수단.equals("C"))
                            {
                                mPosSdk.___KakaoPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, "", 입력방법, 바코드번호,
                                        OTC카드번호, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, "0", "B",
                                        점포코드, _PEM, _trid, 카드BIN, 조회고유번호, _WorkingKeyIndex, 전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터, mTcpDatalistener);
                                return;
                            }
                            ReadySignPad(mCtx,TotalMoney);
//                    Setting.g_sDigSignInfo = "5";
//                    Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
                        }
                    }
                    else
                    {
                        //금액이 5만원이하
                        Setting.g_sDigSignInfo = "5";
                        전자서명사용여부 = "5";
                        mPosSdk.___KakaoPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, "", 입력방법, 바코드번호,
                                OTC카드번호, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, "0", "B",
                                점포코드, _PEM, _trid, 카드BIN, 조회고유번호, _WorkingKeyIndex, 전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터, mTcpDatalistener);

                    }
                }
                else
                {
                    //TODO: 앱투앱이 아닌경우 비교하는 가격이 5만원이면 안된다
                    if(TotalMoney > compareMoney)
                    {
//                Setting.g_sDigSignInfo = "B";
//                전자서명사용여부 = "B";
                        Setting.g_sDigSignInfo = "6";
                        전자서명사용여부 = "6";
                        if(!결제수단.equals("C"))
                        {
                            mPosSdk.___KakaoPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, "", 입력방법, 바코드번호,
                                    OTC카드번호, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, "0", "B",
                                    점포코드, _PEM, _trid, 카드BIN, 조회고유번호, _WorkingKeyIndex, 전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터, mTcpDatalistener);
                            return;
                        }
                        ReadySignPad(mCtx,TotalMoney);
                    }
                    else    //금액이 오만원 이하의 경우에는 서버에 결제 요청을 진행 한다.
                    {
                        //금액이 5만원이하
                        Setting.g_sDigSignInfo = "5";
                        전자서명사용여부 = "5";
                        mPosSdk.___KakaoPay(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, "", 입력방법, 바코드번호,
                                OTC카드번호, 거래금액, 세금, 봉사료, 비과세, 통화코드, 할부개월, 결제수단, "0", "B",
                                점포코드, _PEM, _trid, 카드BIN, 조회고유번호, _WorkingKeyIndex, 전자서명사용여부, 사인패드시리얼번호, 전자서명데이터, 가맹점데이터, mTcpDatalistener);
                    }
                }


            }
            else if (Setting.getEOTResult() == -1 || Setting.getEOTResult() == 0)   //망취소but 거래가 없이 조회업무이므로 종료
            {
                Res_TCP_Kakao_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }

        }
        else
        {
            //기타오류코드로인한 종료
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            Res_TCP_Kakao_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
        }
    }

    /**
     * 서버로 부터 신용거래를 받아서 eot 수신을 체크한다
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _creditCode
     */
    private void Res_TCP_Kakao(List<byte[]> _res, String _date, String _TerminalID, String _creditCode)
    {
        if(_res==null)
        {
            ((BaseActivity)mCtx).ReadyDialogHide();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }
        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        int TaxConvert = 0 ;
        if(this.세금 != null && !this.세금.equals("")) {
            TaxConvert = Integer.parseInt(this.세금);
        }
        int SrvCharge = 0;
        if(this.봉사료 != null && !this.봉사료.equals("")) {
            SrvCharge = Integer.parseInt(this.봉사료);
        }
        int TxfCharge = 0;
        if(this.비과세 != null && !this.비과세.equals("")) {
            TxfCharge = Integer.parseInt(this.비과세);
        }
        int TotalMoney = Integer.parseInt(this.거래금액) + TaxConvert + SrvCharge + TxfCharge;

        String convertMoney = "";
        String convertTax = "";
        String convertSvc = "";
        String convertTaf = "";

        convertTax = "0";
        convertSvc = "0";
        convertTaf = "0";

        convertMoney = StringUtil.leftPad(String.valueOf(TotalMoney),  "0",  12);

        if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            int _tmpEot = 0;
            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
            {
                if (Setting.getEOTResult() != 0) {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult()); break;
                }
                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                _tmpEot ++;
                if(_tmpEot >= 30)
                {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult());
                    break;
                }
            }

            if (Setting.getEOTResult() == 1 && _creditCode.equals("K26"))   //eot정상 정상응답
            {
                Res_TCP_Kakao_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
            else if (Setting.getEOTResult() == -1  && _creditCode.equals("K26")) {  //망취소한다
                String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //신용승인번호
                mICCancelInfo = "I" + _date + ic_appro_number;
                byte[] _tmpCancel = mICCancelInfo.getBytes();
                _tmpCancel[0] = (byte) 0x49;
                mICCancelInfo = new String(_tmpCancel);
                mEotCancel=1;
                전문번호 = TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                조회고유번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13));
                mPosSdk.___KakaoPay(전문번호,_Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, mICCancelInfo, 입력방법, 바코드번호,OTC카드번호,
                        convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, "", "0", "B",
                        점포코드, "", "", "",조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호,
                        Setting.getDscData().getBytes(), 가맹점데이터,mTcpDatalistener);


                mICCancelInfo = "";
                Setting.setEOTResult(0);
            }
            else if(Setting.getEOTResult() == 0 && _creditCode.equals("K26"))   //승인일때 들어와서 시도하며 취소일때는 하지 않는다. 망취소한다
            {
                mEotCancel=1;
                mICCancelInfo = "I" + _date.substring(0,6) + Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3));
                조회고유번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13));
                전문번호 = TCPCommand.CMD_KAKAOPAY_CANCEL_REQ;
                mPosSdk.___KakaoPay(전문번호,_Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, mICCancelInfo, 입력방법, 바코드번호,OTC카드번호,
                        convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, "", "0", "B",
                        점포코드, "", "", "",조회고유번호, _WorkingKeyIndex,전자서명사용여부, 사인패드시리얼번호,
                        Setting.getDscData().getBytes(), 가맹점데이터,mTcpDatalistener);

            }
            else if(_creditCode.equals("K27"))  //정상취소
            {
                Res_TCP_Kakao_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
        }
        else    //기타오류코드로인한 종료
        {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            Res_TCP_Kakao_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
        }
    }

    /**
     * eot수신 체크 후에 서버로부터 받은 신용거래 결과
     * @param data
     * @param _date
     * @param _TerminalID
     * @param _EotCancel
     * @param code
     * @param ic_result_message
     * @param _creditCode
     */
    private void Res_TCP_Kakao_Sucess(List<byte[]> data,String _date,String _TerminalID,int _EotCancel,String code,String ic_result_message,String _creditCode)
    {
//        String ic_result_message = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(1)); //IC_응답메세지
        String noti_message = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //알림메세지
        String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //승인번호
        String 결제수단 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(4)); //결제수단 C:Card M:Money
        String 카카오페이승인금액 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(5)); //승인금액(카카오머니 승인 응답 시(승인금액 = 거래금액 - 카카오할인금액))
        String 카카오페이할인금액 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(6)); //카카오페이할인금액
        String 카카오멤버쉼바코드 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(7)); //카카오멤버쉼바코드
        String 카카오멤버쉼번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(8)); //카카오멤버쉼번호
        String 카드번호정보 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)); //카드번호정보(OTC)
        String PEM = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(10)); //PEM
        String trid = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(11)); //trid
        String tmpCardNum1 = new String(data.get(12)); //카드BIN
        String[] tmpCardNum2 = tmpCardNum1.split("-");
        tmpCardNum1 = "";
        for(String n:tmpCardNum2)
        {
            tmpCardNum1 += n;
        }

        byte[] _CardNum = tmpCardNum1.getBytes();   //카드BIN

        if(_CardNum != null){
            //System.arraycopy( data.get(4),0,_CardNum,0,6);
            for(int i=8; i<_CardNum.length; i++){
                //* = 0x2A
                _CardNum[i] = (byte)0x2A;
            }
            //카드번호가 올라오지만 그중 6개만 사용하고 나머지는 *로 바꾼다
        }

        String print_card_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //출력용카드번호

        //카드 정보 삭제____________________________________________________________________________________
        Random rand = new Random();
        for(int i=0; i<_CardNum.length; i++){
            //* = 0x2A
            _CardNum[i] = (byte)rand.nextInt(255);;
        }
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x01);
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x00);
        data.set(4,_CardNum);

        String 조회고유번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13)); //조회고유번호
        String 출력용바코드번호 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(14)); //출력용바코드번호(전표출력시사용)

        String card_type = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(15)); //카드종류
        String issuer_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(16)); //발급사코드
        String issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(17),"EUC-KR"); //발급사명
        String purchaser_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(18)); //매입사코드
        String purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(19),"EUC-KR"); //매입사명
        String ddc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(20)); //DDC 여부
        String edc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(21)); //EDC 여부

        String 전표출력여부 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(22)); //전표출력여부
        String 전표구분명 = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(23)); //전표구분명

        String merchant_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(24)); //가맹점번호
        String workingkey = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(25)); //workingkey
        String merchant_data = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(26)); //가맹점데이터

        String trd_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(27)); //거래금액
        String tax_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(28)); //세금
        String svc_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(29)); //봉사료
        String tax_free_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(30)); //비과세

        KOCES거래고유번호 = "";
        sendData = new HashMap<>();
        if(mDBAppToApp)
        {
            sendData.put("TrdType",_creditCode.equals(TCPCommand.CMD_KAKAOPAY_RES) ? "E15":"E25");
        }
        else
        {
            sendData.put("TrdType",_creditCode.equals(TCPCommand.CMD_KAKAOPAY_RES) ? "A15":"A25");
        }

        sendData.put("TermID",_TerminalID);
        sendData.put("TrdDate",_date);
        sendData.put("AnsCode", code);
        sendData.put("Message", ic_result_message);
        sendData.put("AuNo",ic_appro_number);
//        sendData.put("TradeNo", KOCES거래고유번호);
        if(!print_card_num.equals(""))
        {
            print_card_num = print_card_num.substring(0,8);
        }
        sendData.put("CardNo", print_card_num);
//        sendData.put("Keydate", encryp_key_expiration_date);
        sendData.put("MchData", merchant_data);
        sendData.put("CardKind", card_type); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
        sendData.put("OrdCd", issuer_code);   //발급사코드
        sendData.put("OrdNm", issuer_name);   //발급사명
        sendData.put("InpNm", purchaser_name);   //매입사명
        sendData.put("InpCd", purchaser_code);   //매입사코드
        sendData.put("DDCYn", ddc_status);   //DDC 여부
        sendData.put("EDCYn", edc_status);   //EDC 여부
//        sendData.put("GiftAmt", giftcard_money); //기프트카드 잔액
        sendData.put("MchNo", merchant_number);   //가맹점번호
        sendData.put("TradeNo", 조회고유번호);   //조회고유번호
        sendData.put("DisAmt", 카카오페이할인금액);   //카카오할임금액
        sendData.put("AuthType", 결제수단);   //카카오페이 결제수단 C:Card, M:Money

        sendData.put("AnswerTrdNo", "");   //위쳇페이거래고유번호
        sendData.put("ChargeAmt", "");   //가맹점수수료
        sendData.put("RefundAmt", "");   //가맹점환불금액

        sendData.put("QrKind", "KP");   //간편결제 거래종류
//        sendData.put("KaKaoPayAuMoney", 카카오페이승인금액); //승인금액(카카오머니 승인 응답 시(승인금액 = 거래금액 - 카카오할인금액))

        //       mARD = 0x00; mIAD = null; mIS = null;
        String codeMessage = "";
        try {
            codeMessage = Utils.getByteToString_euc_kr(data.get(1));
            sendData.put("Message",codeMessage);
        } catch (UnsupportedEncodingException ex) {

        }
        boolean _giftCheck = false;
        if (codeMessage.contains("잔액"))
        {
            _giftCheck = true;
            sendData.put("GiftAmt", codeMessage); //기프트카드 잔액
//            codeMessage.replace("잔액","");
//            codeMessage.replace(":","");
//            codeMessage.replace(" ","");
//            codeMessage.replace("원","");
        }
        if(_EotCancel==0)
        {
            mEotCancel=0;
            if((ic_appro_number.replace(" ","")).equals(""))
            {
                mPaymentListener.result("거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());
                Clear();
                return;
            }
            if(!mDBAppToApp)
            {
                mPosSdk.setSqliteDB_InsertTradeData(
                        _Tid,
                        mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                        sqliteDbSdk.TradeMethod.Kakao,
                        _creditCode.equals(TCPCommand.CMD_KAKAOPAY_RES) ? sqliteDbSdk.TradeMethod.NoCancel:sqliteDbSdk.TradeMethod.Cancel,
                        Integer.parseInt(거래금액.equals("") ? "0":거래금액),
                        _giftCheck ? codeMessage:"0",
                        Integer.parseInt(세금.equals("") ? "0":세금),
                        Integer.parseInt(봉사료.equals("") ? "0":봉사료),
                        Integer.parseInt(비과세.equals("") ? "0":비과세),
                        Integer.parseInt(할부개월.equals("") ? "0":할부개월),
                        sqliteDbSdk.TradeMethod.NULL, sqliteDbSdk.TradeMethod.NULL,"", "",
                        card_type,
                        purchaser_name,
                        issuer_name,
                        merchant_number,
                        _date,
                        _creditCode.equals(TCPCommand.CMD_KAKAOPAY_RES) ? "":원거래일자,
                        ic_appro_number,
                        _creditCode.equals(TCPCommand.CMD_KAKAOPAY_RES) ? "":원승인번호,
                        KOCES거래고유번호, codeMessage,
                        noti_message,
                        결제수단,
                        카카오페이승인금액,
                        카카오페이할인금액,
                        카카오멤버쉼바코드,
                        카카오멤버쉼번호,
                        카드번호정보,
                        PEM,
                        trid,
                        print_card_num,
                        조회고유번호,
                        출력용바코드번호,
                        전표출력여부,
                        전표구분명,
                        "",
                        "");
            }
            mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
        }
        else
        {
            mEotCancel=0;
            mPaymentListener.result("망취소 발생, 거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
        }
        //사용자 카드관련 정보 초기화_________________________________________________________________________
        Clear();
        //__________________________________________________________________________________________________
    }

    /**
     * 서버로 부터 신용거래를 받아서 eot 수신을 체크한다
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _creditCode
     */
    private void Res_TCP_Credit(List<byte[]> _res, String _date, String _TerminalID, String _creditCode)
    {
        if(_res==null)
        {
            ((BaseActivity)mCtx).ReadyDialogHide();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }
        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        int TaxConvert = 0 ;
        if(this.세금 != null && !this.세금.equals("")) {
            TaxConvert = Integer.parseInt(this.세금);
        }
        int SrvCharge = 0;
        if(this.봉사료 != null && !this.봉사료.equals("")) {
            SrvCharge = Integer.parseInt(this.봉사료);
        }
        int TxfCharge = 0;
        if(this.비과세 != null && !this.비과세.equals("")) {
            TxfCharge = Integer.parseInt(this.비과세);
        }
        int TotalMoney = Integer.parseInt(this.거래금액) + TaxConvert + SrvCharge + TxfCharge;

        String convertMoney = "";
        String convertTax = "";
        String convertSvc = "";
        String convertTaf = "";

        convertTax = "0";
        convertSvc = "0";
        convertTaf = "0";

        convertMoney = StringUtil.leftPad(String.valueOf(TotalMoney),  "0",  12);

        if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            int _tmpEot = 0;
            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
            {
                if (Setting.getEOTResult() != 0) {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult()); break;
                }
                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                _tmpEot ++;
                if(_tmpEot >= 30)
                {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult());
                    break;
                }
            }

            if (Setting.getEOTResult() == 1 && _creditCode.equals("A15"))   //eot정상 정상응답
            {
                Res_TCP_Credit_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
            else if (Setting.getEOTResult() == -1  && _creditCode.equals("A15")) {  //망취소한다
                String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //신용승인번호
                mICCancelInfo = "I" + _date + ic_appro_number;
                byte[] _tmpCancel = mICCancelInfo.getBytes();
                _tmpCancel[0] = (byte) 0x49;
                mICCancelInfo = new String(_tmpCancel);
                mEotCancel=1;
                전문번호 = TCPCommand.CMD_ICTRADE_CANCEL_REQ;
//                Req_tcp_Credit(mTid, null, "", mICCancelInfo,"");
                if (입력방법.equals("U"))
                {
                    mPosSdk.___ictrade(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, mICCancelInfo, "U", "", "",
                            new byte[]{}, convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, Utils.AppTmlcNo(),
                            "", "", "", "", 바코드번호.getBytes(StandardCharsets.UTF_8),
                            _WorkingKeyIndex, "", "", "", "", "", "",
                            "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", new byte[]{}, "",
                            가맹점데이터, "", "",
                            Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                } else {
                    mPosSdk.___ictrade(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, mICCancelInfo, "K", 바코드번호 + "=8911", "",
                            new byte[]{}, convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, Utils.AppTmlcNo(),
                            "", "", "", "", 바코드번호.substring(0,6).getBytes(StandardCharsets.UTF_8),
                            _WorkingKeyIndex, "", "", "", "", "", "",
                            "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", new byte[]{}, "",
                            가맹점데이터, "", "",
                            Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                }



                mICCancelInfo = "";
                Setting.setEOTResult(0);
            }
            else if(Setting.getEOTResult() == 0 && _creditCode.equals("A15"))   //승인일때 들어와서 시도하며 취소일때는 하지 않는다. 망취소한다
            {
                mEotCancel=1;
                mICCancelInfo = "I" + _date.substring(0,6) + Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2));
                전문번호 = TCPCommand.CMD_ICTRADE_CANCEL_REQ;
//                Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
                if (입력방법.equals("U"))
                {
                    mPosSdk.___ictrade(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, mICCancelInfo, "U", "", "",
                            new byte[]{}, convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, Utils.AppTmlcNo(),
                            "", "", "", "", 바코드번호.getBytes(StandardCharsets.UTF_8),
                            _WorkingKeyIndex, "", "", "", "", "", "",
                            "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", new byte[]{}, "",
                            가맹점데이터, "", "",
                            Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                } else {
                    mPosSdk.___ictrade(전문번호, _Tid, Utils.getDate("yyMMddHHmmss"), 단말버전, 단말추가정보, mICCancelInfo, "K", 바코드번호 + "=8911", "",
                            new byte[]{}, convertMoney, convertTax, convertSvc, convertTaf, 통화코드, 할부개월, Utils.AppTmlcNo(),
                            "", "", "", "", 바코드번호.substring(0,6).getBytes(StandardCharsets.UTF_8),
                            _WorkingKeyIndex, "", "", "", "", "", "",
                            "", "", new byte[]{}, 전자서명사용여부, 사인패드시리얼번호, "", new byte[]{}, "",
                            가맹점데이터, "", "",
                            Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mDBAppToApp, _Tid), mTcpDatalistener);
                }
            }
            else if(_creditCode.equals("A25"))  //정상취소
            {
                Res_TCP_Credit_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
        }
        else    //기타오류코드로인한 종료
        {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            Res_TCP_Credit_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
        }

    }

    /**
     * eot수신 체크 후에 서버로부터 받은 신용거래 결과
     * @param data
     * @param _date
     * @param _TerminalID
     * @param _EotCancel
     * @param code
     * @param ic_result_message
     * @param _creditCode
     */
    private void Res_TCP_Credit_Sucess(List<byte[]> data,String _date,String _TerminalID,int _EotCancel,String code,String ic_result_message,String _creditCode)
    {
        //String ic_result_message = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(1)); //IC_응답메세지
        String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //신용승인번호
        String koces_tran_unique_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //koces거래고유번호
        String tmpCardNum1 = new String(data.get(4));
        String[] tmpCardNum2 = tmpCardNum1.split("-");
        tmpCardNum1 = "";
        for(String n:tmpCardNum2)
        {
            tmpCardNum1 += n;
        }

        byte[] _CardNum = tmpCardNum1.getBytes();

        if(_CardNum != null){
            //System.arraycopy( data.get(4),0,_CardNum,0,6);
            for(int i=8; i<_CardNum.length; i++){
                //* = 0x2A
                _CardNum[i] = (byte)0x2A;
            }
            //카드번호가 올라오지만 그중 6개만 사용하고 나머지는 *로 바꾼다
        }

        String print_card_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //출력용카드번호

        //카드 정보 삭제____________________________________________________________________________________
        Random rand = new Random();
        for(int i=0; i<_CardNum.length; i++){
            //* = 0x2A
            _CardNum[i] = (byte)rand.nextInt(255);;
        }
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x01);
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x00);
        data.set(4,_CardNum);

        String card_type = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(6)); //카드종류
        String issuer_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(7)); //발급사코드
        String issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(8),"EUC-KR"); //발급사명
        String purchaser_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)); //매입사코드
        String purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(10),"EUC-KR"); //매입사명
        String ddc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(11)); //DDC 여부
        String edc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(12)); //EDC 여부

        String giftcard_money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(15)); //기프트카드 잔액
        String merchant_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(16)); //가맹점번호
        String encryp_key_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(25)); //암호키만료잔여일
        String merchant_data = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(26)); //가맹점데이터

        String trd_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(27)); //거래금액
        String tax_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(28)); //세금
        String svc_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(29)); //봉사료
        String tax_free_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(30)); //비과세

        KOCES거래고유번호 = koces_tran_unique_num;
        sendData = new HashMap<>();
        if(mDBAppToApp)
        {
            sendData.put("TrdType",_creditCode.equals("A15") ? "E15":"E25");
        }
        else
        {
            sendData.put("TrdType",_creditCode);
        }

        sendData.put("TermID",_TerminalID);
        sendData.put("TrdDate",_date);
        sendData.put("AnsCode", code);
        sendData.put("Message", ic_result_message);
        sendData.put("AuNo",ic_appro_number);
        sendData.put("TradeNo", KOCES거래고유번호);
        if(!print_card_num.equals(""))
        {
            print_card_num = print_card_num.substring(0,8);
        }
        sendData.put("CardNo", print_card_num);
//        sendData.put("Keydate", encryp_key_expiration_date);
        sendData.put("MchData", merchant_data);
        sendData.put("CardKind", card_type); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
        sendData.put("OrdCd", issuer_code);   //발급사코드
        sendData.put("OrdNm", issuer_name);   //발급사명
        sendData.put("InpNm", purchaser_name);   //매입사명
        sendData.put("InpCd", purchaser_code);   //매입사코드
        sendData.put("DDCYn", ddc_status);   //DDC 여부
        sendData.put("EDCYn", edc_status);   //EDC 여부
//        sendData.put("GiftAmt", giftcard_money); //기프트카드 잔액
        sendData.put("MchNo", merchant_number);   //가맹점번호

        sendData.put("DisAmt", "");   //카카오할임금액
        sendData.put("AuthType", "");   //카카오페이 결제수단 C:Card, M:Money

        sendData.put("AnswerTrdNo", "");   //위쳇페이거래고유번호
        sendData.put("ChargeAmt", "");   //가맹점수수료
        sendData.put("RefundAmt", "");   //가맹점환불금액

        sendData.put("QrKind", "AP");   //간편결제 거래종류

        KOCES거래고유번호 = "";
        //       mARD = 0x00; mIAD = null; mIS = null;
        String codeMessage = "";
        try {
            codeMessage = Utils.getByteToString_euc_kr(data.get(1));
            sendData.put("Message",codeMessage);
        } catch (UnsupportedEncodingException ex) {

        }
        boolean _giftCheck = false;
        if (codeMessage.contains("잔액"))
        {
            _giftCheck = true;
//            codeMessage.replace("잔액","");
//            codeMessage.replace(":","");
//            codeMessage.replace(" ","");
//            codeMessage.replace("원","");
        }
        if(_EotCancel==0)
        {
            mEotCancel=0;
            if((ic_appro_number.replace(" ","")).equals(""))
            {
                mPaymentListener.result("거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());
                Clear();
                return;
            }
            if (!mDBAppToApp)
            {
                if(Scan_Data_Parser(바코드번호).equals(Constants.EasyPayMethod.App_Card.toString()))
                {
                    mPosSdk.setSqliteDB_InsertTradeData(
                            _Tid,
                            mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                            sqliteDbSdk.TradeMethod.AppCard,
                            _creditCode.equals(TCPCommand.CMD_IC_OK_RES) ? sqliteDbSdk.TradeMethod.NoCancel:sqliteDbSdk.TradeMethod.Cancel,
                            Integer.parseInt(거래금액.equals("") ? "0":거래금액),
                            _giftCheck ? codeMessage:"0",
                            Integer.parseInt(세금.equals("") ? "0":세금),
                            Integer.parseInt(봉사료.equals("") ? "0":봉사료),
                            Integer.parseInt(비과세.equals("") ? "0":비과세),
                            Integer.parseInt(할부개월.equals("") ? "0":할부개월),
                            sqliteDbSdk.TradeMethod.NULL,
                            sqliteDbSdk.TradeMethod.NULL,
                            "",
                            print_card_num,
                            card_type,
                            purchaser_name.replace(" ", ""),
                            issuer_name.replace(" ", ""),
                            merchant_number.replace(" ", ""),
                            _date.replace(" ", ""),
                            _creditCode.equals(TCPCommand.CMD_IC_OK_RES) ? "":원거래일자,
                            ic_appro_number.replace(" ", ""),
                            _creditCode.equals(TCPCommand.CMD_IC_OK_RES) ? "":원승인번호,
                            KOCES거래고유번호, ic_result_message,
                            "", "", "", "", "", "", "", "",
                            "", print_card_num, "", "", "", "", "", "");
                }
                else
                {
                    mPosSdk.setSqliteDB_InsertTradeData(
                            _Tid,
                            mStoreName,mStoreAddr,mStoreNumber,mStorePhone,mStoreOwner,
                            sqliteDbSdk.TradeMethod.EmvQr,
                            _creditCode.equals(TCPCommand.CMD_IC_OK_RES) ? sqliteDbSdk.TradeMethod.NoCancel:sqliteDbSdk.TradeMethod.Cancel,
                            Integer.parseInt(거래금액.equals("") ? "0":거래금액),
                            ic_result_message,
                            Integer.parseInt(세금.equals("") ? "0":세금),
                            Integer.parseInt(봉사료.equals("") ? "0":봉사료),
                            Integer.parseInt(비과세.equals("") ? "0":비과세),
                            Integer.parseInt(할부개월.equals("") ? "0":할부개월),
                            sqliteDbSdk.TradeMethod.NULL,
                            sqliteDbSdk.TradeMethod.NULL,
                            "",
                            print_card_num,
                            card_type,
                            purchaser_name.replace(" ", ""),
                            issuer_name.replace(" ", ""),
                            merchant_number.replace(" ", ""),
                            _date.replace(" ", ""),
                            _creditCode.equals(TCPCommand.CMD_IC_OK_RES) ? "":원거래일자,
                            ic_appro_number.replace(" ", ""),
                            _creditCode.equals(TCPCommand.CMD_IC_OK_RES) ? "":원승인번호,
                            KOCES거래고유번호, ic_result_message,
                            "", "", "", "", "", "", "", "",
                            "", print_card_num, "", "", "", "", "", "");

                }

            }
            mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
        }
        else
        {
            mEotCancel=0;
            mPaymentListener.result("망취소 발생, 거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
        }
        //사용자 카드관련 정보 초기화_________________________________________________________________________
        Clear();
        //__________________________________________________________________________________________________
    }

    //사인패드실행하는 함수
    public void ReadySignPad(Context _ctx, int _money)
    {

        if(!mDBAppToApp)
        {
            if (_ctx instanceof CreditLoadingActivity)
            {
                int _count = ((CreditLoadingActivity) _ctx).getMCount();
                ((CreditLoadingActivity) _ctx).setMCount( Integer.valueOf(Setting.getPreference(mCtx,Constants.USB_TIME_OUT).equals("") ? "30":Setting.getPreference(mCtx,Constants.USB_TIME_OUT)));
            }
            else if (_ctx instanceof CashLoadingActivity)
            {
                int _count = ((CashLoadingActivity) _ctx).getMCount();
                ((CashLoadingActivity) _ctx).setMCount( Integer.valueOf(Setting.getPreference(mCtx,Constants.USB_TIME_OUT).equals("") ? "30":Setting.getPreference(mCtx,Constants.USB_TIME_OUT)));
            }
            else if (_ctx instanceof EasyLoadingActivity)
            {
                int _count = ((EasyLoadingActivity) _ctx).getMCount();
                ((EasyLoadingActivity) _ctx).setMCount( Integer.valueOf(Setting.getPreference(mCtx,Constants.USB_TIME_OUT).equals("") ? "30":Setting.getPreference(mCtx,Constants.USB_TIME_OUT)));
            }
        }
        Intent intent = new Intent((Activity)_ctx, SignPadActivity.class);
        intent.putExtra("Money",_money);
        Activity Ac = (Activity)_ctx;
        Ac.startActivityForResult(intent,REQUEST_SIGNPAD);
    }

    /**
     결과를 해당 뷰컨트롤러로 보낸다
     - Parameters:
     - _status: _status description
     - _resData: <#_resData description#>
     */
//    private void Res_Tcp_KakaoPay(tcpStatus _status:tcpStatus, HashMap<String,String> _resData) {
//        /**
//         내부 완료부 완성해야함
//         */
//        String convertMoney = "";
//        String convertTax = "";
//        String convertSvc = "";
//        String convertTaf = "";
//        if(KakaoPaySdk.getInstance().세금.isEmpty || KakaoPaySdk.getInstance().세금.equal(" ") || KakaoPaySdk.getInstance().세금.equal("0")) { KakaoPaySdk.getInstance().세금 = "" }
//        else
//        {
//            convertTax = Utils.leftPad(str: KakaoPaySdk.getInstance().세금.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//        if KakaoPaySdk.getInstance().봉사료.isEmpty || KakaoPaySdk.getInstance().봉사료 == " " || KakaoPaySdk.getInstance().봉사료 == "0" { KakaoPaySdk.getInstance().봉사료 = "" }
//        else
//        {
//            convertSvc = Utils.leftPad(str: KakaoPaySdk.getInstance().봉사료.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//        if KakaoPaySdk.getInstance().비과세.isEmpty || KakaoPaySdk.getInstance().비과세 == " " || KakaoPaySdk.getInstance().비과세 == "0" { KakaoPaySdk.getInstance().비과세 = "" }
//        else
//        {
//            convertTaf = Utils.leftPad(str: KakaoPaySdk.getInstance().비과세.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//
//        convertMoney = Utils.leftPad(str: KakaoPaySdk.getInstance().거래금액.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        debugPrint(_status)
//        debugPrint(_resData)
//        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [self] in
//            switch _resData["TrdType"] {
//                case Command.CMD_KAKAOPAY_RES:
//                    if _status == .sucess {
//                    if KocesSdk.instance.mEotCheck != 0 {
//                        String 취소정보 = "I" + String(_resData["date"]?.prefix(6) ?? "") + _resData["AuNo"]!
//                                KakaoPaySdk.getInstance().전문번호 = Command.CMD_KAKAOPAY_CANCEL_REQ
//                        int TotalMoney = Integer.parseInt(convertMoney) + Integer.parseInt(convertTax) + Integer.parseInt(convertSvc) + Integer.parseInt(convertTaf) //2021-08-19 kim.jy 취소는 총합으로
//                        convertTax = "0";
//                        convertSvc = "0";
//                        convertTaf = "0";
//
//                        KakaoPaySdk.getInstance().m2TradeCancel = true    //망취소발생
//
//                        KocesSdk.instance.KakaoPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  취소정보,  KakaoPaySdk.getInstance().입력방법, BarCode: KakaoPaySdk.getInstance().바코드번호, OTCCardCode: [UInt8](), Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, PayType: "", CancelMethod: "0", CancelType: "B", StoreCode: KakaoPaySdk.getInstance().점포코드, PEM: "", trid: "", CardBIN: "", SearchNumber: KakaoPaySdk.getInstance().조회고유번호, WorkingKeyIndex: KakaoPaySdk.getInstance()._WorkingKeyIndex, SignUse: KakaoPaySdk.getInstance().전자서명사용여부, SignPadSerial: KakaoPaySdk.getInstance().사인패드시리얼번호, SignData: KakaoPaySdk.getInstance().전자서명데이터,  KakaoPaySdk.getInstance().가맹점데이터)
//                        return
//                    }
//
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if (!mDBAppToApp) {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                신용현금: define.TradeMethod.Kakao,
//                                취소여부: define.TradeMethod.NoCancel,
//                                금액: Int(_resData["Money"] ?? "0")!,
//                                선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                세금: Int(_resData["Tax"] ?? "0")!,
//                                봉사료: Int(_resData["ServiceCharge"] ?? "0")!,
//                                비과세: Int(_resData["TaxFree"] ?? "0")!,
//                                할부: Int(_resData["Installment"] ?? "0" )!,
//                                현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "", 카드번호: "",
//                                카드종류: _resData["CardKind"] ?? "",
//                                카드매입사: _resData["InpNm"] ?? "",
//                                카드발급사: _resData["OrdNm"] ?? "",
//                                가맹점번호: _resData["MchNo"] ?? "",
//                                승인날짜: _resData["TrdDate"] ?? "",
//                                원거래일자: _resData["OriDate"] ?? "",
//                                승인번호: _resData["AuNo"] ?? "", 원승인번호: "",
//                                코세스고유거래키: _resData["TradeNo"] ?? "", 응답메시지: _resData["Message"] ?? "",
//                                KakaoMessage: _resData["KakaoMessage"] ?? "",
//                                PayType: _resData["PayType"] ?? "",
//                                KakaoAuMoney: _resData["KakaoAuMoney"] ?? "",
//                                KakaoSaleMoney: _resData["KakaoSaleMoney"] ?? "",
//                                KakaoMemberCd: _resData["KakaoMemberCd"] ?? "",
//                                KakaoMemberNo: _resData["KakaoMemberNo"] ?? "",
//                                Otc: _resData["Otc"] ?? "",
//                                Pem: _resData["Pem"] ?? "",
//                                Trid: _resData["Trid"] ?? "",
//                                CardBin: _resData["CardBin"] ?? "",
//                                SearchNo: _resData["SearchNo"] ?? "",
//                                PrintBarcd: _resData["PrintBarcd"] ?? "",
//                                PrintUse: _resData["PrintUse"] ?? "",
//                                PrintNm: _resData["PrintNm"] ?? "", MchFee: "", MchRefund: "")
//
//                        if String(describing: Utils.topMostViewController()).contains("CardAnimationViewController") {
//                            let controller = Utils.topMostViewController() as! CardAnimationViewController
//                            controller.GoToReceiptEasyPaySwiftUI()
//
//                        }
//
//                    } else {
//                        //앱투앱인경우 리스너로 날려보냄
//                        KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_KAKAOPAY_CANCEL_RES:
//
//                    if KakaoPaySdk.getInstance().m2TradeCancel == true { //망취소발생
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "망취소 발생. 거래실패 : " + (_resData["Message"] ?? "")
//                    resDataDic["TrdType"] = Command.CMD_KAKAOPAY_CANCEL_RES
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    Clear()
//                    return
//                }
//
//                if _status == .sucess {
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if (!mDBAppToApp) {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                define.TradeMethod.Kakao,
//                                define.TradeMethod.Cancel,
//                                Integer.parseInt(_resData["Money"] ?? "0")),
//                                선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                세금: Int(_resData["Tax"] ?? "0")!,
//                                봉사료: Int(_resData["ServiceCharge"] ?? "0")!,
//                                비과세: Int(_resData["TaxFree"] ?? "0")!,
//                                할부: Int(_resData["Installment"] ?? "0" )!,
//                                현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "", 카드번호: "",
//                                카드종류: _resData["CardKind"]!,
//                                카드매입사: _resData["InpNm"]!,
//                                카드발급사: _resData["OrdNm"]!,
//                                가맹점번호: _resData["MchNo"]!,
//                                승인날짜: _resData["TrdDate"]!,
//                                원거래일자: KakaoPaySdk.getInstance().원거래일자,
//                                승인번호: _resData["AuNo"]!,
//                                원승인번호: KakaoPaySdk.getInstance().원승인번호,
//                                코세스고유거래키: _resData["TradeNo"] ?? "", 응답메시지: _resData["Message"] ?? "",
//                                KakaoMessage: _resData["KakaoMessage"] ?? "",
//                                PayType: _resData["PayType"] ?? "",
//                                KakaoAuMoney: _resData["KakaoAuMoney"] ?? "",
//                                KakaoSaleMoney: _resData["KakaoSaleMoney"] ?? "",
//                                KakaoMemberCd: _resData["KakaoMemberCd"] ?? "",
//                                KakaoMemberNo: _resData["KakaoMemberNo"] ?? "",
//                                Otc: _resData["Otc"] ?? "",
//                                Pem: _resData["Pem"] ?? "",
//                                Trid: _resData["Trid"] ?? "",
//                                CardBin: _resData["CardBin"] ?? "",
//                                SearchNo: _resData["SearchNo"] ?? "",
//                                PrintBarcd: _resData["PrintBarcd"] ?? "",
//                                PrintUse: _resData["PrintUse"] ?? "",
//                                PrintNm: _resData["PrintNm"] ?? "", MchFee: "", MchRefund: "")
//
//                        if String(describing: Utils.topMostViewController()).contains("CardAnimationViewController") {
//                            let controller = Utils.topMostViewController() as! CardAnimationViewController
//                            controller.GoToReceiptEasyPaySwiftUI()
//
//                        }
//
//                    } else {
//                        //앱투앱인경우 리스너로 날려보냄
//                        KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_KAKAOPAY_SEARCH_RES:
//                    //조회가 여기까지 올 이유가 없다.
//                    if _status == .sucess {
//
//                    var _totalMoney = 0
//                    var Money = KakaoPaySdk.getInstance().거래금액
//                    var Tax = KakaoPaySdk.getInstance().세금
//                    var ServiceCharge = KakaoPaySdk.getInstance().봉사료
//                    var TaxFree = KakaoPaySdk.getInstance().비과세
//                    if !(_resData["KakaoSaleMoney"] ?? "").isEmpty {
//                        _totalMoney = Int(Money)! + (Tax.isEmpty ? 0:Int(Tax)!) + (ServiceCharge.isEmpty ? 0:Int(ServiceCharge)!) - Int(_resData["KakaoSaleMoney"] ?? "0")!
//                                let tax:[String:Int]  = mTaxCalc.TaxCalc(금액: _totalMoney,비과세금액: (TaxFree.isEmpty ? 0:Int(TaxFree)!), 봉사료: (ServiceCharge.isEmpty ? 0:Int(ServiceCharge)!))
//                        Money = Utils.leftPad(str: String(tax["Money"]!), fillChar: "0", length: 10)
//                        Tax = String(tax["VAT"]!)
//                        ServiceCharge = String(tax["SVC"]!)
//                        TaxFree = String(tax["TXF"]!)
//                    } else {
//                        _totalMoney = Int(Money)! + (Tax.isEmpty ? 0:Int(Tax)!) + (ServiceCharge.isEmpty ? 0:Int(ServiceCharge)!)
//                    }
//
//                    var compareMoney = 0
//                    if String(describing: KakaoPaySdk.getInstance().paylistener).contains("AppToApp") {
//                        compareMoney = 50000    //앱투앱이면 5만원이상 일때 사인패드
//                    } else  {
//                        //앺투앱이 아니라면 세금설정을 체크.
//                        if Setting.shared.getDefaultUserData(_key: define.UNSIGNED_SETMONEY) == "" {
//                            compareMoney = 50000
//                        } else {
//                            compareMoney = Int(Setting.shared.getDefaultUserData(_key: define.UNSIGNED_SETMONEY)) ?? 50000
//                        }
//                    }
//
//                    KakaoPaySdk.getInstance().전문번호 = Command.CMD_KAKAOPAY_REQ
//                    KakaoPaySdk.getInstance().OTC카드번호 = Command.StrToArr(String(_resData["Otc"] ?? ""))
//                    KakaoPaySdk.getInstance().거래금액 = Money
//                    KakaoPaySdk.getInstance().세금 = Tax
//                    KakaoPaySdk.getInstance().봉사료 = ServiceCharge
//                    KakaoPaySdk.getInstance().비과세 = TaxFree
//                    KakaoPaySdk.getInstance().결제수단 = String(_resData["PayType"]!)
//                    KakaoPaySdk.getInstance()._PEM = String(_resData["Pem"]!)
//                    KakaoPaySdk.getInstance()._trid = String(_resData["Trid"]!)
//                    KakaoPaySdk.getInstance().카드BIN = String(_resData["CardBin"]!)
//                    KakaoPaySdk.getInstance().조회고유번호 = String(_resData["SearchNo"]!)
//                    if _totalMoney > compareMoney {
//                        if KakaoPaySdk.getInstance().전자서명사용여부 == "0" || KakaoPaySdk.getInstance().전자서명사용여부 == "4" {
//                            KakaoPaySdk.getInstance().전자서명사용여부 = "4"
//                            Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KakaoPaySdk.getInstance().paylistener as! PayResultDelegate)
//                            DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                                KocesSdk.instance.KakaoPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  "",  KakaoPaySdk.getInstance().입력방법, BarCode: KakaoPaySdk.getInstance().바코드번호, OTCCardCode: Command.StrToArr(String(_resData["Otc"] ?? "")), Money: Money, Tax: Tax, ServiceCharge: ServiceCharge, TaxFree: TaxFree, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, PayType: String(_resData["PayType"]!), CancelMethod: "0", CancelType: "B", StoreCode: KakaoPaySdk.getInstance().점포코드, PEM: String(_resData["Pem"]!), trid: String(_resData["Trid"]!), CardBIN: String(_resData["CardBin"]!), SearchNumber: String(_resData["SearchNo"]!), WorkingKeyIndex: KakaoPaySdk.getInstance()._WorkingKeyIndex, SignUse: KakaoPaySdk.getInstance().전자서명사용여부, SignPadSerial: KakaoPaySdk.getInstance().사인패드시리얼번호, SignData: KakaoPaySdk.getInstance().전자서명데이터,  KakaoPaySdk.getInstance().가맹점데이터)
//                            }
//                            return
//                        } else  {
//                            KakaoPaySdk.getInstance().전자서명사용여부 = "B"
//
//                            if String(_resData["PayType"]!) == "C" {
//                                var storyboard:UIStoryboard?
//                                if UIDevice.current.userInterfaceIdiom == .phone {
//                                    storyboard = UIStoryboard(name: "Main", bundle: Bundle.main)
//                                } else {
//                                    storyboard = UIStoryboard(name: "pad", bundle: Bundle.main)
//                                }
//                                guard let signPad = storyboard!.instantiateViewController(withIdentifier: "SignatureController") as? SignatureController else {return}
//                                signPad.sdk = "KaKaoPaySdk"
//                                signPad.view.backgroundColor = .white
//                                signPad.modalPresentationStyle = .fullScreen
//                                Utils.CardAnimationViewControllerClear()
//                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5){ [self] in
//                                    Utils.topMostViewController()?.present(signPad, animated: true, completion: nil)
//                                }
//                            } else {
//                                Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KakaoPaySdk.getInstance().paylistener as! PayResultDelegate)
//                                DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                                    KocesSdk.instance.KakaoPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  "",  KakaoPaySdk.getInstance().입력방법, BarCode: KakaoPaySdk.getInstance().바코드번호, OTCCardCode: Command.StrToArr(String(_resData["Otc"] ?? "")), Money: Money, Tax: Tax, ServiceCharge: ServiceCharge, TaxFree: TaxFree, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, PayType: String(_resData["PayType"]!), CancelMethod: "0", CancelType: "B", StoreCode: KakaoPaySdk.getInstance().점포코드, PEM: String(_resData["Pem"]!), trid: String(_resData["Trid"]!), CardBIN: String(_resData["CardBin"]!), SearchNumber: String(_resData["SearchNo"]!), WorkingKeyIndex: KakaoPaySdk.getInstance()._WorkingKeyIndex, SignUse: KakaoPaySdk.getInstance().전자서명사용여부, SignPadSerial: KakaoPaySdk.getInstance().사인패드시리얼번호, SignData: KakaoPaySdk.getInstance().전자서명데이터,  KakaoPaySdk.getInstance().가맹점데이터)
//                                }
//
//                            }
//                            return
//
//                        }
//
//                    } else {
//                        KakaoPaySdk.getInstance().전자서명사용여부 = "5"
//                        Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KakaoPaySdk.getInstance().paylistener as! PayResultDelegate)
//                        DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                            KocesSdk.instance.KakaoPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  "",  KakaoPaySdk.getInstance().입력방법, BarCode: KakaoPaySdk.getInstance().바코드번호, OTCCardCode: Command.StrToArr(String(_resData["Otc"] ?? "")), Money: Money, Tax: Tax, ServiceCharge: ServiceCharge, TaxFree: TaxFree, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, PayType: String(_resData["PayType"]!), CancelMethod: "0", CancelType: "B", StoreCode: KakaoPaySdk.getInstance().점포코드, PEM: String(_resData["Pem"]!), trid: String(_resData["Trid"]!), CardBIN: String(_resData["CardBin"]!), SearchNumber: String(_resData["SearchNo"]!), WorkingKeyIndex: KakaoPaySdk.getInstance()._WorkingKeyIndex, SignUse: KakaoPaySdk.getInstance().전자서명사용여부, SignPadSerial: KakaoPaySdk.getInstance().사인패드시리얼번호, SignData: KakaoPaySdk.getInstance().전자서명데이터,  KakaoPaySdk.getInstance().가맹점데이터)
//                        }
//                        return
//                    }
//
//
//
//
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
////                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//
//                case Command.CMD_IC_OK_RES:
//                    if _status == .sucess {
//                    if KocesSdk.instance.mEotCheck != 0 {
//                        let 취소정보 = "I" + String(_resData["date"]?.prefix(6) ?? "") + _resData["AuNo"]!
//                                KakaoPaySdk.getInstance().전문번호 = Command.CMD_ICTRADE_CANCEL_REQ
//                        var TotalMoney:Int = Int(convertMoney)! + Int(convertTax)! + Int(convertSvc)! + Int(convertTaf)! //2021-08-19 kim.jy 취소는 총합으로
//                                convertTax = "0"
//                        convertSvc = "0"
//                        convertTaf = "0"
//
//                        KakaoPaySdk.getInstance().m2TradeCancel = true    //망취소발생
//
//                        if KakaoPaySdk.getInstance().입력방법 == "U" {
//                            KocesSdk.instance.Credit( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보, ResonCancel: 취소정보,  "U", CardNumber: "", EncryptInfo: [UInt8](), Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KakaoPaySdk.getInstance().통화코드, InstallMent: KakaoPaySdk.getInstance().할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: Array(KakaoPaySdk.getInstance().바코드번호.utf8), WorkingKeyIndex: KakaoPaySdk.getInstance()._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KakaoPaySdk.getInstance().전자서명사용여부, SignPadSerial: KakaoPaySdk.getInstance().사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KakaoPaySdk.getInstance().가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KakaoPaySdk.getInstance().mDBAppToApp == true ? .AppToApp:.KocesICIOSPay,  KakaoPaySdk.getInstance()._Tid))
//                        } else {
//                            KocesSdk.instance.Credit( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보, ResonCancel: 취소정보,  "K", CardNumber: KakaoPaySdk.getInstance().바코드번호 + "=8911", EncryptInfo: [UInt8](), Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KakaoPaySdk.getInstance().통화코드, InstallMent: KakaoPaySdk.getInstance().할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: [UInt8](), WorkingKeyIndex: KakaoPaySdk.getInstance()._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KakaoPaySdk.getInstance().전자서명사용여부, SignPadSerial: KakaoPaySdk.getInstance().사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KakaoPaySdk.getInstance().가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KakaoPaySdk.getInstance().mDBAppToApp == true ? .AppToApp:.KocesICIOSPay,  KakaoPaySdk.getInstance()._Tid))
//                        }
//                        return
//                    }
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if (!mDBAppToApp) {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//
//                        /**
//                         이쪽 DB 업데이트 하는 부분 정리 필요. 어떤 내용이 있는지 체크필요. 앱카드 및 EMV QR 관련 받은데이터를 어떻게 정리하는 지 필요함
//                         그리고 해당 데이터를 영수증화면에 어떻게 출력하는지도 필요
//                         */
//
//                        if Scan_Data_Parser(Scan: KakaoPaySdk.getInstance().바코드번호) == define.EasyPayMethod.App_Card.rawValue {
//                            sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                    신용현금: define.TradeMethod.AppCard,
//                                    취소여부: define.TradeMethod.NoCancel,
//                                    금액: Int(KakaoPaySdk.getInstance().거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KakaoPaySdk.getInstance().세금) ?? 0,
//                                    봉사료: Int(KakaoPaySdk.getInstance().봉사료) ?? 0,
//                                    비과세: Int(KakaoPaySdk.getInstance().비과세) ?? 0,
//                                    할부: Int(KakaoPaySdk.getInstance().할부개월) ?? 0,
//                                    현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "",
//                                    카드번호: _resData["CardNo"] ?? "",
//                                    카드종류: _resData["CardKind"]!,
//                                    카드매입사: _resData["InpNm"]!,
//                                    카드발급사: _resData["OrdNm"]!,
//                                    가맹점번호: _resData["MchNo"]!,
//                                    승인날짜: _resData["TrdDate"]!,
//                                    원거래일자: _resData["OriDate"] ?? "",
//                                    승인번호: _resData["AuNo"]!, 원승인번호: "",
//                                    코세스고유거래키: _resData["TradeNo"]!, 응답메시지: _resData["Message"] ?? "", KakaoMessage: "", PayType: "", KakaoAuMoney: "", KakaoSaleMoney: "", KakaoMemberCd: "", KakaoMemberNo: "", Otc: "", Pem: "", Trid: "", CardBin: "", SearchNo: "", PrintBarcd: "", PrintUse: "", PrintNm: "", MchFee: "", MchRefund: "")
//                        } else {
//                            sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                    신용현금: define.TradeMethod.EmvQr,
//                                    취소여부: define.TradeMethod.NoCancel,
//                                    금액: Int(KakaoPaySdk.getInstance().거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KakaoPaySdk.getInstance().세금) ?? 0,
//                                    봉사료: Int(KakaoPaySdk.getInstance().봉사료) ?? 0,
//                                    비과세: Int(KakaoPaySdk.getInstance().비과세) ?? 0,
//                                    할부: Int(KakaoPaySdk.getInstance().할부개월) ?? 0,
//                                    현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "",
//                                    카드번호: _resData["CardNo"] ?? "",
//                                    카드종류: _resData["CardKind"]!,
//                                    카드매입사: _resData["InpNm"]!,
//                                    카드발급사: _resData["OrdNm"]!,
//                                    가맹점번호: _resData["MchNo"]!,
//                                    승인날짜: _resData["TrdDate"]!,
//                                    원거래일자: _resData["OriDate"] ?? "",
//                                    승인번호: _resData["AuNo"]!, 원승인번호: "",
//                                    코세스고유거래키: _resData["TradeNo"]!, 응답메시지: _resData["Message"] ?? "", KakaoMessage: "", PayType: "", KakaoAuMoney: "", KakaoSaleMoney: "", KakaoMemberCd: "", KakaoMemberNo: "", Otc: "", Pem: "", Trid: "", CardBin: "", SearchNo: "", PrintBarcd: "", PrintUse: "", PrintNm: "", MchFee: "", MchRefund: "")
//                        }
//
//
//                        if String(describing: Utils.topMostViewController()).contains("CardAnimationViewController") {
//                            let controller = Utils.topMostViewController() as! CardAnimationViewController
//                            controller.GoToReceiptEasyPaySwiftUI()
//
//                        }
//                    } else {
//                        //앱투앱인경우 리스너로 날려보냄
//                        KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_IC_CANCEL_RES:
//
//                    if KakaoPaySdk.getInstance().m2TradeCancel == true { //망취소발생
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "망취소 발생. 거래실패 : " + (_resData["Message"] ?? "")
//                    resDataDic["TrdType"] = Command.CMD_IC_CANCEL_RES
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    Clear()
//                    return
//                }
//
//                if _status == .sucess {
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if (!mDBAppToApp) {
//
//                        /**
//                         이쪽 DB 업데이트 하는 부분 정리 필요. 어떤 내용이 있는지 체크필요. 앱카드 및 EMV QR 관련 받은데이터를 어떻게 정리하는 지 필요함
//                         그리고 해당 데이터를 영수증화면에 어떻게 출력하는지도 필요
//                         */
//
//                        if Scan_Data_Parser(Scan: KakaoPaySdk.getInstance().바코드번호) == define.EasyPayMethod.App_Card.rawValue {
//                            sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                    신용현금: define.TradeMethod.AppCard,
//                                    취소여부: define.TradeMethod.Cancel,
//                                    금액: Int(KakaoPaySdk.getInstance().거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KakaoPaySdk.getInstance().세금) ?? 0,
//                                    봉사료: Int(KakaoPaySdk.getInstance().봉사료) ?? 0,
//                                    비과세: Int(KakaoPaySdk.getInstance().비과세) ?? 0,
//                                    할부: Int(KakaoPaySdk.getInstance().할부개월) ?? 0,
//                                    현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "",
//                                    카드번호: _resData["CardNo"] ?? "",
//                                    카드종류: _resData["CardKind"]!,
//                                    카드매입사: _resData["InpNm"]!,
//                                    카드발급사: _resData["OrdNm"]!,
//                                    가맹점번호: _resData["MchNo"]!,
//                                    승인날짜: _resData["TrdDate"]!,
//                                    원거래일자: KakaoPaySdk.getInstance().원거래일자,
//                                    승인번호: _resData["AuNo"]!,
//                                    원승인번호: KakaoPaySdk.getInstance().원승인번호,
//                                    코세스고유거래키: _resData["TradeNo"]!, 응답메시지: _resData["Message"] ?? "", KakaoMessage: "", PayType: "", KakaoAuMoney: "", KakaoSaleMoney: "", KakaoMemberCd: "", KakaoMemberNo: "", Otc: "", Pem: "", Trid: "", CardBin: "", SearchNo: "", PrintBarcd: "", PrintUse: "", PrintNm: "", MchFee: "", MchRefund: "")
//                        } else {
//                            sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                    신용현금: define.TradeMethod.EmvQr,
//                                    취소여부: define.TradeMethod.Cancel,
//                                    금액: Int(KakaoPaySdk.getInstance().거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KakaoPaySdk.getInstance().세금) ?? 0,
//                                    봉사료: Int(KakaoPaySdk.getInstance().봉사료) ?? 0,
//                                    비과세: Int(KakaoPaySdk.getInstance().비과세) ?? 0,
//                                    할부: Int(KakaoPaySdk.getInstance().할부개월) ?? 0,
//                                    현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "",
//                                    카드번호: _resData["CardNo"] ?? "",
//                                    카드종류: _resData["CardKind"]!,
//                                    카드매입사: _resData["InpNm"]!,
//                                    카드발급사: _resData["OrdNm"]!,
//                                    가맹점번호: _resData["MchNo"]!,
//                                    승인날짜: _resData["TrdDate"]!,
//                                    원거래일자: KakaoPaySdk.getInstance().원거래일자,
//                                    승인번호: _resData["AuNo"]!,
//                                    원승인번호: KakaoPaySdk.getInstance().원승인번호,
//                                    코세스고유거래키: _resData["TradeNo"]!, 응답메시지: _resData["Message"] ?? "", KakaoMessage: "", PayType: "", KakaoAuMoney: "", KakaoSaleMoney: "", KakaoMemberCd: "", KakaoMemberNo: "", Otc: "", Pem: "", Trid: "", CardBin: "", SearchNo: "", PrintBarcd: "", PrintUse: "", PrintNm: "", MchFee: "", MchRefund: "")
//                        }
//
//                        if String(describing: Utils.topMostViewController()).contains("CardAnimationViewController") {
//                            let controller = Utils.topMostViewController() as! CardAnimationViewController
//                            controller.GoToReceiptEasyPaySwiftUI()
//
//                        }
//                    } else {
//                        //앱투앱인경우 리스너로 날려보냄
//                        KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_ZEROPAY_RES:
//                    if _status == .sucess {
//                    if KocesSdk.instance.mEotCheck != 0 {
////                        let 취소정보 = "1" + String(_resData["date"]?.prefix(6) ?? "") + _resData["AuNo"]!
//                        KakaoPaySdk.getInstance().전문번호 = Command.CMD_ZEROPAY_CANCEL_REQ
//                        var TotalMoney:Int = Int(convertMoney)! + Int(convertTax)! + Int(convertSvc)! + Int(convertTaf)! //2021-08-19 kim.jy 취소는 총합으로
//                                convertTax = "0"
//                        convertSvc = "0"
//                        convertTaf = "0"
//
//                        KakaoPaySdk.getInstance().m2TradeCancel = true    //망취소발생
//
//                        KocesSdk.instance.ZeroPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  "1",  KakaoPaySdk.getInstance().입력방법, Ori KakaoPaySdk.getInstance().원거래일자, OriAuNumber: KakaoPaySdk.getInstance().원승인번호, BarCode: KakaoPaySdk.getInstance().바코드번호, Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, StoreInfo: KakaoPaySdk.getInstance().가맹점추가정보,  KakaoPaySdk.getInstance().가맹점데이터, KocesUniqueNum: KakaoPaySdk.getInstance().KOCES거래고유번호)
//                        return
//                    }
//
//                    if _resData["AnsCode"]! == "0100" {
//                        KakaoPaySdk.getInstance().전문번호 = Command.CMD_ZEROPAY_SEARCH_REQ
//                        KocesSdk.instance.ZeroPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  "",  "", Ori _resData["TrdDate"] ?? "", OriAuNumber: _resData["AuNo"] ?? "", BarCode: "", Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, StoreInfo: KakaoPaySdk.getInstance().가맹점추가정보,  KakaoPaySdk.getInstance().가맹점데이터, KocesUniqueNum: KakaoPaySdk.getInstance().KOCES거래고유번호)
//                        return
//                    }
//
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if (!mDBAppToApp) {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                신용현금: define.TradeMethod.Zero,
//                                취소여부: define.TradeMethod.NoCancel,
//                                금액: Int(_resData["Money"] ?? "0")!,
//                                선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                세금: Int(_resData["Tax"] ?? "0")!,
//                                봉사료: Int(_resData["ServiceCharge"] ?? "0")!,
//                                비과세: Int(_resData["TaxFree"] ?? "0")!,
//                                할부: Int(_resData["Installment"] ?? "0" )!,
//                                현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "", 카드번호: "",
//                                카드종류: _resData["CardKind"] ?? "",
//                                카드매입사: _resData["InpNm"] ?? "",
//                                카드발급사: _resData["OrdNm"] ?? "",
//                                가맹점번호: _resData["MchNo"] ?? "",
//                                승인날짜: _resData["TrdDate"] ?? "",
//                                원거래일자: _resData["OriDate"] ?? "",
//                                승인번호: _resData["AuNo"] ?? "", 원승인번호: "",
//                                코세스고유거래키: _resData["TradeNo"] ?? "", 응답메시지: _resData["Message"] ?? "",
//                                KakaoMessage: _resData["KakaoMessage"] ?? "",
//                                PayType: _resData["PayType"] ?? "",
//                                KakaoAuMoney: _resData["KakaoAuMoney"] ?? "",
//                                KakaoSaleMoney: _resData["KakaoSaleMoney"] ?? "",
//                                KakaoMemberCd: _resData["KakaoMemberCd"] ?? "",
//                                KakaoMemberNo: _resData["KakaoMemberNo"] ?? "",
//                                Otc: _resData["Otc"] ?? "",
//                                Pem: _resData["Pem"] ?? "",
//                                Trid: _resData["Trid"] ?? "",
//                                CardBin: _resData["CardBin"] ?? "",
//                                SearchNo: _resData["SearchNo"] ?? "",
//                                PrintBarcd: "",
//                                PrintUse: _resData["PrintUse"] ?? "",
//                                PrintNm: _resData["PrintNm"] ?? "",
//                                MchFee: _resData["MchFee"] ?? "0",
//                                MchRefund: _resData["MchRefund"] ?? "0")
//
//                        if String(describing: Utils.topMostViewController()).contains("CardAnimationViewController") {
//                            let controller = Utils.topMostViewController() as! CardAnimationViewController
//                            controller.GoToReceiptEasyPaySwiftUI()
//
//                        }
//
//                    } else {
//                        //앱투앱인경우 리스너로 날려보냄
//                        KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_ZEROPAY_CANCEL_RES:
//
//                    if KakaoPaySdk.getInstance().m2TradeCancel == true { //망취소발생
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "망취소 발생. 거래실패 : " + (_resData["Message"] ?? "")
//                    resDataDic["TrdType"] = Command.CMD_ZEROPAY_CANCEL_RES
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    Clear()
//                    return
//                }
//
//                if _status == .sucess {
//
//                    if _resData["AnsCode"]! == "0100" {
//                        KakaoPaySdk.getInstance().전문번호 = Command.CMD_ZEROPAY_SEARCH_REQ
//                        KocesSdk.instance.ZeroPay( KakaoPaySdk.getInstance().전문번호,  KakaoPaySdk.getInstance()._Tid,  Utils.getDate(format: "yyMMddHHmmss"),  KakaoPaySdk.getInstance().단말버전,  KakaoPaySdk.getInstance().단말추가정보,  "",  "", Ori _resData["TrdDate"] ?? "", OriAuNumber: _resData["AuNo"] ?? "", BarCode: "", Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KakaoPaySdk.getInstance().통화코드, Installment: KakaoPaySdk.getInstance().할부개월, StoreInfo: KakaoPaySdk.getInstance().가맹점추가정보,  KakaoPaySdk.getInstance().가맹점데이터, KocesUniqueNum: KakaoPaySdk.getInstance().KOCES거래고유번호)
//                        return
//                    }
//
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if (!mDBAppToApp) {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade( KakaoPaySdk.getInstance()._Tid,
//                                신용현금: define.TradeMethod.Zero,
//                                취소여부: define.TradeMethod.Cancel,
//                                금액: Int(_resData["Money"] ?? "0")!,
//                                선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                세금: Int(_resData["Tax"] ?? "0")!,
//                                봉사료: Int(_resData["ServiceCharge"] ?? "0")!,
//                                비과세: Int(_resData["TaxFree"] ?? "0")!,
//                                할부: Int(_resData["Installment"] ?? "0" )!,
//                                현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "", 카드번호: "",
//                                카드종류: _resData["CardKind"] ?? "",
//                                카드매입사: _resData["InpNm"] ?? "",
//                                카드발급사: _resData["OrdNm"] ?? "",
//                                가맹점번호: _resData["MchNo"] ?? "",
//                                승인날짜: _resData["TrdDate"] ?? "",
//                                원거래일자: KakaoPaySdk.getInstance().원거래일자,
//                                승인번호: _resData["AuNo"] ?? "",
//                                원승인번호: KakaoPaySdk.getInstance().원승인번호,
//                                코세스고유거래키: _resData["TradeNo"] ?? "", 응답메시지: _resData["Message"] ?? "",
//                                KakaoMessage: _resData["KakaoMessage"] ?? "",
//                                PayType: _resData["PayType"] ?? "",
//                                KakaoAuMoney: _resData["KakaoAuMoney"] ?? "",
//                                KakaoSaleMoney: _resData["KakaoSaleMoney"] ?? "",
//                                KakaoMemberCd: _resData["KakaoMemberCd"] ?? "",
//                                KakaoMemberNo: _resData["KakaoMemberNo"] ?? "",
//                                Otc: _resData["Otc"] ?? "",
//                                Pem: _resData["Pem"] ?? "",
//                                Trid: _resData["Trid"] ?? "",
//                                CardBin: _resData["CardBin"] ?? "",
//                                SearchNo: _resData["SearchNo"] ?? "",
//                                PrintBarcd: "",
//                                PrintUse: _resData["PrintUse"] ?? "",
//                                PrintNm: _resData["PrintNm"] ?? "",
//                                MchFee: _resData["MchFee"] ?? "0",
//                                MchRefund: _resData["MchRefund"] ?? "0")
//
//                        if String(describing: Utils.topMostViewController()).contains("CardAnimationViewController") {
//                            let controller = Utils.topMostViewController() as! CardAnimationViewController
//                            controller.GoToReceiptEasyPaySwiftUI()
//
//                        }
//
//                    } else {
//                        //앱투앱인경우 리스너로 날려보냄
//                        KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_ZEROPAY_SEARCH_RES:
//                    if _status == .sucess {
//
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_WECHAT_ALIPAY_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case Command.CMD_WECHAT_ALIPAY_CANCEL_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case Command.CMD_WECHAT_ALIPAY_SEARCH_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case Command.CMD_WECHAT_ALIPAY_SEARCH_CANCEL_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                default:
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.getInstance().paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                    break
//            }
//            //끝났으니 값들을 모두 정리한다
//            Clear()
//        }
//    }
}
