package com.nanomks.elamal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nanomks.elamal.data.ElamalDatabase
import com.nanomks.elamal.ui.ElamalRootScreen
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiDebounceJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFlagSecureFromPrefs()
        requestNotificationPermissionIfNeeded()
        registerEconomicalWifiCallback()
        ElamalUtils.syncPeriodicWifiPollSchedule(this)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ElamalRootScreen(activity = this)
            }
        }
    }

    fun applyFlagSecureFromPrefs() {
        val prefs = getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        val hasPin = !(prefs.getString("security_pin", "").isNullOrBlank())
        val flagSecureEnabled = prefs.getBoolean("flag_secure_enabled", hasPin)
        if (flagSecureEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun registerEconomicalWifiCallback() {
        val prefs = getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("root_wifi_enabled", true)) return

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val dao = ElamalDatabase.getInstance(this).dao()
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scheduleDebouncedWifiPoll(dao)
            }

            override fun onLost(network: Network) {
                scheduleDebouncedWifiPoll(dao)
            }
        }
        networkCallback = cb
        runCatching { cm.registerNetworkCallback(req, cb) }
    }

    private fun scheduleDebouncedWifiPoll(dao: com.nanomks.elamal.data.ElamalDao) {
        val prefs = getSharedPreferences("elamal_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("root_wifi_enabled", true)) return
        if (prefs.getString("work_ssids", "").isNullOrBlank()) return

        wifiDebounceJob?.cancel()
        wifiDebounceJob = lifecycleScope.launch(Dispatchers.IO) {
            // Wait 4 seconds so unstable Wi-Fi connections don't spawn repeated root processes
            delay(4_000L)
            ElamalUtils.pollAndMergeRootWifi(applicationContext, dao)
        }
    }

    override fun onStop() {
        super.onStop()
        // Clean up temporary shared PDF/CSV files older than 10 minutes when leaving app
        PdfReportGenerator.cleanupCachedReports(applicationContext, maxAgeMillis = 10 * 60_000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiDebounceJob?.cancel()
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
    }
}
