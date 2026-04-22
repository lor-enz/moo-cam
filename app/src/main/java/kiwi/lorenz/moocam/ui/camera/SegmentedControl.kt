package kiwi.lorenz.moocam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kiwi.lorenz.moocam.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> SegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp,
    selectedBackgroundColor: Color = colorResource(R.color.kiwi_green),
    unselectedBackgroundColor: Color = colorResource(R.color.transparent),
    selectedTextColor: Color = colorResource(R.color.off_white),
    unselectedTextColor: Color = colorResource(R.color.off_white),
    itemPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    animationSpec: AnimationSpec<Float> = spring()
) {
    val shape = RoundedCornerShape(percent = 50)
    val density = LocalDensity.current

    val selectedIndex by remember(items, selectedItem) {
        derivedStateOf { items.indexOf(selectedItem).coerceAtLeast(0) }
    }

    var itemWidthPx by remember { mutableFloatStateOf(0f) }
    var componentHeightPx by remember { mutableFloatStateOf(0f) }

    val indicatorOffsetX = remember { Animatable(0f) }
    val indicatorWidthPx = remember { Animatable(0f) }

    val hasLaidOut = remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, itemWidthPx) {
        if (itemWidthPx > 0f) {
            val targetOffset = (selectedIndex * itemWidthPx)
            val targetWidth = itemWidthPx

            if (!hasLaidOut.value) {
                indicatorOffsetX.snapTo(targetOffset)
                indicatorWidthPx.snapTo(targetWidth)
                hasLaidOut.value = true
            } else {
                if (indicatorOffsetX.targetValue != targetOffset) {
                    indicatorOffsetX.animateTo(targetOffset, animationSpec)
                }
                if (indicatorWidthPx.targetValue != targetWidth) {
                    indicatorWidthPx.animateTo(targetWidth, animationSpec)
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .semantics { testTagsAsResourceId = true },
        shape = shape,
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        color = unselectedBackgroundColor
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { indicatorOffsetX.value.toDp() })
                        .size(
                            width = with(density) { indicatorWidthPx.value.toDp() },
                            height = Dp.Unspecified
                        )
                        .fillMaxHeight()
                        .padding(borderWidth)
                        .background(selectedBackgroundColor, shape = shape)
                )

                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
                        label = "textColorAnimation_$index"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { onItemSelected(item) }
                            .padding(itemPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(),
                            color = textColor,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        ) { measurables, constraints ->
            val indicatorMeasurable = measurables.firstOrNull()
            val itemMeasurables = measurables.drop(1)

            val totalWidth = constraints.maxWidth
            val itemCount = itemMeasurables.size

            // I think if itemCount <= 0 we can skip the rest of the code.
            if (itemCount <= 0) {
                return@Layout layout(totalWidth, constraints.minHeight) { }
            }
            itemWidthPx = totalWidth.toFloat() / itemCount
            val itemConstraints = constraints.copy(minWidth = itemWidthPx.toInt(), maxWidth = itemWidthPx.toInt())

            val itemPlaceables = itemMeasurables.map { it.measure(itemConstraints) }

            componentHeightPx = (itemPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight).toFloat()

            val indicatorPlaceable = indicatorMeasurable?.measure(
                Constraints.fixed(width = itemWidthPx.toInt(), height = componentHeightPx.toInt())
            )
            layout(totalWidth, componentHeightPx.toInt()) {
                indicatorPlaceable?.placeRelative(x = 0, y = 0)

                var currentX = 0
                itemPlaceables.forEach { placeable ->
                    placeable.placeRelative(x = currentX, y = 0)
                    currentX += placeable.width
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSegmentedControl() {
    val items = listOf("Option 1", "Option 2", "Option 3")
    var selectedItem by remember { mutableStateOf(items[1]) }

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(16.dp)) {
            SegmentedControl(
                items = items,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSegmentedControl2() {
    val items = listOf("5 s", "7 s", "10 s", "15 s")
    var selectedItem by remember { mutableStateOf(items[1]) }

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(16.dp)) {
            SegmentedControl(
                items = items,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
            )
        }
    }
}