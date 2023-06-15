package com.github.uemoo.appdelivery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.uemoo.appdelivery.ui.theme.AppDeliveryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppDeliveryTheme {
                val value by (flow {
                    repeat(Int.MAX_VALUE) {
                        emit(it.toString())
                        delay(500)
                    }
                }).collectAsStateWithLifecycle(initialValue = "")

                LaunchedEffect(value) {
                    println("XXX [$value] collectAsStateWithLifecycle")
                }

                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        println("XXX ${event.name}")
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                SideEffect {
                    println("XXX SideEffect")
                }

                val (check, setCheck) = remember { mutableStateOf(false) }
                val state = rememberSampleState(check = check)
                val state2 = rememberSampleStateWithLifecycle(check = check)

                LaunchedEffect(state.value) {
                    println("XXX [LIFECYCLE_NG] LaunchedEffect(${state.value})")
                }

                LaunchedEffect(state.value) {
                    println("XXX [LIFECYCLE] LaunchedEffect(${state2.value})")
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Button(onClick = { setCheck(!check) }) {
                        Text(text = state.value)
                    }

                    Button(onClick = { setCheck(!check) }) {
                        Text(text = state2.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSampleState(check: Boolean): State<String> = produceState(
    initialValue = "initial",
    key1 = check,
) {
    // Composable ではないのでインスタンスを作成してもパフォーマンスに問題はない
    repeat(Int.MAX_VALUE) {
        value = it.toString()
        delay(1000)
    }
}

@Composable
private fun rememberSampleStateWithLifecycle(check: Boolean): State<String> =
    produceStateWithLifecycleState(
        initialValue = "initial",
        key1 = check,
    ) {
        // Composable ではないのでインスタンスを作成してもパフォーマンスに問題はない
        repeat(Int.MAX_VALUE) {
            value = it.toString()
            delay(1000)
        }
    }

@Composable
private fun <T> produceStateWithLifecycleState(
    initialValue: T,
    key1: Any?,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
    producer: suspend ProduceStateScopeImpl2<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1, lifecycleOwner.lifecycle, minActiveState, context) {
        ProduceStateScopeImpl2(result, coroutineContext).producer(
            lifecycle = lifecycleOwner.lifecycle,
            minActiveState = minActiveState,
            producer = producer,
        )
    }
    return result
}

private class ProduceStateScopeImpl2<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext,
) : ProduceStateScope<T>, MutableState<T> by state {

    override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
        try {
            suspendCancellableCoroutine<Nothing> { }
        } finally {
            onDispose()
        }
    }

    suspend fun producer(
        lifecycle: Lifecycle,
        minActiveState: Lifecycle.State,
        producer: suspend ProduceStateScopeImpl2<T>.() -> Unit,
    ) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            if (coroutineContext == EmptyCoroutineContext) {
                producer()
            } else withContext(coroutineContext) {
                producer()
            }
        }
    }
}
