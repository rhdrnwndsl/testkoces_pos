package com.koces.androidpos.sdk.log;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;

import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.core.app.ActivityCompat;

import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class LogFile
{
    private static LogFile instance;
    private static Context m_Context;
    /** 저장할 폴더 명칭 */
    private static String m_logfolder = "KocesICAPP/LOG/";
    private static String m_appidfolder = "KocesICAPP/APPID/";
    private static String m_signfolder = "signfile";

    public LogFile(Context _ctx)
    {
        m_Context = _ctx;
        instance = this;
    }
    public static LogFile getinstance()
    {
        if(instance==null) {
            if (m_Context == null) {
                m_Context = Setting.getTopContext();
                if(m_Context != null) {
                    instance = new LogFile(m_Context);
                }
            } else {
                instance = new LogFile(m_Context);
            }

        }
        return instance;
    }

    int count = 0;
    boolean writeCheck = true;
    /**
     * 받아온 메세지를 로그 파일에 쓴다(파일이 없으면 생성한다
     * @param _log
     */
    public void writeLog(String _log)
    {
        writeCheck = true;
        if(!hasPermissions())
        {
            return;
        }
        try
        {
            //파일생성은 아래로 같은 이름의 파일이 있으면 해당 파일에 이어서 쓰며 없으면 생성후 쓰게 된다
            String _fileName =Utils.getDate("yyMMdd") + "_logdata" + String.valueOf(count) + ".log";
            BufferedWriter bfw = new BufferedWriter(new FileWriter(getSaveFolder(m_logfolder) + "/" + _fileName,true));
            bfw.write(_log);
            bfw.flush();
            bfw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            writeCheck = false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!writeCheck) {
            count++;
            writeLog(_log);
        }
    }

    public boolean hasPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT > 30) {
            permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
            };
        } else {
            permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.BLUETOOTH,
            };
        }

        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(m_Context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 30일이 지난 파일은 폴더에서 제거한다 */
    public void deleteLogFile()
    {
        if(!hasPermissions())
        {
            return;
        }
        // Calendar 객체 생성
        Calendar cal = Calendar.getInstance();
        long todayMil = cal.getTimeInMillis();  // 현재시간
        long oneDayMil = 24*60*60*1000; //일단위

        Calendar fileCal = Calendar.getInstance();
        Date fileDate = null;
        File[] list = getSaveFolder(m_logfolder).listFiles(); //파일리스트가져오기
        if(list != null) {
            for (int j = 0; j < list.length; j++) {

                // 파일의 마지막 수정시간 가져오기
                fileDate = new Date(list[j].lastModified());

                // 현재시간과 파일 수정시간 시간차 계산(단위 : 밀리 세컨드)
                fileCal.setTime(fileDate);
                long diffMil = todayMil - fileCal.getTimeInMillis();

                //날짜로 계산
                int diffDay = (int) (diffMil / oneDayMil);

                // 30일이 지난 파일 삭제
                if (diffDay > 30 && list[j].exists()) {
                    list[j].delete();
                }
            }
        }
    }

    /**
     * 부정취소 방지 키 폴더 저장
     */
    public void writeHardwareKey(String _hardwarekey, boolean _appToapp, String _tid)
    {
        try
        {
            String HardwareKey = _hardwarekey;
            File[] list = getSaveFolder(m_appidfolder).listFiles(); //파일리스트가져오기
            boolean _compare = false;
            if(list != null) {
                for (int j = 0; j < list.length; j++) {
                    if (_appToapp) {
                        if (list[j].getName().contains(Constants.KeyChainAppToApp + _tid) ) {
                            _compare = true;
                        }
                    } else {
                        if (list[j].getName().contains(Constants.KeyChainOriginalApp + _tid)) {
                            _compare = true;
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

                if (!_compare) {
                    if (_appToapp) {
                        //파일생성은 아래로 같은 이름의 파일이 있으면 해당 파일에 이어서 쓰며 없으면 생성후 쓰게 된다
                        String _fileName =Constants.KeyChainAppToApp + _tid;
                        BufferedWriter bfw = new BufferedWriter(new FileWriter(getSaveFolder(m_appidfolder) + "/" + _fileName,false));
                        bfw.write(_base64);
                        bfw.flush();
                        bfw.close();
                    } else {
                        //파일생성은 아래로 같은 이름의 파일이 있으면 해당 파일에 이어서 쓰며 없으면 생성후 쓰게 된다
                        String _fileName =Constants.KeyChainOriginalApp + _tid;
                        BufferedWriter bfw = new BufferedWriter(new FileWriter(getSaveFolder(m_appidfolder) + "/" + _fileName,false));
                        bfw.write(_base64);
                        bfw.flush();
                        bfw.close();
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 저장된 부정취소 방지 키를 읽어온다
     */
    public String readHardwareKey(boolean _appToapp, String _tid)
    {
        String HardwareKey = "";

        /////////////////////// 파일 읽기 ///////////////////////
        // 파일 생성
        String line = null; // 한줄씩 읽기

        try {

            if (_appToapp) {
                //파일생성은 아래로 같은 이름의 파일이 있으면 해당 파일에 이어서 쓰며 없으면 생성후 쓰게 된다
                String _fileName =Constants.KeyChainAppToApp + _tid;
                BufferedReader buf = new BufferedReader(new FileReader(getSaveFolder(m_appidfolder) + "/" + _fileName));
                while((line=buf.readLine())!=null){
                    HardwareKey += line;
                }
                buf.close();
            } else {
                //파일생성은 아래로 같은 이름의 파일이 있으면 해당 파일에 이어서 쓰며 없으면 생성후 쓰게 된다
                String _fileName =Constants.KeyChainOriginalApp + _tid;
                BufferedReader buf = new BufferedReader(new FileReader(getSaveFolder(m_appidfolder) + "/" + _fileName));
                while((line=buf.readLine())!=null){
                    HardwareKey += line;
                }
                buf.close();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HardwareKey = new String(Base64.decode(HardwareKey,Base64.DEFAULT)) ;

        return HardwareKey;
    }

    /**
     * 현재 사용하지 않음
     * @param _res
     */
    public void writeSign(byte[] _res)
    {
        //데이터 bmp 로 변경
        Bitmap _tmpbmp = BitmapFactory.decodeByteArray(_res,0,_res.length);

        //이미지 128,64 사이즈로 변경
        Matrix matrix = new Matrix();
        matrix.postScale((float)128.0/_tmpbmp.getWidth(),(float)64.0/_tmpbmp.getHeight());
        Bitmap _bmp = Bitmap.createBitmap(_tmpbmp,0,0,_tmpbmp.getWidth(),_tmpbmp.getHeight(),matrix,false);

        //메모리 누수가 발생할 수 있다고 한다
   //     _tmpbmp.recycle();

        //파일생성은 아래로 같은 이름의 파일이 있으면 해당 파일에 이어서 쓰며 없으면 생성후 쓰게 된다
        File myDir = getSaveFolder(m_signfolder);
        String _fileName ="signdata.jpg";
        File sign = new File (myDir, _fileName);

        try
        {
            //파일을 빈파일로 생성. 동일파일명이 있다면 생성하지 않는다
            sign.createNewFile();

            //파일을 쓸수있는 스트림을 준비한다
            FileOutputStream out = new FileOutputStream(sign);

            //compress함수로 스트림에 비트맵파일을 저장한다
            _bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
    //        out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 해당하는 폴더가 있는지를 체크하고 없으면 만든다
     * @param _dirName
     * @return
     */
    public File getSaveFolder(String _dirName)
    {
        File dir;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            ContextWrapper cw = new ContextWrapper(m_Context);
//            String fullPath =cw.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString();
//            File directory = cw.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
//            dir =  cw.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName);
            if (_dirName.contains("LOG")) {
                dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName + "/");
            } else {
                dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName + "/");
            }

            if(!dir.exists())
            {
                dir.mkdirs();
            }
        } else {
            if (_dirName.contains("LOG")) {
                dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName + "/");
            } else {
                dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName + "/");
            }
//            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/"+_dirName);
//            dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+_dirName);
            if(!dir.exists())
            {
                dir.mkdirs();
            }
        }


        return dir;
//        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.P)
//        {
//            dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+_dirName);
//            if(!dir.exists())
//            {
//                dir.mkdirs();
//            }
//            return dir;
//        }
//        else
//        {
//            String destPath = m_Context.getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
//            String[] paths = destPath.split("Android", 2);
//            if (paths.length > 0)
//            {
//                final String folderPath = paths[0] + "logfile";
//                dir = new File(folderPath);
//                if (!dir.exists())
//                {
//                    dir.mkdirs();
//                }
//                return dir;
//            }
//            else
//            {
//                return null;
//            }
//        }
    }
}
