package ca.polymtl.SimulationMine.MineGui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;

import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulator;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.Station;
import ca.polymtl.SimulationMine.MineSimulator.Sterile;

/**Panneau dans lequel s'affiche la mine
 * 
 * @author Fred
 *
 */
public class JMinePanel extends JPanel{

	/*
	 * Constantes
	 */
	private static final long serialVersionUID = 1L;
	
	/**Dimension reliee a l'affichage des infos du concentrateur dans le rectangle d'information (px)*/
	private static final int INFO_RECT_CONCENTRATEUR_WIDTH = 90;
	/**Dimension reliee a l'affichage des infos du sterile dans le rectangle d'information (px)*/
	private static final int INFO_RECT_STERILE_HEIGHT = 35;
	/**Couleur pour l'affichage des quantités de fer*/
	private static final Color COLOR_IRON = new Color(150, 0, 0);
	/**Couleur pour l'affichage des quantités de soufre*/
	private static final Color COLOR_SULFUR = new Color(255, 100, 0);
	
	/**Largeur d'une station (px)*/
	private static int STATION_WIDTH = 50;
	/**Hauteur d'une station (px)*/
	private static int STATION_HEIGHT = 50;

	/**Largeur relie au rectangle d'information*/
	private static int INFO_RECT_WIDTH = 70;
	/**Hauteur relie au rectangle d'information*/
	private static int INFO_RECT_HEIGHT = 70;



	/**Largeur d'un camion (px)*/
	private static int CAMION_WIDTH = 30;
	/**Hauteur d'un camion (px)*/
	private static int CAMION_HEIGHT = 20;

	/**Image de pelle*/
	private BufferedImage pelleImage;
	/**Image de pelle en panne*/
	private BufferedImage pellePanneImage;

	/**Image de sterile*/
	private BufferedImage sterileImage;
	/**Image de concentrateur*/
	private BufferedImage concentrateurImage;

	/**Image de background*/
	private BufferedImage backgroundImage;

	/**Frame parent*/
	private JMineFrame parentFrame;

	/**Panel de progrès (en mode completion auto)*/
	private JPanel progressBarPanel;
	/**Barre de progrès de progrès (en mode completion auto)*/
	private JProgressBar progressBar;
	/**Rectangle dans lequel se trouve le progressBarPanel (pour ne pas avoir a updater l'affichage au complet en mode completion auto).*/
	private Rectangle bounds;


	/**
	 * Constructeur
	 * @param frame frame
	 */
	public JMinePanel(JMineFrame frame) {

		this.setLayout(null);

		//ajoute le mouselistener du panel
		//
		this.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				//si clique gauche
				if(e.getButton()== MouseEvent.BUTTON1){
					double fracX = 0;
					double fracY = 0;
					if( getWidth() > 0) {
						fracX = 1.*e.getX()/getWidth();
					}
					if( getHeight() > 0) {
						fracY = 1.*e.getY()/getHeight();
					}
					System.out.println("clique sur le minePanel!");
					parentFrame.notifyListenersMinePanelClicked(fracX, fracY);
				}
				else if(e.getButton()== MouseEvent.BUTTON3){
					//si clique droit menu pour changer le plan
					openPopupMenu(e);
				}

			}
			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {

			}

			@Override
			public void mouseReleased(MouseEvent arg0) {

			}

		});

		//cree la barre de progres
		//
		Dimension panelSize = this.getSize();
		int width = 300;
		int height = 50;
		int startx = (int) ( panelSize.getWidth()/2-width/2);
		int starty = (int) (panelSize.getHeight()/2-height/2);
		bounds = new Rectangle(startx, starty, width, height);
		progressBar = new JProgressBar(0,100);
		progressBar.setSize(width, 20);

		//progressBar.setString("Completed %d%% of task.");
		progressBar.setStringPainted(true);
		progressBarPanel = new JPanel();
		progressBarPanel.setLayout(new GridBagLayout());

		progressBarPanel.setOpaque(true);
		progressBarPanel.setBackground(JControlPanel.DARK_BLUE);
		progressBarPanel.setBounds(bounds);
		setComponentZOrder(progressBarPanel, 0);

		progressBarPanel.add(progressBar);
		progressBarPanel.setMinimumSize(new Dimension(300, 50));
		progressBarPanel.setMaximumSize(new Dimension(300, 50));
		progressBarPanel.setPreferredSize(new Dimension(300, 50));
		progressBarPanel.setVisible(false);
		this.add(progressBarPanel);
		revalidate();
		//this.add(progressBarPanel, gc);


		this.setBackground(Color.white);
		this.parentFrame = frame;
		
		String themeFolder = parentFrame.getConfig().getTheme();

		//images des pelles et stations
		//
		try {
			//System.out.println("images/"+themeFolder+"/pelle.png");
			pelleImage = ImageIO.read(new File("images/"+themeFolder+"/pelle.png"));
			pellePanneImage = ImageIO.read(new File("images/"+themeFolder+"/pelle_panne.png"));

			sterileImage = ImageIO.read(new File("images/"+themeFolder+"/sterile.png"));
			concentrateurImage = ImageIO.read(new File("images/"+themeFolder+"/concentrateur.png"));

			backgroundImage = ImageIO.read(new File("images/"+themeFolder+"/background.png"));
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage());
			
		}





		this.repaint();

	}


	/**Quand la completion automatique est terminee, affiche la mine
	 * 
	 */
	public void automaticCompleteFinished() {
		progressBarPanel.setVisible(false);
		repaint();

	}



	/**Quand on commence la completion automatique, affiche le panel de progression
	 * 
	 */
	public void automaticCompleteStarted() {
		Dimension panelSize = this.getSize();
		int width = 300;
		int height = 50;
		int startx = (int) ( panelSize.getWidth()/2-width/2);
		int starty = (int) (panelSize.getHeight()/2-height/2);
		bounds = new Rectangle(startx, starty, width, height);
		progressBarPanel.setBounds(bounds);

		progressBarPanel.setVisible(true);
		//repaint();
		//progressBarPanel.paintImmediately(bounds);
		paintImmediately(bounds);


		//progressBarPanel.paintImmediately(0, 0, 10000, 10000);
		//test.validate();
		//progressBarPanel.repaint(1);		
	}

	/**Update la barre de progrès en mode completion automatique
	 * 
	 * @param fractionComplete Fraction de la simulation completee (en %)
	 */
	public void automaticCompleteUpdated(double fractionComplete) {

		progressBar.setValue((int) fractionComplete);
		//progressBarPanel.paintImmediately(bounds);

		paintImmediately(bounds);

	}

	/**Repeint la mine lorsque reset
	 * 
	 */
	public void mineResetted() {
		this.repaint();		
	}

	/**
	 * Peint la mine
	 */
	public void paintComponent(Graphics g) {

		MineSimulator mineSimulator = parentFrame.getMineSimulator();
		Mine mine = parentFrame.getMine();

		if(mine != null) {
			super.paintComponent(g);

			//background
			g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), null);

			//peint les routes
			//
			paintRoutes(g, mine);

			//peint les concentrateurs
			for(int i = 0 ; i < mine.getConcentrateurs().size(); i++) {
				if(mine.getConcentrateurs().get(i)!= null) {

					paintConcentrateur(mine.getConcentrateurs().get(i), g);
				}
			}

			//peint les steriles
			for(int i = 0 ; i < mine.getSteriles().size(); i++) {
				if(mine.getSteriles().get(i)!= null) {
					paintSterile(mine.getSteriles().get(i), g);
				}
			}


			//peint les camions qui sont en route
			//
			ArrayList<Camion> camions = mine.getCamions();
			for(int i = 0 ; i < camions.size(); i++) {
				Camion camion = camions.get(i);
				if(camion.getState() == Camion.ETAT_EN_ROUTE) {
					paintCamion(camion, g);
				}
			}

			Font normalFont = g.getFont();

			paintStatsPanel(g, mineSimulator);

			g.setFont(normalFont);
			//peint les pelles
			//
			ArrayList<Pelle> pelles = mine.getPelles();
			for(int i = 0 ; i < pelles.size(); i++) {
				paintPelle(pelles.get(i), g);
			}
		}

	}

	/**Repeint la mine lorsque celle-ci est updatee
	 * 
	 */
	public void updateMine() {
		revalidate();
		repaint();	
	}

	/**convertis un point dans l'espace de la mine en point dans l'espace de la fenetre
	 * 
	 * @param double1 Point de l'espace
	 * @return
	 */
	private Point convertPointToWindow(Double double1) {
		double ratioX = this.getWidth()/Mine.WIDTH;
		double ratioY = this.getHeight()/Mine.HEIGHT;

		Point newPoint = new Point((int) (double1.getX()*ratioX), (int) (double1.getY()*ratioY));
		return newPoint;
	}

	/**
	 * peint un camion
	 * @param camion camion
	 * @param g objet graphique
	 */
	private void paintCamion(Camion camion, Graphics g) {
		Point point = convertPointToWindow(camion.getLocation());
		g.setColor(Color.RED);

		BufferedImage camionImage = camion.getGoingWestImage();
		if(camion.isGoingEast()) {
			camionImage = camion.getGoingEastImage();
		}
		g.drawImage(camionImage, (int) (point.getX()-CAMION_WIDTH/2), (int) (point.getY()-CAMION_HEIGHT/2), (int) (point.getX()+CAMION_WIDTH/2), (int) (point.getY()+CAMION_HEIGHT/2), 0, 0, camionImage.getWidth(), camionImage.getHeight(), null);


	}

	/**peint une station
	 * 
	 * @param concentrateur concentrateur
	 * @param g objet graphique
	 */
	private void paintConcentrateur(Concentrateur concentrateur, Graphics g) {

		//format des nombres
		DecimalFormat df = new DecimalFormat("0.0");
		g.setColor(Color.red);

		Font previousFont = g.getFont();
		Font boldFont = g.getFont().deriveFont(Font.BOLD); 
		//new Font ("Sanserif", Font.BOLD, 10);
		Font normalFont = previousFont;

		Point point = convertPointToWindow(concentrateur.getLocation());
		//g.fillOval((int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), STATION_WIDTH, STATION_HEIGHT);
		g.drawImage(concentrateurImage, (int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), (int) (point.getX()+STATION_WIDTH/2), (int) (point.getY()+STATION_HEIGHT/2), 0, 0, concentrateurImage.getWidth(), concentrateurImage.getHeight(), null);

		g.setColor(Color.BLACK);



		//rectangle d'information
		//coordonnees rect
		g.setColor(new Color(255, 255, 255, 170));
		int xrect =(int) point.getX()+STATION_WIDTH/2;
		if(xrect+JMinePanel.INFO_RECT_CONCENTRATEUR_WIDTH > this.getWidth()){
			xrect = this.getWidth()-JMinePanel.INFO_RECT_CONCENTRATEUR_WIDTH;
		}
		if(xrect < 0){
			xrect = 0;
		}
		int yrect = (int) (point.getY()-STATION_HEIGHT);
		if(yrect+JMinePanel.INFO_RECT_HEIGHT >= this.getHeight()){
			yrect = this.getHeight()-JMinePanel.INFO_RECT_HEIGHT;
		}
		if(yrect < 0){
			yrect = 0;
		}

		int paddingx = 2;

		g.fillRect(xrect, yrect, JMinePanel.INFO_RECT_CONCENTRATEUR_WIDTH, JMinePanel.INFO_RECT_HEIGHT);

		//ID de la pelle
		g.setColor(Color.black);
		g.setFont (boldFont);
		g.drawString(concentrateur.getId(), xrect+paddingx, yrect +12);

		g.setFont(normalFont);

		//camions en attente
		//
		int nbCamions = concentrateur.getCamionsEnAttente().size();

		if(concentrateur.getCamionEnTraitement() != null) {
			nbCamions++;
		}
		g.setColor(new Color(0, 0, 170));
		g.drawString("attente :  "+nbCamions, xrect+paddingx, yrect +25 );


		//Qualité du minerai
		//
		double percentMinerai = concentrateur.getPercentIron();
		double percentSouffre = concentrateur.getPercentSulfur();
		if(concentrateur.getTotalQuantity()<0.0001) {
			percentMinerai=0;
			percentSouffre=0;
		}

		g.setColor(new Color(150, 0, 0));
		g.drawString("Fe: "+df.format(percentMinerai)+"%",  xrect+paddingx, yrect +38 );
		g.setColor(new Color(255, 100, 0));
		g.drawString("S  : "+df.format(percentSouffre)+"%",  xrect+paddingx, yrect +51 );

		//remet le font d'avant
		g.setFont(previousFont);

	}


	/**peint une pelle
	 * 
	 * @param pelle pelle
	 * @param g objet graphique
	 */
	private void paintPelle(Pelle pelle, Graphics g) {
		//format des nombres
		DecimalFormat df = new DecimalFormat("0.0");

		Point point = convertPointToWindow(pelle.getLocation());
		//g.drawOval((int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), STATION_WIDTH, STATION_HEIGHT);
		BufferedImage imageToDraw = pelleImage;
		if(pelle.getState() == Station.STATION_STATE_PANNE) {
			imageToDraw = this.pellePanneImage;
		}

		g.drawImage(imageToDraw, (int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), (int) (point.getX()+STATION_WIDTH/2), (int) (point.getY()+STATION_HEIGHT/2), 0, 0, pelleImage.getWidth(), pelleImage.getHeight(), null);

		Font previousFont = g.getFont();
		Font boldFont = g.getFont().deriveFont(Font.BOLD); 
		//new Font ("Sanserif", Font.BOLD, 10);
		Font normalFont = previousFont;

		//rectangle d'information
		//coordonnees rect
		g.setColor(new Color(255, 255, 255, 170));
		int xrect =(int) point.getX()+STATION_WIDTH/2;
		if(xrect+JMinePanel.INFO_RECT_WIDTH > this.getWidth()){
			xrect = this.getWidth()-JMinePanel.INFO_RECT_WIDTH;
		}
		if(xrect < 0){
			xrect = 0;
		}
		int yrect = (int) (point.getY()-STATION_HEIGHT);
		if(yrect+JMinePanel.INFO_RECT_HEIGHT >= this.getHeight()){
			yrect = this.getHeight()-JMinePanel.INFO_RECT_HEIGHT;
		}
		if(yrect < 0){
			yrect = 0;
		}

		int paddingx = 2;

		g.fillRect(xrect, yrect, JMinePanel.INFO_RECT_WIDTH, JMinePanel.INFO_RECT_HEIGHT);

		//ID de la pelle
		g.setColor(Color.black);
		g.setFont (boldFont);
		g.drawString(pelle.getId(), xrect+paddingx, yrect +12);

		g.setFont(normalFont);

		//camions en attente
		//
		int nbCamions = pelle.getCamionsEnAttente().size();

		if(pelle.getCamionEnTraitement() != null) {
			nbCamions++;
		}
		g.setColor(new Color(0, 0, 170));
		g.drawString("attente :  "+nbCamions, xrect+paddingx, yrect +25 );


		//Qualité du minerai
		//
		double percentMinerai = pelle.getRockType().getPercentIron();
		double percentSouffre = pelle.getRockType().getPercentSulfur();
		g.setColor(COLOR_IRON);
		g.drawString("Fe: "+df.format(percentMinerai)+"%",  xrect+paddingx, yrect +38 );
		g.setColor(COLOR_SULFUR);
		g.drawString("S  : "+df.format(percentSouffre)+"%",  xrect+paddingx, yrect +51 );

		//plan
		//
		double nbCamionsParHeure = pelle.getPlanNbTonnesParHeure();
		//g.drawString("Plan : ", xrect+paddingx, yrect +50);
		g.setColor(new Color(0, 100, 0));
		g.drawString(df.format(nbCamionsParHeure)+" t/h", xrect+paddingx, yrect +64);

		//remet le font d'avant
		g.setFont(previousFont);
	}



	/**peint les chemins reliant le concentrateur  et le stérile aux pelles
	 * 
	 * @param g objet graphique
	 * @param mine Mine
	 */
	private void paintRoutes(Graphics g, Mine mine) {
		//Station concentrateur = mine.getConcentrateur();

		//Station sterile = mine.getSterile();

		ArrayList<Pelle> pelles = mine.getPelles();

		for(int i = 0 ; i < pelles.size(); i++) {
			Pelle p = pelles.get(i);
			Point pointPelle = convertPointToWindow(p.getLocation());

			//lien avec concentrateur
			//
			for(int j = 0 ; j < mine.getConcentrateurs().size(); j++) {
				Concentrateur concentrateur = mine.getConcentrateurs().get(j);
				if(concentrateur != null) {
					Point pointConcentrateur = convertPointToWindow(concentrateur.getLocation());
					g.setColor(Color.green);
					g.drawLine((int) pointConcentrateur.getX(), (int) pointConcentrateur.getY(), (int) pointPelle.getX(), (int) pointPelle.getY());
				}
			}
			//lien avec steriles
			//
			for(int j = 0 ; j < mine.getSteriles().size(); j++) {
				Sterile sterile = mine.getSteriles().get(j);
				if(sterile!= null) {
					Point pointSterile = convertPointToWindow(sterile.getLocation());
					g.setColor(Color.green);
					g.drawLine((int) pointSterile.getX(), (int) pointSterile.getY(), (int) pointPelle.getX(), (int) pointPelle.getY());
				}
			}
		}

	}



	/**
	 * Peint le rectangle de statistiques
	 * @param g objet graphique
	 * @param mineSimulator Simulateur
	 */
	private void paintStatsPanel(Graphics g, MineSimulator mineSimulator) {
		int width = 330;
		int height = 120;
		g.setColor(Color.black);

		Mine mine = mineSimulator.getMine();
		//format des nombres

		int nbDays = mine.getDayNumber();

		double time = mine.getTime()/3600 - nbDays*24;

		int nbHours = (int) time;
		int nbMin = (int) ((time-nbHours)*60);
		String timeStr = "Jour "+(nbDays+1)+"   ";
		if(nbMin == 0) {
			timeStr += nbHours+":00";
		}
		else if (nbMin <10) {
			timeStr += nbHours+":0"+nbMin;	
		}
		else {
			timeStr += nbHours+":"+nbMin;	
		}

		g.setColor(new Color(255, 255, 255, 170));
		g.fillRect(this.getWidth()-width-1, 0, width, height);
		g.setColor(Color.black);
		g.drawRect(this.getWidth()-width-1, 0, width, height);

		Font bigFont = g.getFont().deriveFont((float) 20.0);

		g.setFont(bigFont);

		//g.drawString("Temps ecoule : ", this.getWidth()-width+20, 30);
		g.drawString(timeStr, this.getWidth()-width+20, 30);

		//Quantite concentrateur
		//
		double totalConcentrateur = 0;
		double percentSulfur = 0;
		double percentIron = 0;
		for(Concentrateur c : mine.getConcentrateurs()) {
			totalConcentrateur += c.getTotalQuantity();
			percentSulfur += c.getQuantitySulfur();
			percentIron+=c.getQuantityIron();
		}
		if(totalConcentrateur == 0) {
			percentIron = 0;
			percentSulfur = 0;
		}
		else {
			percentIron = percentIron/totalConcentrateur*100;
			percentSulfur = percentSulfur/totalConcentrateur*100;
		}


		String strTotalConcentrateur = String.format("%.0f\n", totalConcentrateur);
		String strPercentSulfur = String.format("%.1f\n", percentSulfur);
		String strPercentIron = String.format("%.1f\n", percentIron);
		
		
		g.drawString("Concentrateurs : ", this.getWidth()-width+20, 55);
		g.drawString(strTotalConcentrateur+" tonnes", this.getWidth()-width+180, 55);
		g.setColor(COLOR_IRON);
		g.drawString("Fe :"+strPercentIron+"%", this.getWidth()-width+60, 80);
		g.setColor(COLOR_SULFUR);
		g.drawString("S :"+strPercentSulfur+"%", this.getWidth()-width+160, 80);
		
		//Stériles
		//
		double totalSterile = 0;
		for(Sterile s : mine.getSteriles()) {
			totalSterile+=s.getTotalQuantity();
		}
		String strTotalSterile = String.format("%.0f\n", totalConcentrateur);
		
		g.setColor(Color.black);
		g.drawString("Steriles : "+strTotalSterile+" tonnes", this.getWidth()-width+20, 105);
		
		//DEBUG
		// temps d'attente de tous les camions
		/*
		int verticalHeight = 190;
		ArrayList<Camion> camions = mine.getCamions();
		for(int i = 0 ; i < camions.size(); i++) {
			double attente = camions.get(i).getWaitTime();
			g.drawString("Camion "+i+" : "+attente, this.getWidth()-width+20, verticalHeight);
			verticalHeight += 20;

		}
		 */


	}


	/**peint un sterile
	 * 
	 * @param sterile sterile
	 * @param g objet graphique
	 */
	private void paintSterile(Sterile sterile, Graphics g) {
		g.setColor(Color.BLACK);	

		Font previousFont = g.getFont();
		Font boldFont = g.getFont().deriveFont(Font.BOLD); 
		//new Font ("Sanserif", Font.BOLD, 10);
		Font normalFont = previousFont;


		Point point = convertPointToWindow(sterile.getLocation());
		//g.fillOval((int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), STATION_WIDTH, STATION_HEIGHT);
		g.drawImage(sterileImage, (int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), (int) (point.getX()+STATION_WIDTH/2), (int) (point.getY()+STATION_HEIGHT/2), 0, 0, sterileImage.getWidth(), sterileImage.getHeight(), null);


		//rectangle d'information
		//coordonnees rect
		g.setColor(new Color(255, 255, 255, 170));
		int xrect =(int) point.getX()+STATION_WIDTH/2;
		if(xrect+JMinePanel.INFO_RECT_WIDTH > this.getWidth()){
			xrect = this.getWidth()-JMinePanel.INFO_RECT_WIDTH;
		}
		if(xrect < 0){
			xrect = 0;
		}
		int yrect = (int) (point.getY()-STATION_HEIGHT);
		if(yrect+JMinePanel.INFO_RECT_STERILE_HEIGHT >= this.getHeight()){
			yrect = this.getHeight()-JMinePanel.INFO_RECT_STERILE_HEIGHT;
		}
		if(yrect < 0){
			yrect = 0;
		}

		int paddingx = 2;

		g.fillRect(xrect, yrect, JMinePanel.INFO_RECT_WIDTH, JMinePanel.INFO_RECT_STERILE_HEIGHT);

		//ID de la pelle
		g.setColor(Color.black);
		g.setFont (boldFont);
		g.drawString(sterile.getId(), xrect+paddingx, yrect +12);

		g.setFont(normalFont);

		//camions en attente
		//
		int nbCamions = sterile.getCamionsEnAttente().size();

		if(sterile.getCamionEnTraitement() != null) {
			nbCamions++;
		}
		g.setColor(new Color(0, 0, 170));
		g.drawString("attente :  "+nbCamions, xrect+paddingx, yrect +25 );

		//remet le font d'avant
		g.setFont(previousFont);
	}


	protected void openPopupMenu(MouseEvent e) {
		//trouve la pelle la plus pres du bouton
		//
		Mine mine = parentFrame.getMine();
		//emplacement relatif de la souris, en fraction
		double fractionX = 1.*e.getX()/this.getWidth();
		double fractionY = 1.*e.getY()/this.getHeight();

		final Pelle closestPelle = mine.closestPelle(fractionX, fractionY);

		//emplacement de la pelle dans le GUI
		Point pointPelle = convertPointToWindow(closestPelle.getLocation());

		//menu seulement si assez pres de la pelle
		//
		if(e.getX() <= pointPelle.getX()+STATION_WIDTH/2 && e.getX() >= pointPelle.getX()-STATION_WIDTH/2 && 
				e.getY() <= pointPelle.getY()+STATION_HEIGHT/2 && e.getY() >= pointPelle.getY()-STATION_HEIGHT/2){

			//cree le menu
			JPopupMenu ppm = new JPopupMenu();
			JMenuItem menuItem = new JMenuItem("Modifier le plan."); 
			ppm.add(menuItem);

			menuItem.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent arg0) {
					String strNbCamionsParHeure = JOptionPane.showInputDialog(closestPelle.getId()+" : Nombre de tonnes/heure?");

					//Utilisation de Float car le package Double est defini pour des coordonnees 2D
					double nbCamionsParHeure = Float.parseFloat(strNbCamionsParHeure);

					parentFrame.notifyListenersPlanPelleChanged(closestPelle, nbCamionsParHeure);

					repaint();
				}

			});
			//affiche le menu
			ppm.show(e.getComponent(), e.getX(), e.getY());
		}

	}
}
