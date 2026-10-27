package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Atlas Lumina Nexus - editor en dos columnas con selectores modales.
 * Las ventanas se dibujan SOBRE el editor: los campos no pierden lo escrito.
 * Usado en Fabric y Forge (Minecraft 26.3).
 */
public final class AtlasWaypointEditorScreen extends Screen {
    private static final int BG = 0xF50D1425;
    private static final int GOLD = 0xFFE3BF75;
    private static final int MUTED = 0xFFB4A993;
    private static final int INK = 0xFF483B2A;
    private static final int PARCHMENT = 0xFFF0E2C3;
    private static final int[] COLORS = {
            0xFFFFD46A, 0xFF73D9FF, 0xFFAF88FF,
            0xFF70E4A4, 0xFFFF7777, 0xFFFFFFFF
    };
    private static final String[] COLOR_NAMES = {
            "Dorado", "Cian", "Morado", "Verde", "Rojo", "Blanco"
    };
    private static final String ICON_PATH = "textures/gui/waypoint_types/";
    private static final String[] ICON_FILES = {
            "pin", "home", "mine", "forest", "gem", "flag"
    };

    private enum Popup { NONE, CATEGORY, COLOR, ICON }

    private final Waypoint editing;
    private final List<AbstractWidget> editorWidgets = new ArrayList<>();
    private final List<AbstractWidget> popupWidgets = new ArrayList<>();
    private final List<Button> categoryRows = new ArrayList<>();
    private final List<Button> colorRows = new ArrayList<>();
    private final List<Button> iconRows = new ArrayList<>();

    private EditBox name, world, x, y, z, newCategory;
    private Button categoryButton, colorButton, iconButton, visibleButton;
    private Button categoryDelete;
    private Popup popup = Popup.NONE;
    private String category;
    private String popupCategory;
    private String message = "";
    private int colorIndex = 0, iconIndex = 0, categoryScroll = 0;
    private int left, top, pw, ph, popupX, popupY, popupW, popupH;
    private boolean visible, recapture;

    public AtlasWaypointEditorScreen(Waypoint waypoint) {
        super(Component.literal(waypoint == null ? "Nuevo waypoint" : "Editar waypoint"));
        this.editing = waypoint;
        this.visible = waypoint == null || waypoint.visible();
        this.category = waypoint == null ? "General" : waypoint.category();
        this.popupCategory = this.category;
        if (waypoint != null) {
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i] == waypoint.argb()) colorIndex = i;
            }
            iconIndex = AtlasWaypointIcons.get(Minecraft.getInstance(), waypoint.id());
        }
    }

    private <T extends AbstractWidget> T editor(T widget) {
        editorWidgets.add(addRenderableWidget(widget));
        return widget;
    }

    private <T extends AbstractWidget> T modal(T widget) {
        popupWidgets.add(addRenderableWidget(widget));
        widget.visible = false;
        return widget;
    }

    private EditBox entry(String label, int px, int py, int w, String value) {
        EditBox field = editor(new EditBox(font, px, py, w, 18, Component.literal(label)));
        field.setMaxLength(120);
        field.setValue(value);
        return field;
    }

    private String dimensionNow() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "minecraft:overworld" : mc.level.dimension().toString();
    }

    private String displayDimension(String value) {
        if (value.contains("the_nether")) return "Nether";
        if (value.contains("the_end")) return "End";
        if (value.contains("overworld")) return "Overworld";
        return value;
    }

    @Override
    protected void init() {
        editorWidgets.clear();
        popupWidgets.clear();
        categoryRows.clear();
        colorRows.clear();
        iconRows.clear();
        popup = Popup.NONE;

        pw = Math.min(424, width - 12);
        ph = Math.min(234, height - 12);
        left = (width - pw) / 2;
        top = (height - ph) / 2;
        int inner = pw - 28;
        int gap = 14;
        int colWidth = (inner - gap) / 2;
        int lx = left + 14;
        int rx = lx + colWidth + gap;
        Minecraft mc = Minecraft.getInstance();

        // Fila 1: Nombre / Mundo
        name = entry("Nombre", lx, top + 46, colWidth,
                editing == null ? "" : editing.name());
        String dim = editing == null ? dimensionNow() : editing.dimension();
        world = entry("Mundo", rx, top + 46, colWidth, displayDimension(dim));
        world.active = false;

        // Fila 2: Categoría / Coordenadas XYZ + Usar actual
        categoryButton = editor(Button.builder(Component.literal(""), b -> openPopup(Popup.CATEGORY))
                .bounds(lx, top + 89, colWidth, 18).build());
        double px = mc.player == null ? 0 : mc.player.getX();
        double py = mc.player == null ? 64 : mc.player.getY();
        double pz = mc.player == null ? 0 : mc.player.getZ();
        int coordWidth = Math.max(26, (colWidth - 77 - 9) / 3);
        x = entry("X", rx, top + 89, coordWidth, Integer.toString((int) Math.floor(editing == null ? px : editing.x())));
        y = entry("Y", rx + coordWidth + 3, top + 89, coordWidth, Integer.toString((int) Math.floor(editing == null ? py : editing.y())));
        z = entry("Z", rx + (coordWidth + 3) * 2, top + 89, coordWidth, Integer.toString((int) Math.floor(editing == null ? pz : editing.z())));
        int useX = rx + (coordWidth + 3) * 3;
        Button useCurrent = editor(Button.builder(Component.literal("Usar actual"), b -> {
            if (mc.player == null || mc.level == null) return;
            x.setValue(Integer.toString(mc.player.getBlockX()));
            y.setValue(Integer.toString(mc.player.getBlockY()));
            z.setValue(Integer.toString(mc.player.getBlockZ()));
            world.setValue(displayDimension(dimensionNow()));
            recapture = true;
            message = "Ubicación y foto se actualizarán al guardar";
        }).bounds(useX, top + 89, Math.max(30, rx + colWidth - useX), 18).build());
        useCurrent.setTooltip(Tooltip.create(Component.literal("Usar coordenadas y vista actuales")));

        // Fila 3: Color / Icono. Ambos abren popup visual.
        colorButton = editor(Button.builder(Component.literal(""), b -> openPopup(Popup.COLOR))
                .bounds(lx, top + 132, colWidth, 18).build());
        iconButton = editor(Button.builder(Component.literal(""), b -> openPopup(Popup.ICON))
                .bounds(rx, top + 132, colWidth, 18).build());

        // Fila 4: Visible
        visibleButton = editor(Button.builder(Component.literal(""), b -> {
            visible = !visible;
            updateLabels();
        }).bounds(lx, top + 169, colWidth, 18).build());

        // Botones inferiores
        int actionY = top + ph - 29;
        editor(Button.builder(Component.literal("Guardar"), b -> save())
                .bounds(lx, actionY, colWidth, 19).build());
        editor(Button.builder(Component.literal("Cancelar"), b -> mc.gui.setScreen(new AtlasScreen()))
                .bounds(rx, actionY, colWidth, 19).build());

        createPopupWidgets();
        updateLabels();
        applyPopupVisibility();
    }

    private void createPopupWidgets() {
        Minecraft mc = Minecraft.getInstance();
        popupW = Math.min(290, width - 24);
        popupH = Math.min(210, height - 24);
        popupX = (width - popupW) / 2;
        popupY = (height - popupH) / 2;
        modal(Button.builder(Component.literal("X"), b -> closePopup())
                .bounds(popupX + popupW - 24, popupY + 10, 16, 16).build());

        // Sólo visible en selector de categorías.
        for (int i = 0; i < 5; i++) {
            final int row = i;
            categoryRows.add(modal(Button.builder(Component.empty(), b -> {
                List<String> values = AtlasCategories.all(mc);
                int selected = categoryScroll + row;
                if (selected < values.size()) {
                    popupCategory = values.get(selected);
                    category = popupCategory;
                    updateLabels();
                    closePopup();
                }
            }).bounds(popupX + 13, popupY + 34 + i * 24, popupW - 26, 21).build()));
        }
        newCategory = modal(new EditBox(font, popupX + 13, popupY + popupH - 43,
                popupW - 103, 18, Component.literal("Nueva categoría")));
        newCategory.setMaxLength(35);
        newCategory.setHint(Component.literal("Nueva categoría..."));
        modal(Button.builder(Component.literal("+"), b -> addCategory())
                .bounds(popupX + popupW - 85, popupY + popupH - 43, 30, 18).build());
        categoryDelete = modal(Button.builder(Component.literal("-"), b -> removeCategory())
                .bounds(popupX + popupW - 49, popupY + popupH - 43, 30, 18).build());
        categoryDelete.setTooltip(Tooltip.create(Component.literal("Eliminar categoría; mover puntos a General")));

        // Selector de colores: 3 columnas x 2 filas; botones con muestras reales.
        int itemWidth = (popupW - 42) / 3;
        for (int i = 0; i < COLORS.length; i++) {
            final int selected = i;
            Button b = modal(Button.builder(Component.empty(), btn -> {
                colorIndex = selected;
                updateLabels();
                closePopup();
            }).bounds(popupX + 13 + (i % 3) * (itemWidth + 8),
                    popupY + 46 + (i / 3) * 45, itemWidth, 35).build());
            b.setTooltip(Tooltip.create(Component.literal(COLOR_NAMES[i])));
            colorRows.add(b);
        }

        // Selector de iconos: mismas dimensiones, cada icono es textura PNG.
        for (int i = 0; i < AtlasWaypointIcons.NAMES.length; i++) {
            final int selected = i;
            Button b = modal(Button.builder(Component.empty(), btn -> {
                iconIndex = selected;
                updateLabels();
                closePopup();
            }).bounds(popupX + 13 + (i % 3) * (itemWidth + 8),
                    popupY + 46 + (i / 3) * 45, itemWidth, 35).build());
            b.setTooltip(Tooltip.create(Component.literal(AtlasWaypointIcons.NAMES[i])));
            iconRows.add(b);
        }
    }

    private void addCategory() {
        try {
            String entered = newCategory.getValue().trim();
            AtlasCategories.add(Minecraft.getInstance(), entered);
            List<String> names = AtlasCategories.all(Minecraft.getInstance());
            for (String found : names) if (found.equalsIgnoreCase(entered)) {
                category = found;
                popupCategory = found;
                break;
            }
            newCategory.setValue("");
            categoryScroll = Math.max(0, names.indexOf(category) - 2);
            message = "Categoría agregada";
            updateLabels();
            updateCategoryRows();
        } catch (Exception e) { message = "Error: " + e.getMessage(); }
    }

    private void removeCategory() {
        if (popupCategory.equalsIgnoreCase("General")) {
            message = "General no se puede eliminar";
            return;
        }
        try {
            AtlasCategories.remove(Minecraft.getInstance(), popupCategory);
            if (category.equalsIgnoreCase(popupCategory)) category = "General";
            popupCategory = "General";
            categoryScroll = 0;
            message = "Puntos movidos a General";
            updateLabels();
            updateCategoryRows();
        } catch (Exception e) { message = "Error: " + e.getMessage(); }
    }

    private void openPopup(Popup next) {
        popup = next;
        if (next == Popup.CATEGORY) {
            popupCategory = category;
            int index = AtlasCategories.all(Minecraft.getInstance()).indexOf(category);
            categoryScroll = Math.max(0, index - 2);
        }
        message = "";
        applyPopupVisibility();
    }

    private void closePopup() {
        popup = Popup.NONE;
        message = "";
        applyPopupVisibility();
    }

    private void updateLabels() {
        if (categoryButton != null) categoryButton.setMessage(Component.literal("Categoría: " + abbreviate(category, 23)));
        if (colorButton != null) colorButton.setMessage(Component.literal("Elegir color"));
        if (iconButton != null) iconButton.setMessage(Component.literal("Icono: " + AtlasWaypointIcons.NAMES[iconIndex]));
        if (visibleButton != null) visibleButton.setMessage(Component.literal("Visible: " + (visible ? "Sí" : "No")));
    }

    private String abbreviate(String value, int limit) {
        return value.length() > limit ? value.substring(0, limit - 1) + "…" : value;
    }

    private void updateCategoryRows() {
        List<String> categories = AtlasCategories.all(Minecraft.getInstance());
        categoryScroll = Math.min(categoryScroll, Math.max(0, categories.size() - 5));
        for (int i = 0; i < categoryRows.size(); i++) {
            Button row = categoryRows.get(i);
            int index = categoryScroll + i;
            row.visible = popup == Popup.CATEGORY && index < categories.size();
            if (index < categories.size()) {
                row.setMessage(Component.literal(abbreviate(categories.get(index), 32)));
            }
        }
        if (categoryDelete != null) categoryDelete.active = !popupCategory.equalsIgnoreCase("General");
    }

    private void applyPopupVisibility() {
        boolean editingVisible = popup == Popup.NONE;
        for (AbstractWidget widget : editorWidgets) widget.visible = editingVisible;
        for (AbstractWidget widget : popupWidgets) widget.visible = false;
        if (popup != Popup.NONE) {
            // Primer widget: X, común a los tres popups.
            if (!popupWidgets.isEmpty()) popupWidgets.get(0).visible = true;
            if (popup == Popup.CATEGORY) {
                updateCategoryRows();
                newCategory.visible = true;
                // Los botones + y - se crearon justo después del campo.
                popupWidgets.get(7).visible = true;
                popupWidgets.get(8).visible = true;
            } else if (popup == Popup.COLOR) {
                for (Button b : colorRows) b.visible = true;
            } else if (popup == Popup.ICON) {
                for (Button b : iconRows) b.visible = true;
            }
        }
    }

    private void save() {
        try {
            Minecraft mc = Minecraft.getInstance();
            String title = name.getValue().trim();
            if (title.isBlank()) throw new IllegalArgumentException("Escribe un nombre");
            double xx = Integer.parseInt(x.getValue().trim());
            double yy = Integer.parseInt(y.getValue().trim());
            double zz = Integer.parseInt(z.getValue().trim());
            String dim = editing != null && !recapture ? editing.dimension() : dimensionNow();
            boolean first = editing == null && AtlasWaypointClient.all(mc).isEmpty();
            Waypoint waypoint = new Waypoint(
                    editing == null ? UUID.randomUUID() : editing.id(),
                    title, dim, xx, yy, zz, COLORS[colorIndex], category, visible,
                    editing == null ? first : editing.emergencyDestination()
            );
            AtlasWaypointClient.upsert(mc, waypoint);
            AtlasWaypointIcons.set(mc, waypoint.id(), iconIndex);
            if (editing == null || recapture) AtlasWaypointClient.queuePreview(waypoint.id());
            mc.gui.setScreen(null);
        } catch (NumberFormatException e) {
            message = "Las coordenadas deben ser números enteros";
        } catch (Exception e) {
            message = "Error: " + e.getMessage();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (popup == Popup.CATEGORY) {
            int max = Math.max(0, AtlasCategories.all(Minecraft.getInstance()).size() - 5);
            categoryScroll = Math.max(0, Math.min(max, categoryScroll + (deltaY < 0 ? 1 : -1)));
            updateCategoryRows();
            return true;
        }
        if (popup != Popup.NONE) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (popup != Popup.NONE && event.key() == 256) {
            closePopup();
            return true;
        }
        if (popup == Popup.NONE && event.key() == InputConstants.KEY_M) {
            Minecraft.getInstance().gui.setScreen(new AtlasScreen());
            return true;
        }
        return super.keyPressed(event);
    }

    private Identifier icon(int index) {
        return Identifier.fromNamespaceAndPath("atlasluminanexus", ICON_PATH + ICON_FILES[index] + ".png");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(left, top, left + pw, top + ph, BG);
        g.outline(left, top, pw, ph, 0xFFAC8B50);
        int inner = pw - 28;
        int gap = 14;
        int cw = (inner - gap) / 2;
        int lx = left + 14;
        int rx = lx + cw + gap;
        g.text(font, editing == null ? "NUEVO WAYPOINT" : "EDITAR WAYPOINT", lx, top + 12, GOLD, false);
        g.text(font, "Nombre", lx, top + 33, MUTED, false);
        g.text(font, "Mundo", rx, top + 33, MUTED, false);
        g.text(font, "Categoría", lx, top + 76, MUTED, false);
        g.text(font, "Coordenadas (X / Y / Z)", rx, top + 76, MUTED, false);
        g.text(font, "Color", lx, top + 119, MUTED, false);
        g.text(font, "Icono", rx, top + 119, MUTED, false);
        g.text(font, "Visibilidad", lx, top + 156, MUTED, false);
        if (popup == Popup.NONE && !message.isEmpty()) {
            g.text(font, abbreviate(message, 54), lx, top + ph - 43, 0xFFFFD48A, false);
        }
        if (popup != Popup.NONE) {
            // Un verdadero cuadro modal: ocupa el frente e impide clics detrás.
            g.fill(0, 0, width, height, 0xB8000000);
            g.fill(popupX, popupY, popupX + popupW, popupY + popupH, PARCHMENT);
            g.outline(popupX, popupY, popupW, popupH, 0xFFC9A563);
            g.outline(popupX + 3, popupY + 3, popupW - 6, popupH - 6, 0xFFDDC594);
            String heading = popup == Popup.CATEGORY ? "Seleccionar categoría" :
                    popup == Popup.COLOR ? "Seleccionar color" : "Seleccionar icono";
            g.text(font, heading, popupX + 13, popupY + 14, INK, false);
            g.horizontalLine(popupX + 13, popupX + popupW - 13, popupY + 30, 0xFFB4945C);
            if (popup == Popup.CATEGORY && !message.isEmpty()) {
                g.text(font, abbreviate(message, 35), popupX + 13,
                        popupY + popupH - 61, 0xFF85512B, false);
            }
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Iconos del selector por encima del fondo de los botones.
        if (popup == Popup.NONE) {
            g.fill(lx + 5, top + 138, lx + 17, top + 146, COLORS[colorIndex]);
            g.blit(icon(iconIndex), rx + 7, top + 134, rx + 21, top + 148, 0f, 1f, 0f, 1f);
        }

        if (popup == Popup.COLOR) {
            for (int i = 0; i < colorRows.size(); i++) {
                Button b = colorRows.get(i);
                int bx = b.getX(), by = b.getY();
                g.fill(bx + 10, by + 7, bx + b.getWidth() - 10, by + b.getHeight() - 7, COLORS[i]);
                if (i == colorIndex) g.outline(bx + 4, by + 3, b.getWidth() - 8, b.getHeight() - 6, 0xFFFFE4A2);
            }
        }
        if (popup == Popup.ICON) {
            for (int i = 0; i < iconRows.size(); i++) {
                Button b = iconRows.get(i);
                int size = 23;
                int bx = b.getX() + (b.getWidth() - size) / 2;
                int by = b.getY() + (b.getHeight() - size) / 2;
                g.blit(icon(i), bx, by, bx + size, by + size, 0f, 1f, 0f, 1f);
                if (i == iconIndex) g.outline(b.getX() + 4, b.getY() + 3,
                        b.getWidth() - 8, b.getHeight() - 6, 0xFFFFE4A2);
            }
        }
        if (popup == Popup.CATEGORY) {
            List<String> categories = AtlasCategories.all(Minecraft.getInstance());
            for (int i = 0; i < categoryRows.size(); i++) {
                int index = categoryScroll + i;
                if (index < categories.size() && categories.get(index).equalsIgnoreCase(popupCategory)) {
                    Button b = categoryRows.get(i);
                    g.outline(b.getX() - 1, b.getY() - 1, b.getWidth() + 2, b.getHeight() + 2, 0xFFD8AB50);
                }
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
