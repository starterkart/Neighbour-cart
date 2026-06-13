package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// App shell wrapper holding the tabs and navigation state
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun NeighborCartApp(viewModel: NeighborCartViewModel) {
    if (!viewModel.isLoggedIn) {
        LoginScreen(viewModel)
        return
    }

    val cartItems by viewModel.cartWithDetailsFlow.collectAsState(initial = emptyList())
    val cartTotal = cartItems.sumOf { it.price * it.quantity }
    val cartCount = cartItems.sumOf { it.quantity }

    if (viewModel.showAddAddressScreen) {
        AddCompleteAddressScreen(viewModel = viewModel)
        return
    }

    if (viewModel.showCheckoutScreen) {
        CheckoutScreen(viewModel = viewModel, cartItems = cartItems, cartTotal = cartTotal)
        return
    }

    val activeTab = viewModel.activeTab
    val notifications by viewModel.notificationsFlow.collectAsState(initial = emptyList())
    val activeOrder by viewModel.activeOrderFlow.collectAsState(initial = null)
    
    val unreadNotificationsCount = notifications.count { !it.isRead }

    val focusManager = LocalFocusManager.current
    var showLocationMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false)
            val coarseLocationGranted = permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
            if (fineLocationGranted || coarseLocationGranted) {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                        if (location != null) {
                            viewModel.updateLocation(UserLocation.LiveGPS(location.latitude, location.longitude))
                        } else {
                            android.widget.Toast.makeText(context, "Location not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SecurityException) {
                    android.widget.Toast.makeText(context, "Location permission missing", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(context, "GPS Permissions Denied", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            Column(modifier = Modifier.background(BrandBackground)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandYellow, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
                ) {
                    // Location Picker & Profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .clickable { showLocationMenu = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = BrandBlack,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (viewModel.userName.isNotEmpty()) "DELIVER TO ${viewModel.userName.uppercase()} ⚡" else "DELIVERY IN 15 MINS ⚡",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandBlack.copy(alpha = 0.75f),
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = viewModel.currentLocation.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BrandBlack,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Drop",
                                            tint = BrandBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showLocationMenu,
                                onDismissRequest = { showLocationMenu = false }
                            ) {
                                if (viewModel.supabaseSavedAddresses.isNotEmpty()) {
                                    viewModel.supabaseSavedAddresses.forEach { address ->
                                        DropdownMenuItem(
                                            text = { Text(address.address, color = BrandBlack) },
                                            onClick = {
                                                viewModel.updateLocation(UserLocation.CustomAddress(address.address, 0.0, 0.0))
                                                showLocationMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }


                    }

                    // Inner Search Bar for Shopping panel
                    if (activeTab == BottomTab.SHOP) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            placeholder = { Text("Search 'milk', 'eggs', 'bread'", fontSize = 13.sp, color = BrandBlack.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandBlack.copy(alpha = 0.6f)) },
                            trailingIcon = {
                                if (viewModel.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = BrandBlack)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("home_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = BrandBlack,
                                unfocusedTextColor = BrandBlack
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    }
                }
                
                // Active Toast Push Alert Simulator Banner
                AnimatedVisibility(
                    visible = viewModel.activeToastNotification != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    viewModel.activeToastNotification?.let { toast ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandGreen)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .clickable {
                                    viewModel.switchTab(BottomTab.PROFILE)
                                    viewModel.dismissToast()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "Price drop alert",
                                tint = BrandYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = toast.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = toast.message,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIEW",
                                color = BrandYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, BrandYellow, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            IconButton(
                                onClick = { viewModel.dismissToast() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column {
                // Smart Pop-up Interaction (Bottom Sheet Toast)
                AnimatedVisibility(
                    visible = cartCount > 0 && activeTab == BottomTab.SHOP,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .testTag("floating_cart_bar"),
                        color = Color(0xFF0C513F),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Side (Cart Summary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Mini stack or text
                                Icon(Icons.Default.ShoppingBag, contentDescription = "Basket", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$cartCount Item${if (cartCount > 1) "s" else ""}",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // Vertical divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(Color.White.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // Total price dynamically updated
                                Text(
                                    text = "₹${String.format("%.2f", cartTotal)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            
                            // Right Side (Dual Actions)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Checkout Button
                                Button(
                                    onClick = {
                                        viewModel.showCheckoutScreen = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandYellow),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Checkout →", color = BrandBlack, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Main navigation bar with glassmorphic styling
                NavigationBar(
                    containerColor = Color.White.copy(alpha = 0.85f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                ) {
                    NavigationBarItem(
                        selected = activeTab == BottomTab.SHOP,
                        onClick = { viewModel.switchTab(BottomTab.SHOP) },
                        icon = { Icon(if (activeTab == BottomTab.SHOP) Icons.Filled.Storefront else Icons.Outlined.Storefront, contentDescription = "Market") },
                        label = { Text("Shop", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandBlack,
                            selectedTextColor = BrandBlack,
                            indicatorColor = BrandYellow
                        )
                    )
                    
                    NavigationBarItem(
                        selected = activeTab == BottomTab.CATEGORIES,
                        onClick = { viewModel.switchTab(BottomTab.CATEGORIES) },
                        icon = { Icon(if (activeTab == BottomTab.CATEGORIES) Icons.Filled.Category else Icons.Outlined.Category, contentDescription = "Categories") },
                        label = { Text("Categories", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandBlack,
                            selectedTextColor = BrandBlack,
                            indicatorColor = BrandYellow
                        )
                    )
                    
                    NavigationBarItem(
                        selected = activeTab == BottomTab.ORDERS,
                        onClick = { viewModel.switchTab(BottomTab.ORDERS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (activeOrder != null) {
                                        Badge(containerColor = BrandGreen) {
                                            Text("Live", color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(if (activeTab == BottomTab.ORDERS) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping, contentDescription = "My Deliveries")
                            }
                        },
                        label = { Text("Orders", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandBlack,
                            selectedTextColor = BrandBlack,
                            indicatorColor = BrandYellow
                        )
                    )

                    NavigationBarItem(
                        selected = activeTab == BottomTab.PROFILE,
                        onClick = { viewModel.switchTab(BottomTab.PROFILE) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(containerColor = Color.Red) {
                                            Text(unreadNotificationsCount.toString(), color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(if (activeTab == BottomTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "My Profile")
                            }
                        },
                        label = { Text("Profile", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandBlack,
                            selectedTextColor = BrandBlack,
                            indicatorColor = BrandYellow
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BrandBackground)
        ) {
            // Multipane screen routing with elegant bidirectional slide & fade animations
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { direction * it / 6 } +
                            fadeIn(animationSpec = tween(durationMillis = 300)) with
                    slideOutHorizontally(animationSpec = tween(durationMillis = 200)) { -direction * it / 6 } +
                            fadeOut(animationSpec = tween(durationMillis = 200))
                },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    BottomTab.SHOP -> ShopScreen(viewModel)
                    BottomTab.CATEGORIES -> CategoriesScreen(viewModel)
                    BottomTab.ORDERS -> MyOrdersScreen(viewModel)
                    BottomTab.PROFILE -> ProfileScreen(viewModel)
                }
            }

            // Global Overlay: Product details overlay sheet containing the Custom Canvas 30-day Price history graph (FR-2.3)
            viewModel.detailedProduct?.let { product ->
                ProductDetailModal(
                    product = product,
                    viewModel = viewModel,
                    onDismiss = { viewModel.detailedProduct = null }
                )
            }

            // Global Overlay: Cart Store Conflict Warning modal (FR-1.2)
            viewModel.cartConflictProduct?.let { product ->
                CartConflictDialog(
                    conflictProduct = product,
                    viewModel = viewModel,
                    onDismiss = { viewModel.cartConflictProduct = null }
                )
            }
        }
    }
}

// Module A (FR-1.1 Geofencing/FR-1.2 Cart Logic) - Shopping Panel
@Composable
fun ShopScreen(viewModel: NeighborCartViewModel) {
    val selectedStore = viewModel.selectedStore

    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isSyncingByBackend) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandGreen.copy(alpha = 0.08f))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = BrandGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Syncing live stores & products with REST backend...",
                        color = BrandGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (viewModel.syncResultSuccess == false) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red.copy(alpha = 0.08f))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️ REST Backend offline. Operating on cash-optimized offline mode.",
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selectedStore == null) {
            // Loading or No Stores STATE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = BrandGreen,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Finding nearest store...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val productsState = viewModel.getProductsByStoreFlow(selectedStore.id).collectAsState(initial = emptyList())
            val products = productsState.value

            val query = viewModel.searchQuery.trim().lowercase(Locale.getDefault())
            val filteredProducts = products.filter {
                it.name.lowercase(Locale.getDefault()).contains(query) ||
                it.category.lowercase(Locale.getDefault()).contains(query)
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // If there's no active query, show the banner, category selector first
                if (query.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        FeaturedExpressBanner()
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    item(span = { GridItemSpan(2) }) {
                        CategorySelectionGrid(
                            onCategorySelect = { categoryKey ->
                                viewModel.searchQuery = categoryKey
                            }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "ALL FRESH INSTANT ITEMS",
                            color = BrandBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                } else {
                    // Show search header
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "SEARCH RESULTS FOR '${query.uppercase(Locale.getDefault())}'",
                                color = BrandBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${filteredProducts.size} items",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = "Empty",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No items match your browse selection.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(filteredProducts.size) { index ->
                        val product = filteredProducts[index]
                        ProductCard(
                            product = product,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeofencedStoreSelectorRow(viewModel: NeighborCartViewModel) {
    val activeStore = viewModel.selectedStore
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        GeofencedStoreSelectorDialog(viewModel = viewModel, onDismiss = { showDialog = false })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geofenced_store_selector_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BrandGreen.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = "Active Shop",
                        tint = BrandGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DELIVERING FROM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandGreen,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = activeStore?.name ?: "No Shop in Range",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (activeStore != null) {
                        val distance = viewModel.calculateDistance(
                            viewModel.currentLocation.latitude, viewModel.currentLocation.longitude,
                            activeStore.latitude, activeStore.longitude
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BrandGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Within Range ($distance KM) • 15 Min Delivery",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "No shops deliver here (too far)",
                                fontSize = 11.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .background(BrandGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Official Store",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }
        }
    }
}

@Composable
fun GeofencedStoreSelectorDialog(
    viewModel: NeighborCartViewModel,
    onDismiss: () -> Unit
) {
    val geofencedStores by viewModel.geofencedStoresFlow.collectAsState()
    val activeStore = viewModel.selectedStore

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Hyperlocal Shops Near You",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = BrandBlack
                )
                Text(
                    text = "Deliverable range is up to 5.0 KM (Geofenced)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                items(geofencedStores) { geofenced ->
                    val isSelected = activeStore?.id == geofenced.store.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = geofenced.isDeliverable) {
                                viewModel.selectStore(geofenced.store)
                                onDismiss()
                            }
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) BrandGreen else if (geofenced.isDeliverable) Color.Transparent else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFE8F5E9) else if (geofenced.isDeliverable) Color.White else Color(0xFFF1F5F9)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = geofenced.store.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (geofenced.isDeliverable) BrandBlack else Color.Gray
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(BrandGreen, CircleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = geofenced.store.category,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (geofenced.isDeliverable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${geofenced.distanceKm} KM",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = if (geofenced.isDeliverable) BrandGreen else Color.Red
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = geofenced.store.description,
                                fontSize = 11.sp,
                                color = if (geofenced.isDeliverable) Color.DarkGray else Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${geofenced.store.rating}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (geofenced.isDeliverable) BrandBlack else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (geofenced.isDeliverable) BrandGreen else Color.Gray, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (geofenced.isDeliverable) "Deliverable ✓" else "Too far ❌",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (geofenced.isDeliverable) BrandGreen else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

// Single Store presentation row card
@Composable
fun StoreCard(
    store: Store,
    distance: Double,
    isInRange: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isInRange, onClick = onClick)
            .testTag("store_card_${store.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isInRange) Color.White else Color.White.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (isInRange) BrandBorder else BrandBorder.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Emoji or symbol icon representative
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isInRange) BrandYellow.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Shop icon",
                            tint = if (isInRange) BrandGreen else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = store.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isInRange) BrandBlack else Color.Gray
                        )
                        Text(
                            text = store.category,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Radius Geofencing pill details
                Surface(
                    color = if (isInRange) BrandGreenLight else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "${distance} KM",
                        color = if (isInRange) BrandGreen else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = store.description,
                fontSize = 12.sp,
                color = if (isInRange) Color.DarkGray else Color.LightGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BrandBorder)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "star", tint = BrandOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${store.rating}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInRange) BrandBlack else Color.Gray
                    )
                }

                if (isInRange) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed ETA", tint = BrandGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delivery in ${store.etaMinutes} mins",
                            fontSize = 12.sp,
                            color = BrandGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, contentDescription = "Locked Radius", tint = Color.Red, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Out of Range (Max 5km)",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Store inventory screen header
@Composable
fun StoreGridHeader(
    store: Store,
    onBack: () -> Unit,
    viewModel: NeighborCartViewModel
) {
    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandBlack)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = BrandBlack
                    )
                    Text(
                        text = "🛒 Standard checkout delivery radius limits",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Surface(
                    color = BrandGreenLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "★ ${store.rating}",
                        color = BrandGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Local store search bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("Search items in ${store.name}...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BrandBackground,
                    unfocusedContainerColor = BrandBackground,
                    focusedBorderColor = BrandGreen,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Grid item product display layout - styled like Blinkit cards
@Composable
fun getProductImageUrl(productName: String, categoryName: String = ""): String {
    val name = productName.lowercase()
    return when {
        name.contains("milk") -> "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300&auto=format&fit=crop&q=80"
        name.contains("egg") -> "https://images.unsplash.com/photo-1506976785307-8732e854ad03?w=300&auto=format&fit=crop&q=80"
        name.contains("bread") || name.contains("bun") -> "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300&auto=format&fit=crop&q=80"
        name.contains("coffee") || name.contains("cafe") -> "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=300&auto=format&fit=crop&q=80"
        name.contains("apple") -> "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=300&auto=format&fit=crop&q=80"
        name.contains("banana") -> "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=300&auto=format&fit=crop&q=80"
        name.contains("tomato") -> "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=300&auto=format&fit=crop&q=80"
        name.contains("onion") -> "https://images.unsplash.com/photo-1508747703725-719777637510?w=300&auto=format&fit=crop&q=80"
        name.contains("potato") -> "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=300&auto=format&fit=crop&q=80"
        name.contains("butter") -> "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=300&auto=format&fit=crop&q=80"
        name.contains("cheese") -> "https://images.unsplash.com/photo-1486299267070-83823f5448dd?w=300&auto=format&fit=crop&q=80"
        name.contains("tea") -> "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=300&auto=format&fit=crop&q=80"
        name.contains("honey") -> "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=300&auto=format&fit=crop&q=80"
        categoryName.contains("fruit", ignoreCase = true) || categoryName.contains("veggie", ignoreCase = true) -> "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=300&auto=format&fit=crop&q=80"
        categoryName.contains("dairy", ignoreCase = true) -> "https://images.unsplash.com/photo-1528498033373-3c6c08e93d79?w=300&auto=format&fit=crop&q=80"
        categoryName.contains("bakery", ignoreCase = true) -> "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=300&auto=format&fit=crop&q=80"
        categoryName.contains("beverag", ignoreCase = true) || categoryName.contains("brew", ignoreCase = true) -> "https://images.unsplash.com/photo-1497515114629-f71d768fd07c?w=300&auto=format&fit=crop&q=80"
        else -> "https://images.unsplash.com/photo-1542838132-92c53300491e?w=300&auto=format&fit=crop&q=80"
    }
}

@Composable
fun ProductCard(
    product: Product,
    viewModel: NeighborCartViewModel
) {
    val cartItems by viewModel.cartWithDetailsFlow.collectAsState(initial = emptyList())
    val cartEntry = cartItems.find { it.productId == product.id }
    val isWatched by viewModel.isProductWatched(product.id).collectAsState(initial = false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.detailedProduct = product } // Opens graph & metadata popup
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Product Image Visual Display Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandBackground),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = getProductImageUrl(product.name, product.category),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(topStart = 6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            product.emoji,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Details text
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandBlack,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(32.dp)
                )

                Text(
                    text = product.unit,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bottom section: Price and Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (product.price < product.originalPrice) {
                            Text(
                                text = "₹${String.format("%.2f", product.originalPrice)}",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        Text(
                            text = "₹${String.format("%.2f", product.price)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = BrandBlack
                        )
                    }

                    // ADD action button / counter
                    if (product.stock <= 0) {
                        Text(
                            "OOS",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, Color.Red, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    } else if (cartEntry == null) {
                        Button(
                            onClick = { viewModel.addToCart(product) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .border(1.dp, BrandGreen, RoundedCornerShape(6.dp))
                                .testTag("add_to_cart_btn_${product.id}")
                        ) {
                            Text("ADD", color = BrandGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    } else {
                        // Incrementor layout controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .border(1.dp, BrandGreen, RoundedCornerShape(6.dp))
                                .height(32.dp)
                                .padding(horizontal = 2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.decreaseQuantity(product.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Less", tint = BrandGreen, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = cartEntry.quantity.toString(),
                                color = BrandGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { viewModel.addToCart(product) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "More", tint = BrandGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Module B - Alerts Screen & Notifications list log
@Composable
fun AlertsScreen(viewModel: NeighborCartViewModel) {
    val watchlistItems by viewModel.watchlistWithDetailsFlow.collectAsState(initial = emptyList())
    val notifications by viewModel.notificationsFlow.collectAsState(initial = emptyList())

    // Instantly mark logs as read upon accessing so indicators clear nicely
    LaunchedEffect(notifications) {
        if (notifications.any { !it.isRead }) {
            viewModel.clearAllNotifications()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Watched items list block (FR-2.1)
        item {
            Text(
                text = "My Price Watchlist 🎯",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = BrandBlack,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "You will receive immediate in-app alerts if any shop drops the price of these items below your tracking target.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (watchlistItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.TrendingDown, contentDescription = "Empty Watch", tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Watchlist empty. Toggle the bell icon on daily goods (milk, coffee, eggs) to monitor prices.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(watchlistItems) { item ->
                WatchedItemCard(item = item, viewModel = viewModel)
            }
        }

        // Notification logs block (FR-2.2 Alert logs)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Price Alarm Notifications 🔔",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = BrandBlack
                )
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllNotifications() }) {
                        Text("Clear All", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = "No alerts", tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "No price drop logs received yet. Modify items downward in the Merchant Portal to fire off alarms!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(notifications) { log ->
                NotificationRowCard(log = log, viewModel = viewModel)
            }
        }
    }
}

// Card for individual watchlist metrics
@Composable
fun WatchedItemCard(
    item: WatchlistItemWithProduct,
    viewModel: NeighborCartViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("watchlist_item_${item.productId}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(BrandBackground, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BrandBlack
                )
                Text(
                    text = "Shop: ${item.storeName} (${item.unit})",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Current: ₹${String.format("%.2f", item.price)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.price < item.addedPrice) BrandGreen else BrandBlack
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Base Target: ₹${String.format("%.2f", item.addedPrice)}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            
            // Percentage discount badge
            if (item.price < item.addedPrice) {
                val dropPercent = ((item.addedPrice - item.price) / item.addedPrice) * 100
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 4.dp).testTag("watched_item_discount_badge")
                ) {
                    Text(
                        text = "-${String.format("%.0f", dropPercent)}%",
                        color = BrandGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            
            // Delete watchlist button
            IconButton(
                onClick = {
                    viewModel.toggleWatchlist(
                        Product(
                            id = item.productId,
                            storeId = item.storeId,
                            name = item.name,
                            category = item.category,
                            price = item.price,
                            originalPrice = item.addedPrice,
                            unit = item.unit,
                            stock = item.stock,
                            barcode = "",
                            description = "",
                            emoji = item.emoji
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}

// Price Drop push-style log notification entry card
@Composable
fun NotificationRowCard(
    log: Notification,
    viewModel: NeighborCartViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val dateStr = try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    } catch (_: Exception) {
        "Just Now"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BrandGreenLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TrendingDown,
                            contentDescription = "Drop alert",
                            tint = BrandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = log.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.message,
                fontSize = 12.sp,
                color = Color.DarkGray,
                lineHeight = 16.sp
            )
            
            if (log.productId != 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            // Opens product details
                            coroutineScope.launch {
                                val p = viewModel.getProductById(log.productId)
                                if (p != null) {
                                    viewModel.detailedProduct = p
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("View History Graph", color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Module A - Orders Screen (Checkouts history list + Live tracking progress maps FR-1.3)
@Composable
fun MyOrdersScreen(viewModel: NeighborCartViewModel) {
    val orders by viewModel.allOrdersFlow.collectAsState(initial = emptyList())
    val activeOrder by viewModel.activeOrderFlow.collectAsState(initial = null)
    var selectedOrderDetails by remember { mutableStateOf<DeliveryOrder?>(null) }

    if (selectedOrderDetails != null) {
        OrderDetailsDialog(
            order = selectedOrderDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedOrderDetails = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Active real-time order layout (FR-1.3 Live map visual track)
        if (activeOrder != null) {
            item {
                Text(
                    "Active Instant Delivery ⚡",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LiveTrackCard(
                    order = activeOrder!!,
                    viewModel = viewModel,
                    onClick = { selectedOrderDetails = activeOrder }
                )
            }
        }

        // Histroic Checkouts list
        item {
            Text(
                "Order History 📦",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = BrandBlack,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }

        val historyOrders = orders.filter { it.status == "Delivered" }

        if (historyOrders.isEmpty() && activeOrder == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = "Empty orders", tint = Color.LightGray, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "You haven't placed any hyperlocal transactions yet.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(historyOrders) { order ->
                HistoricOrderCard(
                    order = order,
                    onClick = { selectedOrderDetails = order }
                )
            }
        }
    }
}

data class ParsedOrderItem(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val price: Double,
    val emoji: String,
    val unit: String
)

fun parseProductsSummary(summary: String): List<ParsedOrderItem> {
    if (summary.isEmpty()) return emptyList()
    val list = mutableListOf<ParsedOrderItem>()
    val items = summary.split(";")
    for (item in items) {
        if (item.isEmpty()) continue
        val parts = item.split("|")
        if (parts.size >= 5) {
            val id = parts[0].toLongOrNull() ?: 0L
            val name = parts[1]
            val qty = parts[2].toIntOrNull() ?: 0
            val price = parts[3].toDoubleOrNull() ?: 0.0
            val emoji = parts[4]
            val unit = if (parts.size >= 6) parts[5] else ""
            list.add(ParsedOrderItem(id, name, qty, price, emoji, unit))
        }
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsDialog(
    order: DeliveryOrder,
    viewModel: NeighborCartViewModel,
    onDismiss: () -> Unit
) {
    val dateStr = try {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(order.timestamp))
    } catch (_: Exception) {
        "Date not available"
    }

    val parsedItems = parseProductsSummary(order.productsSummary)
    val subtotal = order.totalAmount
    val handlingFee = 2.0
    val grandTotal = subtotal + handlingFee

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = BrandBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header (Close and Order Number)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Order Details",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = BrandBlack
                        )
                        Text(
                            text = "ID: #NC-${10000 + order.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BrandBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable details section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Status and Date
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BrandBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (statusColor, statusIcon, statusBg) = when (order.status) {
                                        "Placed" -> Triple(BrandYellow, Icons.Default.AccessTime, Color(0xFFFFFDE7))
                                        "Accepted" -> Triple(Color(0xFF2196F3), Icons.Default.Check, Color(0xFFE3F2FD))
                                        "At Store" -> Triple(Color(0xFF9C27B0), Icons.Default.Storefront, Color(0xFFF3E5F5))
                                        "Out for Delivery" -> Triple(BrandOrange, Icons.Default.LocalShipping, Color(0xFFFFF3E0))
                                        "Delivered" -> Triple(BrandGreen, Icons.Default.CheckCircle, Color(0xFFE8F5E9))
                                        else -> Triple(Color.Gray, Icons.Default.Info, Color(0xFFF5F5F5))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(statusBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = statusIcon,
                                            contentDescription = "Status",
                                            tint = statusColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "ORDER STATUS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusColor,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = order.status.uppercase(Locale.getDefault()),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BrandBlack
                                        )
                                        Text(
                                            text = dateStr,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // Store Info
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BrandBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(BrandGreen.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = "Store",
                                            tint = BrandGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "MERCHANT PARTNER",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandGreen,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = order.storeName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlack
                                        )
                                        Text(
                                            text = "Delivered instantly • Cash On Delivery",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // Delivery Driver details
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BrandBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFECEFF1), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🛵", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "LOCAL RIDER COMPANION",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Gray,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = order.driverName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandBlack
                                            )
                                            Text(
                                                text = "Verified Delivery Agent • ${order.driverPhone}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Interactive Support Call button!
                                    IconButton(
                                        onClick = { /* Simulated Call placeholder */ },
                                        modifier = Modifier
                                            .background(BrandGreen.copy(alpha = 0.1f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = BrandGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Ordered Products List
                        item {
                            Text(
                                text = "PRODUCTS ORDERED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Gray,
                                letterSpacing = 0.75.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        if (parsedItems.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, BrandBorder),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "${order.itemCount} Essentials",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = BrandBlack
                                        )
                                        Text(
                                            text = "Complete package delivered to your registered doorstep address.",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        } else {
                            items(parsedItems) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, BrandBorder),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(BrandBackground, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = item.emoji, fontSize = 18.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = BrandBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.unit} • ${item.quantity} Unit${if (item.quantity > 1) "s" else ""}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        Text(
                                            text = "₹${String.format("%.2f", item.price * item.quantity)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = BrandBlack
                                        )
                                    }
                                }
                            }
                        }

                        // Cost Breakdown Bill Detailed Receipt
                        item {
                            Text(
                                text = "BILL RECEIPT SUMMARY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Gray,
                                letterSpacing = 0.75.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BrandBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Items Subtotal", fontSize = 13.sp, color = Color.Gray)
                                        Text("₹${String.format("%.2f", subtotal)}", fontSize = 13.sp, color = BrandBlack, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Superstore Handling Fee", fontSize = 13.sp, color = Color.Gray)
                                        Text("₹${String.format("%.2f", handlingFee)}", fontSize = 13.sp, color = BrandBlack, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Geofenced Delivery Charge", fontSize = 13.sp, color = Color.Gray)
                                        Row {
                                            Text(
                                                text = "₹15.00", 
                                                fontSize = 12.sp, 
                                                color = Color.Gray, 
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Text("FREE", fontSize = 12.sp, color = BrandGreen, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BrandBorder)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Grand Total Paid", fontSize = 14.sp, fontWeight = FontWeight.Black, color = BrandBlack)
                                            Text("Paid via Cash On Delivery", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text("₹${String.format("%.2f", grandTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandBlack)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Re-order and Back CTAs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BrandBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Back", color = BrandBlack, fontWeight = FontWeight.Bold)
                    }

                    if (order.productsSummary.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.reorderItems(order.productsSummary, order.storeId)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(2.5f)
                                .height(48.dp)
                        ) {
                            Text("Re-order Essentials", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// Real-time map track UI showing scooter movement progress & status steppers (FR-1.3)
@Composable
fun LiveTrackCard(
    order: DeliveryOrder,
    viewModel: NeighborCartViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("live_track_panel"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order from ${order.storeName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandBlack
                    )
                    Text(
                        "${order.itemCount} Items • Total ₹${String.format("%.2f", order.totalAmount)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Surface(
                    color = BrandGreenLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ETA < 15 mins",
                        color = BrandGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BrandBorder)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stepper Checkmark lists representing status details
            Text(
                "Delivery Fulfillment Steps 🚚",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = BrandGreen,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val stages = listOf(
                "Confirmed" to "Order successfully received and confirmed",
                "Picked Up" to "Delivery partner: ${order.driverName} picked up package from merchant",
                "Out for Delivery" to "Partner heading on scenic routes to your address",
                "Delivered" to "Package dropped safely at your doorstep"
            )

            // Dynamic progression checking
            val activeIndex = when (order.status) {
                "Confirmed" -> 0
                "Picked Up" -> 1
                "Out for Delivery" -> 2
                "Delivered" -> 3
                else -> 0
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stages.forEachIndexed { idx, stage ->
                    val isChecked = idx < activeIndex
                    val isActive = idx == activeIndex
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            color = if (isChecked) BrandGreen else if (isActive) BrandYellow else Color.LightGray.copy(alpha = 0.4f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isChecked) {
                                    Icon(Icons.Default.Check, contentDescription = "Passed", tint = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text(
                                        text = (idx + 1).toString(),
                                        color = if (isActive) BrandBlack else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column {
                            Text(
                                text = stage.first,
                                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                color = if (idx <= activeIndex) BrandBlack else Color.Gray,
                                fontSize = 13.sp
                            )
                            Text(
                                text = stage.second,
                                color = if (idx <= activeIndex) Color.DarkGray else Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BrandBorder)
            Spacer(modifier = Modifier.height(10.dp))
            
            // Delivery Partner contact block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandGreenLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Driver", tint = BrandGreen)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(order.driverName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlack)
                        Text(order.driverPhone, fontSize = 11.sp, color = Color.Gray)
                    }
                }
                
                Button(
                    onClick = { /* Simulate ringing driver phone */ },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandYellow),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = "Ring", tint = BrandBlack, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Driver", color = BrandBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Histroic orders row item card
@Composable
fun HistoricOrderCard(order: DeliveryOrder, onClick: () -> Unit) {
    val dateStr = try {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(order.timestamp))
    } catch (_: Exception) {
        "Past Deliveries"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(BrandBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Done", tint = BrandGreen)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Delivered from ${order.storeName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandBlack
                    )
                    Text(
                        text = "$dateStr • ${order.itemCount} Items",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "₹${String.format("%.2f", order.totalAmount)}",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = BrandBlack
            )
        }
    }
}

// Module C - Merchant Portal Utility is deprecated as there will be a separate web app for admins.
@Composable
fun MerchantScreen(viewModel: NeighborCartViewModel) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "👨‍💼 Merchant Portal Moved",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = BrandBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The inventory control panel has migrated to the official web application console.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MerchantScreenOldDeprecatedToDelete(viewModel: NeighborCartViewModel) {
    val productsState = viewModel.allProductsFlow.collectAsState(initial = emptyList())
    val products = productsState.value
    var merchantSearchQuery by remember { mutableStateOf("") }
    
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Merchant Stock Control Portal (FR-3.1) 👨‍💼",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = BrandBlack
        )
        Text(
            text = "As a Mom-and-Pop shop owner, rapidly edit stock indices or slide prices down. Lowering prices will trigger immediate, automatic push notifications to all users tracking that specific item within 5 minutes!",
            fontSize = 12.sp,
            color = Color.Gray
        )

        // Barcode reader trigger controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandYellow.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, BrandYellow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FR-3.1 Inventory Barcode Simulator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandBlack
                )
                Text(
                    text = "Simulate scanning product barcodes on client-side camera captures using our sandbox scan triggers.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.simulateBarcodeScan("890101") },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan Milk (890101)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.simulateBarcodeScan("890104") },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan Coffee (890104)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active notification scanner feedback toasts inside sandbox panels
        viewModel.barcodeResultMsg?.let { msg ->
            Surface(
                color = if (msg.contains("failed", ignoreCase = true)) Color(0xFFFFCDD2) else BrandGreenLight,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (msg.contains("failed", ignoreCase = true)) Color.Red else BrandGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = if (msg.contains("failed", ignoreCase = true)) Color.Red else BrandGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Search block
        OutlinedTextField(
            value = merchantSearchQuery,
            onValueChange = { merchantSearchQuery = it },
            placeholder = { Text("Search catalog items by name/category...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        // Editable product list
        val mQuery = merchantSearchQuery.trim().lowercase(Locale.getDefault())
        val filtered = products.filter {
            it.name.lowercase(Locale.getDefault()).contains(mQuery) ||
            it.category.lowercase(Locale.getDefault()).contains(mQuery)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered.size) { index ->
                val product = filtered[index]
                MerchantProductCard(product = product, viewModel = viewModel)
            }
        }
    }
}

// Single Merchant inventory editable item row
@Composable
fun MerchantProductCard(
    product: Product,
    viewModel: NeighborCartViewModel
) {
    var isEditingPrice by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var stockCount by remember { mutableStateOf(product.stock) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BrandBackground, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.emoji, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandBlack
                    )
                    Text(
                        "Code: ${product.barcode} | Unit: ${product.unit}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                // Live inventory stock level counters
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (stockCount > 0) {
                                stockCount--
                                viewModel.updateProductStock(product.id, stockCount)
                            }
                        }
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Less stock", tint = Color.Gray)
                    }
                    Text(
                        text = "$stockCount",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (stockCount == 0) Color.Red else BrandBlack,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            stockCount++
                            viewModel.updateProductStock(product.id, stockCount)
                        }
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add stock", tint = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BrandBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Pricing editor and interactive price-drop alarm trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isEditingPrice) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Current Pricing: ₹${String.format("%.2f", product.price)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlack
                        )
                        if (product.price < product.originalPrice) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Was ₹${String.format("%.2f", product.originalPrice)}",
                                fontSize = 11.sp,
                                color = BrandGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Button(
                        onClick = { isEditingPrice = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Edit Price", color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("New Price (₹)") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                val dPrice = priceText.toDoubleOrNull()
                                if (dPrice != null && dPrice > 0) {
                                    viewModel.updateProductPrice(product.id, dPrice)
                                    isEditingPrice = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                priceText = product.price.toString()
                                isEditingPrice = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("X", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Global Overlay Dialog (FR-1.2 Single Merchant lock validation modal)
@Composable
fun CartConflictDialog(
    conflictProduct: Product,
    viewModel: NeighborCartViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BrandBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color.Red, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Clear Current Cart?",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = BrandBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A single NeighborCart order can only contain products from one shop to support instant deliveries (FR-1.2 Merchant locks).\n\nDo you want to discard your current cart and shop at this store instead?",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = BrandBlack)
                    }
                    
                    Button(
                        onClick = {
                            viewModel.resolveCartConflict(emptyAndAdd = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear & Add", color = Color.White)
                    }
                }
            }
        }
    }
}

// Global Overlay sheet - displays comprehensive metadata and 30-day Price history graph (FR-2.3)
@Composable
fun ProductDetailModal(
    product: Product,
    viewModel: NeighborCartViewModel,
    onDismiss: () -> Unit
) {
    val priceHistoryState = viewModel.detailedProductHistoryFlow.collectAsState(initial = emptyList())
    val priceHistory = priceHistoryState.value
    val isWatchedState = viewModel.isProductWatched(product.id).collectAsState(initial = false)
    val isWatched = isWatchedState.value

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BrandBorder)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(BrandBackground, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(product.emoji, fontSize = 34.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                product.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = BrandBlack
                            )
                            Text(
                                "${product.category} • ${product.unit}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                // Beautiful prominent Description Callout for FR-2.2 and tap details
                Surface(
                    color = BrandGreenLight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRODUCT DESCRIPTION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = BrandGreen,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = product.description.ifEmpty { "No description provided for this premium fresh item." },
                            fontSize = 13.sp,
                            color = BrandBlack,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BrandBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // FR-2.3: Simple 30-day Price Trend Chart (Custom Canvas Drawing)
                Text(
                    text = "30-Day Valuation Trend Graph (FR-2.3)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BrandGreen,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (priceHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(BrandBackground, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Assembling asset tracking valuation data...", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    PriceTrendChart(priceHistory = priceHistory)
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "Description Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BrandBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Track price button block: FR-2.1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.toggleWatchlist(product)
                        },
                        modifier = Modifier.weight(1f),
                        borderColor = if (isWatched) BrandOrange else Color.LightGray,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isWatched) BrandOrange.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Watch",
                            tint = if (isWatched) BrandOrange else BrandBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWatched) "TRACKING ACTIVE" else "TRACK PRICE",
                            color = if (isWatched) BrandOrange else BrandBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            viewModel.addToCart(product)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ADD TO CART • ₹${product.price}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// Custom Helper for OutlinedButton border color
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.LightGray,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(1.dp, borderColor, shape),
        colors = colors,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        content = content
    )
}

// FR-2.3 Custom Vector Canvas graph rendering price indices
@Composable
fun PriceTrendChart(priceHistory: List<PriceHistoryEntry>) {
    val prices = priceHistory.map { it.price }
    val maxPrice = (prices.maxOrNull() ?: 100.0) * 1.05
    val minPrice = (prices.minOrNull() ?: 0.0) * 0.95
    val priceDiff = if (maxPrice == minPrice) 1.0 else maxPrice - minPrice

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandBackground, RoundedCornerShape(8.dp))
            .border(1.dp, BrandBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // High fidelity Canvas render
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padY = 15f
                val padX = 25f
                
                val points = priceHistory.mapIndexed { index, entry ->
                    val x = padX + (index.toFloat() / (priceHistory.size - 1).coerceAtLeast(1)) * (w - 2 * padX)
                    val y = h - padY - ((entry.price - minPrice) / priceDiff).toFloat() * (h - 2 * padY)
                    Offset(x, y)
                }

                // Draw bounding background grid
                val gridLines = 3
                for (i in 0..gridLines) {
                    val gy = padY + (i.toFloat() / gridLines) * (h - 2 * padY)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(padX, gy),
                        end = Offset(w - padX, gy),
                        strokeWidth = 2f
                    )
                }

                // Draw solid smooth connection curve
                if (points.size > 1) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    
                    // Draw continuous gradient brush volume underneath trend
                    val gradientPath = Path().apply {
                        moveTo(points.first().x, h - padY)
                        for (pt in points) {
                            lineTo(pt.x, pt.y)
                        }
                        lineTo(points.last().x, h - padY)
                        close()
                    }

                    drawPath(
                        path = gradientPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(BrandGreen.copy(alpha = 0.2f), Color.Transparent),
                            startY = points.map { it.y }.minOrNull() ?: 0f,
                            endY = h - padY
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = BrandGreen,
                        style = Stroke(width = 4.5f)
                    )
                }

                // Draw coordinate node handles
                points.forEach { pt ->
                    drawCircle(color = Color.White, radius = 7f, center = pt)
                    drawCircle(color = BrandGreen, radius = 5f, center = pt)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Print base coordinates timeline summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-30 Days Ago", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("Price Target: ₹${String.format("%.1f", prices.firstOrNull() ?: 0.0)} ➔ ₹${String.format("%.1f", prices.lastOrNull() ?: 0.0)}", fontSize = 10.sp, color = BrandGreen, fontWeight = FontWeight.Black)
            Text("Today", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeaturedExpressBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_express_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BrandGreen, BrandGreenLight)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HYPERLOCAL EXPRESS",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "30-MIN\nGUARANTEE",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Radius",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "5 KM",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelectionGrid(onCategorySelect: (String) -> Unit) {
    val categories = listOf(
        CategoryItem("Dairy", "🥛", Color(0xFFFEF3C7), Color(0xFF78350F), "Dairy & Eggs"),
        CategoryItem("Veggies", "🥬", Color(0xFFDCFCE7), Color(0xFF15803D), "Fruits & Veggies"),
        CategoryItem("Snacks", "🍪", Color(0xFFF3E8FF), Color(0xFF6B21A8), "Bakery"),
        CategoryItem("Brew", "☕", Color(0xFFDBEAFE), Color(0xFF1E40AF), "Beverages & Coffee")
    )
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SHOP BY CATEGORY",
            color = BrandBlack,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCategorySelect(item.searchKey) }
                        .testTag("category_cell_${item.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(item.backgroundColor, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.emoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandBlack
                        )
                    }
                }
            }
        }
    }
}

data class CategoryItem(
    val name: String,
    val emoji: String,
    val backgroundColor: Color,
    val textColor: Color,
    val searchKey: String
)
