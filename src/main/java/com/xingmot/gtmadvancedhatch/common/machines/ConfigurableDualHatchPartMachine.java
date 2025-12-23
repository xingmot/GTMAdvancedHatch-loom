package com.xingmot.gtmadvancedhatch.common.machines;

import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableFluidTank;
import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableItemStack;
import com.xingmot.gtmadvancedhatch.api.IMultiCapacity;
import com.xingmot.gtmadvancedhatch.api.gui.ConfigSlotWidget;
import com.xingmot.gtmadvancedhatch.api.gui.HugeTankWidget;
import com.xingmot.gtmadvancedhatch.api.gui.PhantomFluidCapacityWidget;
import com.xingmot.gtmadvancedhatch.api.gui.PhantomItemCapacityWidget;
import com.xingmot.gtmadvancedhatch.config.AHConfig;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.xingmot.gtmadvancedhatch.common.machines.ConfigurableFluidHatchPartMachine.getTankCapacity;

import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * 可配置总成
 * 索引偶数项为流体槽(0,2,4...)，奇数项为物品槽(1,3,5...)
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigurableDualHatchPartMachine extends ConfigurableItemBusPartMachine implements IDistinctPart, IMachineLife, IInteractedMachine, IMultiCapacity {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigurableDualHatchPartMachine.class, ConfigurableItemBusPartMachine.MANAGED_FIELD_HOLDER);
    @Persisted
    public final NotifiableFluidTank tank;
    @Persisted
    public int maxTankCapacity;
    private final int slots;
    protected @Nullable ISubscription tankSubs;
    private boolean hasFluidTransfer;
    private boolean hasItemTransfer;
    public int page = 0;
    public int pages;

    public ConfigurableDualHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int pageMax, int initialCapacity, Object... args) {
        super(holder, tier, io, 8 * pageMax, args);
        this.slots = 8 * pageMax;
        this.pages = pageMax;
        this.tank = this.createTank(initialCapacity, this.slots, args);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // 因为流体槽的容量并不能持久化，因此这里需要手动载入
    public void initTank() {
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            cfTank.init();
        }
    }

    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        if (this.maxTankCapacity == 0L)
            this.maxTankCapacity = getTankCapacity(initialCapacity, tier);
        ConfigNotifiableFluidTank fluidTank = new ConfigNotifiableFluidTank(this, slots, this.maxTankCapacity, this.io);
        fluidTank.setAllowSameFluids(true);
        return fluidTank;
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        super.onMachinePlaced(player, stack);
        if (tank instanceof ConfigNotifiableFluidTank cTank) {
            if (io == IO.IN) cTank.newCapacity(this.maxTankCapacity);
            else cTank.newCapacity(0);
        }
    }

    @Override
    public void setCapacity(int index, int capacity) {
        if (index % 2 == 0) {
            if (this.tank instanceof ConfigNotifiableFluidTank cfTank)
                cfTank.setCapacity(index / 2, capacity);
        } else super.setCapacity(index / 2, capacity);
    }

    @Override
    public int getCapacity(int index) {
        if (index % 2 == 0) {
            if (this.tank instanceof ConfigNotifiableFluidTank cfTank)
                return cfTank.getCapacity(index / 2);
            return -1;
        } else return super.getCapacity(index / 2);
    }

    @Override
    public void onLoad() {
        this.traits.forEach(MachineTrait::onMachineLoad);
        this.coverContainer.onLoad();
        Level var2 = this.getLevel();
        if (var2 instanceof ServerLevel serverLevel) {
            this.initSlot();
            this.initTank();
            serverLevel.getServer().tell(new TickTask(0, this::updateInventorySubscription));
        }

        this.inventorySubs = this.getInventory().addChangedListener(this::updateInventorySubscription);
        this.tankSubs = this.tank.addChangedListener(this::updateInventorySubscription);
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
                    this.updateInventorySubscription();
                    player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
                }
                return InteractionResult.sidedSuccess(this.isRemote());
            }
        } else if (is.is(Items.OAK_SIGN.asItem()) && player.isCreative()) {
            /* 橡木告示牌右击查看调试数据 */
            if (!LDLib.isRemote()) {
                if (tank instanceof ConfigNotifiableFluidTank cfTank) {
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
                }
                if (this.getInventory() instanceof ConfigNotifiableItemStack cfStack) {
                    MutableComponent c = Component.empty();
                    MutableComponent d = Component.empty();
                    c.append("物品过滤：[");
                    d.append("容量：[");
                    for (int i = 0; i < cfStack.getSlots(); i++) {
                        c.append(cfStack.getIndexLocked(i).getDisplayName());
                        d.append(String.valueOf(cfStack.itemCapacity[i]));
                        if (i < cfStack.getSlots() - 1) {
                            c.append(",");
                            d.append(",");
                        }
                    }
                    c.append("]");
                    d.append("]");
                    player.sendSystemMessage(c);
                    player.sendSystemMessage(d);
                }
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
        if (this.getInventory() instanceof ConfigNotifiableItemStack cfStack) {
            CompoundTag stacks = new CompoundTag();
            CompoundTag stack_capacity = new CompoundTag();
            config.put("stacks", stacks);
            config.put("stackCapacity", stack_capacity);
            for (int i = 0; i < this.getInventory().getSize(); i++) {
                if (!cfStack.getIndexLocked(i).isEmpty())
                    stacks.put(String.valueOf(i), cfStack.getIndexLocked(i).save(new CompoundTag()));
                stack_capacity.putLong(String.valueOf(i), cfStack.getCapacity(i));
            }
        }
        tag.putByte("GhostCircuit", (byte) IntCircuitBehaviour.getCircuitConfiguration(this.circuitInventory.getStackInSlot(0)));
        tag.putBoolean("isDistinct", isDistinct());
        return tag;
    }

    protected void readConfigFromTag(CompoundTag tag) {
        if (tag.contains("ConfigurableHatch")) {
            CompoundTag config = tag.getCompound("ConfigurableHatch");
            if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
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
            if (this.getInventory() instanceof ConfigNotifiableItemStack cfStack) {
                CompoundTag stacks = config.getCompound("stacks");
                CompoundTag stack_capacity = config.getCompound("stackCapacity");
                for (int i = 0; i < this.getInventory().getSize(); i++) {
                    if (stacks.contains(String.valueOf(i)))
                        cfStack.setLocked(true, i, ItemStack.of(stacks.getCompound(String.valueOf(i))));
                    else cfStack.setLocked(false, i, ItemStack.EMPTY);
                    cfStack.newCapacity(i, stack_capacity.getInt(String.valueOf(i)));
                }
            }
        }
        if (tag.contains("GhostCircuit")) {
            this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit")));
        }
        if (tag.contains("isDistinct")) {
            this.setDistinct(tag.getBoolean("isDistinct"));
        }
    }

    public Widget createUIWidget() {
        int rowSize = 8;
        int colSize = 2;
        WidgetGroup group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize * 2 + 24);
        WidgetGroup container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize * 2 + 8);
        ButtonWidget left = new ButtonWidget(9 * rowSize - 20 + 4, 18 * colSize * 2 + 13, 8, 8, GuiTextures.BUTTON_LEFT, clickData -> {
            pageUp(clickData);
            refreshUI(container, rowSize, colSize);
        });
        ButtonWidget right = new ButtonWidget(9 * rowSize + 20 + 4, 18 * colSize * 2 + 13, 8, 8, GuiTextures.BUTTON_RIGHT, clickData -> {
            pageDown(clickData);
            refreshUI(container, rowSize, colSize);
        });
        refreshUI(container, rowSize, colSize);
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        if (pages >= 2) {
            group.addWidget(new LabelWidget(9 * rowSize - 20 + 16, 18 * colSize * 2 + 13, () -> this.page + 1 + " / " + this.pages));
            group.addWidget(left);
            group.addWidget(right);
        }
        return group;
    }

    protected void pageUp(ClickData data) {
        if (this.page >= 1) {
            --this.page;
        }
    }

    protected void pageDown(ClickData data) {
        if (this.page < this.pages - 1) {
            ++this.page;
        }
    }

    protected void refreshUI(WidgetGroup container, int rowSize, int colSize) {
        container.clearAllWidgets();
        int index = 16 * page;
        if (this.tank instanceof ConfigNotifiableFluidTank cTank && this.getInventory() instanceof ConfigNotifiableItemStack cTransfer) {
            for (int x = 0; x < rowSize; ++x) { // 换一下顺序，这样就是竖着排索引
                for (int y = 0; y < colSize; ++y) {
                    if (index % 2 == 0) {
                        container.addWidget(new HugeTankWidget(cTank.getStorages()[index / 2], 4 + x * 18, 4 + y * 36 + 18,
                                true, this.io.support(IO.IN)).setBackground(GuiTextures.FLUID_SLOT));
                        container.addWidget((new PhantomFluidCapacityWidget(cTank, cTank, this.getCapacity(index), this.maxTankCapacity, index / 2,
                                4 + x * 18, 4 + y * 36, 18, 18)).setShowAmount(true)
                                .setDrawHoverTips(true)
                                .setBackground(GuiTextures.FLUID_SLOT));
                    } else {
                        container.addWidget(new ConfigSlotWidget(cTransfer, cTransfer.storage, index / 2, 4 + x * 18, 4 + y * 36 + 18,
                                true, this.io.support(IO.IN)).setBackground(GuiTextures.SLOT));
                        container.addWidget((new PhantomItemCapacityWidget(cTransfer, cTransfer, this.getCapacity(index), this.maxCapacity, index / 2,
                                4 + x * 18, 4 + y * 36))
                                .setDrawHoverTips(true)
                                .setBackground(GuiTextures.SLOT_DARK));
                    }
                    index++;
                }
            }
        }
    }

    @Override
    public void onAddFancyInformationTooltip(List<Component> tooltips) {
        this.getDefinition().getTooltipBuilder().accept(this.getDefinition().asStack(), tooltips);
        String mainKey = String.format("%s.machine.%s.tooltip", this.getDefinition().getId().getNamespace(), this.getDefinition().getId().getPath());
        if (LocalizationUtils.exist(mainKey)) {
            tooltips.add(0, Component.translatable(mainKey));
        }
        tooltips.addAll(List.of(
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.0"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.1"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.2.dual"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.3.dual"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.4"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.5")));
    }

    // region 样板代码
    @Override
    public void onUnload() {
        super.onUnload();
        if (this.tankSubs != null) {
            this.tankSubs.unsubscribe();
            this.tankSubs = null;
        }
    }

    @Override
    protected void updateInventorySubscription() {
        boolean canOutput = this.io == IO.OUT && (!this.tank.isEmpty() || !this.getInventory().isEmpty());
        Level level = this.getLevel();
        if (level != null) {
            this.hasItemTransfer = ItemTransferHelper.getItemTransfer(level, this.getPos().relative(this.getFrontFacing()), this.getFrontFacing().getOpposite()) != null;
            this.hasFluidTransfer = FluidTransferHelper.getFluidTransfer(level, this.getPos().relative(this.getFrontFacing()), this.getFrontFacing().getOpposite()) != null;
        } else {
            this.hasItemTransfer = false;
            this.hasFluidTransfer = false;
        }

        if (!this.isWorkingEnabled() || !canOutput && this.io != IO.IN || !this.hasItemTransfer && !this.hasFluidTransfer) {
            if (this.autoIOSubs != null) {
                this.autoIOSubs.unsubscribe();
                this.autoIOSubs = null;
            }
        } else {
            this.autoIOSubs = this.subscribeServerTick(this.autoIOSubs, this::autoIO);
        }
    }

    @Override
    protected void autoIO() {
        if (this.getOffsetTimer() % AHConfig.INSTANCE.configurableFluidIOTick == 0L) {
            if (this.isWorkingEnabled()) {
                if (this.io == IO.OUT) {
                    if (this.hasItemTransfer) {
                        this.getInventory().exportToNearby(this.getFrontFacing());
                    }

                    if (this.hasFluidTransfer) {
                        this.tank.exportToNearby(this.getFrontFacing());
                    }
                } else if (this.io == IO.IN) {
                    if (this.hasItemTransfer) {
                        this.getInventory().importFromNearby(this.getFrontFacing());
                    }

                    if (this.hasFluidTransfer) {
                        this.tank.importFromNearby(this.getFrontFacing());
                    }
                }
            }

            this.updateInventorySubscription();
        }
    }

    public boolean isDistinct() {
        return super.isDistinct() && this.tank.isDistinct();
    }

    public void setDistinct(boolean isDistinct) {
        super.setDistinct(isDistinct);
        this.tank.setDistinct(isDistinct);
    }
    // endregion
}
