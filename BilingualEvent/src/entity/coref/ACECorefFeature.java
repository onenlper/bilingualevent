package entity.coref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.EntityMention;
import model.ParseResult;
import util.Common;

public class ACECorefFeature {

	public static int maxLone[] = { 3, 3, 2, 2, 8, 3, 19, 47 };
	public static int maxDiff[] = { 2, 2, 2, 3, 3, 2, 2, 8, 3, 19, 47, 7, 3, 8, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 11,
			5, 2, 3, 10, 6, 3, 4, 5, 7, 2
			/* modifier and head */, 2, 2, 2 };

	int feat[] = new int[44];

	public static HashMap<String, Integer> subType_10;
	public static HashMap<String, Integer> ner_relation_45 = new HashMap<String, Integer>();
	public static HashMap<String, Integer> subType_relation_39 = new HashMap<String, Integer>();

	public static HashMap<String, Integer> between_39;
	public static HashMap<String, Integer> cancat_ngram;
	public static HashMap<String, Integer> semClass_2_8;

	public static boolean semFea = false;
	private static int semFeaIdx[] = { 7, 8, 9, 10, 12, 21, 29, 30, 32, 40, 42 };

	public static void init(boolean train) {
		if (!semFea) {
			maxLone[4] = 0;
			maxLone[5] = 0;
			maxLone[6] = 0;
			maxLone[7] = 0;
			for (int idx : semFeaIdx) {
				if (idx < maxDiff.length) {
					maxDiff[idx] = 0;
				}
			}
		}
		subType_10 = new HashMap<String, Integer>();
		semClass_2_8 = new HashMap<String, Integer>();
		if (train) {
			cancat_ngram = new HashMap<String, Integer>();
			between_39 = new HashMap<String, Integer>();
		} else {
			between_39 = Common.readFile2Map("ace_between_words");
			cancat_ngram = Common.readFile2Map("ace_cancat_ngram");
		}

		String subTypes[] = { "f-airport", "f-building-grounds", "f-path", "f-plant", "f-subarea-facility",
				"g-continent", "g-county-or-district", "g-gpe-cluster", "g-nation", "g-population-center", "g-special",
				"g-state-or-province", "l-address", "l-boundary", "l-celestial", "l-land-region-natural",
				"l-region-general", "l-region-international", "l-water-body", "o-commercial", "o-educational",
				"o-entertainment", "o-government", "o-media", "o-medical-science", "o-non-governmental", "o-religious",
				"o-sports", "p-group", "p-indeterminate", "p-individual", "v-air", "v-land", "v-subarea-vehicle",
				"v-underspecified", "v-water", "w-biological", "w-blunt", "w-chemical", "w-exploding", "w-nuclear",
				"w-projectile", "w-sharp", "w-shooting", "w-underspecified", "o-other" };
		for (String sub1 : subTypes) {
			for (String sub2 : subTypes) {
				String str = sub1 + "_" + sub2;
				if (!subType_relation_39.containsKey(str)) {
					int i = subType_relation_39.size();
					subType_relation_39.put(str, i);
				}
			}
		}
		for (String sub : subTypes) {
			String str = sub;
			if (!subType_10.containsKey(str)) {
				int i = subType_10.size();
				subType_10.put(str, i);
			}
		}

		String ners[] = { "CARDINAL", "DATE", "EVENT", "FAC", "GPE", "LAW", "LOC", "MONEY", "NORP", "ORDINAL", "ORG",
				"PERCENT", "PERSON", "PRODUCT", "QUANTITY", "TIME", "WORKOFART", "LANGUAGE", "OTHER" };
		for (String ner1 : ners) {
			for (String ner2 : ners) {
				String str = ner1.toLowerCase() + "_" + ner2.toLowerCase();
				if (!ner_relation_45.containsKey(str)) {
					int i = ner_relation_45.size();
					ner_relation_45.put(str, i);
				}
			}
		}

		String sems[] = { "fac", "gpe", "loc", "org", "per", "veh", "wea", "other" };
		for (String sem : sems) {
			String str = sem;
			if (!semClass_2_8.containsKey(str)) {
				int i = semClass_2_8.size();
				semClass_2_8.put(str, i);
			}
		}
	}

	EntityMention current;

	EntityMention candidate;

	ACEDoc doc;

	ArrayList<ParseResult> parseResults;

	public ACECorefFeature(EntityMention[] pair, ACEDoc sgm, ArrayList<ParseResult> parseResults) {
		this.current = pair[0];
		this.candidate = pair[1];
		this.doc = sgm;
		this.parseResults = parseResults;
	}

	public int[] getFeature() {
		return this.feat;
	}

	public void calculate(boolean train) {
		String canStr = candidate.getContent();
		String curStr = current.getContent();

		String canHead = candidate.head;
		String curHead = current.head;
		// 1 PRONOUN_1
		if (this.candidate.isPronoun) {
			this.feat[0] = 0;
		} else {
			this.feat[0] = 1;
		}
		// 2 subject_1
		if (this.candidate.isSub) {
			this.feat[1] = 0;
		} else {
			this.feat[1] = 1;
		}
		// 3 nested_1
		if (this.candidate.isNNP) {
			this.feat[2] = 0;
		} else {
			this.feat[2] = 1;
		}

		// 4 number_2
		this.feat[3] = this.current.number.ordinal();
		// this.feat[3] = -2;

		// 5 gender_2
		this.feat[4] = this.current.gender.ordinal();
		// this.feat[4] = -2;

		// 6 pronoun_2
		if (Common.isPronoun(curStr)) {
			this.feat[5] = 0;
		} else {
			this.feat[5] = 1;
		}
		// 7 nested_2
		if (this.current.isNNP) {
			this.feat[6] = 0;
		} else {
			this.feat[6] = 1;
		}

		// 8 semclass_2
		String sem = this.current.semClass.toLowerCase();
		this.feat[7] = semClass_2_8.get(sem);

		// this.feat[7] = -2;

		// 9 animacy_2
		this.feat[8] = this.current.animacy.ordinal();

		// ner feature
		if (this.current.ner.equals("CARDINAL")) {
			feat[9] = 0;
		} else if (this.current.ner.equals("DATE")) {
			feat[9] = 1;
		} else if (this.current.ner.equals("EVENT")) {
			feat[9] = 2;
		} else if (this.current.ner.equals("FAC")) {
			feat[9] = 3;
		} else if (this.current.ner.equals("GPE")) {
			feat[9] = 4;
		} else if (this.current.ner.equals("LAW")) {
			feat[9] = 5;
		} else if (this.current.ner.equals("LOC")) {
			feat[9] = 6;
		} else if (this.current.ner.equals("MONEY")) {
			feat[9] = 7;
		} else if (this.current.ner.equals("NORP")) {
			feat[9] = 8;
		} else if (this.current.ner.equals("ORDINAL")) {
			feat[9] = 9;
		} else if (this.current.ner.equals("ORG")) {
			feat[9] = 10;
		} else if (this.current.ner.equals("PERCENT")) {
			feat[9] = 11;
		} else if (this.current.ner.equals("PERSON")) {
			feat[9] = 12;
		} else if (this.current.ner.equals("PRODUCT")) {
			feat[9] = 13;
		} else if (this.current.ner.equals("QUANTITY")) {
			feat[9] = 14;
		} else if (this.current.ner.equals("TIME")) {
			feat[9] = 15;
		} else if (this.current.ner.equals("WORKOFART")) {
			feat[9] = 16;
		} else if (this.current.ner.equals("LANGUAGE")) {
			feat[9] = 17;
		} else {
			feat[9] = 18;
		}

		// this.feat[9] = -2;

		// subtype feature
		String subType = this.current.subType.toLowerCase();
		// System.out.println(subType);
		this.feat[10] = subType_10.get(subType);

		// this.feat[10] = -2;

		// 12 string_match
		if (curStr.equals(canStr)) {
			this.feat[11] = 0;
		} // left
		else if (canStr.startsWith(curStr) || curStr.startsWith(canStr)) {
			this.feat[11] = 1;
//			System.out.print(this.candidate + " " + this.current);
//			if(this.candidate.entityIndex==this.current.entityIndex) {
//				right ++;
//				System.out.println(" true");
//			} else {
//				wrong ++;
//				System.out.println(" wrong");
//			}
		} // right
		else if (canStr.endsWith(curStr) || curStr.endsWith(canStr)) {
			this.feat[11] = 2;
		} // contain
		else if (canStr.contains(curStr) || curStr.contains(canStr)) {
			this.feat[11] = 3;
		} // part contain
		else {
			int length1 = canStr.length();
			int length2 = curStr.length();
			boolean alias = true;
			boolean part = false;
			if (length1 <= length2) {
				for (int i = 0; i < length1; i++) {
					int pos = curStr.indexOf(canStr.charAt(i));
					if (pos == -1) {
						alias = false;
						break;
					}
					part = true;
				}
			} else {
				for (int i = 0; i < length2; i++) {
					int pos = canStr.indexOf(curStr.charAt(i));
					if (pos == -1) {
						alias = false;
						break;
					}
					part = true;
				}
			}
			if (alias) {
				this.feat[11] = 4;
			} else if (part) {
				this.feat[11] = 5;
			} else {
				this.feat[11] = 6;
			}
		}

		// 13 subtype equal
		if (this.current.subType.equals("OTHER") || this.candidate.subType.equals("OTHER")) {
			this.feat[12] = 2;
		} else if (this.current.subType.equalsIgnoreCase(this.candidate.subType)) {
			// if(this.current.isProperNoun && this.candidate.isProperNoun &&
			// (this.feat[11] == 6)) {
			// this.feat[12] = 3;
			// } else {
			this.feat[12] = 0;
			// }
		} else {
			this.feat[12] = 1;
		}

		// this.feat[12] = -2;

		// 14 edit distance
		this.feat[13] = Common.getEditDistance(canStr, curStr);
		if (this.feat[13] > 7) {
			this.feat[13] = 7;
		}

		// 15 PN_STR_MATCH
		if (curStr.equals(canStr) && !this.current.ner.equalsIgnoreCase("OTHER")
				&& !this.candidate.ner.equalsIgnoreCase("OTHER")) {
			this.feat[14] = 0;
		} else {
			this.feat[14] = 1;
		}

		// 16 NONPRO_STR_MATCH
		if (!Common.isPronoun(curStr) && !Common.isPronoun(canStr) && curStr.equals(canStr)) {
			this.feat[15] = 0;
		} else {
			this.feat[15] = 1;
		}

		// 17 MODIFIER_MATCH
		if (this.candidate.modifyList.size() != 0 && this.current.modifyList.size() != 0) {
			boolean modiferCompatible = true;
			ArrayList<String> curModifiers = this.current.modifyList;
			ArrayList<String> canModifiers = this.candidate.modifyList;
			HashSet<String> curModifiersHash = new HashSet<String>();
			curModifiersHash.addAll(curModifiers);
			HashSet<String> canModifiersHash = new HashSet<String>();
			canModifiersHash.addAll(canModifiers);
			for (String canModifier : canModifiers) {
				if (!curModifiersHash.contains(canModifier)) {
					modiferCompatible = false;
					break;
				}
			}
			for (String curModifier : curModifiers) {
				if (!canModifiersHash.contains(curModifier)) {
					modiferCompatible = false;
					break;
				}
			}
			if(modiferCompatible) {
				this.feat[16] = 0;
			} else {
				this.feat[16] = 1;
			}
		} else {
			this.feat[16] = 2;
		}
		// 18 PRO_TYPE_MATCH
		if (this.candidate.isPronoun && this.current.isPronoun) {
			if (canStr.equals(curStr)) {
				this.feat[17] = 0;
			} else {
				this.feat[17] = 1;
			}
		} else {
			this.feat[17] = 2;
		}

		// 19 NUMBER AGREEMENT
		if (this.candidate.number == this.current.number) {
			this.feat[18] = 0;
		} else {
			this.feat[18] = 1;
		}

		// 20 GENDER AGREEMENT
		if (this.candidate.gender == this.current.gender) {
			this.feat[19] = 0;
		} else {
			this.feat[19] = 1;
		}
		// this.feat[19] = -2;
		// 21 AGREEMENT
			if (this.candidate.number == this.current.number && this.candidate.gender == this.current.gender) {
				this.feat[20] = 0;
			} else if (this.candidate.number != this.current.number && this.candidate.gender != this.current.gender) {
				this.feat[20] = 1;
			} else {
				this.feat[20] = 2;
			}

		// 22 ANIMACY
		if (this.current.animacy == this.candidate.animacy) {
			this.feat[21] = 0;
		} else {
			this.feat[21] = 2;
		}

		// 23 BOTH_PRONOUNS
		if (Common.isPronoun(curStr) && Common.isPronoun(canStr)) {
			this.feat[22] = 0;
		} else if (!Common.isPronoun(curStr) && !Common.isPronoun(canStr)) {
			this.feat[22] = 1;
		} else {
			this.feat[22] = 2;
		}
		// 24 BOTH_PROPER_NOUNS
		if (this.candidate.isProperNoun && this.current.isProperNoun) {
			this.feat[23] = 0;
		} else if (!this.candidate.isProperNoun && !this.current.isProperNoun) {
			this.feat[23] = 1;
		} else {
			this.feat[23] = 2;
		}

		// 25 MAXIMALNP
//		if (!ACECorefCommon2.isMaximalNP(this.candidate, this.current, this.doc, parseResults)) {
//			this.feat[24] = 0;
//		} else {
//			this.feat[24] = 1;
//		}
		// 26 SPAN
		if (this.current.getS() > this.candidate.getS() && this.current.getE() < this.candidate.getE()) {
			this.feat[25] = 0;
		} else {
			this.feat[25] = 1;
		}
		// 27 Character distance
		int[] position1 = doc.positionMap.get(this.candidate.headStart);
		int[] position2 = doc.positionMap.get(this.candidate.headEnd);
		double charDiff = Math.abs(this.current.getS() - this.candidate.getS());
		this.feat[26] = (int) Math.ceil(Math.log(charDiff + 1) / Math.log(2));
		if (this.feat[26] > 10) {
			this.feat[26] = 10;
		}

		// 28 length ratio
		double ratio = 0;
		if (canStr.length() >= curStr.length()) {
			ratio = (double) curStr.length() / (double) canStr.length();
		} else {
			ratio = (double) canStr.length() / (double) curStr.length();
		}
		if (ratio >= 0.8 && ratio <= 1) {
			this.feat[27] = 0;
		} else if (ratio >= 0.6 && ratio < 0.8) {
			this.feat[27] = 1;
		} else if (ratio >= 0.4 && ratio < 0.6) {
			this.feat[27] = 2;
		} else if (ratio >= 0.2 && ratio < 0.4) {
			this.feat[27] = 3;
		} else if (ratio >= 0 && ratio < 0.2) {
			this.feat[27] = 4;
		}

		// 29 COPULAR
//		if (ACECorefCommon2.isCopular(this.candidate, this.current, this.doc, parseResults)) {
//			this.feat[28] = 0;
//		} else {
//			this.feat[28] = 1;
//		}
		// 30 SEMCLASS
		if (this.current.semClass.equals(this.candidate.semClass) && !this.current.semClass.equals("OTHER")) {
			// if(this.current.isProperNoun && this.candidate.isProperNoun &&
			// (this.feat[11] == 6)) {
			// this.feat[29] = 3;
			// } else {
			this.feat[29] = 0;
			// }
		} else if (this.current.semClass.equals("OTHER") || this.candidate.semClass.equals("OTHER")) {
			this.feat[29] = 2;
		} else {
			this.feat[29] = 1;
		}

		// this.feat[29] = -2;

		// 31 Semantic distance
		int maxSame = 0;
		String canLastWord = parseResults.get(position1[0]).words.get(position1[3]).replace("\n", "");
		String curLastWord = parseResults.get(position2[0]).words.get(position2[3]).replace("\n", "");
//		String sem1 = ACECorefCommon.getSemanticSymbol(this.current, curLastWord);
//		// System.out.println(this.current.head + " " + curLastWord + " " +
//		// sem1);
//		String sem2 = ACECorefCommon.getSemanticSymbol(this.candidate, canLastWord);
//		if (sem1.isEmpty() || sem2.isEmpty()) {
//			this.feat[30] = 9;
//		} else {
//			int same = 0;
//			for (; same < sem1.length(); same++) {
//				if (sem1.charAt(same) != sem2.charAt(same)) {
//					break;
//				}
//			}
//			maxSame = same;
//			this.feat[30] = maxSame;
//		}

		// 32 sentence DISTANCE
		int sentence1 = position1[0];
		int sentence2 = position2[0];
		double sentenceDiff = Math.abs(sentence1 - sentence2);
		this.feat[31] = (int) Math.ceil(Math.log(sentenceDiff + 1) / Math.log(2));
		if (this.feat[31] > 5) {
			this.feat[31] = 5;
		}

		// 33 ner equal
		if (this.current.ner.equalsIgnoreCase("OTHER") || this.candidate.ner.equalsIgnoreCase("OTHER")) {
			this.feat[32] = 2;
		} else if (this.current.ner.equalsIgnoreCase(this.candidate.ner)) {
			this.feat[32] = 0;
		} else {
			this.feat[32] = 1;
		}

		// this.feat[32] = -2;

		// 34 other features
		String whole = curStr + canStr;
		boolean upcase = true;
		boolean lowcase = true;
		boolean digit = true;
		for (int i = 0; i < whole.length(); i++) {
			char c = whole.charAt(i);
			if (!(c >= '0' && c <= '9')) {
				digit = false;
			}
			if (!(c >= 'a' && c <= 'z')) {
				lowcase = false;
			}
			if (!(c >= 'A' && c <= 'Z')) {
				upcase = false;
			}
		}
		if (upcase) {
			feat[33] = 0;
		} else if (lowcase) {
			feat[33] = 1;
		} else if (digit) {
			feat[33] = 2;
		} else {
			feat[33] = 3;
		}

		// 35 word similarity
		int sameChar = 0;
		for (int i = 0; i < canStr.length(); i++) {
			char c = canStr.charAt(i);
			if (curStr.indexOf(c) >= 0) {
				sameChar++;
			}
		}
		double simChar = ((double) 2 * sameChar) / ((double) canStr.length() + curStr.length());
		if (simChar >= 0.8 && simChar <= 1) {
			this.feat[34] = 0;
		} else if (simChar >= 0.6 && simChar < 0.8) {
			this.feat[34] = 1;
		} else if (simChar >= 0.4 && simChar < 0.6) {
			this.feat[34] = 2;
		} else if (simChar >= 0.2 && simChar < 0.4) {
			this.feat[34] = 3;
		} else if (simChar >= 0 && simChar < 0.2) {
			this.feat[34] = 4;
		}

		// 36 head_match
		if (canStr.charAt(canStr.length() - 1) == curStr.charAt(curStr.length() - 1)) {
			this.feat[35] = 0;
		} else {
			this.feat[35] = 1;
		}

		// modifier
		if (this.candidate.headStart == this.current.headStart || this.candidate.headEnd > this.current.headEnd) {
			this.feat[36] = 0;
			// System.out.println("modifier: " + this.candidate + " " +
			// this.current);
		} else {
			this.feat[36] = 1;
		}
		// head
		if (this.candidate.headEnd == this.current.headEnd) {
			this.feat[37] = 0;
			// System.out.println("head: " + this.candidate + " " +
			// this.current);
		} else {
			this.feat[37] = 1;
		}

		// new feature
//		if (ACECorefCommon.isRoleAppositive(this.candidate, this.current, this.doc, parseResults)) {
//			this.feat[38] = 0;
//			System.out.println(this.candidate + " role " + this.current);
//		} else {
//			this.feat[38] = 1;
//		}

		if (Common.isAbbreviation(canHead, curHead)) {
			this.feat[39] = 0;
		} else {
			this.feat[39] = 1;
		}

		// ner relation feature
		String nerRelation = this.current.ner.toLowerCase() + "_" + this.candidate.ner.toLowerCase();
		this.feat[40] = ner_relation_45.get(nerRelation);

		// 37 between words
		String content = this.doc.content;
		String between = "";
		if (current.getS() - candidate.getE() <= 3 && current.getS() - candidate.getE() >= 1) {
			between = content.substring(candidate.getE() + 1, current.getS());
			between = between.replace("\n", "").replace("\r", "");
			if (train) {
				if (between_39.containsKey(between)) {
					this.feat[41] = between_39.get(between);
				} else {
					this.feat[41] = between_39.size();
					between_39.put(between, this.feat[41]);
				}
			} else {
				if (between_39.containsKey(between)) {
					this.feat[41] = between_39.get(between);
				} else {
					this.feat[41] = -1;
				}
			}
		} else {
			this.feat[41] = -1;
		}

		// 38 subtype relation feature
		String subtypeRelaton = this.current.subType.toLowerCase() + "_" + this.candidate.subType.toLowerCase();
		this.feat[42] = subType_relation_39.get(subtypeRelaton);

		// 39 ngram_concate
		canStr = canHead;
		curStr = curHead;
		String concat = canStr + "_" + curStr;
		concat = concat.replace("\n", "").replace("\r", "");
		if (train) {
			if (cancat_ngram.containsKey(concat)) {
				this.feat[43] = cancat_ngram.get(concat);
			} else {
				this.feat[43] = cancat_ngram.size();
				cancat_ngram.put(concat, this.feat[43]);
			}
		} else {
			if (cancat_ngram.containsKey(concat)) {
				this.feat[43] = cancat_ngram.get(concat);
			} else {
				this.feat[43] = -1;
			}
		}

		if (!semFea) {
			for (int idx : semFeaIdx) {
				this.feat[idx] = -1;
			}
		}
		//		
		// // subtype relation feature
		// String subtypeRelaton = this.current.subType.toLowerCase() + "_" +
		// this.candidate.subType.toLowerCase();
		// this.feat[45] = subType_relation_46.get(subtypeRelaton);
	}
public static int right =0;
public static int wrong =0;
	// int PRONOUN_1;
	// int SUBJECT_1;
	// int NESTED_1;
	//	
	// int NUMBER_2;
	// int GENDER_2;
	// int PRONOUN_2;
	// int NESTED_2;
	// int SEMCLASS_2;
	// int ANIMACY_2;
	// int PRO_TYPE_2;
	//	
	// int HEAD_MATCH;
	// int STR_MATCH;
	// int SUBSTR_MATCH;
	// int PRO_STR_MATCH;
	// int PN_STR_MATCH;
	// int NONPRO_STR_MATCH;
	// int MODIFIER_MATCH;
	// int PRO_TYPE_MATCH;
	// int NUMBER;
	// int GENDER;
	// int AGREEMENT;
	// int ANIMACY;
	// int BOTH_PRONOUNS;
	// int BOTH_PROPER_NOUNS;
	// int MAXIMALNP;
	// int SPAN;
	// int INDEFINITE;
	// int APPOSITIVE;
	// int COPULAR;
	// int SEMCLASS;
	// int ALIAS;
	// int DISTANCE;
	//	
	// int NUMBER_;
	// int GENDER_;
	// int PRONOUN_;
	// int NESTED_;
	// int SEMCLASS_;
	// int ANIMACY_;
	// int PRO_TYPE_;

}
