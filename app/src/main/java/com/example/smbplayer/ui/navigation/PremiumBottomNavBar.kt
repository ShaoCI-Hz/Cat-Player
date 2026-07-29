package com.example.smbplayer.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Premium animated bottom navigation bar with:
 * - Elastic bouncing indicator (pill shape)
 * - Icon scale + color animation
 * - Glassmorphism background
 * - Spring physics transitions
 * - Bounce translateY effect on tap
 */
@Composable
fun PremiumBottomNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavTab>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Track bounce animation per tab
    val bounceAnimatables = remember { List(tabs.size) { Animatable(0f) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Glassmorphism background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .drawBehind {
                    // Frosted glass effect simulation
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex

                    // Icon scale animation
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "iconScale_$index"
                    )

                    // Color animation
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF1ED760) else Color.White.copy(alpha = 0.5f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconColor_$index"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF1ED760) else Color.Transparent,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "textColor_$index"
                    )

                    // Bounce translateY
                    val bounceY = bounceAnimatables[index].value

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                translationY = bounceY
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(index)
                                // Trigger bounce animation
                                scope.launch {
                                    bounceAnimatables[index].snapTo(-4f)
                                    bounceAnimatables[index].animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.3f,
                                            stiffness = 600f
                                        )
                                    )
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(iconScale),
                            tint = iconColor
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Animated indicator (pill shape) - positioned under selected tab
        val indicatorOffset by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.5f,
                stiffness = 500f
            ),
            label = "indicatorOffset"
        )

        // Calculate indicator position
        val tabWidth = 1f / tabs.size
        val indicatorX = indicatorOffset * tabWidth + tabWidth / 2f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Indicator pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 4.dp)
                    .graphicsLayer {
                        translationX = (indicatorX - 0.5f) * size.width
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1ED760).copy(alpha = 0.6f),
                                    Color(0xFF1ED760)
                                )
                            )
                        )
                )
            }
        }
    }
}
