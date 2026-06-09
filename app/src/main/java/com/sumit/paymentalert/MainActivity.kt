package com.sumit.paymentalert

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sumit.paymentalert.ui.screens.MainDashboardScreen
import com.sumit.paymentalert.ui.screens.QrScreen
import com.sumit.paymentalert.ui.screens.SettingsScreen
import com.sumit.paymentalert.ui.screens.LoginScreen
import com.sumit.paymentalert.ui.screens.SignupScreen
import com.sumit.paymentalert.ui.screens.RewardsScreen
import com.sumit.paymentalert.ui.theme.PaymentAlertTheme
import com.sumit.paymentalert.ui.viewmodel.PaymentViewModel
import com.sumit.paymentalert.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaymentAlertTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    val context = LocalContext.current
    val viewModel: PaymentViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()
    var authScreenState by remember { mutableStateOf("login") } // login, signup

    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("dashboard") } // dashboard, qr, settings
    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotificationPermission = granted
    }

    // 1. Permissions check & Splash Timer
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        delay(2200) // Delay for sleek Splash visual
        showSplash = false
    }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(600),
        label = "splash_to_content_fade"
    ) { splashState ->
        if (splashState) {
            SplashScreenLayout()
        } else {
            Crossfade(
                targetState = isUserLoggedIn,
                animationSpec = tween(500),
                label = "auth_state_fade"
            ) { loggedIn ->
                if (!loggedIn) {
                    Crossfade(
                        targetState = authScreenState,
                        animationSpec = tween(400),
                        label = "auth_screen_switch"
                    ) { state ->
                        if (state == "login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToSignup = { authScreenState = "signup" }
                            )
                        } else {
                            SignupScreen(
                                authViewModel = authViewModel,
                                onNavigateToLogin = { authScreenState = "login" }
                            )
                        }
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            BottomNavigationBar(
                                currentSelection = currentScreen,
                                onSelected = { currentScreen = it }
                            )
                        },
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { paddingValues ->
                        val modifier = Modifier.padding(paddingValues)
                        when (currentScreen) {
                            "dashboard" -> MainDashboardScreen(viewModel = viewModel, modifier = modifier)
                            "qr" -> QrScreen(viewModel = viewModel, modifier = modifier)
                            "rewards" -> RewardsScreen(viewModel = viewModel, modifier = modifier)
                            "settings" -> SettingsScreen(viewModel = viewModel, authViewModel = authViewModel, modifier = modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreenLayout() {
    // 1. Core State Animators for smooth entering transitions
    val logoScale = remember { Animatable(0.2f) }
    val logoAlpha = remember { Animatable(0.0f) }
    val logoRotation = remember { Animatable(-35f) } // Elegant spin-in entry
    
    val textAlpha = remember { Animatable(0.0f) }
    val textOffset = remember { Animatable(35f) }
    
    val footerAlpha = remember { Animatable(0.0f) }

    // 2. Continuous pulse animation state for outer glowing circular halos
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val haloScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_scale_1"
    )
    val haloAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_alpha_1"
    )

    val haloScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_scale_2"
    )
    val haloAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_alpha_2"
    )

    LaunchedEffect(Unit) {
        // Animate Logo scale with a beautiful spring bouncy effect
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        
        // Logo alpha
        launch {
            logoAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }

        // Logo rotation
        launch {
            logoRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Text slide up and fade with delay
        delay(400)
        launch {
            textAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(1000, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            textOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(1000, easing = LinearOutSlowInEasing)
            )
        }

        // Footer fade in
        delay(400)
        launch {
            footerAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(1000, easing = LinearOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Main Logo Frame with Pulsing Glowing Radar Rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Outer Pulse Ring (Radar Waves 2)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(
                            scaleX = logoScale.value * haloScale2,
                            scaleY = logoScale.value * haloScale2,
                            alpha = logoAlpha.value * haloAlpha2
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )

                // Inner Pulse Ring (Radar Waves 1)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(
                            scaleX = logoScale.value * haloScale1,
                            scaleY = logoScale.value * haloScale1,
                            alpha = logoAlpha.value * haloAlpha1
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                )

                // Custom adaptive ic_app logo with elegant glowing shadow and gradient background bounds
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(
                            scaleX = logoScale.value,
                            scaleY = logoScale.value,
                            rotationZ = logoRotation.value,
                            alpha = logoAlpha.value
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Displays the user's uploaded icon (ic_app)
                    Image(
                        painter = painterResource(id = R.drawable.ic_app),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(114.dp)
                            .clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text column sliding and fading
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer(
                        translationY = textOffset.value,
                        alpha = textAlpha.value
                    )
            ) {
                Text(
                    text = "Payment Alert",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Smart Voice Notifications Assistant",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Beautiful interactive voice payment soundwave
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(40.dp)
                ) {
                    val waveHeights = listOf(14.dp, 26.dp, 40.dp, 20.dp, 34.dp, 16.dp, 24.dp)
                    val waveDurations = listOf(650, 800, 500, 700, 600, 750, 550)
                    
                    waveHeights.forEachIndexed { i, baseHeight ->
                        val barScale by infiniteTransition.animateFloat(
                            initialValue = 0.25f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = waveDurations[i], easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wave_bar_$i"
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(baseHeight * barScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Author footer with elegant animated alpha
        Text(
            text = "Powered by Sumit Kumar",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(footerAlpha.value)
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentSelection: String,
    onSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Dashboard Tab
                BottomNavItem(
                    isSelected = currentSelection == "dashboard",
                    label = "Dashboard",
                    icon = Icons.Default.Home,
                    activeIcon = Icons.Default.Home,
                    onClick = { onSelected("dashboard") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // 2. QR Tab
                BottomNavItem(
                    isSelected = currentSelection == "qr",
                    label = "My QR",
                    icon = Icons.Default.Share,
                    activeIcon = Icons.Default.Share,
                    onClick = { onSelected("qr") },
                    modifier = Modifier.testTag("nav_tab_qr")
                )

                // 3. Rewards Tab
                BottomNavItem(
                    isSelected = currentSelection == "rewards",
                    label = "Rewards",
                    icon = Icons.Default.Star,
                    activeIcon = Icons.Default.Star,
                    onClick = { onSelected("rewards") },
                    modifier = Modifier.testTag("nav_tab_rewards")
                )

                // 4. Settings Tab
                BottomNavItem(
                    isSelected = currentSelection == "settings",
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    activeIcon = Icons.Default.Settings,
                    onClick = { onSelected("settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    isSelected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "icon_color"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(450),
        label = "pill_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "text_color"
    )

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null, // Custom clean indicator replaces the default material ripple
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .width(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(pillColor)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) activeIcon else icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.2.sp
        )
    }
}

// Check notification permissions function helper
fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, "com.sumit.paymentalert.service.NotificationListener")
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}
