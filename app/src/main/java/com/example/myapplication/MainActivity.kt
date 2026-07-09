package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopCollapsed by remember { mutableStateOf(true) }

    fun addFloatingText(text: String, x: Float, y: Float) {
        val id = System.currentTimeMillis()
        floatingTexts = floatingTexts + FloatingTextData(id, text, x, y)
        scope.launch {
            delay(700)
            floatingTexts = floatingTexts.filter { it.id != id }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070912), Color(0xFF1a2240), Color(0xFF070912))
                )
            )
    ) {
        // Stars background
        repeat(70) {
            Star()
        }

        // Active Debris for Drones
        state.scavengeTargets.forEach { target ->
            DebrisTarget(target)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(state, viewModel.calculateDPS())
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Event Banner
                state.activeEvent?.let { event ->
                    EventBanner(event)
                }

                // Planet Button in Center
                PlanetButton(
                    planetId = state.currentPlanetId,
                    planetConfig = viewModel.planets[state.currentPlanetId] ?: viewModel.planets.values.first(),
                    modifier = Modifier.align(Alignment.Center)
                ) { x, y ->
                    val value = viewModel.onPlanetClick()
                    addFloatingText("+${formatNum(value)}", x, y)
                }

                // Drones
                state.drones.forEach { drone ->
                    ScavengingDrone(drone)
                }

                // Interactive Events
                state.activeEvent?.let { event ->
                    when (event.type) {
                        GameEventType.ASTEROID -> {
                            Asteroid(event) { viewModel.onAsteroidClick() }
                        }
                        GameEventType.PIRATES -> {
                            PirateTarget(event, state.pirateTapsLeft) { viewModel.onPirateClick() }
                        }
                        else -> {}
                    }
                }

                // Floating Texts
                floatingTexts.forEach { data ->
                    FloatingText(data)
                }
            }

            ShopBar(
                viewModel = viewModel,
                state = state,
                isCollapsed = isShopCollapsed,
                onToggleCollapse = { isShopCollapsed = !isShopCollapsed }
            )
        }
    }
}

@Composable
fun ScavengingDrone(drone: DroneData, viewModel: GameViewModel = viewModel()) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    val size = when(drone.type) {
        "walker" -> 16.dp
        "station", "portal", "railgun" -> 40.dp
        else -> 28.dp
    }

    val fleetItem = viewModel.fleetItems.find { it.id == drone.type }

    Box(
        modifier = Modifier
            .offset(
                x = (drone.x * screenWidth).dp - (size / 2),
                y = (drone.y * (screenHeight - 350)).dp - (size / 2)
            )
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        fleetItem?.let {
            Image(
                painter = painterResource(id = it.iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Cargo
        if (drone.hasCargo) {
            Box(
                modifier = Modifier
                    .size(size / 3)
                    .background(Color(0xFF8B4513), RoundedCornerShape(1.dp)) 
                    .align(Alignment.BottomCenter)
                    .offset(y = size / 6)
            )
        }
    }
}

@Composable
fun DebrisTarget(target: ScavengeTarget) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    Box(
        modifier = Modifier
            .offset(
                x = (target.x * screenWidth).dp - 15.dp,
                y = (target.y * (screenHeight - 350)).dp - 15.dp
            )
            .size(30.dp)
            .alpha(0.6f),
        contentAlignment = Alignment.Center
    ) {
        Text("⚙️", fontSize = 16.sp)
    }
}

@Composable
fun Star() {
    val x = remember { Random.nextFloat() }
    val y = remember { Random.nextFloat() }
    val size = remember { Random.nextFloat() * 2 + 1 }
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000 + Random.nextInt(2000)), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .offset(
                x = (x * LocalConfiguration.current.screenWidthDp).dp,
                y = (y * LocalConfiguration.current.screenHeightDp).dp
            )
            .size(size.dp)
            .background(Color.White.copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun Header(state: GameState, dps: Double) {
    Column(
        modifier = Modifier
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatNum(state.totalDebris),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4fe3b3)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "debris", color = Color.Gray, fontSize = 14.sp)
        }
        Text(
            text = "+${formatNum(dps)} / sec",
            color = Color(0xFFbfe6da),
            fontSize = 14.sp
        )

        if (state.isHotelDebtActive) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hotel Debt", color = Color(0xFFff6464), fontSize = 12.sp)
                    Text(formatNum(state.currentHotelDebt), color = Color(0xFFff6464), fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (state.currentHotelDebt / 1000000.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Color(0xFFff6464),
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }
        }
    }
}

@Composable
fun PlanetButton(planetId: String, planetConfig: PlanetConfig, modifier: Modifier, onClick: (Float, Float) -> Unit) {
    val mainColor = planetConfig.color
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale, label = "")
    val isLocked = planetConfig.price < 0

    Box(
        modifier = modifier
            .size(240.dp)
            .scale(animatedScale)
            .let { 
                if (!isLocked) it.shadow(60.dp, CircleShape, ambientColor = mainColor, spotColor = mainColor)
                else it
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!isLocked) {
                    scale = 0.92f
                    onClick(0f, 0f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(scale) { if (scale < 1f) { delay(80); scale = 1f } }

        Image(
            painter = painterResource(id = planetConfig.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().let { if(isLocked) it.alpha(0.5f) else it }
        )
    }
}

@Composable
fun EventBanner(event: GameEvent) {
    val color = if (event.type == GameEventType.STORM) Color(0xFFffce54) else Color(0xFFff6464)
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = event.title,
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun Asteroid(event: GameEvent, onClick: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    Box(
        modifier = Modifier
            .offset(
                x = (event.x * (screenWidth - 60)).dp,
                y = (event.y * (screenHeight - 350)).dp
            )
            .size(50.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ast),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PirateTarget(event: GameEvent, tapsLeft: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 100.dp)
                .size(80.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .shadow(10.dp, CircleShape, spotColor = Color.Red)
                .border(2.dp, Color.Red, CircleShape)
                .clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏴‍☠️", fontSize = 32.sp)
            Text("${5 - tapsLeft}/5", color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
fun FloatingText(data: FloatingTextData) {
    val alpha by animateFloatAsState(targetValue = 0f, animationSpec = tween(700), label = "")
    val offset by animateIntOffsetAsState(
        targetValue = IntOffset(0, -100),
        animationSpec = tween(700),
        label = ""
    )
    
    Box(
        modifier = Modifier
            .offset { offset }
            .alpha(alpha)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = data.text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.shadow(4.dp, CircleShape)
        )
    }
}

data class FloatingTextData(val id: Long, val text: String, val x: Float, val y: Float)

@Composable
fun ShopBar(
    viewModel: GameViewModel,
    state: GameState,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Planets", "Fleet", "Click")
    val animatedHeight by animateDpAsState(targetValue = if (isCollapsed) 60.dp else 350.dp, label = "")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0e1320)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Drag handle / Toggle button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clickable { onToggleCollapse() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }

            if (!isCollapsed) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, title ->
                        Button(
                            onClick = { selectedTab = index },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == index) Color(0xFF4fe3b3).copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (selectedTab == index) Color(0xFF4fe3b3) else Color.Gray
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTab == index) Color(0xFF4fe3b3) else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(title, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> {
                            items(viewModel.planets.toList()) { (id, config) ->
                                val active = state.currentPlanetId == id
                                PlanetRow(
                                    name = config.name,
                                    desc = config.desc,
                                    price = config.price.toLong(),
                                    active = active,
                                    canBuy = state.totalDebris >= config.price && !active,
                                    iconRes = config.imageRes
                                ) { viewModel.buyPlanet(id) }
                            }
                        }
                        1 -> {
                            items(viewModel.fleetItems) { item ->
                                val count = state.fleetCounts[item.id] ?: 0
                                var cost = (item.base * 1.15.pow(count.toDouble())).toLong()
                                if (state.currentPlanetId == "mechan_x") cost = (cost * 0.85).toLong()
                                ShopRow(
                                    name = item.name,
                                    meta = "+${item.rate} / sec • qty $count",
                                    cost = cost,
                                    canBuy = state.totalDebris >= cost,
                                    iconRes = item.iconRes
                                ) { viewModel.buyFleet(item.id) }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.takeHotelDebt() },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    enabled = !state.isHotelDebtActive,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6464).copy(alpha = 0.1f), contentColor = Color(0xFFFF6464)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6464))
                                ) {
                                    Text(if (state.isHotelDebtActive) "Debt Active" else "Take Hotel Debt (x5 DPS!)")
                                }
                            }
                        }
                        2 -> {
                            items(viewModel.clickItems) { item ->
                                val lvl = state.clickLevels[item.id] ?: 0
                                val cost = (item.base * 1.15.pow(lvl.toDouble())).toLong()
                                ShopRow(
                                    name = item.name,
                                    meta = "+${item.value.toLong()} per tap • lvl $lvl",
                                    cost = cost,
                                    canBuy = state.totalDebris >= cost,
                                    iconRes = item.iconRes
                                ) { viewModel.buyClickUpgrade(item.id) }
                            }
                        }
                    }
                }
            } else {
                // Minimized view info
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("SHOP (TAP TO OPEN)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShopRow(name: String, meta: String, cost: Long, canBuy: Boolean, iconRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(meta, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Button(
            onClick = onClick,
            enabled = canBuy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4fe3b3), contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(formatNum(cost.toDouble()), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlanetRow(name: String, desc: String, price: Long, active: Boolean, canBuy: Boolean, iconRes: Int, onClick: () -> Unit) {
    val isLocked = price < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (active) Color(0xFF4fe3b3).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, if (active) Color(0xFF4fe3b3) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape)) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().let { if(isLocked) it.alpha(0.3f) else it }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(if(isLocked) "???" else name, color = if(isLocked) Color.Gray else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Button(
            onClick = onClick,
            enabled = canBuy && !isLocked,
            colors = ButtonDefaults.buttonColors(containerColor = if (active || isLocked) Color.Gray else Color(0xFF4fe3b3), contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isLocked) "Locked" else if (active) "Active" else if (price == 0L) "Free" else formatNum(price.toDouble()), fontSize = 12.sp)
        }
    }
}

fun formatNum(n: Double): String {
    if (n < 1000) return n.roundToLong().toString()
    val suffixes = listOf("K", "M", "B", "T")
    var value = n
    var i = -1
    while (value >= 1000 && i < suffixes.size - 1) {
        value /= 1000.0
        i++
    }
    return String.format("%.2f", value).replace(".00", "") + suffixes[i]
}

