package com.example

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldLight
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardDarkElevated
import com.example.ui.theme.CreamText
import com.example.ui.theme.DimGray
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.BlueAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Text-To-Speech
        tts = TextToSpeech(this, this)

        setContent {
            MyApplicationTheme {
                val viewModel: OrderViewModel = viewModel()
                val currentRole by viewModel.currentRole.collectAsState()
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // State for notification Toast
                var activeToastOrder by remember { mutableStateOf<FirebaseOrder?>(null) }
                var showToast by remember { mutableStateOf(false) }

                // Listen for new orders and trigger sound/vocalization
                LaunchedEffect(Unit) {
                    viewModel.notifications.collect { order ->
                        if (viewModel.voiceEnabled.value) {
                            // Play beep sound
                            playAlertBeep()

                            // Build TTS speak text
                            delay(500)
                            val itemDescription = order.items.joinToString(", ") { "${it.qty} ${it.name}" }
                            val textToSpeak = "New order received for Table number ${order.tableNumber}. Items ordered: $itemDescription."
                            speakNotification(textToSpeak)
                        }

                        // Trigger toast
                        activeToastOrder = order
                        showToast = true
                    }
                }

                // Auto hide toast after 6 seconds
                LaunchedEffect(showToast) {
                    if (showToast) {
                        delay(6000)
                        showToast = false
                    }
                }

                // Handle post notifications permission & background notification service lifecycle
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted && currentRole == "Counter") {
                        CounterNotificationService.startService(context)
                    }
                }

                LaunchedEffect(currentRole) {
                    if (currentRole == "Counter") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permission = Manifest.permission.POST_NOTIFICATIONS
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, permission
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                CounterNotificationService.startService(context)
                            } else {
                                launcher.launch(permission)
                            }
                        } else {
                            CounterNotificationService.startService(context)
                        }
                    } else {
                        CounterNotificationService.stopService(context)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepBlack)
                ) {
                    when (currentRole) {
                        "Selection" -> SelectionScreen(viewModel)
                        "Customer" -> CustomerMenuScreen(viewModel)
                        "Counter" -> CounterDashboardScreen(viewModel)
                    }

                    // Elegant Toast Notification for new orders (available everywhere)
                    AnimatedVisibility(
                        visible = showToast && activeToastOrder != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                            .zIndex(999f)
                    ) {
                        activeToastOrder?.let { order ->
                            ToastNotificationCard(
                                order = order,
                                onClose = { showToast = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.05f)
            }
        }
    }

    private fun speakNotification(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "OrderNotification")
        }
    }

    private fun playAlertBeep() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            // Triple beep similar to JS playBeep
            Thread {
                try {
                    Thread.sleep(150)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                    Thread.sleep(150)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                } catch (e: Exception) {
                    // Ignore
                }
            }.start()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error playing tone: ${e.message}")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// ═══ SELECTION SCREEN ═══
@Composable
fun SelectionScreen(viewModel: OrderViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Large Premium Moon Light Icon
        Text(
            text = "🌙",
            fontSize = 72.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Cafe Title
        Text(
            text = "Moon Light Cafe",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = GoldPrimary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        // Subtitle
        Text(
            text = "GOOD COFFEE • GOOD FOOD • GOOD MOMENTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DimGray,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Hero Banner Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1781722022046),
                    contentDescription = "Moon Light Cafe Interior",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark luxurious gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Now Serving",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Authentic Spiced Momos & Craft Brews",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CreamText
                    )
                }
            }
        }

        Text(
            text = "Select Application Mode",
            fontSize = 14.sp,
            color = CreamText.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Customer Button
        Button(
            onClick = { viewModel.currentRole.value = "Customer" },
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("customer_mode_btn"),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "🍽️ ", fontSize = 20.sp)
                Text(
                    text = "Customer Menu (Order)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Counter Button
        OutlinedButton(
            onClick = { viewModel.currentRole.value = "Counter" },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, GoldPrimary),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("counter_mode_btn")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "👨‍🍳 ", fontSize = 20.sp)
                Text(
                    text = "Counter Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GoldLight
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Moon Light digital menu system • Powered by Firebase",
            fontSize = 9.sp,
            color = DimGray,
            letterSpacing = 0.5.sp
        )
    }
}

// ═══ CUSTOMER MENU SYSTEM ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMenuScreen(viewModel: OrderViewModel) {
    val selectedTable by viewModel.selectedTable.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val isSendingOrder by viewModel.isSendingOrder.collectAsState()
    val orderSuccessState by viewModel.orderSuccessState.collectAsState()
    val prepNote by viewModel.prepNote.collectAsState()

    var showCartDrawer by remember { mutableStateOf(false) }
    var tableMenuExpanded by remember { mutableStateOf(false) }

    val tableNumbers = (1..10).map { it.toString() }
    val coroutineScope = rememberCoroutineScope()

    // Filtered menu items based on search keyword and selected category
    val filteredItems = MenuData.menuItems.filter { item ->
        val matchesKeyword = item.name.contains(searchKeyword, ignoreCase = true) || 
                             item.description.contains(searchKeyword, ignoreCase = true)
        val matchesCategory = item.category == selectedCategory
        matchesKeyword && (selectedCategory == "all" || matchesCategory)
    }

    if (orderSuccessState) {
        OrderSuccessScreen(
            tableNumber = selectedTable,
            onClose = { viewModel.orderSuccessState.value = false }
        )
    } else {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1200), Color(0xFF0A0800))
                        ))
                        .padding(bottom = 8.dp)
                        .statusBarsPadding()
                ) {
                    // Title and Table Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.currentRole.value = "Selection" },
                            modifier = Modifier.testTag("back_to_selection_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🌙 MOON LIGHT CAFE",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary,
                                letterSpacing = 2.sp
                            )
                        }

                        // Table Badge Selector Dropdown
                        Box {
                            Surface(
                                onClick = { tableMenuExpanded = true },
                                color = GoldPrimary,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("table_badge_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 Table $selectedTable",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.Black
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Table",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = tableMenuExpanded,
                                onDismissRequest = { tableMenuExpanded = false },
                                modifier = Modifier.background(CardDark)
                            ) {
                                tableNumbers.forEach { tNum ->
                                    DropdownMenuItem(
                                        text = { Text("Table $tNum", color = CreamText, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.selectedTable.value = tNum
                                            tableMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("table_option_$tNum")
                                    )
                                }
                            }
                        }
                    }

                    // Cooking Motto subtitle
                    Text(
                        text = "Good Coffee  ·  Good Food  ·  Good Moments",
                        fontSize = 10.sp,
                        color = DimGray,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Search Input
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { viewModel.searchKeyword.value = it },
                        placeholder = { Text("🔍 Menu खोज्नुस् (Search)...", color = DimGray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamText,
                            unfocusedTextColor = CreamText,
                            focusedContainerColor = CardDark,
                            unfocusedContainerColor = CardDark,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = CardDarkElevated
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(48.dp)
                            .testTag("menu_search_input")
                    )

                    // Horizontal Category Tabs
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            CategoryPill(
                                name = "📋 All Items",
                                isSelected = selectedCategory == "all",
                                onClick = { viewModel.selectedCategory.value = "all" }
                            )
                        }

                        items(MenuData.categories) { category ->
                            CategoryPill(
                                name = "${category.emoji} ${category.name}",
                                isSelected = selectedCategory == category.id,
                                onClick = { viewModel.selectedCategory.value = category.id }
                            )
                        }
                    }
                }
            },
            containerColor = DeepBlack
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (filteredItems.isEmpty()) {
                    // Empty list state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "कुनै Item भेटिएन / No items found", color = CreamText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Different spelling or spelling in English try scan list.", color = DimGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems) { item ->
                            val qty = cartItems[item] ?: 0
                            MenuCardItem(
                                item = item,
                                qty = qty,
                                onAdd = { viewModel.addToCart(item) },
                                onRemove = { viewModel.removeFromCart(item) }
                            )
                        }
                    }
                }

                // Mini Hovering Cart bar
                if (cartItems.isNotEmpty()) {
                    val totalQty = cartItems.values.sum()
                    val totalPrice = cartItems.entries.sumOf { (item, qty) -> item.price * qty }

                    Surface(
                        onClick = { showCartDrawer = true },
                        color = GoldPrimary,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("hovering_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$totalQty Items ordered",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Rs. $totalPrice",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "View Menu Cart 🛒",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Cart Drawer Sheet
            if (showCartDrawer) {
                CartDrawerSheet(
                    cartItems = cartItems,
                    tableNumber = selectedTable,
                    prepNote = prepNote,
                    isSending = isSendingOrder,
                    onNoteChange = { viewModel.prepNote.value = it },
                    onAddQty = { viewModel.addToCart(it) },
                    onSubtractQty = { viewModel.removeFromCart(it) },
                    onSubmit = {
                        coroutineScope.launch {
                            viewModel.submitOrder()
                            showCartDrawer = false
                        }
                    },
                    onDismiss = { showCartDrawer = false }
                )
            }
        }
    }
}

@Composable
fun CategoryPill(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) GoldPrimary else CardDark,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) GoldPrimary else CardDarkElevated),
        modifier = Modifier.testTag("category_pill_$name")
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.Black else DimGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun MenuCardItem(
    item: MenuItem,
    qty: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val cardBorder = if (item.isBestseller) BorderStroke(1.2.dp, GoldPrimary) else BorderStroke(1.dp, CardDarkElevated)
    val cardBg = if (item.isBestseller) Color(0xFF1D1600) else CardDark

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji representation
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (item.isBestseller) GoldPrimary.copy(alpha = 0.15f) else CardDarkElevated,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = item.emoji, fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Item Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Veg / Non-Veg dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (item.isVeg) GreenAccent else RedAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CreamText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Bestseller tag
                    if (item.badge != null) {
                        Surface(
                            color = GoldPrimary,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = item.badge,
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = item.description,
                    color = DimGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = "Rs. ${item.price}",
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quantity Modifier
            if (qty == 0) {
                IconButton(
                    onClick = onAdd,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("add_item_${item.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onRemove,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = CardDarkElevated,
                            contentColor = CreamText
                        ),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("subtract_item_${item.name}")
                    ) {
                        Text("-", color = CreamText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        text = qty.toString(),
                        color = CreamText,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.widthIn(min = 14.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = onAdd,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("add_more_item_${item.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Extra",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══ CUSTOM MINI CART DRAWER ═══
@Composable
fun CartDrawerSheet(
    cartItems: Map<MenuItem, Int>,
    tableNumber: String,
    prepNote: String,
    isSending: Boolean,
    onNoteChange: (String) -> Unit,
    onAddQty: (MenuItem) -> Unit,
    onSubtractQty: (MenuItem) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalPrice = cartItems.entries.sumOf { (item, qty) -> item.price * qty }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Bottom sheet card panel
        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            border = BorderStroke(1.5.dp, GoldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {} // Prevent click-through closing
                .testTag("cart_drawer_panel")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header of Drawer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 YOUR ORDER CART",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = GoldPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_cart_drawer")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = DimGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Order Items
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    cartItems.forEach { (item, qty) ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.name,
                                    color = CreamText,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onSubtractQty(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("-", color = DimGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Text(
                                        text = "× $qty",
                                        color = CreamText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = { onAddQty(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("+", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                Text(
                                    text = "Rs. ${item.price * qty}",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color(0xFF222222))
                        }
                    }
                }

                // Total Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Amount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CreamText)
                    Text(text = "Rs. $totalPrice", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GoldLight)
                }

                // Special Prep instructions note
                OutlinedTextField(
                    value = prepNote,
                    onValueChange = onNoteChange,
                    placeholder = { Text("💬 Add cooking instructions (eg. no onions, spicy...)", color = DimGray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedContainerColor = CardDark,
                        unfocusedContainerColor = CardDark,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = CardDarkElevated
                    ),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("prep_instructions_input")
                )

                // Place Order Button
                Button(
                    onClick = onSubmit,
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("cart_place_order_btn")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "SEND ORDER TO KITCHEN 🔥 (Table $tableNumber)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══ SUCCESS SCREEN OVERLAY ═══
@Composable
fun OrderSuccessScreen(
    tableNumber: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎉", fontSize = 80.sp, modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = "आयो नयाँ Order!",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = GreenAccent,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "ORDER SENT SUCCESSFULLY!",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📍 Table Assigned", fontSize = 11.sp, color = DimGray)
                Text(
                    text = "Table $tableNumber",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldLight,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "हाम्रो भान्से (Chef) ले हजुरको अर्डर तयारी गर्दै हुनुहुन्छ। कृपया केहि समय पर्खनुहोला।",
                    fontSize = 11.sp,
                    color = CreamText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
                .testTag("success_continue_btn")
        ) {
            Text(
                text = "Order More Food ➡️",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black
            )
        }
    }
}

// ═══ STAFF COUNTER INTERACTIVE DASHBOARD ═══
@Composable
fun CounterDashboardScreen(viewModel: OrderViewModel) {
    val orders by viewModel.orders.collectAsState()
    val filter by viewModel.counterFilter.collectAsState()
    val voiceOn by viewModel.voiceEnabled.collectAsState()
    val isPolling by viewModel.isPolling.collectAsState()

    var activeClockText by remember { mutableStateOf("--:--:--") }
    var orderToDeleteId by remember { mutableStateOf<String?>(null) }

    // List of values sorted by timestamp decreasing (newest first)
    val ordersList = orders.entries
        .map { it.key to it.value }
        .filter { (_, order) -> filter == "all" || order.status == filter }
        .sortedByDescending { it.second.timestamp }

    // Today's Clock Thread Tracker
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            activeClockText = sdf.format(Date())
            delay(1000)
        }
    }

    // Calculated Statistics
    val allOrders = orders.values.toList()
    val countNew = allOrders.count { it.status == "new" }
    val countPrep = allOrders.count { it.status == "preparing" }
    val countDone = allOrders.count { it.status == "done" }
    val todayRevenue = allOrders.filter { it.status == "done" }.sumOf { it.totalPrice }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1200), Color(0xFF0A0800))
                    ))
                    .statusBarsPadding()
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.currentRole.value = "Selection" },
                        modifier = Modifier.testTag("back_to_selection_counter")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌙 Moon Light Cafe",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Counter",
                                fontSize = 10.sp,
                                color = DimGray,
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, DimGray), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Audio Switch Voice Mode Toggle
                    Button(
                        onClick = { viewModel.voiceEnabled.value = !voiceOn },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (voiceOn) GoldPrimary.copy(alpha = 0.15f) else CardDark,
                            contentColor = if (voiceOn) GoldPrimary else DimGray
                        ),
                        border = BorderStroke(1.dp, if (voiceOn) GoldPrimary else CardDarkElevated),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("voice_toggle_switch"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            text = if (voiceOn) "🔊 Voice ON" else "🔇 Voice OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Middle live pulsating row & Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsating live indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Repeating pulsator
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GreenAccent.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE SYNCING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = GreenAccent,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = activeClockText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CreamText,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Tab filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabFilterButton(text = "📋 सबै (All)", count = allOrders.size, isActive = filter == "all", onClick = { viewModel.counterFilter.value = "all" })
                    TabFilterButton(text = "🔥 नयाँ", count = countNew, isActive = filter == "new", onClick = { viewModel.counterFilter.value = "new" })
                    TabFilterButton(text = "👨‍🍳 बनाउँदै", count = countPrep, isActive = filter == "preparing", onClick = { viewModel.counterFilter.value = "preparing" })
                    TabFilterButton(text = "✅ सकियो", count = countDone, isActive = filter == "done", onClick = { viewModel.counterFilter.value = "done" })
                }
            }
        },
        containerColor = DeepBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Summary Statistics Row Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCardBox(label = "NEW 🔥", value = countNew.toString(), color = OrangeAccent, modifier = Modifier.weight(1f))
                    StatCardBox(label = "PREP 👨‍🍳", value = countPrep.toString(), color = BlueAccent, modifier = Modifier.weight(1f))
                    StatCardBox(label = "DONE ✅", value = countDone.toString(), color = GreenAccent, modifier = Modifier.weight(1f))
                    StatCardBox(label = "REVENUES 💰", value = "Rs.$todayRevenue", color = GoldPrimary, modifier = Modifier.weight(1.3f))
                }

                // Active orders list
                if (ordersList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📭", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "अहिले कुनै order छैन", color = CreamText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Customer ले QR scan गरी अर्डर गरेपछि यहाँ देखिन्छ।", color = DimGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ordersList) { (id, order) ->
                            CounterOrderCard(
                                orderId = id,
                                order = order,
                                onUpdateStatus = { newStat -> viewModel.updateStatus(id, newStat) },
                                onDelete = { orderToDeleteId = id }
                            )
                        }
                    }
                }
            }

            // Confirm Delete Dialog
            orderToDeleteId?.let { idToDelete ->
                AlertDialog(
                    onDismissRequest = { orderToDeleteId = null },
                    title = { Text(text = "हटाउने पुष्टि गर्नुहोस / Confirm Archive", color = GoldLight, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = { Text(text = "के तपाई यो अर्डर हटाउन चाहनुहुन्छ?\nAre you sure you want to delete this order?", color = CreamText, fontSize = 13.sp) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteOrder(idToDelete)
                                orderToDeleteId = null
                            },
                            modifier = Modifier.testTag("confirm_delete_btn")
                        ) {
                            Text("🗑 हटाउ (Delete)", color = RedAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { orderToDeleteId = null }) {
                            Text("रद्द (Cancel)", color = DimGray)
                        }
                    },
                    containerColor = CardDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun TabFilterButton(
    text: String,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isActive) GoldPrimary else CardDark,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isActive) GoldPrimary else CardDarkElevated),
        modifier = Modifier.testTag("counter_filter_$text")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (isActive) Color.Black else CreamText,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = if (isActive) Color.Black.copy(alpha = 0.2f) else CardDarkElevated,
                shape = CircleShape,
                modifier = Modifier.size(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = count.toString(),
                        color = if (isActive) Color.Black else GoldLight,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StatCardBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, CardDarkElevated),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = DimGray
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CounterOrderCard(
    orderId: String,
    order: FirebaseOrder,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    val elapsedMinutes = ((System.currentTimeMillis() - order.timestamp) / 60000).coerceAtLeast(0)

    val elapsedText = if (elapsedMinutes == 0L) "Just now" else "$elapsedMinutes mins ago"

    val statusBorder = when (order.status) {
        "new" -> BorderStroke(1.5.dp, OrangeAccent)
        "preparing" -> BorderStroke(1.5.dp, BlueAccent)
        else -> BorderStroke(1.dp, CardDarkElevated)
    }

    val headerBg = when (order.status) {
        "new" -> OrangeAccent.copy(alpha = 0.08f)
        "preparing" -> BlueAccent.copy(alpha = 0.08f)
        else -> GreenAccent.copy(alpha = 0.08f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = statusBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${orderId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📍 Table ${order.tableNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = GoldPrimary
                    )
                    Text(
                        text = "#${orderId.takeLast(6).uppercase()}",
                        fontSize = 10.sp,
                        color = DimGray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status Badge
                Surface(
                    color = when (order.status) {
                        "new" -> OrangeAccent
                        "preparing" -> BlueAccent
                        else -> GreenAccent
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = when (order.status) {
                            "new" -> "🔥 नयाँ (New)"
                            "preparing" -> "👨‍🍳 बनाउँदै"
                            else -> "✅ सकियो"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Card Body (Items Info)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.name,
                            color = CreamText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "× ${item.qty}",
                                color = DimGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rs.${item.price * item.qty}",
                                color = GoldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Chef notes if present
                if (!order.note.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .background(Color(0xFF1F1800), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF332600), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "💬 Customer Notes: ${order.note}",
                            color = Color(0xFFFBD15B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Card Footer Actions & Price
                HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(top = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rs. ${order.totalPrice}",
                            color = GoldLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "⏰ $timeFormatted ($elapsedText)",
                            color = DimGray,
                            fontSize = 9.sp
                        )
                    }

                    // Dynamic action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (order.status) {
                            "new" -> {
                                Button(
                                    onClick = { onUpdateStatus("preparing") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("action_prepare_${orderId}")
                                ) {
                                    Text("👨‍🍳 Prepare", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            "preparing" -> {
                                Button(
                                    onClick = { onUpdateStatus("done") },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("action_complete_${orderId}")
                                ) {
                                    Text("✅ Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            "done" -> {
                                IconButton(
                                    onClick = onDelete,
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = CardDarkElevated),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("action_delete_${orderId}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = RedAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══ CUSTOM IN-APP ALERT BANNER ═══
@Composable
fun ToastNotificationCard(
    order: FirebaseOrder,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1400)),
        border = BorderStroke(1.5.dp, GoldPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("toast_alert_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔔", fontSize = 36.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "नयाँ Order आयो!",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = GoldLight
                )
                Text(
                    text = "📍 Table ${order.tableNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CreamText
                )
                Text(
                    text = order.items.joinToString(", ") { "${it.name} × ${it.qty}" },
                    fontSize = 11.sp,
                    color = DimGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = DimGray)
            }
        }
    }
}
