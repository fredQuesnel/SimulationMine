package ca.polymtl.SimulationMine.MineSimulator;

public class Concentrateur extends Station {

	//quantité de minerai (en nombre de voyages)
	private double quantityIron;

	//quantité de souffre (en nombre de voyages)
	private double quantitySulfur;

	private double totalQuantity;

	protected Concentrateur(double i, double j, String id) {
		super(i, j, id);

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
	}

	
	

	//ajoute un load du type de roche spécifié
	private void addLoad(RockType rockType, double numberTons) {
		quantityIron += rockType.getPercentIron()/100*numberTons;
		quantitySulfur += rockType.getPercentSulfur()/100*numberTons;
		totalQuantity += numberTons;
	}


	protected void setCamionOnArrival(Camion camion) {
		camion.setStateIdle();
		camion.setNumberOfRuns(camion.getNumberOfRuns()+1);
		camion.setSpeed(0);
		addLoad(camion.getRockType(), camion.getChargeMax());
	}

	/**
	 * 
	 * @return Quantité total de minerai livré (en tones)
	 */
	public double getTotalQuantity() {
		return totalQuantity;
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




	@Override
	protected void resetStats() {

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
		
	}
}
