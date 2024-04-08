@file:JsQualifier("THREE")

package three.loaders

import three.cameras.Camera
import three.objects.Group

external class GLTFLoader(
    manager: LoadingManager? = definedExternally
) : Loader {

    fun load(
        url: String,
        onLoad: (GLTF) -> Unit = definedExternally,
        onProgress: ((dynamic) -> Unit)? = definedExternally,
        onError: ((dynamic) -> Unit)? = definedExternally
    )
}

external interface GLTF {
    var scene: Group
    var scenes: Array<Group>
    var cameras: Array<Camera>
}
