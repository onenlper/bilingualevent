package entity.semantic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.Depend;
import model.DependTree;
import model.Entity;
import model.ParseResult;
import model.stanford.StanfordResult;
import model.stanford.StanfordSentence;
import model.stanford.StanfordToken;
import model.stanford.StanfordXMLReader.StanfordDep;
import model.syntaxTree.MyTreeNode;

import org.xml.sax.InputSource;

import util.Common;


public class ACECommon {
	
	public static ArrayList<String> getFileList(String posts[]) {
		ArrayList<String> fileLists = new ArrayList<String>();
		for(String post:posts) {
			ArrayList<String> list = Common.getLines("ACE_"+post);
			for(String str:list) {
				fileLists.add(str);
			}
		}
		return fileLists;
	}
	
//	public static String aceDataPath = "/users/yzcchen/ACL12/data/ACE2005/Chinese";
	public static String aceDataPath;
	
//	public static String aceModelPath = "/users/yzcchen/ACL12/model/ACE";
	public static String aceModelPath;
	
	public static String getRelateApf(String sgm) {
		return sgm.substring(0, sgm.length() - 4) + ".apf.xml";
	}

//	public static ArrayList<Entity> getEntities(String apfFn) {
//		ArrayList<Entity> entities = new ArrayList<Entity>();
//		try {
//			InputStream inputStream = new FileInputStream(new File(apfFn));
//			SAXParserFactory sf = SAXParserFactory.newInstance();
//			SAXParser sp = sf.newSAXParser();
//			APFXMLReader reader = new APFXMLReader(entities);
//			sp.parse(new InputSource(inputStream), reader);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return entities;
//	}
	
//	public static ArrayList<Entity> getTimeMentions(String apfFn) {
//		ArrayList<Entity> entities = new ArrayList<Entity>();
//		try {
//			InputStream inputStream = new FileInputStream(new File(apfFn));
//			SAXParserFactory sf = SAXParserFactory.newInstance();
//			SAXParser sp = sf.newSAXParser();
//			TimeXLMReader reader = new TimeXLMReader(entities);
//			sp.parse(new InputSource(inputStream), reader);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return entities;
//	}
//	
//	public static ArrayList<Entity> getValueMentions(String apfFn) {
//		ArrayList<Entity> entities = new ArrayList<Entity>();
//		try {
//			InputStream inputStream = new FileInputStream(new File(apfFn));
//			SAXParserFactory sf = SAXParserFactory.newInstance();
//			SAXParser sp = sf.newSAXParser();
//			ValueXMLReader reader = new ValueXMLReader(entities);
//			sp.parse(new InputSource(inputStream), reader);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return entities;
//	}
	
	
//	public static PlainText getPlainText(String sgmFn) {
//		PlainText sgm = new PlainText();
//		// fix the bug: there may be a newline character in the head of file
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(sgmFn));
//			String line="";
//			while((line=br.readLine())!=null) {
//				if(line.trim().isEmpty()) {
//					sgm.content +="\n";
//				} else {
//					break;
//				}
//			}
//			br.close();
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		try {
//			InputStream inputStream = new FileInputStream(new File(sgmFn));
//			SAXParserFactory sf = SAXParserFactory.newInstance();
//			SAXParser sp = sf.newSAXParser();
//			SGMXMLReader reader = new SGMXMLReader(sgm);
//			sp.parse(new InputSource(inputStream), reader);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		sgm.content = sgm.content.replace("&", "&amp;").replace("•", "&#8226;");
//		return sgm;
//	}
	
	
	public static ArrayList<String> getACEFiles(String postfix) {
		ArrayList<String> filenames = new ArrayList<String>();
		String folders[] = { File.separator+"bn", File.separator+"nw", File.separator+"wl" };
		for (String folder : folders) {
			String subFolder = aceDataPath + folder + File.separator+"adj"+File.separator;
			File files[] = (new File(subFolder)).listFiles();
			for (File file : files) {
				if (file.getName().endsWith(postfix)) {
					filenames.add(file.getAbsolutePath());
				}
			}
		}
		return filenames;
	}
	

	public static HashSet<String> locations = Common.readFile2Set(Common.dicPath+"location2");
	public static HashSet<String> orgs_intl = Common.readFile2Set(Common.dicPath+"orgs_intl");
	public static HashSet<String> proper_industry = Common.readFile2Set(Common.dicPath+"propernames_industry");
	public static HashSet<String> proper_org = Common.readFile2Set(Common.dicPath+"propernames_org");
	public static HashSet<String> proper_other = Common.readFile2Set(Common.dicPath+"propernames_other");
	public static HashSet<String> proper_people = Common.readFile2Set(Common.dicPath+"propernames_people");
	public static HashSet<String> proper_place = Common.readFile2Set(Common.dicPath+"propernames_place");
	public static HashSet<String> proper_press = Common.readFile2Set(Common.dicPath+"propernames_press");
	public static HashSet<String> who_china = Common.readFile2Set(Common.dicPath+"whoswho_china");
	public static HashSet<String> who_intl = Common.readFile2Set(Common.dicPath+"whoswho_international");
	
	// generate IN_LOCATION_NAME(c-1c0), IN_LOCATION_NAME(c0c1) features, and other dic features
//	public static void genInLocationFea(ArrayList<MentionInstance> instances) {
//		for(int i=0;i<instances.size();i++) {
//			MentionInstance instance = (MentionInstance) instances.get(i);
//			if(instance.getCharacter().charAt(0)==' '||Common.isPun(instance.getCharacter().charAt(0))){
//				continue;
//			}
//			int start = i;
//			int end = i;
//			StringBuilder sb = new StringBuilder();
//			while(!Common.isPun(instances.get(end).getCharacter().charAt(0))) {
//				sb.append(instances.get(end).getCharacter());
//				end++;
//				// the maximum length of location is 15
//				if(end-start==15||end==instances.size()) {
//					break;
//				}
//			}
//			end--;
//			String ws = sb.toString();
//			ws = ws.replace("\\n", "");
//			for(int j=ws.length();j>=0;j--) {
//				String str = ws.substring(0,j);
//				if(locations.contains(str)) {
//					((MentionInstance)instances.get(start)).setInLocation1(1);
//					for(int k=start+1;k<end;k++) {
//						((MentionInstance)instances.get(k)).setInLocation1(1);
//						((MentionInstance)instances.get(k)).setInLocation2(1);
//					}
//					((MentionInstance)instances.get(end)).setInLocation2(1);
//				}
//				if(orgs_intl.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_ORGS_INTL("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_ORGS_INTL("I");
//					}
//				}
//				if(proper_industry.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_PROP_INDUSTRY("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_PROP_INDUSTRY("I");
//					}
//				}if(proper_org.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_PROP_ORG("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_PROP_ORG("I");
//					}
//				}if(proper_other.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_PROP_OTHER("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_PROP_OTHER("I");
//					}
//				}if(proper_people.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_PROP_PEOPLE("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_PROP_PEOPLE("I");
//					}
//				}if(proper_place.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_PROP_PLACE("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_PROP_PLACE("I");
//					}
//				}if(proper_press.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_PROP_PRESS("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_PROP_PRESS("I");
//					}
//				}if(who_china.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_WHOWHO_CHINA("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_WHOWHO_CHINA("I");
//					}
//				}if(who_intl.contains(str)) {
//					((MentionInstance)instances.get(start)).setIN_WHOWHO_INTER("B");
//					for(int k=start+1;k<=end;k++) {
//						((MentionInstance)instances.get(k)).setIN_WHOWHO_INTER("I");
//					}
//				}
//				end--;
//			}
//		}
//	}
	

	public static HashSet<String> suffixes = Common.readFile2Set(Common.dicPath +"location_suffix");
	// generate ACE location_suffix features
//	public static void genLocationSuffixFea(ArrayList<MentionInstance> instances) {
//		for(int i=0;i<instances.size();i++) {
//			MentionInstance instance = (MentionInstance) instances.get(i);
//			if(suffixes.contains(instance.getCharacter())) {
//				instance.setLocation_suffix(1);
//			}
//		}
//	}
	
//	public static void genACEEnglishFea(ArrayList<MentionInstance> instances) {
//		for(Instance instance:instances) {
//			char c = instance.getCharacter().charAt(0);
//			if((c>='a' && c<='z') || (c>='A' && c<='Z')) {
//				((MentionInstance)instance).setIsEnglish(1);
//			} 
//			if(c>='A' && c<='Z') {
//				((MentionInstance)instance).setIsUpcaseEnglish(1);
//			}
//		}
//	}
//
//	public static void genACENounPhraseFea(ArrayList<MentionInstance> instances, ArrayList<ParseResult> parseResults) {
//		for(ParseResult pr: parseResults) {
//			for(int i=1;i<pr.words.size();i++) {
//				MyTreeNode node = pr.tree.leaves.get(i);
//				ArrayList<MyTreeNode> ancestors = node.getAncestors();
//				boolean inNP = false;
//				for(MyTreeNode tmp : ancestors) {
//					if(tmp.value.equals("NP")) {
//						inNP = true;
//						break;
//					}
//				}
//				int[] position = pr.positions.get(i);
//				if(inNP) {
//					for(int j=position[0];j<=position[1];j++) {
//						instances.get(j).setIsInNP(1);
//					}
//				}
//			}
//		}
//	}
	
	// generate noun phrase features, whether this character is a part of noun phrase
//	public static void genACENounPhraseFea2(ArrayList<MentionInstance> instances, String filename) {
//		try {
//			String parseFn = filename.substring(0,filename.length()-3) + "parse2";
//			BufferedReader br = Common.getBr(parseFn);
//			String line;
//			// used to locate instance
//			int index=0;
//			while((line=br.readLine())!=null) {
//				// begin the new tree
//				if(line.equals("(ROOT")) {
//					boolean read = true;
//					while(true) {
//						// find NP
//						if(read) {
//							line=br.readLine();
//						}
//						read = true;
//						if(line.isEmpty()) {
//							break;
//						}
//						if(line.trim().startsWith("(NP")) {
//							StringBuilder sb = new StringBuilder();
//							// NP in the same line
//							if(line.trim().startsWith("(NP ")) {
//								sb.append(line);
////								line = br.readLine();
//							} else {
//								// NP does not in the same line
//								int wsCount = Common.findWSCount(line);
//								sb.append(line);
//								while(true) {
//									line=br.readLine();
//									// this subtree stops
//									if(Common.findWSCount(line)<=wsCount) {
//										read =false;
//										break;
//									}
//									sb.append(line);
//								}
//							}
//							String npTree = sb.toString();
//							String plainText = Common.extractPlainText(npTree);
////							boolean first = true;
//							for(int i=0;i<plainText.length();i++) {
//								char c = instances.get(index).getCharacter().charAt(0);
//								while(c==' '||c=='\r'||c=='\n') {
//									index++;
//									c = instances.get(index).getCharacter().charAt(0);
//								}
////								System.out.println(plainText);
////								System.out.println(plainText.charAt(i));
////								System.out.println(c);
//								if(plainText.charAt(i)==c) {
////									if(first) {
////										((MentionInstance)instances.get(index)).setIsInNP("B");
////										first = false;
////									} else {
//										((MentionInstance)instances.get(index)).setIsInNP(1);
////									}
//									index++;
//								}
//							}
//						}
//						else {
//							String plainText = Common.extractPlainText(line);
//							if(!plainText.trim().isEmpty()) {
//								for(int i=0;i<plainText.length();i++) {
//									char c = instances.get(index).getCharacter().charAt(0);
//									while(c==' '||c=='\r'||c=='\n') {
//										index++;
//										c = instances.get(index).getCharacter().charAt(0);
//									}
//									if(plainText.charAt(i)==c) {
//										index++;
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//			br.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	public static HashMap<String, String> POS09DIC;
	
	public static HashMap<String, String> POS10DIC;

//	public static void genACEBoundPOSFea(ArrayList<MentionInstance> instances, ArrayList<ParseResult> prs) {
//		if(POS09DIC==null) {
//			POS09DIC = Common.readFile2Map2(Common.dicPath + "09POSDIC");
//		}
//		if(POS10DIC==null) {
//			POS10DIC = Common.readFile2Map2(Common.dicPath + "10POSDIC");;
//		}
//		//
//		for(ParseResult pr : prs) {
//			for(int i=1;i<pr.words.size();i++) {
//				int start = pr.positions.get(i)[0];
//				int end = pr.positions.get(i)[1];
//				String token = pr.words.get(i);
//				
//				if(start==-1 || end==-1) {
//					start = 0;
//				}
//				
//				String POS = pr.posTags.get(i);
//				String POS09 = POS09DIC.get(token);
//				String POS10 = POS10DIC.get(token);
//				if(POS09==null) {
//					POS09="NULL";
//				}
//				if(POS10==null) {
//					POS10="NULL";
//				}
//				instances.get(start).setLeftBound(1);
//				instances.get(start).setPosFea("B-"+POS);
//				instances.get(start).setPOS09("B-"+POS09);
//				instances.get(start).setPOS10("B-"+POS10);
//				
//				
//				for(int j=start+1;j<=end;j++) {
//					instances.get(j).setPosFea("I-"+POS);
//					instances.get(j).setPOS09("I-"+POS09);
//					instances.get(j).setPOS10("I-"+POS10);
//				}
//				instances.get(end).setRightBound(1);
//			}
//		}
//	}
//	
//	public static void genACEBoundPOSFea(ArrayList<MentionInstance> instances, String tokenCon) {
//		if(POS09DIC==null) {
//			POS09DIC = Common.readFile2Map2(Common.dicPath + "09POSDIC");
//		}
//		if(POS10DIC==null) {
//			POS10DIC = Common.readFile2Map2(Common.dicPath + "10POSDIC");;
//		}
////		try {
////			String tokenFn = filename.substring(0,filename.length()-3) + "pos";
////			BufferedReader br = Common.getBr(tokenFn);
////			String line;
////			StringBuilder sb = new StringBuilder();
////			// skip the first line of POS file
//////			br.readLine();
////			while((line=br.readLine())!=null) {
////				sb.append(" " + line);
////			}
////			String tokenCon=sb.toString().replace("&", "&amp;").replace("•", "&#8226;")+" ";
//			int index=0;
//			String POS = "";
//			String POS09 = "";
//			String POS10 ="";
//			for(int i=0;i<instances.size();i++) {
//				MentionInstance instance = (MentionInstance) instances.get(i);
//				if(instance.getCharacter().equals("\n") || instance.getCharacter().equals(" ")
//						|| instance.getCharacter().equals("\r") || instance.getCharacter().equals("　")) {
//					continue;
//				}
//				while(tokenCon.charAt(index)!=instance.getCharacter().charAt(0)) {
//					index++;
//				}
//				if(tokenCon.charAt(index-1)==' ') {
//					instance.setLeftBound(1);
//					// get POS value
//					int start = 0;
//					int end = 0;
//					for(int j=index;;j++) {
//						if(tokenCon.charAt(j)=='#') {
//							start = j+1;
//						}
//						if(tokenCon.charAt(j)==' ') {
//							end = j;
//							break;
//						}
//					}
//					String token = tokenCon.substring(index,start-1);
//					token = token.replace("\\n", "");
////					System.out.println(token);
//					POS09 = POS09DIC.get(token);
//					POS10 = POS10DIC.get(token);
//					if(POS09==null) {
//						POS09="NULL";
//					}
//					if(POS10==null) {
//						POS10="NULL";
//					}
//					POS = tokenCon.substring(start, end);
//					instance.setPosFea("B-"+POS);
//					instance.setPOS09("B-"+POS09);
//					instance.setPOS10("B-"+POS10);
//					if(tokenCon.charAt(index+1)=='#') {
//						instance.setRightBound(1);
//					}	
//				} else {
//					instance.setPosFea("I-"+POS);
//					instance.setPOS09("I-"+POS09);
//					instance.setPOS10("I-"+POS10);
//					if(tokenCon.charAt(index+1)=='#') {
//						instance.setRightBound(1);
//					}	
//				}
//				index++;
//			}
////		} catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//	}
	
	public static HashSet<String> surnames =Common.readSurname(Common.dicPath + "surname");
	
	// generate surname features, whether this word can be used as a chinese surname
//	public static void genACESurnameFea(ArrayList<MentionInstance> instances) {
//		for(int i=0;i<instances.size();i++) {
//			String c = instances.get(i).getCharacter();
//			if(surnames.contains(c)) {
//				((MentionInstance)instances.get(i)).setSurname(1);
//			}
//		}
//	}
	
	public static HashSet<String> pronouns = Common.readFile2Set(Common.dicPath+"pronoun");
	
	// generate pronoun features, c0, c-1c0, c0c1
//	public static void genPronounFea(ArrayList<MentionInstance> instances) {
//		for(int i=0;i<instances.size();i++) {
//			String c = instances.get(i).getCharacter();
//			if(pronouns.contains(c)) {
//				((MentionInstance)instances.get(i)).setIsPronoun1(1);
//			}
//			if(i>0) {
//				String c_1 = instances.get(i-1).getCharacter();
//				if(pronouns.contains(c_1+c)) {
//					((MentionInstance)instances.get(i)).setIsPronoun2(1);
//				}
//			}
//			if(i<instances.size()-1) {
//				String c1 = instances.get(i+1).getCharacter();
//				if(pronouns.contains(c+c1)) {
//					((MentionInstance)instances.get(i)).setIsPronoun3(1);
//				}
//			}
//		}
//	}
//	
//	public static ArrayList<ArrayList<Element>> getPredictNerElements(ArrayList<String> files, String crfFilePath) {
//		ArrayList<ArrayList<Element>> elementses = new ArrayList<ArrayList<Element>>();
//		elementses = ACECorefCommon.getSemanticsFromCRFFile(files, crfFilePath);
//		return elementses;
//	}
	
	
//	public static ArrayList<Element> getPredictNerElements(String sgmFn, PlainText sgm) {
//		ArrayList<Element> elements = new ArrayList<Element>();
//		int startIdx = 0;
//		try {
//			int pos = sgmFn.lastIndexOf(File.separator);
//			String name = sgmFn.substring(pos, sgmFn.length()-3);
//			BufferedReader br = Common.getBr(aceDataPath+ "/utf8ner/"+name+"source.ner");
//			StringBuilder sb = new StringBuilder();
//			String line;
//			while((line=br.readLine())!=null) {
//				sb.append(line);
//			}
//			br.close();
//			String nerContent = sb.toString();
//			nerContent = nerContent.replace("&", "&amp;").replace("•", "&#8226;");
//			for(int i=0;i<nerContent.length();i++) {
//				if(nerContent.charAt(i)=='\n' || nerContent.charAt(i)=='\r'||nerContent.charAt(i)==' '||nerContent.charAt(i)==' ') {
//					continue;
//				}
//				if(nerContent.charAt(i)=='[') {
//					int start = i+1;
//					while(nerContent.charAt(i)!=']') {
//						i++;
//					}
//					int end = i;
//					int[] position = Common.findPosition(sgm.content, nerContent.substring(start,end), startIdx);
//					startIdx = position[1] + 1;
//					StringBuilder sbLabel = new StringBuilder();
//					i++;
//					while(nerContent.charAt(i)!=' ') {
//						i++;
//						sbLabel.append(nerContent.charAt(i));
//					}
//					String label = sbLabel.toString().trim();
//					Element element = new Element(position[0], position[1], label);
//					elements.add(element);
//				} else {
//					try {
//					while(sgm.content.charAt(startIdx)!=nerContent.charAt(i)) {
//						startIdx++;
//					} }
//					catch (Exception e) {
//						System.out.println(nerContent.charAt(i));
//						System.out.println("=====");
//						System.out.println(sgm.content.charAt(startIdx));
//					}
//				}
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return elements;
//	}
	
//	public static void genACENerFea(ArrayList<MentionInstance> instances, String sgmFn, ArrayList<Element> elements) {
//		for(Element element:elements) {
//			((MentionInstance)instances.get(element.getStart())).setNerFea("B-"+element.getContent());
//			for(int m=element.getStart()+1;m<=element.getEnd();m++) {
//				((MentionInstance)instances.get(m)).setNerFea("I-"+element.getContent());
//			}
//		}
//	}
//	
	
	public static ArrayList<ParseResult> standford2ParseResult(StanfordResult sr, String content) {
		ArrayList<ParseResult> parseReults = new ArrayList<ParseResult>();
		
		for(int i=0;i<sr.sentences.size();i++) {
			StanfordSentence ss = sr.sentences.get(i);
			ParseResult pr = new ParseResult();
			
			pr.words.add("ROOT");
			pr.posTags.add("ROOT");
			pr.lemmas.add("ROOT");
			int[] rootPosition = {-1, -1};
			pr.positions.add(rootPosition);
			
			pr.tree = ss.parseTree;
			StringBuilder sb = new StringBuilder();
			for(int j=0;j<ss.tokens.size();j++) {
				StanfordToken tk = ss.tokens.get(j);
				int start = tk.CharacterOffsetBegin;
				int end = tk.CharacterOffsetEnd;
				
				sb.append(tk.word).append(" ");
				pr.words.add(tk.word);
				pr.posTags.add(tk.POS);
				pr.lemmas.add(tk.lemma);
				
				int[] positions = new int[2];
				positions[0] = start;
				positions[1] = end;
				pr.positions.add(positions);
				
			}
			pr.sentence = sb.toString().trim();
			
			for(StanfordDep dep : ss.collapsedCcprocessedDependencies) {
				int first = dep.getGovernorId();
				int second = dep.getDependentId();
				String type = dep.getType();
				pr.depends.add(new Depend(type, first, second));
			}
			pr.dependTree = new DependTree(pr.depends);
			
			parseReults.add(pr);
		}
		return parseReults;
	}
}
