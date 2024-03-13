package com.shanebeestudios.mcbots.api.bot;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.packetlib.ProxyInfo;
import com.shanebeestudios.mcbots.standalone.bot.StandaloneSessionListener;
import com.shanebeestudios.mcbots.standalone.bot.StandaloneBotManager;
import com.shanebeestudios.mcbots.api.generator.NickGenerator;
import com.shanebeestudios.mcbots.api.generator.ProxyGenerator;
import com.shanebeestudios.mcbots.api.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;

public class BotLoader {

    private final StandaloneBotManager standaloneLoader;
    private final int delayMin;
    private final int delayMax;
    private final Random random;
    private int triedToConnect = 0;
    private int proxyIndex = 0;

    public BotLoader(StandaloneBotManager standaloneLoader) {
        this.standaloneLoader = standaloneLoader;
        this.delayMin = standaloneLoader.getDelayMin();
        this.delayMax = standaloneLoader.getDelayMax();
        this.random = new Random();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void spin() {
        int botCount = this.standaloneLoader.getBotCount();
        boolean minimal = this.standaloneLoader.isMinimal();
        boolean mostMinimal = this.standaloneLoader.isMostMinimal();
        InetSocketAddress inetAddr = this.standaloneLoader.getInetAddr();
        NickGenerator nickGenerator = this.standaloneLoader.getNickGenerator();
        AuthenticationService authService = this.standaloneLoader.getAuthenticationService();

        ProxyGenerator proxyGenerator = this.standaloneLoader.getProxyGenerator();
        int proxyCount = proxyGenerator != null ? proxyGenerator.getProxies().size() : 0;
        ArrayList<InetSocketAddress> proxies = proxyGenerator != null ? proxyGenerator.getProxies() : null;

        new Thread(() -> {
            for (int i = 0; i < botCount; i++) {
                try {
                    ProxyInfo proxyInfo = null;
                    if (proxies != null && !proxies.isEmpty()) {
                        InetSocketAddress proxySocket = proxies.get(this.proxyIndex);

                        if (!minimal) {
                            Logger.info("Using proxy: (" + this.proxyIndex + ")", proxySocket.getHostString() + ":" + proxySocket.getPort());
                        }

                        proxyInfo = new ProxyInfo(proxyGenerator.getProxyType(), proxySocket);

                        //increment or reset current proxy index
                        if (this.proxyIndex < (proxyCount - 1)) {
                            this.proxyIndex++;
                        } else {
                            this.proxyIndex = 0;
                        }
                    }

                    Bot bot;
                    if (authService != null) {
                        bot = new Bot(this.standaloneLoader, authService, inetAddr, proxyInfo);
                    } else {
                        bot = new Bot(this.standaloneLoader, nickGenerator.nextNick(), inetAddr, proxyInfo);
                    }
                    bot.setupSessionListener(new StandaloneSessionListener());
                    bot.connect();

                    if (!mostMinimal) this.standaloneLoader.getBots().add(bot);

                    this.triedToConnect++;

                    if (this.standaloneLoader.isMainListenerMissing() && !minimal) {
                        this.standaloneLoader.setMainListenerMissing(false);
                        bot.registerMainListener();
                    }

                    if (i < botCount - 1) {
                        long delay = getRandomDelay();
                        Logger.info("Waiting", delay + "", "ms");
                        Thread.sleep(delay);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public int getTriedToConnect() {
        return this.triedToConnect;
    }

    private long getRandomDelay() {
        return this.random.nextInt(this.delayMax - this.delayMin) + this.delayMin;
    }

}