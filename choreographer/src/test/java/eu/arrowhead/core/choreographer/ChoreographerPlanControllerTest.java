/********************************************************************************
 * Copyright (c) 2021 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   AITIA - implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.core.choreographer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.database.entity.ChoreographerSession;
import eu.arrowhead.common.dto.internal.ChoreographerSessionStatus;
import eu.arrowhead.common.dto.internal.ChoreographerStartSessionDTO;
import eu.arrowhead.common.dto.shared.ChoreographerCheckPlanResponseDTO;
import eu.arrowhead.common.dto.shared.ChoreographerPlanListResponseDTO;
import eu.arrowhead.common.dto.shared.ChoreographerPlanRequestDTO;
import eu.arrowhead.common.dto.shared.ChoreographerPlanResponseDTO;
import eu.arrowhead.common.dto.shared.ChoreographerRunPlanRequestDTO;
import eu.arrowhead.common.dto.shared.ChoreographerRunPlanResponseDTO;
import eu.arrowhead.common.dto.shared.ErrorMessageDTO;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.core.choreographer.database.service.ChoreographerPlanDBService;
import eu.arrowhead.core.choreographer.database.service.ChoreographerSessionDBService;
import eu.arrowhead.core.choreographer.validation.ChoreographerPlanExecutionChecker;
import eu.arrowhead.core.choreographer.validation.ChoreographerPlanValidator;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChoreographerMain.class)
@ContextConfiguration (classes = { ChoreographerServiceTestContext.class })
public class ChoreographerPlanControllerTest {
	
	//=================================================================================================
	// members
	
    private static final String PLAN_MGMT_URI = CommonConstants.CHOREOGRAPHER_URI + CoreCommonConstants.MGMT_URI + "/plan";
    private static final String START_SESSION_MGMT_URI = CommonConstants.CHOREOGRAPHER_URI + CoreCommonConstants.MGMT_URI + "/session/start";
    private static final String CHECK_PLAN_MGMT_URI = CommonConstants.CHOREOGRAPHER_URI + CoreCommonConstants.MGMT_URI + "/check-plan";
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean(name = "mockChoreographerPlanDBService")
    private ChoreographerPlanDBService planDBService;
    
    @MockBean(name = "mockChoreographerSessionDBService")
    private ChoreographerSessionDBService sessionDBService;
    
    @MockBean(name = "mockPlanValidator")
    private ChoreographerPlanValidator planValidator;

    @MockBean(name = "mockPlanChecker")
    private ChoreographerPlanExecutionChecker planChecker;

	@MockBean(name = "mockJmsService")
    private JmsTemplate jms;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEcho() throws Exception {
		this.mockMvc.perform(get("/choreographer/echo")
					.accept(MediaType.TEXT_PLAIN))
					.andExpect(status().isOk());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPlansWithoutParameters() throws Exception {
		final int numOfEntries = 4;
		final ChoreographerPlanListResponseDTO dtoList = createMockPlanDTOList(numOfEntries);
		
		when(planDBService.getPlanEntriesResponse(anyInt(), anyInt(), any(), anyString())).thenReturn(dtoList);
		
		final MvcResult response = this.mockMvc.perform(get(PLAN_MGMT_URI)
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final ChoreographerPlanListResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ChoreographerPlanListResponseDTO.class);
		
		Assert.assertEquals(numOfEntries, responseBody.getData().size());
		Assert.assertEquals(numOfEntries, responseBody.getCount());
		
		verify(planDBService, times(1)).getPlanEntriesResponse(anyInt(), anyInt(), any(), anyString());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPlansWithPageAndSizeParameter() throws Exception {
		final int numOfEntries = 4;
		final ChoreographerPlanListResponseDTO dtoList = createMockPlanDTOList(numOfEntries);
		
		when(planDBService.getPlanEntriesResponse(anyInt(), anyInt(), any(), anyString())).thenReturn(dtoList);
		
		final MvcResult response = this.mockMvc.perform(get(PLAN_MGMT_URI)
											   .param(CoreCommonConstants.REQUEST_PARAM_PAGE, "0")
											   .param(CoreCommonConstants.REQUEST_PARAM_ITEM_PER_PAGE, String.valueOf(numOfEntries))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final ChoreographerPlanListResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ChoreographerPlanListResponseDTO.class);
		
		Assert.assertEquals(numOfEntries, responseBody.getData().size());
		Assert.assertEquals(numOfEntries, responseBody.getCount());
		
		verify(planDBService, times(1)).getPlanEntriesResponse(anyInt(), anyInt(), any(), anyString());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPlansWithSpecifiedPageButNullSizeParameter() throws Exception {
		final MvcResult response = this.mockMvc.perform(get(PLAN_MGMT_URI)
											   .param(CoreCommonConstants.REQUEST_PARAM_PAGE, "0")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.BAD_PAYLOAD, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("Defined page or size could not be with undefined size or page.", responseBody.getErrorMessage());
		
		verify(planDBService, never()).getPlanEntriesResponse(anyInt(), anyInt(), any(), anyString());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPlansWithInvalidSortDirection() throws Exception {
		final MvcResult response = this.mockMvc.perform(get(PLAN_MGMT_URI)
											   .param(CoreCommonConstants.REQUEST_PARAM_DIRECTION, "invalid")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.BAD_PAYLOAD, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("Invalid sort direction flag", responseBody.getErrorMessage());
		
		verify(planDBService, never()).getPlanEntriesResponse(anyInt(), anyInt(), any(), anyString());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPlanByIdWithInvalidId() throws Exception {
		final MvcResult response = this.mockMvc.perform(get(PLAN_MGMT_URI + "/-1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.BAD_PAYLOAD, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("ID must be greater than 0.", responseBody.getErrorMessage());
		
		verify(planDBService, never()).getPlanByIdResponse(anyInt());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPlanByIdWithValidId() throws Exception {
		final ChoreographerPlanResponseDTO dto = new ChoreographerPlanResponseDTO();
		dto.setId(1);
		dto.setName("plan");
		
		when(planDBService.getPlanByIdResponse(1)).thenReturn(dto);
		
		final MvcResult response = this.mockMvc.perform(get(PLAN_MGMT_URI + "/1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final ChoreographerPlanResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ChoreographerPlanResponseDTO.class);
		
		Assert.assertEquals("plan", responseBody.getName());
		
		verify(planDBService, times(1)).getPlanByIdResponse(1);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemovePlanByIdWithInvalidId() throws Exception {
		final MvcResult response = this.mockMvc.perform(delete(PLAN_MGMT_URI + "/-1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.BAD_PAYLOAD, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("ID must be greater than 0.", responseBody.getErrorMessage());
		
		verify(planDBService, never()).removePlanEntryById(anyInt());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemovePlanByIdWithValidId() throws Exception {
		final ChoreographerPlanResponseDTO dto = new ChoreographerPlanResponseDTO();
		dto.setId(1);
		dto.setName("plan");
		
		doNothing().when(planDBService).removePlanEntryById(1);
		
		this.mockMvc.perform(delete(PLAN_MGMT_URI + "/1")
		 		    .accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
		
		verify(planDBService, times(1)).removePlanEntryById(1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterPlanInvalidInput() throws Exception {
		final ChoreographerPlanRequestDTO request = new ChoreographerPlanRequestDTO();
		
		when(planValidator.validatePlan(any(ChoreographerPlanRequestDTO.class), anyString())).thenThrow(new InvalidParameterException("Plan name is null or blank."));
		
		final MvcResult response = this.mockMvc.perform(post(PLAN_MGMT_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(request))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.INVALID_PARAMETER, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("Plan name is null or blank.", responseBody.getErrorMessage());
		
		verify(planValidator, times(1)).validatePlan(any(ChoreographerPlanRequestDTO.class), anyString());
		verify(planDBService, never()).createPlanResponse(any(ChoreographerPlanRequestDTO.class));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterPlanValidInput() throws Exception {
		final ChoreographerPlanRequestDTO request = new ChoreographerPlanRequestDTO();
		final ChoreographerPlanResponseDTO responseDTO = new ChoreographerPlanResponseDTO();
		responseDTO.setId(1);
		responseDTO.setName("plan");
		
		when(planValidator.validatePlan(any(ChoreographerPlanRequestDTO.class), anyString())).thenReturn(request);
		when(planDBService.createPlanResponse(any(ChoreographerPlanRequestDTO.class))).thenReturn(responseDTO);
		
		final MvcResult response = this.mockMvc.perform(post(PLAN_MGMT_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(request))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isCreated())
											   .andReturn();
		
		final ChoreographerPlanResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ChoreographerPlanResponseDTO.class);
		
		Assert.assertEquals("plan", responseBody.getName());
		
		verify(planValidator, times(1)).validatePlan(any(ChoreographerPlanRequestDTO.class), anyString());
		verify(planDBService, times(1)).createPlanResponse(any(ChoreographerPlanRequestDTO.class));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartPlansEmptyList() throws Exception {
		final MvcResult response = this.mockMvc.perform(post(START_SESSION_MGMT_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(List.of()))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.BAD_PAYLOAD, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("No plan specified to start.", responseBody.getErrorMessage());
		
		verify(planChecker, never()).checkPlanForExecution(any(ChoreographerRunPlanRequestDTO.class));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartPlansProblemWithPlan() throws Exception {
		when(planChecker.checkPlanForExecution(any(ChoreographerRunPlanRequestDTO.class))).thenReturn(new ChoreographerRunPlanResponseDTO(null, List.of("Plan id is not valid."), false));
		
		final MvcResult response = this.mockMvc.perform(post(START_SESSION_MGMT_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(List.of(new ChoreographerRunPlanRequestDTO())))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final TypeReference<List<ChoreographerRunPlanResponseDTO>> typeReference = new TypeReference<>() {};
		final List<ChoreographerRunPlanResponseDTO> responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), typeReference);
		
		Assert.assertEquals(1, responseBody.size());
		Assert.assertEquals(ChoreographerSessionStatus.ABORTED, responseBody.get(0).getStatus());
		Assert.assertEquals("Plan id is not valid.", responseBody.get(0).getErrorMessages().get(0));
		
		verify(planChecker, times(1)).checkPlanForExecution(any(ChoreographerRunPlanRequestDTO.class));
		verify(sessionDBService, never()).initiateSession(anyLong(), anyString());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStartPlansOk() throws Exception {
		final ChoreographerRunPlanRequestDTO runRequest = new ChoreographerRunPlanRequestDTO();
		runRequest.setPlanId(1L);
		
		final ChoreographerSession session = new ChoreographerSession();
		session.setId(1212);
		
		when(planChecker.checkPlanForExecution(any(ChoreographerRunPlanRequestDTO.class))).thenReturn(null);
		when(sessionDBService.initiateSession(1, null)).thenReturn(session);
		doNothing().when(jms).convertAndSend(eq("start-session"), any(ChoreographerStartSessionDTO.class));
		
		final MvcResult response = this.mockMvc.perform(post(START_SESSION_MGMT_URI)
											   .contentType(MediaType.APPLICATION_JSON)
											   .content(objectMapper.writeValueAsBytes(List.of(runRequest)))
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final TypeReference<List<ChoreographerRunPlanResponseDTO>> typeReference = new TypeReference<>() {};
		final List<ChoreographerRunPlanResponseDTO> responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), typeReference);
		
		Assert.assertEquals(1, responseBody.size());
		Assert.assertEquals(ChoreographerSessionStatus.INITIATED, responseBody.get(0).getStatus());
		Assert.assertEquals(1, responseBody.get(0).getPlanId().longValue());
		Assert.assertEquals(1212, responseBody.get(0).getSessionId().longValue());
		
		verify(planChecker, times(1)).checkPlanForExecution(any(ChoreographerRunPlanRequestDTO.class));
		verify(sessionDBService, times(1)).initiateSession(eq(1L), isNull());
		verify(jms, times(1)).convertAndSend(eq("start-session"), any(ChoreographerStartSessionDTO.class));
	}
	
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPlanWithInvalidId() throws Exception {
		final MvcResult response = this.mockMvc.perform(get(CHECK_PLAN_MGMT_URI + "/-1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isBadRequest())
											   .andReturn();
		
		final ErrorMessageDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ErrorMessageDTO.class);
		
		Assert.assertEquals(ExceptionType.BAD_PAYLOAD, responseBody.getExceptionType());
		Assert.assertEquals(400, responseBody.getErrorCode());
		Assert.assertEquals("ID must be greater than 0.", responseBody.getErrorMessage());
		
		verify(planChecker, never()).checkPlanForExecution(anyBoolean(), anyLong());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPlanWithValidId1() throws Exception {
		
		when(planChecker.checkPlanForExecution(false, 1)).thenReturn(new ChoreographerRunPlanResponseDTO(1L, List.of("something wrong"), false));
		
		final MvcResult response = this.mockMvc.perform(get(CHECK_PLAN_MGMT_URI + "/1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final ChoreographerCheckPlanResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ChoreographerCheckPlanResponseDTO.class);
		
		Assert.assertEquals(1, responseBody.getPlanId());
		Assert.assertEquals(1, responseBody.getErrorMessages().size());
		Assert.assertEquals("something wrong", responseBody.getErrorMessages().get(0));
		
		verify(planChecker, times(1)).checkPlanForExecution(false, 1);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPlanWithValidId2() throws Exception {
		
		when(planChecker.checkPlanForExecution(false, 1)).thenReturn(null);
		
		final MvcResult response = this.mockMvc.perform(get(CHECK_PLAN_MGMT_URI + "/1")
											   .accept(MediaType.APPLICATION_JSON))
											   .andExpect(status().isOk())
											   .andReturn();
		
		final ChoreographerCheckPlanResponseDTO responseBody = objectMapper.readValue(response.getResponse().getContentAsByteArray(), ChoreographerCheckPlanResponseDTO.class);
		
		Assert.assertEquals(1, responseBody.getPlanId());
		Assert.assertEquals(0, responseBody.getErrorMessages().size());
		
		verify(planChecker, times(1)).checkPlanForExecution(false, 1);
	}

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private ChoreographerPlanListResponseDTO createMockPlanDTOList(int numOfEntries) {
		final List<ChoreographerPlanResponseDTO> list = new ArrayList<>(numOfEntries);
		for (int i = 0; i < numOfEntries; ++i) {
			list.add(new ChoreographerPlanResponseDTO());
		}
 		
		return new ChoreographerPlanListResponseDTO(list, numOfEntries);	
	}
}