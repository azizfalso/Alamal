package com.example.worktracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordSheet(
    sheetType: String, // "work", "remote", "credit"
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String, isCredit: Boolean, isRemote: Boolean) -> Unit
) {
    val title = when (sheetType) {
        "work" -> "إضافة يوم عمل"
        "remote" -> "إضافة عمل عن بعد"
        else -> "إضافة كريدي (سلفة)"
    }
    
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("المبلغ (DH)", color = Color.Gray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("ملاحظة (اختياري)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("إلغاء", color = Color.White)
                }
                
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        val isCredit = sheetType == "credit"
                        val isRemote = sheetType == "remote"
                        // إذا كان كريدي، نحفظه بالسالب
                        val finalAmount = if (isCredit) -amount else amount
                        
                        onSave(finalAmount, noteInput, isCredit, isRemote)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)), // ذهبي
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
