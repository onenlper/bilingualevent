package entity.coref;

import java.util.ArrayList;
import java.util.Collections;

import model.ACEChiDoc;
import model.ACEDoc;
import model.Entity;
import model.EntityMention;
import util.Common;
import util.Util;
import coref.ToSemEval;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Distribution;

public class EntityCorefTest {

	public static void main(String args[]) throws Exception {
		if(args.length==0) {
			System.err.println("java ~ part [maxent|prepare|load]");
			System.exit(1);
		}
		run(args);
	}

	public static void run(String[] args) throws Exception {
		if(args.length==0) {
			System.out.println("java ~ part");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		
		String mode = args[1];
		
		ACECorefFeature fea = new ACECorefFeature(false, "entityCoref" + Util.part);
		LinearClassifier<String, String> classifier = null;
		if(mode.equals("maxent")) {
			classifier = LinearClassifier
				.readClassifier("stanfordClassifierEntity" + args[0] + ".gz");
		}
		ArrayList<String> predicts = new ArrayList<String>();
		if(mode.equals("load")) {
			predicts = Common.getLines("entityPredict" + args[0]);
		}
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		double thres = .7;
//		double thres = .5;
		
		ArrayList<ArrayList<Entity>> answers = new ArrayList<ArrayList<Entity>>();
		ArrayList<ArrayList<Entity>> goldKeys = new ArrayList<ArrayList<Entity>>(); 
		
		probs.clear();
		
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		
//		HashMap<String, ArrayList<EntityMention>> maps = Util.getMentionsFromCRFFile(files, "yy0");
		ArrayList<String> testLines = new ArrayList<String>();
		
		for (int t=0;t<files.size();t++) {
			String file = files.get(t);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = t;
			
			fileNames.add(doc.fileID.replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese/", "/users/yzcchen/ACL12/data/ACE2005/Chinese/") + ".sgm");
			lengths.add(doc.content.length());
			
//			ArrayList<EventMention> events = doc.goldEventMentions;
			ArrayList<EntityMention> entityMentions = Util.getSieveCorefMentions(doc);
//			ArrayList<EntityMention> entityMentions = maps.get(doc.fileID);
//			for(EntityMention mention : entityMentions) {
//				mention.start = mention.headStart;
//				mention.end = mention.headEnd;
////				System.out.println(mention.headStart + "#" + mention.headEnd);
//				Util.assignSystemSemantic(mention, doc.fileID);
//			}
			
			Util.calEntityMentionAttribute(entityMentions, doc);
			
			ArrayList<Entity> activeChains = new ArrayList<Entity>();
			Collections.sort(entityMentions);

			for (int i = 0; i < entityMentions.size(); i++) {
				EntityMention ana = entityMentions.get(i);

				EntityMention predictAnt = null;
				double maxVal = 0;
				
				for (int j = i - 1; j >= 0; j--) {
					EntityMention candidate = entityMentions.get(j);
					fea.configure(candidate, ana, doc);
					
					double val = 0;
					if(mode.equals("maxent")) {
						String feaStr = fea.getSVMFormatString();
						val = test(feaStr, classifier);
					} else if(mode.equals("load")) {
						String line = predicts.remove(0);
						String tks[] = line.split("\\s+");
						val = Double.parseDouble(tks[1]);
					} else if(mode.equals("prepare")){
						String feaStr = fea.getSVMFormatString();
						testLines.add("1 " + feaStr);
					}
					
					probs.add(file + " " + candidate.toName() + " " + ana.toName() + " " + val);
					
					if (val > thres && val > maxVal) {
//						if(predictAnt == null) 
							predictAnt = candidate;
					}
				}

				if (predictAnt == null) {
					Entity ec = new Entity();
					ec.mentions.add(ana);
					activeChains.add(ec);
				} else {
					out: for(int g=0;g<activeChains.size();g++) {
						Entity chain = activeChains.get(g);
						for(int h=0;h<chain.mentions.size();h++) {
							EntityMention m = chain.mentions.get(h);
							if(m==predictAnt) {
								chain.mentions.add(ana);
								break out;
							}
						}
					}
				}
			}
			// process activeChains
			answers.add(activeChains);
			goldKeys.add(doc.goldEntities);
		}
		
		if(!args[1].equals("prepare")) {
			if(predicts.size()!=0) {
				Common.bangErrorPOS("");
			}
			ToSemEval.outputSemFormatEntity(fileNames, lengths, "entity.sys." + args[0], answers);
			ToSemEval.outputSemFormatEntity(fileNames, lengths, "entity.gold." + args[0], goldKeys);
			Common.outputLines(probs, "entityProbs" + args[0]);
		} else {
			Common.outputLines(testLines, "entityTest" + args[0]);
		}
	}

	static ArrayList<String> probs = new ArrayList<String>();
	
	public static double test(String str,
			LinearClassifier<String, String> classifier) {

		Datum<String, String> testIns = Dataset.svmLightLineToDatum(str);
		Counter<String> scores = classifier.scoresOf(testIns);
		Distribution<String> distr = Distribution
				.distributionFromLogisticCounter(scores);
		double prob = distr.getCount("+1");
		return prob;
	}
}
