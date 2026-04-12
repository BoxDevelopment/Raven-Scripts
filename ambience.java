void onLoad() {
    modules.registerButton("Snow", true);
    modules.registerButton("Leaves", true);
    modules.registerSlider("Range", "blocks", 80, 8, 80, 1);
    modules.registerSlider("Top Height", "", 48, 4, 48, 1);
    modules.registerSlider("Bottom Depth", "", 20, 0, 20, 1);
    modules.registerSlider("Wind", "", 0.35, 0.00, 2.00, 0.01);
    modules.registerSlider("Time Scale", "", 1.00, 0.10, 3.00, 0.05);
    modules.registerButton("Use Depth", true);

    modules.registerDescription("Snow");
    modules.registerSlider("Snow Count", "", 500, 10, 500, 1);
    modules.registerSlider("Snow Size", "", 0.06, 0.01, 0.40, 0.01);
    modules.registerSlider("Snow Speed", "", 1.05, 0.05, 2.00, 0.05);
    modules.registerSlider("Snow R", "", 245, 0, 255, 1);
    modules.registerSlider("Snow G", "", 245, 0, 255, 1);
    modules.registerSlider("Snow B", "", 255, 0, 255, 1);
    modules.registerSlider("Snow Alpha", "", 170, 10, 255, 1);

    modules.registerDescription("Leaves");
    modules.registerSlider("Leaf Count", "", 350, 10, 350, 1);
    modules.registerSlider("Leaf Size", "", 0.23, 0.01, 0.45, 0.01);
    modules.registerSlider("Leaf Speed", "", 0.55, 0.05, 2.50, 0.05);
    modules.registerSlider("Leaf Spin", "", 3.30, 0.00, 8.00, 0.10);
    modules.registerSlider("Leaf R", "", 255, 0, 255, 1);
    modules.registerSlider("Leaf G", "", 85, 0, 255, 1);
    modules.registerSlider("Leaf B", "", 112, 0, 255, 1);
    modules.registerSlider("Leaf Alpha", "", 175, 10, 255, 1);
}

void onRenderWorld(float partialTicks) {
    Entity player = client.getPlayer();
    if (player == null) return;
    Vec3 origin = player.getPosition();
    if (origin == null) return;

    boolean enableSnow = modules.getButton(scriptName, "Snow");
    boolean enableLeaves = modules.getButton(scriptName, "Leaves");
    if (!enableSnow && !enableLeaves) return;

    double spreadRadius = modules.getSlider(scriptName, "Range");
    double spawnHeight = modules.getSlider(scriptName, "Top Height");
    double despawnDepth = modules.getSlider(scriptName, "Bottom Depth");
    double windStrength = modules.getSlider(scriptName, "Wind");
    double timeScale = modules.getSlider(scriptName, "Time Scale");
    boolean useDepth = modules.getButton(scriptName, "Use Depth");
    double fallDistance = spawnHeight + despawnDepth;

    Vec3 cameraPos = render.getPosition();
    double time = client.time() * 0.001 * timeScale;

    beginParticleRender(useDepth);

    if (enableSnow) {
        drawSnow(origin, cameraPos, time, spreadRadius, spawnHeight, fallDistance, windStrength);
    }
    if (enableLeaves) {
        drawLeaves(origin, cameraPos, time, spreadRadius, spawnHeight, fallDistance, windStrength);
    }

    endParticleRender();
}

void beginParticleRender(boolean useDepth) {
    gl.push();
    gl.texture2d(false);
    gl.blend(true);
    gl.alpha(false);
    gl.cull(false);
    gl.depth(useDepth);
    gl.depthMask(false);
    gl.lineSmooth(true);
}

void endParticleRender() {
    gl.lineSmooth(false);
    gl.depthMask(true);
    gl.depth(true);
    gl.cull(true);
    gl.alpha(true);
    gl.blend(false);
    gl.texture2d(true);
    gl.resetColor();
    gl.pop();
}

void drawSnow(Vec3 origin, Vec3 cameraPos, double time, double spreadRadius, double spawnHeight, double fallDistance, double windStrength) {
    int particleCount = (int) modules.getSlider(scriptName, "Snow Count");
    double flakeSize = modules.getSlider(scriptName, "Snow Size");
    double fallSpeed = modules.getSlider(scriptName, "Snow Speed");
    int red = (int) modules.getSlider(scriptName, "Snow R");
    int green = (int) modules.getSlider(scriptName, "Snow G");
    int blue = (int) modules.getSlider(scriptName, "Snow B");
    int baseAlpha = (int) modules.getSlider(scriptName, "Snow Alpha");

    double originX = origin.x, originY = origin.y, originZ = origin.z;
    double camX = cameraPos.x, camY = cameraPos.y, camZ = cameraPos.z;
    double windX = windStrength * 0.45;
    double windZ = windStrength * 0.45;

    gl.begin(7); 
    for (int particle = 0; particle < particleCount; particle++) {
        double seed = particle * 17.123 + 0.51;
        double spawnX = originX + centeredNoise(seed + 3.1) * 2.0 * spreadRadius;
        double spawnZ = originZ + centeredNoise(seed + 8.7) * 2.0 * spreadRadius;
        double life = frac(time * fallSpeed + hash01(seed + 19.3));
        double y = originY + spawnHeight - life * fallDistance;

        double driftX = Math.sin(time * 0.9 + seed * 0.7) * windX;
        double driftZ = Math.cos(time * 0.8 + seed * 0.6) * windZ;
        double x = spawnX + driftX;
        double z = spawnZ + driftZ;

        double halfHeight = flakeSize * (0.55 + hash01(seed + 27.1) * 1.1);
        double halfWidth = halfHeight * 0.6;
        int alpha = (int) (baseAlpha * (0.60 + (1.0 - life) * 0.40));
        if (alpha <= 0) continue;

        double sx = x - camX;
        double sy = y - camY;
        double sz = z - camZ;

        gl.color(red, green, blue, alpha);
        gl.vertex3(sx - halfWidth, sy - halfHeight, sz);
        gl.vertex3(sx + halfWidth, sy - halfHeight, sz);
        gl.vertex3(sx + halfWidth, sy + halfHeight, sz);
        gl.vertex3(sx - halfWidth, sy + halfHeight, sz);

        gl.vertex3(sx, sy - halfHeight, sz - halfWidth);
        gl.vertex3(sx, sy - halfHeight, sz + halfWidth);
        gl.vertex3(sx, sy + halfHeight, sz + halfWidth);
        gl.vertex3(sx, sy + halfHeight, sz - halfWidth);
    }
    gl.end();
}

void drawLeaves(Vec3 origin, Vec3 cameraPos, double time, double spreadRadius, double spawnHeight, double fallDistance, double windStrength) {
    int particleCount = (int) modules.getSlider(scriptName, "Leaf Count");
    double leafSize = modules.getSlider(scriptName, "Leaf Size");
    double fallSpeed = modules.getSlider(scriptName, "Leaf Speed");
    double spinSpeed = modules.getSlider(scriptName, "Leaf Spin");
    int baseRed = (int) modules.getSlider(scriptName, "Leaf R");
    int baseGreen = (int) modules.getSlider(scriptName, "Leaf G");
    int baseBlue = (int) modules.getSlider(scriptName, "Leaf B");
    int baseAlpha = (int) modules.getSlider(scriptName, "Leaf Alpha");

    double originX = origin.x, originY = origin.y, originZ = origin.z;
    double camX = cameraPos.x, camY = cameraPos.y, camZ = cameraPos.z;

    gl.begin(7);
    for (int particle = 0; particle < particleCount; particle++) {
        double seed = particle * 9.917 + 11.73;
        double spawnX = originX + centeredNoise(seed + 1.9) * 2.0 * spreadRadius;
        double spawnZ = originZ + centeredNoise(seed + 6.2) * 2.0 * spreadRadius;
        double life = frac(time * fallSpeed + hash01(seed + 14.7));
        double y = originY + spawnHeight - life * fallDistance;

        double spin = time * spinSpeed + seed * 0.5;
        double driftX = Math.sin(spin * 0.8) * windStrength * 0.8 + Math.sin(time * 1.7 + seed) * 0.07;
        double driftZ = Math.cos(spin * 0.9) * windStrength * 0.8 + Math.cos(time * 1.9 + seed) * 0.07;
        double x = spawnX + driftX;
        double z = spawnZ + driftZ;

        double size = leafSize * (0.65 + hash01(seed + 25.0) * 0.9);
        int red = clampColor((int) (baseRed + centeredNoise(seed + 31.0) * 34.0));
        int green = clampColor((int) (baseGreen + centeredNoise(seed + 37.0) * 28.0));
        int blue = clampColor((int) (baseBlue + centeredNoise(seed + 43.0) * 26.0));
        int alpha = (int) (baseAlpha * (0.60 + (1.0 - life) * 0.40));
        if (alpha <= 0) continue;

        double sx = x - camX;
        double sy = y - camY;
        double sz = z - camZ;

        double cosA = Math.cos(spin);
        double sinA = Math.sin(spin);
        double uX = cosA * size;
        double uZ = sinA * size;
        double vX = -sinA * size * 0.55;
        double vZ = cosA * size * 0.55;
        double vY = Math.sin(spin * 1.7) * size * 0.22;

        gl.color(red, green, blue, alpha);
        gl.vertex3(sx + uX + vX, sy + vY, sz + uZ + vZ);
        gl.vertex3(sx + uX - vX, sy - vY, sz + uZ - vZ);
        gl.vertex3(sx - uX - vX, sy - vY, sz - uZ - vZ);
        gl.vertex3(sx - uX + vX, sy + vY, sz - uZ + vZ);
    }
    gl.end();
}

double centeredNoise(double x) {
    return hash01(x) - 0.5;
}

double hash01(double x) {
    double s = Math.sin(x * 12.9898 + 78.233) * 43758.5453123;
    return s - Math.floor(s);
}

double frac(double x) {
    return x - Math.floor(x);
}

int clampColor(int v) {
    if (v < 0) return 0;
    if (v > 255) return 255;
    return v;
}
