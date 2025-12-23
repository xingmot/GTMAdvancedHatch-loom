package com.xingmot.gtmadvancedhatch.common.machines;

import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableFluidTank;
import com.xingmot.gtmadvancedhatch.api.IMultiCapacity;
import com.xingmot.gtmadvancedhatch.api.gui.HugeTankWidget;
import com.xingmot.gtmadvancedhatch.api.gui.PhantomFluidCapacityWidget;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigurableFluidHatchPartMachine extends FluidHatchPartMachine implements IDistinctPart, IMachineLife, IInteractedMachine, IMultiCapacity, IMultiPart {
    // 想兼容gtlcore就必须继承FluidHatchPartMachine，否则会不识别配方

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigurableFluidHatchPartMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);
    @Persisted
    public int maxCapacity;
    private final int slots;
    protected @Nullable ISubscription tankSubs;

    public ConfigurableFluidHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int initialCapacity, int slots, Object... args) {
        super(holder, tier, io, initialCapacity, slots, args);
        this.slots = slots;
    }

    public static int getTankCapacity(int initialCapacity, int tier) {
        if (tier >= 3) return AHUtil.multiplyWithIntegerBounds(initialCapacity, (1 << (4 * Math.min(13, tier - 1))));
        return initialCapacity * (1 << tier);
    }

    // 因为流体槽的容量并不能持久化，因此这里需要手动载入
    public void initTank() {
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            cfTank.init();
        }
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        if (this.maxCapacity == 0L)
            this.maxCapacity = getTankCapacity(initialCapacity, tier);
        ConfigNotifiableFluidTank fluidTank = new ConfigNotifiableFluidTank(this, slots, this.maxCapacity, this.io);
        fluidTank.setAllowSameFluids(true);
        return fluidTank;
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        if (tank instanceof ConfigNotifiableFluidTank cTank) {
            if (io == IO.IN) cTank.newCapacity(this.maxCapacity);
            else cTank.newCapacity(0);
        }
    }

    @Override
    public void onLoad() {
        this.traits.forEach(MachineTrait::onMachineLoad);
        this.coverContainer.onLoad();
        Level var2 = this.getLevel();
        if (var2 instanceof ServerLevel serverLevel) {
            this.initTank(); // 持久化恢复流体储存的容量数据
            serverLevel.getServer().tell(new TickTask(0, this::updateTankSubscription));
        }

        this.tankSubs = this.tank.addChangedListener(this::updateTankSubscription);
    }

    @Override
    public boolean onLeftClick(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return false;
        } else if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!this.isRemote()) {
                CompoundTag tag = new CompoundTag();
                tag.put("ConfigurableHatch", this.writeConfigToTag());
                is.setTag(tag);
                is.setHoverName(Component.translatable("gtmadvancedhatch.machine.configurable_fluid_hatch.data_stick.name"));
                player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return InteractionResult.PASS;
        } else if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            CompoundTag tag = is.getTag();
            if (tag != null && tag.contains("ConfigurableHatch")) {
                if (!this.isRemote()) {
                    this.readConfigFromTag(tag.getCompound("ConfigurableHatch"));
                    this.updateTankSubscription();
                    player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
                }
                return InteractionResult.sidedSuccess(this.isRemote());
            }
        } else if (is.is(Items.OAK_SIGN.asItem()) && player.isCreative()) {
            /* 橡木告示牌右击查看调试数据 */
            if (!LDLib.isRemote() && tank instanceof ConfigNotifiableFluidTank cfTank) {
                MutableComponent c = Component.empty();
                MutableComponent d = Component.empty();
                c.append("流体过滤：[");
                d.append("容量：[");
                for (int i = 0; i < this.tank.getTanks(); i++) {
                    c.append(cfTank.getLockedRef()[i].getFluid()
                            .getDisplayName());
                    d.append(String.valueOf(cfTank.tankCapacity[i]));
                    if (i < cfTank.getTanks() - 1) {
                        c.append(",");
                        d.append(",");
                    }
                }
                c.append("]");
                d.append("]");
                player.sendSystemMessage(c);
                player.sendSystemMessage(d);
                player.sendSystemMessage(Component.literal(world.getBlockEntity(pos).saveWithFullMetadata().toString()));
            }
            return InteractionResult.SUCCESS;
        } else if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            if (!LDLib.isRemote()) {
                if (ToolHelper.isTool(is, GTToolType.SCREWDRIVER)) {
                    cfTank.setAllowSameFluids(!cfTank.getAllowSameFluids());
                    Component enable = cfTank.getAllowSameFluids() ? Component.translatable("gtmadvancedhatch.gui.universe.no") : Component.translatable("gtmadvancedhatch.gui.universe.yes");
                    player.sendSystemMessage(Component.translatable("gtmadvancedhatch.machine.configurable_fluid_hatch.screwdriver.tooltip").append(enable));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    protected CompoundTag writeConfigToTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag config = new CompoundTag();
        tag.put("ConfigurableHatch", config);
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            CompoundTag fluids = new CompoundTag();
            CompoundTag fluid_capacity = new CompoundTag();
            config.put("fluids", fluids);
            config.put("fluidCapacity", fluid_capacity);
            config.putBoolean("allowSameFluid", cfTank.getAllowSameFluids());
            for (int i = 0; i < this.tank.getTanks(); i++) {
                if (!cfTank.getIndexLocked(i).getFluid().isEmpty())
                    fluids.put(String.valueOf(i), cfTank.getIndexLocked(i).getFluid().writeToNBT(new CompoundTag()));
                fluid_capacity.putLong(String.valueOf(i), cfTank.getCapacity(i));
            }
        }
        tag.putBoolean("isDistinct", isDistinct());
        return tag;
    }

    protected void readConfigFromTag(CompoundTag tag) {
        if (tag.contains("ConfigurableHatch") && this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            CompoundTag config = tag.getCompound("ConfigurableHatch");
            CompoundTag fluids = config.getCompound("fluids");
            CompoundTag fluid_capacity = config.getCompound("fluidCapacity");
            for (int i = 0; i < this.tank.getTanks(); i++) {
                if (fluids.contains(String.valueOf(i)))
                    cfTank.setLocked(true, i, FluidStack.loadFluidStackFromNBT(fluids.getCompound(String.valueOf(i))));
                else cfTank.setLocked(false, i, FluidStack.EMPTY);
                cfTank.newCapacity(i, fluid_capacity.getInt(String.valueOf(i)));
            }
            cfTank.setAllowSameFluids(config.getBoolean("allowSameFluid"));
        }
        if (tag.contains("isDistinct")) {
            this.setDistinct(tag.getBoolean("isDistinct"));
        }
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        this.updateTankSubscription();
    }

    @Override
    public void setCapacity(int index, int capacity) {
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            cfTank.setCapacity(index, capacity);
        }
    }

    @Override
    public int getCapacity(int index) {
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            return cfTank.getCapacity(index);
        }
        return -1;
    }

    @Override
    public boolean isDistinct() {
        return this.tank.isDistinct();
    }

    @Override
    public void setDistinct(boolean b) {
        this.tank.setDistinct(b);
    }

    @Override
    public Widget createUIWidget() {
        return this.createMultiSlotGUI();
    }

    protected Widget createMultiSlotGUI() {
        int rowSize = 1;
        int colSize = 1;
        switch (this.slots) {
            case 8 -> {
                rowSize = 8;
            }
            case 16 -> {
                rowSize = 8;
                colSize = 2;
            }
        }

        WidgetGroup group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize * 2 + 16);
        WidgetGroup container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize * 2 + 8);
        int index = 0;
        if (this.tank instanceof ConfigNotifiableFluidTank cTank) {
            for (int y = 0; y < colSize; ++y) {
                for (int x = 0; x < rowSize; ++x) {
                    container.addWidget(new HugeTankWidget(cTank.getStorages()[index], 4 + x * 18, 4 + y * 36 + 18,
                            true, this.io.support(IO.IN)).setBackground(GuiTextures.FLUID_SLOT));
                    container.addWidget((new PhantomFluidCapacityWidget(cTank, cTank, this.getCapacity(index), this.maxCapacity, index++,
                            4 + x * 18, 4 + y * 36, 18, 18)).setShowAmount(true)
                            .setDrawHoverTips(true)
                            .setBackground(GuiTextures.FLUID_SLOT));
                }
            }
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }

    @Override
    public void onAddFancyInformationTooltip(List<Component> tooltips) {
        super.onAddFancyInformationTooltip(tooltips);
        tooltips.addAll(List.of(
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.0"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.1"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.2.fluid"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.3.fluid"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.4"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.5")));
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators((new IFancyConfiguratorButton.Toggle(GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture((double) 0.0F, (double) 0.5F, (double) 1.0F, (double) 0.5F), GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture((double) 0.0F, (double) 0.0F, (double) 1.0F, (double) 0.5F), this::isDistinct, (clickData, pressed) -> this.setDistinct(pressed))).setTooltipsSupplier((pressed) -> List.of(Component.translatable("gtceu.multiblock.universal.distinct").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)).append(Component.translatable(pressed ? "gtceu.multiblock.universal.distinct.yes" : "gtceu.multiblock.universal.distinct.no")))));
    }
}
