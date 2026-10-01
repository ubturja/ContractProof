package com.contractproof.core.push

fun interface PushNotificationHandler {
    fun onNotificationTap(notification: InboundNotification)
}

object PushNotificationRouter {
    private var handler: PushNotificationHandler? = null

    fun register(handler: PushNotificationHandler) {
        this.handler = handler
    }

    fun clear() {
        handler = null
    }

    fun onNotificationTap(notification: InboundNotification) {
        handler?.onNotificationTap(notification)
    }
}
