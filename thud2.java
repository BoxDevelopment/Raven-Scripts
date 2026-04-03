float openingScale = 0;
float healthAnim = 0;
long lastUpdate = 0;

int dragX = 600; 
int dragY = 600;
float adjustedX, adjustedY; 
int firstX, firstY;
boolean dragging = false;
boolean firstClick = true;

final int EDGE_OFFSET = 4;
final int PADDING = 4;
final int INDENT = 4;
final int FACE_SCALE = 32;
final int HEALTH_BAR_WIDTH = 100;
final int WIDTH = EDGE_OFFSET + FACE_SCALE + PADDING + HEALTH_BAR_WIDTH + INDENT + EDGE_OFFSET;
final int HEIGHT = FACE_SCALE + EDGE_OFFSET * 2;

void onLoad() {
    modules.registerDescription("BMS TargetHUD with Scale & Fade-out");
    modules.registerSlider("Anim Speed", "", 10, 1, 30, 1);
    
    if (config.get("bms_x") != null) dragX = Integer.parseInt(config.get("bms_x"));
    if (config.get("bms_y") != null) dragY = Integer.parseInt(config.get("bms_y"));
    
    syncCoords();
}

void onRenderTick(float partialTicks) {
    Entity target = modules.getKillAuraTarget();
    boolean inChat = client.getScreen().toLowerCase().contains("chat");
    
    if (target == null && inChat) {
        target = client.getPlayer();
    }

    float animSpeed = (float) modules.getSlider(scriptName, "Anim Speed") / 100f;
    if (target != null) {
        openingScale = lerp(openingScale, 1.0f, animSpeed);
    } else {
        openingScale = lerp(openingScale, 0.0f, animSpeed);
    }

    if (openingScale < 0.01f) return;

    int alpha = (int)(openingScale * 255);
    int bgAlpha = (int)(openingScale * 187); // Max 0xBB (187) for the background
    int shadowAlpha = (int)(openingScale * 48); // Max 0x30 (48) for the shadow

    float targetHealth = (target != null) ? target.getHealth() : 0;
    if (client.time() - lastUpdate >= 10) {
        healthAnim = lerp(healthAnim, targetHealth, animSpeed);
        lastUpdate = client.time();
    }

    if (inChat) dragLogic(WIDTH * 2, HEIGHT * 2);

    gl.push();

    float centerX = adjustedX + (WIDTH / 2.0f);
    float centerY = adjustedY + (HEIGHT / 2.0f);

    gl.translate(centerX, centerY, 0);
    gl.scale(openingScale, openingScale, 1.0f);
    gl.translate(-centerX, -centerY, 0);

    float x = adjustedX;
    float y = adjustedY;
    
    render.rect(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, (shadowAlpha << 24)); // Shadow
    render.rect(x, y, x + WIDTH, y + HEIGHT, (bgAlpha << 24) | 0x181818); // Main BG

    int hurtColor = (target != null && target.getHurtTime() > 0) ? 0x80FF0000 : 0x40FFFFFF;
    int headAlpha = (int)(openingScale * ((hurtColor >> 24) & 0xFF));
    render.rect(x + EDGE_OFFSET, y + EDGE_OFFSET, x + EDGE_OFFSET + FACE_SCALE, y + EDGE_OFFSET + FACE_SCALE, (headAlpha << 24) | (hurtColor & 0xFFFFFF));
    
    String name = (target != null) ? target.getName() : "Unknown";
    render.text(name, x + EDGE_OFFSET + FACE_SCALE + PADDING, y + EDGE_OFFSET + 2, 1.0f, (alpha << 24) | 0xFFFFFF, true);

    float barX = x + EDGE_OFFSET + FACE_SCALE + PADDING;
    float barY = y + HEIGHT - EDGE_OFFSET - 14;
    float barHeight = 10;
    
    render.rect(barX, barY, barX + HEALTH_BAR_WIDTH, barY + barHeight, (int)(openingScale * 96) << 24);
    
    float healthWidth = (healthAnim / 20.0f) * HEALTH_BAR_WIDTH;
    if (healthWidth > HEALTH_BAR_WIDTH) healthWidth = HEALTH_BAR_WIDTH;
    
    render.rect(barX, barY, barX + healthWidth, barY + barHeight, (alpha << 24) | 0x00A2FF);

    int healthPercent = (int)((targetHealth / 20.0f) * 100);
    if (healthPercent > 100) healthPercent = 100;
    String hpText = healthPercent + "%";
    float textX = barX + (HEALTH_BAR_WIDTH / 2) - (render.getFontWidth(hpText) / 2);
    render.text(hpText, textX, barY + 1, 1.0f, (alpha << 24) | 0xFFFFFF, true);

    if (dragging) {
        render.rect(x, y, x + WIDTH, y + HEIGHT, (int)(openingScale * 40) << 24 | 0xFFFFFF);
    }

    gl.pop();
}

void dragLogic(int offsetX, int offsetY) {
    int[] displaySize = client.getDisplaySize();
    int[] position = keybinds.getMousePosition();
    position[1] = displaySize[1] * 2 - position[1];

    if (keybinds.isMouseDown(0) && firstClick) {
        firstX = position[0];
        firstY = position[1];
        firstClick = false;
        if (dragX <= firstX && firstX <= dragX + offsetX && dragY <= firstY && firstY <= dragY + offsetY) {
            dragging = true;
        }
    }

    if (!keybinds.isMouseDown(0)) {
        firstClick = true;
        dragging = false;
    }

    if (dragging) {
        int deltaX = position[0] - firstX;
        int deltaY = position[1] - firstY;
        dragX += deltaX;
        dragY += deltaY;
        syncCoords();
        firstX = position[0];
        firstY = position[1];
        config.set("bms_x", String.valueOf(dragX));
        config.set("bms_y", String.valueOf(dragY));
    }
}

void syncCoords() {
    adjustedX = dragX / 2.0f;
    adjustedY = dragY / 2.0f;
}

float lerp(float a, float b, float t) {
    return a + (b - a) * t;
}