package net.cardinalboats.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;


import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@Config(name = "CardinalBoat")
public class CIBConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean showStatusMessages = true;

    @ConfigEntry.Gui.Tooltip
    public boolean maintainVelocityOnTurns = false;

    @ConfigEntry.Gui.Tooltip
    public boolean eightWaySnapKey = true;

    @ConfigEntry.Gui.Tooltip
    public boolean smartCenterOnSnap = false;

    @ConfigEntry.Gui.Tooltip
    public boolean moveWhileChatting = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 2*20) //max 2 seconds
    public int ticksToMoveAfterChatHides = 15; // one and a half seconds

    @ConfigEntry.Gui.Tooltip
    public boolean alwaysSmartCenter = false;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int smartCenterLookAhead = 5;

    @ConfigEntry.Gui.Tooltip
    public boolean smartCenterPrimedTurn = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    public int smartCenterPrimedTurnDelayTicks = 30;


    public static void init() {
        // Peek at the legacy key before AutoConfig loads, then carry it over
        // after registration (see readLegacyShowStatusMessages).
        boolean legacyOptOut = readLegacyShowStatusMessages();
        AutoConfig.register(CIBConfig.class, Toml4jConfigSerializer::new);
        if (legacyOptOut) {
            ConfigHolder<CIBConfig> holder = AutoConfig.getConfigHolder(CIBConfig.class);
            holder.getConfig().showStatusMessages = false;
            holder.save();
        }
    }

    public static CIBConfig getInstance() {
        return AutoConfig.getConfigHolder(CIBConfig.class).getConfig();
    }

    // Cloth AutoConfig has no key-migration API: on load it silently drops keys
    // it does not know, so the doChatShit -> showStatusMessages rename (2.2.3)
    // would reset every existing opt-out on the next save. Read the config file
    // before AutoConfig touches it and map the old value onto the new field.
    private static boolean readLegacyShowStatusMessages() {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("CardinalBoat.toml");
            if (!Files.exists(path)) {
                return false;
            }
            return Pattern.compile("\\bdoChatShit\\s*=\\s*false\\b")
                    .matcher(Files.readString(path))
                    .find();
        } catch (Exception e) {
            return false;
        }
    }
}
