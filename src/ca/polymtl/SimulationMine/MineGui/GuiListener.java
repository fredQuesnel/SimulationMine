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
	 * Indique que le bouton "charger" a �t� cliqu�.
	 * @param evt : evenement
	 */
	public void chargerButtonClicked(GuiEvent evt);
		
	//quand le slider du bouton m�t�o est chang�
	/**
	 * Indique que la m�t�o a �t� chang�e.
	 * 
	 * @param meteoFactor : nouvelle valeur du facteur m�t�o.
	 */
	public void meteoSliderChanged(double meteoFactor);
	
	/** Une info relative a la formule de temps de calcul est chang�
	 * @param evt
	 */
	public void predictTimeChanged(GuiEvent evt);
	

	/** On charge une nouvelle mine
	 * @param evt
	 */
	public void chargeMineConfirmed(GuiEvent evt);
	
	/**Le panel de mine est cliqu� 
	 * fournis la position relative de la souris (fraction du panel)
	 * @param fracX : coordonn�e horizontale relative du curseur.
	 * @param fracY : coordonn�e vertiale relative du curseur.
	 */
	public void minePanelClicked(double fracX, double fracY);

	/** Une nouvelle fonnction de pr�diction a �t� choisie.
	 * @param newPredictFunctionIndex : Nouvel index de fonction de pr�diction.
	 */
	public void predictFunctionChanged(int newPredictFunctionIndex);

	/** Le champs pour la valeur du param�tre lambda (pour fonction de pr�diction) a �t� chang�.
	 * @param lambdaValue : nouvelle valeur du param�tre lambda
	 */
	public void lambdaValueChanged(double lambdaValue);

	/** Le champs pour la valeur du param�tre n (dans la fonction de pr�diction) a �t� chang�.
	 * @param currentNValue : nouvelle valeud du param�tre n.
	 */
	public void numberSampleChanged(int currentNValue);

	/**Une nouvelle simulation est demand�e
	 * 
	 * @param selectedId : Id de la configuration de la mine.
	 * @param numberOfSmallCamions : Nombre de petits camions.
	 * @param numberOfLargeCamions : Nombre de gros camions.
	 * @param tempsSimulationSeconds : Dur�e de la simulation (secondes).
	 */
	public void newSimulationRequested(ExampleId selectedId, int numberOfSmallCamions, int numberOfLargeCamions, double tempsSimulationSeconds);

	/**
	 *  On demande la completion automatique de la simulation.
	 */
	public void automaticCompletionRequested();

	/** Indique qu'on viens de cocher/d�cocher la case "pause � chaque fin de voyage".
	 * 
	 * @param selected : indique si la case est coch�e
	 */
	public void stopOnAssignStateChanged(boolean selected);

	/**
	 * On change la vitesse de simulation
	 * 
	 * @param speed : Nouvelle vitesse
	 */
	public void simulationSpeedChanged(int speed);

	/**
	 * On vient d'appuyer sur le bouton play.
	 */
	public void playButtonPressed();

	/**
	 * On vient d'appuyer sur le bouton pause.
	 */
	public void pauseButtonPressed();

	/**
	 * On vient d'appuyer sur le bouton reset.
	 */
	public void resetSimulationRequested();

	/**
	 * La fonction de score est modifi�e.
	 * @param scoreFunction : nouvelle fonction de score.
	 */
	public void scoreFunctionChanged(String scoreFunction);

	/**
	 * Le plan d'une pelle a �t� modifi�.
	 * 
	 * @param p : pelle.
	 * @param newValue : nouvelle valeur. 
	 */
	public void planPelleChanged(Pelle p, double newValue);
		

}
