@file:JsQualifier("THREE")

package three.core

external class Layers {

    var mask: Int

    fun disable(layer: Int)
    fun enable(layer: Int)
    fun set(layer: Int)
    fun test(layers: Layers): Boolean
    fun toggle(layer: Int)

    fun enableAll()
    fun disableAll()
}
