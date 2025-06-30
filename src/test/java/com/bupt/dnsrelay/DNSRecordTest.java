package com.bupt.dnsrelay;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import com.bupt.dnsrelay.dns.DNSRecord;

/**
 * DNSRecord测试类
 */
public class DNSRecordTest {
    
    @Test
    public void testCreateARecord() {
        DNSRecord record = DNSRecord.createARecord("test.example.com", "192.168.1.100");
        
        assertEquals("域名应该正确设置", "test.example.com", record.getName());
        assertEquals("类型应该是A记录", DNSRecord.TYPE_A, record.getType());
        assertEquals("类别应该是IN", DNSRecord.CLASS_IN, record.getDnsClass());
        assertEquals("TTL应该是300", 300, record.getTtl());
        assertEquals("数据长度应该是4", 4, record.getRdataLength());
        assertEquals("IP地址应该正确", "192.168.1.100", record.getIPAddress());
    }
    
    @Test
    public void testCreateARecordWithInvalidIP() {
        try {
            DNSRecord.createARecord("test.example.com", "invalid.ip.address");
            fail("应该抛出IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue("错误信息应该包含'Invalid IP address'", 
                       e.getMessage().contains("Invalid IP address"));
        }
    }
    
    @Test
    public void testGetIPAddress() {
        DNSRecord record = DNSRecord.createARecord("test.example.com", "10.0.0.1");
        assertEquals("IP地址应该正确解析", "10.0.0.1", record.getIPAddress());
        
        // 测试边界IP地址
        DNSRecord record2 = DNSRecord.createARecord("test.example.com", "255.255.255.255");
        assertEquals("最大IP地址应该正确解析", "255.255.255.255", record2.getIPAddress());
        
        DNSRecord record3 = DNSRecord.createARecord("test.example.com", "0.0.0.0");
        assertEquals("最小IP地址应该正确解析", "0.0.0.0", record3.getIPAddress());
    }
    
    @Test
    public void testGetIPAddressForNonARecord() {
        DNSRecord record = new DNSRecord();
        record.setType(DNSRecord.TYPE_NS);
        record.setRdata(new byte[]{1, 2, 3, 4});
        
        assertNull("非A记录应该返回null", record.getIPAddress());
    }
    
    @Test
    public void testGetTypeString() {
        DNSRecord record = new DNSRecord();
        
        record.setType(DNSRecord.TYPE_A);
        assertEquals("A记录类型字符串", "A", record.getTypeString());
        
        record.setType(DNSRecord.TYPE_NS);
        assertEquals("NS记录类型字符串", "NS", record.getTypeString());
        
        record.setType(DNSRecord.TYPE_CNAME);
        assertEquals("CNAME记录类型字符串", "CNAME", record.getTypeString());
        
        record.setType(DNSRecord.TYPE_PTR);
        assertEquals("PTR记录类型字符串", "PTR", record.getTypeString());
        
        record.setType(DNSRecord.TYPE_MX);
        assertEquals("MX记录类型字符串", "MX", record.getTypeString());
        
        record.setType(999);
        assertEquals("未知类型应该显示TYPE+数字", "TYPE999", record.getTypeString());
    }
    
    @Test
    public void testSettersAndGetters() {
        DNSRecord record = new DNSRecord();
        
        record.setName("example.com");
        assertEquals("域名设置", "example.com", record.getName());
        
        record.setType(DNSRecord.TYPE_A);
        assertEquals("类型设置", DNSRecord.TYPE_A, record.getType());
        
        record.setDnsClass(DNSRecord.CLASS_IN);
        assertEquals("类别设置", DNSRecord.CLASS_IN, record.getDnsClass());
        
        record.setTtl(3600);
        assertEquals("TTL设置", 3600, record.getTtl());
        
        byte[] testData = {(byte) 192, (byte) 168, 1, 1};
        record.setRdata(testData);
        assertArrayEquals("数据设置", testData, record.getRdata());
        assertEquals("数据长度", 4, record.getRdataLength());
    }
    
    @Test
    public void testRdataCloning() {
        DNSRecord record = new DNSRecord();
        byte[] originalData = {(byte) 192, (byte) 168, 1, 1};
        record.setRdata(originalData);
        
        // 修改原始数组
        originalData[0] = 10;
        
        // 记录中的数据不应该被影响
        byte[] recordData = record.getRdata();
        assertEquals("记录中的数据应该不受原始数组修改影响", 192, recordData[0] & 0xFF);
        
        // 修改从记录获取的数组
        recordData[1] = 10;
        
        // 记录中的数据不应该被影响
        byte[] recordData2 = record.getRdata();
        assertEquals("记录中的数据应该不受返回数组修改影响", 168, recordData2[1] & 0xFF);
    }
    
    @Test
    public void testToString() {
        DNSRecord record = DNSRecord.createARecord("test.example.com", "192.168.1.100");
        String str = record.toString();
        
        assertTrue("toString应该包含域名", str.contains("test.example.com"));
        assertTrue("toString应该包含TTL", str.contains("300"));
        assertTrue("toString应该包含IN", str.contains("IN"));
        assertTrue("toString应该包含A", str.contains("A"));
        assertTrue("toString应该包含IP地址", str.contains("192.168.1.100"));
    }
    
    @Test
    public void testToStringForNonARecord() {
        DNSRecord record = new DNSRecord();
        record.setName("example.com");
        record.setType(DNSRecord.TYPE_NS);
        record.setTtl(3600);
        record.setRdata(new byte[]{1, 2, 3, 4, 5});
        
        String str = record.toString();
        assertTrue("toString应该包含域名", str.contains("example.com"));
        assertTrue("toString应该包含TTL", str.contains("3600"));
        assertTrue("toString应该包含IN", str.contains("IN"));
        assertTrue("toString应该包含NS", str.contains("NS"));
        assertTrue("toString应该包含数据长度", str.contains("5 bytes"));
    }
    
    @Test
    public void testDefaultValues() {
        DNSRecord record = new DNSRecord();
        
        assertEquals("默认类别应该是IN", DNSRecord.CLASS_IN, record.getDnsClass());
        assertEquals("默认TTL应该是300", 300, record.getTtl());
        assertNull("默认域名应该是null", record.getName());
        assertEquals("默认类型应该是0", 0, record.getType());
        assertNull("默认数据应该是null", record.getRdata());
        assertEquals("默认数据长度应该是0", 0, record.getRdataLength());
    }
    
    @Test
    public void testConstructorWithParameters() {
        byte[] testData = {10, 0, 0, 1};
        DNSRecord record = new DNSRecord("test.com", DNSRecord.TYPE_A, DNSRecord.CLASS_IN, 600, testData);
        
        assertEquals("域名应该正确设置", "test.com", record.getName());
        assertEquals("类型应该正确设置", DNSRecord.TYPE_A, record.getType());
        assertEquals("类别应该正确设置", DNSRecord.CLASS_IN, record.getDnsClass());
        assertEquals("TTL应该正确设置", 600, record.getTtl());
        assertArrayEquals("数据应该正确设置", testData, record.getRdata());
        assertEquals("数据长度应该正确", 4, record.getRdataLength());
    }
    
    @Test
    public void testConstructorWithNullData() {
        DNSRecord record = new DNSRecord("test.com", DNSRecord.TYPE_A, DNSRecord.CLASS_IN, 600, null);
        
        assertNull("null数据应该保持null", record.getRdata());
        assertEquals("null数据长度应该是0", 0, record.getRdataLength());
    }
}
