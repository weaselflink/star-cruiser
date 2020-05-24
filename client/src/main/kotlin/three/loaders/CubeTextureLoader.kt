@file:JsQualifier("THREE")

package three.loaders

import three.textures.CubeTexture

external class CubeTextureLoader(
    manager: LoadingManager? = definedExternally
) : Loader {

    fun load(
        urls: Array<String>,
        onLoad: (CubeTexture) -> Unit = definedExternally,
        onProgress: ((dynamic) -> Unit)? = definedExternally,
        onError: ((dynamic) -> Unit)? = definedExternally
    ): CubeTexture
}
