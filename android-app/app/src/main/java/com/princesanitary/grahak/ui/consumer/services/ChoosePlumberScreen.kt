package com.princesanitary.grahak.ui.consumer.services

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.EmptyState
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.components.ListCardSkeleton
import com.princesanitary.grahak.ui.components.PillChip
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.components.StatusPill
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.BrandTeal
import com.princesanitary.grahak.ui.theme.BrandTealSoft
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.StatusSuccessSoft
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.TextWhite
import com.princesanitary.grahak.ui.theme.TintMint

/**
 * "Choose your plumber".
 *
 * Shows the approved roster with each plumber's own rate for the selected
 * service, so price is a real comparison rather than a fixed number. The
 * customer either requests one plumber directly, or broadcasts and takes
 * whoever accepts first.
 */
@Composable
fun ChoosePlumberScreen(
    address: SavedAddress,
    description: String,
    initialSpeciality: ServiceSpeciality? = null,
    onBack: () -> Unit,
    onRequested: (jobId: String) -> Unit,
    viewModel: ChoosePlumberViewModel = appViewModel()
) {
    // Pre-select whatever the customer already chose on the booking screen.
    LaunchedEffect(initialSpeciality) {
        initialSpeciality?.let(viewModel::selectSpeciality)
    }

    val speciality by viewModel.speciality.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val request by viewModel.request.collectAsStateWithLifecycle()

    var selectedId by remember { mutableStateOf<String?>(null) }

    val sent = request as? RequestState.Sent
    if (sent != null) {
        RequestSentScreen(
            broadcast = sent.broadcast,
            onDone = {
                viewModel.resetRequest()
                onRequested(sent.jobId)
            }
        )
        return
    }

    val offers = (state as? ChoosePlumberState.Ready)?.offers ?: emptyList()
    val chosen = offers.firstOrNull { it.plumber.id == selectedId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // ---------------- header ----------------
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Choose your plumber",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink
                    )
                    Text(
                        text = address.fullAddress.ifBlank { "Selected site" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "WHAT DO YOU NEED",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ServiceSpeciality.entries.toList()) { option ->
                PillChip(
                    text = option.displayName,
                    selected = option == speciality,
                    onClick = {
                        selectedId = null
                        viewModel.selectSpeciality(option)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- roster ----------------
        Box(Modifier.weight(1f)) {
            when {
                state is ChoosePlumberState.Loading -> ListCardSkeleton(count = 3)

                state is ChoosePlumberState.Error -> Column(Modifier.padding(20.dp)) {
                    ErrorBanner((state as ChoosePlumberState.Error).message)
                }

                offers.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Groups,
                    title = "No plumbers for this yet",
                    message = "No approved plumber currently offers " +
                        "${speciality.displayName.lowercase()} in your area. " +
                        "Try another service, or broadcast and we'll find someone."
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp, bottom = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(offers, key = { it.plumber.id }) { offer ->
                        PlumberOfferCard(
                            offer = offer,
                            selected = offer.plumber.id == selectedId,
                            onClick = {
                                selectedId =
                                    if (selectedId == offer.plumber.id) null else offer.plumber.id
                            }
                        )
                    }
                }
            }
        }

        // ---------------- action ----------------
        Surface(
            color = SurfaceWhite,
            shadowElevation = 14.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(18.dp)
            ) {
                (request as? RequestState.Failed)?.let {
                    ErrorBanner(it.message)
                    Spacer(Modifier.height(12.dp))
                }

                if (chosen != null) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = "${chosen.plumber.name} · ${speciality.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatRupees(chosen.rate),
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                PrimaryButton(
                    text = if (chosen != null) {
                        "Request ${chosen.plumber.name.split(" ").first()}"
                    } else {
                        "Ask any available plumber"
                    },
                    loading = request is RequestState.Sending,
                    onClick = {
                        viewModel.requestJob(
                            plumberId = chosen?.plumber?.id,
                            rate = chosen?.rate ?: speciality.suggestedRate,
                            address = address,
                            description = description
                        )
                    }
                )

                if (chosen == null && offers.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap a plumber to request them directly, or send to everyone " +
                            "and the first to accept takes the job.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------- card */

@Composable
private fun PlumberOfferCard(
    offer: PlumberOffer,
    selected: Boolean,
    onClick: () -> Unit
) {
    val plumber = offer.plumber
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) BrandTealSoft else SurfaceWhite)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BrandTeal else Line,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar, or initials when there is no photo (Storage may be off).
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(TintMint),
            contentAlignment = Alignment.Center
        ) {
            if (plumber.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = plumber.photoUrl,
                    contentDescription = plumber.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = plumber.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
            }
        }

        Spacer(Modifier.width(13.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plumber.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Rounded.Verified, null, Modifier.size(14.dp), tint = BrandTeal)
            }

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (plumber.stats.ratingCount > 0) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(13.dp), tint = Ink)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "%.1f".format(plumber.stats.rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = buildString {
                        append("${plumber.stats.jobsCompleted} jobs")
                        if (plumber.experienceYears > 0) {
                            append(" · ${plumber.experienceYears} yrs")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(7.dp))

            if (offer.isAvailableNow) {
                StatusPill("AVAILABLE NOW", StatusSuccessSoft, StatusSuccess)
            } else {
                StatusPill("OFF DUTY", PaperDim, TextSecondary)
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatRupees(offer.rate),
                style = MaterialTheme.typography.titleMedium,
                color = Ink
            )
            Text(
                text = "visit",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            if (selected) {
                Spacer(Modifier.height(6.dp))
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(19.dp), tint = BrandTeal)
            }
        }
    }
}

/* ------------------------------------------------------------------- sent */

@Composable
private fun RequestSentScreen(
    broadcast: Boolean,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(StatusSuccessSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, null, Modifier.size(40.dp), tint = StatusSuccess)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Request sent",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (broadcast) {
                "We've sent this to every available plumber. The first to accept " +
                    "takes the job, and you'll see their details straight away."
            } else {
                "The plumber has been notified. Accepting is their choice — if they " +
                    "pass, your request opens up to other plumbers automatically."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(26.dp))

        PrimaryButton(text = "Track request", onClick = onDone)
    }
}
