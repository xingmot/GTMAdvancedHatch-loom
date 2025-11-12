package com.xingmot.gtmadvancedhatch.integration.jade;

import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;
import com.xingmot.gtmadvancedhatch.api.IBatchable;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.integration.jade.provider.CapabilityBlockProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class BatchableProvider extends CapabilityBlockProvider<Boolean> {

    protected BatchableProvider() {
        super(GTMAdvancedHatch.id("batchable_provider"));
    }

    @Override
    protected @Nullable Boolean getCapability(Level level, BlockPos pos, @Nullable Direction side) {
        if (level.getBlockEntity(pos) instanceof MetaMachineBlockEntity metaMachineBlockEntity) {
            var metaMachine = metaMachineBlockEntity.getMetaMachine();
            if (metaMachine instanceof IBatchable ib) {
                return ib.isBatchEnable();
            }
        }
        return null;
    }

    @Override
    protected void write(CompoundTag data, Boolean capability) {
        data.putBoolean("batch_enable", capability);
    }

    @Override
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        if (!(blockEntity instanceof MetaMachineBlockEntity metaMachineBlockEntity)) return;
        var metaMachine = metaMachineBlockEntity.getMetaMachine();
        if (metaMachine instanceof IBatchable) {
            tooltip.add(Component.translatable("gtmadvancedhatch.gui.batch")
                    .append(Component.translatable(capData.getBoolean("batch_enable") ? "gtmadvancedhatch.gui.universe.yes" : "gtmadvancedhatch.gui.universe.no")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))));
        }
    }
}
