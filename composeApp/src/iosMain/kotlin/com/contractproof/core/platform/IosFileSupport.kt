package com.contractproof.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal fun readFileBytes(path: String): ByteArray {
    val data = NSData.dataWithContentsOfFile(path) ?: error("Could not read file at $path")
    val length = data.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.writeToFile(path: String): Boolean {
    val data = toNsData()
    return data.writeToFile(path, atomically = true)
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNsData(): NSData {
    if (isEmpty()) {
        return NSData()
    }
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}
