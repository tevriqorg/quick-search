package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import kotlinx.coroutines.delay

private const val TOP_MATCH_SETTLE_DEADLINE_MS = 110L
private const val REGULAR_SEARCH_SETTLE_DEADLINE_MS = 120L

internal class SettledSearchResultsBuffer<T>(private val emptyValue: T) {
    private var settledQuery = ""
    private var settledValue = emptyValue

    fun displayedValue(
        query: String,
        currentValue: T,
        isSearchRefreshing: Boolean,
    ): T {
        if (query.isBlank()) {
            settledQuery = ""
            settledValue = emptyValue
        } else if (!isSearchRefreshing) {
            settledQuery = query
            settledValue = currentValue
        }

        val canReuseSettledValue = queriesAreOnSamePrefixPath(query, settledQuery)
        return if (isSearchRefreshing && !canReuseSettledValue) emptyValue else settledValue
    }
}

private fun queriesAreOnSamePrefixPath(
    first: String,
    second: String,
): Boolean =
    first.isNotBlank() &&
        second.isNotBlank() &&
        (first.startsWith(second, ignoreCase = true) || second.startsWith(first, ignoreCase = true))

internal data class SettledTopMatchesState(
    val matches: List<TopMatchItem> = emptyList(),
    val isReady: Boolean = false,
)

internal class StableTopMatchesBuffer {
    private var activeQuery = ""
    private var state = SettledTopMatchesState()

    fun displayedValue(
        query: String,
        currentMatches: List<TopMatchItem>,
        isSearchRefreshing: Boolean,
        deadlineReached: Boolean,
        limit: Int,
    ): SettledTopMatchesState {
        if (query.isBlank()) {
            activeQuery = ""
            state = SettledTopMatchesState()
            return state
        }
        if (query != activeQuery) {
            val canReuseSettledMatches = queriesAreOnSamePrefixPath(query, activeQuery)
            activeQuery = query
            state =
                SettledTopMatchesState(
                    matches = if (canReuseSettledMatches) state.matches else emptyList(),
                    isReady = false,
                )
        }

        if (!state.isReady && (!isSearchRefreshing || deadlineReached)) {
            state = SettledTopMatchesState(currentMatches.take(limit), isReady = true)
        } else if (state.isReady && !isSearchRefreshing) {
            // The deadline may be reached before slower sources (notably ContactsProvider) return.
            // Once every local search has settled, adopt the final ranked set so a late, stronger
            // match can replace an early result even when the deadline snapshot was already full.
            state = SettledTopMatchesState(currentMatches.take(limit), isReady = true)
        }
        return state
    }
}

@Composable
internal fun rememberSettledTopMatches(
    query: String,
    currentMatches: List<TopMatchItem>,
    isSearchRefreshing: Boolean,
    limit: Int,
): SettledTopMatchesState {
    var deadlineReached by remember(query) { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(TOP_MATCH_SETTLE_DEADLINE_MS)
            deadlineReached = true
        }
    }
    return remember { StableTopMatchesBuffer() }
        .displayedValue(
            query = query,
            currentMatches = currentMatches,
            isSearchRefreshing = isSearchRefreshing,
            deadlineReached = deadlineReached,
            limit = limit,
        )
}

internal fun TopMatchItem.stableKey(): String =
    when (this) {
        is TopMatchItem.App -> "app:${app.packageName}"
        is TopMatchItem.AppGrid -> "app-grid"
        is TopMatchItem.AppShortcut -> "shortcut:${shortcutKey(shortcut)}"
        is TopMatchItem.Contact -> "contact:${contact.contactId}"
        is TopMatchItem.File -> "file:${file.uri}"
        is TopMatchItem.Setting -> "setting:${setting.id}"
        is TopMatchItem.AppSetting -> "app-setting:${setting.id}"
        is TopMatchItem.Calendar -> "calendar:${event.eventId}"
        is TopMatchItem.Reminder -> "reminder:${reminder.reminderId}"
        is TopMatchItem.Bookmark -> "bookmark:${bookmark.bookmarkId}"
        is TopMatchItem.Note -> "note:${note.noteId}"
        is TopMatchItem.Other -> "other:$itemId"
    }

internal data class StableSearchResultsState<T>(
    val value: T,
    val isReady: Boolean = false,
)

/**
 * Keeps one coherent result snapshot on screen while a query is changing. A short deadline lets
 * fast sources appear together without waiting indefinitely for a slow provider; late results are
 * then committed once, when the full local search finishes.
 */
internal class StableSearchResultsBuffer<T>(private val emptyValue: T) {
    private var activeQuery = ""
    private var state = StableSearchResultsState(emptyValue)

    fun displayedValue(
        query: String,
        currentValue: T,
        isSearchRefreshing: Boolean,
        deadlineReached: Boolean,
    ): StableSearchResultsState<T> {
        if (query.isBlank()) {
            activeQuery = ""
            state = StableSearchResultsState(emptyValue)
            return StableSearchResultsState(currentValue, isReady = true)
        }

        if (query != activeQuery) {
            val canReuseSettledValue = queriesAreOnSamePrefixPath(query, activeQuery)
            activeQuery = query
            state =
                StableSearchResultsState(
                    value = if (canReuseSettledValue) state.value else emptyValue,
                    isReady = false,
                )
        }

        if (!state.isReady && (!isSearchRefreshing || deadlineReached)) {
            state = StableSearchResultsState(currentValue, isReady = true)
        } else if (state.isReady && !isSearchRefreshing) {
            // Publish the final combined snapshot once all local providers have completed.
            state = StableSearchResultsState(currentValue, isReady = true)
        }
        return state
    }
}

private data class RegularSearchResultsSnapshot(
    val apps: List<AppInfo> = emptyList(),
    val appShortcuts: List<StaticShortcut> = emptyList(),
    val contacts: List<ContactInfo> = emptyList(),
    val files: List<DeviceFile> = emptyList(),
    val settings: List<DeviceSetting> = emptyList(),
    val appSettings: List<AppSettingResult> = emptyList(),
    val calendarEvents: List<CalendarEventInfo> = emptyList(),
    val reminders: List<ReminderInfo> = emptyList(),
    val bookmarks: List<BookmarkInfo> = emptyList(),
    val notes: List<NoteInfo> = emptyList(),
)

@Composable
internal fun rememberSettledRegularSearchRenderingState(
    query: String,
    currentState: SectionRenderingState,
    isAppSearchRefreshing: Boolean,
    isSecondarySearchRefreshing: Boolean,
): SectionRenderingState {
    val currentSnapshot =
        RegularSearchResultsSnapshot(
            apps = currentState.displayApps,
            appShortcuts = currentState.appShortcutResults,
            contacts = currentState.contactResults,
            files = currentState.fileResults,
            settings = currentState.settingResults,
            appSettings = currentState.appSettingResults,
            calendarEvents = currentState.calendarEvents,
            reminders = currentState.reminderResults,
            bookmarks = currentState.bookmarkResults,
            notes = currentState.noteResults,
        )
    var settleDeadlineReached by remember(query) { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(REGULAR_SEARCH_SETTLE_DEADLINE_MS)
            settleDeadlineReached = true
        }
    }
    val displayedSnapshot =
        remember { StableSearchResultsBuffer(RegularSearchResultsSnapshot()) }
            .displayedValue(
                query = query,
                currentValue = currentSnapshot,
                isSearchRefreshing = isAppSearchRefreshing || isSecondarySearchRefreshing,
                deadlineReached = settleDeadlineReached,
            ).value

    if (query.isBlank()) return currentState

    return currentState.copy(
        hasAppResults = displayedSnapshot.apps.isNotEmpty(),
        hasAppShortcutResults = displayedSnapshot.appShortcuts.isNotEmpty(),
        hasContactResults = displayedSnapshot.contacts.isNotEmpty(),
        hasFileResults = displayedSnapshot.files.isNotEmpty(),
        hasSettingResults = displayedSnapshot.settings.isNotEmpty(),
        hasAppSettingResults = displayedSnapshot.appSettings.isNotEmpty(),
        hasCalendarResults = displayedSnapshot.calendarEvents.isNotEmpty(),
        hasReminderResults = displayedSnapshot.reminders.isNotEmpty(),
        hasBookmarkResults = displayedSnapshot.bookmarks.isNotEmpty(),
        hasNoteResults = displayedSnapshot.notes.isNotEmpty(),
        displayApps = displayedSnapshot.apps,
        appShortcutResults = displayedSnapshot.appShortcuts,
        contactResults = displayedSnapshot.contacts,
        fileResults = displayedSnapshot.files,
        settingResults = displayedSnapshot.settings,
        appSettingResults = displayedSnapshot.appSettings,
        calendarEvents = displayedSnapshot.calendarEvents,
        reminderResults = displayedSnapshot.reminders,
        bookmarkResults = displayedSnapshot.bookmarks,
        noteResults = displayedSnapshot.notes,
    )
}
