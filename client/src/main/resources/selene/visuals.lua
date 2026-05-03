local Visuals = require("selene.visuals.internal")

return {
    Create = function(identifier)
        return coroutine.yield(Visuals.Create(identifier))
    end
}
