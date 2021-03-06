package org.motechproject.mots.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.motechproject.mots.domain.CallDetailRecord;
import org.motechproject.mots.domain.IvrConfig;
import org.motechproject.mots.domain.enums.CallStatus;
import org.motechproject.mots.domain.enums.Language;
import org.motechproject.mots.dto.VotoCallLogDto;
import org.motechproject.mots.dto.VotoResponseDto;
import org.motechproject.mots.exception.IvrException;
import org.motechproject.mots.exception.MotsException;
import org.motechproject.mots.repository.CallDetailRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class IvrService {

  private static final String PHONE = "phone";
  private static final String PREFERRED_LANGUAGE = "preferred_language";
  private static final String GROUPS = "groups";
  private static final String NAME_PROPERTY = "property[name]";
  private static final String SUBSCRIBER_IDS = "subscriber_ids";
  private static final String SEND_TO_SUBSCRIBERS = "send_to_subscribers";
  private static final String MESSAGE_ID = "message_id";
  private static final String SEND_SMS_IF_VOICE_FAILS = "send_sms_if_voice_fails";
  private static final String DETECT_VOICEMAIL_ACTION = "detect_voicemail_action";
  private static final String RETRY_ATTEMPTS_SHORT = "retry_attempts_short";
  private static final String RETRY_ATTEMPTS_LONG = "retry_attempts_long";
  private static final String RETRY_DELAY_SHORT = "retry_delay_short";
  private static final String RETRY_DELAY_LONG = "retry_delay_long";
  private static final String API_KEY = "api_key";

  private static final String SUBSCRIBERS_URL = "/subscribers";
  private static final String MODIFY_SUBSCRIBERS_URL = "/subscribers/%s";
  private static final String ADD_TO_GROUPS_URL = "/subscribers/groups";
  private static final String DELETE_GROUPS_URL = "/subscribers/delete/groups";
  private static final String SEND_MESSAGE_URL = "/outgoing_calls";
  private static final String GET_CALL_LOGS_URL = "/trees/%s/delivery_logs/%s";

  @Value("${mots.ivrApiKey}")
  private String ivrApiKey;

  @Autowired
  private CallDetailRecordRepository callDetailRecordRepository;

  @Autowired
  private IvrConfigService ivrConfigService;

  @Autowired
  private ModuleProgressService moduleProgressService;

  @Autowired
  private ModuleService moduleService;

  private RestOperations restTemplate = new RestTemplate();
  private ObjectMapper mapper = new ObjectMapper();

  /**
   * Create IVR Subscriber with given phone number.
   * @param phoneNumber CHW phone number
   * @param name CHW name
   * @param preferredLanguage CHW preferred language
   * @return ivr id of created subscriber
   */
  public String createSubscriber(String phoneNumber, String name,
      Language preferredLanguage) throws IvrException {
    MultiValueMap<String, String> params = prepareBasicSubscriberParamsToSend(phoneNumber, name,
        preferredLanguage);

    String groups = ivrConfigService.getConfig().getDefaultUsersGroupId();
    params.add(GROUPS, groups);

    VotoResponseDto<String> votoResponse = sendVotoRequest(getAbsoluteUrl(SUBSCRIBERS_URL), params,
        new ParameterizedTypeReference<VotoResponseDto<String>>() {}, HttpMethod.POST);

    if (votoResponse == null || StringUtils.isBlank(votoResponse.getData())) {
      throw new IvrException(
          "Ivr subscriber created successfully, but subscriber id was not returned");
    }

    return votoResponse.getData();
  }

  /**
   * Update existing IVR subscriber.
   * @param ivrId id of existing IVR subscriber
   * @param phoneNumber new CHW phone number
   * @param name new CHW name
   * @param preferredLanguage new CHW preferred language
   */
  public void updateSubscriber(String ivrId, String phoneNumber, String name,
      Language preferredLanguage) throws IvrException {
    MultiValueMap<String, String> params = prepareBasicSubscriberParamsToSend(phoneNumber, name,
        preferredLanguage);

    String url = getUrlWithParams(MODIFY_SUBSCRIBERS_URL, ivrId);
    sendVotoRequest(url, params, new ParameterizedTypeReference<VotoResponseDto<String>>() {},
        HttpMethod.PUT);
  }

  /**
   * Add Subscriber to groups.
   * @param subscriberId subscriber id
   * @param groupsIds ids of groups to add subscriber
   */
  public void addSubscriberToGroups(String subscriberId,
      List<String> groupsIds) throws IvrException {
    if (!groupsIds.isEmpty()) {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add(SUBSCRIBER_IDS, subscriberId);
      params.add(GROUPS, StringUtils.join(groupsIds, ","));

      sendVotoRequest(getAbsoluteUrl(ADD_TO_GROUPS_URL), params,
          new ParameterizedTypeReference<VotoResponseDto<String>>() {}, HttpMethod.POST);

      sendModuleAssignedMessage(subscriberId);
    }
  }

  /**
   * Remove Subscriber from groups.
   * @param subscriberId subscriber id
   * @param groupsIds ids of groups to remove subscriber
   */
  public void removeSubscriberFromGroups(String subscriberId,
      List<String> groupsIds) throws IvrException {
    if (!groupsIds.isEmpty()) {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add(GROUPS, StringUtils.join(groupsIds, ","));
      params.add(SUBSCRIBER_IDS, subscriberId);
      sendVotoRequest(getAbsoluteUrl(DELETE_GROUPS_URL), params,
              new ParameterizedTypeReference<VotoResponseDto<String>>() {}, HttpMethod.DELETE);
    }
  }

  /**
   * Save IVR Call Detail Record.
   * @param callDetailRecord callDetailRecord to be saved
   * @param configName name of the IVR config
   */
  public void saveCallDetailRecord(CallDetailRecord callDetailRecord,
      String configName) throws IvrException {
    IvrConfig ivrConfig = ivrConfigService.getConfig();

    setConfigFields(callDetailRecord, ivrConfig, configName);
    callDetailRecordRepository.save(callDetailRecord);

    CallStatus callStatus = callDetailRecord.getCallStatus();
    boolean callInterrupted = CallStatus.FINISHED_INCOMPLETE.equals(callStatus);

    if ((CallStatus.FINISHED_COMPLETE.equals(callStatus) || callInterrupted)
        && noCallsStartedDuringCall(callDetailRecord)) {
      VotoCallLogDto votoCallLogDto = getVotoCallLog(callDetailRecord);

      moduleProgressService.updateModuleProgress(votoCallLogDto, callInterrupted);
    }
  }

  /**
   * This method checks if another call was started during this one for the same CHW.
   *
   * @param callDetailRecord of call that is going to be parsed.
   * @return true if there was no calls in progress during this one.
   */
  private boolean noCallsStartedDuringCall(CallDetailRecord callDetailRecord) {
    CallDetailRecord callDetailRecordInProgress = callDetailRecordRepository
        .findByCallLogIdAndCallStatus(callDetailRecord.getCallLogId(), CallStatus.IN_PROGRESS);

    Date startDate = callDetailRecordInProgress.getCreatedDate();
    Date endDate = callDetailRecord.getCreatedDate();

    List<CallDetailRecord> callDetailRecords = callDetailRecordRepository
        .findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanAndCallStatusAndChwIvrId(
            startDate, endDate, CallStatus.IN_PROGRESS, callDetailRecord.getChwIvrId());

    // There is always callDetailRecordInProgress included in this list.
    // We have to check if there is anything else.
    return callDetailRecords.size() == 1;
  }

  private VotoCallLogDto getVotoCallLog(CallDetailRecord callDetailRecord) throws IvrException {
    String votoMainTreeId = moduleService.getReleasedCourseIvrId();
    String logUrl = getUrlWithParams(GET_CALL_LOGS_URL, votoMainTreeId,
        callDetailRecord.getCallLogId());

    VotoResponseDto<VotoCallLogDto> response = sendVotoRequest(logUrl, new LinkedMultiValueMap<>(),
        new ParameterizedTypeReference<VotoResponseDto<VotoCallLogDto>>() {}, HttpMethod.GET);

    return response.getData();
  }

  private MultiValueMap<String, String> prepareBasicSubscriberParamsToSend(String phoneNumber,
      String name, Language preferredLanguage) {
    IvrConfig ivrConfig = ivrConfigService.getConfig();
    String preferredLanguageString = ivrConfig.getIvrLanguagesIds().get(preferredLanguage);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add(PHONE, phoneNumber);
    params.add(PREFERRED_LANGUAGE, preferredLanguageString);

    if (StringUtils.isNotBlank(name)) {
      params.add(NAME_PROPERTY, name);
    }
    return params;
  }

  private void sendModuleAssignedMessage(String subscriberId) throws IvrException {
    IvrConfig ivrConfig = ivrConfigService.getConfig();
    String messageId = ivrConfig.getModuleAssignedMessageId();
    String sendSmsIfVoiceFails = boolToIntAsString(ivrConfig.getSendSmsIfVoiceFails());
    String detectVoiceMailAction = boolToIntAsString(ivrConfig.getDetectVoicemailAction());
    String retryAttemptsShort = Integer.toString(ivrConfig.getRetryAttemptsShort());
    String retryDelayShort = Integer.toString(ivrConfig.getRetryDelayShort());
    String retryAttemptsLong = Integer.toString(ivrConfig.getRetryAttemptsLong());
    String retryDelayLong = Integer.toString(ivrConfig.getRetryDelayLong());

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add(SEND_TO_SUBSCRIBERS, subscriberId);
    params.add(MESSAGE_ID, messageId);
    params.add(SEND_SMS_IF_VOICE_FAILS, sendSmsIfVoiceFails);
    params.add(DETECT_VOICEMAIL_ACTION, detectVoiceMailAction);
    params.add(RETRY_ATTEMPTS_SHORT, retryAttemptsShort);
    params.add(RETRY_DELAY_SHORT, retryDelayShort);
    params.add(RETRY_ATTEMPTS_LONG, retryAttemptsLong);
    params.add(RETRY_DELAY_LONG, retryDelayLong);

    sendVotoRequest(getAbsoluteUrl(SEND_MESSAGE_URL), params,
        new ParameterizedTypeReference<VotoResponseDto<String>>() {}, HttpMethod.POST);
  }

  private <T> T sendVotoRequest(String url, MultiValueMap<String, String> params,
      ParameterizedTypeReference<T> responseType, HttpMethod method) throws IvrException {
    params.add(API_KEY, ivrApiKey);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParams(params);

    HttpEntity<?> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<T> responseEntity = restTemplate.exchange(builder.build().toString(),
          method, request, responseType);
      return responseEntity.getBody();
    } catch (RestClientResponseException ex) {
      String responseBodyJson = ex.getResponseBodyAsString();
      String responseMessage = responseBodyJson;
      String clearVotoInfo = "";
      try {
        VotoResponseDto<String> votoResponse = mapper.readValue(responseBodyJson,
            new TypeReference<VotoResponseDto<String>>() {});

        if (votoResponse.getMessage() != null) {
          responseMessage = votoResponse.getMessage();
          clearVotoInfo = "Invalid IVR service response: " + responseMessage;
        }
      } catch (IOException e) {
        responseMessage = responseBodyJson;
      }

      String message = "Invalid IVR service response: " + ex.getRawStatusCode() + " "
          + ex.getStatusText() + ", Response body: " + responseMessage;
      throw new IvrException(message, ex, clearVotoInfo);
    } catch (RestClientException ex) {
      String message = "Error occurred when sending request to IVR service: " + ex.getMessage();
      throw new IvrException(message, ex);
    }
  }

  private void setConfigFields(CallDetailRecord callDetailRecord,
      IvrConfig ivrConfig, String configName) {
    Map<String, String> ivrData = callDetailRecord.getIvrData();

    String ivrCallStatus = ivrData.get(ivrConfig.getCallStatusField());

    callDetailRecord.setCallStatus(getCallStatus(ivrConfig, ivrCallStatus));
    callDetailRecord.setIvrConfigName(configName);
    callDetailRecord.setCallId(ivrData.get(ivrConfig.getCallIdField()));
    callDetailRecord.setChwIvrId(ivrData.get(ivrConfig.getChwIvrIdField()));
    callDetailRecord.setCallLogId(ivrData.get(ivrConfig.getCallLogIdField()));

    ivrData.remove(ivrConfig.getCallIdField());
    ivrData.remove(ivrConfig.getChwIvrIdField());
    ivrData.remove(ivrConfig.getCallLogIdField());
  }

  private CallStatus getCallStatus(IvrConfig ivrConfig, String ivrCallStatus) {
    CallStatus callStatus = ivrConfig.getCallStatusMap().get(ivrCallStatus);

    if (callStatus == null) {
      return CallStatus.UNKNOWN;
    }

    return callStatus;
  }

  private String getAbsoluteUrl(String relativeUrl) {
    return ivrConfigService.getConfig().getBaseUrl() + relativeUrl;
  }

  private String getUrlWithParams(String url, String... params) {
    return getAbsoluteUrl(String.format(url, (Object[]) params));
  }

  private String boolToIntAsString(Boolean bool) {
    if (bool == null) {
      throw new MotsException("Bad IVR config - boolean value is null");
    }
    return Integer.toString(BooleanUtils.toInteger(bool));
  }
}
