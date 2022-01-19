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
	long beginTimeSec;
	//temps de fin de l'événement (la durée totale est donc endTimeSec - beginTimeSec) 
	long endTimeSec;
	//station affectée
	Station station;
	
	StationFailureEvent(long beginTimeSec, long endTimeSec, Station station){
		this.beginTimeSec = beginTimeSec;
		this.endTimeSec = endTimeSec;
		this.station = station;
	}

	/**
	 * 
	 * @return Temps de debut de l'evenement
	 */
	public long getBeginTimeSec() {
		return beginTimeSec;
	}

	/**
	 * 
	 * @return Temps de fin de l'evenement
	 */
	public long getEndTimeSec() {
		return endTimeSec;
	}

	/**
	 * 
	 * @return station concernee par l'evenement
	 */
	public Station getStation() {
		return station;
	}
}
