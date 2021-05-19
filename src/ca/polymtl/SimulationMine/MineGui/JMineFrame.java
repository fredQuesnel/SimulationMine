package ca.polymtl.SimulationMine.MineGui;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulationListener;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulator;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;

public class JMineFrame extends JFrame implements MineSimulationListener{

	private JMinePanel minePanel;
	private JControlPanel controlPanel;
	private ArrayList<GuiListener> listenerList;

	private Mine mine;


	public JMineFrame(Mine mine) {
		
		this.mine = mine;

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




	public Mine getMine() {
		return mine;
	}




	public void setMine(Mine mine) {
		this.mine = mine;
	}




	public JMinePanel getMinePanel() {
		return this.minePanel;
	}

	public JControlPanel getControlPanel() {
		return this.controlPanel;
	}



	//Ajoute un observer au GUI
	public void addObserver(GuiListener mineSimulator) {
		listenerList.add(mineSimulator);
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




	@Override
	public void eventDispatched(AWTEvent arg0) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void mineResetted(MineSimulator mineSimulator) {
		this.mine = mineSimulator.getMine();
		this.minePanel.mineResetted();
		
	}




	@Override
	public void minePaused(Mine mine) {
		this.getControlPanel().setPauseMode();
	}




	@Override
	public void minUnpaused(Mine mine) {
		this.getControlPanel().setPlayMode();
	}

	public void notifyListenersPlanPelleChanged(Pelle p, double newValue){
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).planPelleChanged(p, newValue);
		}
	}



	@Override
	public void mineUpdated(Mine mine) {
		System.out.println("dansMineFrame");
		this.minePanel.updateMine();
		
	}




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
	public void automaticCompleteFinished() {
		this.minePanel.automaticCompleteFinished();
		
	}




	public void notifyListenersPredictFunctionChanged(int newPredictFunctionIndex) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).predictFunctionChanged(newPredictFunctionIndex);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersRhoChanged(double rhoValue) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).rhoValueChanged(rhoValue);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersNumberSampleChanged(int currentNValue) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).numberSampleChanged(currentNValue);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersNewSimulationRequested(ExampleId selectedId, int numberOfSmallCamions,
			int numberOfLargeCamions, double tempsSimulationSeconds) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).newSimulationRequested(selectedId, numberOfSmallCamions, numberOfLargeCamions, tempsSimulationSeconds);
			//SimulationMine.mine.setMeteoFactor(meteoFactor);
		}
	}




	public void notifyListenersAutomaticCompletionRequested() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).automaticCompletionRequested();
		}
	}




	public void notifyListenersStopOnAssignStateChanged(boolean selected) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).stopOnAssignStateChanged(selected);
		}
	}




	public void notifyListenersSimulationSpeedChanged(int speed) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).simulationSpeedChanged(speed);
		}
	}

 


	public void notifyListenersPlayButtonPressed() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).playButtonPressed();
		}
	}




	public void notifyListenersPauseButtonPressed() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).pauseButtonPressed();
		}
	}




	public void notifyListenersResetSimulationRequested() {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).resetSimulationRequested();
		}	
	}


	

	public void notifyListenersScoreFunctionChanged(String scoreFunction) {
		for(int i = 0 ; i < listenerList.size(); i++) {
			listenerList.get(i).scoreFunctionChanged(scoreFunction);
		}	
		
	}

	@Override
	public void camionJustArrived(Camion camion, double time) {
		
	}

}
