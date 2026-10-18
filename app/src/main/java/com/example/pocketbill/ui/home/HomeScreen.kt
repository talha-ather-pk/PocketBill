package com.example.pocketbill.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.invoices.InvoiceRowItem
import com.example.pocketbill.ui.navigation.InvoiceFilter
import com.example.pocketbill.ui.send.InvoiceSendFlow
import com.example.pocketbill.ui.send.SendMessage
import com.example.pocketbill.ui.send.SendRequestHandler
import com.example.pocketbill.ui.send.sendMessageText
import com.example.pocketbill.ui.theme.PocketBillTheme
import com.example.pocketbill.ui.theme.Spacing
import com.example.pocketbill.ui.theme.tabular
import kotlinx.coroutines.flow.map
import java.time.LocalTime

/** Up to this many currency lines before "+2 more currencies" (UX_SPEC 2). */
private const val MAX_CURRENCY_LINES = 3


/** Home (UX_SPEC 2): answers "who owes me, is anything overdue, what did I leave unfinished" without a tap. */
@Composable
fun HomeScreen(
    onCreateInvoice: () -> Unit,
    onOpenInvoice: (Long) -> Unit,
    onContinueDraft: (Long) -> Unit,
    onOpenInvoices: (InvoiceFilter) -> Unit,
    onBackUp: () -> Unit,
) {
    val c = appContainer()
    val vm: HomeViewModel = viewModel {
        HomeViewModel(
            c.invoices, c.clients, c.profile.observe().map { it?.name }, c.invoices, c.sharePreparer, c.clock,
            backupDue = c.backup::reminderDue, prefs = c.homePrefs,
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onShown() }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    vm.send.collectAsStateWithLifecycle().value?.let { flow -> SendEffects(flow, snackbar) }

    if (state.loading) return // the database answers in well under a second (UX_SPEC 2: no spinner)
    val summary = state.summary
    val intro = rememberPlayIntro()
    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = Spacing.l).animateContentSize(),
        ) {
            Column(Modifier.padding(horizontal = Spacing.screen).entrance(0, intro)) {
                Text(stringResource(greeting()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    state.businessName.ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (summary.noInvoices) {
                Welcome(onCreateInvoice, Modifier.entrance(1, intro))
                return@Column
            }
            OutstandingCard(summary, onOpenInvoices, countUp = intro, modifier = Modifier.entrance(1, intro))
            val press = remember { MutableInteractionSource() }
            Button(
                onClick = onCreateInvoice,
                interactionSource = press,
                modifier = Modifier.entrance(2, intro).padding(horizontal = Spacing.screen).fillMaxWidth().heightIn(min = Spacing.buttonHeight).pressScale(press),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text(stringResource(R.string.home_create_invoice), modifier = Modifier.padding(start = Spacing.s))
            }
            // At most one card (UX_SPEC 2): payment details first, then backup. Cards slide open and closed;
            // the last rows are kept while the payment card closes, so it doesn't empty before it shrinks.
            var lastResend by remember { mutableStateOf(summary.resend) }
            if (summary.resend.isNotEmpty()) lastResend = summary.resend
            Column(Modifier.entrance(3, intro)) {
                AnimatedVisibility(visible = summary.resend.isNotEmpty(), enter = cardEnter, exit = cardExit) {
                    PaymentChangedCard(lastResend, state.today, onOpen = onOpenInvoice, onSend = vm::onSendFromRow, onDismiss = vm::onDismissPaymentCard)
                }
                AnimatedVisibility(visible = summary.resend.isEmpty() && state.showBackupCard, enter = cardEnter, exit = cardExit) {
                    BackupCard(onBackUp = onBackUp, onDismiss = vm::onDismissBackupCard)
                }
            }
            if (summary.attention.isNotEmpty()) Column(Modifier.entrance(4, intro)) {
                SectionHeader(
                    stringResource(R.string.home_needs_attention),
                    action = if (summary.attentionTotal > summary.attention.size) {
                        stringResource(R.string.home_see_all_count, summary.attentionTotal) to { onOpenInvoices(summary.attentionFilter) }
                    } else {
                        null
                    },
                )
                summary.attention.forEach { item ->
                    key(item.kind, item.row.id) {
                        AttentionRow(item, state.today, onOpen = { onOpenInvoice(item.row.id) }, onAction = {
                            if (item.kind == AttentionKind.DRAFT) onContinueDraft(item.row.id) else vm.onSendFromRow(item)
                        })
                    }
                }
            }
            if (summary.recent.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.home_recent),
                    action = stringResource(R.string.home_see_all) to { onOpenInvoices(InvoiceFilter.ALL) },
                    modifier = Modifier.entrance(5, intro),
                )
                Column {
                    summary.recent.forEachIndexed { i, row ->
                        key(row.id) {
                            InvoiceRowItem(row, state.today, onClick = { onOpenInvoice(row.id) }, modifier = Modifier.entrance(6 + i, intro))
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar)
    }
}

/** Runs a send started from a Needs attention row: launches the app and shows "Sent" with Undo. */
@Composable
private fun SendEffects(flow: InvoiceSendFlow, snackbar: SnackbarHostState) {
    val sendState by flow.state.collectAsStateWithLifecycle()
    SendRequestHandler(sendState.request, flow)
    val message = sendState.message
    val text = message?.let { sendMessageText(it) }
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(message) {
        if (message == null || text == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(text, actionLabel = if (message is SendMessage.Sent) undoLabel else null, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) flow.onUndoSent() else flow.onMessageShown()
    }
}

@Composable
private fun Welcome(onCreateInvoice: () -> Unit, modifier: Modifier = Modifier) {
    // First use gets the same hero card as Outstanding, so the very first screen already looks like the app.
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.item), modifier = modifier.padding(horizontal = Spacing.screen)) {
        Card(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth().heroBackground().padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = onHero, modifier = Modifier.padding(bottom = Spacing.s))
                Text(stringResource(R.string.home_welcome_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = onHero)
                Text(stringResource(R.string.home_welcome_body), style = MaterialTheme.typography.bodyLarge, color = onHero.copy(alpha = 0.8f))
            }
        }
        val press = remember { MutableInteractionSource() }
        Button(
            onClick = onCreateInvoice,
            interactionSource = press,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight).pressScale(press),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Text(stringResource(R.string.home_create_invoice), modifier = Modifier.padding(start = Spacing.s))
        }
        Text(stringResource(R.string.home_welcome_tip), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Outstanding per currency, never added together, with the overdue row under it (UX_SPEC 2). */
@Composable
private fun OutstandingCard(summary: HomeSummary, onOpenInvoices: (InvoiceFilter) -> Unit, countUp: Boolean, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val lines = summary.outstanding
    val shown = if (expanded) lines else lines.take(MAX_CURRENCY_LINES)
    val amounts = lines.map { amount(it.currency, it.totalMinor) }
    val unpaidText = pluralStringResource(R.plurals.home_unpaid_count, summary.unpaidCount, summary.unpaidCount)
    val overdueText = pluralStringResource(R.plurals.home_overdue_count, summary.overdueCount, summary.overdueCount)
    val outstandingLabel = stringResource(R.string.home_outstanding)
    val nothing = stringResource(R.string.home_nothing_outstanding)
    // TalkBack reads the card as one sentence.
    val spoken = if (lines.isEmpty()) {
        "$outstandingLabel: $nothing."
    } else {
        "$outstandingLabel: ${amounts.joinToString(", ")}. $unpaidText." + if (summary.overdueCount > 0) " $overdueText." else ""
    }

    // The hero of Home: a soft two-colour gradient with two faint circles for depth.
    val colors = MaterialTheme.colorScheme
    val onHero = colors.onPrimaryContainer
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = modifier.padding(horizontal = Spacing.screen).fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .fillMaxWidth()
                .heroBackground()
                .clickable(enabled = lines.isNotEmpty()) { onOpenInvoices(InvoiceFilter.UNPAID) }
                .clearAndSetSemantics { contentDescription = spoken }
                .padding(horizontal = Spacing.xl, vertical = Spacing.l),
        ) {
            Text(outstandingLabel, style = MaterialTheme.typography.titleMedium, color = onHero.copy(alpha = 0.8f))
            if (lines.isEmpty()) {
                Text(nothing, style = MaterialTheme.typography.headlineSmall, color = onHero)
                if (summary.allPaid) {
                    Text(stringResource(R.string.home_all_paid), color = onHero.copy(alpha = 0.8f))
                }
            } else {
                shown.forEachIndexed { i, line ->
                    val minor = rememberCountUp(line.totalMinor, fromZero = countUp)
                    Text(
                        amount(line.currency, minor),
                        // The first currency is the headline figure; others are a step smaller.
                        style = (if (i == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall).tabular,
                        fontWeight = FontWeight.SemiBold,
                        color = onHero,
                    )
                }
                Text(unpaidText, style = MaterialTheme.typography.bodyMedium, color = onHero.copy(alpha = 0.8f))
            }
        }
        if (lines.size > MAX_CURRENCY_LINES) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(horizontal = Spacing.s)) {
                Text(
                    if (expanded) {
                        stringResource(R.string.home_show_less)
                    } else {
                        pluralStringResource(R.plurals.home_more_currencies, lines.size - MAX_CURRENCY_LINES, lines.size - MAX_CURRENCY_LINES)
                    },
                )
            }
        }
        if (summary.overdueCount > 0) {
            val late = PocketBillTheme.statusColors.overdue
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.touchTarget)
                    .clickable { onOpenInvoices(InvoiceFilter.OVERDUE) }
                    .padding(horizontal = Spacing.l, vertical = Spacing.s),
            ) {
                // "!" as well as colour, so colour is never the only signal (UX_SPEC 7.1).
                Text(
                    "! $overdueText · " + summary.overdueTotals.joinToString(" · ") { amount(it.currency, it.totalMinor) },
                    color = late.content,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge.tabular,
                    modifier = Modifier.weight(1f),
                )
                Text("›", color = late.content, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** UX_SPEC 2 card 1: unpaid invoices sent with the old payment details, each with Email again. */
@Composable
private fun PaymentChangedCard(items: List<AttentionItem>, today: Long, onOpen: (Long) -> Unit, onSend: (AttentionItem) -> Unit, onDismiss: () -> Unit) {
    OutlinedCard(Modifier.padding(horizontal = Spacing.screen).fillMaxWidth()) {
        Row(Modifier.padding(start = Spacing.l, top = Spacing.s, end = Spacing.xs)) {
            Column(Modifier.weight(1f).padding(top = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.home_payment_card_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.home_payment_card_body, items.size, items.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.home_payment_card_dismiss))
            }
        }
        items.forEach { item -> AttentionRow(item, today, onOpen = { onOpen(item.row.id) }, onAction = { onSend(item) }) }
    }
}

/** UX_SPEC 2 card: "Back up your invoices." with Back up now; ✕ hides it for 7 days. */
@Composable
private fun BackupCard(onBackUp: () -> Unit, onDismiss: () -> Unit) {
    OutlinedCard(Modifier.padding(horizontal = Spacing.screen).fillMaxWidth()) {
        Row(Modifier.padding(start = Spacing.l, top = Spacing.s, end = Spacing.xs)) {
            Column(Modifier.weight(1f).padding(top = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.home_backup_card_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.home_backup_card_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.home_backup_card_dismiss))
            }
        }
        TextButton(onClick = onBackUp, modifier = Modifier.padding(start = Spacing.s, bottom = Spacing.xs)) {
            Text(stringResource(R.string.home_backup_card_action))
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: Pair<String, () -> Unit>?, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.s, top = Spacing.m)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null) TextButton(onClick = action.second) { Text(action.first) }
    }
}

/** A Needs attention row: tap the row to open the invoice, or the button for the one-tap action. */
@Composable
private fun AttentionRow(item: AttentionItem, today: Long, onOpen: () -> Unit, onAction: () -> Unit) {
    val row = item.row
    val number = row.number ?: stringResource(R.string.status_draft)
    val detail = when (item.kind) {
        AttentionKind.OVERDUE ->
            pluralStringResource(R.plurals.home_overdue_days, item.daysOverdue.toInt(), item.daysOverdue.toInt()) + " · " + amount(row.currency, row.totalMinor)
        AttentionKind.UNSENT -> stringResource(R.string.home_not_sent)
        AttentionKind.RESEND -> amount(row.currency, row.totalMinor)
        AttentionKind.DRAFT -> {
            val day = row.updatedDay
            when (today - day) {
                0L -> stringResource(R.string.home_started_today)
                1L -> stringResource(R.string.home_started_yesterday)
                else -> stringResource(R.string.home_started_on, DateFormatter.inList(day, today))
            }
        }
    }
    val actionLabel = when {
        item.kind == AttentionKind.DRAFT -> R.string.home_continue
        !item.hasEmail -> R.string.home_send
        item.kind == AttentionKind.OVERDUE -> R.string.home_remind
        item.kind == AttentionKind.RESEND -> R.string.send_email_again
        else -> R.string.home_email
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.rowHeight).clickable(onClick = onOpen).padding(horizontal = Spacing.screen, vertical = Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.clientName.ifEmpty { stringResource(R.string.invoices_no_client) } + " · " + number,
                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.kind == AttentionKind.OVERDUE) PocketBillTheme.statusColors.overdue.content else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = Spacing.touchTarget)) { Text(stringResource(actionLabel)) }
    }
}

/** The hero look: a soft primary-to-tertiary gradient with two faint circles for depth. */
@Composable
private fun Modifier.heroBackground(): Modifier {
    val colors = MaterialTheme.colorScheme
    val onHero = colors.onPrimaryContainer
    val hero = Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))
    return background(hero).drawBehind {
        drawCircle(onHero.copy(alpha = 0.06f), radius = size.height * 0.9f, center = Offset(size.width * 0.95f, -size.height * 0.15f))
        drawCircle(onHero.copy(alpha = 0.05f), radius = size.height * 0.55f, center = Offset(size.width * 0.75f, size.height * 1.1f))
    }
}

/** Cards on Home open downwards and close upwards, fading as they go. */
private val cardEnter = expandVertically(expandFrom = Alignment.Top) + fadeIn()
private val cardExit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()

/** "Good morning" before noon, "Good afternoon" until 6 pm, then "Good evening" (phone's local time). */
private fun greeting(hour: Int = LocalTime.now().hour): Int = when (hour) {
    in 5..11 -> R.string.home_greeting_morning
    in 12..17 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
}

private fun amount(currency: String, minor: Long): String =
    CurrencyCode.fromCode(currency)?.let { MoneyFormatter.format(minor, it) } ?: "$currency ${minor}"
