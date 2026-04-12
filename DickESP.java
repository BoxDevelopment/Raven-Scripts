void onLoad() {
    modules.registerSlider("Range", "blocks", 90, 10, 250, 5);
    modules.registerSlider("Segments", "", 28, 12, 72, 1);
    modules.registerSlider("X Offset", "", 0.0, -2.0, 2.0, 0.01);
    modules.registerSlider("Forward Offset", "", 0.22, -1.5, 2.5, 0.01);
    modules.registerSlider("Y Offset", "", -0.225, -1.00, 1.00, 0.01);
    modules.registerSlider("Shaft Length", "", 1.40, 0.30, 3.00, 0.01);
    modules.registerSlider("Shaft Radius", "", 0.11, 0.03, 0.60, 0.01);
    modules.registerSlider("Ball Radius", "", 0.14, 0.04, 0.70, 0.01);
    modules.registerSlider("Tip Radius", "", 0.13, 0.04, 0.70, 0.01);
    modules.registerSlider("Color R", "", 255, 0, 255, 1);
    modules.registerSlider("Color G", "", 140, 0, 255, 1);
    modules.registerSlider("Color B", "", 255, 0, 255, 1);
    modules.registerSlider("Alpha", "", 210, 10, 255, 1);
    modules.registerButton("Use Depth", true);
    modules.registerButton("Show Yourself", false);
    modules.registerButton("Show Invis", true);
}

void onRenderWorld(float partialTicks) {
    Entity self = client.getPlayer();
    if (self == null) return;

    Vec3 selfPos = self.getPosition();
    if (selfPos == null) return;

    double range = modules.getSlider(scriptName, "Range");
    double rangeSq = range * range;
    int segments = (int) modules.getSlider(scriptName, "Segments");
    if (segments < 12) segments = 12;

    double xOffset = modules.getSlider(scriptName, "X Offset");
    double forwardOffset = modules.getSlider(scriptName, "Forward Offset");
    double yOffset = modules.getSlider(scriptName, "Y Offset");
    double shaftLength = modules.getSlider(scriptName, "Shaft Length");
    double shaftRadius = modules.getSlider(scriptName, "Shaft Radius");
    double ballRadius = modules.getSlider(scriptName, "Ball Radius");
    double tipRadius = modules.getSlider(scriptName, "Tip Radius");

    int r = (int) modules.getSlider(scriptName, "Color R");
    int g = (int) modules.getSlider(scriptName, "Color G");
    int b = (int) modules.getSlider(scriptName, "Color B");
    int alpha = (int) modules.getSlider(scriptName, "Alpha");
    boolean useDepth = modules.getButton(scriptName, "Use Depth");
    boolean showSelf = modules.getButton(scriptName, "Show Yourself");
    boolean showInvis = modules.getButton(scriptName, "Show Invis");

    Vec3 cam = render.getPosition();

    gl.push();
    gl.texture2d(false);
    gl.blend(true);
    gl.alpha(false);
    gl.cull(false);
    gl.depth(useDepth);
    gl.depthMask(false);
    gl.lineSmooth(true);

    for (Entity e : world.getPlayerEntities()) {
        if (e == null || e.isDead() || e.getHealth() <= 0) continue;
        if (!showSelf && e.equals(self)) continue;
        if (!showInvis && e.isInvisible()) continue;

        Vec3 cur = e.getPosition();
        if (cur == null) continue;
        double dxSelf = cur.x - selfPos.x;
        double dzSelf = cur.z - selfPos.z;
        if (dxSelf * dxSelf + dzSelf * dzSelf > rangeSq) continue;

        Vec3 last = e.getLastPosition();
        if (last == null) last = cur;

        double px = last.x + (cur.x - last.x) * partialTicks;
        double py = last.y + (cur.y - last.y) * partialTicks;
        double pz = last.z + (cur.z - last.z) * partialTicks;

        double baseY = py + e.getHeight() * 0.5 + yOffset;
        double yawRad = Math.toRadians(e.getYaw());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);

        double centerX = px + rx * xOffset + fx * forwardOffset;
        double centerZ = pz + rz * xOffset + fz * forwardOffset;

        double shaftStartX = centerX + fx * 0.075;
        double shaftStartY = baseY;
        double shaftStartZ = centerZ + fz * 0.075;
        double shaftEndX = shaftStartX + fx * shaftLength;
        double shaftEndY = baseY;
        double shaftEndZ = shaftStartZ + fz * shaftLength;

        double ballLeftX = centerX + rx * (-0.08) + fx * (-0.05);
        double ballLeftY = baseY;
        double ballLeftZ = centerZ + rz * (-0.08) + fz * (-0.05);

        double ballRightX = centerX + rx * (0.08) + fx * (-0.05);
        double ballRightY = baseY;
        double ballRightZ = centerZ + rz * (0.08) + fz * (-0.05);

        double tipX = shaftEndX + fx * 0.065;
        double tipY = baseY;
        double tipZ = shaftEndZ + fz * 0.065;

        drawCylinder(cam, shaftStartX, shaftStartY, shaftStartZ, shaftEndX, shaftEndY, shaftEndZ, shaftRadius, segments, r, g, b, alpha);
        drawSphere(cam, ballLeftX, ballLeftY, ballLeftZ, ballRadius, segments, r, g, b, alpha);
        drawSphere(cam, ballRightX, ballRightY, ballRightZ, ballRadius, segments, r, g, b, alpha);
        drawSphere(cam, tipX, tipY, tipZ, tipRadius, segments, r, g, b, alpha);
    }

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

void drawSphere(Vec3 cam, double cx, double cy, double cz, double radius, int segments, int r, int g, int b, int alpha) {
    if (alpha <= 0 || radius <= 0.001) return;
    int latSteps = Math.max(6, segments / 2);
    int lonSteps = Math.max(10, segments);

    for (int lat = 0; lat < latSteps; lat++) {
        double p1 = -Math.PI * 0.5 + (Math.PI * lat) / latSteps;
        double p2 = -Math.PI * 0.5 + (Math.PI * (lat + 1)) / latSteps;
        double cp1 = Math.cos(p1), sp1 = Math.sin(p1);
        double cp2 = Math.cos(p2), sp2 = Math.sin(p2);

        gl.begin(8);
        for (int lon = 0; lon <= lonSteps; lon++) {
            double t = (Math.PI * 2.0 * lon) / lonSteps;
            double ct = Math.cos(t), st = Math.sin(t);

            gl.color(r, g, b, alpha);
            gl.vertex3(cx - cam.x + ct * cp1 * radius, cy - cam.y + sp1 * radius, cz - cam.z + st * cp1 * radius);
            gl.vertex3(cx - cam.x + ct * cp2 * radius, cy - cam.y + sp2 * radius, cz - cam.z + st * cp2 * radius);
        }
        gl.end();
    }
}

void drawCylinder(Vec3 cam, double x0, double y0, double z0, double x1, double y1, double z1, double radius, int segments, int r, int g, int b, int alpha) {
    if (alpha <= 0 || radius <= 0.001) return;

    double ax = x1 - x0;
    double ay = y1 - y0;
    double az = z1 - z0;
    double len = Math.sqrt(ax * ax + ay * ay + az * az);
    if (len < 0.0001) return;
    ax /= len;
    ay /= len;
    az /= len;

    double refX = 0.0, refY = 1.0, refZ = 0.0;
    if (Math.abs(ay) > 0.98) {
        refX = 1.0; refY = 0.0; refZ = 0.0;
    }

    double ux = ay * refZ - az * refY;
    double uy = az * refX - ax * refZ;
    double uz = ax * refY - ay * refX;
    double uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
    if (uLen < 0.0001) return;
    ux /= uLen;
    uy /= uLen;
    uz /= uLen;

    double vx = uy * az - uz * ay;
    double vy = uz * ax - ux * az;
    double vz = ux * ay - uy * ax;

    double step = (Math.PI * 2.0) / (double) segments;
    gl.begin(8);
    for (int i = 0; i <= segments; i++) {
        int idx = i % segments;
        double a = idx * step;
        double ca = Math.cos(a);
        double sa = Math.sin(a);

        double ox = (ux * ca + vx * sa) * radius;
        double oy = (uy * ca + vy * sa) * radius;
        double oz = (uz * ca + vz * sa) * radius;

        gl.color(r, g, b, alpha);
        gl.vertex3(x0 - cam.x + ox, y0 - cam.y + oy, z0 - cam.z + oz);
        gl.vertex3(x1 - cam.x + ox, y1 - cam.y + oy, z1 - cam.z + oz);
    }
    gl.end();
}
