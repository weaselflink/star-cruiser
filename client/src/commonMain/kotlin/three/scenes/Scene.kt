@file:JsQualifier("THREE")

package three.scenes

import three.core.Object3D
import three.materials.Material

open external class Scene : Object3D {

    var background: dynamic

    var overrideMaterial: Material
}
