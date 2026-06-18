package com.example

import com.squareup.moshi.JsonClass
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class OrderItem(
    val name: String = "",
    val qty: Int = 0,
    val price: Int = 0
)

@JsonClass(generateAdapter = true)
data class FirebaseOrder(
    val tableNumber: String = "",
    val status: String = "new",
    val timestamp: Long = 0,
    val totalPrice: Int = 0,
    val note: String? = null,
    val items: List<OrderItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FirebaseIdResponse(
    val name: String
)

@JsonClass(generateAdapter = true)
data class StatusUpdate(
    val status: String
)

interface FirebaseService {
    @GET("orders.json")
    suspend fun getOrders(): Map<String, FirebaseOrder>?

    @POST("orders.json")
    suspend fun createOrder(@Body order: FirebaseOrder): FirebaseIdResponse

    @PATCH("orders/{id}.json")
    suspend fun updateOrderStatus(@Path("id") id: String, @Body status: StatusUpdate): FirebaseOrder

    @DELETE("orders/{id}.json")
    suspend fun deleteOrder(@Path("id") id: String): Any?
}
