package com.greenhorn.neuronet.client.repository

import android.util.Log
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.utils.BaseRepository
import com.greenhorn.neuronet.utils.Connectivity
import com.greenhorn.neuronet.utils.Failure
import com.greenhorn.neuronet.utils.NetworkResult
import com.greenhorn.neuronet.utils.Result
import com.greenhorn.neuronet.utils.Success
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.greenhorn.neuronet.utils.EloSdkLogger

private const val TAG = "EloAnalyticsRepositoryImpl"
class EloAnalyticsRepositoryImpl(
    private val eloAnalyticsAPI: ApiClient,
    private val connectivity: Connectivity,
) : EloAnalyticsRepository, BaseRepository() {
    override suspend fun sendEloAnalyticEvents(events: List<EloAnalyticsEventDto>): Result<Boolean> {
        return try {
            if (!connectivity.hasNetworkAccess()) {
                return handleNoInternetCase()
            }

            return doNetworkCall {
                eloAnalyticsAPI.sendEventsNew(events = events)
            }.run {
                when (this) {
                    is NetworkResult.Success -> Success(true)
                    is NetworkResult.Failure -> Failure(
                        errorResponse = createErrorResponse(this)
                    )

                    is NetworkResult.HttpFailure -> Failure(
                        errorResponse = createErrorResponse(this)
                    )
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
            EloSdkLogger.e( "Error in sendEloAnalyticEvents error: ${error.message}")
            Failure(
                errorResponse = createExceptionResponse(e = error)
            )
        }
    }
}
