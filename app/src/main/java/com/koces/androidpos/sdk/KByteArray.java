package com.koces.androidpos.sdk;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Random;

/**
 * Byte를 배열 처럼 사용 할 수 있는 클래스
 * 선언 방식은 ByteArray byteArray = new ByteArray()
 * @author kim.jy 2020-02-25
 */
public class KByteArray {
    byte[] m_res;
    private KByteArray instance;
    private String mBusName;
    public KByteArray()
    {
        mBusName = "";
    }
    public void setMode(String _devicesBusNmae){ mBusName = _devicesBusNmae;}
    public String getBusName(){return mBusName;}
    public KByteArray(byte[] _byteArray)
    {
        addData(_byteArray);
        mBusName = "";
    }

    /**
     * 선언된 ByteArray를 리턴한다.
     * null 아닌 경우만 자신을 리턴 하면 hull 경우에는 내부에서 new로 선언 한다
     * @return ByteArray
     */
    public KByteArray getInstance(){
        if(instance==null){instance = new KByteArray();}
        return instance;
    }

    /**
     *
     * @param _Str
     * @return 추가된 바이트 카운트
     */
    public int Add(String _Str)
    {
        if(_Str.isEmpty() || _Str.equals("") || _Str == null)
        {
            return 0;
        }
        byte[] tmp = _Str.getBytes();
        addData(tmp);

        return tmp.length;
    }

    /**
     * 숫자를 int to byte로 전환 할때
     * @param _int
     */
    public void Add(int _int)
    {
        String tmp = String.valueOf(_int);
        addData(tmp.getBytes());
    }
    public void Add(byte _byte)
    {
        byte[] tmp = new byte[1];
        tmp[0] = _byte;
        addData(tmp);
    }
    public void Add(byte[] _byteArray)
    {
        addData(_byteArray);
    }
    /**
     * 일부 데이터 복사
     * @param _byte
     * @param _size 복사 사이즈
     */
    public void Add(byte[] _byte,int _size)
    {
        byte[] tmp = new byte[_size];
        System.arraycopy(_byte,0,tmp,0,_size);
        addData(tmp);
    }

    /**
     * 특정 구간 복사
     * @param _byte
     * @param _size 복사 사이즈
     * @param _startIndex 시작 위치
     */
    public void Add(byte[] _byte,int _size,int _startIndex)
    {
        byte[] tmp = new byte[_size];
        System.arraycopy(_byte,_startIndex,tmp,0,_size);
        addData(tmp);
    }
    public synchronized void Clear()
    {
        Random rand = new Random();
        if(m_res!=null)
        {
            for(int i=0;i<m_res.length;i++)
            {
                m_res[i] = (byte)rand.nextInt(255);
            }
            Arrays.fill(m_res,(byte)0x01);
            Arrays.fill(m_res,(byte)0x00);
        }
        m_res = null;
    }
    private void addData(byte[] _res)
    {
        if(m_res==null)
        {
            m_res = new byte[_res.length];
            System.arraycopy(_res,0,m_res,0,_res.length);
            return;
        }
        byte[] tmp = new byte[_res.length + m_res.length];
        System.arraycopy(m_res,0,tmp,0,m_res.length);
        System.arraycopy(_res,0,tmp,m_res.length,_res.length);

        m_res = new byte[tmp.length];
        System.arraycopy(tmp,0,m_res,0,tmp.length);
        tmp = null;
    }
    public int getlength()
    {
        if(m_res==null)
        {
            return 0;
        }
        return m_res.length;
    }
    public byte[] value()
    {
        return m_res;
    }

    public byte indexData(int _index)
    {
        try {
            if (m_res == null) {
                return (byte) 0x00;
            }

            if (_index < 0 || m_res.length <= _index) {
                return (byte) 0x00;
            }
//        if(_index == m_res.length)
//        {
//            return (byte)0x00;
//        }

            return m_res[_index];
        }
        catch (Exception ex)
        {
            return (byte)0x00;
        }
        finally {

        }
    }
    public byte[] indexRangeData(int _start,int _end)
    {
        byte[] tmp;
        int count = 1;
        if(_start <= _end)
        {
            count = _end - _start + 1;
            if(count == 0 ){ count =1;}

            tmp = new byte[count];
            if(_start > -1 && _end > -1 && _start < m_res.length && _end < m_res.length)
            {
                System.arraycopy(m_res,_start,tmp,0,count);
                return tmp;
            }
        }
        tmp = new byte[1];
        tmp[0] = (byte)0x00;
        return tmp;
    }

    /**
     * 데이터를 삭제 하지 않고 복사만 한다.
     * @param _size
     * @return
     */
    public byte[] CopyToSize(int _size)
    {
        if(_size<1)
        {
            return null;
        }
        byte[] tmp = new byte[_size];
        if(m_res.length < _size)
        {
            return null;
        }

        System.arraycopy(m_res,0,tmp,0,_size);
        return tmp;
    }

    /**
     * 데이터를 잘라낸다.
     * @param _size
     * @return
     */
    public byte[] CutToSize(int _size)
    {
        if(_size<1)
        {
            return null;
        }
        byte[] tmp = new byte[_size];
        if(m_res.length < _size)
        {
            return null;
        }

        System.arraycopy(m_res,0,tmp,0,_size);
        if(m_res.length == _size)
        {
            Clear();
        }
        else
        {
            byte[] buffer = new byte[m_res.length - _size];
            System.arraycopy(m_res,_size,buffer,0,buffer.length);
            Clear();
            m_res = new byte[buffer.length];
            System.arraycopy(buffer,0,m_res,0,buffer.length);
        }
        return tmp;
    }
}
