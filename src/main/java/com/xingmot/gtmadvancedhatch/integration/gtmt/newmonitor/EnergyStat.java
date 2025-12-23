package com.xingmot.gtmadvancedhatch.integration.gtmt.newmonitor;

import cn.qiuye.gtmoremachine.utils.TeamUtils;
import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.UUID;

import lombok.Getter;

public class EnergyStat {

    public static HashMap<UUID, EnergyStat> GlobalEnergyStat = new HashMap<>(100, 0.9F);
    public static MinecraftServer server;

    static boolean observed = false;

    public static EnergyStat createOrgetEnergyStat(UUID uuid) {
        if (LDLib.isRemote()) return new EnergyStat(0);
        if (GlobalEnergyStat.get(TeamUtils.getTeamUUID(uuid)) == null) {
            EnergyStat energyStat = new EnergyStat(server.getTickCount());
            GlobalEnergyStat.put(TeamUtils.getTeamUUID(uuid), energyStat);
        }
        return GlobalEnergyStat.get(TeamUtils.getTeamUUID(uuid));
    }

    private final TimeWheel minute;
    private final TimeWheel hour;
    private final TimeWheel day;
    private BigInteger lastChangedCache = BigInteger.ZERO;

    @Getter
    private BigDecimal avgEnergy = BigDecimal.ZERO;

    public EnergyStat(int windowStart) {
        minute = new TimeWheel(TimeWheel.TIMESCALE.SECOND, 60, windowStart);
        hour = new TimeWheel(TimeWheel.TIMESCALE.MINUTE, 60, windowStart);
        day = new TimeWheel(TimeWheel.TIMESCALE.HOUR, 24, windowStart);
    }

    public void tick() {
        if (minute.tock()) {
            if (hour.tock()) {
                day.tock();
            }
        }
        avgEnergy = lastChangedCache.compareTo(BigInteger.ZERO) == 0 ?
                BigDecimal.ZERO :
                new BigDecimal(lastChangedCache).divide(BigDecimal.valueOf(minute.slotResolution), RoundingMode.HALF_UP);
        lastChangedCache = BigInteger.ZERO;
    }

    public void update(BigInteger value) {
        minute.update(value, server.getTickCount());
        hour.update(value, server.getTickCount());
        day.update(value, server.getTickCount());
        lastChangedCache = lastChangedCache.add(value);
    }

    public BigDecimal getMinuteAvg() {
        return minute.getAvgByTick();
    }

    public BigDecimal getHourAvg() {
        return hour.getAvgByTick();
    }

    public BigDecimal getDayAvg() {
        return day.getAvgByTick();
    }
}
