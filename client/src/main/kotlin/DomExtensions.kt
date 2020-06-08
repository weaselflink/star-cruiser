import org.w3c.dom.HTMLElement

var HTMLElement.visibility: Visibility
    get() = Visibility.valueOf(style.visibility)
    set(value) {
        style.visibility = value.name
    }

enum class Visibility {
    visible,
    hidden
}

var HTMLElement.display: Display
    get() = Display.valueOf(style.display)
    set(value) {
        style.display = value.name
    }

enum class Display {
    none,
    block
}
