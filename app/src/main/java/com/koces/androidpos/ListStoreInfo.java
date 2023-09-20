package com.koces.androidpos;

public class ListStoreInfo {
    private String m_tid;
    private String m_business_number;


    public void ListStoreInfo(String _tid, String _business_num){
        m_tid = _tid;
        m_business_number = _business_num;
    }

    public String GetTid(){
        return m_tid;
    }

    public String GetBusinessNumber(){
        return m_business_number;
    }

}
