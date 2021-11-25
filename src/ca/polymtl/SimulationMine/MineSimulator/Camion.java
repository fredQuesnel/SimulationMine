package ca.polymtl.SimulationMine.MineSimulator;


import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D.Double;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Classe abstraite décrivant un camion. 
 * Cette classe est seulement abstraite pour permettre de déclarer différents types de camions.
 * 
 * @author Frédéric Quesnel
 */
public abstract class Camion {

	/*
	 * Constantes
	 */

	public static int TYPE_SMALL = 1;
	public static int TYPE_LARGE = 2;
	
	//etats possibles
	//
	/** ï¿½tat indiquant un camion auquel aucune activitï¿½ n'est assignï¿½e.	 */
	public static int ETAT_INACTIF = 1;
	/** ï¿½tat indiquant un camion en route vers sa destination.	 */
	public static int ETAT_EN_ROUTE = 2;
	/** ï¿½tat indiquant un camion en attente de chargement.	 */
	public static int ETAT_ATTENTE = 3;
	/** ï¿½tat indiquant un camion en chargement.	 */
	public static int ETAT_EN_TRAITEMENT = 4;
	/** ï¿½tat indiquant un camion qui viens juste d'arriver ï¿½ sa destination.	 */
	public static int ETAT_JUSTE_ARRIVE = 5;
	
	/*
	 * Champs
	 */
	/**mine dans laquelle est le camion*/
	Mine mine;
	
	//Caractéristiques du camion
	protected int type;
	/** Vitesse moyenne du camion	 */
	protected double avgSpeed;
	/** ï¿½cart type sur la vitesse du camion	 */
	protected double stdSpeed;
	/** Charge maximum du camion.	 */
	protected double chargeMax;
	/** Facteur d'ajustement pour la prediction des temps de parcours*/
	protected double predictTimeAdjustFactor; 
	
	
	//Etat du camion
	//
	/**Emplacement du camion*/
	private Double location;
	
	/**Etat du camion (ETAT_INACTIF, ETAT_EN_ROUTE, ETAT_ATTENTE, ETAT_EN_TRAITEMENT, ou ETAT_JUSTE_ARRIVE)*/
	private int state;


	//si en voyagement
	//
	/**Vitesse courante du camion, sans prendre en compte la météo. Indéfini si le camion n'est pas en déplacement (ETAT_EN_ROUTE).*/
	private double speed;
	/**Temps de parcours prédit pour le trajet en cours. Indéfini si le camion n'est pas en déplacement (ETAT_EN_ROUTE).*/
	private double predictedTravelTime;
	/**Temps de parcours pour le trajet en cours. Indéfini si le camion n'est pas en déplacement (ETAT_EN_ROUTE).*/
	private double currentTravelTime;
	
	/**Station de départ pour le trajet en cours. Indéfini si le camion n'est pas en déplacement (ETAT_EN_ROUTE).*/
	private Station origine;
	
	/**Destination du camion. Indéfini si le camion n'est pas en déplacement (ETAT_EN_ROUTE).*/
	private Station destination;
	
	/**True si le camion se déplace vers l'est (la droite). Nécessaire pour savoir dans quelle direction dessiner le camion.*/ 
	private boolean goingEast;

	/** Station où se trouve le camion. Indéfini si le camion n'est pas à une station.*/
	private Station currentStation;

	/** Type de roche dans le camion */
	private RockType rockType;

	/** Quantité de roche dans le camion, en tonnes.*/
	private double charge;

	//statistiques de productivitï¿½
	//
	/**Temps total que le camion a passé à attendre*/
	private double waitTime;

	/** Nombre de "voyages" que le camion a effectué*/
	private int numberOfRuns;

	/**Temps d'attente du camion à chaque station visitée*/
	private HashMap<Station, java.lang.Double> waitTimePerStation;

	/**Nombre de visite à chaque station*/
	private HashMap<Station, java.lang.Integer> nbVisitPerStation;
	//infos sur l'iteration en cours
	//
	
	/**Durée de l'itération en cours*/
	private double iterStepSize;
	/**Temps simulé dans l'itération en cours*/
	private double iterCurrentTime;
	/**true si l'itération est terminée pour le camion*/
	private boolean iterFinished;


	//images du camion
	
	/**Image du camion allant vers l'est (la droite)*/
	private BufferedImage goingEastImage;
	/**Image du camion allant vers l'ouest (la gauche)*/
	private BufferedImage goingWestImage;

	/**Temps passé à voyager vide*/
	private double emptyTravelTime;

	/**constructeur*/
	//
	public Camion(Station station, Mine mine, BufferedImage goingEastImage) {
		
		
		//Créé l'image du camion allant vest l'ouest.
		this.goingEastImage = goingEastImage;
		this.goingWestImage = goingEastImage;
		if(goingEastImage != null) {
			// Flip the image horizontally
			AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
			tx.translate(-(this.goingWestImage).getWidth(null), 0);
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			this.goingWestImage = op.filter(this.goingWestImage, null);
		}
		//mine
		this.mine = mine;
		
		//le camion est inactif au départ
		this.state = ETAT_INACTIF;
		this.goingEast = true;
		this.destination = null;
		
		//station courante
		this.currentStation = station;
		this.setLocation((int) station.getLocation().getX(), (int) station.getLocation().getY());
		
		//Initialise les champs de statistiques
		resetStats();
	}
	
	/**
	 * 
	 * @return Le temps total d'attente du camion a la station
	 */
	public double getAttenteAtStation(Station station){
		return this.waitTimePerStation.get(station).doubleValue();
	}
	
	/**
	 * @return La moyenne du temps d'attente du camion a la station donnee
	 */
	public double getAverageWaitTimeSecondsForStation(Station station){
		double waitTime =this.waitTimePerStation.get(station).doubleValue();
		int nbPeriodes = this.nbVisitPerStation.get(station).intValue();
		if(nbPeriodes == 0){
			return 0;
		}
		return waitTime/nbPeriodes;
	}

	/**
	 * 
	 * @return Vitesse moyenne du camion
	 */
	public double getAvgSpeed() {
		return avgSpeed;
	}

	/**
	 * 
	 * @return Charge max du camion
	 */
	public double getChargeMax() {
		return chargeMax;
	}

	/**
	 * 
	 * @return Facteur d'ajustement pour prediction de temps de parcours
	 */
	public double getPredictTimeAdjustFactor() {
		return predictTimeAdjustFactor;
	}

	/**
	 * 
	 * @return Ecart type sur la vitesse du camion
	 */
	public double getStdSpeed() {
		return stdSpeed;
	}

	
	/**
	 * @return Charge dans le camion
	 */
	public double getCharge() {

		return this.charge;
	}

	/** 
	 * @return Station courante du camion (indï¿½fini si le camion n'est pas ï¿½ une station)
	 */
	public Station getCurrentStation() {
		return currentStation;
	}

	/**
	 * @return the currentTravelTime
	 */
	public double getCurrentTravelTime() {
		return currentTravelTime;
	}

	/** @return Image du camion allant vers l'est*/
	public BufferedImage getGoingEastImage() {
		return goingEastImage;
	}

	/** @return Image du camion allant vers l'ouest*/
	public BufferedImage getGoingWestImage() {
		return goingWestImage;
	}

	/**
	 * @return Emplacement du camion sous la forme d'un Point2D.double
	 */
	public Double getLocation() {
		return this.location;
	}

	/**
	 * @return Le nombre de fois ou le camion est alle a la station
	 */
	public double getNbVisitAtStation(Station station){
		return this.nbVisitPerStation.get(station).intValue();
	}

	/**
	 * @return Le nombre de voyages (alle-retours) que le camion a effectue durant la simulation courante.
	 */
	public int getNumberOfRuns() {
		return numberOfRuns;
	}
	/**
	 * 
	 * @return Destination du camion, ou null si le camion n'est pas en dï¿½placement.
	 */
	public Station getDestination() {
		return destination;

	}

	/**
	 * @return the origine
	 */
	public Station getOrigine() {
		return origine;
	}

	/**
	 * @return the predictedTravelTime
	 */
	public double getPredictedTravelTime() {
		return predictedTravelTime;
	}

	

	/**
	 * @return Type de roche dans le camion (indefini si le camion est vide)
	 */
	public RockType getRockType() {
		return rockType;
	}
	/**
	 * @return La vitesse du camion (m/s) sous forme d'un double
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * @return etat du camion correspondant a l'activite en cours.
	 */
	public int getState() {
		return state;
	}



	/**
	 * @return Le temps total (s) que le camion a passe en attente durant la simulation courante.
	 */
	public double getWaitTime() {
		return waitTime;
	}

	/**
	 * Utilise pour l'interface graphique.
	 * @return true si la destination du camion est a l'est de son emplacement. 
	 */
	public boolean isGoingEast() {
		return goingEast;
	}

	/**
	 * 
	 * NE PAS UTILISER! Donne un objetif au camion.
	 */
	public void setObjective(Station objective) {

		this.origine = this.currentStation;
		this.currentStation = null;
		this.destination = objective;
		this.goingEast = true;
		if(objective.getLocation().getX() > this.location.getX()) {
			this.goingEast = false;
		}
		this.speed = getRandomSpeed();
		this.state = ETAT_EN_ROUTE;

		this.currentTravelTime = 0;

	}
	
	/** Set la prediction pour le parcours courant.*/
	public void setPredictedTravelTime(double predictedTravelTime) {
		this.predictedTravelTime = predictedTravelTime;
	}

	/**calcule le temps restant a la tache actuelle*/
	public double taskTimeRemaining() {
		
		if(this.getState() == Camion.ETAT_ATTENTE) {
			return  java.lang.Double.MAX_VALUE;
		}
		else if(this.getState() == Camion.ETAT_EN_ROUTE) {
			double distanceRestante = destination.getLocation().distance(this.location);

			double speedWithMeteo = this.speed*mine.getMeteoFactor();
			return distanceRestante/speedWithMeteo;
		}
		else if(this.getState() == Camion.ETAT_EN_TRAITEMENT) {
			double chargeATraiter = 0;
			if(this.currentStation.isDecharge) {
				chargeATraiter = this.getCharge();
			}
			else {
				chargeATraiter = this.getChargeMax()-this.getCharge();
			}
			return chargeATraiter/this.currentStation.currentChargeSpeed;
			
		}
		return java.lang.Double.MAX_VALUE;
	}

	/** @return une vitesse aleatoire*/ 
	private double getRandomSpeed() {
		return SimulationMine.random.nextGaussian()*getStdSpeed()+getAvgSpeed();
	}

	/** Set la position du camion*/
	private void setLocation(double i, double j) {
		this.location = new Double(i,j);
	}

	/**avance le camion vers sa destination pour une durée déterminée. */
	protected void advance(double temps) {
		if(this.state != Camion.ETAT_EN_ROUTE) {
			throw new IllegalStateException();
		}

		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalStateException("Le temps de deplacement ne doit pas dï¿½passer le temps restant : "+temps + " > "+this.getRemainingTimeInTurn());
		}
		double timeIncrement = temps;//this.getRemainingTimeInTurn();
		//Si la distance entre l'objectif et la position est inferieur a la distance parcourue dans l'incerment de temps
		//positionne le camion a sa destination.
		double distanceRestante = destination.getLocation().distance(this.location);

		double speedWithMeteo = this.speed*mine.getMeteoFactor();

		double distanceParcourue = timeIncrement*speedWithMeteo;

		if(distanceParcourue >= distanceRestante) {
			if(this.destination == null) {
				throw new IllegalStateException("objective == null");
			}
			this.currentStation = this.destination;
			location.setLocation(destination.getLocation());
			this.state = ETAT_JUSTE_ARRIVE;
			//retourne le temps restant dans le tour
			this.iterCurrentTime+= distanceRestante/speedWithMeteo;
			if(this.getCharge() == 0) {
				this.emptyTravelTime+= distanceRestante/speedWithMeteo;
			}
			this.currentTravelTime += distanceRestante/speedWithMeteo;		
		}

		//sinon, calcule le nouveau point a partir de formule trigonometriques
		else {
			double deltaX = this.destination.getLocation().getX() - this.location.getX();
			double deltaY = this.destination.getLocation().getY() - this.location.getY();


			double theta = Math.atan2(deltaY, deltaX);

			double speedX = 1.*speedWithMeteo*Math.cos(theta);
			double speedY = 1.*speedWithMeteo*Math.sin(theta);


			location.setLocation(location.getX() + speedX*timeIncrement, location.getY() + speedY*timeIncrement);
			//this.setRemainingTimeInTurn(0);

			this.currentTravelTime += timeIncrement;
			
			if(this.getCharge() == 0) {
				this.emptyTravelTime+= timeIncrement;
			}
			
			this.iterCurrentTime += timeIncrement;
			
		}
		if(this.iterStepSize - this.iterCurrentTime < 0.00001 ) {
			iterCurrentTime = iterStepSize;
			this.iterFinished = true;
		}

	}

	/** Fait attendre un camion qui est soit en file d'attente, soit en charge */
	protected void attend(double temps) {
		//on peut attendre en etat "en charge" si la pelle a deja fini son tour quand le camion arrive.
		if(this.state != Camion.ETAT_ATTENTE ) {
			throw new IllegalStateException("Le camion doit etre en attente. Etat actuel : "+this.state);
		}

		if(temps > this.getRemainingTimeInTurn()+0.00001) {
			throw new IllegalArgumentException("Le temps d'attente ne doit pas depasser le temps restant : "+temps+" > "+this.getRemainingTimeInTurn());
		}

		this.waitTime+= temps;
		this.iterCurrentTime+= temps;

		if(this.iterStepSize-iterCurrentTime < 0.00001) {
			iterCurrentTime = iterStepSize;
			this.iterFinished = true;
		}

		double newWaitTime = this.waitTimePerStation.get(currentStation).doubleValue()+temps;
		this.waitTimePerStation.replace(currentStation, newWaitTime);
	}

	/**Charge le camion d'une certaine quantité pour un certain temps.*/ 
	protected void charge(double quantite, double temps) {

		//valide la demande de decharge
		//marge d'erreur de 0.0001 pour erreur numï¿½rique
		if(quantite - 0.0001 > this.getChargeMax()-this.charge) {
			throw new IllegalArgumentException("Quantitï¿½ chargï¿½e trop grande : "+quantite+" > "+(this.getChargeMax()-this.charge));
		}
		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("Temps de dï¿½charge trop grand : "+temps+" > "+this.getRemainingTimeInTurn());
		}
		if(this.getState()!= Camion.ETAT_EN_TRAITEMENT) {
			throw new IllegalStateException("Peut seulement dï¿½charger en ETAT_CHARGE. ï¿½tat actuel : "+this.getState());
		}


		this.iterCurrentTime+=temps;
		this.charge+= quantite;

		if(this.getChargeMax()- this.charge  < 0.00001) {
			charge = this.getChargeMax();
		}
		
		if(iterStepSize- iterCurrentTime < 0.00001 ) {
			iterCurrentTime = iterStepSize;
			this.iterFinished = true;
		}

	}

	/** Décharge le camion d'une certaine quantité pour un certain temps.*/
	protected void decharge(double quantiteDecharge, double tempsTraitement) {
	
		//valide la demande de decharge
		//marge d'erreur de 0.0001 pour erreur numï¿½riques
		if(quantiteDecharge - 0.0001 > this.charge) {
			throw new IllegalArgumentException("Quantitï¿½ dï¿½chargï¿½e trop grande : "+quantiteDecharge+" > "+this.charge);
		}
		if(tempsTraitement > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("Temps de dï¿½charge trop grand : "+tempsTraitement+" > "+this.getRemainingTimeInTurn());
		}
		if(this.getState()!= Camion.ETAT_EN_TRAITEMENT) {
			throw new IllegalStateException("Peut seulement dï¿½charger en ETAT_EN_TRAITEMENT. ï¿½tat actuel : "+this.getState());
		}
	
		this.iterCurrentTime+=tempsTraitement;
		this.charge-= quantiteDecharge;
	
		if(charge < 0.00001) {
			charge = 0;
		}
		if(iterStepSize- iterCurrentTime < 0.00001 ) {
			iterCurrentTime = iterStepSize;
			this.iterFinished = true;
		}
	}

	/**@return le temps de l'iteration pour le camion*/
	protected double getIterCurrentTime() {
		return this.iterCurrentTime;
	}

	/**@return le temps restant dans l'iteration courante*/
	protected double getRemainingTimeInTurn() {
		return this.iterStepSize-this.iterCurrentTime;
	}

	/**@return true si l'iteration actuelle est terminee pour le camion (il a epuise tout son temps)*/
	//
	protected boolean iterFinished() {
		return this.iterFinished;
	}

	/**Reset les statistiques de productivite du camion.*/
	protected void resetStats() {
		this.numberOfRuns = 0;
		this.waitTime = 0;
		this.emptyTravelTime = 0;

		waitTimePerStation = new HashMap<Station, java.lang.Double>();
		nbVisitPerStation = new HashMap<Station, Integer>();
		//set les statistiques d'attente par pelle
		ArrayList<Pelle> pelles = mine.getPelles();
		for(int i = 0 ; i < pelles.size(); i++){

			waitTimePerStation.put(pelles.get(i), 0.);
			nbVisitPerStation.put(pelles.get(i), 0);

		}

		//les concentrateurs
		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
			waitTimePerStation.put(mine.getConcentrateurs().get(i), 0.);
			nbVisitPerStation.put(mine.getConcentrateurs().get(i), 0);

		}

		//les steriles
		for(int i = 0 ; i < mine.getSteriles().size(); i++) {
			waitTimePerStation.put(mine.getSteriles().get(i), 0.);
			nbVisitPerStation.put(mine.getSteriles().get(i), 0);
		}
	}

	/**Met le camion en attente*/
	protected void setAttenteState() {
		if(this.state != Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalStateException("Le camion doit etre en etat ETAT_JUSTE_ARRIVE pour etre mis en attente. Etat actuel : "+this.state);
		}
		int newVisitNb = this.nbVisitPerStation.get(currentStation)+1;
		
		this.nbVisitPerStation.replace(currentStation, newVisitNb);
		
		this.state = Camion.ETAT_ATTENTE;
	}

	/**Prepare le camion pour le debut d'une iteration*/
	protected void setBeginIter(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterStepSize = stepSize;
		this.iterFinished = false;
	}

	/**Met le camion en traitement*/
	protected void setEnTraitement() {
		if(this.currentStation==null) {
			throw new IllegalStateException("Ne peut pas etre mis en traitement si aucune station courante.");
		}
		this.state = Camion.ETAT_EN_TRAITEMENT;
	}

	
	/**Lorsque le camion a terminé de se faire traiter a un concentrateur ou un sterile, augmente le nombre de "voyages" qu'il a effectue.*/
	protected void setNumberOfRuns(int numberOfRuns) {
		this.numberOfRuns = numberOfRuns;
	}
	
	/** Set le type de roches dans le camion*/
	protected void setRockType(RockType rt) {
		this.rockType = rt;
	}

	/** Set la vitesse du camion*/
	protected void setSpeed(double speed) {
		this.speed = speed;
	}

	/**Met le camion en etat inactif*/
	protected void setStateInactif() {
		this.state = Camion.ETAT_INACTIF;
	}
	
	/** @return le temps de transport a vide du camion.*/
	public double getEmptyTravelTime() {
		return this.emptyTravelTime;
	}

	public int getType() {
		return type;
	}





}

