package eu.arrowhead.core.eventhandler.database.service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.EventType;
import eu.arrowhead.common.database.entity.Subscription;
import eu.arrowhead.common.database.entity.SubscriptionPublisherConnection;
import eu.arrowhead.common.database.entity.System;
import eu.arrowhead.common.database.repository.EventTypeRepository;
import eu.arrowhead.common.database.repository.SubscriptionPublisherConnectionRepository;
import eu.arrowhead.common.database.repository.SubscriptionRepository;
import eu.arrowhead.common.database.repository.SystemRepository;
import eu.arrowhead.common.dto.DTOConverter;
import eu.arrowhead.common.dto.DTOUtilities;
import eu.arrowhead.common.dto.EventPublishRequestDTO;
import eu.arrowhead.common.dto.SubscriptionListResponseDTO;
import eu.arrowhead.common.dto.SubscriptionRequestDTO;
import eu.arrowhead.common.dto.SubscriptionResponseDTO;
import eu.arrowhead.common.dto.SystemRequestDTO;
import eu.arrowhead.common.dto.SystemResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class EventHandlerDBService {
	//=================================================================================================
	// members
	
	private static final String LESS_THAN_ONE_ERROR_MESSAGE= " must be greater than zero.";
	private static final String NOT_AVAILABLE_SORTABLE_FIELD_ERROR_MESSAGE = "The following sortable field  is not available : ";
	private static final String NOT_IN_DB_ERROR_MESSAGE = " is not available in database";
	private static final String EMPTY_OR_NULL_ERROR_MESSAGE = " is empty or null";
	private static final String NULL_ERROR_MESSAGE = " is null";
	private static final String NOT_VALID_ERROR_MESSAGE = " is not valid.";
	private static final String VIOLATES_UNIQUE_CONSTRAINT = " violates uniqueConstraint rules";
	
	private static final Logger logger = LogManager.getLogger(EventHandlerDBService.class);
	
	@Autowired
	private SubscriptionRepository subscriptionRepository;
	
	@Autowired
	private SubscriptionPublisherConnectionRepository subscriptionPublisherConnectionRepository;
	
	@Autowired
	private EventTypeRepository eventTypeRepository;
	
	@Autowired
	private SystemRepository systemRepository;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean secure;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SubscriptionListResponseDTO getSubscriptionsRequest(final int page, final int size,
			final Direction direction, final String sortField) {
		logger.debug("getSubscriptionsRequest started ...");
		
		final Page<Subscription> entries = getSubscriptions(page, size, direction, sortField);

		return DTOConverter.convertSubscriptionPageToSubscriptionListResponseDTO( entries );
		
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Subscription> getSubscriptions(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getSubscriptions started ...");
		
		final int validatedPage = page < 0 ? 0 : page;
		final int validatedSize = size < 1 ? Integer.MAX_VALUE : size;
		final Direction validatedDirection = direction == null ? Direction.ASC : direction;
		final String validatedSortField = Utilities.isEmpty(sortField) ? CommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();
		
		if (!Subscription.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
			throw new InvalidParameterException(validatedSortField + NOT_AVAILABLE_SORTABLE_FIELD_ERROR_MESSAGE);
		}
		
		try {
			
			return subscriptionRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
		
		} catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}

	}

	//-------------------------------------------------------------------------------------------------
	public SubscriptionResponseDTO getSubscriptionByIdRequest(final long id) {
		logger.debug("getSubscriptionByIdRequest started ...");
		
		final Subscription subscription = getSubscriptionById( id );
		if ( subscription == null || Utilities.isEmpty( subscription.getNotifyUri() )) {
			
			return new SubscriptionResponseDTO(); //return empty subscriptionResponseDTO
		}
		
		return DTOConverter.convertSubscriptionToSubscriptionResponseDTO( subscription );
	}

	//-------------------------------------------------------------------------------------------------
	public Subscription getSubscriptionById(final long id) {
		logger.debug("getSubscriptionById started ...");
		
		if ( id < 1) {
			
			throw new InvalidParameterException("SubscriberSystemId" + LESS_THAN_ONE_ERROR_MESSAGE );
		}
		
		try {
			
			final Optional<Subscription> subcriptionOptional = subscriptionRepository.findById( id );
			if ( subcriptionOptional.isPresent() ) {
				
				return subcriptionOptional.get();
				
			}
			
		} catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
		return new Subscription();
	}
	
	//-------------------------------------------------------------------------------------------------
	public Subscription getSubscriptionBySubscriptionRequestDTO(final SubscriptionRequestDTO subscriptionRequestDTO) {
		logger.debug("getSubscriptionBySubscriptionRequestDTO started ...");

		final Subscription subscription = validateSubscriptionRequestDTO( subscriptionRequestDTO );

		try {
			
			final Optional<Subscription> subcriptionOptional = subscriptionRepository.findByEventTypeAndSubscriberSystem( subscription.getEventType(), subscription.getSubscriberSystem() );
			if ( subcriptionOptional.isPresent()) {
				
				return  subcriptionOptional.get();
			}
			
		} catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
		return new Subscription(); //return empty subscription
	}
	//-------------------------------------------------------------------------------------------------
	@Transactional
	public void deleteSubscriptionRequest(final long id) {
		logger.debug("deleteSubscriptionRequest started ...");
		
		if ( id < 1) {
			
			throw new InvalidParameterException("SubscriberSystemId" + LESS_THAN_ONE_ERROR_MESSAGE );
		}
		
		try {
			
			final Optional<Subscription> subcriptionOptional = subscriptionRepository.findById( id );
			if ( subcriptionOptional.isPresent() ) {
				final Subscription subscriptionEntry = subcriptionOptional.get();
				final Set<SubscriptionPublisherConnection> involvedPublisherSystems = subscriptionPublisherConnectionRepository.findBySubscriptionEntry(subscriptionEntry);
				
				subscriptionPublisherConnectionRepository.deleteInBatch( involvedPublisherSystems );
				subscriptionRepository.refresh( subscriptionEntry );			
				
				subscriptionRepository.delete( subscriptionEntry );
				
				return;
			}
			
		} catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
		return;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional
	public Subscription registerSubscription(final SubscriptionRequestDTO request,
			final Set<SystemResponseDTO> authorizedPublishers) {
		logger.debug("registerSubscription started ...");
		
		final Subscription subscription = validateSubscriptionRequestDTO(request);
		checkSubscriptionUniqueConstrains( subscription );
		
		try {
			
			final Subscription subscriptionEntry = subscriptionRepository.save(subscription);
			addAndSaveSubscriptionEntryPublisherConnections(subscriptionEntry, request, authorizedPublishers);
			
			return subscriptionRepository.saveAndFlush(subscriptionEntry);
			
			
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional
	public Subscription updateSubscription( final long id, final SubscriptionRequestDTO request,
			final Set<SystemResponseDTO> authorizedPublishers) {
		logger.debug("updateSubscription started ...");
		
		try {

			final Subscription subscriptionToUpdate = getSubscriptionById( id );
			final long originalEventTypeId = subscriptionToUpdate.getEventType().getId();
			final long originalSubscriberSystemId = subscriptionToUpdate.getSubscriberSystem().getId();
			
			final Set<SubscriptionPublisherConnection> involvedPublisherSystems = subscriptionPublisherConnectionRepository.findBySubscriptionEntry( subscriptionToUpdate );
			
			final EventType eventTypeForUpdate = validateEventType( request.getEventType() );
			final System subscriberSystemForUpdate = validateSystemRequestDTO( request.getSubscriberSystem() );
			final String filterMetadataForUpdate = Utilities.map2Text(request.getFilterMetaData());
			final boolean matchMetaDataForUpdate = request.getMatchMetaData() == null ? false : request.getMatchMetaData();
			final String notifyUriForUpdate = request.getNotifyUri();
			final boolean onlyPredifindProvidersForUpdate = request.getSources() != null && !request.getSources().isEmpty();
			final ZonedDateTime startDateForUpdate = Utilities.parseUTCStringToLocalZonedDateTime( request.getStartDate() );
			final ZonedDateTime endDateForUpdate = Utilities.parseUTCStringToLocalZonedDateTime( request.getEndDate() );

			if ( originalEventTypeId != eventTypeForUpdate.getId() ||
					originalSubscriberSystemId != subscriberSystemForUpdate.getId()) {
				
				checkSubscriptionUpdateUniqueConstrains(eventTypeForUpdate, subscriberSystemForUpdate);
			}		
			
			subscriptionPublisherConnectionRepository.deleteInBatch( involvedPublisherSystems );
			subscriptionRepository.refresh( subscriptionToUpdate );		
			
			subscriptionToUpdate.setEventType(eventTypeForUpdate);
			subscriptionToUpdate.setSubscriberSystem(subscriberSystemForUpdate);
			subscriptionToUpdate.setFilterMetaData(filterMetadataForUpdate);
			subscriptionToUpdate.setMatchMetaData(matchMetaDataForUpdate);
			subscriptionToUpdate.setNotifyUri(notifyUriForUpdate);
			subscriptionToUpdate.setOnlyPredefinedPublishers(onlyPredifindProvidersForUpdate);
			subscriptionToUpdate.setStartDate(startDateForUpdate);
			subscriptionToUpdate.setEndDate(endDateForUpdate);
						
			addAndSaveSubscriptionEntryPublisherConnections(subscriptionToUpdate, request, authorizedPublishers);
			
			return subscriptionRepository.saveAndFlush(subscriptionToUpdate);
					
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Set<Subscription> getInvolvedSubscriptions(final EventPublishRequestDTO request) {
		logger.debug("getInvolvedSubscriptions started ...");
		
		final EventType validEventType = validateEventType(request.getEventType());
		
		if (!secure) {
			
			return subscriptionRepository.findAllByEventType(validEventType);
			
		}
		
		try {
			
			final Set<Subscription> involvedSubscriptions = subscriptionRepository.findAllByEventType(validEventType);
			final System validProviderSystem = validateSystemRequestDTO(request.getSource());
			
			return filterInvolvedSubscriptionsByAuthorizedProviders(involvedSubscriptions, validProviderSystem);
			
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public List<Subscription> getInvolvedSubscriptionsBySubscriberSystemId(final Long subscriberSystemId) {
		logger.debug("getInvolvedSubscriptionsBySubscriberSystemId started ...");
		
		try {
			
			final Optional<System> subscriberSystemOptional = systemRepository.findById(subscriberSystemId);
			if ( subscriberSystemOptional.isEmpty() ) {
				
				throw new InvalidParameterException("SubscriberSystem" + NOT_IN_DB_ERROR_MESSAGE);
			}
			
			final List<Subscription> involvedSubscriptions = subscriptionRepository.findAllBySubscriberSystem( subscriberSystemOptional.get() );
			
			return involvedSubscriptions;
			
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public void updateSubscriberAuthorization(final List<Subscription> involvedSubscriptions,
			final Set<SystemResponseDTO> authorizedPublishers) {
		logger.debug("updateSubscriberAuthorization started ...");
		
		for (final Subscription subscriptionEntry : involvedSubscriptions) {
			
			updateSubscriptionEntryPublisherConnections(subscriptionEntry, authorizedPublishers);
		}
		
	}
	
	//=================================================================================================
	//Assistant methods

	//-------------------------------------------------------------------------------------------------
	private Subscription validateSubscriptionRequestDTO(final SubscriptionRequestDTO request) {
		logger.debug("validatesubscriptionRequestDTO started ...");
		
		final System validSubscriberSystem = validateSystemRequestDTO(request.getSubscriberSystem());
		final EventType validEventType = validateEventType(request.getEventType());
		
		final Subscription subscription = new Subscription();
		subscription.setSubscriberSystem(validSubscriberSystem);
		subscription.setEventType(validEventType);
		subscription.setNotifyUri(request.getNotifyUri());
		subscription.setFilterMetaData(Utilities.map2Text(request.getFilterMetaData()));
		subscription.setOnlyPredefinedPublishers(request.getSources() != null && !request.getSources().isEmpty()); //TODO orginize to method
		subscription.setMatchMetaData(request.getMatchMetaData());
		//TODO validate dates by comparing to currentTime and threshold
		subscription.setStartDate(Utilities.parseUTCStringToLocalZonedDateTime(request.getStartDate()));
		subscription.setEndDate(Utilities.parseUTCStringToLocalZonedDateTime(request.getEndDate()));
		
		return subscription;
	}
	
	//-------------------------------------------------------------------------------------------------
	private System validateSystemRequestDTO(final SystemRequestDTO systemRequestDTO) {
		logger.debug("validateSystemRequestDTO started...");

		final String address = systemRequestDTO.getAddress().trim().toLowerCase();
		final String systemName = systemRequestDTO.getSystemName().trim().toLowerCase();
		final int port = systemRequestDTO.getPort();
		
		final Optional<System> systemOptional;
		try {
			
			systemOptional = systemRepository.findBySystemNameAndAddressAndPort(systemName, address, port);
					
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
		if (systemOptional.isEmpty()) {
			throw new InvalidParameterException("System by systemName: " + systemName + ", address: " + address + ", port: " + port + NOT_IN_DB_ERROR_MESSAGE);
		}
		
		return systemOptional.get();
	}	

	//-------------------------------------------------------------------------------------------------
	private EventType validateEventType(final String eventType) {
		logger.debug("validateEventType started...");
		
		try {
			
			final String validEventTypeName = eventType.toUpperCase().trim();//TODO create normalizer in Utilities
			final Optional<EventType> eventTypeOptional = eventTypeRepository.findByEventTypeName( validEventTypeName );
			if ( eventTypeOptional.isEmpty() ) {
				
				return eventTypeRepository.saveAndFlush(new EventType( validEventTypeName ));
			}
			
			return eventTypeOptional.get();		
			
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
	}	

	//-------------------------------------------------------------------------------------------------
	private Set<System> getPreferredPublisherSystems(final Set<SystemRequestDTO> sources) {
		logger.debug("getPreferredPublisherSystems started...");
		
		final Set<System> preferredSystems = new HashSet<>();
		
		for (final SystemRequestDTO source : sources) {
			
			preferredSystems.add(validateSystemRequestDTO(source));
		}
		
		return preferredSystems;
	}
	
	//-------------------------------------------------------------------------------------------------
	private void addAndSaveSubscriptionEntryPublisherConnections(final Subscription subscriptionEntry,
			final SubscriptionRequestDTO request, final Set<SystemResponseDTO> authorizedPublishers) {
		logger.debug("addAndSaveSubscriptionEntryPublisherConnections started...");
		
		if (subscriptionEntry.isOnlyPredefinedPublishers()) {
			
			final Set<System> preferredPublisherSystems = getPreferredPublisherSystems( request.getSources());
			
			for (final System system : preferredPublisherSystems) {
				final SubscriptionPublisherConnection conn = new SubscriptionPublisherConnection(subscriptionEntry, system);
				conn.setAuthorized(false);
				for (final SystemResponseDTO systemResponseDTO : authorizedPublishers) {
					
					if (DTOUtilities.equalsSystemInResponseAndRequest(systemResponseDTO, DTOConverter.convertSystemToSystemRequestDTO(system))) {
						conn.setAuthorized(true);
						break;
					}
				}
				
				subscriptionEntry.getPublisherConnections().add(conn);
			}
		} else {
			
			for (final SystemResponseDTO systemResponseDTO : authorizedPublishers) {
				
				final System system = new System(
						systemResponseDTO.getSystemName(), 
						systemResponseDTO.getAddress(), 
						systemResponseDTO.getPort(), 
						systemResponseDTO.getAuthenticationInfo());
				system.setId(systemResponseDTO.getId());
						
				final SubscriptionPublisherConnection conn = new SubscriptionPublisherConnection(subscriptionEntry, system);
				conn.setAuthorized(true);
				
				subscriptionEntry.getPublisherConnections().add(conn);
			}
		}

		try {
			
			subscriptionPublisherConnectionRepository.saveAll(subscriptionEntry.getPublisherConnections());
			subscriptionPublisherConnectionRepository.flush();
			
			return;
			
		}catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	
	}
	
	//-------------------------------------------------------------------------------------------------
	private void updateSubscriptionEntryPublisherConnections(final Subscription subscriptionEntry, final Set<SystemResponseDTO> authorizedPublishers) {
		logger.debug("updateSubscriptionEntryPublisherConnections started...");
		
		if (subscriptionEntry.isOnlyPredefinedPublishers()) {
			
			final Set<SubscriptionPublisherConnection> involvedPublisherSystems = subscriptionPublisherConnectionRepository.findBySubscriptionEntry(subscriptionEntry);
			
			for (final SubscriptionPublisherConnection conn  : involvedPublisherSystems) {
				final System system = conn.getSystem();
				
				for (final SystemResponseDTO systemResponseDTO : authorizedPublishers) {
					
					if (DTOUtilities.equalsSystemInResponseAndRequest(systemResponseDTO, DTOConverter.convertSystemToSystemRequestDTO(system))) {
						
						conn.setAuthorized( true );
					
					} else {
						
						conn.setAuthorized( false );
					}
				}
			}
			
			try {
				
				subscriptionPublisherConnectionRepository.saveAll( involvedPublisherSystems );
				subscriptionPublisherConnectionRepository.flush();
				
				return;
				
			}catch (final Exception ex) {
				
				logger.debug(ex.getMessage(), ex);
				throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
			}
			
		} else {
			

			try {
				
				final Set<SubscriptionPublisherConnection> involvedPublisherSystems = subscriptionPublisherConnectionRepository.findBySubscriptionEntry(subscriptionEntry);
				
				subscriptionPublisherConnectionRepository.deleteInBatch(involvedPublisherSystems);
				subscriptionRepository.refresh(subscriptionEntry);			
								
			}catch (final Exception ex) {
				
				logger.debug(ex.getMessage(), ex);
				throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
			}
			
			for (final SystemResponseDTO systemResponseDTO : authorizedPublishers) {
				
				final System system = new System(
						systemResponseDTO.getSystemName(), 
						systemResponseDTO.getAddress(), 
						systemResponseDTO.getPort(), 
						systemResponseDTO.getAuthenticationInfo());
				system.setId(systemResponseDTO.getId());
						
				final SubscriptionPublisherConnection conn = new SubscriptionPublisherConnection(subscriptionEntry, system);
				conn.setAuthorized(true);
				
				subscriptionEntry.getPublisherConnections().add(conn);
			}
			
			try {
				
				subscriptionPublisherConnectionRepository.saveAll(subscriptionEntry.getPublisherConnections());
				subscriptionPublisherConnectionRepository.flush();
				
				return;
				
			}catch (final Exception ex) {
				
				logger.debug(ex.getMessage(), ex);
				throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private Set<Subscription> filterInvolvedSubscriptionsByAuthorizedProviders(final Set<Subscription> involvedSubscriptions,
			final System validProviderSystem) {
		logger.debug("filterInvolvedSubscriptionsByAuthorizedProviders started...");
		
		final List<SubscriptionPublisherConnection> involvedConnections = subscriptionPublisherConnectionRepository.findAllBySystemAndAuthorized(validProviderSystem, true);
		final Set<Subscription> involvedSubscriptionsFromConnections = new HashSet<>();
		
		for ( final SubscriptionPublisherConnection spConnection : involvedConnections ) {
			
			final Subscription subscription = spConnection.getSubscriptionEntry();
			if ( involvedSubscriptions.contains( subscription )  && !involvedSubscriptionsFromConnections.contains( subscription ) ) {
				
				involvedSubscriptionsFromConnections.add( subscription );
			}
		}
		
		return involvedSubscriptionsFromConnections;
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkSubscriptionUniqueConstrains(final Subscription subscription) {
		logger.debug("checkSubscriptionUniqueConstrains started...");
		
		final Optional<Subscription> subcriptionOptional;
		try {
			
			subcriptionOptional = subscriptionRepository.findByEventTypeAndSubscriberSystem( subscription.getEventType(), subscription.getSubscriberSystem() );
			
		} catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
		if ( subcriptionOptional.isPresent()) {
			
			throw new InvalidParameterException("Subscription" + VIOLATES_UNIQUE_CONSTRAINT );
			
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkSubscriptionUpdateUniqueConstrains(final EventType eventTypeForUpdate, final System subscriberSystemForUpdate) {
		logger.debug("checkSubscriptionUpdateUniqueConstrains started...");
		
		final Optional<Subscription> subcriptionOptional;
		try {
			
			subcriptionOptional = subscriptionRepository.findByEventTypeAndSubscriberSystem( eventTypeForUpdate, subscriberSystemForUpdate );
			
		} catch (final Exception ex) {
			
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
		if ( subcriptionOptional.isPresent()) {
			
			throw new InvalidParameterException("Subscription" + VIOLATES_UNIQUE_CONSTRAINT );
			
		}
	}

}