package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.*
import com.example.data.database.DataUsageRecord
import com.example.data.database.SpeedTestRecord
import com.example.ui.NetworkViewModel
import com.example.ui.AppThemeMode
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NetworkViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            val useDarkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = useDarkTheme) {
                MainAppScreen(viewModel)
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppScreen(viewModel: NetworkViewModel = viewModel()) {
    val context = LocalContext.current
    
    val activeTab by viewModel.currentTab.collectAsState()
    val signalMetrics by viewModel.signalMetrics.collectAsState()
    val dualSimMetrics by viewModel.dualSimMetrics.collectAsState()
    val dataUsage by viewModel.dataUsage.collectAsState()
    val speedTestState by viewModel.speedTestState.collectAsState()
    val speedTestHistory by viewModel.speedTestHistory.collectAsState()
    val dataUsageHistory by viewModel.dataUsageHistory.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    // Android runtime permissions solicitor
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        viewModel.startPollingMetrics()
    }

    // Proactively launch permission request on startup for premium diagnostic accuracy
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(painterResource(id = R.drawable.ic_signal_cellular), contentDescription = "Signal Diagnostics", modifier = Modifier.size(24.dp)) },
                    label = { Text("Signal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(painterResource(id = R.drawable.ic_speedometer), contentDescription = "Speed Test", modifier = Modifier.size(24.dp)) },
                    label = { Text("Speed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(painterResource(id = R.drawable.ic_data_usage), contentDescription = "Data Tracker", modifier = Modifier.size(24.dp)) },
                    label = { Text("Data", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(painterResource(id = R.drawable.ic_history_log), contentDescription = "History Logs", modifier = Modifier.size(24.dp)) },
                    label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header with Sri Lankan touch and Professional Polish branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NI",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text(
                            text = "NET INFO",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Sri Lanka Network Companion",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Operator details tag in beautiful Professional pill (Dual SIM support)
                val topOperatorText = remember(dualSimMetrics, signalMetrics) {
                    if (dualSimMetrics.size >= 2) {
                        val first = dualSimMetrics[0].operatorName.substringBefore(" ")
                        val second = dualSimMetrics[1].operatorName.substringBefore(" ")
                        "$first | $second"
                    } else {
                        signalMetrics.operatorName
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_signal_cellular),
                            contentDescription = "Active network",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = topOperatorText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Central tab controller
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> SignalDiagnosticsScreen(dualSimMetrics, permissionsGranted) {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_PHONE_STATE
                            )
                        )
                    }
                    1 -> SpeedTestScreen(speedTestState, signalMetrics) {
                        viewModel.runSpeedTest()
                    }
                    2 -> DataTrackerScreen(dataUsage, context) {
                        viewModel.refreshDataUsage()
                    }
                    3 -> HistoryScreen(
                        speedTests = speedTestHistory,
                        dataUsage = dataUsageHistory,
                        currentTheme = themeMode,
                        onThemeSelect = { viewModel.setThemeMode(it) }
                    ) {
                        viewModel.clearAllHistory()
                    }
                }
            }
        }
    }
}

@Composable
fun SignalDiagnosticsScreen(
    dualSimMetrics: List<SignalMetrics>,
    permissionsGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedSimIndex by remember { mutableStateOf(0) }
    
    val metrics = remember(dualSimMetrics, selectedSimIndex) {
        if (dualSimMetrics.isNotEmpty()) {
            dualSimMetrics.getOrElse(selectedSimIndex) { dualSimMetrics[0] }
        } else {
            SignalMetrics()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Warning Banner if permission missing
        if (!permissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Permission Alert",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location Permission Required",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Grant location permission to read RSRP, RSRQ, and SINR levels accurately.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Mode Indicator (Real vs Demo)
        if (metrics.isMocked) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Demo Info",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No active SIM detected (Demo simulation active for testing).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Dual SIM Active Info & Slot Selectors
        Text(
            text = "Dual SIM Channels",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp, top = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            dualSimMetrics.forEachIndexed { index, sim ->
                val isSelected = selectedSimIndex == index
                val border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
                val background = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = background),
                    border = border,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSimIndex = index }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "SIM SLOT ${index + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            
                            Icon(
                                painter = painterResource(id = R.drawable.ic_signal_cellular),
                                contentDescription = "SIM signal",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = sim.operatorName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sim.networkType,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = sim.dbm?.let { "$it dBm" } ?: "Off",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Beautiful Radial Signal Strength Meter - Standard Professional M3 Style primary container card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Signal Strength",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Extract dBm and ASU levels
                    val dbmVal = metrics.dbm ?: -100
                    
                    // Normalise dBm (-120 dBm is terrible, -60 is excellent)
                    val progressRatio = ((dbmVal + 120).toFloat() / 60f).coerceIn(0f, 1f)
                    
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressRatio,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "Signal progress"
                    )

                    val gaugeTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                    val gaugeProgressBrush = Brush.sweepGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            SignalGreen, 
                            TechTurquoise
                        )
                    )

                    Canvas(modifier = Modifier.size(140.dp)) {
                        // Drawing static background arc
                        drawArc(
                            color = gaugeTrackColor,
                            startAngle = 140f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Drawing active signal gauge
                        drawArc(
                            brush = gaugeProgressBrush,
                            startAngle = 140f,
                            sweepAngle = animatedProgress * 260f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$dbmVal",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "dBm",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val signalStatus = when {
                            dbmVal >= -75 -> "Excellent"
                            dbmVal >= -90 -> "Good"
                            dbmVal >= -105 -> "Moderate"
                            else -> "Weak"
                        }
                        
                        val statusTextColor = when {
                            dbmVal >= -75 -> SignalGreen
                            dbmVal >= -90 -> TechTurquoise
                            dbmVal >= -105 -> SignalOrange
                            else -> SignalRed
                        }

                        Text(
                            text = signalStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active State", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                        Text(metrics.networkType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ASU Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                        Text("${metrics.asu ?: 0} ASU", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Diagnostic Grid
        Text(
            text = "Standard Telemetry",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "RSRP",
                    subtitle = "Signal Power",
                    value = metrics.rsrp?.let { "$it dBm" } ?: "N/A",
                    detail = "Excellent: > -85dBm\nNormal cellular gauge.",
                    statusColor = when (metrics.rsrp) {
                        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        in -85..0 -> SignalGreen
                        in -100..-86 -> TechTurquoise
                        else -> SignalRed
                    }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "RSRQ",
                    subtitle = "Signal Quality",
                    value = metrics.rsrq?.let { "$it dB" } ?: "N/A",
                    detail = "Good: > -10dB\nMeasures interference.",
                    statusColor = when (metrics.rsrq) {
                        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        in -10..0 -> SignalGreen
                        in -15..-11 -> SignalOrange
                        else -> SignalRed
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "SINR",
                    subtitle = "Signal-to-Noise Ratio",
                    value = metrics.sinr?.let { "$it dB" } ?: "N/A",
                    detail = "Excellent: > 15dB\nHigher means less noise.",
                    statusColor = when (metrics.sinr) {
                        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        in 16..50 -> SignalGreen
                        in 6..15 -> TechTurquoise
                        else -> SignalRed
                    }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "CELL IDENTITY",
                    subtitle = "Cell Identity",
                    value = metrics.cellId?.toString() ?: "No Connection",
                    detail = "TAC: ${metrics.tac ?: "N/A"}\nCell transmitter ID.",
                    statusColor = if (metrics.cellId != null) TechTurquoise else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    subtitle: String,
    value: String,
    detail: String,
    statusColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), lineHeight = 13.sp)
        }
    }
}

// ----------------------------------------------------
// SPEED TEST SCREEN WITH ANIMATED CANVAS SPEEDOMETER
// ----------------------------------------------------
@Composable
fun SpeedTestScreen(
    state: SpeedTestState,
    metrics: SignalMetrics,
    onStartTest: () -> Unit
) {
    val currentMbps = state.currentMbps
    val maxSpeed = 100.0 // logical meter gauge max threshold
    val currentAnglePercent = (currentMbps / maxSpeed).coerceIn(0.0, 1.0).toFloat()

    val animatedSweepRatio by animateFloatAsState(
        targetValue = currentAnglePercent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "Speed needle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Speed Test Portal",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Dialog, Mobitel, Hutch, SLT high-fidelity speed verification",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Live Speedometer Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            val gaugeTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val selectedTickColor = MaterialTheme.colorScheme.primary
            val unselectedTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            val gaugeProgressBrush = Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    SignalGreen
                ),
                center = Offset(110.dp.value, 110.dp.value),
                radius = 160.dp.value
            )

            // Gauge background rings and needle drawings
            Canvas(modifier = Modifier.size(220.dp)) {
                val startAngle = 135f
                val sweepArc = 270f
                
                // Track arc
                drawArc(
                    color = gaugeTrackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepArc,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Fill sweep active arc representing speed
                drawArc(
                    brush = gaugeProgressBrush,
                    startAngle = startAngle,
                    sweepAngle = animatedSweepRatio * sweepArc,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Drawing gauge tick indicators
                val ticksCount = 10
                for (i in 0..ticksCount) {
                    val angleDeg = startAngle + (sweepArc / ticksCount) * i
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val tickLength = 8.dp.toPx()
                    val paddingInner = 12.dp.toPx()
                    
                    val innerX = (size.width / 2f) + (size.width / 2f - paddingInner - tickLength) * cos(angleRad).toFloat()
                    val innerY = (size.height / 2f) + (size.height / 2f - paddingInner - tickLength) * sin(angleRad).toFloat()
                    
                    val outerX = (size.width / 2f) + (size.width / 2f - paddingInner) * cos(angleRad).toFloat()
                    val outerY = (size.height / 2f) + (size.height / 2f - paddingInner) * sin(angleRad).toFloat()

                    drawLine(
                        color = if (i.toFloat() / ticksCount <= animatedSweepRatio) selectedTickColor else unselectedTickColor,
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Text layout in speedometer center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                val phaseText = when (state.phase) {
                    SpeedTestPhase.IDLE -> "READY"
                    SpeedTestPhase.CONNECTING -> "CONNECTING"
                    SpeedTestPhase.DOWNLOAD -> "DOWNLOAD"
                    SpeedTestPhase.UPLOAD -> "UPLOAD"
                    SpeedTestPhase.COMPLETED -> "FINISHED"
                    SpeedTestPhase.FAILED -> "FAILED"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (state.phase == SpeedTestPhase.DOWNLOAD || state.phase == SpeedTestPhase.UPLOAD) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = phaseText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.phase == SpeedTestPhase.DOWNLOAD || state.phase == SpeedTestPhase.UPLOAD) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val speedDisplay = if (state.phase == SpeedTestPhase.DOWNLOAD || state.phase == SpeedTestPhase.UPLOAD) {
                    String.format(Locale.getDefault(), "%.1f", state.currentMbps)
                } else if (state.phase == SpeedTestPhase.COMPLETED) {
                    String.format(Locale.getDefault(), "%.1f", state.averageDownloadMbps)
                } else {
                    "0.0"
                }

                Text(
                    text = speedDisplay,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "Mbps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (state.phase == SpeedTestPhase.DOWNLOAD || state.phase == SpeedTestPhase.UPLOAD) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Progress: ${(state.progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Speed outcomes container (Download & Upload stats cards)
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Download Dashboard card
                val dlActive = state.phase == SpeedTestPhase.DOWNLOAD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dlActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (dlActive) 2.dp else 1.dp,
                        color = if (dlActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = R.drawable.ic_arrow_down), contentDescription = "Download", tint = SignalGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.averageDownloadMbps > 0) String.format(Locale.getDefault(), "%.2f Mbps", state.averageDownloadMbps) else "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (dlActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Upload Dashboard card
                val ulActive = state.phase == SpeedTestPhase.UPLOAD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ulActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (ulActive) 2.dp else 1.dp,
                        color = if (ulActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = R.drawable.ic_arrow_up), contentDescription = "Upload", tint = TechTurquoise, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.averageUploadMbps > 0) String.format(Locale.getDefault(), "%.2f Mbps", state.averageUploadMbps) else "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ulActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Speed Test Trigger button
            Button(
                onClick = onStartTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_button"), // Map unique search tags for test validations
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = state.phase == SpeedTestPhase.IDLE || state.phase == SpeedTestPhase.COMPLETED || state.phase == SpeedTestPhase.FAILED
            ) {
                val buttonLabel = if (state.phase == SpeedTestPhase.DOWNLOAD || state.phase == SpeedTestPhase.UPLOAD || state.phase == SpeedTestPhase.CONNECTING) {
                    "Speed Testing..."
                } else {
                    "START SPEED TEST"
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// DATA TRACKER SCREEN (Separate Mobile / WiFi meters)
// ----------------------------------------------------
@Composable
fun DataTrackerScreen(
    stats: UsageStats,
    context: Context,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Internet Data Usage",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Separate Monitor for WiFi & Mobile Data",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Dynamic System permission check card
        if (!stats.isPermissionGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Usage settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grant Usage Access (Optional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To see today's network data usage (WiFi/Mobile today), grant 'Usage Access' permission to this app. Alternatively, data used since last boot is displayed instead.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback info to settings home
                                try {
                                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(fallbackIntent)
                                } catch (err: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Open Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- WiFi usage panel ---
        UsageMetricPanel(
            title = "WiFi Usage",
            todayBytes = stats.wifiBytesToday,
            sinceBootBytes = stats.wifiBytesSinceBoot,
            iconId = R.drawable.ic_wifi_arc,
            accentColor = TechTurquoise,
            isPermissionGranted = stats.isPermissionGranted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Mobile usage panel ---
        UsageMetricPanel(
            title = "Mobile Usage",
            todayBytes = stats.mobileBytesToday,
            sinceBootBytes = stats.mobileBytesSinceBoot,
            iconId = R.drawable.ic_signal_cellular,
            accentColor = MaterialTheme.colorScheme.primary,
            isPermissionGranted = stats.isPermissionGranted
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Manual refresh panel
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh usage logs")
            Spacer(modifier = Modifier.width(8.dp))
            Text("REFRESH", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun UsageMetricPanel(
    title: String,
    todayBytes: Long,
    sinceBootBytes: Long,
    iconId: Int,
    accentColor: Color,
    isPermissionGranted: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(id = iconId), contentDescription = "Usage icon", tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Today statistic block (requires permission)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Today's Usage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPermissionGranted) DataUsageHelper.formatBytes(todayBytes) else "Permission Required",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPermissionGranted) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Boot statistic block (zero permission failsafe!)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Since Boot", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DataUsageHelper.formatBytes(sinceBootBytes),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge indicator
            val percentage = if (isPermissionGranted) {
                // assume daily 2GB target limit (arbitrary representation for visually pleasing bars)
                val target = 2_147_483_648L 
                (todayBytes.toFloat() / target.toFloat()).coerceIn(0.01f, 1f)
            } else {
                val targetBoot = 10_737_418_240L // 10GB since boot target
                (sinceBootBytes.toFloat() / targetBoot.toFloat()).coerceIn(0.01f, 1f)
            }

            LinearProgressIndicator(
                progress = percentage,
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "0 B", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Text(
                    text = if (isPermissionGranted) "Target: 2 GB" else "Target Boot: 10 GB",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ----------------------------------------------------
// HISTORICAL STATISTICS SCREEN
// ----------------------------------------------------
@Composable
fun SunIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier.size(24.dp).testTag("sun_icon")) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 4.5f
        
        // Draw glow circle
        drawCircle(
            color = tint.copy(alpha = 0.15f),
            radius = radius * 1.8f,
            center = center
        )
        // Draw core
        drawCircle(
            color = tint,
            radius = radius,
            center = center
        )
        // Rays
        val rayCount = 8
        val rayLength = size.width / 6f
        val rayThickness = 2.dp.toPx()
        for (i in 0 until rayCount) {
            val angle = i * (2 * Math.PI / rayCount)
            val startX = (center.x + (radius + 2.5.dp.toPx()) * cos(angle)).toFloat()
            val startY = (center.y + (radius + 2.5.dp.toPx()) * sin(angle)).toFloat()
            val endX = (center.x + (radius + 2.5.dp.toPx() + rayLength) * cos(angle)).toFloat()
            val endY = (center.y + (radius + 2.5.dp.toPx() + rayLength) * sin(angle)).toFloat()
            drawLine(
                color = tint,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = rayThickness,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun MoonIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier.size(24.dp).testTag("moon_icon")) {
        val width = size.width
        val height = size.height
        
        // Draw crescent moon path
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.70f, height * 0.15f)
            cubicTo(
                width * 0.35f, height * 0.15f,
                width * 0.20f, height * 0.45f,
                width * 0.35f, height * 0.75f
            )
            cubicTo(
                width * 0.50f, height * 0.90f,
                width * 0.70f, height * 0.85f,
                width * 0.75f, height * 0.80f
            )
            cubicTo(
                width * 0.52f, height * 0.75f,
                width * 0.48f, height * 0.50f,
                width * 0.70f, height * 0.30f
            )
            close()
        }
        
        // Draw lunar glow
        drawCircle(
            color = tint.copy(alpha = 0.08f),
            radius = width * 0.4f,
            center = Offset(width * 0.45f, height * 0.5f)
        )
        
        drawPath(
            path = path,
            color = tint
        )
    }
}

@Composable
fun SystemThemeIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier.size(24.dp).testTag("system_icon")) {
        val w = size.width
        val h = size.height
        
        // Draw monitor frame
        val rectPath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.15f,
                    top = h * 0.2f,
                    right = w * 0.85f,
                    bottom = h * 0.7f,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
            )
        }
        
        drawPath(
            path = rectPath,
            color = tint,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw left-half split
        val splitPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, h * 0.2f)
            lineTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.15f, h * 0.7f)
            close()
        }
        drawPath(
            path = splitPath,
            color = tint
        )
        
        // Base / stand of system screen
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.7f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.3f, h * 0.85f),
            end = Offset(w * 0.7f, h * 0.85f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick)
            .testTag("theme_card_" + title.replace(" ", "_").lowercase())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon(contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryScreen(
    speedTests: List<SpeedTestRecord>,
    dataUsage: List<DataUsageRecord>,
    currentTheme: AppThemeMode,
    onThemeSelect: (AppThemeMode) -> Unit,
    onClearAll: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Speed Test, 1 = Daily Usage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // App Theme Selector Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Appearance option",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "App Theme & Appearance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personalize app appearance: Force light theme, dark theme, or sync with standard Android system setting.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionCard(
                        title = "Light Mode",
                        isSelected = currentTheme == AppThemeMode.LIGHT,
                        onClick = { onThemeSelect(AppThemeMode.LIGHT) },
                        icon = { tint -> SunIcon(tint = tint) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Dark Mode",
                        isSelected = currentTheme == AppThemeMode.DARK,
                        onClick = { onThemeSelect(AppThemeMode.DARK) },
                        icon = { tint -> MoonIcon(tint = tint) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "System Default",
                        isSelected = currentTheme == AppThemeMode.SYSTEM,
                        onClick = { onThemeSelect(AppThemeMode.SYSTEM) },
                        icon = { tint -> SystemThemeIcon(tint = tint) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Mode Selector Tab (Speed tests vs Daily registers) - Segmented Control M3 style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Speed Runs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Usage History",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clearing tool header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedSubTab == 0) "Speed Test Runs (${speedTests.size} runs)" else "Daily Usage Logs (${dataUsage.size} days)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (selectedSubTab == 0 && speedTests.isNotEmpty()) {
                Text(
                    text = "Clear Logs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onClearAll() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedSubTab == 0) {
            // Speed Test History List view
            if (speedTests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_history_log),
                            contentDescription = "No speed tests recorded yet",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No speed tests recorded yet\n(Run a Speed Test to see history here)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(speedTests) { log ->
                        SpeedTestRowItem(log)
                    }
                }
            }
        } else {
            // Daily usage history registers
            if (dataUsage.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chart_analytic),
                            contentDescription = "No data logs collected yet",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No usage logs recorded yet\n(Usage logs will accumulate daily with Usage Access permissions)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dataUsage) { log ->
                        DailyUsageCardItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedTestRowItem(log: SpeedTestRecord) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val dateDisplay = formatter.format(Date(log.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Network type badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = log.networkType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = dateDisplay,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DOWNLOAD", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f Mbps", log.downloadSpeedMbps),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = SignalGreen
                    )
                }

                Column {
                    Text("UPLOAD", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f Mbps", log.uploadSpeedMbps),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TechTurquoise
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("OPERATOR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(
                        text = log.operatorName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DailyUsageCardItem(log: DataUsageRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = log.dateString, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                val totalBytes = log.mobileBytes + log.wifiBytes
                Text(
                    text = "Total: ${DataUsageHelper.formatBytes(totalBytes)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mobile: ${DataUsageHelper.formatBytes(log.mobileBytes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TechTurquoise))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WiFi: ${DataUsageHelper.formatBytes(log.wifiBytes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            }
        }
    }
}
