package ca.polymtl.SimulationMine.MineGui;
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

import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.Mine.ExampleId;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.decisionMaker.DecisionMaker;
import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;
import javafx.util.Pair;

/**
 * Panneau de controle de la simulation. Permet de charger ou recharger une simulation, controller ses param�tres, ...
 * @author Fred
 *
 */
public class JControlPanel extends JPanel{


	/**
	 * classe pour le bouton play/pause
	 * @author Fred
	 *
	 */
	private class PlayPauseButton extends JButton{
		
		private static final long serialVersionUID = 1L;
		//�tats possibles
		/** �tat "play"*/
		static final int STATE_PLAY = 1;
		/** �tat "pause" */
		static final int STATE_PAUSE = 2;

		/**�tat courant*/
		private int state;

		/**
		 * constructeur. D�bute en �tat "play"
		 */
		public PlayPauseButton() {
			super();
			this.state = STATE_PAUSE;
			this.setText("Play");


		}
		
		/**
		 * 
		 * @return �tat courant
		 */
		public int getState() {
			return this.state;
		}
		
		/**
		 * Change l'�tat de pause � play ou vice versa
		 */
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

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//----------------------------------
	//constantes
	//----------------------------------
	/** largeur des sliders	 */
	private static int SLIDER_WIDTH_PX = 400;
	/**couleur bleu fonc� */
	public static final Color DARK_BLUE = new Color(19, 86, 174);
	private static final Color LIGHT_BLUE = new Color(207, 236, 255);

	private static final Color COLOR_WRONG_INPUT = new Color(255, 146, 146);

	//Images
	//
	/**Image de soleil*/
	private BufferedImage soleilImage;
	/**Image de flocon*/
	private BufferedImage floconImage;
	/**Image de petit camion*/
	private BufferedImage camionSmallImage;
	/**Image de gros camion*/
	private BufferedImage camionLargeImage;

	//----------------------------------
	//champs
	//----------------------------------

	/**Image d'horloge*/
	private BufferedImage horlogeImage;

	/**frame contenant le GUI*/
	private JMineFrame parentFrame;
	//�l�ments du panneau "parametres de simulation"
	//
	/**liste d�roulante permettant de choisir la mine*/
	private JComboBox<ExampleId> mineComboBox;
	/**champs pour le nombre de petit camions*/
	private JTextField nbSmallCamionsTextField;
	/**champs pour le nombre de gros camions*/
	private JTextField nbLargeCamionsTextField;


	/**champs pour le temps de simulation*/
	private JTextField tempsSimulationTextField;
	//controle de la simulation
	//
	/**bouton play/pause*/
	private PlayPauseButton playPauseButton;

	/**bouton pour completer automatiquement la simulation*/
	private JButton completeButton;

	//optimisation
	//
	/**champs contenant la fonction de score*/
	private JTextField scoreFunctionTextField;
	//prediction du temps de parcours
	//
	/**Liste d�roulante pour la formule d'estimation de temps de parcours*/
	private JComboBox<Pair<Integer, String>> predictComboBox;
	/**Label pour la param�tre n*/
	private JLabel nLabel;
	/**champs pour le param�tre n*/
	private JTextField nTextField;
	/**label pour le param�tre lambda*/
	private JLabel lambdaLabel;

	/**champs pour le param�tre lambda*/
	private JTextField lambdaTextField;
	//valeurs des champs
	//
	/**Temps de simulation*/
	protected double selectedTempsSimulation;
	/**Nombre de petits camions*/
	protected int selectedNumberOfSmallCamions;
	/**Nombre de gros camions*/
	private int selectedNumberOfLargeCamions;
	/**valeur de n*/
	private int currentNValue;

	/**valeur de lambda*/
	private double currentLambdaValue;

	/**
	 * Constructeur
	 * @param frame : frame qui est le parent de cet objet.
	 */
	public JControlPanel(JMineFrame frame ){
		super();

		this.parentFrame = frame;

		loadImages();

		this.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.black));

		this.setBackground(new Color(207, 236, 255));


		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		//panneau de gauche
		this.add(createSimulationSpecsPanel());

		this.add(createRightPanel());

	}

	/**
	 * 
	 * @return Nombre de gros camions dans le champs
	 */
	public int getNumberOfLargeCamions() {
		return this.selectedNumberOfLargeCamions;
	}

	/**
	 * 
	 * @return Nombre de petits camions dans le champs
	 */
	public int getNumberOfSmallCamions() {
		return this.selectedNumberOfSmallCamions;
	}

	/**
	 * 
	 * @returnle temps de simulation (en secondes) inscrite dans le champs
	 */
	public double getTempsSimulationSeconds() {

		//convertis le temps en secondes et le renvoie
		return this.selectedTempsSimulation*3600;
	}

	/**
	 * met en mode pause
	 */
	public void setPauseMode() {
		if(playPauseButton.getState() == PlayPauseButton.STATE_PLAY) {
			tooglePlayPause();
		}

	}

	/**met en mode play.
	 * 
	 */
	public void setPlayMode() {
		if(playPauseButton.getState() == PlayPauseButton.STATE_PAUSE) {
			tooglePlayPause();
		}
	}

	/**
	 * Cr�� le bouton "completer Auto"
	 * @return bouton
	 */
	private Component createAutoCompleteButton() {
		this.completeButton = new JButton("Completer Auto.");
		
		//notifie les listeners lorsqu'appuy�
		completeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				parentFrame.notifyListenersAutomaticCompletionRequested();

			}

		});
		//dimensions du bouton
		//
		completeButton.setMinimumSize(new Dimension(140, 10));
		completeButton.setPreferredSize(new Dimension(140, 20));
		completeButton.setMaximumSize(new Dimension(140, 40));
		
		return completeButton;
	}

	/**
	 * Cr�� le checkbox pour faire pause � la fin de chaque voyage
	 * @return checkbox
	 */
	private JCheckBox createCheckBox() {


		//ajoute la checkbox
		//
		final JCheckBox checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setText("pause a chaque fin de voyage");
		
		checkBox.setSelected(parentFrame.getConfig().isDefaultPauseFinVoyage());
		//notifie les listeners lorsque coch�e/d�coch�e
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				parentFrame.notifyListenersStopOnAssignStateChanged(checkBox.isSelected());
			}

		});
		
		return checkBox;
	}

	/**
	 * cr�� le champs pour la valeur de lambda
	 * @return champs pour la valeur de lambda
	 */
	private Component createLambdaTextField() {
		lambdaTextField = new JTextField();
		
		//dimensions
		lambdaTextField.setMaximumSize(new Dimension(50, 50));
		lambdaTextField.setMinimumSize(new Dimension(50, 20));
		lambdaTextField.setPreferredSize(new Dimension(50, 20));
		
		//valeur par d�faut
		lambdaTextField.setText(""+parentFrame.getConfig().getDefaultTimePredictLambda());
		currentLambdaValue = parentFrame.getConfig().getDefaultTimePredictLambda();

		//listener
		//valide l'input lorsque la valeur change
		//
		lambdaTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				//Ne fait rien
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					double rhoValue =  Double.valueOf(lambdaTextField.getText());
					lambdaTextField.setBackground(Color.white);

					if(currentLambdaValue < 0 || currentLambdaValue > 1) {
						throw new NumberFormatException();
					}
					else {
						currentLambdaValue = rhoValue;
						parentFrame.notifyListenersRhoChanged(rhoValue);
					}
				}
				catch(NumberFormatException exception) {
					lambdaTextField.setText("");
					lambdaTextField.setBackground(COLOR_WRONG_INPUT);
				}
			}
		});

		return lambdaTextField;
	}

	/**
	 * Cr�� le bouton "Charger"
	 * @return le bouton "Charger"
	 */
	private JButton createLoadButton() {
		
		JButton loadButton = new JButton();
		
		//texte du bouton
		loadButton.setText("Charger");
		
		//dimensions
		//
		loadButton.setMinimumSize(new Dimension(100, 10));
		loadButton.setPreferredSize(new Dimension(100, 20));
		loadButton.setMaximumSize(new Dimension(100, 40));


		//Ajoute le listener pour le dropdown
		//On valide que l'usager veut commencer une nouvelle simulation
		//Il faut faire un peu de magie pour que le menu reste coherent avec la simulation dans le cas o� l'usager
		// choisit de rester dans la meme simulation
		loadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				//valide les arguments
				if(validSimulationArguments()) {

					//confirme qu'on arrete la simulation
					int dialogButton = JOptionPane.YES_NO_OPTION;
					int result = JOptionPane.showConfirmDialog (null, "Voulez-vous vraiment arr�ter cette simulation et en d�marrer une nouvelle?","Attention", dialogButton);
					if(result == JOptionPane.YES_OPTION) {
						ExampleId selectedId = (ExampleId) mineComboBox.getSelectedItem();
						System.out.println("selected id : "+selectedId.getName()+" "+selectedId.getFileName());

						parentFrame.notifyListenersNewSimulationRequested(selectedId, getNumberOfSmallCamions(), getNumberOfLargeCamions(), getTempsSimulationSeconds());


					}
				}
				else {
					JOptionPane.showMessageDialog(null, "Impossible de d�marrer une simulation avec les param�tres sp�cifi�s.");

				}

			}
		});

		return loadButton;
	}
	
	/**
	 * Cr�� le dropdown pour choisir la mine
	 */
	private JComboBox<ExampleId> createMineConfigDrowdown() {

		//dropdown
		final JComboBox<Mine.ExampleId> comboBox = new JComboBox<Mine.ExampleId>();

		//pour �crire le nom de la mine dans la liste
		//
		comboBox.setRenderer( new ListCellRenderer<Mine.ExampleId>(){
			@Override
			public JLabel getListCellRendererComponent(JList<? extends ExampleId> list, ExampleId value, int index,
					boolean isSelected, boolean cellHasFocus) {

				return new JLabel(value.getName());
			}

		});
		
		//dimensions
		comboBox.setMaximumSize(new Dimension(1000, 30));

		//items de la liste
		//
		ArrayList<Mine.ExampleId> exampleIds = Mine.exampleIds;
		for(int i = 0; i < exampleIds.size(); i++){
			comboBox.addItem(exampleIds.get(i));
		}

		int selectedIndex = -1;
		for(int i = 0 ; i < Mine.exampleIds.size(); i++) {
			ExampleId exId = Mine.exampleIds.get(i);
			if(exId.getId().compareTo(parentFrame.getConfig().getDefaultMineId()) == 0 ) {
				selectedIndex = i;
			}
		}
		if(selectedIndex == -1) {
			throw new IllegalStateException("JControlPanel::createMineConfigDrowdown Je ne trouve pas la mine avec l'id "+parentFrame.getConfig().getDefaultMineId());
		}
		
		//item s�lectionn�
		comboBox.setSelectedIndex(selectedIndex);

		mineComboBox = comboBox;
		return comboBox;
	}

	/**
	 * Cr�� le champs pour la valeur de n
	 * @return champs pour valeur de n
	 */
	private Component createNTextField() {
		nTextField = new JTextField();
		
		//dimensions
		//
		nTextField.setMaximumSize(new Dimension(50, 50));
		nTextField.setMinimumSize(new Dimension(50, 20));
		nTextField.setPreferredSize(new Dimension(50, 20));
		
		//valeur par d�faut
		//
		nTextField.setText(""+parentFrame.getConfig().getDefaultTimePredictN());
		currentNValue = parentFrame.getConfig().getDefaultTimePredictN();

		//listener
		//valide la valeur et notifie les listeners
		nTextField.addFocusListener(new FocusListener() {


			@Override
			public void focusGained(FocusEvent arg0) {
				//ne fait rien
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
					nTextField.setBackground(COLOR_WRONG_INPUT);
				}
			}
		});
		return nTextField;
	}

	/**
	 * Cree le textfield dans lequel l'utilisateur choisis le nombre de camions
	 */
	private void createNumberCamionsLargeTextField() {

		//Cr�� le champs
		this.nbLargeCamionsTextField = new JTextField();
		
		//valeur par d�faut
		//
		this.selectedNumberOfLargeCamions = parentFrame.getMine().getNumberLargeCamions();
		nbLargeCamionsTextField.setText(""+selectedNumberOfLargeCamions);
		
		//Dimensions
		//
		nbLargeCamionsTextField.setMaximumSize(new Dimension(50, 50));
		nbLargeCamionsTextField.setMinimumSize(new Dimension(50, 20));
		nbLargeCamionsTextField.setPreferredSize(new Dimension(50, 20));
		
		//Lorsqu'on entre une nouvelle valeur, valide l'input
		//
		nbLargeCamionsTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				//ne fait rien
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					int nbCamions =  Integer.valueOf(nbLargeCamionsTextField.getText());
					nbLargeCamionsTextField.setBackground(Color.white);
					selectedNumberOfLargeCamions = nbCamions;
					if(selectedNumberOfLargeCamions < 0) {
						throw new NumberFormatException();
					}
				}
				catch(NumberFormatException exception) {
					nbLargeCamionsTextField.setText("");
					nbLargeCamionsTextField.setBackground(COLOR_WRONG_INPUT);
				}
			}
		});
	}

	/**
	 * Cr�� le champs pour le nombre de petits camions
	 */
	private void createNumberCamionsSmallTextField() {

		//cree le champs
		this.nbSmallCamionsTextField = new JTextField();
		
		//valeur par d�faut = nombre de petits camions dans la mine
		//
		this.selectedNumberOfSmallCamions = parentFrame.getMine().getNumberSmallCamions();
		nbSmallCamionsTextField.setText(""+this.selectedNumberOfSmallCamions);

		//dimensions du champs
		nbSmallCamionsTextField.setMaximumSize(new Dimension(50, 50));
		nbSmallCamionsTextField.setMinimumSize(new Dimension(50, 20));
		nbSmallCamionsTextField.setPreferredSize(new Dimension(50, 20));
		
		//lorsqu'on entre une valeur et qu'on quitte le focus, valide la valeur et colore en rouge si valeur invalide
		nbSmallCamionsTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				//ne fait rien

			}
			//valide la valeur lorsque le camps perd le focus
			@Override
			public void focusLost(FocusEvent arg0) {
				//valide le texte
				try {
					int nbCamions =  Integer.valueOf(nbSmallCamionsTextField.getText());
					nbSmallCamionsTextField.setBackground(Color.white);
					selectedNumberOfSmallCamions = nbCamions;
					if(selectedNumberOfSmallCamions < 0) {
						throw new NumberFormatException();
					}
				}
				catch(NumberFormatException exception) {
					nbSmallCamionsTextField.setText("");
					nbSmallCamionsTextField.setBackground(COLOR_WRONG_INPUT);
				}



			}

		});
	}
	
	/**
	 * Panneau avec fonction de score. Il comprend : 
	 * 1) Nom du champs "Fonction de score"
	 * 2) Champs
	 * @return panneau avec fonction de score
	 */
	private JPanel createOptStrategySection() {

		//panel � retourner
		JPanel optStrategyPanel = new JPanel();
		optStrategyPanel.setOpaque(false);

		//layout horizontal
		optStrategyPanel.setLayout(new BoxLayout(optStrategyPanel, BoxLayout.X_AXIS));

		//-------------------------------
		//Cr�� les �l�ments
		//-------------------------------
		
		//label
		JLabel scoreLabel = new JLabel("  Fonction de score :  ");
		
		//textfield
		//
		this.scoreFunctionTextField = new JTextField();
		//this.scoreFunctionString = parentFrame.getMinePanel().getMine().getScoreFunctionString();
		
		String scoreFunction = parentFrame.getConfig().getDefaultScoreFunction();
		
		if( ! DecisionMaker.isFunctionStringValid(scoreFunction) ) {
			throw new IllegalArgumentException("la fonction de score par defaut n'est pas valide : "+scoreFunction);
		}
		
		scoreFunctionTextField.setText( scoreFunction );
		
		//listener
		// Valide l'input lorsqu'une nouvelle valeur est entr�e
		//
		scoreFunctionTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				// ne fait rien

			}

			@Override
			public void focusLost(FocusEvent arg0) {

				//valide l'input
				String optStrategy = scoreFunctionTextField.getText();
				if(DecisionMaker.isFunctionStringValid(optStrategy)) {
					scoreFunctionTextField.setBackground(Color.white);

					parentFrame.notifyListenersScoreFunctionChanged(scoreFunctionTextField.getText());
				}
				else{
					scoreFunctionTextField.setBackground(COLOR_WRONG_INPUT);
				}



			}

		});
		
		//----------------------------------------
		// Ajoute les �l�ments
		//----------------------------------------
		//label
		optStrategyPanel.add(scoreLabel);
		//champs
		optStrategyPanel.add(scoreFunctionTextField);



		return optStrategyPanel;
	}

	/**
	 * Cr�� le bouton play/pause
	 * @return bouton play/pause
	 */
	private JButton createPlayPauseButton() {
		
		//bouton
		this.playPauseButton = new PlayPauseButton();
		
		//ajoute le listener
		//notifie les listeners du bouton
		playPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//playPauseButton.toogle();

				if(playPauseButton.getState() == PlayPauseButton.STATE_PAUSE) {
					scoreFunctionTextField.setEditable(true);
					parentFrame.notifyListenersPlayButtonPressed();
				}
				else {
					scoreFunctionTextField.setEditable(false);
					parentFrame.notifyListenersPauseButtonPressed();

				}


			}

		});


		//dimensions du bouton
		//
		playPauseButton.setMinimumSize(new Dimension(100, 10));
		playPauseButton.setPreferredSize(new Dimension(100, 20));
		playPauseButton.setMaximumSize(new Dimension(100, 40));

		return playPauseButton;
	}

	/**
	 * Cr�� le dropdown pour les formules de pr�diction
	 */
	private void createPredictComboBox() {

		//Mine mine = parentFrame.getMine();
		predictComboBox = new JComboBox<Pair<Integer, String>>();
		
		ArrayList<Pair<Integer, String>> travelTimePredicrFunctionNames = TravelTimePredictor.travelTimePredictFunctionNames();
		
		
		for(Pair<Integer, String> function : travelTimePredicrFunctionNames ) {
			predictComboBox.addItem(function);
		}
		
		int selectedFunction = parentFrame.getConfig().getDefaultTimePredictFormula();
		int selectedIndex = -1;
		for(int i = 0 ; i < predictComboBox.getItemCount(); i++) {
			if(predictComboBox.getItemAt(i).getKey() == selectedFunction) {
				selectedIndex = i;
				break;
			}
		}
		
		if(selectedIndex == -1) {
			throw new IllegalStateException("JControlPanel::createPredictComboBox Impossible de trouver la fonction de prediction de numero"+selectedFunction);
		}
		
		predictComboBox.setSelectedIndex(selectedIndex);
		
		predictComboBox.setRenderer(new ListCellRenderer<Pair<Integer, String>>(){

			@Override
			public JLabel getListCellRendererComponent(JList<? extends Pair<Integer, String>> list,
					Pair<Integer, String> value, int index, boolean isSelected, boolean cellHasFocus) {
				return new JLabel(value.getValue());
			}});

		this.setPredictFunctionTextFieldsAccordingToSelectedFunction();

		//change le champs de param�tre visible si on change la formule de pr�diction
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

	}

	/**
	 * Panneau en lien avec la pr�diction de temps de parcours. Il contient : 
	 * 1) dropdown pour choisir la formule de pr�diction
	 * 2) champs de texte pour choisir le param�tre de la formule
	 * 
	 * @return Panneau "temps de pr�diction"
	 */
	private JPanel createPredictTimePanel() {

		//Cr�� le panneau � retourner
		//
		JPanel predictTimePanel = new JPanel();
		predictTimePanel.setOpaque(false);
		

		//-----------------------------------------
		//Cr�� les �l�ments
		//-----------------------------------------

		//label "Formule"
		//
		JLabel formuleLabel = new JLabel("Formule");
		

		//label et textField pour n
		//
		nLabel = new JLabel("n =");

		//label et textField pour lambda
		//
		lambdaLabel = new JLabel("\u03bb = ");

		//champs de texte pour n
		createNTextField();
		
		//champs de teste pour lambda
		createLambdaTextField();
		
		//dropdown pour formule de pr�diction
		createPredictComboBox();

		//--------------------------------------
		//ajoute les �l�ments
		//--------------------------------------
		predictTimePanel.setLayout(new BoxLayout(predictTimePanel, BoxLayout.X_AXIS));
		
		//label "formule"
		predictTimePanel.add(formuleLabel);

		//dropdown
		predictTimePanel.add(this.predictComboBox);

		//label "n"
		predictTimePanel.add(nLabel);

		//champs "n"
		predictTimePanel.add(this.nTextField);

		//label "lambda"
		predictTimePanel.add(lambdaLabel);

		//champs "lambda"
		predictTimePanel.add(this.lambdaTextField);


		return predictTimePanel;
	}

	/**
	 * Cr�� le bouton reset
	 * @return bouton reset
	 */
	private JButton createResetButton() {

		JButton resetButton = new JButton();

		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				parentFrame.notifyListenersResetSimulationRequested();
			}
		});

		resetButton.setText("reset");
		
		//dimensions du bouton
		//
		resetButton.setMinimumSize(new Dimension(100, 10));
		resetButton.setPreferredSize(new Dimension(100, 20));
		resetButton.setMaximumSize(new Dimension(100, 40));

		return resetButton;
	}

	/**
	 * Cr�� le panneau de droite, contenant les controlleurs de la simulation. 
	 * Il est organis� en trois sections, verticalement :
	 * 
	 *  1) Contr�les de la simulation (play/pause, completer auto, reset, slider de vitesse)
	 *  2) Pr�diction des temps de parcours
	 *  3) Fonction de score pour optimiser l'assignation des camions aux pelles
	 * 
	 * @return JPanel correspondant au panneau de droite.
	 */
	private JPanel createRightPanel() {

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridBagLayout());

		//controles de la simulation (bouton play/pause, completer auto, rest)
		//
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.VERTICAL;
		rightPanel.add(createSimulationControlPanel(), gc);

		//slider de vitesse (+case "pause � haque fin de voyage")
		//
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		JPanel speedComponent = createSpeedComponent();
		rightPanel.add(speedComponent, gc);


		//titre de la section "Pr�diction du temps de parcours"
		//
		//Cr�ation
		JLabel titreLabel = new JLabel("Pr�diction du temps de parcours");
		titreLabel.setForeground(Color.white);
		titreLabel.setBackground(DARK_BLUE);
		titreLabel.setHorizontalAlignment(JLabel.CENTER);
		titreLabel.setOpaque(true);

		//ajout
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 0;
		gc.gridy = 1;
		gc.gridwidth = 2;
		gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;
		rightPanel.add(titreLabel, gc);


		//Section "Pr�diction du temps de parcours"
		//
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 0;
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.VERTICAL;
		JPanel predictTimePanel = createPredictTimePanel();
		rightPanel.add(predictTimePanel, gc);

		//slider de m�t�o
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
		//cr�ation
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

		//ajout
		gc.anchor = GridBagConstraints.WEST;
		gc.gridx = 0;
		gc.gridy = 4;
		gc.gridwidth = 2;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		rightPanel.add(createOptStrategySection(), gc);

		//background bleu pale
		rightPanel.setBackground(JControlPanel.LIGHT_BLUE);
		rightPanel.setOpaque(true);
		return rightPanel;
	}

	/**
	 * Cr�� le panneau qui contr�le la simulation. Il est compos� de : 
	 * @return Panneau de contr�le
	 */
	private JPanel createSimulationControlPanel() {
		JPanel simulationControlPanel = new JPanel();
		simulationControlPanel.setOpaque(false);

		//layout horizontal
		simulationControlPanel.setLayout(new BoxLayout(simulationControlPanel, BoxLayout.X_AXIS));

		//bouton play/pause
		simulationControlPanel.add(createPlayPauseButton());

		//bouton completer auto
		simulationControlPanel.add(createAutoCompleteButton());

		//bouton reset
		simulationControlPanel.add(createResetButton());

		return simulationControlPanel;
	}

	/**
	 * Cr�� le panneau ou l'utilisateur choisit les param�tres de simulation : 
	 * 1) configuration de mine
	 * 2) Nombre de camions de chaque type
	 * 3) dur�e de simulation
	 * 
	 * Le panneau contient �galement un bouton pour charger la simulation
	 * 
	 * @return paneau des param�tres de simulation
	 */
	private JPanel createSimulationSpecsPanel() {
		//panel � retourner
		JPanel simulationSpecsPanel = new JPanel();

		//couleur du panneau
		simulationSpecsPanel.setOpaque(true);
		simulationSpecsPanel.setBackground(DARK_BLUE);



		//ajoute la bordure
		simulationSpecsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Color.BLACK), new EmptyBorder(5, 5, 0, 5) ));

		//----------------------------------------------
		//Cr�� les �l�ments avant de les ajouter
		//----------------------------------------------

		//choix de configuration
		JComboBox<ExampleId> mineConfigBox = createMineConfigDrowdown();

		//nombre de petit camions
		JLabel nbCamionsSmallLabel = new JLabel(new ImageIcon(camionSmallImage));
		createNumberCamionsSmallTextField();

		//nombre de gros camions
		JLabel nbCamionsLargeLabel = new JLabel(new ImageIcon(camionLargeImage));
		createNumberCamionsLargeTextField();

		//temps de simulation
		createTempsSimulationTextField();
		JLabel horlogeLabel = new JLabel(new ImageIcon(horlogeImage));


		//-----------------------------------------------
		// Ajoute les �l�ments
		//----------------------------------------------

		//gridbaglayout
		simulationSpecsPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();

		//-----------------------------------------
		//Premi�re ligne :
		
		//dropdown de s�lection de mine
		//
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 4;
		gc.anchor = GridBagConstraints.CENTER;
		simulationSpecsPanel.add(mineConfigBox, gc);

		//-----------------------------------------
		//Deuxi�me ligne :
				
		// image petits camions
		//
		gc.gridx = 0;
		gc.gridy = 1;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(nbCamionsSmallLabel, gc);

		// champs petits camions
		//
		gc.gridx = 1;
		gc.gridy = 1;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(nbSmallCamionsTextField, gc);

		// Image gros camions
		//
		gc.gridx = 2;
		gc.gridy = 1;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(nbCamionsLargeLabel, gc);

		// Champs gros camions
		//
		gc.gridx = 3;
		gc.gridy = 1;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(nbLargeCamionsTextField, gc);


		//-----------------------------------------
		//Troisi�me ligne :
		
		// Image temps de simulation
		//
		gc.gridx = 0;
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(horlogeLabel, gc);

		// Champs temps de simulation
		//
		gc.gridx = 1;
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.EAST;
		simulationSpecsPanel.add(tempsSimulationTextField, gc);

		//-----------------------------------------
		//Quatri�me ligne :
				
		//Bouton charger
		//
		gc.gridx = 0;
		gc.gridy = 3;
		gc.gridwidth = 4;
		gc.anchor = GridBagConstraints.CENTER;
		simulationSpecsPanel.add(createLoadButton(), gc);

		return simulationSpecsPanel;
	}

	/**
	 * Ajoute le panneau de vitesse qui comprend : 
	 *  1) titre du slider
	 *  2) slider
	 *  3) case � cocher pour que la simulation pause chaque fois qu'un camion arrive au concentrateur ou au st�rile 
	 * @return JPanel correspondant � la zone de slider de vitesse
	 */
	private JPanel createSpeedComponent() {
		
		//cree le panel
		//
		JPanel speedPanel = new JPanel();
		speedPanel.setOpaque(false);
		
		
		//-----------------------------------
		//Cr�� les �l�ments
		//-----------------------------------
		//titre du slider
		//
		JLabel vitesseLabel = new JLabel("Vitesse");
		vitesseLabel.setHorizontalAlignment(JLabel.CENTER);

		//slider
		//
		final JSlider speedSlider = new JSlider();
		speedSlider.setOpaque(false);
		speedSlider.setMaximum(51);
		speedSlider.setMinimum(1);
		
		int defaultValue = parentFrame.getConfig().getDefaultSimultaionSpeed();
		if(defaultValue < speedSlider.getMinimum() || defaultValue > speedSlider.getMaximum()) {
			throw new IllegalStateException("La vitesse de simulation doit etre entre "+speedSlider.getMinimum()+" et "+speedSlider.getMaximum()+". "+defaultValue+" fourni.");
		}
		speedSlider.setValue(defaultValue);
		
		
		speedSlider.setOrientation(JSlider.HORIZONTAL);
		speedSlider.setMinimumSize(new Dimension(SLIDER_WIDTH_PX, 20));
		speedSlider.setMaximumSize(new Dimension(SLIDER_WIDTH_PX, 20));
		speedSlider.setPreferredSize(new Dimension(SLIDER_WIDTH_PX, 20));

		speedSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				parentFrame.notifyListenersSimulationSpeedChanged(speedSlider.getValue());
			}

		});
		
		// checkbox
		//
		JCheckBox checkbox = createCheckBox();

		//-----------------------------------
		// Ajoute les �l�ments
		//-----------------------------------
		
		//type de layout
		speedPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		
		//Titre du slider
		//
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1;
		speedPanel.add(vitesseLabel, gc);


		//Slider
		//
		gc.gridy = 1;
		gc.weightx = 1;
		speedPanel.add(speedSlider, gc);

		//checkbox
		//
		gc.gridy = 2;
		speedPanel.add(checkbox, gc);

		return speedPanel;

	}

	/**
	 * Cr�� le slider de temp�rature. 
	 * 
	 * Contient : 
	 * 1) titre du slider
	 * 2) Slider, avec images � gauche (flocon) et � droite (soleil).
	 * 
	 * 
	 * @return Slider de temp�rature
	 */
	private JPanel createTemperatureSliderComponent() {

		//JPanel contenant un label et le slider (et son titre)
		//
		JPanel temperatureSliderPanel = new JPanel();
		temperatureSliderPanel.setOpaque(false);

		//-------------------------
		//Cr�� les �l�ments
		//-------------------------
		
		//Titre du slider
		//
		JLabel meteoLabel = new JLabel("M�t�o");
		meteoLabel.setHorizontalAlignment(JLabel.CENTER);
		
		//slider
		//
		final JSlider temperatureSlider = new JSlider();
		temperatureSlider.setOpaque(false);
		temperatureSlider.setMaximum(100);
		temperatureSlider.setMinimum(50);
		
		
		int defaultValue = parentFrame.getConfig().getDefaultMeteo();
		if(defaultValue < temperatureSlider.getMinimum() || defaultValue > temperatureSlider.getMaximum()) {
			throw new IllegalStateException("JControlPanel::createTemperatureSliderComponent la valeur par defaut doit etre entre "+temperatureSlider.getMinimum()+" et "+temperatureSlider.getMaximum()+". "+defaultValue+"fourni.");
		}
		
		temperatureSlider.setValue(defaultValue);
		temperatureSlider.setMaximumSize(new Dimension(100000, 20));
		temperatureSlider.setPreferredSize(new Dimension(SLIDER_WIDTH_PX, 20));
		

		//listener change le facteur de meteo de la mine
		temperatureSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				parentFrame.notifyListenersMeteoChanged(1.0*temperatureSlider.getValue()/100);
			}

		});
		
		//Image de flocon
		//
		JLabel floconLabel = new JLabel(new ImageIcon(floconImage));
		
		//Image de soleil
		//
		JLabel soleilLabel = new JLabel(new ImageIcon(soleilImage));
		
		//------------------------------------
		//Ajoute les �l�ments
		//------------------------------------
				
		//gridBagLayout parce qu'on veut centrer!
		temperatureSliderPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();

		//---------------------------------------
		// Rang�e 1
		
		//titre du slider
		//
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.HORIZONTAL;
		temperatureSliderPanel.add(meteoLabel, gc);

		//---------------------------------------
		//Rang�e 2
		
		//Image de flocon
		//
		gc.gridy++;
		gc.gridx = 0;
		gc.gridheight = 2;
		gc.weightx = 0;
		temperatureSliderPanel.add(floconLabel, gc);
		
		gc.gridx=1;
		gc.gridheight = 2;
		gc.weightx = 0;
		temperatureSliderPanel.add(temperatureSlider, gc);

		gc.gridx = 2;
		gc.gridheight = 2;
		gc.weightx = 0;
		temperatureSliderPanel.add(soleilLabel, gc);

		return temperatureSliderPanel;
	}


	/**
	 * cr�� le champs texte pour le temps total de simulation
	 */
	private void createTempsSimulationTextField() {


		this.tempsSimulationTextField = new JTextField();
		tempsSimulationTextField.setText(""+SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/3600);
		this.selectedTempsSimulation = SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS/3600;

		//dimensions
		//
		tempsSimulationTextField.setMaximumSize(new Dimension(50, 50));
		tempsSimulationTextField.setMinimumSize(new Dimension(50, 20));
		tempsSimulationTextField.setPreferredSize(new Dimension(50, 20));

		//lorsque la valeur change, valide et notifie les listeners
		//
		tempsSimulationTextField.addFocusListener(new FocusListener() {


			@Override
			public void focusGained(FocusEvent arg0) {
				//ne fait rien
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
					tempsSimulationTextField.setBackground(COLOR_WRONG_INPUT);
				}
			}
		});
	}


	/**Charge les images
	 * 
	 */
	private void loadImages() {
		String theme = parentFrame.getConfig().getTheme();
		//Image de flocon
		//
		BufferedImage floconImageLarge = null;

		try {
			floconImageLarge = ImageIO.read(new File("images/"+theme+"/snowflake.png"));
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
			soleilImageLarge = ImageIO.read(new File("images/"+theme+"/sun.png"));
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

		BufferedImage smallCamionImageLarge = null;
		BufferedImage largeCamionImageLarge = null;

		try {
			smallCamionImageLarge = ImageIO.read(new File("images/"+theme+"/camion_small_fois.png"));
			largeCamionImageLarge = ImageIO.read(new File("images/"+theme+"/camion_large_fois.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		int camionX = 65;
		int camionY = 30;
		camionSmallImage = new BufferedImage(camionX, camionY, BufferedImage.TYPE_INT_ARGB);
		g2 = camionSmallImage.createGraphics();


		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(smallCamionImageLarge, 0, 0, camionX, camionY, null);
		g2.dispose();



		camionLargeImage = new BufferedImage(camionX, camionY, BufferedImage.TYPE_INT_ARGB);
		g2 = camionLargeImage.createGraphics();



		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(largeCamionImageLarge, 0, 0, camionX, camionY, null);
		g2.dispose();


		//Image d'horloge
		//

		BufferedImage horlogeImageLarge = null;

		try {
			horlogeImageLarge = ImageIO.read(new File("images/"+theme+"/clock2.png"));
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


	/**
	 * change entre le mode play et le mode pause
	 * 
	 * Mode play : Bouton affiche "pause", champ de score ne peut �tre �dit�
	 * Mode pause : Bouton affiche "play, champ de score peut �tre �dit�
	 */
	private void tooglePlayPause() {
		this.playPauseButton.toogle();

		if(playPauseButton.getState() == PlayPauseButton.STATE_PAUSE) {
			scoreFunctionTextField.setEditable(true);
		}
		else {
			scoreFunctionTextField.setEditable(false);
		}

	}

	/**
	 * Set le champs "param�tre" de la formule de pr�diction de temps de parcours, selon la formule choisie.
	 *  "Moyenne des observations pr�c�dentes" : n
	 *  "Combinaison convexe" : lambda
	 *  "Erreur pr�c�dente" : lambda
	 * 
	 */
	protected void setPredictFunctionTextFieldsAccordingToSelectedFunction() {

		@SuppressWarnings("unchecked")
		int newPredictFunctionIndex = ((Pair<Integer, String>) this.predictComboBox.getSelectedItem()).getKey();
				
		//active/desactive les champs des parametres de la fonction de prediction en fonction de la fonctin choisie
		if(newPredictFunctionIndex == TravelTimePredictor.PREDICT_FUNCTION_AVG_PREV) {
			nLabel.setVisible(true);
			nTextField.setVisible(true);
			lambdaLabel.setVisible(false);
			lambdaTextField.setVisible(false);
		}
		else if(newPredictFunctionIndex == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED) {
			nLabel.setVisible(false);
			nTextField.setVisible(false);
			lambdaLabel.setVisible(true);
			lambdaTextField.setVisible(true);
		}
		else if(newPredictFunctionIndex == TravelTimePredictor.PREDICT_FUNCTION_WEIGTED_ERROR) {
			nLabel.setVisible(false);
			nTextField.setVisible(false);
			lambdaLabel.setVisible(true);
			lambdaTextField.setVisible(true);
		}

	}

	/**
	 * V�rifie que les arguments de la simulation sont valides
	 * 
	 * @return true si les arguments sont valides, false sinon
	 */

	protected boolean validSimulationArguments() {
		if(selectedNumberOfLargeCamions >= 0 && selectedNumberOfSmallCamions >= 0  && selectedTempsSimulation > 0) {
			return true;
		}
		return false;
	}


}
