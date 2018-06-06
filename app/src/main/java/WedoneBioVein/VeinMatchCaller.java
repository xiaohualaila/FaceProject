package WedoneBioVein;

/*
 * Created by wedone on 2017/4/23.
 * JNI调用比对SDK的SO库的方法
 * ①在工程文件中添加类，比如本文件对应的类VeinMatchCaller
 * ②在添加的类(比如VeinMatchCaller)中添加方法与指静脉比对SO库中的方法一一对应
 * ③在Android Studio的Terminal界面中，把目录切换到工程的app\src\main\java目录
 * ④生成类文件命令：javac WedoneBioVein/VeinMatchCaller.java
 * ⑤生成头文件命令：javah -classpath . WedoneBioVein.VeinMatchCaller
 * ⑥在app\src\main\java目录下找到生成的头文件：WedoneBioVein_VeinMatchCaller.h
 * ⑦新建*.c文件(比如：VeinMatchJni.cpp)，实装⑥生成的头文件中的各个函数
 * ⑧使用ndk-build把指静脉比对库SO文件和⑦生成的文件编译成中间库：libVeinMatchCaller.so
 * ⑨把libVeinMatch.so、libVeinMatchCaller.so两个库文件拷贝到目录app\src\main\jniLibs\armeabi-v7a
 *   jniLibs和armeabi-v7a这两层目录如不存在的话则手动创建。
 * ⑩在Activity中调用本文的方法， 完成。
 */

public class VeinMatchCaller {
    static {
        System.loadLibrary("VEINMATCH");
        System.loadLibrary("VeinMatchCaller");
    }

    //以下函数已经封装到JNI库中，以下接口可以直接调用而不需要初始化
    public static native  int FvmGetVersion(byte[] bVersion);
    public static native  int FvmIsValidFeature(byte[] bTemplateBuff, byte bFlag);
    public static native  int FvmIsSameFinger(byte[] featureDataMatch, byte[] featureDataReg, byte RegCnt, byte flag);
    public static native  int FvmMatchFeature(byte[] featureDataMatch, byte[] featureDataReg, byte RegCnt, byte flag, byte securityLevel, int[] diff, byte[] AIDataBuf, int[] AIDataLen);

    //下列函数尚未进行JNI封装
    public static native  int FvmInit(int dwMaxUserNum, short wUserMaxTemplateNum);
    public static native  int FvmStop();
    public static native  int FvmMatchTemplate(byte[] bPtrTemplateLeft, byte[] bPtrTemplateRight, byte bFlag);
    public static native  int FvmMatchTemplateEx(byte[] bPtrTemplateLeft, byte[] bPtrTemplateRight, byte bFlag, short[] Reserved);
    public static native  int FvmMatchTemplates(byte[] bPtrTemplateLeft, byte[] bPtrTemplateRight, short wRightTemplateNum, byte bFlag);
    public static native  int FvmMatchTemplatesEx(byte[] bPtrTemplateLeft, byte[] bPtrTemplateRight, short wRightTemplateNum, byte bFlag, short[] Reserved);
    public static native  int FvmSetSecurityLevel(short wSecurityLevel_1Vn, short wSecurityLevel_1V1);
    public static native  int FvmAddUserForMatch(byte[] bUserHeader, short wHeaderLen, byte[] bTemplatesBuff, short dBuffSize, short wFlag);
    public static native  int FvmDeleteUserForMatch(byte[] bUserID);
    public static native  int FvmMatchWithAll(byte[] bTemplateBuff, short wTemplateBuffSize, byte[] bMatchResultBuff, short wResultBuffSize, byte bFlag);
    public static native  int FvmMatchWithAllEx(byte[] bTemplateBuff, short wTemplateBuffSize, byte[] bMatchResultBuff, short wResultBuffSize, byte bFlag, short[] Reserved);
    public static native  int FvmMatchInDepartment(short wDepNo, byte[] bTemplateBuff, short wTemplateBuffSize, byte[] bMatchResultBuff, short wResultBuffSize, byte bFlag);
    public static native  int FvmMatchByUserID(byte[] bUserID, byte[] bTemplateBuff, short wTemplateBuffSize, byte[] bMatchResultBuff, short wResultBuffSize, byte bFlag);
    public static native  int FvmMatchByCardNo(byte[] bCardNo, byte[] bTemplateBuff, short wTemplateBuffSize, byte[] bMatchResultBuff, short wResultBuffSize, byte bFlag);
}
