void onEnable() {
    print("&7toggled safewalk: &cblatant");
    modules.setSlider("SafeWalk","Mode", 3);
}

void onDisable() {
    print("&7toggled safewalk: &bsafe");
    modules.setSlider("SafeWalk","Mode", 2);
    modules.disable("Scaffold");
}

void print(Object obj) {
    client.print("&7[&dS&7] &r" + obj);
}