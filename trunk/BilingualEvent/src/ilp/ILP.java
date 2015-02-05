package ilp;

/* demo.java */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import coref.ToSemEval;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;

public class ILP {

	ArrayList<EventMention> eventMentions;
	HashMap<String, Double> eventCorefMaps;

	HashSet<String> negativeConstraint;

	int numberOfEvents = 0;

	HashMap<EventMention[], Integer> corefOutput = new HashMap<EventMention[], Integer>();

	public ILP(ArrayList<EventMention> mentions,
			HashMap<String, Double> confMap, HashSet<String> negativeConstraint) {
		// read trigger type probabilities
		this.eventMentions = mentions;
		this.eventCorefMaps = confMap;
		this.numberOfEvents = this.eventMentions.size();
//		System.err.println("S:" + numberOfEvents);
		// read maxent coref probabilities
		this.negativeConstraint = negativeConstraint;
	}

	HashMap<String, Double> probMap = new HashMap<String, Double>();

	HashMap<String, Integer> nameMap = new HashMap<String, Integer>();

	public int execute() throws LpSolveException {
		LpSolve lp;
		int Ncol, m, ret = 0;

		/*
		 * We will build the model row by row So we start with creating a model
		 * with 0 rows and 2 columns
		 */
		Ncol = numberOfEvents * 34 + numberOfEvents * (numberOfEvents - 1) / 2; /* there are two variables in the model */
		if (Ncol == 0) {
			return 0;
		}
		/* create space large enough for one row */
		int[] colno = new int[Ncol * 2];
		double[] row = new double[Ncol * 2];

		lp = LpSolve.makeLp(0, Ncol);
		if (lp.getLp() == 0)
			ret = 1; /* couldn't construct a new model... */

		// set binary
		for (int i = 1; i < Ncol; i++) {
			lp.setBinary(i, true);
		}
		if (ret == 0) {
			/*
			 * let us name our variables. Not required, but can be usefull for
			 * debugging
			 */
			int vNo = 1;

			for (int i = 0; i < numberOfEvents; i++) {
				for (int k = 1; k <= 34; k++) {
					String name = "y(" + i + "," + k + ")";
					lp.setColName(vNo, name);
					nameMap.put(name, vNo);
					probMap.put(name,
							this.eventMentions.get(i).subTypeConfidences.get(k - 1));
					vNo++;
				}
			}

			for (int j = 0; j < numberOfEvents; j++) {
				for (int i = 0; i < j; i++) {
					String name = "z(" + i + "," + j + ")";
					lp.setColName(vNo, name);
					nameMap.put(name, vNo);
					probMap.put(name, this.eventCorefMaps.get(this.eventMentions.get(i).toName() + " " + this.eventMentions.get(j).toName()));
					vNo++;
				}
			}

			lp.setAddRowmode(true);
		}
		// constraint 1: only one type & has type <=> trigger
		if (ret == 0) {
			/* construct xi=sum y(i, k) over all k */
			for (int i = 0; i < numberOfEvents; i++) {
				m = 0;
//				int yi34 = nameMap.get("y(" + i + ",34)");
//				colno[m] = yi34;
//				row[m++] = 1;
				for (int k = 1; k <= 34; k++) {
					int yik = nameMap.get("y(" + i + "," + k + ")");
					colno[m] = yik;
					row[m++] = 1;
				}
				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);
			}
		}

		// constraint 2.a: if coreference, then trigger
		if (ret == 0) {
			/* construct z(i, j) + Zij <= 1 */
			for (int i = 0; i < numberOfEvents; i++) {
				int yi34 = nameMap.get("y(" + i + ",34)");
				for (int j = i + 1; j < numberOfEvents; j++) {
					m = 0;
					colno[m] = yi34;
					row[m++] = 1;
					int zij = nameMap.get("z(" + i + "," + j + ")");
					colno[m] = zij;
					row[m++] = 1;
					/* add the row to lp_solve */
					lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
				}
			}
		}

		// constraint 2.b: if coreference, then trigger
		if (ret == 0) {
			/* construct z(i, j) + Zij <= 1 */
			for (int j = 0; j < numberOfEvents; j++) {
				int yj34 = nameMap.get("y(" + j + ",34)");
				for (int i = 0; i < j; i++) {
					m = 0;
					colno[m] = yj34;
					row[m++] = 1;
					int zij = nameMap.get("z(" + i + "," + j + ")");
					colno[m] = zij;
					row[m++] = 1;
					/* add the row to lp_solve */
					lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
				}
			}
		}
//		// constraint 3.a: if coreference, then type equal
		if (ret == 0) {
			/* construct 1 - zij >= yik - yjk, for all k */
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = 1; k <= 34; k++) {
						int yik = nameMap.get("y(" + i + "," + k + ")");
						int yjk = nameMap.get("y(" + j + "," + k + ")");

						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = yik;
						row[m++] = 1;

						colno[m] = yjk;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 3.b: if coreference, then type equal
		if (ret == 0) {
			/* construct 1 - zij >= yjk - yik, for all k */
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = 1; k <= 34; k++) {
						int yik = nameMap.get("y(" + i + "," + k + ")");
						int yjk = nameMap.get("y(" + j + "," + k + ")");

						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = yjk;
						row[m++] = 1;

						colno[m] = yik;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// // constraint 4.a: if transitive constraint
//		if (ret == 0) {
//			/* construct z(i,j)+z(j,k)-z(i,k)<=1 */
//			for (int i = 0; i < numberOfEvents; i++) {
//				for (int j = i + 1; j < numberOfEvents; j++) {
//					int zij = nameMap.get("z(" + i + "," + j + ")");
//
//					for (int k = j + 1; k < numberOfEvents; k++) {
//						int zjk = nameMap.get("z(" + j + "," + k + ")");
//						int zik = nameMap.get("z(" + i + "," + k + ")");
//						m = 0;
//						colno[m] = zij;
//						row[m++] = 1;
//
//						colno[m] = zjk;
//						row[m++] = 1;
//
//						colno[m] = zik;
//						row[m++] = -1;
//
//						/* add the row to lp_solve */
//						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
//					}
//				}
//			}
//		}
//
//		// constraint 4.b: if transitive constraint
//		if (ret == 0) {
//			/* construct z(i,j)+z(i,k)-z(j,k)<=1 */
//			for (int i = 0; i < numberOfEvents; i++) {
//				for (int j = i + 1; j < numberOfEvents; j++) {
//					int zij = nameMap.get("z(" + i + "," + j + ")");
//
//					for (int k = j + 1; k < numberOfEvents; k++) {
//						int zjk = nameMap.get("z(" + j + "," + k + ")");
//						int zik = nameMap.get("z(" + i + "," + k + ")");
//						m = 0;
//						colno[m] = zij;
//						row[m++] = 1;
//
//						colno[m] = zik;
//						row[m++] = 1;
//
//						colno[m] = zjk;
//						row[m++] = -1;
//
//						/* add the row to lp_solve */
//						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
//					}
//				}
//			}
//		}
//
//		// constraint 4.c: if transitive constraint
//		if (ret == 0) {
//			/* construct z(i,k)+z(j,k)-z(i,j)<=1 */
//			for (int i = 0; i < numberOfEvents; i++) {
//				for (int j = i + 1; j < numberOfEvents; j++) {
//					int zij = nameMap.get("z(" + i + "," + j + ")");
//
//					for (int k = j + 1; k < numberOfEvents; k++) {
//						int zjk = nameMap.get("z(" + j + "," + k + ")");
//						int zik = nameMap.get("z(" + i + "," + k + ")");
//						m = 0;
//						colno[m] = zik;
//						row[m++] = 1;
//
//						colno[m] = zjk;
//						row[m++] = 1;
//
//						colno[m] = zij;
//						row[m++] = -1;
//
//						/* add the row to lp_solve */
//						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
//					}
//				}
//			}
//		}

		// constraint 8: best first constraint
//		 if (ret == 0) {
//		 /* construct z(i,k)+z(j,k)-z(i,j)<=1 */
//		 for (int j = 0; j < numberOfEvents ; j++) {
//		 m = 0;
//		 for (int i = j - 1; i >= 0; i--) {
//		 int zij = nameMap.get("z(" + i + "," + j + ")");
//		
//		 colno[m] = zij;
//		 row[m++] = 1;
//		
//		 }
//		 /* add the row to lp_solve */
//		 lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
//		 }
//		 }

//		// constraint 9: negative constraint
////		System.err.println("Neg:" + this.negativeConstraint.size());
//		if (ret == 0) {
//			for (int j = 0; j < numberOfEvents; j++) {
//				for (int i = j - 1; i >= 0; i--) {
//					m = 0;
//					EventMention m1 = this.eventMentions.get(i);
//					EventMention m2 = this.eventMentions.get(j);
//					String pair = m1.getAnchorStart() + "," + m1.getAnchorEnd()
//							+ "," + m2.getAnchorStart() + ","
//							+ m2.getAnchorEnd();
//					if (this.negativeConstraint.contains(pair)) {
//						int zij = nameMap.get("z(" + i + "," + j + ")");
//						colno[m] = zij;
//						row[m++] = 1;
//						/* add the row to lp_solve */
//						lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
//					}
//				}
//			}
//		}

		HashMap<Integer, Double> obj = new HashMap<Integer, Double>();
		if (ret == 0) {
			/* set the objective function */
			m = 0;
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");
					
					double pij = (probMap.get("z(" + i + "," + j + ")") * 2.0) - 1;

					colno[m] = zij;
					row[m++] = pij * lamda / numberOfEvents;
					obj.put(zij, pij * lamda / numberOfEvents);
				}
			}
			for (int i = 0; i < numberOfEvents; i++) {
				for (int k = 1; k <= 34; k++) {
					int yik = nameMap.get("y(" + i + "," + k + ")");
					double pik = probMap.get("y(" + i + "," + k + ")");

					colno[m] = yik;
					row[m++] = pik * (1 - lamda);
					// obj.put(yik, pik * (1 - lemda));
				}
				// System.err.println("V:" + v);
			}
			/* set the objective in lp_solve */
			lp.setObjFnex(m, row, colno);
		}

		if (ret == 0) {
			lp.setAddRowmode(false); /*
									 * rowmode should be turned off again when
									 * done building the model
									 */
			/* set the object direction to maximize */
			lp.setMaxim();
			// lp.setMinim();
			/*
			 * just out of curioucity, now generate the model in lp format in
			 * file model.lp
			 */
			lp.writeLp("model.lp");
			// lp.writeMps("model.mps");

			/* I only want to see importand messages on screen while solving */
			lp.setVerbose(LpSolve.IMPORTANT);

			/* Now let lp_solve calculate a solution */
			ret = lp.solve();
			if (ret == LpSolve.OPTIMAL)
				ret = 0;
			else
				ret = 5;
		}
//		System.err.println("Return: " + ret);
		if (ret == 0) {
			/* a solution is calculated, now lets get some results */
			/* objective value */
//			System.err.println("Objective value: " + lp.getObjective());

			/* variable values */
			lp.getVariables(row);

			double sum = 0;
			for (Integer key : obj.keySet()) {
				double time = obj.get(key);
				double term = time * row[key.intValue() - 1];
				sum += term;
			}
//			System.err.println("left:\t" + sum);
//			System.err.println("right:\t" + (lp.getObjective() - sum));
			for (m = 0; m < Ncol; m++) {
//				System.out.println(lp.getColName(m + 1) + ": " + row[m]);

				String name = lp.getColName(m + 1);
				int a = name.indexOf("(");
				int b = name.indexOf(")");
				String content = name.substring(a + 1, b);
				double value = row[m];
				if (name.startsWith("x")) {
					int idx = Integer.valueOf(content);
					if (value == 0) {
						eventMentions.get(idx).confidence = -1;
					} else {
						eventMentions.get(idx).confidence = 1;
					}
				} else if (name.startsWith("y")) {
					String tokens[] = content.split(",");
					int idx = Integer.valueOf(tokens[0]);
					String subType = Util.subTypes.get(Integer
							.parseInt(tokens[1]) - 1);
					if (value == 1) {
						eventMentions.get(idx).subType = subType;
						
						if(!subType.equals("null")) {
							eventMentions.get(idx).confidence = 1;
						} else {
							eventMentions.get(idx).confidence = -1;
						}
					}
				} else if (name.startsWith("z")) {
					String tokens[] = content.split(",");
					EventMention m1 = eventMentions.get(Integer.parseInt(tokens[0]));
					EventMention m2 = eventMentions.get(Integer.parseInt(tokens[1]));
					EventMention pair[] = new EventMention[2];
					pair[0] = m1;
					pair[1] = m2;
					
					if (value == 1 && !m1.subType.equals("null")
							&& !m2.subType.equals("null")) {
						String key = m1.toName() + " " + m2.toName(); 
						corefs.add(key);
						this.corefOutput.put(pair, 1);
						if (!m1.subType.equals(m2.subType) || m1.confidence < 0
								|| m2.confidence < 0) {
//							System.err
//									.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
						}
					} else {
						this.corefOutput.put(pair, -1);
					}
				}
			}
			for (int i = 0; i < numberOfEvents; i++) {
				EventMention mention = eventMentions.get(i);
				if (mention.confidence > 0 && mention.subType.equals("null")) {
//					System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
				} else if (mention.confidence < 0
						&& !mention.subType.equals("null")) {
//					System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
				}
			}
			/* we are done now */
		}

		/* clean up such that all used memory by lp_solve is freeed */
		if (lp.getLp() != 0)
			lp.deleteLp();
		return (ret);
	}
	
	HashSet<String> corefs = new HashSet<String>(); 

	public void printResult(LpSolve lp) {

	}

	static double lamda = 0.76;

	private static HashMap<String, HashMap<String, Double>> loadProbs(String fn) {
		ArrayList<String> lines = Common.getLines(fn);
		HashMap<String, HashMap<String, Double>> maps = new HashMap<String, HashMap<String, Double>>();
		for (String line : lines) {
			String tks[] = line.split("\\s+");
			HashMap<String, Double> map = maps.get(tks[0]);
			if (map == null) {
				map = new HashMap<String, Double>();
				maps.put(tks[0], map);
			}
			map.put(tks[1] + " " + tks[2], Double.parseDouble(tks[3]) + .3);
		}
		return maps;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("java ~ folder lemda");
			System.exit(1);
		}
		
		Util.part = args[0];
		
		lamda = Double.parseDouble(args[1]);

		HashMap<String, HashMap<String, Double>> entityCorefProbMaps = loadProbs("entityProbs" + args[0]);
		HashMap<String, HashMap<String, Double>> eventCorefProbMaps = loadProbs("eventProbs" + args[0]);
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<ArrayList<EventChain>> answers = new ArrayList<ArrayList<EventChain>>(); 
		ArrayList<ArrayList<EventChain>> goldKeys = new ArrayList<ArrayList<EventChain>>(); 
		
		HashMap<String, HashMap<String, EventMention>> allEvents = new HashMap<String, HashMap<String, EventMention>>();
		
		int ev1 = 0;
		int ev2 = 0;
		
		for (int k = 0; k < files.size(); k++) {
//			System.err.println(k);
			String file = files.get(k);
			ACEDoc doc = new ACEChiDoc(file);
			
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());
			
			doc.docID = k;
			ArrayList<EventMention> events = Util.loadSystemComponents(doc);
			
			for(EventMention e : events) {
				if(!e.subType.equals("null")) {
					ev1 += 1;
				}
			}
			
			ArrayList<EventChain> answer = new ArrayList<EventChain>();
			if(events.size()==0) {
				answers.add(answer);
				goldKeys.add(doc.goldEventChains);
				continue;
			}
			Collections.sort(events);	
			HashSet<String> negativeConstraint = new HashSet<String>();

			HashMap<String, Double> eventCorefProbMap = eventCorefProbMaps.get(file);
			HashMap<String, Double> entityCorefProbMap = entityCorefProbMaps.get(file);
			
			ILP ilp = new ILP(events, eventCorefProbMap, negativeConstraint);
			ilp.execute();

			HashMap<EventMention[], Integer> corefOutput = ilp.corefOutput;
			ArrayList<String> ilpPred = new ArrayList<String>();
			ArrayList<String> ilpExtend = new ArrayList<String>();
			for (EventMention[] pair : corefOutput.keySet()) {
				StringBuilder sb = new StringBuilder();
				sb.append(pair[0].getAnchorStart()).append(",")
						.append(pair[0].getAnchorEnd()).append(",")
						.append(pair[1].getAnchorStart()).append(",")
						.append(pair[1].getAnchorEnd());
				ilpExtend.add(sb.toString());
				ilpPred.add(Integer.toString(corefOutput.get(pair)));
			}
			
			HashMap<String, EventChain> chainMap = new HashMap<String, EventChain>();
			for(int i=0;i<events.size();i++) {
				if(events.get(i).subType.equals("null")) {
					continue;
				}
				boolean find = false;
				for(int j=i-1;j>=0;j--) {
					String key = events.get(j).toName() + " " + events.get(i).toName();
					if(ilp.corefs.contains(key)) {
						find = true;
						EventChain ec = chainMap.get(events.get(j).toName());
						ec.addEventMention(events.get(i));
						chainMap.put(events.get(i).toName(), ec);
						break;
					}
				}
				if(!find) {
					EventChain ec = new EventChain();
					ec.addEventMention(events.get(i));
					chainMap.put(events.get(i).toName(), ec);
					answer.add(ec);
				}
			}
			answers.add(answer);
			goldKeys.add(doc.goldEventChains);
			
			HashMap<String, EventMention> eventMap = new HashMap<String, EventMention>();
			allEvents.put(file, eventMap);
			
			for(EventMention event : events) {
				eventMap.put(event.toName(), event);
				if(!event.subType.equals("null")) {
						ev2 += 1;
				}
			}
		}
		
		ToSemEval.outputSemFormat(fileNames, lengths, "event.ilp." + args[0], answers);
		ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys." + args[0], goldKeys);
		
		Util.outputResult(allEvents, "ilp_svm/result0");
		System.out.println("Before: " + ev1);
		System.out.println("After: " + ev2);
		// ACECommon.outputResult(ILPUtil.systemEMses,
		// "/users/yzcchen/workspace/NAACL2013-B/src/joint_ilp/result"
		// + Common.part);
		System.out.println("ILP Done.");
	}
}
