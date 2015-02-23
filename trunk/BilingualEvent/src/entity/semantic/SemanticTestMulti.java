package entity.semantic;

import java.io.FileWriter;
import java.util.ArrayList;

import model.ACEChiDoc;
import model.EntityMention;
import model.ParseResult;
import util.Common;
import util.Util;

/*
 * classify subtype directly
 */
public class SemanticTestMulti {

	public static String baseCRFSemPath = "/shared/mlrdir1/disk1/home/yzcchen/tool/CRF/CRF++-0.54/ACE/Semantic/";
	public static ArrayList<ArrayList<EntityMention>> emses;

	public static void main(String args[]) throws Exception {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Util.part = args[0];
		SVMSemanticFeature.init();
		SVMSemanticFeature.semDicFeatures = Common.readFile2Map("semantic_dic"
				+ args[0]);
		SVMSemanticFeature.charFeatures = Common.readFile2Map("semantic_char"
				+ args[0]);
		String test[] = new String[1];
		test[0] = args[0];
		String baseFolder = "./";

		FileWriter mentionFw = new FileWriter(baseFolder + "mention.test"
				+ args[0]);

		FileWriter typeFw = new FileWriter(baseFolder + "multiType.test"
				+ args[0]);

		ArrayList<String> files = Common.getLines("ACE_Chinese_test"
				+ Util.part);

		for (int index = 0; index < files.size(); index++) {
			String file = files.get(index);
			ACEChiDoc doc = new ACEChiDoc(file);

			// ArrayList<Element> ners = nerses2.get(index);
			// String apfFile = ACECommon.getRelateApf(sgmFile);
			// ArrayList<Entity> entities = ACECommon.getEntities(apfFile);
			// PlainText plainText = ACECommon.getPlainText(sgmFile);
			// system mentions
			// ArrayList<EntityMention> ems = emses.get(index);
			String content = doc.content;

			for (ParseResult pr : doc.parseReults) {
				for (int i = 0; i < pr.words.size(); i++) {
					int position[] = pr.positions.get(i);
					int start = position[0];
					int end = position[1];
					String head = pr.words.get(i);

					// for(Element ele:ners) {
					// if(ele.end==em.headEnd) {
					// em.ner = ele.content;
					// }
					// }

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

					String str = SVMSemanticFeature.semanticFeature(head,
							"none", false, cL2, cL1, cR1, cR2);

					typeFw.write("1 " + str + "\n");
					mentionFw.write(start + "," + end + " " + file + "\n");
				}
			}
		}

		mentionFw.close();
		typeFw.close();
	}
}
