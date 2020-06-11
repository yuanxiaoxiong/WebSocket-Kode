package com.personal.kode_websocket.push;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;


import com.personal.kode_websocket.util.NotificationUtils;
import com.personal.kode_websocket.util.Util;

import okio.ByteString;


public class PushService extends Service {

    public WebSocketManager client;
    private PushServiceBinder mBinder = new PushServiceBinder();
    private final static int GRAY_SERVICE_ID = 1001;
    private boolean closeService = false;//标记是否关闭服务

    //灰色保活
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    PowerManager.WakeLock wakeLock;//锁屏唤醒

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    //用于Activity和service通讯
    public class PushServiceBinder extends Binder {
        public PushService getService() {
            return PushService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //初始化websocket
        if (!isConnected) {
            initSocketClient();
            mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
        }
        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25) {
            //Android4.3 - Android7.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            //Android7.0以上app启动后通知栏会出现一条"正在运行"的通知
            startForeground(GRAY_SERVICE_ID, new Notification());//保活关键
        }

        acquireWakeLock();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        closeConnect();
        super.onDestroy();
    }

    public PushService() {
    }


    /**
     * 初始化websocket连接
     */
    private void initSocketClient() {
        client = WebSocketManager.getInstance();
        client.init(Util.ws, new IReceiveMessage() {
            @Override
            public void onConnectSucceeded() {
                Log.e("PushService", "websocket->连接成功");
                isConnected = true;
            }

            @Override
            public void onConnectFailed() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!closeService) {
                    Log.e("PushService", "websocket->连接失败");
                    client.reconnect();
                    isConnected = false;
                }
            }

            @Override
            public void onClosing(String reason) {
                isConnected = false;
            }

            @Override
            public void onClose(String reason) {
                isConnected = false;
            }

            @Override
            public void onStringMessage(String message) {
                Log.e("PushService", "收到的消息：" + message);

                Intent intent = new Intent();
                intent.setAction("com.xch.servicecallback.content");
                intent.putExtra("message", message);
                sendBroadcast(intent);

                checkLockAndShowNotification("标题", message);
            }

            @Override
            public void onByteStringMessage(ByteString text) {

            }
        });
    }


    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(final String msg) {
        if (null != client) {
            Log.e("PushService", "发送的消息：" + msg);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    client.sendMessage(msg);
                }
            }).start();
        }
    }

    /**
     * 断开连接
     */
    @SuppressLint("WrongConstant")
    public void closeConnect() {
        try {
            if (null != client) {
                client.close();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(GRAY_SERVICE_ID);
                }
                stopForeground(true);
                stopSelf();
                isConnected = false;
                closeService = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
            closeService = true;
        }
    }


//    -----------------------------------消息通知--------------------------------------------------------

    /**
     * 检查锁屏状态，如果锁屏先点亮屏幕
     *
     * @param title
     * @param content
     */
    private void checkLockAndShowNotification(String title, String content) {
        //管理锁屏的一个服务
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {//锁屏
            //获取电源管理器对象
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                wl.acquire();  //点亮屏幕
                wl.release();  //任务结束后释放
            }
            sendNotification(title, content);
        } else {
            sendNotification(title, content);
        }
    }

    /**
     * 发送通知
     *
     * @param content
     */
    private void sendNotification(String title, String content) {
//        Intent intent = new Intent();
//        intent.setClass(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification notification = new NotificationCompat.Builder(this)
//                .setAutoCancel(true)
//                // 设置该通知优先级
//                .setPriority(Notification.PRIORITY_MAX)
//                .setSmallIcon(R.drawable.icon)
//                .setContentTitle("服务器")
//                .setContentText(content)
//                .setVisibility(VISIBILITY_PUBLIC)
//                .setWhen(System.currentTimeMillis())
//                // 向通知添加声音、闪灯和振动效果
//                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
//                .setContentIntent(pendingIntent)
//                .build();
//        notifyManager.notify(1, notification);//id要保证唯一
        NotificationUtils utils = new NotificationUtils(this);
        utils.sendNotification(1, title, content);
    }


    //    -------------------------------------websocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 60 * 1000;//每隔10分钟进行一次对长连接的心跳检测,不管是否连接，都进行一次重连（防止线程睡眠）
    private static boolean isConnected;
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e("PushService", "心跳包检测websocket连接状态->" + isConnected);
            if (client != null) {
                reconnectWs();
            } else {
                //如果client已为空，重新初始化连接
                isConnected = false;
                initSocketClient();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    /**
     * 开启重连
     */
    private void reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e("PushService", "开启重连");
                    if (client != null && !closeService) {
                        client.reconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
