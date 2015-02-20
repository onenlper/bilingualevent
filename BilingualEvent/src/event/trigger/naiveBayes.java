package event.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;
import util.Util;

public class naiveBayes {

	public static void addOne(HashMap<Integer, Double> map, Integer key) {
		Double d = map.get(key);
		if(d==null) {
			map.put(key, 1.0);
		} else {
			map.put(key, d.doubleValue() + 1.0);
		}
	}
	
	public static void addOne(HashMap<Integer, HashMap<Integer, Double>> maps, Integer feaIdx, Integer label) {
		HashMap<Integer, Double> map = maps.get(label);
		if(map==null) {
			map = new HashMap<Integer, Double>();
			maps.put(label, map);
		}
		Double d = map.get(feaIdx);
		if(d==null) {
			map.put(feaIdx, 1.0);
		} else {
			map.put(feaIdx, d.doubleValue() + 1.0);
		}
	}
	
	public static void probalistic(HashMap<Integer, Double> map, double norm, double N) {
		double all = 0;
		for(Integer key : map.keySet()) {
			all += map.get(key);
		}
		all += norm * N;
		for(Integer key : map.keySet()) {
			map.put(key, (norm + map.get(key))/all);
		}
	}
	
	public static double getProb(HashMap<Integer, HashMap<Integer, Double>> conditions, int feaIdx, int label) {
		if(!conditions.get(label).containsKey(feaIdx)) {
			return smoother/(feaSpaceSize + smoother*feaSpaceSize);
		}
		return conditions.get(label).get(feaIdx);
	}
	
	static int feaSpaceSize = 0;
	
//	static double smoother = 0.01;
	static double smoother = 0.01;
	
	public static void main(String args[]) {
		if(args.length!=1) {
			System.err.println("java ~ part");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		
		String trainFile = "/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/Joint_triggersFeature_train" + Util.part;
		String testFile = "/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/Joint_triggersFeature_test" + Util.part;
		ArrayList<String> train = Common.getLines(trainFile);
		HashMap<Integer, Double> priors = new HashMap<Integer, Double>();
		for(int i=1;i<=34;i++) {
			if(i!=34) {
				priors.put(i, 6e12);
			}
		}
		HashMap<Integer, HashMap<Integer, Double>> likelihood = new HashMap<Integer, HashMap<Integer, Double>>(); 
		
		HashSet<Integer> feaSpace = new HashSet<Integer>();
		
		for(String line : train) {
			String tks[] = line.split("\\s+");
			int label = Integer.parseInt(tks[0]);
			
			addOne(priors, label);
			for(int i=1;i<tks.length;i++) {
				int a = tks[i].indexOf(":");
				int idx = Integer.parseInt(tks[i].substring(0, a));
				double val = Double.parseDouble(tks[i].substring(a+1));
				if(val!=1.0) {
					continue;
				}
				if(idx>=700000) {
					continue;
				}
				feaSpace.add(idx);
				addOne(likelihood, idx, label);
			}
		}
		feaSpaceSize = feaSpace.size();
		
		for(Integer label : likelihood.keySet()) {
			probalistic(likelihood.get(label), smoother, feaSpaceSize);
		}
//		probalistic(priors, 60000.0, priors.size());
		probalistic(priors, 10000.0, priors.size());

		ArrayList<String> test = Common.getLines(testFile);
		ArrayList<String> output = new ArrayList<String>();
		for(String line : test) {
			String tks[] = line.split("\\s+");
			HashMap<Integer, Double> result = new HashMap<Integer, Double>();
			
			double maxP = -1;
			Integer predict = -1;
			
			for(int label : priors.keySet()) {
				double p = 1.0e255;
				p *= priors.get(label);
				for(int i=1;i<tks.length;i++) {
					int a = tks[i].indexOf(":");
					int idx = Integer.parseInt(tks[i].substring(0, a));
					double val = Double.parseDouble(tks[i].substring(a+1));
					if(idx>=700000) {
						continue;
					}
					p *= getProb(likelihood, idx, label);
					
//					System.out.println(priors.get(label));
//					System.out.println(getProb(conditions, label, idx));
				}
				
				if(p>maxP) {
					maxP = p;
					predict = label;
				}
				result.put(label, p);
			}
			probalistic(result, 0.0, 34);
			
			StringBuilder sb = new StringBuilder();
			sb.append(predict).append("\t");
			for(int i=1;i<=34;i++) {
				if(!result.containsKey(i)) {
					sb.append(0).append("\t");
				} else {
					sb.append(result.get(i)).append("\t");
				}
			}
			output.add(sb.toString().trim());
		}
		System.out.println(priors + "###" + priors.get(34));
		Common.outputLines(output, "/users/yzcchen/tool/svm_multiclass/JointTriggerOutput_test" + Util.part);
	}
	
}
