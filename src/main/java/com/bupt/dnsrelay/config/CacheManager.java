package com.bupt.dnsrelay.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CacheManager 负责cache.txt的读写和LRU缓存管理
 */
public class CacheManager {
    private static final int MAX_CACHE_SIZE = 1000; // 可调整
    private final String cacheFilePath;
    private final LinkedHashMap<String, String> cacheMap;

    public CacheManager(String cacheFilePath) throws IOException {
        this.cacheFilePath = cacheFilePath;
        this.cacheMap = new LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        loadCache();
    }

    // 加载cache.txt到内存
    private void loadCache() throws IOException {
        File file = new File(cacheFilePath);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    cacheMap.put(parts[1].toLowerCase(), parts[0]);
                }
            }
        }
    }

    // 查找缓存
    public String lookup(String domain) {
        if (domain == null) return null;
        return cacheMap.get(domain.toLowerCase());
    }

    // 添加缓存并写入文件
    public synchronized void put(String domain, String ip) {
        if (domain == null || ip == null) return;
        domain = domain.toLowerCase();
        cacheMap.put(domain, ip);
        // 只追加一行，UTF-8编码
        try (BufferedWriter fw = new BufferedWriter(
                new OutputStreamWriter(new java.io.FileOutputStream(cacheFilePath, true), StandardCharsets.UTF_8))) {
            String line = ip + " " + domain + System.lineSeparator();
            fw.write(line);
            System.out.printf("[CACHE WRITE] %s", line);
        } catch (IOException e) {
            System.err.println("[CACHE] Failed to write cache: " + e.getMessage());
        }
        // LRU淘汰
        if (cacheMap.size() > MAX_CACHE_SIZE) {
            rewriteCacheFile();
        }
    }

    // 超限时重写cache.txt
    private void rewriteCacheFile() {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new java.io.FileOutputStream(cacheFilePath, false), StandardCharsets.UTF_8))) {
            for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                writer.write(entry.getValue() + " " + entry.getKey());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[CACHE] Failed to rewrite cache: " + e.getMessage());
        }
    }

    // 获取缓存条数
    public int size() {
        return cacheMap.size();
    }
} 