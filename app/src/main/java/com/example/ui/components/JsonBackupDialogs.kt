package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.PartyEntity
import com.example.data.network.NetworkDevice
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.util.DataJsonHelper

@Composable
fun PartyJsonBackupDialog(
    parties: List<PartyEntity>,
    onImport: (List<PartyEntity>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Export, 1: Import
    val exportedJson = remember(parties) { DataJsonHelper.exportPartiesToJson(parties) }
    var inputJson by remember { mutableStateOf("") }
    var parsedPreview by remember { mutableStateOf<List<PartyEntity>?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(20.dp)),
            color = CyberDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MikroTikPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("تصدير واستيراد العملاء والبقالات", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("نقل ومشاركة بيانات الوكلاء كملف JSON", color = TextSecondaryDark, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabPill(
                        label = "تصدير (${parties.size} طرف)",
                        icon = Icons.Default.FileUpload,
                        isSelected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        label = "استيراد JSON",
                        icon = Icons.Default.FileDownload,
                        isSelected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    // Export Tab
                    Text("بيانات العملاء والبقالات الحالية المنسقة:", color = TextSecondaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(exportedJson, color = TextPrimaryDark, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Parties JSON", exportedJson))
                                Toast.makeText(context, "تم نسخ بيانات العملاء إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية لعملاء شبكة سام ميكروتك")
                                    putExtra(Intent.EXTRA_TEXT, exportedJson)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة بيانات العملاء"))
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", color = MikroTikCyan)
                        }
                    }
                } else {
                    // Import Tab
                    OutlinedTextField(
                        value = inputJson,
                        onValueChange = {
                            inputJson = it
                            parseError = null
                            parsedPreview = null
                            if (it.isNotBlank()) {
                                try {
                                    val list = DataJsonHelper.parsePartiesFromJson(it)
                                    parsedPreview = list
                                } catch (e: Exception) {
                                    parseError = "صيغة JSON غير صحيحة: ${e.localizedMessage}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("الصق كود JSON للعملاء هنا...", color = TextMutedDark, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MikroTikCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedContainerColor = CyberDarkCardElevated,
                            unfocusedContainerColor = CyberDarkCardElevated
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    parseError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    parsedPreview?.let { preview ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "تم التعرف على (${preview.size}) عميل/بقالة جاهزة للإضافة",
                            color = StatusOnline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            parsedPreview?.let {
                                if (it.isNotEmpty()) {
                                    onImport(it)
                                    Toast.makeText(context, "تم استيراد ${it.size} عميل بنجاح!", Toast.LENGTH_SHORT).show()
                                    onDismissRequest()
                                }
                            }
                        },
                        enabled = !parsedPreview.isNullOrEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOnline)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعتماد وإضافة (${parsedPreview?.size ?: 0}) عميل للقاعدة")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Text("إغلاق", color = TextSecondaryDark)
                }
            }
        }
    }
}

@Composable
fun DeviceJsonBackupDialog(
    devices: List<NetworkDevice>,
    onImport: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }
    var inputJson by remember { mutableStateOf("") }
    var parsedCount by remember { mutableStateOf<Int?>(null) }
    var detectedNetworkName by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    val exportedJson = remember(devices) {
        DataJsonHelper.exportDevicesToJson(devices)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(20.dp)),
            color = CyberDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MikroTikPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("تصدير واستيراد أجهزة الشبكة", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("حفظ ونقل بيانات السيكتورات والروابط اللاسلكية", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabPill(
                        label = "تصدير (${devices.size} جهاز)",
                        icon = Icons.Default.FileUpload,
                        isSelected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        label = "استيراد JSON",
                        icon = Icons.Default.FileDownload,
                        isSelected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(exportedJson, color = TextPrimaryDark, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Devices JSON", exportedJson))
                                Toast.makeText(context, "تم نسخ أجهزة الشبكة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "خريطة وأجهزة شبكة سام ميكروتك")
                                    putExtra(Intent.EXTRA_TEXT, exportedJson)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة أجهزة الشبكة"))
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", color = MikroTikCyan)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = inputJson,
                        onValueChange = {
                            inputJson = it
                            parseError = null
                            parsedCount = null
                            detectedNetworkName = null
                            if (it.isNotBlank()) {
                                try {
                                    val res = DataJsonHelper.parseDevicesFromJson(it)
                                    parsedCount = res.devices.size
                                    detectedNetworkName = res.networkName
                                } catch (e: Exception) {
                                    parseError = "صيغة JSON غير صحيحة: ${e.localizedMessage}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("الصق كود JSON لأجهزة الشبكة هنا...", color = TextMutedDark, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MikroTikCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedContainerColor = CyberDarkCardElevated,
                            unfocusedContainerColor = CyberDarkCardElevated
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    parseError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    parsedCount?.let { count ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "تم التعرف على ($count) جهاز بث ومحطة${if (detectedNetworkName != null) " تابعة لـ [$detectedNetworkName]" else ""}",
                            color = StatusOnline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!inputJson.isBlank() && parsedCount != null) {
                                onImport(inputJson)
                                Toast.makeText(context, "تم استيراد الأجهزة بنجاح!", Toast.LENGTH_SHORT).show()
                                onDismissRequest()
                            }
                        },
                        enabled = parsedCount != null && parsedCount!! > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOnline)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعتماد وإضافة الأجهزة")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Text("إغلاق", color = TextSecondaryDark)
                }
            }
        }
    }
}

@Composable
fun PurchaseJsonBackupDialog(
    onImportDrafts: (List<DataJsonHelper.ImportedPurchaseDraft>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var inputJson by remember { mutableStateOf("") }
    var parsedDrafts by remember { mutableStateOf<List<DataJsonHelper.ImportedPurchaseDraft>?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(20.dp)),
            color = CyberDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MikroTikPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("استيراد فواتير المشتريات كمسودات", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("استيراد فواتير الموردين ومشتريات الأصول لمراجعتها", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputJson,
                    onValueChange = {
                        inputJson = it
                        parseError = null
                        parsedDrafts = null
                        if (it.isNotBlank()) {
                            try {
                                val drafts = DataJsonHelper.parsePurchasesFromJson(it)
                                parsedDrafts = drafts
                            } catch (e: Exception) {
                                parseError = "صيغة JSON غير صحيحة: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = {
                        Text(
                            "الصق كود JSON لفواتير المشتريات هنا...\n[\n  {\n    \"vendorName\": \"شركة ستارلينك اليمن\",\n    \"currency\": \"USD\",\n    \"items\": [\n      {\"description\": \"اشتراك انترنت فضائي\", \"accountCode\": \"5101\", \"quantity\": 1, \"unitPriceMinor\": 12000}\n    ]\n  }\n]",
                            color = TextMutedDark,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikroTikCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedContainerColor = CyberDarkCardElevated,
                        unfocusedContainerColor = CyberDarkCardElevated
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                parseError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                parsedDrafts?.let { drafts ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("تم التعرف على (${drafts.size}) فواتير مشتريات جاهزة للمراجعة", color = StatusOnline, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        parsedDrafts?.let {
                            if (it.isNotEmpty()) {
                                onImportDrafts(it)
                                Toast.makeText(context, "تم استيراد ${it.size} فواتير مسودة بنجاح!", Toast.LENGTH_SHORT).show()
                                onDismissRequest()
                            }
                        }
                    },
                    enabled = !parsedDrafts.isNullOrEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOnline)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحويل إلى مسودات مشتريات للمراجعة")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Text("إغلاق", color = TextSecondaryDark)
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MikroTikPrimary else CyberDarkCardElevated)
            .border(BorderStroke(1.dp, if (isSelected) MikroTikCyan else CyberBorder), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondaryDark,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                color = if (isSelected) Color.White else TextSecondaryDark,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
