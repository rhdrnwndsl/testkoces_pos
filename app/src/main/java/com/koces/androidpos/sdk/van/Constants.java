package com.koces.androidpos.sdk.van;

import java.security.PublicKey;

/**
 * 상수 정의
 * Created by hojunbaek on 5/18/16.
 */
public class Constants {
    public static final boolean DB_DEBUG			= false;
    public static final String DB_PATH_FOR_TEST 	= "/mnt/sdcard/";
    public static final String API_KEY              = "5OJ1S6xBo35V5cfuwIEtoK1rZO3TAHL5BGDQPLhc";
    public static final String DB_FILENAME 		    = "data.db";
    public static final String DB_PATH 			    = "/data/data/com.koces/databases/";
    public static final String PACKAGE              = "com.koces";

    //REAL KOCES
//    public static String HOST                       ="211.192.167.38 ";
//    public static int    PORT                       = 10555;

    // TEST KOCES
//    public static String HOST                       ="211.192.167.87";
//    public static int    PORT                       = 10555;
    /** 테스트 TID */
    public static String TEST_TID                        = "0710000900";
    /** 테스트 사업자번호 */
    public static String TEST_BUSINESS_NUMBER            = "2148631917";
    /** 테스트 시리얼번호 */
    public static String TEST_SERIALNUMBER               ="1000000007";
    /** 소프트웨어 버전 */
//    public static String TEST_SOREWAREVERSION               ="A1004";   //2020-08-05 kim.jy 수정
    public static String TEST_SOREWAREVERSION               ="KA117";   //v1007
    public static final String LAST_CONNECTED_DEV_ADDR     = "LAST_CONNECTED_DEV_ADDR";     //마지막 연결된 장치의 mac address
    public static final String LAST_CONNECTED_PRINTER_ADDR = "LAST_CONNECTED_PRINTER_ADDR";
    public static final String MASTER_PASSWORD  = "MASTER_PASSWORD";
    public static final String EMAIL            = "EMAIL";
    public static final String Time            = "Time";
    public static final String IS_AUTO_LOGIN    = "IS_AUTO_LOGIN";
    public static final String STORE_NO         = "STORE_NO";
    public static final String STORE_APPTOAPP_NO         = "STORE_APPTOAPP_NO";
    public static final String DIV_MM           = "DIV_MM";
    public static final String JAN_AMT          = "JAN_AMT";
    public static final String CREATED          = "CREATED";
    public static final String RESULT           = "RESULT";
    public static final String INDEX            = "INDEX";
    public static final String TITLE            = "TITLE";
    public static final String VALUE            = "VALUE";
    public static final String COUNT            = "COUNT";
    public static final String APV_DT           = "APV_DT";
    public static final String TOTAL_AMT        = "TOTAL_AMT";
    public static final String BILL_NO          = "BILL_NO";
    public static final String BILL_NO_REF      = "BILL_NO_REF";
    public static final String PAY_TYPE         = "PAY_TYPE";
    public static final String APV_NO           = "APV_NO";
    public static final String CARD_NO          = "CARD_NO";
    public static final String PAY_SEQ          = "PAY_SEQ";
    public static final String ROOT_TID         = "ROOT_TID";  // ISP에서 사용
    public static final String TID              = "TID";
    public static final String APPTOAPP_TID              = "APPTOAPP_TID";
    public static final String BIZ_GB           = "BIZ_GB";    // 소득공제 또는 증빙 지출
    public static final String APR_TYPE         = "APR_TYPE";
    public static final String CARD_CORP_NM     = "CARD_CORP_NM";
    public static final String ORG_BILL_NO      = "ORG_BILL_NO";
    public static final String ORG_APV_DT       = "ORG_APV_DT";
    public static final String APV_STATUS       = "APV_STATUS";
    public static final String MESSAGE          = "MESSAGE";
    public static final String APP_VERSION      = "version";
    public static final String PASSWORD         = "PASSWORD";
    public static final String VAT              = "VAT";  // 부가세
    public static final String SVC              = "SVC";  // 봉사료
    public static final String AMT              = "AMT";  // 공급가
    public static final String STORE_NM         = "STORE_NM";   //가게 이름
    public static final String PUR_CORP_NM      = "PUR_CORP_NM";
    public static final String OWNER_NM         = "OWNER_NM";   //주인 이름
    public static final String STORE_PHONE      = "STORE_PHONE";
    public static final String STORE_ADDR       = "STORE_ADDR";
    public static final String CARD_REG_NO      = "CARD_REG_NO";
    public static final String LAST_BACKUP_DATE = "LAST_BACKUP_DATE";
    public static final String SIGN_DATA        = "SIGN_DATA";
    public static final String IS_COMMING_RECEIPT_LIST = "IS_COMMING_RECEIPT_LIST";
    public static final String DOWNLOAD_PWD      = "DOWNLOAD_PWD";
    public static final String IS_NONE_SIGN_USED = "IS_NONE_SIGN_USED";
    public static final String NONE_SIGN_AMT     = "NONE_SIGN_AMT";
    public static final String TIME_OUT          = "TIME_OUT";
    public static final String POWER_OPTIOIN          = "POWER_OPTIOIN";
    public static final String NAME              = "NAME";
    public static final String ADDRESS           = "ADDRESS";
    public static final String LOCAL_NO          = "LOCAL_NO";
    public static final String URL               = "URL";
    public static final String BUFF_SW           = "BUFF_SW";
    public static final String VAN_PG            = "VAN_PG";
    public static final String DISPLAY_MSG       = "DISPLAY_MSG";
    public static final String SIGN_PATH         = "SIGN_PATH";
    public static final String CALLBACK_URL     = "CALLBACK_URL";
    public static final String PUR_CORP_CD      = "PUR_CORP_CD";
    public static final String CARD_CORP_CD     = "CARD_CORP_CD";
    public static final String ADDITION_INFO    = "ADDITION_INFO";
    public static final String UNIQUE_KEY       = "UNIQUE_KEY";
    public static final String IS_ISP           = "IS_ISP";
    public static final String SRC_SALE_ID      = "SALE_ID";
    public static final String SALE_TYPE        = "SALE_TYPE";
    public static final String REGIST_DEVICE_SN = "REGIST_DEVICE_SN";   //USB연결,BLE연결
    public static final String REGIST_DEVICE_VERSION = "REGIST_DEVICE_VERSION";   //UBLE 버전
    public static final String REGIST_DEVICE_NAME = "REGIST_DEVICE_NAME";   //BLE 이름
    public static final String REGIST_DEVICE_APPTOAPP_SN = "REGIST_DEVICE_APPTOAPP_SN";
    public static final String MULTI_STORE = "MULTI_STORE";     //복수가맹점인지 아닌지
    public static final String UNSIGNED_SETMONEY = "UNSIGNED_SETMONEY";     //결제설정의 무서명금액설정
    public static final String FALLBACK_USE = "FALLBACK_USE";     //결제설정의 폴백사용설정
    // 1
    public static final String ADDITION_INFO_YN = "ADDITION_INFO_YN";

    public static final String CARD             = "1";  // 카드    | PAY_TYPE
    public static final String CASH             = "2";  // 현금    | PAY_TYPE
    public static final String APPROVE          = "1";  // 승인    | APR_TYPE
    public static final String CANCEL           = "2";  // 취소하다 | APR_TYPE
    public static final String DEV_SERIAL_NO    = "DEV_SERIAL_NO";
    public static final String PMK_VALID_DATE   = "PMK_VALID_DATE";
    public static final String IS_SIGN_NEEDED   = "IS_SIGN_NEEDED";
    public static final String SIGN_PAD_TYPE    = "SIGN_PAD_TYPE";
    public static final String IC_READER_TYPE    = "IC_READER_TYPE";
    public static final String IS_VAT_USED      = "IS_VAT_USED";
    public static final String TAX_TYPE         = "TAX_TYPE";
    public static final String EXCLUDING_TAX         = "EXCLUDING_TAX";
    public static final String DUTY_FREE         = "DUTY_FREE";
    public static final String TAX_FREE_AMT         = "TAX_FREE_AMT";

    public static final String KEY_TOTAL_AMT        = "total_amt";
    public static final String KEY_AUTH_DATE_TIME = "auth_date_time";
    public static final String FINISH_OPTION = "FINISH_OPTION"; //

    public static final String ACTION_BXL_CONNECTED    = "ACTION_BXL_CONNECTED";
    public static final String ACTION_BXL_DISCONNECTED = "ACTION_BXL_DISCONNECTED";
    public static final String NONE_SIGNATURE          = "0";
    public static final String NO_USED_SIGN_PAD = "2";
    public static final String VAN_PHONE        = "VAN_PHONE";
    public static final String DIRECT_CONN_BLE  = "DIRECT_CONN_BLE";
    public static final String CALL_MAIN_NUMBER = "------------";


    //20200305 kim.jy
    public static final String UNIQUE_NUM = "UNIQUE_NUM";
    public static final int MAX_UNIQUE_COUNT = 999999;

    public static final String VAN_IP = "VAN_IP";
    public static final String VAN_PORT = "VAN_PORT";
    public static final String CAT_IP = "CAT_IP";
    public static final String CAT_PORT = "CAT_PORT";
    public static final String TMP_PORT = "TMP_PORT";
    public static final String DOWNLOAD_PORT = "DOWNLOAD_PORT";

    public static final int BLE_CONNECT_RETRY_COUNT = 3;    //블루투스 재연결 시도 카운트

    public static final String TRXN_NO = "TRXN_NO";     //트랜잭션 넘버 20180703 거래일련번호
    public static final String SALE_DATE = "SALE_DATE";
    public static final String LAST_CONNECTED_DEV_SN     = "LAST_CONNECTED_DEV_SN";     //마지막 연결된 장치의 시리얼넘버

    public static final String WORKING_KEY_INDEX = "01";    //비밀번호 또는 전자서명(KOCES 사인패드) 사용 시 설정 데이터에 null(0x30) 스페이스(0x20) 이 있으면 업데이트하지 말것
    public static final String WORKING_KEY = "B77F25FB72762DD3";    //암호화 및 해쉬코드 생성시 이용하는 Working Key
                                                                    // 길이 16. 길이 18자리 working key = WORKING_KEY_INDEX + WORKING_KEY 로 사용한다
//    public static final String CASHIC_RANDOM_NUMBER = "qwerasdfzxcvqwerasdfzxcvqwerasdf";   //현금IC 난수 길이 32자리
    public static final String CASHIC_RANDOM_NUMBER = "                                ";   //현금IC 난수 길이 32자리 - 임의값이 아닌 스페이스패딩으로 변경 보안인증이슈 jiw230223

    // Immediate alert constants
    public static final String VALUE_NO_ALERT = "0x00";
    public static final String VALUE_MID_ALERT = "0x01";
    public static final String VALUE_HIGH_ALERT = "0x02";

    //about device setting
    public static final String SELECTED_TRADE_OPTION = "SELECTED_TRADE_OPTION";
    public static final String SELECTED_CARD_READER_OPTION = "SELECTED_CARD_READER_OPTION";
    public static final String SELECTED_SIGN_PAD_OPTION = "SELECTED_SIGN_PAD_OPTION";
    public static final String SELECTED_DEVICE_CARD_READER_SERIAL = "SELECTED_CARD_READER_SERIAL";
    public static final String SELECTED_DEVICE_CARD_READER = "SELECTED_CARD_READER";
    public static final String SELECTED_DEVICE_MULTI_READER_SERIAL = "SELECTED_MULTI_READER_SERIAL";
    public static final String SELECTED_DEVICE_MULTI_READER = "SELECTED_MULTI_READER";
    public static final String SELECTED_DEVICE_SIGN_PAD = "SELECTED_SIGN_PAD";
    public static final String SELECTED_DEVICE_SIGN_PAD_SERIAL= "SELECTED_SIGN_PAD_SERIAL";
    public static final String SELECTED_DEVICE_MULTI_SIGN_PAD = "SELECTED_MULTI_SIGN_PAD";
    public static final String SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL = "SELECTED_MULTI_SIGN_PAD_SERIAL";
    public static final String SELECTED_DEVICE_SIGN_PAD_SIZE = "SELECTED_SIGN_PAD_SIZE";

    /** 어플 전체의 결제 방식에 대해서 프리퍼런스에 저장하는 KEY 이름 2021.11.24 kim.jy */
    public static final String APPLICATION_PAYMENT_DEVICE_TYPE = "APPLICATION_PAYMENT_DEVICE_TYPE";

    /** 최초 시작시 관련 퍼미션을 활용하기 위해 사용자에게 안내및 주의문구를 보여주는 팝업을 띄우기 위함(1회성) */
    public static final String APP_PERMISSION_CHECK = "APP_PERMISSION_CHECK";

    /** ble 연결시 선택한 ble의 이름과 ble 주소의  */
    public static final String BLE_DEVICE_NAME = "BLE_DEVICE_NAME";
    public static final String BLE_DEVICE_ADDR = "BLE_DEVICE_ADDR";

    /** 유선과 cat qr 데이터를 읽는 장비를 설정한다  */
    public static final String LINE_QR_READER = "LINE_QR_READER";
    public static final String CAT_QR_READER = "LINE_QR_READER";

    //about applicatin setting
    public static final String SELECTED_USE_POPUP = "SELECTED_USE_POPUP";
    public static final String SELECTED_NEW_NOTIFICATION = "SELECTED_NEW_NOTIFICATION";
    public static final String SELECTED_USE_BMPSIGN = "SELECTED_USE_BMPSIGN";
    public static final String BMP_SIGNDATA_FILEPATH = "/storage/emulated/0";

    //about payment setting
    public static final String SELECTED_USE_CLASSFI = "SELECTED_USE_CLASSFI";
    public static final String SELECTED_PAY_METHOD = "SELECTED_PAY_METHOD";
    public static final String SELECTED_REASON_CANCEL = "SELECTED_REASON_CANCEL";

    public static final String DEVICE_KEYUPDATE = "DEVICE_KEYUPDATE";
    public static final String DEVICE_INTERGRITY = "DEVICE_INTERGRITY";

    /** 키체인으로 부정취소방지 용 키 저장시 사용하는 키값. 여기에 뒤에다가 tid를 붙여서 키값으로 저장한다. */
    public static final String KeyChainAppToApp = "AppToApp";
    public static final String KeyChainOriginalApp = "OriginalApp";

    /** 프린트 설정 */
    public static String PRINT_CUSTOMER = "PRINT_CUSTOMER";  //고객용 프린트 출력설정
    public static String PRINT_STORE = "PRINT_STORE";    //가맹점용 프린트 출력설정
    public static String PRINT_CARD = "PRINT_CARD";      //카드사용 프린트 출력설정
    public static String PRINT_LOWLAVEL = "PRINT_LOWLAVEL";  //프린트 시 하단 문구 설정

    /** 터치서명 시 사이즈 */
    public static String BLE_SIGNPAD_USE = "BLE_SIGNPAD_USE";
    public static String SIGNPAD_SIZE = "SIGNPAD_SIZE";

    /** BLE 페어링 해제 후 전원유지 시간 */
    public static final String BLE_POWER_MANAGE          = "BLE_POWER_MANAGE";


    /** BLE프린터 관련 */
    public static String PCENTER = "__JYCE__";
    public static String PLEFT = "__JYLE__";
    public static String PRIGHT = "__JYRI__";
    public static String PBOLDSTART = "__JYBS__";
    public static String PBOLDEND = "__JYBE__";
    public static String PENTER = "___LF___";

    /** CAT프린터 관련 */
    public static byte[] Init = {0x1B, 0x40}; public static String PInit = "Init";   //초기화
    public static byte[] Font_HT = {0x09}; public static String PFont_HT = "Font_HT";  //수평 탭(HT)
    public static byte[] Font_LF = {0x0A}; public static String PFont_LF = "Font_LF";  //인쇄 및 한줄 내림(LF)
    public static byte[] Font_CR = {0x0D}; public static String PFont_CR = "Font_CR";  //인쇄 및 프린터 헤드를 라인의 시작위치로 이동
    public static byte[] Logo_Print = {0x1C, 0x70 ,0x01, 0x30}; public static String PLogo_Print = "Logo_Print"; //저장된 LOGO(prn 이미지)
    public static byte[] Cut_print = {0x1B ,0x69}; public static String PCut_print = "Cut_print";  //용지 커팅
    public static byte[] Money_Tong = {0x1B, 0x70 ,0x31, 0x01, 0x05}; public static String PMoney_Tong = "Money_Tong";   //금전함 열기
    public static byte[] Paper_up = {0x1B, 0x64}; public static String PPaper_up = "Paper_up";    //출력 시에 원하는 만큼 라인 공백 추가, n
    public static byte[] Font_Sort_L = {0x1B, 0x61, 0x30}; public static String PFont_Sort_L = "Font_Sort_L";  //폰트 좌측 정렬
    public static byte[] Font_Sort_C = {0x1B, 0x61, 0x31}; public static String PFont_Sort_C = "Font_Sort_C";  //폰트 중앙 정렬
    public static byte[] Font_Sort_R = {0x1B, 0x61, 0x32}; public static String PFont_Sort_R = "Font_Sort_R";  //폰트 우측 정렬
    public static byte[] Font_Default = {0x1D, 0x21, 0x00}; public static String PFont_Default = "Font_Default_0"; //기본 폰트 크기
    public static byte[] Font_Size_H = {0x1D, 0x21, 0x01}; public static String PFont_Size_H = "Font_Size_H";  //폰트 크기 세로 두배
    public static byte[] Font_Size_W = {0x1D ,0x21, 0x10}; public static String PFont_Size_W = "Font_Size_W";  //폰트 크기 가로 두배
    public static byte[] Font_Size_B = {0x1D, 0x21, 0x11}; public static String PFont_Size_B = "Font_Size_B";  //폰트 크기 전체 두배
    public static byte[] Font_Bold_0 = {0x1B, 0x45, 0x00}; public static String PFont_Bold_0 = "Font_Bold_0";  //폰트 굵기 기본
    public static byte[] Font_Bold_1 = {0x1B, 0x45, 0x01}; public static String PFont_Bold_1 = "Font_Bold_1";   //폰트 굵기 굵게
    public static byte[] Font_DS_0 = {0x1B, 0x47, 0x00}; public static String PFont_DS_0 = "Font_DS_0";    //더블-스트라이크 모드 해제
    public static byte[] Font_DS_1 = {0x1B, 0x47, 0x01}; public static String PFont_DS_1 = "Font_DS_1";    //더블-스트라이크 모드 설정
    public static byte[] Font_Udline_0 = {0x1B, 0x2D, 0x00}; public static String PFont_Udline_0 = "Font_Udline_0";     //밑줄 모드 해제
    public static byte[] Font_Udline_1 = {0x1B, 0x2D, 0x01}; public static String PFont_Udline_1 = "Font_Udline_1";     //밑줄 모드 설정
    public static byte[] Bar_Print_1 = {0x1D, 0x6B, 0x45}; public static String PBar_Print_1 = "Bar_Print_1";   //바코드 출력(CODE39), n : 바코드 데이터 입력 값 길이 계산 하여 입력 필요
    public static byte[] Bar_Print_2 = {0x1D, 0x6B, 0x48}; public static String PBar_Print_2 = "Bar_Print_2";  //바코드 출력(CODE93), n : 바코드 데이터 입력 값 길이 계산 하여 입력 필요
    public static byte[] Bar_Print_3 = {0x1D, 0x6B, 0x49}; public static String PBar_Print_3 = "Bar_Print_3";   //바코드 출력(CODE128), n : 바코드 데이터 입력 값 길이 계산 하여 입력 필요
    public static byte[] BarH_Size = {0x1D, 0x68}; public static String PBarH_Size = "BarH_Size";     //바코드의 높이 지정, n
    public static byte[] BarW_Size = {0x1D, 0x77}; public static String PBarW_Size = "BarW_Size";    //바코드의 넓이 지정, n
    public static byte[] Bar_Position_1 = {0x1D, 0x48, 0x01}; public static String PBar_Position_1 = "Bar_Position_1";   //바코드숫자 위치 위
    public static byte[] Bar_Position_2 = {0x1D, 0x48, 0x02}; public static String PBar_Position_2 = "Bar_Position_2";   //바코드숫자 위치 아래
    public static byte[] Bar_Position_3 = {0x1D, 0x48, 0x03}; public static String PBar_Position_3 = "Bar_Position_3";   //바코드숫자 위치 위아래
    public static byte[] Money = {0x1B, 0x70, 0x31, 0x01, 0x05}; public static String PMoney = "Money";   //금전함열기

    //2022.01.26 kim. 세금 관련 설정
    public static final String TAX_USE = "TAX_USE";
    public static final String TAX_AUTO ="TAX_AUTO";    //자동
    public static final String TAX_MANUAL = "TAX_MANUAL";   //통합 또는 수동 입력
    public static final String TAX_UNUSED = "TAX_UNUSED";
    public static final String TAX_INCLUDED = "TAX_INCLUDED";
    public static final String TAX_NOTINCLUDED = "TAX_NOTINCLUDED";

    public static final String TAX_VAT_USE = "TAX_VAT_USE";
    public static final String TAX_VAT_INCLUDE = "TAX_VAT_INCLUDE";
    public static final String TAX_VAT_METHOD = "TAX_VAT_METHOD";
    public static final String TAX_VAT_RATE = "TAX_VAT_RATE";
    public static final String TAX_SVC_USE = "TAX_SVC_USE";
    public static final String TAX_SVC_METHOD = "TAX_SVC_METHOD";
    public static final String TAX_SVC_INCLUDE = "TAX_SVC_INCLUDE";
    public static final String TAX_SVC_RATE = "TAX_SVC_RATE";
    public static final String TAX_TXF_USE = "TAX_TXF_USE";
    public static final String TAX_TXF_INCLUDE = "TAX_TXF_INCLUDE";
    public static final String TAX_TXF_RATE = "TAX_TXF_RATE";
    public static final String CREDIT_MIN_INSTALLMENT = "CREDIT_MIN_INSTALLMENT";   //할부 최소 금액
    public static final String CREDIT_USE_INSTALLMENT = "CREDIT_NOSIGN";    //무서명 거래 금액

    //USB, CAT, BLE 장비 타임아웃설정 셋팅
    public static final String USB_TIME_OUT = "USB_TIME_OUT";
    public static final String CAT_TIME_OUT = "CAT_TIME_OUT";
    public static final String BLE_TIME_OUT = "BLE_TIME_OUT";
    public static final int SERVER_TIME_OUT = 30;

    //BLE 장비 구분자. C1장비(구형) = KRE-C, C1장비(신형) = KREC, 조아장비 = KRE-Z
    public static final String C1_KRE_OLD_USE_PRINT = "KRE-C101";
    public static final String C1_KRE_OLD_NOT_PRINT = "KRE-C100";
    public static final String C1_KRE_NEW = "KREC";
    public static final String ZOA_KRE = "KRE-Z";
    public static final String KWANGWOO_KRE = "KMR-K";
    public static final String WSP = "WSP";
    public static final String WOOSIM = "WOOSIM";

    public static final String SERVICE_STRING_KWANGWOO = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    public static final String CHARACTERISTIC_WRITE_KWANGWOO = "49535343-8841-43F4-A8D4-ECBE34729BB3";
    public static final String CHARACTERISTIC_READ_KWANGWOO = "49535343-1E4D-4BD9-BA61-23C647249616";

    public static final String SERVICE_STRING_ZOA = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String CHARACTERISTIC_WRITE_ZOA = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String CHARACTERISTIC_READ_ZOA = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    public static final String SERVICE_STRING_C1_NEW_PRINT = "49324541-5211-FA30-4301-48AFD205E400";
    public static final String CHARACTERISTIC_WRITE_C1_NEW_PRINT = "49324541-5211-FA30-4301-48AFD205E401";
    public static final String CHARACTERISTIC_READ_C1_NEW_PRINT = "49324541-5211-FA30-4301-48AFD205E402";

    public static final String SERVICE_STRING_C1_OLD_PRINT = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    public static final String CHARACTERISTIC_WRITE_C1_OLD_PRINT = "49535343-1E4D-4BD9-BA61-23C647249616";
    public static final String CHARACTERISTIC_READ_C1_OLD_PRINT = "49535343-1E4D-4BD9-BA61-23C647249616";

    public static final String SERVICE_STRING_C1_OLD_NOT_PRINT = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    public static final String CHARACTERISTIC_WRITE_C1_OLD_NOT_PRINT = "49535343-1E4D-4BD9-BA61-23C647249616";
    public static final String CHARACTERISTIC_READ_C1_OLD_NOT_PRINT = "49535343-1E4D-4BD9-BA61-23C647249616";

    public static final String SERVICE_STRING_C1_NEW_NOT_PRINT = "";
    public static final String CHARACTERISTIC_WRITE_C1_NEW_NOT_PRINT = "";
    public static final String CHARACTERISTIC_READ_C1_NEW_NOT_PRINT = "";

    /** 현금IC 업무 구분 */
    public enum CashICBusinessClassification {
        Buy {
            @Override
            public String toString() {
                return "C10";
            }
        },
        Cancel {
            @Override
            public String toString() {
                return "C20";
            }
        },
        Search {
            @Override
            public String toString() {
                return "C30";
            }
        },
        BuySearch {
            @Override
            public String toString() {
                return "C40";
            }
        },
        CancelSearch {
            @Override
            public String toString() {
                return "C50";
            }
        }
    }

    /** 신용/현금영수증/현금IC/프린트 구분자 */
    public enum CatPayType {
        Credit {
            @Override
            public String toString() {
                return "Credit";
            }
        },
        Cash {
            @Override
            public String toString() {
                return "Cash";
            }
        },
        CashIC {
            @Override
            public String toString() {
                return "CashIC";
            }
        },
        Easy {
            @Override
            public String toString() {
                return "Easy";
            }
        },
        Print {
            @Override
            public String toString() {
                return "Print";
            }
        }
    }

    /** 각각의 간편결제 구분 */
    public enum EasyPayMethod {
        EMV {
            @Override
            public String toString() {
                return "EMV";
            }
        },
        Kakao {
            @Override
            public String toString() {
                return "Kakao";
            }
        },
        Zero_Bar {
            @Override
            public String toString() {
                return "Zero_Bar";
            }
        },
        Zero_Qr {
            @Override
            public String toString() {
                return "Zero_Qr";
            }
        },
        Wechat {
            @Override
            public String toString() {
                return "Wechat";
            }
        },
        Ali {
            @Override
            public String toString() {
                return "Ali";
            }
        },
        App_Card {
            @Override
            public String toString() {
                return "App_Card";
            }
        }
    }

    /** Line QR 리더 */
    public enum LineQrReader {
        Camera {
            @Override
            public String toString() {
                return "카메라";
            }
        },
        CardReader {
            @Override
            public String toString() {
                return "카드리더기";
            }
        },
        SignPad {
            @Override
            public String toString() {
                return "서명패드";
            }
        }
    }

    /** Line QR 리더 */
    public enum CatQrReader {
        Camera {
            @Override
            public String toString() {
                return "카메라";
            }
        },
        CatReader {
            @Override
            public String toString() {
                return "CAT단말기";
            }
        }
    }

    /** 터치서명 사인패드 크기 */
    public enum TouchSignPadSize {
        FullScreen {
            @Override
            public String toString() {
                return "전체";
            }
        },
        Middle {
            @Override
            public String toString() {
                return "보통";
            }
        },
        Small {
            @Override
            public String toString() {
                return "작게";
            }
        }
    }

    /** BLE 전원 유지 시간 설정 */
    public enum BlePowerManager {
        AllWays {
            @Override
            public String toString() {
                return "상시유지";
            }
        },
        FiveMinute {
            @Override
            public String toString() {
                return "5분";
            }
        },
        TenMinute {
            @Override
            public String toString() {
                return "10분";
            }
        },
        FifteenMinute {
            @Override
            public String toString() {
                return "15분";
            }
        },
        Twenty {
            @Override
            public String toString() {
                return "20분";
            }
        }
    }

}
