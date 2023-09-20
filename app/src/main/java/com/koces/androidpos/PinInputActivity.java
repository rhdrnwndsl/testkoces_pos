package com.koces.androidpos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.van.Constants;

public class PinInputActivity extends BaseActivity {
    private final static String TAG = PinInputActivity.class.getSimpleName();
    Button mbtn_ok,mbtn_cancel;
    SignPad _signView;
    KocesPosSdk mKocesPosSdk;
    int mMoney;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_pad);
        if(KocesPosSdk.getInstance()==null) {
            mKocesPosSdk = new KocesPosSdk(this);
            mKocesPosSdk.setFocusActivity(this,null);
        }
        else
        {
            mKocesPosSdk = KocesPosSdk.getInstance();
            mKocesPosSdk.setFocusActivity(this,null);
        }
        //서명패드 장비 요청
        if(mKocesPosSdk.getUsbDevice()!=null) {
            mKocesPosSdk.__PosInit("99", mDataListener,new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
        }

        Intent intent = new Intent();
        try {
            mMoney = intent.getExtras().getInt("Money");
        }
        catch (NullPointerException ex)
        {
            //Log.d(TAG,ex.toString());
        }

        _signView = findViewById(R.id.sign_signpad);
        Setting.setIsAppForeGround(1);
    }

    private void SaveBitmap()
    {
        Bitmap bmpSignData = _signView.saveImage();

    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        Rect dialogBounds = new Rect();
        getWindow().getDecorView().getHitRect(dialogBounds);
        if(!dialogBounds.contains((int)ev.getX(),(int)ev.getY()))
        {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    SerialInterface.DataListener mDataListener = new SerialInterface.DataListener() {
        @Override
        public void onReceived(byte[] _rev, int _type) {
            if(_type==0)
            {
                final byte[] b = _rev;

                if(b[3]== Command.ACK)
                {
                    String msg1 =  "금액: " + String.valueOf(mMoney) + "원";
                    String msg2 =  "서명하세요";
                    mKocesPosSdk.__PadRequestSign("0","0", Constants.WORKING_KEY,msg1,msg2," "," ",mDataListener,new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                }
            }
            else
            {
                final byte[] b = _rev;
            }
        }
    };
}
