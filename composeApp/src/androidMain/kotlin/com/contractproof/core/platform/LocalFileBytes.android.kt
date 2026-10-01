package com.contractproof.core.platform

import java.io.File

actual fun readLocalFileBytes(path: String): ByteArray {
    return File(path).readBytes()
}
