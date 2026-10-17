package com.example.pocketbill.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.domain.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientDetailUiState(
    val loading: Boolean = true,
    /** Today as an epoch day, for overdue labels and dates in the invoice list. */
    val today: Long = 0,
    /** Null once loaded means the client no longer exists (e.g. just deleted). */
    val client: ClientEntity? = null,
    val owed: List<Owed> = emptyList(),
    val unpaidCount: Int = 0,
    val overdueCount: Int = 0,
    val invoices: List<InvoiceEntity> = emptyList(),
    val confirmDelete: Boolean = false,
    /** Set after a delete; the screen goes back. */
    val deleted: Boolean = false,
) {
    /** Clients with any invoice, drafts included, can only be archived (UX_SPEC 6). */
    val canDelete: Boolean get() = invoices.isEmpty()
}

/** Client detail (UX_SPEC 6): details, what they owe, their invoices, archive or delete. */
class ClientDetailViewModel(
    private val store: ClientStore,
    private val clientId: Long,
    today: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ClientDetailUiState(today = today))
    val state: StateFlow<ClientDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                store.observe(clientId),
                store.observeOwedByClient(),
                store.observeOverdueByClient(),
                store.observeInvoices(clientId),
            ) { client, owed, overdue, invoices ->
                val mine = owed.filter { it.clientId == clientId }
                _state.value.copy(
                    loading = false,
                    client = client,
                    owed = mine.sortedByDescending { it.totalMinor }.map { Owed(it.currency, it.totalMinor) },
                    unpaidCount = mine.sumOf { it.invoiceCount },
                    overdueCount = overdue.firstOrNull { it.clientId == clientId }?.count ?: 0,
                    invoices = invoices,
                )
            }.collect { new -> _state.update { new.copy(confirmDelete = it.confirmDelete, deleted = it.deleted) } }
        }
    }

    fun onArchive(archived: Boolean) {
        viewModelScope.launch { store.setArchived(clientId, archived) }
    }

    fun onDeleteRequest() = _state.update { it.copy(confirmDelete = true) }

    fun onDeleteDismiss() = _state.update { it.copy(confirmDelete = false) }

    fun onDeleteConfirm() {
        _state.update { it.copy(confirmDelete = false) }
        viewModelScope.launch {
            if (store.delete(clientId) is Outcome.Success) _state.update { it.copy(deleted = true) }
        }
    }
}
