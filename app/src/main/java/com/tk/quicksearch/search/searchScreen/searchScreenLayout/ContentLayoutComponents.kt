package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.search.files.FileTypeFilterChips
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.RemindersSectionParams
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.R

/**
 * Render each home section as soon as its data is ready without an extra enter animation.
 * Staggered fade/expand transitions add work during startup and make the screen look as if it
 * is repeatedly rebuilding while asynchronous sources populate.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun HomeLoadingAnimatedContent(
    animationKey: String,
    enabled: Boolean,
    appearedKeys: MutableSet<String>,
    expandIn: Boolean = false,
    content: @Composable () -> Unit,
) {
    content()
}

internal const val HOME_TODAY_CALENDAR_ANIMATION_KEY = "home-today-calendar"
/** Gap between home content items; flat dividers sit half the stack spacing into it. */
internal val HomeSectionContentSpacing = 14.dp

internal fun hasMoreResults(
    renderingState: SectionRenderingState,
    sectionContext: SectionRenderContext,
): Boolean =
    (sectionContext.shouldRenderApps && renderingState.displayApps.isNotEmpty()) ||
        (sectionContext.shouldRenderAppShortcuts && sectionContext.appShortcutsList.isNotEmpty()) ||
        (sectionContext.shouldRenderContacts && sectionContext.contactsList.isNotEmpty()) ||
        (sectionContext.shouldRenderFiles && sectionContext.filesList.isNotEmpty()) ||
        (sectionContext.shouldRenderSettings && sectionContext.settingsList.isNotEmpty()) ||
        (sectionContext.shouldRenderAppSettings && sectionContext.appSettingsList.isNotEmpty()) ||
        (sectionContext.shouldRenderCalendar && sectionContext.calendarEventsList.isNotEmpty()) ||
        (sectionContext.shouldRenderNotes && sectionContext.notesList.isNotEmpty()) ||
        (sectionContext.shouldRenderReminders && sectionContext.remindersList.isNotEmpty()) ||
        (sectionContext.shouldRenderBookmarks && sectionContext.bookmarksList.isNotEmpty())

internal fun SearchSection.supportsPinnedHomeCollapse(): Boolean =
    when (this) {
        SearchSection.APPS, SearchSection.APP_SETTINGS, SearchSection.BOOKMARKS -> false
        SearchSection.APP_SHORTCUTS,
        SearchSection.CONTACTS,
        SearchSection.FILES,
        SearchSection.SETTINGS,
        SearchSection.CALENDAR,
        SearchSection.REMINDERS,
        SearchSection.NOTES,
        -> true
    }

@Composable
internal fun UnifiedPinnedItemsBlock(
    userPreferences: UserAppPreferences,
    showWallpaperBackground: Boolean,
    content: @Composable () -> Unit,
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(userPreferences.isUnifiedPinnedItemsExpanded())
    }
    val interactionSource = remember { MutableInteractionSource() }
    val toggleExpanded = {
        val newExpanded = !isExpanded
        isExpanded = newExpanded
        userPreferences.setUnifiedPinnedItemsExpanded(newExpanded)
    }

    val compactHeader = !LocalSearchCardLayoutEnabled.current
    val headerContent: @Composable (Modifier) -> Unit = { modifier ->
        HomeSectionHeaderRow(
            title = stringResource(R.string.app_suggestions_tab_pinned),
            leadingIcon = null,
            isExpanded = isExpanded,
            showWallpaperBackground = showWallpaperBackground,
            modifier = modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = toggleExpanded,
            ),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isExpanded || compactHeader) {
            headerContent(Modifier)
        } else {
            SearchResultCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                showWallpaperBackground = showWallpaperBackground,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    headerContent(
                        Modifier.padding(horizontal = collapsedPinnedHeaderInset()),
                    )
                }
            }
        }

        HomeSectionCollapsibleContent(visible = isExpanded, content = content)
    }
}

// A selected file type chip keeps the list (and its chips) up even when nothing matches.
internal fun shouldShowAliasRecentItems(
    state: SearchUiState,
    isSectionAliasMode: Boolean,
    hasQuery: Boolean,
): Boolean =
    isSectionAliasMode &&
        !hasQuery &&
        (state.aliasRecentItems.isNotEmpty() || state.aliasFileTypeFilter != null)

internal val ALIAS_RECENT_ELIGIBLE_SECTIONS =
    setOf(
        SearchSection.APP_SHORTCUTS,
        SearchSection.FILES,
        SearchSection.CONTACTS,
        SearchSection.SETTINGS,
        SearchSection.APP_SETTINGS,
        SearchSection.NOTES,
    )

internal fun ItemPriorityConfig.ItemType.toSearchSectionOrNull(): SearchSection? =
    SearchSectionRegistry.sectionForItemType(this)

internal fun sectionAliasPermissionMessageRes(
    state: SearchUiState,
    section: SearchSection,
    isSectionAliasMode: Boolean,
): Int? {
    if (!isSectionAliasMode) return null
    return when (section) {
        SearchSection.CONTACTS ->
            if (state.contactsSectionState is ContactsSectionVisibility.NoPermission) {
                R.string.contacts_section_permission_subtitle
            } else {
                null
            }
        SearchSection.FILES ->
            if (state.filesSectionState is FilesSectionVisibility.NoPermission) {
                R.string.files_section_permission_subtitle
            } else {
                null
            }
        SearchSection.CALENDAR ->
            if (state.calendarSectionState is CalendarSectionVisibility.NoPermission) {
                R.string.calendar_section_permission_subtitle
            } else {
                null
            }
        else -> null
    }
}

@Composable
internal fun AliasRecentItemsSection(
    state: SearchUiState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    notesParams: NotesSectionParams,
    remindersParams: RemindersSectionParams? = null,
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    expandedCardMaxHeight: Dp,
    showWallpaperBackground: Boolean,
    isOverlayPresentation: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (state.detectedAliasSearchSection == SearchSection.FILES) {
            FileTypeFilterChips(
                selectedFileType = state.aliasFileTypeFilter,
                onFileTypeSelected = filesParams.onAliasFileTypeFilterChange,
                showWallpaperBackground = showWallpaperBackground,
                // Chips carry invisible touch padding; match the visible gap above the row.
                modifier = Modifier.padding(bottom = DesignTokens.SpacingSmall),
            )
            if (state.aliasRecentItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.widget_custom_buttons_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (showWallpaperBackground) {
                            AppColors.WallpaperTextSecondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.padding(horizontal = DesignTokens.SpacingMedium),
                )
            }
        }
        AliasRecentItemsList(
            state = state,
            contactsParams = contactsParams,
            filesParams = filesParams,
            settingsParams = settingsParams,
            appShortcutsParams = appShortcutsParams,
            notesParams = notesParams,
            onRecentQueryClick = onRecentQueryClick,
            onDeleteRecentItem = onDeleteRecentItem,
            expandedCardMaxHeight = expandedCardMaxHeight,
            showWallpaperBackground = showWallpaperBackground,
            isOverlayPresentation = isOverlayPresentation,
        )
    }
}

@Composable
private fun AliasRecentItemsList(
    state: SearchUiState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    notesParams: NotesSectionParams,
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    expandedCardMaxHeight: Dp,
    showWallpaperBackground: Boolean,
    isOverlayPresentation: Boolean,
) {
    SearchHistorySection(
        items = state.aliasRecentItems,
        primaryContactButton = contactsParams.primaryContactButton ?: ContactButtonAction.CALL,
        secondaryContactButton = contactsParams.secondaryContactButton ?: ContactButtonAction.SMS,
        onRecentQueryClick = onRecentQueryClick,
        onContactClick = contactsParams.onContactClick,
        onShowContactMethods = contactsParams.onShowContactMethods,
        onCallContact = contactsParams.onCallContact,
        onSmsContact = contactsParams.onSmsContact,
        onContactMethodClick = contactsParams.onContactMethodClick,
        getPrimaryContactCardAction = contactsParams.getPrimaryContactCardAction,
        getSecondaryContactCardAction = contactsParams.getSecondaryContactCardAction,
        onPrimaryActionLongPress = contactsParams.onPrimaryActionLongPress,
        onSecondaryActionLongPress = contactsParams.onSecondaryActionLongPress,
        onCustomAction = contactsParams.onCustomAction,
        onFileClick = filesParams.onFileClick,
        onSettingClick = settingsParams.onSettingClick,
        onAppShortcutClick = appShortcutsParams.onShortcutClick,
        onNoteClick = notesParams.onNoteClick,
        onAppSettingClick = settingsParams.onAppSettingClick,
        onAppSettingToggle = settingsParams.onAppSettingToggle,
        isAppSettingToggleChecked = settingsParams.isAppSettingToggleChecked,
        appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
        onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
        appSettingAppResultRowCount = settingsParams.appSettingAppResultRowCount,
        onAppSettingAppResultRowCountChange = settingsParams.onAppSettingAppResultRowCountChange,
        onDeleteRecentItem = onDeleteRecentItem,
        showInlineCollapseButton = false,
        expandedCardMaxHeight = expandedCardMaxHeight,
        showWallpaperBackground = showWallpaperBackground,
        isOverlayPresentation = isOverlayPresentation,
        alwaysExpanded = true,
        // Alias items are all opened results, so pin the list to that tab without the tab row.
        selectedTab = SearchHistoryTab.RECENTLY_OPENED,
        showTabs = false,
        removableEntryKeys = state.aliasRecentRemovableKeys,
        modifier = Modifier.fillMaxWidth(),
    )
}
