package ca.polymtl.SimulationMine.decisionMaker;

import java.util.ArrayList;
import java.util.HashMap;

import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.Station;
import javafx.util.Pair;

/**
 * Classe chargée de la prédiction des temps de parcours
 * @author Fred
 *
 */
public class TravelTimePredictor {

	//Prediction du temps de parcours
	//
	/** Constante correspondant a la prediction de temps de parcours avec la fonction "moyenne des temps precedents" */
	public static final int PREDICT_FUNCTION_AVG_PREV = 1;
	/** Constante correspondant a la prediction de temps de parcours avec la fonction "combinaison convexe" */
	public static int PREDICT_FUNCTION_WEIGTED = 2;
	/** Constante correspondant a la prediction de temps de parcours avec la fonction "erreur precedente" */
	public static int PREDICT_FUNCTION_WEIGTED_ERROR = 3;

	/**
	 * Calcule la cle correspondant a la paire d'origine et destination pour des fins de prediction de temps de parcours.
	 * NB: Puisque le trajet A->B est équivalent au trajet B->A, on fait en sorte que les cles pour les deux trajets soient identiques.
	 * 
	 * @param origine Station d'origine
	 * @param destination Station de destination
	 * @return Cle correspondant a la paire d'origine et destination.
	 */
	public static String getMapKeyForODPair(Station origine, Station destination) {
		
		if(origine == null) {
			throw new IllegalArgumentException("la station d'origine est nulle");
		}
		if(destination == null) {
			throw new IllegalArgumentException("la station de destination est nulle");
		}
			
		if( origine.getId().compareTo(destination.getId()) < 0 ){
			return origine.getId()+":"+destination.getId();	
		}
		else{
			return destination.getId()+":"+origine.getId();
		}
		
	}
	//Prediction des temps de parcours
	//
	/**fonction de prediction de temps de parcours*/
	private int fPredictFunction;
	/**Valeur de N dans la fonction de prediction (si applicable)*/
	private int predictFunctionNumberSample;
	/**Valeur de lambda dans la fonction de prediction (si applicable)*/
	private double predictFunctionWeight;
	
	//historique des temps de parcours. La clé est une string indiquant l'origine/destination
	//une prédiction de -1 signifie aucune prédiction
	/**Historique des temps de parcours pour chaque trajet*/
	private HashMap<String, ArrayList<Double>> historyMap;
	/**Historique des predictions pour chaque trajet*/
	private HashMap<String, ArrayList<Double>> predictionMap;

	/**Mine*/
	private Mine mine;



	/**
	 * Constructeur 
	 * @param mine Mine
	 * @param config Configuration
	 */
	public TravelTimePredictor(Mine mine, Config config) {

		this.mine = mine;
		this.fPredictFunction = config.getDefaultTimePredictFormula();

		this.predictFunctionNumberSample = config.getDefaultTimePredictN();
		this.predictFunctionWeight = config.getDefaultTimePredictLambda();

		//historique
		this.historyMap = new HashMap<String, ArrayList<Double>>();
		this.predictionMap = new HashMap<String, ArrayList<Double>>();

	}


	
	/**
	 * enregistre l'historique de temps de parcours du camion (incl. temps predit)
	 * @param camion qui vient de terminer son parcours
	 */
	public void enregistreHistoriqueTempsParcours(Camion camion) {
		
		//lance une exceptions si le camion n'est pas dans l'étata just arrived
		//
		if(camion.getState() != Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalStateException("le camion doit etre dans l'état Camion.STATE_JUST_ARRIVED");
		}

		
		//clé d'origine/destination
		String ODKey = getMapKeyForODPair(camion.getOrigine(), camion.getDestination());
		
		double adjustedRealTravelTime= camion.getCurrentTravelTime()/camion.getPredictTimeAdjustFactor();
		double adjustedPredictedTravelTime = camion.getPredictedTravelTime()/camion.getPredictTimeAdjustFactor();

		//System.out.println("enregistre temps parcours : "+ODKey+" "+adjustedRealTravelTime);
		
		//enregistre le temps reel de transport
		//
		//cree l'element du hash au besoin
		if(!this.historyMap.containsKey(ODKey)) {
			this.historyMap.put(ODKey, new ArrayList<Double>());
		}
		this.historyMap.get(ODKey).add(adjustedRealTravelTime);

		//enregistre le temps prédit de transport
		//
		//cree l'element du hash au besoin
		if(!this.predictionMap.containsKey(ODKey)) {
			this.predictionMap.put(ODKey, new ArrayList<Double>());
		}
		this.predictionMap.get(ODKey).add(adjustedPredictedTravelTime);
	}

	/**
	 * 
	 * @return index associe a la fonction de prediction utilisee
	 */
	public int getfPredictFunction() {
		return this.fPredictFunction;
	}



	/**
	 * prédit le temps de parcours selon la formule spécifiée
	 * @param origine station d'origine
	 * @param destination station de destination 
	 * @param camion camion
	 * @return Temps de parcours prédit
	 */
	public double predictTravelTime(Station origine, Station destination, Camion camion) {
		double predictedTime = 0;
		if(this.fPredictFunction == TravelTimePredictor.PREDICT_FUNCTION_AVG_PREV) {
			predictedTime =  predictTravelTimeAveragePrev(origine, destination);
		}
		else if(this.fPredictFunction == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED) {
			predictedTime =  predictTravelTimeWeighted(origine, destination);
		}
		else if(this.fPredictFunction == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED_ERROR) {
			predictedTime = predictTravelTimeWeightedError(origine, destination);
		}
		else {
			throw new IllegalStateException("la fonction de prédiction n'est pas bien définie!");
		}
		
		//System.out.println("predicted travel time "+(predictedTime*c.getPredictTimeAdjustFactor()));
		return predictedTime*camion.getPredictTimeAdjustFactor();

	}


	/**
	 * Modifie la valeur de N pour la formule de prédiction
	 * @param nbSample valeur de N
	 */
	public void setNumberSample(int nbSample) {
		this.predictFunctionNumberSample = nbSample;
	}

	/**
	 * Modifie la fonction de prediction 
	 * @param newPredictFunctionIndex nouvel index de fonction de prediction
	 */
	public void setPredictFunction(int newPredictFunctionIndex) {
		this.fPredictFunction = newPredictFunctionIndex;

	}

	/**
	 * Modifie la valeur de "lambda" pour la formule de prédiction
	 * @param lambdaValue valeur de lambda
	 */
	public void setWeight(double lambdaValue) {
		this.predictFunctionWeight = lambdaValue;

	}



	/**
	 * Prédit le temps de parcours selon la formule "moyenne des n derniers voyages"
	 * option "moyenne des observations precedentes" dans le logiciel
	 * @param origine origine
	 * @param destination destination
	 * @return Temps de parcours prédit
	 */
	private double predictTravelTimeAveragePrev(Station origine, Station destination) {

		//retrouve l'historique des temps de parcours
		try {
			String ODKey = getMapKeyForODPair(origine, destination);
			ArrayList<Double> historyForOD = this.historyMap.get(ODKey);

			//si aucun historique, retourne -1
			if(historyForOD == null || historyForOD.size() == 0) {

				return -1;
			}

			int historySize = historyForOD.size();
			int nbEchantillon = this.predictFunctionNumberSample;


			//si historique trop petit, diminue le nombre de points
			if(historySize < nbEchantillon) {
				nbEchantillon = historySize;
			}

			//calcule la moyenne des nbEchantillon derniers temps de parcours
			//
			double avgTime = 0;
			for(int i = historySize-nbEchantillon; i < historySize ; i++ ) {
				avgTime += historyForOD.get(i); 
			}
			avgTime = avgTime/nbEchantillon;

			//System.out.println("retourne "+avgTime);
			return avgTime;
		}
		catch (IllegalArgumentException e){
			e.printStackTrace();
		}
		return 0;
		
	}

	/**
	 * Prédit le temps de parcours selon la formule "combinaison convexe"
	 * option "combinaison convexe" dans le logiciel
	 * @param origine origine
	 * @param destination destination
	 * @return temps de parcours prédit
	 */
	private double predictTravelTimeWeighted(Station origine, Station destination) {
		//retrouve l'historique des temps de parcours
		String ODKey = getMapKeyForODPair(origine, destination);
		ArrayList<Double> historyForOD = this.historyMap.get(ODKey);

		ArrayList<Double> predictedForOD = this.predictionMap.get(ODKey);

		//si aucun historique, retourne -1
		if(historyForOD == null || predictedForOD == null || historyForOD.size() == 0 || predictedForOD.size() == 0) {
			return -1;
		}

		if(historyForOD.size() != predictedForOD.size()) {
			throw new IllegalStateException();
		}

		int historySize = historyForOD.size();

		//si premiere prediction, retourne le vrai temps de calcul precedent
		if(historySize == 1) {
			return historyForOD.get(0);
		}
		//sinon, effectue la somme pondérée
		else {


			double prevRealTime = historyForOD.get(historySize-1);
			double prevPredict = predictedForOD.get(historySize-1);
			if(prevPredict <= 0) {
				return prevRealTime;
			}

			double weight = this.predictFunctionWeight;
			return weight*prevRealTime + (1-weight)*prevPredict;
		}

	}




	//Option "erreur précédente" dans le logiciel
	/**
	 * Prédit le temps de parcours selon la formule "erreur précédente"
	 * option "erreur précédente" dans le logiciel
	 * @param origine origine
	 * @param destination destination
	 * @return Temps de parcours prédit
	 */
	private double predictTravelTimeWeightedError(Station origine, Station destination) {

		ArrayList<Pelle> pelles = mine.getPelles();
		//Station concentrateur = mine.getConcentrateur();
		//Station sterile = mine.getSterile();

		//retrouve l'historique des temps de parcours
		String ODKey = getMapKeyForODPair(origine, destination);
		ArrayList<Double> historyForOD = this.historyMap.get(ODKey);

		ArrayList<Double> predictedForOD = this.predictionMap.get(ODKey);


		//si aucun historique, retourne -1
		if(historyForOD == null || predictedForOD == null || historyForOD.size() == 0 || predictedForOD.size() == 0) {
			//System.out.println(ODKey+" retourne -1 : pas d'historique");
			return -1;
		}

		if(historyForOD.size() != predictedForOD.size()) {
			throw new IllegalStateException();
		}

		int historySize = historyForOD.size();

		//si premiere prediction, retourne le vrai temps de calcul precedent
		if(historySize == 1) {
			//System.out.println(ODKey+" premiere prediction, retourne "+historyForOD.get(0));
			return historyForOD.get(0);
		}
		//sinon, effectue la somme pondérée
		else {

			int nbEchantillon = this.predictFunctionNumberSample;
			if(nbEchantillon > historySize) {
				nbEchantillon = historySize;
			}


			double premierTerme =  predictTravelTimeWeighted(origine, destination);

			double sommeErreurs = 0;
			double sommeTemps = 0;

			//calcule la somme des erreurs sur les derniers temps de parcours enregistres
			for(int i = 0 ; i < pelles.size();i++) {

				Pelle p = pelles.get(i);

				//erreur concentrateur-pelle
				//
				for(int j = 0 ; j < mine.getConcentrateurs().size(); j++) {
					String key = getMapKeyForODPair(mine.getConcentrateurs().get(j), p);

					ArrayList<Double> hm = historyMap.get(key);
					ArrayList<Double> pm = predictionMap.get(key);

					if(hm!= null && pm != null) {
						sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
						sommeTemps+= hm.get(hm.size()-1);
					}
					key = getMapKeyForODPair(p, mine.getConcentrateurs().get(j));

					hm = historyMap.get(key);
					pm = predictionMap.get(key);

					if(hm!= null && pm != null) {
						sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
						sommeTemps+= hm.get(hm.size()-1);
					}
				}
				
				for(int j = 0 ; j < mine.getSteriles().size(); j++) {
					//erreur sterile-pelle
					String key = getMapKeyForODPair(mine.getSteriles().get(j), p);
	
					ArrayList<Double> hm = historyMap.get(key);
					ArrayList<Double> pm = predictionMap.get(key);
	
					if(hm!= null && pm != null) {
						sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
						sommeTemps+= hm.get(hm.size()-1);
					}
					
					key = getMapKeyForODPair(p, mine.getSteriles().get(j));

					hm = historyMap.get(key);
					pm = predictionMap.get(key);

					if(hm!= null && pm != null) {
						sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
						sommeTemps+= hm.get(hm.size()-1);
					}
				}
			}



			double deuxiemeTerme = 1+sommeErreurs/sommeTemps;

			if(sommeTemps == 0) {
				deuxiemeTerme = 1;
			}

			//System.out.println(ODKey+" cas normal, retourne "+(premierTerme*deuxiemeTerme));
			return premierTerme*deuxiemeTerme;
		}

	}

	/**
	 * 
	 * @return Liste des noms des fonctions de prédiction et des identifiants associes.
	 */
	public static ArrayList<Pair<Integer, String>> travelTimePredictFunctionNames() {
		ArrayList<Pair<Integer, String>> names = new ArrayList<Pair<Integer, String>>();
		
		names.add(new Pair<Integer, String>(PREDICT_FUNCTION_AVG_PREV, "Moyenne des observations précédentes"));
		names.add(new Pair<Integer, String>(PREDICT_FUNCTION_WEIGTED, "Combinaison convexe"));
		names.add(new Pair<Integer, String>(PREDICT_FUNCTION_WEIGTED_ERROR , "Erreur précédente"));
		
		return names;
	}


	/**
	 * Reset les historiques de temps réels et predits.
	 */
	public void resetStats() {
		this.historyMap.clear();
		this.predictionMap.clear();
	}




}
