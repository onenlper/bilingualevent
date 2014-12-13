package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;

// java ~ /users/yzcchen/ACL12/model/ACE/coref_test_predict/ mp
/*
 * not zero pronoun chain
 */
public class AltafToSemEval {
	
	public static void main(String args[]) throws Exception{
		if(args.length<2) {
			System.out.println("java ~ /users/yzcchen/conll12/chinese/train/ mp");
			return;
		}
		outputSemFormat(args[0], args[1]);
	}
	
	public static void outputSemFormat(String folderPath, String model){
		try {
			ArrayList<String> files = Common.getLines(folderPath + File.separator + "all.txt");
			FileWriter systemKeyFw;
			systemKeyFw = new FileWriter(folderPath + "key." + model);
			for (int i=0;i<files.size();i++) {
				String line = files.get(i);
				String systemEntityPath = line+"."+model;
				ArrayList<Entity> systemChain = loadEntities(systemEntityPath);
				String content = Common.getLine(line + ".source");
				writerKey(systemKeyFw, systemChain, "#begin document " + line, content);
				explainChain(content, systemChain, line + ".chain." + model);
			}
			systemKeyFw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void explainChain(String content, ArrayList<Entity> entities, String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for(Entity entity : entities) {
				StringBuilder sb = new StringBuilder();
				for(EntityMention em : entity.mentions) {
					sb.append(em.start).append(",").append(em.end).append(" ");
					for(int i=em.start;i<=em.end;i++) {
						sb.append(content.charAt(i)).append(" ");
					}
					sb.append("#");
				}
				fw.write(sb.toString() + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void writerKey(FileWriter systemKeyFw, ArrayList<Entity> systemChain, String line, String content) throws IOException {
		HashSet<Integer> sentenceEnds = new HashSet<Integer>();
		systemKeyFw.write(line + "\n");
		ArrayList<CRFElement> elements = new ArrayList<CRFElement>();
		for(int i=0;i<content.length();i++) {
			elements.add(new CRFElement());
		}
		for(int i=0;i<systemChain.size();i++) {
			Entity en = systemChain.get(i);
			for(EntityMention em:en.mentions) {
				int start = em.start;
				int end = em.end;
				StringBuilder sb = new StringBuilder();
				if(start==end) {
					sb.append("(").append(i+1).append(")");
					elements.get(start).append(sb.toString());
				} else {
					elements.get(start).append("("+Integer.toString(i+1));
					elements.get(end).append(Integer.toString(i+1) + ")");
				}
			}
		}
		for(int i=0;i<elements.size();i++) {
			CRFElement element = elements.get(i);
			char c = content.charAt(i);
			if(element.predict.isEmpty()) {
				systemKeyFw.write(c + "	" + "-\n");
			} else {
				systemKeyFw.write(c + "	" +element.predict + "\n");
			}
			if(sentenceEnds.contains(i)) {
				systemKeyFw.write("\n");
			}
		}
		systemKeyFw.write("#end document\n");
	}
	
	public static class CRFElement {
		String word;
		String predict = "";
		
		public void append(String str) {
			if(predict.isEmpty()) {
				this.predict = str;
			} else {
				this.predict = str + "|" + this.predict;
			}
		}
	}

	public static ArrayList<Entity> loadEntities(String iFileName) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		try {
			BufferedReader input = Common.getBr(iFileName);
			String line;
			while ((line = input.readLine()) != null) {
				String[] mentions = line.split("\\s");
				Entity anEntity = new Entity();
				anEntity.mentions = new ArrayList<EntityMention>();
				for (int i = 0; i < mentions.length; i++) {
					if (!mentions[i].equals("")) {
						String[] tokens = mentions[i].split(",");
						anEntity.mentions.add(new EntityMention(Integer
								.parseInt(tokens[0]), Integer
								.parseInt(tokens[1])));
					}
				}
				Collections.sort(anEntity.mentions);
				entities.add(anEntity);
			}
			input.close();
		} catch (Exception e) {
			System.err.println("Gotcha creating entities : " + e);
		}
		Collections.sort(entities);
		return entities;
	}
}
