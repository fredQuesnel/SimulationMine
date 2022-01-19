package ca.polymtl.SimulationMine.MineSimulator;


import java.awt.image.BufferedImage;

/**Spécialisation de la classe Camion représentant un petit camion
 * 
 * @author Fred
 *
 */
public class SmallCamion extends Camion {

	/**Constructeur
	 * 
	 * @param station Emplacement de depart du camion
	 * @param mine Mine 
	 * @param goingEastImage image du camion se deplacant vers l'est
	 */
	public SmallCamion(Station station, Mine mine, BufferedImage goingEastImage) {
		super(station, mine, goingEastImage);
		type = Camion.TYPE_SMALL;
		avgSpeed = 7.5;
		stdSpeed = 0.6;
		chargeMax = 60;
		predictTimeAdjustFactor=1;
		
	}
	

}
