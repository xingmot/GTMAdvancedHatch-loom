package com.xingmot.gtmadvancedhatch.mixin.gtm;

import com.gregtechceu.gtceu.integration.jade.provider.ElectricContainerBlockProvider;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.text.DecimalFormat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;

@Mixin(ElectricContainerBlockProvider.class)
public class ElectricContainerBlockProviderMixin {

    @Inject(remap = false, method = "addTooltip", at = @At("HEAD"), cancellable = true)
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block, BlockEntity blockEntity, IPluginConfig config, CallbackInfo ci) {
        long maxStorage = capData.getLong("MaxEnergy");
        if (maxStorage == 0) return; // do not add empty max storage progress bar

        long stored = capData.getLong("Energy");
        var helper = tooltip.getElementHelper();

        DecimalFormat format = new DecimalFormat("0.00E0");
        tooltip.add(
                helper.progress(
                        (float) ((double) stored / maxStorage),
                        Component.translatable("gtceu.jade.energy_stored", format.format(stored),
                                format.format(maxStorage)),
                        helper.progressStyle().color(0xFFEEE600, 0xFFEEE600).textColor(-1),
                        Util.make(BoxStyle.DEFAULT, style -> style.borderColor = 0xFF555555),
                        true));
        ci.cancel();
    }
}
