package atom.hitmarker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("hitmarker.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean playSound = true;
    public static int crosshairStyle = 1;

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                playSound = data.playSound;
                if (data.crosshairStyle >= 1 && data.crosshairStyle <= 4) {
                    crosshairStyle = data.crosshairStyle;
                }
            }
        } catch (Exception e) {
            HitMarker.LOGGER.error("Failed to load hitmarker configuration", e);
        }
    }

    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                ConfigData data = new ConfigData();
                data.playSound = playSound;
                data.crosshairStyle = crosshairStyle;
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            HitMarker.LOGGER.error("Failed to save hitmarker configuration", e);
        }
    }

    private static class ConfigData {
        boolean playSound = true;
        int crosshairStyle = 1;
    }
}
