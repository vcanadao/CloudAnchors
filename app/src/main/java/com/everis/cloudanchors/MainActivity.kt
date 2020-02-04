package com.everis.cloudanchors

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.ar.core.Anchor
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
}
