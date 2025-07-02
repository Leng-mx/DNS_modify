package com.bupt.dnsrelay.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DNS配置文件解析器
 * 负责读取和解析dnsrelay.txt配置文件
 */
public class ConfigParser {
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    
    private Map<String, String> domainMap;  // 域名到IP的映射
    private Set<String> blockedDomains;     // 被拦截的域名集合
    
    /**
     * 构造函数
     */ 
    public ConfigParser() {
        this.domainMap = new HashMap<>();
        this.blockedDomains = new HashSet<>();
    }
    
    /**
     * 加载配置文件
     * @param filename 配置文件路径
     * @return 加载的条目数量
     * @throws IOException 文件读取错误
     */
    public int loadConfig(String filename) throws IOException {
        domainMap.clear();
        blockedDomains.clear();
        
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Warning: Configuration file not found: " + filename);
            return 0;
        }
        
        System.out.println("Loading DNS configuration from: " + filename);
        
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 解析配置行
                if (parseLine(line, lineNumber)) {
                    count++;
                }
            }
        }
        
        System.out.println("Loaded " + count + " domain entries from configuration file");
        return count;
    }
    
    /**
     * 解析配置文件的一行
     * @param line 配置行
     * @param lineNumber 行号
     * @return 是否解析成功
     */
    private boolean parseLine(String line, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            System.out.println("Warning: Invalid line format at line " + lineNumber + ": " + line);
            return false;
        }
        
        String ip = parts[0];
        String domain = parts[1].toLowerCase(); // 域名不区分大小写
        
        // 验证IP地址格式
        if (!isValidIP(ip)) {
            System.out.println("Warning: Invalid IP address at line " + lineNumber + ": " + ip);
            return false;
        }
        
        // 验证域名格式
        if (!isValidDomain(domain)) {
            System.out.println("Warning: Invalid domain name at line " + lineNumber + ": " + domain);
            return false;
        }
        
        // 添加到映射中
        domainMap.put(domain, ip);
        
        // 检查是否为拦截条目
        if ("0.0.0.0".equals(ip)) {
            blockedDomains.add(domain);
            System.out.println("Loaded: " + domain + " -> " + ip + " (BLOCKED)");
        } else {
            System.out.println("Loaded: " + domain + " -> " + ip);
        }
        
        return true;
    }
    
    /**
     * 验证IP地址格式
     * @param ip IP地址字符串
     * @return 是否有效
     */
    private boolean isValidIP(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * 验证域名格式
     * @param domain 域名字符串
     * @return 是否有效
     */
    private boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty() || domain.length() > 255) {
            return false;
        }
        
        // 简单的域名格式检查
        String[] labels = domain.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) {
                return false;
            }
            // 检查标签是否只包含字母、数字和连字符
            if (!label.matches("^[a-zA-Z0-9-]+$")) {
                return false;
            }
            // 标签不能以连字符开始或结束
            if (label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 查找域名对应的IP地址
     * @param domain 域名
     * @return IP地址，如果未找到则返回null
     */
    public String lookupDomain(String domain) {
        if (domain == null) return null;
        return domainMap.get(domain.toLowerCase());
    }
    
    /**
     * 检查域名是否被拦截
     * @param domain 域名
     * @return 是否被拦截
     */
    public boolean isDomainBlocked(String domain) {
        if (domain == null) return false;
        return blockedDomains.contains(domain.toLowerCase());
    }
    
    /**
     * 检查域名是否在本地配置中
     * @param domain 域名
     * @return 是否在本地配置中
     */
    public boolean isLocalDomain(String domain) {
        if (domain == null) return false;
        return domainMap.containsKey(domain.toLowerCase());
    }
    
    /**
     * 获取所有配置的域名
     * @return 域名集合
     */
    public Set<String> getAllDomains() {
        return new HashSet<>(domainMap.keySet());
    }
    
    /**
     * 获取所有被拦截的域名
     * @return 被拦截的域名集合
     */
    public Set<String> getBlockedDomains() {
        return new HashSet<>(blockedDomains);
    }
    
    /**
     * 获取配置条目数量
     * @return 条目数量
     */
    public int getEntryCount() {
        return domainMap.size();
    }
    
    /**
     * 打印配置信息
     */
    public void printConfig() {
        System.out.println("\n=== DNS Configuration ===");
        System.out.println("Total entries: " + domainMap.size());
        System.out.println("Blocked domains: " + blockedDomains.size());
        
        List<String> sortedDomains = new ArrayList<>(domainMap.keySet());
        Collections.sort(sortedDomains);
        
        int count = 1;
        for (String domain : sortedDomains) {
            String ip = domainMap.get(domain);
            boolean blocked = blockedDomains.contains(domain);
            System.out.printf("%3d. %-30s -> %-15s %s%n", 
                count++, domain, ip, blocked ? "(BLOCKED)" : "");
        }
        
        System.out.println("========================\n");
    }
    
    /**
     * 清空配置
     */
    public void clear() {
        domainMap.clear();
        blockedDomains.clear();
    }
}
