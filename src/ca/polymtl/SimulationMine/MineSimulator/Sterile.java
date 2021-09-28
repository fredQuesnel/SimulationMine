package ca.polymtl.SimulationMine.MineSimulator;

public class Sterile extends Station {
	/** Vitesse moyenne de remplissage (en tonnes/secondes)*/
	public final static double AVERAGE_DECHARGE_SPEED = 1./2.;
	
	
	/** Écart type sur la vitesse de remplissage*/
	public final static double ECART_TYPE_DECHARGE_SPEED = 1./30.;
	private double totalQuantity;
	
	public Sterile(double i, double j, String id) {
		super(i, j, id);
		this.isDecharge = true;
		totalQuantity = 0;
	}
	
	

	@Override
	public double averageTraitementSpeed() {
		return AVERAGE_DECHARGE_SPEED;
	}

	public double getTotalQuantity() {
		return totalQuantity;
	}

	private void addLoad(double charge) {
		totalQuantity+=charge;
	}
	

	@Override
	protected void resetStats() {
		totalQuantity = 0;
		
	}

	/*
	 * Met un camion en remplissage
	 */
	protected void setCamionEnTraitement(Camion camion) {
		super.setCamionEnTraitement(camion);
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
		// TODO Auto-generated method stub
		
	}



	@Override
	void computeNewChargeSpeed() {
		double lambda = 0.75;
		double speedAdjust = SimulationMine.random.nextGaussian()*Sterile.ECART_TYPE_DECHARGE_SPEED+Sterile.AVERAGE_DECHARGE_SPEED;

		this.currentChargeSpeed = lambda*this.currentChargeSpeed + (1-lambda)*speedAdjust;

		// TODO Auto-generated method stub
		
	}
	
}
