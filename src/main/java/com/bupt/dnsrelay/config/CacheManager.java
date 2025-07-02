package com.bupt.dnsrelay.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CacheManager 负责cache.txt的读写和LRU缓存管理。
 * 缓存的任何修改都会立即持久化到文件中，确保文件内容始终是内存缓存的精确快照。
 */
public class CacheManager {
    private static final int MAX_CACHE_SIZE = 5; // Default max entries
    private final String cacheFilePath;
    private final LinkedHashMap<String, String> cacheMap;
    private final Object lock = new Object(); // Object lock for thread safety

    /**
     * 构造函数，初始化并从文件加载缓存。
     *
     * @param cacheFilePath 缓存文件路径
     * @throws IOException 如果加载文件时发生IO错误
     */
    public CacheManager(String cacheFilePath) throws IOException {
        this.cacheFilePath = cacheFilePath;
        // Initializes an access-ordered LinkedHashMap for LRU.
        // Entries accessed (get/put) move to the end of the list.
        this.cacheMap = new LinkedHashMap<String, String>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        loadCache();
    }

    /**
     * 从缓存文件中加载记录到内存。
     *
     * @throws IOException IO异常
     */
    private void loadCache() throws IOException {
        File file = new File(cacheFilePath);
        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+", 2); // Use limit=2 to prevent incorrect splitting if domain contains spaces.
                if (parts.length == 2) {
                    cacheMap.put(parts[1].toLowerCase(), parts[0]); // parts[0] is IP, parts[1] is domain
                }
            }
        }
    }

    /**
     * 根据域名查找缓存中的IP地址。
     *
     * @param domain 要查询的域名 (domain:queryType)
     * @return 找到的IP地址，或null（如果未找到）
     */
    public String lookup(String domain) {
        if (domain == null) {
            return null;
        }
        synchronized (lock) {
            return cacheMap.get(domain.toLowerCase());
        }
    }

    /**
     * 将一个新的域名-IP对添加到缓存中，并立即持久化到文件。
     * 如果域名已存在，则会更新其IP。
     *
     * @param domain 域名 (domain:queryType)
     * @param ip     对应的IP地址
     */
    public void put(String domain, String ip) {
        if (domain == null || ip == null) {
            return;
        }
        String lowerCaseDomain = domain.toLowerCase();

        synchronized (lock) {
            // Optimization: if value is unchanged, no need to rewrite file.
            if (ip.equals(cacheMap.get(lowerCaseDomain))) {
                return;
            }
            cacheMap.put(lowerCaseDomain, ip);
        }

        // Calls persistCache to synchronize file.
        // persistCache handles its own synchronization to ensure atomicity and consistency.
        persistCache();
    }

    /**
     * 将内存中的完整缓存内容覆盖写入到文件中。
     */
    private void persistCache() {
        synchronized (lock) {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(cacheFilePath, false), StandardCharsets.UTF_8))) { // false for overwrite
                for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                    writer.write(entry.getValue() + " " + entry.getKey());
                    writer.newLine();
                }
                writer.flush(); // Ensure all content is written to disk.
            } catch (IOException e) {
                System.err.println("[CACHE] Failed to persist cache to file: " + e.getMessage());
            }
        }
    }

    /**
     * 获取当前缓存中的条目数量。
     */
    public int size() {
        synchronized (lock) {
            return cacheMap.size();
        }
    }
}