package com.xingmot.gtmadvancedhatch.common.machines;

import com.xingmot.gtmadvancedhatch.api.IBatchable;
import com.xingmot.gtmadvancedhatch.api.NoConsumeNotifiabbleLaserContainer;
import com.xingmot.gtmadvancedhatch.config.AHConfig;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import static com.xingmot.gtmadvancedhatch.common.data.MachinesConstants.getMaxCapacity;

import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.WirelessLaserHatchPartMachine;
import com.hepdd.gtmthings.utils.TeamUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetLaserHatchPartMachine extends WirelessLaserHatchPartMachine implements IBatchable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(NetLaserHatchPartMachine.class, WirelessLaserHatchPartMachine.MANAGED_FIELD_HOLDER);
    private TickableSubscription updEnergySubs;
    @Persisted
    public boolean isBatchEnable = AHConfig.INSTANCE.isBatchEnable;

    public NetLaserHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int amperage, Object... args) {
        super(holder, tier, io, amperage, args);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected @NotNull NoConsumeNotifiabbleLaserContainer createEnergyContainer(Object... args) {
        NoConsumeNotifiabbleLaserContainer container;
        long capacity = getMaxCapacity(this.io, this.amperage, GTValues.V[this.tier]);
        if (this.io == IO.OUT) {
            container = NoConsumeNotifiabbleLaserContainer.emitterContainer(this, capacity, GTValues.V[this.tier], (long) this.amperage);
        } else {
            container = NoConsumeNotifiabbleLaserContainer.receiverContainer(this, capacity, GTValues.V[this.tier], (long) this.amperage);
        }

        return container;
    }

    // 配方会默认直接拉电网，但这两个方法仍然可以兜底
    private void useEnergy() {
        long currentStored = this.energyContainer.getEnergyStored();
        long maxStored = this.energyContainer.getEnergyCapacity();
        long changeStored = Math.min(maxStored - currentStored, this.energyContainer.getInputVoltage() * this.energyContainer.getInputAmperage());
        if (currentStored != maxStored && WirelessEnergyManager.addEUToGlobalEnergyMap(this.owner_uuid, -changeStored, this)) {
            this.energyContainer.setEnergyStored(maxStored);
        }
    }

    private void addEnergy() {
        long currentStored = this.energyContainer.getEnergyStored();
        if (currentStored != 0L && WirelessEnergyManager.addEUToGlobalEnergyMap(this.owner_uuid, currentStored, this)) {
            this.energyContainer.setEnergyStored(0L);
        }
    }

    private void updateEnergy() {
        if (super.owner_uuid != null) {
            if (((NoConsumeNotifiabbleLaserContainer) this.energyContainer).owner_uuid == null) {
                ((NoConsumeNotifiabbleLaserContainer) this.energyContainer).owner_uuid = this.owner_uuid;
            }
            if (this.io == IO.IN) {
                this.useEnergy();
            } else {
                this.addEnergy();
            }
        }
    }

    //////////////////////////////////////
    // ********** 原封不动 ***********//

    /// ///////////////////////////////////
    public void onLoad() {
        super.onLoad();
        this.updateEnergySubscription();
    }

    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return InteractionResult.PASS;
        } else if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            this.owner_uuid = player.getUUID();
            ((NoConsumeNotifiabbleLaserContainer) this.energyContainer).owner_uuid = player.getUUID();
            if (this.getLevel().isClientSide()) {
                player.sendSystemMessage(Component.translatable("gtmthings.machine.wireless_energy_hatch.tooltip.bind", new Object[] { TeamUtil.GetName(player) }));
            }

            this.updateEnergySubscription();
            return InteractionResult.SUCCESS;
        } else if (is.is(Items.STICK) && player.isCreative()) {
            if (this.io == IO.OUT) {
                this.energyContainer.setEnergyStored(GTValues.V[this.tier] * 64L * (long) this.amperage);
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public boolean onLeftClick(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return false;
        } else if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            this.owner_uuid = null;
            ((NoConsumeNotifiabbleLaserContainer) this.energyContainer).owner_uuid = null;
            if (this.getLevel().isClientSide()) {
                player.sendSystemMessage(Component.translatable("gtmthings.machine.wireless_energy_hatch.tooltip.unbind"));
            }

            this.updateEnergySubscription();
            return true;
        } else {
            return false;
        }
    }

    public void onMachinePlaced(@Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            this.owner_uuid = player.getUUID();
            ((NoConsumeNotifiabbleLaserContainer) energyContainer).setOwner_uuid(player.getUUID());
            this.updateEnergySubscription();
        }
    }

    private void updateEnergySubscription() {
        if (super.owner_uuid != null) {
            this.updEnergySubs = this.subscribeServerTick(this.updEnergySubs, this::updateEnergy);
        } else if (this.updEnergySubs != null) {
            this.updEnergySubs.unsubscribe();
            this.updEnergySubs = null;
        }
    }

    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public boolean isBatchEnable() {
        return this.isBatchEnable;
    }

    @Override
    public void setBatchEnable(boolean isBatchEnable) {
        this.isBatchEnable = isBatchEnable;
    }
}
