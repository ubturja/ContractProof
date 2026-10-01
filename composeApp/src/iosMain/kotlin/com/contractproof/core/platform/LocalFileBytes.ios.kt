package com.contractproof.core.platform

actual fun readLocalFileBytes(path: String): ByteArray = readFileBytes(path)
