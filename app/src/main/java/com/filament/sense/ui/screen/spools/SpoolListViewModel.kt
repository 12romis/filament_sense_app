package com.filament.sense.ui.screen.spools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import com.filament.sense.domain.usecase.SetActiveSpoolUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpoolListUiState(
    val spools: List<SpoolSlot> = emptyList(),
)

@HiltViewModel
class SpoolListViewModel @Inject constructor(
    private val getSpools: GetSpoolsUseCase,
    private val setActiveSpool: SetActiveSpoolUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SpoolListUiState())
    val state: StateFlow<SpoolListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getSpools().collect { spools ->
                _state.value = SpoolListUiState(spools = spools)
            }
        }
    }

    fun onSetActive(index: Int) {
        viewModelScope.launch { setActiveSpool(index) }
    }
}