package com.envyful.api.config.yaml;

import com.envyful.api.config.data.ConfigPath;
import com.envyful.api.config.data.Serializers;
import com.envyful.api.config.yaml.data.YamlConfigStyle;
import com.envyful.api.text.Placeholder;
import com.envyful.api.text.PlaceholderFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;
import org.spongepowered.configurate.reference.WatchServiceListener;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * Static factory used for loading the YAML configs from their classes.
 *
 */
public class YamlConfigFactory {

    private static final WatchServiceListener WATCH_SERVICE;

    static {
        try {
            WATCH_SERVICE = WatchServiceListener.builder().build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * Attempts to save the instance of the Config at the specified location
     * <br>
     * This method should not be used for saving standard configs loaded using either of
     * {@link YamlConfigFactory#getInstances(Class, String, DefaultConfig[])} or
     * {@link YamlConfigFactory#getInstance(Class, Placeholder...)} and
     * should only be used for saving new configs that are created
     * via code.
     *
     * @param config The config instance
     * @param path The path to save the file to
     * @return The file created
     * @param <T> The type being saved
     * @throws IOException Occurs if an error happens when creating the file
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractYamlConfig> File save(Path path, T config) throws IOException {
        File file = path.toFile();

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }

        Class<T> configClass = (Class<T>) config.getClass();
        NodeStyle style = getNodeStyle(configClass);
        List<Class<? extends ScalarSerializer<?>>> serializers =
                Lists.newArrayList();
        Serializers serializedData = configClass.getAnnotation(Serializers.class);

        if (serializedData != null) {
            serializers.addAll(Arrays.asList(serializedData.value()));
        }

        ConfigurationReference<CommentedConfigurationNode> base =
                listenToConfig(WATCH_SERVICE, file.toPath(), serializers, style);
        ValueReference<T, CommentedConfigurationNode> reference =
                base.referenceTo(configClass);

        config.base = base;
        config.config = reference;
        config.save();

        return file;
    }


    /**
     *
     * Loads all files as an instance of the class provided from the directory given
     * <br>
     * Note:
     * if a single file in there errors then an exception will be thrown preventing
     * any of the list from being lodaed
     *
     * @param configClass The class to load the files as
     * @param configDirectory The directory to load the files from
     * @return The files loaded
     * @param <T> The class type
     * @throws IOException Thrown if there is an error loading any of the configs
     */
    @SafeVarargs
    public static <T extends AbstractYamlConfig> List<T>
    getInstances(Class<T> configClass, String configDirectory,
                 DefaultConfig<T>... defaults) throws IOException {
        if (WATCH_SERVICE == null) {
            throw new IOException("Failed to get watch service for configs");
        }

        File configFiles = Paths.get(configDirectory).toFile();

        if (!configFiles.exists()) {
            configFiles.mkdir();
        }

        if (!configFiles.isDirectory()) {
            throw new IOException("Invalid path provided - must be a directory");
        }

        NodeStyle style = getNodeStyle(configClass);
        List<Class<? extends ScalarSerializer<?>>> serializers =
                Lists.newArrayList();
        Serializers serializedData = configClass.getAnnotation(Serializers.class);

        if (serializedData != null) {
            serializers.addAll(Arrays.asList(serializedData.value()));
        }

        for (DefaultConfig<T> defaultConfig : defaults) {
            File file = new File(configDirectory, defaultConfig.getFileName());

            if (file.exists() && !defaultConfig.shouldReplaceExisting()) {
                continue;
            }

            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }

            ConfigurationReference<CommentedConfigurationNode> base =
                    listenToConfig(WATCH_SERVICE, file.toPath(), serializers, style);
            ValueReference<T, CommentedConfigurationNode> reference =
                    base.referenceTo(configClass);
            T instance = reference.get();

            if (instance == null) {
                throw new IOException("Error config loaded as null");
            }

            defaultConfig.getInstance().base = base;
            defaultConfig.getInstance().config = reference;
            defaultConfig.getInstance().save();
        }

        return loadDirectory(configFiles, serializers, style, configClass);
    }

    private static <T extends AbstractYamlConfig> List<T>
    loadDirectory(File file, List<Class<? extends ScalarSerializer<?>>> serializers,
                  NodeStyle style, Class<T> configClass) throws IOException {
        List<T> loadedConfigs = Lists.newArrayList();

        for (File listFile : file.listFiles()) {
            if (listFile.isDirectory()) {
                loadedConfigs.addAll(loadDirectory(listFile, serializers, style, configClass));
                continue;
            }

            ConfigurationReference<CommentedConfigurationNode> base =
                    listenToConfig(WATCH_SERVICE, listFile.toPath(), serializers, style);

            if (base == null) {
                throw new IOException("Error config loaded as null");
            }

            ValueReference<T, CommentedConfigurationNode> reference =
                    base.referenceTo(configClass);
            T instance = reference.get();

            if (instance == null) {
                throw new IOException("Error config loaded as null");
            }

            instance.base = base;
            instance.config = reference;
            instance.save();
            loadedConfigs.add(instance);
        }

        return loadedConfigs;
    }

    /**
     *
     * Gets the instance of the given config from
     * the class using Sponge's Configurate
     *
     * @param clazz The class that represents a config file
     * @param placeholders The placeholders used for handling placeholders in the config path
     * @param <T> The type of the class
     * @return The config instance
     * @throws IOException If an error occurs whilst loading the file
     */
    public static <T extends AbstractYamlConfig> T
    getInstance(Class<T> clazz, Placeholder... placeholders) throws IOException {
        ConfigPath annotation = clazz.getAnnotation(ConfigPath.class);

        if (annotation == null) {
            throw new IOException(
                    "Cannot load config "
                            + clazz.getSimpleName() +
                            " as it's missing @ConfigPath annotation"
            );
        }

        NodeStyle style = getNodeStyle(clazz);

        List<String> configDir = PlaceholderFactory.handlePlaceholders(Collections.singletonList(annotation.value()), placeholders);

        if (configDir.isEmpty()) {
            throw new IOException("Config directory is empty (usually a placeholder error)");
        }

        Path configFile = Paths.get(configDir.get(0));

        if (!configFile.toFile().exists()) {
            configFile.getParent().toFile().mkdirs();
            configFile.toFile().createNewFile();
        }

        List<Class<? extends ScalarSerializer<?>>> serializers =
                Lists.newArrayList();
        Serializers serializedData = clazz.getAnnotation(Serializers.class);

        if (serializedData != null) {
            serializers.addAll(Arrays.asList(serializedData.value()));
        }

        if (WATCH_SERVICE == null) {
            throw new IOException("Failed to get watch service for configs");
        }

        ConfigurationReference<CommentedConfigurationNode> base =
                listenToConfig(WATCH_SERVICE, configFile, serializers, style);

        if (base == null) {
            throw new IOException("Error config loaded as null");
        }

        ValueReference<T, CommentedConfigurationNode> reference =
                base.referenceTo(clazz);
        T instance = reference.get();

        if (instance == null) {
            throw new IOException("Error config loaded as null");
        }

        instance.base = base;
        instance.config = reference;
        instance.save();

        return instance;
    }

    private static NodeStyle getNodeStyle(Class<?> clazz) {
        YamlConfigStyle annotation = clazz.getAnnotation(YamlConfigStyle.class);

        if (annotation == null) {
            return NodeStyle.BLOCK;
        }

        return annotation.value();
    }

    private static ConfigurationReference<CommentedConfigurationNode>
    listenToConfig(WatchServiceListener listener,
                   Path configFile,
                   List<Class<? extends ScalarSerializer<?>>> serializers,
                   NodeStyle style) throws IOException {
        try {
            return listener
                    .listenToConfiguration(file ->
                            YamlConfigurationLoader.builder()
                    .headerMode(HeaderMode.PRESERVE)
                    .nodeStyle(style)
                    .defaultOptions(ConfigurationOptions.defaults().header(
                            "© EnvyWare Ltd Software 2022"
                                    + System.lineSeparator() +
                            "For assistance visit" +
                                    " https://discord.envyware.co.uk"
                    ).serializers(builder -> {
                        try {
                            for (Class<? extends ScalarSerializer<?>>
                                    serializer : serializers) {
                                builder.register(serializer.newInstance());
                            }
                        } catch (InstantiationException
                                 | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }).nativeTypes(
                            Sets.newHashSet(
                                    String.class,
                                    Integer.class,
                                    Byte.class,
                                    Double.class,
                                    Boolean.class,
                                    Long.class,
                                    Map.class,
                                    List.class
                            )
                    ))
                    .defaultOptions(opts -> opts.shouldCopyDefaults(true))
                    .path(file).build(), configFile);
        } catch (ConfigurateException e) {
            throw new IOException(e);
        }
    }
}
