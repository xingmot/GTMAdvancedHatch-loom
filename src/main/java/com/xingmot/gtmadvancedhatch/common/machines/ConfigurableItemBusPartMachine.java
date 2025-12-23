package com.xingmot.gtmadvancedhatch.common.machines;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableItemStack;
import com.xingmot.gtmadvancedhatch.api.IMultiCapacity;
import com.xingmot.gtmadvancedhatch.api.gui.ConfigSlotWidget;
import com.xingmot.gtmadvancedhatch.api.gui.PhantomItemCapacityWidget;
import com.xingmot.gtmadvancedhatch.config.AHConfig;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper;
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
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigurableItemBusPartMachine extends TieredIOPartMachine implements IDistinctPart, IMachineLife, IInteractedMachine, IMultiCapacity {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigurableItemBusPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);
    @Persisted
    public int maxCapacity;
    public final int size;
    @Persisted
    private final NotifiableItemStackHandler inventory;
    protected @Nullable TickableSubscription autoIOSubs;
    protected @Nullable ISubscription inventorySubs;
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;
    @Getter
    @Persisted
    @DescSynced
    private boolean isDistinct = false;

    public ConfigurableItemBusPartMachine(IMachineBlockEntity holder, int tier, IO io, int size, Object... args) {
        super(holder, tier, io);
        this.size = size;
        this.inventory = this.createInventory(args);
        this.circuitInventory = this.createCircuitItemHandler(io);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public static int getSlotCapacity(int tier) {
        if (tier >= 3) return AHUtil.multiplyWithIntegerBounds(64, (1 << (2 * Math.min(13, tier - 1))));
        return 64 * (1 << tier);
    }

    // 因为物品槽的容量并不能持久化，因此这里需要手动载入
    public void initSlot() {
        if (this.inventory instanceof ConfigNotifiableItemStack cfStack) {
            cfStack.init();
        }
    }

    protected int getInventorySize() {
        return this.size;
    }

    protected NotifiableItemStackHandler createInventory(Object... args) {
        if (this.maxCapacity == 0) this.maxCapacity = getSlotCapacity(this.tier);
        return new ConfigNotifiableItemStack(this, this.getInventorySize(), this.maxCapacity, this.io);
    }

    public NotifiableItemStackHandler getInventory() {
        if (this.inventory instanceof ConfigNotifiableItemStack cTransfer) {
            return cTransfer;
        }
        return this.inventory;
    }

    @Override
    public void setCapacity(int index, int capacity) {
        if (this.inventory instanceof ConfigNotifiableItemStack cfStack) {
            cfStack.setCapacity(index, capacity);
        }
    }

    @Override
    public int getCapacity(int index) {
        if (this.inventory instanceof ConfigNotifiableItemStack cfStack) {
            return cfStack.getCapacity(index);
        }
        return -1;
    }

    public Widget createUIWidget() {
        int rowSize = 1;
        int colSize = 1;
        switch (this.size) {
            case 4 -> rowSize = 4;
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
        if (this.inventory instanceof ConfigNotifiableItemStack cTransfer) {
            for (int y = 0; y < colSize; ++y) {
                for (int x = 0; x < rowSize; ++x) {
                    container.addWidget(new ConfigSlotWidget(cTransfer, cTransfer.storage, index, 4 + x * 18, 4 + y * 36 + 18,
                            true, this.io.support(IO.IN)).setBackground(GuiTextures.SLOT));
                    container.addWidget((new PhantomItemCapacityWidget(cTransfer, cTransfer, this.getCapacity(index), this.maxCapacity, index++,
                            4 + x * 18, 4 + y * 36))
                            .setDrawHoverTips(true)
                            .setBackground(GuiTextures.SLOT_DARK));
                }
            }
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
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
                is.setHoverName(Component.translatable("gtmadvancedhatch.machine.configurable_item_bus.data_stick.name"));
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
            MutableComponent c = Component.empty();
            MutableComponent d = Component.empty();
            if (!LDLib.isRemote() && inventory instanceof ConfigNotifiableItemStack cTransfer) {
                c.append("过滤：[");
                d.append("容量：[");
                for (int i = 0; i < cTransfer.getSlots(); i++) {
                    c.append(cTransfer.getIndexLocked(i).getDisplayName());
                    d.append(String.valueOf(cTransfer.itemCapacity[i]));
                    if (i < cTransfer.getSlots() - 1) {
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
        }
        return InteractionResult.PASS;
    }

    protected CompoundTag writeConfigToTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag config = new CompoundTag();
        tag.put("ConfigurableHatch", config);
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
        if (tag.contains("ConfigurableHatch") && this.getInventory() instanceof ConfigNotifiableItemStack cfStack) {
            CompoundTag config = tag.getCompound("ConfigurableHatch");
            CompoundTag stacks = config.getCompound("stacks");
            CompoundTag stack_capacity = config.getCompound("stackCapacity");
            for (int i = 0; i < this.getInventory().getSize(); i++) {
                if (stacks.contains(String.valueOf(i)))
                    cfStack.setLocked(true, i, ItemStack.of(stacks.getCompound(String.valueOf(i))));
                else cfStack.setLocked(false, i, ItemStack.EMPTY);
                cfStack.newCapacity(i, stack_capacity.getInt(String.valueOf(i)));
            }
        }
        if (tag.contains("GhostCircuit")) {
            this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit")));
        }
        if (tag.contains("isDistinct")) {
            this.setDistinct(tag.getBoolean("isDistinct"));
        }
    }

    @Override
    public void onAddFancyInformationTooltip(List<Component> tooltips) {
        super.onAddFancyInformationTooltip(tooltips);
        tooltips.addAll(List.of(
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.0"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.1"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.2.item"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.3.item"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.4"),
                Component.translatable("gtmadvancedhatch.gui.configurable_hatch.tooltip.5")));
    }

    // region all copy
    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        if (args.length > 0) {
            Object var3 = args[0];
            if (var3 instanceof IO) {
                IO io = (IO) var3;
                if (io == IO.IN) {
                    return (new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)).setFilter(IntCircuitBehaviour::isIntegratedCircuit);
                }
            }
        }

        return new NotifiableItemStackHandler(this, 0, IO.NONE);
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        if (this.inventory instanceof ConfigNotifiableItemStack cfStack) {
            if (io == IO.IN) cfStack.newCapacity(this.maxCapacity);
            else cfStack.newCapacity(0);
        }
    }

    @Override
    public void onMachineRemoved() {
        this.clearInventory(this.getInventory().storage);
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            this.clearInventory(this.circuitInventory.storage);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            this.initSlot();
            serverLevel.getServer().tell(new TickTask(0, this::updateInventorySubscription));
        }
        getHandlerList().setDistinct(isDistinct);
        getHandlerList().setColor(getPaintingColor());
        inventorySubs = getInventory().addChangedListener(this::updateInventorySubscription);
    }

    public void onUnload() {
        super.onUnload();
        if (this.inventorySubs != null) {
            this.inventorySubs.unsubscribe();
            this.inventorySubs = null;
        }
    }

    public void setDistinct(boolean distinct) {
        isDistinct = (io != IO.OUT && distinct);
        getHandlerList().setDistinctAndNotify(isDistinct);
    }

    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        this.updateInventorySubscription();
    }

    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        this.updateInventorySubscription();
    }

    protected void updateInventorySubscription() {
        if (this.isWorkingEnabled() && (this.io == IO.OUT && !this.getInventory().isEmpty() || this.io == IO.IN) && ItemTransferHelper.getItemTransfer(this.getLevel(), this.getPos().relative(this.getFrontFacing()), this.getFrontFacing().getOpposite()) != null) {
            this.autoIOSubs = this.subscribeServerTick(this.autoIOSubs, this::autoIO);
        } else if (this.autoIOSubs != null) {
            this.autoIOSubs.unsubscribe();
            this.autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (this.getOffsetTimer() % AHConfig.INSTANCE.configurableFluidIOTick == 0L) {
            if (this.isWorkingEnabled()) {
                if (this.io == IO.OUT) {
                    this.getInventory().exportToNearby(this.getFrontFacing());
                } else if (this.io == IO.IN) {
                    this.getInventory().importFromNearby(this.getFrontFacing());
                }
            }
            this.updateInventorySubscription();
        }
    }

    public void setWorkingEnabled(boolean workingEnabled) {
        super.setWorkingEnabled(workingEnabled);
        this.updateInventorySubscription();
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        if (this.io == IO.IN) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(this.circuitInventory.storage));
        }
        configuratorPanel.attachConfigurators((new IFancyConfiguratorButton.Toggle(GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture((double) 0.0F, (double) 0.5F, (double) 1.0F, (double) 0.5F), GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture((double) 0.0F, (double) 0.0F, (double) 1.0F, (double) 0.5F), this::isDistinct, (clickData, pressed) -> this.setDistinct(pressed))).setTooltipsSupplier((pressed) -> List.of(Component.translatable("gtceu.multiblock.universal.distinct").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)).append(Component.translatable(pressed ? "gtceu.multiblock.universal.distinct.yes" : "gtceu.multiblock.universal.distinct.no")))));
    }
    // endregion
}
