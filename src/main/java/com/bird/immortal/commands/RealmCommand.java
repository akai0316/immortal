package com.bird.immortal.commands;

import com.bird.immortal.Immortal;
import com.bird.immortal.config.ConfigManager;
import com.bird.immortal.realm.PlayerRealm;
import com.bird.immortal.realm.RealmManager;
import com.bird.immortal.realm.Realm;
import com.bird.immortal.cache.ImmortalCache;
import com.bird.immortal.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class RealmCommand implements CommandExecutor {

    // 用於追蹤正在渡劫的玩家
    private static final Set<UUID> playersInTrial = new HashSet<>();
    // 用於追蹤等待確認突破的玩家
    private static final Map<UUID, Long> pendingBreakthroughs = new HashMap<>();
    // 確認有效期(30秒)
    private static final long CONFIRM_TIMEOUT = 30 * 1000;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageManager.getMessage("general.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showOwnRealm(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
            case "查詢":
                if (args.length < 2) {
                    Map<String, String> placeholders = MessageManager.createPlaceholders();
                    player.sendMessage(MessageManager.getMessage("help.realm_info_usage", placeholders));
                    return true;
                }
                showPlayerRealm(player, args[1]);
                break;
            case "top":
            case "排行":
                showTopRealms(player);
                break;
            case "breakthrough":
            case "突破":
                attemptBreakthrough(player);
                break;
            default:
                Map<String, String> placeholders = MessageManager.createPlaceholders();
                player.sendMessage(MessageManager.getMessage("help.realm_unknown_command"));
                player.sendMessage(MessageManager.getMessage("help.realm_commands", placeholders));
                break;
        }
        return true;
    }

    private void showOwnRealm(Player player) {
        PlayerRealm realm = RealmManager.getPlayerRealm(player.getUniqueId());
        if (realm == null) {
            player.sendMessage(MessageManager.getMessage("general.data_load_error"));
            return;
        }

        Map<String, String> placeholders = MessageManager.createPlaceholders();
        placeholders.put("realm", realm.getFullRealmName());
        placeholders.put("power", formatNumber(realm.getSpiritualPower()));
        player.sendMessage(MessageManager.getMessage("realm.current_realm", placeholders));
        
        // 顯示升級所需靈氣
        if (realm.getRealm().getNext() != null || realm.getLevel() < realm.getRealm().getMaxLevel()) {
            long required = RealmManager.getSpiritualRequirement(realm.getRealm(), realm.getLevel());
            placeholders.put("amount", formatNumber(required));
            player.sendMessage(MessageManager.getMessage("realm.next_requirement", placeholders));
            
            placeholders.put("progress", String.format("%.2f", (realm.getSpiritualPower() * 100.0 / required)));
            player.sendMessage(MessageManager.getMessage("realm.cultivation_progress", placeholders));
        }
    }

    private String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1f", number / 1000.0) + "K";
        } else if (number < 1000000000) {
            return String.format("%.1f", number / 1000000.0) + "M";
        } else {
            return String.format("%.1f", number / 1000000000.0) + "B";
        }
    }

    private void showPlayerRealm(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(MessageManager.getMessage("general.player_not_found"));
            return;
        }

        PlayerRealm realm = RealmManager.getPlayerRealm(target.getUniqueId());
        if (realm == null) {
            sender.sendMessage(MessageManager.getMessage("general.data_load_error"));
            return;
        }

        Map<String, String> placeholders = MessageManager.createPlaceholders();
        placeholders.put("player", target.getName());
        placeholders.put("realm", realm.getFullRealmName());
        placeholders.put("power", formatNumber(realm.getSpiritualPower()));
        sender.sendMessage(MessageManager.getMessage("realm.other_player_realm", placeholders));
    }

    private void showTopRealms(Player player) {
        List<PlayerRealm> allRealms = new ArrayList<>();
        for (UUID uuid : RealmManager.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                PlayerRealm realm = RealmManager.getPlayerRealm(uuid);
                if (realm != null) {
                    allRealms.add(realm);
                }
            }
        }

        // 只按境界和層數排序
        allRealms.sort((r1, r2) -> {
            int realmCompare = Integer.compare(r2.getRealm().ordinal(), r1.getRealm().ordinal());
            if (realmCompare != 0) return realmCompare;
            return Integer.compare(r2.getLevel(), r1.getLevel());
        });

        player.sendMessage(MessageManager.getMessage("realm.top_list_title"));
        int rank = 1;
        for (PlayerRealm realm : allRealms) {
            if (rank > ConfigManager.getConfig().getInt("realms.top-list-size", 10)) break;
            Player p = Bukkit.getPlayer(RealmManager.getPlayerByRealm(realm));
            if (p != null) {
                Map<String, String> placeholders = MessageManager.createPlaceholders();
                placeholders.put("rank", String.valueOf(rank));
                placeholders.put("player", p.getName());
                placeholders.put("realm", realm.getFullRealmName());
                player.sendMessage(MessageManager.getMessage("realm.top_list_format", placeholders));
                rank++;
            }
        }
    }

    private void attemptBreakthrough(Player player) {
        PlayerRealm realm = RealmManager.getPlayerRealm(player.getUniqueId());
        if (realm == null) {
            player.sendMessage(MessageManager.getMessage("general.data_load_error"));
            return;
        }

        // 檢查是否有足夠的靈氣突破
        long required = RealmManager.getSpiritualRequirement(realm.getRealm(), realm.getLevel());
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        placeholders.put("amount", formatNumber(required - realm.getSpiritualPower()));
        
        if (realm.getSpiritualPower() < required) {
            player.sendMessage(MessageManager.getMessage("breakthrough.insufficient_power", placeholders));
            return;
        }

        // 檢查冷卻時間
        Long cooldown = ImmortalCache.getTrialCooldown(player.getUniqueId());
        if (cooldown != null && cooldown > System.currentTimeMillis()) {
            long remainingHours = (cooldown - System.currentTimeMillis()) / (1000 * 60 * 60);
            placeholders.put("hours", String.valueOf(remainingHours));
            player.sendMessage(MessageManager.getMessage("breakthrough.cooldown_active", placeholders));
            return;
        }

        // 檢查是否是大境界突破
        if (realm.getLevel() >= realm.getRealm().getMaxLevel() && realm.getRealm().getNext() != null) {
            UUID playerUuid = player.getUniqueId();
            Long confirmTime = pendingBreakthroughs.get(playerUuid);

            // 如果已經確認過且在有效期內
            if (confirmTime != null && System.currentTimeMillis() - confirmTime < CONFIRM_TIMEOUT) {
                pendingBreakthroughs.remove(playerUuid);
                
                // 先檢查突破成功率
                int successRate = RealmManager.getBreakthroughRate(realm.getRealm());
                if (!RealmManager.isBreakthroughSuccessful(realm.getRealm())) {
                    placeholders.put("rate", String.valueOf(successRate));
                    player.sendMessage(MessageManager.getMessage("breakthrough.breakthrough_failed"));
                    player.sendMessage(MessageManager.getMessage("breakthrough.current_rate", placeholders));
                    
                    // 設置冷卻時間
                    double cooldownHours = ConfigManager.getConfig().getDouble(
                        "tribulation.cooldown." + realm.getRealm().name(), 2.0);
                    long cooldownEnd = System.currentTimeMillis() + (long)(cooldownHours * 3600000L);
                    ImmortalCache.updateCooldownCache(playerUuid, cooldownEnd);
                    
                    placeholders.put("hours", String.valueOf(cooldownHours));
                    player.sendMessage(MessageManager.getMessage("breakthrough.cooldown_notice", placeholders));
                    return;
                }
                
                // 突破成功後，檢查是否需要渡劫
                if (realm.getRealm().ordinal() >= Realm.GOLDEN_CORE.ordinal()) {
                    player.sendMessage(MessageManager.getMessage("tribulation.start_notice"));
                    startTrial(player);
                } else {
                    // 不需要渡劫的境界，直接消耗靈氣並突破
                    realm.setSpiritualPower(realm.getSpiritualPower() - required);
                    RealmManager.breakthrough(player, true);
                    
                    // 廣播突破消息
                    if (ConfigManager.getConfig().getBoolean("realms.broadcast-breakthrough", true)) {
                        placeholders.put("player", player.getName());
                        Bukkit.broadcastMessage(MessageManager.getMessage("breakthrough.success_broadcast", placeholders));
                    }
                    
                    showOwnRealm(player);
                }
            } else {
                // 第一次輸入，顯示確認信息
                int successRate = RealmManager.getBreakthroughRate(realm.getRealm());
                placeholders.put("next_realm", realm.getRealm().getNext().getName());
                placeholders.put("rate", String.valueOf(successRate));
                
                player.sendMessage(MessageManager.getMessage("breakthrough.confirm_title"));
                player.sendMessage(MessageManager.getMessage("breakthrough.next_realm", placeholders));
                player.sendMessage(MessageManager.getMessage("breakthrough.success_rate", placeholders));
                player.sendMessage(MessageManager.getMessage("breakthrough.required_power", placeholders));
                
                if (realm.getRealm().ordinal() >= Realm.GOLDEN_CORE.ordinal()) {
                    player.sendMessage(MessageManager.getMessage("breakthrough.need_tribulation"));
                }
                
                player.sendMessage(MessageManager.getMessage("breakthrough.confirm_wait"));
                player.sendMessage(MessageManager.getMessage("breakthrough.confirm_timeout"));
                
                // 記錄確認時間
                pendingBreakthroughs.put(playerUuid, System.currentTimeMillis());
            }
        } else {
            // 小境界突破，直接消耗靈氣並突破
            realm.setSpiritualPower(realm.getSpiritualPower() - required);
            RealmManager.breakthrough(player, true);
            showOwnRealm(player);
        }
    }

    private void startTrial(Player player) {
        PlayerRealm realm = RealmManager.getPlayerRealm(player.getUniqueId());
        FileConfiguration config = ConfigManager.getConfig();
        
        // 檢查玩家位置是否適合渡劫
        if (!isValidTrialLocation(player)) {
            player.sendMessage(MessageManager.getMessage("tribulation.outdoor_required"));
            return;
        }
        
        // 將玩家加入渡劫列表
        playersInTrial.add(player.getUniqueId());
        
        // 獲取當前境界的渡劫設定
        String realmPath = "tribulation.realms." + realm.getRealm().name() + ".";
        
        // 獲取該境界的渡劫參數
        int maxStrikes = config.getInt(realmPath + "lightning-strikes", 5);
        long duration = config.getLong(realmPath + "duration", 60);
        double strikeInterval = config.getDouble(realmPath + "strike-interval", 3.0);
        double baseDamage = config.getDouble(realmPath + "damage.base", 6.0);
        double randomRange = config.getDouble(realmPath + "damage.random-range", 0.2);
        
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        placeholders.put("duration", String.valueOf(duration));
        placeholders.put("strikes", String.valueOf(maxStrikes));
        
        player.sendMessage(MessageManager.getMessage("tribulation.title"));
        player.sendMessage(MessageManager.getMessage("tribulation.duration_notice", placeholders));
        player.sendMessage(MessageManager.getMessage("tribulation.strikes_notice", placeholders));
        
        // 開始雷劫效果
        new BukkitRunnable() {
            int strikes = 0;
            boolean trialFailed = false;
            
            @Override
            public void run() {
                // 如果已經失敗了，直接取消任務
                if (trialFailed) {
                    this.cancel();
                    return;
                }

                // 檢查玩家是否離線
                if (!player.isOnline()) {
                    trialFailed = true;
                    handleTrialFailure(player);
                    removeTrial(player.getUniqueId());
                    this.cancel();
                    return;
                }

                // 檢查玩家是否躲在方塊內
                if (!isValidTrialLocation(player)) {
                    trialFailed = true;
                    handleTrialFailure(player);
                    player.sendMessage(MessageManager.getMessage("tribulation.rule_violation"));
                    removeTrial(player.getUniqueId());
                    this.cancel();
                    return;
                }

                // 檢查玩家是否已經死亡或正在渡劫失敗狀態
                if (!isInTrial(player.getUniqueId())) {
                    trialFailed = true;
                    this.cancel();
                    return;
                }

                if (strikes >= maxStrikes) {
                    handleTrialSuccess(player);
                    removeTrial(player.getUniqueId());
                    this.cancel();
                    return;
                }

                // 生成閃電並造成傷害
                Location strikeLocation = player.getLocation();
                player.getWorld().strikeLightning(strikeLocation);
                
                // 計算隨機傷害
                double randomFactor = 1.0 + (Math.random() * 2 * randomRange - randomRange);
                double finalDamage = baseDamage * randomFactor;
                
                // 對玩家造成傷害
                player.damage(finalDamage);
                
                strikes++;
                
                // 顯示進度
                Map<String, String> progressPlaceholders = MessageManager.createPlaceholders();
                progressPlaceholders.put("current", String.valueOf(strikes));
                progressPlaceholders.put("total", String.valueOf(maxStrikes));
                player.sendMessage(MessageManager.getMessage("tribulation.progress", progressPlaceholders));
            }
        }.runTaskTimer(Immortal.getInstance(), 20L, (long)(strikeInterval * 20L));
    }

    // 檢查玩家位置是否適合渡劫
    private boolean isValidTrialLocation(Player player) {
        Location loc = player.getLocation();
        
        // 檢查玩家頭頂上方16格範圍內是否有方塊
        for (int y = 0; y <= 16; y++) {
            Location checkLoc = loc.clone().add(0, y, 0);
            if (!checkLoc.getBlock().isPassable()) {
                return false;
            }
        }
        
        // 確保玩家在室外（檢查是否能看到天空）
        return loc.getWorld().getHighestBlockYAt(loc) <= loc.getY();
    }

    // 新增的靜態方法
    public static boolean isInTrial(UUID playerUuid) {
        return playersInTrial.contains(playerUuid);
    }

    public static void removeTrial(UUID playerUuid) {
        playersInTrial.remove(playerUuid);
    }

    public static void handleTrialFailure(Player player) {
        RealmManager.breakthrough(player, false);
        
        PlayerRealm realm = RealmManager.getPlayerRealm(player.getUniqueId());
        double cooldownHours = ConfigManager.getConfig().getDouble(
            "tribulation.cooldown." + realm.getRealm().name(), 2.0);
        
        long cooldownEnd = System.currentTimeMillis() + (long)(cooldownHours * 3600000L);
        ImmortalCache.updateCooldownCache(player.getUniqueId(), cooldownEnd);
        
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        placeholders.put("hours", String.valueOf(cooldownHours));
        
        if (player.isOnline()) {
            player.sendMessage(MessageManager.getMessage("tribulation.failure_notice", placeholders));
        }
        
        removeTrial(player.getUniqueId());
    }

    // 定期清理過期的確認記錄
    public static void cleanupPendingBreakthroughs() {
        long now = System.currentTimeMillis();
        pendingBreakthroughs.entrySet().removeIf(entry -> 
            now - entry.getValue() >= CONFIRM_TIMEOUT);
    }

    private void broadcastBreakthrough(Player player, boolean isTrial) {
        if (ConfigManager.getConfig().getBoolean("realms.broadcast-breakthrough", true)) {
            Map<String, String> placeholders = MessageManager.createPlaceholders();
            placeholders.put("player", player.getName());
            String messagePath = isTrial ? "tribulation.success_broadcast" : "breakthrough.success_broadcast";
            Bukkit.broadcastMessage(MessageManager.getMessage(messagePath, placeholders));
        }
    }

    private void handleTrialSuccess(Player player) {
        PlayerRealm realm = RealmManager.getPlayerRealm(player.getUniqueId());
        if (realm == null) return;
        
        long required = RealmManager.getSpiritualRequirement(realm.getRealm(), realm.getLevel());
        realm.setSpiritualPower(realm.getSpiritualPower() - required);
        
        RealmManager.breakthrough(player, true);
        broadcastBreakthrough(player, true);
        showOwnRealm(player);
    }
} 