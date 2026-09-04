package se.fk.rimfrost.framework.regel.oul.storage.entity;

import org.immutables.value.Value;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import jakarta.annotation.Nullable;
import java.util.UUID;

/**
 * Correlation record that links a handläggning to the uppgift created for it and to
 * the corresponding OUL uppgift.
 *
 * <p>Read by {@code OulUppgiftService} when an OUL status callback arrives so the
 * current uppgift state can be updated and synced back to the handläggning.
 */
@Value.Immutable
public interface RegelCommonData
{
   Uppgift uppgift();

   @Nullable
   UUID oulUppgiftId();
}
