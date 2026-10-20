package com.nanomks.elamal.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nanomks.elamal.data.*
import com.nanomks.elamal.data.ElamalDao
import com.nanomks.elamal.util.BackupSafManager
import com.nanomks.elamal.util.ElamalUtils
import com.nanomks.elamal.util.PdfReportGenerator
import com.nanomks.elamal.widget.ElamalWidgetReceiver
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppState.LockScreen() {
            var pinInput by remember { mutableStateOf("") }
            var pinError by remember { mutableStateOf("") }

            Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).background(primaryCustom.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(42.dp), tint = primaryCustom)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("تطبيق «العمل» مقفل", fontSize = (22 * fontScale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("رمز القفل (4 إلى 8 أرقام)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (pinError.isNotBlank()) {
                        Text(pinError, color = Color(0xFFEF4444), modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now < lockoutUntil) {
                                    val remSec = (lockoutUntil - now) / 1000L
                                    pinError = "تم تجاوز 7 محاولات! انتظر $remSec ثانية."
                                    return@Button
                                }
                                if (pinInput == savedPin) {
                                    failedPinAttempts = 0
                                    prefs.edit().putInt("failed_pin_attempts", 0).apply()
                                    isUnlocked = true
                                } else {
                                    failedPinAttempts += 1
                                    prefs.edit().putInt("failed_pin_attempts", failedPinAttempts).apply()
                                    if (failedPinAttempts >= 7) {
                                        lockoutUntil = now + 30_000L
                                        prefs.edit().putLong("lockout_until", lockoutUntil).apply()
                                        pinError = "7 محاولات خاطئة! تم تأخير الفتح 30 ثانية."
                                    } else {
                                        pinError = "رمز خاطئ ($failedPinAttempts / 7)"
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("فتح القفل") }

                        OutlinedButton(
                            onClick = {
                                val executor = ContextCompat.getMainExecutor(context)
                                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        isUnlocked = true
                                    }
                                })
                                val info = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("فتح تطبيق العمل بالبصمة")
                                    .setSubtitle("استخدم البصمة كاختصار لفتح القفل")
                                    .setNegativeButtonText("إلغاء")
                                    .build()
                                prompt.authenticate(info)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("البصمة")
                        }
                    }
                }
            }
}
