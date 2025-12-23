package com.xingmot.gtmadvancedhatch.util;

import cn.qiuye.gtmoremachine.utils.TeamUtils;
import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;

public class AHUtil {

    private AHUtil() {}

    public static Component getTeamName(Level level, UUID playerUUID) {
        Component name = Component.translatable("gtmadvancedhatch.machine.unknow_player");
        if (TeamUtils.hasOwner(level, playerUUID))
            name = AHUtil.GetName(level, playerUUID);
        return name;
    }

    public static Component GetName(Level level, UUID playerUUID) {
        if (LDLib.isModLoaded("ftbteams") && FTBTeamsAPI.api().isManagerLoaded()) {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerUUID);
            if (team.isPresent()) return team.get().getName();
        }
        Player player = level.getPlayerByUUID(playerUUID);
        if (player != null)
            return player.getName();
        else
            return Component.literal(playerUUID.toString());
    }

    // region 》数学工具方法
    public static long addWithLongBounds(long a, long b) {
        long result = a + b;

        // 检查正溢出
        if (a > 0 && b > 0 && result < 0) {
            return Long.MAX_VALUE;
        }
        // 检查负溢出
        if (a < 0 && b < 0 && result > 0) {
            return Long.MIN_VALUE;
        }

        return result;
    }

    public static long multiplyWithLongBounds(long a, long b) {
        if (a == 0 || b == 0) return 0;

        long result = a * b;

        // 检查溢出
        if (a == Long.MIN_VALUE && b == -1) {
            return Long.MAX_VALUE; // 特殊情况
        }
        if (result / a != b) { // 溢出检查
            return ((a > 0 && b > 0) || (a < 0 && b < 0)) ?
                    Long.MAX_VALUE : Long.MIN_VALUE;
        }

        return result;
    }

    public static long divWithLongBounds(long a, long b) {
        if (b == 0) return a > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        long result = a / b;
        // 检查溢出
        if (a == Long.MIN_VALUE && b == -1) {
            return Long.MAX_VALUE; // 特殊情况
        }
        return result;
    }

    public static int addWithIntegerBounds(int a, int b) {
        long result = (long) a + b;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(result, Integer.MAX_VALUE));
    }

    public static int multiplyWithIntegerBounds(int a, int b) {
        if (a == 0 || b == 0) return 0;
        long result = (long) a * b;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(result, Integer.MAX_VALUE));
    }

    public static int divWithIntegerBounds(int a, int b) {
        if (b == 0) return a > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        long result = (long) a / b;
        // 检查溢出
        if (a == Integer.MIN_VALUE && b == -1) {
            return Integer.MAX_VALUE; // 特殊情况
        }
        return (int) result;
    }
    // endregion
    // region 》客户端工具方法

    /**
     * 根据字符串长度动态计算字体大小
     * coded by PORTB from BiggerStacks(Licensed under GNU LGPL v3)
     */
    public static double calculateStringScale(Font font, String string) {
        var width = font.width(string);
        if (width < 16)
            return 1.0;
        else
            return 16.0 / width;
    }

    /**
     * 返回数量占容量的百分比字符串，以及颜色。（绿黄红）
     */
    public static Pair<String, ChatFormatting> getCapacityProgressAndColor(long amount, long capacity, boolean isInputIO) {
        double progress = ((double) amount / capacity * 100);
        ChatFormatting color;
        if (isInputIO)
            color = progress < 25 ? ChatFormatting.RED : progress < 50 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        else color = progress < 25 ? ChatFormatting.GREEN : progress < 50 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return new Pair<>("(" + new DecimalFormat("#.#").format(progress) + "%)", color);
    }

    /**
     * 根据世界时间/服务器时间等，部分tick，需要的取余数，返回一个0~1之间的小数。
     * 
     * @param partialTicks 部分tick，即客户端拿到的tick的小数部分
     * @return 返回值通常是从小到大然后突然归0，再从小到大
     */
    @OnlyIn(Dist.CLIENT)
    public static float tickByNumber(float partialTicks, int number) {
        return tickAndWaitByNumber(partialTicks, number, 0);
    }

    /** 附带一个循环间歇等待时间 */
    @OnlyIn(Dist.CLIENT)
    public static float tickAndWaitByNumber(float partialTicks, int number, int wait) {
        if (Minecraft.getInstance().level == null) return 0;
        long tickTemp = Minecraft.getInstance().level.getGameTime() % (number + wait);
        if (tickTemp < wait) return 0;
        else tickTemp -= wait;
        return (tickTemp + partialTicks) / (float) number;
    }

    /**
     * 根据世界时间/服务器时间等，部分tick，需要的取余数，返回一个0~1之间的小数。
     * 
     * @param partialTicks 部分tick，即客户端拿到的tick的小数部分
     * @return 返回值通常是从小到大然后从大到小不断循环
     */
    @OnlyIn(Dist.CLIENT)
    public static float tickByNumberCycle(float partialTicks, int number) {
        return tickAndWaitByNumberCycle(partialTicks, number, 0);
    }

    /** 附带一个循环间歇等待时间 */
    @OnlyIn(Dist.CLIENT)
    public static float tickAndWaitByNumberCycle(float partialTicks, int number, int wait) {
        if (Minecraft.getInstance().level == null) return 0;
        long tickTemp = Minecraft.getInstance().level.getGameTime() % (number + wait);
        if (tickTemp < wait) return 0;
        else tickTemp -= wait;
        return 1F - Math.abs((tickTemp + partialTicks) / (float) number - 0.5F) * 2;
    }
    // endregion
}
