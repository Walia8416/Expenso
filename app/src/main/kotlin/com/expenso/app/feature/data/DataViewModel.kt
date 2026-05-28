package com.expenso.app.feature.data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.core.io.ExportService
import com.expenso.app.core.io.ImportKind
import com.expenso.app.core.io.ImportPreview
import com.expenso.app.core.io.ImportResult
import com.expenso.app.core.io.ImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataUiState(
    val busy: Boolean = false,
    val preview: ImportPreview? = null,
    val lastImport: ImportResult? = null,
    val error: String? = null,
)

sealed interface DataEvent {
    data class Exported(val uri: Uri, val kind: ImportKind, val rows: Int) : DataEvent
    data class PreviewReady(val preview: ImportPreview) : DataEvent
    data class ImportDone(val result: ImportResult) : DataEvent
    data class Error(val message: String) : DataEvent
}

@HiltViewModel
class DataViewModel @Inject constructor(
    private val exportService: ExportService,
    private val importService: ImportService,
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DataEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DataEvent> = _events.asSharedFlow()

    fun exportExpenses(uri: Uri) = runIo {
        val count = exportService.exportExpenses(uri)
        _events.emit(DataEvent.Exported(uri, ImportKind.EXPENSE, count))
    }

    fun exportIncome(uri: Uri) = runIo {
        val count = exportService.exportIncome(uri)
        _events.emit(DataEvent.Exported(uri, ImportKind.INCOME, count))
    }

    fun previewExpenses(uri: Uri) = runIo {
        val p = importService.previewExpenses(uri)
        _state.update { it.copy(preview = p) }
        _events.emit(DataEvent.PreviewReady(p))
    }

    fun previewIncome(uri: Uri) = runIo {
        val p = importService.previewIncome(uri)
        _state.update { it.copy(preview = p) }
        _events.emit(DataEvent.PreviewReady(p))
    }

    fun confirmImport() {
        val p = _state.value.preview ?: return
        runIo {
            val r = importService.commit(p)
            _state.update { it.copy(preview = null, lastImport = r) }
            _events.emit(DataEvent.ImportDone(r))
        }
    }

    fun dismissPreview() {
        _state.update { it.copy(preview = null) }
    }

    private inline fun runIo(crossinline block: suspend () -> Unit) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _state.update { s -> s.copy(busy = false) } }
                .onFailure { t ->
                    _state.update { s -> s.copy(busy = false, error = t.message ?: "Failed") }
                    _events.emit(DataEvent.Error(t.message ?: "Failed"))
                }
        }
    }
}
