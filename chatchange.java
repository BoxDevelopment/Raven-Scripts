String colorSymbol = util.colorSymbol;

private final String IRC_TAG_PREFIX = colorSymbol + "7[" + colorSymbol + "dIRC" + colorSymbol + "7] " + colorSymbol + "f";
private final String COLOR_ACCENT   = colorSymbol + "b";
private final String COLOR_MUTED    = colorSymbol + "7";
private final String COLOR_PLAIN    = colorSymbol + "f";

String stripColor(String text) {
    if (text == null) return "";
    return text.replaceAll("(?i)[§&][0-9a-fklmnor]", "");
}

boolean onIRCMessage(String raw) {
    int colonIndex = raw.indexOf(": ");
    if (colonIndex == -1) colonIndex = raw.indexOf(":");

    if (colonIndex == -1) {
        client.print(IRC_TAG_PREFIX + raw);
        return false;
    }

    String senderRaw   = raw.substring(0, colonIndex);
    String messageText = raw.substring(colonIndex + 1).trim();

    String cleanSender = stripColor(senderRaw).trim();
    int splitAt = cleanSender.lastIndexOf(' ');

    String username = splitAt == -1 ? cleanSender : cleanSender.substring(0, splitAt).trim();
    String uid      = splitAt == -1 ? ""           : cleanSender.substring(splitAt + 1).trim();

    String formatted = uid.isEmpty()
        ? IRC_TAG_PREFIX + username + COLOR_MUTED + ": " + messageText
        : IRC_TAG_PREFIX + COLOR_ACCENT + username + COLOR_MUTED + " (#" + uid + "): " + COLOR_PLAIN + messageText;

    client.print(formatted);
    return false;
}