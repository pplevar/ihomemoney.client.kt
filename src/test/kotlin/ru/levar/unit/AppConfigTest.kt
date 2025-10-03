package ru.levar.unit

import ru.levar.AppConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for AppConfig
 * Tests configuration loading from properties file
 */
class AppConfigTest {

    @Test
    fun `should load serviceUri from test config`() {
        // Act
        val serviceUri = AppConfig.serviceUri

        // Assert
        assertThat(serviceUri).isNotEmpty()
        assertThat(serviceUri).startsWith("http")

        // CRITICAL SAFETY CHECK: Ensure test config does not point to production
        assertThat(serviceUri)
            .withFailMessage("TEST CONFIGURATION MUST NOT USE PRODUCTION ENDPOINTS! Found: $serviceUri")
            .doesNotContain("ihomemoney.com")
    }

    @Test
    fun `test config should point to localhost`() {
        // Act
        val serviceUri = AppConfig.serviceUri

        // Assert - Test environment should use localhost
        assertThat(serviceUri).contains("localhost")
    }

    @Test
    fun `serviceUri should have valid format`() {
        // Act
        val serviceUri = AppConfig.serviceUri

        // Assert
        assertThat(serviceUri).matches("^https?://.*")
        assertThat(serviceUri).endsWith("/")
    }
}
