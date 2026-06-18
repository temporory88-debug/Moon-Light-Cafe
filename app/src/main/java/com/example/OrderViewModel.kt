package com.example

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    // View roles: "Selection", "Customer", "Counter"
    val currentRole = MutableStateFlow("Selection")

    // For Customer Mode
    val selectedTable = MutableStateFlow("1")
    val selectedCategory = MutableStateFlow("bestseller")
    val searchKeyword = MutableStateFlow("")
    val cartItems = MutableStateFlow<Map<MenuItem, Int>>(emptyMap()) // MenuItem -> Qty
    val prepNote = MutableStateFlow("")
    val isSendingOrder = MutableStateFlow(false)
    val orderSuccessState = MutableStateFlow(false)

    // For Counter Mode
    private val _orders = MutableStateFlow<Map<String, FirebaseOrder>>(emptyMap())
    val orders: StateFlow<Map<String, FirebaseOrder>> = _orders.asStateFlow()

    val counterFilter = MutableStateFlow("all") // "all", "new", "preparing", "done"
    val voiceEnabled = MutableStateFlow(true)
    val isPolling = MutableStateFlow(false)

    // Active notifications/announcements
    private val _notifications = MutableSharedFlow<FirebaseOrder>()
    val notifications: SharedFlow<FirebaseOrder> = _notifications.asSharedFlow()

    private var pollJob: Job? = null
    private var knownOrderIds = mutableSetOf<String>()
    private var isFirstLoad = true

    init {
        startPolling()
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        isPolling.value = true
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    val freshOrders = RetrofitClient.firebaseService.getOrders() ?: emptyMap()
                    
                    // Check for new orders
                    if (!isFirstLoad) {
                        val newKeys = freshOrders.keys.filter { it !in knownOrderIds }
                        for (key in newKeys) {
                            val order = freshOrders[key]
                            if (order != null && order.status == "new") {
                                // Trigger notification
                                _notifications.emit(order)
                            }
                        }
                    } else {
                        isFirstLoad = false
                    }

                    knownOrderIds.clear()
                    knownOrderIds.addAll(freshOrders.keys)
                    
                    _orders.value = freshOrders
                } catch (e: Exception) {
                    Log.e("OrderViewModel", "Error polling orders: ${e.message}")
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        isPolling.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    // Customer Actions
    fun addToCart(item: MenuItem) {
        val current = cartItems.value.toMutableMap()
        val count = current[item] ?: 0
        current[item] = count + 1
        cartItems.value = current
    }

    fun removeFromCart(item: MenuItem) {
        val current = cartItems.value.toMutableMap()
        val count = current[item] ?: 0
        if (count > 1) {
            current[item] = count - 1
        } else {
            current.remove(item)
        }
        cartItems.value = current
    }

    fun clearCart() {
        cartItems.value = emptyMap()
        prepNote.value = ""
    }

    fun submitOrder() {
        if (cartItems.value.isEmpty()) return
        isSendingOrder.value = true
        
        viewModelScope.launch {
            try {
                val orderItemsList = cartItems.value.map { (item, qty) ->
                    OrderItem(
                        name = item.name,
                        qty = qty,
                        price = item.price
                    )
                }
                
                val totalPrice = cartItems.value.entries.sumOf { (item, qty) -> item.price * qty }
                
                val newOrder = FirebaseOrder(
                    tableNumber = selectedTable.value,
                    status = "new",
                    timestamp = System.currentTimeMillis(),
                    totalPrice = totalPrice,
                    note = prepNote.value.trim().takeIf { it.isNotEmpty() },
                    items = orderItemsList
                )
                
                // Submit via Retrofit Rest client
                RetrofitClient.firebaseService.createOrder(newOrder)
                
                // Success!
                orderSuccessState.value = true
                clearCart()
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Failed to submit order: ${e.message}")
            } finally {
                isSendingOrder.value = false
            }
        }
    }

    // Counter Actions
    fun updateStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.firebaseService.updateOrderStatus(orderId, StatusUpdate(newStatus))
                // Refresh immediately
                val freshOrders = RetrofitClient.firebaseService.getOrders() ?: emptyMap()
                _orders.value = freshOrders
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Failed to update status for $orderId: ${e.message}")
            }
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.firebaseService.deleteOrder(orderId)
                // Refresh immediately
                val freshOrders = RetrofitClient.firebaseService.getOrders() ?: emptyMap()
                _orders.value = freshOrders
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Failed to delete order $orderId: ${e.message}")
            }
        }
    }
}
