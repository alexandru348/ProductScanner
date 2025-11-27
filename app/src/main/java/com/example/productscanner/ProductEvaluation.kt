package com.example.productscanner

data class ProductEvaluation(
    val barcode: String,
    val productName: String,
    val healthScore: Int,
    val level: HealthLevel,
    val explanation: String,
    val compareHint: String? = null
)

        enum class HealthLevel {
            HEALTHY,
            MODERATE,
            UNHEALTHY
        }