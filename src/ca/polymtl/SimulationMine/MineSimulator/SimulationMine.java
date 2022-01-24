package ca.polymtl.SimulationMine.MineSimulator;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ca.polymtl.SimulationMine.Config;
import ca.polymtl.SimulationMine.MineGui.JMineFrame;
import ca.polymtl.SimulationMine.MineGui.TravelTimeChart;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 * Classe principale
 * @author Fred
 *
 */
public class SimulationMine {
 
	/**Duree de simulation par defaut*/
	public static double DEFAULT_SIMULATION_TIME_SECONDS;


	/**Classe pourles nombres aleatoires*/
	public static Random random;

	/**Graphique des temps de parcours*/
	public static TravelTimeChart chart; 

	/**Frame d'affichage pour la mine*/
	public static JMineFrame mineFrame;

 
 
	/**
	 * 
	 * Fonction principale
	 * @param args aucun argument
	 */
	public static void main(String[] args) {

		
		try {
			LpSolve.makeLp(10, 10);
		} catch (LpSolveException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("machine "+System.getProperty("sun.arch.data.model")+"bit");
		
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		Config config = new Config();
		
		SimulationMine.DEFAULT_SIMULATION_TIME_SECONDS = config.getDefaultSimulationTimeSeconds();
		random = new Random();

		random.setSeed(1962996941790320022L);
		
		System.out.println("charge le simulateur");
		
		MineSimulator mineSimulator = new MineSimulator(config);

		//test du sommaire
		//SommaireFrame sf = new SommaireFrame(new MineSimulator());
		/**/
		


		System.out.println("charge l'interface graphique");

		mineFrame = new JMineFrame(mineSimulator, config);

		mineFrame.setSize(new Dimension(1100, 800));
		mineFrame.setVisible(true);

		ArrayList<String> dataSeriesHandles = mineSimulator.getMine().getDataSeriesHandles();

		mineFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		mineFrame.addObserver(mineSimulator);

		mineSimulator.addListener(mineFrame);
		//test chart
		chart = new TravelTimeChart(dataSeriesHandles, mineSimulator.getTempsSimulationSeconds());
		chart.setVisible(true);
		chart.setState(JFrame.ICONIFIED);
		chart.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		mineSimulator.addListener(chart);

		
/**/		
	}


}
