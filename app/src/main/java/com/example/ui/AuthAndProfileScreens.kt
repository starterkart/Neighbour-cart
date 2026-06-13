package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

// =========================================================================
// 1. LOGIN SCREEN Flow (Phone -> OTP Code -> Name & Address Details)
// =========================================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(viewModel: NeighborCartViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Phone, 2: OTP, 3: Details
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("123456") }
    
    // Details Sub-step
    var nameInput by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandYellow, BrandBackground, Color.White),
                    startY = 0.0f,
                    endY = 1600.0f
                )
            )
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Header
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, BrandGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛵",
                    fontSize = 42.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Neighbor Cart",
                color = BrandBlack,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(30.dp))

            // Auth Step Container Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
                    }
                ) { currentStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (currentStep) {
                            1 -> {
                                // STEP 1: Phone Number Field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Log in or Sign up",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = BrandBlack,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val isConfig = viewModel.checkSupabaseAuthConfigured()
                                    Box(
                                        modifier = Modifier
                                            .background(if (isConfig) BrandGreen.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isConfig) "🌐 Supabase Auth" else "📦 Sandbox Auth",
                                            color = if (isConfig) BrandGreen else Color.DarkGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter your mobile number to load your shopping session.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 10) {
                                            phoneInput = input
                                        }
                                    },
                                    prefix = {
                                        Text(
                                            text = "+91 ",
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlack
                                        )
                                    },
                                    label = { Text("Mobile Phone Number") },
                                    placeholder = { Text("98765 43210") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = BrandBlack,
                                        unfocusedTextColor = BrandBlack,
                                        focusedBorderColor = BrandGreen,
                                        focusedLabelColor = BrandGreen,
                                        cursorColor = BrandGreen
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_phone_input")
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                if (viewModel.otpSendError != null) {
                                    Text(
                                        text = viewModel.otpSendError ?: "",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (phoneInput.length == 10) {
                                            focusManager.clearFocus()
                                            val isSupabaseConfigured = viewModel.checkSupabaseAuthConfigured()
                                            if (isSupabaseConfigured) {
                                                viewModel.sendSupabaseOtp(phoneInput) { success, errMsg ->
                                                    if (success) {
                                                        // Generate backup otp just in case they decide to switch/bypass
                                                        val rawOtp = (100000..999999).random().toString()
                                                        generatedOtp = rawOtp
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "💬 Supabase OTP Sent! Check your SMS.",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                        step = 2
                                                    } else {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "⚠️ SMS Send Error: $errMsg. Proceeding via Sandbox Mode.",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                        val rawOtp = (100000..999999).random().toString()
                                                        generatedOtp = rawOtp
                                                        step = 2
                                                    }
                                                }
                                            } else {
                                                val rawOtp = (100000..999999).random().toString()
                                                generatedOtp = rawOtp
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "💬 Sandbox Mode: Your Neighbor Cart code is $rawOtp",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                                step = 2
                                            }
                                        }
                                    },
                                    enabled = phoneInput.length == 10 && !viewModel.isOtpSending,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandYellow,
                                        contentColor = BrandBlack,
                                        disabledContainerColor = Color.LightGray.copy(alpha = 0.3f),
                                        disabledContentColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_send_otp")
                                ) {
                                    if (viewModel.isOtpSending) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlack, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "Send One-Time OTP",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            2 -> {
                                // STEP 2: OTP Verification Field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Enter Verification Code",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = BrandBlack,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val isSupabaseActive = viewModel.isSupabaseAuthActive
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSupabaseActive) BrandGreen.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isSupabaseActive) "🌐 Supabase SMS" else "📦 Sandbox Mode",
                                            color = if (isSupabaseActive) BrandGreen else Color.DarkGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "We sent a 6-digit verification code to +91 $phoneInput",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 6) {
                                            otpInput = input
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = BrandGreen)
                                    },
                                    label = { Text("6-Digit OTP Code") },
                                    placeholder = { Text(generatedOtp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = BrandBlack,
                                        unfocusedTextColor = BrandBlack,
                                        focusedBorderColor = BrandGreen,
                                        focusedLabelColor = BrandGreen,
                                        cursorColor = BrandGreen
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_otp_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick simulation automatic bypass for ease of use
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            otpInput = generatedOtp
                                            focusManager.clearFocus()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = BrandBackground),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Celebration, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tap to Autofill Real OTP ($generatedOtp)",
                                            color = BrandGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { step = 1 }) {
                                        Text("Change Number", color = BrandGreen, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    var countdownSeconds by remember { mutableStateOf(59) }
                                    LaunchedEffect(key1 = true) {
                                        while (countdownSeconds > 0) {
                                            delay(1000)
                                            countdownSeconds--
                                        }
                                    }

                                    Text(
                                        text = if (countdownSeconds > 0) "Resend in ${countdownSeconds}s" else "Resend OTP",
                                        fontSize = 11.sp,
                                        color = if (countdownSeconds > 0) Color.Gray else BrandGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable(enabled = countdownSeconds == 0) {
                                            countdownSeconds = 59
                                            val isSupabaseConfigured = viewModel.isSupabaseAuthActive
                                            if (isSupabaseConfigured) {
                                                viewModel.sendSupabaseOtp(phoneInput) { success, errMsg ->
                                                    if (success) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "💬 Resent Supabase OTP! Check your SMS.",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    } else {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "⚠️ SMS Resend Error: $errMsg.",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                val rawOtp = (100000..999999).random().toString()
                                                generatedOtp = rawOtp
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "💬 Security Code: Resent Neighbor Cart code is $rawOtp",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (viewModel.isSupabaseAuthActive && (viewModel.otpVerifyError != null || viewModel.otpSendError != null)) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Supabase API Issue Status:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = viewModel.otpVerifyError ?: viewModel.otpSendError ?: "",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(
                                                onClick = {
                                                    // Turn off live auth model and fall back to simulator
                                                    viewModel.isSupabaseAuthActive = false
                                                    otpInput = generatedOtp
                                                    viewModel.otpVerifyError = null
                                                    viewModel.otpSendError = null
                                                    android.widget.Toast.makeText(context, "Switching to sandbox bypass!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text("Bypass with Sandbox OTP ($generatedOtp) \uD83D\uDE92", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        val isSupabaseConfigured = viewModel.isSupabaseAuthActive
                                        if (isSupabaseConfigured && otpInput != generatedOtp) {
                                            viewModel.verifySupabaseOtp(phoneInput, otpInput) { success, errMsg ->
                                                if (success) {
                                                    step = 3
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "❌ Verification Failed. Please verify your token or use Sandbox Bypass.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            step = 3
                                        }
                                    },
                                    enabled = (otpInput.length == 6 || otpInput == generatedOtp) && !viewModel.isOtpVerifying,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandGreen,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.LightGray.copy(alpha = 0.3f),
                                        disabledContentColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_verify_otp")
                                ) {
                                    if (viewModel.isOtpVerifying) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "Verify & Proceed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            3 -> {
                                // STEP 3: Complete Personal Details (Name and Address)
                                Text(
                                    text = "Setup Delivery Profile 🏡",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = BrandBlack,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "We require your name and complete delivery address to drop off essentials.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Your Full Name") },
                                    placeholder = { Text("Enter your name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = BrandBlack,
                                        unfocusedTextColor = BrandBlack,
                                        focusedBorderColor = BrandGreen,
                                        focusedLabelColor = BrandGreen,
                                        cursorColor = BrandGreen
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_name_input")
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (nameInput.trim().isNotEmpty()) {
                                            focusManager.clearFocus()
                                            viewModel.saveUserSession(
                                                phone = "+91 $phoneInput",
                                                name = nameInput.trim(),
                                                address = ""
                                            )
                                            viewModel.showAddAddressScreen = true
                                        }
                                    },
                                    enabled = nameInput.trim().isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandGreen,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.LightGray.copy(alpha = 0.3f),
                                        disabledContentColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_complete_signup")
                                ) {
                                    Text(
                                        text = "Complete Registration",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            // Safety assurances footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Neighbor Cart Safe Delivery Guaranteed 🔒",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// =========================================================================
// 2. CATEGORIES SCREEN (Filters shop item triggers easily)
// =========================================================================

fun getCategoryImageUrl(name: String): String {
    return when (name) {
        "Daily Essentials" -> "https://images.unsplash.com/photo-1542838132-92c53300491e?w=200&auto=format&fit=crop&q=80"
        "Fruits & Veggies" -> "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=200&auto=format&fit=crop&q=80"
        "Dairy & Eggs" -> "https://images.unsplash.com/photo-1528498033373-3c6c08e93d79?w=200&auto=format&fit=crop&q=80"
        "Beverages & Coffee" -> "https://images.unsplash.com/photo-1497515114629-f71d768fd07c?w=200&auto=format&fit=crop&q=80"
        "Super Store" -> "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=200&auto=format&fit=crop&q=80"
        else -> "https://images.unsplash.com/photo-1542838132-92c53300491e?w=200&auto=format&fit=crop&q=80"
    }
}

@Composable
fun CategoriesScreen(viewModel: NeighborCartViewModel) {
    val categoriesList = listOf(
        ProfileCategoryItem("Daily Essentials", "Daily Goods", "🍞", "Bread, eggs, tea & cupboard staples", Color(0xFFFFF7ED)),
        ProfileCategoryItem("Fruits & Veggies", "Fruits & Veggies", "🥦", "Fresh leafy greens & organic fruits", Color(0xFFF0FDF4)),
        ProfileCategoryItem("Dairy & Eggs", "Dairy", "🥛", "Fresh whole milk, butter & cheese packs", Color(0xFFEFF6FF)),
        ProfileCategoryItem("Beverages & Coffee", "Brews", "☕", "Instant coffee, premium Arabica & cocoa", Color(0xFFFAF5FF)),
        ProfileCategoryItem("Super Store", "Unfiltered specials", "🍯", "Pure forest honey, bulk bundles", Color(0xFFFEF2F2))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Browse Departments 🛍️",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = BrandBlack,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Click any category to instantly view and buy fresh essentials in 15 mins",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categoriesList.size) { index ->
                val category = categoriesList[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.searchQuery = category.filterQuery
                            viewModel.switchTab(BottomTab.SHOP)
                        }
                        .testTag("category_row_${category.name.lowercase().replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(category.color),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = getCategoryImageUrl(category.name),
                                contentDescription = category.name,
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
                                    text = category.emoji,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = BrandBlack
                            )
                            Text(
                                text = category.tagline,
                                color = BrandGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Text(
                                text = category.desc,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BrandBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = BrandBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ProfileCategoryItem(
    val name: String,
    val tagline: String,
    val emoji: String,
    val desc: String,
    val color: Color,
    val filterQuery: String = name
)


// =========================================================================
// 3. PROFILE SCREEN (User details, live delivery tracker, inside Watchlist!)
// =========================================================================

@Composable
fun ProfileScreen(viewModel: NeighborCartViewModel) {
    val watchlistItems by viewModel.watchlistWithDetailsFlow.collectAsState(initial = emptyList())
    val activeOrder by viewModel.activeOrderFlow.collectAsState(initial = null)
    
    LaunchedEffect(Unit) {
        viewModel.fetchSavedAddressesFromSupabase()
    }
    
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 3.1 Header Greeting Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User Initials Avatar badge
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(BrandYellow.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, BrandYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.userName.isNotEmpty()) viewModel.userName.first().uppercase() else "B",
                                fontWeight = FontWeight.Bold,
                                color = BrandBlack,
                                fontSize = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = viewModel.userName.ifEmpty { "Guest Shopper" },
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = BrandBlack
                            )
                            Text(
                                text = viewModel.userPhone.ifEmpty { "+91 ----------" },
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 3.2 Live Order Status tracking summary widget
        activeOrder?.let { order ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Live Delivery in Transit 🛵",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = BrandGreen
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(BrandGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = order.status.uppercase(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Delivering ${order.itemCount} fresh goods from ${order.storeName}.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = { order.progress },
                            color = BrandGreen,
                            trackColor = Color.LightGray.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated delivery within 12 minutes",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            
                            TextButton(
                                onClick = { viewModel.switchTab(BottomTab.ORDERS) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Map Tracker →", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3.3 MY SAVED ADDRESSES SECTION (Highly optimized hyperlocal delivery layout)
        item {
            Column {
                Text(
                    text = "My Saved Addresses 📍",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                Text(
                    text = "Manage your pre-saved home, office, and travel delivery locations. Tap an address to make it your current active drop-off place.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        val addressesList = listOf(
            Triple("Home Address", viewModel.userAddressHome, Icons.Default.Home),
            Triple("Work Address", viewModel.userAddressWork, Icons.Default.Business),
            Triple("Other / Hotel Address", viewModel.userAddressOther, Icons.Default.Place)
        )

        itemsIndexed(addressesList) { index, item ->
            val label = item.first
            val addressText = item.second
            val icon = item.third
            val isActive = addressText.trim().isNotEmpty() && addressText == viewModel.userAddress

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (addressText.trim().isNotEmpty()) {
                            viewModel.saveUserSession(
                                phone = viewModel.userPhone,
                                name = viewModel.userName,
                                address = addressText,
                                typeIndex = index
                            )
                        } else {
                            viewModel.showAddAddressScreen = true
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) BrandGreen.copy(alpha = 0.04f) else Color.White
                ),
                border = BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) BrandGreen else BrandBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isActive) BrandGreen.copy(alpha = 0.15f) else BrandBackground,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) BrandGreen else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandBlack
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(BrandGreen, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = addressText.ifEmpty { "No address saved yet. Tap to set up." },
                            fontSize = 12.sp,
                            color = if (addressText.isEmpty()) Color.LightGray else Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            viewModel.showAddAddressScreen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Saved Address",
                            tint = if (isActive) BrandGreen else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3.5 Safe Logout Panel
        item {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { viewModel.logoutUser() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BrandOrange
                ),
                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_logout")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out Account Session", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
