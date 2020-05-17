import org.w3c.dom.HTMLElement
import kotlin.browser.document

class NavigationUi {

    private val root = document.getElementById("navigation")!! as HTMLElement

    fun show() {
        root.style.visibility = "visible"
    }

    fun hide() {
        root.style.visibility = "hidden"
    }
}
