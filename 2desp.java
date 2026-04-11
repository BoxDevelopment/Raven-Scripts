String colorSymbol = util.colorSymbol;
int status = -1;
long lastStatusCheck = 0;
Map<Integer, Integer> botFirstSeenTicks = new HashMap<>();
Map<Integer, Boolean> botEverMovedHoriz = new HashMap<>();
Map<Integer, Boolean> botAlwaysInvisible = new HashMap<>();

void onLoad() {
    modules.registerSlider("Mode", "", 0, 0, 1, 1);
    modules.registerSlider("Box R", "", 255, 0, 255, 1);
    modules.registerSlider("Box G", "", 255, 0, 255, 1);
    modules.registerSlider("Box B", "", 255, 0, 255, 1);
    modules.registerSlider("Box Alpha", "", 100, 0, 255, 1);
    modules.registerButton("Filled", false);
    modules.registerSlider("Fill R", "", 255, 0, 255, 1);
    modules.registerSlider("Fill G", "", 255, 0, 255, 1);
    modules.registerSlider("Fill B", "", 255, 0, 255, 1);
    modules.registerSlider("Fill Alpha", "", 40, 0, 255, 1);
    modules.registerSlider("Health R", "", 0, 0, 255, 1);
    modules.registerSlider("Health G", "", 255, 0, 255, 1);
    modules.registerSlider("Health B", "", 0, 0, 255, 1);
    modules.registerButton("Show Health", true);
    modules.registerButton("Ignore Bots", true);
    modules.registerButton("Lobby Check", true);
    modules.registerSlider("Range", "blocks", 100, 10, 500, 10);
    modules.registerButton("Bot Check: Invalid UUID", true);
    modules.registerButton("Bot Check: UUID Stationary Only", true);
    modules.registerButton("Bot Check: Always Invisible", true);
    modules.registerButton("Bot Check: Always Stationary", true);
    modules.registerButton("Bot Check: Entity Age", true);
    modules.registerSlider("Bot Min Age", "ticks", 20, 0, 200, 1);
}

void onEnable() {
    clearBotTracking();
}

void onDisable() {
    clearBotTracking();
}

void clearBotTracking() {
    botFirstSeenTicks.clear();
    botEverMovedHoriz.clear();
    botAlwaysInvisible.clear();
}

void onPreUpdate() {
    long now = client.time();
    if (now - lastStatusCheck > 1000) {
        status = getBedwarsStatus();
        lastStatusCheck = now;
    }
}

int getBedwarsStatus() {
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }

    int size = sidebar.size();
    if (size < 7) return -1;

    if (!util.strip(sidebar.get(0)).startsWith("BED WARS")) {
        return -1;
    }

    if (util.strip(sidebar.get(5)).startsWith("R Red:") &&
        util.strip(sidebar.get(6)).startsWith("B Blue:")) {
        return 3;
    }

    String six = util.strip(sidebar.get(6));
    if (six.equals("Waiting...") || six.startsWith("Starting in")) {
        return 2;
    }

    return -1;
}

void onRenderTick(float partialTicks) {
    Entity player = client.getPlayer();
    if (player == null) return;
    if (modules.getButton(scriptName, "Lobby Check") && status != 3) return;

    boolean realMode = (int) modules.getSlider(scriptName, "Mode") == 0;
    boolean showHealth = modules.getButton(scriptName, "Show Health");
    boolean ignoreBots = modules.getButton(scriptName, "Ignore Bots");
    boolean filled = modules.getButton(scriptName, "Filled");
    double range = modules.getSlider(scriptName, "Range");
    boolean checkInvalidUUID = modules.getButton(scriptName, "Bot Check: Invalid UUID");
    boolean invalidUUIDStationaryOnly = modules.getButton(scriptName, "Bot Check: UUID Stationary Only");
    boolean checkAlwaysInvisible = modules.getButton(scriptName, "Bot Check: Always Invisible");
    boolean checkAlwaysStationary = modules.getButton(scriptName, "Bot Check: Always Stationary");
    boolean checkEntityAge = modules.getButton(scriptName, "Bot Check: Entity Age");
    int minEntityAge = (int) modules.getSlider(scriptName, "Bot Min Age");

    int ra = (int) modules.getSlider(scriptName, "Box Alpha");
    int rr = (int) modules.getSlider(scriptName, "Box R");
    int rg = (int) modules.getSlider(scriptName, "Box G");
    int rb = (int) modules.getSlider(scriptName, "Box B");
    int boxColor = (ra << 24) | (rr << 16) | (rg << 8) | rb;

    int fa = (int) modules.getSlider(scriptName, "Fill Alpha");
    int fr = (int) modules.getSlider(scriptName, "Fill R");
    int fg = (int) modules.getSlider(scriptName, "Fill G");
    int fb = (int) modules.getSlider(scriptName, "Fill B");
    int fillColor = (fa << 24) | (fr << 16) | (fg << 8) | fb;

    int hr = (int) modules.getSlider(scriptName, "Health R");
    int hg = (int) modules.getSlider(scriptName, "Health G");
    int hb = (int) modules.getSlider(scriptName, "Health B");
    int healthColor = (255 << 24) | (hr << 16) | (hg << 8) | hb;

    int scale = client.getDisplaySize()[2];
    HashSet<Integer> activeIds = new HashSet<>();

    for (Entity e : world.getPlayerEntities()) {
        if (e == player || e.isDead() || e.getHealth() <= 0) continue;
        activeIds.add(e.entityId);
        if (player.getPosition().distanceTo(e.getPosition()) > range) continue;

        if (ignoreBots) {
            if (isBotEntity(e, checkInvalidUUID, invalidUUIDStationaryOnly, checkAlwaysInvisible, checkAlwaysStationary, checkEntityAge, minEntityAge)) continue;
        }

        Vec3 last = e.getLastPosition();
        Vec3 cur = e.getPosition();
        double x = last.x + (cur.x - last.x) * partialTicks;
        double y = last.y + (cur.y - last.y) * partialTicks;
        double z = last.z + (cur.z - last.z) * partialTicks;

        double w = e.getWidth() / 2;
        double h = e.getHeight();

        double bx1, by1, bx2, by2;

        if (realMode) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean hit = false;

            double[][] offsets = {{-w,-w},{-w,w},{w,-w},{w,w}};
            for (double[] o : offsets) {
                for (double oy : new double[]{0, h}) {
                    Vec3 s = render.worldToScreen(x + o[0], y + oy, z + o[1], scale, partialTicks);
                    if (s == null || s.z < 0 || s.z >= 1) continue;
                    hit = true;
                    if (s.x < minX) minX = s.x;
                    if (s.y < minY) minY = s.y;
                    if (s.x > maxX) maxX = s.x;
                    if (s.y > maxY) maxY = s.y;
                }
            }
            if (!hit) continue;
            bx1 = minX; by1 = minY; bx2 = maxX; by2 = maxY;
        } else {
            Vec3 top = render.worldToScreen(x, y + h, z, scale, partialTicks);
            Vec3 bot = render.worldToScreen(x, y, z, scale, partialTicks);
            if (top == null || bot == null || top.z < 0 || top.z >= 1) continue;
            double ph = bot.y - top.y;
            if (ph <= 0) continue;
            double pw = ph * 0.5;
            bx1 = top.x - pw / 2; by1 = top.y;
            bx2 = top.x + pw / 2; by2 = bot.y;
        }

        if (filled) {
            render.rect((float) bx1, (float) by1, (float) bx2, (float) by2, fillColor);
        }
        drawBox(bx1, by1, bx2, by2, boxColor);

        if (showHealth) {
            double pct = e.getHealth() / e.getMaxHealth();
            double barH = (by2 - by1) * pct;
            render.rect((float)(bx1 - 3), (float)(by2 - barH), (float)(bx1 - 1), (float)by2, healthColor);
        }
    }

    pruneBotTracking(activeIds);
}

boolean isBotEntity(Entity entity, boolean checkInvalidUUID, boolean invalidUUIDStationaryOnly, boolean checkAlwaysInvisible, boolean checkAlwaysStationary, boolean checkEntityAge, int minEntityAge) {
    updateBotState(entity);

    int id = entity.entityId;
    boolean stationary = !botEverMovedHoriz.getOrDefault(id, false);
    boolean alwaysInvisible = botAlwaysInvisible.getOrDefault(id, false);
    int age = Math.max(0, entity.getTicksExisted() - botFirstSeenTicks.getOrDefault(id, entity.getTicksExisted()));

    if (checkInvalidUUID) {
        String uuid = getEntityUUID(entity);
        boolean invalidUUID = !isValidUUID(uuid);
        if (invalidUUID && (!invalidUUIDStationaryOnly || stationary)) {
            return true;
        }
    }

    if (checkAlwaysInvisible && alwaysInvisible) return true;
    if (checkAlwaysStationary && stationary) return true;
    if (checkEntityAge && age < minEntityAge) return true;

    return false;
}

void updateBotState(Entity entity) {
    int id = entity.entityId;
    if (!botFirstSeenTicks.containsKey(id)) {
        botFirstSeenTicks.put(id, entity.getTicksExisted());
        botEverMovedHoriz.put(id, false);
        botAlwaysInvisible.put(id, entity.isInvisible());
    } else {
        boolean wasAlwaysInvisible = botAlwaysInvisible.getOrDefault(id, true);
        if (wasAlwaysInvisible && !entity.isInvisible()) {
            botAlwaysInvisible.put(id, false);
        }
    }

    Vec3 last = entity.getLastPosition();
    Vec3 cur = entity.getPosition();
    if (last != null && cur != null) {
        double dx = cur.x - last.x;
        double dz = cur.z - last.z;
        if (dx * dx + dz * dz > 0.0001) {
            botEverMovedHoriz.put(id, true);
        }
    }
}

String getEntityUUID(Entity entity) {
    String uuid = entity.getUUID();
    if (uuid != null && !uuid.isEmpty()) return uuid;

    NetworkPlayer net = entity.getNetworkPlayer();
    if (net != null) {
        String netUUID = net.getUUID();
        if (netUUID != null && !netUUID.isEmpty()) return netUUID;
    }

    return null;
}

boolean isValidUUID(String uuid) {
    if (uuid == null || uuid.isEmpty()) return false;
    return uuid.matches("^[0-9a-fA-F]{32}$") || uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
}

void pruneBotTracking(HashSet<Integer> activeIds) {
    for (Iterator<Map.Entry<Integer, Integer>> it = botFirstSeenTicks.entrySet().iterator(); it.hasNext();) {
        Map.Entry<Integer, Integer> entry = it.next();
        int id = entry.getKey();
        if (!activeIds.contains(id)) {
            it.remove();
            botEverMovedHoriz.remove(id);
            botAlwaysInvisible.remove(id);
        }
    }
}

void drawBox(double x1, double y1, double x2, double y2, int color) {
    render.line2D((float)x1, (float)y1, (float)x2, (float)y1, 1.5f, color);
    render.line2D((float)x1, (float)y2, (float)x2, (float)y2, 1.5f, color);
    render.line2D((float)x1, (float)y1, (float)x1, (float)y2, 1.5f, color);
    render.line2D((float)x2, (float)y1, (float)x2, (float)y2, 1.5f, color);
}
