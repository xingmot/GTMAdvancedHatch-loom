package com.xingmot.gtmadvancedhatch.util;

import com.xingmot.gtmadvancedhatch.util.copy.NumberUtils;

import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class AHFormattingUtil {

    public enum RainbowSpeed {

        FAST(1),
        FAST2(2),
        FAST3(3),
        FAST4(4),
        NORMAL(5),
        SLOW(25);

        @Getter
        private final int value;

        RainbowSpeed(int value) {
            this.value = value;
        }
    }

    /**
     * 彩色滚动字，需要放进tooltipBuilder或其他动态组件中否则不会滚动
     * tips：Component.translate(key).getString()方法在注册阶段是拿不到翻译的
     * tips2：获取物品名称更常用-> itemstack.getDisplayName() 会返回一个Component
     * 
     * @param s       通常填入Component.translate(key).getString()
     * @param speed   速度
     * @param reverse 反转，为false时是从左往右滚动
     */
    public static MutableComponent getRainbowScrollComponent(String s, RainbowSpeed speed, boolean reverse) {
        MutableComponent component = Component.empty();
        AHTooltipHelper.GTFormattingCode rainbow = speed == RainbowSpeed.FAST ? AHTooltipHelper.RAINBOW_FAST :
                speed == RainbowSpeed.FAST2 ? AHTooltipHelper.RAINBOW_FAST2 :
                        speed == RainbowSpeed.FAST3 ? AHTooltipHelper.RAINBOW_FAST3 :
                                speed == RainbowSpeed.FAST4 ? AHTooltipHelper.RAINBOW_FAST4 :
                                        speed == RainbowSpeed.NORMAL ? AHTooltipHelper.RAINBOW : AHTooltipHelper.RAINBOW_SLOW;
        AtomicInteger index = new AtomicInteger();
        for (char c : s.toCharArray()) {
            if (!Character.isSpaceChar(c)) {
                int a = reverse ? index.getAndIncrement() : index.getAndDecrement();
                component.append(Component.literal(String.valueOf(c))
                        .withStyle(style -> style.withColor(rainbow.getOffset(a))));
            } else {
                component.append(Component.literal(String.valueOf(c)));
            }
        }
        return component;
    }

    public static String formatLongBucketsCompactStringBuckets(long value) {
        if (value == 0) return value + "";
        if (value < 100)
            return String.format("%sm", new DecimalFormat("0.####").format(value * 1000d / FluidHelper.getBucket()));
        return formatNumberBy2(value / 1000.0);
    }

    public static String formatLongBucketsToShort(long value, long min) {
        if (value <= min) return value + " mB";
        return NumberUtils.formatDouble(value / 1000.0) + " B";
    }

    public static String formatLongToShort(long value, long min) {
        if (value <= min) return String.valueOf(value);
        return NumberUtils.formatDouble(value);
    }

    public static @NotNull String formatNumberBy2(double number) {
        final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Z", "Y", "B", "N", "D" };
        DecimalFormat df = new DecimalFormat("#.#");
        DecimalFormat df2 = new DecimalFormat("#");
        double temp = number;
        int unitIndex = 0;
        while (temp >= 1000 && unitIndex < UNITS.length - 1) {
            temp /= 1000;
            unitIndex++;
        }
        if (temp >= 100) {
            temp /= 1000;
            unitIndex++;
        }
        if (Math.floor(temp) != temp && temp < 10)
            return df.format(temp) + UNITS[unitIndex];
        else
            return df2.format(temp) + UNITS[unitIndex];
    }

    /**
     * 主要重构自Savitor的FormatUtil代码
     * 
     * @param leftComponent  左对齐组件
     * @param rightComponent 右对齐组件
     * @param width          宽度
     * @param fill           填充使用的字符串
     * @return 返回组装后的组件
     */
    // TODO 文本长度过长时自动缩放字体
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public static MutableComponent getFormatWidthComponent(MutableComponent leftComponent, MutableComponent rightComponent, int width, String fill) {
        int baseLength = Minecraft.getInstance().font.width(Component.empty().append(leftComponent).append(rightComponent).getString());
        var spaceLength = width - baseLength;
        if (spaceLength <= 0) return Component.empty().append(leftComponent).append(" ").append(rightComponent);
        // 获取字体实例
        Font font = Minecraft.getInstance().font;
        // 测量一个分隔符的宽度
        int fillWidth;
        if (fill.equals("·")) fillWidth = font.width(Component.literal(fill).setStyle(Style.EMPTY.withFont(new ResourceLocation("gtmadvancedhatch", "separator_font"))));
        else fillWidth = font.width(Component.literal(fill));
        // 测量一个空格的宽度
        int spaceWidth = font.width(" ");
        int totalSpacerWidth = spaceLength - 2 * spaceWidth; // 预留2个空格的宽度
        int spacerCount = totalSpacerWidth / fillWidth;
        if (totalSpacerWidth <= 0 || spacerCount <= 0) return Component.empty().append(leftComponent).append(rightComponent);
        if (fill.equals("·")) return Component.empty()
                .append(leftComponent).append(" ")
                .append(Component.literal(fill.repeat(spacerCount))
                        .withStyle(Style.EMPTY.withFont(new ResourceLocation("gtmadvancedhatch", "separator_font"))))
                .append(" ").append(rightComponent);
        else return Component.empty().append(leftComponent).append(" ").append(fill.repeat(spacerCount)).append(" ").append(rightComponent);
    }
}
