package thorny.grasscutters.MobWave;

import emu.grasscutter.plugin.Plugin;
import thorny.grasscutters.MobWave.commands.MobWaveCommand;

public final class MobWave extends Plugin {
    private static MobWave instance;
    public static MobWave getInstance() {
        return instance;
    }
    @Override public void onLoad() {
        // Set the plugin instance.
        instance = this;
    }
    @Override public void onEnable() {

        // Register commands.
        this.getHandle().registerCommand(new thorny.grasscutters.MobWave.commands.MobWaveCommand());

        // Log a plugin status message.
        this.getLogger().info("The MobWave plugin has been enabled.");

        // Read file to memory
        MobWaveCommand.readFile();
    }

    @Override public void onDisable() {
        // Log a plugin status message.
        this.getLogger().info("How could you do this to me... mobwave has been disabled.");
    }
}