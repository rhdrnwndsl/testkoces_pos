package com.koces.androidpos.sdk.van;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.TCPCommand;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.log.LogFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

//테스트 사용 정보
// 1) 단말기번호: 0710000900
// 2) 사업자번호: 2148631917
// 3) 시리얼번호: 1000000007
// 4) 소프트웨어버전: 07201

public class KocesTcpClient {
    private final static String TAG ="TCPClient";
    private static String mServerIP;
    private static int mServerPort;
    private static String mCatServerIP;
    private static int mCatServerPort;
    private static int mTMPPort;
    private KocesTcpClient instance;
    public Socket msocket;
    private KByteArray mBuffer;
    public DataInputStream networkReader;
    public DataOutputStream networkWriter;
    private Handler mhandler;
    private NetworkInterface.ConnectListener mNetworkConnectListener;
    private CatNetworkInterface.ConnectListener mCatNetworkConnectListener;
    private TmpNetworkInterface.ConnectListener mTmpNetworkConnectListener;
    private int TCPPROCEDURE;
    private int mNakCount;
    public KocesTcpClient(String _ip,int _port,String _catip,int _catport,int _tmpport,Handler _handler,NetworkInterface.ConnectListener _networkConnectListener)
    {
        instance = this;
        mServerIP = _ip;
        mServerPort = _port;
        mCatServerIP = _catip;
        mCatServerPort = _catport;
        mTMPPort = _tmpport;
        mhandler = _handler;
        mBuffer = new KByteArray();
        mNetworkConnectListener = _networkConnectListener;
    }
    public KocesTcpClient getInstance()
    {
        if(instance==null)
        {
            return null;
        }
        //여기서 서버 연결 상태를 체크해서 재 연결 여부를 결정 한다.
        //아니면 메세지 보내기 직전에 무조건 서버 연결을 시도 한다.
        return instance;
    }

    /**
     * 서버 ip, port 다시 설정 한다.
     * @param _ip
     * @param _port
     */
    public void reSetIP(String _ip,int _port)
    {
        mServerIP = _ip;
        mServerPort = _port;
    }

    public void reSetCatIP(String _ip,int _port)
    {
        mCatServerIP = _ip;
        mCatServerPort = _port;
    }

    public boolean connect()
    {

        try{
            boolean bResult = setSocket(mServerIP,mServerPort);
            if(!bResult)
            {
                return false;
            }

        } catch (UnknownHostException uhe) {
            // 소켓 생성 시 전달되는 호스트(mServerIP)의 IP를 식별할 수 없음.
            Log.d(TAG,"호스트(mServerIP)의 IP를 식별할 수 없음");
            Setting.setTcpServerConnectionState(false);
            return false;
        } catch (IOException ioe) {
            // 소켓 생성 과정에서 I/O 에러 발생.
            Log.d(TAG,"소켓 생성 과정에서 I/O 에러 발생");
            Setting.setTcpServerConnectionState(false);
            ioe.printStackTrace();
            return false;

        } catch (SecurityException se) {
            // security manager에서 허용되지 않은 기능 수행.
            Log.d(TAG,"security manager에서 허용되지 않은 기능 수행");
            Setting.setTcpServerConnectionState(false);
            return false;
        } catch (IllegalArgumentException ex) {
            // 소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남.
            Log.d(TAG,"소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남");
            Setting.setTcpServerConnectionState(false);
            return false;
        }
        return true;
    }

    boolean bfinish = false;
    public void write(byte[] b)
    {
        //TODO: 만일 네트워크상의 일 등으로 서버로부터 응답값을 받지 못했거나 데이터전송을 미완료 했다면 여기서 쓰레기 값들이 남아있을 수 있으니 이를 지우고 시작한다. 이전에 Buffer.clear()
        // 만으로 부족할까봐 지운다. 22-12-09. by jiw
        Dispose();
        bfinish = true;

        //인증용 로그 찍기
        Log.d("KocesPacketData","[POS -> TCP SERVER]");
        Log.d("KocesPacketData",Utils.bytesToHex_0xType(b));
        //초기화 작업 해야 한다.
        final byte[] bSendData = b;
        TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_NONE;
        mNakCount = 0;


        new Thread(){
            public void run()
            {
//                if(msocket==null)
//                {
//                    mNetworkConnectListener.onState(-1,false, "네트워크 연결 에러");
//                    return;
//                }

//                if(Setting.getTcpServerConnectionState()==false || msocket.isConnected()==false)
//                {
                    if (!connect()) {
                        mNetworkConnectListener.onState(-1,false, "내부 TCP 초기화 및 연결 에러 발생");
                        return;
                    }
//                }

                //TODO: 만일 네트워크상의 일 등으로 서버로부터 응답값을 받지 못했거나 데이터전송을 미완료 했다면 여기서 쓰레기 값들이 남아있을 수 있으니 이를 지우고 시작한다. 이전에 Buffer.clear()
                // 만으로 부족할까봐 지운다. 22-12-09. by jiw
//                try {
//                    mBuffer.Clear();
//                    networkWriter.flush();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                byte[] buff = new byte[4096];
                bfinish = false;

                while (!bfinish) {
                    try {
                        buff = new byte[4096];
                        setDataSendRecvTimeout(TCPPROCEDURE);

                        int Count = networkReader.read(buff);
                        if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
                        {
                            Dispose();
                            return;
                        }
                        if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND)     //Van 응답 전문
                        {
                            int bufferState;
                            byte[] tmp = new byte[Count];
                            System.arraycopy(buff, 0, tmp, 0, Count);
                            mBuffer.Add(tmp);
                            bufferState = CheckProtocol();
                            if(bufferState==1)  //데이터가 모두 정상으로 들어 옮
                            {
                                buff[0]=(byte)0x11;
                            }
                            if(bufferState==0)  //데이터를 더 받아야 함.
                            {
                                buff[0]=(byte)0x12;
                            }
                            if(bufferState==-2)  //CheckLRC가 맞지 않음..
                            {
                                Message message = new Message();
                                message.what = 2003;
                                message.obj =null;
                                mhandler.sendMessage(message);
                                Setting.setEOTResult(0);
                                mCountDownTimer.cancel();
                                bfinish = true;

                                //TODO:혹시 버퍼가 비워지지 않아서 생긴 문제일까 하여 버퍼를 클리어해준다 22-06-13. by jiw
                                mBuffer.Clear();
//                                buff[0]=(byte)0x13;
                            }
                            if(bufferState==-3) //NAK
                            {
//                                Message message = new Message();
//                                message.what = 2004;
//                                message.obj =null;
//                                mhandler.sendMessage(message);
//                                Setting.setEOTResult(0);
//                                mCountDownTimer.cancel();
//                                bfinish = true;
                            }
                        }
                        switch (buff[0]) {
                            case Command.EOT:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_EOT;
                                Setting.setEOTResult(1);
                                mCountDownTimer.cancel();
                                bfinish = true;
//                                Dispose();

                                //TODO:혹시 버퍼가 비워지지 않아서 생긴 문제일까 하여 버퍼를 클리어해준다 22-06-13. by jiw
                                mBuffer.Clear();
                                Arrays.fill(buff,(byte)0x01);
                                Arrays.fill(buff,(byte)0x00);
                                break;
                            case Command.ENQ:
                                if (TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_NONE) {
                                    //Log.d(TAG, "Server Send -> ENQ");
                                    TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_INIT;
                                }
                                break;
                            case Command.NAK:
                                //나중에 삭제 필요
                                //Log.d(TAG, "Server Send -> NAK");
                                mNakCount++;
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_RESEND;
                                if (mNakCount == 2) {
                                    Message message = new Message();
                                    message.what = 2004;
                                    message.obj =null;
                                    mhandler.sendMessage(message);
                                    Setting.setEOTResult(0);
                                    mCountDownTimer.cancel();
                                    bfinish = true;
//                                    Dispose();

                                    //TODO:혹시 버퍼가 비워지지 않아서 생긴 문제일까 하여 버퍼를 클리어해준다 22-06-13. by jiw
                                    mBuffer.Clear();
                                    Arrays.fill(buff,(byte)0x01);
                                    Arrays.fill(buff,(byte)0x00);
                                } //Nak인 경우에는 3회 반복 되면 종료 한다.
                                break;
                            case (byte)0x12:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_DATA;
                                break;
                            case (byte)0x11:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND_ACK;
                                break;
                            case (byte)0x13:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND_NAK;
                                break;
                            default:
                                bfinish = true;
                                mCountDownTimer.cancel();
                                Arrays.fill(buff,(byte)0x01);
                                Arrays.fill(buff,(byte)0x00);
                                //모든 작업이 종료 되면 네트워크 연결을 종료 한다.
                                Dispose();
                                break;
                        }

                        if (TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_RESEND ||
                                TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_INIT ||
                                TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_SEND_ACK ||
                                TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_SEND_NAK) {
                            boolean sendComplete = false;
                            int sendCount = 0;
                            while (!sendComplete) {

                                try {
                                    if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND_ACK)
                                    {
                                        if(msocket.isConnected()==false)
                                        {
                                            sendComplete = true;
                                            bfinish = true;
                                        }

                                        networkWriter = new DataOutputStream(msocket.getOutputStream());
                                        byte[] SendAck = {Command.ACK,Command.ACK,Command.ACK };
                                        networkWriter.write(SendAck, sendCount, SendAck.length - sendCount);
                                        //Log.d(TAG, "client Send data size => " + networkWriter.size());
                                        sendCount += networkWriter.size();
                                        if (sendCount == SendAck.length) {
                                            networkWriter.flush();
                                            sendComplete = true;
                                            Arrays.fill(buff,(byte)0x01);
                                            Arrays.fill(buff,(byte)0x00);
                                            String mLog = "데이터사이즈 = " + sendCount +  ", 데이터 = ";
                                            cout("SEND_SERVER:APP->SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(SendAck));
                                        }
                                    }
                                    else if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND_NAK)
                                    {
                                        if(msocket.isConnected()==false)
                                        {
                                            sendComplete = true;
                                            bfinish = true;
                                        }
                                        networkWriter = new DataOutputStream(msocket.getOutputStream());
                                        byte[] SendNak = {Command.NAK };
                                        networkWriter.write(SendNak, sendCount, SendNak.length - sendCount);
                                        //Log.d(TAG, "client Send data size => " + networkWriter.size());
                                        sendCount += networkWriter.size();
                                        if (sendCount == SendNak.length) {
                                            networkWriter.flush();
                                            sendComplete = true;
                                            Arrays.fill(buff,(byte)0x01);
                                            Arrays.fill(buff,(byte)0x00);
                                            String mLog = "데이터사이즈 = " + sendCount +  ", 데이터 = ";
                                            cout("SEND_SERVER:APP->SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(SendNak));
                                        }
                                    }
                                    else {
                                        //데이터를 보내는 부분
                                        if(msocket.isConnected()==false)
                                        {
                                            sendComplete = true;
                                            bfinish = true;
                                        }
                                        networkWriter = new DataOutputStream(msocket.getOutputStream());
                                        networkWriter.write(bSendData, sendCount, bSendData.length - sendCount);
                                        //Log.d(TAG, "client Send data size => " + networkWriter.size());
                                        sendCount += networkWriter.size();
                                        if(sendCount == bSendData.length) {
                                            sendComplete = true;
                                            TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND;
                                            networkWriter.flush();
                                            String mLog = "데이터사이즈 = " + sendCount +  ", 데이터 = ";
                                            cout("SEND_SERVER:APP->SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(bSendData));
                                            Random rand = new Random();
                                            if(bSendData!=null)
                                            {
                                                for(int i=0;i<bSendData.length;i++)
                                                {
                                                    bSendData[i] = (byte)rand.nextInt(255);
                                                }
                                                Arrays.fill(bSendData,(byte)0x01);
                                                Arrays.fill(bSendData,(byte)0x00);
                                            }
                                            Arrays.fill(buff,(byte)0x01);
                                            Arrays.fill(buff,(byte)0x00);
                                        }


                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                    mNetworkConnectListener.onState(0,false, "데이터 전송중 에러 발생");
                                    bfinish = true;

                                    //TODO jiw.20.06.29 ic 거래중 lrc 오류  및 nak 송신 시 sendcomplete 를 하여야 해당 루프에서 빠져나올 수 있었다.
                                    sendComplete = true;
                                    Arrays.fill(bSendData,(byte)0x01);
                                    Arrays.fill(bSendData,(byte)0x00);
                                    Arrays.fill(buff,(byte)0x01);
                                    Arrays.fill(buff,(byte)0x00);
//                                    Dispose();
                                }


                                if(sendComplete)
                                {
                                    sendCount = 0;
                                    Arrays.fill(bSendData,(byte)0x01);
                                    Arrays.fill(bSendData,(byte)0x00);
                                    Arrays.fill(buff,(byte)0x01);
                                    Arrays.fill(buff,(byte)0x00);
                                }
                            }

                        }
                    } catch (IOException ex) {

                    }
                }

                Dispose();
            }
        }.start();
    }

    public boolean connectTMP()
    {

        try{
            boolean bResult = setSocket(mServerIP,mTMPPort);
            if(!bResult)
            {
                return false;
            }

        } catch (UnknownHostException uhe) {
            // 소켓 생성 시 전달되는 호스트(mServerIP)의 IP를 식별할 수 없음.
            Log.d(TAG,"호스트(mServerIP)의 IP를 식별할 수 없음");
            Setting.setTmpTcpServerConnectionState(false);
            return false;
        } catch (IOException ioe) {
            // 소켓 생성 과정에서 I/O 에러 발생.
            Log.d(TAG,"소켓 생성 과정에서 I/O 에러 발생");
            Setting.setTmpTcpServerConnectionState(false);
            ioe.printStackTrace();
            return false;

        } catch (SecurityException se) {
            // security manager에서 허용되지 않은 기능 수행.
            Log.d(TAG,"security manager에서 허용되지 않은 기능 수행");
            Setting.setTmpTcpServerConnectionState(false);
            return false;
        } catch (IllegalArgumentException ex) {
            // 소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남.
            Log.d(TAG,"소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남");
            Setting.setTmpTcpServerConnectionState(false);
            return false;
        }
        return true;
    }
    public void writeTMP(byte[] b)
    {
        //TODO: 만일 네트워크상의 일 등으로 서버로부터 응답값을 받지 못했거나 데이터전송을 미완료 했다면 여기서 쓰레기 값들이 남아있을 수 있으니 이를 지우고 시작한다. 이전에 Buffer.clear()
        // 만으로 부족할까봐 지운다. 22-12-09. by jiw
        Dispose();
        bfinish = true;

        //인증용 로그 찍기
        Log.d("KocesPacketData","[POS -> TCP SERVER]");
        Log.d("KocesPacketData",Utils.bytesToHex_0xType(b));
        //초기화 작업 해야 한다.
        final byte[] bSendData = b;
        TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_NONE;
        mNakCount = 0;


        new Thread(){
            public void run()
            {
//                if(msocket==null)
//                {
//                    mNetworkConnectListener.onState(-1,false, "네트워크 연결 에러");
//                    return;
//                }

//                if(Setting.getTcpServerConnectionState()==false || msocket.isConnected()==false)
//                {
                if (!connectTMP()) {
                    mNetworkConnectListener.onState(-1,false, "내부 TCP 초기화 및 연결 에러 발생");
                    return;
                }
//                }

                //TODO: 만일 네트워크상의 일 등으로 서버로부터 응답값을 받지 못했거나 데이터전송을 미완료 했다면 여기서 쓰레기 값들이 남아있을 수 있으니 이를 지우고 시작한다. 이전에 Buffer.clear()
                // 만으로 부족할까봐 지운다. 22-12-09. by jiw
//                try {
//                    mBuffer.Clear();
//                    networkWriter.flush();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                byte[] buff = new byte[4096];
                bfinish = false;

                while (!bfinish) {
                    try {
                        buff = new byte[4096];
                        setDataSendRecvTimeout(TCPPROCEDURE);

                        int Count = networkReader.read(buff);
                        if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
                        {
                            Dispose();
                            return;
                        }
                        if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND)     //Van 응답 전문
                        {
                            int bufferState;
                            byte[] tmp = new byte[Count];
                            System.arraycopy(buff, 0, tmp, 0, Count);
                            mBuffer.Add(tmp);
                            bufferState = CheckProtocol();
                            if(bufferState==1)  //데이터가 모두 정상으로 들어 옮
                            {
                                buff[0]=(byte)0x11;
                            }
                            if(bufferState==0)  //데이터를 더 받아야 함.
                            {
                                buff[0]=(byte)0x12;
                            }
                            if(bufferState==-2)  //CheckLRC가 맞지 않음..
                            {
                                Message message = new Message();
                                message.what = 2003;
                                message.obj =null;
                                mhandler.sendMessage(message);
                                Setting.setEOTResult(0);
                                mCountDownTimer.cancel();
                                bfinish = true;

                                //TODO:혹시 버퍼가 비워지지 않아서 생긴 문제일까 하여 버퍼를 클리어해준다 22-06-13. by jiw
                                mBuffer.Clear();
//                                buff[0]=(byte)0x13;
                            }
                            if(bufferState==-3) //NAK
                            {
//                                Message message = new Message();
//                                message.what = 2004;
//                                message.obj =null;
//                                mhandler.sendMessage(message);
//                                Setting.setEOTResult(0);
//                                mCountDownTimer.cancel();
//                                bfinish = true;
                            }
                        }
                        switch (buff[0]) {
                            case Command.EOT:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_EOT;
                                Setting.setEOTResult(1);
                                mCountDownTimer.cancel();
                                bfinish = true;
//                                Dispose();

                                //TODO:혹시 버퍼가 비워지지 않아서 생긴 문제일까 하여 버퍼를 클리어해준다 22-06-13. by jiw
                                mBuffer.Clear();
                                Arrays.fill(buff,(byte)0x01);
                                Arrays.fill(buff,(byte)0x00);
                                break;
                            case Command.ENQ:
                                if (TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_NONE) {
                                    //Log.d(TAG, "Server Send -> ENQ");
                                    TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_INIT;
                                }
                                break;
                            case Command.NAK:
                                //나중에 삭제 필요
                                //Log.d(TAG, "Server Send -> NAK");
                                mNakCount++;
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_RESEND;
                                if (mNakCount == 2) {
                                    Message message = new Message();
                                    message.what = 2004;
                                    message.obj =null;
                                    mhandler.sendMessage(message);
                                    Setting.setEOTResult(0);
                                    mCountDownTimer.cancel();
                                    bfinish = true;
//                                    Dispose();

                                    //TODO:혹시 버퍼가 비워지지 않아서 생긴 문제일까 하여 버퍼를 클리어해준다 22-06-13. by jiw
                                    mBuffer.Clear();
                                    Arrays.fill(buff,(byte)0x01);
                                    Arrays.fill(buff,(byte)0x00);
                                } //Nak인 경우에는 3회 반복 되면 종료 한다.
                                break;
                            case (byte)0x12:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_DATA;
                                break;
                            case (byte)0x11:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND_ACK;
                                break;
                            case (byte)0x13:
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND_NAK;
                                break;
                            default:
                                bfinish = true;
                                mCountDownTimer.cancel();
                                Arrays.fill(buff,(byte)0x01);
                                Arrays.fill(buff,(byte)0x00);
                                //모든 작업이 종료 되면 네트워크 연결을 종료 한다.
                                Dispose();
                                break;
                        }

                        if (TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_RESEND ||
                                TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_INIT ||
                                TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_SEND_ACK ||
                                TCPPROCEDURE == TCPCommand.TCP_PROCEDURE_SEND_NAK) {
                            boolean sendComplete = false;
                            int sendCount = 0;
                            while (!sendComplete) {

                                try {
                                    if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND_ACK)
                                    {
                                        if(msocket.isConnected()==false)
                                        {
                                            sendComplete = true;
                                            bfinish = true;
                                        }

                                        networkWriter = new DataOutputStream(msocket.getOutputStream());
                                        byte[] SendAck = {Command.ACK,Command.ACK,Command.ACK };
                                        networkWriter.write(SendAck, sendCount, SendAck.length - sendCount);
                                        //Log.d(TAG, "client Send data size => " + networkWriter.size());
                                        sendCount += networkWriter.size();
                                        if (sendCount == SendAck.length) {
                                            networkWriter.flush();
                                            sendComplete = true;
                                            Arrays.fill(buff,(byte)0x01);
                                            Arrays.fill(buff,(byte)0x00);
                                            String mLog = "데이터사이즈 = " + sendCount +  ", 데이터 = ";
                                            cout("SEND_SERVER:APP->SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(SendAck));
                                        }
                                    }
                                    else if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND_NAK)
                                    {
                                        if(msocket.isConnected()==false)
                                        {
                                            sendComplete = true;
                                            bfinish = true;
                                        }
                                        networkWriter = new DataOutputStream(msocket.getOutputStream());
                                        byte[] SendNak = {Command.NAK };
                                        networkWriter.write(SendNak, sendCount, SendNak.length - sendCount);
                                        //Log.d(TAG, "client Send data size => " + networkWriter.size());
                                        sendCount += networkWriter.size();
                                        if (sendCount == SendNak.length) {
                                            networkWriter.flush();
                                            sendComplete = true;
                                            Arrays.fill(buff,(byte)0x01);
                                            Arrays.fill(buff,(byte)0x00);
                                            String mLog = "데이터사이즈 = " + sendCount +  ", 데이터 = ";
                                            cout("SEND_SERVER:APP->SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(SendNak));
                                        }
                                    }
                                    else {
                                        //데이터를 보내는 부분
                                        if(msocket.isConnected()==false)
                                        {
                                            sendComplete = true;
                                            bfinish = true;
                                        }
                                        networkWriter = new DataOutputStream(msocket.getOutputStream());
                                        networkWriter.write(bSendData, sendCount, bSendData.length - sendCount);
                                        //Log.d(TAG, "client Send data size => " + networkWriter.size());
                                        sendCount += networkWriter.size();
                                        if(sendCount == bSendData.length) {
                                            sendComplete = true;
                                            TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND;
                                            networkWriter.flush();
                                            String mLog = "데이터사이즈 = " + sendCount +  ", 데이터 = ";
                                            cout("SEND_SERVER:APP->SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(bSendData));
                                            Random rand = new Random();
                                            if(bSendData!=null)
                                            {
                                                for(int i=0;i<bSendData.length;i++)
                                                {
                                                    bSendData[i] = (byte)rand.nextInt(255);
                                                }
                                                Arrays.fill(bSendData,(byte)0x01);
                                                Arrays.fill(bSendData,(byte)0x00);
                                            }
                                            Arrays.fill(buff,(byte)0x01);
                                            Arrays.fill(buff,(byte)0x00);
                                        }


                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                    mNetworkConnectListener.onState(0,false, "데이터 전송중 에러 발생");
                                    bfinish = true;

                                    //TODO jiw.20.06.29 ic 거래중 lrc 오류  및 nak 송신 시 sendcomplete 를 하여야 해당 루프에서 빠져나올 수 있었다.
                                    sendComplete = true;
                                    Arrays.fill(bSendData,(byte)0x01);
                                    Arrays.fill(bSendData,(byte)0x00);
                                    Arrays.fill(buff,(byte)0x01);
                                    Arrays.fill(buff,(byte)0x00);
//                                    Dispose();
                                }


                                if(sendComplete)
                                {
                                    sendCount = 0;
                                    Arrays.fill(bSendData,(byte)0x01);
                                    Arrays.fill(bSendData,(byte)0x00);
                                    Arrays.fill(buff,(byte)0x01);
                                    Arrays.fill(buff,(byte)0x00);
                                }
                            }

                        }
                    } catch (IOException ex) {

                    }
                }

                Dispose();
            }
        }.start();
    }

    /**
     * 항목별로 read 함수 관련 타임 아웃 값을 결정 한다.
     * @param _TCPPROCEDURE
     */
    private void setDataSendRecvTimeout(int _TCPPROCEDURE)
    {
        int timeout = 15000;
        switch (_TCPPROCEDURE)
        {
            case TCPCommand.TCP_PROCEDURE_NONE:
                timeout = 15000;
                break;
            case TCPCommand.TCP_PROCEDURE_SEND:
                timeout = 30000;
                break;
            case TCPCommand.TCP_PROCEDURE_SEND_ACK:
                timeout = 10000;
                break;
        }
        try
        {
            if(msocket==null)
            {
                mNetworkConnectListener.onState(-1,false, "인터넷을 연결 할 수 없습니다.");
                return;
            }
            msocket.setSoTimeout(timeout);
        }
        catch (SocketException ex)
        {
//            Dispose();
        }
    }
    private boolean setSocket(String ip, int port) throws IOException {

        try {
            msocket = new Socket(ip, port);
            OutputStream os = msocket.getOutputStream();
            networkWriter = new DataOutputStream(os);
            InputStream is = msocket.getInputStream();
            networkReader = new DataInputStream(is);
        } catch (IOException e) {
            Log.d(TAG,e.toString());
            e.printStackTrace();
            Setting.setTcpServerConnectionState(false);
            return false;
        }
        return true;
    }

    private int CheckProtocol()
    {
        //int 0 데이터를 더 받아야 한다.
        //int 1 정상
        //int -1 시작값이 STX 가 아님.
        //int -2 응답 오류 LRC가 맞지 않는다.
        if(TCPPROCEDURE==TCPCommand.TCP_PROCEDURE_SEND)
        {
            while (mBuffer.indexData(0)!=Command.STX)
            {
                if(mBuffer.getlength()>1)
                {
                    mBuffer.CutToSize(1);
                }
                else
                {
                    //STX no => NAK
                    mBuffer.Clear();
//                    Message message = new Message();
//                    message.what = 2004;
//                    message.obj =null;
//                    mhandler.sendMessage(message);
//                    Setting.setEOTResult(0);
//                    mCountDownTimer.cancel();
                    return -3;
                }
            }
        }
        //TODO 여기서 부터 작성 필요
        if(mBuffer.indexData(0)!=Command.STX)
        {
            mBuffer.Clear();
            return -1;
        }
        if(mBuffer.indexData(0)==Command.EOT)
        {
            mBuffer.Clear();
            return 10;
        }

        int packetSize = 0;
        int ptlminSize = 12;
        if(mBuffer.getlength()>ptlminSize)
        {
            packetSize = Utils.AsciiArray4ToInt(mBuffer.indexRangeData(1,4));
        }

        if(packetSize>0)
        {
            if(mBuffer.getlength() >= (packetSize + 6))
            {
                //CheckLRC
                if(!Utils.CheckLRC(mBuffer.value()))
                {
                    return -2;
                }
                Message message = new Message();
                message.what = 2001;
                message.obj =mBuffer.CutToSize(packetSize + 6);
                //인증 패킷 로그 표시
                Log.d("KocesPacketData","[TCP SERVER -> POS]");
                Log.d("KocesPacketData",Utils.bytesToHex_0xType((byte[])message.obj));

                mhandler.sendMessage(message);
                mBuffer.Clear();
                Setting.setEOTResult(0);
                mCountDownTimer.cancel();
                mCountDownTimer.start();
                return 1;
            }
        }
        return 0;
    }

    final CountDownTimer mCountDownTimer = new CountDownTimer(10000, 10){
        @Override
        public void onTick(long l) {
            //인증에 방해 되기 때문에 표시 하지 않는다.
            //Log.d(TAG,"Waiting EOT time : " + l/10);
        }

        @Override
        public void onFinish() {
            //Todo 카운트다운으로 커맨드가 정상적으로 수신을 못받았단 것 체크 가능
            Setting.setEOTResult(-1);
            Dispose();
        }
    };

    public void Dispose(){
        bfinish = true;
        mNetworkConnectListener.onState(0,false,"네트워크 연결이 종료 되었습니다.");
        Setting.setTcpServerConnectionState(false);
        try
        {
            mCountDownTimer.cancel();
            mBuffer.Clear();
            if(msocket != null)
            {
                msocket.close();
                msocket = null;
                if(networkReader != null)
                {
                    networkReader.close();
                    networkReader = null;
                }

                if(networkWriter != null)
                {
                    networkWriter.flush();
                    networkWriter.close();
                    networkWriter = null;
                }
//                mBuffer = null;
            }
        }
        catch (IOException ex)
        {

        }


    }

    /**
     * TMP 서버연결시 사용
     */
    public void SetTMPNetwork(int _port, byte[]_data, TmpNetworkInterface.ConnectListener _TmpnetworkConnectListener) {
        mTMPPort = _port;
        mBuffer.Clear();
        Dispose();

        mBuffer = new KByteArray();
        mTmpNetworkConnectListener = _TmpnetworkConnectListener;

        new Thread() {
            public void run() {
                if ( !ConnectTmpServer()) {
                    Dispose();
                    mTmpNetworkConnectListener.onState(0, false, "TMP 서버 연결 실패");
                    return;
                }

                TmpTcpRead(new KocesTcpClient.TmpTcpListener() {

                    @Override
                    public void onSendResult(boolean _result) {

                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {
                        if (_recv == null) {
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                            return;
                        }
                        if (_recv.length <= 0) {
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                            return;
                        }

                        if (_recv[0] != Command.ENQ) {
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK) 수신에 실패했습니다.");
                            return;
                        }

                        String mLog = "데이터사이즈 = " + _data.length +  ", 데이터 = ";
                        cout("SEND_TMS_SERVER:APP->TMS_SERVER",Utils.getDate("yyyyMMddHHmmss"),mLog + Utils.bytesToHex_0xType(_data));

                        TMPTcpSend(_data, new TmpTcpListener() {
                            @Override
                            public void onSendResult(boolean _result) {
                                if(!_result){
                                    Dispose();
                                    mTmpNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                    return;
                                }
                                TmpRead();
                            }

                            @Override
                            public void onReceiveResult(boolean _result, byte[] _recv) {

                            }

                            @Override
                            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                            }
                        });
                        return;
                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });
            }
        }.start();



    }

    private void TmpRead() {
        TmpTcpRead(new KocesTcpClient.TmpTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    Dispose();
                    mTmpNetworkConnectListener.onState(0, false, "서버 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    Dispose();
                    mTmpNetworkConnectListener.onState(0, false, "서버 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv[0] == Command.NAK) {
                    Dispose();
                    mTmpNetworkConnectListener.onState(0, false, "서버 NAK 수신에 실패했습니다.");
                    return;
                }
                final byte[] b = _recv;

                KByteArray byteArray  = new KByteArray(b);
                int _re = 0;
                for (int i=0; i<byteArray.getlength(); i++) {
                    if(byteArray.indexData(i) == Command.STX) {
                        _re = i;
                        break;
                    }

                }
                byteArray.CutToSize(_re);

                if (byteArray.getlength() < 30 && _re == 0) {
                    Log.d("TMP_RECEIVE", "아직 enq만 온 상황");
                    return;
                }
                //완료전문보내기
                mTmpResult = byteArray.value();
                mTMPCountDownTimer.cancel();
                mTMPCountDownTimer.start();
                byte[] ack = {Command.ACK,Command.ACK,Command.ACK};
                TMPTcpSend(ack, new TmpTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if(!_result){
                            mTMPCountDownTimer.cancel();
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "TMS 서버(ACK) 전송에 실패했습니다.");
                            return;
                        }
                        mTMPCountDownTimer.cancel();
                        mTMPCountDownTimer.start();
                        TmpTcpRead(new KocesTcpClient.TmpTcpListener() {

                            @Override
                            public void onSendResult(boolean _result) {

                            }

                            @Override
                            public void onReceiveResult(boolean _result, byte[] _recv) {
                                if (_recv == null) {
                                    mTMPCountDownTimer.cancel();
                                    Dispose();
                                    mTmpNetworkConnectListener.onState(0, false, "TMS 서버(EOT) 데이터 수신에 실패했습니다.");
                                    return;
                                }
                                if (_recv.length <= 0) {
                                    mTMPCountDownTimer.cancel();
                                    Dispose();
                                    mTmpNetworkConnectListener.onState(0, false, "TMS 서버(EOT) 데이터 수신에 실패했습니다.");
                                    return;
                                }

                                if (_recv[0] != Command.EOT) {
                                    mTMPCountDownTimer.cancel();
                                    Dispose();
                                    mTmpNetworkConnectListener.onState(0, false, "TMS 서버(EOT) 수신에 실패했습니다.");
                                    return;
                                }
                                Message message = new Message();
                                message.what = 2001;
                                message.obj = byteArray.value();
                                //인증 패킷 로그 표시
                                Log.d("KocesPacketData","[TCP SERVER -> POS]");
                                Log.d("KocesPacketData",Utils.bytesToHex_0xType((byte[])message.obj));

                                mhandler.sendMessage(message);
                                mBuffer.Clear();
                                Setting.setEOTResult(0);
                                mCountDownTimer.cancel();
                                mTMPCountDownTimer.cancel();
                                Dispose();
                                return;
                            }

                            @Override
                            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                            }
                        });
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });


                return ;

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    public boolean ConnectTmpServer()
    {

        try{
            if(Setting.getTcpServerConnectionState()==false || msocket.isConnected()==false) {
                boolean bResult = setSocket(mServerIP,mTMPPort);
                if(!bResult)
                {
                    return false;
                }
            }

        } catch (UnknownHostException uhe) {
            // 소켓 생성 시 전달되는 호스트(mServerIP)의 IP를 식별할 수 없음.
            Log.d(TAG,"호스트(mServerIP)의 IP를 식별할 수 없음");
            Setting.setTmpTcpServerConnectionState(false);
            return false;
        } catch (IOException ioe) {
            // 소켓 생성 과정에서 I/O 에러 발생.
            Log.d(TAG,"소켓 생성 과정에서 I/O 에러 발생");
            Setting.setTmpTcpServerConnectionState(false);
            ioe.printStackTrace();
            return false;

        } catch (SecurityException se) {
            // security manager에서 허용되지 않은 기능 수행.
            Log.d(TAG,"security manager에서 허용되지 않은 기능 수행");
            Setting.setTmpTcpServerConnectionState(false);
            return false;
        } catch (IllegalArgumentException ex) {
            // 소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남.
            Log.d(TAG,"소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남");
            Setting.setTmpTcpServerConnectionState(false);
            return false;
        }
        Setting.setTmpTcpServerConnectionState(true);
        return true;
    }
    //ack무응답(eot 미수신)
    private void TMPAckError(byte[] _b) {
        mTMPCountDownTimer.cancel();
        mTMPCountDownTimer.start();
        byte[] ack = {Command.ACK,Command.ACK,Command.ACK};
        TMPTcpSend(ack, new TmpTcpListener() {
            @Override
            public void onSendResult(boolean _result) {
                if(!_result){
                    mCountDownTimer.cancel();
                    mTMPCountDownTimer.cancel();
                    Dispose();
                    mTmpNetworkConnectListener.onState(0, false, "TMS 서버(ACK) 전송에 실패했습니다.");
                    return;
                }
                TmpTcpRead(new KocesTcpClient.TmpTcpListener() {

                    @Override
                    public void onSendResult(boolean _result) {

                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {
                        if (_recv == null) {
                            mCountDownTimer.cancel();
                            mTMPCountDownTimer.cancel();
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "TMS 서버(EOT) 데이터 수신에 실패했습니다.");
                            return;
                        }
                        if (_recv.length <= 0) {
                            mCountDownTimer.cancel();
                            mTMPCountDownTimer.cancel();
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "TMS 서버(EOT) 데이터 수신에 실패했습니다.");
                            return;
                        }

                        if (_recv[0] != Command.EOT) {
                            mCountDownTimer.cancel();
                            mTMPCountDownTimer.cancel();
                            Dispose();
                            mTmpNetworkConnectListener.onState(0, false, "TMS 서버(EOT) 수신에 실패했습니다.");
                            return;
                        }
                        Message message = new Message();
                        message.what = 2001;
                        message.obj = _b;
                        //인증 패킷 로그 표시
                        Log.d("KocesPacketData","[TCP SERVER -> POS]");
                        Log.d("KocesPacketData",Utils.bytesToHex_0xType((byte[])message.obj));

                        mhandler.sendMessage(message);
                        mBuffer.Clear();
                        Setting.setEOTResult(0);
                        mCountDownTimer.cancel();
                        mTMPCountDownTimer.cancel();
                        Dispose();
                        return;
                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });
            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    public boolean TMPCheckBeforeTrade() {

        //연결되어 있지않다면
        if (Setting.getTmpTcpServerConnectionState() == false || msocket.isConnected() == false) {
            return false;
        }
        TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_INIT;
        boolean sendComplete = false;
        int sendCount = 0;
        while (!sendComplete) {
            try {
                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND_ACK;
                networkWriter = new DataOutputStream(msocket.getOutputStream());
                byte[] SendAck = {Command.ACK};
                networkWriter.write(SendAck, sendCount, SendAck.length - sendCount);
                sendCount += networkWriter.size();
                if (sendCount == SendAck.length) {
                    networkWriter.flush();
                    sendComplete = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mTmpNetworkConnectListener.onState(0, false, "데이터 전송중 에러 발생");
                sendComplete = true;
            }
        }
        if (sendComplete) {
            sendCount = 0;
        }

        setDataSendRecvTimeoutCAT(TCPPROCEDURE);

        try {
            byte[] buff = new byte[4096];
            setDataSendRecvTimeoutCAT(TCPPROCEDURE);

            int Count = networkReader.read(buff);
            if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
            {
                return false;
            }
            switch (buff[0]) {
                case Command.ACK:
                    break;
                default:
                    return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void TMPTcpSend(byte[] b,TmpTcpListener listener)
    {
        setTmpListener(listener);
        //인증용 로그 찍기
        Log.d("KocesPacketData","[POS -> CAT TCP SERVER]");
        Log.d("KocesPacketData",Utils.bytesToHex_0xType(b));
        //초기화 작업 해야 한다.
        final byte[] bSendData = b;
        mNakCount = 0;
        final boolean[] TcpSendCheck = {false};
        try {
            new Thread(){
                public void run()
                {
                    if (msocket  == null) {
                        TcpSendCheck[0] = false;
                        m_tmplistener.onSendResult(false);
                        return;
                    }
                    if(Setting.getTmpTcpServerConnectionState()==false || msocket.isConnected()==false)
                    {
                        TcpSendCheck[0] = false;
                        m_tmplistener.onSendResult(false);
                        return;
                    }
                    boolean sendComplete = false;
                    int sendCount = 0;
                    TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND;
                    while (!sendComplete) {
                        try {
                            networkWriter = new DataOutputStream(msocket.getOutputStream());
                            networkWriter.write(bSendData, sendCount, bSendData.length - sendCount);
                            //Log.d(TAG, "client Send data size => " + networkWriter.size());
                            sendCount += networkWriter.size();
                            if(sendCount == bSendData.length) {
                                sendComplete = true;
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND;
                                networkWriter.flush();
                                Random rand = new Random();
                                if(bSendData!=null)
                                {
                                    for(int i=0;i<bSendData.length;i++)
                                    {
                                        bSendData[i] = (byte)rand.nextInt(255);
                                    }
                                    Arrays.fill(bSendData,(byte)0x01);
                                    Arrays.fill(bSendData,(byte)0x00);
                                    TcpSendCheck[0] = true;
                                    m_tmplistener.onSendResult(true);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            TcpSendCheck[0] = false;
                            m_tmplistener.onSendResult(false);
                            return;
                        }
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

    public void TmpTcpRead(TmpTcpListener listener) {
        setTmpListener(listener);
        setDataSendRecvTimeoutCAT(TCPPROCEDURE);
//        mBuffer.Clear();
        byte[] buff = new byte[4096];
        try {
            new Thread() {
                public void run() {
                    int Count = 0;
                    try {
                        Count = networkReader.read(buff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
                    {
                        m_tmplistener.onReceiveResult(false,null);
                        mBuffer.Clear();
                        return;
                    } else if (Count == TCPCommand.TCP_PROCEDURE_NONE) {    //아무 데이터도 없다면
                        m_tmplistener.onReceiveResult(false,null);
                        mBuffer.Clear();
                        return;
                    }
                    byte[] tmp = new byte[Count];
                    System.arraycopy(buff, 0, tmp, 0, Count);
                    mBuffer.Add(tmp);
                    m_tmplistener.onReceiveResult(true,mBuffer.value());
                    mBuffer.Clear();
                }
            }.start();
        } finally {

        }

        return;
    }

    public void TmpTcpLastRead(byte[] _res, TmpTcpListener listener) {
        final byte[] finalLast = _res;
        setTmpListener(listener);
        setDataSendRecvTimeoutCAT(TCPPROCEDURE);
//        mBuffer.Clear();
        byte[] buff = new byte[4096];
        try {
            new Thread() {
                public void run() {
                    int Count = 0;
                    try {
                        Count = networkReader.read(buff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
                    {
                        m_listener.onReceiveLastResult(false,null, null);
                        mBuffer.Clear();
                        return;
                    } else if (Count == TCPCommand.TCP_PROCEDURE_NONE) {    //아무 데이터도 없다면
                        m_listener.onReceiveLastResult(false,null, null);
                        mBuffer.Clear();
                        return;
                    }
                    byte[] tmp = new byte[Count];
                    System.arraycopy(buff, 0, tmp, 0, Count);
                    mBuffer.Add(tmp);
                    m_listener.onReceiveLastResult(true,mBuffer.value(), finalLast);
                    mBuffer.Clear();
                }
            }.start();
        } finally {

        }

        return;
    }

    /**
     * CAT 연동시 사용
     */
    public void SetCatNetwork(String _ip, int _port, CatNetworkInterface.ConnectListener _CatnetworkConnectListener) {
        mCatServerIP = _ip;
        mCatServerPort = _port;
        mBuffer.Clear();
//        try {
//            msocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        mBuffer = new KByteArray();
        mCatNetworkConnectListener = _CatnetworkConnectListener;
    }

    public boolean ConnectServer()
    {

        try{
            if(Setting.getCatTcpServerConnectionState()==false || msocket.isConnected()==false) {
                boolean bResult = setSocket(mCatServerIP,mCatServerPort);
                if(!bResult)
                {
                    return false;
                }
            }

        } catch (UnknownHostException uhe) {
            // 소켓 생성 시 전달되는 호스트(mServerIP)의 IP를 식별할 수 없음.
            Log.d(TAG,"호스트(mServerIP)의 IP를 식별할 수 없음");
            Setting.setCatTcpServerConnectionState(false);
            return false;
        } catch (IOException ioe) {
            // 소켓 생성 과정에서 I/O 에러 발생.
            Log.d(TAG,"소켓 생성 과정에서 I/O 에러 발생");
            Setting.setCatTcpServerConnectionState(false);
            ioe.printStackTrace();
            return false;

        } catch (SecurityException se) {
            // security manager에서 허용되지 않은 기능 수행.
            Log.d(TAG,"security manager에서 허용되지 않은 기능 수행");
            Setting.setCatTcpServerConnectionState(false);
            return false;
        } catch (IllegalArgumentException ex) {
            // 소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남.
            Log.d(TAG,"소켓 생성 시 전달되는 포트 번호(65536)이 허용 범위(0~65535)를 벗어남");
            Setting.setCatTcpServerConnectionState(false);
            return false;
        }
        Setting.setCatTcpServerConnectionState(true);
        return true;
    }

    public boolean CheckBeforeTrade() {

        //연결되어 있지않다면
        if (Setting.getCatTcpServerConnectionState() == false || msocket.isConnected() == false) {
            return false;
        }
        TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_INIT;
        boolean sendComplete = false;
        int sendCount = 0;
        while (!sendComplete) {
            try {
                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND_ACK;
                networkWriter = new DataOutputStream(msocket.getOutputStream());
                byte[] SendAck = {Command.ACK};
                networkWriter.write(SendAck, sendCount, SendAck.length - sendCount);
                sendCount += networkWriter.size();
                if (sendCount == SendAck.length) {
                    networkWriter.flush();
                    sendComplete = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mCatNetworkConnectListener.onState(0, false, "데이터 전송중 에러 발생");
                sendComplete = true;
            }
        }
        if (sendComplete) {
            sendCount = 0;
        }

        setDataSendRecvTimeoutCAT(TCPPROCEDURE);

        try {
            byte[] buff = new byte[4096];
            setDataSendRecvTimeoutCAT(TCPPROCEDURE);

            int Count = networkReader.read(buff);
            if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
            {
                return false;
            }
            switch (buff[0]) {
                case Command.ACK:
                    break;
                default:
                    return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void TcpSend(byte[] b,CatTcpListener listener)
    {
        setCatListener(listener);
        //인증용 로그 찍기
        Log.d("KocesPacketData","[POS -> CAT TCP SERVER]");
        Log.d("KocesPacketData",Utils.bytesToHex_0xType(b));
        //초기화 작업 해야 한다.
        final byte[] bSendData = b;
        mNakCount = 0;
        final boolean[] TcpSendCheck = {false};
        try {
            new Thread(){
                public void run()
                {

                    if(Setting.getCatTcpServerConnectionState()==false || msocket.isConnected()==false)
                    {
                        TcpSendCheck[0] = false;
                        m_listener.onSendResult(false);
                        return;
                    }
                    boolean sendComplete = false;
                    int sendCount = 0;
                    TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND;
                    while (!sendComplete) {
                        try {
                            networkWriter = new DataOutputStream(msocket.getOutputStream());
                            networkWriter.write(bSendData, sendCount, bSendData.length - sendCount);
                            //Log.d(TAG, "client Send data size => " + networkWriter.size());
                            sendCount += networkWriter.size();
                            if(sendCount == bSendData.length) {
                                sendComplete = true;
                                TCPPROCEDURE = TCPCommand.TCP_PROCEDURE_SEND;
                                networkWriter.flush();
                                Random rand = new Random();
                                if(bSendData!=null)
                                {
                                    for(int i=0;i<bSendData.length;i++)
                                    {
                                        bSendData[i] = (byte)rand.nextInt(255);
                                    }
                                    Arrays.fill(bSendData,(byte)0x01);
                                    Arrays.fill(bSendData,(byte)0x00);
                                    TcpSendCheck[0] = true;
                                    m_listener.onSendResult(true);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            TcpSendCheck[0] = false;
                            m_listener.onSendResult(false);
                            return;
                        }
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

    public void TcpRead(CatTcpListener listener) {
        setCatListener(listener);
        setDataSendRecvTimeoutCAT(TCPPROCEDURE);
//        mBuffer.Clear();
        byte[] buff = new byte[4096];
        try {
            new Thread() {
                public void run() {
                    int Count = 0;
                    try {
                        Count = networkReader.read(buff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
                    {
                        m_listener.onReceiveResult(false,null);
                        mBuffer.Clear();
                        return;
                    } else if (Count == TCPCommand.TCP_PROCEDURE_NONE) {    //아무 데이터도 없다면
                        m_listener.onReceiveResult(false,null);
                        mBuffer.Clear();
                        return;
                    }
                    byte[] tmp = new byte[Count];
                    System.arraycopy(buff, 0, tmp, 0, Count);
                    mBuffer.Add(tmp);
                    m_listener.onReceiveResult(true,mBuffer.value());
                    mBuffer.Clear();
                }
            }.start();
        } finally {

        }

        return;
    }

    public void TcpLastRead(byte[] _res, CatTcpListener listener) {
        final byte[] finalLast = _res;
        setCatListener(listener);
        setDataSendRecvTimeoutCAT(TCPPROCEDURE);
//        mBuffer.Clear();
        byte[] buff = new byte[4096];
        try {
            new Thread() {
                public void run() {
                    int Count = 0;
                    try {
                        Count = networkReader.read(buff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (Count == TCPCommand.TCPSEVER_DISCONNECTED)    //-1이 넘어 오면 연결이 끊어진 상태
                    {
                        m_listener.onReceiveLastResult(false,null, null);
                        mBuffer.Clear();
                        return;
                    } else if (Count == TCPCommand.TCP_PROCEDURE_NONE) {    //아무 데이터도 없다면
                        m_listener.onReceiveLastResult(false,null, null);
                        mBuffer.Clear();
                        return;
                    }
                    byte[] tmp = new byte[Count];
                    System.arraycopy(buff, 0, tmp, 0, Count);
                    mBuffer.Add(tmp);
                    m_listener.onReceiveLastResult(true,mBuffer.value(), finalLast);
                    mBuffer.Clear();
                }
            }.start();
        } finally {

        }

        return;
    }

    /**
     * 항목별로 read 함수 관련 타임 아웃 값을 결정 한다.
     * @param _TCPPROCEDURE
     */
    private void setDataSendRecvTimeoutCAT(int _TCPPROCEDURE)
    {
        int timeout = 15000;
        switch (_TCPPROCEDURE)
        {
            case TCPCommand.TCP_PROCEDURE_NONE:
                timeout = 15000;
                break;
            case TCPCommand.TCP_PROCEDURE_SEND:
                timeout = 30000;
                break;
            case TCPCommand.TCP_PROCEDURE_SEND_ACK:
                timeout = 10000;
                break;
        }
        try
        {
            if(msocket==null)
            {
                mCatNetworkConnectListener.onState(-1,false, "인터넷을 연결 할 수 없습니다.");
                return;
            }
            msocket.setSoTimeout(timeout);
        }
        catch (SocketException ex)
        {
//            Dispose();
        }
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
     * 로그 파일을 위해 스트링 값을 입력 받아 로그파일클래스로전달한다
     * @param _str
     */
    LogFile m_logfile;
    public void WriteLogFile(String _str)
    {
        if(m_logfile == null) {
            m_logfile = new LogFile(Setting.getTopContext());
        }
        m_logfile.writeLog(_str);
    }

    private byte[] mTmpResult = null;
    final CountDownTimer mTMPCountDownTimer = new CountDownTimer(5000, 1000){
        @Override
        public void onTick(long l) {
            //인증에 방해 되기 때문에 표시 하지 않는다.
            //Log.d(TAG,"Waiting EOT time : " + l/10);
        }

        @Override
        public void onFinish() {
            TMPAckError(mTmpResult);
//            mCatNetworkConnectListener.onState(0, false, "네트워크 연결이 종료 되었습니다.");
        }
    };

    final CountDownTimer mCatCountDownTimer = new CountDownTimer(10000, 10){
        @Override
        public void onTick(long l) {
            //인증에 방해 되기 때문에 표시 하지 않는다.
            //Log.d(TAG,"Waiting EOT time : " + l/10);
        }

        @Override
        public void onFinish() {
            mCatNetworkConnectListener.onState(0, false, "네트워크 연결이 종료 되었습니다.");
        }
    };

    public void DisConnectCatServer(){
        Setting.setCatTcpServerConnectionState(false);
        try
        {
            mCatCountDownTimer.cancel();
            mBuffer.Clear();
            if (msocket != null) {
                if (!msocket.isClosed()) {
                    msocket.close();
                    networkReader.close();
                    networkWriter.close();
                    msocket = null;
                    networkReader = null;
                    networkWriter = null;
//                    mBuffer = null;
                }
            }
        }
        catch (IOException ex)
        {

        }
    }

    private CatTcpListener m_listener;
    public void setCatListener(CatTcpListener listener) { m_listener = listener; }

    public interface CatTcpListener {
        void onSendResult(boolean _result);              // 정상적으로 보냈을때
        void onReceiveResult(boolean _result, byte[] _recv);              // 정상적으로 받았을때
        void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData);              // 정상적으로 받았을때 최종결과
    }

    private TmpTcpListener m_tmplistener;
    public void setTmpListener(TmpTcpListener listener) { m_tmplistener = listener; }

    public interface TmpTcpListener {
        void onSendResult(boolean _result);              // 정상적으로 보냈을때
        void onReceiveResult(boolean _result, byte[] _recv);              // 정상적으로 받았을때
        void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData);              // 정상적으로 받았을때 최종결과
    }
}
