package ca.polymtl.SimulationMine;



public class Config {

	/**Thème */
	private String theme;
	/**Nom de la mine a charger*/
	private int defaultMineId;
	/**Fonction de score */
	private String defaultScoreFunction;
	/**Formule de prediction de temps de parcours*/
	private int defaultTimePredictFormula;
	/** Valeur de N dans la formule de prediction de temps de parcours*/
	private int defaultTimePredictN;
	/** Valeur de lambda dans la formule de prediction de temps de parcours*/
	private double defaultTimePredictLambda;
	/** Facteur meteo par defaut*/
	private int defaultMeteo;
	/** Vitesse de simulation*/
	private int defaultSimultaionSpeed;
	/**Pause a la fin de voyage*/
	private boolean defaultPauseFinVoyage;
	
	/**
	 * Constructeur
	 */
	public Config() {
		this.theme="normal";
		this.defaultMineId=0;
		this.defaultScoreFunction="aleatoire";
		this.defaultTimePredictFormula=0;
		this.defaultTimePredictN=4;
		this.defaultTimePredictLambda=0.5;
		this.defaultMeteo=50;
		this.defaultSimultaionSpeed=50;
		this.defaultPauseFinVoyage=false;
	}
}
