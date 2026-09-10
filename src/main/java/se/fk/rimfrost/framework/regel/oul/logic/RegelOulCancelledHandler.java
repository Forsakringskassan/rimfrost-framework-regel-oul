package se.fk.rimfrost.framework.regel.oul.logic;

import java.util.UUID;

/**
 * Optional extension point for rule implementations that need to perform custom cleanup when
 * a cancelled-event is received. The framework calls this before its own cleanup (ending the
 * OUL task and clearing correlation data). Errors thrown by this method are caught and logged
 * by the framework; they do not prevent the built-in cleanup from running (FROUL-FR-04.6).
 */
public interface RegelOulCancelledHandler
{

   /**
    * Called when a cancelled-event is received for the given handläggning, before the framework
    * cleans up correlation data and ends the OUL task.
    *
    * @param handlaggningId the handläggning whose regel data is being cleaned up
    */
   void handleCancelled(UUID handlaggningId);

}
