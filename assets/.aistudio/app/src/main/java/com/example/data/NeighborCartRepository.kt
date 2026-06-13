package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class NeighborCartRepository(
    private val database: NeighborCartDatabase,
    private val apiService: com.example.data.api.NeighborCartApiService = com.example.data.api.RetrofitClient.apiService
) {

    private val storeDao = database.storeDao()
    private val productDao = database.productDao()
    private val cartDao = database.cartDao()
    private val watchlistDao = database.watchlistDao()
    private val priceHistoryDao = database.priceHistoryDao()
    private val notificationDao = database.notificationDao()
    private val orderDao = database.deliveryOrderDao()

    // Real-Backend Data Sync pipeline with Supabase & Offline-Sandbox adaptiveness
    suspend fun syncWithBackend(): Boolean {
        return try {
            val remoteStores = apiService.getSupabaseStores()
            val remoteProducts = apiService.getSupabaseProducts()
            if (remoteStores.isNotEmpty()) {
                storeDao.insertStores(remoteStores.map { it.toRoomStore() })
            }
            if (remoteProducts.isNotEmpty()) {
                productDao.insertProducts(remoteProducts.map { it.toRoomProduct() })
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to legacy models parsing
            try {
                val legacyStores = apiService.getStores()
                val legacyProducts = apiService.getProducts()
                if (legacyStores.isNotEmpty()) {
                    storeDao.insertStores(legacyStores)
                }
                if (legacyProducts.isNotEmpty()) {
                    productDao.insertProducts(legacyProducts)
                }
                true
            } catch (inner: Exception) {
                inner.printStackTrace()
                false
            }
        }
    }

    // Direct database push mechanism to populate user's newly created Supabase project tables
    suspend fun pushSeedDataToSupabase(): Boolean {
        return try {
            val initialStores = listOf(
                com.example.data.api.SupabaseStore(101, "Neighbor Cart", "Get dairy, bread, eggs, fresh veggies & daily essentials delivered in under 15 minutes.", 4.9f, 15, 12.971598, 77.594562, "Super Store", true),
                com.example.data.api.SupabaseStore(102, "Suburban Express", "Your local neighborhood grocer with organic produce, household items, clean-labeled goods.", 4.6f, 10, 12.9205, 77.5205, "Organic Groceries", true),
                com.example.data.api.SupabaseStore(103, "Metro Core Mart", "Fulfillment hub inside Indiranagar. Best for wholesale dairy, bakery, beverages and bulk packs.", 4.7f, 20, 12.981, 77.601, "Wholesale Hub", true),
                com.example.data.api.SupabaseStore(104, "Srinagar Valley Mart", "Our partner store near Dal Lake. Local Kashmiri apples, Kahwa saffron tea, pure walnuts, fresh buffalo milk, and other valley essentials.", 4.9f, 12, 34.15944035944071, 74.81705853251457, "Kashmir Essentials", true)
            )

            val initialProducts = listOf(
                com.example.data.api.SupabaseProduct(1001, 101, "Fresh Whole Milk Premium", "Dairy & Eggs", 52.0, 52.0, "1 Litre", 25, "890101", "Homogenized and pasteurized farm fresh cow milk.", "🥛"),
                com.example.data.api.SupabaseProduct(1002, 101, "Brown Farm Eggs (Large)", "Dairy & Eggs", 85.0, 95.0, "12 units", 15, "890102", "Rich source of proteins, free-range large brown eggs.", "🥚"),
                com.example.data.api.SupabaseProduct(1003, 101, "Artisanal White Bread", "Bakery", 45.0, 45.0, "400 grams", 8, "890103", "Freshly baked soft sandwich bread slice.", "🍞"),
                com.example.data.api.SupabaseProduct(1004, 101, "Roasted Premium Filter Coffee", "Beverages & Coffee", 290.0, 320.0, "250g Pack", 12, "890104", "Traditional South Indian filter coffee blend.", "☕"),
                com.example.data.api.SupabaseProduct(1005, 101, "Sweet Red Apples", "Fruits & Veggies", 140.0, 140.0, "500 grams", 18, "890105", "Crisp, sweet, and juicy hand-picked red apples.", "🍎"),
                com.example.data.api.SupabaseProduct(4001, 104, "Fresh Himalayan Apple", "Fruits & Veggies", 150.0, 160.0, "1 kg", 45, "890401", "Crisp, sweet, organic local Kashmiri apples.", "🍎"),
                com.example.data.api.SupabaseProduct(4002, 104, "Kashmiri Kahwa Saffron Tea", "Beverages & Coffee", 280.0, 300.0, "100g Box", 30, "890402", "Traditional Kashmiri green tea blend with saffron.", "🍵"),
                com.example.data.api.SupabaseProduct(4003, 104, "Organic Saffron Threads", "Daily Essentials", 450.0, 450.0, "1 gram", 15, "890403", "Premium grade, hand-picked pure red saffron.", "🌸"),
                com.example.data.api.SupabaseProduct(4004, 104, "Whole Walnuts (In Shell)", "Daily Essentials", 390.0, 420.0, "500 grams", 25, "890404", "Premium thin-shelled Kashmiri walnuts.", "🌰"),
                com.example.data.api.SupabaseProduct(4005, 104, "Fresh Valley Buffalo Milk", "Dairy & Eggs", 70.0, 70.0, "1 Litre", 20, "890405", "Pasteurized rich whole buffalo milk.", "🥛")
            )

            apiService.uploadStoresToSupabase(initialStores)
            apiService.uploadProductsToSupabase(initialProducts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Store APIs
    val allStoresFlow: Flow<List<Store>> = storeDao.getAllStores()
    suspend fun getStoreById(id: Long): Store? = storeDao.getStoreById(id)

    // Product APIs
    fun getProductsByStoreFlow(storeId: Long): Flow<List<Product>> = productDao.getProductsByStore(storeId)
    val allProductsFlow: Flow<List<Product>> = productDao.getAllProductsFlow()
    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)
    
    // Merchant product updater and price history trigger
    suspend fun updateProductStock(productId: Long, newStock: Int) {
        productDao.updateProductStock(productId, newStock)
    }

    suspend fun updateProductPrice(productId: Long, newPrice: Double) {
        val product = productDao.getProductById(productId) ?: return
        val oldPrice = product.price
        
        // Update product in DB
        productDao.updateProductPrice(productId, newPrice, oldPrice)
        
        // Add to history trends
        priceHistoryDao.insertHistoryEntry(
            PriceHistoryEntry(
                productId = productId,
                price = newPrice,
                timestamp = System.currentTimeMillis()
            )
        )

        // Check if price was lowered and product is in watchlist -> Trigger Alert
        if (newPrice < oldPrice) {
            val watchlistItem = watchlistDao.getRawWatchlistItemById(productId)
            if (watchlistItem != null) {
                val store = storeDao.getStoreById(product.storeId)
                val storeName = store?.name ?: "Local Shop"
                
                // Add alert notification log
                notificationDao.insertNotification(
                    Notification(
                        title = "${product.emoji} Price Drop Alert: ${product.name}!",
                        message = "Wow! Price dropped to ₹${String.format("%.2f", newPrice)} (was ₹${String.format("%.2f", oldPrice)}) at $storeName. Act fast to secure this deal!",
                        timestamp = System.currentTimeMillis(),
                        productId = productId,
                        storeName = storeName,
                        oldPrice = oldPrice,
                        newPrice = newPrice
                    )
                )
            }
        }
    }

    // Workaround helper to avoid extra custom DAO query types
    private suspend fun WatchlistDao.getRawWatchlistItemById(productId: Long): WatchlistItem? {
        return this.getWatchlistItemByProductId(productId)
    }

    // Cart APIs
    val cartWithDetailsFlow: Flow<List<CartItemWithProduct>> = cartDao.getCartWithDetails()
    suspend fun getRawCartItems(): List<CartItem> = cartDao.getRawCartItems()

    suspend fun addProductToCart(productId: Long, storeId: Long) {
        val cartItems = cartDao.getRawCartItems()
        val existingItem = cartItems.find { it.productId == productId }
        
        if (existingItem != null) {
            cartDao.insertCartItem(existingItem.copy(quantity = existingItem.quantity + 1))
        } else {
            cartDao.insertCartItem(CartItem(productId = productId, storeId = storeId, quantity = 1))
        }
    }

    suspend fun decreaseProductCartQuantity(productId: Long) {
        val cartItems = cartDao.getRawCartItems()
        val existingItem = cartItems.find { it.productId == productId } ?: return
        
        if (existingItem.quantity > 1) {
            cartDao.insertCartItem(existingItem.copy(quantity = existingItem.quantity - 1))
        } else {
            cartDao.deleteCartItem(existingItem)
        }
    }

    suspend fun removeProductFromCart(productId: Long) {
        cartDao.deleteCartItemById(productId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    suspend fun addReorderedToCart(productId: Long, storeId: Long, quantity: Int) {
        cartDao.insertCartItem(CartItem(productId = productId, storeId = storeId, quantity = quantity))
    }

    // Watchlist APIs
    val watchlistWithDetailsFlow: Flow<List<WatchlistItemWithProduct>> = watchlistDao.getWatchlistWithDetails()
    
    suspend fun isItemWatched(productId: Long): Boolean {
        return watchlistDao.getWatchlistItemByProductId(productId) != null
    }

    suspend fun toggleWatchlist(productId: Long, targetPrice: Double, storeId: Long) {
        val existing = watchlistDao.getWatchlistItemByProductId(productId)
        if (existing != null) {
            watchlistDao.deleteWatchlistByProductId(productId)
        } else {
            watchlistDao.insertWatchlist(
                WatchlistItem(
                    productId = productId,
                    addedPrice = targetPrice,
                    targetPrice = targetPrice,
                    storeId = storeId
                )
            )
        }
    }

    // Price History APIs
    fun getHistoryForProductFlow(productId: Long): Flow<List<PriceHistoryEntry>> {
        return priceHistoryDao.getHistoryForProduct(productId)
    }

    // Notifications APIs
    val allNotificationsFlow: Flow<List<Notification>> = notificationDao.getAllNotifications()
    suspend fun markAllNotificationsAsRead() = notificationDao.markAllAsRead()
    suspend fun clearAllNotifications() = notificationDao.clearAllNotifications()
    suspend fun addNotification(notification: Notification) = notificationDao.insertNotification(notification)

    // Order & Tracking APIs
    val allOrdersFlow: Flow<List<DeliveryOrder>> = orderDao.getAllOrders()
    val activeOrderFlow: Flow<DeliveryOrder?> = orderDao.getActiveOrderFlow()
    
    suspend fun insertOrder(order: DeliveryOrder): Long {
        return orderDao.insertOrder(order)
    }

    suspend fun updateOrderStatus(orderId: Long, status: String, progress: Float) {
        orderDao.updateOrderStatus(orderId, status, progress)
    }

    suspend fun getOrderById(id: Long): DeliveryOrder? {
        return orderDao.getOrderById(id)
    }
}
