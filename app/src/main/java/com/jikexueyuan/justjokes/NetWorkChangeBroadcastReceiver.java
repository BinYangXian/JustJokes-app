package com.jikexueyuan.justjokes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by fangc on 2016/5/14.
 */
public class NetWorkChangeBroadcastReceiver extends BroadcastReceiver {
//    final static String ACTION="com.jikexueyuan.justjokes.intent.action.NetWorkConnectedBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();// TODO: 2016/5/14 求替代方法？
            //用getActiveNetworkInfo方法替代
            int i;
            for (i = 0; i < networkInfos.length; i++) {
                NetworkInfo.State state = networkInfos[i].getState();
                if (NetworkInfo.State.CONNECTED == state) {
                    context.sendBroadcast(new Intent(MainFragment.ACTION));
                    System.out.println("------------> Network is ok");
                    return;
                }
            }
            if (i==networkInfos.length){
                System.out.println("------------> No Network");
            }
        }

    }
}
