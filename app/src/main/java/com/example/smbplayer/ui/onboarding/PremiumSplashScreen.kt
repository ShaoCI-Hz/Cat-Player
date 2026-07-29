package com.example.smbplayer.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Premium splash screen with:
 * - Aurora gradient background with flowing glow
 * - Logo entrance with spring bounce
 * - Character-by-character text fade-in
 * - Bottom button with glass background
 * - Breathing scale animation
 */
@Composable
fun PremiumSplashScreen(
    onGetStarted: () -> Unit
) {
    // Aurora flowing animation
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val auroraOffset1 by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora1"
    )
    val auroraOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora2"
    )
    val auroraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraAlpha"
    )

    // Breathing animation
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Logo entrance animation
    var logoVisible by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1.0f else 0.5f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = 200f
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1.0f else 0f,
        animationSpec = tween(800),
        label = "logoAlpha"
    )

    // Text animation
    var textVisible by remember { mutableStateOf(false) }
    val welcomeText = "欢迎使用 Cat Player"

    // Button animation
    var buttonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        logoVisible = true
        delay(1000)
        textVisible = true
        delay(welcomeText.length * 100L + 500)
        buttonVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .drawBehind {
                // Aurora background effect
                val cx1 = size.width * (0.5f + auroraOffset1)
                val cy1 = size.height * 0.3f
                val r1 = size.width * 0.8f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF667eea).copy(alpha = auroraAlpha * 0.5f),
                            Color(0xFF764ba2).copy(alpha = auroraAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(cx1, cy1),
                        radius = r1
                    ),
                    center = Offset(cx1, cy1),
                    radius = r1
                )

                val cx2 = size.width * (0.5f + auroraOffset2)
                val cy2 = size.height * 0.7f
                val r2 = size.width * 0.6f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF764ba2).copy(alpha = auroraAlpha * 0.4f),
                            Color(0xFF667eea).copy(alpha = auroraAlpha * 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(cx2, cy2),
                        radius = r2
                    ),
                    center = Offset(cx2, cy2),
                    radius = r2
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // Logo with entrance animation + breathing
            AnimatedVisibility(
                visible = logoVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = 200f
                    )
                ) + fadeIn(animationSpec = tween(800))
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale * breathScale)
                        .graphicsLayer(alpha = logoAlpha)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1ED760).copy(alpha = 0.3f),
                                    Color(0xFF1ED760).copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFF1ED760)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Character-by-character text
            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                Row {
                    welcomeText.forEachIndexed { index, char ->
                        var charVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 100L)
                            charVisible = true
                        }
                        AnimatedVisibility(
                            visible = charVisible,
                            enter = fadeIn(tween(200)) + scaleIn(
                                animationSpec = spring(
                                    dampingRatio = 0.6f,
                                    stiffness = 400f
                                )
                            )
                        ) {
                            Text(
                                text = char.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Subtitle
            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn(tween(500, delayMillis = 500)) + slideInVertically(
                    animationSpec = tween(400, delayMillis = 500)
                ) { it / 4 }
            ) {
                Text(
                    "SMB/SFTP/WebDAV 直连 NAS 播放",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.weight(1f))

            // Bottom button with glass background
            AnimatedVisibility(
                visible = buttonVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 300f
                    )
                ) { it } + fadeIn(animationSpec = tween(400))
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1ED760)
                    )
                ) {
                    Text(
                        "开始使用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
