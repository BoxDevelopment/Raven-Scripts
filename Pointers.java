String colorSymbol = util.colorSymbol;

void onLoad() {
    modules.registerDescription(scriptName);
    modules.registerSlider("Range", "blocks", 100, 0, 500, 5);
    modules.registerSlider("Radius", "pixels", 50, 30, 200, 5);
    modules.registerSlider("Color R", "", 255, 0, 255, 1);
    modules.registerSlider("Color G", "", 0, 0, 255, 1);
    modules.registerSlider("Color B", "", 0, 0, 255, 1);
    modules.registerSlider("Scale", "", 1.0, 0.5, 2.0, 0.1);
    modules.registerButton("Show Bots", false);
    modules.registerButton("Show Distance", true);
    modules.registerSlider("Distance Text Scale", "", 0.8, 0.5, 1.5, 0.05);
}

void onRenderTick(float partialTicks) {
    if (client.getScreen() != null && !client.getScreen().isEmpty()) return;
    Entity player = client.getPlayer();
    if (player == null) return;

    double rng = modules.getSlider(scriptName, "Range");
    double radiusInput = modules.getSlider(scriptName, "Radius");
    int r = (int) modules.getSlider(scriptName, "Color R");
    int g = (int) modules.getSlider(scriptName, "Color G");
    int b = (int) modules.getSlider(scriptName, "Color B");
    float scale = (float) modules.getSlider(scriptName, "Scale");
    boolean showBots = modules.getButton(scriptName, "Show Bots");
    boolean showDist = modules.getButton(scriptName, "Show Distance");
    float distScale = (float) modules.getSlider(scriptName, "Distance Text Scale");

    int color = (255 << 24) | (r << 16) | (g << 8) | b;
    int[] resSize = client.getDisplaySize();
    double baseX = resSize[0] / 2.0;
    double baseY = resSize[1] / 2.0;

    Vec3 playerPos = player.getPosition();

    for (Entity en : world.getPlayerEntities()) {
        if (en == player || en.isDead() || en.getHealth() <= 0) continue;

        if (!showBots) {
            NetworkPlayer net = en.getNetworkPlayer();
            if (net == null) continue;
            String name = en.getName();
            if (name != null) {
                if (name.indexOf(colorSymbol) != -1) continue;
                if (name.equalsIgnoreCase("Bot") || name.equalsIgnoreCase("NPC") || name.length() <= 2) continue;
            }
        }

        Vec3 targetPos = en.getPosition();
        double dist = playerPos.distanceTo(targetPos);
        if (dist > rng) continue;

        Vec3 lastPos = en.getLastPosition();
        double x = lastPos.x + (targetPos.x - lastPos.x) * partialTicks;
        double y = lastPos.y + (targetPos.y - lastPos.y) * partialTicks + (en.getEyeHeight() / 2);
        double z = lastPos.z + (targetPos.z - lastPos.z) * partialTicks;

        Vec3 vec = render.worldToScreen(x, y, z, 2, partialTicks);
        if (vec == null) continue;

        double dx = vec.x - baseX;
        double dy = vec.y - baseY;
        boolean inFrustum = vec.z < 1.0;
        if (!inFrustum) {
            dx *= -1.0;
            dy *= -1.0;
        }

        double anglePosition = Math.atan2(dx, dy);
        double angleRotation = Math.atan2(dy, dx) * (180.0 / Math.PI) + 90.0;
        double hypotenuse = Math.hypot(dx, dy);

        if (inFrustum && hypotenuse < radiusInput + 10.0) {
            continue;
        }

        double renderX = baseX + radiusInput * Math.sin(anglePosition);
        double renderY = baseY + radiusInput * Math.cos(anglePosition);

        gl.push();
        gl.translate((float)renderX, (float)renderY, 0.0f);
        gl.rotate((float)angleRotation, 0.0f, 0.0f, 1.0f);
        gl.scale(scale, scale, 1.0f);

        for (float i = 0; i < 6; i += 0.5f) {
            float width = i;
            render.rect(-width/2, i - 3, width/2, (i + 0.5f) - 3, color);
        }
        render.line2D(-3.0, 3.0, 0.0, -3.0, 1.5f, 0xFF000000);
        render.line2D(0.0, -3.0, 3.0, 3.0, 1.5f, 0xFF000000);
        render.line2D(3.0, 3.0, -3.0, 3.0, 1.5f, 0xFF000000);
        gl.pop();

        if (showDist) {
            int distInt = (int) Math.round(dist);
            String distText = distInt + "m";
            float textX = (float) (renderX - render.getFontWidth(distText) / 2.0);
            float textY = (float) (renderY + 12 * scale);
            render.text(distText, textX, textY, distScale, 0xFFFFFFFF, true);
        }
    }
}