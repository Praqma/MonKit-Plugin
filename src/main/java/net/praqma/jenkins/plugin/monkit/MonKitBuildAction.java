package net.praqma.jenkins.plugin.monkit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.praqma.monkit.MonKit;
import net.praqma.monkit.MonKitObservation;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;

public class MonKitBuildAction implements HealthReportingAction {

	private List<MonKitObservation> monkit;
	private final AbstractBuild<?, ?> build;
	private boolean onlyStable;
	
	public MonKitBuildAction( AbstractBuild<?, ?> build, List<MonKitObservation> monkit ) {
		this.monkit     = monkit;
		this.build      = build;
		this.onlyStable = false;
	}
	
	public String getDisplayName() {
		return "MonKit Reporter";
	}

	public String getIconFileName() {
		return null;
	}

	public String getUrlName() {
		return "monkit";
	}

	public HealthReport getBuildHealth() {
		return new HealthReport( 1, "Snade" );
	}
	
	public List<MonKitObservation> getObservations() {
		return monkit;
	}
	
	public AbstractBuild<?, ?> getBuild() {
		return build;
	}
	
    public MonKitBuildAction getPreviousResult() {
        return getPreviousResult(build);
    }
    
    private boolean includeOnlyStable() {
        return onlyStable;
    }
    
    /**
     * Gets the previous {@link CoberturaBuildAction} of the given build.
     */
    /*package*/
    static MonKitBuildAction getPreviousResult(AbstractBuild<?,?> start) {
        AbstractBuild<?, ?> b = start;
        while (true) {
            b = b.getPreviousNotFailedBuild();
            if (b == null) {
                return null;
            }
            
            assert b.getResult() != Result.FAILURE : "We asked for the previous not failed build";
            MonKitBuildAction r = b.getAction(MonKitBuildAction.class);
            if(r != null && r.includeOnlyStable() && b.getResult() != Result.SUCCESS){
                r = null;
            }
            
            if (r != null) {
                return r;
            }
        }
    }
	
    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
    	String type = req.getParameter("type");
    	System.out.println("I got " + type);
    	if( type == null ) {
    		throw new IOException( "No type given" );
    	}
    	
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
            return;
        }

        Calendar t = build.getTimestamp();

        if ( req.checkIfModified( t, rsp ) ) {
            return; // up to date
        }

        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();
        
        int min = 1000000, max = -110001100;
        String scale = "Unknown";

        for (MonKitBuildAction a = this; a != null; a = a.getPreviousResult()) {
            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.build);
            for (MonKitObservation mko : a.getObservations() ) {
            	if( mko.getName().equalsIgnoreCase(type) ) {
            		Float f = new Float( mko.getValue() );
            		dsb.add(f, mko.getName(), label);
            		
            		/* MAX */
            		if( f.intValue() > max ) {
            			max = f.intValue();
            		}
            		
            		/* MIN */
            		if( f.intValue() < min ) {
            			min = f.intValue();
            		}
            		
            		scale = mko.getScale();
            	}
            }
        }

        ChartUtil.generateGraph(req, rsp, createChart(dsb.build(), type, scale, max, min), 500, 200);
    }
	
    private JFreeChart createChart(CategoryDataset dataset, String title, String yaxis, int max, int min) {

        final JFreeChart chart = ChartFactory.createLineChart(
                title,                   // chart title
                null,                   // unused
                yaxis,                    // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips
                false                     // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        final LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.RIGHT);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setUpperBound(max);
        rangeAxis.setLowerBound(min);

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke(new BasicStroke(2.0f));
        ColorPalette.apply(renderer);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

        return chart;
    }

}
