package com.personal.kode_websocket.push;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Description: ws 管理类，静态内部单例
 * Created by kode on 2020/6/7.
 */
public class WebSocketManager {

    private final String TAG = WebSocketManager.class.getSimpleName();
    private final static int MAX_NUM = 5;//最大重连次数
    private final static int MILLIS = 5000;//重连间隔时间，毫秒

    private OkHttpClient client;
    private Request request;
    private IReceiveMessage iReceiveMessage;
    private WebSocket mWebSocket;

    private boolean isConnect;//是否已连接

    private int connectNum = 0;//当前重连次数

    private static class HolderClass {
        private final static WebSocketManager instance = new WebSocketManager();
    }

    public static WebSocketManager getInstance() {
        return WebSocketManager.HolderClass.instance;
    }

    public void init(String url, IReceiveMessage receiveMessage) {
        client = new OkHttpClient.Builder()
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
        request = new Request.Builder().url(url).build();
        iReceiveMessage = receiveMessage;
        connect();
    }

    /**
     * 连接
     */
    private void connect() {
        if (isConnect()) {
            Log.d(TAG, "connect: 已连接");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.newWebSocket(request, createWSListener());
            }
        }).start();
    }

    /**
     * 创建ws监听
     *
     * @return
     */
    private WebSocketListener createWSListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                Log.d(TAG, "onOpen: " + response.toString());
                mWebSocket = webSocket;
                isConnect = response.code() == 101;
                if (!isConnect) {
                    //reconnect();
                } else {
                    Log.d(TAG, "onOpen: 连接成功！");
                    if (iReceiveMessage != null) {
                        iReceiveMessage.onConnectSucceeded();
                    }
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                if (null != iReceiveMessage) {
                    iReceiveMessage.onStringMessage(text);
                }
                Log.d(TAG, "onMessage: " + text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                if (null != iReceiveMessage) {
                    iReceiveMessage.onByteStringMessage(bytes);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                mWebSocket = null;
                isConnect = false;
                if (null != iReceiveMessage) {
                    iReceiveMessage.onClosing(reason);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                mWebSocket = null;
                isConnect = false;
                if (null != iReceiveMessage) {
                    iReceiveMessage.onClose(reason);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                if (response != null) {
                    Log.d(TAG, "onFailure: " + response.message());
                }
                Log.d(TAG, "onFailure: " + t.getMessage());
                isConnect = false;
                if (null != iReceiveMessage) {
                    iReceiveMessage.onConnectFailed();
                }
                //reconnect();
            }
        };
    }

    /**
     * 重连,会无限循环
     */
    public void reconnect() {
        if (connectNum < MAX_NUM) {
            try {
                Thread.sleep(MILLIS);
                connect();
                connectNum++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            connectNum = 0;
            connect();
        }
    }

    /**
     * 是否连接
     *
     * @return
     */
    public boolean isConnect() {
        return mWebSocket != null && isConnect;
    }

    /**
     * 发送字符串文本
     *
     * @param text
     * @return
     */
    public boolean sendMessage(String text) {
        if (!isConnect()) return false;
        return mWebSocket.send(text);
    }

    /**
     * 发送字符集文本
     *
     * @param byteString
     * @return
     */
    public boolean sendByteMessage(ByteString byteString) {
        if (!isConnect()) return false;
        return mWebSocket.send(byteString);
    }

    /**
     * 关闭连接
     *
     * @return
     */
    public boolean close() {
        if (isConnect()) {
            mWebSocket.cancel();
            mWebSocket.close(1001, "客户端主动关闭连接");
            mWebSocket = null;
            return true;
        }
        return false;
    }
}
