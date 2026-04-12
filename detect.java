load - "https://raw.githubusercontent.com/BoxDevelopment/Raven-Scripts/refs/heads/main/AntiBot.java"

HashSet<Integer> printedBotIds = new HashSet<>();

void onRenderTick(float partialTicks) {
    Entity self = client.getPlayer();
    if (self == null) return;

    HashSet<Integer> activeIds = new HashSet<>();

    for (Entity entity : world.getPlayerEntities()) {
        if (entity == null || !entity.isPlayer || entity.isUser) continue;
        activeIds.add(entity.entityId);

        if (!isBot(entity)) continue;
        if (!printedBotIds.add(entity.entityId)) continue;

        String display = entity.getDisplayName();
        if (display == null || display.isEmpty()) display = entity.getName();
        if (display == null || display.isEmpty()) display = "#" + entity.entityId;

        client.print("&7[Detect] &cBot detected: &r" + display + " &7(id: &b" + entity.entityId + "&7)");
    }

    printedBotIds.retainAll(activeIds);
}
