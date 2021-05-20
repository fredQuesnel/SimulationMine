package ca.polymtl.SimulationMine.MineSimulator;

public class Concentrateur extends Station {

	//quantit� de minerai (en nombre de voyages)
	private double quantityIron;

	//quantit� de souffre (en nombre de voyages)
	private double quantitySulfur;

	private double totalQuantity;

	protected Concentrateur(double i, double j, String id) {
		super(i, j, id);

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
	}

	
	

	//ajoute un load du type de roche sp�cifi�
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
	 * @return Quantit� total de minerai livr� (en tones)
	 */
	public double getTotalQuantity() {
		return totalQuantity;
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




	@Override
	protected void resetStats() {

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
		
	}
}
