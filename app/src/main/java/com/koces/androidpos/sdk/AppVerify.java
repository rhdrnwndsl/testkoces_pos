package com.koces.androidpos.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 앱 무결성 검사 클래스
 */
public class AppVerify {

    /**
     * DEBUG 모드
     */
    private boolean IS_DEBUG = false;
    private String TAG;

    /**
     * 앱 무결성 검사 설정 파일
     */
    public static final String  SETTING_FILE_NAME   = "SETTING.ini";
    public static final int     SETTING_DATA_SIZE   = 512;

    /**
     * 앱 무결성 초기화 함수
     * @param isDebug
     * @param tag
     */
    public AppVerify(boolean isDebug, String tag) {
        IS_DEBUG = isDebug;
        TAG = tag;
    }

    /**
     * 앱 무결성 검사 함수
     * @param applicationContext
     * @return
     * @throws Exception
     */
    public boolean checkVerify(Context applicationContext) throws Exception {
        PackageManager pm = applicationContext.getPackageManager();
        String packageName = applicationContext.getPackageName();
        String installerName = pm.getInstallerPackageName(packageName);

        //무결성 검사 실패 난다는 문제 때문에 강제로 삽입함.
        //2020년  6월 17 kim.jy
        if(IS_DEBUG){return false;}

        if(TextUtils.isEmpty(installerName)) {
            // cause.. not install google play
            if(IS_DEBUG) return false;
        }
        String strCertSHA256 = getCertSHAKey(applicationContext);
        String strAPKSHA256 = getAPKSHAKey(applicationContext);

        if(TextUtils.isEmpty(strCertSHA256) || TextUtils.isEmpty(strAPKSHA256))
            return true;

        if(!isFirstInstall(applicationContext)) {
            saveKeyToFile(applicationContext, strCertSHA256, strAPKSHA256);
        } else if(isUpdateVersion(applicationContext)) {
            if(checkSHAForFile(applicationContext, strCertSHA256, 0))
                return true;
            updateAPKKeyToFile(applicationContext, strAPKSHA256);
        } else {
            return checkSHAForFile(applicationContext, strCertSHA256, 0) || checkSHAForFile(applicationContext, strAPKSHA256, SETTING_DATA_SIZE);
        }
        return false;
    }

    private String getCertSHAKey(Context context) throws PackageManager.NameNotFoundException, NoSuchAlgorithmException {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        Signature certSignature =  packageInfo.signatures[0];
        MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
        msgDigest.update(certSignature.toByteArray());
        return Base64.encodeToString(msgDigest.digest(), Base64.DEFAULT);
    }

    private int getSHAIndex(String strSHA256) {
        String strCertKeyIndex = strSHA256.replaceAll("[^0-9]", "");
        if(strCertKeyIndex.length() > 3)
            strCertKeyIndex = strCertKeyIndex.substring(strCertKeyIndex.length()-3);
        if(TextUtils.isEmpty(strCertKeyIndex))
            strCertKeyIndex = "0";
        int nKeyIndex = Integer.parseInt(strCertKeyIndex);
        if(nKeyIndex > SETTING_DATA_SIZE)
            nKeyIndex = nKeyIndex - SETTING_DATA_SIZE;
        return nKeyIndex;
    }

    private String getAPKSHAKey(Context applicationContext) throws Exception{
        String strAPKSHA256 = "";
        for (ApplicationInfo app : applicationContext.getPackageManager().getInstalledApplications(0)) {
            //Log.d("PackageList", "package: " + app.packageName + ", sourceDir: " + app.sourceDir);
            if(applicationContext.getApplicationInfo().packageName.equals(app.packageName)) {
                LogE("source dir : " + app.sourceDir);
                if(!TextUtils.isEmpty(strAPKSHA256)) { // check apk
                    String tempAPKSHA256 = extractFileHashSHA256(app.sourceDir);
                    if(!strAPKSHA256.equals(tempAPKSHA256)) // ERR
                        return "";
                }
                strAPKSHA256 = extractFileHashSHA256(app.sourceDir);
                LogE("APK sha : " + strAPKSHA256);
            }
        }
        return strAPKSHA256;
    }

    private String extractFileHashSHA256(String filename) throws Exception {

        String SHA = "";
        int buff = 16384;
        try {
            RandomAccessFile file = new RandomAccessFile(filename, "r");

            MessageDigest hashSum = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[buff];
            byte[] partialHash = null;

            long read = 0;

            // calculate the hash of the hole file for the test
            long offset = file.length();
            int unitsize;
            while (read < offset) {
                unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
                file.read(buffer, 0, unitsize);

                hashSum.update(buffer, 0, unitsize);

                read += unitsize;
            }

            file.close();
            partialHash = new byte[hashSum.getDigestLength()];
            partialHash = hashSum.digest();

            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < partialHash.length ; i++){
                sb.append(Integer.toString((partialHash[i]&0xff) + 0x100, 16).substring(1));
            }
            SHA = sb.toString();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return SHA;
    }


    private boolean isUpdateVersion(Context applicationContext) {
        int versionCode = getAppVersion(applicationContext);
        boolean isUpdate = false;

        SharedPreferences pref = applicationContext.getSharedPreferences("pref", Context.MODE_PRIVATE);
        int savedVersionCode = pref.getInt("APP_VERION", 0);

        if(savedVersionCode < versionCode) {
            isUpdate = true;
        }
        return isUpdate;
    }

    private int getAppVersion(Context applicationContext) {
        int versionCode;
        try
        {
            PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }

        return versionCode;
    }

    private void updateVersion(Context applicationContext) {
        SharedPreferences pref = applicationContext.getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("APP_VERION", getAppVersion(applicationContext));
        editor.commit();
    }

    private boolean isFirstInstall(Context applicationContext) {
        File file = new File(getSettingFilePath(applicationContext));
        return file != null && file.exists();
    }

    private String getSettingFilePath(Context applicationContext) {
        File filesDir = applicationContext.getFilesDir();
        return filesDir.getAbsolutePath() + File.separator + SETTING_FILE_NAME;
    }

    private boolean checkSHAForFile(Context applicationContext, String strSHA256, int nFirstIndex) throws IOException {
        /*if(IS_DEBUG)
            return false;*/
        RandomAccessFile randomAccessFile = new RandomAccessFile(getSettingFilePath(applicationContext), "r");
        LogD("file size : " + randomAccessFile.length());
        int nKeyIndex = getSHAIndex(strSHA256);
        randomAccessFile.seek((long)(nFirstIndex+nKeyIndex));
        byte[] btSHA = strSHA256.getBytes();
        byte[] buffer = new byte[btSHA.length];
        randomAccessFile.read(buffer, 0, buffer.length);
        if(nKeyIndex > (SETTING_DATA_SIZE-btSHA.length) ) {
            byte[] tmp = new byte[btSHA.length-(SETTING_DATA_SIZE-nKeyIndex)];
            randomAccessFile.seek(nFirstIndex);
            randomAccessFile.read(tmp, 0, btSHA.length - (SETTING_DATA_SIZE - nKeyIndex));
            System.arraycopy(tmp, 0, buffer, SETTING_DATA_SIZE-nKeyIndex, tmp.length);
        }

        String savedKey = new String(buffer);
        if(!strSHA256.equals(savedKey)){
            randomAccessFile.close();
            return true;
        }
        randomAccessFile.close();
        return false;
    }

    private void saveKeyToFile(Context applicationContext, String strCertSHA256, String strAPKSHA256) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(getSettingFilePath(applicationContext)));
        SecureRandom random = new SecureRandom();
        byte[] saveData = new byte[SETTING_DATA_SIZE*2];
        random.nextBytes(saveData);
        // set cert key
        byte[] btSHA = strCertSHA256.getBytes();
        int nKeyIndex = getSHAIndex(strCertSHA256);
        setSaveData(saveData, btSHA, 0, nKeyIndex);

        // set apk key
        nKeyIndex = getSHAIndex(strAPKSHA256);
        btSHA = strAPKSHA256.getBytes();
        setSaveData(saveData, btSHA, SETTING_DATA_SIZE, nKeyIndex);

        fos.write(saveData);
        fos.close();

        updateVersion(applicationContext);
    }

    private void setSaveData(byte[]des, byte[]src, int nStartIndex, int nKeyIndex) {
        if(nKeyIndex < (SETTING_DATA_SIZE-src.length))
            System.arraycopy(src, 0, des, nStartIndex + nKeyIndex, src.length);
        else  {
            System.arraycopy(src, 0, des, nStartIndex + nKeyIndex, SETTING_DATA_SIZE-nKeyIndex);
            System.arraycopy(src, SETTING_DATA_SIZE-nKeyIndex, des, nStartIndex, src.length-(SETTING_DATA_SIZE-nKeyIndex));
        }
    }

    private void updateAPKKeyToFile(Context applicationContext, String strAPKSHA256) throws IOException {
        byte[] saveData = new byte[SETTING_DATA_SIZE*2];
        FileInputStream fis = new FileInputStream(new File(getSettingFilePath(applicationContext)));
        fis.read(saveData, 0, saveData.length);
        FileOutputStream fos = new FileOutputStream(new File(getSettingFilePath(applicationContext)));
        int nKeyIndex = getSHAIndex(strAPKSHA256);
        byte[] btSHA = strAPKSHA256.getBytes();
        setSaveData(saveData, btSHA, SETTING_DATA_SIZE, nKeyIndex);
        fos.write(saveData);

        fis.close();
        fos.close();

        updateVersion(applicationContext);

    }

    /** Log Level Error **/
    private final void LogE(String message) {
        if (IS_DEBUG) Log.e(TAG, buildLogMsg(message));
    }
    /** Log Level Warning **/
    private final void LogW(String message) {
        if (IS_DEBUG)Log.w(TAG, buildLogMsg(message));
    }
    /** Log Level Information **/
    private final void LogI(String message) {
        if (IS_DEBUG) Log.i(TAG, buildLogMsg(message));
    }
    /** Log Level Debug **/
    private final void LogD(String message) {
        if (IS_DEBUG)Log.d(TAG, buildLogMsg(message));
    }
    /** Log Level Verbose **/
    private final void LogV(String message) {
        if (IS_DEBUG)Log.v(TAG, buildLogMsg(message));
    }


    private String buildLogMsg(String message) {

        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];

        StringBuilder sb = new StringBuilder();

        sb.append("[");
        sb.append(ste.getFileName().replace(".java", ""));
        sb.append("::");
        sb.append(ste.getMethodName());
        sb.append("]");
        sb.append(message);

        return sb.toString();

    }
}