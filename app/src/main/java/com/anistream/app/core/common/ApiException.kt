package com.anistream.app.core.common

import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** A failure that already carries a message suitable for the UI. */
class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> message ?: FALLBACK_MESSAGE
    is UnknownHostException -> "No internet connection. Check your network and try again."
    is SocketTimeoutException -> "The anime service took too long to respond. Please try again."
    is SerializationException -> "The anime service returned data AniStream could not read."
    is IOException -> "Network error while contacting the anime service. Please try again."
    else -> message?.takeIf { it.isNotBlank() } ?: FALLBACK_MESSAGE
}

private const val FALLBACK_MESSAGE = "Something went wrong. Please try again."
