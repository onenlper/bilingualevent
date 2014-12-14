package entity.coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.ACEChiDoc;
import model.ACEDoc;
import model.Entity;
import model.EntityMention;
import util.Common;
import util.Util;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.Datum;
import event.supercoref.EventCorefFea;

public class EntityCorefTrain {

	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ part");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_train" + args[0]);
		ArrayList<String> trainLines = new ArrayList<String>();
		
		List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();
		ACECorefFeature fea = new ACECorefFeature(true, "entityCoref");
		
		for(String file : files) {
			if(!file.equals("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese/nw/adj/XIN20001020.0200.0006")) {
//				continue;
			}
			System.out.println(file);
			
			ACEDoc doc = new ACEChiDoc(file);
			ArrayList<EntityMention> ems = doc.goldEntityMentions;
			Util.calEntityMentionAttribute(ems, doc);
			Collections.sort(ems);
			
			for(int i=0;i<ems.size();i++) {
				
				EntityMention ana = ems.get(i);
				boolean anaphor = false;
				for(int j=i-1;j>=0;j--) {
					EntityMention ant = ems.get(j);
					boolean coref = doc.entityCorefMap.get(ana.toName())==doc.entityCorefMap.get(ant.toName());
					if(coref) {
						anaphor = true;
					}
				}
				
				if(!anaphor) {
					continue;
				}
				
				for(int j=i-1;j>=0;j--) {
					EntityMention ant = ems.get(j);
					boolean coref = doc.entityCorefMap.get(ana.toName())==doc.entityCorefMap.get(ant.toName());
					
					fea.configure(ant, ana, doc);
					String svm = fea.getSVMFormatString();
					if(coref) {
						svm = "+1 " + svm;
					} else {
						svm = "-1 " + svm;
					}
					trainingData.add(Dataset.svmLightLineToDatum(svm));
					trainLines.add(svm);
				}
					
			}
		}
		Common.outputLines(trainLines, "entityTrain" + args[0]);
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
		LinearClassifier.writeClassifier(classifier, "stanfordClassifierEntity" + args[0] + ".gz");
		fea.freeze();
	}
}
