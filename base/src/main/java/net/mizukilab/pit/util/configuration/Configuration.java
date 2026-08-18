package net.mizukilab.pit.util.configuration;

import net.mizukilab.pit.util.configuration.annotations.ConfigData;
import net.mizukilab.pit.util.configuration.annotations.ConfigSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Configuration {

    private final YamlConfiguration config;
    private final JavaPlugin plugin;
    private final File file;
    private final File directory;
    private final Set<String> unreadablePaths = new HashSet<>();

    public Configuration(JavaPlugin plugin) {
        this(plugin, "config.yml");
    }

    public Configuration(JavaPlugin plugin, String filename) {
        this(plugin, filename, plugin.getDataFolder().getPath());
    }
    public Configuration(JavaPlugin plugin, String filename, String directory,boolean pathTo) {
        this(plugin,filename,plugin.getDataFolder().getPath() + "/" + directory);
    }
    public Configuration(JavaPlugin plugin, String filename, String directory) {
        this.plugin = plugin;
        this.directory = new File(directory);
        this.file = new File(directory, filename);
        this.config = new YamlConfiguration();
        this.createFile();
    }

    public void createFile() {
        if (!this.directory.exists()) {
            this.directory.mkdirs();
        }

        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

        try {
            this.config.load(this.file);
        } catch (InvalidConfigurationException | IOException var2) {
            var2.printStackTrace();
        }

    }

    public void save() {
        Field[] toSave = this.getClass().getDeclaredFields();
        boolean hasChanges = false;

        for (Field f : toSave) {
            if (f.isAnnotationPresent(ConfigData.class)) {
                ConfigData configData = f.getAnnotation(ConfigData.class);

                if (!this.config.contains(configData.path()) || unreadablePaths.contains(configData.path())) {
                    continue;
                }

                try {
                    f.setAccessible(true);
                    Object saveValue = f.get(this);
                    Object configValue = null;
                    if (f.isAnnotationPresent(ConfigSerializer.class)) {
                        ConfigSerializer serializer = f.getAnnotation(ConfigSerializer.class);
                        if (saveValue instanceof List) {
                            configValue = new ArrayList<>();

                            for (Object o : (List) saveValue) {
                                AbstractSerializer as = (AbstractSerializer) serializer.serializer().newInstance();
                                ((List) configValue).add(as.toString(o));
                            }
                        } else {
                            AbstractSerializer as = (AbstractSerializer) serializer.serializer().newInstance();
                            configValue = as.toString(saveValue);
                        }
                    } else if (saveValue instanceof List) {
                        configValue = new ArrayList<>();
                        Iterator var15 = ((List) saveValue).iterator();

                        while (var15.hasNext()) {
                            Object o = var15.next();
                            ((List) configValue).add(o.toString());
                        }
                    }

                    if (configValue == null) {
                        configValue = saveValue;
                    }

                    if (!Objects.equals(this.config.get(configData.path()), configValue)) {
                        this.config.set(configData.path(), configValue);
                        hasChanges = true;
                        System.out.println("Setting: " + configData.path() + " to " + saveValue);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (hasChanges) {
            try {
                this.config.save(this.file);
            } catch (IOException var13) {
                var13.printStackTrace();
            }
        }

    }

    public void load() {
        unreadablePaths.clear();
        Field[] toLoad = this.getClass().getDeclaredFields();

        for (Field f : toLoad) {
            try {
                f.setAccessible(true);
                if (!f.isAnnotationPresent(ConfigData.class)) {
                    continue;
                }

                ConfigData configData = f.getAnnotation(ConfigData.class);
                if (!this.config.contains(configData.path())) {
                    continue;
                }

                if (!f.isAnnotationPresent(ConfigSerializer.class)) {
                    try {
                        if (this.config.isList(configData.path())) {
                            f.set(this, this.config.getList(configData.path(), new ArrayList<>()));
                        } else {
                            f.set(this, this.config.get(configData.path()));
                        }
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        unreadablePaths.add(configData.path());
                        e.printStackTrace();
                    }
                } else if (this.config.isList(configData.path())) {
                    try {
                        List<String> list = this.config.getStringList(configData.path());
                        List<Object> deserializedList = new ArrayList<>();
                        boolean valid = true;

                        for (String s : list) {
                            Object deserializedValue = this.deserializeValue(f, s);
                            if (deserializedValue == null) {
                                valid = false;
                                break;
                            }
                            deserializedList.add(deserializedValue);
                        }

                        if (valid) {
                            f.set(this, deserializedList);
                        } else {
                            unreadablePaths.add(configData.path());
                        }
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                        unreadablePaths.add(configData.path());
                        System.out.println("Error reading list in configuration file: " + this.config.getName() + " path: " + configData.path());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Object object = this.config.get(configData.path());
                        Object deserializedValue = this.deserializeValue(f, object == null ? null : object.toString());
                        if (deserializedValue != null) {
                            f.set(this, deserializedValue);
                        } else {
                            unreadablePaths.add(configData.path());
                        }
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                        unreadablePaths.add(configData.path());
                        System.out.println("Error reading value in configuration file: " + this.config.getName() + " path: " + configData.path());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                if (f.isAnnotationPresent(ConfigData.class)) {
                    unreadablePaths.add(f.getAnnotation(ConfigData.class).path());
                }
                e.printStackTrace();
            }
        }

    }

    public File getFile() {
        return this.file;
    }

    public Object deserializeValue(Field f, Object value) throws IllegalAccessException, InstantiationException {
        AbstractSerializer serializer = (AbstractSerializer) f.getAnnotation(ConfigSerializer.class).serializer().newInstance();
        if (value == null) {
            return serializer.fromString(null);
        } else {
            return serializer.fromString(value.toString());
        }
    }
}
