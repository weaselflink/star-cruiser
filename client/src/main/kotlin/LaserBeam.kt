import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import three.AdditiveBlending
import three.DoubleSide
import three.core.Color
import three.core.Object3D
import three.geometries.PlaneGeometry
import three.materials.MaterialParameters
import three.materials.MeshBasicMaterial
import three.objects.Mesh
import three.textures.Texture
import kotlin.browser.document
import kotlin.math.PI

class LaserBeam(
    val obj: Object3D = Object3D()
) {

    init {
        val canvas = laserCanvas()
        val texture = Texture(canvas).apply {
            needsUpdate = true
        }
        val material = MeshBasicMaterial(
            MaterialParameters(
                map = texture,
                blending = AdditiveBlending,
                color = Color(0xff7f50),
                side = DoubleSide,
                depthWrite = false,
                transparent = true
            )
        )
        val geometry = PlaneGeometry(1.0, 0.1)
        val planes = 16
        (0 until planes).forEach { n ->
            Mesh(geometry, material).also { mesh ->
                mesh.position.x = 0.5
                mesh.rotation.x = n / planes * PI
                obj.add(mesh)
            }
        }
    }

    private fun laserCanvas(): HTMLCanvasElement {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.apply {
            width = 1
            height = 64
        }

        val cw = canvas.width.toDouble()
        val ch = canvas.height.toDouble()
        val ctx = canvas.getContext("2d")!! as CanvasRenderingContext2D

        ctx.fillStyle = ctx.createLinearGradient(0.0, 0.0, cw, ch).apply {
            addColorStop(0.0, "rgba(0, 0, 0, 0.1)")
            addColorStop(0.1, "rgba(160, 160, 160, 0.3)")
            addColorStop(0.5, "rgba(255, 255, 255, 0.5)")
            addColorStop(0.9, "rgba(160, 160, 160, 0.3)")
            addColorStop(1.0, "rgba(0, 0, 0, 0.1)")
        }
        ctx.fillRect(0.0, 0.0, cw, ch)
        return canvas
    }
}
