package com.shuli.root.faceproject.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver  extends BroadcastReceiver {
    private NetCallback netCallback;

    public void setNetCallback(NetCallback netCallback){
        this.netCallback = netCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        netCallback.setNetState(1);
        //得到网络连接管理器
        ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //通过管理器得到网络实例
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        //判断是否连接
        if (networkInfo != null && networkInfo.isAvailable()) {
            netCallback.setNetState(2);
        } else {
            netCallback.setNetState(3);
        }
    }
    public interface NetCallback{
        void setNetState(int state);
    }
}
