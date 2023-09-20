package com.koces.androidpos.sdk;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.koces.androidpos.sdk.Utils.intTo2ByteArray;
import static com.koces.androidpos.sdk.Utils.makeLRC;

import com.koces.androidpos.sdk.van.Constants;

public class Command {
    public static final int F_CONTROL_DAT_BYTE = 4;
    public static final int F_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA = 2; //전문포맷 중 길이로 산정되는 부분에서 DataValue를 제외한 부분의 합
    public static final int F_SIGNPADDATA_TOTAL_OVERROLL_SIZE = 6;
    public static final byte STX = (byte)0x02;
    public static final byte ETX = (byte)0x03;
    public static final byte FS = (byte)0x1C;

    public final static byte ENQ = (byte)0x05;
    public final static byte ACK = (byte)0x06;
    public final static byte NAK = (byte)0x15;
    public final static byte ESC = (byte)0x1B;
    public final static byte EOT = (byte)0x04;

    public static final int T_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA = 5; //서버 통신용 전문포맷 중 길이로 산정되는 부분에서 DataValue를 제외한 부분의 합
    public final static int T_CONTROL_DAT_BYTE=11;

    public final static String DEFAULT_VALUE = "0";
    public final static String ANGLE_VALUE = "1004";

    public final static String[] DEFINE_PAYMENT_PURPOSE_OF_USE = {"사용안함", "개인", "법인", "자진발급"};

    //DB 관련 설정 작업
    public final static String SQLiteDBName = "Integrity";
    public final static String DB_IntegrityTableName = "IntegrityRecord";
    public final static String DB_StoreTableName = "StoreRecord";
    //public final static String DB_TaxTableName = "TaxRecord";
    public final static String DB_TaxTableName = "TaxSetRecord";    //필드 추가 문제로 DB를 새로 생성 한다. 2022-02-06 kim.jy
    public final static String DB_TradeTableName = "Trade";     //거래내역 테이블 22-02-16 kim.jy
    public final static String DB_AppToAppTableName = "AppToAppRecord";   //앱투앱거래완료데이터 테이블 22-04-24 jiw

    //은련비밀번호
    public static final String Unionpay_Password = "111111";
    /**사인패드 사용 여부를 위한 정의*/
    public static final int USE_SIGNPAD_NONE = 0;
    /**사인패드 사용 여부를 위한 정의*/
    public static final int USE_SIGNPAD_ONLY_SIGN = 1;
    /**사인패드 사용 여부를 위한 정의*/
    public static final int USE_SIGNPAD_MULTI_PAD = 2;
    /**사인패드 사용 여부를 위한 정의*/
    public static final int USE_SIGNPAD_TOUCH_SIGN = 3;

    /**
     * 장치 자동 설정 관련 메시지
     * getResource를 사용 할 수 없다.
     */

    /** 디바이스 장치를 재스캔 중입니다. */
    public static final String MSG_START_RESCAN_DEVICE = "장치 인식 중입니다";
    /** 연결된 장치를 찾을 수 없습니다. */
    public static final String MSG_NO_SCAN_DEVICE = "연결된 장치를 찾을 수 없습니다";

    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_INIT = 3000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_RF_INIT = 5000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_POSINFO_REQ = 10000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_SIGN_REQ = 20000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_SIGN_REQ1 = 20000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_NO_ENCYPT_NUMBER_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_ENCYPT_NUMBER_REQ =30000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_SEND_MSG_REQ = 2000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_RF_TRADE_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_QR_SEND_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_BACCOUNT_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_CDC_SELECT_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_IC_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_UNION_IC_= 30000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_IC_RESULT_REQ =30000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_KEYUPDATE_READY_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static final int TMOUT_CMD_KEYUPDATE_REQ = 30000;
    /** 전문별 기본 타입아웃 */
    public static int TMOUT_CMD_UNIONPAY_PARASSWORD_REQ = 30000;

    /** 초기화  */

    public static final byte CMD_INIT = (byte)0xA0; //초기화 요청
    /** 서명 입력 요청 */
    public static final byte CMD_SIGN_REQ = (byte)0xA1; //서명 입력 요청
    /** 서명 입력 응답 */
    public static final byte CMD_SIGN_RES = (byte)0xB1; //서명 입력(실시간) 응답
    /** 서명 입력 요청2 */
    public static final byte CMD_SIGN_REQ1 = (byte)0xA7; //서명 입력 요청
    /** 서명 입력 응답2 */
    public static final byte CMD_SIGN_RES2 = (byte)0xB7; //서명 입력(실시간) 응답
    /** 서명 전송 요청 */
    public static final byte CMD_SIGN_SEND_REQ = (byte)0xA2; //서명 전송 요청
    /** 서명 전송 응답 */
    public static final byte CMD_SIGN_SEND_RES = (byte)0xB2; //서명 전송 응답
    /** 서명 종료 요청 */
    public static final byte CMD_SIGN_CANCEL_REQ = (byte)0xAC; //서명 취소(종료) 요청
    /** 서명 종료 응답 */
    public static final byte CMD_SIGN_CANCEL_RES = (byte)0xBC; //서명 취소(종료) 응답
    /** 암호화하지 않은 번호요청 */
    public static final byte CMD_NO_ENCYPT_NUMBER_REQ = (byte)0xA3; //암호화하지 않은 번호요청
    /** 암호화하지 않은 번호 응답 */
    public static final byte CMD_NO_ENCYPT_NUMBER_RES = (byte)0xB3; //암호화하지 않은 번호 응답
    /** 암호화된 비밀번호 요청 */
    public static final byte CMD_ENCYPT_NUMBER_REQ = (byte)0xA4; //암호화된 비밀번호 요청
    /** 암호화된 비밀번호 응답 */
    public static final byte CMD_ENCYPT_NUMBER_RES = (byte)0xB4; //암호화된 비밀번호 응답
    /** 메시지 전송 요청 */
    public static final byte CMD_SEND_MSG_REQ = (byte)0xA5; //메시지 전송 요청
    /** 메시지 전송 응답 */
    public static final byte CMD_SEND_MSG_RES = (byte)0xB5; //메시지 전송 응답
    /** 고객전표 출력여부 요청 */
    public static final byte CMD_CUSTOMER_CHIT_REQ = (byte)0xA6; //고객전표 출력여부 요청
    /** 고객전표 출력여부 응답 */
    public static final byte CMD_CUSTOMER_CHIT_RES = (byte)0xB6; //고객전표 출력여부 응답
    /** RF초기화 요청 */
    public static final byte CMD_RF_INIT = (byte)0xC0; // RF초기화 요청
    /** RF거래 요청 */
    public static final byte CMD_RF_TRADE_REQ = (byte)0xC1; //RF거래 요청
    /** RF거래 응답 */
    public static final byte CMD_RF_TRADE_RES = (byte)0xD1; //RF거래 응답
    /** QR전송 요청 */
    public static final byte CMD_QR_SEND_REQ = (byte)0xC2; //QR전송 요청
    /** QR전송 응답 */
    public static final byte CMD_RF_SEND_RES = (byte)0xD2; //QR전송 응답
    /** 계좌번호 표시 요청 */
    public static final byte CMD_BACCOUNT_REQ = (byte)0xC3; //계좌번호 표시 요청
    /** 계좌번호 표시 응답 */
    public static final byte CMD_BACCOUNT_RES = (byte)0xD3; //계좌번호 표시 응답
    /** 자국환(DCC) 선택 요청 */
    public static final byte CMD_CDC_SELECT_REQ = (byte)0xC4; //자국환(DCC) 선택 요청
    /** 자국환(DCC) 선택 응답 */
    public static final byte CMD_CDC_SELECT_RES = (byte)0xD4; //자국환(DCC) 선택 응답
    /** IC 거래 요청 */
    public static final byte CMD_IC_REQ  = (byte)0X10; //IC 거래 요청
    /** IC 응답 */
    public static final byte CMD_IC_RES  = (byte)0X11; //IC 응답
    /** 은련IC카드선택결과 */
    public static final byte CMD_UNIONPAY_IC_CARD_SELECT_REQ  = (byte)0X12; //은련IC카드선택결과
    /** 은련IC카드선택 */
    public static final byte CMD_UNION_IC_  = (byte)0x13; //은련IC카드선택
    /** IC 거래결과 요청 */
    public static final byte CMD_IC_RESULT_REQ  = (byte)0X20; //IC 거래결과 요청
    /** IC 거래결과 응답 */
    public static final byte CMD_IC_RESULT_RES = (byte)0X21; //IC 거래결과 응답
    /** 보안키갱신생성요청 */
    public static final byte CMD_KEYUPDATE_READY_REQ = (byte)0X30; //보안키갱신생성요청
    /** IC 응답 – 현금 IC */
    public static final byte CMD_CASHIC_RES  = (byte)0X31; //IC 응답 – 현금 IC
    /** 현금 IC 선택된 계좌 */
    public static final byte CMD_CASHIC_MULTIPLE_ACCOUNT_REQ  = (byte)0X32; //현금 IC 선택된 계좌
    /** 현금 IC 복수 계좌 정보 */
    public static final byte CMD_6  = (byte)0X33; //현금 IC 복수 계좌 정보
    /** 멀티패드 처리상태 알림 */
    public static final byte CMD_MULTIPAD_STATUS_RES  = (byte)0X34; //멀티패드 처리상태 알림
    /** 보안키갱신요청 */
    public static final byte CMD_KEYUPDATE_REQ  = (byte)0X40; //보안키갱신요청
    /** 보안키갱신생성응답 */
    public static final byte CMD_KEYUPDATE_READY_RES  = (byte)0X41; //보안키갱신생성응답
    /** 자체보호요청 */
    public static final byte CMD_SELFKEYCHECK_REQ  = (byte)0X50; //자체보호요청
    /** 자체보호응답 */
    public static final byte CMD_SELFKEYCHECK_RES  = (byte)0X51; //자체보호응답
    /** 상호인증 요청 */
    public static final byte CMD_MUTUAL_AUTHENTICATION_REQ = (byte)0X52; //상호인증 요청
    /** 상호인증 응답 */
    public static final byte CMD_MUTUAL_AUTHENTICATION_RES  = (byte)0X53; //상호인증 응답
    /** 상호 인증정보 결과 요청 */
    public static final byte CMD_MUTUAL_AUTHENTICATION_RESULT_REQ  = (byte)0X54; //상호 인증정보 결과 요청
    /** 상호 인증정보 결과 응답 */
    public static final byte CMD_MUTUAL_AUTHENTICATION_RESULT_RES  = (byte)0X55; //상호 인증정보 결과 응답
    /** 업데이트 파일전송 */
    public static final byte CMD_SEND_UPDATE_DATA_REQ  = (byte)0X56; //업데이트 파일전송
    /** 업데이트 결과 */
    public static final byte CMD_SEND_UPDATE_DATA_RES  = (byte)0X57; //업데이트 결과
    /** 단말기 정보요청 */
    public static final byte CMD_POSINFO_REQ  = (byte)0X58; //단말기 정보요청
    /** 단말기 정보응답 */
    public static final byte CMD_POSINFO_RES  = (byte)0X59; //단말기 정보응답
    /** TwoCard 요청 */
    public static final byte CMD_TWO_CARD_REQ  = (byte)0X14; //TwoCard 요청
    /** 은련 비밀번호 필요(요청) */
    public static final byte CMD_UNIONPAY_PARASSWORD_RES  = (byte)0X16; //은련 비밀번호 필요(요청)
    /** 은련 비밀번호필요 (응답) */
    public static final byte CMD_UNIONPAY_PARASSWORD_REQ  = (byte)0X17; //은련 비밀번호필요 (응답)
    /** 현금 IC 비밀번호(요청) */
    public static final byte CMD_CASHIC_PASSWARD_REQ  = (byte)0X18; //현금 IC 비밀번호(요청)
    /** 카드 상태 응답 */
    public static final byte CMD_IC_STATE_RES  = (byte)0X19; //카드 상태 응답
    /** 카드 넣기 요청 */
    public static final byte CMD_IC_INSERT_REQ  = (byte)0X22; //카드 넣기 요청
    /** 카드 빼기 요청 */
    public static final byte CMD_IC_REMOVE_REQ  = (byte)0X24; //카드 빼기 요청
    /** 카드 상태 요청 */
    public static final byte CMD_IC_STATE_REQ  = (byte)0X26; //카드 상태 요청
    /** hash암호화요청/응답 */
    public static final byte CMD_HASH_ENCYPT_REQ  = (byte)0x60; //hash암호화요청/응답
    /** hash검증요청/응답 */
    public static final byte CMD_CHECK_HASH_DATA_REQ  = (byte)0x61; //hash검증요청/응답
    /** 단말기 공장초기화 요청 */
    public static final byte CMD_RESET_FACTORY_REQ  = (byte)0x62; //단말기 공장초기화 요청
    /** 단말기 공장초기화 응답 */
    public static final byte CMD_RESET_FACTORY_RES  = (byte)0x63; //단말기 공장초기화 응답
    /** KeyIn 정보 암호화 요청 */
    public static final byte CMD_KEYIN_ENCYPT_REQ  = (byte)0x28; //KeyIn 정보 암호화 요청
    /** RF카드정보요청 */
    public static final byte CMD_RFCARD_INFO_REQ  = (byte)0x64; //RF카드정보요청
    /** RF카드정보응답 */
    public static final byte CMD_RFCARD_INFO_RES  = (byte)0x65; //RF카드정보응답
    /** 바코드 리딩 요청 */
    public static final byte CMD_BARCODE_REQ  = (byte)0x66; //바코드 리딩 요청
    /** 바코드 리딩 응답 */
    public static final byte CMD_BARCODE_RES  = (byte)0x67; //바코드 리딩 응답
    /** RF카드정보요청(암호화데이터 추가) */
    public static final byte CMD_RFCARD_INFO_ENCYPT_REQ  = (byte)0x67; //RF카드정보요청(암호화데이터 추가)
    /** RF카드정보응답(암호화데이터 추가) */
    public static final byte CMD_RFCARD_INFO_ENCYPT_RES = (byte)0x67; //RF카드정보응답(암호화데이터 추가)

    public static final byte CMD_30  = (byte)0xC5; //LSAM충전결과응답
    public static final byte CMD_31  = (byte)0xD5; //LSAM충전결과요청
    public static final byte CMD_32  = (byte)0xC6; //지불데이터요청(충전)
    public static final byte CMD_33  = (byte)0xD6; //지불데이터전송(충전)
    public static final byte CMD_34  = (byte)0xC7; //지불데이터요청(충전직전취소)
    public static final byte CMD_35  = (byte)0xD7; //지불데이터전송(충전직전취소)
    public static final byte CMD_36  = (byte)0xC8; //LSAM 잔액조회 요청
    public static final byte CMD_37  = (byte)0xD8; //LSAM 잔액조회 응답

    public static final byte CMD_38  = (byte)0XE1; //설치요청전문
    public static final byte CMD_39  = (byte)0XF1; //단말기설치요청전문
    public static final byte CMD_40  = (byte)0X1F; //단말기설치응답전문

    public static final byte CMD_41  = (byte)0XE2; //발행사정보요청
    public static final byte CMD_42  = (byte)0XF2; //발행사정보파일요청
    public static final byte CMD_43  = (byte)0X2F; //발행사정보파일응답

    public static final byte CMD_44  = (byte)0XE3; //발행사별한도금액요청
    public static final byte CMD_45  = (byte)0XF3; //발행사별한도금액요청
    public static final byte CMD_46  = (byte)0X3F; // 발행사별한도금액응답

    public static final byte CMD_47  = (byte)0XE4; // 발행사별충전가능금액설정요청
    public static final byte CMD_48  = (byte)0XF4; //발행사별충전가능금액설정요청
    public static final byte CMD_49  = (byte)0X4F; //발행사별충전가능금액설정응답0

    public static final byte CMD_50  = (byte)0XE5;// 지불데이터요청(지불)
    public static final byte CMD_51  = (byte)0XE6; //충전카드contact요청
    public static final byte CMD_52  = (byte)0XF5; //지불데이터전송(지불)
    public static final byte CMD_53  = (byte)0XF6; // 지불취소/충전 요청
    public static final byte CMD_54  = (byte)0X6F; //지불취소/충전 응답
    public static final byte CMD_55  = (byte)0XF7; //취소/충전결과완료요청(성공)
    public static final byte CMD_56  = (byte)0XE7; //직전거래요청
    public static final byte CMD_57  = (byte)0X7E; //직전거래응답
    public static final byte CMD_58  = (byte)0XE8; //잔액조회요청
    public static final byte CMD_59  = (byte)0XF8; //잔액조회응답
    public static final byte CMD_60  = (byte)0XE9; //LSAM충전요청
    public static final byte CMD_61  = (byte)0XF9; //LSAM충전요청
    public static final byte CMD_62  = (byte)0X9F; //LSAM충전응답
    public static final byte CMD_63  = (byte)0XF0; //취소/충전결과완료요청(실패)
    public static final byte CMD_64  = (byte)0XE0; //SAM인식오류

    public static final byte  CMD_PRINT_REQ = (byte)0xC6; //출력 요청
    public static final byte  CMD_PRINT_RES = (byte)0xC6; //출력 응답
    public static final byte  CMD_PRINT_ERROR30 = (byte)0x30; //출력 응답
    public static final byte  CMD_PRINT_ERROR31 = (byte)0x31; //출력 응답
    public static final byte  CMD_PRINT_ERROR32 = (byte)0x32; //출력 응답

    /** ble 페어링 종료 후 전원유지시간 */
    public static final byte CMD_BLE_POWER_MANAGER_REQ = (byte)0x4F; //BLE 전원연결유지시간 요청
    public static final byte CMD_BLE_POWER_MANAGER_RES = (byte)0x4F; //BLE 전원연결유지시간 응답
    public static final byte  CMD_BLE_POWER_ALL = (byte)0x00; //ble 페어링 종료 후 전원유지시간 항상
    public static final byte  CMD_BLE_POWER_05 = (byte)0x01; //ble 페어링 종료 후 전원유지시간 항상
    public static final byte  CMD_BLE_POWER_10 = (byte)0x02; //ble 페어링 종료 후 전원유지시간 항상
    public static final byte  CMD_BLE_POWER_15 = (byte)0x03; //ble 페어링 종료 후 전원유지시간 항상
    public static final byte  CMD_BLE_POWER_20 = (byte)0x04; //ble 페어링 종료 후 전원유지시간 항상

    public static final byte TYPEDEFINE_ICCARDREADER = 1;
    public static final byte TYPEDEFINE_SIGNPAD = 2;
    public static final byte TYPEDEFINE_MULTIPAD = 3;
    public static final byte TYPEDEFINE_ICMULTIREADER = 4;
    public static final byte TYPEDEFINE_CAT= 5;
    public static final byte TYPEDEFINE_BLE = 6;
    public static final byte TYPEDEFINE_SCANMODE = 10;

    /** 전문 구분 및 상황 */
    public static final int PROTOCOL_NORMAL = 0;
    /** 전문 구분 및 상황 */
    public static final int PROTOCOL_SIGNDATA = 1;
    /** 전문 구분 및 상황 */
    public static final int PROTOCOL_TIMEOUT = -1;

    /** 서명패드 설정 값 */
    public static final int SIGNPAD_NONE = 0;
    /** 서명패드 설정 값 */
    public static final int SIGNPAD_SIGN = 1;
    /** 서명패드 설정 값 */
    public static final int SIGNPAD_MULIT = 2;
    /** 서명패드 설정 값 */
    public static final int SIGNPAD_TOUCH = 3;

    /** 카드리더기 설정 값 */
    public static final int CARD_READER = 0;
    /** 카드리더기 설정 값 */
    public static final int MULTI_READER = 1;

    /**
     CAT 통신 커멘드
     */

    public static final byte CMD_CAT_AUTH = (byte)0x47;     //(Auth CMD “G”)
    public static final byte CMD_CAT_EASY_AUTH = (byte)0x54;     //(Auth CMD “T”)

    /// 신용승인&*취소 DCC
    public static final byte[] CMD_CAT_CREDIT_REQ = {(byte)0x47,(byte)0x31,(byte)0x32,(byte)0x30};    //"G120"
    /// 현금승인&취소
    public static final byte[] CMD_CAT_CASH_REQ = {(byte)0x47,(byte)0x31,(byte)0x33,(byte)0x30};  // "G130"
    /// 은련승인&취소
    public static final byte[] CMD_CAT_UNIPAY_REQ = {(byte)0x47,(byte)0x31,(byte)0x34,(byte)0x30};  // "G140"
    /// 간편결제
    public static final byte[] CMD_CAT_APPPAY_REQ = {(byte)0x54,(byte)0x31,(byte)0x38,(byte)0x30};  // "T180"
    /// DCC 응답
    public static final byte[] CMD_CAT_DCC_REQ = {(byte)0x47,(byte)0x31,(byte)0x36,(byte)0x30};  // "G160"
    /// 현금 IC
    public static final byte[] CMD_CAT_CASHIC_REQ = {(byte)0x47,(byte)0x31,(byte)0x37,(byte)0x30};  // "G170"

    ///통신 전문 거래응답 신용승인 & 취소
    public static final String CMD_CAT_CREDIT_RES = "G125";
    ///통신 전문 거래응답 현금승인&취소
    public static final String CMD_CAT_CASH_RES = "G135";
    ///통신 전문 거래응답 은련승인&취소
    public static final String CMD_CAT_UNIPAY_RES = "G145";
    ///통신 전문 거래응답 앱카드
    public static final String CMD_CAT_APPPAY_RES = "T185";
    ///통신 전문 거래응답 DCC 응답
    public static final String CMD_CAT_DCC_RES = "G165";
    ///통신 전문 거래응답 현금 IC
    public static final String CMD_CAT_CASHIC_RES = "G175";

    /// 통신확인 TR COMMAND
    public static final byte[] CMD_CAT_TRCHECK_REQ = {(byte)0x41,(byte)0x31,(byte)0x31,(byte)0x30};  //  "A110"  //거래 내역 있을 때
    public static final byte[] CMD_CAT_TRCHECK_REQ2 = {(byte)0x41,(byte)0x31,(byte)0x31,(byte)0x35}; // "A115" //거래 내역 없을 때
    ///미전송거래 요청 전문
    public static final byte[] CMD_CAT_NOTRANS_TRADE_REQ = {(byte)0x4D,(byte)0x31,(byte)0x31,(byte)0x30}; // "M110" //미수신 거래)

    /* 현금IC업무 구분 */
    /// 현금 IC 구매 요청
    public static final String CMD_CAT_CIC_BUY_REQ = "C10";
    /// 현금 IC 구매 응답
    public static final String CAT_CASHIC_BUY_RES = "C15";
    public static final String CAT_CASHIC_CANCEL_REQ = "C20";
    /// 현금 IC 구매 취소(환불) 응답
    public static final String CAT_CASHIC_CANCEL_RES = "C25";
    public static final String CAT_CASHIC_BALANCE_REQ = "C30";
    /// 현금 IC 잔액 조회 응답
    public static final String CAT_CASHIC_BALANCE_RES = "C35";
    public static final String CAT_CASHIC_RESULT_REQ = "C40";
    /// 현금 IC 구매결과 조회 응답
    public static final String CAT_CASHIC_RESULT_RES = "C45";
    public static final String CAT_CASHIC_CANCEL_RESULT_REQ = "C50";
    /// 현금 IC 환불결과 조회 응답
    public static final String CAT_CASHIC_CANCEL_RESULT_RES = "C55";

    /**
     * 단말기 정보 요청 장비정보
     * @return
     */
    public static byte[] pos_info(String _date)
    {
        byte[] b = _date.getBytes();
        return Utils.MakePacket(CMD_POSINFO_REQ,b);
    }

    /** 단말기 초기화
     * 일반 서명패드, 멀티패드: 99
     * 기존 통합동글용: 13
     * COSTCO 서명패드: 88
     * @param _vanCode
     * @return
     */
    public static byte[] pad_init(String _vanCode)
    {
        byte[]b = _vanCode.getBytes();
        return Utils.MakePacket(CMD_INIT,b);
    }

    /**
     *서명 입력 요청
     * @param _createHashCode 해쉬코드 생성 여부
     * @param _SignatureDataSubstitutionMethod 서명데이터 치환방식
     * @param _workingKey 암호화 및 해쉬코드 생성시 이용하는 Working Key
     * @param _msg1 사인패드에서 화면이 눌려질 때 까지 사인패드 첫번째 라인에 표시할  메시지 (예 : ‚금액 : 50,000원‛)
     * @param _msg2 사인패드에서 화면이 눌려질 때 까지 사인패드 두번째 라인에 표시할 메시지 (예 : ‚서명하세요‛)
     * @param _msg3
     * @param _msg4
     * @return
     */
    public static byte[] inputsignreq(String _createHashCode,String _SignatureDataSubstitutionMethod,String _workingKey,String _msg1,String _msg2,String _msg3, String _msg4)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_createHashCode);
        bArray.Add(_SignatureDataSubstitutionMethod);
        bArray.Add(_workingKey);
        if(_msg1.length()>0)
        {
            try {
                for(int i=_msg1.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg1+= " ";
                }
                bArray.Add(_msg1.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        if(_msg2.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg2.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg2+= " ";
                }
                bArray.Add(_msg2.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        if(_msg3.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg3.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg3+= " ";
                }
                bArray.Add(_msg3.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        if(_msg4.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg4.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg4+= " ";
                }
                bArray.Add(_msg4.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        return Utils.MakePacket(CMD_SIGN_REQ,bArray.value());
    }

    /**
     *  서명 전송 요청 및 응답
     * @return
     */
    public static byte[] sendsginreq()
    {
        return Utils.MakePacket(CMD_SIGN_SEND_REQ ,null);
    }

    /**
     * 서명 종료 요청 및 응답
     * @return
     */
    public static byte[] finishsignreq()
    {
        return Utils.MakePacket(CMD_SIGN_CANCEL_REQ,null);
    }


    //핀패드

    /**
     * 암호화하지 않은 번호 요청
     * @param _flag 0 : 사용자가 입력한 값을 사인패드 화면에[*]로 표시
     *              * 1 : 사용자가 입력한 값을 사인패드 화면에 그대로 표시
     * @param _maxlength 사인패드에서 입력받을 최대길이   01 ~ 49 까지
     * @param _msg1 사인패드에서 키가 눌려질 때 까지 사인패드 화면에 표시할 메시지 1 (첫 번째 라인)
     * @param _msg2
     * @param _msg3 사인패드에서 단말기로 데이터를 보내고 사인패드 화면에 표시할 메시지 1 (첫 번째 라인)
     * @param _msg4
     * @param _displaytime
     * @return
     */
    public static byte[] pinpad_no_encypt_req(String _flag,String _maxlength,String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_flag);
        bArray.Add(_maxlength);
        if(_msg1!=null && _msg1.length()>0)
        {
            try {
                for(int i=_msg1.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg1+= " ";
                }
                bArray.Add(_msg1.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {
                ex.printStackTrace();
            }
        }

        if(_msg2!=null && _msg2.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg2.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg2+= " ";
                }
                bArray.Add(_msg2.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(_msg3!=null && _msg3.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg3.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg3+= " ";
                }
                bArray.Add(_msg3.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(_msg4!=null && _msg4.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg4.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg4+= " ";
                }
                bArray.Add(_msg4.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(FS);
        bArray.Add(_displaytime);

        return Utils.MakePacket(CMD_NO_ENCYPT_NUMBER_REQ,bArray.value());

    }

    /**
     *  암호화된 비밀번호 요청 및 응답 전문포맷
     * @param _cardNumber 카드번호  V19 Char Track 2 Data 16 ~ 19
     * @param _workKeyIndex Working Key Index   2 byte
     * @param _workKey Working Key         16 byte
     * @param minPasswd 입력 받을 최소 비밀번호 길이  (최소 : 01 )
     * @param maxPasswd 입력 받을 최대 비밀번호 길이  (최대 : 49 )
     * @param _msg1 사인패드에서 화면이 눌려질 때 까지 사인패드 첫번째 라인에 표시할  메시지 (예 : ‚금액 : 50,000원‛)
     * @param _msg2 사인패드에서 화면이 눌려질 때 까지 사인패드 두번째 라인에 표시할 메시지 (예 : ‚서명하세요‛)
     * @param _msg3 사인패드에서 단말기로 데이터를 보내고 사인패드 화면에 표시할 메시지 1 (첫 번째 라인)
     * @param _msg4 사인패드에서 단말기로 데이터를 보내고 사인패드 화면에 표시할 메시지 2 (두 번째 라인 )
     * @param _displaytime 메시지 3, 4 의 화면 표시 시간 ( 3초 )
     * @return
     */
    public static byte[] pinpad_encypt_req(String _cardNumber,String _workKeyIndex,String _workKey,String minPasswd,String maxPasswd,String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_cardNumber);
        bArray.Add(FS);
        bArray.Add(_workKeyIndex);
        bArray.Add(_workKey);
        bArray.Add(minPasswd);
        bArray.Add(maxPasswd);
        if(_msg1!=null && _msg1.length()>0)
        {
            try {
                for(int i=_msg1.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg1+= " ";
                }
                bArray.Add(_msg1.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(FS);
        if(_msg2!=null && _msg2.length()>0)
        {
            try {
                for(int i=_msg2.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg2+= " ";
                }
                bArray.Add(_msg2.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(FS);
        if(_msg3!=null && _msg3.length()>0)
        {
            try {
                for(int i=_msg3.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg3+= " ";
                }
                bArray.Add(_msg3.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(FS);
        if(_msg4!=null && _msg4.length()>0)
        {
            try {
                for(int i=_msg4.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg4+= " ";
                }
                bArray.Add(_msg4.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(FS);
        bArray.Add(_displaytime);
        return Utils.MakePacket(CMD_ENCYPT_NUMBER_REQ,bArray.value());
    }

    /**
     * 메시지 표시 요청
     * @param _msg1 메시지 1  V16 Char 사인패드 화면 첫 번째 라인 메시지
     * @param _msg2 메시지 2  V16 Char 사인패드 화면 두 번째 라인 메시지
     * @param _msg3 메시지 3  V16 Char 사인패드 화면 세 번째 라인 메시지
     * @param _msg4 메시지 4  V16 Char 사인패드 화면 네 번째 라인 메시지
     * @param _displaytime  화면 표시 유지 시간 3초 (01 ~ 99 초 까지 입력 가능)
     * @return
     */
    public static byte[] Message_Display_Req(String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime)
    {
        KByteArray bArray = new KByteArray();
        if(_msg1!=null && _msg1.length()>0)
        {
            try {
                for(int i=_msg1.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg1+= " ";
                }
                bArray.Add(_msg1.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(_msg2!=null && _msg2.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg2.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg2+= " ";
                }
                bArray.Add(_msg2.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(_msg3!=null && _msg3.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg3.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg3+= " ";
                }
                bArray.Add(_msg3.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(_msg4!=null && _msg4.length()>0)
        {
            bArray.Add(FS);
            try {
                for(int i=_msg4.getBytes("euc-kr").length; i<16; i++)
                {
                    _msg4+= " ";
                }
                bArray.Add(_msg4.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(FS);
        bArray.Add(_displaytime);

        return Utils.MakePacket(CMD_SEND_MSG_REQ,bArray.value());
    }

    public static byte[] customer_chit_print_req(String _msg1,String _msg2,String _msg3)
    {
        KByteArray bArray = new KByteArray();
        if(_msg1!=null && _msg1.length()>0)
        {
            bArray.Add(_msg1);
        }
        if(_msg2!=null && _msg2.length()>0)
        {
            bArray.Add(FS);
            bArray.Add(_msg2);
        }
        if(_msg3!=null && _msg3.length()>0)
        {
            bArray.Add(FS);
            bArray.Add(_msg3);
        }
        bArray.Add(FS);
        return Utils.MakePacket(CMD_CUSTOMER_CHIT_REQ,bArray.value());
    }

    /**
     * RF 초기화
     * 일반 서명패드, 멀티패드: 99
     * 기존 통합동글용: 13
     * COSTCO 서명패드: 88
     * @param _vanCode
     * @return
     */
    public static byte[] RF_init(String _vanCode)
    {
        byte[]b = _vanCode.getBytes();
        return Utils.MakePacket(CMD_RF_INIT,b);
    }

    /**
     *  RF 거래 요청
     * @param _date YYMMDDHHMMSS
     * @param _money 최대 12자리
     * @param _msg1 사인패드에서 카드를 접촉할 때 까지 사인패드 화면에
     * 표시할 메시지 1 (예 : ‚금액 : 50,000원‛)
     * @param _msg2 사인패드에서 카드를 접촉할 때 까지 사인패드 화면에
     * 표시할 메시지 2 (‚카드를 대주세요‛)
     * @param _msg3 사인패드에서 단말기로 데이터를 보내고 사인패드 화면에
     * 표시할 메시지 1 (첫 번째 라인)
     * @param _msg4 사인패드에서 단말기로 데이터를 보내고 사인패드 화면에
     * 표시할 메시지 2 (두 번째 라인)
     * @param _displaytime 메시지 3, 4 의 화면 표시 시간 ( 3초 )
     * @return
     */
    public static byte[] RF_trade_req(String _date,String _money,String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_date);
        bArray.Add(_money);
        if(_msg1!=null && _msg1.length()>0)
        {
            bArray.Add(FS);
            bArray.Add(_msg1);
        }
        if(_msg2!=null && _msg2.length()>0)
        {
            bArray.Add(FS);
            bArray.Add(_msg2);
        }
        if(_msg3!=null && _msg3.length()>0)
        {
            bArray.Add(FS);
            bArray.Add(_msg3);
        }
        if(_msg4!=null && _msg4.length()>0)
        {
            bArray.Add(FS);
            bArray.Add(_msg4);
        }
        bArray.Add(FS);
        bArray.Add(_displaytime);
        return Utils.MakePacket(CMD_RF_TRADE_REQ,bArray.value());
    }

    /**
     * QR코드 전송 요청
     * @param _displaytime Display 시간_최대99초
     * @param _qrbitmap 128*64 bmp 파일
     * @return
     */
    public static byte[] QR_trade_req(String _displaytime, Bitmap _qrbitmap)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_displaytime);
        bArray.Add(_qrbitmap.getRowBytes());
        return Utils.MakePacket(CMD_QR_SEND_REQ,bArray.value());
    }

    /** 현금IC카드 거래 전문
     * 계좌번호 표시 요청전문
     * @param _msg1
     * @param _msg2
     * @param _msg3
     * @param _msg4
     * @param _msg5
     * @param _msg6
     * @param _msg7
     * @param _msg8
     * @param _msg9
     * @param _msg10
     * @param _msg11
     * @param _displaytime 화면 표시 유지 시간(00 ~ 99 초 까지 입력 가능) 기본값 00초(무한)
     * @return
     */
    public static byte[] cashIC_trade_req(String _msg1,String _msg2,String _msg3,String _msg4,String _msg5,String _msg6,String _msg7,String _msg8,String _msg9,String _msg10,String _msg11,String _displaytime)
    {
        KByteArray bArray = new KByteArray();
        if(_msg1!=null && _msg1.length()>0){bArray.Add(_msg1);}
        if(_msg2!=null && _msg2.length()>0){bArray.Add(FS);bArray.Add(_msg2);}
        if(_msg3!=null && _msg3.length()>0){bArray.Add(FS);bArray.Add(_msg3);}
        if(_msg4!=null && _msg4.length()>0){bArray.Add(FS);bArray.Add(_msg4);}
        if(_msg5!=null && _msg5.length()>0){bArray.Add(FS);bArray.Add(_msg5);}
        if(_msg6!=null && _msg6.length()>0){bArray.Add(FS);bArray.Add(_msg6);}
        if(_msg7!=null && _msg7.length()>0){bArray.Add(FS);bArray.Add(_msg7);}
        if(_msg8!=null && _msg8.length()>0){bArray.Add(FS);bArray.Add(_msg8);}
        if(_msg9!=null && _msg9.length()>0){bArray.Add(FS);bArray.Add(_msg9);}
        if(_msg10!=null && _msg10.length()>0){bArray.Add(FS);bArray.Add(_msg10);}
        if(_msg11!=null && _msg11.length()>0){bArray.Add(FS);bArray.Add(_msg11);}
        bArray.Add(FS);
        bArray.Add(_displaytime);
        return Utils.MakePacket(CMD_BACCOUNT_REQ,bArray.value());
    }

    /** 자국환(DCC) 선택여부 요청
     * @param _msg1
     * @param _msg2
     * @param _msg3
     * @param _msg4
     * @param _msg5
     * @return
     */
    public static byte[] DCC_Select_req(String _msg1,String _msg2,String _msg3,String _msg4,String _msg5)
    {
        KByteArray bArray = new KByteArray();
        if(_msg1!=null && _msg1.length()>0){bArray.Add(_msg1);}
        if(_msg2!=null && _msg2.length()>0){bArray.Add(FS);bArray.Add(_msg2);}
        if(_msg3!=null && _msg3.length()>0){bArray.Add(FS);bArray.Add(_msg3);}
        if(_msg4!=null && _msg4.length()>0){bArray.Add(FS);bArray.Add(_msg4);}
        if(_msg5!=null && _msg5.length()>0){bArray.Add(FS);bArray.Add(_msg5);}
        bArray.Add(FS);
        return Utils.MakePacket(CMD_CDC_SELECT_REQ,bArray.value());
    }


    //IC 관련 전문

    /**
     * IC 요청 전문 (PC -> 멀티패드)
     * @param _type 거래구분
     * 01:신용IC(RF포함) 참고2),
     * 02: 은련IC
     * 03: 현금IC
     * 04: 포인트/맴버쉽
     * 05: Two Card(신용+포인트)
     * 06: 현금영수증
     * 07: FallBack MSR
     * 08:RF
     * 09:현금영수증(자진발급)
     * 10:은련 FallBack MSR
     * 11:현금IC 카드조회 참고3)
     * 12:가맹점 자체 MS전용 회원카드 참고4)
     * @param _money 금액 최대 10자리
     * @param date YYYYMMDDHHmmss
     * @param _usePad 서명패드입력여부 (멀티패드만 해당)
     * @param _cashIC 현금IC간소화 1 Char 0:미사용, 1:사용
     * @param _printCount 전표출력카운터 4 Char 단말기에서는 전표출력 순번 사용
     * @param _signType 서명데이터 치환방식 1 Char ‘0’(0x30) : 치환하지 않음
     * @param _minPasswd 입력 최소 길이 2 Char 비밀번호 경우 : ‚00‛ /  현금영수증 경우 ‚01‛
     * @param _MaxPasswd 입력 최대 길이 2 Char 비밀번호 경우 : ‚06‛ /  현금영수증 경우 ‚40‛
     * @param _workingKeyIndex Working Key Index 2 Char Working Key Index   2 byte
     * @param _workingkey Working Key 16 Char Working Key         16 byte
     * @param _cashICRnd 현금IC 난수 32 Char
     * @return
     */
    public static byte[] IC_trade_req(String _type,String _money,String date,String _usePad,String _cashIC,String _printCount,String _signType,String _minPasswd,String _MaxPasswd,String _workingKeyIndex,String _workingkey,String _cashICRnd)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_type);
        bArray.Add(_money);
        bArray.Add(date);
        bArray.Add(_usePad);
        bArray.Add(_cashIC);
        bArray.Add(_printCount);
        bArray.Add(_signType);
        bArray.Add(_minPasswd);
        bArray.Add(_MaxPasswd);
        bArray.Add(_workingKeyIndex);
        bArray.Add(_workingkey);
        bArray.Add(_cashICRnd);
//        if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
//            if(Setting.getBleIsConnected()) {
//                if(Setting.getPreference(Setting.getTopContext(),Constants.REGIST_DEVICE_NAME).contains(Constants.ZOA_KRE)) {
//                    String ctime = "";
//                    Calendar c = Calendar.getInstance();
//                    Date today = Calendar.getInstance().getTime();
//                    SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//
//                    ctime = (today.toString()).replaceAll("\\+","");
//                    ctime = ctime.replaceAll("GMT09:00 ","") + "\n";
//
//                    datef.format(today);
//                    long timestamp = 0;
//                    try {
//                        Date dateL = datef.parse(datef.format(today));
//                        timestamp = dateL.getTime();
//                    } catch (ParseException e) {
//                        e.printStackTrace();
//                    }
//
//                    String unix = String.valueOf(timestamp);
//                    unix = unix.substring(0,10);
//                    ctime = ctime + unix;
//
//                    bArray.Add(ctime);
//                }
//            }
//        }

        return Utils.MakePacket(CMD_IC_REQ,bArray.value());
    }

    // 마스킹 되지 않은 카드번호 요청 시 Command ID ‚0x14‛를 사용한다.
     /**
     *
     * @param _type
     * @param _money
     * @param date
     * @param _usePad
     * @param _cashIC
     * @param _printCount
     * @param _signType
     * @param _minPasswd
     * @param _MaxPasswd
     * @param _workingKeyIndex
     * @param _workingkey
     * @param _cashICRnd
     * @return
     */
    public static byte[] IC_trade_nomarkingcard_req(String _type,String _money,String date,String _usePad,String _cashIC,String _printCount,String _signType,String _minPasswd,String _MaxPasswd,String _workingKeyIndex,String _workingkey,String _cashICRnd)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_type);
        bArray.Add(_money);
        bArray.Add(date);
        bArray.Add(_usePad);
        bArray.Add(_cashIC);
        bArray.Add(_printCount);
        bArray.Add(_signType);
        bArray.Add(_minPasswd);
        bArray.Add(_MaxPasswd);
        bArray.Add(_workingKeyIndex);
        bArray.Add(_workingkey);
        bArray.Add(_cashICRnd);
        return Utils.MakePacket(CMD_TWO_CARD_REQ,bArray.value());
    }

    /**
     * IC 서명 요청 전문
     * @return
     */
    public static byte[] IC_sign_req()
    {
        return getSCommand(CMD_SIGN_SEND_REQ);
    }

    /**
     *  IC 거래결과 전문 요청(PC -> 멀티패드)
     * @param _date YYYYMMDDHHMMSS
     * @param _resData Additional Response Data 26 Bin 1B+up to 25ANS
     * @param _issuer Issuer Authentication Data
     * @param _IssuerScript Issuer Script 260 Bin Tag(71/72)
     * @param _result 승인여부 2 Char 00 : 정상 01 : 거절
     * @return
     */
    public static byte[] IC_result_req(String _date,byte[] _resData,byte[] _issuer,byte[] _IssuerScript, String _result)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_date);
        /*
        byte[] bcer1 = new byte[26];
        for(int i=0;i<26;i++){bcer1[i]=(byte)0x20;}
        byte[] bcer2 = new byte[12];
        for(int i=0;i<12;i++){bcer2[i]=(byte)0x20;}
        byte[] bcer3 = new byte[260];
        for(int i=0;i<260;i++){bcer3[i]=(byte)0x20;}
        bArray.Add(bcer1);
        bArray.Add(bcer2);
        bArray.Add(bcer3);

         */
        bArray.Add(_resData);
        bArray.Add(_issuer);
        bArray.Add(_IssuerScript);
        bArray.Add(_result);
        return Utils.MakePacket(CMD_IC_RESULT_REQ,bArray.value());
    }


    /**
     * 은련 IC 카드선택 내용을 사인패드 쪽으로 보냄
     * @param card_count
     * @param card_op1
     * @param card_op2
     * @param card_op3
     * @param card_op4
     * @param card_op5
     * @return
     */
    public static byte[] UnionPay_IC_card_Select_req(String _flag, String card_count, String card_op1, String card_op2, String card_op3, String card_op4, String card_op5, String _time)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_flag);
        if(card_count!=null && card_count.length()>0)
        {
            bArray.Add(card_count);
        }
        if(card_op1!=null && card_op1.length()>0)
        {
            bArray.Add(FS);
            try {
                bArray.Add(card_op1.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(card_op2!=null && card_op2.length()>0)
        {
            bArray.Add(FS);
            try {
                bArray.Add(card_op2.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(card_op3!=null && card_op3.length()>0)
        {
            bArray.Add(FS);
            try {
                bArray.Add(card_op3.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(card_op4!=null && card_op4.length()>0)
        {
            bArray.Add(FS);
            try {
                bArray.Add(card_op4.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(card_op5!=null && card_op5.length()>0)
        {
            bArray.Add(FS);
            try {
                bArray.Add(card_op5.getBytes("euc-kr"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        bArray.Add(_time);
        return Utils.MakePacket(CMD_NO_ENCYPT_NUMBER_REQ,bArray.value());
    }

    /**
     * .10 은련 IC 비밀번호 응답 (PC -> 멀티패드)
     * @param _pinResult
     * @return
     */
    public static byte[] UnionPay_ic_password_req(String _pinResult)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_pinResult);
        return Utils.MakePacket(CMD_UNIONPAY_PARASSWORD_REQ,bArray.value());
    }

    /**
     *12 현금 IC 복수계좌 정보 (PC  -> 멀티패드)
     * @param _account
     * @return
     */
    public static byte[] cashIC_multi_account_req(String _account)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_account);
        return Utils.MakePacket(CMD_CASHIC_MULTIPLE_ACCOUNT_REQ,bArray.value());
    }

    /**
     *  현금 IC 비밀번호 (PC  -> 멀티패드)
     * @param _passwd
     * @return
     */
    public static byte[] cashIC_password_req(String _passwd)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_passwd);
        return Utils.MakePacket(CMD_CASHIC_PASSWARD_REQ,bArray.value());
    }

    /**
     *  KeyIn정보 암호화 요청(PC  -> 멀티패드)
     * @param _track
     * @return
     */
    public static byte[] keyIn_encypt_req(String _track)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_track);
        return Utils.MakePacket(CMD_KEYIN_ENCYPT_REQ, bArray.value());
    }

    /**
     *  보안키 갱신 생성 요청 (PC -> 멀티패드)
     * @return
     */
    public static byte[] securityKey_update_ready_req()
    {
        return getSCommand(CMD_KEYUPDATE_READY_REQ);
    }

    /**
     * 보안키 갱신 요청 (PC -> 멀티패드)
     * @return
     */
    public static byte[] securityKey_update_req(String _time,byte[] _data)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_time);
        bArray.Add(_data);
        return Utils.MakePacket(CMD_KEYUPDATE_REQ,bArray.value());
    }

    /**
     * 자체보안 요청 (PC -> 멀티패드)
     * @return
     */
    public static byte[] selfChecksecurity_req()
    {
        return  getSCommand(CMD_SELFKEYCHECK_REQ);
    }

    /**
     * 상호인증 요청 (PC -> USB멀티패드)
     * @param _type : 0001:최신펌웨어, 0003:EMV Key
     * @return
     */
    public static byte[] mutual_authenticatoin_req(String _type)
    {
        byte[] b = _type.getBytes();
        return Utils.MakePacket(CMD_MUTUAL_AUTHENTICATION_REQ,b);
    }

    /**
     *  상호 인증정보 결과 요청 (PC  -> 멀티패드)
     * @param _date 시간 14 Char YYYYMMDDHHMMSS
     * @param _multipadAuth 멀티패드인증번호 32 Char 인증업체로부터 발급받은 단말 인증 번호
     * @param _multipadSerial 멀티패드시리얼번호 10 Char 멀티패드 시리얼 번호
     * @param _code 응답코드 4 * Char 정상응답 : ‚0000‛(그외 실패) 실패 수신 시 ‚0x55‛응답도 실패(01)처리
     * @param _resMsg 응답메세지
     * @param _key 접속보안키 48 Bin Srandom(16)+KEY2_ENC(IPEK+Crandom, 32)
     * @param _dataCount 데이터 개수 4 Char 주4) 데이터의 개수
     * @param _protocol 전송 프로토콜 5 Char "SFTP"
     * @param _Addr 다운로드서버주소
     * @param _port 포트
     * @param _id 계정
     * @param _passwd 비밀번호
     * @param _ver 버전 및 데이터 구분
     * @param _verDesc 버전 설명
     * @param _fn 파일명
     * @param _fnSize 파일크기
     * @param _fnCheckType 파일체크방식
     * @param _fnChecksum 파일체크섬
     * @param _dscrKey 파일복호화키
     * @return
     */
    public static byte[] authenticatoin_result_req(byte[] _date,byte[] _multipadAuth,byte[] _multipadSerial,byte[] _code,byte[] _resMsg,byte[] _key,byte[] _dataCount,byte[] _protocol,
                                                   byte[] _Addr,byte[] _port,byte[] _id,byte[] _passwd,byte[] _ver,byte[] _verDesc,byte[] _fn,byte[] _fnSize,byte[] _fnCheckType,
                                                   byte[] _fnChecksum,byte[] _dscrKey)
    {
        KByteArray bArray = new KByteArray();
        if (_date.length < 14) {
            bArray.Add(_date);
            for (int i=_date.length; i<14; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_date);
        }

        if (_multipadAuth.length < 32) {
            bArray.Add(_multipadAuth);
            for (int i=_multipadAuth.length; i<32; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_multipadAuth);
        }

        if (_multipadSerial.length < 10) {
            bArray.Add(_multipadSerial);
            for (int i=_multipadSerial.length; i<10; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_multipadSerial);
        }

        if (_code.length < 4) {
            bArray.Add(_code);
            for (int i=_code.length; i<4; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_code);
        }

        if (_resMsg.length < 80) {
            bArray.Add(_resMsg);
            for (int i=_resMsg.length; i<80; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_resMsg);
        }

        if (_key.length < 48) {
            bArray.Add(_key);
            for (int i=_key.length; i<48; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_key);
        }

        if (_dataCount.length < 4) {
            bArray.Add(_dataCount);
            for (int i=_dataCount.length; i<4; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_dataCount);
        }

        if (_protocol.length < 5) {
            bArray.Add(_protocol);
            for (int i=_protocol.length; i<5; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_protocol);
        }

        if (_Addr.length < 80) {
            bArray.Add(_Addr);
            for (int i=_Addr.length; i<80; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_Addr);
        }

        if (_port.length < 5) {
            bArray.Add(_port);
            for (int i=_port.length; i<5; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_port);
        }

        if (_id.length < 16) {
            bArray.Add(_id);
            for (int i=_id.length; i<16; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_id);
        }

        if (_passwd.length < 16) {
            bArray.Add(_passwd);
            for (int i=_passwd.length; i<16; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_passwd);
        }

        if (_ver.length < 5) {
            bArray.Add(_ver);
            for (int i=_ver.length; i<5; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_ver);
        }

        if (_verDesc.length < 80) {
            bArray.Add(_verDesc);
            for (int i=_verDesc.length; i<80; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_verDesc);
        }

        if (_fn.length < 256) {
            bArray.Add(_fn);
            for (int i=_fn.length; i<256; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_fn);
        }

        if (_fnSize.length < 10) {
            bArray.Add(_fnSize);
            for (int i=_fnSize.length; i<10; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_fnSize);
        }

        if (_fnCheckType.length < 5) {
            bArray.Add(_fnCheckType);
            for (int i=_fnCheckType.length; i<5; i++) {
                bArray.Add((byte)0x20);
            }
        } else {
            bArray.Add(_fnCheckType);
        }

        if (_fnChecksum.length < 64) {
            bArray.Add(_fnChecksum);
            for (int i=_fnChecksum.length; i<64; i++) {
                bArray.Add((byte)0x00);
            }
        } else {
            bArray.Add(_fnChecksum);
        }

        if (_dscrKey.length < 32) {
            bArray.Add(_dscrKey);
            for (int i=_dscrKey.length; i<32; i++) {
                bArray.Add((byte)0x00);
            }
        } else {
            bArray.Add(_dscrKey);
        }

        return Utils.MakePacket(CMD_MUTUAL_AUTHENTICATION_RESULT_REQ,bArray.value());
    }

    /**
     *  업데이트 파일전송 (PC -> 멀티패드)
     * @param _type 요청데이타구분 4 Char 0001:최신펌웨어, 0003:EMV Key
     * @param _dataLength 데이터 총 크기
     * @param _sendDataSize 전송중인 데이터 크기
     * @param _defaultSize 기본사이즈 크기, 아래의 데이터가 이것보다 작으면 데이터의 뒤에 0x00을 붙인다
     * @param _data 데이터
     * @return
     */
    public static byte[] updatefile_transfer_req(String _type,String _dataLength,String _sendDataSize,int _defaultSize, byte[] _data)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_type);
        int _l = 10 - _dataLength.length();
        bArray.Add(_dataLength);
        for(int i=0; i<_l; i++) {
            bArray.Add((byte)0x00);
        }
        if(_sendDataSize.equals("")) {
            bArray.Add(_sendDataSize);
        } else {
            int _j = 10 - _sendDataSize.length();
            bArray.Add(_sendDataSize);
            for(int i=0; i<_j; i++) {
                bArray.Add((byte)0x00);
            }
        }

        if (_data.length < _defaultSize) {
            bArray.Add(_data);
            for (int i=_data.length; i<_defaultSize; i++) {
                bArray.Add((byte)0x00);
            }
        } else {
            bArray.Add(_data);
        }

        return Utils.MakePacket(CMD_SEND_UPDATE_DATA_REQ,bArray.value());
    }

    /**
     * 단말기 카드 넣기
     * @return
     */
    public static byte[] insert_card_req()
    {
        return getSCommand(CMD_IC_INSERT_REQ);
    }

    /**
     * 단말기 카드 빼기
     * @return
     */
    public static byte[] remove_card_req()
    {
        return getSCommand(CMD_IC_REMOVE_REQ);
    }

    /**
     * 단말기 카드 상태 체크 요청
     * @return
     */
    public static byte[] check_card_state_req()
    {
        return getSCommand(CMD_IC_STATE_REQ);
    }

    /**
     * 단말기 공장 초기화
     * @return
     */
    public static byte[] reset_factory_req()
    {
        return getSCommand(CMD_RESET_FACTORY_REQ);
    }

    /**
     * RF 카드 정보 요청
     * @param _filler 스페이스 패딩
     * @return
     */
    public static byte[] RfCard_info_req(String _filler)
    {
        if(_filler==null)
        {
            return getSCommand(CMD_RFCARD_INFO_REQ);
        }
        KByteArray bArray = new KByteArray();
        bArray.Add(_filler);
        return Utils.MakePacket(CMD_RFCARD_INFO_REQ,bArray.value());
    }

    public static byte[] RfEncryptCard_info_req(String _filler)
    {
        if(_filler==null)
        {
            return getSCommand(CMD_RFCARD_INFO_ENCYPT_REQ);
        }
        KByteArray bArray = new KByteArray();
        bArray.Add(_filler);
        return Utils.MakePacket(CMD_RFCARD_INFO_ENCYPT_REQ,bArray.value());
    }

    /**
     * 최초 Hash 암호화 요청 (APP -> Module)
      * @param _originalHanshLen 원본 Hash Len  4 Char Hash data 길이 (예: 32byte = 0032)
     * @param _hashData 원본 Hash Data
     * @return
     */
    public static byte[] hashencypt_req(String _originalHanshLen,String _hashData)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_originalHanshLen);
        bArray.Add(_hashData);
        return Utils.MakePacket(CMD_HASH_ENCYPT_REQ,bArray.value());
    }

    /**
     *
     * @param _originalHanshLen 원본 Hash Len  4 Char Hash data 길이 (예: 32byte = 0032)
     * @param _hashData 원본 Hash Data
     * @param _encyptHanshLen 암호화 된 Hash data 길이 (예: 32byte = 0032)
     * @param _encypthashData 암호화 된 Hash Data
     * @return
     */
    public static byte[] Checkhashdata_req(String _originalHanshLen,String _hashData,String _encyptHanshLen,String _encypthashData)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(_originalHanshLen);
        bArray.Add(_hashData);
        bArray.Add(_encyptHanshLen);
        bArray.Add(_encypthashData);
        return Utils.MakePacket(CMD_CHECK_HASH_DATA_REQ,bArray.value());
    }

    /**
     * 바코드 리딩 전문
     * @param _msg1 안내 메시지 1 (타이틀) V32 Char Ex) <앱카드승인>
     * @param _msg2 안내 메시지 2 V32 Char Ex) 금액 : 1004원
     * @param _msg3 안내 메시지 3 V32 Char Ex) 바코드 또는 QR코드 읽혀주세요
     * @return
     */
    public static byte[] barcode_reading_req(String _msg1,String _msg2,String _msg3)
    {
        KByteArray bArray = new KByteArray();
//        bArray.Add(_msg1);
//        bArray.Add(FS);
//        bArray.Add(_msg2);
//        bArray.Add(FS);
//        bArray.Add(_msg3);
//        bArray.Add(FS);
        if(_msg1.length()>0)
        {
            try {
                bArray.Add(_msg1.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        bArray.Add(FS);
        if(_msg2.length()>0)
        {

            try {
                bArray.Add(_msg2.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        bArray.Add(FS);
        if(_msg3.length()>0)
        {

            try {
                bArray.Add(_msg3.getBytes("euc-kr"));
            }
            catch (UnsupportedEncodingException ex)
            {

            }
        }
        bArray.Add(FS);
        return Utils.MakePacket(CMD_BARCODE_REQ,bArray.value());
    }

    public static class ProtocolInfo
    {
        public int Command;
        public int length;
        public byte[] Contents;
        public ProtocolInfo(byte[] _b)
        {
            Command = _b[3];
            byte[] tmp = new byte[2];
            tmp[0] = _b[1];
            tmp[1] = _b[2];
            length = Utils.byteToInt(tmp);
            if(_b.length>7) {
                Contents = new byte[length - 2];
                System.arraycopy(_b, 4, Contents, 0, length - 2);
            }
        }
    }

    /**
     * _command에는 ACK,NAK,ESC,EOT만 넣어야 한다.
     * 내용이 없는 전문에도 사용할 수 있다.
     * @param _command
     * @return
     */
    public static byte[] getSCommand(byte _command)
    {
//        if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
//            if(Setting.getBleIsConnected()) {
//                if(Setting.getPreference(Setting.getTopContext(),Constants.REGIST_DEVICE_NAME).contains(Constants.ZOA_KRE)) {
//                    if (_command == CMD_KEYUPDATE_READY_REQ || _command == CMD_KEYUPDATE_REQ) {
//                        String ctime = "";
//                        Calendar c = Calendar.getInstance();
//                        Date today = Calendar.getInstance().getTime();
//                        SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//
//                        ctime = (today.toString()).replaceAll("\\+","");
//                        ctime = ctime.replaceAll("GMT09:00 ","") + "\n";
//
//                        datef.format(today);
//                        long timestamp = 0;
//                        try {
//                            Date date = datef.parse(datef.format(today));
//                            timestamp = date.getTime();
//                        } catch (ParseException e) {
//                            e.printStackTrace();
//                        }
//
//                        String unix = String.valueOf(timestamp);
//                        unix = unix.substring(0,10);
//                        ctime = ctime + unix;
//                        byte[] ret = ctime.getBytes();
//
//                        byte[] tmp = new byte[5+ret.length];
//                        tmp[0] = STX;
//                        byte[] dataLen = intTo2ByteArray(F_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA+ret.length);
//                        System.arraycopy(dataLen,0,tmp,1,dataLen.length);
//
//                        tmp[3] = _command;
//
//                        System.arraycopy(ret, 0, tmp, Command.F_CONTROL_DAT_BYTE, ret.length);
//
//                        tmp[4+ret.length] = ETX;
//                        byte sendBuffer[] = new byte[tmp.length + 1];
//                        System.arraycopy(tmp, 0, sendBuffer, 0, tmp.length);
//                        sendBuffer[tmp.length] = makeLRC(tmp);
//
//                        return sendBuffer;
//                    }
//
//                }
//            }
//        }

        byte[] tmp = new byte[5];
        tmp[0] = STX;
        byte[] dataLen = intTo2ByteArray(F_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA);
        System.arraycopy(dataLen,0,tmp,1,dataLen.length);

        tmp[3] = _command;
        tmp[4] = ETX;
        byte sendBuffer[] = new byte[tmp.length + 1];
        System.arraycopy(tmp, 0, sendBuffer, 0, tmp.length);
        sendBuffer[tmp.length] = makeLRC(tmp);

        return sendBuffer;
     }


    static class PacketInfo
    {
        public int Command;
        public int length;
        public byte[] Contents;
        public PacketInfo(byte[] _b)
        {
            Command = _b[3];
            byte[] tmp = new byte[2];
            tmp[0] = _b[1];
            tmp[1] = _b[2];
            length = Utils.byteToInt(tmp);
            if(_b.length>7) {
                Contents = new byte[length - 2];
                System.arraycopy(_b, 4, Contents, 0, length - 2);
            }
        }
    }

    public static String Check_IC_result_code(String _res) {
        if (_res.contains("00")) {
            return "00";
        } else if (_res.contains("M")) {
            return "M";
        } else if (_res.contains("R")) {
            return "R";
        } else if (_res.contains("E")) {
            return "E";
        } else if (_res.contains("F")) {
            return "F";
        } else if (_res.contains("K")) {
            return "K";
        } else if (_res.contains("99")) {
            return "99";
        } else if (_res.contains("01")) {
            return "Chip 전원을 넣었으나 응답이 없습니다.";
        } else if (_res.contains("02")) {
            return "지원하지 않는 어플리케이션 입니다.";
        } else if (_res.contains("03")) {
            return "칩데이터 읽기에 실패 하였습니다.";
        } else if (_res.contains("04")) {
            return "Mandatory 데이터 포함 되어 있지 않습니다.";
        } else if (_res.contains("05")) {
            return "CVM 커맨드 응답에 실패 하였습니다";
        } else if (_res.contains("06")) {
            return "EMV 커맨드 잘못설정 되었습니다.";
        } else if (_res.contains("07")) {
            return "터미널 오작동";
        } else if (_res.contains("08")) {
            return "IC카드 읽기에 실패 하였습니다.";
        } else if (_res.contains("09")) {
            return "IC우선 거래 입니다.";
        } else if (_res.contains("10")) {
            return "처리 불가 카드 입니다.";
        } else if (_res.contains("11")) {
            return "MS 읽기 실패 입니다.";
        } else if (_res.contains("12")) {
            return "해외은련카드(PIN필요카드) 지원 불가 입니다.";
        }
        return "지원 하지 않는 카드 입니다.";
    }

    /**
     * 여기서부터 BLE 요청
     */
    ///장치 초기화
    ///- parameter VanCode: 일반 서명패드, 멀티패드-99 기존 통합동글용-13 COSTCO 서명패드-88
    ///- returns: 완성된 패킷 데이터 UInt8 Array
    public static byte[] BLEDeviceInit(String _vanCode)
    {
        byte[]b = _vanCode.getBytes();
        return Utils.MakePacket(CMD_INIT,b);
    }

    /// 장치 무결성 검사
    /// - Returns: 완성된 패킷 데이터 UInt8 Array
    public static byte[] BLEGetVerity()
    {
        return getSCommand(CMD_SELFKEYCHECK_REQ); // Utils.MakePacket(CMD_SELFKEYCHECK_REQ,b);
    }

    /**
     BLE단말기 정보 요청 장비정보
     - parameter _date: 현재 시간  yyyyMMddhhmmss (14자)
     - Returns: 완성된 패킷 데이터 UInt8 Array
     */
    public static byte[] BLEpos_info(String _date)
    {
        byte[] b = _date.getBytes();
        return Utils.MakePacket(CMD_POSINFO_REQ,b);
    }

    /**
     * BLE단말기 상호인증 요청 (PC -> BLE멀티패드)
     * @param _type : 0001:최신펌웨어, 0003:EMV Key
     * @return
     */
    public static byte[] BLEmutual_authenticatoin_req(String _type)
    {
        byte[] b = _type.getBytes();
        return Utils.MakePacket(CMD_MUTUAL_AUTHENTICATION_REQ,b);
    }


    public static byte[] makeLrcData(byte[] b) {
        //ETX부분 추가 20180116 yowonsm
        byte b1[] = new byte[b.length + 1];
        System.arraycopy(b, 0, b1, 0, b.length);
        b1[b.length] = ETX;

        byte sendBuffer[] = new byte[b1.length + 1];
        System.arraycopy(b1, 0, sendBuffer, 0, b1.length);
        sendBuffer[b1.length] = Utils.makeLRC(b1);
        return sendBuffer;
    }

    /**
     * 시스템 정보를 조회 (0xA0) 리더기 정보조회 DS 2000용입니다. 해당데이터 지워야함
     */
    public static byte[] GetSystemInfo() {
        byte[] d = new byte[18];
        String data = Utils.getDate("yyyyMMddHHmmss");	//데이터 부분은 현재시간
        d[0] = STX;
        d[1] = (byte)0xA0;

        byte[] length = Utils.intTo2ByteArray(d.length);
        d[2] = length[0];
        d[3] = length[1];

        byte[] ret = data.getBytes();
        System.arraycopy(ret, 0, d, Command.F_CONTROL_DAT_BYTE, ret.length);

//        Log.d(TAG, "Get System Info Hex->" + BytesToHex(d));

        return d;
    }



    /**
     * 여기까지 BLE요청
     */


    /**
     * 여기부터 CAT 요청전문
     */
    /// cat 신용 결제
    /// - Parameters:
    ///   - _tid: tid
    ///   - _money: 거래금액
    ///   - _tax: 세금
    ///   - _svc: 봉사료
    ///   - _txf: 비과세
    ///   - _AuDate: 원거래일자
    ///   - _AuNo: 원승인번호
    ///   - _KocesUniqueNumber: 코세스거래고유번호
    ///   - _Installment: 할부
    ///   - _cancel: 취소
    ///   - _mchData: 가맹점데이터
    ///   - _extrafield: 여유필드
    /// - Returns: UInt8 Array
    public static byte[] Cat_Credit(String _tid,String _money,String _tax,String _svc,String _txf,String _AuDate,String _AuNo,
                                    String _KocesUniqueNumber,String _Installment,boolean _cancel,String _mchData,String _extrafield)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(Command.STX);
        bArray.Add(Command.CMD_CAT_AUTH);
        bArray.Add(Command.CMD_CAT_CREDIT_REQ);
        bArray.Add(StringUtil.rightPad(_tid," ",10));
        bArray.Add(StringUtil.leftPad(_money,"0",9));
        bArray.Add(StringUtil.leftPad(_tax,"0",9));
        bArray.Add(StringUtil.leftPad(_svc,"0",9));
        bArray.Add(StringUtil.leftPad(_txf,"0",9));

        if (!_cancel)
        {
            for(int i=0; i<8; i++) {
                bArray.Add((byte)0x20);
            }
            for(int i=0; i<12; i++) {
                bArray.Add((byte)0x20);
            }
            for(int i=0; i<20; i++) {
                bArray.Add((byte) 0x20);
            }
        }
        else
        {
            bArray.Add(StringUtil.rightPad(_AuDate," ",8));
            bArray.Add(StringUtil.rightPad(_AuNo," ",12));
            if (_KocesUniqueNumber == "")
            {    //코세스고유번호취소가 아닌 경우
                for(int i=0; i<20; i++) {
                    bArray.Add((byte) 0x20);
                }
            }
            else
            {
                bArray.Add(StringUtil.rightPad(_KocesUniqueNumber," ",20));
            }
        }

        bArray.Add(StringUtil.leftPad(_Installment,"0",2));

        for(int i=0; i<20; i++) {
            bArray.Add((byte) 0x20);
        }
        bArray.Add((byte) 0x20);
        bArray.Add((byte) 0x20);

        if (_mchData.length() > 0) {
            bArray.Add(StringUtil.rightPad(_mchData," ",20));
        } else {
            for(int i=0; i<20; i++) {
                bArray.Add((byte) 0x20);
            }
        }

        if (_extrafield.length() > 0) {
            bArray.Add(StringUtil.rightPad(_extrafield," ",20));
        } else {
            for (int i = 0; i < 20; i++) {
                bArray.Add((byte) 0x20);
            }
        }
        bArray.Add(Command.ETX);
        byte _lrc = Utils.makeLRC(bArray.value());
        bArray.Add(_lrc);
        return bArray.value();
    }

    public static byte[] Cat_CashRecipt(String _tid,String _money,String _tax,String _svc,String _txf,String _AuDate,String _AuNo,
                                        String _KocesUniqueNumber,String _Installment,String _customNum,String _pb,boolean _cancel,
                                        String _cancelReason,String _mchData,String _extrafield)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(Command.STX);
        bArray.Add(Command.CMD_CAT_AUTH);
        bArray.Add(Command.CMD_CAT_CASH_REQ);
        bArray.Add(StringUtil.rightPad(_tid," ",10));
        bArray.Add(StringUtil.leftPad(_money,"0",9));
        bArray.Add(StringUtil.leftPad(_tax,"0",9));
        bArray.Add(StringUtil.leftPad(_svc,"0",9));
        bArray.Add(StringUtil.leftPad(_txf,"0",9));
        if (!_cancel) {
            for(int i=0; i<8; i++) {
                bArray.Add((byte)0x20);
            }
            for(int i=0; i<12; i++) {
                bArray.Add((byte)0x20);
            }
            for(int i=0; i<20; i++) {
                bArray.Add((byte) 0x20);
            }
        } else {
            bArray.Add(StringUtil.rightPad(_AuDate," ",8));
            bArray.Add(StringUtil.rightPad(_AuNo," ",12));
            if (_KocesUniqueNumber.isEmpty())
            {    //코세스고유번호취소가 아닌 경우
                for(int i=0; i<20; i++) {
                    bArray.Add((byte) 0x20);
                }
            }
            else
            {
                bArray.Add(StringUtil.rightPad(_KocesUniqueNumber," ",20));
            }
        }

        bArray.Add((byte) 0x20);
        bArray.Add((byte) 0x20);

        if (_customNum.isEmpty()) {
            for(int i=0; i<20; i++) {
                bArray.Add((byte) 0x20);
            }
        } else {
            bArray.Add(StringUtil.rightPad(_customNum," ",20));
        }
        bArray.Add(_pb);

        if (_cancelReason.isEmpty()) {
            bArray.Add((byte) 0x20);
        } else {
            bArray.Add(_cancelReason);
        }

        if (_mchData.isEmpty()) {
            for(int i=0; i<20; i++) {
                bArray.Add((byte) 0x20);
            }
        } else {
            bArray.Add(StringUtil.rightPad(_mchData," ",20));
        }

        if (_extrafield.isEmpty()) {
            for(int i=0; i<20; i++) {
                bArray.Add((byte) 0x20);
            }
        } else {
            bArray.Add(StringUtil.rightPad(_extrafield," ",20));
        }

        bArray.Add(Command.ETX);
        byte _lrc = Utils.makeLRC(bArray.value());
        bArray.Add(_lrc);
        return bArray.value();
    }

    public static byte[] Cat_CreditAppCard(String _trdType,String _tid,String _qr,String _money, String _tax,String _svc,String _txf,
                                           String _easyKind,String _AuDate, String _AuNo, String _subAuNo,String _InstallMent,
                                           String _MchData,String _hostMchData,String _UniqueNumber)
    {
        int _length = 0;
        KByteArray btmp = new KByteArray();

        btmp.Add(_trdType);

        btmp.Add("S01="); btmp.Add(";");
        btmp.Add("S02="); btmp.Add(";");
        btmp.Add("S03="); btmp.Add(";");
        btmp.Add("S04="); btmp.Add(";");
        btmp.Add("S05="); btmp.Add(";");
        btmp.Add("S06="); btmp.Add(";");
        btmp.Add("S07="); btmp.Add(";");
        btmp.Add("S08="); btmp.Add(";");
        btmp.Add("S09="); btmp.Add(";");
        btmp.Add("S010="); btmp.Add(";");
        btmp.Add("S011="); btmp.Add(";");
        btmp.Add("S012="); btmp.Add(";");
        btmp.Add("S013="); btmp.Add(";");
        btmp.Add("S014="); btmp.Add(";");
        btmp.Add("S015="); btmp.Add(";");
        btmp.Add(StringUtil.rightPad(_tid," ",10));
        if (!_trdType.equals("Z30"))
        {
            btmp.Add(_qr);
        }

        btmp.Add(StringUtil.leftPad(_money,"0",12));
        btmp.Add(StringUtil.leftPad(_tax,"0",12));
        btmp.Add(StringUtil.leftPad(_svc,"0",12));
        btmp.Add(StringUtil.leftPad(_txf,"0",12));
        if (!_trdType.equals("Z30"))
        {
            btmp.Add(_easyKind);
        }



        if (_trdType.equals("A10")) {

        } else {

            btmp.Add(StringUtil.rightPad(_AuDate," ",6));
            btmp.Add(StringUtil.rightPad(_AuNo," ",40));
            if (_trdType.equals("A20"))
            {
                btmp.Add(StringUtil.rightPad(_subAuNo," ",40));
            }

        }

        if (_trdType.equals("A10"))
        {
            btmp.Add(StringUtil.leftPad(_InstallMent,"0",2));
        }

        if (_MchData.length() > 0) {
            btmp.Add(StringUtil.rightPad(_MchData," ",64));
        }

        if (!_trdType.equals("Z30")) {
            if (_hostMchData.length() > 0) {
                btmp.Add(StringUtil.rightPad(_hostMchData, " ", 50));
            }
        }

        if (!_trdType.equals("A10"))
        {
            if (_UniqueNumber.isEmpty())
            {    //코세스고유번호취소가 아닌 경우

            }
            else
            {
                btmp.Add(StringUtil.rightPad(_UniqueNumber," ",20));
            }
        }

        _length = btmp.getlength();



        KByteArray bArray = new KByteArray();
        bArray.Add(Command.STX);
        bArray.Add(Command.CMD_CAT_EASY_AUTH);
        bArray.Add(Command.CMD_CAT_APPPAY_REQ);
        bArray.Add(StringUtil.leftPad(String.valueOf(_length),"0",4));
        //길이 들어감
        bArray.Add("S01=");
        bArray.Add(_trdType);
        bArray.Add(";");
        bArray.Add("S02=");
        bArray.Add(StringUtil.rightPad(_tid," ",10));
        bArray.Add(";");
        bArray.Add("S03=");
        if (!_trdType.equals("Z30"))
        {
            bArray.Add(_qr);
        }

        bArray.Add(";");
        bArray.Add("S04=");
        bArray.Add(StringUtil.leftPad(_money,"0",12));
        bArray.Add(";");
        bArray.Add("S05=");
        bArray.Add(StringUtil.leftPad(_tax,"0",12));
        bArray.Add(";");
        bArray.Add("S06=");
        bArray.Add(StringUtil.leftPad(_svc,"0",12));
        bArray.Add(";");
        bArray.Add("S07=");
        bArray.Add(StringUtil.leftPad(_txf,"0",12));
        bArray.Add(";");
        bArray.Add("S08=");
        if (!_trdType.equals("Z30"))
        {
            bArray.Add(_easyKind);
        }

        bArray.Add(";");

        if (_trdType.equals("A10")) {
            bArray.Add("S09=");
            bArray.Add(";");
            bArray.Add("S10=");
            bArray.Add(";");
            bArray.Add("S11=");
            bArray.Add(";");

        } else {
            bArray.Add("S09=");
            bArray.Add(StringUtil.rightPad(_AuDate," ",6));
            bArray.Add(";");
            bArray.Add("S10=");
            bArray.Add(StringUtil.rightPad(_AuNo," ",40));
            bArray.Add(";");
            bArray.Add("S11=");
            if (_trdType.equals("A20"))
            {
                bArray.Add(StringUtil.rightPad(_subAuNo," ",40));
            }

            bArray.Add(";");
        }

        bArray.Add("S12=");
        if (_trdType.equals("A10"))
        {
            bArray.Add(StringUtil.leftPad(_InstallMent,"0",2));
        }

        bArray.Add(";");

        bArray.Add("S13=");
        if (_MchData.length() > 0) {
            bArray.Add(StringUtil.rightPad(_MchData," ",64));
        }
        bArray.Add(";");

        bArray.Add("S14=");
        if (!_trdType.equals("Z30")) {
            if (_hostMchData.length() > 0) {
                bArray.Add(StringUtil.rightPad(_hostMchData, " ", 50));
            }
        }
        bArray.Add(";");

        bArray.Add("S15=");
        if (!_trdType.equals("A10"))
        {
            if (_UniqueNumber.isEmpty())
            {    //코세스고유번호취소가 아닌 경우

            }
            else
            {
                bArray.Add(StringUtil.rightPad(_UniqueNumber," ",20));
            }
        }

        bArray.Add(";");

        bArray.Add(Command.ETX);
        byte _lrc = Utils.makeLRC(bArray.value());
        bArray.Add(_lrc);

        return bArray.value();
    }

    /// 무선단말기 현금IC 거래 요청
    /// - Parameters:
    ///   - _Class: <#_Class description#>
    ///   - _tid: <#_tid description#>
    ///   - _money: <#_money description#>
    ///   - _tax: <#_tax description#>
    ///   - _svc: <#_svc description#>
    ///   - _txf: <#_txf description#>
    ///   - _AuDate: <#_AuDate description#>
    ///   - _AuNo: <#_AuNo description#>
    ///   - _KocesUniqueNumber: <#_KocesUniqueNumber description#>
    ///   - _Installment: <#_Installment description#>
    ///   - _cancel: <#_cancel description#>
    ///   - _mchData: <#_mchData description#>
    ///   - _extrafield: <#_extrafield description#>
    /// - Returns: <#description#>
    public static byte[] Cat_CashIC(Constants.CashICBusinessClassification _Class, String _tid,
                                    String _money,String _tax,String _svc,String _txf,
                                    String _AuDate,String _AuNo,String _directTrade,String _cardInfo,
                                    boolean _cancel,String _mchData,String _extrafield)
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(Command.STX);
        bArray.Add(Command.CMD_CAT_AUTH);
        bArray.Add(Command.CMD_CAT_CASHIC_REQ);
        bArray.Add((_Class.toString()).getBytes(StandardCharsets.UTF_8));
        bArray.Add(StringUtil.rightPad(_tid," ",10));

        if (_Class != Constants.CashICBusinessClassification.Search) {
            bArray.Add(StringUtil.leftPad(_money,"0",12));
        } else {
            bArray.Add(StringUtil.leftPad("0","0",12));
        }

        if (_Class == Constants.CashICBusinessClassification.Buy ||
                _Class == Constants.CashICBusinessClassification.Cancel ||
                _Class == Constants.CashICBusinessClassification.Search) {
            bArray.Add(StringUtil.leftPad(_tax,"0",12));
            bArray.Add(StringUtil.leftPad(_svc,"0",12));
            bArray.Add(StringUtil.leftPad(_txf,"0",12));
        } else {
            bArray.Add(StringUtil.leftPad("0","0",12));
            bArray.Add(StringUtil.leftPad("0","0",12));
            bArray.Add(StringUtil.leftPad("0","0",12));
       }

        if (_Class == Constants.CashICBusinessClassification.BuySearch ||
                _Class == Constants.CashICBusinessClassification.Cancel ||
                _Class == Constants.CashICBusinessClassification.CancelSearch) {
            bArray.Add(StringUtil.leftPad(_AuDate," ",6));
            bArray.Add(StringUtil.leftPad(_AuNo," ",13));
        } else {
            for(int i=0; i<6; i++) {
                bArray.Add((byte)0x20);
            }
            for(int i=0; i<13; i++) {
                bArray.Add((byte)0x20);
            }
        }

        if (_Class == Constants.CashICBusinessClassification.Buy ||
                _Class == Constants.CashICBusinessClassification.Cancel) {
            bArray.Add(StringUtil.leftPad(_directTrade," ",1));
        } else {
            bArray.Add((byte) 0x20);
        }

        bArray.Add(StringUtil.leftPad(_cardInfo," ",1));
        if (_mchData != null && _mchData.length() != 0) {
            bArray.Add(StringUtil.rightPad(_mchData," ",64));
        }
        else
        {
            for(int i=0; i<64; i++) {
                bArray.Add((byte)0x20);
            }
        }

        if (_extrafield != null && _extrafield.length() != 0) {
            bArray.Add(StringUtil.rightPad(_extrafield," ",20));
        }
        else
        {
            for(int i=0; i<20; i++) {
                bArray.Add((byte)0x20);
            }
        }

        bArray.Add(Command.ETX);
        byte _lrc = Utils.makeLRC(bArray.value());
        bArray.Add(_lrc);

        return bArray.value();
    }

    /// 통합전문 거래응답 통신전문
    /// - Parameters:
    ///   - _com: 명령코드
    ///   G120(신용승인&취소, DCC, 앱카드),-> G125
    ///   G130(현금승인&취소)-> G135
    ///   G140(은련승인&취소)-> G145
    ///   G160(DCC)-> G165
    ///   G170(현금 IC)-> G175
    ///   - _code: 수신응답코드 0000(성공)/그외(실패)
    ///   - _msg: 응답메세지 0000 시 “정상수신” 그 외 실패코드에 따른 응답메시지
    /// - Returns: Byte Array
    public static byte[] Cat_ResponseTrade(String _com,byte[] _code,String _msg) {
        KByteArray bArray = new KByteArray();
        bArray.Add(Command.STX);
        bArray.Add(_com);
        bArray.Add(_code);
        byte[] msg = new byte[0];
        try {
            msg = _msg.getBytes("EUC-KR");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        bArray.Add(msg);
        for (int i = msg.length; i < 40; i++)
        {
            bArray.Add((byte)0x20);
        }
        bArray.Add(Command.ETX);
        byte _lrc = Utils.makeLRC(bArray.value());
        bArray.Add(_lrc);
        return bArray.value();
    }

    /// Cancel CMD 요청 전문(E)
    public static byte[] Cat_CancelCMD_E()
    {
        KByteArray bArray = new KByteArray();
        bArray.Add(Command.STX);
        bArray.Add("E"); //E
        for (int i = 0; i < 10; i++)
        {
            bArray.Add((byte)0x20);
        }
        bArray.Add(Command.ETX);
        byte _lrc = Utils.makeLRC(bArray.value());
        bArray.Add(_lrc);
        return bArray.value();
    }

    /// 프린트
    /// - Parameter _Contents: <#_Contents description#>
    /// - Returns: <#description#>
    public static byte[] Print(byte[] _Contents)
    {

        byte[] b = _Contents;
        return Utils.MakePacket(CMD_PRINT_REQ,b);

    }

    /** Ble 페어링 해제 후 전원 유지 */
    public static byte[] PowerManager(byte _Contents)
    {
        byte[] b = {_Contents};
        return Utils.MakePacket(Command.CMD_BLE_POWER_MANAGER_REQ, b);
    }

    //=========================================================== BLE프린터 파서 ========================================================================
    public static byte[] BLEPrintParser(String _Contents) {
        byte[] Center = { 0x1B, 0x61, 0x31 };
        byte[] Left = { 0x1B, 0x61, 0x30 };
        byte[] Right = { 0x1B, 0x61, 0x32 };
        byte[] BoldStart = { 0x1B, 0x21, 0x20 };
        byte[] BoldEnd = { 0x1B, 0x21, 0x00 };
        byte[] Money = { 0x1B, 0x70, 0x31, 0x01, 0x05};
        
        byte[] b = new byte[0];
        String _bT = "";
        KByteArray temp = new KByteArray();

        if (_Contents.equals(Constants.PMoney) || _Contents.equals(Constants.PMoney_Tong))
        {
            temp.Add(Money);
        }
        else
        {
            String cont = _Contents.replace("___LF___", "\n");
            cont = cont.replace( "Font_LF",  "\n");
            String[] StrArr = cont.split( "\n");

            for(String n : StrArr) {
                String n1 = n.replace( "__JYCE__", new String(Center));
                String n2 = n1.replace( "__JYLE__", new String(Left));
                String n3 = n2.replace( "__JYRI__", new String(Right) );
                String n4 = n3.replace( "__JYBE__", new String(BoldEnd) );
                String n5 = n4.replace( "__JYBS__", new String(BoldStart) );
                n5 = n5.replace( Constants.PInit,  "");
                n5 = n5.replace( Constants.PFont_HT,  "");
                n5 = n5.replace( Constants.PFont_LF,  "");

                n5 = n5.replace( Constants.PFont_CR,  "");
                n5 = n5.replace( Constants.PLogo_Print,  "");
                n5 = n5.replace( Constants.PCut_print,  "");
                n5 = n5.replace( Constants.PMoney_Tong,  "");
                n5 = n5.replace( Constants.PPaper_up,  "");
                if (n5.contains(Constants.PPaper_up)) {
                    n5 = n5.replace( Constants.PPaper_up,  "");
                    int length = n5.length();
                    n5 = n5.substring(4,length);
//                n5.removeFirst(); n5.removeFirst(); n5.removeFirst(); n5.removeFirst();
                }
                n5 = n5.replace( Constants.PPaper_up,  "");
                n5 = n5.replace( Constants.PFont_Sort_L,  new String( Left));
                n5 = n5.replace( Constants.PFont_Sort_C,  new String( Center));
                n5 = n5.replace( Constants.PFont_Sort_R,  new String( Right));
                n5 = n5.replace( Constants.PFont_Default,  new String( BoldEnd));
                n5 = n5.replace( Constants.PFont_Size_H,  "");
                n5 = n5.replace( Constants.PFont_Size_W,  "");
                n5 = n5.replace( Constants.PFont_Size_B,  new String( BoldStart));
                n5 = n5.replace( Constants.PMoney,  new String( Money));
                n5 = n5.replace( Constants.PFont_Bold_0,  "");
                n5 = n5.replace( Constants.PFont_Bold_1,  "");
                n5 = n5.replace( Constants.PFont_DS_0,  "");
                n5 = n5.replace( Constants.PFont_DS_1,  "");
                n5 = n5.replace( Constants.PFont_Udline_0,  "");
                n5 = n5.replace( Constants.PFont_Udline_1,  "");
                if (n5.contains(Constants.PBar_Print_1)) {
                    n5 = n5.replace( Constants.PBar_Print_1,  "");
                    int length = n5.length();
                    n5 = n5.substring(4,length);
//                n5.removeFirst(); n5.removeFirst(); n5.removeFirst(); n5.removeFirst();

                }
//            n5 = n5.replacingOccurrences( define.PBar_Print_1,  "")
                if (n5.contains(Constants.PBar_Print_2)) {
                    n5 = n5.replace( Constants.PBar_Print_2,  "");
                    int length = n5.length();
                    n5 = n5.substring(4,length);
//                n5.removeFirst(); n5.removeFirst(); n5.removeFirst(); n5.removeFirst();
                }
//            n5 = n5.replacingOccurrences( define.PBar_Print_2,  "")
                if (n5.contains(Constants.PBar_Print_3)) {
                    n5 = n5.replace( Constants.PBar_Print_3,  "");
                    int length = n5.length();
                    n5 = n5.substring(4,length);
//                n5.removeFirst(); n5.removeFirst(); n5.removeFirst(); n5.removeFirst();
                }
//            n5 = n5.replacingOccurrences( define.PBar_Print_3,  "")
                if (n5.contains(Constants.PBarH_Size)) {
                    n5 = n5.replace( Constants.PBarH_Size,  "");
                    int length = n5.length();
                    n5 = n5.substring(4,length);
//                n5.removeFirst(); n5.removeFirst(); n5.removeFirst(); n5.removeFirst();
                }
//            n5 = n5.replacingOccurrences( define.PBarH_Size,  "")
                if (n5.contains(Constants.PBarW_Size)) {
                    n5 = n5.replace( Constants.PBarW_Size,  "");
                    int length = n5.length();
                    n5 = n5.substring(4,length);
//                n5.removeFirst(); n5.removeFirst(); n5.removeFirst(); n5.removeFirst();
                }
//            n5 = n5.replacingOccurrences( define.PBarW_Size,  "")
                n5 = n5.replace( Constants.PBar_Position_1,  "");
                n5 = n5.replace( Constants.PBar_Position_2,  "");
                n5 = n5.replace( Constants.PBar_Position_3,  "");

                _bT += n5 + "\n";

                temp.Add(n5);
                temp.Add((byte)0x0A);

            }
        }


        if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
            if (Setting.getBleIsConnected()) {
                if (Setting.getPreference(Setting.getTopContext(), Constants.REGIST_DEVICE_NAME).contains(Constants.ZOA_KRE) ||
                        Setting.getPreference(Setting.getTopContext(), Constants.REGIST_DEVICE_NAME).contains(Constants.KWANGWOO_KRE) ||
                        Setting.getPreference(Setting.getTopContext(), Constants.REGIST_DEVICE_NAME).contains(Constants.WOOSIM) ||
                        Setting.getPreference(Setting.getTopContext(), Constants.REGIST_DEVICE_NAME).contains(Constants.WSP)) {
                    b = new byte[_bT.length()];
                    try {
                        b= _bT.getBytes("euc-kr");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    return b;
                }
            }
        }


        return temp.value();
    }

    public static String CatPrintParser(String _msg) {
        byte[] Center = { 0x1B, 0x61, 0x31 };
        byte[] Left = { 0x1B, 0x61, 0x30 };
        byte[] Right = { 0x1B, 0x61, 0x32 };
        byte[] BoldStart = { 0x1B, 0x21, 0x20 };
        byte[] BoldEnd = { 0x1B, 0x21, 0x00 };
        byte[] Money = { 0x1B, 0x70, 0x31, 0x01, 0x05};

        String _print = "";

        _print = _msg.replace( Constants.PInit, new String(Constants.Init));

        _print = _print.replace( "__JYCE__", new String(Center));
        _print = _print.replace( "__JYLE__", new String(Left));
        _print = _print.replace( "__JYRI__", new String(Right) );
        _print = _print.replace( "__JYBE__", new String(BoldEnd) );
        _print = _print.replace( "__JYBS__", new String(BoldStart) );

        _print = _print.replace( Constants.PFont_HT,  new String( Constants.Font_HT));
        _print = _print.replace( Constants.PFont_LF,  new String( Constants.Font_LF));

        _print = _print.replace( Constants.PFont_CR,  new String( Constants.Font_CR));
        _print = _print.replace( Constants.PLogo_Print,  new String( Constants.Logo_Print));
        _print = _print.replace( Constants.PCut_print,  new String( Constants.Cut_print));
        _print = _print.replace( Constants.PMoney_Tong,  new String( Constants.Money_Tong));
        _print = _print.replace( Constants.PPaper_up,  new String( Constants.Paper_up));
        _print = _print.replace( Constants.PFont_Sort_L,  new String( Constants.Font_Sort_L));
        _print = _print.replace( Constants.PFont_Sort_C,  new String( Constants.Font_Sort_C));
        _print = _print.replace( Constants.PFont_Sort_R,  new String( Constants.Font_Sort_R));
        _print = _print.replace( Constants.PFont_Default,  new String( Constants.Font_Default));
        _print = _print.replace( Constants.PFont_Size_H,  new String( Constants.Font_Size_H));
        _print = _print.replace( Constants.PFont_Size_W,  new String( Constants.Font_Size_W));
        _print = _print.replace( Constants.PFont_Size_B,  new String( Constants.Font_Size_B));
        _print = _print.replace( Constants.PFont_Bold_0,  new String( Constants.Font_Bold_0));
        _print = _print.replace( Constants.PFont_Bold_1,  new String( Constants.Font_Bold_1));
        _print = _print.replace( Constants.PFont_DS_0,  new String( Constants.Font_DS_0));
        _print = _print.replace( Constants.PFont_DS_1,  new String( Constants.Font_DS_1));
        _print = _print.replace( Constants.PFont_Udline_0,  new String( Constants.Font_Udline_0));
        _print = _print.replace( Constants.PFont_Udline_1,  new String( Constants.Font_Udline_1));
        _print = _print.replace( Constants.PBar_Print_1,  new String( Constants.Bar_Print_1));
        _print = _print.replace( Constants.PBar_Print_2,  new String( Constants.Bar_Print_2));
        _print = _print.replace( Constants.PBar_Print_3,  new String( Constants.Bar_Print_3));
        _print = _print.replace( Constants.PBarH_Size,  new String( Constants.BarH_Size));
        _print = _print.replace( Constants.PBarW_Size,  new String( Constants.BarW_Size));
        _print = _print.replace( Constants.PBar_Position_1,  new String( Constants.Bar_Position_1));
        _print = _print.replace( Constants.PBar_Position_2,  new String( Constants.Bar_Position_2));
        _print = _print.replace( Constants.PBar_Position_3,  new String( Constants.Bar_Position_3));
        _print = _print.replace( Constants.PMoney,  new String( Constants.Money));

        return _print;
    }
}

