package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.event.AWTEventListener;

public interface MineSimulationListener extends AWTEventListener {
	
	//lorsque la mine est reseté
	public void mineResetted(MineSimulator mineSimulator);
	
	//mine mise en pause
	public void minePaused(Mine mine);
	
	//mine sortie du mode pause
	public void minUnpaused(Mine mine);

	//mine updatée
	public void mineUpdated(Mine mine);
	
	public void automaticCompleteStarted();

	public void automaticCompleteUpdated(double fractionComplete);

	public void automaticCompleteFinished();

	public void camionJustArrived(Camion camion, double time);

}
