/**
 * 
 */
package ca.polymtl.SimulationMine.MineSimulator;

/**
 * @author Fred
 *
 *Décrit une panne d'une pelle pour une journée quelconque. 
 */
public class StationFailureEvent {
	
	//début de l'événement, selon le nombre de secondes depuis le début de la journée (donc 0 = minuit, 3600 = 1:00, etc)
	long timeBegin;
	//durée de la panne
	long duration;
	//station affectée
	Station station;
	
	StationFailureEvent(long timeBegin, long duration, Station station){
		this.timeBegin = timeBegin;
		this.duration = duration;
		this.station = station;
	}

	public long getTimeBegin() {
		return timeBegin;
	}

	public long getDuration() {
		return duration;
	}

	public Station getStation() {
		return station;
	}
}
