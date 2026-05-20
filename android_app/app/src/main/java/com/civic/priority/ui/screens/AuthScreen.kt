package com.civic.priority.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius


// ─── Floating Particles Background ───

@Composable
fun FloatingParticles() {
    val particles = remember {
        (0 until 20).map {
            ParticleData(
                x = Random.nextFloat() * 360f - 180f,
                y = Random.nextFloat() * 800f - 400f,
                size = Random.nextFloat() * 2.5f + 1.5f,
                opacity = Random.nextFloat() * 0.17f + 0.08f,
                speed = Random.nextFloat() * 6f + 4f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI.toFloat() * 2f * 20f),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val offsetX = p.x + sin(drift / p.speed) * 20f
            val offsetY = p.y + cos(drift / p.speed) * 15f
            drawCircle(
                color = CivicColors.Glow.copy(alpha = p.opacity),
                radius = p.size,
                center = Offset(
                    size.width / 2f + offsetX,
                    size.height / 2f + offsetY
                )
            )
        }
    }
}

private data class ParticleData(
    val x: Float, val y: Float, val size: Float, val opacity: Float, val speed: Float
)

// ─── Glowing Arc Background ───

@Composable
fun GlowArc(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    val brightness by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "brightness"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Outer glow
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    CivicColors.Glow.copy(alpha = 0.3f * brightness),
                    CivicColors.GlowDeep.copy(alpha = 0.12f * brightness),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = 280f * pulse
            ),
            topLeft = Offset(cx - 250f * pulse, cy - 200f * pulse),
            size = androidx.compose.ui.geometry.Size(500f * pulse, 400f * pulse)
        )
    }
}

// ─── Shimmer Text ───

@Composable
fun ShimmerText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerOffset"
    )

    Text(
        text = text,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.headlineLarge.copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    CivicColors.Button,
                    Color.White.copy(alpha = 0.35f),
                    CivicColors.ButtonLt
                ),
                start = Offset(shimmerOffset * 300f, 0f),
                end = Offset(shimmerOffset * 300f + 200f, 0f)
            )
        )
    )
}

// ─── Civic Logo ───

@Composable
fun CivicLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val rotateY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing, delayMillis = 3000),
            repeatMode = RepeatMode.Restart
        ), label = "rotateY"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                rotationY = rotateY
                cameraDistance = 12f * density
            }
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(CivicColors.Button, CivicColors.ButtonLt)
                ),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🏛",
            fontSize = 16.sp
        )
    }
}

// ─── Pulsing Button ───

@Composable
fun PulsingButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = CivicColors.Button.copy(alpha = if (enabled) glowAlpha else 0f),
                spotColor = CivicColors.Button.copy(alpha = if (enabled) glowAlpha else 0f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CivicColors.Button, CivicColors.ButtonLt)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

// ─── AuthScreen ───

@Composable
fun AuthScreen(viewModel: AppViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // Appear animation
    var appeared by remember { mutableStateOf(false) }
    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(800), label = "appear"
    )
    val appearOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 50f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f), label = "offset"
    )

    // Gentle vertical float animation for the card
    val cardFloatTransition = rememberInfiniteTransition(label = "cardFloat")
    val floatOffset by cardFloatTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardFloatOffset"
    )

    val borderAngle by cardFloatTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderAngle"
    )

    LaunchedEffect(Unit) {
        delay(100)
        appeared = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CivicColors.LoginBg)
    ) {
        // Floating particles background
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ── Logo + Shimmer Title ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(appearAlpha)
            ) {
                CivicLogo()
                Spacer(modifier = Modifier.width(10.dp))
                ShimmerText("CivicPriority")
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(appearAlpha)
                    .offset(y = (appearOffset + floatOffset).dp)
            ) {
                // Glow arc behind card
                GlowArc(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .offset(y = 30.dp)
                )

                // Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = CivicColors.LoginCardBg.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .drawWithContent {
                            drawContent()
                            val angleRad = Math.toRadians(borderAngle.toDouble()).toFloat()
                            val cosVal = cos(angleRad)
                            val sinVal = sin(angleRad)
                            val startOffset = Offset(
                                x = size.width / 2f + cosVal * size.width / 2f,
                                y = size.height / 2f + sinVal * size.height / 2f
                            )
                            val endOffset = Offset(
                                x = size.width / 2f - cosVal * size.width / 2f,
                                y = size.height / 2f - sinVal * size.height / 2f
                            )
                            val brush = Brush.linearGradient(
                                colors = listOf(
                                    CivicColors.Primary.copy(alpha = 0.8f),
                                    CivicColors.ButtonLt.copy(alpha = 0.5f),
                                    Color(0xFFCE93D8).copy(alpha = 0.5f),
                                    CivicColors.Primary.copy(alpha = 0.8f)
                                ),
                                start = startOffset,
                                end = endOffset
                            )
                            drawOutline(
                                outline = Outline.Rounded(
                                    RoundRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width,
                                        bottom = size.height,
                                        cornerRadius = CornerRadius(16.dp.toPx())
                                    )
                                ),
                                brush = brush,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = if (isLoginMode) "Login" else "Register",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = if (isLoginMode) "Log in to continue to your dashboard"
                        else "Create your community account",
                        fontSize = 12.sp,
                        color = CivicColors.Subtitle
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Username field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Your@email.com", color = CivicColors.Subtitle) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CivicColors.LoginFieldBg,
                            unfocusedContainerColor = CivicColors.LoginFieldBg,
                            focusedBorderColor = CivicColors.Button.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                            cursorColor = CivicColors.Button
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = CivicColors.Subtitle) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CivicColors.LoginFieldBg,
                            unfocusedContainerColor = CivicColors.LoginFieldBg,
                            focusedBorderColor = CivicColors.Button.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                            cursorColor = CivicColors.Button
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (isLoginMode) {
                        Text(
                            "Forgot password?",
                            fontSize = 11.sp,
                            color = CivicColors.Button,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm password (register mode)
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Confirm password", color = CivicColors.Subtitle) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = CivicColors.LoginFieldBg,
                                    unfocusedContainerColor = CivicColors.LoginFieldBg,
                                    focusedBorderColor = CivicColors.Button.copy(alpha = 0.3f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                                    cursorColor = CivicColors.Button
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Error message
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color.Red.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        msg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Red
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Success message
                    AnimatedVisibility(
                        visible = showSuccess,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.Green.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Welcome!", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Green)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Action button
                    PulsingButton(
                        label = if (isLoginMode) "Log In" else "Create Account",
                        enabled = username.isNotEmpty() && password.isNotEmpty()
                    ) {
                        errorMessage = null
                        showSuccess = false
                        if (isLoginMode) {
                            val (success, error) = viewModel.login(username, password)
                            if (!success) errorMessage = error
                        } else {
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match."
                                return@PulsingButton
                            }
                            val (success, error) = viewModel.register(username, password)
                            if (!success) errorMessage = error
                            else showSuccess = true
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle login/register
                    TextButton(onClick = {
                        isLoginMode = !isLoginMode
                        errorMessage = null
                        confirmPassword = ""
                    }) {
                        Text(
                            text = if (isLoginMode) "Don't have an account? Register"
                            else "Already have an account? Log In",
                            fontSize = 11.sp,
                            color = CivicColors.Button
                        )
                    }

                    // Hint badges
                    AnimatedVisibility(
                        visible = isLoginMode,
                        enter = expandVertically() + fadeIn(animationSpec = tween(500, delayMillis = 100)),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                HintBadge("admin", "admin123") {
                                    username = "admin"
                                    password = "admin123"
                                }
                                HintBadge("john_doe", "john123") {
                                    username = "john_doe"
                                    password = "john123"
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HintBadge(user: String, pass: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            user,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = CivicColors.ButtonLt
        )
        Text(
            pass,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = CivicColors.Subtitle.copy(alpha = 0.6f)
        )
    }
}
