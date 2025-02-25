package com.example.farmeraid.farm

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cesarferreira.pluralize.pluralize
import com.example.farmeraid.data.InventoryRepository
import com.example.farmeraid.data.TransactionRepository
import com.example.farmeraid.data.UserRepository
import com.example.farmeraid.data.model.InventoryModel
import com.example.farmeraid.data.model.ResponseModel
import com.example.farmeraid.data.model.TransactionModel
import com.example.farmeraid.farm.model.FarmModel
import com.example.farmeraid.farm.model.FarmModel.FarmViewState
import com.example.farmeraid.farm.model.getMicButton
import com.example.farmeraid.farm.model.getStopButton
import com.example.farmeraid.farm.model.getSubmitButton
import com.example.farmeraid.navigation.AppNavigator
import com.example.farmeraid.snackbar.SnackbarDelegate
import com.example.farmeraid.speech_recognition.KontinuousSpeechRecognizer
import com.example.farmeraid.speech_recognition.SpeechRecognizerUtility
import com.example.farmeraid.uicomponents.models.UiComponentModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FarmViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val transactionRepository: TransactionRepository,
    private val speechRecognizerUtility: SpeechRecognizerUtility,
    private val kontinuousSpeechRecognizer: KontinuousSpeechRecognizer,
    private val appNavigator: AppNavigator,
    private val snackbarDelegate: SnackbarDelegate,
    ): ViewModel() {
    private val _state = MutableStateFlow(FarmViewState(
        micFabUiState = getMicButton(),
        micFabUiEvent = getMicButtonEvent(),
        submitButtonUiState = getSubmitButton(),
        speechResult = ""
    ))
    val state: StateFlow<FarmViewState>
        get() = _state

    private val harvestList : MutableStateFlow<List<FarmModel.ProduceHarvest>> = MutableStateFlow(listOf())
    private val submitButtonUiState: MutableStateFlow<UiComponentModel.ButtonUiState> = MutableStateFlow(_state.value.submitButtonUiState)
    private val micFabUiState: MutableStateFlow<UiComponentModel.FabUiState> = MutableStateFlow(_state.value.micFabUiState)
    private val micFabUiEvent: MutableStateFlow<UiComponentModel.FabUiEvent> = MutableStateFlow(_state.value.micFabUiEvent)
    private val speechRes: MutableStateFlow<String> = MutableStateFlow("")
    private val isLoading : MutableStateFlow<Boolean> = MutableStateFlow(_state.value.isLoading)

    inline fun <T1, T2, T3, T4, T5, T6, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R
    ): Flow<R> {
        return kotlinx.coroutines.flow.combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
            @Suppress("UNCHECKED_CAST")
            transform(
                args[0] as T1,
                args[1] as T2,
                args[2] as T3,
                args[3] as T4,
                args[4] as T5,
                args[5] as T6,
            )
        }
    }
    init {
        viewModelScope.launch {
            combine(harvestList, submitButtonUiState, micFabUiState, micFabUiEvent, speechRes, isLoading) {
                    harvestList: List<FarmModel.ProduceHarvest>,
                    submitButtonUiState: UiComponentModel.ButtonUiState,
                    micFabUiState: UiComponentModel.FabUiState,
                    micFabUiEvent: UiComponentModel.FabUiEvent,
                    speechRes: String,
                    isLoading:Boolean ->
                Log.d("Result", "triggered")
                FarmViewState(
                    micFabUiState = micFabUiState,
                    micFabUiEvent = micFabUiEvent,
                    submitButtonUiState = submitButtonUiState,
                    produceHarvestList = harvestList,
                    speechResult = speechRes,
                    isLoading = isLoading
                )
            }.collect {
                _state.value = it

            }
        }

    }

    init{
        viewModelScope.launch {
            speechRecognizerUtility.speechRecognizerResult.collect {speechText ->
                harvestList.value = updateCount(speechText)
            }
//            kontinuousSpeechRecognizer.speechRecognizerResult.collect {speechText ->
//                harvestList.value = parseSpeech(speechText)
//            }
        }
    }

    init{
        viewModelScope.launch {
                inventoryRepository.getInventory().collect{ produce ->
                    produce.data?.let {
                        harvestList.value = it.map {(produceName, _) ->
                            FarmModel.ProduceHarvest(
                                produceName = produceName,
                                produceCount = 0,
                            )
                        }
                    } ?: run {
                        snackbarDelegate.showSnackbar(produce.error ?: "Unknown error")
                    }
                }
        }
    }

    fun startListening(){
        if(speechRecognizerUtility.isPermissionGranted()){
            micFabUiState.value = getStopButton()
            micFabUiEvent.value = getStopButtonEvent()
        }
        speechRecognizerUtility.startSpeechRecognition()
//        if(kontinuousSpeechRecognizer.isPermissionGranted()){
//            micFabUiState.value = getStopButton()
//            micFabUiEvent.value = getStopButtonEvent()
//        }
//        kontinuousSpeechRecognizer.startRecognition()
    }

    fun stopListening(){
        micFabUiState.value = getMicButton()
        micFabUiEvent.value = getMicButtonEvent()
        speechRecognizerUtility.stopSpeechRecognition()
        //kontinuousSpeechRecognizer.stopRecognition()
    }

    private fun updateCount(speechResult: String): List<FarmModel.ProduceHarvest>{

        speechRes.value = speechResult


        return harvestList.value.map { produce ->
            val addCount = parseSpeech(speechResult,"(add|at)( )*([0-9]+)( )*([a-z]+)(( )*([0-9]+)( )*([a-z]+))*(( )?(and)( )*([0-9]+)( )*([a-z]+)( )*)*", produce)

            val removeCount = parseSpeech(speechResult,"(remove|take away)( )*([0-9]+)( )*([a-z]+)(( )*([0-9]+)( )*([a-z]+))*(( )?(and)( )*([0-9]+)( )*([a-z]+)( )*)*", produce)

            var total = 0
            if((produce.produceCount?:0) == 0 && (addCount-removeCount) < 0){
                snackbarDelegate.showSnackbar(message = "Do not have any ${produce.produceName} to remove!")
            } else if((produce.produceCount?:0) + (addCount-removeCount) < 0){
            snackbarDelegate.showSnackbar(message = "Do not have that many ${produce.produceName} to remove!")
            } else{
                total = addCount-removeCount
            }

            FarmModel.ProduceHarvest(produceName = produce.produceName, produceCount = (produce.produceCount?:0) + total)
        }

    }

    private fun parseSpeech(speechResult:String, regexPattern: String, produce: FarmModel.ProduceHarvest): Int{
        var reAdd = Regex(regexPattern)
        var addMatches = reAdd.findAll(speechResult.lowercase())
        var count = 0
        //Get every add phrase in our speech Result
        addMatches.forEach { addM ->
            val addCommand:String = addM.value
            val prod = produce.produceName.lowercase()
            var reAddAmount = Regex("([0-9]+)( )*(${prod}|${prod.pluralize()})")
            var amountMatches = reAddAmount.findAll(addCommand)
            //Get every instance of the current produce being incremented in the add phrase
            amountMatches.forEach { amountM ->
                val quantity = amountM.groupValues[1].toIntOrNull()
                if (quantity != null){
                    count += quantity
                }
            }

        }
        return count
    }

    private fun getMicButtonEvent(): UiComponentModel.FabUiEvent {
        return UiComponentModel.FabUiEvent(onClick = {startListening()})
    }

    private fun getStopButtonEvent(): UiComponentModel.FabUiEvent {
        return UiComponentModel.FabUiEvent(onClick = {stopListening()})
    }

    fun incrementProduceCount(produceName : String) {
        harvestList.value = harvestList.value.map { produceHarvest ->
            FarmModel.ProduceHarvest(
                produceName = produceHarvest.produceName,
                produceCount = (produceHarvest.produceCount?:0) + if (produceName == produceHarvest.produceName) 1 else 0
            )
        }
    }

    fun decrementProduceCount(produceName : String) {
        harvestList.value = harvestList.value.map { produceHarvest ->
            FarmModel.ProduceHarvest(
                produceName = produceHarvest.produceName,
                produceCount = (produceHarvest.produceCount?:1) - if (produceName == produceHarvest.produceName) 1 else 0
            )
        }
    }

    fun setProduceCount(produceName : String, count : Int?) {
        harvestList.value = harvestList.value.map { produceHarvest ->
            FarmModel.ProduceHarvest(
                produceName = produceHarvest.produceName,
                produceCount = if (produceName == produceHarvest.produceName) count else produceHarvest.produceCount
            )
        }
    }

    fun submitHarvest() {
        viewModelScope.launch {
            submitButtonUiState.value = submitButtonUiState.value.copy(isLoading = true)
            // error check
            if (harvestList.value.any { it.produceCount == null }) {
                snackbarDelegate.showSnackbar("There are one or more invalid values")
            } else {

                val harvestMap: Map<String, Int> = harvestList.value.associate {
                    it.produceName to (it.produceCount ?: 0)
                }

                inventoryRepository.harvest(harvestMap)
                harvestMap.forEach { (produceName, produceAmount) ->
                    if (produceAmount > 0) {
                        when(val res = transactionRepository.addTransaction(
                            TransactionModel.Transaction(
                                transactionType = TransactionModel.TransactionType.HARVEST,
                                produce = InventoryModel.Produce(produceName, produceAmount),
                                pricePerProduce = 0.0,
                                location = "",
                            )
                        )) {
                            is ResponseModel.FAResponse.Error -> {
                                snackbarDelegate.showSnackbar(res.error ?: "Unknown error")
                            }
                            else -> {}
                        }
                    }
                }

                harvestList.value = harvestList.value.map {(produceName, _) ->
                    FarmModel.ProduceHarvest(
                        produceName = produceName,
                        produceCount = 0,
                    )
                }
            }

            submitButtonUiState.value = submitButtonUiState.value.copy(isLoading = false)
        }
    }

    fun navigateToTransactions() {
        appNavigator.navigateToTransactions(TransactionModel.TransactionType.HARVEST.stringValue)
    }
}