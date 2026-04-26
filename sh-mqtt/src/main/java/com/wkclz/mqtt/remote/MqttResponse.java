package com.wkclz.mqtt.remote;

import lombok.Data;

@Data
public class MqttResponse {

    /**
     * ok状态，正常返回result，否则返回errorMessage
     */
    public static final int OK = 20000;

    /**
     * 客户端超时未处理
     */
    public static final int TIMEOUT = 40001;

    /**
     * 服务端主动取消
     */
    public static final int CANCEL = 40002;

    private int mStatus = OK;

    /**
     * request生成的messageId
     */
    private String mId;

    /**
     * 收到的消息体
     */
    private String messageResult;

    /**
     * 状态不是成功返回的错误信息
     */
    private String errorMessage;

    public MqttResponse(String mId) {
        this.mId = mId;
    }
}
