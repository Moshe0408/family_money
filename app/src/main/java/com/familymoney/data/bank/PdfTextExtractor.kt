package com.familymoney.data.bank

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls the text layer out of a bank statement PDF.
 *
 * Only the text layer — a scanned statement is an image and yields nothing,
 * which is reported rather than silently returning an empty result.
 */
object PdfTextExtractor {

    sealed interface Result {
        data class Ok(val text: String, val pages: Int) : Result
        data class Error(val message: String) : Result
    }

    /** Statements longer than this are almost certainly the wrong document. */
    private const val MAX_PAGES = 40

    suspend fun extract(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    return@withContext Result.Error("לא ניתן לפתוח את הקובץ.")
                }

                PDDocument.load(input).use { document ->
                    if (document.isEncrypted) {
                        return@withContext Result.Error(
                            "הקובץ מוגן בסיסמה. שמרו עותק ללא הגנה ונסו שוב."
                        )
                    }

                    val pages = document.numberOfPages
                    if (pages == 0) {
                        return@withContext Result.Error("הקובץ ריק.")
                    }

                    val stripper = PDFTextStripper().apply {
                        // Statements read top to bottom; position-based sorting
                        // keeps rows intact when columns are laid out oddly.
                        sortByPosition = true
                        startPage = 1
                        endPage = minOf(pages, MAX_PAGES)
                    }

                    val text = stripper.getText(document).trim()

                    when {
                        text.length < 40 -> Result.Error(
                            "לא נמצא טקסט בקובץ. ייתכן שזהו סריקה או צילום — " +
                                "הורידו את דף החשבון כ־PDF רגיל מאתר הבנק."
                        )
                        else -> Result.Ok(text, pages)
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.Error("הקובץ גדול מדי לעיבוד במכשיר.")
        } catch (e: Exception) {
            Result.Error("קריאת ה־PDF נכשלה: ${e.message ?: "קובץ לא תקין"}")
        }
    }
}
