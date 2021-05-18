package ca.polymtl.SimulationMine.MineGui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bsh.EvalError;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulator;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.decisionMaker.DecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;

public class JControlPanel extends JPanel{

	public static final Color DARK_BLUE = new Color(19, 86, 174);

	//constantes
	private static int sliderWidth = 400;

	private JMineFrame parentFrame;


	//champs
	//

	//parametres de simulation
	//
	private JComboBox mineComboBox;
	private JTextField nbCamionsTextField;
	private JTextField tempsSimulationTextField;

	//controle de la simulation
	//
	private PlayPauseButton playPauseButton;

	//optimisation
	//
	private JTextField optStrategyTextField;

	//prediction du temps de parcours
	//
	private JComboBox predictComboBox;
	private JLabel nLabel;
	private JTextField nTextField;
	private JLabel lembdaLabel;
	private JTextField rhoTextField;

	//valeurs des champs
	protected double selectedTempsSimulation;
	protected int selectedNumberOfCamions;

	protected int predictN;
	protected int predictRho;

	//protected String scoreFunctionString;

	private JButton completeButton;

	private int currentNValue;

	private double currentRhoValue;

	private BufferedImage soleilImage;

	private BufferedImage floconImage;

	private BufferedImage camionImage;

	private BufferedImage horlogeImage;

	public JControlPanel(JMineFrame frame ){
		super();


		loadImages();

		this.parentFrame = frame;

		this.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.black));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));





		JPanel topPanel = new JPanel();

		topPanel.setBackground(new Color(207, 236, 255));

		this.add(topPanel);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

		topPanel.add(createSimulationSpecsPanel());

		topPanel.add(createRightPanel());









	}

	private void loadImages() {

		//Image de flocon
		//
		BufferedImage floconImageLarge = null;

		try {
			floconImageLarge = ImageIO.read(new File("images/snowflake.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}


		floconImage = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = floconImage.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(floconImageLarge, 0, 0, 25, 25, null);
		g2.dispose();


		//Image de soleil
		//
		BufferedImage soleilImageLarge = null;
		try {
			soleilImageLarge = ImageIO.read(new File("images/sun.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		soleilImage = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
		g2 = soleilImage.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(soleilImageLarge, 0, 0, 25, 25, null);
		g2.dispose();

		//Image de camion
		//

		BufferedImage camionImageLarge = null;

		try {
			camionImageLarge = ImageIO.read(new File("images/camionFois.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		int camionX = 65;
		int camionY = 30;
		camionImage = new BufferedImage(camionX, camionY, BufferedImage.TYPE_INT_ARGB);
		g2 = camionImage.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(camionImageLarge, 0, 0, camionX, camionY, null);
		g2.dispose();

		//Image d'horloge
		//

		BufferedImage horlogeImageLarge = null;

		try {
			horlogeImageLarge = ImageIO.read(new File("images/clock2.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		int horlogeX = 55;
		int horlogeY = 30;
		horlogeImage = new BufferedImage(horlogeX, horlogeY, BufferedImage.TYPE_INT_ARGB);
		g2 = horlogeImage.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(horlogeImageLarge, 0, 0, horlogeX, horlogeY, null);
		g2.dispose();


	}

	private JPanel createRightPanel() {

		JPanel rightPanel = new JPanel();


		//rightPanel.setBackground(Color.red);

		rightPanel.setLayout(new GridBagLayout());


		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.WEST;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.VERTICAL;
		rightPanel.add(createSimulationControlPanel(), gc);

		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		JPanel speedComponent = createSpeedComponent();
		rightPanel.add(speedComponent, gc);


		//titre de la section
		//
		JLabel titreLabel = new JLabel("Prédiction du temps de parcours");
		titreLabel.setForeground(Color.white);
		titreLabel.setBackground(DARK_BLUE);
		titreLabel.setHorizontalAlignment(JLabel.CENTER);
		titreLabel.setOpaque(true);
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 0;
		gc.gridy = 1;
		gc.gridwidth = 2;
		gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;
		rightPanel.add(titreLabel, gc);



		gc.anchor = GridBagConstraints.WEST;
		gc.gridx = 0;
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.VERTICAL;
		JPanel predictTimePanel = createPredictTimePanel();

		rightPanel.add(predictTimePanel, gc);

		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 1;
		gc.gridy = 2;
		gc.weighty = 1;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		JPanel temperatureSliderComponent = createTemperatureSliderComponent();
		rightPanel.add(temperatureSliderComponent, gc);


		//titre de la section d'optimisation
		//
		JLabel titreLabel2 = new JLabel("Optimisation des camions");
		titreLabel2.setForeground(Color.white);
		titreLabel2.setBackground(DARK_BLUE);
		titreLabel2.setHorizontalAlignment(JLabel.CENTER);
		titreLabel2.setOpaque(true);
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 0;
		gc.gridy = 3;
		gc.gridwidth = 2;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;
		rightPanel.add(titreLabel2, gc);

		gc.anchor = GridBagConstraints.WEST;
		gc.gridx = 0;
		gc.gridy = 4;
		gc.gridwidth = 2;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		rightPanel.add(createOptStrategySection(), gc);


		rightPanel.setOpaque(false);
		return rightPanel;
	}

	private JPanel createTemperatureSliderComponent() {
		//JPanel contenant un label et le slider
		//
		JPanel temperatureSliderPanel = new JPanel();
		temperatureSliderPanel.setOpaque(false);


		//gridBagLayout parce qu'on veut centrer!
		temperatureSliderPanel.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.HORIZONTAL;

		//label du slider
		//
		JLabel meteoLabel = new JLabel("Météo");
		meteoLabel.setHorizontalAlignment(JLabel.CENTER);
		temperatureSliderPanel.add(meteoLabel, gc);

		gc.gridy++;


		//slider
		//
		final JSlider temperatureSlider = new JSlider();
		temperatureSlider.setOpaque(false);
		temperatureSlider.setMaximum(100);
		temperatureSlider.setMinimum(50);
		temperatureSlider.setValue(100);
		//temperatureSlider.setMinimumSize(new Dimension(30, 20));
		temperatureSlider.setMaximumSize(new Dimension(100000, 20));
		temperatureSlider.setPreferredSize(new Dimension(sliderWidth, 20));
		temperatureSliderPanel.add(temperatureSlider, gc);
		//listener change le facteur de meteo de la mine
		temperatureSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				parentFrame.notifyListenersMeteoChanged(1.0*temperatureSlider.getValue()/100);
			}

		});

		gc.gridx = 0;
		gc.gridy=0;
		gc.gridheight = 2;
		gc.weightx = 0;


		JLabel floconLabel = new JLabel(new ImageIcon(floconImage));
		temperatureSliderPanel.add(floconLabel, gc);

		gc.gridx = 2;
		gc.gridy=0;
		gc.gridheight = 2;
		gc.weightx = 0;


		JLabel soleilLabel = new JLabel(new ImageIcon(soleilImage));
		temperatureSliderPanel.add(soleilLabel, gc);

		return temperatureSliderPanel;
	}


	//ajoute le panneau ou l'utilisateur choisit les specifications de la simulation
	//
	private JPanel createSimulationSpecsPanel() {
		JPanel simulationSpecsPanel = new JPanel();


		simulationSpecsPanel.setOpaque(true);
		simulationSpecsPanel.setBackground(DARK_BLUE);

		simulationSpecsPanel.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();

		//ajoute la bordure
		//
		simulationSpecsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Color.BLACK), new EmptyBorder(5, 5, 0, 5) ));








		JLabel nbCamionsLabel = new JLabel(new ImageIcon(camionImage));
		createNumberCamionsTextField();

		createTempsSimulationTextField();
		JLabel horlogeLabel = new JLabel(new ImageIcon(horlogeImage));
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 3;
		gc.anchor = GridBagConstraints.CENTER;
		simulationSpecsPanel.add(createDropdownButton(), gc);

		gc.gridx = 0;
		gc.gridy = 1;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(nbCamionsLabel, gc);

		gc.gridx = 1;
		gc.gridy = 1;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(nbCamionsTextField, gc);


		gc.gridx = 0;
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(horlogeLabel, gc);

		gc.gridx = 1;
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(tempsSimulationTextField, gc);


		gc.gridx = 0;
		gc.gridy = 3;
		gc.gridwidth = 3;
		gc.anchor = GridBagConstraints.CENTER;
		simulationSpecsPanel.add(createLoadButton(), gc);

		return simulationSpecsPanel;
	}

	private JPanel createSimulationControlPanel() {
		JPanel simulationControlPanel = new JPanel();
		simulationControlPanel.setOpaque(false);

		simulationControlPanel.setLayout(new BoxLayout(simulationControlPanel, BoxLayout.X_AXIS));


		simulationControlPanel.add(createPlayPauseButton());

		simulationControlPanel.add(createAutoCompleteButton());

		simulationControlPanel.add(createResetButton());

		simulationControlPanel.add(Box.createHorizontalGlue());

		simulationControlPanel.setOpaque(false);
		return simulationControlPanel;
	}

	private JPanel createOptStrategySection() {

		//panel de strategie
		JPanel optStrategyPanel = new JPanel();
		optStrategyPanel.setOpaque(false);

		optStrategyPanel.setLayout(new BoxLayout(optStrategyPanel, BoxLayout.X_AXIS));

		//label
		//
		JLabel scoreLabel = new JLabel("  Fonction de score :  ");
		optStrategyPanel.add(scoreLabel);

		//textfield
		//
		this.optStrategyTextField = new JTextField();
		//this.scoreFunctionString = parentFrame.getMinePanel().getMine().getScoreFunctionString();
		optStrategyTextField.setText( DecisionMaker.ALEATOIRE_FUNCTION_STRING );
		optStrategyPanel.add(optStrategyTextField);


		//listener
		//
		optStrategyTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				// ne fait rien

			}

			@Override
			public void focusLost(FocusEvent arg0) {

				//valide le input
				String optStrategy = optStrategyTextField.getText();
				if(DecisionMaker.isFunctionStringValid(optStrategy)) {
					optStrategyTextField.setBackground(Color.white);
					
					parentFrame.notifyListenersScoreFunctionChanged(optStrategyTextField.getText());
				}
				else{
					optStrategyTextField.setBackground(Color.red);
				}



			}

		});

		return optStrategyPanel;
	}

	private JPanel createPredictTimePanel() {

		JPanel predictTimePanel = new JPanel();
		predictTimePanel.setOpaque(false);
		predictTimePanel.setLayout(new BoxLayout(predictTimePanel, BoxLayout.X_AXIS));

		//-----------------------------------------
		//Ajoute les champs pour la formule
		//-----------------------------------------

		//label "Formule"
		//
		JLabel formuleLabel = new JLabel("Formule");
		predictTimePanel.add(formuleLabel);


		//label et textField pour n
		//
		nLabel = new JLabel("n =");

		//label et textField pour lembda
		//
		lembdaLabel = new JLabel("\u03bb = ");

		createNTextField();
		createRhoTextField();

		createPredictComboBox();
		//combobox pour choisir la formule
		//


		predictTimePanel.add(this.predictComboBox);



		predictTimePanel.add(nLabel);


		predictTimePanel.add(this.nTextField);



		predictTimePanel.add(lembdaLabel);


		predictTimePanel.add(this.rhoTextField);



		predictTimePanel.add(Box.createHorizontalGlue());

		return predictTimePanel;
	}

	private Component createPredictComboBox() {

		Mine mine = parentFrame.getMine();
		predictComboBox = new JComboBox();
		predictComboBox.addItem("Moyenne des observations précédentes");
		predictComboBox.addItem("Combinaison convexe");
		predictComboBox.addItem("Erreur précédente");

		predictComboBox.setSelectedIndex(mine.getTravelTimePredictor().getfPredictFunction()-1);

		this.setPredictFunctionTextFieldsAccordingToSelectedFunction();

		predictComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				if(event.getStateChange() == ItemEvent.SELECTED) {
					int newPredictFunctionIndex = predictComboBox.getSelectedIndex()+1;
					parentFrame.notifyListenersPredictFunctionChanged(newPredictFunctionIndex);
					setPredictFunctionTextFieldsAccordingToSelectedFunction();
				}
			}

		});

		return predictComboBox;

	}

	protected void setPredictFunctionTextFieldsAccordingToSelectedFunction() {

		int newPredictFunctionIndex = this.predictComboBox.getSelectedIndex()+1;
		//active/desactive les champs des parametres de la fonction de prediction en fonction de la fonctin choisie
		if(newPredictFunctionIndex == TravelTimePredictor.PREDICT_FUNCTION_AVG_PREV) {
			nLabel.setVisible(true);
			nTextField.setVisible(true);
			lembdaLabel.setVisible(false);
			rhoTextField.setVisible(false);
		}
		else if(newPredictFunctionIndex == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED) {
			nLabel.setVisible(false);
			nTextField.setVisible(false);
			lembdaLabel.setVisible(true);
			rhoTextField.setVisible(true);
		}
		else if(newPredictFunctionIndex == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED_ERROR) {
			nLabel.setVisible(false);
			nTextField.setVisible(false);
			lembdaLabel.setVisible(true);
			rhoTextField.setVisible(true);
		}

	}

	private Component createRhoTextField() {
		rhoTextField = new JTextField();
		rhoTextField.setMaximumSize(new Dimension(50, 50));
		rhoTextField.setMinimumSize(new Dimension(50, 20));
		rhoTextField.setPreferredSize(new Dimension(50, 20));
		rhoTextField.setText(""+TravelTimePredictor.DEFAULT_PREDICT_FUNCTION_WEIGHT);
		currentRhoValue = TravelTimePredictor.DEFAULT_PREDICT_FUNCTION_WEIGHT;

		//listener
		rhoTextField.addFocusListener(new FocusListener() {


			@Override
			public void focusGained(FocusEvent arg0) {


			}

			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					double rhoValue =  Double.valueOf(rhoTextField.getText());
					rhoTextField.setBackground(Color.white);

					if(currentRhoValue < 0 || currentRhoValue > 1) {
						throw new NumberFormatException();
					}
					else {
						currentRhoValue = rhoValue;
						parentFrame.notifyListenersRhoChanged(rhoValue);

					}

				}
				catch(NumberFormatException exception) {
					rhoTextField.setText("");
					rhoTextField.setBackground(Color.red);
				}



			}

		});

		return rhoTextField;
	}

	private Component createNTextField() {
		nTextField = new JTextField();
		nTextField.setMaximumSize(new Dimension(50, 50));
		nTextField.setMinimumSize(new Dimension(50, 20));
		nTextField.setPreferredSize(new Dimension(50, 20));
		nTextField.setText(""+TravelTimePredictor.DEFAULT_PREDICT_FUNCTION_NB_SAMLE);
		currentNValue = TravelTimePredictor.DEFAULT_PREDICT_FUNCTION_NB_SAMLE;

		nTextField.addFocusListener(new FocusListener() {


			@Override
			public void focusGained(FocusEvent arg0) {


			}

			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					int nValue =  Integer.valueOf(nTextField.getText());
					nTextField.setBackground(Color.white);
					if(currentNValue < 1) {
						throw new NumberFormatException();
					}
					else {
						currentNValue = nValue;
						parentFrame.notifyListenersNumberSampleChanged(currentNValue);

					}
				}
				catch(NumberFormatException exception) {
					nTextField.setText("");
					nTextField.setBackground(Color.red);
				}



			}

		});
		return nTextField;
	}

	private JButton createLoadButton() {
		JButton loadButton = new JButton();
		loadButton.setText("Charger");
		loadButton.setMinimumSize(new Dimension(100, 10));
		loadButton.setPreferredSize(new Dimension(100, 20));
		loadButton.setMaximumSize(new Dimension(100, 40));


		//Ajoute le listener pour le dropdown
		//On valide que l'usager veut commencer une nouvelle simulation
		//Il faut faire un peu de magie pour que le menu reste coherent avec la simulation dans le cas où l'usager
		// choisit de rester dans la meme simulation
		loadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				//confirme qu'on arrete la simulation
				int dialogButton = JOptionPane.YES_NO_OPTION;
				int result = JOptionPane.showConfirmDialog (null, "Voulez-vous vraiment arrêter cette simulation et en démarrer une nouvelle?","Attention", dialogButton);
				if(result == JOptionPane.YES_OPTION) {
					int indexSelected = mineComboBox.getSelectedIndex();
					int exempleNb=-1;
					if(indexSelected == 0) {
						exempleNb = Mine.EXEMPLE1;
					}
					else if(indexSelected == 1) {
						exempleNb = Mine.EXEMPLE2;
					}
					parentFrame.notifyListenersNewSimulationRequested(exempleNb, getNumberOfCamions(), getTempsSimulationSeconds());


				}

			}
		});

		return loadButton;
	}

	private Component createAutoCompleteButton() {
		this.completeButton = new JButton("Completer Auto.");
		completeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				parentFrame.notifyListenersAutomaticCompletionRequested();

			}

		});
		return completeButton;
	}

	private JCheckBox createCheckBox() {


		//ajoute la checkbox
		//
		final JCheckBox checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setText("pause a chaque fin de voyage");
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				parentFrame.notifyListenersStopOnAssignStateChanged(checkBox.isSelected());
			}

		});
		return checkBox;
	}

	private void createNumberCamionsTextField() {

		Mine mine = parentFrame.getMine();
		//nbCamionsLabel.setMaximumSize(new Dimension(100, 40));
		//nbCamionsLabel.setMinimumSize(new Dimension(100, 40));
		//nbCamionsLabel.setPreferredSize(new Dimension(100, 40));


		this.nbCamionsTextField = new JTextField();
		this.selectedNumberOfCamions = mine.getCamions().size();
		nbCamionsTextField.setText(""+mine.getCamions().size());
		nbCamionsTextField.setMaximumSize(new Dimension(50, 50));
		nbCamionsTextField.setMinimumSize(new Dimension(50, 20));
		nbCamionsTextField.setPreferredSize(new Dimension(50, 20));
		nbCamionsTextField.addFocusListener(new FocusListener() {


			@Override
			public void focusGained(FocusEvent arg0) {


			}

			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					int nbCamions =  Integer.valueOf(nbCamionsTextField.getText());
					nbCamionsTextField.setBackground(Color.white);
					selectedNumberOfCamions = nbCamions;
					if(selectedNumberOfCamions < 1) {
						throw new NumberFormatException();
					}
				}
				catch(NumberFormatException exception) {
					nbCamionsTextField.setText("");
					nbCamionsTextField.setBackground(Color.red);
				}



			}

		});
	}

	//panel contenant un champs texte indiquant le temps total de simulation
	private void createTempsSimulationTextField() {


		this.tempsSimulationTextField = new JTextField();
		tempsSimulationTextField.setText(""+SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/3600);
		this.selectedTempsSimulation = SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/3600;

		tempsSimulationTextField.setMaximumSize(new Dimension(50, 50));
		tempsSimulationTextField.setMinimumSize(new Dimension(50, 20));
		tempsSimulationTextField.setPreferredSize(new Dimension(50, 20));


		tempsSimulationTextField.addFocusListener(new FocusListener() {


			@Override
			public void focusGained(FocusEvent arg0) {

			}

			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					double temps =  Double.valueOf(tempsSimulationTextField.getText());
					tempsSimulationTextField.setBackground(Color.white);
					selectedTempsSimulation = temps;

					if(selectedTempsSimulation <= 0) {
						throw new NumberFormatException();
					}
				}
				catch(NumberFormatException exception) {
					tempsSimulationTextField.setText("");
					tempsSimulationTextField.setBackground(Color.red);
				}


			}

		});

	}

	//Ajoute le panneau de vitesse qui comprend : 
	// - un slider de vitesse

	private JPanel createSpeedComponent() {
		JPanel speedPanel = new JPanel();
		speedPanel.setOpaque(false);
		speedPanel.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;

		JLabel vitesseLabel = new JLabel("Vitesse");
		vitesseLabel.setHorizontalAlignment(JLabel.CENTER);

		speedPanel.add(vitesseLabel, gc);


		//ajoute le slider
		//
		final JSlider speedSlider = new JSlider();
		speedSlider.setOpaque(false);
		speedSlider.setMaximum(51);
		speedSlider.setValue(26);
		speedSlider.setMinimum(1);
		speedSlider.setOrientation(JSlider.HORIZONTAL);
		speedSlider.setMinimumSize(new Dimension(sliderWidth, 20));
		speedSlider.setMaximumSize(new Dimension(sliderWidth, 20));
		speedSlider.setPreferredSize(new Dimension(sliderWidth, 20));

		speedSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				parentFrame.notifyListenersSimulationSpeedChanged(speedSlider.getValue());
			}

		});

		gc.gridy = 1;
		gc.weightx = 1;

		speedPanel.add(speedSlider, gc);

		gc.gridy = 2;

		speedPanel.add(createCheckBox(), gc);

		return speedPanel;

	}

	//ajoute le bouton reset
	private JButton createResetButton() {

		Mine mine = parentFrame.getMine();
		JButton resetButton = new JButton();
		final int exemple = mine.getExemple();
		final int nbCamions = mine.getCamions().size();
		
		resetButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parentFrame.notifyListenersResetSimulationRequested();
			}
		});

		resetButton.setText("reset");
		//grandeur du bouton
		//
		resetButton.setMinimumSize(new Dimension(100, 10));
		resetButton.setPreferredSize(new Dimension(100, 20));
		resetButton.setMaximumSize(new Dimension(100, 40));

		return resetButton;
	}

	//ajoute le bouton play/pause
	//
	private JButton createPlayPauseButton() {
		//bouton
		this.playPauseButton = new PlayPauseButton();
		//ajoute le listener
		playPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//playPauseButton.toogle();

				if(playPauseButton.getState() == PlayPauseButton.STATE_PAUSE) {
					optStrategyTextField.setEditable(true);
					parentFrame.notifyListenersPlayButtonPressed();
				}
				else {
					optStrategyTextField.setEditable(false);
					parentFrame.notifyListenersPauseButtonPressed();
					
				}


			}

		});


		//grandeur du bouton
		//
		playPauseButton.setMinimumSize(new Dimension(100, 10));
		playPauseButton.setPreferredSize(new Dimension(100, 20));
		playPauseButton.setMaximumSize(new Dimension(100, 40));

		return playPauseButton;
	}

	//Ajoute le dropdown pour choisir la mine
	//
	private JComboBox createDropdownButton() {

		Mine mine = parentFrame.getMine();

		final JComboBox<Mine.ExampleId> comboBox = new JComboBox<Mine.ExampleId>();
		
		comboBox.setRenderer( new ListCellRenderer<Mine.ExampleId>(){
			
			
			@Override
			public JLabel getListCellRendererComponent(JList<? extends ExampleId> list, ExampleId value, int index,
					boolean isSelected, boolean cellHasFocus) {

						        
		        if (isSelected) {
		            setBackground(list.getSelectionBackground());
		            setForeground(list.getSelectionForeground());
		        } else {
		            setBackground(list.getBackground());
		            setForeground(list.getForeground());
		        }

				// TODO Auto-generated method stub
				return new JLabel(value.getName());
			}
			
		});
		comboBox.setMaximumSize(new Dimension(1000, 30));
		
		ArrayList<Mine.ExampleId> exampleIds = Mine.exampleIds;
		for(int i = 0; i < exampleIds.size(); i++){
			//Ajoute les 2  options possibles
			
			comboBox.addItem(exampleIds.get(i));
		}

		comboBox.setSelectedIndex(mine.getExemple()-1);


		mineComboBox = comboBox;
		return comboBox;


	}

	public int getNumberOfCamions() {

		return this.selectedNumberOfCamions;
	}


	//classe pour le bouton play/pause
	private class PlayPauseButton extends JButton{
		static final int STATE_PLAY = 1;
		static final int STATE_PAUSE = 2;

		private int state;

		public PlayPauseButton() {
			super();
			this.state = STATE_PAUSE;
			this.setText("Play");


		}
		public int getState() {
			return this.state;
		}
		public void toogle() {

			if(state == STATE_PLAY) {
				this.setText("Play");
				this.state = STATE_PAUSE;
			}
			else {
				this.setText("Pause");
				this.state = STATE_PLAY;
			}
		}

	}



	public double getTempsSimulationSeconds() {

		//convertis le temps en secondes et le renvoie
		return this.selectedTempsSimulation*3600;
	}

	public JTextField getOptStrategyTextField() {
		return optStrategyTextField;
	}

	//change entre le mode play et le mode pause
	// Mode play : Bouton affiche "pause", champ de score ne peut être édité
	// Mode pause : Bouton affiche "play, champ de score peut être édité
	public void tooglePlayPause() {
		this.playPauseButton.toogle();

		if(playPauseButton.getState() == PlayPauseButton.STATE_PAUSE) {
			optStrategyTextField.setEditable(true);
		}
		else {
			optStrategyTextField.setEditable(false);
		}

	}

	//met en mode play.
	public void setPlayMode() {
		if(playPauseButton.getState() == PlayPauseButton.STATE_PAUSE) {
			tooglePlayPause();
		}
	}

	//met en mode pause
	public void setPauseMode() {
		if(playPauseButton.getState() == PlayPauseButton.STATE_PLAY) {
			tooglePlayPause();
		}

	}


}
