package com.bupt.dnsrelay.dns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * DNS报文解析器
 * 负责DNS报文的解析和构造
 */
public class DNSParser {
    
    private static final int MAX_LABEL_SIZE = 63;
    private static final int MAX_DOMAIN_NAME = 255;
    
    /**
     * 解析DNS报文
     * @param data 报文字节数组
     * @return DNS报文对象
     * @throws IOException 解析错误
     */
    public static DNSMessage parseMessage(byte[] data) throws IOException {
        if (data.length < 12) {
            throw new IOException("DNS message too short");
        }
        
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        DNSMessage message = new DNSMessage();
        
        // 解析报文头
        parseHeader(dis, message);
        
        // 解析问题部分
        for (int i = 0; i < message.getQuestions().size(); i++) {
            DNSQuestion question = parseQuestion(dis, data);
            message.getQuestions().set(i, question);
        }
        
        // 解析答案部分
        for (int i = 0; i < message.getAnswers().size(); i++) {
            DNSRecord record = parseRecord(dis, data);
            message.getAnswers().set(i, record);
        }
        
        // 解析权威部分
        for (int i = 0; i < message.getAuthorities().size(); i++) {
            DNSRecord record = parseRecord(dis, data);
            message.getAuthorities().set(i, record);
        }
        
        // 解析附加部分
        for (int i = 0; i < message.getAdditionals().size(); i++) {
            DNSRecord record = parseRecord(dis, data);
            message.getAdditionals().set(i, record);
        }
        
        return message;
    }
    
    /**
     * 构造DNS报文
     * @param message DNS报文对象
     * @return 报文字节数组
     * @throws IOException 构造错误
     */
    public static byte[] buildMessage(DNSMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // 构造报文头
        buildHeader(dos, message);
        
        // 构造问题部分
        for (DNSQuestion question : message.getQuestions()) {
            buildQuestion(dos, question);
        }
        
        // 构造答案部分
        for (DNSRecord record : message.getAnswers()) {
            buildRecord(dos, record);
        }
        
        // 构造权威部分
        for (DNSRecord record : message.getAuthorities()) {
            buildRecord(dos, record);
        }
        
        // 构造附加部分
        for (DNSRecord record : message.getAdditionals()) {
            buildRecord(dos, record);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * 解析报文头
     */
    private static void parseHeader(DataInputStream dis, DNSMessage message) throws IOException {
        // 事务ID
        message.setId(dis.readUnsignedShort());
        
        // 标志位
        int flags = dis.readUnsignedShort();
        message.setResponse((flags & 0x8000) != 0);
        message.setOpcode((flags >> 11) & 0x0F);
        message.setAuthoritative((flags & 0x0400) != 0);
        message.setTruncated((flags & 0x0200) != 0);
        message.setRecursionDesired((flags & 0x0100) != 0);
        message.setRecursionAvailable((flags & 0x0080) != 0);
        message.setRcode(flags & 0x000F);
        
        // 计数器
        int qdcount = dis.readUnsignedShort();
        int ancount = dis.readUnsignedShort();
        int nscount = dis.readUnsignedShort();
        int arcount = dis.readUnsignedShort();
        
        // 初始化列表大小
        for (int i = 0; i < qdcount; i++) {
            message.getQuestions().add(new DNSQuestion());
        }
        for (int i = 0; i < ancount; i++) {
            message.getAnswers().add(new DNSRecord());
        }
        for (int i = 0; i < nscount; i++) {
            message.getAuthorities().add(new DNSRecord());
        }
        for (int i = 0; i < arcount; i++) {
            message.getAdditionals().add(new DNSRecord());
        }
    }
    
    /**
     * 构造报文头
     */
    private static void buildHeader(DataOutputStream dos, DNSMessage message) throws IOException {
        // 事务ID
        dos.writeShort(message.getId());
        
        // 标志位
        int flags = 0;
        if (message.isResponse()) flags |= 0x8000;
        flags |= (message.getOpcode() & 0x0F) << 11;
        if (message.isAuthoritative()) flags |= 0x0400;
        if (message.isTruncated()) flags |= 0x0200;
        if (message.isRecursionDesired()) flags |= 0x0100;
        if (message.isRecursionAvailable()) flags |= 0x0080;
        flags |= message.getRcode() & 0x000F;
        dos.writeShort(flags);
        
        // 计数器
        dos.writeShort(message.getQuestions().size());
        dos.writeShort(message.getAnswers().size());
        dos.writeShort(message.getAuthorities().size());
        dos.writeShort(message.getAdditionals().size());
    }
    
    /**
     * 解析问题
     */
    private static DNSQuestion parseQuestion(DataInputStream dis, byte[] data) throws IOException {
        DNSQuestion question = new DNSQuestion();
        
        // 解析域名
        String name = parseDomainName(dis, data);
        question.setName(name);
        
        // 解析类型和类别
        question.setType(dis.readUnsignedShort());
        question.setDnsClass(dis.readUnsignedShort());
        
        return question;
    }
    
    /**
     * 构造问题
     */
    private static void buildQuestion(DataOutputStream dos, DNSQuestion question) throws IOException {
        // 编码域名
        encodeDomainName(dos, question.getName());
        
        // 写入类型和类别
        dos.writeShort(question.getType());
        dos.writeShort(question.getDnsClass());
    }
    
    /**
     * 解析资源记录
     */
    private static DNSRecord parseRecord(DataInputStream dis, byte[] data) throws IOException {
        DNSRecord record = new DNSRecord();
        
        // 解析域名
        String name = parseDomainName(dis, data);
        record.setName(name);
        
        // 解析类型、类别、TTL和数据长度
        record.setType(dis.readUnsignedShort());
        record.setDnsClass(dis.readUnsignedShort());
        record.setTtl(dis.readInt() & 0xFFFFFFFFL);
        int rdlength = dis.readUnsignedShort();
        
        // 读取记录数据
        byte[] rdata = new byte[rdlength];
        dis.readFully(rdata);
        record.setRdata(rdata);
        
        return record;
    }
    
    /**
     * 构造资源记录
     */
    private static void buildRecord(DataOutputStream dos, DNSRecord record) throws IOException {
        // 编码域名
        encodeDomainName(dos, record.getName());
        
        // 写入类型、类别、TTL和数据长度
        dos.writeShort(record.getType());
        dos.writeShort(record.getDnsClass());
        dos.writeInt((int) record.getTtl());
        dos.writeShort(record.getRdataLength());
        
        // 写入记录数据
        if (record.getRdata() != null) {
            dos.write(record.getRdata());
        }
    }

    /**
     * 解析域名（支持压缩格式）
     */
    private static String parseDomainName(DataInputStream dis, byte[] data) throws IOException {
        StringBuilder name = new StringBuilder();

        while (true) {
            int len = dis.readUnsignedByte();

            // 检查是否为压缩指针
            if ((len & 0xC0) == 0xC0) {
                // 读取指针的第二个字节但不使用（简化实现）
                dis.readUnsignedByte();
                // 这里需要重新定位到指针位置，但DataInputStream不支持随机访问
                // 简化处理：抛出异常或使用其他方法
                throw new IOException("DNS name compression not fully supported in this implementation");
            }

            // 域名结束
            if (len == 0) {
                break;
            }

            // 检查标签长度
            if (len > MAX_LABEL_SIZE) {
                throw new IOException("DNS label too long: " + len);
            }

            // 读取标签
            if (name.length() > 0) {
                name.append('.');
            }

            byte[] label = new byte[len];
            dis.readFully(label);
            name.append(new String(label, "UTF-8"));

            if (name.length() > MAX_DOMAIN_NAME) {
                throw new IOException("DNS name too long");
            }
        }

        return name.toString();
    }

    /**
     * 编码域名
     */
    private static void encodeDomainName(DataOutputStream dos, String name) throws IOException {
        if (name == null || name.isEmpty()) {
            dos.writeByte(0);
            return;
        }

        String[] labels = name.split("\\.");
        for (String label : labels) {
            if (label.length() > MAX_LABEL_SIZE) {
                throw new IOException("DNS label too long: " + label);
            }

            byte[] labelBytes = label.getBytes("UTF-8");
            dos.writeByte(labelBytes.length);
            dos.write(labelBytes);
        }
        dos.writeByte(0); // 结束标记
    }

    /**
     * 打印DNS报文信息（调试用）
     */
    public static void printMessage(DNSMessage message, int debugLevel) {
        if (debugLevel < 1) return;

        System.out.println("=== DNS Message ===");
        System.out.printf("ID: 0x%04X%n", message.getId());
        System.out.printf("Flags: QR=%s, OPCODE=%d, AA=%s, TC=%s, RD=%s, RA=%s, RCODE=%s%n",
                message.isResponse() ? "Response" : "Query",
                message.getOpcode(),
                message.isAuthoritative() ? "1" : "0",
                message.isTruncated() ? "1" : "0",
                message.isRecursionDesired() ? "1" : "0",
                message.isRecursionAvailable() ? "1" : "0",
                message.getRcodeString());
        System.out.printf("Questions: %d, Answers: %d, Authority: %d, Additional: %d%n",
                message.getQuestions().size(),
                message.getAnswers().size(),
                message.getAuthorities().size(),
                message.getAdditionals().size());

        // 打印问题部分
        for (int i = 0; i < message.getQuestions().size(); i++) {
            System.out.printf("Question %d: %s%n", i + 1, message.getQuestions().get(i));
        }

        // 打印答案部分
        for (int i = 0; i < message.getAnswers().size(); i++) {
            System.out.printf("Answer %d: %s%n", i + 1, message.getAnswers().get(i));
        }

        System.out.println("==================");
    }
}
