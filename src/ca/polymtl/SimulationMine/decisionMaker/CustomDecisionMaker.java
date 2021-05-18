package ca.polymtl.SimulationMine.decisionMaker;

import java.util.ArrayList;

import bsh.EvalError;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.MineSimulator.Station;

public class CustomDecisionMaker extends DecisionMaker {
 
	/*
	 * Constantes pouvant �tre utilis�es lors du calcul de la fonction de score
	 * Note : Ces valeurs changent � chaque appel de la fonction de score.
	 * Note : Je sais que ce n'est pas une bonne pratique, mais l'autre option �tait de commencer la 
	 * 			fonction computeCustomDecisionScore avec 30 lignes de d�finition de variables...
	 */
	// Nombre al�atoire entre 0 et 1
	private double random;
	// Score maximum
	private double max;
	// Vitesse moyenne du camion en m/s
	private double vitesse_moyenne_camion;
	// Temps moyen de remplissage d'un camion par la pelle
	private double temps_moyen_remplissage;
	// Indique 1 si la pelle est pr�sentement occup�e, 0 sinon
	private int pelleOccupee;
	// Indique le nombre de camions dans la file d'attente de la pelle (sans compter le camion en remplissage
	private int nbCamionsEnAttente;
	// Indique le nombre de camions � la pelle (en attente + en remplissage)
	private int nbCamionsALaPelle;
	//distance entre le camion et la pelle.
	private double distanceEntreCamionEtPelle;
	//temps esp�r� pour le parcours entre le camion et la pelle
	private double tempsDeParcoursEspere;
	//nombre de camions pr�sentement en route pour la pelle
	private int nbCamionsEnRoutePourLaPelle;
	//Temps esp�r� avant que la pelle n'ait fini de remplir les camions pr�sentement dans la file d'attente 
	// (sans compter les camions pr�sentement en route)
	private double tempsRestantAvantFinPelle;
	// Si le camion est affect� � la pelle, temps esp�r� avant le d�but du remplissage du camion.
	private double tempsEspereAvantDebutRemplissage;
	// Si le camion est affect� � la pelle, temps d'attente esp�r� du camion
	private double attenteEspereeCamion;
	// Si le camion est affect� � la pelle, temps d'attente esp�r� de la pelle
	private double attenteEspereePelle;

	public CustomDecisionMaker(Mine mine) {
		super(mine);
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
		
		//Concentrateur
		Concentrateur concentrateur = mine.getConcentrateur();
		
		//Sterile
		Station sterile = mine.getSterile();
		
		
		//concentration de fer et soufre au concentrateur
		double concentrationFer = concentrateur.getPercentIron();
		double concentrationSoufre = concentrateur.getPercentSulfur();
		
		//distance entre le camion et la pelle (m)
		//si le camion est en route pour un autre objectif, compte la distance entre camion et objectif + distance entre objectif et pelle

		double distanceEntreCamionEtPelle = calculeDistanceEntreCamionEtPelle(camion, pelle);

		//temps espere vers la pelle (s)
		double tempsDeParcoursEspere = calculeTempsParcoursMoyen(distanceEntreCamionEtPelle);


		//temps espere avant le debut du remplissage
		//
		double tempsEspereAvantDebutRemplissage = calculeTempsEspereAvantRemplissage(camion, pelle);

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
		//si n�gatif, remet a zero
		//if(attenteEspereePelle < 0){
			//attenteEspereePelle = 0; 
		//}		

		//System.out.println("attente esperee pelle "+attenteEspereePelle+" vs cible attente pelle "+pelle.cibleAttentePelleSeconds());
		//System.out.println("attente esperee camion "+attenteEspereeCamion);
		//convertis en secondes pour �viter instabilit�s
		//
		double penaliteQuadAttentePelle = super.calculePenaliteQuadAttentePelle(attenteEspereePelle/3600., pelle.cibleAttentePelleSeconds()/3600.);

		double penaliteQuadAttenteCamion = super.calculePenaliteQuadAttenteCamion(attenteEspereeCamion/3600.,0);// pelle.cibleAttenteCamionSeconds()/3600.);

		//System.out.println("penalite attente pelle "+penaliteQuadAttentePelle);
		//System.out.println("penalite attente camion "+penaliteQuadAttenteCamion);
		
		//System.out.println("total "+(penaliteQuadAttentePelle+penaliteQuadAttenteCamion));
		
		//System.out.println("");
		
		
		//System.out.println("attente esperee pelle "+attenteEspereePelle);
		//System.out.println("cible attente camion "+pelle.cibleAttenteCamionSeconds());
		//System.out.println("attente esperee camion "+attenteEspereeCamion);
		//System.out.println("score camion "+penaliteQuadAttenteCamion);
		//System.out.println("score pelle "+penaliteQuadAttentePelle);
		
		return penaliteQuadAttentePelle+penaliteQuadAttenteCamion;
		
		/* Lire la documentation pour voir comment acc�der aux autres caract�ristiques des pelles et des camions
		 * 
		 */
		
		/*
		 * Votre code ici!
		 */
		
		//return score;
	}

	private void computeUsefulConstants(Camion camion, Pelle pelle) {
		
		ArrayList<Camion> camions = mine.getCamions();
		//nombre al�atoire
		random = SimulationMine.random.nextDouble();

		//score max
		max = Double.MAX_VALUE;

		vitesse_moyenne_camion = Camion.VITESSE_MOYENNE;
		temps_moyen_remplissage = Camion.CHARGE_MAX/Pelle.AVERAGE_CHARGE_SPEED;

		//indique 1 si la pelle est occupee
		pelleOccupee = 0;
		if(pelle.getCamionEnRemplissage() != null) pelleOccupee = 1;

		//nombre de camions en attente
		nbCamionsEnAttente = pelle.getCamionsEnAttente().size();

		//nombre de camions a la pelle (attente + remplissage)
		nbCamionsALaPelle = 0;
		if(pelle.getCamionEnRemplissage()!= null) {
			nbCamionsALaPelle = nbCamionsEnAttente+1;
		}

		//distance entre le camion et la pelle (m)
		//si le camion est en route pour un autre objectif, compte la distance entre camion et objectif + distance entre objectif et pelle

		distanceEntreCamionEtPelle = calculeDistanceEntreCamionEtPelle(camion, pelle);




		//temps espere vers la pelle (s)
		tempsDeParcoursEspere = calculeTempsParcoursMoyen(distanceEntreCamionEtPelle);


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
		Camion camionEnRemplissage = pelle.getCamionEnRemplissage();
		if(camionEnRemplissage != null) {

			//temps de remplissage restant = charge restante / vitesse moyenne charge
			double esperanceTempsRemplissageRestant = camionEnRemplissage.getChargeRemaining()/Pelle.AVERAGE_CHARGE_SPEED;

			tempsRestantAvantFinPelle = esperanceTempsRemplissageRestant + pelle.getCamionsEnAttente().size()*Camion.CHARGE_MAX/Pelle.AVERAGE_CHARGE_SPEED;
		}


		//temps espere avant le debut du remplissage
		//
		tempsEspereAvantDebutRemplissage = calculeTempsEspereAvantRemplissage(camion, pelle);

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

	private double computeCustomAssignCost(Camion camion, Pelle pelle) {
		// TODO Auto-generated method stub
		return 0;
	}

}
