package ca.polymtl.SimulationMine.MineSimulator;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class Pelle extends Station{
	/*
	 * constantes
	 */
	//public final static double AVERAGE_CHARGE_TIME=300;//5 minutes
	//public final static double ECART_TYPE_CHARGE_TIME=60;//1 min

	//vitesse en pourcentage de chargement par seconde
	/** Vitesse moyenne de remplissage (en %/ secondes)*/
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
	private Station returnStation;

	//type de roches
	//
	private RockType rockType;


	//plan de travail
	private double cibleCamionsParHeure;

	//plan de travail par defaut
	private final double defaultCibleCamionsParHeure;


	//ETAT de la pelle 
	//
	private Camion camionEnRemplissage;
	private ArrayList<Camion> camionsEnAttente;
	private int state;
	
	//Temps passé en attente depuis le dernier remplissage
	private double currentWaitingPeriod;
	

	private double currentChargeSpeed;

	//statistiques
	//
	private double waitingTime;
	private int nbCamionsRemplis;
	//	private double timeRemainingInTurn;


	//infos sur l'iteration en cours
	//
	private double iterTotalTime;
	private double iterCurrentTime;
	private boolean iterFinished;
	/*
	 * Constructeur
	 */
	public Pelle(int i, int j, String id, double cibleCamionsParHeure) {

		super(i,j, id);
		this.camionsEnAttente = new ArrayList<Camion>();
		camionEnRemplissage = null;
		waitingTime = 0;
		nbCamionsRemplis = 0;
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

		//type de roche du camion
		//
		camion.setRockType(this.rockType);

		//si aucun camion en remplissage, met le camion en remplissage
		//
		if(camionEnRemplissage == null) {
			setCamionEnRemplissage(camion);
			//fait attendre la pelle jusqu'a ce que le camion arrive
			//
			double waitingTime = camion.getIterCurrentTime()- this.iterCurrentTime;
			if(waitingTime < 0) {
				waitingTime = 0;
			}
			this.attend(waitingTime);

		}
		//sinon, ajoute le camion a la file d'attente
		else {
			setCamionEnAttente(camion);
		}
	}

	/*
	 * Met un camion dans la file d'attente
	 */
	private void setCamionEnAttente(Camion camion) {
		camionsEnAttente.add(camion);
		camion.setAttenteState();
	}

	/*
	 * Met un camion en remplissage
	 */
	private void setCamionEnRemplissage(Camion camion) {
		if(this.camionEnRemplissage != null){
			throw new IllegalStateException("Je veux mettre un camion en remplissage alors qu'il y en a deja un!");
		}
		this.camionEnRemplissage = camion;
		camion.setEnChargeState();
		this.nbCamionsRemplis++;
	}

	/*
	 * Retourne une vitesse de charge aleatoire, basé sur une distribution gaussienne de moyenne et ecart type determine
	 */
	/*private double getRandomChargeSpeed() {

		return SimulationMine.random.nextGaussian()*Pelle.ECART_TYPE_CHARGE_SPEED+Pelle.AVERAGE_CHARGE_SPEED;
	}
	 */
	/**
	 * 
	 * @return Le Camion présentement en remplissage, ou null si aucun camion n'est présentement en remplissage à la station.
	 * 
	 */
	public Camion getCamionEnRemplissage() {
		return camionEnRemplissage;
	}

	/**
	 * 
	 * @return La station où sont envoyés les camions après le remplissage
	 */
	public Station getReturnStation() {
		return returnStation;
	}

	/*
	 * set la station de retour de la pelle
	 */
	protected void setReturnStation(Station returnStation) {
		this.returnStation = returnStation;
	}

	/**
	 * 
	 * @return La liste des camions en attente de remplissage (excluant le camion présentement en remplissage, si il y a lieu)
	 */
	public ArrayList<Camion> getCamionsEnAttente() {
		return camionsEnAttente;
	}


	/**
	 * 
	 * @return Le temps total passé par la pelle en attente.
	 */
	public double getWaitTime() {
		return this.waitingTime;
	}

	//reset les stats de la pelle
	//
	protected void resetStats() {
		this.waitingTime = 0;
		this.nbCamionsRemplis = 0;

	}

	/**
	 * 
	 * @return Nombre total de camions remplis par la pelle
	 */
	public int getNbCamionsRemplis() {
		return nbCamionsRemplis;
	}


	/*
	 * active la pelle pour remplir les camions en remplissage et/ou en attente
	 */
	protected void activate() {
		//Si il y a un camion en remplissage, le remplis
		//
		if(camionEnRemplissage == null) {
			throw new IllegalStateException("la pelle ne peut pas etre activee, aucun camion en remplissage");
		}
		//met en mode travail (si ce n'etait pas deja le cas
		//
		setWorkingMode();

		//temps restant dans le tour
		//
		double pelleRemainingTime = this.getRemainingTimeInTurn();

		//determine la vitesse de remplissage
		//
		double chargeSpeed = this.currentChargeSpeed;

		//determine le temps de charge. Il s'agit du max entre
		//	1) temps de charge max du camion
		//	2) temps de charge max de la pelle
		//
		double chargeTime = camionEnRemplissage.getMaxChargeTimeInTurn(chargeSpeed);
		if(chargeTime > pelleRemainingTime) {
			chargeTime = pelleRemainingTime;

		}

		//remplis le camion pour le temps prevu
		//
		camionEnRemplissage.remplis(chargeTime, chargeSpeed);

		//tous les camions de la file d'attente attendent 
		for(int i = 0 ; i < camionsEnAttente.size(); i++) {
			camionsEnAttente.get(i).attend(chargeTime);
		}

		//determine si camion remplis
		boolean camionIsFull = camionEnRemplissage.isFull();

		//incremente le temps du tour
		//
		this.iterCurrentTime+= chargeTime;

		//determine si le tour est termine
		//
		if(iterCurrentTime >= this.iterTotalTime) {
			this.iterFinished = true;
		}

		//si le camion est remplis :
		//  -renvoie le camion remplis a la bonne station
		//  -met le premier camion de la file en remplissage (si un tel camion existe)
		//
		if(camionIsFull) {

			//nouvelle dest. du camion
			camionEnRemplissage.setObjective(returnStation);

			//met un nouveau camion sous la pelle (si il y en a au moins un)
			//
			camionEnRemplissage = null;
			if(camionsEnAttente.size()!=0) {
				Camion c = camionsEnAttente.get(0);
				camionsEnAttente.remove(c);
				setCamionEnRemplissage(c);
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

	/*
	 * retourne le temps restant dans le tour 
	 */
	protected double getRemainingTimeInTurn() {
		return this.iterTotalTime-this.iterCurrentTime;
	}

	/*
	 * Atend jusqu'à la fin du tour
	 */
	protected void waitForRemainingTime() {
		this.attend(this.getRemainingTimeInTurn());
		this.iterFinished = true;
	}

	/*
	 * Fais attendre la pelle un temps donné, ou le reste du tour 
	 */
	private void attend(double time) {
		//met en mode attente au besoin
		//
		setIdleMode();

		//si le temps d'attente depasse le temps de l'iteration
		//fais attendre le temps de l'iteration
		//
		if(time >= this.getRemainingTimeInTurn()){
			this.currentWaitingPeriod+=this.getRemainingTimeInTurn();
			this.waitingTime += this.getRemainingTimeInTurn();
			this.iterCurrentTime = this.iterTotalTime;
			this.iterFinished = true;
		}
		else{
			this.currentWaitingPeriod+=time;
			this.waitingTime+= time;
			this.iterCurrentTime+= time;
		}
	}

	/*
	 * prepare la pelle pour le debut d'un tour
	 */
	protected void setBeginStep(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterTotalTime = stepSize;
		this.iterFinished = false;

		computeNewChargeSpeed();

	}

	//TODO rendre indépendant du nombre de pas de simulation.
	private void computeNewChargeSpeed() {
		double lambda = 0.75;
		double speedAdjust = SimulationMine.random.nextGaussian()*Pelle.ECART_TYPE_CHARGE_SPEED+Pelle.AVERAGE_CHARGE_SPEED;

		this.currentChargeSpeed = lambda*this.currentChargeSpeed + (1-lambda)*speedAdjust;

	}

	protected void makeAllCamionWaitUntilEndIter() {
		if(this.camionEnRemplissage!= null) {
			camionEnRemplissage.waitUntilEndIter();
		}
		for(int i = 0 ; i < this.camionsEnAttente.size(); i++) {
			camionsEnAttente.get(i).waitUntilEndIter();
		}
	}

	protected boolean iterFinished() {
		return this.iterFinished;
	}

	/** 
	 * 
	 * @return Temps moyen d'attente entre le remplissage de deux camions.
	 */
	public double getAverageWaitTimeSeconds(){

		if(this.nbCamionsRemplis==0) {
			return 0;
		}
		return this.waitingTime/this.nbCamionsRemplis;
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