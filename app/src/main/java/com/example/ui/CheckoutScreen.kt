package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.CartItemWithProduct
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandBlack
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandYellow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: NeighborCartViewModel,
    cartItems: List<CartItemWithProduct>,
    cartTotal: Double
) {
    val deliveryFee = 15.0
    val handlingFee = 2.0
    val grandTotal = cartTotal + handlingFee // delivery is typically free or crossed out as requested

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Review Cart & Checkout", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showCheckoutScreen = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            // Section 5: Sticky Bottom Bar (Place Order)
            Surface(
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "₹${String.format("%.2f", grandTotal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = BrandBlack
                        )
                        Text(
                            text = "TOTAL",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SwipeToPlaceOrderButton(
                        onOrderPlaced = {
                            val currentStore = viewModel.selectedStore
                            if (currentStore != null) {
                                viewModel.showCheckoutScreen = false
                                viewModel.checkout(cartItems, currentStore)
                                viewModel.switchTab(BottomTab.ORDERS) // go to orders on success to track
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            item {
                // Section 1: Top Navigation & Delivery Address
                DeliveryAddressCard(viewModel)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 2: Detailed Bill Items List
            item {
                Text(
                    text = "Items in Cart",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            items(cartItems) { item ->
                CheckoutItemRow(item, viewModel)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
            }

            // Section 3: Bill Detailed Breakdown
            item {
                Spacer(modifier = Modifier.height(16.dp))
                BillDetailsCard(cartTotal, deliveryFee, handlingFee, grandTotal)
            }

            // Section 4: Payment Methods
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PaymentMethodsSection()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DeliveryAddressCard(viewModel: NeighborCartViewModel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = BrandYellow,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Delivering to",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlack,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = viewModel.currentLocation.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandBlack
                    )
                }
                Text(
                    text = "Change",
                    color = BrandGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.showAddAddressScreen = true }
                )
            }
        }
    }
}

@Composable
fun CheckoutItemRow(item: CartItemWithProduct, viewModel: NeighborCartViewModel) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder - since it's a list, we can just use a colored box or text
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(item.emoji, fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = BrandBlack
            )
            Text(
                text = item.unit,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "₹${item.price}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlack,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        // Quantity Controls from product card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(1.dp, BrandGreen, RoundedCornerShape(6.dp))
                .height(28.dp)
                .padding(horizontal = 2.dp)
        ) {
            IconButton(
                onClick = { viewModel.decreaseQuantity(item.productId) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Less", tint = BrandGreen, modifier = Modifier.size(14.dp))
            }
            Text(
                text = item.quantity.toString(),
                color = BrandGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = { 
                    scope.launch {
                        viewModel.getProductById(item.productId)?.let { product ->
                            viewModel.addToCart(product)
                        }
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "More", tint = BrandGreen, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun BillDetailsCard(itemTotal: Double, deliveryFee: Double, handlingFee: Double, grandTotal: Double) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bill Details",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandBlack,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Item Total", fontSize = 13.sp, color = Color.DarkGray)
                Text("₹${String.format("%.2f", itemTotal)}", fontSize = 13.sp, color = BrandBlack)
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Delivery Partner Fee", fontSize = 13.sp, color = Color.DarkGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${String.format("%.2f", deliveryFee)}", fontSize = 13.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FREE", fontSize = 13.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Handling Charge", fontSize = 13.sp, color = Color.DarkGray)
                Text("₹${String.format("%.2f", handlingFee)}", fontSize = 13.sp, color = BrandBlack)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Grand Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlack)
                Text("₹${String.format("%.2f", grandTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandBlack)
            }
        }
    }
}

@Composable
fun PaymentMethodsSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Payment Method",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandBlack,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // COD Option
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = Color(0xFFE8F5E9).copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrandGreen)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Money, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Cash on Delivery (Pay on Arrival)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = BrandBlack,
                    modifier = Modifier.weight(1f)
                )
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Disabled Online Option
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small visual placeholders for UPI/Cards
                    Row(modifier = Modifier.padding(end = 12.dp)) {
                        Box(modifier = Modifier.size(24.dp).background(Color.LightGray, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(24.dp).background(Color.LightGray, RoundedCornerShape(4.dp)))
                    }
                    Text(
                        text = "UPI / Credit Card / NetBanking",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(
                        selected = false,
                        onClick = null,
                        enabled = false
                    )
                }
                
                // Warning text
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color(0xFFFFF3E0), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Online payment currently unavailable",
                        fontSize = 11.sp,
                        color = Color(0xFFF57C00),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeToPlaceOrderButton(
    modifier: Modifier = Modifier,
    onOrderPlaced: () -> Unit
) {
    val width = 220.dp
    val thumbSize = 52.dp
    val swipeDistance = with(LocalDensity.current) { (width - thumbSize).toPx() }
    
    var offsetX by remember { mutableStateOf(0f) }
    var actionTriggered by remember { mutableStateOf(false) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "")
    
    LaunchedEffect(animatedOffsetX) {
        if (!actionTriggered && animatedOffsetX >= swipeDistance - 10f) {
            actionTriggered = true
            onOrderPlaced()
        }
    }
    
    Box(
        modifier = modifier
            .width(width)
            .height(52.dp)
            .background(BrandGreen.copy(alpha = 0.1f), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Slide to Order >>", 
            color = BrandGreen, 
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .size(thumbSize)
                .background(BrandGreen, RoundedCornerShape(26.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < swipeDistance - 10f) {
                                offsetX = 0f
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceIn(0f, swipeDistance)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = null, tint = Color.White)
        }
    }
}
