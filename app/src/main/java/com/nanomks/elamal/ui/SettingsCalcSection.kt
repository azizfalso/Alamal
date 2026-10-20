package com.nanomks.elamal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanomks.elamal.widget.ElamalWidgetReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppState.SettingsCalcSection() {
    val context = LocalContext.current

    // Section 2: Calculation & Widget Settings
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = primaryCustom)
                Text("الحساب، التنبيهات، والودجت", fontWeight = FontWeight.Bold, fontSize = 15.5f.sp)
            }

            var rateInput by remember(hourlyRate) { mutableStateOf(hourlyRate.toString()) }
            OutlinedTextField(
                value = rateInput,
                onValueChange = {
                    rateInput = it
                    it.toDoubleOrNull()?.let { r ->
                        hourlyRate = r
                        prefs.edit().putFloat("hourly_rate", r.toFloat()).apply()
                    }
                },
                label = { Text("سعر الساعة الافتراضي (DH) - لا يغير الأرشيف") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Text("تنبيه نسيان الخروج: بعد $forgotAlarmHours ساعة (من 0 إلى 48)", fontWeight = FontWeight.Medium)
            Slider(
                value = forgotAlarmHours.toFloat(),
                onValueChange = {
                    forgotAlarmHours = it.toInt()
                    prefs.edit().putInt("forgot_alarm_hours", forgotAlarmHours).apply()
                },
                valueRange = 0f..48f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إظهار البحث والفلتر في الدفتر")
                Switch(
                    checked = searchFilterEnabled,
                    onCheckedChange = {
                        searchFilterEnabled = it
                        prefs.edit().putBoolean("search_filter_enabled", it).apply()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تفعيل الودجت (الدخول الجاري لا يضيع)")
                Switch(
                    checked = widgetEnabled,
                    onCheckedChange = {
                        widgetEnabled = it
                        prefs.edit().putBoolean("widget_enabled", it).apply()
                        ElamalWidgetReceiver.refreshAllWidgets(context)
                    }
                )
            }
        }
    }
}
