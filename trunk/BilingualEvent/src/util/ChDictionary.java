package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChDictionary {

	public ChDictionary() {
		personPronouns.addAll(this.animatePronouns);
		allPronouns.addAll(firstPersonPronouns);
		allPronouns.addAll(secondPersonPronouns);
		allPronouns.addAll(thirdPersonPronouns);
		allPronouns.addAll(otherPronouns);
		this.surnames = Common.readSurname("dict/surname");
		this.locationSuffix = Common.readFile2Set("dict/location_suffix");
		// load gender number dictionary
		loadDemonymLists("dict/demonyms.txt");
		loadStateAbbreviation("dict/state-abbreviations.txt");
		adjectiveNation.addAll(demonymSet);
		adjectiveNation.removeAll(demonyms.keySet());

		animateHead = Common.readFile2Map("dict/chinese_animate");
		inanimateHead = Common.readFile2Map("dict/chinese_inanimate");
		maleHead = Common.readFile2Map("dict/chinese_male");
		femaleHead = Common.readFile2Map("dict/chinese_female");
		singleHead = Common.readFile2Map("dict/chinese_single");
		pluraHead = Common.readFile2Map("dict/chinese_plura");
		countries = Common.readFile2Set("dict/country2");
	}

	public HashSet<String> countries;

	public HashMap<String, Integer> animateHead = new HashMap<String, Integer>();
	public HashMap<String, Integer> inanimateHead = new HashMap<String, Integer>();

	public HashMap<String, Integer> singleHead = new HashMap<String, Integer>();
	public HashMap<String, Integer> pluraHead = new HashMap<String, Integer>();

	public HashMap<String, Integer> maleHead = new HashMap<String, Integer>();
	public HashMap<String, Integer> femaleHead = new HashMap<String, Integer>();

	public HashSet<String> locationSuffix;

	public final Map<String, Set<String>> demonyms = new HashMap<String, Set<String>>();
	public final Set<String> demonymSet = new HashSet<String>();
	public final Set<String> adjectiveNation = new HashSet<String>();

	private void loadDemonymLists(String demonymFile) {
		ArrayList<String> lines = Common.getLines(demonymFile);
		for (String oneline : lines) {
			String[] line = oneline.split("\t");
			if (line[0].startsWith("#"))
				continue;
			Set<String> set = new HashSet<String>();
			for (String s : line) {
				set.add(s.toLowerCase());
				demonymSet.add(s.toLowerCase());
			}
			demonyms.put(line[0].toLowerCase(), set);
			adjectiveNation.addAll(demonymSet);
			adjectiveNation.removeAll(demonyms.keySet());
		}
	}

	public void loadStateAbbreviation(String statesFile) {
		ArrayList<String> lines = Common.getLines(statesFile);
		for (String line : lines) {
			String[] tokens = line.split("\t");
			statesAbbreviation.put(tokens[1], tokens[0]);
			statesAbbreviation.put(tokens[2], tokens[0]);
		}
	}

	public final Map<String, String> statesAbbreviation = new HashMap<String, String>();

	public final Set<String> pluralModify = new HashSet<String>(Arrays.asList("几", "多", "不少", "很多", "一些", "有些", "部分",
			"多数", "少数", "更多", "更少", "所有", "５", "大量"));

	public final Set<String> singleModify = new HashSet<String>(Arrays.asList("一"));

	public final Set<String> nonModify = new HashSet<String>(Arrays.asList(""));

	public final Set<String> temporals = new HashSet<String>(Arrays.asList("秒", "分钟", "小时", "天", "日", "周", "月", "年",
			"世纪", "毫秒", "周一", "周二", "周三", "周四", "周五", "周六", "周日", "今天", "现在", "昨天", "明年", "岁", "时间", "早晨", "早上", "晚上",
			"白天", "下午", "中午", "学期", "冬天", "春天", "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月",
			"十二月", "时候"));

	public final Set<String> titleWords = new HashSet<String>(Arrays.asList("总统", "总理", "顾问", "部长", "市长", "省长", "先生",
			"外长", "教授", "副总理", "副总统", "大使", "同志", "王妃", "国王", "主席", "王后", "王子", "首相", "经理", "秘书", "女士", "总经理"));

	public final Set<String> notResolve = new HashSet<String>(Arrays.asList("这", "那"));
	// 新 旧 前 原
	public final Set<String> newOld = new HashSet<String>(Arrays.asList("新", "旧", "前", "原"));

	public final Set<String> personPronouns = new HashSet<String>();
	public final Set<String> allPronouns = new HashSet<String>();
	public final Set<String> quantifiers = new HashSet<String>(Arrays.asList("not", "every", "any", "none",
			"everything", "anything", "nothing", "all", "enough"));
	public final Set<String> parts = new HashSet<String>(Arrays.asList("不少", "很多", "一些", "有些", "部分", "多数", "少数", "更多",
			"更少", "所有", "一个", "hundred", "thousand", "million", "billion", "tens", "dozens", "hundreds", "thousands",
			"millions", "billions", "group", "groups", "bunch", "number", "numbers", "pinch", "amount", "amount",
			"total", "all", "mile", "miles", "pounds"));
	public final Set<String> nonWords = new HashSet<String>(Arrays.asList("mm", "hmm", "ahem", "um", "ha", "er"));

	public final HashMap<ArrayList<String>, int[]> bigGenderNumber = new HashMap<ArrayList<String>, int[]>();

	public final HashSet<String> surnames;

	public final Set<String> femalePronouns = new HashSet<String>(Arrays.asList(new String[] { "她", "她们", "herself",
			"she" }));
	public final Set<String> malePronouns = new HashSet<String>(Arrays.asList(new String[] { "他", "他们" }));
	public final Set<String> neutralPronouns = new HashSet<String>(Arrays.asList(new String[] { "它" }));
	public final Set<String> possessivePronouns = new HashSet<String>(Arrays.asList(new String[] { "my", "your", "his",
			"her", "its", "our", "their", "whose" }));

	public final Set<String> pluralWords = new HashSet<String>(Arrays.asList(new String[] { "二", "三", "四", "五", "六",
			"七", "八", "九", "十", "百", "千", "万", "亿", "些", "多", "2", "3", "4", "5", "6", "7", "8", "9", "0", "所有" }));

	public final Set<String> numberWords = new HashSet<String>(Arrays.asList(new String[] { "二", "三", "四", "五", "六",
			"七", "八", "九", "十", "百", "千", "万", "亿", "2", "3", "4", "5", "6", "7", "8", "9", "0" }));

	public final Set<String> singleWords = new HashSet<String>(Arrays.asList(new String[] { "1", "一" }));

	public final Set<String> removeChars = new HashSet<String>(Arrays
			.asList(new String[] { "什么的", "哪", "什么", "谁", "啥", "哪儿", "哪里", "人们", "年", "原因", "啥时", "nothing", "one",
					"other", "plenty", "somebody", "someone", "something", "both", "few", "fewer", "many", "others",
					"several", "all", "any", "more", "most", "none", "some", "such" }));

	public final Set<String> removeWords = new HashSet<String>(Arrays.asList(new String[] { "_", "ｑｕｏｔ", "人", "时候",
			"问题", "情况", "未来", "战争", "可能" }));

	public final Set<String> singularPronouns = new HashSet<String>(Arrays.asList(new String[] { "他", "它", "哪个", "谁",
			"这", "那", "其", "其他", "其它", "那里", "那位", "你", "您", "我", "本身", "俺", "自己", "本人", "她", "此", "这个", "那个",
			"oneself", "one's" }));
	public final Set<String> pluralPronouns = new HashSet<String>(Arrays.asList(new String[] { "你们", "我们", "大家", "咱们",
			"您们", "那些", "这些", "它们", "她们", "他们", "双方", "themselves", "theirs", "their" }));
	public final Set<String> otherPronouns = new HashSet<String>(Arrays.asList(new String[] { "谁", "什么", "哪个", "双方",
			"when", "which" }));
	public final Set<String> thirdPersonPronouns = new HashSet<String>(Arrays.asList(new String[] { "他", "她", "它",
			"他们", "她们", "它们", "这", "那", "这些", "那些", "其", "其他", "其它", "那里", "那位", "they", "them", "themself",
			"themselves", "theirs", "their", "they", "them", "'em", "themselves" }));
	public final Set<String> secondPersonPronouns = new HashSet<String>(Arrays.asList(new String[] { "你", "你们", "您",
			"您们", "yourselves" }));

	public final Set<String> timePronoun = new HashSet<String>(Arrays.asList(new String[] { "当时", "那时", "此时" }));

	public final Set<String> firstPersonPronouns = new HashSet<String>(Arrays.asList(new String[] { "我", "我们", "俺",
			"大家", "本人", "咱们", "ourselves", "ours", "our" }));
	public final Set<String> moneyPercentNumberPronouns = new HashSet<String>(Arrays
			.asList(new String[] { "it", "its" }));
	public final Set<String> dateTimePronouns = new HashSet<String>(Arrays.asList(new String[] { "when" }));
	public final Set<String> organizationPronouns = new HashSet<String>(Arrays.asList(new String[] { "it", "its",
			"they", "their", "them", "which" }));
	public final Set<String> locationPronouns = new HashSet<String>(Arrays.asList(new String[] { "it", "its", "where",
			"here", "there" }));
	public final Set<String> inanimatePronouns = new HashSet<String>(Arrays.asList(new String[] { "它", "它们", "这", "那",
			"这些", "那些", "什么", "哪个", "其它", "那里" }));
	public final Set<String> unknownAnimatePronouns = new HashSet<String>(Arrays.asList(new String[] { "一些", "许多", "其",
			"themselves", "theirs", "their", "they", "them", "themselves", }));
	public final Set<String> animatePronouns = new HashSet<String>(Arrays.asList(new String[] { "他", "你", "你们", "我",
			"她", "她们", "他们", "我们", "你们", "她", "您", "谁", "其他", "本身", "俺", "自己", "大家", "那位", "双方", "本人", "咱们", "her",
			"herself", "hers", "her", "one", "oneself", "one's", "they", "them", "themself", "themselves", "theirs",
			"their", "they", "them", "'em", "themselves", "who", "whom", "whose" }));
	public final Set<String> indefinitePronouns = new HashSet<String>(Arrays.asList(new String[] { "another",
			"anybody", "anyone", "anything", "each", "either", "enough", "everybody", "everyone", "everything", "less",
			"little", "much", "neither", "no one", "nobody", "nothing", "one", "other", "plenty", "somebody",
			"someone", "something", "both", "few", "fewer", "many", "others", "several", "all", "any", "more", "most",
			"none", "some", "such" }));

	public final Set<String> relativePronouns = new HashSet<String>(Arrays.asList(new String[] { "that", "who",
			"which", "whom", "where", "whose" }));
	public final Set<String> GPEPronouns = new HashSet<String>(Arrays.asList(new String[] { "it", "itself", "its",
			"they", "where" }));

	public final Set<String> facilityVehicleWeaponPronouns = new HashSet<String>(Arrays.asList(new String[] { "it",
			"itself", "its", "they", "where" }));
	public final Set<String> miscPronouns = new HashSet<String>(Arrays.asList(new String[] { "it", "itself", "its",
			"they", "where" }));
	public final Set<String> reflexivePronouns = new HashSet<String>(Arrays.asList(new String[] { "自己", "本身",
			"yourselves", "himself", "herself", "itself", "ourselves", "themselves", "oneself" }));
	public final Set<String> transparentNouns = new HashSet<String>(Arrays.asList(new String[] { "bunch", "group",
			"breed", "class", "ilk", "kind", "half", "segment", "top", "bottom", "glass", "bottle", "box", "cup",
			"gem", "idiot", "unit", "part", "stage", "name", "division", "label", "group", "figure", "series",
			"member", "members", "first", "version", "site", "side", "role", "largest", "title", "fourth", "third",
			"second", "number", "place", "trio", "two", "one", "longest", "highest", "shortest", "head", "resident",
			"collection", "result", "last" }));

	public final Set<String> stopWords = new HashSet<String>(Arrays.asList(new String[] { "the", "of", "at", "on",
			"upon", "in", "to", "from", "out", "as", "so", "such", "or", "and", "those", "this", "these", "that",
			"for", ",", "is", "was", "am", "are", "'s", "been", "were" }));
	public final Set<String> notOrganizationPRP = new HashSet<String>(Arrays.asList(new String[] { "i", "me", "myself",
			"mine", "my", "yourself", "he", "him", "himself", "his", "she", "her", "herself", "hers", "here" }));

	public final Set<String> reportVerb = new HashSet<String>(Arrays.asList("表示", "讲起", "说话", "说", "advise", "agree",
			"alert", "allege", "announce", "answer", "apologize", "argue", "ask", "assert", "assure", "beg", "blame",
			"boast", "caution", "charge", "cite", "claim", "clarify", "command", "comment", "compare", "complain",
			"concede", "conclude", "confirm", "confront", "congratulate", "contend", "contradict", "convey", "counter",
			"criticize", "debate", "decide", "declare", "defend", "demand", "demonstrate", "deny", "describe",
			"determine", "disagree", "disclose", "discount", "discover", "discuss", "dismiss", "dispute", "disregard",
			"doubt", "emphasize", "encourage", "endorse", "equate", "estimate", "expect", "explain", "express",
			"extoll", "fear", "feel", "find", "forbid", "forecast", "foretell", "forget", "gather", "guarantee",
			"guess", "hear", "hint", "hope", "illustrate", "imagine", "imply", "indicate", "inform", "insert",
			"insist", "instruct", "interpret", "interview", "invite", "issue", "justify", "learn", "maintain", "mean",
			"mention", "negotiate", "note", "observe", "offer", "oppose", "order", "persuade", "pledge", "point",
			"point out", "praise", "pray", "predict", "prefer", "present", "promise", "prompt", "propose", "protest",
			"prove", "provoke", "question", "quote", "raise", "rally", "read", "reaffirm", "realise", "realize",
			"rebut", "recall", "reckon", "recommend", "refer", "reflect", "refuse", "refute", "reiterate", "reject",
			"relate", "remark", "remember", "remind", "repeat", "reply", "report", "request", "respond", "restate",
			"reveal", "rule", "say", "see", "show", "signal", "sing", "slam", "speculate", "spoke", "spread", "state",
			"stipulate", "stress", "suggest", "support", "suppose", "surmise", "suspect", "swear", "teach", "tell",
			"testify", "think", "threaten", "told", "uncover", "underline", "underscore", "urge", "voice", "vow",
			"warn", "welcome", "wish", "wonder", "worry", "write"));

}
