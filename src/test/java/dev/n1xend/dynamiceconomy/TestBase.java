package dev.n1xend.dynamiceconomy;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Base class for unit tests providing common mocks.
 *
 * @author n1xend
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class TestBase {

    @Mock
    protected Server mockServer;

    @Mock
    protected PluginManager mockPluginManager;

    @BeforeEach
    void setUpBase() {
        MockitoAnnotations.openMocks(this);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
    }

    /**
     * Creates a mock online player with given name and UUID.
     *
     * @param name player name
     * @param uuid player UUID
     * @return configured mock player
     */
    protected Player createMockPlayer(String name, UUID uuid) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.getServer()).thenReturn(mockServer);
        return player;
    }

    /**
     * Creates a mock player with a random UUID.
     *
     * @param name player name
     * @return configured mock player
     */
    protected Player createMockPlayer(String name) {
        return createMockPlayer(name, UUID.randomUUID());
    }
}
