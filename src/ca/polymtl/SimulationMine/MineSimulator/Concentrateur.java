package ca.polymtl.SimulationMine.MineSimulator;

public class Concentrateur extends Station {

	/** Vitesse moyenne de remplissage (en tonnes secondes)*/
	public final static double AVERAGE_DECHARGE_SPEED = 1.3;//1./2.;

	/** �cart type sur la vitesse de remplissage*/
	public final static double ECART_TYPE_DECHARGE_SPEED = 1./30.;

	//quantit� de minerai (en nombre de voyages)
	private double quantityIron;

	//quantit� de souffre (en nombre de voyages)
	private double quantitySulfur;
	private double totalQuantity;
	
	protected Concentrateur(double i, double j, String id) {
		super(i, j, id);
		this.isDecharge = true;

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
	}



	@Override
	public double averageTraitementSpeed() {
		return AVERAGE_DECHARGE_SPEED;
	}

	/**
	 * 
	 * @return Le pourcentage de fer dans le m�lange
	 */
	public double getPercentIron() {

		return quantityIron/totalQuantity*100;
	}

	/**
	 * 
	 * @return Quantit� de souffre dans le m�lange
	 */
	public double getPercentSulfur() {

		return quantitySulfur/totalQuantity*100;
	}

	/**
	 * 
	 * @return Quantit� totale de fer livr� (en tonnes)
	 */
	public double getQuantityIron() {
		return quantityIron;
	}

	/**
	 * 
	 * @return Quantit� total de souffre livr� (en tonnes)
	 */
	public double getQuantitySulfur() {
		return quantitySulfur;
	}




	/**
	 * 
	 * @return Quantit� total de minerai livr� (en tones)
	 */
	public double getTotalQuantity() {
		return totalQuantity;
	}


	@Override
	protected void resetStats() {
		super.resetStats();
		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
		
	}


	protected void setCamionOnArrival(Camion camion) {
		super.setCamionOnArrival(camion);
		camion.setNumberOfRuns(camion.getNumberOfRuns()+1);
	}

	@Override
	protected void updateQteTraite(double quantite, RockType rockType) {
		quantityIron += rockType.getPercentIron()/100.*quantite;
		quantitySulfur += rockType.getPercentSulfur()/100.*quantite;
		totalQuantity += quantite;
		
		
	}



	@Override
	void computeNewTraitementSpeed() {
		double lambda = 0.75;
		double speedAdjust = SimulationMine.random.nextGaussian()*Concentrateur.ECART_TYPE_DECHARGE_SPEED+Concentrateur.AVERAGE_DECHARGE_SPEED;

		this.currentChargeSpeed = lambda*this.currentChargeSpeed + (1-lambda)*speedAdjust;

		// TODO Auto-generated method stub
		
	}
	
}
