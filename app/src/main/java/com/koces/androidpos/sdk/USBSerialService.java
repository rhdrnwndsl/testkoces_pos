package com.koces.androidpos.sdk;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.koces.androidpos.sdk.SerialPort.KocesSerial;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;

public class USBSerialService extends Service {
    private IBinder mBinder;
    public static KocesSerial mSerial;
    public USBSerialService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        mBinder = new LocalBinder();

    }

    @Override
    public int onStartCommand(Intent _intent,int _flag,int _startId){
        return Service.START_STICKY;

    }

    @Override
    public void onDestroy()
    {
        if(mSerial!=null)
        {
            //mSerial.Close();
        }
        super.onDestroy();
    }

    public class LocalBinder extends Binder
    {
        public USBSerialService getUsbSerialService()
        {
            return USBSerialService.this;
        }
    }
    public void initSerial(Context _ctx)
    {
        mSerial = new KocesSerial(_ctx,null,null);
    }
    public void setSerial(Handler _handler, SerialInterface.ConnectListener _listener2)
    {
        mSerial.SerialDataSet(_handler,_listener2);

    }

    public KocesSerial getSerial()
    {
        return mSerial;
    }
}
