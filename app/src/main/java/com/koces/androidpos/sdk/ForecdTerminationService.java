package com.koces.androidpos.sdk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.sdk.ble.bleSdk;

public class ForecdTerminationService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) { //핸들링 하는 부분
        Log.e("Error","onTaskRemoved - 강제 종료 " + rootIntent);
        Toast.makeText(this, "onTaskRemoved ", Toast.LENGTH_SHORT).show();
        KocesPosSdk mPosSdk;
        mPosSdk = KocesPosSdk.getInstance();
        if (mPosSdk != null) {
            mPosSdk.mTcpClient.Dispose();
            mPosSdk.mSerial.Dispose();
            mPosSdk.mSerial.Close();
        }
        bleSdk mbleSdk;
        mbleSdk = bleSdk.getInstance();
        if (mbleSdk != null) {
            mbleSdk.DisConnect();
        }



        stopSelf(); //서비스 종료
    }
}

