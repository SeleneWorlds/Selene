package world.selene.docgen

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.github.czyzby.lml.parser.impl.tag.Dtd
import world.selene.client.ui.lml.SeleneLmlParser

class LmlAnalyzer {

    fun writeDtd(appendable: Appendable) {
        Lwjgl3Application(object : ApplicationAdapter() {
            override fun create() {
                val parser = SeleneLmlParser.parser().build()
                Dtd.saveSchema(parser, appendable)
                Gdx.app.exit()
            }
        })
    }

}