package client.messages.commands.gm;
import client.messages.Command;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.MapleCharacter;

public class GiveSkillCommand extends Command { 
    @Override
    public void execute(MapleClient c, String[] splitted) {
        if (splitted.length < 3) {
            c.getPlayer().dropMessage(5, "Syntax: !giveskill <player> <skillid> [level] [masterlevel]");
            return;
        }

        MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[0]);
        Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
        byte level = (byte) getOptionalIntArg(splitted, 2, 1);
        byte masterlevel = (byte) getOptionalIntArg(splitted, 3, 1);

        if (level > skill.getMaxLevel()) {
            level = (byte) skill.getMaxLevel();
        }

        if (masterlevel > skill.getMaxLevel()) {
            masterlevel = (byte) skill.getMaxLevel();
        }
        victim.changeSingleSkillLevel(skill, level, masterlevel);   
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