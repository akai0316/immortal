package com.bird.immortal;

import com.bird.immortal.commands.MeditateCommand;
import com.bird.immortal.commands.RealmCommand;
import com.bird.immortal.commands.RealmTabCompleter;
import com.bird.immortal.commands.ImmortalCommand;
import com.bird.immortal.commands.ImmortalTabCompleter;
import com.bird.immortal.listeners.PlayerListener;
import com.bird.immortal.meditation.MeditationManager;
import com.bird.immortal.storage.StorageManager;
import com.bird.immortal.config.ConfigManager;
import com.bird.immortal.realm.RealmManager;
import com.bird.immortal.realm.PlayerRealm;
import com.bird.immortal.spiritual.SpiritualBonus;
import com.bird.immortal.spiritual.SpiritualManager;
import com.bird.immortal.cache.ImmortalCache;
import com.bird.immortal.message.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Immortal extends JavaPlugin {
    private static Immortal instance;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置管理器
        ConfigManager.initialize(this);
        
        // 初始化消息管理器
        MessageManager.initialize();
        
        // 加載靈氣加成設定
        SpiritualBonus.reloadBonuses();
        
        // 加載生態域靈氣設定
        SpiritualManager.loadBiomeRanges();
        
        // 初始化打坐管理器
        MeditationManager.initialize(this);
        
        // 初始化儲存系統
        StorageManager.initialize(this);
        
        // 初始化境界管理器
        RealmManager.initialize(this);
        
        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        
        // 註冊命令
        getCommand("realm").setExecutor(new RealmCommand());
        getCommand("realm").setTabCompleter(new RealmTabCompleter());
        getCommand("meditate").setExecutor(new MeditateCommand());
        getCommand("immortal").setExecutor(new ImmortalCommand());
        getCommand("immortal").setTabCompleter(new ImmortalTabCompleter());
        
        // 定期保存緩存數據到資料庫
        new BukkitRunnable() {
            @Override
            public void run() {
                ImmortalCache.saveAllCache();
            }
        }.runTaskTimer(this, 12000L, 12000L); // 每10分鐘保存一次
        
        // 定期清理過期緩存
        new BukkitRunnable() {
            @Override
            public void run() {
                ImmortalCache.cleanExpiredCache();
            }
        }.runTaskTimer(this, 6000L, 6000L); // 每5分鐘清理一次
        
        // 定期清理過期的突破確認
        new BukkitRunnable() {
            @Override
            public void run() {
                RealmCommand.cleanupPendingBreakthroughs();
            }
        }.runTaskTimer(this, 600L, 600L); // 每30秒清理一次
        
        getLogger().info("修仙插件已啟動！");
    }

    @Override
    public void onDisable() {
        // 確保所有玩家停止打坐
        getServer().getOnlinePlayers().forEach(MeditationManager::stopMeditation);
        
        // 保存所有玩家數據
        getServer().getOnlinePlayers().forEach(player -> {
            PlayerRealm realm = RealmManager.getPlayerRealm(player.getUniqueId());
            if (realm != null) {
                StorageManager.savePlayerRealm(player.getUniqueId(), realm);
            }
        });
        
        // 保存所有數據
        StorageManager.saveAllData();
        
        // 保存所有緩存數據
        ImmortalCache.saveAllCache();
        
        // 關閉境界管理器
        RealmManager.onDisable();
        
        getLogger().info("修仙插件已關閉！");
    }

    public static Immortal getInstance() {
        return instance;
    }
}
