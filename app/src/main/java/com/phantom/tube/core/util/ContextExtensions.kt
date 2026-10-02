package com.phantom.tube.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses ContextWrapper chain to retrieve the hosting Activity.
 * Ensures compatibility when LocalContext is wrapped by a ConfigurationContext for localization.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx != null) {
        if (ctx is Activity) return ctx
        if (ctx is ContextWrapper) {
            ctx = ctx.baseContext
        } else {
            break
        }
    }
    return null
}
