package com.shanebeestudios.stress;

import com.shanebeestudios.stress.api.bot.Bot;
import com.shanebeestudios.stress.api.bot.BotManager;
import com.shanebeestudios.stress.api.util.Logger;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collection;

/**
 * @hidden
 */
public class Command {

    private final StressTestBots plugin;
    private final BotManager botManager;
    private final BukkitScheduler scheduler;

    public Command(StressTestBots plugin) {
        this.plugin = plugin;
        this.botManager = plugin.getBotManager();
        this.scheduler = Bukkit.getScheduler();
        registerCommand();
    }

    @SuppressWarnings("unchecked")
    public void registerCommand() {
        CommandTree command = new CommandTree("stress")
            .then(new LiteralArgument("info")
                .then(new EntitySelectorArgument.ManyPlayers("players")
                    .executes((sender, args) -> {
                        Collection<Entity> players = (Collection<Entity>) args.get("players");
                        assert players != null;
                        players.forEach(player -> {
                            Bot bot = this.botManager.findBotByName(player.getName());
                            if (bot != null) {
                                Logger.logToSender(sender, "BotInfo: &7'" + bot);
                            }
                        });
                    })))
            // Create a bot
            .then(new LiteralArgument("create")
                .withPermission("stresstestbots.command.create")
                .then(new LiteralArgument("named")
                    .then(new StringArgument("name")
                        .executes((sender, args) -> {
                            String name = (String) args.get("name");
                            Bot bot = this.botManager.createBot(name);
                            if (bot != null) {
                                Logger.logToSender(sender, "Created new bot '&b" + bot.getNickname() + "&7'");
                            } else {
                                Logger.logToSender(sender, "&cFailed to create bot '&b" + name + "&c'");
                            }
                        })))
                .then(new LiteralArgument("random")
                    .then(new IntegerArgument("amount", 1, Bukkit.getMaxPlayers())
                        .setOptional(true)
                        .then(new IntegerArgument("delay-ticks", 0)
                            .setOptional(true)
                            .executes((sender, args) -> {
                                int amount = (int) args.getOrDefault("amount", 1);
                                long delay = (int) args.getOrDefault("delay-ticks", 20);
                                for (int i = 0; i < amount; i++) {
                                    this.scheduler.runTaskLater(this.plugin, () -> {
                                        Bot bot = this.botManager.createBot(null);
                                        if (bot != null) {
                                            Logger.logToSender(sender, "Created new bot '&b" + bot.getNickname() + "&7'");
                                        } else {
                                            Logger.logToSender(sender, "&cFailed to create random bot!");
                                        }
                                    }, delay * i);
                                }
                            })))))

            // Remove a bot
            .then(new LiteralArgument("remove")
                .withPermission("stresstestbots.command.remove")
                .then(new EntitySelectorArgument.ManyPlayers("players")
                    .executes((sender, args) -> {
                        Collection<Entity> players = (Collection<Entity>) args.get("players");
                        assert players != null;
                        players.forEach(player -> {
                            Bot bot = this.botManager.findBotByName(player.getName());
                            if (bot != null) {
                                this.botManager.disconnectBot(bot);
                                Logger.logToSender(sender, "Removed bot &7'" + bot.getNickname() + "&7'");
                            }
                        });
                    })))

            // Make a bot send a chat message or command
            .then(new LiteralArgument("chat")
                .withPermission("stresstestbots.command.chat")
                .then(new EntitySelectorArgument.ManyPlayers("players")
                    .then(new GreedyStringArgument("message")
                        .executes((sender, args) -> {
                            String message = (String) args.get("message");
                            Collection<Entity> players = (Collection<Entity>) args.get("players");
                            assert message != null;
                            assert players != null;
                            players.forEach(player -> {
                                Bot bot = this.botManager.findBotByName(player.getName());
                                if (bot != null) bot.sendChat(message);
                            });
                        }))));

        command.register();
    }

}
