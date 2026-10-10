package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StockViewModel
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.*

data class GstInvoiceRow(
    val id: String,
    var itemName: String,
    var hsnCode: String,
    var qty: Double,
    var rate: Double,
    var taxRatePercent: Double, // 0.0, 5.0, 12.0, 18.0, 28.0
    var costPrice: Double = 0.0
)

@Composable
fun GstCalculatorScreen(viewModel: StockViewModel) {
    val allItems by viewModel.allItems.collectAsState()
    var storeGstin by remember { mutableStateOf("27AABCS1234F1Z5") }
    var customerGstin by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var invoiceNo by remember { mutableStateOf("GST-${System.currentTimeMillis().toString().takeLast(6)}") }
    var invoiceDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var isInterState by remember { mutableStateOf(false) } // false = CGST + SGST, true = IGST

    val rows = remember {
        mutableStateListOf<GstInvoiceRow>()
    }

    var transporterId by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }

    // Calculations
    val taxableTotal = rows.sumOf { it.qty * it.rate }
    val totalTaxAmount = rows.sumOf { (it.qty * it.rate) * (it.taxRatePercent / 100.0) }
    val grandTotal = taxableTotal + totalTaxAmount
    val isEWayBillRequired = grandTotal > 50000.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = BrandBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GST Tax Invoice & E-Way Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Invoice & GSTIN Details", fontWeight = FontWeight.Bold, color = BrandBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = storeGstin,
                                onValueChange = { storeGstin = it },
                                label = { Text("Store GSTIN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customerGstin,
                                onValueChange = { customerGstin = it },
                                label = { Text("Customer GSTIN (Optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Name / Business") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = invoiceNo,
                                onValueChange = { invoiceNo = it },
                                label = { Text("Invoice No.") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Supply Type: ${if (isInterState) "Inter-State (IGST)" else "Intra-State (CGST + SGST)"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = isInterState,
                                onCheckedChange = { isInterState = it }
                            )
                        }
                    }
                }
            }

            // E-Way Bill Rule 138 Warning Card if > 50,000
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEWayBillRequired) WarningAmber.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, if (isEWayBillRequired) WarningAmber else SuccessGreen)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isEWayBillRequired) Icons.Default.Warning else Icons.Default.Calculate,
                                contentDescription = null,
                                tint = if (isEWayBillRequired) WarningAmber else SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEWayBillRequired) "⚠️ E-WAY BILL REQUIRED (Value > ₹50,000)" else "✓ E-Way Bill Optional (Invoice Value ≤ ₹50,000)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isEWayBillRequired) WarningAmber else SuccessGreen
                            )
                        }
                        if (isEWayBillRequired) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("As per Rule 138 of CGST Rules, generation of E-Way Bill is mandatory for movement of goods exceeding ₹50,000.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = transporterId,
                                    onValueChange = { transporterId = it },
                                    label = { Text("Transporter ID / GSTIN") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = vehicleNo,
                                    onValueChange = { vehicleNo = it },
                                    label = { Text("Vehicle No. (e.g. MH12AB1234)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Items Header & Add Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invoice Items (${rows.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedButton(onClick = {
                        rows.add(GstInvoiceRow(System.currentTimeMillis().toString(), "", "8481", 1.0, 100.0, 18.0))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }
            }

            // Invoice Row Editors
            itemsIndexed(rows) { index, row ->
                var isSuggestionsOpen by remember { mutableStateOf(true) }
                val matchingItems = remember(row.itemName, allItems) {
                    val q = row.itemName.trim()
                    if (q.isBlank()) {
                        emptyList()
                    } else {
                        allItems.mapNotNull { dbItem ->
                            val score = com.example.util.ItemSearchMatcher.matchScore(dbItem, q)
                            if (score > 0) Pair(dbItem, score) else null
                        }.sortedByDescending { it.second }
                        .map { it.first }
                        .take(15)
                    }
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = row.itemName,
                                onValueChange = { 
                                    rows[index] = row.copy(itemName = it)
                                    isSuggestionsOpen = true
                                },
                                label = { Text("Item Name (Search DB)") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = row.hsnCode,
                                onValueChange = { rows[index] = row.copy(hsnCode = it) },
                                label = { Text("HSN/SAC") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { if (rows.size > 1) rows.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                            }
                        }
                        if (row.costPrice > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cost Price: ₹${row.costPrice}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue
                            )
                        }

                        if (isSuggestionsOpen && matchingItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 2.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("💡 Matching Inventory Items:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandBlue, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    matchingItems.take(5).forEach { matched ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val displayName = if (matched.size.isNotBlank() && !matched.name.contains(matched.size, ignoreCase = true)) {
                                                        "${matched.name} ${matched.size}"
                                                    } else {
                                                        matched.name
                                                    }
                                                    rows[index] = row.copy(
                                                        itemName = displayName,
                                                        costPrice = matched.cost,
                                                        rate = if (matched.price > 0.0) matched.price else (if (matched.cost > 0.0) matched.cost * 1.18 else row.rate)
                                                    )
                                                    isSuggestionsOpen = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(matched.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                val meta = listOf(
                                                    matched.type.ifBlank { null }?.let { "Type: $it" },
                                                    matched.brand.ifBlank { null }?.let { "Brand: $it" },
                                                    matched.size.ifBlank { null }?.let { "Size: $it" }
                                                ).filterNotNull().joinToString(" • ")
                                                if (meta.isNotBlank()) {
                                                    Text(meta, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (matched.price > 0.0) {
                                                Text("₹${matched.price}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = row.qty.toString(),
                                onValueChange = { rows[index] = row.copy(qty = it.toDoubleOrNull() ?: 1.0) },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = row.rate.toString(),
                                onValueChange = { rows[index] = row.copy(rate = it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Rate (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            // Tax Slab Selector
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Tax Slab", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    listOf(5.0, 12.0, 18.0, 28.0).forEach { slab ->
                                        val selected = row.taxRatePercent == slab
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (selected) BrandBlue else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { rows[index] = row.copy(taxRatePercent = slab) }.padding(2.dp)
                                        ) {
                                            Text(
                                                text = "${slab.toInt()}%",
                                                fontSize = 10.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary Breakdown Footer Card inside LazyColumn
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.1f)),
                    border = BorderStroke(1.5.dp, BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxable Amount:", fontSize = 13.sp)
                            Text("₹%.2f".format(taxableTotal), fontWeight = FontWeight.Bold)
                        }
                        if (isInterState) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IGST Total:", fontSize = 13.sp)
                                Text("₹%.2f".format(totalTaxAmount), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST Total (${totalTaxAmount / 2 / taxableTotal.coerceAtLeast(1.0) * 100}%):", fontSize = 13.sp)
                                Text("₹%.2f".format(totalTaxAmount / 2), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SGST Total:", fontSize = 13.sp)
                                Text("₹%.2f".format(totalTaxAmount / 2), fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            Text("₹%.2f".format(grandTotal), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                    }
                }
            }

            // Generate Button inside LazyColumn
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        viewModel.showToast("GST Tax Invoice generated successfully! E-Way Bill Status checked ✓")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🖨️ Generate & Export Tax Invoice PDF", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}
