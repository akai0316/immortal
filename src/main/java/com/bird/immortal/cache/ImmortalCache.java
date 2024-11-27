package com.bird.immortal.cache;

import com.bird.immortal.realm.PlayerRealm;
import com.bird.immortal.storage.StorageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImmortalCache {
    // 玩家境界緩存
    private static final Map<UUID, CacheEntry<PlayerRealm>> realmCache = new HashMap<>();
    // 渡劫冷卻緩存
    private static final Map<UUID, CacheEntry<Long>> cooldownCache = new HashMap<>();
    // 緩存過期時間（毫秒）
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分鐘
    
    // 添加這個方法來獲取 realmCache Map
    public static Map<UUID, PlayerRealm> getPlayerRealms() {
        // 創建一個新的 Map 來存儲有效的緩存
        Map<UUID, PlayerRealm> validRealms = new HashMap<>();
        for (Map.Entry<UUID, CacheEntry<PlayerRealm>> entry : realmCache.entrySet()) {
            if (!entry.getValue().isExpired()) {
                validRealms.put(entry.getKey(), entry.getValue().value);
            }
        }
        return validRealms;
    }
    
    private static class CacheEntry<T> {
        final T value;
        final long expireTime;
        
        CacheEntry(T value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
    
    // 獲取玩家境界
    public static PlayerRealm getPlayerRealm(UUID uuid) {
        CacheEntry<PlayerRealm> entry = realmCache.get(uuid);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        
        // 從資料庫讀取
        PlayerRealm realm = StorageManager.loadPlayerRealm(uuid);
        updateRealmCache(uuid, realm);
        return realm;
    }
    
    // 更新玩家境界緩存
    public static void updateRealmCache(UUID uuid, PlayerRealm realm) {
        realmCache.put(uuid, new CacheEntry<>(realm, 
            System.currentTimeMillis() + CACHE_EXPIRE_TIME));
    }
    
    // 獲取渡劫冷卻時間
    public static Long getTrialCooldown(UUID uuid) {
        CacheEntry<Long> entry = cooldownCache.get(uuid);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        
        // 從資料庫讀取
        Long cooldown = StorageManager.loadTrialCooldown(uuid);
        updateCooldownCache(uuid, cooldown);
        return cooldown;
    }
    
    // 更新渡劫冷卻緩存
    public static void updateCooldownCache(UUID uuid, Long cooldown) {
        cooldownCache.put(uuid, new CacheEntry<>(cooldown, 
            System.currentTimeMillis() + CACHE_EXPIRE_TIME));
    }
    
    // 清理過期緩存
    public static void cleanExpiredCache() {
        realmCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        cooldownCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    // 清除指定玩家的所有緩存
    public static void clearPlayerCache(UUID uuid) {
        realmCache.remove(uuid);
        cooldownCache.remove(uuid);
    }
    
    // 保存所有緩存數據到資料庫
    public static void saveAllCache() {
        for (Map.Entry<UUID, CacheEntry<PlayerRealm>> entry : realmCache.entrySet()) {
            if (!entry.getValue().isExpired()) {
                StorageManager.savePlayerRealm(entry.getKey(), entry.getValue().value);
            }
        }
        
        for (Map.Entry<UUID, CacheEntry<Long>> entry : cooldownCache.entrySet()) {
            if (!entry.getValue().isExpired()) {
                StorageManager.saveTrialCooldown(entry.getKey(), entry.getValue().value);
            }
        }
    }
} 