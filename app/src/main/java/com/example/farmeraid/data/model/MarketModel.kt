package com.example.farmeraid.data.model

class MarketModel {
    data class Market(
        val id : String,
        val name: String,
        val prices: MutableMap<String, Double>,
        val saleCount: Double,
    ) {
        override fun toString(): String = name
    }

    data class MarketWithQuota(
        val id : String,
        val name : String,
        val prices: MutableMap<String, Double>,
        val quota : QuotaModel.Quota,
        val saleCount: Double,
    ) {
        override fun toString(): String = name
    }
}