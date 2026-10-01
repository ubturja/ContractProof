package com.contractproof.core.push

object PendingNotificationTap {
    var pending: InboundNotification? = null

    fun consume(): InboundNotification? {
        val value = pending
        pending = null
        return value
    }
}
