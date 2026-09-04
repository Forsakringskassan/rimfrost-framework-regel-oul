package se.fk.rimfrost.framework.regel.oul.storage.internal;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Hibernate physical naming strategy that prepends a configurable prefix to every table name.
 *
 * <p>The prefix is read from the {@code regel.persistence.table-prefix} configuration property
 * (e.g. {@code rtf_manuell}). This allows multiple regel services to share the same database
 * schema without table-name collisions. The property is mandatory; startup fails with an
 * {@link IllegalStateException} if it is absent.
 */
public class RegelPhysicalNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl
{
   @Override
   public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context)
   {
      Identifier physical = super.toPhysicalTableName(logicalName, context);
      String prefix = ConfigProvider.getConfig()
            .getOptionalValue("regel.persistence.table-prefix", String.class)
            .orElseThrow(() -> new IllegalStateException(
                  "regel.persistence.table-prefix must be configured — set it to a unique identifier for this regel service (e.g. rtf_manuell)"));
      return Identifier.toIdentifier(prefix + "_" + physical.getText());
   }
}
