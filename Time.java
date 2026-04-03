String colorSymbol = util.colorSymbol;
boolean enabled = false;
long cycleStartTime = 0;

void onLoad() {
    modules.registerDescription(scriptName);
    modules.registerSlider("Cycle Duration", "seconds", 10.0, 1.0, 60.0, 0.5);
}

void onEnable() {
    enabled = true;
    cycleStartTime = client.time();
}

void onDisable() {
    enabled = false;
}

void onPreUpdate() {
    if (!enabled) return;
    
    double duration = modules.getSlider(scriptName, "Cycle Duration");
    if (duration <= 0) duration = 10.0;
    
    long now = client.time();
    double elapsed = (now - cycleStartTime) / 1000.0;
    double period = duration;
    double pos = (elapsed % period) / period;  // [0,1)
    double triangle = pos < 0.5 ? pos * 2 : 2 - (pos * 2);
    double timeValue = 0.1 + triangle * (24.0 - 0.1);
    modules.setSlider("Weather", "Time", timeValue);
}