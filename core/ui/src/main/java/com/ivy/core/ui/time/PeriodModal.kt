package com.ivy.core.ui.time

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.common.atEndOfDay
import com.ivy.common.dateNowLocal
import com.ivy.core.domain.pure.time.allTime
import com.ivy.core.ui.R
import com.ivy.core.ui.data.period.*
import com.ivy.core.ui.temp.rootScreen
import com.ivy.core.ui.time.handling.SelectPeriodEvent
import com.ivy.core.ui.time.handling.SelectedPeriodViewModel
import com.ivy.data.time.Period
import com.ivy.data.time.TimeUnit
import com.ivy.design.l0_system.UI
import com.ivy.design.l1_buildingBlocks.DividerH
import com.ivy.design.l1_buildingBlocks.SpacerHor
import com.ivy.design.l1_buildingBlocks.SpacerVer
import com.ivy.design.l2_components.B1
import com.ivy.design.l2_components.modal.IvyModal
import com.ivy.design.l2_components.modal.Modal
import com.ivy.design.l2_components.modal.components.Done
import com.ivy.design.l2_components.modal.components.Title
import com.ivy.design.l2_components.modal.rememberIvyModal
import com.ivy.design.l3_ivyComponents.button.ButtonFeeling
import com.ivy.design.l3_ivyComponents.button.ButtonSize
import com.ivy.design.l3_ivyComponents.button.ButtonVisibility
import com.ivy.design.l3_ivyComponents.button.IvyButton
import com.ivy.design.util.IvyPreview
import com.ivy.design.util.hiltViewmodelPreviewSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BoxScope.PeriodModal(
    modal: IvyModal,
    selectedPeriod: SelectedPeriodUi
) {
    val viewModel: SelectedPeriodViewModel? = hiltViewmodelPreviewSafe()
    val state = viewModel?.uiState?.collectAsState()?.value ?: previewState()
    UI(
        modal = modal,
        selectedPeriod = selectedPeriod,
        state = state,
        onEvent = { viewModel?.onEvent(it) },
    )
}

@Composable
private fun BoxScope.UI(
    modal: IvyModal,
    selectedPeriod: SelectedPeriodUi,
    state: SelectedPeriodViewModel.UiState,
    onEvent: (SelectPeriodEvent) -> Unit,
) {
    val moreOptionsModal = rememberIvyModal()
    Modal(
        modal = modal,
        actions = {
            Done {
                modal.hide()
            }
        }
    ) {
        ChooseMonth(months = state.months, selected = selectedPeriod, onEvent = onEvent)

        SpacerVer(height = 16.dp)
        DividerH(width = 1.dp, color = UI.colors.neutral)
        SpacerVer(height = 12.dp)

        FromToRange(selected = selectedPeriod, onEvent = onEvent)

        SpacerVer(height = 16.dp)
        DividerH(width = 1.dp, color = UI.colors.neutral)
        SpacerVer(height = 12.dp)

        MoreOptions(
            selected = selectedPeriod,
            onEvent = onEvent,
            onShowMoreOptionsModal = {
                moreOptionsModal.show()
            }
        )

        SpacerVer(height = 48.dp)
    }

    MoreOptionsModal(modal = moreOptionsModal, onEvent = onEvent)
}

// region Choose month
@Composable
private fun ChooseMonth(
    months: List<MonthUi>,
    selected: SelectedPeriodUi,
    onEvent: (SelectPeriodEvent) -> Unit,
) {
    SpacerVer(height = 16.dp)
    val selectedMonthly = selected is SelectedPeriodUi.Monthly
    SectionText(
        text = "Month",
        selected = selectedMonthly,
        modifier = Modifier.padding(start = 24.dp),
    )
    SpacerVer(height = 8.dp)

    val state = rememberLazyListState()
    val selectedMonth = (selected as? SelectedPeriodUi.Monthly)?.month
    var firstTimeScrolling by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (selectedMonth != null) {
            val selectedMonthIndex = withContext(Dispatchers.Default) {
                months.indexOf(selectedMonth).takeIf { it != -1 }
            }
            if (selectedMonthIndex != null) {
                if (firstTimeScrolling) {
                    state.scrollToItem(selectedMonthIndex)
                    firstTimeScrolling = false
                } else {
                    state.animateScrollToItem(selectedMonthIndex)
                }
            }
        }
    }

    LazyRow(
        state = state
    ) {
        item {
            SpacerHor(width = 8.dp)
        }
        items(months) { month ->
            MonthItem(month = month, selected = month == selectedMonth) {
                onEvent(SelectPeriodEvent.Monthly(month))
            }
            SpacerHor(width = 8.dp)
        }
    }
}

@Composable
private fun MonthItem(
    month: MonthUi,
    selected: Boolean,
    onClick: (MonthUi) -> Unit
) {
    IvyButton(
        size = ButtonSize.Small,
        visibility = if (selected) ButtonVisibility.High else ButtonVisibility.Medium,
        feeling = if (selected) ButtonFeeling.Positive else ButtonFeeling.Neutral,
        text = if (month.currentYear) month.fullName else "${month.fullName}, ${month.year}",
        icon = null
    ) {
        onClick(month)
    }
}
// endregion

// region From - To
@Composable
private fun FromToRange(
    selected: SelectedPeriodUi,
    onEvent: (SelectPeriodEvent) -> Unit,
) {
    val periodUi = selected.periodUi()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val selectedCustom = selected is SelectedPeriodUi.CustomRange ||
                selected is SelectedPeriodUi.InTheLast
        val rootScreen = rootScreen()
        PeriodClosureColumn(
            label = "From",
            dateText = periodUi.fromText,
            selectedCustom = selectedCustom,
        ) {
            rootScreen.datePicker(
                minDate = null,
                maxDate = periodUi.period.to.toLocalDate(),
                initialDate = periodUi.period.from.toLocalDate()
            ) { pickedDate ->
                onEvent(
                    SelectPeriodEvent.CustomRange(
                        period = Period.FromTo(
                            from = pickedDate.atStartOfDay(),
                            to = periodUi.period.to
                        )
                    )
                )
            }
        }
        SpacerHor(width = 16.dp)
        PeriodClosureColumn(
            label = "To",
            dateText = periodUi.toText,
            selectedCustom = selectedCustom,
        ) {
            rootScreen.datePicker(
                minDate = periodUi.period.from.toLocalDate(),
                maxDate = null,
                initialDate = periodUi.period.to.toLocalDate()
            ) { pickedDate ->
                onEvent(
                    SelectPeriodEvent.CustomRange(
                        period = Period.FromTo(
                            from = periodUi.period.from,
                            to = pickedDate.atEndOfDay()
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun RowScope.PeriodClosureColumn(
    selectedCustom: Boolean,
    label: String,
    dateText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f)
    ) {
        SectionText(
            text = label,
            selected = selectedCustom,
            modifier = Modifier.padding(start = 16.dp),
        )
        SpacerVer(height = 4.dp)
        DateButton(
            dateText = dateText,
            onClick = onClick,
        )
    }
}

@Composable
private fun DateButton(
    modifier: Modifier = Modifier,
    dateText: String,
    onClick: () -> Unit
) {
    IvyButton(
        modifier = modifier,
        size = ButtonSize.Big,
        visibility = ButtonVisibility.Medium,
        feeling = ButtonFeeling.Neutral,
        text = dateText,
        icon = R.drawable.ic_round_calendar_month_24,
        onClick = onClick
    )

}
// endregion

// region More Options
@Composable
private fun MoreOptions(
    selected: SelectedPeriodUi,
    onEvent: (SelectPeriodEvent) -> Unit,
    onShowMoreOptionsModal: () -> Unit,
) {
    val selectedMore = selected is SelectedPeriodUi.AllTime
    SectionText(
        text = "More options",
        selected = selectedMore,
        modifier = Modifier.padding(start = 24.dp),
    )
    SpacerVer(height = 8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IvyButton(
            modifier = Modifier.weight(1f),
            size = ButtonSize.Big,
            visibility = if (selected is SelectedPeriodUi.AllTime)
                ButtonVisibility.High else ButtonVisibility.Medium,
            feeling = ButtonFeeling.Positive,
            text = "All-time",
            icon = R.drawable.ic_baseline_all_inclusive_24
        ) {
            onEvent(SelectPeriodEvent.AllTime)
        }
        SpacerHor(width = 8.dp)
        IvyButton(
            modifier = Modifier.weight(1f),
            size = ButtonSize.Big,
            visibility = ButtonVisibility.Medium,
            feeling = ButtonFeeling.Negative,
            text = "Reset",
            icon = R.drawable.ic_round_undo_24
        ) {
            onEvent(SelectPeriodEvent.ResetToCurrentPeriod)
        }
    }
    SpacerVer(height = 8.dp)
    IvyButton(
        modifier = Modifier.padding(horizontal = 8.dp),
        size = ButtonSize.Big,
        visibility = ButtonVisibility.Low,
        feeling = ButtonFeeling.Neutral,
        text = "See more",
        icon = R.drawable.ic_round_expand_less_24,
        onClick = onShowMoreOptionsModal
    )
}
// endregion

// region More Options modal
@Composable
private fun BoxScope.MoreOptionsModal(
    modal: IvyModal,
    onEvent: (SelectPeriodEvent) -> Unit
) {
    Modal(modal = modal, actions = {}) {
        Title(text = "More Options")
        SpacerVer(height = 24.dp)
        val thisYear = remember { dateNowLocal().year.toString() }
        val lastYear = remember { (dateNowLocal().year - 1).toString() }
        LazyColumn {
            optionItem(text = thisYear) {
                onEvent(SelectPeriodEvent.ThisYear)
                modal.hide()
            }
            optionItem(text = lastYear) {
                onEvent(SelectPeriodEvent.LastYear)
                modal.hide()
            }
            optionItem(text = "Last 6 months") {
                onEvent(SelectPeriodEvent.InTheLast(n = 6, unit = TimeUnit.Month))
                modal.hide()
            }
            optionItem(text = "Last 3 months") {
                onEvent(SelectPeriodEvent.InTheLast(n = 3, unit = TimeUnit.Month))
                modal.hide()
            }
            optionItem(text = "Last 30 days") {
                onEvent(SelectPeriodEvent.InTheLast(n = 30, unit = TimeUnit.Day))
                modal.hide()
            }
            optionItem(text = "Last 15 days") {
                onEvent(SelectPeriodEvent.InTheLast(n = 15, unit = TimeUnit.Day))
                modal.hide()
            }
            optionItem(text = "Last 7 days") {
                onEvent(SelectPeriodEvent.InTheLast(n = 7, unit = TimeUnit.Day))
                modal.hide()
            }
            optionItem(text = "Last 3 days") {
                onEvent(SelectPeriodEvent.InTheLast(n = 3, unit = TimeUnit.Day))
                modal.hide()
            }
        }
        SpacerVer(height = 48.dp)
    }
}

private fun LazyListScope.optionItem(text: String, onClick: () -> Unit) {
    item {
        MoreOptionsButton(text = text, onClick = onClick)
        SpacerVer(height = 8.dp)
    }
}

@Composable
private fun MoreOptionsButton(
    text: String,
    onClick: () -> Unit
) {
    IvyButton(
        modifier = Modifier.padding(horizontal = 16.dp),
        size = ButtonSize.Big,
        visibility = ButtonVisibility.Medium,
        feeling = ButtonFeeling.Positive,
        text = text,
        icon = null,
        onClick = onClick
    )
}
// endregion

// region Components
@Composable
private fun SectionText(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    B1(
        text = text,
        modifier = modifier,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
        color = if (selected) UI.colors.primary else UI.colorsInverted.pure
    )
}
// endregion

// region Previews
@Preview
@Composable
private fun Preview() {
    IvyPreview {
        val modal = rememberIvyModal()
        modal.show()
        PeriodModal(
            modal = modal,
            selectedPeriod = SelectedPeriodUi.Monthly(
                btnText = "",
                month = MonthUi(
                    number = 2,
                    year = dateNowLocal().year,
                    currentYear = true,
                    fullName = fullMonthName(LocalContext.current, monthNumber = 2),
                ),
                periodUi = PeriodUi(
                    period = allTime(),
                    fromText = "Feb. 01",
                    toText = "Feb. 28"
                )
            )
        )
    }
}

@Composable
private fun previewState() = SelectedPeriodViewModel.UiState(
    startDayOfMonth = 1,
    months = monthsList(LocalContext.current, year = dateNowLocal().year, currentYear = true)
)
// endregion