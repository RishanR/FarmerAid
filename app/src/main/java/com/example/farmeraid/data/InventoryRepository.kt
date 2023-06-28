package com.example.farmeraid.data

import com.example.farmeraid.home.model.HomeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class InventoryRepository {

    private val inventory: MutableMap<String, Int> = mutableMapOf<String, Int>().apply {
        put("Apples", 15)
        put("Bananas", 20)
        put("Oranges", 50)
        put("Strawberries", 30)
        put("Mangoes", 10)
    }

    fun getInventory(): Flow<MutableMap<String, Int>> {
        return flow {
            emit(inventory)
        }.flowOn(Dispatchers.IO)
    }

    fun addNewProduce(produceName: String, produceAmount: Int) {
        if (inventory.containsKey(produceName)) { }
        else {
            inventory[produceName] = produceAmount
        }
    }

    fun editProduce(produceName: String, produceAmount: Int) {
        if (!inventory.containsKey(produceName)) { }
        else inventory[produceName] = produceAmount
    }

    fun harvest(harvestChanges: MutableMap<String, Int> ) {
        for ((produceName, produceAmount) in harvestChanges.entries) {
            inventory[produceName] = inventory[produceName]!! + produceAmount
        }
    }
}