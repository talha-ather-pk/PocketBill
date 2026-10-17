package com.example.pocketbill.pdf

/** Something placed on a page, with its top edge in points. */
sealed interface Placed {
    val top: Float

    /** The items table's header row; repeated on every page that has items. */
    data class TableHeader(override val top: Float) : Placed

    data class Row(val index: Int, override val top: Float) : Placed

    /** A block after the table (totals, payment details, notes); never split across pages. */
    data class Tail(val index: Int, override val top: Float) : Placed
}

/**
 * Splits the items table and the blocks after it into pages (MVP_SPEC 6): the table continues
 * across pages with its header row repeated, and no tail block is split. Pure, so it is unit
 * tested; the renderer measures heights and draws what this returns.
 *
 * A row or block taller than a whole page still goes on its own page (and is cut off there)
 * rather than looping forever.
 */
object PdfPagination {
    fun paginate(
        firstPageTop: Float,
        otherPagesTop: Float,
        bottom: Float,
        headerHeight: Float,
        rowHeights: List<Float>,
        tailHeights: List<Float>,
        tailGap: Float,
    ): List<List<Placed>> {
        val pages = mutableListOf<MutableList<Placed>>(mutableListOf())
        var y = firstPageTop
        // A continuation page with nothing on it yet: moving on would not give more room.
        fun fresh() = pages.size > 1 && pages.last().isEmpty()
        fun newPage() {
            pages += mutableListOf<Placed>()
            y = otherPagesTop
        }

        var headerOnPage = false
        rowHeights.forEachIndexed { index, height ->
            val needed = height + if (headerOnPage) 0f else headerHeight
            if (y + needed > bottom && !fresh()) {
                newPage()
                headerOnPage = false
            }
            if (!headerOnPage) {
                pages.last() += Placed.TableHeader(y)
                y += headerHeight
                headerOnPage = true
            }
            pages.last() += Placed.Row(index, y)
            y += height
        }
        tailHeights.forEachIndexed { index, height ->
            if (y + tailGap + height > bottom && !fresh()) newPage()
            if (pages.last().isNotEmpty() || pages.size == 1) y += tailGap
            pages.last() += Placed.Tail(index, y)
            y += height
        }
        return pages
    }
}
