package com.shanebeestudios.stress.api.event;

import com.shanebeestudios.stress.api.bot.Bot;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * @hidden Abstract bot event
 */
@SuppressWarnings("unused")
public abstract class BotEvent extends Event {

    private final Bot bot;

    BotEvent(Bot bot) {
        this.bot = bot;
    }

    /**
     * Get the bot that was created
     *
     * @return Bot that was created
     */
    public Bot getBot() {
        return this.bot;
    }

    /**
     * This overrides a paper method, hence the boolean
     *
     * @return always returns true
     * @hidden
     */
    @Override
    public boolean callEvent() {
        Bukkit.getPluginManager().callEvent(this);
        return true;
    }

}
