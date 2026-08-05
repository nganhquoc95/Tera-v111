/*
    Fire Flower reactor (1022003) - GMS v111 behavior
    Drops configured reactor rewards and removes the reactor to
    prevent duplicate rewards. Uses the existing ReactorActionManager API (`rm`).
*/

function act() {
    // Use configured drops from the reactordrops DB via ReactorActionManager
    try {
        rm.dropSingleItem(4033051); // Fire Flower
    } catch (e) {
        // Log to server side; script engine will surface exceptions server-side too
        rm.mapMessage("An error occurred while processing the Fire Flower.");
    }
}
