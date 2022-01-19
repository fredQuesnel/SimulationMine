package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

/**
 * Classe abstraite representant une station
 * @author Fred
 *
 */
public abstract class Station {

	/** État indiquant une pelle inactive*/
	public final static int STATION_STATE_IDLE = 1;
	/** État indiquant une pelle active*/
	public final static int STATION_STATE_WORKING = 2;
	/** État indiquant une pelle en panne */
	public final static int STATION_STATE_PANNE = 3;
	
	/** On calcule la charge moyenne d'un camion visitant la mine en prenant la moyenne des N derniers camions. Ce paramètre définit N.*/
	protected final static int AVG_LOAD_FORMULA_N = 20;
	
	/** Emplacement de la station*/
	private Point2D.Double location;
	
	/** Identifiant de la station*/
	private String id;
	
	/** True si la station est une station de décharge (concentrateur ou sterile)*/
	public boolean isDecharge; 
	
	
	/** Log d'arrivée des camions. Utile pour calculer la moyenne mobile des charges.*/
	protected ArrayList<Camion> arrivalLog;
	
	/** Camions en attente a la station*/
	protected ArrayList<Camion> camionsEnAttente;
	
	//ETAT de la station
	//
	/** Camion en traitement a la station*/
	protected Camion camionEnTraitement;
	/** Temps passé en attente depuis le dernier remplissage.*/
	protected double currentWaitingPeriod;
	/** Vitesse de traitement (charge/decharge) courante (en tonnes/secondes).*/
	protected double currentChargeSpeed;
	/**Etat de la pelle. Les options sont :
	 *  - STATION_STATE_IDLE : Ne fait rien.
	 *  - STATION_STATE_WORKING : Charge/decharge un camion.
	 *  - STATION_STATE_PANNE : Station est en panne.
	 */
	private int state;

	//infos sur l'iteration en cours
	//
	/**Taille du pas de l'iteration en cours.*/
	protected double iterStepSize;
	/**Temps passé dans l'iteration en cours*/
	protected double iterCurrentTime;
	/**True si la station a epuise le temps de l'iteration en cours*/
	protected boolean iterFinished;


	//statistiques
	//
	/** Temps total passe a attendre*/
	protected double waitingTime;
	/**Nombre de camions traites*/
	protected int nbCamionsTraites;

	
	/**Constructeur
	 * - i  : coordonnee en x de la station.
	 * - j  : coordonnee en y de la station.
	 * - id : identifiant de la station.
	 */
	protected Station(double i, double j, String id) {
		this.location = new Point2D.Double(i, j);
		this.id = id;
		this.state = Station.STATION_STATE_IDLE;
		
		this.arrivalLog = new ArrayList<Camion>();
		this.camionsEnAttente = new ArrayList<Camion>();
		camionEnTraitement = null;
		waitingTime = 0;
		nbCamionsTraites = 0;
	}

	/**Ajoute de temps d'iteration a la station
	 * 
	 * @param temps temps a ajouter
	 * */
	public void addIterTime(double temps) {
		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("temps ajouté plus grand que temps restant : "+temps+" > "+this.getRemainingTimeInTurn());
		}
		this.iterCurrentTime+= temps;
	}
	
	/**Temps de traitement moyen
	 * @return temps de traitement moyen*/
	public abstract double averageTraitementSpeed();

	/** 
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


	/**
	 * 
	 * @return Duree de la periode d'attente courante.
	 */
	public double getCurrentWaitingPeriod() {
		if(this.state != Station.STATION_STATE_IDLE && this.state != Station.STATION_STATE_PANNE ) {
			throw new IllegalStateException("Ne peut pas retourner la periode d'attente courante si la pelle n'est pas en attente ou en panne.");
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
	 * @return Nombre total de camions remplis par la station
	 */
	public int getNbCamionsTraites() {
		return nbCamionsTraites;
	}
	
	/**
	 * 
	 * @return Etat de la station
	 */
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
	
	/**
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
			//this.iterCurrentTime+= time;
		}
	}

	/**
	 * Retourne le temps restant dans le tour 
	 */
	protected double getRemainingTimeInTurn() {
		return this.iterStepSize-this.iterCurrentTime;
	}

	/**
	 * 
	 * @return true si l'iteration est terminee, false sinon.
	 */
	protected boolean iterFinished() {
		return this.iterFinished;
	}
	
	/**
	 * reset les stats de la pelle
	 */
	protected void resetStats() {
		this.waitingTime = 0;
		this.nbCamionsTraites = 0;
	}
	
	/**
	 * Prepare la pelle pour le debut d'un tour
	 */
	protected void setBeginStep(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterStepSize = stepSize;
		this.iterFinished = false;
		computeNewTraitementSpeed();

	}

	/**
	 * Met un camion dans la file d'attente
	 */
	protected void setCamionEnAttente(Camion camion) {
		camionsEnAttente.add(camion);
		camion.setAttenteState();
	}

	/**
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
	
	/**
	 * Traite un camion qui vient d'arriver.
	 * 
	 * @param camion : camion qui vient d'arriver.
	 */
	protected void setCamionOnArrival(Camion camion) {
		
		//Dans tous les cas, ajoute le camion au log des arrivées.
		this.arrivalLog.add(camion);
		
		
		if(camion.getCurrentStation()==null) {
			throw new IllegalStateException("Le camion doit avoir une station!");
		}
		
		if( this.state == Station.STATION_STATE_PANNE) {
			camion.setStateInactif();
		}
		//si aucun camion en remplissage, met le camion en remplissage
		//
		else if( this.state == Station.STATION_STATE_IDLE) {
			setCamionEnTraitement(camion);
		}
		//sinon, ajoute le camion a la file d'attente
		else if( this.state == Station.STATION_STATE_WORKING) {
			setCamionEnAttente(camion);
		}
		else {
			throw new IllegalStateException("État de la station "+this.getId()+" inconnu : "+this.getState());
		}
	}

	/**
	 * si encore un camion en traitement, ne fait rien. Sinon, choisit le nouveau camion en traitement et update la file d'attente
	 */
	protected void updateFileAttente() {
		if(this.getCamionEnTraitement() == null || this.getCamionEnTraitement().getState()!= Camion.ETAT_EN_TRAITEMENT) {
			camionEnTraitement = null;
			if(camionsEnAttente.size()!=0) {
				
				Camion c = camionsEnAttente.get(0);
				if(c.getCurrentStation()==null) {
					throw new IllegalStateException("Station "+this.getId()+" le camion ne peu pas avoir une currentstation nulle!");
				}
				
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

	/**
	 * Indique la quantite traitee
	 * @param quantite : quantite qui a ete traitee.
	 * @param rockType : type de roche traite.
	 */
	protected abstract void updateQteTraite(double quantite, RockType rockType);

	//TODO rendre indépendant du nombre de pas de simulation.
	abstract void computeNewTraitementSpeed();

	protected void setFailureMode(boolean failure) {
		if(failure) {
			if(this.state == Station.STATION_STATE_WORKING) {
				this.currentWaitingPeriod = 0;
			}
			this.camionEnTraitement = null;
			this.camionsEnAttente = new ArrayList<Camion>();
			this.state = Station.STATION_STATE_PANNE;
		}
		else {
			this.state = Station.STATION_STATE_IDLE;
		}
	}
}


