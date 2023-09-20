package com.koces.androidpos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.TCPCommand;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.MyBleListDialog;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

/**
 * 사용안함. 프래그먼트의 frag_store 로 변경
 */
public class StoreInfoActivity extends BaseActivity {
    private final static String TAG = StoreInfoActivity.class.getSimpleName();
    /** 가맹점사업자번 */
    private String mStoreNumber;
    /** tid */
    private String mTid;
    /** 시리얼번호 */
    private String mSerialNumber;
    /** 버튼 */
    Button mbtn_home,mbtn_StoreInfo,mbtn_Env,mbtn_getSerial,mbtn_regist;
    /** 가맹점 TID 텍스트뷰 */
    TextView[] mtbx_Tid;
    /** 가맹점 사업자번호 텍스트뷰 */
    TextView[] mtbx_Bsn;
    /** 가맹점 가맹점명 텍스트뷰 */
    TextView[] mtbx_StoreName;
    /** 가맹점 가맹점전화번호 텍스트뷰 */
    TextView[] mtbx_StorePhone;
    /** 가맹점 주소 텍스트뷰 */
    TextView[] mtbx_StoreAddr;
    /** 가맹점 가맹점주 텍스트뷰 */
    TextView[] mtbx_StoreOwner;

    /** 아래 3가지 항목이 각각의 가맹점에 관한 레이아웃들이다. */
    /** 각 가맹점 타이틀 레이아웃 */
    LinearLayout[] mLayout_Title;
    /** 각 가맹점 주요정보 레이아웃 */
    TableLayout[] mStore_Info;
    /** 각 가맹점 정보수정버튼 레이아웃 */
    TableLayout[] mStore_Change;

    /** 복수가망점 스위치 */
    Switch mswitch_multiStore;

    /** 가맹점 정보 수정 버튼 */
    Button mBtn_Edit, mBtn_Edit_1, mBtn_Edit_2, mBtn_Edit_3, mBtn_Edit_4, mBtn_Edit_5;

    /** 가맹점 정보 리스트 버튼 */
    ImageButton mbtn_StoreInfo_list;

    /** 가맹점 등록 입력박스 */
    EditText mEdt_Tid,mEdt_Bsn,mEdt_Srl;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 가맹점등록다운로드를 위해 사용한다 */
    KocesPosSdk mPosSdk;
    /** 가맹점정보수정 */
    StoreInfoDialog mDlgBox;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storeinfo);

        init();
        initStoreInfoValue("");   //화면에 표시 할 값을 표시 한다.
        Setting.setIsAppForeGround(1);
    }

    public void init()
    {
        //현재 최상위 엑티비티를 정의하기 위해서. 22-03-09.jiw
        Setting.setTopContext(this);
        mPosSdk = KocesPosSdk.getInstance();

        /** 화면에 뿌릴 메인텍스트박스 초기화 */
        mtbx_Tid = new TextView[6];
        mtbx_Bsn = new TextView[6];
        mtbx_StoreName = new TextView[6];
        mtbx_StorePhone = new TextView[6];
        mtbx_StoreAddr = new TextView[6];
        mtbx_StoreOwner = new TextView[6];

        /** 화면에 뿌릴 메인텍스트박스 설정 */
        mtbx_Tid[0] = (TextView)findViewById(R.id.storeinfo_tvw_tid);
        mtbx_Bsn[0] = (TextView)findViewById(R.id.storeinfo_tvw_bsn);
        mtbx_StoreName[0] = (TextView)findViewById(R.id.storeinfo_tvw_storename);
        mtbx_StorePhone[0] = (TextView)findViewById(R.id.storeinfo_tvw_storephone);
        mtbx_StoreAddr[0] = (TextView)findViewById(R.id.storeinfo_tvw_storeaddr);
        mtbx_StoreOwner[0] = (TextView)findViewById(R.id.storeinfo_tvw_storeowner);

        /** 화면에 뿌릴 서브텍스트박스1 설정 */
        mtbx_Tid[1] = (TextView)findViewById(R.id.storeinfo_tvw_tid_01);
        mtbx_Bsn[1] = (TextView)findViewById(R.id.storeinfo_tvw_bsn_01);
        mtbx_StoreName[1] = (TextView)findViewById(R.id.storeinfo_tvw_storename_01);
        mtbx_StorePhone[1] = (TextView)findViewById(R.id.storeinfo_tvw_storephone_01);
        mtbx_StoreAddr[1] = (TextView)findViewById(R.id.storeinfo_tvw_storeaddr_01);
        mtbx_StoreOwner[1] = (TextView)findViewById(R.id.storeinfo_tvw_storeowner_01);

        /** 화면에 뿌릴 서브텍스트박스2 설정 */
        mtbx_Tid[2] = (TextView)findViewById(R.id.storeinfo_tvw_tid_02);
        mtbx_Bsn[2] = (TextView)findViewById(R.id.storeinfo_tvw_bsn_02);
        mtbx_StoreName[2] = (TextView)findViewById(R.id.storeinfo_tvw_storename_02);
        mtbx_StorePhone[2] = (TextView)findViewById(R.id.storeinfo_tvw_storephone_02);
        mtbx_StoreAddr[2] = (TextView)findViewById(R.id.storeinfo_tvw_storeaddr_02);
        mtbx_StoreOwner[2] = (TextView)findViewById(R.id.storeinfo_tvw_storeowner_02);

        /** 화면에 뿌릴 서브텍스트박스3 설정 */
        mtbx_Tid[3] = (TextView)findViewById(R.id.storeinfo_tvw_tid_03);
        mtbx_Bsn[3] = (TextView)findViewById(R.id.storeinfo_tvw_bsn_03);
        mtbx_StoreName[3] = (TextView)findViewById(R.id.storeinfo_tvw_storename_03);
        mtbx_StorePhone[3] = (TextView)findViewById(R.id.storeinfo_tvw_storephone_03);
        mtbx_StoreAddr[3] = (TextView)findViewById(R.id.storeinfo_tvw_storeaddr_03);
        mtbx_StoreOwner[3] = (TextView)findViewById(R.id.storeinfo_tvw_storeowner_03);

        /** 화면에 뿌릴 서브텍스트박스4 설정 */
        mtbx_Tid[4] = (TextView)findViewById(R.id.storeinfo_tvw_tid_04);
        mtbx_Bsn[4] = (TextView)findViewById(R.id.storeinfo_tvw_bsn_04);
        mtbx_StoreName[4] = (TextView)findViewById(R.id.storeinfo_tvw_storename_04);
        mtbx_StorePhone[4] = (TextView)findViewById(R.id.storeinfo_tvw_storephone_04);
        mtbx_StoreAddr[4] = (TextView)findViewById(R.id.storeinfo_tvw_storeaddr_04);
        mtbx_StoreOwner[4] = (TextView)findViewById(R.id.storeinfo_tvw_storeowner_04);

        /** 화면에 뿌릴 서브텍스트박스5 설정 */
        mtbx_Tid[5] = (TextView)findViewById(R.id.storeinfo_tvw_tid_05);
        mtbx_Bsn[5] = (TextView)findViewById(R.id.storeinfo_tvw_bsn_05);
        mtbx_StoreName[5] = (TextView)findViewById(R.id.storeinfo_tvw_storename_05);
        mtbx_StorePhone[5] = (TextView)findViewById(R.id.storeinfo_tvw_storephone_05);
        mtbx_StoreAddr[5] = (TextView)findViewById(R.id.storeinfo_tvw_storeaddr_05);
        mtbx_StoreOwner[5] = (TextView)findViewById(R.id.storeinfo_tvw_storeowner_05);

        /** 각 가맹점 타이틀 레이아웃 */
        mLayout_Title = new LinearLayout[6];
        mLayout_Title[0] = findViewById(R.id.store_linear_store_00);
        mLayout_Title[1] = findViewById(R.id.store_linear_store_01);
        mLayout_Title[2] = findViewById(R.id.store_linear_store_02);
        mLayout_Title[3] = findViewById(R.id.store_linear_store_03);
        mLayout_Title[4] = findViewById(R.id.store_linear_store_04);
        mLayout_Title[5] = findViewById(R.id.store_linear_store_05);

        /** 각 가맹점 주요정보 레이아웃 */
        mStore_Info = new TableLayout[6];
        mStore_Info[0] = findViewById(R.id.store_table_store_00);
        mStore_Info[1] = findViewById(R.id.store_table_store_01);
        mStore_Info[2] = findViewById(R.id.store_table_store_02);
        mStore_Info[3] = findViewById(R.id.store_table_store_03);
        mStore_Info[4] = findViewById(R.id.store_table_store_04);
        mStore_Info[5] = findViewById(R.id.store_table_store_05);

        /** 각 가맹점 정보수정버튼 레이아웃 */
        mStore_Change = new TableLayout[6];
        mStore_Change[0] = findViewById(R.id.store_btn_store_00);
        mStore_Change[1] = findViewById(R.id.store_btn_store_01);
        mStore_Change[2] = findViewById(R.id.store_btn_store_02);
        mStore_Change[3] = findViewById(R.id.store_btn_store_03);
        mStore_Change[4] = findViewById(R.id.store_btn_store_04);
        mStore_Change[5] = findViewById(R.id.store_btn_store_05);


        /** 대표가맹점 정보 수정 */
        mBtn_Edit = (Button) findViewById(R.id.storeinfo_btn_edit);
        mBtn_Edit.setOnClickListener(v ->{
            StoreInfoChange(0,mtbx_Bsn[0].getText().toString(),
                mtbx_StoreName[0].getText().toString(),
                mtbx_StorePhone[0].getText().toString(),
                mtbx_StoreAddr[0].getText().toString(),
                mtbx_StoreOwner[0].getText().toString());
        });
        /** 대표가맹점 정보 수정 */
        mBtn_Edit_1 = (Button) findViewById(R.id.storeinfo_btn_edit_01);
        mBtn_Edit_1.setOnClickListener(v ->{
            StoreInfoChange(1,mtbx_Bsn[1].getText().toString(),
                    mtbx_StoreName[1].getText().toString(),
                    mtbx_StorePhone[1].getText().toString(),
                    mtbx_StoreAddr[1].getText().toString(),
                    mtbx_StoreOwner[1].getText().toString());
        });
        /** 대표가맹점 정보 수정 */
        mBtn_Edit_2 = (Button) findViewById(R.id.storeinfo_btn_edit_02);
        mBtn_Edit_2.setOnClickListener(v ->{
            StoreInfoChange(2,mtbx_Bsn[2].getText().toString(),
                    mtbx_StoreName[2].getText().toString(),
                    mtbx_StorePhone[2].getText().toString(),
                    mtbx_StoreAddr[2].getText().toString(),
                    mtbx_StoreOwner[2].getText().toString());
        });
        /** 대표가맹점 정보 수정 */
        mBtn_Edit_3 = (Button) findViewById(R.id.storeinfo_btn_edit_03);
        mBtn_Edit_3.setOnClickListener(v ->{
            StoreInfoChange(3,mtbx_Bsn[3].getText().toString(),
                    mtbx_StoreName[3].getText().toString(),
                    mtbx_StorePhone[3].getText().toString(),
                    mtbx_StoreAddr[3].getText().toString(),
                    mtbx_StoreOwner[3].getText().toString());
        });
        /** 대표가맹점 정보 수정 */
        mBtn_Edit_4 = (Button) findViewById(R.id.storeinfo_btn_edit_04);
        mBtn_Edit_4.setOnClickListener(v ->{
            StoreInfoChange(4,mtbx_Bsn[4].getText().toString(),
                    mtbx_StoreName[4].getText().toString(),
                    mtbx_StorePhone[4].getText().toString(),
                    mtbx_StoreAddr[4].getText().toString(),
                    mtbx_StoreOwner[4].getText().toString());
        });
        /** 대표가맹점 정보 수정 */
        mBtn_Edit_5 = (Button) findViewById(R.id.storeinfo_btn_edit_05);
        mBtn_Edit_5.setOnClickListener(v ->{
            StoreInfoChange(5,mtbx_Bsn[5].getText().toString(),
                    mtbx_StoreName[5].getText().toString(),
                    mtbx_StorePhone[5].getText().toString(),
                    mtbx_StoreAddr[5].getText().toString(),
                    mtbx_StoreOwner[5].getText().toString());
        });


        /** 입력박스 설정 */
        mEdt_Tid = (EditText)findViewById(R.id.storeinfo_edt_tid);
        mEdt_Bsn = (EditText)findViewById(R.id.storeinfo_edt_bsn);
        mEdt_Srl = (EditText)findViewById(R.id.storeinfo_edt_srl);

        /** Button 설정 */
        /** 가맹점정보 메인 ~서브5까지 전체 리스트를 보이기 또는 감추기 */
        mbtn_StoreInfo_list = (ImageButton) findViewById(R.id.store_btn_down);
        mbtn_StoreInfo_list.setOnClickListener(v->{
            if (mLayout_Title[1].getVisibility() == View.VISIBLE)
            {
                StoreInfoViewList(false);
            }
            else
            {
                StoreInfoViewList(true);
            }
        });

        /** 복수가맹점으로 다운로드를 할 지 말지를 설정 */
        mswitch_multiStore = findViewById(R.id.store_switch_multi);
        mswitch_multiStore.setChecked(false);   //디폴트값은 무조건 일반가맹점이다.

        /** 홈으로 이동 */
        mbtn_home = (Button) findViewById(R.id.storeinfo_btn_gotomain);
        mbtn_home.setOnClickListener( v->{ GotoMain(); });  //홈으로 이동

        /** 가맹점정보 화면으로 이동 */
        //자기 자신으로 누른건데 뭐.. 이벤트 연결 안
        mbtn_StoreInfo = (Button) findViewById(R.id.storeinfo_btn_storeInfo);

        /** 환경 설정으로 이동 시키기 */
        mbtn_Env = (Button)findViewById(R.id.storeinfo_btn_env);
        mbtn_Env.setOnClickListener(v->{Gotoenvironment();});

        /** 가맹점 등록 하기 */
        mbtn_regist = (Button)findViewById(R.id.storeinfo_btn_regist);
        mbtn_regist.setOnClickListener(v->{ registration();});

        /** 장치 정보 가져오기*/
        mbtn_getSerial = (Button) findViewById(R.id.storeinfo_btn_getsrl);
        mbtn_getSerial.setOnClickListener(v->{getDeviceSerial();});

        /** 장치 시리얼 번호 가져오기 */
        mSerialNumber = Setting.getPreference(mPosSdk.getActivity(), Constants.REGIST_DEVICE_SN);


        /** 최초 시작시에는 가맹점 리스트는 메인가맹점만 나오게 하기 */
        StoreInfoViewList(false);

        mPosSdk.BleConnectionListener(result -> {

            if(result==true)
            {
//                Toast.makeText(mPosSdk.getContext(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
//                ReadyDialogHide();
                new Handler(Looper.getMainLooper()).post(()-> {
                    if (Setting.getBleName().equals(Setting.getPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                        BleDeviceInfo();
                    } else {
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        setBleInitializeStep();
                    }
                });
            }
            else
            {
                ReadyDialogHide();
            }
        });
        mPosSdk.BleWoosimConnectionListener(result -> {

            if(result==true)
            {
//                Toast.makeText(mPosSdk.getContext(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
//                ReadyDialogHide();
                new Handler(Looper.getMainLooper()).post(()-> {
                    if (Setting.getBleName().equals(Setting.getPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                        BleDeviceInfo();
                    } else {
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(mPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        setBleInitializeStep();
                    }
                });
            }
            else
            {
                ReadyDialogHide();
            }
        });

    }

    /** _see 가 true 이면 모든 가맹점(서브) 정보를 보이게 하고 아니면 감춘다 */
    private void StoreInfoViewList(boolean _see)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(_see)
                {
                    for (int i=1; i< 6; i++)
                    {
                        if (!Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NO + i).equals(""))
                        {
                            mLayout_Title[i].setVisibility(View.VISIBLE);
                            mStore_Info[i].setVisibility(View.VISIBLE);
                            mStore_Change[i].setVisibility(View.VISIBLE);
                        }
                    }
                }
                else
                {
                    for (int i=1; i<6; i++)
                    {
                        mLayout_Title[i].setVisibility(View.GONE);
                        mStore_Info[i].setVisibility(View.GONE);
                        mStore_Change[i].setVisibility(View.GONE);
                    }
                }
            }
        },200);

    }

    private void registration()
    {
        if(!CheckCode())
        {
            return;
        }

        if(mswitch_multiStore.isChecked())
        {
            Toast.makeText(this, "복수 가맹점은 준비 중입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        ReadyDialogShow(this,getResources().getString(R.string.msgbox_regist_n_download_store),0);
//        Setting.Showpopup(activity,getResources().getString(R.string.msgbox_regist_n_download_store),"","");
        new Thread(new Runnable() {
            @Override
            public void run() {

                /** 가맹점다운로드를 시작하면 일단 기존 정보는 모두 지운다 */
                Setting.setPreference(mPosSdk.getActivity(), Constants.TID,"");
                Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_NO,"");
                Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_NM ,"");
                Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_ADDR ,"");
                Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_PHONE ,"");
                Setting.setPreference(mPosSdk.getActivity(),Constants.OWNER_NM ,"");
                mStoreNumber = mEdt_Bsn.getText().toString();
                mTid = mEdt_Tid.getText().toString();
                mSerialNumber = mEdt_Srl.getText().toString();
                for (int i=1; i<6; i++)
                {
                    Setting.setPreference(mPosSdk.getActivity(), Constants.TID + i,"");
                    Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_NO + i,"");
                    Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_NM + i,"");
                    Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_ADDR + i,"");
                    Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_PHONE + i,"");
                    Setting.setPreference(mPosSdk.getActivity(),Constants.OWNER_NM + i,"");
                }

                /* 가맹점등록다운로드 실행 */
                mPosSdk.___registedShopInfo_KeyDownload(
                        mswitch_multiStore.isChecked() ? TCPCommand.CMD_REGISTERED_SHOPS_DOWNLOAD_REQ:TCPCommand.CMD_REGISTERED_SHOP_DOWNLOAD_REQ,
                        mTid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "", "0003", "MDO",
                        mStoreNumber, mSerialNumber, "", Utils.getMacAddress(mPosSdk.getActivity()), _rev -> {
                            ReadyDialogHide();
                            final Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);

                            switch (tp.getResponseCode()) {
                                case TCPCommand.CMD_SHOP_DOWNLOAD_RES:
                                    Res_Dealer_registration(tp.getResData());
                                    break;
                                default:
                                    break;
                            }
                        });
            }
        }).start();
    }

    /**
     * 사업자 번호 유효성 검사. 기타 등등을 체크 한다.
     * @return boolean
     */
    private boolean CheckCode()
    {
        mTid = mEdt_Tid.getText().toString();
        mStoreNumber = mEdt_Bsn.getText().toString();
        mSerialNumber = mEdt_Srl.getText().toString();

        if(mTid.length()!=10) {
            ShowDialog(getResources().getString(R.string.error_store_Tid_number_length_is_not_correct));
            return false;
        }


        if(mSerialNumber.equals(""))
        {
            ShowDialog("장치 시리얼이 입력 되지 않았습니다");
            return false;
        }
        if(mStoreNumber.length()!=10)
        {
            ShowDialog(getResources().getString(R.string.error_store_Business_number_length_is_not_correct));
            return false;
        }
        return true;
    }

    private void GotoMain(){
        Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("AppToApp",2);
        startActivity(intent);
    }

    /** 환경 설정으로 이동 시키기 */
    private void Gotoenvironment(){
        Intent intent = new Intent(getApplicationContext(), menu2Activity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /** 가맹점정보 정보수정및 수동입력 */
    private void StoreInfoChange(int _storeNumber,String _bsn, String _name, String _phone, String _addr, String _owner)
    {
        //_storeNumber = 0 메인, _storeNumber = 1 서브1, _storeNumber=2 서브2, _storeNumber=3 서브3, _storeNumber=4 서브4, _storeNumber=5 서브5
        mDlgBox = new StoreInfoDialog(this, _bsn, _name, _phone,
                _addr, _owner, new StoreInfoDialog.DialogBoxListener() {
            @Override
            public void onClickCancel(String _msg) {
                ShowDialog(_msg);
            }

            @Override
            public void onClickConfirm(String _bsn, String _name, String _phone, String _addr, String _owner) {

                ShowDialog("가맹점정보를 변경하였습니다");
                switch (_storeNumber)
                {
                    case 0:
                        Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_NO,_bsn);
                        Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_NM,_name);
                        Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_ADDR,_addr);
                        Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_PHONE,_phone);
                        Setting.setPreference(mPosSdk.getActivity(),Constants.OWNER_NM,_owner);

                        mStoreNumber = Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NO);

                        mtbx_Bsn[0].setText(mStoreNumber);
                        mtbx_StoreAddr[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_ADDR));
                        mtbx_StoreOwner[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.OWNER_NM));
                        mtbx_StorePhone[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_PHONE));
                        mtbx_StoreName[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NM));
                        return;
                }

                //메인은 위에서 처리하고 나머지 서브는 아래에서 처리한다
                Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_NO + _storeNumber,_bsn);
                Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_NM + _storeNumber,_name);
                Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_ADDR + _storeNumber,_addr);
                Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_PHONE + _storeNumber,_phone);
                Setting.setPreference(mPosSdk.getActivity(),Constants.OWNER_NM + _storeNumber,_owner);

                mtbx_Bsn[_storeNumber].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NO + _storeNumber));
                mtbx_StoreAddr[_storeNumber].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_ADDR + _storeNumber));
                mtbx_StoreOwner[_storeNumber].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.OWNER_NM + _storeNumber));
                mtbx_StorePhone[_storeNumber].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_PHONE + _storeNumber));
                mtbx_StoreName[_storeNumber].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NM + _storeNumber));

            }
        });
        mDlgBox.show();
    }

    private Setting.PayDeviceType checkDevice()
    {
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        return Setting.g_PayDeviceType;
    }

    private void getDeviceSerial()
    {
        switch (checkDevice())
        {
            case NONE:
                ShowDialog("환경설정에서 장치를 설정해 주십시오");
                break;
            case LINES:
            case CAT:
                ShowDialog("블루투스 리더기만 지원 합니다");
                break;
            case BLE:
                mPosSdk.BleIsConnected();
                if(Setting.getBleIsConnected() && !mSerialNumber.equals("")) {
                    ShowDialog(mSerialNumber + " 장치가 연결되어 있습니다");
                    mEdt_Srl.setText(mSerialNumber);
                }else{

                    /**
                     * STATE_OFF = 10;
                     * STATE_TURNING_ON = 11;
                     * STATE_ON = 12;
                     * STATE_TURNING_OFF = 13;
                     * STATE_BLE_TURNING_ON = 14;
                     * STATE_BLE_ON = 15;
                     * STATE_BLE_TURNING_OFF = 16;
                     */

                    switch (mPosSdk.mblsSdk.GetBlueToothAdapter().getState())
                    {
                        case 10:
                        case 13:
                        case 16:
                            Toast.makeText(mPosSdk.getContext(),"블루투스, 위치 설정을 사용으로 해 주세요", Toast.LENGTH_SHORT).show();
                            ReadyDialogHide();
                            return;
                        case 11:
                            break;
                        case 12:
                            break;
                        case 14:
                            break;
                        case 15:
                            break;
                    }
                    MyBleListDialog myBleListDialog = new MyBleListDialog(this) {
                        @Override
                        protected void onSelectedBleDevice(String bleName, String bleAddr) {
                            if (bleName.equals("") || bleAddr.equals(""))
                            {
                                Toast.makeText(mPosSdk.getContext(),"연결을 취소하였습니다.", Toast.LENGTH_SHORT).show();
                                ReadyDialogHide();
                                return;
                            }

                            ReadyDialogShow(mPosSdk.getContext(), "장치에 연결중입니다", 0);
//                    ShowDialog(); //여기서 장치연결중이라는 메세지박스를 띄워주어야 한다. 만들지 않아서 일단 주석처리    21-11-24.진우
                            if (!mPosSdk.BleConnect(mPosSdk.getContext(),bleAddr, bleName)) {
                                Toast.makeText(mPosSdk.getContext(), "리더기 연결 작업을 먼저 진행해야 합니다", Toast.LENGTH_SHORT).show();
                            }
                            Setting.setBleAddr(bleAddr);
                            Setting.setBleName(bleName);
                        }

                        @Override
                        protected void onFindLastBleDevice(String bleName, String bleAddr) {
                            ReadyDialogShow(mPosSdk.getContext(), "장치에 연결중입니다", 0);
                            if (!mPosSdk.BleConnect(mPosSdk.getContext(),bleAddr, bleName)) {
                                Toast.makeText(mPosSdk.getContext(), "리더기 연결에 실패하였습니다", Toast.LENGTH_SHORT).show();
                            }
                            Setting.setBleAddr(bleAddr);
                            Setting.setBleName(bleName);
                        }
                    };

                    myBleListDialog.show();
                }
                break;
        }
    }

    private void setBleInitializeStep()
    {

        ReadyDialogShow(mPosSdk.getActivity(), "무결성 검증 중 입니다.",0);

        /* 무결성 검증. 초기화진행 */
        DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(this, (result, Code, state, resultData) -> {

            if (result.equals("00")) {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
//                    m_menuActivity.ReadyDialogHide();
                Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                String temp = String.valueOf(Setting.g_PayDeviceType);
                Setting.setPreference(this, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
            } else {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
//                    m_menuActivity.ReadyDialogHide();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (result.equals("00")) {
                        Toast.makeText(mPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mPosSdk.getActivity(), "무결성 검증에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }

                }
            },200);

            new Handler().postDelayed(()->{
                //장치 정보 요청
                Toast.makeText(mPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
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
        mPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddHHmmss"), res ->{
            ReadyDialogHide();
            Toast.makeText(mPosSdk.getContext(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
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

            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_NAME,authNum);
            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_VERSION,version);
            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
            //공백을 제거하여 추가 한다.
            String tmp = authNum.trim();
//            Setting.mAuthNum = authNum.trim(); //BLE는 이것을 쓰지 않는다. 유선이 사용한다
            mSerialNumber = Setting.getPreference(mPosSdk.getActivity(), Constants.REGIST_DEVICE_SN);
            mEdt_Srl.setText(mSerialNumber);
//            mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
            //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(_bleDeviceCheck==1)
                {
                    mPosSdk.BleDisConnect();
                    new Handler().postDelayed(()->{
                        ReadyDialogShow(mPosSdk.getActivity(), "장치 연결 중 입니다.", 0);
                    },200);

                    new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                        mPosSdk.BleConnect(mPosSdk.getActivity(),
                                Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                    },500);
                    return;
                } else if (_bleDeviceCheck > 1)
                {
                    _bleDeviceCheck = 0;
                    Toast.makeText(mPosSdk.getActivity(), "블루투스 통신 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                    mPosSdk.BleDisConnect();
                    return;
                }
                else if (_bleDeviceCheck == 0)
                {
                    //정상
                }

            }
        },2000);
    }

    private void initStoreInfoValue(String _serial){
        mStoreNumber = Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NO);
        mTid = Setting.getPreference(mPosSdk.getActivity(), Constants.TID);
        mSerialNumber = Setting.getPreference(mPosSdk.getActivity(), Constants.REGIST_DEVICE_SN);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mEdt_Srl.setText(_serial);
                mtbx_Bsn[0].setText(mStoreNumber);
                mtbx_Tid[0].setText(mTid);
                mtbx_StoreAddr[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_ADDR));
                mtbx_StoreOwner[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.OWNER_NM));
                mtbx_StorePhone[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_PHONE));
                mtbx_StoreName[0].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NM));
                for (int i=1; i<6; i++)
                {
                    mtbx_Tid[i].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.TID + i));
                    mtbx_Bsn[i].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NO + i));
                    mtbx_StoreAddr[i].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_ADDR + i));
                    mtbx_StoreOwner[i].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.OWNER_NM + i));
                    mtbx_StorePhone[i].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_PHONE + i));
                    mtbx_StoreName[i].setText(Setting.getPreference(mPosSdk.getActivity(), Constants.STORE_NM + i));
                }
            }
        },200);

        if (mTid.equals("") && mStoreNumber.equals("")) {
            Log.d(TAG,"가맹점 정보가 없음");
            return;
        }

    }

    /**
     * 서버 대리점 response data 처리 함수
     * @param _data
     */
    private synchronized void Res_Dealer_registration(List<byte[]> _data)
    {
        final List<byte[]> data = _data;
        String code = new String(data.get(0));
        String codeMessage = "";
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
        String Working_Key_Index = Constants.WORKING_KEY_INDEX;
        String Working_Key= "";
        String TMK = "";
        String PointCardCount = "";
        String PointCardInfo= "";
        String Etc= "";
        String HardwareKey = "";
        String ShpCount = "";
        try {
            codeMessage = Utils.getByteToString_euc_kr(data.get(1));
            byte[] responseData = data.get(2);
            creditConnNumberA1200 = Utils.ByteArrayToString(data.get(3));
            creditConnNumberB1200 = Utils.ByteArrayToString(data.get(4));
            etcConnNumberA1200 = Utils.ByteArrayToString(data.get(5));
            etcConnNumberB1200 = Utils.ByteArrayToString(data.get(6));
            creditConnNumberA2400 = Utils.ByteArrayToString(data.get(7));
            creditConnNumberB2400 = Utils.ByteArrayToString(data.get(8));
            etcConnNumberA2400 = Utils.ByteArrayToString(data.get(9));
            etcConnNumberB2400 = Utils.ByteArrayToString(data.get(10));

            ASPhoneNumber = Utils.ByteArrayToString(data.get(11));//A/S(가맹점)전화번호

            //복수가맹점인경우
            if(mswitch_multiStore.isChecked())
            {

            }
            else
            {
                StoreName = Utils.getByteToString_euc_kr(data.get(12));//가맹점 이름
                StoreBusinessNumber = Utils.ByteArrayToString(data.get(13));//가맹점 사업자 번호
                StoreCEOName = Utils.getByteToString_euc_kr(data.get(14));//대표자명
                StoreAddr = Utils.getByteToString_euc_kr(data.get(15));   //주소
                StorePhoneNumber = Utils.ByteArrayToString(data.get(16)); //가맹점 전화번호
                Working_Key_Index = Utils.ByteArrayToString(data.get(17)); //working key index
                Working_Key = Utils.ByteArrayToString(data.get(18));
                TMK = Utils.ByteArrayToString(data.get(19));
                PointCardCount = Utils.ByteArrayToString(data.get(20));
                PointCardInfo = Utils.getByteToString_euc_kr(data.get(21));
                Etc = Utils.ByteArrayToString(data.get(22));
                HardwareKey = Utils.ByteArrayToString(data.get(23));
            }



        }
        catch (UnsupportedEncodingException ex)
        {

        }
        if(code.equals("0000"))
        {
            ShowDialog("가맹점 등록 성공");
            //tid 사업자번호를 비롯한 정보를 로컬저장소에 저장한다
            Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_NO,mStoreNumber);
            Setting.setPreference(mPosSdk.getActivity(), Constants.TID,mTid);
            Setting.setPreference(mPosSdk.getActivity(), Constants.REGIST_DEVICE_SN,mSerialNumber);
            Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_NM,StoreName);
            Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_ADDR,StoreAddr);
            Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_PHONE,StorePhoneNumber);
            Setting.setPreference(mPosSdk.getActivity(),Constants.OWNER_NM,StoreCEOName);
            Utils.setHardwareKey(mPosSdk.getActivity(),HardwareKey,false,mTid);
            /* 가맹점정보를 sqlite에 기록한다 */
            mPosSdk.setSqliteDB_StoreTable(creditConnNumberA1200,creditConnNumberB1200,etcConnNumberA1200,etcConnNumberB1200,creditConnNumberA2400,creditConnNumberB2400,
                    etcConnNumberA2400,etcConnNumberB2400,ASPhoneNumber,StoreName,mTid,StoreBusinessNumber,StoreCEOName,StoreAddr,StorePhoneNumber,Working_Key_Index,Working_Key,
                    TMK,PointCardCount,PointCardInfo,Etc);
            //fstore_txt_name.setText(StoreName);

            //정상적으로 정보를 화면에 표시 한다.
            initStoreInfoValue(mSerialNumber);
        }
        else
        {
            //TID를 등록하지 못한 경우에는 기존에 저장 되어 있는 preference 값을 삭제 한다.
            Setting.setPreference(mPosSdk.getActivity(), Constants.STORE_NO,"");
            Setting.setPreference(mPosSdk.getActivity(), Constants.TID,"");
//            Setting.setPreference(mPosSdk.getActivity(), Constants.REGIST_DEVICE_SN,"");
            Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_NM,"");
            Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_ADDR,"");
            Setting.setPreference(mPosSdk.getActivity(),Constants.STORE_PHONE,"");
            Setting.setPreference(mPosSdk.getActivity(),Constants.OWNER_NM,"");
            initStoreInfoValue("");
            ShowDialog("가맹점 등록 실패" + "\n" + code + ", " + codeMessage);
        }
    }

    /**
     * 화면하단에 한줄자리 메세지표시 후 사라짐
     * @param _str
     */
    private void ShowDialog(String _str)
    {
        Toast.makeText(mPosSdk.getActivity(),_str,Toast.LENGTH_SHORT).show();
    }
}