package com.koces.androidpos.sdk.ble;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.ble.BleInterface;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.ble.Paired;
import com.koces.androidpos.R;
import com.koces.androidpos.sdk.van.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public abstract class MyBleListDialog extends Dialog implements View.OnClickListener {
    private Context mContext = null;

//    private Button btn_search = null;
    private Button btn_cancel = null;

    private TextView txt_search_ble_fail = null;

    private TextView txt_device = null;
    private TextView txt_address = null;

    private ListView list_view = null;

    private static ArrayList<Paired> mBleDeviceList = new ArrayList<Paired>();

    private BluetoothAdapter mBtAdapter;
    private PairedListAdapter mBleDeviceAdapter;
    private bleSdk mBleSdk;

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            hideSystemUI();
//        }
//    }
//
//    private void hideSystemUI() {
//        // Enables regular immersive mode.
//        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
//        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        // Set the content to appear under the system bars so that the
//                        // content doesn't resize when the system bars hide and show.
//
//                        // Hide the nav bar and status bar
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
//    }

    public MyBleListDialog(Context context) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars());
//        } else {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }

        setContentView(R.layout.dialog_blelist);
        setCanceledOnTouchOutside(false);
        mContext = context;
        mBleSdk = bleSdk.getInstance();
        initVariable();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            Toast.makeText(mContext, "지원하지 않는 기기입니다.", Toast.LENGTH_LONG);
            dismiss();
            return;
        }

        putBleDeviceAdapter();

        searchDevice();
    }

    private void initVariable() {
//        btn_search = (Button)findViewById(R.id.btn_search);
        btn_cancel = (Button)findViewById(R.id.btn_cancel);

        txt_search_ble_fail = (TextView)findViewById(R.id.txt_search_ble_fail);
        txt_search_ble_fail.setVisibility(View.VISIBLE);
        list_view = (ListView)findViewById(R.id.list_view);

//        btn_search.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_cancel.setVisibility(View.GONE);
    }

    private void putBleDeviceAdapter() {
        if (mBleDeviceAdapter != null) {
            mBleDeviceAdapter = null;
            mBleDeviceList.clear();
        }

        mBleDeviceAdapter = new PairedListAdapter(mContext, R.layout.paired_device_item, mBleDeviceList);
        list_view.setAdapter(mBleDeviceAdapter);

        list_view.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView arg0, int arg1) {
            }
            @Override
            public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {

            }
        });

        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
                stopSearching();

                String bleName = ((TextView)v.findViewById(R.id.txt_device)).getText().toString();
                String bleAddr = ((TextView)v.findViewById(R.id.txt_address)).getText().toString();

                onSelectedBleDevice(bleName, bleAddr);

                dismiss();
            }
        });
    }

    @Override
    public void onClick(View v) {
//        if (CommonUtil.isDoubleClick() == true) {
//            return;
//        }

        switch (v.getId()){
//            case R.id.btn_search:
//                searchDevice();
//                break;

            case R.id.btn_cancel:
                stopSearching();
                onSelectedBleDevice("", "");
                dismiss();
                break;
        }
    }

    private class PairedListAdapter extends ArrayAdapter<Paired> {
        private ArrayList<Paired> items;

        public PairedListAdapter(Context context, int textViewResourceId, ArrayList<Paired> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int fposition = position;
            View v  = convertView;
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.paired_device_item, null);

            Paired al = items.get(fposition);
            if(al != null){
                txt_device = (TextView) v.findViewById(R.id.txt_device);
                txt_address= (TextView) v.findViewById(R.id.txt_address);
                txt_device.setText(al.getDevice());
                txt_address.setText(al.getAddress());
            }
            return v;
        }
    }

    private void searchDevice() {
//        txt_search_ble_fail.setVisibility(View.GONE);
        txt_search_ble_fail.setText("장비를 찾는중입니다. 잠시만 기다려주세요.");
        if (mBleSdk.isScanning()) {
            stopSearching();
            return;

        } else {
//            btn_search.setText("취소");

            mBleDeviceList.clear();
            mBleDeviceAdapter.notifyDataSetChanged();

            showProgressBar();

            final HashMap<String, String> bleList = new HashMap<String, String>();
            mBleSdk.setOnScanResultListener(new BleInterface.ScanResultLinstener() {
                @Override
                public void onScanFinished() {
//                    btn_search.setText("장치검색");
                    hideProgressBar();
                    Set key = bleList.keySet();
                    for (Iterator iterator = key.iterator(); iterator.hasNext();) {
                        String addr = (String) iterator.next();
                        String name = (String) bleList.get(addr);
                        //TODO: 여기서 추가하지 않는다 jiw230223
//                        mBleDeviceList.add(new Paired(name, addr));
                        /** 만일 기존 연결이라면 바로 다이렉트로 연결한다 */
                        if (Setting.getPreference(mContext, Constants.BLE_DEVICE_ADDR) != null) {
                            if (Setting.getPreference(mContext, Constants.BLE_DEVICE_ADDR).equals(addr)) {
                                //TODO: 이미 노티체인지와 스탑서칭은 그 전에 처리해둔다 jiw230223
//                                mBleDeviceAdapter.notifyDataSetChanged();
//                                stopSearching();
                                onFindLastBleDevice(name, addr);
                                dismiss();
//                                mBleSdk.Connect(addr,name);
                                return;
                            }
                        }
                    }

                    btn_cancel.setVisibility(View.VISIBLE);

                    mBleDeviceAdapter.notifyDataSetChanged();
                    txt_search_ble_fail.setText("");
                    if (bleList.size() == 0) {
                        txt_search_ble_fail.setText("장비를 찾을 수 없습니다.");
//                        txt_search_ble_fail.setVisibility(View.VISIBLE);

                    }
                }
                @Override
                public void onResult(int state, BluetoothDevice device, int rssi, String Message) {
                    if ( device.getName() != null) {
                        //21-11-24 //어떤 용도의 SN SP SR RN 을 검색하는지 모르겠다. 일단 코세스는 KRE 로 시작하니 해당으로 검색해본다
                        if(device.getName().contains(Constants.C1_KRE_OLD_NOT_PRINT) ||
                                device.getName().contains(Constants.C1_KRE_OLD_USE_PRINT) ||
                                device.getName().contains(Constants.C1_KRE_NEW) ||
                                device.getName().contains(Constants.ZOA_KRE) || device.getName().contains(Constants.KWANGWOO_KRE)) {
                            bleList.put(device.getAddress(), device.getName());
                            //TODO: 기존에 서치를 완료하고 하던 처리를 하나씩 주소를 받을때마다로 변경 jiw230223
                            txt_search_ble_fail.setText("");
                            for (Paired _device:mBleDeviceList) {
                                if (_device.getAddress().equals(device.getAddress()))
                                {
                                    Log.d("BLEDialog", device.getAddress() + " is equall");
                                    return;
                                }
                            }
                            mBleDeviceList.add(new Paired(device.getName(), device.getAddress()));
                            mBleDeviceAdapter.notifyDataSetChanged();
                        }
                        /** 우심 ble 검색 */
                        else if (device.getName().contains(Constants.WSP) || device.getName().contains(Constants.WOOSIM) ) {
                            bleList.put(device.getAddress(),device.getName());
                            txt_search_ble_fail.setText("");
                            for (Paired _device:mBleDeviceList) {
                                if (_device.getAddress().equals(device.getAddress()))
                                {
                                    Log.d("BLEDialog", device.getAddress() + " is equall");
                                    return;
                                }
                            }
                            mBleDeviceList.add(new Paired(device.getName(), device.getAddress()));
                            mBleDeviceAdapter.notifyDataSetChanged();
                        }

                        /** 만일 기존 연결이라면 바로 다이렉트로 연결한다 */
                        if (Setting.getPreference(mContext, Constants.BLE_DEVICE_ADDR) != null) {
                            if (Setting.getPreference(mContext, Constants.BLE_DEVICE_ADDR).equals(device.getAddress())) {
                                mBleDeviceAdapter.notifyDataSetChanged();
                                stopSearching();
                                return;
                            }
                        }
//                        if (device.getName().contains("SN") || device.getName().contains("SP")) {
//
//                        } else if (device.getName().contains("SR") || device.getName().contains("RN")) {
//                            bleList.put(device.getAddress(),device.getName());
//                        }
                        // bleList.put(address, name);
                    }
                }
            });

            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBleSdk.BleScan(true);
                    mBleSdk.BleWoosimScan(true);
                }
            },1000);
        }
    }

    private void stopSearching() {
        if (mBleSdk.isScanning()) {
            mBleSdk.BleScan(false);
//            btn_search.setText("장치검색");
            hideProgressBar();
        }  else {
            if (mBleSdk.isWoosimScanning()){
                hideProgressBar();
                mBleSdk.BleWoosimScan(false);
            }
        }
    }

    private void showProgressBar() {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        findViewById(R.id.progress).setVisibility(View.GONE);
    }

    protected abstract void onSelectedBleDevice(String bleName, String bleAddr);

    protected abstract void onFindLastBleDevice(String bleName, String bleAddr);

    @Override
    public void onBackPressed() {
        mBleDeviceList.clear();
        mBleDeviceAdapter.notifyDataSetChanged();

        stopSearching();
        onSelectedBleDevice("", "");
        dismiss();
        super.onBackPressed();
    }


}
