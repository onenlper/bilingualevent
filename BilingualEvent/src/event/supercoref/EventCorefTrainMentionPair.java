package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.Datum;

public class EventCorefTrainMentionPair {

	public static void main(String args[]) {
		if(args.length==0) {
			System.out.println("java ~ part");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_train" + args[0]);
		EventCorefFea fea = new EventCorefFea(true, "corefFea" + args[0]);
		ArrayList<String> trainLines = new ArrayList<String>();
		
		List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();
		
		for(String file : files) {
			ACEDoc doc = new ACEChiDoc(file);
			ArrayList<EventMention> ems = doc.goldEventMentions;
			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();
			Collections.sort(ems);
			
			for(int i=0;i<ems.size();i++) {
				EventMention ana = ems.get(i);
				
				int corefID = -1;
				for(int j=activeChains.size()-1;j>=0;j--) {
					EventChain ec = activeChains.get(j);
					EventMention lem = ec.getEventMentions().get(ec.getEventMentions().size()-1);
					
					boolean coref = doc.eventCorefMap.get(ana.toName())==doc.eventCorefMap.get(lem.toName());
					if(coref) {
						corefID = j;
					}
					fea.configure(ec, ana, doc, ems);
					
					String feaStr = fea.getSVMFormatString();
					String svm = "";
					if(coref) {
						trainLines.add("1 " + feaStr);
						svm = "+1 " + feaStr;	
					} else {
						trainLines.add("2 " + feaStr);
						svm = "-1 " + feaStr;
					}
					trainingData.add(Dataset.svmLightLineToDatum(svm));
				}
					
//				if(corefID==-1) {
					EventChain ec = new EventChain();
					ec.addEventMention(ana);
					activeChains.add(ec);
//				} else {
//					activeChains.get(corefID).addEventMention(ana);
//				}
			}
		}
		fea.freeze();
		Common.outputLines(trainLines, "eventTrain" + args[0]);
		
		if(args[1].equals("maxent")) {
			System.out.println("Train model...");
			LinearClassifierFactory<String, String> factory = new LinearClassifierFactory<String, String>();
			factory.useConjugateGradientAscent();
			// Turn on per-iteration convergence updates
			factory.setVerbose(false);
			// Small amount of smoothing
			factory.setSigma(1);
	
			LinearClassifier<String, String> classifier = factory
					.trainClassifier(trainingData);
	//		classifier.dump();
			LinearClassifier.writeClassifier(classifier, "stanfordClassifier" + args[0] + ".gz");
		}
	}
}
