package org.cytoscape.enhancer;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
//import static org.cytoscape.ding.DVisualLexicon.NODE_CUSTOMGRAPHICS_1;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
//import org.cytoscape.ding.customgraphics.NullCustomGraphics;
//import org.cytoscape.ding.impl.visualproperty.CustomGraphicsVisualProperty;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

/*
 * EnhancerController
 * 
 
 */

public class EnhancerController implements CytoPanelComponentSelectedListener, SetCurrentNetworkListener {

	private static final String ENHANCER_NAME = "egPie";
	private CyServiceRegistrar registrar;
	public EnhancerController(CyServiceRegistrar reg)
	{
		registrar = reg;
		initialize();
	}

//----------------------------------------------------------
	private CyApplicationManager cyApplicationManager;
	private CyNetwork network;
	private CyNetworkView networkView;
	VisualMappingFunctionFactory factory;
	private EnhancerPanel enhancerPanel;
	public void setEnhancerPanel(EnhancerPanel p) { enhancerPanel = p; }
	//----------------------------------------------------------
	private void initialize()
	{
		cyApplicationManager = registrar.getService(CyApplicationManager.class);
		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();
	}
	//-------------------------------------------------------------------
	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent arg0) 
	{	
		Component comp = arg0.getCytoPanel().getSelectedComponent();
//		if (comp instanceof EnhancerPanel)
//			scanNetwork();
	}
	//-------------------------------------------------------------------
	public CyNetworkView getNetworkView()		{ 	 return networkView;		}
		
		
	public void scanNetwork() {	}
	
	
	private void ensureCategoryCompatibility() {
		if (network == null)  return;		// don't clear list until a conflict is found
		List<String> extant = enhancerPanel.getColumnNames();
		List<String> networkColumns = getColumnNames();
		for (String col : extant)
		{
			CyColumn c =  network.getDefaultNodeTable().getColumn(col);
			if (c == null)
				enhancerPanel.clearCategories();
		}
	}

	//-------------------------------------------------------------------------------
	private CyNetworkView currentNetworkView;
	private boolean verbose = true;
	
	public String getCurrentNetworkName() {
		if (currentNetworkView != null)
			return "" + currentNetworkView.getModel().getSUID();		// TODO -- getCurrentNetworkName
		return "";
	}
		//-------------------------------------------------------------------------------
	public void layout()
	{
		networkView = cyApplicationManager.getCurrentNetworkView();

	}
	
	List<String> getColumnNames()
	{
		List<String> strs = new ArrayList<String>();
		CyNetwork net = cyApplicationManager.getCurrentNetwork();
		if (net != null)
		{
			CyTable table = net.getDefaultNodeTable();
			for (CyColumn col : table.getColumns())
			{
				if (null == col) 	continue;
				if (!isNumeric(col)) continue;
				if ("SUID".equals(col.getName())) continue;
				strs.add(col.getName());
			}
		}
		return strs;
	}
	
	private boolean isNumeric(CyColumn col) {
//		if (col == null) return false;
		Class<?> c = col.getType();  //getClass();
		String classs = c.getName();
		if (classs.equals("java.lang.Integer")) return true;
		if (classs.equals("java.lang.Long")) return true;
		if (classs.equals("java.lang.Float")) return true;
		if (classs.equals("java.lang.Double")) return true;
		return false;
	}

	public void enhance(String extracted, List<String> colNames) {
		
		String spec = extracted;
//		System.out.println(spec);

		if (network == null) return ;
		CyTable nodeTable = network.getDefaultNodeTable();
		CyColumn col = nodeTable.getColumn(ENHANCER_NAME);
		if (col == null)
		{
			nodeTable.createColumn(ENHANCER_NAME, String.class, false);
			col = nodeTable.getColumn(ENHANCER_NAME);
		}
		for (CyRow row : nodeTable.getAllRows())
			if (rowHasValues(nodeTable, row, colNames)) 
				row.set(ENHANCER_NAME, extracted);

		VisualStyle currentStyle = getStyle();

		VisualMappingFunctionFactory passthroughFactory = 
				registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = registrar.getService(RenderingEngineManager.class).getDefaultVisualLexicon();

// Set up the pass-through mapping

		VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		PassthroughMapping pMapping = 
				(PassthroughMapping<String, ?>) passthroughFactory.createVisualMappingFunction(ENHANCER_NAME,   String.class, customGraphics);
		currentStyle.addVisualMappingFunction(pMapping);
		
		networkView = cyApplicationManager.getCurrentNetworkView();
		if (networkView != null)
		{
			currentStyle.apply(networkView);
			networkView.updateView();
		}
	}

	private boolean rowHasValues(CyTable nodeTable, CyRow row, List<String> colNames )
	{
		for (String column : colNames)
		{
			CyColumn col = nodeTable.getColumn(column);
			Class<?> c = col.getType();
			Object d = row.get(column, c);
			if (d == null)	return false;
		}
		return true;
	}
	
	private VisualStyle getStyle()
	{
		// Now we cruise thru the list of node, then edge attributes looking for mappings.  Each mapping may potentially be a legend entry.
		VisualMappingManager manager = registrar.getService( VisualMappingManager.class);
		VisualStyle style = manager.getCurrentVisualStyle();
//		System.out.println("style: " + style.getTitle());
		return style;

	}
	
	//-------------------------------------------------------------------------------
	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {

		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();
		setCurrentNetView(networkView);
	}

	public void setCurrentNetView(CyNetworkView newView)
	{
		if (newView == null) return;		// use ""
//		if (newView.getSUID() == currentNetworkView.getSUID()) return;
		currentNetworkView = newView;
		currentNetworkView.getModel();
		enhancerPanel.enableControls(currentNetworkView != null);
		ensureCategoryCompatibility();
	}
	
}
