package com.minehuina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.kyori.adventure.text.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static Set<String> OPS = new HashSet<>();
    public static Set<String> BANNED = new HashSet<>();
    public static Set<String> MUTED = new HashSet<>();
    public static Set<String> WHITELIST = new HashSet<>();
    public static boolean whitelistEnabled = false;
    public static InstanceContainer worldInstance;

    public static void main(String[] args) {
        System.out.println("Запускаем наше кастомное ядро...");

        OPS = DataManager.loadOps();
        BANNED = DataManager.loadBans();
        MUTED = DataManager.loadMutes();
        WHITELIST = DataManager.loadWhitelist();
        System.out.println("Загружено: OP=" + OPS.size() + ", Баны=" + BANNED.size() + ", Муты=" + MUTED.size() + ", Whitelist=" + WHITELIST.size());

        MinecraftServer server = MinecraftServer.init();
        CommandManager commandManager = MinecraftServer.getCommandManager();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        worldInstance = instanceManager.createInstanceContainer();

        worldInstance.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 40, Block.STONE);
            unit.modifier().fillHeight(40, 43, Block.DIRT);
            unit.modifier().fillHeight(43, 44, Block.GRASS_BLOCK);
        });

        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            String name = player.getUsername().toLowerCase();

            if (BANNED.contains(name)) {
                player.kick(Component.text("§cТы забанен."));
                return;
            }
            if (whitelistEnabled && !WHITELIST.contains(name)) {
                player.kick(Component.text("§cСервер закрыт (whitelist)."));
                return;
            }
            event.setSpawningInstance(worldInstance);
            player.setRespawnPoint(new Pos(0, 45, 0));
        });

        handler.addListener(PlayerChatEvent.class, event -> {
            if (MUTED.contains(event.getPlayer().getUsername().toLowerCase())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("§cТы замучен."));
            }
        });

        handler.addListener(PlayerSpawnEvent.class, event -> {
            event.getPlayer().sendMessage(Component.text("§aДобро пожаловать!"));
        });

        registerStop(commandManager);
        registerAdminCommands(commandManager);

        startConsoleThread(commandManager);
        server.start("0.0.0.0", 25565);
        System.out.println("Сервер запущен!");
    }

    private static void registerAdminCommands(CommandManager cm) {
        registerAdmin(cm, "op", (s, args) -> {
            if (args.length > 0) {
                OPS.add(args[0].toLowerCase());
                DataManager.saveOps(OPS);
                s.sendMessage(Component.text("§aOP выдан " + args[0]));
            }
        });
        registerAdmin(cm, "deop", (s, args) -> {
            if (args.length > 0) {
                OPS.remove(args[0].toLowerCase());
                DataManager.saveOps(OPS);
                s.sendMessage(Component.text("§aOP забран у " + args[0]));
            }
        });
        registerAdmin(cm, "ban", (s, args) -> {
            if (args.length > 0) {
                BANNED.add(args[0].toLowerCase());
                DataManager.saveBans(BANNED);
                Player p = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(args[0]);
                if (p != null) p.kick(Component.text("§cЗабанен."));
                s.sendMessage(Component.text("§aЗабанен."));
            }
        });
        registerAdmin(cm, "unban", (s, args) -> {
            if (args.length > 0 && BANNED.remove(args[0].toLowerCase())) {
                DataManager.saveBans(BANNED);
                s.sendMessage(Component.text("§aРазбанен."));
            }
        });
        registerAdmin(cm, "pardon", (s, args) -> {
            if (args.length > 0 && BANNED.remove(args[0].toLowerCase())) {
                DataManager.saveBans(BANNED);
                s.sendMessage(Component.text("§aРазбанен."));
            }
        });
        registerAdmin(cm, "mute", (s, args) -> {
            if (args.length > 0) {
                MUTED.add(args[0].toLowerCase());
                DataManager.saveMutes(MUTED);
                s.sendMessage(Component.text("§aЗамучен."));
            }
        });
        registerAdmin(cm, "unmute", (s, args) -> {
            if (args.length > 0 && MUTED.remove(args[0].toLowerCase())) {
                DataManager.saveMutes(MUTED);
                s.sendMessage(Component.text("§aРазмучен."));
            }
        });
        registerAdmin(cm, "whitelist", (s, args) -> {
            if (args.length == 0) return;
            switch (args[0].toLowerCase()) {
                case "on" -> { whitelistEnabled = true; s.sendMessage(Component.text("§aВайтлист ВКЛ.")); }
                case "off" -> { whitelistEnabled = false; s.sendMessage(Component.text("§aВайтлист ВЫКЛ.")); }
                case "add" -> {
                    if (args.length > 1) {
                        WHITELIST.add(args[1].toLowerCase());
                        DataManager.saveWhitelist(WHITELIST);
                        s.sendMessage(Component.text("§aДобавлен."));
                    }
                }
                case "remove" -> {
                    if (args.length > 1) {
                        WHITELIST.remove(args[1].toLowerCase());
                        DataManager.saveWhitelist(WHITELIST);
                        s.sendMessage(Component.text("§aУдален."));
                    }
                }
                case "list" -> s.sendMessage(Component.text("§eВ вайтлисте: " + WHITELIST));
            }
        });
        registerAdmin(cm, "kick", (s, args) -> {
            if (args.length > 0) {
                Player p = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(args[0]);
                if (p != null) { p.kick(Component.text("§cКикнут.")); s.sendMessage(Component.text("§aКикнут.")); }
            }
        });
        registerAdmin(cm, "gamemode", (s, args) -> {
            if (!(s instanceof Player p) || args.length == 0) return;
            switch (args[0].toLowerCase()) {
                case "0", "s", "survival" -> p.setGameMode(GameMode.SURVIVAL);
                case "1", "c", "creative" -> p.setGameMode(GameMode.CREATIVE);
                case "2", "a", "adventure" -> p.setGameMode(GameMode.ADVENTURE);
                case "3", "sp", "spectator" -> p.setGameMode(GameMode.SPECTATOR);
            }
        });
        registerAdmin(cm, "give", (s, args) -> {
            if (!(s instanceof Player p) || args.length == 0) return;
            try {
                Material mat = Material.fromKey(args[0].toLowerCase());
                if (mat != null) {
                    int amt = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                    p.getInventory().addItemStack(ItemStack.of(mat, amt));
                }
            } catch (Exception ignored) {}
        });
        registerAdmin(cm, "tp", (s, args) -> {
            if (!(s instanceof Player p) || args.length == 0) return;
            Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(args[0]);
            if (target != null) p.teleport(target.getPosition());
        });
        registerAdmin(cm, "clear", (s, args) -> {
            if (!(s instanceof Player p) || args.length == 0) return;
            Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(args[0]);
            if (target != null) target.getInventory().clear();
        });
        registerAdmin(cm, "xp", (s, args) -> {
            if (!(s instanceof Player p) || args.length == 0) return;
            try { p.setLevel(p.getLevel() + Integer.parseInt(args[0])); } catch (Exception ignored) {}
        });
        registerAdmin(cm, "say", (s, args) -> {
            String msg = String.join(" ", args);
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(p -> p.sendMessage(Component.text("§d[SERVER] " + msg)));
        });
        registerAdmin(cm, "time", (s, args) -> {
            if (args.length >= 2 && args[0].equalsIgnoreCase("set")) {
                long time = switch (args[1].toLowerCase()) {
                    case "day" -> 1000L;
                    case "night" -> 13000L;
                    default -> Long.parseLong(args[1]);
                };
                if (worldInstance != null) worldInstance.setTime(time);
            }
        });
        registerAdmin(cm, "weather", (s, args) -> s.sendMessage(Component.text("§eПогода (заглушка): " + args[0])));
        registerAdmin(cm, "save-all", (s, args) -> s.sendMessage(Component.text("§aСохранено.")));
        registerAdmin(cm, "difficulty", (s, args) -> s.sendMessage(Component.text("§eСложность (заглушка).")));
        registerAdmin(cm, "gamerule", (s, args) -> s.sendMessage(Component.text("§eГеймрулы (заглушка).")));
        registerAdmin(cm, "ban-ip", (s, args) -> s.sendMessage(Component.text("§cBan-IP пока не реализован.")));
        registerAdmin(cm, "banlist", (s, args) -> s.sendMessage(Component.text("§eБаны: " + BANNED)));
    }

    public interface Executor { void run(net.minestom.server.command.CommandSender s, String[] args); }

    private static void registerAdmin(CommandManager cm, String name, Executor executor) {
        Command cmd = new Command(name);
        cmd.setDefaultExecutor((s, c) -> {
            if (isAdmin(s)) executor.run(s, new String[0]);
            else s.sendMessage(Component.text("§cУ тебя нет прав."));
        });
        var argsArg = ArgumentType.StringArray("args");
        cmd.addSyntax((s, c) -> {
            if (isAdmin(s)) executor.run(s, c.get(argsArg));
            else s.sendMessage(Component.text("§cУ тебя нет прав."));
        }, argsArg);
        cm.register(cmd);
    }

    private static boolean isAdmin(net.minestom.server.command.CommandSender sender) {
        if (sender instanceof Player p) return OPS.contains(p.getUsername().toLowerCase());
        return true;
    }

    private static void registerStop(CommandManager cm) {
        Command cmd = new Command("stop");
        cmd.setDefaultExecutor((s, c) -> { MinecraftServer.stopCleanly(); System.exit(0); });
        cm.register(cmd);
    }

    private static void startConsoleThread(CommandManager cm) {
        Thread t = new Thread(() -> {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            try {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.trim().isEmpty()) cm.execute(cm.getConsoleSender(), line);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
        t.setDaemon(true);
        t.start();
    }
}
