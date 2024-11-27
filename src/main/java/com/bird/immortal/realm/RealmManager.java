package com.bird.immortal.realm;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Random;

import com.bird.immortal.storage.StorageManager;
import com.bird.immortal.config.ConfigManager;
import com.bird.immortal.Immortal;
import com.bird.immortal.cache.ImmortalCache;

public class RealmManager {
    private static final long[][] SPIRITUAL_REQUIREMENTS = {
        // 凡人
        {100L},
        // 煉氣期
        {100L, 300L, 600L},
        // 築基期
        {1200L, 1800L, 2600L, 3600L, 4800L, 6200L},
        // 金丹期
        {8000L, 10000L, 12500L, 15500L, 19000L, 23000L},
        // 元嬰期
        {30000L, 36000L, 43000L, 51000L, 60000L, 70000L, 81000L, 93000L, 106000L},
        // 化神期
        {120000L, 140000L, 165000L, 195000L, 230000L, 270000L, 315000L, 365000L, 420000L},
        // 煉虛期
        {500000L, 540000L, 580000L, 620000L, 670000L, 720000L, 780000L, 850000L, 930000L,
         1020000L, 1120000L, 1230000L, 1350000L, 1480000L, 1620000L, 1770000L, 1930000L, 2100000L},
        // 合體期
        {2000000L, 2100000L, 2220000L, 2350000L, 2490000L, 2640000L, 2800000L, 2970000L, 3150000L,
         3340000L, 3540000L, 3750000L, 3970000L, 4200000L, 4440000L, 4690000L, 4950000L, 5220000L},
        // 大乘期
        {5000000L, 5150000L, 5320000L, 5500000L, 5690000L, 5890000L, 6100000L, 6320000L, 6550000L,
         6790000L, 7040000L, 7300000L, 7570000L, 7850000L, 8140000L, 8440000L, 8750000L, 9070000L,
         9400000L, 9740000L, 10090000L, 10450000L, 10820000L, 11200000L, 11590000L, 11990000L, 12400000L,
         12820000L, 13250000L, 13690000L, 14140000L, 14600000L, 15070000L},
        // 渡劫期
        {10000000L, 10300000L, 10610000L, 10930000L, 11260000L, 11600000L, 11950000L, 12310000L, 12680000L,
         13060000L, 13450000L, 13850000L, 14260000L, 14680000L, 15110000L, 15550000L, 16000000L, 16460000L,
         16930000L, 17410000L, 17900000L, 18400000L, 18910000L, 19430000L, 19960000L, 20500000L, 21050000L,
         21610000L, 22180000L, 22760000L, 23350000L, 23950000L, 24560000L, 25180000L, 25810000L, 26450000L}
    };

    private static final Map<UUID, Long> trialCooldowns = new HashMap<>();
    private static Immortal plugin;

    // 初始化插件實例
    public static void initialize(Immortal instance) {
        plugin = instance;
        // 每分鐘自動保存一次數據
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllData();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // 1200 ticks = 1 分鐘
    }

    // 保存所有數據
    private static void saveAllData() {
        for (Map.Entry<UUID, PlayerRealm> entry : ImmortalCache.getPlayerRealms().entrySet()) {
            StorageManager.savePlayerRealm(entry.getKey(), entry.getValue());
        }
    }

    public static void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        // 只在玩家數據不存在時才從文件加載
        if (!ImmortalCache.getPlayerRealms().containsKey(uuid)) {
            PlayerRealm realm = StorageManager.loadPlayerRealm(uuid);
            ImmortalCache.updateRealmCache(uuid, realm);
        }
        // 載入渡劫冷卻時間
        if (!trialCooldowns.containsKey(uuid)) {
            Long cooldown = StorageManager.loadTrialCooldown(uuid);
            if (cooldown > System.currentTimeMillis()) {
                trialCooldowns.put(uuid, cooldown);
            }
        }
    }

    public static PlayerRealm getPlayerRealm(UUID uuid) {
        return ImmortalCache.getPlayerRealm(uuid);
    }

    public static long getSpiritualRequirement(Realm realm, int level) {
        String path = "realms.requirements." + realm.name() + "." + (level - 1);
        long defaultValue = SPIRITUAL_REQUIREMENTS[realm.ordinal()][level - 1];
        return ConfigManager.getConfig().getLong(path, defaultValue);
    }

    public static boolean canLevelUp(Player player) {
        PlayerRealm playerRealm = getPlayerRealm(player.getUniqueId());
        if (playerRealm == null) return false;

        long currentSpiritual = playerRealm.getSpiritualPower();
        long required = getSpiritualRequirement(playerRealm.getRealm(), playerRealm.getLevel());

        return currentSpiritual >= required;
    }

    public static void levelUp(Player player) {
        PlayerRealm playerRealm = getPlayerRealm(player.getUniqueId());
        if (playerRealm == null || !canLevelUp(player)) return;

        Realm currentRealm = playerRealm.getRealm();
        int currentLevel = playerRealm.getLevel();

        if (currentLevel >= currentRealm.getMaxLevel()) {
            Realm nextRealm = currentRealm.getNext();
            if (nextRealm != null) {
                playerRealm.setRealm(nextRealm);
                playerRealm.setLevel(1);
            }
        } else {
            playerRealm.setLevel(currentLevel + 1);
        }
    }

    public static Set<UUID> getAllPlayers() {
        return ImmortalCache.getPlayerRealms().keySet();
    }

    public static UUID getPlayerByRealm(PlayerRealm realm) {
        for (Map.Entry<UUID, PlayerRealm> entry : ImmortalCache.getPlayerRealms().entrySet()) {
            if (entry.getValue() == realm) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void setTrialCooldown(UUID playerUuid) {
        long cooldownEnd = System.currentTimeMillis() + (2 * 60 * 60 * 1000);
        trialCooldowns.put(playerUuid, cooldownEnd);
        StorageManager.saveTrialCooldown(playerUuid, cooldownEnd);
    }

    public static boolean isInTrialCooldown(UUID playerUuid) {
        Long cooldownEnd = trialCooldowns.get(playerUuid);
        if (cooldownEnd == null) {
            cooldownEnd = StorageManager.loadTrialCooldown(playerUuid);
            if (cooldownEnd > System.currentTimeMillis()) {
                trialCooldowns.put(playerUuid, cooldownEnd);
            }
        }
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    public static long getTrialCooldownTime(UUID playerUuid) {
        Long cooldownEnd = trialCooldowns.get(playerUuid);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        if (remaining <= 0) {
            trialCooldowns.remove(playerUuid);
            return 0;
        }
        return remaining;
    }

    public static void breakthrough(Player player, boolean isTrialSuccess) {
        PlayerRealm realm = getPlayerRealm(player.getUniqueId());
        if (realm == null) return;

        // 檢查是否是大境界突破
        if (realm.getLevel() >= realm.getRealm().getMaxLevel()) {
            if (realm.getRealm().getNext() != null) {
                // 如果是渡劫境界且渡劫成功
                if (realm.getRealm().ordinal() >= Realm.GOLDEN_CORE.ordinal()) {
                    if (!isTrialSuccess) {
                        // 渡劫失敗，設置冷卻時間
                        setTrialCooldown(player.getUniqueId());
                        return;
                    }
                }
                
                // 檢查突破成功率
                if (!isBreakthroughSuccessful(realm.getRealm())) {
                    // 突破失敗
                    player.sendMessage("§c突破失敗！需要等待冷卻時間後再次嘗試。");
                    player.sendMessage("§e當前境界突破成功率: §c" + getBreakthroughRate(realm.getRealm()) + "%");
                    // 設置冷卻時間
                    setTrialCooldown(player.getUniqueId());
                    return;
                }

                // 突破成功，消耗靈氣
                long required = getSpiritualRequirement(realm.getRealm(), realm.getLevel());
                realm.setSpiritualPower(realm.getSpiritualPower() - required);
                
                // 設置新境界
                realm.setRealm(realm.getRealm().getNext());
                realm.setLevel(1);
                player.sendMessage("§a突破成功！");
            }
        } else {
            // 小境界突破
            long required = getSpiritualRequirement(realm.getRealm(), realm.getLevel());
            realm.setSpiritualPower(realm.getSpiritualPower() - required);
            realm.setLevel(realm.getLevel() + 1);
        }
        
        // 保存數據
        savePlayerRealm(player.getUniqueId(), realm);
    }

    public static void savePlayerRealm(UUID uuid, PlayerRealm realm) {
        // 更新緩存
        ImmortalCache.updateRealmCache(uuid, realm);
    }

    public static void reloadConfig() {
        // 重新加載境界相關設定
        ConfigurationSection realmSection = ConfigManager.getConfig().getConfigurationSection("realms");
        if (realmSection != null) {
            // 清除所有現有的玩家境界數據
            clearAllPlayerData();
        }
    }

    public static void reloadTrialConfig() {
        // 重新加載渡劫相關設定
        ConfigurationSection trialSection = ConfigManager.getConfig().getConfigurationSection("tribulation");
        if (trialSection != null) {
            // 清除現有的冷卻時間
            trialCooldowns.clear();
            // 重新加載所有玩家的冷卻時間
            ConfigurationSection cooldownSection = ConfigManager.getConfig().getConfigurationSection("cooldowns");
            if (cooldownSection != null) {
                for (String uuidString : cooldownSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        long cooldownEnd = cooldownSection.getLong(uuidString);
                        if (System.currentTimeMillis() < cooldownEnd) {
                            trialCooldowns.put(uuid, cooldownEnd);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // 忽略無效的UUID
                    }
                }
            }
        }
    }

    // 添加清除所有玩家數據的方法
    public static void clearAllPlayerData() {
        ImmortalCache.getPlayerRealms().clear();
        // 重新加載所有在線玩家的數據
        for (Player player : Bukkit.getOnlinePlayers()) {
            initializePlayer(player);
        }
    }

    // 在插件關閉時保存所有數據
    public static void onDisable() {
        saveAllData();
    }

    // 添加獲取突破成功率的方法
    public static int getBreakthroughRate(Realm currentRealm) {
        String path = "realms.breakthrough_rates." + currentRealm.name();
        return ConfigManager.getConfig().getInt(path, 100);
    }
    
    // 檢查突破是否成功
    public static boolean isBreakthroughSuccessful(Realm currentRealm) {
        int successRate = getBreakthroughRate(currentRealm);
        return new Random().nextInt(100) < successRate;
    }
} 