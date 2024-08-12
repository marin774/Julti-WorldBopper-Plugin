package me.marin.worldbopperplugin;

import com.google.common.io.Resources;
import me.marin.worldbopperplugin.gui.ConfigGUI;
import me.marin.worldbopperplugin.io.InstanceManagerRunnable;
import me.marin.worldbopperplugin.io.WorldBopperSettings;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static me.marin.worldbopperplugin.util.VersionUtil.CURRENT_VERSION;

/**
 * Most code for this was reused from my Julti stats plugin
 */
public class WorldBopperPlugin implements PluginInitializer {

    public static final Path WORLD_BOPPER_FOLDER_PATH = JultiOptions.getJultiDir().resolve("worldbopper-plugin");
    public static final Path SETTINGS_PATH = WORLD_BOPPER_FOLDER_PATH.resolve("settings.json");

    public static ConfigGUI configGUI;

    @Override
    public void initialize() {
        PluginEvents.RunnableEventType.LAUNCH.register(() -> {
            Julti.log(Level.INFO, "Running WorldBopper Plugin v" + CURRENT_VERSION + "!");

            WORLD_BOPPER_FOLDER_PATH.toFile().mkdirs();
            WorldBopperSettings.load();

            WorldBopperUtil.runTimerAsync(new InstanceManagerRunnable(), 1000);
        });
    }

    public static void main(String[] args) throws IOException {
        JultiAppLaunch.launchWithDevPlugin(args, PluginManager.JultiPluginData.fromString(
                Resources.toString(Resources.getResource(WorldBopperPlugin.class, "/julti.plugin.json"), Charset.defaultCharset())
        ), new WorldBopperPlugin());
    }

    @Override
    public void onMenuButtonPress() {
        if (configGUI == null || configGUI.isClosed()) {
            configGUI = new ConfigGUI();
        } else {
            configGUI.requestFocus();
        }
    }

    @Override
    public String getMenuButtonName() {
        return "Open Config";
    }
}
