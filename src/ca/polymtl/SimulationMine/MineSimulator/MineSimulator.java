package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.Timer;

import ca.polymtl.SimulationMine.MineGui.GuiEvent;
import ca.polymtl.SimulationMine.MineGui.GuiListener;
import ca.polymtl.SimulationMine.MineGui.SommaireFrame;
import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.decisionMaker.CustomDecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.DecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;

//classe représentant une simulation de mine. Elle gere la simulation en tant que telle et 
//l'interfacage avec le gui
//
public class MineSimulator implements GuiListener {

	//engin de décision
	public CustomDecisionMaker decisionMaker;

	//mine simulée
	private Mine mine;

	//objets qui écoutent la simulation
	private ArrayList<MineSimulationListener> listeners;

	private SommaireFrame sommaireFrame;
	
	/*
	 * État de la simulation
	 */
	//nombre de pas de simulation effectués
	private int stepCounter;	
	//nombre max de pas pour la simulation actuelle
	private int max_steps;
	//nombre de pas par iteration (determine la vitesse de simulation)
	private int nbIterPerStep;
	//timer en charge d'updater la simulation à intervalle régulier
	private Timer timer;
	//si true, stop à chaque fois qu'un camion arrive au stérile/au concentrateur	
	private boolean stopOnAssign;

	private TravelTimePredictor travelTimePredictor;


	//constructeur
	public MineSimulator() {
		//cree le module en charge de l'IA des camions


		//instantie la liste des listeners
		listeners = new ArrayList<MineSimulationListener>();

		//Créé la mine et l'initialise
		//
		mine = new Mine(this);
		mine.init(Mine.exampleIds.get(0), 20, 0);
		
		//cree le predicteur de temps de parcours
		this.travelTimePredictor = new TravelTimePredictor(mine);

		//Créé l'engin de décision
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

		//par defaut, on ne stop pas a chaque fois qu'un camion arrive au stérile/concentrateur
		this.stopOnAssign = false;

		//ajoute le timer de la simulation
		//
		this.timer = createTimer();
		//delai par défaut (millisecondes) entre 2 itérations
		//
		this.setNbIterPerStep(26);

	}

	/*
	 * Ajoute un listener à la liste des listeners
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

	/**
	 * 
	 * @return Objet responsable de la prédiction des temps de parcours.
	 */
	public TravelTimePredictor getTravelTimePredictor() {

		return this.travelTimePredictor;
	}
	
	protected void setTravelTimePredictor(TravelTimePredictor predictor) {

		this.travelTimePredictor = predictor;
	}
	
	public int getTempsSimulationSeconds() {
		return (int) (this.max_steps*Mine.TIME_INCREMENT);
	}

	/*
	 * reset la mine selon un exemple donne
	 */
	public void chargeMine(ExampleId exempleId, int nbSmallCamions, int nbLargeCamions, double temps) {
	
		setPauseMode();
	
		//reinitialise la nouvelle mine
		//
		mine.init(exempleId, nbSmallCamions, nbLargeCamions);
		warmup();
	
		notifyListenersMineReset();
		this.stepCounter = 0;
		this.max_steps = (int) (temps/Mine.TIME_INCREMENT);
	}

	/*
	 * Complète automatiquement la simulation (sans attendre les evenements du timer)
	 */
	public void completerSimulation() {
		//avertis les listeners
		notifyListenersAutomaticCompleteStarted();

		setPauseMode();

		//compte combien de pas représentent 1% de complétion
		//
		int counterStart = stepCounter;
		int stepsLeft = max_steps-counterStart;
		int onePercent = (stepsLeft)/100;
		
		//effectue les pas
		//
		while(stepCounter < max_steps) {
			step();
			//à chaque pourcentage, avertis les listeners
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
	 * Lorsque le timer s'active, effectue un nombre de pas à la simulation
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
				//effectue des pas jusqu'à ce que soit : 
				//	1) on ait effectué nbIterPerStep pas
				//	2) on ait terminé la simulation
				//	3) un camion vienne d'etre assigné, et on doit faire pause
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
	 *retourne un boolean indiquant si un camion s'est retrouvé en état "idle" au cours du tour 
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
			
			System.out.println("\nsous-iteration");
			//avance les camions qui sont en route
			//
			for(int i = 0 ; i < camions.size(); i++) {
				Camion camion = camions.get(i);
				if(camion.getState() == Camion.ETAT_EN_ROUTE) {
					camion.advance();
				}
			}

			// Traite les camions qui viennent d'arriver à destination 
			//(incluant ce qu'ils font du temps restant dans le tour)
			//
			for(int i = 0 ; i < camions.size(); i++) {
				Camion camion = camions.get(i);
				if(camion.getState() == Camion.ETAT_JUSTE_ARRIVE) {
					//enregistre le temps de parcours du camion à des fins statistiques
					//
					this.travelTimePredictor.enregistreHistoriqueTempsParcours(camion);
					
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
			// Demande à l'engin de decision de leur donner une tache
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
				if(!p.iterFinished && p.getCamionEnTraitement()!= null) {
					System.out.println("Active la pelle "+p.getId());
					p.activate();
					//si une pelle a épuise son temps, fait attendre le camion en remplissage
					//
					if(p.iterFinished()) {
						p.makeAllCamionWaitUntilEndIter();
					}
				}
			}
			
			//active les concentrateurs
			//
			for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
				Concentrateur concentrateur = mine.getConcentrateurs().get(i);
				System.out.println("Active le concentrateur");
				concentrateur.activate();
				if(concentrateur.iterFinished()) {
					System.out.println("Concentrateur terminé");
					concentrateur.makeAllCamionWaitUntilEndIter();
				}
			}
			
			//active les stériles
			//
			for(int i = 0 ; i < mine.getSteriles().size(); i++) {
				Sterile sterile = mine.getSteriles().get(i);
				System.out.println("Active le sterile");
				sterile.activate();
				if(sterile.iterFinished()) {
					System.out.println("Concentrateur terminé");
					sterile.makeAllCamionWaitUntilEndIter();
				}
			}
			
		}

		// Une fois que tout ce qui pouvait être fait a été fait,
		// fait attendre les pelles pour le reste du tour
		//
		for(int i = 0 ; i < pelles.size(); i++) {
			Pelle p = pelles.get(i);
			if(p.getCamionEnTraitement()== null) {
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
	 *condition de fin d'un pas : Quand tous les camions ont terminé de travailler
	 */
	private boolean endStepCondition() {
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			Camion c = mine.getCamions().get(i);
			if(!c.iterFinished()) {
				System.out.println("Camion "+c.getRemainingTimeInTurn()+" "+c.getState());
				return false;
			}

		}
		return true;
	}

	/*
	 *set les camions et les pelles pour le debut d'une iteration
	 */
	private void setCamionsEtPellesBeginStep(double stepSize) {
		//camions
		//
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			mine.getCamions().get(i).setBeginIter(stepSize);
		}

		//pelles
		//
		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			mine.getPelles().get(i).setBeginStep(stepSize);

		}

		//concentrateurs
		//
		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
			mine.getConcentrateurs().get(i).setBeginStep(stepSize);
		}
		
		//steriles
		//
		for(int i = 0 ; i < mine.getSteriles().size(); i++) {
			mine.getSteriles().get(i).setBeginStep(stepSize);
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
	 * Implémentation des méthode de l'interface GuiListener
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
	//si le panel de mine est cliqué, toogle le timer
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
		this.travelTimePredictor.setPredictFunction(newPredictFunctionIndex);

	}


	@Override
	public void lambdaValueChanged(double rhoValue) {
		this.travelTimePredictor.setWeight(rhoValue);

	}


	@Override
	public void numberSampleChanged(int nbSample) {
		this.travelTimePredictor.setNumberSample(nbSample);
	}


	@Override
	public void newSimulationRequested(ExampleId exempleId, int numberOfSmallCamions, int numberOfLargeCamions, double tempsSimulationSeconds) {
		
		if(this.sommaireFrame!= null) {
			sommaireFrame.dispose();
		}
		sommaireFrame = null;
		System.out.println("charge mine "+exempleId.getName()+" "+exempleId.getFileName());
		this.chargeMine(exempleId, numberOfSmallCamions, numberOfLargeCamions, tempsSimulationSeconds);

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

	
	/**
	 * 
	 * @return Efficacité moyenne des camoins
	 */
	public double getAverageCamionEfficiency() {
		double sumEff = 0;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			double eff = computeCamionEfficiency(mine.getCamions().get(i));
			sumEff += eff;
		}
		return sumEff/mine.getCamions().size();
	}

	/**
	 * 
	 * @return Efficacité moyenne des pelles
	 */
	public double getAveragePelleEfficiency() {
		double sumEff = 0;
		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			double eff = computePelleEfficiency(mine.getPelles().get(i));
			sumEff += eff;
		}
		return sumEff/mine.getPelles().size();
	}

	/**
	 * 
	 * @return L'efficacité du camion en % tu temps passé à faire des activités autre que l'attente.
	 */
	public double computeCamionEfficiency(Camion camion) {
		if(mine.getTime() == 0) {
			return 0;
		}
		double totalTime = mine.getTime();
		double waitingTime = camion.getWaitTime();

		double eff = (totalTime - waitingTime)/totalTime *100;
		return eff;
	}



	/**
	 * 
	 * @return L'efficacité de la pelle en % du temps passé à remplir des camions.
	 */
	public double computePelleEfficiency(Pelle pelle) {
		if(mine.getTime() == 0) {
			return 0;
		}
		double totalTime = mine.getTime();
		double waitingTime = pelle.getWaitTime();

		double eff = (totalTime - waitingTime)/totalTime *100;
		return eff;
	}
	
	/**
	 * 
	 * @return Efficacité du camion le plus efficace
	 */
	public double getMaxCamionEfficiency() {
		double effMax = 0;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			double eff = computeCamionEfficiency(mine.getCamions().get(i));
			if(eff > effMax) {
				effMax = eff;
			}
		}
		return effMax;
	}



	/**
	 * 
	 * @return Efficacité de la pelle la plus efficace
	 */
	public double getMaxPelleEfficiency() {
		double effMax = 0;
		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			double eff = computePelleEfficiency(mine.getPelles().get(i));
			if(eff > effMax) {
				effMax = eff;
			}
		}
		return effMax;
	}

	
	/**
	 * 
	 * @return Efficacité du camion le moins efficace
	 */
	public double getMinCamionEfficiency() {
		double effMin = 1000;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			double eff = computeCamionEfficiency(mine.getCamions().get(i));
			if(eff < effMin) {
				effMin = eff;
			}
		}

		return effMin;
	}


	/**
	 * 
	 * @return Efficacité de la pelle la moins efficace.
	 */
	public double getMinPelleEfficiency() {
		double effMin = 1000;
		for(int i = 0 ; i < mine.getPelles().size(); i++) {
			double eff = computePelleEfficiency(mine.getPelles().get(i));
			if(eff < effMin) {
				effMin = eff;
			}
		}

		return effMin;
	}

	/**
	 * 
	 * @return Nombre total de voyages effectués par les camions
	 */
	public int getNumberOfRuns() {
		int nbVoyages = 0;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			nbVoyages += mine.getCamions().get(i).getNumberOfRuns();
		}
		return nbVoyages;
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
