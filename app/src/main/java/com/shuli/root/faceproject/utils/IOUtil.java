package com.shuli.root.faceproject.utils;

import java.io.RandomAccessFile;

public class IOUtil {
    /*
     *默认是0，名称要输入大写，接了两个IO口，一个PB7,PB2
     */
    public static void setGpio(String name,boolean isCfg,int value){
        String fileName = "/sys/class/gpio_sw/" + name +(isCfg?"/cfg":"/data");
        try{
            RandomAccessFile file = new RandomAccessFile(fileName,"rw");
            file.writeBytes(Integer.toString(value));
            file.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static int getGpio(String name,boolean isCfg){
        String fileName = "/sys/class/gpio_sw/" + name +(isCfg?"/cfg":"/data");
        try {
            RandomAccessFile file = new RandomAccessFile(fileName, "rw");
            int reValue = Integer.valueOf(file.readLine().toString());
            file.close();
            return reValue;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }
}
