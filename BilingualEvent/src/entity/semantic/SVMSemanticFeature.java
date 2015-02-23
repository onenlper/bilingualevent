package entity.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class SVMSemanticFeature {
	static String subTypes[] = { "f-airport", "f-building-grounds", "f-path",
			"f-plant", "f-subarea-facility", "g-continent",
			"g-county-or-district", "g-gpe-cluster", "g-nation",
			"g-population-center", "g-special", "g-state-or-province",
			"l-address", "l-boundary", "l-celestial", "l-land-region-natural",
			"l-region-general", "l-region-international", "l-water-body",
			"o-commercial", "o-educational", "o-entertainment", "o-government",
			"o-media", "o-medical-science", "o-non-governmental",
			"o-religious", "o-sports", "p-group", "p-indeterminate",
			"p-individual", "v-air", "v-land", "v-subarea-vehicle",
			"v-underspecified", "v-water", "w-biological", "w-blunt",
			"w-chemical", "w-exploding", "w-nuclear", "w-projectile",
			"w-sharp", "w-shooting", "w-underspecified", "o-other" };

	public static String types[] = { "wea", "veh", "per", "fac", "gpe", "loc",
			"org" };

	static String ners[] = { "CARDINAL", "DATE", "EVENT", "FAC", "GPE", "LAW",
			"LOC", "MONEY", "NORP", "ORDINAL", "ORG", "PERCENT", "PERSON",
			"PRODUCT", "QUANTITY", "TIME", "WORKOFART", "LANGUAGE", "OTHER" };

	public static void init() {
		Common.loadSemanticDic();
		subTypes2 = new HashMap<String, String[]>();
		String str1[] = { "f-airport", "f-building-grounds", "f-path",
				"f-plant", "f-subarea-facility", };
		String str2[] = { "g-continent", "g-county-or-district",
				"g-gpe-cluster", "g-nation", "g-population-center",
				"g-special", "g-state-or-province", };
		String str3[] = { "l-address", "l-boundary", "l-celestial",
				"l-land-region-natural", "l-region-general",
				"l-region-international", "l-water-body", };
		String str4[] = { "o-commercial", "o-educational", "o-entertainment",
				"o-government", "o-media", "o-medical-science",
				"o-non-governmental", "o-religious", "o-sports", };
		String str5[] = { "p-group", "p-indeterminate", "p-individual", };
		String str6[] = { "v-air", "v-land", "v-subarea-vehicle",
				"v-underspecified", "v-water", };
		String str7[] = { "w-biological", "w-blunt", "w-chemical",
				"w-exploding", "w-nuclear", "w-projectile", "w-sharp",
				"w-shooting", "w-underspecified", };
		subTypes2.put("fac", str1);
		subTypes2.put("gpe", str2);
		subTypes2.put("loc", str3);
		subTypes2.put("org", str4);
		subTypes2.put("per", str5);
		subTypes2.put("veh", str6);
		subTypes2.put("wea", str7);
		loc_suffix
				.addAll(Common
						.getLines("dict/location_suffix"));
	}

	public static HashMap<String, String[]> subTypes2;

	public static HashMap<String, Integer> charFeatures = new HashMap<String, Integer>();
	public static HashMap<String, Integer> semDicFeatures = new HashMap<String, Integer>();

	public static String getTypeIndex(String type) {
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals(type)) {
				return Integer.toString(i + 1) + " ";
			}
		}
		return "";
	}

	public static String getSubTypeIndex(String subType) {
		for (int i = 0; i < subTypes.length; i++) {
			if (subTypes[i].equals(subType)) {
				return Integer.toString(i + 1) + " ";
			}
		}
		return "";
	}

	public static String getSubTypeIndex2(String type, String subType) {
		String subs[] = subTypes2.get(type);
		for (int i = 0; i < subs.length; i++) {
			if (subs[i].equals(subType)) {
				return Integer.toString(i + 1) + " ";
			}
		}
		System.out.println(type + "#" + subType);
		System.exit(1);
		return "";
	}

	public static String semanticFeature(String head, String ner, boolean train,
		String cL2, String cL1, String cR1, String cR2) {
		// String head = "";
		ArrayList<Fea> allFeas = new ArrayList<Fea>();

		ArrayList<FeatureGroup> featureGroups = new ArrayList<FeatureGroup>();
		// length feature
		// FeatureGroup lengthFea = new FeatureGroup(1,2);
		// if(em.head.length()<4) {
		// lengthFea.setIndex(0);
		// } else {
		// lengthFea.setIndex(1);
		// }
		// featureGroups.add(lengthFea);

		FeatureGroup surnameFea = new FeatureGroup(1, 2);
		if (ACECommon.surnames.contains(head.substring(0, 1))) {
			surnameFea.setIndex(0);
		} else {
			surnameFea.setIndex(1);
		}
		featureGroups.add(surnameFea);

		FeatureGroup locationFea = new FeatureGroup(1, 2);
		if (ACECommon.locations.contains(head)) {
			locationFea.setIndex(0);
		} else {
			locationFea.setIndex(1);
		}
		featureGroups.add(locationFea);

		FeatureGroup locationSuffixFea = new FeatureGroup(1, 2);
		if (ACECommon.suffixes.contains(head.substring(head.length() - 1))) {
			locationSuffixFea.setIndex(0);
		} else {
			locationSuffixFea.setIndex(1);
		}
		featureGroups.add(locationSuffixFea);

		FeatureGroup pronounsFea = new FeatureGroup(1, 2);
		if (ACECommon.pronouns.contains(head)) {
			pronounsFea.setIndex(0);
		} else {
			pronounsFea.setIndex(1);
		}
		featureGroups.add(pronounsFea);

		FeatureGroup orgs_intlFea = new FeatureGroup(1, 2);
		if (ACECommon.orgs_intl.contains(head)) {
			orgs_intlFea.setIndex(0);
		} else {
			orgs_intlFea.setIndex(1);
		}
		featureGroups.add(orgs_intlFea);

		FeatureGroup proper_industryFea = new FeatureGroup(1, 2);
		if (ACECommon.proper_industry.contains(head)) {
			proper_industryFea.setIndex(0);
		} else {
			proper_industryFea.setIndex(1);
		}
		featureGroups.add(proper_industryFea);

		FeatureGroup proper_orgFea = new FeatureGroup(1, 2);
		if (ACECommon.proper_org.contains(head)) {
			proper_orgFea.setIndex(0);
		} else {
			proper_orgFea.setIndex(1);
		}
		featureGroups.add(proper_orgFea);

		FeatureGroup proper_otherFea = new FeatureGroup(1, 2);
		if (ACECommon.proper_other.contains(head)) {
			proper_otherFea.setIndex(0);
		} else {
			proper_otherFea.setIndex(1);
		}
		featureGroups.add(proper_otherFea);

		FeatureGroup proper_peopleFea = new FeatureGroup(1, 2);
		if (ACECommon.proper_people.contains(head)) {
			proper_peopleFea.setIndex(0);
		} else {
			proper_peopleFea.setIndex(1);
		}
		featureGroups.add(proper_peopleFea);

		FeatureGroup proper_placeFea = new FeatureGroup(1, 2);
		if (ACECommon.proper_place.contains(head)) {
			proper_placeFea.setIndex(0);
		} else {
			proper_placeFea.setIndex(1);
		}
		featureGroups.add(proper_placeFea);

		FeatureGroup proper_pressFea = new FeatureGroup(1, 2);
		if (ACECommon.proper_press.contains(head)) {
			proper_pressFea.setIndex(0);
		} else {
			proper_pressFea.setIndex(1);
		}
		featureGroups.add(proper_pressFea);

		FeatureGroup who_chinaFea = new FeatureGroup(1, 2);
		if (ACECommon.who_china.contains(head)) {
			who_chinaFea.setIndex(0);
		} else {
			who_chinaFea.setIndex(1);
		}
		featureGroups.add(who_chinaFea);

		FeatureGroup who_intlFea = new FeatureGroup(1, 2);
		if (ACECommon.who_intl.contains(head)) {
			who_intlFea.setIndex(0);
		} else {
			who_intlFea.setIndex(1);
		}
		featureGroups.add(who_intlFea);

		FeatureGroup nerFea = new FeatureGroup(1, 19);
		if (!ner.equalsIgnoreCase("other")) {
			for (int index = 0; index < ners.length; index++) {
				if (ners[index].equalsIgnoreCase(ner)) {
					nerFea.setIndex(index);
					break;
				}
			}
		}
		featureGroups.add(nerFea);
		int offset = 1;
		for (FeatureGroup fg : featureGroups) {
			if (fg.index != -1) {
				Fea fea = new Fea(offset + fg.index, fg.value);
				allFeas.add(fea);
			}
			offset += fg.limit;
		}

		String semantic = getSemanticSymbol(head, ner);
		ArrayList<Fea> semDicFeas = getSemDicFeas(semantic, train, offset);
		allFeas.addAll(semDicFeas);
		offset += 100000;
		ArrayList<Fea> charBaseFeas = getCharBaseFea(head, train, offset, cL2, cL1, cR1, cR2);
		allFeas.addAll(charBaseFeas);

		String str = genFeatureStr(allFeas);
		return str;
	}

	private static ArrayList<Fea> getSemDicFeas(String semantic, boolean train,
			int offset) {
		ArrayList<Fea> feas = new ArrayList<Fea>();
		for (int i = 1; i <= semantic.length(); i++) {
			String subSem = semantic.substring(0, i);
			if (semDicFeatures.containsKey(subSem)) {
				Fea fea = new Fea(semDicFeatures.get(subSem) + offset, 1);
				feas.add(fea);
			} else {
				if (train) {
					int index = semDicFeatures.size() + 1;
					semDicFeatures.put(subSem, index);
					Fea fea = new Fea(index + offset, 1);
					feas.add(fea);
				}
			}
		}
		return feas;
	}

	private static int find = 0;
	private static int miss = 0;
	private static int ner = 0;
	private static int per = 0;
	private static int loc = 0;
	private static int eng = 0;
	public static HashSet<String> loc_suffix = new HashSet<String>();

	private static String getSemanticSymbol(String head, String ner) {
		// String head = em.head;
		if (head.charAt(0) == '副' && head.length()!=1) {
			head = head.substring(1);
		}
		String semantics[] = Common.semanticDic.get(head);
		String semantic = "";
		if (semantics != null) {
			semantic = semantics[0];
			find++;
		} else {
			boolean findNer = !ner.equalsIgnoreCase("OTHER");
			if (ner.equalsIgnoreCase("PERSON")) {
				semantic = "A0000001";
			} else if (ner.equalsIgnoreCase("LOC")) {
				semantic = "Be000000";
			} else if (ner.equalsIgnoreCase("GPE")) {
				semantic = "Di020000";
			} else if (ner.equalsIgnoreCase("ORG")) {
				semantic = "Dm000000";
			} else {
				// System.out.println(ele.content + " " + em.head);
			}
			// System.out.println(ele.content + " " + em.head);
			if (!findNer) {
				if (head.endsWith("们") || head.endsWith("人")
						|| head.endsWith("者") || head.endsWith("哥")
						|| head.endsWith("员") || head.endsWith("弟")
						|| head.endsWith("爸")) {
					per++;
					semantic = "A0000001";
				} else {
					if (loc_suffix.contains(head.substring(head.length() - 1))) {
						// System.out.println("LOC: " + head);
						loc++;
					} else {
						boolean english = true;
						for (int h = 0; h < head.length(); h++) {
							char c = head.charAt(h);
							if (!(c > 0 && c < 127)) {
								english = false;
								break;
							}
						}
						if (!english) {
							miss++;
						} else {
							eng++;
						}
					}
				}
			}
		}
		return semantic;
	}

	private static class FeatureGroup {
		int index;
		int value;
		int limit;

		public FeatureGroup(int index, int value, int limit) {
			this.index = index;
			this.value = value;
			this.limit = limit;
		}

		public FeatureGroup(int value, int limit) {
			this.value = value;
			this.limit = limit;
			this.index = -1;
		}

		public void setIndex(int index) {
			this.index = index;
		}
	}

	private static ArrayList<Fea> getCharBaseFea(String head,
			boolean train, int offset, String cL2, String cL1, String cR1, String cR2) {
		ArrayList<Fea> feas = new ArrayList<Fea>();
		String c0 = Character.toString(head.charAt(0));
		int index = getCharFeaIdx(train, c0);
		if (index != -1) {
			feas.add(new Fea(index + offset, 1));
		}
		String cn = Character.toString(head.charAt(head.length() - 1));
		index = getCharFeaIdx(train, cn);
		if (index != -1) {
			feas.add(new Fea(index + offset, 1));
		}
		for (int i = 1; i < head.length() - 1; i++) {
			String ci = head.substring(i, i + 1);
			index = getCharFeaIdx(train, ci + "_in");
			if (index != -1) {
				feas.add(new Fea(index + offset, 0.8));
			}
		}

		if (cL2!=null) {
			index = getCharFeaIdx(train, cL2 + "_before");
			if (index != -1) {
				feas.add(new Fea(index + offset, 0.5));
			}
		}
		if (cL1!=null) {
			index = getCharFeaIdx(train, cL1 + "_before");
			if (index != -1) {
				feas.add(new Fea(index + offset, 0.5));
			}
		}
		if (cR1!=null) {
			index = getCharFeaIdx(train, cR1 + "_after");
			if (index != -1) {
				feas.add(new Fea(index + offset, 0.5));
			}
		}
		if (cR2!=null) {
			index = getCharFeaIdx(train, cR2 + "_after");
			feas.add(new Fea(index + offset, 0.5));
		}
		return feas;
	}

	private static String genFeatureStr(ArrayList<Fea> feas) {
		HashSet<Fea> hashFeas = new HashSet<Fea>();
		hashFeas.addAll(feas);
		feas.clear();
		feas.addAll(hashFeas);
		Collections.sort(feas);
		StringBuilder sb = new StringBuilder();
		// sb.append(label).append(" ");
		for (Fea fea : feas) {
			if (fea.index != -1) {
				sb.append(fea.index).append(":").append(fea.weight).append(" ");
			}
		}
		return sb.toString();
	}

	private static int getCharFeaIdx(boolean train, String str) {
		if (charFeatures.containsKey(str)) {
			return charFeatures.get(str);
		} else {
			if (train) {
				int index = charFeatures.size() + 1;
				charFeatures.put(str, index);
				return index;
			} else {
				return -1;
			}
		}
	}

	private static class Fea implements Comparable {
		int index;
		double weight;

		@Override
		public int compareTo(Object o) {
			return (this.index - ((Fea) o).index);
		}

		public Fea(int index, double weight) {
			this.index = index;
			this.weight = weight;
		}

		public int hashCode() {
			return index;
		}

		public boolean equals(Object obj) {
			return (index == ((Fea) obj).index);
		}
	}
}
