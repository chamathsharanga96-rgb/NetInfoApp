package com.example.data

import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

data class UsageStats(
    val mobileBytesToday: Long = 0,
    val wifiBytesToday: Long = 0,
    val mobileBytesSinceBoot: Long = 0,
    val wifiBytesSinceBoot: Long = 0,
    val isPermissionGranted: Boolean = false
)

object DataUsageHelper {

    fun isUsagePermissionGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
                ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("ServiceCast")
    fun getDailyDataUsage(context: Context): UsageStats {
        val permissionGranted = isUsagePermissionGranted(context)
        
        // Always extract live data since state boot - this is zero-permission!
        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()
        val mobileTotalBoot = if (mobileRx != TrafficStats.UNSUPPORTED.toLong() && mobileTx != TrafficStats.UNSUPPORTED.toLong()) {
            mobileRx + mobileTx
        } else {
            0L
        }

        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val wifiTotalBoot = if (totalRx != TrafficStats.UNSUPPORTED.toLong() && totalTx != TrafficStats.UNSUPPORTED.toLong()) {
            val wifiRx = totalRx - mobileRx
            val wifiTx = totalTx - mobileTx
            val calculatedWifi = wifiRx + wifiTx
            if (calculatedWifi > 0L) calculatedWifi else 0L
        } else {
            0L
        }

        if (!permissionGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return UsageStats(
                mobileBytesToday = 0,
                wifiBytesToday = 0,
                mobileBytesSinceBoot = mobileTotalBoot,
                wifiBytesSinceBoot = wifiTotalBoot,
                isPermissionGranted = false
            )
        }

        try {
            val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
                ?: return UsageStats(
                    mobileBytesSinceBoot = mobileTotalBoot,
                    wifiBytesSinceBoot = wifiTotalBoot,
                    isPermissionGranted = permissionGranted
                )

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            // Query Mobile Usage for today
            var mobileBytesToday = 0L
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val subscriberId = try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    telephonyManager?.subscriberId
                } else null
            } catch (e: Exception) {
                null
            }

            try {
                val bucket = networkStatsManager.querySummaryForDevice(
                    NetworkCapabilities.TRANSPORT_CELLULAR,
                    subscriberId,
                    startTime,
                    endTime
                )
                mobileBytesToday = bucket.rxBytes + bucket.txBytes
            } catch (e: Exception) {
                Log.e("DataUsageHelper", "Error querying cellular usage today: ${e.message}")
            }

            // Query WiFi Usage for today
            var wifiBytesToday = 0L
            try {
                val bucket = networkStatsManager.querySummaryForDevice(
                    NetworkCapabilities.TRANSPORT_WIFI,
                    "",
                    startTime,
                    endTime
                )
                wifiBytesToday = bucket.rxBytes + bucket.txBytes
            } catch (e: Exception) {
                Log.e("DataUsageHelper", "Error querying WIFI usage today: ${e.message}")
            }

            return UsageStats(
                mobileBytesToday = mobileBytesToday,
                wifiBytesToday = wifiBytesToday,
                mobileBytesSinceBoot = mobileTotalBoot,
                wifiBytesSinceBoot = wifiTotalBoot,
                isPermissionGranted = true
            )

        } catch (e: Exception) {
            Log.e("DataUsageHelper", "Error extracting querySummary values: ${e.message}")
            return UsageStats(
                mobileBytesSinceBoot = mobileTotalBoot,
                wifiBytesSinceBoot = wifiTotalBoot,
                isPermissionGranted = permissionGranted
            )
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1] + "B"
        return String.format(Locale.getDefault(), "%.2f %s", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }
}
