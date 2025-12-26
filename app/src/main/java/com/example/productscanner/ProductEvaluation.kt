package com.example.productscanner

data class ProductEvaluation(
    var barcode: String = "",
    var productName: String = "",
    var healthScore: Int = 0,
    var level: HealthLevel = HealthLevel.MODERATE,
    var explanation: String = "",
    var compareHint: String? = null
)

enum class HealthLevel {
    HEALTHY,
    MODERATE,
    UNHEALTHY
}