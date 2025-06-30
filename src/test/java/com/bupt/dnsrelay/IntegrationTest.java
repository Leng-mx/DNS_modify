package com.bupt.dnsrelay;

import com.bupt.dnsrelay.config.ConfigParser;
import com.bupt.dnsrelay.dns.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * 集成测试类
 * 测试各个模块之间的协作
 */
public class IntegrationTest {
    
    private ConfigParser configParser;
    private File testConfigFile;
    
    @Before
    public void setUp() throws IOException {
        configParser = new ConfigParser();
        
        // 创建测试配置文件
        testConfigFile = File.createTempFile("integration_test_config", ".txt");
        try (FileWriter writer = new FileWriter(testConfigFile)) {
            writer.write("# 集成测试配置文件\n");
            writer.write("0.0.0.0 blocked.test.com\n");
            writer.write("0.0.0.0 ads.malware.com\n");
            writer.write("192.168.1.100 local.test.com\n");
            writer.write("127.0.0.1 localhost.test.com\n");
            writer.write("10.0.0.1 server.internal.com\n");
        }
        
        configParser.loadConfig(testConfigFile.getAbsolutePath());
    }
    
    @After
    public void tearDown() {
        if (testConfigFile != null && testConfigFile.exists()) {
            testConfigFile.delete();
        }
    }
    
    @Test
    public void testDomainBlockingWorkflow() {
        // 模拟DNS查询处理流程 - 域名拦截场景
        String queryDomain = "blocked.test.com";
        
        // 1. 检查域名是否被拦截
        assertTrue("域名应该被拦截", configParser.isDomainBlocked(queryDomain));
        
        // 2. 创建原始查询
        DNSMessage query = DNSMessage.createQuery(12345, queryDomain, DNSRecord.TYPE_A);
        assertFalse("原始查询应该不是响应", query.isResponse());
        assertEquals("查询域名应该正确", queryDomain, query.getQuestions().get(0).getName());
        
        // 3. 创建拦截响应
        DNSMessage response = DNSMessage.createErrorResponse(query, DNSMessage.RCODE_NXDOMAIN);
        assertTrue("响应应该是响应类型", response.isResponse());
        assertTrue("响应应该是权威答案", response.isAuthoritative());
        assertEquals("响应码应该是NXDOMAIN", DNSMessage.RCODE_NXDOMAIN, response.getRcode());
        assertEquals("响应ID应该与查询相同", query.getId(), response.getId());
        assertTrue("响应不应该有答案", response.getAnswers().isEmpty());
        
        // 4. 验证响应包含原始问题
        assertEquals("响应应该包含原始问题", 1, response.getQuestions().size());
        assertEquals("问题域名应该相同", queryDomain, response.getQuestions().get(0).getName());
    }
    
    @Test
    public void testLocalResolutionWorkflow() {
        // 模拟DNS查询处理流程 - 本地解析场景
        String queryDomain = "local.test.com";
        String expectedIP = "192.168.1.100";
        
        // 1. 检查域名是否在本地配置中
        assertTrue("域名应该在本地配置中", configParser.isLocalDomain(queryDomain));
        assertFalse("域名不应该被拦截", configParser.isDomainBlocked(queryDomain));
        
        // 2. 获取本地IP
        String localIP = configParser.lookupDomain(queryDomain);
        assertEquals("本地IP应该正确", expectedIP, localIP);
        
        // 3. 创建原始查询
        DNSMessage query = DNSMessage.createQuery(54321, queryDomain, DNSRecord.TYPE_A);
        
        // 4. 创建本地解析响应
        DNSMessage response = DNSMessage.createLocalResponse(query, localIP);
        assertTrue("响应应该是响应类型", response.isResponse());
        assertTrue("响应应该是权威答案", response.isAuthoritative());
        assertEquals("响应码应该是NOERROR", DNSMessage.RCODE_NOERROR, response.getRcode());
        assertEquals("响应ID应该与查询相同", query.getId(), response.getId());
        
        // 5. 验证答案记录
        assertEquals("应该有一个答案", 1, response.getAnswers().size());
        DNSRecord answer = response.getAnswers().get(0);
        assertEquals("答案域名应该正确", queryDomain, answer.getName());
        assertEquals("答案类型应该是A", DNSRecord.TYPE_A, answer.getType());
        assertEquals("答案IP应该正确", expectedIP, answer.getIPAddress());
    }
    
    @Test
    public void testRelayForwardingWorkflow() {
        // 模拟DNS查询处理流程 - 中继转发场景
        String queryDomain = "www.example.com";
        
        // 1. 检查域名不在本地配置中
        assertFalse("域名不应该在本地配置中", configParser.isLocalDomain(queryDomain));
        assertFalse("域名不应该被拦截", configParser.isDomainBlocked(queryDomain));
        assertNull("本地查找应该返回null", configParser.lookupDomain(queryDomain));
        
        // 2. 创建原始查询
        DNSMessage query = DNSMessage.createQuery(98765, queryDomain, DNSRecord.TYPE_A);
        
        // 3. 模拟上游服务器响应（在实际场景中，这会通过网络获取）
        DNSMessage upstreamResponse = new DNSMessage(query.getId());
        upstreamResponse.setResponse(true);
        upstreamResponse.setRecursionAvailable(true);
        upstreamResponse.setRcode(DNSMessage.RCODE_NOERROR);
        
        // 复制问题
        for (DNSQuestion question : query.getQuestions()) {
            upstreamResponse.addQuestion(question);
        }
        
        // 添加模拟答案
        DNSRecord answer = DNSRecord.createARecord(queryDomain, "93.184.216.34");
        upstreamResponse.addAnswer(answer);
        
        // 4. 验证转发响应
        assertEquals("响应ID应该与查询相同", query.getId(), upstreamResponse.getId());
        assertTrue("响应应该是响应类型", upstreamResponse.isResponse());
        assertEquals("应该有一个答案", 1, upstreamResponse.getAnswers().size());
        assertEquals("答案域名应该正确", queryDomain, upstreamResponse.getAnswers().get(0).getName());
    }
    
    @Test
    public void testMultipleDomainTypes() {
        // 测试配置文件中的多种域名类型
        
        // 拦截域名
        assertTrue("blocked.test.com应该被拦截", configParser.isDomainBlocked("blocked.test.com"));
        assertTrue("ads.malware.com应该被拦截", configParser.isDomainBlocked("ads.malware.com"));
        
        // 本地解析域名
        assertEquals("local.test.com应该解析到192.168.1.100", 
                     "192.168.1.100", configParser.lookupDomain("local.test.com"));
        assertEquals("localhost.test.com应该解析到127.0.0.1", 
                     "127.0.0.1", configParser.lookupDomain("localhost.test.com"));
        assertEquals("server.internal.com应该解析到10.0.0.1", 
                     "10.0.0.1", configParser.lookupDomain("server.internal.com"));
        
        // 不存在的域名
        assertFalse("unknown.domain.com不应该在本地配置中", 
                    configParser.isLocalDomain("unknown.domain.com"));
        assertFalse("unknown.domain.com不应该被拦截", 
                    configParser.isDomainBlocked("unknown.domain.com"));
        assertNull("unknown.domain.com应该返回null", 
                   configParser.lookupDomain("unknown.domain.com"));
    }
    
    @Test
    public void testDNSMessageAndRecordIntegration() {
        // 测试DNS消息和记录类的集成
        
        // 创建复杂的DNS消息
        DNSMessage message = new DNSMessage(12345);
        message.setResponse(true);
        message.setAuthoritative(true);
        message.setRecursionDesired(true);
        message.setRecursionAvailable(true);
        
        // 添加问题
        DNSQuestion question = DNSQuestion.createAQuery("test.example.com");
        message.addQuestion(question);
        
        // 添加多个答案
        message.addAnswer(DNSRecord.createARecord("test.example.com", "192.168.1.1"));
        message.addAnswer(DNSRecord.createARecord("test.example.com", "192.168.1.2"));
        
        // 添加权威记录
        message.addAuthority(DNSRecord.createARecord("ns1.example.com", "192.168.1.10"));
        
        // 添加附加记录
        message.addAdditional(DNSRecord.createARecord("mail.example.com", "192.168.1.20"));
        
        // 验证消息结构
        assertEquals("应该有1个问题", 1, message.getQuestions().size());
        assertEquals("应该有2个答案", 2, message.getAnswers().size());
        assertEquals("应该有1个权威记录", 1, message.getAuthorities().size());
        assertEquals("应该有1个附加记录", 1, message.getAdditionals().size());
        
        // 验证各部分内容
        assertEquals("问题域名应该正确", "test.example.com", message.getQuestions().get(0).getName());
        assertEquals("第一个答案IP应该正确", "192.168.1.1", message.getAnswers().get(0).getIPAddress());
        assertEquals("第二个答案IP应该正确", "192.168.1.2", message.getAnswers().get(1).getIPAddress());
        assertEquals("权威记录IP应该正确", "192.168.1.10", message.getAuthorities().get(0).getIPAddress());
        assertEquals("附加记录IP应该正确", "192.168.1.20", message.getAdditionals().get(0).getIPAddress());
    }
    
    @Test
    public void testCaseInsensitiveIntegration() {
        // 测试大小写不敏感的集成场景
        
        String[] testDomains = {
            "BLOCKED.TEST.COM",
            "blocked.TEST.com",
            "Blocked.Test.Com",
            "LOCAL.TEST.COM",
            "local.TEST.com",
            "Local.Test.Com"
        };
        
        for (String domain : testDomains) {
            if (domain.toLowerCase().contains("blocked")) {
                assertTrue("大小写变体应该被识别为拦截域名: " + domain, 
                           configParser.isDomainBlocked(domain));
                
                // 创建拦截响应
                DNSMessage query = DNSMessage.createQuery(12345, domain, DNSRecord.TYPE_A);
                DNSMessage response = DNSMessage.createErrorResponse(query, DNSMessage.RCODE_NXDOMAIN);
                assertEquals("响应码应该是NXDOMAIN", DNSMessage.RCODE_NXDOMAIN, response.getRcode());
                
            } else if (domain.toLowerCase().contains("local")) {
                assertTrue("大小写变体应该被识别为本地域名: " + domain, 
                           configParser.isLocalDomain(domain));
                assertEquals("大小写变体应该正确解析: " + domain, 
                             "192.168.1.100", configParser.lookupDomain(domain));
                
                // 创建本地解析响应
                DNSMessage query = DNSMessage.createQuery(12345, domain, DNSRecord.TYPE_A);
                DNSMessage response = DNSMessage.createLocalResponse(query, "192.168.1.100");
                assertEquals("响应码应该是NOERROR", DNSMessage.RCODE_NOERROR, response.getRcode());
                assertEquals("应该有一个答案", 1, response.getAnswers().size());
            }
        }
    }
}
