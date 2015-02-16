package coref;

import java.io.FileWriter;
import java.util.ArrayList;

import model.Entity;
import model.EntityMention;
import model.EventChain;
import model.EventMention;

/*
 * 
 */
public class ToSemEval {
	
	// public static String basePath =
	// "/users/yzcchen/ACL12/model/ACE/coref_test_predict"+File.separator;

	public static boolean singleton = false;
	
	public static void outputSemFormat(ArrayList<String> files, ArrayList<Integer> lengths,
					String outputPath, ArrayList<ArrayList<EventChain>> chainses) throws Exception {
		FileWriter fw = new FileWriter(outputPath);
		for (int k = 0; k < files.size(); k++) {
			String line = files.get(k);
			ArrayList<EventChain> chains = chainses.get(k);
			int length = lengths.get(k);
			
			fw.write("#begin document " + line + "\n");
			ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
			for (int i = 0; i < length; i++) {
				CRFElement element = new CRFElement();
				elements.add(element);
			}
			// System.out.println(line);

			for (int i = 0; i < chains.size(); i++) {
				EventChain en = chains.get(i);
				if(en.getEventMentions().size()==1 && !singleton) {
					continue;
				}
				for (EventMention em : en.getEventMentions()) {
					int start = em.getAnchorStart();
					int end = em.getAnchorEnd();

					StringBuilder sb = new StringBuilder();
					if (start == end) {
						sb.append("(").append(i + 1).append(")");
						elements.get(start).append(sb.toString());
					} else {
						elements.get(start).append("(" + Integer.toString(i + 1));
						elements.get(end).append(Integer.toString(i + 1) + ")");
					}
				}
			}
			for (int i = 0; i < elements.size(); i++) {
				CRFElement element = elements.get(i);
				if (element.predict.isEmpty()) {
					fw.write(Integer.toString(i + 1) + "	" + "_\n");
				} else {
					fw.write(Integer.toString(i + 1) + "	" + element.predict + "\n");
				}
			}

			fw.write("#end document " + line + "\n");
		}
		fw.close();
	}
	
	
	public static void outputSemFormatEntity(ArrayList<String> files, ArrayList<Integer> lengths,
			String outputPath, ArrayList<ArrayList<Entity>> chainses) throws Exception {
		FileWriter fw = new FileWriter(outputPath);
		for (int k = 0; k < files.size(); k++) {
			String line = files.get(k);
			ArrayList<Entity> chains = chainses.get(k);
			int length = lengths.get(k);
			
			fw.write("#begin document " + line + "\n");
			ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
			for (int i = 0; i < length; i++) {
				CRFElement element = new CRFElement();
				elements.add(element);
			}
			// System.out.println(line);
		
			for (int i = 0; i < chains.size(); i++) {
				Entity en = chains.get(i);
				if(en.getMentions().size()==1 && !singleton) {
					continue;
				}
				for (EntityMention em : en.mentions) {
					int start = em.headStart;
					int end = em.headEnd;
		
					StringBuilder sb = new StringBuilder();
					if (start == end) {
						sb.append("(").append(i + 1).append(")");
						elements.get(start).append(sb.toString());
					} else {
						elements.get(start).append("(" + Integer.toString(i + 1));
						elements.get(end).append(Integer.toString(i + 1) + ")");
					}
				}
			}
			for (int i = 0; i < elements.size(); i++) {
				CRFElement element = elements.get(i);
				if (element.predict.isEmpty()) {
					fw.write(Integer.toString(i + 1) + "	" + "_\n");
				} else {
					fw.write(Integer.toString(i + 1) + "	" + element.predict + "\n");
				}
			}
		
			fw.write("#end document " + line + "\n");
		}
		fw.close();
	}

	

	public static class CRFElement {
		String word;
		String predict = "";

		public void append(String str) {
			if (predict.isEmpty()) {
				this.predict = str;
			} else {
				this.predict = str + "|" + this.predict;
			}
		}

	}

}
