package ca.polymtl.SimulationMine.MineGui;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulationListener;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulator;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;

/**
 * Frame contenant la fenêtre principale de l'interface graphique. Cette classe gère également la communication avec
 * la classe MineSimulator, la classe qui contrôle la simulation.
 * 
 * @author Frédéric Quesnel
 *
 */

public class JMineFrame extends JFrame implements MineSimulationListener{

	private static final long serialVersionUID = 1L;
	
	/** Panel affichant l'animation de la mine.	 */
	private JMinePanel minePanel;
	/** Panel avec les éléments de contrôle de la mine*/
	private JControlPanel controlPanel;
	
	/** Liste des listeners de cette classe*/
	private ArrayList<GuiListener> listenerList;

	/** Objet controlant la simulation*/
	private MineSimulator mineSimulator;

	/**Donnees de configuration*/
	private Config config;


	/** Constructeur
	 * 
	 * Initialise les Panels
	 * @param mineSimulator simulateur de mine
	 * @param config objet config
	 */
	public JMineFrame(MineSimulator mineSimulator, Config config) {
		
		this.mineSimulator = mineSimulator;
		this.config = config;

		listenerList = new ArrayList<GuiListener>();
		JPanel mainPanel = new JPanel();

		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		this.add(mainPanel);

		
		this.setMinimumSize(new Dimension(900, 600));
		 
		this.minePanel = new JMinePanel(this);


		minePanel.repaint();
		mainPanel.add(minePanel);

				



		this.controlPanel = new JControlPanel(this);
		int height = 150;
		controlPanel.setMinimumSize(new Dimension(0, height));
		controlPanel.setPreferredSize(new Dimension(100000, height));
		controlPanel.setMaximumSize(new Dimension(100000, height));
		controlPanel.setBackground(Color.white);

		mainPanel.add(controlPanel);

	}




	/**
	 * Ajoute un observer
	 * @param mineSimulator observer
	 */
	public void addObserver(GuiListener mineSimulator) {
		listenerList.add(mineSimulator);
	}



	/**
	 * Lorsque le controleur de mine indique que la complétion automatique de la simulation est terminée
	 */
	@Override
	public void automaticCompleteFinished() {
		this.minePanel.automaticCompleteFinished();
		
	}



	/**
	 * Lorsque le controleur de mine indique que la complétion automatique de la simulation a débutée
	 */
	@Override
	public void automaticCompleteStarted() {
		this.minePanel.automaticCompleteStarted();
	}

	@Override
	public void automaticCompleteUpdated(double fractionComplete) {
		this.minePanel.automaticCompleteUpdated(fractionComplete);
	}

	@Override
	public void camionJustArrived(Camion camion, double time) {
		
	}


	@Override
	public void eventDispatched(AWTEvent arg0) {
	}

	/**Retourne le control panel
	 * 
	 * @return control panel
	 */
	public JControlPanel getControlPanel() {
		return this.controlPanel;
	}

	/**Retourne la mine
	 * 
	 * @return Mine
	 */
	public Mine getMine() {
		return mineSimulator.getMine();
	}


	/**Retourne le panel de mine
	 * 
	 * @return panel de mine
	 */
	public JMinePanel getMinePanel() {
		return this.minePanel;
	}


	/**Retourne l'objet MineSimulator
	 * 
	 * @return objet MineSimulator
	 */
	public MineSimulator getMineSimulator() {
		
		return this.mineSimulator;
	}


	@Override
	public void minePaused(Mine mine) {
		this.getControlPanel().setPauseMode();
	}

	@Override
	public void mineResetted(MineSimulator mineSimulator) {
		this.minePanel.mineResetted();
		
	}



	@Override
	public void mineUpdated(Mine mine) {
		this.minePanel.updateMine();
		
	}




	@Override
	public void minUnpaused(Mine mine) {
		this.getControlPanel().setPlayMode();
	}



	/**Avertis les listeners que l'usager vient de demander la completion automatique
	 * 
	 */
	public void notifyListenersAutomaticCompletionRequested() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).automaticCompletionRequested();
		}
	}



	/** Avertis les listeners que l'usager vient de changer la meteo
	 * 
	 * @param meteoFactor Nouveau facteur meteo
	 */
	public void notifyListenersMeteoChanged(double meteoFactor) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).meteoSliderChanged(meteoFactor);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
		
	}

	/**Avertis les listeners que l'usager vient de cliquer sur le panel de mine.
	 * 
	 * @param fracX Position X (relatif) du clic 
	 * @param fracY Position Y (relatif) du clic
	 */
	public void notifyListenersMinePanelClicked(double fracX, double fracY){
		for(int i = 0 ; i < listenerList.size(); i++) {
			System.out.println("dans le frame : clicked");
			listenerList.get(i).minePanelClicked(fracX, fracY);
		}
	}



	/**Avertis  les listeners que l'usager souhaite charger une nouvelle mine
	 * 
	 * @param selectedId Id de la mine selectionnee
	 * @param numberOfSmallCamions Nombre de petits camions
	 * @param numberOfLargeCamions Nombre de gros camions
	 * @param tempsSimulationSeconds Temps de simulation
	 */
	public void notifyListenersNewSimulationRequested(ExampleId selectedId, int numberOfSmallCamions,
			int numberOfLargeCamions, double tempsSimulationSeconds) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).newSimulationRequested(selectedId, numberOfSmallCamions, numberOfLargeCamions, tempsSimulationSeconds);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}



	/**Avertis les listeners que l'usager vient de changer la valeur de N dans la fonction de prediction
	 * 
	 * @param currentNValue Nouvelle valeur de N
	 */
	public void notifyListenersNumberSampleChanged(int currentNValue) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).numberSampleChanged(currentNValue);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}



	/**Avertis les listeners que l'usager vient de cliquer sur le bouton pause*/
	public void notifyListenersPauseButtonPressed() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).pauseButtonPressed();
		}
	}


	/**Avertis les listeners que l'usager vient de changer le plan d'une pelle
	 * 
	 * @param p pelle
	 * @param newValue Nouvelle valeur du plan pour la pelle
	 */
	public void notifyListenersPlanPelleChanged(Pelle p, double newValue){
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).planPelleChanged(p, newValue);
		}
	}

	/**Avertis les listeners que l'usager vient de cliquer sur le bouton play
	 * 
	 */
	public void notifyListenersPlayButtonPressed() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).playButtonPressed();
		}
	}

	/**Avertis les listeners que l'usager vient de modifier la formule de prediction
	 * 
	 * @param newPredictFunctionIndex Id de la nouvelle formule de prediction
	 */
	public void notifyListenersPredictFunctionChanged(int newPredictFunctionIndex) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).predictFunctionChanged(newPredictFunctionIndex);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}

 
	/**Avertis les listeners que l'usager vient de cliquer sur le bouton reset
	 * 
	 */
	public void notifyListenersResetSimulationRequested() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).resetSimulationRequested();
		}	
	}



	/**Avertis les listeners que l'usager vient de changer la valeur de lambda
	 * 
	 * @param lambdaValue : Nouvelle valeur de lambda
	 */
	public void notifyListenersLambdaChanged(double lambdaValue) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).lambdaValueChanged(lambdaValue);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}



	/**Avertis les listeners que l'usager vient de changer la fonction de score pour les petits camions.
	 * 
	 * @param scoreFunction Nouvelle fonction de score pour les petits camions.
	 */
	public void notifyListenersScoreFunctionSmallCamionsChanged(String scoreFunction) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).scoreFunctionSmallCamionsChanged(scoreFunction);
		}	
		
	}


	
	/**Avertis les listeners que l'usager vient de changer la fonction de score pour les gros camions.
	 * 
	 * @param scoreFunction Nouvelle fonction de score pour les gros camions.
	 */
	public void notifyListenersScoreFunctionLargeCamionsChanged(String scoreFunction) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).scoreFunctionLargeCamionsChanged(scoreFunction);
		}	
	}



	/**Avertis les listeners que l'usager vient de changer la vitesse de simulation
	 * 
	 * @param speed Nouvelle vitesse de simulation
	 */
	public void notifyListenersSimulationSpeedChanged(int speed) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).simulationSpeedChanged(speed);
		}
	}

	/**Avertis les listeners que l'usager vient de cliquer sur le bouton "pause a chaque fin de voyage".
	 * 
	 * @param selected true si la fonctione est activée, false sinon.
	 */
	public void notifyListenersStopOnAssignStateChanged(boolean selected) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).stopOnAssignStateChanged(selected);
		}
	}



	/**Retourne l'objet config
	 * 
	 * @return Objet config
	 */
	public Config getConfig() {
		return config;
	}




}
