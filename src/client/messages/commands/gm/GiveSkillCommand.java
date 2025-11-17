package client.messages.commands.gm;
import client.messages.Command;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.MapleCharacter;

public class GiveSkillCommand extends Command { 
    @Override
    public void execute(MapleClient c, String[] splitted) {
        if (splitted.length < 2) {
            c.getPlayer().dropMessage(5, "Syntax: !giveskill <player> <skillid> [level] [masterlevel]");
            return;
        }

        MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[0]);
        if (victim == null) {
            c.getPlayer().dropMessage(5, "Player not found.");
            return;
        }

        Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
        if (skill == null) {
            c.getPlayer().dropMessage(5, "Skill not found.");
            return;
        }

        byte level = (byte) getOptionalIntArg(splitted, 2, 1);
        byte masterlevel = (byte) getOptionalIntArg(splitted, 3, 1);

        if (level > skill.getMaxLevel()) {
            level = (byte) skill.getMaxLevel();
        }

        if (masterlevel > skill.getMaxLevel()) {
            masterlevel = (byte) skill.getMaxLevel();
        }

        victim.changeSingleSkillLevel(skill, level, masterlevel);
        c.getPlayer().dropMessage(5, "Skill " + skill.getId() + " given to " + victim.getName() + " at level " + level + ".");
    }

    public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }
}