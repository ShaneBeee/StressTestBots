package com.shanebeestudios.stress.api.timer;

import com.shanebeestudios.stress.api.bot.Bot;
import com.shanebeestudios.stress.api.bot.BotManager;

import java.util.Timer;
import java.util.TimerTask;


/**
 * @hidden
 */
public class GravityTimer {

    private final Timer timer = new Timer();
    private final BotManager botManager;

    public GravityTimer(BotManager botManager) {
        this.botManager = botManager;
    }


    public void startTimer() {
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                GravityTimer.this.botManager.getBots().forEach(Bot::fallDown);
            }
        }, 1000L, 50L);
    }

}
