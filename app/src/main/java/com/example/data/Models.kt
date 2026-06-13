package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
    val rating: Float,
    val etaMinutes: Int,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val isOnline: Boolean = true
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Long,
    val storeId: Long,
    val name: String,
    val category: String,
    val price: Double,
    val originalPrice: Double,
    val unit: String,
    val stock: Int,
    val barcode: String,
    val description: String,
    val emoji: String
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val productId: Long,
    val storeId: Long,
    val quantity: Int
)

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey val productId: Long,
    val addedPrice: Double,
    val targetPrice: Double,
    val storeId: Long
)

@Entity(tableName = "price_history")
data class PriceHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val price: Double,
    val timestamp: Long
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val productId: Long,
    val storeName: String,
    val oldPrice: Double,
    val newPrice: Double,
    val isRead: Boolean = false
)

@Entity(tableName = "delivery_orders")
data class DeliveryOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeId: Long,
    val storeName: String,
    val totalAmount: Double,
    val itemCount: Int,
    val status: String, // "Placed", "Accepted", "At Store", "Out for Delivery", "Delivered"
    val timestamp: Long,
    val driverName: String,
    val driverPhone: String,
    val progress: Float = 0.0f,
    val productsSummary: String = "" // semi-colon delimited: productId|name|quantity|price|emoji|unit
)
