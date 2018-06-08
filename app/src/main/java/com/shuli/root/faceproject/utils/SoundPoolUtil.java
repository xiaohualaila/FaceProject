package com.shuli.root.faceproject.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import com.shuli.root.faceproject.R;

/**
 * Created by Administrator on 2017/12/27.
 */

public class SoundPoolUtil {
    private static SoundPoolUtil soundPoolUtil;
    private static SoundPool soundPool;


    //单例模式
    public static SoundPoolUtil getInstance(Context context) {
        if (soundPoolUtil == null)
            soundPoolUtil = new SoundPoolUtil(context);
        return soundPoolUtil;
    }

    private SoundPoolUtil(Context context) {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 10);
        //加载音频文件
        soundPool.load(context, R.raw.face_right, 1);//验证成功！
        soundPool.load(context, R.raw.face_wrong, 2);//验证失败！
        soundPool.load(context, R.raw.put_your_finger, 8);//请放入手指
        soundPool.load(context, R.raw.put_move_finger, 9);//请移开手指
        soundPool.load(context, R.raw.data_wrong, 3);//数据有误
        soundPool.load(context, R.raw.same_finger, 4);//采集的特征必须属于同一根手指
        soundPool.load(context, R.raw.feature_successful, 5);//验证成功
        soundPool.load(context, R.raw.read_wrong, 6);//读取失败
        soundPool.load(context, R.raw.no_find_finger, 7);//没有找到手指
    }

    public static void play(int number) {
        Log.d("tag", "number " + number);
        //播放音频
        soundPool.play(number, 0.9f, 0.9f, 0, 0, 1);
    }
}
