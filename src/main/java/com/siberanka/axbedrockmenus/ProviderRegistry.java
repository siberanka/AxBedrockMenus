package com.siberanka.axbedrockmenus;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProviderRegistry {
    private final Map<String, MenuProvider> providers = new HashMap<>();
    private final MenuProvider fallback = new DefaultMenuProvider();

    public ProviderRegistry(LanguageService language) {
        providers.put("axrankmenu", new AxRankMenuProvider(language));
    }

    public MenuProvider providerFor(Integration integration) {
        return providers.getOrDefault(integration.id().toLowerCase(Locale.ROOT), fallback);
    }
}
