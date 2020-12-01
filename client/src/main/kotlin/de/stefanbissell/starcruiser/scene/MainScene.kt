package de.stefanbissell.starcruiser.scene

import de.stefanbissell.starcruiser.AsteroidMessage
import de.stefanbissell.starcruiser.CameraMessage
import de.stefanbissell.starcruiser.ContactMessage
import de.stefanbissell.starcruiser.MainScreenView
import de.stefanbissell.starcruiser.SnapshotMessage
import de.stefanbissell.starcruiser.Vector2
import kotlinx.browser.window
import three.cameras.Camera
import three.cameras.PerspectiveCamera
import three.lights.AmbientLight
import three.lights.DirectionalLight
import three.loaders.CubeTextureLoader
import three.loaders.GLTFLoader
import three.math.Vector3
import three.objects.Group
import three.plusAssign
import three.scenes.Scene
import three.set
import three.updateSize
import kotlin.math.PI
import kotlin.math.max

class MainScene {

    val scene = Scene()

    private val ownShip = ShipGroup().also { scene.add(it) }
    private var ownShipModel: String? = null
    private val contactHandler = GroupHandler<ShipGroup, ContactMessage>(
        scene = scene,
        factory = { message ->
            ShipGroup().also { node ->
                objectModels[message.model]?.let { model ->
                    node.model = model.clone(true)
                }
                node.shieldModel = shieldModel?.clone(true)
            }
        },
        update = { contact ->
            position.copy(contact.relativePosition.toWorld())
            rotation.y = contact.rotation
            jumpAnimationScale(contact.jumpAnimation).also {
                scale.set(it, it, it)
            }
        }
    )
    private val asteroidHandler = GroupHandler<AsteroidGroup, AsteroidMessage>(
        scene = scene,
        factory = { message ->
            AsteroidGroup(message.radius).also { node ->
                objectModels[message.model]?.let { model ->
                    node.model = model.clone(true)
                }
            }
        },
        update = { asteroid ->
            position.copy(asteroid.relativePosition.toWorld())
            rotation.y = asteroid.rotation
        }
    )
    private val objectModels = mutableMapOf<String, Group>()
    private var shieldModel: Group? = null

    private val cameras = CameraPosition.values()
        .associate { pos ->
            if (pos == CameraPosition.Top) {
                pos to createTopCamera().also { ownShip += it }
            } else {
                pos to createCamera().also { ownShip += it }
            }
        }

    init {
        createLights()
        loadBackground()
        loadObjectModels()
        loadShieldModel()
    }

    fun updateSize(windowWidth: Int, windowHeight: Int) {
        cameras.values
            .forEach {
                it.updateSize(windowWidth, windowHeight)
            }
    }

    fun update(snapshot: SnapshotMessage.MainScreen3d): Camera {
        ownShip.rotation.y = snapshot.ship.rotation
        jumpAnimationScale(snapshot.ship.jumpDrive.animation).also {
            ownShip.scale.set(it, it, it)
        }
        val model = snapshot.ship.model
        if (ownShipModel != model) {
            ownShipModel = model
            objectModels[model]?.let {
                ownShip.model = it
            }
            updateCamera(cameras[CameraPosition.Front], snapshot.ship.frontCamera)
            updateCamera(cameras[CameraPosition.Left], snapshot.ship.leftCamera)
            updateCamera(cameras[CameraPosition.Right], snapshot.ship.rightCamera)
            updateCamera(cameras[CameraPosition.Rear], snapshot.ship.rearCamera)
        }

        contactHandler.update(snapshot.contacts)
        asteroidHandler.update(snapshot.asteroids)

        updateBeams(snapshot)
        updateShields(snapshot)

        return CameraPosition.fromMessage(snapshot.ship.mainScreenView).getCamera()
    }

    private fun CameraPosition.getCamera() =
        cameras[this] ?: cameras.values.first()

    private fun updateBeams(snapshot: SnapshotMessage.MainScreen3d) {
        ownShip.updateBeams(snapshot, snapshot.ship.beams)

        contactHandler.nodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.let { contact ->
                node.value.updateBeams(snapshot, contact.beams)
            }
        }
    }

    private fun updateShields(snapshot: SnapshotMessage.MainScreen3d) {
        contactHandler.nodes.values.forEach { it.hideShield() }

        if (snapshot.ship.shield.activated) {
            ownShip.showShield(snapshot.ship.shield.radius)
        } else {
            ownShip.hideShield()
        }
        contactHandler.nodes.forEach { node ->
            snapshot.contacts.firstOrNull { it.id == node.key }?.also { contact ->
                if (contact.shield.activated) {
                    node.value.showShield(contact.shield.radius)
                } else {
                    node.value.hideShield()
                }
            }
        }
    }

    private fun createCamera(): PerspectiveCamera {
        return PerspectiveCamera(
            fov = 75,
            aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
            near = 0.1,
            far = 10_000
        )
    }

    private fun updateCamera(camera: PerspectiveCamera?, cameraMessage: CameraMessage) {
        camera?.apply {
            fov = cameraMessage.fov
            position.set(cameraMessage.position)
            rotation.y = cameraMessage.rotation
        }
    }

    private fun createTopCamera(): PerspectiveCamera {
        return PerspectiveCamera(
            fov = 75,
            aspect = window.innerWidth.toDouble() / window.innerHeight.toDouble(),
            near = 1,
            far = 10_000
        ).apply {
            position.y = 100.0
            rotation.x = PI * -0.5
        }
    }

    private fun createLights() {
        scene += AmbientLight(intensity = 0.25)
        scene += DirectionalLight(intensity = 4).apply {
            position.x = 5000.0
            position.y = 1000.0
        }
    }

    private fun jumpAnimationScale(animation: Double?) =
        max(
            0.01,
            when {
                animation == null -> 1.0
                animation > -0.2 && animation <= 0.0 -> -animation / 0.2
                animation > 0.0 && animation < 0.2 -> animation / 0.2
                else -> 1.0
            }
        )

    private fun loadBackground() {
        CubeTextureLoader().load(
            urls = backgroundUrls { "/assets/backgrounds/default/$it.png" },
            onLoad = { scene.background = it }
        )
    }

    private fun backgroundUrls(block: (String) -> String): Array<String> {
        return listOf("right", "left", "top", "bottom", "front", "back")
            .map(block)
            .toTypedArray()
    }

    private fun loadObjectModels() {
        listOf("carrier", "cruiser01", "asteroid01", "torpedo01").forEach(this::loadObjectModel)
    }

    private fun loadObjectModel(name: String) {
        loadModel("$name.glb") { group ->
            objectModels[name] = group
            if (ownShipModel == name && ownShip.model == null) {
                ownShip.model = group.clone(true)
            }
            contactHandler.assignModel(name, group)
            asteroidHandler.assignModel(name, group)
        }
    }

    private fun loadShieldModel() {
        loadModel("shield-cube.glb") { group ->
            shieldModel = group
            ownShip.shieldModel = group.clone(true)
            contactHandler.nodes.values.forEach {
                it.shieldModel = group.clone(true)
            }
        }
    }

    private fun loadModel(name: String, onLoad: (Group) -> Unit) {
        GLTFLoader().load(
            url = "/assets/models/$name",
            onLoad = { onLoad(it.scene) }
        )
    }
}

enum class CameraPosition {
    Front,
    Left,
    Right,
    Rear,
    Top;

    companion object {
        fun fromMessage(mainScreenView: MainScreenView) =
            when (mainScreenView) {
                MainScreenView.Front -> Front
                MainScreenView.Left -> Left
                MainScreenView.Right -> Right
                MainScreenView.Rear -> Rear
                else -> Top
            }
    }
}

fun Vector2.toWorld() = Vector3(-y, 0.0, -x)
