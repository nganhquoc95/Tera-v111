/*
 * NPC: Shammos
 * ID: 2022008
 * Map: Chief's Residence (211000002)
 * Function: Shammos PQ / Quest 3122 Helper
 */

var status = -1;
var minLevel = 30;
var maxLevel = 200;
var minPartySize = 2; // Số người tối thiểu tham gia PQ
var targetMap = 921120000; // Map chờ Hob King PQ

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        if (status == 0) {
            cm.dispose();
            return;
        }
        status--;
    }

    if (status == 0) {
        // Kiểm tra xem người chơi có đang làm Quest 3122 hay không
        if (cm.isQuestActive(3122)) {
            cm.sendNext("Ah, you want to inspect the seal of Rex the Hob King? Be careful, the ancient powers here are dangerous...");
        } else if (cm.getQuestStatus(3122) == 2) {
            // Đã hoàn thành quest 3122 -> Mở menu vào PQ
            var text = "What brings you to me?\r\n#b";
            text += "\r\n#L0# Enter the Hob King Escort (Party Quest)#l";
            text += "\r\n#L1# Hear the story about Rex the Hob King#l";
            cm.sendSimple(text);
        } else {
            // Chưa nhận quest
            cm.sendOk("I am Shammos... Look at these chains. I am bound here until my duty is complete.");
            cm.dispose();
        }
    } else if (status == 1) {
        if (cm.isQuestActive(3122)) {
            // Tiến hành cập nhật tiến trình quest 3122
            cm.forceStartQuest(3122, "1");
            cm.sendOk("I have granted you access to check the seal. Return to the Chief after inspecting it.");
            cm.dispose();
        } else {
            // Lựa chọn Menu PQ
            if (selection == 0) {
                // Kiểm tra điều kiện Party Quest
                if (cm.getParty() == null) { // Không có party
                    cm.sendOk("You must be in a party to attempt the Hob King Escort.");
                    cm.dispose();
                } else if (!cm.isLeader()) { // Không phải trưởng nhóm
                    cm.sendOk("Please ask your party leader to speak with me.");
                    cm.dispose();
                } else {
                    // Kiểm tra thành viên party (Cấp độ & Vị trí)
                    var party = cm.getParty().getMembers();
                    var inMap = cm.partyMembersInMap();
                    var levelValid = true;

                    for (var i = 0; i < party.size(); i++) {
                        if (party.get(i).getLevel() < minLevel || party.get(i).getLevel() > maxLevel) {
                            levelValid = false;
                            break;
                        }
                    }

                    if (inMap < minPartySize) {
                        cm.sendOk("You need at least " + minPartySize + " party members present in this map.");
                    } else if (!levelValid) {
                        cm.sendOk("One of your party members does not meet the level requirement (" + minLevel + " - " + maxLevel + ").");
                    } else {
                        // Đủ điều kiện -> Đưa cả party vào Map PQ
                        cm.warpParty(targetMap, 0);
                    }
                    cm.dispose();
                }
            } else if (selection == 1) {
                cm.sendOk("Rex was a powerful Hobgoblin leader who was sealed long ago...");
                cm.dispose();
            }
        }
    } else {
        cm.dispose();
    }
}