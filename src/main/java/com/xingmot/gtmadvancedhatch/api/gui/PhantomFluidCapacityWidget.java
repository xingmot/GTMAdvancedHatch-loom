package com.xingmot.gtmadvancedhatch.api.gui;

import com.xingmot.gtmadvancedhatch.api.IConfigFluidTransfer;
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
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
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

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    protected Long maxCapacity;
    /** 锁定滚动，每次打开gui时会自动锁定全部槽位 */
    @Setter
    protected boolean lockScrool = true;
    /** 截断警告，即将发生流体截断时会暂时锁定流体槽并设为true，解锁时设为false */
    @Setter
    protected boolean isWarned = false;

    @Getter
    private IConfigFluidTransfer icFluidTank;

    public PhantomFluidCapacityWidget(@Nullable IConfigFluidTransfer icFluidTank, long currentCapacity, long maxCapacity, @Nullable IFluidTransfer fluidTank, int tank, int x, int y, int width, int height, Supplier<FluidStack> phantomFluidGetter, Consumer<FluidStack> phantomFluidSetter) {
        super(fluidTank, tank, x, y, width, height, phantomFluidGetter, phantomFluidSetter);
        this.icFluidTank = icFluidTank;
        this.lastTankCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
        this.phantomFluidGetter = phantomFluidGetter;
        this.phantomFluidSetter = phantomFluidSetter;
    }

    @Override
    public long getAmount() {
        return this.lastTankCapacity;
    }

    @Override
    public void setAmount(long capacity) {
        this.lastTankCapacity = capacity;
    }

    @Override
    public FluidStack getPhantomStack() {
        return this.lastPhantomStack;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (!this.isMouseOverElement(mouseX, mouseY) || lockScrool) {
            return false;
        } else {
            long newCapacity = this.getnewCapacity(wheelDelta > (double) 0.0F ? 1 : -1);
            this.writeClientAction(MachinesConstants.SCROLL_ACTION_ID, (buf) -> buf.writeLong(newCapacity));
            return true;
        }
    }

    private long getnewCapacity(int wheel) {
        if (GTUtil.isCtrlDown()) {
            long multi = 2;
            if (GTUtil.isShiftDown()) {
                multi *= 2;
            }
            if (GTUtil.isAltDown()) {
                multi *= 4;
            }
            return wheel > 0 ? Math.min(maxCapacity, AHUtil.multiplyWithBounds(this.getAmount(), multi)) : AHUtil.divWithBounds(this.getAmount(), multi);
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
        return Math.min(maxCapacity, AHUtil.addWithBounds(this.getAmount(), add));
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
                    writeClientAction(MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID, buffer -> {});
            }
            return true;
        }
        return false;
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case MachinesConstants.SCROLL_ACTION_ID -> this.handleScrollAction(buffer.readLong());
            case MachinesConstants.MOUSE_LEFT_CLICK_ACTION_ID -> this.handlePhantomClick();
            case MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID -> this.handleMidleClick();
            default -> super.handleClientAction(id, buffer);
        }

        this.detectAndSendChanges();
    }

    private void handleScrollAction(long newAmount) {
        if (this.getFluidTank() != null)
            if (icFluidTank.isTruncateFluid(this.tank, newAmount) && !isWarned) {
                setWarnedAndWrite(true);
                reverseLockScrool();
            } else {
                setWarnedAndWrite(false);
                icFluidTank.newTankCapacity(this.tank, Math.max(newAmount, 0));
            }
    }

    private void handleMidleClick() {
        if (!isCtrlDown() || lockScrool) {
            reverseLockScrool();
        } else {
            if (this.getFluidTank() != null)
                if (icFluidTank.isTruncateFluid(this.tank, 0) && !isWarned) {
                    setWarnedAndWrite(true);
                    reverseLockScrool();
                } else {
                    setWarnedAndWrite(false);
                    icFluidTank.newTankCapacity(this.tank, 0);
                }
        }
    }

    private void setWarnedAndWrite(boolean setTo) {
        this.isWarned = setTo;
        writeUpdateInfo(7, buf -> buf.writeBoolean(setTo));
    }

    private void reverseLockScrool() {
        this.lockScrool = !lockScrool;
        writeUpdateInfo(6, buf -> {
            buf.writeBoolean(lockScrool);
        });
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case 6 -> this.lockScrool = buffer.readBoolean();
            case 7 -> this.isWarned = buffer.readBoolean();
            default -> super.readUpdateInfo(id, buffer);
        }
    }

    private void handlePhantomClick() {
        ItemStack itemStack = this.gui.getModularUIContainer().getCarried().copy();
        if (!itemStack.isEmpty()) {
            itemStack.setCount(1);
            IFluidTransfer handler = FluidTransferHelper.getFluidTransfer(this.gui.entityPlayer, this.gui.getModularUIContainer());
            if (handler != null) {
                FluidStack resultFluid = handler.drain(2147483647L, true);
                if (this.phantomFluidSetter != null) {
                    this.phantomFluidSetter.accept(resultFluid);
                }
            }
        } else if (this.phantomFluidSetter != null) {
            this.phantomFluidSetter.accept(FluidStack.empty());
        }
    }

    /** 写的石山代码，不知道在写什么 */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.drawBackgroundTexture(graphics, 0, 0);
        if (isClientSideWidget && fluidTank != null) {
            FluidStack fluidStack = fluidTank.getFluidInTank(tank);
            long capacity = fluidTank.getTankCapacity(tank);
            if (capacity != lastTankCapacity) {
                this.lastTankCapacity = capacity;
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
            double progress = lastFluidInTank.getAmount() * 1.0 / Math.max(Math.max(lastFluidInTank.getAmount(), lastTankCapacity), 1);
            float drawnU = (float) fillDirection.getDrawnU(progress);
            float drawnV = (float) fillDirection.getDrawnV(progress);
            float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
            float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
            int width = size.width - 2;
            int height = size.height - 2;
            int x = pos.x + 1;
            int y = pos.y + 1;
            DrawerHelper.drawFluidForGui(graphics, lastFluidInTank, lastFluidInTank.getAmount(),
                    (int) (x + drawnU * width), (int) (y + drawnV * height), ((int) (width * drawnWidth)), ((int) (height * drawnHeight)));
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (showAmount) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.5F, 0.5F, 1);
            // String s = "/" + TextFormattingUtil.formatLongToCompactStringBuckets(lastTankCapacity, 3) + "B";
            String s = "/" + AHFormattingUtil.formatLongBucketsCompactStringBuckets(lastTankCapacity) + "B";
            Font fontRenderer = Minecraft.getInstance().font;
            graphics.drawString(fontRenderer, s,
                    (int) ((pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 21),
                    (int) ((pos.y + (size.height / 3f) + 6) * 2), 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        if (lockScrool && lockTexture != null) {
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
            tooltips.add(FluidHelper.getDisplayName(fluidStack));
            if (!isShiftDown() && (fluidStack.getAmount() > min || this.lastTankCapacity > min)) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", AHFormattingUtil.formatLongBucketsToShort(fluidStack.getAmount(), min), AHFormattingUtil.formatLongBucketsToShort(this.lastTankCapacity, min)));
                if (!Platform.isForge()) {
                    tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(fluidStack.getAmount() * 1000L / FluidHelper.getBucket(), this.lastTankCapacity * 1000L / FluidHelper.getBucket())).append(" mB"));
                }
                tooltips.add(Component.translatable("ldlib.fluid.temperature", FluidHelper.getTemperature(fluidStack)));
                tooltips.add(FluidHelper.isLighterThanAir(fluidStack) ? Component.translatable("ldlib.fluid.state_gas") : Component.translatable("ldlib.fluid.state_liquid"));
                if (this.lastTankCapacity != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.phantom_capacity_tank_widget.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", fluidStack.getAmount(), this.lastTankCapacity).append(" " + FluidHelper.getUnit()));
                if (!Platform.isForge()) {
                    tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(fluidStack.getAmount() * 1000L / FluidHelper.getBucket(), this.lastTankCapacity * 1000L / FluidHelper.getBucket())).append(" mB"));
                }
                tooltips.add(Component.translatable("ldlib.fluid.temperature", FluidHelper.getTemperature(fluidStack)));
                tooltips.add(FluidHelper.isLighterThanAir(fluidStack) ? Component.translatable("ldlib.fluid.state_gas") : Component.translatable("ldlib.fluid.state_liquid"));
                if (!isShiftDown() && this.lastTankCapacity != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.phantom_capacity_tank_widget.tooltips").withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltips.add(Component.translatable("ldlib.fluid.empty"));
            if (!isShiftDown() && this.lastTankCapacity > min) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, AHFormattingUtil.formatLongBucketsToShort(this.lastTankCapacity, min)));
                if (this.lastTankCapacity != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.phantom_capacity_tank_widget.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, this.lastTankCapacity)
                        .append(" " + FluidHelper.getUnit()));
            }
            if (!Platform.isForge()) {
                tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(0, this.lastTankCapacity * 1000L / FluidHelper.getBucket()))
                        .append(" mB"));
            }
        }

        tooltips.addAll(this.getTooltipTexts());
        return tooltips;
    }
}
