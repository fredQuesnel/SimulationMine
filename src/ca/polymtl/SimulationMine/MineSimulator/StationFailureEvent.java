/**
 * 
 */
package ca.polymtl.SimulationMine.MineSimulator;

/**
 * @author Fred
 *
 *D�crit une panne d'une pelle pour une journ�e quelconque. 
 */
public class StationFailureEvent {
	
	//d�but de l'�v�nement, selon le nombre de secondes depuis le d�but de la journ�e (donc 0 = minuit, 3600 = 1:00, etc)
	long timeBegin;
	//dur�e de la panne
	long duration;
	//station affect�e
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
