/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2012 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation= 0, either version 3 of the License= 0, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not= 0, see <http://www.gnu.org/licenses/>.
 */
package server;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author AlphaEta
 */
public class StructItemOption {

    public static String[] types = { "incSTR", "incDEX", "incINT", "incLUK", "incACC", "incEVA", "incSpeed", "incJump",
            "incPAD", "incMAD", "incPDD", "incMDD", "prop", "time", "incSTRr", "incDEXr", "incINTr",
            "incLUKr", "incMHPr", "incMMPr", "incACCr", "incEVAr", "incPADr", "incMADr", "incPDDr",
            "incMDDr", "incCr", "incDAMr", "RecoveryHP", "RecoveryMP", "HP", "MP", "level",
            "ignoreTargetDEF", "ignoreDAM", "incAllskill", "ignoreDAMr", "RecoveryUP",
            "incCriticaldamageMin", "incCriticaldamageMax", "incTerR", "incAsrR", "DAMreflect",
            "mpconReduce", "reduceCooltime", "incMesoProp", "incRewardProp", "boss", "incMHP", "incMMP", "attackType" };
    public int optionType, reqLevel, opID; // opID = nebulite Id or potential ID
    public String face; // angry, cheers, love, blaze, glitter
    public String stringTemplate;
    public Map<String, Integer> data = new HashMap<>();

    public int get(final String type) {
        return data.getOrDefault(type, 0);
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("#([A-Za-z0-9_]+)");

    @Override
    public final String toString() { // I should read from the "string" value instead.
        if (stringTemplate == null || stringTemplate.isEmpty()) {
            return "";
        }

        Matcher m = PLACEHOLDER.matcher(stringTemplate);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String key = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(get(key))));
        }

        m.appendTail(sb);
        return sb.toString();

    }
}
