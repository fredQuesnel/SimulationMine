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

	/** État indiquant une pelle inactive*/
	public final static int PELLE_STATE_IDLE = 1;
	/** État indiquant une pelle active*/
	public final static int PELLE_STATE_WORKING = 2;


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


	private int state;
	
	

	private double currentChargeSpeed;

	
	//	private double timeRemainingInTurn;


	/*
	 * Constructeur
	 */
	public Pelle(int i, int j, String id, double cibleCamionsParHeure) {
		super(i,j, id);
		this.state = this.PELLE_STATE_IDLE;
		defaultCibleCamionsParHeure = cibleCamionsParHeure;
		this.cibleCamionsParHeure = cibleCamionsParHeure;
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

	/*
	 * Met un camion en remplissage
	 */
	protected void setCamionEnTraitement(Camion camion) {
		super.setCamionEnTraitement(camion);
		camion.setEnChargeState();
	}

	/*
	 * Retourne une vitesse de charge aleatoire, basé sur une distribution gaussienne de moyenne et ecart type determine
	 */
	/*private double getRandomChargeSpeed() {

		return SimulationMine.random.nextGaussian()*Pelle.ECART_TYPE_CHARGE_SPEED+Pelle.AVERAGE_CHARGE_SPEED;
	}
	 */
	

	/*
	 * active la pelle pour remplir les camions en remplissage et/ou en attente
	 */
	protected void activate() {
		//Si il y a un camion en remplissage, le remplis
		//
		if(camionEnTraitement == null) {
			throw new IllegalStateException("la pelle ne peut pas etre activee, aucun camion en remplissage");
		}
		//met en mode travail (si ce n'etait pas deja le cas)
		//
		setWorkingMode();

		//temps restant dans le tour
		//
		double pelleRemainingTime = this.getRemainingTimeInTurn();

		

		//determine le temps de charge. Il s'agit du max entre
		//	1) temps de charge pour remplir le camion
		//	2) temps restant au camion
		//	2) temps de charge max de la pelle
		//
		double chargeTime = (camionEnTraitement.getChargeMax() - camionEnTraitement.getCharge())/this.currentChargeSpeed;
		if(camionEnTraitement.getRemainingTimeInTurn() < chargeTime) {
			chargeTime = camionEnTraitement.getRemainingTimeInTurn();
		}
		
		if(pelleRemainingTime < chargeTime) {
			chargeTime = pelleRemainingTime;
		}

		double quantite = chargeTime*this.currentChargeSpeed;
		
		//remplis le camion pour le temps prevu
		//
		camionEnTraitement.remplis(quantite, chargeTime);

		//tous les camions de la file d'attente attendent 
		for(int i = 0 ; i < camionsEnAttente.size(); i++) {
			if(camionsEnAttente.get(i).getRemainingTimeInTurn()< chargeTime) {
				camionsEnAttente.get(i).waitUntilEndIter();
			}
			else {
				camionsEnAttente.get(i).attend(chargeTime);
			}
		}

		//determine si camion remplis
		boolean camionIsFull = camionEnTraitement.isFull();

		//incremente le temps du tour
		//
		this.iterCurrentTime+= chargeTime;

		//determine si le tour est termine
		//
		if(iterCurrentTime >= this.iterStepSize) {
			this.iterFinished = true;
		}

		//si le camion est remplis :
		//  -renvoie le camion remplis a la bonne station
		//  -met le premier camion de la file en remplissage (si un tel camion existe)
		//
		if(camionIsFull) {

			Station returnStation = DecisionMaker.selectReturnStation(camionEnTraitement, this);
			//nouvelle dest. du camion
			camionEnTraitement.setObjective(returnStation);

			//met un nouveau camion sous la pelle (si il y en a au moins un)
			//
			camionEnTraitement = null;
			if(camionsEnAttente.size()!=0) {
				Camion c = camionsEnAttente.get(0);
				camionsEnAttente.remove(c);
				setCamionEnTraitement(c);
			}
		}

	}

	//met la pelle en mode travail
	private void setWorkingMode() {
		this.state = Pelle.PELLE_STATE_WORKING;
		this.currentWaitingPeriod =0;
	}

	//met la pelle en mode attente
	//Si la pelle n'etait pas en mode attente, augmente le nombre de periodes d'attente
	private void setIdleMode() {
		this.state = Pelle.PELLE_STATE_IDLE;
	}

	
	protected void attend(double time) {
		//met en mode attente au besoin
		//
		setIdleMode();
		super.attend(time);
	}



	

	/*
	 * prepare la pelle pour le debut d'un tour
	 */
	protected void setBeginStep(double stepSize) {
		super.setBeginStep(stepSize);

		computeNewChargeSpeed();
	}

	//TODO rendre indépendant du nombre de pas de simulation.
	private void computeNewChargeSpeed() {
		double lambda = 0.75;
		double speedAdjust = SimulationMine.random.nextGaussian()*Pelle.ECART_TYPE_CHARGE_SPEED+Pelle.AVERAGE_CHARGE_SPEED;

		this.currentChargeSpeed = lambda*this.currentChargeSpeed + (1-lambda)*speedAdjust;

	}

	/**
	 * 
	 * @return Type de roche à la pelle
	 */
	public RockType getRockType() {
		return rockType;
	}

	protected void setRockType(double percentOre, double percentSulfur) {
		RockType rt = new RockType(percentOre, percentSulfur);

		this.rockType = rt;

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
		double mu = this.AVERAGE_CHARGE_SPEED/100*3600;

		//attente en heures
		double attente = lambda/(mu*(mu-lambda));

		//attente en secondes
		return attente*3600;
	}

	/**
	 * 
	 * @return Selon le plan, nombre de camions par heure se rendant à la pelle.
	 */
	public double getPlanNbCamionsParHeure() {

		return this.cibleCamionsParHeure;
	}

	public double getCurrentWaitingPeriod() {
		return currentWaitingPeriod;
	}



}