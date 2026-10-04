package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Item
import com.example.ui.StockViewModel
import com.example.ui.components.ValuePickerDialog
import com.example.ui.theme.BrandBlue

@Composable
fun ItemFormDialog(
    itemToEdit: Item?,
    viewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    val allItems by viewModel.allItems.collectAsState()
    val isEdit = itemToEdit != null

    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var code by remember { mutableStateOf(itemToEdit?.code ?: "") }
    var barcode by remember { mutableStateOf(itemToEdit?.barcode ?: "") }
    var type by remember { mutableStateOf(itemToEdit?.type ?: "") }
    var brand by remember { mutableStateOf(itemToEdit?.brand ?: "") }
    var size by remember { mutableStateOf(itemToEdit?.size ?: "") }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "pcs") }
    var mrp by remember { mutableStateOf(itemToEdit?.mrp?.toString() ?: "") }
    var cost by remember { mutableStateOf(if (isEdit) itemToEdit?.cost?.toString() ?: "" else "") }
    var price by remember { mutableStateOf(if (isEdit) itemToEdit?.price?.toString() ?: "" else "") }
    var openingQty by remember { mutableStateOf("") }
    var lowStock by remember { mutableStateOf(itemToEdit?.low?.toString() ?: "0") }
    var aliases by remember { mutableStateOf(itemToEdit?.aliases ?: "") }

    var pickerField by remember { mutableStateOf<String?>(null) } // "type", "brand", "size", "unit"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEdit) "Edit item" else "New item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Type Picker Box
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    placeholder = { Text("Tap to select type") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickerField = "type" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Brand Picker Box
                OutlinedTextField(
                    value = brand,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Brand") },
                    placeholder = { Text("Tap to select brand") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickerField = "brand" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Size Picker Box
                OutlinedTextField(
                    value = size,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Size") },
                    placeholder = { Text("Tap to select size") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickerField = "size" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Unit Picker Box
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickerField = "unit" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mrp,
                    onValueChange = { mrp = it },
                    label = { Text("MRP (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isEdit) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = openingQty,
                        onValueChange = { openingQty = it },
                        label = { Text("Opening quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lowStock,
                    onValueChange = { lowStock = it },
                    label = { Text("Low-stock alert at (0 = off)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text("Aliases (separated by |)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                viewModel.showToast("Enter item name")
                                return@Button
                            }
                            viewModel.saveItem(
                                id = itemToEdit?.id,
                                name = name.trim(),
                                code = code.trim(),
                                barcode = barcode.trim(),
                                type = type.trim(),
                                brand = brand.trim(),
                                size = size.trim(),
                                unit = unit.ifBlank { "pcs" },
                                mrp = mrp.trim().toDoubleOrNull(),
                                cost = cost.trim().toDoubleOrNull() ?: 0.0,
                                price = price.trim().toDoubleOrNull() ?: 0.0,
                                low = lowStock.trim().toDoubleOrNull() ?: 0.0,
                                aliases = aliases.trim(),
                                openingQty = openingQty.trim().toDoubleOrNull() ?: 0.0
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Field Picker Dialogs
    pickerField?.let { field ->
        val counts = remember(field, allItems) {
            when (field) {
                "type" -> allItems.map { it.type }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList()
                "brand" -> allItems.map { it.brand }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList()
                "size" -> allItems.map { it.size }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList()
                "unit" -> listOf("pcs", "kg", "g", "L", "ml", "box", "pack", "bag", "m", "ft", "cm", "set", "pair", "dozen", "roll", "sheet")
                    .map { Pair(it, allItems.count { item -> item.unit == it }) }
                else -> emptyList()
            }
        }

        ValuePickerDialog(
            title = "Select ${field.replaceFirstChar { it.uppercase() }}",
            optionsWithCount = counts,
            currentValue = when (field) {
                "type" -> type
                "brand" -> brand
                "size" -> size
                "unit" -> unit
                else -> ""
            },
            onSelect = { selectedVal ->
                when (field) {
                    "type" -> type = selectedVal
                    "brand" -> brand = selectedVal
                    "size" -> size = selectedVal
                    "unit" -> unit = selectedVal.ifBlank { "pcs" }
                }
                pickerField = null
            },
            onDismiss = { pickerField = null }
        )
    }
}
