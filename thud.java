String lastDisplayedPlayerName = "";
float lastDisplayedPlayerHealth = 0.0f;
long lastDisplayTime = 0;
long fadeStartTime = 0; 
long fadeDuration = 400; 
final int MIN_BOX_WIDTH = 100;

int dragX = 500;
int dragY = 500;
int x, y;
float adjustedX, adjustedY;
int firstX, firstY;
boolean track = false;
boolean firstClick = true;
boolean inChat = false;

void onLoad() {
    modules.registerSlider("Offset X", "", 0.0, -100.0, 100.0, 1);
    modules.registerSlider("Offset Y", "", 0.0, -100.0, 100.0, 1);
    modules.registerButton("W/L", false);
    
    if (config.get("dragX") != null && config.get("dragY") != null) {
        dragX = (int) Integer.parseInt(config.get("dragX"));
        dragY = (int) Integer.parseInt(config.get("dragY"));
    }
    x = dragX;
    y = dragY;
    adjustedX = x / 2f;
    adjustedY = y / 2f;
}

void onPreUpdate() {
    inChat = client.getScreen().contains("Chat");
}

void onRenderTick(float partialTicks) {
    if (!client.getScreen().isEmpty() && !inChat) {
        return;
    }

    Entity killAuraTarget = modules.getKillAuraTarget();
    Entity self = client.getPlayer();
    
    if (killAuraTarget != null) {
        lastDisplayedPlayerName = killAuraTarget.getDisplayName();
        lastDisplayedPlayerHealth = killAuraTarget.getHealth();
        lastDisplayTime = client.time(); 
        fadeStartTime = lastDisplayTime; 
    } else if (inChat) {
        lastDisplayedPlayerName = self.getDisplayName();
        lastDisplayedPlayerHealth = self.getHealth();
    }

    float fadeProgress = (float) (client.time() - fadeStartTime) / fadeDuration;
    if (fadeProgress > 1.0f) {
        fadeProgress = 1.0f;
    }

    if (inChat || (client.time() - lastDisplayTime <= fadeDuration)) {
        float maxHealth = 20.0f;

        int nameWidth = render.getFontWidth("Target: " + lastDisplayedPlayerName);
        int textHeight = render.getFontHeight(); 
        int padding = 5;

        int boxWidth = Math.max(nameWidth + 2 * padding, MIN_BOX_WIDTH); 
        int boxHeight = 2 * textHeight + 3 * padding; 

        if (inChat) {
            dragLogic(boxWidth * 2, boxHeight * 2);
        } else {
            track = false;
        }

        double offsetX = modules.getSlider(scriptName, "Offset X");
        double offsetY = modules.getSlider(scriptName, "Offset Y");
        
        int startX = (int) adjustedX + (int) offsetX;
        int startY = (int) adjustedY + (int) offsetY;
        int endX = startX + boxWidth;
        int endY = startY + boxHeight;

        int initialOpacity = 0x80; 
        int finalOpacity = 0x00;
        int currentOpacity = inChat ? initialOpacity : (int) lerp(initialOpacity, finalOpacity, fadeProgress);
        
        render.rect(startX, startY, endX, endY, currentOpacity << 24);

        render.text("Target: " + lastDisplayedPlayerName, startX + padding, startY + padding, 1, 0xFFFFFFFF, true);
        String healthText = "Health: ";
        Color healthNumberColor;
        if (lastDisplayedPlayerHealth >= maxHealth * 0.75) {
            healthNumberColor = Color.green;
        } else if (lastDisplayedPlayerHealth >= maxHealth * 0.5) {
            healthNumberColor = Color.yellow;
        } else if (lastDisplayedPlayerHealth >= maxHealth * 0.25) {
            healthNumberColor = Color.orange;
        } else {
            healthNumberColor = Color.red;
        }

        render.text(healthText, startX + padding, startY + textHeight + 2 * padding, 1, Color.WHITE.getRGB(), true);
        render.text(String.format("%.1f", lastDisplayedPlayerHealth), startX + padding + render.getFontWidth(healthText), startY + textHeight + 2 * padding, 1, healthNumberColor.getRGB(), true);

        if (modules.getButton(scriptName, "W/L")) {
            float playerHealth = self.getHealth();
            String winLossText = (playerHealth >= lastDisplayedPlayerHealth) ? "W" : "L";
            Color winLossColor = (playerHealth >= lastDisplayedPlayerHealth) ? Color.green : Color.red;
            
            int winLossX = startX + padding + render.getFontWidth(healthText + String.format("%.1f", lastDisplayedPlayerHealth)) + padding;
            render.text(winLossText, winLossX, startY + textHeight + 2 * padding, 1, winLossColor.getRGB(), true);
        }

        float healthBarHeight = (boxHeight - 2) * (lastDisplayedPlayerHealth / maxHealth);
        float lineWidth = 2.0f;
        int backgroundColor = (currentOpacity << 24) | 0x808080; 
        render.line2D(startX - lineWidth + 2, startY, startX - lineWidth + 2, endY, lineWidth, backgroundColor);

        int healthBarColor;
        if (lastDisplayedPlayerHealth >= maxHealth * 0.75) {
            healthBarColor = 0x00FF00; 
        } else if (lastDisplayedPlayerHealth >= maxHealth * 0.5) {
            healthBarColor = 0xFFFF00; 
        } else if (lastDisplayedPlayerHealth >= maxHealth * 0.25) {
            healthBarColor = 0xFFA500; 
        } else {
            healthBarColor = 0xFF0000;
        }

        healthBarColor = (currentOpacity << 24) | healthBarColor;
        render.line2D(startX - lineWidth + 2, endY, startX - lineWidth + 2, endY - healthBarHeight, lineWidth, healthBarColor);
        
        if (track) {
            render.line2D(startX, startY, endX, startY, 1, 0x96FFFFFF);
            render.line2D(startX, endY, endX, endY, 1, 0x96FFFFFF);
            render.line2D(startX, startY, startX, endY, 1, 0x96FFFFFF);
            render.line2D(endX, startY, endX, endY, 1, 0x96FFFFFF);
        }
    }
}

void dragLogic(int width, int height) {
    if (keybinds.isMouseDown(0) && firstClick) {
        int[] displaySize = client.getDisplaySize();
        int[] position = keybinds.getMousePosition();
        position[1] = displaySize[1] * 2 - position[1];
        firstX = position[0];
        firstY = position[1];
        firstClick = false;
        if (x <= firstX && firstX <= x + width && y <= firstY && firstY <= y + height) {
            track = true;
        }
    }
    if (!keybinds.isMouseDown(0)) {
        firstClick = true;
        track = false;
    }
    if (track) {
        int[] displaySize = client.getDisplaySize();
        int[] position = keybinds.getMousePosition();
        position[1] = displaySize[1] * 2 - position[1];
        int deltaX = position[0] - firstX;
        int deltaY = position[1] - firstY;
        dragX = dragX + deltaX;
        dragY = dragY + deltaY;
        x = dragX;
        y = dragY;
        adjustedX = x / 2f;
        adjustedY = y / 2f;
        firstX = firstX + deltaX;
        firstY = firstY + deltaY;
        config.set("dragX", Integer.toString(dragX));
        config.set("dragY", Integer.toString(dragY));
    }
}

float lerp(float a, float b, float t) {
    return a + (b - a) * t;
}