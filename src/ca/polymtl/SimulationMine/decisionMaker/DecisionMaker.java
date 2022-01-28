package ca.polymtl.SimulationMine.decisionMaker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;

import bsh.EvalError;
import bsh.Interpreter;
import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.MineSimulator.Station;
import ca.polymtl.SimulationMine.MineSimulator.Sterile;
import lpsolve.*; 

/**
 * 
 * @author Fred
 *
 *	Classe en charge de prendre des décisions en temps réel. Il faut décider : 
 *		- A quelle pelle envoyer un camion lorsqu'il est disponible.
 *		- A quelle station de décharge envoyer un camion lorsqu'il est plein.
 *		- Comment modifier le plan d'opérations lorsqu'il y a une panne.
 */
public class DecisionMaker {

	//==========================================
	// Constantes
	//==========================================

	//fonction des score par defaut
	//
	/**Fonction de score : Assignation aleatoire des camions*/
	public static final String ALEATOIRE_FUNCTION_STRING = "aleatoire";

	/**Fonction de score : Assigne les camions un a un, mais avec la meme formule que pour le probleme d'affectation qui maximise le respect du plan*/
	protected static final String OPTIMAL_PLAN_SCORE_FUNCTION_STRING = "optimal_assign_plan";

 
	/**Fonction de score : Maximiser la production l'aide d'un problème d'affectation*/
	protected static final String OPTIMIZE_PROD_FUNCTION_STRING = "optimise_production";


	/**Fonction de score : Maximiser le respect du plan à l'aide d'un problème d'affectation*/
	protected static final String OPTIMIZE_PLAN_FUNCTION_STRING = "optimise_plan";

	/**Fonction de score : En warmup, on assigne les camions aleatoirement*/
	public static final String WARMUP_SCORE_FUNCTION_STRING = "aleatoire";


	//===============================
	//CHAMPS
	//===============================
	
	/**classe contenant la configuration*/
	private static Config config;

	/**Mine*/
	protected static Mine mine;

	/**
	 * Valide une fonction de score. Pour ce faire, on commence par regarder si la fonction de score correspond à une fonction de score définie comme constante. Si oui, la fonction est valide.
	 * Sinon, cree un camion dummy et tente de calculer un score pour un camion. Si cela ne cause pas d'erreur, la fonction est valide.
	 * 
	 * @param function : string correspondant a une fonction de score fournie par l'utilisateur.
	 * @return true si la fonction de score est valide, false sinon
	 */
	public static boolean isFunctionStringValid(String function) {
		
		//valide si il s'agit d'une des fonction de score predefinie
		//
		if(	function.equals(DecisionMaker.ALEATOIRE_FUNCTION_STRING) ||
				function.equals(DecisionMaker.OPTIMAL_PLAN_SCORE_FUNCTION_STRING) ||
				function.equals(DecisionMaker.OPTIMIZE_PROD_FUNCTION_STRING) ||
				function.equals(DecisionMaker.OPTIMIZE_PLAN_FUNCTION_STRING) ||
				function.equals(DecisionMaker.WARMUP_SCORE_FUNCTION_STRING) ) {
			return true;
		}
	
		Camion camion = new Camion(mine.getSteriles().get(0), mine,  null) {
	
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
				return 0;
			}
	
			@Override
			public double getStdSpeed() {
				return 0;
			}
	
		};
		Pelle pelle = mine.getPelles().get(0);
	
	
	
		if(function.equals(DecisionMaker.OPTIMIZE_PROD_FUNCTION_STRING)) return true;
	
		try {
	
			DecisionMaker dm = new DecisionMaker(mine, config);
			dm.computeDecisionScore(camion,pelle, function);
		} catch (EvalError e) {
			return false;
		}
		return true;
	}

	 



	//==========================================
	// Champs
	//==========================================

	/**Discount factor pour le camion qu'on souhaite affecter*/
	private double affectDiscountFactor;
	/**Fonction de score pour les petits camions pour la simulation*/
	private String scoreFunctionSmallCamionsString;

	/**Fonction de score pour les gros camions pour la simulation*/
	private String scoreFunctionLargeCamionsString;

	/**Solveur*/
	private LpSolve solver;

	/**
	 * Constructeur
	 * @param mine mine
	 * @param config objet config
	 */
	@SuppressWarnings("static-access")
	public DecisionMaker(Mine mine, Config config) {
		this.mine = mine;

		this.scoreFunctionSmallCamionsString = config.getDefaultScoreFunctionSmallCamions();
		this.scoreFunctionLargeCamionsString = config.getDefaultScoreFunctionLargeCamions();
		this.affectDiscountFactor = config.getAffectDiscountFactor();
		
		this.config = config;
	}

	/**
	 * Calcule le score associé à l'assignation d'un camion à une pelle pour le problème d'affectation
	 * @param camion
	 * @param pelle
	 * @return score
	 */
	//Cette fonction mériterait d'être réécrite, mais j'ai eu beaucoup de misère a la faire fonctionner alors je ne la briserai pas tout de suite!!
	protected double affectScoreOptPlan(Camion camion, Pelle pelle) {
		//System.out.println(camion.getState());
		// - si le camion est en route vers un concentrateur ou un stérile, 
		//   ou si le camion est déjà à un concentrateur ou un stérile, on calcule le temps avant qu'il arrive à la pelle
		// - sinon, retourne 0 (dans le problème d'assignation, le score sera 0 pour toutes les pelles, donc c'est comme si on ne prenait pas en compte le camion).
		if(		(camion.getState() == Camion.ETAT_INACTIF && camion.getCurrentStation().isDecharge == true) ||
				(camion.getState() == Camion.ETAT_EN_ROUTE && camion.getDestination().isDecharge == true) || //si le camion va se faire décharger
				(camion.getState() == Camion.ETAT_EN_TRAITEMENT && camion.getCurrentStation().isDecharge == true) || //si le camion est en traitement
				(camion.getState()== Camion.ETAT_ATTENTE && camion.getCurrentStation().isDecharge == true) //si le camion est en attente de se faire décharger
				) {
			
			
			Station s = null;
			if(camion.getState() == Camion.ETAT_EN_ROUTE) {
				s = camion.getDestination();
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
				if(c.getState() == Camion.ETAT_EN_ROUTE && c.getDestination().equals(pelle) && c.getLocation().distance(pelle.getLocation()) < distanceEntreStationEtPelle) {
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



			double penaliteQuadAttentePelle = (attenteEspereePelle/3600. - pelle.cibleAttentePelleSeconds()/3600.) *(attenteEspereePelle/3600. - pelle.cibleAttentePelleSeconds()/3600.);

			double penaliteQuadAttenteCamion = (attenteEspereeCamion/3600.)*(attenteEspereeCamion/3600.);// pelle.cibleAttenteCamionSeconds()/3600.);

			//DEBUG
			if(false) {

				System.out.println("attente esperee pelle  : "+attenteEspereePelle);
				System.out.println("attente esperee camion : "+attenteEspereeCamion);
				System.out.println("penalite pelle         : "+penaliteQuadAttentePelle);
				System.out.println("penalite camion        : "+penaliteQuadAttenteCamion);
				System.out.println("score                  : "+(penaliteQuadAttentePelle+penaliteQuadAttenteCamion));	
			}

			return 0.000001*(penaliteQuadAttentePelle+penaliteQuadAttenteCamion);
		}
		else {
			return 0;
		}

	}


	/**
	 * Score d'affectation d'un camion a une pelle pour maximiser la production totale
	 * @param camion
	 * @param pelle
	 * @return score
	 */
	private double affectScoreOptProd(Camion camion, Pelle pelle) {
		double attenteEspereePelle = calculeTempsAttenteEspereePelle(camion, pelle);
		//if(attenteEspereePelle < 0) attenteEspereePelle = 0;
		double tempsEspereAvantDebutRemplissage = calculeTempsEspereAvantTraitement(camion, pelle);
		double distanceEntreCamionEtPelle = calculeDistanceEntreCamionEtStation(camion, pelle);
	
		double tempsDeParcoursEspere = distanceEntreCamionEtPelle/( camion.getAvgSpeed()*mine.getMeteoFactor());
		double attenteEspereeCamion = tempsEspereAvantDebutRemplissage - tempsDeParcoursEspere;
		//if(attenteEspereeCamion < 0) {
		//	attenteEspereeCamion = 0;
		//}
		//return attenteEspereePelle*Math.abs(attenteEspereePelle)+ attenteEspereeCamion*Math.abs(attenteEspereeCamion);
		
		//On veut minimiser l'attente esperee de la pelle, mais lorsque plusieurs pelles ont une attente esperee egale, on veut prendre la pelle la plus proche.
		//La constante 0.06 a ete selectionnee par essai erreur.
		return Math.abs(attenteEspereePelle)+0.06*distanceEntreCamionEtPelle;
	}


	/**
	 * Calcule la distance entre le camion et la pelle (m). Pour que cette fonction fonctionne, il faut que le camion soit :
	 *  - en route vers la station.
	 *  - en route vers une autre station b telle que le chemin b--station existe.
	 *  - a la station en ce moment.
	 *  - a une autre station b tel qu'il existe un chemin entre b et station.
	 *   
	 * @param camion
	 * @param station
	 * @return distance entre camion et station.
	 */
	protected double calculeDistanceEntreCamionEtStation(Camion camion, Station station) {

		double distanceEntreCamionEtPelle = 0;
		if(camion.getState() == Camion.ETAT_EN_ROUTE && camion.getDestination().equals(station)) {
			distanceEntreCamionEtPelle = camion.getLocation().distance(station.getLocation());
		}
		else if(camion.getState() == Camion.ETAT_EN_ROUTE && !camion.getDestination().equals(station) && mine.routeEntre(camion.getDestination(), station)) {
			distanceEntreCamionEtPelle = camion.getLocation().distance(camion.getDestination().getLocation()) + camion.getDestination().getLocation().distance(station.getLocation());
		}
		//si le camion est a une station qui est soit "station" soit une autre station b telle que le chemin b -- station existe.
		else if((camion.getState() == Camion.ETAT_EN_TRAITEMENT || camion.getState() == Camion.ETAT_ATTENTE || camion.getState() == Camion.ETAT_INACTIF || camion.getState() == Camion.ETAT_JUSTE_ARRIVE) && 
				(camion.getLocation().equals(station) || mine.routeEntre(station, camion.getCurrentStation()))) {
			distanceEntreCamionEtPelle = camion.getCurrentStation().getLocation().distance(station.getLocation());
		}
		else {
			throw new IllegalStateException("DistanceEntreCamionEtStation : impossible d'évaluer la distance pour ce camion.");
		}
		return distanceEntreCamionEtPelle;
	}

	/**
	 * Calcule, si le camion est assigné à la pelle, le temps que la pelle attendra avant que le camion commence à se faire traiter.
	 * Prends en compte les camions en route vers la pelle. 
	 * Si la pelle n'est pas disponible lorsque le camion arrive, on soustrait le temps restant avant que la pelle termine. 
	 * De cette manière, le temps d'attente peut être négatif.
	 * @param camion
	 * @param pelle
	 * @return temps d'attente espere
	 */
	protected double calculeTempsAttenteEspereePelle(Camion camion, Pelle pelle) {
		boolean debug = false;

		double tempsAttente = 0;

		double tempsRestantAvantFinPelle = calculeTempsRestantAvantFinPelle(pelle);

		double tempsCamionPelle = camion.getLocation().distance(pelle.getLocation())/camion.getAvgSpeed();
		//liste des camions en route pour la pelle qui vont arriver avant le camion.
		ArrayList<Camion> camionsEnRoute = new ArrayList<Camion>();
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			Camion c = mine.getCamions().get(i);
			double tempsCPelle = c.getLocation().distance(pelle.getLocation())/c.getAvgSpeed();
			if(c.getState()==Camion.ETAT_EN_ROUTE && c.getDestination().equals(pelle) && tempsCPelle < tempsCamionPelle) {
				camionsEnRoute.add(c);
			}
		}

		//trie les camions par ordre d'arrivée.
		camionsEnRoute.sort(new Comparator<Camion>() {
			@Override
			public int compare(Camion c1, Camion c2) {
				double tempsC1 = c1.getLocation().distance(pelle.getLocation())/c1.getAvgSpeed()/mine.getMeteoFactor();
				double tempsC2 = c2.getLocation().distance(pelle.getLocation())/c2.getAvgSpeed()/mine.getMeteoFactor();

				if(tempsC1-tempsC2 < 0 ) {
					return -1;
				}
				return 1;
			}
		});

		//ajoute en dernier notre camion
		camionsEnRoute.add(camion);


		//debug
		if(debug) {
			System.out.println("DEBUG ORDRE");
			for(int i = 0 ; i < camionsEnRoute.size(); i++ ) {
				double temps = camionsEnRoute.get(i).getLocation().distance(pelle.getLocation())/camionsEnRoute.get(i).getAvgSpeed();
				System.out.println("temps "+temps);
			}
		}


		//ajoute le temps necessaire pour chaque camion
		//
		for(int i = 0 ; i < camionsEnRoute.size(); i++) {

			double tempsArrivee = camionsEnRoute.get(i).getLocation().distance(pelle.getLocation())/camionsEnRoute.get(i).getAvgSpeed()/mine.getMeteoFactor();
			//si arrive alors que la pelle travaille, il attends et on ajuste le nouveau temps avant que la pelle n'ait terminée
			if(tempsArrivee <tempsRestantAvantFinPelle) {
				tempsRestantAvantFinPelle += camionsEnRoute.get(i).getChargeMax()/pelle.averageTraitementSpeed();
				if(i == camionsEnRoute.size()-1) {
					tempsAttente -= tempsRestantAvantFinPelle - tempsArrivee;
				}
			}
			//si arrive alors que la pelle attends, calcule le temps d'attente et ajuste le nouveau temps avant que la pelle n'ait terminée
			else {
				tempsAttente += tempsArrivee= tempsRestantAvantFinPelle;

				tempsRestantAvantFinPelle += tempsArrivee + camionsEnRoute.get(i).getChargeMax()/pelle.averageTraitementSpeed();

			}
			if(debug) {
				System.out.println("temps attente                 : "+tempsAttente);
				System.out.println("temps restant avant fin pelle : "+tempsRestantAvantFinPelle);
			}
		}
		return tempsAttente;
	}


	/**
	 * Calcule le temps (espere) avant que le camion soit disponible. Pour que cette fonction fonctionne, il faut que le camion soit : 
	 *  - En route pour se faire décharger.
	 *  - En train de se faire décharger.
	 *  - En attente pour se faire décharger.
	 *  - Inactif (donc disponible maintenant)
	 *  Dans les autres états, le temps avant que le camion soit disponible dépend de décisions futures, qu'on ne peut prévoir.
	 * @param camion
	 * @return temps (espere) avant que le camion soit disponible.
	 */
	protected double calculeTempsAvantDispo(Camion camion) {



		if(camion.getState() == Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion qui vient juste d'arriver");
		}
		else if(camion.getState() == Camion.ETAT_EN_TRAITEMENT && !camion.getCurrentStation().isDecharge) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion en train de se faire traiter à une pelle");
		}
		else if(camion.getState() == Camion.ETAT_ATTENTE&& !camion.getCurrentStation().isDecharge) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion en attente à une pelle");
		}
		else if(camion.getState()== Camion.ETAT_EN_ROUTE && !camion.getDestination().isDecharge) {
			throw new IllegalArgumentException("impossible d'évaluer le temps avant dispo pour un camion en route vers une pelle");
		}



		//si le camion est inactif, il est disponible MAINTENANT
		//
		else if(camion.getState()== Camion.ETAT_INACTIF) {
			return 0;
		}
		//si le camion est en route pour se faire décharger
		else if(camion.getState() == Camion.ETAT_EN_ROUTE && camion.getDestination().isDecharge) {
			Station objective = camion.getDestination();
			//temps avant d'arriver
			double distance = camion.getLocation().distance(objective.getLocation());
			double tempsDeplacement = distance/(camion.getSpeed()*mine.getMeteoFactor());


			//temps avant que la station ne commence à traiter le camion
			double tempsTraitementCurrent = 0;
			if(camion.getDestination().getCamionEnTraitement()!= null) {
				tempsTraitementCurrent = objective.getCamionEnTraitement().getCharge()/objective.averageTraitementSpeed();
			}
			double qteATraiter = 0;
			//ajoute les camion en route vers la pelle et plus pres
			for(int i = 0 ; i < mine.getCamions().size(); i++) {
				Camion c = mine.getCamions().get(i);
				if(c.getState() == Camion.ETAT_EN_ROUTE && 
						c.getDestination().equals(objective) && 
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
			return camion.getCharge()/camion.getDestination().averageTraitementSpeed();

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


	//-----------------------------------------------------------------
	// Calcule le tmeps espere avant que le camion commence à se faire traiter à la pelle
	// En considérant qu'il s'y rend dès que possible
	//-----------------------------------------------------------------
	/**
	 * Calcule le temps espere avant que le camion ne commence à se faire traiter a la station s'il s'y rend dès qu'il est disponible.
	 * 
	 * Si le camion est a une station de traitement ou en route vers une station de traitement, on suppose que l'assignation vers station est possible ensuite.
	 * 
	 * Pour que cette fonction fonctionne, il faut que le camion soit :
	 * 	- Inactif (disponible maintenant)
	 *  - En route pour la station.
	 *  - En route vers, en attente, ou en traitenement à une autre station b  ET qu'il y a une route entre b et "station"
	 *  
	 * @param camion
	 * @param station
	 * @return temps espere avant que le camion ne commence à se faire traiter a la pelle s'il s'y rend dès qu'il est disponible.
	 */
	protected double calculeTempsEspereAvantTraitement(Camion camion, Station station) {

		boolean debug = false;


		double tempsAvantArrivee = 0;

		//le camion doit seulement se rendre à la station
		if(camion.getState() == Camion.ETAT_INACTIF && camion.getCurrentStation()!= null) {
			tempsAvantArrivee += camion.getLocation().distance(station.getLocation())/camion.getSpeed()/mine.getMeteoFactor();
		}
		else if(camion.getState() == Camion.ETAT_EN_ROUTE && camion.getDestination().equals(station)) {
			tempsAvantArrivee += camion.getLocation().distance(station.getLocation())/camion.getSpeed()/mine.getMeteoFactor();
		}
		//le camion doit se rendre à sa destination, attendre, puis se faire traiter, puis se rendre à la station
		else if(camion.getState() == Camion.ETAT_EN_ROUTE && ! camion.getDestination().equals(station) && mine.routeEntre(station, camion.getDestination())) {
			tempsAvantArrivee += calculeTempsEspereAvantTraitement(camion, camion.getDestination());
			if(camion.getDestination().isDecharge) {
				tempsAvantArrivee += camion.getCharge()/camion.getDestination().averageTraitementSpeed();	
			}
			else {
				tempsAvantArrivee += (camion.getChargeMax() - camion.getCharge())/camion.getDestination().averageTraitementSpeed();
			}
			tempsAvantArrivee += camion.getLocation().distance(station.getLocation())/camion.getSpeed()/mine.getMeteoFactor();
		}
		//le camion doit finir de se faire traiter, puis se rendre
		else if(camion.getState() == Camion.ETAT_EN_TRAITEMENT && mine.routeEntre(station, camion.getCurrentStation())) {
			if(camion.getCurrentStation().isDecharge) {
				tempsAvantArrivee += camion.getCharge()/camion.getCurrentStation().averageTraitementSpeed();
			}
			else {
				tempsAvantArrivee += (camion.getChargeMax()-camion.getCharge())/camion.getCurrentStation().averageTraitementSpeed();
			}
			tempsAvantArrivee += camion.getLocation().distance(station.getLocation())/camion.getSpeed()/mine.getMeteoFactor();
		}
		else if(camion.getState() == Camion.ETAT_ATTENTE) {
			//temps pour traiter le camion en cours
			//
			Station currentStation = camion.getCurrentStation();
			if(currentStation.isDecharge) {
				tempsAvantArrivee += currentStation.getCamionEnTraitement().getCharge()/currentStation.averageTraitementSpeed();
				for(int i = 0 ; i < currentStation.getCamionsEnAttente().size(); i++) {
					if(currentStation.getCamionsEnAttente().get(i).equals(camion)) {
						break;
					}
					else {
						tempsAvantArrivee += currentStation.getCamionsEnAttente().get(i).getCharge()/currentStation.averageTraitementSpeed();
					}
				}
			}
			else {
				tempsAvantArrivee += (currentStation.getCamionEnTraitement().getChargeMax() - currentStation.getCamionEnTraitement().getCharge())/currentStation.averageTraitementSpeed();
				for(int i = 0 ; i < currentStation.getCamionsEnAttente().size(); i++) {
					if(currentStation.getCamionsEnAttente().get(i).equals(camion)) {
						break;
					}
					else {
						tempsAvantArrivee += (currentStation.getCamionsEnAttente().get(i).getChargeMax()- currentStation.getCamionsEnAttente().get(i).getCharge())/currentStation.averageTraitementSpeed();
					}
				}

			}
			tempsAvantArrivee += camion.getLocation().distance(station.getLocation())/camion.getSpeed()/mine.getMeteoFactor();
		}
		else {
			throw new IllegalStateException("Mauvais état pour le camion");
		}


		double tempsParcoursRestantCamionOpt = tempsAvantArrivee;
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
				if(camionIter.getState() == Camion.ETAT_EN_ROUTE && camionIter.getDestination() == station) {
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

	/**
	 * Calcule le temps avant que la pelle termine sa file d'attente actuelle. Ne prends pas en compte les camions en route pour la pelle
	 * @param station
	 * @return temps avant que la pelle termine sa file d'attente actuelle.
	 */
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


	/**
	 * Calcule un score associé à l'assignation d'un camion à une pelle, selon une fonction de score donnée en entrée.
	 * @param camion
	 * @param pelle
	 * @param scoreFunctionString : chaine de caracteres representant la fonction de score a utiliser
	 * @return score calcule
	 * @throws EvalError
	 */
	private double computeDecisionScore(Camion camion, Pelle pelle, String scoreFunctionString) throws EvalError {

		boolean objectiveIsConcentrator = false;
		boolean objectiveIsSterile = false;

		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
			if(camion.getDestination() == mine.getConcentrateurs().get(i)) {
				objectiveIsConcentrator = true;
				break;
			}
		}
		for(int i = 0 ; i < mine.getSteriles().size(); i++) {
			if(camion.getDestination() == mine.getSteriles().get(i)) {
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
		double optimalPlanAssignCost = affectScoreOptPlan(camion, pelle);

		if(scoreFunctionString.equals(DecisionMaker.OPTIMAL_PLAN_SCORE_FUNCTION_STRING)){
			return optimalPlanAssignCost;
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
			if(camions.get(i).getState() == Camion.ETAT_EN_ROUTE && camions.get(i).getDestination() == pelle) {
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
		double attenteEspereePelle = calculeTempsAttenteEspereePelle(camion, pelle);
		//double penaliteQuadAttentePelle = calculePenaliteQuadAttentePelle(attenteEspereePelle, pelle.cibleAttentePelleSeconds());
		//double penaliteQuadAttenteCamion = calculePenaliteQuadAttenteCamion(attenteEspereeCamion, pelle.cibleAttenteCamionSeconds());


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

		//interpreter.set("ecart_cible_quadratique", penaliteQuadAttentePelle);

		interpreter.set(OPTIMAL_PLAN_SCORE_FUNCTION_STRING, optimalPlanAssignCost);

		//interpreter.set("optimise_min_attente_pelle", attenteEspereePelle*Math.abs(attenteEspereePelle)+ attenteEspereeCamion*attenteEspereeCamion*0.5);

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

	/**
	 * Trouve des camions qui sont "optimisables". Un camion est optimisable si il est :
	 * 	- En route pour se faire décharger.
	 *  - En attente ou en traitement pour se faire décharger
	 *  On doit choisir autant de camions que de pelles. Si il n'y a pas assez de camions optimisables, ajoute des copies du dernier camion.
	 *  
	 * @param optimizablePelles Pelles qui sont optimisables
	 * @param camionToAssign Camion que l'on essaie d'assigner.
	 * @return liste de camions optimisables.
	 */
	private ArrayList<Camion> findOptimizableCamions(ArrayList<Pelle> optimizablePelles, Camion camionToAssign) {
		ArrayList<Camion> optimisableCamions = new ArrayList<Camion>();

		optimisableCamions.add(camionToAssign);

		@SuppressWarnings("unchecked")
		//liste des potentiels camions
		ArrayList<Camion> camionsClone = (ArrayList<Camion>) mine.getCamions().clone();

		//enleve le camion a assignere de la liste des candidats, puisqu'il doit absolument
		//etre optimise (on le rajoute a la fin)
		camionsClone.remove(camionToAssign);

		//si n pelles, trouve les n-1 camions qui seront disponibles le plus rapidement.
		for(int i = 0 ; i < optimizablePelles.size()-1; i++) {
			Camion bestCamion = null;
			double bestTemps = Double.MAX_VALUE;
			for(int j = 0 ; j < camionsClone.size(); j++) {
				//si le camion se qualifie
				//
				Camion c = camionsClone.get(j);
				if((c.getState() == Camion.ETAT_EN_ROUTE && c.getDestination().isDecharge) ||
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

	}


	/**
	 * 
	 * @return chaine de caracteres correspondant a la fonction de score pour les gros camions.
	 */
	public String getScoreFunctionLargeCamionsString() {
		return scoreFunctionLargeCamionsString;
	}

	/**
	 * @return la chaine de caracteres correspondant a la fonction de score pour les petits camions.
	 */
	public String getScoreFunctionSmallCamionsString() {
		return scoreFunctionSmallCamionsString;
	}




	//donne un nouvel objectif a un camion sans but
	//
	/**
	 * Trouve une nouvelle destination pour un camion
	 * @param camion Camion auquel on doit donner un objectif
	 * @return Station correspondant au nouvel objectif du camion.
	 */
	public Station giveObjectiveToCamion(Camion camion) {

		//si le camion doit se faire décharger
		//
		if(camion.getCurrentStation()!=null && !camion.getCurrentStation().isDecharge) {

			//Choisis les candidats
			//
			ArrayList<Station> candidates = new ArrayList<Station>();

			//on doit retourner vers un sterile
			if(camion.getRockType().isSterile()) {
				for(Sterile s : mine.getSteriles()) {
					candidates.add(s);
				}
			}
			//on doit retourner vers un concentrateur
			else {
				for(Concentrateur c : mine.getConcentrateurs()) {
					candidates.add(c);
				}
			}

			Station returnStation = selectReturnStation(camion, candidates);



			return returnStation;
		}
		//si le camion doit aller se faire remplir
		//
		else {

			//Choisis les candidats
			//
			ArrayList<Pelle> candidates = new ArrayList<Pelle>();
			for(Pelle p : mine.getPelles()) {
				if(p.getState() != Pelle.STATION_STATE_PANNE) {
					candidates.add(p);
				}
			}

			return selectPelleForCamion(camion, candidates);

		}

	}


	/**
	 * Trouve une destination "optimale" pour un camion. Pour ce faire, résout un probleme d'affectation
	 * @param camionToAssign Cammion a affecter.
	 * @param pelles liste des pelles candidates.
	 * @param scoreFunctionString fonction de score
	 * @return pelle correspondat a la destination "optimale".
	 */
	public Pelle giveOptimalObjectiveToCamion(Camion camionToAssign, ArrayList<Pelle> pelles, String scoreFunctionString) {

		ArrayList<Pelle> optimizablePelles = pelles;
		ArrayList<Camion> optimizableCamions = findOptimizableCamions(optimizablePelles, camionToAssign);

		//si il manque de camions, ajoute des clones du dernier camion.
		while(optimizablePelles.size() > optimizableCamions.size()) {
			optimizableCamions.add(optimizableCamions.get(optimizableCamions.size()-1));
		}


		HashMap<Camion, Pelle> optimalAssign = resoutProblemeAffectation(optimizableCamions, optimizablePelles, scoreFunctionString);

		if(!optimalAssign.containsKey(camionToAssign)) {
			//System.out.println("Erreur d'assignation");
			throw new IllegalStateException();
		}
		else {
			//System.out.println("je retourne la pelle "+optimalAssign.get(camionToAssign).getId());
			return optimalAssign.get(camionToAssign);
		}
	} 



	/**
	 * cree et resout un probleme d'affectation de camions a des pelles 
	 * @param camions liste des camions
	 * @param pelles liste des pelles
	 * @param scoreFunctionString 
	 * @return HashMap indiquant a quel camion sera assigne quelle pelle.
	 */
	private HashMap<Camion, Pelle> resoutProblemeAffectation(ArrayList<Camion> camions, ArrayList<Pelle> pelles, String scoreFunctionString	) {

		boolean debug = false;



		if(debug) System.out.println("\nbegin resoutProblemeAssignation");

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
			if(debug) {
				System.out.println("scores : ");
			}
			double[] costFunction = new double[camions.size()*pelles.size()+1];
			int index = 0;
			for(int i = 0 ; i < camions.size(); i++ ) {
				for(int j = 0 ; j < pelles.size(); j++) {
					Camion camion = camions.get(i);
					Pelle pelle = pelles.get(j);


					double score = 0;

					if(scoreFunctionString.equals(DecisionMaker.OPTIMIZE_PLAN_FUNCTION_STRING)) {
						//System.out.println("optimise pour le plan");
						score = affectScoreOptPlan(camion, pelle);
						if(i == 0) {
							score = score * this.affectDiscountFactor;
						}
						//set la fonction de score (le /100000 est pour avoir une stabilite numerique)
						costFunction[index+1] = score;

					}
					else if(scoreFunctionString.equals(DecisionMaker.OPTIMIZE_PROD_FUNCTION_STRING)) {
						//System.out.println("optimise pour la production");
						score = affectScoreOptProd(camion, pelle);
						//discount factor pour le camion qui nous interesse
						if(i == 0) {
							score = score * this.affectDiscountFactor;
						}
						//set la fonction de score (le /1000 est pour avoir une stabilite numerique)
						costFunction[index+1] = score/1000;

					}
					else {
						throw new IllegalArgumentException("Fonction de score inconnue : "+scoreFunctionString);
					}
					
				
					
					
					index ++;
					//if(debug) 
						System.out.println("camion "+i+" "+pelle.getId()+" : "+score);

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
			return assign;
		}
		catch (LpSolveException e) {
			e.printStackTrace();
		}
		if(debug) System.out.println("\nend resoutProblemeAssignation");
		return null;
	}

	/**
	 * Choisis à quelle pelle affecter un camion selon la stratégie indiquée par la fonction de score
	 * @param camion
	 * @param candidates 
	 * @return Pelle à laquelle affecter le camion
	 */
	protected Pelle selectPelleForCamion(Camion camion, ArrayList<Pelle> candidates) {


		//-------------------------------------------------------
		//choisis la methode utilisee pour selectionner la pelle
		//


		// Choisis la fonction de score a utiliser
		//
		String scoreFunction = "";
		if(camion.getType() == Camion.TYPE_SMALL) {

			scoreFunction = this.scoreFunctionSmallCamionsString;
		}
		else if(camion.getType() == Camion.TYPE_LARGE) {
			scoreFunction = this.scoreFunctionLargeCamionsString;
		}
		else {
			throw new IllegalStateException("DecisionMaker::giveObjectiveToCamion : Type de camion inconnu : "+camion.getType());
		}


		//utilise un probleme d'affectation
		//
		if(scoreFunction.equals(OPTIMIZE_PROD_FUNCTION_STRING) || scoreFunction.equals(OPTIMIZE_PLAN_FUNCTION_STRING)) {
			Pelle optimalPelle = giveOptimalObjectiveToCamion(camion, candidates, scoreFunction);
			return optimalPelle;
			//camion.setObjective(optimalPelle);
		}
		//affectation greedy avec fonction de score
		//
		else {
			//on peut donner un objectif seulement si le camion est à une station
			//Lance une erreur si le camion n'est pas a une station

			double maxScore = -Double.MAX_VALUE;
			double minScore = Double.MAX_VALUE;
			Pelle pelleMinScore = null;
			for(int i = 0 ; i < candidates.size(); i++) {

				double score = 0;

				try {
					score = computeDecisionScore(camion, candidates.get(i), scoreFunction);
				} catch (EvalError e) {
					e.printStackTrace();
				}

				if(score >= maxScore) {
					maxScore = score;
				}
				if(score <= minScore) {
					pelleMinScore = candidates.get(i);
					minScore = score;
				}
			}

			
			return pelleMinScore;
		}

	}

	/**
	 * Choisis la station de retours pour un camion qui viens de se faire remplir à la pelle
	 * @param camion Camion a assigner
	 * @param candidates 
	 * @param pelle Pelle ou se trouve le camion
	 * @return Station ou se rendra le camion
	 */
	protected Station selectReturnStation(Camion camion, ArrayList<Station> candidates) {

		Station returnStation = null;

		int index = (int) (Math.random()*candidates.size());
		returnStation = candidates.get(index);

		return returnStation;
	}


	/**
	 * Set la fonction de score pour les gros camions
	 * @param text texte
	 */
	public void setScoreFunctionLargeCamionsString(String text) {
		this.scoreFunctionLargeCamionsString = text;

	}

	/**
	 * Set la fonction de score pour les petits camions
	 * @param text texte
	 */
	public void setScoreFunctionSmallCamionsString(String text) {
		this.scoreFunctionSmallCamionsString = text;

	}

	/**
	 * Ajuste le plan des pelles. Pour l'instant, ne change rien.
	 */
	public void updatePlan() {
		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			Pelle p = mine.getPelles().get(i);
			p.setPlan(p.getPlanNbTonnesParHeure());
		}
	}

}