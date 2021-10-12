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

	//etats possibles
	//
	/** État indiquant un camion auquel aucune activité n'est assignée.	 */
	public static int ETAT_INACTIF = 1;
	/** État indiquant un camion en route vers sa destination.	 */
	public static int ETAT_EN_ROUTE = 2;
	/** État indiquant un camion en attente de chargement.	 */
	public static int ETAT_ATTENTE = 3;
	/** État indiquant un camion en chargement.	 */
	public static int ETAT_EN_TRAITEMENT = 4;
	/** État indiquant un camion qui viens juste d'arriver à sa destination.	 */
	public static int ETAT_JUSTE_ARRIVE = 5;
	/** État indiquant un camion en déchargement.	 */
	//public static int ETAT_DECHARGE = 6;


	
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

	// Type de roche dans le camion (ou de la pelle a laquelle le camion attend)
	//
	private RockType rockType;

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
	private MineSimulator mineSimulator;

	//constructeur
	//
	public Camion(Station station, Mine mine, MineSimulator mineSimulator, BufferedImage goingEastImage) {
		this.mineSimulator = mineSimulator;
		
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
	 * @return Le temps total d'attente du camion à la station
	 */
	public double getAttenteAtStation(Station station){
		return this.waitTimePerStation.get(station).doubleValue();
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

	public abstract double getAvgSpeed();
	
	/**
	 * 
	 * @return Si le camion est présentement en charge, retourne le temps courant de chargement.
	 */
	public double getCharge() {

		return this.charge;
	}

	public abstract double getChargeMax();

	/** 
	 * 
	 * @return Station courante du camion (indéfini si le camion n'est pas à une station)
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

	public BufferedImage getGoingEastImage() {
		return goingEastImage;
	}

	public BufferedImage getGoingWestImage() {
		return goingWestImage;
	}

	/**
	 * 
	 * @return Emplacement du camion sous la forme d'un Point2D.double
	 */
	public Double getLocation() {
		return this.location;
	}

	/**
	 * 
	 * @return Le nombre de fois où le camion est allé à la station
	 */
	public double getNbVisitAtStation(Station station){
		return this.nbVisitPerStation.get(station).intValue();
	}

	/**
	 * 
	 * @return Le nombre de voyages (allé-retours) que le camion a effectué durant la simulation courante.
	 */
	public int getNumberOfRuns() {
		return numberOfRuns;
	}
	/**
	 * 
	 * @return Destination du camion, ou null si le camion n'est pas en déplacement.
	 */
	public Station getObjective() {
		return objective;

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


	public abstract double getPredictTimeAdjustFactor();

	/**
	 * 
	 * @return Type de roche dans le camion (indéfini si le camion est vide)
	 */
	public RockType getRockType() {
		return rockType;
	}
	/**
	 * 
	 * @return La vitesse du camion (m/s) sous forme d'un double
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * 
	 * @return État du camion correspondant à l'activité en cours.
	 */
	public int getState() {
		return state;
	}

	public abstract double getStdSpeed();

	/**
	 * 
	 * @return Le temps total (s) que le camion a passé en attente durant la simulation courante.
	 */
	public double getWaitTime() {
		return waitTime;
	}

	/**
	 * 
	 * @return true si la destination du camion est a l'est de son emplacement. 
	 * Utilisé pour l'interface graphique.
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
		this.objective = objective;
		this.goingEast = true;
		if(objective.getLocation().getX() > this.location.getX()) {
			this.goingEast = false;
		}
		this.speed = getRandomSpeed();
		this.state = ETAT_EN_ROUTE;

		this.predictedTravelTime = mineSimulator.getTravelTimePredictor().predictTravelTime(currentStation, objective, this);
		this.currentTravelTime = 0;

	}

	//calcule le temps restant a la tache actuelle
	public double taskTimeRemaining() {
		
		if(this.getState() == Camion.ETAT_ATTENTE) {
			return  java.lang.Double.MAX_VALUE;
		}
		else if(this.getState() == Camion.ETAT_EN_ROUTE) {
			double distanceRestante = objective.getLocation().distance(this.location);

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

	//retourne une vitesse aleatoire 
	//
	private double getRandomSpeed() {
		return SimulationMine.random.nextGaussian()*getStdSpeed()+getAvgSpeed();
	}

	//set la position du camion
	//
	private void setLocation(double i, double j) {
		this.location = new Double(i,j);
	}

	//avance le camion vers sa destination
	//
	protected void advance(double temps) {
		if(this.state != Camion.ETAT_EN_ROUTE) {
			throw new IllegalStateException();
		}

		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalStateException("Le temps de deplacement ne doit pas dépasser le temps restant : "+temps + " > "+this.getRemainingTimeInTurn());
		}
		double timeIncrement = temps;//this.getRemainingTimeInTurn();
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
			this.iterCurrentTime+= distanceRestante/speedWithMeteo;

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
			this.iterCurrentTime += timeIncrement;
			
		}
		if(this.iterStepSize - this.iterCurrentTime < 0.00001 ) {
			iterCurrentTime = iterStepSize;
			this.iterFinished = true;
		}

	}

	//fait attendre un camion qui est soit en file d'attente, soit en charge
	//
	protected void attend(double temps) {
		//on peut attendre en etat "en charge" si la pelle a deja fini son tour quand le camion arrive.
		if(this.state != Camion.ETAT_ATTENTE ) {
			throw new IllegalStateException("Le camion doit etre en attente. Etat actuel : "+this.state);
		}

		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("Le temps d'attente ne doit pas dépasser le temps restant : "+temps+" > "+this.getRemainingTimeInTurn());
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

	//charge le camion 
	//
	protected void charge(double quantite, double temps) {

		//valide la demande de decharge
		//marge d'erreur de 0.0001 pour erreur numérique
		if(quantite - 0.0001 > this.getChargeMax()-this.charge) {
			throw new IllegalArgumentException("Quantité chargée trop grande : "+quantite+" > "+(this.getChargeMax()-this.charge));
		}
		if(temps > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("Temps de décharge trop grand : "+temps+" > "+this.getRemainingTimeInTurn());
		}
		if(this.getState()!= Camion.ETAT_EN_TRAITEMENT) {
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

	protected void decharge(double quantiteDecharge, double tempsTraitement) {
	
		//valide la demande de decharge
		//marge d'erreur de 0.0001 pour erreur numériques
		if(quantiteDecharge - 0.0001 > this.charge) {
			throw new IllegalArgumentException("Quantité déchargée trop grande : "+quantiteDecharge+" > "+this.charge);
		}
		if(tempsTraitement > this.getRemainingTimeInTurn()+0.0001) {
			throw new IllegalArgumentException("Temps de décharge trop grand : "+tempsTraitement+" > "+this.getRemainingTimeInTurn());
		}
		if(this.getState()!= Camion.ETAT_EN_TRAITEMENT) {
			throw new IllegalStateException("Peut seulement décharger en ETAT_EN_TRAITEMENT. État actuel : "+this.getState());
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

	//retourne le temps de l'iteration pour le camion
	//
	protected double getIterCurrentTime() {
		return this.iterCurrentTime;
	}

	//retourne le temps restant dans l'iteration courante
	//
	protected double getRemainingTimeInTurn() {
		return this.iterStepSize-this.iterCurrentTime;
	}



	//retourne true si l'iteration actuelle est terminee pour le camion (il a épuisé tout son temps)
	//
	protected boolean iterFinished() {
		return this.iterFinished;
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



	//met le camion en attente
	//
	protected void setAttenteState() {
		if(this.state != Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalStateException("Le camion doit etre en etat ETAT_JUSTE_ARRIVE pour etre mis en attente. Etat actuel : "+this.state);
			
		}
		int newVisitNb = this.nbVisitPerStation.get(currentStation)+1;
		
		this.nbVisitPerStation.replace(currentStation, newVisitNb);
		
		this.state = Camion.ETAT_ATTENTE;

	}

	//prepare le camion pour le debut d'une iteration
	//
	protected void setBeginIter(double stepSize) {
		this.iterCurrentTime = 0;
		this.iterStepSize = stepSize;
		this.iterFinished = false;
	}

	//fait attendre le camion jusqu'à la fin du tour
	//
	/*
	protected void waitUntilEndIter() {
		this.attend(this.getRemainingTimeInTurn());
		this.iterFinished = true;
	}
	*/

	//met le camion en traitement
	protected void setEnTraitement() {
		this.state = Camion.ETAT_EN_TRAITEMENT;
	}

	

	protected void setNumberOfRuns(int numberOfRuns) {
		this.numberOfRuns = numberOfRuns;
	}

	protected void setRockType(RockType rt) {
		this.rockType = rt;
	}

	protected void setSpeed(double speed) {
		this.speed = speed;
	}

	//met le camion en etat inactif
	protected void setStateInactif() {
		this.state = Camion.ETAT_INACTIF;
	}





}

