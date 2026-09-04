package se.fk.rimfrost.framework.regel.oul.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Idtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.UppgiftSpecifikation;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.oul.logic.OulHandlerInterface;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableCreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableProcessInfo;
import se.fk.rimfrost.framework.oul.model.OperativUppgift;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.framework.regel.integration.kafka.dto.ImmutableRegelResponse;
import se.fk.rimfrost.framework.regel.integration.kafka.dto.RegelResponse;
import se.fk.rimfrost.framework.regel.logic.RegelCancelledException;
import se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;
import se.fk.rimfrost.framework.regel.oul.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.ProcessTopicInfoStorage;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableProcessTopicInfo;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ProcessTopicInfo;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Creates OUL uppgifter, persists correlation state on behalf of consumer regel
 * services, and processes OUL status callbacks by syncing them back to the
 * handläggning.
 *
 * <p>Encapsulates the OUL adapter call together with the three correlation stores
 * ({@link CloudEventDataStorage}, {@link ProcessTopicInfoStorage},
 * {@link RegelCommonDataStorage}) so consumers do not repeat the creation + persist
 * + orphan-cleanup pattern in every regel implementation.
 *
 * <p>The OUL reply-subtopic is injected from the {@code kafka.subtopic}
 * configuration property; consumers do not pass it on {@link OulUppgiftSpec}.
 *
 * <p>As part of {@link #createOulUppgift(OulUppgiftSpec)} the handläggning is
 * updated with the new uppgift reference and specification (FROUL-FR-01.7)
 * using the {@link Handlaggning} supplied on the spec.
 *
 * <p>Implements {@link OulHandlerInterface} so that the framework-oul Kafka
 * consumer routes OUL status messages here (FROUL-FR-02). Only one
 * implementation of {@link OulHandlerInterface} is permitted on the consumer
 * classpath.
 */
@ApplicationScoped
public class OulUppgiftService implements OulHandlerInterface
{
   private static final Logger LOGGER = LoggerFactory.getLogger(OulUppgiftService.class);
   private static final String FSSA_INFORMATION_PLACEHOLDER = "FSSAinformation.HANDLAGGNING_PAGAR"; // TODO

   @ConfigProperty(name = "kafka.subtopic")
   String oulReplyToSubTopic;

   @Inject
   OulAdapter oulAdapter;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @Inject
   ProcessTopicInfoStorage processTopicInfoStorage;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   @Inject
   RegelKafkaProducer regelKafkaProducer;

   /**
    * Creates an OUL uppgift, updates the handläggning with the new uppgift
    * reference, and persists correlation state (CloudEventData,
    * ProcessTopicInfo, RegelCommonData) for the handläggning.
    *
    * <p>If the handläggning update or any persistence step fails after the OUL
    * uppgift has been created, the just-created OUL uppgift is best-effort
    * ended and any partially written correlation rows are best-effort deleted
    * before the original exception is rethrown — this avoids leaving an
    * orphaned OUL uppgift in the handläggare's inbox.
    *
    * @param spec consumer-supplied specification of the OUL uppgift to create
    * @return the {@link OperativUppgift} returned by OUL, containing the OUL uppgift id and status
    * @throws OulException            if OUL uppgift creation fails
    * @throws RegelCancelledException if handläggning update or correlation persistence fails after OUL create
    */
   public OperativUppgift createOulUppgift(OulUppgiftSpec spec) throws OulException
   {
      CreateOperativUppgiftRequest oulRequest = buildOulRequest(spec);
      OperativUppgift operativUppgift = oulAdapter.createOperativUppgift(oulRequest);

      try
      {
         Uppgift uppgift = createUppgift(spec, operativUppgift.getStatus());

         HandlaggningUpdate handlaggningUpdate = createHandlaggningUpdate(
               spec.handlaggning(),
               uppgift,
               spec.cloudEventData().kogitoprocinstanceid(),
               spec.handlaggning().version() + 1);
         updateHandlaggning(handlaggningUpdate, spec.cloudEventData());

         writeCloudEventData(spec.handlaggningId(), spec.cloudEventData());
         writeProcessTopicInfo(spec.handlaggningId(),
               ImmutableProcessTopicInfo.builder().replyTopic(spec.replyTo()).build());

         RegelCommonData commonData = ImmutableRegelCommonData.builder()
               .uppgift(uppgift)
               .oulUppgiftId(operativUppgift.getUppgiftId())
               .build();
         writeRegelCommonData(spec.handlaggningId(), commonData);
      }
      catch (RuntimeException persistEx)
      {
         tryEndOulUppgift(operativUppgift.getUppgiftId(),
               "Handläggning update or correlation storage failed — ending uppgift to avoid orphan");
         cleanupCorrelation(spec.handlaggningId());
         throw persistEx;
      }

      return operativUppgift;
   }

   /**
    * Best-effort ends an OUL uppgift. Logs on failure; never throws.
    *
    * @param uppgiftId OUL uppgift id
    * @param reason    human-readable reason recorded on the OUL uppgift
    */
   public void tryEndOulUppgift(UUID uppgiftId, String reason)
   {
      try
      {
         oulAdapter.endOperativUppgift(uppgiftId, reason);
      }
      catch (OulException e)
      {
         LOGGER.error("Could not end operativ uppgift with id {}", uppgiftId, e);
      }
   }

   /**
    * Best-effort deletes all correlation state for a handläggning. Individual
    * failures are logged but never abort the remaining deletes.
    *
    * @param handlaggningId the handläggning whose correlation state to remove
    */
   public void cleanupCorrelation(UUID handlaggningId)
   {
      tryDeleteRegelCommonData(handlaggningId);
      tryDeleteProcessTopicInfo(handlaggningId);
      tryDeleteCloudEventData(handlaggningId);
   }

   /**
    * Handles an OUL status callback by syncing the reported uppgift state
    * back to correlation storage and the handläggning (FROUL-FR-02).
    *
    * <p>If no {@link RegelCommonData} row exists for {@code handlaggningId}
    * the callback is silently ignored (FROUL-FR-02.5); this is expected for
    * flows that are correlated by other means (e.g. komplettering).
    *
    * <p>The handläggning is updated with the new uppgift state without
    * incrementing the handläggning's own version (FROUL-FR-02.4).
    *
    * <p>On any failure during processing, the OUL uppgift is best-effort ended,
    * correlation state is best-effort cleaned up, and an error response is sent
    * on the reply topic carried on the OUL status message.
    *
    * @param oulStatus the OUL status message
    */
   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      CloudEventData cloudEventData = null;
      try
      {
         cloudEventData = CloudEventAttributesMapper.toCloudEventData(oulStatus.processInfo().cloudeventAttributes());

         RegelCommonData commonRegelData = readRegelCommonData(oulStatus.handlaggningId());
         if (commonRegelData == null)
         {
            return;
         }
         Uppgift uppgift = commonRegelData.uppgift();
         Handlaggning handlaggning = getHandlaggning(oulStatus.handlaggningId(), cloudEventData);

         Uppgift updatedUppgift = ImmutableUppgift.builder()
               .from(uppgift)
               .version(uppgift.version() + 1)
               .utforarId(toHandlaggningModelIdtyp(Objects.requireNonNull(oulStatus.utforarId())))
               .planeradTs(oulStatus.planeradTill())
               .uppgiftStatus(oulStatus.uppgiftStatus())
               .build();

         HandlaggningUpdate handlaggningUpdate = createHandlaggningUpdate(
               handlaggning,
               updatedUppgift,
               handlaggning.processInstansId(),
               handlaggning.version());

         RegelCommonData updatedCommonRegelData = ImmutableRegelCommonData.builder()
               .from(commonRegelData)
               .uppgift(updatedUppgift)
               .build();

         writeRegelCommonData(oulStatus.handlaggningId(), updatedCommonRegelData);

         updateHandlaggning(handlaggningUpdate, cloudEventData);
      }
      catch (RuntimeException e)
      {
         LOGGER.error("Regel run in handleOulStatus cancelled due to error", e);

         RegelErrorInformation regelErrorInformation = createRegelErrorInformation(
               RegelFelkod.RIMFROST_OTHER,
               "Regel failed due to unexpected internal error. Handlaggning id: " + oulStatus.handlaggningId());
         if (e instanceof RegelCancelledException ex)
         {
            regelErrorInformation = ex.getRegelErrorInformation();
         }

         tryEndOulUppgift(oulStatus.uppgiftId(), "Internal error");
         cleanupCorrelation(oulStatus.handlaggningId());
         sendErrorResponse(oulStatus.handlaggningId(), cloudEventData, regelErrorInformation,
               oulStatus.processInfo().replyTopic());
      }
   }

   private CreateOperativUppgiftRequest buildOulRequest(OulUppgiftSpec spec)
   {
      return ImmutableCreateOperativUppgiftRequest.builder()
            .handlaggningId(spec.handlaggningId())
            .version(spec.version())
            .regel(spec.regel())
            .beskrivning(spec.beskrivning())
            .verksamhetslogik(spec.verksamhetslogik())
            .roll(spec.roll())
            .url(spec.url())
            .subTopic(oulReplyToSubTopic)
            .erbjudande(spec.erbjudande())
            .processInfo(ImmutableProcessInfo.builder()
                  .replyTopic(spec.replyTo())
                  .cloudeventAttributes(spec.cloudEventAttributes())
                  .build())
            .build();
   }

   private Uppgift createUppgift(OulUppgiftSpec spec, String status)
   {
      return ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
            .aktivitetId(spec.aktivitetId())
            .skapadTs(OffsetDateTime.now())
            .uppgiftStatus(status)
            .fSSAinformation(FSSA_INFORMATION_PLACEHOLDER)
            .uppgiftSpecifikation(createUppgiftSpecifikation(spec))
            .build();
   }

   private UppgiftSpecifikation createUppgiftSpecifikation(OulUppgiftSpec spec)
   {
      return ImmutableUppgiftSpecifikation.builder()
            .id(spec.uppgiftSpecifikationId())
            .version(spec.uppgiftSpecifikationVersion())
            .build();
   }

   private void writeCloudEventData(UUID handlaggningId,
         se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData cloudEventData)
   {
      try
      {
         cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);
      }
      catch (Exception e)
      {
         String message = String.format(
               "Failed to write CloudEventData to correlation storage. handlaggningId: %s", handlaggningId);
         throw new RegelCancelledException(
               createRegelErrorInformation(RegelFelkod.RIMFROST_CLOUD_EVENT_DATA_WRITE_FAILURE, message),
               message, e);
      }
   }

   private void writeProcessTopicInfo(UUID handlaggningId, ProcessTopicInfo processTopicInfo)
   {
      try
      {
         processTopicInfoStorage.setProcessTopicInfo(handlaggningId, processTopicInfo);
      }
      catch (Exception e)
      {
         String message = String.format(
               "Failed to write ProcessTopicInfo to correlation storage. handlaggningId: %s", handlaggningId);
         throw new RegelCancelledException(
               createRegelErrorInformation(RegelFelkod.RIMFROST_PROCESS_TOPIC_INFO_WRITE_FAILURE, message),
               message, e);
      }
   }

   private void writeRegelCommonData(UUID handlaggningId, RegelCommonData regelCommonData)
   {
      try
      {
         regelCommonDataStorage.setRegelCommonData(handlaggningId, regelCommonData);
      }
      catch (Exception e)
      {
         String message = String.format(
               "Failed to write RegelCommonData to data storage. handlaggningId: %s", handlaggningId);
         throw new RegelCancelledException(
               createRegelErrorInformation(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_WRITE_FAILURE, message),
               message, e);
      }
   }

   private RegelCommonData readRegelCommonData(UUID handlaggningId)
   {
      try
      {
         return regelCommonDataStorage.getRegelCommonData(handlaggningId);
      }
      catch (Exception e)
      {
         String message = String.format(
               "Failed to read RegelCommonData from data storage. handlaggningId: %s", handlaggningId);
         throw new RegelCancelledException(
               createRegelErrorInformation(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_READ_FAILURE, message),
               message, e);
      }
   }

   private void sendErrorResponse(UUID handlaggningId, CloudEventData cloudEventData,
         RegelErrorInformation regelErrorInformation, String replyTo)
   {
      if (handlaggningId == null || cloudEventData == null || regelErrorInformation == null)
      {
         LOGGER.warn(
               "Could not send error response. Missing one or more required parameters. handlaggningId: {}, cloudEventData: {}, regelErrorInformation: {}",
               handlaggningId, cloudEventData, regelErrorInformation);
         return;
      }

      try
      {
         RegelResponse regelResponse = ImmutableRegelResponse.builder()
               .id(cloudEventData.id())
               .handlaggningId(handlaggningId)
               .kogitoparentprociid(cloudEventData.kogitoparentprociid())
               .kogitorootprociid(cloudEventData.kogitorootprociid())
               .kogitoprocid(cloudEventData.kogitoprocid())
               .kogitorootprocid(cloudEventData.kogitorootprocid())
               .kogitoprocinstanceid(cloudEventData.kogitoprocinstanceid())
               .kogitoprocist(cloudEventData.kogitoprocist())
               .kogitoprocversion(cloudEventData.kogitoprocversion())
               .utfall(Utfall.ERROR)
               .type(cloudEventData.type())
               .source(cloudEventData.source())
               .regelErrorInformation(regelErrorInformation)
               .build();
         regelKafkaProducer.sendRegelResponse(regelResponse, Objects.requireNonNull(replyTo));
      }
      catch (IllegalStateException e)
      {
         LOGGER.error(
               "Failed to send regel response for handlaggning. handlaggningId: {}, regelErrorInformation: {}",
               handlaggningId, regelErrorInformation, e);
      }
   }

   private void tryDeleteCloudEventData(UUID handlaggningId)
   {
      try
      {
         cloudEventDataStorage.deleteCloudEventData(handlaggningId);
      }
      catch (Exception e)
      {
         LOGGER.error("Could not delete CloudEventData. handlaggningId: {}", handlaggningId, e);
      }
   }

   private void tryDeleteProcessTopicInfo(UUID handlaggningId)
   {
      try
      {
         processTopicInfoStorage.deleteProcessTopicInfo(handlaggningId);
      }
      catch (Exception e)
      {
         LOGGER.error("Could not delete ProcessTopicInfo. handlaggningId: {}", handlaggningId, e);
      }
   }

   private void tryDeleteRegelCommonData(UUID handlaggningId)
   {
      try
      {
         regelCommonDataStorage.deleteRegelCommonData(handlaggningId);
      }
      catch (Exception e)
      {
         LOGGER.error("Could not delete RegelCommonData. handlaggningId: {}", handlaggningId, e);
      }
   }

   private Handlaggning getHandlaggning(UUID handlaggningId,
         se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData cloudEventData)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         String message = String.format(
               "Failed to read handlaggning. handlaggningId: %s, kogitoprocId: %s",
               handlaggningId, cloudEventData.kogitoprocinstanceid());
         throw new RegelCancelledException(
               createRegelErrorInformation(RegelFelkod.RIMFROST_HANDLAGGNING_READ_FAILURE, message),
               message, e);
      }
   }

   private void updateHandlaggning(HandlaggningUpdate handlaggningUpdate,
         se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData cloudEventData)
   {
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         String message = String.format(
               "Failed to write handlaggning update. handlaggningId: %s, kogitoprocId: %s",
               handlaggningUpdate.id(), cloudEventData.kogitoprocinstanceid());
         throw new RegelCancelledException(
               createRegelErrorInformation(RegelFelkod.RIMFROST_HANDLAGGNING_WRITE_FAILURE, message),
               message, e);
      }
   }

   private HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning, Uppgift uppgift,
         UUID kogitoprocInstanceId, int version)
   {
      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(version)
            .yrkande(handlaggning.yrkande())
            .processInstansId(kogitoprocInstanceId)
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(uppgift)
            .build();
   }

   private Idtyp toHandlaggningModelIdtyp(se.fk.rimfrost.framework.oul.logic.dto.Idtyp idtyp)
   {
      return ImmutableIdtyp.builder()
            .typId(idtyp.typId())
            .varde(idtyp.varde())
            .build();
   }

   private RegelErrorInformation createRegelErrorInformation(String felkod, String meddelande)
   {
      RegelErrorInformation info = new RegelErrorInformation();
      info.setFelkod(felkod);
      info.setFelmeddelande(meddelande);
      return info;
   }
}
