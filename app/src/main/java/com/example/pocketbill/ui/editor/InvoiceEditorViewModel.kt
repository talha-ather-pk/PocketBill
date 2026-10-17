package com.example.pocketbill.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceStore
import com.example.pocketbill.data.repository.PresetStore
import com.example.pocketbill.data.repository.ProfileStore
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.money.AmountParser
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.domain.money.ParseResult
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.ui.components.SaveStatus
import com.example.pocketbill.ui.navigation.NO_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the editor was opened (UX_SPEC 3 "Other ways into the editor"). */
enum class EditorMode {
    /** Nothing saved yet. Becomes DRAFT on the first autosave. */
    NEW,

    /** An autosaved draft. */
    DRAFT,

    /** An issued, unpaid invoice: no autosave, explicit "Save changes". */
    ISSUED,
}

/** Route arguments; [NO_ID] means "not given". */
data class EditorArgs(val invoiceId: Long = NO_ID, val duplicateOf: Long = NO_ID, val clientId: Long = NO_ID)

/** Saved-item suggestions shown under one line's description (F-18). */
data class Suggestions(val lineIndex: Int, val presets: List<ItemPresetEntity>)

sealed interface EditorPrompt {
    /** "This invoice totals €0.00. Issue it anyway?" (UX_SPEC 3). */
    data class ConfirmZeroTotal(val formattedTotal: String) : EditorPrompt

    /** Back with unsaved changes to an issued invoice: Save / Discard / Keep editing. */
    data object UnsavedChanges : EditorPrompt

    /** Delete this draft? */
    data object ConfirmDelete : EditorPrompt
}

/** Where the editor goes next. */
sealed interface EditorResult {
    /** Issued: open detail, then the PDF preview (UX_SPEC 1). */
    data class Issued(val invoiceId: Long, val number: String) : EditorResult

    /** An issued invoice was edited and saved. */
    data class SavedChanges(val invoiceId: Long) : EditorResult

    /** Left the editor; [draftSaved] when a draft exists ("Draft saved" message). */
    data class Closed(val draftSaved: Boolean) : EditorResult

    data object DraftDeleted : EditorResult

    /** Paid or void invoices are read-only (I-9). */
    data object NotEditable : EditorResult
}

data class EditorUiState(
    val loading: Boolean = true,
    val mode: EditorMode = EditorMode.NEW,
    val invoiceId: Long = NO_ID,
    val issuedNumber: String? = null,
    val lastSentAt: Long? = null,
    val form: EditorForm = EditorForm(),
    val totals: EditorTotals = EditorForm().totals(),
    val errors: List<EditorError> = emptyList(),
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val selectedClient: ClientEntity? = null,
    val recentClients: List<ClientEntity> = emptyList(),
    val allClients: List<ClientEntity> = emptyList(),
    val nextNumberPreview: String? = null,
    val suggestions: Suggestions? = null,
    val prompt: EditorPrompt? = null,
    val busy: Boolean = false,
    val result: EditorResult? = null,
)

/** Fields the user changed by hand: picking a client never overwrites these (D-015). */
private enum class Touched { CURRENCY, TERMS, TAX }

/** The invoice editor (UX_SPEC 3): fast creation (D-010), autosaved drafts (D-013), issuing. */
class InvoiceEditorViewModel(
    private val invoices: InvoiceStore,
    private val clients: ClientStore,
    private val profileStore: ProfileStore,
    private val presets: PresetStore,
    private val clock: Clock,
    private val args: EditorArgs,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var profile: BusinessProfileEntity? = null
    private val touched = mutableSetOf<Touched>()
    private var dirty = false
    private var autosaveJob: Job? = null
    private var showErrors = false

    init {
        viewModelScope.launch { clients.observeRecent().collect { list -> _state.update { it.copy(recentClients = list) } } }
        viewModelScope.launch { clients.observeActive().collect { list -> _state.update { it.copy(allClients = list) } } }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        profile = profileStore.get()
        val today = clock.todayEpochDay()
        val preview = invoices.previewNextNumber()
        when {
            args.invoiceId != NO_ID -> {
                val saved = invoices.get(args.invoiceId)
                when (saved?.invoice?.status) {
                    InvoiceStatus.DRAFT, InvoiceStatus.ISSUED -> {
                        val mode = if (saved.invoice.status == InvoiceStatus.DRAFT) EditorMode.DRAFT else EditorMode.ISSUED
                        touched += Touched.entries // a saved invoice's values are deliberate
                        setForm(saved.toForm(), mode = mode, invoiceId = args.invoiceId, preview = preview)
                        _state.update { it.copy(issuedNumber = saved.invoice.number, lastSentAt = saved.invoice.lastSentAt) }
                    }
                    else -> _state.update { it.copy(loading = false, result = EditorResult.NotEditable) }
                }
            }
            args.duplicateOf != NO_ID -> {
                val source = invoices.get(args.duplicateOf)
                if (source == null) {
                    setForm(newForm(today), EditorMode.NEW, NO_ID, preview)
                } else {
                    touched += Touched.entries
                    setForm(source.toDuplicateForm(today), EditorMode.NEW, NO_ID, preview)
                    markChanged() // a copy is a new draft straight away
                }
            }
            else -> {
                setForm(newForm(today), EditorMode.NEW, NO_ID, preview)
                if (args.clientId != NO_ID) applyClient(args.clientId, countsAsChange = false)
            }
        }
    }

    private fun newForm(today: Long) = EditorForm(
        currency = profile?.defaultCurrency ?: CurrencyCode.USD.name,
        issueDate = today,
        dueDays = profile?.defaultTermsDays ?: 14,
        taxRate = percentText(profile?.defaultTaxRateBp ?: 0),
    )

    private suspend fun setForm(form: EditorForm, mode: EditorMode, invoiceId: Long, preview: String?) {
        val client = form.clientId?.let { clients.get(it) }
        _state.update {
            it.copy(
                loading = false, mode = mode, invoiceId = invoiceId, form = form, totals = form.totals(),
                selectedClient = client, nextNumberPreview = preview,
            )
        }
    }

    // ---- Client ----------------------------------------------------------------------------------

    /** Picking a client fills currency, terms and tax (D-015), except fields changed by hand. */
    fun onClientPicked(clientId: Long) {
        viewModelScope.launch { applyClient(clientId, countsAsChange = true) }
    }

    private suspend fun applyClient(clientId: Long, countsAsChange: Boolean) {
        val client = clients.get(clientId) ?: return
        val defaults = resolveDefaults(client, invoices.lastInvoiceForClient(clientId), profile)
        _state.update { it.copy(selectedClient = client) }
        val apply: (EditorForm) -> EditorForm = { form ->
            var f = form.copy(clientId = clientId)
            if (Touched.CURRENCY !in touched) f = f.copy(currency = defaults.currency)
            if (Touched.TERMS !in touched) f = f.copy(dueDays = defaults.dueDays, customDueDate = null)
            if (Touched.TAX !in touched) f = f.copy(taxRate = percentText(defaults.taxBp))
            f
        }
        if (countsAsChange) change(apply) else _state.update { it.copy(form = apply(it.form), totals = apply(it.form).totals()) }
    }

    // ---- Field changes ---------------------------------------------------------------------------

    fun onCurrencyChange(code: String) {
        touched += Touched.CURRENCY
        change { it.copy(currency = code) }
    }

    fun onIssueDateChange(epochDay: Long) = change { it.copy(issueDate = epochDay) }

    fun onDueDaysChange(days: Int) {
        touched += Touched.TERMS
        change { it.copy(dueDays = days, customDueDate = null) }
    }

    fun onCustomDueDate(epochDay: Long) {
        touched += Touched.TERMS
        change { it.copy(dueDays = null, customDueDate = epochDay) }
    }

    fun onTaxRateChange(text: String) {
        touched += Touched.TAX
        change { it.copy(taxRate = text) }
    }

    fun onDiscountTypeChange(type: DiscountType?) = change { it.copy(discountType = type, discountValue = if (type == null) "" else it.discountValue) }

    fun onDiscountValueChange(text: String) = change { it.copy(discountValue = text) }

    fun onNotesChange(text: String) = change { it.copy(notes = text) }

    fun onLineChange(index: Int, update: (LineForm) -> LineForm) {
        val before = _state.value.form.lines.getOrNull(index) ?: return
        val after = update(before)
        change { form -> form.copy(lines = form.lines.mapIndexed { i, l -> if (i == index) after else l }) }
        if (after.description != before.description) loadSuggestions(index, after.description)
    }

    fun onAddLine() = change { it.copy(lines = it.lines + LineForm()) }

    fun onRemoveLine(index: Int) = change { form ->
        val remaining = form.lines.filterIndexed { i, _ -> i != index }
        form.copy(lines = remaining.ifEmpty { listOf(LineForm()) })
    }

    fun onMoveLine(index: Int, by: Int) = change { form ->
        val target = index + by
        if (target !in form.lines.indices) return@change form
        val lines = form.lines.toMutableList()
        lines.add(target, lines.removeAt(index))
        form.copy(lines = lines)
    }

    // ---- Saved items (F-18) --------------------------------------------------------------------

    private fun loadSuggestions(index: Int, description: String) {
        viewModelScope.launch {
            val found = if (description.trim().length >= 2) presets.suggestions(description) else emptyList()
            val useful = found.filterNot { it.description.equals(description.trim(), ignoreCase = true) }
            _state.update { it.copy(suggestions = if (useful.isEmpty()) null else Suggestions(index, useful)) }
        }
    }

    fun onSuggestionPicked(preset: ItemPresetEntity) {
        val index = _state.value.suggestions?.lineIndex ?: return
        val form = _state.value.form
        val currency = CurrencyCode.fromCode(form.currency)
        val priceText = if (preset.unitPriceMinor != null && preset.currency == form.currency && currency != null) {
            MoneyFormatter.formatPlain(preset.unitPriceMinor, currency)
        } else {
            null
        }
        _state.update { it.copy(suggestions = null) }
        change { f ->
            f.copy(lines = f.lines.mapIndexed { i, l -> if (i == index) l.copy(description = preset.description, price = priceText ?: l.price) else l })
        }
        viewModelScope.launch { presets.markUsed(preset.id) }
    }

    fun onDismissSuggestions() = _state.update { it.copy(suggestions = null) }

    fun onSaveLineAsPreset(index: Int) {
        val form = _state.value.form
        val line = form.lines.getOrNull(index) ?: return
        val currency = CurrencyCode.fromCode(form.currency) ?: return
        val price = (AmountParser.parseAmount(line.price, currency) as? ParseResult.Ok)?.value
        viewModelScope.launch { presets.saveFromLine(line.description, price, form.currency) }
    }

    // ---- Autosave (UX_SPEC 3, D-013) -------------------------------------------------------------

    private fun change(update: (EditorForm) -> EditorForm) {
        _state.update { s ->
            val form = update(s.form)
            val errors = if (showErrors) form.issueErrors() else emptyList()
            s.copy(form = form, totals = form.totals(), errors = errors)
        }
        markChanged()
    }

    /** Drafts save 500 ms after the last change; issued invoices wait for "Save changes". */
    private fun markChanged() {
        dirty = true
        if (_state.value.mode == EditorMode.ISSUED) return
        _state.update { it.copy(saveStatus = SaveStatus.SAVING) }
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveDraftNow()
        }
    }

    /** Saves a pending draft immediately: when the app goes to the background, and before leaving or issuing. */
    fun flush() {
        if (autosaveJob?.isActive != true) return
        autosaveJob?.cancel()
        viewModelScope.launch { saveDraftNow() }
    }

    private suspend fun saveDraftNow(): Long? {
        val s = _state.value
        if (s.mode == EditorMode.ISSUED || !dirty) return s.invoiceId.takeIf { it != NO_ID }
        return when (val outcome = invoices.saveDraft(s.invoiceId, s.form.toInput())) {
            is Outcome.Success -> {
                dirty = false
                _state.update { it.copy(invoiceId = outcome.value, mode = EditorMode.DRAFT, saveStatus = SaveStatus.SAVED) }
                outcome.value
            }
            else -> {
                _state.update { it.copy(saveStatus = SaveStatus.FAILED) }
                null
            }
        }
    }

    // ---- Issue, save, leave ------------------------------------------------------------------------

    /** Issue checks the rules first and shows every problem at its field (UX_SPEC 3 "Issuing"). */
    fun onIssue(confirmedZeroTotal: Boolean = false) {
        val s = _state.value
        if (s.busy || s.mode == EditorMode.ISSUED) return
        val errors = s.form.issueErrors()
        if (errors.isNotEmpty()) {
            showErrors = true
            _state.update { it.copy(errors = errors) }
            return
        }
        if (!confirmedZeroTotal && s.totals.totalMinor == 0L) {
            val currency = CurrencyCode.fromCode(s.form.currency) ?: CurrencyCode.USD
            _state.update { it.copy(prompt = EditorPrompt.ConfirmZeroTotal(MoneyFormatter.format(0, currency))) }
            return
        }
        _state.update { it.copy(busy = true, prompt = null) }
        autosaveJob?.cancel()
        viewModelScope.launch {
            dirty = true // make sure the latest form is saved before issuing
            val id = saveDraftNow()
            if (id == null) {
                _state.update { it.copy(busy = false) }
                return@launch
            }
            when (val outcome = invoices.issue(id)) {
                is Outcome.Success -> _state.update { it.copy(busy = false, result = EditorResult.Issued(id, outcome.value)) }
                is Outcome.ValidationFailed -> {
                    showErrors = true
                    _state.update { it.copy(busy = false, errors = outcome.reasons.map(::toEditorError).distinct()) }
                }
                else -> _state.update { it.copy(busy = false, saveStatus = SaveStatus.FAILED) }
            }
        }
    }

    fun onConfirmZeroTotal() = onIssue(confirmedZeroTotal = true)

    /** "Save changes" on an issued invoice (UX_SPEC 3): checked like issuing, keeps its number. */
    fun onSaveChanges() {
        val s = _state.value
        if (s.busy || s.mode != EditorMode.ISSUED) return
        val errors = s.form.issueErrors()
        if (errors.isNotEmpty()) {
            showErrors = true
            _state.update { it.copy(errors = errors, prompt = null) }
            return
        }
        _state.update { it.copy(busy = true, prompt = null) }
        viewModelScope.launch {
            when (val outcome = invoices.updateIssued(s.invoiceId, s.form.toInput())) {
                is Outcome.Success -> {
                    dirty = false
                    _state.update { it.copy(busy = false, result = EditorResult.SavedChanges(s.invoiceId)) }
                }
                is Outcome.ValidationFailed -> {
                    showErrors = true
                    _state.update { it.copy(busy = false, errors = outcome.reasons.map(::toEditorError).distinct()) }
                }
                else -> _state.update { it.copy(busy = false, saveStatus = SaveStatus.FAILED) }
            }
        }
    }

    /** Back: drafts are already saved (no dialog); edited issued invoices ask first (UX_SPEC 3). */
    fun onBack() {
        val s = _state.value
        if (s.mode == EditorMode.ISSUED) {
            if (dirty) _state.update { it.copy(prompt = EditorPrompt.UnsavedChanges) } else _state.update { it.copy(result = EditorResult.Closed(false)) }
            return
        }
        autosaveJob?.cancel()
        viewModelScope.launch {
            val id = if (dirty) saveDraftNow() else s.invoiceId.takeIf { it != NO_ID }
            _state.update { it.copy(result = EditorResult.Closed(draftSaved = id != null)) }
        }
    }

    fun onDiscardChanges() = _state.update { it.copy(prompt = null, result = EditorResult.Closed(false)) }

    fun onDeleteRequest() = _state.update { it.copy(prompt = EditorPrompt.ConfirmDelete) }

    fun onDeleteConfirm() {
        autosaveJob?.cancel()
        val id = _state.value.invoiceId
        _state.update { it.copy(prompt = null) }
        viewModelScope.launch {
            if (id != NO_ID) invoices.deleteDraft(id)
            dirty = false
            _state.update { it.copy(result = EditorResult.DraftDeleted) }
        }
    }

    fun onPromptDismiss() = _state.update { it.copy(prompt = null) }

    private fun toEditorError(problem: Problem): EditorError = when (problem) {
        Problem.NO_CLIENT -> EditorError.NoClient
        Problem.NO_LINES -> EditorError.NoLines
        Problem.LINE_WITHOUT_DESCRIPTION -> EditorError.LineDescription(_state.value.form.lines.indexOfFirst { it.description.isBlank() }.coerceAtLeast(0))
        Problem.DUE_BEFORE_ISSUE -> EditorError.DueBeforeIssue
        Problem.BAD_TAX_RATE -> EditorError.TaxRate
        Problem.BAD_DISCOUNT -> EditorError.DiscountValue
        else -> EditorError.TooLarge
    }

    companion object {
        /** UX_SPEC 3: drafts save 500 ms after the last change. */
        const val AUTOSAVE_DELAY_MS = 500L
    }
}
