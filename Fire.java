
List<Integer> fireballDistances = new ArrayList<>();

float openingScale = 0;
int dragX = 600;
int dragY = 600;
float adjustedX, adjustedY;
int firstX, firstY;
boolean dragging = false;
boolean firstClick = true;

final int WIDTH = 67;
final int HEIGHT = 14;

void onLoad() {
    modules.registerDescription("Fireball tracker with Scale Animation & Dragging.");
    modules.registerSlider("Search Range", "", 250.0, 1.0, 800.0, 1.0);
    modules.registerSlider("Anim Speed", "", 10, 1, 30, 1);

    if (config.get("fire_x") != null) {
        dragX = Integer.parseInt(config.get("fire_x"));
    }
    if (config.get("fire_y") != null) {
        dragY = Integer.parseInt(config.get("fire_y"));
    }

    syncCoords();
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    if (player == null) {
        return;
    }

    List<Entity> entities = world.getEntities();
    fireballDistances.clear();

    for (Entity e : entities) {
        if (e != player && (e.type.contains("Fireball") || e.type.contains("fireball"))) {
            int distance = (int) player.getPosition().distanceTo(e.getPosition());
            if (distance <= modules.getSlider(scriptName, "Search Range")) {
                fireballDistances.add(distance);
            }
        }
    }
}

void onRenderTick(float partialTicks) {
    boolean inChat = client.getScreen().toLowerCase().contains("chat");
    if (inChat) {
        dragLogic(WIDTH * 2, HEIGHT * 2);
    }

    float animSpeed = (float) modules.getSlider(scriptName, "Anim Speed") / 100f;

    if (!fireballDistances.isEmpty() || inChat) {
        openingScale = lerp(openingScale, 1.0f, animSpeed);
    } else {
        openingScale = lerp(openingScale, 0.0f, animSpeed);
    }

    if (openingScale < 0.01f) {
        return;
    }

    gl.push();

    float centerX = adjustedX + (WIDTH / 2.0f);
    float centerY = adjustedY + (HEIGHT / 2.0f);

    gl.translate(centerX, centerY, 0);
    gl.scale(openingScale, openingScale, 1.0f);
    gl.translate(-centerX, -centerY, 0);

    float x = adjustedX;
    float y = adjustedY;

    if (fireballDistances.isEmpty() && inChat) {
        String preview = util.colorSymbol + "7Fireball: " + util.colorSymbol + "c82m";
        render.text(preview, x + 2, y + 3, 1, 0xFFFFFFFF, true);
    }

    int alpha = (int) (openingScale * 255);

    for (int i = 0; i < fireballDistances.size(); i++) {
        int distance = fireballDistances.get(i);
        String color = "a";

        if (distance < 20) {
            color = "c";
        } else if (distance < 60) {
            color = "6";
        }

        String text = util.colorSymbol + "fFireball: " + util.colorSymbol + color + distance + "m";
        render.text(text, x, y + (i * 12), 1, (alpha << 24) | 0xFFFFFF, true);
    }

    if (dragging) {
        render.rect(x, y, x + WIDTH, y + HEIGHT, (int) (openingScale * 40) << 24 | 0xFFFFFF);
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

        config.set("fire_x", String.valueOf(dragX));
        config.set("fire_y", String.valueOf(dragY));
    }
}

void syncCoords() {
    adjustedX = dragX / 2.0f;
    adjustedY = dragY / 2.0f;
}

float lerp(float a, float b, float t) {
    return a + (b - a) * t;
}
