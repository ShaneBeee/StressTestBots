package com.shanebeestudios.stress.api.bot;

import com.shanebeestudios.stress.api.util.Logger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RespawnScreenValue;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @hidden
 */
@SuppressWarnings({"DuplicatedCode", "FieldCanBeLocal", "unused"})
public class PacketListener extends SessionAdapter {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    private final Bot bot;
    private int entityId;
    private final Session client;
    private final BotManager botManager;
    private final List<String> joinMessages;
    private int autoRespawnDelay;
    private final int latency;

    public PacketListener(Bot bot) {
        this.bot = bot;
        this.client = bot.getClient();
        this.botManager = bot.getBotManager();
        this.joinMessages = this.botManager.getJoinMessages();
        this.autoRespawnDelay = this.botManager.getAutoRespawnDelay();
        this.latency = new Random().nextInt(25, 500);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        switch (packet) {
            case ClientboundGameEventPacket gameEventPacket -> gameEvent(gameEventPacket);
            case ClientboundLoginPacket loginPacket -> login(loginPacket);
            case ClientboundPlayerPositionPacket positionPacket -> playerPosition(positionPacket);
            case ClientboundPlayerCombatKillPacket killPacket -> playerDeath(killPacket);
            case ClientboundKeepAlivePacket keepAlivePacket -> playerLatency(keepAlivePacket);
            case ClientboundBlockUpdatePacket blockUpdatePacket -> blockChange(blockUpdatePacket);
            default -> {
            }
        }
    }

    private void gameEvent(ClientboundGameEventPacket gameEventPacket) {
        if (gameEventPacket.getNotification() == GameEvent.ENABLE_RESPAWN_SCREEN) {
            RespawnScreenValue respawnScreenValue = (RespawnScreenValue) gameEventPacket.getValue();
            if (respawnScreenValue == RespawnScreenValue.IMMEDIATE_RESPAWN) {
                this.autoRespawnDelay = 0;
            } else {
                this.autoRespawnDelay = this.botManager.getAutoRespawnDelay();
            }
        }
    }

    private void login(ClientboundLoginPacket loginPacket) {
        this.entityId = loginPacket.getEntityId();
        if (!loginPacket.isEnableRespawnScreen()) {
            this.autoRespawnDelay = 0;
        }
        // Delay to make sure gravity works
        // No clue why it is needed, but here we are
        EXECUTOR.schedule(() -> {
            PacketListener.this.bot.setConnected(true);
            startPosUpdateTimer();
        }, 50, TimeUnit.MILLISECONDS);
    }

    private void playerPosition(ClientboundPlayerPositionPacket packet) {
        Vector3d position = packet.getPosition();
        this.bot.setLastPosition(position.getX(), position.getY(), position.getZ());
        this.client.send(new ServerboundAcceptTeleportationPacket(packet.getId()));
    }

    @SuppressWarnings("unused")
    private void playerDeath(ClientboundPlayerCombatKillPacket killPacket) {
        if (this.autoRespawnDelay < 0) return;
        EXECUTOR.schedule(() -> PacketListener.this.client.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN)),
            this.autoRespawnDelay, TimeUnit.MILLISECONDS);
    }

    private void playerLatency(ClientboundKeepAlivePacket keepAlivePacket) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                PacketListener.this.client.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
            }
        }, this.latency);
    }

    private void blockChange(ClientboundBlockUpdatePacket packet) {
        BlockChangeEntry entry = packet.getEntry();
        this.bot.blockChange(entry.getPosition());
    }

    public void disconnected(DisconnectedEvent event) {
        this.bot.setConnected(false);
        this.botManager.logBotDisconnected(this.bot.getNickname());
        this.botManager.removeBot(this.bot);
        String reason = LegacyComponentSerializer.legacyAmpersand().serialize(event.getReason());
        Logger.info("Bot disconnected reason: &e" + reason);
        Thread.currentThread().interrupt();
    }

    private void startPosUpdateTimer() {
        EXECUTOR.scheduleAtFixedRate(PacketListener.this.bot::updatePosToServer,
            1, 1, TimeUnit.SECONDS);
    }

}
