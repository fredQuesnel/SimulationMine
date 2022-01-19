package ca.polymtl.SimulationMine.MineSimulator;

import java.util.ArrayList;
/**
 * @author Fred
 *
 * Définit un scénario de pannes pour une journée (de 0:00 à 24:00)
 * Chaque scénario est caractérisé par une liste d'échecs de pelles.
 */
public class FailureScenario {

	/**Evenements de pannes dans le scenario*/
	private ArrayList<StationFailureEvent> failureEvents;
	
	/**
	 * constructeur
	 */
	FailureScenario(){
		this.failureEvents = new ArrayList<StationFailureEvent>();
	}
	
	/**Ajoute une panne au scenario*/
	void addStationFailureEvent(StationFailureEvent event) {
		failureEvents.add(event);
	}
	
	/**Retourne les pannes du scenario*/
	ArrayList<StationFailureEvent> getFailureEvents(){
		return failureEvents;
	}
}
