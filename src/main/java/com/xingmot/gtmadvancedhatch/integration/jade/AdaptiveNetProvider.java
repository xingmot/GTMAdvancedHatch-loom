package com.xingmot.gtmadvancedhatch.integration.jade;

import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;
import com.xingmot.gtmadvancedhatch.common.data.MachinesConstants;
import com.xingmot.gtmadvancedhatch.common.machines.adaptivehatch.AdaptiveNetEnergyHatchPartMachine;
import com.xingmot.gtmadvancedhatch.common.machines.adaptivehatch.AdaptiveNetEnergyTerminal;
import com.xingmot.gtmadvancedhatch.integration.jade.caps.IAdaptiveNetCap;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.integration.jade.provider.CapabilityBlockProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;

public class AdaptiveNetProvider extends CapabilityBlockProvider<IAdaptiveNetCap> {

    protected AdaptiveNetProvider() {
        super(GTMAdvancedHatch.id("adaptive_net_provider"));
    }

    @Override
    protected @Nullable IAdaptiveNetCap getCapability(Level level, BlockPos pos, @Nullable Direction side) {
        if (level.getBlockEntity(pos) instanceof MetaMachineBlockEntity metaMachineBlockEntity) {
            var metaMachine = metaMachineBlockEntity.getMetaMachine();
            if (metaMachine instanceof AdaptiveNetEnergyTerminal an) {
                return new IAdaptiveNetCap() {

                    @Override
                    public boolean isSlaveTerminal() {
                        return an.isSlave();
                    }

                    @Override
                    public boolean isAutoRebind() {
                        return an.isAutoRebind();
                    }

                    @Override
                    public UUID getUUID() {
                        return an.getUUID();
                    }

                    @Override
                    public String getName() {
                        if (an.getUUID().equals(MachinesConstants.UUID_ZERO))
                            return "everyone";
                        return AHUtil.getTeamName(level, an.getUUID()).getString();
                    }

                    @Override
                    public long getFrequency() {
                        return an.getFrequency();
                    }
                };
            } else if (metaMachine instanceof AdaptiveNetEnergyHatchPartMachine a) {
                return new IAdaptiveNetCap() {

                    @Override
                    public UUID getUUID() {
                        return a.getNet_uuid();
                    }

                    @Override
                    public String getName() {
                        if (a.getNet_uuid().equals(MachinesConstants.UUID_ZERO))
                            return "everyone";
                        return AHUtil.getTeamName(level, a.getUUID()).getString();
                    }

                    @Override
                    public long getFrequency() {
                        return a.getFrequency();
                    }
                };
            }
        }
        return null;
    }

    @Override
    protected void write(CompoundTag data, IAdaptiveNetCap capability) {
        if (capability.getUUID() != null) {
            data.putUUID("UUID", capability.getUUID());
            data.putString("Name", capability.getName());
            data.putLong("Frequency", capability.getFrequency());
            data.putBoolean("AutoRebind", capability.isAutoRebind());
            data.putBoolean("IsSlave", capability.isSlaveTerminal());
        }
    }

    @Override
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block, BlockEntity blockEntity, IPluginConfig config) {
        if (!(blockEntity instanceof MetaMachineBlockEntity metaMachineBlockEntity)) return;
        var metaMachine = metaMachineBlockEntity.getMetaMachine();
        if (capData.hasUUID("UUID") && metaMachine instanceof AdaptiveNetEnergyTerminal) {
            tooltips(tooltip, capData);
            var isAutoRebind = capData.getBoolean("AutoRebind");
            var isSlave = capData.getBoolean("IsSlave");
            var frequency = capData.getLong("Frequency");
            Component c = isAutoRebind ? Component.translatable("gtmadvancedhatch.gui.auto_rebind.yes").withStyle(ChatFormatting.DARK_GREEN) : Component.translatable("gtmadvancedhatch.gui.auto_rebind.no").withStyle(ChatFormatting.RED);
            tooltip.add(Component.translatable("gtmadvancedhatch.gui.auto_rebind").append(c));
            if (frequency != 0 && isSlave)
                tooltip.add(Component.translatable("gtmadvancedhatch.machine.adaptive.fail").withStyle(ChatFormatting.BLUE));
        } else if (capData.hasUUID("UUID") && metaMachine instanceof AdaptiveNetEnergyHatchPartMachine) {
            tooltips(tooltip, capData);
        }
    }

    private void tooltips(ITooltip tooltip, CompoundTag capData) {
        var uuid = capData.getUUID("UUID");
        var name = capData.getString("Name");
        var frequency = capData.getLong("Frequency");
        // Jade配置控制是否显示uuid
        if (IWailaConfig.get().getPlugin().get(GTMAdvancedHatch.id("adaptive_net_provider.show_uuid")))
            tooltip.add(Component.translatable("gtmadvancedhatch.jade.adaptive_net_provider.uuid")
                    .append(uuid.toString()).withStyle(ChatFormatting.YELLOW));
        if (name.equals("everyone"))
            tooltip.add(Component.translatable("gtmadvancedhatch.jade.adaptive_net_provider.name")
                    .append(Component.translatable("gtmadvancedhatch.gui.bind_uuid.everyone").withStyle(ChatFormatting.AQUA)));
        else tooltip.add(Component.translatable("gtmadvancedhatch.jade.adaptive_net_provider.name")
                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.translatable("gtmadvancedhatch.jade.adaptive_net_provider.frequency")
                .append(Component.literal(String.valueOf(frequency)).withStyle(ChatFormatting.AQUA)));
    }
}
