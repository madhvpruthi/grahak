package com.princesanitary.grahak.ui.consumer.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.components.SegmentedToggle
import com.princesanitary.grahak.ui.consumer.services.MyRequestsScreen
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.GrahakRedBright
import com.princesanitary.grahak.ui.theme.TextSecondary

/**
 * One home for everything the customer has asked for.
 *
 * Product orders and plumber bookings are two different collections but the same
 * question - "what did I order and where is it?" - so they live behind one tab
 * with a segmented switch rather than competing for space in the nav bar.
 */
@Composable
fun OrdersHubScreen(
    orderHistoryViewModel: OrderHistoryViewModel,
    onOrderClick: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        orderHistoryViewModel.refresh()
    }

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
                text = "Your activity",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Material orders and plumber bookings",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            SegmentedToggle(
                options = listOf("Orders", "Plumbers"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
            Spacer(Modifier.height(16.dp))
        }

        Box(Modifier.fillMaxSize()) {
            if (tab == 0) {
                OrderHistoryList(
                    viewModel = orderHistoryViewModel,
                    onOrderClick = onOrderClick
                )
            } else {
                MyRequestsScreen()
            }
        }
    }
}
