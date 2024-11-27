package com.bird.immortal.spiritual;

import com.bird.immortal.config.ConfigManager;
import com.bird.immortal.realm.Realm;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpiritualBonus {
    private static final Map<Realm, double[]> REALM_BONUSES = new HashMap<>();

    static {
        initializeDefaultBonuses();
    }

    private static void initializeDefaultBonuses() {
        ConfigurationSection bonusSection = ConfigManager.getConfig().getConfigurationSection("spiritual_bonus");
        if (bonusSection != null) {
            for (Realm realm : Realm.values()) {
                if (bonusSection.contains(realm.name())) {
                    List<String> percentages = bonusSection.getStringList(realm.name());
                    double[] bonuses = new double[percentages.size()];
                    for (int i = 0; i < percentages.size(); i++) {
                        bonuses[i] = parsePercentage(percentages.get(i));
                    }
                    REALM_BONUSES.put(realm, bonuses);
                }
            }
        }
    }

    /**
     * 解析百分比字符串為小數
     * @param percentage 百分比字符串 (例如 "5%")
     * @return 小數值 (例如 0.05)
     */
    private static double parsePercentage(String percentage) {
        try {
            String value = percentage.replace("%", "").trim();
            return Double.parseDouble(value) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 重新加載靈氣加成配置
     */
    public static void reloadBonuses() {
        REALM_BONUSES.clear();
        initializeDefaultBonuses();
    }

    /**
     * 獲取指定境界和層數的靈氣吸收加成
     * @param realm 境界
     * @param level 層數
     * @return 靈氣吸收加成（以小數表示，例如0.05表示5%加成）
     */
    public static double getBonus(Realm realm, int level) {
        if (realm == null || level < 1) return 0.0;
        double[] bonuses = REALM_BONUSES.get(realm);
        if (bonuses == null || level > bonuses.length) return 0.0;
        return bonuses[level - 1];
    }

    /**
     * 獲取指定境界和層數的靈氣吸收加成百分比
     * @param realm 境界
     * @param level 層數
     * @return 靈氣吸收加成百分比（例如5表示5%加成）
     */
    public static double getBonusPercentage(Realm realm, int level) {
        return getBonus(realm, level) * 100;
    }

    /**
     * 獲取指定境界和層數的靈氣吸收加成百分比文字
     * @param realm 境界
     * @param level 層數
     * @return 靈氣吸收加成百分比文字（例如"5%"）
     */
    public static String getBonusPercentageText(Realm realm, int level) {
        return String.format("%.1f%%", getBonusPercentage(realm, level));
    }
} 