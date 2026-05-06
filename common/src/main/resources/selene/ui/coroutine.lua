local LML = require("selene.ui.lml")

return {
    createAtlas = function(textures)
        return coroutine.yield(LML.createAtlas(textures))
    end,
    loadTheme = function(theme, atlas)
        return coroutine.yield(LML.loadTheme(theme, atlas))
    end,
    loadUI = function(path, options)
        return coroutine.yield(LML.loadUI(path, options))
    end
}
