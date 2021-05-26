package ca.polymtl.SimulationMine.MineSimulator;

public class Concentrateur extends Station {

	//quantité de minerai (en nombre de voyages)
	private double quantityIron;

	//quantité de souffre (en nombre de voyages)
	private double quantitySulfur;

	private double totalQuantity;

	/** Vitesse moyenne de remplissage (en tonnes secondes)*/
	public final static double AVERAGE_DECHARGE_SPEED = 1./2.;
	/** Écart type sur la vitesse de remplissage*/
	public final static double ECART_TYPE_DECHARGE_SPEED = 1./30.;
	
	protected Concentrateur(double i, double j, String id) {
		super(i, j, id);

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
	}

	
	

	//ajoute un load du type de roche spécifié
	private void addLoad(RockType rockType, double numberTons) {
		quantityIron += rockType.getPercentIron()/100.*numberTons;
		quantitySulfur += rockType.getPercentSulfur()/100.*numberTons;
		totalQuantity += numberTons;
		
	}



	/**
	 * 
	 * @return Quantité total de minerai livré (en tones)
	 */
	public double getTotalQuantity() {
		return totalQuantity;
	}

	/**
	 * 
	 * @return Quantité totale de fer livré (en tonnes)
	 */
	public double getQuantityIron() {
		return quantityIron;
	}

	/**
	 * 
	 * @return Quantité total de souffre livré (en tonnes)
	 */
	public double getQuantitySulfur() {
		return quantitySulfur;
	}

	/**
	 * 
	 * @return Le pourcentage de fer dans le mélange
	 */
	public double getPercentIron() {

		return quantityIron/totalQuantity*100;
	}

	/**
	 * 
	 * @return Quantité de souffre dans le mélange
	 */
	public double getPercentSulfur() {

		return quantitySulfur/totalQuantity*100;
	}




	@Override
	protected void resetStats() {

		quantityIron = 0;
		quantitySulfur = 0;
		totalQuantity = 0;
		
	}


	protected void setCamionOnArrival(Camion camion) {
		super.setCamionOnArrival(camion);
		camion.setNumberOfRuns(camion.getNumberOfRuns()+1);
	}

	/*
	 * Met un camion en remplissage
	 */
	protected void setCamionEnTraitement(Camion camion) {
		super.setCamionEnTraitement(camion);
		camion.setstateDecharge();
	}
	
	public void activate() {
		
		//si il y a un camion en traitement, le traite le plus possible
		//
		if(this.camionEnTraitement!= null) {
			
			
			
			double camionCharge = this.camionEnTraitement.getCharge();
			double tempsDecharge = camionCharge/AVERAGE_DECHARGE_SPEED;
			
			
			//trouve le temps de traitement (min entre temps restant au concentrateur, temps restant au camion, et temps avant que le camion soit déchargé)
			double tempsTraitement= tempsDecharge;
			if(this.getRemainingTimeInTurn()< tempsTraitement) {
				tempsTraitement = this.getRemainingTimeInTurn();
			}
			if(this.camionEnTraitement.getRemainingTimeInTurn() < tempsTraitement) {
				tempsTraitement = this.camionEnTraitement.getRemainingTimeInTurn();
			}
			
			System.out.println("temps de décharge : "+tempsDecharge);
			System.out.println("temps du concentrateur : "+this.getRemainingTimeInTurn());
			System.out.println("temps du camion : "+this.camionEnTraitement.getRemainingTimeInTurn());
			System.out.println("temps de traitement : "+tempsTraitement);
			System.out.println("iter finished : "+this.iterFinished());
			
			//decharge le camion
			//
			double quantiteDecharge = AVERAGE_DECHARGE_SPEED*tempsTraitement;
			
			this.camionEnTraitement.decharge(quantiteDecharge, tempsTraitement);
			addLoad(camionEnTraitement.getRockType(),quantiteDecharge);
			
			this.iterCurrentTime+= tempsTraitement;
			if(this.getRemainingTimeInTurn()<0.00001) {
				this.iterCurrentTime = iterStepSize;
				this.iterFinished = true;
			}
			
			//fait attendre les camions de la file d'attente
			//
			for(int i = 0 ; i< this.camionsEnAttente.size(); i++) {
				if(camionsEnAttente.get(i).getRemainingTimeInTurn() < tempsTraitement) {
					this.camionsEnAttente.get(i).waitUntilEndIter();
				}
				else {
					this.camionsEnAttente.get(i).attend(tempsTraitement);
				}
			}
			
			//si on a terminé le camion, on passe au suivant
			//
			if(this.camionEnTraitement.getCharge()<0.00001) {
				camionEnTraitement.setStateIdle();		
				camionEnTraitement.setSpeed(0);
				
				camionEnTraitement = null;
				if(camionsEnAttente.size()!=0) {
					Camion c = camionsEnAttente.get(0);
					camionsEnAttente.remove(c);
					setCamionEnTraitement(c);
				}
				
				
			}
			//si le camion en traitement a terminé son iteration mais n'est pas rempli, 
			//fait attendre le concentrateur pour le reste de l'itération
			//Cela veut dire que la pelle a attendu un peu avant l'arrivée d'un camion.
			if(camionEnTraitement != null && camionEnTraitement.iterFinished() && camionEnTraitement.getCharge()>0.00001) {
				this.waitForRemainingTime();
				this.iterFinished = true;
			}
		}
	}
}
