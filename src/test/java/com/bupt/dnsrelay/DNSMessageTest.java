package com.bupt.dnsrelay;

import com.bupt.dnsrelay.dns.*;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * DNSMessage测试类
 */
public class DNSMessageTest {
    
    @Test
    public void testDefaultConstructor() {
        DNSMessage message = new DNSMessage();
        
        assertEquals("默认ID应该是0", 0, message.getId());
        assertFalse("默认应该不是响应", message.isResponse());
        assertEquals("默认操作码应该是0", 0, message.getOpcode());
        assertFalse("默认不是权威答案", message.isAuthoritative());
        assertFalse("默认不截断", message.isTruncated());
        assertFalse("默认不期望递归", message.isRecursionDesired());
        assertFalse("默认递归不可用", message.isRecursionAvailable());
        assertEquals("默认响应码应该是NOERROR", DNSMessage.RCODE_NOERROR, message.getRcode());
        
        assertTrue("问题列表应该为空", message.getQuestions().isEmpty());
        assertTrue("答案列表应该为空", message.getAnswers().isEmpty());
        assertTrue("权威列表应该为空", message.getAuthorities().isEmpty());
        assertTrue("附加列表应该为空", message.getAdditionals().isEmpty());
    }
    
    @Test
    public void testConstructorWithId() {
        DNSMessage message = new DNSMessage(12345);
        assertEquals("ID应该正确设置", 12345, message.getId());
    }
    
    @Test
    public void testCreateQuery() {
        DNSMessage query = DNSMessage.createQuery(54321, "example.com", DNSRecord.TYPE_A);
        
        assertEquals("查询ID应该正确", 54321, query.getId());
        assertFalse("应该是查询而不是响应", query.isResponse());
        assertTrue("应该期望递归", query.isRecursionDesired());
        assertEquals("应该有一个问题", 1, query.getQuestions().size());
        
        DNSQuestion question = query.getQuestions().get(0);
        assertEquals("问题域名应该正确", "example.com", question.getName());
        assertEquals("问题类型应该是A", DNSRecord.TYPE_A, question.getType());
        assertEquals("问题类别应该是IN", DNSRecord.CLASS_IN, question.getDnsClass());
    }
    
    @Test
    public void testCreateErrorResponse() {
        DNSMessage query = DNSMessage.createQuery(12345, "blocked.example.com", DNSRecord.TYPE_A);
        DNSMessage response = DNSMessage.createErrorResponse(query, DNSMessage.RCODE_NXDOMAIN);
        
        assertEquals("响应ID应该与查询相同", 12345, response.getId());
        assertTrue("应该是响应", response.isResponse());
        assertTrue("应该是权威答案", response.isAuthoritative());
        assertEquals("响应码应该是NXDOMAIN", DNSMessage.RCODE_NXDOMAIN, response.getRcode());
        assertEquals("应该复制查询的递归标志", query.isRecursionDesired(), response.isRecursionDesired());
        assertEquals("应该有一个问题", 1, response.getQuestions().size());
        assertEquals("问题应该与原查询相同", "blocked.example.com", response.getQuestions().get(0).getName());
        assertTrue("答案列表应该为空", response.getAnswers().isEmpty());
    }
    
    @Test
    public void testCreateLocalResponse() {
        DNSMessage query = DNSMessage.createQuery(12345, "local.example.com", DNSRecord.TYPE_A);
        DNSMessage response = DNSMessage.createLocalResponse(query, "192.168.1.100");
        
        assertEquals("响应ID应该与查询相同", 12345, response.getId());
        assertTrue("应该是响应", response.isResponse());
        assertTrue("应该是权威答案", response.isAuthoritative());
        assertEquals("响应码应该是NOERROR", DNSMessage.RCODE_NOERROR, response.getRcode());
        assertEquals("应该有一个问题", 1, response.getQuestions().size());
        assertEquals("应该有一个答案", 1, response.getAnswers().size());
        
        DNSRecord answer = response.getAnswers().get(0);
        assertEquals("答案域名应该正确", "local.example.com", answer.getName());
        assertEquals("答案类型应该是A", DNSRecord.TYPE_A, answer.getType());
        assertEquals("答案IP应该正确", "192.168.1.100", answer.getIPAddress());
    }
    
    @Test
    public void testCreateLocalResponseForNonAQuery() {
        DNSMessage query = DNSMessage.createQuery(12345, "example.com", DNSRecord.TYPE_NS);
        DNSMessage response = DNSMessage.createLocalResponse(query, "192.168.1.100");
        
        // 对于非A记录查询，不应该添加答案
        assertTrue("非A记录查询不应该有答案", response.getAnswers().isEmpty());
    }
    
    @Test
    public void testCreateLocalResponseWithEmptyQuery() {
        DNSMessage query = new DNSMessage(12345);
        DNSMessage response = DNSMessage.createLocalResponse(query, "192.168.1.100");
        
        // 没有问题的查询不应该有答案
        assertTrue("没有问题的查询不应该有答案", response.getAnswers().isEmpty());
    }
    
    @Test
    public void testAddMethods() {
        DNSMessage message = new DNSMessage();
        
        DNSQuestion question = DNSQuestion.createAQuery("example.com");
        message.addQuestion(question);
        assertEquals("应该有一个问题", 1, message.getQuestions().size());
        assertEquals("问题应该正确添加", question, message.getQuestions().get(0));
        
        DNSRecord answer = DNSRecord.createARecord("example.com", "192.168.1.1");
        message.addAnswer(answer);
        assertEquals("应该有一个答案", 1, message.getAnswers().size());
        assertEquals("答案应该正确添加", answer, message.getAnswers().get(0));
        
        DNSRecord authority = DNSRecord.createARecord("ns.example.com", "192.168.1.2");
        message.addAuthority(authority);
        assertEquals("应该有一个权威记录", 1, message.getAuthorities().size());
        assertEquals("权威记录应该正确添加", authority, message.getAuthorities().get(0));
        
        DNSRecord additional = DNSRecord.createARecord("additional.example.com", "192.168.1.3");
        message.addAdditional(additional);
        assertEquals("应该有一个附加记录", 1, message.getAdditionals().size());
        assertEquals("附加记录应该正确添加", additional, message.getAdditionals().get(0));
    }
    
    @Test
    public void testSettersAndGetters() {
        DNSMessage message = new DNSMessage();
        
        message.setId(65535);
        assertEquals("ID设置", 65535, message.getId());
        
        message.setResponse(true);
        assertTrue("响应标志设置", message.isResponse());
        
        message.setOpcode(15);
        assertEquals("操作码设置", 15, message.getOpcode());
        
        message.setAuthoritative(true);
        assertTrue("权威标志设置", message.isAuthoritative());
        
        message.setTruncated(true);
        assertTrue("截断标志设置", message.isTruncated());
        
        message.setRecursionDesired(true);
        assertTrue("期望递归标志设置", message.isRecursionDesired());
        
        message.setRecursionAvailable(true);
        assertTrue("递归可用标志设置", message.isRecursionAvailable());
        
        message.setRcode(DNSMessage.RCODE_SERVFAIL);
        assertEquals("响应码设置", DNSMessage.RCODE_SERVFAIL, message.getRcode());
    }
    
    @Test
    public void testGetRcodeString() {
        DNSMessage message = new DNSMessage();
        
        message.setRcode(DNSMessage.RCODE_NOERROR);
        assertEquals("NOERROR字符串", "NOERROR", message.getRcodeString());
        
        message.setRcode(DNSMessage.RCODE_FORMERR);
        assertEquals("FORMERR字符串", "FORMERR", message.getRcodeString());
        
        message.setRcode(DNSMessage.RCODE_SERVFAIL);
        assertEquals("SERVFAIL字符串", "SERVFAIL", message.getRcodeString());
        
        message.setRcode(DNSMessage.RCODE_NXDOMAIN);
        assertEquals("NXDOMAIN字符串", "NXDOMAIN", message.getRcodeString());
        
        message.setRcode(DNSMessage.RCODE_NOTIMP);
        assertEquals("NOTIMP字符串", "NOTIMP", message.getRcodeString());
        
        message.setRcode(DNSMessage.RCODE_REFUSED);
        assertEquals("REFUSED字符串", "REFUSED", message.getRcodeString());
        
        message.setRcode(99);
        assertEquals("未知响应码字符串", "RCODE99", message.getRcodeString());
    }
    
    @Test
    public void testToString() {
        DNSMessage message = new DNSMessage(12345);
        message.setResponse(true);
        message.setRcode(DNSMessage.RCODE_NOERROR);
        message.addQuestion(DNSQuestion.createAQuery("example.com"));
        message.addAnswer(DNSRecord.createARecord("example.com", "192.168.1.1"));
        
        String str = message.toString();
        assertTrue("toString应该包含ID", str.contains("12345"));
        assertTrue("toString应该包含Response", str.contains("Response"));
        assertTrue("toString应该包含NOERROR", str.contains("NOERROR"));
        assertTrue("toString应该包含Questions=1", str.contains("Questions=1"));
        assertTrue("toString应该包含Answers=1", str.contains("Answers=1"));
    }
    
    @Test
    public void testResponseCodeConstants() {
        assertEquals("NOERROR常量", 0, DNSMessage.RCODE_NOERROR);
        assertEquals("FORMERR常量", 1, DNSMessage.RCODE_FORMERR);
        assertEquals("SERVFAIL常量", 2, DNSMessage.RCODE_SERVFAIL);
        assertEquals("NXDOMAIN常量", 3, DNSMessage.RCODE_NXDOMAIN);
        assertEquals("NOTIMP常量", 4, DNSMessage.RCODE_NOTIMP);
        assertEquals("REFUSED常量", 5, DNSMessage.RCODE_REFUSED);
    }
}
