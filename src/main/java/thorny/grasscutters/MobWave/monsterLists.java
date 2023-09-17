package thorny.grasscutters.MobWave;

import java.util.List;

public class monsterLists {
    public static List<Integer> commonMobs = List.of(20010101, 20010201, 20010301,
            20010401, 20010501, 20010601, 20010701, 20010801, 20010901, 20011001,
            20011101, 20011201, 20011301, 20011401, 20011501, 20050201, 20050301,
            20050401, 20050501, 20050601, 20050701, 20050801, 20050901, 26120301, 26120201, 26120101, 21010101,
            21010201, 21010401, 21010501, 21010601, 21010701, 21010901, 21011001, 21011201,
            21011301, 21011401, 21011501, 21011601, 21030101, 21030201, 21030301, 21030401,
            21030501, 21030601, 22040101, 22040201, 25010101, 25010102, 25010103, 25010201,
            25010301, 25010401, 25010501, 25010601, 25010701, 25020101, 25020102, 25020201,
            25030101, 25030201, 25030301, 25040101, 25050101, 25050201, 25050301, 25050401,
            25050501, 25060101, 25070101, 25070202, 25080101, 25080201, 25080301, 25080401,
            26030101, 26051001, 26051101, 26060101, 26060201, 26060301, 26090101, 25210204,
            25210105, 25210306, 25210504, 22110101, 22110102, 22110201, 22110202, 22110301,
            22110302, 22110402, 22110403, 20051001, 20051002, 20051003, 20051401);

    public static List<Integer> eliteMobs = List.of(20020101, 21010301, 21020101, 21020201,
            21020301, 21020401, 21020501, 21020601, 21020701, 21020801, 22010101, 22010201, 22010301,
            22010401, 22050101, 22050201, 22070101, 22070201, 22070301, 22080101, 22090101, 23010101,
            23010201, 23010301, 23010401, 23010501, 23010601, 23020101, 23020102, 23030101, 23040101,
            23050101, 24010101, 24010201, 24010301, 24020101, 24020201, 24020301, 24020401, 24030201,
            25100101, 25100201, 25100301, 25100401, 26010101, 26010104, 26010201, 26010301, 26040101,
            26040102, 26040103, 26040104, 26040105, 26050601, 26050901, 20051101, 20051102, 20051103,
            20051501, 25310201, 26010102);

    public static List<Integer> bossMobs = List.of(20040101, 20040201, 20040301, 20040401, 20040501, 20040601,
            20040701, 20050102, 20070101, 22020101, 22030101, 22030201, 22060101, 24010401, 24030101, 25090101,
            25090102, 25090103, 25090104, 25090201, 25090401, 25090301, 26020101, 26020201, 26020301, 26050101,
            26050201, 26050301, 26050401, 26050501, 26080101, 26110101, 29020101, 29020102, 29030101, 29030102,
            29030103, 29050101, 29050102, 29060101, 29060102, 29060201, 29040101, 29040102, 29040103, 29040104,
            29040111, 24030301, 26010103, 26170101);

    public static List<Integer> getCommonMobs() {
        return commonMobs;
    }

    public static List<Integer> getEliteMobs() {
        return eliteMobs;
    }

    public static List<Integer> getBossMobs() {
        return bossMobs;
    }
}
