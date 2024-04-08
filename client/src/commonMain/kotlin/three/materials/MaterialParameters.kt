package three.materials

import three.Blending
import three.Side
import three.core.Color
import three.textures.Texture

open class MaterialParameters(
    var blending: Blending? = undefined,
    var color: Color? = undefined,
    var depthWrite: Boolean? = undefined,
    var map: Texture? = undefined,
    var side: Side? = undefined,
    var transparent: Boolean? = undefined,
    var wireframe: Boolean = false
)
