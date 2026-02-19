package dev.n1xend.dynamiceconomy.economy;

import dev.n1xend.dynamiceconomy.TestBase;
import dev.n1xend.dynamiceconomy.config.ConfigManager;
import dev.n1xend.dynamiceconomy.data.models.MarketItem;
import dev.n1xend.dynamiceconomy.services.PriceCalculator;
import org.bukkit.Material;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PriceCalculator}.
 *
 * @author n1xend
 */
@DisplayName("PriceCalculator Tests")
class PriceCalculatorTest extends TestBase {

    @Mock
    private ConfigManager mockConfigManager;

    private PriceCalculator priceCalculator;
    private MarketItem testItem;

    @BeforeEach
    void setUp() {
        when(mockConfigManager.getMinPriceMultiplier()).thenReturn(0.2);
        when(mockConfigManager.getMaxPriceMultiplier()).thenReturn(1.0);
        when(mockConfigManager.getPriceDropPerStack()).thenReturn(0.02);
        when(mockConfigManager.getPriceRecoveryPerHour()).thenReturn(0.05);
        when(mockConfigManager.getSellTax()).thenReturn(0.05);

        priceCalculator = new PriceCalculator(mockConfigManager);
        testItem = new MarketItem("WHEAT", "farming", "§eПшеница", Material.WHEAT, 10.0);
    }

    // -------------------------------------------------------------------------
    // applySale tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("applySale Tests")
    class ApplySaleTests {

        @Test
        @DisplayName("Should drop multiplier by drop-per-stack for 64 units")
        void shouldDropMultiplierFor64Units() {
            // Arrange
            double expectedMultiplier = 1.0 - 0.02; // 1 stack = 1 drop

            // Act
            priceCalculator.applySale(testItem, 64);

            // Assert
            assertEquals(expectedMultiplier, testItem.getCurrentMultiplier(), 0.001);
        }

        @Test
        @DisplayName("Should clamp multiplier at minimum when selling large amounts")
        void shouldClampMultiplierAtMinimum() {
            // Arrange — set item near minimum
            testItem.setCurrentMultiplier(0.25);

            // Act — sell enough to push below min
            priceCalculator.applySale(testItem, 640); // 10 stacks = -0.2

            // Assert
            assertEquals(0.2, testItem.getCurrentMultiplier(), 0.001,
                "Multiplier should not go below minimum");
        }

        @Test
        @DisplayName("Should record sell timestamp when sale is applied")
        void shouldRecordSellTimestamp() {
            // Arrange
            long before = System.currentTimeMillis();

            // Act
            priceCalculator.applySale(testItem, 64);

            // Assert
            assertTrue(testItem.getLastSellTimestamp() >= before,
                "Last sell timestamp should be updated");
        }

        @Test
        @DisplayName("Should increment total sold by amount")
        void shouldIncrementTotalSold() {
            // Arrange
            long initialSold = testItem.getTotalSold();

            // Act
            priceCalculator.applySale(testItem, 100);

            // Assert
            assertEquals(initialSold + 100, testItem.getTotalSold());
        }

        @ParameterizedTest
        @DisplayName("Should proportionally drop for different amounts")
        @ValueSource(ints = {32, 64, 128, 256})
        void shouldProportionallyDropForDifferentAmounts(int amount) {
            // Arrange
            double expectedDrop = 0.02 * (amount / 64.0);
            double expectedMult = Math.max(0.2, 1.0 - expectedDrop);

            // Act
            priceCalculator.applySale(testItem, amount);

            // Assert
            assertEquals(expectedMult, testItem.getCurrentMultiplier(), 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // applyRecovery tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("applyRecovery Tests")
    class ApplyRecoveryTests {

        @Test
        @DisplayName("Should recover price over time")
        void shouldRecoverPriceOverTime() {
            // Arrange
            testItem.setCurrentMultiplier(0.5);

            // Act
            priceCalculator.applyRecovery(testItem, 1.0); // 1 hour

            // Assert
            assertEquals(0.55, testItem.getCurrentMultiplier(), 0.001);
        }

        @Test
        @DisplayName("Should not recover above 1.0")
        void shouldNotRecoverAbove1() {
            // Arrange
            testItem.setCurrentMultiplier(0.98);

            // Act
            priceCalculator.applyRecovery(testItem, 1.0); // would push to 1.03

            // Assert
            assertEquals(1.0, testItem.getCurrentMultiplier(), 0.001,
                "Multiplier should not exceed 1.0 during recovery");
        }

        @Test
        @DisplayName("Should skip recovery when multiplier is already at base")
        void shouldSkipRecoveryAtBase() {
            // Arrange — item at full price
            testItem.setCurrentMultiplier(1.0);

            // Act
            priceCalculator.applyRecovery(testItem, 1.0);

            // Assert — no change
            assertEquals(1.0, testItem.getCurrentMultiplier());
        }
    }

    // -------------------------------------------------------------------------
    // calculatePayout tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("calculatePayout Tests")
    class CalculatePayoutTests {

        @Test
        @DisplayName("Should deduct sell tax from payout")
        void shouldDeductTaxFromPayout() {
            // Arrange
            // item price = 10.0 * 1.0 = 10.0
            // gross = 10.0 * 1 = 10.0
            // net = 10.0 * (1 - 0.05) = 9.5

            // Act
            double payout = priceCalculator.calculatePayout(testItem, 1, 1.0, 1.0, 1.0);

            // Assert
            assertEquals(9.5, payout, 0.001);
        }

        @Test
        @DisplayName("Should apply seasonal multiplier to payout")
        void shouldApplySeasonalMultiplier() {
            // Arrange — 1.5x seasonal bonus
            // gross = 10.0 * 1.5 * 1 = 15.0
            // net = 15.0 * 0.95 = 14.25

            // Act
            double payout = priceCalculator.calculatePayout(testItem, 1, 1.5, 1.0, 1.0);

            // Assert
            assertEquals(14.25, payout, 0.001);
        }

        @Test
        @DisplayName("Should stack all multipliers correctly")
        void shouldStackAllMultipliers() {
            // Arrange
            // price = 10.0, seasonal = 1.5, diversity = 1.1, contract = 1.4
            // gross = 10.0 * 1.5 * 1.1 * 1.4 * 1 = 23.1
            // net = 23.1 * 0.95 = 21.945

            // Act
            double payout = priceCalculator.calculatePayout(testItem, 1, 1.5, 1.1, 1.4);

            // Assert
            assertEquals(21.945, payout, 0.001);
        }

        @Test
        @DisplayName("Should never return payout less than 0.01")
        void shouldNeverReturnPayoutBelowMinimum() {
            // Arrange — push multiplier to minimum
            testItem.setCurrentMultiplier(0.0001);

            // Act
            double payout = priceCalculator.calculatePayout(testItem, 1, 1.0, 1.0, 1.0);

            // Assert
            assertTrue(payout >= 0.01, "Payout should always be at least 0.01");
        }
    }

    // -------------------------------------------------------------------------
    // previewMultiplierAfterSale tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should predict multiplier after sale without mutating item")
    void shouldPredictMultiplierWithoutMutation() {
        // Arrange
        double originalMult = testItem.getCurrentMultiplier();

        // Act
        double preview = priceCalculator.previewMultiplierAfterSale(testItem, 64);

        // Assert
        assertEquals(originalMult, testItem.getCurrentMultiplier(),
            "Item multiplier should not change during preview");
        assertEquals(originalMult - 0.02, preview, 0.001,
            "Preview should show expected drop");
    }
}
