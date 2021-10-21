package ca.polymtl.SimulationMine.MineGui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel; 
import javax.swing.JScrollPane;

import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulator;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import javafx.util.Pair;

public class SommaireFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//couleurs de police
	//
	private static Color darkGreen = new Color(45, 140, 45);
	private static Color darkRed = new Color(170, 0, 0);

	//nombre de pixels correspondant à une tabulation.
	int tabWidth = 30;
	JPanel tabPanel;
	//styles de texte
	//
	Font fontTitre1;

	Font fontTitre2;
	Font fontNormal;

	//--------------------------------------
	//statistiques de la mine a afficher
	//--------------------------------------

	//parametres de simulation
	//
	private int dureeSimulationSeconds;
	private String stringDuree;
	private int nbCamions;
	private String nomMine;

	//Sommaire productivité
	//
	private int nbVoyage;
	private double percentEffPellesMin, percentEffPellesMax, percentEffPellesAvg;
	private double percentEffCamionsMin, percentEffCamionsMax, percentEffCamionsAvg;

	//productivité des pelles
	//
	private ArrayList<Pair<Pelle, Double>> effPelles;
	private HashMap<Pelle, Double> attenteMoyenCamions;

	private double ecartTypeProdPelle;
	private double attenteMoyenGlobalCamions;
	private double ecartTypeAttenteCamions;
	private double attenteMoyenGlobalPelles;
	private double ecartTypeAttentePelle;
	private double pourcentFer;
	private double pourcentSouffre;
	private double quantiteMinerai;
	private double quantiteSterile;
	private ArrayList<Pair<Pelle, Double>> tauxPelles;
	private ArrayList<Pair<Pelle, Double>> planPelles;


	public SommaireFrame(MineSimulator mineSimulator) {

		super();

		//Cree les differents styles de texte
		setFonts();

		getMineStats(mineSimulator);

		//cree panneau tab
		//
		tabPanel = new JPanel();
		tabPanel.setMaximumSize(new Dimension(tabWidth, 100000));
		tabPanel.setMinimumSize(new Dimension(tabWidth, 10));

		this.setMinimumSize(new Dimension(900, 600));
		this.setTitle("Sommaire");

		JPanel framePanel = new JPanel();
		framePanel.setLayout(new BorderLayout());
		this.add(framePanel);

		framePanel.add(titrePanel(), BorderLayout.NORTH);
		framePanel.add(sommairePanel(), BorderLayout.CENTER);
		framePanel.add(buttons(), BorderLayout.SOUTH);
		this.setVisible(true);
		framePanel.setOpaque(true);
		framePanel.setBackground(Color.white);

		//pour debuggage
		//exportSommaireToFile(null);
	}

	private double arrondiDeuxDecimales(double number) {

		return 1.*((int)((number+0.005)*100))/100.;
	}

	private JButton boutonAnnuler() {
		JButton boutonAnnuler = new JButton("Annuler");
		final JFrame frame = (JFrame) this;
		boutonAnnuler.addActionListener(new ActionListener(){
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
				
			}
			
		});
		
		return boutonAnnuler;
	}

	private JButton boutonExporter() {
		JButton boutonExporter = new JButton("Exporter");

		boutonExporter.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				System.out.println("filechooser");
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("exporter...");

				fileChooser.setSize(new Dimension(1100, 800));

				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

				fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

				fileChooser.setVisible(true);



				if(fileChooser.showSaveDialog(new JLabel("testing")) == JFileChooser.APPROVE_OPTION){
					File file = fileChooser.getSelectedFile();
					exportSommaireToFile(file);
					System.out.println("fichier choisi"+ file.getName());
				}




			}

		});

		return boutonExporter;
	}

	private JPanel buttons() {
		JPanel buttonsPanel = new JPanel();

		JButton button1 = boutonExporter();
		buttonsPanel.add(button1);

		JButton button2 = boutonAnnuler();
		buttonsPanel.add(button2);

		return buttonsPanel;
	}

	private void getMineStats(MineSimulator mineSimulator) {
		Mine mine = mineSimulator.getMine();

		//Infos de la simulation
		//
		//nom de mine
		this.nomMine = mine.getName();
		this.dureeSimulationSeconds = mineSimulator.getTempsSimulationSeconds();
		this.stringDuree = String.format("%02d h, %02d min, %02d sec", 
				TimeUnit.SECONDS.toHours(dureeSimulationSeconds),
				TimeUnit.SECONDS.toMinutes(dureeSimulationSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(dureeSimulationSeconds)), 
				TimeUnit.SECONDS.toSeconds(dureeSimulationSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(dureeSimulationSeconds))
				);
		this.nbCamions = mine.getCamions().size();

		//Sommaire de productivité
		//
		this.nbVoyage = mineSimulator.getNumberOfRuns();

		this.quantiteMinerai =0;
		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++ ) {
			quantiteMinerai+= mine.getConcentrateurs().get(i).getTotalQuantity();
		}
		
		this.quantiteSterile = 0;
		for(int i = 0 ; i < mine.getSteriles().size(); i++) {
			quantiteSterile+= mine.getSteriles().get(i).getTotalQuantity();
		}
		

		this.percentEffCamionsMin = mineSimulator.getMinCamionEfficiency();
		this.percentEffCamionsMax = mineSimulator.getMaxCamionEfficiency();
		this.percentEffCamionsAvg = mineSimulator.getAverageCamionEfficiency();

		this.percentEffPellesMin = mineSimulator.getMinPelleEfficiency();
		this.percentEffPellesMax = mineSimulator.getMaxPelleEfficiency();
		this.percentEffPellesAvg = mineSimulator.getAveragePelleEfficiency();

		//Qualité du mélange
		//
		double totalFer = 0;
		double totalSoufre = 0;
		double totalMineraiConc = 0;
		for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
			//Concentrateur
			Concentrateur concentrateur = mine.getConcentrateurs().get(i);
			totalFer += concentrateur.getQuantityIron();
			totalSoufre+= concentrateur.getQuantitySulfur();
			totalMineraiConc+= concentrateur.getTotalQuantity();
		}
		
		this.pourcentFer = totalFer/totalMineraiConc*100;
		this.pourcentSouffre = totalSoufre/totalMineraiConc*100;

		ArrayList<Pelle> pelles = mine.getPelles();
		
		//efficacité des pelles
		//
		effPelles = new ArrayList<Pair<Pelle, Double>>();
		
		for(int i = 0 ; i < pelles.size(); i++){
			double efficiency = mineSimulator.computePelleEfficiency(pelles.get(i));
			Pair<Pelle, Double> pair = new Pair<Pelle, Double>(pelles.get(i), efficiency);
			effPelles.add(pair);
		}
		
		//nb voyages/heures
		//
		tauxPelles = new ArrayList<Pair<Pelle, Double>>();
		for(int i = 0 ; i < pelles.size(); i++){
			double taux = 1.*pelles.get(i).getNbCamionsTraites()/mineSimulator.getTempsSimulationSeconds()*3600;
			Pair<Pelle, Double> pair = new Pair<Pelle, Double>(pelles.get(i), taux);
			tauxPelles.add(pair);
		}
		
		//plan
		//
		planPelles = new ArrayList<Pair<Pelle, Double>>();
		for(int i = 0 ; i < pelles.size(); i++){
			double plan = pelles.get(i).getPlanNbTonnesParHeure();
			Pair<Pelle, Double> pair = new Pair<Pelle, Double>(pelles.get(i), plan);
			planPelles.add(pair);
		}

		//calcule l'ecart type d'efficacite
		//
		double sommeEcartCarre = 0;
		for(int i = 0; i < pelles.size(); i++){
			double effPelle = mineSimulator.computePelleEfficiency(pelles.get(i));
			sommeEcartCarre += (effPelle - percentEffPellesAvg )*(effPelle - percentEffPellesAvg );
		}
		this.ecartTypeProdPelle = Math.sqrt(sommeEcartCarre/pelles.size());

		//calcule attente moyen des camions a chaque pelle
		//
		ArrayList<Camion> camions = mine.getCamions();
		attenteMoyenCamions = new HashMap<Pelle, Double>();
		for(int i = 0 ; i < pelles.size(); i++){
			double sumAttentePelle = 0;
			int nbVisitAtPelle = 0;
			double attenteMoyenneAtPelle = 0;
			for(int j = 0 ; j < camions.size(); j++){
				sumAttentePelle += camions.get(j).getAttenteAtStation(pelles.get(i));

				nbVisitAtPelle +=camions.get(j).getNbVisitAtStation(pelles.get(i));
			}
			if(nbVisitAtPelle != 0){
				attenteMoyenneAtPelle = sumAttentePelle/nbVisitAtPelle;
			}
			attenteMoyenCamions.put(pelles.get(i), attenteMoyenneAtPelle);
		}

		//attente moyen general des camions
		//
		double nbPeriodeAttenteCamions = 0;
		double sommeAttenteCamions = 0;
		for(int i = 0 ; i < pelles.size(); i++){
			for(int j = 0 ; j < camions.size(); j++){
				sommeAttenteCamions += camions.get(j).getAttenteAtStation(pelles.get(i));
				nbPeriodeAttenteCamions +=camions.get(j).getNbVisitAtStation(pelles.get(i));
			}
		}
		attenteMoyenGlobalCamions = 0.;
		if(nbPeriodeAttenteCamions != 0){
			attenteMoyenGlobalCamions = sommeAttenteCamions/nbPeriodeAttenteCamions;
		}

		//ecart type attente camions
		ecartTypeAttenteCamions=0.;
		double sommeEcartCarreAttenteCamions = 0;
		for(int i = 0 ; i < pelles.size(); i++){
			sommeEcartCarreAttenteCamions += (attenteMoyenCamions.get(pelles.get(i)).doubleValue()- attenteMoyenGlobalCamions)*(attenteMoyenCamions.get(pelles.get(i)).doubleValue()- attenteMoyenGlobalCamions);
		}
		if(nbPeriodeAttenteCamions >0){
			ecartTypeAttenteCamions = Math.sqrt(sommeEcartCarreAttenteCamions);
		}

		//attente moyen des pelles
		//
		double sumAttenteMoyenPelles = 0;
		for(int i = 0 ; i < pelles.size(); i++){
			sumAttenteMoyenPelles += pelles.get(i).getAverageWaitTimeSeconds();
		}

		this.attenteMoyenGlobalPelles =0;
		if(pelles.size()!=0){
			this.attenteMoyenGlobalPelles = sumAttenteMoyenPelles/pelles.size();
		}

		//ecart type attente pelle
		//
		double sumEcartCarreAttentePelle = 0;
		for(int i = 0 ; i < pelles.size(); i++){
			sumEcartCarreAttentePelle += (pelles.get(i).getAverageWaitTimeSeconds()-attenteMoyenGlobalPelles)*(pelles.get(i).getAverageWaitTimeSeconds()-attenteMoyenGlobalPelles);
		}

		ecartTypeAttentePelle = Math.sqrt(sumEcartCarreAttentePelle);

	}

	private Component paramsSimulationPanel() {

		JPanel paramsSimulationPanel = new JPanel();
		paramsSimulationPanel.setOpaque(false);


		paramsSimulationPanel.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.insets = new Insets(0, 10, 0, 0);
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.WEST;
		//paramsSimulationPanel.setMinimumSize(new Dimension(100, 1));
		//paramsSimulationPanel.setPreferredSize(new Dimension(100, 1));
		//titre
		//
		JLabel titreLabel = new JLabel("Paramètres de la simulation");
		titreLabel.setFont(fontTitre2);

		paramsSimulationPanel.add(titreLabel, gc);

		gc.insets = new Insets(5, 10+tabWidth, 0, 0);

		//Nom de mine
		//
		String mineText = "Mine : "+this.nomMine;
		JLabel mineLabel = new JLabel(mineText);
		mineLabel.setFont(fontNormal);
		gc.gridy++;
		paramsSimulationPanel.add(mineLabel, gc);


		//Nombre de camions
		//
		String nbCamionsText = "Nombre de camions : "+this.nbCamions;
		JLabel nbCamionsLabel = new JLabel(nbCamionsText);
		nbCamionsLabel.setFont(fontNormal);
		gc.gridy++;
		paramsSimulationPanel.add(nbCamionsLabel, gc);



		//Temps de simulation
		//


		String tempsSimulationText = "Durée simulé : "+this.stringDuree;
		JLabel tempsSimulationLabel = new JLabel(tempsSimulationText);
		tempsSimulationLabel.setFont(fontNormal);
		gc.gridy++;
		paramsSimulationPanel.add(tempsSimulationLabel, gc);



		return paramsSimulationPanel ;
	}

	private Component prodPellesPanel() {
		JPanel prodPellesPanel = new JPanel();
		prodPellesPanel.setOpaque(false);


		prodPellesPanel.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.insets = new Insets(0, 10, 0, 0);
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.WEST;
		gc.gridwidth = 200;
		//paramsSimulationPanel.setMinimumSize(new Dimension(100, 1));
		//paramsSimulationPanel.setPreferredSize(new Dimension(100, 1));
		//titre
		//
		JLabel titreLabel = new JLabel("Productivité des pelles");
		titreLabel.setFont(fontTitre2);

		prodPellesPanel.add(titreLabel, gc);
		gc.gridwidth = 1;


		//productivité de chaque pelle
		//
		for(int i = 0 ; i < effPelles.size(); i++){
			Pelle pelle = effPelles.get(i).getKey();
			double eff = effPelles.get(i).getValue();
			
			double tauxPlan = planPelles.get(i).getValue();
			double tauxReel = tauxPelles.get(i).getValue();


			//Label de la pelle
			//
			String pelleNameText = "Pelle "+pelle.getId()+" : ";
			JLabel pelleNameLabel = new JLabel(pelleNameText);
			pelleNameLabel.setFont(fontNormal);
			gc.gridx = 0;
			gc.weightx = 0.0;
			gc.gridy++;
			gc.gridwidth = 200;
			gc.insets = new Insets(5, 10+tabWidth, 0, 0);
			prodPellesPanel.add(pelleNameLabel, gc);

			
			//Taux de service planifié
			//
			gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
			gc.gridy++;
			gc.gridx = 0;
			gc.gridwidth = 1;
			String tauxPlanText = "Taux de service planifié : ";
			JLabel tauxPlanLabel = new JLabel(tauxPlanText );
			tauxPlanLabel.setFont(fontNormal);
			prodPellesPanel.add(tauxPlanLabel, gc);
			
			gc.insets = new Insets(5, 0, 0, 0);
			gc.gridx = 1;
			gc.weightx = 0.0;
			String tauxPlanText2 = ""+arrondiDeuxDecimales(tauxPlan)+"camions/h.";
			JLabel tauxPlanLabel2 = new JLabel(tauxPlanText2);
			tauxPlanLabel2.setFont(fontNormal);
			prodPellesPanel.add(tauxPlanLabel2, gc);
			
			//Taux de service réel
			//
			gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
			gc.gridy++;
			gc.gridx = 0;
			gc.gridwidth = 1;
			String tauxReelText = "Taux de service Réel : ";
			JLabel tauxReelLabel = new JLabel(tauxReelText );
			tauxReelLabel.setFont(fontNormal);
			prodPellesPanel.add(tauxReelLabel, gc);
			
			gc.insets = new Insets(5, 0, 0, 0);
			gc.gridx = 1;
			gc.weightx = 0.0;
			String tauxReelText2 = ""+arrondiDeuxDecimales(tauxReel)+"camions/h.";
			JLabel tauxReelLabel2 = new JLabel(tauxReelText2);
			tauxReelLabel2.setFont(fontNormal);
			prodPellesPanel.add(tauxReelLabel2, gc);
	
			
			//efficacité
			//
			gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
			gc.gridy++;
			gc.gridx = 0;
			gc.gridwidth = 1;
			String effPelleTitleText = "Efficacité : ";
			JLabel effPelleTitleLabel = new JLabel(effPelleTitleText );
			effPelleTitleLabel.setFont(fontNormal);
			prodPellesPanel.add(effPelleTitleLabel, gc);

			gc.insets = new Insets(5, 0, 0, 0);
			gc.gridx = 1;
			gc.weightx = 0.0;
			String effPelleText = ""+arrondiDeuxDecimales(eff)+"%";
			JLabel effPelleLabel = new JLabel(effPelleText);
			effPelleLabel.setFont(fontNormal);
			prodPellesPanel.add(effPelleLabel, gc);
			if(eff > this.percentEffPellesAvg+this.ecartTypeProdPelle){
				effPelleLabel.setForeground(darkGreen);
			}
			else if(eff < this.percentEffPellesAvg-this.ecartTypeProdPelle){
				effPelleLabel.setForeground(darkRed);
			}


			//attente pelle
			//
			gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
			gc.gridy++;
			gc.gridx = 0;
			gc.gridwidth = 1;
			String attenteMoyPelleTitleText = "Attente moy. pelle : ";
			JLabel attenteMoyPelleTitleLabel = new JLabel(attenteMoyPelleTitleText );
			attenteMoyPelleTitleLabel.setFont(fontNormal);
			prodPellesPanel.add(attenteMoyPelleTitleLabel, gc);

			gc.insets = new Insets(5, 0, 0, 0);
			gc.gridx = 1;
			gc.weightx = 0.0;
			String attenteMoyPelleText = ""+arrondiDeuxDecimales(pelle.getAverageWaitTimeSeconds())+" sec.";
			JLabel attenteMoyPelleLabel = new JLabel(attenteMoyPelleText);
			attenteMoyPelleLabel.setFont(fontNormal);
			prodPellesPanel.add(attenteMoyPelleLabel, gc);
			if(pelle.getAverageWaitTimeSeconds() > this.attenteMoyenGlobalPelles+this.ecartTypeAttentePelle){
				attenteMoyPelleLabel.setForeground(darkGreen);
			}
			else if(pelle.getAverageWaitTimeSeconds() < this.attenteMoyenGlobalPelles-this.ecartTypeAttentePelle){
				attenteMoyPelleLabel.setForeground(darkRed);
			}

			//attente camion
			//
			gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
			gc.gridy++;
			gc.gridx = 0;
			gc.gridwidth = 1;
			String attenteMoyCamionTitleText = "Attente moy. camions : ";
			JLabel attenteMoyCamionTitleLabel = new JLabel(attenteMoyCamionTitleText );
			attenteMoyCamionTitleLabel.setFont(fontNormal);
			prodPellesPanel.add(attenteMoyCamionTitleLabel, gc);

			gc.insets = new Insets(5, 0, 0, 0);
			gc.gridx = 1;
			gc.weightx = 0.0;
			String attenteMoyCamionText = ""+arrondiDeuxDecimales(this.attenteMoyenCamions.get(pelle).doubleValue())+" sec.";
			JLabel attenteMoyCamionLabel = new JLabel(attenteMoyCamionText);
			attenteMoyCamionLabel.setFont(fontNormal);
			prodPellesPanel.add(attenteMoyCamionLabel, gc);
			if(this.attenteMoyenCamions.get(pelle).doubleValue() > this.attenteMoyenGlobalCamions+this.ecartTypeAttenteCamions){
				attenteMoyCamionLabel.setForeground(darkGreen);
			}
			else if(this.attenteMoyenCamions.get(pelle).doubleValue() < this.attenteMoyenGlobalCamions-this.ecartTypeAttenteCamions){
				attenteMoyCamionLabel.setForeground(darkRed);
			}

		}


		return prodPellesPanel ;
	}

	private void setFonts() {
		Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
		fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		fontTitre1 = new Font("Serif",Font.BOLD, 24).deriveFont(fontAttributes);

		fontTitre2 = new Font("Serif",Font.BOLD, 20).deriveFont(fontAttributes);

		fontNormal = new Font("Serif",Font.BOLD, 16);


	}

	private Component sommairePanel() {

		JPanel sommairePanel = new JPanel();

		//layout
		//
		sommairePanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		sommairePanel.setOpaque(true);
		sommairePanel.setBackground(Color.WHITE);


		//scrollbar
		//
		JScrollPane scrollPane = new JScrollPane(sommairePanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//sommairePanel.add(scrollPane);

		//Ajoute les composantes
		//

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx=0.001;
		//gc.weighty=0.001;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.NORTHWEST;

		//parametres du simulateur
		sommairePanel.add(paramsSimulationPanel(), gc);

		gc.gridy++;
		sommairePanel.add(sommaireProdPanel(), gc);

		gc.gridy++;
		sommairePanel.add(prodPellesPanel(), gc);

		//JPanel pour prendre le reste de la place
		JPanel filler = new JPanel();
		filler.setOpaque(false);
		gc.gridy++;
		gc.fill = GridBagConstraints.BOTH;
		gc.weighty = 1;

		sommairePanel.add(filler, gc);
		return scrollPane;
	}

	private JPanel sommaireProdPanel() {
		JPanel sommaireProdPanel = new JPanel();
		sommaireProdPanel.setOpaque(false);

		DecimalFormat df = new DecimalFormat("0.0");

		sommaireProdPanel.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.insets = new Insets(0, 10, 0, 0);
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.WEST;
		//paramsSimulationPanel.setMinimumSize(new Dimension(100, 1));
		//paramsSimulationPanel.setPreferredSize(new Dimension(100, 1));
		//titre
		//
		JLabel titreLabel = new JLabel("Sommaire de productivité");
		titreLabel.setFont(fontTitre2);

		sommaireProdPanel.add(titreLabel, gc);

		gc.insets = new Insets(5, 10+tabWidth, 0, 0);
		gc.gridy++;
		//Nombres de voyages
		//
		String nbVoyageText = "Quantités livrées : ";
		JLabel nbVoyageLabel = new JLabel(nbVoyageText);
		nbVoyageLabel.setFont(fontNormal);
		sommaireProdPanel.add(nbVoyageLabel, gc);


		//----------------------------------
		//Au concentrateur
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String qteConcText = "Au concentrateur : "+df.format(this.quantiteMinerai)+" tonnes";
		JLabel qteConcLabel = new JLabel(qteConcText);
		qteConcLabel.setFont(fontNormal);
		sommaireProdPanel.add(qteConcLabel, gc);

		//----------------------------------
		//Sterile
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String qteSterileText = "Au stérile : "+df.format(this.quantiteSterile)+" tonnes";
		JLabel qteSterileLabel = new JLabel(qteSterileText);
		qteSterileLabel.setFont(fontNormal);
		sommaireProdPanel.add(qteSterileLabel, gc);
		
		//----------------------------------
		//Total
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String qteTotalText = "Total : "+df.format(this.quantiteMinerai+this.quantiteSterile)+" tonnes";
		JLabel qteTotalLabel = new JLabel(qteTotalText);
		qteTotalLabel.setFont(fontNormal);
		sommaireProdPanel.add(qteTotalLabel, gc);


		//----------------------------------
		//Mélange
		//----------------------------------
		gc.insets = new Insets(5, 10+tabWidth, 0, 0);
		gc.gridy++;

		String melangeText = "Qualité du mélange : ";
		JLabel melangeLabel = new JLabel(melangeText);
		melangeLabel.setFont(fontNormal);
		sommaireProdPanel.add(melangeLabel, gc);

		//----------------------------------
		//Fer
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String ferText = "Fer : "+df.format(this.pourcentFer)+"%";
		JLabel ferLabel = new JLabel(ferText);
		ferLabel.setFont(fontNormal);
		sommaireProdPanel.add(ferLabel, gc);

		//----------------------------------
		//Souffre
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String souffreText = "Souffre : "+df.format(this.pourcentSouffre)+"%";
		JLabel souffreLabel = new JLabel(souffreText);
		souffreLabel.setFont(fontNormal);
		sommaireProdPanel.add(souffreLabel, gc);



		//----------------------------------
		//Efficacité des pelles
		//----------------------------------
		gc.insets = new Insets(5, 10+tabWidth, 0, 0);
		gc.gridy++;
		String effPellesText = "Efficacité des pelles";
		JLabel effPellesLabel = new JLabel(effPellesText);
		effPellesLabel.setFont(fontNormal);
		sommaireProdPanel.add(effPellesLabel, gc);

		//Maximum
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String effPellesMaxText = "Maximum : "+arrondiDeuxDecimales(this.percentEffPellesMax)+"%";
		JLabel effPellesMaxLabel = new JLabel(effPellesMaxText);
		effPellesMaxLabel.setFont(fontNormal);
		effPellesMaxLabel.setForeground(darkGreen);
		sommaireProdPanel.add(effPellesMaxLabel, gc);

		//Moyenne
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String effPellesAvgText = "Moyenne : "+arrondiDeuxDecimales(this.percentEffPellesAvg)+"%";
		JLabel effPellesAvgLabel = new JLabel(effPellesAvgText);
		effPellesAvgLabel.setFont(fontNormal);
		sommaireProdPanel.add(effPellesAvgLabel, gc);

		//Minimum
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String effPellesMinText = "Minimum : "+arrondiDeuxDecimales(this.percentEffPellesMin)+"%";
		JLabel effPellesMinLabel = new JLabel(effPellesMinText);
		effPellesMinLabel.setFont(fontNormal);
		effPellesMinLabel.setForeground(darkRed);
		sommaireProdPanel.add(effPellesMinLabel, gc);


		//--------------------------------------
		//Efficacité des camions
		//--------------------------------------
		gc.insets = new Insets(5, 10+tabWidth, 0, 0);
		gc.gridy++;
		String effCamionText = "Efficacité des camions :";
		JLabel effCamionLabel = new JLabel(effCamionText);
		effCamionLabel.setFont(fontNormal);
		sommaireProdPanel.add(effCamionLabel, gc);

		//Maximum
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String effCamionsMaxText = "Maximum : "+arrondiDeuxDecimales(this.percentEffCamionsMax)+"%";
		JLabel effCamionsMaxLabel = new JLabel(effCamionsMaxText);
		effCamionsMaxLabel.setFont(fontNormal);
		effCamionsMaxLabel.setForeground(darkGreen);
		sommaireProdPanel.add(effCamionsMaxLabel, gc);

		//Moyenne
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String effCamionsAvgText = "Maximum : "+arrondiDeuxDecimales(this.percentEffCamionsAvg)+"%";
		JLabel effCamionsAvgLabel = new JLabel(effCamionsAvgText);
		effCamionsAvgLabel.setFont(fontNormal);
		sommaireProdPanel.add(effCamionsAvgLabel, gc);

		//Minimum
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String effCamionsMinText = "Maximum : "+arrondiDeuxDecimales(this.percentEffCamionsMin)+"%";
		JLabel effCamionsMinLabel = new JLabel(effCamionsMinText);
		effCamionsMinLabel.setFont(fontNormal);
		effCamionsMinLabel.setForeground(darkRed);
		sommaireProdPanel.add(effCamionsMinLabel, gc);

		//----------------------------------
		//Attente moyenne
		//----------------------------------
		gc.insets = new Insets(5, 10+tabWidth, 0, 0);
		gc.gridy++;
		String waitText = "Attente moyenne :";
		JLabel waitTitleLabel = new JLabel(waitText);
		waitTitleLabel.setFont(fontNormal);
		sommaireProdPanel.add(waitTitleLabel, gc);

		//Pelles
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String waitPellesAvgText = "Pelles : "+arrondiDeuxDecimales(attenteMoyenGlobalPelles)+" sec.";
		JLabel waitPellesAvgLabel = new JLabel(waitPellesAvgText);
		waitPellesAvgLabel.setFont(fontNormal);
		sommaireProdPanel.add(waitPellesAvgLabel, gc);

		//Camions
		//
		gc.insets = new Insets(5, 10+2*tabWidth, 0, 0);
		gc.gridy++;
		String waitCamionsAvgText = "Camions : "+arrondiDeuxDecimales(this.attenteMoyenGlobalCamions)+" sec.";
		JLabel waitCamionsAvgLabel = new JLabel(waitCamionsAvgText);
		waitCamionsAvgLabel.setFont(fontNormal);
		sommaireProdPanel.add(waitCamionsAvgLabel, gc);





		return sommaireProdPanel ;
	}

	private Component titrePanel() {
		JPanel mainTitlePanel = new JPanel();

		mainTitlePanel.setOpaque(false);




		JLabel titreLabel = new JLabel("Sommaire");
		titreLabel.setFont(fontTitre1);

		mainTitlePanel.add(titreLabel);
		return mainTitlePanel;
	}

	protected void exportSommaireToFile(File file) {
		String outString = "";

		//parametres de simulation
		outString+=this.nomMine+"\t"+this.nbCamions+"\t"+this.dureeSimulationSeconds+"\t";

		//sommaire de productivité
		outString += this.quantiteMinerai +"\t"+this.quantiteSterile +"\t"+this.nbVoyage +"\t"+this.pourcentFer+"\t"+this.pourcentSouffre;
		outString +="\t"+this.percentEffPellesMax+"\t"+this.percentEffPellesAvg+"\t"+this.percentEffPellesMin+"\t"+this.percentEffCamionsMax+"\t"+this.percentEffCamionsAvg+"\t"+this.percentEffCamionsMin+"\t"+this.attenteMoyenGlobalPelles+"\t"+this.attenteMoyenGlobalCamions+"\t";




		//productivité de chaque pelle
		//
		for(int i = 0 ; i < effPelles.size(); i++){
			Pelle pelle = effPelles.get(i).getKey();
			double tauxPlan = planPelles.get(i).getValue();
			double tauxReel = tauxPelles.get(i).getValue();
			
			double eff = effPelles.get(i).getValue();

			outString +=pelle.getId()+"\t"+tauxPlan+"\t"+tauxReel+"\t"+eff+"\t"+pelle.getAverageWaitTimeSeconds()+"\t"+this.attenteMoyenCamions.get(pelle).doubleValue()+"\t";

		}





		System.out.println(outString);

		try {
			FileWriter fw = new FileWriter(file);
			fw.write(outString);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

}
