package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.database.DataUsageRecord
import com.example.data.database.SpeedTestRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SpeedTestUIState {
    data object Idle : SpeedTestUIState
    data class Testing(
        val phase: SpeedTestPhase,
        val currentMbps: Double,
        val progress: Float,
        val avgDownload: Double,
        val avgUpload: Double
    ) : SpeedTestUIState
    data class Completed(val downloadMbps: Double, val uploadMbps: Double) : SpeedTestUIState
    data class Error(val message: String) : SpeedTestUIState
}

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NetworkRepository(application)
    private val context: Context get() = getApplication()

    // Shared preferences for visual settings persistence
    private val sharedPrefs = application.getSharedPreferences("net_info_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(sharedPrefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode.name).apply()
    }

    // Tab state management
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Live Signal Metrics State Flow
    private val _signalMetrics = MutableStateFlow(SignalMetrics())
    val signalMetrics: StateFlow<SignalMetrics> = _signalMetrics.asStateFlow()

    // Live Dual SIM Metrics State Flow
    private val _dualSimMetrics = MutableStateFlow<List<SignalMetrics>>(emptyList())
    val dualSimMetrics: StateFlow<List<SignalMetrics>> = _dualSimMetrics.asStateFlow()

    // Live Data Usage State Flow
    private val _dataUsage = MutableStateFlow(UsageStats())
    val dataUsage: StateFlow<UsageStats> = _dataUsage.asStateFlow()

    // Live Speed Test State Flow
    private val _speedTestState = MutableStateFlow<SpeedTestState>(SpeedTestState())
    val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    // Database History Flows
    val speedTestHistory: StateFlow<List<SpeedTestRecord>> = repository.allSpeedTests
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dataUsageHistory: StateFlow<List<DataUsageRecord>> = repository.allUsageRecords
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var signalPollJob: Job? = null
    private var speedTestJob: Job? = null

    init {
        startPollingMetrics()
        viewModelScope.launch {
            // Log daily usage records on startup + generate fallback records if permission not active yet
            repository.recordTodayUsage(context)
        }
    }

    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        refreshDataUsage()
    }

    // Refresh data usage measurements manually
    fun refreshDataUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getLiveUsageStats(context)
            _dataUsage.value = stats
            
            // Record to DB history if possible
            repository.recordTodayUsage(context)
        }
    }

    // High performance background pollers for cellular signals
    fun startPollingMetrics() {
        signalPollJob?.cancel()
        signalPollJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                try {
                    val metricsList = repository.getLiveDualSimMetrics(context)
                    _dualSimMetrics.value = metricsList
                    if (metricsList.isNotEmpty()) {
                        _signalMetrics.value = metricsList[0]
                    } else {
                        _signalMetrics.value = repository.getLiveSignalMetrics(context)
                    }
                } catch (e: Exception) {
                    // Fail-safe protection on low-end devices
                }
                delay(2500) // Poll every 2.5s for power efficiency
            }
        }
    }

    fun stopPollingMetrics() {
        signalPollJob?.cancel()
    }

    // Interactive Core Speed Tester Initiator
    fun runSpeedTest() {
        if (_speedTestState.value.phase != SpeedTestPhase.IDLE && 
            _speedTestState.value.phase != SpeedTestPhase.COMPLETED &&
            _speedTestState.value.phase != SpeedTestPhase.FAILED) {
            return // Work in progress, ignore duplicate taps
        }

        speedTestJob?.cancel()
        speedTestJob = viewModelScope.launch {
            repository.runSpeedTest()
                .onEach { state ->
                    _speedTestState.value = state
                    
                    // On complete, log into Room database
                    if (state.phase == SpeedTestPhase.COMPLETED) {
                        val currentOperator = _signalMetrics.value.operatorName
                        val currentNetwork = _signalMetrics.value.networkType
                        repository.saveSpeedTest(
                            download = state.averageDownloadMbps,
                            upload = state.averageUploadMbps,
                            operator = currentOperator,
                            networkType = currentNetwork
                        )
                    }
                }
                .catch { error ->
                    _speedTestState.value = SpeedTestState(
                        phase = SpeedTestPhase.FAILED,
                        errorMessage = error.localizedMessage ?: "Unknown Test Failure"
                    )
                }
                .collect()
        }
    }

    fun resetSpeedTest() {
        speedTestJob?.cancel()
        _speedTestState.value = SpeedTestState()
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPollingMetrics()
        speedTestJob?.cancel()
    }
}
