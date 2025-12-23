package com.xingmot.gtmadvancedhatch.common.machines;

import com.xingmot.gtmadvancedhatch.api.MutableLimitAndFilterStackTransfer;
import com.xingmot.gtmadvancedhatch.api.gui.LockStackSlotWidget;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.xingmot.gtmadvancedhatch.common.data.MachinesConstants.*;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * 留存输出总线
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LockItemOutputBus extends TieredIOPartMachine implements IDistinctPart, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LockItemOutputBus.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler inventory;
    @Getter
    @Persisted
    private final NotifiableItemStackHandler outerInventory;
    @Nullable
    protected ISubscription inventorySubs;
    @Nullable
    protected ISubscription trueInventorySubs;
    @Nullable
    protected TickableSubscription autoIOSubs;

    public LockItemOutputBus(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, IO.OUT);
        // 创建两个同样大小的存储空间，用于隔离输出
        // 第一个IO参数表示作为机器配方的输入输出
        // 第二个IO参数，IO.OUT 表示能被外部物流存取，也能被ME存储总线识别，IO.IN则会阻止外部抽取和识别
        // inventory用于与管道或ME存储总线进行交互，数量为对应格子-1
        // outerInventory则是实际存储，玩家可以放入取出
        this.inventory = createInventory(IO.NONE, IO.OUT, args);
        this.outerInventory = createInventory(IO.OUT, IO.NONE, args);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected int getInventorySize() {
        return getLockItemOutputBusSlot(getTier());
    }

    public static int getLockItemOutputBusSlotLimit(int tier) {
        if (tier >= 3) return AHUtil.multiplyWithIntegerBounds(64, (1 << Math.min(13, tier - 2)));
        return 64;
    }

    protected NotifiableItemStackHandler createInventory(IO handlerIO, IO capIO, Object... args) {
        // 重写，不知道为什么不重写就用不了
        return new NotifiableItemStackHandler(this, getInventorySize(), handlerIO, capIO,
                integer -> new MutableLimitAndFilterStackTransfer(integer, getLockItemOutputBusSlotLimit(this.tier))) {

            @Override
            public boolean isEmpty() {
                boolean isEmpty = true;
                for (int i = 0; i < this.storage.getSlots(); ++i) {
                    if (!this.storage.getStackInSlot(i).isEmpty()) {
                        isEmpty = false;
                        break;
                    }
                }
                return isEmpty;
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateInventorySubscription));
            serverLevel.getServer().tell(new TickTask(0, this::updateOuterInventorySubscription));
        }
        inventorySubs = getInventory().addChangedListener(this::updateInventorySubscription);
        trueInventorySubs = getOuterInventory().addChangedListener(this::updateOuterInventorySubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (inventorySubs != null) {
            inventorySubs.unsubscribe();
            inventorySubs = null;
        }
        if (trueInventorySubs != null) {
            trueInventorySubs.unsubscribe();
            trueInventorySubs = null;
        }
    }

    // 挖了里面东西掉出来
    @Override
    public void onMachineRemoved() {
        clearInventory(getOuterInventory());
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
    }

    @Override
    public boolean isDistinct() {
        return getInventory().isDistinct();
    }

    @Override
    public void setDistinct(boolean isDistinct) {
        getInventory().setDistinct(isDistinct);
    }

    //////////////////////////////////////
    // ******** Auto IO 和 物品数量同步*********//
    //////////////////////////////////////

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoIO();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateAutoIO();
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            if (isWorkingEnabled()) {
                if (io == IO.OUT) {
                    getInventory().exportToNearby(getFrontFacing());
                } else if (io == IO.IN) {
                    getInventory().importFromNearby(getFrontFacing());
                } else if (io == IO.BOTH) {
                    getInventory().importFromNearby(getFrontFacing());
                    getInventory().exportToNearby(getFrontFacing().getOpposite());
                }
            }
            updateInventorySubscription();
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        super.setWorkingEnabled(workingEnabled);
        updateAutoIO();
    }

    protected void updateInventorySubscription() {
        // 内存变化，更新外存中物品数量逻辑
        List<String> changedSlots = new ArrayList<>();
        List<String> changedSlotsCount = new ArrayList<>();
        boolean changed = false;
        for (int index = 0; index < getInventory().getSlots(); index++) {
            ItemStack stack = getInventory().getStackInSlot(index);
            if (stack.isEmpty()) {
                if (!getOuterInventory().getStackInSlot(index).isEmpty() && getOuterInventory().getStackInSlot(index).getCount() > 1) {
                    getOuterInventory().getStackInSlot(index).setCount(1);
                    changed = true;

                    changedSlots.add(String.valueOf(index));
                    changedSlotsCount.add(String.valueOf(stack.getCount() - 1));
                }
            } else if (getOuterInventory().getStackInSlot(index).getCount() != stack.getCount() + 1) {
                getOuterInventory().getStackInSlot(index).setCount(stack.getCount() + 1);
                changed = true;

                changedSlots.add(String.valueOf(index));
                changedSlotsCount.add(String.valueOf(stack.getCount() - 1));
            }
        }
        updateAutoIO();
        if (changed) {
            // GTMAdvancedHatch.LOGGER.info(String.format("oldUpdateStorage: %d", oldUpdateStorage));
            // String s = String.format("内存的 [%s] 进行更新，分别是 [%s] 个", String.join(",", changedSlots), String.join(",",
            // changedSlotsCount));
            // GTMAdvancedHatch.LOGGER.info(s);
            getOuterInventory().notifyListeners();
        }
        // GTMAdvancedHatch.LOGGER.info(String.format("registed map: %s",
        // WailaClientRegistration.INSTANCE.tooltipCollectedCallback));
    }

    protected void updateAutoIO() {
        // auto io
        if (isWorkingEnabled() && (io == IO.OUT && !getInventory().isEmpty()) && ItemTransferHelperImpl.getItemTransfer(getLevel(), getPos().relative(getFrontFacing()), getFrontFacing().getOpposite()) != null) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    private void updateOuterInventorySubscription() {
        // 外存变化，更新内存中物品数量逻辑
        boolean changed = false;
        for (int index = 0; index < getInventory().getSlots(); index++) {
            ItemStack outer_stack = getOuterInventory().getStackInSlot(index);
            ItemStack stack = getInventory().getStackInSlot(index);
            // 外存格为空或数量为1，则将内存格清空
            if ((outer_stack.isEmpty() || outer_stack.getCount() == 1)) {
                // 内存格不为空时才执行清空操作
                if (!stack.isEmpty()) {
                    getInventory().getStackInSlot(index).setCount(0);
                    changed = true;
                }
                // （内存格与外存格物品相同且数量恰好是外存格数量-1）的情况外再执行复制减一操作
            } else if (!(stack.getItem() == outer_stack.getItem() && stack.getCount() == outer_stack.getCount() - 1)) {
                getInventory().setStackInSlot(index, outer_stack.copyWithCount(outer_stack.getCount() - 1));
                changed = true;
            }
        }
        if (changed) {
            getInventory().notifyListeners();
        }
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////
    @Override
    public Widget createUIWidget() {
        int rowSize = getLockItemOutputBusSlotRow(getTier());
        int colSize = getLockItemOutputBusSlotCol(getTier());
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);
        int index = 0;
        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                container.addWidget(new LockStackSlotWidget(getOuterInventory().storage, index++,
                        4 + x * 18, 4 + y * 18, true, true)
                        .setOccupiedTexture(GuiTextures.SLOT)
                        .setBackgroundTexture(new GuiTextureGroup(GuiTextures.SLOT, GuiTextures.CONFIG_ARROW_DARK))
                        // .setChangeListener(()-> onOuterSlotChanged(finalIndex))
                        .setIngredientIO(IngredientIO.OUTPUT));
                new LockStackSlotWidget(getInventory().storage, index,
                        4 + x * 18, 4 + y * 18, true, true)
                        .setIngredientIO(IngredientIO.OUTPUT).setVisible(false).setActive(false);
            }
        }
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }
}
