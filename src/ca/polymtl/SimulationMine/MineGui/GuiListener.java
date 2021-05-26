package ca.polymtl.SimulationMine.MineGui;

import java.awt.event.AWTEventListener;

import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;

/**
 * 
 * @author Fred
 *
 * Listener pour l'interface graphique.
 */

public interface GuiListener extends AWTEventListener {
	
	
	//quand on clique sur le bouton "charger"
	/**
	 * Indique que le bouton "charger" a été cliqué.
	 * @param evt : evenement
	 */
	public void chargerButtonClicked(GuiEvent evt);
		
	//quand le slider du bouton météo est changé
	/**
	 * Indique que la météo a été changée.
	 * 
	 * @param meteoFactor : nouvelle valeur du facteur météo.
	 */
	public void meteoSliderChanged(double meteoFactor);
	
	/** Une info relative a la formule de temps de calcul est changé
	 * @param evt
	 */
	public void predictTimeChanged(GuiEvent evt);
	

	/** On charge une nouvelle mine
	 * @param evt
	 */
	public void chargeMineConfirmed(GuiEvent evt);
	
	/**Le panel de mine est cliqué 
	 * fournis la position relative de la souris (fraction du panel)
	 * @param fracX : coordonnée horizontale relative du curseur.
	 * @param fracY : coordonnée vertiale relative du curseur.
	 */
	public void minePanelClicked(double fracX, double fracY);

	/** Une nouvelle fonnction de prédiction a été choisie.
	 * @param newPredictFunctionIndex
	 */
	public void predictFunctionChanged(int newPredictFunctionIndex);

	/**
	 * @param rhoValue
	 */
	public void rhoValueChanged(double rhoValue);

	/**
	 * @param currentNValue
	 */
	public void numberSampleChanged(int currentNValue);

	/**
	 * @param selectedId
	 * @param numberOfSmallCamions
	 * @param numberOfLargeCamions
	 * @param tempsSimulationSeconds
	 */
	public void newSimulationRequested(ExampleId selectedId, int numberOfSmallCamions, int numberOfLargeCamions, double tempsSimulationSeconds);

	/**
	 * 
	 */
	public void automaticCompletionRequested();

	/**
	 * @param selected
	 */
	public void stopOnAssignStateChanged(boolean selected);

	/**
	 * @param speed
	 */
	public void simulationSpeedChanged(int speed);

	/**
	 * 
	 */
	public void playButtonPressed();

	/**
	 * 
	 */
	public void pauseButtonPressed();

	/**
	 * 
	 */
	public void resetSimulationRequested();

	/**
	 * @param scoreFunction
	 */
	public void scoreFunctionChanged(String scoreFunction);

	/**
	 * @param p
	 * @param newValue
	 */
	public void planPelleChanged(Pelle p, double newValue);
		

}
