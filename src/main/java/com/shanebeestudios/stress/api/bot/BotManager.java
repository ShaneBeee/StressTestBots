package com.shanebeestudios.stress.api.bot;

import com.shanebeestudios.stress.api.event.BotCreateEvent;
import com.shanebeestudios.stress.api.generator.NickGenerator;
import com.shanebeestudios.stress.api.timer.GravityTimer;
import com.shanebeestudios.stress.api.util.Logger;
import com.shanebeestudios.stress.api.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bot manager for server plugin
 */
@SuppressWarnings("unused")
public class BotManager {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    private final int autoRespawnDelay;
    private final boolean hasGravity;
    private final List<String> joinMessages = new ArrayList<>();
    private final InetSocketAddress inetAddr;
    private final List<Bot> bots = new ArrayList<>();
    private final NickGenerator nickGenerator;
    private final GravityTimer gravityTimer;

    /**
     * @hidden
     */
    public BotManager() {
        this.autoRespawnDelay = 3000;
        this.inetAddr = Utils.createInetAddress(getServerAddress(), Bukkit.getPort());
        this.nickGenerator = new NickGenerator("plugins/StressTestBots/nicks.txt", "", true);
        this.hasGravity = true;
        this.gravityTimer = new GravityTimer(this);
        this.gravityTimer.startTimer();
    }

    /**
     * Create an instance of bot manager
     *
     * @param autoRespawnDelay Delay for auto-respawning (0 = instant, -1 = never)
     * @param hasGravity       Whether the bots have gravity
     * @param inetAddr         Address to connect bots to (see {@link Utils#createInetAddress(String, int)})
     * @param nickPath         Path for nickname file (if null will generate from built in file)
     * @param nickPrefix       Prefix for nicknames
     */
    public BotManager(int autoRespawnDelay, boolean hasGravity, InetSocketAddress inetAddr, @Nullable String nickPath, @Nullable String nickPrefix) {
        this.autoRespawnDelay = autoRespawnDelay;
        this.hasGravity = hasGravity;
        this.inetAddr = inetAddr;
        this.nickGenerator = new NickGenerator(nickPath, nickPrefix, true);
        this.gravityTimer = new GravityTimer(this);
        this.gravityTimer.startTimer();
    }

    /**
     * Get the auto respawn delay
     *
     * @return Auto respawn delay
     */
    public int getAutoRespawnDelay() {
        return this.autoRespawnDelay;
    }

    /**
     * Wether the gravity timer is enabled
     *
     * @return Gravity timer is enabled
     */
    public boolean hasGravity() {
        return this.hasGravity;
    }

    /**
     * @return hidden
     * @hidden currently unused
     */
    public List<String> getJoinMessages() {
        return this.joinMessages;
    }

    /**
     * Get the InetAddress these bots will connect to
     *
     * @return Address of connection
     */
    public InetSocketAddress getInetAddr() {
        return this.inetAddr;
    }

    /**
     * @return hidden
     * @hidden not sure if this has a use by others
     */
    public GravityTimer getGravityTimer() {
        return this.gravityTimer;
    }

    /**
     * Get all loaded bots
     *
     * @return All loaded bots
     */
    public List<Bot> getBots() {
        return this.bots;
    }

    /**
     * Get instance of nick generator
     *
     * @return Nick generator
     */
    public NickGenerator getNickGenerator() {
        return this.nickGenerator;
    }

    /**
     * Remove a bot
     *
     * @param bot Bot to remove
     */
    public void removeBot(Bot bot) {
        this.bots.remove(bot);
    }

    /**
     * Disconnect and remove a bot
     *
     * @param bot Bot to disconnect and remove
     */
    public void disconnectBot(Bot bot) {
        bot.disconnect();
        this.bots.remove(bot);
    }

    /**
     * Find a bot by name
     *
     * @param text Name of player to get bot from
     * @return Bot from name
     */
    @Nullable
    public Bot findBotByName(String text) {
        for (Bot bot : this.getBots()) {
            // Starts with and ignore case
            // https://stackoverflow.com/a/38947571/11787611
            if (bot.getNickname().regionMatches(true, 0, text, 0, text.length())) {
                return bot;
            }
        }
        return null;
    }

    /**
     * Create a bot
     *
     * @param name Name of bot or null to create random named bot
     *             Name must be between 1 and 16 characters
     * @return Bot if was created, else null
     */
    @Nullable
    public Bot createBot(@Nullable String name) {
        return createBot(name, 0);
    }

    /**
     * Create a bot
     *
     * @param name       Name of bot or null to create random named bot
     *                   Name must be between 1 and 16 characters
     * @param loginDelay Delay in ticks for the bot to login
     * @return Bot if was created, else null
     */
    @Nullable
    public Bot createBot(@Nullable String name, long loginDelay) {
        if (name != null && name.length() > 16) return null;

        String botname = name != null ? name : getNickGenerator().nextNick();
        Bot bot = new Bot(this, botname, getInetAddr(), null);
        Player player = Bukkit.getPlayer(botname);
        if (player != null) {
            // Let's not create a bot if a player with that name is already online
            return null;
        }
        this.bots.add(bot);
        if (loginDelay > 0) {
            EXECUTOR.schedule(bot::connect, loginDelay * 50, TimeUnit.MILLISECONDS);
        } else {
            bot.connect();
        }
        BotCreateEvent botCreateEvent = new BotCreateEvent(bot);
        botCreateEvent.callEvent();
        return bot;
    }

    private String getServerAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Log to console that a bot was created
     *
     * @param name Name of bot
     */
    public void logBotCreated(String name) {
        Logger.info("Bot '&b" + name + "&7' created");
    }

    /**
     * Log disconnection of bot to console
     *
     * @param name Name of bot
     */
    public void logBotDisconnected(String name) {
        Logger.info("Bot '&b" + name + "&7' disconnected");
    }

}
