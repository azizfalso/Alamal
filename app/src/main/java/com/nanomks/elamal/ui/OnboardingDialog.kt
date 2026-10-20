package com.nanomks.elamal.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppState.OnboardingDialog() {
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(20.dp),
        title = { Text("مرحباً بك في «العمل» 👋", fontWeight = FontWeight.Bold) },
        text = {
            Text("لحماية ساعات عملك ودفعاتك تلقائياً، اختر مجلداً على هاتفك لحفظ النسخ الاحتياطية التلقائية (عبر SAF).")
        },
        confirmButton = {
            Button(
                onClick = { folderPicker.launch(null) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("اختيار مجلد النسخ الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                prefs.edit().putBoolean("first_run_done", true).apply()
                firstRunDone = true
            }) { Text("لاحقاً من الإعدادات") }
        }
    )
}



