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
	
	
	/**
	 *  On demande la completion automatique de la simulation.
	 */
	public void automaticCompletionRequested();
		
	/** On charge une nouvelle mine
	 * @param evt
	 */
	public void chargeMineConfirmed(GuiEvent evt);
	
	//quand on clique sur le bouton "charger"
	/**
	 * Indique que le bouton "charger" a été cliqué.
	 * @param evt : evenement
	 */
	public void chargerButtonClicked(GuiEvent evt);
	

	/** Le champs pour la valeur du paramètre lambda (pour fonction de prédiction) a été changé.
	 * @param lambdaValue : nouvelle valeur du paramètre lambda
	 */
	public void lambdaValueChanged(double lambdaValue);
	
	//quand le slider du bouton météo est changé
	/**
	 * Indique que la météo a été changée.
	 * 
	 * @param meteoFactor : nouvelle valeur du facteur météo.
	 */
	public void meteoSliderChanged(double meteoFactor);

	/**Le panel de mine est cliqué 
	 * fournis la position relative de la souris (fraction du panel)
	 * @param fracX : coordonnée horizontale relative du curseur.
	 * @param fracY : coordonnée vertiale relative du curseur.
	 */
	public void minePanelClicked(double fracX, double fracY);

	/**Une nouvelle simulation est demandée
	 * 
	 * @param selectedId : Id de la configuration de la mine.
	 * @param numberOfSmallCamions : Nombre de petits camions.
	 * @param numberOfLargeCamions : Nombre de gros camions.
	 * @param tempsSimulationSeconds : Durée de la simulation (secondes).
	 */
	public void newSimulationRequested(ExampleId selectedId, int numberOfSmallCamions, int numberOfLargeCamions, double tempsSimulationSeconds);

	/** Le champs pour la valeur du paramètre n (dans la fonction de prédiction) a été changé.
	 * @param currentNValue : nouvelle valeud du paramètre n.
	 */
	public void numberSampleChanged(int currentNValue);

	/**
	 * On vient d'appuyer sur le bouton pause.
	 */
	public void pauseButtonPressed();

	/**
	 * Le plan d'une pelle a été modifié.
	 * 
	 * @param p : pelle.
	 * @param newValue : nouvelle valeur. 
	 */
	public void planPelleChanged(Pelle p, double newValue);

	/**
	 * On vient d'appuyer sur le bouton play.
	 */
	public void playButtonPressed();

	/** Une nouvelle fonnction de prédiction a été choisie.
	 * @param newPredictFunctionIndex : Nouvel index de fonction de prédiction.
	 */
	public void predictFunctionChanged(int newPredictFunctionIndex);

	/** Une info relative a la formule de temps de calcul est changé
	 * @param evt
	 */
	public void predictTimeChanged(GuiEvent evt);

	/**
	 * On vient d'appuyer sur le bouton reset.
	 */
	public void resetSimulationRequested();

	/**
	 * La fonction de score est modifiée.
	 * @param scoreFunction : nouvelle fonction de score.
	 */
	public void scoreFunctionChanged(String scoreFunction);

	/**
	 * On change la vitesse de simulation
	 * 
	 * @param speed : Nouvelle vitesse
	 */
	public void simulationSpeedChanged(int speed);

	/** Indique qu'on viens de cocher/décocher la case "pause à chaque fin de voyage".
	 * 
	 * @param selected : indique si la case est cochée
	 */
	public void stopOnAssignStateChanged(boolean selected);
		

}
