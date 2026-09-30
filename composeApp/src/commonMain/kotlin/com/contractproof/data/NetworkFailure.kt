package com.contractproof.data

fun Throwable.isOfflineFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val name = current::class.simpleName.orEmpty()
        if (
            name == "IOException" ||
            name == "UnknownHostException" ||
            name == "ConnectException" ||
            name == "SocketTimeoutException" ||
            name == "HttpRequestTimeoutException"
        ) {
            return true
        }
        current = current.cause
    }
    return false
}

fun Throwable.describeChain(): String {
    return generateSequence(this) { cause -> cause.cause }
        .joinToString(separator = " ") { error ->
            "${error::class.simpleName.orEmpty()} ${error.message.orEmpty()}"
        }
}
