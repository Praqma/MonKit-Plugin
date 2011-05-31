package net.praqma.jenkins.plugin.monkit;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.praqma.monkit.MonKit;
import net.praqma.monkit.MonKitException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class MonKitPublisher extends Recorder {

	private String monKitFile = "monkit.xml";
	public static final MonKitFilenameFilter __MONKIT_FILENAME_FILTER = new MonKitFilenameFilter();
	private boolean onlyStable;
	
    private String[] unitsArray;
    private String units;
	
    @DataBoundConstructor 
    public MonKitPublisher( String monKitFile, boolean onlyStable, String units ) {
        this.monKitFile = monKitFile;
        this.onlyStable = onlyStable;
        System.out.println("I GOT=" + units);
        this.unitsArray = units.split("[;:\\+]");
        this.units = units;
    }
	
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		
        final FilePath[] moduleRoots = build.getModuleRoots();
        final boolean multipleModuleRoots = moduleRoots != null && moduleRoots.length > 1;
        final FilePath moduleRoot = multipleModuleRoots ? build.getWorkspace() : build.getModuleRoot();
               
        FilePath[] reports = new FilePath[0];
        
        System.out.println( "MODULEROOT: " + moduleRoot.getName() );
                
        try {
            reports = moduleRoot.list(monKitFile);

            // if the build has failed, then there's not
            // much point in reporting an error
            if (build.getResult().isWorseOrEqualTo(Result.FAILURE) && reports.length == 0)
                return true;

        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to find MonKit files"));
            build.setResult(Result.FAILURE);
        }
        
        List<MonKit> mks = new ArrayList<MonKit>();
        
        for (int i = 0; i < reports.length; i++) {
        	try {
				MonKit mke = MonKit.fromString( reports[i].readToString() );
				mks.add(mke);
			} catch (MonKitException e) {
                e.printStackTrace(listener.fatalError("Unable to get " + reports[i].getName() + ". Skipping"));
			}
        }
        
        MonKit mk = MonKit.merge(mks);
        
        final MonKitBuildAction mka = new MonKitBuildAction( build, mk.getObservations() );
        build.getActions().add(mka);
		
		return true;
	}
	
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
    private static class MonKitFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.matches("^monkit.*\\.xml$");
        }
    }
    
    public boolean getOnlyStable() {
        return onlyStable;
    }
    
    public String getMonKitFile() {
    	return monKitFile;
    }
    
    public boolean isOnlyStable() {
    	return onlyStable;
    }
    
    public String getUnits() {
    	return units;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new MonKitProjectAction(project, getOnlyStable(), units);
    }
    
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public String getDisplayName() {
			return "MonKit Report";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}
    }

}
