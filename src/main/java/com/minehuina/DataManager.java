package com.minehuina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class DataManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FOLDER = "data/";

    static {
        File dir = new File(DATA_FOLDER);
        if (!dir.exists()) dir.mkdirs();
    }

    public static Set<String> loadList(String fileName) {
        File file = new File(DATA_FOLDER + fileName);
        if (!file.exists()) return new HashSet<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<HashSet<String>>(){}.getType();
            Set<String> data = gson.fromJson(reader, type);
            return data != null ? data : new HashSet<>();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки " + fileName + ": " + e.getMessage());
            return new HashSet<>();
        }
    }

    public static void saveList(String fileName, Set<String> data) {
        File file = new File(DATA_FOLDER + fileName);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения " + fileName + ": " + e.getMessage());
        }
    }

    public static Set<String> loadBans() { return loadList("bans.json"); }
    public static void saveBans(Set<String> bans) { saveList("bans.json", bans); }
    public static Set<String> loadMutes() { return loadList("mutes.json"); }
    public static void saveMutes(Set<String> mutes) { saveList("mutes.json", mutes); }
    public static Set<String> loadWhitelist() { return loadList("whitelist.json"); }
    public static void saveWhitelist(Set<String> whitelist) { saveList("whitelist.json", whitelist); }
    public static Set<String> loadOps() { return loadList("ops.json"); }
    public static void saveOps(Set<String> ops) { saveList("ops.json", ops); }
}
