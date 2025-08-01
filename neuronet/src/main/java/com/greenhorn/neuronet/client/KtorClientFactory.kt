package com.greenhorn.neuronet.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory class for creating and configuring Ktor HTTP clients.
 * 
 * This factory provides a centralized way to create HTTP clients with consistent configuration
 * including content negotiation, logging, and JSON serialization settings. The configuration
 * is optimized for analytics API calls with proper error handling and debugging capabilities.
 * 
 * @author EloAnalytics SDK Team
 * @since 1.0.0
 */
object KtorClientFactory {
    
    /**
     * Creates a configured Ktor HTTP client with all necessary plugins and settings.
     * 
     * The client is configured with:
     * - Content negotiation for JSON serialization/deserialization
     * - Logging for debugging and monitoring
     * - Proper JSON configuration with lenient parsing
     * 
     * @return Configured HttpClient instance ready for API calls
     */
    fun createHttpClient(): HttpClient {
        return HttpClient {
            // Install content negotiation plugin for JSON handling
            install(ContentNegotiation) {
                json(Json {
                    // Allow unknown keys in JSON responses for flexibility
                    ignoreUnknownKeys = true
                    // Use lenient parsing to handle malformed JSON gracefully
                    isLenient = true
                })
            }
            
            // Install logging plugin for debugging and monitoring
            install(Logging) {
                // Use default logger implementation
                logger = Logger.DEFAULT
                // Log all levels for comprehensive debugging
                level = LogLevel.ALL
            }
        }
    }
} 