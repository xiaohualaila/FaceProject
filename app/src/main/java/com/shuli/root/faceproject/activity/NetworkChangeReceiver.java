package com.shuli.root.faceproject.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkChangeReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "网络状态改变..",
                Toast.LENGTH_SHORT).show();
        //得到网络连接管理器
        ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //通过管理器得到网络实例
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        //判断是否连接
        if (networkInfo != null && networkInfo.isAvailable()) {
            Toast.makeText(context, "网络可以使用!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "网络未连接",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
