package com.koces.androidpos.sdk;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.koces.androidpos.R;

/**
 * App to APP 을 Service형태로 올리기 위한 함수<br>
 * (현재 사용하지 않는다. BLE 또는 CAT을 위해서 함수 제작)
 */
public class AppToAppService extends Service {
    private final static String TAG = "KOCES_AppToAppService";
    WindowManager wm;
    View mView;
    public AppToAppService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        CreateViewPage();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand()");
        //String param = intent.getStringExtra("name");
        //Toast.makeText(this, "received param : "+param, Toast.LENGTH_SHORT).show();
        //여기서 OnDestroy 된 서비스를 다시 startActivity로 실행한 경우 여기로 들어온다.
        if(wm==null && mView ==null) {
            CreateViewPage();
        }
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wm != null) {
            if(mView != null) {
                wm.removeView(mView);
                mView = null;
            }
            wm = null;
        }
        Log.d(TAG,"onDestroy()");
    }

    /**
     * 최상단에 팝업을 표시 하는 함수
     */
    private void CreateViewPage()
    {
        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                /*ViewGroup.LayoutParams.MATCH_PARENT*/ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                //PixelFormat.TRANSLUCENT);
                PixelFormat.RGB_888);
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        mView = inflate.inflate(R.layout.activity_app_to_app_pop_up, null);
        final ProgressBar atoa_progressBar = (ProgressBar)mView.findViewById(R.id.atoa_progressBar);
        final LinearLayout body = (LinearLayout) mView.findViewById(R.id.atoa_linearLayout);
        final TextView textView = (TextView) mView.findViewById(R.id.atoa_txt_test);
        final Button bt =  (Button) mView.findViewById(R.id.atoa_btn_test1);
        final Button bt2 =  (Button) mView.findViewById(R.id.atoa_btn_test2);

        //atoa_progressBar.setProgress();
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("on click!!");
            }
        });

        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestroy();
            }
        });
        wm.addView(mView, params);
    }
}