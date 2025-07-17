package com.greenhorn.neuronet.worker.syncWorker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.greenhorn.neuronet.AnalyticsSdkUtilProvider
import com.greenhorn.neuronet.EloAnalyticsSdk
import com.greenhorn.neuronet.EloAnalyticsEvent
import com.greenhorn.neuronet.client.ApiClient
import com.greenhorn.neuronet.client.eloAnalytics.EloAnalyticsRepositoryImpl
import com.greenhorn.neuronet.client.useCase.EloAnalyticsEventUseCase
import com.greenhorn.neuronet.client.useCase.EloAnalyticsEventUseCaseImpl
import com.greenhorn.neuronet.db.AnalyticsDatabase
import com.greenhorn.neuronet.db.repository.EloAnalyticsLocalRepositoryImpl
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCase
import com.greenhorn.neuronet.db.usecase.EloAnalyticsLocalEventUseCaseImpl
import com.greenhorn.neuronet.header.MutableHeaderProvider
import com.greenhorn.neuronet.interceptor.HttpInterceptor
import com.greenhorn.neuronet.interceptor.RetryInterceptor
import com.greenhorn.neuronet.log.utils.ConnectivityImpl
import com.greenhorn.neuronet.log.utils.DataStoreConstants
import com.greenhorn.neuronet.log.utils.onFailure
import com.greenhorn.neuronet.log.utils.onSuccess
import com.greenhorn.neuronet.model.mapper.EventMapper
import com.greenhorn.neuronet.service.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

internal class EloAnalyticsSyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val eloAnalyticsLocalEventUseCase: EloAnalyticsLocalEventUseCase,
    private val eloAnalyticsEventUseCase: EloAnalyticsEventUseCase,
    private val analyticsSdkUtilProvider: AnalyticsSdkUtilProvider
) : CoroutineWorker(context, params = workerParameters) {

    companion object {
        private const val TAG = "EloAnalyticsSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(
            EloAnalyticsSdk.TAG2,
            "ELO ANALYTICS worker started!"
        )

        return withContext(Dispatchers.IO) {
            try {
                val storedPendingEvents = fetchStoredEvents().map { event ->
                    event.copy(
                        eventData = event.eventData.toMutableMap().apply {
                            val id =
                                if (event.isUserLogin) analyticsSdkUtilProvider.getCurrentUserId() else analyticsSdkUtilProvider.getGuestUserId()
                            this[DataStoreConstants.USER_ID] = id.toString()
                        }
                    )
                }

                if (storedPendingEvents.isEmpty()) {
                    Log.d(
                        EloAnalyticsSdk.TAG2,
                        "pending events are zero, so finishing worker!"
                    )
                    return@withContext Result.success()
                }

                sendEventsToServer(storedPendingEvents)

                return@withContext Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(EloAnalyticsSdk.TAG2, "exception in $TAG exception: ${e.message}")
                analyticsSdkUtilProvider.recordFirebaseNonFatal(error = e)

                Result.failure()
            }
        }
    }

    private suspend fun fetchStoredEvents(): List<EloAnalyticsEvent> {
        return eloAnalyticsLocalEventUseCase.getEvents().also { storedEvents ->
            Log.d(EloAnalyticsSdk.TAG2, "Fetched pending events count: ${storedEvents.size}")
        }
    }

    private suspend fun sendEventsToServer(storedEvents: List<EloAnalyticsEvent>) {
        eloAnalyticsEventUseCase.sendAnalyticEvents(
            EventMapper.toEventDtos(
                events = storedEvents,
                currentUserId = analyticsSdkUtilProvider.getCurrentUserId(),
                guestUserId = analyticsSdkUtilProvider.getGuestUserId()
            )
        )
            .onSuccess {
                Log.d(EloAnalyticsSdk.TAG2, "Successfully sent ${storedEvents.size} events to server!")
                deleteSentEvents(storedEvents)
            }
            .onFailure {
                Log.e(
                    EloAnalyticsSdk.TAG2,
                    "Failed to send ${storedEvents.size} events: ${it.message}"
                )
            }
    }

    private suspend fun deleteSentEvents(storedEvents: List<EloAnalyticsEvent>) {
        val deletedCount = eloAnalyticsLocalEventUseCase.deleteEvents(storedEvents.map { it.id })
        Log.d(EloAnalyticsSdk.TAG2, "Deleted $deletedCount events from local DB after sending.")
    }
}

class EloAnalyticsWorkerFactory(
//    private val delegate: WorkerFactory?, todo: check
    private val analyticsSdkApiData: AnalyticsSdkApiData
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == EloAnalyticsSyncWorker::class.java.name) {
            val baseUrl = checkNotNull(analyticsSdkApiData.baseUrl) { "Base URL must be set." }
            val finalApiEndpoint =
                requireNotNull(analyticsSdkApiData.apiEndPoint) { "API endpoint must be set." }
            AnalyticsSdkUtilProvider.setApiEndPoint(endPoint = baseUrl + finalApiEndpoint)

            // 2. Build dependencies in a clean, chained manner
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level =
                    if (analyticsSdkApiData.isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }

            val headerProvider = MutableHeaderProvider(analyticsSdkApiData.headers)
            // Use a custom OkHttpClient if provided, otherwise build a default one.
            val finalOkHttpClient = analyticsSdkApiData.customOkHttpClient ?: OkHttpClient.Builder()
                .addInterceptor(
                    analyticsSdkApiData.customInterceptor ?: HttpInterceptor(
                        headerProvider
                    )
                )
                .addInterceptor(RetryInterceptor())
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(finalOkHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            val analyticsDao = AnalyticsDatabase.getInstance(appContext).eloAnalyticsEventDao()
            val localEventRepository = EloAnalyticsLocalRepositoryImpl(analyticsDao)
            val localEventUseCase = EloAnalyticsLocalEventUseCaseImpl(localEventRepository)
            val apiClient = ApiClient(apiClient = apiService)
            val connectivity = ConnectivityImpl(appContext)
            val remoteEventRepository = EloAnalyticsRepositoryImpl(apiClient, connectivity)
            val remoteEventUseCase = EloAnalyticsEventUseCaseImpl(remoteEventRepository)
            EloAnalyticsSyncWorker(
                context = appContext,
                workerParameters = workerParameters,
                eloAnalyticsLocalEventUseCase = localEventUseCase,
                eloAnalyticsEventUseCase = remoteEventUseCase,
                analyticsSdkUtilProvider = AnalyticsSdkUtilProvider
            )
        } else {
            // Return null to delegate to the default WorkerFactory
            null //todo: check if needs to return null or
            //    delegate?.createWorker(appContext, workerClassName, workerParameters)

        }
    }
}

// In your analytics library, perhaps in an object or a public class
object AnalyticsWorkManagerInitializer {
    fun createAnalyticsWorkerFactory(sdkApiDependency: AnalyticsSdkApiData): WorkerFactory {
        return EloAnalyticsWorkerFactory(sdkApiDependency)
    }
}

data class AnalyticsSdkApiData(
    val baseUrl: String,
    val apiEndPoint: String,
    val isDebug: Boolean,
    val headers: Map<String, String>,
    val customOkHttpClient: OkHttpClient?,
    val customInterceptor: Interceptor?
)