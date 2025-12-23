package com.xingmot.gtmadvancedhatch.integration.gtmt.newmonitor;

import com.xingmot.gtmadvancedhatch.config.AHConfig;
import com.xingmot.gtmadvancedhatch.util.copy.FormattingUtil;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.xingmot.gtmadvancedhatch.integration.gtmt.newmonitor.FormatUtil.formatWithConstantWidth;

public class FormatUtil {

    public static String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1_000_000) {
            return String.format("%.1fK", number / 1000.0);
        } else if (number < 1_000_000_000) {
            return String.format("%.2fM", number / 1_000_000.0);
        } else {
            return String.format("%.2fG", number / 1_000_000_000.0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static List<Component> getLastText(List<Component> old) {
        List<Component> newList = new ArrayList<>();
        for (int i = 0; i < old.size(); ++i) {
            Component textComponent = old.get(i);
            if (textComponent.getString().startsWith("formatWidth,")) {
                String[] strings = textComponent.getString().split(",");
                String key = strings[1];
                int width = Integer.parseInt(strings[2]);
                ChatFormatting color = strings[3].equals("GOLD") ? ChatFormatting.GOLD :
                        strings[3].equals("AQUA") ? ChatFormatting.AQUA :
                                strings[3].equals("YELLOW") ? ChatFormatting.YELLOW :
                                        strings[3].equals("GREEN") ? ChatFormatting.GREEN :
                                                strings[3].equals("LIGHT_PURPLE") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE;
                newList.add(formatWithConstantWidth(key, width, old.get(++i)).withStyle(color));
                continue;
            }
            newList.add(textComponent);
        }
        return newList;
    }

    @SuppressWarnings("removal")
    public static MutableComponent formatWithConstantWidth(String labelKey, int width, Component body, Component... appends) {
        var a = new Component[appends.length + 1];
        a[0] = body;
        int i = 0;
        for (var c : appends) {
            a[++i] = c;
        }
        var tmp = Component.translatable(labelKey, (Object[]) a);
        var baseLength = getComponentLength(tmp);
        var spaceLength = width - baseLength;
        if (spaceLength <= 0) return tmp;

        int dotWidth = 3; // 这俩是原版默认值
        int spaceWidth = 4;
        if (LDLib.isClient()) { // 获取字体必须在客户端渲染线程
            // 获取字体实例
            Font font = Minecraft.getInstance().font;
            // 测量一个分隔符的宽度
            dotWidth = font.width(Component.literal("·")
                    .setStyle(Style.EMPTY.withFont(new ResourceLocation("gtmadvancedhatch", "separator_font"))));
            // 测量一个空格的宽度
            spaceWidth = font.width(" ");
        }
        // 计算可以容纳的分隔符数量
        int totalSpacerWidth = spaceLength - 2 * spaceWidth; // 预留2个空格的宽度
        int spacerCount = totalSpacerWidth / dotWidth;
        if (totalSpacerWidth <= 0 || spacerCount <= 0) return tmp;

        var separatorComponent = Component.literal("·".repeat(spacerCount))
                .setStyle(Style.EMPTY.withFont(new ResourceLocation("gtmadvancedhatch", "separator_font")));
        var spacerComponent = Component.literal(" ")
                .append(separatorComponent)
                .append(Component.literal(" "));
        a[0] = spacerComponent.append(body);
        return Component.translatable(labelKey, (Object[]) a);
    }

    public static Component voltageName(BigDecimal avgEnergy) {
        return Component.literal(GTValues.VNF[GTUtil.getFloorTierByVoltage(avgEnergy.abs()
                .longValue())]);
    }

    public static BigDecimal voltageAmperage(BigDecimal avgEnergy) {
        return avgEnergy.abs()
                .divide(BigDecimal.valueOf(GTValues.VEX[GTUtil.getFloorTierByVoltage(avgEnergy.abs()
                        .longValue())]), 1, RoundingMode.FLOOR);
    }

    public static String formatBigDecimalNumberOrSicWithSign(BigDecimal number, boolean isScientificNotation) {
        if (isScientificNotation) {
            String result = new DecimalFormat("0.00E0").format(number);
            return number.compareTo(BigDecimal.ZERO) > 0 ? "+" + result : result;
        }
        if (number.compareTo(BigDecimal.ZERO) == 0) return AHConfig.INSTANCE.WirelessEnergyMonitorZeroFormat;
        else if (number.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + FormattingUtil.formatNumberReadable(number);
        } else {
            return FormattingUtil.formatNumberReadable(number);
        }
    }

    private static int getComponentLength(Component component) {
        if (LDLib.isClient()) {
            return Minecraft.getInstance().font.width(component.getString());
        } else {
            return component.getString()
                    .length() * 9;
        }
    }
}
