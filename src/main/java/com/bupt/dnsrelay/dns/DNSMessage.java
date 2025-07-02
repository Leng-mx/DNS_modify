package com.bupt.dnsrelay.dns;

import java.util.ArrayList;
import java.util.List;

/**
 * DNS报文类
 * 表示完整的DNS查询或响应报文
 */
public class DNSMessage {
    
    // DNS响应码常量
    public static final int RCODE_NOERROR = 0;   // 无错误
    public static final int RCODE_FORMERR = 1;   // 格式错误
    public static final int RCODE_SERVFAIL = 2;  // 服务器失败
    public static final int RCODE_NXDOMAIN = 3;  // 域名不存在
    public static final int RCODE_NOTIMP = 4;    // 未实现
    public static final int RCODE_REFUSED = 5;   // 拒绝
    
    // DNS报文头字段
    private int id;                    // 事务ID
    private boolean isResponse;        // QR位：查询(false)或响应(true)
    private int opcode;               // 操作码
    private boolean isAuthoritative;   // AA位：权威答案
    private boolean isTruncated;      // TC位：截断标志
    private boolean isRecursionDesired; // RD位：期望递归
    private boolean isRecursionAvailable; // RA位：递归可用
    private int rcode;                // 响应码
    
    // DNS报文内容
    private List<DNSQuestion> questions;     // 问题部分
    private List<DNSRecord> answers;         // 答案部分
    private List<DNSRecord> authorities;     // 权威部分
    private List<DNSRecord> additionals;     // 附加部分
    
    /**
     * 构造函数
     */
    public DNSMessage() {
        this.questions = new ArrayList<>();
        this.answers = new ArrayList<>();
        this.authorities = new ArrayList<>();
        this.additionals = new ArrayList<>();
        this.opcode = 0; // 标准查询
        this.rcode = RCODE_NOERROR;
    }
    
    /**
     * 构造函数
     * @param id 事务ID
     */
    public DNSMessage(int id) {
        this();
        this.id = id;
    }
    
    /**
     * 创建查询报文
     * @param id 事务ID
     * @param domain 查询域名
     * @param type 查询类型
     * @return DNS查询报文
     */
    public static DNSMessage createQuery(int id, String domain, int type) {
        DNSMessage message = new DNSMessage(id);
        message.setResponse(false);
        message.setRecursionDesired(true);
        
        DNSQuestion question = new DNSQuestion(domain, type, DNSRecord.CLASS_IN);
        message.addQuestion(question);
        
        return message;
    }
    
    /**
     * 创建错误响应报文
     * @param query 原始查询报文
     * @param rcode 错误码
     * @return DNS错误响应报文
     */
    public static DNSMessage createErrorResponse(DNSMessage query, int rcode) {
        DNSMessage response = new DNSMessage(query.getId());
        response.setResponse(true);
        response.setAuthoritative(true);
        response.setRecursionDesired(query.isRecursionDesired());
        response.setRcode(rcode);
        
        // 复制问题部分
        for (DNSQuestion question : query.getQuestions()) {
            response.addQuestion(question);
        }
        
        return response;
    }
    
    /**
     * 创建本地解析响应报文
     * @param query 原始查询报文
     * @param ipAddress IP地址
     * @return DNS响应报文
     */
    public static DNSMessage createLocalResponse(DNSMessage query, String ipAddress) {
        DNSMessage response = createErrorResponse(query, RCODE_NOERROR);
        
        if (!query.getQuestions().isEmpty()) {
            DNSQuestion question = query.getQuestions().get(0);
            if (question.getType() == DNSRecord.TYPE_A) {
                DNSRecord answer = DNSRecord.createARecord(question.getName(), ipAddress);
                response.addAnswer(answer);
            }
        }
        
        return response;
    }
    
    // 添加记录的方法
    public void addQuestion(DNSQuestion question) {
        questions.add(question);
    }
    
    public void addAnswer(DNSRecord record) {
        answers.add(record);
    }
    
    public void addAuthority(DNSRecord record) {
        authorities.add(record);
    }
    
    public void addAdditional(DNSRecord record) {
        additionals.add(record);
    }
    
    // Getter和Setter方法
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public boolean isResponse() {
        return isResponse;
    }
    
    public void setResponse(boolean response) {
        isResponse = response;
    }
    
    public int getOpcode() {
        return opcode;
    }
    
    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }
    
    public boolean isAuthoritative() {
        return isAuthoritative;
    }
    
    public void setAuthoritative(boolean authoritative) {
        isAuthoritative = authoritative;
    }
    
    public boolean isTruncated() {
        return isTruncated;
    }
    
    public void setTruncated(boolean truncated) {
        isTruncated = truncated;
    }
    
    public boolean isRecursionDesired() {
        return isRecursionDesired;
    }
    
    public void setRecursionDesired(boolean recursionDesired) {
        isRecursionDesired = recursionDesired;
    }
    
    public boolean isRecursionAvailable() {
        return isRecursionAvailable;
    }
    
    public void setRecursionAvailable(boolean recursionAvailable) {
        isRecursionAvailable = recursionAvailable;
    }
    
    public int getRcode() {
        return rcode;
    }
    
    public void setRcode(int rcode) {
        this.rcode = rcode;
    }
    
    public List<DNSQuestion> getQuestions() {
        return questions;
    }
    
    public List<DNSRecord> getAnswers() {
        return answers;
    }
    
    public List<DNSRecord> getAuthorities() {
        return authorities;
    }
    
    public List<DNSRecord> getAdditionals() {
        return additionals;
    }
    
    /**
     * 获取响应码的字符串表示
     */
    public String getRcodeString() {
        switch (rcode) {
            case RCODE_NOERROR: return "NOERROR";
            case RCODE_FORMERR: return "FORMERR";
            case RCODE_SERVFAIL: return "SERVFAIL";
            case RCODE_NXDOMAIN: return "NXDOMAIN";
            case RCODE_NOTIMP: return "NOTIMP";
            case RCODE_REFUSED: return "REFUSED";
            default: return "RCODE" + rcode;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DNS Message [ID=").append(id);
        sb.append(", QR=").append(isResponse ? "Response" : "Query");
        sb.append(", RCODE=").append(getRcodeString());
        sb.append(", Questions=").append(questions.size());
        sb.append(", Answers=").append(answers.size());
        sb.append("]");
        return sb.toString();
    }

    public DNSMessage getHeader() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHeader'");
    }
}
