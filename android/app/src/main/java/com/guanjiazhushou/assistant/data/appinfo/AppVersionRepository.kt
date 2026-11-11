package com.guanjiazhushou.assistant.data.appinfo

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.guanjiazhushou.assistant.BuildConfig
import com.guanjiazhushou.assistant.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppVersionInfo(
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int,
    val kotlinVersion: String,
    val deviceAndroidVersion: String,
    val deviceApiLevel: Int,
)

@Singleton
class AppVersionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getAppVersionInfo(): AppVersionInfo {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        val appLabel = packageManager.getApplicationLabel(packageInfo.applicationInfo!!).toString()
        return AppVersionInfo(
            appName = appLabel,
            versionName = packageInfo.versionName.orEmpty(),
            versionCode = versionCode,
            packageName = packageName,
            minSdk = BuildConfig.MIN_SDK,
            targetSdk = BuildConfig.TARGET_SDK,
            compileSdk = BuildConfig.COMPILE_SDK,
            kotlinVersion = BuildConfig.KOTLIN_VERSION,
            deviceAndroidVersion = Build.VERSION.RELEASE,
            deviceApiLevel = Build.VERSION.SDK_INT,
        )
    }

    fun loadThirdPartyLicenses(): String =
        context.resources.openRawResource(R.raw.third_party_licenses)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
}
