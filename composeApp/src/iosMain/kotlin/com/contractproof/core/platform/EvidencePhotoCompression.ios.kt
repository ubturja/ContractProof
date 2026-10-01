package com.contractproof.core.platform

actual fun compressEvidencePhoto(sourcePath: String): CompressedEvidencePhoto {
    val bytes = readFileBytes(sourcePath)
    val destination = "$sourcePath.upload.jpg"
    if (!bytes.writeToFile(destination)) {
        error("Could not write compressed photo.")
    }
    return CompressedEvidencePhoto(localPath = destination, bytes = bytes)
}
