package WedoneBioVein;

/**
 * Created by Achang on 2016/1/23.
 */
public class UserData {
    public static int D_USER_HDR_LEN = 124;//用户信息头数据长度
    public static int D_USER_TEMPLATE_SIZE = 512;//单模板数据长度
    public static int D_USER_TEMPLATE_NUM = 20;//模板个数

    public static int D_USER_HDR_USERNAME_LEN = 8;//用户姓名数据长度
    public static int D_USER_HDR_ICCARD_LEN = 8;//用户卡号数据长度
    public static int D_USER_HDR_USERID_LEN = 16;//用户编号数据长度

    public static int D_USER_HDR_OFFSET_UID = 0;
    public static int D_USER_HDR_OFFSET_TEMPLATENUM = 4;
    public static int D_USER_HDR_OFFSET_DEPNO = 5;
    public static int D_USER_HDR_OFFSET_USERNAME = 6;
    public static int D_USER_HDR_OFFSET_CARDNUMBER = 14;
    public static int D_USER_HDR_OFFSET_USERID = 22;
    public static int D_USER_HDR_OFFSET_DOORCTRL = 38;

    private byte m_UserData[] = new byte[D_USER_HDR_LEN + D_USER_TEMPLATE_SIZE*D_USER_TEMPLATE_NUM];

    public boolean ClearData(){
        for(int nCnt = 0; nCnt < D_USER_HDR_LEN + D_USER_TEMPLATE_SIZE*D_USER_TEMPLATE_NUM; nCnt++){
            m_UserData[nCnt] = 0x00;
        }
        return true;
    }

    public long GetUserData(byte bDataBuff[], short sMaxLen){
        if((D_USER_HDR_LEN + D_USER_TEMPLATE_SIZE*GetTemplateNum()) > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的用户信息头数据
            return 0;
        }

        //把内部数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < (D_USER_HDR_LEN + D_USER_TEMPLATE_SIZE*GetTemplateNum()); nCnt++){
            bDataBuff[nCnt] = m_UserData[nCnt];
        }
        return D_USER_HDR_LEN + D_USER_TEMPLATE_SIZE*GetTemplateNum();
    }
    public long SetUserData(byte bDataBuff[], short sDataLen){
        if((D_USER_HDR_LEN + D_USER_TEMPLATE_SIZE*D_USER_TEMPLATE_NUM) < sDataLen){
            //数据超过缓冲区大小，不进行拷贝
            return 0;
        }

        //把缓冲区数据拷贝到内部数据区中去。
        for(int nCnt = 0; nCnt < sDataLen; nCnt++){
            m_UserData[nCnt] = bDataBuff[nCnt];
        }
        return sDataLen;
    }

    public long GetUserHdrData(byte bDataBuff[], short sMaxLen){
        if(D_USER_HDR_LEN > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的用户信息头数据
            return 0;
        }

        //把内部数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < D_USER_HDR_LEN; nCnt++){
            bDataBuff[nCnt] = m_UserData[nCnt];
        }
        return D_USER_HDR_LEN;
    }
    public long SetUserHdrData(byte bDataBuff[], short sDataLen){
        if(D_USER_HDR_LEN != sDataLen){
            //数据超过缓冲区大小，不进行拷贝
            return 0;
        }

        //把缓冲区数据拷贝到内部数据区中去。
        for(int nCnt = 0; nCnt < sDataLen; nCnt++){
            m_UserData[nCnt] = bDataBuff[nCnt];
        }
        return sDataLen;
    }

    public long GetTemplateData(byte bDataBuff[], short sMaxLen){
        if(D_USER_TEMPLATE_SIZE*GetTemplateNum() > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的模板数据
            return 0;
        }

        //把内部数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < D_USER_TEMPLATE_SIZE*GetTemplateNum(); nCnt++){
            bDataBuff[nCnt] = m_UserData[D_USER_HDR_LEN + nCnt];
        }
        return D_USER_TEMPLATE_SIZE*GetTemplateNum();
    }
    public byte[] TemplateData(){
        int nTemplateNum = GetTemplateNum();
        if(0 == nTemplateNum){
            return null;
        }
        byte[] templateData = new byte[D_USER_TEMPLATE_SIZE*nTemplateNum];
        //把内部数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < D_USER_TEMPLATE_SIZE*nTemplateNum; nCnt++){
            templateData[nCnt] = m_UserData[D_USER_HDR_LEN + nCnt];
        }
        return templateData;
    }
    public long SetTemplateData(byte bDataBuff[], short sDataLen){
        if((D_USER_TEMPLATE_SIZE > sDataLen) || (D_USER_TEMPLATE_SIZE*D_USER_TEMPLATE_NUM < sDataLen)){
            //数据超过缓冲区大小，不进行拷贝
            return 0;
        }

        //把缓冲区数据拷贝到内部数据区中去。
        for(int nCnt = 0; nCnt < sDataLen; nCnt++){
            m_UserData[D_USER_HDR_LEN + nCnt] = bDataBuff[nCnt];
        }
        SetTemplateNum((byte)(sDataLen/D_USER_TEMPLATE_SIZE));

        return D_USER_TEMPLATE_SIZE*GetTemplateNum();
    }
    public long AddTemplateData(byte bDataBuff[], short sDataLen){
        if((D_USER_TEMPLATE_SIZE > sDataLen) || (D_USER_TEMPLATE_SIZE*D_USER_TEMPLATE_NUM < (sDataLen + D_USER_TEMPLATE_SIZE*GetTemplateNum()))){
            //数据超过缓冲区大小，不进行拷贝
            return 0;
        }

        //把缓冲区数据拷贝到内部数据区中去。
        int nTotalOffset = D_USER_TEMPLATE_SIZE*GetTemplateNum() + D_USER_HDR_LEN;
        sDataLen = (short)(D_USER_TEMPLATE_SIZE*((short)(sDataLen/D_USER_TEMPLATE_SIZE)));
        for(int nCnt = 0; nCnt < sDataLen; nCnt++){
            m_UserData[nTotalOffset + nCnt] = bDataBuff[nCnt];
        }
        SetTemplateNum((byte)((nTotalOffset - D_USER_HDR_LEN + sDataLen)/D_USER_TEMPLATE_SIZE));

        return sDataLen;
    }

    //设置与读取UID
    public void SetUid(long lUid){
        m_UserData[D_USER_HDR_OFFSET_UID] = (byte)(lUid & 0xFF);
        m_UserData[D_USER_HDR_OFFSET_UID + 1] = (byte)((lUid & 0xFF00) >>> 8);
        m_UserData[D_USER_HDR_OFFSET_UID + 2] = (byte)((lUid & 0xFF0000) >>> 16);
        m_UserData[D_USER_HDR_OFFSET_UID + 3] = (byte)((lUid & 0xFF000000) >>> 24);
    }
    public long GetUid(){
        long lRetVal = 0;
        lRetVal = (m_UserData[D_USER_HDR_OFFSET_UID] & 0xFF) + ((m_UserData[D_USER_HDR_OFFSET_UID + 1] & 0xFF) << 8);
        lRetVal += ((m_UserData[D_USER_HDR_OFFSET_UID + 2] & 0xFF) << 16) + ((m_UserData[D_USER_HDR_OFFSET_UID + 3] & 0xFF) << 24);
        return lRetVal;
    }

    //设置与读取模板数量
    public void SetTemplateNum(byte bTemplateNum){
        m_UserData[D_USER_HDR_OFFSET_TEMPLATENUM] = bTemplateNum;
    }
    public byte GetTemplateNum(){
        return m_UserData[D_USER_HDR_OFFSET_TEMPLATENUM];
    }

    //设置与读取部门编号
    public void SetDepNo(byte bDepNo){
        m_UserData[D_USER_HDR_OFFSET_DEPNO] = bDepNo;
    }
    public byte GetDepNo(){
        return m_UserData[D_USER_HDR_OFFSET_DEPNO];
    }

    //设置与读取姓名数据
    public long SetUserName(byte bDataBuff[], short sDataLen){
        if(D_USER_HDR_USERNAME_LEN < sDataLen){
            return 0;
        }

        for(int nCnt = 0; nCnt < sDataLen; nCnt++) {
            m_UserData[D_USER_HDR_OFFSET_USERNAME + nCnt] = bDataBuff[nCnt];
        }

        return sDataLen;
    }
    public long GetUserName(byte bDataBuff[], short sMaxLen){
        if(D_USER_HDR_USERNAME_LEN > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的用户姓名数据
            return 0;
        }

        //把数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < D_USER_HDR_USERNAME_LEN; nCnt++){
            bDataBuff[nCnt] = m_UserData[D_USER_HDR_OFFSET_USERNAME + nCnt];
        }
        return D_USER_HDR_USERNAME_LEN;
    }

    //设置与读取IC卡数据
    public long SetIcCard(byte bDataBuff[], short sDataLen){
        if(D_USER_HDR_ICCARD_LEN < sDataLen){
            return 0;
        }

        for(int nCnt = 0; nCnt < sDataLen; nCnt++) {
            m_UserData[D_USER_HDR_OFFSET_CARDNUMBER + nCnt] = bDataBuff[nCnt];
        }

        return sDataLen;
    }
    public long GetIcCard(byte bDataBuff[], short sMaxLen){
        if(D_USER_HDR_ICCARD_LEN > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的数据
            return 0;
        }

        //把数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < D_USER_HDR_ICCARD_LEN; nCnt++){
            bDataBuff[nCnt] = m_UserData[D_USER_HDR_OFFSET_CARDNUMBER + nCnt];
        }
        return D_USER_HDR_ICCARD_LEN;
    }

    //设置与读取用户ID数据
    public long SetUserId(byte bDataBuff[], short sDataLen){
        if(D_USER_HDR_USERID_LEN < sDataLen){
            return 0;
        }

        for(int nCnt = 0; nCnt < sDataLen; nCnt++) {
            m_UserData[D_USER_HDR_OFFSET_USERID + nCnt] = bDataBuff[nCnt];
        }

        return sDataLen;
    }
    public long GetUserId(byte bDataBuff[], short sMaxLen){
        if(D_USER_HDR_USERID_LEN > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的数据
            return 0;
        }

        //把数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < D_USER_HDR_USERID_LEN; nCnt++){
            bDataBuff[nCnt] = m_UserData[D_USER_HDR_OFFSET_USERID + nCnt];
        }
        return D_USER_HDR_USERID_LEN;
    }

    //设置与读取开门控制的设置
    public void SetDoorCtrl(byte bDoorCtrl){
        m_UserData[D_USER_HDR_OFFSET_DOORCTRL] = bDoorCtrl;
    }
    public byte GetDoorCtrl(){
        return m_UserData[D_USER_HDR_OFFSET_DOORCTRL];
    }

}
