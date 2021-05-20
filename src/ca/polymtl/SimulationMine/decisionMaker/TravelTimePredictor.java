package ca.polymtl.SimulationMine.decisionMaker;

import java.util.ArrayList;
import java.util.HashMap;

import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.Station;

public class TravelTimePredictor {

	//Prediction du temps de parcours
	//
	public static final int PREDICT_FUNCTION_AVG_PREV = 1;
	public static int PREDICT_FUNCTION_WEIGTED = 2;
	public static int PREDICT_FUNCTION_WEIGTED_ERROR = 3;
	public final static int DEFAULT_PREDICT_FUNCTION = PREDICT_FUNCTION_AVG_PREV;
	public final static int DEFAULT_PREDICT_FUNCTION_NB_SAMLE = 4;
	public final static double DEFAULT_PREDICT_FUNCTION_WEIGHT = 0.5;
	//Prediction des temps de parcours
	//
	//fonction de prediction de temps de parcours
	private int fPredictFunction;
	private int predictFunctionNumberSample;
	private double predictFunctionWeight;

	
	//historique des temps de parcours. La clé est une string indiquant l'origine/destination
	//une prédiction de -1 signifie aucune prédiction
	private HashMap<String, ArrayList<Double>> historyMap;
	private HashMap<String, ArrayList<Double>> predictionMap;
	private Mine mine;

	/*
	 * Constructeur
	 */
	public TravelTimePredictor(Mine mine) {

		this.mine = mine;
		this.fPredictFunction = TravelTimePredictor.DEFAULT_PREDICT_FUNCTION;

		this.predictFunctionNumberSample = TravelTimePredictor.DEFAULT_PREDICT_FUNCTION_NB_SAMLE;
		this.predictFunctionWeight = TravelTimePredictor.DEFAULT_PREDICT_FUNCTION_WEIGHT;
		
		//historique
		this.historyMap = new HashMap<String, ArrayList<Double>>();
		this.predictionMap = new HashMap<String, ArrayList<Double>>();
		
	}




	//prédit le temps de parcours selon la formule spécifiée
	public double predictTravelTime(Station origine, Station destination) {
		if(this.fPredictFunction == TravelTimePredictor.PREDICT_FUNCTION_AVG_PREV) {
			return predictTravelTimeAveragePrev(origine, destination);
		}
		else if(this.fPredictFunction == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED) {
			return predictTravelTimeWeighted(origine, destination);
		}
		else if(this.fPredictFunction == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED_ERROR) {
			return predictTravelTimeWeightedError(origine, destination);
		}
		else {
			throw new IllegalStateException("la fonction de prédiction n'est pas bien définie!");
		}

	}

	
	public static String getMapKeyForODPair(Station origine, Station destination) {
		return origine.getId()+":"+destination.getId();
	}


	//Option "erreur précédente" dans le logiciel
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
				String key = getMapKeyForODPair(p.getReturnStation(), p);

				ArrayList<Double> hm = historyMap.get(key);
				ArrayList<Double> pm = predictionMap.get(key);

				if(hm!= null && pm != null) {
					sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
					sommeTemps+= hm.get(hm.size()-1);
				}
				//erreur sterile-pelle
				key = getMapKeyForODPair(p.getReturnStation(), p);

				hm = historyMap.get(key);
				pm = predictionMap.get(key);

				if(hm!= null && pm != null) {
					sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
					sommeTemps+= hm.get(hm.size()-1);
				}

				//erreur pelle-point de retour
				key = getMapKeyForODPair(p, p.getReturnStation());

				hm = historyMap.get(key);
				pm = predictionMap.get(key);

				if(hm!= null && pm != null) {
					sommeErreurs += hm.get(hm.size()-1)-pm.get(pm.size()-1);
					sommeTemps+= hm.get(hm.size()-1);
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



	//option "combinaison convexe" dans le logiciel
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



	//predit selon la moyenne des k derniers voyages
	//option "moyenne des observations precedentes" dans le logiciel
	private double predictTravelTimeAveragePrev(Station origine, Station destination) {

		//retrouve l'historique des temps de parcours
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
	
	//enregistre l'historique de temps de parcours du camion
	// Cela inclus le temps réel de parcours et le temps prédit
	//
	public void enregistreHistoriqueTempsParcours(Camion camion) {
		//lance une exceptions si le camion n'est pas dans l'étata just arrived
		//
		if(camion.getState() != Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalStateException("le camion doit etre dans l'état Camion.STATE_JUST_ARRIVED");
		}

		//clé d'origine/destination
		String ODKey = getMapKeyForODPair(camion.getOrigine(), camion.getObjective());


		//enregistre le temps reel de transport
		//
		//cree l'element du hash au besoin
		if(!this.historyMap.containsKey(ODKey)) {
			this.historyMap.put(ODKey, new ArrayList<Double>());
		}
		this.historyMap.get(ODKey).add(camion.getCurrentTravelTime());

		//enregistre le temps prédit de transport
		//
		//cree l'element du hash au besoin
		if(!this.predictionMap.containsKey(ODKey)) {
			this.predictionMap.put(ODKey, new ArrayList<Double>());
		}
		this.predictionMap.get(ODKey).add(camion.getPredictedTravelTime());
	}




	public void setPredictFunction(int newPredictFunctionIndex) {
		this.fPredictFunction = newPredictFunctionIndex;
		
	}




	public void setWeight(double rhoValue) {
		this.predictFunctionWeight = rhoValue;
		
	}




	public void setNumberSample(int nbSample) {
		this.predictFunctionNumberSample = nbSample;
	}




	public int getfPredictFunction() {
		return this.fPredictFunction;
	}




}
