package com.chrismod.motionblur.gui;

import com.chrismod.motionblur.MotionBlurMod;
import com.chrismod.motionblur.config.MotionBlurConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class MotionBlurScreen extends Screen {

    private static final int BG_COLOR = 0xD0101010;
    private static final int HEADER_BG = 0xFF1A1A1A;
    private static final int ROW_HOVER = 0x22FFFFFF;
    private static final int YELLOW = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF888888;
    private static final int CHECK_ON = 0xFFFFD700;
    private static final int CHECK_OFF = 0xFF555555;
    private static final int CHECK_FILL_ON = 0xFF2A2A00;
    private static final int PANEL_W = 240;
    private static final int ROW_H = 14;
    private static final int ROW_PAD = 3;
    private static final int HEADER_H = 20;
    private static final int FOOTER_H = 18;
    private static final int SIDE_PAD = 8;
    private static final int CORNER_R = 6;
    private static final int BORDER_COLOR = 0xFF303030;

    private final Screen parent;
    private final MotionBlurConfig config;

    private int panelX;
    private int panelY;
    private int panelH;

    private final List<Row> rows = new ArrayList<>();
    private SliderRow draggedSlider = null;
    private int draggedSliderY = 0;

    public MotionBlurScreen(Screen parent, MotionBlurConfig config) {
        super(Text.literal("MotionBlur v" + MotionBlurMod.MOD_VERSION));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        rows.clear();

        rows.add(new SectionRow("--- General ---"));
        rows.add(
            new ToggleRow(
                "Enabled",
                () -> config.enabled,
                v -> config.enabled = v
            )
        );
        rows.add(
            new CycleRow("Blur Type", this::blurTypeName, this::cycleBlurType)
        );
        rows.add(new SectionRow("--- Quality ---"));
        rows.add(
            new SliderRow(
                "Intensity",
                () -> config.intensity,
                v -> config.intensity = v,
                0.01f,
                1.0f,
                false
            )
        );
        rows.add(
            new SliderRow(
                "Sample Count",
                () -> (float) config.sampleCount,
                v -> config.sampleCount = Math.round(v),
                3f,
                15f,
                true
            )
        );
        rows.add(new SectionRow("--- Presets ---"));
        rows.add(
            new ButtonRow("Performance", () ->
                config.applyPreset(MotionBlurConfig.Preset.PERFORMANCE)
            )
        );
        rows.add(
            new ButtonRow("Balanced", () ->
                config.applyPreset(MotionBlurConfig.Preset.BALANCED)
            )
        );
        rows.add(
            new ButtonRow("Quality", () ->
                config.applyPreset(MotionBlurConfig.Preset.QUALITY)
            )
        );
        rows.add(
            new ButtonRow("Ultra", () ->
                config.applyPreset(MotionBlurConfig.Preset.ULTRA)
            )
        );
        rows.add(new SectionRow("--- Advanced ---"));
        rows.add(
            new ToggleRow(
                "Refresh Rate Opt.",
                () -> config.refreshRateOptimization,
                v -> config.refreshRateOptimization = v
            )
        );
        rows.add(
            new ToggleRow(
                "Camera Only Mode",
                () -> config.cameraOnlyMode,
                v -> config.cameraOnlyMode = v
            )
        );
        rows.add(
            new ToggleRow(
                "Glossy Mode",
                () -> config.glossyMode,
                v -> config.glossyMode = v
            )
        );
        rows.add(
            new ToggleRow(
                "CRT Mode",
                () -> config.crtMode,
                v -> config.crtMode = v
            )
        );
        rows.add(
            new ToggleRow(
                "Show HUD",
                () -> config.showHud,
                v -> config.showHud = v
            )
        );

        panelH =
            HEADER_H + rows.size() * (ROW_H + ROW_PAD) + ROW_PAD + FOOTER_H;
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x80000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);

        drawRoundedRect(
            ctx,
            panelX,
            panelY,
            PANEL_W,
            panelH,
            CORNER_R,
            BG_COLOR
        );
        drawRoundedRect(
            ctx,
            panelX,
            panelY,
            PANEL_W,
            HEADER_H,
            CORNER_R,
            HEADER_BG
        );
        ctx.fill(
            panelX,
            panelY + CORNER_R,
            panelX + PANEL_W,
            panelY + HEADER_H,
            HEADER_BG
        );
        drawRoundedBorder(
            ctx,
            panelX,
            panelY,
            PANEL_W,
            panelH,
            CORNER_R,
            BORDER_COLOR
        );

        ctx.drawCenteredTextWithShadow(
            textRenderer,
            "MotionBlur v" + MotionBlurMod.MOD_VERSION,
            panelX + PANEL_W / 2,
            panelY + (HEADER_H - 8) / 2,
            YELLOW
        );

        int ry = panelY + HEADER_H + ROW_PAD;
        for (Row row : rows) {
            row.render(ctx, panelX, ry, PANEL_W, mx, my);
            ry += ROW_H + ROW_PAD;
        }

        ctx.drawCenteredTextWithShadow(
            textRenderer,
            "Made by chris",
            panelX + PANEL_W / 2,
            panelY + panelH - FOOTER_H + (FOOTER_H - 8) / 2,
            GRAY
        );

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean focused) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (click.button() != 0) return super.mouseClicked(click, focused);
        int ry = panelY + HEADER_H + ROW_PAD;
        for (Row row : rows) {
            if (my >= ry && my < ry + ROW_H) {
                row.onClick(mx, my, panelX, ry, PANEL_W);
                if (row instanceof SliderRow sr) {
                    draggedSlider = sr;
                    draggedSliderY = ry;
                }
                return true;
            }
            ry += ROW_H + ROW_PAD;
        }
        return super.mouseClicked(click, focused);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggedSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click.button() == 0 && draggedSlider != null) {
            draggedSlider.onClick(
                (int) click.x(),
                draggedSliderY,
                panelX,
                draggedSliderY,
                PANEL_W
            );
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(
        double mx,
        double my,
        double hScroll,
        double vScroll
    ) {
        int ry = panelY + HEADER_H + ROW_PAD;
        for (Row row : rows) {
            if (my >= ry && my < ry + ROW_H) {
                row.onScroll((int) mx, (int) my, vScroll, panelX, ry, PANEL_W);
                return true;
            }
            ry += ROW_H + ROW_PAD;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        MotionBlurConfig.save(config);
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private String blurTypeName() {
        return switch (config.blurType) {
            case 1 -> "Accumulation [recommended]";
            case 2 -> "Velocity";
            case 3 -> "Velocity (Gaussian)";
            default -> "Unknown";
        };
    }

    private void cycleBlurType() {
        config.blurType = (config.blurType % 3) + 1;
    }

    // ---- Drawing helpers ----

    private static void drawRoundedRect(
        DrawContext ctx,
        int x,
        int y,
        int w,
        int h,
        int r,
        int color
    ) {
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int xOff =
                r -
                (int) Math.round(
                    Math.sqrt((double) r * r - (double) (r - i) * (r - i))
                );
            ctx.fill(x + xOff, y + i, x + w - xOff, y + i + 1, color);
            ctx.fill(x + xOff, y + h - i - 1, x + w - xOff, y + h - i, color);
        }
    }

    private static void drawRoundedBorder(
        DrawContext ctx,
        int x,
        int y,
        int w,
        int h,
        int r,
        int color
    ) {
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int xOff =
                r -
                (int) Math.round(
                    Math.sqrt((double) r * r - (double) (r - i) * (r - i))
                );
            int xOffNext =
                r -
                (int) Math.round(
                    Math.sqrt(
                        (double) r * r - (double) (r - i - 1) * (r - i - 1)
                    )
                );
            ctx.fill(x + xOff, y + i, x + xOffNext + 1, y + i + 1, color);
            ctx.fill(
                x + w - xOffNext - 1,
                y + i,
                x + w - xOff,
                y + i + 1,
                color
            );
            ctx.fill(
                x + xOff,
                y + h - i - 1,
                x + xOffNext + 1,
                y + h - i,
                color
            );
            ctx.fill(
                x + w - xOffNext - 1,
                y + h - i - 1,
                x + w - xOff,
                y + h - i,
                color
            );
        }
    }

    private static void drawBorderRect(
        DrawContext ctx,
        int x,
        int y,
        int w,
        int h,
        int color
    ) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    // ---- Row types ----

    interface Row {
        void render(DrawContext ctx, int px, int ry, int pw, int mx, int my);

        default void onClick(int mx, int my, int px, int ry, int pw) {}

        default void onScroll(
            int mx,
            int my,
            double scroll,
            int px,
            int ry,
            int pw
        ) {}
    }

    private class SectionRow implements Row {

        private final String label;

        SectionRow(String label) {
            this.label = label;
        }

        @Override
        public void render(
            DrawContext ctx,
            int px,
            int ry,
            int pw,
            int mx,
            int my
        ) {
            ctx.drawTextWithShadow(
                textRenderer,
                label,
                px + SIDE_PAD,
                ry + (ROW_H - 8) / 2,
                GRAY
            );
        }
    }

    private class ToggleRow implements Row {

        private final String label;
        private final java.util.function.BooleanSupplier getter;
        private final java.util.function.Consumer<Boolean> setter;

        ToggleRow(
            String label,
            java.util.function.BooleanSupplier getter,
            java.util.function.Consumer<Boolean> setter
        ) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void render(
            DrawContext ctx,
            int px,
            int ry,
            int pw,
            int mx,
            int my
        ) {
            boolean val = getter.getAsBoolean();
            boolean hovered =
                mx >= px && mx < px + pw && my >= ry && my < ry + ROW_H;
            if (hovered) ctx.fill(px, ry, px + pw, ry + ROW_H, ROW_HOVER);
            ctx.drawTextWithShadow(
                textRenderer,
                label,
                px + SIDE_PAD,
                ry + (ROW_H - 8) / 2,
                val ? YELLOW : WHITE
            );

            int cbX = px + pw - SIDE_PAD - 9;
            int cbY = ry + (ROW_H - 9) / 2;
            ctx.fill(cbX, cbY, cbX + 9, cbY + 9, val ? YELLOW : 0xFF1A1A1A);
            drawBorderRect(ctx, cbX, cbY, 9, 9, val ? YELLOW : CHECK_OFF);
        }

        @Override
        public void onClick(int mx, int my, int px, int ry, int pw) {
            setter.accept(!getter.getAsBoolean());
        }
    }

    private class CycleRow implements Row {

        private final String label;
        private final java.util.function.Supplier<String> valueGetter;
        private final Runnable cycler;

        CycleRow(
            String label,
            java.util.function.Supplier<String> valueGetter,
            Runnable cycler
        ) {
            this.label = label;
            this.valueGetter = valueGetter;
            this.cycler = cycler;
        }

        @Override
        public void render(
            DrawContext ctx,
            int px,
            int ry,
            int pw,
            int mx,
            int my
        ) {
            boolean hovered =
                mx >= px && mx < px + pw && my >= ry && my < ry + ROW_H;
            if (hovered) ctx.fill(px, ry, px + pw, ry + ROW_H, ROW_HOVER);
            ctx.drawTextWithShadow(
                textRenderer,
                label,
                px + SIDE_PAD,
                ry + (ROW_H - 8) / 2,
                YELLOW
            );
            String val = "< " + valueGetter.get() + " >";
            ctx.drawTextWithShadow(
                textRenderer,
                val,
                px + pw - SIDE_PAD - textRenderer.getWidth(val),
                ry + (ROW_H - 8) / 2,
                WHITE
            );
        }

        @Override
        public void onClick(int mx, int my, int px, int ry, int pw) {
            cycler.run();
        }
    }

    private class SliderRow implements Row {

        private final String label;
        private final java.util.function.Supplier<Float> getter;
        private final java.util.function.Consumer<Float> setter;
        private final float min;
        private final float max;
        private final boolean isInt;

        SliderRow(
            String label,
            java.util.function.Supplier<Float> getter,
            java.util.function.Consumer<Float> setter,
            float min,
            float max,
            boolean isInt
        ) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.isInt = isInt;
        }

        @Override
        public void render(
            DrawContext ctx,
            int px,
            int ry,
            int pw,
            int mx,
            int my
        ) {
            boolean hovered =
                mx >= px && mx < px + pw && my >= ry && my < ry + ROW_H;
            if (hovered) ctx.fill(px, ry, px + pw, ry + ROW_H, ROW_HOVER);
            ctx.drawTextWithShadow(
                textRenderer,
                label,
                px + SIDE_PAD,
                ry + (ROW_H - 8) / 2,
                WHITE
            );

            float val = getter.get();
            String valStr = isInt
                ? String.valueOf(Math.round(val))
                : String.format("%.2f", val);

            int sliderX = px + pw - SIDE_PAD - 60;
            int sliderY = ry + (ROW_H - 6) / 2;
            ctx.fill(sliderX, sliderY, sliderX + 60, sliderY + 6, 0xFF2A2A2A);
            int fillW = (int) (((val - min) / (max - min)) * 58);
            ctx.fill(
                sliderX + 1,
                sliderY + 1,
                sliderX + 1 + fillW,
                sliderY + 5,
                YELLOW
            );
            drawBorderRect(ctx, sliderX, sliderY, 60, 6, 0xFF555555);

            int labelW = textRenderer.getWidth(valStr);
            ctx.drawTextWithShadow(
                textRenderer,
                valStr,
                sliderX - labelW - 4,
                ry + (ROW_H - 8) / 2,
                YELLOW
            );
        }

        @Override
        public void onClick(int mx, int my, int px, int ry, int pw) {
            int sliderX = px + pw - SIDE_PAD - 60;
            float t = Math.max(0f, Math.min(1f, (float) (mx - sliderX) / 60f));
            float val = min + t * (max - min);
            if (isInt) val = Math.round(val);
            setter.accept(val);
        }

        @Override
        public void onScroll(
            int mx,
            int my,
            double scroll,
            int px,
            int ry,
            int pw
        ) {
            float step = isInt ? 1f : 0.05f;
            setter.accept(
                Math.max(
                    min,
                    Math.min(max, getter.get() + (float) scroll * step)
                )
            );
        }
    }

    private class ButtonRow implements Row {

        private final String label;
        private final Runnable action;

        ButtonRow(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public void render(
            DrawContext ctx,
            int px,
            int ry,
            int pw,
            int mx,
            int my
        ) {
            boolean hovered =
                mx >= px && mx < px + pw && my >= ry && my < ry + ROW_H;
            if (hovered) ctx.fill(px, ry, px + pw, ry + ROW_H, ROW_HOVER);
            ctx.drawTextWithShadow(
                textRenderer,
                "> " + label,
                px + SIDE_PAD,
                ry + (ROW_H - 8) / 2,
                hovered ? YELLOW : GRAY
            );
        }

        @Override
        public void onClick(int mx, int my, int px, int ry, int pw) {
            action.run();
        }
    }
}
