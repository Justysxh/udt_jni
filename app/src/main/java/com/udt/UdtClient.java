package com.udt;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by sxh on 2016/11/11.
 */
public class UdtClient
{
    private static final String TAG = "UdtClient";
    private Handler mHandler;
    private udt mClient;
    private int mSock = 0;
    private String mDevId;
    private String mServerIp;
    private short mServerPort=8888;
    private boolean mIsConnecting = false;
    private static final int MSG_CONNECT = 596;
    public UdtClient()
    {
        mHandler = new Handler( Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSG_CONNECT:
                        IUdpClientEventListener listener = (IUdpClientEventListener)msg.obj;
                        listener.onConnect(msg.arg1==1);
                        break;
                }
            }
        };
        mClient = new udt();
    }

    private String bufferToIp(byte[] ip)
    {
        int[] intIp = new int[4];
        for(int i=0; i<4; ++i)
        {
            intIp[i] = ip[i]&0xFF;
        }
        return  String.format("%d.%d.%d.%d",intIp[0],intIp[1],intIp[2],intIp[3]);
    }

    public int send(byte[] buff)
    {
        return send(buff, 0, buff.length);
    }

    public int send(byte[] buf, int offset,int size)
    {
        if(buf==null || buf.length==0 || size==0 || offset<0 || offset>=size || size>buf.length)
        {
            return -1;
        }
        return mClient.send(mSock, buf,offset, size,0);
    }

    public int recv(byte[] buff, int maxSize)
    {
        if(buff==null || buff.length==0 || maxSize==0)
        {
            return -1;
        }
        return mClient.recv(mSock, buff,0, buff.length,0);
    }

    public void waitSendFinish()
    {
        mClient.waitSendFinish(mSock);
    }


    public void connect(String devID,String ip, short port, IUdpClientEventListener listener)
    {
        if(mIsConnecting)
        {
            return;
        }
        mDevId = devID;
        mServerIp = ip;
        mServerPort = port;
        MyThread thread = new MyThread(listener)
        {
            @Override
            public void run()
            {
                doInBackConnect(mListener);
            }
        };
        thread.start();
    }

    public String getLastErrorString()
    {
        return  mClient.getLastError().mErrorDesc;
    }

    class MyThread extends Thread
    {
        protected IUdpClientEventListener mListener;
        MyThread(){super();}
        MyThread(IUdpClientEventListener listener)
        {
            this();
            mListener = listener;
        }

    }

    //p2p连接尝试
    private void doInBackConnect(IUdpClientEventListener listener)
    {
        udt u = mClient;
        int fdServer = u.socket();
        Message msg = new Message();
        msg.what = MSG_CONNECT;
        msg.arg1 = 0;
        msg.obj = listener;
        mIsConnecting = true;
        do
        {
            short localPort = (short) (9001 + (short)(Math.random() * 1000));
            boolean ret = u.bind(fdServer,"", localPort);//绑定本地端口 这里可能会失败, 可能需要多次随机端口,得到可用端口
            if(ret==false)
            {
                break;
            }
            Log.i(TAG, "bind:" + ret + "\nwait connect...");
            ret = u.connect(fdServer, mServerIp, mServerPort); //连接服务器
            if(ret==false)
            {
                break;
            }
            Log.i(TAG, "connetc server ret: "+ret+"\nwait recv peer...");
            byte[] recvBuff = new byte[0x10];
            int recvRet = u.recv(fdServer,recvBuff,0, 6,0);
            //解析数据包, 有可能目标设备未上线
            if(recvRet<=0)//接收错误返回
            {
                break;
            }
            u.close(fdServer);

            String peerIp = bufferToIp(recvBuff);
            short peerPort = (short)(recvBuff[4]&0xFF);
            peerPort<<=8;
            peerPort |= (short)(recvBuff[5]&0xFF);
            Log.i(TAG, "recv peer: "+peerIp+":"+peerPort);
            int fdPeer = u.socket();
            mSock = fdPeer;
            ret = u.setsockopt(fdPeer,0, udt.UDT_RENDEZVOUS,1);
            ret = u.bind(fdPeer,"", localPort);
            if(ret==false)
            {
                Log.i(TAG,u.getLastError().mErrorDesc);
                break;
            }
            if(ret==false)
            {
                Log.i(TAG,u.getLastError().mErrorDesc);
                break;
            }

            Log.i(TAG, "wait to connect peer");
            ret = u.connect(fdPeer,peerIp, peerPort);
            Log.i(TAG, "connect peer ret:"+ret);
            if(ret ==false)
            {
                break;
            }

            msg.arg1 = 1;
        }while(false);

        mIsConnecting = false;
        mHandler.sendMessage(msg);

    }

}
