package com.shuli.root.faceproject.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.bjw.bean.ComBean;
import com.bjw.utils.FuncUtil;
import com.bjw.utils.SerialHelper;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.utils.IOUtil;
import com.shuli.root.faceproject.utils.SoundPoolUtil;
import java.io.IOException;
import WedoneBioVein.SdkMain;
import WedoneBioVein.UserData;
import WedoneBioVein.VeinMatchCaller;
import android_serialport_api.SerialPortFinder;
import static java.lang.Thread.sleep;

public class FingerActivity extends AppCompatActivity implements NetworkChangeReceiver.NetCallback {
    private SdkMain mSdkMain = null;
    UserData mRegUserData = new UserData(); //用于保存采集(注册)的模板
    UserData mAIUserData = new UserData(); //用于保存验证时通过自动学习生成的模板，在比对时也作为比对模板的一部分
    int mUserCnt = 0;

    int mMaxVeinDeviceNum = 20;
    int mVeinDevIdLen = 64;
    byte[][] mVeinDevIdList = null;
    int mVeinDevCnt = 0;

    private TextView textView,tv_code,net_state,tv_mac;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            textView.setText((CharSequence) msg.obj);
        }
    };

    //二维码部分
    private String code = "";
    private SerialPortFinder serialPortFinder;
    private SerialHelper serialHelper;
    private boolean isOpenDoor = false;

    private IntentFilter intentFilter;
    private NetworkChangeReceiver networkChangeReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger);
        textView = findViewById(R.id.textView);
        tv_code = findViewById(R.id.tv_finger_code);
        net_state = findViewById(R.id.net_state);
        tv_mac = findViewById(R.id.tv_mac);
        iniview2();//二维码部分
        InitOperations();
        doBtnEnumDevice();

       //接收广播
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkChangeReceiver = new NetworkChangeReceiver();
        networkChangeReceiver.setNetCallback(this);

        registerReceiver(networkChangeReceiver, intentFilter);
        IOUtil.setGpio("PB7",false,1);//DZ
        IOUtil.setGpio("PB2",false,1);//DZ
    }

    //初始化
    private void InitOperations(){
        if(null == mSdkMain){
            mSdkMain = new SdkMain();
            mVeinDevIdList = new byte[mMaxVeinDeviceNum][];
            for(int nCnt = 0; nCnt < mMaxVeinDeviceNum; nCnt++){
                mVeinDevIdList[nCnt] = new byte[mVeinDevIdLen];
            }
            mRegUserData.ClearData();
            mAIUserData.ClearData();
        }
    }

    private  byte[] mergeBytes(byte[] part1, byte[] part2){
        int p1Len = (null == part1)?0:part1.length;
        int p2Len = (null == part2)?0:part2.length;

        if(0 == (p1Len + p2Len)){
            return null;
        }

        byte[] ret = new byte[p1Len + p2Len];
        for(int i = 0; i < p1Len; i++){
            ret[i] = part1[i];
        }
        for(int i = 0; i < p2Len; i++){
            ret[p1Len + i] = part2[i];
        }
        SdkMain.DebugStringPrint("mergeBytes:p1Len=" + p1Len + ",p2Len=" + p2Len);
        return ret;
    }

    //枚举设备按钮
    /**/
    public void doBtnEnumDevice() {
        /**
         * @返回 int: 0=正确，其他=错误代码
         */
        int retVal = mSdkMain.FV_EnumDevice(mVeinDevIdList);
        if(mSdkMain.FV_ERRCODE_SUCCESS == retVal){
            mVeinDevCnt = 0;
            String devId = null, msg = "";
            for(int devIdx = 0; devIdx < mMaxVeinDeviceNum; devIdx++){
                devId = new String(mVeinDevIdList[devIdx]);
                if(0 == devId.trim().length()) {
                    break;
                }
                mVeinDevCnt++;
                msg += "{Dev" + (devIdx + 1) + ":" + devId + "}";
            }

            if(0 >= mVeinDevCnt){
                DisplayNoticeMsg("不存在有效的指静脉设备！", 0);
                return;
            }else {
                Log.i("sss","枚举设备成功！" +msg);
                doBtnInitDevice();
            }
        } else{
            Log.i("sss","枚举初始化失败！");
        }
        return;
    }

    //初始化设备按钮
    public void doBtnInitDevice() {
        int retVal;

        String devId = null;
        for(int devIdx = 0; devIdx < mVeinDevCnt; devIdx++){
            devId = new String(mVeinDevIdList[devIdx]);
            if(0 == devId.trim().length()) {
                break;
            }

            retVal = mSdkMain.FV_InitDevice(mVeinDevIdList[devIdx]);//初始化设备
            if(mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                retVal = mSdkMain.FV_OpenDevice(mVeinDevIdList[devIdx]);//打开设备按钮
                if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                    if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                        DisplayNoticeMsg("设备初始化成功！", 0);
                    } else {
                        DisplayNoticeMsg("设备初始化失败！", 0);
                    }
                }
            }
        }
        return;
    }

    //关闭设备按钮
    public void doCloseDevice() {
        if(0 >= mVeinDevCnt){
            return;
        }

        int retVal;
        String devId = null, msg = "";
        for(int devIdx = 0; devIdx < mVeinDevCnt; devIdx++){
            devId = new String(mVeinDevIdList[devIdx]);
            if(0 == devId.trim().length()) {
                break;
            }
            retVal = mSdkMain.FV_CloseDevice(mVeinDevIdList[devIdx]);
            if(mSdkMain.FV_ERRCODE_SUCCESS == retVal){
                msg += "{" + devId + ":成功}";
            }
            else{
                msg += "{" + devId + ":err=" + retVal + "}";
            }
        }
        Log.i("sss","初始化设备：" + msg);

        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doCloseDevice();
        serialHelper.close();
        //解除广播
        unregisterReceiver(networkChangeReceiver);
    }

    /**
     *   注册1根手指(1次)按钮
     */
    public void OnClickOneBtnRegister(View v) {
        if (0 >= mVeinDevCnt) {

            Log.i("sss","不存在有效的指静脉设备，请先进行枚举设备操作！");
            DisplayNoticeMsg("不存在有效的指静脉设备，请先进行枚举设备操作！", 0);
            return;
        }
        if (mRegUserData.D_USER_TEMPLATE_NUM <= mRegUserData.GetTemplateNum()) {
            Log.i("sss","单个用户最多只能注册20个模板！\n");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retVal;
                String devId = null, msg = "";
                int readCnt = 0; //读取模板次数的计数器
                int retryCnt = 0;
                byte[] featureData = new byte[SdkMain.FV_CONST_FEATURE_LEN];
                int devIdx = 0; //Wedone:指定在第一台设备上进行操作

                devId = new String(mVeinDevIdList[devIdx]);
                if (0 == devId.trim().length()) {//Wedone:无效的设备ID，则直接返回
                    return;
                }

                //清除上次注册的数据
                mRegUserData.ClearData();
                mAIUserData.ClearData();

                while(true) {
                    if(6 <= retryCnt)break; //已经重试了6次，结束采集
                    retryCnt++;
                    //Wedone:检测手指，直到检测到手指已经放好才进行后续读取静脉特征的操作
                    boolean isWaitSuccess = true;
                    msg = "正在{" + devId + "}上采集第" + (readCnt + 1) + "个静脉特征:";
                    DisplayNoticeMsg("采集第" + (readCnt + 1)  + "个静脉特征，"+"请放入手指！", 0);
                    SoundPoolUtil.play(8);
                    isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x03, 20, 500, msg + "请放手指!");
                    if (!isWaitSuccess) {
                        DisplayNoticeMsg("没有发现手指", 0);
                        SoundPoolUtil.play(7);
                        handler.postDelayed(runnable, 1000);
                        return;
                    }

                    //Wedone:读取指静脉特征模板数据
                    retVal = mSdkMain.FV_GrabFeature(mVeinDevIdList[devIdx], featureData, (byte) 0);
                    if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                          DisplayNoticeMsg("读取成功！", 0);
                    } else {
                        DisplayNoticeMsg("读取失败！", 0);
                        SoundPoolUtil.play(6);
                        handler.postDelayed(runnable, 1000);
                        break; //Wedone: 采集过程中发生错误，则直接退出
                    }

                    //Wedone:检测手指，直到检测到手指已经移开才进行后续读操作，确保每次采集都是重新放置了手指而不是手指一直放着不动
                 //   msg = "读取完成第" + (readCnt + 1) + "个静脉特征:";
                    DisplayNoticeMsg("请移开手指！", 0);
                    SoundPoolUtil.play(9);
                    isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x00, 20, 500, msg + "请移开手指!");
                    if (!isWaitSuccess) {
                        DisplayNoticeMsg("没有移开手指！", 0);
                        handler.postDelayed(runnable, 1000);
                        return;
                    }

                    //Wedone:确认采集的指静脉特征数据是否有效
                    retVal = VeinMatchCaller.FvmIsValidFeature(featureData, (byte)0x01);
                    if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                        DisplayNoticeMsg("错误：指静脉特征数据无效！", 0);
                        SoundPoolUtil.play(3);
                        continue;
                    }

                    //Wedone:调用FV_GrabFeature返回成功的话，第二个参数的缓冲区中就保存了所读取的模板数据，
                    if (0 == readCnt) {
                        //采集完成第一个静脉特征，生成对应的用户信息
                        mUserCnt++;
                        byte bUserId[] = new byte[mRegUserData.D_USER_HDR_USERID_LEN];
                        byte bUserName[] = new byte[mRegUserData.D_USER_HDR_USERNAME_LEN];

                        mRegUserData.SetUid((long) mUserCnt);

                        bUserId[0] = 'I';
                        bUserId[1] = 'D';
                        bUserId[2] = (byte) (0x30 + (mUserCnt % 10000) / 1000);
                        bUserId[3] = (byte) (0x30 + (mUserCnt % 1000) / 100);
                        bUserId[4] = (byte) (0x30 + (mUserCnt % 100) / 10);
                        bUserId[5] = (byte) (0x30 + (mUserCnt % 10));
                        mRegUserData.SetUserId(bUserId, (short) 6);

                        bUserName[0] = 'U';
                        bUserName[1] = 'S';
                        bUserName[2] = 'E';
                        bUserName[3] = 'R';
                        bUserName[4] = (byte) (0x30 + (mUserCnt % 10000) / 1000);
                        bUserName[5] = (byte) (0x30 + (mUserCnt % 1000) / 100);
                        bUserName[6] = (byte) (0x30 + (mUserCnt % 100) / 10);
                        bUserName[7] = (byte) (0x30 + (mUserCnt % 10));
                        mRegUserData.SetUserName(bUserName, (short) 8);
                    }
                    if(0 < readCnt){ //Wedone:之前已经有采集的特征，把当前采集的静脉特征与之前采集的进行验证是否属于同一根手指
                        byte[] regTemplateData = mRegUserData.TemplateData();
                        //Wedone: 注册过程中，检测采集的静脉特征是否属于同一根手指
                        retVal = VeinMatchCaller.FvmIsSameFinger(featureData,//本次采集的指静脉特征值
                                regTemplateData,  //包含之前采集的指静脉特征值数据
                                (byte)readCnt, //第二个参数中包含的指静脉特征值的个数
                                (byte)0x03); //加密方式，当前请固定为3
                        if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                            DisplayNoticeMsg("错误：注册过程中采集的特征必须属于同一根手指！\r\n", 0);
                            SoundPoolUtil.play(4);
                            continue;
                        }
                    }
                    //Wedone：采集的特征值符合要求，保存到本地缓冲区中
                    mRegUserData.AddTemplateData(featureData, (short) featureData.length);
                  //  readCnt++;

               //     if(3 <= readCnt){
                        DisplayNoticeMsg("采集指静脉特征成功！已采集" + mRegUserData.GetTemplateNum() + "个特征模板", 0);
                        SoundPoolUtil.play(5);
                        handler.postDelayed(runnable, 1000);
                        // TODO: 2018/4/18 将获取到的模板数组上传服务器
                        byte[] regTemplateData = mRegUserData.TemplateData(); //获取注册采集的特征数据

//                        MessageFinger messageFinger = new MessageFinger();
//                        messageFinger.setRegTemplateData(regTemplateData);
//                        messageFinger.setName("xxxxx");
//                        Gson gson = new Gson();
//                        String postInfoStr = gson.toJson(messageFinger);
//                        Log.i("sss","postInfoStr" + postInfoStr);
//                        upload(postInfoStr);

                        break; //采集完成3个有效模板，则结束采集
                 //   }
                }
            }
        }).start();
        return;
    }

   Runnable runnable = new Runnable() {
       @Override
       public void run() {
           textView.setText("");
       }
   };

    /**
     *  验证手指按钮
     */
    public void OnClickBtnIdentify(View v) {
        if (0 >= mVeinDevCnt) {
            DisplayNoticeMsg("不存在有效的指静脉设备，请先进行枚举设备操作！", 0);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retVal;
                String devId = null, msg = "";
                byte[] featureData = new byte[SdkMain.FV_CONST_FEATURE_LEN];
                int devIdx = 0; //Wedone:指定在第一台设备上进行操作

                devId = new String(mVeinDevIdList[devIdx]);
                if (0 == devId.trim().length()) {//Wedone:无效的设备ID，则直接返回
                    return;
                }

                //Wedone:检测手指，直到检测到手指已经放好才进行后续读取静脉特征的操作
                boolean isWaitSuccess = true;
                msg = "正在{" + devId + "}上进行验证：";
                DisplayNoticeMsg("请放手指!", 0);
                isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x03, 20, 500, msg + "请放手指!");
                if (!isWaitSuccess) {
                    return;
                }

                //Wedone:读取指静脉特征模板数据
                retVal = mSdkMain.FV_GrabFeature(mVeinDevIdList[devIdx], featureData, (byte) 0);
                if (mSdkMain.FV_ERRCODE_SUCCESS == retVal) {
                    DisplayNoticeMsg("读取成功！", 0);
                } else {
                    DisplayNoticeMsg("失败！", 0);
                    return; //Wedone: 采集过程中发生错误，则直接退出
                }

                //Wedone:检测手指，直到检测到手指已经移开才进行后续读操作，确保每次采集都是重新放置了手指而不是手指一直放着不动
                msg = "采集验证模板成功:";
                DisplayNoticeMsg("请移开手指!", 0);
                isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x00, 20, 500, msg + "请移开手指!");
                if (!isWaitSuccess) {
                    return;
                }

                //Wedone:确认采集的指静脉特征数据是否有效
                retVal = VeinMatchCaller.FvmIsValidFeature(featureData, (byte)0x01);
                if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                    DisplayNoticeMsg("错误：指静脉特征数据无效！", 0);
                    return;
                }

                    int regTemplateCnt = mRegUserData.GetTemplateNum();
                    int aiTemplateCnt = mAIUserData.GetTemplateNum();
                    byte[] regTemplateData = mRegUserData.TemplateData(); //获取注册采集的特征数据
                    byte[] aiTemplateData = mAIUserData.TemplateData(); //获取AI自学习的特征数据
                    byte[] aiTemplateBuff = new byte[UserData.D_USER_TEMPLATE_SIZE*1]; //准备3个模板大小的缓冲区用于自动学习
                    byte[] mergeTemplateData = mergeBytes(regTemplateData, aiTemplateData); //把注册时采集的特征数据和AI自学习的数据合并起来验证
                    byte securityLevel = 4;
                    int[] diff = new int[1];
                    int[] AIDataLen = new int[1];
                    diff[0] = 10000;
                    AIDataLen[0] = UserData.D_USER_TEMPLATE_SIZE*1;
                    SdkMain.DebugStringPrint("指静脉比对:regTemplateCnt=" + regTemplateCnt + ",aiTemplateCnt=" + aiTemplateCnt);
                    //Wedone: 调用静脉特征值比对接口进行比对，各个参数意义如下说明
                    retVal = VeinMatchCaller.FvmMatchFeature(featureData, //采集的用于验证的指静脉特征值
                            mergeTemplateData, //包含注册的特征值+AI自学习的特征值数据
                            (byte) (regTemplateCnt + aiTemplateCnt), //第二个参数的比对特征值的个数，包含注册+AI自学习的特征值个数
                            (byte) 0x03, //加密方式，当前请固定为3
                            securityLevel, //比对时使用的安全级别， 1:1场景：范围[6-10],建议值为6，1:N场景：范围[1-5],建议值为4
                            diff, //用于返回比对结果的差异度值
                            aiTemplateBuff, //用于输出比对时自动学习生成的特征数据
                            AIDataLen); //输入时初始化值为学习缓冲区的大小，验证通过返回时为学习成功的数据长度
                    if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                        DisplayNoticeMsg("验证失败！！！", 0);
                        return;
                    }
                    //Wedone：验证通过，并且返回的AI学习缓冲区的数据长度大于0，保存AI学习的数据
                    if(0 < AIDataLen[0]){
                        mAIUserData.ClearData();
                        mAIUserData.SetTemplateData(aiTemplateBuff, (short)AIDataLen[0]);
                    }
                    DisplayNoticeMsg("验证通过！差异度=" + diff[0] + "，学习数据长度=" + AIDataLen[0], 0);
                    handler.postDelayed(runnable,1000);
                }
        }).start();
        return;
    }

    //循环读取指静脉
    public void OnWhileIdentify(View v) {
        if (0 >= mVeinDevCnt) {
            DisplayNoticeMsg("不存在有效的指静脉设备，请先进行枚举设备操作！", 0);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retVal;
                String devId = null, msg = "";
                byte[] featureData = new byte[SdkMain.FV_CONST_FEATURE_LEN];
                int devIdx = 0; //Wedone:指定在第一台设备上进行操作

                devId = new String(mVeinDevIdList[devIdx]);
                if (0 == devId.trim().length()) {//Wedone:无效的设备ID，则直接返回
                    return;
                }

                while (true){
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i("sss",">>>>>>>>>>");
                    //Wedone:检测手指，直到检测到手指已经放好才进行后续读取静脉特征的操作
                    boolean isWaitSuccess = true;
                    msg = "正在{" + devId + "}上进行验证：";
                    DisplayNoticeMsg("请放手指!", 0);
                    isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x03, 20, 500, msg + "请放手指!");
                    if (!isWaitSuccess) {
                        //Wedone:读取指静脉特征模板数据
                        continue;
                    }
                    retVal = mSdkMain.FV_GrabFeature(mVeinDevIdList[devIdx], featureData, (byte) 0);
                    if (mSdkMain.FV_ERRCODE_SUCCESS != retVal) {
                        DisplayNoticeMsg("采集过程中发生错误！", 0);
                        continue;
                    }
//                    DisplayNoticeMsg("读取成功！", 0);
                    //Wedone:检测手指，直到检测到手指已经移开才进行后续读操作，确保每次采集都是重新放置了手指而不是手指一直放着不动
                    DisplayNoticeMsg("请移开手指!", 0);
                    isWaitSuccess = WaitFingerStatus(mVeinDevIdList[devIdx], (byte) 0x00, 20, 500, msg + "请移开手指!");
                    if (!isWaitSuccess) {
                        //Wedone:确认采集的指静脉特征数据是否有效
                        DisplayNoticeMsg("没有移开手指！", 0);
                         continue;
                    }
                    retVal = VeinMatchCaller.FvmIsValidFeature(featureData, (byte)0x01);
                    if(SdkMain.FV_ERRCODE_SUCCESS != retVal){
                        DisplayNoticeMsg("错误：指静脉特征数据无效！\r\n", 0);
                    } else {
                        DisplayNoticeMsg("指静脉特征数据有效！", 0);
                        // TODO: 2018/4/18 向服务器发送数据


                    }
                }
            }
        }).start();
        return;
    }

    /**
     * Wedone: 等待手指传感器变为某种指定的状态，等待的时间为nInterval*nTimes
     *
     * @参数(IN)  byte bFingerStatus: 等待的状态；0：手指已经移开，3：手指已经放置好。
     * @参数(IN)  int nTimes: 检测的次数，必须大于0。
     * @参数(IN)  int nInterval: 每次检测的间隔，单位为毫秒，建议在500 - 1000毫秒之间。
     * @调用 public
     * @返回 boolean: true=成功的等到了指定的状态：
     *             false=没有等到指定的状态就超时了
     */
    public boolean WaitFingerStatus(byte[] devId, byte bFingerStatus, int nTimes, int nInterval, String msghdr){
        if((0 >= nTimes) || (200 >= nInterval) || (1000 < nInterval)){
            return false;
        }
        String msg = "";
        byte[] fingerStatus = new byte[1];
        int retVal;
        if(null != msghdr){
            msg = msghdr;
        }
        else if(0 == bFingerStatus){
            msg = "请移开手指！";
        }
        else if(0x03 == bFingerStatus){
            msg = "请放手指！";
        }
        for(int nCnt = 0; nCnt < nTimes; nCnt++){
          //  DisplayNoticeMsg(msg, 0);
            retVal = mSdkMain.FV_FingerDetect(devId, fingerStatus);
            if (mSdkMain.FV_ERRCODE_SUCCESS != retVal) {
              //  DisplayNoticeMsg("检测手指放置状态失败，错误码=" + retVal + "!\r\n", 0);
                return false;
            }
            if(bFingerStatus == fingerStatus[0]){
                return true;
            }
            try {
                sleep(nInterval, 0);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            if(0 == (nCnt%(1000/nInterval))) {
                msg += "*";
            }
        }
        return false;
    }

    public  void DisplayNoticeMsg(String msg, int action){
        Message msgObj = new Message();
        msgObj.obj = msg;
        handler.sendMessage(msgObj);
        return;
    }

    private void iniview2() {
        serialPortFinder = new SerialPortFinder();
        serialHelper = new SerialHelper() {
            @Override
            protected void onDataReceived(final ComBean comBean) {
                String str = new String(comBean.bRec).trim();
                Log.i("sss","xxxxx " + str);
                if(TextUtils.isEmpty(str)){
                    return;
                }
                String s = str.substring(str.length() - 1, str.length());
                if (s.equals("\n")) {
                    s = str.substring(0, str.length() - 1);
                    code = code + s;
                    if(code.contains("\n")){
                        int num = code.indexOf("\n");
                        code = code.substring(0,num);
                    }
                    if(code.equals("suriot -> open gate")){
                        isOpenDoor = true;
                        IOUtil.setGpio("PB7",false,0);//DZ
                        IOUtil.setGpio("PB2",false,0);//DZ
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tv_code.setText(code);
                            Log.i("sss",code);
                            code = "";
                        }
                    });
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tv_code.setText("");
                            if(isOpenDoor){
                                IOUtil.setGpio("PB7",false,1);//DZ
                                IOUtil.setGpio("PB2",false,1);//DZ
                                isOpenDoor = false;
                            }
                        }
                    },2000);

                } else {
                    code += str;
                }
            }
        };
        serialHelper.setPort("/dev/ttyS2");
        serialHelper.setBaudRate("9600");

        if(!serialHelper.isOpen()){
            try {
                serialHelper.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setNetState(int state) {
        switch (state){
            case 1:
                net_state.setText("状态改变");
                tv_mac.setText("");
                break;
            case 2:
                net_state.setText("正常");
                tv_mac.setText(getMacAddress());
                break;
            case 3:
                net_state.setText("未连接！");
                tv_mac.setText("");
                break;
        }
    }

    public String getMacAddress(){
        WifiManager wifiMan = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE) ;
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        return wifiInf.getMacAddress();
    }

    public void doFinish(View view) {
        finish();
    }
}



