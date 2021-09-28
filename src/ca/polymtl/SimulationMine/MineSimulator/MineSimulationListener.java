package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.event.AWTEventListener;

public interface MineSimulationListener extends AWTEventListener {
	
	public void automaticCompleteFinished();
	
	public void automaticCompleteStarted();
	
	public void automaticCompleteUpdated(double fractionComplete);

	public void camionJustArrived(Camion camion, double time);
	
	//mine mise en pause
	public void minePaused(Mine mine);

	//lorsque la mine est reseté
	public void mineResetted(MineSimulator mineSimulator);

	//mine updatée
	public void mineUpdated(Mine mine);

	//mine sortie du mode pause
	public void minUnpaused(Mine mine);

}
