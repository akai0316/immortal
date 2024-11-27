package com.bird.immortal.storage;

import com.bird.immortal.Immortal;
import com.bird.immortal.realm.PlayerRealm;
import com.bird.immortal.realm.Realm;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.HashSet;
import java.util.Set;

public class StorageManager {
    private static File playerDataFile;
    private static FileConfiguration playerData;
    private static Immortal plugin;
    
    public static void initialize(Immortal plugin) {
        StorageManager.plugin = plugin;
        loadPlayerData();
    }
    
    private static void loadPlayerData() {
        // 創建數據文件夾
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "無法創建玩家數據文件", e);
            }
        }
        
        try {
            // 直接從文件讀取，完全繞過 Bukkit 的緩存
            playerData = new YamlConfiguration();
            playerData.load(playerDataFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "載入玩家數據文件時發生錯誤", e);
            // 如果載入失敗，創建新的空配置
            playerData = new YamlConfiguration();
        }
    }
    
    public static void savePlayerRealm(UUID playerUuid, PlayerRealm realm) {
        String path = "players." + playerUuid.toString();
        playerData.set(path + ".realm", realm.getRealm().name());
        playerData.set(path + ".level", realm.getLevel());
        playerData.set(path + ".spiritual_power", realm.getSpiritualPower());
        
        saveData();
    }
    
    public static PlayerRealm loadPlayerRealm(UUID playerUuid) {
        String path = "players." + playerUuid.toString();
        if (!playerData.contains(path)) {
            return new PlayerRealm(Realm.MORTAL, 1, 0);
        }
        
        String realmName = playerData.getString(path + ".realm");
        int level = playerData.getInt(path + ".level");
        long spiritualPower = playerData.getLong(path + ".spiritual_power");
        
        return new PlayerRealm(Realm.valueOf(realmName), level, spiritualPower);
    }
    
    public static void saveTrialCooldown(UUID playerUuid, long cooldownEnd) {
        playerData.set("cooldowns." + playerUuid.toString(), cooldownEnd);
        saveData();
    }
    
    public static Long loadTrialCooldown(UUID playerUuid) {
        return playerData.getLong("cooldowns." + playerUuid.toString(), 0);
    }
    
    private static void saveData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "無法保存玩家數據", e);
        }
    }

    // 在插件關閉時保存所有數據
    public static void saveAllData() {
        saveData();
    }

    public static Set<String> getAllTrialCooldowns() {
        ConfigurationSection cooldownSection = playerData.getConfigurationSection("cooldowns");
        if (cooldownSection == null) {
            return new HashSet<>();
        }
        return cooldownSection.getKeys(false);
    }

    // 添加重新載入數據文件的方法
    public static void reloadPlayerData() {
        // 直接重新載入，不保存當前數據
        loadPlayerData();
    }
} 