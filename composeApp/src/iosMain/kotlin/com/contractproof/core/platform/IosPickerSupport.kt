package com.contractproof.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal fun presentPdfPicker(onPicked: (PickedPdf?) -> Unit) {
    val host = topViewController() ?: run {
        onPicked(null)
        return
    }
    val types = listOf(UTTypePDF)
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
    val delegate = DocumentPickerDelegate(onPicked)
    picker.delegate = delegate
    picker.allowsMultipleSelection = false
    host.presentViewController(picker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
internal fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onPicked: (CapturedPhoto?) -> Unit,
) {
    val host = topViewController() ?: run {
        onPicked(null)
        return
    }
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        onPicked(null)
        return
    }
    val picker = UIImagePickerController()
    picker.sourceType = sourceType
    val delegate = ImagePickerDelegate(onPicked)
    picker.delegate = delegate
    host.presentViewController(picker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
internal fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}

@OptIn(ExperimentalForeignApi::class)
internal fun shareUrl(url: String, title: String) {
    val host = topViewController() ?: return
    val nsUrl = NSURL.URLWithString(url) ?: return
    val items = listOf(title, nsUrl)
    val controller = platform.UIKit.UIActivityViewController(items, null)
    host.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toJpegCapturedPhoto(): CapturedPhoto? {
    val data = UIImageJPEGRepresentation(this, 0.85) ?: return null
    val bytes = data.toByteArray()
    val path = NSTemporaryDirectory() + "evidence-${kotlin.random.Random.nextLong()}.jpg"
    if (!bytes.writeToFile(path)) {
        return null
    }
    return CapturedPhoto(localPath = path, bytes = bytes, mimeType = "image/jpeg")
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = length.toInt()
    val result = ByteArray(length)
    if (length > 0) {
        result.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return result
}

private class ImagePickerDelegate(
    private val onPicked: (CapturedPhoto?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onPicked(image?.toJpegCapturedPhoto())
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onPicked(null)
    }
}

private class DocumentPickerDelegate(
    private val onPicked: (PickedPdf?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: run {
            onPicked(null)
            return
        }
        val data = NSData.dataWithContentsOfURL(url) ?: run {
            onPicked(null)
            return
        }
        val bytes = data.toByteArray()
        if (bytes.isEmpty()) {
            onPicked(null)
            return
        }
        val name = url.lastPathComponent ?: "contract.pdf"
        onPicked(PickedPdf(fileName = name, bytes = bytes))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(null)
    }
}
