package com.greenhorn.neuronet.model.mapper

import com.greenhorn.neuronet.model.EloAnalyticsEvent
import kotlinx.serialization.json.JsonArray

internal fun Map<String, Any>.toStringMap(): Map<String, String> {
    return this.mapValues { entry -> entry.value.toString() }
}

internal fun List<EloAnalyticsEvent>.toJsonArray(): JsonArray {
    return JsonArray(this.map { it.toJsonObject() })
}