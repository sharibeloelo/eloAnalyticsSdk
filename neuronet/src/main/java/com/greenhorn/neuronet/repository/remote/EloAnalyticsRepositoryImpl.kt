package com.greenhorn.neuronet.repository.remote

import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.utils.BaseRepository
import com.greenhorn.neuronet.utils.Connectivity
import com.greenhorn.neuronet.utils.EloSdkLogger
import com.greenhorn.neuronet.utils.Failure
import com.greenhorn.neuronet.utils.NetworkResult
import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.utils.Success

/**
 * Implementation of the analytics repository for remote event transmission.
 * 
 * This repository handles the transmission of analytics events to the remote backend
 * using Ktor HTTP client. It provides a clean abstraction layer that handles:
 * - Network connectivity checks
 * - HTTP request execution
 * - Response processing and error handling
 * - Logging and debugging information
 * 
 * The repository extends BaseRepository to leverage common network handling patterns
 * and error processing logic.
 * 
 * @author EloAnalytics SDK Team
 * @since 1.0.0
 */
internal class EloAnalyticsRepositoryImpl(
    private val eloAnalyticsAPI: ApiClient,
    private val connectivity: Connectivity,
) : EloAnalyticsRepository, BaseRepository() {
    
    /**
     * Sends analytics events to the remote backend.
     * 
     * This method orchestrates the complete flow of sending analytics events:
     * 1. Validates network connectivity
     * 2. Executes the HTTP request through the API client
     * 3. Processes the response and handles errors
     * 4. Provides detailed logging for debugging
     * 
     * The method uses the BaseRepository's network handling infrastructure
     * to ensure consistent error processing and response handling.
     * 
     * @param events List of analytics events to send to the backend
     * @return Result<Boolean> indicating success (true) or failure with error details
     * 
     * @throws Exception if unexpected errors occur during the process
     */
    override suspend fun sendEloAnalyticEvents(events: List<EloAnalyticsEventDto>): Result<Boolean> {
        return try {
            // Check network connectivity before attempting to send events
            if (!connectivity.hasNetworkAccess()) {
                return handleNoInternetCase()
            }

            // Execute the network call using BaseRepository's infrastructure
            return doNetworkCall<Boolean> {
                eloAnalyticsAPI.sendEventsNew(events = events)
            }.run {
                // Process the network result and convert to SDK Result type
                when (this) {
                    is NetworkResult.Success -> {
                        // Successfully sent events to the backend
                        Success(true)
                    }
                    is NetworkResult.Failure -> {
                        // Network call failed, create error response
                        Failure(
                            errorResponse = createErrorResponse(this)
                        )
                    }
                    is NetworkResult.HttpFailure -> {
                        // HTTP-level failure occurred, create error response
                        Failure(
                            errorResponse = createErrorResponse(this)
                        )
                    }
                }
            }
        } catch (error: Exception) {
            // Log the error for debugging purposes
            error.printStackTrace()
            EloSdkLogger.e("Error in sendEloAnalyticEvents error: ${error.message}")
            
            // Return failure result with exception details
            Failure(
                errorResponse = createExceptionResponse(e = error)
            )
        }
    }
}
