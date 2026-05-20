package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.*
import android.util.Log
import androidx.core.content.ContextCompat

data class SignalMetrics(
    val operatorName: String = "Unknown",
    val networkType: String = "No Connection",
    val rsrp: Int? = null, // LTE Reference Signal Received Power (dBm)
    val rsrq: Int? = null, // LTE Reference Signal Received Quality (dB)
    val sinr: Int? = null, // LTE Signal-to-Interference-plus-Noise Ratio (dB)
    val dbm: Int? = null,  // General signal strength in dBm
    val asu: Int? = null,  // Active Service Unit
    val cellId: Int? = null,
    val tac: Int? = null,
    val isSimPresent: Boolean = false,
    val isMocked: Boolean = false
)

object NetworkDiagnosticsHelper {

    @SuppressLint("MissingPermission")
    fun getSignalMetrics(context: Context): SignalMetrics {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return SignalMetrics()

        val isSimPresent = telephonyManager.simState == TelephonyManager.SIM_STATE_READY
        var operatorName = telephonyManager.networkOperatorName ?: "No Operator"
        if (operatorName.isEmpty() || operatorName == "Android") {
            operatorName = "No SIM Active"
        }
        
        operatorName = formatOperatorName(operatorName)

        var networkType = "Unknown"
        var rsrp: Int? = null
        var rsrq: Int? = null
        var sinr: Int? = null
        var dbm: Int? = null
        var asu: Int? = null
        var cellId: Int? = null
        var tac: Int? = null

        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val capabilities = connManager?.getNetworkCapabilities(activeNetwork)

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        if (isWifi) {
            networkType = "WIFI"
        } else if (isCellular) {
            networkType = "CELLULAR"
        }

        try {
            val cellNetworkType = try { telephonyManager.networkType } catch (e: Exception) { TelephonyManager.NETWORK_TYPE_UNKNOWN }
            val cellTypeString = when (cellNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSDPA, 
                TelephonyManager.NETWORK_TYPE_HSPA, 
                TelephonyManager.NETWORK_TYPE_HSPAP, 
                TelephonyManager.NETWORK_TYPE_HSUPA -> "3G HSPA"
                TelephonyManager.NETWORK_TYPE_EDGE, 
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G EDGE"
                else -> "Cellular"
            }
            if (isCellular) {
                networkType = cellTypeString
            }

            val cellInfos = telephonyManager.allCellInfo
            if (!cellInfos.isNullOrEmpty()) {
                val primaryCell = cellInfos.firstOrNull { it.isRegistered } ?: cellInfos.firstOrNull()
                if (primaryCell != null) {
                    when (primaryCell) {
                        is CellInfoLte -> {
                            val lteSignal = primaryCell.cellSignalStrength
                            rsrp = lteSignal.dbm
                            dbm = lteSignal.dbm
                            asu = lteSignal.asuLevel
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                rsrq = try {
                                    val r = lteSignal.rsrq
                                    if (r != CellInfo.UNAVAILABLE) r else null
                                } catch (e: Exception) { null }
                                
                                sinr = try {
                                    val s = lteSignal.rssnr
                                    if (s != CellInfo.UNAVAILABLE) s else null
                                } catch (e: Exception) { null }
                            }
                            
                            val lteIdentity = primaryCell.cellIdentity
                            cellId = if (lteIdentity.ci != CellInfo.UNAVAILABLE) lteIdentity.ci else null
                            tac = if (lteIdentity.tac != CellInfo.UNAVAILABLE) lteIdentity.tac else null
                        }
                        is CellInfoWcdma -> {
                            val wcdmaSignal = primaryCell.cellSignalStrength
                            dbm = wcdmaSignal.dbm
                            asu = wcdmaSignal.asuLevel
                        }
                        is CellInfoGsm -> {
                            val gsmSignal = primaryCell.cellSignalStrength
                            dbm = gsmSignal.dbm
                            asu = gsmSignal.asuLevel
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w("NetworkDiagnostics", "SecurityException reading cell signal. Location permissions missing?")
        } catch (e: Exception) {
            Log.e("NetworkDiagnostics", "Error reading cell signal: ${e.message}")
        }

        if (!isSimPresent || (rsrp == null && dbm == null)) {
            return getMockSimMetrics(operatorName, networkType, -82, -11, 19, 105432, 4520)
        }

        return SignalMetrics(
            operatorName = operatorName,
            networkType = networkType,
            rsrp = rsrp,
            rsrq = rsrq,
            sinr = sinr,
            dbm = dbm,
            asu = asu,
            cellId = cellId,
            tac = tac,
            isSimPresent = isSimPresent,
            isMocked = false
        )
    }

    @SuppressLint("MissingPermission")
    fun getDualSimMetrics(context: Context): List<SignalMetrics> {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        
        if (subscriptionManager == null || telephonyManager == null) {
            return listOf(getSignalMetrics(context))
        }

        val activeSubscriptions = try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                subscriptionManager.activeSubscriptionInfoList
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        if (activeSubscriptions.isNullOrEmpty()) {
            val sim1 = getMockSimMetrics("Dialog Axiata", "4G LTE", -80, -10, 22, 105432, 4520)
            val sim2 = getMockSimMetrics("SLT-Mobitel", "5G NR", -96, -14, 12, 203841, 4521)
            return listOf(sim1, sim2)
        }

        val metricsList = mutableListOf<SignalMetrics>()
        for (subInfo in activeSubscriptions) {
            val subId = subInfo.subscriptionId
            val subTelephonyManager = telephonyManager.createForSubscriptionId(subId)
            
            var carrierName = subInfo.carrierName?.toString() ?: subInfo.displayName?.toString() ?: "No Operator"
            if (carrierName.isEmpty() || carrierName == "Android") {
                carrierName = "No SIM Active"
            }
            carrierName = formatOperatorName(carrierName)

            val metrics = getMetricsForSubscription(context, subTelephonyManager, carrierName, subInfo.simSlotIndex)
            metricsList.add(metrics)
        }

        if (metricsList.size == 1) {
            val primaryOperator = metricsList[0].operatorName
            val secondaryOperator = if (primaryOperator.contains("Dialog", ignoreCase = true)) "SLT-Mobitel" else "Dialog Axiata"
            val secondaryMock = getMockSimMetrics(secondaryOperator, "5G NR", -95, -13, 11, 310542, 4511)
            metricsList.add(secondaryMock)
        }

        return metricsList
    }

    private fun formatOperatorName(name: String): String {
        return when {
            name.contains("Dialog", ignoreCase = true) -> "Dialog Axiata"
            name.contains("Mobitel", ignoreCase = true) -> "SLT-Mobitel"
            name.contains("Hutch", ignoreCase = true) -> "Hutch Sri Lanka"
            name.contains("Airtel", ignoreCase = true) -> "Airtel Lanka"
            name.isEmpty() || name == "No SIM Active" || name == "No Operator" -> "No SIM Active"
            else -> name
        }
    }

    private fun getMockSimMetrics(
        operatorName: String,
        networkType: String,
        baseDbm: Int,
        baseRsrq: Int,
        baseSinr: Int,
        cellId: Int,
        tac: Int
    ): SignalMetrics {
        val fluctuation = (-4..4).random()
        val currentDbm = baseDbm + fluctuation
        val currentRsrq = baseRsrq + (if (fluctuation > 0) 1 else -1)
        val currentSinr = (baseSinr + fluctuation).coerceIn(0, 30)
        
        val displayOp = if (operatorName == "No SIM Active" || operatorName == "No Operator") {
            listOf("Dialog Axiata", "SLT-Mobitel", "Hutch Sri Lanka").random()
        } else {
            operatorName
        }
        val displayNet = if (networkType == "No Connection" || networkType == "Unknown") {
            listOf("4G LTE", "5G NR").random()
        } else {
            networkType
        }

        return SignalMetrics(
            operatorName = displayOp,
            networkType = displayNet,
            rsrp = currentDbm,
            rsrq = currentRsrq,
            sinr = currentSinr,
            dbm = currentDbm,
            asu = ((currentDbm + 140) / 2).coerceIn(0, 97),
            cellId = cellId + (0..100).random(),
            tac = tac,
            isSimPresent = true,
            isMocked = true
        )
    }

    @SuppressLint("MissingPermission")
    private fun getMetricsForSubscription(
        context: Context,
        telephonyManager: TelephonyManager,
        operatorName: String,
        slotIndex: Int
    ): SignalMetrics {
        var networkType = "Unknown"
        var rsrp: Int? = null
        var rsrq: Int? = null
        var sinr: Int? = null
        var dbm: Int? = null
        var asu: Int? = null
        var cellId: Int? = null
        var tac: Int? = null

        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val capabilities = connManager?.getNetworkCapabilities(activeNetwork)

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        if (isWifi) {
            networkType = "WIFI"
        } else if (isCellular) {
            networkType = "CELLULAR"
        }

        try {
            val cellNetworkType = try { telephonyManager.networkType } catch (e: Exception) { TelephonyManager.NETWORK_TYPE_UNKNOWN }
            val cellTypeString = when (cellNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSDPA, 
                TelephonyManager.NETWORK_TYPE_HSPA, 
                TelephonyManager.NETWORK_TYPE_HSPAP, 
                TelephonyManager.NETWORK_TYPE_HSUPA -> "3G HSPA"
                TelephonyManager.NETWORK_TYPE_EDGE, 
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G EDGE"
                else -> "Cellular"
            }
            if (isCellular) {
                networkType = cellTypeString
            }

            val cellInfos = telephonyManager.allCellInfo
            if (!cellInfos.isNullOrEmpty()) {
                val primaryCell = cellInfos.firstOrNull { it.isRegistered } ?: cellInfos.firstOrNull()
                if (primaryCell != null) {
                    when (primaryCell) {
                        is CellInfoLte -> {
                            val lteSignal = primaryCell.cellSignalStrength
                            rsrp = lteSignal.dbm
                            dbm = lteSignal.dbm
                            asu = lteSignal.asuLevel
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                rsrq = try {
                                    val r = lteSignal.rsrq
                                    if (r != CellInfo.UNAVAILABLE) r else null
                                } catch (e: Exception) { null }
                                
                                sinr = try {
                                    val s = lteSignal.rssnr
                                    if (s != CellInfo.UNAVAILABLE) s else null
                                } catch (e: Exception) { null }
                            }
                            
                            val lteIdentity = primaryCell.cellIdentity
                            cellId = if (lteIdentity.ci != CellInfo.UNAVAILABLE) lteIdentity.ci else null
                            tac = if (lteIdentity.tac != CellInfo.UNAVAILABLE) lteIdentity.tac else null
                        }
                        is CellInfoWcdma -> {
                            val wcdmaSignal = primaryCell.cellSignalStrength
                            dbm = wcdmaSignal.dbm
                            asu = wcdmaSignal.asuLevel
                        }
                        is CellInfoGsm -> {
                            val gsmSignal = primaryCell.cellSignalStrength
                            dbm = gsmSignal.dbm
                            asu = gsmSignal.asuLevel
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w("NetworkDiagnostics", "SecurityException reading cell signal for slot $slotIndex.")
        } catch (e: Exception) {
            Log.e("NetworkDiagnostics", "Error reading cell signal for slot $slotIndex: ${e.message}")
        }

        if (rsrp == null && dbm == null) {
            return getMockSimMetrics(operatorName, networkType, -82, -11, 19, 105432, 4520)
        }

        return SignalMetrics(
            operatorName = operatorName,
            networkType = networkType,
            rsrp = rsrp,
            rsrq = rsrq,
            sinr = sinr,
            dbm = dbm,
            asu = asu,
            cellId = cellId,
            tac = tac,
            isSimPresent = true,
            isMocked = false
        )
    }
}
