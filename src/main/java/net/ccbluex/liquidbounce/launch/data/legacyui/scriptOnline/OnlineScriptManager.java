package net.ccbluex.liquidbounce.launch.data.legacyui.scriptOnline;

import net.ccbluex.liquidbounce.script.Script;

public class OnlineScriptManager {
    public static boolean isOnlineScript(Script script) {
        return script.isOnline();
    }
}