import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.dp

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalGap: Int = 8, // 水平间距
    verticalGap: Int = 8, // 垂直间距
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->

        val horizontalGapPx = horizontalGap.dp.toPx().toInt()
        val verticalGapPx = verticalGap.dp.toPx().toInt()

        val rows = mutableListOf<List<Placeable>>()
        var rowWidth = 0
        var rowHeight = 0
        var row = mutableListOf<Placeable>()

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (rowWidth + placeable.width > constraints.maxWidth) {
                rows.add(row)
                rowWidth = 0
                rowHeight += placeable.height + verticalGapPx
                row = mutableListOf()
            }

            row.add(placeable)
            rowWidth += placeable.width + horizontalGapPx
        }
        rows.add(row)

        val width = constraints.maxWidth
        val height = rowHeight

        layout(width, height) {
            var yPosition = 0

            rows.forEach { row ->
                var xPosition = 0

                row.forEach { placeable ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width + horizontalGapPx
                }

                yPosition += row.first().height + verticalGapPx
            }
        }
    }
}
