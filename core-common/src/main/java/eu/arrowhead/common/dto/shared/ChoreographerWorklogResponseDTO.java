package eu.arrowhead.common.dto.shared;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChoreographerWorklogResponseDTO implements Serializable {

	//=================================================================================================
    // members

    private static final long serialVersionUID = 1163490391868818182L;
	
    private long id;
    private String entryDate;
    private String planName;
    private String actionName;
    private String stepName;
    private Long sessionId;
    private String message;
    private String exception;
    
    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public ChoreographerWorklogResponseDTO() {}
    
    //-------------------------------------------------------------------------------------------------
	public ChoreographerWorklogResponseDTO(final long id, final String entryDate, final String planName, final String actionName, final String stepName, final Long sessionId,
										   final String message, final String exception) {
		this.id = id;
		this.entryDate = entryDate;
		this.planName = planName;
		this.actionName = actionName;
		this.stepName = stepName;
		this.sessionId = sessionId;
		this.message = message;
		this.exception = exception;
	}

	//-------------------------------------------------------------------------------------------------
	public long getId() { return id; }
	public String getEntryDate() { return entryDate; }
	public String getPlanName() { return planName; }
	public String getActionName() { return actionName; }
	public String getStepName() { return stepName; }
	public Long getSessionId() { return sessionId; }
	public String getMessage() { return message; }
	public String getException() { return exception; }
	
	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) { this.id = id; }
	public void setEntryDate(final String entryDate) { this.entryDate = entryDate; }
	public void setPlanName(final String planName) { this.planName = planName; }
	public void setActionName(final String actionName) { this.actionName = actionName; }
	public void setStepName(final String stepName) { this.stepName = stepName; }
	public void setSessionId(final Long sessionId) { this.sessionId = sessionId; }
	public void setMessage(final String message) { this.message = message; } 
	public void setException(final String exception) { this.exception = exception; }
	
	//-------------------------------------------------------------------------------------------------
    @Override
    public String toString() {
    	try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (final JsonProcessingException ex) {
			return "toString failure";
		}
    }
}
