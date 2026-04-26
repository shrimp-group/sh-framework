package com.wkclz.web.helper;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Description:
 * Created: wangkaicun @ 2017-10-21 上午12:41
 */
public class IpHelper {
    private static final Logger logger = LoggerFactory.getLogger(IpHelper.class);

    public static String getUpstreamIp(HttpServletRequest req){
        String remoteAddr = req.getRemoteAddr();
        return remoteAddr;
    }

    public static String getOriginIp(HttpServletRequest req) {
        String ipAddress = req.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = req.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = req.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = req.getRemoteAddr();
            if ("127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
                // 根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    logger.error(e.getMessage(), e);
                }
                ipAddress = inet == null ? null : inet.getHostAddress();
            }
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(",")).trim();
        }
        return ipAddress;
    }

}
