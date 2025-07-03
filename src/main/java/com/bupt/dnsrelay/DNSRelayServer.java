package com.bupt.dnsrelay;

import java.io.IOException;

import com.bupt.dnsrelay.config.CacheManager;
import com.bupt.dnsrelay.config.ConfigParser;
import com.bupt.dnsrelay.dns.DNSMessage;
import com.bupt.dnsrelay.dns.DNSParser;
import com.bupt.dnsrelay.dns.DNSRecord;
import com.bupt.dnsrelay.network.UDPServer;
import com.bupt.dnsrelay.utils.DebugUtils;

/**
 * DNS中继服务器主程序
 */
public class DNSRelayServer {
    
    private static final String DEFAULT_UPSTREAM_DNS = "10.3.9.4";
    private static final String DEFAULT_CONFIG_FILE = "config\\dnsrelay.txt";
    
    private ConfigParser configParser = new ConfigParser();
    private UDPServer udpServer;
    private String upstreamDNS;
    private int debugLevel;
    private volatile boolean isRunning = false;
    private CacheManager cacheManager;
    
    /**
     * 构造函数
     * @param upstreamDNS 上游DNS服务器地址
     * @param configFile 配置文件路径
     * @param debugLevel 调试级别
     */
    public DNSRelayServer(String upstreamDNS, String configFile, int debugLevel) {
        this.upstreamDNS = upstreamDNS;
        this.debugLevel = debugLevel;
        this.udpServer = new UDPServer(debugLevel);
        try {
            configParser.loadConfig(configFile);
            DebugUtils.debug(debugLevel, "Configuration loaded successfully");
            if (debugLevel >= 1) configParser.printConfig();
            // 初始化CacheManager
            this.cacheManager = new CacheManager("config/cache.txt");
            DebugUtils.debugf(debugLevel, "[CACHE] Loaded %d entries from cache.txt%n", cacheManager.size());
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * 启动DNS中继服务器
     */
    public void start() {
        try {
            udpServer.start();
            isRunning = true;
            System.out.println("DNS Relay Server started successfully\nDebug level: " + debugLevel +
                "\nUpstream DNS: " + upstreamDNS +
                "\nConfiguration entries: " + configParser.getEntryCount() +
                "\nWaiting for DNS queries...\n");
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            runServerLoop();
        } catch (IOException e) {
            System.err.println("Failed to start DNS Relay Server: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * 停止DNS中继服务器
     */
    public void stop() {
        if (isRunning) {
            udpServer.stop();
            isRunning = false;
            System.out.println("DNS Relay Server stopped");
        }
    }
    
    /**
     * 主服务循环
     */
    private void runServerLoop() {
        while (isRunning) {
            try {
                UDPServer.DNSPacket packet = udpServer.receiveQuery();
                if (packet == null) continue;
                
                DebugUtils.debugf(debugLevel, "\n--- Received DNS Query from %s ---\n", packet.getClientInfo());
                
                byte[] responseData = handleDNSQuery(packet.getData());
                if (responseData != null) {
                    udpServer.sendResponse(responseData, packet.getClientAddress(), packet.getClientPort());
                    DebugUtils.debugf(debugLevel, "Response sent to client (%d bytes)\n", responseData.length);
                } else {
                    System.err.println("Error: Failed to generate response");
                }
                
                DebugUtils.debug(debugLevel, "----------------------------------------\n");
            } catch (IOException e) {
                if (isRunning) System.err.println("Error in server loop: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理DNS查询的核心逻辑
     * @param queryData 查询数据
     * @return 响应数据
     */
    private byte[] handleDNSQuery(byte[] queryData) {
        try {
            // 解析DNS查询消息（只解析一次）
            DNSMessage queryMessage = DNSParser.parseMessage(queryData);
            if (queryMessage.getQuestions().isEmpty()) {
                System.err.println("Error: No questions in DNS query");
                return null;
            }
            
            String domain = queryMessage.getQuestions().get(0).getName().toLowerCase();
            int queryType = queryMessage.getQuestions().get(0).getType();
            
            DebugUtils.printQueryInfo(debugLevel, domain, queryType);
            DebugUtils.printDNSMessage(debugLevel, queryMessage);
            
            // 1. 检查域名是否被拦截
            if (configParser.isDomainBlocked(domain)) {
                System.out.printf("[BLOCKED] %s -> NXDOMAIN\n", domain);
                return createErrorResponse(queryMessage, DNSMessage.RCODE_NXDOMAIN);
            }
            
            // 2. 检查本地解析（仅A记录）
            if (queryType == DNSRecord.TYPE_A) {
                String localIP = configParser.lookupDomain(domain);
                if (localIP != null && !"0.0.0.0".equals(localIP)) {
                    System.out.printf("[LOCAL] %s -> %s\n", domain, localIP);
                    return createLocalResponse(queryMessage, localIP);
                }
            }
            
            // 3. 检查缓存
            String cacheIP = cacheManager.lookup(domain + ":" + queryType);
            if (cacheIP != null) {
                System.out.printf("[CACHE] %s -> %s\n", domain, cacheIP);
                if (queryType == DNSRecord.TYPE_A) {
                    return createLocalResponse(queryMessage, cacheIP);
                } else if (queryType == DNSRecord.TYPE_AAAA) {
                    // 这里只返回原始响应，或可自定义IPv6响应
                    // 暂时直接转发上游响应
                }
            }
            
            // 4. 转发到上游DNS服务器
            System.out.printf("[UPSTREAM] %s -> querying upstream DNS\n", domain);
            byte[] upstreamResponse = udpServer.forwardQuery(queryData, upstreamDNS);
            if (upstreamResponse != null) {
                String upstreamIP = extractIPFromResponse(upstreamResponse, queryType);
                if (upstreamIP != null) {
                    cacheManager.put(domain + ":" + queryType, upstreamIP);
                    System.out.printf("[UPSTREAM] %s -> %s (cached)\n", domain, upstreamIP);
                } 
                return upstreamResponse;
            } else {
                System.out.printf("[UPSTREAM] %s -> query failed\n", domain);
                return createErrorResponse(queryMessage, DNSMessage.RCODE_SERVFAIL);
            }
        } catch (Exception e) {
            System.err.println("Error handling DNS query: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建错误响应
     * @param queryMessage 原始查询消息
     * @param rcode 错误码
     * @return 响应数据
     */
    private byte[] createErrorResponse(DNSMessage queryMessage, int rcode) {
        try {
            DNSMessage response = DNSMessage.createErrorResponse(queryMessage, rcode);
            byte[] responseData = DNSParser.buildMessage(response);
            
            DebugUtils.printDNSMessage(debugLevel, response);
            
            return responseData;
        } catch (Exception e) {
            System.err.println("Error creating error response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建本地解析响应
     * @param queryMessage 原始查询消息
     * @param ipAddress IP地址
     * @return 响应数据
     */
    private byte[] createLocalResponse(DNSMessage queryMessage, String ipAddress) {
        try {
            DNSMessage response = DNSMessage.createLocalResponse(queryMessage, ipAddress);
            byte[] responseData = DNSParser.buildMessage(response);
            
            DebugUtils.printDNSMessage(debugLevel, response);
            
            return responseData;
        } catch (Exception e) {
            System.err.println("Error creating local response: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从DNS响应中提取IP地址
     * @param response 响应数据
     * @param queryType 查询类型
     * @return IP地址字符串
     */
    private String extractIPFromResponse(byte[] response, int queryType) {
        try {
            DNSMessage msg = DNSParser.parseMessage(response);
            if (msg.getAnswers() != null) {
                for (DNSRecord rec : msg.getAnswers()) {
                    if (queryType == DNSRecord.TYPE_A && rec.getType() == DNSRecord.TYPE_A) {
                        return rec.getIPAddress();
                    } else if (queryType == DNSRecord.TYPE_AAAA && rec.getType() == DNSRecord.TYPE_AAAA) {
                        return rec.getIPv6Address();
                    }
                }
            }
        } catch (Exception e) {
            // 兼容处理：直接从响应包末尾尝试提取IPv4
            if (queryType == DNSRecord.TYPE_A && response.length > 16) {
                int ipStart = response.length - 4;
                int b1 = response[ipStart] & 0xFF;
                int b2 = response[ipStart + 1] & 0xFF;
                int b3 = response[ipStart + 2] & 0xFF;
                int b4 = response[ipStart + 3] & 0xFF;
                if (b1 > 0 && b1 < 255) {
                    return b1 + "." + b2 + "." + b3 + "." + b4;
                }
            }
        }
        return null;
    }

    /**
     * 打印使用说明
     * @param programName 程序名称
     */
    private static void printUsage(String programName) {
        System.out.println("DNS Relay Server - BUPT Network Communication Course Project");
        System.out.println("Usage: java " + programName + " [-d | -dd] [dns-server-ipaddr] [filename]");
        System.out.println("Options:");
        System.out.println("  -d          Enable debug mode (level 1)");
        System.out.println("  -dd         Enable verbose debug mode (level 2)");
        System.out.println("  dns-server  Upstream DNS server IP address (default: " + DEFAULT_UPSTREAM_DNS + ")");
        System.out.println("  filename    Configuration file path (default: " + DEFAULT_CONFIG_FILE + ")");
        System.out.println("\nExamples:");
        System.out.println("  java " + programName);
        System.out.println("  java " + programName + " -d 10.3.9.44");
        System.out.println("  java " + programName + " -dd 8.8.8.8 config/dnsrelay.txt");
    }

    /**
     * 主函数
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("=== DNS Relay Server ===");
        System.out.println("BUPT-QMUL Network Communication Course Project");
        System.out.println("Version 1.0\n");

        // 解析命令行参数
        int debugLevel = 0;
        String upstreamDNS = DEFAULT_UPSTREAM_DNS;
        String configFile = DEFAULT_CONFIG_FILE;

        int argIndex = 0;

        // 解析调试选项
        while (argIndex < args.length && args[argIndex].startsWith("-")) {
            if ("-d".equals(args[argIndex])) {
                debugLevel = 1;
            } else if ("-dd".equals(args[argIndex])) {
                debugLevel = 2;
            } else if ("-h".equals(args[argIndex]) || "--help".equals(args[argIndex])) {
                printUsage("DNSRelayServer");
                return;
            } else {
                System.err.println("Unknown option: " + args[argIndex]);
                printUsage("DNSRelayServer");
                System.exit(1);
            }
            argIndex++;
        }

        // 解析DNS服务器地址
        if (argIndex < args.length) {
            if (ConfigParser.isValidIP(args[argIndex])) {
                upstreamDNS = args[argIndex];
                argIndex++;
            } else {
                System.err.println("Invalid DNS server IP address: " + args[argIndex]);
                System.exit(1);
            }
        }

        // 解析配置文件路径
        if (argIndex < args.length) {
            configFile = args[argIndex];
            argIndex++;
        }

        // 检查多余参数
        if (argIndex < args.length) {
            System.err.println("Too many arguments");
            printUsage("DNSRelayServer");
            System.exit(1);
        }

        // 创建并启动DNS中继服务器
        DNSRelayServer server = new DNSRelayServer(upstreamDNS, configFile, debugLevel);
        server.start();
    }
}
