package ca.polymtl.SimulationMine.MineGui;

import java.awt.Event;
import java.awt.event.AWTEventListener;

import ca.polymtl.SimulationMine.MineSimulator.Pelle;

public interface GuiListener extends AWTEventListener {
	
	
	//quand on clique sur le bouton "charger"
	public void chargerButtonClicked(GuiEvent evt);
		
	//quand le slider du bouton météo est changé
	public void meteoSliderChanged(double meteoFactor);
	
	//quand une info relative a la formule de temps de calcul est changé
	public void predictTimeChanged(GuiEvent evt);
	
	//quand on charge une nouvelle mide
	public void chargeMineConfirmed(GuiEvent evt);
	
	//quand le panel de mine est cliqué 
	//fournis la position relative de la souris (fraction du panel)
	public void minePanelClicked(double fracX, double fracY);

	public void predictFunctionChanged(int newPredictFunctionIndex);

	public void rhoValueChanged(double rhoValue);

	public void numberSampleChanged(int currentNValue);

	public void newSimulationRequested(int exempleNb, int numberOfCamions, double tempsSimulationSeconds);

	public void automaticCompletionRequested();

	public void stopOnAssignStateChanged(boolean selected);

	public void simulationSpeedChanged(int speed);

	public void playButtonPressed();

	public void pauseButtonPressed();

	public void resetSimulationRequested();

	public void scoreFunctionChanged(String scoreFunction);

	public void planPelleChanged(Pelle p, double newValue);
		

}
