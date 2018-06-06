package WedoneBioVein;

import android.app.Activity;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Vector;

import android_serialport_api.SerialPort;


class BioVeinDevice{
    //常熟变量
    public static int FV_DEFAULT_UART_BAUDRATE  = 115200;   //UART串口默认波特率

    //管理用成员变量
    public String mPortFilePath = null;
    public SerialPort mSerialPort = null;
    public OutputStream mOutputStream = null;
    public InputStream mInputStream = null;
    public int mDeviceId = -1;

    public int open(){
        if(null != mSerialPort) {
            return SdkMain.FV_ERRCODE_SUCCESS;
        }

        try {
            //打开UART串口设备
            mSerialPort = new SerialPort(new File(mPortFilePath), BioVeinDevice.FV_DEFAULT_UART_BAUDRATE, 0);
        }
        catch (IOException ioe){
            //打开UART串口设备发生IO错误
            SdkMain.DebugStringPrint("FV_EnumDevice: IO error on " + mPortFilePath);
            mSerialPort = null;
            return SdkMain.FV_ERRCODE_DEVICE_NOT_EXIST;
        }
        catch (SecurityException se){
            //打开UART串口设备权限不够
            SdkMain.DebugStringPrint("FV_EnumDevice: Security error on " + mPortFilePath);
            mSerialPort = null;
            return SdkMain.FV_ERRCODE_DEVICE_NOT_EXIST;
        }
        mInputStream = mSerialPort.getInputStream();
        mOutputStream = mSerialPort.getOutputStream();

        return SdkMain.FV_ERRCODE_SUCCESS;
    }

    public int close(){
        if(null == mSerialPort) {
            return SdkMain.FV_ERRCODE_SUCCESS;
        }
        mSerialPort.close();

        mSerialPort = null;
        mInputStream = null;
        mOutputStream = null;
        mDeviceId = -1;

        return SdkMain.FV_ERRCODE_SUCCESS;
    }
};

/**
 * Created by Achang on 2016/2/5.
 */
public class SdkMain extends Activity implements Runnable {

    //常量定义：通用
    public static String FV_CONST_SDK_VERSION   = "1.0.1"; //SDK版本号
    public static String FV_CONST_DEBUG_TAG     = "WEDONE"; //模板数据长度
    public static int FV_CONST_SDK_VERSION_LEN  = 32; //SDK版本字符的长度
    public static int FV_CONST_FEATURE_LEN      = 512; //模板数据长度
    public static int FV_CONST_VEINIMG_WIDTH    = 352; //微盾指静脉图片的默认宽度
    public static int FV_CONST_VEINIMG_HEIGHT   = 240; //微盾指静脉图片的默认高度
    public static int FV_CONST_IMAGE_MAX_SIZE   = (FV_CONST_VEINIMG_WIDTH*FV_CONST_VEINIMG_HEIGHT + 1024 + 54); //指静脉图片数据最大长度
    public static int FV_CONST_USER_HDR_SIZE    = 124;//用户信息头结构体长度
    public static int FV_CONST_DEVSERIAL_LEN    = 32; //设备序列号的长度
    public static int FV_CONST_MSG_BUFF_LEN     = 21000;
    public static int FV_CONST_MSG_MIN_LEN      = 32;
    public static int FV_CONST_DEFAULT_TIMEOUT  = 1500;

    //常量定义：设备工作模式
    public static byte FV_CONST_WORKMODE_VERIFY   = 0;
    public static byte FV_CONST_WORKMODE_REGISTER = 1;

    //常量定义：消息ID
    public static byte FV_MSGID_QUERY            = (byte)0x80;
    public static byte FV_MSGID_CHANGE_SETTING   = (byte)0x81;
    public static byte FV_MSGID_GET_SETTING      = (byte)0x82;
    public static byte FV_MSGID_REGISTER         = (byte)0x83;
    public static byte FV_MSGID_GETIMAGE         = (byte)0x85;
    public static byte FV_MSGID_GET_TEMPLATE     = (byte)0x88;
    public static byte FV_MSGID_IDENTIFY         = (byte)0x8B;
    public static byte FV_MSGID_REGISTER_BEGIN   = (byte)0x90;
    public static byte FV_MSGID_REGISTER_END     = (byte)0x91;
    public static byte FV_MSGID_DOWNLOAD_USRHDR  = (byte)0x98;
    public static byte FV_MSGID_DOWNLOAD_TEMPLATE= (byte)0xB5;
    public static byte FV_MSGID_FINGER_STATUS    = (byte)0xC3;
    public static byte FV_MSGID_SERVER_MATCH_TEMPLATE   = (byte)0xF1;
    public static byte FV_MSGID_DEVICE_MATCH_TEMPLATE   = (byte)0xF3;

    //常量定义：设备设置参数类型
    public static short FV_SETTYPE_WORKMODE   = 15;
    public static short FV_SETTYPE_READSERIAL = 18;

    //常量定义：SDK错误代码
    public static int FV_ERRCODE_SUCCESS              = 0;   //成功、正常
    public static int FV_ERRCODE_WRONG_PARAMETER      = -1;  //参数错误
    public static int FV_ERRCODE_MEMORY_ALLOC_FAIL    = -2;  //内存分配失败
    public static int FV_ERRCODE_FUNCTION_INVALID     = -3;  //功能无效
    public static int FV_ERRCODE_DEVICE_NOT_EXIST     = -4;  //设备不存在
    public static int FV_ERRCODE_DEVICE_NOT_INITED    = -5;  //设备未初始化
    public static int FV_ERRCODE_INVALID_ERR_CODE     = -6;  //无效错误代码
    public static int FV_ERRCODE_UNKNOWN              = -9;  //未知错误
    public static int FV_ERRCODE_MATCH_FAILED         = -10; //验证失败

    private SerialPortFinder mSerialPortFinder = null;
    private Vector<BioVeinDevice> mBioVeinDevice = new Vector<BioVeinDevice>();;

    //以下为SDK部分的代码
    public static long SDK_ERRCODE_SUCCESS = 0;     //成功
    public static long SDK_ERRCODE_NOTINITED = 1;   //未初始化
    public static long SDK_ERRCODE_NOTEXIST = 2;    //设备不存在
    public static long SDK_ERRCODE_OPENFAIL = 3;    //打开设备端口失败
    public static long SDK_ERRCODE_NOTOPENED = 4;

    public static long SDK_ERRCODE_DEVICEREMOVED = 5;  //设备已经拔出
    public static long SDK_ERRCODE_SENDFAILED = 6;      //发送失败
    public static long SDK_ERRCODE_RECVFAILED = 7;      //数据接收失败
    public static long SDK_ERRCODE_INVALIDDATA = 8;     //无效的数据
    public static long SDK_ERRCODE_MSGMISMATCH = 9;     //消息应答不匹配，通讯冲突
    public static long SDK_ERRCODE_WRONGTEMPLATE = 10;  //模版错误
    public static long SDK_ERRCODE_WRONGPARAMETER = 11; //参数错误

    //各个接口返回错误代码的偏移量
    public static long SDK_ERRCODE_IF_GETDEVICEID = 0x0100;
    
    public static short FV_SETTYPE_LED_BEEP  = 23;

    public static short SDK_BLINK_GREEN             = 0; //只亮绿灯
    public static short SDK_BLINK_RED               = 1; //只亮红灯
    public static short SDK_BLINK_GREEN_BEEP        = 2; //绿灯 + BEEP
    public static short SDK_BLINK_RED_BEEP          = 3; //红灯 + BEEP
    public static short SDK_BLINK_RED_GREEN         = 4; //红绿灯同时点亮
    public static short SDK_BLINK_RED_GREEN_BEEP    = 5; //红绿灯同时点亮 + BEEP
    public static short SDK_BLINK_BEEP              = 6; //只响BEEP

    public static void DebugStringPrint(String debugString){
        Log.d(FV_CONST_DEBUG_TAG, debugString);
    }

    public static void DebugAsciiPrint(byte[] bDataBuf, short sDataLen){
        String strDataLogTemp = new String(bDataBuf);
        String strDataLog = strDataLogTemp.substring(0, sDataLen - 1);
        Log.d(FV_CONST_DEBUG_TAG, "" + strDataLog);
    }

    public static void DebugHexPrint(byte[] bDataBuf, short sDataLen){
        String strDataLogTemp = new String();
        String strDataLog = new String();
        for(int nByteCnt = 0; nByteCnt < sDataLen; nByteCnt++){
            strDataLogTemp = Integer.toHexString(((int) bDataBuf[nByteCnt] & 0xFF)).toUpperCase();
            if(2 > strDataLogTemp.length()){
                strDataLogTemp = "0" + strDataLogTemp;
            }
            strDataLog += strDataLogTemp;
        }
        Log.d(FV_CONST_DEBUG_TAG, "" + strDataLog);
    }

    public static int ByteArrayReset(byte[] src, int bytes){
        int resetLen = src.length;
        if(resetLen > bytes)resetLen = bytes;
        for(int nByteCnt = 0; nByteCnt < resetLen; nByteCnt++){
            src[nByteCnt] = 0;
        }
        return bytes;
    }

    public static int ByteArrayCopy(byte[] dst, byte[] src, int bytes){
        int copyLen = src.length;
        if(copyLen > bytes)copyLen = bytes;
        if(copyLen > dst.length)copyLen = dst.length;
        for(int nByteCnt = 0; nByteCnt < copyLen; nByteCnt++){
            dst[nByteCnt] = src[nByteCnt];
        }
        return bytes;
    }

    public static int CharArrayReset(char[] src, int bytes){
        int resetLen = src.length;
        if(resetLen > bytes)resetLen = bytes;
        for(int nByteCnt = 0; nByteCnt < resetLen; nByteCnt++){
            src[nByteCnt] = 0;
        }
        return bytes;
    }

    public static int CharArrayCopy(char[] dst, char[] src, int bytes){
        int copyLen = src.length;
        if(copyLen > bytes)copyLen = bytes;
        if(copyLen > dst.length)copyLen = dst.length;
        for(int nByteCnt = 0; nByteCnt < copyLen; nByteCnt++){
            dst[nByteCnt] = src[nByteCnt];
        }
        return bytes;
    }

    // char[]转byte[]
    public static byte[] Chars2Bytes (char[] chars) {
        Charset cs = Charset.forName ("UTF-8");
        CharBuffer cb = CharBuffer.allocate (chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    // byte[]转char[]
    public static char[] Bytes2Chars (byte[] bytes) {
        Charset cs = Charset.forName ("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate (bytes.length);
        bb.put (bytes);
        bb.flip ();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    //从向量表中获取设备类。
    //action: 1=从向量表中移除，2=从向量表中复制
    private BioVeinDevice getBioVeinDevice(byte[] devId, int action){
        if(null == devId || (1 != action && 2 != action)){
            return null;
        }

        String devIdString = new String(devId);
        devIdString = devIdString.trim().toUpperCase();
        if(0 == devIdString.length()){
            return null;
        }

        int devNum = mBioVeinDevice.size();
        BioVeinDevice bioVeinDevice = null;
        for(int devIdx = 0; devIdx < devNum; devIdx++){
            bioVeinDevice = mBioVeinDevice.get(devIdx);
            if(0 != bioVeinDevice.mPortFilePath.trim().toUpperCase().compareTo(devIdString)){
                continue;
            }
            if(1 == action){
                mBioVeinDevice.remove(devIdx);
            }
            return bioVeinDevice;
        }
        return null;
    }

    private int TransferMsgData(BioVeinDevice bioVeinDevice, VeinMsg vmMsg, int nTimeout){
        byte bMsgIdSent, bMsgIdRecv;
        int nMsgIdSent, nMsgIdRecv;
        int nRepeatCnt = 0;
        int availableByteCount;
        int receivedByteCnt;
        boolean msgAvailable;
        int nErrCode;
        long lDataLen;
        boolean msgStartReceived, msgEndReceived;

        if (null == bioVeinDevice || null == vmMsg || null == bioVeinDevice.mSerialPort) {
            return FV_ERRCODE_WRONG_PARAMETER;
        }
        DebugStringPrint("TransferMsgData: dev=" + bioVeinDevice.mPortFilePath);

        bMsgIdSent = vmMsg.GetMsgId();

        //发送数据
        lDataLen = FV_CONST_MSG_BUFF_LEN;
        byte bDataBuff[] = new byte[(int)lDataLen];
        if(bDataBuff==null){
            return FV_ERRCODE_MEMORY_ALLOC_FAIL;
        }
        vmMsg.SetDevId((long)bioVeinDevice.mDeviceId);
        lDataLen = vmMsg.GetMsgData(bDataBuff, (short)lDataLen);
        DebugAsciiPrint(bDataBuff, (short) lDataLen);
        try {
            bioVeinDevice.mOutputStream.write(bDataBuff, 0, (int)lDataLen);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //接收应答数据
        while(true) {
            nRepeatCnt++;
            if(3 < nRepeatCnt)break;//最多重试接受3次数据
            receivedByteCnt = 0;
            msgAvailable = false;
            msgStartReceived = false;
            msgEndReceived = false;
            availableByteCount = 0;
            while (true) {
                try{
                    availableByteCount = (short) bioVeinDevice.mInputStream.available();//检查缓冲区中是否有数据可以读取
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugStringPrint("TransferMsgData: Exception on available check!");
                    return FV_ERRCODE_INVALID_ERR_CODE;
                }
                if(0 < availableByteCount){ //有数据的话进行读取
                    //接收数据
                    try{
                        availableByteCount = bioVeinDevice.mInputStream.read(bDataBuff, receivedByteCnt, availableByteCount);
                    } catch (Exception e) {
                        e.printStackTrace();
                        DebugStringPrint("TransferMsgData: Exception on read !");
                        return FV_ERRCODE_INVALID_ERR_CODE;
                    }
                    //判断接收的数据是否包含完整的消息
                    for(int nCnt = 0; nCnt < availableByteCount; nCnt++){
                        if(0x40 == bDataBuff[receivedByteCnt + nCnt]){
                            msgStartReceived = true;
                        }
                        if(0x0D == bDataBuff[receivedByteCnt + nCnt]){
                            msgEndReceived = true;
                        }
                    }
                    receivedByteCnt += availableByteCount;
                    if(msgStartReceived && msgEndReceived){
                        DebugStringPrint("TransferMsgData: Msg data received!");
                        msgAvailable = true;
                        break; //如果包含完整的消息,则退出接受
                    }
                    //如果不包含完整的消息，则继续进行接收
                    continue;
                }
                try{
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugStringPrint("TransferMsgData: Exception on sleep !");
                    return FV_ERRCODE_INVALID_ERR_CODE;
                }
                nTimeout -= 100;
                if(0 >= nTimeout) {
                    break;
                }
            }
            if (!msgAvailable) {
                DebugStringPrint("TransferMsgData: not valid vein msg!");
                return FV_ERRCODE_DEVICE_NOT_EXIST;
            }
            DebugAsciiPrint(bDataBuff, (short) receivedByteCnt);
            vmMsg.SetMsgData(bDataBuff, (short) receivedByteCnt);
            //判断返回消息ID与发送消息ID是否匹配
            bMsgIdRecv = vmMsg.GetMsgId();
            nMsgIdSent = 0 + (bMsgIdSent & 0xff);
            nMsgIdRecv = 0 + (bMsgIdRecv & 0xff);
            if ((nMsgIdRecv & 0x7F) != (nMsgIdSent & 0x7F)) {
                //接受的消息与发送的消息ID不匹配，则继续接收
                if ((0x0B == (nMsgIdRecv & 0x7F)) || (0x71 == (byte) (nMsgIdRecv & 0x7F)) || (0x73 == (byte) (nMsgIdSent & 0x7F))) {
                } else {
                    DebugStringPrint("TransferMsgData:接收消息ID = " + nMsgIdRecv + ",发送ID=" + nMsgIdSent);
                    continue;
                }
            }
            //判断设备中是否有返回错误代码
            nErrCode = (int) vmMsg.GetErrCode();
            if ((FV_ERRCODE_SUCCESS != nErrCode) && ((0x0B != (nMsgIdRecv & 0x7F)) && (0x71 != (nMsgIdRecv & 0x7F)) && ((0x73 != (nMsgIdRecv & 0x7F))))) {
                DebugStringPrint("TransferMsgData:ErrCode=" + nErrCode + "\r\n");
                return nErrCode;
            }
            return FV_ERRCODE_SUCCESS;
        }
        return FV_ERRCODE_SUCCESS;
    }

    //读取设备的ID号
    private int getDeviceId(BioVeinDevice bioVeinDevice){
        int retVal = FV_ERRCODE_SUCCESS;

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(0xFFFF);
        vmMsg.SetMsgId(FV_MSGID_QUERY);

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT/3);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("getDeviceId:发送数据失败，错误码=" + retVal + "\r\n");
            return retVal;
        }

        bioVeinDevice.mDeviceId = (int)vmMsg.GetDevId();
        DebugStringPrint("getDeviceId:获取设备编号成功，DevId=" + bioVeinDevice.mDeviceId + "\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    private int setWorkMode(BioVeinDevice bioVeinDevice, byte bWorkMode){
        int retVal = FV_ERRCODE_SUCCESS;

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(bioVeinDevice.mDeviceId);
        vmMsg.SetMsgId(FV_MSGID_CHANGE_SETTING);
        vmMsg.SetSettingType(FV_SETTYPE_WORKMODE);
        vmMsg.SetSettingValue((long) (bWorkMode & 0xFF));

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT/3);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("setWorkMode:发送数据失败，错误码=" + retVal + "\r\n");
            return retVal;
        }
        DebugStringPrint("setWorkMode:设置工作模式成功\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    private int getDeviceSerial(BioVeinDevice bioVeinDevice, int[] serialVal, byte[] serialString){
        int retVal = FV_ERRCODE_SUCCESS;
        short serialLen = (short)serialString.length;

        if(null == serialString){
            return FV_ERRCODE_WRONG_PARAMETER;
        }
        ByteArrayReset(serialString, serialString.length);

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(bioVeinDevice.mDeviceId);
        vmMsg.SetMsgId(FV_MSGID_GET_SETTING);
        vmMsg.SetSettingType(FV_SETTYPE_READSERIAL);

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT/3);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("getDeviceSerial:发送数据失败，错误码=" + retVal + "\r\n");
            return retVal;
        }

        if(null != serialVal){
            serialVal[0] = (int)vmMsg.GetExtraInfo();
        }
        byte[] bSerialBuff = new byte[serialLen];
        serialLen = (short)vmMsg.GetExtraData(bSerialBuff, serialLen);
        if(0 == serialLen){
            return FV_ERRCODE_SUCCESS;
        }
        ByteArrayCopy(serialString, bSerialBuff, serialLen);
        DebugStringPrint("getDeviceSerial:获取设备序列号成功，Serial=" + new String(serialString) + "\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    private int detectFinger(BioVeinDevice bioVeinDevice, byte[] fingerStatus){
        int retVal = FV_ERRCODE_SUCCESS;

        if(null == fingerStatus || 0 == fingerStatus.length){
            return FV_ERRCODE_WRONG_PARAMETER;
        }

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(bioVeinDevice.mDeviceId);
        vmMsg.SetMsgId(FV_MSGID_FINGER_STATUS);

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT/3);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("detectFinger:发送数据失败，错误码=" + retVal + "\r\n");
            return retVal;
        }
        fingerStatus[0] = (byte)(vmMsg.GetExtraInfo() & 0x03);

        return FV_ERRCODE_SUCCESS;
    }

    //开始用户注册
    private int beginUserRegister(BioVeinDevice bioVeinDevice){
        int retVal = FV_ERRCODE_SUCCESS;

        byte bUserName[] = new byte[8];
        bUserName[0] = 'W';
        bUserName[0] = 'E';
        bUserName[0] = 'D';
        bUserName[0] = 'O';
        bUserName[0] = 'N';
        bUserName[0] = 'E';
        byte bUserId[] = new byte[16];
        bUserId[0] = 0x31;
        UserData usUserData = new UserData();
        usUserData.ClearData();
        usUserData.SetUserId(bUserId, (short) 16);
        usUserData.SetUserName(bUserName, (short) 8);
        byte bUserHdrData[] = new byte[UserData.D_USER_HDR_LEN];
        usUserData.GetUserHdrData(bUserHdrData, (short) UserData.D_USER_HDR_LEN);

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(bioVeinDevice.mDeviceId);
        vmMsg.SetMsgId(FV_MSGID_REGISTER_BEGIN);
        vmMsg.SetRegExtra((short) FV_CONST_DEFAULT_TIMEOUT);
        vmMsg.SetExtraData(bUserHdrData, (short) UserData.D_USER_HDR_LEN);

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT/3);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("beginUserRegister:发送数据失败，错误码="+retVal+"\r\n");
            return retVal;
        }

        DebugStringPrint("beginUserRegister:开始用户注册成功\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    //注册模板
    private int registerTemplate(BioVeinDevice bioVeinDevice){
        int retVal = FV_ERRCODE_SUCCESS;

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(bioVeinDevice.mDeviceId);
        vmMsg.SetMsgId(FV_MSGID_REGISTER);
        vmMsg.SetRegUid(1);
        vmMsg.SetRegTid((short) 1);
        vmMsg.SetRegExtra((short)(6500));

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT * 20);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("registerTemplate:注册失败，错误码=" + retVal + "\r\n");
            return retVal;
        }
        DebugStringPrint("registerTemplate:注册成功\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    //读取指静脉模板
    private int getOneTemplate(BioVeinDevice bioVeinDevice, byte[] featureData){
        int retVal = FV_ERRCODE_SUCCESS;

        if(null == featureData || FV_CONST_FEATURE_LEN > featureData.length){
            return FV_ERRCODE_WRONG_PARAMETER;
        }

        VeinMsg vmMsg = new VeinMsg();
        long lReadLen = 0, lReadLenTotal = 0;
        byte featureBuffTmp[] = new byte[FV_CONST_FEATURE_LEN];
        byte featureBuff[] = new byte[FV_CONST_FEATURE_LEN];
        for(int nCnt = 1; nCnt <= 2; nCnt++) {
            vmMsg.ClearData();
            vmMsg.SetDevId(bioVeinDevice.mDeviceId);
            vmMsg.SetMsgId(FV_MSGID_GET_TEMPLATE);
            vmMsg.SetFlag((byte) 0x01);
            vmMsg.SetRegUid(0xFFFFFFFF);
            vmMsg.SetRegTid((short) 0);
            vmMsg.SetRegExtra((short) (FV_CONST_DEFAULT_TIMEOUT));
            vmMsg.SetRegReserved((long) nCnt);

            retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT);
            if (SDK_ERRCODE_SUCCESS != retVal) {
                DebugStringPrint("getOneTemplate:获取模板失败，错误码=" + retVal + "\r\n");
                return retVal;
            }
            lReadLen = vmMsg.GetExtraData(featureBuffTmp, (short)FV_CONST_FEATURE_LEN);
            for(int nByteCnt = 0; nByteCnt < lReadLen; nByteCnt++){
                featureBuff[(int)(lReadLenTotal + nByteCnt)] = featureBuffTmp[nByteCnt];
            }
            lReadLenTotal += lReadLen;
        }

        if (FV_CONST_FEATURE_LEN > lReadLenTotal) {
            DebugStringPrint("getOneTemplate:读取的模板数据错误,读取长度=" + lReadLenTotal + "\r\n");
            return FV_ERRCODE_INVALID_ERR_CODE;
        }
        ByteArrayCopy(featureData, featureBuff, featureBuff.length);
        DebugStringPrint("getOneTemplate:获取模板成功\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    //结束用户注册
    private int finishUserRegister(BioVeinDevice bioVeinDevice){
        int retVal = FV_ERRCODE_SUCCESS;

        VeinMsg vmMsg = new VeinMsg();
        vmMsg.ClearData();
        vmMsg.SetDevId(bioVeinDevice.mDeviceId);
        vmMsg.SetMsgId(FV_MSGID_REGISTER_END);
        vmMsg.SetRegExtra((short)(FV_CONST_DEFAULT_TIMEOUT));

        retVal = TransferMsgData(bioVeinDevice, vmMsg, FV_CONST_DEFAULT_TIMEOUT);
        if(FV_ERRCODE_SUCCESS != retVal){
            DebugStringPrint("FV_RegisterEnd:结束注册失败，错误码=" + retVal + "\r\n");
            return retVal;
        }
        DebugStringPrint("FV_RegisterEnd:结束注册成功\r\n");

        return FV_ERRCODE_SUCCESS;
    }

    //获取一个指静脉模板
    private int grabOneFeature(BioVeinDevice bioVeinDevice, byte[] featureData, byte flag)
    {
        int retVal = FV_ERRCODE_SUCCESS;

        if(null == featureData || FV_CONST_FEATURE_LEN > featureData.length){
            return FV_ERRCODE_WRONG_PARAMETER;
        }

        retVal = setWorkMode(bioVeinDevice, FV_CONST_WORKMODE_REGISTER);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = beginUserRegister(bioVeinDevice);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = registerTemplate(bioVeinDevice);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = getOneTemplate(bioVeinDevice, featureData);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = finishUserRegister(bioVeinDevice);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = setWorkMode(bioVeinDevice, FV_CONST_WORKMODE_REGISTER);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }
        return FV_ERRCODE_SUCCESS;
    }

    //获取一个指静脉模板及其对应的指静脉图片
    private int grabOneFeatureImage(BioVeinDevice bioVeinDevice, byte[] featureData, byte[] imageData, int[] imageSize, int[] imageWidth, int[] imageHeight, byte flag)
    {
        int retVal = FV_ERRCODE_SUCCESS;

        if(null == featureData || FV_CONST_FEATURE_LEN > featureData.length){
            return FV_ERRCODE_WRONG_PARAMETER;
        }

        retVal = setWorkMode(bioVeinDevice, FV_CONST_WORKMODE_REGISTER);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = beginUserRegister(bioVeinDevice);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = registerTemplate(bioVeinDevice);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = getOneTemplate(bioVeinDevice, featureData);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = finishUserRegister(bioVeinDevice);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }

        retVal = setWorkMode(bioVeinDevice, FV_CONST_WORKMODE_REGISTER);
        if(FV_ERRCODE_SUCCESS != retVal){
            return retVal;
        }
        return FV_ERRCODE_SUCCESS;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_EnumDevice(byte[][] devIds)
     * 功能:：
     *       枚举设备Id
     *
     * @参数(OUT)  byte[][] devIds: 用户保存有效设备Id的输出缓冲区，在调用本接口前必须分配好相应的内存空间；
     *                              返回正确时
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_EnumDevice(byte[][] devIds){
        int retVal = FV_ERRCODE_SUCCESS;
        char[] devId = null;
        int validDevCnt = 0;

        if(null == devIds || 0 == devIds.length){
            return FV_ERRCODE_WRONG_PARAMETER;
        }
        for(int devIdx = 0; devIdx < devIds.length; devIdx++){
            ByteArrayReset(devIds[devIdx], devIds[devIdx].length);
        }

        if(null == mSerialPortFinder){
            mSerialPortFinder = new SerialPortFinder();
        }
        if(null == mSerialPortFinder){
            return FV_ERRCODE_MEMORY_ALLOC_FAIL;
        }

        //清除设备列表中的所有设备
        if(0 < mBioVeinDevice.size()) {
            for(int devIdx = 0; devIdx < mBioVeinDevice.size(); devIdx++){
                BioVeinDevice bioVeinDevice = mBioVeinDevice.get(devIdx);
                bioVeinDevice.close();
            }
            mBioVeinDevice.removeAllElements();
        }

        //读取UART串口设备的文件名数组
        String[] veinDevicesPath = mSerialPortFinder.getUartDevicesPath();
        BioVeinDevice bioVeinDevice = null;
        for(int devIdx = 0; devIdx < veinDevicesPath.length; devIdx++){
            if(null == bioVeinDevice) {
                bioVeinDevice = new BioVeinDevice();
            }
            bioVeinDevice.mPortFilePath = veinDevicesPath[devIdx];
            try {
                //打开UART串口设备
                bioVeinDevice.mSerialPort = new SerialPort(new File(bioVeinDevice.mPortFilePath), BioVeinDevice.FV_DEFAULT_UART_BAUDRATE, 0);
            }
            catch (IOException ioe){
                //打开UART串口设备发生IO错误
                DebugStringPrint("FV_EnumDevice: IO error on " + bioVeinDevice.mPortFilePath);
                continue;
            }
            catch (SecurityException se){
                //打开UART串口设备权限不够
                DebugStringPrint("FV_EnumDevice: Security error on " + bioVeinDevice.mPortFilePath);
                continue;
            }
            bioVeinDevice.mInputStream = bioVeinDevice.mSerialPort.getInputStream();
            bioVeinDevice.mOutputStream = bioVeinDevice.mSerialPort.getOutputStream();

            //通过获取设备编号确认是否为指静脉识别设备
            DebugStringPrint("FV_EnumDevice: dev=" + bioVeinDevice.mPortFilePath);
            retVal = getDeviceId(bioVeinDevice);
            if(FV_ERRCODE_SUCCESS != retVal){
                DebugStringPrint("FV_EnumDevice:getDeviceId err = " + retVal);
                bioVeinDevice.mSerialPort.close();
                continue;
            }
            if(devIds[validDevCnt].length < bioVeinDevice.mPortFilePath.length()){
                bioVeinDevice.mSerialPort.close();
                return FV_ERRCODE_WRONG_PARAMETER;
            }
            bioVeinDevice.close();
            devId = bioVeinDevice.mPortFilePath.toCharArray();
            byte[] bDevId = Chars2Bytes(devId);
            ByteArrayCopy(devIds[validDevCnt], bDevId, bDevId.length);
            validDevCnt++;
            DebugStringPrint("FV_EnumDevice:success, devid = " + bioVeinDevice.mDeviceId);
            mBioVeinDevice.add(bioVeinDevice);
            bioVeinDevice = null;
            if(devIds.length == validDevCnt){//输出缓冲区已满，停止继续枚举设备
                break;
            }
        }

        return FV_ERRCODE_SUCCESS;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_InitDevice(byte[] devId)
     * 功能:：
     *      初始化设备
     *
     * @参数(IN)  byte[] devId: 指定初始化的设备的ID
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_InitDevice(byte[] devId){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        return FV_ERRCODE_SUCCESS;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_OpenDevice(byte[] devId)
     * 功能:：
     *      打开设备
     *
     * @参数(IN)  byte[] devId: 指定要打开的设备的ID
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_OpenDevice(byte[] devId){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 1);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        int retVal = bioVeinDevice.open();
        if(FV_ERRCODE_SUCCESS == retVal){
            retVal = getDeviceId(bioVeinDevice);
            if(FV_ERRCODE_SUCCESS != retVal){
                retVal = getDeviceId(bioVeinDevice);
                if(FV_ERRCODE_SUCCESS != retVal){
                    DebugStringPrint("FV_OpenDevice:getDeviceId err = " + retVal);
                    bioVeinDevice.close();
                }
            }
        }
        mBioVeinDevice.add(bioVeinDevice);
        retVal = setWorkMode(bioVeinDevice, FV_CONST_WORKMODE_REGISTER);

        return retVal;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_CloseDevice(byte[] devId)
     * 功能:：
     *        关闭设备
     *
     * @参数(IN)  byte[] devId: 指定要关闭的设备的ID
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_CloseDevice(byte[] devId){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 1);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        int retVal = bioVeinDevice.close();
        mBioVeinDevice.add(bioVeinDevice);

        return retVal;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_GetDevSerialNum(byte[] devId, byte[] serialNum)
     * 功能:：
     *        获取设备序列号
     *
     * @参数(IN)  byte[] devId: 指定要操作的设备的ID
     * @参数(OUT)  byte[] serialNum: 获取的设备序列号信息
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_GetDevSerialNum(byte[] devId, byte[] serialNum){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        int retVal = getDeviceSerial(bioVeinDevice, null, serialNum);
        return retVal;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_GetSdkVersion(byte[] version)
     * 功能:：
     *        获取SDK版本号
     *
     * @参数(OUT)  byte[] version: 获取的SDK版本号信息
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_GetSdkVersion(byte[] version){
        if(null == version || 16 > version.length){
            return FV_ERRCODE_WRONG_PARAMETER;
        }

        ByteArrayReset(version, version.length);
        int versionLen = version.length;
        byte[] sdkVersion = FV_CONST_SDK_VERSION.getBytes();
        if(sdkVersion.length < versionLen)versionLen = sdkVersion.length;
        ByteArrayCopy(version, sdkVersion, versionLen);

        return FV_ERRCODE_SUCCESS;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_FingerDetect(byte[] devId, byte[] fingerStatus)
     * 功能:：
     *        检测手指的放置状态
     *
     * @参数(IN)  byte[] devId: 指定要操作的设备的ID
     * @参数(OUT)  byte[] fingerStatus: 获取的手指状态，0：没有检测到手指，1、2：检测到手指但没有放好，3：检测到手指并且已经放置好
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_FingerDetect(byte[] devId, byte[] fingerStatus){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        int retVal = detectFinger(bioVeinDevice, fingerStatus);
        return retVal;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_GrabFeature(byte[] devId, byte[] featureData, byte flag){
     *                   BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2)
     * 功能:：
     *        读取一个指静脉特征模板数据
     *
     * @参数(IN)  byte[] devId: 指定要操作的设备的ID
     * @参数(OUT)  byte[] featureData: 用于保存特征模板数据的缓冲区
     * @参数(IN)  byte flag: 操作标志位，暂时未作定义
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_GrabFeature(byte[] devId, byte[] featureData, byte flag){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        int retVal = grabOneFeature(bioVeinDevice, featureData, flag);
        return retVal;
    }

    /**
     * Wedone:
     * 定义：
     *        int FV_GrabFeature(byte[] devId, byte[] featureData, byte flag){
     *                   BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2)
     * 功能:：
     *        读取一个指静脉特征模板数据及其对应的指静脉图片（****本接口的图片信息输出尚未封装完成****）
     *
     * @参数(IN)  byte[] devId: 指定要操作的设备的ID
     * @参数(OUT)  byte[] featureData: 用于保存特征模板数据的缓冲区
     * @参数(OUT)  byte[] imageData: 用于保存特征模板数据的缓冲区
     * @参数(OUT)  int[] imageSize: 用于保存特征模板数据的缓冲区
     * @参数(OUT)  int[] imageWidth: 用于保存特征模板数据的缓冲区
     * @参数(OUT)  int[] imageHeight: 用于保存特征模板数据的缓冲区
     * @参数(IN)  byte flag: 操作标志位，暂时未作定义
     * @调用 public
     * @返回 int: 0=正确，其他=错误代码
     */
    public int FV_GrabFeatureAndImage(byte[] devId, byte[] featureData, byte[] imageData, int[] imageSize, int[] imageWidth, int[] imageHeight, byte flag){
        BioVeinDevice bioVeinDevice  = getBioVeinDevice(devId, 2);
        if(null == bioVeinDevice)return FV_ERRCODE_DEVICE_NOT_EXIST;

        int retVal = grabOneFeatureImage(bioVeinDevice, featureData, imageData, imageSize, imageWidth, imageHeight, flag);
        return retVal;
    }

    @Override
    public void run() {
    }

}
