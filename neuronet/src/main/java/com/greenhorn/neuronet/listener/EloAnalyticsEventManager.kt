package com.greenhorn.neuronet.listener

/**
 * Main interface for the EloAnalytics SDK that provides event tracking and management capabilities.
 *
 * This interface defines the public API for the analytics SDK, providing methods for:
 * - Tracking custom events with attributes
 * - Managing session timestamps
 * - Updating API headers
 * - Flushing events with various triggers
 *
 * ## Features
 * - **Event Tracking**: Track custom events with flexible attributes
 * - **Session Management**: Update session timestamps for event grouping
 * - **Header Management**: Set custom headers for API requests
 * - **Flexible Flushing**: Multiple ways to flush pending events
 *
 * ## Usage Example
 * ```kotlin
 * // Get SDK instance
 * val analytics = EloAnalyticsSdk.getInstance()
 *
 * // Track events
 * analytics.trackEvent("user_login", mapOf(
 *     "user_id" to "12345",
 *     "login_method" to "email"
 * ))
 *
 * // Update session
 * analytics.updateSessionTimeStamp("session_${System.currentTimeMillis()}")
 *
 * // Set headers
 * analytics.updateHeader(mapOf(
 *     "Authorization" to "Bearer token",
 *     "X-Client-Version" to "1.0.0"
 * ))
 *
 * // Flush events
 * analytics.flushEventsOnBackgroundOrKilled()
 * analytics.flushEventsOnDestroy()
 * analytics.flushEvents("custom_scenario")
 * ```
 *
 * ## Event Tracking
 * Events can be tracked with any custom attributes. The SDK automatically adds:
 * - Timestamp
 * - Session information
 * - User context (login status, user ID)
 * - AppsFlyer ID (if configured)
 *
 * ## Flush Triggers
 * Events can be flushed using:
 * - **Specific Methods**: For common scenarios (background, destroy)
 * - **Flexible Method**: For custom scenarios with any trigger name
 *
 * @author EloAnalytics SDK Team
 * @since 1.0.0
 */
interface EloAnalyticsEventManager {
    
    /**
     * Tracks an analytics event with the provided name and data.
     *
     * This method handles the complete event tracking flow, including validation,
     * metadata addition, local storage, and batch processing.
     *
     * ## Event Structure
     * Events are stored with the following structure:
     * - **Event Name**: Identifier for the event type
     * - **Timestamp**: When the event occurred
     * - **Attributes**: Custom data associated with the event
     * - **User Context**: Login status and user ID
     * - **Session Info**: Session timestamp for grouping
     *
     * ## Usage Examples
     * ```kotlin
     * // Simple event
     * analytics.trackEvent("app_opened", emptyMap())
     *
     * // Event with attributes
     * analytics.trackEvent("user_login", mapOf(
     *     "user_id" to "12345",
     *     "login_method" to "email",
     *     "platform" to "android"
     * ))
     *
     * // Complex event with nested data
     * analytics.trackEvent("purchase_completed", mapOf(
     *     "order_id" to "ORD-12345",
     *     "amount" to 99.99,
     *     "currency" to "USD",
     *     "items" to listOf("product1", "product2"),
     *     "payment_method" to "credit_card"
     * ))
     * ```
     *
     * ## Batch Processing
     * Events are automatically batched and sent to the server when:
     * - The configured batch size is reached
     * - Manual flush is triggered
     * - App goes to background
     * - Activity is destroyed
     *
     * @param name The event name/identifier (e.g., "user_login", "purchase_completed")
     * @param attributes Map containing event data and properties. Can be empty for simple events.
     *
     * @throws IllegalArgumentException if event name is empty
     * @throws IllegalStateException if SDK is not properly initialized
     */
    fun trackEvent(name: String, attributes: Map<String, Any>)

    /**
     * Updates the session timestamp used for grouping related events.
     *
     * This method allows clients to set a custom session identifier that will be
     * associated with all events tracked during that session. This is useful for
     * grouping events that belong to the same user session.
     *
     * ## Usage Examples
     * ```kotlin
     * // Set session timestamp when user logs in
     * analytics.updateSessionTimeStamp("session_${System.currentTimeMillis()}")
     *
     * // Or use a custom session ID
     * analytics.updateSessionTimeStamp("user_123_session_456")
     *
     * // Session with user context
     * analytics.updateSessionTimeStamp("user_${userId}_session_${sessionId}")
     * ```
     *
     * ## Session Management
     * - Sessions help group related events together
     * - Useful for analyzing user behavior patterns
     * - Can be updated multiple times during app lifecycle
     * - Events are tagged with the current session timestamp
     *
     * @param timeStamp The session timestamp to associate with events
     */
    fun updateSessionTimeStamp(timeStamp: String)

    /**
     * Updates the HTTP headers used for API requests.
     *
     * This method allows clients to set custom headers that will be included
     * in all API requests to the analytics server. Common use cases include
     * authentication tokens, API keys, or custom identifiers.
     *
     * ## Usage Examples
     * ```kotlin
     * // Set authentication header
     * analytics.updateHeader(mapOf(
     *     "Authorization" to "Bearer your_token_here",
     *     "X-API-Key" to "your_api_key"
     * ))
     *
     * // Set custom headers for tracking
     * analytics.updateHeader(mapOf(
     *     "X-Client-Version" to "1.0.0",
     *     "X-Platform" to "android",
     *     "X-User-ID" to "user_123"
     * ))
     *
     * // Update headers dynamically
     * analytics.updateHeader(mapOf(
     *     "X-Session-ID" to sessionId,
     *     "X-Device-ID" to deviceId
     * ))
     * ```
     *
     * ## Header Management
     * - Headers are applied to all API requests
     * - Can be updated multiple times during app lifecycle
     * - Useful for authentication and tracking purposes
     * - Headers are included in background sync operations
     *
     * @param header Map of header name-value pairs to include in API requests
     */
    fun updateHeader(header: Map<String, String>)
    
    /**
     * Flushes pending events when app goes to background or is killed.
     *
     * This method should be called by the client when the app lifecycle changes
     * to background state. It ensures that pending events are sent to the server
     * before the app is potentially killed by the system.
     *
     * ## Usage Examples
     * ```kotlin
     * // In your Application class or Activity
     * override fun onPause() {
     *     super.onPause()
     *     analytics.flushEventsOnBackgroundOrKilled()
     * }
     *
     * // Or in Application.ActivityLifecycleCallbacks
     * override fun onActivityPaused(activity: Activity) {
     *     analytics.flushEventsOnBackgroundOrKilled()
     * }
     *
     * // In Fragment
     * override fun onPause() {
     *     super.onPause()
     *     analytics.flushEventsOnBackgroundOrKilled()
     * }
     * ```
     *
     * ## Trigger Source
     * This method uses the trigger source "app_background" for logging and
     * analytics purposes.
     *
     * ## When to Call
     * - When app goes to background
     * - When activity is paused
     * - Before app might be killed by system
     * - When user switches to another app
     */
    fun flushEventsOnBackgroundOrKilled()
    
    /**
     * Flushes pending events when activity/view is destroyed.
     *
     * This method should be called by the client when the activity/view lifecycle
     * ends. It ensures that any pending events are sent to the server before
     * the activity is destroyed.
     *
     * ## Usage Examples
     * ```kotlin
     * // In your Activity
     * override fun onDestroy() {
     *     super.onDestroy()
     *     analytics.flushEventsOnDestroy()
     * }
     *
     * // Or in Fragment
     * override fun onDestroyView() {
     *     super.onDestroyView()
     *     analytics.flushEventsOnDestroy()
     * }
     *
     * // In custom View
     * override fun onDetachedFromWindow() {
     *     super.onDetachedFromWindow()
     *     analytics.flushEventsOnDestroy()
     * }
     * ```
     *
     * ## Trigger Source
     * This method uses the trigger source "activity_destroy" for logging and
     * analytics purposes.
     *
     * ## When to Call
     * - When activity is destroyed
     * - When fragment is destroyed
     * - When view is detached from window
     * - Before component lifecycle ends
     */
    fun flushEventsOnDestroy()
    
    /**
     * Flexible method to flush events with a custom trigger name.
     *
     * This method provides complete flexibility for custom flush scenarios.
     * Clients can use any trigger name they want for their specific use cases.
     *
     * ## Usage Examples
     * ```kotlin
     * // User logout scenario
     * analytics.flushEvents("user_logout")
     *
     * // Network available scenario
     * analytics.flushEvents("network_available")
     *
     * // Manual flush
     * analytics.flushEvents("manual_flush")
     *
     * // Custom business logic
     * analytics.flushEvents("payment_completed")
     * analytics.flushEvents("session_timeout")
     * analytics.flushEvents("error_occurred")
     *
     * // App update scenario
     * analytics.flushEvents("app_update")
     *
     * // Battery low scenario
     * analytics.flushEvents("battery_low")
     *
     * // Memory pressure
     * analytics.flushEvents("memory_pressure")
     *
     * // Custom business scenarios
     * analytics.flushEvents("checkout_completed")
     * analytics.flushEvents("level_completed")
     * analytics.flushEvents("achievement_unlocked")
     * ```
     *
     * ## Benefits
     * - **Complete Flexibility**: Use any trigger name you want
     * - **No Restrictions**: No enum limitations
     * - **Future-Proof**: No SDK changes needed for new triggers
     * - **Better Logging**: Descriptive trigger names for debugging
     * - **Custom Scenarios**: Support for any business logic
     *
     * ## Common Trigger Names
     * - `user_logout`: When user logs out
     * - `network_available`: When network becomes available
     * - `manual_flush`: User-initiated flush
     * - `payment_completed`: After payment processing
     * - `session_timeout`: When session expires
     * - `error_occurred`: When errors happen
     * - `app_update`: When app is updated
     * - `battery_low`: When battery is low
     * - `memory_pressure`: When memory is low
     *
     * @param triggerName Custom name for the trigger (e.g., "user_logout", "network_available", "manual_flush")
     */
    fun flushEvents(triggerName: String)
}