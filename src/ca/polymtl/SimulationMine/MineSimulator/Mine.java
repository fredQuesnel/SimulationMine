package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;
import javafx.util.Pair;

/**
 * Classe representant une mine
 * @author Fred
 *
 */
public class Mine {



	/**
	 * 
	 * @author Frederic Quesnel
	 *
	 *	Identifiant d'un exemplaire de mine. 
	 */
	public static class ExampleId{
		
		/**Numero d'identifiant*/
		private String id;
		/** Nom utilise dans le GUI*/
		private String name;
		/** Nom de fichier decrivant la mine*/
		private String fileName;
		
		/** Constructeur. Initialise les champs
		 * 
		 * @param id Identifiant de la mine
		 * @param name nom de la mine
		 * @param fileName fichier decrivant la mine
		 */
		public ExampleId(String id, String name, String fileName){
			this.id = id;
			this.name = name;
			this.fileName = fileName;
		}
		/**
		 * 
		 * @return Nom de fichier de l'exemplaire.
		 */
		public String getFileName() {
			return this.fileName;
		}
		/**
		 * 
		 * @return Identifiant
		 */
		public String getId() {
			return id;
		}
		/**
		 * 
		 * @return Nom
		 */
		public String getName() {
			return name;
		}
	}
	//----------------------------
	// Constantes
	//----------------------------
	/** Largeur de la mine en metres */
	public static double WIDTH = 10000;

	/** Hauteur de la mine.*/
	public static double HEIGHT = 10000;
	
	/** Un jour en secondes*/
	public static long ONE_DAY_SEC = 24*3600;

	/** Facteur meteo */
	protected static double DEFAULT_METEO_FACTOR = 1;


	/**Liste des IDs de mine*/
	public static ArrayList<ExampleId> exampleIds = createExampleIds();
	
	//========================================
	//Champs 
	//========================================


	//-------------------------------------
	// etat de la mine
	/**Numero de jour*/
	private int dayNumber;
	
	/**temps en secondes*/
	private double time;

	/**flag indiquant si on est en warmup*/
	private boolean inWarmup;
	
	/**Facteur meteo*/
	private double meteoFactor;
	
	//------------------------------------
	//modelisation de la mine

	/**Identifiant de la mine courante*/
	private ExampleId currentExampleId;

	/** Liste des scénarios de panne possibles */
	private ArrayList<FailureScenario> failureScenarios;

	
	/** Liste des concentrateurs */
	private ArrayList<Concentrateur> concentrateurs;
	/** Liste des steriles */
	private ArrayList<Sterile> steriles;
	/** Liste des pelles */
	private ArrayList<Pelle> pelles;
	/** Liste des camions */
	private ArrayList<Camion> camions;
	
	/** "handles" des trajets qui seront enregistres*/
	protected ArrayList<String> dataSeriesHandles;

	/** Nombre de "gros" camions (100 t)*/
	private int numberLargeCamions;

	/** Nombre de "petits" camions (60 t)*/
	private int numberSmallCamions;

	/**Liste des routes*/
	private ArrayList<Pair<Station, Station>> routes;

	/**Objet config*/
	private Config config;

	
	/** Cree la liste des configurations de mine. Les configurations sont les fichier dans le dossier "mines" qui ont l'extension ".mine"
	 * 
	 * @return Liste d'objets ExampleId
	 */
	private static ArrayList<ExampleId> createExampleIds() {
		ArrayList<ExampleId> exampleIds = new ArrayList<ExampleId>();
	
		String[] mineFiles;
		File f = new File("mines");
		mineFiles = f.list();
		int currentIndex = 1;
		//Pour chaque fichier dont l'extension est .mine, trouve les identifiants.
		//
		for(int i = 0 ; i < mineFiles.length; i++) {
			if(mineFiles[i].substring(mineFiles[i].length()-5, mineFiles[i].length()).equals(".mine")) {
				System.out.println("Charge le fichier de mine "+mineFiles[i]);
				try {
					Scanner scanner = new Scanner(new File("mines/"+mineFiles[i]));
					
					scanner.next("id");
					scanner.next(":");			
					String id = scanner.findInLine(Pattern.compile("\"(.+)\""));
					
					//enleve les double guillemets
					id = id.substring(1, id.length()-1);
					System.out.println("  ID : "+id);
					
					scanner.next("name");
					scanner.next(":");			
					String name = scanner.findInLine(Pattern.compile("\"(.+)\""));
					//enleve les double guillemets
					name = name.substring(1, name.length()-1);
					System.out.println("  Nom : "+name);
					System.out.println("");
					exampleIds.add(new ExampleId(id, name, mineFiles[i]));
					currentIndex++;
	
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
			}
			else {
				//System.out.println(mineFiles[i].substring(mineFiles[i].length()-4, mineFiles[i].length()));
			}
		}
		return exampleIds;
	}


	//private MineSimulator mineSimulator;
	//------------------------------------------
	/** constructeur qui construit une mine vide
	 * 
	 * @param config objet config
	 */
	//------------------------------------------
	public Mine(Config config) {

		this.config = config;
		this.pelles = new ArrayList<Pelle>();
		this.camions= new ArrayList<Camion>();

		this.meteoFactor = Mine.DEFAULT_METEO_FACTOR;



		this.time = 0;


	}

	 
	/** 
	 * 
	 * @param fractionX coordonnee x relative de la pelle
	 * @param fractionY coordonnee y relative de la pelle
	 * @return Pelle la plus pres  des coordonnees relatives donnees.
	 */
	 
	public Pelle closestPelle(double fractionX, double fractionY) {
		double x = fractionX*Mine.WIDTH;
		double y = fractionY*Mine.HEIGHT;

		Pelle closest = null;
		double smallestDistSquare = Double.MAX_VALUE;

		for(int i = 0 ; i < this.pelles.size(); i++){
			Pelle p = pelles.get(i);
			double distanceSquare = (p.getLocation().getX()-x)*(p.getLocation().getX()-x) + (p.getLocation().getY()-y)*(p.getLocation().getY()-y);
			if(distanceSquare < smallestDistSquare){
				smallestDistSquare = distanceSquare;
				closest = p;
			}
		}

		return closest;
	}



	/**
	 * 
	 * @return ArrayList des camions
	 */
	public ArrayList<Camion> getCamions() {
		return camions;
	}



	/**
	 * 
	 * @return Le concentrateur
	 */
	public ArrayList<Concentrateur> getConcentrateurs() {
		return concentrateurs;
	}


	/**
	 * 
	 * @return Objet ExampleId correspondant a la configuration courante de la mine.
	 */
	public ExampleId getCurrentExampleId() {
		return this.currentExampleId;
	}

	/**
	 * 
	 * @return Les noms des series de donnees qui sont suivies.
	 */
	public ArrayList<String> getDataSeriesHandles() {
		return this.dataSeriesHandles;
	}

	/**
	 * 
	 * @return Numero de la mine active.
	 */
	public String getExemple() {
		return this.currentExampleId.getId();
	}

	/**
	 * 
	 * @return Facteur meteo (entre 50 et 100)
	 */
	public double getMeteoFactor() {
		return meteoFactor;
	}


	/**
	 * 
	 * @return Nom de la mine
	 */
	public String getName() {
		return this.getCurrentExampleId().getName();
	}

	/**
	 * 
	 * @return Nombre de gros camions (100 t)
	 */
	public int getNumberLargeCamions() {
		return this.numberLargeCamions;
	}


	/**
	 * 
	 * @return Nombre de petits camions (60 t)
	 */
	public int getNumberSmallCamions() {
		return this.numberSmallCamions;
	}

	/**
	 * 
	 * @return Liste des pelles
	 */
	public ArrayList<Pelle> getPelles() {
		return pelles;
	}

	/**
	 * 
	 * @return Liste des steriles
	 */
	public ArrayList<Sterile> getSteriles() {
		return steriles;
	}

	/**
	 * 
	 * @return Temps depuis le debut de la simulation (secondes)
	 */
	public double getTime() {
		return time;
	}



	/**
	 * 
	 * @return true si la mine est presenement en warmup (avant le debut d'une simulation), false sinon.
	 */
	public boolean isInWarmup() {
		return this.inWarmup;
	}


	/**
	 * Efface toutes les informations qui doivent etre effaces lorsqu'on reinitialise la mine
	 */
	private void erasePrevious() {
		this.time = 0;
		this.inWarmup = false;
		this.concentrateurs = new ArrayList<Concentrateur>();
		this.steriles = new ArrayList<Sterile>();
		this.pelles = new ArrayList<Pelle>();
		this.camions = new ArrayList<Camion>();
		this.failureScenarios = new ArrayList<FailureScenario>();
		this.numberLargeCamions = 0;
		this.numberSmallCamions = 0;
		this.dayNumber = 0;
		this.dataSeriesHandles = null;

	}


	/** Ajoute du temps a l'horloge 
	 * 
	 * @param time temps a ajouter
	 */
	protected void addTime(double time) {
		double previousTime = this.time;
		this.time+=time;
		
		//incremente le numero du jour si on doit le faire.
		if((int) this.time/Mine.ONE_DAY_SEC > previousTime/Mine.ONE_DAY_SEC ) {
			this.dayNumber++;
		}
	}


	/**initialise la mine en fonction de l'exemple de mine desiree
	 * 
	 * @param exempleId Exemplaire de mine
	 * @param nbSmallCamions Nombre de petits camions
	 * @param nbLargeCamions Nombre de gros camions
	 */
	protected void init(ExampleId exempleId, int nbSmallCamions, int nbLargeCamions) {
		
		//efface la mine precedente
		erasePrevious();

		String failureScenariosFilename = "";

		System.out.println("charge mine "+exempleId.getName());
		
		
		//Initialise les "handles" pour l'affichage des temps de parcours
		this.dataSeriesHandles = new ArrayList<String>();
		
		//Initialise les scenarios de panne
		this.failureScenarios = new ArrayList<FailureScenario>();
		
		//jour courant
		this.dayNumber = 0;

		//retrouve l'objet ExampleId desire
		for(int i = 0 ; i < exampleIds.size(); i++){
			if(exampleIds.get(i).getName().equals(exempleId.getName())){
				this.currentExampleId = exampleIds.get(i);
				break;
			}
		}
		//exception si l'exemple ne correspond a aucun enregistre
		if(this.currentExampleId == null){
			throw new IllegalArgumentException("numero d'exemple : "+exempleId);
		}	

		//Lis le fichier contenant la configuration de la mine
		//
		try {
			System.out.println("mines/"+currentExampleId.getFileName());

			Scanner scanner = new Scanner(new File("mines/"+currentExampleId.getFileName()));
			//pour que le point delimite la pratie fractionnaire
			scanner.useLocale(Locale.US);
			//ignore la premiere ligne
			scanner.nextLine();
			//int defaultLargeCamion = 0;
			//int defaultSmallCamion = 0;


			while(scanner.hasNext()) {
				
				//Nom du fichier de scenarios de panne
				//
				if( scanner.hasNext("failure_scenarios")) {
					scanner.next();
					failureScenariosFilename = scanner.next(Pattern.compile("\".*\""));
					failureScenariosFilename = failureScenariosFilename.substring(1,  failureScenariosFilename.length()-1);
					System.out.println("failure scenarios "+failureScenariosFilename);
				}
				/*
				//Nombre de petits camions
				//
				else if(	scanner.hasNext("default_camions_small")) {
					scanner.next();
					scanner.next(":");
					defaultSmallCamion = scanner.nextInt(); 
				}
				//Nombre de gros camions
				//
				else if(	scanner.hasNext("default_camions_large")) {
					scanner.next();
					scanner.next(":");
					defaultLargeCamion = scanner.nextInt(); 
				}
				*/
				//Lis une pelle
				//
				else if( scanner.hasNext("pelle")) {
					scanner.next();
					scanner.next();//si minerais ou sterile
					String nom = scanner.next(Pattern.compile("\".*\""));
					nom = nom.substring(1, nom.length()-1);
					int posX = scanner.nextInt();
					int posY = scanner.nextInt();
					double tauxFe = scanner.nextDouble();
					double tauxS = scanner.nextDouble();
					double cible = scanner.nextDouble();
					Pelle pelle = new Pelle(posX, posY, nom, cible);
					pelle.setRockType(tauxFe, tauxS);
					pelles.add(pelle);
				}
				//Lis un concentrateur
				//
				else if(scanner.hasNext("concentrateur")) {
					scanner.next();
					String nom  = scanner.next(Pattern.compile("\".*\""));
					nom = nom.substring(1, nom.length()-1);
					int posX = scanner.nextInt();
					int posY = scanner.nextInt();
					Concentrateur concentrateur = new Concentrateur(posX, posY, nom);
					concentrateurs.add( concentrateur );
				}
				//Lis un sterile
				else if(scanner.hasNext("sterile")) {
					scanner.next();
					String nom  = scanner.next(Pattern.compile("\".*\""));
					nom = nom.substring(1, nom.length()-1);
					int posX = scanner.nextInt();
					int posY = scanner.nextInt();
					Sterile sterile = new Sterile(posX, posY, nom);
					steriles.add(sterile); 
				}
				//Lis une paire origine/destination dont on affichera les temps de parcours reels/predits
				else if(scanner.hasNext("display")) {
					scanner.next();
					String nomStation1  = scanner.next(Pattern.compile("\"\\S*\""));
					String nomStation2  = scanner.next(Pattern.compile("\"\\S*\""));


					nomStation1 = nomStation1.substring(1, nomStation1.length()-1);
					nomStation2 = nomStation2.substring(1, nomStation2.length()-1);

					//retrouve les objets station
					//
					Station station1 = null;
					Station station2 = null;
					//regarde les pelles
					for(int i = 0 ; i< pelles.size(); i++) {
						if(pelles.get(i).getId().equals(nomStation1)) {
							station1 = pelles.get(i);
						}
						if(pelles.get(i).getId().equals(nomStation2)) {
							station2 = pelles.get(i);
						}
					}
					//regarde les concentrateurs
					for(int i = 0 ; i< concentrateurs.size(); i++) {
						if(concentrateurs.get(i).getId().equals(nomStation1)) {
							station1 = concentrateurs.get(i);
						}
						if(concentrateurs.get(i).getId().equals(nomStation2)) {
							station2 = concentrateurs.get(i);
						}
					}
					//regarde les steriles
					for(int i = 0 ; i< steriles.size(); i++) {
						if(steriles.get(i).getId().equals(nomStation1)) {
							station1 = steriles.get(i);
						}
						if(steriles.get(i).getId().equals(nomStation2)) {
							station2 = steriles.get(i);
						}
					}

					if(station1==null) {
						throw new Exception("Station non definie : "+nomStation1);
					}
					if(station2==null) {
						throw new Exception("Station non definie : "+nomStation2);
					}

					dataSeriesHandles.add("reel:"+TravelTimePredictor.getMapKeyForODPair(station1, station2 ));
					dataSeriesHandles.add("pred:"+TravelTimePredictor.getMapKeyForODPair(station1, station2 ));
				}
				else {
					scanner.next();
				}


			}

			createRoutes();
			//determine si on utilise le nombre de camions par defaut
			//
			/*
			if(nbSmallCamions == -1) {
				nbSmallCamions = defaultSmallCamion;
			}

			if(nbLargeCamions == -1) {
				nbLargeCamions = defaultLargeCamion;
			}
			*/


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}


		//-----------------------------------------------------------
		// Cree les objets
		//-----------------------------------------------------------
		
		
		BufferedImage smallCamionImage = null;
		BufferedImage largeCamionImage = null;
		//Cree les camions
		//
		try {
			String theme = config.getTheme();
			smallCamionImage = ImageIO.read(new File("images/"+theme+"/camion_small.png"));
			largeCamionImage = ImageIO.read(new File("images/"+theme+"/camion_large.png"));
		} catch (IOException e) {
			String theme = config.getTheme();
			System.out.println("images/"+theme+"/camion_small.png");
			e.printStackTrace();
		}

		this.numberSmallCamions = nbSmallCamions;
		this.numberLargeCamions = nbLargeCamions;
		//Cree les petits camions
		//
		for(int i = 0 ; i < nbSmallCamions; i++) {
			Camion camion = new SmallCamion(steriles.get(0), this, smallCamionImage);
			camions.add(camion);
		}

		//Cree les gros camions
		//
		for(int i = 0 ; i < nbLargeCamions; i++) {
			Camion camion = new LargeCamion(steriles.get(0), this, largeCamionImage);
			camions.add(camion);
		}

		//cree les "failureScenarios"
		//
		if(!failureScenariosFilename.equals("")) {
			createFailureScenarios(failureScenariosFilename);
		}
	}

	/**
	 * Cree les routes. Il y a une route entre :
	 *  - Chaque concentrateur et chaque pelle.
	 *  - Chaque sterile et chaque pelle.
	 */
	private void createRoutes() {
		routes = new ArrayList<Pair<Station, Station>>();
		
		for(int i = 0 ; i < concentrateurs.size(); i++) {
			for(int j = 0 ; j < pelles.size(); j++) {
				routes.add(new Pair<Station, Station>(concentrateurs.get(i), pelles.get(j)));
			}
		}
		for(int i = 0 ; i < steriles.size(); i++) {
			for(int j = 0 ; j < pelles.size(); j++) {
				routes.add(new Pair<Station, Station>(steriles.get(i), pelles.get(j)));
				
			}
		}		
	}


	/**
	 * Cree les scenarios de panne a partir d'un fichier.
	 * @param failureScenariosFilename : nom du fichier contenantl es scenarios de panne.
	 */
	//
	private void createFailureScenarios(String failureScenariosFilename) {
		
		//reset
		this.failureScenarios = new ArrayList<FailureScenario>();
		//Lis le fichier
		//
		try {

			Scanner scanner = new Scanner(new File("mines/"+failureScenariosFilename));
			//pour que le point délimite la pratie fractionnaire
			scanner.useLocale(Locale.US);

			String line;
			//chaque ligne  représentee un scénario
			//
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				Scanner lineScanner = new Scanner(line);
				lineScanner.useLocale(Locale.US);
				
				FailureScenario fs = new FailureScenario();
				
				//lis chaque panne du scénario
				//
				while(lineScanner.hasNext("\".*\"")) {
					String stationName = lineScanner.next("\".*\"");
					stationName = stationName.substring(1, stationName.length()-1);
					Station station = getStation(stationName); 
					if( station == null) {
						lineScanner.close();
						throw new RuntimeException("La station "+stationName+" n'existe pas.");
					}
					String heureString = lineScanner.next(".+:.+");
					int nbHours = Integer.parseInt(heureString.substring(0, heureString.indexOf(":")));
					int nbMinutes = Integer.parseInt(heureString.substring(heureString.indexOf(":")+1));
				
					long beginTimeSec = nbHours*3600+nbMinutes*60;
							
					long endTimeSec = beginTimeSec + lineScanner.nextLong();
					
					fs.addStationFailureEvent(new StationFailureEvent(beginTimeSec, endTimeSec, station));
					this.failureScenarios.add(fs);
					
					if(lineScanner.hasNext(",")) {
						lineScanner.next(",");
					}

				}
				lineScanner.close();

			}

			scanner.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param stationId
	 * @return station dont l'ID correspond à l'argument, ou null si une telle station n'existe pas.
	 */
	private Station getStation(String stationId) {
		Station s = null;
		//recherche dans les pelles
		s = getPelle(stationId);
		//sinon recherche dans les steriles
		if(s == null) {
			s= getSterile(stationId);
		}

		if(s == null) {
			s = getConcentrator(stationId);
		}

		return s;
	}



	/**
	 * @param stationId
	 * @return Concentrateur dont l'ID correspond à celui fourni, ou null si un tel concentrateur n'existe pas.
	 */
	private Station getConcentrator(String stationId) {
		for(int i = 0 ; i < this.concentrateurs.size(); i++) {
			if(this.concentrateurs.get(i).getId().compareTo(stationId)==0) {
				return this.concentrateurs.get(i);
			}
		}
		return null;
	}



	/**
	 * @param stationId
	 * @return sterile dont l'ID correspond à celui fourni, ou null si un tel sterile n'existe pas.
	 */
	private Station getSterile(String stationId) {
		for(int i = 0 ; i < this.steriles.size(); i++) {
			if(this.steriles.get(i).getId().compareTo(stationId)==0) {
				return this.steriles.get(i);
			}
		}
		return null;
	}

	/**
	 * 
	 * @param stationId
	 * @return Pelle correspondant à l'ID fourni, ou null si une telle pelle n'existe pas.
	 */
	private Station getPelle(String stationId) {
		for(int i = 0; i < this.getPelles().size(); i++) {
			if(this.getPelles().get(i).getId().compareTo(stationId) == 0) {
				return this.getPelles().get(i);
			}
		}
		return null;
	}


	/**
	 * reset toutes les stats de la mine et de ses objets
	 * */
	protected void resetAllStats() {

		//reset les stats de la mine
		//
		this.resetTime();

		//reset les stats des camions
		//
		for(int i = 0 ; i < this.camions.size(); i++) {
			camions.get(i).resetStats();
		}

		//reset les stats des pelles
		//
		for(int i = 0 ; i < this.pelles.size(); i++) {
			pelles.get(i).resetStats();
		}
		for(int i = 0 ; i < concentrateurs.size(); i++) {
			concentrateurs.get(i).resetStats();
		}
		for(int i = 0 ; i < steriles.size(); i++) {
			steriles.get(i).resetStats();
		}

	}

	/**met le temps a 0*/
	protected void resetTime() {
		this.time = 0;
		this.dayNumber = 0;
	}





	/**Change le status "en warmup" de la mine
	 * 
	 * @param inWarmup Nouveau status
	 */
	protected void setInWarmup(boolean inWarmup) {
		this.inWarmup = inWarmup;
	}


	/**Change le facteur meteo de la mine
	 * 
	 * @param meteoFactor nouveau facteur meteo
	 */
	protected void setMeteoFactor(double meteoFactor) {
		this.meteoFactor = meteoFactor;
	}

	/** Retourne le jour courant
	 * 
	 * @return jour courant
	 */
	public int getDayNumber() {
		return this.dayNumber;
	}


	/**choisis un scenario de pannes au hasard
	 * 
	 * @return scenario de pannes
	 */
	public FailureScenario getRandomFailureScenario() {
		
		FailureScenario fs = null;
		if(failureScenarios.size() > 0) {
		int index = (int) (Math.random()*failureScenarios.size());
		fs = failureScenarios.get(index);
		}
		return fs;
	}


	/**
	 *	Détermine si il y a une route entre la station 1 et la station 2. 
	 * @param station1 station 1
	 * @param station2 station 2
	 * @return true si il y a une route entre la station1 et la station2, false sinon.
	 */
	public boolean routeEntre(Station station1, Station station2) {
		
		for(int i = 0 ; i < routes.size(); i++) {
			if(routes.get(i).getKey().equals(station1) && routes.get(i).getValue().equals(station2) ||
					routes.get(i).getKey().equals(station2) && routes.get(i).getValue().equals(station1)	) {
				return true;
			}
			
		}
		return false;
	}


}
