package ca.polymtl.SimulationMine.MineGui;
import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ca.polymtl.SimulationMine.MineSimulator.Camion;
import ca.polymtl.SimulationMine.MineSimulator.Mine;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulationListener;
import ca.polymtl.SimulationMine.MineSimulator.MineSimulator;
import ca.polymtl.SimulationMine.decisionMaker.TravelTimePredictor;

public class TravelTimeChart extends JFrame implements MineSimulationListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<String, XYSeries> seriesMap;
	private XYSeriesCollection dataset;
	private ChartPanel chartPanel;


	public TravelTimeChart(ArrayList<String> dataSeriesHandles, double tempsSimulationSeconds) {
		setChart(dataSeriesHandles,tempsSimulationSeconds);

	}

	public void addDataPoint(String serieKey, double x, double y) {

		if(seriesMap.containsKey(serieKey)) {

			XYSeries serie = seriesMap.get(serieKey);
			serie.add(x/3600, y/60);
		}
	}

	@Override
	public void automaticCompleteFinished() {
	}

	@Override
	public void automaticCompleteStarted() {
	}

	@Override
	public void automaticCompleteUpdated(double fractionComplete) {
	}

	@Override
	public void camionJustArrived(Camion camion, double time) {
		if(camion.getState() != Camion.ETAT_JUSTE_ARRIVE) {
			throw new IllegalStateException("le camion doit etre dans l'état Camion.STATE_JUST_ARRIVED");
		}

		//clé d'origine/destination
		String ODKey = TravelTimePredictor.getMapKeyForODPair(camion.getOrigine(), camion.getDestination());

		if(camion.getPredictedTravelTime() >0) {			
			
			addDataPoint("pred:"+ODKey, time, camion.getPredictedTravelTime()/camion.getPredictTimeAdjustFactor());
		}

		addDataPoint("reel:"+ODKey, time, camion.getCurrentTravelTime()/camion.getPredictTimeAdjustFactor());
		
	}

	@Override
	public void eventDispatched(AWTEvent arg0) {
	} 

	@Override
	public void minePaused(Mine mine) {
	}

	@Override
	public void mineResetted(MineSimulator mineSimulator) {
		this.remove(chartPanel);
		setChart(mineSimulator.getMine().getDataSeriesHandles(), mineSimulator.getTempsSimulationSeconds());
		
	}

	@Override
	public void mineUpdated(Mine mine) {
	}

	@Override
	public void minUnpaused(Mine mine) {
	}

	private JFreeChart createChart(final XYDataset dataset, double tempsSimulationSeconds) {

		JFreeChart chart = ChartFactory.createXYLineChart(
				"Temps de parcours observés et prédits", 
				"Temps de simulation (h)",
				"Temps de parcours (min)", 
				dataset, 
				PlotOrientation.VERTICAL,
				true, 
				true, 
				false
				);


		XYPlot plot = chart.getXYPlot();

		Range range = new Range(0, tempsSimulationSeconds/3600);
		plot.getDomainAxis().setRange(range);
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

		for(int i = 0 ; i < dataset.getSeriesCount(); i++) {
			if( ((String)dataset.getSeriesKey(i)).substring(0, 4).equals("reel") ) {
				renderer.setSeriesPaint(i, Color.blue);
			}
			else {
				renderer.setSeriesPaint(i, Color.red);
			}
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		}
		/*renderer.setSeriesPaint(0, Color.blue);
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));

		renderer.setSeriesPaint(1, Color.red);
		renderer.setSeriesStroke(1, new BasicStroke(2.0f));    
		
		renderer.setSeriesPaint(2, Color.RED);
		renderer.setSeriesStroke(2, new BasicStroke(2.0f));

		renderer.setSeriesPaint(3, Color.BLUE);
		renderer.setSeriesStroke(3, new BasicStroke(2.0f));  */

		plot.setRenderer(renderer);
		plot.setBackgroundPaint(Color.white);

		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);

		chart.getLegend().setFrame(BlockBorder.NONE);

		chart.setTitle(new TextTitle("Temps de parcours observés et prédits",
				new Font("Serif", Font.BOLD, 18)
				)
				);

		return chart;
	}

	private XYSeriesCollection createDataset() {    

		XYSeriesCollection dataset = new XYSeriesCollection();

		Iterator<Entry<String, XYSeries>> it = seriesMap.entrySet().iterator();
		while(it.hasNext()) {
			dataset.addSeries(it.next().getValue());
		}


		return dataset;
	}

	private void initUI(double tempsSimulationSeconds) {

		dataset = createDataset();

		JFreeChart chart = createChart(dataset, tempsSimulationSeconds);
		this.chartPanel = new ChartPanel(chart);

		chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		chartPanel.setBackground(Color.white);
		add(chartPanel);

		pack();
		setTitle("Temps de parcours observés et prédits");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private void setChart(ArrayList<String> dataSeriesHandles, double tempsSimulationSeconds) {
		
		//cree les series de donnees
		//
		seriesMap = new HashMap<String, XYSeries>();
		for(int i = 0 ; i < dataSeriesHandles.size(); i++) {
			
			seriesMap.put(dataSeriesHandles.get(i), new XYSeries(dataSeriesHandles.get(i)));
		}


		initUI(tempsSimulationSeconds);
		
	}


}