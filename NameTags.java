String[] HEALTH_DISPLAY_MODES = {"Hearts", "Health"};
String[] HEART_SYMBOL_MODES = {"h", "\u2665", "\u2764"};
Map<Integer, Long> botJoinTimesMs = new HashMap<>();

void onLoad() {
    modules.registerSlider("Range", "blocks", 120, 10, 300, 5);
    modules.registerSlider("Scale", "", 1.0, 0.1, 2.5, 0.1);
    modules.registerButton("Auto Scale", false);
    modules.registerButton("Distance scaling", true);

    modules.registerButton("Background", true);
    modules.registerSlider("Background Opacity", "", 120, 0, 255, 1);
    modules.registerButton("Background Border", false);
    modules.registerButton("Clamp to sides", false);
    modules.registerButton("Only render name", false);
    modules.registerButton("Text Shadow", false);

    modules.registerDescription("Extra info");
    modules.registerButton("Show Health", true);
    modules.registerSlider("Health display", "", 0, HEALTH_DISPLAY_MODES);
    modules.registerButton("Show Heart Symbol", true);
    modules.registerSlider("Heart Symbol", "", 0, HEART_SYMBOL_MODES);
    modules.registerButton("Show Distance", true);

    modules.registerButton("Show Invis", true);
    modules.registerButton("Show Armor", false);
    modules.registerButton("Show Enchantments", false);
    modules.registerButton("Show Durability", false);
    modules.registerButton("Show Yourself", false);
    
    modules.registerDescription("AntiBot");
    modules.registerButton("Ignore Bots", true);
    modules.registerSlider("Bot Check: Delay", "s", -1, -1, 15, 0.5);
    modules.registerSlider("Bot Check: Pit Spawn", "", -1, -1, 120, 1);
    modules.registerButton("Bot Check: Tab List", false);
    modules.registerButton("Bot Check: Print World Join", false);

    modules.registerDescription("Friend color");
    modules.registerSlider("Friend R", "", 85, 0, 255, 1);
    modules.registerSlider("Friend G", "", 255, 0, 255, 1);
    modules.registerSlider("Friend B", "", 255, 0, 255, 1);

    modules.registerDescription("Enemy color");
    modules.registerSlider("Enemy R", "", 255, 0, 255, 1);
    modules.registerSlider("Enemy G", "", 85, 0, 255, 1);
    modules.registerSlider("Enemy B", "", 85, 0, 255, 1);
}

void onEnable() {
    clearBotTracking();
    modules.setSlider("Nametags", "Scale", -1);
    modules.setButton("Nametags", "Show armor", false);
}

void onDisable() {
    clearBotTracking();
}

void clearBotTracking() {
    botJoinTimesMs.clear();
}

void onPreUpdate() {
    pruneDelayTracking();
}

void onWorldJoin(Entity entity) {
    if (entity == null) return;
    if (entity.isUser) {
        clearBotTracking();
        return;
    }
    if (!entity.isPlayer) return;

    double delaySeconds = modules.getSlider(scriptName, "Bot Check: Delay");
    if (delaySeconds != -1) {
        botJoinTimesMs.put(entity.entityId, client.time());
    }

    if (modules.getButton(scriptName, "Bot Check: Print World Join")) {
        String display = entity.getDisplayName();
        if (display == null || display.isEmpty()) display = entity.getName();
        if (display == null) display = "";
        client.print("&7Entity &b" + entity.entityId + " &7joined: &r" + display);
    }
}

void onRenderTick(float partialTicks) {
    Entity self = client.getPlayer();
    if (self == null) return;
    String currentScreen = client.getScreen();
    if (!currentScreen.isEmpty() && !currentScreen.toLowerCase().contains("chat")) return;

    double range = modules.getSlider(scriptName, "Range");
    float baseScale = (float) modules.getSlider(scriptName, "Scale");
    boolean autoScale = modules.getButton(scriptName, "Auto Scale");
    boolean distanceScaling = modules.getButton(scriptName, "Distance scaling");
    boolean showBackground = modules.getButton(scriptName, "Background");
    int bgOpacity = (int) modules.getSlider(scriptName, "Background Opacity");
    boolean bgBorder = modules.getButton(scriptName, "Background Border");
    boolean clampToSides = modules.getButton(scriptName, "Clamp to sides");
    boolean onlyName = modules.getButton(scriptName, "Only render name");
    boolean textShadow = modules.getButton(scriptName, "Text Shadow");
    boolean showHealth = modules.getButton(scriptName, "Show Health");
    boolean heartsMode = ((int) modules.getSlider(scriptName, "Health display")) == 0;
    boolean showHeartSymbol = modules.getButton(scriptName, "Show Heart Symbol");
    int heartSymbolMode = (int) modules.getSlider(scriptName, "Heart Symbol");
    boolean showDistance = modules.getButton(scriptName, "Show Distance");
    boolean showInvis = modules.getButton(scriptName, "Show Invis");
    boolean showArmor = modules.getButton(scriptName, "Show Armor");
    boolean showEnchants = modules.getButton(scriptName, "Show Enchantments");
    boolean showDurability = modules.getButton(scriptName, "Show Durability");
    boolean showSelf = modules.getButton(scriptName, "Show Yourself");
    boolean ignoreBots = modules.getButton(scriptName, "Ignore Bots");
    double botDelaySeconds = modules.getSlider(scriptName, "Bot Check: Delay");
    double pitSpawnY = modules.getSlider(scriptName, "Bot Check: Pit Spawn");
    boolean checkTabList = modules.getButton(scriptName, "Bot Check: Tab List");
    HashSet<String> tabListNames = checkTabList ? getTablistNames() : null;
    boolean hypixelPit = pitSpawnY != -1 && isHypixelPitGame();

    int friendColor = toColor(
        (int) modules.getSlider(scriptName, "Friend R"),
        (int) modules.getSlider(scriptName, "Friend G"),
        (int) modules.getSlider(scriptName, "Friend B"),
        255
    );
    int enemyColor = toColor(
        (int) modules.getSlider(scriptName, "Enemy R"),
        (int) modules.getSlider(scriptName, "Enemy G"),
        (int) modules.getSlider(scriptName, "Enemy B"),
        255
    );

    int[] displaySize = client.getDisplaySize();
    int screenWidth = displaySize[0];
    int screenHeight = displaySize[1];
    int guiScale = displaySize[2];
    float edgePadding = 4.0f;
    Vec3 selfPos = self.getPosition();
    if (selfPos == null) return;

    ArrayList<Object[]> renderList = new ArrayList<>();
    HashSet<Integer> activeIds = new HashSet<>();
    for (Entity e : world.getPlayerEntities()) {
        if (e == null || e.isDead() || e.getHealth() <= 0) continue;
        if (e.equals(self) && !showSelf) continue;
        activeIds.add(e.entityId);
        if (!showInvis && e.isInvisible()) continue;

        Vec3 pos = e.getPosition();
        if (pos == null) continue;

        double dist = selfPos.distanceTo(pos);
        if (dist > range) continue;
        
        if (ignoreBots && isBotEntity(e, botDelaySeconds, pitSpawnY, checkTabList, tabListNames, hypixelPit)) {
            continue;
        }

        Vec3 last = e.getLastPosition();
        if (last == null) last = pos;
        double x = last.x + (pos.x - last.x) * partialTicks;
        double y = last.y + (pos.y - last.y) * partialTicks + e.getHeight() + (e.isSneaking() ? 0.15 : 0.45);
        double z = last.z + (pos.z - last.z) * partialTicks;

        if (clampToSides) {
            double[] screenPos = resolveTagScreenPosition(self, x, y, z, guiScale, partialTicks, screenWidth, screenHeight, edgePadding);
            if (screenPos == null) continue;
            renderList.add(new Object[]{e, screenPos[0], screenPos[1], dist});
        } else {
            Vec3 screen = render.worldToScreen(x, y, z, guiScale, partialTicks);
            if (screen == null || screen.z < 0 || screen.z >= 1) continue;
            renderList.add(new Object[]{e, screen.x, screen.y, dist});
        }
    }

    renderList.sort((a, b) -> Double.compare((double) b[3], (double) a[3]));

    for (Object[] entry : renderList) {
        Entity e = (Entity) entry[0];
        double screenX = (double) entry[1];
        double screenY = (double) entry[2];
        double dist = (double) entry[3];

        String rawDisplay = e.getDisplayName();
        String raw = onlyName ? e.getName() : rawDisplay;
        if (raw == null || raw.isEmpty()) raw = e.getName();
        if (raw == null || raw.isEmpty()) continue;
        String name = util.strip(raw);
        if (name.isEmpty()) continue;
        String username = e.getName();
        if (username == null || username.isEmpty()) username = name;

        int fallbackNameColor = 0xFFFFFFFF;
        if (client.isFriend(username)) fallbackNameColor = friendColor;
        else if (client.isEnemy(username)) fallbackNameColor = enemyColor;
        int nameColor = extractTeamColor(rawDisplay, username, fallbackNameColor);

        String distPart = showDistance ? ((int) Math.round(dist)) + "m" : "";
        int distColor = distanceColor(dist, range);

        String hpMainPart = "";
        String hpExtraPart = "";
        int hpColor = 0xFFFFFFFF;
        int hpExtraColor = 0xFFFFAA00;
        String heartSuffix = showHeartSymbol ? getHeartSymbol(heartSymbolMode) : "";
        if (showHealth) {
            float hp = Math.max(0.0f, e.getHealth());
            float absorption = Math.max(0.0f, e.getAbsorption());
            float totalHp = hp + absorption;
            float mainHp = Math.min(20.0f, totalHp);
            hpColor = healthColor(mainHp, 20.0f);

            float displayMain = heartsMode ? mainHp / 2.0f : mainHp;
            hpMainPart = fastOneDecimal(displayMain);
            if (!heartSuffix.isEmpty()) hpMainPart += heartSuffix;

            if (totalHp > 20.0f) {
                float extra = totalHp - 20.0f;
                float extraDisplay = heartsMode ? extra / 2.0f : extra;
                hpExtraPart = "+" + fastOneDecimal(extraDisplay);
                if (!heartSuffix.isEmpty()) hpExtraPart += heartSuffix;
            }
        }

        float scale = computeScale(baseScale, dist, range, distanceScaling, autoScale);
        float spaceW = render.getFontWidth(" ") * scale;
        boolean hasHealth = !hpMainPart.isEmpty();
        boolean hasDistance = !distPart.isEmpty();
        String sneakPart = e.isSneaking() ? " S" : "";
        boolean hasSneak = !sneakPart.isEmpty();
        int sneakColor = 0xFFFFFF55;
        float hpMainW = render.getFontWidth(hpMainPart) * scale;
        float hpExtraW = render.getFontWidth(hpExtraPart) * scale;
        float nameW = render.getFontWidth(name) * scale;
        float sneakW = render.getFontWidth(sneakPart) * scale;
        float distW = render.getFontWidth(distPart) * scale;
        float totalW = nameW
            + (hasSneak ? sneakW : 0.0f)
            + (hasHealth ? hpMainW + hpExtraW + spaceW : 0.0f)
            + (hasDistance ? spaceW + distW : 0.0f);
        float textH = render.getFontHeight() * scale;
        float x = (float) screenX - totalW * 0.5f;
        float y = (float) screenY - textH;
        if (clampToSides) {
            float minX = 2.0f;
            float maxX = Math.max(2.0f, screenWidth - totalW - 2.0f);
            float minY = 2.0f;
            float maxY = Math.max(2.0f, screenHeight - textH - 2.0f);
            if (screenX <= edgePadding + 1.0) {
                x = minX;
            } else if (screenX >= screenWidth - edgePadding - 1.0) {
                x = maxX;
            } else {
                x = clampFloat(x, minX, maxX);
            }
            if (screenY <= edgePadding + 1.0) {
                y = minY;
            } else if (screenY >= screenHeight - edgePadding - 1.0) {
                y = maxY;
            } else {
                y = clampFloat(y, minY, maxY);
            }
        }

        if (showBackground && bgOpacity > 0) {
            float pad = 2.0f * scale;
            int bg = toColor(0, 0, 0, bgOpacity);
            render.rect(x - pad, y - pad, x + totalW + pad, y + textH + pad, bg);
            if (bgBorder) {
                int border = (nameColor & 0x00FFFFFF) | 0xAA000000;
                render.rect(x - pad - 1, y - pad - 1, x + totalW + pad + 1, y - pad, border);
                render.rect(x - pad - 1, y + textH + pad, x + totalW + pad + 1, y + textH + pad + 1, border);
                render.rect(x - pad - 1, y - pad, x - pad, y + textH + pad, border);
                render.rect(x + totalW + pad, y - pad, x + totalW + pad + 1, y + textH + pad, border);
            }
        }

        float drawX = x;
        if (hasHealth) {
            render.text(hpMainPart, drawX, y, scale, hpColor, textShadow);
            drawX += hpMainW;
            if (!hpExtraPart.isEmpty()) {
                render.text(hpExtraPart, drawX, y, scale, hpExtraColor, textShadow);
                drawX += hpExtraW;
            }
            drawX += spaceW;
        }
        render.text(name, drawX, y, scale, nameColor, textShadow);
        drawX += nameW;
        if (hasSneak) {
            render.text(sneakPart, drawX, y, scale, sneakColor, textShadow);
            drawX += sneakW;
        }
        if (hasDistance) {
            drawX += spaceW;
            render.text(distPart, drawX, y, scale, distColor, textShadow);
        }

        if (showArmor) {
            float armorIconScale = Math.max(0.55f, Math.min(1.0f, scale * 0.95f));
            float armorYOffset = (13.0f * armorIconScale) + (3.0f * scale);
            renderArmorLine(e, (float) screenX, y - armorYOffset, scale, showEnchants, showDurability, textShadow);
        }
    }
    
    pruneBotTracking(activeIds);
}

void renderArmorLine(Entity e, float centerX, float y, float tagScale, boolean showEnchants, boolean showDurability, boolean textShadow) {
    ArrayList<ItemStack> items = new ArrayList<>();
    ItemStack held = e.getHeldItem();
    if (held != null) items.add(held);

    ItemStack helmet = e.getArmorInSlot(3);
    ItemStack chest = e.getArmorInSlot(2);
    ItemStack legs = e.getArmorInSlot(1);
    ItemStack boots = e.getArmorInSlot(0);
    if (helmet != null) items.add(helmet);
    if (chest != null) items.add(chest);
    if (legs != null) items.add(legs);
    if (boots != null) items.add(boots);

    if (items.isEmpty()) return;

    float iconScale = Math.max(0.55f, Math.min(1.0f, tagScale * 0.95f));
    float spacing = 14.0f * iconScale;
    float startX = centerX - ((items.size() - 1) * spacing) * 0.5f - (8.0f * iconScale);

    for (int i = 0; i < items.size(); i++) {
        ItemStack item = items.get(i);
        float ix = startX + i * spacing;
        render.item(item, ix, y, iconScale);

        if (showEnchants) {
            List<Object[]> ench = item.getEnchantments();
            if (ench != null && !ench.isEmpty()) {
                render.text("E" + ench.size(), ix, y - (6.0f * iconScale), 0.5f * iconScale, 0xFF99D8FF, textShadow);
            }
        }

        if (showDurability && item.maxDurability > 0) {
            int pct = (int) Math.round((item.durability * 100.0) / item.maxDurability);
            int dColor = pct > 66 ? 0xFF55FF55 : (pct > 33 ? 0xFFFFCC55 : 0xFFFF5555);
            render.text(pct + "%", ix, y + (11.0f * iconScale), 0.45f * iconScale, dColor, textShadow);
        }
    }
}

float computeScale(float baseScale, double dist, double range, boolean distanceScaling, boolean autoScale) {
    if (distanceScaling) {
        return baseScale;
    }
    if (autoScale) {
        float scaled = baseScale * (float) (Math.max(1.0, dist / 5.0));
        return Math.max(baseScale, scaled);
    }
    double t = Math.min(1.0, dist / Math.max(1.0, range));
    float s = (float) (baseScale * (1.35 - t * 0.75));
    return Math.max(0.45f, s);
}

double[] resolveTagScreenPosition(Entity viewer, double worldX, double worldY, double worldZ, int guiScale, float partialTicks, int screenWidth, int screenHeight, float edgePadding) {
    double centerX = screenWidth * 0.5;
    double centerY = screenHeight * 0.5;

    Vec3 projected = render.worldToScreen(worldX, worldY, worldZ, guiScale, partialTicks);
    if (projected != null && projected.z >= 0.0 && projected.z < 1.0) {
        double px = projected.x;
        double py = projected.y;
        if (px >= edgePadding && px <= screenWidth - edgePadding && py >= edgePadding && py <= screenHeight - edgePadding) {
            return new double[]{px, py};
        }
    }

    Vec3 viewerPos = viewer.getPosition();
    if (viewerPos == null) return null;

    double dx = worldX - viewerPos.x;
    double dy = worldY - (viewerPos.y + viewer.getEyeHeight());
    double dz = worldZ - viewerPos.z;
    if (Math.abs(dx) + Math.abs(dz) < 0.0001) {
        return new double[]{centerX, centerY};
    }

    double horizontal = Math.sqrt(dx * dx + dz * dz);
    if (horizontal < 0.001) horizontal = 0.001;
    float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
    float yawDelta = wrapDegrees(targetYaw - viewer.getYaw());
    float pitchDelta = targetPitch - viewer.getPitch();
    double angleRad = Math.toRadians(yawDelta);

    double dirX = Math.sin(angleRad);
    double dirY = -Math.cos(angleRad);

    double left = edgePadding;
    double right = screenWidth - edgePadding;
    double top = edgePadding;
    double bottom = screenHeight - edgePadding;
    if (Math.abs(dirX) >= Math.abs(dirY)) {
        double sideYRange = (centerY - edgePadding) * 0.35;
        double py = centerY + clampDouble(pitchDelta / 60.0, -1.0, 1.0) * sideYRange;
        return new double[]{
            dirX < 0.0 ? left : right,
            clampDouble(py, top, bottom)
        };
    }

    double px = centerX + clampDouble(dirX / Math.max(0.0001, Math.abs(dirY)), -1.0, 1.0) * (centerX - edgePadding);
    return new double[]{
        clampDouble(px, left, right),
        dirY < 0.0 ? top : bottom
    };
}

float wrapDegrees(float angle) {
    while (angle <= -180.0f) angle += 360.0f;
    while (angle > 180.0f) angle -= 360.0f;
    return angle;
}

double clampDouble(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
}

float clampFloat(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
}

int distanceColor(double dist, double maxDist) {
    float t = (float) (dist / Math.max(1.0, maxDist));
    return greenYellowRed(t);
}

int healthColor(float health, float maxHealth) {
    float ratio = health / Math.max(1.0f, maxHealth);
    return greenYellowRed(1.0f - Math.max(0.0f, Math.min(1.0f, ratio)));
}

int greenYellowRed(float t) {
    float clamped = Math.max(0.0f, Math.min(1.0f, t));
    if (clamped <= 0.5f) {
        return blendColor(0xFF55FF55, 0xFFFFFF55, clamped / 0.5f);
    }
    return blendColor(0xFFFFFF55, 0xFFFF5555, (clamped - 0.5f) / 0.5f);
}

int blendColor(int from, int to, float t) {
    float c = Math.max(0.0f, Math.min(1.0f, t));
    int a1 = (from >>> 24) & 255;
    int r1 = (from >>> 16) & 255;
    int g1 = (from >>> 8) & 255;
    int b1 = from & 255;
    int a2 = (to >>> 24) & 255;
    int r2 = (to >>> 16) & 255;
    int g2 = (to >>> 8) & 255;
    int b2 = to & 255;
    int a = a1 + (int) ((a2 - a1) * c);
    int r = r1 + (int) ((r2 - r1) * c);
    int g = g1 + (int) ((g2 - g1) * c);
    int b = b1 + (int) ((b2 - b1) * c);
    return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
}

int extractTeamColor(String rawDisplay, String username, int fallback) {
    if (rawDisplay == null || rawDisplay.isEmpty()) return fallback;

    if (username != null && !username.isEmpty()) {
        int nameIndex = rawDisplay.indexOf(username);
        if (nameIndex >= 0) {
            int nameColor = activeColorBefore(rawDisplay, nameIndex);
            if (nameColor != -1) return nameColor;
        }
    }

    int trailingColor = activeColorBefore(rawDisplay, rawDisplay.length());
    return trailingColor != -1 ? trailingColor : fallback;
}

int activeColorBefore(String text, int endExclusive) {
    if (text == null || text.isEmpty()) return -1;
    int active = -1;
    int limit = Math.min(endExclusive, text.length() - 1);
    for (int i = 0; i < limit; i++) {
        char c = text.charAt(i);
        if (c == '\u00A7' || c == '&') {
            char code = text.charAt(i + 1);
            int mapped = minecraftColor(code);
            if (mapped != -1) active = mapped;
            else if (Character.toLowerCase(code) == 'r') active = 0xFFFFFFFF;
            i++;
        }
    }
    return active;
}

int minecraftColor(char code) {
    switch (Character.toLowerCase(code)) {
        case '0': return 0xFF000000;
        case '1': return 0xFF0000AA;
        case '2': return 0xFF00AA00;
        case '3': return 0xFF00AAAA;
        case '4': return 0xFFAA0000;
        case '5': return 0xFFAA00AA;
        case '6': return 0xFFFFAA00;
        case '7': return 0xFFAAAAAA;
        case '8': return 0xFF555555;
        case '9': return 0xFF5555FF;
        case 'a': return 0xFF55FF55;
        case 'b': return 0xFF55FFFF;
        case 'c': return 0xFFFF5555;
        case 'd': return 0xFFFF55FF;
        case 'e': return 0xFFFFFF55;
        case 'f': return 0xFFFFFFFF;
        default: return -1;
    }
}

boolean isBotEntity(Entity entity, double delaySeconds, double pitSpawnY, boolean checkTabList, HashSet<String> tabListNames, boolean hypixelPit) {
    if (entity == null || !entity.isPlayer) return true;
    if (isDelayBot(entity, delaySeconds)) return true;
    if (entity.isDead()) return true;

    String name = entity.getName();
    if (name == null || name.isEmpty()) return true;

    if (checkTabList && tabListNames != null && !tabListNames.contains(name)) {
        return true;
    }

    String display = entity.getDisplayName();
    if (entity.getHealth() != 20.0f && ((name != null && name.startsWith("§c")) || (display != null && display.startsWith("§c")))) {
        return true;
    }

    if (pitSpawnY != -1 && hypixelPit && isPitSpawnBot(entity, pitSpawnY)) {
        return true;
    }

    if (entity.getMaxHurtTime() == 0) {
        if (display == null) display = "";
        if (entity.getHealth() == 20.0f) {
            if (display.length() == 10 && display.charAt(0) != '&') {
                return true;
            }
            if (display.length() == 12 && entity.isSleeping() && display.charAt(0) == '&') {
                return true;
            }
            if (display.length() >= 7 && display.charAt(2) == '[' && display.charAt(3) == 'N' && display.charAt(6) == ']') {
                return true;
            }
            if (name.indexOf(' ') != -1) {
                return true;
            }
        } else if (entity.isInvisible()) {
            if (display.length() >= 2 && display.charAt(0) == '&' && display.charAt(1) == 'c') {
                return true;
            }
        }
    }

    return false;
}

boolean isDelayBot(Entity entity, double delaySeconds) {
    if (delaySeconds == -1 || botJoinTimesMs.isEmpty()) return false;
    Long joinedAt = botJoinTimesMs.get(entity.entityId);
    if (joinedAt == null) return false;
    long delayMs = Math.max(0L, (long) Math.round(delaySeconds * 1000.0));
    return client.time() - joinedAt < delayMs;
}

void pruneDelayTracking() {
    if (botJoinTimesMs.isEmpty()) return;
    double delaySeconds = modules.getSlider(scriptName, "Bot Check: Delay");
    if (delaySeconds == -1) return;
    long delayMs = Math.max(0L, (long) Math.round(delaySeconds * 1000.0));
    long cutoff = client.time() - delayMs;
    botJoinTimesMs.values().removeIf(seenAt -> seenAt < cutoff);
}

HashSet<String> getTablistNames() {
    HashSet<String> names = new HashSet<>();
    List<NetworkPlayer> netPlayers = world.getNetworkPlayers();
    if (netPlayers == null) return names;
    for (NetworkPlayer net : netPlayers) {
        if (net == null) continue;
        String name = net.getName();
        if (name != null && !name.isEmpty()) {
            names.add(name);
        }
    }
    return names;
}

boolean isHypixelPitGame() {
    String serverIp = client.getServerIP();
    if (serverIp == null || serverIp.isEmpty()) return false;
    if (!serverIp.toLowerCase().contains("hypixel")) return false;
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null || sidebar.isEmpty()) return false;
    String first = util.strip(sidebar.get(0));
    return first != null && first.toUpperCase().contains("THE HYPIXEL PIT");
}

boolean isPitSpawnBot(Entity entity, double pitSpawnY) {
    Vec3 pos = entity.getPosition();
    if (pos == null) return false;
    if (pos.y < pitSpawnY || pos.y > 130.0) return false;
    double dx = pos.x;
    double dy = pos.y - 114.0;
    double dz = pos.z;
    return dx * dx + dy * dy + dz * dz <= 625.0;
}

void pruneBotTracking(HashSet<Integer> activeIds) {
    for (Iterator<Map.Entry<Integer, Long>> it = botJoinTimesMs.entrySet().iterator(); it.hasNext();) {
        Map.Entry<Integer, Long> entry = it.next();
        int id = entry.getKey();
        if (!activeIds.contains(id)) {
            it.remove();
        }
    }
}

String fastOneDecimal(float value) {
    int whole = (int) value;
    if (Math.abs(value - whole) < 0.001f) {
        return String.valueOf(whole);
    }
    int tenths = Math.round(value * 10.0f);
    int intPart = tenths / 10;
    int fracPart = Math.abs(tenths % 10);
    return intPart + "." + fracPart;
}

String getHeartSymbol(int mode) {
    if (mode == 1) return "\u2665";
    if (mode == 2) return "\u2764";
    return "h";
}

int toColor(int r, int g, int b, int a) {
    return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
}
