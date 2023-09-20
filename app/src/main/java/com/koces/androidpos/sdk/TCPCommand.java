package com.koces.androidpos.sdk;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.koces.androidpos.sdk.van.Constants;

import java.math.BigInteger;

public class TCPCommand {
    private static final String TAG = "TCPCommand";
    /**
     * TCP 전문용 설정값
     */
    public static final String PROTOCOL_VERSION = "1029";
    public static final String POS_TYPE = "P";
    public static final int TCPSEVER_DISCONNECTED = -1;
    public static final int TCP_PROCEDURE_NONE = 0;
    public static final int TCP_PROCEDURE_INIT = 1;
    public static final int TCP_PROCEDURE_SEND = 2;
    public static final int TCP_PROCEDURE_RESEND = 3;
    public static final int TCP_PROCEDURE_DATA = 4;
    public static final int TCP_PROCEDURE_SEND_ACK = 5;
    public static final int TCP_PROCEDURE_SEND_NAK = 6;
    public static final int TCP_PROCEDURE_EOT = 7;
    public static final int TCP_PROCEDURE_RE_REQUEST = 10;
    public static final int TCP_PROCEDURE_ERROR_PACKER_REQUEST = 11;

    public static final String CMD_ICTRADE_REQ = "A10";
    public static final String CMD_ICTRADE_CANCEL_REQ = "A20";
    public static final String CMD_REGISTERED_SHOP_DOWNLOAD_REQ = "D10"; //가맹점 다운로드
    public static final String CMD_REGISTERED_SHOPS_DOWNLOAD_REQ = "D11"; //복수 가맹점 다운로드
    public static final String CMD_KEY_UPDATE_REQ = "D20"; // 키업데이트
    public static final String CMD_SHOP_DOWNLOAD_RES = "D15";
    public static final String CMD_SHOPS_DOWNLOAD_RES = "D16"; //복수 가맹점
    public static final String CMD_KEY_UPDATE_RES = "D25";
    public static final String CMD_IC_OK_RES = "A15";
    public static final String CMD_IC_CANCEL_RES = "A25";

    public static final String CMD_CASH_RECEIPT_REQ = "B10";  //현금영수증 전문 요청
    public static final String CMD_CASH_RECEIPT_CANCEL_REQ = "B20";  //현금영수증 전문 취소 요청
    public static final String CMD_CASH_RECEIPT_RES = "B15"; //현금영수증 전문 응답
    public static final String CMD_CASH_RECEIPT_CANCEL_RES = "B25"; //현금영수증 전문 취소 응답

    public static final String CMD_CASHIC_BUY_RES = "C15"; //현금 IC 구매 응답
    public static final String CMD_CASHIC_BUY_CANCEL_RES = "C25"; //현금 IC 구매 취소(환불) 응답
    public static final String CMD_CASHIC_CHECK_ACCOUNT_RES = "C35";// 현금 IC 잔액 조회 응답

    public static final String CMD_WECHAT_ALIPAY_REQ = "W10";  //위쳇/알리페이 전문 요청
    public static final String CMD_WECHAT_ALIPAY_CANCEL_REQ = "W20";  //위쳇/알리페이 전문 취소 요청
    public static final String CMD_WECHAT_ALIPAY_SEARCH_REQ = "W30";  //위쳇/알리페이 전문 조회 요청
    public static final String CMD_WECHAT_ALIPAY_SEARCH_CANCEL_REQ = "W40";  //위쳇 전문 조회취소 요청(알리는 조회취소가 없다)
    public static final String CMD_WECHAT_ALIPAY_RES = "W15"; //위쳇/알리페이 전문 응답
    public static final String CMD_WECHAT_ALIPAY_CANCEL_RES = "W25"; //위쳇/알리페이 전문 취소 응답
    public static final String CMD_WECHAT_ALIPAY_SEARCH_RES = "W35"; //위쳇/알리페이 전문 조회 응답
    public static final String CMD_WECHAT_ALIPAY_SEARCH_CANCEL_RES = "W45"; //위쳇 전문 조회취소 응답(알리는 조회취소가 없다)

    public static final String CMD_ZEROPAY_REQ = "Z10";  //제로페이 전문 요청
    public static final String CMD_ZEROPAY_CANCEL_REQ = "Z20";  //제로페이 전문 취소 요청
    public static final String CMD_ZEROPAY_SEARCH_REQ = "Z30";  //제로페이 전문 취소조회 요청(취소 거래 이며 응답 코드 "0100" 수신 시 응답 메세지"처리결과 확인 필요" 문구 포스 디스플레이 후 가맹점 사용자 확인 후 취소 조회 업무 진행하여 정상 취소 처 리 여부 확인 필요)
    public static final String CMD_ZEROPAY_RES = "Z15"; //제로페이 전문 응답
    public static final String CMD_ZEROPAY_CANCEL_RES = "Z25"; //제로페이 전문 취소 응답
    public static final String CMD_ZEROPAY_SEARCH_RES = "Z35"; //제로페이 전문 취소조회 응답(취소 거래 이며 응답 코드 "0100" 수신 시 응답 메세지"처리결과 확인 필요" 문구 포스 디스플레이 후 가맹점 사용자 확인 후 취소 조회 업무 진행하여 정상 취소 처 리 여부 확인 필요)

    public static final String CMD_KAKAOPAY_REQ = "K21";  //카카오페이 전문 요청
    public static final String CMD_KAKAOPAY_CANCEL_REQ = "K22";  //카카오페이 전문 취소 요청
    public static final String CMD_KAKAOPAY_SEARCH_REQ = "K23";  //카카오페이 전문 승인조회 요청(카카오페이는 반드시 취소 전에 조회를 요청하고 결과를 받은 뒤에 해야 정상처리가 된다)
    public static final String CMD_KAKAOPAY_RES = "K26"; //카카오페이 전문 응답
    public static final String CMD_KAKAOPAY_CANCEL_RES = "K27"; //카카오페이 전문 취소 응답
    public static final String CMD_KAKAOPAY_SEARCH_RES = "K28"; //카카오페이 전문 승인조회 응답(카카오페이는 반드시 취소 전에 조회를 요청하고 결과를 받은 뒤에 해야 정상처리가 된다)


    /*---------------------------------------------------------------------------------------------
     //서버통신 관련 전문
    -----------------------------------------------------------------------------------------------*/
    /**
     * 통신망 조회(요청)
     * @param _Command 전문번호 3 A O "000" : 통신망 조회 요청
     * @param _Tid Terminal ID 10 A O 단말기 ID
     * @param _date 거래일시 12 N O YYMMDDhhmmss
     * @param _posVer 단말버전 단말 : 단말 version( 5자리)
     * @param _etc 단말추가정보 23 A O 미정
     * @return
     */

    public static byte[] TCP_CheckServerConnectStateReq(String _Command,String _Tid,String _date,String _posVer,String _etc)
    {

        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);

        if(_etc.equals("")){
//            byte[] bEtc = new byte[23];
//            for(int i=0;i<23;i++){bEtc[i]=(byte)0x20;}
//            b.Add(bEtc);
        } else {
            b.Add(_etc);
        }
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * TMS 단말기 데이타 다운로드 정보 요청
     * @param _Command
     * @param _Tid
     * @param _swVer
     * @param _serialNum
     * @param _dataType
     * @param _secKey
     * @return
     */
    public static byte[] TCP_TMSDownInfo(String _Command,String _Tid,String _swVer,byte[] _serialNum,String _dataType,byte[] _secKey)
    {

        KByteArray b = new KByteArray();
        b.Add("1"); //유무선구분 1:유선 2:무선 1만쓴다
        if (_Tid.equals(""))
        {
            byte[] tmp = new byte[10];
            for(int i=0;i<tmp.length;i++){tmp[i]=(byte)0x20;}
            b.Add(tmp);
        }
         else {
            b.Add(_Tid);
        }

        b.Add(_swVer);
        b.Add(_serialNum);
        b.Add(Command.FS);
        b.Add(_dataType);
        b.Add(Command.FS);
        b.Add(Command.FS);
        b.Add(Command.FS);
        b.Add(Command.FS);
        b.Add(Command.FS);
        String ConvertsignData = Utils.bytesToHex(_secKey);
        b.Add(ConvertsignData);
//        b.Add(_secKey);
        b.Add(Command.FS);
        return Utils.MakeTMSClientPacket(b.value(), _Command);
    }

    /**
     * "D10" : 가맹점다운로드  "D11" : 복수가맹점다운로드  "D20" : 키업데이트
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @param _length 128byte => 0128
     * @param _posCheckdata 길이(3byte, ex.087)+TrsmID(7)+Crandom(16)+KEY1_ENC(25바이 트Timpstamp+가변SvrInfo+7바이트TID+16바이트CRandom)
     * @param _businessnum 가맹점 사업자번호
     * @param _posSerialnum 장비 제조일련번호(Serial)
     * @param _posData 가맹점데이터 64 A * *
     * @param _macAddress 맥어드레스 15
     * @return
     */
    public static byte[] TCP_StoreDownloadKeyReq(String _Command,String _Tid,String _date,String _posVer,String _etc,String _length,
                                                 byte[] _posCheckdata,String _businessnum,String _posSerialnum,String _posData, String _macAddress)
    {

        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);

        if(_etc.equals("")){
//            byte[] bEtc = new byte[23];
//            for(int i=0;i<23;i++){bEtc[i]=(byte)0x20;}
//            b.Add(bEtc);
        } else {
            b.Add(_etc);
        }
        b.Add(Command.FS);
        b.Add(_length);
        if(_posCheckdata.equals(""))    //단말 검증 요청 데이터는 Binary 타입
        {
            byte[] tmp = new byte[Integer.valueOf(_length)];
            for(int i=0;i<tmp.length;i++){tmp[i]=(byte)0x00;}
            b.Add(tmp);
        }
        else {
            b.Add(_posCheckdata);
        }
        b.Add(Command.FS);
        if(!_Command.equals("D20")) // D20의 경우에는 사업자 번호를 넣지 않는다.
        {
            b.Add(_businessnum);
        }
        b.Add(Command.FS);
        b.Add(_posSerialnum);
        b.Add(Command.FS);
        if(_posData.equals(""))
        {
            byte[] tmp = new byte[64];
            for(int i=0;i<tmp.length;i++){tmp[i]=(byte)0x20;}
            b.Add(tmp);
        }
        else
        {
            b.Add(_posData);
        }

        b.Add(Command.FS);
        if(!_Command.equals("D20")) // D20의 경우에는 UUID 주소를 넣지않는다
        {
            b.Add(_macAddress);
        }
//        b.Add(_macAddress);

        return Utils.MakeClientPacket(b.value());
    }

    /**
     *
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @param _cancelInfo
     * @param _inputType
     * @param _CardNumber
     * @param _encryptInfo
     * @param _money
     * @param _tax
     * @param _serviceCharge
     * @param _taxfree
     * @param _currency
     * @param _Installment
     * @param _PoscertificationNumber
     * @param _tradeType
     * @param _emvData
     * @param _fallbackreason
     * @param _ICreqData
     * @param _workingKeyIndex
     * @param _passwd
     * @param _oilsurpport
     * @param _texfreeoil
     * @param _Dccflag
     * @param _DccreqInfo
     * @param _pointCardCode
     * @param _ptCardNum
     * @param _ptCardEncprytInfo
     * @param _digSignInfo
     * @param _signPadSerial
     * @param _digSignData
     * @param _Certification
     * @param _posData
     * @param _kocesUid
     * @param _macAddress
     * @param _hardwareKey
     * @return
     */
    public static byte[] TCP_ICReq(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputType,String _CardNumber,byte[] _encryptInfo,
                                   String _money, String _tax,String _serviceCharge,String _taxfree,String _currency,String _Installment,String _PoscertificationNumber,String _tradeType,
                                   String _emvData,String _fallbackreason,byte[] _ICreqData,String _workingKeyIndex,String _passwd,String _oilsurpport,String _texfreeoil,
                                   String _Dccflag,String _DccreqInfo,String _pointCardCode, String _ptCardNum,byte[] _ptCardEncprytInfo,String _digSignInfo,
                                   String _signPadSerial,byte[] _digSignData,String _Certification,String _posData,String _kocesUid,String _uniqueCode,
                                   String _macAddress, String _hardwareKey)
    {

        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);

        //단말추가정보 미설정 시, 데이터 미설정(20.05.20)
        if(_etc.equals("")){
         //   byte[] bEtc = new byte[23];
         //   for(int i=0;i<23;i++){bEtc[i]=(byte)0x20;}
         //   b.Add(bEtc);
        } else {
            b.Add(_etc);
        }
        b.Add(Command.FS);

        if(!_cancelInfo.equals("")){
            b.Add(_cancelInfo);
        }

        b.Add(Command.FS);
        b.Add(_inputType);
        b.Add(Command.FS);
        b.Add(_CardNumber);
        b.Add(Command.FS);

        if(_encryptInfo != null)
        {
            b.Add(Utils.intTo4AsciiArray(_encryptInfo.length));
            b.Add(_encryptInfo);
        }
        else
        {
            b.Add("0000");
        }

        b.Add(Command.FS);
        b.Add(_money);
        b.Add(Command.FS);

        if(!_tax.equals("") || !_tax.equals("0"))
        {
            b.Add(_tax);
        } //세금이 0이 아니면
        b.Add(Command.FS);
        if(!_serviceCharge.equals("") || !_serviceCharge.equals("0"))
        {
            b.Add(_serviceCharge);
        } //봉사료가  0이 아니면
        b.Add(Command.FS);
        if(!_taxfree.equals("") || !_taxfree.equals("0"))
        {
            b.Add(_taxfree);
        } //비과세가 0이 아니면
        b.Add(Command.FS);

        if(!_currency.equals("") || !_currency.equals("410"))
        {
            b.Add(_currency);
        } //통화코드가 410이 아니면
        b.Add(Command.FS);

        if(_Installment.equals("") || _Installment.equals("0") || _Installment.equals("1") || _Installment.equals("00") || _Installment.equals("01"))
        {

        }
        else
        {
            if(_Installment.length()==1)
            {
                b.Add("0" + _Installment);
            }
            else
            {
                b.Add(_Installment);
            }
        }//할부개월이 없으면

        b.Add(Command.FS);
        b.Add(_PoscertificationNumber);
        b.Add(Command.FS);

        if(!_tradeType.equals(""))
        {
            b.Add(_tradeType);
        } //전화승인or 은련핫키입력일경우
        b.Add(Command.FS);

        b.Add(_emvData);
        b.Add(Command.FS);

        //fallback거래 아닐경우 폴백 사유 미설정, 스페이스패딩 하지 말 것(20.05.23)
        if(!_fallbackreason.equals(""))
        {
            b.Add(_fallbackreason);
        }

        b.Add(Command.FS);
        //
        if(_ICreqData != null)
        {
            b.Add(Utils.intTo4AsciiArray(_ICreqData.length));
            b.Add(_ICreqData);
        }
        else
        {
            b.Add("0000");
        }

        b.Add(Command.FS);
        b.Add(_workingKeyIndex);
        b.Add(Command.FS);
        b.Add(_passwd);
        b.Add(Command.FS);
        if(!_oilsurpport.equals(""))
        {
            b.Add(_oilsurpport);
        }//유류지원정보없을경우
        b.Add(Command.FS);

        if(!_texfreeoil.equals(""))
        {
            b.Add(_texfreeoil);
        }//면세유정보없을경우
        b.Add(Command.FS);
        if(!_Dccflag.equals(""))
        {
            b.Add(_Dccflag);
        }
        b.Add(Command.FS);

        if(!_DccreqInfo.equals("")){ b.Add(_DccreqInfo); }

        b.Add(Command.FS);
        if(!_pointCardCode.equals(""))
        {
            b.Add(_pointCardCode);
        }
        b.Add(Command.FS);
        if(!_ptCardNum.equals(""))
        {
            b.Add(_ptCardNum);
        }
        b.Add(Command.FS);

        if(_ptCardEncprytInfo != null) {
            b.Add(Utils.intTo4AsciiArray(_ptCardEncprytInfo.length));
            b.Add(_ptCardEncprytInfo);
        }
        else
        {
            b.Add("0000");
        }
        b.Add(Command.FS);
        b.Add(_digSignInfo);
        b.Add(Command.FS);

        if(!_signPadSerial.equals("")){ b.Add(_signPadSerial); }
        b.Add(Command.FS);

        if(_digSignData != null && _digSignData.length>0)
        {
            /* 장치 정보를 읽어서 설정 하는 함수         */
            String deviceType = Setting.getPreference(Setting.getTopContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
            if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
                Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
            }else
            {

                Setting.g_PayDeviceType = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            }
            switch (Setting.g_PayDeviceType) {
                case NONE:
                    //아무 장비도 연결되지 않았는데 여기로 올 일이 없다.
                    b.Add(Utils.intTo4AsciiArray(_digSignData.length));
                    b.Add(_digSignData);
                    break;
                case BLE:       //BLE의 경우
                    if(_digSignData.length > 2000)
                    {

                        byte[] blen = Utils.intTo4AsciiArray(_digSignData.length);
                        b.Add(blen);
                        b.Add(_digSignData);
                    }
                    else {
                        String ConvertsignData = Utils.bytesToHex_0xType_nospace(_digSignData);
                        int dataSize = ConvertsignData.length();
                        byte[] blen = Utils.intTo4AsciiArray(ConvertsignData.length());
                        b.Add(blen);
                        b.Add(ConvertsignData);
                    }

                    break;
                case CAT:       //WIFI CAT의 경우
                    //캣의 경우는 이곳에 올 일이 없다.
                    b.Add(Utils.intTo4AsciiArray(_digSignData.length));
                    b.Add(_digSignData);
                    break;
                case LINES:     //유선장치의 경우
                    //아래쪽으로 그대로 진행한다.
                    //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
                    switch (Setting.getSignPadType(Setting.getTopContext()))
                    {
                        case 1:
                        case 2:
                            b.Add(Utils.intTo4AsciiArray(_digSignData.length));
                            b.Add(_digSignData);
                            break;

                        case 3:

                            if(_digSignData.length > 2000)
                            {

                                byte[] blen = Utils.intTo4AsciiArray(_digSignData.length);
                                b.Add(blen);
                                b.Add(_digSignData);
                            }
                            else {
                                String ConvertsignData = Utils.bytesToHex_0xType_nospace(_digSignData);
                                int dataSize = ConvertsignData.length();
                                byte[] blen = Utils.intTo4AsciiArray(ConvertsignData.length());
                                b.Add(blen);
                                b.Add(ConvertsignData);
                            }
                            break;
                    }
                    break;
                default:
                    b.Add(Utils.intTo4AsciiArray(_digSignData.length));
                    b.Add(_digSignData);
                    break;
            }

//            if(_digSignInfo.equals("B") || _digSignInfo.equals("6"))
//            {
//                //wofhddl20200628   //사인데이터가 BM 데이터 인 경우에는 byte를 hex로 변환 하여 데이터를 전송 한다.
//                if (_digSignData.length == 2172) {
//                    byte[] blen = Utils.intTo4AsciiArray(_digSignData.length);
//                    b.Add(blen);
//                    b.Add(_digSignData);
//                } else {
//                    String ConvertsignData = Utils.bytesToHex_0xType_nospace(_digSignData);
//                    int dataSize = ConvertsignData.length();
//                    byte[] blen = Utils.intTo4AsciiArray(ConvertsignData.length());
//                    b.Add(blen);
//                    b.Add(ConvertsignData);
//                }
//            }
//            else
//            {
//                b.Add(Utils.intTo4AsciiArray(_digSignData.length));
//                b.Add(_digSignData);
//            }
        }
        else
        {
            b.Add("0000");
        }

        b.Add(Command.FS);

        if(!_Certification.equals("")){ b.Add(_Certification);  }

        b.Add(Command.FS);

        if(!_posData.equals("")){ b.Add(_posData); }

        b.Add(Command.FS);

        if(!_cancelInfo.equals(""))
        {
            b.Add(_kocesUid);
        }
        b.Add(Command.FS);

        //접속업체코드 미설정시 데이터 미설정
        if(_uniqueCode.equals(""))
        {
         //   byte[] bunique = new byte[3];
         //   for(int i=0;i<3;i++){bunique[i]=(byte)0x20;}
         //   b.Add((bunique));
        }
        else
        {
            b.Add(_uniqueCode);
        }

        b.Add(Command.FS);
        b.Add(_macAddress);

        b.Add(Command.FS);
        b.Add(_hardwareKey);

        return Utils.MakeClientPacket(b.value());
    }

    public static byte[] TCP_KAKAOReq(String 전문번호,String _Tid,String 거래일시,String 단말버전,String 단말추가정보,String 취소정보,String 입력방법,
                                      String 바코드번호, byte[] OTC카드번호,String 거래금액,String 세금,String 봉사료,String 비과세,String 통화코드,
                                      String 할부개월, String 결제수단, String 취소종류, String 취소타입, String 점포코드,String _PEM,String _trid,
                                      String 카드BIN, String 조회고유번호, String _WorkingKeyIndex, String 전자서명사용여부,String 사인패드시리얼번호,
                                      byte[] 전자서명데이터,String 가맹점데이터)
    {
        KByteArray b = new KByteArray();
        b.Add(전문번호);
        b.Add(_Tid);
        b.Add(거래일시);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(단말버전);
        if (단말추가정보 != null && !단말추가정보.equals(""))
        {
            b.Add(단말추가정보);
        }
        b.Add(Command.FS);

        if (전문번호.equals("K22"))
        {
            b.Add(취소정보);
        }
        b.Add(Command.FS);
        b.Add(입력방법);
        b.Add(Command.FS);
        b.Add(바코드번호);
        b.Add(Command.FS);
        if (전문번호.equals("K21") && OTC카드번호 != null &&
                OTC카드번호.length > 1 && OTC카드번호[0] != (byte)0x00 && OTC카드번호[0] != (byte)0x48)
        {
            b.Add(OTC카드번호);
        }
        b.Add(Command.FS);
        b.Add(거래금액);
        b.Add(Command.FS);

        if (세금 != null && !세금.equals("") && !세금.equals("0"))
        {
            b.Add(세금);
        }
        b.Add(Command.FS);
        if (봉사료 != null && !봉사료.equals("") && !봉사료.equals("0"))
        {
            b.Add(봉사료);
        }
        b.Add(Command.FS);
        if (비과세 != null && !비과세.equals("") && !비과세.equals("0"))
        {
            b.Add(비과세);
        }
        b.Add(Command.FS);
        if (통화코드 != null && !통화코드.equals("") && !통화코드.equals("410"))
        {
            b.Add(통화코드);
        }
        b.Add(Command.FS);
        if (할부개월 != null && !할부개월.equals("") && !할부개월.equals("0") &&
                !할부개월.equals("00") && !할부개월.equals("1") && !할부개월.equals("01"))
        {
            b.Add(할부개월.length() == 1 ? "0"+할부개월:할부개월);
        }
        b.Add(Command.FS);

        if (전문번호.equals("K21")) {
            b.Add(결제수단);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K22") && !취소종류.equals("")) {
            b.Add(취소종류);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K22") && !취소타입.equals("")) {
            b.Add(취소타입);
        }
        b.Add(Command.FS);
        if (!점포코드.equals("")) {
            b.Add(점포코드);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") && !_PEM.equals(""))
        {
            b.Add(_PEM);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") && !_trid.equals("")) {
            b.Add(_trid);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") && !카드BIN.equals("")) {
            b.Add(카드BIN);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") || 전문번호.equals("K22") && !조회고유번호.equals("")) {
            b.Add(조회고유번호);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") || 전문번호.equals("K22") && !_WorkingKeyIndex.equals("")) {
            b.Add(_WorkingKeyIndex);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") || 전문번호.equals("K22") && !전자서명사용여부.equals("")) {
            b.Add(전자서명사용여부);
        }
        b.Add(Command.FS);
        if (전문번호.equals("K21") || 전문번호.equals("K22") && !사인패드시리얼번호.equals("")) {
            b.Add(사인패드시리얼번호);
        }
        b.Add(Command.FS);
        if (전자서명데이터 != null && 전자서명데이터.length > 0) {
            b.Add(Utils.intTo4AsciiArray(전자서명데이터.length));
            b.Add(전자서명데이터);
        }
        else {
            b.Add("0000");
        }
        b.Add(Command.FS);
        if (가맹점데이터 != null && !가맹점데이터.equals("")) {
            b.Add(가맹점데이터);
        }

        return Utils.MakeClientPacket(b.value());
    }
    ///제로페이 서버요청함수
    public static byte[] TCP_ZEROReq(String 전문번호,String _Tid,String 거래일시,String 단말버전,String 단말추가정보,String 취소정보,String 입력방법,
                                     String 원거래일자, String 원승인번호, String 바코드번호, String 거래금액,String 세금,String 봉사료,String 비과세,
                                     String 통화코드, String 할부개월, String 가맹점추가정보, String 가맹점데이터, String KOCES거래고유번호)
    {
        KByteArray b = new KByteArray();
        b.Add(전문번호);
        b.Add(_Tid);
        b.Add(거래일시);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(단말버전);
        if (단말추가정보 != null && !단말추가정보.equals(""))
        {
            b.Add(단말추가정보);
        }
        b.Add(Command.FS);

        if (전문번호.equals("Z20"))
        {
            b.Add(취소정보);
        }
        b.Add(Command.FS);
        if (전문번호.equals("Z10"))
        {
            b.Add(입력방법);
        }
        b.Add(Command.FS);
        if (전문번호.equals("Z20") || 전문번호.equals("Z30"))
        {
            b.Add(원거래일자);
        }
        b.Add(Command.FS);
        if (전문번호.equals("Z20") || 전문번호.equals("Z30"))
        {
            b.Add(원승인번호);
        }
        b.Add(Command.FS);
        if (전문번호.equals("Z10"))
        {
            b.Add(바코드번호);
        }
        b.Add(Command.FS);
        b.Add(거래금액);
        b.Add(Command.FS);

        if (세금 != null && !세금.equals("") && !세금.equals("0"))
        {
            b.Add(세금);
        }
        b.Add(Command.FS);
        if (봉사료 != null && !봉사료.equals("") && !봉사료.equals("0"))
        {
            b.Add(봉사료);
        }
        b.Add(Command.FS);
        if (비과세 != null && !비과세.equals("") && !비과세.equals("0"))
        {
            b.Add(비과세);
        }
        b.Add(Command.FS);
        if (통화코드 != null && !통화코드.equals("") && !통화코드.equals("410"))
        {
            b.Add(통화코드);
        }
        b.Add(Command.FS);
        if (할부개월 != null && !할부개월.equals("") && !할부개월.equals("0") &&
                !할부개월.equals("00") && !할부개월.equals("1") && !할부개월.equals("01"))
        {
            b.Add(할부개월.length() == 1 ? "0"+할부개월:할부개월);
        }
        b.Add(Command.FS);

        if (가맹점추가정보 != null && !가맹점추가정보.equals("")) {
            b.Add(가맹점추가정보);
        }
        b.Add(Command.FS);
        if (가맹점데이터 != null && !가맹점데이터.equals("")) {
            b.Add(가맹점데이터);
        }
        b.Add(Command.FS);
        if (KOCES거래고유번호 != null && !KOCES거래고유번호.equals("")) {
            b.Add(KOCES거래고유번호);
        }

        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 현금 영수증
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @param _cancelInfo
     * @param _inputMethod
     * @param _id
     * @param _idencrpyt
     * @param _money
     * @param _tax
     * @param _serviceCharge
     * @param _taxfree
     * @param _privateOrCorp
     * @param _cancelReson
     * @param _pointCardCode
     * @param _pointAcceptNum
     * @param _businessData
     * @param _bangi
     * @return
     */
    public static byte[] cashReceipt(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputMethod,byte[] _id,byte[] _idencrpyt,String _money,String _tax,String _serviceCharge,
                                     String _taxfree,String _privateOrCorp,String _cancelReson,String _pointCardCode,String _pointAcceptNum,String _businessData,String _bangi, String _kocesNumber)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);

        //단말추가정보 미설정시 데이터 미설정(20.05.23)
        if(_etc.equals("")){
         //   byte[] bEtc = new byte[23];
         //   for(int i=0;i<23;i++){bEtc[i]=(byte)0x20;}
         //   b.Add(bEtc);
        } else {
            b.Add(_etc);
        }
        b.Add(Command.FS);
        if(_Command.equals("B10"))  //현금 영수증 등록 요청
        {
        }
        else if(_Command.equals("B20")) //현금 영수증 등록 취소
        {
            b.Add(_cancelInfo);
        }
        b.Add(Command.FS);
        b.Add(_inputMethod);
        b.Add(Command.FS);
        if(_id!=null)
        {
            b.Add(_id);
        }
        b.Add(Command.FS);
        byte[] _tmp = new byte[58];
        for(int i=0;i<58;i++){_tmp[i]=(byte)0x30;}
        if(_idencrpyt!=null) {
            b.Add(Utils.intTo4AsciiArray(_idencrpyt.length));
            b.Add(_idencrpyt);
        }
        else
        {
            //22.02.24 kim.jy 이완재 차장님과 통화 후에 ksn 오류 난다고 통보하고 다시 주석 처리 함.
            //b.Add("0000");
        }
        b.Add(Command.FS);
        b.Add(_money);
        b.Add(Command.FS);
        if(!_tax.equals("")){ b.Add(_tax);  } //세금이 0이 아니면
        b.Add(Command.FS);
        if(!_serviceCharge.equals("")){ b.Add(_serviceCharge); } //봉사료가  0이 아니면
        b.Add(Command.FS);
        if(!_taxfree.equals("")){ b.Add(_taxfree); } //비과세가 0이 아니면
        b.Add(Command.FS);
        b.Add(_privateOrCorp);
        b.Add(Command.FS);
        b.Add(_cancelReson);
        b.Add(Command.FS);
        if(_pointCardCode != null && !_pointCardCode.equals("")){ b.Add(_pointCardCode); }
        b.Add(Command.FS);
        if(_pointAcceptNum != null && !_pointAcceptNum.equals("")){ b.Add(_pointAcceptNum);  }
        b.Add(Command.FS);
        if(_businessData != null && !_businessData.equals("")){ b.Add(_businessData); }
        b.Add(Command.FS);
        if(_bangi != null && !_bangi.equals("")){ b.Add(_bangi); }
        b.Add(Command.FS);
        if(_kocesNumber != null && !_kocesNumber.equals("")){b.Add(_kocesNumber);}
        return Utils.MakeClientPacket(b.value());


    }


    //TODO kim.jy 20200312 추가 작업 필요

    /**
     * CashIC
     * @param _Command
     * "C10" : 현금 IC 구매 요청
     * "C20" : 현금 IC 구매 취소(환불) 요청
     * "C30" : 현금 IC 잔액 조회 요청
     * "C40" : 현금 IC 구매결과 조회 요청
     * "C50" : 현금 IC 환불결과 조회 요청
     * "C60" : 난수업데이트 요청
     * @param _Tid 단밀기 ID
     * @param _date YYMMDDhhmmss
     * @param _posVer 단말 : 단말 version( 5자리)
     * @param _etc 미정
     * @param _cancelInfo 취소정보 20 字
     * 취소구분(1) + 원거래일자(6) + 원승인번호 (13)  - 취소구분(1)
     * '0' : 사용자 취소    '1' : 단말 EOT 수신 오류 망취소    '5' : 직전 취소    '6' : 현금 IC 망취소   'C' : 현금IC 구매/환불 결과조회 시 설 정  - 원거래일자(6) : YYMMDD - 원승인번호(13)
     * @param _inputMethod 입력방법
     * @param _Track3Data  '카드정보수록여부'가 '1' 일 경우 미 설정 18
     * @param _Issuer_info 발급기관정보
     * @param _Encrypt_info 암호화정보
     * @param _icCardNumber IC카드 일련 번호
     * @param _money 거래금액
     * @param _tax 세금
     * @param _serviceCharge 봉사료
     * @param _taxfree 비과세
     * @param _Simplified_Transaction 간소화거래여부
     * @param _includeCardInfo 카드정보수록여부
     * @param _StoreData 가맹점데이터
     * @return
     */
    public static byte[] CashIC(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputMethod,String _Track3Data,String _Issuer_info
                                ,String _Encrypt_info,String _icCardNumber,String _money,String _tax,String _serviceCharge,String _taxfree,String _Simplified_Transaction,String _includeCardInfo
                                ,String _StoreData)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);

        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 포인트단독
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @param _cancelInfo
     * @param _inputMethod
     * @param _PointCardNubmer
     * @param _Pointcard_encrypt_info
     * @param _money
     * @param _PointCompanyCode
     * @param _PaymentType
     * @param _WorkingKey_Index
     * @param _Passwd
     * @param _StoreData
     * @return
     */
    public static byte[] Point(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputMethod,String _PointCardNubmer,String _Pointcard_encrypt_info
                                ,String _money,String _PointCompanyCode,String _PaymentType,String _WorkingKey_Index,String _Passwd,String _StoreData)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 멤버십단독
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @param _cancelInfo
     * @param _inputMethod
     * @param _MemberShipCardNubmer
     * @param _Membershipcard_encrypt_info
     * @param _money
     * @param _productCode
     * @param _dongleInfo
     * @param _StoreData
     * @return
     */
    public static byte[] MemberShip(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputMethod,String _MemberShipCardNubmer,String _Membershipcard_encrypt_info
                                    ,String _money,String _productCode,String _dongleInfo,String _StoreData)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 수표 조회
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @return
     */
    public static byte[] Checkcheck(String _Command,String _Tid,String _date,String _posVer,String _etc)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 광고메시지다운로드
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @return
     */
    public static byte[] AdvertisingMessageDownload(String _Command,String _Tid,String _date,String _posVer,String _etc)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 정산
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @return
     */
    public static byte[] Settlement(String _Command,String _Tid,String _date,String _posVer,String _etc)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 온라인집계
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @return
     */
    public static byte[] Online_Aggregate_data(String _Command,String _Tid,String _date,String _posVer,String _etc)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

    /**
     * 대리점카드사정보조회
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @return
     */
    public static byte[] inquiry_chine_card_info(String _Command,String _Tid,String _date,String _posVer,String _etc)
    {
        KByteArray b = new KByteArray();
        b.Add(_Command);
        b.Add(_Tid);
        b.Add(_date);
        b.Add(Utils.getUniqueProtocolNumbering());
        b.Add(POS_TYPE);
        b.Add(_posVer);
        b.Add(_etc);
        return Utils.MakeClientPacket(b.value());
    }

}
