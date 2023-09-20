package com.koces.androidpos;

public class ListDeviceReader {
    private String m_date;
    private String m_date_result;
    private String m_date_solt;

    public void ListDeviceReader(String _date, String _result, String _solt){
        m_date = _date;
        m_date_result = _result;
        m_date_solt = _solt;
    }

    public String GetDate(){
        return m_date;
    }

    public String GetDateResult(){
        return m_date_result;
    }

    public String GetDateSolt(){
        return m_date_solt;
    }
}
