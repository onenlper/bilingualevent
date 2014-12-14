package entity.coref;

import java.util.ArrayList;
import java.util.HashSet;

import model.ACEDoc;
import model.EntityMention;
import model.EntityMention.Animacy;
import model.EntityMention.Gender;
import model.EntityMention.Numb;
import model.ParseResult;
import util.Common;
import util.Common.Feature;
import util.YYFeature;

public class ACECorefFeature extends YYFeature {

	EntityMention current;

	EntityMention candidate;

	ACEDoc doc;

	ArrayList<ParseResult> parseResults;

	public ACECorefFeature(boolean train, String name) {
		super(train, name);
	}
	
	public void configure(EntityMention ant, EntityMention anaphor, ACEDoc sgm) {
		this.current = anaphor;
		this.candidate = ant;
		this.doc = sgm;
		this.parseResults = sgm.parseReults;
	}

	public void calculate(boolean train) {
		
		// 25 MAXIMALNP
//		if (!ACECorefCommon2.isMaximalNP(this.candidate, this.current, this.plainText, parseResults)) {
//			this.feat[24] = 0;
//		} else {
//			this.feat[24] = 1;
//		}
//		
//		// 29 COPULAR
//		if (ACECorefCommon2.isCopular(this.candidate, this.current, this.plainText, parseResults)) {
//			this.feat[28] = 0;
//		} else {
//			this.feat[28] = 1;
//		}
//		// 30 SEMCLASS
//
//		// new feature
//		if (ACECorefCommon.isRoleAppositive(this.candidate, this.current, this.plainText, parseResults)) {
//			this.feat[38] = 0;
//			System.out.println(this.candidate + " role " + this.current);
//		} else {
//			this.feat[38] = 1;
//		}

	}

	@Override
	public ArrayList<Feature> getCategoryFeatures() {
		ArrayList<Feature> features = new ArrayList<Feature>();
		String canStr = candidate.getContent();
		String curStr = current.getContent();

		String canHead = candidate.head;
		String curHead = current.head;
		// 1 PRONOUN_1
		if (this.candidate.isPronoun) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 2 subject_1
		if (this.candidate.isSub) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 3 nested_1
		if (this.candidate.isNNP) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// 4 number_2
		features.add(new Feature(this.current.number.ordinal(), Numb.values().length));
		
		// 5 gender_2
		features.add(new Feature(this.current.gender.ordinal(), Gender.values().length));
		// this.feat[4] = -2;

		// 6 pronoun_2
		if (Common.isPronoun(curStr)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// 7 nested_2
		if (this.current.isNNP) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		features.add(new Feature(this.current.animacy.ordinal(), Animacy.values().length));

		// 12 string_match
		if (curStr.equals(canStr)) {
			features.add(new Feature(0, 7));
		} // left
		else if (canStr.startsWith(curStr) || curStr.startsWith(canStr)) {
			features.add(new Feature(1, 7));
		} // right
		else if (canStr.endsWith(curStr) || curStr.endsWith(canStr)) {
			features.add(new Feature(2, 7));
		} // contain
		else if (canStr.contains(curStr) || curStr.contains(canStr)) {
			features.add(new Feature(3, 7));
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
				features.add(new Feature(4, 7));
			} else if (part) {
				features.add(new Feature(5, 7));
			} else {
				features.add(new Feature(6, 7));
			}
		}
		
		if (this.current.subType.equals("OTHER") || this.candidate.subType.equals("OTHER")) {
			features.add(new Feature(0, 3));
		} else if (this.current.subType.equalsIgnoreCase(this.candidate.subType)) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		int editDistance = Common.getEditDistance(canStr, curStr);
		if (editDistance > 7) {
			editDistance = 7;
		}
		features.add(new Feature(editDistance, 8));
		
		if (curStr.equals(canStr) && !this.current.ner.equalsIgnoreCase("OTHER")
				&& !this.candidate.ner.equalsIgnoreCase("OTHER")) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		// 16 NONPRO_STR_MATCH
		if (!Common.isPronoun(curStr) && !Common.isPronoun(canStr) && curStr.equals(canStr)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
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
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
		} else {
			features.add(new Feature(2, 3));
		}
		
		
		// 18 PRO_TYPE_MATCH
		if (this.candidate.isPronoun && this.current.isPronoun) {
			if (canStr.equals(curStr)) {
				features.add(new Feature(0, 3));
			} else {
				features.add(new Feature(1, 3));
			}
		} else {
			features.add(new Feature(2, 3));
		}
		
		// 19 NUMBER AGREEMENT
		if (this.candidate.number == this.current.number && this.current.number != Numb.UNKNOWN) {
			features.add(new Feature(0, 3));
		} else if (this.candidate.number == Numb.UNKNOWN || this.current.number == Numb.UNKNOWN ) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 20 GENDER AGREEMENT
		if (this.candidate.gender == this.current.gender && this.current.gender != Gender.UNKNOWN) {
			features.add(new Feature(0, 3));
		} else if (this.candidate.gender == Gender.UNKNOWN || this.current.gender == Gender.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}

		// 21 AGREEMENT
		if (this.candidate.number != Numb.UNKNOWN && this.current.number != Numb.UNKNOWN && this.candidate.gender != Gender.UNKNOWN
				&& this.current.gender != Gender.UNKNOWN) {
			if (this.candidate.number == this.current.number && this.candidate.gender == this.current.gender) {
				features.add(new Feature(0, 4));
			} else if (this.candidate.number != this.current.number && this.candidate.gender != this.current.gender) {
				features.add(new Feature(1, 4));
			} else {
				features.add(new Feature(2, 4));
			}
		} else {
			features.add(new Feature(3, 4));
		}

		// 22 ANIMACY
		if (this.current.animacy == this.candidate.animacy && this.current.animacy!=Animacy.UNKNOWN) {
			features.add(new Feature(0, 3));
		} else if (this.current.animacy == Animacy.UNKNOWN || this.candidate.animacy == Animacy.UNKNOWN) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}
		
		if (Common.isPronoun(curStr) && Common.isPronoun(canStr)) {
			features.add(new Feature(0, 3));
		} else if (!Common.isPronoun(curStr) && !Common.isPronoun(canStr)) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}
		
		// 24 BOTH_PROPER_NOUNS
		if (this.candidate.isProperNoun && this.current.isProperNoun) {
			features.add(new Feature(0, 3));
		} else if (!this.candidate.isProperNoun && !this.current.isProperNoun) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}
		
		if (Common.isAbbreviation(canHead, curHead)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		
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
			features.add(new Feature(0, 4));
		} else if (lowcase) {
			features.add(new Feature(1, 4));
		} else if (digit) {
			features.add(new Feature(2, 4));
		} else {
			features.add(new Feature(3, 4));
		}

		// 36 head_match
		if (canStr.charAt(canStr.length() - 1) == curStr.charAt(curStr.length() - 1)) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}

		// modifier
		if (this.candidate.headStart == this.current.headStart || this.candidate.headEnd > this.current.headEnd) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		// head
		if (this.candidate.headEnd == this.current.headEnd) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
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
			features.add(new Feature(0, 6));
		} else if (simChar >= 0.6 && simChar < 0.8) {
			features.add(new Feature(1, 6));
		} else if (simChar >= 0.4 && simChar < 0.6) {
			features.add(new Feature(2, 6));
		} else if (simChar >= 0.2 && simChar < 0.4) {
			features.add(new Feature(3, 6));
		} else if (simChar >= 0 && simChar < 0.2) {
			features.add(new Feature(4, 6));
		} else {
			features.add(new Feature(5, 6));
		}
		
		// 28 length ratio
		double ratio = 0;
		if (canStr.length() >= curStr.length()) {
			ratio = (double) curStr.length() / (double) canStr.length();
		} else {
			ratio = (double) canStr.length() / (double) curStr.length();
		}
		if (ratio >= 0.8 && ratio <= 1) {
			features.add(new Feature(0, 6));
		} else if (ratio >= 0.6 && ratio < 0.8) {
			features.add(new Feature(1, 6));
		} else if (ratio >= 0.4 && ratio < 0.6) {
			features.add(new Feature(2, 6));
		} else if (ratio >= 0.2 && ratio < 0.4) {
			features.add(new Feature(3, 6));
		} else if (ratio >= 0 && ratio < 0.2) {
			features.add(new Feature(4, 6));
		} else {
			features.add(new Feature(5, 6));	
		}
		
		// 33 ner equal
		if (this.current.ner.equalsIgnoreCase("OTHER") || this.candidate.ner.equalsIgnoreCase("OTHER")) {
			features.add(new Feature(0, 3));
		} else if (this.current.ner.equalsIgnoreCase(this.candidate.ner)) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}
		
		if (this.current.semClass.equals(this.candidate.semClass) && !this.current.semClass.equals("OTHER")) {
			features.add(new Feature(0, 3));
		} else if (this.current.semClass.equals("OTHER") || this.candidate.semClass.equals("OTHER")) {
			features.add(new Feature(1, 3));
		} else {
			features.add(new Feature(2, 3));
		}
		
		// 32 sentence DISTANCE
		int[] position1 = doc.positionMap.get(this.candidate.headStart);
		int[] position2 = doc.positionMap.get(this.current.headStart);
		
		int sentence1 = position1[0];
		int sentence2 = position2[0];
		double sentenceDiff = Math.abs(sentence1 - sentence2);
		int dif = (int) Math.ceil(Math.log(sentenceDiff + 1) / Math.log(2));
		if (dif > 5) {
			dif = 5;
		}
		features.add(new Feature(dif, 6));

		
		// 26 SPAN
		if (this.current.getS() > this.candidate.getS() && this.current.getE() < this.candidate.getE()) {
			features.add(new Feature(0, 2));
		} else {
			features.add(new Feature(1, 2));
		}
		
		// 27 Character distance
		double charDiff = Math.abs(this.current.headStart - this.candidate.headEnd);
		dif = (int) Math.ceil(Math.log(charDiff + 1) / Math.log(2));
		if (dif > 10) {
			dif = 10;
		}
		features.add(new Feature(dif, 11));
		
		
		// 31 Semantic distance
		String sem1[] = Common.getSemantic(this.candidate.head);
		// System.out.println(this.current.head + " " + curLastWord + " " +
		// sem1);
		String sem2[] = Common.getSemantic(this.current.head);
		if (sem1==null || sem2==null) {
			features.add(new Feature(9, 10));
		} else {
			int same = 0;
			for (; same < sem1[0].length(); same++) {
				if (sem1[0].charAt(same) != sem2[0].charAt(same)) {
					break;
				}
			}
			features.add(new Feature(same, 10));
		}
		
		
		
		// TODO
		return features;
	}
	
	@Override
	public ArrayList<String> getStrFeatures() {
		
		String canStr = candidate.getContent();
		String curStr = current.getContent();

		String canHead = candidate.head;
		String curHead = current.head;
		
		canStr = canHead;
		curStr = curHead;
		
		ArrayList<String> strs = new ArrayList<String>();
		strs.add(this.current.semClass);
		strs.add(this.current.ner);
		strs.add(this.current.subType);
		
		// TODO
		strs.add(this.current.subType + "#" + this.candidate.subType);
		

		String concat = canStr + "_" + curStr;
		concat = concat.replace("\n", "").replace("\r", "");
		strs.add(concat);
				
		// ner relation feature
		String nerRelation = this.current.ner.toLowerCase() + "_" + this.candidate.ner.toLowerCase();
		strs.add(nerRelation);
		
		// 37 between words
		String content = this.doc.content;
		String between = "###";
		if (current.getS() - candidate.getE() <= 3 && current.getS() - candidate.getE() >= 1) {
			between = content.substring(candidate.getE() + 1, current.getS());
			between = between.replace("\n", "").replace("\r", "");
		}
		strs.add(between);
		return strs;
	}

}
