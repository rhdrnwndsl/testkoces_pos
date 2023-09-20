package com.koces.androidpos;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.koces.androidpos.sdk.BlePaymentSdk;
import com.koces.androidpos.sdk.Devices.AutoDetectDeviceInterface;
import com.koces.androidpos.sdk.Devices.AutoDetectDevices;
import com.koces.androidpos.sdk.EasyPaySdk;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.PaymentSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.StringUtil;
import com.koces.androidpos.sdk.TCPCommand;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.MyBleListDialog;
import com.koces.androidpos.sdk.ble.bleSdkInterface;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import com.koces.androidpos.sdk.CatPaymentSdk;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.woosim.printer.WoosimCmd;

/**
 * 앱투앱을 통해 가맹점/신용/현금 거래를 받아서 실행 후 결과 값을 다시 앱투앱으로 넘겨주는 Activity
 */
public class AppToAppActivity extends BaseActivity {

    /** 앱투앱에 해쉬맵을 보낼때 정상오류메세지일 경우 처리하는 키 */
    private final int RETURN_CANCEL = 1;
    /** 앱투앱에 해쉬맵을 보낼때 정상메세지일 경우 처리하는 키 */
    private final int RETURN_OK = 0;
    /** 앱투앱_취소메세지_대리점 등록 다운로드 글자수가 10자가 아닌 경우 */
    private final String ERROR_MCHSTRING_COUNT = "가맹점 등록 글자수는 10 자 입니다.";
    /** 앱투앱_취소메세지_등록된 TID가 없거나 등록된 TID와 다릅니다 */
    private final String ERROR_MCHTID_DIFF = "등록된 TID가 없거나 등록된 TID와 다릅니다.";
    /** 앱투앱_취소메세지_장치가 설정 되지 않았습니다 */
    private final String ERROR_NODEVICE = "장치가 설정 되지 않았습니다.";
    /** 앱투앱_취소메세지_취소 구분자가 없습니다 */
    private final String ERROR_EMPTY_CANCEL_REASON = "취소 구분자가 없습니다.";
    /** 앱투앱_취소메세지_고객번호가 없을시 현금영수증요청했는데 서명패드 미사용or터치사명일경우 */
    private final String ERROR_NOSIGNPAD = "고객 번호가 없거나 장치가 연결 되어 있지 않습니다.";
    /** 앱투앱 비엘이 장치에서 현금 거래영수증 번호 없어 , 키인으로 요청 한 경우에 에려 메세지 211230 kim.jy */
    private final String ERROR_NOBLEKEYIN = "고객번호가 없습니다. 고객번호를 포함하여 거래하여 주십시오.";
    /** 앱투앱_취소메세지_원승인번호 혹은 원승인일자가 없을경우 */
    private final String ERROR_NOAUDATE = "원승인번호 원승인일자가 없습니다.";
    /** 앱투앱_취소메세지_가맹점다운로드 시 필요데이터가 없는경우 */
    private final String ERROR_NODATA = "필요 데이터가 없습니다.";
    /** 앱투앱_취소메세지_네트워크 연결이 불량인 경우 */
    private final String ERROR_NO_NETWORK_SERVICE = "네트워크 연결 상태를 확인 해 주십시요";
    /** 앱투앱_취소메세지_사용하지않음 */
    private final String ERROR_NO_APP_RUNNING = "앱이 미실행 중입니다.";
    /** 앱투앱_취소메세지_사용하지않음 */
    private final String ERROR_NO_DEVICE_INIT = "장치교체 후 장치설정을 완료하지 않았습니다.";
    /** 앱투앱_취소메세지 */
    private final String ERROR_SET_ENVIRONMENT = "환경설정에서 장치설정을 해야합니다.";
    private final String ERROR_NOT_SUPPORT_DEVICE = "지원하지 않는 장비입니다";
    /** 앱투앱_취소메세지_전자서명데이터없음(2. bmp data 사용인데 데이터를 보내오지 않았다) */
    private final String ERROR_NO_BMPDATA = "BMP 데이터가 올바르지 않습니다";
    /** 앱투앱과 값을 주고받을 시 사용한다 */
    Intent mIntent;
    /** 실제 통신, 거래, 등의 연동이 정의된 곳 */
    KocesPosSdk mPosSdk;
    /** 거래 관련 함수들이 정의 된 곳 */
    PaymentSdk mPaymentSdk;
    /** 해당 엑티비티 인스턴스 */
    AppToAppActivity instance;
    /** 사인패드의 결과 값이 들어올 경우 이를 리시브 하기 위한 키 */
    private int REQUEST_SIGNPAD = 10001;
    /** TID */
    String mTermID = "";
    /** EOT취소를 위한 변수 */
    int mEotCancel=0;
    /** (테스트용으로 사용함. 현재 사용안함) 앱투앱에서 인텐트로 받아온 데이터를 해쉬맵으로 저장하여 활용한다 */
    HashMap<String,String> mhashMap;
    /** CAT 거래 관련 함수들이 정의 된곳 */
    CatPaymentSdk mCatPaymentSdk;

    /** BLE 거래 관련 함수들이 정의 된곳 */
    BlePaymentSdk mBlePaymentSdk;

    /** Easy 거래 관려s */
    EasyPaySdk mEasyPaymentSdk;

    /** 화면에 장치를 선택 했을 때의 번호 */
    int mSelectedDeviceIndex = -1;
    /** 화면 전체 linearlayout
     * 설정이 필요한 경우에만 화면에 표시 한다.*/
    LinearLayout mAppToAppLayout = null;

    /** QR스캔카메라연동 */
    IntentIntegrator intentIntegrator;

    String BillNo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_to_app);
        instance =this;
        mAppToAppLayout = (LinearLayout)findViewById(R.id.init_set_apptoapplayout);
        mAppToAppLayout.setVisibility(View.INVISIBLE);
        mIntent = new Intent(getIntent());
        HashMap<String, String> hashMap = (HashMap<String, String>) mIntent.getSerializableExtra("hashMap");
        if (hashMap == null) {
            ComponentName compName = new ComponentName("com.koces.androidpos", "com.koces.androidpos.MainActivity");
            Intent intent23 = new Intent(Intent.ACTION_MAIN);
            intent23.setFlags(Intent.FLAG_FROM_BACKGROUND);
            intent23.addCategory(Intent.CATEGORY_LAUNCHER);
            intent23.putExtra("AppToApp", 0);
            intent23.setComponent(compName);
            startActivity(intent23);
            finish();
            return;
        }
        /* 최초 실행 시 실행하여 메인엑티비티부터 구동하여 앱의 설정들을 정상적으로 셋팅한다. 해당 작업이 끝나고 나서 기타 요청작업을 실행한다 */
        if(Setting.g_bfirstexecAppToApp)
        {
            ComponentName compName = new ComponentName("com.koces.androidpos", "com.koces.androidpos.MainActivity");
            Intent intent23 = new Intent(Intent.ACTION_MAIN);
            intent23.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_FROM_BACKGROUND);
            intent23.addCategory(Intent.CATEGORY_LAUNCHER);
            intent23.putExtra("AppToApp", 1);
            intent23.setComponent(compName);
            startActivity(intent23);
        }
        Setting.setIsAppForeGround(2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 화면 꺼짐 방지
        //간편결제라고 설정을 다시 기본값으로 돌려놓는다
        Setting.setEasyCheck(false);
        int delayTime = 300;
        if(!Setting.g_bMainIntegrity)
        {
            delayTime = 10000;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run() {
                mPosSdk = KocesPosSdk.getInstance();
                mPosSdk.setFocusActivity(instance,appToAppUsbSerialStateListener);
//                mPosSdk.BleregisterReceiver(instance);
                mIntent = new Intent(getIntent());
                AppToAppAsyncTask mAsyncTask = new AppToAppAsyncTask(); /* asynctask 를 통해 초기 앱설정들(장비설정및 무결성검증포함)이 정상적으로 완료 될때까지 일정시간 대기한다 */
                mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        },delayTime);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPosSdk != null) {
            mPosSdk.BleUnregisterReceiver(this);
            mPosSdk.BleregisterReceiver(Setting.getTopContext());
        }

    }

    /** 거래 통신 중. 비정상상황으로 인한 오류메세지을 받는 리스너 */
    BaseActivity.AppToAppUsbSerialStateListener appToAppUsbSerialStateListener = new BaseActivity.AppToAppUsbSerialStateListener()
    {
        @Override
        public void onState(int _state) {
            HideDialog();
            switch (_state)
            {
                case -1:
                    SendreturnData(RETURN_CANCEL,null,"USB 장치로부터 아무 응답이 없습니다");
                    break;
                case -2:
                    SendreturnData(RETURN_CANCEL,null,"서버로 부터 아무 응답이 없습니다. 인터넷 연결을 확인 하십시요");
                    break;
                case -3:
                    SendreturnData(RETURN_CANCEL,null,"거래 중 카드 LRC 장애가 발생하였습니다");
                    break;
                case -4:
                    SendreturnData(RETURN_CANCEL,null,"거래 중 NAK 발생. 인터넷 연결을 확인 하십시오");
                    break;
                case -5:
                    SendreturnData(RETURN_CANCEL,null,"카드 대기시간 초과 다시 거래 해주세요");
                    break;
            }
        }
    };

    /** 퍼미션을 체크하여 정상적으로 권한을 받아왔다면 다음을 실행한다. */
    BaseActivity.PermissionCheckListener mPermissionCheckListener = new PermissionCheckListener() {
        @Override
        public void onResult(boolean _result) {
            if (_result == true)
            {
                //퍼미션 권한을 받아왔다.
                String recv_Command = mhashMap.get("TrdType");
                actAdd(instance);
                switch (recv_Command) {
                    case "D10":
                        MchDownload(mhashMap);
                        break;
                    case "A10":
                    case "A20":
                    case "Print":   //프린트
                    case "B10":
                    case "B20":
                    case "K21":
                    case "K22":
                    case "E10":
                    case "E20":
                    case "E30":
                    case "Q10":
                    case "Q20":
                    case "F10":
                    case "R10":
                    case "R20":
                    case "P10":
                    case "D20":
                        //TODO: 일단 주석처리 대기한다. 20-03-03. by.jiw
//                        if (needPermissionForBlocking(instance))
//                        {
//                            CheckDevice();
//                        }
//                        else
//                        {
//                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
//                        }

                        if(Setting.getPreference(instance, Constants.APP_PERMISSION_CHECK).equals(""))
                        {
                            CheckPermissionPage();
                        }
                        else
                        {
                            CheckDevice();
                        }
                        break;
//                    case "F10":
//                        AppToApp_ReCommand(mhashMap);
//                        break;
                    default:
                        break;
                }
            } else {
                //퍼미션 권한을 받아오지 못했다.
                final String finalTmp = "권한요청을 사용자가 승인하지 않았습니다";
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mPosSdk.getActivity(), finalTmp,Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(()->{
                            finishAndRemoveTask();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        },2000);
                    }
                });
            }
        }
    };

    /**
     * APP TO APP 데이터 수신시 TID 등록된 TID 인지 아닌지 검사 한다.
     */
    private boolean CheckTid(String _Tid)
    {
        if(_Tid.equals(""))
        {
            return false;
        }
        return Setting.CheckAppToAppTIDregistration(mPosSdk.getActivity(),_Tid);
    }

    /** 앱투앱으로 가맹점 의 인텐트데이터를 받아서 각 함수로 전달한다 */
    private void RecvMchIntentData()
    {
        if(mIntent!=null) {
            HashMap<String, String> hashMap = (HashMap<String, String>) mIntent.getSerializableExtra("hashMap");
            String recv_Command = hashMap.get("TrdType");
            String _tid;
            String _date;
            actAdd(instance);
            switch (recv_Command) {
                case "D10":
                    MchDownload(hashMap);
                    break;
                case "A10":
                case "A20":

                case "B10":
                case "B20":

                case "K21":
                case "K22":

                case "E10":
                case "E20":
                case "E30":
                    mhashMap = hashMap;
                    BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                    if(BillNo.length()>0 && BillNo.length() != 12)
                    {
                        SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                        return;
                    }
                    _tid = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                    _date = Utils.getDate("yyMMdd");
//                    if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid,_date,BillNo))
//                    {
//                        SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                        return;
//                    }

                    checkPermission(1,mPermissionCheckListener);

                    return;
                case "Q10":
                case "Q20":
                    mhashMap = hashMap;
                    BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                    if(BillNo.length()>0 && BillNo.length() != 12)
                    {
                        SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                        return;
                    }
                    checkPermission(1,mPermissionCheckListener);
                    return;
                case "Print":   //프린트
                case "P10":
                    mhashMap = hashMap;
                    BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                    if(BillNo.length()>0 && BillNo.length() != 12)
                    {
                        SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                        return;
                    }
                    _tid = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                    _date = Utils.getDate("yyMMdd");
                    if(BillNo.length()>0 && !mPosSdk.checkAppToAppTradeList(_tid,_date,BillNo))
                    {
                        SendreturnData(RETURN_CANCEL,null,"데이터가 없습니다");
                        return;
                    }

                    checkPermission(1,mPermissionCheckListener);

                    return;
                case "F10":
                    mhashMap = hashMap;
                    BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                    if(BillNo.length()>0 && BillNo.length() != 12)
                    {
                        SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                        return;
                    }
                    _tid = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                    _date = Utils.getDate("yyMMdd");
                    if(BillNo.equals(""))
                    {
                        SendreturnData(RETURN_CANCEL,null,"전표번호가 없습니다");
                        return;
                    }
                    AppToApp_ReCommand(mhashMap);
                    break;
                case "R10":
                case "R20":
                    mhashMap = hashMap;
                    BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                    if(BillNo.length()>0 && BillNo.length() != 12)
                    {
                        SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                        return;
                    }
                    _tid = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                    _date = Utils.getDate("yyMMdd");
//                    if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid,_date,BillNo))
//                    {
//                        SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                        return;
//                    }
                    AppToApp_VersionInfo(mhashMap);
                    break;
                case "D20":
                    mhashMap = hashMap;
                    AppToApp_KeyDownload(mhashMap);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 처음 시작하여 퍼미션에 관한 안내 페이지를 보여준다.
     * 처음에만 보여주고 다시 보이지 않는다.
     */
    private void CheckPermissionPage()
    {
        StartTermsSetDialog termsSetDialog = new StartTermsSetDialog(instance, new StartTermsSetDialog.DialogBoxListener() {
            @Override
            public void onClickConfirm() {
                StartPermissionSetDialog permissionSetDialog = new StartPermissionSetDialog(instance, new StartPermissionSetDialog.DialogBoxListener() {
                    @Override
                    public void onClickConfirm() {
                        Setting.setPreference(instance, Constants.APP_PERMISSION_CHECK,"APP_PERMISSION_CHECK");
                        CheckDevice();
                    }
                });
                permissionSetDialog.show();
            }
        });
        termsSetDialog.show();

    }

    private Setting.PayDeviceType getDeviceType() {

        String deviceType = Setting.getPreference(this, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        return Setting.g_PayDeviceType;
    }

    /**
     * 최초 실행하여 장비를 셋팅한다
     */
    private void CheckDevice()
    {
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        switch (Setting.g_PayDeviceType) {
            case NONE:
                //앱투앱이 아닌 본앱을 연상황
                //여기서 셋팅한다.
                showSelectDeviceMenu();
                return;
            case BLE:       //BLE의 경우
            case CAT:       //WIFI CAT의 경우
                String recv_Command = mhashMap.get("TrdType");
                //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
                // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면 해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다
                String recv_MtidYn = mhashMap.get("MtidYn") == null ? "":mhashMap.get("MtidYn");
                actAdd(instance);
                switch (recv_Command) {
                    case "D10":
                        MchDownload(mhashMap);
                        break;
                    case "K21":
                    case "K22":
                    case "E10":
                    case "E20":
                    case "E30":
                        if(recv_MtidYn != null && recv_MtidYn.equals("1") && Setting.g_PayDeviceType.equals(Setting.PayDeviceType.BLE))
                        {
                            MchDownload(mhashMap);
                            return;
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Easy(mhashMap);
                            }
                        },200);
                        break;
                    case "Q10":
                    case "Q20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                QR_Reader(mhashMap);
                            }
                        },200);
                        break;
                    case "A10":
                    case "A20":
                        if(recv_MtidYn != null && recv_MtidYn.equals("1") && Setting.g_PayDeviceType.equals(Setting.PayDeviceType.BLE))
                        {
                            MchDownload(mhashMap);
                            return;
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Credit(mhashMap);
                            }
                        },200);
                        break;
                    case "B10":
                    case "B20":
                        if(recv_MtidYn != null && recv_MtidYn.equals("1") && Setting.g_PayDeviceType.equals(Setting.PayDeviceType.BLE))
                        {
                            MchDownload(mhashMap);
                            return;
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Cash(mhashMap);
                            }
                        },200);
                        break;
                    case "Print":   //프린트
                    case "P10":

                        if (Setting.g_PayDeviceType == Setting.PayDeviceType.LINES)
                        {
                            ReadyToastShow(instance,"USB유선연결은 프린트 할 수 없습니다",1);
                            SendreturnData(RETURN_CANCEL,null,"USB유선연결은 프린트 할 수 없습니다");
                            HideDialog();
                        } else {

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        PrintRecipt(mhashMap);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 200);
                        }
                        break;
                    case "F10":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppToApp_ReCommand(mhashMap);
                            }
                        },200);

                        break;
                    case "R10":
                    case "R20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppToApp_VersionInfo(mhashMap);
                            }
                        },100);

                        break;
                    case "D20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppToApp_KeyDownload(mhashMap);
                            }
                        },100);
                        break;
                    default:
                        break;
                }
                return;
            case LINES:     //유선장치의 경우
                LineSet();
                break;
            default:
                ReadyToastShow(instance,"장치설정에 오류가 발생하였습니다. 앱을 제거 후 재설치해 주십시오.",1);
                SendreturnData(RETURN_CANCEL,null,"장치설정에 오류가 발생하였습니다. 앱을 제거 후 재설치해 주십시오.");
                HideDialog();
                return;
        }
    }

    /** 본앱을 최초로 실행하여 유선을 셋팅했는데 아직 카드리더기인지 멀티패드인지 셋팅이 되어 있지 않다. 여기서 환경설정에 들어가지 않고 다 해결해버린다 */
    private void LineSet()
    {
        HideDialog();
        //카드리더기인지 멀티패드인지
        String SelectedCardReaderOption = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_CARD_READER_OPTION);
        /* 현재 설정되어 있는 카드리더기(카드리더기OR멀티리더기)의 장치이름 */
        String SelectedCardReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL);
        String SelectedMultiReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL);

        if(SelectedCardReaderOption.equals("카드리더기"))
        {
            //카드리더기와 같을 때
            if (mPosSdk.getICReaderAddr2()[0] != null && !mPosSdk.getICReaderAddr2()[0].equals(""))
            {
                //카드리더기가 정상적으로 설정되어 있다. 아무것도 하지 않는다
                LineCheckComplete();
            } else {
                LineSetDialog mLineDialog = new LineSetDialog(this,  new LineSetDialog.DialogBoxListener() {
                    @Override
                    public void onClickCancel(String _msg) {
                        ReadyToastShow(instance,_msg,1);
                        SendreturnData(RETURN_CANCEL,null,_msg);
                        HideDialog();
                        return;
                    }

                    @Override
                    public void onClickConfirm(String _msg) {
                        LineCheckComplete();
                    }
                });
                mLineDialog.show();
                return;
            }
        }
        else if(SelectedCardReaderOption.equals("멀티패드"))
        {
            //멀티리더기와 같을 때
            if (mPosSdk.getMultiReaderAddr2()[0] != null && !mPosSdk.getMultiReaderAddr2()[0].equals(""))
            {
                //멀티리더기가 정상적으로 설정되어 있다. 아무것도 하지 않는다
                LineCheckComplete();
            } else {
                LineSetDialog mLineDialog = new LineSetDialog(this,  new LineSetDialog.DialogBoxListener() {
                    @Override
                    public void onClickCancel(String _msg) {
                        ReadyToastShow(instance,_msg,1);
                        SendreturnData(RETURN_CANCEL,null,_msg);
                        HideDialog();
                        return;
                    }

                    @Override
                    public void onClickConfirm(String _msg) {
                        LineCheckComplete();
                    }
                });
                mLineDialog.show();
                return;
            }
        } else {
            LineSetDialog mLineDialog = new LineSetDialog(this,  new LineSetDialog.DialogBoxListener() {
                @Override
                public void onClickCancel(String _msg) {
                    ReadyToastShow(instance,_msg,1);
                    SendreturnData(RETURN_CANCEL,null,_msg);
                    HideDialog();
                    return;
                }

                @Override
                public void onClickConfirm(String _msg) {
                    LineCheckComplete();
                }
            });
            mLineDialog.show();
            return;
        }


    }

    /** 유선연결이 정상적으로 연결 확인을 하였다 */
    private void LineCheckComplete()
    {
        String recv_Command = mhashMap.get("TrdType");
        //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
        // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면 해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다
        String recv_MtidYn = mhashMap.get("MtidYn") == null ? "":mhashMap.get("MtidYn");
        actAdd(instance);
        switch (recv_Command) {
            case "D10":
                MchDownload(mhashMap);
                break;
            case "K21":
            case "K22":
            case "E10":
            case "E20":
            case "E30":
                if(recv_MtidYn != null && recv_MtidYn.equals("1"))
                {
                    MchDownload(mhashMap);
                    return;
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Easy(mhashMap);
                    }
                },200);
                break;
            case "Q10":
            case "Q20":
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        QR_Reader(mhashMap);
                    }
                },200);
                break;
            case "A10":
            case "A20":
                if(recv_MtidYn != null && recv_MtidYn.equals("1"))
                {
                    MchDownload(mhashMap);
                    return;
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Credit(mhashMap);
                    }
                },200);
                break;
            case "B10":
            case "B20":
                if(recv_MtidYn != null && recv_MtidYn.equals("1"))
                {
                    MchDownload(mhashMap);
                    return;
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Cash(mhashMap);
                    }
                },200);
                break;
            case "Print":   //프린트
            case "P10":
                if (Setting.g_PayDeviceType == Setting.PayDeviceType.LINES)
                {
                    ReadyToastShow(instance,"USB유선연결은 프린트 할 수 없습니다",1);
                    SendreturnData(RETURN_CANCEL,null,"USB유선연결은 프린트 할 수 없습니다");
                    HideDialog();
                    return;
                } else {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PrintRecipt(mhashMap);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 200);
                }
                break;
            case "F10":
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppToApp_ReCommand(mhashMap);
                    }
                },200);

                break;
            case "R10":
            case "R20":
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppToApp_VersionInfo(mhashMap);
                    }
                },100);

                break;
            case "D20":
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppToApp_KeyDownload(mhashMap);
                    }
                },100);
                break;
            default:
                break;
        }
    }


    /** 화면에 장치 선택 화면을 표시하고 관련 처리를 한다.
     * 211222 kim.jy 추가
     * */
    private void showSelectDeviceMenu(){
        LinearLayout layoutLines,layoutBle,layoutCat;
        ImageView imgLines,imgBle,imgCat;
        Button btnSave;

        layoutLines = (LinearLayout)findViewById(R.id.apptoapp_linearLayout_lines);
        layoutBle = (LinearLayout)findViewById(R.id.apptoapp_linearLayout_ble);
        layoutCat = (LinearLayout)findViewById(R.id.apptoapp_linearLayout_cat);

        imgLines = (ImageView)findViewById(R.id.apptoapp_imgbtn_lines);
        imgBle = (ImageView)findViewById(R.id.apptoapp_imgbtn_ble);
        imgCat = (ImageView)findViewById(R.id.apptoapp_imgbtn_cat);

        btnSave = (Button)findViewById(R.id.apptoapp_btn_setok);
        btnSave.setVisibility(View.INVISIBLE);

        Setting.setPreference(instance,Constants.SELECTED_SIGN_PAD_OPTION,"터치서명");

        imgBle.setOnClickListener(v->{
            layoutLines.setBackgroundResource(R.drawable.rectangle);
            layoutBle.setBackgroundResource(R.drawable.main_selected_background);
            layoutCat.setBackgroundResource(R.drawable.rectangle);
            mSelectedDeviceIndex = 1;
            btnSave.setVisibility(View.VISIBLE);
        });
        imgCat.setOnClickListener(v->{
            layoutLines.setBackgroundResource(R.drawable.rectangle);
            layoutBle.setBackgroundResource(R.drawable.rectangle);
            layoutCat.setBackgroundResource(R.drawable.main_selected_background);
            mSelectedDeviceIndex = 2;
            btnSave.setVisibility(View.VISIBLE);
        });
        imgLines.setOnClickListener( v->{
            layoutLines.setBackgroundResource(R.drawable.main_selected_background);
            layoutBle.setBackgroundResource(R.drawable.rectangle);
            layoutCat.setBackgroundResource(R.drawable.rectangle);
            mSelectedDeviceIndex = 3;
            btnSave.setVisibility(View.VISIBLE);
        });

        mAppToAppLayout.setVisibility(View.VISIBLE);

        final boolean[] _btnCheck = {false};
        btnSave.setOnClickListener( v-> {
            if (_btnCheck[0])
            {
                return;
            }
            _btnCheck[0] = true;
            switch (mSelectedDeviceIndex) {
                case 1:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    Setting.setPreference(mPosSdk.getActivity(),Constants.BLE_TIME_OUT,"20");
                    mAppToAppLayout.setVisibility(View.INVISIBLE);
                    break;
                case 2:
                    mAppToAppLayout.setVisibility(View.INVISIBLE);
                    CatIPSet(); //캣이라면 ip/port 를 설정해야 한다.
//                                    Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
//                                    Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    return;
                case 3:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.LINES;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    Setting.setPreference(mPosSdk.getActivity(),Constants.USB_TIME_OUT,"30");
                    mAppToAppLayout.setVisibility(View.INVISIBLE);
                    LineSet();
                    return;
                default:
                    Toast.makeText(getApplicationContext(),"연결 하실 장치를 선택해 주세요.",Toast.LENGTH_LONG).show();
                    break;
            }

            ///BLE 셋팅은 여기서 한다
            String recv_Command = mhashMap.get("TrdType");
            //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
            // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면 해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다
            String recv_MtidYn = mhashMap.get("MtidYn") == null ? "":mhashMap.get("MtidYn");
            actAdd(instance);
            switch (recv_Command) {
                case "D10":
                    MchDownload(mhashMap);
                    break;
                case "K21":
                case "K22":
                case "E10":
                case "E20":
                case "E30":
                    if(recv_MtidYn != null && recv_MtidYn.equals("1"))
                    {
                        MchDownload(mhashMap);
                        return;
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Easy(mhashMap);
                        }
                    },200);
                    break;
                case "Q10":
                case "Q20":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            QR_Reader(mhashMap);
                        }
                    },200);
                    break;
                case "A10":
                case "A20":
                    if(recv_MtidYn != null && recv_MtidYn.equals("1"))
                    {
                        MchDownload(mhashMap);
                        return;
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Credit(mhashMap);
                        }
                    },200);
                    break;
                case "B10":
                case "B20":
                    if(recv_MtidYn != null && recv_MtidYn.equals("1"))
                    {
                        MchDownload(mhashMap);
                        return;
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Cash(mhashMap);
                        }
                    },200);
                    break;
                case "Print":   //프린트
                case "P10":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PrintRecipt(mhashMap);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    },200);
                    break;
                case "F10":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppToApp_ReCommand(mhashMap);
                        }
                    },200);

                    break;
                case "R10":
                case "R20":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppToApp_VersionInfo(mhashMap);
                        }
                    },100);

                    break;
                case "D20":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppToApp_KeyDownload(mhashMap);
                        }
                    },100);
                    break;
                default:
                    break;
            }
        });

    }

//    AlertDialog catDialog;
    /** 다른 장치를 선택하면 그대로 진행하지만 만일 CAT 을 선택했다면 ip/port 를 설정해야한다 */
    private void CatIPSet()
    {
        CatSetDialog mCatSet = new CatSetDialog(this, new CatSetDialog.DialogBoxListener() {
            @Override
            public void onClickCancel(String _msg) {
                ReadyToastShow(instance,_msg,1);
                SendreturnData(RETURN_CANCEL,null,_msg);
                HideDialog();
                return;
            }

            @Override
            public void onClickConfirm(String _msg) {
                Toast.makeText(instance,_msg,Toast.LENGTH_SHORT).show();
                String recv_Command = mhashMap.get("TrdType");
                //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
                // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면 해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다
                // CAT 일 경우 다중사업자 하지 않는다.
                String recv_MtidYn = mhashMap.get("MtidYn") == null ? "":mhashMap.get("MtidYn");
                actAdd(instance);
                switch (recv_Command) {
                    case "D10":
                        MchDownload(mhashMap);
                        break;
                    case "K21":
                    case "K22":
                    case "E10":
                    case "E20":
                    case "E30":
//                        if(recv_MtidYn != null && !recv_MtidYn.equals(""))
//                        {
//                            MchDownload(mhashMap);
//                            return;
//                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Easy(mhashMap);
                            }
                        },200);
                        break;
                    case "Q10":
                    case "Q20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                QR_Reader(mhashMap);
                            }
                        },200);
                        break;
                    case "A10":
                    case "A20":
//                        if(recv_MtidYn != null && !recv_MtidYn.equals(""))
//                        {
//                            MchDownload(mhashMap);
//                            return;
//                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Credit(mhashMap);
                            }
                        },200);
                        break;
                    case "B10":
                    case "B20":
//                        if(recv_MtidYn != null && !recv_MtidYn.equals(""))
//                        {
//                            MchDownload(mhashMap);
//                            return;
//                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Cash(mhashMap);
                            }
                        },200);
                        break;
                    case "Print":   //프린트
                    case "P10":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    PrintRecipt(mhashMap);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        },200);
                        break;
                    case "F10":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppToApp_ReCommand(mhashMap);
                            }
                        },200);

                        break;
                    case "R10":
                    case "R20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppToApp_VersionInfo(mhashMap);
                            }
                        },100);

                        break;
                    case "D20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppToApp_KeyDownload(mhashMap);
                            }
                        },100);
                        break;
                    default:
                        break;
                }
            }
        });
        mCatSet.show();
        return;

//        LayoutInflater li = LayoutInflater.from(getApplicationContext());
//        View promptsView = li.inflate(R.layout.dialog_device_set, null);
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,R.style.MyDialogTheme);
//
//
//        // set alert_dialog.xml to alertdialog builder
//        alertDialogBuilder.setView(promptsView);
//
//        final EditText edit_cat_ip = (EditText) promptsView.findViewById(R.id.edit_cat_ip);
//        final EditText edit_cat_port = (EditText) promptsView.findViewById(R.id.edit_cat_port);
//
//        // set dialog message
//        alertDialogBuilder
//                .setCancelable(false)
//                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        // get user input and set it to result
//                        // edit text
//                        if (edit_cat_ip.getText() == null || edit_cat_ip.getText().toString().equals(""))
//                        {
//                            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
//                            Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
//                            Setting.setCatVanPORT(instance,"");
//                            Setting.setCatIPAddress(instance, "");
//                            ReadyToastShow(instance,"CAT IP 설정이 잘못되었습니다",1);
//                            SendreturnData(RETURN_CANCEL,null,"CAT IP 설정이 잘못되었습니다");
//                            HideDialog();
//                            return;
//                        }
//                        if (edit_cat_port.getText() == null || edit_cat_port.getText().toString().equals(""))
//                        {
//                            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
//                            Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
//                            Setting.setCatVanPORT(instance,"");
//                            Setting.setCatIPAddress(instance, "");
//                            ReadyToastShow(instance,"CAT PORT 설정이 잘못되었습니다",1);
//                            SendreturnData(RETURN_CANCEL,null,"CAT PORT 설정이 잘못되었습니다");
//                            HideDialog();
//                            return;
//                        }
//
//                        Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
//                        Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
//                        Setting.setCatVanPORT(instance, edit_cat_port.getText().toString());
//                        Setting.setCatIPAddress(instance, edit_cat_ip.getText().toString());
//                        String recv_Command = mhashMap.get("TrdType");
//                        actAdd(instance);
//                        switch (recv_Command) {
//                            case "D10":
//                                MchDownload(mhashMap);
//                                catDialog.dismiss();
//                                break;
//                            case "A10":
//                            case "A20":
//
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Credit(mhashMap);
//                                        catDialog.dismiss();
//                                    }
//                                },200);
//                                break;
//                            case "B10":
//                            case "B20":
//
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Cash(mhashMap);
//                                        catDialog.dismiss();
//                                    }
//                                },200);
//                                break;
//                            case "Print":   //프린트
//
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        PrintRecipt(mhashMap);
//                                        catDialog.dismiss();
//                                    }
//                                },200);
//                                break;
//                            default:
//                                break;
//                        }
//
//                    }
//                });
//
//        // create alert dialog
//        catDialog = alertDialogBuilder.create();
//
//        // show it
//        catDialog.show();
    }


    /**
     * 앱투앱_신용거래를 실행한다
     * @param _hashMap
     */
    private synchronized void Credit(HashMap<String,String> _hashMap)
    {
        if(_hashMap.get("QrKind") != null && !_hashMap.get("QrKind").equals(""))
        {
            Easy(_hashMap);
            return;
        }
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                ReadyToastShow(instance,"환경설정에 장치 설정을 해야 합니다.",1);
                SendreturnData(RETURN_CANCEL,null,ERROR_SET_ENVIRONMENT);
                HideDialog();
                return;
            case BLE:       //BLE의 경우
                String TrdCode = _hashMap.get("TrdCode");
                String TradeNo = _hashMap.get("TradeNo");
//                mPosSdk.BleregisterReceiver(this);
                if(TrdCode.equals("T") && !TradeNo.equals(""))
                {
                    BleCredit(_hashMap);
                } else {
                    SetBle();
                }
//                BleCredit(_hashMap);

                return;
            case CAT:       //WIFI CAT의 경우
                CatCredit(_hashMap);
                return;
            case LINES:     //유선장치의 경우
                break;
            default:
                break;
        }
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");                       //단말기 ID
        String AuDate = _hashMap.get("AuDate");                 //원거래일자 YYMMDD
        String AuNo = _hashMap.get("AuNo");                     //원승인번호
        String KeyYn = _hashMap.get("KeyYn");                   //입력방법 K=keyin, S-swipe, I=ic
        String TrdAmt = _hashMap.get("TrdAmt");                 //거래금액 승인:공급가액, 취소:원승인거래총액
        String TaxAmt = _hashMap.get("TaxAmt");                 //세금
        String SvcAmt = _hashMap.get("SvcAmt");                 //봉사료
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");         //비과세
        String Month = _hashMap.get("Month");                   //할부개월(현금은 X)
        String MchData = _hashMap.get("MchData");               //가맹점데이터
        Setting.setMchdata(MchData);
        String TrdCode = _hashMap.get("TrdCode");               //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        String TradeNo = _hashMap.get("TradeNo");               //Koces거래고유번호(거래고유키 취소 시 사용)
        String CompCode = _hashMap.get("CompCode");             //업체코드(koces에서 부여한 업체코드)
        String DgTmout = _hashMap.get("DgTmout");               //카드입력대기시간 10~99, 거래요청 후 카드입력 대기시간
        String DscYn = _hashMap.get("DscYn");                   //전자서명사용여부(현금X) 0:무서명 1:전자서명 2: bmp data 사용
        Setting.setDscyn(DscYn);
        String DscData = _hashMap.get("DscData");               //전자서명 데이터 위의 전자서명여부 2를 선택시 사용한다
        Setting.setDscData(DscData);
        String FBYn = _hashMap.get("FBYn");                     //fallback 사용 0:fallback 사용 1: fallback 미사용
        String QrNo = _hashMap.get("QrNo");                     //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        String InsYn = _hashMap.get("InsYn");                   //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
        String CancelReason = _hashMap.get("CancelReason");     //취소사유(신용X) 현금영수증 취소 시 필수 1:거래취소 2:오류발급 3:기타
        String CashNum = _hashMap.get("CashNum");               //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호

        if(!CheckTid(mTermID))  //Tid체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
            HideDialog();
            return;
        }

        if(TrdCode.equals("T") && !TradeNo.equals(""))
        {

        } else {
            if(mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getICReaderAddr().equals(""))
            {

                if (mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getICReaderAddr()))
                {

                }
                else
                {
                    if (mAutoCount==0)
                    {
                        DeviceReScan(true);
                        return;
                    }
                    else
                    {
                        SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                        HideDialog();
                        return;
                    }
                }

            }
            else if(mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getMultiReaderAddr().equals(""))
            {
                if(mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getMultiReaderAddr()))
                {

                }
                else
                {
                    if (mAutoCount==0)
                    {
                        DeviceReScan(true);
                        return;
                    }
                    else
                    {
                        SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                        HideDialog();
                        return;
                    }
                }

            }
            else
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                HideDialog();
                return;
            }
//            if(!CheckDeviceState(1))    //장치설정체크
//            {
//                //SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE + DebugWebComePadTempString());
//                SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
//                HideDialog();
//                return;
//            }
        }


        if(DscYn != null && DscYn.equals("2"))
        {
            if (DscData == null || DscData.equals("") || DscData.length() != 2172)
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NO_BMPDATA);
                HideDialog();
                return;
            }
        }

//        if(_Command.equals("A20") && TrdCode.equals("")) //취소 커맨드에 취소 구분자가 없는 경우
//        {
//            SendreturnData(RETURN_CANCEL,null,ERROR_EMPTY_CANCEL_REASON);
//            HideDialog();
//            return;
//        }

        /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
        mPaymentSdk = new PaymentSdk(1,true,((result, Code, resultData) -> {
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        }));

        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (TaxFreeAmt == null || TaxFreeAmt.equals(""))
        {
            TaxFreeAmt = "0";
        }
        if (TrdAmt == null || TrdAmt.equals(""))
        {
            TrdAmt = "0";
        }
        int mTrdAmt = Integer.valueOf(TrdAmt) + Integer.valueOf(TaxFreeAmt);
        TrdAmt = String.valueOf(mTrdAmt);

        String CanRes = "";
        if(TrdCode.equals("t") || TrdCode.equals("T"))
        {
            TrdCode = "a";
        } else {
            TrdCode = "0";
        }
        CanRes = TrdCode + AuDate + AuNo;

        /* 로그기록. 신용거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "카드입력대기시간 : " + DgTmout + "," + "전자서명사융유무 : " + DscYn + "," + "fallback사융유무 : " + FBYn;
        cout("RECV : CREDIT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);

//        if(DgTmout.equals("") || DgTmout.equals("0"))   /* 앱투앱으로 결제타임아웃시간을 받아와서 이를 셋팅해준다 */
//        {
//            Setting.setTimeOutValue(30);
//            ReadyDialog("IC를 요청 중입니다.",30, true);
//        }
//        else
//        {
//            Setting.setCommandTimeOut(Integer.valueOf(DgTmout));
//            Setting.setTimeOutValue(Integer.valueOf(DgTmout));
//            ReadyDialog("IC를 요청 중입니다.",Integer.valueOf(DgTmout), true);
//        }
        Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)));
        Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)));
        ReadyDialog("IC카드를 넣어주세요",Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)), true);

        //만일 거래고유키취소사용시 코세스거래고유번호사용
        if(TrdCode.equals("a") && _Command.equals("A20"))
        {
            if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                HideDialog();
                return;
            }
            Setting.g_sDigSignInfo = "4";   //거래고유키 취소일 때 설정한다
            /* 거래고유키 취소시 앱투앱엑티비티에서 처리한다 */
            mPosSdk.___ictrade(TCPCommand.CMD_ICTRADE_CANCEL_REQ, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRes, "K", "", "", null,
                    TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, "", Month, "", "", " ", "", "", null,
                    "", "", "", "", "", "", "", "", "", null,
                    Setting.g_sDigSignInfo, "", "", null, "", MchData, TradeNo, CompCode,
                    Utils.getMacAddress(this), Utils.getHardwareKey(this,true,mTermID) , new TcpInterface.DataListener() {
                        @Override
                        public void onRecviced(byte[] _rev) {

                            int _tmpEot = 0;
                            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
                            {
                                if (Setting.getEOTResult() != 0) {
                                    break;
                                }
                                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                                _tmpEot ++;
                                if(_tmpEot >= 30)
                                {
                                    break;
                                }
                            }
                            KByteArray _b = new KByteArray(_rev);
                            _b.CutToSize(10);
                            String _responsecode = new String(_b.CutToSize(3));

                            Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                            String code = new String(tp.getResData().get(0));
                            String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //신용승인번호
                            String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //koces거래고유번호

                            String tmpCardNum1 = new String(tp.getResData().get(4));
                            String[] tmpCardNum2 = tmpCardNum1.split("-");
                            tmpCardNum1 = "";
                            for(String n:tmpCardNum2)
                            {
                                tmpCardNum1 += n;
                            }

                            /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리  */
                            byte[] _CardNum = tmpCardNum1.getBytes();
                            String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum);
                            StringBuffer sb = new StringBuffer();
                            sb.append(tmpCardNum1);
                            if (_CardNum.length > 12) {
                                sb.replace(8, 12, "****");
                            }
                            if (tmpCardNum1.indexOf("=") > 0) {
                                sb.replace(tmpCardNum1.indexOf("="), tmpCardNum1.indexOf("=") + 1, "*");
                            }

                            CardNo = sb.toString();

//                            byte[] _CardNum = tmpCardNum1.getBytes();
//
//                            if(_CardNum != null){
//                                //System.arraycopy( data.get(4),0,_CardNum,0,6);
//                                for(int i=8; i<_CardNum.length; i++){
//                                    //* = 0x2A
//                                    _CardNum[i] = (byte)0x2A;
//                                }
//                                //카드번호가 올라오지만 그중 6개만 사용하고 나머지는 *로 바꾼다
//                            }
//
//                            String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //출력용카드번호

                            //카드 정보 삭제____________________________________________________________________________________
                            Random rand = new Random();
                            for(int i=0; i<_CardNum.length; i++){
                                //* = 0x2A
                                _CardNum[i] = (byte)rand.nextInt(255);;
                            }
                            tp.getResData().set(4,_CardNum);
                            Arrays.fill(_CardNum,(byte)0x01);
                            tp.getResData().set(4,_CardNum);
                            Arrays.fill(_CardNum,(byte)0x00);
                            tp.getResData().set(4,_CardNum);

                            String card_type = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6)); //카드종류
                            String issuer_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7)); //발급사코드
                            String issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8),"EUC-KR"); //발급사명
                            String[] issuer_names = issuer_name.split(" ");
                            issuer_name = "";
                            for(String n:issuer_names)
                            {
                                issuer_name += n;
                            }
                            String purchaser_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9)); //매입사코드
                            String purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10),"EUC-KR"); //매입사명
                            String[] purchaser_names = purchaser_name.split(" ");
                            purchaser_name = "";
                            for(String n:purchaser_names)
                            {
                                purchaser_name += n;
                            }
                            String ddc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11)); //DDC 여부
                            String edc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12)); //EDC 여부

                            String giftcard_money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(15)); //기프트카드 잔액
                            String merchant_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(16)); //가맹점번호
                            String encryp_key_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(25)); //암호키만료잔여일
                            String merchant_data = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(26)); //가맹점데이터

                            HashMap<String, String> sendData = new HashMap<String, String>();

                            sendData.put("TrdType", tp.getResponseCode());
                            sendData.put("TermID", tp.getTerminalID());
                            sendData.put("TrdDate", tp.getDate());
                            sendData.put("AnsCode", code);

                            sendData.put("AuNo", authorizationnumber);
                            sendData.put("TradeNo", kocesTradeUniqueNumber);
                            if(CardNo.length()>8)
                            {
                                sendData.put("CardNo", CardNo.substring(0,8));
                            }
                            else
                            {
                                sendData.put("CardNo", CardNo);
                            }
                            sendData.put("Keydate", encryp_key_expiration_date);
                            sendData.put("MchData", merchant_data);
                            sendData.put("CardKind", card_type); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                            sendData.put("OrdCd", issuer_code);   //발급사코드
                            sendData.put("InpCd", purchaser_code);   //매입사코드
                            sendData.put("OrdNm",issuer_name);   //발급사명
                            sendData.put("InpNm",purchaser_name);   //매입사명
                            sendData.put("DDCYn", ddc_status);   //DDC 여부
                            sendData.put("EDCYn", edc_status);   //EDC 여부
                            sendData.put("GiftAmt", giftcard_money); //기프트카드 잔액
                            sendData.put("MchNo", merchant_number);   //가맹점번호
                            try {
                                sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                            } catch (UnsupportedEncodingException ex) {

                            }
                            /* 로그기록 거래고유키취소 결과 */
                            String mLog ="전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + encryp_key_expiration_date + "," +
                                    "가맹점데이터 : " + merchant_data + "," + "카드종류 : " + card_type + "," + "발급사코드 : " + issuer_code + "," + "발급사명 : " + issuer_name + "," +
                                    "메입사코드 : " + purchaser_code + "," + "메입사명 : " + purchaser_name + "," + "DDC여부 : " + ddc_status + "," + "EDC여부 : " + edc_status + "," +
                                    "기프트잔액 : " + giftcard_money + "," + "가맹점번호 : " + merchant_number;
                            cout("SEND : CREDIT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                            SendreturnData(RETURN_OK,sendData,null);
//                            if(code.equals("0000")) {
//                                String mLog ="전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + encryp_key_expiration_date + "," +
//                                        "가맹점데이터 : " + merchant_data + "," + "카드종류 : " + card_type + "," + "발급사코드 : " + issuer_code + "," + "발급사명 : " + issuer_name + "," +
//                                        "메입사코드 : " + purchaser_code + "," + "메입사명 : " + purchaser_name + "," + "DDC여부 : " + ddc_status + "," + "EDC여부 : " + edc_status + "," +
//                                        "기프트잔액 : " + giftcard_money + "," + "가맹점번호 : " + merchant_number;
//                                cout("SEND : CREDIT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                SendreturnData(RETURN_OK,sendData,null);
//
//                            }
//                            else
//                            {
//                                SendreturnData(RETURN_OK,sendData,null);
////                                SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                            }

                        }
                    });
        }
        else
        {
            if(_Command.equals("A10"))
            {
                /* 신용요청인경우_A10 */
                mPaymentSdk.CreditIC(this,mTermID, TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
                        ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),Month, "", "", MchData, TradeNo, CompCode, FBYn,
                        "","","","","");
            }
            else
            {
                if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
                {
                    SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                    HideDialog();
                    return;
                }
                /* 신용취소인경우_A20 */
                mPaymentSdk.CreditIC(this,mTermID, TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
                        ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),Month,AuDate,CanRes, MchData, TradeNo, CompCode, FBYn,
                        "","","","","");
            }

        }
    }

    /**
     * 앱투앱_현금거래를 실행한다
     * @param _hashMap
     */
    private synchronized void Cash(HashMap<String,String> _hashMap)
    {
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                ReadyToastShow(instance,"환경설정에 장치 설정을 해야 합니다.",1);
                SendreturnData(RETURN_CANCEL,null,ERROR_SET_ENVIRONMENT);
                HideDialog();

                return;
            case BLE:       //BLE의 경우
                String TrdCode = _hashMap.get("TrdCode");
                String TradeNo = _hashMap.get("TradeNo");
//                mPosSdk.BleregisterReceiver(this);
                if(TrdCode.equals("T") && !TradeNo.equals(""))
                {
                    BleCash(_hashMap);
                    return;
                }
//                BleCash(_hashMap);
                String CashNum = _hashMap.get("CashNum"); //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
                if (!CashNum.equals(""))
                {
                    BleCash(_hashMap);
                } else {
                    SetBle();
                }

                return;
            case CAT:       //WIFI CAT의 경우
                CatCash(_hashMap);
                return;
            case LINES:     //유선장치의 경우
                break;
            default:
                break;
        }

        //ReadyDialog("현금 영수증 처리중입니다.",30, false);
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");
        String AuDate = _hashMap.get("AuDate");
        String AuNo = _hashMap.get("AuNo");
        String Input = _hashMap.get("KeyYn");
        String TrdAmt = _hashMap.get("TrdAmt");
        String TaxAmt = _hashMap.get("TaxAmt");
        String SvcAmt = _hashMap.get("SvcAmt");
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");
        String Month = _hashMap.get("Month");
        String MchData = _hashMap.get("MchData");
        String TrdCode = _hashMap.get("TrdCode");
        String TradeNo = _hashMap.get("TradeNo");
        String CompCode = _hashMap.get("CompCode");
        String DgTmout = _hashMap.get("DgTmout");
        String DscYn = _hashMap.get("DscYn");
        String FBYn = _hashMap.get("FBYn");
        String QrNo = _hashMap.get("QrNo");
        String InsYn = _hashMap.get("InsYn"); //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
        String CancelReason = _hashMap.get("CancelReason"); //취소사유(신용X) 현금영수증 취소 시 필수 1:거래취소 2:오류발급 3:기타
        String CashNum = _hashMap.get("CashNum"); //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        if(CashNum.length()> 12) {
            CashNum = CashNum.substring(0, 13);
        }
        //만일 현금영수증 자진발급(3)일 경우 자진발급번호로 즉시 거래를 진행한다.결제방법을 카드리더기 선택이거나 입력된 번호가 있더라도 모두 무시하고
        //자진발급번호(고객번호) "0100001234" 로 진행한다. 신분확인번호(CashNum)"0100001234" 입력방법(KeyYn)"K" 개인/법인(InsYn) 구분:3
        if(InsYn.equals("3"))
        {
            CashNum = "0100001234";
            Input = "K";
        }
        String KeyYn = Input;


        if(!CheckTid(mTermID))  //Tid체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
            HideDialog();
            return;
        }

        if(TrdCode.equals("T") && !TradeNo.equals(""))
        {

        } else {
            if (!CashNum.equals("")) {

            } else {
                if(mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getICReaderAddr().equals(""))
                {
                    if (mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getICReaderAddr()))
                    {

                    }
                    else
                    {
                        if (mAutoCount==0)
                        {
                            DeviceReScan(false);
                            return;
                        }
                        else
                        {
                            mAutoCount = 0;
                            SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                            HideDialog();
                            return;
                        }
                    }
                }
                else if(mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getMultiReaderAddr().equals(""))
                {
                    if(mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getMultiReaderAddr()))
                    {

                    }
                    else
                    {
                        if (mAutoCount==0)
                        {
                            DeviceReScan(false);
                            return;
                        }
                        else
                        {
                            mAutoCount = 0;
                            SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                            HideDialog();
                            return;
                        }
                    }
                }
                else
                {
                    SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                    HideDialog();
                }
//                if(!CheckDeviceState(1))    //장치체크
//                {
//                    //SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE + DebugWebComePadTempString());
//                    SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
//                    HideDialog();
//                    return;
//                }
            }

        }


        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (TaxFreeAmt == null || TaxFreeAmt.equals(""))
        {
            TaxFreeAmt = "0";
        }
        if (TrdAmt == null || TrdAmt.equals(""))
        {
            TrdAmt = "0";
        }
        int mTrdAmt = Integer.valueOf(TrdAmt) + Integer.valueOf(TaxFreeAmt);
        TrdAmt = String.valueOf(mTrdAmt);

        /* 현금거래시 타임아웃시간을 셋팅한다 */
//        if(DgTmout.equals("") || DgTmout.equals("0"))
//        {
//            Setting.setTimeOutValue(30);
//            ReadyDialog("현금 영수증 처리중입니다.",30, true);
//        }
//        else
//        {
//            Setting.setCommandTimeOut(Integer.valueOf(DgTmout));
//            Setting.setTimeOutValue(Integer.valueOf(DgTmout));
//            ReadyDialog("현금 영수증 처리중입니다.",Integer.valueOf(DgTmout), true);
//        }
        Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)));
        Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)));
        ReadyDialog("현금 영수증 처리중입니다.",Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)), true);

        /* 로그기록. 현금거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "키인 : " + KeyYn + "," + "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "카드입력대기시간 : " + DgTmout + "," + "개인법인구분 : " + InsYn + "," + "취소사유 : " + CancelReason + "," + "고객번호 : " + CashNum;
        cout("RECV : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);

        //거래 고유키 예외 처리
        if(_Command.equals("B20"))
        {
            if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                HideDialog();
                return;
            }

            if(TrdCode.equals("T") || TrdCode.equals("t"))
            {
                if(TradeNo.equals(""))
                {
                    SendreturnData(RETURN_CANCEL,null,"거래고유번호가 없습니다.");
                    HideDialog();
                    return;
                }
                String CanRea = "";
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3"))
                {
                    CanRea = "a"  + AuDate + AuNo;
                }
                /* 현금거래고유키 취소 */
                mPosSdk.___cashtrade(_Command, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRea, KeyYn, null, null,
                        TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, InsYn, CancelReason, "", "", MchData, "", TradeNo, new TcpInterface.DataListener()
                        {
                            @Override
                            public void onRecviced(byte[] _rev)
                            {

                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                String code = new String(tp.getResData().get(0));
                                String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));
                                String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                HashMap<String, String> sendData = new HashMap<String, String>();
                                sendData.put("TrdType", tp.getResponseCode());
                                sendData.put("TermID", tp.getTerminalID());
                                sendData.put("TrdDate", tp.getDate());
                                sendData.put("AnsCode", code);

                                sendData.put("AuNo", authorizationnumber);
                                sendData.put("TradeNo", kocesTradeUniqueNumber);
                                sendData.put("CardNo", CardNo);
                                sendData.put("Keydate", Encryptionkey_expiration_date);
                                sendData.put("MchData", StoreData);
                                sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                sendData.put("OrdCd", "");   //발급사코드
                                sendData.put("OrdNm", "");   //발급사명
                                sendData.put("InpCd", "");   //매입사코드
                                sendData.put("InpNm", "");   //매입사명
                                sendData.put("DDCYn", "");   //DDC 여부
                                sendData.put("EDCYn", "");   //EDC 여부
                                sendData.put("GiftAmt", ""); //기프트카드 잔액
                                sendData.put("MchNo", "");   //가맹점번호


                                try {
                                    sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                } catch (UnsupportedEncodingException ex) {

                                }
                                /* 로그기록. 현금거래고유키취소 */
                                String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                        "가맹점데이터 : " + StoreData;
                                cout("SEND : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                SendreturnData(RETURN_OK,sendData,null);
//                                if(code.equals("0000")) {
//                                    String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                            "가맹점데이터 : " + StoreData;
//                                    cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                    SendreturnData(RETURN_OK,sendData,null);
//                                }
//                                else
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                }


                            }
                        });
                return;
            }


        }


        if(CashNum.equals("") && KeyYn.equals("S"))  //사용자 정보를 직접 입력 하지 않은 경우 카드 리더기의 현금 영수증 발행
        {
            int DeviceType = 0;
            for(Devices n:mPosSdk.mDevicesList)
            {
                if(n.getmType()==1 || n.getmType()==4)
                {
                    DeviceType = 1;
                }
            }
            if(DeviceType==0)
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOSIGNPAD);
                HideDialog();
//                Toast.makeText(this,ERROR_NOSIGNPAD,Toast.LENGTH_SHORT).show();
                return;
            }
            /* 콜백으로 현금거래결과를 받는다 */
            mPaymentSdk = new PaymentSdk(2,true,((result, Code, resultData) -> {
                if(Code.equals("SHOW"))
                {
                    ReadyDialog(result,0,false);
                }
                else
                {
                    CallBackCashReciptResult(result,Code,resultData);
                }
            }));

            /* 취소구분자체크 */
            String CanRes = "";
            if(_Command.equals("B20")) {
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3")) {
                    CanRes = reSetCancelReason(CancelReason) + AuDate + AuNo;
                } else if (_Command.equals("B20") && CancelReason.equals(""))      //취소 커맨드에 취소 구분자가 없는 경우
                {
                    SendreturnData(RETURN_CANCEL, null, ERROR_EMPTY_CANCEL_REASON);
                    HideDialog();
                    return;
                }
            }
            /* 카드(멀티)리더기로 현금체크카드를 사용한다 */
            mPaymentSdk.CashRecipt(this,mTermID, TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
                    ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),InsYn.equals("")?1:Integer.valueOf(InsYn), "0000", CanRes,
                    CanRes.equals("") ? "":AuDate,KeyYn,CancelReason,
                    "","",MchData,"",TradeNo,
                    Setting.getPreference(this,Constants.SELECTED_DEVICE_CARD_READER).equals("")? Command.TYPEDEFINE_ICMULTIREADER:Command.TYPEDEFINE_ICCARDREADER,
                    "","","","","");

        }
        else if (CashNum.equals("") && KeyYn.equals("K"))   //사용자가 정보를 직접 입력 하지 않은 경우 키인입력
        {
            //만일 터치서명 혹은 사인패드미사용 일 경우
            int DeviceType = 0;
            for(Devices n:mPosSdk.mDevicesList)
            {
                if(n.getmType()==2 || n.getmType()==3|| n.getmType()==4)
                {
                    DeviceType = 1;
                }
            }
            if(DeviceType==0)
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOSIGNPAD);
                HideDialog();
//                Toast.makeText(this,ERROR_NOSIGNPAD,Toast.LENGTH_SHORT).show();
                return;
            }

            mPaymentSdk = new PaymentSdk(2,true,((result, Code, resultData) -> {
                if(Code.equals("SHOW"))
                {
                    ReadyDialog(result,0,false);
                } else
                {
                    CallBackCashReciptResult(result,Code,resultData);
                }
            }));

            /* 취소구분자체크 */
            String CanRes = "";
            if(_Command.equals("B20")) {
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3")) {
                    CanRes = reSetCancelReason(CancelReason) + AuDate + AuNo;
                }
            }
            /* 현금거래 서명패드(멀티패드)에 키인입력거래 */
            mPaymentSdk.CashRecipt(this, mTermID,TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),
                    SvcAmt.equals("")?0:Integer.valueOf( SvcAmt),TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),
                    InsYn.equals("")?1:Integer.valueOf(InsYn), "0000", CanRes,CanRes.equals("") ? "":AuDate,KeyYn,CancelReason,
                    "","",MchData,"",TradeNo,Command.TYPEDEFINE_SIGNPAD,
                    "","","","","");

        }
        else
        {
            byte[] id = new byte[40];
            Arrays.fill(id,(byte)0x20);
            System.arraycopy(CashNum.getBytes(),0,id,0,CashNum.length());
            if(_Command.equals(TCPCommand.CMD_CASH_RECEIPT_REQ))
            {
                //MchData ="";
                //고객번호를 입력하여 현금거래
                String finalTrdAmt = TrdAmt;
                String finalTaxFreeAmt = TaxFreeAmt;
                mPosSdk.___cashtrade(_Command, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", "", KeyYn, id, null,
                        TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, InsYn, "", "", "", MchData, "", TradeNo, new TcpInterface.DataListener()
                        {
                            @Override
                            public void onRecviced(byte[] _rev)
                            {
                                int _tmpEot = 0;
                                while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
                                {
                                    if (Setting.getEOTResult() != 0) {
                                        break;
                                    }
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    _tmpEot++;
                                    if (_tmpEot >= 30) {
                                        break;
                                    }
                                }
                                KByteArray _b = new KByteArray(_rev);
                                _b.CutToSize(10);
                                String _receivecode = new String(_b.CutToSize(3));

                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                String code = new String(tp.getResData().get(0));
                                String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));

                                String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                HashMap<String, String> sendData = new HashMap<String, String>();
                                sendData.put("TrdType", tp.getResponseCode());
                                sendData.put("TermID", tp.getTerminalID());
                                sendData.put("TrdDate", tp.getDate());
                                sendData.put("AnsCode", code);

                                sendData.put("AuNo", authorizationnumber);
                                sendData.put("TradeNo", kocesTradeUniqueNumber);
                                sendData.put("CardNo", CardNo);

                                sendData.put("Keydate", Encryptionkey_expiration_date);
                                sendData.put("MchData", StoreData);
                                sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                sendData.put("OrdCd", "");   //발급사코드
                                sendData.put("OrdNm", "");   //발급사명
                                sendData.put("InpCd", "");   //매입사코드
                                sendData.put("InpNm", "");   //매입사명
                                sendData.put("DDCYn", "");   //DDC 여부
                                sendData.put("EDCYn", "");   //EDC 여부
                                sendData.put("GiftAmt", ""); //기프트카드 잔액
                                sendData.put("MchNo", "");   //가맹점번호

                                if (Setting.getEOTResult() == 1 && _receivecode.equals("B15"))  /* EOT정상으로 현금영수증전문응답정상실행 */
                                {

                                    try {
                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                    } catch (UnsupportedEncodingException ex) {

                                    }

                                    if(mEotCancel==0)
                                    {
                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                "가맹점데이터 : " + StoreData;
                                        cout("SEND : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                        SendreturnData(RETURN_OK,sendData,null);
                                    }
                                    else
                                    {
                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                    }

//                                    if(code.equals("0000")) {
//                                        if(mEotCancel==0)
//                                        {
//                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                    "가맹점데이터 : " + StoreData;
//                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                            SendreturnData(RETURN_OK,sendData,null);
//                                        }
//                                        else
//                                        {
//                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                        }
//                                    }
//                                    else
//                                    {
//                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                    }
                                }
                                else if (Setting.getEOTResult() == -1 && _receivecode.equals("B15"))      /* EOT비정상으로 현금영수증취소실행 */
                                {
                                    String cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //신용승인번호
                                    String mCancelInfo = "1" + tp.getDate().substring(0,6) + cash_appro_num;
                                    byte[] _tmpCancel = mCancelInfo.getBytes();
                                    _tmpCancel[0] = (byte) 0x49;
                                    mCancelInfo = new String(_tmpCancel);
                                    mEotCancel=1;
                                    //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                                    int mMoney = Integer.parseInt(finalTrdAmt) + Integer.parseInt(TaxAmt) + Integer.parseInt(SvcAmt) + Integer.parseInt(finalTaxFreeAmt);

                                    mPosSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", mCancelInfo, KeyYn, id, null,
                                            String.valueOf(mMoney), "0", "0", "0", InsYn, "1", "", "", MchData, "", TradeNo, new TcpInterface.DataListener() {
                                                @Override
                                                public void onRecviced(byte[] _rev) {
                                                    Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                                    String code = new String(tp.getResData().get(0));
                                                    String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                                    String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                                    String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));

                                                    String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                                    String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                                    String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                                    String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                                    String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                                    String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                                    String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                                    String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                                    String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                                    String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                                    HashMap<String, String> sendData = new HashMap<String, String>();
                                                    sendData.put("TrdType", tp.getResponseCode());
                                                    sendData.put("TermID", tp.getTerminalID());
                                                    sendData.put("TrdDate", tp.getDate());
                                                    sendData.put("AnsCode", code);

                                                    sendData.put("AuNo", authorizationnumber);
                                                    sendData.put("TradeNo", kocesTradeUniqueNumber);
                                                    sendData.put("CardNo", CardNo);

                                                    sendData.put("Keydate", Encryptionkey_expiration_date);
                                                    sendData.put("MchData", StoreData);
                                                    sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                                    sendData.put("OrdCd", "");   //발급사코드
                                                    sendData.put("OrdNm", "");   //발급사명
                                                    sendData.put("InpCd", "");   //매입사코드
                                                    sendData.put("InpNm", "");   //매입사명
                                                    sendData.put("DDCYn", "");   //DDC 여부
                                                    sendData.put("EDCYn", "");   //EDC 여부
                                                    sendData.put("GiftAmt", ""); //기프트카드 잔액
                                                    sendData.put("MchNo", "");   //가맹점번호

                                                    try {
                                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                                    } catch (UnsupportedEncodingException ex) {

                                                    }

                                                    if(mEotCancel==0)
                                                    {

                                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                                "가맹점데이터 : " + StoreData;
                                                        cout("SEND : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                                        SendreturnData(RETURN_OK,sendData,null);
                                                    }
                                                    else
                                                    {

                                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                                    }

//                                                    if(code.equals("0000")) {
//                                                        if(mEotCancel==0)
//                                                        {
//
//                                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                                    "가맹점데이터 : " + StoreData;
//                                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                                            SendreturnData(RETURN_OK,sendData,null);
//                                                        }
//                                                        else
//                                                        {
//
//                                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                                        }
//                                                    }
//                                                    else
//                                                    {
//                                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                                    }
                                                }
                                            });
                                    mCancelInfo = "";
                                    Setting.setEOTResult(0);
                                } else if (Setting.getEOTResult() == 0 && _receivecode.equals("B15"))   /* 승인일때 들어와서 시도하며 취소일때는 하지 않는다 EOT비정상으로 현금영수증취소실행 */
                                {
                                    String cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //신용승인번호
                                    String mCancelInfo = "1" + tp.getDate().substring(0,6) + cash_appro_num;
                                    mEotCancel=1;
                                    //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                                    int mMoney = Integer.parseInt(finalTrdAmt) + Integer.parseInt(TaxAmt) + Integer.parseInt(SvcAmt) + Integer.parseInt(finalTaxFreeAmt);
                                    mPosSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", mCancelInfo, KeyYn, id, null,
                                            String.valueOf(mMoney), "0", "0", "0", InsYn, "1", "", "", MchData, "", TradeNo, new TcpInterface.DataListener() {
                                                @Override
                                                public void onRecviced(byte[] _rev) {
                                                    Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                                    String code = new String(tp.getResData().get(0));
                                                    String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                                    String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                                    String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));

                                                    String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                                    String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                                    String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                                    String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                                    String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                                    String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                                    String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                                    String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                                    String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                                    String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                                    HashMap<String, String> sendData = new HashMap<String, String>();
                                                    sendData.put("TrdType", tp.getResponseCode());
                                                    sendData.put("TermID", tp.getTerminalID());
                                                    sendData.put("TrdDate", tp.getDate());
                                                    sendData.put("AnsCode", code);

                                                    sendData.put("AuNo", authorizationnumber);
                                                    sendData.put("TradeNo", kocesTradeUniqueNumber);
                                                    sendData.put("CardNo", CardNo);

                                                    sendData.put("Keydate", Encryptionkey_expiration_date);
                                                    sendData.put("MchData", StoreData);
                                                    sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                                    sendData.put("OrdCd", "");   //발급사코드
                                                    sendData.put("OrdNm", "");   //발급사명
                                                    sendData.put("InpCd", "");   //매입사코드
                                                    sendData.put("InpNm", "");   //매입사명
                                                    sendData.put("DDCYn", "");   //DDC 여부
                                                    sendData.put("EDCYn", "");   //EDC 여부
                                                    sendData.put("GiftAmt", ""); //기프트카드 잔액
                                                    sendData.put("MchNo", "");   //가맹점번호

                                                    try {
                                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                                    } catch (UnsupportedEncodingException ex) {

                                                    }

                                                    if(mEotCancel==0)
                                                    {

                                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                                "가맹점데이터 : " + StoreData;
                                                        cout("SEND : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                                        SendreturnData(RETURN_OK,sendData,null);
                                                    }
                                                    else
                                                    {

                                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                                    }

//                                                    if(code.equals("0000"))
//                                                    {
//                                                        if(mEotCancel==0)
//                                                        {
//
//                                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                                    "가맹점데이터 : " + StoreData;
//                                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                                            SendreturnData(RETURN_OK,sendData,null);
//                                                        }
//                                                        else
//                                                        {
//
//                                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                                        }
//                                                    }
//                                                    else
//                                                    {
//                                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                                    }
                                                }
                                            });
                                } else if (_receivecode.equals("B25")) /* 현금영수증취소 인경우 */
                                {


                                    try {
                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                    } catch (UnsupportedEncodingException ex) {

                                    }

                                    if(mEotCancel==0)
                                    {

                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                "가맹점데이터 : " + StoreData;
                                        cout("SEND : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                        SendreturnData(RETURN_OK,sendData,null);
                                    }
                                    else
                                    {

                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                    }

//                                    if(code.equals("0000")) {
//                                        if(mEotCancel==0)
//                                        {
//
//                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                    "가맹점데이터 : " + StoreData;
//                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                            SendreturnData(RETURN_OK,sendData,null);
//                                        }
//                                        else
//                                        {
//
//                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                        }
//                                    }
//                                    else
//                                    {
//                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                    }
                                }


                            }
                        });
            }
            else if(_Command.equals(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ))    //고객번호 입력하여 현금 영수증 취소의 경우
            {
                String CanRea = "";
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3")) {
                    CanRea = reSetCancelReason(CancelReason) + AuDate + AuNo;
                }

                mPosSdk.___cashtrade(_Command, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRea, KeyYn, id, null,
                        TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, InsYn, CancelReason, "", "", MchData, "", TradeNo, new TcpInterface.DataListener()
                        {
                            @Override
                            public void onRecviced(byte[] _rev)
                            {

                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                String code = new String(tp.getResData().get(0));
                                String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));
                                String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                HashMap<String, String> sendData = new HashMap<String, String>();
                                sendData.put("TrdType", tp.getResponseCode());
                                sendData.put("TermID", tp.getTerminalID());
                                sendData.put("TrdDate", tp.getDate());
                                sendData.put("AnsCode", code);

                                sendData.put("AuNo", authorizationnumber);
                                sendData.put("TradeNo", kocesTradeUniqueNumber);
                                sendData.put("CardNo", CardNo);
                                sendData.put("Keydate", Encryptionkey_expiration_date);
                                sendData.put("MchData", StoreData);
                                sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                sendData.put("OrdCd", "");   //발급사코드
                                sendData.put("OrdNm", "");   //발급사명
                                sendData.put("InpCd", "");   //매입사코드
                                sendData.put("InpNm", "");   //매입사명
                                sendData.put("DDCYn", "");   //DDC 여부
                                sendData.put("EDCYn", "");   //EDC 여부
                                sendData.put("GiftAmt", ""); //기프트카드 잔액
                                sendData.put("MchNo", "");   //가맹점번호
                                try {
                                    sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                } catch (UnsupportedEncodingException ex) {

                                }

                                String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                        "가맹점데이터 : " + StoreData;
                                cout("SEND : CASHRECIPT_SERIAL",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                SendreturnData(RETURN_OK,sendData,null);

//                                if(code.equals("0000")) {
//                                    String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                            "가맹점데이터 : " + StoreData;
//                                    cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                    SendreturnData(RETURN_OK,sendData,null);
//                                }
//                                else
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                }
                            }
                        });
            }
        }
    }

    /**
     *  App to App 현금영수증 취소 시 필수 '1' : 거래취소, '2' : 오류발급, '3' : 기타
     * @param _str
     * @return
     */
    private String reSetCancelReason(String _str)
    {
        if(_str.equals("1"))
        {
            return "0";
        }
        else if(_str.equals("2"))
        {
            return "0";
        }
        else if(_str.equals("3"))
        {
            return "0";
        }
        return "0";
    }

    /**
     * 콜백으로 현금정상거래/신용정상거래/비정상거래 를 받아서 SendreturnData()로 보낸다
     * @param result
     * @param Code
     * @param resultData
     */
    private synchronized void CallBackCashReciptResult(String result,String Code,HashMap<String,String> resultData)
    {
        //간편결제라고 설정을 다시 기본값으로 돌려놓는다
        Setting.setEasyCheck(false);
        if(Code.equals("COMPLETE") )    /* 현금영수증정상콜백 */
        {
            HashMap<String, String> sendData = new HashMap<String, String>();
            sendData.put("TrdType",resultData.get("TrdType"));
            sendData.put("TermID",  resultData.get("TermID"));
            sendData.put("TrdDate", resultData.get("date"));
            if (resultData.get("date") == null) {
                sendData.put("TrdDate", resultData.get("TrdDate"));
            }
            sendData.put("AnsCode", resultData.get("AnsCode"));

            sendData.put("AuNo", resultData.get("AuNo"));
            sendData.put("TradeNo", resultData.get("TradeNo"));
            String OriginalMakingCardNumber = resultData.get("CardNo");
            String DesCardNumber = OriginalMakingCardNumber;
            if(OriginalMakingCardNumber.length()>8)
            {
                DesCardNumber = OriginalMakingCardNumber.substring(0,8);

                int length = OriginalMakingCardNumber.length();
                for(int i =0;i<length-8;i++)
                {
                    DesCardNumber += "*";
                }
                sendData.put("CardNo", DesCardNumber);
            }
            else
            {
                sendData.put("CardNo", DesCardNumber);
            }
            OriginalMakingCardNumber ="";
            resultData.put("CardNo","");
            sendData.put("Keydate", resultData.get("Keydate"));
            sendData.put("MchData", resultData.get("MchData"));
            sendData.put("CardKind", resultData.get("CardKind")); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
            sendData.put("OrdCd", resultData.get("OrdCd"));   //발급사코드
            sendData.put("OrdNm", resultData.get("OrdNm"));   //발급사명
            sendData.put("InpCd", resultData.get("InpCd"));   //매입사코드
            sendData.put("InpNm", resultData.get("InpNm"));   //매입사명
            sendData.put("DDCYn", resultData.get("DDCYn"));   //DDC 여부
            sendData.put("EDCYn", resultData.get("EDCYn"));   //EDC 여부
            sendData.put("GiftAmt",resultData.get( "GiftAmt")); //기프트카드 잔액
            sendData.put("MchNo",resultData.get( "MchNo"));   //가맹점번호
            sendData.put("Message",resultData.get("Message"));
            sendData.put("BillNo",BillNo);

            sendData.put("DisAmt", resultData.get("DisAmt"));   //카카오페이 할인금액 (응답시 전표출력)
            sendData.put("AuthType", resultData.get("AuthType"));   //카카오페이 결제수단 C:Card, M:Money
            sendData.put("AnswerTrdNo", resultData.get("AnswerTrdNo"));   //위챗페이 출력용 거래고유번호
            sendData.put("ChargeAmt", resultData.get("ChargeAmt"));   //제로페이 가맹점 수수료
            sendData.put("RefundAmt", resultData.get("RefundAmt"));   //제로페이 가맹점 환불 금액
            sendData.put("QrKind", resultData.get("QrKind"));   //간편결제 거래 종류

            String mLog = "거래일자 : " + resultData.get("date") + "," + "응답코드 : " + resultData.get("AnsCode") + "," + "응답메세지 : " + resultData.get("Message") + "," +
                    "승인번호 : " + resultData.get("AuNo") + "," + "거래고유번호 : " + resultData.get("TradeNo") + "," + "출력용카드번호 : " + DesCardNumber + "," +
                    "암호키만료잔여일 : " + resultData.get("Keydate") + "," + "가맹점데이터 : " + resultData.get("MchData");
            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
            SendreturnData(RETURN_OK,sendData,null);
            HideDialog();
        }
        else if(Code.equals("COMPLETE_IC"))    /* 신용거래정상콜백 */
        {
            HashMap<String, String> sendData = new HashMap<String, String>();
            sendData.put("TrdType",resultData.get("TrdType"));
            sendData.put("TermID",  resultData.get("TermID"));
            sendData.put("TrdDate",resultData.get("TrdDate"));
            sendData.put("AnsCode",  resultData.get("AnsCode"));
            sendData.put("Message", resultData.get("Message"));
            sendData.put("AuNo",resultData.get("AuNo"));
            sendData.put("TradeNo", resultData.get("TradeNo"));
            String OriginalMakingCardNumber = resultData.get("CardNo");
            String DesCardNumber = OriginalMakingCardNumber;
            if(OriginalMakingCardNumber.length()>8)
            {
                DesCardNumber = OriginalMakingCardNumber.substring(0,8);

                int length = OriginalMakingCardNumber.length();
                for(int i =0;i<length-8;i++)
                {
                    DesCardNumber += "*";
                }
                sendData.put("CardNo", DesCardNumber);
            }
            else
            {
                sendData.put("CardNo", DesCardNumber);
            }

            OriginalMakingCardNumber = "";
            resultData.put("CardNo","");
            sendData.put("Keydate", resultData.get("Keydate"));
            sendData.put("MchData", resultData.get("MchData"));
            sendData.put("CardKind", resultData.get("CardKind")); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
            sendData.put("OrdCd", resultData.get("OrdCd"));   //발급사코드
            String[] issuer_names = resultData.get("OrdNm").split(" ");
            String issuer_name = "";
            for(String n:issuer_names)
            {
                issuer_name += n;
            }
            sendData.put("OrdNm", issuer_name);   //발급사명

            sendData.put("InpCd", resultData.get("InpCd"));   //매입사코드
            String[] purchaser_names = resultData.get("InpNm").split(" ");
            String purchaser_name = "";
            for(String n:purchaser_names)
            {
                purchaser_name += n;
            }
            sendData.put("InpNm",purchaser_name );   //매입사명
            sendData.put("DDCYn", resultData.get("DDCYn"));   //DDC 여부
            sendData.put("EDCYn", resultData.get("EDCYn"));   //EDC 여부
            sendData.put("GiftAmt", resultData.get("GiftAmt")); //기프트카드 잔액
            sendData.put("MchNo", resultData.get("MchNo"));   //가맹점번호
            sendData.put("BillNo",BillNo);

            sendData.put("DisAmt", resultData.get("DisAmt"));   //카카오페이 할인금액 (응답시 전표출력)
            sendData.put("AuthType", resultData.get("AuthType"));   //카카오페이 결제수단 C:Card, M:Money
            sendData.put("AnswerTrdNo", resultData.get("AnswerTrdNo"));   //위챗페이 출력용 거래고유번호
            sendData.put("ChargeAmt", resultData.get("ChargeAmt"));   //제로페이 가맹점 수수료
            sendData.put("RefundAmt", resultData.get("RefundAmt"));   //제로페이 가맹점 환불 금액
            sendData.put("QrKind", resultData.get("QrKind"));   //간편결제 거래 종류

            String mLog =  "거래일자 : " + resultData.get("TrdDate") + "," + "응답코드 : " + resultData.get("AnsCode") + "," + "응답메세지 : " + resultData.get("Message") + "," +
                    "승인번호 : " + resultData.get("AuNo") + "," + "거래고유번호 : " + resultData.get("TradeNo") + "," + "출력용카드번호 : " + DesCardNumber + "," +
                    "암호키만료잔여일 : " + resultData.get("Keydate") + "," + "가맹점데이터 : " + resultData.get("MchData") + "," + "카드종류 : " + resultData.get("CardKind") + "," +
                    "발급사코드 : " + resultData.get("OrdCd") + "," + "발급사명 : " + resultData.get("OrdNm") + "," + "메입사코드 : " + resultData.get("InpCd") + "," +
                    "메입사명 : " + resultData.get("InpNm") + "," + "DDC여부 : " + resultData.get("DDCYn") + "," + "EDC여부 : " + resultData.get("EDCYn") + "," +
                    "기프트잔액 : " + resultData.get("GiftAmt") + "," + "가맹점번호 : " + resultData.get("MchNo");
            cout("SEND : CREDIT",Utils.getDate("yyyyMMddHHmmss"),mLog);
            SendreturnData(RETURN_OK,sendData,null);
            HideDialog();
        } else if (Code.equals("COMPLETE_PRINT"))
        {
            HashMap<String, String> sendData = new HashMap<String, String>();
            sendData.put("Message", "프린트를 완료하였습니다.");
            String mLog = "메세지 : " + sendData.get("Message");
            cout("SEND : PRINT",Utils.getDate("yyyyMMddHHmmss"),mLog);
            SendreturnData(RETURN_OK,sendData,"");
            HideDialog();
        } else if(Code.equals("COMPLETE_EASY"))
        {
            //USB 바코드리딩 처리. 캣은 바코드 리딩이 여기로 오지 않는다
            if(mhashMap.get("TrdType").equals("Q10") || mhashMap.get("TrdType").equals("Q20"))
            {
                HashMap<String, String> sendData = new HashMap<String, String>();
                sendData.put("TrdType", mhashMap.get("TrdType").equals("Q10") ? "Q15":"Q25");
                sendData.put("AnsCode", "0000");
                if(mhashMap.get("QrNo") != null && !mhashMap.get("QrNo").equals(""))
                {
                    sendData.put("PadData", mhashMap.get("QrNo"));
                }
                else
                {
                    sendData.put("PadData", resultData.get("Qr"));
                }

                sendData.put("Message", "정상출력");
                String mLog = "TrdType : " + sendData.get("TrdType") + ", " + "AnsCode : " + sendData.get("AnsCode") +
                        ", " + "PadData : " + sendData.get("PadData") + ", " + "Message : " + sendData.get("Message");
                cout("SEND : QRMULTIPAD",Utils.getDate("yyyyMMddHHmmss"),mLog);
                SendreturnData(RETURN_OK,sendData,"");
                HideDialog();
            }
            else
            {
                HideDialog();
                mhashMap.put("QrNo", resultData.get("Qr"));
                Easy(mhashMap);
            }

        }
        else
        {
            SendreturnData(RETURN_CANCEL,null,result);
            HideDialog();
        }

    }

    /**
     * 앱투앱_가맹점다운로드
     * @param _hashMap
     */
    private synchronized void MchDownload(HashMap<String,String> _hashMap)
    {
        String termID = _hashMap.get("TermID") == null ? "":_hashMap.get("TermID");
        String BsnNo = _hashMap.get("BsnNo") == null ? "":_hashMap.get("BsnNo");
        String serial = _hashMap.get("Serial") == null ? "":_hashMap.get("Serial");
        String TrdType = _hashMap.get("TrdType") == null ? "":_hashMap.get("TrdType");
        String MchData = _hashMap.get("MchData") == null ? "":_hashMap.get("MchData");

        if(termID==null || BsnNo==null || serial == null)   //tid 사업자번호 시리얼번호 체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_NODATA);
            return;
        }

        //tid,사업자번호,시리얼번호가 10자가 넘는 경우 에러를 발생 시킨다F
        if(termID.length()!=10 || BsnNo.length()!=10 || serial.length()!=10)
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHSTRING_COUNT);
            return;
        }

        /* 로그기록. 가맹점다운로드 */
        String mLog = "TrdType : " + TrdType + ", " + "TermID : " + termID +
                ", " + "사업자번호 : " + BsnNo + ", " + "SN : " + serial  +
                ", " + "가맹점데이터 : " + MchData;
        if(TrdType.equals("D10") || TrdType.equals("D20"))
        {
            cout("RECV : STORE_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),mLog);
        }
        else
        {
            cout("RECV : MULTI_STORE_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),mLog);
        }


        Toast.makeText(instance, "가맹점 다운로드 중입니다", Toast.LENGTH_SHORT).show();
//        ReadyDialogShow(instance, "가맹점 다운로드 중입니다", 0);
//        ReadyDialog("가맹점 다운로드 중입니다",0, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                /* 가맹점키다운로드 */
                mPosSdk.___registedShopInfo_KeyDownload(TCPCommand.CMD_REGISTERED_SHOP_DOWNLOAD_REQ, termID, Utils.getDate("yyMMddHHmmss"),
                        Constants.TEST_SOREWAREVERSION, "", "0003", "MDO", BsnNo, serial, MchData,
                        Utils.getMacAddress(mPosSdk.getActivity()), new TcpInterface.DataListener() {
                            @Override
                            public void onRecviced(byte[] _rev) {
                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
//                                ReadyDialogHide();
                                if(tp.getResponseCode().equals(TCPCommand.CMD_SHOP_DOWNLOAD_RES))
                                {
                                    HashMap<String,String> hashMap = new HashMap<String, String>();
                                    final List<byte[]> data = tp.getResData();
                                    String code = new String(data.get(0));


                                    String codeMessage = "";
                                    String creditConnNumberA1200 = "";
                                    String creditConnNumberB1200 = "";
                                    String etcConnNumberA1200 = "";
                                    String etcConnNumberB1200 = "";
                                    String creditConnNumberA2400= "";
                                    String creditConnNumberB2400= "";
                                    String etcConnNumberA2400 = "";
                                    String etcConnNumberB2400 = "";
                                    String ASPhoneNumber = "";
                                    String StoreName = "";
                                    String StoreBusinessNumber = "";
                                    String StoreCEOName = "";
                                    String StoreAddr = "";
                                    String StorePhoneNumber = "";
                                    String Working_Key_Index = "";
                                    String Working_Key= "";
                                    String TMK = "";
                                    String PointCardCount = "";
                                    String PointCardInfo= "";
                                    String Etc= "";
                                    String HardwareKey = "";
                                    try {
                                        codeMessage = Utils.getByteToString_euc_kr(data.get(1));
                                        creditConnNumberA1200 = Utils.ByteArrayToString(data.get(3));
                                        creditConnNumberB1200 = Utils.ByteArrayToString(data.get(4));
                                        etcConnNumberA1200 = Utils.ByteArrayToString(data.get(5));
                                        etcConnNumberB1200 = Utils.ByteArrayToString(data.get(6));
                                        creditConnNumberA2400 = Utils.ByteArrayToString(data.get(7));
                                        creditConnNumberB2400 = Utils.ByteArrayToString(data.get(8));
                                        etcConnNumberA2400 = Utils.ByteArrayToString(data.get(9));
                                        etcConnNumberB2400 = Utils.ByteArrayToString(data.get(10));
                                        ASPhoneNumber = Utils.ByteArrayToString(data.get(11));
                                        StoreName = Utils.getByteToString_euc_kr(data.get(12));
                                        StoreBusinessNumber = Utils.ByteArrayToString(data.get(13));
                                        StoreCEOName = Utils.getByteToString_euc_kr(data.get(14));
                                        StoreAddr = Utils.getByteToString_euc_kr(data.get(15));
                                        StorePhoneNumber = Utils.ByteArrayToString(data.get(16));
                                        Working_Key_Index = Utils.ByteArrayToString(data.get(17));
                                        Working_Key = Utils.ByteArrayToString(data.get(18));
                                        TMK = Utils.ByteArrayToString(data.get(19));
                                        PointCardCount = Utils.ByteArrayToString(data.get(20));
                                        PointCardInfo = Utils.getByteToString_euc_kr(data.get(21));
                                        Etc = Utils.ByteArrayToString(data.get(22));
                                        HardwareKey = Utils.ByteArrayToString(data.get(23));
                                    }catch (UnsupportedEncodingException ex)
                                    {

                                    }
                                    hashMap.put("TrdType",TCPCommand.CMD_SHOP_DOWNLOAD_RES);
                                    hashMap.put("TrdDate",Utils.getDate("yyMMddHHmmss"));
                                    hashMap.put("AnsCode",code);
                                    hashMap.put("Message",codeMessage);
                                    hashMap.put("AsNum",ASPhoneNumber);
                                    hashMap.put("ShpNm",StoreName);
                                    hashMap.put("BsnNo",StoreBusinessNumber);
                                    hashMap.put("PreNm",StoreCEOName);
                                    hashMap.put("ShpAdr",StoreAddr);
                                    hashMap.put("ShpTel",StorePhoneNumber);
                                    hashMap.put("MchData",Etc);
                                    hashMap.put("HardwareKey",HardwareKey);
                                    hashMap.put("TermID",termID);

                                    /* 로그기록. 가맹점다운로드 */
                                    String mLog = "TrdType : " + TrdType + ", " + "TermID : " + termID +
                                            ", " + "AnsCode : " + code + ", " + "Message : " + codeMessage  +
                                            ", " + "AsNum : " + ASPhoneNumber + ", " + "ShpNm : " + StoreName +
                                            ", " + "BsnNo : " + StoreBusinessNumber + ", " + "PreNm : " + StoreCEOName +
                                            ", " + "ShpAdr : " + StoreAddr + ", " + "ShpTel : " + StorePhoneNumber +
                                            ", " + "MchData : " + Etc + ", " + "HardwareKey : " + HardwareKey;
                                    if(mhashMap != null) {
                                        String recv_MtidYn = mhashMap.get("MtidYn") == null ? "" : mhashMap.get("MtidYn");

                                        if (recv_MtidYn != null && recv_MtidYn.equals("1")) {
                                            cout("SEND : MULTI_STORE_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                        }
                                        else
                                        {
                                            cout("SEND : STORE_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                        }
                                    }
                                    else
                                    {
                                        cout("SEND : STORE_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                    }


                                    if(code.equals("0000")) //대리점 다운로드가 성공적으로 수행 되면 TID를 저장한다.
                                    {
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_APPTOAPP_NO,BsnNo);
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.APPTOAPP_TID,termID);
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.REGIST_DEVICE_APPTOAPP_SN,serial);
                                        Utils.setHardwareKey(mPosSdk.getActivity(),HardwareKey,true,termID);

                                        if(mhashMap != null) {
                                            String recv_MtidYn = mhashMap.get("MtidYn") == null ? "" : mhashMap.get("MtidYn");

                                            if (recv_MtidYn != null && recv_MtidYn.equals("1")) {
                                                String recv_Command = mhashMap.get("TrdType");
                                                //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
                                                // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면 해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다
                                                switch (recv_Command) {

                                                    case "K21":
                                                    case "K22":
                                                    case "E10":
                                                    case "E20":
                                                    case "E30":
                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Easy(mhashMap);
                                                            }
                                                        }, 200);
                                                        break;
                                                    case "Q10":
                                                    case "Q20":
                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                QR_Reader(mhashMap);
                                                            }
                                                        },200);
                                                        break;
                                                    case "A10":
                                                    case "A20":

                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Credit(mhashMap);
                                                            }
                                                        }, 200);
                                                        break;
                                                    case "B10":
                                                    case "B20":

                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Cash(mhashMap);
                                                            }
                                                        }, 200);
                                                        break;

                                                    default:
                                                        break;
                                                }
                                                return;
                                            }
                                        }
                                        //todo: db 에 저장할 이유가 없다.22-05-16.jiw
//                                        mPosSdk.setSqliteDB_StoreTable(creditConnNumberA1200,creditConnNumberB1200,etcConnNumberA1200,etcConnNumberB1200,creditConnNumberA2400,creditConnNumberB2400,
//                                                etcConnNumberA2400,etcConnNumberB2400,ASPhoneNumber,StoreName,termID,StoreBusinessNumber,StoreCEOName,StoreAddr,StorePhoneNumber,Working_Key_Index,Working_Key,
//                                                TMK,PointCardCount,PointCardInfo,Etc);
                                        SendreturnStoreData(RETURN_OK,hashMap,null);
                                    }
                                    else
                                    {
                                        SendreturnStoreData(RETURN_CANCEL,null,"가맹점 정보 확인" + code);
                                    }

                                }
                            }
                        });
            }
        }).start();
    }

    /**
     * 신용/현금 을 정상/실패 를 완료 후에 해당 메시지및데이터를 앱투앱으로 보내고 종료한다
     * @param _state
     * @param _hashMap
     * @param _Message
     */
    public void SendreturnData(int _state,HashMap _hashMap,String _Message)
    {
        HideDialog();
        mEotCancel=0;
        mAutoCount = 0;
        bleCount = 0;
        bleFirstCheck = false;
        _bleDeviceCheck = 0;
        AppToApp_VersionBleCount = 0;
        Setting.setDscyn("1");
        Setting.setDscData("");
        Setting.setEasyCheck(false);
        Setting.setIsAppToApp(false);
        Setting.setIsAppForeGround(2);
//        mPosSdk.setFocusActivity(instance,appToAppUsbSerialStateListener);
        Setting.setTopContext(instance);
        //TODO: 일단 주석처리 대기한다. 20-03-03. by.jiw
//        UsageStatsManager usm = (UsageStatsManager)this.getSystemService(Context.USAGE_STATS_SERVICE);
//        long time = System.currentTimeMillis();
//        UsageEvents appEvent = usm.queryEvents(time - 10000, time);
//
//
//        while(appEvent.hasNextEvent())
//        {
//            UsageEvents.Event ev = new UsageEvents.Event();
//            appEvent.getNextEvent(ev);
//            if (ev.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND)
//            {
//                String name = ev.getPackageName();
//                Log.d("USAGE_STAT_EVENT", name);
//                if (mIntent.getPackage().equals(name))
//                {
//                    Log.d("USAGE_STAT_EVENT_APPTOAPP", name);
//                    if(_state==0) {
//                        mIntent = new Intent();
//                        mIntent.putExtra("hashMap", _hashMap);
//                        this.setResult(RESULT_OK, mIntent);
//                    }
//                    else if(_state==1)
//                    {
//                        mIntent = new Intent();
//                        HashMap<String,String> hashMap = new HashMap<String, String>();
//                        hashMap.put("AnsCode","9999");
//                        hashMap.put("Message",_Message);
////            mIntent.putExtra("AnsCode", "9999");
////            mIntent.putExtra("Message", _Message);
//                        mIntent.putExtra("hashMap", hashMap);
//                        this.setResult(RESULT_CANCELED, mIntent);
//                    }
//                    else
//                    {
//                        mIntent = new Intent();
//                        HashMap<String,String> hashMap = new HashMap<String, String>();
//                        hashMap.put("AnsCode","9999");
//                        hashMap.put("Message",_Message);
//                        mIntent.putExtra("hashMap", hashMap);
////            mIntent.putExtra("Msg", _Message);
//                        this.setResult(-100, mIntent);
//                    }
//                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            mPosSdk.DeviceReset();  /* 장치전체초기화 */
//                        }
//                    },500);
//                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            Runtime.getRuntime().gc();
//                            finishAndRemoveTask();
//                            /* finish() -> finishAndRemoveTask() 안드로이드 정책변경으로 안드로이드버전이 9이상이라면 해당함수로 피니쉬를 선언 intent를 통해 저장된결과(setResult()) 가 정상적으로 앱투앱으로 보내진다 */
//                        }
//                    },1000);
//                    return;
//                }
//            }
//        }



        if(_state==0) {
            if (!(mhashMap.get("TrdType")).equals("F10") && !(mhashMap.get("TrdType")).equals("R10") && !(mhashMap.get("TrdType")).equals("R20") && !(mhashMap.get("TrdType")).equals("P10") && !(mhashMap.get("TrdType")).equals("D20"))
            {
                HashMap<String,String> hashMap = new HashMap<String, String>();
                hashMap = _hashMap;
                if((mhashMap.get("TrdType")).contains("20"))
                {
                    hashMap.put("OriAuDate", mhashMap.get("AuDate"));
                    hashMap.put("OriAuNo", mhashMap.get("AuNo"));
                }

                hashMap.put("BillNo", BillNo);
                hashMap.put("TrdAmt", mhashMap.get("TrdAmt"));
                hashMap.put("TaxAmt", mhashMap.get("TaxAmt"));
                hashMap.put("SvcAmt", mhashMap.get("SvcAmt"));
                hashMap.put("TaxFreeAmt", mhashMap.get("TaxFreeAmt"));
                hashMap.put("Month", mhashMap.get("Month"));
                if(BillNo.length()>0 && BillNo.length() == 12) {
                    String _tid = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                    String _date = Utils.getDate("yyMMdd");
//                    if(!mPosSdk.checkAppToAppTradeList(_tid,_date,BillNo))
//                    {
                        mPosSdk.setAppToAppTradeResult(hashMap);
//                    }

                }
            }

            mIntent = new Intent();
            mIntent.putExtra("hashMap", _hashMap);
            this.setResult(RESULT_OK, mIntent);
        }
        else if(_state==1)
        {
            mIntent = new Intent();
            HashMap<String,String> hashMap = new HashMap<String, String>();
            hashMap.put("AnsCode","9999");
            hashMap.put("Message",_Message);
            hashMap.put("TermID",mhashMap == null ? "":mhashMap.get("TermID"));
            hashMap.put("BillNo", BillNo);
//            mIntent.putExtra("AnsCode", "9999");
//            mIntent.putExtra("Message", _Message);
            mIntent.putExtra("hashMap", hashMap);
            if(mhashMap != null) {
                if (!(mhashMap.get("TrdType")).equals("F10") && !(mhashMap.get("TrdType")).equals("R10") && !(mhashMap.get("TrdType")).equals("R20") && !(mhashMap.get("TrdType")).equals("P10") && !(mhashMap.get("TrdType")).equals("D20")) {
                    if (BillNo.length() > 0 && BillNo.length() == 12) {
                        String _tid = hashMap.get("TermID") == null ? "" : hashMap.get("TermID");
                        String _date = Utils.getDate("yyMMdd");
//                        if (!mPosSdk.checkAppToAppTradeList(_tid, _date, BillNo)) {
                            mPosSdk.setAppToAppTradeResult(hashMap);
//                        }
                    }
                }
            }

            String mLog = "응답코드 : " + hashMap.get("AnsCode") + "," + "응답메세지 : " + hashMap.get("Message") + "," +
                    "TermID : " + hashMap.get("TermID") + "," + "BillNo : " + hashMap.get("BillNo") + "hashMap : " + hashMap.get("hashMap");
            cout("SEND : CANCEL",Utils.getDate("yyyyMMddHHmmss"),mLog);

            this.setResult(RESULT_CANCELED, mIntent);
        }
        else
        {
            mIntent = new Intent();
            HashMap<String,String> hashMap = new HashMap<String, String>();
            hashMap.put("AnsCode","9999");
            hashMap.put("TermID",mhashMap == null ? "":mhashMap.get("TermID"));
            hashMap.put("Message",_Message);
            hashMap.put("BillNo", BillNo);
            mIntent.putExtra("hashMap", hashMap);
            if(mhashMap != null) {
                if (!(mhashMap.get("TrdType")).equals("F10") && !(mhashMap.get("TrdType")).equals("R10") && !(mhashMap.get("TrdType")).equals("R20") && !(mhashMap.get("TrdType")).equals("P10")  && !(mhashMap.get("TrdType")).equals("R20")) {
                    if (BillNo.length() > 0 && BillNo.length() == 12) {
                        String _tid = hashMap.get("TermID") == null ? "" : hashMap.get("TermID");
                        String _date = Utils.getDate("yyMMdd");
//                        if (!mPosSdk.checkAppToAppTradeList(_tid, _date, BillNo)) {
                            mPosSdk.setAppToAppTradeResult(hashMap);
//                        }
                    }
                }
            }
//            mIntent.putExtra("Msg", _Message);
            String mLog = "응답코드 : " + hashMap.get("AnsCode") + "," + "응답메세지 : " + hashMap.get("Message") + "," +
                    "TermID : " + hashMap.get("TermID") + "," + "BillNo : " + hashMap.get("BillNo") + "hashMap : " + hashMap.get("hashMap");
            cout("SEND : ERROR",Utils.getDate("yyyyMMddHHmmss"),mLog);

            this.setResult(-100, mIntent);
        }

        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                //이곳으로 들어올 일이 없다
                break;
            case BLE:       //BLE의 경우
                if(mPosSdk.BleIsConnected())
                {
                    //P10 = 거래내역전표출력, R20 = 버전정보요청(bLE연결), A10,A20 = 신용결제, B10,B20 = 현금결제
                    if((mhashMap.get("TrdType")).equals("P10") ||
                            (mhashMap.get("TrdType")).equals("R20") ||
                            (mhashMap.get("TrdType")).equals("A10") ||
                            (mhashMap.get("TrdType")).equals("A20") ||
                            (mhashMap.get("TrdType")).equals("B10") ||
                            (mhashMap.get("TrdType")).equals("B20") ||
                            (mhashMap.get("TrdType")).equals("D20")) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPosSdk.__BLEDeviceInit(null,"99");  /* 장치전체초기화 */
                            }
                        },500);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPosSdk.__BLEDeviceInit(null,"99");  /* 장치전체초기화 */
                            }
                        },700);
                    }

                }

                break;
            case CAT:       //WIFI CAT의 경우
                //이곳으로 들어올 일이 없다
                break;
            case LINES:     //유선장치의 경우
                //바로 아래쪽으로 들어간다
                //P10 = 거래내역전표출력, R20 = 버전정보요청(bLE연결), A10,A20 = 신용결제, B10,B20 = 현금결제
                if((mhashMap.get("TrdType")).equals("P10") ||
                        (mhashMap.get("TrdType")).equals("R20") ||
                        (mhashMap.get("TrdType")).equals("A10") ||
                        (mhashMap.get("TrdType")).equals("A20") ||
                        (mhashMap.get("TrdType")).equals("B10") ||
                        (mhashMap.get("TrdType")).equals("B20") ||
                        (mhashMap.get("TrdType")).equals("Q10") ||
                        (mhashMap.get("TrdType")).equals("Q20") ||
                        (mhashMap.get("TrdType")).equals("QR") ||
                        (mhashMap.get("TrdType")).equals("D20")) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPosSdk.DeviceReset();  /* 장치전체초기화 */
                        }
                    },500);
                }

                break;
            default:
                break;
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Setting.setTopContext(instance);
                Runtime.getRuntime().gc();
                finishAndRemoveTask();
                /* finish() -> finishAndRemoveTask() 안드로이드 정책변경으로 안드로이드버전이 9이상이라면 해당함수로 피니쉬를 선언 intent를 통해 저장된결과(setResult()) 가 정상적으로 앱투앱으로 보내진다 */
            }
        },1000);
        return;

    }

    /**
     * 가맹점다운로드를 정상/실패 를 완료 후에 해당 메시지및데이터를 앱투앱으로 보내고 종료한다
     * @param _state
     * @param _hashMap
     * @param _Message
     */
    public void SendreturnStoreData(int _state,HashMap _hashMap,String _Message)
    {
        Setting.setIsAppToApp(false);
        HideDialog();
        mEotCancel=0;
        if(_state==0) {
            mIntent = new Intent();
            mIntent.putExtra("hashMap", _hashMap);
            this.setResult(RESULT_OK, mIntent);
        }
        else if(_state==1)
        {
            mIntent = new Intent();
            HashMap<String,String> hashMap = new HashMap<String, String>();
            hashMap.put("AnsCode","9999");
            hashMap.put("Message",_Message);
//            mIntent.putExtra("AnsCode", "9999");
//            mIntent.putExtra("Message", _Message);
            mIntent.putExtra("hashMap", hashMap);
            this.setResult(RESULT_CANCELED, mIntent);
        }
        else
        {
            mIntent = new Intent();
            HashMap<String,String> hashMap = new HashMap<String, String>();
            hashMap.put("AnsCode","9999");
            hashMap.put("Message",_Message);
            mIntent.putExtra("hashMap", hashMap);
//            mIntent.putExtra("Msg", _Message);
            this.setResult(-100, mIntent);
        }
        Runtime.getRuntime().gc();
        finishAndRemoveTask();
    }




    /**
     * 사인데이터의 결과를 받아서 처리한다
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(requestCode!=REQUEST_SIGNPAD)
        {
            if (result != null) {
                if (result.getContents() == null || result.getContents().equals("")) {
                    SendreturnData(RETURN_CANCEL,null,"바코드 스캔이 처리되지 않았습니다");
                    HideDialog();
                    return;
                }

                mhashMap.put("QrNo",result.getContents());
                /** 만일 현재 CAT 연동중이라면 여기서 CAT_APP으로 보낸다. */
                /** 장치 정보를 읽어서 설정 하는 함수         */
                String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
                if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
                    Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
                }else
                {
                    Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
                    Setting.g_PayDeviceType = _type;
                }
                switch (Setting.g_PayDeviceType) {
                    case CAT:       //WIFI CAT의 경우
                        if(mhashMap.get("TrdType").equals("Q10") || mhashMap.get("TrdType").equals("Q20") )
                        {
                            CallBackCashReciptResult("정상출력", "COMPLETE_EASY", mhashMap);
//                            QR_Reader(mhashMap);
                        }
                        else
                        {
                            CatAppCard(mhashMap);
                        }

                        return;
                }

                if(mhashMap.get("TrdType").equals("Q10") || mhashMap.get("TrdType").equals("Q20") )
                {
                    CallBackCashReciptResult("정상출력", "COMPLETE_EASY", mhashMap);
//                    QR_Reader(mhashMap);
                }
                else
                {
                    Easy(mhashMap);
                }


            }
            else
            {
                SendreturnData(RETURN_CANCEL,null,"바코드 스캔이 처리되지 않았습니다");
                HideDialog();
                return;
            }
            return;
        }

        //만일간편결제라면 아래분기를 타지않고 처리한다
        if (Setting.getEashCheck())
        {
            if(requestCode==REQUEST_SIGNPAD)
            {
                if(resultCode==RESULT_OK && data.getStringExtra("signresult").equals("OK"))
                {

                    byte[] bytes = data.getByteArrayExtra("signdata");;
                    mEasyPaymentSdk.Result_SignPad(true,bytes);

                }
                else if(resultCode==RESULT_CANCELED && data.getStringExtra("signresult").equals("CANCEL"))
                {
                    //사인처리가 취소됨. 모든 정보 클리어 처리
                    HideDialog();
                    mPaymentSdk.DeviceReset();
                    SendreturnData(RETURN_CANCEL,null,"서명 입력이 취소되었습니다");
                }
            }
            return;
        }

        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                //이곳으로 들어올 일이 없다
                break;
            case BLE:       //BLE의 경우

                if(requestCode==REQUEST_SIGNPAD)
                {
                    if(resultCode==RESULT_OK && data.getStringExtra("signresult").equals("OK"))
                    {
                        //만일 서명패드가 없이 터치서명 또는 서명없음일 경우 _img 파일을 저장된 파일로 대체 한다. 저장된 파일이 없다면 이미지없음으로 처리한다.
                        if(Setting.getDscyn().equals("0")){
                            byte[] bytes = data.getByteArrayExtra("signdata");;

//                    mPaymentSdk.Req_tcp_Credit(mTermID,bytes,data.getStringExtra("codeNver"),"","");
                            mBlePaymentSdk.Req_tcp_Credit(mTermID,bytes,"","",""); /* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                        }
                        else{
                            byte[] tmpSign = data.getByteArrayExtra("signdata");
                            String CodeNversion = data.getStringExtra("codeNver");
                            mBlePaymentSdk.Req_tcp_Credit(mTermID,tmpSign,"","","");/* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                        }
                    }
                    else if(resultCode==RESULT_CANCELED && data.getStringExtra("signresult").equals("CANCEL"))
                    {
                        //사인처리가 취소됨. 모든 정보 클리어 처리
                        HideDialog();
                        mBlePaymentSdk.DeviceReset();
                        SendreturnData(RETURN_CANCEL,null,"서명 입력이 취소되었습니다");
                    }
                }
                return;
            case CAT:       //WIFI CAT의 경우
                //이곳으로 들어올 일이 없다
                break;
            case LINES:     //유선장치의 경우
                //바로 아래쪽으로 들어간다
                break;
            default:
                break;
        }

        if(requestCode==REQUEST_SIGNPAD)
        {
            if(resultCode==RESULT_OK && data.getStringExtra("signresult").equals("OK"))
            {
                //만일 서명패드가 없이 터치서명 또는 서명없음일 경우 _img 파일을 저장된 파일로 대체 한다. 저장된 파일이 없다면 이미지없음으로 처리한다.
                if(Setting.getSignPadType(mPosSdk.getActivity()) == 0  || Setting.getSignPadType(mPosSdk.getActivity()) == 3){
                    byte[] bytes = data.getByteArrayExtra("signdata");;

//                    mPaymentSdk.Req_tcp_Credit(mTermID,bytes,data.getStringExtra("codeNver"),"","");
                    mPaymentSdk.Req_tcp_Credit(mTermID,bytes,"","",""); /* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                }
                else{
                    byte[] tmpSign = data.getByteArrayExtra("signdata");
                    String CodeNversion = data.getStringExtra("codeNver");
                    mPaymentSdk.Req_tcp_Credit(mTermID,tmpSign,"","","");/* 단말기로 부터 받은 신용거래정보를 서버로 보낸다 */
                }
            }
            else if(resultCode==RESULT_CANCELED && data.getStringExtra("signresult").equals("CANCEL"))
            {
                //사인처리가 취소됨. 모든 정보 클리어 처리
                HideDialog();
                mPaymentSdk.DeviceReset();
                SendreturnData(RETURN_CANCEL,null,"서명 입력이 취소되었습니다");
            }
        }
    }

    /** 사용하지않음 */
    ServiceConnection mSrvConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

//    /**
//     * 카드리더기or멀티리더기가 정상적으로 앱에 셋팅되어 있는지 체크한다
//     * @param _DeviceType
//     * @return
//     */
//    private boolean CheckDeviceState(int _DeviceType)
//    {
////        boolean tmp = false;
////        if(mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getICReaderAddr().equals(""))
////        {
////            if (mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getICReaderAddr()))
////            {
////                return true;
////            }
////            else
////            {
////                DeviceReScan();
////            }
////
////        }
////        else if(mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getMultiReaderAddr().equals(""))
////        {
////            if(mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getMultiReaderAddr()))
////            {
////                return true;
////            }
////            else
////            {
////                DeviceReScan();
////            }
////
////        }
////        else
////        {
////            return false;
////        }
//
//    }

    String mAddr = "";
    int mType = 0; //1=카드리더기, 4=멀티리더기
    boolean mCreditOrCash = false; //false = cash, true = credit
    int mAutoCount = 0;
    private void DeviceReScan(boolean _credit)
    {
        mAutoCount++;
        mCreditOrCash = _credit;
        /* 장치를 검색하여 현재 정상적으로 usb가 붙어있는 사용가능장비들을 체크한다 */
        final AutoDetectDevices autoDetectDevices = new AutoDetectDevices(mPosSdk.getActivity(), new AutoDetectDeviceInterface.DataListener() {
            @Override
            public void onReceived(boolean State, int _DeviceCount, String _Evt) {
                if(_DeviceCount>0)
                {
                    String[] deviceName = new String[mPosSdk.mDevicesList.size()+1];
                    int mCount = 0;
                    deviceName[0] = "";
                    mCount++;
                    for (Devices n: mPosSdk.mDevicesList) {
                        deviceName[mCount] = n.getDeviceSerial();
                        mCount++;
                    }

                    if(mPosSdk.mDevicesList != null)
                    {
                        String[] _deviceName = new String[mPosSdk.mDevicesList.size()+1];
                        int _mCount = 0;
                        _deviceName[0] = "";
                        _mCount++;
                        for (Devices n: mPosSdk.mDevicesList) {
                            _deviceName[_mCount] = n.getDeviceSerial();
                            if (n.getmType() == 1) {
                                mAddr  = n.getDeviceSerial();
                                mType = 1;
//                                m_txt_line_tvw_product.setText(n.getName());
//                                m_txt_line_tvw_serial.setText(n.getDeviceSerial());
//                                m_txt_line_tvw_version.setText(n.getVersion());
                            } else if(n.getmType() == 4) {
                                mAddr  = n.getDeviceSerial();
                                mType = 4;
//                                m_txt_line_tvw_product.setText(n.getName());
//                                m_txt_line_tvw_serial.setText(n.getDeviceSerial());
//                                m_txt_line_tvw_version.setText(n.getVersion());
                            } else if (n.getmType() == 0) {
//                                    m_txt_reader.setText(n.getName());
                            }

                            if (Setting.getPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN).equals(n.getDeviceSerial()))
                            {
                                mAddr  = n.getDeviceSerial();
                            }
                            _mCount++;
                        }
                        if (!mAddr.equals(""))
                        {
                            CardConnectCheck();
                        } else
                        {
                            SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE + " 장치주소이상");
                            HideDialog();
                            return;
                        }

                    }
                    else
                    {
                        SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE + " 디바이스정보이상");
                        HideDialog();
                        return;
                    }

                }
                else
                {
                    SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE + "디바이스갯수이상");
                    HideDialog();
                    return;
                }
            }
        });
    }

    private void CardConnectCheck()
    {
        if(mPosSdk.getUsbDevice().size()!=0 && mPosSdk.mDevicesList!=null )
        {

            for (Devices n: mPosSdk.mDevicesList)
            {
                if(n.getDeviceSerial().equals(mAddr))
                {

                    if(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_CARD_READER_OPTION).equals("카드리더기")) {
                        mPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                            @Override
                            public void onReceived(byte[] _rev, int _type) {
                                if(_type==Command.PROTOCOL_TIMEOUT)
                                {
                                    Toast.makeText(instance, "장치 정보를 읽어오지 못했습니다.", Toast.LENGTH_SHORT).show();
//                                    ShowDialog("장치 정보를 읽어오지 못했습니다.");
                                    SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                                    HideDialog();
                                    return;
                                }
                                if(_rev[3]==(byte)0x15){
                                    //장비에서 NAK 올라 옮
                                    Toast.makeText(instance, "장치 NAK", Toast.LENGTH_SHORT).show();
//                                    ShowDialog("장치 NAK");
                                    SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                                    HideDialog();
                                    return;
                                }
                                if(Setting.ICResponseDeviceType == 3)
                                {
                                    Toast.makeText(instance, "장치 오류", Toast.LENGTH_SHORT).show();
//                                    ShowDialog("장치 오류");
                                    SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
                                    HideDialog();
                                    return;
                                }
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                Setting.mAuthNum = authNum.trim();
                                n.setmType(1);
                                n.setConnected(true);
                                n.setName(authNum);
                                n.setVersion(version);
                                n.setDeviceName(serialNum);
                                if (n.getmType() == 1) {
//                                    m_txt_line_tvw_product.setText(n.getName());
//                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
//                                    m_txt_line_tvw_version.setText(n.getVersion());
                                } else if(n.getmType() == 4) {
//                                    m_txt_line_tvw_product.setText(n.getName());
//                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
//                                    m_txt_line_tvw_version.setText(n.getVersion());
                                } else if (n.getmType() == 0) {

                                }
                                mPosSdk.setICReaderAddr(n.getmAddr());
                                Toast.makeText(instance, "연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                                if (mCreditOrCash)
                                {
                                    Credit(mhashMap);
                                    return;
                                }
                                else
                                {
                                    Cash(mhashMap);
                                    return;
                                }
//                                ShowDialog("장치연결확인");
                            }
                        },new String[]{n.getmAddr()});
//                        mPosSdk.__CardStatusCheck( mDataListener,mPosSdk.getICReaderAddr2());   /* 카드리더기로 설정할 건지 멀티패드로 설정할건지를 정하여 셋팅한다 */
                    }else{

//                        mPosSdk.__CardStatusCheck( mDataListener,mPosSdk.getMultiReaderAddr2());    /* 카드리더기로 설정할 건지 멀티패드로 설정할건지를 정하여 셋팅한다 */
                        mPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                            @Override
                            public void onReceived(byte[] _rev, int _type) {
                                if(_type==Command.PROTOCOL_TIMEOUT)
                                {

                                    return;
                                }
                                if(_rev[3]==(byte)0x15){
                                    //장비에서 NAK 올라 옮

                                    return;
                                }
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                Setting.mAuthNum = authNum.trim();
                                n.setmType(4);
                                n.setName(authNum);
                                n.setVersion(version);
                                n.setDeviceName(serialNum);
                                n.setConnected(true);
                                if (n.getmType() == 1) {
//                                    m_txt_line_tvw_product.setText(n.getName());
//                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
//                                    m_txt_line_tvw_version.setText(n.getVersion());

                                } else if(n.getmType() == 4) {
//                                    m_txt_line_tvw_product.setText(n.getName());
//                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
//                                    m_txt_line_tvw_version.setText(n.getVersion());

                                } else if (n.getmType() == 0) {

                                }
                                mPosSdk.setMultiReaderAddr(n.getmAddr());
                                Toast.makeText(instance, "연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                                if (mCreditOrCash)
                                {
                                    Credit(mhashMap);
                                    return;
                                }
                                else
                                {
                                    Cash(mhashMap);
                                    return;
                                }
//                                ShowDialog("장치연결확인");
                            }
                        },new String[]{n.getmAddr()});
                    }

                }
            }
//            SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
//            HideDialog();
//            return;
        }
        else
        {
            Toast.makeText(instance, "연결된된 장치를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
//            ShowDialog("No devices connected to this device.");
            SendreturnData(RETURN_CANCEL,null,ERROR_NODEVICE);
            HideDialog();
            return;
        }
    }

    /**
     * 화면에 다이얼로그 메시지박스를 보여준다
     * @param _str
     * @param _TimerCount
     * @param _dgTimeOut
     */
    private void ReadyDialog(String _str,int _TimerCount, boolean _dgTimeOut)
    {
        HideDialog();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
        ComponentName componentName= info.get(0).topActivity;
        String topActivityName = componentName.getShortClassName().substring(1);

        if(_dgTimeOut == true)
        {
            Setting.setIsAppToApp(true);
            ReadyDialogShow(instance, _str,_TimerCount, true);
        }
        else
        {
            Setting.setIsAppToApp(true);
            ReadyDialogShow(instance, _str,_TimerCount);
        }
    }

    /** 화면에 다이얼로그 메세지박스를 지운다 */
    public void HideDialog() {
        ReadyDialogHide();
    }

    /**
     * 로그파일에 보낼 데이터(메시지)들을 정렬하여 보낸다
     * @param _title
     * @param _time
     * @param _Contents
     */
    private void cout(String _title,String _time, String _Contents)
    {
        if(_title!=null && !_title.equals(""))
        {
            WriteLogFile("\n");
            WriteLogFile("<" + _title + ">\n");
        }

        if(_time!=null && !_time.equals(""))
        {
            WriteLogFile("[" + _time + "]  ");
        }

        if(_Contents!=null && !_Contents.equals(""))
        {
            WriteLogFile(_Contents);
            WriteLogFile("\n");
        }
    }

    /** 앱투앱_현금/신용/가맹점을 실행하기 전에 어플이 정상적으로 셋팅이 완료되었는지를 체크하여 실행한다
     * AppToAppAsyncTask() -> doInBackground(Void... strings) -> onProgressUpdate(Integer... values) -> onPostExecute(Boolean s) 로 진행
     * */

    public class AppToAppAsyncTask extends AsyncTask<Void, Integer, Boolean> {

        public AppToAppAsyncTask()
        {
//            if(Setting.g_bfirstexecAppToApp)
//            {
//                ReadyToastShow(instance,"연결 확인 중입니다",1);
//            }
        }

        @Override
        protected Boolean doInBackground(Void... strings){
            for(int i=1; i< 25; i++)
            {
                publishProgress(i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /* 루프를 돌면서 아래의 읽고/쓰기권한 현재 메인함수의무결성검증이 끝마쳐졌는지를 체크한다 */
                if (Build.VERSION.SDK_INT <= 30) {
                    if(mPosSdk.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            mPosSdk.getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    {
                        return true;
                    }
                }
                if(Setting.g_bMainIntegrity)
                {
                    return true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean s) {
            super.onPostExecute(s);
            /* doInBackground()이 실행후 리턴된 결과값 */
            if(!s)
            {
                if (Build.VERSION.SDK_INT <= 30) {
                    if(mPosSdk.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            mPosSdk.getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        SendreturnData(RETURN_CANCEL,null,"필수 퍼미션을 허락받지 못했습니다. IC앱을 실행 후 퍼미션을 허락 받으세요");
                        return;
                    }
                }

                if(!Setting.g_bMainIntegrity) {
                    SendreturnData(RETURN_CANCEL, null, "무결성 검증 실패. 환경설정의 장치확인이 필요합니다");
                    return;
                }
                SendreturnData(RETURN_CANCEL, null, "무결성 검증 시간 초과. 네트워크 및 장치연결 확인이 필요합니다");
                return;
            }
            else {
                RecvMchIntentData();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            /* doInBackground() 중 업데이트된다. 위의 values[]가 카운트다운되는숫자 */
            if(values[0]==4) {
                ReadyToastShow(instance, "앱 설정 확인 중입니다", 0);
            }
        }

        @Override
        protected void onCancelled(Boolean s) {
            super.onCancelled(s);
        }
    }

//    private String DebugWebComePadTempString()
//    {
//        String temp = "";
//        int Count = mPosSdk.getUsbDevice().size();
//        String aa = mPosSdk.getICReaderAddr().equals("")==true?"true ":"false ";
//        String ab = mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getICReaderAddr())==true?"true ":"false ";
//        String ac = Setting.g_Verity == true?"true":"false";
//
//
//        temp = "Usb Count " + String.valueOf(Count) + "GetICReaderAddr null: " + aa + "현재 USB 장치와 설정된 ICReaderAddr 같은지 다른지  " + ab +
//        " g_Verity : " + ac;
//
//        return temp;
//
//    }

    /**
     * 여기서부터 BLE 관련 데이터 처리
     */
    int bleCount = 0;
    boolean bleFirstCheck = false;
    private synchronized void SetBle()
    {
//        mPosSdk.BleUnregisterReceiver(this);
//        mPosSdk.BleregisterReceiver(Setting.getTopContext());
        mPosSdk.BleConnectionListener(result -> {

            if(result==true)
            {

                if(bleCount > 0)
                {
                    return;
                }
//                        Toast.makeText(instance,"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
//                        ReadyDialogHide();
                HashMap<String, String> hashMap = (HashMap<String, String>) mIntent.getSerializableExtra("hashMap");
                String recv_Command = hashMap.get("TrdType");

                new Handler(Looper.getMainLooper()).post(()-> {
                    if (Setting.getBleName().equals(Setting.getPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                        BleDeviceInfo();
                    } else {
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        if (recv_Command.equals("R10") || recv_Command.equals("R20")) {
                            BleDeviceInfo();
                        } else {
                            setBleInitializeStep();
                        }

                    }
                });
                return;
            }

            SendreturnData(RETURN_CANCEL,null,"BLE 연결에 실패하였습니다." + bleFirstCheck);
            HideDialog();


            return;
        });
        mPosSdk.BleWoosimConnectionListener(result -> {

            if(result==true)
            {

                if(bleCount > 0)
                {
                    return;
                }
//                        Toast.makeText(instance,"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
//                        ReadyDialogHide();
                HashMap<String, String> hashMap = (HashMap<String, String>) mIntent.getSerializableExtra("hashMap");
                String recv_Command = hashMap.get("TrdType");

                new Handler(Looper.getMainLooper()).postDelayed(()-> {
                    if (Setting.getBleName().equals(Setting.getPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                        BleDeviceInfo();
                    } else {
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        if (recv_Command.equals("R10") || recv_Command.equals("R20")) {
                            BleDeviceInfo();
                        } else {
                            setBleInitializeStep();
                        }
                    }
                },500);
                return;
            }

            SendreturnData(RETURN_CANCEL,null,"BLE 연결에 실패하였습니다." + bleFirstCheck);
            HideDialog();


            return;
        });

        mPosSdk.BleIsConnected();

        if(!Setting.getBleIsConnected()) {
            //jiw-221114 테스트필요. 여기서 디스커넥트를 안했을 때 발생할 문제가 있는지 확인이 필요하다(다른 곳에서는 디스커넥트를 하지 않고 있다)
//            mPosSdk.BleDisConnect();
            new Handler(Looper.getMainLooper()).postDelayed(()-> {


                /**
                 * STATE_OFF = 10;
                 * STATE_TURNING_ON = 11;
                 * STATE_ON = 12;
                 * STATE_TURNING_OFF = 13;
                 * STATE_BLE_TURNING_ON = 14;
                 * STATE_BLE_ON = 15;
                 * STATE_BLE_TURNING_OFF = 16;
                 */

                switch (mPosSdk.mblsSdk.GetBlueToothAdapter().getState())
                {
                    case 10:
                    case 13:
                    case 16:
                        SendreturnData(RETURN_CANCEL,null,"블루투스, 위치 설정을 사용으로 해 주세요");
                        HideDialog();
                        return;
                    case 11:
                        break;
                    case 12:
                        break;
                    case 14:
                        break;
                    case 15:
                        break;
                }


                MyBleListDialog myBleListDialog = new MyBleListDialog(this) {
                    @Override
                    protected void onSelectedBleDevice(String bleName, String bleAddr) {
                        if (bleName.equals("") || bleAddr.equals(""))
                        {
                            SendreturnData(RETURN_CANCEL,null,"BLE 연결을 취소하였습니다.");
                            HideDialog();
                            return;
                        }
                        Setting.setIsAppToApp(true);
                        ReadyDialogShow(instance, "장치에 연결중입니다", 0);
//                    ShowDialog(); //여기서 장치연결중이라는 메세지박스를 띄워주어야 한다. 만들지 않아서 일단 주석처리    21-11-24.진우
                        if (!mPosSdk.BleConnect(mPosSdk.getContext(),bleAddr, bleName)) {
                            Toast.makeText(Setting.getTopContext(), "리더기 연결 작업을 먼저 진행해야 합니다", Toast.LENGTH_SHORT).show();
                        }
                        Setting.setBleAddr(bleAddr);
                        Setting.setBleName(bleName);
                    }

                    @Override
                    protected void onFindLastBleDevice(String bleName, String bleAddr) {
                        Setting.setIsAppToApp(true);
                        ReadyDialogShow(instance, "장치에 연결중입니다", 0);
                        if (!mPosSdk.BleConnect(mPosSdk.getContext(),bleAddr, bleName)) {
                            Toast.makeText(Setting.getTopContext(), "리더기 연결에 실패하였습니다", Toast.LENGTH_SHORT).show();
                        }
                        Setting.setBleAddr(bleAddr);
                        Setting.setBleName(bleName);
                    }
                };

                myBleListDialog.show();
            },1000);

        } else {
            //이미 연결되어 있다면
            if(bleCount > 0)
            {
                return;
            }
            bleCount++;
            HashMap<String, String> hashMap = (HashMap<String, String>) mIntent.getSerializableExtra("hashMap");
            String recv_Command = hashMap.get("TrdType");
            final String[] _tid = new String[1];
            final String[] _date = new String[1];
            actAdd(instance);
            switch (recv_Command) {
                case "K21":
                case "K22":
                case "E10":
                case "E20":
                case "E30":
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                            if(BillNo.length()>0 && BillNo.length() != 12)
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                return;
                            }
                            _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                            _date[0] = Utils.getDate("yyMMdd");
//                            if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0], _date[0],BillNo))
//                            {
//                                SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                return;
//                            }
                            Easy(hashMap);
                        }
                    });
                    break;
                case "Q10":
                case "Q20":
                    mhashMap = hashMap;
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            QR_Reader(hashMap);
                        }
                    });
                    break;
                case "A10":
                case "A20":
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                            if(BillNo.length()>0 && BillNo.length() != 12)
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                return;
                            }
                            _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                            _date[0] = Utils.getDate("yyMMdd");
//                            if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
//                            {
//                                SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                return;
//                            }
                            BleCredit(hashMap);
                        }
                    });
                    break;
                case "B10":
                case "B20":
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                            if(BillNo.length()>0 && BillNo.length() != 12)
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                return;
                            }
                            _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                            _date[0] = Utils.getDate("yyMMdd");
//                            if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
//                            {
//                                SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                return;
//                            }
                            BleCash(hashMap);
                        }
                    });
                    break;
                case "Print":   //프린트
                case "P10":
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                            if(BillNo.length()>0 && BillNo.length() != 12)
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                return;
                            }
                            _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                            _date[0] = Utils.getDate("yyMMdd");
                            if(BillNo.length()>0 && !mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
                            {
                                SendreturnData(RETURN_CANCEL,null,"데이터가 없습니다");
                                return;
                            }
                            try {
                                BlePrint(hashMap);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                case "F10":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                            if(BillNo.length()>0 && BillNo.length() != 12)
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                return;
                            }
                            _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                            _date[0] = Utils.getDate("yyMMdd");
                            if(BillNo.equals(""))
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호가 없습니다");
                                return;
                            }
                            AppToApp_ReCommand(mhashMap);
                        }
                    },200);

                    break;
                case "R10":
                case "R20":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                            if(BillNo.length()>0 && BillNo.length() != 12)
                            {
                                SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                return;
                            }
                            _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                            _date[0] = Utils.getDate("yyMMdd");
//                            if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
//                            {
//                                SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                return;
//                            }
                            AppToApp_VersionInfo(mhashMap);
                        }
                    },100);

                    break;
                case "D20":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mhashMap = hashMap;
                            AppToApp_KeyDownload(mhashMap);
                        }
                    },100);

                    break;
                default:
                    break;
            }
        }
    }

    int _bleCount = 0;
    private void setBleInitializeStep()
    {
        Setting.setIsAppToApp(true);
        bleFirstCheck = true;
        ReadyDialogShow(instance, "무결성 검증 중 입니다.",0);
        _bleCount += 1;
        /* 무결성 검증. 초기화진행 */
        DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(this, (result, Code, state, resultData) -> {

            if (result.equals("00")) {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
                ReadyDialogHide();
            }
            else if(result.equals("9999"))
            {

                if(_bleCount >1)
                {
                    _bleCount = 0;
                    mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
                    Toast.makeText(instance, "네트워크 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                    mPosSdk.BleDisConnect();
                    SendreturnData(RETURN_CANCEL,null,"BLE 무결성 검증에 실패하였습니다.");
                    HideDialog();

                }
                else {
                    mPosSdk.BleDisConnect();
                    new Handler().postDelayed(()->{
                        Setting.setIsAppToApp(true);
                        ReadyDialogShow(instance,"무결성 검증 중 입니다.",0);
                    },200);
                    new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                        mPosSdk.BleConnect(mPosSdk.getActivity(),
                                Setting.getPreference(Setting.getTopContext(),Constants.BLE_DEVICE_ADDR),
                                Setting.getPreference(Setting.getTopContext(),Constants.BLE_DEVICE_NAME));
                    },500);
                    return;
                }
            }
            else {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
                Toast.makeText(Setting.getTopContext(), "무결성 검증에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                SendreturnData(RETURN_CANCEL,null,"BLE 무결성 검증에 실패하였습니다.");
                HideDialog();
                return;
            }

//            if(result.equals("9999"))
//            {
//                return;
//            }
            if(!result.equals("00"))
            {
                return;
            }

            new Handler().postDelayed(()->{
                Toast.makeText(Setting.getTopContext(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                //장치 정보 요청
                BleDeviceInfo();
            },500);



        });

        deviceSecuritySDK.Req_BLEIntegrity();   /* 무결성 검증. 키갱신 요청 */


    }

    /**
     * ble 리더기 식별번호 표시를 위한 장치 정보 요청 함
     */
    int _bleDeviceCheck = 0;
    private void BleDeviceInfo(){
        _bleDeviceCheck += 1;
        bleFirstCheck = true;
        mPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddHHmmss"), res ->{
            if(res[3]==(byte)0x15){
                //장비에서 NAK 올라 옮
                SendreturnData(RETURN_CANCEL,null,"BLE 장치정보를 읽지 못했습니다. 연결을 확인해 주십시오.");
                HideDialog();
                return;
            }
            if (res.length == 6) {      //ACK가 6바이트 올라옴
                return;
            }
            if (res.length < 50) {
                SendreturnData(RETURN_CANCEL,null,"BLE 장치정보를 읽지 못했습니다. 연결을 확인해 주십시오.");
                HideDialog();
                return;
            }
            _bleDeviceCheck = 0;
            KByteArray KByteArray = new KByteArray(res);
            KByteArray.CutToSize(4);
            String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
            String serialNum = new String(KByteArray.CutToSize(10));
            String version = new String(KByteArray.CutToSize(5));
            String key = new String(KByteArray.CutToSize(2));
            Setting.mBleHScrKeyYn = key;

            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_NAME,authNum);
            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_VERSION,version);
            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
            //공백을 제거하여 추가 한다.
            String tmp = authNum.trim();
//            Setting.mAuthNum = authNum.trim();    //BLE는 이것을 쓰지 않는다. 유선이 사용한다
            //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
            HideDialog();
            Toast.makeText(mPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(()->{
                //이미 연결되어 있다면
                HashMap<String, String> hashMap = (HashMap<String, String>) mIntent.getSerializableExtra("hashMap");
                String recv_Command = hashMap.get("TrdType");
                actAdd(instance);
                final String[] _tid = new String[1];
                final String[] _date = new String[1];
                if(recv_Command == null)
                {
                    return;
                }
                switch (recv_Command) {
                    case "K21":
                    case "K22":
                    case "E10":
                    case "E20":
                    case "E30":
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                                if(BillNo.length()>0 && BillNo.length() != 12)
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                    return;
                                }
                                _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                                _date[0] = Utils.getDate("yyMMdd");
//                                if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0], _date[0],BillNo))
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                    return;
//                                }
                                Easy(hashMap);
                            }
                        });
                        break;
                    case "Q10":
                    case "Q20":
                        mhashMap = hashMap;
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                QR_Reader(hashMap);
                            }
                        });
                        break;
                    case "A10":
                    case "A20":
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                                if(BillNo.length()>0 && BillNo.length() != 12)
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                    return;
                                }
                                _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                                _date[0] = Utils.getDate("yyMMdd");
//                                if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                    return;
//                                }
                                BleCredit(hashMap);
                            }
                        });
                        break;
                    case "B10":
                    case "B20":
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                                if(BillNo.length()>0 && BillNo.length() != 12)
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                    return;
                                }
                                _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                                _date[0] = Utils.getDate("yyMMdd");
//                                if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                    return;
//                                }
                                BleCash(hashMap);
                            }
                        });
                        break;
                    case "Print":   //프린트
                    case "P10":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                                if(BillNo.length()>0 && BillNo.length() != 12)
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                    return;
                                }
                                _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                                _date[0] = Utils.getDate("yyMMdd");
                                if(BillNo.length()>0 && !mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
                                {
                                    SendreturnData(RETURN_CANCEL,null,"데이터가 없습니다");
                                    return;
                                }
                                try {
                                    BlePrint(hashMap);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        },200);
                        break;
                    case "F10":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                                if(BillNo.length()>0 && BillNo.length() != 12)
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                    return;
                                }
                                _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                                _date[0] = Utils.getDate("yyMMdd");
                                if(BillNo.equals(""))
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호가 없습니다");
                                    return;
                                }
                                AppToApp_ReCommand(mhashMap);
                            }
                        },200);

                        break;
                    case "R10":
                    case "R20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                BillNo = hashMap.get("BillNo") == null ? "":hashMap.get("BillNo");
                                if(BillNo.length()>0 && BillNo.length() != 12)
                                {
                                    SendreturnData(RETURN_CANCEL,null,"전표번호 길이값 은 12자리입니다");
                                    return;
                                }
                                _tid[0] = hashMap.get("TermID") == null ? "":hashMap.get("TermID");
                                _date[0] = Utils.getDate("yyMMdd");
//                                if(BillNo.length()>0 && mPosSdk.checkAppToAppTradeList(_tid[0],_date[0],BillNo))
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,"해당번호는 이미 사용중인 전표번호입니다");
//                                    return;
//                                }
                                AppToApp_VersionInfo(mhashMap);
                            }
                        },100);

                        break;
                    case "D20":
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mhashMap = hashMap;
                                AppToApp_KeyDownload(mhashMap);
                            }
                        },100);
                        break;
                    default:
                        break;
                }
            },500);
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(_bleDeviceCheck==1)
                {
                    mPosSdk.BleDisConnect();
                    new Handler().postDelayed(()->{
                        Setting.setIsAppToApp(true);
                        ReadyDialogShow(instance, "장치 연결 중 입니다.", 0);
                    },200);

                    new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                        mPosSdk.BleConnect(mPosSdk.getActivity(),
                                Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                    },500);
                    return;
                } else if (_bleDeviceCheck > 1)
                {
                    _bleDeviceCheck = 0;
                    SendreturnData(RETURN_CANCEL,null,"블루투스 통신 오류. 다시 시도해 주세요");
                    HideDialog();
                    mPosSdk.BleDisConnect();
                    return;
                }
                else if (_bleDeviceCheck == 0)
                {
                    //정상
                }

            }
        },2000);
    }


    private synchronized void BleCredit(HashMap<String,String> _hashMap)
    {
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");                       //단말기 ID
        String AuDate = _hashMap.get("AuDate");                 //원거래일자 YYMMDD
        String AuNo = _hashMap.get("AuNo");                     //원승인번호
        String KeyYn = _hashMap.get("KeyYn");                   //입력방법 K=keyin, S-swipe, I=ic
        String TrdAmt = _hashMap.get("TrdAmt");                 //거래금액 승인:공급가액, 취소:원승인거래총액
        String TaxAmt = _hashMap.get("TaxAmt");                 //세금
        String SvcAmt = _hashMap.get("SvcAmt");                 //봉사료
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");         //비과세
        String Month = _hashMap.get("Month");                   //할부개월(현금은 X)
        String MchData = _hashMap.get("MchData");               //가맹점데이터
        Setting.setMchdata(MchData);
        String TrdCode = _hashMap.get("TrdCode");               //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        String TradeNo = _hashMap.get("TradeNo");               //Koces거래고유번호(거래고유키 취소 시 사용)
        String CompCode = _hashMap.get("CompCode");             //업체코드(koces에서 부여한 업체코드)
        String DgTmout = _hashMap.get("DgTmout");               //카드입력대기시간 10~99, 거래요청 후 카드입력 대기시간
        String DscYn = _hashMap.get("DscYn");                   //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn(DscYn);
        String DscData = _hashMap.get("DscData");               //위의 dscyn 이 2 혹은 3일 경우 아래로 데이터를 체크하여 서명에 실어서 보낸다
        Setting.setDscData(DscData);
        String FBYn = _hashMap.get("FBYn");                     //fallback 사용 0:fallback 사용 1: fallback 미사용
        String QrNo = _hashMap.get("QrNo");                     //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        String InsYn = _hashMap.get("InsYn");                   //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
        String CancelReason = _hashMap.get("CancelReason");     //취소사유(신용X) 현금영수증 취소 시 필수 1:거래취소 2:오류발급 3:기타
        String CashNum = _hashMap.get("CashNum");               //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        String Print = _hashMap.get("Print");                   //프린트일때 사용
//        String returnAddr = _hashMap.get("returnAddr");         //웹투엡일 경우 해당 웹의 주소도 함께 보내주어야 이곳으로 값을 전달 할 수 있다
        String MtidYn = _hashMap.get("MtidYn");                 //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
        // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면
        //해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다

        if(!CheckTid(mTermID))  //Tid체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
            HideDialog();
            return;
        }

        if(DscYn != null && DscYn.equals("2"))
        {
            if (DscData == null || DscData.equals("") || DscData.length() != 2172)
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NO_BMPDATA);
                HideDialog();
                return;
            }
        }

        mBlePaymentSdk = new BlePaymentSdk(1, (result, Code, resultData)-> {
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        });

        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (TaxFreeAmt == null || TaxFreeAmt.equals(""))
        {
            TaxFreeAmt = "0";
        }
        if (TrdAmt == null || TrdAmt.equals(""))
        {
            TrdAmt = "0";
        }
        int mTrdAmt = Integer.valueOf(TrdAmt) + Integer.valueOf(TaxFreeAmt);
        TrdAmt = String.valueOf(mTrdAmt);

        String CanRes = "";
        if(TrdCode.equals("t") || TrdCode.equals("T"))
        {
            TrdCode = "a";
        } else {
            TrdCode = "0";
        }
        CanRes = TrdCode + AuDate + AuNo;

        /* 로그기록. 신용거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "카드입력대기시간 : " + DgTmout + "," + "전자서명사융유무 : " + DscYn + "," + "fallback사융유무 : " + FBYn;
        cout("RECV : CREDIT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);

//        if(DgTmout.equals("") || DgTmout.equals("0"))   /* 앱투앱으로 결제타임아웃시간을 받아와서 이를 셋팅해준다 */
//        {
//            Setting.setTimeOutValue(20);
//            ReadyDialog("IC를 요청 중입니다.",20, true);
//        }
//        else
//        {
//            Setting.setCommandTimeOut(Integer.valueOf(DgTmout));
//            Setting.setTimeOutValue(Integer.valueOf(DgTmout));
//            ReadyDialog("IC를 요청 중입니다.",Integer.valueOf(DgTmout), true);
//        }
        Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT)));
        Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT)));
        ReadyDialog("IC카드를 넣어주세요",Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT)), true);

        //만일 거래고유키취소사용시 코세스거래고유번호사용
        if(TrdCode.equals("a") && _Command.equals("A20"))
        {
            if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                HideDialog();
                return;
            }
            Setting.g_sDigSignInfo = "4";   //거래고유키 취소일 때 설정한다
            /* 거래고유키 취소시 앱투앱엑티비티에서 처리한다 */
            mPosSdk.___ictrade(TCPCommand.CMD_ICTRADE_CANCEL_REQ, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRes, "K", "", "", null,
                    TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, "", Month, "", "", " ", "", "", null,
                    "", "", "", "", "", "", "", "", "", null,
                    Setting.g_sDigSignInfo, "", "", null, "", MchData, TradeNo, CompCode,
                    Utils.getMacAddress(this), Utils.getHardwareKey(this,true,mTermID),new TcpInterface.DataListener() {
                        @Override
                        public void onRecviced(byte[] _rev) {

                            int _tmpEot = 0;
                            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
                            {
                                if (Setting.getEOTResult() != 0) {
                                    break;
                                }
                                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                                _tmpEot ++;
                                if(_tmpEot >= 30)
                                {
                                    break;
                                }
                            }
                            KByteArray _b = new KByteArray(_rev);
                            _b.CutToSize(10);
                            String _responsecode = new String(_b.CutToSize(3));

                            Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                            String code = new String(tp.getResData().get(0));
                            String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //신용승인번호
                            String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //koces거래고유번호

                            String tmpCardNum1 = new String(tp.getResData().get(4));
                            String[] tmpCardNum2 = tmpCardNum1.split("-");
                            tmpCardNum1 = "";
                            for(String n:tmpCardNum2)
                            {
                                tmpCardNum1 += n;
                            }

                            /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리  */
                            byte[] _CardNum = tmpCardNum1.getBytes();
                            String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum);
                            StringBuffer sb = new StringBuffer();
                            sb.append(tmpCardNum1);
                            if (_CardNum.length > 12) {
                                sb.replace(8, 12, "****");
                            }
                            if (tmpCardNum1.indexOf("=") > 0) {
                                sb.replace(tmpCardNum1.indexOf("="), tmpCardNum1.indexOf("=") + 1, "*");
                            }

                            CardNo = sb.toString();

//                            byte[] _CardNum = tmpCardNum1.getBytes();
//
//                            if(_CardNum != null){
//                                //System.arraycopy( data.get(4),0,_CardNum,0,6);
//                                for(int i=8; i<_CardNum.length; i++){
//                                    //* = 0x2A
//                                    _CardNum[i] = (byte)0x2A;
//                                }
//                                //카드번호가 올라오지만 그중 6개만 사용하고 나머지는 *로 바꾼다
//                            }
//
//                            String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //출력용카드번호

                            //카드 정보 삭제____________________________________________________________________________________
                            Random rand = new Random();
                            for(int i=0; i<_CardNum.length; i++){
                                //* = 0x2A
                                _CardNum[i] = (byte)rand.nextInt(255);;
                            }
                            tp.getResData().set(4,_CardNum);
                            Arrays.fill(_CardNum,(byte)0x01);
                            tp.getResData().set(4,_CardNum);
                            Arrays.fill(_CardNum,(byte)0x00);
                            tp.getResData().set(4,_CardNum);

                            String card_type = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6)); //카드종류
                            String issuer_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7)); //발급사코드
                            String issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8),"EUC-KR"); //발급사명
                            String[] issuer_names = issuer_name.split(" ");
                            issuer_name = "";
                            for(String n:issuer_names)
                            {
                                issuer_name += n;
                            }
                            String purchaser_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9)); //매입사코드
                            String purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10),"EUC-KR"); //매입사명
                            String[] purchaser_names = purchaser_name.split(" ");
                            purchaser_name = "";
                            for(String n:purchaser_names)
                            {
                                purchaser_name += n;
                            }
                            String ddc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11)); //DDC 여부
                            String edc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12)); //EDC 여부

                            String giftcard_money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(15)); //기프트카드 잔액
                            String merchant_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(16)); //가맹점번호
                            String encryp_key_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(25)); //암호키만료잔여일
                            String merchant_data = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(26)); //가맹점데이터

                            HashMap<String, String> sendData = new HashMap<String, String>();

                            sendData.put("TrdType", tp.getResponseCode());
                            sendData.put("TermID", tp.getTerminalID());
                            sendData.put("TrdDate", tp.getDate());
                            sendData.put("AnsCode", code);

                            sendData.put("AuNo", authorizationnumber);
                            sendData.put("TradeNo", kocesTradeUniqueNumber);
                            if(CardNo.length()>8)
                            {
                                sendData.put("CardNo", CardNo.substring(0,8));
                            }
                            else
                            {
                                sendData.put("CardNo", CardNo);
                            }
                            sendData.put("Keydate", encryp_key_expiration_date);
                            sendData.put("MchData", merchant_data);
                            sendData.put("CardKind", card_type); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                            sendData.put("OrdCd", issuer_code);   //발급사코드
                            sendData.put("InpCd", purchaser_code);   //매입사코드
                            sendData.put("OrdNm",issuer_name);   //발급사명
                            sendData.put("InpNm",purchaser_name);   //매입사명
                            sendData.put("DDCYn", ddc_status);   //DDC 여부
                            sendData.put("EDCYn", edc_status);   //EDC 여부
                            sendData.put("GiftAmt", giftcard_money); //기프트카드 잔액
                            sendData.put("MchNo", merchant_number);   //가맹점번호
                            try {
                                sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                            } catch (UnsupportedEncodingException ex) {

                            }
                            /* 로그기록 거래고유키취소 결과 */
                            String mLog ="전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + encryp_key_expiration_date + "," +
                                    "가맹점데이터 : " + merchant_data + "," + "카드종류 : " + card_type + "," + "발급사코드 : " + issuer_code + "," + "발급사명 : " + issuer_name + "," +
                                    "메입사코드 : " + purchaser_code + "," + "메입사명 : " + purchaser_name + "," + "DDC여부 : " + ddc_status + "," + "EDC여부 : " + edc_status + "," +
                                    "기프트잔액 : " + giftcard_money + "," + "가맹점번호 : " + merchant_number;
                            cout("SEND : CREDIT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                            SendreturnData(RETURN_OK,sendData,null);
//                            if(code.equals("0000")) {
//                                String mLog ="전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + encryp_key_expiration_date + "," +
//                                        "가맹점데이터 : " + merchant_data + "," + "카드종류 : " + card_type + "," + "발급사코드 : " + issuer_code + "," + "발급사명 : " + issuer_name + "," +
//                                        "메입사코드 : " + purchaser_code + "," + "메입사명 : " + purchaser_name + "," + "DDC여부 : " + ddc_status + "," + "EDC여부 : " + edc_status + "," +
//                                        "기프트잔액 : " + giftcard_money + "," + "가맹점번호 : " + merchant_number;
//                                cout("SEND : CREDIT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                SendreturnData(RETURN_OK,sendData,null);
//
//                            }
//                            else
//                            {
//                                SendreturnData(RETURN_OK,sendData,null);
////                                SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                            }

                        }
                    });
        }
        else
        {
            if(_Command.equals("A10"))
            {
                /* 신용요청인경우_A10 */
                mBlePaymentSdk.CreditIC(this,mTermID, TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
                        ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),Month,"","", MchData, TradeNo, CompCode, true,FBYn,
                        "","","","","");
            }
            else
            {
                if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
                {
                    SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                    HideDialog();
                    return;
                }
                /* 신용취소인경우_A20 */
                mBlePaymentSdk.CreditIC(this,mTermID, TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
                        ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),Month,AuDate,CanRes, MchData, TradeNo, CompCode, true,FBYn,
                        "","","","","");
            }

        }
    }

    private synchronized void BleCash(HashMap<String,String> _hashMap)
    {
        //ReadyDialog("현금 영수증 처리중입니다.",30, false);
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");
        String AuDate = _hashMap.get("AuDate");
        String AuNo = _hashMap.get("AuNo");
        String Input = _hashMap.get("KeyYn");
        String TrdAmt = _hashMap.get("TrdAmt");
        String TaxAmt = _hashMap.get("TaxAmt");
        String SvcAmt = _hashMap.get("SvcAmt");
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");
        String Month = _hashMap.get("Month");
        String MchData = _hashMap.get("MchData");
        String TrdCode = _hashMap.get("TrdCode");
        String TradeNo = _hashMap.get("TradeNo");
        String CompCode = _hashMap.get("CompCode");
        String DgTmout = _hashMap.get("DgTmout");
        String DscYn = _hashMap.get("DscYn");
        String FBYn = _hashMap.get("FBYn");
        String QrNo = _hashMap.get("QrNo");
        String InsYn = _hashMap.get("InsYn"); //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
        String CancelReason = _hashMap.get("CancelReason"); //취소사유(신용X) 현금영수증 취소 시 필수 1:거래취소 2:오류발급 3:기타
        String CashNum = _hashMap.get("CashNum"); //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        if(CashNum.length()> 12) {
            CashNum = CashNum.substring(0, 13);
        }
        //만일 현금영수증 자진발급(3)일 경우 자진발급번호로 즉시 거래를 진행한다.결제방법을 카드리더기 선택이거나 입력된 번호가 있더라도 모두 무시하고
        //자진발급번호(고객번호) "0100001234" 로 진행한다. 신분확인번호(CashNum)"0100001234" 입력방법(KeyYn)"K" 개인/법인(InsYn) 구분:3
        if(InsYn.equals("3"))
        {
            CashNum = "0100001234";
            Input = "K";
        }
        String KeyYn = Input;


        if(!CheckTid(mTermID))  //Tid체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
            HideDialog();
            return;
        }

        /**
         * cat 과의 동일성을 위해서 ble, 유선일 경우 거래금액 = 거래금액 + 비과세금액 으로 한다. 비과세금액도 정상적으로 함께 보낸다.
         */
        if (TaxFreeAmt == null || TaxFreeAmt.equals(""))
        {
            TaxFreeAmt = "0";
        }
        if (TrdAmt == null || TrdAmt.equals(""))
        {
            TrdAmt = "0";
        }
        int mTrdAmt = Integer.valueOf(TrdAmt) + Integer.valueOf(TaxFreeAmt);
        TrdAmt = String.valueOf(mTrdAmt);


        /* 현금거래시 타임아웃시간을 셋팅한다 */
//        Setting.setTimeOutValue(20);
        Setting.setCommandTimeOut(Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT)));
        Setting.setTimeOutValue(Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT)));
        ReadyDialog("현금 영수증 처리중입니다.",Integer.valueOf(Setting.getPreference(this,Constants.BLE_TIME_OUT)), CashNum.equals("") ? true:false);

        /* 로그기록. 현금거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "키인 : " + KeyYn + "," + "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "카드입력대기시간 : " + DgTmout + "," + "개인법인구분 : " + InsYn + "," + "취소사유 : " + CancelReason + "," + "고객번호 : " + CashNum;
        cout("RECV : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);

        //거래 고유키 예외 처리
        if(_Command.equals("B20"))
        {
            if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                HideDialog();
                return;
            }

            if(TrdCode.equals("T") || TrdCode.equals("t"))
            {
                if(TradeNo.equals(""))
                {
                    SendreturnData(RETURN_CANCEL,null,"거래고유번호가 없습니다.");
                    HideDialog();
                    return;
                }
                String CanRea = "";
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3"))
                {
                    CanRea = "a"  + AuDate + AuNo;
                }
                /* 현금거래고유키 취소 */
                mPosSdk.___cashtrade(_Command, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRea, KeyYn, null, null,
                        TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, InsYn, CancelReason, "", "", MchData, "", TradeNo == null ? "":TradeNo, new TcpInterface.DataListener()
                        {
                            @Override
                            public void onRecviced(byte[] _rev)
                            {

                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                String code = new String(tp.getResData().get(0));
                                String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));
                                String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                HashMap<String, String> sendData = new HashMap<String, String>();
                                sendData.put("TrdType", tp.getResponseCode());
                                sendData.put("TermID", tp.getTerminalID());
                                sendData.put("TrdDate", tp.getDate());
                                sendData.put("AnsCode", code);

                                sendData.put("AuNo", authorizationnumber);
                                sendData.put("TradeNo", kocesTradeUniqueNumber);
                                sendData.put("CardNo", CardNo);
                                sendData.put("Keydate", Encryptionkey_expiration_date);
                                sendData.put("MchData", StoreData);
                                sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                sendData.put("OrdCd", "");   //발급사코드
                                sendData.put("OrdNm", "");   //발급사명
                                sendData.put("InpCd", "");   //매입사코드
                                sendData.put("InpNm", "");   //매입사명
                                sendData.put("DDCYn", "");   //DDC 여부
                                sendData.put("EDCYn", "");   //EDC 여부
                                sendData.put("GiftAmt", ""); //기프트카드 잔액
                                sendData.put("MchNo", "");   //가맹점번호


                                try {
                                    sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                } catch (UnsupportedEncodingException ex) {

                                }
                                /* 로그기록. 현금거래고유키취소 */
                                String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                        "가맹점데이터 : " + StoreData;
                                cout("SEND : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                SendreturnData(RETURN_OK,sendData,null);
//                                if(code.equals("0000")) {
//                                    String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                            "가맹점데이터 : " + StoreData;
//                                    cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                    SendreturnData(RETURN_OK,sendData,null);
//                                }
//                                else
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                }


                            }
                        });
                return;
            }


        }


        if(CashNum.equals("") && KeyYn.equals("S"))  //사용자 정보를 직접 입력 하지 않은 경우 카드 리더기의 현금 영수증 발행
        {

            /* 콜백으로 현금거래결과를 받는다 */
            mBlePaymentSdk = new BlePaymentSdk(2,((result, Code, resultData) -> {
                if(Code.equals("SHOW"))
                {
                    ReadyDialog(result,0,false);
                }
                else
                {
                    CallBackCashReciptResult(result,Code,resultData);
                }
            }));

            /* 취소구분자체크 */
            String CanRes = "";
            if(_Command.equals("B20")) {
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3")) {
                    CanRes = reSetCancelReason(CancelReason) + AuDate + AuNo;
                } else if (_Command.equals("B20") && CancelReason.equals(""))      //취소 커맨드에 취소 구분자가 없는 경우
                {
                    SendreturnData(RETURN_CANCEL, null, ERROR_EMPTY_CANCEL_REASON);
                    HideDialog();
                    return;
                }
            }
            /* 카드(멀티)리더기로 현금체크카드를 사용한다 */
            mBlePaymentSdk.CashRecipt(this,mTermID, TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
                    ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),InsYn.equals("")?1:Integer.valueOf(InsYn), "0000", CanRes,"",KeyYn,CancelReason,
                    "","",MchData,"",TradeNo, true,
                    "","","","","");

        }
        else if (CashNum.equals("") && KeyYn.equals("K"))   //사용자가 정보를 직접 입력 하지 않은 경우 키인입력
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_NOBLEKEYIN);
            HideDialog();
            return;
            /** 211230 kim.jy ble 거래 입력 번호가 없는 경우 KeyYn이 k로 오는 경우에는 에러 처리 한다. */
//            mPaymentSdk = new PaymentSdk(2,true,((result, Code, resultData) -> {
//                if(Code.equals("SHOW"))
//                {
//                    ReadyDialog(result,0,false);
//                } else
//                {
//                    CallBackCashReciptResult(result,Code,resultData);
//                }
//            }));

            /* 취소구분자체크 */
//            String CanRes = "";
//            if(_Command.equals("B20")) {
//                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3")) {
//                    CanRes = reSetCancelReason(CancelReason) + AuDate + AuNo;
//                }
//            }
//            /* 현금거래 서명패드(멀티패드)에 키인입력거래 */
//            mPaymentSdk.CashRecipt(mPosSdk.getActivity(), mTermID,TrdAmt,TaxAmt.equals("")?0:Integer.valueOf( TaxAmt),SvcAmt.equals("")?0:Integer.valueOf( SvcAmt)
//                    ,TaxFreeAmt.equals("")?0:Integer.valueOf( TaxFreeAmt),InsYn.equals("")?1:Integer.valueOf(InsYn), "0000", CanRes,KeyYn,CancelReason,
//                    "","",MchData,"",TradeNo,Command.TYPEDEFINE_SIGNPAD);

        }
        else
        {
            byte[] id = new byte[40];
            Arrays.fill(id,(byte)0x20);
            System.arraycopy(CashNum.getBytes(),0,id,0,CashNum.length());
            if(_Command.equals(TCPCommand.CMD_CASH_RECEIPT_REQ))
            {
                //MchData ="";
                //고객번호를 입력하여 현금거래
                String finalTrdAmt = TrdAmt;
                String finalTaxFreeAmt = TaxFreeAmt;
                mPosSdk.___cashtrade(_Command, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", "", KeyYn, id, null,
                        TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, InsYn, "", "", "", MchData, "", TradeNo, new TcpInterface.DataListener()
                        {
                            @Override
                            public void onRecviced(byte[] _rev)
                            {
                                int _tmpEot = 0;
                                while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
                                {
                                    if (Setting.getEOTResult() != 0) {
                                        break;
                                    }
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    _tmpEot++;
                                    if (_tmpEot >= 30) {
                                        break;
                                    }
                                }
                                KByteArray _b = new KByteArray(_rev);
                                _b.CutToSize(10);
                                String _receivecode = new String(_b.CutToSize(3));

                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                String code = new String(tp.getResData().get(0));
                                String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));

                                String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                HashMap<String, String> sendData = new HashMap<String, String>();
                                sendData.put("TrdType", tp.getResponseCode());
                                sendData.put("TermID", tp.getTerminalID());
                                sendData.put("TrdDate", tp.getDate());
                                sendData.put("AnsCode", code);

                                sendData.put("AuNo", authorizationnumber);
                                sendData.put("TradeNo", kocesTradeUniqueNumber);
                                sendData.put("CardNo", CardNo);

                                sendData.put("Keydate", Encryptionkey_expiration_date);
                                sendData.put("MchData", StoreData);
                                sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                sendData.put("OrdCd", "");   //발급사코드
                                sendData.put("OrdNm", "");   //발급사명
                                sendData.put("InpCd", "");   //매입사코드
                                sendData.put("InpNm", "");   //매입사명
                                sendData.put("DDCYn", "");   //DDC 여부
                                sendData.put("EDCYn", "");   //EDC 여부
                                sendData.put("GiftAmt", ""); //기프트카드 잔액
                                sendData.put("MchNo", "");   //가맹점번호

                                if (Setting.getEOTResult() == 1 && _receivecode.equals("B15"))  /* EOT정상으로 현금영수증전문응답정상실행 */
                                {

                                    try {
                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                    } catch (UnsupportedEncodingException ex) {

                                    }

                                    if(mEotCancel==0)
                                    {
                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                "가맹점데이터 : " + StoreData;
                                        cout("SEND : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                        SendreturnData(RETURN_OK,sendData,null);
                                    }
                                    else
                                    {
                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                    }

//                                    if(code.equals("0000")) {
//                                        if(mEotCancel==0)
//                                        {
//                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                    "가맹점데이터 : " + StoreData;
//                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                            SendreturnData(RETURN_OK,sendData,null);
//                                        }
//                                        else
//                                        {
//                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                        }
//                                    }
//                                    else
//                                    {
//                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                    }
                                }
                                else if (Setting.getEOTResult() == -1 && _receivecode.equals("B15"))      /* EOT비정상으로 현금영수증취소실행 */
                                {
                                    String cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //신용승인번호
                                    String mCancelInfo = "1" + tp.getDate().substring(0,6) + cash_appro_num;
                                    byte[] _tmpCancel = mCancelInfo.getBytes();
                                    _tmpCancel[0] = (byte) 0x49;
                                    mCancelInfo = new String(_tmpCancel);
                                    mEotCancel=1;
                                    //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                                    int mMoney = Integer.parseInt(finalTrdAmt) + Integer.parseInt(TaxAmt) + Integer.parseInt(SvcAmt) + Integer.parseInt(finalTaxFreeAmt);
                                    mPosSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", mCancelInfo, KeyYn, id, null,
                                            String.valueOf(mMoney), "0", "0", "0", InsYn, "1", "", "", MchData, "", TradeNo, new TcpInterface.DataListener() {
                                                @Override
                                                public void onRecviced(byte[] _rev) {
                                                    Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                                    String code = new String(tp.getResData().get(0));
                                                    String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                                    String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                                    String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));

                                                    String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                                    String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                                    String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                                    String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                                    String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                                    String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                                    String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                                    String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                                    String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                                    String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                                    HashMap<String, String> sendData = new HashMap<String, String>();
                                                    sendData.put("TrdType", tp.getResponseCode());
                                                    sendData.put("TermID", tp.getTerminalID());
                                                    sendData.put("TrdDate", tp.getDate());
                                                    sendData.put("AnsCode", code);

                                                    sendData.put("AuNo", authorizationnumber);
                                                    sendData.put("TradeNo", kocesTradeUniqueNumber);
                                                    sendData.put("CardNo", CardNo);

                                                    sendData.put("Keydate", Encryptionkey_expiration_date);
                                                    sendData.put("MchData", StoreData);
                                                    sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                                    sendData.put("OrdCd", "");   //발급사코드
                                                    sendData.put("OrdNm", "");   //발급사명
                                                    sendData.put("InpCd", "");   //매입사코드
                                                    sendData.put("InpNm", "");   //매입사명
                                                    sendData.put("DDCYn", "");   //DDC 여부
                                                    sendData.put("EDCYn", "");   //EDC 여부
                                                    sendData.put("GiftAmt", ""); //기프트카드 잔액
                                                    sendData.put("MchNo", "");   //가맹점번호

                                                    try {
                                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                                    } catch (UnsupportedEncodingException ex) {

                                                    }

                                                    if(mEotCancel==0)
                                                    {

                                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                                "가맹점데이터 : " + StoreData;
                                                        cout("SEND : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                                        SendreturnData(RETURN_OK,sendData,null);
                                                    }
                                                    else
                                                    {

                                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                                    }

//                                                    if(code.equals("0000")) {
//                                                        if(mEotCancel==0)
//                                                        {
//
//                                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                                    "가맹점데이터 : " + StoreData;
//                                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                                            SendreturnData(RETURN_OK,sendData,null);
//                                                        }
//                                                        else
//                                                        {
//
//                                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                                        }
//                                                    }
//                                                    else
//                                                    {
//                                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                                    }
                                                }
                                            });
                                    mCancelInfo = "";
                                    Setting.setEOTResult(0);
                                } else if (Setting.getEOTResult() == 0 && _receivecode.equals("B15"))   /* 승인일때 들어와서 시도하며 취소일때는 하지 않는다 EOT비정상으로 현금영수증취소실행 */
                                {
                                    String cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //신용승인번호
                                    String mCancelInfo = "1" + tp.getDate().substring(0,6) + cash_appro_num;
                                    mEotCancel=1;
                                    //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                                    int mMoney = Integer.parseInt(finalTrdAmt) + Integer.parseInt(TaxAmt) + Integer.parseInt(SvcAmt) + Integer.parseInt(finalTaxFreeAmt);
                                    mPosSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", mCancelInfo, KeyYn, id, null,
                                            String.valueOf(mMoney), "0", "0", "0", InsYn, "1", "", "", MchData, "", TradeNo, new TcpInterface.DataListener() {
                                                @Override
                                                public void onRecviced(byte[] _rev) {
                                                    Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                                    String code = new String(tp.getResData().get(0));
                                                    String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                                    String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                                    String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));

                                                    String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                                    String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                                    String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                                    String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                                    String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                                    String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                                    String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                                    String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                                    String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                                    String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                                    HashMap<String, String> sendData = new HashMap<String, String>();
                                                    sendData.put("TrdType", tp.getResponseCode());
                                                    sendData.put("TermID", tp.getTerminalID());
                                                    sendData.put("TrdDate", tp.getDate());
                                                    sendData.put("AnsCode", code);

                                                    sendData.put("AuNo", authorizationnumber);
                                                    sendData.put("TradeNo", kocesTradeUniqueNumber);
                                                    sendData.put("CardNo", CardNo);

                                                    sendData.put("Keydate", Encryptionkey_expiration_date);
                                                    sendData.put("MchData", StoreData);
                                                    sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                                    sendData.put("OrdCd", "");   //발급사코드
                                                    sendData.put("OrdNm", "");   //발급사명
                                                    sendData.put("InpCd", "");   //매입사코드
                                                    sendData.put("InpNm", "");   //매입사명
                                                    sendData.put("DDCYn", "");   //DDC 여부
                                                    sendData.put("EDCYn", "");   //EDC 여부
                                                    sendData.put("GiftAmt", ""); //기프트카드 잔액
                                                    sendData.put("MchNo", "");   //가맹점번호

                                                    try {
                                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                                    } catch (UnsupportedEncodingException ex) {

                                                    }

                                                    if(mEotCancel==0)
                                                    {

                                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                                "가맹점데이터 : " + StoreData;
                                                        cout("SEND : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                                        SendreturnData(RETURN_OK,sendData,null);
                                                    }
                                                    else
                                                    {

                                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                                    }

//                                                    if(code.equals("0000"))
//                                                    {
//                                                        if(mEotCancel==0)
//                                                        {
//
//                                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                                    "가맹점데이터 : " + StoreData;
//                                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                                            SendreturnData(RETURN_OK,sendData,null);
//                                                        }
//                                                        else
//                                                        {
//
//                                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                                        }
//                                                    }
//                                                    else
//                                                    {
//                                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                                    }
                                                }
                                            });
                                } else if (_receivecode.equals("B25")) /* 현금영수증취소 인경우 */
                                {


                                    try {
                                        sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                    } catch (UnsupportedEncodingException ex) {

                                    }

                                    if(mEotCancel==0)
                                    {

                                        String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                                "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                                "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                                "가맹점데이터 : " + StoreData;
                                        cout("SEND : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                        SendreturnData(RETURN_OK,sendData,null);
                                    }
                                    else
                                    {

                                        SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
                                    }

//                                    if(code.equals("0000")) {
//                                        if(mEotCancel==0)
//                                        {
//
//                                            String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                                    "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                                    "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                                    "가맹점데이터 : " + StoreData;
//                                            cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                            SendreturnData(RETURN_OK,sendData,null);
//                                        }
//                                        else
//                                        {
//
//                                            SendreturnData(RETURN_CANCEL,null,"망취소발생, 거래실패"+sendData.get("Message"));
//                                        }
//                                    }
//                                    else
//                                    {
//                                        SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                    }
                                }


                            }
                        });
            }
            else if(_Command.equals(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ))    //고객번호 입력하여 현금 영수증 취소의 경우
            {
                String CanRea = "";
                if (CancelReason.equals("1") || CancelReason.equals("2") || CancelReason.equals("3")) {
                    CanRea = reSetCancelReason(CancelReason) + AuDate + AuNo;
                }

                mPosSdk.___cashtrade(_Command, mTermID, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", CanRea, KeyYn, id, null,
                        TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, InsYn, CancelReason, "", "", MchData, "", TradeNo, new TcpInterface.DataListener()
                        {
                            @Override
                            public void onRecviced(byte[] _rev)
                            {

                                Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);
                                String code = new String(tp.getResData().get(0));
                                String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(2)); //현금영수증 승인번호
                                String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(3)); //KOCES거래고유번호
                                String CardNo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(4));
                                String PtResCode = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(5));
                                String PtResMessage = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(6));
                                String PtResInfo = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(7));
                                String Encryptionkey_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(8));
                                String StoreData = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(9));
                                String Money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(10));
                                String Tax = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(11));
                                String ServiceCharge = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(12));
                                String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(13));
                                String bangi = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(tp.getResData().get(14));

                                HashMap<String, String> sendData = new HashMap<String, String>();
                                sendData.put("TrdType", tp.getResponseCode());
                                sendData.put("TermID", tp.getTerminalID());
                                sendData.put("TrdDate", tp.getDate());
                                sendData.put("AnsCode", code);

                                sendData.put("AuNo", authorizationnumber);
                                sendData.put("TradeNo", kocesTradeUniqueNumber);
                                sendData.put("CardNo", CardNo);
                                sendData.put("Keydate", Encryptionkey_expiration_date);
                                sendData.put("MchData", StoreData);
                                sendData.put("CardKind", ""); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
                                sendData.put("OrdCd", "");   //발급사코드
                                sendData.put("OrdNm", "");   //발급사명
                                sendData.put("InpCd", "");   //매입사코드
                                sendData.put("InpNm", "");   //매입사명
                                sendData.put("DDCYn", "");   //DDC 여부
                                sendData.put("EDCYn", "");   //EDC 여부
                                sendData.put("GiftAmt", ""); //기프트카드 잔액
                                sendData.put("MchNo", "");   //가맹점번호
                                try {
                                    sendData.put("Message", Utils.getByteToString_euc_kr(tp.getResData().get(1)));
                                } catch (UnsupportedEncodingException ex) {

                                }

                                String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
                                        "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
                                        "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
                                        "가맹점데이터 : " + StoreData;
                                cout("SEND : CASHRECIPT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);
                                SendreturnData(RETURN_OK,sendData,null);

//                                if(code.equals("0000")) {
//                                    String mLog = "전문번호 : " + tp.getResponseCode() + "," + "단말기ID : " + tp.getTerminalID() + "," + "거래일자 : " + tp.getDate() + "," +
//                                            "응답코드 : " + code + "," + "응답메세지 : " + tp.getResData().get(1) + "," + "원승인번호 : " + authorizationnumber + "," +
//                                            "거래고유번호 : " + kocesTradeUniqueNumber + "," + "출력용카드번호 : " + CardNo + "," + "암호키만료잔여일 : " + Encryptionkey_expiration_date + "," +
//                                            "가맹점데이터 : " + StoreData;
//                                    cout("SEND : CASHRECIPT",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                    SendreturnData(RETURN_OK,sendData,null);
//                                }
//                                else
//                                {
//                                    SendreturnData(RETURN_CANCEL,null,sendData.get("Message"));
//                                }
                            }
                        });
            }
        }

    }

    /**
     * 여기서부터 CAT 관련 데이터 처리
     */
    private synchronized void CatCredit(HashMap<String,String> _hashMap)
    {
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");                       //단말기 ID
        String AuDate = _hashMap.get("AuDate");                 //원거래일자 YYMMDD
        String AuNo = _hashMap.get("AuNo");                     //원승인번호
        String KeyYn = _hashMap.get("KeyYn");                   //입력방법 K=keyin, S-swipe, I=ic
        String TrdAmt = _hashMap.get("TrdAmt");                 //거래금액 승인:공급가액, 취소:원승인거래총액
        String TaxAmt = _hashMap.get("TaxAmt");                 //세금
        String SvcAmt = _hashMap.get("SvcAmt");                 //봉사료
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");         //비과세
        String Month = _hashMap.get("Month");                   //할부개월(현금은 X)
        String MchData = _hashMap.get("MchData");               //가맹점데이터
        Setting.setMchdata(MchData);
        String TrdCode = _hashMap.get("TrdCode");               //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        String TradeNo = _hashMap.get("TradeNo");               //Koces거래고유번호(거래고유키 취소 시 사용)
        String CompCode = _hashMap.get("CompCode");             //업체코드(koces에서 부여한 업체코드)
        String DgTmout = _hashMap.get("DgTmout");               //카드입력대기시간 10~99, 거래요청 후 카드입력 대기시간
        String DscYn = _hashMap.get("DscYn");                   //전자서명사용여부(현금X) 0:무서명 1:전자서명
        Setting.setDscyn(DscYn);
        String DscData = _hashMap.get("DscData");               //위의 dscyn 이 2 혹은 3일 경우 아래로 데이터를 체크하여 서명에 실어서 보낸다
        String FBYn = _hashMap.get("FBYn");                     //fallback 사용 0:fallback 사용 1: fallback 미사용
        String QrNo = _hashMap.get("QrNo");                     //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        String InsYn = _hashMap.get("InsYn");                   //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
        String CancelReason = _hashMap.get("CancelReason");     //취소사유(신용X) 현금영수증 취소 시 필수 1:거래취소 2:오류발급 3:기타
        String CashNum = _hashMap.get("CashNum");               //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        String Print = _hashMap.get("Print");                   //프린트일때 사용
//        String returnAddr = _hashMap.get("returnAddr");         //웹투엡일 경우 해당 웹의 주소도 함께 보내주어야 이곳으로 값을 전달 할 수 있다
        String MtidYn = _hashMap.get("MtidYn");                 //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.
        // 1일 경우 가맹점등록 후 정상일 때 일반거래를 처리하며 만일 시리얼번호 사업자번호가 없다면
        //해당 정보 미확인으로 인한 가맹점 등록 실패를 리턴한다

//        if(!CheckTid(mTermID))  //Tid체크
//        {
//            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
//            HideDialog();
//            return;
//        }



//        if(_Command.equals("A20") && TrdCode.equals("")) //취소 커맨드에 취소 구분자가 없는 경우
//        {
//            SendreturnData(RETURN_CANCEL,null,ERROR_EMPTY_CANCEL_REASON);
//            HideDialog();
//            return;
//        }

        /* 로그기록. 신용거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "카드입력대기시간 : " + DgTmout + "," + "전자서명사융유무 : " + DscYn + "," + "fallback사융유무 : " + FBYn;
        cout("RECV : CREDIT_CAT",Utils.getDate("yyyyMMddHHmmss"),mLog);

        mCatPaymentSdk = new CatPaymentSdk(this,Constants.CatPayType.Credit, (result, Code, resultData)->{
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        });

        /** 아래 커스텀박스를 다른 것으로 바꾼다 */
//        ReadyDialog("CAT 거래를 요청 중입니다.",60, true);
        String CanRes = "";
        if(TrdCode.equals("t") || TrdCode.equals("T"))
        {
            TrdCode = "a";
        } else {
            TrdCode = "0";
        }
        if (!AuDate.equals("")) {
            AuDate = AuDate.substring(0,6);
        }
        if (!AuNo.equals("")) {
            AuNo = AuNo.replace(" ", "");
        }
        //만일 취소인 경우
        if(_Command.equals("A20")) {
            if (AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL, null, ERROR_NOAUDATE);
                HideDialog();
                return;
            }

            //취소
            if (TrdCode.equals("a")) {
                mCatPaymentSdk.PayCredit(MtidYn == "1" ? mTermID:mTermID,TrdAmt,TaxAmt,SvcAmt,TaxFreeAmt,AuDate,AuNo,TradeNo,
                        Month,true,MchData,"",true,"","","","","");
            } else {
                mCatPaymentSdk.PayCredit(MtidYn == "1" ? mTermID:mTermID,TrdAmt,TaxAmt,SvcAmt,TaxFreeAmt,AuDate,AuNo,"",
                        Month,true,MchData,"",true,"","","","","");
            }

            return;
        }

        //신용
        mCatPaymentSdk.PayCredit(MtidYn == "1" ? mTermID:mTermID,TrdAmt,TaxAmt,SvcAmt,TaxFreeAmt,"","","",
                Month,false,MchData,"",true,"","","","","");
        return;
    }

    private synchronized void CatCash(HashMap<String,String> _hashMap)
    {
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");
        String AuDate = _hashMap.get("AuDate");
        String AuNo = _hashMap.get("AuNo");
        String Input = _hashMap.get("KeyYn");
        String TrdAmt = _hashMap.get("TrdAmt");
        String TaxAmt = _hashMap.get("TaxAmt");
        String SvcAmt = _hashMap.get("SvcAmt");
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");
        String Month = _hashMap.get("Month");
        String MchData = _hashMap.get("MchData");
        String TrdCode = _hashMap.get("TrdCode");
        String TradeNo = _hashMap.get("TradeNo");
        String CompCode = _hashMap.get("CompCode");
        String DgTmout = _hashMap.get("DgTmout");
        String DscYn = _hashMap.get("DscYn");
        String FBYn = _hashMap.get("FBYn");
        String QrNo = _hashMap.get("QrNo");
        String InsYn = _hashMap.get("InsYn"); //개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천
        String CancelReason = _hashMap.get("CancelReason"); //취소사유(신용X) 현금영수증 취소 시 필수 1:거래취소 2:오류발급 3:기타
        String CashNum = _hashMap.get("CashNum"); //고객번호(신용X) 현금영수증 거래 시: 신분확인 번호
        String Print = _hashMap.get("Print");                   //프린트일때 사용
        String MtidYn = _hashMap.get("MtidYn");                 //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.



        if(CashNum != "" && CashNum.length()> 12) {
            CashNum = CashNum.substring(0, 13);
        }
        //만일 현금영수증 자진발급(3)일 경우 자진발급번호로 즉시 거래를 진행한다.결제방법을 카드리더기 선택이거나 입력된 번호가 있더라도 모두 무시하고
        //자진발급번호(고객번호) "0100001234" 로 진행한다. 신분확인번호(CashNum)"0100001234" 입력방법(KeyYn)"K" 개인/법인(InsYn) 구분:3
        if(InsYn.equals("3"))
        {
            CashNum = "0100001234";
            Input = "K";
        }
        String KeyYn = Input;


//        if(!CheckTid(mTermID))  //Tid체크
//        {
//            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
//            HideDialog();
//            return;
//        }

        String CanRes = "";
        if(TrdCode.equals("t") || TrdCode.equals("T"))
        {
            TrdCode = "a";
        } else {
            TrdCode = "0";
        }
//        if (!AuDate.equals("")) {
//            AuDate = AuDate.substring(0,6);
//        }
        if (!AuNo.equals("")) {
            AuNo = AuNo.replace(" ", "");
        }

        /* 로그기록. 현금거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "키인 : " + KeyYn + "," + "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "카드입력대기시간 : " + DgTmout + "," + "개인법인구분 : " + InsYn + "," + "취소사유 : " + CancelReason + "," + "고객번호 : " + CashNum;
        cout("RECV : CASHRECIPT_CAT",Utils.getDate("yyyyMMddHHmmss"),mLog);

        mCatPaymentSdk = new CatPaymentSdk(this,Constants.CatPayType.Cash, (result, Code, resultData)->{
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        });

        //만일 취소인 경우
        if(_Command.equals("B20")) {
            if (AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL, null, ERROR_NOAUDATE);
                HideDialog();
                return;
            }

            //취소
            if (TrdCode.equals("a")) {
                if (CashNum == "") {
                    mCatPaymentSdk.CashRecipt(MtidYn == "1" ? mTermID:mTermID, TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, AuDate, AuNo, TradeNo,
                            "", "", InsYn, true, CancelReason, MchData, "", true,
                            "","","","","");
                } else {
                    mCatPaymentSdk.CashRecipt(MtidYn == "1" ? mTermID:mTermID, TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, AuDate, AuNo, TradeNo,
                            "", CashNum, InsYn, true, CancelReason, MchData, "", true,
                            "","","","","");
                }

            } else {
                if (CashNum == "") {
                    mCatPaymentSdk.CashRecipt(MtidYn == "1" ? mTermID:mTermID, TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, AuDate, AuNo, "",
                            "", "", InsYn, true, CancelReason, MchData, "", true,
                            "","","","","");
                } else {
                    mCatPaymentSdk.CashRecipt(MtidYn == "1" ? mTermID:mTermID, TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, AuDate, AuNo, "",
                            "", CashNum, InsYn, true, CancelReason, MchData, "", true,
                            "","","","","");
                }

            }

            return;
        }

        if (CashNum == "") {
            mCatPaymentSdk.CashRecipt(MtidYn == "1" ? mTermID:mTermID, TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, "", "", "",
                    "", "", InsYn, false, "", MchData, "", true,
                    "","","","","");
        } else {
            mCatPaymentSdk.CashRecipt(MtidYn == "1" ? mTermID:mTermID, TrdAmt, TaxAmt, SvcAmt, TaxFreeAmt, "", "", "",
                    "", CashNum, InsYn, false, "", MchData, "", true,
                    "","","","","");
        }

        return;
    }

    private synchronized void CatCashIC(HashMap<String,String> _hashMap)
    {

    }

    private synchronized void CatAppCard(HashMap<String,String> _hashMap)
    {
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");
        String AuDate = _hashMap.get("AuDate");
        String AuNo = _hashMap.get("AuNo");
        String TrdAmt = _hashMap.get("TrdAmt");
        String TaxAmt = _hashMap.get("TaxAmt");
        String SvcAmt = _hashMap.get("SvcAmt");
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");
        String Month = _hashMap.get("Month");
        String MchData = _hashMap.get("MchData") == null ? "":_hashMap.get("MchData");               //가맹점데이터
        Setting.setMchdata(MchData);
        String TrdCode = _hashMap.get("TrdCode");               //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        String TradeNo = _hashMap.get("TradeNo");               //Koces거래고유번호(거래고유키 취소 시 사용)
        String CompCode = _hashMap.get("CompCode");             //업체코드(koces에서 부여한 업체코드)
        String DscYn = _hashMap.get("DscYn");                   //전자서명사용여부(현금X) 0:무서명 1:전자서명 2: bmp data 사용
        Setting.setDscyn(DscYn);
        String DscData = _hashMap.get("DscData");               //전자서명 데이터 위의 전자서명여부 2를 선택시 사용한다
        Setting.setDscData(DscData);
        String QrNo = _hashMap.get("QrNo");                     //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        String QrKind = _hashMap.get("QrKind");                 //간편결제거래종류
        String SearchNumber = _hashMap.get("TradeNo");     //조회번호
        String HostMchData = _hashMap.get("HostMchData");     //HOST 전송 가맹점 정보(제로페이)
        String MtidYn = _hashMap.get("MtidYn");                 //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.

        if(_Command.equals("E20"))
        {
            if(QrKind.equals("UN"))
            {
                SendreturnData(RETURN_CANCEL,null,"취소시에는 UN 통합 사용 불가");
                HideDialog();
                return;
            }
        }
//        if(!CheckTid(mTermID))  //Tid체크
//        {
//            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
//            HideDialog();
//            return;
//        }

        //간편결제라고 설정함.
        Setting.setEasyCheck(true);
        /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
        mCatPaymentSdk = new CatPaymentSdk(this,Constants.CatPayType.Easy, (result, Code, resultData)->{
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        });

        /** 아래 커스텀박스를 다른 것으로 바꾼다  */
        if (!AuDate.equals("")) {
            AuDate = AuDate.substring(0, 6);
        }
        if (!AuNo.equals("")) {
            AuNo = AuNo.replace(" ", "");
        }


        //만일 취소인 경우
        if (_Command.equals("E20")) {
            if (AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                HideDialog();
                return;
            }
        }

        mCatPaymentSdk.EasyRecipt(_Command,mTermID,QrNo,
                TrdAmt,
                TaxAmt,
                SvcAmt,
                TaxFreeAmt,QrKind,
                AuDate,
                AuNo,"",Month,MchData,HostMchData,
                "",
                true,
                "",
                "","","","");
    }

    private synchronized void QR_Reader(HashMap<String,String> _hashMap)
    {
        String _Command = _hashMap.get("TrdType");
        String Msg1 = _hashMap.get("Msg1");
        String Msg2 = _hashMap.get("Msg2");
        String Msg3 = _hashMap.get("Msg3");

        /** 만일 현재 CAT 연동중이라면 여기서 CAT_APP으로 보낸다. */
        /** 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        switch (Setting.g_PayDeviceType) {
            case BLE:
            case CAT:       //WIFI CAT의 경우
                intentIntegrator = new IntentIntegrator(this);
                intentIntegrator.setOrientationLocked(false);   //세로
                intentIntegrator.setPrompt("");
                intentIntegrator.setTimeout(30 * 1000);
                intentIntegrator.setBeepEnabled(false);
                intentIntegrator.initiateScan();
                return;
            case LINES:       //WIFI CAT의 경우
//                CatAppCard(_hashMap);
                if (Setting.getPreference(this, Constants.LINE_QR_READER).equals(Constants.LineQrReader.CardReader.toString()) ||
                        Setting.getPreference(this, Constants.LINE_QR_READER).equals(Constants.LineQrReader.SignPad.toString())) {
                    /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
                    mPaymentSdk = new PaymentSdk(1, true, ((result, Code, resultData) -> {
                        if (Code.equals("SHOW")) {
                            ReadyDialog(result, 0, false);
                        } else {
                            if (result.contains("NAK")) {
                                CallBackCashReciptResult("사용이 불가한 장치 입니다", Code, resultData);
                                return;
                            }
                            CallBackCashReciptResult(result, Code, resultData);   /* 콜백으로 결과를 받아온다 */
                        }
                    }));
                    ReadyDialog("바코드/QR코드 읽혀주세요",Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)),true);
                    mPaymentSdk.BarcodeReader(this, Msg1, Msg2, Msg3);
                }
                else
                {
                    intentIntegrator = new IntentIntegrator(this);
                    intentIntegrator.setOrientationLocked(false);   //세로
                    intentIntegrator.setPrompt("");
                    intentIntegrator.setTimeout(30 * 1000);
                    intentIntegrator.setBeepEnabled(false);
                    intentIntegrator.initiateScan();

                }
                return;
        }
    }

    private synchronized void  Easy(HashMap<String,String> _hashMap)
    {
        String _Command = _hashMap.get("TrdType");
        mTermID = _hashMap.get("TermID");
        String AuDate = _hashMap.get("AuDate");
        String AuNo = _hashMap.get("AuNo");
        String TrdAmt = _hashMap.get("TrdAmt");
        String TaxAmt = _hashMap.get("TaxAmt");
        String SvcAmt = _hashMap.get("SvcAmt");
        String TaxFreeAmt = _hashMap.get("TaxFreeAmt");
        String Month = _hashMap.get("Month");
        String MchData = _hashMap.get("MchData");               //가맹점데이터
        Setting.setMchdata(MchData);
        String TrdCode = _hashMap.get("TrdCode");               //거래구분(T:거래고유키취소, C:해외은련, A:App카드결제 U:BC(은련) 또는 QR결제 (일반신용일경우 미설정)
        String TradeNo = _hashMap.get("TradeNo");               //Koces거래고유번호(거래고유키 취소 시 사용)
        String CompCode = _hashMap.get("CompCode");             //업체코드(koces에서 부여한 업체코드)
        String DscYn = _hashMap.get("DscYn");                   //전자서명사용여부(현금X) 0:무서명 1:전자서명 2: bmp data 사용
        Setting.setDscyn(DscYn);
        String DscData = _hashMap.get("DscData");               //전자서명 데이터 위의 전자서명여부 2를 선택시 사용한다
        Setting.setDscData(DscData);
        String QrKind = _hashMap.get("QrKind");                 //간편결제거래종류
        String QrNo = _hashMap.get("QrNo");                     //QR, 바코드번호 QR, 바코드 거래 시 사용(App 카드, BC QR 등)
        String HostMchData = _hashMap.get("HostMchData");                     //HOST 전송 가맹점 정보(제로페이)
        String SearchNumber = _hashMap.get("TradeNo");     //조회번호
        String MtidYn = _hashMap.get("MtidYn");                 //다중사업자 0: 일반, 1: 다중사업자(가맹점 등록 포함) 0 일경우 일반거래로 처리.

        if(_Command.equals("E20"))
        {
            if(QrKind.equals("UN"))
            {
                SendreturnData(RETURN_CANCEL,null,"취소시에는 UN 통합 사용 불가");
                HideDialog();
                return;
            }
        }

        /** 만일 현재 CAT 연동중이라면 여기서 CAT_APP으로 보낸다. */
        /** 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        switch (Setting.g_PayDeviceType) {
            case CAT:       //WIFI CAT의 경우
                if (Setting.getPreference(this, Constants.CAT_QR_READER).equals(Constants.CatQrReader.CatReader.toString())) {
                    CatAppCard(_hashMap);
                    return;
                }
                if (QrNo == null || QrNo.equals("")) {
                    if (QrKind.equals("WC") || QrKind.equals("ZP")) {
                        if (_Command.equals("E20")) {

                        } else {
                            intentIntegrator = new IntentIntegrator(this);
                            intentIntegrator.setOrientationLocked(false);   //세로
                            intentIntegrator.setPrompt("");
                            intentIntegrator.setTimeout(30 * 1000);
                            intentIntegrator.setBeepEnabled(false);
                            intentIntegrator.initiateScan();
                            return;
                        }
                    }
                    else
                    {
                        intentIntegrator = new IntentIntegrator(this);
                        intentIntegrator.setOrientationLocked(false);   //세로
                        intentIntegrator.setPrompt("");
                        intentIntegrator.setTimeout(30 * 1000);
                        intentIntegrator.setBeepEnabled(false);
                        intentIntegrator.initiateScan();
                        return;
                    }

                }
                break;
            case LINES:       //WIFI CAT의 경우
//                CatAppCard(_hashMap);
                if (QrNo == null || QrNo.equals("")) {
                    if (Setting.getPreference(this, Constants.LINE_QR_READER).equals(Constants.LineQrReader.CardReader.toString()) ||
                            Setting.getPreference(this, Constants.LINE_QR_READER).equals(Constants.LineQrReader.SignPad.toString())) {
                        /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
                        mPaymentSdk = new PaymentSdk(1, true, ((result, Code, resultData) -> {
                            if (Code.equals("SHOW")) {
                                ReadyDialog(result, 0, false);
                            } else {
                                if (result.contains("NAK")) {
                                    CallBackCashReciptResult("사용이 불가한 장치 입니다", Code, resultData);
                                    return;
                                }
                                CallBackCashReciptResult(result, Code, resultData);   /* 콜백으로 결과를 받아온다 */
                            }
                        }));
                        ReadyDialog("바코드/QR코드 읽혀주세요",Integer.valueOf(Setting.getPreference(this,Constants.USB_TIME_OUT)),true);
                        mPaymentSdk.BarcodeReader(this, "간편결제", "금액: " + TrdAmt + "원", "바코드/QR 코드를 읽혀주세요");
                        return;
                    }

                    if (QrKind.equals("WC") || QrKind.equals("ZP")) {
                        if (_Command.equals("E20")) {

                        } else {
                            intentIntegrator = new IntentIntegrator(this);
                            intentIntegrator.setOrientationLocked(false);   //세로
                            intentIntegrator.setPrompt("");
                            intentIntegrator.setTimeout(30 * 1000);
                            intentIntegrator.setBeepEnabled(false);
                            intentIntegrator.initiateScan();
                            return;
                        }
                    }
                    else
                    {
                        intentIntegrator = new IntentIntegrator(this);
                        intentIntegrator.setOrientationLocked(false);   //세로
                        intentIntegrator.setPrompt("");
                        intentIntegrator.setTimeout(30 * 1000);
                        intentIntegrator.setBeepEnabled(false);
                        intentIntegrator.initiateScan();
                        return;
                    }
                }
                break;
            case BLE:
                if (QrNo == null || QrNo.equals("")) {
                    if (QrKind.equals("WC") || QrKind.equals("ZP")) {
                        if (_Command.equals("E20")) {

                        } else {
                            intentIntegrator = new IntentIntegrator(this);
                            intentIntegrator.setOrientationLocked(false);   //세로
                            intentIntegrator.setPrompt("");
                            intentIntegrator.setTimeout(30 * 1000);
                            intentIntegrator.setBeepEnabled(false);
                            intentIntegrator.initiateScan();
                            return;
                        }
                    }
                    else
                    {
                        intentIntegrator = new IntentIntegrator(this);
                        intentIntegrator.setOrientationLocked(false);   //세로
                        intentIntegrator.setPrompt("");
                        intentIntegrator.setTimeout(30 * 1000);
                        intentIntegrator.setBeepEnabled(false);
                        intentIntegrator.initiateScan();
                        return;
                    }
                }
                break;
        }


        switch (Setting.g_PayDeviceType) {
            case CAT:       //WIFI CAT의 경우
                CatAppCard(_hashMap);
                return;
        }

        if(!CheckTid(mTermID))  //Tid체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHTID_DIFF);
            HideDialog();
            return;
        }

        if(DscYn != null && DscYn.equals("2"))
        {
            if (DscData == null || DscData.equals("") || DscData.length() != 2172)
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NO_BMPDATA);
                HideDialog();
                return;
            }
        }

        //간편결제라고 설정함.
        Setting.setEasyCheck(true);

        /* 결제관련 프로세스실행 콜백으로 결과를 받는다 */
        mEasyPaymentSdk = new EasyPaySdk(this,true,((result, Code, resultData) -> {
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        }));

        if (TaxFreeAmt == null || TaxFreeAmt.equals(""))
        {
            TaxFreeAmt = "0";
        }
        if (TrdAmt == null || TrdAmt.equals(""))
        {
            TrdAmt = "0";
        }
        if (TaxAmt == null || TaxAmt.equals(""))
        {
            TaxAmt = "0";
        }
        if (SvcAmt == null || SvcAmt.equals(""))
        {
            SvcAmt = "0";
        }
        int mTrdAmt = Integer.valueOf(TrdAmt) + Integer.valueOf(TaxFreeAmt);
        TrdAmt = String.valueOf(mTrdAmt);

        /* 로그기록. 신용거래요청 */
        String mLog = "TrdType : " + _Command + "," +"단말기ID : " + mTermID + "," + "원거래일자 : " + AuDate + "," + "원승인번호 : " + AuNo + "," +
                "거래금액 : " + TrdAmt + "," + "세금 : " + TaxAmt + "," + "봉사료 : " + SvcAmt + "," + "비과세 : " + TaxFreeAmt + "," +
                "할부개월 : " + Month + "," + "가맹점데이터 : " + MchData + "," + "거래구분 : " + TrdCode + "," + "거래고유번호 : " + TradeNo + "," +
                "업체코드 : " + CompCode + "," + "전자서명사융유무 : " + DscYn;
        cout("RECV : CREDIT_EASY",Utils.getDate("yyyyMMddHHmmss"),mLog);

        if (QrKind.equals("ZP"))
        {
            if(_Command.equals("A10"))
            {
                _Command = "Z10";
            }
            else if(_Command.equals("A20"))
            {
                _Command = "Z20";
            }
            else if(_Command.equals("E10"))
            {
                _Command = "Z10";
            }
            else if(_Command.equals("E20"))
            {
                _Command = "Z20";
            }
        }

        if (_Command.equals("A10"))
        {
            _Command = "K21";
        }
        else if (_Command.equals("A20"))
        {
            _Command = "K22";
        }
        else if(_Command.equals("E10"))
        {
            _Command = "K21";
        }
        else if(_Command.equals("E20"))
        {
            _Command = "K22";
        }



        Setting.setTimeOutValue(30);
        ReadyDialog("거래 승인 중 입니다",30, false);

        if(_Command.equals("K21") || _Command.equals("Z10"))
        {
            /* 신용요청인경우_A10 */
            mEasyPaymentSdk.EasyPay(_Command,mTermID,Utils.getDate("yyMMddHHmmss"),Constants.TEST_SOREWAREVERSION,"","",
                    "","","B",QrNo,new byte[]{},TrdAmt,TaxAmt,SvcAmt,TaxFreeAmt,"",Month,"","","",
                    "","","","","","",Setting.getDscyn(),"",Setting.getDscData().getBytes(),
                    MchData,HostMchData,TradeNo,"","","","","",QrKind);
        }
        else
        {
            if(AuDate.equals("") || AuNo.equals("")) //만일 원승인번호 원승인일자가 없는경우
            {
                SendreturnData(RETURN_CANCEL,null,ERROR_NOAUDATE);
                HideDialog();
                return;
            }
            /* 신용취소인경우_A20 */
            int mMoney = mTrdAmt + Integer.parseInt(SvcAmt) + Integer.parseInt(TaxAmt);
            mEasyPaymentSdk.EasyPay(_Command,mTermID,Utils.getDate("yyMMddHHmmss"),Constants.TEST_SOREWAREVERSION,"","0",
                    AuDate,AuNo,"B",QrNo,new byte[]{},String.valueOf(mMoney),"0","0","0","",Month,"","0","B",
                    "","","","",SearchNumber,"",Setting.getDscyn(),"",Setting.getDscData().getBytes(),
                    MchData,HostMchData,TradeNo,"","","","","",QrKind);
        }
        return;
    }

    private synchronized void PrintRecipt(HashMap<String,String> _hashMap) throws UnsupportedEncodingException {
        String _Command = _hashMap.get("TrdType");
        String _Print = _hashMap.get("Print");
//        printMsg = "";

        if(_Command.equals("Print"))
        {
            if (_Print == null || _Print.equals("")) {
                SendreturnData(RETURN_CANCEL,null,"프린트 내용이 없습니다.");
                HideDialog();
                return;
            }
        }


        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                ReadyToastShow(instance,"환경설정에 장치 설정을 해야 합니다.",1);
                SendreturnData(RETURN_CANCEL,null,ERROR_SET_ENVIRONMENT);
                HideDialog();

                return;
            case BLE:       //BLE의 경우
//                BleCash(_hashMap);
//                mPosSdk.BleregisterReceiver(this);
                SetBle();
                return;
            case CAT:       //WIFI CAT의 경우
                //이곳에서 진행한다
                break;
            case LINES:     //유선장치의 경우

                ReadyToastShow(instance,"지원하지 않는 장비입니다",1);
                SendreturnData(RETURN_CANCEL,null,ERROR_NOT_SUPPORT_DEVICE);
                HideDialog();

                return;
            default:
                break;
        }

        mCatPaymentSdk = new CatPaymentSdk(this,Constants.CatPayType.Print, (result, Code, resultData)->{
            if(Code.equals("SHOW"))
            {
                ReadyDialog(result,0,false);
            } else
            {
                CallBackCashReciptResult(result,Code,resultData);   /* 콜백으로 결과를 받아온다 */
            }
        });

        if(_Command.equals("Print"))
        {
            /* 로그기록. 프린트 */
            String mLog = "TrdType : " + _Command + ", " + "Print : " + _Print;
            cout("RECV : PRINT_CAT",Utils.getDate("yyyyMMddHHmmss"),mLog);
            mCatPaymentSdk.Print(_Print,true);
            return;
        }

        if(_Command.equals("P10"))
        {
            String _TermId = _hashMap.get("TermID");
            String _AuDate = _hashMap.get("AuDate");
            String _BillNo = _hashMap.get("BillNo");

            //가맹점명
            String _ShpNm = _hashMap.get("ShpNm");
            //사업자번호
            String _BsnNo = _hashMap.get("BsnNo");
            //단말기TID
            String _TermID = _hashMap.get("TermID");
            //대표자명
            String _PreNm = _hashMap.get("PreNm");
            //연락처
            String _ShpTno = _hashMap.get("ShpTno");
            //주소
            String _ShpAr = _hashMap.get("ShpAr");

            if (_ShpNm == null || _ShpNm.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_BsnNo == null || _BsnNo.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_TermID == null || _TermID.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_PreNm == null || _PreNm.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_ShpTno == null || _ShpTno.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_ShpAr == null || _ShpAr.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            }


            /* 로그기록. 프린트 */
            String mLog = "TrdType : " + _Command + ", " + "TermID : " + _TermId + ", " +
                    "거래일자 : " + _AuDate + ", " + "가맹점명칭 : " + _ShpNm + ", " +
                    "사업자번호 : " + _BsnNo + ", " + "대표자명 : " + _PreNm + ", " +
                    "가맹점전화번호 : " + _ShpTno + ", " + "가맹점주소 : " + _ShpAr;
            cout("RECV : PRINT_CAT",Utils.getDate("yyyyMMddHHmmss"),mLog);

            HashMap<String,String> reHash = new HashMap<>();
            reHash = mPosSdk.getAppToAppTradeResult(_TermId,_AuDate,_BillNo);
            if (reHash.get("AnsCode") == null || !(reHash.get("AnsCode")).equals("0000"))
            {
                SendreturnData(RETURN_CANCEL,null,"승인 데이터가 없습니다");
                HideDialog();
                return;
            }

            // 신용승인/신용취소/현금승인/현금취소 총4개로 구분지어서 파싱한다
            //신용매출
            if ((reHash.get("TrdType")).equals("A25")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("카드취소")) + Constants.PENTER);
                printCredit(mhashMap,reHash);
            } else if((reHash.get("TrdType")).equals("A15")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("카드승인")) + Constants.PENTER);
                printCredit(mhashMap,reHash);
            }
            //간편매출
            else if ((reHash.get("TrdType")).equals("E25")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("간편취소")) + Constants.PENTER);
                printEasy(mhashMap,reHash);
            } else if ((reHash.get("TrdType")).equals("E15")){
                printParser(Utils.PrintCenter(Utils.PrintBold("간편승인")) + Constants.PENTER);
                printEasy(mhashMap,reHash);
            } else if ((reHash.get("TrdType")).equals("E35")) {
                SendreturnData(RETURN_CANCEL,null,"제로페이 취소 조회 응답데이터 입니다");
                HideDialog();
                return;
            }
            //현금매출
            else if ((reHash.get("TrdType")).equals("B25")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("현금취소")) + Constants.PENTER);
                printCash(mhashMap,reHash);
            } else if ((reHash.get("TrdType")).equals("B15")){
                printParser(Utils.PrintCenter(Utils.PrintBold("현금승인")) + Constants.PENTER);
                printCash(mhashMap,reHash);
            }
            String _totalMsg = "";
            _totalMsg += printMsg;
            mCatPaymentSdk.Print(_totalMsg,true);
            return;
        }

    }

    private synchronized void BlePrint(HashMap<String,String> _hashMap) throws UnsupportedEncodingException {
        String _Command = _hashMap.get("TrdType");
        final String _Print = _hashMap.get("Print");
//        printMsg = "";

        if(_Command.equals("Print")) {
            if (_Print == null || _Print.equals("")) {
                SendreturnData(RETURN_CANCEL, null, "프린트 내용이 없습니다.");
                HideDialog();
                return;
            }
        }

        mPosSdk.BleUnregisterReceiver(Setting.getTopContext());
        mPosSdk.BleregisterReceiver(Setting.getTopContext());


        if ((Setting.getPreference(this, Constants.BLE_DEVICE_NAME)).contains(Constants.C1_KRE_OLD_NOT_PRINT) ||
                (Setting.getPreference(this, Constants.BLE_DEVICE_NAME)).contains(Constants.KWANGWOO_KRE))
        {
            SendreturnData(RETURN_CANCEL,null,"프린트 할 수 없는 장비입니다.");
            HideDialog();
            return;
        }
//        mPosSdk.__BLEDeviceInit(null, "99");
        ReadyDialog("프린트 중입니다",20, false);
        if(_Command.equals("Print"))
        {
            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
//            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
                try {
                    String _totalMsg = "";
                    _totalMsg += _Print;
                    byte[] text = null;
                    printParser(Utils.PrintLine("  ") + Constants.PENTER);
                    printParser(Utils.PrintLine("  ") + Constants.PENTER);
                    byte[] prtStr = Command.BLEPrintParser(_totalMsg);
                    String string = _totalMsg;

                    text = prtStr;

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byteStream.write(WoosimCmd.setTextStyle(true, true, false, 1, 1));
                    byteStream.write(WoosimCmd.setTextAlign(0));
                    if (text != null) byteStream.write(text);
                    byteStream.write(WoosimCmd.printData());


                    mPosSdk.__Printer(Utils.MakePacket((byte)0xc6,byteStream.toByteArray()), new bleSdkInterface.ResDataListener() {
                        @Override
                        public void OnResult(byte[] res) {

                            if (res[3] == Command.CMD_PRINT_RES)
                            {
                                HashMap<String,String> hashMap = new HashMap<String, String>();
                                HideDialog();
                                if (res[4] == 0x30) {
                                    hashMap.put("Message","프린트를 완료하였습니다.");
                                    /* 로그기록. 버전정보 */
                                    cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
                                    SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
                                } else if (res[4] == 0x31) {
                                    hashMap.put("Message","용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                } else if (res[4] == 0x32) {
                                    hashMap.put("Message","배터리 부족으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "배터리 부족으로 프린트를 실패하였습니다");
                                } else {
                                    hashMap.put("Message","프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "프린트를 실패하였습니다");
                                }
//                                HideDialog();
//                                mPosSdk.__BLEDeviceInit(null, "99");
                                return;
                            }

                            if (res[9] == Command.CMD_PRINT_RES)
                            {
                                HashMap<String,String> hashMap = new HashMap<String, String>();
                                HideDialog();
                                if (res[10] == 0x30) {
                                    hashMap.put("Message","프린트를 완료하였습니다.");
                                    /* 로그기록. 버전정보 */
                                    cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
                                    SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
                                } else if (res[10] == 0x31) {
                                    hashMap.put("Message","용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                } else if (res[10] == 0x32) {
                                    hashMap.put("Message","배터리 부족으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "배터리 부족으로 프린트를 실패하였습니다");
                                } else {
                                    hashMap.put("Message","프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "프린트를 실패하였습니다");
                                }
//                                HideDialog();
//                                mPosSdk.__BLEDeviceInit(null, "99");
                                return;
                            }
                        }
                    });



                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    String _totalMsg = "";
                    _totalMsg += _Print;
                    /* 로그기록. 프린트 */
                    String mLog = "TrdType : " + _Command + ", " + "Print : " + _totalMsg;
                    cout("RECV : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);

                    byte[] prtStr = null;
                    prtStr = Command.BLEPrintParser(_totalMsg);

                    mPosSdk.__Printer(prtStr, new bleSdkInterface.ResDataListener() {
                        @Override
                        public void OnResult(byte[] res) {

                            if (res[3] == Command.CMD_PRINT_RES)
                            {
                                HashMap<String,String> hashMap = new HashMap<String, String>();
                                HideDialog();
                                if (res[4] == 0x30) {
                                    hashMap.put("Message","프린트를 완료하였습니다.");
                                    /* 로그기록. 버전정보 */
                                    cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
                                    SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
                                } else if (res[4] == 0x31) {
                                    hashMap.put("Message","용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                } else if (res[4] == 0x32) {
                                    hashMap.put("Message","배터리 부족으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "배터리 부족으로 프린트를 실패하였습니다");
                                } else {
                                    hashMap.put("Message","프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "프린트를 실패하였습니다");
                                }
//                                HideDialog();
//                                mPosSdk.__BLEDeviceInit(null, "99");
                                return;
                            }
                        }
                    });
                }
            },1000);

        }
        else if(_Command.equals("P10"))
        {
            String _TermId = _hashMap.get("TermID");
            String _AuDate = _hashMap.get("AuDate");
            String _BillNo = _hashMap.get("BillNo");

            //가맹점명
            String _ShpNm = _hashMap.get("ShpNm");
            //사업자번호
            String _BsnNo = _hashMap.get("BsnNo");
            //단말기TID
            String _TermID = _hashMap.get("TermID");
            //대표자명
            String _PreNm = _hashMap.get("PreNm");
            //연락처
            String _ShpTno = _hashMap.get("ShpTno");
            //주소
            String _ShpAr = _hashMap.get("ShpAr");

            if (_ShpNm == null || _ShpNm.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_BsnNo == null || _BsnNo.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_TermID == null || _TermID.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_PreNm == null || _PreNm.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_ShpTno == null || _ShpTno.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            } else if (_ShpAr == null || _ShpAr.equals(""))
            {
                SendreturnData(RETURN_CANCEL,null,"가맹점 정보가 없습니다");
                HideDialog();
                return;
            }

            /* 로그기록. 프린트 */
            String mLog = "TrdType : " + _Command + ", " + "TermID : " + _TermId + ", " +
                    "거래일자 : " + _AuDate + ", " + "가맹점명칭 : " + _ShpNm + ", " +
                    "사업자번호 : " + _BsnNo + ", " + "대표자명 : " + _PreNm + ", " +
                    "가맹점전화번호 : " + _ShpTno + ", " + "가맹점주소 : " + _ShpAr;
            cout("RECV : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),mLog);

            HashMap<String,String> reHash = new HashMap<>();

            reHash = mPosSdk.getAppToAppTradeResult(_TermId,_AuDate,_BillNo);
            if (reHash.get("AnsCode") == null || !(reHash.get("AnsCode")).equals("0000"))
            {
                SendreturnData(RETURN_CANCEL,null,"승인 데이터가 없습니다");
                HideDialog();
                return;
            }

            // 신용승인/신용취소/현금승인/현금취소 총4개로 구분지어서 파싱한다
            //신용매출
            if ((reHash.get("TrdType")).equals("A25")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("카드취소")) + Constants.PENTER);
                printCredit(mhashMap,reHash);
            } else if((reHash.get("TrdType")).equals("A15")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("카드승인")) + Constants.PENTER);
                printCredit(mhashMap,reHash);
            }
            //간편매출
            else if ((reHash.get("TrdType")).equals("E25")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("간편취소")) + Constants.PENTER);
                printEasy(mhashMap,reHash);
            } else if ((reHash.get("TrdType")).equals("E15")){
                printParser(Utils.PrintCenter(Utils.PrintBold("간편승인")) + Constants.PENTER);
                printEasy(mhashMap,reHash);
            } else if ((reHash.get("TrdType")).equals("E35")) {
                SendreturnData(RETURN_CANCEL,null,"제로페이 취소 조회 응답데이터 입니다");
                HideDialog();
                return;
            }
            //현금매출
            else if ((reHash.get("TrdType")).equals("B25")) {
                printParser(Utils.PrintCenter(Utils.PrintBold("현금취소")) + Constants.PENTER);
                printCash(mhashMap,reHash);
            } else if ((reHash.get("TrdType")).equals("B15")){
                printParser(Utils.PrintCenter(Utils.PrintBold("현금승인")) + Constants.PENTER);
                printCash(mhashMap,reHash);
            } else
            {
                String _type = reHash.get("TrdType") == null ? "":reHash.get("TrdType");
                SendreturnData(RETURN_CANCEL,null,"거래타입을 알 수 없습니다 : " + _type);
                HideDialog();
                return;
            }

//            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
                try {
                    byte[] text = null;
                    printParser(Utils.PrintLine("  ") + Constants.PENTER);
                    printParser(Utils.PrintLine("  ") + Constants.PENTER);
                    byte[] prtStr = Command.BLEPrintParser(printMsg);
                    String string = printMsg;

                    text = prtStr;

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byteStream.write(WoosimCmd.setTextStyle(true, true, false, 1, 1));
                    byteStream.write(WoosimCmd.setTextAlign(0));
                    if (text != null) byteStream.write(text);
                    byteStream.write(WoosimCmd.printData());
                    mPosSdk.__Printer(Utils.MakePacket((byte)0xc6,byteStream.toByteArray()), new bleSdkInterface.ResDataListener() {
                        @Override
                        public void OnResult(byte[] res) {

                            if (res[3] == Command.CMD_PRINT_RES)
                            {
                                HashMap<String,String> hashMap = new HashMap<String, String>();
                                HideDialog();
                                if (res[4] == 0x30) {
                                    hashMap.put("Message","프린트를 완료하였습니다.");
                                    /* 로그기록. 버전정보 */
                                    cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
                                    SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
                                } else if (res[4] == 0x31) {
                                    hashMap.put("Message","용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                } else if (res[4] == 0x32) {
                                    hashMap.put("Message","배터리 부족으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "배터리 부족으로 프린트를 실패하였습니다");
                                } else {
                                    hashMap.put("Message","프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "프린트를 실패하였습니다");
                                }
//                                HideDialog();
//                                mPosSdk.__BLEDeviceInit(null, "99");
                                return;
                            }

                            if (res[9] == Command.CMD_PRINT_RES)
                            {
                                HashMap<String,String> hashMap = new HashMap<String, String>();
                                HideDialog();
                                if (res[10] == 0x30) {
                                    hashMap.put("Message","프린트를 완료하였습니다.");
                                    /* 로그기록. 버전정보 */
                                    cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
                                    SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
                                } else if (res[10] == 0x31) {
                                    hashMap.put("Message","용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                } else if (res[10] == 0x32) {
                                    hashMap.put("Message","배터리 부족으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "배터리 부족으로 프린트를 실패하였습니다");
                                } else {
                                    hashMap.put("Message","프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "프린트를 실패하였습니다");
                                }
//                                HideDialog();
//                                mPosSdk.__BLEDeviceInit(null, "99");
                                return;
                            }
                        }
                    });

//                    mPosSdk.mbleWoosimSdk.mPrintService.write(Utils.MakePacket((byte)0xc6,byteStream.toByteArray()));
//
//                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            HashMap<String,String> hashMap = new HashMap<String, String>();
//                            HideDialog();
//                            hashMap.put("Message","프린트를 완료하였습니다.");
//                            /* 로그기록. 버전정보 */
//                            cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
//                            SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
//                        }
//                    },8000);

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    String _totalMsg = "";
                    _totalMsg += printMsg;


                    byte[] prtStr = null;
                    prtStr = Command.BLEPrintParser(_totalMsg);
                    /* 로그기록. 프린트 */
                    String mLog = "Print : " + _totalMsg;
                    cout("TEST_BLE : Print",Utils.getDate("yyyyMMddHHmmss"),mLog);

                    mPosSdk.__Printer(prtStr, new bleSdkInterface.ResDataListener() {
                        @Override
                        public void OnResult(byte[] res) {

                            if (res[3] == Command.CMD_PRINT_RES)
                            {
                                HashMap<String,String> hashMap = new HashMap<String, String>();
                                HideDialog();
                                if (res[4] == 0x30) {
                                    hashMap.put("Message","프린트를 완료하였습니다.");
                                    /* 로그기록. 버전정보 */
                                    cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),hashMap.toString());
                                    SendreturnData(RETURN_OK, hashMap, "프린트를 완료하였습니다.");
                                } else if (res[4] == 0x31) {
                                    hashMap.put("Message","용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "용지 없음(프린터 커버 열림)으로 프린트를 실패하였습니다");
                                } else if (res[4] == 0x32) {
                                    hashMap.put("Message","배터리 부족으로 프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "배터리 부족으로 프린트를 실패하였습니다");
                                } else {
                                    hashMap.put("Message","프린트를 실패하였습니다");
                                    SendreturnData(RETURN_CANCEL, hashMap, "프린트를 실패하였습니다");
                                }

//                                mPosSdk.__BLEDeviceInit(null, "99");
                                return;
                            }

                        }
                    });
                }
            },1000);

        }



    }

    /**
     * 지난 데이터 처리
     */
    private synchronized void AppToApp_ReCommand(HashMap<String,String> _hashMap)
    {
        String _TermId = _hashMap.get("TermID");
        String _AuDate = _hashMap.get("AuDate");
        String _BillNo = _hashMap.get("BillNo");

        if (_TermId == null || _TermId.equals("")) {
            SendreturnData(RETURN_CANCEL,null,"TID 가 없습니다");
            HideDialog();
            return;
        }
        if (_AuDate == null || _AuDate.equals("")) {
            SendreturnData(RETURN_CANCEL,null,"거래일자 가 없습니다");
            HideDialog();
            return;
        }
        if (_BillNo == null || _BillNo.equals("")) {
            SendreturnData(RETURN_CANCEL,null,"해당 전표번호의 거래가 없습니다");
            HideDialog();
            return;
        }

        /* 로그기록. 거래내역재전송 */
        String mLog = "TrdType : " + _hashMap.get("TrdType") + "," +"단말기ID : " + _TermId + "," + "거래일자 : " + _AuDate + "," + "전표번호 : " + _BillNo;
        cout("RECV : RE_COMMAND",Utils.getDate("yyyyMMddHHmmss"),mLog);

        HashMap<String,String> reHash = new HashMap<>();
        reHash = mPosSdk.getAppToAppTradeResult(_TermId,_AuDate,_BillNo);
        if (reHash.size() > 2)
        {
            reHash.remove("OriAuNo");
            reHash.remove("OriAuDate");

            reHash.remove("TrdAmt");
            reHash.remove("TaxAmt");
            reHash.remove("SvcAmt");
            reHash.remove("TaxFreeAmt");
            reHash.remove("Month");

            /* 로그기록. 버전정보 */
            cout("SEND : RE_COMMAND",Utils.getDate("yyyyMMddHHmmss"),reHash.toString());
            SendreturnData(RETURN_OK, reHash, reHash.get("Message"));
            HideDialog();
            return;
        }
        else
        {
            SendreturnData(RETURN_CANCEL,null,"데이터가 없습니다");
            HideDialog();
            return;
        }
    }

    /**
     * 앱투앱으로 버전정보 전송
     */
    int AppToApp_VersionBleCount = 0; // ㄱR20으로 요청 시 BLE 의 경우 셋팅하고 다시 와서 실행하는데 이 때에도 BLE가 안 잡혀있을 경우를 뜻한다.
    private synchronized void AppToApp_VersionInfo(HashMap<String,String> _hashMap)
    {
        HashMap<String,String> reHash = new HashMap<>();
        if (_hashMap.get("TrdType").equals("R10")) {
            reHash.put("TrdType","R15");
        } else {
            reHash.put("TrdType","R25");
        }

        reHash.put("TmlcNo",Utils.getAppID());
        reHash.put("SfVer",Constants.TEST_SOREWAREVERSION);

        String deviceType = Setting.getPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        /* 로그기록. 버전정보 */
        String mLog = "TrdType : " + _hashMap.get("TrdType");
        cout("RECV : VERSION_INFO",Utils.getDate("yyyyMMddHHmmss"),mLog);

        switch (Setting.g_PayDeviceType)
        {
            case NONE:
                reHash.put("HSet","0");
                break;
            case LINES:     //유선장치의 경우
                reHash.put("HSet","1");
                if(mPosSdk.mDevicesList != null)
                {
                    int mDCount = mPosSdk.getUsbDevice().size();
                    if(mDCount<=0) //연결된 장치가 없다면
                    {

                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!Setting.g_AutoDetect) {
                                    CompareDeviceList();
                                }
                            }
                        }, 500);
                        return;
                    }
                }
                else {
                    mPosSdk.mDevicesList = new ArrayList<>();
                    Devices tempDevice;
                    String SelectedCardReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER);
                    if(!SelectedCardReader.equals(""))
                    {
                        tempDevice = new Devices(SelectedCardReader,SelectedCardReader,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL));
                        tempDevice.setmType(1);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }
                    String SelectedMultiReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER);
                    if(!SelectedMultiReader.equals(""))
                    {
                        tempDevice = new Devices(SelectedMultiReader,SelectedMultiReader,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL));
                        tempDevice.setmType(4);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }
                    String SelectedSignPad = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_DEVICE_SIGN_PAD);
                    if(!SelectedSignPad.equals(""))
                    {
                        tempDevice = new Devices(SelectedSignPad,SelectedSignPad,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL));
                        tempDevice.setmType(2);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }
                    String SelectedMultiPad = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);
                    if(!SelectedMultiPad.equals(""))
                    {
                        tempDevice = new Devices(SelectedMultiPad,SelectedMultiPad,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL));
                        tempDevice.setmType(3);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }

                    int mDCount = mPosSdk.getUsbDevice().size();
                    if(mDCount<=0) //연결된 장치가 없다면
                    {

                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!Setting.g_AutoDetect) {
                                    CompareDeviceList();
                                }
                            }
                        }, 500);
                        return;
                    }

                }

                break;
            case BLE:       //BLE의 경우
                reHash.put("HSet","2");
                mPosSdk.BleIsConnected();
                if(Setting.getBleIsConnected()) {
                    if(Setting.getPreference(instance, Constants.REGIST_DEVICE_NAME).length()> 16)
                    {
                        reHash.put("HTmlcNo", Setting.getPreference(instance, Constants.REGIST_DEVICE_NAME).substring(0,16));
                    }
                    else
                    {
                        reHash.put("HTmlcNo", Setting.getPreference(instance, Constants.REGIST_DEVICE_NAME));
                    }

                    reHash.put("HSn", Setting.getPreference(instance, Constants.REGIST_DEVICE_SN));
                    reHash.put("HSfVer", Setting.getPreference(instance, Constants.REGIST_DEVICE_VERSION));
                    reHash.put("HScrKeyYn", Setting.mBleHScrKeyYn);
                } else {
                    if (_hashMap.get("TrdType").equals("R10")) {
                        AppToApp_VersionBleCount = 0;
                    } else {
                        if(AppToApp_VersionBleCount == 0) {
                            AppToApp_VersionBleCount++;
                            SetBle();
                            return;
                        } else {
                            AppToApp_VersionBleCount = 0;
                        }
                    }


                }
                break;
            case CAT:       //WIFI CAT의 경우
                reHash.put("HSet","3");

                reHash.put("HTmlcNo", "                ");
                reHash.put("HSn", "          ");
                reHash.put("HSfVer", "     ");
                reHash.put("HScrKeyYn", "  ");
                break;
            default:
                break;
        }

        if( (Setting.getPreference(instance, Constants.APPTOAPP_TID)).equals(""))
        {
            reHash.put("TermIDSet","0");
            reHash.put("CntTermID","0");
            reHash.put("TermID","");
        }
        else
        {
            reHash.put("TermIDSet","1");
            //Todo: 일단 1개만 사용가능하다. 차후 복수 가맹점 가능하도록 한다. 22-04-25 by.jiw
            reHash.put("CntTermID","1");
            reHash.put("TermID",Setting.getPreference(instance, Constants.APPTOAPP_TID));
        }

        /* 로그기록. 버전정보 */
        cout("SEND : VERSION_INFO",Utils.getDate("yyyyMMddHHmmss"),reHash.toString());
        SendreturnData(RETURN_OK, reHash, "버전정보응답");
        HideDialog();
        return;
    }


    /**
     * 앱투앱으로 키다운로드 전송
     */
    int AppToApp_KeyDownBleCount = 0; // D20으로 요청 시 BLE 의 경우 셋팅하고 다시 와서 실행하는데 이 때에도 BLE가 안 잡혀있을 경우를 뜻한다.
    private synchronized void AppToApp_KeyDownload(HashMap<String,String> _hashMap)
    {
        AtomicReference<HashMap<String, String>> reHash = new AtomicReference<>(new HashMap<>());

        String termID = _hashMap.get("TermID") == null ? "":_hashMap.get("TermID");
        String BsnNo = _hashMap.get("BsnNo") == null ? "":_hashMap.get("BsnNo");
        String serial = _hashMap.get("Serial") == null ? "":_hashMap.get("Serial");
        String TrdType = _hashMap.get("TrdType") == null ? "":_hashMap.get("TrdType");
        String MchData = _hashMap.get("MchData") == null ? "":_hashMap.get("MchData");

        if(termID==null || BsnNo==null || serial == null)   //tid 사업자번호 시리얼번호 체크
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_NODATA);
            return;
        }

        //tid,사업자번호,시리얼번호가 10자가 넘는 경우 에러를 발생 시킨다F
        if(termID.length()!=10 || BsnNo.length()!=10 || serial.length()!=10)
        {
            SendreturnData(RETURN_CANCEL,null,ERROR_MCHSTRING_COUNT);
            return;
        }

        reHash.get().put("TrdType","D25");
        reHash.get().put("TermID",termID);

        String deviceType = Setting.getPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        /* 로그기록. 버전정보 */
        String mLog = "TrdType : " + _hashMap.get("TrdType");
        cout("RECV : KEY_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),mLog);

        switch (Setting.g_PayDeviceType)
        {
            case NONE:

                break;
            case LINES:     //유선장치의 경우

                if(mPosSdk.mDevicesList != null)
                {
                    int mDCount = mPosSdk.getUsbDevice().size();
                    if(mDCount<=0) //연결된 장치가 없다면
                    {

                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!Setting.g_AutoDetect) {
                                    CompareDeviceList();
                                }
                            }
                        }, 500);
                        return;
                    }
                }
                else {
                    mPosSdk.mDevicesList = new ArrayList<>();
                    Devices tempDevice;
                    String SelectedCardReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER);
                    if(!SelectedCardReader.equals(""))
                    {
                        tempDevice = new Devices(SelectedCardReader,SelectedCardReader,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL));
                        tempDevice.setmType(1);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }
                    String SelectedMultiReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER);
                    if(!SelectedMultiReader.equals(""))
                    {
                        tempDevice = new Devices(SelectedMultiReader,SelectedMultiReader,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL));
                        tempDevice.setmType(4);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }
                    String SelectedSignPad = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_DEVICE_SIGN_PAD);
                    if(!SelectedSignPad.equals(""))
                    {
                        tempDevice = new Devices(SelectedSignPad,SelectedSignPad,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL));
                        tempDevice.setmType(2);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }
                    String SelectedMultiPad = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);
                    if(!SelectedMultiPad.equals(""))
                    {
                        tempDevice = new Devices(SelectedMultiPad,SelectedMultiPad,false);
                        tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL));
                        tempDevice.setmType(3);
                        mPosSdk.mDevicesList.add(tempDevice);
                    }

                    int mDCount = mPosSdk.getUsbDevice().size();
                    if(mDCount<=0) //연결된 장치가 없다면
                    {

                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!Setting.g_AutoDetect) {
                                    CompareDeviceList();
                                }
                            }
                        }, 500);
                        return;
                    }

                }

                break;
            case BLE:       //BLE의 경우
                mPosSdk.BleIsConnected();
                if(Setting.getBleIsConnected()) {
                    Toast.makeText(instance, "키 다운로드 중입니다", Toast.LENGTH_SHORT).show();
                    DeviceSecuritySDK securitySDK = new DeviceSecuritySDK(mPosSdk.getActivity(),(result,Code,state,resultData) ->{
                        new Handler(Looper.getMainLooper()).post(()->{
                            reHash.set(resultData);
                            /* 로그기록. 버전정보 */
                            cout("SEND : KEY_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),reHash.toString());
                            SendreturnData(RETURN_OK, reHash.get(), "키다운로드응답");
                            HideDialog();
//                            ReadyDialogHide();
                        });
                    });

                    securitySDK.Req_BLESecurityKeyUpdate(termID,BsnNo,serial);    //무결성검증을 완료한다
                } else {
                    if(AppToApp_KeyDownBleCount == 0) {
                        AppToApp_KeyDownBleCount++;
                        SetBle();
                        return;
                    } else {
                        AppToApp_KeyDownBleCount = 0;
                        SendreturnData(RETURN_CANCEL,null,"키다운로드 실패");
                        /* 로그기록. 버전정보 */
                        cout("SEND : KEY_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),reHash.toString());
                        HideDialog();
                    }
                }
                break;
            case CAT:       //WIFI CAT의 경우
                SendreturnData(RETURN_CANCEL,null,"CAT은 지원하지 않습니다");
                /* 로그기록. 버전정보 */
                cout("SEND : KEY_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),reHash.toString());
                HideDialog();
                break;
            default:
                break;
        }


        return;
    }

    /**
     * 전표출력(거래내역을 통해서 출력방식을 코세스식으로 처리
     * @param _hashMap
     */
    String tradeType = "";   //거래구분자 캣신용, 일반신용 등을 구분
    String CancelInfo = "";  //취소인지/승인인지 구분자
    String oriAudate = "";   //원거래일자
    String mchNo = "";       //가맹점번호
    String printMsg = "";    //영수증에서 프린트 시 출력할 내용
    String mtestTID = ""; //취소시 사용하는 DB 에 저장된 TID
    private synchronized void AppToApp_Print_Parsing(HashMap<String,String> _hashMap) throws UnsupportedEncodingException {


        String _TermId = _hashMap.get("TermID");
        String _AuDate = _hashMap.get("AuDate");
        String _BillNo = _hashMap.get("BillNo");

        if (_TermId == null || _TermId.equals("")) {
            SendreturnData(RETURN_CANCEL,null,"TID 가 없습니다");
            HideDialog();
            return;
        }
        if (_AuDate == null || _AuDate.equals("")) {
            SendreturnData(RETURN_CANCEL,null,"거래일자 가 없습니다");
            HideDialog();
            return;
        }
        if (_BillNo == null || _BillNo.equals("")) {
            SendreturnData(RETURN_CANCEL,null,"해당 전표번호의 거래가 없습니다");
            HideDialog();
            return;
        }




    }

    private void printCash(HashMap<String,String> _hashMap, HashMap<String,String> reHash) throws UnsupportedEncodingException {

        String _TermId = _hashMap.get("TermID");
        String _AuDate = _hashMap.get("AuDate");
        String _BillNo = _hashMap.get("BillNo");

        //전표번호(로컬DB에 저장되어 있는 거래내역리스트의 번호) + 전표출력일시
        printParser(Utils.PrintPad("No." + StringUtil.leftPad(_BillNo, "0", 6),parsingDate(Utils.getDate("yyMMdd"))) + Constants.PENTER);
        //만약 취소 시에는 여기에서 원거래일자를 삽입해야 한다. 결국 sqlLITE 에 원거래일자 항목을 하나 만들어서 취소시에는 원거래일자에 승인일자를 삽입해야 한다.
        if ((reHash.get("TrdType")).equals("B25")) {
            printParser(Utils.PrintPad("원거래일", parsingDate(reHash.get("OriAuDate"))) + Constants.PENTER);
        }
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

        //가맹점명
        printParser(Utils.PrintPad("가맹점명", _hashMap.get("ShpNm")) + Constants.PENTER);
        //사업자번호
        printParser(Utils.PrintPad("사업자번호", parsingbsn(_hashMap.get("BsnNo"))) + Constants.PENTER);
        //단말기TID
        printParser(Utils.PrintPad("단말기ID", _hashMap.get("TermID")) + Constants.PENTER);
        //대표자명
        printParser(Utils.PrintPad("대표자명", _hashMap.get("PreNm")) + Constants.PENTER);
        //연락처
        printParser(Utils.PrintPad("연락처", phoneParser(_hashMap.get("ShpTno"))) + Constants.PENTER);
        //주소
        printParser(Utils.PrintPad("주소  ", _hashMap.get("ShpAr")) + Constants.PENTER);
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

        //승인일시
        printParser(Utils.PrintPad("승인일시", parsingDate(reHash.get("TrdDate")))  + Constants.PENTER);

        //카드번호
        printParser(Utils.PrintPad( "고객번호",  reHash.get("CardNo")) + Constants.PENTER);
        //승인번호
        printParser(Utils.PrintPad("승인번호", reHash.get("AuNo").replace(" ","")) + Constants.PENTER);

        //-------------
        printParser(Utils.PrintLine( "- ") + Constants.PENTER);

        //공급가액
        //TODO:프린트시 금액계산은 자체거래 프린트시 금액계산을 따라하면 안된다. 세금계산 없이 진행되었기 때문이다
        int correctMoney = 0;
        int _totalMoney = 0;

        correctMoney = Integer.parseInt(reHash.get("TrdAmt"));
        _totalMoney = Integer.parseInt(reHash.get("TrdAmt")) + Integer.parseInt(reHash.get("TaxAmt")) +
                Integer.parseInt(reHash.get("SvcAmt")) + Integer.parseInt(reHash.get("TaxFreeAmt"));

        if((reHash.get("TrdType")).equals("B25")){     //거래 취소의 경우에 앞에 - 붙인다.
            //공급가액
            printParser(Utils.PrintPad("공급가액", "-" + Utils.PrintMoney( String.valueOf(correctMoney)) + "원" ) + Constants.PENTER);
            //부가세
            printParser(Utils.PrintPad("부가세", "-" + Utils.PrintMoney(reHash.get("TaxAmt")) + "원") + Constants.PENTER);
            //봉사료
            printParser(Utils.PrintPad("봉사료", "-" + Utils.PrintMoney(reHash.get("SvcAmt")) + "원") + Constants.PENTER);
            //비과세
            printParser(Utils.PrintPad("비과세", "-" + Utils.PrintMoney(reHash.get("TaxFreeAmt")) + "원") + Constants.PENTER);

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , "-" + Utils.PrintBold(Utils.PrintMoney(String.valueOf(_totalMoney)) + "원")) + Constants.PENTER);
        }
        else
        {
            //공급가액
            printParser(Utils.PrintPad("공급가액", Utils.PrintMoney( String.valueOf(correctMoney)) + "원" ) + Constants.PENTER);
            //부가세
            printParser(Utils.PrintPad("부가세", Utils.PrintMoney(reHash.get("TaxAmt")) + "원") + Constants.PENTER);
            //봉사료
            printParser(Utils.PrintPad("봉사료", Utils.PrintMoney(reHash.get("SvcAmt")) + "원") + Constants.PENTER);
            //비과세
            printParser(Utils.PrintPad("비과세", Utils.PrintMoney(reHash.get("TaxFreeAmt")) + "원") + Constants.PENTER);

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , Utils.PrintBold(Utils.PrintMoney(String.valueOf(_totalMoney)) + "원")) + Constants.PENTER);
        }

        //기프트카드 잔액
        if (reHash.get("CardKind").contains("3") || reHash.get("CardKind").contains("4")) {
//            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(mData.giftAmt.replace(("[^\\d.]").toRegex(), "")) + "원") + Constants.PENTER)
            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(reHash.get("GiftAmt")) + "원") + Constants.PENTER);
        }
        //응답메시지
        printParser(reHash.get("Message") + Constants.PENTER);
        printParser(Utils.PrintLine("- ") + Constants.PENTER);
    }

    private void printCredit(HashMap<String,String> _hashMap, HashMap<String,String> reHash) throws UnsupportedEncodingException {

        String _TermId = _hashMap.get("TermID");
        String _AuDate = _hashMap.get("AuDate");
        String _BillNo = _hashMap.get("BillNo");

        //전표번호(로컬DB에 저장되어 있는 거래내역리스트의 번호) + 전표출력일시
        printParser(Utils.PrintPad("No." + StringUtil.leftPad(_BillNo, "0", 6),parsingDate(Utils.getDate("yyMMdd"))) + Constants.PENTER);
        //만약 취소 시에는 여기에서 원거래일자를 삽입해야 한다. 결국 sqlLITE 에 원거래일자 항목을 하나 만들어서 취소시에는 원거래일자에 승인일자를 삽입해야 한다.
        if ((reHash.get("TrdType")).equals("A25")) {
            printParser(Utils.PrintPad("원거래일", parsingDate(reHash.get("OriAuDate"))) + Constants.PENTER);
        }
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

        //가맹점명
        printParser(Utils.PrintPad("가맹점명", _hashMap.get("ShpNm")) + Constants.PENTER);
        //사업자번호
        printParser(Utils.PrintPad("사업자번호", parsingbsn(_hashMap.get("BsnNo"))) + Constants.PENTER);
        //단말기TID
        printParser(Utils.PrintPad("단말기ID", _hashMap.get("TermID")) + Constants.PENTER);
        //대표자명
        printParser(Utils.PrintPad("대표자명", _hashMap.get("PreNm")) + Constants.PENTER);
        //연락처
        printParser(Utils.PrintPad("연락처", phoneParser(_hashMap.get("ShpTno"))) + Constants.PENTER);
        //주소
        printParser(Utils.PrintPad("주소  ", _hashMap.get("ShpAr")) + Constants.PENTER);
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

        //승인일시
        printParser(Utils.PrintPad("승인일시", parsingDate(reHash.get("TrdDate") == null ? "":reHash.get("TrdDate")))  + Constants.PENTER);
        //할부개월
        if(reHash.get("Month") == null || reHash.get("Month").equals(""))
        {
            printParser(Utils.PrintPad("할부개월", "일시불") + Constants.PENTER);

        }
        else
        {
            printParser(Utils.PrintPad("할부개월", reHash.get("Month") + " 개월") + Constants.PENTER);

        }

        //카드번호
        printParser(Utils.PrintPad( "고객번호",  reHash.get("CardNo") == null ? "":reHash.get("CardNo")) + Constants.PENTER);
        //승인번호
        printParser(Utils.PrintPad("승인번호", reHash.get("AuNo") == null ? "":reHash.get("AuNo").replace(" ", "")) + Constants.PENTER);

        //가맹점번호
        printParser(Utils.PrintPad("가맹점번호", reHash.get("MchNo") == null ? "":reHash.get("MchNo").replace(" ", "")) + Constants.PENTER);
        //매입사명
        printParser(Utils.PrintPad("매입사명", reHash.get("InpNm") == null ? "":reHash.get("InpNm").replace(" ", "")) + Constants.PENTER);
        //카드종류
        printParser(Utils.PrintPad("발급사명", reHash.get("OrdNm") == null ? "":reHash.get("OrdNm").replace(" ", "")) + Constants.PENTER);

        //-------------
        printParser(Utils.PrintLine( "- ") + Constants.PENTER);

        //공급가액
        int correctMoney = 0;
        int _totalMoney = 0;

        correctMoney = Integer.parseInt(reHash.get("TrdAmt"));
        _totalMoney = Integer.parseInt(reHash.get("TrdAmt")) + Integer.parseInt(reHash.get("TaxAmt")) +
                Integer.parseInt(reHash.get("SvcAmt")) + Integer.parseInt(reHash.get("TaxFreeAmt"));



        if((reHash.get("TrdType")).equals("A25")){     //거래 취소의 경우에 앞에 - 붙인다.
            //공급가액
            printParser(Utils.PrintPad("공급가액", "-" + Utils.PrintMoney( String.valueOf(correctMoney)) + "원" ) + Constants.PENTER);
            //부가세
            printParser(Utils.PrintPad("부가세", "-" + Utils.PrintMoney(reHash.get("TaxAmt")) + "원") + Constants.PENTER);
            //봉사료
            printParser(Utils.PrintPad("봉사료", "-" + Utils.PrintMoney(reHash.get("SvcAmt")) + "원") + Constants.PENTER);
            //비과세
            printParser(Utils.PrintPad("비과세", "-" + Utils.PrintMoney(reHash.get("TaxFreeAmt")) + "원") + Constants.PENTER);

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , "-" + Utils.PrintBold(Utils.PrintMoney(String.valueOf(_totalMoney)) + "원")) + Constants.PENTER);
        }
        else
        {
            //공급가액
            printParser(Utils.PrintPad("공급가액", Utils.PrintMoney( String.valueOf(correctMoney)) + "원" ) + Constants.PENTER);
            //부가세
            printParser(Utils.PrintPad("부가세", Utils.PrintMoney(reHash.get("TaxAmt")) + "원") + Constants.PENTER);
            //봉사료
            printParser(Utils.PrintPad("봉사료", Utils.PrintMoney(reHash.get("SvcAmt")) + "원") + Constants.PENTER);
            //비과세
            printParser(Utils.PrintPad("비과세", Utils.PrintMoney(reHash.get("TaxFreeAmt")) + "원") + Constants.PENTER);

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , Utils.PrintBold(Utils.PrintMoney(String.valueOf(_totalMoney)) + "원")) + Constants.PENTER);
        }

        //기프트카드 잔액
        if (reHash.get("CardKind").contains("3") || reHash.get("CardKind").contains("4")) {
//            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(mData.giftAmt.replace(("[^\\d.]").toRegex(), "")) + "원") + Constants.PENTER)
            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(reHash.get("GiftAmt")) + "원") + Constants.PENTER);
        }
        //응답메시지
        printParser(reHash.get("Message") + Constants.PENTER);
        printParser(Utils.PrintLine("- ") + Constants.PENTER);
    }

    private void printEasy(HashMap<String,String> _hashMap, HashMap<String,String> reHash) throws UnsupportedEncodingException {

        String _TermId = _hashMap.get("TermID");
        String _AuDate = _hashMap.get("AuDate");
        String _BillNo = _hashMap.get("BillNo");

        //전표번호(로컬DB에 저장되어 있는 거래내역리스트의 번호) + 전표출력일시
        printParser(Utils.PrintPad("No." + StringUtil.leftPad(_BillNo, "0", 6),parsingDate(Utils.getDate("yyMMdd"))) + Constants.PENTER);
        //만약 취소 시에는 여기에서 원거래일자를 삽입해야 한다. 결국 sqlLITE 에 원거래일자 항목을 하나 만들어서 취소시에는 원거래일자에 승인일자를 삽입해야 한다.
        if ((reHash.get("TrdType")).equals("E25")) {
            printParser(Utils.PrintPad("원거래일", parsingDate(reHash.get("OriAuDate"))) + Constants.PENTER);
        }
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

        //가맹점명
        printParser(Utils.PrintPad("가맹점명", _hashMap.get("ShpNm")) + Constants.PENTER);
        //사업자번호
        printParser(Utils.PrintPad("사업자번호", parsingbsn(_hashMap.get("BsnNo"))) + Constants.PENTER);
        //단말기TID
        printParser(Utils.PrintPad("단말기ID", _hashMap.get("TermID")) + Constants.PENTER);
        //대표자명
        printParser(Utils.PrintPad("대표자명", _hashMap.get("PreNm")) + Constants.PENTER);
        //연락처
        printParser(Utils.PrintPad("연락처", phoneParser(_hashMap.get("ShpTno"))) + Constants.PENTER);
        //주소
        printParser(Utils.PrintPad("주소  ", _hashMap.get("ShpAr")) + Constants.PENTER);
        //-------------
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

        //승인일시
        printParser(Utils.PrintPad("승인일시", parsingDate(reHash.get("TrdDate")))  + Constants.PENTER);
        //할부개월
        printParser(Utils.PrintPad("할부개월", reHash.get("Month").equals("") ? "일시불":reHash.get("Month") + " 개월") + Constants.PENTER);

        //카드번호
        printParser(Utils.PrintPad( "고객번호",  reHash.get("CardNo")) + Constants.PENTER);
        //승인번호
        printParser(Utils.PrintPad("승인번호", reHash.get("AuNo").replace(" ","")) + Constants.PENTER);

        //가맹점번호
        printParser(Utils.PrintPad("가맹점번호", reHash.get("MchNo").replace(" ","")) + Constants.PENTER);
        //매입사명
        printParser(Utils.PrintPad("매입사명", reHash.get("InpNm").replace(" ", "")) + Constants.PENTER);
        //카드종류
        printParser(Utils.PrintPad("발급사명", reHash.get("OrdNm").replace(" ", "")) + Constants.PENTER);

        //-------------
        printParser(Utils.PrintLine( "- ") + Constants.PENTER);

        //공급가액
        int correctMoney = 0;
        int _totalMoney = 0;

        correctMoney = Integer.parseInt(reHash.get("TrdAmt"));
        _totalMoney = Integer.parseInt(reHash.get("TrdAmt")) + Integer.parseInt(reHash.get("TaxAmt")) +
                Integer.parseInt(reHash.get("SvcAmt")) + Integer.parseInt(reHash.get("TaxFreeAmt"));

        if((reHash.get("TrdType")).equals("E25")){     //거래 취소의 경우에 앞에 - 붙인다.
            //공급가액
            printParser(Utils.PrintPad("공급가액", "-" + Utils.PrintMoney( String.valueOf(correctMoney)) + "원" ) + Constants.PENTER);
            //부가세
            printParser(Utils.PrintPad("부가세", "-" + Utils.PrintMoney(reHash.get("TaxAmt")) + "원") + Constants.PENTER);
            //봉사료
            printParser(Utils.PrintPad("봉사료", "-" + Utils.PrintMoney(reHash.get("SvcAmt")) + "원") + Constants.PENTER);
            //비과세
            printParser(Utils.PrintPad("비과세", "-" + Utils.PrintMoney(reHash.get("TaxFreeAmt")) + "원") + Constants.PENTER);

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , "-" + Utils.PrintBold(Utils.PrintMoney(String.valueOf(_totalMoney)) + "원")) + Constants.PENTER);
        }
        else
        {
            //공급가액
            printParser(Utils.PrintPad("공급가액", Utils.PrintMoney( String.valueOf(correctMoney)) + "원" ) + Constants.PENTER);
            //부가세
            printParser(Utils.PrintPad("부가세", Utils.PrintMoney(reHash.get("TaxAmt")) + "원") + Constants.PENTER);
            //봉사료
            printParser(Utils.PrintPad("봉사료", Utils.PrintMoney(reHash.get("SvcAmt")) + "원") + Constants.PENTER);
            //비과세
            printParser(Utils.PrintPad("비과세", Utils.PrintMoney(reHash.get("TaxFreeAmt")) + "원") + Constants.PENTER);

            //결제금액
            printParser(Utils.PrintPad(Utils.PrintBold("결제금액") , Utils.PrintBold(Utils.PrintMoney(String.valueOf(_totalMoney)) + "원")) + Constants.PENTER);
        }

        //기프트카드 잔액
        if (reHash.get("CardKind").contains("3") || reHash.get("CardKind").contains("4")) {
//            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(mData.giftAmt.replace(("[^\\d.]").toRegex(), "")) + "원") + Constants.PENTER)
            printParser(Utils.PrintPad("기프트카드잔액", Utils.PrintMoney(reHash.get("GiftAmt")) + "원") + Constants.PENTER);
        }

        //카카오승인금액 카카오할인금액
        if (reHash.get("DisAmt") != null && !(reHash.get("DisAmt")).equals("")) {
//            printParser(Utils.PrintPad("카카오승인금액", Utils.PrintMoney(mData.kakaoAuMoney) + "원") + Constants.PENTER)
            printParser(Utils.PrintPad("카카오할인금액", Utils.PrintMoney(reHash.get("DisAmt")) + "원") + Constants.PENTER);
        }
        //제로페이 가맹점수수료 가맹점환불금액
        if (reHash.get("ChargeAmt") != null && !(reHash.get("ChargeAmt")).equals("")) {
            printParser(Utils.PrintPad("가맹점수수료", Utils.PrintMoney(reHash.get("ChargeAmt")) + "원") + Constants.PENTER);
        }
        if (reHash.get("RefundAmt") != null && !(reHash.get("RefundAmt")).equals("")) {
            printParser(Utils.PrintPad("가맹점환불금액", Utils.PrintMoney(reHash.get("RefundAmt")) + "원") + Constants.PENTER);
        }

        //응답메시지
        printParser(reHash.get("Message") + Constants.PENTER);
        printParser(Utils.PrintLine("- ") + Constants.PENTER);

    }

    private String parsingDate(String date){
        String _Date = "";
        _Date = date;
        if(date.length()>12){
            _Date = date.substring(0,12);
        }

        if (_Date.length()<10)
        {
            String _Date2 = _Date.substring(0,2);
            _Date2 += "/";
            _Date2 += _Date.substring(2,4);
            _Date2 += "/";
            _Date2 += _Date.substring(4,6);


            return _Date2;
        }
        String _Date2 = _Date.substring(0,2);
        _Date2 += "/";
        _Date2 += _Date.substring(2,4);
        _Date2 += "/";
        _Date2 += _Date.substring(4,6);
        _Date2 += " ";
        _Date2 += _Date.substring(6,8);
        _Date2 += ":";
        _Date2 += _Date.substring(8,10);
        _Date2 += ":";
        _Date2 += _Date.substring(10,12);

        return _Date2;
    }

    /** 여기까지 프린트 관련 파싱 */
    private String  parsingbsn(String bsn){
        if (bsn.equals(""))
        {
            return "";
        }
        String _bsn = bsn.replace(" ","");

        if(_bsn.length()>12){
            String _bsn2 = _bsn.substring(0,3);
            _bsn2 += "-";
            _bsn2 += _bsn.substring(3,5);
            _bsn2 += "-";
            _bsn2 += _bsn.substring(5,10);

            return _bsn2;
        }
        else
        {
            return _bsn;
        }

    }

    //전화번호 중간에 - 를 넣는다
    private String  phoneParser(String tel) {
        if (tel.equals(""))
        {
            return "";
        }
        String _telchars = tel.replace(" ","");
        KByteArray telchars = new KByteArray();
        telchars.Add(_telchars.getBytes(StandardCharsets.UTF_8));
        String _tel = "";

        KByteArray ba2 = new KByteArray();

        if (_telchars.length() == 9) {
            _tel = _telchars.substring(0,2);
            _tel += "-";
            _tel += _telchars.substring(2,5);
            _tel += "-";
            _tel += _telchars.substring(5,9);

            return _tel;
//            ba2 = telchars.sliceArray(0..1)
//            ba2 += 0x2D.toByte()
//            ba2 += telchars.sliceArray(2..4)
//            ba2 += 0x2D.toByte()
//            ba2 += telchars.sliceArray(5..8)
//            return ba2.toString(Charset.defaultCharset())
        } else if (_telchars.length() == 10) {
            _tel = _telchars.substring(0,2);
            _tel += "-";
            _tel += _telchars.substring(2,6);
            _tel += "-";
            _tel += _telchars.substring(6,10);
            return _tel;
//            ba2 = telchars.sliceArray(0..1)
//            ba2 += 0x2D.toByte()
//            ba2 += telchars.sliceArray(2..5)
//            ba2 += 0x2D.toByte()
//            ba2 += telchars.sliceArray(6..9)
//            return ba2.toString(Charset.defaultCharset())
        } else if (_telchars.length() == 11) {
            _tel = _telchars.substring(0,3);
            _tel += "-";
            _tel += _telchars.substring(3,7);
            _tel += "-";
            _tel += _telchars.substring(7,11);

            return _tel;
//            ba2 = telchars.sliceArray(0..2)
//            ba2 += 0x2D.toByte()
//            ba2 += telchars.sliceArray(3..6)
//            ba2 += 0x2D.toByte()
//            ba2 += telchars.sliceArray(7..10)
//            return ba2.toString(Charset.defaultCharset())
        } else {
            _tel = tel.replace( " ",  "");
        }
        return _tel;
    }

    //개개 메세지 한줄씩 파싱
    private void printParser(String _msg) {
        printMsg += _msg;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        return;
    }


    /** 무결성 검증및 장비셋팅에서 시리얼 주소 체크를 위해 사용하는 변수*/
    private String[] busAddr;
    /** 장치가 2개인 경우 각각의 장치를 초기화 및 셋팅을 위한 변수 */
    int Count = 0;
    /** busAddr 로 받은 주소들을 개별로 초기화 하기 위한 변수 */
    private static String TmepAddr = "";
    AppToAppDetectTimer mMainTimer;
    /** 저장되어 있던 장치일 경우를 체크하여 카드리더기 식별번호를 출력할지 말지를 정하는 변수 */
    boolean b_detectSet = false;
    HashMap<String,String> _reHash = new HashMap<>();
    /**
     * 현재 연결 되어 있는 장비와 저장 되어 있는 장비 리스트를 비교하여 시리얼은 같으나 주소가 다른 경우에 자동으로 디바이스를 주소를 수정 하는 코드를 작성 한다.
     */
    private void CompareDeviceList()
    {
        _reHash = null;
        _reHash = new HashMap<>();
//        ReadyDialog(Command.MSG_START_RESCAN_DEVICE, 0,false);
//        int mDCount = mKocesPosSdk.ConnectedUsbSerialCount();;
        int mDCount = mPosSdk.getUsbDevice().size();
        if(mDCount>0)
        {
            //장치 확인
//            busAddr = mKocesPosSdk.ConnectedUsbSerialDevicesAddr();
            busAddr = mPosSdk.getSerialDevicesBusList();
//            ReadyDialogShow(this, "연결 확인 중입니다",0);
            Count=0;
            SendDetectData2();
        }
        else //연결된 장치가 없다면
        {
            HideDialog();
            Toast.makeText(instance,Command.MSG_NO_SCAN_DEVICE ,Toast.LENGTH_SHORT).show();
            SendUSBDeviceInfo();
        }
    }

    /** 장치가 정상적으로 연결되어 통신할 수 있는 지를 체크하고 getDeviceInfo()를 통해 장치를 셋팅한다 */
    private void SendDetectData2()   //재귀 호촐 코드 포함
    {
//        busAddr = null;
//        busAddr = mKocesPosSdk.ConnectedUsbSerialDevicesAddr();
        TmepAddr = busAddr[Count];
//        mKocesPosSdk.CheckConnectState(TmepAddr);
        mPosSdk.__PosInitAutoDetect("99",null,new String[]{TmepAddr});
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getDeviceInfo();
            }
        },500);
    }

    /** 기존의 장치면 데이터를 셋팅하고 이 후 무결성 검증으로 이동한다 */
    private void getDeviceInfo()
    {
        if(mMainTimer!= null)
        {
            mMainTimer.cancel();
            mMainTimer = null;
        }
        mMainTimer = new AppToAppDetectTimer(5000,1000);
        mMainTimer.cancel();
        mMainTimer.start();
        mPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
        {
            @Override
            public void onReceived(byte[] _rev, int _type)
            {
                if (_type == Command.PROTOCOL_TIMEOUT) {
                    ReadyDialog("장치 연결에 실패 하였습니다.", 0,false);
                }
                else if (_rev[3] ==Command.NAK) {
                    //장비에서 NAK 올라 옮
                    ReadyDialog("장치로부터 NAK가 수신 되었습니다.", 0,false);
                }
                else if(_rev[3] ==Command.ACK)
                {}
                else
                {
                    if(_rev.length>=53) {
                        KByteArray KByteArray = new KByteArray(_rev);
                        KByteArray.CutToSize(4);
                        String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                        String serialNum = new String(KByteArray.CutToSize(10));
                        String version = new String(KByteArray.CutToSize(5));
                        String key = new String(KByteArray.CutToSize(2));
                        Setting.mLineHScrKeyYn = key;

                        for (Devices n : mPosSdk.mDevicesList) {
                            if (n.getDeviceSerial().equals(serialNum)) {
//                                if (n.getmAddr().equals(TmepAddr)) {
                                switch (n.getmType()) {
                                    case 4: //multi reader
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setName(authNum);
                                        n.setVersion(version);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                        b_detectSet = true;
                                        Setting.mAuthNum = authNum.trim();
//                                        mtv_icreader.setText(Setting.mAuthNum);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                                        mPosSdk.setMultiReaderAddr(TmepAddr);
                                    case 0: //multi reader
                                        break;
                                    case 1: //IC reader
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setName(authNum);
                                        n.setVersion(version);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                        b_detectSet = true;
                                        Setting.mAuthNum = authNum.trim();
//                                        mtv_icreader.setText(Setting.mAuthNum);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                                        mPosSdk.setICReaderAddr(TmepAddr);
                                        break;
                                    case 2: //sign pad
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                        //        b_detectSet = true;
                                        mPosSdk.setSignPadAddr(TmepAddr);
                                        break;
                                    case 3: //multi pad
                                        Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                        //    b_detectSet = true;
                                        mPosSdk.setMultiPadAddr(TmepAddr);
                                        break;
                                    default:
                                        break;
                                }
//                                }
                            }
                        }
                    }
                }
                //이 코드는 필요 없어 보임
                Count++;
                if (mPosSdk.getSerialDevicesBusList().length == Count)
                {
//                    ReadyDialog("장치 스캔 완료", 0,false);
                    //카드 리더기 식별 번호 가져오기
                    ReadICReaderUniqueID();
//                    ClassfiCheck(); //장치 스캔이 끝난 후에 무결성 검사를 진행 한다.
                }
                else {
                    SendDetectData2();
                }
            }
        }, TmepAddr);
    }

    /** 무결성 검증 시 카드리더기 식별번호를 출력하기 위해 다시 한번 장치정보를 호출한다 */
    private void ReadICReaderUniqueID()
    {

        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
        if(mMainTimer!= null)
        {
            mMainTimer.cancel();
            mMainTimer = null;
        }
        HideDialog();

        /** */
        if(CheckDeviceState(Command.TYPEDEFINE_ICCARDREADER))
        {
            mPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                @Override
                public void onReceived(byte[] _rev, int _type) {
                    if(_type==Command.PROTOCOL_TIMEOUT)
                    {
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        Setting.g_bMainIntegrity = true;
                        SendUSBDeviceInfo();
                        return;
                    }
                    if(_rev[3]==(byte)0x15){
                        //장비에서 NAK 올라 옮
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        SendUSBDeviceInfo();
                        return;
                    }
                    if(Setting.ICResponseDeviceType == 3)
                    {
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        SendUSBDeviceInfo();
                        return;
                    }
                    KByteArray KByteArray = new KByteArray(_rev);
                    KByteArray.CutToSize(4);
                    String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                    String serialNum = new String(KByteArray.CutToSize(10));
                    String version = new String(KByteArray.CutToSize(5));
                    String key = new String(KByteArray.CutToSize(2));
                    Setting.mLineHScrKeyYn = key;

                    Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                    //공백을 제거하여 추가 한다.
                    String tmp = authNum.trim();
                    Setting.mAuthNum = authNum.trim();
                    if(Setting.mAuthNum != null && Setting.mAuthNum.length()>16)
                    {
                        _reHash.put("HTmlcNo",Setting.mAuthNum.substring(0,16));
                    } else {
                        _reHash.put("HTmlcNo",Setting.mAuthNum);
                    }
                    _reHash.put("HSn",serialNum);
                    _reHash.put("HSfVer",version);
                    _reHash.put("HScrKeyYn",Setting.mLineHScrKeyYn);
//                    mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                    //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
                    SendUSBDeviceInfo();
                }
            },mPosSdk.getICReaderAddr2());
        }
        else if(CheckDeviceState(Command.TYPEDEFINE_ICMULTIREADER))
        {
            mPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                @Override
                public void onReceived(byte[] _rev, int _type) {
                    if(_type==Command.PROTOCOL_TIMEOUT)
                    {

                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        Setting.g_bMainIntegrity = true;
                        /** */
                        SendUSBDeviceInfo();
                        return;
                    }
                    if(_rev[3]==(byte)0x15){
                        //장비에서 NAK 올라 옮
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        SendUSBDeviceInfo();
                        return;
                    }
                    KByteArray KByteArray = new KByteArray(_rev);
                    KByteArray.CutToSize(4);
                    String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                    String serialNum = new String(KByteArray.CutToSize(10));
                    String version = new String(KByteArray.CutToSize(5));
                    String key = new String(KByteArray.CutToSize(2));
                    Setting.mLineHScrKeyYn = key;

                    Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                    //공백을 제거하여 추가 한다.
                    String tmp = authNum.trim();
                    Setting.mAuthNum = authNum.trim();
//                    mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                    //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
                    if(Setting.mAuthNum != null && Setting.mAuthNum.length()>16)
                    {
                        _reHash.put("HTmlcNo",Setting.mAuthNum.substring(0,16));
                    } else {
                        _reHash.put("HTmlcNo",Setting.mAuthNum);
                    }
                    _reHash.put("HSn",serialNum);
                    _reHash.put("HSfVer",version);
                    _reHash.put("HScrKeyYn",Setting.mLineHScrKeyYn);
                    SendUSBDeviceInfo();
                }
            },mPosSdk.getMultiReaderAddr2());
        }
        else
        {
            /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
            if(mMainTimer!= null)
            {
                mMainTimer.cancel();
                mMainTimer = null;
            }
            HideDialog();
            /** */
            //장비가 연결 되어 있지 않아 무결성 검사도 카드리더기 식별 번호도 확인 할 수 없는 경우에 환경 설정을 넘기기 위해서 강제 세팅

            b_detectSet = false;
            SendUSBDeviceInfo();
        }
    }

    /** 연결된 장치가 카드리더기 또는 멀티카드리더기인지를 체크한다 */
    private boolean CheckDeviceState(int _DeviceType)
    {
        boolean tmp = false;
        if(_DeviceType == Command.TYPEDEFINE_ICCARDREADER && mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getICReaderAddr().equals("") &&
                mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getICReaderAddr()))
        {
            return true;
        }
        else if(_DeviceType == Command.TYPEDEFINE_ICMULTIREADER && mPosSdk.getUsbDevice().size()>0 && !mPosSdk.getMultiReaderAddr().equals("") &&
                mPosSdk.CheckConnectedUsbSerialState(mPosSdk.getMultiReaderAddr()))
        {
            return true;
        }
        return tmp;
    }

    private void SendUSBDeviceInfo()
    {
        AtomicReference<HashMap<String, String>> reHash = new AtomicReference<>(new HashMap<>());
        if (mhashMap.get("TrdType").equals("R10")) {
            _reHash.put("TrdType","R15");
        } else {
            String termID = mhashMap.get("TermID") == null ? "":mhashMap.get("TermID");
            String BsnNo = mhashMap.get("BsnNo") == null ? "":mhashMap.get("BsnNo");
            String serial = mhashMap.get("Serial") == null ? "":mhashMap.get("Serial");
//            String serial = _reHash.get("HSn") == null ? "":_reHash.get("HSn");
            Toast.makeText(instance, "키 다운로드 중입니다", Toast.LENGTH_SHORT).show();
            int tmpreader = 0;
            String tmp = Setting.getPreference(this,Constants.SELECTED_DEVICE_CARD_READER);
            if(tmp.equals(""))
            {
                tmpreader =  Command.TYPEDEFINE_ICMULTIREADER;
            }
            else
            {
                tmpreader =  Command.TYPEDEFINE_ICCARDREADER;
            }
            DeviceSecuritySDK securitySDK = new DeviceSecuritySDK(mPosSdk.getActivity(),tmpreader,"",(result,Code,state,resultData) ->{
                new Handler(Looper.getMainLooper()).post(()->{
                    reHash.set(resultData);
                    /* 로그기록. 버전정보 */
                    cout("SEND : KEY_DOWNLOAD",Utils.getDate("yyyyMMddHHmmss"),reHash.toString());
                    SendreturnData(RETURN_OK, reHash.get(), "키다운로드응답");
                    HideDialog();
//                            ReadyDialogHide();
                });
            });

            securitySDK.Req_SecurityKeyUpdate(termID,BsnNo,serial);    //무결성검증을 완료한다
            return;
        }

        _reHash.put("TmlcNo",Utils.getAppID());
        _reHash.put("SfVer",Constants.TEST_SOREWAREVERSION);

        _reHash.put("HSet","1");

        if( (Setting.getPreference(instance, Constants.APPTOAPP_TID)).equals(""))
        {
            _reHash.put("TermIDSet","0");
            _reHash.put("CntTermID","0");
            _reHash.put("TermID","");
        }
        else
        {
            _reHash.put("TermIDSet","1");
            //Todo: 일단 1개만 사용가능하다. 차후 복수 가맹점 가능하도록 한다. 22-04-25 by.jiw
            _reHash.put("CntTermID","1");
            _reHash.put("TermID",Setting.getPreference(instance, Constants.APPTOAPP_TID));
        }

        /* 로그기록. 버전정보 */
        cout("SEND : VERSION_INFO",Utils.getDate("yyyyMMddHHmmss"),_reHash.toString());
        SendreturnData(RETURN_OK, _reHash, "버전정보응답");
        HideDialog();
    }


    class AppToAppDetectTimer extends CountDownTimer
    {
        public AppToAppDetectTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if(Count<mPosSdk.getSerialDevicesBusList().length-1) {
                Count++;
                SendDetectData2();
            }
            else {
                /** 무결성검증이 장치정보요청 뒤로 순서가 밀림. 이유는 무결성검증을 안 할 수도 있기 때문 */
                ReadICReaderUniqueID();
//                ClassfiCheck();
                /** */
            }
        }
    }
}
