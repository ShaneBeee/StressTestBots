package com.shanebeestudios.stress;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.shanebeestudios.stress.api.bot.BotManager;
import com.shanebeestudios.stress.api.util.Logger;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import dev.jorel.commandapi.exceptions.UnsupportedVersionException;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Main plugin class
 */
@SuppressWarnings("unused")
public class StressTestBots extends JavaPlugin {

    private static StressTestBots instance;
    private boolean commandApiCanLoad;
    private BotManager botManager;

    /**
     * @hidden
     */
    @Override
    public void onLoad() {
        if (Bukkit.getOnlineMode()) {
            // Don't load CommandAPI
            return;
        }
        try {
            CommandAPI.onLoad(new CommandAPIPaperConfig(this)
                .verboseOutput(false).silentLogs(true));
            this.commandApiCanLoad = true;
        } catch (UnsupportedVersionException ignore) {
            this.commandApiCanLoad = false;
        }
    }

    /**
     * @hidden
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (Bukkit.getOnlineMode()) {
            Logger.error("***********************************************");
            Logger.error("*                                             *");
            Logger.error("* This plugin will only work in offline mode! *");
            Logger.error("*                                             *");
            Logger.error("***********************************************");
            pluginManager.disablePlugin(this);
            return;
        }

        instance = this;

        if (!this.commandApiCanLoad) {
            Logger.error("CommandAPI could not be loaded, plugin disabling!");
            pluginManager.disablePlugin(this);
            return;
        }
        loadNicknameFile();
        setupBotLogic();
        setupCommand();
        setupMetrics();

        String version = getDescription().getVersion();
        long finish = System.currentTimeMillis() - start;
        Logger.info("&aSuccessfully enabled v%s&7 in &b%s ms", version, finish);
    }

    /**
     * @hidden
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (!Bukkit.getOnlineMode()) CommandAPI.onDisable();
        instance = null;
    }

    private void loadNicknameFile() {
        if (!new File(getDataFolder(), "nicks.txt").exists()) {
            this.saveResource("nicks.txt", false);
        }
    }

    private void setupBotLogic() {
        this.botManager = new BotManager();
    }

    private void setupCommand() {
        CommandAPI.onEnable();
        new Command(this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 30808);

        // New Metrics
        // Many of these are copied from Skript -> SkriptMetrics.class
        metrics.addCustomChart(new DrilldownPie("plugin_version_drilldown_pie", () -> {
            String version = this.getPluginMeta().getVersion();
            Table<String, String, Integer> table = HashBasedTable.create(1, 1);
            table.put(
                version.split("-")[0], // upper label
                version, // lower label
                1 // weight
            );
            return table.rowMap();
        }));
        metrics.addCustomChart(new DrilldownPie("minecraft_version_drilldown_pie", () -> {
            String[] version = Bukkit.getMinecraftVersion().split("\\.");
            Table<String, String, Integer> table = HashBasedTable.create(1, 1);

            table.put(
                version[0] + ".x", // upper label
                String.join(".", version), // lower label
                1 // weight
            );
            return table.rowMap();
        }));
    }

    /**
     * Get an instance of this plugin
     *
     * @return Instance of this plugin
     */
    // Getters
    public static StressTestBots getInstance() {
        return instance;
    }

    /**
     * Get an instance of the {@link BotManager}
     *
     * @return Instance of bot manager
     */
    public BotManager getBotManager() {
        return this.botManager;
    }

}
