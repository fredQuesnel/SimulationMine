package ca.polymtl.SimulationMine;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;
/**
 * Classe contenant les informations du fichier de configuration. 
 * @author Fred
 *
 */
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
	/**Facteur d'actualisation (discount factor) pour le camion a affecter*/
	private double affectDiscountFactor;
	/**Nombre par defaut de petits camions dans la mine*/
	private int defaultNbCamionsSmall;
	/**Nombre par defaut de gros camions dans la mine*/
	private int defaultNbCamionsLarge;
	/**Temps de simulation par defaut*/
	private int defaultSimulationTimeSeconds;
	
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
	}

	/**
	 * Lis le fichier de configuration. 
	 * TODO : Verifier que toutes les infomations nécessaires sont bien extraites! Je laisse ca pour l'an prochain!
	 * 
	 * @throws IOException
	 */
	private void readConfigFile() throws IOException {

		String stringPattern = "\".*\"";
		Scanner sc = new Scanner(new File("config"));
		//pour que le point délimite la pratie fractionnaire
		sc.useLocale(Locale.US);
		
		while(sc.hasNext()) {
			String token = sc.next();
			sc.next(":");
			System.out.println("token "+token);

			switch(token){
			case "theme":
				String theme = sc.next(stringPattern);
				this.theme= theme.substring(1, theme.length()-1);
				break;
			case "default_mine_id":
				String defaultMineId = sc.next(stringPattern);
				this.defaultMineId = defaultMineId.substring(1, defaultMineId.length()-1);
				break;
			case "default_camions_small":
				System.out.println("default_camions_small ");
				this.defaultNbCamionsSmall = sc.nextInt();
				break;
			case "default_camions_large":
				this.defaultNbCamionsLarge = sc.nextInt();
				break;
			case "default_simulation_time_seconds":
				this.defaultSimulationTimeSeconds= sc.nextInt();
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
	

/**Retourne le nom du theme
 * 
 * @return Nom du theme
 */
public String getTheme() {
	return theme;
}

/**Retourne l'id de la mine chargee au depart
 * 
 * @return Id de la mine chargee au depart
 */
public String getDefaultMineId() {
	return defaultMineId;
}

/**Retourne la fonction de score pour les petits camions qui est chargée au depart
 * 
 * @return Fonction de score pour les petits camions au depart.
 */
public String getDefaultScoreFunctionSmallCamions() {
	return defaultScoreFunctionSmallCamions;
}

/**Retourne la fonction de score pour les gros camions qui est chargée au depart
 * 
 * @return fonction de score pour les gros camions qui est chargée au depart
 */
public String getDefaultScoreFunctionLargeCamions() {
	return defaultScoreFunctionLargeCamions;
}

/**Retourne la fonction de prediction qui est utilisee au depart
 * 
 * @return la fonction de prediction qui est utilisee au depart
 */
public int getDefaultTimePredictFormula() {
	return defaultTimePredictFormula;
}

/**Retourne la valeur de N dans la fonction de prediction qui est utilisee au depart (si celle-ci utilise N)
 * 
 * @return la valeur de N dans la fonction de prediction qui est utilisee au depart (si celle-ci utilise N)
 */
public int getDefaultTimePredictN() {
	return defaultTimePredictN;
}
/**Retourne la valeur de lambda dans la fonction de prediction qui est utilisee au depart (si celle-ci utilise lambda)
 * 
 * @return la valeur de lambda dans la fonction de prediction qui est utilisee au depart (si celle-ci utilise lambda)
 */
public double getDefaultTimePredictLambda() {
	return defaultTimePredictLambda;
}

/**Retourne la valeur de la meteo au depart
 * 
 * @return la valeur de la meteo au depart
 */
public int getDefaultMeteo() {
	return defaultMeteo;
}

/**Retourne la vitesse de simulation au depart
 * 
 * @return la vitesse de simulation au depart
 */
public int getDefaultSimultaionSpeed() {
	return defaultSimultaionSpeed;
}

/**Retourne true si on a l'option "pause a la fin de chaque voyage" activee au depart, false sinon
 * 
 * @return true si on a l'option "pause a la fin de chaque voyage" activee au depart, false sinon
 */
public boolean isDefaultPauseFinVoyage() {
	return defaultPauseFinVoyage;
}

/**Retourne le facteur d'actualisation (Discount factor) pour le camion affecte
 * 
 * @return le facteur d'actualisation (Discount factor) pour le camion affecte
 */
public double getAffectDiscountFactor() {
	return this.affectDiscountFactor;
}

/**Retourne le nombre de petits camions au depart
 * 
 * @return le nombre de petits camions au depart
 */
public int getDefaultNbCamionsSmall() {
	return defaultNbCamionsSmall;
}

/**Retourne le nombre de gros camions au depart
 * 
 * @return le nombre de gros camions au depart
 */
public int getDefaultNbCamionsLarge() {
	return defaultNbCamionsLarge;
}
/**Retourne le temps de simulation au depart
 * 
 * @return le temps de simulation au depart
 */
public int getDefaultSimulationTimeSeconds() {
	return defaultSimulationTimeSeconds;
}
}
