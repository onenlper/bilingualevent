package coref;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class Parameter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	HashMap<String, HashMap<String, Double>> values;

	HashMap<String, HashMap<String, Double>> fracCounts;
	HashMap<String, Double> keyCounts;

	double defaultV;

	HashSet<String> subKeys;

	boolean init;

	double theta = 1;

	public Parameter(HashSet<String> subKey) {
		values = new HashMap<String, HashMap<String, Double>>();
		fracCounts = new HashMap<String, HashMap<String, Double>>();
		keyCounts = new HashMap<String, Double>();
		subKeys = subKey;
		this.defaultV = 1.0/subKey.size();
		this.init = true;
	}

	public Parameter(double defaultV, HashSet<String> subKey) {
		values = new HashMap<String, HashMap<String, Double>>();
		fracCounts = new HashMap<String, HashMap<String, Double>>();
		keyCounts = new HashMap<String, Double>();
		subKeys = subKey;
		this.defaultV = defaultV;
		this.init = true;
	}

	public double getVal(String key, String subKey) {
		if (this.init) {
			return this.defaultV;
		} else {
			HashMap<String, Double> subVals = values.get(key);
			if(subVals!=null) {
				Double d = subVals.get(subKey);
				if(d!=null) {
					return d.doubleValue();
				}
			}
			return this.defaultV;
		}
	}

	public void setVals() {
		values.clear();
		for (String key : keyCounts.keySet()) {
			HashMap<String, Double> subMap = fracCounts.get(key);
			HashMap<String, Double> subValMap = new HashMap<String, Double>();
			values.put(key, subValMap);
 			double denominator = theta * subKeys.size();
			denominator += keyCounts.get(key);
			
			for (String subKey : subKeys) {
				double numerator = theta;
				if(subMap.containsKey(subKey)) {
					numerator += subMap.get(subKey);
				}
				double val = numerator/denominator;
				subValMap.put(subKey, val);
			}
		}
		this.init = false;
	}

	public void resetCounts() {
		this.fracCounts.clear();
		this.keyCounts.clear();
	}

	public void addFracCount(String key, String subKey, double val) {
//		subKeys.add(subKey);
		if(!subKeys.contains(subKey)) {
			System.out.println(subKey + "# " + key);
			System.out.println(subKeys);
			Common.bangErrorPOS("!");
		}
		HashMap<String, Double> subMap = fracCounts.get(key);
		if (subMap == null) {
			subMap = new HashMap<String, Double>();
			fracCounts.put(key, subMap);
		}
		Double d = subMap.get(subKey);
		if (d == null) {
			subMap.put(subKey, val);
		} else {
			subMap.put(subKey, val + d.doubleValue());
		}
		// add count
		Double c = keyCounts.get(key);
		if (c == null) {
			keyCounts.put(key, val);
		} else {
			keyCounts.put(key, val + c.doubleValue());
		}
	}

	public void printParameter(String fn) {
		ArrayList<String> output = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		sb.append("Antecedent:");
		for (String key : this.subKeys) {
			sb.append("\t").append(key);
		}
		output.add(sb.toString());

		for (String key : this.values.keySet()) {
			sb = new StringBuilder();
			sb.append(key);
			double all = 0;
			for (String subKey : this.subKeys) {
				if (this.values.get(key).containsKey(subKey)) {
					sb.append("\t").append(
							String.format("%.8f",
									this.values.get(key).get(subKey)));
					all += this.values.get(key).get(subKey);
				} else {
					sb.append("\t").append(0.000);
				}
			}
			output.add(sb.toString());
//			System.out.println(key + ":" + all);
		}
		Common.outputLines(output, fn);
	}

	public String round(double p, int digits) {
		String str = Double.toString(p);
		if (str.length() < digits) {
			while (str.length() < digits) {
				str += '0';
			}
		} else {
			str = str.substring(0, digits);
		}
		return str;
	}
}
