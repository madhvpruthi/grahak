package com.princesanitary.grahak.ui.plumber.wallet

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.PlumberWallet
import com.princesanitary.grahak.data.model.Transaction
import com.princesanitary.grahak.data.model.TransactionType
import com.princesanitary.grahak.data.model.WithdrawalRequest
import com.princesanitary.grahak.data.model.WithdrawalStatus
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.GrahakCard
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.BentoBlue
import com.princesanitary.grahak.ui.theme.BentoGreen
import com.princesanitary.grahak.ui.theme.BentoOnDark
import com.princesanitary.grahak.ui.theme.BentoOnVibrant
import com.princesanitary.grahak.ui.theme.BentoYellow
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.PlumberAccent
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.StatusWarning
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlumberWalletScreen(
    viewModel: PlumberWalletViewModel = appViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val withdrawalAction by viewModel.withdrawalAction.collectAsStateWithLifecycle()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showUpiDialog by remember { mutableStateOf(false) }

    // Handle withdrawal success
    LaunchedEffect(withdrawalAction) {
        when (withdrawalAction) {
            is WithdrawalAction.Success -> {
                showWithdrawDialog = false
                viewModel.resetWithdrawalAction()
            }
            else -> {}
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Wallet",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Earnings, Commissions & Payouts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PaperDim)
                        .border(1.dp, Line, CircleShape)
                        .clickable { viewModel.refresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when (val state = uiState) {
            is WalletUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricLime, strokeWidth = 2.5.dp)
                }
            }
            is WalletUiState.Success -> {
                WalletContent(
                    wallet = state.wallet,
                    transactions = state.transactions,
                    withdrawalRequests = state.withdrawalRequests,
                    onWithdrawClick = { showWithdrawDialog = true },
                    onUpdateUpiId = { showUpiDialog = true }
                )

                if (showWithdrawDialog) {
                    WithdrawalDialog(
                        availableBalance = state.wallet.withdrawableBalance,
                        currentUpiId = state.wallet.upiId,
                        isProcessing = withdrawalAction is WithdrawalAction.Processing,
                        error = (withdrawalAction as? WithdrawalAction.Error)?.message,
                        onWithdraw = { amount, upiId, notes ->
                            viewModel.requestWithdrawal(amount, upiId, notes)
                        },
                        onDismiss = {
                            showWithdrawDialog = false
                            viewModel.resetWithdrawalAction()
                        }
                    )
                }

                if (showUpiDialog) {
                    UpdateUpiDialog(
                        currentUpiId = state.wallet.upiId,
                        onUpdate = { upiId ->
                            viewModel.updateUpiId(upiId)
                            showUpiDialog = false
                        },
                        onDismiss = { showUpiDialog = false }
                    )
                }
            }
            is WalletUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun WalletContent(
    wallet: PlumberWallet,
    transactions: List<Transaction>,
    withdrawalRequests: List<WithdrawalRequest>,
    onWithdrawClick: () -> Unit,
    onUpdateUpiId: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bento Hero 1: Withdrawable Balance (Neon Green Bento Card)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(BentoGreen)
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE TO WITHDRAW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = BentoOnVibrant.copy(alpha = 0.75f)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BentoOnVibrant.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BentoOnVibrant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = formatRupees(wallet.withdrawableBalance),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = BentoOnVibrant
                    )

                    Spacer(Modifier.height(18.dp))

                    // Tactile Withdraw Button
                    Surface(
                        onClick = onWithdrawClick,
                        enabled = wallet.withdrawableBalance > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        color = BentoOnVibrant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (wallet.withdrawableBalance > 0) "Withdraw Funds →" else "No funds to withdraw",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (wallet.withdrawableBalance > 0) BentoGreen else TextTertiary
                            )
                        }
                    }
                }
            }
        }

        // Bento Row 2: Side-by-side Escrow & Total Earnings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // In Escrow (Electric Blue)
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
                                text = "IN ESCROW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BentoOnVibrant.copy(alpha = 0.75f)
                            )
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = BentoOnVibrant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = formatRupees(wallet.escrowBalance),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BentoOnVibrant
                            )
                            Text(
                                text = "Safe till job sign-off",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoOnVibrant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Total Earnings (Electric Yellow)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(BentoYellow)
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
                                text = "TOTAL EARNED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BentoOnVibrant.copy(alpha = 0.75f)
                            )
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = BentoOnVibrant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = formatRupees(wallet.totalEarnings),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BentoOnVibrant
                            )
                            Text(
                                text = "Jobs & Commissions",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoOnVibrant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Commission Earning Callout Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PaperDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.LocalOffer,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Material Order Commissions",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Order pipes and fittings from Prince Sanitary for your customer sites to earn instant commissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // UPI ID Configuration
        item {
            GrahakCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PaperDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Payout UPI ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = wallet.upiId.ifEmpty { "No UPI ID added yet" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Ink
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PaperDim)
                            .border(1.dp, Line, CircleShape)
                            .clickable(onClick = onUpdateUpiId)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (wallet.upiId.isEmpty()) "Add" else "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricLime
                        )
                    }
                }
            }
        }

        // Pending Withdrawals
        if (withdrawalRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Withdrawal Requests",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(withdrawalRequests.take(3)) { request ->
                WithdrawalRequestCard(request)
            }
        }

        // Transaction History
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                GrahakCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No wallet transactions recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(transactions.take(10)) { transaction ->
                TransactionCard(transaction)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    val isCredit = transaction.type == TransactionType.COMMISSION_EARNED ||
        transaction.type == TransactionType.COMMISSION_RELEASED ||
        transaction.type == TransactionType.COMMISSION_RELEASE

    GrahakCard(cornerRadius = 18.dp, contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) BentoGreen.copy(alpha = 0.15f) else PaperDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isCredit) BentoGreen else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description.ifEmpty { transaction.type.name.replace("_", " ") },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink
                    )
                    Text(
                        text = formatDate(transaction.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "${if (isCredit) "+" else "-"}${formatRupees(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isCredit) BentoGreen else Ink
            )
        }
    }
}

@Composable
fun WithdrawalRequestCard(request: WithdrawalRequest) {
    GrahakCard(cornerRadius = 18.dp, contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatRupees(request.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
                Text(
                    text = "To: ${request.upiId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatDate(request.requestedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        when (request.status) {
                            WithdrawalStatus.PENDING -> StatusWarning.copy(alpha = 0.18f)
                            WithdrawalStatus.APPROVED -> BentoBlue.copy(alpha = 0.18f)
                            WithdrawalStatus.COMPLETED -> BentoGreen.copy(alpha = 0.18f)
                            WithdrawalStatus.REJECTED -> StatusError.copy(alpha = 0.18f)
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = request.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = when (request.status) {
                        WithdrawalStatus.PENDING -> StatusWarning
                        WithdrawalStatus.APPROVED -> BentoBlue
                        WithdrawalStatus.COMPLETED -> BentoGreen
                        WithdrawalStatus.REJECTED -> StatusError
                    }
                )
            }
        }
    }
}

@Composable
fun WithdrawalDialog(
    availableBalance: Double,
    currentUpiId: String,
    isProcessing: Boolean,
    error: String?,
    onWithdraw: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf(currentUpiId) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                "Withdraw Funds",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
        },
        text = {
            Column {
                Text(
                    text = "Available to withdraw: ${formatRupees(availableBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = BentoGreen
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)", color = TextSecondary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = Line,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID", color = TextSecondary) },
                    placeholder = { Text("e.g. name@okhdfcbank", color = TextTertiary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = Line,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", color = TextSecondary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = Line,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    onWithdraw(amt, upiId, notes)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
                enabled = !isProcessing && amount.toDoubleOrNull() != null && upiId.isNotEmpty()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BentoOnVibrant)
                } else {
                    Text("Request Withdrawal", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun UpdateUpiDialog(
    currentUpiId: String,
    onUpdate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var upiId by remember { mutableStateOf(currentUpiId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                "Update UPI ID",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
        },
        text = {
            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("UPI ID", color = TextSecondary) },
                placeholder = { Text("e.g. mobile@upi", color = TextTertiary) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(upiId) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
                enabled = upiId.isNotEmpty()
            ) {
                Text("Save UPI ID", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(date)
}
