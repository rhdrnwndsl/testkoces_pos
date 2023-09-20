package com.koces.androidpos.sdk.van;

public class TcpInterface {
    public interface ConnectListener {
        void onState(boolean _bState,String _EventMsg);
    }
    public interface DataListener {
        //void onRecviced(byte[] _rev,int _type);
        void onRecviced(byte[] _rev);
    }
}
