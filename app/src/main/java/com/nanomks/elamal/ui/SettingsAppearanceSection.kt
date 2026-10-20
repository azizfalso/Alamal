package com.nanomks.elamal.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppState.SettingsAppearanceSection() {
    val context = LocalContext.current

    // Section 3: Appearance, Colors & Icon
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = primaryCustom)
                Text("المظهر، الخط، وأيقونة التطبيق", fontWeight = FontWeight.Bold, fontSize = 15.5f.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = themeMode == 0, onClick = { themeMode = 0; prefs.edit().putInt("theme_mode", 0).apply() }, label = { Text("تلقائي") })
                FilterChip(selected = themeMode == 1, onClick = { themeMode = 1; prefs.edit().putInt("theme_mode", 1).apply() }, label = { Text("فاتح (للـ PDF)") })
                FilterChip(selected = themeMode == 2, onClick = { themeMode = 2; prefs.edit().putInt("theme_mode", 2).apply() }, label = { Text("داكن (للـ PDF)") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { appColorHex = "#059669"; prefs.edit().putString("app_color_hex", "#059669").apply() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("زمردي") }

                Button(
                    onClick = { appColorHex = "#2563EB"; prefs.edit().putString("app_color_hex", "#2563EB").apply() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("ملكي") }

                Button(
                    onClick = { appColorHex = "#7C3AED"; prefs.edit().putString("app_color_hex", "#7C3AED").apply() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("بنفسجي") }
            }

            Text("حجم الخط: ${String.format(Locale.US, "%.1fx", fontScale)}")
            Slider(
                value = fontScale,
                onValueChange = {
                    fontScale = it
                    prefs.edit().putFloat("font_scale", it).apply()
                },
                valueRange = 0.85f..1.35f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ألوان النظام (Material You)")
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        prefs.edit().putBoolean("dynamic_color", it).apply()
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    val pm = context.packageManager
                    val def = ComponentName(context, "com.nanomks.elamal.LauncherDefault")
                    val alt = ComponentName(context, "com.nanomks.elamal.LauncherAlt")
                    val isDefEnabled = pm.getComponentEnabledSetting(def) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    if (isDefEnabled) {
                        pm.setComponentEnabledSetting(alt, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                        pm.setComponentEnabledSetting(def, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                        Toast.makeText(context, "تم التبديل إلى الأيقونة الزرقاء ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        pm.setComponentEnabledSetting(def, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                        pm.setComponentEnabledSetting(alt, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                        Toast.makeText(context, "تم التبديل إلى الأيقونة الخضراء ✓", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تبديل أيقونة التطبيق (أخضر / أزرق)")
            }
        }
    }
}
