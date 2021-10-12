package ca.polymtl.SimulationMine.MineGui;

import java.awt.AWTEvent;

public class GuiEvent extends AWTEvent {

	
	
	/**
	 * Classe décrivant un événement de l'interface utilisateur
	 * @author Frédéric Quesnel
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public GuiEvent(Object arg0, int arg1) {
		super(arg0, arg1);
	}

}
