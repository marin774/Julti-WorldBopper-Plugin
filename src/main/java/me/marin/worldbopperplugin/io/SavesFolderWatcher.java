package me.marin.worldbopperplugin.io;

import me.marin.worldbopperplugin.util.FileStillEmptyException;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class SavesFolderWatcher extends FileWatcher {

    private final Set<String> netherWorldsToKeep = ConcurrentHashMap.newKeySet();

    public SavesFolderWatcher(Path path) {
        super("saves-folder-watcher", path.toFile());
        Julti.log(Level.DEBUG, "Saves folder watcher is running...");
    }

    @Override
    protected void handleFileUpdated(File file) {
        // ignored
    }

    @Override
    protected void handleFileCreated(File file) {
        if (!WorldBopperSettings.getInstance().worldbopperEnabled) {
            return;
        }
        if (!file.isDirectory()) return;

        String dirName = file.getName();
        if (!isValidDirectoryName(dirName)) return;

        long worldsToKeep = WorldBopperSettings.getInstance().savesBuffer;
        if (WorldBopperSettings.getInstance().keepNetherWorlds) {
            worldsToKeep += netherWorldsToKeep.size();
        }

        File[] directories = this.file.listFiles(File::isDirectory);
        if (directories == null) {
            // IO error occurred, clear next time I guess
            return;
        }
        File[] validDirectories = Arrays.stream(directories)
                .filter(d -> isValidDirectoryName(d.getName()))
                .toArray(File[]::new);

        if (validDirectories.length <= worldsToKeep) {
            Julti.log(Level.DEBUG, "Not deleting any worlds (" + validDirectories.length + " <= " + worldsToKeep + ")");
            return;
        }
        // Sort valid worlds by time
        try {
            Arrays.sort(validDirectories, Comparator.comparingLong(File::lastModified));
        } catch (IllegalArgumentException ignored) {
            // rare JDK bug (https://stackoverflow.com/questions/13575224/comparison-method-violates-its-general-contract-timsort-and-gridlayout)
            // probably not fixable because Arrays class might be loaded before the plugin
            return;
        }

        for (int i = 0; i < validDirectories.length - worldsToKeep; i++) {
            File oldestDir = validDirectories[i];

            // Keep worlds with nether (rsg.enter_nether in srigt events.log)
            if (WorldBopperSettings.getInstance().keepNetherWorlds) {
                if (netherWorldsToKeep.contains(oldestDir.getName())) {
                    continue;
                }
                if (shouldKeepWorld(oldestDir)) {
                    netherWorldsToKeep.add(oldestDir.getName());
                    Julti.log(Level.DEBUG, "Not deleting " + oldestDir.getName() + " because it has nether enter!");
                    continue;
                }
            }

            // Delete
            Julti.log(Level.DEBUG, "Deleting " + oldestDir.getName());
            try (Stream<Path> stream = Files.walk(oldestDir.toPath())) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (NoSuchFileException ignored) {
                Julti.log(Level.DEBUG, "File " + oldestDir.getName() + " is missing? (NoSuchFileException)");
            } catch (AccessDeniedException e) {
                Julti.log(Level.DEBUG, "Access for file " + oldestDir.getName() + " denied? (AccessDeniedException)");
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Unknown error while bopping worlds:\n" + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private boolean isValidDirectoryName(String name) {
        return name.contains("Speedrun #") || name.startsWith("Benchmark Reset #") || name.startsWith("New World") || name.startsWith("Practice Seed") || name.startsWith("Seed Paster");
    }

    private boolean shouldKeepWorld(File file) {
        File speedrunIGTDir = new File(file, "speedrunigt");
        if (!speedrunIGTDir.exists()) {
            return false;
        }
        File eventsLog = new File(speedrunIGTDir, "events.log");
        if (!eventsLog.exists()) {
            return false;
        }
        try {
            String eventsLogText = WorldBopperUtil.readFile(eventsLog.toPath());
            for (String line : eventsLogText.split("[\\r\\n]+")) {
                if (line.startsWith("rsg.enter_nether")) {
                    return true;
                }
            }
            return false;
        } catch (FileStillEmptyException e) {
            // file is probably actually empty, this should never happen, clear this world
            return false;
        }
    }

}
