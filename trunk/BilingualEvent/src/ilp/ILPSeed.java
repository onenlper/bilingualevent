package ilp;

/* demo.java */

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import model.ACEChiDoc;
import model.ACEDoc;
import model.Entity;
import model.EntityMention;
import model.EventChain;
import model.EventMention;
import model.EventMentionArgument;
import seeds.SeedUtil;
import util.Common;
import util.Util;
import coref.EMLearnSeed;
import coref.ToSemEval;
import entity.semantic.SemanticTrainMultiSeed;
import event.argument.JointArgumentSeed;

public class ILPSeed {

	ArrayList<EventMention> eventMentions;
	HashMap<String, Double> eventCorefMaps;

	ArrayList<EntityMention> entityMentions;
	HashMap<String, Double> entityCorefMaps;

	int numberOfEvents = 0;
	int numberOfEntities = 0;
	HashSet<String> eventCorefs = new HashSet<String>();
	HashSet<String> entityCorefs = new HashSet<String>();

	HashMap<EventMention[], Integer> corefOutput = new HashMap<EventMention[], Integer>();

	ArrayList<ArrayList<Integer>> sameBVs;

	HashMap<String, Integer> eventPositionMap;
	HashMap<String, Integer> entityPositionMap;
	HashMap<String, Integer> argPositionMap;

	int numberOfArgs = 0;

	ArrayList<EventMentionArgument> args = new ArrayList<EventMentionArgument>();
	
	ACEDoc doc;
	
	public ILPSeed(
			ACEDoc doc,
			ArrayList<EventMention> eventMs,
			HashMap<String, Double> confMap,
			HashMap<String, ArrayList<String>> sameBVsStr,
			ArrayList<EntityMention> entityMentions,
			HashMap<String, Double> confMap2) {
		// read trigger type probabilities
		this.doc = doc;
		this.eventMentions = eventMs;
		this.eventCorefMaps = confMap;

		this.entityMentions = entityMentions;
		this.entityCorefMaps = confMap2;

		this.numberOfEvents = this.eventMentions.size();
		this.numberOfEntities = this.entityMentions.size();

		// System.err.println("S:" + numberOfEvents);
		// read maxent coref probabilities

		this.eventPositionMap = new HashMap<String, Integer>();
		for (int i = 0; i < eventMs.size(); i++) {
			EventMention event = eventMs.get(i);
			eventPositionMap.put(event.toName(), i);
			Collections.sort(event.getEventMentionArguments());
			numberOfArgs += event.getEventMentionArguments().size();
			args.addAll(event.getEventMentionArguments());
		}

		this.argPositionMap = new HashMap<String, Integer>();
		for (int i = 0; i < args.size(); i++) {
			EventMentionArgument arg = args.get(i);
			argPositionMap.put(
					arg.getEventMention().toName() + " " + arg.toString(), i);
		}

		this.entityPositionMap = new HashMap<String, Integer>();
		for (int i = 0; i < entityMentions.size(); i++) {
			entityPositionMap.put(entityMentions.get(i).toName(), i);
		}

		this.sameBVs = new ArrayList<ArrayList<Integer>>();

		for (String key : sameBVsStr.keySet()) {
			ArrayList<String> lst = sameBVsStr.get(key);
			ArrayList<Integer> sameBV = new ArrayList<Integer>();
			for (String s : lst) {
				sameBV.add(eventPositionMap.get(s));
			}
			sameBVs.add(sameBV);
		}
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
		Ncol = numberOfEvents * 34 + numberOfEvents * (numberOfEvents - 1)
				+ numberOfEntities * (numberOfEntities - 1) / 2 + numberOfArgs
				* JointArgumentSeed.roles.size()
				+ numberOfEntities * (SemanticTrainMultiSeed.semClasses.size())
				; /* there are three variables in the model */
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
			defineVariable(lp);
		}
		
		basicConstraints(lp, ret, colno, row);

		eventCorefThenTriggerConstraint(lp, ret, colno, row);

		addEventTypeSame(lp, ret, colno, row);

		sameTriggerWord(lp, ret, colno, row);

		addEventTransitivityConstraint(lp, ret, colno, row);
		
		addEntityTransitivityConstraint(lp, ret, colno, row);
		
		List<String> discreteRoles = new ArrayList<String>(Arrays.asList(
				"Place", "Org", "Position", "Adjudicator", "Origin", "Giver",
				"Recipient", "Defendant", "Agent", "Person"
		// "Prosecutor"
				));
		
		// // constraint 9, two arguments not coref => event not coref
//		addEntityCorefThenEventCoref(lp, colno, row, discreteRoles);
		
		addRoleToEntityConstraints(lp, colno, row);
		setObjective(lp, ret, colno, row);

		ret = solveILP(lp, ret);
		// System.err.println("Return: " + ret);
		collectResult(lp, Ncol, ret, row);

		/* clean up such that all used memory by lp_solve is freeed */
		if (lp.getLp() != 0)
			lp.deleteLp();
		return (ret);
	}

	private void easyEnityCorefToEventCoref(int[] colno, double[] row,
			List<String> discreteRoles) {
		int m;
		for (int i = 0; i < this.args.size(); i++) {
			EventMentionArgument arg1 = this.args.get(i);
			for (int j = i + 1; j < this.args.size(); j++) {
				EventMentionArgument arg2 = this.args.get(j);

				if (arg1.role.equals(arg2.role) && !arg1.role.equals("null")
						&& discreteRoles.contains(arg1.role)) {
					int e1 = this.eventPositionMap.get(arg1.getEventMention()
							.toName());
					int e2 = this.eventPositionMap.get(arg2.getEventMention()
							.toName());

					Integer m1 = this.entityPositionMap.get(arg1.toString());
					Integer m2 = this.entityPositionMap.get(arg2.toString());

					if (m1 != null && m2 != null && e1 != e2 && m1 < m2) {
						EntityMention entityMention1 = this.entityMentions
								.get(m1);
						EntityMention entityMention2 = this.entityMentions
								.get(m2);
						int zij = nameMap.get("z(" + (e1 < e2 ? e1 : e2) + ","
								+ (e1 < e2 ? e2 : e1) + ")");
						int eij = nameMap.get("e(" + m1 + "," + m2 + ")");
						m = 0;
						colno[m] = zij;
						row[m++] = 1;
						// double probEij = probMap.get("e(" + m1 + "," + m2 +
						// ")");
						// if(probEij<0) {
						// lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
						// }
						colno[m] = eij;
						row[m++] = -1;
//						 lp.addConstraintex(m, row, colno, LpSolve.GE, 0);
//						 lp.addConstraintex(m, row, colno, LpSolve.GE, 0);
					}
				}
			}
		}
	}

	private void defineVariable(LpSolve lp) throws LpSolveException {
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
						this.eventMentions.get(i).subTypeConfidences
								.get(k - 1));
				vNo++;
			}
		}

		for (int j = 0; j < numberOfEvents; j++) {
			for (int i = 0; i < j; i++) {
				String name = "z(" + i + "," + j + ")";
				lp.setColName(vNo, name);
				nameMap.put(name, vNo);

				String key = this.eventMentions.get(i).toName() + " "
						+ this.eventMentions.get(j).toName();
				if(this.eventCorefMaps.containsKey(key)) {
					probMap.put(name, 1.0 * this.eventCorefMaps
							.get(this.eventMentions.get(i).toName() + " "
									+ this.eventMentions.get(j).toName()));
				} else {
					probMap.put(name, 1.0);
					System.out.println(this.eventMentions.get(i).toName() + " "
							+ this.eventMentions.get(j).toName() + ":" + this.eventMentions.get(i).getAnchor()+ "." 
							+ this.eventMentions.get(j).getAnchor());
				}
				vNo++;
			}
		}

		for (int j = 0; j < numberOfEntities; j++) {
			for (int i = 0; i < j; i++) {
				String name = "e(" + i + "," + j + ")";
				lp.setColName(vNo, name);
				nameMap.put(name, vNo);
				String key = this.entityMentions.get(i).toName() + " "
						+ this.entityMentions.get(j).toName();

				EntityMention m1 = this.entityMentions.get(i);
				EntityMention m2 = this.entityMentions.get(j);
				
				if(!this.entityCorefMaps.containsKey(key)) {
//						System.out.println(doc.content.substring(m1.headStart, m1.headEnd+1) + "," + m1.toName());
//						System.out.println(doc.content.substring(m2.headStart, m2.headEnd+1) + "," + m2.toName());
//						System.out.println("========");
					probMap.put(name, 0.0);
				} else {
					probMap.put(name, 1.0 * this.entityCorefMaps.get(key));
				}
				vNo++;
			}
		}

		for (int i = 0; i < this.args.size(); i++) {
			EventMentionArgument arg = this.args.get(i);
			for (int k = 1; k <= JointArgumentSeed.roles.size(); k++) {
				String name = "r(" + i + "," + k + ")";
				lp.setColName(vNo, name);
				nameMap.put(name, vNo);
				probMap.put(name, arg.roleConfidences.get(k - 1));
				vNo++;
			}
		}

		for (int j = 0; j < numberOfEvents; j++) {
			for (int i = 0; i < j; i++) {
				String name = "c(" + i + "," + j + ")";
				lp.setColName(vNo, name);
				nameMap.put(name, vNo);
				vNo++;
			}
		}
		
		for(int i=0;i<numberOfEntities;i++) {
			EntityMention m = this.entityMentions.get(i);
			for(int k=1;k<=SemanticTrainMultiSeed.semClasses.size();k++) {
				String name = "s(" + i + "," + k + ")";
				lp.setColName(vNo, name);
				nameMap.put(name, vNo);
				probMap.put(name, m.semClassConf.get(k-1));
				vNo++;
			}
		}
		
		lp.setAddRowmode(true);
	}

	private void collectResult(LpSolve lp, int Ncol, int ret, double[] row)
			throws LpSolveException {
		int m;
		if (ret == 0) {
			/* a solution is calculated, now lets get some results */
			/* objective value */
			// System.err.println("Objective value: " + lp.getObjective());

			/* variable values */
			lp.getVariables(row);

			for (m = 0; m < Ncol; m++) {
				// System.out.println(lp.getColName(m + 1) + ": " + row[m]);
				String name = lp.getColName(m + 1);
				int a = name.indexOf("(");
				int b = name.indexOf(")");
				String content = name.substring(a + 1, b);
				double value = row[m];
				if (name.startsWith("r")) {
					String tokens[] = content.split(",");
					int idx = Integer.valueOf(tokens[0]);
					String role = JointArgumentSeed.roles
							.get(Integer.parseInt(tokens[1]) - 1);
					if (value == 1) {
						this.args.get(idx).role = role;
						if (!role.equals("null")) {
							this.args.get(idx).confidence = 1;
						} else {
							this.args.get(idx).confidence = -1;
						}
					}
				} else if (name.startsWith("y")) {
					String tokens[] = content.split(",");
					int idx = Integer.valueOf(tokens[0]);
					String subType = Util.subTypes.get(Integer
							.parseInt(tokens[1]) - 1);
					if (value == 1) {
						eventMentions.get(idx).subType = subType;

						if (!subType.equals("null")) {
							eventMentions.get(idx).confidence = 1;
						} else {
							eventMentions.get(idx).confidence = -1;
						}
					}
				} else if (name.startsWith("z")) {
					String tokens[] = content.split(",");
					EventMention m1 = eventMentions.get(Integer
							.parseInt(tokens[0]));
					EventMention m2 = eventMentions.get(Integer
							.parseInt(tokens[1]));
					EventMention pair[] = new EventMention[2];
					pair[0] = m1;
					pair[1] = m2;

					if (value == 1 && !m1.subType.equals("null")
							&& !m2.subType.equals("null")) {
						String key = m1.toName() + " " + m2.toName();
						eventCorefs.add(key);
						this.corefOutput.put(pair, 1);
						if (!m1.subType.equals(m2.subType) || m1.confidence < 0
								|| m2.confidence < 0) {
							System.err
									.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
						}
					} else {
						this.corefOutput.put(pair, -1);
					}
				} else if (name.startsWith("e")) {
					String tokens[] = content.split(",");
					EntityMention m1 = entityMentions.get(Integer
							.parseInt(tokens[0]));
					EntityMention m2 = entityMentions.get(Integer
							.parseInt(tokens[1]));
					EntityMention pair[] = new EntityMention[2];
					pair[0] = m1;
					pair[1] = m2;
					String key = m1.toName() + " " + m2.toName();
					if (value == 1) {
						this.entityCorefs.add(key);
					}
				}
			}
			for (int i = 0; i < numberOfEvents; i++) {
				EventMention mention = eventMentions.get(i);
				if (mention.confidence > 0 && mention.subType.equals("null")) {
					// System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
				} else if (mention.confidence < 0
						&& !mention.subType.equals("null")) {
					// System.err.println("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
				}
			}
			/* we are done now */
		}
	}

	private int solveILP(LpSolve lp, int ret) throws LpSolveException {
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
		return ret;
	}

	private void setObjective(LpSolve lp, int ret, int[] colno, double[] row)
			throws LpSolveException {
		int m;
		// TODO OBJECTIVE FUNCTION
//		HashMap<Integer, Double> obj = new HashMap<Integer, Double>();
		if (ret == 0) {
			/* set the objective function */
			m = 0;
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");
					double pij = probMap.get("z(" + i + "," + j + ")") - 0.5;
					colno[m] = zij;
					row[m++] = (pij + 2) * lamda;
				}
			}

			// for (int i = 0; i < numberOfEvents; i++) {
			// for (int j = i + 1; j < numberOfEvents; j++) {
			// int zij = nameMap.get("z(" + i + "," + j + ")");
			// double pij = (probMap.get("z(" + i + "," + j + ")") * 2.0) - 1;
			// colno[m] = zij;
			// row[m++] = pij * lamda / numberOfEvents;
			// obj.put(zij, pij * lamda / numberOfEvents);
			// }
			// }

			for (int i = 0; i < numberOfEntities; i++) {
				for (int j = i + 1; j < numberOfEntities; j++) {
					int rij = nameMap.get("e(" + i + "," + j + ")");
					double pij = probMap.get("e(" + i + "," + j + ")") - 0.5;

					colno[m] = rij;
					row[m++] = pij * beta;
				}
			}

			for (int i = 0; i < numberOfEvents; i++) {
				for (int k = 1; k <= 34; k++) {
					int yik = nameMap.get("y(" + i + "," + k + ")");
					double pik = probMap.get("y(" + i + "," + k + ")");

					colno[m] = yik;
					row[m++] = pik * (1 - lamda);
				}
			}

			for (int i = 0; i < this.numberOfArgs; i++) {
				for (int k = 1; k <= JointArgumentSeed.roles.size(); k++) {
					int rik = nameMap.get("r(" + i + "," + k + ")");
					double pik = probMap.get("r(" + i + "," + k + ")");

					colno[m] = rik;
					row[m++] = pik * gamma;
				}
			}
			
			for(int i=0;i<this.numberOfEntities;i++) {
				for(int k=1;k<=SemanticTrainMultiSeed.semClasses.size();k++) {
					int sik = nameMap.get("s(" + i + "," + k + ")");
					double pik = probMap.get("s(" + i + "," + k + ")");
					
					colno[m] = sik;
					row[m++] = pik * theta;
				}
			}
			
			/* set the objective in lp_solve */
			lp.setObjFnex(m, row, colno);
		}
	}
	
	HashMap<String, HashSet<String>> roleToEntityConstraints;
	
	private void addRoleToEntityConstraints(LpSolve lp, int[] colno,
			double[] row) throws LpSolveException {
		if(roleToEntityConstraints==null) {
			roleToEntityConstraints = SeedUtil.loadRoleToSemConstraints();
		}
		for(int i=0;i<this.args.size();i++) {
			EventMentionArgument arg1 = this.args.get(i);
			
			int e1 = this.eventPositionMap.get(arg1.getEventMention()
					.toName());
			
			Integer m1 = this.entityPositionMap.get(arg1.toString());
			
			for(String key : roleToEntityConstraints.keySet()) {
				String eventSubType = key.split("\\s+")[0];
				String role = key.split("\\s+")[1];
				int m = 0;
				int yik = nameMap.get("y(" + e1 + "," + (Util.subTypes.indexOf(eventSubType)+1) + ")");
				colno[m] = yik;
				row[m++] = 1;
				
				int rik = nameMap.get("r(" + e1 + "," + (JointArgumentSeed.roles.indexOf(role)+1) + ")");
				colno[m] = rik;
				row[m++] = 1;
				
				HashSet<String> semClasses = roleToEntityConstraints.get(key);
				for(String semClass : semClasses) {
//					System.out.println("s(" + m1 + "," + (SemanticTrainMultiSeed.semClasses.indexOf(semClass)+1) + ")");
					int sik = nameMap.get("s(" + m1 + "," + (SemanticTrainMultiSeed.semClasses.indexOf(semClass)+1) + ")");
					colno[m] = sik;
					row[m++] = -1;
				}
				lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
			}
		}
	}

	private void addEntityCorefThenEventCoref(LpSolve lp, int[] colno,
			double[] row, List<String> discreteRoles) throws LpSolveException {
		int m;
		// constriant, rik + rjk - em1m2 + Ze1e2 <= 2
		for (String role : discreteRoles) {
			int k = JointArgumentSeed.roles.indexOf(role) + 1;

			for (int i = 0; i < this.args.size(); i++) {
				EventMentionArgument arg1 = this.args.get(i);
				for (int j = i + 1; j < this.args.size(); j++) {
					EventMentionArgument arg2 = this.args.get(j);

					if (arg1.getEventMention().confidence < 0
							|| arg2.getEventMention().confidence < 0) {
						// continue;
					}

					int e1 = this.eventPositionMap.get(arg1.getEventMention()
							.toName());
					int e2 = this.eventPositionMap.get(arg2.getEventMention()
							.toName());

					Integer m1 = this.entityPositionMap.get(arg1.toString());
					Integer m2 = this.entityPositionMap.get(arg2.toString());

					if (m1 != null && m2 != null && e1 != e2 && m1 < m2) {

						int zij = nameMap.get("z(" + (e1 < e2 ? e1 : e2) + ","
								+ (e1 < e2 ? e2 : e1) + ")");
						int eij = nameMap.get("e(" + m1 + "," + m2 + ")");

						m = 0;
						colno[m] = nameMap.get("r(" + i + "," + k + ")");
						row[m++] = 1;

						colno[m] = nameMap.get("r(" + j + "," + k + ")");
						row[m++] = 1;

						colno[m] = eij;
						row[m++] = 1;

						colno[m] = zij;
						row[m++] = -1;
						lp.addConstraintex(m, row, colno, LpSolve.LE, 2);

						m = 0;
						colno[m] = nameMap.get("r(" + i + "," + k + ")");
						row[m++] = 1;

						colno[m] = nameMap.get("r(" + j + "," + k + ")");
						row[m++] = 1;

						colno[m] = eij;
						row[m++] = -1;

						colno[m] = zij;
						row[m++] = 1;
//						lp.addConstraintex(m, row, colno, LpSolve.LE, 2);
					}
				}
			}
		}
	}

	private void eventCorefThenTriggerConstraint(LpSolve lp, int ret,
			int[] colno, double[] row) throws LpSolveException {
		int m;
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
	}

	private void sameTriggerWord(LpSolve lp, int ret, int[] colno, double[] row)
			throws LpSolveException {
		int m;
		// constraints 6 same BV has the same fate
		// yik == yjk, i+1=j
		if (ret == 0) {
			for (ArrayList<Integer> sameBV : sameBVs) {
				for (int e = 0; e < sameBV.size() - 1; e++) {
					int i = sameBV.get(e);
					int j = sameBV.get(e + 1);
					for (int k = 1; k <= 34; k++) {
						if (k != 34) {
							// continue;
						}
						m = 0;
						int yik = nameMap.get("y(" + i + "," + k + ")");
						int yjk = nameMap.get("y(" + j + "," + k + ")");
						colno[m] = yjk;
						row[m++] = 1;
						colno[m] = yik;
						row[m++] = -1;
						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
					}
				}
			}
		}
	}

	private void addEventTypeSame(LpSolve lp, int ret, int[] colno, double[] row)
			throws LpSolveException {
		int m;
		// // constraint 3.a: if coreference, then type equal
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
	}

	private void addNewConstraint(LpSolve lp, int[] colno, double[] row,
			List<String> discreteRoles) throws LpSolveException {
		
		for(int i=0;i<this.numberOfEvents;i++) {
			for(int j=i+1;j<this.numberOfEvents;j++) {
				EventMention e1 = this.eventMentions.get(i);
				EventMention e2 = this.eventMentions.get(j);
				
				for(EventMentionArgument arg1 : e1.eventMentionArguments) {
					for(EventMentionArgument arg2 : e2.eventMentionArguments) {
						
						Integer m1 = this.entityPositionMap.get(arg1.toString());
						Integer m2 = this.entityPositionMap.get(arg2.toString());
						
						if(m1!=null && m2!=null) {
							EntityMention em1 = this.entityMentions.get(m1);
							EntityMention em2 = this.entityMentions.get(m2);
							
							if(!em1.subType.equals(em2.subType)) {

								for(String role : JointArgumentSeed.roles) {
									if(role.equals("null")) {
										continue;
									}
									
									
									
								}
							}
						}
					}
				}
			}
		}
		
		
		int m;
		// constriant, rik + rjk - em1m2 + Ze1e2 <= 2
		for (String role : discreteRoles) {
			int k = JointArgumentSeed.roles.indexOf(role) + 1;

			for (int i = 0; i < this.args.size(); i++) {
				EventMentionArgument arg1 = this.args.get(i);
				for (int j = i + 1; j < this.args.size(); j++) {
					EventMentionArgument arg2 = this.args.get(j);

					if (arg1.getEventMention().confidence < 0
							|| arg2.getEventMention().confidence < 0) {
						// continue;
					}

					int e1 = this.eventPositionMap.get(arg1.getEventMention()
							.toName());
					int e2 = this.eventPositionMap.get(arg2.getEventMention()
							.toName());

					Integer m1 = this.entityPositionMap.get(arg1.toString());
					Integer m2 = this.entityPositionMap.get(arg2.toString());

					if (m1 != null && m2 != null && e1 != e2 && m1 < m2) {

						int zij = nameMap.get("z(" + (e1 < e2 ? e1 : e2) + ","
								+ (e1 < e2 ? e2 : e1) + ")");
						int eij = nameMap.get("e(" + m1 + "," + m2 + ")");

						m = 0;
						colno[m] = nameMap.get("r(" + i + "," + k + ")");
						row[m++] = 1;

						colno[m] = nameMap.get("r(" + j + "," + k + ")");
						row[m++] = 1;

						colno[m] = eij;
						row[m++] = 1;

						colno[m] = zij;
						row[m++] = -1;
//						lp.addConstraintex(m, row, colno, LpSolve.LE, 2);

						m = 0;
						colno[m] = nameMap.get("r(" + i + "," + k + ")");
						row[m++] = 1;

						colno[m] = nameMap.get("r(" + j + "," + k + ")");
						row[m++] = 1;

						colno[m] = eij;
						row[m++] = -1;

						colno[m] = zij;
						row[m++] = 1;
//						lp.addConstraintex(m, row, colno, LpSolve.LE, 2);
					}
				}
			}
		}
	}

	private void basicConstraints(LpSolve lp, int ret, int[] colno, double[] row)
			throws LpSolveException {
		int m;
		// constraint 1.a: only one type & has type <=> trigger
		if (ret == 0) {
			/* construct xi=sum y(i, k) over all k */
			for (int i = 0; i < numberOfEvents; i++) {
				m = 0;
				// int yi34 = nameMap.get("y(" + i + ",34)");
				// colno[m] = yi34;
				// row[m++] = 1;
				for (int k = 1; k <= 34; k++) {
					int yik = nameMap.get("y(" + i + "," + k + ")");
					colno[m] = yik;
					row[m++] = 1;
				}
				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);
			}
		}

		// constraint 1.b only one role & has role
		for (int i = 0; i < numberOfArgs; i++) {
			m = 0;
			for (int k = 1; k <= JointArgumentSeed.roles.size(); k++) {
				int rik = nameMap.get("r(" + i + "," + k + ")");
				colno[m] = rik;
				row[m++] = 1;
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);
		}
		
		// constraint 1.c only one semclass & has semclass
		for (int i = 0; i < numberOfEntities; i++) {
			m = 0;
			for (int k = 1; k <= SemanticTrainMultiSeed.semClasses.size(); k++) {
				int rik = nameMap.get("s(" + i + "," + k + ")");
				colno[m] = rik;
				row[m++] = 1;
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);
		}
	}

	private void addEventTransitivityConstraint(LpSolve lp, int ret,
			int[] colno, double[] row) throws LpSolveException {
		int m;
		// constraint 4.a: if transitive constraint
		if (ret == 0) {
			/* construct z(i,j)+z(j,k)-z(i,k)<=1 */
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = j + 1; k < numberOfEvents; k++) {
						int zjk = nameMap.get("z(" + j + "," + k + ")");
						int zik = nameMap.get("z(" + i + "," + k + ")");
						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = zjk;
						row[m++] = 1;

						colno[m] = zik;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 4.b: if transitive constraint
		if (ret == 0) {
			/* construct z(i,j)+z(i,k)-z(j,k)<=1 */
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = j + 1; k < numberOfEvents; k++) {
						int zjk = nameMap.get("z(" + j + "," + k + ")");
						int zik = nameMap.get("z(" + i + "," + k + ")");
						m = 0;
						colno[m] = zij;
						row[m++] = 1;

						colno[m] = zik;
						row[m++] = 1;

						colno[m] = zjk;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 4.c: if transitive constraint
		if (ret == 0) {
			/* construct z(i,k)+z(j,k)-z(i,j)<=1 */
			for (int i = 0; i < numberOfEvents; i++) {
				for (int j = i + 1; j < numberOfEvents; j++) {
					int zij = nameMap.get("z(" + i + "," + j + ")");

					for (int k = j + 1; k < numberOfEvents; k++) {
						int zjk = nameMap.get("z(" + j + "," + k + ")");
						int zik = nameMap.get("z(" + i + "," + k + ")");
						m = 0;
						colno[m] = zik;
						row[m++] = 1;

						colno[m] = zjk;
						row[m++] = 1;

						colno[m] = zij;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}
	}

	private void addEntityTransitivityConstraint(LpSolve lp, int ret, int[] colno,
			double[] row) throws LpSolveException {
		int m;
		// constraint 5.a: if entity transitive constraint
		if (ret == 0) {
			/* construct e(i,j)+e(j,k)-e(i,k)<=1 */
			for (int i = 0; i < numberOfEntities; i++) {
				for (int j = i + 1; j < numberOfEntities; j++) {
					int eij = nameMap.get("e(" + i + "," + j + ")");

					for (int k = j + 1; k < numberOfEntities; k++) {
						int ejk = nameMap.get("e(" + j + "," + k + ")");
						int eik = nameMap.get("e(" + i + "," + k + ")");
						m = 0;
						colno[m] = eij;
						row[m++] = 1;

						colno[m] = ejk;
						row[m++] = 1;

						colno[m] = eik;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 5.b: if entity transitive constraint
		if (ret == 0) {
			/* construct e(i,j)+e(i,k)-e(j,k)<=1 */
			for (int i = 0; i < numberOfEntities; i++) {
				for (int j = i + 1; j < numberOfEntities; j++) {
					int eij = nameMap.get("e(" + i + "," + j + ")");

					for (int k = j + 1; k < numberOfEntities; k++) {
						int ejk = nameMap.get("e(" + j + "," + k + ")");
						int eik = nameMap.get("e(" + i + "," + k + ")");
						m = 0;
						colno[m] = eij;
						row[m++] = 1;

						colno[m] = eik;
						row[m++] = 1;

						colno[m] = ejk;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}

		// constraint 5.c: if entity transitive constraint
		if (ret == 0) {
			/* construct e(i,k)+e(j,k)-e(i,j)<=1 */
			for (int i = 0; i < numberOfEntities; i++) {
				for (int j = i + 1; j < numberOfEntities; j++) {
					int eij = nameMap.get("e(" + i + "," + j + ")");

					for (int k = j + 1; k < numberOfEntities; k++) {
						int ejk = nameMap.get("e(" + j + "," + k + ")");
						int eik = nameMap.get("e(" + i + "," + k + ")");
						m = 0;
						colno[m] = eik;
						row[m++] = 1;

						colno[m] = ejk;
						row[m++] = 1;

						colno[m] = eij;
						row[m++] = -1;

						/* add the row to lp_solve */
						lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
					}
				}
			}
		}
	}

	public void printResult(LpSolve lp) {

	}

	static double lamda = 0.16;
	static double beta = .4;
	static double gamma = 4.0;
	static double theta = 1.0;

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
			map.put(tks[1] + " " + tks[2], Double.parseDouble(tks[3]));
		}
		return maps;
	}

	public static HashMap<String, ArrayList<EntityMention>> loadSVMResult(String part) {
		HashMap<String, ArrayList<EntityMention>> entityMentionses = new HashMap<String, ArrayList<EntityMention>>();
		
		String folder = "./";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test" + part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder + "multiType.result" + part);
		
		HashMap<String, EntityMention> mMap = new HashMap<String, EntityMention>();
		
		for(int i=0;i<mentionStrs.size();i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headStart = headStart;
			em.headEnd = headEnd;
			em.start = headStart;
			em.end = headEnd;
			
			mMap.put(em.toName(), em);
			
			String tks[] = typeResult.get(i).split("\\s+");
			int typeIndex = Integer.valueOf(tks[0]);
			
			for(int k=1;k<tks.length;k++) {
				em.semClassConf.add(Double.parseDouble(tks[k]));
			}
			
			String type = SemanticTrainMultiSeed.semClasses.get(typeIndex - 1);
			if(type.equalsIgnoreCase("none")) {
				continue;
			}
			
			em.semClass = type;
			
			ArrayList<EntityMention> mentions = entityMentionses.get(fileKey);
			if(mentions==null) {
				mentions = new ArrayList<EntityMention>();
				entityMentionses.put(fileKey, mentions);
			}
			if(type.equalsIgnoreCase("val")) {
				em.type = "Value";
			} else if(type.equalsIgnoreCase("time")) {
				em.type = "Time";
			} else {
			}
			mentions.add(em);
		}
		return entityMentionses;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("java ~ folder lemda");
			System.exit(1);
		}

		Util.part = args[0];
//		lamda = Double.parseDouble(args[1]);
//		beta = Double.parseDouble(args[2]);

		HashMap<String, HashMap<String, Double>> entityCorefProbMaps = loadProbs("/users/yzcchen/chen3/entityEMACE/EntityEMACE/src/" +
				"entityCorefProbTest"
				+ args[0]);
		HashMap<String, HashMap<String, Double>> eventCorefProbMaps = loadProbs("eventCorefProbTest"
				+ args[0]);

		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);

		ArrayList<String> fileNames = new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();

		ArrayList<ArrayList<Entity>> entityAnswers = new ArrayList<ArrayList<Entity>>();
		ArrayList<ArrayList<Entity>> entityGoldKeys = new ArrayList<ArrayList<Entity>>();

		ArrayList<ArrayList<EventChain>> eventAnswers = new ArrayList<ArrayList<EventChain>>();
		ArrayList<ArrayList<EventChain>> eventGoldKeys = new ArrayList<ArrayList<EventChain>>();

		HashMap<String, HashMap<String, EventMention>> allEvents = new HashMap<String, HashMap<String, EventMention>>();

		int ev1 = 0;
		int ev2 = 0;

		// TODO
		ObjectInputStream modelInput = new ObjectInputStream(
				new FileInputStream("sameBV" + Util.part));
		HashMap<String, HashMap<String, ArrayList<String>>> sameBVses = (HashMap<String, HashMap<String, ArrayList<String>>>) modelInput
				.readObject();
		modelInput.close();
		
		HashMap<String, ArrayList<EntityMention>> entityMentionses = EMLearnSeed
				.loadSVMResult("0");
		Util.seed = true;
		for (int k = 0; k < files.size(); k++) {
			if(k%10==0)
			System.err.println(k + "/" + files.size());
			String file = files.get(k);
			HashMap<String, ArrayList<String>> sameBVs = sameBVses.get(file);
			ACEDoc doc = new ACEChiDoc(file);
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());

			doc.docID = k;
			
			ArrayList<EntityMention> allNouns = new ArrayList<EntityMention>(
					entityMentionses.get(file));
			
			ArrayList<EventMention> events = Util.loadSystemComponentsSeed(doc, allNouns);
			for (EventMention e : events) {
				if (!e.subType.equals("null")) {
					ev1 += 1;
				}
			}

			ArrayList<EventChain> eventCorefAnswer = new ArrayList<EventChain>();
			ArrayList<Entity> entityCorefAnswer = new ArrayList<Entity>();

			Collections.sort(events);
			
//			ArrayList<EntityMention> entityMentions = new ArrayList<EntityMention>();
//			for(EntityMention m : allNouns) {
//				if(!m.semClass.equalsIgnoreCase("time") && !m.semClass.equalsIgnoreCase("val")) {
//					entityMentions.add(m);
//				}
//			}
//			Collections.sort(entityMentions);
			Collections.sort(allNouns);

			HashMap<String, Double> eventCorefProbMap = eventCorefProbMaps
					.get(file);
			HashMap<String, Double> entityCorefProbMap = entityCorefProbMaps
					.get(file);

			ILPSeed ilp = new ILPSeed(doc, events, eventCorefProbMap, sameBVs, allNouns, entityCorefProbMap);
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

			HashMap<String, EventChain> eventChainMap = new HashMap<String, EventChain>();
			HashMap<String, Entity> entityChainMap = new HashMap<String, Entity>();

			for (int i = 0; i < events.size(); i++) {
				if (events.get(i).subType.equals("null")) {
					continue;
				}
				EventChain ec = null;
				for (int j = i - 1; j >= 0; j--) {
					String key = events.get(j).toName() + " "
							+ events.get(i).toName();
					if (ilp.eventCorefs.contains(key)) {
						ec = eventChainMap.get(events.get(j).toName());
						break;
					}
				}
				if (ec == null) {
					ec = new EventChain();
					eventCorefAnswer.add(ec);
				}
				ec.addEventMention(events.get(i));
				eventChainMap.put(events.get(i).toName(), ec);
			}

			for (int i = 0; i < allNouns.size(); i++) {
				Entity ec = null;
				for (int j = i - 1; j >= 0; j--) {
					String key = allNouns.get(j).toName() + " "
							+ allNouns.get(i).toName();
					if (ilp.entityCorefs.contains(key)) {
						ec = entityChainMap.get(allNouns.get(j).toName());
						break;
					}
				}
				if (ec == null) {
					ec = new Entity();
					entityCorefAnswer.add(ec);
				}
				ec.addMention(allNouns.get(i));
				entityChainMap.put(allNouns.get(i).toName(), ec);
			}

			eventAnswers.add(eventCorefAnswer);
			eventGoldKeys.add(doc.goldEventChains);

			entityAnswers.add(entityCorefAnswer);
			entityGoldKeys.add(doc.goldEntities);

			HashMap<String, EventMention> eventMap = new HashMap<String, EventMention>();
			allEvents.put(file, eventMap);

			for (EventMention event : events) {
				eventMap.put(event.toName(), event);
				if (!event.subType.equals("null")) {
					ev2 += 1;
				}
			}
		}

		ToSemEval.singleton = false;

		ToSemEval.outputSemFormat(fileNames, lengths, "event.ilp.nosingle."
				+ args[0], eventAnswers);
		ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys.nosingle."
				+ args[0], eventGoldKeys);

		ToSemEval.outputSemFormatEntity(fileNames, lengths,
				"entity.sys.nosingle." + args[0], entityAnswers);
		ToSemEval.outputSemFormatEntity(fileNames, lengths,
				"entity.gold.nosingle." + args[0], entityGoldKeys);

		ToSemEval.singleton = true;
		ToSemEval.outputSemFormat(fileNames, lengths, "event.ilp.single."
				+ args[0], eventAnswers);
		ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys.single."
				+ args[0], eventGoldKeys);

		ToSemEval.outputSemFormatEntity(fileNames, lengths,
				"entity.sys.single." + args[0], entityAnswers);
		ToSemEval.outputSemFormatEntity(fileNames, lengths,
				"entity.gold.single." + args[0], entityGoldKeys);

		Util.outputResult(allEvents, "ilp_svm/result0");
		System.out.println("Before: " + ev1);
		System.out.println("After: " + ev2);
		// ACECommon.outputResult(ILPUtil.systemEMses,
		// "/users/yzcchen/workspace/NAACL2013-B/src/joint_ilp/result"
		// + Common.part);
		System.out.println("ILP Done.");
	}
}

// // constraint 9, two arguments not coref => event not coref
// for (int i = 0; i < this.args.size(); i++) {
// EventMentionArgument arg1 = this.args.get(i);
// for (int j = i + 1; j < this.args.size(); j++) {
// EventMentionArgument arg2 = this.args.get(j);
//
// if (arg1.role.equals(arg2.role) && !arg1.role.equals("null")
// && discreteRoles.contains(arg1.role)) {
// int e1 = this.eventPositionMap.get(arg1.getEventMention()
// .toName());
// int e2 = this.eventPositionMap.get(arg2.getEventMention()
// .toName());
//
// Integer m1 = this.entityPositionMap.get(arg1.toString());
// Integer m2 = this.entityPositionMap.get(arg2.toString());
//
// if (m1 != null && m2 != null && e1 != e2 && m1 < m2) {
// EntityMention entityMention1 = this.entityMentions
// .get(m1);
// EntityMention entityMention2 = this.entityMentions
// .get(m2);
// int zij = nameMap.get("z(" + (e1 < e2 ? e1 : e2) + ","
// + (e1 < e2 ? e2 : e1) + ")");
// int eij = nameMap.get("e(" + m1 + "," + m2 + ")");
// m = 0;
// colno[m] = zij;
// row[m++] = 1;
// // double probEij = probMap.get("e(" + m1 + "," + m2 +
// // ")");
// // if(probEij<0) {
// // lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
// // }
// colno[m] = eij;
// row[m++] = -1;
// // lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
//
// // lp.addConstraintex(m, row, colno, LpSolve.GE, 0);
// }
// }
// }
// }

// // constraints 5.a, the first event mention has to coreferent
// //TODO
// if(ret==0) {
// m = 0;
// for (int j = 1; j < numberOfEvents; j++) {
// int zij = nameMap.get("z(0," + j + ")");
// colno[m] = zij;
// row[m++] = 1;
// }
// lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
// }

// constraint 7, entity vs. event the same
// for (String key : entityEventOverlap) {
// String tks[] = key.split("\\s+");
// int i1 = this.eventPositionMap.get(tks[0]);
// int j1 = this.eventPositionMap.get(tks[1]);
//
// int i2 = this.entityPositionMap.get(tks[0]);
// int j2 = this.entityPositionMap.get(tks[1]);
//
// int zij = nameMap.get("z(" + i1 + "," + j1 + ")");
// int eij = nameMap.get("e(" + i2 + "," + j2 + ")");
//
// m = 0;
// colno[m] = zij;
// row[m++] = 1;
// colno[m] = eij;
// row[m++] = -1;
// /* add the row to lp_solve */
// // lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
// }
//
// constraint 8, non trigger=>no argument, argument=>trigger
// sum of yik(k<34) - sum of rjk(k<34) >= 0
// for (int i = 0; i < this.eventMentions.size(); i++) {
// EventMention event = this.eventMentions.get(i);
// int yi34 = nameMap.get("y(" + i + "," + 34 + ")");
//
// if (!event.subType.equals("null")) {
// // continue;
// }
//
// for (EventMentionArgument arg : event.getEventMentionArguments()) {
// int j = this.argPositionMap.get(event.toName() + " "
// + arg.toString());
// int rj36 = nameMap.get("r(" + i + "," + 36 + ")");
//
// m = 0;
// colno[m] = yi34;
// row[m++] = 1;
// colno[m] = rj36;
// row[m++] = -1;
// /* add the row to lp_solve */
// // lp.addConstraintex(m, row, colno, LpSolve.GE, 0);
// }
// }

// constraint 9
// for(int i=0;i<this.eventMentions.size();i++) {
// for(int j=i+1;j<this.eventMentions.size();j++) {
// EventMention e1 = this.eventMentions.get(i);
// EventMention e2 = this.eventMentions.get(j);
//
// if(e1.number!=e2.number) {
// int zij = nameMap.get("z(" + i + "," + j + ")");
// m = 0;
// colno[m] = zij;
// row[m++] = 1;
//
// lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
// }
// }
// }

// for (int i = 0; i < this.args.size(); i++) {
// EventMentionArgument arg1 = this.args.get(i);
// for (int j = i + 1; j < this.args.size(); j++) {
// EventMentionArgument arg2 = this.args.get(j);
//
// int ri36 = nameMap.get("r(" + i + ",36)");
// int rj36 = nameMap.get("r(" + j + ",36)");
//
// int e1 = this.eventPositionMap.get(arg1.getEventMention()
// .toName());
// int e2 = this.eventPositionMap.get(arg2.getEventMention()
// .toName());
//
// Integer m1 = this.entityPositionMap.get(arg1.toString());
// Integer m2 = this.entityPositionMap.get(arg2.toString());
//
// if (m1 != null && m2 != null && e1 != e2 && m1 < m2) {
// EntityMention entityMention1 = this.entityMentions.get(m1);
// EntityMention entityMention2 = this.entityMentions.get(m2);
// int zij = nameMap.get("z(" + (e1 < e2 ? e1 : e2) + ","
// + (e1 < e2 ? e2 : e1) + ")");
// int eij = nameMap.get("e(" + m1 + "," + m2 + ")");
// m = 0;
//
// colno[m] = ri36;
// row[m++] = 1;
// colno[m] = rj36;
// row[m++] = 1;
// colno[m] = eij;
// row[m++] = 1;
// colno[m] = zij;
// row[m++] = -1;
// lp.addConstraintex(m, row, colno, LpSolve.GE, 0);
// }
// }
// }

// // constraint 9, event coref, if entity coref => same role
// for(int i=0;i<this.args.size();i++) {
// EventMentionArgument arg1 = this.args.get(i);
// for(int j=i+1;j<this.args.size();j++) {
// EventMentionArgument arg2 = this.args.get(j);
//
// if(arg1.role.equals(arg2.role) && !arg1.role.equals("null") &&
// discreteRoles.contains(arg1.role)) {
// int e1 = this.eventPositionMap.get(arg1.getEventMention().toName());
// int e2 = this.eventPositionMap.get(arg2.getEventMention().toName());
//
// Integer m1 = this.entityPositionMap.get(arg1.toString());
// Integer m2 = this.entityPositionMap.get(arg2.toString());
//
// if(m1!=null && m2!=null && e1!=e2 && m1<m2) {
// EntityMention entityMention1 = this.entityMentions.get(m1);
// EntityMention entityMention2 = this.entityMentions.get(m2);
// int zij = nameMap.get("z(" + (e1<e2?e1:e2) + "," + (e1<e2?e2:e1) +
// ")");
//
//
//
// int eij = nameMap.get("e(" + m1 + "," + m2 + ")");
//
// m = 0;
// colno[m] = zij;
// row[m++] = 1;
//
// // double probEij = probMap.get("e(" + m1 + "," + m2 + ")");
// // if(probEij<0) {
// // lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
// // }
//
// colno[m] = eij;
// row[m++] = -1;
// lp.addConstraintex(m, row, colno, LpSolve.GE, 0);
// }
// }
// }
// }

// constraint 8: best first constraint
// if (ret == 0) {
// /* construct z(i,k)+z(j,k)-z(i,j)<=1 */
// for (int j = 0; j < numberOfEvents ; j++) {
// m = 0;
// for (int i = j - 1; i >= 0; i--) {
// int zij = nameMap.get("z(" + i + "," + j + ")");
//
// colno[m] = zij;
// row[m++] = 1;
//
// }
// /* add the row to lp_solve */
// lp.addConstraintex(m, row, colno, LpSolve.LE, 1);
// }
// }

// // constraint 9: negative constraint
// // System.err.println("Neg:" + this.negativeConstraint.size());
// if (ret == 0) {
// for (int j = 0; j < numberOfEvents; j++) {
// for (int i = j - 1; i >= 0; i--) {
// m = 0;
// EventMention m1 = this.eventMentions.get(i);
// EventMention m2 = this.eventMentions.get(j);
// String pair = m1.getAnchorStart() + "," + m1.getAnchorEnd()
// + "," + m2.getAnchorStart() + ","
// + m2.getAnchorEnd();
// if (this.negativeConstraint.contains(pair)) {
// int zij = nameMap.get("z(" + i + "," + j + ")");
// colno[m] = zij;
// row[m++] = 1;
// /* add the row to lp_solve */
// lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
// }
// }
// }
// }
// constraint 9,
// for (int i = 0; i < this.args.size(); i++) {
// EventMentionArgument arg1 = this.args.get(i);
// for (int j = i + 1; j < this.args.size(); j++) {
// EventMentionArgument arg2 = this.args.get(j);
//
// if (arg1.role.equals(arg2.role) && !arg1.role.equals("null")) {
//
// int e1 = this.eventPositionMap.get(arg1.getEventMention()
// .toName());
// int e2 = this.eventPositionMap.get(arg2.getEventMention()
// .toName());
//
// Integer m1 = this.entityPositionMap.get(arg1.toString());
// Integer m2 = this.entityPositionMap.get(arg2.toString());
//
// if (m1 != null && m2 != null && e1 != e2 && m1 < m2) {
// EntityMention entityMention1 = this.entityMentions
// .get(m1);
// EntityMention entityMention2 = this.entityMentions
// .get(m2);
// int zij = nameMap.get("z(" + (e1 < e2 ? e1 : e2) + ","
// + (e1 < e2 ? e2 : e1) + ")");
// int eij = nameMap.get("e(" + m1 + "," + m2 + ")");
//
// if (!entityMention1.semClass
// .equals(entityMention2.semClass)) {
// m = 0;
// colno[m] = zij;
// row[m++] = 1;
// // colno[m] = eij;
// // row[m++] = -1;
// // lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
// }
// }
// }
// }
// }

