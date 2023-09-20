package com.koces.androidpos.sdk;

import android.os.Build;
import android.util.Log;
import android.widget.CheckBox;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * MainActivity 루팅 체크 클래스
 */
public class InitialProc {
    public final static String TAG = InitialProc.getClassName();

    /**
     * 루팅 여부 체크 함수
     * @return
     */
    public static boolean CheckSuperUser()
    {
        if(CheckFirst_SudoPermission()){return true;}
        if(CheckSecond_RootFileds()){return true;}
//        if(CheckThird_Tags()){return true;}
        if(CheckFourth_SudoPermission()){return true;}
        return false;
    }

    /**
     * 첫번째 루팅 체크 함수
     * @return
     */
    private static boolean CheckFirst_SudoPermission()
    {
        try {
            Runtime.getRuntime().exec("su");
        }
        catch (Exception ex)
        {
            return false;
        }
        Log.d(TAG,"sudo 권한이 살아 있습니다.");

        return  true;
    }

    /**
     * 두번째 루팅파일 있는 존재 여부 체크
     * @return
     */
    private static boolean CheckSecond_RootFileds()
    {
        String [] files= {"/sbin/su","/system/su","/system/bin/su",
                "/system/sbin/su","/system/xbin/su","/system/xbin/mu",
                "/system/bin/.ext/.su","/system/usr/su-backup","/data/data/com.noshufou.android.su",
                "/system/app/Superuser.apk","/system/app/su.apk","/system/bin/.ext",
                "/system/xbin/.ext","/data/local/bin/su","/data/local/xbin/su",
                "/data/local/su","/system/sd/bin/su","/system/sd/xbin/su",
                "/system/bin/failsafe/su","/su/bin/su"  };

        for(int i=0;i<files.length;i++)
        {
            File f1 = new File(files[i]);
            if(f1!=null && f1.exists())
            {
                Log.d(TAG,"루팅된 파일은 " + f1.getAbsolutePath() + "/" + f1.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * 테스트키로 제작 되어 있는지 체크
     * @return
     */
    private static boolean CheckThird_Tags()
    {
        String buildTags = Build.TAGS;
        if(buildTags!=null && buildTags.contains("test-keys"))
        {
            return true;
        }
        return false;
    }

    /**
     * 네번째 체크 함수로 sudo 퍼미션 여부 체크
     * @return
     */
    private static boolean CheckFourth_SudoPermission()
    {
        Process process = null;
        try
        {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which","su"});
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if(br.readLine()!=null) {

                return true;
            }
        }
        catch (Throwable t)
        {
            if(process!=null)
            {
                process.destroy();
            }
            return false;
        }
        finally {
            if(process!=null)
            {
                process.destroy();
            }
        }
        return true;
    }
    private static String getClassName()
    {
        return "InitialProc";
    }
}
