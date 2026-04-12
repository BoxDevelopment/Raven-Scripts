Map<Integer, Long> botJoinTimesMs = new HashMap<>();

void onLoad() {
    modules.registerDescription("AntiBot Library.");
    modules.registerSlider("Delay", "s", -1, -1, 15, 0.5);
    modules.registerSlider("Pit spawn", "", -1, -1, 120, 1);
    modules.registerButton("Tab list", false);
    modules.registerButton("Print world join", false);
}

void onEnable() {
    botJoinTimesMs.clear();
}

void onDisable() {
    botJoinTimesMs.clear();
}

void onPreUpdate() {
    pruneDelayTracking();
}

void onWorldJoin(Entity entity) {
    if (entity == null) return;
    if (entity.isUser) {
        botJoinTimesMs.clear();
        return;
    }
    if (!entity.isPlayer) return;

    if (modules.getButton(scriptName, "Print world join")) {
        String display = entity.getDisplayName();
        if (display == null || display.isEmpty()) display = entity.getName();
        if (display == null) display = "";
        client.print("&7Entity &b" + entity.entityId + " &7joined: &r" + display);
    }

    double delaySeconds = modules.getSlider(scriptName, "Delay");
    if (delaySeconds != -1) {
        botJoinTimesMs.put(entity.entityId, client.time());
    }
}

boolean isBot(Entity entity) {
    if (entity == null || !entity.isPlayer) return true;

    if (isDelayBot(entity)) {
        return true;
    }

    if (entity.isDead()) {
        return true;
    }

    String name = entity.getName();
    if (name == null || name.isEmpty()) {
        return true;
    }

    if (modules.getButton(scriptName, "Tab list") && !getTablistNames().contains(name)) {
        return true;
    }

    String display = entity.getDisplayName();
    if (entity.getHealth() != 20.0f && ((name != null && name.startsWith("&c")) || (display != null && display.startsWith("&c")))) {
        return true;
    }

    if (isPitSpawnBot(entity)) {
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
            if (display.length() >= 3 && display.charAt(0) == '&' && display.charAt(1) == 'c') {
                return true;
            }
        }
    }

    return false;
}

boolean isDelayBot(Entity entity) {
    double delaySeconds = modules.getSlider(scriptName, "Delay");
    if (delaySeconds == -1 || botJoinTimesMs.isEmpty()) return false;

    Long joinedAt = botJoinTimesMs.get(entity.entityId);
    if (joinedAt == null) return false;

    long delayMs = Math.max(0L, (long) Math.round(delaySeconds * 1000.0));
    return client.time() - joinedAt < delayMs;
}

void pruneDelayTracking() {
    if (botJoinTimesMs.isEmpty()) return;

    double delaySeconds = modules.getSlider(scriptName, "Delay");
    if (delaySeconds == -1) {
        botJoinTimesMs.clear();
        return;
    }

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

boolean isPitSpawnBot(Entity entity) {
    double pitSpawnY = modules.getSlider(scriptName, "Pit spawn");
    if (pitSpawnY == -1) return false;
    if (!isHypixelPitGame()) return false;

    Vec3 pos = entity.getPosition();
    if (pos == null) return false;
    if (pos.y < pitSpawnY || pos.y > 130.0) return false;

    double dx = pos.x;
    double dy = pos.y - 114.0;
    double dz = pos.z;
    return dx * dx + dy * dy + dz * dz <= 625.0;
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
