package ca.polymtl.SimulationMine.MineSimulator;

public class Concentrateur extends Station {

	/** Vitesse moyenne de remplissage (en tonnes secondes)*/
	public final static double AVERAGE_DECHARGE_SPEED = 1.3;//1./2.;

	/** Écart type sur la vitesse de remplissage*/
	public final static double ECART_TYPE_DECHARGE_SPEED = 1./30.;

	//quantité de minerai (en nombre de voyages)
	private double quantityIron;

	//quantité de souffre (en nombre de voyages)
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
	 * @return Le pourcentage de fer dans le mélange
	 */
	public double getPercentIron() {

		return quantityIron/totalQuantity*100;
	}

	/**
	 * 
	 * @return Quantité de souffre dans le mélange
	 */
	public double getPercentSulfur() {

		return quantitySulfur/totalQuantity*100;
	}

	/**
	 * 
	 * @return Quantité totale de fer livré (en tonnes)
	 */
	public double getQuantityIron() {
		return quantityIron;
	}

	/**
	 * 
	 * @return Quantité total de souffre livré (en tonnes)
	 */
	public double getQuantitySulfur() {
		return quantitySulfur;
	}




	/**
	 * 
	 * @return Quantité total de minerai livré (en tones)
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
