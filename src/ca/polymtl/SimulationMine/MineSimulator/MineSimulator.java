package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.Timer;

import ca.polymtl.SimulationMine.MineGui.GuiEvent;
import ca.polymtl.SimulationMine.MineGui.GuiListener;
import ca.polymtl.SimulationMine.MineGui.SommaireFrame;
import ca.polymtl.SimulationMine.decisionMaker.CustomDecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.DecisionMaker;

//classe repr�sentant une simulation de mine. Elle gere la simulation en tant que telle et 
//l'interfacage avec le gui
//
public class MineSimulator implements GuiListener {

	//engin de d�cision
	public CustomDecisionMaker decisionMaker;

	//mine simul�e
	private Mine mine;

	//objets qui �coutent la simulation
	private ArrayList<MineSimulationListener> listeners;

	private SommaireFrame sommaireFrame;
	
	/*
	 * �tat de la simulation
	 */
	//nombre de pas de simulation effectu�s
	private int stepCounter;	
	//nombre max de pas pour la simulation actuelle
	private int max_steps;
	//nombre de pas par iteration (determine la vitesse de simulation)
	private int nbIterPerStep;
	//timer en charge d'updater la simulation � intervalle r�gulier
	private Timer timer;
	//si true, stop � chaque fois qu'un camion arrive au st�rile/au concentrateur	
	private boolean stopOnAssign;


	//constructeur
	public MineSimulator() {
		//cree le module en charge de l'IA des camions


		//instantie la liste des listeners
		listeners = new ArrayList<MineSimulationListener>();

		//Cr�� la mine et l'initialise
		//
		mine = new Mine();
		mine.init(Mine.EXEMPLE2, 20);

		//Cr�� l'engin de d�cision
		//
		decisionMaker = new CustomDecisionMaker(mine);

		//warmup la mine
		//
		warmup();

		// Set les parametres de la simulation
		//
		//nombre de pas
		this.stepCounter = 0;
		//nombre max de pas
		this.max_steps = (int) (SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/Mine.TIME_INCREMENT);

		//par defaut, on ne stop pas a chaque fois qu'un camion arrive au st�rile/concentrateur
		this.stopOnAssign = false;

		//ajoute le timer de la simulation
		//
		this.timer = createTimer();
		//delai par d�faut (millisecondes) entre 2 it�rations
		//
		this.setNbIterPerStep(26);

	}

	/*
	 * Ajoute un listener � la liste des listeners
	 */
	public void addListener(MineSimulationListener listener){
		listeners.add(listener);
	}

	public Mine getMine() {
		return this.mine;
	}

	public Timer getTimer() {
		return timer;
	}

	public void initStepCounter() {
		this.stepCounter = 0;
	}

	public int getTempsSimulationSeconds() {
		return (int) (this.max_steps*Mine.TIME_INCREMENT);
	}

	/*
	 * reset la mine selon un exemple donne
	 */
	public void chargeMine(int ExempleNb, int nbCamions, double temps) {
	
		setPauseMode();
	
		//reinitialise la nouvelle mine
		//
		mine.init(ExempleNb, nbCamions);
		warmup();
	
		notifyListenersMineReset();
		this.stepCounter = 0;
		this.max_steps = (int) (temps/Mine.TIME_INCREMENT);
	}

	/*
	 * Compl�te automatiquement la simulation (sans attendre les evenements du timer)
	 */
	public void completerSimulation() {
		//avertis les listeners
		notifyListenersAutomaticCompleteStarted();

		setPauseMode();

		//compte combien de pas repr�sentent 1% de compl�tion
		//
		int counterStart = stepCounter;
		int stepsLeft = max_steps-counterStart;
		int onePercent = (stepsLeft)/100;
		
		//effectue les pas
		//
		while(stepCounter < max_steps) {
			step();
			//� chaque pourcentage, avertis les listeners
			//
			if((max_steps-stepCounter)%onePercent == 0) {
				int progress = max_steps-stepsLeft+stepCounter;
				notifyListenersAutomaticCompleteUpdated(1.0*progress/stepsLeft*100);
			}
			stepCounter++;
		}
		//avertis les listeners que la simulation est completee
		notifyListenersAutomaticCompleteFinished();
		createSommaireFrame();
	}

	private void createSommaireFrame() {
		this.sommaireFrame = new SommaireFrame(this);
		
	}

	/*
	 * Cree le timer de la simulation
	 * Lorsque le timer s'active, effectue un nombre de pas � la simulation
	 */
	private Timer createTimer() {

		return  new Timer(40, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				//stop la simulation si on a atteint le max
				//
				boolean stop = false;
				if(stepCounter >= max_steps) {

					stop = true;
					setPauseMode();
				}
				//effectue des pas jusqu'� ce que soit : 
				//	1) on ait effectu� nbIterPerStep pas
				//	2) on ait termin� la simulation
				//	3) un camion vienne d'etre assign�, et on doit faire pause
				for(int i = 0 ; i < nbIterPerStep && !stop ; i++) {

					//effectue le pas
					boolean justAssigned = step();

					//fait pause si necessaire
					//
					if(stopOnAssign && justAssigned ) {
						setPauseMode();
						stop = true;
					}

					//incremente le compteur de step
					stepCounter++;

					//met fin a la simulation si on atteint le max de steps
					//
					if(stepCounter >= max_steps) {
						createSommaireFrame();
						stop = true;
						setPauseMode();
					}
				}
				//avertis les listeners que la simulation a ete updatee
				notifyListenersUpdated();
			}
		});
	}

	/*
	 *avance la simulation de 1 increment
	 *retourne un boolean indiquant si un camion s'est retrouv� en �tat "idle" au cours du tour 
	 */
	private boolean step() {
		//a priori, aucun camion idle
		boolean wasIdle = false;

		//longueur de temps du step (plus long si on est en phase de warmup
		//
		double stepSize = Mine.TIME_INCREMENT;
		if(mine.isInWarmup()) {
			stepSize = Mine.TIME_INCREMENT_WARMUP;
		}

		//prepare les camions et les pelles pour leur debut de step
		//
		setCamionsEtPellesBeginStep(stepSize);
		//liste des pelles et des camions de la mine
		//
		ArrayList<Pelle> pelles = mine.getPelles();
		ArrayList<Camion> camions = mine.getCamions();

		// Tant que pas fin du step, avance les camions et les pelles
		// Cette condition est necessaire car les camions et les pelles peuvent effectuer plus d'une action par tour
		//
		while(!endStepCondition()) {
			//avance les camions qui sont en route
			//
			for(int i = 0 ; i < camions.size(); i++) {
				Camion camion = camions.get(i);
				if(camion.getState() == Camion.ETAT_EN_ROUTE) {
					camion.advance();
				}
			}

			// Traite les camions qui viennent d'arriver � destination 
			//(incluant ce qu'ils font du temps restant dans le tour)
			//
			for(int i = 0 ; i < camions.size(); i++) {
				Camion camion = camions.get(i);
				if(camion.getState() == Camion.ETAT_JUSTE_ARRIVE) {
					//enregistre le temps de parcours du camion � des fins statistiques
					//
					mine.getTravelTimePredictor().enregistreHistoriqueTempsParcours(camion);
					
					//avertis les listeners qu'un camion vient d'arriver (Sauf si en warmup!)
					//
					if(!mine.isInWarmup()) {
						this.notifyListenersCamionJustArrived(camion, mine.getTime());
					}
					
					//la station du camion determine ce qui doit etre fait du camion
					Station station = camion.getObjective();
					station.setCamionOnArrival(camion);
				}
			}


			// Traite les camions sans taches 
			// Demande � l'engin de decision de leur donner une tache
			//
			for(int i = 0 ; i < camions.size(); i++) {
				Camion camion = camions.get(i);
				if(camion.getState() == Camion.ETAT_INACTIF) {
					//indique qu'un camion a ete inactif durant le tour
					wasIdle = true;
					decisionMaker.giveObjectiveToCamion(camion);
				}
			}

			//active les pelles pour remplir les camions
			//
			for(int i = 0 ; i < pelles.size(); i++) {
				Pelle p = pelles.get(i);
				//si la pelle a un camion a remplir
				if(p.getCamionEnRemplissage()!= null) {
					p.activate();
					//si une pelle a �puise son temps, fait attendre le camion en remplissage
					//
					if(p.iterFinished()) {
						p.makeAllCamionWaitUntilEndIter();
					}
				}
			}
		}

		// Une fois que tout ce qui pouvait �tre fait a �t� fait,
		// fait attendre les pelles pour le reste du tour
		//
		for(int i = 0 ; i < pelles.size(); i++) {
			Pelle p = pelles.get(i);
			if(p.getCamionEnRemplissage()== null) {
				p.waitForRemainingTime();
			}
		}
		
		//incremente l'heure de la mine
		mine.addTime(stepSize);

		//si en warmup, ajuste dynamiquement le temps moyen d'attente
		//
		if(mine.isInWarmup()) {
			//System.out.println("temps attente moyen :"+mine.calculeTempsAttenteMoyenPelle());
			//System.out.println("cible temps attente :"+(mine.calculeTempsAttenteMoyenPelle()*0.5));
			//decisionMaker.setCibleTempsAttentePelle(mine.cibleTempsAttente());
		}

		return wasIdle;
	}

	/*
	 *condition de fin d'un pas : Quand tous les camions ont termin� de travailler
	 */
	private boolean endStepCondition() {
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			Camion c = mine.getCamions().get(i);
			if(!c.iterFinished()) {
				return false;
			}

		}
		return true;
	}

	/*
	 *set les camions et les pelles pour le debut d'une iteration
	 */
	private void setCamionsEtPellesBeginStep(double stepSize) {
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			mine.getCamions().get(i).setBeginIter(stepSize);
		}

		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			mine.getPelles().get(i).setBeginStep(stepSize);

		}

	}


	/*
	 * Effectue le warmup de la mine
	 */
	private void warmup() {
		//effectue les steps de warmup aleatoirement
		//
		String decisionFunction =decisionMaker.getScoreFunctionString(); 
		decisionMaker.setScoreFunctionString(DecisionMaker.WARMUP_SCORE_FUNCTION_STRING);

		// Effectue les steps
		//
		mine.setInWarmup(true);
		for(int i = 0 ; i < Mine.NB_WARMUP_STEPS; i++) {
			step();
		}
		
		//desactive le mode warmup
		//
		mine.setInWarmup(false);
		decisionMaker.setScoreFunctionString(decisionFunction);

		//calcule le temps moyen d'attente des pelles
		//
		//decisionMaker.setCibleTempsAttentePelle(mine.cibleTempsAttente());

		//reset les statistiques
		mine.resetAllStats();
	}

	/*
	 * setter du champ nbIterPerStep
	 */
	private void setNbIterPerStep(int delai) {
		this.nbIterPerStep = delai;	
	}



	/*
	 * Met la mine en mode play
	 * Avertis les listeners
	 */
	private void setPlayMode() {
		timer.start();
		notifyListenersUnpaused();

	}


	/*
	 * Met la mine en mode pause
	 * Avertis les listeners
	 */
	private void setPauseMode() {
		timer.stop();
		notifyListenersPaused();

	}

	/*
	 * Impl�mentation des m�thode de l'interface GuiListener
	 */

	@Override
	public void eventDispatched(AWTEvent arg0) {
	}

	@Override
	public void chargerButtonClicked(GuiEvent evt) {
	}

	@Override
	public void predictTimeChanged(GuiEvent evt) {
	
	}

	@Override
	public void chargeMineConfirmed(GuiEvent evt) {
	
	}

	@Override
	public void meteoSliderChanged(double meteoFactor) {
		System.out.println("meteoFactor"+meteoFactor);
		mine.setMeteoFactor(meteoFactor);
	}

	@Override
	//si le panel de mine est cliqu�, toogle le timer
	public void minePanelClicked(double fracX, double fracY) {
	
		if(getTimer().isRunning()) {
			System.out.println("set mode pause");
			setPauseMode();
		}
		else {
			System.out.println("set mode play");
			setPlayMode();
	
		}
	
	}

	@Override
	public void predictFunctionChanged(int newPredictFunctionIndex) {
		mine.getTravelTimePredictor().setPredictFunction(newPredictFunctionIndex);

	}


	@Override
	public void rhoValueChanged(double rhoValue) {
		mine.getTravelTimePredictor().setWeight(rhoValue);

	}


	@Override
	public void numberSampleChanged(int nbSample) {
		mine.getTravelTimePredictor().setNumberSample(nbSample);
	}


	@Override
	public void newSimulationRequested(int exempleNb, int numberOfCamions, double tempsSimulationSeconds) {
		
		if(this.sommaireFrame!= null) {
			sommaireFrame.dispose();
		}
		sommaireFrame = null;
		
		this.chargeMine(exempleNb, numberOfCamions, tempsSimulationSeconds);

	}


	@Override
	public void automaticCompletionRequested() {
		completerSimulation();

	}


	@Override
	public void stopOnAssignStateChanged(boolean selected) {
		this.stopOnAssign = selected;

	}


	@Override
	public void simulationSpeedChanged(int speed) {
		this.setNbIterPerStep(speed);

	}


	@Override
	public void playButtonPressed() {
		this.setPlayMode();

	}


	@Override
	public void pauseButtonPressed() {
		this.setPauseMode();

	}


	@Override
	public void resetSimulationRequested() {
		
		if(this.sommaireFrame!= null) {
			sommaireFrame.dispose();
		}
		sommaireFrame = null;
		
		setPauseMode();
		
		mine.resetTime();
		warmup();
		
		notifyListenersMineReset();
		this.stepCounter = 0;

	}


	@Override
	public void scoreFunctionChanged(String scoreFunction) {
		decisionMaker.setScoreFunctionString(scoreFunction);

	}

	/*
	 * Interractions avec les listeners
	 */
	private void notifyListenersCamionJustArrived(Camion camion, double time) {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).camionJustArrived(camion, time);
		}
	
	}

	private void notifyListenersUpdated() {
	
		for(int i = 0 ; i < listeners.size(); i++) {
			System.out.println("notifyListener");
			listeners.get(i).mineUpdated(mine);
		}	
	}

	private void notifyListenersUnpaused() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).minUnpaused(mine);
		}
	}

	private void notifyListenersPaused() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).minePaused(mine);
		}
	}

	private void notifyListenersAutomaticCompleteFinished() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteFinished();
		}	
	}

	private void notifyListenersAutomaticCompleteUpdated(double fractionComplete) {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteUpdated(fractionComplete);
		}	
	}

	private void notifyListenersMineReset() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).mineResetted(this);
		}
	
	}

	private void notifyListenersAutomaticCompleteStarted() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteStarted();
		}
	 
	}

	@Override
	public void planPelleChanged(Pelle p, double newValue) {
		
		p.setPlan(newValue);

	}


}