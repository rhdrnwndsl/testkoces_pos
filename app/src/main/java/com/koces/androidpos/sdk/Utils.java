package com.koces.androidpos.sdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
//test
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.koces.androidpos.sdk.log.LogFile;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.KocesTcpClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.koces.androidpos.sdk.Command.F_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA;
import static com.koces.androidpos.sdk.Command.F_CONTROL_DAT_BYTE;
import static com.koces.androidpos.sdk.Command.T_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA;
import static com.koces.androidpos.sdk.Command.T_CONTROL_DAT_BYTE;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private final static String TAG = "Utils";
    private final static int lineCount = 48;    //한 줄에 찍는 프린트 바이트 수 48 -> 42 -> 38?
    /**
     * 네트워크 연결 상태를 체크 한다.
     * @param _ctx
     * @return
     */
    public static int getNetworkState(Context _ctx){
        ConnectivityManager connectivityManager = (ConnectivityManager) _ctx.getSystemService(CONNECTIVITY_SERVICE);
        //Network network =  connectivityManager.getActiveNetwork();

//        //5G의 경우에는 network가 null로 체크 됨
//        if(network==null){return 5;}
//
//        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
//
//        if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
//            return 1;
//        }
//        else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
//            return 2;
//        }
//        else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
//            return 3;
//        }
//        return 0;
        boolean isWifiConn = false;
        boolean isMobileConn = false;
        boolean isENetConn = false;
        for (Network network : connectivityManager.getAllNetworks()) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                isWifiConn |= networkInfo.isConnected();
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                isMobileConn |= networkInfo.isConnected();
            }
            //2020-08-05 kim.ky 이더넷 체크 코드 추가
            if(networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET){
                isENetConn |= networkInfo.isConnected();
            }
        }
        Log.d("INTERNET", "Wifi connected: " + isWifiConn);
        Log.d("INTERNET", "Mobile connected: " + isMobileConn);
        Log.d("INTERNET","Ethermet Connected" + isENetConn);

        if(isWifiConn == false && isMobileConn==false && isENetConn==false)
        {
            return 0;
        }
        return 1;
    }

    /**
     * Main2Activity화면에 표시할 ID
     * @return
     */
    public static String getAppID()
    {
        //2020-08-05 kim.jy 수정
        return "##KOCESICAPP1117";
    }

    /**
     * Koces 서버용 패킷을 만드는 함수
     * @param _b
     * @return
     */
    public static byte[] MakeClientPacket(byte[] _b)
    {
        byte[] cmdFrame = null;
        if(_b!=null && _b.length> 0) {
            cmdFrame = new byte[_b.length + T_CONTROL_DAT_BYTE];
            System.arraycopy(_b, 0, cmdFrame, T_CONTROL_DAT_BYTE-1, _b.length);
        }
        int dataSize = cmdFrame.length;    //데이터 size는 STX, CMD, 다음 2byte로 표시한다 (d[2],d[3])
        byte[] dataLen = intTo4AsciiArray(dataSize - T_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA);

        for(int i=0;i<dataLen.length;i++)
        {
            if(dataLen[i]==(byte)0x20)
            {
                dataLen[i]=(byte)0x30;
            }
        }

        cmdFrame[0] = 0x02;
        cmdFrame[1] = dataLen[0];
        cmdFrame[2] = dataLen[1];
        cmdFrame[3] = dataLen[2];
        cmdFrame[4] = dataLen[3];

        byte[] protocolver = TCPCommand.PROTOCOL_VERSION.getBytes();
        cmdFrame[5] = protocolver[0];
        cmdFrame[6] = protocolver[1];
        cmdFrame[7] = protocolver[2];
        cmdFrame[8] = protocolver[3];

        //FS
        cmdFrame[9] = Command.FS;

        cmdFrame[cmdFrame.length - 1] = Command.ETX;

        byte sendBuffer[] = new byte[cmdFrame.length + 1];
        System.arraycopy(cmdFrame, 0, sendBuffer, 0, cmdFrame.length);
        sendBuffer[cmdFrame.length] = makeLRC2(cmdFrame);

        //test 위해서 만들어 놓은 코드 ->
//        String tmpLog = bytesToHex_0xType(sendBuffer);
//        Log.d(TAG,tmpLog);
//        String tmpLog2 ="";
//        for(int i=0;i<sendBuffer.length;i++)
//        {
//            String sssss = String.valueOf(sendBuffer[i]);
//
//            tmpLog2 += sssss.length()>1?sssss+" ":"0"+sssss+" ";
//        }
//        Log.d(TAG,tmpLog2);
        //<-
        return sendBuffer;
    }

    /**
     * Koces TMS서버용 패킷을 만드는 함수
     * @param _b
     * @return
     */
    public static byte[] MakeTMSClientPacket(byte[] _b, String _cmd)
    {
        byte[] cmdFrame = null;
        if(_b!=null && _b.length> 0) {
            cmdFrame = new byte[_b.length + T_CONTROL_DAT_BYTE + 4];
            System.arraycopy(_b, 0, cmdFrame, T_CONTROL_DAT_BYTE+4-1, _b.length);
        }
        int dataSize = cmdFrame.length;    //데이터 size는 STX, CMD, 다음 2byte로 표시한다 (d[2],d[3])
        byte[] dataLen = intTo4AsciiArray(dataSize - T_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA + 1);

        for(int i=0;i<dataLen.length;i++)
        {
            if(dataLen[i]==(byte)0x20)
            {
                dataLen[i]=(byte)0x30;
            }
        }

        cmdFrame[0] = 0x02;
        cmdFrame[1] = dataLen[0];
        cmdFrame[2] = dataLen[1];
        cmdFrame[3] = dataLen[2];
        cmdFrame[4] = dataLen[3];

        byte[] cmdver = _cmd.getBytes();
        cmdFrame[5] = cmdver[0];
        cmdFrame[6] = cmdver[1];
        cmdFrame[7] = cmdver[2];
        cmdFrame[8] = cmdver[3];

        cmdFrame[9] = (byte)0x20;
        cmdFrame[10] = (byte)0x20;
        cmdFrame[11] = (byte)0x20;
        cmdFrame[12] = (byte)0x20;

        //FS
        cmdFrame[13] = Command.FS;

        cmdFrame[cmdFrame.length - 1] = Command.ETX;

        byte sendBuffer[] = new byte[cmdFrame.length + 1];
        System.arraycopy(cmdFrame, 0, sendBuffer, 0, cmdFrame.length);
        sendBuffer[cmdFrame.length] = makeLRC2(cmdFrame);

        return sendBuffer;
    }
    public static byte[] MakePacket(byte _command,byte[] _b)
    {
        byte[] cmdFrame;
        if(_b!=null && _b.length> 0) {
            cmdFrame = new byte[_b.length + F_CONTROL_DAT_BYTE];
            System.arraycopy(_b,0,cmdFrame,F_CONTROL_DAT_BYTE,_b.length);
        }
        else
        {
            cmdFrame = new byte[F_CONTROL_DAT_BYTE];
        }

        int dataSize = cmdFrame.length;    //데이터 size는 STX, CMD, 다음 2byte로 표시한다 (d[2],d[3])
        byte[] dataLen = intTo2ByteArray(dataSize - F_CONTROL_DATA_LEN_EXCLUDE_NOT_DATA);

        cmdFrame[0] = 0x02;
        cmdFrame[1] = dataLen[0];
        cmdFrame[2] = dataLen[1];
        cmdFrame[3] = _command;

        byte b1[] = new byte[cmdFrame.length + 1];
        System.arraycopy(cmdFrame, 0, b1, 0, cmdFrame.length);
        b1[cmdFrame.length] = Command.ETX;

        byte sendBuffer[] = new byte[b1.length + 1];
        System.arraycopy(b1, 0, sendBuffer, 0, b1.length);
        sendBuffer[b1.length] = makeLRC(b1);

        //String sendBufferString = bytesToHex(sendBuffer);
        return sendBuffer;
    }

    public static byte makeLRC(byte[] bytes) {
        byte lrc = 0;

        for(int idx = 1; idx < bytes.length; ++idx) {
            lrc ^= bytes[idx];
        }
        return lrc;
    }
    public static byte makeLRC2(byte[] bytes) {
        byte lrc = (byte)0x00;

        for(int idx = 1; idx < bytes.length; idx++) {
            lrc ^= bytes[idx];
        }
        return lrc;
    }
    public static boolean CheckLRC(byte[] bytes)
    {
        byte lrc = 0;

        for(int idx = 1; idx < bytes.length-1; idx++) {
            lrc ^= bytes[idx];
        }

        if(bytes[bytes.length-1]==lrc)
        {
            return true;
        }
        return false;
    }
    public static final byte[] intTo2ByteArray(int value) {
        return new byte[]{(byte)(value >> 8 & 255), (byte)(value & 255)};
    }
    public static final byte[] intTo4ByteArray(int value) {
        return new byte[]{(byte)(value >> 24 & 255),(byte)(value >> 16 & 255),(byte)(value >> 8 & 255), (byte)(value & 255)};
    }
    public static final byte[] intTo4AsciiArray(int value) {
        String tmp="";
        if(value>9999)
        {
            tmp = String.valueOf(9999);
            return tmp.getBytes();
        }

        tmp = String.format("%04d",value);

        return tmp.getBytes();
    }
    public static int AsciiArray4ToInt(byte[] src) {
        String tmp ="";
        tmp = new String(src);

        return Integer.parseInt(tmp);
    }
    public static int byteToInt(byte[] src) {
        if(src.length != 2) return -1;
        int s1 = src[0] & 0xFF;
        int s2 = src[1] & 0xFF;
        return ((s1 << 8) + (s2 << 0));
    }
    public static int byte4ToInt(byte[] src) {
        int s1 = src[0] & 0xFF;
        int s2 = src[1] & 0xFF;
        int s3 = src[2] & 0xFF;
        int s4 = src[3] & 0xFF;

        return ((s1 << 24) + (s2 << 16) + (s3 << 8) + (s4 << 0));
    }

    public static String intToHexString(int _val)
    {
        return "0x" + Integer.toHexString(_val);
    }

    /**
     * 전문일련번호 생성 코드
     * @return
     */
    public static String getUniqueProtocolNumbering()
    {
        int Count;
        String localSaveDate = Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.KEY_AUTH_DATE_TIME);
        if(localSaveDate.equals(getDate()))
        {
            if(Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.UNIQUE_NUM).isEmpty())
            {
                Count = 1;
                Setting.setPreference(KocesPosSdk.getInstance().getActivity(),Constants.UNIQUE_NUM,String.valueOf(Count));
            }
            else
            {
                Count = Integer.parseInt(Setting.getPreference(KocesPosSdk.getInstance().getActivity(), Constants.UNIQUE_NUM));
                Count++;
                Setting.setPreference(KocesPosSdk.getInstance().getActivity(),Constants.UNIQUE_NUM,String.valueOf(Count));

            }
        }
        else
        {
            Setting.setPreference(KocesPosSdk.getInstance().getActivity(),Constants.KEY_AUTH_DATE_TIME,getDate());
            Count = 1;
        }

        if(Count > Constants.MAX_UNIQUE_COUNT)
        {
            Count = 1;
        }

        String tmp = String.format("%06d", Count);
        return tmp;
    }

    public static String getDate()
    {
        SimpleDateFormat sdf =new  SimpleDateFormat("yyyyMMdd");
        return sdf.format(new Date());
    }

    /**
     *
     * @param _format yyyyMMdd 또는 yyMMddHHmmss 또는
     * @return
     */
    public static String getDate(String _format)
    {
        SimpleDateFormat sdf =new  SimpleDateFormat(_format);
        return sdf.format(new Date());
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return sb.toString();
    }

    public static String bytesToHexString2(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }

    public static String bytesToHex_0xType(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        String tmp = "";

        for(int i=0;i<hexChars.length;i+=2)
        {
            //tmp += "0x" + String.valueOf(hexChars[i])+String.valueOf(hexChars[i+1]) + " ";
            tmp += String.valueOf(hexChars[i])+String.valueOf(hexChars[i+1]) + " ";
        }
        return tmp;
    }
    public static String bytesToHex_0xType_nospace(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        String tmp = "";

        for(int i=0;i<hexChars.length;i+=2)
        {
            //tmp += "0x" + String.valueOf(hexChars[i])+String.valueOf(hexChars[i+1]) + " ";
            tmp += String.valueOf(hexChars[i])+String.valueOf(hexChars[i+1]);
        }
        return tmp;
    }

    /**
     * FS ETX를 기준으로 잘라서 List<Byte[]> 형태로 리턴 시켜준다.
     * @param _res
     * @return Size를 통해서 현재 몇개의 KByteArray가 존재 하는지 확인 가능
     */
    public static List<byte[]> getresData(byte[] _res) {
        List<byte[]> Response =new ArrayList<>();
        byte[] minusLRC = new byte[_res.length-1];
        System.arraycopy(_res,0,minusLRC,0,minusLRC.length);
        KByteArray tmp = new KByteArray();
        for(byte n:minusLRC)
        {

            if(n==Command.FS || n==Command.ETX)
            {
                if(tmp.getlength()==0)
                {
                    byte[] NoData = new byte[1];
                    NoData[0] = (byte)0x00;
                    Response.add(NoData);
                }
                else
                {
                    Response.add(tmp.value());
                    tmp.Clear();
                }
            }
            else
            {
                tmp.Add(n);
            }
        }
        return Response;
    }

    /**
     *  byte[] 로 받은 배열을 EUC-KR String로 변환한다
     */
    public static String getByteToString_euc_kr(byte[] _res) throws UnsupportedEncodingException {
        String _euc = new String(_res, "EUC-KR");
        return _euc;
    }

    public static byte[] getString_euc_kr(String _res) throws UnsupportedEncodingException{
        byte[] _euc = _res.getBytes("EUC-KR");
        return _euc;
    }
    /**
     * 오른쪽 정렬하고 사이즈보다 스트링이 작으면 스페이스를 채운다.
     * @param _str
     * @param size
     * @return
     */
    public static String StringAlignright(String _str,int size)
    {
        if(_str==null)
        {
            return null;
        }
        if(_str.length()==size)
        {
            return _str;
        }
        int leftCount = size - _str.length();

        String tmp="";
        for(int i=0;i<leftCount;i++)
        {
            tmp += " ";
        }
        return tmp + _str;
    }

    public static String StringAlignrightzero(String _str,int size)
    {
        if(_str==null)
        {
            return null;
        }
        if(_str.length()==size)
        {
            return _str;
        }
        int leftCount = size - _str.length();

        String tmp="";
        for(int i=0;i<leftCount;i++)
        {
            tmp += "0";
        }
        return tmp + _str;
    }
    /**
     * byte[]에서 앞부분을 지우고 byte[]로 반환한다
     */
    public static byte[] reByte(byte[] _res, int _in, int _out, int _length)
    {
        byte[] _tmp;
        if(_length == 0)
        {
            _tmp = new byte[_res.length-_in];
            System.arraycopy(_res,_in,_tmp,_out,_res.length-_in);
        }
        else
        {
            _tmp = new byte[_length+1];
            System.arraycopy(_res,_in,_tmp,_out,_length+1);
        }
        return _tmp;
    }

    public CCTcpPacket TcpPacketParser(byte[] _b)
    {
        CCTcpPacket tp = new CCTcpPacket(_b);
        return tp;
    }

    /**
     * Unnecessary code (aka garbage code)
     * @param _b
     * @return string value
     */
    public static String ByteArrayToString_IfContains_Null_it_returns_empty(byte[] _b)
    {
        boolean isAllDataNull = true;
        for(byte n:_b)
        {
            if(n!=(byte)0x00)
            {
                isAllDataNull = false;
            }
        }
        if(!isAllDataNull)
        {
            return new String(_b);
        }
        return "";
    }

    /**
     * Unnecessary code (aka garbage code)
     * @param _b
     * @return string value
     */
    public static String ByteArrayToString_IfContains_Null_it_returns_empty(byte[] _b,String _encoding)
    {
        boolean isAllDataNull = true;
        for(byte n:_b)
        {
            if(n!=(byte)0x00)
            {
                isAllDataNull = false;
            }
        }
        if(!isAllDataNull)
        {
            if(!_encoding.equals(""))
            {
                try{
                    return new String(_b,_encoding);
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                return new String(_b);
            }

        }
        return "";
    }

    /**
     * Unnecessary code (aka garbage code)
     * @param _b
     * @return string value
     */
    public static String ByteArrayToString(byte[] _b)
    {
        boolean isAllDataNull = true;
        for(byte n:_b)
        {
            if(n!=(byte)0x00)
            {
                isAllDataNull = false;
            }
        }
        if(!isAllDataNull)
        {
            return new String(_b);
        }
        return "";
    }

    /**
     * TCp 패킷을 앞부분과 뒷부분으로 나누고 뒷 부분에 오는 부분을 List<Byte[]> 로 처리 하여 전달 한다.
     */
    public static class CCTcpPacket{
        KByteArray mData;
        byte[] mbyteData;
        byte[] posEtc;

        public String getResponseCode() {
            return ResponseCode;
        }

        String terminalID;
        public String getTerminalID(){return terminalID;}
        String ResponseCode ;
        String Date;
        String UniqueNum;

        public String getDate(){return Date; }
        int mCountFS;
        List<byte[]> Response;
        public CCTcpPacket(byte[] _b)
        {
            mData = new KByteArray(_b);
            mbyteData = new byte[_b.length];
            System.arraycopy(_b,0,mbyteData,0,_b.length);
            CheckIndex();
            SortCode();
        }
        private void CheckIndex()
        {
            mData.CutToSize(1); //STX
            String tmp1 = new String(mData.CutToSize(4)); //전문길이
            String tmp2 = new String(mData.CutToSize(4)); //전문버전
            mData.CutToSize(1); //FS
            ResponseCode = new String(mData.CutToSize(3)); //전문 응답 코드
            terminalID  =  new String(mData.CutToSize(10)); //단말기 ID
            Date = new String(mData.CutToSize(12));//거래일시
            UniqueNum = new String(mData.CutToSize(6)); //전문 일련 번호
            String tmp4 =  new String(mData.CutToSize(1)); //단말 구분
            String tmp5  =  new String(mData.CutToSize(5)); //단말 버전
//            posEtc = new byte[23];
//            System.arraycopy(mData.CutToSize(23),0,posEtc,0,posEtc.length);
            mData.CutToSize(1); //FS
        }

        public int CountFS()
        {
            mCountFS = 0;
            for(byte n:mbyteData)
            {
                if(n==Command.FS){mCountFS++;}
            }
            return mCountFS;
        }
        private void SortCode()
        {
            switch (ResponseCode)
            {
                case TCPCommand.CMD_IC_OK_RES:
                case TCPCommand.CMD_IC_CANCEL_RES:
                case TCPCommand.CMD_ZEROPAY_RES:
                case TCPCommand.CMD_ZEROPAY_CANCEL_RES:
                case TCPCommand.CMD_ZEROPAY_SEARCH_RES:
                case TCPCommand.CMD_KAKAOPAY_RES:
                case TCPCommand.CMD_KAKAOPAY_CANCEL_RES:
                case TCPCommand.CMD_KAKAOPAY_SEARCH_RES:
                    FSETX_SeparationWork(mData.value());
                    break;
                case TCPCommand.CMD_SHOP_DOWNLOAD_RES:
                case TCPCommand.CMD_SHOPS_DOWNLOAD_RES:
                case TCPCommand.CMD_KEY_UPDATE_RES:
                    FSETX_SeparationWork(mData.value());
                    break;
                case TCPCommand.CMD_CASH_RECEIPT_RES:
                case TCPCommand.CMD_CASH_RECEIPT_CANCEL_RES:
                    FSETX_SeparationWork(mData.value());
                    break;
                case TCPCommand.CMD_CASHIC_BUY_RES:
                case TCPCommand.CMD_CASHIC_BUY_CANCEL_RES:
                case TCPCommand.CMD_CASHIC_CHECK_ACCOUNT_RES:
                    FSETX_SeparationWork(mData.value());
                    break;
                default:
                    break;
            }
        }
        private void FSETX_SeparationWork(byte [] _b)
        {
            Response =new ArrayList<>();
            byte[] minusLRC = new byte[_b.length-1];
            System.arraycopy(_b,0,minusLRC,0,minusLRC.length);
            KByteArray tmp = new KByteArray();
            for(byte n:minusLRC)
            {

                if(n==Command.FS || n==Command.ETX)
                {
                    if(tmp.getlength()==0)
                    {
                        byte[] NoData = new byte[1];
                        NoData[0] = (byte)0x00;
                        Response.add(NoData);
                    }
                    else
                    {
                        byte[] tmp2 = new byte[tmp.getlength()];
                        System.arraycopy(tmp.value(),0,tmp2,0,tmp2.length);
                        Response.add(tmp2);
                        tmp.Clear();
                    }
                }
                else
                {
                    tmp.Add(n);
                }
            }
        }
        public List<byte[]> getResData()
        {
            if(Response==null)
            {
                return null;
            }
            return Response;
        }
        public void deletememoney()
        {
            mData.Clear();
            Response.clear();
            ResponseCode = null;
            Date= null;
            UniqueNum= null;
            mCountFS= 0;

            mbyteData = new byte[10];
            if(mbyteData!=null)
            {
                mbyteData = new byte[10];
                for (int i = 0;i<mbyteData.length;i++)
                {
                    mbyteData[i] = (byte)0x01;
                }
                mbyteData = new byte[10];
                for (int i = 0;i<mbyteData.length;i++)
                {
                    mbyteData[i] = (byte)0x00;
                }
            }

            posEtc = new byte[10];
            if(posEtc!=null)
            {
                posEtc = new byte[10];
                for (int i = 0;i<posEtc.length;i++)
                {
                    posEtc[i] = (byte)0x01;
                }
                posEtc = new byte[10];
                for (int i = 0;i<posEtc.length;i++)
                {
                    posEtc[i] = (byte)0x00;
                }
            }

        }
    }

    public static String CheckNullOrEmptyReturnZero(String _src)
    {
        if(_src==null)
        {
            return "0";
        }
        if(_src.equals(""))
        {
            return "0";
        }

        return _src;
    }

    /**
     * 문자열이 null이거나 빈 경우검사
     * @param str
     * @return null이거나 빈 경우에는 true를 그렇지 않은 경우에는 false 를 리턴한다.
     */
    public static boolean isNullOrEmpty(String str)
    {
        if(str==null)
        {
            return true;
        }

        if(str.equals("null"))
        {
            return true;
        }

        if(str.equals(""))
        {
            return true;
        }
        return false;
    }


    public static byte[] byteCopy(byte[] bytes, int srcPos, int newLength) {
        if (bytes.length < newLength) {
            return bytes;
        } else {
            byte[] truncated = new byte[newLength];
            System.arraycopy(bytes, srcPos, truncated, 0, newLength);
            return truncated;
        }
    }

    /** 가맹점다운로드 시 사용 하는 맥어드레스. 현재 맥어드레스를 사용할 수 없는 기기들이 많아 기기고유ID를 가져온다.(15자리만 첨부) */
    public static String getMacAddress (Context mCtx){
        String androidID = Settings.Secure.getString(mCtx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if(androidID.length()>=15)
        {
            return androidID.substring(0,15);
        }

        int rightCount = 15 - androidID.length();

        String tmp="";
        for(int i=0;i<rightCount;i++)
        {
            tmp += " ";
        }
        return androidID + tmp;
    }

    /** 가맹점다운로드 완료 후 받은 하드웨어 키를 저장한다. (15자리여야 하며 해당 키는 base64 인코딩하여 저장후 사용시에는 디코딩하여 사용한다) */
    public static void setHardwareKey (Context _ctx, String _hardwarekey, boolean _appToapp, String _tid) {
//        LogFile mlog = LogFile.getinstance();
//        mlog.writeHardwareKey(_hardwarekey,_appToapp,_tid);

        String HardwareKey = _hardwarekey;
        if (_appToapp == true) {
            if (Setting.getPreference(_ctx, Constants.KeyChainAppToApp + _tid) != null) {
                if (Setting.getPreference(_ctx, Constants.KeyChainAppToApp + _tid) != "") {
                    return;
                }
            }
        } else {
            if (Setting.getPreference(_ctx, Constants.KeyChainOriginalApp + _tid) != null) {
                if (Setting.getPreference(_ctx, Constants.KeyChainOriginalApp + _tid) != "") {
                    return;
                }
            }
        }

        if(HardwareKey.length()>=15)
        {
            HardwareKey = HardwareKey.substring(0,15);
        }

        int rightCount = 15 - HardwareKey.length();

        String tmp="";
        for(int i=0;i<rightCount;i++)
        {
            tmp += " ";
        }
        HardwareKey = HardwareKey + tmp;
        String _base64 = Base64.encodeToString(HardwareKey.getBytes(), Base64.DEFAULT);
        if (_appToapp == true) {
            Setting.setPreference(_ctx, Constants.KeyChainAppToApp + _tid, _base64);
        } else {
            Setting.setPreference(_ctx, Constants.KeyChainOriginalApp + _tid, _base64);
        }

    }

    public static String getHardwareKey (Context _ctx,boolean _appToapp, String _tid) {
//        LogFile mlog = LogFile.getinstance();
        String HardwareKey = "";
//        HardwareKey =   mlog.readHardwareKey(_appToapp,_tid);

        if (_appToapp == true) {
            HardwareKey = Setting.getPreference(_ctx, Constants.KeyChainAppToApp + _tid);
            HardwareKey = new String(Base64.decode(HardwareKey,Base64.DEFAULT)) ;
        } else {
            HardwareKey = Setting.getPreference(_ctx, Constants.KeyChainOriginalApp + _tid);
            HardwareKey = new String(Base64.decode(HardwareKey,Base64.DEFAULT)) ;
        }

        return HardwareKey;
    }

    /** TmicNo 단말인증번호가 리더기를 사용하지 않는 거래를 서버로 전달할 때는
     ##KOCESICIOS1002################
     APP 의 식별번호 + # 패딩으로 처리
     */
    public static String AppTmlcNo()
    {
        String appid = "################" + getAppID();

        return appid;
    }

    /**
     프린트 할 때 좌우로 값이 있고 가운데 빈칸을 얼마나 채워야 하는지를 체크 한줄은 48byte
     */
    public static String PrintPad(String _left, String _right) throws UnsupportedEncodingException {
        String returnStr = "";
        int sizeLeft = getString_euc_kr(_left).length;
        int sizeRight = getString_euc_kr(_right).length;

        char[] buffer;
        if (_left.contains(Constants.PBOLDEND)) {
            buffer = new char[(sizeLeft/3)*2];
        } else {
            buffer = new char[sizeLeft];
        }

        if (_right.contains(Constants.PBOLDEND)) {
            if (sizeRight < 25) {
                buffer = new char[buffer.length  + (sizeRight-12)/3*2 + 8];
            } else {
                switch (sizeRight)
                {
                    case 25:
                        buffer = new char[buffer.length  + (sizeRight-10)/3*2 + 8];
                        break;
                    case 26:
                        buffer = new char[buffer.length  + (sizeRight-8)/3*2 + 8];
                        break;
                    case 27:
                        buffer = new char[buffer.length  + (sizeRight-6)/3*2 + 8];
                        break;
                    case 28:
                        buffer = new char[buffer.length  + (sizeRight-4)/3*2 + 8];
                        break;
                    case 29:
                        buffer = new char[buffer.length  + (sizeRight-2)/3*2 + 8];
                        break;
                    case 30:
                        buffer = new char[buffer.length  + (sizeRight)/3*2 + 8];
                        break;
                    default:
                        buffer = new char[buffer.length  + sizeRight];
                        break;
                }
            }
        }
        else
        {
            buffer = new char[buffer.length  + sizeRight];
        }

        if (buffer.length >= lineCount) {
            returnStr = _left + _right;
            return returnStr;
        }

        returnStr += _left;

        for (int i = buffer.length; i < lineCount; i++)
        {
            returnStr += " ";
        }

        returnStr += _right;

        return returnStr;
    }

    /**
     프린트 할 때 제목:값. 제목:값  형태로 4개를 입력받을 때
     */
    public static String Print4Pad(String _firTitle, String _firValue, String _secTitle, String _secValue) throws UnsupportedEncodingException
    {
        String returnStr = "";
        int sizeFirTitle = getString_euc_kr(_firTitle).length;
        int sizeFirValue = getString_euc_kr(_firValue).length;
        int sizeSecTitle = getString_euc_kr(_secTitle).length;
        int sizeSecValue = getString_euc_kr(_secValue).length;

        char[] bufferLeft;
        if (_firValue.contains(Constants.PBOLDEND)) {
            bufferLeft = new char[sizeFirTitle+sizeFirValue - 8];
        } else {
            bufferLeft = new char[sizeFirTitle+sizeFirValue];
        }

        char[] bufferRight;
        if (_secValue.contains(Constants.PBOLDEND)) {
            bufferRight = new char[sizeSecTitle+sizeSecValue - 8];
        } else {
            bufferRight = new char[sizeSecTitle+sizeSecValue];
        }

        if (bufferLeft.length + bufferRight.length >= 48) {
            returnStr = _firTitle + _firValue + _secTitle + _secValue;
            return returnStr;
        }

        returnStr += _firTitle;
        for (int i = bufferLeft.length; i < 22; i++)
        {
            returnStr += " ";
        }

        returnStr += _firValue;

        for (int i = 0; i < 4; i++)
        {
            returnStr += " ";
        }

        returnStr += _secTitle;
        for (int i = bufferRight.length; i < 22; i++)
        {
            returnStr += " ";
        }

        returnStr += _secValue;

        return returnStr;
    }

    /**
     프린트 시 라인 ----- 을 그린다 48bytes
     */
    public static String PrintLine(String _line)
    {
        String returnStr = "";
        for (int i = 0; i < lineCount/_line.length(); i++)
        {
            returnStr += _line;
        }

        return returnStr;
    }

    /**
     프린트 시 돈을 계산할 때 자릿수에 , 삽입한다
     */
    public static String PrintMoney(String _money)
    {
        if (_money.length() < 4) {
            return _money;
        }

        DecimalFormat myFormatter = new DecimalFormat("###,###");
        String formattedStringPrice = myFormatter.format(Integer.parseInt(_money));
        return formattedStringPrice;
    }

    /**
     프린트 시 강조구문을 만든다
     */
    public static String PrintBold(String _bold) {
        return Constants.PBOLDSTART + _bold + Constants.PBOLDEND;
    }

    /**
     가운데정렬해서 표시
     */
    public static String PrintCenter(String _center) throws UnsupportedEncodingException {
        String returnStr = "";
        int sizeCenter = getString_euc_kr(_center).length;

        char[] buffer;
        if (_center.contains(Constants.PBOLDEND)) {
            buffer = new char[sizeCenter - 8];
        } else {
            buffer = new char[sizeCenter];
        }

        if (buffer.length >= lineCount) {
            returnStr = _center;
            return returnStr;
        }

        for (int i=0; i<(lineCount-buffer.length)/2; i++)
        {
            returnStr += " ";
        }

        returnStr += _center;

        for (int i=0; i<(lineCount-buffer.length)/2; i++)
        {
            returnStr += " ";
        }

        return returnStr;
    }

    ///출력이 가능한 장치인지 체크
    public static String PrintDeviceCheck(Context mCtx)
    {
        String _check = "";
        if (!Setting.getBleIsConnected())
        {
            _check += " 출력 가능 장비 없음. ";
            return _check;
        }

        String TempDeviceName = Setting.getPreference(mCtx,Constants.REGIST_DEVICE_NAME);
        if (TempDeviceName.contains(Constants.C1_KRE_OLD_NOT_PRINT) ||
                TempDeviceName.contains(Constants.KWANGWOO_KRE)) {
            _check += " 출력 가능 장비 없음. ";
        }

        return _check;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] Aes128Dec(byte[] _str, byte[] _key) {
        byte[] key; //16Byte == 128bit 사용할 복호화키
        byte[] initVector = {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f}; //초기화백터
        java.util.Base64.Decoder dec = java.util.Base64.getDecoder();
        try {
            key = _key;
            IvParameterSpec iv = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            KByteArray _dd2 = new KByteArray();
            for (int i= 0; i<100; i++) {
                _dd2.Add(_str[i]);
            }

            String _str4 = Utils.bytesToHex_0xType(_dd2.value());

            byte[] _aa = cipher.doFinal(_str);
            byte[]  Str3 = cipher.update(_str);
            Str3 = cipher.doFinal();
            KByteArray _dd = new KByteArray();
            for (int i= 0; i<100; i++) {
                _dd.Add(Str3[i]);
            }

            String _str3 = Utils.bytesToHex_0xType(_dd.value());
            byte[]  Str4  =cipher.doFinal();
            String str = new String(_str, "UTF-8");
//            byte[]  Str2 = cipher.doFinal(dec.decode(str));
            return Str3;
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    // ==== [AES 복호화(디코딩) 메소드] ====
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String AesDecrypt(byte[] _str, byte[] _key) {
        byte[] key; //16Byte == 128bit 사용할 복호화키
        String initVector = "0123456789abcdef"; //초기화백터
        java.util.Base64.Decoder dec = java.util.Base64.getUrlDecoder();
        Cipher cipher;
        try {

            key = _key;
            byte[] encrypted = _str;
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            cipher = Cipher.getInstance("AES/CFB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[]  Str2 =cipher.doFinal(encrypted);
            byte[] byteT = org.apache.commons.net.util.Base64.decodeBase64(encrypted);
            byte[] byteT2 = com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base64.decodeBase64(encrypted);

            byte[] byteStr = org.apache.commons.codec.binary.Base64.decodeBase64(encrypted);
            byte[] original = cipher.doFinal(  Base64.decode(encrypted, Base64.NO_WRAP)); //base64 to byte
//            byte[] original = cipher.doFinal(dec.decode(encrypted)); //base64 to byte
            byte[]  Str = cipher.doFinal(byteStr);
//            byte[] original = cipher.doFinal(byteStr); //base64 to byte

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

//        byte[] ips;
////        String ips;
//        Key keySpec;
//        try {
//            byte[] keyBytes = new byte[16];
////            byte[] b = key.getBytes("UTF-8");
//            byte[] b = key;
//            System.arraycopy(b, 0, keyBytes, 0, keyBytes.length);
//            SecretKeySpec _keySpec = new SecretKeySpec(keyBytes, "AES");
//            ips = key;
//            keySpec = _keySpec;
//
//            Cipher cipher = Cipher.getInstance("AES/CFB/PKCS5Padding");
//            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ips));
////            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ips.getBytes("UTF-8")));
//            byte[] byteStr = org.apache.commons.codec.binary.Base64.decodeBase64(str);
//            String Str = new String(cipher.doFinal(byteStr), "UTF-8");
//
//            return byteStr;
////            return Str;
//        } catch (Exception e) {
//            Log.d(TAG,e.toString());
//            return null;
//        }
    }
}

