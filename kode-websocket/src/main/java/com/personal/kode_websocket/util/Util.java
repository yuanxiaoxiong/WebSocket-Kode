package com.personal.kode_websocket.util;

import android.content.Context;
import android.widget.Toast;

public class Util {
    //public static final String ws = "ws://echo.websocket.org";//websocket测试地址
    public static String ws = "ws://kodes.free.idcfengye.com/websocket/push";//websocket测试地址

    public void setWs(String ws) {
        Util.ws = ws;
    }

    public String getWs() {
        return ws;
    }

    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }
}
