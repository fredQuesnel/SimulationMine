package ca.polymtl.SimulationMine.MineSimulator;

import javax.swing.JOptionPane;


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
	private static final double MAX_CIBLE_ATTENTE_PELLE_SECONDS = 240*3600;//la cible d'attente ne depasse pas 10 jour. 

	


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
	private double cibleTonnesParHeure;

	private double defaultCibleTonnesParHeure;

	private double totalQuantity;
	
	
	

	

	
	//	private double timeRemainingInTurn;


	/*
	 * Constructeur
	 */
	public Pelle(int i, int j, String id, double cibleTonnesParHeure) {
		super(i,j, id);
		
		this.totalQuantity = 0;
		this.cibleTonnesParHeure = cibleTonnesParHeure;
		this.defaultCibleTonnesParHeure = cibleTonnesParHeure;
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
		//Formule : attente = lambda/(mu (mu-lembda))
		//
		// Avec : 
		// lambda = taux arrivée des camions (camions/h)
		// mu    = taux de service de la pelle (camions/h)

		double averageChargeParCamion = averageChargeParCamion();
		double lambda = this.cibleTonnesParHeure/averageChargeParCamion;
		//en nb camions par heure
		double mu = Pelle.AVERAGE_CHARGE_SPEED/averageChargeParCamion*3600;

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
		

		double cibleAttente = 0;
		
		double averageChargeParCamion = averageChargeParCamion();

		double cibleCamionsParHeure = cibleTonnesParHeure/averageChargeParCamion;
		
		double tempsTravailParHeureEnSecondes = cibleTonnesParHeure/AVERAGE_CHARGE_SPEED;

		//si travaille plus d'une heure par heure, temps cible d'attente nul.
		if(tempsTravailParHeureEnSecondes >=3600){
			cibleAttente = 0;
		}
		else{
			cibleAttente = (3600-tempsTravailParHeureEnSecondes)/cibleCamionsParHeure;
		}
		
		if(cibleAttente > Pelle.MAX_CIBLE_ATTENTE_PELLE_SECONDS) {
			cibleAttente = Pelle.MAX_CIBLE_ATTENTE_PELLE_SECONDS;
		}
		return cibleAttente;
	}

	/**
	 * Estime la charge moyenne d'un camion visitant la pelle selon la moyenne des N derniers camions à l'avoir visité. 
	 * Si moins de N camions ont visité la pelle, prends la moyenne de tous les camions. 
	 * Si aucun camion n'a visité la pelle, retourne 0.
	 * @return
	 */
	private double averageChargeParCamion() {
		double averageChargeParCamion = 0;
		int nbDataPoint = Station.AVG_LOAD_FORMULA_N;
		if(nbDataPoint >this.arrivalLog.size()) {
			nbDataPoint = this.arrivalLog.size();
		}
		
		for(int i = this.arrivalLog.size()-nbDataPoint; i < this.arrivalLog.size(); i++) {
			averageChargeParCamion += this.arrivalLog.get(i).getChargeMax();
		}
		averageChargeParCamion = averageChargeParCamion/nbDataPoint;
		return averageChargeParCamion;
	}

	/**
	 * 
	 * @return Selon le plan, nombre de camions par heure se rendant à la pelle.
	 */
	public double getPlanNbTonnesParHeure() {

		return this.cibleTonnesParHeure;
	}

	/**
	 * 
	 * @return Type de roche à la pelle
	 */
	public RockType getRockType() {
		return rockType;
	}

	//TODO rendre indépendant du nombre de pas de simulation.
	protected void computeNewTraitementSpeed() {
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

	public void setPlan(double newValue){

		// lambda = taux arrivée des camions (camions/h)
		// mu    = taux de service de la pelle (camions/h)

		double lambda = newValue;
		//en nb camions par heure
		double mu = Pelle.AVERAGE_CHARGE_SPEED/100*3600;

		if(newValue < 0){
			JOptionPane.showMessageDialog(null, "Vous devez choisir une valeur > 0");
		}
		else{
			this.cibleTonnesParHeure = newValue;
		}

	}

	protected void setRockType(double percentOre, double percentSulfur) {
		RockType rt = new RockType(percentOre, percentSulfur);

		this.rockType = rt;

	}

	@Override
	protected void updateQteTraite(double quantite, RockType rockType) {
		this.totalQuantity += quantite;
		// Ne fait rien.
		
	}

	public double getTotalQuantity() {
		
		return totalQuantity;
	}

	
	/**
	 * reset les stats de la pelle
	 */
	protected void resetStats() {
		super.resetStats();
		this.totalQuantity = 0;
	}


}