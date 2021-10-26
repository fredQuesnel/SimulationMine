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

import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;


public class Mine {




	public static class ExampleId{

		private int id;
		private String name;
		private String fileName;
		public ExampleId(int id, String name, String fileName){
			this.id = id;
			this.name = name;
			this.fileName = fileName;
		}
		public String getFileName() {
			return this.fileName;
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
	}
	//----------------------------
	// Constantes
	//----------------------------
	/**
	 * dimensions de la mine en metres
	 */
	public static double WIDTH = 10000;

	public static double HEIGHT = 10000;
	
	public static long ONE_DAY_SEC = 24*3600;

	/**
	 * facteur m�t�o
	 */
	protected static double DEFAULT_METEO_FACTOR = 1;




	//nombre de pas d'increment avant le debut de l'affichage
	protected static int NB_WARMUP_STEPS = 300;

	//numeros d'exemples
	public static ArrayList<ExampleId> exampleIds = createExampleIds();



	//========================================
	//Champs 
	//========================================

	private static ArrayList<ExampleId> createExampleIds() {
		ArrayList<ExampleId> exampleIds = new ArrayList<ExampleId>();

		String[] mineFiles;
		File f = new File("mines");
		mineFiles = f.list();
		int currentIndex = 1;
		for(int i = 0 ; i < mineFiles.length; i++) {
			if(mineFiles[i].substring(mineFiles[i].length()-5, mineFiles[i].length()).equals(".mine")) {
				System.out.println(mineFiles[i]);
				try {
					Scanner scanner = new Scanner(new File("mines/"+mineFiles[i]));
					//System.out.println(scanner.next());
					scanner.next("id");
					scanner.next(":");			
					String id = scanner.findInLine(Pattern.compile("\"(.+)\""));
					//enleve les double guillemets
					id = id.substring(1, id.length()-1);
					System.out.println(id);

					exampleIds.add(new ExampleId(currentIndex, id, mineFiles[i]));
					currentIndex++;

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println(mineFiles[i].substring(mineFiles[i].length()-4, mineFiles[i].length()));
			}
		}


		return exampleIds;
	}

	//nombre de steps dans la simulation

	private int dayNumber;
	//temps
	private double time;

	//flag indiquant si on est en warmup
	private boolean inWarmup;
	//modelisation de la mine
	//
	private int exemple; //numero d'exemple


	private String name; //nom de l'exemple



	private ArrayList<Concentrateur> concentrateurs;



	private ArrayList<Sterile> steriles;
	private ArrayList<Pelle> pelles;
	private ArrayList<Camion> camions;
	private double meteoFactor;
	// Graphe
	//
	protected ArrayList<String> dataSeriesHandles;



	private ExampleId currentExampleId;

	private int numberLargeCamions;

	private int numberSmallCamions;

	private ArrayList<FailureScenario> failureScenarios;

	//private MineSimulator mineSimulator;
	//------------------------------------------
	// constructeur qui construit une mine vide
	//------------------------------------------
	public Mine() {

		this.pelles = new ArrayList<Pelle>();
		this.camions= new ArrayList<Camion>();

		this.meteoFactor = Mine.DEFAULT_METEO_FACTOR;



		this.time = 0;


	}






	//retourne la pelle la plus pres des coordonnees relatives fournies
	/**
	 * 
	 * @return La  pelle la plus pr�s des coordonn�es relatives donn�es.
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



	public ExampleId getCurrentExampleId() {
		return this.currentExampleId;
	}



	/**
	 * 
	 * @return Les noms des s�ries de donn�es qui sont suivies.
	 */
	public ArrayList<String> getDataSeriesHandles() {
		return this.dataSeriesHandles;
	}



	/**
	 * 
	 * @return Num�ro de la mine active
	 */
	public int getExemple() {
		return exemple;
	}


	/**
	 * 
	 * @return Facteur m�t�o (entre 50 et 100)
	 */
	public double getMeteoFactor() {
		return meteoFactor;
	}


	/**
	 * 
	 * @return Nom de la mine
	 */
	public String getName() {
		return name;
	}



	/*
	private void printHistoriqueTempsParcours(Station station1, Station station2) {

		String ODKey = getMapKeyForODPair(station1, station2);
		ArrayList<Double> tempsReels = this.historyMap.get(ODKey);
		ArrayList<Double> tempsPredits = this.predictionMap.get(ODKey);

		System.out.println(" Historique    Prediction");
		if(tempsReels!=null && tempsPredits!=null && tempsReels.size() == tempsPredits.size()) {
			for(int i = 0 ; i < tempsReels.size(); i++) {
				System.out.println(tempsReels.get(i)+"    "+tempsPredits.get(i));
			}
		}
		else {
			System.out.println("ERREUR");
		}

	}

	 */




	public int getNumberLargeCamions() {
		return this.numberLargeCamions;
	}



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
	 * @return Station de st�rile
	 */
	public ArrayList<Sterile> getSteriles() {
		return steriles;
	}

	/**
	 * 
	 * @return Temps depuis le d�but de la simulation (secondes)
	 */
	public double getTime() {
		return time;
	}



	/**
	 * 
	 * @return true si la mine est pr�senement en warmup (avant le d�but d'une simulation), false sinon.
	 */
	public boolean isInWarmup() {
		return this.inWarmup;
	}




	//efface toutes les informations qui doivent etre effaces lorsqu'on reinitialise la mine
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



	protected void addTime(double time) {
		double previousTime = this.time;
		this.time+=time;
		
		if((int) this.time/Mine.ONE_DAY_SEC > previousTime/Mine.ONE_DAY_SEC ) {
			this.dayNumber++;
		}
		
	}



	//initialise la mine en fonction de l'exemple de mine desiree avec le nombre de camions par defaut
	protected void init(ExampleId exempleId) {
		init(exempleId, -1, -1);
	}
	//initialise la mine en fonction de l'exemple de mine desiree
	protected void init(ExampleId exempleId, int nbSmallCamions, int nbLargeCamions) {
		erasePrevious();

		String failureScenariosFilename = "";

		System.out.println("charge mine "+exempleId.getName());

		this.dataSeriesHandles = new ArrayList<String>();
		this.failureScenarios = new ArrayList<FailureScenario>();
		this.dayNumber = 0;

		//retrouve l'objet ExampleId de l'exemple
		ExampleId exId = null;
		for(int i = 0 ; i < exampleIds.size(); i++){
			System.out.println("Compare "+exampleIds.get(i).getName()+" "+exampleIds.get(i).getId());
			if(exampleIds.get(i).getName().equals(exempleId.getName())){
				exId = exampleIds.get(i);
				break;
			}
		}
		this.currentExampleId = exId;
		//exception si l'exemple ne correspond a aucun enregistre
		if(this.currentExampleId == null){
			throw new IllegalArgumentException("numero d'exemple : "+exempleId);
		}	

		//Lis le fichier
		//
		try {
			System.out.println("mines/"+currentExampleId.getFileName());

			Scanner scanner = new Scanner(new File("mines/"+currentExampleId.getFileName()));
			//pour que le point d�limite la pratie fractionnaire
			scanner.useLocale(Locale.US);
			//ignore la premiere ligne
			scanner.nextLine();
			int defaultLargeCamion = 0;
			int defaultSmallCamion = 0;


			while(scanner.hasNext()) {
				if( scanner.hasNext("failure_scenarios")) {
					scanner.next();
					failureScenariosFilename = scanner.next(Pattern.compile("\".*\""));
					failureScenariosFilename = failureScenariosFilename.substring(1,  failureScenariosFilename.length()-1);
					System.out.println("failure scenarios "+failureScenariosFilename);
				}
				else if(	scanner.hasNext("default_camions_small")) {
					scanner.next();
					scanner.next(":");
					defaultSmallCamion = scanner.nextInt(); 
				}
				else if(	scanner.hasNext("default_camions_large")) {
					scanner.next();
					scanner.next(":");
					defaultLargeCamion = scanner.nextInt(); 
				}
				else if( scanner.hasNext("pelle")) {
					scanner.next();
					scanner.next();//si minerais ou st�rile
					String nom = scanner.next(Pattern.compile("\".*\""));
					nom = nom.substring(1, nom.length()-1);
					int posX = scanner.nextInt();
					int posY = scanner.nextInt();
					double tauxFe = scanner.nextDouble();
					double tauxS = scanner.nextDouble();
					double cible = scanner.nextDouble();
					Pelle pelle = new Pelle(posX, posY, nom, cible);
					pelle.setRockType(tauxFe, tauxS);
					System.out.println(nom+" "+posX+" "+posY+" "+tauxFe+" "+tauxS+" "+cible);
					pelles.add(pelle);
				}
				else if(scanner.hasNext("concentrateur")) {
					scanner.next();
					String nom  = scanner.next(Pattern.compile("\".*\""));
					nom = nom.substring(1, nom.length()-1);
					int posX = scanner.nextInt();
					int posY = scanner.nextInt();
					Concentrateur concentrateur = new Concentrateur(posX, posY, nom);
					concentrateurs.add( concentrateur );
				}
				else if(scanner.hasNext("sterile")) {
					scanner.next();
					String nom  = scanner.next(Pattern.compile("\".*\""));
					nom = nom.substring(1, nom.length()-1);
					int posX = scanner.nextInt();
					int posY = scanner.nextInt();
					Sterile sterile = new Sterile(posX, posY, nom);
					steriles.add(sterile); 
				}
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
						throw new Exception("Station non d�finie : "+nomStation1);
					}
					if(station2==null) {
						throw new Exception("Station non d�finie : "+nomStation2);
					}

					dataSeriesHandles.add("reel:"+TravelTimePredictor.getMapKeyForODPair(station1, station2 ));
					dataSeriesHandles.add("pred:"+TravelTimePredictor.getMapKeyForODPair(station1, station2 ));
				}
				else {
					scanner.next();
				}


			}


			if(nbSmallCamions == -1) {
				nbSmallCamions = defaultSmallCamion;
			}

			if(nbLargeCamions == -1) {
				nbLargeCamions = defaultLargeCamion;
			}


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}



		this.name = this.currentExampleId.getName();

		this.exemple = this.currentExampleId.getId();

		BufferedImage smallCamionImage = null;
		BufferedImage largeCamionImage = null;
		//Cree les camions
		//
		try {
			smallCamionImage = ImageIO.read(new File("images/camion_small.png"));
			largeCamionImage = ImageIO.read(new File("images/camion_large.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.numberSmallCamions = nbSmallCamions;
		this.numberLargeCamions = nbLargeCamions;
		for(int i = 0 ; i < nbSmallCamions; i++) {
			Camion camion = new Camion(steriles.get(0), this, smallCamionImage) {

				/** Vitesse moyenne du camion	 */
				public static final double VITESSE_MOYENNE = 7.5;
				/** �cart type sur la vitesse du camion	 */
				private static final double ECART_TYPE_VITESSE = 0.6;//ancien 0.5
				/** Charge maximum du camion.	 */
				public static final double CHARGE_MAX = 60.;

				private static final double PREDICT_TIME_ADJUST_FACTOR = 1.; 

				@Override
				public double getAvgSpeed() {
					return VITESSE_MOYENNE;
				}

				@Override
				public double getChargeMax() {
					return CHARGE_MAX;
				}

				@Override
				public double getPredictTimeAdjustFactor() {
					return PREDICT_TIME_ADJUST_FACTOR;
				}

				@Override
				public double getStdSpeed() {
					return ECART_TYPE_VITESSE;
				}

			};
			camions.add(camion);
		}

		for(int i = 0 ; i < nbLargeCamions; i++) {
			Camion camion = new Camion(steriles.get(0), this, largeCamionImage) {

				/** Vitesse moyenne du camion	 */
				public static final double VITESSE_MOYENNE = 5;
				/** �cart type sur la vitesse du camion	 */
				private static final double ECART_TYPE_VITESSE = 0.3;//ancien 0.5
				/** Charge maximum du camion.	 */
				public static final double CHARGE_MAX = 100.;

				private static final double PREDICT_TIME_ADJUST_FACTOR = 9./5;
				@Override
				public double getAvgSpeed() {
					return VITESSE_MOYENNE;
				}

				@Override
				public double getChargeMax() {
					return CHARGE_MAX;
				}

				@Override
				public double getPredictTimeAdjustFactor() {
					return PREDICT_TIME_ADJUST_FACTOR;
				}

				@Override
				public double getStdSpeed() {
					return ECART_TYPE_VITESSE;
				}


			};
			camions.add(camion);
		}

		//cree les "failureScenarios"
		if(!failureScenariosFilename.equals("")) {
			createFailureScenarios(failureScenariosFilename);
		}
	}

	//load le fichier d�crivant les sc�narios de panne
	//
	private void createFailureScenarios(String failureScenariosFilename) {
		
		//reset
		this.failureScenarios = new ArrayList<FailureScenario>();
		//Lis le fichier
		//
		try {
			System.out.println("mines/"+failureScenariosFilename);

			Scanner scanner = new Scanner(new File("mines/"+failureScenariosFilename));
			//pour que le point d�limite la pratie fractionnaire
			scanner.useLocale(Locale.US);

			String line;
			//chaque ligne  repr�sentee un sc�nario
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				Scanner lineScanner = new Scanner(line);
				lineScanner.useLocale(Locale.US);
				
				FailureScenario fs = new FailureScenario();
				
				//lis chaque "failure" du sc�nario
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
					System.out.println(stationName+" "+heureString+" "+endTimeSec);
					
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
	 * @return station dont l'ID correspond � l'argument, ou null si une telle station n'existe pas.
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
	 * 
	 * @param stationId
	 * @return Concentrateur dont l'ID correspond � celui fourni, ou null si un tel concentrateur n'existe pas.
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
	 * 
	 * @param stationId
	 * @return sterile dont l'ID correspond � celui fourni, ou null si un tel sterile n'existe pas.
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
	 * @return Pelle correspondant � l'ID fourni, ou null si une telle pelle n'existe pas.
	 */
	private Station getPelle(String stationId) {
		for(int i = 0; i < this.getPelles().size(); i++) {
			if(this.getPelles().get(i).getId().compareTo(stationId) == 0) {
				return this.getPelles().get(i);
			}
		}
		return null;
	}






	//reset toutes les stats de la mine et de ses objets
	//
	protected void resetAllStats() {

		//reset les stats de la mine
		//
		this.time = 0;

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










	protected void resetTime() {
		this.time = 0;

	}






	protected void setInWarmup(boolean inWarmup) {
		this.inWarmup = inWarmup;
	}






	protected void setMeteoFactor(double meteoFactor) {
		this.meteoFactor = meteoFactor;
	}






	public int getDayNumber() {
		return this.dayNumber;
	}






	public FailureScenario getRandomFailureScenario() {
		
		FailureScenario fs = null;
		if(failureScenarios.size() > 0) {
		int index = (int) (Math.random()*failureScenarios.size());
		fs = failureScenarios.get(index);
		}
		return fs;
	}


}
