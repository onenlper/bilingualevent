package entity.semantic;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.Entity;
import model.EntityMention;
import model.ParseResult;
import util.Common;
import util.Util;

/*
 * classify subtype directly
 */
public class SemanticTrainMulti {

	public static void main(String args[]) throws Exception {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Util.part = args[0];
		
		SVMSemanticFeature.init();

		FileWriter typeFw = new FileWriter("multiType.train" + Util.part);
		ArrayList<String> files = Common.getLines("ACE_Chinese_train" + Util.part);
		
		for (int index = 0; index < files.size(); index++) {
			String file = files.get(index);
			ACEChiDoc doc = new ACEChiDoc(file);
			HashMap<String, String> typeMap = new HashMap<String, String>();
			
			for (Entity en : doc.goldEntities) {
				for (EntityMention em : en.mentions) {
					String type = en.type.toLowerCase();
					String key = Integer.toString(em.headEnd);
					
//					System.out.println(em.head + "#" + doc.content.substring(em.headStart, em.headEnd+1)
//							+ "$$" + file);
					typeMap.put(key, type);
				}
			}
			String content = doc.content;
			
			for (ParseResult pr : doc.parseReults) {
				for(int i=0;i<pr.words.size();i++) {
					int position[] = pr.positions.get(i);
					int start = position[0];
					int end = position[1];
					String key = Integer.toString(end); 
					String head = pr.words.get(i);
					
					String cL2 = null;
					String cL1 = null;
					String cR1 = null;
					String cR2 = null;
					
					if (start - 2 > 0) {
						cL2 = content.substring(start - 2, start - 1);
					}
					if (start - 1 > 0) {
						cL1 = content.substring(start - 1, start);
					}
					if (end + 1 < content.length()) {
						cR1 = content.substring(end + 1, end + 2);
					}
					if (end + 2 < content.length()) {
						cR2 = content.substring(end + 2, end + 3);
					}
					
					String str = SVMSemanticFeature.semanticFeature(head, "none", true, cL2, cL1, cR1, cR2);
					String type = "none";
					
					if(typeMap.containsKey(key)) {
						type = typeMap.get(key);
					}
					
					int idx = types.indexOf(type) + 1;
					
					// System.out.println(type + " " + subType);
					typeFw.write(idx + " " + str + "\n");
				}
			}
			
		}

		typeFw.close();
		Common.outputHashMap(SVMSemanticFeature.charFeatures, "semantic_char" + Util.part);
		Common.outputHashMap(SVMSemanticFeature.semDicFeatures, "semantic_dic" + Util.part);
	}
	
	public static ArrayList<String> types = new ArrayList<String>(Arrays.asList("wea", "veh", "per", "fac", "gpe", "loc",
		"org", "none"));
}
