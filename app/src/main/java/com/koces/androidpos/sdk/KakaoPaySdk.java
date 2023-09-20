//package com.koces.androidpos.sdk;
//
//public class KakaoPaySdk {
////
////  KaKaoPaySdk.swift
////  osxapp
////
////  Created by 金載龍 on 2021/04/29.
////
//    /**KocesSdk,tcpSdk 를 싱글톤으로 가져온다 */
//    private static KakaoPaySdk instance;
//    public static KakaoPaySdk getInstance(){
//        if(instance==null){
//            instance = new KakaoPaySdk();
//        }
//        return instance;
//    }
//    var paylistener: PayResultDelegate?
//    TaxSdk mTaxCalc = TaxSdk.getInstance();
//    String 전문번호 = "";
//    String _Tid = "";
//    String 거래일시 = "";
//    String 단말버전 = "";
//    String 단말추가정보 = "";
//    String 취소구분 = "";
//    String 원거래일자 = "";
//    String 원승인번호 = "";
//    String 입력방법 = "";
//    String 바코드번호 = "";
//    byte[] OTC카드번호 = new byte[]{};
//    String 거래금액 = "";
//    String 세금 = "";
//    String 봉사료 = "";
//    String 비과세 = "";
//    String 통화코드 = "";
//    String 할부개월 = "";
//    String 결제수단 = "";
//    String 취소종류 = "";
//    String 취소타입 = "";
//    String 점포코드 = "";
//    String _PEM = "";
//    String _trid = "";
//    String 카드BIN = "";
//    String 조회고유번호 = "";
//    String _WorkingKeyIndex = "";
//    String 전자서명사용여부 = "";
//    String 사인패드시리얼번호 = "";
//    byte[] 전자서명데이터 = new byte[]{};
//    String 가맹점데이터 = "";
//
//    //제로페이 추가데이터
//    String 가맹점추가정보 = "";
//    String KOCES거래고유번호 = "";
//
//    /** 현재 화면이 앱투앱인지 아닌지를 체크 DB 저장을 위해서 */
//    boolean mDBAppToApp = false;
//
//    /** 망취소발생하여 거래를 취소했다는 것을 알려주는 내용 */
//    boolean m2TradeCancel = false;
//
//    private void Clear(){
//        전문번호 = ""
//        _Tid = ""
//        거래일시 = ""
//        단말버전 = ""
//        단말추가정보 = ""
//        취소구분 = ""
//        원거래일자 = ""
//        원승인번호 = ""
//        입력방법 = ""
//        바코드번호 = ""
//        OTC카드번호 = []
//        거래금액 = ""
//        세금 = ""
//        봉사료 = ""
//        비과세 = ""
//        통화코드 = ""
//        할부개월 = ""
//        결제수단 = ""
//        취소종류 = ""
//        취소타입 = ""
//        점포코드 = ""
//        _PEM = ""
//        _trid = ""
//        카드BIN = ""
//        조회고유번호 = ""
//        _WorkingKeyIndex = ""
//        전자서명사용여부 = ""
//        사인패드시리얼번호 = ""
//        전자서명데이터 = []
//        가맹점데이터 = ""
//        m2TradeCancel = false
//    }
//
//    /**
//     일단 카카오페이만 테스트한다. 다른 부분이 들어온다면 공통 처리부분만 만들어두고 나머지를 이후 처리한다
//     */
//    public void EasyPay(String 전문번호,String _Tid,String 거래일시,String 단말버전,String 단말추가정보,String 취소구분,
//                        String 원거래일자, String 원승인번호, String 입력방법,String 바코드번호,byte[] OTC카드번호,
//                        String 거래금액:String,String 세금,String 봉사료,String 비과세,String 통화코드,String 할부개월,
//                        String 결제수단,String 취소종류,String 취소타입,String 점포코드,String _PEM,String _trid,
//                        String 카드BIN,String 조회고유번호,String _WorkingKeyIndex,String 전자서명사용여부,String 사인패드시리얼번호,
//                        byte[] 전자서명데이터,String 가맹점데이터,String 가맹점추가정보, String KOCES거래고유번호,payLinstener _paymentlistener:PayResultDelegate) {
//        //시작전에 항상 클리어 한다.
//        Clear();
//        KaKaoPaySdk.instance.paylistener = _paymentlistener;
//        KakaoPaySdk.instance.전문번호 = 전문번호;
//        KakaoPaySdk.instance._Tid = _Tid;
//        KakaoPaySdk.instance.거래일시 = 거래일시;
//        KakaoPaySdk.instance.단말버전 = 단말버전;
//        KakaoPaySdk.instance.단말추가정보 = 단말추가정보;
//        KakaoPaySdk.instance.취소구분 = 취소구분;
//        KakaoPaySdk.instance.원거래일자 = 원거래일자;
//        KakaoPaySdk.instance.원승인번호 = 원승인번호;
//        KakaoPaySdk.instance.입력방법 = 입력방법;
//        KakaoPaySdk.instance.바코드번호 = 바코드번호;
//        KakaoPaySdk.instance.OTC카드번호 = OTC카드번호;
//        KakaoPaySdk.instance.거래금액 = 거래금액;
//        KakaoPaySdk.instance.세금 = 세금;
//        KakaoPaySdk.instance.봉사료 = 봉사료;
//        KakaoPaySdk.instance.비과세 = 비과세;
//        KakaoPaySdk.instance.통화코드 = 통화코드;
//        KakaoPaySdk.instance.할부개월 = 할부개월;
//        KakaoPaySdk.instance.결제수단 = 결제수단;
//        KakaoPaySdk.instance.취소종류 = 취소종류;
//        KakaoPaySdk.instance.취소타입 = 취소타입;
//        KakaoPaySdk.instance.점포코드 = 점포코드;
//        KakaoPaySdk.instance._PEM = _PEM;
//        KakaoPaySdk.instance._trid = _trid;
//        KakaoPaySdk.instance.카드BIN = 카드BIN;
//        KakaoPaySdk.instance.조회고유번호 = 조회고유번호;
//        KakaoPaySdk.instance._WorkingKeyIndex = _WorkingKeyIndex;
//        KakaoPaySdk.instance.전자서명사용여부 = 전자서명사용여부;
//        KakaoPaySdk.instance.사인패드시리얼번호 = 사인패드시리얼번호;
//        KakaoPaySdk.instance.전자서명데이터 = 전자서명데이터;
//        KakaoPaySdk.instance.가맹점데이터 = 가맹점데이터;
//
//        //제로페이 추가데이터
//        KakaoPaySdk.instance.KOCES거래고유번호 = KOCES거래고유번호;
//        KakaoPaySdk.instance.가맹점추가정보 = 가맹점추가정보;
//
//        if(!거래금액.equals(""))  //금액이 이상한 경우
//        {
//            let trimmedString = 거래금액.trimmingCharacters(in: .whitespaces)
//            KaKaoPaySdk.instance.거래금액 = trimmedString
//
//            if Int(KaKaoPaySdk.instance.거래금액)! < 0 || Int(KaKaoPaySdk.instance.거래금액)! > 900000000  //금액은 최대 9억을 넘을 수 없다.
//            {
//                Clear()
//                var resDataDic:[String:String] = [:]
//                resDataDic["Message"] = "입력한 금액은 결제 할 수 없습니다"
//                KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                return;
//            }
//        }
//        else
//        {
//            Clear()
//            var resDataDic:[String:String] = [:]
//            resDataDic["Message"] = "입력한 금액은 결제 할 수 없습니다"
//            KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//            return;
//        }
//        if 할부개월 != ""  //할부금액이 이상한 경우
//        {
//            KaKaoPaySdk.instance.할부개월 = 할부개월
//
//            if Int(KaKaoPaySdk.instance.할부개월)! == 0 {}
//            else
//            {
//                if Int(KaKaoPaySdk.instance.할부개월)! < 2 || Int(KaKaoPaySdk.instance.할부개월)! > 99 //할부의 경우에는 최대 99개월을 넘길 수 없다.
//                {
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "할부 개월이 정상적이지 않습니다"
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    return;
//                }
//            }
//        }
//
//
//
//        KaKaoPaySdk.instance.할부개월 = 할부개월
//
//        if !취소구분.isEmpty {
//            KaKaoPaySdk.instance.원거래일자 = 원거래일자
//        }
//
//        if String(describing: _paymentlistener).contains("AppToApp") {
//            KaKaoPaySdk.instance.mDBAppToApp = true
//        } else  {
//            KaKaoPaySdk.instance.mDBAppToApp = false
//        }
//
//        if 전문번호 == "Z20" {
//            //제로페이 취소요청의 경우 바코드를 읽지 않고 바로 취소요청을 한다
//            Res_Scanner(Result: true, Message: "", Scanner: "")
//            return
//        }
//
//        //만일 바코드번호가 있는경우 스캐너를 열지 않고 해당 값으로 비교한다
//        if KaKaoPaySdk.instance.바코드번호 != "" {
//            Res_Scanner(Result: true, Message: "", Scanner: KaKaoPaySdk.instance.바코드번호)
//            return
//        }
//
//
//        Utils.ScannerOpen(Sdk: "KAKAO")
//
//
//
//    }
//
//    /**
//     스캐너 결과값
//     */
//    func Res_Scanner(Result _result:Bool, Message _msg:String, Scanner _scanner:String)
//    {
//        if _result != true {
//        var resDataDic:[String:String] = [:]
//        resDataDic["Message"] = _msg
//        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//        return
//    }
//        KaKaoPaySdk.instance.바코드번호 = _scanner
//
//        var scan_result = Scan_Data_Parser(Scan: _scanner)
//
//        if _scanner.isEmpty {
//        if KaKaoPaySdk.instance.전문번호 == "Z20" {
//            //제로페이 취소일 경우
//            scan_result = define.EasyPayMethod.Zero_Bar.rawValue
//        }
//    }
//
//        var 취소정보 = ""
//        if !KaKaoPaySdk.instance.취소구분.isEmpty { 취소정보 = KaKaoPaySdk.instance.취소구분 + String(KaKaoPaySdk.instance.원거래일자.prefix(6)) + KaKaoPaySdk.instance.원승인번호 }
//
//        // 금액이 5만원 이상, 이하인 경우
//        var TaxConvert:Int = 0 ;
//        if(!KaKaoPaySdk.instance.세금.isEmpty) {
//            TaxConvert = Int(KaKaoPaySdk.instance.세금) ?? 0
//        }
//        var SrvCharge:Int = 0;
//        if(!KaKaoPaySdk.instance.봉사료.isEmpty) {
//            SrvCharge = Int(KaKaoPaySdk.instance.봉사료) ?? 0
//        }
//        let TotalMoney:Int = (Int(KaKaoPaySdk.instance.거래금액) ?? 0) + TaxConvert + SrvCharge;
//
//        var compareMoney = 0
//        if String(describing: KaKaoPaySdk.instance.paylistener).contains("AppToApp") {
//        compareMoney = 50000    //앱투앱이면 5만원이상 일때 사인패드
//    } else  {
//        //앺투앱이 아니라면 세금설정을 체크.
//        if Setting.shared.getDefaultUserData(_key: define.UNSIGNED_SETMONEY) == "" {
//            compareMoney = 50000
//        } else {
//            compareMoney = Int(Setting.shared.getDefaultUserData(_key: define.UNSIGNED_SETMONEY)) ?? 50000
//        }
//    }
//
//        var convertMoney = ""
//        var convertTax = ""
//        var convertSvc = ""
//        var convertTaf = ""
//        if KaKaoPaySdk.instance.세금.isEmpty || KaKaoPaySdk.instance.세금 == " " || KaKaoPaySdk.instance.세금 == "0" { KaKaoPaySdk.instance.세금 = "" }
//        else
//        {
//            convertTax = Utils.leftPad(str: KaKaoPaySdk.instance.세금.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//        if KaKaoPaySdk.instance.봉사료.isEmpty || KaKaoPaySdk.instance.봉사료 == " " || KaKaoPaySdk.instance.봉사료 == "0" { KaKaoPaySdk.instance.봉사료 = "" }
//        else
//        {
//            convertSvc = Utils.leftPad(str: KaKaoPaySdk.instance.봉사료.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//        if KaKaoPaySdk.instance.비과세.isEmpty || KaKaoPaySdk.instance.비과세 == " " || KaKaoPaySdk.instance.비과세 == "0" { KaKaoPaySdk.instance.비과세 = "" }
//        else
//        {
//            convertTaf = Utils.leftPad(str: KaKaoPaySdk.instance.비과세.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//
//        convertMoney = Utils.leftPad(str: KaKaoPaySdk.instance.거래금액.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//
//        if TotalMoney > compareMoney {
//        if KaKaoPaySdk.instance.전자서명사용여부 == "0" || KaKaoPaySdk.instance.전자서명사용여부 == "4" {
//            KaKaoPaySdk.instance.전자서명사용여부 = "4"
//            Utils.CardAnimationViewControllerInit(Message: scan_result == define.EasyPayMethod.Kakao.rawValue ? "서버에 조회중입니다":"서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//
//            DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                switch scan_result {
//                    case define.EasyPayMethod.EMV.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ?  Command.CMD_ICTRADE_REQ : Command.CMD_ICTRADE_CANCEL_REQ
//                        KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "U", CardNumber: "", EncryptInfo: [UInt8](), Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: Array(KaKaoPaySdk.instance.바코드번호.utf8), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                        break
//                    case define.EasyPayMethod.Kakao.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ?  Command.CMD_KAKAOPAY_SEARCH_REQ : Command.CMD_KAKAOPAY_CANCEL_REQ
//                        KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: 취소정보, InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: KaKaoPaySdk.instance.OTC카드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: KaKaoPaySdk.instance.결제수단, CancelMethod: KaKaoPaySdk.instance.취소종류, CancelType: KaKaoPaySdk.instance.취소타입, StoreCode: KaKaoPaySdk.instance.점포코드, PEM: KaKaoPaySdk.instance._PEM, trid: KaKaoPaySdk.instance._trid, CardBIN: KaKaoPaySdk.instance.카드BIN, SearchNumber: KaKaoPaySdk.instance.조회고유번호, WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                        break
//                    case define.EasyPayMethod.Zero_Bar.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        break
//                    case define.EasyPayMethod.Zero_Qr.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        break
//                    case define.EasyPayMethod.Wechat.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                        Clear()
//                        var resDataDic:[String:String] = [:]
//                        resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                        break
//                    case define.EasyPayMethod.Ali.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                        Clear()
//                        var resDataDic:[String:String] = [:]
//                        resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                        break
//                    case define.EasyPayMethod.App_Card.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ICTRADE_REQ : Command.CMD_ICTRADE_CANCEL_REQ
//                        KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "K", CardNumber: KaKaoPaySdk.instance.바코드번호 + "=8911", EncryptInfo: [UInt8](), Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: [UInt8](), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                        break
//                    default:
//                        break
//                }
//
//            }
//        } else {
//            KaKaoPaySdk.instance.전자서명사용여부 = "B"
//            if scan_result == define.EasyPayMethod.Zero_Bar.rawValue ||
//                    scan_result == define.EasyPayMethod.Zero_Qr.rawValue {
//                Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//                DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                    KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                }
//                return
//            } else if scan_result == define.EasyPayMethod.Ali.rawValue ||
//                    scan_result == define.EasyPayMethod.Wechat.rawValue {
//                KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                Clear()
//                var resDataDic:[String:String] = [:]
//                resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                return
//            } else if scan_result == define.EasyPayMethod.Kakao.rawValue && KaKaoPaySdk.instance.결제수단 != "C" {
//                Utils.CardAnimationViewControllerInit(Message: "서버에 조회중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//                KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ?  Command.CMD_KAKAOPAY_SEARCH_REQ : Command.CMD_KAKAOPAY_CANCEL_REQ
//                KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: 취소정보, InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: KaKaoPaySdk.instance.OTC카드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: KaKaoPaySdk.instance.결제수단, CancelMethod: KaKaoPaySdk.instance.취소종류, CancelType: KaKaoPaySdk.instance.취소타입, StoreCode: KaKaoPaySdk.instance.점포코드, PEM: KaKaoPaySdk.instance._PEM, trid: KaKaoPaySdk.instance._trid, CardBIN: KaKaoPaySdk.instance.카드BIN, SearchNumber: KaKaoPaySdk.instance.조회고유번호, WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                return
//            }
//
//            var storyboard:UIStoryboard?
//            if UIDevice.current.userInterfaceIdiom == .phone {
//                storyboard = UIStoryboard(name: "Main", bundle: Bundle.main)
//            } else {
//                storyboard = UIStoryboard(name: "pad", bundle: Bundle.main)
//            }
//            guard let signPad = storyboard!.instantiateViewController(withIdentifier: "SignatureController") as? SignatureController else {return}
//            signPad.sdk = "KaKaoPaySdk"
//            signPad.view.backgroundColor = .white
//            signPad.modalPresentationStyle = .fullScreen
//            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5){ [self] in
//                Utils.topMostViewController()?.present(signPad, animated: true, completion: nil)
//            }
//
//        }
//
//    } else {
//        Utils.CardAnimationViewControllerInit(Message: scan_result == define.EasyPayMethod.Kakao.rawValue ? "서버에 조회중입니다":"서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//        KaKaoPaySdk.instance.전자서명사용여부 = "5"
//        DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//            switch scan_result {
//                case define.EasyPayMethod.EMV.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ?  Command.CMD_ICTRADE_REQ : Command.CMD_ICTRADE_CANCEL_REQ
//                    KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "U", CardNumber: "", EncryptInfo: [UInt8](), Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: Array(KaKaoPaySdk.instance.바코드번호.utf8), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                    break
//                case define.EasyPayMethod.Kakao.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ?  Command.CMD_KAKAOPAY_SEARCH_REQ : Command.CMD_KAKAOPAY_CANCEL_REQ
//                    KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: 취소정보, InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: KaKaoPaySdk.instance.OTC카드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: KaKaoPaySdk.instance.결제수단, CancelMethod: KaKaoPaySdk.instance.취소종류, CancelType: KaKaoPaySdk.instance.취소타입, StoreCode: KaKaoPaySdk.instance.점포코드, PEM: KaKaoPaySdk.instance._PEM, trid: KaKaoPaySdk.instance._trid, CardBIN: KaKaoPaySdk.instance.카드BIN, SearchNumber: KaKaoPaySdk.instance.조회고유번호, WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                    break
//                case define.EasyPayMethod.Zero_Bar.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                    KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                    break
//                case define.EasyPayMethod.Zero_Qr.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                    KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                    break
//                case define.EasyPayMethod.Wechat.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case define.EasyPayMethod.Ali.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case define.EasyPayMethod.App_Card.rawValue:
//                    KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ICTRADE_REQ : Command.CMD_ICTRADE_CANCEL_REQ
//
//                    KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "K", CardNumber: Utils.rightPad(str: KaKaoPaySdk.instance.바코드번호 + "=8911", fillChar: " ", length: 40) , EncryptInfo: [UInt8](), Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: [UInt8](), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                    break
//                default:
//                    break
//            }
//
//        }
//    }
//
//
//    }
//
//    /**
//     스캔한 데이터를 파싱해서 제로/카카오/emv 등을 구분한다
//     */
//    func Scan_Data_Parser(Scan _scan:String) -> String{
//        var returnData = ""
//        if _scan.prefix(7) == "hQVDUFY" {
//            //EMV QR
//            returnData = define.EasyPayMethod.EMV.rawValue
//        } else if _scan.prefix(6) == "281006" {
//            //카카오페이
//            returnData = define.EasyPayMethod.Kakao.rawValue
//        } else if _scan.prefix(6) == "800088" {
//            //제로페이 Barcode
//            returnData = define.EasyPayMethod.Zero_Bar.rawValue
//        } else if _scan.prefix(2) == "3-" {
//            //제로페이 QRcode
//            returnData = define.EasyPayMethod.Zero_Qr.rawValue
//        } else {
//            if _scan.prefix(2) == "11" || _scan.prefix(2) == "12" ||
//                    _scan.prefix(2) == "13" || _scan.prefix(2) == "14" ||
//                    _scan.prefix(2) == "15" || _scan.prefix(2) == "10"  {
//                //위쳇페이
//                returnData = define.EasyPayMethod.Wechat.rawValue
//            } else if _scan.prefix(2) == "25" || _scan.prefix(2) == "26" ||
//                    _scan.prefix(2) == "27" || _scan.prefix(2) == "28" ||
//                    _scan.prefix(2) == "29" || _scan.prefix(2) == "30" {
//                //알리페이
//                returnData = define.EasyPayMethod.Ali.rawValue
//            } else {
//                if _scan.count == 21 {
//                    //APP 카드
//                    returnData = define.EasyPayMethod.App_Card.rawValue
//                }
//            }
//        }
//
//        return returnData
//    }
//
//    /**
//     사인패드에서 이미지를 그리면 이곳으로 보낸다
//     */
//    func Result_SignPad(signCheck _sign:Bool, signImage _signImage:[UInt8]) {
//        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5){ [self] in
//            if !_sign {
//                Clear()
//                var resDataDic:[String:String] = [:]
//                resDataDic["Message"] = "서명오류입니다. 서명이 정상적으로 진행되지 않았습니다"
//                KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                return
//            }
//
//            debugPrint("signData :", Utils.UInt8ArrayToHexCode(_value: _signImage,_option: true))
//            //사인데이터 1086 사이즈를 2배로 늘린다
//            let _sign = Utils.SignUin8ArrayToStringHexCode(_value: _signImage)
//            var 취소정보 = ""
//            if !KaKaoPaySdk.instance.취소구분.isEmpty { 취소정보 = KaKaoPaySdk.instance.취소구분 + String(KaKaoPaySdk.instance.원거래일자.prefix(6)) + KaKaoPaySdk.instance.원승인번호 }
//            KaKaoPaySdk.instance.전자서명사용여부 = "B"
//
//            Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//
//            var convertMoney = ""
//            var convertTax = ""
//            var convertSvc = ""
//            var convertTaf = ""
//            if KaKaoPaySdk.instance.세금.isEmpty || KaKaoPaySdk.instance.세금 == " " || KaKaoPaySdk.instance.세금 == "0" { KaKaoPaySdk.instance.세금 = "" }
//            else
//            {
//                convertTax = Utils.leftPad(str: KaKaoPaySdk.instance.세금.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//            }
//            if KaKaoPaySdk.instance.봉사료.isEmpty || KaKaoPaySdk.instance.봉사료 == " " || KaKaoPaySdk.instance.봉사료 == "0" { KaKaoPaySdk.instance.봉사료 = "" }
//            else
//            {
//                convertSvc = Utils.leftPad(str: KaKaoPaySdk.instance.봉사료.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//            }
//            if KaKaoPaySdk.instance.비과세.isEmpty || KaKaoPaySdk.instance.비과세 == " " || KaKaoPaySdk.instance.비과세 == "0" { KaKaoPaySdk.instance.비과세 = "" }
//            else
//            {
//                convertTaf = Utils.leftPad(str: KaKaoPaySdk.instance.비과세.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//            }
//
//            convertMoney = Utils.leftPad(str: KaKaoPaySdk.instance.거래금액.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//            DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                switch Scan_Data_Parser(Scan: KaKaoPaySdk.instance.바코드번호) {
//                    case define.EasyPayMethod.EMV.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ?  Command.CMD_ICTRADE_REQ : Command.CMD_ICTRADE_CANCEL_REQ
//                        KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "U", CardNumber: "", EncryptInfo: [UInt8](), Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: Array(KaKaoPaySdk.instance.바코드번호.utf8), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                        break
//                    case define.EasyPayMethod.Kakao.rawValue:
//                        if KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ || KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_SEARCH_REQ {
//                        KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ
//                    } else {
//                        KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_CANCEL_REQ
//                    }
//                    KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: 취소정보, InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: KaKaoPaySdk.instance.OTC카드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: KaKaoPaySdk.instance.결제수단, CancelMethod: KaKaoPaySdk.instance.취소종류, CancelType: KaKaoPaySdk.instance.취소타입, StoreCode: KaKaoPaySdk.instance.점포코드, PEM: KaKaoPaySdk.instance._PEM, trid: KaKaoPaySdk.instance._trid, CardBIN: KaKaoPaySdk.instance.카드BIN, SearchNumber: KaKaoPaySdk.instance.조회고유번호, WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: _sign, StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                    break
//                    case define.EasyPayMethod.Zero_Bar.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        break
//                    case define.EasyPayMethod.Zero_Qr.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ZEROPAY_REQ : Command.CMD_ZEROPAY_CANCEL_REQ
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: KaKaoPaySdk.instance.취소구분, InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        break
//                    case define.EasyPayMethod.Wechat.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                        Clear()
//                        var resDataDic:[String:String] = [:]
//                        resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                        break
//                    case define.EasyPayMethod.Ali.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_WECHAT_ALIPAY_REQ : Command.CMD_WECHAT_ALIPAY_CANCEL_REQ
//                        Clear()
//                        var resDataDic:[String:String] = [:]
//                        resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                        break
//                    case define.EasyPayMethod.App_Card.rawValue:
//                        KaKaoPaySdk.instance.전문번호 = KaKaoPaySdk.instance.전문번호 == Command.CMD_KAKAOPAY_REQ ? Command.CMD_ICTRADE_REQ : Command.CMD_ICTRADE_CANCEL_REQ
//                        KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: KaKaoPaySdk.instance.거래일시, PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "K", CardNumber: KaKaoPaySdk.instance.바코드번호 + "=8911", EncryptInfo: [UInt8](), Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: [UInt8](), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                        break
//                    default:
//                        break
//                }
//            }
//        }
//    }
//
//    /**
//     결과를 해당 뷰컨트롤러로 보낸다
//     - Parameters:
//     - _status: _status description
//     - _resData: <#_resData description#>
//     */
//    func Res_Tcp_KakaoPay(tcpStatus _status:tcpStatus,ResData _resData:[String:String]) {
//        /**
//         내부 완료부 완성해야함
//         */
//        var convertMoney = ""
//        var convertTax = ""
//        var convertSvc = ""
//        var convertTaf = ""
//        if KaKaoPaySdk.instance.세금.isEmpty || KaKaoPaySdk.instance.세금 == " " || KaKaoPaySdk.instance.세금 == "0" { KaKaoPaySdk.instance.세금 = "" }
//        else
//        {
//            convertTax = Utils.leftPad(str: KaKaoPaySdk.instance.세금.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//        if KaKaoPaySdk.instance.봉사료.isEmpty || KaKaoPaySdk.instance.봉사료 == " " || KaKaoPaySdk.instance.봉사료 == "0" { KaKaoPaySdk.instance.봉사료 = "" }
//        else
//        {
//            convertSvc = Utils.leftPad(str: KaKaoPaySdk.instance.봉사료.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//        if KaKaoPaySdk.instance.비과세.isEmpty || KaKaoPaySdk.instance.비과세 == " " || KaKaoPaySdk.instance.비과세 == "0" { KaKaoPaySdk.instance.비과세 = "" }
//        else
//        {
//            convertTaf = Utils.leftPad(str: KaKaoPaySdk.instance.비과세.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        }
//
//        convertMoney = Utils.leftPad(str: KaKaoPaySdk.instance.거래금액.trimmingCharacters(in: .whitespaces), fillChar: "0", length: 12)
//        debugPrint(_status)
//        debugPrint(_resData)
//        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [self] in
//            switch _resData["TrdType"] {
//                case Command.CMD_KAKAOPAY_RES:
//                    if _status == .sucess {
//                    if KocesSdk.instance.mEotCheck != 0 {
//                        let 취소정보 = "I" + String(_resData["date"]?.prefix(6) ?? "") + _resData["AuNo"]!
//                                KaKaoPaySdk.instance.전문번호 = Command.CMD_KAKAOPAY_CANCEL_REQ
//                        var TotalMoney:Int = Int(convertMoney)! + Int(convertTax)! + Int(convertSvc)! + Int(convertTaf)! //2021-08-19 kim.jy 취소는 총합으로
//                                convertTax = "0"
//                        convertSvc = "0"
//                        convertTaf = "0"
//
//                        KaKaoPaySdk.instance.m2TradeCancel = true    //망취소발생
//
//                        KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: 취소정보, InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: [UInt8](), Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: "", CancelMethod: "0", CancelType: "B", StoreCode: KaKaoPaySdk.instance.점포코드, PEM: "", trid: "", CardBIN: "", SearchNumber: KaKaoPaySdk.instance.조회고유번호, WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: KaKaoPaySdk.instance.전자서명데이터, StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                        return
//                    }
//
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if !self.mDBAppToApp {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
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
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_KAKAOPAY_CANCEL_RES:
//
//                    if KaKaoPaySdk.instance.m2TradeCancel == true { //망취소발생
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "망취소 발생. 거래실패 : " + (_resData["Message"] ?? "")
//                    resDataDic["TrdType"] = Command.CMD_KAKAOPAY_CANCEL_RES
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    Clear()
//                    return
//                }
//
//                if _status == .sucess {
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if !self.mDBAppToApp {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
//                                신용현금: define.TradeMethod.Kakao,
//                                취소여부: define.TradeMethod.Cancel,
//                                금액: Int(_resData["Money"] ?? "0")!,
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
//                                원거래일자: KaKaoPaySdk.instance.원거래일자,
//                                승인번호: _resData["AuNo"]!,
//                                원승인번호: KaKaoPaySdk.instance.원승인번호,
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
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_KAKAOPAY_SEARCH_RES:
//                    //조회가 여기까지 올 이유가 없다.
//                    if _status == .sucess {
//
//                    var _totalMoney = 0
//                    var Money = KaKaoPaySdk.instance.거래금액
//                    var Tax = KaKaoPaySdk.instance.세금
//                    var ServiceCharge = KaKaoPaySdk.instance.봉사료
//                    var TaxFree = KaKaoPaySdk.instance.비과세
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
//                    if String(describing: KaKaoPaySdk.instance.paylistener).contains("AppToApp") {
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
//                    KaKaoPaySdk.instance.전문번호 = Command.CMD_KAKAOPAY_REQ
//                    KaKaoPaySdk.instance.OTC카드번호 = Command.StrToArr(String(_resData["Otc"] ?? ""))
//                    KaKaoPaySdk.instance.거래금액 = Money
//                    KaKaoPaySdk.instance.세금 = Tax
//                    KaKaoPaySdk.instance.봉사료 = ServiceCharge
//                    KaKaoPaySdk.instance.비과세 = TaxFree
//                    KaKaoPaySdk.instance.결제수단 = String(_resData["PayType"]!)
//                    KaKaoPaySdk.instance._PEM = String(_resData["Pem"]!)
//                    KaKaoPaySdk.instance._trid = String(_resData["Trid"]!)
//                    KaKaoPaySdk.instance.카드BIN = String(_resData["CardBin"]!)
//                    KaKaoPaySdk.instance.조회고유번호 = String(_resData["SearchNo"]!)
//                    if _totalMoney > compareMoney {
//                        if KaKaoPaySdk.instance.전자서명사용여부 == "0" || KaKaoPaySdk.instance.전자서명사용여부 == "4" {
//                            KaKaoPaySdk.instance.전자서명사용여부 = "4"
//                            Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//                            DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                                KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: "", InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: Command.StrToArr(String(_resData["Otc"] ?? "")), Money: Money, Tax: Tax, ServiceCharge: ServiceCharge, TaxFree: TaxFree, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: String(_resData["PayType"]!), CancelMethod: "0", CancelType: "B", StoreCode: KaKaoPaySdk.instance.점포코드, PEM: String(_resData["Pem"]!), trid: String(_resData["Trid"]!), CardBIN: String(_resData["CardBin"]!), SearchNumber: String(_resData["SearchNo"]!), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: KaKaoPaySdk.instance.전자서명데이터, StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                            }
//                            return
//                        } else  {
//                            KaKaoPaySdk.instance.전자서명사용여부 = "B"
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
//                                Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//                                DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                                    KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: "", InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: Command.StrToArr(String(_resData["Otc"] ?? "")), Money: Money, Tax: Tax, ServiceCharge: ServiceCharge, TaxFree: TaxFree, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: String(_resData["PayType"]!), CancelMethod: "0", CancelType: "B", StoreCode: KaKaoPaySdk.instance.점포코드, PEM: String(_resData["Pem"]!), trid: String(_resData["Trid"]!), CardBIN: String(_resData["CardBin"]!), SearchNumber: String(_resData["SearchNo"]!), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: KaKaoPaySdk.instance.전자서명데이터, StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                                }
//
//                            }
//                            return
//
//                        }
//
//                    } else {
//                        KaKaoPaySdk.instance.전자서명사용여부 = "5"
//                        Utils.CardAnimationViewControllerInit(Message: "서버에 요청중입니다", isButton: false, CountDown: Setting.shared.mDgTmout, Listener: KaKaoPaySdk.instance.paylistener as! PayResultDelegate)
//                        DispatchQueue.main.asyncAfter(deadline: .now() + 1){ [self] in
//                            KocesSdk.instance.KakaoPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: "", InputType: KaKaoPaySdk.instance.입력방법, BarCode: KaKaoPaySdk.instance.바코드번호, OTCCardCode: Command.StrToArr(String(_resData["Otc"] ?? "")), Money: Money, Tax: Tax, ServiceCharge: ServiceCharge, TaxFree: TaxFree, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, PayType: String(_resData["PayType"]!), CancelMethod: "0", CancelType: "B", StoreCode: KaKaoPaySdk.instance.점포코드, PEM: String(_resData["Pem"]!), trid: String(_resData["Trid"]!), CardBIN: String(_resData["CardBin"]!), SearchNumber: String(_resData["SearchNo"]!), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, SignUse: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: KaKaoPaySdk.instance.전자서명데이터, StoreData: KaKaoPaySdk.instance.가맹점데이터)
//                        }
//                        return
//                    }
//
//
//
//
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
////                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//
//                case Command.CMD_IC_OK_RES:
//                    if _status == .sucess {
//                    if KocesSdk.instance.mEotCheck != 0 {
//                        let 취소정보 = "I" + String(_resData["date"]?.prefix(6) ?? "") + _resData["AuNo"]!
//                                KaKaoPaySdk.instance.전문번호 = Command.CMD_ICTRADE_CANCEL_REQ
//                        var TotalMoney:Int = Int(convertMoney)! + Int(convertTax)! + Int(convertSvc)! + Int(convertTaf)! //2021-08-19 kim.jy 취소는 총합으로
//                                convertTax = "0"
//                        convertSvc = "0"
//                        convertTaf = "0"
//
//                        KaKaoPaySdk.instance.m2TradeCancel = true    //망취소발생
//
//                        if KaKaoPaySdk.instance.입력방법 == "U" {
//                            KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "U", CardNumber: "", EncryptInfo: [UInt8](), Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: Array(KaKaoPaySdk.instance.바코드번호.utf8), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                        } else {
//                            KocesSdk.instance.Credit(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, ResonCancel: 취소정보, InputType: "K", CardNumber: KaKaoPaySdk.instance.바코드번호 + "=8911", EncryptInfo: [UInt8](), Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, InstallMent: KaKaoPaySdk.instance.할부개월, PosCertificationNumber: Utils.AppTmlcNo(), TradeType: "", EmvData: "", ResonFallBack: "", ICreqData: [UInt8](), WorkingKeyIndex: KaKaoPaySdk.instance._WorkingKeyIndex, Password: "", OilSurpport: "", OilTaxFree: "", DccFlag: "", DccReqInfo: "", PointCardCode: "", PointCardNumber: "", PointCardEncprytInfo: [UInt8](), SignInfo: KaKaoPaySdk.instance.전자서명사용여부, SignPadSerial: KaKaoPaySdk.instance.사인패드시리얼번호, SignData: [UInt8](), Certification: "", PosData: KaKaoPaySdk.instance.가맹점데이터, KocesUid: "", UniqueCode: "", MacAddr: Utils.getKeyChainUUID(), HardwareKey: Utils.getPosKeyChainUUIDtoBase64(Target: KaKaoPaySdk.instance.mDBAppToApp == true ? .AppToApp:.KocesICIOSPay, Tid: KaKaoPaySdk.instance._Tid))
//                        }
//                        return
//                    }
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if !self.mDBAppToApp {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//
//                        /**
//                         이쪽 DB 업데이트 하는 부분 정리 필요. 어떤 내용이 있는지 체크필요. 앱카드 및 EMV QR 관련 받은데이터를 어떻게 정리하는 지 필요함
//                         그리고 해당 데이터를 영수증화면에 어떻게 출력하는지도 필요
//                         */
//
//                        if Scan_Data_Parser(Scan: KaKaoPaySdk.instance.바코드번호) == define.EasyPayMethod.App_Card.rawValue {
//                            sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
//                                    신용현금: define.TradeMethod.AppCard,
//                                    취소여부: define.TradeMethod.NoCancel,
//                                    금액: Int(KaKaoPaySdk.instance.거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KaKaoPaySdk.instance.세금) ?? 0,
//                                    봉사료: Int(KaKaoPaySdk.instance.봉사료) ?? 0,
//                                    비과세: Int(KaKaoPaySdk.instance.비과세) ?? 0,
//                                    할부: Int(KaKaoPaySdk.instance.할부개월) ?? 0,
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
//                            sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
//                                    신용현금: define.TradeMethod.EmvQr,
//                                    취소여부: define.TradeMethod.NoCancel,
//                                    금액: Int(KaKaoPaySdk.instance.거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KaKaoPaySdk.instance.세금) ?? 0,
//                                    봉사료: Int(KaKaoPaySdk.instance.봉사료) ?? 0,
//                                    비과세: Int(KaKaoPaySdk.instance.비과세) ?? 0,
//                                    할부: Int(KaKaoPaySdk.instance.할부개월) ?? 0,
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
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_IC_CANCEL_RES:
//
//                    if KaKaoPaySdk.instance.m2TradeCancel == true { //망취소발생
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "망취소 발생. 거래실패 : " + (_resData["Message"] ?? "")
//                    resDataDic["TrdType"] = Command.CMD_IC_CANCEL_RES
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    Clear()
//                    return
//                }
//
//                if _status == .sucess {
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if !self.mDBAppToApp {
//
//                        /**
//                         이쪽 DB 업데이트 하는 부분 정리 필요. 어떤 내용이 있는지 체크필요. 앱카드 및 EMV QR 관련 받은데이터를 어떻게 정리하는 지 필요함
//                         그리고 해당 데이터를 영수증화면에 어떻게 출력하는지도 필요
//                         */
//
//                        if Scan_Data_Parser(Scan: KaKaoPaySdk.instance.바코드번호) == define.EasyPayMethod.App_Card.rawValue {
//                            sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
//                                    신용현금: define.TradeMethod.AppCard,
//                                    취소여부: define.TradeMethod.Cancel,
//                                    금액: Int(KaKaoPaySdk.instance.거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KaKaoPaySdk.instance.세금) ?? 0,
//                                    봉사료: Int(KaKaoPaySdk.instance.봉사료) ?? 0,
//                                    비과세: Int(KaKaoPaySdk.instance.비과세) ?? 0,
//                                    할부: Int(KaKaoPaySdk.instance.할부개월) ?? 0,
//                                    현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "",
//                                    카드번호: _resData["CardNo"] ?? "",
//                                    카드종류: _resData["CardKind"]!,
//                                    카드매입사: _resData["InpNm"]!,
//                                    카드발급사: _resData["OrdNm"]!,
//                                    가맹점번호: _resData["MchNo"]!,
//                                    승인날짜: _resData["TrdDate"]!,
//                                    원거래일자: KaKaoPaySdk.instance.원거래일자,
//                                    승인번호: _resData["AuNo"]!,
//                                    원승인번호: KaKaoPaySdk.instance.원승인번호,
//                                    코세스고유거래키: _resData["TradeNo"]!, 응답메시지: _resData["Message"] ?? "", KakaoMessage: "", PayType: "", KakaoAuMoney: "", KakaoSaleMoney: "", KakaoMemberCd: "", KakaoMemberNo: "", Otc: "", Pem: "", Trid: "", CardBin: "", SearchNo: "", PrintBarcd: "", PrintUse: "", PrintNm: "", MchFee: "", MchRefund: "")
//                        } else {
//                            sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
//                                    신용현금: define.TradeMethod.EmvQr,
//                                    취소여부: define.TradeMethod.Cancel,
//                                    금액: Int(KaKaoPaySdk.instance.거래금액) ?? 0,
//                                    선불카드잔액: _resData["GiftAmt"] ?? "0",
//                                    세금: Int(KaKaoPaySdk.instance.세금) ?? 0,
//                                    봉사료: Int(KaKaoPaySdk.instance.봉사료) ?? 0,
//                                    비과세: Int(KaKaoPaySdk.instance.비과세) ?? 0,
//                                    할부: Int(KaKaoPaySdk.instance.할부개월) ?? 0,
//                                    현금영수증타겟: define.TradeMethod.NULL, 현금영수증발급형태: define.TradeMethod.NULL,현금발급번호: "",
//                                    카드번호: _resData["CardNo"] ?? "",
//                                    카드종류: _resData["CardKind"]!,
//                                    카드매입사: _resData["InpNm"]!,
//                                    카드발급사: _resData["OrdNm"]!,
//                                    가맹점번호: _resData["MchNo"]!,
//                                    승인날짜: _resData["TrdDate"]!,
//                                    원거래일자: KaKaoPaySdk.instance.원거래일자,
//                                    승인번호: _resData["AuNo"]!,
//                                    원승인번호: KaKaoPaySdk.instance.원승인번호,
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
//                        KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KaKaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_ZEROPAY_RES:
//                    if _status == .sucess {
//                    if KocesSdk.instance.mEotCheck != 0 {
////                        let 취소정보 = "1" + String(_resData["date"]?.prefix(6) ?? "") + _resData["AuNo"]!
//                        KaKaoPaySdk.instance.전문번호 = Command.CMD_ZEROPAY_CANCEL_REQ
//                        var TotalMoney:Int = Int(convertMoney)! + Int(convertTax)! + Int(convertSvc)! + Int(convertTaf)! //2021-08-19 kim.jy 취소는 총합으로
//                        convertTax = "0";
//                        convertSvc = "0";
//                        convertTaf = "0";
//
//                        KakaoPaySdk.instance.m2TradeCancel = true;    //망취소발생
//
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: "1", InputType: KaKaoPaySdk.instance.입력방법, OriDate: KaKaoPaySdk.instance.원거래일자, OriAuNumber: KaKaoPaySdk.instance.원승인번호, BarCode: KaKaoPaySdk.instance.바코드번호, Money: String(TotalMoney), Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        return
//                    }
//
//                    if _resData["AnsCode"]! == "0100" {
//                        KakaoPaySdk.instance.전문번호 = Command.CMD_ZEROPAY_SEARCH_REQ
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: "", InputType: "", OriDate: _resData["TrdDate"] ?? "", OriAuNumber: _resData["AuNo"] ?? "", BarCode: "", Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        return
//                    }
//
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if !self.mDBAppToApp {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
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
//                        KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_ZEROPAY_CANCEL_RES:
//
//                    if KakaoPaySdk.instance.m2TradeCancel == true { //망취소발생
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "망취소 발생. 거래실패 : " + (_resData["Message"] ?? "")
//                    resDataDic["TrdType"] = Command.CMD_ZEROPAY_CANCEL_RES
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    Clear()
//                    return
//                }
//
//                if _status == .sucess {
//
//                    if _resData["AnsCode"]! == "0100" {
//                        KakaoPaySdk.instance.전문번호 = Command.CMD_ZEROPAY_SEARCH_REQ
//                        KocesSdk.instance.ZeroPay(Command: KaKaoPaySdk.instance.전문번호, Tid: KaKaoPaySdk.instance._Tid, Date: Utils.getDate(format: "yyMMddHHmmss"), PosVer: KaKaoPaySdk.instance.단말버전, Etc: KaKaoPaySdk.instance.단말추가정보, CancelInfo: "", InputType: "", OriDate: _resData["TrdDate"] ?? "", OriAuNumber: _resData["AuNo"] ?? "", BarCode: "", Money: convertMoney, Tax: convertTax, ServiceCharge: convertSvc, TaxFree: convertTaf, Currency: KaKaoPaySdk.instance.통화코드, Installment: KaKaoPaySdk.instance.할부개월, StoreInfo: KaKaoPaySdk.instance.가맹점추가정보, StoreData: KaKaoPaySdk.instance.가맹점데이터, KocesUniqueNum: KaKaoPaySdk.instance.KOCES거래고유번호)
//                        return
//                    }
//
//                    //앱투앱이 아닌 경우에만 DB에 저장
//                    if !self.mDBAppToApp {
//
//                        //여기서 sqlite에 거래 내역 저장 한다. 신용/현금인경우 리스너를 제거하고 영수증으로 보냄
//                        sqlite.instance.InsertTrade(Tid: KaKaoPaySdk.instance._Tid,
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
//                                원거래일자: KaKaoPaySdk.instance.원거래일자,
//                                승인번호: _resData["AuNo"] ?? "",
//                                원승인번호: KaKaoPaySdk.instance.원승인번호,
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
//                        KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//                    }
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_ZEROPAY_SEARCH_RES:
//                    if _status == .sucess {
//
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .OK, payResult: _resData) //여기서 paylistener 가 널임
//
//                } else {
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                }
//                break
//                case Command.CMD_WECHAT_ALIPAY_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case Command.CMD_WECHAT_ALIPAY_CANCEL_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case Command.CMD_WECHAT_ALIPAY_SEARCH_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                case Command.CMD_WECHAT_ALIPAY_SEARCH_CANCEL_RES:
//                    Clear()
//                    var resDataDic:[String:String] = [:]
//                    resDataDic["Message"] = "거래불가. 위쳇 알리페이는 지원하지 않습니다."
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: resDataDic)
//                    break
//                default:
//                    //정상적인 데이터가 아니라면 여기서 에러로 처리한다
//                    KakaoPaySdk.instance.paylistener?.onPaymentResult(payTitle: .ERROR, payResult: _resData)
//                    break
//            }
//            //끝났으니 값들을 모두 정리한다
//            Clear()
//        }
//    }
//}
