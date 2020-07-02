@file:Suppress("EnumEntryName")

import org.w3c.dom.Document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

enum class Visibility {
    visible,
    hidden
}

var HTMLElement.visibility: Visibility
    get() = Visibility.values().firstOrNull { it.name == style.visibility } ?: Visibility.visible
    set(value) {
        style.visibility = value.name
    }

enum class Display {
    none,
    block
}

var HTMLElement.display: Display
    get() = Display.valueOf(style.display)
    set(value) {
        style.display = value.name
    }

val HTMLElement.canvas: HTMLCanvasElement
    get() = querySelector("canvas") as HTMLCanvasElement

fun Document.getHtmlElementById(id: String): HTMLElement = getElementById(id)!! as HTMLElement
