package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;
import coref.ToSemEval;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Distribution;

public class EventCorefTest {

	public static void main(String args[]) throws Exception {
		if(args.length!=1) {
			System.out.println("java ~ part");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		LinearClassifier<String, String> classifier = LinearClassifier
				.readClassifier("stanfordClassifier" + args[0] + ".gz");

		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		EventCorefFea fea = new EventCorefFea(false, "corefFea" + args[0]);
		double thres = .3;
		ArrayList<ArrayList<EventChain>> answers = new ArrayList<ArrayList<EventChain>>(); 
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();

//		HashMap<String, HashMap<String, EventMention>> jointSVMLines = EngArgEval.jointSVMLine();
//		HashMap<String, HashMap<String, String>> polarityMaps = AttriEvaluate.loadSystemAttri("polarity", "0");
//		HashMap<String, HashMap<String, String>> modalityMaps = AttriEvaluate.loadSystemAttri("modality", "0");
//		HashMap<String, HashMap<String, String>> genericityMaps = AttriEvaluate.loadSystemAttri("genericity", "0");
//		HashMap<String, HashMap<String, String>> tenseMaps = AttriEvaluate.loadSystemAttri("tense", "0");
		
		for (int g=0;g<files.size();g++) {
			String file = files.get(g);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = g;
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());
			
//			ArrayList<EventMention> events = doc.goldEventMentions;
//			Util.assignArgumentWithEntityMentions(doc.goldEventMentions,
//			doc.goldEntityMentions, doc.goldValueMentions,
//			doc.goldTimeMentions, doc);
			
			ArrayList<EventMention> events = Util.loadSystemComponents(doc);
//			HashMap<String, EventMention> evmMaps = jointSVMLines.get(file);
//			
//			if(evmMaps==null) {
//				evmMaps = new HashMap<String, EventMention>();
//			}
//			ArrayList<EventMention> ems = new ArrayList<EventMention>(evmMaps.values());
//			for(EventMention m : ems) {
//				m.setAnchor(doc.content.substring(m.getAnchorStart(), m.getAnchorEnd() + 1));
//			}
			
			Collections.sort(events);
			
			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();

//			Util.setSystemAttribute(ems, polarityMaps, modalityMaps, genericityMaps, tenseMaps, file);
			
			for (int i = 0; i < events.size(); i++) {
				EventMention ana = events.get(i);

				int corefID = -1;
				double maxVal = 0;
				for (int j = activeChains.size() - 1; j >= 0; j--) {
					EventChain ec = activeChains.get(j);

					fea.configure(ec, ana, doc, events);
					String feaStr = fea.getSVMFormatString();

					double val = test(feaStr, classifier);
					if (val > thres && val > maxVal) {
						maxVal = val;
						corefID = j;
					}
				}

				if (corefID == -1) {
					EventChain ec = new EventChain();
					ec.addEventMention(ana);
					activeChains.add(ec);
				} else {
					activeChains.get(corefID).addEventMention(ana);
				}
			}
			// process activeChains
			answers.add(activeChains);
		}
		ToSemEval.outputSemFormat(fileNames, lengths, "baseline.keys." + args[0], answers);
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
