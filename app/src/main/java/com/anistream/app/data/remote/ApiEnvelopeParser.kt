package com.anistream.app.data.remote

import com.anistream.app.core.common.ApiException
import com.anistream.app.data.remote.dto.ApiEnvelope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal fun defaultApiJson(): Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

internal inline fun <reified T> decodeApiData(json: Json, body: String): T {
    val envelope = json.decodeFromString<ApiEnvelope<T>>(body)
    val meta = envelope.meta
    if (meta != null && !meta.status) {
        val message = meta.message.ifBlank { "The anime service rejected the request." }
        throw ApiException(if (meta.code > 0) "$message (${meta.code})" else message)
    }
    return envelope.data ?: throw ApiException("The anime service returned no data.")
}

internal fun apiHttpMessage(code: Int): String = when (code) {
    404 -> "The anime service could not find that title (404)."
    429 -> "The anime service is rate limiting requests. Wait a moment, then retry."
    in 500..599 -> "The anime service is unavailable right now ($code). Try again shortly."
    else -> "The anime service returned HTTP $code."
}
