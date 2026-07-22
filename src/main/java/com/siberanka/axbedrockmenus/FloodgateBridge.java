package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class FloodgateBridge {
    private final JavaPlugin plugin;
    private Object api;
    private Method isFloodgatePlayer;

    public FloodgateBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        refresh();
    }

    public boolean isAvailable() {
        return api != null && Bukkit.getPluginManager().isPluginEnabled("floodgate");
    }

    public boolean isFloodgatePlayer(UUID uuid) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Object result = isFloodgatePlayer.invoke(api, uuid);
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                plugin.getLogger().warning("Floodgate player check failed: " + exception.getMessage());
            }
            return false;
        }
    }

    public boolean sendSimpleForm(UUID uuid, String title, String content, List<String> buttons, Consumer<Integer> clickHandler, Runnable closeHandler) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleFormClass.getMethod("builder").invoke(null);
            invokeBuilder(builder, "title", BedrockFormText.title(title));
            invokeBuilder(builder, "content", BedrockFormText.content(content));
            for (String button : buttons) {
                invokeBuilder(builder, "button", BedrockFormText.button(button));
            }
            attachConsumer(builder, "validResultHandler", result -> {
                Integer clicked = clickedButtonId(result);
                if (clicked != null) {
                    clickHandler.accept(clicked);
                }
            });
            attachRunnable(builder, "closedOrInvalidResultHandler", closeHandler);
            Object form = builder.getClass().getMethod("build").invoke(builder);
            Method sendForm = findSendForm(form);
            if (sendForm == null) {
                return false;
            }
            sendForm.invoke(api, uuid, form);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not send Floodgate form: " + exception.getMessage());
            return false;
        }
    }

    private void refresh() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            this.api = apiClass.getMethod("getInstance").invoke(null);
            this.isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            this.api = null;
            this.isFloodgatePlayer = null;
        }
    }

    private Method findSendForm(Object form) {
        for (Method method : api.getClass().getMethods()) {
            if (!method.getName().equals("sendForm") || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types[0].equals(UUID.class) && types[1].isAssignableFrom(form.getClass())) {
                return method;
            }
        }
        return null;
    }

    private void invokeBuilder(Object builder, String method, String value) throws ReflectiveOperationException {
        builder.getClass().getMethod(method, String.class).invoke(builder, value == null ? "" : value);
    }

    private void attachConsumer(Object builder, String method, Consumer<Object> consumer) throws ReflectiveOperationException {
        builder.getClass().getMethod(method, Consumer.class).invoke(builder, consumer);
    }

    private void attachRunnable(Object builder, String method, Runnable runnable) throws ReflectiveOperationException {
        builder.getClass().getMethod(method, Runnable.class).invoke(builder, runnable);
    }

    private Integer clickedButtonId(Object result) {
        String[] names = {"clickedButtonId", "clickedButtonIndex", "buttonId"};
        for (String name : names) {
            try {
                Object value = result.getClass().getMethod(name).invoke(result);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
