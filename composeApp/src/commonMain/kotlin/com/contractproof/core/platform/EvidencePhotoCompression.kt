package com.contractproof.core.platform

data class CompressedEvidencePhoto(
    val localPath: String,
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg",
)

expect fun compressEvidencePhoto(sourcePath: String): CompressedEvidencePhoto
