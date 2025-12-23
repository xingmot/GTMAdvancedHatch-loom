package com.xingmot.gtmadvancedhatch.api;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import lombok.val;
import net.minecraft.MethodsReturnNonnullByDefault;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 容量可变，可单独设置过滤的流体存储
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigNotifiableFluidTank extends NotifiableFluidTank implements IConfigTransfer<CustomFluidTank[], CustomFluidTank, FluidStack> {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigNotifiableFluidTank.class, NotifiableFluidTank.MANAGED_FIELD_HOLDER);
    @Persisted
    @DescSynced
    protected CustomFluidTank[] lockedFluids;
    @Persisted
    public final long maxCapacity;
    @Persisted
    @DescSynced
    public int[] tankCapacity;
    // TODO 锁空功能
    @Persisted
    protected boolean isLockedEmptySlot;

    public ConfigNotifiableFluidTank(MetaMachine machine, int slots, int maxCapacity, IO io, IO capabilityIO) {
        super(machine, slots, maxCapacity, io, capabilityIO);
        this.lockedFluids = new CustomFluidTank[slots];
        this.tankCapacity = new int[slots];
        this.maxCapacity = maxCapacity;
        Arrays.fill(this.tankCapacity, maxCapacity);
        for (int i = 0; i < slots; i++) {
            lockedFluids[i] = new CustomFluidTank(tankCapacity[i]);
        }
    }

    public ConfigNotifiableFluidTank(MetaMachine machine, int slots, int maxCapacity, IO io) {
        this(machine, slots, maxCapacity, io, io);
    }

    public void setAllowSameFluids(boolean allowSameFluids) {
        this.allowSameFluids = allowSameFluids;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void setCapacity(int index, int capacity) {
        this.tankCapacity[index] = capacity;
    }

    @Override
    public int getCapacity(int index) {
        return tankCapacity[index];
    }

    /**
     * 外界使用时请调用下面这两个
     */
    @Override
    public void newCapacity(int capacity) {
        resetBasicInfo(capacity);
    }

    @Override
    public void newCapacity(int tank, int capacity) {
        resetOneBasicInfo(tank, capacity);
    }

    // 需要在持久化数据载入完成后调用
    public void init() {
        for (int i = 0; i < this.tankCapacity.length; i++) {
            this.resetOneBasicInfo(i, getCapacity(i));
            int finalI = i;
            if (isLocked(i))
                this.setFilter(i, (stack) -> stack.isFluidEqual(this.lockedFluids[finalI].getFluid()));
            else
                this.setFilter(i, (stack) -> !isLockedEmptySlot);
        }
    }

    @Override
    public CustomFluidTank[] getLockedRef() {
        return this.lockedFluids;
    }

    @Override
    public CustomFluidTank getIndexLocked(int index) {
        return this.lockedFluids[index];
    }

    /**
     * 设置全部格子的流体容量
     *
     * @param capacity 容量
     */
    public void resetBasicInfo(int capacity) {
        Arrays.fill(this.tankCapacity, capacity);
        for (int i = 0; i < this.getSize(); i++) {
            resetOneBasicInfo(i, capacity);
        }
    }

    /**
     * 单独设置某一格的流体容量
     *
     * @param index    格子索引
     * @param capacity 容量，最小为0桶，最大为long.max
     */
    public void resetOneBasicInfo(int index, int capacity) {
        if (machine instanceof IMultiCapacity) {
            capacity = Math.max(capacity, 0);
            // 容量小于流体量时进行截断
            if (isTruncate(index, capacity))
                this.getFluidInTank(index).setAmount(capacity);
            this.tankCapacity[index] = capacity; // 持久化用的
            this.getStorages()[index].setCapacity(capacity); // 实际操作格子容量，HugeTankWidget会从里拿
            lockedFluids[index].setCapacity(capacity); // 虚拟格容量从这里拿
        }
    }

    @Override
    public boolean isTruncate(int index, int capacity) {
        return !this.getFluidInTank(index).isEmpty() && capacity < this.getFluidInTank(index).getAmount();
    }

    @Override
    @Nullable
    public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left, boolean simulate) {
        return handleIngredient(io, recipe, left, simulate, this.getStorages());
    }

    // Copied from gtceu 1.6.4 mainly coded by kross
    @Nullable
    public List<FluidIngredient> handleIngredient(IO io, GTRecipe recipe, List<FluidIngredient> left, boolean simulate, CustomFluidTank[] storages) {
        if (io != handlerIO) return left;
        if (io != IO.IN && io != IO.OUT) return left.isEmpty() ? null : left;
        FluidAction fluidAction;
        if(simulate) fluidAction = FluidAction.SIMULATE;
        else fluidAction = FluidAction.EXECUTE;

        FluidStack[] visited = new FluidStack[storages.length];
        for (var it = left.iterator(); it.hasNext();) {
            var ingredient = it.next();
            if (ingredient.isEmpty()) {
                it.remove();
                continue;
            }

            var fluids = ingredient.getStacks();
            if (fluids.length == 0 || fluids[0].isEmpty()) {
                it.remove();
                continue;
            }

            if (io == IO.OUT && !allowSameFluids) {
                CustomFluidTank existing = null;
                for (var storage : storages) {
                    if (!storage.getFluid().isEmpty() && storage.getFluid().isFluidEqual(fluids[0])) {
                        existing = storage;
                        break;
                    }
                }
                if (existing != null) {
                    FluidStack output = fluids[0];
                    int filled = existing.fill(output, fluidAction);
                    ingredient.setAmount(ingredient.getAmount() - filled);
                    if (ingredient.getAmount() <= 0) {
                        it.remove();
                    }
                    // Continue to next ingredient regardless of if we filled this ingredient completely
                    continue;
                }
            }

            for (int tank = 0; tank < storages.length; ++tank) {
                FluidStack fluidStack = fluids[0].copy();
                if (storages[tank].getCapacity() == 0) continue;
                if (!storages[tank].isFluidValid(fluidStack)) continue;
                // GTMAdvancedHatch.LOGGER.info("{},capacity{}",
                // fluidStack.getDisplayName().getString(),storages[tank].getCapacity());
                @NotNull
                FluidStack stored = getFluidInTank(tank);
                int amount = (visited[tank] == null ? stored.getAmount() : visited[tank].getAmount());
                if (io == IO.IN) {
                    if (amount == 0) continue;
                    if ((visited[tank] == null && ingredient.test(stored)) || ingredient.test(visited[tank])) {
                        var drained = storages[tank].drain(ingredient.getAmount(), fluidAction);
                        if (drained.getAmount() > 0) {
                            visited[tank] = drained.copy();
                            visited[tank].setAmount(amount - drained.getAmount());
                            ingredient.setAmount(ingredient.getAmount() - drained.getAmount());
                        }
                    }
                } else { // IO.OUT && No tank already has this fluidStack
                    fluidStack.setAmount(ingredient.getAmount());
                    if (visited[tank] == null || visited[tank].isFluidEqual(fluidStack)) {
                        int filled = storages[tank].fill(fluidStack, fluidAction);
                        if (filled > 0) {
                            visited[tank] = fluidStack.copy();
                            visited[tank].setAmount(filled);
                            ingredient.setAmount(ingredient.getAmount() - filled);
                            if (!allowSameFluids) {
                                if (ingredient.getAmount() <= 0) it.remove();
                                break;
                            }
                        }
                    }
                }

                if (ingredient.getAmount() <= 0) {
                    it.remove();
                    break;
                }
            }
        }
        return left.isEmpty() ? null : left;
    }

    @Override
    public boolean test(FluidIngredient ingredient) {
        if (ingredient.isEmpty()) return false;
        for (int i = 0; i < this.getSize(); i++) {
            if (!isLocked(i) || ingredient.test(this.getFluidInTank(i)))
                return true;
        }
        return false;
    }

    @Override
    public int getPriority() {
        return isLockedFluidsEmpty() ? 1073741823 - this.getTanks() : super.getPriority();
    }

    public boolean isLockedFluidsEmpty() {
        for (CustomFluidTank fluid : this.lockedFluids) {
            if (fluid != null && !fluid.getFluid().isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public CustomFluidTank getLockedFluid() {
        return this.lockedFluids[0];
    }

    @Override
    public boolean isLocked() {
        return this.isLocked(0);
    }

    @Override
    public void setLocked(boolean locked) {
        this.setLocked(locked, 0);
    }

    @Override
    public void setLocked(boolean locked, FluidStack fluidStack) {
        this.setLocked(locked, 0, fluidStack);
    }

    /** 这个方法的index不会超出大小 */
    public boolean isLocked(int index) {
        return !this.lockedFluids[index].getFluid().isEmpty();
    }

    public void setLocked(boolean locked, int tank) {
        if (tank < this.getSize() && this.isLocked(tank) != locked) {
            FluidStack fluidStack = this.getStorages()[tank].getFluid();
            setLocked(locked, tank, fluidStack);
        }
    }

    public void setLocked(boolean locked, int tank, FluidStack fluidStack) {
        if (tank < this.getSize() && this.isLocked(tank) != locked) {
            if (locked && !fluidStack.isEmpty()) {
                this.lockedFluids[tank].setFluid(fluidStack.copy());
                this.lockedFluids[tank].getFluid().setAmount(FluidType.BUCKET_VOLUME);
                this.setFilter(tank, (stack) -> stack.isFluidEqual(this.lockedFluids[tank].getFluid()));
                this.onContentsChanged();
            } else {
                this.lockedFluids[tank].setFluid(FluidStack.EMPTY);
                this.setFilter(tank, (stack) -> !isLockedEmptySlot);
                this.onContentsChanged();
            }

        }
    }

    public ConfigNotifiableFluidTank setFilter(int tank, Predicate<FluidStack> filter) {
        this.getStorages()[tank].setValidator(filter);
        return this;
    }

    public boolean getAllowSameFluids() {
        return this.allowSameFluids;
    }
}
