package eu.arrowhead.common.database.entity;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import eu.arrowhead.common.Defaults;

@Entity
@Table (uniqueConstraints = @UniqueConstraint(columnNames = {"address", "port"}))
public class Relay {
	
	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column (nullable = false, length = Defaults.VARCHAR_BASIC)
	private String address;
	
	@Column (nullable = false)
	private int port;
	
	@Column (nullable = false)
	private Boolean secure;
	
	@Column (nullable = false)
	private ZonedDateTime createdAt = ZonedDateTime.now();
	
	@Column (nullable = false)
	private ZonedDateTime updatedAt = ZonedDateTime.now();

	public Relay() {
		
	}

	public Relay(String address, int port, Boolean secure) {
		this.address = address;
		this.port = port;
		this.secure = secure;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Boolean getSecure() {
		return secure;
	}

	public void setSecure(Boolean secure) {
		this.secure = secure;
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

	@Override
	public String toString() {
		return "Relay [id=" + id + ", address=" + address + ", port=" + port + "]";
	}
	
}