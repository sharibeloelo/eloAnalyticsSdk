package com.greenhorn.neuronet.client.eloAnalytics

import android.util.Log
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.log.utils.BaseRepository
import com.greenhorn.neuronet.log.utils.Connectivity
import com.greenhorn.neuronet.log.utils.Failure
import com.greenhorn.neuronet.log.utils.NetworkResult
import com.greenhorn.neuronet.log.utils.Result
import com.greenhorn.neuronet.log.utils.Success
import com.greenhorn.neuronet.model.EloAnalyticsEventDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

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
                eloAnalyticsAPI.sendEventsNew(events = events) //todo: need to convert into json
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
            Log.e(TAG, "Error in sendEloAnalyticEvents error: ${error.message}")
            Failure(
                errorResponse = createExceptionResponse(e = error)
            )
        }
    }
}

fun List<EloAnalyticsEventDto>.toJsonString(moshi: Moshi): String {
    val type = Types.newParameterizedType(List::class.java, EloAnalyticsEventDto::class.java)
    val adapter = moshi.adapter<List<EloAnalyticsEventDto>>(type)
    return adapter.toJson(this)
}
