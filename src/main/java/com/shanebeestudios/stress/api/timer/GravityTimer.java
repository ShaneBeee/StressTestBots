package com.shanebeestudios.stress.api.timer;

import com.shanebeestudios.stress.api.bot.Bot;
import com.shanebeestudios.stress.api.bot.BotManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @hidden
 */
public class GravityTimer {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);
    private final BotManager botManager;

    public GravityTimer(BotManager botManager) {
        this.botManager = botManager;
    }


    public void startTimer() {
        EXECUTOR.scheduleAtFixedRate(() -> GravityTimer.this.botManager.getBots().forEach(Bot::fallDown),
            1000L, 50L, TimeUnit.MILLISECONDS);
    }

}
