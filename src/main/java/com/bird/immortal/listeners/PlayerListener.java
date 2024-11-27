package com.bird.immortal.listeners;

import com.bird.immortal.meditation.MeditationManager;
import com.bird.immortal.storage.StorageManager;
import com.bird.immortal.cache.ImmortalCache;
import org.bukkit.entity.Player;
import com.bird.immortal.realm.PlayerRealm;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.bird.immortal.commands.RealmCommand;
import java.util.UUID;

public class PlayerListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 玩家登入時從資料庫加載數據並更新緩存
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerRealm realm = StorageManager.loadPlayerRealm(uuid);
        ImmortalCache.updateRealmCache(uuid, realm);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家登出時保存緩存數據到資料庫
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerRealm realm = ImmortalCache.getPlayerRealm(uuid);
        if (realm != null) {
            StorageManager.savePlayerRealm(uuid, realm);
        }
        // 清除該玩家的緩存
        ImmortalCache.clearPlayerCache(uuid);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // 忽略視角移動
        }

        if (MeditationManager.isMeditating(event.getPlayer())) {
            MeditationManager.stopMeditation(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && MeditationManager.isMeditating(player)) {
            MeditationManager.stopMeditation(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // 如果玩家正在渡劫中死亡，立即觸發渡劫失敗
        if (RealmCommand.isInTrial(player.getUniqueId())) {
            RealmCommand.handleTrialFailure(player);
            RealmCommand.removeTrial(player.getUniqueId());
        }
    }
} 