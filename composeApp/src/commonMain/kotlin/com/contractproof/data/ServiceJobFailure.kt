package com.contractproof.data

sealed class ServiceJobFailure : Exception() {
    data object Network : ServiceJobFailure()

    data object Rejected : ServiceJobFailure()
}
