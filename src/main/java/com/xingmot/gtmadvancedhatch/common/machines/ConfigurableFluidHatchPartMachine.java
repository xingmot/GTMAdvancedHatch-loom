package com.xingmot.gtmadvancedhatch.common.machines;

import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableFluidTank;
import com.xingmot.gtmadvancedhatch.api.IConfigFluidTransfer;
import com.xingmot.gtmadvancedhatch.api.IMultiCapacity;
import com.xingmot.gtmadvancedhatch.api.gui.HugeTankWidget;
import com.xingmot.gtmadvancedhatch.api.gui.PhantomFluidCapacityWidget;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigurableFluidHatchPartMachine extends FluidHatchPartMachine implements IMachineLife, IInteractedMachine, IMultiCapacity, IMultiPart {
    // 想兼容gtlcore就必须继承FluidHatchPartMachine，否则会不识别配方

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigurableFluidHatchPartMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);
    @Persisted
    public long maxCapacity;
    private final int slots;
    protected @Nullable ISubscription tankSubs;

    public ConfigurableFluidHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, long initialCapacity, int slots, Object... args) {
        super(holder, tier, io, initialCapacity, slots, args);
        this.slots = slots;
    }

    public static long getTankCapacity(long initialCapacity, int tier) {
        if (tier >= 3) return AHUtil.multiplyWithBounds(initialCapacity, (1L << (4 * Math.min(13, tier - 1))));
        return initialCapacity * (1L << tier);
    }

    public void initTank() {
        if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            cfTank.initTank();
        }
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableFluidTank createTank(long initialCapacity, int slots, Object... args) {
        if (this.maxCapacity == 0L)
            this.maxCapacity = getTankCapacity(initialCapacity, tier);
        ConfigNotifiableFluidTank fluidTank = new ConfigNotifiableFluidTank(this, slots, this.maxCapacity, this.io);
        fluidTank.setAllowSameFluids(true);
        return fluidTank;
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        if (tank instanceof ConfigNotifiableFluidTank cTank) {
            if (io == IO.IN) cTank.newTankCapacity(this.maxCapacity);
            else cTank.newTankCapacity(0L);
        }
    }

    @Override
    public void onLoad() {
        Level var2 = this.getLevel();
        if (var2 instanceof ServerLevel serverLevel) {
            this.initTank(); // 持久化恢复流体储存的容量数据
            serverLevel.getServer()
                    .tell(new TickTask(0, this::updateTankSubscription));
        }

        this.tankSubs = this.tank.addChangedListener(this::updateTankSubscription);
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
        } else if (this.tank instanceof ConfigNotifiableFluidTank cfTank) {
            if (LDLib.isClient()) {
                if (ToolHelper.isTool(is, GTToolType.SCREWDRIVER)) {
                    this.tank.setAllowSameFluids(!cfTank.getAllowSameFluids());
                    Component enable = cfTank.getAllowSameFluids() ? Component.translatable("gtmadvancedhatch.gui.universe.no") : Component.translatable("gtmadvancedhatch.gui.universe.yes");
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
    public long[] getTankCapacity() {
        if (this.tank instanceof IConfigFluidTransfer ctank) {
            return ctank.getTankCapacity();
        }
        return null;
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
