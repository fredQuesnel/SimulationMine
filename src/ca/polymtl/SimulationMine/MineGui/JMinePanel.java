package ca.polymtl.SimulationMine.MineGui;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D.Double;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Concentrateur;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulationListener;
import ca.polymtl.SimulationMine.MineSimulator.Pelle;
import ca.polymtl.SimulationMine.MineSimulator.SimulationMine;
import ca.polymtl.SimulationMine.MineSimulator.Station;
import ca.polymtl.SimulationMine.MineSimulator.Sterile;
import javafx.scene.control.ProgressBar;

public class JMinePanel extends JPanel{

	private static final int INFO_RECT_CONCENTRATEUR_WIDTH = 90;
	private static final int INFO_RECT_STERILE_HEIGHT = 35;
	private static int STATION_WIDTH = 50;
	private static int STATION_HEIGHT = 50;

	private static int INFO_RECT_WIDTH = 70;
	private static int INFO_RECT_HEIGHT = 70;




	private static int CAMION_WIDTH = 30;
	private static int CAMION_HEIGHT = 20;

	private BufferedImage pelleImage;

	private BufferedImage sterileImage;
	private BufferedImage concentrateurImage;

	private BufferedImage backgroundImage;

	private JMineFrame parentFrame;

	private JPanel progressBarPanel;
	private JProgressBar progressBar;
	private Rectangle bounds;


	//constructeur
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

		GridBagConstraints gc = new GridBagConstraints();

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

		//image du camion
		//
		BufferedImage camionImage = null;
		try {
			camionImage = ImageIO.read(new File("images/camion.png"));
			pelleImage = ImageIO.read(new File("images/pelle.png"));

			sterileImage = ImageIO.read(new File("images/sterile.png"));
			concentrateurImage = ImageIO.read(new File("images/concentrateur.png"));

			backgroundImage = ImageIO.read(new File("images/background.png"));
		} catch (IOException e) {

		}





		this.repaint();

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
					System.out.println("clic");
					String strNbCamionsParHeure = JOptionPane.showInputDialog(closestPelle.getId()+" : Nombre de camions/heure?");

					//Utilisation de Float car le package Double est défini pour des coordonneés 2D
					double nbCamionsParHeure = Float.parseFloat(strNbCamionsParHeure);

					parentFrame.notifyListenersPlanPelleChanged(closestPelle, nbCamionsParHeure);

					repaint();
				}

			});
			//affiche le menu
			ppm.show(e.getComponent(), e.getX(), e.getY());
		}

	}



	//peint la mine
	public void paintComponent(Graphics g) {


		Mine mine = parentFrame.getMine();

		if(mine != null) {
			System.out.println("je desine une mine");
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

			paintStatsPanel(g, mine);

			g.setFont(normalFont);
			//peint les pelles
			//
			ArrayList<Pelle> pelles = mine.getPelles();
			for(int i = 0 ; i < pelles.size(); i++) {
				paintPelle(pelles.get(i), g);
			}
		}

	}

	//peint une station
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
			xrect = this.getWidth()-this.INFO_RECT_CONCENTRATEUR_WIDTH;
		}
		if(xrect < 0){
			xrect = 0;
		}
		int yrect = (int) (point.getY()-STATION_HEIGHT);
		if(yrect+JMinePanel.INFO_RECT_HEIGHT >= this.getHeight()){
			yrect = this.getHeight()-this.INFO_RECT_HEIGHT;
		}
		if(yrect < 0){
			yrect = 0;
		}

		int paddingx = 2;

		g.fillRect(xrect, yrect, this.INFO_RECT_CONCENTRATEUR_WIDTH, this.INFO_RECT_HEIGHT);

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

	//peint les chemins reliant le concentrateur  et le stérile aux pelles
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

	private void paintStatsPanel(Graphics g, Mine mine) {
		int width = 430;
		int height = 180;
		g.setColor(Color.black);

		//format des nombres
		DecimalFormat df = new DecimalFormat("0.00");

		double minCamionEff = mine.getMinCamionEfficiency();
		String minCamionEffStr = df.format(minCamionEff);
		double maxCamionEff = mine.getMaxCamionEfficiency();
		String maxCamionEffStr = df.format(maxCamionEff);
		double avgCamionEff = mine.getAverageCamionEfficiency();
		String avgCamionEffStr = df.format(avgCamionEff);

		double minPelleEff = mine.getMinPelleEfficiency();
		String minPelleEffStr = df.format(minPelleEff);
		double maxPelleEff = mine.getMaxPelleEfficiency();
		String maxPelleEffStr = df.format(maxPelleEff);
		double avgPelleEff = mine.getAveragePelleEfficiency();
		String avgPelleEffStr = df.format(avgPelleEff);

		double time = mine.getTime()/3600;
		String timeStr = df.format(time);

		g.setColor(new Color(255, 255, 255, 170));
		g.fillRect(this.getWidth()-width-1, 0, width, height);
		g.setColor(Color.black);
		g.drawRect(this.getWidth()-width-1, 0, width, height);

		Font bigFont = g.getFont().deriveFont((float) 20.0);
		Font smallFont = g.getFont().deriveFont((float) 17.0);

		g.setFont(bigFont);
		g.drawString("Efficacité moyenne des camions : ", this.getWidth()-width+20, 30);

		int alignXMinMaxAvg = this.getWidth()-width+290;
		//g.setFont(smallFont);
		//g.drawString("Min.  : "+minCamionEffStr,alignXMinMaxAvg , 30);
		//g.drawString("Max. : "+maxCamionEffStr ,alignXMinMaxAvg , 50);
		g.drawString(avgCamionEffStr+" %" ,alignXMinMaxAvg+65 , 30);

		g.drawString("Efficacité des pelles : ", this.getWidth()-width+20, 60);

		//g.setFont(smallFont);
		g.drawString("Min.  : "+minPelleEffStr+" %",alignXMinMaxAvg , 60);
		g.drawString("Max. : "+maxPelleEffStr+" %",alignXMinMaxAvg , 80);
		g.drawString("Moy. : "+avgPelleEffStr+" %",alignXMinMaxAvg , 100);

		g.drawString("Nombre de voyages : "+mine.getNumberOfRuns(), this.getWidth()-width+20, 130);
		System.out.println(timeStr);
		g.drawString("Temps écoulé : "+timeStr+" h", this.getWidth()-width+20, 160);


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

	//peint une station
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
			xrect = this.getWidth()-this.INFO_RECT_WIDTH;
		}
		if(xrect < 0){
			xrect = 0;
		}
		int yrect = (int) (point.getY()-STATION_HEIGHT);
		if(yrect+JMinePanel.INFO_RECT_STERILE_HEIGHT >= this.getHeight()){
			yrect = this.getHeight()-this.INFO_RECT_STERILE_HEIGHT;
		}
		if(yrect < 0){
			yrect = 0;
		}

		int paddingx = 2;

		g.fillRect(xrect, yrect, this.INFO_RECT_WIDTH, this.INFO_RECT_STERILE_HEIGHT);

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

	//peint une pelle
	private void paintPelle(Pelle pelle, Graphics g) {
		//format des nombres
		DecimalFormat df = new DecimalFormat("0.0");

		Point point = convertPointToWindow(pelle.getLocation());
		//g.drawOval((int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), STATION_WIDTH, STATION_HEIGHT);
		g.drawImage(pelleImage, (int) (point.getX()-STATION_WIDTH/2), (int) (point.getY()-STATION_HEIGHT/2), (int) (point.getX()+STATION_WIDTH/2), (int) (point.getY()+STATION_HEIGHT/2), 0, 0, pelleImage.getWidth(), pelleImage.getHeight(), null);

		Font previousFont = g.getFont();
		Font boldFont = g.getFont().deriveFont(Font.BOLD); 
		//new Font ("Sanserif", Font.BOLD, 10);
		Font normalFont = previousFont;

		//rectangle d'information
		//coordonnees rect
		g.setColor(new Color(255, 255, 255, 170));
		int xrect =(int) point.getX()+STATION_WIDTH/2;
		if(xrect+JMinePanel.INFO_RECT_WIDTH > this.getWidth()){
			xrect = this.getWidth()-this.INFO_RECT_WIDTH;
		}
		if(xrect < 0){
			xrect = 0;
		}
		int yrect = (int) (point.getY()-STATION_HEIGHT);
		if(yrect+JMinePanel.INFO_RECT_HEIGHT >= this.getHeight()){
			yrect = this.getHeight()-this.INFO_RECT_HEIGHT;
		}
		if(yrect < 0){
			yrect = 0;
		}

		int paddingx = 2;

		g.fillRect(xrect, yrect, this.INFO_RECT_WIDTH, this.INFO_RECT_HEIGHT);

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
		g.setColor(new Color(150, 0, 0));
		g.drawString("Fe: "+df.format(percentMinerai)+"%",  xrect+paddingx, yrect +38 );
		g.setColor(new Color(255, 100, 0));
		g.drawString("S  : "+df.format(percentSouffre)+"%",  xrect+paddingx, yrect +51 );

		//plan
		//
		double nbCamionsParHeure = pelle.getPlanNbCamionsParHeure();
		//g.drawString("Plan : ", xrect+paddingx, yrect +50);
		g.setColor(new Color(0, 100, 0));
		g.drawString(df.format(nbCamionsParHeure)+" cam./h", xrect+paddingx, yrect +64);

		//remet le font d'avant
		g.setFont(previousFont);
	}

	//peint un camion
	private void paintCamion(Camion camion, Graphics g) {
		Point point = convertPointToWindow(camion.getLocation());
		g.setColor(Color.RED);

		BufferedImage camionImage = camion.getGoingWestImage();
		if(camion.isGoingEast()) {
			camionImage = camion.getGoingEastImage();
		}
		g.drawImage(camionImage, (int) (point.getX()-CAMION_WIDTH/2), (int) (point.getY()-CAMION_HEIGHT/2), (int) (point.getX()+CAMION_WIDTH/2), (int) (point.getY()+CAMION_HEIGHT/2), 0, 0, camionImage.getWidth(), camionImage.getHeight(), null);


	}

	//convertis un point dans l'espace de la mine en point dans l'espace de la fenetre
	private Point convertPointToWindow(Double double1) {
		double ratioX = this.getWidth()/Mine.WIDTH;
		double ratioY = this.getHeight()/Mine.HEIGHT;

		Point newPoint = new Point((int) (double1.getX()*ratioX), (int) (double1.getY()*ratioY));
		return newPoint;
	}


	public void mineResetted() {
		this.repaint();		
	}



	public void updateMine() {
		System.out.println("update");
		revalidate();
		repaint();	
	}




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


	public void automaticCompleteUpdated(double fractionComplete) {

		progressBar.setValue((int) fractionComplete);
		//progressBarPanel.paintImmediately(bounds);

		paintImmediately(bounds);

	}


	public void automaticCompleteFinished() {
		progressBarPanel.setVisible(false);
		repaint();

	}
}
