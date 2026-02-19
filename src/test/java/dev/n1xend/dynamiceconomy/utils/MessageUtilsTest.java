package dev.n1xend.dynamiceconomy.utils;

import dev.n1xend.dynamiceconomy.TestBase;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MessageUtils}.
 *
 * @author n1xend
 */
@DisplayName("MessageUtils Tests")
class MessageUtilsTest extends TestBase {

    // -------------------------------------------------------------------------
    // colorize tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should translate & color codes to § codes")
    void shouldTranslateColorCodes() {
        // Arrange
        String input = "&aHello &bWorld";
        String expected = "§aHello §bWorld";

        // Act
        String result = MessageUtils.colorize(input);

        // Assert
        assertEquals(expected, result, "Color codes should be translated");
    }

    @ParameterizedTest
    @DisplayName("Should handle all standard color codes")
    @ValueSource(strings = {"&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9",
        "&a", "&b", "&c", "&d", "&e", "&f", "&l", "&o", "&n", "&m", "&k", "&r"})
    void shouldHandleAllColorCodes(String colorCode) {
        // Arrange
        String input = colorCode + "text";

        // Act
        String result = MessageUtils.colorize(input);

        // Assert
        assertTrue(result.startsWith("§"), "Should start with section sign");
    }

    @Test
    @DisplayName("Should return unchanged string with no color codes")
    void shouldReturnUnchangedStringWithNoColorCodes() {
        // Arrange
        String input = "Hello World";

        // Act
        String result = MessageUtils.colorize(input);

        // Assert
        assertEquals(input, result);
    }

    // -------------------------------------------------------------------------
    // replacePlaceholders tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should replace single placeholder correctly")
    void shouldReplaceSinglePlaceholder() {
        // Arrange
        String template = "Hello %player%!";

        // Act
        String result = MessageUtils.replacePlaceholders(template, "%player%", "n1xend");

        // Assert
        assertEquals("Hello n1xend!", result);
    }

    @Test
    @DisplayName("Should replace multiple placeholders correctly")
    void shouldReplaceMultiplePlaceholders() {
        // Arrange
        String template = "Hello %player%, welcome to %server%!";

        // Act
        String result = MessageUtils.replacePlaceholders(template,
            "%player%", "n1xend",
            "%server%", "TestServer");

        // Assert
        assertEquals("Hello n1xend, welcome to TestServer!", result);
    }

    @Test
    @DisplayName("Should throw exception for odd replacement count")
    void shouldThrowExceptionForOddReplacementCount() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            MessageUtils.replacePlaceholders("Hello %player%", "%player%"),
            "Odd number of replacements should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should leave unmatched placeholders unchanged")
    void shouldLeaveUnmatchedPlaceholdersUnchanged() {
        // Arrange
        String template = "Hello %player% and %unknown%";

        // Act
        String result = MessageUtils.replacePlaceholders(template, "%player%", "n1xend");

        // Assert
        assertEquals("Hello n1xend and %unknown%", result);
    }

    // -------------------------------------------------------------------------
    // sendMessage tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("sendMessage Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message to online player")
        void shouldSendMessageToOnlinePlayer() {
            // Arrange
            Player player = createMockPlayer("n1xend");
            String message = "Test message";

            // Act
            MessageUtils.sendMessage(player, message);

            // Assert
            verify(player, times(1)).sendMessage(message);
        }

        @Test
        @DisplayName("Should not send message to offline player")
        void shouldNotSendMessageToOfflinePlayer() {
            // Arrange
            Player player = createMockPlayer("n1xend");
            when(player.isOnline()).thenReturn(false);

            // Act
            MessageUtils.sendMessage(player, "Test");

            // Assert
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should not throw when player is null")
        void shouldNotThrowWhenPlayerIsNull() {
            // Act & Assert
            assertDoesNotThrow(() -> MessageUtils.sendMessage(null, "Test"),
                "Should handle null player gracefully");
        }
    }
}
