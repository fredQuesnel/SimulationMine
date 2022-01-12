package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.image.BufferedImage;

public class LargeCamion extends Camion {
	
	public LargeCamion(Station station, Mine mine, BufferedImage goingEastImage) {
		super(station, mine, goingEastImage);
		type = Camion.TYPE_LARGE;
		avgSpeed = 6.5;
		stdSpeed = 0.3;
		chargeMax = 100;
		predictTimeAdjustFactor=7.5/6.5;
	}

}
