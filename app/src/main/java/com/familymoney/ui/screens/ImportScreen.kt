package com.familymoney.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.bank.CsvImporter
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.TxType
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

private enum class ImportMode { FILE, PASTE }

/** Section 12 fallback that works today: import a bank or card statement. */
@Composable
fun ImportScreen(
    vm: MainViewModel,
    container: com.familymoney.AppContainer,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by vm.accounts.collectAsState()
    val cards by vm.cards.collectAsState()
    val profile by vm.profile.collectAsState()

    var report by remember { mutableStateOf<CsvImporter.Report?>(null) }
    var prepared by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<String?>(null) }
    var cardId by remember { mutableStateOf<String?>(null) }
    var defaultType by remember { mutableStateOf(TxType.EXPENSE) }
    var mode by remember { mutableStateOf(ImportMode.FILE) }
    var pastedText by remember { mutableStateOf("") }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var pdfPages by remember { mutableStateOf(0) }

    fun process(uri: Uri) {
        loading = true
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        // Israeli exports are usually UTF-8, occasionally Windows-1255.
                        val utf8 = String(bytes, Charsets.UTF_8)
                        if (utf8.contains('�')) {
                            runCatching { String(bytes, Charset.forName("windows-1255")) }
                                .getOrDefault(utf8)
                        } else utf8
                    }
                }.getOrNull()
            }

            if (text == null) {
                report = CsvImporter.Report(emptyList(), 0, emptyMap(), "לא ניתן לקרוא את הקובץ.")
                loading = false
                return@launch
            }

            val parsed = CsvImporter.parse(text, defaultType)
            report = parsed
            prepared = CsvImporter.toTransactions(
                rows = parsed.rows,
                familyId = profile?.familyId.orEmpty(),
                createdBy = profile?.name.orEmpty(),
                accountId = accountId,
                cardId = cardId,
                categorize = { merchant -> vm.suggestCategory(merchant) }
            )
            loading = false
        }
    }

    /** Shared tail for both import paths: categorise and build entities. */
    suspend fun buildPreview(rows: List<CsvImporter.Row>) {
        prepared = CsvImporter.toTransactions(
            rows = rows,
            familyId = profile?.familyId.orEmpty(),
            createdBy = profile?.name.orEmpty(),
            accountId = accountId,
            cardId = cardId,
            categorize = { merchant -> vm.suggestCategory(merchant) }
        )
    }

    fun parsePastedText() {
        aiBusy = true
        aiError = null
        scope.launch {
            when (val r = container.statementParser.parse(pastedText)) {
                is com.familymoney.data.ai.StatementParser.Result.Error -> {
                    aiError = r.message
                    report = null
                    prepared = emptyList()
                }
                is com.familymoney.data.ai.StatementParser.Result.Ok -> {
                    report = CsvImporter.Report(
                        rows = r.rows,
                        skipped = r.skipped,
                        detectedColumns = mapOf("מקור" to "טקסט שהודבק (AI)")
                    )
                    buildPreview(r.rows)
                }
            }
            aiBusy = false
        }
    }

    fun processPdf(uri: Uri) {
        aiBusy = true
        aiError = null
        scope.launch {
            when (val ex = com.familymoney.data.bank.PdfTextExtractor.extract(context, uri)) {
                is com.familymoney.data.bank.PdfTextExtractor.Result.Error -> {
                    aiError = ex.message
                    aiBusy = false
                }
                is com.familymoney.data.bank.PdfTextExtractor.Result.Ok -> {
                    pastedText = ex.text
                    pdfPages = ex.pages
                    when (val r = container.statementParser.parse(ex.text)) {
                        is com.familymoney.data.ai.StatementParser.Result.Error -> {
                            aiError = r.message
                            report = null
                            prepared = emptyList()
                        }
                        is com.familymoney.data.ai.StatementParser.Result.Ok -> {
                            report = CsvImporter.Report(
                                rows = r.rows,
                                skipped = r.skipped,
                                detectedColumns = mapOf("מקור" to "קובץ PDF (${ex.pages} עמודים)")
                            )
                            buildPreview(r.rows)
                        }
                    }
                    aiBusy = false
                }
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { pdfName = it.lastPathSegment.orEmpty(); processPdf(it) } }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { fileName = it.lastPathSegment.orEmpty(); process(it) } }

    DetailScaffold("📥 ייבוא דף חשבון", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        "📊 קובץ CSV", MaterialTheme.colorScheme.primary,
                        selected = mode == ImportMode.FILE
                    ) { mode = ImportMode.FILE; report = null; prepared = emptyList(); aiError = null }
                    Pill(
                        "📄 קובץ PDF", MaterialTheme.colorScheme.secondary,
                        selected = mode == ImportMode.PASTE
                    ) { mode = ImportMode.PASTE; report = null; prepared = emptyList(); aiError = null }
                }
            }

            if (mode == ImportMode.PASTE) {
                item {
                    SectionCard {
                        Text("ייבוא מ־PDF", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "העלו את דף החשבון שהורדתם מהבנק. האפליקציה קוראת את " +
                                "הטקסט מהקובץ וה־AI מחלץ ממנו את העסקאות.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                pdfPicker.launch(arrayOf("application/pdf"))
                            },
                            enabled = !aiBusy,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (aiBusy) {
                                CircularProgressIndicator(
                                    Modifier.width(18.dp).height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("קורא וממיין…")
                            } else {
                                Text("📄 בחר קובץ PDF")
                            }
                        }

                        if (pdfName.isNotBlank() && !aiBusy) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "נקרא: $pdfName" +
                                    if (pdfPages > 0) " · $pdfPages עמודים" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        aiError?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            "הטקסט משמש לחילוץ העסקאות בלבד. אין צורך להעלות מסמכים " +
                                "שאינם דף חשבון.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    SectionCard {
                        Text(
                            "או הדביקו טקסט ידנית",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "שימושי אם ה־PDF מוגן או סרוק.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            label = { Text("תוכן דף החשבון") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { parsePastedText() },
                            enabled = pastedText.trim().length >= 20 && !aiBusy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("✨ חלץ מהטקסט") }
                    }
                }
            }

            if (mode == ImportMode.FILE) item {
                SectionCard {
                    Text("איך מייבאים", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "היכנסו לאתר הבנק או חברת האשראי.",
                        "הורידו את פירוט התנועות כקובץ CSV או Excel (שמרו כ־CSV).",
                        "בחרו את הקובץ כאן — הכותרות מזוהות אוטומטית.",
                        "בדקו את התצוגה המקדימה ואשרו."
                    ).forEachIndexed { i, step ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}. ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Text("שיוך העסקאות", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    Text("סוג ברירת מחדל", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            "דף אשראי (הוצאות)", MaterialTheme.colorScheme.error,
                            selected = defaultType == TxType.EXPENSE
                        ) { defaultType = TxType.EXPENSE }
                        Pill(
                            "דף בנק (מעורב)", MaterialTheme.colorScheme.primary,
                            selected = defaultType == TxType.INCOME
                        ) { defaultType = TxType.INCOME }
                    }

                    if (accounts.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("חשבון", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accounts) { a ->
                                Pill(
                                    a.name, MaterialTheme.colorScheme.primary,
                                    selected = accountId == a.id
                                ) {
                                    accountId = if (accountId == a.id) null else a.id
                                    if (accountId != null) cardId = null
                                }
                            }
                        }
                    }

                    if (cards.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("כרטיס", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cards) { c ->
                                Pill(
                                    "${c.provider} ••${c.last4}",
                                    MaterialTheme.colorScheme.secondary,
                                    selected = cardId == c.id
                                ) {
                                    cardId = if (cardId == c.id) null else c.id
                                    if (cardId != null) accountId = null
                                }
                            }
                        }
                    }
                }
            }

            if (mode == ImportMode.FILE) item {
                Button(
                    onClick = {
                        picker.launch(
                            arrayOf(
                                "text/csv", "text/comma-separated-values", "text/plain",
                                "application/vnd.ms-excel", "application/octet-stream", "*/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("בחירת קובץ CSV") }
            }

            if (loading) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("מנתח את הקובץ…")
                    }
                }
            }

            report?.let { r ->
                if (r.error != null) {
                    item {
                        SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                "לא הצלחנו לקרוא את הקובץ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                r.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "ודאו שהקובץ מכיל עמודת תאריך ועמודת סכום (או חובה/זכות).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    item { SectionHeader("תצוגה מקדימה", emoji = "👀") }
                    item {
                        SectionCard {
                            AmountRow("עסקאות שזוהו", "${r.rows.size}")
                            if (r.skipped > 0) AmountRow("שורות שדולגו", "${r.skipped}")
                            val income = r.rows.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                            val expense = r.rows.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                            if (income > 0) AmountRow("סך הכנסות", Money.format(income), Success)
                            if (expense > 0) {
                                AmountRow("סך הוצאות", Money.format(expense),
                                    MaterialTheme.colorScheme.error)
                            }
                            if (r.detectedColumns.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "עמודות שזוהו: " +
                                        r.detectedColumns.entries.joinToString(", ") {
                                            "${it.key}→${it.value}"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(prepared.take(12)) { tx ->
                        SectionCard(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(tx.merchant, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${ExpenseCategory.fromKey(tx.category).he} · " +
                                            Dates.formatDay(tx.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    (if (tx.type == TxType.INCOME) "+" else "−") +
                                        Money.format(tx.amount),
                                    style = MoneySmall,
                                    color = if (tx.type == TxType.INCOME) Success
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (prepared.size > 12) {
                        item {
                            Text(
                                "ועוד ${prepared.size - 12} עסקאות…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    vm.importTransactions(prepared)
                                    navController.popBackStack()
                                },
                                enabled = prepared.isNotEmpty(),
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("ייבא ${prepared.size} עסקאות") }
                            Button(
                                onClick = { report = null; prepared = emptyList() },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) { Text("ביטול") }
                        }
                    }
                }
            }
        }
    }
}
