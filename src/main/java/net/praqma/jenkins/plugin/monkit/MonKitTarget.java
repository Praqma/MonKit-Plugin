package net.praqma.jenkins.plugin.monkit;

public class MonKitTarget {
	private Integer healthy;
	private Integer unstable;
	
	private String category;
	
	public MonKitTarget() {
		
	}
	
	public MonKitTarget( String category, Integer healthy, Integer unhealthy, Integer unstable ) {
		this.category  = category;
		this.healthy   = healthy;
		this.unstable  = unstable;
	}
	
    public Integer getHealthy() {
        return healthy == null ? 80 : healthy;
    }

    public void setHealthy(Integer healthy) {
        this.healthy = healthy;
    }

    public Integer getUnstable() {
        return unstable == null ? 0 : unstable;
    }

    public void setUnstable(Integer unstable) {
        this.unstable = unstable;
    }

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}
}
