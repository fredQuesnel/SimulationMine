package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D.Double;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class Camion {

	/*
	 * Constantes
	 */




	//etats possibles
	//
	/** État indiquant un camion auquel aucune activité n'est assignée.	 */
	public static int ETAT_INACTIF = 1;
	/** État indiquant un camion en route vers sa destination.	 */
	public static int ETAT_EN_ROUTE = 2;
	/** État indiquant un camion en attente de chargement.	 */
	public static int ETAT_ATTENTE = 3;
	/** État indiquant un camion en chargement.	 */
	public static int ETAT_EN_CHARGE = 4;
	/** État indiquant un camion qui viens juste d'arriver à sa destination.	 */
	public static int ETAT_JUSTE_ARRIVE = 5;
	/** État indiquant un camion en déchargement.	 */
	public static int ETAT_DECHARGE = 6;



	/*
	 * Champs
	 */
	//mine dans laquelle est le camion
	Mine mine;

	//Etat du camion
	//
	private Double location;
	private int state;


	//si en voyagement
	//
	private double speed;
	private double predictedTravelTime;
	private double currentTravelTime;
	private Station origine;
	private Station objective;
	private boolean goingEast;

	// Station (null si à aucune station)
	//
	private Station currentStation;

	/** 
	 * 
	 * @return Station courante du camion (indéfini si le camion n'est pas à une station)
	 */
	public Station getCurrentStation() {
		return currentStation;
	}

	// Type de roche dans le camion (ou de la pelle a laquelle le camion attend)
	//
	private RockType rockType;

	/**
	 * 
	 * @return Type de roche dans le camion (indéfini si le camion est vide)
	 */
	public RockType getRockType() {
		return rockType;
	}

	protected void setRockType(RockType rt) {
		this.rockType = rt;
	}

	//si en chargement
	private double charge;

	//statistiques de productivité
	//
	private double waitTime;
	private int numberOfRuns;
	private HashMap<Station, java.lang.Double> waitTimePerStation;
	private HashMap<Station, java.lang.Integer> nbVisitPerStation;


	//infos sur l'iteration en cours
	//
	private double iterStepSize;
	private double iterCurrentTime;
	private boolean iterFinished;

	//images du camion
	private BufferedImage goingEastImage;
	private BufferedImage goingWestImage;

	//constructeur
	//
	public Camion(Station station, Mine mine, BufferedImage goingEastImage) {

		this.goingEastImage = goingEastImage;
		this.goingWestImage = goingEastImage;
		if(goingEastImage != null) {
			// Flip the image horizontally
			AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
			tx.translate(-(this.goingWestImage).getWidth(null), 0);
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			this.goingWestImage = op.filter(this.goingWestImage, null);
		}


		this.mine = mine;

		this.speed = 2;
		this.state = ETAT_INACTIF;
		this.waitTime = 0;
		this.numberOfRuns = 0;
		this.goingEast = true;
		this.currentStation = station;
		this.objective = station;

		this.setLocation((int) station.getLocation().getX(), (int) station.getLocation().getY());
		resetStats();
	}

	/**
	 * 
	 * @return La vitesse du camion (m/s) sous forme d'un double
	 */
	public double getSpeed() {
		return speed;
	}

	protected void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * 
	 * @return Le temps total (s) que le camion a passé en attente durant la simulation courante.
	 */
	public double getWaitTime() {
		return waitTime;
	}

	/**
	 * 
	 * @return Le nombre de voyages (allé-retours) que le camion a effectué durant la simulation courante.
	 */
	public int getNumberOfRuns() {
		return numberOfRuns;
	}

	protected void setNumberOfRuns(int numberOfRuns) {
		this.numberOfRuns = numberOfRuns;
	}

	/**
	 * 
	 * @return État du camion correspondant à l'activité en cours.
	 */
	public int getState() {
		return state;
	}

	//set la position du camion
	//
	private void setLocation(double i, double j) {
		this.location = new Double(i,j);
	}

	/**
	 * 
	 * @return Destination du camion, ou null si le camion n'est pas en déplacement.
	 */
	public Station getObjective() {
		return objective;

	}
	/**
	 * 
	 * NE PAS UTILISER! Donne un objetif au camion.
	 */
	public void setObjective(Station objective) {

		this.origine = this.currentStation;
		this.objective = objective;
		this.goingEast = true;
		if(objective.getLocation().getX() > this.location.getX()) {
			this.goingEast = false;
		}
		this.speed = getRandomSpeed();
		this.state = ETAT_EN_ROUTE;

		this.predictedTravelTime = mine.getTravelTimePredictor().predictTravelTime(currentStation, objective);
		this.currentTravelTime = 0;

	}

	/**
	 * 
	 * @return true si la destination du camion est a l'est de son emplacement. 
	 * Utilisé pour l'interface graphique.
	 */
	public boolean isGoingEast() {
		return goingEast;
	}

	//retourne une vitesse aleatoire 
	//
	private double getRandomSpeed() {
		return SimulationMine.random.nextGaussian()*getStdSpeed()+getAvgSpeed();
	}


	public abstract double getChargeMax();

	public abstract double getAvgSpeed();
	public abstract double getStdSpeed();

	/**
	 * 
	 * @return Emplacement du camion sous la forme d'un Point2D.double
	 */
	public Double getLocation() {
		return this.location;
	}

	//avance le camion vers sa destination
	//
	protected void advance() {
		if(this.state != Camion.ETAT_EN_ROUTE) {
			throw new IllegalStateException();
		}

		double timeIncrement = this.getRemainingTimeInTurn();
		//Si la distance entre l'objectif et la position est inferieur a la distance parcourue dans l'incerment de temps
		//positionne le camion a sa destination.
		double distanceRestante = objective.getLocation().distance(this.location);

		double speedWithMeteo = this.speed*mine.getMeteoFactor();

		double distanceParcourue = timeIncrement*speedWithMeteo;

		if(distanceParcourue >= distanceRestante) {
			this.currentStation = this.objective;
			location.setLocation(objective.getLocation());
			this.state = ETAT_JUSTE_ARRIVE;
			//retourne le temps restant dans le tour
			this.iterCurrentTime+= timeIncrement - distanceRestante/speedWithMeteo;

			this.currentTravelTime += distanceRestante/speedWithMeteo;		
		}

		//sinon, calcule le nouveau point a partir de formule trigonometriques
		else {
			double deltaX = this.objective.getLocation().getX() - this.location.getX();
			double deltaY = this.objective.getLocation().getY() - this.location.getY();


			double theta = Math.atan2(deltaY, deltaX);

			double speedX = 1.*speedWithMeteo*Math.cos(theta);
			double speedY = 1.*speedWithMeteo*Math.sin(theta);


			location.setLocation(location.getX() + speedX*timeIncrement, location.getY() + speedY*timeIncrement);
			//this.setRemainingTimeInTurn(0);

			this.currentTravelTime += timeIncrement;
			this.iterCurrentTime = this.iterStepSize;
			this.iterFinished = true;
		}


	}

	//charge le camion 
	//
	protected void remplis(double quantite, double temps) {

		//valide la demande de decharge
		//marge d'erreur de 0.0001 pour erreur numérique
		if(quantite - 0.0001 > this.getChargeMax()-this.charge) {
			throw new IllegalArgumentException("Quantité chargée trop grande : "+quantite+" > "+(this.getChargeMax()-this.charge));
		}
		if(temps > this.getRemainingTimeInTurn()) {
			throw new IllegalArgumentException("Temps de décharge trop grand : "+temps+" > "+this.getRemainingTimeInTurn());
		}
		if(this.getState()!= Camion.ETAT_EN_CHARGE) {
			throw new IllegalStateException("Peut seulement décharger en ETAT_CHARGE. État actuel : "+this.getState());
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

	//fait attendre un camion qui est soit en file d'attente, soit en charge
	//
	protected void attend(double temps) {
		//on peut attendre en etat "en charge" si la pelle a deja fini son tour quand le camion arrive.
		if(this.state != Camion.ETAT_ATTENTE && this.state != Camion.ETAT_EN_CHARGE && this.state != Camion.ETAT_DECHARGE  ) {
			throw new IllegalStateException("Le camion doit etre en attente. Etat actuel : "+this.state);
		}

		if(temps > this.getRemainingTimeInTurn()) {
			throw new IllegalArgumentException("Le temps d'attente ne doit pas dépasser le temps restant : "+temps+" > "+this.getRemainingTimeInTurn());
		}

		this.waitTime+= temps;
		this.iterCurrentTime+= temps;

		if(this.iterStepSize-iterCurrentTime < 0.00001) {
			this.iterFinished = true;
		}

		double newWaitTime = this.waitTimePerStation.get(currentStation).doubleValue()+temps;
		this.waitTimePerStation.replace(currentStation, newWaitTime);
	}

	//retourne le temps restant dans l'iteration courante
	//
	protected double getRemainingTimeInTurn() {
		return this.iterStepSize-this.iterCurrentTime;
	}

	//met le camion en attente
	//
	protected void setAttenteState() {

		this.state = Camion.ETAT_ATTENTE;

	}

	//met le camion en etat inactif
	protected void setStateIdle() {
		this.state = Camion.ETAT_INACTIF;
	}

	//reset les statistiques de productivite du camion
	protected void resetStats() {
		this.numberOfRuns = 0;
		this.waitTime = 0;

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

	/**
	 * @return the predictedTravelTime
	 */
	public double getPredictedTravelTime() {
		return predictedTravelTime;
	}

	/**
	 * @return the currentTravelTime
	 */
	public double getCurrentTravelTime() {
		return currentTravelTime;
	}

	/**
	 * @return the origine
	 */
	public Station getOrigine() {
		return origine;
	}

	/**
	 * 
	 * @return Si le camion est présentement en charge, retourne le temps courant de chargement.
	 */
	public double getCharge() {

		return this.charge;
	}

	//retourne la charge restante
	//
	public double getChargeRemaining() {
		return this.getChargeMax()-this.getCharge();
	}

	public BufferedImage getGoingEastImage() {
		return goingEastImage;
	}

	public BufferedImage getGoingWestImage() {
		return goingWestImage;
	}

	//retourne le temps maximum ou le camion peut etre en charge dans l'iteration courante
	//Il sagit du min entre
	//	1) temps pour que le camion soit completement charge
	//	2) temps avant la fin du tour
	protected double getMaxChargeTimeInTurn(double chargeSpeed) {

		double completeChargeTime = this.getChargeRemaining()/chargeSpeed;
		if(completeChargeTime < this.getRemainingTimeInTurn()) {
			return completeChargeTime;
		}
		else {
			return this.getRemainingTimeInTurn();
		}
	}

	//prepare le camion pour le debut d'une iteration
	//
	protected void setBeginIter(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterStepSize = stepSize;
		this.iterFinished = false;
	}

	//retourne true si l'iteration actuelle est terminee pour le camion (il a épuisé tout son temps)
	//
	protected boolean iterFinished() {
		return this.iterFinished;
	}

	//fait attendre le camion jusqu'à la fin du tour
	//
	protected void waitUntilEndIter() {
		this.attend(this.getRemainingTimeInTurn());
		this.iterFinished = true;
	}

	/**
	 * 
	 * @return true si le camion est complètement rempli, false sinon.
	 */
	public boolean isFull() {

		if(this.charge >= this.getChargeMax()) {
			return true;
		}
		else {
			return false;
		}
	}

	//retourne le temps de l'iteration pour le camion
	//
	protected double getIterCurrentTime() {
		return this.iterCurrentTime;
	}

	//met le camion en charge
	//
	protected void setEnChargeState() {
		this.charge = 0;
		this.state = Camion.ETAT_EN_CHARGE;

		//incremente le nombre de visites
		//
		int newValue = this.nbVisitPerStation.get(this.currentStation).intValue()+1;
		this.nbVisitPerStation.replace(currentStation, newValue);

	}

	/**
	 * 
	 * @return La moyenne du temps d'attente du camion à la station donnée
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
	 * @return Le temps total d'attente du camion à la station
	 */
	public double getAttenteAtStation(Station station){
		return this.waitTimePerStation.get(station).doubleValue();
	}

	/**
	 * 
	 * @return Le nombre de fois où le camion est allé à la station
	 */
	public double getNbVisitAtStation(Station station){
		return this.nbVisitPerStation.get(station).intValue();
	}

	public void decharge(double quantiteDecharge, double tempsTraitement) {

		//valide la demande de decharge
		//marge d'erreur de 0.0001 pour erreur numériques
		if(quantiteDecharge - 0.0001 > this.charge) {
			throw new IllegalArgumentException("Quantité déchargée trop grande : "+quantiteDecharge+" > "+this.charge);
		}
		if(tempsTraitement > this.getRemainingTimeInTurn()) {
			throw new IllegalArgumentException("Temps de décharge trop grand : "+tempsTraitement+" > "+this.getRemainingTimeInTurn());
		}
		if(this.getState()!= Camion.ETAT_DECHARGE) {
			throw new IllegalStateException("Peut seulement décharger en ETAT_DECHARGE. État actuel : "+this.getState());
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

	//met le camion en etat decharge
	public void setstateDecharge() {
		this.state = Camion.ETAT_DECHARGE;
	}





}

