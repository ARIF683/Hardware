package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Item
import com.example.data.repository.BillRowData
import com.example.ui.StockViewModel
import com.example.ui.components.CalculatorDialog
import com.example.ui.components.ValuePickerDialog
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillScreen(viewModel: StockViewModel) {
    val allItems by viewModel.allItems.collectAsState()

    var stage by remember { mutableStateOf("idle") } // "idle" or "review"
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullImage by remember { mutableStateOf(false) }

    var supplier by remember { mutableStateOf("") }
    var billNo by remember { mutableStateOf("") }
    var billDate by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }

    val rows = remember {
        mutableStateListOf(
            BillRowData(
                id = "row_0",
                name = "",
                rate = 0.0,
                qty = 1.0,
                unit = "pcs",
                include = true
            )
        )
    }

    var showCalculator by remember { mutableStateOf(false) }

    // Pick image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            photoBitmap = null
        }
    }

    // Take photo with camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBitmap = bitmap
            imageUri = null
        }
    }

    if (showCalculator) {
        CalculatorDialog(onDismiss = { showCalculator = false })
    }

    if (showFullImage && (imageUri != null || photoBitmap != null)) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap!!.asImageBitmap(),
                            contentDescription = "Full bill",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Full bill",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { showFullImage = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (stage == "review") {
                    stage = "idle"
                } else {
                    viewModel.navigateBack()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = if (stage == "review") "Enter items" else "Bill",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (stage == "review") {
                IconButton(onClick = { showCalculator = true }) {
                    Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = BrandBlue)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (stage == "idle") {
            // Idle Screen
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📷 Bill image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Take a photo or pick from gallery, then tap Continue. You'll enter items while viewing the photo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera")
                                }

                                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery")
                                }

                                if (imageUri != null || photoBitmap != null) {
                                    OutlinedButton(onClick = {
                                        imageUri = null
                                        photoBitmap = null
                                    }) {
                                        Text("Remove")
                                    }
                                }
                            }

                            if (photoBitmap != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Image(
                                    bitmap = photoBitmap!!.asImageBitmap(),
                                    contentDescription = "Bill photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (imageUri != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Bill photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            if (imageUri != null || photoBitmap != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { stage = "review" },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                ) {
                                    Text("Continue →")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No image?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You can skip the photo and just enter rows directly. Useful if you already have the physical bill.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { stage = "review" }) {
                                Text("Enter items without a photo →")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else {
            // Review Stage
            val includedRows = rows.filter { it.include }
            val billTotal = includedRows.sumOf { it.rate * it.qty }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Image preview strip (if image chosen)
                if (photoBitmap != null || imageUri != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showFullImage = true }
                        ) {
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap!!.asImageBitmap(),
                                    contentDescription = "Bill thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Bill thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text(
                                text = "Tap image to enlarge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Header Fields Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = supplier,
                                onValueChange = { supplier = it },
                                label = { Text("Supplier") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = billNo,
                                onValueChange = { billNo = it },
                                label = { Text("Bill no") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = billDate,
                                onValueChange = { billDate = it },
                                label = { Text("Bill date (dd/mm/yyyy)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set Type / Brand / Size first if you want to narrow the item list. Then tap item name to pick.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Row Items
                itemsIndexed(rows) { index, row ->
                    BillRowEditor(
                        index = index,
                        row = row,
                        allItems = allItems,
                        onUpdate = { updated -> rows[index] = updated },
                        onDelete = {
                            if (rows.size > 1) {
                                rows.removeAt(index)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Summary & Actions
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bill total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = StockViewModel.formatRupees(billTotal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = {
                            rows.add(
                                BillRowData(
                                    id = "row_${System.currentTimeMillis()}",
                                    name = "",
                                    rate = 0.0,
                                    qty = 1.0,
                                    unit = "pcs",
                                    include = true
                                )
                            )
                        }) {
                            Text("+ Add row")
                        }

                        Button(
                            onClick = {
                                val namedRows = rows.filter { it.include && it.name.trim().isNotEmpty() }
                                if (namedRows.isEmpty()) {
                                    viewModel.showToast("Add at least one named row")
                                    return@Button
                                }
                                viewModel.confirmBill(supplier, billNo, billDate, rows)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("Confirm (${includedRows.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun BillRowEditor(
    index: Int,
    row: BillRowData,
    allItems: List<Item>,
    onUpdate: (BillRowData) -> Unit,
    onDelete: () -> Unit
) {
    var showItemPicker by remember { mutableStateOf(false) }
    var pickerField by remember { mutableStateOf<String?>(null) } // "type", "brand", "size", "unit"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = row.include,
                    onCheckedChange = { onUpdate(row.copy(include = it)) },
                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Item #${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (row.name.isNotBlank()) row.name + (if (row.matchedItem == null) " (new)" else "") else "— tap to pick item —",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (row.name.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showItemPicker = true }
                            .padding(vertical = 4.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rate and Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (row.rate > 0) row.rate.toString() else "",
                    onValueChange = {
                        val r = it.toDoubleOrNull() ?: 0.0
                        onUpdate(row.copy(rate = r))
                    },
                    label = { Text("Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = if (row.qty > 0) row.qty.toString() else "",
                    onValueChange = {
                        val q = it.toDoubleOrNull() ?: 0.0
                        onUpdate(row.copy(qty = q))
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type, Brand, Size, Unit chips/buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { pickerField = "type" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.type.ifEmpty { "Type" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { pickerField = "brand" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.brand.ifEmpty { "Brand" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { pickerField = "size" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.size.ifEmpty { "Size" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { pickerField = "unit" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.unit.ifEmpty { "pcs" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Item Picker Dialog
    if (showItemPicker) {
        val filteredScope = remember(row, allItems) {
            allItems.filter { it ->
                if (row.type.isNotBlank() && it.type != row.type) false
                else if (row.brand.isNotBlank() && it.brand != row.brand) false
                else if (row.size.isNotBlank() && it.size != row.size) false
                else true
            }
        }
        val options = filteredScope.map { it.name to it.cost.toInt() }
        ValuePickerDialog(
            title = "Pick Item",
            optionsWithCount = options,
            currentValue = row.name,
            onSelect = { selectedName ->
                val matched = filteredScope.firstOrNull { it.name.equals(selectedName, ignoreCase = true) }
                if (matched != null) {
                    onUpdate(
                        row.copy(
                            name = matched.name,
                            matchedItem = matched,
                            rate = if (row.rate == 0.0) matched.cost else row.rate,
                            type = if (row.type.isEmpty()) matched.type else row.type,
                            brand = if (row.brand.isEmpty()) matched.brand else row.brand,
                            size = if (row.size.isEmpty()) matched.size else row.size,
                            unit = if (row.unit.isEmpty() || row.unit == "pcs") matched.unit else row.unit
                        )
                    )
                } else {
                    onUpdate(
                        row.copy(
                            name = selectedName,
                            matchedItem = null
                        )
                    )
                }
                showItemPicker = false
            },
            onDismiss = { showItemPicker = false }
        )
    }

    // Type / Brand / Size / Unit picker
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
                "type" -> row.type
                "brand" -> row.brand
                "size" -> row.size
                "unit" -> row.unit
                else -> ""
            },
            onSelect = { selectedVal ->
                when (field) {
                    "type" -> onUpdate(row.copy(type = selectedVal))
                    "brand" -> onUpdate(row.copy(brand = selectedVal))
                    "size" -> onUpdate(row.copy(size = selectedVal))
                    "unit" -> onUpdate(row.copy(unit = selectedVal.ifBlank { "pcs" }))
                }
                pickerField = null
            },
            onDismiss = { pickerField = null }
        )
    }
}
