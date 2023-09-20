package com.koces.androidpos.sdk.SerialPort;

import java.io.IOException;
import java.util.HashMap;

public class SerialInterface {
    public interface DataListener {
        /**
         * _type 0은 일반적인 응답 전문,_type 1은 사인패드 데이터 전문
         * @param _rev
         * @param _type
         */
        void onReceived(byte[] _rev,int _type);
    }
    public interface DataListenerCash {
        /**
         * _type 0은 일반적인 응답 전문,_type 1은 사인패드 데이터 전문
         * @param _rev
         * @param _type
         */
        void onReceived(byte[] _rev,int _type);
    }
    public interface PaymentListener{
        void result(String result, String Code, HashMap<String,String> resultData);
    }

    public interface CatPaymentListener{
        void result(String result, String Code, HashMap<String,String> resultData);
    }


    public interface KeyUpdateListener{
        void result(String result, String Code, String state, HashMap<String,String> resultData);
    }
    public interface ConnectListener {
        void onState(boolean _bState,String _EventMsg,String _BusName);
    }
}
