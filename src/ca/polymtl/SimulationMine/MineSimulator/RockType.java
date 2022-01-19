package ca.polymtl.SimulationMine.MineSimulator;

/**
 * Classe representant un type de roche
 * @author Fred
 *
 */
public class RockType {
	
	/**Taux de fer (%)*/
	private double percentIron;
	
	/**Taux de soufre (%)*/
	private double percentSulfur;
	
	/**Constructeur
	 * 
	 * @param percentIron Taux de fer
	 * @param percentSulfur Taux de soufre
	 */
	public RockType(double percentIron, double percentSulfur) {
		if(percentIron + percentSulfur > 100) {
			throw new IllegalArgumentException("le gisement contient plus de 100% : "+percentIron+"+"+percentSulfur);
		}
		
		this.percentIron = percentIron;
		this.percentSulfur = percentSulfur;
		
	}

	/**
	 * 
	 * @return Pourcentage de fer dans le type de roche.
	 */
	public double getPercentIron() {
		return percentIron;
	}

	/**
	 * 
	 * @return Pourcentage de souffre dans le type de roche.
	 */
	public double getPercentSulfur() {
		return percentSulfur;
	}

	/**
	 * 
	 * @return true si il s'agit de sterile
	 */
	public boolean isSterile() {
		if(this.getPercentIron() == 0 && this.getPercentSulfur() == 0) {
			return true;
		}
		return false;
	}
	

}
