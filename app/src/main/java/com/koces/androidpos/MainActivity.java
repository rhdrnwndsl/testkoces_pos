package com.koces.androidpos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.koces.androidpos.sdk.AppVerify;
import com.koces.androidpos.sdk.InitialProc;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

import java.util.ArrayList;

/**
 * 앱 실행시 가장 먼저 실행 되는 Activity<br>
 * <진행 절차 표시><br>
 * 체크 루팅 여부<br>
 *      ↓<br>
 * 앱 무결성 검사<br>
 *      ↓<br>
 *  퍼미션 체크<br>
 *      ↓<br>
 * 네트워크 상태 체크<br>
 *      ↓<br>
 * 메인 구성 페이지<br>
 */
public class MainActivity extends BaseActivity {
    /** Log를 위한 TAG 선언 */
    private final static String TAG = MainActivity.class.getSimpleName();
    /** 현재 context 를 instance */
    private static Context instance;
    /** (현재 사용되지 않는 변수) */
    private static boolean bServiceFlag =false;
    /** 앱투앱으로 실행 시 인텐트로 받은 변수 해당변수는 메인2엑티비티로 보낸다 */
    int mAppToApp = 0;
    /** 화면에 장치를 선택 했을 때의 번호 */
    int mSelectedDeviceIndex = -1;

    /** 화면 전체 linearlayout
     * 설정이 필요한 경우에만 화면에 표시 한다.*/
    LinearLayout mMainLayout = null;
    KocesPosSdk mKocesPosSdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMainLayout = (LinearLayout)findViewById(R.id.init_set_linearlayout);
        mMainLayout.setVisibility(View.INVISIBLE);
        instance = this;
        Intent intent = getIntent();
        try {
            mAppToApp = intent.getExtras().getInt("AppToApp");
        }
        catch (NullPointerException ex)
        {
            mAppToApp = 0;
            Log.d(TAG,ex.toString());
        }
        long currentClickTime = SystemClock.uptimeMillis();
        long elapsedTime = currentClickTime - Setting.getIsmLastClickTime();
        Setting.setIsmLastClickTime(currentClickTime);

        // 중복클릭 아닌 경우
        if (elapsedTime <= Setting.getIsMIN_CLICK_INTERVAL()) {
            Log.d(TAG,"현재  중복클릭 " +  elapsedTime);
            finishAffinity();
            return;
        }
        if (mAppToApp == 0) {
            if (Setting.getIsAppForeGround() == 2) {
                Log.d(TAG,"현재  Setting.getIsAppForeGround() " +  Setting.getIsAppForeGround());
//                Setting.setIsAppForeGround(1);
                finishAffinity();
                return;
            }
        } else {
//            Setting.setIsAppForeGround(1);
        }
        actAdd(this);
        CheckRooting();
    }

    /**
     * 루팅 여부를 체크 하는 함수
     */
    private void CheckRooting()
    {
        Log.d(TAG,"Checking Rooting Device");
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = InitialProc.CheckSuperUser();
                if (result == true) {
                    Dispose("루팅된 폰입니다.");
                } else {
                    //CheckVerity();기기에서 에러가 발생해서 원인을 알 수 없어 일단 이 부분을 패스 한다.
                    Verity();
                }
            }
        }).start();
    }

    /**
     * new Handler에서 앱 무결성 검사 함수
     */
    private void Verity()
    {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CheckVerity();
            }
        });
    }

    /**
     * 앱 무결성 검사 함수
     */
    private void CheckVerity()
    {
        boolean isAppVerity = false;
        try {
            isAppVerity = new AppVerify(true, "com.Koces.androidPos").checkVerify(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!isAppVerity){
            mainfest();
        }else {
            Dispose("무결성 검사 에러 발생");
        }
    }

    /**
     * 안드로이드 퍼미션을 확인 요청 하는 함수
     */
    private void mainfest()
    {
        CheckNetworkState();
    }

    /**
     * 네트워크 연결 상태를 체크 한다.
     */
    private void CheckNetworkState()
    {
        if(Utils.getNetworkState(this)>0)
        {
            CheckRegistagency();
        }
        else {
            Dispose("네트워크에 연결 되어 있지 않습니다.");
        }
    }

    /**
     * Main2Activity로 이동 한다. 만일 앱투앱을 통해 실행되었을 때 메인2로 변수값을 보낸다
     */
    private void CheckRegistagency()
    {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Setting.getPreference(instance, Constants.APP_PERMISSION_CHECK).equals(""))
                {
                    if(mAppToApp == 1)
                    {
                        CheckDevice();
                    }
                    else
                    {
                        CheckPermissionPage();
                    }

                }
                else
                {
                    CheckDevice();
                }


            }
        },500);
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
                if(mAppToApp == 1) {
                    //앱투앱을 실행했을 때. 이 때, 특정한 요청이 없으면 메인으로 들어온다. 특정한 요청이 있을 경우에만 앱투앱엑티비티가 정상실행된다.
                    //여기서는 따로 어떤 것을 할 이유가 없다. 앱투앱엑티비티에서 셋팅할 것이기 때문이다.
                    Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
                    intent.putExtra("AppToApp", mAppToApp);
                    mAppToApp = 0;
                    startActivity(intent);
                } else {
                    //퍼미션을 가져온다
                    checkPermission(mAppToApp,mPermissionCheckListener);

                }
                return;
            case BLE:       //BLE의 경우
            case CAT:       //WIFI CAT의 경우
            case LINES:     //유선장치의 경우
                Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
                intent.putExtra("AppToApp", mAppToApp);
                mAppToApp = 0;
                startActivity(intent);
                return;
            default:
                Dispose("장치설정에 오류가 발생하였습니다. 앱을 제거 후 재설치해 주십시오.");
                return;
        }
    }
    /** 화면에 장치 선택 화면을 표시하고 관련 처리를 한다.
     * 211222 kim.jy 추가
     * */
    private void showSelectDeviceMenu(){
        LinearLayout layoutLines,layoutBle,layoutCat;
        ImageView imgLines,imgBle,imgCat;
        Button btnSave;

        layoutLines = (LinearLayout)findViewById(R.id.main_linearLayout_lines);
        layoutBle = (LinearLayout)findViewById(R.id.main_linearLayout_ble);
        layoutCat = (LinearLayout)findViewById(R.id.main_linearLayout_cat);

        imgLines = (ImageView)findViewById(R.id.main_imgbtn_lines);
        imgBle = (ImageView)findViewById(R.id.main_imgbtn_ble);
        imgCat = (ImageView)findViewById(R.id.main_imgbtn_cat);

        btnSave = (Button)findViewById(R.id.main_btn_setok);
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

        mMainLayout.setVisibility(View.VISIBLE);

        final boolean[] _btnCheck = {false};
        btnSave.setOnClickListener( v-> {
            if(_btnCheck[0])
            {
                return;
            }
            _btnCheck[0] = true;
            Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
            Setting.setTopContext(this);
            if(KocesPosSdk.getInstance()==null) {
                mKocesPosSdk = new KocesPosSdk(this);
                mKocesPosSdk.setFocusActivity(this,null);
//                        mKocesPosSdk.ResetSerial();
            }
            else
            {
                mKocesPosSdk = KocesPosSdk.getInstance();
                mKocesPosSdk.setFocusActivity(this,null);
            }
//            mKocesPosSdk.BleregisterReceiver(this);
            switch (mSelectedDeviceIndex) {
                case 1:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    Setting.setPreference(instance,Constants.BLE_TIME_OUT,"20");
                    intent.putExtra("AppToApp", mAppToApp);
                    mAppToApp = 0;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(intent);
                        }
                    },3000);
                    break;
                case 2:
                    mMainLayout.setVisibility(View.INVISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CatIPSet(); //캣이라면 ip/port 를 설정해야 한다.
//                                    Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
//                                    Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                        }
                    },100);
                    break;
                case 3:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.LINES;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    Setting.setPreference(instance,Constants.USB_TIME_OUT,"60");
                    intent.putExtra("AppToApp", mAppToApp);
                    mAppToApp = 0;

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(intent);
                        }
                    },3000);

                    break;
                default:
                    Toast.makeText(getApplicationContext(),"연결 하실 장치를 선택해 주세요.",Toast.LENGTH_LONG).show();
                    break;
            }
        });

    }
    /** 다른 장치를 선택하면 그대로 진행하지만 만일 CAT 을 선택했다면 ip/port 를 설정해야한다 */
    private void CatIPSet()
    {
        CatSetDialog mCatSet = new CatSetDialog(this, new CatSetDialog.DialogBoxListener() {
            @Override
            public void onClickCancel(String _msg) {
                Toast.makeText(instance,_msg,Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
                intent.putExtra("AppToApp", mAppToApp);
                mAppToApp = 0;
                startActivity(intent);
            }

            @Override
            public void onClickConfirm(String _msg) {
                Toast.makeText(instance,_msg,Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
                intent.putExtra("AppToApp", mAppToApp);
                mAppToApp = 0;
                startActivity(intent);
            }
        });
        mCatSet.show();
//        return;

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
//                            Dispose("CAT IP 설정이 잘못되었습니다");
//                            return;
//                        }
//                        if (edit_cat_port.getText() == null || edit_cat_port.getText().toString().equals(""))
//                        {
//                            Dispose("CAT PORT 설정이 잘못되었습니다");
//                            return;
//                        }
//
//                        Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
//                        Setting.setPreference(instance, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
//                        Setting.setCatVanPORT(instance, edit_cat_port.getText().toString());
//                        Setting.setCatIPAddress(instance, edit_cat_ip.getText().toString());
//                        Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
//                        if(mAppToApp == 1) {
//                            intent.putExtra("AppToApp", mAppToApp);
//                            mAppToApp = 0;
//                        }
//                        startActivity(intent);
//
//                    }
//                });
//
//        // create alert dialog
//        AlertDialog alertDialog = alertDialogBuilder.create();
//
//        // show it
//        alertDialog.show();
    }

    /** 퍼미션을 체크하여 정상적으로 권한을 받아왔다면 다음을 실행한다. */
    BaseActivity.PermissionCheckListener mPermissionCheckListener = new PermissionCheckListener() {
        @Override
        public void onResult(boolean _result) {
            if (_result == true)
            {
                //퍼미션 권한을 받아왔다.
                //앱투앱이 아닌 본앱을 연상황
                //여기서 셋팅한다.
                //여기서 화면의 레이아웃을 표시 한다. 211222 kim.jy
                showSelectDeviceMenu();
            } else {
                //퍼미션 권한을 받아오지 못했다.
                final String finalTmp = "권한요청을 사용자가 승인하지 않았습니다";
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(instance, finalTmp,Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(()->{
                            finishAndRemoveTask();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        },2000);
                    }
                });
            }
        }
    };

    /** 만일 루팅,무결성,네트워크이상이 발생할 때 앱 종료를 위한 전단계 */
    private void Dispose(String _str)
    {
        final String finalTmp = _str;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(instance, finalTmp,Toast.LENGTH_SHORT).show();
                FinishApp();
            }
        });
    }

    /** 만일 루팅,무결성,네트워크이상이 발생할 때 최종 앱 종료 */
    private void FinishApp()
    {
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            finishAndRemoveTask();
            android.os.Process.killProcess(android.os.Process.myPid());
        },2000);
    }

//    @Override
//    protected void onDestroy() {
//        unregisterReceiver(screenOffReceiver);
//        finishAndRemoveTask();
//        super.onDestroy();
//        android.os.Process.killProcess(android.os.Process.myPid());
//
//    }

//    class SrvConn implements ServiceConnection {
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mUsbSerialService = ((USBSerialService.LocalBinder)service).getUsbSerialService();
//            mUsbSerialService.initSerial(instance);
//            Setting.mUsbService = mUsbSerialService;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            Log.d(TAG,"finish UsbSerialService service");
//        }
//    }

    /**
     * (사용하지 않음)<br>
     * 안드로이드 화면에 아이콘을 만드는 함수
     */
    private void MakeIcon()
    {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        pref.getString("check", "");
        if(pref.getString("check", "").isEmpty()){
            Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
            shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            shortcutIntent.setClassName(this, getClass().getName());
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Intent intent = new Intent();

            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_foreground));
            intent.putExtra("duplicate", false);
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");

            sendBroadcast(intent);
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("check", "exist");
        editor.commit();
    }
}
