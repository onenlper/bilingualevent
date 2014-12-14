package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import util.Common.Feature;

public abstract class YYFeature {
	public boolean train;

	public HashMap<String, Integer> strFeaMap;

	public static int strFeaFrom = 1000;
	String name;

	public YYFeature(boolean train, String name) {
		this.train = train;
		this.name = name;
		if (train) {
			this.strFeaMap = new HashMap<String, Integer>();
		} else {
			this.strFeaMap = Common.readFile2Map(name);
		}
	}

	public void freeze() {
		if (!train) {
			System.err.println("Should not be called when testing");
		}
		Common.outputHashMap(this.strFeaMap, name);
	}

	public void setStrFeaFrom(int k) {
		this.strFeaFrom = k;
	}

	private int getStrIdx(String str) {
		if (strFeaMap.containsKey(str)) {
			return this.strFeaMap.get(str);
		} else {
			if (train) {
				int v = strFeaMap.size();
				strFeaMap.put(str, v);
				return v;
			} else {
				return -1;
			}
		}
	}

	public abstract ArrayList<Feature> getCategoryFeatures();

	public abstract ArrayList<String> getStrFeatures();

	public String getSVMFormatString() {
		StringBuilder sb = new StringBuilder();
		ArrayList<Feature> features = this.getCategoryFeatures();

		if (features != null) {
			if (categoryFeaSize != -1 && categoryFeaSize != features.size()) {
				System.err.println("Category fea size not equal!: " + features.size() + "####" + categoryFeaSize);
				System.exit(1);
			}
			categoryFeaSize = features.size();

			String feasToString = Common.feasToSVMString(features);

			sb.append(Common.feasToSVMString(features));
			
//			System.out.println(features.size() + ":" + feasToString);
			
			int lastIndex = Integer.parseInt(feasToString.substring(feasToString.lastIndexOf(" ") + 1).split(":")[0]);
			if (lastIndex > this.strFeaFrom) {
				Common.bangErrorPOS("Increase string feature start index!");
			}
		}
		ArrayList<String> strFeas = this.getStrFeatures();
		if (strFeas != null) {
//			if (this.strFeaSize != -1 && this.strFeaSize != strFeas.size()) {
//				System.err.println("String fea size not equal!");
//				System.exit(1);
//			}
			strFeaSize = strFeas.size();

			HashSet<Integer> strIndexSet = new HashSet<Integer>();
			for (int i = 0; i < strFeas.size(); i++) {
				String str = strFeas.get(i);
				int idx = this.getStrIdx(str);
				if (idx != -1) {
					strIndexSet.add(idx);
				}
			}
			ArrayList<Integer> sortedIndexes = new ArrayList<Integer>(strIndexSet);
			Collections.sort(sortedIndexes);

			for (Integer idx : sortedIndexes) {
				if(idx+this.strFeaFrom==15006) {
//					Common.bangErrorPOS("Get");
				}
				
				sb.append(" ").append(idx + this.strFeaFrom).append(":1");
			}
		}

		return sb.toString();
	}

	int categoryFeaSize = -1;
	int strFeaSize = -1;
}
