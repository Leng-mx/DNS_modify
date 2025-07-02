package com.bupt.dnsrelay.network;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * UDP服务器类
 * 负责DNS查询的接收和响应发送
 */
public class UDPServer {
    
    private static final int DNS_PORT = 53;
    private static final int MAX_PACKET_SIZE = 512;
    private static final int SOCKET_TIMEOUT = 5000; // 5秒超时
    
    private DatagramSocket serverSocket;
    private boolean isRunning;
    private int debugLevel;
    
    /**
     * 构造函数
     * @param debugLevel 调试级别
     */
    public UDPServer(int debugLevel) {
        this.debugLevel = debugLevel;
        this.isRunning = false;
    }
    
    /**
     * 启动UDP服务器
     * @throws IOException 网络错误
     */
    public void start() throws IOException {
        if (isRunning) {
            throw new IllegalStateException("Server is already running");
        }
        
        try {
            serverSocket = new DatagramSocket(DNS_PORT);
            serverSocket.setSoTimeout(SOCKET_TIMEOUT);
            isRunning = true;
            
            System.out.println("DNS Relay Server listening on port " + DNS_PORT);
            if (debugLevel >= 1) {
                System.out.println("Debug level: " + debugLevel);
                System.out.println("Socket timeout: " + SOCKET_TIMEOUT + "ms");
            }
            
        } catch (BindException e) {
            throw new IOException("Failed to bind to port " + DNS_PORT + 
                ". Port may be in use or requires administrator privileges.", e);
        }
    }
    
    /**
     * 停止UDP服务器
     */
    public void stop() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            System.out.println("DNS Relay Server stopped");
        }
    }
    
    /**
     * 接收DNS查询
     * @return DNS查询数据包，如果超时则返回null
     * @throws IOException 网络错误
     */
    public DNSPacket receiveQuery() throws IOException {
        if (!isRunning) {
            throw new IllegalStateException("Server is not running");
        }
        
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try {
            serverSocket.receive(packet);
            
            if (debugLevel >= 2) {
                System.out.printf("Received %d bytes from %s:%d%n",
                    packet.getLength(),
                    packet.getAddress().getHostAddress(),
                    packet.getPort());
            }
            
            // 创建DNS数据包对象
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
            
            return new DNSPacket(data, packet.getAddress(), packet.getPort());
            
        } catch (SocketTimeoutException e) {
            // 超时是正常的，返回null
            return null;
        }
    }
    
    /**
     * 发送DNS响应
     * @param responseData 响应数据
     * @param clientAddress 客户端地址
     * @param clientPort 客户端端口
     * @throws IOException 网络错误
     */
    public void sendResponse(byte[] responseData, InetAddress clientAddress, int clientPort) 
            throws IOException {
        if (!isRunning) {
            throw new IllegalStateException("Server is not running");
        }
        
        DatagramPacket responsePacket = new DatagramPacket(
            responseData, responseData.length, clientAddress, clientPort);
        
        serverSocket.send(responsePacket);
        
        if (debugLevel >= 2) {
            System.out.printf("Sent %d bytes to %s:%d%n",
                responseData.length,
                clientAddress.getHostAddress(),
                clientPort);
        }
    }
    
    /**
     * 转发DNS查询到上游服务器
     * @param queryData 查询数据
     * @param upstreamServer 上游DNS服务器地址
     * @return 上游服务器的响应数据，如果失败则返回null
     */
    public byte[] forwardQuery(byte[] queryData, String upstreamServer) {
        DatagramSocket clientSocket = null;
        
        try {
            // 创建客户端套接字
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(SOCKET_TIMEOUT);
            
            // 解析上游服务器地址
            InetAddress serverAddress = InetAddress.getByName(upstreamServer);
            
            if (debugLevel >= 1) {
                System.out.println("Forwarding query to upstream DNS: " + upstreamServer);
            }
            
            // 发送查询到上游服务器
            DatagramPacket queryPacket = new DatagramPacket(
                queryData, queryData.length, serverAddress, DNS_PORT);
            clientSocket.send(queryPacket);
            
            // 接收响应
            byte[] responseBuffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(
                responseBuffer, responseBuffer.length);
            clientSocket.receive(responsePacket);
            
            if (debugLevel >= 1) {
                System.out.printf("Received response from upstream DNS (%d bytes)%n",
                    responsePacket.getLength());
            }
            
            // 返回响应数据
            byte[] responseData = new byte[responsePacket.getLength()];
            System.arraycopy(responsePacket.getData(), 0, responseData, 0, responsePacket.getLength());
            return responseData;
            
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout waiting for response from upstream DNS: " + upstreamServer);
            return null;
        } catch (IOException e) {
            System.err.println("Error forwarding query to upstream DNS: " + e.getMessage());
            return null;
        } finally {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        }
    }
    
    /**
     * 检查服务器是否正在运行
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取服务器端口
     * @return 端口号
     */
    public int getPort() {
        return DNS_PORT;
    }
    
    /**
     * DNS数据包类
     * 封装DNS查询数据和客户端信息
     */
    public static class DNSPacket {
        private final byte[] data;
        private final InetAddress clientAddress;
        private final int clientPort;
        
        public DNSPacket(byte[] data, InetAddress clientAddress, int clientPort) {
            this.data = data.clone();
            this.clientAddress = clientAddress;
            this.clientPort = clientPort;
        }
        
        public byte[] getData() {
            return data.clone();
        }
        
        public InetAddress getClientAddress() {
            return clientAddress;
        }
        
        public int getClientPort() {
            return clientPort;
        }
        
        public String getClientInfo() {
            return clientAddress.getHostAddress() + ":" + clientPort;
        }
    }
}
