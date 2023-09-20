package com.koces.androidpos;

import android.Manifest;
import android.app.Activity;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.koces.androidpos.sdk.ForecdTerminationService;
import com.koces.androidpos.sdk.ForegroundDetector;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.log.LogFile;
import com.koces.androidpos.sdk.van.Constants;

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {
    /** 오토디텍트 시 사용되는 토스트박스. 앱투앱 실행시 다이알로그박스가 최상단으로 나오지 않기 때문에 사용한다 */
    Toast m_toast=null;
    /** 일반적으로 사용되는 다이알로그 메시지 박스 */
    CustomDialog m_dialog;
    /** 각 액티비티에서 베이스엑티비티를 통해 로그파일 접근 */
    LogFile m_logfile;
    /** Oreo 버전 이상의 안드로이드에서 정상적으로 권한을 획득했다는 것을 체크 */
    final int PERMISSIONS_REQUEST_CODE = 1;
    /** 실행하는 엑티비티를 배열로 저장한다. 여러 엑티비티를 실행 하고 종료하면 제대로 종료 안되는 경우가 발생한다 */
    public static ArrayList<Activity> actlist = new ArrayList<Activity>();

    /** 메인2, 앱투앱엑티비티 에서 퍼미션(권한) 체크를 하는데 이때 결과를 보내줄 리스너 */
    PermissionCheckListener mPermissionCheckListener;

    private ForegroundDetector foregroundDetector;

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            hideSystemUI();
//        }
//    }
//
//    private void hideSystemUI() {
//        // Enables regular immersive mode.
//        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
//        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        // Set the content to appear under the system bars so that the
//                        // content doesn't resize when the system bars hide and show.
//
//                        // Hide the nav bar and status bar
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 화면 꺼짐 방지

        m_dialog = null;

        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) //dialog의 경우에는 actionbar가 null인 경우가 있음
        {
            actionBar.hide();
        }

        foregroundDetector = new ForegroundDetector(this.getApplication());
        foregroundDetector.addListener(new ForegroundDetector.Listener() {
            @Override
            public void onBecameForeground() {
                Log.d("TAG", "Became Foreground");
//                Setting.setIsAppForeGround (1);
            }

            @Override
            public void onBecameBackground() {
                Log.d("TAG", "Became Background");
//                Setting.setIsAppForeGround (2);
            }
        });

        startService(new Intent(this, ForecdTerminationService.class));
    }

    //Target Sdk버전이 올라가면서 투명 윈도우에서 에러 발생하는 문제
    //2021.12.24 kim.jy
//    @Override
//    public void setRequestedOrientation(int requestedOrientation) {
//   /*
//    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
//        // no-op
//    }else{
//        super.setRequestedOrientation(requestedOrientation);
//    }
//    */
//        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
//            super.setRequestedOrientation(requestedOrientation);
//        }
//    }



    /**
     * 종료 시 각각의 엑티비티들을 테스크에서 제거하면서 종료한다
     */
    public void actFinish()
    {
        for(int i=actlist.size()-1; i>=0; i--)
        {
            actlist.get(i).finishAndRemoveTask();
        }
    }

    /**
     * 각 엑티비티들이 실행 시 해당 엑티비티들을 배열로 저장한다. 향후 종료시 모든 엑티비티들을 정상 종료하기 위해서
     * @param _activity
     */
    public void actAdd(Activity _activity)
    {
        for(Activity n:actlist)
        {
            if(n.getLocalClassName().equals(_activity.getLocalClassName()))
            {
                return;
            }
        }
        actlist.add(_activity);
    }

    /**
     * 다이얼로그 프로그래스바를 출력 한다.
     * @param _ctx  현재 액티비티
     * @param _msg  출력 메세지
     * @param _TimerCount  타이머 카운트
     */
    public void ReadyDialogShow(Context _ctx,String _msg,int _TimerCount)
    {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
                ComponentName componentName= info.get(0).topActivity;
                String topActivityName = componentName.getShortClassName().substring(1);
                Log.d("Dialog_Activity_topActivityName",topActivityName);
                Log.d("Dialog_Activity_getTopContext",Setting.getTopContext().toString());
                Log.d("Dialog_Activity__ctx",_ctx.toString());
                if(m_dialog==null) {
                    m_dialog = new CustomDialog(_ctx, _msg, null, null, _TimerCount);
                }
                else {
                    m_dialog.setDialog(_ctx,_msg, null, null, _TimerCount, false);
                }
                m_dialog.getWindow().setGravity(Gravity.CENTER);
//            if(Settings.canDrawOverlays(this)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    m_dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
//                } else {
//                    m_dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
//                }
//            }
                m_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                m_dialog.setCanceledOnTouchOutside(false);
                Activity _ac = (Activity) _ctx;
                if(!_ac.isFinishing() && !_ac.isDestroyed())
                {
                    try {
                        m_dialog.show();
                    } catch (Exception e)
                    {

                    }
                }
                else
                {
                    Toast.makeText(Setting.getTopContext(),_msg,Toast.LENGTH_LONG).show();
                }

            }
        },200);

    }

    /**
     * 카드리더기에서 카드를 읽기 전 카드 취소버튼 활성화를 위해서만 사용. 그 이외에는 미사용
     * @param _ctx
     * @param _msg
     * @param _TimerCount
     * @param _dgTimeOut
     */
    public void ReadyDialogShow(Context _ctx,String _msg,int _TimerCount, boolean _dgTimeOut)
    {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
                ComponentName componentName= info.get(0).topActivity;
                String topActivityName = componentName.getShortClassName().substring(1);
                Log.d("Dialog_Activity_topActivityName",topActivityName);
                Log.d("Dialog_Activity_getTopContext",Setting.getTopContext().toString());
                Log.d("Dialog_Activity__ctx",_ctx.toString());
                if(m_dialog==null) {
                    m_dialog = new CustomDialog(_ctx,_msg,null,null,_TimerCount,_dgTimeOut);
                }
                else {
                    m_dialog.setDialog(_ctx,_msg,null,null,_TimerCount,_dgTimeOut);
                }
                m_dialog.getWindow().setGravity(Gravity.CENTER);
//            if(Settings.canDrawOverlays(this)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    m_dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
//                } else {
//                    m_dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
//                }
//            }
                m_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                m_dialog.setCanceledOnTouchOutside(false);
                Activity _ac = (Activity) _ctx;
                if(!_ac.isFinishing() && !_ac.isDestroyed())
                {
                    try {
                        m_dialog.show();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
                else
                {
                    Toast.makeText(Setting.getTopContext(),_msg,Toast.LENGTH_LONG).show();
                    cout("DIALOG : MESSAGE", Utils.getDate("yyyyMMddHHmmss"),_msg);
                }
            }
        },200);

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

    /**
     * 다이알로그 프로그래스바를 닫는다
     */
    public void ReadyDialogHide()
    {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (m_dialog != null) {
                    m_dialog.stopTimer();
                    try {
                        m_dialog.dismiss();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        },200);
    }

    /**
     * 토스트 메시지 박스. 앱투앱으로 실행 시 다이얼로그메시지박스가 화면 제일 앞으로 나오지 않기때문에
     * 특정 상황에서만 사용한다
     * @param _ctx
     * @param _str
     * @param _length
     */
    public void ReadyToastShow(Context _ctx, String _str, int _length)
    {
        if(m_toast != null){m_toast.cancel();}
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                (ViewGroup) findViewById(R.id.custom_toast_container));

        TextView text = (TextView) layout.findViewById(R.id.toast_msr);
        text.setText(_str);

        Toast toast = new Toast(_ctx);
        m_toast = toast;
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.setDuration(_length);
        toast.setView(layout);
        toast.show();
    }

    /**
     * 로그 파일을 위해 스트링 값을 입력 받아 로그파일클래스로전달한다
     * @param _str
     */
    public void WriteLogFile(String _str)
    {
        if(m_logfile == null) {
            m_logfile = new LogFile(this);
        }
        m_logfile.writeLog(_str);
    }

    private int mAppToAppCheck = 0; //0 = 본앱, 1 = 앱투앱
    /** 로그파일을 만들기 위한 읽기,쓰기권한을 체크한다*/
    public void checkPermission(int _AppToApp, BaseActivity.PermissionCheckListener _PermissionCheckListener)
    {
        mAppToAppCheck = _AppToApp;
        mPermissionCheckListener = _PermissionCheckListener;
        String[] permissions;
        if (Build.VERSION.SDK_INT > 30) {

            permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
            };
        } else {
            permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.BLUETOOTH,
            };
        }

        if(!hasPermissions(this, permissions)){
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            if(m_logfile == null){m_logfile = new LogFile(this);}
            m_logfile.deleteLogFile();
            mPermissionCheckListener.onResult(true);
            return;
//            if (Settings.System.canWrite(this))
//            {
//                //모든 퍼미션 정상체크
//            } else {
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
//                    intent.setData(Uri.parse("package:" + getPackageName()));
//                    startActivityForResult(intent, 23);
//                }
//            }
        }


    }

    /**
     * (사용하지 않음)<br>
     * 앱이 최상단에 나오게끔 하기 위한 권한 요청 */
    public void askPermission() {
        Intent intent= new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
        startActivityForResult(intent,5469);
    }

    /** checkPermission() 를 통해 권한을 요청하고 이곳에서 그 결과를 받는다 */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            //사용자가 권한 거절시. 일단 읽고 쓰기 권한을 거절해도 다른 권한(메시지박스를 다른 앱들 위에 띄우기)을 물어봐야한다.
                            mPermissionCheckListener.onResult(false);
                            return;
                        }
                    }

                    //권한 허용 선택시
                    //오레오부터 꼭 권한체크내에서 파일 만들어줘야함
                    //권한을 확인(파일 읽고 쓰기문제. 로그파일 만들기 위해서는 권한을 확인해 줘야 한다
                    if (m_logfile == null) {
                        m_logfile = new LogFile(this);
                    }
                    m_logfile.deleteLogFile();
                    mPermissionCheckListener.onResult(true);
                    return;

                } else {
                    mPermissionCheckListener.onResult(false);
                    return;
                }

        }
    }



    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * (사용하지 않음)<br>
     * 앱이 최상위로 되었는지 권한요청에 대한 결과를 받는다 */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 5469){
            if(!Settings.canDrawOverlays(this)){
                //정상적으로 최상위 앱으로 되었다면 해당 값이 true로 떨어졌어야 했다
                //일단 체크 확인을 안 했어도 다시 불러들이거나 하지 않는다.
       //         askPermission();
            }
            else{
//                checkPermission(0,BaseActivity.PermissionCheckListener);
            }
        } else if (requestCode == 23) {
            Toast.makeText(this, "모든 권한 요청이 완료되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /**
     * 백버튼시 상황설정(현재 메인화면에서만 상황 구현(백그라운드 이동)
     * 활성화된 액티비티단에 따라 구현내용변경 종료 하지 않고 background task 로 보낸다.
     */
    @Override
    public void onBackPressed() {
        String classname = this.getClass().getSimpleName();
        switch (classname){
            case "Main2Activity":
                MoveBackGround();
                break;
            default:
                break;
        }
    }

    /** 앱을 백그라운드로 이동(최소화) */
    public void MoveBackGround(){
/*
    둘 다 동일하게 작동합니다.
    MoveTaskToBack의 인자가 true일 땐 RootActivity에서 호출되었을 때만 Background로
    			false일 땐 그냥 Background로 보내버립니다.
    */
        //moveTaskToBack(true);
//        moveTaskToBack(false);
        Setting.setIsAppForeGround (2);
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    /**
     * (사용하지 않음)<br>
     * 안드로이드가 해당 앱이 정상적으로 설치되어 있는지를 확인하는 함수 */
    public synchronized boolean getPackageList()
    {
        boolean isExist = false;

        PackageManager pkgMgr = getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith("com.koces.androidpos")) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    /** 리스너_AppToAppUsbSerialStateListener 거래 중 특정오류(상황) 발생 시 리스너를 통해 결과를 전달한다*/
    public interface AppToAppUsbSerialStateListener
    {
        void onState(int _state);
    }

    /** 퍼미션체크하여 정상적으로 완료되었을 경우 리스너를 통해 메인2 또는 앱투앱엑티비티로 전달한다 */
    public interface PermissionCheckListener
    {
        void onResult(boolean _result);
    }

    public static boolean needPermissionForBlocking(Context context){
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return  (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isForeGroundEvent(UsageEvents.Event event) {

        if(event == null) return false;

        if(BuildConfig.VERSION_CODE >= 29)
            return event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED;

        return event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND;
    }
}
