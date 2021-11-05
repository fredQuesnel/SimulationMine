package ca.polymtl.SimulationMine;



public class Config {

	/**Thème */
	private String theme;
	/**Nom de la mine a charger*/
	private String defaultMineId;
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
		this.theme="espace";
		this.defaultMineId="10pellessanspannes";
		this.defaultScoreFunction="aleatoire";
		this.defaultTimePredictFormula=0;
		this.defaultTimePredictN=4;
		this.defaultTimePredictLambda=0.5;
		this.defaultMeteo=50;
		this.defaultSimultaionSpeed=50;
		this.defaultPauseFinVoyage=false;
	}

	public String getTheme() {
		return theme;
	}

	public String getDefaultMineId() {
		return defaultMineId;
	}

	public String getDefaultScoreFunction() {
		return defaultScoreFunction;
	}

	public int getDefaultTimePredictFormula() {
		return defaultTimePredictFormula;
	}

	public int getDefaultTimePredictN() {
		return defaultTimePredictN;
	}

	public double getDefaultTimePredictLambda() {
		return defaultTimePredictLambda;
	}

	public int getDefaultMeteo() {
		return defaultMeteo;
	}

	public int getDefaultSimultaionSpeed() {
		return defaultSimultaionSpeed;
	}

	public boolean isDefaultPauseFinVoyage() {
		return defaultPauseFinVoyage;
	}
}
