/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import androidx.annotation.VisibleForTesting
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController

/**
 * [Middleware] that reacts to various [TabsTrayAction]s.
 *
 * @property metrics reference to the configured [MetricController] to record general page load events.
 */
class TabsTrayMiddleware(
    private val metrics: MetricController
) : Middleware<TabsTrayState, TabsTrayAction> {

    private var shouldReportInactiveTabMetrics: Boolean = true
    private var shouldReportSearchGroupMetrics: Boolean = true

    override fun invoke(
        context: MiddlewareContext<TabsTrayState, TabsTrayAction>,
        next: (TabsTrayAction) -> Unit,
        action: TabsTrayAction
    ) {
        next(action)

        when (action) {
            is TabsTrayAction.UpdateInactiveTabs -> {
                if (shouldReportInactiveTabMetrics) {
                    shouldReportInactiveTabMetrics = false
                    metrics.track(Event.InactiveTabsCountUpdate(action.tabs.size))
                    metrics.track(Event.TabsTrayHasInactiveTabs(action.tabs.size))
                }
            }
            is TabsTrayAction.UpdateSearchGroupTabs -> {
                if (shouldReportSearchGroupMetrics) {
                    shouldReportSearchGroupMetrics = false

                    metrics.track(Event.SearchTermGroupCount(action.groups.size))

                    if (action.groups.isNotEmpty()) {
                        val tabsPerGroup = action.groups.map { it.tabs.size }
                        val averageTabsPerGroup = tabsPerGroup.average()
                        metrics.track(Event.AverageTabsPerSearchTermGroup(averageTabsPerGroup))

                        val tabGroupSizeMapping = tabsPerGroup.map { generateTabGroupSizeMappedValue(it) }
                        metrics.track(Event.SearchTermGroupSizeDistribution(tabGroupSizeMapping))
                    }
                }
            }
        }
    }

    @Suppress("MagicNumber")
    @VisibleForTesting
    /**
     * This follows the logic outlined in metrics.yaml for "search_terms.group_size_distribution"
     */
    internal fun generateTabGroupSizeMappedValue(size: Int): Long =
        when (size) {
            2 -> 1L
            in 3..5 -> 2L
            in 6..10 -> 3L
            else -> 4L
        }
}
