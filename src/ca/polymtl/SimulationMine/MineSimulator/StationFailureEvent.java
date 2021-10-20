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
	long beginTimeSec;
	//temps de fin de l'�v�nement (la dur�e totale est donc endTimeSec - beginTimeSec)
	long endTimeSec;
	//station affect�e
	Station station;
	
	StationFailureEvent(long beginTimeSec, long endTimeSec, Station station){
		this.beginTimeSec = beginTimeSec;
		this.endTimeSec = endTimeSec;
		this.station = station;
	}

	public long getBeginTimeSec() {
		return beginTimeSec;
	}

	public long getEndTimeSec() {
		return endTimeSec;
	}

	public Station getStation() {
		return station;
	}
}
