package com.koces.androidpos;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListStoreAdapter extends BaseAdapter{
    private ArrayList<ListStoreInfo> m_list_store_info = new ArrayList<ListStoreInfo>() ;

    // ListViewAdapter의 생성자
    public ListStoreAdapter() {

    }

    // Adapter에 사용되는 데이터의 개수를 리턴. : 필수 구현
    @Override
    public int getCount() {
        return m_list_store_info.size() ;
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_storeinfo, parent, false);
        }

        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        TextView _number = (TextView) convertView.findViewById(R.id.txt_count_number) ;
        TextView _tid = (TextView) convertView.findViewById(R.id.txt_tid) ;
        TextView _business_number = (TextView) convertView.findViewById(R.id.txt_business_number) ;


        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        ListStoreInfo _list_store_info = m_list_store_info.get(position);

        // 아이템 내 각 위젯에 데이터 반영
        _number.setText(String.valueOf(position+1));
        _tid.setText(_list_store_info.GetTid());
        _business_number.setText(_list_store_info.GetBusinessNumber());
        return convertView;
    }

    // 지정한 위치(position)에 있는 데이터와 관계된 아이템(row)의 ID를 리턴.
    @Override
    public long getItemId(int position) {
        return position ;
    }

    // 지정한 위치(position)에 있는 데이터 리턴
    @Override
    public Object getItem(int position) {
        return m_list_store_info.get(position) ;
    }

    // 아이템 데이터 추가를 위한 함수
    public void addItem(String _tid, String _business_num) {
        ListStoreInfo item = new ListStoreInfo();
        item.ListStoreInfo(_tid, _business_num);

        m_list_store_info.add(item);
    }
}
