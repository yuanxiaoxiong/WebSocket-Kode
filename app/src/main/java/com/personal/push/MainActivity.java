package com.personal.push;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.personal.kode_websocket.push.PushManger;
import com.personal.kode_websocket.push.PushService;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;


/**
 * @Kode 消息推送使用示例
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private PushService pushService;
    private PushService.PushServiceBinder binder;
    private Map<String, String> map = new HashMap<>();

    /**
     * 服务连接
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "服务与活动成功绑定");
            binder = (PushService.PushServiceBinder) iBinder;
            pushService = binder.getService();

            //模拟发送消息
            Map<String, Object> map = new HashMap<>();
            TestModel testModel = new TestModel();
            testModel.setId("001");
            testModel.setName("我是某某阿");
            map.put("to", "2017001");
//            map.put("content", JSON.toJSON(testModel));
            map.put("content", "17软件工程2班-201710098182-柯华富 查看详情>>");
            map.put("status", "200");
            sendMessage(JSON.toJSONString(map));
            try {
                //模拟关闭推送服务
                Thread.sleep(3000);
                //pushService.closeConnect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "服务与活动成功断开");
        }
    };

    /**
     * 消息接收器
     */
    private class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "onReceive->服务器返回来的消息:" + message);
            //模拟收到服务器消息的响应
            JSONObject jsonObject = JSON.parseObject(message);
            map.put("uuid", jsonObject.getString("uuid"));
            map.put("status", "201");
            sendMessage(JSON.toJSONString(map));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String url = "ws://kodes.free.idcfengye.com/websocket/2017001";
        //初始化消息推送服务
        PushManger.getInstance().init(url, this, serviceConnection, new MessageReceiver());
    }

    //发送消息
    private void sendMessage(String text) {
        try {
            Thread.sleep(2000);//等待2秒，待连接上服务器再发送消息
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (pushService.client.isConnect()) {
            pushService.sendMsg(text);
        }
    }
}
