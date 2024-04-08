package three.materials

import three.core.Color

class MeshStandardMaterialParameters(
    color: Color? = undefined,
    wireframe: Boolean = false
) : MaterialParameters(
    color = color,
    wireframe = wireframe
)
