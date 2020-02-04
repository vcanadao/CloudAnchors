package com.everis.cloudanchors

import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment

class CustomArFragment : ArFragment(){

    override fun getSessionConfiguration(session: Session?): Config {
     //Deshabilitamos la animacion de la mano del inicio
        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        //Obtenemos la configuracion por defecto llamando al metodo getSessionConfiguration de la clase Padre
        val config = super.getSessionConfiguration(session)

        //Habilitamos el modo CloudAnchor
        config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED

        return config
    }
}