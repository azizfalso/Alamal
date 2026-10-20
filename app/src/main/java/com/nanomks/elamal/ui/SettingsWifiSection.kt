package com.nanomks.elamal.ui

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
import androidx.lifecycle.viewModelScope
import com.nanomks.elamal.util.ElamalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppState.SettingsWifiSection(newSsidInputState: MutableState<String>) {
    val context = LocalContext.current
    var newSsidInput by newSsidInputState
    val ssidList = workSsids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    // Section 1: Root Wi-Fi Tracking inside Settings
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = primaryCustom)
                Text("تتبع شبكات العمل بالرووت (بدون GPS)", fontWeight = FontWeight.Bold, fontSize = 15.5f.sp)
            }
            Text(
                text = "الشبكة المقروءة بالرووت الآن: ${detectedRootSsid ?: "اضغط فحص لطلب صلاحية الرووت (su)"}",
                fontSize = 13.sp,
                color = if (detectedRootSsid != null) Color(0xFF059669) else colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModelScope.launch {
                            val ssid = withContext(Dispatchers.IO) {
                                ElamalUtils.pollAndMergeRootWifi(context, dao)
                            }
                            detectedRootSsid = ssid
                            if (ssid != null) {
                                Toast.makeText(context, "تم قراءة الشبكة عبر الرووت: $ssid ✓", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تأكد من منح صلاحية الرووت للتطبيق في Magisk/KernelSU", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("فحص بالرووت الآن")
                }

                OutlinedButton(
                    onClick = {
                        val current = detectedRootSsid
                        if (current != null && ssidList.size < 6 && !ssidList.contains(current)) {
                            val updated = (ssidList + current).joinToString(",")
                            workSsids = updated
                            prefs.edit().putString("work_ssids", updated).apply()
                            viewModelScope.launch(Dispatchers.IO) { ElamalUtils.pollAndMergeRootWifi(context, dao) }
                        } else {
                            Toast.makeText(context, "افحص الشبكة بالرووت أولاً أو أضف اسمها بالأسفل", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إضافة الشبكة الحالية")
                }
            }

            Text("قائمة شبكات العمل (${ssidList.size} / 6 شبكات لدعم الترددين 2.4G و 5G):", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ssidList.forEach { itemSsid ->
                    InputChip(
                        selected = true,
                        onClick = {
                            val updated = ssidList.filter { it != itemSsid }.joinToString(",")
                            workSsids = updated
                            prefs.edit().putString("work_ssids", updated).apply()
                        },
                        label = { Text(itemSsid) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(15.dp)) }
                    )
                }
            }

            if (ssidList.size < 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSsidInput,
                        onValueChange = { newSsidInput = it },
                        label = { Text("أضف اسم شبكة عمل يدوياً (SSID)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            val clean = newSsidInput.trim()
                            if (clean.isNotEmpty() && ssidList.size < 6 && !ssidList.contains(clean)) {
                                val updated = (ssidList + clean).joinToString(",")
                                workSsids = updated
                                prefs.edit().putString("work_ssids", updated).apply()
                                newSsidInput = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("إضافة") }
                }
            }
        }
    }
}
