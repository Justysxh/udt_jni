#include <arpa/inet.h>
#include <netdb.h>
#include <stdio.h>

#include "lib/udt.h"
#include "udtApi.h"
#include <android/log.h>

#include <string>


namespace {

    std::string to_string(int value)
    {
        char buffer[100] = {0};
        sprintf(buffer, "%d", value);

        return std::string(buffer);
    }
}

const char TAG[] = "UDT-JNI";

jboolean JNICALL Java_com_udt_udt_startup(JNIEnv *, jobject)
{
    return 0==UDT::startup();
}

jboolean JNICALL Java_com_udt_udt_cleanup(JNIEnv *, jobject)
{
    return 0==UDT::cleanup();
}

jint JNICALL Java_com_udt_udt_socket(JNIEnv *, jobject)
{

    return UDT::socket(AF_INET, SOCK_STREAM, 0);
}


jboolean JNICALL Java_com_udt_udt_connect(JNIEnv* env, jobject thiz, jint handle, jstring ip, jint port)
{

    const char *ip_address = env->GetStringUTFChars(ip, NULL);
    if(ip_address ==0)
    {
        return false;
    }
    sockaddr_in my_addr;
    my_addr.sin_family = AF_INET;
    my_addr.sin_port = htons(port);
    my_addr.sin_addr.s_addr = inet_addr(ip_address);
    memset(&(my_addr.sin_zero), '\0', 8);

    return 0== UDT::connect(handle, (sockaddr *)&my_addr, sizeof(sockaddr_in)) ;

}

jboolean JNICALL Java_com_udt_udt_close(JNIEnv *env, jobject thiz, jint handle)
{
    return UDT::close(handle);
}

jint JNICALL Java_com_udt_udt_send(JNIEnv *env, jobject thiz, jint handle, jbyteArray buffer, jint offset, jint max_send, jint flag)
{
    //
    // TODO: Consider to use GetPrimitiveArrayCritical which though may stall GC. The current implementation
    // may copy the whole buffer.
    //
    jbyte *local_buffer = new jbyte[max_send]; //可能会存在内存碎片和性能瓶颈,还有内存问题(申请不到)
    env->GetByteArrayRegion(buffer, offset, max_send, local_buffer);

    int sent_size = UDT::send(handle, (const char*)local_buffer, max_send, flag);
    if (sent_size == UDT::ERROR)
    {
        __android_log_write(ANDROID_LOG_ERROR, TAG, UDT::getlasterror().getErrorMessage());
    }

    delete local_buffer;
    return sent_size;
}

jint JNICALL Java_com_udt_udt_recv(JNIEnv *env, jobject thiz, jint handle, jbyteArray buffer, jint offset, jint max_read, jint flags)
{
    //
    // TODO: Consider to use GetPrimitiveArrayCritical which though may stall GC. The current implementation
    // may copy the whole buffer.
    //
    jbyte *local_buffer = new jbyte[max_read];

    int recv_size = 0;
    if (UDT::ERROR == (recv_size = UDT::recv(handle, (char*)local_buffer, max_read, flags)))
    {
        recv_size = 0;
        __android_log_write(ANDROID_LOG_ERROR, TAG, UDT::getlasterror().getErrorMessage());
    }

    env->SetByteArrayRegion(buffer, offset, recv_size, local_buffer);

    delete local_buffer;
    return recv_size;
}


jboolean JNICALL Java_com_udt_udt_bind(JNIEnv* env, jobject thiz, jint handle, jstring ip, jint port)
{
    const char *ip_address = env->GetStringUTFChars(ip, NULL);

    long ipAddr = INADDR_ANY;
    if(ip_address!=0 && strlen(ip_address)>0)
    {
        ipAddr = inet_addr(ip_address);
    }

    sockaddr_in my_addr;
    my_addr.sin_family = AF_INET;
    my_addr.sin_port = htons(port);
    my_addr.sin_addr.s_addr = ipAddr;
    memset(&(my_addr.sin_zero), '\0', 8);

    return 0==UDT::bind(handle, (sockaddr*)&my_addr, sizeof(sockaddr_in));
}

//JNIEXPORT jint JNICALL Java_com_udt_udt_bind(JNIEnv * env, jobject thiz, jint udtsock,  jint udpsock)
//{
//    return UDT::bind2(udtsock, udpsock);
//}


jboolean JNICALL Java_com_udt_udt_listen(JNIEnv *env, jobject thiz, jint sock,  jint backLog)
{
    return 0==UDT::listen(sock, backLog);
}

jboolean JNICALL Java_com_udt_udt_setsockopt(JNIEnv *evn, jobject thiz, jint sock,  jint level, jint optname,jint optval)
{
    int val = optval;
    return 0==UDT::setsockopt(sock,level,(UDT::SOCKOPT)optname, &val, sizeof(int));
}

jobject JNICALL Java_com_udt_udt_getLastError(JNIEnv *env, jobject thiz)
{
    jclass stucls = env->FindClass("com/udt/UDTLastError"); //或得Student类引用
//获得得该类型的构造函数  函数名为 <init> 返回类型必须为 void 即 V
    jmethodID constrocMID = env->GetMethodID(stucls,"<init>","(ILjava/lang/String;)V");
    UDT::ERRORINFO& info =  UDT::getlasterror();
    jstring str = env->NewStringUTF(info.getErrorMessage());

    jobject stu_ojb = env->NewObject(stucls,constrocMID,info.getErrorCode(),str);  //构造一个对象，调用该类的构造函数，并且传
    return stu_ojb;
}

JNIEXPORT jobject JNICALL Java_com_udt_udt_waitSendFinish(JNIEnv *evn, jobject thiz, jint sock)
{
    UDT::waitSendFinish(sock);
}