package com.nanomks.elamal.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.util.ElamalSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppState.LockScreen(activity: FragmentActivity) {
    val context = LocalContext.current
    var pinInput by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    val expectedPinLength = remember(savedPin) {
        ElamalSecurity.getStoredPinLengthHint(prefs)
    }

    fun attemptUnlock(candidatePin: String, isAutoCheck: Boolean = false) {
        if (isVerifying) return
        if (isAutoCheck && expectedPinLength in 4..8 && candidatePin.length != expectedPinLength) {
            return
        }

        val now = System.currentTimeMillis()
        if (now < lockoutUntil) {
            val remSec = ((lockoutUntil - now) / 1000L).coerceAtLeast(1L)
            pinError = "تم تجاوز 7 محاولات! انتظر $remSec ثانية."
            return
        }

        isVerifying = true
        viewModelScope.launch {
            val matched = withContext(Dispatchers.Default) {
                ElamalSecurity.verifyPin(candidatePin, savedPin)
            }
            isVerifying = false
            if (matched) {
                if (!savedPin.startsWith("PBKDF2_V2:")) {
                    val hashed = withContext(Dispatchers.Default) {
                        ElamalSecurity.hashPinPbkdf2(candidatePin)
                    }
                    savedPin = hashed
                    prefs.edit()
                        .putString("security_pin", hashed)
                        .putInt("security_pin_length", candidatePin.length)
                        .apply()
                } else if (expectedPinLength == 0) {
                    prefs.edit().putInt("security_pin_length", candidatePin.length).apply()
                }
                failedPinAttempts = 0
                prefs.edit().putInt("failed_pin_attempts", 0).apply()
                isUnlocked = true
            } else if (!isAutoCheck || (expectedPinLength in 4..8 && candidatePin.length == expectedPinLength)) {
                failedPinAttempts += 1
                prefs.edit().putInt("failed_pin_attempts", failedPinAttempts).apply()
                pinInput = ""
                if (failedPinAttempts >= 7) {
                    lockoutUntil = now + 30_000L
                    prefs.edit().putLong("lockout_until", lockoutUntil).apply()
                    pinError = "7 محاولات خاطئة! تم تأخير الفتح 30 ثانية."
                } else {
                    pinError = "رمز القفل غير صحيح ($failedPinAttempts / 7)"
                }
            }
        }
    }

    fun triggerBiometric() {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                failedPinAttempts = 0
                prefs.edit().putInt("failed_pin_attempts", 0).apply()
                isUnlocked = true
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("فتح تطبيق العمل بالبصمة")
            .setSubtitle("استخدم البصمة لفتح القفل بسرعة")
            .setNegativeButtonText("إدخال الرمز")
            .build()
        prompt.authenticate(info)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(primaryCustom.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "قفل الأمان",
                        modifier = Modifier.size(38.dp),
                        tint = primaryCustom
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "تطبيق «العمل» محمي",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "أدخل رمز القفل من اللوحة أدناه أو استخدم البصمة",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(26.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val targetDots = if (expectedPinLength in 4..8) expectedPinLength else maxOf(4, pinInput.length.coerceAtMost(8))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until targetDots) {
                            val isFilled = i < pinInput.length
                            val dotColor by animateColorAsState(
                                targetValue = if (isFilled) primaryCustom else Color.Transparent,
                                label = "PinDot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(
                                        width = 2.dp,
                                        color = if (isFilled) primaryCustom else colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                if (pinError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = pinError,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )

                    rows.forEach { rowDigits ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowDigits.forEach { digit ->
                                PinPadButton(
                                    label = digit,
                                    surfaceColor = colorScheme.surface,
                                    contentColor = colorScheme.onSurface,
                                    onClick = {
                                        if (pinInput.length < 8 && !isVerifying) {
                                            pinError = ""
                                            val updated = pinInput + digit
                                            pinInput = updated
                                            if (updated.length >= 4) {
                                                attemptUnlock(updated, isAutoCheck = true)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(primaryCustom.copy(alpha = 0.14f))
                                .clickable { triggerBiometric() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "فتح بالبصمة",
                                tint = primaryCustom,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        PinPadButton(
                            label = "0",
                            surfaceColor = colorScheme.surface,
                            contentColor = colorScheme.onSurface,
                            onClick = {
                                if (pinInput.length < 8 && !isVerifying) {
                                    pinError = ""
                                    val updated = pinInput + "0"
                                    pinInput = updated
                                    if (updated.length >= 4) {
                                        attemptUnlock(updated, isAutoCheck = true)
                                    }
                                }
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceVariant)
                                .clickable {
                                    if (pinInput.isNotEmpty() && !isVerifying) {
                                        pinInput = pinInput.dropLast(1)
                                        pinError = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "مسح رقم",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { attemptUnlock(pinInput, isAutoCheck = false) },
                        enabled = pinInput.length >= 4 && !isVerifying,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryCustom)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "تأكيد الرمز", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح القفل", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PinPadButton(
    label: String,
    surfaceColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = surfaceColor,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
