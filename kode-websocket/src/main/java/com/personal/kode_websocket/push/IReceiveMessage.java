package com.personal.kode_websocket.push;

import okio.ByteString;

/**
 * Description: 消息状态回调接口
 * Created by kode on 2020/6/7.
 */
public interface IReceiveMessage {

    void onConnectSucceeded();

    void onConnectFailed();

    void onClosing(String reason);

    void onClose(String reason);

    void onStringMessage(String text);

    void onByteStringMessage(ByteString text);
}
