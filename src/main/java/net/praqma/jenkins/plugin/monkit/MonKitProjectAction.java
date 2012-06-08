package net.praqma.jenkins.plugin.monkit;

import hudson.model.*;
import java.io.IOException;
import net.sf.json.JSONArray;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class MonKitProjectAction extends Actionable implements ProminentProjectAction {

    private final AbstractProject<?, ?> project;
    private boolean onlyStable;

    public MonKitProjectAction(AbstractProject<?, ?> project, boolean onlyStable) {
        this.project    = project;
        this.onlyStable = onlyStable;
    }
	
    @Override
	public String getDisplayName() {
		return "MonKit";
	}

    @Override
	public String getSearchUrl() {
		return getUrlName();
	}

    @Override
	public String getIconFileName() {
		return "graph.gif";
	}

    @Override
	public String getUrlName() {
		return "monkit";
	}
	
    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (getLastResult() != null) {
            getLastResult().doGraph(req, rsp);
        }
    }
    
    public JSONArray getGraphData(String cat, MonKitTarget target) {
        if (getLastResult() != null) {
            return getLastResult().graphData(cat,target);
        }
        return null;
    }
    
    public JSONArray getAllGraphData() {           
        if (getLastResult() != null) {
            MonKitBuildAction mba = getLastResult();
            JSONArray arrays = new JSONArray();
            int i = 0;
            for(String cat : mba.getCategories()) {
                arrays.add(mba.graphData(cat, mba.getMonkitTargetForCategory(cat)));
                i++;
            }
            return arrays;
            
        }
        return null;
    }
    
    public MonKitBuildAction getLastResult() {
        for (AbstractBuild<?, ?> b = getLastBuildToBeConsidered(); b != null; b = b.getPreviousNotFailedBuild()) {
            if (b.getResult() == Result.FAILURE || (b.getResult() != Result.SUCCESS && onlyStable)) {
                continue;
            }
            
            MonKitBuildAction r = b.getAction(MonKitBuildAction.class);
            if (r != null) {
                return r;
            }
        }
        return null;
    }
    
    private AbstractBuild<?, ?> getLastBuildToBeConsidered(){
        return onlyStable ? project.getLastStableBuild() : project.getLastSuccessfulBuild();
    }

}
