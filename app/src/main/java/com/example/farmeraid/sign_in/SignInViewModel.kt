package com.example.farmeraid.sign_in

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmeraid.data.UserRepository
import com.example.farmeraid.navigation.AppNavigator
import com.example.farmeraid.navigation.NavRoute
import com.example.farmeraid.sign_in.model.SignInModel
import com.example.farmeraid.sign_in.model.getSignInButton
import com.example.farmeraid.snackbar.SnackbarDelegate
import com.example.farmeraid.uicomponents.models.UiComponentModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appNavigator: AppNavigator,
    private val snackbarDelegate: SnackbarDelegate,
) : ViewModel() {
    private val _state = MutableStateFlow(SignInModel.SignInViewState(
        buttonUiState = getSignInButton()
    ))
    val state: StateFlow<SignInModel.SignInViewState>
        get() = _state


    private val username: MutableStateFlow<String> = MutableStateFlow(_state.value.userName)
    private val password: MutableStateFlow<String> = MutableStateFlow(_state.value.passWord)
    private val buttonUiState: MutableStateFlow<UiComponentModel.ButtonUiState> = MutableStateFlow(_state.value.buttonUiState)



    init {
        viewModelScope.launch {
            combine(username, password, buttonUiState) {
                    userName: String, password: String, buttonUiState: UiComponentModel.ButtonUiState ->
                SignInModel.SignInViewState(
                    userName = userName,
                    passWord = password,
                    buttonUiState = buttonUiState,
                )
            }.collect {
                _state.value = it
            }
        }
    }

    fun setUsername(newVal: String) {
        username.value = newVal
    }

    fun setPassword(newVal: String) {
        password.value = newVal
    }

    fun login() = viewModelScope.launch {
        buttonUiState.value = buttonUiState.value.copy(isLoading = true)
        val result: SignInModel.AuthResponse = userRepository.login(username.value, password.value)
        buttonUiState.value = buttonUiState.value.copy(isLoading = false)

        when (result) {
            is SignInModel.AuthResponse.Success -> {
                Log.d("MESSAGE", "LOGGED IN")
                userRepository.getUserId()?.let { Log.d("UserID", it) }

                val farmId = userRepository.getFarmId().toString()
                Log.d("FARM ID", farmId)

                if (farmId == "none") {
                    appNavigator.navigateToFarmSelection()
                } else {
                    appNavigator.navigateToMode(NavRoute.Home)
                }
            }

            is SignInModel.AuthResponse.Error -> {
                Log.d("MESSAGE", result.error)
                snackbarDelegate.showSnackbar(
                    message = result.error
                )

            }
        }
    }

    fun moveToSignUp (){
        appNavigator.navigateToMode(NavRoute.SignUp)
    }

}