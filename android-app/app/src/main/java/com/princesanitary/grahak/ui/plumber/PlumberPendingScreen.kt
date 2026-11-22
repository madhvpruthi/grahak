package com.princesanitary.grahak.ui.plumber

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.ui.components.GrahakCard
import com.princesanitary.grahak.ui.components.SecondaryButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.FixitYellow
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusErrorSoft
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary

/**
 * Shown between submitting an application and being approved.
 *
 * Rather than a bare "pending" message this plays back exactly what the store
 * received, so the plumber can see their rates are recorded and spot a mistake
 * while they wait.
 */
@Composable
fun PlumberPendingScreen(
    plumber: Plumber,
    rejected: Boolean,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (rejected) StatusErrorSoft else FixitYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (rejected) Icons.Rounded.Report else Icons.Rounded.HourglassTop,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = if (rejected) StatusError else Ink
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (rejected) "Application not approved" else "Under review",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (rejected) {
                plumber.kyc.rejectionReason.ifBlank {
                    "Prince Sanitary Store could not verify your details. " +
                        "Please contact the store."
                }
            } else {
                "Prince Sanitary Store is checking your details. You'll start " +
                    "receiving job requests as soon as you're approved."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // ---- what the store received ----
        GrahakCard {
            Text("Your application", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(14.dp))

            SummaryRow("Name", plumber.name)
            SummaryRow("Mobile", plumber.phone)
            SummaryRow(
                "Experience",
                if (plumber.experienceYears > 0) "${plumber.experienceYears} years" else "—"
            )
            SummaryRow("Area", plumber.baseAddress.ifBlank { plumber.servicePincode })
            SummaryRow("Visit charge", formatRupees(plumber.rates.visitCharge))

            Spacer(Modifier.height(14.dp))
            Text("SERVICES & RATES", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Spacer(Modifier.height(8.dp))

            plumber.specialities.forEach { id ->
                val speciality = ServiceSpeciality.fromId(id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), tint = StatusSuccess)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = speciality?.displayName ?: id,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatRupees(plumber.rates.customRates[id] ?: 0.0),
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperDim)
                .padding(14.dp)
        ) {
            Text(
                text = "This screen updates by itself the moment the store approves you — " +
                    "no need to reinstall or sign in again.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(28.dp))

        SecondaryButton(
            text = "Log out",
            leadingIcon = Icons.Rounded.Logout,
            onClick = onLogout
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = Ink
        )
    }
}
