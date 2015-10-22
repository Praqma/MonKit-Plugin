package net.praqma.jenkins.plugin.monkit;

public class MonKitTarget {
	private Long healthy;
	private Long unstable;
	
	private String category;
	
	public MonKitTarget() {
		
	}
	
	public MonKitTarget( String category, Long healthy, Long unhealthy, Long unstable ) {
		this.category  = category;
		this.healthy   = healthy;
		this.unstable  = unstable;
	}
	
    public Long getHealthy() {
        return healthy == null ? 80 : healthy;
    }

    public void setHealthy(Long healthy) {
        this.healthy = healthy;
    }

    public Long getUnstable() {
        return unstable == null ? 0 : unstable;
    }

    public void setUnstable(Long unstable) {
        this.unstable = unstable;
    }

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}
}
