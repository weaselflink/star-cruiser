package three

import three.core.Object3D
import three.scenes.Scene

operator fun Scene.plusAssign(obj: Object3D) = this.add(obj)