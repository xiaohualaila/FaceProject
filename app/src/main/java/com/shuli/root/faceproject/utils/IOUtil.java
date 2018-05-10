package com.shuli.root.faceproject.utils;

import java.io.DataOutputStream;
import java.io.IOException;

public class IOUtil {
    public static  void input_num_1(String str){
        int num = Integer.parseInt(str);
        String exp_cmd = "echo " + num + " > /sys/class/gpio/export";
        CommandExec(exp_cmd);
        String dir_cmd = "echo out" + " > /sys/class/gpio/gpio"+num +"/direction";
        CommandExec(dir_cmd);
        String out_cmd = "echo "+ 1 + " > /sys/class/gpio/gpio"+num+"/value";
        CommandExec(out_cmd);
        String unexp_cmd = "echo " + num + " > /sys/class/gpio/unexport";
        CommandExec(unexp_cmd);
    }

    public static  void input_num_0(String str){
        int num = Integer.parseInt(str);
        String exp_cmd = "echo " + num + " > /sys/class/gpio/export";
        CommandExec(exp_cmd);
        String dir_cmd = "echo out" + " > /sys/class/gpio/gpio"+num +"/direction";
        CommandExec(dir_cmd);
        String out_cmd = "echo "+ 0 + " > /sys/class/gpio/gpio"+num+"/value";
        CommandExec(out_cmd);
        String unexp_cmd = "echo " + num + " > /sys/class/gpio/unexport";
        CommandExec(unexp_cmd);
    }

    public static void CommandExec(String cmd) {
        Process process;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os=new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd+"\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
