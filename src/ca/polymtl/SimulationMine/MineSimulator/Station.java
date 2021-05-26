package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

public abstract class Station {

	private Point2D.Double location;
	private String id;
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

	//constructeur
	protected Station(double i, double j, String id) {
		this.location = new Point2D.Double(i, j);
		this.id = id;
		
		this.camionsEnAttente = new ArrayList<Camion>();
		camionEnTraitement = null;
		waitingTime = 0;
		nbCamionsTraites = 0;
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
	 * @return Le temps total passé par la pelle en attente.
	 */
	public double getWaitTime() {
		return this.waitingTime;
	}

	//reset les stats de la pelle
	//
	protected void resetStats() {
		this.waitingTime = 0;
		this.nbCamionsTraites = 0;

	}
	
	/**
	 * 
	 * @return Nombre total de camions remplis par la pelle
	 */
	public int getNbCamionsTraites() {
		return nbCamionsTraites;
	}


	/**
	 * Retourne la coordonnée géographique de la station.
	 * @return Point2D.Double représentant la coordonnée géographique de la station.
	 */
	public Double getLocation() {

		return location;
	}


	/*
	 * Attend jusqu'à la fin du tour
	 */
	protected void waitForRemainingTime() {
		this.attend(this.getRemainingTimeInTurn());
		this.iterFinished = true;
	}
	
	
	/*
	 * prepare la pelle pour le debut d'un tour
	 */
	protected void setBeginStep(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterStepSize = stepSize;
		this.iterFinished = false;


	}
	
	
	protected void makeAllCamionWaitUntilEndIter() {
		if(this.camionEnTraitement!= null) {
			camionEnTraitement.waitUntilEndIter();
			System.out.println("Camion en traitement : "+camionEnTraitement.getRemainingTimeInTurn());
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

		if(this.nbCamionsTraites==0) {
			return 0;
		}
		return this.waitingTime/this.nbCamionsTraites;
	}
	protected void setCamionOnArrival(Camion camion) {
		//si aucun camion en remplissage, met le camion en remplissage
		//
		if(camionEnTraitement == null) {
			setCamionEnTraitement(camion);
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
	protected void setCamionEnAttente(Camion camion) {
		camionsEnAttente.add(camion);
		camion.setAttenteState();
	}


	/*
	 * retourne le temps restant dans le tour 
	 */
	protected double getRemainingTimeInTurn() {
		return this.iterStepSize-this.iterCurrentTime;
	}

	/*
	 * Fais attendre la pelle un temps donné, ou le reste du tour 
	 */
	protected void attend(double time) {

		//si le temps d'attente depasse le temps de l'iteration
		//fais attendre le temps de l'iteration
		//
		if(time >= this.getRemainingTimeInTurn()){
			this.currentWaitingPeriod+=this.getRemainingTimeInTurn();
			this.waitingTime += this.getRemainingTimeInTurn();
			this.iterCurrentTime = this.iterStepSize;
			this.iterFinished = true;
		}
		else{
			this.currentWaitingPeriod+=time;
			this.waitingTime+= time;
			this.iterCurrentTime+= time;
		}
	}
	/*
	 * Met un camion en traitement
	 */
	protected void setCamionEnTraitement(Camion camion) {
		if(this.camionEnTraitement != null){
			throw new IllegalStateException("Je veux mettre un camion en remplissage alors qu'il y en a deja un!");
		}
		this.camionEnTraitement = camion;
		this.nbCamionsTraites++;
	}

	/**
	 * 
	 * @return String contenant l'identifiant de la station.
	 */
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return La liste des camions en attente de remplissage (excluant le camion présentement en remplissage, si il y a lieu)
	 */
	public ArrayList<Camion> getCamionsEnAttente() {
		return camionsEnAttente;
	}

	/**Méthode abstraite pour activer la statiion.
	 * 
	 */
	protected void activate() {
		
		
	}


}


