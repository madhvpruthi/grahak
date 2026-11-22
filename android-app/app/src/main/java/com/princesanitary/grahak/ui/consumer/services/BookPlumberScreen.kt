package com.princesanitary.grahak.ui.consumer.services

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.WaterDamage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.GrahakCard
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.EmeraldSignalBorder
import com.princesanitary.grahak.ui.theme.EmeraldSignalSurface
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary

@Composable
fun BookPlumberScreen(
    viewModel: BookPlumberViewModel = appViewModel(),
    onBookingSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availablePlumbers by viewModel.availablePlumbers.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf<SavedAddress?>(null) }
    var description by remember { mutableStateOf("") }
    var showAddressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is BookingUiState.Success) {
            onBookingSuccess((uiState as BookingUiState.Success).jobId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // Header
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Book Plumber",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Text(
                        text = "On-demand • Direct store rates",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(EmeraldSignalSurface)
                        .border(1.dp, EmeraldSignalBorder, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(EmeraldSignal)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Live Plumber Dispatch",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = EmeraldSignal
                            )
                            val plumberCount = if (availablePlumbers.isNotEmpty()) availablePlumbers.size else 4
                            Text(
                                text = "$plumberCount verified plumbers online in Saharanpur • Instant dispatch",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Service Category Selection
            item {
                GrahakCard {
                    Text(
                        text = "1. Select Service Type",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Choose the issue you need fixed today",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))

                    ServiceCategoryGrid(
                        categories = ServiceSpeciality.entries.map { it.displayName },
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }
            }

            // Address Selection
            item {
                GrahakCard(
                    onClick = { showAddressDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PaperDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = ElectricLime
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. Service Location",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = selectedAddress?.fullAddress ?: "Pin your site address on map",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedAddress == null) TextSecondary else Ink
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PaperDim)
                                .border(1.dp, Line, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedAddress == null) "Set Pin" else "Change",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ElectricLime
                            )
                        }
                    }
                }
            }

            // Description
            item {
                GrahakCard {
                    Text(
                        text = "3. Issue Details (Optional)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Describe what is broken, pipe size, tap brand, etc.",
                                color = TextTertiary
                            )
                        },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = Line,
                            focusedContainerColor = PaperDim,
                            unfocusedContainerColor = PaperDim,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink
                        )
                    )
                }
            }

            // Primary Booking Button
            item {
                Spacer(Modifier.height(8.dp))
                if (uiState is BookingUiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.princesanitary.grahak.ui.components.FloatingBallLoader(
                            label = "Broadcasting request to nearby plumbers..."
                        )
                    }
                } else {
                    PrimaryButton(
                        text = if (selectedAddress == null) "Set location to request dispatch" else "Request Plumber Dispatch",
                        enabled = selectedCategory.isNotBlank() && selectedAddress != null,
                        onClick = {
                            selectedAddress?.let { addr ->
                                viewModel.bookPlumber(
                                    serviceCategory = selectedCategory,
                                    siteAddress = addr,
                                    description = description.ifBlank { selectedCategory }
                                )
                            }
                        }
                    )
                }
                if (selectedAddress == null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Pin your site address to dispatch the nearest verified plumber.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Map site picker dialog
    if (showAddressDialog) {
        Dialog(
            onDismissRequest = { showAddressDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LocationPickerSheet(
                onDismiss = { showAddressDialog = false },
                onConfirm = { address ->
                    selectedAddress = address
                    showAddressDialog = false
                }
            )
        }
    }
}

@Composable
fun PlumberPreviewCard(
    plumber: com.princesanitary.grahak.data.model.Plumber
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PaperDim),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = plumber.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ElectricLime
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plumber.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = LuxeGold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (plumber.stats.rating > 0)
                            "${plumber.stats.rating} • ${plumber.stats.jobsCompleted} jobs"
                        else "New on Grahak",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRupees(plumber.rates.visitCharge),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = LuxeGold
                )
                Text(
                    text = "Visit charge",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ServiceCategoryGrid(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { category ->
                    ServiceCategoryTile(
                        label = category,
                        selected = selected == category,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(category) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ServiceCategoryTile(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = iconForService(label)

    val container by animateColorAsState(
        targetValue = if (selected) LuxeGold else PaperDim,
        label = "serviceTileBg"
    )
    val content = if (selected) Color(0xFF09090B) else Ink
    val borderCol = if (selected) LuxeGold else Line

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Color(0xFF09090B).copy(alpha = 0.15f)
                    else SurfaceWhite
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = content)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun iconForService(label: String) =
    when (ServiceSpeciality.entries.find { it.displayName == label }) {
        ServiceSpeciality.LEAK_REPAIR -> Icons.Rounded.WaterDamage
        ServiceSpeciality.PIPE_FITTING -> Icons.Rounded.Plumbing
        ServiceSpeciality.BATHROOM_FITTING -> Icons.Rounded.Shower
        ServiceSpeciality.TOILET_INSTALL -> Icons.Rounded.Build
        ServiceSpeciality.KITCHEN_PLUMBING -> Icons.Rounded.Kitchen
        ServiceSpeciality.DRAIN_CLEANING -> Icons.Rounded.Handyman
        ServiceSpeciality.MOTOR_PUMP -> Icons.Rounded.Build
        ServiceSpeciality.EMERGENCY -> Icons.Rounded.Bolt
        null -> Icons.Rounded.MoreHoriz
    }
