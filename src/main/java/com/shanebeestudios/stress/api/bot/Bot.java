package com.shanebeestudios.stress.api.bot;

import com.shanebeestudios.stress.StressTestBots;
import com.shanebeestudios.stress.api.event.BotDisconnectEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.netty.DefaultPacketHandlerExecutor;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a bot that can join the server
 */
@SuppressWarnings("unused")
public class Bot {

    private final BotManager botManager;
    private final String nickname;
    private final ClientSession client;
    private double lastX, lastY, lastZ = -1;
    private double highestY;
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
        MinecraftProtocol protocol = new MinecraftProtocol(nickname);
        this.client = new ClientNetworkSession(address, protocol, DefaultPacketHandlerExecutor.createExecutor(), null, proxy);
    }

    /**
     * Connect the bot to the server
     */
    public void connect() {
        // We'll manage the keep alive, to create a fake letancy
        this.client.setFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, false);
        this.client.addListener(new PacketListener(this));
        this.client.connect(true);
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
     * Set whether the bot is connected to the server
     *
     * @param connected Bot is connected to the server
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
            packet = new ServerboundChatCommandPacket(text.substring(1));
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
            double distance = this.lastY - this.highestY;
            if (distance > 0.5) {
                move(0, -0.5, 0);
            } else if (distance > 0) {
                this.lastY = this.highestY;
                moveTo(this.lastX, this.lastY, this.lastZ);
            }
        }
    }

    private CompletableFuture<Double> requestHighestBlock() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(StressTestBots.getInstance(), () -> {
            Player player = Bukkit.getPlayer(Bot.this.getNickname());
            if (player != null) {
                Block block = player.getWorld().getHighestBlockAt(player.getLocation());
                BoundingBox boundingBox = block.getBoundingBox();

                Location location = block.getLocation().add(0, boundingBox.getHeight(), 0);
                future.complete(location.getY());
            }
        });
        return future;
    }

    private void updateHighestY() {
        requestHighestBlock().thenAccept(highestY -> this.highestY = highestY);
    }

    /**
     * Handle a block change from the server
     * <p>Used to recalculate highest Y position</p>
     *
     * @param block Location of block that changed
     */
    public void blockChange(Vector3i block) {
        double x = Math.abs(block.getX() - this.lastX);
        double z = Math.abs(block.getZ() - this.lastZ);
        if (x < 1.5 && z < 1.5) {
            updateHighestY();
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
        if (this.lastX != x && this.lastZ != z) {
            updateHighestY();
        }
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    public void updatePosToServer() {
        moveTo(this.lastX, this.lastY, this.lastZ);
    }

    /**
     * Move the bot by an amount
     * <p>NOTE: The bot should not move more than 8 blocks</p>
     * <p>Will send a packet to the server as well</p>
     *
     * @param x Amount to move on the X axis
     * @param y Amount to move on the Y axis
     * @param z Amount to move on the Z axis
     */
    public void move(double x, double y, double z) {
        this.lastX += x;
        this.lastY += y;
        this.lastZ += z;
        moveTo(this.lastX, this.lastY, this.lastZ);
    }

    /**
     * Move the bot to a new location
     * <p>NOTE: The bot should not move more than 8 blocks</p>
     *
     * @param x X coord of new location
     * @param y Y coord of new location
     * @param z Z coord of new location
     */
    public void moveTo(double x, double y, double z) {
        this.client.send(new ServerboundMovePlayerPosPacket(false, true, x, y, z));
    }

    /**
     * Move the bot to a new location
     * <p>NOTE: The bot should not move more than 8 blocks</p>
     *
     * @param x     X coord of new location
     * @param y     Y coord of new location
     * @param z     Z coord of new location
     * @param yaw   Yaw of new location
     * @param pitch Pitch of new location
     */
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        this.client.send(new ServerboundMovePlayerPosRotPacket(true, true, x, y, z, yaw, pitch));
    }

    /**
     * Rotate the bot
     *
     * @param yaw   Yaw of new position
     * @param pitch Pitch of new position
     */
    private void look(float yaw, float pitch) {
        this.client.send(new ServerboundMovePlayerRotPacket(true, true, yaw, pitch));
    }

    /**
     * Disconnect the bot from the server
     */
    public void disconnect() {
        this.manualDisconnecting = true;
        this.client.disconnect("Leaving");
        BotDisconnectEvent botDisconnectEvent = new BotDisconnectEvent(this);
        botDisconnectEvent.callEvent();
    }

    @Override
    public String toString() {
        String x = String.format("%.2f", this.lastX);
        String y = String.format("%.2f", this.lastY);
        String z = String.format("%.2f", this.lastZ);
        return "Bot{" +
            "nickname='" + nickname + '\'' +
            ", lastX=" + x +
            ", lastY=" + y +
            ", lastZ=" + z +
            ", highestY=" + highestY +
            ", connected=" + connected +
            '}';
    }

}
