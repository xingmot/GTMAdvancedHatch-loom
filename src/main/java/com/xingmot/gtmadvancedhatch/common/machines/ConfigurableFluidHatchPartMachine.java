package com.xingmot.gtmadvancedhatch.common.machines;

import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableFluidTank;
import com.xingmot.gtmadvancedhatch.api.IConfigFluidTransfer;
import com.xingmot.gtmadvancedhatch.api.IMultiCapacity;
import com.xingmot.gtmadvancedhatch.api.gui.HugeTankWidget;
import com.xingmot.gtmadvancedhatch.api.gui.PhantomFluidCapacityWidget;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigurableFluidHatchPartMachine extends TieredIOPartMachine implements IMachineLife, IInteractedMachine, IMultiCapacity {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigurableFluidHatchPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);
    @Persisted
    public final NotifiableFluidTank tank;
    @Persisted
    public long maxCapacity;
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;
    private final int slots;
    protected @Nullable TickableSubscription autoIOSubs;
    protected @Nullable ISubscription tankSubs;

    public ConfigurableFluidHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, long initialCapacity, int slots, Object... args) {
        super(holder, tier, io);
        this.slots = slots;
        this.maxCapacity = getTankCapacity(initialCapacity, tier);
        this.tank = this.createTank(slots, args);
        this.tank.setAllowSameFluids(true);
        this.circuitInventory = this.createCircuitItemHandler(io);
    }

    public static long getTankCapacity(long initialCapacity, int tier) {
        if (tier >= 3) return AHUtil.multiplyWithBounds(initialCapacity, (1L << (4 * Math.min(13, tier - 1))));
        return initialCapacity * (1L << tier);
    }

    public void initTank() {
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank)
            cfTank.initTank();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected NotifiableFluidTank createTank(int slots, Object... args) {
        return new ConfigNotifiableFluidTank(this, slots, this.maxCapacity, this.io);
    }

    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        if (args.length > 0) {
            Object var3 = args[0];
            if (var3 instanceof IO io) {
                if (io == IO.IN) {
                    return (new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)).setFilter(IntCircuitBehaviour::isIntegratedCircuit);
                }
            }
        }

        return new NotifiableItemStackHandler(this, 0, IO.NONE);
    }

    @Override
    public void onMachineRemoved() {
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            this.clearInventory(this.circuitInventory.storage);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level var2 = this.getLevel();
        if (var2 instanceof ServerLevel serverLevel) {
            this.initTank();
            serverLevel.getServer()
                    .tell(new TickTask(0, this::updateTankSubscription));
        }

        this.tankSubs = this.tank.addChangedListener(this::updateTankSubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (this.tankSubs != null) {
            this.tankSubs.unsubscribe();
            this.tankSubs = null;
        }
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return InteractionResult.PASS;
        } else if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            // TODO复制配置
            return InteractionResult.SUCCESS;
        } else if (is.is(Items.OAK_SIGN.asItem()) && player.isCreative()) {
            /* 橡木告示牌右击查看调试数据 */
            MutableComponent c = Component.empty();
            if (LDLib.isClient() && !LDLib.isRemote() && tank instanceof ConfigNotifiableFluidTank cfTank) {
                c.append("过滤：[");
                for (int i = 0; i < this.tank.getTanks(); i++) {
                    c.append(cfTank.getLockedFluids()[i].getFluid()
                            .getDisplayName());
                    if (i < this.tank.getTanks() - 1)
                        c.append(",");
                }
                c.append("]");

                player.sendSystemMessage(c);
            }
            return InteractionResult.SUCCESS;
        } else if (player.isCreative() && this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            if (LDLib.isClient()) {
                if (ToolHelper.isTool(is, GTToolType.SCREWDRIVER)) {
                    this.tank.setAllowSameFluids(!cfTank.getAllowSameFluids());
                    Component enable = cfTank.getAllowSameFluids() ? Component.translatable("gtmadvancedhatch.gui.universe.yes") : Component.translatable("gtmadvancedhatch.gui.universe.no");
                    player.sendSystemMessage(Component.translatable("gtmadvancedhatch.machine.configurable_fluid_hatch.screwdriver.tooltip").append(enable));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        this.updateTankSubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        this.updateTankSubscription();
    }

    protected void updateTankSubscription() {
        if (this.isWorkingEnabled() && (this.io == IO.OUT && !this.tank.isEmpty() || this.io == IO.IN) &&
                FluidTransferHelper.getFluidTransfer(this.getLevel(), this.getPos().relative(this.getFrontFacing()),
                        this.getFrontFacing().getOpposite()) != null) {
            this.autoIOSubs = this.subscribeServerTick(this.autoIOSubs, this::autoIO);
        } else if (this.autoIOSubs != null) {
            this.autoIOSubs.unsubscribe();
            this.autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (this.getOffsetTimer() % 5L == 0L) {
            if (this.isWorkingEnabled()) {
                if (this.io == IO.OUT) {
                    this.tank.exportToNearby(this.getFrontFacing());
                } else if (this.io == IO.IN) {
                    this.tank.importFromNearby(this.getFrontFacing());
                }
            }

            this.updateTankSubscription();
        }
    }

    @Override
    public long[] getTankCapacity() {
        if (this.tank instanceof IConfigFluidTransfer ctank) {
            return ctank.getTankCapacity();
        }
        return null;
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        super.setWorkingEnabled(workingEnabled);
        this.updateTankSubscription();
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        if (this.io == IO.IN) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(this.circuitInventory.storage));
        }
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
        if (this.tank instanceof ConfigNotifiableFluidTank ctank) {
            for (int y = 0; y < colSize; ++y) {
                for (int x = 0; x < rowSize; ++x) {
                    int finalIndex = index;
                    container.addWidget(new HugeTankWidget(ctank.getStorages()[index], 4 + x * 18, 4 + y * 36 + 18,
                            true, this.io.support(IO.IN)).setBackground(GuiTextures.FLUID_SLOT));
                    container.addWidget((new PhantomFluidCapacityWidget(ctank, this.getTankCapacity()[finalIndex], this.maxCapacity, ctank.getLockedFluids()[index], index++,
                            4 + x * 18, 4 + y * 36, 18, 18,
                            () -> ctank.getLockedFluids()[finalIndex].getFluid(), (f) -> {
                                if (f != null && !f.isEmpty()) {
                                    if (ctank.getFluidInTank(finalIndex).isEmpty() || ctank.getFluidInTank(finalIndex).getFluid() == f.getFluid()) {
                                        ctank.setLocked(true, finalIndex, f.copy());
                                    }
                                } else {
                                    ctank.setLocked(false, finalIndex);
                                }
                            })).setShowAmount(true)
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
    public void onAddFancyInformationTooltip(@NotNull List<Component> tooltips) {
        super.onAddFancyInformationTooltip(tooltips);
        tooltips.addAll(Arrays.stream(
                LangHandler.getMultiLang("gtmadvancedhatch.gui.configurable_fluid_hatch.tooltip").toArray(new MutableComponent[0]))
                .toList());
    }
}
