package com.princesanitary.grahak.ui.plumber.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.princesanitary.grahak.data.model.OperationalStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.GrahakCard
import com.princesanitary.grahak.ui.components.SecondaryButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.BentoBlue
import com.princesanitary.grahak.ui.theme.BentoGreen
import com.princesanitary.grahak.ui.theme.BentoOnVibrant
import com.princesanitary.grahak.ui.theme.BentoYellow
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.StatusWarning
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary

@Composable
fun PlumberProfileScreen(
    viewModel: PlumberProfileViewModel = appViewModel(),
    onOrderMaterials: () -> Unit = {},
    onLogout: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    var showStatusDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Plumber Profile",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Manage your service rates, commissions & status",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(14.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (val state = profileState) {
                    is ProfileUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ElectricLime, strokeWidth = 2.5.dp)
                        }
                    }
                    is ProfileUiState.Success -> {
                        ProfileHeader(
                            plumber = state.plumber,
                            onStatusClick = { showStatusDialog = true }
                        )
                    }
                    is ProfileUiState.Error -> {
                        GrahakCard {
                            Text(text = state.message, color = StatusError)
                        }
                    }
                }
            }

            // Bento Stats (Completed Jobs & Material Commissions)
            item {
                if (profileState is ProfileUiState.Success) {
                    val plumber = (profileState as ProfileUiState.Success).plumber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Bento (Completed Jobs)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(BentoGreen)
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "JOBS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BentoOnVibrant.copy(alpha = 0.75f)
                                    )
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = BentoOnVibrant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = plumber.stats.jobsCompleted.toString(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BentoOnVibrant
                                    )
                                    Text(
                                        text = if (plumber.stats.rating > 0) "Rating ${plumber.stats.rating} ★" else "Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoOnVibrant.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        // Right Bento (Material Commission Tier)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(BentoBlue)
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "COMMISSIONS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BentoOnVibrant.copy(alpha = 0.75f)
                                    )
                                    Icon(
                                        Icons.Rounded.LocalOffer,
                                        contentDescription = null,
                                        tint = BentoOnVibrant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "5% - 10%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BentoOnVibrant
                                    )
                                    Text(
                                        text = "On store orders",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoOnVibrant.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Material Commission Rate List Card
            item {
                GrahakCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BentoYellow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.LocalOffer,
                                    contentDescription = null,
                                    tint = BentoYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "3% Plumber Purchase Incentive",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Ink
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Order plumbing & sanitary materials directly from Prince Sanitary Store for your jobs and get an instant 3% cashback directly in your FixIt wallet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onOrderMaterials,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = BentoOnVibrant)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Order Materials (Earn 3% Cashback)",
                                fontWeight = FontWeight.Bold,
                                color = BentoOnVibrant
                            )
                        }
                    }
                }
            }

            // Service Rates Card
            item {
                if (profileState is ProfileUiState.Success) {
                    val plumber = (profileState as ProfileUiState.Success).plumber
                    GrahakCard {
                        Text(
                            text = "Your Service Rates",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Standard Visit Charge",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink
                            )
                            Text(
                                text = formatRupees(plumber.rates.visitCharge),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BentoGreen
                            )
                        }

                        if (plumber.rates.customRates.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Line)
                            Spacer(Modifier.height(10.dp))

                            plumber.rates.customRates.forEach { (service, rate) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = service,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "₹$rate",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Ink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // KYC Status Card
            item {
                if (profileState is ProfileUiState.Success) {
                    val plumber = (profileState as ProfileUiState.Success).plumber
                    GrahakCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (plumber.kyc.verified) BentoGreen.copy(alpha = 0.15f)
                                        else StatusWarning.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (plumber.kyc.verified) Icons.Rounded.Shield else Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (plumber.kyc.verified) BentoGreen else StatusWarning
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (plumber.kyc.verified) "Verified Plumber Partner" else "Verification In Review",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Ink
                                )
                                Text(
                                    text = if (plumber.kyc.verified)
                                        "Verified by Prince Sanitary Store"
                                    else
                                        "Your partner profile is being approved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Logout Action Button
            item {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = "Sign out of Plumber Account",
                    onClick = onLogout
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // Operational Status Dialog
    if (showStatusDialog && profileState is ProfileUiState.Success) {
        OperationalStatusDialog(
            currentStatus = (profileState as ProfileUiState.Success).plumber.operationalStatus,
            onDismiss = { showStatusDialog = false },
            onStatusSelected = { status ->
                viewModel.updateOperationalStatus(status)
                showStatusDialog = false
            }
        )
    }
}

@Composable
fun ProfileHeader(
    plumber: Plumber,
    onStatusClick: () -> Unit
) {
    GrahakCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar
            if (plumber.photoUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(plumber.photoUrl),
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(PaperDim)
                        .border(1.dp, Line, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plumber.name.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = BentoGreen
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plumber.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    if (plumber.kyc.verified) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Verified",
                            tint = BentoGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = plumber.phone.ifEmpty { "Verified Plumber" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                // Operational Status Badge (Clickable)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when (plumber.operationalStatus) {
                                OperationalStatus.ONLINE -> BentoGreen.copy(alpha = 0.2f)
                                OperationalStatus.BUSY -> StatusWarning.copy(alpha = 0.2f)
                                OperationalStatus.OFFLINE -> StatusError.copy(alpha = 0.2f)
                            }
                        )
                        .border(
                            1.dp,
                            when (plumber.operationalStatus) {
                                OperationalStatus.ONLINE -> BentoGreen.copy(alpha = 0.5f)
                                OperationalStatus.BUSY -> StatusWarning.copy(alpha = 0.5f)
                                OperationalStatus.OFFLINE -> StatusError.copy(alpha = 0.5f)
                            },
                            CircleShape
                        )
                        .clickable(onClick = onStatusClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (plumber.operationalStatus) {
                                        OperationalStatus.ONLINE -> BentoGreen
                                        OperationalStatus.BUSY -> StatusWarning
                                        OperationalStatus.OFFLINE -> StatusError
                                    }
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${plumber.operationalStatus.name} • Change",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = when (plumber.operationalStatus) {
                                OperationalStatus.ONLINE -> BentoGreen
                                OperationalStatus.BUSY -> StatusWarning
                                OperationalStatus.OFFLINE -> StatusError
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OperationalStatusDialog(
    currentStatus: OperationalStatus,
    onDismiss: () -> Unit,
    onStatusSelected: (OperationalStatus) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                "Change Duty Status",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OperationalStatus.entries.forEach { status ->
                    val isSelected = status == currentStatus
                    val col = when (status) {
                        OperationalStatus.ONLINE -> BentoGreen
                        OperationalStatus.BUSY -> StatusWarning
                        OperationalStatus.OFFLINE -> StatusError
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) col.copy(alpha = 0.15f) else PaperDim)
                            .border(1.dp, if (isSelected) col else Line, RoundedCornerShape(16.dp))
                            .clickable { onStatusSelected(status) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(col)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = status.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) col else Ink
                            )
                            Text(
                                text = when (status) {
                                    OperationalStatus.ONLINE -> "Available for customer bookings"
                                    OperationalStatus.BUSY -> "Currently working on a site"
                                    OperationalStatus.OFFLINE -> "Not accepting bookings right now"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
