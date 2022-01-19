package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.event.AWTEventListener;
/**
 * Implementation du patron listener pour le simulateur de mine.
 * @author Fred
 *
 */
public interface MineSimulationListener extends AWTEventListener {
	
	/**Lorsque la completion automatique est terminee
	 * 
	 */
	public void automaticCompleteFinished();
	
	/**Lorsque la completion automatique debute
	 * 
	 */
	public void automaticCompleteStarted();
	
	/**Lorsqu'on update la completion automatique
	 * 
	 * @param fractionComplete fraction de la simulation completee
	 */
	public void automaticCompleteUpdated(double fractionComplete);

	/**
	 * Lorsqu'un camion vient d'arriver a sa destination
	 * @param camion camion venant d'arriver
	 * @param time temps
	 */
	public void camionJustArrived(Camion camion, double time);
	
	/**mine mise en pause
	 * 
	 * @param mine mine
	 */
	public void minePaused(Mine mine);

	/**lorsque la mine est reseté
	 * 
	 * @param mineSimulator simulateur de mine
	 */
	public void mineResetted(MineSimulator mineSimulator);

	/**mine updatée
	 * 
	 * @param mine mine
	 */
	public void mineUpdated(Mine mine);

	/**mine sortie du mode pause
	 * 
	 * @param mine mine
	 */
	public void minUnpaused(Mine mine);

}
