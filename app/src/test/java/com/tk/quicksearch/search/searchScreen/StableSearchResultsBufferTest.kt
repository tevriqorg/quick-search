package com.tk.quicksearch.search.searchScreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StableSearchResultsBufferTest {
    @Test
    fun firstQueryShowsOneCoherentSnapshotAtTheSettleDeadline() {
        val buffer = StableSearchResultsBuffer(emptyList<String>())
        val partialResults = listOf("app:Alpha")

        val beforeDeadline =
            buffer.displayedValue(
                query = "a",
                currentValue = partialResults,
                isSearchRefreshing = true,
                deadlineReached = false,
            )

        assertTrue(beforeDeadline.value.isEmpty())
        assertFalse(beforeDeadline.isReady)

        val atDeadline =
            buffer.displayedValue(
                query = "a",
                currentValue = partialResults,
                isSearchRefreshing = true,
                deadlineReached = true,
            )

        assertEquals(partialResults, atDeadline.value)
        assertTrue(atDeadline.isReady)
    }

    @Test
    fun prefixQueryKeepsPreviousSnapshotThenPublishesFinalResultsOnce() {
        val oldSnapshot = listOf("app:Teams", "contact:Teja")
        val firstPartialSnapshot = listOf("app:Telegram")
        val laterPartialSnapshot = listOf("app:Telegram", "file:Template")
        val finalSnapshot = listOf("app:Telegram", "file:Template", "contact:Test")
        val buffer = StableSearchResultsBuffer(emptyList<String>())

        assertEquals(
            oldSnapshot,
            buffer.displayedValue("t", oldSnapshot, isSearchRefreshing = false, deadlineReached = false).value,
        )

        val whileRefreshing =
            buffer.displayedValue(
                "te",
                firstPartialSnapshot,
                isSearchRefreshing = true,
                deadlineReached = false,
            )
        assertEquals(oldSnapshot, whileRefreshing.value)
        assertFalse(whileRefreshing.isReady)

        val afterDeadline =
            buffer.displayedValue(
                "te",
                firstPartialSnapshot,
                isSearchRefreshing = true,
                deadlineReached = true,
            )
        assertEquals(firstPartialSnapshot, afterDeadline.value)
        assertTrue(afterDeadline.isReady)

        val laterPartial =
            buffer.displayedValue(
                "te",
                laterPartialSnapshot,
                isSearchRefreshing = true,
                deadlineReached = true,
            )
        assertEquals(firstPartialSnapshot, laterPartial.value)

        val finished =
            buffer.displayedValue(
                "te",
                finalSnapshot,
                isSearchRefreshing = false,
                deadlineReached = true,
            )
        assertEquals(finalSnapshot, finished.value)
    }

    @Test
    fun unrelatedQueryDoesNotReusePreviousResults() {
        val buffer = StableSearchResultsBuffer(emptyList<String>())
        buffer.displayedValue("gm", listOf("Gmail"), isSearchRefreshing = false, deadlineReached = false)

        val state =
            buffer.displayedValue(
                "weather",
                listOf("Weather"),
                isSearchRefreshing = true,
                deadlineReached = false,
            )

        assertTrue(state.value.isEmpty())
        assertFalse(state.isReady)
    }

    @Test
    fun clearingQueryResetsThePreviousSnapshot() {
        val buffer = StableSearchResultsBuffer(emptyList<String>())
        buffer.displayedValue("t", listOf("Teams"), isSearchRefreshing = false, deadlineReached = false)

        val home = listOf("home:Suggestions")
        assertEquals(home, buffer.displayedValue("", home, isSearchRefreshing = false, deadlineReached = false).value)

        val nextQuery =
            buffer.displayedValue(
                "w",
                listOf("Weather"),
                isSearchRefreshing = true,
                deadlineReached = false,
            )
        assertTrue(nextQuery.value.isEmpty())
    }
}
