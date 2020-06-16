package com.personal.kode_websocket.push;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.personal.kode_websocket.util.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Description:  消息推送管理类
 * Created by kode on 2020/6/11.
 */
public class PushManger {

    private Context mContext;

    //静态内部类单例
    private static class HolderClass {
        private final static PushManger instance = new PushManger();
    }

    public static PushManger getInstance() {
        return PushManger.HolderClass.instance;
    }

    /**
     * 初始化消息推送服务
     *
     * @param url
     * @param context
     */
    public void init(String url, Context context, ServiceConnection serviceConnection, BroadcastReceiver chatMessageReceiver) {
        if (url != null && !url.isEmpty()) {
            Util.ws = url;
        }
        mContext = context;
        startJWebSClientService();
        bindService(serviceConnection);
        doRegisterReceiver(chatMessageReceiver);
        checkNotification(mContext);
    }

    /**
     * 启动服务（WebSocket客户端服务）
     */
    private void startJWebSClientService() {
        Intent intent = new Intent(mContext, PushService.class);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            mContext.startForegroundService(intent);
        } else {
            mContext.startService(intent);
        }
    }

    /**
     * 绑定服务
     */
    private void bindService(ServiceConnection serviceConnection) {
        Intent bindIntent = new Intent(mContext, PushService.class);
        mContext.bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * 动态注册广播
     */
    private void doRegisterReceiver(BroadcastReceiver chatMessageReceiver) {
        IntentFilter filter = new IntentFilter("com.xch.servicecallback.content");
        mContext.registerReceiver(chatMessageReceiver, filter);
    }

    /**
     * 检测是否开启通知
     *
     * @param context
     */
    private void checkNotification(final Context context) {
        if (!isNotificationEnabled(context)) {
            new AlertDialog.Builder(context).setTitle("温馨提示")
                    .setMessage("你还未开启系统通知，将影响消息的接收，要去开启吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setNotification(context);
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        }
    }

    /**
     * 如果没有开启通知，跳转至设置界面
     *
     * @param context
     */
    private void setNotification(Context context) {
        Intent localIntent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            localIntent.putExtra("app_package", context.getPackageName());
            localIntent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.addCategory(Intent.CATEGORY_DEFAULT);
            localIntent.setData(Uri.parse("package:" + context.getPackageName()));
        } else {
            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
            }
        }
        context.startActivity(localIntent);
    }

    /**
     * 获取通知权限,监测是否开启了系统通知
     *
     * @param context
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isNotificationEnabled(Context context) {

        String CHECK_OP_NO_THROW = "checkOpNoThrow";
        String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

        AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;

        Class appOpsClass = null;
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
                    String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);

            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
