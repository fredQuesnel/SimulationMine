package ca.polymtl.SimulationMine.MineSimulator;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import ca.polymtl.SimulationMine.decisionMaker.DecisionMaker;

public class Pelle extends Station{
	/*
	 * constantes
	 */
	//public final static double AVERAGE_CHARGE_TIME=300;//5 minutes
	//public final static double ECART_TYPE_CHARGE_TIME=60;//1 min

	//vitesse en pourcentage de chargement par seconde
	/** Vitesse moyenne de remplissage (en tonnes secondes)*/
	public final static double AVERAGE_CHARGE_SPEED = 1./6.;
	/** Écart type sur la vitesse de remplissage*/
	public final static double ECART_TYPE_CHARGE_SPEED = 1./30.;

	


	/*
	 * Champs
	 */
	//-----------------------
	//constantes
	//-----------------------
	//station de retour des camions
	//
	//private Station returnStation;

	//type de roches
	//
	private RockType rockType;


	//plan de travail
	private double cibleCamionsParHeure;

	//plan de travail par defaut
	private final double defaultCibleCamionsParHeure;


	
	
	

	

	
	//	private double timeRemainingInTurn;


	/*
	 * Constructeur
	 */
	public Pelle(int i, int j, String id, double cibleCamionsParHeure) {
		super(i,j, id);
		
		defaultCibleCamionsParHeure = cibleCamionsParHeure;
		this.cibleCamionsParHeure = cibleCamionsParHeure;
		this.isDecharge = false;
	}

	@Override
	public double averageTraitementSpeed() {
		return AVERAGE_CHARGE_SPEED;
	}

	/**
	 * 
	 * @return Cible d'attente des camions à la pelle, selon le plan
	 */
	public double cibleAttenteCamionSeconds(){
		//TODO
		//Formule : attente = lambda/(mu (mu-lembda))
		//
		// Avec : 
		// lambda = taux arrivée des camions (camions/h)
		// mu    = taux de service de la pelle (camions/h)

		double lambda = this.cibleCamionsParHeure;
		//en nb camions par heure
		double mu = Pelle.AVERAGE_CHARGE_SPEED/100*3600;

		//attente en heures
		double attente = lambda/(mu*(mu-lambda));

		//attente en secondes
		return attente*3600;
	}
	

	/**
	 * 
	 * @return La cible d'attente de la pelle selon le plan
	 */
	public double cibleAttentePelleSeconds(){
		double averageChargeTimeSeconds = 100./this.currentChargeSpeed;

		double tempsTravailParHeureEnSecondes = averageChargeTimeSeconds*cibleCamionsParHeure;

		//si travaille plus d'une heure par heure, temps cible d'attente nul.
		if(tempsTravailParHeureEnSecondes >=3600){
			return 0;
		}
		else{
			return (3600-tempsTravailParHeureEnSecondes)/cibleCamionsParHeure;
		}
	}

	/**
	 * 
	 * @return Selon le plan, nombre de camions par heure se rendant à la pelle.
	 */
	public double getPlanNbCamionsParHeure() {

		return this.cibleCamionsParHeure;
	}

	/**
	 * 
	 * @return Type de roche à la pelle
	 */
	public RockType getRockType() {
		return rockType;
	}

	//TODO rendre indépendant du nombre de pas de simulation.
	protected void computeNewChargeSpeed() {
		double lambda = 0.75;
		double speedAdjust = SimulationMine.random.nextGaussian()*Pelle.ECART_TYPE_CHARGE_SPEED+Pelle.AVERAGE_CHARGE_SPEED;

		this.currentChargeSpeed = lambda*this.currentChargeSpeed + (1-lambda)*speedAdjust;

	}

	/*
	 * (non-Javadoc)
	 * @see ca.polymtl.SimulationMine.MineSimulator.Station#setCamionOnArrival(ca.polymtl.SimulationMine.MineSimulator.Camion)
	 * 
	 * Determine ce qu'on fait avec un camion qui vient juste d'arriver : 
	 *  - Si pas de camion en remplissage, on le met en remplissage
	 *  - Sinon, on met le camion en attente
	 */
	protected void setCamionOnArrival(Camion camion) {
		
		//met comme camion à traiter ou dans la file d'attente.
		super.setCamionOnArrival(camion);
		
		//type de roche du camion
		//
		camion.setRockType(this.rockType);

		
	}

	protected void setPlan(double newValue){
		System.out.println("je veux maintenant "+newValue);

		// lambda = taux arrivée des camions (camions/h)
		// mu    = taux de service de la pelle (camions/h)

		double lambda = newValue;
		//en nb camions par heure
		double mu = this.AVERAGE_CHARGE_SPEED/100*3600;

		if(lambda > mu){
			JOptionPane.showMessageDialog(null, "Taux d'arrivée trop grand. Vous devez choisir une valeur inférieure à "+mu);
		}
		else{
			this.cibleCamionsParHeure = newValue;
		}

	}

	protected void setRockType(double percentOre, double percentSulfur) {
		RockType rt = new RockType(percentOre, percentSulfur);

		this.rockType = rt;

	}

	@Override
	protected void updateQteTraite(double quantite, RockType rockType) {
		// Ne fait rien.
		
	}

	



}