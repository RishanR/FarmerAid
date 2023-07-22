package com.example.farmeraid.data

import com.example.farmeraid.data.model.MarketModel
import com.example.farmeraid.data.model.ResponseModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class MarketRepository(
    private val quotasRepository: QuotasRepository,
    private val farmRepository: FarmRepository,
    private val userRepository: UserRepository,
) {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun addOrUpdateMarket(marketName: String, producePrices: Map<String, Double>) : ResponseModel.FAResponse {

        val querySnapshot = db.collection("market").whereEqualTo("name", marketName).get().await()

        if (querySnapshot.isEmpty) {
            return try {
                val docRef = db.collection("market").add(
                    mapOf (
                        "name" to marketName,
                        "prices" to producePrices,
                    )
                ).await()

                userRepository.getUserId()?.let{
                    db.collection("farm").document(userRepository.getFarmId()!!).update(
                        "markets", FieldValue.arrayUnion(docRef.id)
                    )
                }

                ResponseModel.FAResponse.Success
            } catch(e: Exception) {
                return ResponseModel.FAResponse.Error(e.message?:"Error creating a market. Please try again.")
            }
        } else {
            return try {
                db.collection("market").document(querySnapshot.documents[0].id).update(
                    "prices", producePrices
                )

                ResponseModel.FAResponse.Success
            } catch(e: Exception) {
                return ResponseModel.FAResponse.Error(e.message?:"Error updating the market. Please try again.")
            }
        }
    }
    private suspend fun readMarketData(): ResponseModel.FAResponseWithData<List<MarketModel.Market>> {
        val marketIds = farmRepository.getMarketIds()
        if (marketIds.error != null) {
            return ResponseModel.FAResponseWithData.Error(marketIds.error)
        }

        val marketModel: MutableList<DocumentSnapshot> = mutableListOf()
        marketIds.data?.forEach {
            try {
                marketModel.add(db.collection("market").document(it).get().await())
            } catch (e: Exception) {
                return ResponseModel.FAResponseWithData.Error(
                    e.message ?: "Unknown error while fetching market"
                )
            }
        }

        val marketModelList = mutableListOf<MarketModel.Market>()

        for (market in marketModel) {
            MarketModel.Market(
                id = market.id,
                name = market.get("name") as String,
                prices = market.get("prices") as MutableMap<String, Double>
            )
                .let { marketModelList.add(it) }
        }

        marketModelList.sortBy { it.name.lowercase() }
        return ResponseModel.FAResponseWithData.Success(marketModelList)
    }

    private suspend fun readMarketDataWithQuotas(): ResponseModel.FAResponseWithData<List<MarketModel.MarketWithQuota>> {
        val marketIds = farmRepository.getMarketIds()
        if (marketIds.error != null) {
            return ResponseModel.FAResponseWithData.Error(marketIds.error)
        }

        val marketModel : MutableList<DocumentSnapshot> = mutableListOf()
        marketIds.data?.forEach {
            try {
                marketModel.add(db.collection("market").document(it).get().await())
            } catch (e : Exception) {
                return ResponseModel.FAResponseWithData.Error(e.message ?: "Unknown error while fetching market")
            }
        }

        val marketModelList = mutableListOf<MarketModel.MarketWithQuota>()

        for (market in marketModel) {
            quotasRepository.getQuota(market.id).let { quotaResponse ->
                quotaResponse.data?.let {
                    marketModelList.add(
                        MarketModel.MarketWithQuota(
                            id = market.id,
                            name = market.get("name") as String,
                            quota = it,
                            prices = market.get("prices") as MutableMap<String, Double>
                        )
                    )
                } ?: run {
                    if (quotaResponse.error != "Quota does not exist") {
                        return ResponseModel.FAResponseWithData.Error(quotaResponse.error ?: "Unknown error while getting quotas")
                    }
                }

            }
        }
        marketModelList.sortBy { it.name.lowercase() }
        return ResponseModel.FAResponseWithData.Success(marketModelList)
    }

    suspend fun getMarkets(): Flow<ResponseModel.FAResponseWithData<List<MarketModel.Market>>> {
        return flow {
            emit(readMarketData())
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getMarketsWithQuota() : Flow<ResponseModel.FAResponseWithData<List<MarketModel.MarketWithQuota>>> {
        return flow {
            emit(
                readMarketDataWithQuotas()
            )
        }
    }

    suspend fun getMarketWithQuota(id : String) : ResponseModel.FAResponseWithData<MarketModel.MarketWithQuota> {

        val marketModel : DocumentSnapshot = try {
                db.collection("market").document(id).get().await()
            } catch (e : Exception) {
                return ResponseModel.FAResponseWithData.Error(e.message ?: "Unknown error while fetching market")
            }

        if (!marketModel.exists()) {
            return ResponseModel.FAResponseWithData.Error("Market does not exist")
        }

        val marketWithQuota = quotasRepository.getQuota(marketModel.id).let { quotaResponse ->
                quotaResponse.data?.let {
                    MarketModel.MarketWithQuota(
                        id = marketModel.id,
                        name = marketModel.get("name") as String,
                        quota = it,
                        prices = marketModel.get("prices") as MutableMap<String, Double>
                    )
                } ?: run {
                    return ResponseModel.FAResponseWithData.Error(quotaResponse.error ?: "Unknown error while fetching market's quota")
                }
            }

        return ResponseModel.FAResponseWithData.Success(marketWithQuota)
    }

    suspend fun getMarket(id : String) : ResponseModel.FAResponseWithData<MarketModel.Market> {

        val marketModel: DocumentSnapshot = try {
            db.collection("market").document(id).get().await()
        } catch (e : Exception) {
            return ResponseModel.FAResponseWithData.Error(e.message ?: "Unknown error while fetching market")
        }

        if (!marketModel.exists()) {
            return ResponseModel.FAResponseWithData.Error("Market does not exist")
        }

        val market: MarketModel.Market = marketModel.let { marketModel ->
            MarketModel.Market(
                id = marketModel.id,
                name = marketModel.get("name") as String,
                prices = marketModel.get("prices") as MutableMap<String, Double>,
            )
        }

        return ResponseModel.FAResponseWithData.Success(market)
    }
}