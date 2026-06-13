package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Watchlist item combined with product and store details
data class WatchlistItemWithProduct(
    val productId: Long,
    val addedPrice: Double,
    val targetPrice: Double,
    val storeId: Long,
    val name: String,
    val category: String,
    val price: Double,
    val originalPrice: Double,
    val unit: String,
    val stock: Int,
    val emoji: String,
    val storeName: String
)

// Cart item combined with product details
data class CartItemWithProduct(
    val productId: Long,
    val storeId: Long,
    val quantity: Int,
    val name: String,
    val category: String,
    val price: Double,
    val unit: String,
    val stock: Int,
    val emoji: String
)

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores")
    fun getAllStores(): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<Store>)

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getStoreById(id: Long): Store?
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE storeId = :storeId")
    fun getProductsByStore(storeId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET price = :newPrice, originalPrice = :oldPrice WHERE id = :productId")
    suspend fun updateProductPrice(productId: Long, newPrice: Double, oldPrice: Double)

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateProductStock(productId: Long, newStock: Int)
}

@Dao
interface CartDao {
    @Query("""
        SELECT c.productId, c.storeId, c.quantity, p.name, p.category, p.price, p.unit, p.stock, p.emoji
        FROM cart_items c
        INNER JOIN products p ON c.productId = p.id
    """)
    fun getCartWithDetails(): Flow<List<CartItemWithProduct>>

    @Query("SELECT * FROM cart_items")
    suspend fun getRawCartItems(): List<CartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItemById(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface WatchlistDao {
    @Query("""
        SELECT w.productId, w.addedPrice, w.targetPrice, w.storeId, 
               p.name, p.category, p.price, p.originalPrice, p.unit, p.stock, p.emoji, 
               s.name as storeName
        FROM watchlist_items w
        INNER JOIN products p ON w.productId = p.id
        INNER JOIN stores s ON w.storeId = s.id
    """)
    fun getWatchlistWithDetails(): Flow<List<WatchlistItemWithProduct>>

    @Query("SELECT * FROM watchlist_items WHERE productId = :productId")
    suspend fun getWatchlistItemByProductId(productId: Long): WatchlistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlistItem: WatchlistItem)

    @Query("DELETE FROM watchlist_items WHERE productId = :productId")
    suspend fun deleteWatchlistByProductId(productId: Long)
}

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp ASC")
    fun getHistoryForProduct(productId: Long): Flow<List<PriceHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: PriceHistoryEntry)

    @Query("DELETE FROM price_history WHERE productId = :productId")
    suspend fun deleteHistoryForProduct(productId: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface DeliveryOrderDao {
    @Query("SELECT * FROM delivery_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<DeliveryOrder>>

    @Query("SELECT * FROM delivery_orders WHERE status != 'Delivered' LIMIT 1")
    fun getActiveOrderFlow(): Flow<DeliveryOrder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: DeliveryOrder): Long

    @Query("UPDATE delivery_orders SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateOrderStatus(id: Long, status: String, progress: Float)

    @Query("SELECT * FROM delivery_orders WHERE id = :id")
    suspend fun getOrderById(id: Long): DeliveryOrder?
}
