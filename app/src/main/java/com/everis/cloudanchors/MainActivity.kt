package com.everis.cloudanchors

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
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
                        appAnchorState != AppAnchorState.None) { //touch horizontal planos
                    return@setOnTapArPlaneListener
                }

                //Alojamos en cloud el anchor recien creado al pulsar sobre la pantalla
                val newAnchor = arSceneView.session?.hostCloudAnchor(hitResult.createAnchor())
                setCloudAnchor(newAnchor)

                appAnchorState = AppAnchorState.Hosting
                Toast.makeText(context, "Hosting anchor....", Toast.LENGTH_SHORT).show()


                placeObject(
                    this,
                    cloudAnchor!!,
                    Uri.parse("dog.sfb")
                ) //Posicionamos el objeto 3D con su Anchor

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
                    val resolvedAnchor = arSceneView.session!!.resolveCloudAnchor(cloudAnchorId)
                    setCloudAnchor(resolvedAnchor)
                    placeObject(this, resolvedAnchor, Uri.parse("dog.sfb"))
                    Toast.makeText(context, "Now resolving anchor....", Toast.LENGTH_SHORT).show()
                    appAnchorState = AppAnchorState.Resolving
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
            //Tenemos que asegurarnos que la peticion de hosting sólo se haga una vez
        if (appAnchorState != AppAnchorState.Hosting && appAnchorState != AppAnchorState.Resolving) {
            return
        }
        val cloudState = cloudAnchor?.cloudAnchorState

        if (appAnchorState == AppAnchorState.Hosting) {
            if (cloudState!!.isError) {
                Toast.makeText(this, "Error hosting anchor.... $cloudState", Toast.LENGTH_SHORT).show()
                appAnchorState = AppAnchorState.None
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                //cloudAnchorId es el ID que pueden usar otras aplicaciones para encontrar o compartir
                //el Anchor de Google Cloud service
                Toast.makeText(this, "Anchor hosted! Cloud ID: ${cloudAnchor!!.cloudAnchorId}", Toast.LENGTH_SHORT).show()
                appAnchorState = AppAnchorState.None
            }
        } else if (appAnchorState == AppAnchorState.Resolving) {
            if (cloudState!!.isError) {
                Toast.makeText(this, "Error resolving anchor.... $cloudState", Toast.LENGTH_SHORT).show()
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
