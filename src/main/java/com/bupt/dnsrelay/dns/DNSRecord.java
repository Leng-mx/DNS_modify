package com.bupt.dnsrelay.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * DNS资源记录类
 * 表示DNS报文中的资源记录（Resource Record）
 */
public class DNSRecord {
    
    // DNS记录类型常量
    public static final int TYPE_A = 1;      // IPv4地址记录
    public static final int TYPE_NS = 2;     // 名称服务器记录
    public static final int TYPE_CNAME = 5;  // 别名记录
    public static final int TYPE_PTR = 12;   // 指针记录
    public static final int TYPE_MX = 15;    // 邮件交换记录
    
    // DNS类别常量
    public static final int CLASS_IN = 1;    // Internet类别
    
    private String name;        // 域名
    private int type;           // 记录类型
    private int dnsClass;       // 记录类别
    private long ttl;           // 生存时间
    private byte[] rdata;       // 记录数据
    
    /**
     * 构造函数
     */
    public DNSRecord() {
        this.dnsClass = CLASS_IN;
        this.ttl = 300; // 默认5分钟TTL
    }
    
    /**
     * 构造函数
     * @param name 域名
     * @param type 记录类型
     * @param dnsClass 记录类别
     * @param ttl 生存时间
     * @param rdata 记录数据
     */
    public DNSRecord(String name, int type, int dnsClass, long ttl, byte[] rdata) {
        this.name = name;
        this.type = type;
        this.dnsClass = dnsClass;
        this.ttl = ttl;
        this.rdata = rdata != null ? rdata.clone() : null;
    }
    
    /**
     * 创建A记录
     * @param name 域名
     * @param ipAddress IP地址字符串
     * @return DNS记录对象
     */
    public static DNSRecord createARecord(String name, String ipAddress) {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            byte[] ipBytes = addr.getAddress();
            return new DNSRecord(name, TYPE_A, CLASS_IN, 300, ipBytes);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress, e);
        }
    }
    
    /**
     * 获取A记录的IP地址字符串
     * @return IP地址字符串，如果不是A记录则返回null
     */
    public String getIPAddress() {
        if (type == TYPE_A && rdata != null && rdata.length == 4) {
            return String.format("%d.%d.%d.%d", 
                rdata[0] & 0xFF, rdata[1] & 0xFF, 
                rdata[2] & 0xFF, rdata[3] & 0xFF);
        }
        return null;
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public int getDnsClass() {
        return dnsClass;
    }
    
    public void setDnsClass(int dnsClass) {
        this.dnsClass = dnsClass;
    }
    
    public long getTtl() {
        return ttl;
    }
    
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
    
    public byte[] getRdata() {
        return rdata != null ? rdata.clone() : null;
    }
    
    public void setRdata(byte[] rdata) {
        this.rdata = rdata != null ? rdata.clone() : null;
    }
    
    public int getRdataLength() {
        return rdata != null ? rdata.length : 0;
    }
    
    /**
     * 获取记录类型的字符串表示
     */
    public String getTypeString() {
        switch (type) {
            case TYPE_A: return "A";
            case TYPE_NS: return "NS";
            case TYPE_CNAME: return "CNAME";
            case TYPE_PTR: return "PTR";
            case TYPE_MX: return "MX";
            default: return "TYPE" + type;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ");
        sb.append(ttl).append(" ");
        sb.append("IN ").append(getTypeString()).append(" ");
        
        if (type == TYPE_A) {
            sb.append(getIPAddress());
        } else {
            sb.append("[").append(getRdataLength()).append(" bytes]");
        }
        
        return sb.toString();
    }
}
