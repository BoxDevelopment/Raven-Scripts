String colorSymbol = util.colorSymbol;

int status = -1;
long lastStatusCheck = 0;
long lastSampleTime = 0;
long lastLeaveAttempt = 0;

String selfName = "";
int previousPlayerCount = -1;
int previousVisibleNameCount = -1;
boolean dodgedThisQueue = false;
boolean hasScannedInitialLobby = false;
boolean pendingLeave = false;
HashSet<String> previousPlayerIds = new HashSet<>();

void onLoad() {
    modules.registerDescription("Leaves queue if suspicious players are present on join or join in a spike (party‑safe).");
    modules.registerSlider("Join Threshold", "players", 4, 1, 16, 1);
    modules.registerSlider("Sample Delay", "ms", 250, 50, 1000, 50);
    modules.registerButton("Debug", false);
    modules.registerButton("Name Debug", false);
}

void onEnable() {
    Entity self = client.getPlayer();
    selfName = self != null ? self.getName() : "";
    resetQueueTracking();
}

void onDisable() {
    resetQueueTracking();
}

void resetQueueTracking() {
    previousPlayerCount = -1;
    previousVisibleNameCount = -1;
    dodgedThisQueue = false;
    hasScannedInitialLobby = false;
    pendingLeave = false;
    lastLeaveAttempt = 0;
    lastSampleTime = 0;
    previousPlayerIds.clear();
}

void onPreUpdate() {
    Entity self = client.getPlayer();
    if (self == null) return;
    if (selfName.isEmpty()) selfName = self.getName();

    if (pendingLeave) {
        long now = client.time();
        if (now - lastLeaveAttempt >= 250) {
            client.chat("/l");
            pendingLeave = false;
            dodgedThisQueue = true;
            if (modules.getButton(scriptName, "Debug")) {
                client.print(colorSymbol + "c[QueueDodge] Left queue after delay.");
            }
        }
        return;
    }

    long now = client.time();
    if (now - lastStatusCheck > 1000) {
        status = getBedwarsStatus();
        lastStatusCheck = now;
    }

    if (status != 2) {
        resetQueueTracking();
        return;
    }

    long sampleDelay = (long) modules.getSlider(scriptName, "Sample Delay");
    if (now - lastSampleTime < sampleDelay) return;
    lastSampleTime = now;

    HashMap<String, Map<String, String>> queueEntries = getQueueEntries();
    if (queueEntries == null) return;

    int playerCount = queueEntries.size();
    int visibleNameCount = countVisibleEntries(queueEntries);

    if (!hasScannedInitialLobby) {
        int threshold = (int) modules.getSlider(scriptName, "Join Threshold");
        int nonPartyInitial = playerCount - visibleNameCount;

        if (nonPartyInitial >= threshold) {
            lastLeaveAttempt = client.time();
            pendingLeave = true;
            client.print(colorSymbol + "c[QueueDodge] " + nonPartyInitial + " players on join");
            if (modules.getButton(scriptName, "Debug")) {
                client.print(colorSymbol + "7[QueueDodge] Non-party players in lobby: " + nonPartyInitial);
            }
        }
        hasScannedInitialLobby = true;
        previousPlayerCount = playerCount;
        previousVisibleNameCount = visibleNameCount;
        previousPlayerIds.clear();
        previousPlayerIds.addAll(queueEntries.keySet());
        return;
    }

    if (previousPlayerCount == -1) {
        previousPlayerCount = playerCount;
        previousVisibleNameCount = visibleNameCount;
        previousPlayerIds.clear();
        previousPlayerIds.addAll(queueEntries.keySet());
        return;
    }

    if (modules.getButton(scriptName, "Name Debug")) {
        printJoinDebug(queueEntries);
    }

    int totalDelta = playerCount - previousPlayerCount;
    int visibleDelta = visibleNameCount - previousVisibleNameCount;

    if (!dodgedThisQueue && !pendingLeave && totalDelta > 0) {
        int threshold = (int) modules.getSlider(scriptName, "Join Threshold");
        int nonPartyDelta = totalDelta - Math.max(0, visibleDelta);

        if (nonPartyDelta >= threshold) {
            lastLeaveAttempt = client.time();
            pendingLeave = true;
            client.print(colorSymbol + "c[QueueDodge] " + nonPartyDelta + " layers joined! Leaving queue...");
            if (modules.getButton(scriptName, "Debug")) {
                client.print(colorSymbol + "7[QueueDodge] Non-party spike: " + nonPartyDelta + " (raw: " + totalDelta + ")");
            }
        } else if (modules.getButton(scriptName, "Debug") && totalDelta >= threshold) {
            client.print(colorSymbol + "e[QueueDodge] Ignored spike (likely party). Raw: " + totalDelta + ", visible: +" + Math.max(0, visibleDelta));
        }
    }

    previousPlayerCount = playerCount;
    previousVisibleNameCount = visibleNameCount;
    previousPlayerIds.clear();
    previousPlayerIds.addAll(queueEntries.keySet());
}

HashMap<String, Map<String, String>> getQueueEntries() {
    List<NetworkPlayer> players = world.getNetworkPlayers();
    if (players == null) return null;
    HashMap<String, Map<String, String>> entries = new HashMap<>();

    for (NetworkPlayer player : players) {
        if (player == null) continue;

        String name = player.getName();
        if (!selfName.isEmpty() && name != null && name.equalsIgnoreCase(selfName)) continue;

        String displayName = player.getDisplayName();
        String id = getPlayerId(player);

        HashMap<String, String> info = new HashMap<>();
        info.put("name", name == null ? "" : name);
        info.put("display", displayName == null ? "" : displayName);
        entries.put(id, info);
    }

    return entries;
}

int countVisibleEntries(HashMap<String, Map<String, String>> entries) {
    int visible = 0;
    for (Map<String, String> info : entries.values()) {
        if (!isObfuscated(info.get("display"))) {
            visible++;
        }
    }
    return visible;
}

void printJoinDebug(HashMap<String, Map<String, String>> queueEntries) {
    for (String id : queueEntries.keySet()) {
        if (previousPlayerIds.contains(id)) continue;

        Map<String, String> info = queueEntries.get(id);
        String name = info.get("name");
        String display = info.get("display");
        boolean obf = isObfuscated(display);
        String clean = util.strip(display);

        client.print(
            colorSymbol + "7[QueueDodge] Join " +
            (obf ? colorSymbol + "c[OBF]" : colorSymbol + "a[VISIBLE]") +
            colorSymbol + "7 name=" + colorSymbol + "f" + name +
            colorSymbol + "7 display=" + display +
            colorSymbol + "7 clean=" + colorSymbol + "f" + clean
        );
    }
}

String getPlayerId(NetworkPlayer player) {
    String uuid = player.getUUID();
    if (uuid != null && !uuid.isEmpty()) return uuid;

    String name = player.getName();
    if (name != null && !name.isEmpty()) return "name:" + name.toLowerCase();

    String display = player.getDisplayName();
    if (display != null && !display.isEmpty()) return "display:" + display;

    return "unknown:" + client.time();
}

boolean isObfuscated(String displayName) {
    if (displayName == null || displayName.isEmpty()) return true;
    String lower = displayName.toLowerCase();
    String obfCode = colorSymbol.toLowerCase() + "k";
    return lower.contains(obfCode);
}

int getBedwarsStatus() {
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }

    if (sidebar.size() < 7) return -1;
    if (!util.strip(sidebar.get(0)).startsWith("BED WARS")) return -1;

    if (util.strip(sidebar.get(5)).startsWith("R Red:") && util.strip(sidebar.get(6)).startsWith("B Blue:")) {
        return 3;
    }

    String six = util.strip(sidebar.get(6));
    if (six.equals("Waiting...") || six.startsWith("Starting in")) {
        return 2;
    }

    return -1;
}