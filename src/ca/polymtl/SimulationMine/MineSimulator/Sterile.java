package ca.polymtl.SimulationMine.MineSimulator;

public class Sterile extends Station {
	private double totalQuantity;
	
	
	public Sterile(double i, double j, String id) {
		super(i, j, id);
		totalQuantity = 0;
	}
	
	protected void setCamionOnArrival(Camion camion) {
		camion.setStateIdle();
		camion.setNumberOfRuns(camion.getNumberOfRuns()+1);
		camion.setSpeed(0);
		addLoad(camion.getChargeMax());
	}

	private void addLoad(double charge) {
		totalQuantity+=charge;
	}

	public double getTotalQuantity() {
		return totalQuantity;
	}

	
}
