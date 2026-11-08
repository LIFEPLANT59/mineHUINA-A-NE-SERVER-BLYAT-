package com.minehuina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;

public class Main {
    public static void main(String[] args) {
        System.out.println("Запускаем наше кастомное ядро...");

        // 1. Инициализация сервера
        MinecraftServer server = MinecraftServer.init();

        // 2. Создаем мир (инстанс)
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instance = instanceManager.createInstanceContainer();

        // Генерируем простейший мир (плоский слой камня)
        instance.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 40, Block.STONE);
        });

        // 3. Обработчик событий (когда игрок заходит)
        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            // Спавним игрока на высоте 42 (над камнем)
            event.getPlayer().setRespawnPoint(new net.minestom.server.coordinate.Pos(0, 42, 0));
        });

        // 4. Запускаем сервер на порту 25565
        server.start("0.0.0.0", 25565);
        
        System.out.println("Сервер запущен! Подключайся.");
    }
}
