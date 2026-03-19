package com.example.i230657

data class Notification(
    var id: String? = null,
    var fromUserId: String = "",
    var type: String = "",
    var status: String = "pending",
    var timestamp: Long = 0
)



