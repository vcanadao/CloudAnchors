package com.everis.cloudanchors

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*

sealed class AppAnchorState {
    object None : AppAnchorState()
    object Hosting : AppAnchorState()
    object Hosted : AppAnchorState()
    object Resolving : AppAnchorState()
    object Resolved : AppAnchorState()
}

class MainActivity : AppCompatActivity() {
    private var cloudAnchor: Anchor? = null
    private var appAnchorState: AppAnchorState = AppAnchorState.None


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(sceneformFragment as CustomArFragment) {
            arSceneView.scene.addOnUpdateListener(::onUpdateFrame)

            setOnTapArPlaneListener { hitResult, plane, motionEvent ->
                //Nos aseguramos de sólo posicionar en plano horizontal
                if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING ||
                    appAnchorState != AppAnchorState.None
                ) { //touch horizontal planos y nos aseguramos de que el estado del Anchor sea None
                    return@setOnTapArPlaneListener
                }

                /*
                    Alojamos en cloud el anchor recien creado al pulsar sobre la pantalla,
                    es decir lo almacenamos en cloud. Creamos el Anchor correspondiente a la posición
                    de la panttalla pulsada por el usuario usando el objeto HitResult devuelto en el
                    listener
                 */
                Toast.makeText(context, "Hosting anchor....", Toast.LENGTH_SHORT).show()
                val newAnchor = arSceneView.session?.hostCloudAnchor(hitResult.createAnchor())
                setCloudAnchor(newAnchor)

                /*
                    Cambiamos el estado a Hosting. Es importante hacerlo en este punto porque al setear
                    un nuevo Anchor se pone por defecto a None
                */
                appAnchorState = AppAnchorState.Hosting

                //Posicionamos el objeto 3D con su Anchor
                placeObject(
                    this,
                    cloudAnchor!!,
                    Uri.parse("dog.sfb")
                )

            }
        }

        clearButton.setOnClickListener {
            //Al limpiar seteamos nuestra variable global cloudAnchor a null para eliminarlo
            //al poner el cloudAnchor a null se eliminar de la escena el elemento 3D renderizado
            setCloudAnchor(null)
        }

        resolveButton.setOnClickListener {
            val cloudAnchorId = etResolve.text.toString()

            with(sceneformFragment as CustomArFragment) {
                if (cloudAnchorId.isNotEmpty()) {
                    Toast.makeText(context, "Now resolving anchor....", Toast.LENGTH_SHORT).show()
                    val resolvedAnchor = arSceneView.session!!.resolveCloudAnchor(cloudAnchorId) //Resolvemos el Anchor que está en cloud a partir de su ID
                    setCloudAnchor(resolvedAnchor) //Seteamos el Anchor resuelto
                    placeObject(this, resolvedAnchor, Uri.parse("dog.sfb")) //Cargamos la figura 3D en el Anchor
                    appAnchorState = AppAnchorState.Resolving //Cambiamos el state del Anchor a Resolving. Es importnte hacerlo al final porque cuando lo seteamos se pone a None
                }
            }
        }
    }

    //Debemos comprobar que el Anchor ha sido alojado correctamente
    private fun onUpdateFrame(frameTime: FrameTime) {
        checkUpdateAnchor() //Cada vez que el frame de la scene se actualiza comprobamos el updated anchor
    }

    @Synchronized //Nos aseguramos que sólo un hilo pueda estar accediendo a la función
    private fun checkUpdateAnchor() {
        /*
            Tenemos que asegurarnos que la peticion de hosting sólo se haga una vez
            es decir sólo hospedamos el Anchor cuando el estado sea Hosting o Resolving
         */
        if (appAnchorState != AppAnchorState.Hosting && appAnchorState != AppAnchorState.Resolving) {
            return
        }
        /*
            Obtenemos el estado del cloudAnchor en cloud
            Hay que diferenciar este estado al appAnchorState. appAnchorState es un estado nuestro
            que usamos para saber la situación actual en la que estamos con el Anchor y cloudAnchorState
            es el estado
         */
        val cloudState = cloudAnchor?.cloudAnchorState

        if (appAnchorState == AppAnchorState.Hosting) {
            if (cloudState!!.isError) {
                Toast.makeText(this, "Error hosting anchor.... $cloudState", Toast.LENGTH_SHORT)
                    .show()
                appAnchorState = AppAnchorState.None
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                /*
                    Si el estado es Hosting y no ha dado error obtenemos el cloudAnchorId
                    cloudAnchorId es el ID que pueden usar otras aplicaciones para encontrar o compartir
                    el Anchor de Google Cloud service
                 */

                val id = cloudAnchor!!.cloudAnchorId
                etResolve.setText(id) //Lo seteamos por comidada en el cuadro de texto
                Toast.makeText(this, "Anchor hosted! Cloud ID: $id", Toast.LENGTH_SHORT).show()
                appAnchorState = AppAnchorState.None //Cambiamos el state a none
            }
        } else if (appAnchorState == AppAnchorState.Resolving) {
            if (cloudState!!.isError) {
                Toast.makeText(this, "Error resolving anchor.... $cloudState", Toast.LENGTH_SHORT)
                    .show()
                appAnchorState = AppAnchorState.None
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                Toast.makeText(this, "Anchor resolved successfully", Toast.LENGTH_SHORT).show()
                appAnchorState = AppAnchorState.Resolved
            }
        }
    }

    private fun setCloudAnchor(newAnchor: Anchor?) {
        if (cloudAnchor != null) { //Si el cloudAnchor existe le hacemos un detach, para poder reasignarlo
            cloudAnchor!!.detach()
        }
        //Asignamos el CloudAnchor a un nuevo Anchor
        cloudAnchor = newAnchor
        appAnchorState = AppAnchorState.None
    }

    private fun addNodeToScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable) {
        //renderable es el modelo 3d ya renderizado y que queremos posicionar en la escena
        //anchor es la posición relativa al mundo real donde queremos posicionar el renderable
        var anchorNode = AnchorNode(anchor)
        anchorNode.setParent(fragment.arSceneView.scene)

        //Usamos un TransformableNode para poder trabajar con el Node y aplicarle cambios. En nuestro caso será cargar el modelo renderizado
        var nodeTransformation = TransformableNode(fragment.transformationSystem)

        nodeTransformation.setParent(anchorNode)
        nodeTransformation.renderable = renderable
        nodeTransformation.select()
    }

    private fun placeObject(fragment: ArFragment, anchor: Anchor, model: Uri?) {
        /*
        AGREGANDO RENDERABLE
        Mediante el ModelRenderalbe  obtenemos el Nodo con el modelo renderizado
        para luego ponerlo en la escena
         */
        ModelRenderable.builder()
            .setSource(fragment.context, model)
            .build()
            .thenAccept { renderable ->
                //Objeto renderizado
                addNodeToScene(fragment, anchor, renderable)
            }
            .exceptionally { throwable ->
                val toast =
                    Toast.makeText(
                        this,
                        "Renderable no cargado",
                        Toast.LENGTH_LONG
                    )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }
    }

}
