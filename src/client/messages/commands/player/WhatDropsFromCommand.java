//package client.messages.commands.player;
//
//import client.MapleCharacter;
//import client.MapleClient;
//import client.messages.Command;
//import database.DatabaseConnection;
//import java.util.Iterator;
//import server.MapleItemInformationProvider;
//import java.sql.*;
//import server.ItemInformation;
//import server.life.MapleMonsterInformationProvider;
//
//import static com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolver.iterator;

//public class WhatDropsFromCommand extends Command {
//    public void execute(MapleClient c, String[] params) {
//        MapleCharacter player = c.getPlayer();
//        if (params.length < 1) {
//            player.dropMessage(5, "Synax: @whatdropsfrom <monster name>");
//            return;
//        }
//        String monsterName = player.getLastCommandMessage();
//        String output = "Results for " + searchString + "\r\n";
//        int limit = 3;
//        Iterator<MapleMonsterInformationProvider> listIterator = MapleMonsterInformationProvider.getInstance().getMobNameFromId(monsterName)).iterator();
//    }
//}
