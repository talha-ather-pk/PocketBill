package com.example.pocketbill.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MVP_SPEC 6 pagination: the table continues with its header repeated; tail blocks never split. */
class PdfPaginationTest {

    private fun paginate(rows: List<Float>, tails: List<Float> = listOf(60f)) = PdfPagination.paginate(
        firstPageTop = 300f,
        otherPagesTop = 60f,
        bottom = 786f,
        headerHeight = 20f,
        rowHeights = rows,
        tailHeights = tails,
        tailGap = 18f,
    )

    @Test fun `short invoice fits on one page`() {
        val pages = paginate(List(5) { 22f })
        assertEquals(1, pages.size)
        assertEquals(1, pages[0].count { it is Placed.TableHeader })
        assertEquals(5, pages[0].count { it is Placed.Row })
    }

    @Test fun `fifty items continue across pages with the header repeated on each`() {
        val pages = paginate(List(50) { 22f })
        assertTrue(pages.size >= 2)
        pages.filter { p -> p.any { it is Placed.Row } }.forEach { page ->
            assertTrue("header must come first", page.first() is Placed.TableHeader)
            assertEquals(1, page.count { it is Placed.TableHeader })
        }
        val rows = pages.flatten().filterIsInstance<Placed.Row>().map { it.index }
        assertEquals((0 until 50).toList(), rows)
    }

    @Test fun `nothing goes below the bottom edge`() {
        val heights = List(80) { i -> 18f + (i % 7) * 9f }
        val tails = listOf(90f, 120f, 40f)
        paginate(heights, tails).forEach { page ->
            page.forEach { item ->
                val h = when (item) {
                    is Placed.TableHeader -> 20f
                    is Placed.Row -> heights[item.index]
                    is Placed.Tail -> tails[item.index]
                }
                assertTrue("$item overflows", item.top + h <= 786f)
            }
        }
    }

    @Test fun `totals that do not fit move whole to the next page`() {
        // 21 rows of 22 pt end at 300 + 20 + 462 = 782, so the 60 pt totals block moves on.
        val pages = paginate(List(21) { 22f })
        assertEquals(2, pages.size)
        assertEquals(listOf<Placed>(Placed.Tail(0, 60f)), pages[1])
    }

    @Test fun `no items still places the totals on page one`() {
        assertEquals(listOf<Placed>(Placed.Tail(0, 318f)), paginate(emptyList()).single())
    }

    @Test fun `a row taller than a page gets its own page instead of looping`() {
        val pages = paginate(listOf(22f, 2000f, 22f))
        val rowPages = pages.map { p -> p.filterIsInstance<Placed.Row>().map { it.index } }
        assertEquals(listOf(0), rowPages[0])
        assertEquals(listOf(1), rowPages[1])
        assertEquals(listOf(2), rowPages[2])
    }
}
