package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEDoc;
import model.ACEEngDoc;
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
import event.postProcess.AttriEvaluate;
import event.triggerEng.EngArgEval;

public class EventCorefTest {

	public static void main(String args[]) throws Exception {
		
		Util.part = "0";
		LinearClassifier<String, String> classifier = LinearClassifier
				.readClassifier("stanfordClassifier.gz");

		ArrayList<String> files = Common.getLines("ACE_English_test0");
		EventCorefFea fea = new EventCorefFea(false, "corefFea");
		double thres = .3;
		ArrayList<ArrayList<EventChain>> answers = new ArrayList<ArrayList<EventChain>>(); 
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();

		HashMap<String, HashMap<String, EventMention>> jointSVMLines = EngArgEval.jointSVMLine();
		
		HashMap<String, HashMap<String, String>> polarityMaps = AttriEvaluate.loadSystemAttri("polarity", "0");
		HashMap<String, HashMap<String, String>> modalityMaps = AttriEvaluate.loadSystemAttri("modality", "0");
		HashMap<String, HashMap<String, String>> genericityMaps = AttriEvaluate.loadSystemAttri("genericity", "0");
		HashMap<String, HashMap<String, String>> tenseMaps = AttriEvaluate.loadSystemAttri("tense", "0");
		
		for (String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());
			
			HashMap<String, EventMention> evmMaps = jointSVMLines.get(file);
			
			if(evmMaps==null) {
				evmMaps = new HashMap<String, EventMention>();
			}
			
			ArrayList<EventMention> ems = doc.goldEventMentions;
//			ArrayList<EventMention> ems = new ArrayList<EventMention>(evmMaps.values());
			
			Collections.sort(ems);
			
			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();

//			Util.setSystemAttribute(ems, polarityMaps, modalityMaps, genericityMaps, tenseMaps, file);
			
			for (int i = 0; i < ems.size(); i++) {
				EventMention ana = ems.get(i);

				int corefID = -1;
				double maxVal = 0;
				for (int j = activeChains.size() - 1; j >= 0; j--) {
					EventChain ec = activeChains.get(j);

					fea.configure(ec, ana, doc, ems);
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
		ToSemEval.outputSemFormat(fileNames, lengths, "baseline.keys.all", answers);
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
