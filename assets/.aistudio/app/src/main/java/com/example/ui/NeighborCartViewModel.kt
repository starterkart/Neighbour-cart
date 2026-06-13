package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BottomTab {
    SHOP, CATEGORIES, ORDERS, PROFILE
}

sealed interface UserLocation {
    val name: String
    val latitude: Double
    val longitude: Double

    object CentralOffice : UserLocation {
        override val name = "Central Offices (Town Centre)"
        override val latitude = 12.971598
        override val longitude = 77.594562
    }

    object SuburbanHome : UserLocation {
        override val name = "Suburban Home (Outer Ring)"
        override val latitude = 12.920000
        override val longitude = 77.520000
    }

    object MetroStation : UserLocation {
        override val name = "Metro Station (Indiranagar)"
        override val latitude = 12.980000
        override val longitude = 77.600000
    }

    object SrinagarArea : UserLocation {
        override val name = "Dal Lake, Srinagar"
        override val latitude = 34.15944035944071
        override val longitude = 74.81705853251457
    }

    data class LiveGPS(override val latitude: Double, override val longitude: Double) : UserLocation {
        override val name = "Live GPS Location"
    }
}

object SafeConfig {
    fun getString(fieldName: String, defaultValue: String = ""): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField(fieldName)
            field.get(null) as? String ?: defaultValue
        } catch (t: Throwable) {
            defaultValue
        }
    }
}

class NeighborCartViewModel(
    application: Application,
    private val repository: NeighborCartRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("blink_user_prefs", android.content.Context.MODE_PRIVATE)

    var isLoggedIn by mutableStateOf(prefs.getBoolean("is_logged_in", false))
    var userPhone by mutableStateOf(prefs.getString("user_phone", "") ?: "")
    var userName by mutableStateOf(prefs.getString("user_name", "") ?: "")
    var userAddress by mutableStateOf(prefs.getString("user_address", "") ?: "")

    var userAddressHome by mutableStateOf(prefs.getString("user_address_home", "Flat 402, Elite Residency, Near Central Park Sector-5") ?: "")
    var userAddressWork by mutableStateOf(prefs.getString("user_address_work", "Floor 12, Tower B, Town Centre Corporate Offices") ?: "")
    var userAddressOther by mutableStateOf(prefs.getString("user_address_other", "Ghat No. 12, Boulevard Road, near Dal Lake, Srinagar") ?: "")

    private fun getInitialSupabaseUrl(): String {
        val envUrl = SafeConfig.getString("SUPABASE_URL")
        if (envUrl.isNotEmpty() && !envUrl.contains("your-project")) {
            return envUrl
        }
        val storedUrl = prefs.getString("supabase_url", "") ?: ""
        if (storedUrl.isNotEmpty()) return storedUrl
        return "https://cjczkritylenhrzlplkn.supabase.co"
    }

    private fun getInitialSupabaseKey(): String {
        val envKey = SafeConfig.getString("SUPABASE_KEY")
        if (envKey.isNotEmpty() && !envKey.contains("your-supabase-anon")) {
            return envKey
        }
        val storedKey = prefs.getString("supabase_key", "") ?: ""
        if (storedKey.isNotEmpty()) return storedKey
        return "sb_publishable_4qZ8h-FY2tvop7CjmZHVRA_n8Fiocdd"
    }

    // Supabase variables
    var supabaseUrl by mutableStateOf(getInitialSupabaseUrl())
    var supabaseKey by mutableStateOf(getInitialSupabaseKey())
    var isSeedingActive by mutableStateOf(false)
    var seedingStatusMessage by mutableStateOf<String?>(null)

    fun updateSupabaseConfig(url: String, key: String) {
        supabaseUrl = url.trim()
        supabaseKey = key.trim()
        prefs.edit().apply {
            putString("supabase_url", url.trim())
            putString("supabase_key", key.trim())
            apply()
        }
        com.example.data.api.RetrofitClient.updateConfig(url.trim(), key.trim())
        triggerBackendSyncOnDemand()
    }

    fun triggerBackendSyncOnDemand() {
        viewModelScope.launch {
            isSyncingByBackend = true
            val success = repository.syncWithBackend()
            isSyncingByBackend = false
            syncResultSuccess = success
        }
    }

    fun pushDemoSeedData() {
        viewModelScope.launch {
            isSeedingActive = true
            val success = repository.pushSeedDataToSupabase()
            isSeedingActive = false
            if (success) {
                seedingStatusMessage = "Successfully pushed stores & products tables schema to your Supabase instance! 🎉 Triggering automatic live synchronization..."
                triggerBackendSyncOnDemand()
            } else {
                seedingStatusMessage = "❌ Seed upload failed. Please verify that both 'stores' and 'products' tables exist in your Supabase project (and that their columns match, e.g. RLS Policies have insert permissions!)."
            }
        }
    }

    // Backend sync status tracking
    var isSyncingByBackend by mutableStateOf(false)
    var syncResultSuccess by mutableStateOf<Boolean?>(null)

    // Supabase Saved Addresses list
    var supabaseSavedAddresses by mutableStateOf<List<com.example.data.api.SupabaseAddress>>(emptyList())
    var isAddressesLoading by mutableStateOf(false)
    var addressesLoadError by mutableStateOf<String?>(null)

    // Supabase OTP Send / Verify State variables
    var isOtpSending by mutableStateOf(false)
    var otpSendError by mutableStateOf<String?>(null)
    var isOtpVerifying by mutableStateOf(false)
    var otpVerifyError by mutableStateOf<String?>(null)
    var isSupabaseAuthActive by mutableStateOf(false)

    fun checkSupabaseAuthConfigured(): Boolean {
        val url = supabaseUrl.trim().removeSuffix("/")
        val key = supabaseKey.trim()
        val isConfigured = url.isNotEmpty() && key.isNotEmpty() &&
                !url.contains("your-project") && !key.contains("your-supabase-anon")
        isSupabaseAuthActive = isConfigured
        return isConfigured
    }

    fun sendSupabaseOtp(phone: String, onResponse: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isOtpSending = true
            otpSendError = null
            
            val url = supabaseUrl.trim().removeSuffix("/")
            val key = supabaseKey.trim()
            
            if (url.isEmpty() || key.isEmpty() || url.contains("your-project") || key.contains("your-supabase-anon")) {
                isSupabaseAuthActive = false
                isOtpSending = false
                // Sandbox fallback
                onResponse(true, null)
                return@launch
            }
            
            isSupabaseAuthActive = true
            val formattedPhone = formatPhoneForSupabase(phone)
            
            try {
                val client = OkHttpClient()
                val json = "{\"phone\":\"$formattedPhone\"}"
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                
                val request = Request.Builder()
                    .url("$url/auth/v1/otp")
                    .post(body)
                    .addHeader("apikey", key)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                
                val code = response.code
                val responseBodyStr = response.body?.string() ?: ""
                response.close()
                
                if (response.isSuccessful || code == 200 || code == 201) {
                    isOtpSending = false
                    onResponse(true, null)
                } else {
                    android.util.Log.e("SupabaseOTP", "Failed to send OTP: $code -> $responseBodyStr")
                    isOtpSending = false
                    otpSendError = "Supabase API returned error (code $code): $responseBodyStr"
                    onResponse(false, "Supabase Error: $responseBodyStr")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                isOtpSending = false
                otpSendError = "Failed to connect to Supabase: ${t.localizedMessage}"
                onResponse(false, t.localizedMessage)
            }
        }
    }

    fun verifySupabaseOtp(phone: String, token: String, onResponse: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isOtpVerifying = true
            otpVerifyError = null
            
            val url = supabaseUrl.trim().removeSuffix("/")
            val key = supabaseKey.trim()
            
            if (url.isEmpty() || key.isEmpty() || url.contains("your-project") || key.contains("your-supabase-anon")) {
                isSupabaseAuthActive = false
                isOtpVerifying = false
                // Sandbox fallback
                onResponse(true, null)
                return@launch
            }
            
            isSupabaseAuthActive = true
            val formattedPhone = formatPhoneForSupabase(phone)
            
            try {
                val client = OkHttpClient()
                val json = "{\"type\":\"sms\",\"phone\":\"$formattedPhone\",\"token\":\"$token\"}"
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                
                val request = Request.Builder()
                    .url("$url/auth/v1/verify")
                    .post(body)
                    .addHeader("apikey", key)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                
                val code = response.code
                val responseBodyStr = response.body?.string() ?: ""
                response.close()
                
                if (response.isSuccessful || code == 200 || code == 201) {
                    isOtpVerifying = false
                    onResponse(true, null)
                } else {
                    android.util.Log.e("SupabaseOTP", "Failed to verify OTP: $code -> $responseBodyStr")
                    isOtpVerifying = false
                    otpVerifyError = "Supabase OTP code lookup failed (code $code): $responseBodyStr"
                    onResponse(false, "Failed (code $code): $responseBodyStr")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                isOtpVerifying = false
                otpVerifyError = "Failed to connect to Supabase: ${t.localizedMessage}"
                onResponse(false, t.localizedMessage)
            }
        }
    }

    private fun formatPhoneForSupabase(phone: String): String {
        var clean = phone.replace(" ", "").replace("-", "")
        if (!clean.startsWith("+")) {
            if (clean.length == 10) {
                clean = "+91$clean"
            } else if (clean.startsWith("91") && clean.length == 12) {
                clean = "+$clean"
            } else {
                clean = "+91$clean"
            }
        }
        return clean
    }

    fun fetchSavedAddressesFromSupabase() {
        val phone = userPhone
        if (phone.isEmpty()) {
            supabaseSavedAddresses = emptyList()
            return
        }
        viewModelScope.launch {
            isAddressesLoading = true
            addressesLoadError = null
            try {
                if (supabaseUrl.isNotEmpty() && supabaseKey.isNotEmpty()) {
                    val apiService = com.example.data.api.RetrofitClient.apiService
                    val queryStr = "eq.$phone"
                    val response = apiService.getSupabaseAddresses(queryStr)
                    supabaseSavedAddresses = response
                } else {
                    supabaseSavedAddresses = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addressesLoadError = e.localizedMessage ?: "Failed to fetch saved addresses from Supabase"
            } finally {
                isAddressesLoading = false
            }
        }
    }

    fun saveUserSession(phone: String, name: String, address: String, typeIndex: Int = 0) {
        userPhone = phone
        userName = name
        userAddress = address
        isLoggedIn = true
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_phone", phone)
            putString("user_name", name)
            putString("user_address", address)
            when (typeIndex) {
                0 -> {
                    userAddressHome = address
                    putString("user_address_home", address)
                }
                1 -> {
                    userAddressWork = address
                    putString("user_address_work", address)
                }
                else -> {
                    userAddressOther = address
                    putString("user_address_other", address)
                }
            }
            apply()
        }

        // Async Push to Supabase if credentials are setup
        viewModelScope.launch {
            if (supabaseUrl.trim().isNotEmpty() && supabaseKey.trim().isNotEmpty()) {
                try {
                    val apiService = com.example.data.api.RetrofitClient.apiService
                    val newAddress = com.example.data.api.SupabaseAddress(
                        phone = phone,
                        name = name,
                        address = address,
                        typeIndex = typeIndex
                    )
                    apiService.postSupabaseAddress(newAddress)
                    fetchSavedAddressesFromSupabase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun logoutUser() {
        isLoggedIn = false
        userPhone = ""
        userName = ""
        userAddress = ""
        userAddressHome = "Flat 402, Elite Residency, Near Central Park Sector-5"
        userAddressWork = "Floor 12, Tower B, Town Centre Corporate Offices"
        userAddressOther = "Ghat No. 12, Boulevard Road, near Dal Lake, Srinagar"
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            putString("user_phone", "")
            putString("user_name", "")
            putString("user_address", "")
            putString("user_address_home", "")
            putString("user_address_work", "")
            putString("user_address_other", "")
            apply()
        }
        activeTab = BottomTab.SHOP
    }

    // Tab State
    var activeTab by mutableStateOf(BottomTab.SHOP)

    fun switchTab(tab: BottomTab) {
        activeTab = tab
    }

    // Geofencing Location state
    var currentLocation by mutableStateOf<UserLocation>(UserLocation.SrinagarArea)
        private set

    fun updateLocation(location: UserLocation) {
        currentLocation = location
        // Check if selected store is out of range, if so we deselect and look for a local alternative
        val selected = selectedStore
        var needsNewStore = selected == null
        if (selected != null) {
            val dist = calculateDistance(
                location.latitude, location.longitude,
                selected.latitude, selected.longitude
            )
            if (dist > 5.0) {
                needsNewStore = true
                selectedStore = null
            }
        }

        if (needsNewStore) {
            viewModelScope.launch {
                val list = repository.allStoresFlow.first()
                val nearbyStore = list.firstOrNull { store ->
                    calculateDistance(location.latitude, location.longitude, store.latitude, store.longitude) <= 5.0
                }
                if (nearbyStore != null) {
                    selectedStore = nearbyStore
                }
            }
        }
    }

    // Geofenced stores status flow combining user's active coordinate and registered merchants
    val geofencedStoresFlow: StateFlow<List<GeofencedStore>> = combine(
        snapshotFlow { currentLocation },
        repository.allStoresFlow
    ) { loc, stores ->
        // This is app only for one shop
        val singleStoreList = if (stores.isNotEmpty()) listOf(stores.first()) else emptyList()
        singleStoreList.map { store ->
            val dist = calculateDistance(loc.latitude, loc.longitude, store.latitude, store.longitude)
            GeofencedStore(
                store = store,
                distanceKm = dist,
                isDeliverable = dist <= 5.0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and Modals state within tabs
    var selectedStore by mutableStateOf<Store?>(null)
    
    fun selectStore(store: Store) {
        selectedStore = store
    }

    var detailedProduct by mutableStateOf<Product?>(null)
    
    // Cart conflict dialog state
    var cartConflictProduct by mutableStateOf<Product?>(null)

    // Search query within the active store or merchant catalog
    var searchQuery by mutableStateOf("")

    // Checkout screen state
    var showCheckoutScreen by mutableStateOf(false)
    var showAddAddressScreen by mutableStateOf(false)

    // Barcode scanner modal simulation
    var showBarcodeScanner by mutableStateOf(false)
    var barcodeResultMsg by mutableStateOf<String?>(null)

    // In-app hovering toast notification state
    var activeToastNotification by mutableStateOf<Notification?>(null)

    // Subscriptions to Repository Flows
    val storesFlow: Flow<List<Store>> = repository.allStoresFlow.map { list ->
        if (list.isNotEmpty()) listOf(list.first()) else emptyList()
    }
    val allProductsFlow: Flow<List<Product>> = repository.allProductsFlow
    val cartWithDetailsFlow: Flow<List<CartItemWithProduct>> = repository.cartWithDetailsFlow
    val watchlistWithDetailsFlow: Flow<List<WatchlistItemWithProduct>> = repository.watchlistWithDetailsFlow
    val notificationsFlow: Flow<List<Notification>> = repository.allNotificationsFlow
    val allOrdersFlow: Flow<List<DeliveryOrder>> = repository.allOrdersFlow
    val activeOrderFlow: Flow<DeliveryOrder?> = repository.activeOrderFlow

    fun getProductsByStoreFlow(storeId: Long): Flow<List<Product>> = repository.getProductsByStoreFlow(storeId)
    suspend fun getProductById(id: Long): Product? = repository.getProductById(id)

    // Detailed product price history
    val detailedProductHistoryFlow = snapshotFlow { detailedProduct }
        .flatMapLatest { product ->
            if (product != null) {
                repository.getHistoryForProductFlow(product.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks if a product is watched
    fun isProductWatched(productId: Long): Flow<Boolean> = flow {
        emit(repository.isItemWatched(productId))
    }

    private var trackingJob: Job? = null

    init {
        // Initialize Retrofit configuration from prefs or fallback
        val storedUrl = supabaseUrl.ifEmpty { "https://cjczkritylenhrzlplkn.supabase.co" }
        val storedKey = supabaseKey.ifEmpty { "sb_publishable_4qZ8h-FY2tvop7CjmZHVRA_n8Fiocdd" }

        if (storedUrl.isNotEmpty() && storedKey.isNotEmpty()) {
            com.example.data.api.RetrofitClient.updateConfig(storedUrl, storedKey)
        }

        // Trigger REST backend sync over Retrofit client
        viewModelScope.launch {
            isSyncingByBackend = true
            val success = repository.syncWithBackend()
            isSyncingByBackend = false
            syncResultSuccess = success
            if (userPhone.isNotEmpty()) {
                fetchSavedAddressesFromSupabase()
            }
        }

        // Observe stores and auto-select a nearby store within geofence range
        viewModelScope.launch {
            repository.allStoresFlow.collect { list ->
                if (list.isNotEmpty()) {
                    // This is app only for one shop
                    selectedStore = list.first()
                }
            }
        }

        // Observe notifications to display high-priority hovering toasts
        viewModelScope.launch {
            repository.allNotificationsFlow.collect { notifications ->
                val latest = notifications.firstOrNull()
                // If notification is fresh (created in the last 15 seconds) and is a price drop, display as banner toast!
                if (latest != null && System.currentTimeMillis() - latest.timestamp < 15000 && latest.productId != 0L) {
                    activeToastNotification = latest
                }
            }
        }
    }

    fun dismissToast() {
        activeToastNotification = null
    }

    // Location utility - Haversine distance in KM
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in KM
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return Math.round(r * c * 10.0) / 10.0 // Round to 1 decimal place
    }

    // Add product to Cart conforming to FR-1.2: One merchant per order limit
    fun addToCart(product: Product) {
        viewModelScope.launch {
            val currentCart = repository.getRawCartItems()
            if (currentCart.isNotEmpty() && currentCart.first().storeId != product.storeId) {
                // Conflict detected!
                cartConflictProduct = product
            } else {
                repository.addProductToCart(product.id, product.storeId)
            }
        }
    }

    fun resolveCartConflict(emptyAndAdd: Boolean) {
        val prod = cartConflictProduct ?: return
        viewModelScope.launch {
            if (emptyAndAdd) {
                repository.clearCart()
                repository.addProductToCart(prod.id, prod.storeId)
            }
            cartConflictProduct = null
        }
    }

    fun decreaseQuantity(productId: Long) {
        viewModelScope.launch {
            repository.decreaseProductCartQuantity(productId)
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch {
            repository.removeProductFromCart(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun reorderItems(productsSummary: String, storeId: Long) {
        if (productsSummary.isEmpty()) return
        viewModelScope.launch {
            repository.clearCart()
            val items = productsSummary.split(";")
            for (item in items) {
                if (item.isEmpty()) continue
                val parts = item.split("|")
                if (parts.size >= 3) {
                    val productId = parts[0].toLongOrNull() ?: continue
                    val qty = parts[2].toIntOrNull() ?: 1
                    repository.addReorderedToCart(productId, storeId, qty)
                }
            }
            // Auto select this store to switch UI context
            val store = repository.getStoreById(storeId)
            if (store != null) {
                selectedStore = store
            }
            switchTab(BottomTab.SHOP)
        }
    }

    fun toggleWatchlist(product: Product) {
        viewModelScope.launch {
            repository.toggleWatchlist(product.id, product.price, product.storeId)
            // Refresh detailed product state if looking at it
            if (detailedProduct?.id == product.id) {
                detailedProduct = repository.getProductById(product.id)
            }
        }
    }

    // Merchant operations
    fun updateProductPrice(productId: Long, newPrice: Double) {
        viewModelScope.launch {
            repository.updateProductPrice(productId, newPrice)
            // Sync detailed view if looking at it
            if (detailedProduct?.id == productId) {
                detailedProduct = repository.getProductById(productId)
            }
        }
    }

    fun updateProductStock(productId: Long, newStock: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, newStock)
            // Sync detailed view
            if (detailedProduct?.id == productId) {
                detailedProduct = repository.getProductById(productId)
            }
        }
    }

    fun searchProductByBarcode(barcode: String): Flow<Product?> = flow {
        emit(repository.getProductByBarcode(barcode))
    }

    fun simulateBarcodeScan(barcode: String) {
        viewModelScope.launch {
            val prod = repository.getProductByBarcode(barcode)
            if (prod != null) {
                barcodeResultMsg = "Successfully scanned: ${prod.emoji} ${prod.name}"
                // Display in detailed modal for rapid editing
                detailedProduct = prod
                activeTab = BottomTab.SHOP
            } else {
                barcodeResultMsg = "Scanning failed: Barcode '$barcode' not found in inventory."
            }
            delay(4000)
            barcodeResultMsg = null
        }
    }

    // Checkout: Places the order and launches the real-time delivery tracking simulator (FR-1.3)
    fun checkout(cartItems: List<CartItemWithProduct>, store: Store) {
        val total = cartItems.sumOf { it.price * it.quantity }
        val count = cartItems.sumOf { it.quantity }
        
        val itemsSummary = cartItems.joinToString(separator = ";") {
            "${it.productId}|${it.name.replace("|", "").replace(";", "")}|${it.quantity}|${it.price}|${it.emoji}|${it.unit.replace("|", "").replace(";", "")}"
        }
        
        viewModelScope.launch {
            var driverName = "Amit Kumar"
            var driverPhone = "+91 99882-12345"
            try {
                val apiService = com.example.data.api.RetrofitClient.apiService
                val request = com.example.data.api.OrderRequest(
                    storeId = store.id,
                    items = cartItems.map { com.example.data.api.OrderItemRequest(it.productId, it.quantity) },
                    totalAmount = total
                )
                val response = apiService.placeOrder(request)
                if (response.success) {
                    driverName = response.driverName
                    driverPhone = response.driverPhone
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val orderId = repository.insertOrder(
                DeliveryOrder(
                    storeId = store.id,
                    storeName = store.name,
                    totalAmount = total,
                    itemCount = count,
                    status = "Confirmed",
                    timestamp = System.currentTimeMillis(),
                    driverName = driverName,
                    driverPhone = driverPhone,
                    productsSummary = itemsSummary
                )
            )
            // Clear cart
            repository.clearCart()
            
            // Navigate to tracking
            selectedStore = null
            switchTab(BottomTab.ORDERS)
            
            // Run delivery progress tracking
            startDeliverySimulation(orderId)
        }
    }

    private fun startDeliverySimulation(orderId: Long) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            // Step 1: Confirmed initially. Wait 4 seconds, mark as "Picked Up"
            delay(4000)
            repository.updateOrderStatus(orderId, "Picked Up", 0.33f)

            // Step 2: Picked Up -> Out for Delivery after 5 seconds
            delay(5000)
            repository.updateOrderStatus(orderId, "Out for Delivery", 0.66f)

            // Step 3: Out for Delivery -> Delivered after 6 seconds
            delay(6000)
            repository.updateOrderStatus(orderId, "Delivered", 1.0f)

            // Log general system completion notification
            val order = repository.getOrderById(orderId)
            if (order != null) {
                repository.addNotification(
                    Notification(
                        title = "Order Delivered! 🛵",
                        message = "Your essentials package of ${order.itemCount} items from ${order.storeName} has been successfully dropped at your doorstep by ${order.driverName} in under 20 minutes!",
                        timestamp = System.currentTimeMillis(),
                        productId = 0,
                        storeName = order.storeName,
                        oldPrice = 0.0,
                        newPrice = 0.0,
                        isRead = false
                    )
                )
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}

// Factory to pass database instance into ViewModel securely
class NeighborCartViewModelFactory(
    private val application: Application,
    private val repository: NeighborCartRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NeighborCartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NeighborCartViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class GeofencedStore(
    val store: Store,
    val distanceKm: Double,
    val isDeliverable: Boolean
)

