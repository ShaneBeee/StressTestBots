package com.shanebeestudios.stress.api.bot;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.level.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @hidden
 */
@SuppressWarnings({"DuplicatedCode", "FieldCanBeLocal", "unused"})
public class PacketListener extends SessionAdapter {

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
        this.latency = new Random().nextInt(100, 1000);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundGameEventPacket gameEventPacket) {
            gameEvent(gameEventPacket);
        } else if (packet instanceof ClientboundLoginPacket loginPacket) {
            login(loginPacket);
        } else if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
            playerPosition(positionPacket);
        } else if (packet instanceof ClientboundPlayerCombatKillPacket killPacket) {
            playerDeath(killPacket);
        } else if (packet instanceof ClientboundKeepAlivePacket keepAlivePacket) {
            playerLatency(keepAlivePacket);
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
        this.bot.setConnected(true);

        // Might implement this later
        if (!this.joinMessages.isEmpty()) {
            for (String msg : joinMessages) {
                this.bot.sendChat(msg);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void playerPosition(ClientboundPlayerPositionPacket positionPacket) {
        this.bot.setLastPosition(positionPacket.getX(), positionPacket.getY(), positionPacket.getZ());
        this.client.send(new ServerboundAcceptTeleportationPacket(positionPacket.getTeleportId()));
    }

    @SuppressWarnings("unused")
    private void playerDeath(ClientboundPlayerCombatKillPacket killPacket) {
        if (this.autoRespawnDelay < 0) return;
        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    PacketListener.this.client.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
                }
            }, this.autoRespawnDelay);
    }

    private void playerLatency(ClientboundKeepAlivePacket keepAlivePacket) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                PacketListener.this.client.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
            }
        }, this.latency);
    }

    public void disconnected(DisconnectedEvent event) {
        this.bot.setConnected(false);
        this.botManager.logBotDisconnected(this.bot.getNickname());
        this.botManager.removeBot(this.bot);
        Thread.currentThread().interrupt();
    }

}
