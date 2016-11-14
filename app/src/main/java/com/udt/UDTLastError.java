package com.udt;

/**
 * Created by sxh on 2016/11/10.
 */
public  class UDTLastError
{
    public UDTLastError(int code, String errDesc)
    {
        mErrorCode = code;
        mErrorDesc  = errDesc;
    }
    public int mErrorCode;
    public String mErrorDesc;
}
