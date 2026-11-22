package com.princesanitary.grahak.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.princesanitary.grahak.R
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.UserRole
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.theme.LuxeGold

private enum class AuthMode { LOGIN, SIGN_UP }

/**
 * Luxury Dark Auth Screen.
 * Inspired by modern premium black aesthetics with high-contrast surfaces,
 * refined capsule tags, and Luxe Gold accents.
 */
@Composable
fun AuthScreen(
    onBack: (() -> Unit)?,
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = appViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var role by remember { mutableStateOf(UserRole.CONSUMER) }
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signedInAs) {
        if (uiState.signedInAs != null) onAuthenticated()
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            viewModel.signInWithGoogle(
                idToken = account.idToken,
                role = role,
                displayName = account.displayName,
                email = account.email,
                phone = phone
            )
        } catch (e: ApiException) {
            viewModel.onGoogleSignInFailed(e)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (onBack != null) {
                BackButton(onClick = onBack)
                Spacer(Modifier.height(16.dp))
            }

            // ---------- 1. Luxury Brand Mark ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Mark: fixit. with gold accent
                Text(
                    text = buildAnnotatedString {
                        append("fixit")
                        withStyle(SpanStyle(color = LuxeGold, fontWeight = FontWeight.Black)) {
                            append(".")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(18.dp))

            // Headline
            Text(
                text = if (mode == AuthMode.SIGN_UP) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (mode == AuthMode.SIGN_UP) {
                    "Join Prince Sanitary & Hardware hub."
                } else {
                    "Sign in to access your orders and saved profile."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA1A1AA)
            )

            Spacer(Modifier.height(20.dp))

            // ---------- 2. Role Selector (Customer / Plumber) ----------
            TwoRoleSegmentedToggle(
                selectedRole = role,
                onSelectRole = { newRole ->
                    role = newRole
                    viewModel.clearError()
                }
            )

            Spacer(Modifier.height(14.dp))

            // ---------- 3. Google Sign-In ----------
            Surface(
                onClick = {
                    val options = GoogleSignInOptions
                        .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()

                    val client = GoogleSignIn.getClient(context, options)
                    client.signOut()
                    googleLauncher.launch(client.signInIntent)
                },
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141418),
                border = BorderStroke(1.dp, Color(0xFF27272A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (mode == AuthMode.SIGN_UP) "Sign up with Google" else "Continue with Google",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFF27272A))
                Text(
                    text = "  or sign in with email  ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF71717A)
                )
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFF27272A))
            }

            Spacer(Modifier.height(20.dp))

            uiState.error?.let { message ->
                ErrorBanner(message)
                Spacer(Modifier.height(16.dp))
            }

            // ---------- 3. Input Form Card ----------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141418),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    AnimatedVisibility(visible = mode == AuthMode.SIGN_UP) {
                        Column {
                            LuxuryFieldLabel("FULL NAME")
                            LuxuryAuthTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = "e.g. Madhav Sharma",
                                keyboardType = KeyboardType.Text,
                                leadingIcon = Icons.Rounded.Person
                            )
                            Spacer(Modifier.height(16.dp))

                            LuxuryFieldLabel("MOBILE NUMBER")
                            LuxuryAuthTextField(
                                value = phone,
                                onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                                placeholder = "10-digit mobile number",
                                keyboardType = KeyboardType.Phone,
                                leadingIcon = Icons.Rounded.Phone
                            )
                            Text(
                                text = "Required for order delivery confirmation & site dispatches.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF71717A),
                                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp)
                            )
                        }
                    }

                    LuxuryFieldLabel("EMAIL ADDRESS")
                    LuxuryAuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "you@example.com",
                        keyboardType = KeyboardType.Email,
                        leadingIcon = Icons.Rounded.Email
                    )

                    Spacer(Modifier.height(16.dp))

                    LuxuryFieldLabel("PASSWORD")
                    LuxuryAuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = if (mode == AuthMode.SIGN_UP) "At least 6 characters" else "Enter your password",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        leadingIcon = Icons.Rounded.Lock,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFFA1A1AA)
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(22.dp))

                    // Primary Button: Luxe Gold high-tactile button
                    Surface(
                        onClick = {
                            if (mode == AuthMode.SIGN_UP) {
                                viewModel.signUpWithEmail(name, email, phone, password, role)
                            } else {
                                viewModel.signInWithEmail(email, password, role)
                            }
                        },
                        enabled = !uiState.isLoading,
                        shape = CircleShape,
                        color = LuxeGold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF09090B),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (mode == AuthMode.SIGN_UP) "Create Account" else "Log In",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color(0xFF09090B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ---------- 4. Switch between Sign Up & Log In ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == AuthMode.SIGN_UP) {
                        "Already have an account?"
                    } else {
                        "New to Prince Sanitary?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA1A1AA)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (mode == AuthMode.SIGN_UP) "Log In" else "Sign Up",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = LuxeGold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            mode = if (mode == AuthMode.SIGN_UP) AuthMode.LOGIN else AuthMode.SIGN_UP
                            viewModel.clearError()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        if (uiState.requiresOwnerVerification) {
            OwnerVerificationDialog(
                pendingEmail = uiState.pendingOwnerEmail ?: "",
                error = uiState.error,
                isLoading = uiState.isLoading,
                onVerify = viewModel::verifyOwnerVerificationCode,
                onCancel = viewModel::cancelOwnerVerification
            )
        }
    }
}

@Composable
private fun OwnerVerificationDialog(
    pendingEmail: String,
    error: String?,
    isLoading: Boolean,
    onVerify: (String) -> Unit,
    onCancel: () -> Unit
) {
    var verificationCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141418),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(LuxeGold.copy(alpha = 0.15f))
                        .border(1.dp, LuxeGold.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = LuxeGold, modifier = Modifier.size(24.dp))
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "STORE OWNER SECURITY CHECK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    ),
                    color = LuxeGold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Enter Master Passcode",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Owner account detected ($pendingEmail). Enter your 6-digit Master PIN to unlock store operations.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                    color = Color(0xFFA1A1AA)
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) verificationCode = it },
                    label = { Text("6-digit Master PIN") },
                    placeholder = { Text("••••••", color = Color(0xFF71717A)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 6.sp,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedBorderColor = LuxeGold,
                        unfocusedBorderColor = Color(0xFF27272A),
                        cursorColor = LuxeGold
                    )
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color(0xFFEF4444)),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(20.dp))

                Surface(
                    onClick = {
                        if (verificationCode.length >= 4) {
                            onVerify(verificationCode)
                        }
                    },
                    shape = CircleShape,
                    color = LuxeGold,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF09090B),
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Verify & Access Store",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF09090B)
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        }
    }
}

@Composable
private fun TwoRoleSegmentedToggle(
    selectedRole: UserRole,
    onSelectRole: (UserRole) -> Unit
) {
    val roles = listOf(
        UserRole.CONSUMER to "Customer",
        UserRole.PLUMBER to "Plumber"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF27272A), CircleShape)
            .padding(4.dp)
    ) {
        roles.forEach { (roleItem, label) ->
            val selected = roleItem == selectedRole
            val bg by animateColorAsState(
                if (selected) LuxeGold else Color.Transparent,
                label = "roleBg"
            )
            val fg by animateColorAsState(
                if (selected) Color(0xFF09090B) else Color(0xFFA1A1AA),
                label = "roleFg"
            )

            Surface(
                onClick = { onSelectRole(roleItem) },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = CircleShape,
                color = bg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = fg
                    )
                }
            }
        }
    }
}

@Composable
private fun LuxuryRoleBlurb(role: UserRole) {
    val (icon, copy) = when (role) {
        UserRole.CONSUMER -> Icons.Rounded.Person to
            "Shop sanitaryware & pipes at store rates. Book verified local plumbers."
        UserRole.PLUMBER -> Icons.Rounded.Plumbing to
            "Accept nearby installation jobs, provide site estimates, and earn commissions."
        UserRole.OWNER -> Icons.Rounded.Storefront to
            "Manage store inventory, site dispatches, plumbers, and store accounts."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(LuxeGold.copy(alpha = 0.15f))
                .border(1.dp, LuxeGold.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = LuxeGold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = copy,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            color = Color(0xFFA1A1AA)
        )
    }
}

@Composable
private fun LuxuryFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.8.sp
        ),
        color = Color(0xFF71717A),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun LuxuryAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    leadingIcon: ImageVector,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = Color(0xFF52525B)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFFA1A1AA),
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Color.White,
            fontSize = 14.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF18181B),
            unfocusedContainerColor = Color(0xFF18181B),
            focusedBorderColor = LuxeGold,
            unfocusedBorderColor = Color(0xFF27272A),
            cursorColor = LuxeGold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    )
}
