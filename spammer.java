AtomicBoolean running = new AtomicBoolean(false);
String target = "";

void onLoad() {
    modules.registerDescription("Type ,spam <username> to spam party invites. ,spam stop to stop.");
}

boolean onPacketSent(CPacket packet) {
    if (!(packet instanceof C01)) return true;
    C01 c01 = (C01) packet;
    String msg = c01.message;
    if (!msg.startsWith(",spam")) return true;
    String[] parts = msg.split(" ");
    if (parts.length >= 2 && parts[1].equalsIgnoreCase("stop")) {
        running.set(false);
        client.print(util.colorSymbol + "aSpam stopped.");
        return false;
    }

    if (parts.length < 2) {
        client.print("&eUsage: &b,spam <username> &7or &b,spam stop");
        return false;
    }
    target = parts[1];
    startSpam();
    return false;
}

void startSpam() {
    running.set(false);
    running.set(true);

    client.async(() -> {
        while (running.get()) {
            client.chat("/p invite " + target);
            client.sleep(250);
            if (!running.get()) break;
            client.chat("/p disband");
            client.sleep(250);
        }
    });
}

void onDisable() {
    running.set(false);
}
