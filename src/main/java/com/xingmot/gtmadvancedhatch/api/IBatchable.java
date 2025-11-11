package com.xingmot.gtmadvancedhatch.api;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * 实验性添加：添加了这个接口的仓室，表示该仓室支持并行设置，会强制设置配方的最低运行时间秒数
 */
public interface IBatchable extends IMultiPart {

    default boolean isBatchEnable() {
        return false;
    }

    void setBatchEnable(boolean var1);

    @Override
    default void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IMultiPart.super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(
                (new IFancyConfiguratorButton.Toggle(GuiTextures.BUTTON_ALLOW_IMPORT_EXPORT.getSubTexture(0.0, (double) 0.0F, (double) 1.0F, (double) 0.5F),
                        GuiTextures.BUTTON_ALLOW_IMPORT_EXPORT.getSubTexture(0.0, 0.5, 1.0, 0.5),
                        this::isBatchEnable, (clickData, pressed) -> this.setBatchEnable(pressed)))
                        .setTooltipsSupplier((pressed) -> List.of(
                                Component.translatable("gtmadvancedhatch.gui.batch").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                                        .append(Component.translatable(pressed ? "gtmadvancedhatch.gui.universe.yes" : "gtmadvancedhatch.gui.universe.no")),
                                Component.translatable("gtmadvancedhatch.gui.batch.info"))));
    }
}
