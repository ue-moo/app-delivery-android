package com.github.uemoo.appdelivery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionClock.ContextClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        println("XXX ERROR! $throwable")
    }

    private val moleculeScope =
        CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    private val _count = MutableStateFlow("")
    private val _count2 = MutableStateFlow("")

    val uiState = moleculeScope.launchMolecule(clock = ContextClock) {
        CountPresenter(count1Flow = _count, count2Flow = _count2)
    }

    fun action() {
        viewModelScope.launch {
            repeat(Int.MAX_VALUE) {
                _count.value = it.toString()
                delay(500)
            }
        }
    }

    fun action2() {
        viewModelScope.launch(handler) {
            repeat(Int.MAX_VALUE) {
                _count2.value = it.toString()
                delay(1000)
                throw IllegalStateException()
            }
        }
    }
}

sealed interface UiState {
    object Loading : UiState
    data class Data(
        val count1: String,
        val count2: String,
    ) : UiState
}

@Composable
fun CountPresenter(
    count1Flow: Flow<String>,
    count2Flow: Flow<String>,
): UiState {
    val count1 by count1Flow.collectAsState("")
    val count2 by count2Flow.collectAsState("")

    return if (count1 == "" || count2 == "") {
        UiState.Loading
    } else {
        UiState.Data(
            count1 = count1,
            count2 = count2,
        )
    }
}
