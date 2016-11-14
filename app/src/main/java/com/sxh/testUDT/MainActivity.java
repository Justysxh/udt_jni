package com.sxh.testUDT;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.udt.IUdpClientEventListener;
import com.udt.R;
import com.udt.UdtClient;
import com.udt.udt;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends Activity implements View.OnClickListener
{

    UdtClient mClient;
    private static final String TAG = "UDT-Activity";

    TextView mConLog;
    EditText mEditMsg;
    TextView mMsgLog;
    EditText mEditIP;
    EditText mEditPort;
    TextView mFilePath;
    TextView mFileStatu;

    private static final int MSG_SEND_FILE_PERCENT = 966;
    private static final int MSG_RECV_FILE_PRRCENT = 415;

    Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == MSG_SEND_FILE_PERCENT)
            {
                String str = String.format("发送:%d/%d  %.02f%%",msg.arg1, msg.arg2,(float)msg.arg1*100.0f/(float)msg.arg2);
                mFileStatu.setText(str);
            }
            else if(msg.what == MSG_RECV_FILE_PRRCENT)
            {
                String str = String.format("接收:%d/%d  %.02f%%",msg.arg1, msg.arg2,(float)msg.arg1*100.0f/(float)msg.arg2);
                mFileStatu.setText(str);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnTest).setOnClickListener(this);
        findViewById(R.id.btnConnect).setOnClickListener(this);
        findViewById(R.id.btnSend).setOnClickListener(this);
        findViewById(R.id.btnFile).setOnClickListener(this);
        mEditIP = (EditText) findViewById(R.id.editIP);
        mEditPort = (EditText) findViewById(R.id.editPort);
        mConLog = (TextView) findViewById(R.id.logView);
        mEditMsg = (EditText) findViewById(R.id.inputMsg);
        mFilePath = (TextView) findViewById(R.id.filePath);
        mFileStatu = (TextView) findViewById(R.id.fileStat);

        mEditMsg.setText(String.format("rand number %d",(int)(Math.random()*1000)));
        mMsgLog  = (TextView) findViewById(R.id.msgLog);
        mClient = new UdtClient();



    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btnConnect:
                v.setEnabled(false);
                onClickConnect();
                break;
            case R.id.btnSend:
                onCLickSend();
                break;
            case R.id.btnFile:
                onChoseFile();
                break;
            default:
                break;
        }
    }

    private void onChoseFile()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult( Intent.createChooser(intent, "Select a File to Upload"), 10001);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",  Toast.LENGTH_SHORT).show();
        }
    }

    String mSendFilePath;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case 10001:
                if (resultCode == RESULT_OK && data !=null) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
                    mSendFilePath = path;
                    mFilePath.setText(path);
                    startSend();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void startSend()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    File file = new File(mSendFilePath);
                    long fileSize = file.length();
                    byte[] buf = new byte[1024*1024];
                    buf[0]=1;
                    buf[1] = (byte)((fileSize>>24)&0xFF);
                    buf[2] = (byte)((fileSize>>16)&0xFF);
                    buf[3] = (byte)((fileSize>>8)&0xFF);
                    buf[4] = (byte)((fileSize)&0xFF);
                    //发送文件大小.
                    mClient.send(buf,0,5);
                    FileInputStream in = new FileInputStream(file);
                    long sendSize = 0;

                    //发送文件数据
                    int readLen = 0;
                    long total = fileSize;



                    while(total>0)
                    {
                        readLen = in.read(buf);
                        readLen = mClient.send(buf,0,readLen);
                        if(readLen<=0)
                        {
                            final int lastLen = readLen;
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    String tStr = mFileStatu.getText().toString();
                                    mFileStatu.setText(tStr+"\nsend error :"+lastLen+" es:"+mClient.getLastErrorString());
                                }
                            });
                            break;
                        }
                        total -= readLen;
                        sendSize += readLen;

                        Message msg = new Message();
                        msg.what = MSG_SEND_FILE_PERCENT;
                        msg.arg1 = (int)sendSize;
                        msg.arg2 = (int)fileSize;
                        mHandler.sendMessage(msg);

                        mClient.waitSendFinish();

                    }
                    mClient.waitSendFinish();
                }
                catch (Exception e)
                {

                }
            }
        }.start();
    }

    private void onCLickSend()
    {
        final String str = mEditMsg.getText().toString();
        mEditMsg.setText(String.format("rand number %d",(int)(Math.random()*1000)));
        new Thread()
        {
            @Override
            public void run()
            {
                mClient.send(str.getBytes());
            }
        }.start();
    }

    private void onClickConnect()
    {
        String strIP = mEditIP.getText().toString();
        String strPort = mEditPort.getText().toString();
        short port = Short.parseShort(strPort);
        mClient.connect("abc", strIP, port, new IUdpClientEventListener()
        {
            @Override
            public void onConnect(boolean bSuccess)
            {
                findViewById(R.id.btnConnect).setEnabled(true);
                mConLog.setText("connect "+bSuccess);
                if(bSuccess)
                {
                    startRecvThread();
                }
            }
        });
    }

    boolean mIsRecvRun=false;
    private  void startRecvThread()
    {
        mIsRecvRun = true;
        new Thread()
        {
            @Override
            public void run()
            {
                final int BUFF_SIZE = 1024*1024;
                byte[] buf = new byte[BUFF_SIZE];
                int ret = 0;
                while(mIsRecvRun)
                {
                    ret = mClient.recv(buf, BUFF_SIZE);
                    if(ret==0)//???? 判断断开, 或者超时
                    {
                        break;
                    }
                    if(buf[0]==1)//文件模式
                    {
                        int len = buf[1]&0xFF;
                        len <<= 8;
                        len |= buf[2]&0xFF;
                        len <<= 8;
                        len |= buf[3]&0xFF;
                        len <<= 8;
                        len |= buf[4]&0xFF;
                        int total = len;
                        try
                        {
                            FileOutputStream fileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory()+"/recv.png",false);
                            if(ret>5)
                            {
                                fileOutputStream.write(buf,5,ret-5);
                            }
                            len -= ret-5;
                            while(len>0)
                            {
                                ret = Math.min(len,BUFF_SIZE);
                                ret = mClient.recv(buf,ret);
                                if(ret>0)
                                {
                                    fileOutputStream.write(buf,0,ret);
                                }
                                len -= ret;

                                Message msg = new Message();
                                msg.what = MSG_RECV_FILE_PRRCENT;
                                msg.arg1 = (int)total-len;
                                msg.arg2 = (int)total;
                                mHandler.sendMessage(msg);
                            }
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            Log.i(TAG, "recv file finish\n");
                        }
                        catch (FileNotFoundException e)
                        {
                            Log.i(TAG, "write failed:"+e.getMessage());
                            break;
                        }
                        catch (IOException e)
                        {
                            Log.i(TAG, "write failed:"+e.getMessage());
                            break;
                        }

                        Log.i(TAG, "recv file finish\n");

                    }
                    else //消息模式
                    {
                        final String strMsg = new String(buf,0,ret);
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                String tStr = mMsgLog.getText().toString();
                                tStr += "\n";
                                tStr += strMsg;
                                mMsgLog.setText(tStr);
                            }
                        });
                    }
                }
                Log.i(TAG,"recv thread finish\n");

            }
        }.start();
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

    private void UdtHoleConnet()
    {
        udt u = new udt();
        u.startup();

        int fdServer = u.socket();
        boolean ret = u.bind(fdServer,"", 9891);
        Log.i(TAG, "bind:"+ret +"\nwait connect...");
        ret = u.connect(fdServer,"192.168.201.135", 6890);
        Log.i(TAG, "connetc server ret: "+ret+"\nwait recv peer...");
        byte[] recvBuff = new byte[0x10];
        u.recv(fdServer,recvBuff,0, 6,0);

        String peerIp = bufferToIp(recvBuff);
        short peerPort = (short)(recvBuff[4]&0xFF);
        peerPort<<=8;
        peerPort |= (short)(recvBuff[5]&0xFF);
        Log.i(TAG, "recv peer: "+peerIp+":"+peerPort);
        int fdPeer = u.socket();
        ret = u.setsockopt(fdPeer,0, udt.UDT_RENDEZVOUS,1);
        u.bind(fdPeer,"", 9891);
        u.listen(fdPeer, 10);
        Log.i(TAG, "wait to connect peer");
        ret = u.connect(fdPeer,peerIp, peerPort);
        Log.i(TAG, "connect peer ret:"+ret);

        String msg = "543212345";
        byte[] msgBytes = msg.getBytes();
        u.send(fdPeer,msgBytes,0,msgBytes.length,0);


        u.close(fdServer);

        u.cleanup();
    }


    // application port from the appclient
    private void appclient() {
        boolean result = false;

        udt u = new udt();
        result = u.startup();
        Log.e(TAG, "startup result = " + result);

        int handle = u.socket();
        Log.e(TAG, "handle = " + handle);


        result = u.connect(handle, "192.100.1.2", 9000);
        Log.e(TAG, "connect result = " + result);

        int data_size = 1024*1024*40;
        byte[] buffer = new byte[data_size];
        //for (int i = 0; i < data_size; i++) buffer[i] = (byte)i;


        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(data_size);
        u.send(handle, byteBuffer.array(),0,4,0);

        int sent_size = 0;
        while (sent_size != data_size)
        {
            int send_count = u.send(handle, buffer, sent_size, data_size-sent_size, 0);
            if(send_count == -1)
            {
                return;
            }
            Log.e(TAG, "send count = " + send_count);
            sent_size += send_count;
        }

        u.recv(handle,buffer,0,10,0);


        /*
        int recv_size = 0;
        byte[] recv_back = new byte[data_size];
        while (recv_size != data_size)
        {
            int size = u.recv(handle, recv_back, recv_size, data_size-recv_size, 0);
            if(size==-1)
            {
                return;
            }
            Log.e(TAG, "   recv size = " + size);
            recv_size += size;
        }

        // verify the send result
        boolean verify_result = true;
        for (int i = 0; i < data_size; i++)
        {
            if (buffer[i] != recv_back[i])
            {
                Log.e(TAG, "Fail on entry " + i);
                verify_result = false;
                break;
            }
        }
        if (verify_result)
            Log.e(TAG, "Verify ok!");
        else
            Log.e(TAG, "Verify fail!");
            //*/

        result = u.close(handle);
        Log.e(TAG, "close result = " + result);

        result = u.cleanup();
        Log.e(TAG, "cleanup result = " + result);
    }





}
