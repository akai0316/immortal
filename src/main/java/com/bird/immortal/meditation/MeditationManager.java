package com.bird.immortal.meditation;

import com.bird.immortal.Immortal;
import com.bird.immortal.realm.PlayerRealm;
import com.bird.immortal.realm.RealmManager;
import com.bird.immortal.spiritual.SpiritualManager;
import com.bird.immortal.spiritual.SpiritualBonus;
import com.bird.immortal.message.MessageManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MeditationManager {
    private static final Map<UUID, MeditationState> meditatingPlayers = new HashMap<>();
    private static Immortal plugin;

    public static void initialize(Immortal instance) {
        plugin = instance;
    }
    
    public static void startMeditation(Player player) {
        if (isMeditating(player)) {
            player.sendMessage(MessageManager.getMessage("meditation.already_meditating"));
            return;
        }

        PlayerRealm playerRealm = RealmManager.getPlayerRealm(player.getUniqueId());
        if (playerRealm == null) return;

        Location loc = player.getLocation();
        int density = SpiritualManager.getBiomeSpiritualDensity(loc.getBlock().getBiome());
        double bonus = SpiritualBonus.getBonus(playerRealm.getRealm(), playerRealm.getLevel());
        
        // 計算每秒獲得的靈氣
        double baseGain = density;
        double totalGain = baseGain * (1 + bonus);
        
        // 創建盔甲架並設置屬性
        ArmorStand chair = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        chair.setVisible(false);
        chair.setGravity(false);
        chair.setInvulnerable(true);
        chair.setSmall(true);
        
        // 讓玩家坐在盔甲架上
        chair.addPassenger(player);
        
        // 創建打坐狀態
        MeditationState state = new MeditationState((int)totalGain, chair);
        meditatingPlayers.put(player.getUniqueId(), state);

        // 創建靈氣獲取任務
        BukkitTask spiritualTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isMeditating(player)) {
                    stopMeditation(player);
                    cancel();
                    return;
                }
                playerRealm.setSpiritualPower(playerRealm.getSpiritualPower() + state.gainPerTick);
            }
        }.runTaskTimer(plugin, 20L, 20L); // 每秒執行一次
        
        // 創建粒子效果任務
        BukkitTask particleTask = new BukkitRunnable() {
            double angle = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !isMeditating(player)) {
                    cancel();
                    return;
                }

                // 產生螺旋上升的粒子效果
                spawnMeditationParticles(player, angle);
                
                angle += 0.2;
                if (angle > 2 * Math.PI) {
                    angle = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        state.spiritualTask = spiritualTask;
        state.particleTask = particleTask;
        
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        placeholders.put("density", String.valueOf(density));
        placeholders.put("bonus", String.format("%.1f", bonus * 100));
        placeholders.put("amount", String.format("%.1f", totalGain));
        
        player.sendMessage(MessageManager.getMessage("meditation.start"));
        player.sendMessage(MessageManager.getMessage("meditation.density", placeholders));
        player.sendMessage(MessageManager.getMessage("meditation.realm_bonus", placeholders));
        player.sendMessage(MessageManager.getMessage("meditation.gain_rate", placeholders));
    }

    public static void stopMeditation(Player player) {
        MeditationState state = meditatingPlayers.remove(player.getUniqueId());
        if (state != null) {
            if (state.chair != null) {
                state.chair.removePassenger(player);
                state.chair.remove();
            }
            if (state.spiritualTask != null) {
                state.spiritualTask.cancel();
            }
            if (state.particleTask != null) {
                state.particleTask.cancel();
            }
            player.sendMessage(MessageManager.getMessage("meditation.stop"));
        }
    }

    public static boolean isMeditating(Player player) {
        return meditatingPlayers.containsKey(player.getUniqueId());
    }

    private static void spawnMeditationParticles(Player player, double angle) {
        Location particleLoc = player.getLocation().add(0, 0.5, 0);
        double radius = 0.8;
        int particlesPerCircle = 8;
        
        for (int i = 0; i < particlesPerCircle; i++) {
            double currentAngle = angle + ((2 * Math.PI * i) / particlesPerCircle);
            double x = radius * Math.cos(currentAngle);
            double z = radius * Math.sin(currentAngle);
            
            Location spawnLoc = particleLoc.clone().add(x, 0, z);
            
            player.getWorld().spawnParticle(
                Particle.END_ROD,
                spawnLoc,
                0,
                0,
                0.1,
                0,
                0.02
            );
        }
    }

    private static class MeditationState {
        final int gainPerTick;
        final ArmorStand chair;
        BukkitTask spiritualTask;
        BukkitTask particleTask;

        MeditationState(int gainPerTick, ArmorStand chair) {
            this.gainPerTick = gainPerTick;
            this.chair = chair;
        }
    }
} 