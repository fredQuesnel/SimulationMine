package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.image.BufferedImage;

/**Specialisation de la classe Camion representant un gros camion
 * 
 * @author Fred
 *
 */
public class LargeCamion extends Camion {
	
	/**Constructeur
	 * 
	 * @param station station
	 * @param mine mine
	 * @param goingEastImage image du camion se deplacant vers l'est
	 */
	public LargeCamion(Station station, Mine mine, BufferedImage goingEastImage) {
		super(station, mine, goingEastImage);
		type = Camion.TYPE_LARGE;
		avgSpeed = 6.5;
		stdSpeed = 0.3;
		chargeMax = 100;
		predictTimeAdjustFactor=7.5/6.5;
	}

}
