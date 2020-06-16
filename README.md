# WebSocket-Kode
基于WebSocket实现的即时通信（消息推送、聊天IM、实时音视频）

- 消息推送（已实现，兼容android 4.3-9.0）
- 聊天IM（未实现）
- 实时音视频（未实现）

1.build.gradle中添加依赖：

```java
implementation 'com.github.kehuafu:WebSocket-Kode:1.0.5'
```

2.在需要【消息推送】的activity中初始化：

```java
//初始化消息推送
PushManger.getInstance().init(websocketUrl, context,connection,receiver);
```

（1）其中:

​	connection：将消息推送服务与当前acticity进行绑定

​	receiver：用于接收推送服务返回来的消息

（2）connection示例：

```java
    private PushService pushService;
    private PushService.PushServiceBinder binder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: ");
            binder = (PushService.PushServiceBinder) iBinder;
            pushService = binder.getService();
            //模拟发送消息
            Map<String, Object> map = new HashMap<>();
            map.put("to", "2017001");
            map.put("content", "xxxx 查看详情>>");
            map.put("status", "200");
            if (pushService.client.isConnect()) {
            pushService.sendMsg(JSON.toJSONString(map));
        	}
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };
```

(3) receiver示例：

```java
    private MessageReceiver receiver;

    private class MessageReceiver extends BroadcastReceiver {

        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("message");
            //接收消息
            Log.d(TAG, "onReceive: " + msg);
        }
    }
```

3.注意事项：

- 若非androidx版本下的项目，需要在gradle.properties中添加：

  ```java
  android.useAndroidX=true
  android.enableJetifier=true
  ```

- 解决AS编译安装失败报Error: INSTALL_PARSE_FAILED_MANIFEST_MALFORMED的处理方式（android 9.0以上）：

  [https://blog.csdn.net/final__static/article/details/90712655]: 

- serivce保活关键，需在app/builld.gradle中修改applicationId:

  ```java
  applicationId "com.yxc.websocket"
  ```

  