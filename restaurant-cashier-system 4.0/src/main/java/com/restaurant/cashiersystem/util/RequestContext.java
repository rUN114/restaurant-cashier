package com.restaurant.cashiersystem.util;

/**
 * 请求上下文 ThreadLocal，用于操作日志记录 IP 等信息
 */
public class RequestContext {
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();

    public static void setClientIp(String ip) { CLIENT_IP.set(ip); }
    public static String getClientIp() { return CLIENT_IP.get(); }
    public static void clear() { CLIENT_IP.remove(); }
}
