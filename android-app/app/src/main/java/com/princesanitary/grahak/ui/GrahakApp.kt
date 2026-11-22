package com.princesanitary.grahak.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.auth.AuthScreen
import com.princesanitary.grahak.ui.auth.WelcomeScreen
import com.princesanitary.grahak.ui.consumer.ConsumerMainScreen
import com.princesanitary.grahak.ui.plumber.PlumberMainScreen
import com.princesanitary.grahak.ui.splash.FixItSplash
import com.princesanitary.grahak.ui.theme.Paper

/** Which half of the product a signed-in account belongs to. */
enum class UserRole {
    CONSUMER,
    PLUMBER,
    OWNER;

    val label: String
        get() = when (this) {
            CONSUMER -> "Customer"
            PLUMBER -> "Plumber"
            OWNER -> "Store Owner"
        }

    companion object {
        fun fromStored(value: String?): UserRole? = when (value) {
            "CONSUMER" -> CONSUMER
            "PLUMBER" -> PLUMBER
            "OWNER" -> OWNER
            else -> null
        }
    }
}

sealed interface SessionState {
    /** Still resolving the cached Firebase user + their stored role. */
    data object Loading : SessionState

    data object SignedOut : SessionState

    /** Signed in, but no role on the profile yet - ask before routing. */
    data class NeedsRole(val uid: String) : SessionState

    data class SignedIn(val uid: String, val role: UserRole) : SessionState
}

/**
 * Root of the app.
 *
 * Two things are layered here:
 *
 *  - the FixIt splash, which owns the screen until both its animation has run
 *    AND the session has resolved (so we never hand over to a spinner),
 *  - the app content, which switches on [SessionState].
 *
 * Content is composed underneath the splash from the start and revealed by
 * fading the splash away, so the handover is one continuous motion instead of a
 * screen swap.
 */
@Composable
fun GrahakApp() {
    val viewModel: RootViewModel = appViewModel()
    val session by viewModel.session.collectAsStateWithLifecycle()

    var splashAnimationDone by remember { mutableStateOf(false) }

    val sessionReady = session != SessionState.Loading
    val showSplash = !splashAnimationDone || !sessionReady

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // ---------------- app content (underneath) ----------------
        Crossfade(
            targetState = session,
            animationSpec = tween(220),
            label = "session"
        ) { state ->
            when (state) {
                // Nothing to draw yet; the splash is covering this.
                SessionState.Loading -> Box(Modifier.fillMaxSize().background(Paper))

                SessionState.SignedOut -> {
                    AuthScreen(
                        onBack = null,
                        onAuthenticated = { viewModel.refresh() }
                    )
                }

                is SessionState.NeedsRole -> AuthScreen(
                    onBack = null,
                    onAuthenticated = { viewModel.refresh() }
                )

                is SessionState.SignedIn -> when (state.role) {
                    UserRole.CONSUMER -> ConsumerMainScreen(
                        onLogout = { viewModel.signOut() }
                    )

                    UserRole.PLUMBER -> PlumberMainScreen(
                        onLogout = { viewModel.signOut() }
                    )

                    UserRole.OWNER -> com.princesanitary.grahak.ui.owner.OwnerMainScreen(
                        onLogout = { viewModel.signOut() }
                    )
                }
            }
        }

        // ---------------- splash (on top) ----------------
        AnimatedVisibility(
            visible = showSplash,
            // No enter animation: it is already on screen when the app starts.
            enter = fadeIn(tween(0)),
            // Fading + the splash's own scale-up makes the reveal feel like the
            // yellow surface lifting away from the dashboard.
            exit = fadeOut(tween(340))
        ) {
            FixItSplash(onFinished = { splashAnimationDone = true })
        }
    }
}
