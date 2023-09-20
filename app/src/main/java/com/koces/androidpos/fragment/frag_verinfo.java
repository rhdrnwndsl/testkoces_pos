package com.koces.androidpos.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import android.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.koces.androidpos.R;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class frag_verinfo extends Fragment {
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    View mView;
    public frag_verinfo() {
        // Required empty public constructor
    }

    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_menuActivity = null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_verinfo, container, false);
        init();
        return mView;
    }

    private void init(){
        TextView appid = (TextView) mView.findViewById(R.id.frag_verinfo_appid);
        TextView version = (TextView) mView.findViewById(R.id.frag_verinfo_ver);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                appid.setText(Utils.getAppID());
                version.setText("v1" + Constants.TEST_SOREWAREVERSION.substring(2) + "(20230504)");
            }

        },200);

    }
}