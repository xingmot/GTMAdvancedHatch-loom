package com.xingmot.gtmadvancedhatch.api;

import cn.qiuye.gtmoremachine.api.misc.wireless.energy.WirelessEnergyContainer;
import cn.qiuye.gtmoremachine.common.machine.multiblock.part.WirelessEnergyHatchPartMachine;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NoConsumeNotifiableEnergyContainer extends NotifiableEnergyContainer {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(NoConsumeNotifiableEnergyContainer.class, NotifiableRecipeHandlerTrait.MANAGED_FIELD_HOLDER);

    private @Nullable WirelessEnergyContainer WirelessEnergyContainerCache;
    @Setter
    @Getter
    @Persisted
    public UUID owner_uuid;

    public NoConsumeNotifiableEnergyContainer(WirelessEnergyHatchPartMachine machine, long maxCapacity, long maxInputVoltage, long maxInputAmperage, long maxOutputVoltage, long maxOutputAmperage) {
        super(machine, maxCapacity, maxInputVoltage, maxInputAmperage, maxOutputVoltage, maxOutputAmperage);
        this.WirelessEnergyContainerCache = machine.getWirelessEnergyContainer();
    }

    public static NoConsumeNotifiableEnergyContainer emitterContainer(WirelessEnergyHatchPartMachine machine, long maxCapacity, long maxOutputVoltage, long maxOutputAmperage) {
        return new NoConsumeNotifiableEnergyContainer(machine, maxCapacity, 0L, 0L, maxOutputVoltage, maxOutputAmperage);
    }

    public static NoConsumeNotifiableEnergyContainer receiverContainer(WirelessEnergyHatchPartMachine machine, long maxCapacity, long maxInputVoltage, long maxInputAmperage) {
        return new NoConsumeNotifiableEnergyContainer(machine, maxCapacity, maxInputVoltage, maxInputAmperage, 0L, 0L);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // 把实际操作能量的部分改为操作电网
    @Override
    public List<EnergyStack> handleRecipeInner(IO io, GTRecipe recipe, List<EnergyStack> left, boolean simulate) {
        for (var it = left.listIterator(); it.hasNext();) {
            EnergyStack stack = it.next();
            if (stack.isEmpty()) {
                it.remove();
                continue;
            }

            long totalEU = stack.getTotalEU();
            long canTransfer = Math.min(totalEU, (io == IO.IN ? this.getEnergyStored() :
                    this.getEnergyCapacity() - this.getEnergyStored()));
            if (!simulate) {
                // invert the EU value if we're doing inputs (inputting *to the recipe* -> removing from handlers)
                if (this.WirelessEnergyContainerCache != null) {
                    canTransfer -= WirelessEnergyContainerCache.addEnergy(io == IO.IN ? -canTransfer : canTransfer,this.machine);
                }
                this.changeEnergy(io == IO.IN ? -canTransfer : canTransfer);
            }

            totalEU -= canTransfer;
            if (totalEU <= 0) {
                it.remove();
            } else {
                it.set(new EnergyStack(totalEU));
            }

        }

        return left.isEmpty() ? null : left;
    }
}
