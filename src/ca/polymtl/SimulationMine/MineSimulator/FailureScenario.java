package ca.polymtl.SimulationMine.MineSimulator;

import java.util.ArrayList;
/**
 * @author Fred
 *
 * D�finit un sc�nario de pannes pour une journ�e (de 0:00 � 24:00)
 * Chaque sc�nario est caract�ris� par une liste d'�checs de pelles.
 */
public class FailureScenario {

	private ArrayList<StationFailureEvent> failureEvents;
	
	//constructeur
	FailureScenario(){
		this.failureEvents = new ArrayList<StationFailureEvent>();
	}
	
	void addPelleFailureEvent(StationFailureEvent event) {
		failureEvents.add(event);
	}
	
	ArrayList<StationFailureEvent> getFailureEvents(){
		return failureEvents;
	}
}
