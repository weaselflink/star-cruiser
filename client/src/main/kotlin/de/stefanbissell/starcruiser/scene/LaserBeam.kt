package de.stefanbissell.starcruiser.scene

import de.stefanbissell.starcruiser.context2D
import org.w3c.dom.HTMLCanvasElement
import three.AdditiveBlending
import three.DoubleSide
import three.core.Color
import three.core.Object3D
import three.geometries.PlaneGeometry
import three.lights.PointLight
import three.materials.MaterialParameters
import three.materials.MeshBasicMaterial
import three.objects.Mesh
import three.textures.Texture
import kotlin.browser.document
import kotlin.math.PI

class LaserBeam(
    private val length: Number = 1.0,
    private val width: Number = 0.2
) {

    val obj = Object3D()

    init {
        createBeam()
        createLight()
    }

    private fun createLight() {
        PointLight(
            color = Color(0xff9900),
            intensity = 10.0,
            distance = 5.0
        ).apply {
            position.z = 1.0
        }.also {
            obj.add(it)
        }
    }

    private fun createBeam() {
        val beam = Object3D()
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
        val geometry = PlaneGeometry(1.0, width)
        val planes = 16
        (0 until planes).forEach { n ->
            Mesh(geometry, material).also { mesh ->
                mesh.position.x = 0.5
                mesh.rotation.x = n.toDouble() / planes * PI
                beam.add(mesh)
            }
        }
        beam.rotation.y = -PI * 0.5
        Object3D().apply {
            add(beam)
            scale.z = length.toDouble()
        }.also {
            obj.add(it)
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
        val ctx = canvas.context2D

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
