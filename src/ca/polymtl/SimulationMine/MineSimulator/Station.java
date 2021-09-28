package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

public abstract class Station {

	/** État indiquant une pelle inactive*/
	public final static int STATION_STATE_IDLE = 1;
	/** État indiquant une pelle active*/
	public final static int STATION_STATE_WORKING = 2;
	
	private Point2D.Double location;
	private String id;
	
	public boolean isDecharge; 
	
	
	protected ArrayList<Camion> camionsEnAttente;
	//ETAT de la pelle 
	//
	protected Camion camionEnTraitement;

	protected int nbCamionsTraites;

	//infos sur l'iteration en cours
	//
	protected double iterStepSize;
	protected double iterCurrentTime;
	protected boolean iterFinished;


	//statistiques
	//
	protected double waitingTime;

	//Temps passé en attente depuis le dernier remplissage
	protected double currentWaitingPeriod;
	protected double currentChargeSpeed;
	private int state;

	//constructeur
	protected Station(double i, double j, String id) {
		this.location = new Point2D.Double(i, j);
		this.id = id;
		this.state = this.STATION_STATE_IDLE;
		
		this.camionsEnAttente = new ArrayList<Camion>();
		camionEnTraitement = null;
		waitingTime = 0;
		nbCamionsTraites = 0;
	}

	public void addIterTime(double temps) {
		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("temps ajouté plus grand que temps restant : "+temps+" > "+this.getRemainingTimeInTurn());
		}
		this.iterCurrentTime+= temps;
		
		
	}
	

	public abstract double averageTraitementSpeed();

	/** 
	 * 
	 * @return Temps moyen d'attente entre le remplissage de deux camions.
	 */
	public double getAverageWaitTimeSeconds(){

		if(this.nbCamionsTraites==0) {
			return 0;
		}
		return this.waitingTime/this.nbCamionsTraites;
	}
	
	/**
	 * 
	 * @return Le Camion présentement en remplissage, ou null si aucun camion n'est présentement en remplissage à la station.
	 * 
	 */
	public Camion getCamionEnTraitement() {
		return camionEnTraitement;
	}


	/**
	 * 
	 * @return La liste des camions en attente de remplissage (excluant le camion présentement en remplissage, si il y a lieu)
	 */
	public ArrayList<Camion> getCamionsEnAttente() {
		return camionsEnAttente;
	}


	
	
	public double getCurrentWaitingPeriod() {
		if(this.state != Station.STATION_STATE_IDLE) {
			throw new IllegalStateException("Ne peut pas retourner la periode d'attente courante si la pelle n'est pas en attente.");
		}
		return currentWaitingPeriod;
	}
	
	
	/**
	 * 
	 * @return String contenant l'identifiant de la station.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retourne la coordonnée géographique de la station.
	 * @return Point2D.Double représentant la coordonnée géographique de la station.
	 */
	public Double getLocation() {

		return location;
	}
	/**
	 * 
	 * @return Nombre total de camions remplis par la pelle
	 */
	public int getNbCamionsTraites() {
		return nbCamionsTraites;
	}
	
	public int getState() {
		return this.state;
	}


	/**
	 * 
	 * @return Le temps total passé par la pelle en attente.
	 */
	public double getWaitTime() {
		return this.waitingTime;
	}
	
	/*
	 * Fais attendre la pelle un temps donné, ou le reste du tour 
	 */
	protected void attend(double time) {

		//si le temps d'attente depasse le temps de l'iteration
		//fais attendre le temps de l'iteration
		//
		if(time >= this.getRemainingTimeInTurn()+0.0001){
			throw new IllegalArgumentException("impossible d'attendre plus de temps qu'il n'en reste : "+time+" > "+this.getRemainingTimeInTurn());
		}	
		else{
			this.currentWaitingPeriod+=time;
			this.waitingTime+= time;
			this.iterCurrentTime+= time;
		}
	}

	/*
	 * retourne le temps restant dans le tour 
	 */
	protected double getRemainingTimeInTurn() {
		return this.iterStepSize-this.iterCurrentTime;
	}

	protected boolean iterFinished() {
		return this.iterFinished;
	}
	
	//reset les stats de la pelle
	//
	protected void resetStats() {
		this.waitingTime = 0;
		this.nbCamionsTraites = 0;

	}
	
	/*
	 * prepare la pelle pour le debut d'un tour
	 */
	protected void setBeginStep(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterStepSize = stepSize;
		this.iterFinished = false;
		computeNewChargeSpeed();


	}

	/*
	 * Met un camion dans la file d'attente
	 */
	protected void setCamionEnAttente(Camion camion) {
		camionsEnAttente.add(camion);
		camion.setAttenteState();
	}

	/*
	 * Met un camion en traitement
	 */
	protected void setCamionEnTraitement(Camion camion) {
		if(this.camionEnTraitement != null){
			throw new IllegalStateException("Je veux mettre un camion en remplissage alors qu'il y en a deja un!");
		}
		this.camionEnTraitement = camion;
		camion.setEnTraitement();
		this.nbCamionsTraites++;
		
		//met en mode travail, reset le temps d'attente courant
		//
		this.state = Station.STATION_STATE_WORKING;
		this.currentWaitingPeriod = 0;
	}
	protected void setCamionOnArrival(Camion camion) {
		//si aucun camion en remplissage, met le camion en remplissage
		//
		if(camionEnTraitement == null) {
			setCamionEnTraitement(camion);
		}
		//sinon, ajoute le camion a la file d'attente
		else {
			setCamionEnAttente(camion);
		}
	}

	//si encore un camion en traitement, ne fait rien. Sinon, choisit le nouveau camion en traitement et update la file d'attente
	protected void updateFileAttente() {
		if(this.getCamionEnTraitement() == null || this.getCamionEnTraitement().getState()!= Camion.ETAT_EN_TRAITEMENT) {
			camionEnTraitement = null;
			if(camionsEnAttente.size()!=0) {
				Camion c = camionsEnAttente.get(0);
				camionsEnAttente.remove(c);
				setCamionEnTraitement(c);
			}
		}
		
		//si la station travaillait, mais est maintenant vide, met l'état idle
		if(this.state == Station.STATION_STATE_WORKING && this.camionEnTraitement == null) {
			this.state = Station.STATION_STATE_IDLE;
			this.currentWaitingPeriod = 0;
		}
	}

	protected abstract void updateQteTraite(double quantite, RockType rockType);

	//TODO rendre indépendant du nombre de pas de simulation.
	abstract void computeNewChargeSpeed();
}


