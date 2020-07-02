package components

import Visibility
import byQuery
import de.bissell.starcruiser.pad
import getHtmlElementById
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import visibility
import kotlin.browser.document
import kotlin.math.roundToInt

class SelectionDetails(
    private val onScan: () -> Unit,
    private val onDelete: () -> Unit
) {

    private val root = document.getHtmlElementById("selection-details")
    private val scanButton: HTMLButtonElement = root.byQuery(".detailsScanButton")
    private val deleteButton: HTMLButtonElement = root.byQuery(".detailsDeleteButton")
    private val designationLabel: HTMLElement = root.byQuery(".designation")
    private val bearingLabel: HTMLElement = root.byQuery(".bearing")
    private val rangeLabel: HTMLElement = root.byQuery(".range")

    init {
        hide()
        scanButton.onclick = { onScan() }
        deleteButton.onclick = { onDelete() }
    }

    fun hide() {
        root.visibility = Visibility.hidden
        scanButton.visibility = Visibility.hidden
        deleteButton.visibility = Visibility.hidden
    }

    fun draw(selection: Selection?) {
        if (selection != null) {
            root.visibility = Visibility.visible
            designationLabel.innerHTML = selection.label
            bearingLabel.innerHTML = selection.bearing.roundToInt().pad(3)
            rangeLabel.innerHTML = selection.range.roundToInt().toString()

            when {
                selection.canScan -> {
                    scanButton.visibility = Visibility.visible
                    deleteButton.visibility = Visibility.hidden
                }
                selection.canDelete -> {
                    scanButton.visibility = Visibility.hidden
                    deleteButton.visibility = Visibility.visible
                }
                else -> {
                    scanButton.visibility = Visibility.hidden
                    deleteButton.visibility = Visibility.hidden
                }
            }
        } else {
            hide()
        }
    }
}