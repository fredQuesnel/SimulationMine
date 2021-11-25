package ca.polymtl.SimulationMine.MineSimulator;


import java.awt.image.BufferedImage;

public class SmallCamion extends Camion {

	public SmallCamion(Station station, Mine mine, BufferedImage goingEastImage) {
		super(station, mine, goingEastImage);
		type = Camion.TYPE_SMALL;
		avgSpeed = 7.5;
		stdSpeed = 0.6;
		chargeMax = 60;
		predictTimeAdjustFactor=1;
		
	}
	

}
