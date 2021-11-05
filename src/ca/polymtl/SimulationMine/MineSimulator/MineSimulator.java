package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.Timer;

import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.MineGui.GuiEvent;
import ca.polymtl.SimulationMine.MineGui.GuiListener;
import ca.polymtl.SimulationMine.MineGui.SommaireFrame;
import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.decisionMaker.CustomDecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.DecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;

//classe repr�sentant une simulation de mine. Elle gere la simulation en tant que telle et 
//l'interfacage avec le gui
//
public class MineSimulator implements GuiListener {


	//duree d'un pas de temps (secondes)
	protected static double TIME_INCREMENT = 4; // secondes

	/**nombre de pas d'increment avant le debut de l'affichage*/
	protected static int NB_WARMUP_STEPS = 300;

	protected static double TIME_INCREMENT_WARMUP = 60;//1 minute 

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

	private TravelTimePredictor travelTimePredictor;

	private boolean justAssigned;

	private ArrayList<StationFailureEvent> plannedFailureEvents;
	private ArrayList<StationFailureEvent> ongoingFailureEvents;
	private ArrayList<StationFailureEvent> completedFailureEvents;

	/**Parametres de configuration*/
	private Config config;


	//constructeur
	public MineSimulator(Config config) {
		//cree le module en charge de l'IA des camions

		this.config = config;
		//instantie la liste des listeners
		listeners = new ArrayList<MineSimulationListener>();

		//Cree la mine et l'initialise
		//
		mine = new Mine(config);
		
		//retrouve l'exampleId desire.
		ExampleId exIdChosen = null;
		for(ExampleId exId : Mine.exampleIds) {
			if(exId.getId().compareTo( config.getDefaultMineId() )==0) {
				exIdChosen = exId;
				break;
			}
		}
		
		if(exIdChosen == null) {
			throw new IllegalArgumentException("Aucune mine avec l'ID par d�faut : "+config.getDefaultMineId());
		}
		
		mine.init(exIdChosen);



		//Cree les listes d'evenements de pannes 
		//
		this.plannedFailureEvents = new ArrayList<StationFailureEvent>();
		this.ongoingFailureEvents = new ArrayList<StationFailureEvent>();
		this.completedFailureEvents = new ArrayList<StationFailureEvent>();


		//cree le predicteur de temps de parcours
		this.travelTimePredictor = new TravelTimePredictor(mine);

		//Cr�� l'engin de d�cision
		//
		decisionMaker = new CustomDecisionMaker(mine);

		//warmup la mine
		//
		warmup();


		//�v�nements (pannes) pour le premier jour
		this.selectFailureScenarioForNextDay();

		// Set les parametres de la simulation
		//
		//nombre de pas
		this.stepCounter = 0;
		//nombre max de pas
		this.max_steps = (int) (SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/MineSimulator.TIME_INCREMENT);

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

	@Override
	public void automaticCompletionRequested() {
		completerSimulation();

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
		this.max_steps = (int) (temps/MineSimulator.TIME_INCREMENT);

		this.plannedFailureEvents = new ArrayList<StationFailureEvent>();
		this.ongoingFailureEvents = new ArrayList<StationFailureEvent>();
		this.completedFailureEvents = new ArrayList<StationFailureEvent>();
		this.selectFailureScenarioForNextDay();



	}

	@Override
	public void chargeMineConfirmed(GuiEvent evt) {

	}

	@Override
	public void chargerButtonClicked(GuiEvent evt) {
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

	/**
	 * 
	 * @return L'efficacit� du camion en % tu temps pass� � faire des activit�s autre que l'attente.
	 */
	public double computeCamionEfficiency(Camion camion) {
		if(mine.getTime() == 0) {
			return 0;
		}
		double totalTime = mine.getTime();
		double waitingTime = camion.getWaitTime();
		double emptyTravelTime= camion.getEmptyTravelTime();

		double eff = (totalTime - waitingTime - emptyTravelTime)/totalTime *100;
		return eff;
	}

	/**
	 * 
	 * @return L'efficacit� de la pelle en % du temps pass� � remplir des camions.
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

	@Override
	public void eventDispatched(AWTEvent arg0) {
	}

	/**
	 * 
	 * @return Efficacit� moyenne des camoins
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
	 * @return Efficacit� moyenne des pelles
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
	 * @return Efficacit� du camion le plus efficace
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
	 * @return Efficacit� de la pelle la plus efficace
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
	 * @return Efficacit� du camion le moins efficace
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

	public Mine getMine() {
		return this.mine;
	}


	/**
	 * 
	 * @return Efficacit� de la pelle la moins efficace.
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
	 * @return Nombre total de voyages effectu�s par les camions
	 */
	public int getNumberOfRuns() {
		int nbVoyages = 0;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			nbVoyages += mine.getCamions().get(i).getNumberOfRuns();
		}
		return nbVoyages;
	}

	public int getTempsSimulationSeconds() {
		return (int) (this.max_steps*MineSimulator.TIME_INCREMENT);
	}

	public Timer getTimer() {
		return timer;
	}


	/**
	 * 
	 * @return Objet responsable de la pr�diction des temps de parcours.
	 */
	public TravelTimePredictor getTravelTimePredictor() {

		return this.travelTimePredictor;
	}

	public void initStepCounter() {
		this.stepCounter = 0;
	}



	@Override
	public void lambdaValueChanged(double rhoValue) {
		this.travelTimePredictor.setWeight(rhoValue);

	}


	@Override
	public void meteoSliderChanged(double meteoFactor) {
		mine.setMeteoFactor(meteoFactor);
	}

	/*
	 * Impl�mentation des m�thode de l'interface GuiListener
	 */

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
	public void newSimulationRequested(ExampleId exempleId, int numberOfSmallCamions, int numberOfLargeCamions, double tempsSimulationSeconds) {

		if(this.sommaireFrame!= null) {
			sommaireFrame.dispose();
		}
		sommaireFrame = null;
		System.out.println("charge mine "+exempleId.getName()+" "+exempleId.getFileName());
		this.chargeMine(exempleId, numberOfSmallCamions, numberOfLargeCamions, tempsSimulationSeconds);

	}

	@Override
	public void numberSampleChanged(int nbSample) {
		this.travelTimePredictor.setNumberSample(nbSample);
	}

	@Override
	public void pauseButtonPressed() {
		this.setPauseMode();

	}

	@Override
	public void planPelleChanged(Pelle p, double newValue) {

		p.setPlan(newValue);

	}

	@Override
	public void playButtonPressed() {
		this.setPlayMode();

	}

	@Override
	public void predictFunctionChanged(int newPredictFunctionIndex) {
		this.travelTimePredictor.setPredictFunction(newPredictFunctionIndex);

	}


	@Override
	public void predictTimeChanged(GuiEvent evt) {

	}


	@Override
	public void resetSimulationRequested() {

		if(this.sommaireFrame!= null) {
			sommaireFrame.dispose();
		}
		sommaireFrame = null;

		setPauseMode();

		ExampleId exempleId = mine.getCurrentExampleId();
		int nbSmallCamions = mine.getNumberSmallCamions();
		int nbLargeCamions = mine.getNumberLargeCamions();
		chargeMine(exempleId, nbSmallCamions, nbLargeCamions, this.max_steps*MineSimulator.TIME_INCREMENT);
		/*

		 */

	}


	@Override
	public void scoreFunctionChanged(String scoreFunction) {
		decisionMaker.setScoreFunctionString(scoreFunction);

	}


	@Override
	public void simulationSpeedChanged(int speed) {
		this.setNbIterPerStep(speed);

	}


	@Override
	public void stopOnAssignStateChanged(boolean selected) {
		this.stopOnAssign = selected;

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

	private void notifyListenersAutomaticCompleteFinished() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteFinished();
		}	
	}


	private void notifyListenersAutomaticCompleteStarted() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteStarted();
		}

	}


	private void notifyListenersAutomaticCompleteUpdated(double fractionComplete) {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteUpdated(fractionComplete);
		}	
	}

	/*
	 * Interractions avec les listeners
	 */
	private void notifyListenersCamionJustArrived(Camion camion, double time) {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).camionJustArrived(camion, time);
		}

	}

	private void notifyListenersMineReset() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).mineResetted(this);
		}

	}



	private void notifyListenersPaused() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).minePaused(mine);
		}
	}

	private void notifyListenersUnpaused() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).minUnpaused(mine);
		}
	}



	private void notifyListenersUpdated() {

		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).mineUpdated(mine);
		}	
	}


	//choisis le camion qui a termin� le plus tot
	//
	private Camion selectCamion() {

		double bestTime = Double.MAX_VALUE;
		Camion bestCamion = null;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			Camion c = mine.getCamions().get(i);
			if(c.getState() == Camion.ETAT_EN_TRAITEMENT) {
			}
			if(!c.iterFinished()) {
				if(bestCamion == null) {
					bestCamion = c;
					bestTime = c.taskTimeRemaining();
				}
				else if(c.taskTimeRemaining() < bestTime) {
					bestTime = c.taskTimeRemaining();
					bestCamion = c;
				}
			}
		}

		return bestCamion;
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
	 * setter du champ nbIterPerStep
	 */
	private void setNbIterPerStep(int delai) {
		this.nbIterPerStep = delai;	
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
	 * Met la mine en mode play
	 * Avertis les listeners
	 */
	private void setPlayMode() {
		timer.start();
		notifyListenersUnpaused();

	}

	/*
	 *avance la simulation de 1 increment
	 *retourne un boolean indiquant si un camion s'est retrouv� en �tat "idle" au cours du tour 
	 */
	private boolean step() {

		//a priori, aucun camion idle
		this.justAssigned = false;

		//longueur de temps du step (plus long si on est en phase de warmup
		//
		double stepSize = MineSimulator.TIME_INCREMENT;
		if(mine.isInWarmup()) {
			stepSize = MineSimulator.TIME_INCREMENT_WARMUP;
		}

		if(!mine.isInWarmup() && (int)mine.getTime()%3600 == 0) {
			System.out.println(mine.getTime());
		}

		//prepare les camions et les pelles pour leur debut de step
		//
		setCamionsEtPellesBeginStep(stepSize);
		//liste des pelles et des camions de la mine
		//

		// Tant que pas fin du step, avance les camions et les pelles
		// Cette condition est necessaire car les camions et les pelles peuvent effectuer plus d'une action par tour
		//
		while(selectCamion() != null) {

			double temps = timeUntilNextEvent();
			if(temps >= stepSize) {
				temps = stepSize;
			}
			stepSize -= temps;



			//calcule l'attente des stations (difference entre le temps d'iter et le temps interne d'iter)
			// si une pelle est inactive, elle le restera pour toute l'iteration
			// Il faut faire cela avant le reste pour eviter que l'etat des pelles ne soit chang�!
			//
			//pelles
			for(int i = 0 ; i < this.mine.getPelles().size(); i++) {
				if(this.mine.getPelles().get(i).getState() == Station.STATION_STATE_IDLE || this.mine.getPelles().get(i).getState() == Station.STATION_STATE_PANNE) {
					this.mine.getPelles().get(i).attend(temps);
				}
				this.mine.getPelles().get(i).addIterTime(temps);
			}
			//concentrateurs
			for(int i = 0 ; i < this.mine.getConcentrateurs().size(); i++) {
				if(this.mine.getConcentrateurs().get(i).getState() == Station.STATION_STATE_IDLE || this.mine.getPelles().get(i).getState() == Station.STATION_STATE_PANNE) {
					this.mine.getConcentrateurs().get(i).attend(temps);
				}
				this.mine.getConcentrateurs().get(i).addIterTime(temps);
			}
			//steriles
			for(int i = 0 ; i < this.mine.getSteriles().size(); i++) {
				if(this.mine.getSteriles().get(i).getState() == Station.STATION_STATE_IDLE || this.mine.getPelles().get(i).getState() == Station.STATION_STATE_PANNE) {
					this.mine.getSteriles().get(i).attend(temps);
				}
				this.mine.getSteriles().get(i).addIterTime(temps);
			}


			//traite tous les camions pour la meme duree. On sait que ces camions ne terminent pas leur tache
			for(int i = 0 ; i < this.mine.getCamions().size(); i++) {
				Camion camion = this.mine.getCamions().get(i);
				traiteCamion(camion, temps);
			}

			//update les files d'attente des stations
			//
			//pelles
			for(int i = 0 ; i < this.mine.getPelles().size(); i++) {
				this.mine.getPelles().get(i).updateFileAttente();

			}
			//steriles
			for(int i = 0 ; i < this.mine.getSteriles().size(); i++) {
				this.mine.getSteriles().get(i).updateFileAttente();
			}
			//concentrateurs
			for(int i = 0 ; i < this.mine.getConcentrateurs().size(); i++) {
				this.mine.getConcentrateurs().get(i).updateFileAttente();
			}




			int prevDayNumber = mine.getDayNumber();

			//incremente l'heure de la mine
			mine.addTime(temps);

			if(mine.getDayNumber() > prevDayNumber) {
				selectFailureScenarioForNextDay();
			}

			//update les pannes en cours
			updateFailureEvents();

			//si en warmup, ajuste dynamiquement le temps moyen d'attente
			//
			if(mine.isInWarmup()) {
				//System.out.println("temps attente moyen :"+mine.calculeTempsAttenteMoyenPelle());
				//System.out.println("cible temps attente :"+(mine.calculeTempsAttenteMoyenPelle()*0.5));
				//decisionMaker.setCibleTempsAttentePelle(mine.cibleTempsAttente());
			}
		}
		return justAssigned;
	}

	private void updateFailureEvents() {

		boolean situationChanged = false;

		ArrayList<StationFailureEvent> addToOngoing = new ArrayList<StationFailureEvent>();
		ArrayList<StationFailureEvent> addToCompleted = new ArrayList<StationFailureEvent>();

		//met en panne les pelles qui le deviennent.
		// On utilise un iterateur pour pouvoir supprimer des evenement en iterant
		ListIterator<StationFailureEvent> iter = this.plannedFailureEvents.listIterator();
		while(iter.hasNext()) {
			StationFailureEvent fe = iter.next();
			if( fe.getBeginTimeSec() <= mine.getTime() ) {
				situationChanged = true;
				Station s = fe.getStation();
				s.setFailureMode(true);
				iter.remove();

				System.out.println("temps :"+mine.getTime()+" Debute panne a la station "+s.getId()+". Fin de la panne a "+fe.getEndTimeSec());

				//Redirige les camions en attente, en remplissage, et en route vers la pelle
				//
				for(int i = 0 ; i < mine.getCamions().size(); i++) {
					Camion c = mine.getCamions().get(i);
					//si camion est soit : 
					//	- en traitement � la station
					//	- en attente a la station
					// met le camion en etat idle et lui donne un nouvel objectif
					// Note : Les camions en route vers la station s'y rendent quand meme. Ils ne savent pas que la pelle est inactive.
					if((c.getState() == Camion.ETAT_ATTENTE && c.getCurrentStation().equals(s)) ||
							c.getState() == Camion.ETAT_EN_TRAITEMENT && c.getCurrentStation().equals(s) ) {
						c.setStateInactif();
					}
				}


				addToOngoing.add(fe);
			}
		}

		//R�active les pelles dont la panne se termine
		// On utilise un iterateur pour pouvoir supprimer des evenement en iterant
		iter = this.ongoingFailureEvents.listIterator();
		while(iter.hasNext()) {
			StationFailureEvent fe = iter.next();
			if(fe.getEndTimeSec() <= mine.getTime()) {
				situationChanged = true;
				Station s = fe.getStation();
				s.setFailureMode(false);
				iter.remove();

				System.out.println("temps :"+mine.getTime()+" Fin de panne a la station "+s.getId());
				addToCompleted.add(fe);
			}
		}

		//update le plan si les pannes ont chang�.
		if(situationChanged) {
			this.decisionMaker.updatePlan();
		}
		//ajoute les evenements aux listes des "ongoing" et "completed
		//
		for(int i = 0 ; i < addToOngoing.size(); i++) {
			this.ongoingFailureEvents.add(addToOngoing.get(i));
		}
		for(int i = 0 ; i < addToCompleted.size(); i++) {
			this.completedFailureEvents.add(addToCompleted.get(i));
		}

	}

	private double timeUntilNextEvent() {

		double timeUntilNextEvent = Double.MAX_VALUE;

		//temps min avant qu'un camion n'ait termin�
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			if(mine.getCamions().get(i).taskTimeRemaining() < timeUntilNextEvent) {
				timeUntilNextEvent = mine.getCamions().get(i).taskTimeRemaining();
			}
		}

		//regarde les StationFailureEvents � venir
		for(int i = 0 ; i < this.plannedFailureEvents.size(); i++) {
			if(this.plannedFailureEvents.get(i).getBeginTimeSec() - mine.getTime() < timeUntilNextEvent) {
				timeUntilNextEvent = 1.*plannedFailureEvents.get(i).getBeginTimeSec() - mine.getTime();
			}
		}

		//regarde les StationFailureEvents en cours
		for(int i = 0 ; i < this.plannedFailureEvents.size(); i++) {
			if(this.plannedFailureEvents.get(i).getEndTimeSec() - mine.getTime() < timeUntilNextEvent) {
				timeUntilNextEvent = 1.*plannedFailureEvents.get(i).getEndTimeSec() - mine.getTime();
			}
		}

		return timeUntilNextEvent;
	}

	/**
	 * Choisis et applique un sc�nario d'�checs pour la journ�e suivante.
	 */
	private void selectFailureScenarioForNextDay() {

		System.out.println("selectionne failureScenarios");
		//choisis un sc�nario d'�checs
		FailureScenario fs = mine.getRandomFailureScenario();

		if(fs != null) {
			for(int i = 0 ; i < fs.getFailureEvents().size(); i++) {
				StationFailureEvent fe = fs.getFailureEvents().get(i);
				//cree un nouvel objet StationFailureEvent avec la "vraie" heure de d�part et de fin
				long realBeginTimeSec = fe.beginTimeSec+mine.getDayNumber()*Mine.ONE_DAY_SEC;
				long realEndTimeSec = fe.endTimeSec+mine.getDayNumber()*Mine.ONE_DAY_SEC;
				StationFailureEvent feCopy = new StationFailureEvent(realBeginTimeSec, realEndTimeSec, fe.getStation() );

				System.out.println("evenement planifie : "+fe.getStation().getId()+" "+realBeginTimeSec+" "+realEndTimeSec);
				this.plannedFailureEvents.add(feCopy);
			}
		}
		if(fs == null || fs.getFailureEvents().size()==0) {
			System.out.println("Aucun evenement pour la journee");
		}

	}

	//traite un camion pour une duree determinee
	private void traiteCamion(Camion c, double temps) {

		if(c.getState() == Camion.ETAT_INACTIF) {
			Station s = decisionMaker.giveObjectiveToCamion(c);
			c.setObjective(s);

			c.setPredictedTravelTime(this.travelTimePredictor.predictTravelTime(c.getOrigine(), s, c));
			traiteCamion(c, temps);
		}
		//si le camion est en route, on le fait rouler vers sa destination. Si il arrive � destination avant la fin du tour, on le traite imm�diatement � nouveau.
		//
		else if(c.getState() == Camion.ETAT_EN_ROUTE) {
			traiteCamionEnRoute(c, temps);
		}
		//si en traitement, charge/decharge le camion selon
		else if(c.getState() == Camion.ETAT_EN_TRAITEMENT ) {
			traiteCamionEnTraitement(c, temps);
		}
		else if(c.getState() == Camion.ETAT_ATTENTE) {
			traiteCamionEnAttente(c, temps);
		}
	}

	//traite camion en attente
	private void traiteCamionEnAttente(Camion c, double temps) {
		Camion camionEnTraitement = c.getCurrentStation().getCamionEnTraitement();
		if(camionEnTraitement == null) {
			throw new IllegalStateException("On ne peut pas traiter un camion en attente si aucun camion n'est en traitement");
		}
		c.attend(temps);
	}

	//traite un camion qui est en route
	private void traiteCamionEnRoute(Camion c, double temps) {
		c.advance(temps);
		//si viens juste d'arriver a destination, traite immediatement le meme camion
		//
		if(c.getState() == Camion.ETAT_JUSTE_ARRIVE) {

			travelTimePredictor.enregistreHistoriqueTempsParcours(c);
			notifyListenersCamionJustArrived(c, mine.getTime());
			c.getCurrentStation().setCamionOnArrival(c);
		}
	}

	private void traiteCamionEnTraitement(Camion c, double temps) {
		//1) remplis/vide le camion autant que possible
		//2) fait attendre les camions de la file d'attente d'autant
		//3) si le camion a termine, 
		//		- on choisit le nouveau camion en traitement.
		//		- on donne un nouvel objectif au camion (qui devient en route). 

		//trouve le temps de traitement maximum. Il s'agit du max entre : 
		//	- le temps restant � la pelle
		//	- le temps restant au camion
		//	- le temps de chargement/dechargement max
		Station s = c.getCurrentStation();

		boolean modeCharge = !s.isDecharge;


		double tempsChargement = temps;
		double chargeSpeed = s.currentChargeSpeed;
		double quantite = tempsChargement*chargeSpeed;

		if(!modeCharge) {
			c.decharge(quantite, tempsChargement);
		}
		else {
			c.charge(quantite, tempsChargement);
		}
		s.updateQteTraite(quantite, c.getRockType());
		/*
		ArrayList<Camion> camionsEnAttente = s.getCamionsEnAttente();
		for(int i = 0 ; i < camionsEnAttente.size(); i++) {
			camionsEnAttente.get(i).attend(tempsChargement);

		}*/

		if((modeCharge && c.getCharge() == c.getChargeMax()) || (!modeCharge && c.getCharge() == 0)){
			c.setStateInactif();
			//s.setCamionEnTraitement(s.camionsEnAttente.get(0));
			Station objective = decisionMaker.giveObjectiveToCamion(c);
			c.setObjective(objective);
			c.setPredictedTravelTime(this.travelTimePredictor.predictTravelTime(c.getOrigine(), s, c));
			if(!modeCharge) {
				this.justAssigned = true;
			}

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
		for(int i = 0 ; i < MineSimulator.NB_WARMUP_STEPS; i++) {
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

	protected void setTravelTimePredictor(TravelTimePredictor predictor) {

		this.travelTimePredictor = predictor;
	}


}
