package com.bird.immortal.spiritual;

import com.bird.immortal.config.ConfigManager;
import org.bukkit.block.Biome;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SpiritualManager {
    private static final Map<Biome, SpiritualRange> biomeRanges = new HashMap<>();
    private static final Random random = new Random();

    public static void loadBiomeRanges() {
        ConfigurationSection biomesSection = ConfigManager.getConfig().getConfigurationSection("biomes");
        if (biomesSection == null) return;

        // 清空現有的設定
        biomeRanges.clear();

        // 遍歷所有生態域分類
        for (String category : biomesSection.getKeys(false)) {
            ConfigurationSection categorySection = biomesSection.getConfigurationSection(category);
            if (categorySection == null) continue;

            // 遍歷該分類下的所有生態域
            for (String biomeName : categorySection.getKeys(false)) {
                try {
                    // 使用 Registry 獲取生態域
                    for (Biome biome : Registry.BIOME) {
                        if (biome.getKey().toString().equals("minecraft:" + biomeName.toLowerCase())) {
                            ConfigurationSection biomeSection = categorySection.getConfigurationSection(biomeName);
                            if (biomeSection != null) {
                                int min = biomeSection.getInt("min", 0);
                                int max = biomeSection.getInt("max", 0);
                                biomeRanges.put(biome, new SpiritualRange(min, max));
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // 忽略無效的生態域名稱
                }
            }
        }
    }

    public static int getBiomeSpiritualDensity(Biome biome) {
        SpiritualRange range = biomeRanges.getOrDefault(biome, new SpiritualRange(0, 0));
        if (range.max <= range.min) return range.min;
        return random.nextInt(range.max - range.min + 1) + range.min;
    }

    private static class SpiritualRange {
        final int min;
        final int max;

        SpiritualRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }
} 