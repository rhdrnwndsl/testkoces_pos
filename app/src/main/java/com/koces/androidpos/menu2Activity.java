package com.koces.androidpos;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.koces.androidpos.fragment.frag_device_ble;
import com.koces.androidpos.fragment.frag_device_cat;
import com.koces.androidpos.fragment.frag_device_integrity;
import com.koces.androidpos.fragment.frag_devices_select;
import com.koces.androidpos.fragment.frag_device_line;
import com.koces.androidpos.fragment.frag_print;
import com.koces.androidpos.fragment.frag_manager;
import com.koces.androidpos.fragment.frag_store;
import com.koces.androidpos.fragment.frag_password;
import com.koces.androidpos.fragment.frag_verinfo;
import com.koces.androidpos.fragment.frag_network;
import com.koces.androidpos.fragment.frag_tax;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;

//tablayout 타입의 인터페이스를 만들기 위해서 해당 메뉴를 추가 한다.

/**환경 설정을 구성하기 위한 클래스 <br>
 * fragment를 하위에 두고 작동하는 클래스
 */
public class menu2Activity extends BaseActivity {

    /** 상단 탭메뉴 */
    public TabLayout tabs;
    FragmentManager fm;
    FragmentTransaction tran;
    /** 대리점 등록 */
    frag_store frag_store1;
    /** 장치 설정 */
    frag_device_line frag_device_line1;
    frag_print frag_print1;
    frag_manager frag_manager1;
    frag_device_integrity frag_device_integrity1;
    frag_device_ble frag_device_ble1;
    frag_device_cat frag_device_cat1;
    frag_devices_select frag_device_select;
    frag_password frag_password1;
    frag_verinfo frag_verinfo1;
    frag_network frag_network1;
    frag_tax frag_tax1;
    /** KocesPosSdk - 실제 usb연결. 시리얼통신. 거래 등을 연동하는 곳 */
    bleSdk mbleSdk;
    KocesPosSdk mKocesPosSdk;

    /** 하단 버튼 설정 */
    Button mBtnHome,mBtnStoreInfo,mBtnEnv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu2);
        tabs = findViewById(R.id.tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setFrag(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                setFrag(tab.getPosition());
            }
        });

        frag_store1 = new frag_store();
        frag_device_line1 = new frag_device_line();
        frag_print1 = new frag_print();
        frag_manager1 = new frag_manager();
        frag_device_integrity1 = new frag_device_integrity();
        frag_device_ble1 = new frag_device_ble();
        frag_device_cat1 = new frag_device_cat();
        frag_device_select = new frag_devices_select();
        frag_password1 = new frag_password();
        frag_verinfo1 = new frag_verinfo();
        frag_network1 = new frag_network();
        //22.01.26 테스트를 위해서 추가함 kim.jy
        frag_tax1 = new frag_tax();
        // 처음 시작할 때 최초의 텝 위치가 어디인지 체크
        fm = getFragmentManager();
        tran = fm.beginTransaction();
        tran.replace(R.id.frag_main, frag_device_select);
        tran.commit();
        tabs.selectTab(tabs.getTabAt(0));
        Setting.setTopContext(this);
        actAdd(this);
        if(KocesPosSdk.getInstance()==null) {
            mKocesPosSdk = new KocesPosSdk(this);
            mKocesPosSdk.setFocusActivity(this,null);
        }
        else
        {
            mKocesPosSdk = KocesPosSdk.getInstance();
            mKocesPosSdk.setFocusActivity(this,null);
        }
        mKocesPosSdk.BleregisterReceiver(this);

        mKocesPosSdk.BleConnectionListener(result -> {

            if(result==true)
            {
//                Toast.makeText(mKocesPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).post(()-> {
                    if (Setting.getBleName().equals(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                        BleDeviceInfo();
                    } else {
                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        setBleInitializeStep();
                    }
                });

//                BleDeviceInfo();
            }
            else
            {
                ReadyDialogHide();
            }
        });
        mKocesPosSdk.BleWoosimConnectionListener(result -> {

            if(result==true)
            {
//                Toast.makeText(mKocesPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).post(()-> {
                    if (Setting.getBleName().equals(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                        BleDeviceInfo();
                    } else {
                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        setBleInitializeStep();
                    }
                });

//                BleDeviceInfo();
            }
            else
            {
                ReadyDialogHide();
            }
        });
        /** 하단 버튼 마우스 이벤트 처리
         * 211222 kim.jy */
        mBtnHome = (Button)findViewById(R.id.menu2_btn_gotomain);
        mBtnStoreInfo = (Button)findViewById(R.id.menu2_btn_storeInfo);
        mBtnEnv = (Button)findViewById(R.id.menu2_btn_env);

        mBtnHome.setOnClickListener(v->{
            GotoMain();     //메인화면으로 이동 시키기
        });
        mBtnStoreInfo.setOnClickListener(v->{
            Intent intent = new Intent(getApplicationContext(),StoreMenuActivity.class);
            startActivity(intent);

        });
        mBtnEnv.setOnClickListener(v-> {
            //현재 환경 설정이라서 반응 하지 않는다. 그러나 특정 조건이 주어지면 여기에 코드를 추가 한다.
        });

        int betweenSpace = 1;

        ViewGroup slidingTabStrip = (ViewGroup) tabs.getChildAt(0);

        for (int i=0; i<slidingTabStrip.getChildCount()-1; i++) {
            View v = slidingTabStrip.getChildAt(i);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.rightMargin = betweenSpace;
        }

        Setting.setIsAppForeGround(1);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mKocesPosSdk.BleUnregisterReceiver(this);
        mKocesPosSdk.BleregisterReceiver(Setting.getTopContext());
    }

    public void setFrag(int n){    //프래그먼트를 교체하는 작업을 하는 메소드를 만들었습니다
        fm = getFragmentManager();
        tran = fm.beginTransaction();
        switch (n){
//            case 0:
//                GotoMain();
//                break;
//            case 1:
//                tran.replace(R.id.frag_main, frag_store1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
//                tran.commit();
//                break;
//            case 2:
//                tran.replace(R.id.frag_main, frag_device_select);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
//                tran.commit();
//                break;
            case 0:
                tran.replace(R.id.frag_main, frag_device_select);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
                tran.commit();
                break;
            case 1:
//                tran.replace(R.id.frag_main, frag_network1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
//                tran.commit();
                tran.replace(R.id.frag_main,frag_password1);
                tran.commit();
                break;
            case 2:
                tran.replace(R.id.frag_main, frag_verinfo1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
                tran.commit();
                break;
            case 3: //관리자 들어가기 전의 패스워드 입력창
//                tran.replace(R.id.frag_main,frag_password1);
//                tran.commit();
                break;
            case 4:
                tran.replace(R.id.frag_main, frag_device_integrity1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
                tran.commit();
                break;
            case 5:         //serial 장치
                tran.replace(R.id.frag_main,frag_device_line1);

                tran.commit();
                break;
            case 6:         //ble
                tran.replace(R.id.frag_main, frag_device_ble1);
                tran.commit();
                break;
            case 7:         //cat
                tran.replace(R.id.frag_main,frag_device_cat1);
                tran.commit();
                break;
            case 8:
                tran.replace(R.id.frag_main, frag_manager1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
                tran.commit();
                break;
            case 9:
                tran.replace(R.id.frag_main, frag_network1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
                tran.commit();
                break;
            case frag_tax.fragMentNumber:
                tran.replace(R.id.frag_main, frag_tax1);  //replace의 매개변수는 (프래그먼트를 담을 영역 id, 프래그먼트 객체) 입니다.
                tran.commit();
                break;
        }
    }

    int _bleCount = 0;
    private void setBleInitializeStep()
    {
        ReadyDialogShow(Setting.getTopContext(), "무결성 검증 중 입니다.",0);
        _bleCount += 1;
        /* 무결성 검증. 초기화진행 */
        DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(this, (result, Code, state, resultData) -> {

            if (result.equals("00")) {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
//                ReadyDialogHide();
            } else {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
//                ReadyDialogHide();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (result.equals("00")) {
                        Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();

                    }
                    else if(result.equals("9999"))
                    {
                        mbleSdk = bleSdk.getInstance();
                        if(_bleCount >1)
                        {
                            _bleCount = 0;
                            Toast.makeText(mKocesPosSdk.getActivity(), "네트워크 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                            mbleSdk = bleSdk.getInstance();
                            mbleSdk.DisConnect();
                        }
                        else {
                            mbleSdk = bleSdk.getInstance();
                            mbleSdk.DisConnect();
                            new Handler().postDelayed(()->{
                                ReadyDialogShow(Setting.getTopContext(),"무결성 검증 중 입니다.",0);
                            },200);

                            new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                                mKocesPosSdk.BleConnect(mKocesPosSdk.getActivity(),
                                        Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                        Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                            },500);
                            return;
                        }



                    }
                    else {
                        Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }

                }
            },200);

            if(result.equals("9999"))
            {
                return;
            }
            new Handler().postDelayed(()->{
                //장치 정보 요청
                Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                BleDeviceInfo();
            },500);


        });

        deviceSecuritySDK.Req_BLEIntegrity();   /* 무결성 검증. 키갱신 요청 */


    }

    /**
     * ble 리더기 식별번호 표시를 위한 장치 정보 요청 함
     */
    int _bleDeviceCheck = 0;
    private void BleDeviceInfo(){
        _bleDeviceCheck += 1;
        mKocesPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddHHmmss"), res ->{
            ReadyDialogHide();
            Toast.makeText(mKocesPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
            if(res[3]==(byte)0x15){
                //장비에서 NAK 올라 옮
                return;
            }
            if (res.length == 6) {      //ACK가 6바이트 올라옴
                return;
            }
            if (res.length < 50) {
                return;
            }
            _bleDeviceCheck = 0;
            KByteArray KByteArray = new KByteArray(res);
            KByteArray.CutToSize(4);
            String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
            String serialNum = new String(KByteArray.CutToSize(10));
            String version = new String(KByteArray.CutToSize(5));
            String key = new String(KByteArray.CutToSize(2));
            Setting.mBleHScrKeyYn = key;

            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.REGIST_DEVICE_NAME,authNum);
            Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_VERSION,version);
            Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
            //공백을 제거하여 추가 한다.
            String tmp = authNum.trim();
//            Setting.mAuthNum = authNum.trim(); //BLE는 이것을 쓰지 않는다. 유선이 사용한다
//            mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
            //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(_bleDeviceCheck==1)
                {
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.DisConnect();
                    new Handler().postDelayed(()->{
                        ReadyDialogShow(mKocesPosSdk.getActivity(), "장치 연결 중 입니다.", 0);
                    },200);

                    new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                        mKocesPosSdk.BleConnect(mKocesPosSdk.getActivity(),
                                Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                    },500);
                    return;
                } else if (_bleDeviceCheck > 1)
                {
                    _bleDeviceCheck = 0;
                    Toast.makeText(mKocesPosSdk.getActivity(), "블루투스 통신 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.DisConnect();
                    return;
                }
                else if (_bleDeviceCheck == 0)
                {
                    //정상
                }

            }
        },2000);
    }

    public void GotoMain(){
        Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("AppToApp",2);
        startActivity(intent);
    }
}