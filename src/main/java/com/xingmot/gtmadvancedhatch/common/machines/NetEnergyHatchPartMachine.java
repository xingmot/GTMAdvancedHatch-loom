package com.xingmot.gtmadvancedhatch.common.machines;

import cn.qiuye.gtmoremachine.api.misc.wireless.energy.WirelessEnergyContainer;
import cn.qiuye.gtmoremachine.common.machine.multiblock.part.WirelessEnergyHatchPartMachine;
import cn.qiuye.gtmoremachine.utils.TeamUtils;
import com.xingmot.gtmadvancedhatch.api.IMutableBind;
import com.xingmot.gtmadvancedhatch.api.NoConsumeNotifiableEnergyContainer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
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

import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NetEnergyHatchPartMachine extends WirelessEnergyHatchPartMachine implements IMutableBind {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(NetEnergyHatchPartMachine.class, WirelessEnergyHatchPartMachine.MANAGED_FIELD_HOLDER);
    private TickableSubscription updEnergySubs;
    @Persisted
    public boolean isBatchEnable;

    public NetEnergyHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int amperage,boolean isLaser, Object... args) {
        super(holder, tier, io, amperage, isLaser, args);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NoConsumeNotifiableEnergyContainer createEnergyContainer(Object... args) {
        NoConsumeNotifiableEnergyContainer container;
        if (this.io == IO.OUT) {
            container = NoConsumeNotifiableEnergyContainer.emitterContainer(this, GTValues.V[this.tier] * 64L * (long) this.amperage, GTValues.V[this.tier], (long) this.amperage);
        } else {
            container = NoConsumeNotifiableEnergyContainer.receiverContainer(this, GTValues.V[this.tier] * 16L * (long) this.amperage, GTValues.V[this.tier], (long) this.amperage);
        }

        return container;
    }

    // 配方会默认直接拉电网，但这两个方法仍然可以兜底
    private void updateEnergy() {
        if (this.getUUID() == null) return;
        if (io == IO.IN) {
            useEnergy();
        } else {
            addEnergy();
        }
    }

    private void useEnergy() {
        var currentStored = energyContainer.getEnergyStored();
        var maxStored = energyContainer.getEnergyCapacity();
        var changeStored = Math.min(maxStored - currentStored, energyContainer.getInputVoltage() * energyContainer.getInputAmperage());
        if (changeStored <= 0) return;
        WirelessEnergyContainer container = getWirelessEnergyContainer();
        if (container == null) return;
        changeStored = container.removeEnergy(changeStored, this);
        if (changeStored > 0) energyContainer.setEnergyStored(currentStored + changeStored);
    }

    private void addEnergy() {
        var currentStored = energyContainer.getEnergyStored();
        if (currentStored <= 0) return;
        var changeStored = Math.min(energyContainer.getOutputVoltage() * energyContainer.getOutputAmperage(), currentStored);
        WirelessEnergyContainer container = getWirelessEnergyContainer();
        if (container == null) return;
        changeStored = container.addEnergy(changeStored, this);
        if (changeStored > 0) energyContainer.setEnergyStored(currentStored - changeStored);
    }

    //////////////////////////////////////
    // ********** 原封不动 ***********//
    //////////////////////////////////////
    public void onLoad() {
        super.onLoad();
        this.updateEnergySubscription();
    }

    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return InteractionResult.PASS;
        } else if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            setOwnerUUID(player.getUUID());
            ((NoConsumeNotifiableEnergyContainer) this.energyContainer).owner_uuid = player.getUUID();
            if (this.getLevel()!=null&&this.getLevel().isClientSide()) {
                player.sendSystemMessage(Component.translatable("gtmoremachine.machine.wireless_energy_hatch.tooltip.bind", TeamUtils.getName(player)));
            }
            this.updateEnergySubscription();
            return InteractionResult.SUCCESS;
        } else if (is.is(Items.STICK)) {
            if (this.io == IO.OUT && player.isCreative()) {
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
            setOwnerUUID(null);
            ((NoConsumeNotifiableEnergyContainer) this.energyContainer).owner_uuid = null;
            if (this.getLevel()!=null&&this.getLevel().isClientSide()) {
                player.sendSystemMessage(Component.translatable("gtmoremachine.machine.wireless_energy_hatch.tooltip.unbind"));
            }

            this.updateEnergySubscription();
            return true;
        } else {
            return false;
        }
    }

    public void onMachinePlaced(@Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            setOwnerUUID(player.getUUID());
            ((NoConsumeNotifiableEnergyContainer) energyContainer).setOwner_uuid(player.getUUID());
            this.updateEnergySubscription();
        }
    }

    private void updateEnergySubscription() {
        if (this.getOwnerUUID() != null) {
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
    public void setUUID(UUID uuid) {
        this.setOwnerUUID(uuid);
    }
}
