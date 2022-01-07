package ca.polymtl.SimulationMine;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class Config {

	/**Thème */
	private String theme;
	/**Nom de la mine a charger*/
	private String defaultMineId;
	/**Fonction de score */
	private String defaultScoreFunctionSmallCamions;
	private String defaultScoreFunctionLargeCamions;
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
	private double affectDiscountFactor;
	/**
	 * Constructeur
	 */
	public Config() {

		try {
			readConfigFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("petits camions : "+this.getDefaultScoreFunctionSmallCamions());
		System.out.println("gros camions : "+this.getDefaultScoreFunctionLargeCamions());
	}

	private void readConfigFile() throws IOException {

		String stringPattern = "\".*\"";
		Scanner sc = new Scanner(new File("config"));
		//pour que le point délimite la pratie fractionnaire
		sc.useLocale(Locale.US);
		
		while(sc.hasNext()) {
			String token = sc.next();
			System.out.println(token);
			sc.next(":");

			switch(token){
			case "theme":
				String theme = sc.next(stringPattern);
				this.theme= theme.substring(1, theme.length()-1);
				break;
			case "default_mine_id":
				String defaultMineId = sc.next(stringPattern);
				this.defaultMineId = defaultMineId.substring(1, defaultMineId.length()-1);
				break;
			case "default_score_function_small_camions":
				String defaultScoreFunctionSmallCamions = sc.next(stringPattern);
				this.defaultScoreFunctionSmallCamions = defaultScoreFunctionSmallCamions.substring(1, defaultScoreFunctionSmallCamions.length()-1);
				break;
			case "default_score_function_large_camions":
				String defaultScoreFunctionLargeCamions = sc.next(stringPattern);
				this.defaultScoreFunctionLargeCamions = defaultScoreFunctionLargeCamions.substring(1, defaultScoreFunctionLargeCamions.length()-1);
				break;
				
				
			case "default_time_predict_formula":
				this.defaultTimePredictFormula = sc.nextInt();
				break;
			case "default_time_predict_n":
				this.defaultTimePredictN = sc.nextInt();
				break;
			case "default_time_predict_lambda":
				this.defaultTimePredictLambda = sc.nextDouble();
				break;
			case "default_simulation_speed":
				this.defaultSimultaionSpeed = sc.nextInt();
				break;
			case "default_meteo" : 
				this.defaultMeteo = sc.nextInt();
				break;
			case "affect_discount_factor" : 
				System.out.println("ici");
				this.affectDiscountFactor = sc.nextDouble();
				break;	
			case "default_pause_fin_voyage":
				int val = sc.nextInt();
				if(val == 1) {
					this.defaultPauseFinVoyage = true;
				}
				else if (val == 0) {
					this.defaultPauseFinVoyage = false;
				}
				else {
					throw new IOException("le parametre \"default_pause_fin_voyage\" doit etre 0 ou 1");
				}
				break;
			default : 
				throw new IOException("je ne reconnais pas le parametre "+token);
			}
		}
		sc.close();
	}
	


public String getTheme() {
	return theme;
}

public String getDefaultMineId() {
	return defaultMineId;
}

public String getDefaultScoreFunctionSmallCamions() {
	return defaultScoreFunctionSmallCamions;
}

public String getDefaultScoreFunctionLargeCamions() {
	return defaultScoreFunctionLargeCamions;
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

public double getAffectDiscountFactor() {
	return this.affectDiscountFactor;
}
}
