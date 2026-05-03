local LML = require("selene.ui.lml")

return {
    CreateAtlas = function(textures)
        return coroutine.yield(LML.CreateAtlas(textures))
    end,
    LoadTheme = function(theme, atlas)
        return coroutine.yield(LML.LoadTheme(theme, atlas))
    end,
    LoadUI = function(path, options)
        return coroutine.yield(LML.LoadUI(path, options))
    end
}
