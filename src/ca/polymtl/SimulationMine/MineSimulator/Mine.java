package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public String getFileName() {
			return this.fileName;
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

	/**
	 * facteur météo
	 */
	protected static double DEFAULT_METEO_FACTOR = 1;
	//duree d'un pas de temps (secondes)
	protected static double TIME_INCREMENT = 4; // secondes

	protected static double TIME_INCREMENT_WARMUP = 60;//1 minute 



	//nombre de pas d'increment avant le debut de l'affichage
	protected static int NB_WARMUP_STEPS = 300;

	//numeros d'exemples
	public static ArrayList<ExampleId> exampleIds = createExampleIds();


	public static final int EXEMPLE1 = 1;
	public static final int EXEMPLE2 = 2;


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


		//exampleIds.add(new ExampleId(Mine.EXEMPLE1, "4 pelles", "none"));
		//exampleIds.add(new ExampleId(Mine.EXEMPLE2, "10 pelles", "none2"));
		return exampleIds;
	}

	//nombre de steps dans la simulation

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



	private TravelTimePredictor travelTimePredictor;

	private ExampleId currentExampleId;

	private int numberLargeCamions;

	private int numberSmallCamions;
	//------------------------------------------
	// constructeur qui construit une mine vide
	//------------------------------------------
	public Mine() {


		this.pelles = new ArrayList<Pelle>();
		this.camions= new ArrayList<Camion>();

		this.meteoFactor = Mine.DEFAULT_METEO_FACTOR;

		this.travelTimePredictor = new TravelTimePredictor(this);

		this.time = 0;


	}






	protected void addTime(double time) {
		this.time+=time;
	}



	private double calculeTempsAttenteMoyenPelle() {
		double totalAttentePelles = 0;
		int totalRemplissagePelles = 0;
		for(int i = 0 ; i < pelles.size(); i++) {
			totalAttentePelles += pelles.get(i).getWaitTime();
			totalRemplissagePelles += pelles.get(i).getNbCamionsRemplis();
		}

		double tempsAttenteMoyen = totalAttentePelles/totalRemplissagePelles;

		if(totalRemplissagePelles == 0) {
			tempsAttenteMoyen = 0;
		}
		return tempsAttenteMoyen;
	}



	//retourne la pelle la plus pres des coordonnees relatives fournies
	/**
	 * 
	 * @return La  pelle la plus près des coordonnées relatives données.
	 */
	public Pelle closestPelle(double fractionX, double fractionY) {
		double x = fractionX*this.WIDTH;
		double y = fractionY*this.HEIGHT;



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
	 * @return L'efficacité du camion en % tu temps passé à faire des activités autre que l'attente.
	 */
	public double computeCamionEfficiency(Camion camion) {
		if(this.getTime() == 0) {
			return 0;
		}
		double totalTime = this.getTime();
		double waitingTime = camion.getWaitTime();

		double eff = (totalTime - waitingTime)/totalTime *100;
		return eff;
	}



	/**
	 * 
	 * @return L'efficacité de la pelle en % du temps passé à remplir des camions.
	 */
	public double computePelleEfficiency(Pelle pelle) {
		if(this.getTime() == 0) {
			return 0;
		}
		double totalTime = this.getTime();
		double waitingTime = pelle.getWaitTime();

		double eff = (totalTime - waitingTime)/totalTime *100;
		return eff;
	}

	//efface toutes les informations qui doivent etre effaces lorsqu'on reinitialise la mine
	private void erasePreviousMine() {
		this.time = 0;
		this.inWarmup = false;
		this.concentrateurs = new ArrayList<Concentrateur>();
		this.steriles = new ArrayList<Sterile>();
		this.pelles = new ArrayList<Pelle>();
		this.camions = new ArrayList<Camion>();
		this.numberLargeCamions = 0;
		this.numberSmallCamions = 0;
		this.dataSeriesHandles = null;

	}

	/**
	 * 
	 * @return Efficacité moyenne des camoins
	 */
	public double getAverageCamionEfficiency() {
		double sumEff = 0;
		for(int i = 0 ; i < camions.size(); i++) {
			double eff = computeCamionEfficiency(camions.get(i));
			sumEff += eff;
		}
		return sumEff/camions.size();
	}

	/**
	 * 
	 * @return Efficacité moyenne des pelles
	 */
	public double getAveragePelleEfficiency() {
		double sumEff = 0;
		for(int i = 0 ; i < pelles.size(); i++) {
			double eff = computePelleEfficiency(pelles.get(i));
			sumEff += eff;
		}
		return sumEff/pelles.size();
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
	 * @return Les noms des séries de données qui sont suivies.
	 */
	public ArrayList<String> getDataSeriesHandles() {
		return this.dataSeriesHandles;
	}


	/**
	 * 
	 * @return Numéro de la mine active
	 */
	public int getExemple() {
		return exemple;
	}

	/**
	 * 
	 * @return Efficacité du camion le plus efficace
	 */
	public double getMaxCamionEfficiency() {
		double effMax = 0;
		for(int i = 0 ; i < camions.size(); i++) {
			double eff = computeCamionEfficiency(camions.get(i));
			if(eff > effMax) {
				effMax = eff;
			}
		}
		return effMax;
	}



	/**
	 * 
	 * @return Efficacité de la pelle la plus efficace
	 */
	public double getMaxPelleEfficiency() {
		double effMax = 0;
		for(int i = 0 ; i < pelles.size(); i++) {
			double eff = computePelleEfficiency(pelles.get(i));
			if(eff > effMax) {
				effMax = eff;
			}
		}
		return effMax;
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




	/**
	 * 
	 * @return Facteur météo (entre 50 et 100)
	 */
	public double getMeteoFactor() {
		return meteoFactor;
	}


	/**
	 * 
	 * @return Efficacité du camion le moins efficace
	 */
	public double getMinCamionEfficiency() {
		double effMin = 1000;
		for(int i = 0 ; i < camions.size(); i++) {
			double eff = computeCamionEfficiency(camions.get(i));
			if(eff < effMin) {
				effMin = eff;
			}
		}

		return effMin;
	}


	/**
	 * 
	 * @return Efficacité de la pelle la moins efficace.
	 */
	public double getMinPelleEfficiency() {
		double effMin = 1000;
		for(int i = 0 ; i < pelles.size(); i++) {
			double eff = computePelleEfficiency(pelles.get(i));
			if(eff < effMin) {
				effMin = eff;
			}
		}

		return effMin;
	}

	/**
	 * 
	 * @return Nom de la mine
	 */
	public String getName() {
		return name;
	}


	/**
	 * 
	 * @return Nombre total de voyages effectués par les camions
	 */
	public int getNumberOfRuns() {
		int nbVoyages = 0;
		for(int i = 0 ; i < camions.size(); i++) {
			nbVoyages += camions.get(i).getNumberOfRuns();
		}
		return nbVoyages;
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
	 * @return Station de stérile
	 */
	public ArrayList<Sterile> getSteriles() {
		return steriles;
	}

	/**
	 * 
	 * @return Temps depuis le début de la simulation (secondes)
	 */
	public double getTime() {
		return time;
	}


	/**
	 * 
	 * @return Objet responsable de la prédiction des temps de parcours.
	 */
	public TravelTimePredictor getTravelTimePredictor() {

		return this.travelTimePredictor;
	}

	//initialise la mine en fonction de l'exemple de mine desiree
	protected void init(ExampleId exempleId, int nbSmallCamions, int nbLargeCamions) {
		erasePreviousMine();

		System.out.println("charge mine "+exempleId.getName());


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
			//pour que le point délimite la pratie fractionnaire
			scanner.useLocale(Locale.US);
			//ignore la premiere ligne
			scanner.nextLine();
			int defaultLargeCamion = 0;
			int defaultSmallCamion = 0;
			while(scanner.hasNext()) {
				if(	scanner.hasNext("default_camions_small")) {
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
					scanner.next();//si minerais ou stérile
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
				else {
					scanner.next();
				}


			}
			
			//set les stations de retour
			for(int i = 0 ; i < pelles.size(); i++) {
				Pelle p = pelles.get(i);
				if(p.getRockType().getPercentIron() == 0 && p.getRockType().getPercentSulfur() == 0) {
					p.setReturnStation(steriles.get(0));
				}
				else {
					p.setReturnStation(concentrateurs.get(0));
				}

			}
		} catch (FileNotFoundException e) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.numberSmallCamions = nbSmallCamions;
		this.numberLargeCamions = nbLargeCamions;
		for(int i = 0 ; i < nbSmallCamions; i++) {
			Camion camion = new Camion(steriles.get(0), this, smallCamionImage) {

				/** Vitesse moyenne du camion	 */
				public static final double VITESSE_MOYENNE = 9;
				/** Écart type sur la vitesse du camion	 */
				private static final double ECART_TYPE_VITESSE = 0.6;//ancien 0.5
				/** Charge maximum du camion.	 */
				public static final double CHARGE_MAX = 60.;

				@Override
				public double getAvgSpeed() {
					return VITESSE_MOYENNE;
				}

				@Override
				public double getStdSpeed() {
					return ECART_TYPE_VITESSE;
				}

				@Override
				public double getChargeMax() {
					// TODO Auto-generated method stub
					return CHARGE_MAX;
				}

			};
			camions.add(camion);
		}

		for(int i = 0 ; i < nbLargeCamions; i++) {
			Camion camion = new Camion(steriles.get(0), this, largeCamionImage) {

				/** Vitesse moyenne du camion	 */
				public static final double VITESSE_MOYENNE = 5;
				/** Écart type sur la vitesse du camion	 */
				private static final double ECART_TYPE_VITESSE = 0.3;//ancien 0.5
				/** Charge maximum du camion.	 */
				public static final double CHARGE_MAX = 100.;

				@Override
				public double getAvgSpeed() {
					return VITESSE_MOYENNE;
				}

				@Override
				public double getStdSpeed() {
					return ECART_TYPE_VITESSE;
				}

				@Override
				public double getChargeMax() {
					// TODO Auto-generated method stub
					return CHARGE_MAX;
				}

			};
			camions.add(camion);
		}

		//cree les handles des donnees
		this.dataSeriesHandles = new ArrayList<String>();
		for(int i = 0 ; i < pelles.size(); i++) {
			if(pelles.get(i).getId().equals("pelle1") || pelles.get(i).getId().equals("pelle3") ) {
				dataSeriesHandles.add("reel:"+TravelTimePredictor.getMapKeyForODPair(steriles.get(0), pelles.get(i) ));
				dataSeriesHandles.add("pred:"+TravelTimePredictor.getMapKeyForODPair(steriles.get(0), pelles.get(i) ));
			}
		}
		/*if(this.exemple == Mine.EXEMPLE1) {
			initExemple1(nbSmallCamions, nbLargeCamions);	
		}
		else if(this.exemple == Mine.EXEMPLE2) {
			initExemple2(nbSmallCamions, nbLargeCamions);
		}
		else {
			throw new IllegalArgumentException();
		}*/

	}



	/**
	 * 
	 * @return true si la mine est présenement en warmup (avant le début d'une simulation), false sinon.
	 */
	public boolean isInWarmup() {
		return this.inWarmup;
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



	protected void setTravelTimePredictor(TravelTimePredictor predictor) {

		this.travelTimePredictor = predictor;
	}






	public ExampleId getCurrentExampleId() {
		return this.currentExampleId;
	}






	public int getNumberLargeCamions() {
		return this.numberLargeCamions;
	}






	public int getNumberSmallCamions() {
		return this.numberSmallCamions;
	}


}
