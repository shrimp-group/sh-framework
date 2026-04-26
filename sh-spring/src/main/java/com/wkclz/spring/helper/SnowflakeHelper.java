package com.wkclz.spring.helper;


import com.wkclz.core.exception.SystemException;
import com.wkclz.spring.config.Sys;
import com.wkclz.tool.utils.SnowflakeIdWorker;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author shrimp
 */
public class SnowflakeHelper {

    private static SnowflakeIdWorker SFIW = null;

    // 生成唯一序列
    public static synchronized long getSnowflakeId() {
        if (SFIW == null) {
            long workId = SnowflakeHelper.getWorkId();
            long datacenterId = SnowflakeHelper.getDatacenterId();
            SFIW = new SnowflakeIdWorker(workId, datacenterId);
        }
        return SFIW.nextId();
    }

    /**
     * 获取机器编码 (0~31)
     */
    private static long getWorkId() {
        StringBuilder sb = new StringBuilder();
        Enumeration<NetworkInterface> e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
            throw SystemException.of("获取机器编码失败");
        }
        while (e.hasMoreElements()) {
            NetworkInterface ni = e.nextElement();
            sb.append(ni.toString());
        }
        // hashCode 可能为负数，需取绝对值后对 maxWorkerId(31) 取模
        long hashCode = sb.toString().hashCode();
        return Math.abs(hashCode % 31L);
    }

    /**
     * 获取数据中心编码 (0~31)
     */
    private static long getDatacenterId() {
        // hashCode 可能为负数，需取绝对值后对 maxDatacenterId(31) 取模
        long hashCode = Sys.getCurrentEnv().hashCode();
        return Math.abs(hashCode % 31L);
    }

    /*
    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            long snowflakeId = SnowflakeHelper.getSnowflakeId();
            System.out.println(snowflakeId);
        }
    }
    */


}
