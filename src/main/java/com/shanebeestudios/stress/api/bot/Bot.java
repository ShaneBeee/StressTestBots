package com.shanebeestudios.stress.api.bot;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collections;

/**
 * Represents a bot that can join the server
 */
@SuppressWarnings("unused")
public class Bot {

    private final MinecraftProtocol protocol;
    private final BotManager botManager;
    private final String nickname;
    private final Session client;
    private double lastX, lastY, lastZ = -1;
    private boolean connected;
    private boolean manualDisconnecting = false;

    /**
     * Create an offline server bot
     *
     * @param botManager Instance of bot manager
     * @param nickname   Name of the bot
     * @param address    Address the bot will connect to
     * @param proxy      Proxy if available
     */
    public Bot(@NotNull BotManager botManager, @NotNull String nickname, @NotNull InetSocketAddress address, @Nullable ProxyInfo proxy) {
        this.botManager = botManager;
        this.nickname = nickname;

        botManager.logBotCreated(nickname);
        this.protocol = new MinecraftProtocol(nickname);
        this.client = new TcpClientSession(address.getHostString(), address.getPort(), this.protocol, proxy);
    }

    /**
     * Create an online authenticated bot
     *
     * @param botManager  Instance of bot manager
     * @param authService Auth service for connection
     * @param address     Address the bot will connect to
     * @param proxy       Proxy if available
     */
    public Bot(BotManager botManager, @NotNull AuthenticationService authService, @NotNull InetSocketAddress address, @Nullable ProxyInfo proxy) {
        this.botManager = botManager;
        this.nickname = authService.getUsername();

        botManager.logBotCreated(this.nickname);
        this.protocol = new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());

        this.client = new TcpClientSession(address.getHostString(), address.getPort(), this.protocol, proxy);

        SessionService sessionService = new SessionService();
        this.client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
    }

    /**
     * Connect the bot to the server
     */
    public void connect() {
        new Thread(() -> {
            // We'll manage the keep alive, to create a fake letancy
            this.client.setFlag("manage-keep-alive", false);
            this.client.addListener(new PacketListener(this));
            this.client.connect();
        }).start();
    }

    /**
     * Get the bot manager
     *
     * @return Bot manager
     */
    public BotManager getBotManager() {
        return this.botManager;
    }

    /**
     * Get the session client this bot connects from
     *
     * @return Session client
     */
    public Session getClient() {
        return this.client;
    }

    /**
     * Get the name of this bot
     *
     * @return Name of this bot
     */
    public String getNickname() {
        return this.nickname;
    }

    /**
     * Check if the bot is connected
     *
     * @return True if connected else false
     */
    public boolean isConnected() {
        return this.connected;
    }

    /**
     * Set whether the bot is connected
     *
     * @param connected Bot is connected
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Check if the bot was manually disconnected
     *
     * @return True if manually disconnected, false if removed by server
     */
    public boolean isManualDisconnecting() {
        return this.manualDisconnecting;
    }

    /**
     * Send a chat message from the bot
     *
     * @param text Message (or command) to send
     */
    public void sendChat(String text) {
        // timeStamp will provide when this message was sent by the user. If this value was not set or was set to 0,
        // The server console will print out that the message was "expired". To avoid this, set timeStamp as now.
        long timeStamp = Instant.now().toEpochMilli();
        BitSet bitSet = new BitSet();

        Packet packet;
        if (text.startsWith("/")) {
            // Send command
            packet = new ServerboundChatCommandPacket(text.substring(1), timeStamp, 0L, Collections.emptyList(), 0, bitSet);
        } else {
            // Send chat
            packet = new ServerboundChatPacket(text, timeStamp, 0L, null, 0, bitSet);
        }
        this.client.send(packet);
    }

    /**
     * @hidden
     */
    public void fallDown() {
        if (this.connected && this.lastY > 0) {
            move(0, -0.5, 0);
        }
    }

    /**
     * Set the last postion of this bot
     * <p>May be renamed later</p>
     *
     * @param x X pos of bot
     * @param y Y pos of bot
     * @param z Z pos of bot
     */
    public void setLastPosition(double x, double y, double z) {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    /**
     * Move the bot to a new location
     * <p>Will send a packet to the server as well</p>
     *
     * @param x X coord of new location
     * @param y Y coord of new location
     * @param z Z coord of new location
     */
    public void move(double x, double y, double z) {
        this.lastX += x;
        this.lastY += y;
        this.lastZ += z;
        moveTo(this.lastX, this.lastY, this.lastZ);
    }

    private void moveTo(double x, double y, double z) {
        this.client.send(new ServerboundMovePlayerPosPacket(true, x, y, z));
    }

    /**
     * Disconnect the bot from the server
     */
    public void disconnect() {
        this.manualDisconnecting = true;
        this.client.disconnect("Leaving");
    }

}
