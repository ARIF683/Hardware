package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionRecord
import com.example.ui.NavigationTab
import com.example.ui.StockViewModel
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(viewModel: StockViewModel) {
    val items by viewModel.allItems.collectAsState()
    val recentTx by viewModel.recentTransactions.collectAsState()

    val totalItems = items.size
    val totalQty = items.sumOf { it.qty }
    val costVal = items.sumOf { it.qty * it.cost }
    val priceVal = items.sumOf { it.qty * it.price }
    val profit = priceVal - costVal
    val lowStockCount = items.count { it.qty < 0 || (it.low > 0 && it.qty <= it.low) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Stock Overview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (lowStockCount > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            viewModel.inStockOnly.value = false
                            viewModel.navigateTo(NavigationTab.ITEMS)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$lowStockCount items low or negative in stock",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tap to review items in inventory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        item {
            // Stats Grid 2 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Items",
                    value = "$totalItems",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total quantity",
                    value = if (totalQty % 1.0 == 0.0) totalQty.toLong().toString() else "%.1f".format(totalQty),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Stock value (cost)",
                    value = StockViewModel.formatRupees(costVal),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Stock value (price)",
                    value = StockViewModel.formatRupees(priceVal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Potential profit",
                    value = StockViewModel.formatRupees(profit),
                    valueColor = if (profit >= 0) SuccessGreen else DangerRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Low / negative stock",
                    value = "$lowStockCount",
                    valueColor = if (lowStockCount > 0) DangerRed else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.navigateTo(NavigationTab.TRANSACTIONS) }) {
                    Text("View all")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (recentTx.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet.\nOpen an item and tap + New.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(recentTx) { tx ->
                TransactionRowItem(
                    tx = tx,
                    onItemClick = {
                        if (!tx.itemId.isNullOrEmpty()) {
                            viewModel.openItemDetail(tx.itemId)
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionRecord,
    onItemClick: () -> Unit
) {
    val isIn = tx.action == "in"
    val dateFormatted = remember(tx.createdAt) {
        try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = sdfIn.parse(tx.createdAt.take(19))
            val sdfOut = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            date?.let { sdfOut.format(it) } ?: tx.createdAt
        } catch (e: Exception) {
            tx.createdAt
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.itemName,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            val noteDetails = listOfNotNull(
                dateFormatted,
                tx.note.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            Text(
                text = noteDetails,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val sign = if (isIn) "+" else "−"
            val qtyStr = if (tx.qty % 1.0 == 0.0) tx.qty.toLong().toString() else "%.1f".format(tx.qty)
            val unitStr = if (tx.unit != "pcs" && tx.unit.isNotBlank()) " ${tx.unit}" else ""
            Text(
                text = "$sign$qtyStr$unitStr",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isIn) SuccessGreen else DangerRed
            )
            Text(
                text = "Bal ${if (tx.balance % 1.0 == 0.0) tx.balance.toLong().toString() else "%.1f".format(tx.balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
