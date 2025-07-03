package com.bupt.dnsrelay.dns;

/**
 * DNS问题类
 * 表示DNS报文中的问题部分（Question Section）
 */
public class DNSQuestion {
    
    private String name;      // 查询域名
    private int type;         // 查询类型
    private int dnsClass;     // 查询类别
    
    /**
     * 构造函数
     */
    public DNSQuestion() {
        this.dnsClass = DNSRecord.CLASS_IN; // 默认为Internet类别
    }
    
    /**
     * 构造函数
     * @param name 查询域名
     * @param type 查询类型
     * @param dnsClass 查询类别
     */
    public DNSQuestion(String name, int type, int dnsClass) {
        this.name = name;
        this.type = type;
        this.dnsClass = dnsClass;
    }
    
    /**
     * 创建A记录查询
     * @param domain 域名
     * @return DNS问题对象
     */
    public static DNSQuestion createAQuery(String domain) {
        return new DNSQuestion(domain, DNSRecord.TYPE_A, DNSRecord.CLASS_IN);
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
    
    /**
     * 获取查询类型的字符串表示
     */
    public String getTypeString() {
        switch (type) {
            case DNSRecord.TYPE_A: return "A";
            case DNSRecord.TYPE_AAAA: return "AAAA";
            case DNSRecord.TYPE_NS: return "NS";
            case DNSRecord.TYPE_CNAME: return "CNAME";
            case DNSRecord.TYPE_PTR: return "PTR";
            case DNSRecord.TYPE_MX: return "MX";
            default: return "TYPE" + type;
        }
    }
    
    @Override
    public String toString() {
        return name + " IN " + getTypeString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DNSQuestion that = (DNSQuestion) obj;
        return type == that.type && 
               dnsClass == that.dnsClass && 
               (name != null ? name.equals(that.name) : that.name == null);
    }
    
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + type;
        result = 31 * result + dnsClass;
        return result;
    }
}
