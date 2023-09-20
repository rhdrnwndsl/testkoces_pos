package com.koces.androidpos.sdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.koces.androidpos.R;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.ble.bleSdkInterface;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * 키 갱신을 위한 클래스
 */
public class DeviceSecuritySDK {

    private KocesPosSdk mPosSdk;
    private bleSdk mbleSdk;
    private int mTradeType = 0;  //장비 선택f
    SerialInterface.KeyUpdateListener mKeyUpdateListener;
    private bleSdkInterface.BLEKeyUpdateListener mBLEKeyUpdateListener;
    private Context mCtx;
    private String mBleOrSerial;
    private BleIntegTimer m_bletimer;

    private String mProductName = "";    //유선연결 장비 중 제품식별번호가 ###### 만 날아오는 제품이 있다. 해당제품의 경우 키갱신이 nak 되어도 정상처리한다

    HashMap<String,String> hashMap = new HashMap<String, String>();
    String mStoreNumber = "";
    String mTid = "";
    String mSerialNumber = "";

    //키갱신 시 거절당하면 한번더 시도한다
    /**
     * 키갱신 시 거절당하면 한번더 시도 하기 위한 변수
     */
    private int mKeyUpdate = 0;
    private int LastCommand;

    /**
     * 초기화 함수
     * @param _ctx
     * @param _tradeType
     * @param function
     * @param _KeyUpdateListener
     */
    public DeviceSecuritySDK(Context _ctx, int _tradeType, String function, SerialInterface.KeyUpdateListener _KeyUpdateListener)
    {
        hashMap = null;
        mStoreNumber = "";
        mTid = "";
        mSerialNumber = "";

        mTradeType = _tradeType;
        mPosSdk = KocesPosSdk.getInstance();
        mKeyUpdateListener = _KeyUpdateListener;
        mCtx = _ctx;
        mBleOrSerial = "Serial";
    }

    private void Clear()
    {
        mTradeType = 0;
        mKeyUpdateListener = null;
        mKeyUpdate = 0;
        mBleOrSerial = "";
    }

    /**
     * 키 갱신 요청 함수
     */
    public void Req_Integrity()
    {
        if(mPosSdk.getUsbDevice().size()>0) {
            mPosSdk.__PosInit("99",null,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
            if(mTradeType==1) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LastCommand = Command.CMD_SELFKEYCHECK_REQ;
                        mPosSdk.__Integrity(mDataListener, mPosSdk.getICReaderAddr2());
                    }
                }, 1000);
            }
            else
            {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LastCommand = Command.CMD_SELFKEYCHECK_REQ;
                        mPosSdk.__Integrity(mDataListener,new String[]{mPosSdk.getMultiReaderAddr()});
                    }
                }, 1000);
            }

        }
        else
        {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String tmp = mCtx.getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device);
                    mKeyUpdateListener.result(tmp,"","ERROR",null);
                }
            });

        }
    }

    /**
     * 보안 키 업데이트 요청 함수(특정제품 제품식별번호가 ###### 만 있는 것)
     */
    public void Req_SecurityKeyUpdate(String _product)
    {
        if(mPosSdk.getUsbDevice().size()>0) {
            mProductName = _product;
            if(!Setting.getPreference(mCtx, Constants.SELECTED_DEVICE_CARD_READER).equals(""))
            {
                mPosSdk.__PosInit("99", new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        mPosSdk.__SecurityKeyUpdateReady(mDataListener, mPosSdk.getICReaderAddr2());
                    }
                }, mPosSdk.getICReaderAddr2());


            }
            else
            {
                mPosSdk.__PosInit("99", new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        mPosSdk.__SecurityKeyUpdateReady(mDataListener, mPosSdk.getMultiReaderAddr2());
                    }
                }, mPosSdk.getMultiReaderAddr2());

            }

        }
        else
        {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String tmp = mCtx.getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device);
                    mKeyUpdateListener.result(tmp,"","ERROR",null);
                }
            });

        }

    }

    public void Req_SecurityKeyUpdate(String _tid, String _bsn, String _serial)
    {
        mTid = _tid;
        mStoreNumber = _bsn;
        mSerialNumber = _serial;

        if(mPosSdk.getUsbDevice().size()>0) {
            if(!Setting.getPreference(mCtx, Constants.SELECTED_DEVICE_CARD_READER).equals(""))
            {
                mPosSdk.__PosInit("99", new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        mPosSdk.__SecurityKeyUpdateReady(mDataListener, mPosSdk.getICReaderAddr2());
                    }
                }, mPosSdk.getICReaderAddr2());

            }
            else
            {
                mPosSdk.__PosInit("99", new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        mPosSdk.__SecurityKeyUpdateReady(mDataListener, mPosSdk.getMultiReaderAddr2());
                    }
                }, mPosSdk.getMultiReaderAddr2());

            }

        }
        else
        {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String tmp = mCtx.getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device);
                    mKeyUpdateListener.result(tmp,"","ERROR",null);
                }
            });

        }

    }

    /**
     * 보안 키 업데이트 요청 함수
     */
    public void Req_SecurityKeyUpdate()
    {
        if(mPosSdk.getUsbDevice().size()>0) {
            if(!Setting.getPreference(mCtx, Constants.SELECTED_DEVICE_CARD_READER).equals(""))
            {
                mPosSdk.__PosInit("99", new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        mPosSdk.__SecurityKeyUpdateReady(mDataListener, mPosSdk.getICReaderAddr2());
                    }
                }, mPosSdk.getICReaderAddr2());

            }
            else
            {
                mPosSdk.__PosInit("99", new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        mPosSdk.__SecurityKeyUpdateReady(mDataListener, mPosSdk.getMultiReaderAddr2());
                    }
                }, mPosSdk.getMultiReaderAddr2());

            }

        }
        else
        {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String tmp = mCtx.getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device);
                    mKeyUpdateListener.result(tmp,"","ERROR",null);
                }
            });

        }

    }

    private SerialInterface.DataListener mDataListener = new SerialInterface.DataListener() {
        @Override
        public void onReceived(byte[] _rev, int _type) {
            if(_type==Command.PROTOCOL_TIMEOUT)
            {
                mKeyUpdateListener.result("실패 : 장치 정보를 읽어오지 못했습니다", "", "SHOW", null);

                return;
            }
            if(_type != Command.PROTOCOL_TIMEOUT) {
                Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(_rev);
                switch (protocolInfo.Command) {
                    case Command.ACK:
                        mKeyUpdateListener.result("키 갱신 완료", "", "SHOW", hashMap);
                        if(mBleOrSerial == "Serial" && Setting.ICResponseDeviceType==1&&!mPosSdk.getMultiAddr().equals(""))
                        {
                            mPosSdk.__SecurityKeyUpdateReady(mDataListener, new String[]{mPosSdk.getMultiAddr()});
                        }

                        break;
                    case Command.NAK:
                        if (mProductName.equals("################"))
                        {
                            mKeyUpdateListener.result("키 갱신 실패", "", "SHOW", hashMap);
                            if(mBleOrSerial == "Serial" && Setting.ICResponseDeviceType==1&&!mPosSdk.getMultiAddr().equals(""))
                            {
                                mPosSdk.__SecurityKeyUpdateReady(mDataListener, new String[]{mPosSdk.getMultiAddr()});
                            }
                        }
                        else
                        {
                            mKeyUpdateListener.result("키 갱신 실패", "", "SHOW", null);
//                            if(Setting.ICResponseDeviceType==1&&!mPosSdk.getMultiAddr().equals(""))
//                            {
//                                mPosSdk.__SecurityKeyUpdateReady(mDataListener, new String[]{mPosSdk.getMultiAddr()});
//                            }
//                            mKeyUpdateListener.result("키 갱신 실패", "", "SHOW", null);
                        }

                        break;
                    case Command.CMD_KEYUPDATE_READY_RES:
                        Res_SecurityKeyUpdateReady(_rev);
                        break;
                    case Command.CMD_SELFKEYCHECK_RES:
                        Res_Integrity(_rev);
                        break;
                }
            }

        }
    };

    private TcpInterface.DataListener mTcpDatalistener = new TcpInterface.DataListener() {
        @Override
        public void onRecviced(byte[] _rev) {
            final Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);

            switch (tp.getResponseCode())
            {
                case TCPCommand.CMD_SHOP_DOWNLOAD_RES:
                case TCPCommand.CMD_SHOPS_DOWNLOAD_RES:
                case TCPCommand.CMD_KEY_UPDATE_RES:
                        new Handler(Looper.getMainLooper()).post(()->{
                            hashMap = new HashMap<String, String>();
                            final List<byte[]> data = tp.getResData();
                            String code = new String(data.get(0));


                            String codeMessage = "";
                            String Message = "";
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
                                Message = Utils.getByteToString_euc_kr(data.get(2));
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
//                            hashMap.put("TrdType",TCPCommand.CMD_SHOP_DOWNLOAD_RES);
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
//                            hashMap.put("TermID",termID);
                            if (mBleOrSerial == "Serial") {
                                Res_Dealer_registration(_rev);
                            } else if (mBleOrSerial == "Ble") {
                                Res_BLEDealer_registration(_rev);
                            }
                        });

                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 무걸성 응답 함수
     * @param _rev
     */
    private synchronized void Res_Integrity(byte[] _rev)
    {
        KByteArray byteArray = new KByteArray(_rev);
//        byteArray.CutToSize(1);
//        String length = "24";
//        length = new String(byteArray.CutToSize(2));
//        int _length = Integer.parseInt(length);
//        byteArray.CutToSize(1);
//        String result = new String(byteArray.CutToSize(2));
//        String codeMessage = "";
//        try {
//            codeMessage = Utils.getByteToString_euc_kr(byteArray.CutToSize(_length - 4));
//        }catch (UnsupportedEncodingException ex)
//        {
//
//        }
//
//        codeMessage = result;

        byteArray.CutToSize(1);
        byteArray.CutToSize(2);
        byteArray.CutToSize(1);
        String result = new String(byteArray.CutToSize(2));
        int count = 0;
        for (int i=0; i<byteArray.getlength(); i++) {
            if (byteArray.indexData(i) == (byte)0x03) {
                count = i;
            }
        }

        String codeMessage = "";
        try {
            if (count == 0) {
                if (byteArray.getlength() > 2) {
                    codeMessage = Utils.getByteToString_euc_kr(byteArray.CutToSize(byteArray.getlength() - 2));
                } else {
                    codeMessage = "";
                }
            } else {
                codeMessage = Utils.getByteToString_euc_kr(byteArray.CutToSize(count));
            }

        }catch (UnsupportedEncodingException ex)
        {

        }

        codeMessage = result;

        Setting.IntegrityResult = result.equals("00")?true:false;       //무결성 성공 실패 여부를 설정에 저장하고 메인 화면에서 신용/현금 결제 버튼을 활성 비활성화 처리 한다.
        mKeyUpdateListener.result(codeMessage,"1","SHOW",null);
    }
    private synchronized void Res_Dealer_registration(byte[] _res)
    {
        Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_res);
        final List<byte[]> data = tp.getResData();
        String code = new String(data.get(0));

        if(code.equals("0000"))
        {
            if(data.get(2).length < 4)
            {
                mKeyUpdateListener.result("서버 응답 데이터 오류 발생",code , "SHOW",null);
                return;
            }

            KByteArray ba = new KByteArray(_res);
            ba.CutToSize(76-23);
            byte[] resData = new byte[48];
            while (true)
            {
                byte[] tmp;
                tmp = ba.CutToSize(1);
                if(tmp[0] == Command.FS)
                {
                    break;
                }
            }
            ba.CutToSize(4);
            resData = ba.CutToSize(48);

            if (mBleOrSerial == "Serial") {
                if(!Setting.getPreference(mCtx, Constants.SELECTED_DEVICE_CARD_READER).equals(""))
                {
                    if(Setting.ICResponseDeviceType==1)
                    {
                        mPosSdk.__SecurityKeyUpdate("20" + tp.getDate(),resData,mDataListener,mPosSdk.getICReaderAddr2());
                    }
                    else
                    {
                        mPosSdk.__SecurityKeyUpdate("20" + tp.getDate(),resData,mDataListener,new String[]{mPosSdk.getMultiAddr()});
                    }
                }
                else
                {
                    mPosSdk.__SecurityKeyUpdate("20" + tp.getDate(),resData,mDataListener,mPosSdk.getMultiReaderAddr2());
                }
            } else if (mBleOrSerial == "Ble") {
                if (m_bletimer != null)
                {
                    m_bletimer.cancel();
                    m_bletimer = null;
                }
                m_bletimer = new BleIntegTimer(10 * 1000, 1000);
                m_bletimer.start();
                mPosSdk.__BLESecurityKeyUpdate("20" + tp.getDate(),resData,mBleDataListener);
            }






//            //IPEK 삭제______________________________________________
//            Random rand = new Random();
//            if(resData!=null)
//            {
//                for(int i=0;i<resData.length;i++)
//                {
//                    resData[i] = (byte)rand.nextInt(255);
//                }
//                Arrays.fill(resData,(byte)0x01);
//                Arrays.fill(resData,(byte)0x00);
//            }
//            resData = null;
//            ba.Clear();
//            //_______________________________________________________
//            //res데이터 삭제_______________________________________________________
//            if(_res!=null)
//            {
//                for(int i=0;i<_res.length;i++)
//                {
//                    _res[i] = (byte)rand.nextInt(255);
//                }
//                Arrays.fill(_res,(byte)0x01);
//                Arrays.fill(_res,(byte)0x00);
//            }
//            _res = null;
            //_______________________________________________________
        }
        else
        {
            String codeMessage = "";
            try {
                codeMessage = Utils.getByteToString_euc_kr(data.get(1));
            }catch (UnsupportedEncodingException ex)
            {

            }

//            //res데이터 삭제_______________________________________________________
//            Random rand = new Random();
//            if(_res!=null)
//            {
//                for(int i=0;i<_res.length;i++)
//                {
//                    _res[i] = (byte)rand.nextInt(255);
//                }
//                Arrays.fill(_res,(byte)0x01);
//                Arrays.fill(_res,(byte)0x00);
//            }
//            _res = null;
            //_______________________________________________________
//            mKeyUpdateListener.result(codeMessage,"보안키 생성 에러 발생","SHOW",null);

            KByteArray ba = new KByteArray(_res);
            ba.CutToSize(76-23);
            byte[] resData = new byte[48];
            while (true)
            {
                byte[] tmp;
                tmp = ba.CutToSize(1);
                if(tmp[0] == Command.FS)
                {
                    break;
                }
            }
            ba.CutToSize(4);
            resData = ba.CutToSize(48);

            if (resData == null) {
                resData = new byte[48];
                for(int i=0; i<48; i++) {
                    resData[i] = (byte)0x20;
                }
            }


            if (mBleOrSerial == "Serial") {
                if(!Setting.getPreference(mCtx, Constants.SELECTED_DEVICE_CARD_READER).equals(""))
                {
                    if(Setting.ICResponseDeviceType==1)
                    {
                        mPosSdk.__SecurityKeyUpdate("20" + tp.getDate(),resData,mDataListener,mPosSdk.getICReaderAddr2());
                    }
                    else
                    {
                        mPosSdk.__SecurityKeyUpdate("20" + tp.getDate(),resData,mDataListener,new String[]{mPosSdk.getMultiAddr()});
                    }
                }
                else
                {
                    mPosSdk.__SecurityKeyUpdate("20" + tp.getDate(),resData,mDataListener,mPosSdk.getMultiReaderAddr2());
                }
            } else if (mBleOrSerial == "Ble") {
                if (m_bletimer != null)
                {
                    m_bletimer.cancel();
                    m_bletimer = null;
                }
                m_bletimer = new BleIntegTimer(10 * 1000, 1000);
                m_bletimer.start();
                mPosSdk.__BLESecurityKeyUpdate("20" + tp.getDate(),resData,mBleDataListener);
            }

        }


    }

    /**
     * 보안키 업데이트 시작 응답
     * @param _rev
     */
    private void Res_SecurityKeyUpdateReady(byte[] _rev)
    {
        KByteArray bArray = new KByteArray(_rev);
        bArray.CutToSize(4);    //STX + 길이 + CommandID 삭제
        String authNumber = new String(bArray.CutToSize(32));
        final byte[] data = bArray.CutToSize(128);
        String result = new String(bArray.CutToSize(2));
        if (mTid.equals("")) {
            mStoreNumber = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.STORE_NO);
            mTid = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.TID);
            mSerialNumber = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.REGIST_DEVICE_SN);
        }

        if (mTid.equals("")){
            mTid = Constants.TEST_TID;
        }

//        Random rand = new Random();
//        rand = new Random();
//        if(_rev!=null)
//        {
//            for(int i=0;i<_rev.length;i++)
//            {
//                _rev[i] = (byte)rand.nextInt(255);
//            }
//            Arrays.fill(_rev,(byte)0x01);
//            Arrays.fill(_rev,(byte)0x00);
//        }
        // 키업데이트 시 거절되었을 때
        if(result.equals("01"))
        {
            mKeyUpdate++;
            if(mKeyUpdate == 1)
            {
                Req_SecurityKeyUpdate();
            }
            else if(mKeyUpdate == 2)
            {
                mKeyUpdate = 0;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mKeyUpdateListener.result("보안키 갱신 생성 결과 비정상",result,"ERROR",null);
                        bArray.Clear();

                    }
                });
                return;
            }

        }
        else if(result.equals("00"))
        {
            String finalMTid = mTid;
            new Thread(new Runnable() {
                @Override
                public void run() {


                    mPosSdk.___registedShopInfo_KeyDownload(TCPCommand.CMD_KEY_UPDATE_REQ, finalMTid, Utils.getDate("yyMMddHHmmss"),
                            Constants.TEST_SOREWAREVERSION,"","0128",data, mStoreNumber,mSerialNumber,"",Utils.getMacAddress(mCtx),mTcpDatalistener);

//                    mPosSdk.___registedShopInfo_KeyDownload(TCPCommand.CMD_REGISTERED_SHOP_DOWNLOAD_REQ,mTid, Utils.getDate("yyMMddHHmmss"),
//                            Constants.TEST_SOREWAREVERSION,"","0128",data,mStoreNumber,mSerialNumber,"",Utils.getMacAddress(mCtx),mTcpDatalistener);

//                    Random rand = new Random();
//                    rand = new Random();
//                    if(data!=null)
//                    {
//                        for(int i=0;i<data.length;i++)
//                        {
//                            data[i] = (byte)rand.nextInt(255);
//                        }
//                        Arrays.fill(data,(byte)0x01);
//                        Arrays.fill(data,(byte)0x00);
//                        bArray.Clear();
//
//
//                    }
                }
            }).start();

        }

    }


    /**
     * 여기서부터 BLE 무결성검사
     */
    int _count = 0;
    //이상하게 우심에서 한번 더 분기를 타고 있다 원인 아직 미파악
//    int mWSP_Integrity = 0;
    private bleSdkInterface.ResDataListener mBleDataListener = res -> {
        Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);

        if (m_bletimer != null)
        {
            m_bletimer.cancel();
            m_bletimer = null;
        }

        if(   Setting.getBleName().contains(Constants.C1_KRE_OLD_USE_PRINT) ||
                Setting.getBleName().contains(Constants.C1_KRE_OLD_NOT_PRINT) ||
                Setting.getBleName().contains(Constants.C1_KRE_NEW)) {
            _count += 1;
            if (_count < 2) {
                return;
            }
            _count = 0;
        }
//        else if (Setting.getBleName().contains(Constants.WSP) ||
//                Setting.getBleName().contains(Constants.WOOSIM)) {
//            if (protocolInfo.Command == Command.CMD_KEYUPDATE_READY_RES ||
//                    protocolInfo.Command == Command.CMD_SELFKEYCHECK_RES) {
//                _count = 0;
//                mWSP_Integrity = 0;
//            } else {
//                if (mWSP_Integrity == 1 && protocolInfo.Command == Command.ACK) {
//                    _count += 1;
//                    if (_count < 2) {
//                        return;
//                    }
//                    _count = 0;
//                }
//            }
//        }

        switch (protocolInfo.Command) {
            case Command.ACK:
                mBLEKeyUpdateListener.result("키 갱신 완료", "", "SHOW", hashMap);
                break;
            case Command.NAK:
                _count = 0;
//                mWSP_Integrity = 0;
                mBLEKeyUpdateListener.result("키 갱신 실패", "", "SHOW", hashMap);
                break;
            case Command.CMD_KEYUPDATE_READY_RES:
                Res_BLESecurityKeyUpdateReady(res);
                break;
            case Command.CMD_SELFKEYCHECK_RES:
                Res_BLEIntegrity(res);
                break;
        }
    };

    /**
     * 초기화 함수
     * @param _ctx
     * @param _mBLEKeyUpdateListener
     */
    public DeviceSecuritySDK(Context _ctx, bleSdkInterface.BLEKeyUpdateListener _mBLEKeyUpdateListener)
    {
        mPosSdk = KocesPosSdk.getInstance();
        mbleSdk = bleSdk.getInstance();
        mBLEKeyUpdateListener = _mBLEKeyUpdateListener;
        mCtx = _ctx;
        mBleOrSerial = "Ble";
    }


    /**
     * BLE 키 갱신 요청 함수
     */
    public void Req_BLEIntegrity() {
        mPosSdk.BleIsConnected();
        if (Setting.getBleIsConnected()) {
//            mPosSdk.__BLEDeviceInit(null, "99");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    LastCommand = Command.CMD_SELFKEYCHECK_REQ;
                    if (m_bletimer != null)
                    {
                        m_bletimer.cancel();
                        m_bletimer = null;
                    }
                    m_bletimer = new BleIntegTimer(10 * 1000, 1000);
                    m_bletimer.start();
//                    mWSP_Integrity = 1;
                    mPosSdk.__BLEGetVerity(mBleDataListener);
                }
            }, 1000);
        } else {
            if (m_bletimer != null)
            {
                m_bletimer.cancel();
                m_bletimer = null;
            }
            mBLEKeyUpdateListener.result("BLE 장비가 연결되어 있지 않습니다","01" , "ERROR",null);
        }
    }

    /**
     * BLE 보안키 업데이트 시작 응답
     * @param _rev
     */
    private void Res_BLESecurityKeyUpdateReady(byte[] _rev)
    {
        KByteArray bArray = new KByteArray(_rev);
        bArray.CutToSize(4);    //STX + 길이 + CommandID 삭제
        String authNumber = new String(bArray.CutToSize(32));
        final byte[] data = bArray.CutToSize(128);
        String result = new String(bArray.CutToSize(2));

        if (mTid.equals("")) {
            mStoreNumber = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.STORE_NO);
            mTid = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.TID);
            mSerialNumber = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.REGIST_DEVICE_SN);
        }
        if (mTid.equals("")){
            mTid = Constants.TEST_TID;
        }

        // 키업데이트 시 거절되었을 때
        if(result.equals("01"))
        {
            mKeyUpdate++;
            if(mKeyUpdate == 1)
            {
                Req_BLESecurityKeyUpdate();
            }
            else if(mKeyUpdate == 2)
            {
                mKeyUpdate = 0;
                if (m_bletimer != null)
                {
                    m_bletimer.cancel();
                    m_bletimer = null;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mBLEKeyUpdateListener.result("보안키 갱신 생성 결과 비정상",result,"ERROR",null);
                        bArray.Clear();

                    }
                });
                return;
            }

        }
        else if(result.equals("00"))
        {
            String finalMTid = mTid;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPosSdk.___registedShopInfo_KeyDownload(TCPCommand.CMD_KEY_UPDATE_REQ, finalMTid, Utils.getDate("yyMMddHHmmss"),
                            Constants.TEST_SOREWAREVERSION,"","0128",data, mStoreNumber,mSerialNumber,"",Utils.getMacAddress(mCtx),mTcpDatalistener);
                }
            },500);


        }

    }

    /**
     * BLE 보안키 업데이트 요청 함수
     */
    public void Req_BLESecurityKeyUpdate()
    {
        if (m_bletimer != null)
        {
            m_bletimer.cancel();
            m_bletimer = null;
        }
        m_bletimer = new BleIntegTimer(10 * 1000, 1000);
        m_bletimer.start();
        mPosSdk.__BLESecurityKeyUpdateReady(mBleDataListener);

    }

    public void Req_BLESecurityKeyUpdate(String _tid, String _bsn, String _serial)
    {
        if (m_bletimer != null)
        {
            m_bletimer.cancel();
            m_bletimer = null;
        }
        mTid = _tid;
        mSerialNumber = _serial;
        mStoreNumber = _bsn;
        m_bletimer = new BleIntegTimer(10 * 1000, 1000);
        m_bletimer.start();
        mPosSdk.__BLESecurityKeyUpdateReady(mBleDataListener);

    }

    /**
     * BLE 무걸성 응답 함수
     * @param _rev
     */
    private synchronized void Res_BLEIntegrity(byte[] _rev)
    {
        KByteArray byteArray = new KByteArray(_rev);
        byteArray.CutToSize(1);
        byteArray.CutToSize(2);
        byteArray.CutToSize(1);
        String result = new String(byteArray.CutToSize(2));
        int count = 0;
        for (int i=0; i<byteArray.getlength(); i++) {
            if (byteArray.indexData(i) == (byte)0x03) {
                count = i;
            }
        }

        String codeMessage = "";
        try {
            if (count == 0) {
                if (byteArray.getlength() > 2) {
                    codeMessage = Utils.getByteToString_euc_kr(byteArray.CutToSize(byteArray.getlength() - 2));
                } else {
                    codeMessage = "";
                }
            } else {
                codeMessage = Utils.getByteToString_euc_kr(byteArray.CutToSize(count));
            }

        }catch (UnsupportedEncodingException ex)
        {

        }

        codeMessage = result;

        Setting.IntegrityResult = result.equals("00")?true:false;       //무결성 성공 실패 여부를 설정에 저장하고 메인 화면에서 신용/현금 결제 버튼을 활성 비활성화 처리 한다.
        if (m_bletimer != null)
        {
            m_bletimer.cancel();
            m_bletimer = null;
        }
        mBLEKeyUpdateListener.result(codeMessage,"1","SHOW",null);
    }

    private synchronized void Res_BLEDealer_registration(byte[] _res)
    {
        Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_res);
        final List<byte[]> data = tp.getResData();
        String code = new String(data.get(0));

        if(code.equals("0000"))
        {
            if(data.get(2).length < 4)
            {
                if (m_bletimer != null)
                {
                    m_bletimer.cancel();
                    m_bletimer = null;
                }
                mBLEKeyUpdateListener.result("서버 응답 데이터 오류 발생",code , "SHOW",null);
                return;
            }

            KByteArray ba = new KByteArray(_res);
            ba.CutToSize(76-23);
            byte[] resData = new byte[48];
            while (true)
            {
                byte[] tmp;
                tmp = ba.CutToSize(1);
                if(tmp[0] == Command.FS)
                {
                    break;
                }
            }
            ba.CutToSize(4);
            resData = ba.CutToSize(48);

            if (m_bletimer != null)
            {
                m_bletimer.cancel();
                m_bletimer = null;
            }
            m_bletimer = new BleIntegTimer(10 * 1000, 1000);
            m_bletimer.start();
            mPosSdk.__BLESecurityKeyUpdate("20" + tp.getDate(),resData,mBleDataListener);


//            //IPEK 삭제______________________________________________
//            Random rand = new Random();
//            if(resData!=null)
//            {
//                for(int i=0;i<resData.length;i++)
//                {
//                    resData[i] = (byte)rand.nextInt(255);
//                }
//                Arrays.fill(resData,(byte)0x01);
//                Arrays.fill(resData,(byte)0x00);
//            }
//            resData = null;
//            ba.Clear();
//            //_______________________________________________________
//            //res데이터 삭제_______________________________________________________
//            if(_res!=null)
//            {
//                for(int i=0;i<_res.length;i++)
//                {
//                    _res[i] = (byte)rand.nextInt(255);
//                }
//                Arrays.fill(_res,(byte)0x01);
//                Arrays.fill(_res,(byte)0x00);
//            }
//            _res = null;
            //_______________________________________________________
        }
        else
        {
            String codeMessage = "";
            try {
                codeMessage = Utils.getByteToString_euc_kr(data.get(1));
            }catch (UnsupportedEncodingException ex)
            {

            }

//            //res데이터 삭제_______________________________________________________
//            Random rand = new Random();
//            if(_res!=null)
//            {
//                for(int i=0;i<_res.length;i++)
//                {
//                    _res[i] = (byte)rand.nextInt(255);
//                }
//                Arrays.fill(_res,(byte)0x01);
//                Arrays.fill(_res,(byte)0x00);
//            }
//            _res = null;
            //_______________________________________________________
//            mBLEKeyUpdateListener.result(codeMessage,"보안키 생성 에러 발생","SHOW",null);


            KByteArray ba = new KByteArray(_res);
            ba.CutToSize(76-23);
            byte[] resData = new byte[48];
            while (true)
            {
                byte[] tmp;
                tmp = ba.CutToSize(1);
                if(tmp[0] == Command.FS)
                {
                    break;
                }
            }
            ba.CutToSize(4);
            resData = ba.CutToSize(48);

            if (resData == null) {
                resData = new byte[48];
                for(int i=0; i<48; i++) {
                    resData[i] = (byte)0x20;
                }
            }

            if (m_bletimer != null)
            {
                m_bletimer.cancel();
                m_bletimer = null;
            }
            m_bletimer = new BleIntegTimer(10 * 1000, 1000);
            m_bletimer.start();
            mPosSdk.__BLESecurityKeyUpdate("20" + tp.getDate(),resData,mBleDataListener);
        }

    }

    class BleIntegTimer extends CountDownTimer
    {
        public BleIntegTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d("DeviceSecuritySdk","BleTimeOutCount : " + millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            mBLEKeyUpdateListener.result("9999","타임아웃","ERROR",null);
        }
    }
}
