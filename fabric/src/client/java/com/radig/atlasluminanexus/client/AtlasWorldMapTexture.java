package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Cartografia mundial con doble buffer GPU.
 * Nunca se borra la textura visible al cambiar el zoom: se muestra reescalada
 * mientras el nuevo nivel de detalle se calcula en una segunda textura.
 */
public final class AtlasWorldMapTexture {
    private static final int CELL = 2;
    private static final int BUFFER = 36;
    private static final int PIXELS_PER_FRAME = 18000;
    private static final int PLACEHOLDER = 0xFF19283B;
    private static final long ZOOM_SETTLE_NANOS = 85_000_000L;
    private static final Identifier SLOT_A = Identifier.fromNamespaceAndPath("atlasluminanexus", "runtime/world_map_a");
    private static final Identifier SLOT_B = Identifier.fromNamespaceAndPath("atlasluminanexus", "runtime/world_map_b");

    private static final class Layer {
        Identifier id;
        DynamicTexture texture;
        int[] colors;
        int cols, rows, viewCols, viewRows, zoom, cursor, epoch;
        double originX, originZ, block;
        boolean high, smooth, grid, ready;
        int terrainQuality;
        String dimension;
    }

    private Layer visible;
    private Layer pending;
    private int epoch = 0;
    private int lastZoom = Integer.MIN_VALUE;
    private long lastZoomChange;

    /** Invalida el muestreo sin destruir la imagen visible. */
    public void invalidate() { epoch++; }

    private void release(Layer layer) {
        if (layer != null && layer.texture != null) {
            Minecraft.getInstance().getTextureManager().release(layer.id);
            layer.texture = null;
        }
    }

    public void close() {
        release(pending);
        release(visible);
        pending = null;
        visible = null;
    }

    private boolean sameMode(Layer l, Minecraft mc, int w, int h, double worldBlock, int zoom) {
        return l != null && l.epoch == epoch && l.zoom == zoom
                && l.viewCols == Math.max(1, (w + CELL - 1) / CELL)
                && l.viewRows == Math.max(1, (h + CELL - 1) / CELL)
                && Double.compare(l.block, worldBlock) == 0
                && l.dimension.equals(mc.level.dimension().toString())
                && l.high == AtlasMinimapConfig.highDetail
                && l.smooth == AtlasMinimapConfig.smoothTerrain
                && l.grid == AtlasMinimapConfig.showUnknownGrid
                && l.terrainQuality == AtlasMinimapConfig.terrainWorldMap;
    }

    private boolean covers(Layer l, Minecraft mc, int w, int h,
                           double cx, double cz, double block, int zoom) {
        if (!sameMode(l, mc, w, h, block, zoom)) return false;
        double left = cx - w * block / (2.0 * CELL);
        double top = cz - h * block / (2.0 * CELL);
        double right = left + w * block / CELL;
        double bottom = top + h * block / CELL;
        return left >= l.originX + BUFFER * .40 * block
                && right <= l.originX + (l.cols - BUFFER * .40) * block
                && top >= l.originZ + BUFFER * .40 * block
                && bottom <= l.originZ + (l.rows - BUFFER * .40) * block;
    }

    private Layer create(Minecraft mc, int w, int h, double cx, double cz,
                         double block, int zoom) {
        Layer next = new Layer();
        next.id = visible == null || SLOT_B.equals(visible.id) ? SLOT_A : SLOT_B;
        next.viewCols = Math.max(1, (w + CELL - 1) / CELL);
        next.viewRows = Math.max(1, (h + CELL - 1) / CELL);
        next.cols = next.viewCols + BUFFER * 2;
        next.rows = next.viewRows + BUFFER * 2;
        next.block = block;
        next.originX = Math.floor((cx - w * block / (2.0 * CELL) - BUFFER * block) / block) * block;
        next.originZ = Math.floor((cz - h * block / (2.0 * CELL) - BUFFER * block) / block) * block;
        next.zoom = zoom;
        next.epoch = epoch;
        next.dimension = mc.level.dimension().toString();
        next.high = AtlasMinimapConfig.highDetail;
        next.smooth = AtlasMinimapConfig.smoothTerrain;
        next.grid = AtlasMinimapConfig.showUnknownGrid;
        next.terrainQuality = AtlasMinimapConfig.terrainWorldMap;
        next.colors = new int[next.cols * next.rows];
        NativeImage pixels = new NativeImage(next.cols, next.rows, false);
        Layer old = visible;
        boolean copy = sameMode(old, mc, w, h, block, zoom);
        int shiftX = 0, shiftZ = 0;
        if (copy) {
            shiftX = (int) Math.round((next.originX - old.originX) / block);
            shiftZ = (int) Math.round((next.originZ - old.originZ) / block);
            copy = Math.abs(shiftX) < old.cols && Math.abs(shiftZ) < old.rows;
        }
        for (int row = 0; row < next.rows; row++) {
            for (int col = 0; col < next.cols; col++) {
                int value = 0;
                if (copy) {
                    int previousX = col + shiftX;
                    int previousY = row + shiftZ;
                    if (previousX >= 0 && previousX < old.cols && previousY >= 0 && previousY < old.rows)
                        value = old.colors[previousY * old.cols + previousX];
                }
                next.colors[row * next.cols + col] = value;
                pixels.setPixel(col, row, value == 0 ? PLACEHOLDER : value);
            }
        }
        next.texture = new DynamicTexture(() -> "aln_world_map_buffer", pixels);
        mc.getTextureManager().register(next.id, next.texture);
        next.texture.upload();
        return next;
    }

    private void process(Layer layer, boolean showProgress) {
        if (layer == null || layer.ready) return;
        NativeImage pixels = layer.texture.getPixels();
        int processed = 0;
        boolean touched = false;
        // Quality-aware CPU budget: don't monopolize the render thread in HD mode.
        int budget = layer.terrainQuality == AtlasTerrainEngine.LUMINA_HD ? 7000
                : layer.terrainQuality == AtlasTerrainEngine.DETAILED ? 12000 : PIXELS_PER_FRAME;
        long deadline = System.nanoTime() + 3_500_000L;
        while (layer.cursor < layer.colors.length && processed < budget) {
            if (processed >= 256 && (processed & 127) == 0 && System.nanoTime() >= deadline) break;
            int index = layer.cursor++;
            if (layer.colors[index] != 0) continue;
            int column = index % layer.cols;
            int row = index / layer.cols;
            double wx = layer.originX + (column + .5) * layer.block;
            double wz = layer.originZ + (row + .5) * layer.block;
            int color = AtlasMinimapVisuals.sample(wx, wz, layer.block, layer.high, layer.smooth, layer.terrainQuality);
            if (color == 0)
                color = AtlasMinimapVisuals.unknown((int)Math.floor(wx), (int)Math.floor(wz), layer.grid);
            color = 0xFF000000 | (color & 0xFFFFFF);
            layer.colors[index] = color;
            pixels.setPixel(column, row, color);
            touched = true;
            processed++;
        }
        layer.ready = layer.cursor >= layer.colors.length;
        // Si ya hay un mapa visible, sólo subimos el nuevo cuando esté listo:
        // evita mostrar franjas provisionales y reduce subidas a la GPU.
        if (touched && (showProgress || layer.ready)) layer.texture.upload();
    }

    private void drawLayer(GuiGraphicsExtractor g, Layer layer, int x, int y,
                           int w, int h, double cx, double cz, double wantedBlock) {
        if (layer == null) return;
        double left = cx - w * wantedBlock / (2.0 * CELL);
        double top = cz - h * wantedBlock / (2.0 * CELL);
        double right = left + w * wantedBlock / CELL;
        double bottom = top + h * wantedBlock / CELL;
        float u0 = (float)Math.max(0, Math.min(1, (left - layer.originX) / (layer.cols * layer.block)));
        float v0 = (float)Math.max(0, Math.min(1, (top - layer.originZ) / (layer.rows * layer.block)));
        float u1 = (float)Math.max(0, Math.min(1, (right - layer.originX) / (layer.cols * layer.block)));
        float v1 = (float)Math.max(0, Math.min(1, (bottom - layer.originZ) / (layer.rows * layer.block)));
        if (u1 <= u0 || v1 <= v0) return;
        g.blit(layer.id, x, y, x + w, y + h, u0, u1, v0, v1);
    }

    public void draw(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, int h,
                     double cx, double cz, double worldBlock, int level) {
        if (mc.level == null) return;
        if (visible != null && !visible.dimension.equals(mc.level.dimension().toString())) {
            release(pending);
            release(visible);
            pending = null;
            visible = null;
        }
        if (level != lastZoom) {
            lastZoom = level;
            lastZoomChange = System.nanoTime();
        }
        if (covers(visible, mc, w, h, cx, cz, worldBlock, level)) {
            if (pending != null) { release(pending); pending = null; }
        } else {
            // Pequeña pausa al girar repetidamente la rueda. La imagen anterior
            // sigue ampliándose/reduciéndose mientras termina el gesto.
            boolean settling = visible != null && visible.zoom != level
                    && System.nanoTime() - lastZoomChange < ZOOM_SETTLE_NANOS;
            if (!settling) {
                if (!covers(pending, mc, w, h, cx, cz, worldBlock, level)) {
                    release(pending);
                    pending = create(mc, w, h, cx, cz, worldBlock, level);
                }
                process(pending, visible == null);
                if (pending.ready) {
                    release(visible);
                    visible = pending;
                    pending = null;
                }
            }
        }
        // Sólo se muestra la capa vieja hasta que la nueva está completa.
        // En el primer arranque se dibuja la textura en construcción.
        drawLayer(g, visible != null ? visible : pending, x, y, w, h, cx, cz, worldBlock);
    }
}
