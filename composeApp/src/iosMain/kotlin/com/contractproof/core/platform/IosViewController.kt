package com.contractproof.core.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

internal fun topViewController(): UIViewController? {
    IosRootViewController.current?.let { return it }
    val app = UIApplication.sharedApplication
    val keyWindow = app.keyWindow
        ?: app.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
    return keyWindow?.rootViewController
}
