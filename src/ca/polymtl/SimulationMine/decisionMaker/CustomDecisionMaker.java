package ca.polymtl.SimulationMine.decisionMaker;

import java.util.ArrayList;

import bsh.EvalError;
import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.MineSimulator.Station;
import ca.polymtl.SimulationMine.MineSimulator.Sterile;

public class CustomDecisionMaker extends DecisionMaker {
 


	public CustomDecisionMaker(Mine mine, Config config) {
		super(mine, config);
	}


	@Override
	/*
	 * Calcule le coût d'assignation d'un camion à une pelle dans le problème d'affectation
	 * 
	 * Input : 
	 * 		Camion : Camion à assigner
	 * 		Pelle  : Pelle évaluée
	 * 
	 * Output : Coût d'affectation, sous la forme d'un double. Une faible coût est meilleur.
	 * 
	 */
	protected double calculeOptimalAssignCost(Camion camion, Pelle pelle) {

		//Fonction par défaut
		// Commenter pour utiliser votre propre fonction de score
		//
		return super.calculeOptimalAssignCost(camion, pelle);

		//Votre propre fonction de score!
		//Décommenter pour utiliser! Vous ne devriez pas le faire, car cela ne fait pas partie du TP!
		//
		//return computeCustomAssignCost(camion, pelle);
	}

	//Votre propre fonction de score, en java
	//
	protected Pelle customSelectPelleForCamion(Camion camion) {


		//variable contenant le score d'affecter le camion à la pelle
		double score = 0;

		// La mine
		Mine mine = this.mine;
		
		// Liste des camions
		ArrayList<Camion> camions = mine.getCamions();
		
		// Liste des pelles
		ArrayList<Pelle> pelles = mine.getPelles();
		
		
		double totalFer = 0;
		double totalSoufre = 0;
		double totalMineraiConc = 0;
		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
			//Concentrateur
			Concentrateur concentrateur = mine.getConcentrateurs().get(i);
			totalFer += concentrateur.getQuantityIron();
			totalSoufre+= concentrateur.getQuantitySulfur();
			totalMineraiConc+= concentrateur.getTotalQuantity();
		}
		
		
		
		//concentration de fer et soufre au concentrateur
		double concentrationFer = totalFer/totalMineraiConc*100;
		double concentrationSoufre = totalSoufre/totalMineraiConc*100;
		
		
		/* Lire la documentation pour voir comment accéder aux autres caractéristiques des pelles et des camions
		 * 
		 */
		
		/*
		 * Votre code ici!
		 */
		
		//Pour l'instant, ne retourne rien et cause une erreur!
		return null;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see ca.polymtl.SimulationMine.decisionMaker.DecisionMaker#computeDecisionScore(ca.polymtl.SimulationMine.MineSimulator.Camion, ca.polymtl.SimulationMine.MineSimulator.Pelle, java.lang.String)
	 * 
	 * Détermine le score associé à l'envoi d'un camion à une pelle, selon une fonction de score donnée
	 * 
	 *  Input : 
	 *  	camion              : Camion à assigner
	 *  	pelle               : Pelle auquel le camion serait assigné
	 *  	scoreFunctionString : String indiquant la fonction de score à utilisé
	 *  
	 *  Output : 
	 *  	score sous la forme d'un double. Un petit score est meilleur.
	 */
	protected Pelle selectPelleForCamion(Camion camion) {
		//Fonctionalité par défaut : Évalue selon ce qui est écrit dans le champs "fonction de score".
		// Commentez la ligne suivante pour créer votre propre fonction de score.
		//
		return super.selectPelleForCamion(camion);


		//Votre propre fonction!
		//Décommenter pour utiliser.
		//
		//return customSelectPelleForCamion(camion);
	}
	
	@Override
	/**
	 * Choisis la station de déchargement pour un camion présentement a la pelle indiquee
	 * @param camion
	 * @param pelle
	 * @return Station de déchargement
	 */
	protected Station selectReturnStation(Camion camion, Pelle pelle) {
		
		//Fonctionalité par défaut : Choisis aléatoirement parmis les candidats valides.
		//
		//return super.selectReturnStation(camion, pelle);


		//Votre propre fonction!
		//Décommenter pour utiliser.
		//
		return customSelectReturnStation(camion, pelle);

	}

	/**
	 * Choisis à quelle station doivent se rendre les camions pour se faire décharger.
	 * @param camion
	 * @param pelle pelle ou se trouve presentement le camion.
	 * @return Station où doit se rendre le camion
	 */
	private Station customSelectReturnStation(Camion camion, Pelle pelle) {
		
		//station à retourner
		Station returnStation = null;
		
		//-------------------------------------------
		// Trouve les candidats valides 
		// (steriles ou concentrateurs selon la pelle)
		//-------------------------------------------
		
		ArrayList<Station> candidateStations = new ArrayList<Station>();
		
		
		//si le camion est remplis de sterile, choisis parmis les steriles
		//
		if(pelle.getRockType().isSterile()) {
			for(Sterile s : mine.getSteriles()) {
				candidateStations.add(s);
			}
		}
		//sinon, choisis parmis les concentrateurs
		//
		else {
			for(Concentrateur c : mine.getConcentrateurs()) {
				candidateStations.add(c);
			}
		}
		
		
		/*
		 * Votre code ici! 
		 * Vous devez choisir une station parmis les candidats.
		 */
		returnStation = candidateStations.get(0);
		
		return returnStation;
	}


	@Override
	/**
	 * Ajuste le plan des pelles. Pour l'instant, ne change rien.
	 */
	public void updatePlan() {
		
		//Par défaut : ne change pas le plan.
		//Commenter la ligne suivante pour implémenter votre propre fonction d'ajustement du plan.
		super.updatePlan();
		
		//Votre fonction ici. Vous pouvez changer le plan de la pelle p pelle en effectuant : 
		// p.setPlan(nb_camions_par_heure)
		
		//Pour savoir si une pelle est en panne, effectuez : 
		// if(p.getState() == Pelle.STATION_STATE_PANNE)
		
		//Pour plus de détails, lire la documentation
		
		//Exemple : 
		/*
		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			Pelle p = mine.getPelles().get(i);
			if(p.getState() == Pelle.STATION_STATE_PANNE){
				p.setPlan(0);
			}
			else{
				p.setPlan(p.getPlanNbCamionsParHeure()); //conserve le plan actuel
			}
		}
		*/
		
	}

}
