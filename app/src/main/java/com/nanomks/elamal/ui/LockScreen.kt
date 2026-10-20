package com.nanomks.elamal.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nanomks.elamal.util.ElamalSecurity

@Composable
fun AppState.LockScreen(activity: FragmentActivity) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(primaryCustom.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(42.dp), tint = primaryCustom)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("تطبيق «العمل» محمي", fontSize = (22 * fontScale).sp, fontWeight = FontWeight.ExtraBold)
            Text("أدخل رمز القفل أو استخدم البصمة للمتابعة", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(22.dp))

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
                Text(pinError, color = Color(0xFFEF4444), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now < lockoutUntil) {
                            val remSec = (lockoutUntil - now) / 1000L
                            pinError = "تم تجاوز 7 محاولات! انتظر $remSec ثانية."
                            return@Button
                        }
                        if (ElamalSecurity.verifyPin(pinInput, savedPin)) {
                            // Auto-upgrade legacy PIN to salted PBKDF2 hash
                            if (!savedPin.startsWith("PBKDF2_V1:")) {
                                val hashed = ElamalSecurity.hashPinPbkdf2(pinInput)
                                savedPin = hashed
                                prefs.edit().putString("security_pin", hashed).apply()
                            }
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) { Text("فتح القفل", fontWeight = FontWeight.Bold) }

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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("البصمة")
                }
            }
        }
    }
}



