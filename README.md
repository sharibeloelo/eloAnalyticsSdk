# EloAnalytics SDK

A modern Android analytics SDK built with Kotlin and Ktor for seamless event tracking and synchronization.

## 🚀 Features

- **Event Tracking**: Track custom analytics events with flexible data structures
- **Batch Processing**: Efficiently batch events before sending to reduce network overhead
- **Offline Support**: Store events locally when network is unavailable
- **Automatic Sync**: Background synchronization using WorkManager
- **Ktor Integration**: Modern HTTP client for better Kotlin Multiplatform (KMP) support
- **Type Safety**: Full Kotlin serialization support with compile-time safety
- **Simple Batch Processing**: Uses LIMIT only for efficient database fetching

## 📋 Requirements

- Android API 24+ (Android 7.0+)
- Kotlin 1.9+
- Ktor 2.3.12+
- Room Database 2.7.2+
- WorkManager 2.10.2+

## 🔧 Installation

Add the following dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
```

## 🏗️ Architecture

### Network Layer (Ktor Migration)

The SDK has been migrated from Retrofit to Ktor for better Kotlin Multiplatform support:

#### Key Components:

1. **KtorClientFactory**: Centralized HTTP client configuration
   - Content negotiation for JSON serialization
   - Logging for debugging and monitoring
   - Lenient JSON parsing for robust error handling

2. **ApiService**: Low-level HTTP communication
   - POST requests to analytics endpoints
   - Custom header management
   - Content-Type configuration

3. **ApiClient**: High-level API abstraction
   - Event data transformation
   - Endpoint configuration
   - Header management

4. **BaseRepository**: Common network handling
   - Standardized error processing
   - Response validation
   - Exception handling

#### Migration Benefits:

- ✅ **KMP Ready**: Designed for Kotlin Multiplatform
- ✅ **Modern Kotlin**: Better coroutines and serialization support
- ✅ **Type Safety**: Compile-time safety with Kotlin serialization
- ✅ **Error Handling**: More granular control over HTTP responses
- ✅ **Performance**: Optimized for modern Android development

## 🚀 Quick Start

### 1. Initialize the SDK

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        EloAnalyticsSdk.Builder(this)
            .setConfig(
                EloAnalyticsConfig(
                    baseUrl = "https://api.example.com",
                    endpointUrl = "/analytics/events",
                    batchSize = 20,
                    isDebug = BuildConfig.DEBUG,
                    appsFlyerId = "your-appsflyer-id",
                    headers = mapOf("Authorization" to "Bearer token"),
                    userIdAttributeKeyName = "user_id"
                )
            )
            .setRuntimeProvider(object : EloAnalyticsRuntimeProvider {
                override fun getAppVersionCode(): String = BuildConfig.VERSION_NAME
                override suspend fun isUserLoggedIn(): Boolean = true
                override suspend fun getCurrentUserId(): Long = 123L
                override suspend fun getGuestUserId(): Long = 0L
                override fun isAnalyticsSdkEnabled(): Boolean = true
            })
            .build()
    }
}
```

### 2. Track Events

```kotlin
// Get the SDK instance
val analytics = EloAnalyticsSdk.getInstance()

// Track a simple event
analytics.trackEvent("user_login", mapOf(
    "method" to "email",
    "timestamp" to System.currentTimeMillis()
))

// Track a complex event with custom data
analytics.trackEvent("purchase_completed", mapOf(
    "product_id" to "PROD_123",
    "price" to 29.99,
    "currency" to "USD",
    "payment_method" to "credit_card"
))
```

### 3. Update Headers (if needed)

```kotlin
analytics.updateHeader(mapOf(
    "Authorization" to "Bearer new-token",
    "X-Custom-Header" to "value"
))
```

## 📊 Event Structure

Events are automatically enriched with:

- **Event Name**: The identifier you provide
- **Timestamp**: When the event occurred
- **User ID**: Current user or guest ID
- **Session ID**: Unique session identifier
- **AppsFlyer ID**: For attribution tracking
- **Custom Data**: Your provided event attributes

## 🔄 Background Synchronization

The SDK automatically handles:

- **Batch Processing**: Events are batched before sending (default: 10000 events per batch)
- **Offline Storage**: Events stored locally when offline
- **Background Sync**: WorkManager handles background synchronization
- **Simple Processing**: Uses LIMIT only for efficient database fetching
- **Error Handling**: Comprehensive error processing and logging

### Batch Processing Details:

The SDK uses a simplified approach for batch processing:

```kotlin
// Example: Processing 25,000 events
// Batch 1: SELECT * FROM analytics_sdk_events LIMIT 10000
// Batch 2: SELECT * FROM analytics_sdk_events LIMIT 10000  
// Batch 3: SELECT * FROM analytics_sdk_events LIMIT 5000
```

**Key Features:**
- ✅ **Default Batch Size**: 10,000 events per batch (from constants)
- ✅ **Simple Database Queries**: Uses only LIMIT, no OFFSET
- ✅ **No Retry Logic**: Failed batches are logged but not retried
- ✅ **Memory Efficient**: Only current batch loaded into memory
- ✅ **Progress Tracking**: Detailed logging of batch processing

## 🛠️ Configuration Options

### EloAnalyticsConfig

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `baseUrl` | String | Yes | Base URL for the analytics API |
| `endpointUrl` | String | Yes | Endpoint path for events |
| `batchSize` | Int | Yes | Number of events to batch before sending |
| `isDebug` | Boolean | No | Enable debug logging (default: false) |
| `appsFlyerId` | String? | No | AppsFlyer ID for attribution |
| `headers` | Map<String, String> | No | Custom HTTP headers |
| `userIdAttributeKeyName` | String | Yes | Key name for user ID in events |
| `syncBatchSize` | Int? | No | Number of events to process in each sync batch (min: 1000, default: 10000) |

### Configuration Constants

The SDK uses centralized constants defined in `Constant.kt`:

```kotlin
object Constant {
    const val DEFAULT_SYNC_BATCH_SIZE = 10000     // Default batch size
    const val MIN_SYNC_BATCH_SIZE = 1000          // Minimum valid batch size
    const val DEFAULT_DELETE_BATCH_SIZE = 900     // DB delete batch size
}
```

### Batch Size Validation

The SDK validates the `syncBatchSize` parameter during initialization:

- **Minimum Value**: 1000 events
- **Default Value**: 10000 events (if not provided or invalid)
- **Validation**: Automatically uses default if value is null or < 1000

#### Example Logs:

```kotlin
// Valid batch size
EloAnalyticsSdk.Builder(this)
    .setConfig(EloAnalyticsConfig(
        syncBatchSize = 5000,  // ✅ Valid
        // ... other config
    ))
    .build()
// Log: ✅ Sync batch size set to 5000

// Invalid batch size (too small)
EloAnalyticsSdk.Builder(this)
    .setConfig(EloAnalyticsConfig(
        syncBatchSize = 500,   // ❌ Too small
        // ... other config
    ))
    .build()
// Log: ❌ ERROR: Sync batch size (500) cannot be less than 1000. Using default value of 10000
// Log: 💡 TIP: Set syncBatchSize to at least 1000 for optimal performance

// Null batch size
EloAnalyticsSdk.Builder(this)
    .setConfig(EloAnalyticsConfig(
        syncBatchSize = null,  // Will use default
        // ... other config
    ))
    .build()
// Log: Sync batch size is null, using default value of 10000
```

## 🔧 Advanced Usage

### Custom Runtime Provider

```kotlin
class CustomRuntimeProvider : EloAnalyticsRuntimeProvider {
    override fun getAppVersionCode(): String = BuildConfig.VERSION_NAME
    
    override suspend fun isUserLoggedIn(): Boolean {
        // Your authentication logic
        return authManager.isLoggedIn()
    }
    
    override suspend fun getCurrentUserId(): Long {
        // Get current user ID
        return userManager.getCurrentUserId()
    }
    
    override suspend fun getGuestUserId(): Long {
        // Get guest user ID
        return userManager.getGuestUserId()
    }
    
    override fun isAnalyticsSdkEnabled(): Boolean {
        // Check if analytics should be enabled
        return settingsManager.isAnalyticsEnabled()
    }
}
```

### Manual Event Flushing

```kotlin
// Force flush pending events
analytics.flushPendingEvents(FlushPendingEventTriggerSource.MANUAL)
```

## 🐛 Debugging

Enable debug logging during initialization:

```kotlin
EloAnalyticsConfig(
    isDebug = true,  // Enable debug logging
    // ... other config
)
```

Debug logs will show:
- Event tracking details
- Network request/response information
- Background sync status
- Error details and stack traces

## 🔧 Migration from Retrofit

The SDK has been migrated from Retrofit to Ktor for better KMP support:

### Breaking Changes:
- Error handling improved with better type safety

### Benefits:
- Better KMP compatibility
- Modern Kotlin coroutines support
- Improved error handling
- Type-safe JSON serialization

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Contact the EloAnalytics SDK Team
- Check the documentation for common issues

---