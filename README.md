# Elo Analytics SDK

A modular, extensible analytics SDK for Android, designed to batch, store, and synchronize analytics events efficiently. Built with clean architecture, SOLID principles, and ready for integration into any Android app or library.

---

## Features
- **Event Tracking**: Track custom analytics events with rich metadata.
- **Batching & Offline Support**: Events are batched and stored locally (Room DB) for reliable delivery, even offline.
- **Configurable**: Easily configure endpoints, batch sizes, and runtime providers.
- **Background Sync**: Uses WorkManager for reliable background event synchronization.
- **Extensible & Testable**: Clean separation of concerns, interface-driven, and easy to extend or test.

---

## Architecture Overview

- **SDK Entry Point**: `EloAnalyticsSdk` (with builder pattern)
- **Data Layer**: Room entities, DAOs, and repositories for local storage
- **Network Layer**: API client, service interface, interceptors, and remote repositories
- **Domain Layer**: Use cases and business logic
- **Worker**: Background sync with WorkManager
- **Utilities**: Config, constants, error/result wrappers, and extensions

See the `/src/main/java/com/greenhorn/neuronet/` directory for full modular structure.

---

## Installation

Add the SDK to your project (example for Gradle):

```kotlin
dependencies {
    implementation("com.github.greenhorn-eloelo-event:analytics:1.2.3")
    // Or use your local module
}
```

---

## Quick Start

```kotlin
val sdk = EloAnalyticsSdk.Builder(context)
    .setConfig {
        endpointUrl = "https://api.example.com/analytics"
        batchSize = 20
        appsFlyerId = "your-appsflyer-id"
    }
    .setRuntimeProvider(object : EloAnalyticsRuntimeProvider {
        override fun getHeaders() = mapOf("Authorization" to "Bearer ...")
        override fun getAppVersionCode() = "1.0.0"
        override suspend fun isUserLoggedIn() = true
        override suspend fun getCurrentUserId(key: String) = 123L
        override suspend fun getGuestUserId(key: String) = 0L
        override fun isAnalyticsSdkEnabled() = true
    })
    .build()

sdk.trackEvent("login", mutableMapOf("method" to "google"))
```

---

## Configuration

- **Endpoint URL**: Set your analytics server endpoint.
- **Batch Size**: Number of events to batch before sending.
- **AppsFlyer ID**: (Optional) For attribution.
- **Runtime Provider**: Supply user/session/app info and enable/disable analytics at runtime.

---

## Contributing

Contributions are welcome! Please open issues or submit pull requests for bug fixes, improvements, or new features.

---

## License

[MIT License](LICENSE) 
