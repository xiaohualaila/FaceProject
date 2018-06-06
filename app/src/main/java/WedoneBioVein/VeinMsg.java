package WedoneBioVein;

/**
 * Created by Achang on 2016/1/21.
 */
public class VeinMsg {
    public static int D_MSG_HEXLEN = 10396;//消息原始数据缓冲区长度
    public static int D_MSG_HDRLEN = 32; //消息头长度

    public static int D_MSG_OFFSET_MSEEAGEID = 0;
    public static int D_MSG_OFFSET_FLAG = 1;
    public static int D_MSG_OFFSET_DEVID = 2;
    public static int D_MSG_OFFSET_EXTRALEN = 4;
    public static int D_MSG_OFFSET_TIME = 8;
    public static int D_MSG_OFFSET_REGUID = 20;
    public static int D_MSG_OFFSET_REGTID = 24;
    public static int D_MSG_OFFSET_REGEXTRA = 26;
    public static int D_MSG_OFFSET_REGRESERVED = 28;
    public static int D_MSG_OFFSET_ERRCODE = 20;
    public static int D_MSG_OFFSET_EXTRAINFO = 24;
    public static int D_MSG_OFFSET_RESERVE = 28;
    public static int D_MSG_OFFSET_SETTINGTYPE = 20;
    public static int D_MSG_OFFSET_SETTINGEXTEND = 22;
    public static int D_MSG_OFFSET_SETTINGVALUE = 24;
    public static int D_MSG_OFFSET_SETTINGVALUE1 = 28;

    private byte[] m_HexData = new byte[D_MSG_HEXLEN];
    private byte[] m_AscData = new byte[D_MSG_HEXLEN*2 + 2];
    private int m_ExtraDataLen = 0;

    void VeinMsg(){
        m_HexData = new byte[D_MSG_HEXLEN];
        m_AscData = new byte[D_MSG_HEXLEN*2 + 2];
        m_ExtraDataLen = 0;
    }


    public boolean EncodeData(){
        byte LowByte, HighByte;
        m_AscData[0] = (byte)0x40; //消息编码以'@'符号开头
        for(int nCnt = 0; nCnt < (D_MSG_HDRLEN + m_ExtraDataLen); nCnt++){
            HighByte = (byte)((m_HexData[nCnt] & 0xF0) >>> 4);
            if(0x09 >= HighByte){
                HighByte += 0x30;
            }
            else{
                HighByte += (0x41 - 10);
            }
            LowByte = (byte)(m_HexData[nCnt] & 0x0F);
            if(0x09 >= LowByte){
                LowByte += 0x30;
            }
            else{
                LowByte += (0x41 - 10);
            }
            m_AscData[nCnt*2 + 1] = HighByte;
            m_AscData[nCnt*2 + 2] = LowByte;
        }
        m_AscData[(D_MSG_HDRLEN + m_ExtraDataLen)*2 + 1] = (byte)0x0D; //消息编码以0x0D结束
        return true;
    }

    public boolean ClearData(){
        m_ExtraDataLen = 0;
        for(int nCnt = 0; nCnt < D_MSG_HDRLEN; nCnt++){
            m_HexData[nCnt] = (byte)0x00;
        }
        return true;
    }

    //第一个参数bDataBuff为缓冲区，第二个参数sMaxLen为缓冲区中允许保存的最大数据长度
    public long GetMsgData(byte bDataBuff[], short sMaxLen){
        if(((D_MSG_HDRLEN + m_ExtraDataLen)*2 + 2) > sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的消息数据
            return 0;
        }

        EncodeData();//对数据进行编码

        //把编码的数据拷贝到参数指定的缓冲区中去。
        for(int nCnt = 0; nCnt < ((D_MSG_HDRLEN + m_ExtraDataLen)*2 + 2); nCnt++){
            bDataBuff[nCnt] = m_AscData[nCnt];
        }
        return ((D_MSG_HDRLEN + m_ExtraDataLen)*2 + 2);
    }

    public long SetMsgData(byte bDataBuff[], short sMaxLen){
        if((D_MSG_HEXLEN*2 + 2) < sMaxLen){
            //验证缓冲区的大小是否可以容纳完整的消息数据
            return 0;
        }

        int nStartPos = 0xFFFF, nEndPos = 0x00;
        //检测数据的起始和结束位置。
        for(int nCnt = 0; nCnt < sMaxLen; nCnt++){
            if((0x40 == bDataBuff[nCnt]) && (0xFFFF == nStartPos)){
                nStartPos = nCnt;
            }
            else if((0x0D == bDataBuff[nCnt]) && (nStartPos >= nEndPos)){
                nEndPos = nCnt;
                if(nEndPos > nStartPos){
                    break;
                }
            }
        }
        //判断有效数据的长度是否正确
        if((64 >= (nEndPos - nStartPos)) || (0 == ((nEndPos - nStartPos) % 2))){
            return 0;
        }
        //拷贝数据到内部缓冲区
        m_AscData[0] = 0x40;
        m_AscData[nEndPos - nStartPos] = 0x0D;
        byte bLowByte, bHighByte;
        for(int nCnt = nStartPos + 1; nCnt < nEndPos; nCnt += 2){
            bHighByte = bDataBuff[nCnt];
            bLowByte = bDataBuff[nCnt + 1];
            m_AscData[nCnt - nStartPos] = bHighByte;
            m_AscData[nCnt - nStartPos + 1] = bLowByte;
            if(0x41 <= bHighByte){
                bHighByte = (byte)(bHighByte - 0x41 + 10);
            }
            else{
                bHighByte = (byte)(bHighByte - 0x30);
            }
            if(0x41 <= bLowByte){
                bLowByte = (byte)(bLowByte - 0x41 + 10);
            }
            else{
                bLowByte = (byte)(bLowByte - 0x30);
            }
            m_HexData[(nCnt - nStartPos - 1)/2] = (byte)(bHighByte*16 + bLowByte);
        }
        m_ExtraDataLen = m_HexData[D_MSG_OFFSET_EXTRALEN] + m_HexData[D_MSG_OFFSET_EXTRALEN + 1]*256;
        m_ExtraDataLen += ((m_HexData[D_MSG_OFFSET_EXTRALEN + 2] << 16) + (m_HexData[D_MSG_OFFSET_EXTRALEN + 3] << 24));
        //解码内部缓冲区的数据
        return (nEndPos - nStartPos + 1);
    }

    public void SetMsgId(byte bMsgId){
        m_HexData[D_MSG_OFFSET_MSEEAGEID] = bMsgId;
    }
    public byte GetMsgId(){
        return m_HexData[D_MSG_OFFSET_MSEEAGEID];
    }

    public void SetFlag(byte bFlag){
        m_HexData[D_MSG_OFFSET_FLAG] = bFlag;
    }
    public byte GetFlag(){
        return m_HexData[D_MSG_OFFSET_FLAG];
    }

    public void SetDevId(long lDevId){
        m_HexData[D_MSG_OFFSET_DEVID] = (byte)(lDevId  & 0xFF);
        m_HexData[D_MSG_OFFSET_DEVID + 1] = (byte)((lDevId & 0xFF00) >>> 8);
    }
    public long GetDevId(){
        return  (m_HexData[D_MSG_OFFSET_DEVID] & 0xFF) + ((m_HexData[D_MSG_OFFSET_DEVID + 1] & 0xFF) << 8);
    }

    public void SetErrCode(long lErrorCode){
        m_HexData[D_MSG_OFFSET_ERRCODE] = (byte)(lErrorCode  & 0xFF);
        m_HexData[D_MSG_OFFSET_ERRCODE + 1] = (byte)((lErrorCode & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_ERRCODE + 2] = (byte)((lErrorCode & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_ERRCODE + 3] = (byte)((lErrorCode & 0xFF000000) >>> 24);
    }
    public long GetErrCode(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_ERRCODE] & 0xFF) + ((m_HexData[D_MSG_OFFSET_ERRCODE + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_ERRCODE + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_ERRCODE + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public void SetExtraInfo(long lExtraInfo){
        m_HexData[D_MSG_OFFSET_EXTRAINFO] = (byte)(lExtraInfo  & 0xFF);
        m_HexData[D_MSG_OFFSET_EXTRAINFO + 1] = (byte)((lExtraInfo & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_EXTRAINFO + 2] = (byte)((lExtraInfo & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_EXTRAINFO + 3] = (byte)((lExtraInfo & 0xFF000000) >>> 24);
    }
    public long GetExtraInfo(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_EXTRAINFO] & 0xFF) + ((m_HexData[D_MSG_OFFSET_EXTRAINFO + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_EXTRAINFO + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_EXTRAINFO + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public void SetReserveData(long lReserveData){
        m_HexData[D_MSG_OFFSET_RESERVE] = (byte)(lReserveData & 0xFF);
        m_HexData[D_MSG_OFFSET_RESERVE + 1] = (byte)((lReserveData & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_RESERVE + 2] = (byte)((lReserveData & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_RESERVE + 3] = (byte)((lReserveData & 0xFF000000) >>> 24);
    }
    public long GetReserveData(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_RESERVE] & 0xFF) + ((m_HexData[D_MSG_OFFSET_RESERVE + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_RESERVE + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_RESERVE + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public void SetRegUid(long lUid){
        m_HexData[D_MSG_OFFSET_REGUID] = (byte)(lUid  & 0xFF);
        m_HexData[D_MSG_OFFSET_REGUID + 1] = (byte)((lUid & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_REGUID + 2] = (byte)((lUid & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_REGUID + 3] = (byte)((lUid & 0xFF000000) >>> 24);
    }
    public long GetRegUid(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_REGUID] & 0xFF) + ((m_HexData[D_MSG_OFFSET_REGUID + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_REGUID + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_REGUID + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public void SetRegTid(short sTid){
        m_HexData[D_MSG_OFFSET_REGTID] = (byte)(sTid  & 0xFF);
        m_HexData[D_MSG_OFFSET_REGTID + 1] = (byte)((sTid & 0xFF00) >>> 8);
    }
    public short GetRegTid(){
        short sRetVal = 0;
        sRetVal =  (short)((m_HexData[D_MSG_OFFSET_REGUID] & 0xFF) + ((m_HexData[D_MSG_OFFSET_REGUID + 1] & 0xFF) << 8));
        return sRetVal;
    }

    public void SetRegExtra(short sRegExtra){
        m_HexData[D_MSG_OFFSET_REGEXTRA] = (byte)(sRegExtra & 0xFF);
        m_HexData[D_MSG_OFFSET_REGEXTRA + 1] = (byte)((sRegExtra & 0xFF00) >>> 8);
    }
    public short GetRegExtra(){
        short sRetVal = 0;
        sRetVal =  (short)((m_HexData[D_MSG_OFFSET_REGEXTRA] & 0xFF) + ((m_HexData[D_MSG_OFFSET_REGEXTRA + 1] & 0xFF) << 8));
        return sRetVal;
    }

    public void SetRegReserved(long lRegReserved){
        m_HexData[D_MSG_OFFSET_REGRESERVED] = (byte)(lRegReserved  & 0xFF);
        m_HexData[D_MSG_OFFSET_REGRESERVED + 1] = (byte)((lRegReserved & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_REGRESERVED + 2] = (byte)((lRegReserved & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_REGRESERVED + 3] = (byte)((lRegReserved & 0xFF000000) >>> 24);
    }
    public long GetRegReserved(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_REGRESERVED] & 0xFF) + ((m_HexData[D_MSG_OFFSET_REGRESERVED + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_REGRESERVED + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_REGRESERVED + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public void SetSettingType(short sSettingType){
        m_HexData[D_MSG_OFFSET_SETTINGTYPE] = (byte)(sSettingType & 0xFF);
        m_HexData[D_MSG_OFFSET_SETTINGTYPE + 1] = (byte)((sSettingType & 0xFF00) >>> 8);
    }
    public short GetSettingType(){
        short sRetVal = 0;
        sRetVal =  (short)((m_HexData[D_MSG_OFFSET_SETTINGTYPE] & 0xFF) + ((m_HexData[D_MSG_OFFSET_SETTINGTYPE + 1] & 0xFF) << 8));
        return sRetVal;
    }

    public void SetSettingExtend(short sSettingType){
        m_HexData[D_MSG_OFFSET_SETTINGEXTEND] = (byte)(sSettingType & 0xFF);
        m_HexData[D_MSG_OFFSET_SETTINGEXTEND + 1] = (byte)((sSettingType & 0xFF00) >>> 8);
    }
    public short GetSettingExtend(){
        short sRetVal = 0;
        sRetVal =  (short)((m_HexData[D_MSG_OFFSET_SETTINGEXTEND] & 0xFF) + ((m_HexData[D_MSG_OFFSET_SETTINGEXTEND + 1] & 0xFF) << 8));
        return sRetVal;
    }

    public void SetSettingValue(long lSettingValue){
        m_HexData[D_MSG_OFFSET_SETTINGVALUE] = (byte)(lSettingValue & 0xFF);
        m_HexData[D_MSG_OFFSET_SETTINGVALUE + 1] = (byte)((lSettingValue & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_SETTINGVALUE + 2] = (byte)((lSettingValue & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_SETTINGVALUE + 3] = (byte)((lSettingValue & 0xFF000000) >>> 24);
    }
    public long GetSettingValue(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_SETTINGVALUE] & 0xFF) + ((m_HexData[D_MSG_OFFSET_SETTINGVALUE + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_SETTINGVALUE + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_SETTINGVALUE + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public void SetSettingValue1(long lSettingValue){
        m_HexData[D_MSG_OFFSET_SETTINGVALUE1] = (byte)(lSettingValue & 0xFF);
        m_HexData[D_MSG_OFFSET_SETTINGVALUE1 + 1] = (byte)((lSettingValue & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_SETTINGVALUE1 + 2] = (byte)((lSettingValue & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_SETTINGVALUE1 + 3] = (byte)((lSettingValue & 0xFF000000) >>> 24);
    }
    public long GetSettingValue1(){
        long lRetVal = 0;
        lRetVal =  (m_HexData[D_MSG_OFFSET_SETTINGVALUE1] & 0xFF) + ((m_HexData[D_MSG_OFFSET_SETTINGVALUE1 + 1] & 0xFF) << 8);
        lRetVal += (((m_HexData[D_MSG_OFFSET_SETTINGVALUE1 + 2] & 0xFF) << 16) + ((m_HexData[D_MSG_OFFSET_SETTINGVALUE1 + 3] & 0xFF) << 24));
        return lRetVal;
    }

    public long GetExtraData(byte bDataBuff[], short sMaxLen){
        if(sMaxLen < m_ExtraDataLen) {
            return 0;
        }

        for(int nCnt = 0; nCnt < m_ExtraDataLen; nCnt++){
            bDataBuff[nCnt] = m_HexData[D_MSG_HDRLEN + nCnt];
        }

        return m_ExtraDataLen;
    }

    public long SetExtraData(byte bDataBuff[], short sExtraDataLen){
        if(sExtraDataLen > (D_MSG_HEXLEN - D_MSG_HDRLEN)) {
            return 0;
        }

        for(int nCnt = 0; nCnt < sExtraDataLen; nCnt++){
            m_HexData[D_MSG_HDRLEN + nCnt] = bDataBuff[nCnt];
        }
        m_ExtraDataLen = sExtraDataLen;

        m_HexData[D_MSG_OFFSET_EXTRALEN] = (byte)(m_ExtraDataLen & 0xFF);
        m_HexData[D_MSG_OFFSET_EXTRALEN + 1] = (byte)((m_ExtraDataLen & 0xFF00) >>> 8);
        m_HexData[D_MSG_OFFSET_EXTRALEN + 2] = (byte)((m_ExtraDataLen & 0xFF0000) >>> 16);
        m_HexData[D_MSG_OFFSET_EXTRALEN + 3] = (byte)((m_ExtraDataLen & 0xFF000000) >>> 24);

        return m_ExtraDataLen;
    }

}
