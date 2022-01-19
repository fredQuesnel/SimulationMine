package ca.polymtl.SimulationMine.MineSimulator;

/**
 * Specialisation de la classe Station representant une sterile.
 * @author Fred
 *
 */
public class Sterile extends Station {
	/** Vitesse moyenne de remplissage (en tonnes/secondes)*/
	public final static double AVERAGE_DECHARGE_SPEED = 1./2.;
	
	
	/** Écart type sur la vitesse de remplissage*/
	public final static double ECART_TYPE_DECHARGE_SPEED = 1./30.;
	private double totalQuantity;
	
	/** constructeur
	 * 
	 * @param i position x du sterile
	 * @param j position y du sterile
	 * @param id identifiant du sterile
	 */
	public Sterile(double i, double j, String id) {
		super(i, j, id);
		this.isDecharge = true;
		totalQuantity = 0; 
	}
	
	 

	@Override
	public double averageTraitementSpeed() {
		return AVERAGE_DECHARGE_SPEED;
	}

	/**
	 * 
	 * @return Quantite totale livree au sterile
	 */
	public double getTotalQuantity() {
		return totalQuantity;
	}

	
	@Override
	protected void resetStats() {
		super.resetStats();
		totalQuantity = 0;
		
	}




	protected void setCamionOnArrival(Camion camion) {
		super.setCamionOnArrival(camion);
		camion.setNumberOfRuns(camion.getNumberOfRuns()+1);
	}



	@Override
	protected void updateQteTraite(double quantite, RockType rockType) {
		
		if(rockType.getPercentIron()>0 || rockType.getPercentSulfur()>0) {
			throw new IllegalArgumentException("On doit déverser du sterile au stérile. (s="+rockType.getPercentSulfur()+" , i="+rockType.getPercentIron()+") donné." );
		}
		this.totalQuantity+=quantite;
	}



	@Override
	void computeNewTraitementSpeed() {
		double lambda = 0.75;
		double speedAdjust = SimulationMine.random.nextGaussian()*Sterile.ECART_TYPE_DECHARGE_SPEED+Sterile.AVERAGE_DECHARGE_SPEED;

		this.currentChargeSpeed = lambda*this.currentChargeSpeed + (1-lambda)*speedAdjust;

	}
	
}
