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
		run(args);
	}

	private static void run(String[] args) throws Exception {
		if(args.length!=1) {
			System.out.println("java ~ part");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		ACECorefFeature fea = new ACECorefFeature(false, "entityCoref");
		
		LinearClassifier<String, String> classifier = LinearClassifier
				.readClassifier("stanfordClassifierEntity" + args[0] + ".gz");

		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		double thres = .8;
		
		ArrayList<ArrayList<Entity>> answers = new ArrayList<ArrayList<Entity>>();
		ArrayList<ArrayList<Entity>> goldKeys = new ArrayList<ArrayList<Entity>>(); 
		
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		for (int t=0;t<files.size();t++) {
			String file = files.get(t);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = t;
			
			fileNames.add(doc.fileID.replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese/", "/users/yzcchen/ACL12/data/ACE2005/Chinese/") + ".sgm");
			lengths.add(doc.content.length());
			
//			ArrayList<EventMention> events = doc.goldEventMentions;
			ArrayList<EntityMention> entityMentions = Util.getSieveCorefMentions(doc);
			Util.calEntityMentionAttribute(entityMentions, doc);
			
			ArrayList<Entity> activeChains = new ArrayList<Entity>();
			Collections.sort(entityMentions);

			for (int i = 0; i < entityMentions.size(); i++) {
				EntityMention ana = entityMentions.get(i);

				double maxVal = 0;
				EntityMention predictAnt = null;
				
				for (int j = i - 1; j >= 0; j--) {
					EntityMention candidate = entityMentions.get(j);
					fea.configure(candidate, ana, doc);
					
					String feaStr = fea.getSVMFormatString();

					double val = test(feaStr, classifier);
					if (val > thres && val > maxVal) {
						maxVal = val;
						predictAnt = candidate;
						break;
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
		ToSemEval.outputSemFormatEntity(fileNames, lengths, "entity.sys." + args[0], answers);
		ToSemEval.outputSemFormatEntity(fileNames, lengths, "entity.gold." + args[0], goldKeys);
	}

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
