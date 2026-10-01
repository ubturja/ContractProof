package com.contractproof.app

import androidx.compose.ui.window.ComposeUIViewController
import com.contractproof.core.platform.IosRootViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController { App() }
    IosRootViewController.current = controller
    return controller
}
