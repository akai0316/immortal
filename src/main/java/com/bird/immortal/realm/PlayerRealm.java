package com.bird.immortal.realm;

public class PlayerRealm {
    private Realm realm;
    private int level;
    private long spiritualPower;

    public PlayerRealm(Realm realm, int level, long spiritualPower) {
        this.realm = realm;
        this.level = level;
        this.spiritualPower = spiritualPower;
    }

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getSpiritualPower() {
        return spiritualPower;
    }

    public void setSpiritualPower(long spiritualPower) {
        this.spiritualPower = spiritualPower;
    }

    public String getFullRealmName() {
        if (realm == Realm.MORTAL) {
            return realm.getName();
        }
        return realm.getName() + "期" + level + "層";
    }
} 