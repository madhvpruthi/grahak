package com.princesanitary.grahak.ui.consumer.services

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.theme.Amber
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary

/**
 * Rating prompt shown after a job is completed.
 *
 * Feeds the `reviews` collection and the plumber's running average. Until this
 * existed nothing ever wrote `stats.rating`, so the stars on the plumber list
 * were decorative and customers had no basis for choosing between plumbers.
 *
 * The comment is optional on purpose - requiring prose collapses response rates,
 * and a bare star rating is still useful.
 */
@Composable
fun RateJobSheet(
    plumberName: String,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = SurfaceWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Line)
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "How did it go?",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Rate $plumberName so other customers know what to expect.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    val active = star <= rating
                    // A small pop on the selected stars makes the tap feel answered.
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.12f else 1f,
                        animationSpec = tween(160),
                        label = "star$star"
                    )
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "$star star${if (star == 1) "" else "s"}",
                        tint = if (active) Amber else Line,
                        modifier = Modifier
                            .size(42.dp)
                            .scale(scale)
                            .clickable { rating = star }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = when (rating) {
                    0 -> "Tap a star"
                    1 -> "Poor"
                    2 -> "Not great"
                    3 -> "Fine"
                    4 -> "Good"
                    else -> "Excellent"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (rating == 0) TextTertiary else Ink
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(300) },
                placeholder = {
                    Text(
                        "Add a note (optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Paper,
                    unfocusedContainerColor = Paper,
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Line,
                    cursorColor = Ink
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                ErrorBanner(error)
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "Submit rating",
                enabled = rating > 0 && !submitting,
                loading = submitting,
                onClick = { onSubmit(rating, comment) }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Skip for now",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
