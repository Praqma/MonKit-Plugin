package net.praqma.jenkins.plugin.monkit;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import net.sf.json.JSONArray;

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
    
    public JSONArray getGraphData(String cat) {
        if (getLastResult() != null) {
            return getLastResult().graphData(cat);
        }
        return null;
    }
    
    public JSONArray getAllGraphData() {
            
        if (getLastResult() != null) {
            int size = getLastResult().getCategories().size();
            JSONArray arrays = new JSONArray();
            int i = 0;
            for(String cat : getLastResult().getCategories()) {
                arrays.add(getLastResult().graphData(cat));
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
