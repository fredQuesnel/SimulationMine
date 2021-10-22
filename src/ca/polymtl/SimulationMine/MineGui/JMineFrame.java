package ca.polymtl.SimulationMine.MineGui;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
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


	/** Constructeur
	 * 
	 * Initialise les Panels
	 */
	public JMineFrame(MineSimulator mineSimulator) {
		
		this.mineSimulator = mineSimulator;

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
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
	}

	public JControlPanel getControlPanel() {
		return this.controlPanel;
	}




	public Mine getMine() {
		return mineSimulator.getMine();
	}




	public JMinePanel getMinePanel() {
		return this.minePanel;
	}




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
		System.out.println("dansMineFrame");
		this.minePanel.updateMine();
		
	}




	@Override
	public void minUnpaused(Mine mine) {
		this.getControlPanel().setPlayMode();
	}




	public void notifyListenersAutomaticCompletionRequested() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).automaticCompletionRequested();
		}
	}




	public void notifyListenersMeteoChanged(double meteoFactor) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).meteoSliderChanged(meteoFactor);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
		
	}




	public void notifyListenersMinePanelClicked(double fracX, double fracY){
		for(int i = 0 ; i < listenerList.size(); i++) {
			System.out.println("dans le frame : clicked");
			listenerList.get(i).minePanelClicked(fracX, fracY);
		}
		
	}




	public void notifyListenersNewSimulationRequested(ExampleId selectedId, int numberOfSmallCamions,
			int numberOfLargeCamions, double tempsSimulationSeconds) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).newSimulationRequested(selectedId, numberOfSmallCamions, numberOfLargeCamions, tempsSimulationSeconds);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersNumberSampleChanged(int currentNValue) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).numberSampleChanged(currentNValue);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersPauseButtonPressed() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).pauseButtonPressed();
		}
	}




	public void notifyListenersPlanPelleChanged(Pelle p, double newValue){
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).planPelleChanged(p, newValue);
		}
	}




	public void notifyListenersPlayButtonPressed() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).playButtonPressed();
		}
	}




	public void notifyListenersPredictFunctionChanged(int newPredictFunctionIndex) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).predictFunctionChanged(newPredictFunctionIndex);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}

 


	public void notifyListenersResetSimulationRequested() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).resetSimulationRequested();
		}	
	}




	public void notifyListenersRhoChanged(double rhoValue) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).lambdaValueChanged(rhoValue);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersScoreFunctionChanged(String scoreFunction) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).scoreFunctionChanged(scoreFunction);
		}	
		
	}


	

	public void notifyListenersSimulationSpeedChanged(int speed) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).simulationSpeedChanged(speed);
		}
	}

	public void notifyListenersStopOnAssignStateChanged(boolean selected) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).stopOnAssignStateChanged(selected);
		}
	}




}
