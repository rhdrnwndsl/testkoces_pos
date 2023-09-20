package com.koces.androidpos.sdk.SerialPort;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.io.ByteStreams;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import com.koces.androidpos.sdk.AES;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ConnectFTP {
    private final String TAG = "CONNECT_FTP";
    private FTPClient ftpClient;
    private FTPListener m_listener;
    private byte[] mKey;
    public ConnectFTP() {
        ftpClient = new FTPClient();
    }

    public void fileDownload(String ip, int port, String id, String pw, String filename, byte[] _key, FTPListener _listener){
//        ftpClient = new FTPClient();
        mKey = _key;
        setListener(_listener);
        try {
            boolean result = false;
            ftpClient.connect(ip, port);			//FTP 연결
            int reply = ftpClient.getReplyCode();	//응답코드 받기

            if (!FTPReply.isPositiveCompletion(reply)) {	//응답 False인 경우 연결 해제
                ftpClient.disconnect();
                m_listener.onFail("FTP서버 연결실패");
                return;
            }
            if(!ftpClient.login(id, pw)) {
                ftpClient.logout();
                m_listener.onFail("FTP서버 로그인실패");
                return;
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);	//파일타입설정
            ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();			//Active 모드 설정

            result = ftpClient.changeWorkingDirectory("/firmware");	//저장파일경로

            if(!result){	// result = False 는 저장파일경로가 존재하지 않음
                m_listener.onFail("FTP 서버에 firmware 파일경로가 존재하지 않음");
                return;
            } else {
                FTPFile[] ftpfiles = ftpClient.listFiles();  // public 폴더의 모든 파일을 list 합니다
                for (int i = 0; i < ftpfiles.length; i++) {
                    if(ftpfiles[i].getName().equals(filename.substring(10))) {
                        if(ftpfiles[i].getSize() == 0 ) {
                            m_listener.onFail("파일 크기 비정상. 서버에 연락 요망");
                            return;
                        }

//                        WriteToFile(filename.substring(10),null);
//
//                        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//                        String path = root.getAbsolutePath() +"/" + filename.substring(10);
//                        File file1 = new File(path); //path는 파일의 경로를 가리키는 문자열이다.
//
//                        OutputStream os = new FileOutputStream(file1);
//                        result = ftpClient.retrieveFile(filename, os);

                        InputStream io = ftpClient.retrieveFileStream(filename.substring(10));
                        if (io == null) {
                            m_listener.onFail("FTP 서버에서 해당파일을 가져오지 못함");
                            return;
                        }
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                        int nextByte = io.read();
                        while (nextByte != -1) {
                            outputStream.write(nextByte);
                            nextByte = io.read();
                        }

                        byte[] ret = outputStream.toByteArray();
//                        byte[] aes = aes128(mKey,ret);
                        byte[] aesCustom = aesDec(mKey,ret);

                        m_listener.onSuccess(aesCustom, ret);
//                        if (io == null) {
//                            m_listener.onFail("FTP 서버에서 해당파일을 가져오지 못함");
//                            return;
//                        } else {
//                            byte[] _b = downloadFile(io, ftpfiles[i].getSize(),mKey);
//                            if (_b == null) {
//                                m_listener.onFail("파일 크기 비정상. 서버에 연락 요망");
//                            } else {
//                                m_listener.onSuccess(_b);
//                            }
//
//                        }
                        return;
                    }
                }

                m_listener.onFail("FTP 서버에 해당파일이 존재하지 않음");
                return;
            }

        } catch (Exception e) {
//            if(e.getMessage().indexOf("refused") != -1) {
//                m_listener.onFail("FTP서버 연결실패");
//                return;
//            }
            m_listener.onFail("기타에러 : " + e.toString());
            return;
        }
    }

    // FTP 연결 및 설정
    // ip : FTP IP, port : FTP port, id : FTP login Id, pw : FTP login pw, dir : FTP download Path
    public void connect(String ip, int port, String id, String pw, String filename) throws Exception{
        ftpClient = new FTPClient();
        try {
            boolean result = false;
            ftpClient.connect(ip, port);			//FTP 연결
//            ftpClient.setControlEncoding("UTF-8");	//FTP 인코딩 설정
            int reply = ftpClient.getReplyCode();	//응답코드 받기

            if (!FTPReply.isPositiveCompletion(reply)) {	//응답 False인 경우 연결 해제
                ftpClient.disconnect();
                throw new Exception("FTP서버 연결실패");
            }
            if(!ftpClient.login(id, pw)) {
                ftpClient.logout();
                throw new Exception("FTP서버 로그인실패");
            }

//            ftpClient.setSoTimeout(1000 * 10);		//Timeout 설정
//            ftpClient.login(id, pw);				//FTP 로그인
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);	//파일타입설정
//            ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();			//Active 모드 설정

//            FTPFile[] ftpfiles = ftpClient.listFiles("/firmware");  // public 폴더의 모든 파일을 list 합니다
//            if (ftpfiles != null) {
//                for (int i = 0; i < ftpfiles.length; i++) {
//                    if(ftpfiles[i].getName().equals(filename.substring(10))) {
//                        FTPFile file = ftpfiles[i];
//                        WriteToFile(filename.substring(10),null);
//
//                        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//                        String path = root.getAbsolutePath() +"/" + filename.substring(10);
//                        File file1 = new File(path); //path는 파일의 경로를 가리키는 문자열이다.
//
//                        OutputStream os = new FileOutputStream(file1);
//                        result = ftpClient.retrieveFile(filename, os);
//                        if (result) {
//                            System.out.println(file.getLink());  // file.getName(), file.getSize() 등등..
//                        } else {
//                            InputStream io = ftpClient.retrieveFileStream(filename);
//                            if (io == null) {
//                                System.out.println(file.getLink());  // file.getName(), file.getSize() 등등..
//                            } else {
//                                downloadFile(io, 8192);
//                            }
//                        }
//                    }
//                }
//            }

            result = ftpClient.changeWorkingDirectory("/firmware");	//저장파일경로

            if(!result){	// result = False 는 저장파일경로가 존재하지 않음
//                ftpClient.makeDirectory(dir);	//저장파일경로 생성
//                ftpClient.changeWorkingDirectory(dir);
                System.out.println(filename.substring(10));  // file.getName(), file.getSize() 등등..
                throw new Exception("저장파일경로가 존재하지 않음");
            } else {
                FTPFile[] ftpfiles = ftpClient.listFiles();  // public 폴더의 모든 파일을 list 합니다
                for (int i = 0; i < ftpfiles.length; i++) {
                    if(ftpfiles[i].getName().equals(filename.substring(10))) {
                        InputStream io = ftpClient.retrieveFileStream(filename.substring(10));
                        if (io == null) {
                            System.out.println(filename.substring(10));  // file.getName(), file.getSize() 등등..
                        } else {
//                            downloadFile(io, ftpfiles[i].getSize());
                        }
                        return;
                    }
                }

//                WriteToFile(filename.substring(10),null);
//
//                File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//                String path = root.getAbsolutePath() +"/" + filename.substring(10);
//                File file1 = new File(path); //path는 파일의 경로를 가리키는 문자열이다.
//
//                OutputStream os = new FileOutputStream(file1);
//                result = ftpClient.retrieveFile(filename.substring(10), os);
//                if (result) {
//                    System.out.println(filename);  // file.getName(), file.getSize() 등등..
//                } else {
//                    InputStream io = ftpClient.retrieveFileStream(filename.substring(10));
//                    if (io == null) {
//                        System.out.println(filename.substring(10));  // file.getName(), file.getSize() 등등..
//                    } else {
//                        downloadFile(io, 8192);
//                    }
//                }
            }

        } catch (Exception e) {
            if(e.getMessage().indexOf("refused") != -1) {
                throw new Exception("FTP서버 연결실패");
            }
            throw e;
        }
    }

    // FTP 연결해제
    public void disconnect(){
        try {
            if(ftpClient.isConnected()){
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            int a = 0;
            int b = 0;
            a = b;
            b = a;
        }
    }

    private byte[] aesDec(byte[] _key, byte[] _data) {
        byte[] key = _key;
        byte[] inputText = _data;
        AES cipher;
        // CBC test
        byte[] iv = {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f};
        cipher = new AES(key, iv);
        byte[] rec = null;
        try {
            rec = cipher.decrypt(inputText,_key,"CFB",iv);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
//        byte[] res = cipher.CBC_decrypt(inputText);

        return rec;
    }

    private byte[] aes128(byte[] _key, byte[] _data) {
        byte[] key; //16Byte == 128bit 사용할 복호화키
        byte[] initVector = {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f}; //초기화백터
        try {
            key = _key;
            IvParameterSpec iv = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance("AES/CFB/ZeroBytePadding");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            return cipher.doFinal(_data);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private byte[] downloadFile(InputStream is, long fileSize, byte[] _key)
            throws Exception {
        byte[] key; //16Byte == 128bit 사용할 복호화키
        byte[] initVector = {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f}; //초기화백터
        try {
            key = _key;
            IvParameterSpec iv = new IvParameterSpec(initVector);
//            PKCS5Padding , PKCS7Padding, ZeroBytePadding

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] buffer2 = new byte[(int) fileSize];
            byte[] outBuf2 = null;
            if (is.read(buffer2, 0, buffer2.length) == -1) {
                return null;
            }
//            outBuf2 = cipher.update(buffer2,0,buffer2.length);
//
//            byte[] buffer = new byte[(int) fileSize];
//            byte[] outBuf = null;
//            int read = 0;
//            KByteArray _out = new KByteArray();
//            while ((read = is.read(buffer)) != -1) {
//                outBuf = cipher.update(buffer,0,read);
//                if (outBuf != null) {
//                    _out.Clear();
//                    _out.Add(outBuf);
//                }
//            }
////            outBuf = cipher.doFinal();
//            if (outBuf != null) {
//                _out.Clear();
//                _out.Add(outBuf);
//            }

            return buffer2;
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

//        if (is.read(buffer, 0, buffer.length) == -1) {
//            return null;
//        }
//        return buffer; // <-- Here is your file's contents !!!
    }

    public void WriteToFile(String fileName, byte[] content){
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File newDir = new File(path + "/" + fileName);

        try{
            if (!newDir.exists()) {
                newDir.createNewFile();
            }
            FileOutputStream writer = new FileOutputStream(newDir);
            if(content != null) {
                writer.write(content);
            }
            writer.close();
            Log.e("TAG", "Wrote to file: "+fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getSaveFolder( String _dirName)
    {
        File dir;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName + "/");
            if(!dir.exists())
            {
                dir.mkdirs();
            }
        } else {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName + "/");

            if(!dir.exists())
            {
                dir.mkdirs();
            }
        }


        return dir;
    }

    public void setListener(FTPListener listener) { m_listener = listener; }
    public interface FTPListener {
        void onSuccess(byte[] _result, byte[] _original);              // 취소 버튼 클릭 시 호출
        void onFail(String _msg);
    }

}
