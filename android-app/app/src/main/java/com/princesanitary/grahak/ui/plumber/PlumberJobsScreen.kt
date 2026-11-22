package com.princesanitary.grahak.ui.plumber

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.plumber.jobs.JobRequestsScreen
import com.princesanitary.grahak.ui.plumber.wallet.PlumberWalletViewModel
import com.princesanitary.grahak.ui.plumber.wallet.WalletUiState
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.LocalOffer
import com.princesanitary.grahak.ui.theme.Amber
import com.princesanitary.grahak.ui.theme.BentoBlue
import com.princesanitary.grahak.ui.theme.BentoGreen
import com.princesanitary.grahak.ui.theme.BentoOnDark
import com.princesanitary.grahak.ui.theme.BentoOnVibrant
import com.princesanitary.grahak.ui.theme.BentoYellow
import com.princesanitary.grahak.ui.theme.BrandTeal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextWhite

/**
 * Plumber landing screen: an earnings hero followed by the live job feed.
 *
 * Earnings are the number a plumber opens the app for, so they lead. The
 * existing [JobRequestsScreen] is embedded underneath rather than duplicated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlumberJobsScreen(
    plumber: com.princesanitary.grahak.data.model.Plumber,
    walletViewModel: PlumberWalletViewModel,
    onToggleOnline: (Boolean) -> Unit,
    onOpenJob: (String) -> Unit,
    onOpenWallet: () -> Unit
) {
    val walletState by walletViewModel.uiState.collectAsStateWithLifecycle()

    val firstName = remember(plumber.name) {
        plumber.name.trim().split(" ").firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: ServiceLocator.auth.currentUser?.email?.substringBefore("@")
            ?: "there"
    }

    val wallet = (walletState as? WalletUiState.Success)
    val isOnline = plumber.operationalStatus ==
        com.princesanitary.grahak.data.model.OperationalStatus.ONLINE

    val tabState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val tab = tabState.intValue

    var isRefreshing by remember { mutableStateOf(false) }
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            isRefreshing = true
            coroutineScope.launch {
                walletViewModel.refresh()
                delay(500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Hi, $firstName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Here's what's waiting for you today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.princesanitary.grahak.ui.theme.TextSecondary
                )

                Spacer(Modifier.height(16.dp))

                // Availability switch - the plumber controls whether customers can
                // see them as available right now.
                DutyToggle(isOnline = isOnline, onToggle = onToggleOnline)

                Spacer(Modifier.height(16.dp))

                EarningsHero(
                    withdrawable = wallet?.wallet?.withdrawableBalance ?: 0.0,
                    escrow = wallet?.wallet?.escrowBalance ?: 0.0,
                    jobsCompleted = wallet?.transactions?.size ?: 0,
                    onOpenWallet = onOpenWallet
                )

                Spacer(Modifier.height(24.dp))

                // Open queue vs. their own work. Accepting a job used to make it
                // disappear from the plumber's view entirely.
                com.princesanitary.grahak.ui.components.SegmentedToggle(
                    options = listOf("Requests", "My jobs"),
                    selectedIndex = tab,
                    onSelect = { tabState.intValue = it }
                )

                Spacer(Modifier.height(12.dp))
            }

            Box(Modifier.fillMaxSize()) {
                if (tab == 0) {
                    JobRequestsScreen(onJobAccepted = onOpenJob)
                } else {
                    com.princesanitary.grahak.ui.plumber.jobs.PlumberMyJobsScreen(
                        onOpenJob = onOpenJob
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningsHero(
    withdrawable: Double,
    escrow: Double,
    jobsCompleted: Int,
    onOpenWallet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(SurfaceWhite)
            .border(1.dp, Line, RoundedCornerShape(26.dp))
            .clickable(onClick = onOpenWallet)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AVAILABLE TO WITHDRAW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5f.sp
                    ),
                    color = BentoGreen
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatRupees(withdrawable),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PaperDim)
                    .border(1.dp, Line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowForward,
                    "Open wallet",
                    Modifier.size(20.dp),
                    tint = BentoGreen
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroStat(
                icon = Icons.Rounded.Star,
                label = "In Escrow",
                value = formatRupees(escrow),
                accentColor = BentoBlue,
                modifier = Modifier.weight(1f)
            )
            HeroStat(
                icon = Icons.Rounded.TaskAlt,
                label = "Completed",
                value = jobsCompleted.toString(),
                accentColor = BentoYellow,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Commission callout pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PaperDim)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = BentoYellow
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Order materials from shop to earn direct commissions",
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun HeroStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color = BentoGreen,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PaperDim)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = accentColor)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = com.princesanitary.grahak.ui.theme.TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
        }
    }
}

/* ------------------------------------------------------------- duty toggle */

@Composable
private fun DutyToggle(
    isOnline: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (isOnline) BentoGreen.copy(alpha = 0.12f)
        else PaperDim,
        label = "duty"
    )
    val borderCol = if (isOnline) BentoGreen.copy(alpha = 0.4f) else Line

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isOnline) BentoGreen
                    else com.princesanitary.grahak.ui.theme.TextTertiary
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isOnline) "You're on duty" else "You're off duty",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isOnline) BentoGreen else Ink
            )
            Text(
                text = if (isOnline) {
                    "Customers can book you for site visits"
                } else {
                    "Switch on to receive customer service bookings"
                },
                style = MaterialTheme.typography.bodySmall,
                color = com.princesanitary.grahak.ui.theme.TextSecondary
            )
        }
        androidx.compose.material3.Switch(
            checked = isOnline,
            onCheckedChange = onToggle,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = SurfaceWhite,
                checkedTrackColor = BentoGreen,
                uncheckedThumbColor = SurfaceWhite,
                uncheckedTrackColor = com.princesanitary.grahak.ui.theme.TextTertiary
            )
        )
    }
}
