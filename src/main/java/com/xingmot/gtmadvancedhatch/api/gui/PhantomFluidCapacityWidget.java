package com.xingmot.gtmadvancedhatch.api.gui;

import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.xingmot.gtmadvancedhatch.api.IConfigTransfer;
import com.xingmot.gtmadvancedhatch.common.data.MachinesConstants;
import com.xingmot.gtmadvancedhatch.util.AHFormattingUtil;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.gui.widget.ScrollablePhantomFluidWidget;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

// TODO 翻页

/**
 * 可设置数量的虚拟流体槽
 */
public class PhantomFluidCapacityWidget extends ScrollablePhantomFluidWidget implements IPhantomAmountWidget<FluidStack> {

    private final Supplier<FluidStack> phantomFluidGetter;
    private final Consumer<FluidStack> phantomFluidSetter;
    FluidStack currentJEIRenderedIngredient;
    @Setter
    private static IGuiTexture lockTexture = new GuiTextureGroup(new ResourceTexture("gtceu:textures/gui/widget/button_lock.png").scale(0.65F));
    @Setter
    protected int maxCapacity;
    /** 锁定滚动，每次打开gui时会自动锁定全部槽位 */
    @Setter
    protected boolean lockScroll = true;
    /** 截断警告，即将发生流体截断时会暂时锁定流体槽并设为true，解锁时设为false */
    @Setter
    protected boolean isWarned = false;

    @Getter
    private IConfigTransfer<CustomFluidTank[], CustomFluidTank, FluidStack> icFluidTank;

    public PhantomFluidCapacityWidget(@Nonnull IConfigTransfer<CustomFluidTank[], CustomFluidTank, FluidStack> cTransfer, @Nonnull IFluidHandler fluidTank, int currentCapacity, int maxCapacity, int tank, int x, int y, int width, int height) {
        this(cTransfer, currentCapacity, maxCapacity, tank, x, y, width, height,
                () -> cTransfer.getLockedRef()[tank].getFluidInTank(0),
                (f) -> {
                    if (f != null && !f.isEmpty()) {
                        if (fluidTank.getFluidInTank(tank).isEmpty() || fluidTank.getFluidInTank(tank).getFluid() == f.getFluid()) {
                            cTransfer.setLocked(true, tank, f.copy());
                        }
                    } else {
                        cTransfer.setLocked(false, tank);
                    }
                });
    }

    public PhantomFluidCapacityWidget(@Nonnull IConfigTransfer<CustomFluidTank[], CustomFluidTank, FluidStack> cTransfer, int currentCapacity, int maxCapacity, int tank, int x, int y, int width, int height, Supplier<FluidStack> phantomFluidGetter, Consumer<FluidStack> phantomFluidSetter) {
        super(cTransfer.getIndexLocked(tank), tank, x, y, width, height, phantomFluidGetter, phantomFluidSetter);
        this.icFluidTank = cTransfer;
        this.lastTankCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
        this.phantomFluidGetter = phantomFluidGetter;
        this.phantomFluidSetter = phantomFluidSetter;
    }

    @Override
    public int getAmount(int slot) {
        return this.lastTankCapacity;
    }

    @Override
    public void setAmount(int slot, int capacity) {
        this.lastTankCapacity = capacity;
    }

    @Override
    public FluidStack getPhantomStack(int slot) {
        return this.lastPhantomStack;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (!this.isMouseOverElement(mouseX, mouseY) || lockScroll) {
            return false;
        } else {
            long newCapacity = this.getNewCapacity(wheelDelta > (double) 0.0F ? 1 : -1);
            this.writeClientAction(MachinesConstants.SCROLL_ACTION_ID, (buf) -> buf.writeLong(newCapacity));
            return true;
        }
    }

    private long getNewCapacity(int wheel) {
        if (GTUtil.isCtrlDown()) {
            long multi = 2;
            if (GTUtil.isShiftDown()) {
                multi *= 2;
            }
            if (GTUtil.isAltDown()) {
                multi *= 4;
            }
            return wheel > 0 ? Math.min(maxCapacity, AHUtil.multiplyWithLongBounds(this.getAmount(this.tank), multi)) : AHUtil.divWithLongBounds(this.getAmount(this.tank), multi);
        }
        long add = wheel;
        if (GTUtil.isShiftDown()) {
            add *= 10;
            if (GTUtil.isAltDown()) {
                add *= 10;
            }
        }

        if (!GTUtil.isAltDown()) {
            add *= 1000;
        }
        return Math.min(maxCapacity, AHUtil.addWithLongBounds(this.getAmount(this.tank), add));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if (isClientSideWidget) {
                handlePhantomClick();
            } else {
                if (button == 0)
                    writeClientAction(MachinesConstants.MOUSE_LEFT_CLICK_ACTION_ID, buffer -> {});
                else if (button == 1)
                    writeClientAction(MachinesConstants.MOUSE_RIGHT_CLICK_ACTION_ID, buffer -> {});
                else if (button == 2)
                    writeClientAction(MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID, buffer -> buffer.writeBoolean(isCtrlDown()));
            }
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case MachinesConstants.SCROLL_ACTION_ID -> this.handleScrollAction(buffer.readInt());
            case MachinesConstants.MOUSE_LEFT_CLICK_ACTION_ID -> this.handlePhantomClick();
            case MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID -> this.handleMiddleClick(buffer.readBoolean());
            case 2 -> super.handleClientAction(2, buffer); // 不要动父类的流体设置方法
            default -> super.handleClientAction(id, buffer);
        }

        this.detectAndSendChanges();
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private void handleScrollAction(int newAmount) {
        if (this.getFluidTank() != null)
            if (icFluidTank.isTruncate(this.tank, newAmount) && !isWarned) {
                setWarnedAndWrite(true);
                reverseLockScroll();
            } else {
                setWarnedAndWrite(false);
                icFluidTank.newCapacity(this.tank, Math.max(newAmount, 0));
            }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private void handleMiddleClick(boolean isCtrlDown) {
        if (!isCtrlDown || lockScroll) {
            reverseLockScroll();
        } else {
            if (this.getFluidTank() != null)
                if (icFluidTank.isTruncate(this.tank, 0) && !isWarned) {
                    setWarnedAndWrite(true);
                    reverseLockScroll();
                } else {
                    setWarnedAndWrite(false);
                    icFluidTank.newCapacity(this.tank, 0);
                }
        }
    }

    private void setWarnedAndWrite(boolean setTo) {
        this.isWarned = setTo;
        writeUpdateInfo(7, buf -> buf.writeBoolean(setTo));
    }

    private void reverseLockScroll() {
        this.lockScroll = !lockScroll;
        writeUpdateInfo(6, buf -> {
            buf.writeBoolean(lockScroll);
        });
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case 6 -> this.lockScroll = buffer.readBoolean();
            case 7 -> this.isWarned = buffer.readBoolean();
            default -> super.readUpdateInfo(id, buffer);
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private void handlePhantomClick() {
        ItemStack itemStack = this.gui.getModularUIContainer().getCarried().copy();
        if (!itemStack.isEmpty()) {
            itemStack.setCount(1);
            IFluidTransfer handler = FluidTransferHelper.getFluidTransfer(this.gui.entityPlayer, this.gui.getModularUIContainer());
            if (handler != null) {
                FluidStack resultFluid = FluidHelperImpl.toFluidStack(handler.drain(2147483647L, true));
                if (this.phantomFluidSetter != null) {
                    this.phantomFluidSetter.accept(resultFluid);
                }
            }
        } else if (this.phantomFluidSetter != null) {
            this.phantomFluidSetter.accept(FluidStack.EMPTY);
        }
    }

    /** 写的石山代码，不知道在写什么 */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.drawBackgroundTexture(graphics, 0, 0);
        if (isClientSideWidget && fluidTank != null) {
            FluidStack fluidStack = fluidTank.getFluidInTank(tank);
            int capacity = fluidTank.getTankCapacity(tank);
            if (capacity != getAmount(-1)) {
                setAmount(-1, capacity);
            }
            if (!fluidStack.isFluidEqual(lastFluidInTank)) {
                this.lastFluidInTank = fluidStack.copy();
            } else if (fluidStack.getAmount() != lastFluidInTank.getAmount()) {
                this.lastFluidInTank.setAmount(fluidStack.getAmount());
            }
        }
        Position pos = getPosition();
        Size size = getSize();
        RenderSystem.disableBlend();
        if (this.lastFluidInTank != null && !lastFluidInTank.isEmpty()) {
            double progress = lastFluidInTank.getAmount() * 1.0 / Math.max(Math.max(lastFluidInTank.getAmount(), getAmount(-1)), 1);
            float drawnU = (float) fillDirection.getDrawnU(progress);
            float drawnV = (float) fillDirection.getDrawnV(progress);
            float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
            float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
            int width = size.width - 2;
            int height = size.height - 2;
            int x = pos.x + 1;
            int y = pos.y + 1;
            DrawerHelper.drawFluidForGui(graphics, FluidHelperImpl.toFluidStack(lastFluidInTank), lastFluidInTank.getAmount(),
                    (int) (x + drawnU * width), (int) (y + drawnV * height), ((int) (width * drawnWidth)), ((int) (height * drawnHeight)));
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (showAmount) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.5F, 0.5F, 1);
            // String s = "/" + TextFormattingUtil.formatLongToCompactStringBuckets(lastTankCapacity, 3) + "B";
            String s = "/" + AHFormattingUtil.formatLongBucketsCompactStringBuckets(getAmount(-1)) + "B";
            Font fontRenderer = Minecraft.getInstance().font;
            graphics.drawString(fontRenderer, s,
                    (int) ((pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 21),
                    (int) ((pos.y + (size.height / 3f) + 6) * 2), 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        if (lockScroll && lockTexture != null) {
            lockTexture.draw(graphics, mouseX, mouseY, pos.x + 6, pos.y - 6, size.width, size.height);
        }
        if (isWarned) {
            graphics.pose().pushPose();
            float tickBy20 = AHUtil.tickAndWaitByNumberCycle(partialTicks, 10, 10);
            graphics.renderOutline(pos.x, pos.y, size.width, size.height, ColorUtils.color((int) (255 * tickBy20), 255, 0, 0));
            graphics.pose().popPose();

            if (MachinesConstants.debugGUI) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1);
                Font fontRenderer = Minecraft.getInstance().font;
                String s_tickBy20 = new DecimalFormat("#.##").format(tickBy20);
                graphics.drawString(fontRenderer, s_tickBy20, (int) ((pos.x + (size.width / 3.0F)) * 2.0F - (float) fontRenderer.width(s_tickBy20) + 21.0F),
                        (int) (pos.y + (size.height / 3.0F) + 6.0F) * 2.0F, ColorUtils.color(255, 255, 0, 0), true);
                graphics.pose().popPose();
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /* 石山警告 */
    @Override
    public List<Component> getFullTooltipTexts() {
        // 缩略显示数值的阈值
        long min = 128000;
        ArrayList<Component> tooltips = new ArrayList<>();
        FluidStack fluidStack = this.currentJEIRenderedIngredient != null ? this.currentJEIRenderedIngredient : this.lastFluidInTank;
        if (fluidStack != null && !fluidStack.isEmpty()) {
            tooltips.add(fluidStack.getDisplayName());
            if (!isShiftDown() && (fluidStack.getAmount() > min || getAmount(-1) > min)) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", AHFormattingUtil.formatLongBucketsToShort(fluidStack.getAmount(), min), AHFormattingUtil.formatLongBucketsToShort(getAmount(-1), min)));
                if (!Platform.isForge()) {
                    tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(fluidStack.getAmount() * 1000L / FluidHelper.getBucket(), getAmount(-1) * 1000L / FluidHelper.getBucket())).append(" mB"));
                }
                TooltipsHandler.appendFluidTooltips(fluidStack, tooltips::add, null);
                if (getAmount(-1) != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_phantom_capacity.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", fluidStack.getAmount(), getAmount(-1)).append(" " + FluidHelper.getUnit()));
                if (!Platform.isForge()) {
                    tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(fluidStack.getAmount() * 1000L / FluidHelper.getBucket(), getAmount(-1) * 1000L / FluidHelper.getBucket())).append(" mB"));
                }
                TooltipsHandler.appendFluidTooltips(fluidStack, tooltips::add, null);
                if (!isShiftDown() && getAmount(-1) != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_phantom_capacity.tooltips").withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltips.add(Component.translatable("ldlib.fluid.empty"));
            if (!isShiftDown() && getAmount(-1) > min) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, AHFormattingUtil.formatLongBucketsToShort(getAmount(-1), min)));
                if (getAmount(-1) != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_phantom_capacity.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, getAmount(-1))
                        .append(" " + FluidHelper.getUnit()));
                if (getAmount(-1) != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_phantom_capacity.tooltips").withStyle(ChatFormatting.GOLD));
            }
            if (!Platform.isForge()) {
                tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(0, getAmount(-1) * 1000L / FluidHelper.getBucket()))
                        .append(" mB"));
            }
        }

        tooltips.addAll(this.getTooltipTexts());
        return tooltips;
    }
}
