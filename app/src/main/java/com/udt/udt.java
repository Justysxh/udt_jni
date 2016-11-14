package com.udt;


public class udt
{



    public native boolean startup();
    public native boolean cleanup();
    public native int socket();
    public native boolean connect(int socket, String ip, int port);
    public native boolean bind(int socket, String ip, int port);
    public native boolean listen(int socket, int backLog);

    public native boolean waitSendFinish(int socket);

    public native boolean close(int socket);
    public native int send(int socket, byte[] buffer, int offset, int size, int flags);
    public native int recv(int socket, byte[] buffer, int offset, int size, int flags);
    public native boolean setsockopt(int socket, int level, int optName, int optVal);

    public native UDTLastError getLastError();


    static {
        System.loadLibrary("udt");
    }


    public static final int UDT_MSS = 0;// the Maximum Transfer Unit
    public static final int  UDT_SNDSYN=1;
    public static final int UDT_RCVSYN=2;         // if receiving is blocking
    public static final int UDT_CC=3;              // custom congestion control algorithm
    public static final int UDT_FC=4;		// Flight flag size (window size)
    public static final int UDT_SNDBUF=5;          // maximum buffer in sending queue
    public static final int UDT_RCVBUF=6;          // UDT receiving buffer size
    public static final int UDT_LINGER=7;          // waiting for unsent data when closing
    public static final int UDP_SNDBUF=8;          // UDP sending buffer size
    public  static final int UDP_RCVBUF=9;          // UDP receiving buffer size
    public  static final int UDT_MAXMSG=10;          // maximum datagram message size
    public static final int UDT_MSGTTL=11;          // time-to-live of a datagram message
    public static final int UDT_RENDEZVOUS=12;      // rendezvous connection mode
    public static final int UDT_SNDTIMEO=13;        // send() timeout
    public static final int UDT_RCVTIMEO=14;        // recv() timeout
    public static final int UDT_REUSEADDR=15;	// reuse an existing port or create a new one
    public static final int UDT_MAXBW=16;		// maximum bandwidth (bytes per second) that the connection can use
    public static final int UDT_STATE=17;		// current socket state, see UDTSTATUS, read only
    public static final int UDT_EVENT=18;		// current avalable events associated with the socket
    public static final int UDT_SNDDATA=19;		// size of data in the sending buffer
    public static final int UDT_RCVDATA=20;		// size of data available for recv
}
