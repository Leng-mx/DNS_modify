package com.bupt.dnsrelay.utils;

import com.bupt.dnsrelay.dns.DNSMessage;
import com.bupt.dnsrelay.dns.DNSParser;
import com.bupt.dnsrelay.dns.DNSRecord;

/**
 * 调试工具类
 * 统一管理调试输出，避免重复的调试级别检查
 */
public class DebugUtils {
    
    /**
     * 打印调试信息（级别1）
     * @param debugLevel 当前调试级别
     * @param message 调试信息
     */
    public static void debug(int debugLevel, String message) {
        if (debugLevel >= 1) {
            System.out.println(message);
        }
    }
    
    /**
     * 打印详细调试信息（级别2）
     * @param debugLevel 当前调试级别
     * @param message 调试信息
     */
    public static void debugVerbose(int debugLevel, String message) {
        if (debugLevel >= 2) {
            System.out.println(message);
        }
    }
    
    /**
     * 格式化打印调试信息（级别1）
     * @param debugLevel 当前调试级别
     * @param format 格式字符串
     * @param args 参数
     */
    public static void debugf(int debugLevel, String format, Object... args) {
        if (debugLevel >= 1) {
            System.out.printf(format, args);
        }
    }
    
    /**
     * 格式化打印详细调试信息（级别2）
     * @param debugLevel 当前调试级别
     * @param format 格式字符串
     * @param args 参数
     */
    public static void debugVerboseff(int debugLevel, String format, Object... args) {
        if (debugLevel >= 2) {
            System.out.printf(format, args);
        }
    }
    
    /**
     * 打印DNS消息（级别2）
     * @param debugLevel 当前调试级别
     * @param message DNS消息
     */
    public static void printDNSMessage(int debugLevel, DNSMessage message) {
        if (debugLevel >= 2) {
            DNSParser.printMessage(message, debugLevel);
        }
    }
    
    /**
     * 打印查询类型信息
     * @param debugLevel 当前调试级别
     * @param domain 域名
     * @param queryType 查询类型
     */
    public static void printQueryInfo(int debugLevel, String domain, int queryType) {
        if (debugLevel >= 1) {
            String typeStr = getQueryTypeString(queryType);
            System.out.println("Query domain: " + domain + ", type: " + typeStr);
        }
    }
    
    /**
     * 获取查询类型的字符串表示
     * @param queryType 查询类型
     * @return 类型字符串
     */
    private static String getQueryTypeString(int queryType) {
        switch (queryType) {
            case DNSRecord.TYPE_A:
                return "A";
            case DNSRecord.TYPE_AAAA:
                return "AAAA";
            case DNSRecord.TYPE_NS:
                return "NS";
            case DNSRecord.TYPE_CNAME:
                return "CNAME";
            case DNSRecord.TYPE_PTR:
                return "PTR";
            case DNSRecord.TYPE_MX:
                return "MX";
            default:
                return String.valueOf(queryType);
        }
    }
    
    /**
     * 打印网络数据包信息
     * @param debugLevel 当前调试级别
     * @param operation 操作类型（"Received"或"Sent"）
     * @param bytes 字节数
     * @param address 地址
     * @param port 端口
     */
    public static void printPacketInfo(int debugLevel, String operation, int bytes, String address, int port) {
        if (debugLevel >= 2) {
            System.out.printf("%s %d bytes from %s:%d%n", operation, bytes, address, port);
        }
    }
} 