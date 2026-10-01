package com.contractproof.core.platform

import com.contractproof.core.isApplicationDebuggable

actual fun isDebugBuild(): Boolean = isApplicationDebuggable()
