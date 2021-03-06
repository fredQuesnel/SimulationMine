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

/**
 * classe representant une simulation de mine. Elle gere la simulation en tant que telle. 
 * @author Fred
 *
 */
public class MineSimulator implements GuiListener {


	/**duree d'un pas de temps (secondes)*/
	protected static double TIME_INCREMENT = 4; // secondes

	/**nombre de pas d'increment avant le debut de l'affichage*/
	protected static int NB_WARMUP_STEPS = 300;

	/**Temps par iteration en warmup*/
	protected static double TIME_INCREMENT_WARMUP = 60;//1 minute 

	/**engin de decision*/
	public CustomDecisionMaker decisionMaker;

	/**mine simulee*/
	private Mine mine;

	/**Liste des listeners*/
	private ArrayList<MineSimulationListener> listeners;

	/**Frame sommaire*/
	private SommaireFrame sommaireFrame;

	/*
	 *etat de la simulation
	 */
	/**nombre de pas de simulation effectues*/
	private int stepCounter;	
	/**nombre max de pas pour la simulation actuelle*/
	private int max_steps;
	/**nombre de pas par iteration (determine la vitesse de simulation)*/
	private int nbIterPerStep;
	/**timer en charge d'updater la simulation a intervalle regulier*/
	private Timer timer;
	/**si true, stop a chaque fois qu'un camion arrive au sterile/au concentrateur*/	
	private boolean stopOnAssign;

	/**Classe de prediction des temps de parcours*/
	private TravelTimePredictor travelTimePredictor;

	/**true si un camion vient d'etre assigne, false sinon*/
	//TODO cela ne devrait pas etre un champs...
	private boolean justAssigned;

	/**Pannes "pr?vues"*/
	private ArrayList<StationFailureEvent> plannedFailureEvents;
	/**Pannes en cours*/
	private ArrayList<StationFailureEvent> ongoingFailureEvents;
	/**Pannes pass?es*/
	private ArrayList<StationFailureEvent> completedFailureEvents;

	/**Parametres de configuration*/
	private Config config;


	/**constructeur
	 * 
	 * @param config objet config
	 */
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
			throw new IllegalArgumentException("Aucune mine avec l'ID par defaut : "+config.getDefaultMineId());
		}
		
		mine.init(exIdChosen, config.getDefaultNbCamionsSmall(), config.getDefaultNbCamionsLarge());



		//Cree les listes d'evenements de pannes 
		//
		this.plannedFailureEvents = new ArrayList<StationFailureEvent>();
		this.ongoingFailureEvents = new ArrayList<StationFailureEvent>();
		this.completedFailureEvents = new ArrayList<StationFailureEvent>();


		//cree le predicteur de temps de parcours
		this.travelTimePredictor = new TravelTimePredictor(mine, config);

		//Cree l'engin de decision
		//
		decisionMaker = new CustomDecisionMaker(mine, config);

		//warmup la mine
		//
		warmup();


		//Evenements (pannes) pour le premier jour
		this.selectFailureScenarioForNextDay();

		// Set les parametres de la simulation
		//
		//nombre de pas
		this.stepCounter = 0;
		//nombre max de pas
		this.max_steps = (int) (SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/MineSimulator.TIME_INCREMENT);

		//par defaut, on ne stop pas a chaque fois qu'un camion arrive au sterile/concentrateur
		this.stopOnAssign = config.isDefaultPauseFinVoyage();

		//ajoute le timer de la simulation
		//
		this.timer = createTimer();
		//delai par defaut (millisecondes) entre 2 iterations
		//
		this.setNbIterPerStep(config.getDefaultSimultaionSpeed());

	}

	/**Ajoute un listener a la liste des listeners
	 * 
	 * @param listener listener a ajouter
	 */
	public void addListener(MineSimulationListener listener){
		listeners.add(listener);
	}

	@Override
	public void automaticCompletionRequested() {
		completerSimulation();
	}

	/**Reset la mine selon un exemple donne
	 * 
	 * @param exempleId Exemplaire de mine
	 * @param nbSmallCamions Nombre de petits camions
	 * @param nbLargeCamions Nombre de gros camions
	 * @param temps temps
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

	/**
	 * Complete automatiquement la simulation (sans attendre les evenements du timer)
	 */
	public void completerSimulation() {
		//avertis les listeners
		notifyListenersAutomaticCompleteStarted();

		setPauseMode();

		//compte combien de pas representent 1% de completion
		//
		int counterStart = stepCounter;
		int stepsLeft = max_steps-counterStart;
		int onePercent = (stepsLeft)/100;

		//effectue les pas
		//
		while(stepCounter < max_steps) {
			step();
			//a chaque pourcentage, avertis les listeners
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
	  * @param camion camion
	  * @return  L'efficacite du camion en % tu temps passe a faire des activites autre que l'attente.
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
	  * @param pelle pelle
	  * @return L'efficacite de la pelle en % du temps passe a remplir des camions.
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
	 * @return Efficacite moyenne des camoins
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
	 * @return Efficacite moyenne des pelles
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
	 * @return Efficacite du camion le plus efficace
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
	 * @return Efficacite de la pelle la plus efficace
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
	 * @return Efficacite du camion le moins efficace
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

	/**Retourne la mine
	 * 
	 * @return la mine
	 */
	public Mine getMine() {
		return this.mine;
	}


	/**
	 * 
	 * @return Efficacite de la pelle la moins efficace.
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
	 * @return Nombre total de voyages effectues par les camions
	 */
	public int getNumberOfRuns() {
		int nbVoyages = 0;
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			nbVoyages += mine.getCamions().get(i).getNumberOfRuns();
		}
		return nbVoyages;
	}

	/**
	 * 
	 * @return Temps de simulation, en secondes.
	 */
	public int getTempsSimulationSeconds() {
		return (int) (this.max_steps*MineSimulator.TIME_INCREMENT);
	}

	/**Retourne l'objet Timer
	 * 
	 * @return l'objet Timer
	 */
	public Timer getTimer() {
		return timer;
	}


	/**
	 * 
	 * @return Objet responsable de la prediction des temps de parcours.
	 */
	public TravelTimePredictor getTravelTimePredictor() {

		return this.travelTimePredictor;
	}

	/**Initialise le nombre de pas de simulation
	 * 
	 */
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
	 * Implementation des methode de l'interface GuiListener
	 */

	@Override
	//si le panel de mine est clique, toogle le timer
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

		//reinitialise la nouvelle mine
		//
		
		warmup();

		notifyListenersMineReset();
		this.stepCounter = 0;
		

		this.plannedFailureEvents = new ArrayList<StationFailureEvent>();
		this.ongoingFailureEvents = new ArrayList<StationFailureEvent>();
		this.completedFailureEvents = new ArrayList<StationFailureEvent>();
		this.selectFailureScenarioForNextDay();
		
		this.travelTimePredictor.resetStats();
		
	}


	@Override
	public void scoreFunctionSmallCamionsChanged(String scoreFunction) {
		decisionMaker.setScoreFunctionSmallCamionsString(scoreFunction);

	}


	@Override
	public void scoreFunctionLargeCamionsChanged(String scoreFunction) {
		decisionMaker.setScoreFunctionLargeCamionsString(scoreFunction);
	}

	@Override
	public void simulationSpeedChanged(int speed) {
		this.setNbIterPerStep(speed);
	}


	@Override
	public void stopOnAssignStateChanged(boolean selected) {
		this.stopOnAssign = selected;

	}

	/**Cree le panneau sommaire a la fin de la simulation*/
	private void createSommaireFrame() {
		this.sommaireFrame = new SommaireFrame(this);

	}


	/**
	 * Cree le timer de la simulation
	 * Lorsque le timer s'active, effectue un nombre de pas a la simulation
	 * @return le timer
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
				//effectue des pas jusqu'a ce que soit : 
				//	1) on ait effectue nbIterPerStep pas
				//	2) on ait termine la simulation
				//	3) un camion vienne d'etre assigne, et on doit faire pause
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

	/**Avertis les listeners que la completion automatique est terminee*/
	private void notifyListenersAutomaticCompleteFinished() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteFinished();
		}	
	}


	/**Avertis les listeners que la completion automatique vient de debuter*/
	private void notifyListenersAutomaticCompleteStarted() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteStarted();
		}

	}


	/**Avertis les listeners que l'on vient de mettre a jour la completion automatique*/
	private void notifyListenersAutomaticCompleteUpdated(double fractionComplete) {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).automaticCompleteUpdated(fractionComplete);
		}	
	}

	
	/**Avertis les listeners qu'un camion vient d'arriver a destination*/
	private void notifyListenersCamionJustArrived(Camion camion, double time) {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).camionJustArrived(camion, time);
		}

	}

	/**Avertis les listeners que la mine vient d'etre reinitialisee*/
	private void notifyListenersMineReset() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).mineResetted(this);
		}

	}


	/**Avertis les listeners qu'on vient de mettre la simulation en pause*/
	private void notifyListenersPaused() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).minePaused(mine);
		}
	}

	/**Avertis les listeners qu'on vient de reprendre la simulation (play)*/
	private void notifyListenersUnpaused() {
		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).minUnpaused(mine);
		}
	}


	/**Avertis les listeners qu'on vient d'updater la mine*/
	private void notifyListenersUpdated() {

		for(int i = 0 ; i < listeners.size(); i++) {
			listeners.get(i).mineUpdated(mine);
		}	
	}


	/**choisis le camion qui a termine le plus tot
	 * 
	 * @return camion qui termine le plus tot
	 */
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


	/**
	 *set les camions et les pelles pour le debut d'une iteration
	 *@param stepSize : taille du pas
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

	/**
	 * setter du champ nbIterPerStep
	 * @param delai nombre de pas d'iter par step
	 */
	private void setNbIterPerStep(int delai) {
		this.nbIterPerStep = delai;	
	}
	/**
	 * Met la mine en mode pause
	 * Avertis les listeners
	 */
	private void setPauseMode() {
		timer.stop();
		notifyListenersPaused();

	}

	/**
	 * Met la mine en mode play
	 * Avertis les listeners
	 */
	private void setPlayMode() {
		timer.start();
		notifyListenersUnpaused();

	}

	/**
	 *avance la simulation de 1 increment
	 * @return true si un camion s'est retrouve en etat "idle" au cours du tour 
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
			// Il faut faire cela avant le reste pour eviter que l'etat des pelles ne soit change!
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

	/**Met a jour les pannes (pannes qui se terminent, pannes qui d?butent).
	 * 
	 */
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
					//	- en traitement a la station
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

		//Reactive les pelles dont la panne se termine
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

		//update le plan si les pannes ont changees
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

	/**Calcule le temps avant qu'un evenement se produise
	 * 
	 * @return temps avant qu'un evenement se produise
	 */
	private double timeUntilNextEvent() {

		double timeUntilNextEvent = Double.MAX_VALUE;

		//temps min avant qu'un camion n'ait termine		
		for(int i = 0 ; i < mine.getCamions().size(); i++) {
			if(mine.getCamions().get(i).taskTimeRemaining() < timeUntilNextEvent) {
				timeUntilNextEvent = mine.getCamions().get(i).taskTimeRemaining();
			}
		}

		//regarde les StationFailureEvents venir
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
	 * Choisis et applique un scenario d'echecs pour la journee suivante.
	 */
	private void selectFailureScenarioForNextDay() {

		System.out.println("selectionne failureScenarios");
		//choisis un scenario d'echecs
		FailureScenario fs = mine.getRandomFailureScenario();

		if(fs != null) {
			for(int i = 0 ; i < fs.getFailureEvents().size(); i++) {
				StationFailureEvent fe = fs.getFailureEvents().get(i);
				//cree un nouvel objet StationFailureEvent avec la "vraie" heure de depart et de fin
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

	/**traite un camion pour une duree determinee
	 * 
	 * @param c camion a traiter
	 * @param temps duree de traitement
	 */
	private void traiteCamion(Camion c, double temps) {

		if(c.getState() == Camion.ETAT_INACTIF) {
			Station s = decisionMaker.giveObjectiveToCamion(c);
			c.setObjective(s);

			c.setPredictedTravelTime(this.travelTimePredictor.predictTravelTime(c.getOrigine(), s, c));
			traiteCamion(c, temps);
		}
		//si le camion est en route, on le fait rouler vers sa destination. Si il arrive a destination avant la fin du tour, on le traite immediatement  a nouveau.
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

	/**traite un camion en attente pour une duree determinee
	 * 
	 * @param c camion en attente
	 * @param temps duree de traitement
	 */
	private void traiteCamionEnAttente(Camion c, double temps) {
		Camion camionEnTraitement = c.getCurrentStation().getCamionEnTraitement();
		if(camionEnTraitement == null) {
			throw new IllegalStateException("On ne peut pas traiter un camion en attente si aucun camion n'est en traitement");
		}
		c.attend(temps);
	}

	/**traite un camion qui est en route pour une duree determinee
	 * 
	 * @param c camion en route
	 * @param temps temps de traitement
	 */
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

	/**Traite un camion en traitement (Remplissage ou decharge) pour une duree determinee
	 * 
	 * @param c camion en traitement
	 * @param temps duree
	 */
	private void traiteCamionEnTraitement(Camion c, double temps) {
		//1) remplis/vide le camion autant que possible
		//2) fait attendre les camions de la file d'attente d'autant
		//3) si le camion a termine, 
		//		- on choisit le nouveau camion en traitement.
		//		- on donne un nouvel objectif au camion (qui devient en route). 

		//trouve le temps de traitement maximum. Il s'agit du max entre : 
		//	- le temps restant a la pelle
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
			c.setPredictedTravelTime(this.travelTimePredictor.predictTravelTime(c.getOrigine(), objective, c));
			if(!modeCharge) {
				this.justAssigned = true;
			}

		}

	}

	/**
	 * Effectue le warmup de la mine. simule un certain temps avant de commencer la "vraie" simulation
	 */
	private void warmup() {
		//effectue les steps de warmup aleatoirement
		//
		String decisionFunctionSmallCamion = decisionMaker.getScoreFunctionSmallCamionsString();
		String decisionFunctionLargeCamion = decisionMaker.getScoreFunctionLargeCamionsString();
		decisionMaker.setScoreFunctionSmallCamionsString(DecisionMaker.WARMUP_SCORE_FUNCTION_STRING);
		decisionMaker.setScoreFunctionLargeCamionsString(DecisionMaker.WARMUP_SCORE_FUNCTION_STRING);

		// Effectue les steps
		//
		mine.setInWarmup(true);
		for(int i = 0 ; i < MineSimulator.NB_WARMUP_STEPS; i++) {
			step();
		}

		//desactive le mode warmup
		//
		mine.setInWarmup(false);
		decisionMaker.setScoreFunctionSmallCamionsString(decisionFunctionSmallCamion);
		decisionMaker.setScoreFunctionLargeCamionsString(decisionFunctionLargeCamion);

		//calcule le temps moyen d'attente des pelles
		//
		//decisionMaker.setCibleTempsAttentePelle(mine.cibleTempsAttente());

		//reset les statistiques
		mine.resetAllStats();
	}

	/**Set l'objet TravelTimePredictor
	 * 
	 * @param predictor Objet
	 */
	protected void setTravelTimePredictor(TravelTimePredictor predictor) {

		this.travelTimePredictor = predictor;
	}


}
