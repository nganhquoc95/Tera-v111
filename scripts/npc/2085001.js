/*
    Dragon Rider
*/

function start() {
    cm.sendSimple("#e<Party Quest: Dragon Rider>#n\r\n\r\n\
        Welcome to the entrance to Crimson Sky. What would you like to do?\r\n\r\n\
        #L0##bEnter Crimson Sky.(Lv 120 or above)#l");
}

function action(mode, type, selection) {
    if (mode > 0) {
        console.log("Mode: " + mode);
        if (cm.getPlayer().getMap().getId() == 240080000) {
            console.log("Map ID: " + cm.getPlayer().getMap().getId());
            cm.getPlayer().saveLocation(Packages.server.maps.SavedLocationType.fromString("DRAGON_RIDER"));
            cm.getPlayer().changeMap(cm.getMap(180000005), cm.getMap(180000005).getPortal(1));
            cm.dispose();
            return;
        }
        // cm.sendOk("The Party Quest is in progress, please try other channels.");
    }
    cm.dispose();
}