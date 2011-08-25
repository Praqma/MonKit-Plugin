package net.praqma.jenkins.plugin.monkit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.praqma.jenkins.plugin.monkit.MonKitPublisher.Case;
import net.praqma.monkit.MonKitCategory;
import net.praqma.monkit.MonKitObservation;

import org.codehaus.groovy.syntax.Numbers;
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
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;

public class MonKitBuildAction implements HealthReportingAction, Action {

	private List<MonKitCategory> monkit;
	private final AbstractBuild<?, ?> build;
	private boolean onlyStable;
	private MonKitPublisher publisher;
	
	public MonKitBuildAction( AbstractBuild<?, ?> build, List<MonKitCategory> monkit ) {
		this.monkit     = monkit;
		this.build      = build;
		this.onlyStable = false;
	}
	
	public void setPublisher( MonKitPublisher publisher) {
		this.publisher = publisher;
	}
	
	public String getDisplayName() {
		return "MonKit";
	}

	public String getIconFileName() {
		return "graph.gif";
	}

	public String getUrlName() {
		return "monkit";
	}

	public HealthReport getBuildHealth() {
		Case worst = publisher.getWorst(monkit);
		
		/* Unstable */
		if( worst.health == null ) {
			return new HealthReport( 0, "MonKit Report: " + worst.category + " for " + worst.name );
		} else if( worst.category == null ) {
			return new HealthReport( 100, "MonKit Report" );
		} else {
			return new HealthReport( worst.health.intValue(), "MonKit Report: " + worst.category + " for " + worst.name );
		}
	}
	
	public HealthReport getBuildHealth1() {
		Integer health = 1;
		float worst = 100f;
		String worstStr = "Unknown";
		boolean healthy = true;
		
		/* Stupid n^2 running time.... */
		for( MonKitTarget mkt : publisher.getTargets() ) {
			for( MonKitCategory mkc : monkit ) {
				/* We got the correct category */
				if( mkt.getCategory().equalsIgnoreCase(mkc.getName()) ) {
					/* Loop the observations */
					for( MonKitObservation mko : mkc ) {
						
						/* Calculate health */
						Float f = new Float( mko.getValue() );
						
						Float fu = new Float( mkt.getUnstable() );
						Float fh = new Float( mkt.getHealthy() );
						
						boolean isGreater = fu < fh;
						
						//System.out.println( "F=" + f + ". FU=" + fu + ". FH=" + fh + ". ISGREATER=" + isGreater );
						
						if( ( isGreater && f < fu ) || ( !isGreater && f > fu ) ) {
							return new HealthReport( 0, "MonKit Report: " + mkc.getName() + " for " + mko.getName() );
						}
						
						//System.out.println( "F3=" + fh );
						
						if( ( isGreater && f < fh ) || (  !isGreater && f > fh ) ) {
							float diff = fh - fu;
							float nf1 = f - fu;
							float inter = ( nf1 / diff ) * 100;
							
							//System.out.println( "DIFF=" + diff + ". NF1=" + nf1 + ". INTER=" +  inter );
							
							//System.out.println( "INTER=" +  inter );
							
							if( inter < worst ) {
								worst = inter;
								worstStr = mkc.getName() + " for " + mko.getName();
							}
							healthy = false;
						}
					}
				}
			}
		}
		
		if( healthy ) {
			return new HealthReport( 100, "MonKit Report" );
		} else {
			return new HealthReport( (int)worst, "MonKit Report: " + worstStr);
		}
	}
	
	public List<String> getCategories() {
		List<String> categories = new ArrayList<String>();
		for( MonKitCategory mkc : monkit ) {
			categories.add(mkc.getName());
		}
		
		return categories;
	}
	
	/*
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
    	rsp.getOutputStream().println("Her kommer der noget herre fedt paa et tidspunkt....");
    }
    */
	
	public List<MonKitCategory> getMonKitCategories() {
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
    	String category = req.getParameter("category");
    	
    	int width = 500, height = 200;
    	String w = req.getParameter("width");
    	String h = req.getParameter("height");
    	if( w != null && w.length() > 0 ) {
    		width = Integer.parseInt(w);
    	}
    	
    	if( h != null && h.length() > 0 ) {
    		height = Integer.parseInt(h);
    	}
    	
    	//System.out.println("I got cat " + category);
    	if( category == null ) {
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
        
        float health = 100.0f;
        int min = 1000000, max = -110001100;
        String scale = "Unknown";

        for (MonKitBuildAction a = this; a != null; a = a.getPreviousResult()) {
            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.build);
            /* Loop the categories */
            for (MonKitCategory mkc : a.getMonKitCategories() ) {
            	/* Check the category name */
            	if( mkc.getName().equalsIgnoreCase(category) ) {
            		
            		/**/
            		MonKitTarget mkt = publisher.getTarget(category);
            		
            		Float fu = null;
            		Float fh = null;
            		if( mkt != null ) {
						fu = new Float( mkt.getUnstable() );
						fh = new Float( mkt.getHealthy() );
            		}
					
					/* Loop the observations */
            		Float f = 0f;
            		for( MonKitObservation mko : mkc ) {
            			//System.out.println( "OBS=" + mko.getName() );
            			
            			try {
            				f = new Float( mko.getValue() );
            			} catch( NumberFormatException e) {
            				System.err.println( "[MonKitPlugin] Unknown number " + mko.getValue() );
            				continue;
            			}
	            		dsb.add(f, mko.getName(), label);
	            		
	            		if( f.intValue() > max ) {
	            			max = f.intValue() + 1;
	            		}
	            		
	            		if( f.intValue() < min ) {
	            			min = f.intValue();
	            			if( min != 0 ) {
	            				min--;
	            			}
	            		}
	            		
	            		scale = mkc.getScale();
	            		
            		}
            		
            		/*  HEALTH!!!
            		 * Only consider last  */
            		if( mkt != null ) {
						boolean isGreater = fu < fh;
						
						/* Mark build as unstable */
						if( ( isGreater && f < fu ) || ( !isGreater && f > fu ) ) {
							health = 0.0f;
						} else if( ( isGreater && f < fh ) || (  !isGreater && f > fh ) ) {
							float diff = fh - fu;
							float nf1 = f - fu;
							float inter = ( nf1 / diff ) * 100;
							
							if( inter < health ) {
								health = inter;
							}
						}
            		}
            	}
            }
        }
        
        if( health < 100.0f ) {
        	category += " health @ " + Math.abs( Math.floor( health ) ) + "%";
        }
        
        //System.out.println("MIN=" + min + ", MAX=" + max);

        ChartUtil.generateGraph(req, rsp, createChart(dsb.build(), category, scale, max, min), width, height);
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
