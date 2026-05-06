local Visuals = require("selene.visuals.internal")

return {
    create = function(identifier)
        return coroutine.yield(Visuals.create(identifier))
    end
}
