package com.example.farmeraid.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

// TODO: currently, we have mock demo functionality but need to modify to use firestore db after demo
// TODO: currently, we are lacking user permission checks for appropriate functions, need to add these

class InventoryRepository {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val inventory: MutableMap<String, Int> = mutableMapOf<String, Int>().apply {
        put("Apple", 15)
        put("Banana", 20)
        put("Orange", 50)
        put("Strawberry", 30)
        put("Mango", 10)
    }
    suspend fun getInventory(userId: String): Flow<MutableMap<String, Int>> {
        return flow {
            emit(readInventoryData(userId))
        }.flowOn(Dispatchers.IO)
    }
    private suspend fun readInventoryData(userId: String): MutableMap<String, Int> {
        val docRef = db.collection("inventory").document(userId)

        val map  = docRef.get().await().data?.get("produce")
        if (map != null) {
            return map as MutableMap<String, Int>
            }
        else{
            return mutableMapOf()
        }
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