package ca.polymtl.SimulationMine.MineSimulator;

public class RockType {
	
	//pourcentage de mineral
	private double percentOre;
	
	//pourcentage souffre
	private double percentSulfur;
	
	public RockType(double percentOre, double percentSulfur) {
		if(percentOre + percentSulfur > 100) {
			throw new IllegalArgumentException("le gisement contient plus de 100% : "+percentOre+"+"+percentSulfur);
		}
		
		this.percentOre = percentOre;
		this.percentSulfur = percentSulfur;
		
	}

	/**
	 * 
	 * @return Pourcentage de fer dans le type de roche.
	 */
	public double getPercentIron() {
		return percentOre;
	}

	/**
	 * 
	 * @return Pourcentage de souffre dans le type de roche.
	 */
	public double getPercentSulfur() {
		return percentSulfur;
	}

	public boolean isSterile() {
		if(this.getPercentIron() == 0 && this.getPercentSulfur() == 0) {
			return true;
		}
		return false;
	}
	

}
