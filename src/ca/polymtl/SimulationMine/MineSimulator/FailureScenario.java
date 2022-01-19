package ca.polymtl.SimulationMine.MineSimulator;

import java.util.ArrayList;
/**
 * @author Fred
 *
 * D�finit un sc�nario de pannes pour une journ�e (de 0:00 � 24:00)
 * Chaque sc�nario est caract�ris� par une liste d'�checs de pelles.
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
