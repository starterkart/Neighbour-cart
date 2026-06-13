package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Store::class,
        Product::class,
        CartItem::class,
        WatchlistItem::class,
        PriceHistoryEntry::class,
        Notification::class,
        DeliveryOrder::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NeighborCartDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun deliveryOrderDao(): DeliveryOrderDao

    companion object {
        @Volatile
        private var INSTANCE: NeighborCartDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NeighborCartDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NeighborCartDatabase::class.java,
                    "neighborcart_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        private suspend fun populateDatabase(db: NeighborCartDatabase) {
            val notificationDao = db.notificationDao()
            notificationDao.insertNotification(
                Notification(
                    title = "NeighborCart: Backend Sync Active! 🎉",
                    message = "Welcome to your hyperlocal companion. All stores and products are synchronized directly from the real REST backend service.",
                    timestamp = System.currentTimeMillis(),
                    productId = 0,
                    storeName = "System",
                    oldPrice = 0.0,
                    newPrice = 0.0,
                    isRead = false
                )
            )
            if (1 == 1) return
            val storeDao = db.storeDao()
            val productDao = db.productDao()
            val historyDao = db.priceHistoryDao()
            val dummyNotificationDao = db.notificationDao()

            // Seed Stores
            val initialStores = listOf(
                Store(
                    id = 101,
                    name = "Neighbor Cart",
                    description = "Get dairy, bread, eggs, fresh veggies & daily essentials delivered in under 15 minutes.",
                    rating = 4.9f,
                    etaMinutes = 15,
                    latitude = 12.971598, // Current location coordinate baseline
                    longitude = 77.594562,
                    category = "Super Store"
                ),
                Store(
                    id = 102,
                    name = "Suburban Express",
                    description = "Your local neighborhood grocer with organic produce, household items, clean-labeled goods.",
                    rating = 4.6f,
                    etaMinutes = 10,
                    latitude = 12.920500, // Near Suburban Home
                    longitude = 77.520500,
                    category = "Organic Groceries"
                ),
                Store(
                    id = 103,
                    name = "Metro Core Mart",
                    description = "Fulfillment hub inside Indiranagar. Best for wholesale dairy, bakery, beverages and bulk packs.",
                    rating = 4.7f,
                    etaMinutes = 20,
                    latitude = 12.981000, // Near Metro Station
                    longitude = 77.601000,
                    category = "Wholesale Hub"
                ),
                Store(
                    id = 104,
                    name = "Srinagar Valley Mart",
                    description = "Our partner store near Dal Lake. Local Kashmiri apples, Kahwa saffron tea, pure walnuts, fresh buffalo milk, and other valley essentials.",
                    rating = 4.9f,
                    etaMinutes = 12,
                    latitude = 34.15944035944071,
                    longitude = 74.81705853251457,
                    category = "Kashmir Essentials"
                )
            )
            storeDao.insertStores(initialStores)

            // Seed Products
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L

            val initialProducts = listOf(
                // Store 101 Products: Neighbor Cart
                Product(
                    id = 1001,
                    storeId = 101,
                    name = "Fresh Whole Milk Premium",
                    category = "Dairy & Eggs",
                    price = 52.00,
                    originalPrice = 52.00,
                    unit = "1 Litre",
                    stock = 25,
                    barcode = "890101",
                    description = "Homogenized and pasteurized farm fresh cow milk. High protein and delicious.",
                    emoji = "🥛"
                ),
                Product(
                    id = 1002,
                    storeId = 101,
                    name = "Brown Farm Eggs (Large)",
                    category = "Dairy & Eggs",
                    price = 85.00,
                    originalPrice = 95.00, // simulated price drop init!
                    unit = "12 units",
                    stock = 15,
                    barcode = "890102",
                    description = "Rich source of proteins, free-range and ethically produced large brown eggs.",
                    emoji = "🥚"
                ),
                Product(
                    id = 1003,
                    storeId = 101,
                    name = "Artisanal White Bread",
                    category = "Bakery",
                    price = 45.00,
                    originalPrice = 45.00,
                    unit = "400 grams",
                    stock = 8,
                    barcode = "890103",
                    description = "Freshly baked soft sandwich bread slice. Preservative-free.",
                    emoji = "🍞"
                ),
                Product(
                    id = 1004,
                    storeId = 101,
                    name = "Roasted Premium Filter Coffee",
                    category = "Beverages & Coffee",
                    price = 290.00,
                    originalPrice = 320.00, // simulated drop
                    unit = "250g Pack",
                    stock = 12,
                    barcode = "890104",
                    description = "Traditional South Indian filter coffee blend of selected Arabica & Chicory.",
                    emoji = "☕"
                ),
                Product(
                    id = 1005,
                    storeId = 101,
                    name = "Sweet Red Apples",
                    category = "Fruits & Veggies",
                    price = 140.00,
                    originalPrice = 140.00,
                    unit = "500 grams",
                    stock = 18,
                    barcode = "890105",
                    description = "Crisp, sweet, and juicy hand-picked Washington apples.",
                    emoji = "🍎"
                ),

                // Now all other fruits/veg and items mapped to 101
                Product(
                    id = 1006,
                    storeId = 101,
                    name = "Organic Whole Milk",
                    category = "Dairy & Eggs",
                    price = 72.00,
                    originalPrice = 72.00,
                    unit = "1 Litre",
                    stock = 20,
                    barcode = "890106",
                    description = "Certified 100% organic pasture-raised milk. Pristine nutrition directly from grass-fed cows.",
                    emoji = "🥛"
                ),
                Product(
                    id = 1007,
                    storeId = 101,
                    name = "Premium Arabica Coffee Beans",
                    category = "Beverages & Coffee",
                    price = 450.00,
                    originalPrice = 490.00, // simulated drop
                    unit = "250g Pack",
                    stock = 10,
                    barcode = "890107",
                    description = "Single-origin whole beans roasted medium for a smooth and chocolatey body.",
                    emoji = "🫘"
                ),
                Product(
                    id = 1008,
                    storeId = 101,
                    name = "California Avocados",
                    category = "Fruits & Veggies",
                    price = 190.00,
                    originalPrice = 190.00,
                    unit = "2 units",
                    stock = 7,
                    barcode = "890108",
                    description = "Hass variety, buttery taste and perfect for guacamole or breakfast toasts.",
                    emoji = "🥑"
                ),
                Product(
                    id = 1009,
                    storeId = 101,
                    name = "Fresh Organic Bananas",
                    category = "Fruits & Veggies",
                    price = 60.00,
                    originalPrice = 65.00, // drop
                    unit = "6 units",
                    stock = 24,
                    barcode = "890109",
                    description = "Directly sourced from trusted local organic farmers. Naturally sweet.",
                    emoji = "🍌"
                ),

                // Former mega saver items remapped to 101
                Product(
                    id = 1010,
                    storeId = 101,
                    name = "Bulk Whole Milk Carton",
                    category = "Dairy & Eggs",
                    price = 45.00,
                    originalPrice = 45.00,
                    unit = "1 Litre",
                    stock = 50,
                    barcode = "890110",
                    description = "Standard wholesale packaged whole milk container, budget essential.",
                    emoji = "🥛"
                ),
                Product(
                    id = 1011,
                    storeId = 101,
                    name = "Instant Coffee Classic",
                    category = "Beverages & Coffee",
                    price = 220.00,
                    originalPrice = 250.00, // drop
                    unit = "100g Jar",
                    stock = 40,
                    barcode = "890111",
                    description = "Soluble coffee granules made from selected Robusta and Arabica blends.",
                    emoji = "☕"
                ),
                Product(
                    id = 1012,
                    storeId = 101,
                    name = "Discount Family Eggs Pack",
                    category = "Dairy & Eggs",
                    price = 150.00,
                    originalPrice = 175.00, // drop
                    unit = "30 units",
                    stock = 30,
                    barcode = "890112",
                    description = "Value tray of fresh table white eggs for large household needs.",
                    emoji = "🥚"
                ),

                // Former remote item mapped to 101
                Product(
                    id = 1013,
                    storeId = 101,
                    name = "Pure Organic Honey",
                    category = "Daily Essentials",
                    price = 320.00,
                    originalPrice = 320.00,
                    unit = "500 grams",
                    stock = 5,
                    barcode = "890113",
                    description = "Pure forest honey, 100% natural and unfiltered.",
                    emoji = "🍯"
                ),
                // Store 102 Products: Suburban Express
                Product(
                    id = 2001,
                    storeId = 102,
                    name = "Organic Almond Milk",
                    category = "Dairy & Eggs",
                    price = 120.00,
                    originalPrice = 120.00,
                    unit = "1 Litre",
                    stock = 8,
                    barcode = "890201",
                    description = "Pure organic unsweetened almond milk. Lactose-free, dairy-free alternative.",
                    emoji = "🥛"
                ),
                Product(
                    id = 2002,
                    storeId = 102,
                    name = "Organic Avocados Pack",
                    category = "Fruits & Veggies",
                    price = 220.00,
                    originalPrice = 240.00,
                    unit = "2 units",
                    stock = 12,
                    barcode = "890202",
                    description = "Delicious, handpicked organic avocados from local farms.",
                    emoji = "🥑"
                ),
                Product(
                    id = 2003,
                    storeId = 102,
                    name = "Sourdough Artisanal Bread",
                    category = "Bakery",
                    price = 90.00,
                    originalPrice = 90.00,
                    unit = "500 grams",
                    stock = 5,
                    barcode = "890203",
                    description = "Crunchy, chewy French style classic sourdough boule.",
                    emoji = "🍞"
                ),
                // Store 103 Products: Metro Core Mart
                Product(
                    id = 3001,
                    storeId = 103,
                    name = "Bulk Whole Milk Carton",
                    category = "Dairy & Eggs",
                    price = 45.00,
                    originalPrice = 45.00,
                    unit = "1 Litre",
                    stock = 100,
                    barcode = "890301",
                    description = "Standard wholesale packaged whole milk container, budget essential.",
                    emoji = "🥛"
                ),
                Product(
                    id = 3002,
                    storeId = 103,
                    name = "Value Eggs Tray (30 units)",
                    category = "Dairy & Eggs",
                    price = 180.00,
                    originalPrice = 200.00,
                    unit = "30 units",
                    stock = 50,
                    barcode = "890302",
                    description = "Huge table white eggs value tray for whole-family breakfast.",
                    emoji = "🥚"
                ),
                Product(
                    id = 3003,
                    storeId = 103,
                    name = "Indiranagar Coffee Blend",
                    category = "Beverages & Coffee",
                    price = 350.00,
                    originalPrice = 380.00,
                    unit = "500g Pack",
                    stock = 30,
                    barcode = "890303",
                    description = "Specialty custom medium dark roast whole beans. Bold flavor profile with chicory.",
                    emoji = "☕"
                ),
                Product(
                    id = 4001,
                    storeId = 104,
                    name = "Fresh Himalayan Apple",
                    category = "Fruits & Veggies",
                    price = 150.00,
                    originalPrice = 160.00,
                    unit = "1 kg",
                    stock = 45,
                    barcode = "890401",
                    description = "Crisp, sweet, organic local Kashmiri apples freshly plucked from Srinagar valley orchards.",
                    emoji = "🍎"
                ),
                Product(
                    id = 4002,
                    storeId = 104,
                    name = "Kashmiri Kahwa Saffron Tea",
                    category = "Beverages & Coffee",
                    price = 280.00,
                    originalPrice = 300.00,
                    unit = "100g Box",
                    stock = 30,
                    barcode = "890402",
                    description = "Traditional Kashmiri green tea blend with pure saffron threads, cinnamon, and cardamoms.",
                    emoji = "🍵"
                ),
                Product(
                    id = 4003,
                    storeId = 104,
                    name = "Organic Saffron Threads",
                    category = "Daily Essentials",
                    price = 450.00,
                    originalPrice = 450.00,
                    unit = "1 gram",
                    stock = 15,
                    barcode = "890403",
                    description = "Premium grade, hand-picked pure red saffron stigma. Collected carefully in Pampore orchards near Srinagar.",
                    emoji = "🌸"
                ),
                Product(
                    id = 4004,
                    storeId = 104,
                    name = "Whole Walnuts (In Shell)",
                    category = "Daily Essentials",
                    price = 390.00,
                    originalPrice = 420.00,
                    unit = "500 grams",
                    stock = 25,
                    barcode = "890404",
                    description = "Premium thin-shelled Kashmiri walnuts with rich oil content, crunchy texture, and high nutrition inside.",
                    emoji = "🌰"
                ),
                Product(
                    id = 4005,
                    storeId = 104,
                    name = "Fresh Valley Buffalo Milk",
                    category = "Dairy & Eggs",
                    price = 70.00,
                    originalPrice = 70.00,
                    unit = "1 Litre",
                    stock = 20,
                    barcode = "890405",
                    description = "Pasteurized rich whole buffalo milk sourced directly from cooperative dairies around Dal Lake.",
                    emoji = "🥛"
                )
            )
            productDao.insertProducts(initialProducts)

            // Seed Price History for all products to populate beautiful charts
            val historySeeds = mutableListOf<PriceHistoryEntry>()
            for (prod in initialProducts) {
                // Generate a 30-day random trend ending at actual price
                val base = prod.originalPrice
                val stepTimes = listOf(
                    now - 30 * oneDayMs,
                    now - 24 * oneDayMs,
                    now - 18 * oneDayMs,
                    now - 12 * oneDayMs,
                    now - 6 * oneDayMs,
                    now - 1 * oneDayMs
                )
                val multipliers = listOf(1.10, 1.05, 1.07, 1.02, 1.05, 1.00)

                for (i in stepTimes.indices) {
                    val p = Math.round(base * multipliers[i] * 100.0) / 100.0
                    historySeeds.add(
                        PriceHistoryEntry(
                            productId = prod.id,
                            price = if (i == stepTimes.size - 1) prod.price else p,
                            timestamp = stepTimes[i]
                        )
                    )
                }
            }
            for (h in historySeeds) {
                historyDao.insertHistoryEntry(h)
            }

            // Seed first Welcome Notification
            notificationDao.insertNotification(
                Notification(
                    title = "NeighborCart Open for Business! 🎉",
                    message = "Welcome to your hyperlocal companion. Get dairy, coffee, and fresh food delivered in under 25 mins from shops within 5km! Save items to track prices and receive alerts.",
                    timestamp = now,
                    productId = 0,
                    storeName = "System",
                    oldPrice = 0.0,
                    newPrice = 0.0,
                    isRead = false
                )
            )
        }
    }
}
