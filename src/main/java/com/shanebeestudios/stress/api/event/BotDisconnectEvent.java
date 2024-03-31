package com.shanebeestudios.stress.api.event;

import com.shanebeestudios.stress.api.bot.Bot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a bot is disconnected
 */
public class BotDisconnectEvent extends BotEvent {

    private static final HandlerList handlers = new HandlerList();

    public BotDisconnectEvent(Bot bot) {
        super(bot);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

}
