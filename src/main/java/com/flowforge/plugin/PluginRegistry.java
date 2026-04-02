package com.flowforge.plugin;
import java.util.logging.Logger;
import java.util.*;

public class PluginRegistry {
    private static final Logger LOGGER = Logger.getLogger(PluginRegistry.class.getName());
    private final Map<String, Plugin> plugins = new LinkedHashMap<>();
    private final Map<String, PluginState> states = new HashMap<>();
    private final PluginContext context;

    public enum PluginState {
        REGISTERED,
        INITIALIZED,
        STARTED,
        STOPPED,
        FAILED
    }

    public PluginRegistry(PluginContext context) {
        this.context = context;
    }

    /**
     * Registers a plugin. Does not initialize or start it yet.
     *
     * @param plugin the plugin to register
     * @throws IllegalArgumentException if a plugin with the same ID exists
     */
    public void register(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (plugins.containsKey(plugin.getId())) {
            throw new IllegalArgumentException(
                    "Plugin already registered: " + plugin.getId()
                            + " (existing: " + plugins.get(plugin.getId()).getName() + ")"
            );
        }
        plugins.put(plugin.getId(), plugin);
        states.put(plugin.getId(), PluginState.REGISTERED);
        LOGGER.info(() -> "[PluginRegistry] Registered: " + plugin.getName()
                + " v" + plugin.getVersion() + " (" + plugin.getId() + ")");
    }

    /**
     * Initializes all registered plugins by calling plugin.initialize(context).
     * Plugins that fail initialization are marked FAILED but don't block others.
     */
    public void initializeAll() {
        for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
            String id = entry.getKey();
            Plugin plugin = entry.getValue();

            if (states.get(id) != PluginState.REGISTERED) {
                continue;
            }

            try {
                LOGGER.info(() -> "[PluginRegistry] Initializing: " + plugin.getName());
                plugin.initialize(context);
                states.put(id, PluginState.INITIALIZED);
                LOGGER.info(() -> "[PluginRegistry] Initialized: " + plugin.getName());
            } catch (Exception e) {
                states.put(id, PluginState.FAILED);
                LOGGER.warning(() -> "[PluginRegistry] FAILED to initialize " + plugin.getName()
                        + ": " + e.getMessage());
            }
        }
    }

    /**
     * Starts all initialized plugins.
     */
    public void startAll() {
        for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
            String id = entry.getKey();
            Plugin plugin = entry.getValue();

            if (states.get(id) != PluginState.INITIALIZED) {
                continue;
            }

            try {
                plugin.start();
                states.put(id, PluginState.STARTED);
                LOGGER.info(() -> "[PluginRegistry] Started: " + plugin.getName());
            } catch (Exception e) {
                states.put(id, PluginState.FAILED);
                LOGGER.warning(() -> "[PluginRegistry] FAILED to start " + plugin.getName()
                        + ": " + e.getMessage());
            }
        }
    }

    /**
     * Stops all started plugins (in reverse order for clean shutdown).
     */
    public void stopAll() {
        List<String> ids = new ArrayList<>(plugins.keySet());
        Collections.reverse(ids);

        for (String id : ids) {
            Plugin plugin = plugins.get(id);
            if (states.get(id) == PluginState.STARTED) {
                try {
                    plugin.stop();
                    states.put(id, PluginState.STOPPED);
                    LOGGER.info(() -> "[PluginRegistry] Stopped: " + plugin.getName());
                } catch (Exception e) {
                    LOGGER.warning(() -> "[PluginRegistry] Error stopping " + plugin.getName()
                            + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Lists the plugin names that are registered.
     */
    public List<String> listPlugins() {
        List<String> names = new ArrayList<>();
        for (Plugin plugin : plugins.values()) {
            names.add(plugin.getName());
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Returns the state of a plugin.
     */
    public PluginState getState(String pluginId) {
        return states.get(pluginId);
    }

    /**
     * Lists all registered plugins with their states.
     */
    public void printStatus() {
        LOGGER.info("=== Plugin Registry Status ===");
        for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
            Plugin p = entry.getValue();
            PluginState state = states.get(entry.getKey());
            LOGGER.info(() -> "  " + p.getName() + " v" + p.getVersion()
                    + " [" + p.getId() + "] — " + state);
        }
    }

    public int getPluginCount() {
        return plugins.size();
    }

    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
}
