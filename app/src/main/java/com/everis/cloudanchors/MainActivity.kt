package com.everis.cloudanchors

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var cloudAnchor: Anchor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(sceneformFragment as CustomArFragment) {
            this.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
                //Nos aseguramos de sólo posicionar en plano horizontal
                if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) { //touch horizontal planos
                    return@setOnTapArPlaneListener
                }

                val newAnchor = hitResult.createAnchor()
                setCloudAnchor(newAnchor)

                placeObject(
                    this,
                    cloudAnchor,
                    Uri.parse("dog.sfb")
                ) //Posicionamos el objeto 3D con su Anchor

            }
        }
    }

    private fun setCloudAnchor(newAnchor: Anchor) {
        if (cloudAnchor != null) { //Si el cloudAnchor existe le hacemos un detach, para poder reasignarlo
            cloudAnchor.detach()
        }
        //Asignamos el CloudAnchor a un nuevo Anchor
        cloudAnchor = newAnchor
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
                        getString(R.string.error_message_renderable_not_loaded),
                        Toast.LENGTH_LONG
                    )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }
    }

}
