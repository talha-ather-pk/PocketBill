package com.example.pocketbill.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.repository.ClientStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientListUiState(
    val loading: Boolean = true,
    val query: String = "",
    val sort: ClientSort = ClientSort.NAME,
    val rows: List<ClientRow> = emptyList(),
    val archivedRows: List<ClientRow> = emptyList(),
    val showArchived: Boolean = false,
    /** True when there are no clients at all (not just no search matches). */
    val noClients: Boolean = false,
)

/** Clients tab (UX_SPEC 6): search, A–Z or most owed, archived clients listed separately. */
class ClientListViewModel(private val store: ClientStore) : ViewModel() {
    private val _state = MutableStateFlow(ClientListUiState())
    val state: StateFlow<ClientListUiState> = _state.asStateFlow()

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(ClientSort.NAME)

    init {
        viewModelScope.launch {
            combine(
                store.observeActive(), store.observeArchived(), store.observeOwedByClient(),
                store.observeOverdueByClient(), combine(query, sort) { q, s -> q to s },
            ) { active, archived, owed, overdue, (q, s) ->
                _state.value.copy(
                    loading = false,
                    query = q,
                    sort = s,
                    rows = buildClientRows(active, owed, overdue, q, s),
                    archivedRows = buildClientRows(archived, owed, overdue, q, ClientSort.NAME),
                    noClients = active.isEmpty() && archived.isEmpty(),
                )
            }.collect { _state.value = it.copy(showArchived = _state.value.showArchived) }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        _state.update { it.copy(query = value) }
    }

    fun onSortChange(value: ClientSort) {
        sort.value = value
    }

    fun onToggleArchived() = _state.update { it.copy(showArchived = !it.showArchived) }
}
