package dev.n1xend.dynamiceconomy.economy;

import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import org.bukkit.Material;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MarketItem}.
 *
 * @author n1xend
 */
@DisplayName("MarketItem Tests")
class MarketItemTest {

    private MarketItem item;

    @BeforeEach
    void setUp() {
        item = new MarketItem("WHEAT", "farming", "§eПшеница", Material.WHEAT, 10.0);
    }

    @Test
    @DisplayName("Should initialize with multiplier of 1.0")
    void shouldInitializeWithDefaultMultiplier() {
        assertEquals(1.0, item.getCurrentMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Should calculate current price correctly")
    void shouldCalculateCurrentPrice() {
        // Arrange
        item.setCurrentMultiplier(0.5);

        // Act
        double price = item.getCurrentPrice();

        // Assert
        assertEquals(5.0, price, 0.001, "currentPrice = basePrice * multiplier");
    }

    @Test
    @DisplayName("Should return correct trend for suppressed price")
    void shouldReturnNegativeTrendForSuppressedPrice() {
        // Arrange
        item.setCurrentMultiplier(0.7);

        // Act
        double trend = item.getTrend();

        // Assert
        assertEquals(-0.3, trend, 0.001, "Trend should be negative when below base");
    }

    @Test
    @DisplayName("Should record sell and update total sold")
    void shouldRecordSellAndUpdateTotalSold() {
        // Arrange
        long before = System.currentTimeMillis();

        // Act
        item.recordSell(100);

        // Assert
        assertEquals(100, item.getTotalSold());
        assertTrue(item.getLastSellTimestamp() >= before);
    }

    @Test
    @DisplayName("Should accumulate total sold across multiple sells")
    void shouldAccumulateTotalSold() {
        // Act
        item.recordSell(50);
        item.recordSell(75);

        // Assert
        assertEquals(125, item.getTotalSold());
    }

    @Nested
    @DisplayName("Boundary Tests")
    class BoundaryTests {

        @Test
        @DisplayName("Should handle zero base price")
        void shouldHandleZeroBasePrice() {
            // Arrange
            MarketItem zeroItem = new MarketItem("TEST", "cat", "Test", Material.AIR, 0.0);

            // Assert
            assertEquals(0.0, zeroItem.getCurrentPrice());
        }

        @Test
        @DisplayName("Should handle multiplier of exactly minimum (0.2)")
        void shouldHandleMinimumMultiplier() {
            // Arrange
            item.setCurrentMultiplier(0.2);

            // Assert
            assertEquals(2.0, item.getCurrentPrice(), 0.001);
            assertEquals(-0.8, item.getTrend(), 0.001);
        }
    }
}
