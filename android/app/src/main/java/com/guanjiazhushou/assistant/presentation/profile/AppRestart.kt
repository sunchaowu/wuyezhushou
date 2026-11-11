package com.guanjiazhushou.assistant.presentation.profile

import android.app.Activity
import android.content.Context
import kotlin.system.exitProcess

object AppRestart {
    fun restart(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        intent.addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
        )
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
        exitProcess(0)
    }
}
