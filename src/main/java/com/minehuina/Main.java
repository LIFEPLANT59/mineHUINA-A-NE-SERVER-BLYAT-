package com.minehuina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.kyori.adventure.text.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        System.out.println("Запускаем наше кастомное ядро...");

        MinecraftServer server = MinecraftServer.init();
        CommandManager commandManager = MinecraftServer.getCommandManager();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instance = instanceManager.createInstanceContainer();

        instance.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 40, Block.STONE);
            unit.modifier().fillHeight(40, 43, Block.DIRT);
            unit.modifier().fillHeight(43, 44, Block.GRASS_BLOCK);
        });

        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 45, 0));
        });

        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendMessage(Component.text("§aДобро пожаловать на сервер, " + player.getUsername() + "!"));
        });

        // --- ИСПРАВЛЕННЫЙ БЛОК СОЗДАНИЯ КОМАНДЫ STOP ---
        Command stopCommand = new Command("stop");
        stopCommand.setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("§cОстановка сервера..."));
            MinecraftServer.stopCleanly();
            System.exit(0);
        });
        commandManager.register(stopCommand);
        // ---------------------------------------------

        startConsoleThread(commandManager);

        server.start("0.0.0.0", 25565);
        System.out.println("Сервер запущен! Вводи команды в консоль (например, 'stop').");
    }

    private static void startConsoleThread(CommandManager commandManager) {
        Thread consoleThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        commandManager.execute(commandManager.getConsoleSender(), line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Console-Thread");
        consoleThread.setDaemon(true);
        consoleThread.start();
    }
}
