package ca.polymtl.SimulationMine.decisionMaker;
import java.util.ArrayList;
import java.util.HashMap;

import bsh.EvalError;
import bsh.Interpreter;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.MineSimulator.Station;
import ca.polymtl.SimulationMine.MineSimulator.Sterile;
import lpsolve.*; 

public class DecisionMaker {

	//fonction des score par defaut
	protected static final String DEFAULT_SCORE_FUNCTION_STRING = "aleatoire";
	protected static final String OPTIMIZE_FUNCTION_STRING = "optimise";

	//protected static final String OPTIMAL_SCORE_FUNCTION_STRING = "temps_espere_avant_remplissage";
	public static final String WARMUP_SCORE_FUNCTION_STRING = "aleatoire";
	protected static final String OPTIMAL_SCORE_FUNCTION_STRING = "optimal_assign";
	//protected static final String OPTIMAL_SCORE_FUNCTION_STRING = "attenteEspereeCamion";
	public static final String ALEATOIRE_FUNCTION_STRING = "aleatoire";
	private static Mine dummyMine;

	//mine
	protected static Mine mine;

	//efficacite cible
	//private double cibleTempsAttentePelle;

	public static boolean isFunctionStringValid(String function) {

		Camion camion = new Camion(dummyMine.getSteriles().get(0), dummyMine,  null) {

			@Override
			public double getAvgSpeed() {
				return 0;
			}

			@Override
			public double getChargeMax() {
				return 0;
			}

			@Override
			public double getPredictTimeAdjustFactor() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getStdSpeed() {
				return 0;
			}

		};
		Pelle pelle = new Pelle(0, 0, "test", 3);

		if(function.equals(DecisionMaker.OPTIMIZE_FUNCTION_STRING)) return true;

		try {

			DecisionMaker dm = new DecisionMaker(dummyMine);
			dm.computeDecisionScore(camion, pelle, function);
		} catch (EvalError e) {
			return false;
		}
		return true;
	} 

	//Choisis la station de retours pour un camion qui viens de se faire remplir à la pelle
	public static Station selectReturnStation(Camion camion, Pelle pelle) {
		Station returnStation = null;
		//si remplis de sterile, choisis parmis les steriles
		if(pelle.getRockType().getPercentIron() == 0 && pelle.getRockType().getPercentSulfur() ==0) {
			returnStation = mine.getSteriles().get(0);
		}
		else {
			returnStation = mine.getConcentrateurs().get(0);
		}
		//sinon, choisis parmis les concentrateurs


		return returnStation;
	}

	//fonction de score pour la simulation
	private String scoreFunctionString;


	private LpSolve solver;


	public DecisionMaker(Mine mine) {
		this.mine = mine;

		this.scoreFunctionString = DEFAULT_SCORE_FUNCTION_STRING;
		//this.cibleTempsAttentePelle = 0;

		dummyMine = mine;
	}



	//constructeur privé : utilisé seulement pour evaluer la validité de certaines règles
	private DecisionMaker() {
		//cibleTempsAttentePelle=0;
	}




	public String getScoreFunctionString() {

		return this.scoreFunctionString;
	}


	//donne un nouvel objectif a un camion sans but
	//
	public Station giveObjectiveToCamion(Camion camion) {

		if(!camion.getCurrentStation().isDecharge) {
			return selectReturnStation(camion, (Pelle) camion.getCurrentStation());
		}
		ArrayList<Pelle> pelles = mine.getPelles();

		//System.out.println("objectif : "+this.scoreFunctionString);

		if(this.scoreFunctionString.equals(OPTIMIZE_FUNCTION_STRING)) {
			Pelle optimalPelle = giveOptimalObjectiveToCamion(camion);
			return optimalPelle;
			//camion.setObjective(optimalPelle);
		}
		else {
			//on peut donner un objectif seulement si le camion est à une station
			//Lance une erreur si le camion n'est pas a une station


			double maxScore = -Double.MAX_VALUE;
			double minScore = Double.MAX_VALUE;
			Pelle pelleMinScore = null;
			//System.out.println("");
			for(int i = 0 ; i < pelles.size(); i++) {



				double score = 0;


				try {
					score = computeDecisionScore(camion, pelles.get(i), this.scoreFunctionString);
				} catch (EvalError e) {
					e.printStackTrace();
				}

				//System.out.println("pelle "+i);
				// System.out.println("pelle "+pelles.get(i).getId()+"  score :"+score);

				if(score >= maxScore) {
					maxScore = score;
				}
				if(score <= minScore) {
					pelleMinScore = pelles.get(i);
					minScore = score;
				}
			}

			return pelleMinScore;
			//set la pelle avec le score MAX
			//camion.setObjective(pelleMaxScore);

			//set la pelle avec le score MIN
			//camion.setObjective(pelleMinScore);
		}


	}


	public Pelle giveOptimalObjectiveToCamion(Camion camionToAssign) {

		ArrayList<Pelle> optimizablePelles = mine.getPelles();
		ArrayList<Camion> optimizableCamions = findOptimizableCamions(optimizablePelles, camionToAssign);

		HashMap<Camion, Pelle> optimalAssign = resoutProblemeAssignation(optimizableCamions, optimizablePelles);

		if(!optimalAssign.containsKey(camionToAssign)) {
			//System.out.println("Erreur d'assignation");

		}
		else {
			//System.out.println("je retourne la pelle "+optimalAssign.get(camionToAssign).getId());
			return optimalAssign.get(camionToAssign);
		}
		throw new IllegalStateException();
	}


	public void setScoreFunctionString(String text) {
		this.scoreFunctionString = text;

	}


	private double calculeTempsAvantDispo(Camion camion) {
		
		
		
		if(camion.getState() == Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion qui vient juste d'arriver");
		}
		else if(camion.getState() == Camion.ETAT_EN_TRAITEMENT && !camion.getCurrentStation().isDecharge) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion en train de se faire traiter à une pelle");
		}
		else if(camion.getState() == Camion.ETAT_ATTENTE&& !camion.getCurrentStation().isDecharge) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion en attente à une pelle");
		}
		else if(camion.getState()== Camion.ETAT_EN_ROUTE && !camion.getObjective().isDecharge) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion en route vers une pelle");
		}
	
	
		
		//si le camion est inactif, il est disponible MAINTENANT
		//
		else if(camion.getState()== Camion.ETAT_INACTIF) {
			return 0;
		}
		//si le camion est en route pour se faire décharger
		else if(camion.getState() == Camion.ETAT_EN_ROUTE && camion.getObjective().isDecharge) {
			Station objective = camion.getObjective();
			//temps avant d'arriver
			double distance = camion.getLocation().distance(objective.getLocation());
			double tempsDeplacement = distance/(camion.getSpeed()*mine.getMeteoFactor());
	
	
			//temps avant que la station ne commence à traiter le camion
			double tempsTraitementCurrent = 0;
			if(camion.getObjective().getCamionEnTraitement()!= null) {
				tempsTraitementCurrent = objective.getCamionEnTraitement().getCharge()/objective.averageTraitementSpeed();
			}
			double qteATraiter = 0;
			//ajoute les camion en route vers la pelle et plus pres
			for(int i = 0 ; i < mine.getCamions().size(); i++) {
				Camion c = mine.getCamions().get(i);
				if(c.getState() == Camion.ETAT_EN_ROUTE && 
						c.getObjective().equals(objective) && 
						c.getLocation().distance(objective.getLocation())/c.getAvgSpeed() < distance/camion.getAvgSpeed()) {
					qteATraiter+=c.getCharge();
				}
				else if(c.getState() == Camion.ETAT_ATTENTE && c.getCurrentStation().equals(objective) ) {
					qteATraiter+=c.getCharge();
				}
			}
			double tempsFileAttente = tempsTraitementCurrent+qteATraiter/objective.averageTraitementSpeed();
	
			//temps avant que le camion ne commence a etre traite = max{tempsDeplacement, tempsFileAttente}
			double tempsTotal = tempsFileAttente;
			if(tempsDeplacement>tempsFileAttente) {
				tempsTotal = tempsDeplacement;
			}
	
			//on ajoute le temps de traitement
			tempsTotal += camion.getCharge()/objective.averageTraitementSpeed();
			return tempsTotal;
		}
		//si le camion est en déchargement
		else if(camion.getState() == Camion.ETAT_EN_TRAITEMENT && camion.getCurrentStation().isDecharge) {
			return camion.getCharge()/camion.getObjective().averageTraitementSpeed();
	
		}
		//si le camion est en attente à la station de déchargement
		else if(camion.getState() == Camion.ETAT_ATTENTE && camion.getCurrentStation().isDecharge) {
			//calcule le temps de decharge du camion courant, plus celui des camions en attente AVANT, plus le temps de décharge du camion.
			Station objective = camion.getCurrentStation();
			if(objective.getCamionEnTraitement() == null) {
				throw new IllegalStateException("Impossible d'avoir un camion en attente mais pas de camion en traitement!");
			}
	
			double temps = objective.getCamionEnTraitement().getCharge()/objective.averageTraitementSpeed();
			double qteATraiter = 0;
			for(int i = 0 ; i < objective.getCamionsEnAttente().size(); i ++) {
				if(objective.getCamionsEnAttente().get(i).equals(camion)) {
					qteATraiter += objective.getCamionsEnAttente().get(i).getCharge();
					break;
				}
				else {
					qteATraiter += objective.getCamionsEnAttente().get(i).getCharge();
				}
			}
			temps += qteATraiter += objective.averageTraitementSpeed();
			return temps;
		}
		//etat du camion non valide pour une raison quelconque
		else {
			throw new IllegalStateException("état du camion non valide pour une raison inconnue.");
		}
	}


	private ArrayList<Camion> findOptimizableCamions(ArrayList<Pelle> optimizablePelles, Camion camionToAssign) {
		ArrayList<Camion> optimisableCamions = new ArrayList<Camion>();

		optimisableCamions.add(camionToAssign);

		@SuppressWarnings("unchecked")
		//liste des potentiels camions
		ArrayList<Camion> camionsClone = (ArrayList<Camion>) mine.getCamions().clone();

		//enleve le camion a assignere de la liste des candidats, puisqu'il doit absolument
		//etre optimise (on le rajoute a la fin)
		camionsClone.remove(camionToAssign);


		@SuppressWarnings("unchecked")
		ArrayList<Camion> camionsCandidates = (ArrayList<Camion>) camionsClone.clone();

		//si n pelles, trouve les n-1 camions qui seront disponibles le plus rapidement.
		for(int i = 0 ; i < optimizablePelles.size()-1; i++) {
			Camion bestCamion = null;
			double bestTemps = Double.MAX_VALUE;
			for(int j = 0 ; j < camionsClone.size(); j++) {
				//si le camion se qualifie
				//
				Camion c = camionsClone.get(j);
				if((c.getState() == Camion.ETAT_EN_ROUTE && c.getObjective().isDecharge) ||
						(c.getState() == Camion.ETAT_ATTENTE && c.getCurrentStation().isDecharge)||
						(c.getState() == Camion.ETAT_EN_TRAITEMENT) && c.getCurrentStation().isDecharge) {
					double temps = calculeTempsAvantDispo(camionsClone.get(j));
					if(temps < bestTemps) {
						bestCamion = camionsClone.get(j);
						bestTemps = temps;
					}
				}
			}
			if(bestCamion == null) {
				break;
			}
			else {
				camionsClone.remove(bestCamion);
				optimisableCamions.add(bestCamion);
			}
		}
		return(optimisableCamions);
		/*
		//les seuls candidats sont ceux qui :
		// - sont en déplacement vers le concentrateur ou le stérile
		// - ceux qui sont juste arrives au concentrateur ou au sterile
		// - ceux qui sont IDLE au concentrateur ou au sterile
		for(int i = 0 ; i < camionsClone.size(); i++) {
			Camion camion = camionsClone.get(i);


			if(camion.getState() != Camion.ETAT_JUSTE_ARRIVE &&
					camion.getState() != Camion.ETAT_EN_ROUTE &&  
					camion.getState() != Camion.ETAT_INACTIF) {
				camionsCandidates.remove(camion);
			}
			else if(!camion.getObjective().equals(mine.getSterile()) &&
					!camion.getObjective().equals(mine.getConcentrateur())) {
				camionsCandidates.remove(camion);
			}

		}

		//enleve le camion le plus loin camion jusqu'a ce qu'on ait le bon nombre de candidat
		//
		while(camionsCandidates.size() > optimizablePelles.size()-1) {
			double maxDist = 0;
			Camion plusLoin = camionsCandidates.get(0);

			//trouve le camion le plus loin. A la sortie de la boucle, plusLoin est soit le camion le plus loin,
			//soit le premier camion de la liste si aucun candidat n'est en deplacement
			//
			for(int i = 0 ; i < camionsCandidates.size(); i++) {
				Camion c = camionsCandidates.get(i);
				if(c.getState() == Camion.ETAT_EN_ROUTE) {
					double distanceRestante = c.getObjective().getLocation().distance(c.getLocation());
					if(distanceRestante > maxDist) {
						maxDist = distanceRestante;
						plusLoin = c;
					}
				}
			}
			camionsCandidates.remove(plusLoin);

		}


		camionsCandidates.add(camionToAssign);
		return camionsCandidates;
		 */
	}




	//cree et resout un probleme d'affectation de camions a des pelles
	//
	private HashMap<Camion, Pelle> resoutProblemeAssignation(ArrayList<Camion> camions, ArrayList<Pelle> pelles	) {

		boolean debug = false;



		if(debug) System.out.println("\nbegin resoutProblemeAssignation");

		//System.out.println("debut de resolution du probleme d'assignation");
		//le nombre de camions doit etre <= au nombre de pelles
		//
		if(camions.size() > pelles.size()) {
			throw new IllegalArgumentException("il y a "+camions.size()+" camions, mais "+pelles.size()+" pelles!");
		}


		int nbConstraints = camions.size()+pelles.size();


		try {
			solver = LpSolve.makeLp(2*pelles.size(), camions.size()*pelles.size());
			if(!debug) {

				solver.setVerbose(LpSolve.SEVERE);
				//solver.setVerbose(LpSolve.FULL);

			}
			if(debug) {
				solver.setVerbose(LpSolve.FULL);
			}
			//ajoute les contraintes
			//
			//contraintes camions sont de type =
			//




			for(int i = 0 ; i < camions.size(); i++) {
				//System.out.println("camion");
				solver.setRh(i+1, 1);
				solver.setConstrType(i+1, LpSolve.EQ);
				//solver.addConstraint(null, LpSolve.EQ, 1);
			}
			for(int i = pelles.size() ; i < nbConstraints; i++) {
				//solver.addConstraint(null, LpSolve.LE, 1);
				solver.setRh(i+1, 1);
				solver.setConstrType(i+1, LpSolve.EQ);//etait LE
			}
			//contraintes de pelles dont de type <=


			//Ajoute les colonnes une a une
			//
			int index1 =1;
			for(int i = 0 ; i < camions.size(); i++ ) {
				for(int j = 0 ; j < pelles.size(); j++) {
					double[] colonneVals = new double[2];
					colonneVals[0] = 1;
					colonneVals[1] = 1;
					int[] coloneIndex = new int[2];
					coloneIndex[0] = i+1;
					coloneIndex[1] = camions.size()+j+1;

					//ajoute la contrainte

					//solver.addColumnex(2, colonneVals, coloneIndex);
					solver.setColumnex(index1, 2, colonneVals, coloneIndex);
					index1++;

				}
			}

			//Fonction de cout
			//
			double[] costFunction = new double[camions.size()*pelles.size()+1];
			int index = 0;
			for(int i = 0 ; i < camions.size(); i++ ) {
				for(int j = 0 ; j < pelles.size(); j++) {
					Camion camion = camions.get(i);
					Pelle pelle = pelles.get(j);

					double score = 0;
					try {
						score = computeDecisionScore(camion, pelle, DecisionMaker.OPTIMAL_SCORE_FUNCTION_STRING);
					} catch (EvalError e) {
						e.printStackTrace();
					}

					costFunction[index+1] = score;

					index ++;
					if(debug) System.out.println("score "+i+" "+j+" : "+score);

				}
			}
			solver.setObjFn(costFunction);

			solver.setMinim();
			// solve the problem
			solver.solve();


			HashMap<Camion, Pelle> assign = new HashMap<Camion, Pelle>();
			// print solution

			double[] var = solver.getPtrVariables();
			for (int i = 0; i < var.length; i++) {
				// > 0.9 pour éviter les erreurs numériques
				int camionIndex = i/pelles.size();
				int pelleIndex = i%pelles.size();
				if(debug) System.out.println("col "+camionIndex+ " "+ pelleIndex +" = "+var[i]);
				if(var[i] > 0.9) {



					assign.put(camions.get(camionIndex), pelles.get(pelleIndex));		
				}
			}

			solver.deleteLp();


			// delete the problem and free memory
			//solver.deleteLp();
			//System.out.println("fin de resolution du probleme d'assignation");
			return assign;
		}
		catch (LpSolveException e) {
			e.printStackTrace();
		}
		if(debug) System.out.println("\nend resoutProblemeAssignation");
		return null;
	}


	//distance entre le camion et la pelle (m)
	//si le camion est en route pour un autre objectif, compte la distance entre camion et objectif + distance entre objectif et pelle
	protected double calculeDistanceEntreCamionEtStation(Camion camion, Station station) {
		double distanceEntreCamionEtPelle = camion.getLocation().distance(station.getLocation());
		//si le camion est deja en route vers une autre destination (sterile ou concentrateur) prend la distance jusqu'a la destination, 
		//plus la distance entre la destination et la pelle
		if(camion.getObjective() != null && !camion.getObjective().equals(station) ) {
			distanceEntreCamionEtPelle =  camion.getLocation().distance(camion.getObjective().getLocation())+ camion.getObjective().getLocation().distance(station.getLocation());

		}
		return distanceEntreCamionEtPelle;
	}



	protected double calculeOptimalAssignCost(Camion camion, Pelle pelle) {
		// - si le camion est en route vers un concentrateur ou un stérile, 
		//ou si le camion est déjà à un concentrateur ou un stérile, on calcule le temps avant qu'il arrive à la pelle
		// - sinon, retourne 0 (dans le problème d'assignation, le score sera 0 pour toutes les pelles, donc c'est comme si on ne prenait pas en compte le camion).

		
		if( (camion.getState() == Camion.ETAT_EN_ROUTE && camion.getObjective().isDecharge == true) || //si le camion va se faire décharger
				(camion.getState() == Camion.ETAT_EN_TRAITEMENT && camion.getCurrentStation().isDecharge == true) || //si le camion est en traitement
				(camion.getState()== Camion.ETAT_ATTENTE && camion.getCurrentStation().isDecharge == true) //si le camion est en attente de se faire décharger
				) {
			
			Station s = null;
			if(camion.getState() == Camion.ETAT_EN_ROUTE) {
				s = camion.getObjective();
			}
			else {
				s = camion.getCurrentStation();
			}
			double tempsAvantDispo = calculeTempsAvantDispo(camion);
			
			double distanceEntreStationEtPelle = s.getLocation().distance(pelle.getLocation());
			double tempsAvantArriveeAPelle = tempsAvantDispo + distanceEntreStationEtPelle/(camion.getAvgSpeed()*mine.getMeteoFactor());
			
			//temps avant que pelle soit disponible
			//
			double qteAvantDisponible = 0;
			if(pelle.getCamionEnTraitement() != null) {
				qteAvantDisponible += pelle.getCamionEnTraitement().getChargeMax()-pelle.getCamionEnTraitement().getCharge();
			}
			
			//file d'attente
			for(int i = 0 ; i< pelle.getCamionsEnAttente().size(); i++) {
				qteAvantDisponible += pelle.getCamionsEnAttente().get(i).getChargeMax();
			}
			
			//camions se dirigeant vers la pelle qui sont plus proche
			//
			for(int i = 0 ; i < mine.getCamions().size(); i++) {
				Camion c = mine.getCamions().get(i);
				if(c.getState() == Camion.ETAT_EN_ROUTE && c.getObjective().equals(pelle) && c.getLocation().distance(pelle.getLocation()) < distanceEntreStationEtPelle) {
					qteAvantDisponible += c.getChargeMax();
				}
			}
			
			double tempsAvantPelleDispo = qteAvantDisponible/pelle.averageTraitementSpeed();
			
			//attente esperee du camion
			//
			double attenteEspereeCamion = tempsAvantPelleDispo - tempsAvantArriveeAPelle;
			if(attenteEspereeCamion < 0) {
				attenteEspereeCamion = 0;
			}
			
			double attenteEspereePelle = tempsAvantArriveeAPelle - tempsAvantPelleDispo;
			if(pelle.getState() == Station.STATION_STATE_IDLE) {
				attenteEspereePelle += pelle.getCurrentWaitingPeriod();
			}
			if(attenteEspereePelle < 0) {
				attenteEspereePelle = 0;
			}
			
			
			
			double penaliteQuadAttentePelle = calculePenaliteQuadAttentePelle(attenteEspereePelle/3600., pelle.cibleAttentePelleSeconds()/3600.);

			double penaliteQuadAttenteCamion = calculePenaliteQuadAttenteCamion(attenteEspereeCamion/3600.,0);// pelle.cibleAttenteCamionSeconds()/3600.);
			
			System.out.println(pelle.getId());
			if(pelle.getId().compareTo("pelle3")== 0 || true) {
				
				System.out.println("attente esperee pelle  : "+attenteEspereePelle);
				System.out.println("attente esperee camion : "+attenteEspereeCamion);
				System.out.println("penalite pelle         : "+penaliteQuadAttentePelle);
				System.out.println("penalite camion        : "+penaliteQuadAttenteCamion);
				
			}
			System.out.println("score                  : "+penaliteQuadAttentePelle+penaliteQuadAttenteCamion);
			return penaliteQuadAttentePelle+penaliteQuadAttenteCamion;
		}
		else {
			return 0;
		}

	}

	protected double calculePenaliteQuadAttenteCamion(double attenteEspereeCamionSeconds, double cibleAttenteCamionSeconds) {
		return (attenteEspereeCamionSeconds-cibleAttenteCamionSeconds)*((attenteEspereeCamionSeconds-cibleAttenteCamionSeconds));
	}


	protected double calculePenaliteQuadAttentePelle(double attenteEspereePelle, double cibleTempsAttentePelle) {

		//System.out.println("test "+cibleTempsAttentePelle);
		//en minute pour eviter instabilites
		double ecartCible = (attenteEspereePelle-cibleTempsAttentePelle);


		//System.out.println("attenteEspereePelle "+attenteEspereePelle);
		/*if(ecartCible > 0) {
			return ecartCible*ecartCible;
		}
		else {
			return 0.5*ecartCible*ecartCible;
		}*/

		return ecartCible*(ecartCible);
		//return ecartCible*ecartCible;
		//return attenteEspereePelle*attenteEspereePelle;
	}


	//-----------------------------------------------------------------
	// Calcule le tmeps espere avant que le camion commence à se faire traiter à la pelle
	// En considérant qu'il s'y rend dès que possible
	//-----------------------------------------------------------------
	protected double calculeTempsEspereAvantTraitement(Camion camion, Station station) {
		boolean debug = false;

		//considere que le camion est libre
		//si on veut que cette méthode fonctionne pour un camion dans un état quelconque, 
		//il faut tenir en compte le temps pour aller se faire remplir, puis vider. Considérant qu'il peut
		//y avoir plusieurs stations de vidage possible, on commence à devoir faire des choix, 
		//estimer des quantités (par exemple le nombre de camions aux stations de remplissage/vidage, etc). 
		//Bref, ça deviendrait vite compliqué.
		//
		if( camion.getState()!= Camion.ETAT_INACTIF) {
			throw new IllegalStateException("Le camion doit etre inactif");
		}
		
		
		
		double distanceCamion = calculeDistanceEntreCamionEtStation(camion, station);


		double tempsParcoursRestantCamionOpt = distanceCamion/(camion.getAvgSpeed()*mine.getMeteoFactor());
		if(debug) {
			System.out.println("\ndebut calculeTempsEspereAvantRemplissage");
		}

		@SuppressWarnings("unchecked")
		ArrayList<Camion> camionsClone = (ArrayList<Camion>) mine.getCamions().clone();

		//ordonne les camions selon le temps pour arriver a la pelle (croisssant)
		ArrayList<Camion> orderedCamionsEnRoute = new ArrayList<Camion>();


		//trouve le camion qui arrivera le prochain a la pelle

		Camion camionPlusPres= null;

		do {
			camionPlusPres = null;
			double minTempsParcours = Double.MAX_VALUE;
			for(int i = 0 ; i < camionsClone.size(); i++) {

				Camion camionIter = camionsClone.get(i);
				if(camionIter.getState() == Camion.ETAT_EN_ROUTE && camionIter.getObjective() == station) {
					double distance = camionIter.getLocation().distance(station.getLocation());
					double tempsParcoursRestant = distance/( camionIter.getAvgSpeed()*mine.getMeteoFactor());
					//2e condition : on ne considere pas les camions qui arrivent apres le camion a optimiser
					//
					if(tempsParcoursRestant < minTempsParcours && tempsParcoursRestant < tempsParcoursRestantCamionOpt) {
						camionPlusPres = camionIter;
						minTempsParcours = tempsParcoursRestant;
					}
				}

			}
			if(camionPlusPres!= null) {
				if(debug) {System.out.println("ajoute un camion a "+minTempsParcours+"secondes de la pelle");}
				orderedCamionsEnRoute.add(camionPlusPres);
				camionsClone.remove(camionPlusPres);
			}

		}while(camionPlusPres != null);

		//distance entre le camion et la pelle (m)
		double distanceEntreCamionEtPelle = camion.getLocation().distance(station.getLocation());

		//temps espere vers la pelle (m/s)
		double tempsDeParcoursEspere = distanceEntreCamionEtPelle/(camion.getAvgSpeed()*mine.getMeteoFactor());

		//temps espere avant que la pelle termine sa file d'attente actuelle
		double tempsRestantAvantFinPelle = calculeTempsRestantAvantFinPelle(station);


		if(debug) {
			System.out.println("temps d'attente a la pelle : "+ tempsRestantAvantFinPelle+" secondes" );
		}

		//nombre de camions en route vers la pelle
		int nbCamionsEnRoute = orderedCamionsEnRoute.size();



		orderedCamionsEnRoute.add(camion);


		for(int i = 0; i < orderedCamionsEnRoute.size(); i++) {

			Camion camionIter = orderedCamionsEnRoute.get(i);
			double distance = camionIter.getLocation().distance(station.getLocation());
			double tempsParcoursRestant = distance/( camionIter.getAvgSpeed()*mine.getMeteoFactor());
			if(debug) {
				System.out.println("Camion a "+tempsParcoursRestant+" secondes de la pelle");
			}
		}

		//si aucun camion en route, le temps avant le debut du chargement = max{tempsRestantAvantFinPelle, tempsDeParcoursEspere}
		if(nbCamionsEnRoute == 0) {
			if(tempsDeParcoursEspere > tempsRestantAvantFinPelle) {
				if(debug) {
					System.out.println("Retourne temps de parcours espere"+ tempsDeParcoursEspere);
				}
				return tempsDeParcoursEspere;
			}
			else {
				if(debug) {
					System.out.println("Retourne tempsRestantAvantFinPelle"+ tempsRestantAvantFinPelle);
				}
				return tempsRestantAvantFinPelle;
			}

		}
		else {
			//Ok, ici c'est complique... On veut calculer le temps avant que le camion puisse commencer a se faire remplir. 
			//Cela est égal à : max{ temps de fin de remplissage du camion précédent , temps pour se rendre  }
			// et pour obtenir " temps de fin de remplissage du camion précédent", il faut obtenir le temps de début du camion précédent par la meme
			// formule
			//
			Camion premierCamion = orderedCamionsEnRoute.get(0);
			double distance = premierCamion.getLocation().distance(station.getLocation());
			double tempsParcoursRestant = distance/( premierCamion.getAvgSpeed()*mine.getMeteoFactor());
			double tempsDebutChargementCamionI = tempsParcoursRestant;
			if(tempsParcoursRestant < tempsRestantAvantFinPelle) {
				tempsDebutChargementCamionI = tempsRestantAvantFinPelle;
			}
			for(int i = 1; i < orderedCamionsEnRoute.size(); i++) {

				Camion camionIter = orderedCamionsEnRoute.get(i);
				distance = camionIter.getLocation().distance(station.getLocation());
				tempsParcoursRestant = distance/( camionIter.getAvgSpeed()*mine.getMeteoFactor());

				tempsDebutChargementCamionI = tempsDebutChargementCamionI + camionIter.getChargeMax()/Pelle.AVERAGE_CHARGE_SPEED;
				if(tempsDebutChargementCamionI < tempsParcoursRestant ) {
					tempsDebutChargementCamionI = tempsParcoursRestant;
				}
			}
			if(debug) {
				System.out.println("debut calculeTempsEspereAvantRemplissage");
				System.out.println("retourne"+ tempsDebutChargementCamionI);
			}

			return tempsDebutChargementCamionI;

		}
	}


	//-----------------------------------------------------------------------
	// Calcule le temps restant avant que la pelle ne finisse de traiter
	//	sa file d'attente actuelle (ne tiens pas compte des camions en route)
	//-----------------------------------------------------------------------
	protected double calculeTempsRestantAvantFinPelle(Station station) {
		double tempsRestantAvantFinPelle = 0;
		Camion camionEnRemplissage = station.getCamionEnTraitement();
		if(camionEnRemplissage != null && camionEnRemplissage.getCharge() < camionEnRemplissage.getChargeMax()) {
			double chargeRestante = camionEnRemplissage.getChargeMax() - camionEnRemplissage.getCharge();
			for(int i = 0 ; i < station.getCamionsEnAttente().size(); i++) {
				chargeRestante += station.getCamionsEnAttente().get(i).getChargeMax();
			}
			tempsRestantAvantFinPelle = chargeRestante/Pelle.AVERAGE_CHARGE_SPEED;
		}
		return tempsRestantAvantFinPelle;
	}


	//fonction qui calcule le score associe a l'assignation d'un camion a une pelle
	//
	protected double computeDecisionScore(Camion camion, Pelle pelle, String scoreFunctionString) throws EvalError {

		boolean objectiveIsConcentrator = false;
		boolean objectiveIsSterile = false;

		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
			if(camion.getObjective() == mine.getConcentrateurs().get(i)) {
				objectiveIsConcentrator = true;
				break;
			}
		}
		for(int i = 0 ; i < mine.getSteriles().size(); i++) {
			if(camion.getObjective() == mine.getSteriles().get(i)) {
				objectiveIsSterile= true;
				break;
			}
		}


		//erreur si le camion n'est pas soit : 
		//idle 
		//en route vers sterile ou concentrateur
		//vient d'arriver au sterile ou concentrateur
		if(camion.getState() != Camion.ETAT_INACTIF &&
				!(camion.getState() == Camion.ETAT_EN_ROUTE && (objectiveIsConcentrator|| objectiveIsSterile)) &&
				!(camion.getState() == Camion.ETAT_JUSTE_ARRIVE && (objectiveIsConcentrator|| objectiveIsSterile))) {
			//throw new IllegalArgumentException("le camion est dans un mauvais état");
		}




		ArrayList<Camion> camions = mine.getCamions();



		//calcule plein de métriques qui pourront etre utilisees par les etudiants
		//


		//--------------------------------------------------

		//nombre aléatoire
		double random = SimulationMine.random.nextDouble();

		//si la fonction aleatoire est choisie, retourne tout de suite pour accelerer
		if(scoreFunctionString.equals("aleatoire")){
			return random;
		}

		//de meme, si on choisit "optimal_assign", retourne immediatement la value pour accelerer
		double optimalAssignCost = calculeOptimalAssignCost(camion, pelle);

		if(scoreFunctionString.equals(DecisionMaker.OPTIMAL_SCORE_FUNCTION_STRING)){
			return optimalAssignCost;
		}

		//score max
		double max = Double.MAX_VALUE;

		double vitesse_moyenne_camion = camion.getAvgSpeed();
		double temps_moyen_remplissage = camion.getChargeMax()/Pelle.AVERAGE_CHARGE_SPEED;

		//indique 1 si la pelle est occupee
		int pelleOccupee = 0;
		if(pelle.getCamionEnTraitement() != null) pelleOccupee = 1;

		//nombre de camions en attente
		int nbCamionsEnAttente = pelle.getCamionsEnAttente().size();

		//nombre de camions a la pelle (attente + remplissage)
		int nbCamionsALaPelle = 0;
		if(pelle.getCamionEnTraitement()!= null) {
			nbCamionsALaPelle = nbCamionsEnAttente+1;
		}

		//distance entre le camion et la pelle (m)
		//si le camion est en route pour un autre objectif, compte la distance entre camion et objectif + distance entre objectif et pelle

		double distanceEntreCamionEtPelle = calculeDistanceEntreCamionEtStation(camion, pelle);




		//temps espere vers la pelle (s)
		double tempsDeParcoursEspere = distanceEntreCamionEtPelle/( camion.getAvgSpeed()*mine.getMeteoFactor());


		//nombre de camions presentement en route pour la pelle
		int nbCamionsEnRoutePourLaPelle = 0;
		for(int i = 0 ; i < camions.size(); i++) {
			if(camions.get(i).getState() == Camion.ETAT_EN_ROUTE && camions.get(i).getObjective() == pelle) {
				nbCamionsEnRoutePourLaPelle++;
			}
		}

		//temps restant avant que la pelle n'aie plus de travail (en considerant seulement les camions en attente)
		//
		double tempsRestantAvantFinPelle = calculeTempsRestantAvantFinPelle(pelle);
		Camion camionEnRemplissage = pelle.getCamionEnTraitement();
		if(camionEnRemplissage != null) {

			//temps de remplissage restant = charge restante / vitesse moyenne charge
			double chargeRemaining = camionEnRemplissage.getChargeMax()-camionEnRemplissage.getCharge(); 
			double esperanceTempsRemplissageRestant = chargeRemaining/Pelle.AVERAGE_CHARGE_SPEED;

			tempsRestantAvantFinPelle = esperanceTempsRemplissageRestant + pelle.getCamionsEnAttente().size()*camion.getChargeMax()/Pelle.AVERAGE_CHARGE_SPEED;
		}


		//temps espere avant le debut du remplissage
		//
		double tempsEspereAvantDebutRemplissage = calculeTempsEspereAvantTraitement(camion, pelle);

		//temps espere d'attente de la pelle
		//


		//attente esperee du camion
		//
		double attenteEspereeCamion = tempsEspereAvantDebutRemplissage - tempsDeParcoursEspere;
		if(attenteEspereeCamion < 0) {
			attenteEspereeCamion = 0;
		}

		//attente esperee de la pelle (compte seulement le temps ou la pelle attend pour le camion courant, pas pour ceux deja en route!)
		//peut etre négatif, signifiant que le camion ira en attente.
		//
		double attenteEspereePelle = tempsDeParcoursEspere - tempsEspereAvantDebutRemplissage;

		double penaliteQuadAttentePelle = calculePenaliteQuadAttentePelle(attenteEspereePelle, pelle.cibleAttentePelleSeconds());



		double penaliteQuadAttenteCamion = calculePenaliteQuadAttenteCamion(attenteEspereeCamion, pelle.cibleAttenteCamionSeconds());


		Interpreter interpreter = new Interpreter();  // Construct an interpreter
		// Eval a statement and get the result




		interpreter.set("x1", vitesse_moyenne_camion);
		interpreter.set("x2", pelleOccupee);
		interpreter.set("x3", random);
		interpreter.set("aleatoire", random);
		interpreter.set("x4", max);
		interpreter.set("t1", temps_moyen_remplissage);
		interpreter.set("t2", tempsDeParcoursEspere);
		interpreter.set("t3", tempsRestantAvantFinPelle);
		interpreter.set("t4", tempsEspereAvantDebutRemplissage);
		interpreter.set("t5", attenteEspereeCamion);
		interpreter.set("t6", attenteEspereePelle);
		interpreter.set("d1", distanceEntreCamionEtPelle);
		interpreter.set("n1", nbCamionsEnAttente);
		interpreter.set("n2", nbCamionsALaPelle);
		interpreter.set("n3", nbCamionsEnRoutePourLaPelle);

		interpreter.set("ecart_cible_quadratique", penaliteQuadAttentePelle);

		interpreter.set("optimal_assign", optimalAssignCost);

		interpreter.eval("double score = "+ scoreFunctionString); 

		double score = (double) interpreter.get("score");
		if(score == Double.POSITIVE_INFINITY) {
			score = Double.MAX_VALUE;
		}
		if(score == Double.NEGATIVE_INFINITY) {
			score = -Double.MAX_VALUE;
		}

		return score;

	}

}