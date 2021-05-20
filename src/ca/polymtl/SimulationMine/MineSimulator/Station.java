package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public abstract class Station {

	private Point2D.Double location;
	private String id;
	
	protected Station(double i, double j, String id) {
		this.location = new Point2D.Double(i, j);
		this.id = id;
	}

	/**
	 * Retourne la coordonnée géographique de la station.
	 * @return Point2D.Double représentant la coordonnée géographique de la station.
	 */
	public Double getLocation() {
		
		return location;
	}
	
	
	protected abstract void resetStats();
	
	protected abstract void setCamionOnArrival(Camion camion);

	/**
	 * 
	 * @return String contenant l'identifiant de la station.
	 */
	public String getId() {
		return id;
	}



}


