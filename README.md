# DNS中继服务器 (DNS Relay Server)

## 项目概述

北京邮电大学（BUPT）与伦敦玛丽女王大学（QMUL）合作办学项目"通信与网络"课程的大作业，实现一个DNS中继服务器。

## 功能特性

### 三种核心功能
1. **域名拦截**: 对配置文件中IP为0.0.0.0的域名返回"域名不存在"错误
2. **本地解析**: 对配置文件中有有效IP的域名直接返回配置的IP地址
3. **中继转发**: 对不在配置文件中的域名转发给上游DNS服务器

## 项目结构

```
DNS_modify/
├── src/                           # 源代码目录
│   ├── main/
│   │   └── java/com/bupt/dnsrelay/  # Java源代码包
│   │       ├── DNSRelayServer.java    # 主程序类
│   │       ├── dns/                   # DNS处理包
│   │       │   ├── DNSMessage.java    # DNS报文类
│   │       │   ├── DNSParser.java     # DNS解析器
│   │       │   ├── DNSQuestion.java   # DNS问题类
│   │       │   └── DNSRecord.java     # DNS记录类
│   │       ├── config/                # 配置处理包
│   │       │   ├── CacheManager.java  # 缓存管理器
│   │       │   └── ConfigParser.java  # 配置解析器
│   │       └── network/               # 网络通信包
│   │           └── UDPServer.java     # UDP服务器
│   └── test/
│       └── java/                    # 测试代码目录
├── config/                         # 配置文件目录
│   └── dnsrelay.txt               # DNS映射配置文件
├── pom.xml                        # Maven配置文件
└── README.md                      # 项目说明文件
```

## 编译和运行

### 编译
```bash
# 使用Maven编译
mvn clean compile

# 打包成可执行JAR
mvn clean package
```

### 运行
```bash
# 使用Maven运行
mvn exec:java -Dexec.mainClass="com.bupt.dnsrelay.DNSRelayServer"

# 运行打包后的JAR文件
java -jar target/dns-relay.jar

# 指定上游DNS服务器
java -jar target/dns-relay.jar 8.8.8.8

# 指定配置文件
java -jar target/dns-relay.jar 8.8.8.8 config/dnsrelay.txt

# 调试模式
java -jar target/dns-relay.jar -d 8.8.8.8 config/dnsrelay.txt
java -jar target/dns-relay.jar -dd 8.8.8.8 config/dnsrelay.txt
```

## 测试方法

1. **配置DNS服务器**: 将系统DNS设置为127.0.0.1
2. **启动程序**: 运行dns_relay程序
3. **测试命令**:
   ```bash
   nslookup blocked.example.com    # 测试域名拦截
   nslookup local.example.com      # 测试本地解析
   nslookup www.baidu.com          # 测试中继转发
   ```

## 技术实现

- **编程语言**: Java 8+
- **构建工具**: Maven
- **网络协议**: UDP Socket (java.net.DatagramSocket)
- **端口**: 53 (DNS标准端口)
- **支持系统**: Windows/Linux/macOS (跨平台)

## 作者信息

- 课程: 通信与网络 小学期
- 指导教师: 高占春