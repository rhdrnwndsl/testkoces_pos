package com.koces.androidpos.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.koces.androidpos.MainActivity;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

//        String action= intent.getAction();
//
//        if( action.equals("android.intent.action.BOOT_COMPLETED") ){
//
//            Intent i= new Intent(context, MainActivity.class);
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(i);
//        }
    }
}



