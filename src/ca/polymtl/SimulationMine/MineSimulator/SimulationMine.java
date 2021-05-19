package ca.polymtl.SimulationMine.MineSimulator;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ca.polymtl.SimulationMine.MineGui.JMineFrame;
import ca.polymtl.SimulationMine.MineGui.SommaireFrame;
import ca.polymtl.SimulationMine.MineGui.TravelTimeChart;

public class SimulationMine {

	public static double DEFAULT_SIMULATION_TIME_SECONDS = 24*3600;


	public static Random random;

	//public static Mine mine;
	public static TravelTimeChart chart; 
	//public static DecisionMaker decisionMaker;

	public static JMineFrame mineFrame;
 
	
	public static void main(String[] args) {

		System.out.println(System.getProperty("sun.arch.data.model"));
		
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		random = new Random();

		
		
		
		MineSimulator mineSimulator = new MineSimulator();

		//test du sommaire
		//SommaireFrame sf = new SommaireFrame(new MineSimulator());
		/**/
		


		

		mineFrame = new JMineFrame(mineSimulator.getMine());

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

		//----------------------------------------
		//test de stats
		//----------------------------------------

		boolean testMode = false;

		if(testMode) {
			int nbSim = 10;

			double sommeEffCamions = 0;
			double sommeEffPelleMin = 0;
			double sommeEffPelleMax = 0;
			double sommeEffPelleMoy = 0;
			double sommeNbVoyages = 0;
			for(int i = 0 ; i < nbSim; i++) {

				//roule simulation
				mineSimulator.completerSimulation();

				//calculeStats
				sommeEffCamions += mineSimulator.getMine().getAverageCamionEfficiency();
				sommeEffPelleMin += mineSimulator.getMine().getMinPelleEfficiency();
				sommeEffPelleMax += mineSimulator.getMine().getMaxPelleEfficiency();
				sommeEffPelleMoy += mineSimulator.getMine().getAveragePelleEfficiency();
				sommeNbVoyages += mineSimulator.getMine().getNumberOfRuns();

				//reset
				mineSimulator.chargeMine(mineSimulator.getMine().getCurrentExampleId(), mineSimulator.getMine().getCamions().size(), 0, mineSimulator.getTempsSimulationSeconds());
			}

			double effCamions = sommeEffCamions/nbSim;
			double effPelleMin = sommeEffPelleMin/nbSim;
			double effPelleMax = sommeEffPelleMax/nbSim;
			double effPelleMoy = sommeEffPelleMoy/nbSim;
			double nbVoyages = sommeNbVoyages/nbSim;

			System.out.println(""+effCamions+"\t"+effPelleMin+"\t"+effPelleMax+"\t"+effPelleMoy+"\t"+nbVoyages);
		}
/**/		
	}


}
