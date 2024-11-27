package com.bird.immortal.realm;

public enum Realm {
    MORTAL("凡人", 1),
    LIANQI("煉氣", 3),
    FOUNDATION_ESTABLISHMENT("築基", 6),
    GOLDEN_CORE("金丹", 6),
    NASCENT_SOUL("元嬰", 9),
    SPIRIT_SEVERING("化神", 9),
    VOID_TRAINING("煉虛", 18),
    INTEGRATION("合體", 18),
    GREAT_VEHICLE("大乘", 33),
    TRIBULATION("渡劫", 36);

    private final String name;
    private final int maxLevel;

    Realm(String name, int maxLevel) {
        this.name = name;
        this.maxLevel = maxLevel;
    }

    public String getName() {
        return name;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Realm getNext() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return null;
        }
        return values()[nextOrdinal];
    }
} 