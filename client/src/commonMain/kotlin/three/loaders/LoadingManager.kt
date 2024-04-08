@file:JsQualifier("THREE")

package three.loaders

external class LoadingManager(
    onLoad: (() -> Unit)? = definedExternally,
    onProgress: ((String, Number, Number) -> Unit)? = definedExternally,
    onError: ((String) -> Unit)? = definedExternally
)
