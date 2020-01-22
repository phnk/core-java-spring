package eu.arrowhead.core.choreographer;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.CoreDefaults;
import eu.arrowhead.common.CoreUtilities;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.CoreUtilities.ValidatedPageParams;
import eu.arrowhead.common.dto.internal.ChoreographerPlanRequestDTO;
import eu.arrowhead.common.dto.shared.ChoreographerPlanResponseDTO;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.core.choreographer.database.service.ChoreographerDBService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = { CoreCommonConstants.SWAGGER_TAG_ALL })
@CrossOrigin(maxAge = Defaults.CORS_MAX_AGE, allowCredentials = Defaults.CORS_ALLOW_CREDENTIALS,
			 allowedHeaders = { HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION }
)
@RestController
@RequestMapping(CommonConstants.CHOREOGRAPHER_URI)
public class ChoreographerController {


	//=================================================================================================
	// members

    private static final String PATH_VARIABLE_ID = "id";
    private static final String ID_NOT_VALID_ERROR_MESSAGE = "ID must be greater than 0.";

    private static final String CHOREOGRAPHER_ACTION_PLAN_MGMT_URI = CoreCommonConstants.MGMT_URI + "/actionplan";
    private static final String CHOREOGRAPHER_ACTION_PLAN_MGMT_BY_ID_URI = CHOREOGRAPHER_ACTION_PLAN_MGMT_URI + "/{" + PATH_VARIABLE_ID + "}";

    private static final String GET_CHOREOGRAPHER_ACTION_PlAN_MGMT_HTTP_200_MESSAGE = "ChoreographerActionStep returned.";
    private static final String GET_CHOREOGRAPHER_ACTION_PLAN_MGMT_HTTP_400_MESSAGE = "Could not retrieve ChoreographerActionStep.";

    private static final String POST_CHOREOGRAPHER_ACTION_PLAN_WITH_SERVICE_DEFINITIONS_MGMT_HTTP_201_MESSAGE = "ChoreographerActionPlan created with given service definitions.";
    private static final String POST_CHOREOGRAPHER_ACTION_PLAN_WITH_SERVICE_DEFINITIONS_MGMT_HTTP_400_MESSAGE = "Could not create ChoreographerActionPlan.";

    private static final String DELETE_CHOREOGRAPHER_ACTION_PLAN_HTTP_200_MESSAGE = "ChoreographerActionPlan successfully removed.";
    private static final String DELETE_CHOREOGRAPHER_ACTION_PLAN_HTTP_400_MESSAGE = "Could not remove ChoreographerActionPlan.";

    private final Logger logger = LogManager.getLogger(ChoreographerController.class);

    @Autowired
    private ChoreographerDBService choreographerDBService;
    
    //=================================================================================================
	// methods

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Return an echo message with the purpose of testing the core service availability", response = String.class, tags = { CoreCommonConstants.SWAGGER_TAG_CLIENT })
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = CommonConstants.ECHO_URI)
    public String echoService() {
        return "Got it!";
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Register one or more ActionPlans.", 
    			  notes = "Please note that creating ActionPlans this way means that Actions included in the ActionPlan(s) can't be already existing Actions.",
    			  tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_CREATED, message = POST_CHOREOGRAPHER_ACTION_PLAN_WITH_SERVICE_DEFINITIONS_MGMT_HTTP_201_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = POST_CHOREOGRAPHER_ACTION_PLAN_WITH_SERVICE_DEFINITIONS_MGMT_HTTP_400_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PostMapping(path = CHOREOGRAPHER_ACTION_PLAN_MGMT_URI, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = org.springframework.http.HttpStatus.CREATED)
    @ResponseBody public void registerPlans(@RequestBody final List<ChoreographerPlanRequestDTO> requests) {
        for (final ChoreographerPlanRequestDTO request : requests) {
            checkPlanRequest(request, CommonConstants.CHOREOGRAPHER_URI + CHOREOGRAPHER_ACTION_PLAN_MGMT_URI);
            choreographerDBService.createPlan(request.getName(), request.getFirstActionName(), request.getActions());
        }
    }

    //-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Remove the requested ChoreographerActionPlan entry.", tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = DELETE_CHOREOGRAPHER_ACTION_PLAN_HTTP_200_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = DELETE_CHOREOGRAPHER_ACTION_PLAN_HTTP_400_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @DeleteMapping(path = CHOREOGRAPHER_ACTION_PLAN_MGMT_BY_ID_URI)
    public void removeActionPlanById(@PathVariable(value = PATH_VARIABLE_ID) final long id) {
        logger.debug("New ChoreographerActionStep delete request received with id of " + id + ".");

        if (id < 1) {
            throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, CommonConstants.CHOREOGRAPHER_URI + CHOREOGRAPHER_ACTION_PLAN_MGMT_BY_ID_URI);
        }

        choreographerDBService.removePlanEntryById(id);
        logger.debug("ChoreographerActionStep with id: " + id + " successfully deleted!");
    }

    //-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Return the requested ChoreographerActionPlan entry.", response = ChoreographerPlanResponseDTO.class, tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
    @ApiResponses (value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = GET_CHOREOGRAPHER_ACTION_PlAN_MGMT_HTTP_200_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = GET_CHOREOGRAPHER_ACTION_PLAN_MGMT_HTTP_400_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = CHOREOGRAPHER_ACTION_PLAN_MGMT_BY_ID_URI, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public ChoreographerPlanResponseDTO getActionPlanById(@PathVariable(value = PATH_VARIABLE_ID) final long id) {
        logger.debug("New ChoreographerActionStep get request received with id: " + id + ".");

        if (id < 1) {
            throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, CommonConstants.CHOREOGRAPHER_URI + CHOREOGRAPHER_ACTION_PLAN_MGMT_BY_ID_URI);
        }

        final ChoreographerPlanResponseDTO choreographerActionPlanEntryByIdResponse = choreographerDBService.getPlanByIdResponse(id);
        logger.debug("ChoreographerActionPlan entry with id: " + " successfully retrieved!");

        return choreographerActionPlanEntryByIdResponse;
    }

    //-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Return requested ChoreographerActionPlan entries by the given parameters.", response = List.class, tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
    @ApiResponses (value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = GET_CHOREOGRAPHER_ACTION_PlAN_MGMT_HTTP_200_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = GET_CHOREOGRAPHER_ACTION_PLAN_MGMT_HTTP_400_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @GetMapping(path = CHOREOGRAPHER_ACTION_PLAN_MGMT_URI, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody public List<ChoreographerPlanResponseDTO> getChoreographerActionPlans(
            @RequestParam(name = CoreCommonConstants.REQUEST_PARAM_PAGE, required = false) final Integer page,
            @RequestParam(name = CoreCommonConstants.REQUEST_PARAM_ITEM_PER_PAGE, required = false) final Integer size,
            @RequestParam(name = CoreCommonConstants.REQUEST_PARAM_DIRECTION, defaultValue = CoreDefaults.DEFAULT_REQUEST_PARAM_DIRECTION_VALUE) final String direction,
            @RequestParam(name = CoreCommonConstants.REQUEST_PARAM_SORT_FIELD, defaultValue = CoreCommonConstants.COMMON_FIELD_NAME_ID) final String sortField) {
        logger.debug("New ChoreographerActionPlan get request received with page: {} and item_per page: {}.", page, size);

        final ValidatedPageParams validatedPageParams = CoreUtilities.validatePageParameters(page, size, direction, sortField);
        final List<ChoreographerPlanResponseDTO> choreographerActionPlanEntriesResponse = choreographerDBService.getPlanEntriesResponse(validatedPageParams.getValidatedPage(),
        																																						 validatedPageParams.getValidatedSize(),
        																																						 validatedPageParams.getValidatedDirecion(),
        																																						 sortField);
        logger.debug("ChoreographerActionPlans with page: {} and item_per page: {} retrieved successfully", page, size);

        return choreographerActionPlanEntriesResponse;
    }
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
    private void checkPlanRequest(final ChoreographerPlanRequestDTO request, final String origin) {
        logger.debug("checkPlanRequest started...");

        if (Utilities.isEmpty(request.getName())) {
            throw new BadPayloadException("Plan name is null or blank.", HttpStatus.SC_BAD_REQUEST, origin);
        }
    }

}