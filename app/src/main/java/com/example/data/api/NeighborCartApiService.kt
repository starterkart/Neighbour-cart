package com.example.data.api

import com.example.data.Product
import com.example.data.Store
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Headers
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- REAL SUPABASE DATA TRANSFER OBJECTS (DTOs) ---

data class SupabaseStore(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "rating") val rating: Float,
    @Json(name = "eta_minutes") val etaMinutes: Int,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "category") val category: String,
    @Json(name = "is_online") val isOnline: Boolean = true
) {
    fun toRoomStore(): Store = Store(
        id = id,
        name = name,
        description = description,
        rating = rating,
        etaMinutes = etaMinutes,
        latitude = latitude,
        longitude = longitude,
        category = category,
        isOnline = isOnline
    )

    companion object {
        fun fromRoomStore(store: Store): SupabaseStore = SupabaseStore(
            id = store.id,
            name = store.name,
            description = store.description,
            rating = store.rating,
            etaMinutes = store.etaMinutes,
            latitude = store.latitude,
            longitude = store.longitude,
            category = store.category,
            isOnline = store.isOnline
        )
    }
}

data class SupabaseProduct(
    @Json(name = "id") val id: Long,
    @Json(name = "store_id") val storeId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "price") val price: Double,
    @Json(name = "original_price") val originalPrice: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "stock") val stock: Int,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "description") val description: String,
    @Json(name = "emoji") val emoji: String
) {
    fun toRoomProduct(): Product = Product(
        id = id,
        storeId = storeId,
        name = name,
        category = category,
        price = price,
        originalPrice = originalPrice,
        unit = unit,
        stock = stock,
        barcode = barcode,
        description = description,
        emoji = emoji
    )

    companion object {
        fun fromRoomProduct(product: Product): SupabaseProduct = SupabaseProduct(
            id = product.id,
            storeId = product.storeId,
            name = product.name,
            category = product.category,
            price = product.price,
            originalPrice = product.originalPrice,
            unit = product.unit,
            stock = product.stock,
            barcode = product.barcode,
            description = product.description,
            emoji = product.emoji
        )
    }
}

data class SupabaseOrder(
    @Json(name = "store_id") val storeId: Long,
    @Json(name = "store_name") val storeName: String,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "item_count") val itemCount: Int,
    @Json(name = "status") val status: String,
    @Json(name = "driver_name") val driverName: String,
    @Json(name = "driver_phone") val driverPhone: String,
    @Json(name = "products_summary") val productsSummary: String
)

data class SupabaseAddress(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "phone") val phone: String,
    @Json(name = "name") val name: String?,
    @Json(name = "address") val address: String,
    @Json(name = "type_index") val typeIndex: Int,
    @Json(name = "created_at") val createdAt: String? = null
)

// Legacy wrappers to keep existing code functional
data class OrderRequest(
    val storeId: Long,
    val items: List<OrderItemRequest>,
    val totalAmount: Double
)

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int
)

data class OrderResponse(
    val success: Boolean,
    val orderId: Long,
    val estimatedArrivalMinutes: Int,
    val driverName: String,
    val driverPhone: String,
    val message: String
)

// --- RETROFIT API INTERFACE ---

interface NeighborCartApiService {
    // Legacy simulated mock endpoints (redirected dynamically if Supabase configured)
    @GET("stores")
    suspend fun getStores(): List<Store>

    @GET("products")
    suspend fun getProducts(): List<Product>

    @POST("orders/place")
    suspend fun placeOrder(@Body orderData: OrderRequest): OrderResponse

    // Supabase REST endpoints (PostgREST table targets)
    @GET("stores")
    suspend fun getSupabaseStores(): List<SupabaseStore>

    @GET("products")
    suspend fun getSupabaseProducts(): List<SupabaseProduct>

    @POST("delivery_orders")
    suspend fun postSupabaseOrder(@Body order: SupabaseOrder): List<Map<String, Any>>

    @GET("addresses")
    suspend fun getSupabaseAddresses(@retrofit2.http.Query("phone") phoneParam: String): List<SupabaseAddress>

    @POST("addresses")
    suspend fun postSupabaseAddress(@Body address: SupabaseAddress): List<Map<String, Any>>

    @POST("stores")
    suspend fun uploadStoresToSupabase(@Body stores: List<SupabaseStore>)

    @POST("products")
    suspend fun uploadProductsToSupabase(@Body products: List<SupabaseProduct>)
}

// --- CLIENT ENGINE & RE-ROUTING ENGINE ---

object RetrofitClient {
    private const val DEFAULT_MOCK_URL = "https://hyperlocal-delivery-api.neighborcart.io/v1/"

    // Thread-safe dynamic backend properties
    @Volatile var activeSupabaseUrl: String = ""
    @Volatile var activeSupabaseKey: String = ""

    fun updateConfig(url: String, key: String) {
        activeSupabaseUrl = url.trim().removeSuffix("/")
        activeSupabaseKey = key.trim()
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Dynamic core interceptor: routes to Supabase if configured, otherwise falls back to local JSON
    private val supabaseRequestInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()

        val urlHost = activeSupabaseUrl
        val apiKey = activeSupabaseKey

        // If credentials are NOT provided, automatically route to mock responses
        if (urlHost.isEmpty() || apiKey.isEmpty()) {
            return@Interceptor handleMockResponses(chain, originalUrl)
        }

        // We have active Supabase credentials! Rewrite standard queries to Supabase PostgREST endpoints
        try {
            val endpoint = when {
                originalUrl.contains("stores") -> "stores"
                originalUrl.contains("products") -> "products"
                originalUrl.contains("orders/place") || originalUrl.contains("delivery_orders") -> "delivery_orders"
                originalUrl.contains("addresses") -> "addresses"
                else -> originalUrl.substringAfterLast("/")
            }

            val targetUrl = "$urlHost/rest/v1/$endpoint"

            val supabaseRequestBuilder = originalRequest.newBuilder()
                .url(targetUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")

            val finalRequest = supabaseRequestBuilder.build()
            val response = chain.proceed(finalRequest)

            // If the query is successful, pass it forward, otherwise if it's 404 or auth failure, allow interceptor to handle gracefully or fallback
            if (response.isSuccessful) {
                return@Interceptor response
            } else {
                // If remote database returns an error, log it and try mock fallback to ensure robust app never breaks!
                Log.e("SupabaseSync", "Supabase error code ${response.code}: ${response.message}. Falling back to sandbox mode.")
                response.close()
                return@Interceptor handleMockResponses(chain, originalUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@Interceptor handleMockResponses(chain, originalUrl)
        }
    }

    // Standard high-fidelity delivery database sandbox
    private fun handleMockResponses(chain: okhttp3.Interceptor.Chain, url: String): Response {
        val request = chain.request()
        val responseBuilder = Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .message("OK")

        when {
            url.contains("stores") -> {
                val json = """
                [
                    {
                        "id": 101,
                        "name": "Neighbor Cart",
                        "description": "Get dairy, bread, eggs, fresh veggies & daily essentials delivered in under 15 minutes.",
                        "rating": 4.9,
                        "eta_minutes": 15,
                        "latitude": 12.971598,
                        "longitude": 77.594562,
                        "category": "Super Store",
                        "is_online": true
                    },
                    {
                        "id": 102,
                        "name": "Suburban Express",
                        "description": "Your local neighborhood grocer with organic produce, household items, clean-labeled goods.",
                        "rating": 4.6,
                        "eta_minutes": 10,
                        "latitude": 12.920500,
                        "longitude": 77.520500,
                        "category": "Organic Groceries",
                        "is_online": true
                    },
                    {
                        "id": 103,
                        "name": "Metro Core Mart",
                        "description": "Fulfillment hub inside Indiranagar. Best for wholesale dairy, bakery, beverages and bulk packs.",
                        "rating": 4.7,
                        "eta_minutes": 20,
                        "latitude": 12.981000,
                        "longitude": 77.601000,
                        "category": "Wholesale Hub",
                        "is_online": true
                    },
                    {
                        "id": 104,
                        "name": "Srinagar Valley Mart",
                        "description": "Our partner store near Dal Lake. Local Kashmiri apples, Kahwa saffron tea, pure walnuts, fresh buffalo milk, and other valley essentials.",
                        "rating": 4.9,
                        "eta_minutes": 12,
                        "latitude": 34.15944035944071,
                        "longitude": 74.81705853251457,
                        "category": "Kashmir Essentials",
                        "is_online": true
                    }
                ]
                """.trimIndent()
                return responseBuilder
                    .code(200)
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            url.contains("products") -> {
                val json = """
                [
                    {
                        "id": 1001,
                        "store_id": 101,
                        "name": "Fresh Whole Milk Premium",
                        "category": "Dairy & Eggs",
                        "price": 52.0,
                        "original_price": 52.0,
                        "unit": "1 Litre",
                        "stock": 25,
                        "barcode": "890101",
                        "description": "Homogenized and pasteurized farm fresh cow milk. High protein and delicious.",
                        "emoji": "🥛"
                    },
                    {
                        "id": 1002,
                        "store_id": 101,
                        "name": "Brown Farm Eggs (Large)",
                        "category": "Dairy & Eggs",
                        "price": 85.0,
                        "original_price": 95.0,
                        "unit": "12 units",
                        "stock": 15,
                        "barcode": "890102",
                        "description": "Rich source of proteins, free-range and ethically produced large brown eggs.",
                        "emoji": "🥚"
                    },
                    {
                        "id": 1003,
                        "store_id": 101,
                        "name": "Artisanal White Bread",
                        "category": "Bakery",
                        "price": 45.0,
                        "original_price": 45.0,
                        "unit": "400 grams",
                        "stock": 8,
                        "barcode": "890103",
                        "description": "Freshly baked soft sandwich bread slice. Preservative-free.",
                        "emoji": "🍞"
                    },
                    {
                        "id": 1004,
                        "store_id": 101,
                        "name": "Roasted Premium Filter Coffee",
                        "category": "Beverages & Coffee",
                        "price": 290.0,
                        "original_price": 320.0,
                        "unit": "250g Pack",
                        "stock": 12,
                        "barcode": "890104",
                        "description": "Traditional South Indian filter coffee blend of selected Arabica & Chicory.",
                        "emoji": "☕"
                    },
                    {
                        "id": 1005,
                        "store_id": 101,
                        "name": "Sweet Red Apples",
                        "category": "Fruits & Veggies",
                        "price": 140.0,
                        "original_price": 140.0,
                        "unit": "500 grams",
                        "stock": 18,
                        "barcode": "890105",
                        "description": "Crisp, sweet, and juicy hand-picked Washington apples.",
                        "emoji": "🍎"
                    },
                    {
                        "id": 4001,
                        "store_id": 104,
                        "name": "Fresh Himalayan Apple",
                        "category": "Fruits & Veggies",
                        "price": 150.0,
                        "original_price": 160.0,
                        "unit": "1 kg",
                        "stock": 45,
                        "barcode": "890401",
                        "description": "Crisp, sweet, organic local Kashmiri apples freshly plucked from Srinagar valley orchards.",
                        "emoji": "🍎"
                    },
                    {
                        "id": 4002,
                        "store_id": 104,
                        "name": "Kashmiri Kahwa Saffron Tea",
                        "category": "Beverages & Coffee",
                        "price": 280.0,
                        "original_price": 300.0,
                        "unit": "100g Box",
                        "stock": 30,
                        "barcode": "890402",
                        "description": "Traditional Kashmiri green tea blend with pure saffron threads, cinnamon, and cardamoms.",
                        "emoji": "🍵"
                    },
                    {
                        "id": 4003,
                        "store_id": 104,
                        "name": "Organic Saffron Threads",
                        "category": "Daily Essentials",
                        "price": 450.0,
                        "original_price": 450.0,
                        "unit": "1 gram",
                        "stock": 15,
                        "barcode": "890403",
                        "description": "Premium grade, hand-picked pure red saffron stigma. Collected carefully in Pampore orchards near Srinagar.",
                        "emoji": "🌸"
                    },
                    {
                        "id": 4004,
                        "store_id": 104,
                        "name": "Whole Walnuts (In Shell)",
                        "category": "Daily Essentials",
                        "price": 390.0,
                        "original_price": 420.0,
                        "unit": "500 grams",
                        "stock": 25,
                        "barcode": "890404",
                        "description": "Premium thin-shelled Kashmiri walnuts with rich oil content, crunchy texture, and high nutrition inside.",
                        "emoji": "🌰"
                    },
                    {
                        "id": 4005,
                        "store_id": 104,
                        "name": "Fresh Valley Buffalo Milk",
                        "category": "Dairy & Eggs",
                        "price": 70.0,
                        "original_price": 70.0,
                        "unit": "1 Litre",
                        "stock": 20,
                        "barcode": "890405",
                        "description": "Pasteurized rich whole buffalo milk sourced directly from cooperative dairies around Dal Lake.",
                        "emoji": "🥛"
                    }
                ]
                """.trimIndent()
                return responseBuilder
                    .code(200)
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            url.contains("orders/place") || url.contains("delivery_orders") -> {
                val json = """
                {
                    "success": true,
                    "orderId": ${System.currentTimeMillis()},
                    "estimatedArrivalMinutes": 15,
                    "driverName": "Amit Kumar (Super Rider)",
                    "driverPhone": "+91 99882-12345",
                    "message": "Order successfully placed and routed to your nearest delivery partner."
                }
                """.trimIndent()
                return responseBuilder
                    .code(200)
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            url.contains("addresses") -> {
                val json = "[]"
                return responseBuilder
                    .code(200)
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            else -> {
                return responseBuilder
                    .code(404)
                    .body("Endpoint not found".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(supabaseRequestInterceptor)
        .build()

    val apiService: NeighborCartApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_MOCK_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NeighborCartApiService::class.java)
    }
}

// Places APIs removed

// Google Places removed as per zero-cost requirement.
