package com.personal.push;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.personal.kode_websocket.push.JWebSocketClientService;


/**
 * 已实现保活，保活关键在于：applicationId "com.yxc.websocketclientdemo"//该id为进程保活的关键。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, JWebSocketClientService.class);
        startService(intent);
    }
}
