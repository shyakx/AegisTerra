package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.application.notification.channel.NotificationChannelProvider;
import com.aegisterra.platform.domain.notification.NotificationChannel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChannelResolver {

    private final Map<NotificationChannel, NotificationChannelProvider> providers;

    public ChannelResolver(List<NotificationChannelProvider> providers) {
        Map<NotificationChannel, NotificationChannelProvider> map = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannelProvider provider : providers) {
            map.put(provider.channel(), provider);
        }
        this.providers = Map.copyOf(map);
    }

    public List<NotificationChannel> resolveChannels(String eventType) {
        // Stage 6D default: IN_APP only for operator UX. Stub channels available when preferences enable them.
        return List.of(NotificationChannel.IN_APP);
    }

    public NotificationChannelProvider require(NotificationChannel channel) {
        NotificationChannelProvider provider = providers.get(channel);
        if (provider == null) {
            throw new IllegalStateException("No provider registered for channel " + channel);
        }
        return provider;
    }
}
