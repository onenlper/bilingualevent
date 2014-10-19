package util;

import java.util.HashMap;

public class Why {
	public static void main(String args[]) {
		HashMap<String, Integer> map1 = Common.readFile2Map("argumentJointFeaSpace_svm1");
		
		HashMap<String, Integer> map2 = Common.readFile2Map("argumentPipeFeaSpace_svm1");
		
		for(String key : map2.keySet()) {
			if(!map1.containsKey(key)) {
				System.out.println(key);
			}
		}
	}
}
