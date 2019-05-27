package eu.arrowhead.common.database.entity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import eu.arrowhead.common.Defaults;

@Entity
public class ServiceDefinition {

	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column (nullable = false, unique = true, length = Defaults.VARCHAR_BASIC)
	private String serviceDefinition;
	
	@Column (nullable = false)
	private ZonedDateTime createdAt = ZonedDateTime.now();
	
	@Column (nullable = false)
	private ZonedDateTime updatedAt = ZonedDateTime.now();
	
	@OneToMany (mappedBy = "serviceDefinition", fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<ServiceRegistry> serviceRegistryEntries = new HashSet<ServiceRegistry>();
	
	@OneToMany (mappedBy = "serviceDefinition", fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<IntraCloudAuthorization> intraCloudAuthorizations = new HashSet<IntraCloudAuthorization>();
	
	@OneToMany (mappedBy = "serviceDefinition", fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<InterCloudAuthorization> interCloudAuthorizations = new HashSet<InterCloudAuthorization>();
	
	public ServiceDefinition() {
		
	}

	public ServiceDefinition(String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getServiceDefinition() {
		return serviceDefinition;
	}

	public void setServiceDefinition(String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public ZonedDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(ZonedDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public Set<ServiceRegistry> getServiceRegistryEntries() {
		return serviceRegistryEntries;
	}

	public void setServiceRegistryEntries(Set<ServiceRegistry> serviceRegistryEntries) {
		this.serviceRegistryEntries = serviceRegistryEntries;
	}
	
	public Set<IntraCloudAuthorization> getIntraCloudAuthorizations() {
		return intraCloudAuthorizations;
	}

	public void setIntraCloudAuthorizations(Set<IntraCloudAuthorization> intraCloudAuthorizations) {
		this.intraCloudAuthorizations = intraCloudAuthorizations;
	}

	public Set<InterCloudAuthorization> getInterCloudAuthorizations() {
		return interCloudAuthorizations;
	}

	public void setInterCloudAuthorizations(Set<InterCloudAuthorization> interCloudAuthorizations) {
		this.interCloudAuthorizations = interCloudAuthorizations;
	}

	@Override
	public String toString() {
		return "ServiceDefinition [id=" + id + ", serviceDefinition=" + serviceDefinition + "]";
	}
		
}