package com.bupt.dnsrelay;

import com.bupt.dnsrelay.config.ConfigParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * ConfigParser测试类
 */
public class ConfigParserTest {
    
    private ConfigParser configParser;
    private File testConfigFile;
    
    @Before
    public void setUp() throws IOException {
        configParser = new ConfigParser();
        
        // 创建测试配置文件
        testConfigFile = File.createTempFile("test_dns_config", ".txt");
        try (FileWriter writer = new FileWriter(testConfigFile)) {
            writer.write("# 测试配置文件\n");
            writer.write("0.0.0.0 blocked.test.com\n");
            writer.write("192.168.1.100 local.test.com\n");
            writer.write("127.0.0.1 localhost.test.com\n");
            writer.write("# 注释行\n");
            writer.write("10.0.0.1 server.test.com\n");
            writer.write("\n"); // 空行
            writer.write("0.0.0.0 ads.example.com\n");
        }
    }
    
    @After
    public void tearDown() {
        if (testConfigFile != null && testConfigFile.exists()) {
            testConfigFile.delete();
        }
    }
    
    @Test
    public void testLoadConfig() throws IOException {
        int count = configParser.loadConfig(testConfigFile.getAbsolutePath());
        assertEquals("应该加载5个有效配置条目", 5, count);
        assertEquals("总条目数应该为5", 5, configParser.getEntryCount());
    }
    
    @Test
    public void testDomainBlocking() throws IOException {
        configParser.loadConfig(testConfigFile.getAbsolutePath());
        
        assertTrue("blocked.test.com应该被拦截", 
                   configParser.isDomainBlocked("blocked.test.com"));
        assertTrue("ads.example.com应该被拦截", 
                   configParser.isDomainBlocked("ads.example.com"));
        assertFalse("local.test.com不应该被拦截", 
                    configParser.isDomainBlocked("local.test.com"));
        assertFalse("不存在的域名不应该被拦截", 
                    configParser.isDomainBlocked("nonexistent.com"));
    }
    
    @Test
    public void testLocalResolution() throws IOException {
        configParser.loadConfig(testConfigFile.getAbsolutePath());
        
        assertEquals("local.test.com应该解析到192.168.1.100", 
                     "192.168.1.100", configParser.lookupDomain("local.test.com"));
        assertEquals("localhost.test.com应该解析到127.0.0.1", 
                     "127.0.0.1", configParser.lookupDomain("localhost.test.com"));
        assertEquals("server.test.com应该解析到10.0.0.1", 
                     "10.0.0.1", configParser.lookupDomain("server.test.com"));
        assertNull("不存在的域名应该返回null", 
                   configParser.lookupDomain("nonexistent.com"));
    }
    
    @Test
    public void testCaseInsensitive() throws IOException {
        configParser.loadConfig(testConfigFile.getAbsolutePath());
        
        // 测试大小写不敏感
        assertTrue("大写域名应该被识别", 
                   configParser.isDomainBlocked("BLOCKED.TEST.COM"));
        assertEquals("大写域名应该能正确解析", 
                     "192.168.1.100", configParser.lookupDomain("LOCAL.TEST.COM"));
        assertTrue("混合大小写域名应该被识别", 
                   configParser.isDomainBlocked("Blocked.Test.Com"));
    }
    
    @Test
    public void testIsLocalDomain() throws IOException {
        configParser.loadConfig(testConfigFile.getAbsolutePath());
        
        assertTrue("blocked.test.com应该在本地配置中", 
                   configParser.isLocalDomain("blocked.test.com"));
        assertTrue("local.test.com应该在本地配置中", 
                   configParser.isLocalDomain("local.test.com"));
        assertFalse("不存在的域名不应该在本地配置中", 
                    configParser.isLocalDomain("nonexistent.com"));
    }
    
    @Test
    public void testEmptyConfigFile() throws IOException {
        // 创建空配置文件
        File emptyFile = File.createTempFile("empty_config", ".txt");
        try {
            int count = configParser.loadConfig(emptyFile.getAbsolutePath());
            assertEquals("空配置文件应该返回0个条目", 0, count);
            assertEquals("总条目数应该为0", 0, configParser.getEntryCount());
        } finally {
            emptyFile.delete();
        }
    }
    
    @Test
    public void testNonExistentConfigFile() throws IOException {
        int count = configParser.loadConfig("nonexistent_file.txt");
        assertEquals("不存在的配置文件应该返回0个条目", 0, count);
        assertEquals("总条目数应该为0", 0, configParser.getEntryCount());
    }
    
    @Test
    public void testInvalidConfigLines() throws IOException {
        // 创建包含无效行的配置文件
        File invalidFile = File.createTempFile("invalid_config", ".txt");
        try (FileWriter writer = new FileWriter(invalidFile)) {
            writer.write("192.168.1.100 valid.test.com\n");
            writer.write("invalid_ip domain.com\n"); // 无效IP
            writer.write("192.168.1.101\n"); // 缺少域名
            writer.write("192.168.1.102 valid2.test.com\n");
        }
        
        try {
            int count = configParser.loadConfig(invalidFile.getAbsolutePath());
            assertEquals("应该只加载2个有效条目", 2, count);
            
            assertEquals("valid.test.com应该能正确解析", 
                         "192.168.1.100", configParser.lookupDomain("valid.test.com"));
            assertEquals("valid2.test.com应该能正确解析", 
                         "192.168.1.102", configParser.lookupDomain("valid2.test.com"));
            assertNull("无效行的域名不应该被加载", 
                       configParser.lookupDomain("domain.com"));
        } finally {
            invalidFile.delete();
        }
    }
    
    @Test
    public void testGetAllDomains() throws IOException {
        configParser.loadConfig(testConfigFile.getAbsolutePath());
        
        assertEquals("应该有5个域名", 5, configParser.getAllDomains().size());
        assertTrue("应该包含blocked.test.com", 
                   configParser.getAllDomains().contains("blocked.test.com"));
        assertTrue("应该包含local.test.com", 
                   configParser.getAllDomains().contains("local.test.com"));
    }
    
    @Test
    public void testGetBlockedDomains() throws IOException {
        configParser.loadConfig(testConfigFile.getAbsolutePath());
        
        assertEquals("应该有2个被拦截的域名", 2, configParser.getBlockedDomains().size());
        assertTrue("应该包含blocked.test.com", 
                   configParser.getBlockedDomains().contains("blocked.test.com"));
        assertTrue("应该包含ads.example.com", 
                   configParser.getBlockedDomains().contains("ads.example.com"));
        assertFalse("不应该包含local.test.com", 
                    configParser.getBlockedDomains().contains("local.test.com"));
    }
}
