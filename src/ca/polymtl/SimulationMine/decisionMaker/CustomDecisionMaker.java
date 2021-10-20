package ca.polymtl.SimulationMine.decisionMaker;

import java.util.ArrayList;

import bsh.EvalError;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;

public class CustomDecisionMaker extends DecisionMaker {
 
	/*
	 * Constantes pouvant �tre utilis�es lors du calcul de la fonction de score
	 * Note : Ces valeurs changent � chaque appel de la fonction de score.
	 * Note : Je sais que ce n'est pas une bonne pratique, mais l'autre option �tait de commencer la 
	 * 			fonction computeCustomDecisionScore avec 30 lignes de d�finition de variables...
	 */
	// Nombre al�atoire entre 0 et 1
	@SuppressWarnings("unused")
	private double random;
	// Score maximum
	@SuppressWarnings("unused")
	private double max;
	// Vitesse moyenne du camion en m/s
	@SuppressWarnings("unused")
	private double vitesse_moyenne_camion;
	// Temps moyen de remplissage d'un camion par la pelle
	@SuppressWarnings("unused")
	private double temps_moyen_remplissage;
	// Indique 1 si la pelle est pr�sentement occup�e, 0 sinon
	@SuppressWarnings("unused")
	private int pelleOccupee;
	// Indique le nombre de camions dans la file d'attente de la pelle (sans compter le camion en remplissage
	private int nbCamionsEnAttente;
	// Indique le nombre de camions � la pelle (en attente + en remplissage)
	@SuppressWarnings("unused")
	private int nbCamionsALaPelle;
	//distance entre le camion et la pelle.
	private double distanceEntreCamionEtPelle;
	//temps esp�r� pour le parcours entre le camion et la pelle
	private double tempsDeParcoursEspere;
	//nombre de camions pr�sentement en route pour la pelle
	@SuppressWarnings("unused")
	private int nbCamionsEnRoutePourLaPelle;
	//Temps esp�r� avant que la pelle n'ait fini de remplir les camions pr�sentement dans la file d'attente 
	// (sans compter les camions pr�sentement en route)
	@SuppressWarnings("unused")
	private double tempsRestantAvantFinPelle;
	// Si le camion est affect� � la pelle, temps esp�r� avant le d�but du remplissage du camion.
	private double tempsEspereAvantDebutRemplissage;
	// Si le camion est affect� � la pelle, temps d'attente esp�r� du camion
	private double attenteEspereeCamion;
	// Si le camion est affect� � la pelle, temps d'attente esp�r� de la pelle
	@SuppressWarnings("unused")
	private double attenteEspereePelle;

	public CustomDecisionMaker(Mine mine) {
		super(mine);
	}


	private void computeUsefulConstants(Camion camion, Pelle pelle) {
		
		ArrayList<Camion> camions = mine.getCamions();
		//nombre al�atoire
		random = SimulationMine.random.nextDouble();

		//score max
		max = Double.MAX_VALUE;

		vitesse_moyenne_camion = camion.getAvgSpeed();
		temps_moyen_remplissage = camion.getChargeMax()/Pelle.AVERAGE_CHARGE_SPEED;

		//indique 1 si la pelle est occupee
		pelleOccupee = 0;
		if(pelle.getCamionEnTraitement() != null) pelleOccupee = 1;

		//nombre de camions en attente
		nbCamionsEnAttente = pelle.getCamionsEnAttente().size();

		//nombre de camions a la pelle (attente + remplissage)
		nbCamionsALaPelle = 0;
		if(pelle.getCamionEnTraitement()!= null) {
			nbCamionsALaPelle = nbCamionsEnAttente+1;
		}

		//distance entre le camion et la pelle (m)
		//si le camion est en route pour un autre objectif, compte la distance entre camion et objectif + distance entre objectif et pelle

		distanceEntreCamionEtPelle = calculeDistanceEntreCamionEtStation(camion, pelle);




		//temps espere vers la pelle (s)
		tempsDeParcoursEspere = distanceEntreCamionEtPelle/( camion.getAvgSpeed()*mine.getMeteoFactor());


		//nombre de camions presentement en route pour la pelle
		nbCamionsEnRoutePourLaPelle = 0;
		for(int i = 0 ; i < camions.size(); i++) {
			if(camions.get(i).getState() == Camion.ETAT_EN_ROUTE && camions.get(i).getObjective() == pelle) {
				nbCamionsEnRoutePourLaPelle++;
			}
		}

		//temps restant avant que la pelle n'aie plus de travail (en considerant seulement les camions en attente)
		//
		tempsRestantAvantFinPelle = calculeTempsRestantAvantFinPelle(pelle);
		Camion camionEnRemplissage = pelle.getCamionEnTraitement();
		if(camionEnRemplissage != null) {

			//temps de remplissage restant = charge restante / vitesse moyenne charge
			double chargeRemaining = camionEnRemplissage.getChargeMax()-camionEnRemplissage.getCharge(); 
			double esperanceTempsRemplissageRestant = chargeRemaining/Pelle.AVERAGE_CHARGE_SPEED;

			tempsRestantAvantFinPelle = esperanceTempsRemplissageRestant + pelle.getCamionsEnAttente().size()*camion.getChargeMax()/Pelle.AVERAGE_CHARGE_SPEED;
		}


		//temps espere avant le debut du remplissage
		//
		tempsEspereAvantDebutRemplissage = calculeTempsEspereAvantTraitement(camion, pelle);

		//temps espere d'attente de la pelle
		//


		//attente esperee du camion
		//
		attenteEspereeCamion = tempsEspereAvantDebutRemplissage - tempsDeParcoursEspere;
		if(attenteEspereeCamion < 0) {
			attenteEspereeCamion = 0;
		}

		//attente esperee de la pelle (compte seulement le temps ou la pelle attend pour le camion courant, pas pour ceux deja en route!)
		//peut etre n�gatif, signifiant que le camion ira en attente.
		//
		attenteEspereePelle = tempsDeParcoursEspere - tempsEspereAvantDebutRemplissage;
	}

	@Override
	/*
	 * Calcule le co�t d'assignation d'un camion � une pelle dans le probl�me d'affectation
	 * 
	 * Input : 
	 * 		Camion : Camion � assigner
	 * 		Pelle  : Pelle �valu�e
	 * 
	 * Output : Co�t d'affectation, sous la forme d'un double. Une faible co�t est meilleur.
	 * 
	 */
	protected double calculeOptimalAssignCost(Camion camion, Pelle pelle) {

		//Fonction par d�faut
		// Commenter pour utiliser votre propre fonction de score
		//
		return super.calculeOptimalAssignCost(camion, pelle);

		//Votre propre fonction de score!
		//D�commenter pour utiliser! Vous ne devriez pas le faire, car cela ne fait pas partie du TP!
		//
		//return computeCustomAssignCost(camion, pelle);
	}

	//Votre propre fonction de score, en java
	//
	protected double computeCustomDecisionScore(Camion camion, Pelle pelle) {

		//calcule des constantes utiles. Vous pouvez voir leurs d�finitions au d�but du fichier.
		computeUsefulConstants(camion, pelle);

		//variable contenant le score d'affecter le camion � la pelle
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
		
		//distance entre le camion et la pelle (m)
		//si le camion est en route pour un autre objectif, compte la distance entre camion et objectif + distance entre objectif et pelle

		double distanceEntreCamionEtPelle = calculeDistanceEntreCamionEtStation(camion, pelle);

		//temps espere vers la pelle (s)
		double tempsDeParcoursEspere = distanceEntreCamionEtPelle/(camion.getAvgSpeed()*mine.getMeteoFactor());


		//temps espere avant le debut du remplissage
		//
		double tempsEspereAvantDebutRemplissage = calculeTempsEspereAvantTraitement(camion, pelle);

		//temps espere d'attente de la pelle
		//

		//temps espere avant que la pelle termine sa file d'attente actuelle
		double tempsRestantAvantFinPelle = calculeTempsRestantAvantFinPelle(pelle);
		
		//attente esperee du camion
		//
		double attenteEspereeCamion = tempsEspereAvantDebutRemplissage - tempsDeParcoursEspere;
		if(attenteEspereeCamion < 0) {
			attenteEspereeCamion = 0;
		}
		
		

		//attente esperee de la pelle (compte seulement le temps ou la pelle attend pour le camion courant, pas pour ceux deja en route!)
		//peut etre n�gatif, signifiant que le camion ira en attente.
		//
		//double attenteEspereePelle = tempsDeParcoursEspere - tempsEspereAvantDebutRemplissage;
		double attenteEspereePelle = tempsDeParcoursEspere - tempsRestantAvantFinPelle +pelle.getCurrentWaitingPeriod();
		if(attenteEspereePelle > pelle.cibleAttentePelleSeconds()){
			attenteEspereePelle = pelle.cibleAttentePelleSeconds();
		}
		

		//System.out.println("attente esperee pelle "+attenteEspereePelle+" vs cible attente pelle "+pelle.cibleAttentePelleSeconds());
		//System.out.println("attente esperee camion "+attenteEspereeCamion);
		//convertis en secondes pour �viter instabilit�s
		//
		double penaliteQuadAttentePelle = super.calculePenaliteQuadAttentePelle(attenteEspereePelle/3600., pelle.cibleAttentePelleSeconds()/3600.);

		double penaliteQuadAttenteCamion = super.calculePenaliteQuadAttenteCamion(attenteEspereeCamion/3600.,0);// pelle.cibleAttenteCamionSeconds()/3600.);

		
		/* Lire la documentation pour voir comment acc�der aux autres caract�ristiques des pelles et des camions
		 * 
		 */
		
		/*
		 * Votre code ici!
		 */
		
		return score;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see ca.polymtl.SimulationMine.decisionMaker.DecisionMaker#computeDecisionScore(ca.polymtl.SimulationMine.MineSimulator.Camion, ca.polymtl.SimulationMine.MineSimulator.Pelle, java.lang.String)
	 * 
	 * D�termine le score associ� � l'envoi d'un camion � une pelle, selon une fonction de score donn�e
	 * 
	 *  Input : 
	 *  	camion              : Camion � assigner
	 *  	pelle               : Pelle auquel le camion serait assign�
	 *  	scoreFunctionString : String indiquant la fonction de score � utilis�
	 *  
	 *  Output : 
	 *  	score sous la forme d'un double. Un petit score est meilleur.
	 */
	protected double computeDecisionScore(Camion camion, Pelle pelle, String scoreFunctionString) throws EvalError {
		//Fonctionalit� par d�faut : �value le score � l'aide des param�tres pr�programm�s
		//Commenter pour impl�menter votre propre fonction de score.
		//
		return super.computeDecisionScore(camion, pelle, scoreFunctionString);


		//Votre propre fonction de score!
		//D�commenter pour utiliser!
		//
		//return computeCustomDecisionScore(camion, pelle);
	}

}
